---
name: k-documentos
description: Documentos PDF de los trámites de la secretaría virtual — los PDF que los usuarios presentan al centro (p.ej. solicitudes) y los que la aplicación emite para los usuarios (p.ej. resoluciones), pieza central de la tramitación. Qué contiene la carpeta `documentospdf/` (o los XML de definición de los que EducaFlowBuildTools genera los PDF en el build, o directamente los PDF), el formato XML de definición de los formularios oficiales (GVA) (documento/seccion/fila/campo/check/texto con hijos valenciano/castellano, rejilla de 12 columnas, campos inline ${...}, rowSpan, nombreCampo como expresión Groovy con self, fragmentos reutilizables _*.xml con raíz fragmento incluidos con include href, XSD documento.xsd en EducaFlowBuildTools) y cómo transcribir un .odt antiguo GVA a este XML. Úsalo siempre que haya que crear, modificar o transcribir uno de estos XML o trabajar con una carpeta documentospdf. La generación del PDF desde el XML NO es de este skill - la hace EducaFlowBuildTools en el build de secretaria-virtual (tarea generatePdfDocuments).
allowed-tools: Read, Write, Edit, Bash(python3 .claude/skills/libreoffice-writer/scripts/odt_verify.py:*), Bash(unzip:*), Bash(xmllint:*), Bash(grep:*)
---

# k-documentos

Los documentos son una pieza central de la aplicación: son los PDF con los que se materializa la tramitación administrativa. Son de dos clases según quién los aporta:

- Los que los usuarios **presentan** al centro (p.ej. una solicitud), que entran por el registro de entrada.
- Los que la aplicación **emite** para los usuarios (p.ej. una resolución), que salen por el registro de salida.

Cada trámite tiene una carpeta `documentospdf/` con sus documentos: para cada documento contiene **o** el XML de definición del que `EducaFlowBuildTools` genera el PDF rellenable en el build, **o** directamente el PDF ya hecho. Este skill documenta **el formato de ese XML** (para escribirlo o modificarlo) y cómo **transcribir** un `.odt` antiguo GVA a este XML. Cómo se implementa la generación del PDF **no** es responsabilidad de este skill: la hace `EducaFlowBuildTools`. Ejemplos reales: los `*.xml` de las carpetas `documentospdf/` de los trámites (`src/main/java/com/educaflow/tramites/**`) y `disenyo-grafico/documentos/`.

---

## 1. Conceptos clave

- El documento renderizado es **una única tabla** sobre una rejilla lógica de **12 columnas** (19 cm: página A4 con márgenes de 1 cm), con la cabecera corporativa (logo GVA + título bilingüe) y secciones con letra automática (A, B, C…) en celda gris.
- Todo texto visible es **bilingüe** (el castellano se renderiza en cursiva) y va **siempre** en los elementos hijos `<valenciano>` y `<castellano>` del elemento (`titulo`, `seccion`, `campo`, `check`, `texto`). Un hijo omitido o vacío omite ese idioma.
- El formato está descrito por el XSD `documento.xsd`, que vive en `EducaFlowBuildTools` (`src/main/resources/com/educaflow/common/buildtools/xml2pdf/documento.xsd`). Todo XML de definición **MUST** referenciarlo en la raíz con `xsi:noNamespaceSchemaLocation` usando su URL de GitHub en la rama master (la del ejemplo de §2.1, idéntica en todos los XML). El generador valida cada XML contra ese XSD al cargarlo y aborta con ERROR si no valida (usa el XSD incluido en su jar, sin acceso a red; la URL es solo la referencia declarativa).
- Cada `<campo>`/`<check>` produce un **campo rellenable** del formulario cuyo nombre es el `nombreCampo` **literal**. **CRITICAL**: `nombreCampo` no es realmente un nombre — es una **expresión Groovy** que el motor de relleno evalúa para obtener el valor del campo, y `self` referencia el **modelo del tipo de expediente** en cuya carpeta está el XML. Por eso son válidos `self.dni`, `String.valueOf(self.anyo)`, `self.tipo==com.educaflow...Enum.VALOR` (checks) o `true` (casilla siempre marcada).
- Carpeta `documentospdf/` (o `documentos/`): cada documento del trámite está **o** como XML de definición —el build de `secretaria-virtual` genera su PDF (tarea `generatePdfDocuments` → `EducaFlowBuildTools`)— **o** directamente como PDF versionado. Los `_*.xml` son **fragmentos** reutilizables (raíz `<fragmento>`) que los documentos incluyen con `<include href="..."/>` (§2.5) y no generan PDF propio. **MUST NOT** convivir en la misma carpeta un `aa.xml` (raíz `<documento>`) con un `aa.pdf` versionado: el build aborta por ambigüedad.

