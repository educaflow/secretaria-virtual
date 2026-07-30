#!/usr/bin/env python3
"""Genera un formulario .odt de LibreOffice Writer a partir de un XML de definición.

Formato del XML de entrada:

    <documento>
        <titulo>
            <valenciano>...</valenciano>
            <castellano>...</castellano>
        </titulo>
        <seccion>
            <valenciano>...</valenciano>
            <castellano>...</castellano>
            <fila>
                <campo nombreCampo="self.x" colspan="5">
                    <valenciano>...</valenciano>
                    <castellano>...</castellano>
                </campo>
                <check nombreCampo="self.y" colspan="3">
                    <valenciano>...</valenciano>
                    <castellano>...</castellano>
                </check>
                <texto colspan="4">
                    <valenciano>...</valenciano>
                    <castellano>...</castellano>
                </texto>
            </fila>
        </seccion>
    </documento>

Reglas:
- Rejilla lógica de 12 columnas. Los colspan pueden tener decimales (p.ej. 3.5).
- Los colspan de una <fila> deben sumar 12 o un múltiplo de 12: cada grupo que
  suma 12 es una línea, y las líneas siguientes se apilan debajo DENTRO del
  mismo rectángulo (sin borde entre ellas).
- Los textos bilingües van SIEMPRE como elementos hijos <valenciano> y
  <castellano> (un hijo omitido o vacío omite ese idioma).
- En los textos bilingües se puede escribir ${nombre;n}: se sustituye por un
  campo de formulario inline llamado `nombre` de `n` columnas de ancho.
- rowSpan="n" (opcional, admite decimales) en campo/check/texto hace la fila
  (y el campo de texto) n veces más alto.
- Cada <campo> genera un control de texto y cada <check> una casilla, con
  form:name igual a nombreCampo.
- Un <campo> sin etiqueta (sin hijos <valenciano>/<castellano> o vacíos)
  genera una fila más baja: el campo se centra verticalmente y solo se deja
  un pequeño hueco arriba y abajo.

Uso:
    python3 xml2odt.py entrada.xml [salida.odt] [--field-height=n] [--check-height=m]

Si no se indica salida.odt, se usa el nombre de entrada.xml cambiando la
extensión .xml por .odt.

Opciones:
    --field-height=n  alto en cm de los campos de texto rellenables (defecto 0.541)
    --check-height=m  alto mínimo en cm de una fila cuyo contenido son solo
                      checks (defecto 0.818)
"""

import argparse
import re
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

ASSETS_DIR = Path(__file__).parent / "assets"
LOGO_FILE = ASSETS_DIR / "logo-gva.png"
STYLES_FILE = ASSETS_DIR / "styles-template.xml"

MIMETYPE = "application/vnd.oasis.opendocument.text"

# Todas las posiciones horizontales se manejan en centésimas de columna
# (12 columnas = 1200). Dos límites especiales: LETRA_EDGE (borde derecho de
# la celda gris de letra de sección, derivado de LETRA_W_CM) y 290 (borde
# derecho de la celda del logo en la cabecera: lo justo para el logo de
# 4.343 cm más el padding, y así dejar el máximo ancho al título).
FULL = 1200
TABLE_CM = 19.0           # ancho útil de la página: 21cm - 2 x margen de 1cm (styles-template.xml)
LETRA_W_CM = 0.8          # ancho de la celda gris de la letra de sección
LETRA_EDGE = round(LETRA_W_CM * FULL / TABLE_CM)
LOGO_EDGE = 290

LOGO_W_CM = 4.343
LOGO_H_CM = 1.939

CTL_H_CM = 0.541          # alto de un campo de texto de una línea
EXTRA_H_CM = 0.62         # alto extra por cada unidad adicional de rowSpan
LABEL_H_CM = 0.31         # parte fija de la fila de un campo: padding sup (0.05) + línea de la
                          # etiqueta al 80% (~0.23) + separación con el borde inferior (0.03)
LABEL_VACIO_H_CM = 0.1    # hueco arriba y abajo del campo cuando no tiene etiqueta
ROW_CHECK_CM = 0.818      # alto mínimo de una fila con checks
ROW_TITULO_CM = 1.901     # alto mínimo de la fila de cabecera
ROW_SECCION_CM = 0.649    # alto mínimo de la fila de sección (dos líneas de 9pt al 80%)

