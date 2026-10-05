# 行政立法智能辅助平台 — 答辩 PPT(模板版)

> **生成说明**:本文档是 16 页 PPT 的内容模板,C 可用以下任一方式生成 `.pptx` 文件:
> 1. **手动**:PowerPoint / Keynote 打开 PPT 内容(每页"标题+讲稿要点+配图说明"),套模板出图
> 2. **脚本**:用 `python-pptx` 自动生成,见附录脚本
> 3. **AI 工具**:粘贴本文档到 Gamma.app / 美图 AI PPT / 通义听悟,自动生成 PPT
>
> **配套讲稿**:`docs/答辩版讲稿.md`(每页 5-10 句口播稿,25 分钟)
> **配套演示脚本**:`docs/demo-voc.json`(1.2 万字 JSON 化演示步骤)
> **建议配色**:主色 `#4f46e5`(靛蓝)+ 辅色 `#10b981`(绿)+ `#f59e0b`(橙)+ `#f43f5e`(红)

---

## Slide 1:封面页

```
[主标题] 智立法 · 行政立法智能辅助平台
[副标题] 行政立法的全流程智能化解决方案
[团队]   智立法团队 (A · B · C)
[日期]   2026-10-12
[底部]   项目代号:legislation-platform v0.2.0
```

**配图建议**:Logo + 立法场景插画(罗马柱 + 数字)

---

## Slide 2:背景 — 立法痛点

```
[小标题] 背景 — 立法现状与痛点

[数据卡 1] 5 部   法律
[数据卡 2] 30 部  行政法规
[数据卡 3] 8000 部 地方规章
[数据卡 4] 800+ 部 年新增地方规章

[痛点 1] 效率低 — 立项到公布 18 个月
[痛点 2] 风险高 — 越权/引用冲突靠人工查
[痛点 3] 参与弱 — 公众意见碎片化

[目标] 18 个月 → 9-12 个月,人工 → AI,碎片化 → 结构化
```

**配图建议**:3 个痛点配示意图(时钟 / 漏洞 / 拼图)

---

## Slide 3:架构图

```
[小标题] 整体架构 — 4 层分层设计

┌─────────────────────────────────┐
│  接入层  Vue 3 Web + 小程序 + H5  │
├─────────────────────────────────┤
│  应用层  Spring Boot 3.1 + 14C   │
│          + 9S + Qwen AI          │
├─────────────────────────────────┤
│  数据层  MySQL 24表 + Neo4j + Redis │
├─────────────────────────────────┤
│  可观测  Prometheus + Grafana + ELK │
└─────────────────────────────────┘

[技术亮点]
- Jwt 鉴权 + Spring Security
- @Async + ThreadPoolTaskExecutor
- @RestControllerAdvice 全局异常
- SpringDoc + Knife4j OpenAPI
- Qwen 在线/离线双通道
```

**配图建议**:4 层架构图(可手绘或用 draw.io)

---

## Slide 4:8 大模块总览

```
[小标题] 8 大模块 — 立法全周期覆盖

| 编号 | 模块     | 核心功能        | 核心接口                |
|-----|---------|---------------|----------------------|
|  1  | 立法项目 | 立项/推进/回退 | /legislative-project/* |
|  2  | 草案生成 | AI 模板+RAG    | /draft/*             |
|  3  | 智慧审查 | 6 类规则引擎    | /review/*            |
|  4  | 清理任务 | 3 种触发模式    | /cleanup/*           |
|  5  | 实施评估 | 三维评分        | /evaluation/*        |
|  6  | 意见征集 | 多渠道汇总      | /consultation/*      |
|  7  | 资料库   | 法规/草案/报告  | /library/*           |
|  8  | 信息展示 | Dashboard 大屏  | /info/*              |
```

---

## Slide 5:演示 1 — 项目流程端到端

