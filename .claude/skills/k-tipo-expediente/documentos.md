# Los documentos PDF (`documentospdf/`)

Los documentos son los PDF con los que se materializa la tramitación: los que los usuarios **presentan** al centro (p.ej. una solicitud, entran por registro de entrada) y los que la aplicación **emite** (p.ej. una resolución, salen por registro de salida). Cada tipo de expediente tiene una carpeta `documentospdf/` con sus documentos: para cada uno contiene **o** el XML de definición del documento, **o** directamente el PDF ya hecho. Con el XML, el build expande los `<include>`, pone el título del trámite y traduce el valenciano, y la aplicación **dibuja el PDF en runtime** cada vez que un `PhaseEventManagerImpl` lo pide, ya con los datos del expediente: el PDF sale **plano**, sin formulario, y los elementos con `visible` se quitan o se dejan en blanco según el expediente (§2.9). Este fichero documenta **el formato de ese XML**. Ejemplos reales: los `*.xml` de las carpetas `documentospdf/` de los trámites (`src/main/java/com/educaflow/tramites/**`) y `disenyo-grafico/documentos/`.

Hay **dos tipos de documento**, cada uno con su elemento raíz y su XSD:

| Tipo | Raíz | XSD | Qué dibuja | Formato |
|---|---|---|---|---|
| **FORMULARIO** | `<documentoFormulario>` | `documentoFormulario.xsd` | Una tabla sobre una rejilla de 12 columnas: secciones, filas, campos, casillas y textos. **Bilingüe**: estampa siempre el castellano y el valenciano | §2 de este fichero |
| **TEXTO** | `<documentoTexto>` | `documentoTexto.xsd` | Un documento en prosa: párrafos, listas, tablas sin bordes, con una cabecera de logo y título en su primera página. **Un solo idioma**, el que se pida | `documentotexto.md` |

**Común a los dos tipos** (y documentado aquí): los fragmentos `_*.xml` y el `<include>` (§2.5), el `<valenciano>` que se traduce del `<castellano>` (§2.6), las expresiones Groovy —`${expresion}` inline y el test P1— (§2.8), la visibilidad condicional `visible`/`siOculto` (§2.9) y el hueco de la firma `campoFirma` (§2.10).

---

## 1. Conceptos clave

- El tipo de documento lo decide su **elemento raíz**, y cada raíz se valida contra **su** XSD. Todo XML de definición **MUST** referenciar el suyo en la raíz con `xsi:noNamespaceSchemaLocation` usando su URL de GitHub en la rama master (la del ejemplo de §2.1, con el nombre del XSD que toque). El build valida cada XML contra ese XSD y aborta si no valida.
- **CRITICAL**: cada XSD declara **su propio** `<fragmento>`, con el contenido de su tipo. Incluir en un documento un fragmento **del otro tipo** no valida y rompe el build (§2.5).
- **FORMULARIO** — el documento renderizado es **una única tabla** sobre una rejilla lógica de **12 columnas** (19 cm: página A4 con márgenes de 1 cm), con la cabecera corporativa (logo GVA + título bilingüe) y secciones con letra automática (A, B, C…) en celda gris.
- Los idiomas van **siempre** en los elementos hijos `<valenciano>` y `<castellano>` del elemento, nunca como atributos, en los **dos** tipos de documento. Un FORMULARIO estampa los dos (el castellano en cursiva) e **ignora** el idioma que se le pida; un TEXTO estampa **solo** el idioma pedido (§1.1).
- Los **dos** tipos llevan `<titulo>`, pero con reglas distintas: en un FORMULARIO es **opcional** y, si falta, el build lo rellena con el `<name>` del `TramiteInstance.xml` del trámite padre (§2.7); en un TEXTO es **obligatorio** y no se inyecta nada, así que el documento sin él no valida (`documentotexto.md` §1).
- El **`<valenciano>` es opcional**: si se omite, el generador lo calcula **traduciendo el `<castellano>`** con el traductor `apertium` (§2.6). Un `<castellano>` omitido o vacío omite el castellano; un `<valenciano>` **vacío** (`<valenciano></valenciano>`) omite el valenciano — omitirlo y ponerlo vacío **no** es lo mismo.
- En un FORMULARIO, cada `<campo>`/`<check>` estampa **un valor**: el de su `nombreCampo`. **CRITICAL**: `nombreCampo` no es realmente un nombre — es una **expresión Groovy** que se evalúa en runtime para obtener el valor del campo (o el `Boolean` que marca la casilla), donde `self` es **el objeto del tipo de expediente** (la instancia de la entidad del expediente concreto en cuya carpeta está el XML). Todo el detalle del contexto, la potencia de las expresiones y la conversión de valores: §2.8.
- Casi cualquier elemento puede llevar `visible="<expresión Groovy>"`: si evalúa a `false` el elemento no se dibuja, y `siOculto` dice si desaparece (`colapsar`, por omisión: lo de detrás sube) o deja su hueco en blanco (`reservar`). Es **la** forma de hacer alternativas excluyentes (p.ej. una línea distinta por cada valor de un enum): §2.9.
- Carpeta `documentospdf/`: cada documento del trámite está **o** como XML de definición **o** directamente como PDF versionado. La disyuntiva es **por documento, no por carpeta**: es lícito y normal que en la misma carpeta convivan el XML de un documento con el PDF de otro. Si el trámite tiene **impreso oficial** (de la administración), lo habitual es **redefinirlo por XML** y guardar el PDF original, solo como referencia, en la subcarpeta `documentospdf/originales/`: el build solo mira los ficheros que cuelgan directamente de `documentospdf/`, así que lo que hay en una subcarpeta no produce constante en el enum ni interviene en nada. Dejar el PDF del impreso directamente en `documentospdf/` es la alternativa cuando debe usarse tal cual (se rellena y se aplana). Los `_*.xml` son **fragmentos** reutilizables (raíz `<fragmento>`) que los documentos incluyen con `<include href="..."/>` (§2.5) y no generan PDF propio. **MUST NOT** convivir en la misma carpeta un `aa.xml` (con raíz de documento, de cualquiera de los dos tipos) con un `aa.pdf` versionado: el build aborta por ambigüedad.
- Cada documento (XML o PDF versionado) produce una constante del enum `TipoDocumentoPdf` de la entidad (`modelo.md` §1) con la que el PhaseEventManager lo obtiene ya con los datos (`phaseeventmanager.md` §6.1). Un XML se dibuja; un PDF versionado se rellena (sus campos de formulario son expresiones, §2.8) y se aplana. Nombres de fichero en camelCase sin espacios ni guiones y extensión en minúsculas (`solicitudFirmada.xml` → `SOLICITUD_FIRMADA`; un nombre inválido rompe la compilación después, sin aviso del build).
- **CRITICAL — la carpeta MUST llamarse `documentospdf`**. Un documento puesto en otra carpeta (p. ej. `documentos/`) **no** tiene constante en el enum `TipoDocumentoPdf`, así que no hay forma de pedirlo: queda muerto, sin ningún aviso del build.
- Al enum de **cada** tipo se añaden, además de los suyos, los documentos de la carpeta compartida `tramites/shared/documentospdf/`. Hoy esa carpeta no existe, así que no hay ningún efecto visible, pero el mecanismo está activo: un documento puesto ahí aparecería en el `TipoDocumentoPdf` de **todos** los tipos de expediente.

