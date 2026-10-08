export function normalizeWords(payload) {
  const list = Array.isArray(payload) ? payload : payload?.words || [];
  const words = list
    .map((item) => ({
      word: String(item.name ?? item.word ?? ""),
      count: Number(item.value ?? item.weight ?? 0),
    }))
    .filter(
      (item) => item.word && Number.isFinite(item.count) && item.count > 0,
    );
  const max = Math.max(1, ...words.map((item) => item.count));
  return words.map((item) => ({ ...item, weight: item.count / max }));
}
export function reportText(report) {
  if (typeof report === "string") return report;
  if (report?.content) return String(report.content);
  const c = report?.consultation || {};
  const stats = report?.statistics || {};
  const categories = Object.entries(stats.byCategory || {}).map(
    ([name, item]) =>
      name +
      "：" +
      (item.total ?? 0) +
      " 条（支持 " +
      (item.support ?? 0) +
      "，反对 " +
      (item.oppose ?? 0) +
      "，中立 " +
      (item.neutral ?? 0) +
      "）",
  );
  return [
    c.title || "意见征集报告",
    "征集期：" + (c.startDate || "—") + " 至 " + (c.endDate || "—"),
    "意见总数：" + (stats.totalOpinions ?? "—"),
    "浏览次数：" + (stats.totalViews ?? "—"),
    "",
    "分类统计",
    ...(categories.length ? categories : ["暂无分类统计"]),
    "",
    "生成时间：" + (report?.generatedAt || "—"),
  ].join("\n");
}
