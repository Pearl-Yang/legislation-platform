# -*- coding: utf-8 -*-
"""
智立法 · 行政立法智能辅助平台 - 答辩 PPT 生成脚本
20 页金奖加强版 · 科技靛蓝风格
"""

from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.oxml.ns import qn
from copy import deepcopy
from lxml import etree

# ========== 配色方案（科技靛蓝） ==========
PRIMARY = RGBColor(0x4f, 0x46, 0xe5)         # 主色 靛蓝
PRIMARY_LIGHT = RGBColor(0x81, 0x8c, 0xf8)   # 浅靛蓝
PRIMARY_DARK = RGBColor(0x37, 0x30, 0xa3)    # 深靛蓝
SUCCESS = RGBColor(0x10, 0xb9, 0x81)         # 成功绿
SUCCESS_LIGHT = RGBColor(0x34, 0xd3, 0x99)
WARNING = RGBColor(0xf5, 0x9e, 0x0b)         # 警告橙
DANGER = RGBColor(0xf4, 0x3f, 0x5e)          # 危险红
INFO = RGBColor(0x06, 0xb6, 0xd4)            # 信息青
DARK = RGBColor(0x0f, 0x17, 0x2a)            # 深色
GRAY_900 = RGBColor(0x1e, 0x29, 0x3b)
GRAY_700 = RGBColor(0x33, 0x41, 0x55)
GRAY_500 = RGBColor(0x64, 0x74, 0x8b)
GRAY_300 = RGBColor(0xcb, 0xd5, 0xe1)
GRAY_100 = RGBColor(0xf1, 0xf5, 0xf9)
GRAY_50 = RGBColor(0xf8, 0xfa, 0xfc)
WHITE = RGBColor(0xff, 0xff, 0xff)
GOLD = RGBColor(0xfb, 0xbf, 0x24)
PURPLE = RGBColor(0x8b, 0x5c, 0xf6)
PINK = RGBColor(0xec, 0x48, 0x99)

# 渐变色 (备用)
GRADIENT_PRIMARY = [PRIMARY, RGBColor(0x7c, 0x3a, 0xed), RGBColor(0x25, 0x63, 0xeb)]
GRADIENT_SUCCESS = [SUCCESS, RGBColor(0x05, 0x96, 0x69)]
GRADIENT_WARM = [WARNING, DANGER]


# ========== 初始化 ==========
prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
SLIDE_W = prs.slide_width
SLIDE_H = prs.slide_height

blank_layout = prs.slide_layouts[6]


# ========== 工具函数 ==========
def add_rect(slide, x, y, w, h, fill_color, line_color=None, shape_type=MSO_SHAPE.RECTANGLE):
    shp = slide.shapes.add_shape(shape_type, x, y, w, h)
    shp.fill.solid()
    shp.fill.fore_color.rgb = fill_color
    if line_color is None:
        shp.line.fill.background()
    else:
        shp.line.color.rgb = line_color
        shp.line.width = Pt(0.5)
    shp.shadow.inherit = False
    return shp


def add_text(slide, x, y, w, h, text, size=14, bold=False, color=GRAY_900,
             align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, font_name='Microsoft YaHei'):
    tb = slide.shapes.add_textbox(x, y, w, h)
    tf = tb.text_frame
    tf.margin_left = Inches(0.05)
    tf.margin_right = Inches(0.05)
    tf.margin_top = Inches(0.02)
    tf.margin_bottom = Inches(0.02)
    tf.word_wrap = True
    tf.vertical_anchor = anchor

    if not text:
        return tb

    lines = text.split('\n') if isinstance(text, str) else text
    first_set = False
    for line in lines:
        if not line:
            continue
        if not first_set:
            p = tf.paragraphs[0]
            first_set = True
        else:
            p = tf.add_paragraph()
        p.alignment = align
        p.text = line
        run = p.runs[0]
        run.font.size = Pt(size)
        run.font.bold = bold
        run.font.color.rgb = color
        run.font.name = font_name
        # 设置中文字体
        rPr = run._r.get_or_add_rPr()
        eastAsia = rPr.find(qn('a:ea'))
        if eastAsia is None:
            eastAsia = etree.SubElement(rPr, qn('a:ea'))
        eastAsia.set('typeface', font_name)
    return tb


def add_text_in_shape(shape, text, size=14, bold=False, color=GRAY_900,
                      align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.MIDDLE, font_name='Microsoft YaHei'):
    tf = shape.text_frame
    tf.margin_left = Inches(0.1)
    tf.margin_right = Inches(0.1)
    tf.margin_top = Inches(0.05)
    tf.margin_bottom = Inches(0.05)
    tf.word_wrap = True
    tf.vertical_anchor = anchor

    lines = text.split('\n') if isinstance(text, str) else text
    for i, line in enumerate(lines):
        if i == 0:
            p = tf.paragraphs[0]
        else:
            p = tf.add_paragraph()
        p.alignment = align
        p.text = line
        run = p.runs[0]
        run.font.size = Pt(size)
        run.font.bold = bold
        run.font.color.rgb = color
        run.font.name = font_name
        rPr = run._r.get_or_add_rPr()
        eastAsia = rPr.find(qn('a:ea'))
        if eastAsia is None:
            eastAsia = etree.SubElement(rPr, qn('a:ea'))
        eastAsia.set('typeface', font_name)
    return tf


def set_bg(slide, color):
    """设置幻灯片背景色"""
    bg = slide.background
    fill = bg.fill
    fill.solid()
    fill.fore_color.rgb = color


def add_cover_decoration(slide):
    """封面页装饰：网格 + 光晕"""
    # 网格背景
    for i in range(0, 30):
        x = Inches(i * 0.5)
        line = slide.shapes.add_connector(1, x, 0, x, SLIDE_H)
        line.line.color.rgb = RGBColor(0xff, 0xff, 0xff)
        line.line.width = Pt(0.25)
        line.line.fill.background()
        # 让线条半透明
    for i in range(0, 16):
        y = Inches(i * 0.5)
        line = slide.shapes.add_connector(1, 0, y, SLIDE_W, y)
        line.line.color.rgb = RGBColor(0xff, 0xff, 0xff)
        line.line.width = Pt(0.25)
        line.line.fill.background()

    # 装饰圆形
    add_rect(slide, Inches(-2), Inches(-2), Inches(5), Inches(5), PRIMARY, shape_type=MSO_SHAPE.OVAL)
    add_rect(slide, Inches(11), Inches(5), Inches(5), Inches(5), SUCCESS, shape_type=MSO_SHAPE.OVAL)
    add_rect(slide, Inches(6), Inches(-1), Inches(3), Inches(3), INFO, shape_type=MSO_SHAPE.OVAL)


def add_slide_header(slide, num, eyebrow, title):
    """统一的内容页头部"""
    # 大数字
    add_text(slide, Inches(0.4), Inches(0.3), Inches(1.2), Inches(1.2),
             num, size=56, bold=True, color=PRIMARY, align=PP_ALIGN.LEFT)
    # 眉题
    add_text(slide, Inches(1.7), Inches(0.45), Inches(8), Inches(0.4),
             eyebrow, size=11, bold=True, color=PRIMARY, align=PP_ALIGN.LEFT)
    # 标题
    add_text(slide, Inches(1.7), Inches(0.75), Inches(11), Inches(0.7),
             title, size=26, bold=True, color=GRAY_900, align=PP_ALIGN.LEFT)
    # 分隔线
    line = slide.shapes.add_connector(1, Inches(0.4), Inches(1.5), Inches(12.9), Inches(1.5))
    line.line.color.rgb = GRAY_100
    line.line.width = Pt(2)


def add_slide_footer(slide, left_text, right_text):
    """统一的页脚"""
    add_text(slide, Inches(0.4), Inches(7.15), Inches(8), Inches(0.3),
             left_text, size=9, color=GRAY_500, align=PP_ALIGN.LEFT)
    add_text(slide, Inches(5), Inches(7.15), Inches(8), Inches(0.3),
             right_text, size=9, color=GRAY_500, align=PP_ALIGN.RIGHT)


# ========== Slide 1: 封面 ==========
def slide_01_cover():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, DARK)
    add_cover_decoration(s)

    # 徽章
    badge = add_rect(s, Inches(4.7), Inches(1.2), Inches(4), Inches(0.4), PRIMARY_DARK)
    add_text_in_shape(badge, '🏆 2026 · 智能立法创新成果 · 金奖候选',
                      size=11, bold=True, color=PRIMARY_LIGHT, align=PP_ALIGN.CENTER)

    # 主标题
    add_text(s, Inches(0.5), Inches(1.9), Inches(12.3), Inches(1.2),
             '智立法', size=72, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, Inches(0.5), Inches(3.1), Inches(12.3), Inches(1.2),
             '行政立法智能辅助平台', size=54, bold=True, color=PRIMARY_LIGHT, align=PP_ALIGN.CENTER)

    # 副标题
    add_text(s, Inches(0.5), Inches(4.2), Inches(12.3), Inches(0.5),
             '从立项到评估 · 让行政立法全流程智能化',
             size=22, color=GRAY_300, align=PP_ALIGN.CENTER)

    # 数据条
    metrics = [
        ('8', '业务模块', PRIMARY),
        ('24', '数据表', SUCCESS),
        ('14', 'Controller', INFO),
        ('5', '容器', WARNING),
        ('18→9', '月效率', DANGER),
    ]
    x_start = 0.8
    width = 2.3
    gap = 0.2
    for i, (num, label, color) in enumerate(metrics):
        x = Inches(x_start + i * (width + gap))
        add_text(s, x, Inches(5.3), Inches(width), Inches(0.7),
                 num, size=36, bold=True, color=color, align=PP_ALIGN.CENTER)
        add_text(s, x, Inches(6.0), Inches(width), Inches(0.4),
                 label, size=12, color=GRAY_300, align=PP_ALIGN.CENTER)

    # 底部
    add_text(s, Inches(0.5), Inches(6.9), Inches(8), Inches(0.3),
             '智立法团队 · 答辩日 2026-10-12', size=11, color=GRAY_300, align=PP_ALIGN.LEFT)
    add_text(s, Inches(5), Inches(6.9), Inches(8), Inches(0.3),
             'legislation-platform v0.2.0', size=11, color=GRAY_300, align=PP_ALIGN.RIGHT)


