#!/usr/bin/env python3
"""Reference implementation: every ODT construct that is easy to get wrong.

Run:  python3 example_report.py out.odt
Then: python3 odt_verify.py out.odt   (renders to images so you can look at it)
"""
import sys

from odf.opendocument import OpenDocumentText
from odf.style import (
    Style, TextProperties, ParagraphProperties, GraphicProperties,
    TableProperties, TableColumnProperties, TableRowProperties, TableCellProperties,
    PageLayout, PageLayoutProperties, MasterPage, Header, Footer,
    FontFace, TabStops, TabStop, ListLevelProperties, ListLevelLabelAlignment,
    Columns, Column,
)
from odf.text import (
    H, P, Span, PageNumber, PageCount, List, ListItem, ListStyle,
    ListLevelStyleBullet, ListLevelStyleNumber,
    TableOfContent, TableOfContentSource, IndexBody, IndexTitle,
    TableOfContentEntryTemplate, IndexTitleTemplate, IndexEntryChapter, IndexEntryText,
    IndexEntryTabStop, IndexEntryPageNumber, IndexEntryLinkStart, IndexEntryLinkEnd,
    LineBreak, SoftPageBreak,
)
from odf.table import Table, TableColumn, TableRow, TableCell
from odf.draw import Frame, Image

doc = OpenDocumentText()

# ---------------------------------------------------------------- 1. FONTS
# A font must be DECLARED in <office:font-face-decls> or LibreOffice falls back
# to the default face even though your TextProperties names it.
for name, family, gen, pitch in [
    ("Liberation Serif", "'Liberation Serif'", "roman", "variable"),
    ("Liberation Sans", "'Liberation Sans'", "swiss", "variable"),
    ("Liberation Mono", "'Liberation Mono'", "modern", "fixed"),
]:
    doc.fontfacedecls.addElement(
        FontFace(name=name, fontfamily=family,
                 fontfamilygeneric=gen, fontpitch=pitch)
    )

# ------------------------------------------------- 2. NAMED PARAGRAPH STYLES
# doc.styles  -> named styles: reusable, show up in the LibreOffice sidebar.
# doc.automaticstyles -> one-off direct formatting, invisible in the UI.
# Page layouts ALWAYS go in automaticstyles, never in styles.

body = Style(name="Text body", family="paragraph")
body.addElement(TextProperties(fontname="Liberation Serif", fontsize="11pt"))
body.addElement(ParagraphProperties(
    margintop="0cm", marginbottom="0.25cm", textalign="justify",
    lineheight="140%", orphans="2", widows="2"))
doc.styles.addElement(body)

# parentstylename inherits; only override what differs.
h1 = Style(name="Heading 1", family="paragraph", parentstylename=body,
           nextstylename=body)
h1.addElement(TextProperties(fontname="Liberation Sans", fontsize="20pt",
                             fontweight="bold", color="#1a3d5c"))
h1.addElement(ParagraphProperties(
    margintop="0.8cm", marginbottom="0.3cm", textalign="left",
    borderbottom="0.06pt solid #1a3d5c", paddingbottom="0.1cm",
    keepwithnext="always"))
doc.styles.addElement(h1)

h2 = Style(name="Heading 2", family="paragraph", parentstylename=body,
           nextstylename=body)
h2.addElement(TextProperties(fontname="Liberation Sans", fontsize="14pt",
                             fontweight="bold", color="#2f6690"))
h2.addElement(ParagraphProperties(margintop="0.5cm", marginbottom="0.2cm",
                                  textalign="left", keepwithnext="always"))
doc.styles.addElement(h2)

code = Style(name="Code", family="paragraph", parentstylename=body)
code.addElement(TextProperties(fontname="Liberation Mono", fontsize="9.5pt"))
code.addElement(ParagraphProperties(
    backgroundcolor="#f2f2f2", padding="0.2cm", marginleft="0.5cm",
    textalign="start", border="0.5pt solid #cccccc"))
doc.styles.addElement(code)

# --------------------------------------------------------- 3. CHARACTER STYLES
strong = Style(name="Strong", family="text")
strong.addElement(TextProperties(fontweight="bold"))
doc.styles.addElement(strong)

lit = Style(name="Literal", family="text")
lit.addElement(TextProperties(fontname="Liberation Mono", fontsize="9.5pt",
                              backgroundcolor="#eeeeee"))
doc.styles.addElement(lit)

