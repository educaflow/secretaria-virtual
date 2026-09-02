# Guía de los ficheros de la especificación de un trámite

Explica qué debe contener cada fichero de la especificación de un **trámite y su tipo de expediente**, cómo clasificar sus elementos y cómo numerarlos. Esta guía dirige la redacción y la revisión de la spec; **MUST NOT** copiarse ninguno de sus bloques explicativos al output.

Es **el único fichero de esta carpeta de plantillas que el skill `sdd-specification` conoce por nombre**: el skill lee este `README.md` y, a través de él, descubre y usa el resto. Por eso aquí se declara qué hay en la carpeta y cómo se usa cada cosa.

## 0. REGLA DE GENERALIDAD — léela antes que nada

**CRITICAL.** Esta guía y sus plantillas describen **el patrón**, nunca un trámite concreto.

- **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, acción, dato, perfil ni documento **reales**: se usan placeholders (`<FASE>`, `<ESTADO>`, `<ACCION>`, `<PERFIL>`, `<dato>`).
- **MUST NOT** escribirse ninguna regla que solo valga para un número fijo de cosas. El patrón **MUST** funcionar con **1, 2, 3 o N fases**; con **N estados** por fase; con **0, 1 o N documentos**; con firma **en el equipo del interesado, del centro, en ambas o en ninguna**; con **0, 1 o N registros** de entrada o de salida; con estados **con y sin** perfil; con acciones **con y sin** ramificación.
- Todo ejemplo va encerrado en un bloque que empieza por `> **Ejemplo** (ilustrativo, NO normativo):`.
- Lo confinado en `example/` está exento: es un trámite **inventado**, marcado como ejemplo, y **MUST NOT** tomarse por norma.

---

## 1. Ficheros de esta carpeta de plantillas

| Fichero | Qué es | Cómo se usa |
|---|---|---|
| `README.md` | **Esta guía**: el conjunto de ficheros, los apartados de cada uno, la clasificación de los elementos, las reglas de numeración y los barridos. | Es la única referencia que el skill conoce por nombre; dirige las preguntas de la Fase 2 y las validaciones de la Fase 3. **MUST NOT** copiarse al output. |
| `specification.md` | **La plantilla del índice.** | Se reproduce **literalmente**, sustituyendo los placeholders por contenido real. Produce **un** fichero índice (el único con frontmatter `type: specification`). |
| `estados.md` | **La plantilla del ciclo de vida.** | Se instancia **una sola vez** por spec, con el nombre `estados.md`. |
| `pantallas.md` | **La plantilla de las pantallas de una fase.** | Se instancia **una vez por cada fase**, con el nombre `pantallas-<fase>.md`. |
| `documentos.md` | **La plantilla de los documentos.** | Se instancia **una sola vez** por spec, con el nombre `documentos.md`, **solo si** el trámite genera al menos un documento. |
| `catalogos/` | **Carpeta de catálogos de referencia**, uno por barrido: `catalogo-cobertura-estados.md`, `catalogo-historias-escenarios.md`, `catalogo-pasos-escenario.md`, `catalogo-validaciones.md`, `catalogo-reglas-negocio.md`, `catalogo-datos-calculados.md` y `catalogo-reglas-ui.md`. | Se consultan al rellenar cada apartado y son la referencia de los **barridos de completitud** (§9). **MUST NOT** copiarse al output. |
| `example/` | **Carpeta con un ejemplo completo** de spec terminada e instanciada, de un trámite **inventado**. | Referencia del aspecto final. **MUST NOT** copiarse su contenido al output ni tomarse sus nombres, sus fases o su número de documentos por norma. |

## Exploración del contexto

**Esta sección la ejecuta el propio skill `sdd-specification` en su Fase 1** (no los subagentes), antes de preguntar o revisar:

1. **Lista los trámites reales** (no de memoria):
   ```bash
   ls src/main/java/com/educaflow/tramites/
   ```
   Si la iniciativa versiona o modifica un trámite existente, lee su `TramiteInstance.xml` y la carpeta de la versión afectada antes de preguntar.
2. **Lista los subsistemas** (`ls src/main/java/com/educaflow/subsystem/`) solo para conocer las dependencias funcionales que la spec pueda nombrar en lenguaje de negocio (registro de entrada/salida, notificaciones, firmas…). **MUST NOT** cargar skills técnicos (`k-tipo-expediente`, `k-tramite`, `k-vistas`…): lo que la spec necesita saber del patrón lo declara esta plantilla.
3. **Comprueba si la solicitud es divisible**: si mezcla más de un trámite, propón una spec por trámite.
4. **MUST NOT** tomar `subsystem/` ni `system/` como referencia arquitectónica — siguen otra arquitectura (otra plantilla); citarlos como dependencia de negocio sí se puede.

## 2. Ficheros que produce la especificación

La especificación **no es un único fichero**: es un conjunto de ficheros dentro de la carpeta de la iniciativa.

| Fichero | Plantilla | Cardinalidad | Qué contiene |
|---|---|---|---|
| `specification.md` | `specification.md` | **exactamente 1** | El **índice**: objetivo, el trámite, actores y perfiles, historias de usuario con sus escenarios, resumen de fases y estados, tablas de enlaces, registros y avisos, seguridad, datos iniciales y fuera de alcance. Es el único con frontmatter `type: specification`. |
| `estados.md` | `estados.md` | **exactamente 1** (en una **modificación**, §3.8: solo si cambia el ciclo de vida) | El **ciclo de vida completo**: qué pasa al crear el expediente, las fases con sus estados, y por cada estado sus acciones con sus comprobaciones (`VAL-`), sus efectos (`RN-`) y sus transiciones; la tabla de transiciones y los datos que rellena el sistema (`CC-`). |
| `pantallas-<fase>.md` | `pantallas.md` | **una por fase** (en una **modificación**, §3.8: solo las fases con alguna pantalla nueva o cambiada) | Las **pantallas** de los estados de esa fase: una por cada pareja (estado, perfil), más la de solo consulta para el resto de perfiles, con sus bloques, sus botones y sus reglas de pantalla (`RUI-`). |
| `documentos.md` | `documentos.md` | **0 o 1** (en una **modificación**, §3.8: solo si cambian los documentos) | Los **documentos** que el trámite genera: cuándo, quién los firma y dónde, si se registran y qué datos del expediente aparecen en ellos. **No se crea** si el trámite no genera ninguno. |

- `<fase>` es el nombre de la fase **en minúsculas**, con `_` sustituido por `-`. `specification.md`, `estados.md` y `documentos.md` son **nombres fijos**.
- Solo `specification.md` lleva frontmatter. Los demás empiezan directamente por su `# …`.
- **MUST NOT** producirse ningún otro fichero: ni diagramas, ni ficheros de modelo, ni de entidad, ni un fichero por estado o por documento.

