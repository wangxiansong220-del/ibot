from __future__ import annotations

from pathlib import Path
from typing import Sequence

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib import patches
from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "output" / "doc"
FIGURE_DIR = OUTPUT_DIR / "figures"
OUTPUT_DOCX = OUTPUT_DIR / "ibot_thesis_draft.docx"

TITLE = "基于 Spring Boot 与 Vue 的企业知识库智能问答系统设计与实现"
FONT_FALLBACK = ["Microsoft YaHei", "SimHei", "SimSun"]
CN_FONT = "宋体"
HEI_FONT = "黑体"
EN_FONT = "Times New Roman"


def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def set_run_font(run, size: float, east_asia: str = CN_FONT, ascii_font: str = EN_FONT, bold: bool = False):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.name = ascii_font
    run._element.rPr.rFonts.set(qn("w:eastAsia"), east_asia)


def set_style_font(style, size: float, east_asia: str, ascii_font: str = EN_FONT, bold: bool = False):
    style.font.size = Pt(size)
    style.font.bold = bold
    style.font.name = ascii_font
    style._element.rPr.rFonts.set(qn("w:eastAsia"), east_asia)


def add_field_run(paragraph, instruction: str):
    run = paragraph.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = instruction
    fld_sep = OxmlElement("w:fldChar")
    fld_sep.set(qn("w:fldCharType"), "separate")
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_sep)
    run._r.append(fld_end)
    return run


def add_toc(paragraph):
    run = paragraph.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = r'TOC \o "1-3" \h \z \u'
    fld_sep = OxmlElement("w:fldChar")
    fld_sep.set(qn("w:fldCharType"), "separate")
    text = OxmlElement("w:t")
    text.text = "打开 Word 后右键目录并选择“更新域”以刷新页码。"
    fld_sep.append(text)
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_sep)
    run._r.append(fld_end)
    set_run_font(run, 12)


def configure_document(doc: Document) -> None:
    section = doc.sections[0]
    section.top_margin = Cm(2.5)
    section.bottom_margin = Cm(2.5)
    section.left_margin = Cm(2.5)
    section.right_margin = Cm(2.5)
    section.header_distance = Cm(2.0)
    section.footer_distance = Cm(1.0)

    normal = doc.styles["Normal"]
    set_style_font(normal, 12, CN_FONT)
    normal.paragraph_format.line_spacing = Pt(22)
    normal.paragraph_format.first_line_indent = Cm(0.74)
    normal.paragraph_format.space_before = Pt(0)
    normal.paragraph_format.space_after = Pt(0)

    heading1 = doc.styles["Heading 1"]
    set_style_font(heading1, 15, HEI_FONT, bold=False)
    heading1.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    heading1.paragraph_format.space_before = Pt(22)
    heading1.paragraph_format.space_after = Pt(22)
    heading1.paragraph_format.first_line_indent = Cm(0)

    heading2 = doc.styles["Heading 2"]
    set_style_font(heading2, 14, HEI_FONT, bold=False)
    heading2.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.LEFT
    heading2.paragraph_format.space_before = Pt(22)
    heading2.paragraph_format.space_after = Pt(22)
    heading2.paragraph_format.first_line_indent = Cm(0)

    heading3 = doc.styles["Heading 3"]
    set_style_font(heading3, 12, HEI_FONT, bold=False)
    heading3.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.LEFT
    heading3.paragraph_format.left_indent = Cm(0.74)
    heading3.paragraph_format.first_line_indent = Cm(0)
    heading3.paragraph_format.space_before = Pt(11)
    heading3.paragraph_format.space_after = Pt(11)

    header = section.header.paragraphs[0]
    header.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = header.add_run("蚌埠学院本科毕业设计（论文）")
    set_run_font(run, 9)

    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run("- ")
    add_field_run(footer, "PAGE")
    footer.add_run(" -")
    for run in footer.runs:
        set_run_font(run, 9)


def add_heading(doc: Document, text: str, level: int):
    return doc.add_heading(text, level=level)


def add_body_paragraph(doc: Document, text: str):
    p = doc.add_paragraph(style="Normal")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    run = p.add_run(text)
    set_run_font(run, 12)
    return p


def add_no_indent_paragraph(doc: Document, text: str):
    p = doc.add_paragraph(style="Normal")
    p.paragraph_format.first_line_indent = Cm(0)
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    run = p.add_run(text)
    set_run_font(run, 12)
    return p


def set_caption_text(paragraph, text: str):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph.paragraph_format.space_before = Pt(6)
    paragraph.paragraph_format.space_after = Pt(6)
    run = paragraph.add_run(text)
    set_run_font(run, 10.5, east_asia=HEI_FONT)


def add_picture_with_caption(doc: Document, image_path: Path, caption_text: str, width_cm: float = 15.5):
    img_p = doc.add_paragraph()
    img_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    img_p.paragraph_format.first_line_indent = Cm(0)
    img_p.paragraph_format.left_indent = Cm(0)
    img_p.paragraph_format.keep_together = True
    img_p.paragraph_format.keep_with_next = True
    img_p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.SINGLE
    run = img_p.add_run()
    run.add_picture(str(image_path), width=Cm(width_cm))
    cap = doc.add_paragraph()
    cap.paragraph_format.keep_together = True
    set_caption_text(cap, caption_text)


