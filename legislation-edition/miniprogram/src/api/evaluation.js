/**
 * 模块五：实施评估
 */
import request from "./request.js";

export const listEvaluations = (status, regulationId) =>
  request({ url: "/evaluation/list", data: { status, regulationId } });

export const createEvaluation = (body) =>
  request({ url: "/evaluation", method: "POST", data: body });

export const evaluationDetail = (id) => request({ url: `/evaluation/${id}` });

export const evaluationChart = (id) =>
  request({ url: `/evaluation/${id}/chart-data` });

export const evaluationReport = (id, format = "HTML") =>
  request({ url: `/evaluation/${id}/report`, data: { format } });

export const listIndicators = () =>
  request({ url: "/evaluation/indicator-list" });

export const syncData = (periodStart, periodEnd) =>
  request({
    url: "/evaluation/data-sync",
    method: "POST",
    data: { periodStart, periodEnd },
  });

export const compareByType = (regulationType, period) =>
  request({ url: "/evaluation/compare", data: { regulationType, period } });