> **Ejemplo** (ilustrativo, NO normativo): un trámite con las fases `PRESENTACION` y `RESOLUCION` que genera dos documentos produce cinco ficheros: `specification.md`, `estados.md`, `pantallas-presentacion.md`, `pantallas-resolucion.md` y `documentos.md`.

---

## 3. Reglas transversales

### 3.1 Vocabulario del trámite — lo que SÍ se nombra

**CRITICAL — los nombres propios del trámite son vocabulario de NEGOCIO, no tecnología.** Se usan sin complejos en toda la spec, y **MUST NOT** filtrarse por error junto con las prohibiciones de §3.2:

| Qué | Se nombra así | Por qué es negocio |
|---|---|---|
| Los **perfiles** | `CREADOR`, `RESPONSABLE`, `SECRETARIO`, `DIRECTOR`, `AUDITOR` | Son los papeles que juegan las personas en el trámite; el negocio decide quién ostenta cada uno. Es una lista cerrada: **MUST NOT** inventarse otros. |
| Las **fases** | `<FASE>` en MAYÚSCULAS con guiones bajos, más su **título**, que el usuario ve en la cabecera de todas las pantallas de sus estados | Una fase agrupa los estados de una etapa del trámite. Su título no es documentación: es texto que el usuario lee. |
| Los **estados** | `<ESTADO>` en MAYÚSCULAS con guiones bajos, más su **título** | Un estado es «en qué punto está el expediente», que es exactamente lo que el negocio decide. |
| Las **acciones** | `<ACCION>` en MAYÚSCULAS con guiones bajos, más el **texto del botón** que el usuario pulsa | Una acción es lo que una persona hace para que el expediente avance. |
| Los **datos** del expediente | por su nombre funcional, en minúsculas y en lenguaje llano | Es lo que el usuario rellena o lee. |

Un estado se identifica **siempre** por la pareja `<FASE> / <ESTADO>`: dos estados de fases distintas pueden llamarse igual. **MUST** escribirse siempre la fase junto al estado cuando haya ambigüedad.

> **Ejemplo** (ilustrativo, NO normativo): «En el estado `REVISION / PENDIENTE_INFORME`, el perfil `RESPONSABLE` puede lanzar la acción `INFORMAR` con el botón «Emitir el informe»». Los cuatro nombres son inventados para el ejemplo.

Dos avisos sobre los perfiles, porque se confunden a menudo:

- El perfil `SECRETARIO` (quién actúa en un estado) **no es lo mismo** que el cargo Secretario del centro (cuya firma sella un documento). Un trámite puede usar el perfil sin usar la firma, y al revés.
- Un perfil solo dice **quién tiene el turno**; no dice quién puede *ver* el expediente. Lo segundo va en el apartado «Seguridad» del índice.

### 3.2 Lenguaje de negocio y prohibiciones

**Regla de oro:** ¿lo entendería un supervisor del centro sin formación técnica? Si **no**, no va en la spec. La prohibición aplica a **todos** los ficheros: índice, `estados.md`, `pantallas-<fase>.md` y `documentos.md`.

**MUST NOT** aparecer en ningún fichero de la spec:

| Familia | ❌ Prohibido | ✅ En su lugar |
|---|---|---|
| Clases y componentes | `PhaseEventManagerImpl`, `StateEventValidatorImpl`, `InitialEventManagerImpl`, `Tramitador`, `ExpedienteController`, `FirmaController`, cualquier `*Service`/`*Impl` | «lo que hace el sistema al lanzar la acción», «las comprobaciones de la acción», «lo que se rellena al crear el expediente» |
| Ficheros del árbol | `TramiteInstance.xml`, `TipoExpedienteInstance.xml`, `domains.xml`, `views.xml`, `permisos-demo.xml`, `estados.puml`, `documentospdf/<algo>.xml` | «el trámite», «el ciclo de vida», «la pantalla», «los permisos», «el documento» |
| Atributos y etiquetas XML | `<state name=…>`, `events=""`, `initial="true"`, `closed="true"`, `profile="…"`, `<include-panels>`, `<footer>`, `showIf`, `hideIf`, `readonly`, `colSpan` | «el estado en el que nace el expediente», «el estado cierra el expediente», «quién tiene el turno», «los bloques que se ven», «solo consulta» |
| Campos internos | `codePhase`, `codeState`, `abierto`, `usuarioRegistrador`, `personaSolicitante`, `dniFirmaDocumentoEntrada`, `numeroExpediente`, `MetaFile`, o un nombre de campo tal cual (`importeSolicitado`, `pdfAutorizacion`) | «la fase y el estado en que está», «quién creó el expediente», «el documento de identidad con el que se firma», «el importe que se pide», «el documento de autorización» |
| Identificadores de código | `Profile.CREADOR`, `States.<Fase>.<ESTADO>`, `EXIT`, `DELETE`, `@WhenEvent`, `onEnter…`, `trigger…`, `getForState…` | `CREADOR`, `<FASE> / <ESTADO>`, «el botón «Salir»», «borrar el expediente» |
| Firma | `AutoFirma` como nombre de aplicación o de clase, `FirmaPdf`, `serial:`, `CampoFirma`, `Rectangulo` | «se firma en el equipo del interesado con su certificado digital», «lo firma el centro con la firma del Director», «el recuadro de firma va en la esquina inferior derecha de la última página» |
| Tipos y consultas | tipos Java, FQN `com.educaflow.*`, `many-to-one`, `one-to-many`, `enum`, JPQL, SQL, Groovy, `self.<algo>` | «un dato que apunta a otra ficha», «una lista de opciones», «los expedientes de su centro» |
| Códigos de configuración | el código de la categoría del trámite (`PROFESOR`, `ALUMNO`…), el código del tipo de expediente, el nombre de la carpeta de versión | «va dirigido al profesorado», «al alumnado», «la primera versión del trámite» |
| Taxonomías ajenas | cualquier prefijo de identificador distinto de los de §3.4, y en particular `RES-` | los prefijos de §3.4 |

- ✅ CORRECTO: *«Al presentar sin haber firmado el documento, el sistema muestra «Debe firmar la matrícula antes de presentarla».»*
- ❌ INCORRECTO: *«El validador de la fase de presentación exige `+Required()` y `+FirmaPdf(...)` sobre `pdfMatriculaFirmada`.»*
- ✅ CORRECTO: *«Al lanzar la acción, el sistema genera el documento de resolución, lo firma con la firma del Director del centro y deja constancia de su salida.»*
- ❌ INCORRECTO: *«El `trigger*` llama a `GENERAR_PDF`, `FIRMAR_SERVIDOR(cargo=DIRECTOR)` y `createRegistroSalida`.»*

**Excepción — esta plantilla NO usa `AllowProperties`.** El skill admite ese término «allí donde la plantilla lo prevea»: esta **no** lo prevé, así que **MUST NOT** aparecer. Su equivalente funcional aquí es la lista «Datos que el usuario envía al lanzarla» de cada acción (§3.5).