---

## 2. Formato del XML

### 2.1 Estructura

```xml
<?xml version="1.0" encoding="UTF-8"?>
<documento xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/educaflow/EducaFlowBuildTools/master/src/main/resources/com/educaflow/common/buildtools/xml2pdf/documento.xsd">
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
```

(La URL del `xsi:noNamespaceSchemaLocation` es siempre esa, idéntica en todos los XML, esté donde esté el fichero.)

### 2.2 Elementos

| Elemento | Genera | Atributos | Hijos |
|---|---|---|---|
| `<titulo>` | Fila de cabecera: logo GVA + título bilingüe centrado | — | `<valenciano>`, `<castellano>` (opc.) |
| `<seccion>` | Fila con letra gris automática (A, B, C…) + título | — | `<valenciano>`, `<castellano>` (opc.), `<fila>` |
| `<fila>` | Una o varias líneas de la tabla | — | `campo`/`check`/`texto` |
| `<campo>` | Etiqueta bilingüe en mayúsculas + campo de texto rellenable | `nombreCampo`, `colspan`, `rowSpan` (opc.) | `<valenciano>`, `<castellano>` (opc.) |
| `<check>` | Casilla (ocupa 1 columna) + etiqueta bilingüe al lado | `nombreCampo`, `colspan`, `rowSpan` (opc.) | `<valenciano>`, `<castellano>` (opc.) |
| `<texto>` | Párrafos bilingües sin campo | `colspan`, `rowSpan` (opc.) | `<valenciano>`, `<castellano>` (opc.) |
| `<include>` | Nada por sí mismo: se sustituye por los hijos de la raíz del fragmento `href` (§2.5) | `href` | — |

### 2.3 Reglas

- **MUST**: como máximo un `<titulo>` y, si está, es lo primero del documento (o del fragmento). Aplica también tras expandir los includes: si un fragmento aporta el título, el documento que lo incluye **MUST NOT** declarar otro.
- **MUST**: los `colspan` de una `<fila>` suman **12 o un múltiplo de 12**. Cada grupo que suma 12 es una línea; las líneas siguientes se apilan debajo **dentro del mismo rectángulo** (sin borde entre ellas). Un elemento **MUST NOT** cruzar el límite de 12.
- `colspan` admite decimales (`colspan="3.5"`).
- `rowSpan` (≥1, admite decimales) hace más alta la fila y su campo de texto. Úsalo para campos multilínea y para el recuadro de firma.
- **Campos inline**: dentro de los hijos `<valenciano>`/`<castellano>` de cualquier elemento, `${expresion;n}` se sustituye por un campo rellenable de `n` columnas de ancho (admite decimales) cuyo nombre es la expresión. Si aparece en ambos idiomas, en el PDF queda **un único campo con dos widgets** (mismo nombre, mismo valor en ambos).
- `nombreCampo` con comillas dobles (expresiones como `"    " + f(x)`) → escribe el atributo XML con comillas simples: `nombreCampo='"    " + com...sha256(self.justificante)'`.
- Un `<campo>` sin hijos `<valenciano>`/`<castellano>` (o vacíos) es un campo **sin etiqueta** (útil para apilar varios campos en un rectángulo).
- **MUST NOT** poner `valenciano`/`castellano` como atributos de ningún elemento (formato antiguo): el XSD no valida y el generador aborta.

### 2.4 Ejemplos ✅/❌