### 1.1 El idioma con el que se pide el documento

- Un **FORMULARIO lo ignora**: es bilingüe y estampa siempre los dos idiomas, se le pida el que se le pida. Un **TEXTO emite solo el idioma pedido**.
- El idioma es el del **expediente** (`Expediente.idioma`: `"es"` → `CASTELLANO`, `"ca"` → `VALENCIANO`; desconocido o nulo → `CASTELLANO`), **no** el del usuario que dispara el evento: un TEXTO que genera la secretaría sale en el idioma de quien presentó el expediente, no en el de quien lo tramita. Lo resuelve `ExpedienteDocumentoPdfUtil` al leer `getIdioma()` del expediente; el resguardo del registro de entrada lo recibe igual, por el `idioma` del `RegistroEntradaInsertDTO` que `EventContext.createRegistroEntrada` rellena del expediente.
- El idioma del expediente lo fija el motor al crearlo (`Tramitador.updateIdioma`): el del usuario que lo presenta por «Mis trámites», o el que indica quien lo registra cuando se presenta en papel. **MUST NOT** leer `SecurityUtil.getUser().getLanguage()` para elegir el idioma de un documento.
- **MUST NOT** pasarle el idioma como dato al documento ni escribir el mismo documento dos veces, uno por idioma: los dos idiomas de un TEXTO viven en el mismo XML, en los hijos `<valenciano>`/`<castellano>` de cada elemento, y el generador elige.

---

## 2. Formato del XML

§2.1–§2.4 y §2.7 describen el documento **FORMULARIO**; el **TEXTO** está en `documentotexto.md`. §2.5, §2.6, §2.8 y §2.9 valen para los **dos** tipos.

### 2.1 Estructura

```xml
<?xml version="1.0" encoding="UTF-8"?>
<documentoFormulario xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/educaflow/EducaFlowBuildTools/master/src/main/resources/com/educaflow/common/buildtools/xml2pdf/documentoFormulario.xsd">
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
</documentoFormulario>
```

(La URL del `xsi:noNamespaceSchemaLocation` es siempre esa para un FORMULARIO, esté donde esté el fichero; un TEXTO referencia igual su `documentoTexto.xsd`.)

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
| `<seccion>` | Fila con letra gris automática (A, B, C…) + título | `visible` (opc.), `siOculto` (opc.) | `<valenciano>` (opc.), `<castellano>` (opc.), `<fila>` |
| `<fila>` | Una o varias líneas de la tabla | `visible` (opc.), `siOculto` (opc.) | `campo`/`check`/`texto` |
| `<campo>` | Etiqueta bilingüe en mayúsculas + el valor de `nombreCampo` debajo | `nombreCampo`, `colspan`, `rowSpan` (opc.), `visible` (opc.), `siOculto` (opc.) | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<check>` | Casilla (ocupa 1 columna), marcada según el `Boolean` de `nombreCampo`, + etiqueta bilingüe al lado | `nombreCampo`, `colspan`, `rowSpan` (opc.), `visible` (opc.), `siOculto` (opc.) | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<texto>` | Párrafos bilingües sin valor propio | `colspan`, `rowSpan` (opc.), `campoFirma` (opc., §2.10), `visible` (opc.), `siOculto` (opc.) | `<valenciano>` (opc.), `<castellano>` (opc.) |
| `<include>` | Nada por sí mismo: se sustituye por los hijos de la raíz del fragmento `href` (§2.5) | `href` | — |

