/**
 * 模块四：智能清理 + 法规主数据
 */
import request from "./request.js";

// ====== 清理任务 ======
export const listCleanupTasks = (status, taskType) =>
  request({ url: "/cleanup/task/list", data: { status, taskType } });

export const cleanupDetail = (id) => request({ url: `/cleanup/task/${id}` });

export const createCleanupTask = (body) =>
  request({ url: "/cleanup/task", method: "POST", data: body });

export const affectedRegulations = (id) =>
  request({ url: `/cleanup/task/${id}/affected-regulations` });

export const suggestCleanup = (id) =>
  request({ url: `/cleanup/task/${id}/suggest`, method: "POST" });

export const cleanupReport = (id) =>
  request({ url: `/cleanup/task/${id}/report` });

export const decideSuggestion = (id, finalDecision, remark) =>
  request({
    url: `/cleanup/suggestion/${id}/decide`,
    method: "POST",
    data: { finalDecision, remark },
  });

// ====== 法规主数据 ======
export const listRegulations = (params) =>
  request({ url: "/regulation/list", data: params });

export const searchRegulations = (keyword, page = 1, size = 20) =>
  request({ url: "/regulation/search", data: { keyword, page, size } });

export const regulationDetail = (id) => request({ url: `/regulation/${id}` });

export const regulationRelations = (id, depth = 2) =>
  request({ url: `/regulation/${id}/relations`, data: { depth } });

export const upsertRegulation = (body) =>
  request({ url: "/regulation", method: "POST", data: body });
