# Los documentos PDF (`documentospdf/`)

Los documentos son los PDF con los que se materializa la tramitación: los que los usuarios **presentan** al centro (p.ej. una solicitud, entran por registro de entrada) y los que la aplicación **emite** (p.ej. una resolución, salen por registro de salida). Cada tipo de expediente tiene una carpeta `documentospdf/` con sus documentos: para cada uno contiene **o** el XML de definición del que EducaFlowBuildTools genera el PDF rellenable en el build (tarea `generatePdfDocuments`), **o** directamente el PDF ya hecho. Este fichero documenta **el formato de ese XML**; la generación del PDF no es responsabilidad de este skill (la hace EducaFlowBuildTools, herramienta `xml2pdf`). Ejemplos reales: los `*.xml` de las carpetas `documentospdf/` de los trámites (`src/main/java/com/educaflow/tramites/**`) y `disenyo-grafico/documentos/`.

---

## 1. Conceptos clave

- El documento renderizado es **una única tabla** sobre una rejilla lógica de **12 columnas** (19 cm: página A4 con márgenes de 1 cm), con la cabecera corporativa (logo GVA + título bilingüe) y secciones con letra automática (A, B, C…) en celda gris.
- Todo texto visible es **bilingüe** (el castellano se renderiza en cursiva) y va **siempre** en los elementos hijos `<valenciano>` y `<castellano>` del elemento (`titulo`, `seccion`, `campo`, `check`, `texto`), nunca como atributos.
- El **`<titulo>` es opcional**: si el documento no lleva ninguno, se titula con el `<name>` del `TramiteInstance.xml` del trámite padre (§2.7).
- El **`<valenciano>` es opcional**: si se omite, el generador lo calcula **traduciendo el `<castellano>`** con el traductor `apertium` (§2.6). Un `<castellano>` omitido o vacío omite el castellano; un `<valenciano>` **vacío** (`<valenciano></valenciano>`) omite el valenciano — omitirlo y ponerlo vacío **no** es lo mismo.
- El formato está descrito por el XSD `documento.xsd`, que vive en `EducaFlowBuildTools` (`src/main/resources/com/educaflow/common/buildtools/xml2pdf/documento.xsd`). Todo XML de definición **MUST** referenciarlo en la raíz con `xsi:noNamespaceSchemaLocation` usando su URL de GitHub en la rama master (la del ejemplo de §2.1, idéntica en todos los XML). El generador valida cada XML contra ese XSD al cargarlo y aborta con ERROR si no valida (usa el XSD incluido en su jar, sin acceso a red; la URL es solo la referencia declarativa).
- Cada `<campo>`/`<check>` produce un **campo rellenable** del formulario cuyo nombre es el `nombreCampo` **literal**. **CRITICAL**: `nombreCampo` no es realmente un nombre — es una **expresión Groovy** que se evalúa en runtime para obtener el valor del campo, donde `self` es **el objeto del tipo de expediente** (la instancia de la entidad del expediente concreto en cuya carpeta está el XML). Todo el detalle del contexto, la potencia de las expresiones y la conversión de valores: §2.8.
- Carpeta `documentospdf/` (o `documentos/`): cada documento del trámite está **o** como XML de definición **o** directamente como PDF versionado. La disyuntiva es **por documento, no por carpeta**: es lícito y normal que en la misma carpeta convivan el XML de un documento con el PDF de otro. El caso típico: si el trámite tiene **impreso oficial** (de la administración), **se usa ese PDF tal cual** — se deja versionado en la carpeta y no se redefine por XML; el XML es para los documentos propios del centro que no tienen impreso oficial. Los `_*.xml` son **fragmentos** reutilizables (raíz `<fragmento>`) que los documentos incluyen con `<include href="..."/>` (§2.5) y no generan PDF propio. **MUST NOT** convivir en la misma carpeta un `aa.xml` (raíz `<documento>`) con un `aa.pdf` versionado: el build aborta por ambigüedad.
- Cada `.pdf` resultante (generado o versionado) produce una constante del enum `TipoDocumentoPdf` de la entidad (`modelo.md` §5) con la que el EventManager lo obtiene y rellena (`eventmanager.md` §6.1). Nombres de fichero en camelCase sin espacios ni guiones y extensión `.pdf` en minúsculas (`solicitudFirmada.pdf` → `SOLICITUD_FIRMADA`; un nombre inválido rompe la compilación después, sin aviso del build).

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

