# Los documentos de tipo TEXTO (`<documentoTexto>`)

El segundo tipo de documento de `documentospdf/` (el primero, el FORMULARIO, está en `documentos.md`): un documento **en prosa** —párrafos, listas, tablas sin bordes— con una cabecera de logo y título en su primera página. Es el formato de los certificados, resoluciones y escritos que hoy el centro hace con un procesador de textos.

**MUST** leerse junto a `documentos.md`, que es donde está lo **común a los dos tipos** y no se repite aquí: la carpeta `documentospdf/` y el enum `TipoDocumentoPdf` (§1), el idioma (§1.1), los fragmentos `_*.xml` y el `<include>` (§2.5), el `<valenciano>` que se traduce del `<castellano>` (§2.6), las expresiones Groovy y el test P1 (§2.8), la visibilidad `visible`/`siOculto` (§2.9) y el hueco de la firma `campoFirma` (§2.10).

Ejemplo real y ya probado: `src/test/resources/com/educaflow/base/infrastructure/pdfgenerator/documentotexto/certificado/certificado_horario.xml` (un certificado de horario del profesorado) y los demás `documentotexto/*.xml` de esa carpeta.

---

## 1. Conceptos clave

- Raíz `<documentoTexto>`, XSD `documentoTexto.xsd`. **MUST** referenciarlo con `xsi:noNamespaceSchemaLocation` igual que un FORMULARIO (`documentos.md` §1).
- **CRITICAL — un solo idioma**: el que se le pide al generador (`documentos.md` §1.1). Los dos idiomas viven igualmente en los hijos `<valenciano>`/`<castellano>` de cada elemento; el generador estampa **solo uno**. **MUST NOT** escribirse dos documentos, uno por idioma.
- **No hay rejilla ni `colspan`**: el cuerpo es texto corrido de arriba abajo entre la cabecera y el pie. No hay `nombreCampo` ni `<check>`: el único valor que se estampa es el de los `${expresion}` inline.
- **CRITICAL — `<titulo>` es la CABECERA, no un encabezado del texto**: el `<titulo>` es obligatorio y es lo que se estampa en la cabecera de la **primera** página, centrado a la derecha del logo de la GVA (§4.3). Un **encabezado dentro del texto** (`VISTO:`, `SE RESUELVE:`) NO es un `<titulo>`: es un `<parrafo>` con `negrita` y `mayusculas` (§3).
- A diferencia del FORMULARIO, el build **no** inyecta el nombre del trámite cuando falta el `<titulo>`: un documento sin `<titulo>` **no valida** y rompe el build (`documentos.md` §2.7).
- **No hay imágenes**: la cabecera es vectorial (logo + texto) y no hay pie. Un `<documentoTexto>` no declara ningún fichero de imagen.
- Los fragmentos `_*.xml` de un TEXTO son **de tipo TEXTO**: cada XSD declara su propio `<fragmento>`, así que incluir un fragmento de FORMULARIO en un TEXTO (o al revés) **no valida** y rompe el build.

---

## 2. Estructura del XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<documentoTexto xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/educaflow/EducaFlowBuildTools/master/src/main/resources/com/educaflow/common/buildtools/xml2pdf/documentoTexto.xsd">
    <titulo>
        <valenciano>Certificat d'horari del professorat</valenciano>
        <castellano>Certificado de horario del profesorado</castellano>
    </titulo>

    <parrafo negrita="true" mayusculas="true" alineamiento="izquierda">
        <valenciano>...</valenciano>
        <castellano>...</castellano>
    </parrafo>
    <espacio alto="14.76"/>
    <parrafo>
        <valenciano>... ${self.x} ...</valenciano>
        <castellano>... ${self.x} ...</castellano>
    </parrafo>
    <lista>
        <item><castellano>...</castellano></item>
        <item><castellano>...</castellano></item>
    </lista>
    <tabla columnas="2">
        <fila>
            <parrafo alineamiento="centrado"><castellano>...</castellano></parrafo>
            <parrafo alineamiento="centrado"><castellano>...</castellano></parrafo>
        </fila>
    </tabla>