**Válvula de escape.** Toda pista técnica o de diseño que el usuario suelte en la conversación **MUST NOT** meterse en la spec, pero **MUST NOT** perderse: va al fichero hermano `design-guidelines.md`, donde el vocabulario técnico **sí** se admite.

### 3.3 El enunciado inicial del usuario suele venir cargado de vocabulario técnico

**CRITICAL.** Quien pide el trámite conoce el proyecto y escribe con sus nombres: cita ficheros, clases, campos y atributos. Eso es **material de partida**, no texto de la spec.

- **MUST** traducir cada frase a lenguaje de negocio antes de escribirla. **MUST NOT** copiarla tal cual «porque el usuario lo dijo así».
- **MUST** conservar el **contenido**: lo que se filtra es el vocabulario, nunca el requisito.
- Si al traducir queda una **decisión técnica** que el usuario impone (reutilizar un mecanismo, no usar cierto patrón), esa decisión va a `design-guidelines.md`, no a la spec.
- Si la traducción deja una duda de negocio (qué significa funcionalmente algo que el usuario nombró técnicamente), **MUST** preguntarla en el chat en vez de inventarla.

> **Ejemplo** (ilustrativo, NO normativo): siete traducciones sobre trámites **inventados**. Lo que se copia es el **tipo** de traducción, nunca los nombres.

| Lo que dice el enunciado | Lo que va en la spec |
|---|---|
| «vive en `TramiteInstance.xml` (`<help>`) y se muestra en un popup» | «es el texto de ayuda que el usuario lee antes de crear el expediente» |
| «el `InitialEventManagerImpl` copia nombre, apellidos y DNI del usuario registrador» | «al crear el expediente, el interesado es la persona que lo crea, con su nombre, sus apellidos y su documento de identidad» |
| «`closed="true"`: sin eventos disponibles» | «el estado cierra el expediente: desde él ya no se puede lanzar ninguna acción» |
| «`serial:` de dos pasos con `FirmaController.firmarDocumentoEntrada`» | «al pulsar el botón, el usuario firma el documento en su equipo con su certificado digital y a continuación se presenta» |
| «se genera el PDF `documentospdf/autorizacion.xml` en el campo `pdfAutorizacion`» | «se genera el documento de autorización y queda guardado en el expediente» |
| «`showIf` sobre `numeroCuenta` cuando `formaCobro == "TRANSFERENCIA"`» | «el número de cuenta solo se muestra cuando se elige cobrar por transferencia» |
| «regla `FirmaPdf`» | «el sistema comprueba que la firma es válida y que corresponde al documento de identidad de la persona esperada» |

### 3.4 Identificadores y numeración

| Elemento | Formato | Ámbito de numeración | Dónde vive |
|---|---|---|---|
| Historias de usuario | `HU-NNN` | **Global a la spec** | `specification.md` |
| Escenarios | `ESC-NNN` | **Global a la spec** (no por historia) | `specification.md` |
| Comprobaciones de una acción | `VAL-<ESTADO>-<ACCION>-NNN` | **Por pareja (estado, acción)**: cada pareja arranca su cuenta en `001` | `estados.md` |
| Reglas de negocio | `RN-NNN` | **Global a la spec** | `estados.md` |
| Datos que rellena el sistema | `CC-NNN` | **Global a la spec** | `estados.md` |
| Reglas de pantalla | `RUI-<ESTADO>-<PERFIL>-NNN` | **Por pantalla** (pareja estado + perfil): cada pantalla arranca su cuenta en `001` | `pantallas-<fase>.md` |

Reglas:

- Numeración desde `001`, **tres dígitos**, sin huecos al crear.
- `<ESTADO>`, `<ACCION>` y `<PERFIL>` van tal cual, en MAYÚSCULAS con guiones bajos. En la pantalla de solo consulta para el resto de perfiles, `<PERFIL>` es la palabra `GENERICA`.
- El ámbito es **local**: para añadir una comprobación basta mirar las de su pareja (estado, acción); para añadir una regla de pantalla, las de su pantalla. Solo `RN-` y `CC-` obligan a mirar el fichero entero, y ambos viven en `estados.md`.
- **Los identificadores no se renumeran nunca.** Al borrar un elemento su número se conserva como hueco. Si se renombra un estado, una acción o un perfil, sus identificadores cambian con él y el renombrado **MUST** propagarse a toda la spec.
- **MUST NOT** usarse el prefijo `RES-`. En un expediente **no existen** las restricciones de entidad: el expediente vive guardado desde que nace, con casi todos sus datos vacíos, y cada dato se exige **solo** en la pareja (estado, acción) en que se pide. Toda obligatoriedad es, por tanto, una `VAL-`.
- **MUST NOT** usarse ningún otro prefijo ni taxonomía: la conversión a la taxonomía técnica es trabajo del diseño.
- ✅ CORRECTO: `HU-002`, `ESC-007`, `VAL-PENDIENTE_INFORME-INFORMAR-001`, `RN-004`, `CC-002`, `RUI-REVISION_DATOS-CREADOR-003`, `RUI-REVISION_DATOS-GENERICA-001`
- ❌ INCORRECTO: `VAL-001` (sin estado ni acción), `VAL-Pendiente-Informar-001` (no va en mayúsculas con guiones bajos), `RUI-001` (sin estado ni perfil: se duplicaría entre pantallas), `RES-Expediente-001` (prefijo prohibido en esta plantilla), `RN-PENDIENTE-001` (`RN-` no lleva ámbito), `VAL_001` (guión bajo como separador de prefijo), `HU-001-ESC-001` (el escenario no anida el identificador de la historia)

### 3.5 La lista de datos por acción es una lista de permisos

**CRITICAL — es la regla de seguridad de esta plantilla.** Cada acción declara, en `estados.md`, la lista **cerrada** de «Datos que el usuario envía al lanzarla». Esa lista tiene dos consecuencias, y las dos hacen daño si se redacta mal:

- Un dato que **falta** en la lista **no se guarda**: el usuario lo escribirá en pantalla y su valor se perderá **en silencio**, sin ningún error. Por eso **MUST** figurar en la lista **todo** dato que la pantalla de ese estado deje rellenar.
- Un dato que **sobra** en la lista queda **a merced de quien use el expediente**: podría enviar el valor que quisiera aunque la pantalla no se lo ofrezca. Por eso **MUST NOT** figurar ningún dato que el sistema rellena solo (`CC-`), ni el estado del expediente, ni quién lo creó, ni su centro, ni ningún documento generado.

Reglas de redacción:

