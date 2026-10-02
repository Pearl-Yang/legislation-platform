# 立法版 API 速查

> 完整 API 草图见 `行政立法智能辅助平台.md` 第三章。本文件是落地版的"已实现 / 待实现"对照表。

| 模块 | 方法 | 路径 | 状态 | 备注 |
|------|------|------|------|------|
| 公共 | POST | `/auth/login` | 🟡 占位 | TODO: 接入 JWT |
| **项目** | GET  | `/legislative-project/list` | 🟢 已实现 | projectType/status 过滤 |
|        | GET  | `/legislative-project/{id}` | 🟢 已实现 | 含 stages + deadlines + progress |
|        | POST | `/legislative-project` | 🟢 已实现 | 自动初始化流程 |
|        | PUT  | `/legislative-project/{id}` | 🟡 占位 | |
|        | DELETE | `/legislative-project/{id}` | 🟡 占位 | |
|        | POST | `/legislative-project/{id}/advance` | 🟢 已实现 | 推进到下一阶段 |
|        | POST | `/legislative-project/{id}/rollback` | 🟢 已实现 | 回退到指定 stage_order |
|        | GET  | `/legislative-project/{id}/stages` | 🟢 已实现 | |
|        | GET  | `/legislative-project/{id}/current-stage` | 🟢 已实现 | |
|        | GET  | `/legislative-project/{id}/progress` | 🟢 已实现 | 0-100 |
|        | GET  | `/legislative-project/{id}/deadlines` | 🟢 已实现 | 默认 30 天内 |
|        | GET  | `/legislative-project/dashboard` | 🟢 已实现 | 4 项计数 |
|        | GET  | `/legislative-project/upcoming` | 🟡 占位 | |
|        | GET  | `/legislative-project/stage-template/list` | 🟡 占位 | |
| **草案** | POST | `/draft/generate` | 🟡 占位 | Phase 1.2 实现 |
|        | GET  | `/draft/list` | 🟡 占位 | |
| **审查** | POST | `/review/submit` | 🟡 占位 | Phase 1.3 实现 |
|        | GET  | `/review/rule-list` | 🟡 占位 | |
| **清理** | GET  | `/cleanup/task/list` | 🟡 占位 | Phase 1.4 实现 |
|        | POST | `/cleanup/task` | 🟡 占位 | |
|        | GET  | `/cleanup/task/{id}` | 🟡 占位 | |
|        | GET  | `/cleanup/task/{id}/affected-regulations` | 🟡 占位 | |
|        | POST | `/cleanup/task/{id}/suggest` | 🟡 占位 | |
|        | GET  | `/cleanup/task/{id}/report` | 🟡 占位 | |
| **法规** | GET  | `/regulation/list` | 🟡 占位 | |
|        | GET  | `/regulation/search` | 🟡 占位 | |
|        | GET  | `/regulation/{id}/relations` | 🟡 占位 | |
| **评估** | GET  | `/evaluation/list` | 🟡 占位 | Phase 1.5 实现 |
|        | POST | `/evaluation` | 🟡 占位 | |
|        | GET  | `/evaluation/{id}` | 🟡 占位 | |
|        | GET  | `/evaluation/{id}/chart-data` | 🟡 占位 | |
|        | GET  | `/evaluation/{id}/report` | 🟡 占位 | |
|        | GET  | `/evaluation/indicator-list` | 🟡 占位 | |
|        | POST | `/evaluation/data-sync` | 🟡 占位 | |
| **征集** | GET  | `/consultation/list` | 🟡 占位 | Phase 1.6 实现 |
|        | POST | `/consultation` | 🟡 占位 | |
|        | GET  | `/consultation/{id}` | 🟡 占位 | |
|        | PUT  | `/consultation/{id}` | 🟡 占位 | |
|        | POST | `/consultation/{id}/opinion` | 🟡 占位 | |
|        | GET  | `/consultation/{id}/opinions` | 🟡 占位 | |
|        | GET  | `/consultation/{id}/statistics` | 🟡 占位 | |
|        | POST | `/consultation/{id}/classify` | 🟡 占位 | |
|        | POST | `/consultation/{id}/dedup` | 🟡 占位 | |
|        | GET  | `/consultation/{id}/report` | 🟡 占位 | |
| **资料库** | GET  | `/library/material/list` | 🟡 占位 | Phase 1.7 实现 |
|        | POST | `/library/material` | 🟡 占位 | |
|        | PUT  | `/library/material/{id}` | 🟡 占位 | |
|        | DELETE | `/library/material/{id}` | 🟡 占位 | |
|        | GET  | `/library/material/search` | 🟡 占位 | |
|        | POST | `/library/material/{id}/favorite` | 🟡 占位 | |
|        | DELETE | `/library/material/{id}/favorite` | 🟡 占位 | |
|        | GET  | `/library/material/favorites` | 🟡 占位 | |
|        | POST | `/library/material/{id}/note` | 🟡 占位 | |
|        | GET  | `/library/material/{id}/notes` | 🟡 占位 | |
|        | GET  | `/library/material/{id}/related` | 🟡 占位 | |
|        | POST | `/library/material/batch-import` | 🟡 占位 | |
|        | GET  | `/library/tag/list` | 🟡 占位 | |
| **信息** | GET  | `/info/news` | 🟡 占位 | Phase 1.8 实现 |
|        | GET  | `/info/news/{id}` | 🟡 占位 | |
|        | GET  | `/info/regulation-index` | 🟡 占位 | |
|        | GET  | `/info/policy-interpretations` | 🟡 占位 | |
|        | GET  | `/info/academic-literature` | 🟡 占位 | |
|        | GET  | `/info/bulletin` | 🟡 占位 | |
|        | GET  | `/info/dashboard` | 🟡 占位 | |
|        | GET  | `/info/dashboard/chart` | 🟡 占位 | |
|        | POST | `/info/subscription` | 🟡 占位 | |
|        | DELETE | `/info/subscription/{id}` | 🟡 占位 | |
|        | GET  | `/info/subscription/list` | 🟡 占位 | |
|        | GET  | `/info/recommend` | 🟡 占位 | |

🟢 = 已实现  🟡 = Controller 路由已通，业务待补

---

## 内部接口（Service 层）

| 接口 | 实现 | 说明 |
|------|------|------|
| `LegislativeFlowService.initializeStages` | ✅ | 立项后实例化流程节点 |
| `LegislativeFlowService.advanceToNextStage` | ✅ | 推进到下一阶段 |
| `LegislativeFlowService.rollbackToStage` | ✅ | 回退到指定阶段 |
| `LegislativeFlowService.getProgressPercentage` | ✅ | 0-100 进度 |
| `LegislativeFlowService.listStages` | ✅ | 全部节点 |
| `LegislativeFlowService.getCurrentStage` | ✅ | 当前活跃节点 |
| `LegislativeFlowService.getDeadlineOverview` | ✅ | 期限概览 |
| `LegislativeProjectService` | ✅ | 项目 CRUD + dashboard |
| `RegulationGraphService.upsertRegulation` | ✅ | Neo4j 节点 upsert（内存版） |
| `RegulationGraphService.upsertRelation` | ✅ | Neo4j 关系 upsert |
| `RegulationGraphService.getSubgraph` | ✅ | N 度子图 |
| `RegulationGraphService.findImpactedRegulations` | ✅ | 受影响法规 |
| `DeadlineReminderTask` | ✅ | 定时提醒 |