</documentoTexto>
```

**MUST**: el `<titulo>` va **el primero**, antes de todo el cuerpo, exactamente uno, y **no puede faltar**.

## 3. Elementos

| Elemento | Qué dibuja | Atributos | Hijos |
|---|---|---|---|
| `<titulo>` (req., exactamente 1) | La cabecera de la **primera** página: logo de la GVA a la izquierda, este texto centrado en medio, en seminegrita y en mayúsculas, y un hueco en blanco a la derecha (§4.3) | ninguno | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<include>` | Nada por sí mismo: se sustituye por los hijos de la raíz del fragmento `href`, igual que en FORMULARIO (`documentos.md` §2.5) | `href` (req.) | — |
| `<parrafo>` | Un párrafo. Por omisión, **justificado**, en redonda y en la caja en que esté escrito | `alineamiento`, `negrita`, `mayusculas`, `visible`, `siOculto` | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<lista>` | Una lista con viñetas, sangrada | `visible`, `siOculto` | `<item>` (1..n) |
| `<item>` | Un elemento de la lista, detrás de su viñeta | `visible`, `siOculto` | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<espacio>` | Un hueco vertical en blanco | `alto` (req., puntos, > 0, admite decimales), `campoFirma` (el hueco de una firma, `documentos.md` §2.10), `visible`, `siOculto` | — |
| `<tabla>` | Una rejilla **sin bordes** | `columnas` (req., entero > 0), `visible`, `siOculto` | `<fila>` (1..n) |
| `<fila>` | Una fila de la tabla | `visible`, `siOculto` | `<parrafo>`, `<espacio>` |

`alineamiento`: `izquierda` \| `centrado` \| `derecha` \| `justificado`. Cualquier otro valor aborta la generación.

`negrita` y `mayusculas`: `true` \| `false` (por omisión `false`). Cualquier otro valor aborta la generación.
Un **encabezado** (lo que en un FORMULARIO sería un `<titulo>`) es un `<parrafo negrita="true" mayusculas="true" alineamiento="izquierda">`: el `alineamiento` va explícito porque el de un `<parrafo>` es `justificado`, que estiraría un encabezado de más de una línea hasta el borde derecho.

## 4. Reglas

### 4.1 Textos

- **MUST**: los idiomas van en los hijos `<valenciano>`/`<castellano>`, nunca como atributos, igual que en FORMULARIO. El `<valenciano>` omitido lo traduce el build del `<castellano>`; vacío = sin valenciano (`documentos.md` §2.6).
- Un `\n` (un salto de línea de verdad dentro del texto del elemento) es un **salto de línea duro**: abre línea nueva.
- La línea que **cierra un `\n`** no se justifica, ni tampoco la **última de un párrafo**: solo se estiran las líneas interiores de un texto `justificado`.
- `${expresion}` vale en cualquier `<castellano>`/`<valenciano>` y se estampa **como una palabra más**: ocupa lo que mide, empuja lo que le sigue y salta de línea con el texto. Un valor vacío no ocupa nada. Solo se evalúan los del **idioma que se emite**.
- El valor de un `${expresion}` **hereda las `mayusculas`** de su párrafo (una línea en mayúsculas lo está entera) pero **no la `negrita`**: se estampa siempre en redonda.
- `<item>` **no** admite `alineamiento`, `negrita` ni `mayusculas`: va siempre a la izquierda, en redonda, sangrado, y sus líneas siguientes vuelven al sangrado del texto, no a la viñeta.

### 4.2 Tablas

- **MUST**: cada `<fila>` lleva **exactamente `columnas` hijos**, uno por columna. Lo comprueba el build sobre el XML **completo** (ignorando los `visible`) y aborta con ERROR diciendo cuántos hijos tiene y cuántos esperaba. Una celda que debe ir en blanco se escribe como un `<parrafo/>` vacío.
- El **ancho** se reparte a **partes iguales** entre las `columnas`.
- **La tabla NO reparte el alto**: cada fila mide lo que su **celda más alta**, y las celdas de una fila arrancan todas a la misma altura.
- Una `<fila>` es **un solo renglón**: no se parte entre páginas. Un `<parrafo>` o una `<lista>` sueltos sí se parten.
- Dentro de una `<fila>` solo caben `<parrafo>` y `<espacio>`: **MUST NOT** anidarse una `<tabla>` ni una `<lista>` en una celda.

