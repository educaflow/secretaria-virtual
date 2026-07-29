# Recetario de odfpy

`scripts/example_report.py` es la versión ejecutable de todo lo que sigue.
Léelo antes de escribir un generador nuevo: copiar su esqueleto es más rápido
que empezar en blanco.

## Los tres cajones de estilos

| Cajón | Acaba en | Visible en la barra lateral | Para qué |
|---|---|---|---|
| `doc.styles` | `styles.xml` | sí | Todo lo que el usuario querrá reestilar: cuerpo, encabezados, estilos de carácter, estilos de lista |
| `doc.automaticstyles` | `styles.xml` o `content.xml` | no | Formato directo puntual: columnas y celdas de tabla, párrafos de pie |
| `doc.masterstyles` | `styles.xml` | — | Solo `MasterPage` |

Un `PageLayout` **tiene** que ir en `automaticstyles` y un `MasterPage` en
`masterstyles`. Intercambiarlos da un fichero que LibreOffice abre pero cuya
configuración de página ignora sin avisar.

## Trampas que cuestan tiempo de verdad

**Las referencias a estilo padre deben ser objetos, no cadenas.** odfpy
codifica `style:name` (`"Text body"` → `Text_20_body`) pero deja intactos los
`parentstylename` / `nextstylename` pasados como texto, con lo que la
referencia queda colgada y la herencia se pierde en silencio:

```python
h1 = Style(name="Heading 1", family="paragraph", parentstylename=body)          # ✅ objeto
h1 = Style(name="Heading 1", family="paragraph", parentstylename="Text body")   # ❌ se rompe
```

**Las fuentes hay que declararlas.** `TextProperties(fontname="Fira Sans")` por
sí solo no hace nada si la fuente no está en `office:font-face-decls`:

```python
doc.fontfacedecls.addElement(FontFace(
    name="Fira Sans", fontfamily="'Fira Sans'",
    fontfamilygeneric="swiss", fontpitch="variable"))
```
En `TextProperties` se usa `fontname=` (el nombre declarado), no `fontfamily=`.
Para que la fuente sobreviva en otra máquina hay que incrustarla: ver más abajo.

**`P` no interpreta `\n`.** Un `P` por párrafo; dentro del párrafo, `LineBreak()`.
Los espacios seguidos se colapsan: usa `text.S(c="4")` para espaciado real y
`text.Tab()` para tabuladores.

**Valores de celda.** `TableCell(valuetype="float", value="412.8")` hace que el
número sea ordenable y sumable en Writer; el texto visible sigue saliendo del
`P` de dentro. Las celdas de texto necesitan `valuetype="string"`. Una celda
sin `P` se dibuja sin altura de línea.

**`doc.save("x")` añade `.odt`; `doc.save("x.odt")` no.** Pasa el nombre
completo para no acabar con `x.odt.odt`.

**Los colores son cadenas `#rrggbb` y las medidas cadenas con unidad**
(`"2.5cm"`, `"11pt"`, `"140%"`). Nunca números sueltos.

## Página, encabezado y pie

```python
pl = PageLayout(name="Standard-PL")
pl.addElement(PageLayoutProperties(
    pagewidth="21.0cm", pageheight="29.7cm",   # A4; Letter = 21.59 x 27.94
    printorientation="portrait",               # apaisado: intercambia w/h Y pon esto
    margintop="2.5cm", marginbottom="2.0cm",
    marginleft="2.2cm", marginright="2.2cm"))
doc.automaticstyles.addElement(pl)

mp = MasterPage(name="Standard", pagelayoutname=pl)
hdr = Header(); hdr.addElement(P(stylename=head_style, text="Título"))
ftr = Footer(); ftr.addElement(parrafo_con_numero)
mp.addElement(hdr); mp.addElement(ftr)
doc.masterstyles.addElement(mp)
```

Los números de página son **campos**, nunca texto literal:

```python
p.addElement(PageNumber(selectpage="current"))
p.addElement(PageCount())
```

Para poner "izquierda … centro … derecha" en una sola línea de pie, añade unos
`TabStops` a las `ParagraphProperties` del párrafo, en el centro y en el
extremo del ancho *de texto* (ancho de página menos los dos márgenes), y luego
`addText("izquierda\t")`, campo, `"\tderecha"`.

Primera página distinta: define un segundo `MasterPage` (p. ej. `"First Page"`)
con `nextstylename` apuntando a `"Standard"`, y da al primer párrafo un estilo
de párrafo con `masterpagename="First Page"`.
Márgenes simétricos para doble cara: `PageLayoutProperties(pageusage="mirrored")`
más `HeaderLeft` / `FooterLeft` para las páginas pares.

Página a varias columnas: añade un `Columns(columncount="2", columngap="0.7cm")`
a `PageLayoutProperties`. Para columnas en solo una parte de la página, usa una
`text.Section` con un estilo de familia `section`.

## Tablas

```python
tbl = Table(name="Resultados", stylename=tstyle)
tbl.addElement(TableColumn(stylename="DataTable.A"))   # una por columna
thr = TableHeaderRows(); thr.addElement(fila_cabecera) # se repite al partir página
tbl.addElement(thr)
tbl.addElement(fila_datos)
```

- Los anchos vienen de estilos `table-column`, uno por ancho distinto. Deben
  sumar el `width` de la tabla o Writer reescala todo.
- `TableProperties(align="margins")` hace que la tabla ocupe el ancho de texto;
  `align="center"` con un ancho menor la centra.