- ✅ CORRECTO: `<fila>` con `colspan` 5 + 5 + 2 (suma 12).
- ✅ CORRECTO: `<fila>` con cuatro `<texto colspan="12">` (suma 48 = 4 líneas apiladas en un rectángulo).
- ✅ CORRECTO: `<fila>` con 6 + 2.1 + 3.9 (decimales, suma 12).
- ✅ CORRECTO: `<valenciano>Jornada parcial. De ${self.horaInicio;1.1} hores a ${self.horaFin;1.1} hores</valenciano>`.
- ❌ INCORRECTO: `<fila>` con 5 + 3 + 2 (suma 10; el generador aborta con ERROR).
- ❌ INCORRECTO: `<fila>` con 8 + 8 (el segundo elemento cruza el límite de 12).
- ❌ INCORRECTO: `${self.hora}` (falta el ancho; la sintaxis es `${expresion;n}`).
- ❌ INCORRECTO: `<campo nombreCampo="self.x" valenciano="Nom" castellano="Nombre" colspan="4"/>` (formato antiguo: los idiomas van siempre como elementos hijos, también en `titulo`/`seccion`).

### 2.5 Fragmentos reutilizables (`_*.xml` + `<include>`)

Para compartir partes comunes entre documentos (del mismo trámite o de varios):

- Un **fragmento** es un fichero cuyo nombre **MUST** empezar por `_`, con raíz `<fragmento>`, la misma referencia al XSD y el mismo contenido posible que `<documento>`: `titulo`, `seccion` e `include`.
- `<include href="..."/>` va **solo como hijo directo** de `<documento>` o `<fragmento>`, en cualquier posición y cuantas veces haga falta. **MUST NOT** ir dentro de una `<seccion>`: no se incluyen trozos de sección.
- El generador sustituye cada `<include>` por **los hijos de la raíz** del fragmento, recursivamente (un fragmento puede incluir otros fragmentos). El `href` se resuelve relativo al fichero que lo incluye. Un ciclo de includes aborta con ERROR.
- Se valida contra el XSD cada fichero por separado **y** el documento ya expandido. Las letras de sección (A, B, C…) se asignan sobre el documento expandido.
- Cambiar un fragmento regenera en el build los PDF de todos los documentos que lo incluyen, directa o transitivamente.

```xml
<documento ...>
    <include href="_cabecera.xml"/>
    <seccion>... lo específico de este documento ...</seccion>
    <include href="../../otra_carpeta/_proteccion_datos.xml"/>
</documento>
```

- ✅ CORRECTO: `<include href="_template.xml"/>` como hijo de `<documento>`, antes o después de cualquier `<seccion>`.
- ✅ CORRECTO: un fragmento con `<titulo>` (el documento que lo incluye ya no declara otro).
- ❌ INCORRECTO: `<include>` dentro de `<seccion>` (los includes solo van al nivel documento/fragmento; el XSD no valida).
- ❌ INCORRECTO: `href="cabecera.xml"` (el nombre de fichero del fragmento debe empezar por `_`; el XSD no valida).

---

## 3. Transcribir un .odt antiguo al XML

Receta para convertir un formulario `.odt` de formato antiguo (hecho a mano en la GVA) en el XML:

1. Renderiza el `.odt` a imagen y estudia su estructura visual (usa el skill `libreoffice-writer`):
   ```bash
   python3 .claude/skills/libreoffice-writer/scripts/odt_verify.py <doc>.odt -r 120
   ```
2. Extrae y formatea su contenido:
   ```bash
   unzip -p <doc>.odt content.xml > /tmp/content.xml   # formatea con xml.dom.minidom para leerlo
   ```