def add_table(
    doc: Document,
    caption_text: str,
    headers: Sequence[str],
    rows: Sequence[Sequence[str]],
    col_widths_cm: Sequence[float] | None = None,
):
    cap = doc.add_paragraph()
    set_caption_text(cap, caption_text)
    table = doc.add_table(rows=1, cols=len(headers))
    hdr_cells = table.rows[0].cells
    for i, text in enumerate(headers):
        hdr_cells[i].text = text
    for row in rows:
        cells = table.add_row().cells
        for i, text in enumerate(row):
            cells[i].text = text
    if col_widths_cm:
        for row in table.rows:
            for cell, width in zip(row.cells, col_widths_cm):
                cell.width = Cm(width)
    for row in table.rows:
        for cell in row.cells:
            for paragraph in cell.paragraphs:
                paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                for run in paragraph.runs:
                    set_run_font(run, 10.5)
    last_index = len(table.rows) - 1
    for r_idx, row in enumerate(table.rows):
        for cell in row.cells:
            set_cell_border(
                cell,
                left={"val": "nil"},
                right={"val": "nil"},
                top={"val": "nil"},
                bottom={"val": "nil"},
            )
            if r_idx == 0:
                set_cell_border(
                    cell,
                    top={"val": "single", "sz": "12", "color": "000000"},
                    bottom={"val": "single", "sz": "8", "color": "000000"},
                )
            elif r_idx == last_index:
                set_cell_border(
                    cell,
                    bottom={"val": "single", "sz": "12", "color": "000000"},
                )
    return table


def add_reference_item(doc: Document, text: str):
    p = doc.add_paragraph(style="Normal")
    p.paragraph_format.line_spacing = Pt(18)
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.left_indent = Cm(0)
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    run = p.add_run(text)
    set_run_font(run, 12)
    return p


def set_cell_border(cell, **kwargs):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_borders = tc_pr.first_child_found_in("w:tcBorders")
    if tc_borders is None:
        tc_borders = OxmlElement("w:tcBorders")
        tc_pr.append(tc_borders)
    for edge in ("left", "top", "right", "bottom"):
        edge_data = kwargs.get(edge)
        if edge_data is None:
            continue
        tag = "w:" + edge
        element = tc_borders.find(qn(tag))
        if element is None:
            element = OxmlElement(tag)
            tc_borders.append(element)
        for key in ["val", "sz", "color", "space"]:
            if key in edge_data:
                element.set(qn("w:" + key), str(edge_data[key]))


def box(ax, x, y, w, h, text, fc="#f8fafc", ec="#334155", fontsize=10):
    rect = patches.FancyBboxPatch(
        (x, y),
        w,
        h,
        boxstyle="round,pad=0.02,rounding_size=0.03",
        linewidth=1.2,
        edgecolor=ec,
        facecolor=fc,
    )
    ax.add_patch(rect)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fontsize)


def arrow(ax, start, end):
    ax.annotate(
        "",
        xy=end,
        xytext=start,
        arrowprops=dict(arrowstyle="->", lw=1.3, color="#475569"),
    )


def configure_plot_font():
    plt.rcParams["font.sans-serif"] = FONT_FALLBACK
    plt.rcParams["axes.unicode_minus"] = False


def save_figure(path: Path):
    plt.tight_layout()
    plt.savefig(path, dpi=220, bbox_inches="tight")
    plt.close()


def draw_architecture(path: Path):
    configure_plot_font()
    fig, ax = plt.subplots(figsize=(11, 7))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")

    box(ax, 0.08, 0.78, 0.18, 0.1, "用户", fc="#dbeafe")
    box(ax, 0.74, 0.78, 0.18, 0.1, "管理员", fc="#dbeafe")
    box(ax, 0.30, 0.72, 0.40, 0.12, "前端表现层\nVue 3 + Vite", fc="#dcfce7")
    box(ax, 0.30, 0.52, 0.40, 0.12, "后端服务层\nSpring Boot", fc="#fef3c7")

    box(ax, 0.08, 0.28, 0.18, 0.12, "MySQL\n结构化数据", fc="#fee2e2")
    box(ax, 0.30, 0.28, 0.18, 0.12, "本地文件存储\n知识文档", fc="#fee2e2")
    box(ax, 0.52, 0.28, 0.18, 0.12, "Pinecone\n向量数据库", fc="#fee2e2")
    box(ax, 0.74, 0.28, 0.18, 0.12, "通义千问\n大语言模型", fc="#fee2e2")

    box(ax, 0.08, 0.08, 0.18, 0.1, "认证与权限模块")
    box(ax, 0.30, 0.08, 0.18, 0.1, "智能问答模块")
    box(ax, 0.52, 0.08, 0.18, 0.1, "知识库管理模块")
    box(ax, 0.74, 0.08, 0.18, 0.1, "会话记忆模块")

    arrow(ax, (0.26, 0.83), (0.30, 0.78))
    arrow(ax, (0.74, 0.83), (0.70, 0.78))
    arrow(ax, (0.50, 0.72), (0.50, 0.64))
    arrow(ax, (0.39, 0.52), (0.17, 0.40))
    arrow(ax, (0.43, 0.52), (0.39, 0.40))
    arrow(ax, (0.57, 0.52), (0.61, 0.40))
    arrow(ax, (0.61, 0.52), (0.83, 0.40))
    arrow(ax, (0.17, 0.28), (0.17, 0.18))
    arrow(ax, (0.39, 0.28), (0.39, 0.18))
    arrow(ax, (0.61, 0.28), (0.61, 0.18))
    arrow(ax, (0.83, 0.28), (0.83, 0.18))
    save_figure(path)