### 2.3 Reglas

- **MUST**: como máximo un `<titulo>` y, si está, es lo primero del documento (o del fragmento). Aplica también tras expandir los includes: si un fragmento aporta el título, el documento que lo incluye **MUST NOT** declarar otro.
- El `<titulo>` de un FORMULARIO es **opcional**: si el documento no lleva ninguno (ni propio ni de un fragmento), el build le pone al principio del todo el nombre del trámite (§2.7). El de un TEXTO es obligatorio (`documentotexto.md` §1).
- **MUST**: los `colspan` de una `<fila>` suman **12 o un múltiplo de 12**. Cada grupo que suma 12 es una línea; las líneas siguientes se apilan debajo **dentro del mismo rectángulo** (sin borde entre ellas). Un elemento **MUST NOT** cruzar el límite de 12. El build lo comprueba sobre el XML **completo**, ignorando `visible`: lo que colapse en runtime puede dejar una línea corta, y eso es lícito (§2.9).
- `colspan` admite decimales (`colspan="3.5"`).
- `rowSpan` (≥1, admite decimales) hace más alta la fila y el hueco del valor de un `<campo>`. Es un **mínimo**: si el valor necesita más líneas, el hueco (y la fila) crecen solos. Úsalo para el recuadro de firma y para dar aire a los campos largos.
- **Valores inline**: dentro de los hijos `<valenciano>`/`<castellano>` de cualquier elemento, `${expresion}` estampa el valor de la expresión **en el propio texto**, como una palabra más: ocupa lo que mide y salta de línea con el texto. Un valor vacío **no ocupa nada**: el PDF sale plano, sin campos de formulario, así que no hay ningún hueco que reservar. Si aparece en ambos idiomas se estampa en los dos.
- `nombreCampo` con comillas dobles (expresiones como `"    " + f(x)`) → escribe el atributo XML con comillas simples: `nombreCampo='"    " + com...sha256(self.justificante)'`.
- Un `<campo>` sin ninguno de los dos hijos (o con los dos vacíos) es un campo **sin etiqueta** (útil para apilar varios campos en un rectángulo).
- El `<valenciano>` **omitido** se traduce del `<castellano>`; para un texto **solo en castellano** hay que poner el `<valenciano>` **vacío** (§2.6).
- **MUST NOT** poner `valenciano`/`castellano` como atributos de ningún elemento (formato antiguo): el XSD no valida y el generador aborta.

### 2.4 Ejemplos ✅/❌

- ✅ CORRECTO: `<fila>` con `colspan` 5 + 5 + 2 (suma 12).
- ✅ CORRECTO: `<fila>` con cuatro `<texto colspan="12">` (suma 48 = 4 líneas apiladas en un rectángulo).
- ✅ CORRECTO: `<fila>` con 6 + 2.1 + 3.9 (decimales, suma 12).
- ✅ CORRECTO: `<valenciano>Periodo parcial. De ${self.horaInicio} hores a ${self.horaFin} hores</valenciano>`.
- ✅ CORRECTO: `<campo nombreCampo="self.x" colspan="4"><castellano>Nombre</castellano></campo>` (sin `<valenciano>`: se traduce a "Nom" en el build).
- ✅ CORRECTO: `<valenciano></valenciano>` + `<castellano>...</castellano>` (etiqueta **solo** en castellano, sin traducir).
- ❌ INCORRECTO: `<fila>` con 5 + 3 + 2 (suma 10; el generador aborta con ERROR).
- ❌ INCORRECTO: `<fila>` con 8 + 8 (el segundo elemento cruza el límite de 12).
- ✅ CORRECTO: `${self.hora}` (la sintaxis es `${expresion}`, sin nada más).
- ❌ INCORRECTO: añadir un ancho al inline, `${…;n}` (formato antiguo: el `;n` reservaba el hueco de un campo de formulario y ya no existe. Todo lo que va entre `${` y `}` es **la expresión Groovy**, así que el `;n` entra en ella y lo que se estampa no es el valor del campo).
- ❌ INCORRECTO: `<campo nombreCampo="self.x" valenciano="Nom" castellano="Nombre" colspan="4"/>` (formato antiguo: los idiomas van siempre como elementos hijos, también en `titulo`/`seccion`).
- ✅ CORRECTO: `<check nombreCampo="true" colspan="12" visible="self.tipoPeriodo==com...TipoPeriodoMiTramiteV1.PERIODO_COMPLETO">` (una línea por opción del enum; solo sale la elegida, §2.9).
- ❌ INCORRECTO: `<check nombreCampo="self.tipoPeriodo==...PERIODO_COMPLETO" colspan="12"><castellano>Del ${self.tipoPeriodo==...PERIODO_COMPLETO ? self.fechaInicio : null}</castellano></check>` repetido por cada opción (salen todas las líneas con una sola casilla marcada, y la condición se repite en cada inline: es lo que resuelve `visible`).

### 2.5 Fragmentos reutilizables (`_*.xml` + `<include>`)

Para compartir partes comunes entre documentos (del mismo trámite o de varios).

**Mecánica**:

