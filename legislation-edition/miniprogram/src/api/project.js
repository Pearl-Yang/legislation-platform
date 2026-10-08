/**
 * 模块一：立法项目接口
 */
import request from "./request.js";

// 列表（分页 + 类型 + 状态 过滤）
export const listProjects = (params) =>
  request({ url: "/legislative-project/list", data: params });

// 项目详情（含阶段、期限、汇总）
export const projectDetail = (id) =>
  request({ url: `/legislative-project/${id}` });

// 创建立法项目
export const createProject = (data) =>
  request({ url: "/legislative-project", method: "POST", data });

// 更新
export const updateProject = (id, data) =>
  request({ url: `/legislative-project/${id}`, method: "PUT", data });

// 逻辑删除
export const deleteProject = (id) =>
  request({ url: `/legislative-project/${id}`, method: "DELETE" });

// 推进到下一阶段
export const advanceProject = (id, remark) =>
  request({
    url: `/legislative-project/${id}/advance`,
    method: "POST",
    data: { remark },
  });

// 回退到指定阶段
export const rollbackProject = (id, targetStageOrder, remark) =>
  request({
    url: `/legislative-project/${id}/rollback`,
    method: "POST",
    data: { targetStageOrder, remark },
  });

// 流程节点
export const listStages = (id) =>
  request({ url: `/legislative-project/${id}/stages` });
export const currentStage = (id) =>
  request({ url: `/legislative-project/${id}/current-stage` });
export const projectProgress = (id) =>
  request({ url: `/legislative-project/${id}/progress` });
export const projectDeadlines = (id, daysAhead = 30) =>
  request({ url: `/legislative-project/${id}/deadlines`, data: { daysAhead } });

// 仪表盘 / 跨项目到期
export const dashboard = () =>
  request({ url: "/legislative-project/dashboard" });
export const upcomingAll = (daysAhead = 30) =>
  request({ url: "/legislative-project/upcoming", data: { daysAhead } });

// 流程模板
export const stageTemplates = (type) =>
  request({ url: "/legislative-project/stage-template/list", data: { type } });