INLINE_RE = re.compile(r"\$\{([^;{}]+);([0-9]+(?:\.[0-9]+)?)\}")

CONTENT_ROOT_ATTRS = (
    'xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" '
    'xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" '
    'xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" '
    'xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" '
    'xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0" '
    'xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" '
    'xmlns:xlink="http://www.w3.org/1999/xlink" '
    'xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0" '
    'xmlns:form="urn:oasis:names:tc:opendocument:xmlns:form:1.0" '
    'xmlns:loext="urn:org:documentfoundation:names:experimental:office:xmlns:loext:1.0" '
    'xmlns:ooo="http://openoffice.org/2004/office" '
    'office:version="1.3"'
)

BORDER = "0.5pt solid #000000"

FONT_DECLS = """
 <office:font-face-decls>
  <style:font-face style:name="Liberation Sans" svg:font-family="'Liberation Sans'" style:font-family-generic="swiss" style:font-pitch="variable"/>
  <style:font-face style:name="Roboto" svg:font-family="Roboto" style:font-pitch="variable"/>
  <style:font-face style:name="Roboto SemiBold" svg:font-family="'Roboto SemiBold'" style:font-pitch="variable"/>
 </office:font-face-decls>
"""

PARAGRAPH_STYLES = """
  <style:style style:name="PTitulo" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <style:paragraph-properties fo:margin-top="0cm" fo:margin-bottom="0cm" fo:text-align="center"/>
   <style:text-properties fo:text-transform="uppercase" style:font-name="Roboto SemiBold" fo:font-size="11pt" fo:font-weight="normal"/>
  </style:style>
  <style:style style:name="PTituloCast" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <style:paragraph-properties fo:margin-top="0cm" fo:margin-bottom="0cm" fo:text-align="center"/>
   <style:text-properties fo:text-transform="uppercase" style:font-name="Roboto SemiBold" fo:font-size="11pt" fo:font-style="italic" fo:font-weight="normal"/>
  </style:style>
  <style:style style:name="PLetra" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <!-- line-height reducido: que la letra no imponga más alto de fila que el título bilingüe. -->
   <style:paragraph-properties fo:margin-top="0cm" fo:margin-bottom="0cm" fo:text-align="center" fo:line-height="80%"/>
   <style:text-properties fo:color="#000000" style:font-name="Roboto" fo:font-size="16pt" fo:font-weight="normal"/>
  </style:style>
  <style:style style:name="PSeccion" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <!-- line-height reducido: junta la línea de valenciano con la de castellano. -->
   <style:paragraph-properties fo:line-height="80%"/>
   <style:text-properties fo:text-transform="uppercase" style:font-name="Roboto SemiBold" fo:font-size="9pt" fo:font-weight="bold"/>
  </style:style>
  <style:style style:name="PSeccionCast" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <style:paragraph-properties fo:line-height="80%"/>
   <style:text-properties fo:text-transform="uppercase" style:font-name="Roboto SemiBold" fo:font-size="9pt" fo:font-style="italic" fo:font-weight="bold"/>
  </style:style>
  <style:style style:name="PEtiqueta" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <!-- line-height reducido: recorta el espacio entre la etiqueta y el campo de debajo. -->
   <style:paragraph-properties fo:line-height="80%"/>
   <style:text-properties fo:text-transform="uppercase" style:font-name="Roboto" fo:font-size="7pt" fo:font-style="normal"/>
  </style:style>
  <style:style style:name="PControl" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <!-- Fuente mínima: la línea que aloja el control apenas añade alto extra al del propio control. -->
   <style:text-properties style:font-name="Roboto" fo:font-size="2pt"/>
  </style:style>
  <style:style style:name="PTexto" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <style:text-properties fo:font-variant="normal" fo:text-transform="none" style:font-name="Roboto" fo:font-size="7pt" fo:font-style="normal"/>
  </style:style>
  <style:style style:name="PTextoCast" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <style:text-properties fo:font-variant="normal" fo:text-transform="none" style:font-name="Roboto" fo:font-size="7pt" fo:font-style="italic"/>
  </style:style>
  <style:style style:name="PCheckbox" style:family="paragraph" style:parent-style-name="Table_20_Contents">
   <style:paragraph-properties fo:text-align="end"/>
   <style:text-properties fo:text-transform="none" style:font-name="Roboto" fo:font-size="7pt"/>
  </style:style>
  <style:style style:name="PCampoTexto" style:family="paragraph">
   <style:paragraph-properties fo:text-align="start"/>
   <style:text-properties style:font-name="Roboto" fo:font-size="9pt" fo:font-style="normal" fo:font-weight="normal"/>
  </style:style>
  <style:style style:name="TCursiva" style:family="text">
   <style:text-properties fo:font-style="italic"/>
  </style:style>
  <style:style style:name="GrLogo" style:family="graphic" style:parent-style-name="Graphics">
   <style:graphic-properties style:vertical-pos="top" style:vertical-rel="baseline" style:horizontal-pos="center" style:horizontal-rel="paragraph" style:mirror="none"/>
  </style:style>
  <style:style style:name="GrControl" style:family="graphic">
   <style:graphic-properties fo:border="none" style:wrap="run-through" style:number-wrapped-paragraphs="no-limit" style:vertical-pos="middle" style:vertical-rel="line" style:horizontal-pos="from-left" style:horizontal-rel="paragraph" style:flow-with-text="false"/>
  </style:style>
  <style:style style:name="GrCheck" style:family="graphic">
   <style:graphic-properties draw:textarea-vertical-align="middle" style:wrap="run-through" style:number-wrapped-paragraphs="no-limit" style:vertical-pos="middle" style:vertical-rel="baseline" style:horizontal-pos="from-left" style:horizontal-rel="paragraph" style:flow-with-text="false"/>
  </style:style>
"""