```
[小标题] 演示 1 — 项目流程端到端

[步骤列表]
1. 创建「XX 智慧城市管理条例」→ 自动初始化 10 节点
2. 进入详情 → 顶部 7 天预警徽章
3. ECharts 流程图:当前节点蓝色高亮
4. 流程明细:步骤条 + 法定期限 + 操作时间
5. 推进 3 次 → 进度 55% → 88%
6. 回退到第 5 阶段 → 演示操作回退

[讲点] 基于《行政法规制定程序条例》配置 10 节点模板
[配图] 浏览器截图:流程图可视化
```

**配图**:截图 `views/project/Detail.vue` 中 ECharts 流程图

---

## Slide 6:演示 2 — 草案生成

```
[小标题] 演示 2 — 草案生成(异步任务)

[步骤列表]
1. 详情页 → 前往生成
2. 选项目 + 填上位法条款 + 细化事项
3. 开始生成 → 拿到 taskId → 异步后台跑
4. 1.5 秒轮询 → 进度条 5% → 100%
5. 显示 6 章 Markdown + 参考片段
6. 导出 Markdown / Word

[讲点] AI 生成 + 异步任务 + 轮询 + 版本快照
[配图] 截图 Generate.vue 的 SUCCESS 视图
```

**配图**:截图生成结果 + 6 章占位文本示例

---

## Slide 7:演示 3 — 智慧审查

```
[小标题] 演示 3 — 智慧审查(6 类规则)

[4 级问题卡]
┌─────────────┬─────────────┐
│ 红 严重冲突  2           │ 红 越权立法  1       │
├─────────────┼─────────────┤
│ 黄 需关注    3           │ 蓝 格式建议  1       │
└─────────────┴─────────────┘

[规则清单]
- R001 上位法冲突(红)
- R002 违规越权(红)
- R003 引用失效(黄)
- R004 条文重复(黄)
- R005 格式不规范(蓝)
- R006 语言冗杂(灰)
```

**配图**:截图 Submit.vue 的 4 级问题统计 + 问题列表

---

## Slide 8:演示 4 — 清理 + 评估

```
[小标题] 演示 4 — 清理任务 + 实施评估

[清理任务]
- 2026Q4 规章集中清理 → 124 候选法规
- 建议: KEEP 87 / MODIFY 24 / OBSOLETE 13
- 决策: 保留/修改/废止 三态

[评估任务]
- 三维评分: 合法性 30% + 落实性 40% + 满意度 30%
- 综合分 = Σ(维度 × 权重)
- 雷达图 + 折线 + 报告
```

**配图**:截图清理 + 评估报告抽屉

---

## Slide 9:演示 5 — 意见征集

```
[小标题] 演示 5 — 意见征集(端到端)

[5 渠道]
Web 后台 | H5 公众页 | 微信小程序 | 政务 APP | 座谈会

[AI 能力]
- 自动归类: 合法性/合理性/可行性/操作性/其他
- SimHash 去重: 80% 自动合并
- 立场识别: 支持/反对/建议/中立
- 词云 + 统计 + 报告
```

**配图**:截图 List.vue 的抽屉 + 词云 + 分类统计

---

## Slide 10:演示 6 — Dashboard 大屏

```
[小标题] 演示 6 — Dashboard 大屏

[4 行布局]
第 1 行: 4 KPI(法规/项目/草案/资料+意见)
第 2 行: 趋势 + 类型 + AI + 意见热度
第 3 行: 地区 + 状态 + 即将到期
第 4 行: 时钟 + 30 秒轮播

[数字]
8 个真实接口 / 11 个图表 / 30 秒轮播
```

**配图**:截图 Dashboard.vue 全屏

---

## Slide 11:工程化

```
[小标题] 工程化交付

[Docker 5 容器]
MySQL 8 + Neo4j 5 + Backend(Spring Boot 3.1) + Prometheus + Grafana

[可观测]
- Prometheus 8 类业务指标
- Grafana 大盘
- ELK 风格 JSON 日志
- 集成测试 20 用例
- 冒烟脚本 36 步

[演示] 打开 Prometheus → 抓取 backend /actuator/prometheus
       打开 Grafana → 立法平台大盘
```

**配图**:截图 Prometheus + Grafana 大盘