### 4.3 Cabecera y paginación

- La cabecera es la del FORMULARIO pero **sin su recuadro**, en **un solo idioma** y en **tres partes**: el logo de la GVA a la izquierda, el `<titulo>` centrado en la caja del medio, en seminegrita y **siempre en mayúsculas** (no hay atributo que lo decida), y un **hueco en blanco** a la derecha.
- El hueco es el sitio del **código QR del registro de salida** y sus dos líneas de texto, que se estampan después sobre el PDF ya generado, arriba a la derecha de la primera página. El generador no dibuja nada en él y **no se declara** en el XML: lo deja siempre, se registre o no la salida del documento.
- Se dibuja **dentro del flujo**, así que va **solo en la primera página**: las siguientes son cuerpo de arriba abajo. Igual que en un FORMULARIO, que tampoco repite su fila de cabecera.
- Logo y título quedan **centrados verticalmente a la altura del hueco**, que es la parte más alta: un título de varias líneas crece hacia arriba y hacia abajo sin descuadrar el logo.
- El cuerpo arranca **por debajo del hueco**, así que el QR no tapa nunca el primer párrafo. El hueco estrecha la caja del título: un título largo ocupa más líneas, no invade el hueco.
- La cabecera ocupa una banda **más ancha que el cuerpo**: sobresale por los dos lados.
- **No hay pie**: el cuerpo baja hasta el margen inferior en todas las páginas.
- El `<titulo>` admite `${expresion}` inline como cualquier otro texto, y solo se evalúan los del idioma que se emite.

### 4.4 Visibilidad y errores

- `visible`/`siOculto` valen en **todos** los elementos del cuerpo (el `<titulo>` no: va siempre), con la misma semántica que en FORMULARIO (`colapsar` por omisión, o `reservar`) y las mismas reglas: `siOculto` sin `visible` aborta el build, y lo oculto **no se evalúa** (`documentos.md` §2.9).
- Un elemento **reservado** conserva su sitio y su alto pero no dibuja nada: se mide con sus inline **vacíos**, porque sus expresiones no se evalúan.
- **CRITICAL**: toda expresión que no evalúe, y todo `visible` que no devuelva `Boolean`, lanzan `RuntimeException` y **abortan el evento**, igual que en FORMULARIO (`documentos.md` §2.8). Un `<documentoTexto>` sin `<titulo>` no valida contra el XSD y **rompe el build**.

---

## 5. Ejemplo

Recorte del certificado de horario (fichero completo en `src/test/resources/.../documentotexto/certificado/certificado_horario.xml`; ahí las filas de la tabla van con una sola celda porque ese recurso lo lee el generador directamente, sin pasar por la validación del build — un documento de `documentospdf/` **MUST** llevar un hijo por columna, §4.2):

```xml
<documentoTexto ...>
    <titulo>
        <valenciano>Certificat d'horari del professorat</valenciano>
        <castellano>Certificado de horario del profesorado</castellano>
    </titulo>

    <parrafo negrita="true" mayusculas="true" alineamiento="justificado">
        <valenciano>JAVIER ELVIRA SORIA, SECRETARI DEL CENTRE ...</valenciano>
        <castellano>JAVIER ELVIRA SORIA, SECRETARIO DEL CENTRO ...</castellano>
    </parrafo>
    <espacio alto="14.76"/>
    <parrafo negrita="true" mayusculas="true" alineamiento="izquierda">
        <valenciano>CERTIFICA:</valenciano>
        <castellano>CERTIFICA:</castellano>
    </parrafo>

    <espacio alto="29.52"/>
    <parrafo>
        <valenciano>Que Sr. ${self.nombre} amb DNI ${self.dni} és professor definitiu en aquest centre, amb el següent horari:</valenciano>
        <castellano>Que D. ${self.nombre} con DNI ${self.dni} es profesor definitivo en este centro, con el siguiente horario:</castellano>
    </parrafo>

    <espacio alto="14.76"/>
    <lista>
        <item>
            <valenciano>Dilluns: 16.50 - 20.50 hores</valenciano>
            <castellano>Lunes: 16:50 - 20:50 horas</castellano>
        </item>
        <item>
            <valenciano>Dimarts: 9.10 - 14.05 hores</valenciano>
            <castellano>Martes: 9:10 - 14:05 horas</castellano>
        </item>
    </lista>

    <espacio alto="44.28"/>
    <tabla columnas="2">
        <fila>
            <parrafo alineamiento="centrado">
                <valenciano>Vistiplau</valenciano>
                <castellano>VºBº</castellano>
            </parrafo>
            <parrafo/>
        </fila>
        <fila>
            <parrafo alineamiento="centrado">
                <valenciano>EL DIRECTOR</valenciano>
                <castellano>EL DIRECTOR</castellano>
            </parrafo>
            <parrafo alineamiento="centrado">
                <valenciano>EL SECRETARI</valenciano>
                <castellano>EL SECRETARIO</castellano>
            </parrafo>
        </fila>
        <fila>
            <espacio alto="132.84"/>
            <parrafo/>
        </fila>
        <fila>
            <parrafo alineamiento="centrado">
                <valenciano>Signat: José Mariano Monzó del Olmo</valenciano>
                <castellano>Fdo: José Mariano Monzó del Olmo</castellano>
            </parrafo>
            <parrafo alineamiento="centrado">
                <valenciano>Signat: Javier Elvira Soria</valenciano>
                <castellano>Fdo: Javier Elvira Soria</castellano>
            </parrafo>
        </fila>
    </tabla>
</documentoTexto>
```