- Un **fragmento** es un fichero cuyo nombre **MUST** empezar por `_`, con raíz `<fragmento>`, la referencia al XSD **de su tipo** y el mismo contenido posible que la raíz de ese tipo. En un FORMULARIO: `titulo`, `seccion` e `include`.
- **CRITICAL**: un fragmento es **de un tipo de documento**, porque cada XSD declara su propio `<fragmento>` con el contenido de su tipo. Un documento **MUST NOT** incluir un fragmento del otro tipo: no valida y el build aborta.
- `<include href="..."/>` va **solo como hijo directo** de la raíz del documento o de un `<fragmento>`, en cualquier posición y cuantas veces haga falta. **MUST NOT** ir dentro de una `<seccion>`: no se incluyen trozos de sección.
- El generador sustituye cada `<include>` por **los hijos de la raíz** del fragmento, recursivamente (un fragmento puede incluir otros fragmentos). El `href` se resuelve relativo al fichero que lo incluye. Un ciclo de includes aborta con ERROR.
- Se valida contra el XSD cada fichero por separado **y** el documento ya expandido. Las letras de sección (A, B, C…) se asignan sobre el documento expandido.
- Cambiar un fragmento vuelve a resolver en el build todos los documentos que lo incluyen, directa o transitivamente.
- **CRITICAL para el versionado**: si un fragmento contiene expresiones Groovy con FQCN de enums versionados (`...TipoPeriodoMiTramiteV1.PERIODO_COMPLETO`), esas referencias cambian en cada versión nueva (`recetas/versionado.md`).

**Cuándo MUST extraerse** (regla de autoría: el build **no** la comprueba):

- **MUST**: toda `<seccion>` **literalmente idéntica** en dos o más documentos va a un fragmento que ambos incluyen. El umbral es **una sección y dos documentos** —la `<seccion>` es la unidad mínima que un `<include>` puede aportar (§2.2), así que por debajo la regla sería inaplicable—, y es **MUST** y no SHOULD porque el criterio es mecánico (idéntica o no) y el incumplimiento es silencioso: el texto se actualiza en un documento, el duplicado se queda viejo y nada avisa.
- **MUST** extraer **al escribir los documentos**, no después: un tipo de expediente con varios documentos (p.ej. solicitud + resolución) se diseña ya con sus fragmentos.
- **Literalmente idéntica** = mismo texto en **ambos** idiomas, mismos `nombreCampo`, mismos `colspan`/`rowSpan`.
- **MUST**: **un fragmento por cada bloque común**, nunca uno solo con todos dentro. Dos secciones comunes que en los documentos no van seguidas, o no van en el mismo orden, son **dos** fragmentos: cada documento coloca sus `<include>` donde le toca, y ese orden es el que fija las letras A, B, C… de sus secciones.
- **MUST NOT** unificar un bloque que **no** es literalmente idéntico cambiando el contenido de uno de los documentos: `<include>` no admite parámetros, así que eso no es factorizar sino **cambiar el PDF** — decisión funcional, no de autoría. Si el bloque se parece pero difiere (otra fecha, otros `colspan`, un campo de más), cada documento se queda con el suyo.
- Que **no** haya nada literalmente idéntico es un resultado válido: **MUST NOT** inventar una factorización artificial para que haya fragmentos.

**Nombres** — el fragmento se llama por **lo que contiene**, en camelCase tras el `_`:

- ✅ CORRECTO: `_datosAlumno.xml`, `_proteccionDatos.xml`, `_pieFirma.xml`.
- ❌ INCORRECTO: `_template1.xml`, `_fragmento2.xml`, `_comun.xml` (un correlativo o un genérico no dice qué hay dentro: obliga a abrir el fichero para entender el `<include>` que lo trae).

```xml
<documentoFormulario ...>
    <include href="_cabecera.xml"/>
    <seccion>... lo específico de este documento ...</seccion>
    <include href="../../otra_carpeta/_proteccionDatos.xml"/>
</documentoFormulario>
```

- ✅ CORRECTO: `<include href="_datosAlumno.xml"/>` como hijo de `<documentoFormulario>`, antes o después de cualquier `<seccion>`.
- ✅ CORRECTO: un fragmento con `<titulo>` (el documento que lo incluye ya no declara otro).
- ✅ CORRECTO: dos documentos que comparten dos secciones no contiguas → dos fragmentos, cada uno incluido donde le corresponde en cada documento.
- ❌ INCORRECTO: `<include>` dentro de `<seccion>` (los includes solo van al nivel documento/fragmento; el XSD no valida).
- ❌ INCORRECTO: `href="cabecera.xml"` (el nombre de fichero del fragmento debe empezar por `_`; el XSD no valida).
- ❌ INCORRECTO: un `_comun.xml` con las tres secciones compartidas dentro (cajón de sastre: obliga a los dos documentos a llevarlas seguidas y en el mismo orden).

### 2.6 El `<valenciano>` que falta se traduce del `<castellano>`

Si un elemento no lleva `<valenciano>`, el build lo calcula traduciendo su `<castellano>` con el proceso traductor externo `apertium` (el mismo que usa la i18n de la aplicación). La traducción ocurre **en el build**, después de expandir los `<include>` (también se traducen los textos de los fragmentos); el XML versionado **no** se modifica: el `<valenciano>` traducido solo existe en el XML resuelto que el build deja en `build/src-gen/main/resources` (y del que la aplicación dibuja el PDF).