# ------------------------------------------- 4. PAGE LAYOUT + HEADER + FOOTER
pl = PageLayout(name="Standard-PL")
pl.addElement(PageLayoutProperties(
    pagewidth="21.0cm", pageheight="29.7cm", printorientation="portrait",
    margintop="2.5cm", marginbottom="2.0cm",
    marginleft="2.2cm", marginright="2.2cm",
    writingmode="lr-tb"))
doc.automaticstyles.addElement(pl)          # <-- automaticstyles, always

# Right-aligned page number needs a tab stop at the right margin
# (page width 21 - 2.2 - 2.2 = 16.6cm of text width).
foot_style = Style(name="FooterP", family="paragraph")
foot_style.addElement(TextProperties(fontname="Liberation Sans", fontsize="8pt",
                                     color="#666666"))
tabs = TabStops()
tabs.addElement(TabStop(position="8.3cm", type="center"))
tabs.addElement(TabStop(position="16.6cm", type="right"))
fp = ParagraphProperties(bordertop="0.5pt solid #cccccc", paddingtop="0.15cm")
fp.addElement(tabs)
foot_style.addElement(fp)
doc.automaticstyles.addElement(foot_style)

head_style = Style(name="HeaderP", family="paragraph")
head_style.addElement(TextProperties(fontname="Liberation Sans", fontsize="8pt",
                                     color="#666666", fontstyle="italic"))
doc.automaticstyles.addElement(head_style)

mp = MasterPage(name="Standard", pagelayoutname=pl)

hdr = Header()
hdr.addElement(P(stylename=head_style, text="Quarterly Engineering Report — Confidential"))
mp.addElement(hdr)

ftr = Footer()
fp_par = P(stylename=foot_style)
fp_par.addText("Acme Corp\t")
fp_par.addElement(PageNumber(selectpage="current"))   # field, not literal text
fp_par.addText(" / ")
fp_par.addElement(PageCount())
fp_par.addText("\t2026-07-29")
ftr.addElement(fp_par)
mp.addElement(ftr)

doc.masterstyles.addElement(mp)

# ------------------------------------------------------------- 5. LIST STYLE
bullets = ListStyle(name="BulletList")
for lvl in (1, 2, 3):
    b = ListLevelStyleBullet(level=str(lvl), bulletchar="•▪‣"[lvl - 1],
                             stylename="Bullet_20_Symbols")
    llp = ListLevelProperties(listlevelpositionandspacemode="label-alignment")
    llp.addElement(ListLevelLabelAlignment(
        labelfollowedby="listtab",
        listtabstopposition="%.2fcm" % (0.6 * lvl + 0.4),
        textindent="-0.4cm",
        marginleft="%.2fcm" % (0.6 * lvl + 0.4)))
    b.addElement(llp)
    bullets.addElement(b)
doc.styles.addElement(bullets)

# ------------------------------------------------------------ 6. TABLE STYLES
tstyle = Style(name="DataTable", family="table")
tstyle.addElement(TableProperties(width="16.6cm", align="margins"))
doc.automaticstyles.addElement(tstyle)

# One column style per width. Widths must sum to the table width.
colw = {"A": "6.6cm", "B": "3.5cm", "C": "3.5cm", "D": "3.0cm"}
for key, w in colw.items():
    cs = Style(name="DataTable.%s" % key, family="table-column")
    cs.addElement(TableColumnProperties(columnwidth=w))
    doc.automaticstyles.addElement(cs)

hdr_row = Style(name="DataTable.HR", family="table-row")
hdr_row.addElement(TableRowProperties(minrowheight="0.7cm", keeptogether="always"))
doc.automaticstyles.addElement(hdr_row)

def cell_style(name, bg, border_bottom="0.5pt solid #b8c6d1"):
    st = Style(name=name, family="table-cell")
    st.addElement(TableCellProperties(
        backgroundcolor=bg, padding="0.15cm",
        borderbottom=border_bottom,
        borderleft="none", borderright="none", bordertop="none",
        verticalalign="middle"))
    doc.automaticstyles.addElement(st)
    return st

c_head = cell_style("DataTable.CH", "#1a3d5c", "1pt solid #1a3d5c")
c_odd  = cell_style("DataTable.C1", "#ffffff")
c_even = cell_style("DataTable.C2", "#eef3f7")

p_th = Style(name="TableHead", family="paragraph")
p_th.addElement(TextProperties(fontname="Liberation Sans", fontsize="10pt",
                               fontweight="bold", color="#ffffff"))
p_th.addElement(ParagraphProperties(textalign="start", margintop="0cm",
                                    marginbottom="0cm"))