## 6. Ejemplos ✅/❌

- ✅ CORRECTO: `<parrafo><castellano>Expediente: ${self.numeroExpediente}</castellano></parrafo>` (el inline se estampa como una palabra más del párrafo).
- ✅ CORRECTO: `<espacio alto="44.28"/>` entre el último párrafo y la tabla de firmas (el hueco de firma se hace con espacios, no con `rowSpan`: aquí no hay `rowSpan`).
- ✅ CORRECTO: una `<tabla columnas="2">` cuyas filas llevan dos `<parrafo>`, y el hueco de la firma como `<fila><espacio alto="132.84"/><parrafo/></fila>`.
- ✅ CORRECTO: `<espacio alto="88.56" campoFirma="firmaDirector"/>` como hueco de la firma que estampa la aplicación (la firma se estampa en ese espacio, esté donde esté: `documentos.md` §2.10).
- ✅ CORRECTO: `<parrafo alineamiento="centrado">` para un cargo bajo el recuadro de firma (el `<parrafo>` es justificado por omisión, y un texto de una línea justificado se queda a la izquierda).
- ✅ CORRECTO: `<parrafo negrita="true" mayusculas="true" alineamiento="izquierda"><castellano>Visto:</castellano></parrafo>` para un encabezado (sale `VISTO:` en seminegrita).
- ✅ CORRECTO: `<titulo><castellano>Resolución de anulación de matrícula</castellano></titulo>` como primer hijo (sale en la cabecera, en mayúsculas, aunque se escriba en caja normal).
- ❌ INCORRECTO: `<fila><parrafo>...</parrafo></fila>` en una `<tabla columnas="2">` (falta la celda de la segunda columna: el build aborta).
- ❌ INCORRECTO: `<include href="_datosAlumno.xml"/>` apuntando a un fragmento cuya raíz `<fragmento>` lleva `<seccion>` (es un fragmento de FORMULARIO: no valida contra `documentoTexto.xsd`).
- ❌ INCORRECTO: `<parrafo valenciano="..." castellano="..."/>` (los idiomas van como elementos hijos, nunca como atributos).
- ❌ INCORRECTO: `<item alineamiento="centrado">` (el `<item>` no admite `alineamiento`: el XSD no valida).
- ❌ INCORRECTO: `<titulo><castellano>CERTIFICA:</castellano></titulo>` para un encabezado del texto (el `<titulo>` es la cabecera de la página y solo hay uno; un encabezado es un `<parrafo negrita="true" mayusculas="true" alineamiento="izquierda">`).
- ❌ INCORRECTO: `<parrafo negrita="si">` (los dos booleanos solo admiten `true` y `false`: el XSD no valida y el generador aborta).
- ❌ INCORRECTO: `<parrafo negrita="true" mayusculas="true">` sin `alineamiento` para un encabezado de dos líneas (hereda el `justificado` del `<parrafo>` y se estira hasta el borde derecho).
- ❌ INCORRECTO: un `<documentoTexto>` sin `<titulo>`, o con el `<titulo>` detrás del primer `<parrafo>` (el XSD no valida y el build aborta).
- ❌ INCORRECTO: un `<fragmento>` que lleve `<titulo>` (el título es del documento, no de un fragmento: el XSD no valida).
- ❌ INCORRECTO: `siOculto="colapsar"` sin `visible` (no significa nada: el build aborta).