- **MUST** escribirse la lista en **todas** las acciones, aunque sea `*(ninguno)*`.
- **MUST** cuadrar, dato a dato, con el «Qué puede rellenar» de la pantalla del perfil que lanza la acción: lo rellenable en pantalla y lo enviable por la acción son la misma lista.
- **MUST NOT** escribirse `todos` ni `todos los datos del estado`: la lista es siempre explícita.
- **MUST NOT** confiarse en que la pantalla oculte o bloquee un dato: eso es comodidad para el usuario, nunca una defensa.
- **Excepción — el documento que el interesado firma en su propio equipo** sí entra en la lista de la acción que lo presenta, aunque lo produzca el sistema: es el único sitio donde se puede comprobar la firma. **MUST** decirse explícitamente que es una excepción.

### 3.6 Lo obvio pesa tanto como lo complicado

**CRITICAL — al redactar y al revisar, presta a lo trivial la MISMA atención que a lo sutil.** El mayor riesgo de la spec de un trámite no son las reglas difíciles —esas reciben atención de sobra— sino las **obvias**: un dato que no puede quedar vacío, la pantalla de solo consulta de un estado cerrado, el botón «Salir», el estado en el que nace el expediente, el escenario del camino feliz, la confirmación antes de algo irreversible.

- **MUST** declarar una regla aunque sea evidente: si «no hace falta decirla» porque se sobreentiende, es justo la que más se olvida.
- **MUST NOT** saltarse una candidata trivial por obvia mientras se resuelven con detalle las complejas.

### 3.7 Una versión nueva de un trámite que ya existe

Una iniciativa puede especificar una **versión nueva** de un trámite ya implementado, en vez de un trámite nuevo. Se declara en el apartado «El trámite» del índice, en la línea **Versión**, diciendo de qué trámite es versión y **qué cambia** respecto a la anterior.

- La versión nueva se especifica **completa**: fases, estados, acciones, pantallas y documentos, como si fuera nueva. **MUST NOT** escribirse como un delta, porque una versión es un ciclo de vida independiente del de la anterior.
- **MUST** declararse explícitamente qué **no** cambia respecto a la versión anterior, en el apartado «Fuera de alcance».
- La numeración de los identificadores empieza en `001` y es **local** a esta spec: **MUST NOT** referenciarse identificadores de iniciativas anteriores.

### 3.8 Una modificación de una versión que ya existe

Una iniciativa puede **modificar en su sitio** una versión ya implementada de un trámite, en vez de crear un trámite o una versión nueva. Se declara en la línea **Versión** del apartado «El trámite», diciendo qué trámite y qué versión se modifican y **qué cambia**.

- **Cuándo modificar y cuándo versionar:** modificar solo vale si el cambio es **compatible con los expedientes ya abiertos** de esa versión (no elimina ni renombra fases, estados ni datos por los que un expediente pueda estar pasando, ni reinterpreta datos ya guardados). Si no lo es → es una **versión nueva** (§3.7). Ante la duda, el skill lo pregunta al usuario. Es el mismo criterio que el anti-patrón de `k-tipo-expediente` (`versionado.md` §4).
- **Delta + conservación por defecto.** La spec declara **SOLO** lo nuevo o cambiado; todo lo no mencionado de la versión real **MUST** conservarse tal cual. **MUST NOT** copiarse en la spec el estado actual que no cambia — el código de la carpeta de versión es la fuente de verdad del as-is (el diseñador lo lee de `src/main/java/com/educaflow/tramites/…`).
- **Ficheros de la spec:** las cardinalidades de §2 se relajan — solo se crean los ficheros con algo cambiado. `estados.md` solo si cambia el ciclo de vida; `pantallas-<fase>.md` solo para las fases con alguna pantalla nueva o cambiada; `documentos.md` solo si cambian los documentos. En las tablas del índice, la fila de una fase o apartado sin cambios dice `*(sin cambios)*` en vez de enlazar un fichero.
- **Dentro de un fichero del delta**, cada estado, pantalla, acción o documento que se toque se describe **completo en lo que cambia** y marca el resto como conservado; los elementos **nuevos** se especifican completos, como en greenfield.
- **Historias y escenarios:** cubren **lo cambiado** (camino feliz y errores del cambio); **MUST NOT** re-especificarse escenarios del comportamiento que no se toca.
- **MUST** declararse explícitamente qué **no** cambia, en el apartado «Fuera de alcance».
- La numeración de los identificadores empieza en `001` y es **local** a esta spec: **MUST NOT** referenciarse identificadores de iniciativas anteriores.

---

## 4. El índice — `specification.md`

Los apartados van en el orden de la plantilla, sin inventar ninguno ni omitir ninguno.

- **`# Objetivo`** — una frase con lo que permite hacer el trámite y a quién. **Qué NO va:** rutas, paquetes, nombres de carpeta.
- **`# El trámite`** — el nombre visible, el colectivo al que va dirigido, quién puede iniciarlo, para qué sirve, el **texto de ayuda literal** que el usuario lee antes de empezar, y si es la primera versión, una versión nueva de un trámite existente (§3.7) o una modificación de una versión existente (§3.8). El texto de ayuda se escribe **tal cual lo verá el usuario**: es contenido, no una nota para el desarrollador.
- **`# Actores y perfiles`** — una fila por cada perfil que use algún estado, con qué papel juega **en este trámite** y **quién lo ostenta** (un tipo de usuario o un cargo del centro). **MUST** quedar asignado todo perfil que use algún estado: un perfil sin nadie que lo ostente deja su estado inalcanzable. **MUST NOT** declararse un perfil que ningún estado use.
- **`# Historias de usuario`** — una sección `## HU-NNN — Como [Actor] quiero [feature] para [motivo]` y, **debajo de cada una**, sus escenarios `ESC-NNN`. No hay apartado de escenarios aparte. Reglas en §4.1.
- **`# Fases y estados`** — el resumen (estado en que nace, estados que cierran, desde dónde se borra) y la tabla de fases con enlace a su fichero de pantallas. El detalle vive en `estados.md`.
- **`# Pantallas`** — una fila por fase, con enlace a su `pantallas-<fase>.md`. **MUST** haber una fila por cada fase y un fichero por cada fila.
- **`# Documentos`** — una frase con cuántos documentos genera el trámite y el enlace a `documentos.md`. Si no genera ninguno, se dice y **no** se crea el fichero.
- **`# Registros de entrada y salida, y notificaciones`** — en qué momento del trámite queda constancia oficial de que algo entró o salió del centro, con qué documento principal y con qué anexos; y qué avisos se envían, a quién y cuándo. Si no hay, `*(ninguno)*`.
- **`# Seguridad`** — **solo los roles con algún acceso** (tipos de usuario y cargos de `CLAUDE.md`); la seguridad es *deny by default* y **MUST NOT** listarse un rol «sin acceso». Por cada rol: qué puede hacer y su **alcance por centro** (solo los suyos / los de su centro / todos los centros). Distíngase de los perfiles: el perfil dice quién tiene el turno; la seguridad, quién puede ver.
- **`# Datos iniciales`** — qué debe existir precargado para que el trámite funcione: la asignación de perfiles (a qué tipo de usuario o cargo, y si vale para todo el trámite o solo para esta versión), la categoría del trámite y cualquier otro dato maestro. Este apartado es el **único estado previo** que los escenarios pueden presuponer, junto con los datos de demo.
- **`# Fuera de alcance`** — lo que el negocio decide **no** hacer.