- Para un texto **solo en castellano** hay que poner el `<valenciano>` **vacío**: `<valenciano></valenciano>`. Omitirlo significa "tradúcelo".
- **No se traducen** los inline `${expresion}` ni las URL: el build los protege y los restaura tal cual.
- Para que **no se traduzca** ninguna otra cosa (siglas, nombres propios, marcas…) se le pega el sufijo `__!!` en el `<castellano>`: `(RATs__!!)`. El sufijo **no se dibuja** en el PDF, solo evita la traducción de esa palabra.
- Si el traductor no sabe traducir alguna palabra, **el build falla** con el texto y el fichero. Se arregla de una de estas dos formas: escribir el `<valenciano>` a mano, o marcar la palabra con `__!!`.
- La traducción automática es una comodidad para textos nuevos y sencillos (etiquetas, títulos). Para el texto legal largo, revísala: apertium acierta la gramática pero no el registro administrativo.

### 2.7 El `<titulo>` que falta sale del trámite

Si un **FORMULARIO** no lleva `<titulo>` (ni propio ni aportado por un fragmento), el build le añade uno **al principio del todo** con el `<name>` del `TramiteInstance.xml` del **trámite padre**, y lo traduce al valenciano (§2.6). O sea: por omisión, un formulario se titula como su trámite.

**Esto es solo del FORMULARIO**: en un TEXTO el `<titulo>` es obligatorio y el build no inyecta nada.

- El `TramiteInstance.xml` se busca **subiendo por las carpetas padre** desde la del XML: los documentos están en `<tramite>/<vN>/documentospdf/` y el `TramiteInstance.xml` en `<tramite>/`. **MUST** existir y tener `<name>`, o el build falla.
- Cambiar el `<name>` del `TramiteInstance.xml` vuelve a resolver en el build los documentos de ese trámite.
- Pon un `<titulo>` explícito solo cuando el documento **deba** titularse distinto del trámite.

```xml
<documentoFormulario ...>
    <!-- sin <titulo>: se titula con el <name> del TramiteInstance.xml del trámite -->
    <seccion>...</seccion>
</documentoFormulario>
```

### 2.8 Las expresiones Groovy (`nombreCampo` y `${...}`)

**Cuándo se evalúan**: NO en el build — el build solo resuelve el XML. Las expresiones se evalúan **en runtime**, cada vez que el PhaseEventManager pide el documento (`expediente.getDocumentoPdf(...)`), y el PDF se dibuja ya con los valores: sale **plano**, sin formulario. Primero se evalúan los `visible` **por niveles** (en un FORMULARIO: secciones, luego las filas de las secciones que se ven, luego sus celdas) y después las expresiones de valor **solo de lo que se ve**: el `nombreCampo` o el inline de un elemento oculto no se evalúa nunca (§2.9).

En un documento TEXTO no hay `nombreCampo`: sus únicas expresiones de valor son los `${expresion}` inline de los `<castellano>`/`<valenciano>`, y solo se evalúan las del **idioma que se emite**. Todo lo demás de esta sección vale igual para los dos tipos.

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
- Comparaciones para los `<check>` y los `visible`: `self.tipoPeriodo==com.educaflow.subsystem.expedientes.db.TipoPeriodo<Entidad>.PERIODO_COMPLETO`, o el literal `true` (casilla siempre marcada). Un `<check>` **MUST** devolver `Boolean`; un `visible` también.
- Concatenación: `'"    " + com...sha256(self.justificante)'` (atributo con comillas simples, §2.3).

**Conversión del resultado a texto** (lo que se estampa en el campo o en el inline):

| Resultado | Se estampa |
|---|---|
| `Boolean` | En un `<check>`, marca o desmarca la casilla; en un `<campo>` o un inline sale "Sí"/"No" |
| `null` | vacío (en un inline **no ocupa nada**) |
| Enteros / decimales | formateados con locale español (decimales: máximo 2) |
| `LocalDate` / `LocalTime` / `LocalDateTime` | `dd/MM/yyyy` / `HH:mm` / `dd/MM/yyyy HH:mm` |
| Enum de Axelor | su `title` del `<item>` del dominio (o el `name` humanizado si no tiene `title`); también vale acceder explícitamente: `self.tipoResolucion.title` |
| Resto | `toString()` |

**CRITICAL — los fallos en runtime abortan el evento**: una expresión que revienta al evaluarse (una relación a `null` en mitad de la cadena, un método que no existe…), un `visible` o un `<check>` que no devuelven `Boolean`, hacen que `PdfGenerator.generate` lance `RuntimeException` y el evento **no** termine: no se emite un documento con un dato de menos. Es el comportamiento elegido a propósito, así que **MUST NOT** taparse con `?.` ni con `?: ""` por sistema; si un dato puede faltar legítimamente (un adjunto opcional, una relación que solo existe en algunos casos), lo que sobra en el documento se quita con `visible` (§2.9). Solo los PDF versionados que se rellenan (`DocumentoPdfUtil.generate`) conservan el comportamiento antiguo: log y campo vacío.

**Cómo se testean** (`src/test/java/com/educaflow/tiposexpedientes/documentos/ExpresionesDeDocumentoTest.java`, regla **P1**; se escribe a mano, `SKILL.md` §3.3):