Los `<valenciano>` pueden omitirse; entonces se calculan traduciendo el `<castellano>` (§2.6):

```xml
    <campo nombreCampo="self.centro.name" colspan="4">
        <castellano>Nombre</castellano>       <!-- valenciano: "Nom", traducido en el build -->
    </campo>
```

### 2.2 Elementos

| Elemento | Genera | Atributos | Hijos |
|---|---|---|---|
| `<titulo>` (opc., §2.7) | Fila de cabecera: logo GVA + título bilingüe centrado | — | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<seccion>` | Fila con letra gris automática (A, B, C…) + título | — | `<valenciano>` (opc.), `<castellano>` (opc.), `<fila>` |
| `<fila>` | Una o varias líneas de la tabla | — | `campo`/`check`/`texto` |
| `<campo>` | Etiqueta bilingüe en mayúsculas + campo de texto rellenable | `nombreCampo`, `colspan`, `rowSpan` (opc.) | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<check>` | Casilla (ocupa 1 columna) + etiqueta bilingüe al lado | `nombreCampo`, `colspan`, `rowSpan` (opc.) | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<texto>` | Párrafos bilingües sin campo | `colspan`, `rowSpan` (opc.) | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<include>` | Nada por sí mismo: se sustituye por los hijos de la raíz del fragmento `href` (§2.5) | `href` | — |

### 2.3 Reglas

- **MUST**: como máximo un `<titulo>` y, si está, es lo primero del documento (o del fragmento). Aplica también tras expandir los includes: si un fragmento aporta el título, el documento que lo incluye **MUST NOT** declarar otro.
- El `<titulo>` es **opcional**: si el documento no lleva ninguno (ni propio ni de un fragmento), el generador le pone al principio del todo el nombre del trámite (§2.7).
- **MUST**: los `colspan` de una `<fila>` suman **12 o un múltiplo de 12**. Cada grupo que suma 12 es una línea; las líneas siguientes se apilan debajo **dentro del mismo rectángulo** (sin borde entre ellas). Un elemento **MUST NOT** cruzar el límite de 12.
- `colspan` admite decimales (`colspan="3.5"`).
- `rowSpan` (≥1, admite decimales) hace más alta la fila y su campo de texto. Úsalo para campos multilínea y para el recuadro de firma.
- **Campos inline**: dentro de los hijos `<valenciano>`/`<castellano>` de cualquier elemento, `${expresion;n}` se sustituye por un campo rellenable de `n` columnas de ancho (admite decimales) cuyo nombre es la expresión. Si aparece en ambos idiomas, en el PDF queda **un único campo con dos widgets** (mismo nombre, mismo valor en ambos).
- `nombreCampo` con comillas dobles (expresiones como `"    " + f(x)`) → escribe el atributo XML con comillas simples: `nombreCampo='"    " + com...sha256(self.justificante)'`.
- Un `<campo>` sin ninguno de los dos hijos (o con los dos vacíos) es un campo **sin etiqueta** (útil para apilar varios campos en un rectángulo).
- El `<valenciano>` **omitido** se traduce del `<castellano>`; para un texto **solo en castellano** hay que poner el `<valenciano>` **vacío** (§2.6).
- **MUST NOT** poner `valenciano`/`castellano` como atributos de ningún elemento (formato antiguo): el XSD no valida y el generador aborta.

### 2.4 Ejemplos ✅/❌

- ✅ CORRECTO: `<fila>` con `colspan` 5 + 5 + 2 (suma 12).
- ✅ CORRECTO: `<fila>` con cuatro `<texto colspan="12">` (suma 48 = 4 líneas apiladas en un rectángulo).
- ✅ CORRECTO: `<fila>` con 6 + 2.1 + 3.9 (decimales, suma 12).
- ✅ CORRECTO: `<valenciano>Jornada parcial. De ${self.horaInicio;1.1} hores a ${self.horaFin;1.1} hores</valenciano>`.
- ✅ CORRECTO: `<campo nombreCampo="self.x" colspan="4"><castellano>Nombre</castellano></campo>` (sin `<valenciano>`: se traduce a "Nom" en el build).
- ✅ CORRECTO: `<valenciano></valenciano>` + `<castellano>...</castellano>` (etiqueta **solo** en castellano, sin traducir).
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
- **CRITICAL para el versionado**: si un fragmento contiene expresiones Groovy con FQCN de enums versionados (`...TipoJornadaFaltaJustificacionFaltaProfesoradoV1.TODA_LA_JORNADA`), esas referencias cambian en cada versión nueva (`versionado.md`).

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

