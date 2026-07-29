# El contenedor .odt, edición de ficheros existentes y plantillas

## Estructura del archivo

Un `.odt` es un ZIP:

| Miembro | Contiene |
|---|---|
| `mimetype` | `application/vnd.oasis.opendocument.text`, **primera entrada y sin comprimir** |
| `content.xml` | El cuerpo más los estilos automáticos que solo él usa |
| `styles.xml` | Estilos con nombre, page layouts, master pages, contenido de encabezado/pie |
| `meta.xml` | Título, autor, estadísticas |
| `settings.xml` | Estado de vista, `EmbedFonts`, actualización de campos |
| `META-INF/manifest.xml` | Lista todos los demás miembros — **hay que actualizarlo al añadir ficheros** |
| `Pictures/` | Imágenes incrustadas |

Reempaquetado a mano:

```bash
unzip -q doc.odt -d unpacked/
find unpacked -type l -delete        # entrada no fiable: elimina enlaces simbólicos
# ... editar unpacked/content.xml ...
cd unpacked
zip -q -X -0 ../out.odt mimetype     # mimetype primero y sin comprimir (-0)
zip -q -X -r ../out.odt . -x mimetype
```

Si el `mimetype` queda mal, algunos lectores abren el fichero como un ZIP
genérico. La comprobación rápida de validez es
`soffice --headless --convert-to odt out.odt`: si hace el viaje de ida y vuelta,
el contenedor está bien.

## Editar un documento existente

1. `python3 scripts/odt_inspect.py doc.odt` — mira qué estilos hay para
   reutilizarlos en vez de inventar otros en paralelo.
2. Lee el texto con `pandoc -t markdown doc.odt` (rápido, con pérdida) o con
   `odf.text.teletype.extractText` (respeta la estructura).
3. Edita `content.xml` con `lxml` manteniendo los prefijos de namespace que ya
   están. **No lo reformatees**: el espacio en blanco entre `text:span` es
   contenido, y el pretty-print mete espacios en el texto visible.
4. Reempaqueta y verifica.

A diferencia de Word, Writer no fragmenta el texto en decenas de runs, así que
una frase que ves suele existir como cadena contigua en el XML. Aun así el
texto se parte en cada cambio de formato, campo, marcador o ancla de comentario.

Namespaces que vas a necesitar:

```python
NS = {"office": "urn:oasis:names:tc:opendocument:xmlns:office:1.0",
      "style":  "urn:oasis:names:tc:opendocument:xmlns:style:1.0",
      "text":   "urn:oasis:names:tc:opendocument:xmlns:text:1.0",
      "table":  "urn:oasis:names:tc:opendocument:xmlns:table:1.0",
      "draw":   "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0",
      "fo":     "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"}
```

## Partir de una plantilla corporativa — la vía preferible

Escribir a mano toda una identidad visual en odfpy es lento y fácil de errar en
detalles. Cuando el usuario tiene (o puede hacer) un `.ott` / `.odt` con los
estilos correctos:

1. `odt_inspect.py plantilla.ott --styles` para saber los nombres exactos
   (recuerda la codificación `_20_` de los espacios).
2. Copia la plantilla a la ruta de salida e inyecta el contenido en
   `content.xml` usando los nombres de estilo que ya trae.
3. O genera con odfpy y luego trasplanta el diseño:
   `odfdo-styles -m plantilla.ott generado.odt` (con `pip install odfdo`)
   sustituye los estilos del generado por los de la plantilla.

Para cartas y contratos funciona muy bien la sustitución de marcadores: pon
`{{nombre_cliente}}` en la plantilla y reemplaza en los nodos de texto de cada
`text:p`. Haz la sustitución sobre el texto concatenado del párrafo y reescribe
sus hijos; si no, un marcador partido entre dos spans no se encuentra nunca.

## Conversión

```bash
soffice --headless --convert-to pdf   informe.odt --outdir out/
soffice --headless --convert-to docx  informe.odt          # para gente con Word
soffice --headless --convert-to "txt:Text (encoded):UTF8" informe.odt
pandoc informe.md -o informe.odt --reference-doc=estilo-casa.odt
```

`--reference-doc` es el camino más rápido de Markdown a un Writer con estilo,
pero solo mapea los estilos que pandoc conoce (encabezados, cuerpo, código,
cita). Las tablas salen con los valores por defecto de pandoc; más allá de eso
hace falta odfpy o un post-proceso.

Solo un proceso `soffice` puede usar un perfil a la vez. En pipelines paralelos
dale uno propio a cada invocación:
`-env:UserInstallation=file:///tmp/lo-$$`.

## Cuándo recurrir a UNO

`scripts/lo_uno.py` pilota una instancia viva de Writer. Úsalo para lo que el
formato de fichero no puede expresar por sí solo:

- Rellenar índices, índices alfabéticos o bibliografías (necesitan paginación).
- Refrescar campos y recuentos de páginas.
- Combinar correspondencia sobre un origen de datos.
- Grabar control de cambios (`doc.RecordChanges = True`) o aceptarlos.
- Exportar PDF con opciones — PDF etiquetado, PDF/A, campos de formulario,
  marca de agua: pasa propiedades `FilterData` extra a `storeToURL`.

Es más lento y necesita un soffice arrancado, así que construye el documento
con odfpy y usa UNO solo como pasada final.