---

## 7. Anti-patrones

- **MUST NOT** escribir el texto de un encabezado ya en mayúsculas en vez de poner `mayusculas="true"`: la caja es cosa del atributo, y así el `<castellano>` sigue siendo el texto que el traductor pasa al valenciano (`documentos.md` §2.6).
- **MUST NOT** duplicar el documento para tener una versión en castellano y otra en valenciano: los dos idiomas van en el mismo XML y el generador emite el que se le pide (`documentos.md` §1.1).
- **MUST NOT** simular una tabla con espacios o tabuladores dentro de un `<parrafo>` para alinear dos columnas (p.ej. los dos cargos que firman): para eso está `<tabla>`, que reparte el ancho a partes iguales.
- **MUST NOT** usar una `<tabla>` para dibujar un recuadro: las tablas de un documento en prosa **no llevan bordes**.
- **MUST NOT** contar con que una `<fila>` alta se parta entre páginas: no se parte. Si un bloque debe poder partirse, es un `<parrafo>` o una `<lista>`, no una fila.
- **MUST NOT** tapar por sistema con `?.` o `?: ""` una expresión que puede fallar: lo que no aplica se quita con `visible` (`documentos.md` §2.8).
- **MUST NOT** repetir el `<titulo>` como primer `<parrafo>` del cuerpo: ya sale en la cabecera y saldría dos veces.
- **MUST NOT** documentar ni reimplementar aquí cómo se resuelve el XML ni cómo se maqueta el PDF: este fichero describe el **formato**.

---

## Quick Guidelines

- Raíz `<documentoTexto>`, XSD `documentoTexto.xsd`; un solo idioma, el que se le pida al generador; lo común con el FORMULARIO está en `documentos.md`.
- `<titulo>` **obligatorio** y el primero: es la **cabecera** de la primera página (logo GVA + texto centrado, en mayúsculas + hueco para el QR del registro de salida), no un encabezado del texto. Cuerpo de arriba abajo: `<parrafo>`, `<lista>`/`<item>`, `<espacio>` y `<tabla>`/`<fila>`. No hay rejilla, ni `colspan`, ni `nombreCampo`, ni `<check>`.
- `alineamiento` = `izquierda`/`centrado`/`derecha`/`justificado` (por omisión `justificado`); `negrita` y `mayusculas` = `true`/`false` (por omisión `false`). El `<item>` no admite ninguno. Un encabezado es `<parrafo negrita="true" mayusculas="true" alineamiento="izquierda">`.
- `\n` = salto de línea duro; la línea que lo cierra y la última de un párrafo no se justifican.
- `${expresion}` en cualquier `<castellano>`/`<valenciano>`, estampado como una palabra más; solo se evalúan los del idioma emitido. Hereda las `mayusculas` de su párrafo, no su `negrita`.
- El hueco de una firma es un `<espacio campoFirma="<nombre>">`: se firma por ese nombre, sin coordenadas (`documentos.md` §2.10).
- `<tabla>`: sin bordes, ancho a partes iguales, cada `<fila>` con **exactamente `columnas` hijos** (lo valida el build) y alta como su celda más alta; una fila no se parte entre páginas.
- La cabecera va **solo en la primera página** y **no hay pie**: el cuerpo baja hasta el margen inferior en todas. No hay imágenes en un documento en prosa.
- `visible`/`siOculto` en **todos** los elementos del cuerpo (el `<titulo>` no), misma semántica que en FORMULARIO; lo reservado guarda el hueco y no evalúa sus inline.
- **CRITICAL**: expresión que no evalúa o `visible` no `Boolean` → `RuntimeException` y el evento **no** termina. Documento sin `<titulo>` → el build no valida.
