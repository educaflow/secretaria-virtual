---
name: libreoffice-writer
description: Usar siempre que se quiera crear, leer, editar o convertir documentos de LibreOffice Writer (.odt, .ott, .fodt) o documentos ODF de texto. Se activa al mencionar 'LibreOffice', 'Writer', 'ODT', 'OpenDocument', o al pedir informes, memorias, contratos, cartas, actas, manuales o plantillas en formato ODF con formato profesional — tablas con estilos, fuentes, encabezados y pies, numeración de páginas, índices, listas, imágenes o control de cambios. Usar también para rellenar plantillas .ott, sustituir marcadores, extraer texto de un .odt, o convertir entre .odt, .docx y PDF. NO usar para .docx nativo de Word (usar la skill docx), hojas de cálculo (.ods/.xlsx) ni presentaciones (.odp/.pptx).
---

# Documentos de LibreOffice Writer (ODF)

Un `.odt` es un ZIP de ficheros XML. Elige la vía según la tarea:

| Tarea | Vía |
|---|---|
| **Crear** un documento nuevo | `odfpy` — copiar el esqueleto de `scripts/example_report.py` |
| **Crear** desde Markdown simple | `pandoc x.md -o x.odt --reference-doc=estilo.odt` |
| **Editar** uno existente | `unzip` → editar `content.xml` con `lxml` → `zip` |
| **Leer** el contenido | `pandoc -t markdown x.odt` |
| **Partir de una plantilla** de empresa | copiar el `.ott` e inyectar contenido con sus estilos |
| **Índice, campos, PDF avanzado** | `scripts/lo_uno.py` (LibreOffice vía UNO) |

Las rutas son relativas al directorio de esta skill.

## Empieza aquí

`scripts/example_report.py` es un generador **completo y ejecutable** con
fuentes declaradas, estilos con herencia, maquetación de página, encabezado y
pie con numeración, lista con viñetas, tabla profesional con cabecera repetida
y filas alternas, e índice. Léelo y adáptalo; partir de cero cuesta mucho más.

```bash
python3 scripts/example_report.py informe.odt
python3 scripts/lo_uno.py refresh informe.odt     # rellena el índice
python3 scripts/odt_verify.py informe.odt         # renderiza a JPG
```

`reference/odfpy-cookbook.md` — recetas de estilos, tablas, listas, campos,
imágenes, índices y las trampas concretas de la API.
`reference/odt-format.md` — estructura del ZIP, edición de ficheros ajenos,
plantillas y conversión.

## Las cinco trampas que más tiempo cuestan

1. **`parentstylename` debe recibir el objeto `Style`, no una cadena.** odfpy
   codifica `style:name` (`"Text body"` → `Text_20_body`) pero no las
   referencias en texto, así que la herencia se rompe en silencio.
2. **Las fuentes hay que declararlas** en `doc.fontfacedecls` con `FontFace`;
   si no, `TextProperties(fontname=...)` no tiene ningún efecto.
3. **`PageLayout` va en `doc.automaticstyles`** y `MasterPage` en
   `doc.masterstyles`. Al revés el fichero abre pero ignora los márgenes.
4. **El índice sale vacío** hasta que LibreOffice pagina el documento: hay que
   pasar `scripts/lo_uno.py refresh`.
5. **Los anchos de columna de una tabla deben sumar el ancho de la tabla**, y
   los bordes van en `TableCellProperties`, nunca en `TableProperties`.

## Verifica siempre antes de entregar

Un `.odt` puede ser válido y verse mal. Renderízalo y **mira las imágenes**:

```bash
python3 scripts/odt_verify.py informe.odt -r 120
```

Revisa: márgenes y saltos de página, que las tablas no se desborden ni partan
mal, que las fuentes no hayan caído al sustituto por defecto, que el índice
tenga números de página, y que encabezado y pie salgan en todas las páginas.

Antes de tocar un documento ajeno o una plantilla:

```bash
python3 scripts/odt_inspect.py plantilla.ott --styles
```

## Decisiones de diseño

- Reutiliza estilos con nombre en vez de formato directo: es lo que hace el
  documento editable después. El formato directo va en `automaticstyles` y solo
  para casos únicos (una columna de tabla, un pie).
- Encabezados con `H(outlinelevel=N)`, nunca un `P` con la fuente en grande: de
  eso dependen el índice, el navegador y los marcadores del PDF.
- Listas con `List`/`ListItem` y un `ListStyle`, nunca `•` ni `1.` escritos.
- Por defecto Liberation Serif / Sans / Mono: son métricamente compatibles con
  Times New Roman / Arial / Courier New y existen en cualquier instalación.
- Si el destinatario usa Word, genera `.odt` y convierte al final con
  `soffice --headless --convert-to docx`; las tablas y los estilos sobreviven.

## Dependencias

Vía recomendada — paquetes del sistema. El módulo `uno` lo instala LibreOffice
y debe coincidir con su versión, así que apt es la única forma correcta de
obtenerlo; `odfpy` en apt va además por delante de PyPI (1.4.2 frente a 1.4.1):

```bash
sudo apt install libreoffice python3-odf python3-lxml python3-uno pandoc poppler-utils
# Fedora: python3-odfpy python3-lxml libreoffice-pyuno poppler-utils
```
Ojo: en Debian/Ubuntu odfpy se llama `python3-odf`, sin el "py".

Así los scripts corren con `python3` a secas desde cualquier directorio, que es
lo que conviene a una skill.

Alternativa si necesitas versiones fijadas (CI, reproducibilidad). El venv debe
crearse con `--system-site-packages` o no verá `uno`:

```bash
python3 -m venv --system-site-packages ~/.venvs/odf
~/.venvs/odf/bin/pip install odfpy lxml
```
En ese caso invoca los scripts con `~/.venvs/odf/bin/python`.