### 4.1 Historias de usuario y escenarios

Cada historia tiene **al menos un escenario** y cada escenario pertenece a exactamente una historia (la que lo contiene). Los escenarios cubren el camino feliz, los alternativos y los errores.

**CRITICAL — claridad, especificidad y explicitud.** Cada escenario nombra los datos concretos que usa, el valor exacto que se introduce en cada dato, el botón exacto que se pulsa y la respuesta literal del sistema (el texto del mensaje, el estado resultante). Nada se deja implícito.

**CRITICAL — formato y autosuficiencia.** Cada `ESC-NNN` se convierte en el diseño en un test que se ejecuta contra una aplicación recién arrancada, **sin estado previo**. Por eso **cada escenario se escribe SIEMPRE como una lista de pasos numerados** (un paso por línea); **MUST NOT** escribirse como frases separadas por «;». La secuencia es completa y autosuficiente:

1. Empieza con el actor **iniciando sesión**.
2. Sigue con la **preparación**: crear el expediente y **recorrer, uno a uno, todos los estados intermedios** hasta llegar al que se quiere probar, cerrando e iniciando sesión cada vez que cambia el actor. El único estado previo admisible es el de «Datos iniciales».
3. Realiza la **acción que se prueba**, con sus valores concretos y el botón exacto.
4. Termina con la **respuesta del sistema**.

Entre medias puede haber más pasos y **ramas condicionales** (*«si <condición> el sistema hace X; si no, hace Y y no hace Z»*). Un escenario con ramas puede dar lugar a más de un test.

**CRITICAL — usuarios y centros reales de los datos de demo.** Cuando un escenario nombre un usuario o un centro, **MUST** usar los de `src/main/resources/data-demo/input/` (`centros-demo.xml` y `usuarios-demo.xml`); **MUST NOT** inventar centros, cuentas, logins ni documentos de identidad. Léelos antes de redactar. De ahí salen los centros `CIPFP Mislata` (código `46019660`) y `CIPFP Batoi` (código `03012165`), las cuentas de cada tipo de usuario y cargo (el login es su correo, p. ej. `alumno1@mislata.es`, `jefeestudios1@mislata.es`, `secretario@mislata.es`), todas con contraseña **`demo1234`**, y los documentos de identidad reales de cada cuenta. La única identidad admitida que no está ahí es el administrador global `admin` / `admin`. Solo se admite inventar valores que **no identifican** a nadie.

- ✅ CORRECTO:
  ```
  - ESC-004 — Se intenta resolver sin indicar el motivo:
    1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
    2. Abre la lista de trámites disponibles, elige «<nombre del trámite>» y crea un expediente nuevo.
    3. Rellena <dato> con «<valor>» y pulsa «<texto del botón>».
    4. …
  ```
- ❌ INCORRECTO (presupone estado): un escenario cuyo primer paso es *«El tramitador abre un expediente pendiente de resolución»* — nadie lo ha creado ni lo ha hecho llegar hasta ahí dentro del escenario.
- ❌ INCORRECTO (varias acciones en un paso): *«Rellena la solicitud y la presenta.»*

**Qué NO va:** nombres de clase, capas, comandos de prueba, pasos Given/When/Then (eso es del diseño).

---

## 5. El ciclo de vida — `estados.md`

Un único fichero, con las secciones de la plantilla en su orden.

### 5.1 Resumen

**MUST** declarar: las fases con su título, el estado en el que nace el expediente (**exactamente uno en todo el trámite**, no uno por fase), los estados que lo cierran y desde qué estado se puede borrar (o que no se puede desde ninguno).

### 5.2 Al crear el expediente

**MUST** estar siempre, aunque no se rellene nada. Declara lo que el sistema pone solo entre que el usuario pide crear el expediente y la primera pantalla que ve, y en qué estado nace.

- **MUST NOT** listarse aquí lo que el sistema rellena en cualquier caso por el mero hecho de crear un expediente (el trámite al que pertenece, el centro, quién lo crea, su número): eso lo hace la plataforma sola y no es decisión del negocio.
- **REQUIRED** — si algún documento del trámite se firma **en el equipo del interesado**, **MUST** declararse con qué documento de identidad se firmará.
- **REQUIRED** — si el trámite deja constancia de **entrada** en algún momento, **MUST** declararse quién queda como persona interesada y quién como solicitante.
- Si no aplica ninguna de las dos, **MUST** decirse explícitamente que no aplica.

### 5.3 Fases y estados

Una sección `## Fase <FASE>` por fase, en el orden en que se recorren, y dentro una `### Estado <ESTADO>` por estado.

Cada estado **MUST** declarar: quién tiene el turno (un perfil, o ninguno), si cierra el expediente, qué consulta el usuario, qué datos introduce y qué acciones puede lanzar.

- **MUST** declararse **todos** los estados, incluidos los que no tienen acciones y los que cierran el expediente.
- Un estado **sin perfil** es aquel en el que nadie tiene el turno: solo se puede consultar y salir.
- El **borrado** del expediente y la **salida** de la pantalla se describen en lenguaje de negocio («borrar el expediente», «salir sin cambiar nada»); **MUST NOT** nombrarse como acciones técnicas.

### 5.4 Acciones

Una subsección `#### Acción <ACCION>` por cada acción disponible en el estado. **MUST** declarar, en este orden: quién la lanza, si pide confirmación (y con qué texto), la lista cerrada de datos que el usuario envía (§3.5), las comprobaciones (`VAL-`), lo que produce (`RN-`, **en orden**) y a qué estado lleva.

**Comprobaciones (`VAL-`).** Son bloqueantes: si fallan, la acción se cancela y **no ocurre nada**. El **texto** es lo que debe cumplirse (la afirmación); la `condición` es la guardia que dice **cuándo** se comprueba. Si la guardia repite lo que el texto ya afirma, la comprobación nunca falla y sobra.

- Atributos: `mensaje` (el error literal que ve el usuario), `condición` (opcional), `actor` (opcional, si cambia según quién la lance).
- **REQUIRED — empieza por la obligatoriedad**, dato a dato de la lista de la acción: *«¿puede quedar vacío?»*. Es la comprobación más olvidada.
- El `mensaje` se redacta en lenguaje de negocio, diciendo cómo debe ser el dato, no cómo no debe ser, y sin jerga.
- Recorre `catalogos/catalogo-validaciones.md`.