- El test recorre todos los tipos de expediente y lee cada documento de `documentospdf/` **del classpath**: el XML resuelto que dejó el build, con el mismo parser que el runtime (`PdfGenerator.getExpresiones`: cada `nombreCampo`, cada inline de los dos idiomas y cada `visible`, con los fragmentos ya expandidos), o el PDF versionado con `DocumentoPdfFactory` (sus nombres de campo). Es exactamente lo que el runtime va a evaluar.
- Cada expresión se **compila** (no se evalúa) envuelta en un método `@groovy.transform.TypeChecked` con parámetros `<Entidad> self` y `java.time.LocalDateTime now`, donde `<Entidad>` es la primera `<entity>` del `domains.xml`. El comprobador estático de Groovy rechaza la propiedad que no existe en la entidad ni en la relación navegada, el FQCN que no resuelve (el enum `...V1` que se queda en el XML al duplicar la versión) y el error de sintaxis. El mensaje trae el tipo, el fichero, la expresión y el error del compilador.
- Corre con `./gradlew test` y por tanto con `./run.sh`: un fallo de P1 rompe el build. Para lanzarlo solo: `./gradlew test --tests 'com.educaflow.tiposexpedientes.documentos.*'`.
- **Lo que P1 NO ve** es lo que depende de los datos: una relación a `null` en mitad de una cadena (NPE), o un patrón de `DateTimeFormatter` inválido. Eso sigue fallando solo en runtime, y ahí aborta el evento: tras cambiar expresiones de ese tipo, **MUST** probar el evento que genera el documento con datos reales (con y sin los datos opcionales).
- ✅ CORRECTO: `self.tipoResolucion.title` (`ValueEnum` de Axelor aporta `getTitle()` como método `default`, así que compila y devuelve el `title` del `<item>`).
- ❌ INCORRECTO: `self.personaInteresada.nombreCompleto` si `Persona` no tiene ese campo (P1 falla: `No such property: nombreCompleto for class: ...Persona`).
- ❌ INCORRECTO: `...db.SentidoRevisionMiTramiteV1.RECHAZAR` en el XML de la `v2` (P1 falla: `The variable [com] is undeclared` — el enum de la `v2` es `...V2`).

### 2.9 Visibilidad condicional (`visible` y `siOculto`)

En un FORMULARIO lo admiten `campo`, `check`, `texto`, `fila` y `seccion`; en un TEXTO, **todos** sus elementos (`documentotexto.md`). En los dos tipos significa lo mismo:

| Atributo | Valor | Efecto |
|---|---|---|
| `visible` | expresión Groovy (mismo contexto que `nombreCampo`: `self`, `now`) que **MUST** devolver `Boolean` | `false` → el elemento **no se dibuja**. Omitido → siempre se dibuja |
| `siOculto` | `colapsar` (por omisión) o `reservar` | Qué se hace con el sitio del elemento oculto. **Solo** junto a `visible`: sin él, el build aborta |

- **`colapsar`**: el elemento **desaparece** como si no estuviera en el XML. Lo que va detrás en su línea se desplaza a la izquierda, lo que va debajo sube, una `fila` colapsada no ocupa nada y una `seccion` colapsada **no consume letra** (la siguiente hereda la suya).
- **`reservar`**: el elemento **conserva su sitio y su alto** y no se dibuja su contenido; en un FORMULARIO se dibujan solo sus bordes: ni textos, ni valor, ni casilla, ni letra ni gris de sección. La rejilla queda intacta y el hueco en blanco. Una `seccion` reservada **sí consume letra**.
- Lo oculto no se evalúa: ni el `nombreCampo` ni los inline de una celda oculta, ni los `visible` de los hijos de algo oculto. Por eso `visible` es también la forma de **proteger** una expresión que solo tiene sentido cuando un dato existe (`visible="self.justificante != null"` en la fila que enseña `self.justificante.fileName`).
- Tras colapsar, la última línea de una `fila` puede quedar corta: se dibuja igual y el tramo que falta se cierra con un recuadro vacío. La regla del múltiplo de 12 (§2.3) se comprueba en el build sobre el XML completo, no sobre lo que se ve.
- Una `fila` que se queda sin celdas desaparece; una `seccion` visible sin filas se queda solo con su cabecera.

```xml
<fila>
    <texto colspan="12"><castellano>No ha podido impartir las siguientes horas</castellano></texto>
    <check nombreCampo="true" colspan="12" visible="self.tipoJornada==com...TipoJornadaMiTramiteV1.UN_DIA">
        <castellano>El día ${self.fechaInicio}</castellano>
    </check>
    <check nombreCampo="true" colspan="12" visible="self.tipoJornada==com...TipoJornadaMiTramiteV1.VARIOS_DIAS">
        <castellano>Desde el ${self.fechaInicio} hasta el ${self.fechaFin}</castellano>
    </check>
</fila>
<seccion visible="self.justificante != null">
    <castellano>Documentación aportada</castellano>
    <fila><campo nombreCampo="self.justificante.fileName" colspan="12"/></fila>
</seccion>
```

- ✅ CORRECTO: alternativas excluyentes = un elemento por alternativa, cada uno con su `visible`; `nombreCampo="true"` en el check porque, si se ve, es que aplica.
- ✅ CORRECTO: `visible="self.presentadoEnRepresentacion" siOculto="reservar"` en la fila del representante de un impreso que debe conservar su maquetación aunque no haya representante.
- ❌ INCORRECTO: `visible="self.tipoJornada"` (no es `Boolean`: el evento aborta).
- ❌ INCORRECTO: `siOculto="reservar"` sin `visible` (no significa nada: el build aborta).
- ❌ INCORRECTO: repetir la condición del `visible` dentro de los inline (`${cond ? self.fechaInicio : null}`): si el elemento se ve es que la condición ya se cumple.
### 2.10 El hueco de la firma (`campoFirma`)

