package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.legislation.entity.Consultation;
import com.legal.legislation.entity.Opinion;
import com.legal.legislation.entity.OpinionCategory;
import com.legal.legislation.entity.OpinionReply;
import com.legal.legislation.mapper.ConsultationMapper;
import com.legal.legislation.mapper.OpinionCategoryMapper;
import com.legal.legislation.mapper.OpinionMapper;
import com.legal.legislation.mapper.OpinionReplyMapper;
import com.legal.legislation.notify.NotifyMessage;
import com.legal.legislation.notify.NotifyService;
import com.legal.legislation.service.ConsultationService;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 意见征集 Service
 *
 * AI 部分当前用占位实现:
 *   - classify: 基于关键词 + 长度 推一个 category
 *   - dedup   : SimHash (简单 sha1 前 8 字节 16 进制) 占位
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationMapper     consultationMapper;
    private final OpinionMapper          opinionMapper;
    private final OpinionReplyMapper     replyMapper;
    private final OpinionCategoryMapper  categoryMapper;
    private final NotifyService          notifyService;

    private static final List<String[]> KEYWORDS = List.of(
        new String[]{"数据安全", "数据", "个人信息", "数据出境"},
        new String[]{"行政许可", "许可", "审批", "备案"},
        new String[]{"行政处罚", "处罚", "罚款", "强制"},
        new String[]{"程序正当", "听证", "告知", "申辩"},
        new String[]{"法律责任", "责任", "追究", "赔偿"}
    );

    @Override
    public Task<?> list(int page, int size, String status) {
        QueryWrapper<Consultation> qw = new QueryWrapper<>();
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("created_at");
        return Task.ok(consultationMapper.selectPage(new Page<>(page, size), qw));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Consultation> create(Consultation consultation) {
        if (consultation.getTitle() == null || consultation.getTitle().isBlank()) {
            return Task.error("征集标题不能为空");
        }
        if (consultation.getStatus() == null) consultation.setStatus(Consultation.STATUS_DRAFT);
        consultation.setCreatedAt(LocalDateTime.now());
        consultation.setTotalViews(0);
        consultation.setTotalOpinions(0);
        consultationMapper.insert(consultation);
        return Task.ok(consultation);
    }

    @Override
    public Task<Map<String, Object>> getDetail(Long id) {
        Consultation c = consultationMapper.selectById(id);
        if (c == null) return Task.error("征集不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("consultation", c);
        QueryWrapper<OpinionCategory> qw = new QueryWrapper<>();
        qw.eq("consultation_id", id);
        data.put("categories", categoryMapper.selectList(qw));
        return Task.ok(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Boolean> update(Long id, Consultation consultation) {
        Consultation exist = consultationMapper.selectById(id);
        if (exist == null) return Task.error("征集不存在");
        consultation.setId(id);
        int rows = consultationMapper.updateById(consultation);
        return Task.ok(rows > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Opinion> submitOpinion(Long consultationId, Opinion opinion) {
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null) return Task.error("征集不存在");
        if (!Consultation.STATUS_OPEN.equals(c.getStatus())) {
            return Task.error("征集未在征集中，无法提交意见");
        }
        if (opinion.getContent() == null || opinion.getContent().isBlank()) {
            return Task.error("意见正文不能为空");
        }
        opinion.setConsultationId(consultationId);
        opinion.setStatus(Opinion.STATUS_NEW);
        opinion.setSubmittedAt(LocalDateTime.now());
        opinion.setSimilarityHash(simHash(opinion.getContent()));
        opinionMapper.insert(opinion);

        // 更新咨询累计
        c.setTotalOpinions((c.getTotalOpinions() == null ? 0 : c.getTotalOpinions()) + 1);
        consultationMapper.updateById(c);
        return Task.ok(opinion);
    }

    @Override
    public Task<?> listOpinions(Long consultationId, int page, int size, String status, String category) {
        QueryWrapper<Opinion> qw = new QueryWrapper<>();
        qw.eq("consultation_id", consultationId);
        if (status   != null)   qw.eq("status", status);
        if (category != null)   qw.eq("classified_category", category);
        qw.orderByDesc("submitted_at");
        return Task.ok(opinionMapper.selectPage(new Page<>(page, size), qw));
    }

    @Override
    public Task<Map<String, Object>> getStatistics(Long consultationId) {
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null) return Task.error("征集不存在");
        QueryWrapper<Opinion> qw = new QueryWrapper<>();
        qw.eq("consultation_id", consultationId);
        List<Opinion> opinions = opinionMapper.selectList(qw);

        Map<String, Map<String, Integer>> stat = new HashMap<>();
        for (Opinion o : opinions) {
            String cat = o.getClassifiedCategory() == null ? "未分类" : o.getClassifiedCategory();
            stat.computeIfAbsent(cat, k -> {
                Map<String, Integer> m = new HashMap<>();
                m.put("total", 0); m.put("support", 0); m.put("oppose", 0); m.put("neutral", 0);
                return m;
            });
            Map<String, Integer> bucket = stat.get(cat);
            bucket.merge("total", 1, Integer::sum);
            String sentiment = classifySentiment(o.getContent());
            bucket.merge(sentiment, 1, Integer::sum);
        }
        // 同步更新 opinion_category 表
        syncCategoryTable(consultationId, stat);

        Map<String, Object> result = new HashMap<>();
        result.put("totalOpinions", opinions.size());
        result.put("totalViews",    c.getTotalViews());
        result.put("byCategory",    stat);
        return Task.ok(result);
    }

    private void syncCategoryTable(Long consultationId, Map<String, Map<String, Integer>> stat) {
        QueryWrapper<OpinionCategory> qw = new QueryWrapper<>();
        qw.eq("consultation_id", consultationId);
        categoryMapper.delete(qw);
        for (Map.Entry<String, Map<String, Integer>> e : stat.entrySet()) {
            OpinionCategory cat = new OpinionCategory();
            cat.setConsultationId(consultationId);
            cat.setCategoryName(e.getKey());
            cat.setOpinionCount(e.getValue().get("total"));
            cat.setSupportCount(e.getValue().get("support"));
            cat.setOpposeCount(e.getValue().get("oppose"));
            cat.setNeutralCount(e.getValue().get("neutral"));
            categoryMapper.insert(cat);
        }
    }

    @Override
    public Task<Integer> classify(Long consultationId) {
        QueryWrapper<Opinion> qw = new QueryWrapper<>();
        qw.eq("consultation_id", consultationId)
          .and(w -> w.isNull("classified_category").or().eq("classified_category", ""));
        List<Opinion> pending = opinionMapper.selectList(qw);
        int count = 0;
        for (Opinion o : pending) {
            String cat = classifyCategory(o.getContent());
            o.setClassifiedCategory(cat);
            o.setAiCategoryConfidence(new BigDecimal("0.75"));
            opinionMapper.updateById(o);
            count++;
        }
        return Task.ok(count);
    }

    @Override
    public Task<Integer> dedup(Long consultationId) {
        QueryWrapper<Opinion> qw = new QueryWrapper<>();
        qw.eq("consultation_id", consultationId);
        List<Opinion> all = opinionMapper.selectList(qw);
        Map<String, Long> hash2Id = new HashMap<>();
        int removed = 0;
        for (Opinion o : all) {
            String hash = simHash(o.getContent());
            o.setSimilarityHash(hash);
            Long prev = hash2Id.put(hash, o.getId());
            if (prev != null) {
                // 标记为重复 → 状态置 PROCESSED + 在 content 末尾追加 [重复于#prev]
                o.setStatus(Opinion.STATUS_PROCESSED);
                o.setClassifiedCategory((o.getClassifiedCategory() == null ? "" : o.getClassifiedCategory()) + "[重复]");
                opinionMapper.updateById(o);
                removed++;
            } else {
                opinionMapper.updateById(o);
            }
        }
        return Task.ok(removed);
    }

    @Override
    public Task<List<Map<String, Object>>> wordCloud(Long consultationId, int topN) {
        if (topN <= 0 || topN > 200) topN = 50;
        QueryWrapper<Opinion> qw = new QueryWrapper<>();
        qw.eq("consultation_id", consultationId);
        List<Opinion> opinions = opinionMapper.selectList(qw);

        // 用 HanLP / HanLP-Tokenizer 简单切词过于重,这里用 stop words + 双字/三字滑窗
        java.util.Set<String> STOP = new java.util.HashSet<>(java.util.Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "也", "很", "到",
            "说", "要", "去", "你", "会", "着", "没有", "看", "好", "这", "那", "把", "它", "自己",
            "为", "但是", "因为", "所以", "如果", "或者", "以及", "可以", "应该", "应当", "需要", "建议",
            "我们", "你们", "他们", "这个", "那个", "什么", "怎么", "现在", "以前", "以后", "一次",
            "一种", "一定", "这样", "那样", "可能", "得到", "包括", "进行", "通过"
        ));
        java.util.Map<String, Integer> freq = new HashMap<>();
        for (Opinion o : opinions) {
            if (o.getContent() == null) continue;
            String text = o.getContent().replaceAll("[\\p{P}\\p{S}\\s]+", "");
            // 抽取 2~4 字短语
            for (int n = 2; n <= 4; n++) {
                for (int i = 0; i + n <= text.length(); i++) {
                    String w = text.substring(i, i + n);
                    if (STOP.contains(w)) continue;
                    if (java.util.regex.Pattern.matches(".*\\d.*", w)) continue;
                    freq.merge(w, 1, Integer::sum);
                }
            }
        }
        // 排序 + 截取 topN
        List<java.util.Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, entries.size()); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name",  entries.get(i).getKey());
            item.put("value", entries.get(i).getValue());
            out.add(item);
        }
        return Task.ok(out);
    }

    @Override
    public Task<Map<String, Object>> exportReport(Long consultationId) {
        Task<Map<String, Object>> stats = getStatistics(consultationId);
        if (!stats.isSuccess()) return stats;
        Map<String, Object> data = stats.getData();
        Consultation c = consultationMapper.selectById(consultationId);
        Map<String, Object> report = new HashMap<>();
        report.put("consultation", c);
        report.put("statistics",   data);
        report.put("generatedAt",  LocalDateTime.now());
        return Task.ok(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<OpinionReply> replyOpinion(Long opinionId, String content, Long replyBy) {
        Opinion o = opinionMapper.selectById(opinionId);
        if (o == null) return Task.error("意见不存在");
        if (content == null || content.isBlank()) return Task.error("回复内容不能为空");
        OpinionReply r = new OpinionReply();
        r.setOpinionId(opinionId);
        r.setReplyContent(content);
        r.setReplyBy(replyBy);
        r.setReplyAt(LocalDateTime.now());
        r.setNotifySent(0);
        replyMapper.insert(r);

        o.setStatus(Opinion.STATUS_REPLIED);
        o.setProcessedBy(replyBy);
        o.setProcessedAt(LocalDateTime.now());
        opinionMapper.updateById(o);

        notifyService.send(new NotifyMessage(
            null, // 提交者 ID 没存,先广播
            "SYSTEM",
            "您的意见已被回复",
            "您提交的意见已收到官方回复：" + truncate(content, 80),
            "opinion",
            opinionId,
            LocalDateTime.now()
        ));
        return Task.ok(r);
    }

    // ---------- AI 占位 ----------

    private String classifyCategory(String content) {
        if (content == null) return "其他";
        for (String[] pair : KEYWORDS) {
            for (String kw : pair) {
                if (content.contains(kw)) return pair[0];
            }
        }
        return "其他";
    }

    private String classifySentiment(String content) {
        if (content == null) return "neutral";
        if (Pattern.compile("(反对|不应|不能|不得|禁止)").matcher(content).find()) return "oppose";
        if (Pattern.compile("(支持|赞同|应该|必须|建议)").matcher(content).find()) return "support";
        return "neutral";
    }

    private String simHash(String content) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-1")
                    .digest(content.replaceAll("\\s+", "").getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "0";
        }
    }

    private String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len) + "...";
    }

    @Async
    void log(Class<?> ignored) {} // 占位: 留异步扩展位
}