### 2.6 El `<valenciano>` que falta se traduce del `<castellano>`

Si un elemento no lleva `<valenciano>`, el generador lo calcula traduciendo su `<castellano>` con el proceso traductor externo `apertium` (el mismo que usa la i18n de la aplicación). La traducción ocurre **en el build**, después de expandir los `<include>` (también se traducen los textos de los fragmentos); el XML **no** se modifica y el `<valenciano>` traducido solo existe dentro del PDF generado.

- Para un texto **solo en castellano** hay que poner el `<valenciano>` **vacío**: `<valenciano></valenciano>`. Omitirlo significa "tradúcelo".
- **No se traducen** los campos inline `${expresion;n}` ni las URL: el generador los protege y los restaura tal cual.
- Para que **no se traduzca** ninguna otra cosa (siglas, nombres propios, marcas…) se le pega el sufijo `__!!` en el `<castellano>`: `(RATs__!!)`. El sufijo **no se dibuja** en el PDF, solo evita la traducción de esa palabra.
- Si el traductor no sabe traducir alguna palabra, **el build falla** con el texto y el fichero. Se arregla de una de estas dos formas: escribir el `<valenciano>` a mano, o marcar la palabra con `__!!`.
- La traducción automática es una comodidad para textos nuevos y sencillos (etiquetas, títulos). Para el texto legal largo, revísala: apertium acierta la gramática pero no el registro administrativo.

### 2.7 El `<titulo>` que falta sale del trámite

Si el documento no lleva `<titulo>` (ni propio ni aportado por un fragmento), el generador le añade uno **al principio del todo** con el `<name>` del `TramiteInstance.xml` del **trámite padre**, y lo traduce al valenciano (§2.6). O sea: por omisión, un documento se titula como su trámite.

- El `TramiteInstance.xml` se busca **subiendo por las carpetas padre** desde la del XML: los documentos están en `<tramite>/<vN>/documentospdf/` y el `TramiteInstance.xml` en `<tramite>/`. **MUST** existir y tener `<name>`, o el build falla.
- Cambiar el `<name>` del `TramiteInstance.xml` regenera en el build los PDF de los documentos de ese trámite.
- Pon un `<titulo>` explícito solo cuando el documento **deba** titularse distinto del trámite.

```xml
<documento ...>
    <!-- sin <titulo>: se titula con el <name> del TramiteInstance.xml del trámite -->
    <seccion>...</seccion>
</documento>
```

### 2.8 Las expresiones Groovy (`nombreCampo` y `${...}`)

**Cuándo se evalúan**: NO en el build — el build solo genera el PDF con el formulario vacío. Las expresiones se evalúan **en runtime**, cada vez que el EventManager pide el documento (`expediente.getDocumentoPdf(...)` → `DocumentoPdfUtil.generate`); después el formulario se **aplana** (el PDF resultante ya no es editable).

**Contexto disponible** (variables del binding):

| Variable | Valor |
|---|---|
| `self` | El **objeto del tipo de expediente**: la instancia de la entidad (`extends Expediente`) del expediente concreto. Da acceso a todos sus campos propios y heredados (`self.personaInteresada.nombre`, `self.numeroExpediente`, `self.centro.name`…) |
| `now` | `java.time.LocalDateTime.now()` (fecha/hora de generación) |

**Potencia**: se evalúan con `GroovyShell`, así que vale cualquier expresión Groovy:

- Navegación de propiedades: `self.personaInteresada.dni`, `self.centro.municipio.name`.
- Navegación segura y elvis: `self.otroMotivo?.toUpperCase()`, `self.otroMotivo ?: ""`.
- Llamadas a métodos: `String.valueOf(self.anyo)`, `now.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))`.
- Clases por FQCN (no hay imports): `com.educaflow.base.util.MetaFileUtil.sha256(self.justificante)`.
- Comparaciones para los `<check>`: `self.tipoJornadaFalta==com.educaflow.subsystem.expedientes.db.TipoJornadaFalta<Entidad>.TODA_LA_JORNADA`, o el literal `true` (casilla siempre marcada).
- Concatenación: `'"    " + com...sha256(self.justificante)'` (atributo con comillas simples, §2.3).

