/**
 * 模块三：智慧审查接口
 */
import request from "./request.js";

// 提交审查任务
export const submitReview = (draftId, reviewType = "AUTO") =>
  request({
    url: "/review/submit",
    method: "POST",
    data: { draftId, reviewType },
  });

// 审查结果（含问题列表）
export const reviewRecord = (id) => request({ url: `/review/record/${id}` });

// 审查规则
export const listRules = (ruleType, severity) =>
  request({ url: "/review/rule-list", data: { ruleType, severity } });

export const updateRule = (id, body) =>
  request({ url: `/review/rule/${id}`, method: "PUT", data: body });

export const resolveIssue = (id) =>
  request({ url: `/review/issue/${id}/resolve`, method: "POST" });

// 批量审查
export const batchSubmit = (draftIds) =>
  request({ url: "/review/batch-submit", method: "POST", data: draftIds });
