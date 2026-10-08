export function normalizeApiUrl(value) {
  const url = value.trim().replace(/\/$/, "");
  if (!/^https?:\/\/[^\s?#]+\/api$/.test(url))
    throw new Error("请填写以 /api 结尾的完整地址");
  return url;
}