`campoFirma="<nombre>"` marca el elemento que es el hueco de una firma. El generador deja ahí, en el PDF, un **campo de firma vacío** con ese nombre, y quien firma indica el nombre en vez de unas coordenadas (`recetas/firma.md`): la firma cae en su hueco aunque el contenido de delante lo desplace o lo mande a otra página.

| Tipo | Elemento que lo admite | Dónde queda el campo de firma |
|---|---|---|
| FORMULARIO | `<texto>` | En el hueco de la celda **por debajo de sus textos**, a todo su ancho |
| TEXTO | `<espacio>` | En todo el `<espacio>`: su `alto` y el ancho que le toque (el del cuerpo, o el de su columna si es celda de una `<tabla>`) |

- El nombre es un identificador (letra inicial y luego letras sin acento, dígitos o `_`) y **MUST NOT** repetirse en el documento ya expandido: el build aborta en los dos casos.
- Nómbralo por **quién firma**: `firmaSolicitante`, `firmaDirector`.
- En un FORMULARIO el `<texto>` **MUST** llevar `rowSpan` suficiente (el de un recuadro de firma es `4`): si bajo sus textos no queda hueco, generar el documento aborta.
- Un elemento oculto (colapsado **o** reservado) **no** deja campo, y firmar en un campo que no existe aborta el evento: **MUST NOT** poner `visible` al hueco de una firma que se firma siempre.
- Marca **solo** los huecos que alguien firma de verdad: un campo que se queda vacío aparece en el PDF como una firma pendiente.
- **MUST NOT** usar `siOculto="reservar"` para que el hueco de la firma no se mueva: con `campoFirma` la firma sigue a su hueco. `reservar` es solo para conservar la maquetación de un impreso.
- El campo sobrevive a `anyadirDocumentoPdf` (anexar un justificante detrás no lo pierde).
- Un PDF versionado no pasa por aquí: dónde se firma lo dice `recetas/firma.md` §4.
- **MUST NOT** poner junto a la firma, en **ningún** documento (FORMULARIO o TEXTO), ni el municipio ni la fecha («Municipio, 4 de octubre de 2026»): ni con un campo de la entidad ni con `now`, ni como texto ni como título de sección («Lugar, fecha y firma»).
  - Es un resto del documento en papel, donde se fechaba y firmaba a la vez. Aquí lo que importa es la fecha de la firma digital, que ya va dentro de la propia firma.
  - El documento se genera en un evento y se firma cuando quiere el firmante: una fecha escrita en el documento no coincidiría con la de la firma.
  - Junto al hueco de la firma solo va la antefirma con el cargo de quien firma, si lo tiene.

- ✅ CORRECTO: `<texto colspan="12" rowSpan="4" campoFirma="firmaSolicitante"><castellano>Firma:</castellano></texto>`
- ✅ CORRECTO: en un TEXTO, `<fila><espacio alto="88.56"/><espacio alto="88.56" campoFirma="firmaDirector"/></fila>` bajo la fila de los dos cargos (solo firma el director: solo su hueco lleva campo).
- ❌ INCORRECTO: `<texto colspan="12" campoFirma="firmaSolicitante">` sin `rowSpan` (no queda hueco bajo el texto: la generación aborta).
- ❌ INCORRECTO: `<campo nombreCampo="self.x" colspan="12" campoFirma="firmaSolicitante">` (en un FORMULARIO solo lo admite `<texto>`: no valida).
- ❌ INCORRECTO: `campoFirma="firma del director"` (no es un identificador: no valida).
- ❌ INCORRECTO: `campoFirma="firma"` en la solicitud y en un fragmento que esta incluye (nombre repetido tras expandir: el build aborta).
- ✅ CORRECTO: en un FORMULARIO, `<seccion><castellano>Firma</castellano><fila><texto colspan="12" rowSpan="4" campoFirma="firmaSolicitante">…</texto></fila></seccion>` (solo el recuadro de la firma).
- ❌ INCORRECTO: `<parrafo alineamiento="derecha"><castellano>${self.centro.municipio.name}, ${now}</castellano></parrafo>` encima de los huecos de firma (municipio y fecha junto a la firma: la fecha que cuenta es la de la firma digital).
- ❌ INCORRECTO: una `<seccion>` «Lugar, fecha y firma» con un `<texto>` `${self.personaInteresada.municipio.name}, ${self.fechaSolicitud}` junto al recuadro de firma (la solicitud se genera en un evento y se firma en otro: la fecha no es la de la firma).

---

## 3. Anti-patrones

- **MUST NOT** poner el municipio ni la fecha junto a la firma en ningún documento: la fecha que cuenta es la de la firma digital (§2.10).
- **MUST NOT** buscar un PDF generado que "arreglar": no existe hasta que un expediente lo pide. Cambia el XML de definición; el build lo vuelve a resolver y la aplicación lo dibuja de nuevo en el siguiente evento.
- **MUST NOT** hacer alternativas excluyentes con varios `<check>` siempre visibles y ternarios en sus inline: salen todas las líneas con una sola casilla marcada. Cada alternativa lleva su `visible` (§2.9).
- **MUST NOT** tapar por sistema con `?.` o `?: ""` una relación que puede ser `null`: lo que no aplica se quita con `visible`; lo que debería existir y no existe **debe** hacer fallar el evento (§2.8).
- **MUST NOT** quitar un `<valenciano>` ya escrito para dejar que lo traduzca el build: la traducción automática (§2.6) es para textos **nuevos**, no para sustituir el valenciano oficial de un documento existente.
- **MUST NOT** copiar una `<seccion>` literalmente idéntica en dos documentos en vez de extraerla a un fragmento (§2.5): el duplicado se desincroniza en silencio en el primer cambio del texto.
- **MUST NOT** meter todos los bloques comunes en un único fragmento cajón de sastre, ni nombrar un fragmento con un correlativo (`_template1.xml`): un fragmento por bloque, nombrado por su contenido (§2.5).
- **MUST NOT** escribir el mismo documento dos veces, uno por idioma, ni meter el idioma en el contexto: el TEXTO lleva los dos idiomas en el mismo XML y el generador elige (§1.1).
- **MUST NOT** seguir usando la sintaxis `${…;n}`: el `;n` era el hueco de un campo de formulario y desapareció con él. Hoy todo lo que va entre `${` y `}` es la expresión Groovy (§2.3).