doc.automaticstyles.addElement(p_th)

p_td = Style(name="TableCellP", family="paragraph")
p_td.addElement(TextProperties(fontname="Liberation Sans", fontsize="10pt"))
p_td.addElement(ParagraphProperties(textalign="start", margintop="0cm",
                                    marginbottom="0cm"))
doc.automaticstyles.addElement(p_td)

p_td_r = Style(name="TableCellR", family="paragraph", parentstylename=p_td)
p_td_r.addElement(ParagraphProperties(textalign="end"))
doc.automaticstyles.addElement(p_td_r)

# =============================================================== CONTENT ====
t = doc.text

t.addElement(H(outlinelevel=1, stylename=h1, text="1. Executive summary"))

toc = TableOfContent(name="TOC")
src = TableOfContentSource(outlinelevel=3, useindexmarks="false")
src.addElement(IndexTitleTemplate(stylename="Contents_20_Heading",
                                  text="Contents"))
for lvl in range(1, 4):
    tpl = TableOfContentEntryTemplate(outlinelevel=lvl,
                                      stylename="Contents_20_%d" % lvl)
    tpl.addElement(IndexEntryLinkStart(stylename="Internet_20_link"))
    tpl.addElement(IndexEntryChapter())
    tpl.addElement(IndexEntryText())
    tpl.addElement(IndexEntryTabStop(type="right", leaderchar="."))
    tpl.addElement(IndexEntryPageNumber())
    tpl.addElement(IndexEntryLinkEnd())
    src.addElement(tpl)
toc.addElement(src)
toc.addElement(IndexBody())      # empty until LibreOffice updates the index
t.addElement(toc)

p = P(stylename=body)
p.addText("Throughput rose 23% quarter over quarter. The ")
p.addElement(Span(stylename=strong, text="p99 latency"))
p.addText(" target of 250 ms was met in every region except ")
p.addElement(Span(stylename=lit, text="ap-southeast-2"))
p.addText(", where a storage migration is still in flight.")
t.addElement(p)

t.addElement(H(outlinelevel=2, stylename=h2, text="1.1 Key results"))

lst = List(stylename=bullets)
for item in ["Median build time down from 14 min to 9 min.",
             "Two Sev-1 incidents, both resolved inside the 4-hour SLO.",
             "Test coverage on the payments module reached 81%."]:
    li = ListItem()
    li.addElement(P(stylename=body, text=item))
    lst.addElement(li)
t.addElement(lst)

t.addElement(H(outlinelevel=2, stylename=h2, text="1.2 Regional breakdown"))

tbl = Table(name="Results", stylename=tstyle)
for key in colw:
    tbl.addElement(TableColumn(stylename="DataTable.%s" % key))

# Header row inside <table:table-header-rows> repeats on every page break.
from odf.table import TableHeaderRows
thr = TableHeaderRows()
hr = TableRow(stylename=hdr_row)
for label in ["Region", "Requests (M)", "p99 (ms)", "Δ QoQ"]:
    c = TableCell(stylename=c_head, valuetype="string")
    c.addElement(P(stylename=p_th, text=label))
    hr.addElement(c)
thr.addElement(hr)
tbl.addElement(thr)

rows = [("us-east-1", "412.8", "188", "+19%"),
        ("eu-west-1", "233.1", "204", "+27%"),
        ("ap-southeast-2", "97.4", "311", "+8%"),
        ("sa-east-1", "41.0", "229", "+35%")]
for i, row in enumerate(rows):
    tr = TableRow()
    for j, val in enumerate(row):
        # numeric cells: valuetype="float" + value= makes them sortable/summable
        if j in (1, 2):
            c = TableCell(stylename=c_odd if i % 2 == 0 else c_even,
                          valuetype="float", value=val)
        else:
            c = TableCell(stylename=c_odd if i % 2 == 0 else c_even,
                          valuetype="string")
        c.addElement(P(stylename=p_td_r if j else p_td, text=val))
        tr.addElement(c)
    tbl.addElement(tr)
t.addElement(tbl)

t.addElement(P(stylename=body))   # spacer paragraph after a table

t.addElement(H(outlinelevel=1, stylename=h1, text="2. Deployment configuration"))
t.addElement(P(stylename=code, text="replicas: 6"))
t.addElement(P(stylename=code, text="maxSurge: 2   # rolling update budget"))

out = sys.argv[1] if len(sys.argv) > 1 else "out.odt"
doc.save(out)          # note: no .odt suffix is appended when the name has one
print("wrote", out)