---

## Slide 12:AI 能力

```
[小标题] AI 能力 — Qwen 双通道

```java
String content = qwenFacade.execute(
    userPrompt,
    () -> buildPlaceholderContent(...)  // 离线兜底
);
```

[4 场景]
1. 草案生成: 模板占位 + RAG 预留
2. 智慧审查: 6 类规则 + 关键词命中
3. 清理建议: 置信度评分
4. 意见归类: SimHash + 立场识别

[降级机制] 在线失败 → 自动降级离线
```

---

## Slide 13:技术亮点清单

```
[小标题] 技术亮点(7 维度)

| 维度    | 亮点                                       |
|--------|-------------------------------------------|
| 后端    | Spring Boot 3.1 / MyBatis-Plus / Security |
| AI     | Qwen 双通道 + 模板兜底                      |
| 数据    | MySQL 24 表 + Neo4j + Redis              |
| 可观测  | Prometheus + JSON + Grafana              |
| 前端    | Vue 3 + Element Plus + ECharts + Pinia   |
| 工程    | Docker 5 容器 + 集成测试 20 + 冒烟 36    |
| 文档    | ROADMAP + API + TEAM + 演示 1.2 万字     |
```

---

## Slide 14:可扩展性

```
[小标题] 可扩展性(4 个扩展点)

1. 多租户: 基于 tenant_id 字段
2. 多 LLM: QwenFacade.execute 支持换 DeepSeek / 文心 / GPT
3. 知识图谱: RegulationCorpusService 接入 Neo4j 后做上位法全文比对
4. 小程序: miniprogram/ 已预留,扩展到政务 App / 派单 App
```

---

## Slide 15:风险与应对

```
[小标题] 风险与应对

| 风险          | 应对                                |
|--------------|------------------------------------|
| Neo4j 启不来 | NEO4J_ENABLED=false 降级到内存版     |
| Qwen API 限流 | 切回 offline mode + 文案说明        |
| MySQL 表不全 | 已补 supplement_public.sql          |
| Node 装不上  | npm install --legacy-peer-deps      |
```

---

## Slide 16:结束页

```
[主标题] 谢谢聆听
[副标题] 智立法 · 行政立法智能辅助平台
[底部]   智立法团队 · 2026-10-12

[联系] GitHub / 内部 GitLab
[版本] v0.2.0
```

---

## 附录:用 python-pptx 自动生成脚本