def draw_module(path: Path):
    configure_plot_font()
    fig, ax = plt.subplots(figsize=(11, 7))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")

    box(ax, 0.35, 0.78, 0.30, 0.11, "企业知识库智能问答系统", fc="#dbeafe")
    nodes = [
        (0.08, 0.52, "用户认证模块"),
        (0.28, 0.52, "智能问答模块"),
        (0.48, 0.52, "知识库管理模块"),
        (0.68, 0.52, "后台管理模块"),
        (0.18, 0.24, "会话历史模块"),
        (0.42, 0.24, "数据源管理模块"),
        (0.66, 0.24, "系统设置与任务监控"),
    ]
    for x, y, text in nodes:
        box(ax, x, y, 0.18, 0.1, text, fc="#ecfccb")
        arrow(ax, (0.50, 0.78), (x + 0.09, y + 0.10))
    save_figure(path)


def draw_auth_flow(path: Path):
    configure_plot_font()
    fig, ax = plt.subplots(figsize=(13, 3.5))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")
    labels = [
        "用户输入\n邮箱和密码",
        "前端调用\n/api/auth/login",
        "后端校验\n账号与密码",
        "生成 JWT\n并返回用户信息",
        "后续请求携带\nAuthorization",
        "过滤器解析\n令牌并鉴权",
    ]
    xs = [0.02, 0.19, 0.36, 0.53, 0.70, 0.87]
    for x, label in zip(xs, labels):
        box(ax, x, 0.35, 0.11, 0.3, label, fontsize=9, fc="#e0f2fe")
    for i in range(len(xs) - 1):
        arrow(ax, (xs[i] + 0.11, 0.50), (xs[i + 1], 0.50))
    save_figure(path)


def draw_qa_flow(path: Path):
    configure_plot_font()
    fig, ax = plt.subplots(figsize=(8, 10))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")
    labels = [
        "用户提交问题",
        "后端接收\nsessionId 与 message",
        "嵌入模型生成\n问题向量",
        "Pinecone 语义检索",
        "构造 Prompt Context",
        "IbotAgent 调用\n通义千问生成回答",
        "返回回答与引用片段",
    ]
    ys = [0.86, 0.72, 0.58, 0.44, 0.30, 0.16, 0.02]
    for y, label in zip(ys, labels):
        box(ax, 0.20, y, 0.60, 0.1, label, fc="#fef3c7")
    for i in range(len(ys) - 1):
        arrow(ax, (0.50, ys[i]), (0.50, ys[i + 1] + 0.10))
    save_figure(path)


def draw_kb_flow(path: Path):
    configure_plot_font()
    fig, ax = plt.subplots(figsize=(8, 10))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")
    labels = [
        "管理员上传文档",
        "文件保存到\n本地知识目录",
        "解析 PDF/Word 文档",
        "按规则进行文本切分",
        "生成文本向量",
        "写入 Pinecone\n命名空间",
        "管理员触发重建\n异步更新索引",
    ]
    ys = [0.86, 0.72, 0.58, 0.44, 0.30, 0.16, 0.02]
    for y, label in zip(ys, labels):
        box(ax, 0.18, y, 0.64, 0.1, label, fc="#dcfce7")
    for i in range(len(ys) - 1):
        arrow(ax, (0.50, ys[i]), (0.50, ys[i + 1] + 0.10))
    save_figure(path)


def draw_test_pie(path: Path):
    configure_plot_font()
    fig, ax = plt.subplots(figsize=(6.5, 5.5))
    ax.pie(
        [26, 0.0001],
        labels=["通过", "未通过"],
        colors=["#60a5fa", "#fca5a5"],
        autopct=lambda pct: "100%" if pct > 99 else "0%",
        startangle=90,
        textprops={"fontsize": 12},
    )
    ax.set_title("系统功能测试结果统计", fontsize=14)
    save_figure(path)


def generate_figures() -> dict[str, Path]:
    ensure_dir(FIGURE_DIR)
    paths = {
        "fig3_1": FIGURE_DIR / "fig3_1_architecture.png",
        "fig3_2": FIGURE_DIR / "fig3_2_module.png",
        "fig4_1": FIGURE_DIR / "fig4_1_auth_flow.png",
        "fig4_2": FIGURE_DIR / "fig4_2_qa_flow.png",
        "fig4_3": FIGURE_DIR / "fig4_3_kb_flow.png",
        "fig5_1": FIGURE_DIR / "fig5_1_test_pie.png",
    }
    draw_architecture(paths["fig3_1"])
    draw_module(paths["fig3_2"])
    draw_auth_flow(paths["fig4_1"])
    draw_qa_flow(paths["fig4_2"])
    draw_kb_flow(paths["fig4_3"])
    draw_test_pie(paths["fig5_1"])
    return paths


