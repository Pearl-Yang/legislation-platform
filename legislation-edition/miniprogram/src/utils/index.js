/**
 * 小程序端通用工具函数
 */

/* =========================================================
 * 一、日期 / 数字 格式化
 * ========================================================= */

/** 日期格式化 */
export const formatDate = (d, fmt = "YYYY-MM-DD") => {
  if (!d) return "";
  const date = typeof d === "string" || typeof d === "number" ? new Date(d) : d;
  if (isNaN(date.getTime())) return "";
  const pad = (n) => (n < 10 ? "0" + n : "" + n);
  return fmt
    .replace("YYYY", date.getFullYear())
    .replace("MM", pad(date.getMonth() + 1))
    .replace("DD", pad(date.getDate()))
    .replace("HH", pad(date.getHours()))
    .replace("mm", pad(date.getMinutes()))
    .replace("ss", pad(date.getSeconds()));
};

/** 距今天数 (负数 = 已过期) */
export const daysFromNow = (d) => {
  if (!d) return null;
  const t = d instanceof Date ? d : new Date(d);
  if (isNaN(t.getTime())) return null;
  return Math.floor((t.getTime() - Date.now()) / 86400000);
};

/** 中文相对时间："3 天前 / 2 小时后 / 刚刚" */
export const relativeTime = (d) => {
  if (!d) return "";
  const t = typeof d === "string" ? new Date(d) : d;
  const sec = (t.getTime() - Date.now()) / 1000;
  const abs = Math.abs(sec);
  const future = sec > 0;
  if (abs < 60) return future ? "即将" : "刚刚";
  if (abs < 3600)
    return future
      ? `${Math.ceil(abs / 60)} 分钟后`
      : `${Math.floor(abs / 60)} 分钟前`;
  if (abs < 86400)
    return future
      ? `${Math.ceil(abs / 3600)} 小时后`
      : `${Math.floor(abs / 3600)} 小时前`;
  if (abs < 86400 * 30)
    return future
      ? `${Math.ceil(abs / 86400)} 天后`
      : `${Math.floor(abs / 86400)} 天前`;
  return formatDate(t, "YYYY-MM-DD");
};

/** 数字保留 N 位小数 */
export const num = (v, n = 2) => {
  if (v === null || v === undefined || v === "") return "—";
  const x = Number(v);
  if (isNaN(x)) return "—";
  return x.toFixed(n);
};

/** 百分比展示 */
export const percent = (v, n = 1) => {
  if (v === null || v === undefined) return "—";
  return (Number(v) * (v <= 1 ? 100 : 1)).toFixed(n) + "%";
};

/* =========================================================
 * 二、业务枚举映射（与后端对应）
 * ========================================================= */

/** 项目类型（模块一/二/四 等） */
export const PROJECT_TYPE = {
  ADMIN_REGULATION: { label: "行政法规", cls: "admin", color: "#1e5a96" },
  DEPT_RULE: { label: "部门规章", cls: "dept", color: "#4a86c5" },
  LOCAL_RULE: { label: "地方政府规章", cls: "local", color: "#39734c" },
};

/** 项目状态 */
export const PROJECT_STATUS = {
  DRAFT: { label: "草稿", color: "#64748b" },
  ACTIVE: { label: "进行中", color: "#1e5a96" },
  PUBLISHED: { label: "已发布", color: "#39734c" },
  OBSOLETE: { label: "已废止", color: "#a44342" },
};

/** 流程节点状态 */
export const STAGE_STATUS = {
  PENDING: { label: "未开始", color: "#64748b", dot: "pending" },
  IN_PROGRESS: { label: "进行中", color: "#1e5a96", dot: "in-progress" },
  DONE: { label: "已完成", color: "#39734c", dot: "done" },
  SKIPPED: { label: "已跳过", color: "#64748b", dot: "skipped" },
  RETURNED: { label: "已退回", color: "#a44342", dot: "returned" },
};

/** 审查严重度（红/黄/蓝/灰） */
export const SEVERITY = {
  RED: { label: "严重冲突", color: "#a44342", cls: "red" },
  YELLOW: { label: "建议修改", color: "#9b621f", cls: "yellow" },
  BLUE: { label: "格式提示", color: "#2b66a0", cls: "blue" },
  GREY: { label: "冗余建议", color: "#64748b", cls: "grey" },
};

/** 审查问题类型 */
export const ISSUE_TYPE = {
  SUPERIOR_CONFLICT: "与上位法冲突",
  OVER_POWER: "超越权限",
  OUTDATED_REF: "引用失效法条",
  DUPLICATE: "条文重复",
  FORMAT: "格式不规范",
  VERBOSE: "语言冗杂",
};

/** 资料类型（模块七） */
export const MATERIAL_TYPE = {
  REGULATION: "法规",
  DRAFT: "草案",
  REPORT: "评估报告",
  EXPERT_OPINION: "专家意见",
  CASE: "典型案例",
};

/** 评估维度（模块五） */
export const DIMENSION = {
  LEGALITY: { label: "合法性", color: "#1e5a96" },
  EXECUTION: { label: "落实性", color: "#39734c" },
  SATISFACTION: { label: "满意度", color: "#9b621f" },
};