def esc(s):
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace('"', "&quot;"))


def colspan_units(elem):
    try:
        units = round(float(elem.get("colspan")) * 100)
    except (TypeError, ValueError):
        raise SystemExit(f"ERROR: <{elem.tag}> sin colspan numérico")
    if units <= 0:
        raise SystemExit(f"ERROR: colspan {elem.get('colspan')} no positivo")
    return units


def row_span(elem):
    try:
        rs = float(elem.get("rowSpan", "1"))
    except ValueError:
        raise SystemExit(f"ERROR: rowSpan {elem.get('rowSpan')} no numérico")
    if rs < 1:
        raise SystemExit(f"ERROR: rowSpan {rs} menor que 1")
    return rs


def bilingual_texts(elem):
    """Lee los textos bilingües de un elemento desde sus hijos
    <valenciano>/<castellano>. Un hijo omitido o vacío es cadena vacía."""
    for attr in ("valenciano", "castellano"):
        if elem.get(attr) is not None:
            raise SystemExit(
                f"ERROR: <{elem.tag}> lleva '{attr}' como atributo (formato antiguo); "
                f"escríbelo como elemento hijo: <{elem.tag} ...><{attr}>...</{attr}></{elem.tag}>")
    texts = []
    for name in ("valenciano", "castellano"):
        child = elem.find(name)
        texts.append((child.text or "").strip() if child is not None else "")
    return texts


def partition_fila(fila):
    """Divide los hijos de una <fila> en líneas que suman exactamente 12."""
    lines, line, cursor = [], [], 0
    for h in fila:
        if h.tag not in ("campo", "check", "texto"):
            raise SystemExit(f"ERROR: elemento <{h.tag}> desconocido dentro de <fila>")
        units = colspan_units(h)
        if cursor + units > FULL:
            raise SystemExit(
                f"ERROR: un <{h.tag}> con colspan {h.get('colspan')} cruza el límite de 12 columnas")
        line.append((h, cursor, cursor + units))
        cursor += units
        if cursor == FULL:
            lines.append(line)
            line, cursor = [], 0
    if line:
        raise SystemExit(f"ERROR: los colspan de una <fila> suman {cursor / 100 + len(lines) * 12}, "
                         "deben sumar 12 o un múltiplo de 12")
    return lines