```python
# scripts/generate_pptx.py
# 用法: pip install python-pptx --break-system-packages
#       python scripts/generate_pptx.py

from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor

prs = Presentation()
prs.slide_width  = Inches(13.33)
prs.slide_height = Inches(7.5)

# 主色
PRIMARY = RGBColor(0x4f, 0x46, 0xe5)
SUCCESS = RGBColor(0x10, 0xb9, 0x81)
WARN    = RGBColor(0xf5, 0x9e, 0x0b)
DANGER  = RGBColor(0xf4, 0x3f, 0x5e)
TEXT    = RGBColor(0x1e, 0x29, 0x3b)

def add_title_slide (title, subtitle, footer='智立法团队 · 2026-10-12'):
    layout = prs.slide_layouts[6]  # blank
    slide = prs.slides.add_slide(layout)
    tb = slide.shapes.add_textbox(Inches(1), Inches(2), Inches(11.33), Inches(1.5))
    tf = tb.text_frame
    tf.text = title
    p = tf.paragraphs[0]
    p.font.size = Pt(40); p.font.bold = True; p.font.color.rgb = PRIMARY
    sb = slide.shapes.add_textbox(Inches(1), Inches(4), Inches(11.33), Inches(1))
    sb.text_frame.text = subtitle
    sb.text_frame.paragraphs[0].font.size = Pt(20)
    sb.text_frame.paragraphs[0].font.color.rgb = TEXT
    fb = slide.shapes.add_textbox(Inches(1), Inches(7), Inches(11.33), Inches(0.5))
    fb.text_frame.text = footer
    fb.text_frame.paragraphs[0].font.size = Pt(12)
    fb.text_frame.paragraphs[0].font.color.rgb = TEXT
    return slide

def add_content_slide (title, bullets, key_numbers=None):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    tb = slide.shapes.add_textbox(Inches(0.5), Inches(0.5), Inches(12), Inches(1))
    tb.text_frame.text = title
    tb.text_frame.paragraphs[0].font.size = Pt(28)
    tb.text_frame.paragraphs[0].font.bold = True
    tb.text_frame.paragraphs[0].font.color.rgb = PRIMARY
    bb = slide.shapes.add_textbox(Inches(0.5), Inches(2), Inches(12), Inches(5))
    tf = bb.text_frame
    tf.word_wrap = True
    for i, b in enumerate(bullets):
        if i == 0:
            tf.text = '• ' + b
        else:
            p = tf.add_paragraph(); p.text = '• ' + b
        p.font.size = Pt(18); p.font.color.rgb = TEXT
    return slide

# === Slide 1:封面 ===
add_title_slide(
    '智立法 · 行政立法智能辅助平台',
    '行政立法的全流程智能化解决方案'
)

# === Slide 2:背景 ===
add_content_slide(
    '背景 — 立法现状与痛点',
    [
        '5 部法律 / 30 部行政法规 / 8000 部地方规章',
        '每年新增 800+ 部,立法流程平均 18 个月',
        '痛点 1:效率低 — 流程节点 10+ 个,人工推进慢',
        '痛点 2:风险高 — 越权/引用冲突靠人工查',
        '痛点 3:参与弱 — 公众意见碎片化',
        '目标:18 月 → 9 月,人工 → AI,碎片化 → 结构化'
    ]
)

# === Slide 3:架构 ===
add_content_slide(
    '整体架构 — 4 层分层设计',
    [
        '接入层:Vue 3 Web + 小程序 + H5',
        '应用层:Spring Boot 3.1 + 14 Controller + 9 Service + Qwen',
        '数据层:MySQL 24 表 + Neo4j + Redis',
        '可观测:Prometheus + Grafana + ELK JSON',
        '亮点:Jwt / @Async / @RestControllerAdvice / SpringDoc'
    ]
)

# === Slide 4:8 模块 ===
add_content_slide(
    '8 大模块 — 立法全周期',
    [
        '1. 立法项目 — 立项/推进/回退(10 节点)',
        '2. 草案生成 — AI 模板 + 上位法 RAG',
        '3. 智慧审查 — 6 类规则引擎(红黄蓝灰)',
        '4. 清理任务 — DAILY / PERIODIC / THEMATIC',
        '5. 实施评估 — 合法性 / 落实性 / 满意度 三维',
        '6. 意见征集 — 多渠道 + AI 归类',
        '7. 资料库 — 法规/草案/报告',
        '8. 信息展示 — Dashboard 大屏'
    ]
)

# ... Slides 5-15 同模式 ...

# === Slide 16:结束 ===
add_title_slide(
    '谢谢聆听',
    '智立法 · 行政立法智能辅助平台'
)

prs.save('行政立法智能辅助平台-答辩版.pptx')
print('✅ PPT 已生成:行政立法智能辅助平台-答辩版.pptx')
```

---

## 附录:也可使用 AI 工具生成

| 工具 | 用法 | 适合 |
|---|---|---|
| **Gamma.app** | 粘贴本文档 → 自动生成 → 选模板 | 快速出图 |
| **美图 AI PPT** | 粘贴讲稿 → 一键美化 | 中文模板多 |
| **讯飞智文** | 粘贴内容 → 自动生成大纲 | 中文报告 |
| **Beautiful.ai** | 粘贴要点 → 选择行业模板 | 商务风格 |
| **Slidev** | Markdown → PPT | 工程师风格 |

---

**总页数**:16 页
**总时长**:25 分钟(20 分钟演示 + 5 分钟问答)
**讲稿配套**:`docs/答辩版讲稿.md`
**演示配套**:(`docs/demo-voc.json`)