/** 清理任务类型 */
export const CLEANUP_TYPE = {
  DAILY: { label: "日常清理", color: "#64748b" },
  PERIODIC: { label: "定期清理", color: "#1e5a96" },
  THEMATIC: { label: "专项清理", color: "#8e44ad" },
};

/** 清理任务状态 */
export const CLEANUP_STATUS = {
  PENDING: { label: "待执行", color: "#64748b" },
  RUNNING: { label: "进行中", color: "#1e5a96" },
  DONE: { label: "已完成", color: "#39734c" },
};

/** 清理建议（最终决定） */
export const CLEANUP_DECISION = {
  KEEP: { label: "保留", color: "#39734c" },
  MODIFY: { label: "修订", color: "#9b621f" },
  OBSOLETE: { label: "废止", color: "#a44342" },
  MERGE: { label: "合并", color: "#2b66a0" },
};

/** 征集状态 */
export const CONSULTATION_STATUS = {
  DRAFT: { label: "草稿", color: "#64748b" },
  OPEN: { label: "征集中", color: "#1e5a96" },
  CLOSED: { label: "已结束", color: "#39734c" },
};

/** 意见立场 */
export const OPINION_STANCE = {
  SUPPORT: { label: "支持", color: "#39734c" },
  OPPOSE: { label: "反对", color: "#a44342" },
  NEUTRAL: { label: "中立", color: "#64748b" },
};

/** 意见处理状态 */
export const OPINION_STATUS = {
  NEW: { label: "新提交", color: "#1e5a96" },
  PROCESSED: { label: "已处理", color: "#9b621f" },
  REPLIED: { label: "已回复", color: "#39734c" },
};

/** 评估任务状态 */
export const EVALUATION_STATUS = {
  PENDING: { label: "待执行", color: "#64748b" },
  RUNNING: { label: "进行中", color: "#1e5a96" },
  COMPLETED: { label: "已完成", color: "#39734c" },
};

/** 法规效力 */
export const REGULATION_STATUS = {
  EFFECTIVE: { label: "现行有效", color: "#39734c" },
  REVISING: { label: "修订中", color: "#9b621f" },
  OBSOLETE: { label: "已废止", color: "#a44342" },
};

/* =========================================================
 * 三、通用方法
 * ========================================================= */

/** 显示 loading */
export const showLoading = (title = "加载中…") => {
  uni.showLoading({ title, mask: true });
};
export const hideLoading = () => uni.hideLoading();

/** 安全取值（防 undefined.xx 报错） */
export const safe = (obj, ...keys) => {
  let cur = obj;
  for (const k of keys) {
    if (cur === null || cur === undefined) return undefined;
    cur = cur[k];
  }
  return cur;
};

/** 分页参数规范化（后端 page 从 1 开始） */
export const pageWrap = (page = 1, size = 20) => ({ page, size });

/** 防抖（用于搜索框） */
export const debounce = (fn, wait = 300) => {
  let timer = null;
  return function (...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), wait);
  };
};

/** 跳转 tab 页 / 普通页智能判断 */
export const goPage = (url) => {
  const TAB = [
    "/pages/dashboard/index",
    "/pages/project/index",
    "/pages/consultation/index",
    "/pages/profile/index",
  ];
  if (TAB.includes(url)) {
    uni.switchTab({ url });
  } else {
    uni.navigateTo({ url });
  }
};

// Keep legacy utility imports on the same authenticated request implementation.
export { request } from "../api/request.js";

/** 列表数据兜底（后端 MyBatis-Plus 分页对象兼容） */
export const pickList = (resp) => {
  if (!resp) return [];
  if (Array.isArray(resp)) return resp;
  if (resp.records) return resp.records;
  if (resp.list) return resp.list;
  if (resp.data && Array.isArray(resp.data)) return resp.data;
  return [];
};

/** 列表分页兜底 */
export const pickPager = (resp) => {
  if (!resp) return { total: 0, size: 0, list: [] };
  if (Array.isArray(resp))
    return { total: resp.length, list: resp, size: resp.length };
  return {
    total: Number(resp.total ?? resp.totalCount ?? 0),
    size: Number(resp.size ?? 20),
    list: pickList(resp),
  };
};

/** 文本高亮（在 0.1.0 用作搜索结果摘要） */
export const highlight = (text, kw) => {
  if (!text || !kw) return text || "";
  const idx = text.indexOf(kw);
  if (idx < 0) return text;
  return (
    text.slice(0, idx) +
    `<text style="color:#a44342;font-weight:600">${kw}</text>` +
    text.slice(idx + kw.length)
  );
};

/** 文本截断 */
export const ellipsis = (s, n = 80) => {
  if (!s) return "";
  return s.length > n ? s.slice(0, n) + "…" : s;
};

/** 复制到剪贴板 */
export const copy = (text) => {
  uni.setClipboardData({
    data: String(text || ""),
    success: () => uni.showToast({ title: "已复制", icon: "none" }),
  });
};