class DocumentoBuilder:
    def __init__(self, root, field_height=CTL_H_CM, check_height=ROW_CHECK_CM):
        self.root = root
        self.field_height = field_height
        self.check_height = check_height
        self.controls = []          # (id, tipo, nombreCampo)
        self.rows = []
        self.z_index = 1
        self.edges = []             # límites de columna, en centésimas
        self.cell_styles = {}       # nombre -> xml del estilo
        self.row_styles = {}        # nombre -> xml del estilo

    # ----------------------------------------------------------- columnas
    def collect_edges(self):
        edges = {0, FULL, LETRA_EDGE, LOGO_EDGE}
        for seccion in self.root:
            if seccion.tag != "seccion":
                continue
            for fila in seccion:
                if fila.tag != "fila":
                    continue
                for h, start, end in [c for line in partition_fila(fila) for c in line]:
                    edges.add(start)
                    edges.add(end)
                    if h.tag == "check" and end - start > 100:
                        edges.add(start + 100)
        self.edges = sorted(edges)

    def edge_index(self, pos):
        try:
            return self.edges.index(pos)
        except ValueError:
            raise SystemExit(f"ERROR interno: posición de columna {pos / 100} fuera de la rejilla")

    # ------------------------------------------------------------- estilos
    def cell_style(self, kind, no_top=False, no_bottom=False):
        name = f"C{kind}{'NT' if no_top else ''}{'NB' if no_bottom else ''}"
        if name in self.cell_styles:
            return name
        props = {
            "base": ('fo:padding="0.101cm"', True, True),
            "campo": ('fo:padding-left="0.101cm" fo:padding-right="0.101cm"'
                      ' fo:padding-top="0.05cm" fo:padding-bottom="0.03cm"', True, True),
            "mid": ('style:vertical-align="middle" fo:padding="0.101cm"', True, True),
            "chk": ('style:vertical-align="middle" fo:padding="0.101cm"', True, False),
            "chklbl": ('style:vertical-align="middle" fo:padding="0.101cm"', False, True),
            "letra": ('style:vertical-align="middle" fo:background-color="#999999" fo:padding="0cm"', True, True),
            "seccion": ('fo:padding-left="0.101cm" fo:padding-right="0cm" fo:padding-top="0.049cm" fo:padding-bottom="0cm"', True, True),
        }
        extra, left, right = props[kind]
        borders = (f' fo:border-left="{BORDER if left else "none"}"'
                   f' fo:border-right="{BORDER if right else "none"}"'
                   f' fo:border-top="{"none" if no_top else BORDER}"'
                   f' fo:border-bottom="{"none" if no_bottom else BORDER}"')
        self.cell_styles[name] = (
            f'<style:style style:name="{name}" style:family="table-cell">'
            f'<style:table-cell-properties {extra}{borders} style:writing-mode="page"/>'
            f'</style:style>')
        return name

    def row_style(self, min_height_cm):
        if min_height_cm <= 0:
            return ""
        name = f"R{int(round(min_height_cm * 1000))}"
        if name not in self.row_styles:
            self.row_styles[name] = (
                f'<style:style style:name="{name}" style:family="table-row">'
                f'<style:table-row-properties style:min-row-height="{min_height_cm:.3f}cm"/>'
                f'</style:style>')
        return f' table:style-name="{name}"'

    # ------------------------------------------------------------ controles
    def new_control(self, tipo, nombre_campo):
        if not nombre_campo:
            raise SystemExit("ERROR: falta nombreCampo en un campo/check")
        cid = f"control{len(self.controls) + 1}"
        self.controls.append((cid, tipo, nombre_campo))
        return cid

    def forms_xml(self):
        items = []
        for cid, tipo, nombre in self.controls:
            if tipo == "campo":
                items.append(
                    f'<form:text form:name="{esc(nombre)}"'
                    f' form:control-implementation="ooo:com.sun.star.form.component.TextField"'
                    f' xml:id="{cid}" form:id="{cid}" form:input-required="false"'
                    f' form:convert-empty-to-null="true">'
                    '<form:properties>'
                    '<form:property form:property-name="DefaultControl" office:value-type="string"'
                    ' office:string-value="com.sun.star.form.control.TextField"/>'
                    '</form:properties>'
                    '</form:text>'
                )
            else:
                items.append(
                    f'<form:checkbox form:name="{esc(nombre)}"'
                    f' form:control-implementation="ooo:com.sun.star.form.component.CheckBox"'
                    f' xml:id="{cid}" form:id="{cid}" form:visual-effect="flat"'
                    f' form:input-required="false" form:image-position="center">'
                    '<form:properties>'
                    '<form:property form:property-name="DefaultControl" office:value-type="string"'
                    ' office:string-value="com.sun.star.form.control.CheckBox"/>'
                    '</form:properties>'
                    '</form:checkbox>'
                )
        controls = "\n     ".join(items)
        return f"""
   <office:forms form:automatic-focus="true" form:apply-design-mode="true">
    <form:form form:name="Formulario" form:apply-filter="true" form:command-type="table"
     form:control-implementation="ooo:com.sun.star.form.component.Form" office:target-frame="">
     {controls}
    </form:form>
   </office:forms>
"""

    def inline_control(self, cid, width_cm, height_cm=None):
        if height_cm is None:
            height_cm = self.field_height
        z = self.z_index
        self.z_index += 1
        return (f'<draw:control text:anchor-type="as-char" draw:z-index="{z}"'
                f' draw:name="Control {z}" draw:style-name="GrControl"'
                f' draw:text-style-name="PCampoTexto" svg:width="{width_cm:.3f}cm"'
                f' svg:height="{height_cm:.3f}cm" draw:control="{cid}"/>')

    def rich_text(self, s):
        """Escapa texto sustituyendo cada ${nombre;n} por un campo inline de n columnas."""
        out, last = [], 0
        for m in INLINE_RE.finditer(s):
            out.append(esc(s[last:m.start()]))
            cid = self.new_control("campo", m.group(1))
            width = TABLE_CM * float(m.group(2)) / 12.0
            out.append(self.inline_control(cid, width))
            last = m.end()
        out.append(esc(s[last:]))
        return "".join(out)

    # ---------------------------------------------------------------- celdas
    def cell(self, style, start, end, inner):
        span = self.edge_index(end) - self.edge_index(start)
        covered = "<table:covered-table-cell/>" * (span - 1)
        span_attr = f' table:number-columns-spanned="{span}"' if span > 1 else ""
        return (f'<table:table-cell table:style-name="{style}"{span_attr}'
                f' office:value-type="string">{inner}</table:table-cell>{covered}')

    def bilingual_paragraphs(self, val, cast, style_val, style_cast):
        paras = ""
        if val:
            paras += f'<text:p text:style-name="{style_val}">{self.rich_text(val)}</text:p>'
        if cast:
            paras += f'<text:p text:style-name="{style_cast}">{self.rich_text(cast)}</text:p>'
        return paras or '<text:p text:style-name="PTexto"/>'

    def label_paragraph(self, val, cast):
        # Etiqueta de campo: una sola línea "VALENCIÀ / CASTELLANO (cursiva)".
        inner = self.rich_text(val)
        if cast:
            inner += f' / <text:span text:style-name="TCursiva">{self.rich_text(cast)}</text:span>'
        return f'<text:p text:style-name="PEtiqueta">{inner}</text:p>'

    def text_control_paragraph(self, cid, width_cm, height_cm, style="PControl"):
        return (f'<text:p text:style-name="{style}">'
                f'{self.inline_control(cid, width_cm, height_cm)}</text:p>')

    def checkbox_control_paragraph(self, cid):
        z = self.z_index
        self.z_index += 1
        return (f'<text:p text:style-name="PCheckbox">'
                f'<draw:control text:anchor-type="as-char" draw:z-index="{z}"'
                f' draw:name="Control {z}" draw:style-name="GrCheck"'
                f' draw:text-style-name="PCheckbox" svg:width="0.479cm"'
                f' svg:height="0.745cm" draw:control="{cid}"/></text:p>')

    # ------------------------------------------------------------------ filas
    def add_titulo(self, elem):
        z = self.z_index
        self.z_index += 1
        logo = (f'<text:p text:style-name="PControl">'
                f'<draw:frame draw:style-name="GrLogo" draw:name="Logo"'
                f' text:anchor-type="as-char" svg:width="{LOGO_W_CM}cm"'
                f' svg:height="{LOGO_H_CM}cm" draw:z-index="{z}">'
                f'<draw:image xlink:href="Pictures/logo-gva.png" xlink:type="simple"'
                f' xlink:show="embed" xlink:actuate="onLoad" draw:mime-type="image/png"/>'
                f'</draw:frame></text:p>')
        val, cast = bilingual_texts(elem)
        titulo = self.bilingual_paragraphs(val, cast, "PTitulo", "PTituloCast")
        cells = (self.cell(self.cell_style("mid"), 0, LOGO_EDGE, logo)
                 + self.cell(self.cell_style("mid"), LOGO_EDGE, FULL, titulo))
        self.rows.append(
            f'<table:table-row{self.row_style(ROW_TITULO_CM)}>{cells}</table:table-row>')

    def add_seccion_header(self, elem, letra):
        letra_p = f'<text:p text:style-name="PLetra">{esc(letra)}</text:p>'
        val, cast = bilingual_texts(elem)
        titulo = self.bilingual_paragraphs(val, cast, "PSeccion", "PSeccionCast")
        cells = (self.cell(self.cell_style("letra"), 0, LETRA_EDGE, letra_p)
                 + self.cell(self.cell_style("seccion"), LETRA_EDGE, FULL, titulo))
        self.rows.append(
            f'<table:table-row{self.row_style(ROW_SECCION_CM)}>{cells}</table:table-row>')

    def add_fila(self, fila):
        lines = partition_fila(fila)
        for i, line in enumerate(lines):
            no_top = i > 0
            no_bottom = i < len(lines) - 1
            self.add_linea(line, no_top, no_bottom)

    def add_linea(self, line, no_top, no_bottom):
        cells = []
        min_height = 0.0
        solo_checks = all(h.tag == "check" for h, _, _ in line)
        for h, start, end in line:
            val, cast = bilingual_texts(h)
            rs = row_span(h)
            extra = (rs - 1) * EXTRA_H_CM
            if h.tag == "campo":
                cid = self.new_control("campo", h.get("nombreCampo"))
                width = cell_width_cm(start, end) - 0.28
                ctl_h = self.field_height + extra
                if val or cast:
                    min_height = max(min_height, LABEL_H_CM + ctl_h)
                    inner = (self.label_paragraph(val, cast)
                             + self.text_control_paragraph(cid, width, ctl_h))
                    style = self.cell_style("campo", no_top, no_bottom)
                else:
                    # Sin etiqueta: solo la línea del control, centrada verticalmente,
                    # con un pequeño hueco arriba y abajo.
                    min_height = max(min_height, ctl_h + 2 * LABEL_VACIO_H_CM)
                    inner = self.text_control_paragraph(cid, width, ctl_h)
                    style = self.cell_style("mid", no_top, no_bottom)
                cells.append(self.cell(style, start, end, inner))
            elif h.tag == "check":
                base = self.check_height if solo_checks else ROW_CHECK_CM
                min_height = max(min_height, base + extra)
                cid = self.new_control("check", h.get("nombreCampo"))
                cells.append(self.cell(self.cell_style("chk", no_top, no_bottom),
                                       start, start + 100,
                                       self.checkbox_control_paragraph(cid)))
                if end - start > 100:
                    label = self.bilingual_paragraphs(val, cast, "PTexto", "PTextoCast")
                    cells.append(self.cell(self.cell_style("chklbl", no_top, no_bottom),
                                           start + 100, end, label))
            else:  # texto
                if rs > 1:
                    min_height = max(min_height, 0.5 * rs)
                cells.append(self.cell(self.cell_style("base", no_top, no_bottom),
                                       start, end,
                                       self.bilingual_paragraphs(val, cast, "PTexto", "PTextoCast")))
        self.rows.append(
            f'<table:table-row{self.row_style(min_height)}>{"".join(cells)}</table:table-row>')

    # ------------------------------------------------------------------ build
    def build_content(self):
        self.collect_edges()
        letra = "A"
        for elem in self.root:
            if elem.tag == "titulo":
                self.add_titulo(elem)
            elif elem.tag == "seccion":
                self.add_seccion_header(elem, letra)
                letra = chr(ord(letra) + 1)
                for fila in elem:
                    if fila.tag in ("valenciano", "castellano"):
                        continue
                    if fila.tag != "fila":
                        raise SystemExit(f"ERROR: elemento <{fila.tag}> desconocido dentro de <seccion>")
                    self.add_fila(fila)
            else:
                raise SystemExit(f"ERROR: elemento <{elem.tag}> desconocido dentro de <documento>")

        col_styles = []
        columns = []
        for i in range(len(self.edges) - 1):
            rel = (self.edges[i + 1] - self.edges[i]) * 10
            width = cell_width_cm(self.edges[i], self.edges[i + 1])
            col_styles.append(
                f'<style:style style:name="Datos.C{i}" style:family="table-column">'
                f'<style:table-column-properties style:column-width="{width:.3f}cm"'
                f' style:rel-column-width="{rel}*"/></style:style>')
            columns.append(f'<table:table-column table:style-name="Datos.C{i}"/>')

        autostyles = "\n  ".join(
            [f'<style:style style:name="Datos" style:family="table">'
             f'<style:table-properties style:width="{TABLE_CM}cm" table:align="margins"/></style:style>']
            + col_styles
            + list(self.row_styles.values())
            + list(self.cell_styles.values()))

        rows = "\n    ".join(self.rows)
        return f"""<?xml version="1.0" encoding="UTF-8"?>
<office:document-content {CONTENT_ROOT_ATTRS}>
 <office:scripts/>
{FONT_DECLS}
 <office:automatic-styles>
  {autostyles}
{PARAGRAPH_STYLES}
 </office:automatic-styles>
 <office:body>
  <office:text>
{self.forms_xml()}
   <table:table table:name="Datos" table:style-name="Datos">
    {"".join(columns)}
    {rows}
   </table:table>
   <text:p text:style-name="Standard"/>
  </office:text>
 </office:body>
</office:document-content>
"""