3. Lista los controles (`grep -o 'form:name="[^"]*"' `): el orden de declaración da `control1..N`; anota tipo (`form:text`/`form:checkbox`) y nombre. **MUST** copiar los nombres **literales** al `nombreCampo`, aunque sean expresiones (`String.valueOf(...)`, `==Enum.X`, `true`).
4. Convierte los anchos: con las `style:rel-column-width` de las columnas, cada celda ocupa `suma_rel_celda * 12 / suma_rel_total` columnas → ese es su `colspan`. Redondea a enteros o a un decimal limpio (2.1, 3.9) según lo que claramente quiso el autor.
5. Mapea cada fila de la tabla:
   - Fila de título con logo → `<titulo>`.
   - Fila-cabecera de bloque (fondo/negrita, texto "Val / Cast") → `<seccion>` separando los dos idiomas en sus hijos `<valenciano>`/`<castellano>`.
   - Celda con etiqueta + control → `<campo>`; celda con casilla + texto → `<check>`; celda solo texto → `<texto>`; los idiomas van siempre en los hijos `<valenciano>`/`<castellano>`.
   - Varios bloques apilados en una misma celda o celdas sin borde entre sí → misma `<fila>` con varias líneas de 12 (campos extra sin etiqueta si no la tienen).
   - Control incrustado en medio de una frase → `${expresion;n}` con `n = ancho_cm * 12 / 19` (12 / ancho de tabla; en los `.odt` antiguos, de márgenes más anchos, la tabla solía medir 17).
   - Celda notablemente alta (campo multilínea, recuadro de firma) → `rowSpan` (≈ alto_cm / 0.6 unidades extra).
6. Detalles de equivalencia: los enlaces van como texto plano (no hay hipervínculos en el XML); si una URL aparece una sola vez compartida, ponla solo en `castellano`; texto solo en un idioma → omite el otro hijo.
7. Valida el XML contra el XSD (el build lo hace igualmente al cargarlo, pero así el fallo aparece antes):
   ```bash
   xmllint --noout --schema ../EducaFlowBuildTools/src/main/resources/com/educaflow/common/buildtools/xml2pdf/documento.xsd <definicion>.xml
   ```
   Las sumas de `colspan` **no** las valida el XSD: las comprueba el generador de `EducaFlowBuildTools` al compilar (aborta con ERROR si no suman múltiplo de 12).
8. Avisa al usuario de lo que **no** se conserva al regenerar: el estándar nuevo añade letras de sección y cabeceras bilingües en dos líneas, y unifica tipografías; el documento puede quedar más alto y repaginar.

---

## 4. Anti-patrones

- **MUST NOT** editar el PDF generado para "arreglar" el documento: cambia el XML de definición — el PDF se regenera al compilar.
- **MUST NOT** "mejorar" los textos al transcribir: la transcripción es literal (incluidos errores del original; señálalos al usuario aparte).
- **MUST NOT** inventar `nombreCampo`: si el control del documento original se llama `true` o es una expresión, se copia tal cual.
- **MUST NOT** documentar ni reimplementar aquí la generación del PDF: esa implementación vive en `EducaFlowBuildTools` (herramienta `xml2pdf`).

---

## Quick Guidelines

- Rejilla de 12 columnas; cada `<fila>` suma 12 o un múltiplo de 12 (múltiplo = líneas apiladas en el mismo rectángulo).
- Idiomas: siempre elementos hijos `<valenciano>`/`<castellano>`, en todos los elementos (`titulo`, `seccion`, `campo`, `check`, `texto`); nunca atributos.
- Todo XML referencia el XSD `documento.xsd` de `EducaFlowBuildTools` con `xsi:noNamespaceSchemaLocation` = su URL de GitHub en master; el generador lo valida al cargarlo (y puedes adelantarte con `xmllint --schema` contra la copia local de `../EducaFlowBuildTools`).
- `colspan` y `rowSpan` admiten decimales; la casilla de un `<check>` ocupa siempre 1 columna.
- `${expresion;n}` = campo inline de `n` columnas dentro de cualquier texto bilingüe.
- `nombreCampo` no es un nombre: es una **expresión Groovy** que obtiene el valor del campo; `self` = modelo del tipo de expediente donde está el XML. Con comillas dobles dentro, atributo con comillas simples.
- En `documentospdf/` cada documento está **o** como XML de definición **o** directamente como PDF versionado (nunca ambos). Este skill solo define el **formato del XML**; el PDF lo genera el build (`generatePdfDocuments` / `EducaFlowBuildTools`).
- Partes comunes: fragmentos `_*.xml` (raíz `<fragmento>`) incluidos con `<include href="..."/>` solo a nivel de documento/fragmento, recursivos, validados también tras expandir (§2.5).
- Transcribir ODT→XML: render + `content.xml` + `form:name` literales + anchos relativos → colspans; validar con `xmllint`.
