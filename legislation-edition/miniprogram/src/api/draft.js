/**
 * 模块二：草案生成接口
 */
import request from "./request.js";

// 提交生成任务（异步），返回 taskId
export const submitGenerate = (body) =>
  request({ url: "/draft/generate", method: "POST", data: body });

// 轮询任务状态
export const taskStatus = (taskId) => request({ url: `/draft/task/${taskId}` });

// 某项目下的所有草案
export const listDrafts = (projectId) =>
  request({ url: "/draft/list", data: { projectId } });

// 草案详情
export const draftDetail = (id) => request({ url: `/draft/${id}` });

// 人工修订（提交后会写历史版本）
export const reviseDraft = (id, body) =>
  request({ url: `/draft/${id}/revise`, method: "POST", data: body });

// 历史版本
export const draftVersionList = (id) =>
  request({ url: `/draft/${id}/versions` });

// 导出
export const exportDraft = (id, format = "MARKDOWN") =>
  request({ url: `/draft/${id}/export`, data: { format } });
