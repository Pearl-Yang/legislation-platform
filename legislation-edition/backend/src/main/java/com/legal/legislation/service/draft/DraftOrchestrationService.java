package com.legal.legislation.service.draft;

import com.legal.legislation.entity.Regulation;
import com.legal.legislation.mapper.RegulationMapper;
import com.legal.legislation.service.draft.SuperiorLawParser.ParsedItem;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 草案生成编排服务(P1 增强版,无 LLM 也能跑通)。
 *
 * 流程:
 *   1) 拿上位法全文(从 regulation 表)
 *   2) SuperiorLawParser 切条款 + 识别"待细化事项"
 *   3) 从 regulation 表"按地区 / 类型"匹配候选"异地参考规章"片段
 *   4) 把 (事项清单 + 异地参考片段 + 提示词) 灌进 prompt
 *   5) 调 Qwen 生成(若 AI_DRAFT_ENABLED=false,则用模板占位)
 *   6) 落库到 legislative_draft
 */
@Service
@RequiredArgsConstructor
public class DraftOrchestrationService {

    private final SuperiorLawParser parser;
    private final RegulationMapper regulationMapper;
    private final com.legal.legislation.service.DraftService draftService;

    public Task<Map<String, Object>> orchestrate(Long projectId, Long sourceRegulationId,
                                                 String prompt, Long operatorId) {
        Regulation src = regulationMapper.selectById(sourceRegulationId);
        if (src == null) {
            return Task.error("上位法不存在: id=" + sourceRegulationId);
        }

        // 1) 解析上位法事项
        List<ParsedItem> items = parser.parse(src.getFullText(), src.getRegulationName());

        // 2) 检索异地参考规章(同 regulation_type、不同 region_code,全文包含 source 名称的关键词)
        List<Regulation> similar = regulationMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Regulation>()
                .eq("regulation_type", src.getRegulationType())
                .ne("id", sourceRegulationId)
                .ne("region_code", src.getRegionCode() == null ? "" : src.getRegionCode())
                .last("LIMIT 5")
        );

        // 3) 拼 prompt
        String composedPrompt = composePrompt(items, similar, prompt, src.getRegulationName());

        // 4) 调 DraftService 触发生成(异步)
        //    DraftService 内部会按 ai.enabled 决定模板占位还是 LLM
        Task<String> task = draftService.submitGenerateTask(projectId, null, composedPrompt, operatorId);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("similarRegulations", similar);
        result.put("composedPrompt", composedPrompt);
        result.put("taskId", task.getData());
        return Task.ok(result);
    }

    private String composePrompt(List<ParsedItem> items, List<Regulation> similar,
                                 String userPrompt, String sourceName) {
        StringBuilder sb = new StringBuilder();
        sb.append("上位法:").append(sourceName).append("\n\n");
        sb.append("=== 待细化事项清单 ===\n");
        for (ParsedItem it : items) {
            sb.append("- ").append(it.articleNo).append(" [").append(it.type).append("] ")
              .append(it.reason).append("\n  原文: ").append(truncate(it.articleText, 80)).append("\n");
        }
        sb.append("\n=== 异地参考规章(节选) ===\n");
        for (Regulation r : similar) {
            sb.append("- ").append(r.getRegulationName())
              .append(" (").append(r.getRegionCode() == null ? "" : r.getRegionCode()).append(")\n")
              .append("  摘要: ").append(truncate(r.getDigest(), 100)).append("\n");
        }
        if (userPrompt != null && !userPrompt.isBlank()) {
            sb.append("\n=== 起草者要求 ===\n").append(userPrompt).append("\n");
        }
        sb.append("\n请按以上上位法要求 + 异地参考经验 + 起草者要求,生成本地实施细则草案。\n");
        return sb.toString();
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}
