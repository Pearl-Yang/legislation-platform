package com.legal.legislation.service.cleanup;

import com.legal.legislation.entity.CleanupSuggestion;
import com.legal.legislation.entity.Regulation;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 清理建议生成器(规则引擎版)。
 *
 * 输入:一组受影响的法规(regulation 列表)
 * 输出:每条的"建议处置(KEEP/MODIFY/OBSOLETE)" + 理由 + 置信度
 *
 * 决策矩阵(简化版):
 *   - 上位法已 ABOLISHED      -> OBSOLETE
 *   - status = OBSOLETE        -> OBSOLETE
 *   - 上位法被 REVISED         -> MODIFY
 *   - 距生效日 > 5 年         -> KEEP(需人工看是否续期)
 *   - 其它                    -> KEEP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupAdvisor {

    public Task<List<CleanupSuggestion>> suggest(List<Regulation> affected,
                                                Long triggerRegulationId,
                                                String triggerChangeType) {
        List<CleanupSuggestion> out = new ArrayList<>();
        for (Regulation r : affected) {
            CleanupSuggestion s = new CleanupSuggestion();
            s.setRegulationId(r.getId());
            s.setAiConfidence(new BigDecimal("0.75"));

            // 决策
            String suggestion;
            String reason;
            if (Regulation.STATUS_OBSOLETE.equalsIgnoreCase(r.getStatus())) {
                suggestion = CleanupSuggestion.SUG_OBSOLETE;
                reason    = "法规当前状态已是废止,清理任务直接确认";
            } else if (triggerChangeType != null && "REVISED".equalsIgnoreCase(triggerChangeType)
                       && triggerRegulationId != null && triggerRegulationId.equals(r.getId())) {
                // 自身被修订(用于子任务)
                suggestion = CleanupSuggestion.SUG_MODIFY;
                reason    = "本法规被标记为已修订,需配套修改";
            } else if (r.getEffectiveDate() != null
                       && r.getEffectiveDate().isBefore(LocalDate.now().minusYears(5))) {
                suggestion = CleanupSuggestion.SUG_KEEP;
                reason    = "已生效满 5 年,建议保留并启动定期评估;若已不适应现实,改判为 MODIFY/OBSOLETE";
            } else {
                suggestion = CleanupSuggestion.SUG_KEEP;
                reason    = "现行有效,建议继续保留";
            }

            s.setSuggestion(suggestion);
            s.setReason(reason);
            out.add(s);
        }
        log.info("[CleanupAdvisor] 生成 {} 条清理建议(触发: changeType={}, regulationId={})",
            out.size(), triggerChangeType, triggerRegulationId);
        return Task.ok(out);
    }
}