---

## Quick Guidelines

- Dos tipos de documento, por elemento raíz: `<documentoFormulario>` (rejilla de 12 columnas, bilingüe, §2) y `<documentoTexto>` (prosa, un solo idioma, `documentotexto.md`). Cada uno con su XSD, y cada XSD con su propio `<fragmento>`: un documento **MUST NOT** incluir un fragmento del otro tipo.
- El idioma es el del expediente (`Expediente.idioma`), nunca el del usuario que genera el documento: el FORMULARIO lo ignora, el TEXTO emite solo ese (§1.1).
- FORMULARIO: rejilla de 12 columnas; cada `<fila>` suma 12 o un múltiplo de 12 (múltiplo = líneas apiladas en el mismo rectángulo).
- Idiomas: siempre elementos hijos `<valenciano>`/`<castellano>`, en todos los elementos; nunca atributos.
- El `<titulo>` de un FORMULARIO es opcional: sin él, el documento se titula con el `<name>` del `TramiteInstance.xml` del trámite padre, traducido al valenciano (§2.7). El de un TEXTO es **obligatorio** y no se inyecta nada.
- El `<valenciano>` es opcional: **omitido** = lo traduce el build del `<castellano>`; **vacío** = solo castellano. Los `${...}` y las URL no se traducen; lo demás que no deba traducirse se marca con el sufijo `__!!`, que no se dibuja (§2.6).
- Todo XML referencia con `xsi:noNamespaceSchemaLocation` el XSD **de su tipo** (`documentoFormulario.xsd` o `documentoTexto.xsd`) de `EducaFlowBuildTools`, por su URL de GitHub en master; el build lo valida al resolverlo (y puedes adelantarte con `xmllint --schema` contra la copia local de `../EducaFlowBuildTools`).
- `colspan` y `rowSpan` admiten decimales; la casilla de un `<check>` ocupa siempre 1 columna. `rowSpan` y el hueco del valor son mínimos: un valor largo hace crecer la fila.
- `${expresion}` = valor inline dentro de cualquier texto bilingüe, en los dos tipos: ocupa lo que mide, y nada si está vacío. **MUST NOT** llevar `;n` (sintaxis antigua, §2.3).
- `nombreCampo` no es un nombre: es una **expresión Groovy** que obtiene el valor del campo, evaluada **en runtime** con `self` (el objeto del tipo de expediente) y `now`. El test P1 (`ExpresionesDeDocumentoTest`) compila cada expresión (también los `visible`) contra la entidad en `./gradlew test`; lo que depende de los datos (relación a `null` en la cadena, patrón de fecha) falla en runtime y **aborta el evento** (§2.8). Con comillas dobles dentro, atributo con comillas simples.
- `visible="<expresión Boolean>"` (en un FORMULARIO: `campo`/`check`/`texto`/`fila`/`seccion`; en un TEXTO: todos sus elementos) quita el elemento cuando no aplica; `siOculto="colapsar"` (por omisión, lo de detrás sube; una sección no consume letra) o `"reservar"` (queda el hueco con sus bordes). Lo oculto no se evalúa. Es la forma de hacer alternativas excluyentes y de no evaluar lo que depende de un dato opcional (§2.9).
- `campoFirma="<nombre>"` en el `<texto>` (FORMULARIO) o el `<espacio>` (TEXTO) que es el hueco de una firma: deja un campo de firma vacío en el que se firma por su nombre, sin coordenadas. Nombre único, por quién firma; nunca en un elemento con `visible` (§2.10).
- En `documentospdf/` cada documento está **o** como XML de definición **o** directamente como PDF versionado (nunca ambos para el mismo documento; mezclar XML de unos y PDF de otros en la carpeta es lo normal). Un **impreso oficial** se redefine por XML y su PDF original se guarda como referencia en `documentospdf/originales/`, que el build ignora (§1).
- Partes comunes: fragmentos `_*.xml` (raíz `<fragmento>`, del mismo tipo de documento) incluidos con `<include href="..."/>` solo a nivel de documento/fragmento, recursivos, validados también tras expandir (§2.5).
- **MUST** extraer a fragmento, **desde que se escriben los documentos**, toda `<seccion>` literalmente idéntica en dos o más de ellos: **un fragmento por bloque común** (no un cajón de sastre) y nombrado por su contenido (`_datosAlumno.xml`, nunca `_template1.xml`). Lo que no sea literalmente idéntico **MUST NOT** unificarse cambiando el contenido de un documento: eso cambia el PDF (§2.5).