**Lo que produce (`RN-`).** Son efectos automáticos que ocurren una vez superadas las comprobaciones. **Nunca bloquean**: si algo debe impedir la acción, es una `VAL-`. Si solo cambia lo que se ve, es una `RUI-`.

- **CRITICAL — el orden de la lista es normativo**: es el orden en que las cosas ocurren, y se implementa tal cual. Generar un documento, firmarlo y dejar constancia de su salida son tres reglas en ese orden, no una.
- Atributo opcional: `condición`, si el efecto solo ocurre en algunos casos.
- Recorre `catalogos/catalogo-reglas-negocio.md`.

**A qué estado lleva.** Una acción puede llevar a un estado de **otra fase** sin nada especial, **ramificar** según el valor de un dato (y entonces **MUST** cubrir todos los valores posibles de ese dato), **no cambiar de estado** (y entonces **MUST** decirse explícitamente) o **hacer desaparecer** el expediente (el borrado).

### 5.5 Tabla de transiciones

Una fila por cada combinación (estado, acción), más la fila del arranque, más la del borrado si lo hay. Una acción que ramifica produce **varias filas**, una por rama, con su condición.

**MUST** coincidir exactamente con las secciones de estados: es la vista de conjunto de lo mismo. Si discrepan, mandan las secciones y hay que corregir la tabla.

### 5.6 Datos que rellena el sistema

Los valores que **nunca** aporta el usuario. **MUST** declarar `momento` (al crear el expediente / al lanzar una acción concreta / cada vez que se consulta), `sobreescribible` (`nunca` o los perfiles que pueden forzar un valor) y `cálculo` (de dónde sale el valor, en lenguaje de negocio).

Un dato declarado aquí con `sobreescribible: nunca` **MUST NOT** aparecer en la lista de datos de ninguna acción. Recorre `catalogos/catalogo-datos-calculados.md`.

---

## 6. Las pantallas — `pantallas-<fase>.md`

Un fichero por fase, con una sección `## Estado <ESTADO>` por cada estado de esa fase.

### 6.1 Cuántas pantallas tiene un estado

**CRITICAL — todo estado tiene SIEMPRE, sin excepción, una pantalla de solo consulta para el resto de perfiles**, con un único botón **«Salir»**. Es la red de seguridad: cualquiera con acceso al expediente puede abrirlo estando en cualquier estado, y sin esa pantalla no podría ni verlo.

- Un estado **con perfil y con al menos una acción** tiene **además** la pantalla de ese perfil, que es la editable y la que lleva los botones de las acciones. Sin ella, quien tiene el turno caería en la de solo consulta y **el expediente se quedaría atascado sin ningún aviso**.
- Un estado **sin perfil**, o **sin acciones**, o que **cierra el expediente**, tiene **solo** la de solo consulta.
- Si un estado se presenta de forma distinta a más de un perfil, hay una pantalla por perfil.
- **MUST NOT** haber dos pantallas del mismo estado para el mismo perfil.

### 6.2 Qué declara cada pantalla

Quién la ve, qué ve bloque a bloque, qué puede rellenar, qué solo consulta, qué documentos se le muestran (y si incrustados en la pantalla o solo como descarga), el aviso permanente si lo hay, y sus botones.

**Botones.** Uno por viñeta: el **texto que ve el usuario**, qué acción lanza y si pide confirmación (con su texto).

- **MUST** haber un botón para **cada** acción declarada en ese estado, repartido entre las pantallas del estado. Una acción sin botón es una acción a la que el usuario no puede llegar.
- **MUST NOT** aparecer en una pantalla un botón de una acción que ese estado no declara.
- La pantalla de solo consulta lleva **únicamente** «Salir».
- Si desde el estado se puede borrar el expediente, su botón va en la pantalla del perfil que puede borrarlo, y **MUST** pedir confirmación.

**Coherencia con el ciclo de vida (CRITICAL).** El «Qué puede rellenar» de la pantalla del perfil **MUST** coincidir, dato a dato, con la lista «Datos que el usuario envía» de la acción que se lanza desde ella. Un dato rellenable que no esté en esa lista se pierde en silencio; un dato de la lista que no sea rellenable sobra y abre un agujero.

### 6.3 Reglas de pantalla (`RUI-`)

Cambian lo que el usuario **ve** o puede editar. **No bloquean** (eso es una `VAL-`) ni **producen** nada (eso es una `RN-`).

- Atributos: `disparador` (`continuo`, `al abrir la pantalla`, `al cambiar <dato>`) y `condición` (la que lo activa, o `Siempre`).
- Una regla de pantalla **nunca es una defensa**: ocultar o bloquear en pantalla no impide enviar el dato. Toda regla que oculte o bloquee algo importante **MUST** tener detrás su `VAL-` o su exclusión de la lista de datos de la acción.
- Si una regla combina efectos sobre datos distintos, se parte en varias.
- Recorre `catalogos/catalogo-reglas-ui.md`.

---

## 7. Los documentos — `documentos.md`

Solo existe si el trámite genera al menos un documento. Un bloque `## Documento: <Nombre>` por documento, más la tabla resumen y, al final, los trozos comunes a varios documentos.

Cada documento **MUST** declarar:

- **Qué es** y qué acredita, en lenguaje llano.
- **Cuándo se genera:** la acción y el estado exactos desde los que se produce, y si solo se genera en algunos casos.
- **Quién lo firma y dónde.** **MUST** elegirse una explícitamente:
  - **el propio interesado, en su equipo, con su certificado digital** — necesita tener un certificado instalado y la aplicación de firma del ciudadano; el sistema comprueba después que la firma es válida, que es una sola, que el certificado es de confianza, que no ha alterado el texto y que corresponde al documento de identidad de la persona esperada;
  - **el centro, con la firma institucional de un cargo** — **MUST** decirse **qué cargo** firma (hoy la plataforma resuelve la clave del **Director** y la del **Secretario** del centro); la pone el servidor y el usuario no interviene;
  - **una persona concreta, con el certificado custodiado que corresponde a su documento de identidad** — **MUST** decirse **quién** es esa persona (por el papel que juega en el trámite, no por su nombre); la pone el servidor y el usuario no interviene;
  - **no se firma**.
- **Dónde se estampa la firma:** en qué página y en qué zona de la hoja aparece el recuadro. Es una decisión de negocio (el impreso oficial la fija), no de diseño.
- **Si se registra:** de entrada, de salida o no se registra; y con qué documento principal y qué anexos.
- **Qué datos del expediente aparecen en él**, uno a uno, y cómo se presentan (etiqueta, casilla que se marca, texto libre).
- **Textos fijos** que lleva impresos y los **idiomas** en que se emite.
- **A quién se le muestra y dónde**, que **MUST** cuadrar con los «Documentos que se le muestran» de las pantallas.

Reglas:

- Todo documento **MUST** producirse por alguna acción declarada en `estados.md`, y toda acción que dice generar un documento **MUST** tener su bloque aquí.
- **MUST NOT** describirse el formato del fichero, su ruta, sus etiquetas ni la anchura de sus columnas: eso es del diseño.
- **MUST NOT** darse por sobreentendido que un documento se registra: si no se dice, no se registra.

---

## 8. Mapeo spec → diseño (test de completitud de la plantilla)

Cada sección del diseño que se construye a partir de esta spec se alimenta de un apartado concreto. Si un apartado queda vacío, la sección del diseño correspondiente **no se puede escribir**. Úsalo como comprobación final: recorre la columna izquierda y verifica que su fuente existe y está completa.

| Lo que el diseño tiene que decidir | Apartado de la spec que lo alimenta |
|---|---|
| Objetivo del trámite | `# Objetivo` |
| Identidad del trámite: nombre visible, categoría, versión activa | `# El trámite` (nombre visible, colectivo, versión) |
| Texto de ayuda del trámite | `# El trámite` (texto de ayuda literal) |
| Las **fases**, con su título y su orden | `estados.md` → `## Resumen` y las secciones `## Fase <FASE>` |
| Los **estados** de cada fase, con su título, su perfil, si es el inicial y si cierra | `estados.md` → `### Estado <ESTADO>` |
| Las **acciones** disponibles en cada estado, en su orden | `estados.md` → «Qué acciones puede lanzar» y las `#### Acción` |
| La **tabla de transiciones**, con sus ramas y sus condiciones | `estados.md` → `## Tabla de transiciones` (y «A qué estado lleva» de cada acción) |
| El **diagrama** del ciclo de vida | `estados.md` → la misma tabla de transiciones |
| Los **datos del expediente** y quién rellena cada uno (usuario o sistema) | `estados.md` → «Qué datos introduce el usuario» (usuario), «Datos que rellena el sistema» y «Qué produce la acción» (sistema) |
| Las **listas de opciones** de un dato con valores cerrados | `estados.md` → la descripción del dato y las ramas de las acciones que dependen de él |
| Lo que se rellena **al crear** el expediente, el documento de identidad de firma y quién es interesado y solicitante | `estados.md` → `## Al crear el expediente` |
| Los **efectos ordenados** de cada acción: generar, firmar, registrar, limpiar, avisar, transicionar | `estados.md` → «Qué produce la acción» (el orden es normativo) + «A qué estado lleva» |
| Las **comprobaciones** de cada pareja (estado, acción), con sus mensajes | `estados.md` → «Comprobaciones que deben pasar» (`VAL-`) |
| Qué **datos puede dictar el usuario** en cada acción (frontera de confianza) | `estados.md` → «Datos que el usuario envía al lanzarla» (§3.5) |
| Los **documentos**: cuándo se generan, en qué se guardan, quién los firma, si se registran | `documentos.md` |
| El **contenido** de cada documento y su recuadro de firma | `documentos.md` → «Qué datos del expediente aparecen en él», «Textos fijos», «Dónde se estampa la firma», «Idiomas» |
| Los **registros de entrada y de salida**, con su documento principal y sus anexos | `# Registros de entrada y salida, y notificaciones` + `documentos.md` |
| Los **bloques** de cada pantalla y qué datos agrupan | `pantallas-<fase>.md` → «Qué ve el usuario, bloque a bloque» |
| Qué pantallas existen por estado y por perfil, incluida la de solo consulta | `pantallas-<fase>.md` → §6.1 |
| Los **botones** de cada pantalla, su texto y su confirmación | `pantallas-<fase>.md` → «Botones» |
| Qué es editable y qué es de solo lectura en cada pantalla | `pantallas-<fase>.md` → «Qué puede rellenar» / «Qué solo puede consultar» |
| Qué documento se muestra incrustado y cuál solo como descarga | `pantallas-<fase>.md` → «Documentos que se le muestran» |
| Las reglas de visibilidad, obligatoriedad visual y avisos de pantalla | `pantallas-<fase>.md` → `#### Reglas de pantalla` (`RUI-`) |
| La **asignación de perfiles**: qué perfil a qué actor y con qué alcance | `# Actores y perfiles` + `# Datos iniciales` |
| Los **permisos** de lectura y el alcance por centro | `# Seguridad` |
| Los **datos maestros** que hay que precargar | `# Datos iniciales` |
| Los **tests**: uno por transición, uno de fallo por cada pareja con comprobaciones, y todos los estados visitados | `# Historias de usuario` (los `ESC-`) + `## Tabla de transiciones` + las `VAL-` |
| Los **supuestos** ante ambigüedades | `# Fuera de alcance` + lo que la conversación deje sin cerrar |

---

## 9. Barridos de completitud

Tras (re)generar el borrador de la spec, el skill lanza **subagentes de barrido** que buscan **candidatas que falten**, cada uno con su catálogo. Esta tabla **declara** los barridos de esta plantilla.

Los barridos se ejecutan por **etapas en orden** (las candidatas aceptadas de una etapa modifican la spec que leen las siguientes); dentro de una misma etapa, todos en paralelo:

- **Etapa A — cobertura**: primero, porque una historia o un escenario aceptados dan material nuevo al resto.
- **Etapa B — calidad y reglas**: sobre la spec ya completada con lo aceptado en A.