- Los bordes van en la **celda** (`TableCellProperties`), no en la tabla. Define
  los cuatro lados explícitamente: `border="none"` en tres y `borderbottom` en
  el cuarto da filetes horizontales limpios. Writer ignora los bordes puestos
  en `TableProperties`.
- El sombreado alterno es manual: alterna dos estilos `table-cell` por índice.
- Celdas combinadas: `TableCell(numbercolumnsspanned="2", numberrowsspanned="1")`
  seguido de un `CoveredTableCell()` por cada celda absorbida. Si omites los
  covered, la fila queda corrupta.
- Columna vacía repetida: `TableColumn(numbercolumnsrepeated="5")`.
- `TableRowProperties(keeptogether="always")` evita que una fila se parta.
- Una tabla no puede ser el último elemento de `office:text`: añade un `P()`
  final o Writer no tiene dónde poner el cursor.

## Listas

```python
bullets = ListStyle(name="BulletList")
b = ListLevelStyleBullet(level="1", bulletchar="•")
llp = ListLevelProperties(listlevelpositionandspacemode="label-alignment")
llp.addElement(ListLevelLabelAlignment(
    labelfollowedby="listtab", listtabstopposition="1.0cm",
    textindent="-0.4cm", marginleft="1.0cm"))
b.addElement(llp); bullets.addElement(b)
doc.styles.addElement(bullets)
```

Las numeradas usan `ListLevelStyleNumber(level="1", numsuffix=".",
numformat="1", startvalue="1")`; `numformat` acepta `1`, `a`, `A`, `i`, `I`.
Para numeración tipo `1.2.3`, pon `displaylevels="3"`.
Nunca escribas `•` ni `1.` dentro de un `P`: la lista tiene que ser real o se
rompen el índice, el navegador y la renumeración.

## Encabezados e índice

Los encabezados son `H(outlinelevel=N, stylename=...)`, no `P` con estilo. Solo
`text:h` alimenta el índice, el navegador y los marcadores del PDF.

El índice se monta con `TableOfContent` + `TableOfContentSource` +
un `TableOfContentEntryTemplate` por nivel, más un `IndexBody()` vacío.
**El cuerpo sigue vacío hasta que LibreOffice pagina el documento**: un índice
que generes y entregues tal cual se verá en blanco. Rellénalo con:

```bash
python3 scripts/lo_uno.py refresh informe.odt
```

Una plantilla de entrada es una secuencia ordenada:
`IndexEntryLinkStart` → `IndexEntryChapter` → `IndexEntryText` →
`IndexEntryTabStop(type="right", leaderchar=".")` → `IndexEntryPageNumber` →
`IndexEntryLinkEnd`. El orden es el orden impreso.

## Imágenes

```python
href = doc.addPicture("grafico.png")   # lo copia a Pictures/ y lo registra
frame = Frame(width="12cm", height="7cm", anchortype="paragraph",
              stylename=estilo_grafico, zindex="0")
frame.addElement(Image(href=href))
par = P(); par.addElement(frame); doc.text.addElement(par)
```

Un `Frame` va siempre dentro de un `P`. Dale un estilo de familia `graphic` con
`GraphicProperties(wrap="none", verticalpos="top", horizontalpos="center",
horizontalrel="paragraph")`; sin él, el anclaje es impredecible.
Para pies de figura numerados, usa campos `Sequence`.

## Campos, notas y enlaces

- Referencia cruzada: `ReferenceMark(name="tbl1")` en el destino y
  `ReferenceRef(referenceformat="chapter", refname="tbl1")` en la mención.
- Marcadores: `Bookmark(name="x")`; enlace interno: `A(href="#x", text="ver")`
  de `odf.text`.
- Nota al pie: `Note(noteclass="footnote")` con `NoteCitation` y `NoteBody`.
- Fecha/título/autor: `text.Date`, `text.Title`, `text.CreationDate`,
  `text.UserDefined(name="Cliente")`, con los metadatos puestos en `doc.meta`.
- Texto condicional: `ConditionalText(condition="ooow:Total > 1000",
  stringvalueiftrue=..., stringvalueiffalse=...)`.

## Comentarios y control de cambios

Los comentarios en ODF son sencillos:

```python
from odf.office import Annotation
from odf.dc import Creator, Date
a = Annotation()
a.addElement(Creator(text="Revisor"))
a.addElement(Date(text="2026-07-29T10:00:00"))
a.addElement(P(text="Esta cifra necesita fuente."))
parrafo.addElement(a)
```

El control de cambios vive en un `<text:tracked-changes>` al principio de
`office:text`, con `<text:changed-region>` referenciados por marcas
`<text:change-start text:change-id="ct1"/>` … `<text:change-end .../>`.
Generarlo a mano es propenso a errores: mejor entregar un documento limpio y
que el usuario compare, o pilotar Writer por UNO con `RecordChanges = True`.

## Incrustar fuentes

Para que el documento se vea igual en otra máquina, incrusta las fuentes. odfpy
no tiene ayuda: pon `<config:config-item config:name="EmbedFonts"
config:type="boolean">true</config:config-item>` en `settings.xml`, o pasa el
fichero por `scripts/lo_uno.py refresh` con la opción activada.
Opción segura por defecto: quédate en Liberation Serif / Sans / Mono, que son
métricamente compatibles con Times New Roman / Arial / Courier New.