def cell_width_cm(start, end):
    return TABLE_CM * (end - start) / 1200.0


MANIFEST = f"""<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.3">
 <manifest:file-entry manifest:full-path="/" manifest:version="1.3" manifest:media-type="{MIMETYPE}"/>
 <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
 <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
 <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
 <manifest:file-entry manifest:full-path="Pictures/logo-gva.png" manifest:media-type="image/png"/>
</manifest:manifest>
"""

META = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
 xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" office:version="1.3">
 <office:meta><meta:generator>secretaria-virtual/xml2odt</meta:generator></office:meta>
</office:document-meta>
"""


def main():
    parser = argparse.ArgumentParser(
        description="Genera un formulario .odt de LibreOffice Writer a partir de un XML de definición.")
    parser.add_argument("entrada", type=Path, help="XML de definición del formulario")
    parser.add_argument("salida", type=Path, nargs="?", default=None,
                        help="fichero .odt a generar (defecto: la entrada con extensión .odt)")
    parser.add_argument("--field-height", type=float, default=CTL_H_CM, metavar="n",
                        help=f"alto en cm de los campos de texto rellenables (defecto {CTL_H_CM})")
    parser.add_argument("--check-height", type=float, default=ROW_CHECK_CM, metavar="m",
                        help="alto mínimo en cm de una fila cuyo contenido son solo checks "
                             f"(defecto {ROW_CHECK_CM})")
    args = parser.parse_args()
    if args.field_height <= 0:
        raise SystemExit(f"ERROR: --field-height {args.field_height} no positivo")
    if args.check_height <= 0:
        raise SystemExit(f"ERROR: --check-height {args.check_height} no positivo")
    salida = args.salida if args.salida is not None else args.entrada.with_suffix(".odt")

    root = ET.parse(args.entrada).getroot()
    if root.tag != "documento":
        raise SystemExit("ERROR: el elemento raíz debe ser <documento>")

    content = DocumentoBuilder(root, field_height=args.field_height,
                               check_height=args.check_height).build_content()

    with zipfile.ZipFile(salida, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr(zipfile.ZipInfo("mimetype"), MIMETYPE, zipfile.ZIP_STORED)
        z.writestr("content.xml", content)
        z.writestr("styles.xml", STYLES_FILE.read_text(encoding="utf-8"))
        z.writestr("meta.xml", META)
        z.writestr("META-INF/manifest.xml", MANIFEST)
        z.write(LOGO_FILE, "Pictures/logo-gva.png")

    print(f"Generado {salida}")


if __name__ == "__main__":
    main()