| Etapa | Barrido | Ámbito — una instancia por cada… | Iteración interna del subagente | Catálogo | Contrato de salida |
|---|---|---|---|---|---|
| A | **historias-escenarios** | toda la spec (**instancia única**) | Cruza los perfiles, la seguridad, las pantallas y los documentos contra las historias existentes: qué persona declarada no protagoniza ninguna historia, qué acceso declarado no ejercita ningún escenario, qué historia carece de camino feliz o de escenario de error. Propone la historia o el escenario que falta **con sus pasos ya redactados** (numerados, concretos, autosuficientes, con cuentas y centros de demo). | `catalogos/catalogo-historias-escenarios.md` | historias-escenarios |
| A | **cobertura-estados** | toda la spec (**instancia única**) | Recorre `estados.md` estado a estado, acción a acción y fila a fila de la tabla de transiciones, y comprueba cuáles no ejercita ningún escenario: estados a los que nadie llega, ramas que nadie prueba, el borrado, la vuelta atrás, los estados que cierran, la pantalla de solo consulta de cada estado, cada documento generado y cada registro. Propone el escenario que falta con sus pasos redactados. | `catalogos/catalogo-cobertura-estados.md` | historias-escenarios |
| B | **pasos-escenarios** | historia de usuario (`HU-NNN`) del índice | Escenario a escenario de su historia y, dentro de cada uno, paso a paso: (1) **granularidad** — ningún paso agrupa varias acciones ni deja datos sin concretar; (2) **pasos que faltan** — inicio de sesión, creación del expediente, recorrido de los estados intermedios, cambio de actor, valores concretos, pulsación del botón, respuesta literal; (3) **autosuficiencia** — nada presupone estado previo fuera de «Datos iniciales» y los datos de demo. Propone los pasos concretos que sustituyen o se insertan. | `catalogos/catalogo-pasos-escenario.md` | pasos-escenarios |
| B | **validaciones** | el fichero `estados.md` (**instancia única**) | **Primero** recorre dato a dato la lista «Datos que el usuario envía» de **cada** acción y comprueba la **obligatoriedad** (¿puede quedar vacío? si no → candidata), la comprobación más olvidada. **Después**, acción a acción y dato a dato, recorre las tablas del catálogo. Toda candidata es de tipo `VAL` y se ancla a una pareja (estado, acción). Propone `mensaje` y, si aplican, `condición` y `actor`. | `catalogos/catalogo-validaciones.md` | reglas |
| B | **reglas-negocio** | el fichero `estados.md` (**instancia única**) | Acción a acción y transición a transición: qué hace el sistema automáticamente al confirmarse cada una (generar un documento, firmarlo, registrar, avisar, limpiar residuos de un intento anterior, anotar quién y cuándo, tocar algo fuera del expediente). Propone **en qué posición del orden** entra la regla y, si aplica, su `condición`. | `catalogos/catalogo-reglas-negocio.md` | reglas |
| B | **datos-calculados** | el fichero `estados.md` (**instancia única**) | Dato a dato: cuáles los dicta el sistema y no el usuario, cruzando lo que aparece en pantallas y documentos con las listas «Datos que el usuario envía». Un dato que nadie envía pero que se ve en algún sitio es candidato; y un dato calculado que **sí** figura en una lista de acción es un agujero que hay que señalar. Propone `momento`, `sobreescribible` y `cálculo`. | `catalogos/catalogo-datos-calculados.md` | reglas |
| B | **reglas-ui** | fichero `pantallas-<fase>.md` | Pantalla a pantalla de la fase y, dentro de cada una, bloque a bloque, dato a dato y botón a botón, considerando el perfil que la ve y el estado del expediente: visibilidad condicional, solo lectura, obligatoriedad visual, valores propuestos, opciones limitadas, confirmaciones, avisos y cómo se presenta cada documento. Propone `disparador` y, si aplica, `condición`. | `catalogos/catalogo-reglas-ui.md` | reglas |

Reglas de los barridos:

- Cada subagente **propone candidatas, no escribe la spec**: entran solo cuando el usuario las acepta en la conversación.
- Cada subagente recibe su **elemento asignado**, su catálogo y la carpeta completa de la spec. **MUST** leer lo ya declarado en toda la spec y **MUST NOT** proponer una candidata que duplique algo existente (mismo efecto sobre el mismo dato, acción, pantalla o escenario, aunque esté redactado distinto).
- El catálogo es **solo una guía no exhaustiva**: el subagente **puede y debe** proponer candidatas que no figuren en él si el negocio de la spec las sugiere, indicando `(fuera de catálogo)`.
- **CRITICAL — lo obvio cuenta igual que lo complejo.** **MUST** proponer también las candidatas obvias: un dato obligatorio, la pantalla de solo consulta de un estado, el botón «Salir», la confirmación antes de borrar, el escenario del camino feliz.
- Toda candidata va en **lenguaje de negocio**: aplican las prohibiciones de §3.2. Los pasos de escenario propuestos usan **cuentas y centros de demo**.
- Una candidata **debe deducirse de lo que la spec ya cuenta**: el subagente **MUST NOT** inventar fases, estados, acciones, datos ni documentos que la spec no tiene.
- Respetar la frontera entre familias: si **bloquea** → `validaciones` (`VAL`); si **produce o escribe** → `reglas-negocio` (`RN`); si es un **valor que pone el sistema** → `datos-calculados` (`CC`); si solo cambia **lo que se ve** → `reglas-ui` (`RUI`). Ante la duda, se propone una sola vez, en la familia de su efecto real.
- **MUST NOT** proponerse ninguna candidata de tipo `RES`: esta plantilla no usa ese prefijo (§3.4).
- **CRITICAL — iniciativa de MODIFICACIÓN de una versión existente** (la línea **Versión** del apartado «El trámite» declara una modificación, §3.8): los barridos proponen candidatas **solo del delta declarado**. La spec es un delta y lo no mencionado se conserva tal cual, así que proponer sobre el comportamiento intacto es re-especificarlo — justo lo que §3.8 prohíbe.
  - El **universo a barrer** son los estados, acciones, pantallas, datos y documentos que la spec **toca**, más lo que el delta rompería si estuviera mal (los estados de origen y destino de una transición cambiada, las pantallas modificadas).
  - **MUST NOT** proponerse una candidata cuyo anclaje (la pareja estado/acción, la pantalla o el dato) no aparezca en los ficheros de esta spec: si no está, es as-is y no se toca.
  - **MUST NOT** proponerse una candidata "que falta" por comparación con el **código real** de la versión modificada: los barridos leen la spec, no el árbol.
  - Sí **MUST** proponerse lo que el delta deja a medias: un dato nuevo sin obligatoriedad declarada, una acción nueva sin pantalla, un estado nuevo sin su pantalla de solo consulta, un escenario que no ejercita el cambio.
- **Ficheros ausentes:** en una modificación las cardinalidades de §2 se relajan y `estados.md` o `documentos.md` pueden no existir. Un barrido cuyo ámbito sea un fichero que **no** está en la carpeta de la spec **MUST NOT** lanzarse (cero instancias); no es un fallo ni algo que reportar.
- La columna **«Contrato de salida»** dice cuál de los contratos JSONL del skill usa cada barrido (`reglas`, `historias-escenarios` o `pasos-escenarios`). El motor **MUST** leerla en vez de deducir el contrato del nombre del barrido, que lo pone esta plantilla: aquí los barridos de reglas se llaman `validaciones` y `datos-calculados`, y `cobertura-estados` entrega el contrato `historias-escenarios`.
- En el contrato de reglas, el campo `elemento` se escribe como `estado: <FASE>/<ESTADO> — acción: <ACCION> — dato: <dato>` para una `VAL` o una `RN`, como `dato: <dato>` para un `CC` y como `pantalla: <ESTADO>/<PERFIL> — dato: <dato>` para una `RUI`. Los atributos que aplican son `mensaje`, `condición` y `actor` (`VAL`); `condición` (`RN`); `momento`, `sobreescribible` y `cálculo` (`CC`); `disparador` y `condición` (`RUI`).

---

## 10. Ejemplo

La carpeta [`example/`](./example/) contiene una spec completa, terminada e instanciada, de un trámite **inventado** que no existe en el proyecto. Sirve solo como referencia del aspecto final.

- **MUST NOT** copiarse su contenido al output.
- **MUST NOT** tomarse por norma su número de fases, de estados ni de documentos, ni sus nombres, ni su reparto de perfiles: el patrón vale para cualquier combinación.