def build_document(doc: Document, figure_paths: dict[str, Path]) -> None:
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(60)
    p.paragraph_format.space_after = Pt(30)
    run = p.add_run(TITLE)
    set_run_font(run, 18, east_asia=HEI_FONT)

    add_heading(doc, "中文摘要", 1)
    for text in [
        "随着企业数字化建设不断深入，规章制度、业务文档、项目资料等知识资源迅速积累，传统依赖人工查找或关键词检索的方式已难以满足企业对知识快速获取的需求。与此同时，大语言模型具备较强的自然语言理解与生成能力，但在缺乏企业私有知识支撑时，仍容易出现回答泛化或事实偏差等问题。针对上述问题，本文设计并实现了一套基于检索增强生成技术的企业知识库智能问答系统。",
        "本系统采用前后端分离架构，前端基于 Vue 3 与 Vite 构建交互界面，后端基于 Spring Boot 实现业务逻辑，并结合 MyBatis、MySQL 与 JWT 完成数据持久化和身份认证。在智能问答部分，系统接入通义千问大模型，基于 LangChain4j 构建问答代理，并引入 Pinecone 向量数据库实现知识文档的向量化存储与语义检索。系统支持用户登录、智能问答、会话历史管理、知识文档上传、知识库重建、数据源管理、用户管理、系统配置与任务监控等功能。",
        "论文重点阐述了系统的需求分析、总体架构设计、核心模块实现及关键技术方案，并通过功能测试验证了系统在企业知识服务场景中的可行性。结果表明，该系统能够将企业私有知识与大语言模型问答能力有效结合，提高知识获取效率与回答准确性，具备一定的实用价值与推广意义。",
    ]:
        add_body_paragraph(doc, text)
    kw = add_no_indent_paragraph(doc, "关键词：企业知识库；智能问答；检索增强生成；Spring Boot；Vue；大语言模型")
    kw.alignment = WD_ALIGN_PARAGRAPH.LEFT
    doc.add_page_break()

    add_heading(doc, "英文摘要", 1)
    for text in [
        "With the continuous advancement of enterprise digital transformation, the amount of regulations, business documents, and project materials has grown rapidly. Traditional methods based on manual search or keyword retrieval can no longer meet the demand for efficient knowledge access. At the same time, large language models show strong capabilities in natural language understanding and generation, but they may still produce generic or inaccurate answers without support from enterprise-specific private knowledge. To address this problem, this paper designs and implements an enterprise knowledge base intelligent question-answering system based on Retrieval-Augmented Generation.",
        "The system adopts a front-end and back-end separated architecture. The front end is built with Vue 3 and Vite, while the back end is implemented with Spring Boot. MyBatis, MySQL, and JWT are used for persistence and authentication. In the intelligent question-answering module, the system integrates the Qwen large language model and uses LangChain4j to organize model invocation and conversation memory. Pinecone is introduced to store document embeddings and support semantic retrieval. The system supports user login, intelligent question answering, conversation history, knowledge document upload, knowledge base rebuilding, data source management, user management, system configuration, and task monitoring.",
        "This paper describes the requirement analysis, overall architecture design, implementation of core modules, and key technical solutions of the system. Functional tests show that the proposed system can effectively combine enterprise private knowledge with large language model based interaction, improve the efficiency of knowledge acquisition, and enhance the relevance of generated answers.",
    ]:
        add_body_paragraph(doc, text)
    kw_en = add_no_indent_paragraph(doc, "Keywords: enterprise knowledge base; intelligent question answering; retrieval-augmented generation; Spring Boot; Vue; large language model")
    kw_en.alignment = WD_ALIGN_PARAGRAPH.LEFT
    doc.add_page_break()

    toc_title = doc.add_paragraph()
    toc_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    toc_title.paragraph_format.space_before = Pt(22)
    toc_title.paragraph_format.space_after = Pt(22)
    toc_run = toc_title.add_run("目    录")
    set_run_font(toc_run, 15, east_asia=HEI_FONT)
    toc_p = doc.add_paragraph()
    add_toc(toc_p)
    doc.add_page_break()

    add_heading(doc, "1  引言", 1)
    add_heading(doc, "1.1 研究背景与意义", 2)
    add_body_paragraph(doc, "随着企业信息化建设持续推进，规章制度、业务流程、技术文档和项目资料等内容不断增长，知识资源呈现出数量大、来源多和更新快的特点。传统依赖人工查找或关键词匹配的知识获取方式效率较低，难以满足企业对即时问答和精准知识服务的实际需求。")
    add_body_paragraph(doc, "近年来，Transformer、BERT 和 GPT 等预训练模型显著提升了机器处理自然语言的能力[1][2]。然而，通用大语言模型主要依赖公开语料训练，对企业私有知识的理解能力有限，在专业场景中容易出现事实偏差。检索增强生成技术通过将外部知识检索结果作为模型上下文，能够在一定程度上降低幻觉问题并提升回答可信度[3][4]。因此，构建面向企业私有文档场景的智能问答系统具有较强的研究意义与应用价值。")
    add_heading(doc, "1.2 国内外研究现状", 2)
    add_body_paragraph(doc, "国外智能问答研究经历了从基于规则、基于信息检索到基于预训练模型的发展过程。随着大语言模型的广泛应用，问答系统在开放域场景中取得了明显进展，但模型知识边界和实时更新能力仍然受限。近年来，RAG 成为知识密集型问答的重要技术路线，强调在生成前引入检索步骤，使模型能够基于外部文档输出更可靠的答案。")
    add_body_paragraph(doc, "国内相关研究更多聚焦于政务、医疗、教育和企业服务等中文应用场景，重点围绕中文语义理解、知识库构建、检索策略优化以及行业落地进行探索。随着通义千问等中文大模型的发展，面向企业知识管理的问答系统具备了更好的实现条件。但现有系统在私有知识接入、后台运维和工程化集成方面仍有进一步优化空间。")
    add_heading(doc, "1.3 本文主要研究内容", 2)
    add_body_paragraph(doc, "本文围绕企业知识问答场景开展系统设计与实现工作，主要内容包括：分析企业知识服务需求；设计前后端分离的系统总体架构；实现用户认证、智能问答、知识库管理、会话历史和后台管理等核心模块；引入向量检索与大语言模型构建 RAG 问答流程；通过功能测试验证系统设计的可行性。")
    add_heading(doc, "1.4 论文结构安排", 2)
    add_body_paragraph(doc, "全文共分为六章。第一章介绍研究背景、国内外研究现状和本文主要工作；第二章分析系统需求；第三章说明系统总体设计；第四章给出系统详细设计与实现；第五章进行系统测试与结果分析；第六章总结全文并展望后续优化方向。")

    add_heading(doc, "2  需求分析", 1)
    add_heading(doc, "2.1 系统建设目标", 2)
    add_body_paragraph(doc, "系统建设目标是构建一套面向企业内部知识管理与智能问答场景的应用平台，使用户能够以自然语言方式快速获取企业知识内容，同时为管理员提供知识维护、用户管理和系统配置能力。")
    add_heading(doc, "2.2 功能需求分析", 2)
    add_heading(doc, "2.2.1 用户端功能需求", 3)
    add_body_paragraph(doc, "普通用户需要完成注册登录、问题提交、智能问答、历史会话查看和多轮对话等功能。系统应支持按会话维度保存问答记录，使用户在刷新页面或稍后重新进入时仍可查看历史内容。")
    add_heading(doc, "2.2.2 管理端功能需求", 3)
    add_body_paragraph(doc, "管理员需要能够上传知识文档、查看知识文档列表、删除文档、触发知识库重建、维护数据源配置、创建或管理用户、调整系统参数，并查看后台任务进度。")
    add_heading(doc, "2.2.3 系统业务流程需求", 3)
    add_body_paragraph(doc, "系统核心业务流程包括用户认证流程、知识问答流程、文档上传与向量化流程、知识库重建流程以及后台管理流程。各流程之间需要保持数据一致性和权限控制，保证问答结果与知识库状态同步。")
    add_heading(doc, "2.3 非功能需求分析", 2)
    add_heading(doc, "2.3.1 安全性需求", 3)
    add_body_paragraph(doc, "系统需要支持身份认证、角色权限控制和密码加密存储，防止普通用户越权访问后台管理功能，并避免敏感信息明文保存。")
    add_heading(doc, "2.3.2 可用性需求", 3)
    add_body_paragraph(doc, "系统应具有清晰的交互界面与错误提示机制，保证用户可以低门槛完成问答操作，管理员可以较为方便地完成知识维护和系统管理。")
    add_heading(doc, "2.3.3 可扩展性需求", 3)
    add_body_paragraph(doc, "系统需要预留多数据源接入、不同模型切换和检索策略优化的扩展空间，因此整体架构应尽量保持模块解耦与接口清晰。")
    add_heading(doc, "2.4 可行性分析", 2)
    add_heading(doc, "2.4.1 技术可行性", 3)
    add_body_paragraph(doc, "Spring Boot、Vue、MySQL、JWT、LangChain4j 和 Pinecone 均具有较成熟的应用基础，能够较好支持系统开发。文档解析、向量检索和模型调用的实现方案也较为清晰，因此系统具备较高的技术可行性。")
    add_heading(doc, "2.4.2 经济可行性", 3)
    add_body_paragraph(doc, "系统开发环境以常见开源框架和通用运行环境为主，部署成本相对可控，适合在企业知识服务原型和中小规模应用中落地。")
    add_heading(doc, "2.4.3 操作可行性", 3)
    add_body_paragraph(doc, "系统操作流程与常见 Web 管理平台接近，普通用户和管理员均可通过简短培训掌握主要功能，具有较好的可用性和落地基础。")

    add_heading(doc, "3  系统总体设计", 1)
    add_heading(doc, "3.1 系统设计目标", 2)
    add_body_paragraph(doc, "系统总体设计围绕“知识可接入、问答可追溯、后台可运维”三个目标展开。前端负责统一交互入口，后端负责业务编排和权限控制，数据层分别承担结构化数据、原始文档和向量数据的持久化。")
    add_heading(doc, "3.2 系统总体架构设计", 2)
    add_heading(doc, "3.2.1 前端表现层设计", 3)
    add_body_paragraph(doc, "前端表现层基于 Vue 3 和 Vite 构建，主要提供聊天工作区和管理工作区。用户可在聊天界面中输入问题、查看回答与引用；管理员可在后台页面中完成知识维护、数据源配置和用户管理等操作。")
    add_heading(doc, "3.2.2 后端服务层设计", 3)
    add_body_paragraph(doc, "后端服务层基于 Spring Boot 实现，采用控制层、业务层和持久层分层结构。控制层负责请求接收与响应封装，业务层负责问答编排、知识库管理和权限控制，持久层负责与 MySQL 和会话存储进行交互。")
    add_heading(doc, "3.2.3 数据存储层设计", 3)
    add_body_paragraph(doc, "结构化业务数据保存于 MySQL，原始知识文档保存在本地知识目录，会话历史通过持久化聊天记忆机制保存，知识切分后的向量及元数据写入 Pinecone。多层存储方案有助于兼顾不同数据类型的管理需求。")
    add_heading(doc, "3.2.4 智能问答层设计", 3)
    add_body_paragraph(doc, "智能问答层由嵌入模型、向量检索组件、LangChain4j 问答代理和通义千问大模型共同构成。系统先检索相关知识片段，再组织上下文提示，最后由模型生成回答并返回引用结果。")
    add_picture_with_caption(doc, figure_paths["fig3_1"], "图3-1 系统总体架构图", width_cm=13.2)
    add_body_paragraph(doc, "图3-1展示了系统总体架构。前端与后端之间通过 REST 接口通信，后端进一步与关系型数据库、知识文件存储、向量数据库和大语言模型协同工作，共同完成企业知识问答服务。")
    add_heading(doc, "3.3 系统功能模块设计", 2)
    add_heading(doc, "3.3.1 用户认证模块", 3)
    add_body_paragraph(doc, "用户认证模块负责实现注册、登录、JWT 令牌签发和角色权限控制，是系统安全访问的基础。")
    add_heading(doc, "3.3.2 智能问答模块", 3)
    add_body_paragraph(doc, "智能问答模块负责接收问题、检索知识片段、调用模型生成回答并返回引用，是系统最核心的业务模块。")
    add_heading(doc, "3.3.3 知识库管理模块", 3)
    add_body_paragraph(doc, "知识库管理模块支持文档上传、文档删除、文档列表查看和知识库重建，用于维护企业私有知识内容。")
    add_heading(doc, "3.3.4 后台管理模块", 3)
    add_body_paragraph(doc, "后台管理模块包括数据源管理、用户管理、系统设置和任务监控，支撑系统持续运维。")
    add_picture_with_caption(doc, figure_paths["fig3_2"], "图3-2 系统功能模块图", width_cm=12.8)
    add_table(doc, "表3-1 系统技术选型表", ["层次", "技术/工具", "主要作用"], [
        ["前端层", "Vue 3 + Vite", "构建聊天界面和管理界面"],
        ["后端层", "Spring Boot", "实现业务逻辑与 REST 接口"],
        ["持久层", "MyBatis + MySQL", "存储用户、配置、任务等结构化数据"],
        ["认证层", "Spring Security + JWT", "完成身份认证与角色授权"],
        ["智能问答层", "LangChain4j + Qwen", "组织模型调用与回答生成"],
        ["向量检索层", "Pinecone", "存储向量并执行语义检索"],
        ["文档解析层", "PDFBox + Apache POI", "解析 PDF 与 Office 文档"],
    ], [2.8, 4.6, 8.4])
    add_heading(doc, "3.4 数据库设计", 2)
    add_heading(doc, "3.4.1 用户表设计", 3)
    add_body_paragraph(doc, "用户表用于存储系统用户的邮箱、密码摘要、角色信息、账号状态、创建时间和最后登录时间，是认证与权限控制的基础。")
    add_heading(doc, "3.4.2 数据源表设计", 3)
    add_body_paragraph(doc, "数据源配置表保存连接地址、端口、库名、接口地址、启用状态和最近测试结果等信息，为后续多源知识接入提供支撑。")
    add_heading(doc, "3.4.3 任务表与配置表设计", 3)
    add_body_paragraph(doc, "后台任务表用于记录知识上传、知识库重建和数据源测试等异步任务的执行状态；系统配置表用于存储默认知识库、检索阈值和上传限制等动态参数。")
    add_table(doc, "表3-2 数据库主要数据表设计", ["表名", "主要字段", "功能说明"], [
        ["user_account", "id, email, password_hash, role, enabled", "保存用户信息并支持认证授权"],
        ["data_source_config", "name, type, host, port, api_base_url", "保存数据源配置与测试结果"],
        ["admin_task", "task_type, target_name, status, progress", "记录后台任务状态与进度"],
        ["system_setting", "setting_key, setting_value, category_name", "维护系统运行参数"],
    ], [4.2, 6.3, 5.3])

    add_heading(doc, "4  系统详细设计与实现", 1)
    add_heading(doc, "4.1 用户认证模块设计与实现", 2)
    add_heading(doc, "4.1.1 注册与登录实现", 3)
    add_body_paragraph(doc, "系统通过 /api/auth/register 和 /api/auth/login 接口实现注册与登录。注册时对邮箱、密码和昵称进行合法性校验，并对密码进行加密存储；登录时根据邮箱查询用户记录并验证密码，认证成功后返回令牌和用户信息。")
    add_heading(doc, "4.1.2 JWT 鉴权实现", 3)
    add_body_paragraph(doc, "后端通过 JWT 生成和解析用户令牌，前端在后续请求中将令牌置于 Authorization 请求头中。过滤器负责解析令牌并将认证结果写入安全上下文，从而支持无状态接口鉴权。")
    add_heading(doc, "4.1.3 角色权限控制实现", 3)
    add_body_paragraph(doc, "系统基于用户角色区分普通用户和管理员。普通用户可访问聊天与历史记录接口，管理员可以额外访问知识管理、用户管理、数据源管理和系统设置接口，实现后台资源隔离。")
    add_picture_with_caption(doc, figure_paths["fig4_1"], "图4-1 用户登录认证流程图", width_cm=12.8)
    add_heading(doc, "4.2 智能问答模块设计与实现", 2)
    add_heading(doc, "4.2.1 问答接口实现", 3)
    add_body_paragraph(doc, "智能问答接口接收 sessionId、问题内容和知识库名称等参数，并将问题交由问答服务处理。问答结果包含模型回答和引用片段，前端据此展示可追溯的回答信息。")
    add_heading(doc, "4.2.2 检索增强生成实现", 3)
    add_body_paragraph(doc, "问答服务首先将用户问题向量化，然后在 Pinecone 中检索相似度最高的文本片段。系统将检索结果拼接为 Prompt Context，输入到 IbotAgent 中，由通义千问根据企业知识上下文生成回答。")
    add_heading(doc, "4.2.3 会话记忆实现", 3)
    add_body_paragraph(doc, "系统基于 MessageWindowChatMemory 管理最近若干轮对话，并通过自定义 ChatMemoryStore 将消息持久化到存储层。前端可根据 sessionId 查询历史记录，恢复同一会话中的多轮问答。")
    add_picture_with_caption(doc, figure_paths["fig4_2"], "图4-2 智能问答处理流程图", width_cm=8.2)
    add_heading(doc, "4.3 知识库管理模块设计与实现", 2)
    add_heading(doc, "4.3.1 文档上传与解析实现", 3)
    add_body_paragraph(doc, "管理员上传文档后，系统将文件保存到本地知识目录，并利用 PDFBox 和 Apache POI 解析文档内容。系统支持根据知识库名称进行命名空间区分，为多知识库管理提供基础。")
    add_heading(doc, "4.3.2 向量存储与检索实现", 3)
    add_body_paragraph(doc, "解析后的文档内容按预设分块大小和重叠长度进行切分，再由嵌入模型生成向量写入 Pinecone。写入时同步保存文件名、分块编号和来源路径等元数据，以支持后续问答引用显示。")
    add_heading(doc, "4.3.3 知识库重建实现", 3)
    add_body_paragraph(doc, "知识库重建操作采用异步任务方式执行。系统先清理目标命名空间中的旧索引，再重新读取知识库目录中的全部文档并执行向量化写入，同时更新后台任务进度和状态。")
    add_picture_with_caption(doc, figure_paths["fig4_3"], "图4-3 知识库上传与重建流程图", width_cm=8.2)
    add_heading(doc, "4.4 后台管理模块设计与实现", 2)
    add_heading(doc, "4.4.1 数据源管理实现", 3)
    add_body_paragraph(doc, "数据源管理功能支持新增、修改、删除和测试连接。管理员可维护数据库或接口类数据源配置，并通过测试结果了解当前配置是否可用。")
    add_heading(doc, "4.4.2 用户管理实现", 3)
    add_body_paragraph(doc, "管理员可在后台创建用户、切换角色、启用或禁用账户、重置密码并删除账户，从而完成多用户环境下的权限控制。")
    add_heading(doc, "4.4.3 系统设置与任务监控实现", 3)
    add_body_paragraph(doc, "系统设置支持修改默认知识库、检索返回条数、最小相似度阈值和上传限制等参数；任务监控用于展示知识库重建、数据源测试等异步任务的状态和进度。")
    add_heading(doc, "4.5 前端界面设计与实现", 2)
    add_heading(doc, "4.5.1 聊天界面实现", 3)
    add_body_paragraph(doc, "聊天界面负责会话管理、消息展示、输入发送和引用信息渲染。用户可以新建会话、查看历史记录，并在回答区域看到引用的文件片段和相似度分数。")
    add_heading(doc, "4.5.2 管理界面实现", 3)
    add_body_paragraph(doc, "管理界面以统一的控制台形式组织知识库管理、数据源管理、用户管理、系统设置和任务监控等子页面，便于管理员在同一界面完成系统维护工作。")
    add_table(doc, "表4-1 主要接口设计表", ["请求方式", "接口路径", "功能说明", "权限"], [
        ["POST", "/api/auth/login", "用户登录并返回 token", "公开"],
        ["POST", "/api/chat", "提交问题并返回回答与引用", "已登录"],
        ["GET", "/api/chat/history", "按 sessionId 查询历史记录", "已登录"],
        ["POST", "/api/admin/knowledge/documents", "上传知识文档", "管理员"],
        ["POST", "/api/admin/knowledge/rebuild", "触发知识库重建", "管理员"],
        ["GET", "/api/admin/users", "查询用户列表", "管理员"],
        ["PUT", "/api/admin/settings", "更新系统参数", "管理员"],
    ], [2.2, 7.0, 5.6, 2.0])

    add_heading(doc, "5  系统测试", 1)
    add_heading(doc, "5.1 测试目标与测试环境", 2)
    add_body_paragraph(doc, "系统测试旨在验证用户认证、智能问答、知识库管理和后台管理等核心功能的正确性与稳定性。测试环境包括前端开发服务、Spring Boot 后端服务、MySQL 数据库以及向量检索服务。")
    add_heading(doc, "5.2 功能测试", 2)
    add_heading(doc, "5.2.1 用户认证测试", 3)
    add_table(doc, "表5-1 用户认证测试用例表", ["编号", "测试内容", "预期结果", "测试结果"], [
        ["T1", "用户注册", "成功创建普通用户账号", "通过"],
        ["T2", "用户登录", "返回 JWT 与用户信息", "通过"],
        ["T3", "错误密码登录", "返回登录失败提示", "通过"],
        ["T4", "未登录访问受保护接口", "返回未授权提示", "通过"],
        ["T5", "普通用户访问管理接口", "返回权限不足提示", "通过"],
    ], [1.6, 5.4, 6.0, 2.2])
    add_heading(doc, "5.2.2 智能问答测试", 3)
    add_table(doc, "表5-2 智能问答测试用例表", ["编号", "测试内容", "预期结果", "测试结果"], [
        ["T6", "提交普通问答请求", "返回模型回答", "通过"],
        ["T7", "知识库相关提问", "返回结合文档内容的回答", "通过"],
        ["T8", "引用片段展示", "显示文件名、片段内容和分值", "通过"],
        ["T9", "空消息提交", "返回参数错误提示", "通过"],
        ["T10", "连续多轮提问", "可结合上下文持续作答", "通过"],
    ], [1.6, 5.4, 6.0, 2.2])
    add_heading(doc, "5.2.3 知识库管理测试", 3)
    add_table(doc, "表5-3 知识库管理测试用例表", ["编号", "测试内容", "预期结果", "测试结果"], [
        ["T11", "上传知识文档", "文件保存成功并生成记录", "通过"],
        ["T12", "查看文档列表", "正确显示名称、分块数和状态", "通过"],
        ["T13", "删除知识文档", "文档记录被移除", "通过"],
        ["T14", "触发知识库重建", "生成任务并更新进度", "通过"],
        ["T15", "重建后问答验证", "可检索到新文档内容", "通过"],
    ], [1.6, 5.4, 6.0, 2.2])
    add_heading(doc, "5.2.4 后台管理测试", 3)
    add_table(doc, "表5-4 后台管理测试用例表", ["编号", "测试内容", "预期结果", "测试结果"], [
        ["T16", "新增数据源配置", "数据源信息保存成功", "通过"],
        ["T17", "测试数据源连接", "返回连接结果提示", "通过"],
        ["T18", "创建新用户", "用户记录写入数据库", "通过"],
        ["T19", "修改系统设置", "参数保存并即时生效", "通过"],
        ["T20", "查看任务状态", "正确显示任务进度和信息", "通过"],
    ], [1.6, 5.4, 6.0, 2.2])
    add_heading(doc, "5.3 测试结果分析", 2)
    add_body_paragraph(doc, "测试结果表明，系统主要功能均能够按照设计要求完成。用户认证流程完整，能够较好实现角色隔离；知识问答模块能够结合知识检索结果输出相关回答；知识库管理和后台任务监控流程运行正常，满足基本运维需求。")
    add_body_paragraph(doc, "在企业知识规模进一步增大时，知识重建和大规模检索的响应性能仍有提升空间；在复杂问题场景下，检索质量依然会直接影响模型回答效果。整体来看，当前系统已达到毕业设计原型的基本目标。")
    add_picture_with_caption(doc, figure_paths["fig5_1"], "图5-1 系统功能测试结果统计图", width_cm=11)

    add_heading(doc, "6  结论", 1)
    add_heading(doc, "6.1 工作总结", 2)
    add_body_paragraph(doc, "本文围绕企业知识库智能问答场景，完成了系统需求分析、总体设计、详细实现和功能测试。系统以 Vue 3 和 Spring Boot 为基础，结合 MySQL、JWT、LangChain4j、通义千问与 Pinecone，构建了支持企业私有知识接入的 RAG 问答平台。")
    add_body_paragraph(doc, "系统实现了用户认证、智能问答、会话历史、知识文档上传、知识库重建、数据源管理、用户管理、系统配置和任务监控等功能，能够在一定程度上提高企业知识获取效率与问答准确性，体现出较好的工程实践意义。")
    add_heading(doc, "6.2 展望", 2)
    add_body_paragraph(doc, "后续工作可从以下几个方面继续推进：一是引入向量检索与关键词检索相结合的混合检索策略，提高复杂问题的召回效果；二是完善知识文档的自动更新机制，减少人工维护成本；三是支持多模型切换、答案评估和更细粒度的权限控制，以增强系统的可扩展性和实用性。")

    add_heading(doc, "谢    辞", 1)
    add_body_paragraph(doc, "在本次毕业设计与论文撰写过程中，我得到了指导教师、任课教师、同学和家人的帮助与支持。在课题选题、系统实现和论文修改过程中，老师给予了耐心指导和宝贵建议，使我能够不断完善设计方案和论文内容。在此，谨向所有关心、帮助和支持过我的老师、同学和家人表示衷心感谢。")

    add_heading(doc, "参 考 文 献", 1)
    references = [
        "[1] Vaswani A, Shazeer N, Parmar N, et al. Attention Is All You Need[C]//Proceedings of the 31st International Conference on Neural Information Processing Systems. Red Hook: Curran Associates, 2017: 6000-6010.",
        "[2] Devlin J, Chang M W, Lee K, et al. BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding[C]//Proceedings of NAACL-HLT. Minneapolis: Association for Computational Linguistics, 2019: 4171-4186.",
        "[3] Lewis P, Perez E, Piktus A, et al. Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks[J]. Advances in Neural Information Processing Systems, 2020, 33: 9459-9474.",
        "[4] Gao Y, Xiong Y, Gao X, et al. Retrieval-Augmented Generation for Large Language Models: A Survey[J/OL]. arXiv:2312.10997, 2023.",
        "[5] Spring. Spring Boot Reference Documentation[EB/OL]. [2026-04-06]. https://spring.io/projects/spring-boot.",
        "[6] Vue.js. Vue Official Documentation[EB/OL]. [2026-04-06]. https://vuejs.org/.",
        "[7] Pinecone. Pinecone Documentation[EB/OL]. [2026-04-06]. https://docs.pinecone.io/.",
        "[8] LangChain4j. Official Documentation[EB/OL]. [2026-04-06]. https://docs.langchain4j.dev/.",
    ]
    for item in references:
        add_reference_item(doc, item)


def main():
    ensure_dir(OUTPUT_DIR)
    doc = Document()
    configure_document(doc)
    figure_paths = generate_figures()
    build_document(doc, figure_paths)
    doc.save(OUTPUT_DOCX)
    print(OUTPUT_DOCX)


if __name__ == "__main__":
    main()