**Conversión del resultado a texto** (lo que se estampa en el campo):

| Resultado | Se estampa |
|---|---|
| `Boolean` | `Yes`/`Off` — marca o desmarca la casilla de un `<check>` (en un campo de texto saldría literalmente "Yes") |
| `null` | vacío |
| Enteros / decimales | formateados con locale español (decimales: máximo 2) |
| `LocalDate` / `LocalTime` / `LocalDateTime` | `dd/MM/yyyy` / `HH:mm` / `dd/MM/yyyy HH:mm` |
| Enum de Axelor | su `title` del `<item>` del dominio (o el `name` humanizado si no tiene `title`); también vale acceder explícitamente: `self.tipoResolucion.title` |
| Resto | `toString()` |

**CRITICAL — los fallos son silenciosos**: una expresión que revienta al evaluarse (propiedad inexistente, NPE en la cadena…) **no hace fallar ni el build ni el evento** — el error solo se escribe en el log del servidor y el campo queda **vacío**. Tras cambiar expresiones, **MUST** revisar el PDF generado en runtime (y el log si falta algún valor). Una expresión vacía se salta sin evaluar.

---

## 3. Anti-patrones

- **MUST NOT** editar el PDF generado para "arreglar" el documento: cambia el XML de definición — el PDF se regenera al compilar.
- **MUST NOT** quitar un `<valenciano>` ya escrito para dejar que lo traduzca el build: la traducción automática (§2.6) es para textos **nuevos**, no para sustituir el valenciano oficial de un documento existente.
- **MUST NOT** documentar ni reimplementar aquí la generación del PDF: esa implementación vive en `EducaFlowBuildTools` (herramienta `xml2pdf`).

---

## Quick Guidelines

- Rejilla de 12 columnas; cada `<fila>` suma 12 o un múltiplo de 12 (múltiplo = líneas apiladas en el mismo rectángulo).
- Idiomas: siempre elementos hijos `<valenciano>`/`<castellano>`, en todos los elementos (`titulo`, `seccion`, `campo`, `check`, `texto`); nunca atributos.
- El `<titulo>` es opcional: sin él, el documento se titula con el `<name>` del `TramiteInstance.xml` del trámite padre, traducido al valenciano (§2.7).
- El `<valenciano>` es opcional: **omitido** = lo traduce el build del `<castellano>`; **vacío** = solo castellano. Los `${...}` y las URL no se traducen; lo demás que no deba traducirse se marca con el sufijo `__!!`, que no se dibuja (§2.6).
- Todo XML referencia el XSD `documento.xsd` de `EducaFlowBuildTools` con `xsi:noNamespaceSchemaLocation` = su URL de GitHub en master; el generador lo valida al cargarlo (y puedes adelantarte con `xmllint --schema` contra la copia local de `../EducaFlowBuildTools`).
- `colspan` y `rowSpan` admiten decimales; la casilla de un `<check>` ocupa siempre 1 columna.
- `${expresion;n}` = campo inline de `n` columnas dentro de cualquier texto bilingüe.
- `nombreCampo` no es un nombre: es una **expresión Groovy** que obtiene el valor del campo, evaluada **en runtime** con `self` (el objeto del tipo de expediente) y `now`; los fallos de evaluación son silenciosos (log + campo vacío) — revisa el PDF generado (§2.8). Con comillas dobles dentro, atributo con comillas simples.
- En `documentospdf/` cada documento está **o** como XML de definición **o** directamente como PDF versionado (nunca ambos para el mismo documento; mezclar XML de unos y PDF de otros en la carpeta es lo normal). Si existe **impreso oficial**, se versiona ese PDF tal cual en vez de definirlo por XML. Este fichero solo define el **formato del XML**; el PDF lo genera el build (`generatePdfDocuments` / EducaFlowBuildTools).
- Partes comunes: fragmentos `_*.xml` (raíz `<fragmento>`) incluidos con `<include href="..."/>` solo a nivel de documento/fragmento, recursivos, validados también tras expandir (§2.5).
