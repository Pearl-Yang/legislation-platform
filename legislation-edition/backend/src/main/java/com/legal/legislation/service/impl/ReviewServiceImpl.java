package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.entity.LegislativeDraft;
import com.legal.legislation.entity.ReviewIssue;
import com.legal.legislation.entity.ReviewRecord;
import com.legal.legislation.entity.ReviewRule;
import com.legal.legislation.mapper.LegislativeDraftMapper;
import com.legal.legislation.mapper.ReviewIssueMapper;
import com.legal.legislation.mapper.ReviewRecordMapper;
import com.legal.legislation.mapper.ReviewRuleMapper;
import com.legal.legislation.notify.NotifyMessage;
import com.legal.legislation.notify.NotifyService;
import com.legal.legislation.service.ReviewService;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智慧审查 Service
 *
 * 审查规则基于 review_rule 表配置；当前内置了 6 类规则的轻量检测：
 *  - 关键词命中：与上位法冲突、越权、引用失效
 *  - 格式正则：章节格式、条款格式
 *  - 文本特征：长度过长（VERBOSE）
 *
 * 真正的"上位法全文比对 / 失效引用比对"需要先建立 regulation 语料库，
 * 由后续 legislation_commons 的 RegulationCorpusService 提供。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRecordMapper recordMapper;
    private final ReviewIssueMapper  issueMapper;
    private final ReviewRuleMapper   ruleMapper;
    private final LegislativeDraftMapper draftMapper;
    private final NotifyService      notifyService;

    private static final Pattern CHAPTER_PATTERN  = Pattern.compile("(?m)^第[一二三四五六七八九十百千]+章");
    private static final Pattern ARTICLE_PATTERN  = Pattern.compile("(?m)^第[一二三四五六七八九十百千]+条");
    private static final Pattern KEYWORD_CONFLICT = Pattern.compile("(上位法|违反|抵触|不一致|相冲突)");
    private static final Pattern KEYWORD_OVERPWR  = Pattern.compile("(增设行政许可|增设行政处罚|强制规定|应当许可)");
    private static final Pattern KEYWORD_OUTDATED = Pattern.compile("(废止|失效|已修订|已被修订)");

    @Override
    public Task<Long> submitReview(Long draftId, String reviewType) {
        LegislativeDraft draft = draftMapper.selectById(draftId);
        if (draft == null) {
            return Task.error("草案不存在: " + draftId);
        }
        ReviewRecord record = new ReviewRecord();
        record.setDraftId(draftId);
        record.setReviewType(reviewType == null ? ReviewRecord.REVIEW_TYPE_AUTO : reviewType);
        record.setStatus(ReviewRecord.STATUS_PENDING);
        record.setErrorCount(0);
        recordMapper.insert(record);
        runReviewAsync(record.getId());
        return Task.ok(record.getId());
    }

    @Async
    void runReviewAsync(Long recordId) {
        try {
            ReviewRecord record = recordMapper.selectById(recordId);
            if (record == null) return;
            LegislativeDraft draft = draftMapper.selectById(record.getDraftId());
            if (draft == null) {
                record.setStatus(ReviewRecord.STATUS_DONE);
                record.setOverallPass(0);
                recordMapper.updateById(record);
                return;
            }
            String content = draft.getDraftContent() == null ? "" : draft.getDraftContent();

            List<ReviewIssue> issues = new ArrayList<>();
            runRuleCheck(ReviewIssue.TYPE_SUPERIOR_CONFLICT, ReviewIssue.SEVERITY_RED,
                    detectInContent(content, KEYWORD_CONFLICT, "可能与上位法存在冲突"), issues, recordId);
            runRuleCheck(ReviewIssue.TYPE_OVER_POWER, ReviewIssue.SEVERITY_RED,
                    detectInContent(content, KEYWORD_OVERPWR, "疑似越权设定行政许可 / 行政处罚"), issues, recordId);
            runRuleCheck(ReviewIssue.TYPE_OUTDATED_REF, ReviewIssue.SEVERITY_YELLOW,
                    detectInContent(content, KEYWORD_OUTDATED, "引用了疑似失效 / 修订条文"), issues, recordId);
            runRuleCheck(ReviewIssue.TYPE_FORMAT, ReviewIssue.SEVERITY_BLUE,
                    detectChapterFormat(content), issues, recordId);
            runRuleCheck(ReviewIssue.TYPE_VERBOSE, ReviewIssue.SEVERITY_GREY,
                    detectVerboseText(content), issues, recordId);

            int redCount = 0;
            for (ReviewIssue i : issues) {
                issueMapper.insert(i);
                if (ReviewIssue.SEVERITY_RED.equals(i.getSeverity())) redCount++;
            }

            record.setStatus(ReviewRecord.STATUS_DONE);
            record.setReviewedAt(LocalDateTime.now());
            record.setErrorCount(redCount);
            record.setOverallPass(redCount == 0 ? 1 : 0);
            recordMapper.updateById(record);

            notifyService.send(new NotifyMessage(
                draft.getCreatedBy(),
                "SYSTEM",
                "智慧审查完成",
                String.format("草案 #%d 审查完成：红色问题 %d 条，建议修改 %d 条。",
                    draft.getId(), redCount, issues.size() - redCount),
                "review_record",
                recordId,
                LocalDateTime.now()
            ));
        } catch (Exception ex) {
            log.error("审查执行失败 recordId={}", recordId, ex);
            ReviewRecord r = recordMapper.selectById(recordId);
            if (r != null) {
                r.setStatus(ReviewRecord.STATUS_DONE);
                r.setOverallPass(0);
                recordMapper.updateById(r);
            }
        }
    }

    private void runRuleCheck(String type, String severity, List<String[]> findings,
                              List<ReviewIssue> issues, Long recordId) {
        if (findings == null) return;
        for (String[] f : findings) {
            ReviewIssue i = new ReviewIssue();
            i.setReviewRecordId(recordId);
            i.setIssueType(type);
            i.setSeverity(severity);
            i.setArticleIndex(f[0]);
            i.setDescription(f[1]);
            i.setSuggestion(suggestionFor(type));
            i.setIsResolved(0);
            issues.add(i);
        }
    }

    private List<String[]> detectInContent(String content, Pattern p, String desc) {
        List<String[]> results = new ArrayList<>();
        if (content == null || content.isEmpty()) return results;
        String[] lines = content.split("\\R");
        for (String line : lines) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                String article = nearestArticle(lines, line);
                results.add(new String[]{article, desc + "：\n" + line.trim()});
            }
        }
        return results;
    }

    private List<String[]> detectChapterFormat(String content) {
        List<String[]> results = new ArrayList<>();
        if (content == null || content.isEmpty()) return results;
        boolean hasChapter = CHAPTER_PATTERN.matcher(content).find();
        boolean hasArticle = ARTICLE_PATTERN.matcher(content).find();
        if (!hasChapter) {
            results.add(new String[]{"(全文)", "未检测到「第 X 章」章节标题，建议按规范补全。"});
        }
        if (!hasArticle) {
            results.add(new String[]{"(全文)", "未检测到「第 X 条」条款编号，建议按规范补全。"});
        }
        return results;
    }

    private List<String[]> detectVerboseText(String content) {
        List<String[]> results = new ArrayList<>();
        if (content == null || content.isEmpty()) return results;
        String[] lines = content.split("\\R");
        for (String line : lines) {
            if (line.length() > 200) {
                results.add(new String[]{nearestArticle(lines, line),
                    "单行字数超过 200，可读性较差，建议拆分为多条或精简。"});
            }
        }
        if (results.size() > 5) {
            return results.subList(0, 5);
        }
        return results;
    }

    private String nearestArticle(String[] lines, String current) {
        String last = "(全文)";
        for (int i = 0; i < lines.length; i++) {
            Matcher m = ARTICLE_PATTERN.matcher(lines[i]);
            if (m.find()) last = m.group();
            if (lines[i] == current) break;
        }
        return last;
    }

    private String suggestionFor(String type) {
        return switch (type) {
            case ReviewIssue.TYPE_SUPERIOR_CONFLICT -> "请检索上位法相关条款，确认是否实质冲突；如冲突，调整表述或删除重复义务。";
            case ReviewIssue.TYPE_OVER_POWER        -> "请确认本级立法主体是否享有该权限；如无，删除相应条款。";
            case ReviewIssue.TYPE_OUTDATED_REF      -> "请通过立法资料库核对引用法条是否现行有效。";
            case ReviewIssue.TYPE_FORMAT            -> "按《行政法规制定程序条例》格式补全章节与条款编号。";
            case ReviewIssue.TYPE_VERBOSE           -> "建议拆分为多条短句或精简措辞。";
            default                                  -> "请人工复核。";
        };
    }

    @Override
    public Task<Map<String, Object>> getReviewRecord(Long recordId) {
        ReviewRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            return Task.error("审查记录不存在");
        }
        QueryWrapper<ReviewIssue> qw = new QueryWrapper<>();
        qw.eq("review_record_id", recordId);
        // 严重级别排序: RED > YELLOW > BLUE > GREY
        qw.orderByAsc("severity");
        List<ReviewIssue> issues = issueMapper.selectList(qw);
        Map<String, Object> data = new HashMap<>();
        data.put("record", record);
        data.put("issues", issues);
        return Task.ok(data);
    }

    @Override
    public Task<List<ReviewRule>> listRules(String ruleType, String severity) {
        QueryWrapper<ReviewRule> qw = new QueryWrapper<>();
        if (ruleType != null) qw.eq("rule_type", ruleType);
        if (severity != null) qw.eq("severity", severity);
        qw.orderByAsc("id");
        return Task.ok(ruleMapper.selectList(qw));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Boolean> updateRule(Long id, ReviewRule rule) {
        ReviewRule exist = ruleMapper.selectById(id);
        if (exist == null) {
            return Task.error("规则不存在");
        }
        rule.setId(id);
        rule.setUpdatedAt(LocalDateTime.now());
        int rows = ruleMapper.updateById(rule);
        return Task.ok(rows > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Boolean> resolveIssue(Long issueId) {
        ReviewIssue issue = issueMapper.selectById(issueId);
        if (issue == null) {
            return Task.error("问题不存在");
        }
        issue.setIsResolved(1);
        int rows = issueMapper.updateById(issue);
        return Task.ok(rows > 0);
    }

    @Override
    public Task<List<Long>> batchSubmit(List<Long> draftIds) {
        if (draftIds == null || draftIds.isEmpty()) {
            return Task.error("draftIds 不能为空");
        }
        List<Long> recordIds = new ArrayList<>();
        for (Long draftId : draftIds) {
            Task<Long> t = submitReview(draftId, ReviewRecord.REVIEW_TYPE_AUTO);
            if (t.isSuccess()) {
                recordIds.add(t.getData());
            }
        }
        return Task.ok(recordIds);
    }
}