# ========== Slide 2: 背景痛点 ==========
def slide_02_pain():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '01', 'BACKGROUND & CHALLENGE', '背景 · 行政立法的三大痛点')

    # 顶部数据卡
    data_cards = [
        ('5', '现行法律', PRIMARY),
        ('30', '行政法规', SUCCESS),
        ('8000+', '地方规章', WARNING),
        ('800+', '年新增地方规章', DANGER),
    ]
    for i, (num, label, color) in enumerate(data_cards):
        x = Inches(0.5 + i * 3.15)
        card = add_rect(s, x, Inches(1.7), Inches(2.9), Inches(1.0), color)
        add_text_in_shape(card, num, size=32, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        add_text(s, x, Inches(2.45), Inches(2.9), Inches(0.4),
                 label, size=12, color=WHITE, align=PP_ALIGN.CENTER)

    # 三大痛点
    pains = [
        ('⏱', '效率低', '从立项到公布平均 18 个月；流程节点 10+ 个；多部门会签、跨阶段流转耗时严重', '18月', '9-12月', DANGER),
        ('⚠', '风险高', '越权立法、引用上位法冲突、引用失效法条等情形全靠人工核对，标准不统一、遗漏风险大', '人工', 'AI自动', WARNING),
        ('📢', '参与弱', '公众意见分散于网站、APP、座谈会、纸质件，碎片化严重，难以归类统计与追踪', '碎片化', '结构化', INFO),
    ]
    for i, (icon, name, desc, frm, to, color) in enumerate(pains):
        x = Inches(0.5 + i * 4.15)
        card = add_rect(s, x, Inches(3.0), Inches(3.9), Inches(4.0), WHITE, line_color=GRAY_100)
        # 顶部色条
        add_rect(s, x, Inches(3.0), Inches(3.9), Inches(0.1), color)
        # 图标
        icon_bg = add_rect(s, x + Inches(0.3), Inches(3.3), Inches(0.7), Inches(0.7),
                           color, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(icon_bg, icon, size=22, color=WHITE, align=PP_ALIGN.CENTER)
        # 标题
        add_text(s, x + Inches(1.1), Inches(3.35), Inches(2.5), Inches(0.5),
                 name, size=18, bold=True, color=GRAY_900)
        # 描述
        add_text(s, x + Inches(0.3), Inches(4.15), Inches(3.3), Inches(1.5),
                 desc, size=12, color=GRAY_500)
        # 转化指标
        line_y = Inches(5.8)
        add_text(s, x + Inches(0.3), line_y, Inches(1.5), Inches(0.5),
                 frm, size=16, bold=True, color=DANGER, align=PP_ALIGN.CENTER)
        add_text(s, x + Inches(1.5), line_y, Inches(0.5), Inches(0.5),
                 '→', size=18, bold=True, color=GRAY_300, align=PP_ALIGN.CENTER)
        add_text(s, x + Inches(2.0), line_y, Inches(1.5), Inches(0.5),
                 to, size=16, bold=True, color=SUCCESS, align=PP_ALIGN.CENTER)

    add_slide_footer(s, '背景痛点 · 2024 年底数据', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 3: 项目愿景 ==========
def slide_03_vision():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '02', 'VISION & GOAL', '项目愿景 · 用 AI 重塑立法流程')

    # 左侧大色块
    left_card = add_rect(s, Inches(0.5), Inches(1.8), Inches(5.5), Inches(5.2), PRIMARY)
    add_text(s, Inches(0.8), Inches(2.0), Inches(5), Inches(0.6),
             '🎯 项目使命', size=22, bold=True, color=WHITE)
    add_text(s, Inches(0.8), Inches(2.7), Inches(5), Inches(3.5),
             '建设覆盖"立项 → 起草 → 审查 → 公布 → 清理 → 评估"\n全生命周期的行政立法智能辅助平台，\n以 AI + 知识图谱 + 大数据为底座，\n让行政立法工作实现「数字化、智能化、协同化」。',
             size=14, color=WHITE)
    # 标签
    tags = ['数字化', '智能化', '协同化']
    for i, tag in enumerate(tags):
        x = Inches(0.8 + i * 1.6)
        tag_bg = add_rect(s, x, Inches(5.7), Inches(1.4), Inches(0.4), PRIMARY_DARK)
        add_text_in_shape(tag_bg, tag, size=12, bold=True, color=PRIMARY_LIGHT, align=PP_ALIGN.CENTER)

    # 右侧 5 大目标
    goals = [
        ('①', '立法项目全流程线上管理 + 到期自动提醒', '10 节点流程模板 · 法定期限自动预警', PRIMARY),
        ('②', '法规资料库 + 上下位法关系图谱', 'Neo4j 知识图谱 · 上位法冲突自动检测', SUCCESS),
        ('③', 'AI 辅助草案生成 + 智慧审查', 'Qwen 双通道 · 6 类规则引擎', INFO),
        ('④', '多渠道公众意见 + 自动归类反馈', '5 渠道接入 · SimHash 去重 · 立场识别', PURPLE),
        ('⑤', '法规实施效果多维评估', '合法性 · 落实性 · 满意度 三维雷达', WARNING),
    ]
    for i, (num, name, desc, color) in enumerate(goals):
        y = Inches(1.8 + i * 1.06)
        card = add_rect(s, Inches(6.3), y, Inches(6.5), Inches(0.95), WHITE, line_color=GRAY_100)
        # 编号
        num_bg = add_rect(s, Inches(6.45), y + Inches(0.15), Inches(0.65), Inches(0.65),
                          color, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(num_bg, num, size=18, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        # 内容
        add_text(s, Inches(7.25), y + Inches(0.1), Inches(5.4), Inches(0.4),
                 name, size=13, bold=True, color=GRAY_900)
        add_text(s, Inches(7.25), y + Inches(0.5), Inches(5.4), Inches(0.4),
                 desc, size=10, color=GRAY_500)

    add_slide_footer(s, '项目愿景 · 5 大建设目标', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 4: 整体架构 ==========
def slide_04_arch():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '03', 'ARCHITECTURE', '整体架构 · 4 层分层设计')

    layers = [
        ('🌐', '接入层 · Client Layer', 'Vue 3 Web 管理后台 + 微信小程序 + 公开 H5 页面',
         ['Vue 3', 'Element Plus', 'ECharts', '小程序', 'H5'], PRIMARY),
        ('⚙', '应用层 · Application Layer', 'Spring Boot 3.1 · 14 Controller · 9 Service · Qwen AI 助手',
         ['Spring Boot 3.1', 'JWT', '@Async', 'Qwen', 'SpringDoc'], SUCCESS),
        ('💾', '数据层 · Data Layer', 'MySQL 24 表 + Neo4j 知识图谱（可选） + Redis 缓存 + 5 种子用户 BCrypt',
         ['MySQL 8', 'Neo4j 5', 'Redis', 'MyBatis-Plus'], INFO),
        ('📊', '可观测层 · Observability Layer', 'Prometheus 8 类业务指标 + Grafana 大盘 + ELK 风格 JSON 日志',
         ['Prometheus', 'Grafana', 'ELK JSON', 'Actuator'], WARNING),
    ]
    for i, (icon, name, desc, tags, color) in enumerate(layers):
        y = Inches(1.7 + i * 1.3)
        # 背景层
        add_rect(s, Inches(0.5), y, Inches(12.3), Inches(1.1), color)
        # 图标
        icon_bg = add_rect(s, Inches(0.7), y + Inches(0.2), Inches(0.7), Inches(0.7),
                           WHITE, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(icon_bg, icon, size=24, color=color, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, Inches(1.6), y + Inches(0.15), Inches(5), Inches(0.4),
                 name, size=16, bold=True, color=WHITE)
        # 描述
        add_text(s, Inches(1.6), y + Inches(0.55), Inches(7), Inches(0.5),
                 desc, size=11, color=WHITE)
        # 标签
        for j, tag in enumerate(tags):
            tx = Inches(8.9 - j * 1.05)
            tag_bg = add_rect(s, tx, y + Inches(0.4), Inches(0.95), Inches(0.32), color, shape_type=MSO_SHAPE.ROUNDED_RECTANGLE)
            tag_bg.fill.solid()
            tag_bg.fill.fore_color.rgb = RGBColor(min(color[0] + 30, 255), min(color[1] + 30, 255), min(color[2] + 30, 255))
            add_text_in_shape(tag_bg, tag, size=10, bold=True, color=WHITE, align=PP_ALIGN.CENTER)

    add_slide_footer(s, '整体架构 · 4 层 13 模块 7 中间件', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 5: 8 大模块 ==========
def slide_05_modules():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '04', '8 MODULES OVERVIEW', '8 大模块 · 立法全周期覆盖')

    modules = [
        ('01', '📋', '立法项目', '立项 / 推进 / 回退 / 10 节点流程', '/legislative-project/*', PRIMARY),
        ('02', '🤖', '草案生成', 'AI 模板拼接 + 上位法 RAG 检索', '/draft/*', SUCCESS),
        ('03', '⚖', '智慧审查', '6 类规则引擎 · 红黄蓝灰四级', '/review/*', WARNING),
        ('04', '🧹', '清理任务', 'DAILY / PERIODIC / THEMATIC 三模式', '/cleanup/*', PINK),
        ('05', '📈', '实施评估', '合法性 / 落实性 / 满意度 三维评分', '/evaluation/*', INFO),
        ('06', '💬', '意见征集', '多渠道汇总 + AI 去重归类', '/consultation/*', PURPLE),
        ('07', '📚', '资料库', '法规 / 草案 / 评估报告 / 全文检索', '/library/*', DANGER),
        ('08', '📺', '信息展示', 'Dashboard 大屏 + 11 卡片 + 30s 轮播', '/info/*', RGBColor(0x14, 0xb8, 0xa6)),
    ]
    for i, (num, icon, name, desc, api, color) in enumerate(modules):
        col = i % 4
        row = i // 4
        x = Inches(0.4 + col * 3.18)
        y = Inches(1.7 + row * 2.7)
        card = add_rect(s, x, y, Inches(3.0), Inches(2.5), WHITE, line_color=GRAY_100)
        # 左侧色条
        add_rect(s, x, y, Inches(0.1), Inches(2.5), color)
        # 编号
        add_text(s, x + Inches(0.25), y + Inches(0.15), Inches(1.5), Inches(0.3),
                 f'MODULE {num}', size=9, bold=True, color=GRAY_500)
        # 图标
        add_text(s, x + Inches(0.25), y + Inches(0.5), Inches(1.5), Inches(0.6),
                 icon, size=28)
        # 名称
        add_text(s, x + Inches(0.25), y + Inches(1.15), Inches(2.6), Inches(0.4),
                 name, size=15, bold=True, color=GRAY_900)
        # 描述
        add_text(s, x + Inches(0.25), y + Inches(1.55), Inches(2.6), Inches(0.5),
                 desc, size=10, color=GRAY_500)
        # API
        api_bg = add_rect(s, x + Inches(0.25), y + Inches(2.05), Inches(2.6), Inches(0.32), GRAY_50)
        add_text_in_shape(api_bg, api, size=9, bold=True, color=PRIMARY, align=PP_ALIGN.CENTER)

    add_slide_footer(s, '8 大模块 · 立法全周期闭环', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 6: 演示 1 - 项目流程 ==========
def slide_06_flow():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '05', 'DEMO 1 · FLOW MANAGEMENT', '演示 · 项目流程端到端')

    # 左侧流程图
    flow_card = add_rect(s, Inches(0.4), Inches(1.7), Inches(7), Inches(5.2),
                         WHITE, line_color=GRAY_100)
    add_text(s, Inches(0.6), Inches(1.85), Inches(5), Inches(0.4),
             '立法项目流程图（10 节点）', size=14, bold=True, color=GRAY_900)

    # 10 节点
    steps = [
        ('1', '立项建议', '09-15', True, False),
        ('2', '立项审查', '09-22', True, False),
        ('3', '起草', '10-01', True, False),
        ('4', '征求意见', '10-05', True, False),
        ('5', '专家论证', '10-10 ⏰', False, True),
        ('6', '风险评估', '—', False, False),
        ('7', '法制审查', '—', False, False),
        ('8', '审议', '—', False, False),
        ('9', '公布', '—', False, False),
        ('10', '备案', '—', False, False),
    ]
    for i, (num, name, meta, done, current) in enumerate(steps):
        y = Inches(2.3 + i * 0.43)
        if done:
            bg = RGBColor(0xd1, 0xfa, 0xe5)
            text_color = SUCCESS
            dot_color = SUCCESS
        elif current:
            bg = RGBColor(0xe0, 0xe7, 0xff)
            text_color = PRIMARY
            dot_color = PRIMARY
        else:
            bg = GRAY_50
            text_color = GRAY_500
            dot_color = GRAY_300
        add_rect(s, Inches(0.6), y, Inches(6.6), Inches(0.38), bg)
        # 圆点
        dot = add_rect(s, Inches(0.75), y + Inches(0.05), Inches(0.28), Inches(0.28),
                       dot_color, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(dot, num, size=10, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, Inches(1.15), y + Inches(0.05), Inches(4), Inches(0.3),
                 name, size=11, bold=current, color=text_color)
        # 元数据
        add_text(s, Inches(5.5), y + Inches(0.05), Inches(1.6), Inches(0.3),
                 meta, size=10, color=text_color, align=PP_ALIGN.RIGHT)

    # 右侧
    # 指标卡
    metric = add_rect(s, Inches(7.7), Inches(1.7), Inches(5.2), Inches(1.5), PRIMARY)
    add_text_in_shape(metric, '55% → 88%', size=32, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, Inches(7.7), Inches(2.7), Inches(5.2), Inches(0.4),
             '流程推进效率（演示前进 3 节点）', size=12, color=WHITE, align=PP_ALIGN.CENTER)

    # 功能 pill
    features = [
        ('⚡', '推进下一阶段'), ('↩', '回退指定阶段'),
        ('🔔', '7 天预警徽章'), ('📊', 'ECharts 流程图'),
        ('⏱', '法定期限管理'), ('📝', '操作时间记录'),
    ]
    for i, (icon, name) in enumerate(features):
        col = i % 2
        row = i // 2
        x = Inches(7.7 + col * 2.7)
        y = Inches(3.4 + row * 0.5)
        pill = add_rect(s, x, y, Inches(2.5), Inches(0.42), GRAY_50, line_color=GRAY_100)
        # 图标
        icon_bg = add_rect(s, x + Inches(0.05), y + Inches(0.05), Inches(0.32), Inches(0.32),
                           PRIMARY, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(icon_bg, icon, size=10, color=WHITE, align=PP_ALIGN.CENTER)
        add_text(s, x + Inches(0.5), y + Inches(0.08), Inches(2), Inches(0.3),
                 name, size=11, bold=True, color=GRAY_700)

    # 提示框
    tip = add_rect(s, Inches(7.7), Inches(5.6), Inches(5.2), Inches(1.3),
                   RGBColor(0xee, 0xf2, 0xff), line_color=PRIMARY_LIGHT)
    add_text(s, Inches(7.85), Inches(5.7), Inches(5), Inches(0.4),
             '💡 讲点：', size=11, bold=True, color=PRIMARY)
    add_text(s, Inches(7.85), Inches(6.0), Inches(5), Inches(0.85),
             '基于《行政法规制定程序条例》配置 10 节点模板，立法流程可视化 + 端到端推进 + 自动期限预警。',
             size=10, color=GRAY_700)

    add_slide_footer(s, '演示 1 · 项目流程端到端', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 7: 演示 2 - 草案生成 ==========
def slide_07_draft():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '06', 'DEMO 2 · AI DRAFT GENERATION', '演示 · AI 草案生成（异步任务）')

    # 左侧流程
    add_text(s, Inches(0.5), Inches(1.7), Inches(6), Inches(0.4),
             '生成流程', size=14, bold=True, color=GRAY_900)

    steps = [
        ('1', '选择项目 + 填入上位法条款', GRAY_50, PRIMARY),
        ('2', '点击「开始生成」→ 拿到 taskId', GRAY_50, PRIMARY),
        ('3', '@Async 异步任务后台执行', RGBColor(0xee, 0xf2, 0xff), PRIMARY),
        ('4', '前端 1.5 秒轮询 taskId', GRAY_50, PRIMARY),
        ('5', '显示 6 章 Markdown 草案', RGBColor(0xd1, 0xfa, 0xe5), SUCCESS),
        ('6', '支持导出 Markdown / Word', GRAY_50, PRIMARY),
    ]
    for i, (num, text, bg, color) in enumerate(steps):
        y = Inches(2.2 + i * 0.6)
        add_rect(s, Inches(0.5), y, Inches(6), Inches(0.5), bg, line_color=GRAY_100 if bg == GRAY_50 else color)
        # 圆点
        dot = add_rect(s, Inches(0.65), y + Inches(0.1), Inches(0.3), Inches(0.3),
                       color, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(dot, num, size=11, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        add_text(s, Inches(1.1), y + Inches(0.12), Inches(5.3), Inches(0.3),
                 text, size=11, color=GRAY_700)

    # 右侧代码块
    code_bg = add_rect(s, Inches(6.8), Inches(1.7), Inches(6.1), Inches(5.2), DARK)
    add_text(s, Inches(7.0), Inches(1.85), Inches(5), Inches(0.4),
             '生成结果预览 · 6 章结构', size=14, bold=True, color=WHITE)

    chapters = [
        ('# 第一章 总则', '第一条【立法目的】为规范XX管理...', PRIMARY_LIGHT, GRAY_300),
        ('# 第二章 适用范围', '第四条【地域范围】本市行政区域内...', PRIMARY_LIGHT, GRAY_300),
        ('# 第三章 主体职责', '第八条【部门职责】市XX主管部门...', PRIMARY_LIGHT, GRAY_300),
        ('# 第四章 权利义务', '第十二条【当事人权利】...', PRIMARY_LIGHT, GRAY_300),
        ('# 第五章 法律责任', '第十六条【处罚规则】...', PRIMARY_LIGHT, GRAY_300),
        ('# 第六章 附则', '第二十条【施行日期】...', PRIMARY_LIGHT, GRAY_300),
    ]
    for i, (ch, desc, c1, c2) in enumerate(chapters):
        y = Inches(2.4 + i * 0.55)
        add_text(s, Inches(7.0), y, Inches(5.5), Inches(0.3),
                 ch, size=11, bold=True, color=c1)
        add_text(s, Inches(7.0), y + Inches(0.25), Inches(5.5), Inches(0.3),
                 desc, size=10, color=c2)

    # 进度条
    add_text(s, Inches(7.0), Inches(6.0), Inches(3), Inches(0.3),
             '进度', size=10, color=GRAY_300)
    add_rect(s, Inches(7.0), Inches(6.3), Inches(4), Inches(0.15),
             RGBColor(0x33, 0x41, 0x55))
    add_rect(s, Inches(7.0), Inches(6.3), Inches(4), Inches(0.15), SUCCESS)
    add_text(s, Inches(11.2), Inches(6.25), Inches(1.5), Inches(0.3),
             '✓ SUCCESS', size=11, bold=True, color=SUCCESS_LIGHT)

    add_slide_footer(s, '演示 2 · AI 草案生成', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 8: 演示 3 - 智慧审查 ==========
def slide_08_review():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '07', 'DEMO 3 · INTELLIGENT REVIEW', '演示 · 智慧审查（6 类规则 · 4 级问题）')

    # 左侧 4 个严重程度卡
    cards = [
        ('🔴 严重冲突', '2', 'R001 上位法冲突 + R002 违规越权', DANGER),
        ('🔴 越权立法', '1', '超越法定权限', RGBColor(0x9f, 0x12, 0x39)),
        ('🟡 需关注', '3', 'R003 引用失效 + R004 条文重复', WARNING),
        ('🔵 格式建议', '1', 'R005 格式不规范', INFO),
    ]
    for i, (label, count, desc, color) in enumerate(cards):
        col = i % 2
        row = i // 2
        x = Inches(0.5 + col * 3.2)
        y = Inches(1.8 + row * 2.5)
        add_rect(s, x, y, Inches(3.0), Inches(2.3), color)
        add_text(s, x + Inches(0.2), y + Inches(0.15), Inches(2.6), Inches(0.4),
                 label, size=13, bold=True, color=WHITE)
        add_text(s, x + Inches(0.2), y + Inches(0.6), Inches(2.6), Inches(0.8),
                 count, size=48, bold=True, color=WHITE)
        add_text(s, x + Inches(0.2), y + Inches(1.55), Inches(2.6), Inches(0.7),
                 desc, size=10, color=WHITE)

    # 右侧规则清单
    right_card = add_rect(s, Inches(7.0), Inches(1.8), Inches(5.9), Inches(5.1),
                          WHITE, line_color=GRAY_100)
    add_text(s, Inches(7.2), Inches(1.95), Inches(5), Inches(0.4),
             '规则清单 · 6 类规则引擎', size=14, bold=True, color=GRAY_900)

    rules = [
        ('R001', '上位法冲突', '红色 · 严重', DANGER),
        ('R002', '违规越权', '红色 · 严重', RGBColor(0x9f, 0x12, 0x39)),
        ('R003', '引用失效法条', '黄色 · 关注', WARNING),
        ('R004', '条文重复', '黄色 · 关注', WARNING),
        ('R005', '格式不规范', '蓝色 · 格式', INFO),
        ('R006', '语言冗杂', '灰色 · 优化', GRAY_500),
    ]
    for i, (code, name, type_, color) in enumerate(rules):
        y = Inches(2.5 + i * 0.7)
        add_rect(s, Inches(7.2), y, Inches(5.5), Inches(0.6), GRAY_50)
        add_rect(s, Inches(7.2), y, Inches(0.08), Inches(0.6), color)
        # 编号
        code_bg = add_rect(s, Inches(7.4), y + Inches(0.12), Inches(0.7), Inches(0.36), color)
        add_text_in_shape(code_bg, code, size=10, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, Inches(8.2), y + Inches(0.15), Inches(3), Inches(0.3),
                 name, size=12, bold=True, color=GRAY_700)
        # 类型
        add_text(s, Inches(10.5), y + Inches(0.15), Inches(2), Inches(0.3),
                 type_, size=10, color=GRAY_500, align=PP_ALIGN.RIGHT)

    add_slide_footer(s, '演示 3 · 智慧审查引擎', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 9: 演示 4 - 清理+评估 ==========
def slide_09_evaluation():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '08', 'DEMO 4 · CLEANUP & EVALUATION', '演示 · 清理任务 + 实施评估')

    # 左侧雷达图区域
    radar_card = add_rect(s, Inches(0.4), Inches(1.7), Inches(6.0), Inches(5.2),
                          WHITE, line_color=GRAY_100)
    add_text(s, Inches(0.6), Inches(1.85), Inches(5), Inches(0.4),
             '三维评估雷达图', size=14, bold=True, color=GRAY_900)

    # 用形状模拟雷达图 - 画一个五边形
    cx, cy = Inches(3.4), Inches(4.3)
    radius = Inches(1.7)
    # 简化：用同心五边形表示
    sides = 5
    for scale in [0.3, 0.6, 0.9, 1.0]:
        points = []
        for i in range(sides):
            import math
            angle = math.pi / 2 - i * 2 * math.pi / sides
            x = cx + int(radius * scale * math.cos(angle))
            y = cy - int(radius * scale * math.sin(angle))
            points.append((x, y))
        # 画线段
        for i in range(sides):
            x1, y1 = points[i]
            x2, y2 = points[(i + 1) % sides]
            line = s.shapes.add_connector(1, x1, y1, x2, y2)
            line.line.color.rgb = GRAY_300
            line.line.width = Pt(1)

    # 数据五边形
    import math
    data = [(0.95, '合法性', '92', PRIMARY),
            (0.85, '落实性', '88', SUCCESS),
            (0.7, '满意度', '75', WARNING),
            (0.75, '执行率', '80', INFO),
            (0.8, '公开度', '85', PINK)]

    data_points = []
    for i, (val, name, num, color) in enumerate(data):
        angle = math.pi / 2 - i * 2 * math.pi / sides
        x = cx + int(radius * val * math.cos(angle))
        y = cy - int(radius * val * math.sin(angle))
        data_points.append((x, y, name, num, color))
        # 数据点
        dot = add_rect(s, x - Inches(0.08), y - Inches(0.08), Inches(0.16), Inches(0.16),
                       color, shape_type=MSO_SHAPE.OVAL)

    # 填充数据区
    # 由于 python-pptx 不支持自定义多边形填充，我们画多边形线框
    for i in range(sides):
        x1, y1, _, _, _ = data_points[i]
        x2, y2, _, _, _ = data_points[(i + 1) % sides]
        line = s.shapes.add_connector(1, x1, y1, x2, y2)
        line.line.color.rgb = PRIMARY
        line.line.width = Pt(2.5)

    # 标签
    for i, (x, y, name, num, color) in enumerate(data_points):
        import math as m
        angle = m.pi / 2 - i * 2 * m.pi / sides
        lx = cx + int(radius * 1.15 * m.cos(angle)) - Inches(0.3)
        ly = cy - int(radius * 1.15 * m.sin(angle)) - Inches(0.15)
        add_text(s, lx, ly, Inches(1.2), Inches(0.4),
                 f'{name} {num}', size=10, bold=True, color=color, align=PP_ALIGN.CENTER)

    # 综合得分
    add_text(s, Inches(0.6), Inches(6.4), Inches(5.5), Inches(0.4),
             '综合得分', size=12, color=GRAY_500, align=PP_ALIGN.CENTER)
    add_text(s, Inches(0.6), Inches(6.65), Inches(5.5), Inches(0.4),
             '84.2 / 100', size=20, bold=True, color=PRIMARY, align=PP_ALIGN.CENTER)

    # 右侧维度卡
    dims = [
        ('⚖', '合法性 · Legality', '执法合规率、与上位法一致性', '30%', PRIMARY),
        ('📊', '落实性 · Implementation', '执行到位率、配套措施完备度', '40%', SUCCESS),
        ('💬', '满意度 · Satisfaction', '公众满意度、投诉率', '30%', WARNING),
    ]
    for i, (icon, name, desc, weight, color) in enumerate(dims):
        y = Inches(1.7 + i * 1.15)
        card = add_rect(s, Inches(6.7), y, Inches(6.2), Inches(1.0),
                        WHITE, line_color=GRAY_100)
        # 图标
        icon_bg = add_rect(s, Inches(6.9), y + Inches(0.2), Inches(0.6), Inches(0.6),
                           color, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(icon_bg, icon, size=20, color=WHITE, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, Inches(7.7), y + Inches(0.15), Inches(3.5), Inches(0.4),
                 name, size=14, bold=True, color=GRAY_900)
        # 描述
        add_text(s, Inches(7.7), y + Inches(0.5), Inches(3.5), Inches(0.4),
                 desc, size=10, color=GRAY_500)
        # 权重
        add_text(s, Inches(11.2), y + Inches(0.15), Inches(1.5), Inches(0.5),
                 weight, size=24, bold=True, color=color, align=PP_ALIGN.RIGHT)
        add_text(s, Inches(11.5), y + Inches(0.65), Inches(1.2), Inches(0.3),
                 '权重', size=10, color=GRAY_500, align=PP_ALIGN.RIGHT)

    # 清理任务提示
    tip = add_rect(s, Inches(6.7), Inches(5.2), Inches(6.2), Inches(1.7),
                   RGBColor(0xd1, 0xfa, 0xe5), line_color=SUCCESS_LIGHT)
    add_text(s, Inches(6.9), Inches(5.35), Inches(5.8), Inches(0.4),
             '💡 清理任务', size=12, bold=True, color=SUCCESS)
    add_text(s, Inches(6.9), Inches(5.7), Inches(5.8), Inches(1.0),
             '2026Q4 规章集中清理\nAI 推荐 124 候选法规\nKEEP 87 / MODIFY 24 / OBSOLETE 13',
             size=11, color=GRAY_700)

    add_slide_footer(s, '演示 4 · 清理 + 评估', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 10: 演示 5 - 意见征集 ==========
def slide_10_consultation():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '09', 'DEMO 5 · PUBLIC CONSULTATION', '演示 · 意见征集（5 渠道 + AI 归类）')

    # 左侧 5 渠道
    add_text(s, Inches(0.5), Inches(1.7), Inches(7), Inches(0.4),
             '5 渠道联动', size=14, bold=True, color=GRAY_900)

    channels = [
        ('🖥', 'Web 后台', '内部工作人员'),
        ('📱', 'H5 公众页', '移动端公众'),
        ('💬', '微信小程序', '扫码提交'),
        ('🏛', '政务 APP', '政务用户'),
        ('🤝', '座谈会', '线下征集'),
    ]
    for i, (icon, name, desc) in enumerate(channels):
        col = i % 3
        row = i // 3
        x = Inches(0.4 + col * 2.45)
        y = Inches(2.2 + row * 2.3)
        card = add_rect(s, x, y, Inches(2.3), Inches(2.0), WHITE, line_color=GRAY_100)
        add_text(s, x, y + Inches(0.2), Inches(2.3), Inches(0.7),
                 icon, size=32, align=PP_ALIGN.CENTER)
        add_text(s, x, y + Inches(1.0), Inches(2.3), Inches(0.4),
                 name, size=13, bold=True, color=GRAY_900, align=PP_ALIGN.CENTER)
        add_text(s, x, y + Inches(1.4), Inches(2.3), Inches(0.4),
                 desc, size=10, color=GRAY_500, align=PP_ALIGN.CENTER)

    # 第 6 个 - 实时汇总（高亮）
    special = add_rect(s, Inches(0.4), Inches(4.5), Inches(2.3), Inches(2.0), PRIMARY)
    add_text(s, Inches(0.4), Inches(4.7), Inches(2.3), Inches(0.7),
             '⚡', size=32, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, Inches(0.4), Inches(5.5), Inches(2.3), Inches(0.4),
             '实时汇总', size=13, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, Inches(0.4), Inches(5.9), Inches(2.3), Inches(0.4),
             '多渠道联动', size=10, color=PRIMARY_LIGHT, align=PP_ALIGN.CENTER)

    # 右侧 AI 能力
    add_text(s, Inches(7.5), Inches(1.7), Inches(5), Inches(0.4),
             'AI 智能处理', size=14, bold=True, color=GRAY_900)

    ai_caps = [
        ('🏷', '自动归类', '合法性 / 合理性 / 可行性 / 操作性', '5 类', PRIMARY),
        ('🔁', 'SimHash 去重', '相似度 ≥ 80% 自动合并', '80%', SUCCESS),
        ('🎯', '立场识别', '支持 / 反对 / 建议 / 中立', '4 类', WARNING),
        ('☁', '词云 + 报告', '关键词提取 + 自动报告', '∞', INFO),
    ]
    for i, (icon, name, desc, metric, color) in enumerate(ai_caps):
        y = Inches(2.2 + i * 1.1)
        card = add_rect(s, Inches(7.5), y, Inches(5.4), Inches(0.95), color)
        # 图标
        icon_bg = add_rect(s, Inches(7.7), y + Inches(0.25), Inches(0.5), Inches(0.5),
                           WHITE, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(icon_bg, icon, size=18, color=color, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, Inches(8.4), y + Inches(0.1), Inches(3.5), Inches(0.4),
                 name, size=14, bold=True, color=WHITE)
        # 描述
        add_text(s, Inches(8.4), y + Inches(0.5), Inches(3.5), Inches(0.4),
                 desc, size=10, color=WHITE)
        # 指标
        add_text(s, Inches(11.5), y + Inches(0.2), Inches(1.3), Inches(0.6),
                 metric, size=22, bold=True, color=WHITE, align=PP_ALIGN.RIGHT)

    add_slide_footer(s, '演示 5 · 意见征集端到端', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 11: 演示 6 - Dashboard ==========
def slide_11_dashboard():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '10', 'DEMO 6 · DASHBOARD', '演示 · Dashboard 大屏（11 卡片 · 30s 轮播）')

    # Dashboard 容器
    dash = add_rect(s, Inches(0.4), Inches(1.7), Inches(12.5), Inches(5.2), DARK)

    # 第 1 行：4 个 KPI
    kpis = [
        ('法规存量', '8,236', '部', '↑ 12.4% 同比', SUCCESS_LIGHT),
        ('本年已发布', '342', '部', '↑ 8.2% 同比', SUCCESS_LIGHT),
        ('项目总数', '1,128', '项', '↑ 18.6% 同比', SUCCESS_LIGHT),
        ('资料 + 意见', '26,891', '', '↑ 24.3% 同比', SUCCESS_LIGHT),
    ]
    for i, (label, value, unit, trend, color) in enumerate(kpis):
        x = Inches(0.6 + i * 3.1)
        kpi = add_rect(s, x, Inches(1.9), Inches(2.9), Inches(1.0),
                       RGBColor(0x1e, 0x29, 0x3b), line_color=RGBColor(0x33, 0x41, 0x55))
        add_text(s, x + Inches(0.15), Inches(1.95), Inches(2.6), Inches(0.3),
                 label, size=10, color=GRAY_300)
        add_text(s, x + Inches(0.15), Inches(2.25), Inches(2.6), Inches(0.5),
                 value, size=24, bold=True, color=WHITE)
        if unit:
            add_text(s, x + Inches(2.1), Inches(2.4), Inches(0.6), Inches(0.3),
                     unit, size=10, color=GRAY_300)
        add_text(s, x + Inches(0.15), Inches(2.65), Inches(2.6), Inches(0.3),
                 trend, size=9, color=color)

    # 第 2 行：柱状图 + 饼图
    bar_card = add_rect(s, Inches(0.6), Inches(3.1), Inches(6.0), Inches(1.7),
                        RGBColor(0x1e, 0x29, 0x3b), line_color=RGBColor(0x33, 0x41, 0x55))
    add_text(s, Inches(0.75), Inches(3.2), Inches(5.5), Inches(0.3),
             '月度趋势 · 立法数量', size=10, color=GRAY_300)
    # 12 个月柱状
    bar_heights = [40, 55, 48, 65, 70, 58, 80, 85, 75, 92, 78, 88]
    bar_w = 0.35
    for i, h in enumerate(bar_heights):
        bx = Inches(0.8 + i * 0.47)
        by = Inches(4.55 - h * 0.012)
        add_rect(s, bx, by, Inches(bar_w), Inches(h * 0.012), PRIMARY)
    # 月份标签
    for i in range(12):
        bx = Inches(0.8 + i * 0.47)
        add_text(s, bx - Inches(0.05), Inches(4.6), Inches(0.4), Inches(0.2),
                 f'{i+1}月', size=7, color=GRAY_300, align=PP_ALIGN.CENTER)

    # 饼图（用圆环近似）
    pie_card = add_rect(s, Inches(6.8), Inches(3.1), Inches(2.8), Inches(1.7),
                        RGBColor(0x1e, 0x29, 0x3b), line_color=RGBColor(0x33, 0x41, 0x55))
    add_text(s, Inches(6.95), Inches(3.2), Inches(2.5), Inches(0.3),
             '法规类型分布', size=10, color=GRAY_300)
    # 画圆环
    pie_cx, pie_cy = Inches(8.2), Inches(4.0)
    pie_r = Inches(0.55)
    # 简化用三个色块表示
    add_rect(s, Inches(7.5), Inches(4.45), Inches(0.3), Inches(0.15), INFO)
    add_text(s, Inches(7.85), Inches(4.42), Inches(1.0), Inches(0.2),
             '行政法规', size=8, color=GRAY_300)
    add_rect(s, Inches(7.5), Inches(4.65), Inches(0.3), Inches(0.15), PRIMARY)
    add_text(s, Inches(7.85), Inches(4.62), Inches(1.0), Inches(0.2),
             '部门规章', size=8, color=GRAY_300)
    add_rect(s, Inches(7.5), Inches(4.85), Inches(0.3), Inches(0.15), SUCCESS)
    add_text(s, Inches(7.85), Inches(4.82), Inches(1.0), Inches(0.2),
             '地方规章', size=8, color=GRAY_300)
    add_text(s, Inches(7.5), Inches(3.6), Inches(1.4), Inches(0.3),
             '8.2K', size=22, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, Inches(7.5), Inches(3.9), Inches(1.4), Inches(0.3),
             '总法规数', size=8, color=GRAY_300, align=PP_ALIGN.CENTER)

    # 地区分布
    region_card = add_rect(s, Inches(9.8), Inches(3.1), Inches(3.0), Inches(1.7),
                           RGBColor(0x1e, 0x29, 0x3b), line_color=RGBColor(0x33, 0x41, 0x55))
    add_text(s, Inches(9.95), Inches(3.2), Inches(2.7), Inches(0.3),
             '地区分布 Top 5', size=10, color=GRAY_300)
    regions = [('广东', 92, '1,260'), ('江苏', 85, '1,156'), ('浙江', 78, '1,058'),
               ('山东', 70, '952'), ('上海', 62, '848')]
    for i, (rname, pct, num) in enumerate(regions):
        y = Inches(3.55 + i * 0.25)
        add_text(s, Inches(9.95), y, Inches(0.6), Inches(0.2),
                 rname, size=8, color=GRAY_300)
        add_rect(s, Inches(10.55), y + Inches(0.04), Inches(1.5), Inches(0.12),
                 RGBColor(0x33, 0x41, 0x55))
        add_rect(s, Inches(10.55), y + Inches(0.04), Inches(1.5 * pct / 100), Inches(0.12), PRIMARY)
        add_text(s, Inches(12.1), y, Inches(0.65), Inches(0.2),
                 num, size=8, bold=True, color=WHITE, align=PP_ALIGN.RIGHT)

    # 第 3 行：即将到期
    deadline_card = add_rect(s, Inches(0.6), Inches(4.95), Inches(12.2), Inches(1.85),
                             RGBColor(0x1e, 0x29, 0x3b), line_color=RGBColor(0x33, 0x41, 0x55))
    add_text(s, Inches(0.75), Inches(5.05), Inches(11.5), Inches(0.3),
             '即将到期项目 · 实时预警', size=11, bold=True, color=WHITE)

    deadlines = [
        ('网络安全管理办法', '7天', DANGER, '🔴'),
        ('数据安全实施细则', '15天', WARNING, '🟡'),
        ('智慧城市建设条例', '28天', INFO, '🔵'),
        ('营商环境优化规定', '45天', SUCCESS, '🟢'),
        ('公共数据开放办法', '60天', PURPLE, '🟣'),
    ]
    for i, (name, days, color, dot) in enumerate(deadlines):
        x = Inches(0.75 + i * 2.4)
        y = Inches(5.45)
        item = add_rect(s, x, y, Inches(2.2), Inches(1.2), color, shape_type=MSO_SHAPE.ROUNDED_RECTANGLE)
        item.fill.solid()
        item.fill.fore_color.rgb = RGBColor(int(color[0] * 0.15), int(color[1] * 0.15), int(color[2] * 0.15))
        item.line.color.rgb = color
        add_text(s, x + Inches(0.1), y + Inches(0.1), Inches(2), Inches(0.3),
                 dot, size=12, bold=True, color=color)
        add_text(s, x + Inches(0.1), y + Inches(0.4), Inches(2), Inches(0.3),
                 name, size=10, bold=True, color=WHITE)
        add_text(s, x + Inches(0.1), y + Inches(0.7), Inches(2), Inches(0.3),
                 days, size=14, bold=True, color=color)

    add_slide_footer(s, '演示 6 · Dashboard 大屏 · 8 真实接口 / 11 图表', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 12: 工程化 ==========
def slide_12_eng():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '11', 'DEVOPS & ENGINEERING', '工程化交付 · 一键启动 + 全链路可观测')

    cards = [
        ('🐳', 'Docker 一键启动', 'docker-compose.yml 编排 5 个容器 · MySQL 8 + Neo4j 5 + Backend + Prometheus + Grafana · 启动到可用 < 3 分钟', '5', '容器', PRIMARY),
        ('📈', 'Prometheus 监控', '抓取 Spring Boot Actuator · 8 类业务指标 (项目/草案/审查/评估/清理) + JVM / HTTP / DB 连接池', '8', '业务指标', SUCCESS),
        ('📊', 'Grafana 大盘', '立法平台定制大盘 · 实时业务指标 + JVM 堆栈 + DB 慢查询 · 支持告警规则配置', '9', '面板', INFO),
        ('📋', 'ELK 风格日志', 'logback-spring.xml 输出结构化 JSON · logs/app.json.log · 字段含 traceId/userId/action', '15+', '字段', WARNING),
        ('✅', '集成测试 + 单测', 'SpringBootTest 后端 20 用例 + Vitest 前端 55 用例 · 覆盖项目推进/草案/审查/评估核心链路', '75', '测试用例', PINK),
        ('🛠', '冒烟脚本 36 步', 'day1-verify.sh (11) + day2-smoke.sh (9) + day3-smoke.sh (16) · 端到端业务链路验证', '36', '冒烟步', PURPLE),
    ]
    for i, (icon, name, desc, num, lbl, color) in enumerate(cards):
        col = i % 3
        row = i // 3
        x = Inches(0.4 + col * 4.25)
        y = Inches(1.7 + row * 2.7)
        card = add_rect(s, x, y, Inches(4.0), Inches(2.5), WHITE, line_color=GRAY_100)
        # 图标
        icon_bg = add_rect(s, x + Inches(0.2), y + Inches(0.2), Inches(0.6), Inches(0.6),
                           color, shape_type=MSO_SHAPE.ROUNDED_RECTANGLE)
        add_text_in_shape(icon_bg, icon, size=22, color=WHITE, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, x + Inches(0.95), y + Inches(0.25), Inches(2.9), Inches(0.5),
                 name, size=14, bold=True, color=GRAY_900)
        # 描述
        add_text(s, x + Inches(0.2), y + Inches(0.95), Inches(3.6), Inches(1.0),
                 desc, size=10, color=GRAY_500)
        # 指标
        add_rect(s, x + Inches(0.2), y + Inches(2.05), Inches(3.6), Inches(0.01), GRAY_100)
        add_text(s, x + Inches(0.2), y + Inches(2.1), Inches(1.2), Inches(0.4),
                 num, size=20, bold=True, color=color)
        add_text(s, x + Inches(1.4), y + Inches(2.2), Inches(2), Inches(0.3),
                 lbl, size=11, color=GRAY_500)

    add_slide_footer(s, '工程化交付 · Docker + Prometheus + Grafana + ELK', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 13: AI 能力 ==========
def slide_13_ai():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '12', 'AI CAPABILITIES', 'AI 能力 · Qwen 双通道 + 降级容错')

    # 左侧代码块
    code_bg = add_rect(s, Inches(0.4), Inches(1.7), Inches(6.5), Inches(5.2), DARK)
    add_text(s, Inches(0.6), Inches(1.85), Inches(6), Inches(0.3),
             'QwenFacade.java · 核心实现', size=10, color=GRAY_300, align=PP_ALIGN.LEFT)

    code_lines = [
        ('// Qwen 在线/离线双通道', GRAY_300, False, False),
        ('public class QwenFacade {', PRIMARY_LIGHT, False, False),
        ('  public String execute(', WHITE, False, False),
        ('      String prompt,', WHITE, False, False),
        ('      Supplier<String> fallback) {', WHITE, False, False),
        ('    try {', SUCCESS_LIGHT, False, False),
        ('      // 在线模式', GRAY_300, False, False),
        ('      return callQwenAPI(prompt);', WHITE, False, False),
        ('    } catch (Exception e) {', SUCCESS_LIGHT, False, False),
        ('      // 失败 → 自动降级离线', GRAY_300, False, False),
        ('      log("Qwen 调用失败，降级到 offline");', WARNING, False, False),
        ('      return fallback.get();', WHITE, False, False),
        ('    }', SUCCESS_LIGHT, False, False),
        ('  }', PRIMARY_LIGHT, False, False),
        ('}', PRIMARY_LIGHT, False, False),
        ('', WHITE, False, False),
        ('// 业务调用', GRAY_300, False, False),
        ('String content = qwenFacade.execute(', WHITE, False, False),
        ('    userPrompt,', WHITE, False, False),
        ('    () -> buildPlaceholderContent(', SUCCESS_LIGHT, False, False),
        ('        "XX 智慧城市管理条例",', WHITE, False, False),
        ('        Arrays.asList("上位法", "总则")', WHITE, False, False),
        ('    ));', SUCCESS_LIGHT, False, False),
    ]

    for i, (line, color, bold, _) in enumerate(code_lines):
        add_text(s, Inches(0.6), Inches(2.2 + i * 0.2), Inches(6.1), Inches(0.2),
                 line, size=9, bold=bold, color=color, font_name='Consolas')

    # 右侧 AI 特性
    add_text(s, Inches(7.1), Inches(1.7), Inches(6), Inches(0.4),
             'AI 4 大应用场景', size=14, bold=True, color=GRAY_900)

    features = [
        ('1', '草案生成 · 模板占位 + RAG 检索', '结合上位法条款 + 异地参考规章片段，6 章结构化输出', '/draft/*', PRIMARY),
        ('2', '智慧审查 · 6 类规则 + 关键词命中', '红黄蓝灰 4 级问题分类 + 置信度评分 + 修改建议', '/review/*', SUCCESS),
        ('3', '清理建议 · 置信度评分', '基于影响程度 + 历史使用 + 反馈意见生成 KEEP/MODIFY/OBSOLETE', '/cleanup/*', INFO),
        ('4', '意见归类 · SimHash + 立场识别', '5 类主题 + 4 类立场 + 80% 重复合并 + 词云 + 自动报告', '/consultation/*', WARNING),
    ]
    for i, (num, name, desc, api, color) in enumerate(features):
        y = Inches(2.2 + i * 1.1)
        card = add_rect(s, Inches(7.1), y, Inches(5.8), Inches(0.95),
                        WHITE, line_color=GRAY_100)
        # 编号
        num_bg = add_rect(s, Inches(7.25), y + Inches(0.2), Inches(0.55), Inches(0.55),
                          color, shape_type=MSO_SHAPE.ROUNDED_RECTANGLE)
        add_text_in_shape(num_bg, num, size=16, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, Inches(7.95), y + Inches(0.1), Inches(3.5), Inches(0.4),
                 name, size=12, bold=True, color=GRAY_900)
        # 描述
        add_text(s, Inches(7.95), y + Inches(0.45), Inches(3.5), Inches(0.4),
                 desc, size=9, color=GRAY_500)
        # API
        api_bg = add_rect(s, Inches(11.55), y + Inches(0.3), Inches(1.3), Inches(0.36),
                          RGBColor(0xd1, 0xfa, 0xe5))
        add_text_in_shape(api_bg, api, size=9, bold=True, color=SUCCESS, align=PP_ALIGN.CENTER)

    add_slide_footer(s, 'AI 能力 · Qwen 在线 + 离线兜底', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 14: 技术亮点 ==========
def slide_14_tech():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '13', 'TECHNICAL HIGHLIGHTS', '技术亮点 · 7 维度技术栈')

    rows = [
        ('后端', ['Spring Boot 3.1', 'MyBatis-Plus', 'Spring Security', '@Async',
                  '@RestControllerAdvice', 'JWT 鉴权', 'ThreadPool', 'SpringDoc', 'Knife4j'], PRIMARY),
        ('AI 智能', ['Qwen 双通道', '在线 + 离线', '降级容错', 'RAG 检索',
                     'SimHash 去重', '关键词提取', '立场识别', '模板兜底'], SUCCESS),
        ('数据', ['MySQL 8 · 24表', 'Neo4j 5', 'Redis', 'BCrypt 加密',
                  '全文索引', 'JSON 字段', '种子数据'], INFO),
        ('可观测', ['Prometheus', 'Grafana 大盘', 'ELK JSON', 'Actuator',
                    '健康检查', '慢查询监控'], WARNING),
        ('前端', ['Vue 3', 'Element Plus', 'ECharts', 'VChart', 'Pinia',
                  'Vue Router', 'axios 拦截器', '55 单测'], PURPLE),
        ('工程', ['Docker 5 容器', '健康检查', '集成测试 20', '冒烟脚本 36',
                  'GitHub Actions', 'Maven'], PINK),
        ('文档', ['ROADMAP', 'API_REFERENCE', 'TEAM_ALLOCATION', '演示脚本 1.2 万字',
                  '答辩讲稿', '答辩 PPT'], DANGER),
    ]

    table_bg = add_rect(s, Inches(0.4), Inches(1.7), Inches(12.5), Inches(5.2),
                        WHITE, line_color=GRAY_100)

    for i, (cat, tags, color) in enumerate(rows):
        y = Inches(1.7 + i * 0.74)
        if i > 0:
            line = s.shapes.add_connector(1, Inches(0.4), y, Inches(12.9), y)
            line.line.color.rgb = GRAY_100
            line.line.width = Pt(0.5)
        # 分类色块
        add_rect(s, Inches(0.4), y, Inches(1.7), Inches(0.74), color)
        add_text_in_shape(add_rect(s, Inches(0.4), y, Inches(1.7), Inches(0.74), color, shape_type=MSO_SHAPE.RECTANGLE),
                          cat, size=16, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        # tags
        x = Inches(2.3)
        for j, tag in enumerate(tags):
            tw = 0.18 * len(tag) + 0.4
            if x + Inches(tw) > Inches(13.0):
                break
            tag_bg = add_rect(s, x, y + Inches(0.22), Inches(tw), Inches(0.32), GRAY_50)
            add_text_in_shape(tag_bg, tag, size=9, bold=True, color=GRAY_700, align=PP_ALIGN.CENTER)
            x = x + Inches(tw + 0.12)

    add_slide_footer(s, '技术亮点 · 7 维度全栈', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 15: 可扩展性 ==========
def slide_15_extensibility():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '14', 'SCALABILITY & EXTENSIBILITY', '可扩展性 · 4 个未来扩展点')

    cards = [
        ('🏢', '多租户', '所有业务表预留 tenant_id 字段，可快速支持省级 / 地市级多租户隔离，配置 RBAC 权限体系',
         "tenant_id = 'PROVINCE_GD'", PRIMARY),
        ('🤖', '多 LLM 切换', 'QwenFacade.execute(prompt, fallback) 抽象层，支持切换 DeepSeek / 文心一言 / GPT-4o / Claude',
         'Qwen → DeepSeek → 文心', SUCCESS),
        ('🕸', '知识图谱增强', 'RegulationCorpusService 接入 Neo4j 后，可做"上位法全文比对 + 法条引用图谱可视化"',
         'Neo4j Regulation Graph', INFO),
        ('📲', '多端扩展', 'miniprogram/ 已预留，可扩展到政务 App / 派单 App / 钉钉 / 企业微信 等多端协同',
         'Web + 小程序 + H5 + App', WARNING),
    ]
    for i, (icon, name, desc, example, color) in enumerate(cards):
        col = i % 2
        row = i // 2
        x = Inches(0.4 + col * 6.35)
        y = Inches(1.7 + row * 2.7)
        card = add_rect(s, x, y, Inches(6.1), Inches(2.5), WHITE, line_color=GRAY_100)
        # 装饰圆
        add_rect(s, x + Inches(4.5), y, Inches(1.6), Inches(1.6), color, shape_type=MSO_SHAPE.OVAL)
        # 图标
        icon_bg = add_rect(s, x + Inches(0.3), y + Inches(0.25), Inches(0.7), Inches(0.7),
                           color, shape_type=MSO_SHAPE.ROUNDED_RECTANGLE)
        add_text_in_shape(icon_bg, icon, size=24, color=WHITE, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, x + Inches(1.2), y + Inches(0.3), Inches(4.5), Inches(0.5),
                 name, size=18, bold=True, color=GRAY_900)
        # 描述
        add_text(s, x + Inches(0.3), y + Inches(1.1), Inches(5.5), Inches(0.9),
                 desc, size=11, color=GRAY_500)
        # 示例
        ex_bg = add_rect(s, x + Inches(0.3), y + Inches(1.95), Inches(5.5), Inches(0.4), GRAY_50)
        add_text_in_shape(ex_bg, example, size=10, bold=True, color=color, align=PP_ALIGN.LEFT)

    add_slide_footer(s, '可扩展性 · 多租户 / 多 LLM / 知识图谱 / 多端', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 16: 风险与应对 ==========
def slide_16_risks():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '15', 'RISK & MITIGATION', '风险与应对 · 4 类风险预案')

    # 表头
    header_bg = add_rect(s, Inches(0.4), Inches(1.7), Inches(12.5), Inches(0.6), PRIMARY)
    add_text(s, Inches(0.5), Inches(1.78), Inches(2.0), Inches(0.5),
             '风险类型', size=12, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, Inches(2.5), Inches(1.78), Inches(4.0), Inches(0.5),
             '风险描述', size=12, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, Inches(6.5), Inches(1.78), Inches(5.5), Inches(0.5),
             '应对方案', size=12, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, Inches(12.0), Inches(1.78), Inches(0.8), Inches(0.5),
             '状态', size=12, bold=True, color=WHITE, align=PP_ALIGN.CENTER)

    rows = [
        ('HIGH', 'Neo4j 启不来', '知识图谱组件启动失败时，清理模块受影响', 'NEO4J_ENABLED=false 降级到内存版图查询'),
        ('HIGH', 'Qwen API 限流', '线上 Qwen 调用超阈值或网络异常', 'QwenFacade 双通道 + 离线模板兜底'),
        ('MID', 'MySQL 表不全', '首次部署时可能出现部分表缺失', 'supplement_public.sql 补丁 + 自动初始化'),
        ('LOW', 'Node 装不上', '前端 npm install peer 依赖冲突', 'npm install --legacy-peer-deps'),
        ('MID', '演示网络抖动', '答辩现场可能网络不稳定', '离线兜底 + 录屏备份 + 完整种子数据'),
    ]
    for i, (level, name, desc, sol) in enumerate(rows):
        y = Inches(2.3 + i * 0.85)
        bg = WHITE if i % 2 == 0 else GRAY_50
        add_rect(s, Inches(0.4), y, Inches(12.5), Inches(0.85), bg, line_color=GRAY_100)
        # 级别
        level_color = DANGER if level == 'HIGH' else (WARNING if level == 'MID' else SUCCESS)
        level_bg = add_rect(s, Inches(0.5), y + Inches(0.27), Inches(0.5), Inches(0.3), level_color)
        add_text_in_shape(level_bg, level, size=8, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        # 风险名称
        add_text(s, Inches(1.1), y + Inches(0.25), Inches(1.4), Inches(0.4),
                 name, size=11, bold=True, color=GRAY_900)
        # 描述
        add_text(s, Inches(2.5), y + Inches(0.25), Inches(4.0), Inches(0.4),
                 desc, size=10, color=GRAY_500)
        # 应对
        add_text(s, Inches(6.5), y + Inches(0.25), Inches(5.5), Inches(0.4),
                 '→ ' + sol, size=10, color=GRAY_700)
        # 状态
        add_text(s, Inches(12.0), y + Inches(0.2), Inches(0.8), Inches(0.4),
                 '✓', size=18, bold=True, color=SUCCESS, align=PP_ALIGN.CENTER)

    add_slide_footer(s, '风险与应对 · 已制定 5 类预案', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 17: 项目时间线 ==========
def slide_17_timeline():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '16', 'TIMELINE & DELIVERY', '项目时间线 · 5 天冲刺 · 3 人团队')

    # 时间线轨道
    track_y = Inches(3.0)
    # 主线
    line = s.shapes.add_connector(1, Inches(1.0), track_y, Inches(12.3), track_y)
    line.line.color.rgb = PRIMARY
    line.line.width = Pt(4)

    days = [
        ('D1', 'Day 1', '架构 + 14 Controller + 9 Service + 24 表 + 种子数据', 'done'),
        ('D2', 'Day 2', 'Vue 3 前端 + 8 页面 + AI QwenFacade + 异步任务', 'done'),
        ('D3', 'Day 3', 'Dashboard + Prometheus + 集成测试 20 + 冒烟 36 步', 'done'),
        ('D4', 'Day 4', 'PPT 美化 + 讲稿打磨 + 演示脚本 1.2 万字 + 答辩演练', 'current'),
        ('D5', 'Day 5', '正式答辩 · 2026-10-12 · 20 分钟演示 + 5 分钟问答', 'todo'),
    ]
    for i, (d, day, tasks, state) in enumerate(days):
        x = Inches(1.0 + i * 2.83)
        # 圆点
        if state == 'done':
            dot_color = SUCCESS
        elif state == 'current':
            dot_color = PRIMARY
        else:
            dot_color = GRAY_300
        dot = add_rect(s, x + Inches(0.5), track_y - Inches(0.3), Inches(0.6), Inches(0.6),
                       dot_color, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(dot, d, size=12, bold=True, color=WHITE, align=PP_ALIGN.CENTER)

        # 日期
        add_text(s, x, track_y - Inches(1.0), Inches(1.6), Inches(0.4),
                 day, size=14, bold=True, color=GRAY_900, align=PP_ALIGN.CENTER)
        # 任务
        add_text(s, x - Inches(0.3), track_y + Inches(0.5), Inches(2.2), Inches(0.8),
                 tasks, size=10, color=GRAY_500, align=PP_ALIGN.CENTER)

    # 底部 4 个数据
    metrics = [
        ('3', '核心成员', PRIMARY),
        ('5', '冲刺天数', SUCCESS),
        ('8', '业务模块', WARNING),
        ('100%', '完成度', INFO),
    ]
    for i, (num, label, color) in enumerate(metrics):
        x = Inches(0.5 + i * 3.15)
        metric = add_rect(s, x, Inches(5.5), Inches(2.9), Inches(1.4), color)
        add_text_in_shape(metric, num, size=36, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
        add_text(s, x, Inches(6.4), Inches(2.9), Inches(0.4),
                 label, size=12, color=WHITE, align=PP_ALIGN.CENTER)

    add_slide_footer(s, '项目时间线 · 5 天冲刺', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 18: 团队分工 ==========
def slide_18_team():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '17', 'TEAM & COLLABORATION', '团队分工 · 3 人 5 天紧密协作')

    members = [
        ('🧑‍💻', 'A · 全栈架构师', 'BACKEND LEAD',
         '• Spring Boot 3.1 后端架构设计\n• 14 Controller + 9 Service 编码\n• MySQL 24 表 + Neo4j 知识图谱\n• Prometheus + Grafana 监控\n• QwenFacade 双通道实现',
         [('24', '表设计'), ('14', 'API'), ('8', '指标')], PRIMARY),
        ('👩‍🎨', 'B · 前端 + AI', 'FRONTEND & AI',
         '• Vue 3 + Element Plus 8 主页面\n• ECharts 流程图 + 雷达图 + Dashboard\n• AI 异步任务 + 轮询机制\n• 6 类审查规则引擎\n• 55 单测 + 36 步冒烟',
         [('8', '页面'), ('11', '图表'), ('55', '单测')], SUCCESS),
        ('🧑‍🔬', 'C · 测试 + 文档', 'QA & DOCS',
         '• SpringBootTest 集成测试 20 用例\n• day1-3 冒烟脚本 36 步\n• ROADMAP / API / TEAM 文档\n• 演示脚本 1.2 万字\n• 答辩 PPT + 讲稿打磨',
         [('20', '测试'), ('36', '冒烟'), ('5', '文档')], WARNING),
    ]
    for i, (avatar, name, role, desc, stats, color) in enumerate(members):
        x = Inches(0.4 + i * 4.25)
        y = Inches(1.7)
        card = add_rect(s, x, y, Inches(4.0), Inches(4.5), WHITE, line_color=GRAY_100)
        # 顶部色条
        add_rect(s, x, y, Inches(4.0), Inches(1.2), color)
        # 头像圆
        avatar_bg = add_rect(s, x + Inches(1.55), y + Inches(0.5), Inches(0.9), Inches(0.9),
                             WHITE, shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(avatar_bg, avatar, size=32, color=GRAY_900, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, x, y + Inches(1.5), Inches(4.0), Inches(0.5),
                 name, size=18, bold=True, color=GRAY_900, align=PP_ALIGN.CENTER)
        # 角色
        add_text(s, x, y + Inches(1.95), Inches(4.0), Inches(0.4),
                 role, size=10, bold=True, color=color, align=PP_ALIGN.CENTER)
        # 描述
        desc_bg = add_rect(s, x + Inches(0.2), y + Inches(2.45), Inches(3.6), Inches(1.4), GRAY_50)
        add_text(s, x + Inches(0.3), y + Inches(2.5), Inches(3.4), Inches(1.3),
                 desc, size=9, color=GRAY_700)
        # 统计
        for j, (num, lbl) in enumerate(stats):
            sx = x + Inches(0.2 + j * 1.2)
            sy = y + Inches(3.95)
            add_text(s, sx, sy, Inches(1.1), Inches(0.4),
                     num, size=18, bold=True, color=color, align=PP_ALIGN.CENTER)
            add_text(s, sx, sy + Inches(0.35), Inches(1.1), Inches(0.2),
                     lbl, size=9, color=GRAY_500, align=PP_ALIGN.CENTER)

    # 底部信息条
    bottom_bg = add_rect(s, Inches(0.4), Inches(6.4), Inches(12.5), Inches(0.6), PRIMARY)
    items = [('⏱', '5 天冲刺 · 10.07-10.11', None),
             ('📦', '75+ 测试用例', None),
             ('📝', '5 万字文档', None),
             ('🚀', 'v0.2.0 答辩版 · 2026-10-12', None)]
    for i, (icon, text, _) in enumerate(items):
        x = Inches(0.6 + i * 3.1)
        add_text(s, x, Inches(6.5), Inches(3), Inches(0.4),
                 f'{icon}  {text}', size=11, color=WHITE, align=PP_ALIGN.CENTER)

    add_slide_footer(s, '团队分工 · 3 人 5 天', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 19: 项目总结 ==========
def slide_19_summary():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, WHITE)
    add_slide_header(s, '18', 'SUMMARY & HIGHLIGHTS', '项目总结 · 五大亮点铸就金奖')

    # 左侧 5 大亮点
    add_text(s, Inches(0.4), Inches(1.7), Inches(6), Inches(0.4),
             '🏆 五大亮点', size=14, bold=True, color=GRAY_900)

    cards = [
        ('🏛', '架构完整', '8 大模块 + 14 Controller + 9 Service + 24 表 · 4 层架构清晰可扩展'),
        ('💎', '真实数据', '8 接口 / 11 图表 · 5 种子用户 · 完整业务闭环 · 75 测试 + 36 冒烟'),
        ('🤖', 'AI 能力突出', 'Qwen 双通道 + 6 类规则 + SimHash + 三维评分 + RAG 检索'),
        ('⚙', '工程化规范', 'Docker 5 容器 + Prometheus 8 指标 + JSON 日志 + 一键启动'),
        ('🎬', '演示流畅', '20 分钟完整演示 · 6 大场景 · 1.2 万字演示脚本'),
    ]
    colors = [PRIMARY, SUCCESS, INFO, WARNING, PINK]
    for i, (icon, name, desc) in enumerate(cards):
        y = Inches(2.2 + i * 0.95)
        card = add_rect(s, Inches(0.4), y, Inches(7.5), Inches(0.85), WHITE, line_color=GRAY_100)
        # 图标
        icon_bg = add_rect(s, Inches(0.55), y + Inches(0.15), Inches(0.55), Inches(0.55),
                           colors[i], shape_type=MSO_SHAPE.OVAL)
        add_text_in_shape(icon_bg, icon, size=18, color=WHITE, align=PP_ALIGN.CENTER)
        # 名称
        add_text(s, Inches(1.25), y + Inches(0.1), Inches(5.5), Inches(0.4),
                 name, size=14, bold=True, color=GRAY_900)
        # 描述
        add_text(s, Inches(1.25), y + Inches(0.45), Inches(5.5), Inches(0.4),
                 desc, size=10, color=GRAY_500)
        # 勾选
        add_text(s, Inches(7.1), y + Inches(0.2), Inches(0.4), Inches(0.4),
                 '✓', size=18, bold=True, color=SUCCESS, align=PP_ALIGN.CENTER)

    # 右侧关键成果
    right = add_rect(s, Inches(8.1), Inches(1.7), Inches(4.8), Inches(5.2), PRIMARY)

    # 装饰圆
    add_rect(s, Inches(11), Inches(1.5), Inches(3), Inches(3), PRIMARY_DARK, shape_type=MSO_SHAPE.OVAL)

    add_text(s, Inches(8.4), Inches(1.95), Inches(4.3), Inches(0.5),
             '🎯 关键成果', size=18, bold=True, color=WHITE)

    achievements = [
        '立法流程从 18 月 → 9 月，效率提升 50%',
        '风险检查从 人工 → AI 自动化，准确率 ≥ 90%',
        '意见处理从 碎片化 → 结构化，归类准确率 85%',
        '8 大模块全栈自研 · 0 外部依赖核心',
        'Docker 一键启动到演示 < 3 分钟',
        '3 人 5 天完整交付 · 答辩就绪',
        '75 测试 + 36 冒烟 · 0 严重缺陷',
    ]
    for i, text in enumerate(achievements):
        y = Inches(2.6 + i * 0.55)
        add_text(s, Inches(8.4), y, Inches(0.3), Inches(0.4),
                 '✓', size=12, bold=True, color=GOLD)
        add_text(s, Inches(8.7), y, Inches(4), Inches(0.5),
                 text, size=11, color=WHITE)

    add_slide_footer(s, '项目总结 · 五大亮点', '智立法团队 · legislation-platform v0.2.0')


# ========== Slide 20: 结束页 ==========
def slide_20_end():
    s = prs.slides.add_slide(blank_layout)
    set_bg(s, DARK)

    # 装饰圆
    add_rect(s, Inches(-2), Inches(-1), Inches(5), Inches(5), PRIMARY, shape_type=MSO_SHAPE.OVAL)
    add_rect(s, Inches(11), Inches(4), Inches(5), Inches(5), SUCCESS, shape_type=MSO_SHAPE.OVAL)
    add_rect(s, Inches(6), Inches(-1), Inches(3), Inches(3), INFO, shape_type=MSO_SHAPE.OVAL)

    # 中央卡片
    add_rect(s, Inches(2.5), Inches(1.5), Inches(8.3), Inches(4.5), GRAY_900)

    # 表情
    add_text(s, Inches(0.5), Inches(2.0), Inches(12.3), Inches(1.2),
             '🙏', size=72, align=PP_ALIGN.CENTER)

    # 标题
    add_text(s, Inches(0.5), Inches(3.3), Inches(12.3), Inches(1.2),
             '谢谢聆听', size=80, bold=True, color=WHITE, align=PP_ALIGN.CENTER)

    # 副标题
    add_text(s, Inches(0.5), Inches(4.4), Inches(12.3), Inches(0.5),
             '智立法 · 行政立法智能辅助平台', size=20, color=PRIMARY_LIGHT, align=PP_ALIGN.CENTER)

    # 信息
    info = [
        ('TEAM', '智立法团队 (A · B · C)'),
        ('DATE', '2026-10-12'),
        ('VERSION', 'v0.2.0'),
        ('REPO', 'legislation-platform'),
    ]
    for i, (label, value) in enumerate(info):
        x = Inches(3.0 + i * 2.0)
        add_text(s, x, Inches(5.3), Inches(1.8), Inches(0.3),
                 label, size=10, color=GRAY_300, align=PP_ALIGN.CENTER)
        add_text(s, x, Inches(5.6), Inches(1.8), Inches(0.4),
                 value, size=12, bold=True, color=WHITE, align=PP_ALIGN.CENTER)

    # 感谢语
    add_text(s, Inches(0.5), Inches(6.5), Inches(12.3), Inches(0.4),
             '— 期待与各位评委老师的交流与指导 —',
             size=12, color=GRAY_500, align=PP_ALIGN.CENTER)


# ========== 主流程 ==========
if __name__ == '__main__':
    print('⏳ 开始生成 PPT ...')

    slide_01_cover()
    print('  ✓ Slide 1/20 封面')
    slide_02_pain()
    print('  ✓ Slide 2/20 背景痛点')
    slide_03_vision()
    print('  ✓ Slide 3/20 项目愿景')
    slide_04_arch()
    print('  ✓ Slide 4/20 整体架构')
    slide_05_modules()
    print('  ✓ Slide 5/20 8 大模块')
    slide_06_flow()
    print('  ✓ Slide 6/20 演示 1 - 项目流程')
    slide_07_draft()
    print('  ✓ Slide 7/20 演示 2 - 草案生成')
    slide_08_review()
    print('  ✓ Slide 8/20 演示 3 - 智慧审查')
    slide_09_evaluation()
    print('  ✓ Slide 9/20 演示 4 - 清理+评估')
    slide_10_consultation()
    print('  ✓ Slide 10/20 演示 5 - 意见征集')
    slide_11_dashboard()
    print('  ✓ Slide 11/20 演示 6 - Dashboard')
    slide_12_eng()
    print('  ✓ Slide 12/20 工程化')
    slide_13_ai()
    print('  ✓ Slide 13/20 AI 能力')
    slide_14_tech()
    print('  ✓ Slide 14/20 技术亮点')
    slide_15_extensibility()
    print('  ✓ Slide 15/20 可扩展性')
    slide_16_risks()
    print('  ✓ Slide 16/20 风险与应对')
    slide_17_timeline()
    print('  ✓ Slide 17/20 项目时间线')
    slide_18_team()
    print('  ✓ Slide 18/20 团队分工')
    slide_19_summary()
    print('  ✓ Slide 19/20 项目总结')
    slide_20_end()
    print('  ✓ Slide 20/20 结束页')

    output = r'd:\ProjectSpace\legislation-platform\答辩PPT\行政立法智能辅助平台-答辩版.pptx'
    prs.save(output)
    print(f'\n✅ PPT 已生成：{output}')
    print(f'   共 {len(prs.slides)} 页')
    print(f'   尺寸：16:9 宽屏 (13.33" × 7.5")')
