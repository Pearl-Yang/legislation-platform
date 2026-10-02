package com.legal.legislation.service.review;

import com.legal.legislation.entity.Regulation;
import com.legal.legislation.mapper.RegulationMapper;
import com.legal.legislation.service.review.ReviewRule.MatchResult;
import com.legal.legislation.service.review.ReviewRule.RuleContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审查引擎:执行所有 ReviewRule,返回合并后的问题列表。
 *
 * 输入: 草案全文 + 关联的上位法 ID(可空) + 项目类型
 * 输出: 6 类问题(去重后),每类带严重程度 / 位置 / 建议
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEngine {

    private final List<ReviewRule> rules;
    private final RegulationMapper regulationMapper;

    public List<MatchResult> run(String draftContent, Long superiorLawId) {
        if (draftContent == null || draftContent.isBlank()) {
            return List.of();
        }

        // 准备上下文:上位法(用于 CONFLICT / OUTDATED_REF) + 异地参考(用于 DUPLICATE)
        Map<String, Object> superiorLaws = new HashMap<>();
        Map<String, String> statusMap   = new HashMap<>();   // 名字 -> 状态
        if (superiorLawId != null) {
            Regulation sup = regulationMapper.selectById(superiorLawId);
            if (sup != null) {
                superiorLaws.put(String.valueOf(sup.getId()), sup.getFullText());
                statusMap.put(sup.getRegulationName(), sup.getStatus());
            }
        }
        // 同 regulation_type 其它规章,作为"参照库"
        List<Regulation> similar = regulationMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Regulation>()
                .ne("id", superiorLawId == null ? -1 : superiorLawId)
                .isNotNull("full_text")
                .last("LIMIT 50")
        );
        Map<String, Object> localRules = new HashMap<>();
        for (Regulation r : similar) {
            if (r.getFullText() != null && !r.getFullText().isBlank()) {
                localRules.put(String.valueOf(r.getId()), r.getFullText());
            }
        }

        RuleContext ctx = new RuleContext(draftContent, merge(superiorLaws, statusMap), localRules);

        List<MatchResult> all = new ArrayList<>();
        for (ReviewRule r : rules) {
            try {
                all.addAll(r.apply(ctx));
            } catch (Exception e) {
                log.warn("[ReviewEngine] 规则 {} 执行失败: {}", r.code(), e.getMessage());
            }
        }
        log.info("[ReviewEngine] 草案 {} 字,命中 {} 条问题", draftContent.length(), all.size());
        return all;
    }

    /** 把 statusMap 合并进 superiorLaws(作为键值对),OutdatedReferenceRule 实际读 name->status */
    @SuppressWarnings("unchecked")
    private Map<String, Object> merge(Map<String, Object> laws, Map<String, String> status) {
        Map<String, Object> merged = new HashMap<>(laws);
        for (var e : status.entrySet()) {
            merged.put("STATUS::" + e.getKey(), e.getValue());
        }
        return merged;
    }
}
