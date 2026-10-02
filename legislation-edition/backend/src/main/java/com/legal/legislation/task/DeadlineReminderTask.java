package com.legal.legislation.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.entity.LegislativeDeadline;
import com.legal.legislation.entity.LegislativeProject;
import com.legal.legislation.mapper.LegislativeDeadlineMapper;
import com.legal.legislation.mapper.LegislativeProjectMapper;
import com.legal.legislation.notify.NotifyMessage;
import com.legal.legislation.notify.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 立法项目期限定时任务
 *
 * 实现要点：
 *  - checkDeadlineReminders  : 每小时整点扫描未来 3 天内到期的期限
 *  - checkOverdueDeadlines   : 每小时 30 分扫描已逾期的期限
 *  - sendDailyTodoReminder   : 每天 9 点汇总即将到期项目
 *
 * 所有提醒通过 NotifyService 发送，便于后续接入邮件/短信/企微。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineReminderTask {

    private final LegislativeDeadlineMapper deadlineMapper;
    private final LegislativeProjectMapper projectMapper;
    private final NotifyService notifyService;

    @Scheduled(cron = "0 0 * * * ?")
    public void checkDeadlineReminders() {
        LocalDate today = LocalDate.now();
        LocalDate limit  = today.plusDays(3);
        QueryWrapper<LegislativeDeadline> qw = new QueryWrapper<>();
        qw.in("status", LegislativeDeadline.STATUS_PENDING)
          .le("deadline_date", limit)
          .isNotNull("remind_before_days");
        List<LegislativeDeadline> list = deadlineMapper.selectList(qw);
        log.info("[DeadlineReminder] 即将到期 {} 条", list.size());
        for (LegislativeDeadline d : list) {
            long daysLeft = ChronoUnit.DAYS.between(today, d.getDeadlineDate());
            NotifyMessage msg = new NotifyMessage(
                null,
                "DEADLINE",
                "立法项目期限即将到期",
                String.format("项目 #%d 的「%s」节点将于 %d 天后到期，请尽快处理。",
                    d.getProjectId(), d.getNodeName(), daysLeft),
                "legislative_project",
                d.getProjectId(),
                LocalDateTime.now()
            );
            notifyService.send(msg);

            if (d.getRemindedAt() == null) {
                d.setRemindedAt(LocalDateTime.now());
                deadlineMapper.updateById(d);
            }
        }
    }

    @Scheduled(cron = "0 30 * * * ?")
    public void checkOverdueDeadlines() {
        LocalDate today = LocalDate.now();
        QueryWrapper<LegislativeDeadline> qw = new QueryWrapper<>();
        qw.eq("status", LegislativeDeadline.STATUS_PENDING)
          .lt("deadline_date", today);
        List<LegislativeDeadline> list = deadlineMapper.selectList(qw);
        log.info("[DeadlineReminder] 逾期 {} 条", list.size());
        for (LegislativeDeadline d : list) {
            d.setStatus(LegislativeDeadline.STATUS_OVERDUE);
            deadlineMapper.updateById(d);

            NotifyMessage msg = new NotifyMessage(
                null,
                "DEADLINE",
                "立法项目期限已逾期",
                String.format("项目 #%d 的「%s」节点已逾期，请立即处理。",
                    d.getProjectId(), d.getNodeName()),
                "legislative_project",
                d.getProjectId(),
                LocalDateTime.now()
            );
            notifyService.send(msg);
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDailyTodoReminder() {
        long active = projectMapper.selectCount(new QueryWrapper<LegislativeProject>()
                .eq("status", LegislativeProject.STATUS_ACTIVE));
        log.info("[DailySummary] 当前进行中立法项目 {} 个", active);

        NotifyMessage msg = new NotifyMessage(
            null,
            "SYSTEM",
            "立法项目日报",
            String.format("当前共 %d 个进行中立法项目，请关注即将到期的项目。", active),
            "system",
            null,
            LocalDateTime.now()
        );
        notifyService.send(msg);
    }
}