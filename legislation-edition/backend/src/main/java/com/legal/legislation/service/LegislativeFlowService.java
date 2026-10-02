package com.legal.legislation.service;

import com.legal.legislation.entity.LegislativeStage;
import com.legal.legislation.entity.LegislativeDeadline;

import java.util.List;

/**
 * 立法流程服务接口
 *
 * 设计要点（继承自 CaseFlowService 的概念）：
 *  - 立项时，根据 project.project_type 拉 legislative_stage_template，
 *    实例化为 legislative_stage 行 + legislative_deadline 行；
 *  - 推进到下一节点时，把当前节点标 DONE、推进 deadline；
 *  - 退回时回退到指定节点。
 */
public interface LegislativeFlowService {

    /**
     * 立项后初始化流程节点 + 期限模板。
     * 通常在创建 LegislativeProject 后的同一事务中调用。
     */
    void initializeStages(Long projectId, String projectType);

    /**
     * 推进到下一阶段。
     * 返回推进后的节点 id；如果已经是最后阶段则返回 null。
     */
    Long advanceToNextStage(Long projectId, Long operatorId, String remark);

    /**
     * 回退到指定 stageOrder。
     */
    void rollbackToStage(Long projectId, Integer targetStageOrder, Long operatorId, String remark);

    /**
     * 计算项目完成进度百分比 0..100。
     */
    int getProgressPercentage(Long projectId);

    /**
     * 查询项目的所有节点（按 stageOrder 升序）。
     */
    List<LegislativeStage> listStages(Long projectId);

    /**
     * 查询当前活跃节点。
     */
    LegislativeStage getCurrentStage(Long projectId);

    /**
     * 查询项目即将到期 / 已逾期的期限节点。
     */
    List<LegislativeDeadline> getDeadlineOverview(Long projectId, int daysAhead);

    /**
     * 跨项目查询即将到期 / 已逾期的期限节点,用于仪表盘汇总。
     */
    List<LegislativeDeadline> getUpcomingAcrossProjects(int daysAhead);
}