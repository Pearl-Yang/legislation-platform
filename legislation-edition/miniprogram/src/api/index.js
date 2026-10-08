/**
 * 统一对外：所有业务模块的接口聚合
 */
export * as projectApi from "./project.js";
export * as draftApi from "./draft.js";
export * as reviewApi from "./review.js";
export * as cleanupApi from "./cleanup.js";
export * as evaluationApi from "./evaluation.js";
export * as consultationApi from "./consultation.js";
export * as libraryApi from "./library.js";
export * as infoApi from "./info.js";
export * as authApi from "./auth.js";

import { BASE_URL } from "./request.js";
export { BASE_URL };
