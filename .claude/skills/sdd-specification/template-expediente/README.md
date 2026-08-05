# Guía de los ficheros de la especificación de un expediente

Explica qué debe contener cada fichero de la especificación de un **trámite y su tipo de expediente** (en adelante, "el expediente"), cómo clasificar sus elementos y cómo numerarlos. Esta guía dirige la redacción y la revisión de la spec; **MUST NOT** copiarse ninguno de sus bloques explicativos al output.

Es **el único fichero de esta carpeta de plantillas que el skill `sdd-specification` conoce por nombre**: el skill lee este `README.md` y, a través de él, descubre y usa el resto. Por eso aquí se declara qué hay en la carpeta y cómo se usa cada cosa.

**En qué se diferencia de la plantilla de sistemas (`template-system`):** un expediente no es un mantenimiento CRUD con listados y menús — es una **tramitación**: un único registro (el expediente) que avanza por una **máquina de estados**, donde en cada estado "tiene el turno" un **perfil**, cada botón dispara un **evento**, y las transiciones ejecutan efectos sobre otros subsistemas (documentos PDF, registro de entrada/salida, firmas, correos). Por eso aquí la unidad central de especificación no es la pareja entidad+pantalla sino la terna **(estado, evento, perfil)**, y todo lo demás (validaciones, campos editables, vistas, efectos) se ancla a ella.

**La máquina no se especifica estado a estado desde cero: se compone a partir de FASES.** Una fase ([`fases.md`](fases.md)) es un grupo estándar de estados con un objetivo común (informar, recoger y presentar datos, emitir la respuesta, terminar) que ya trae decididos sus estados, eventos, perfiles, vistas y efectos; la spec **elige qué fases usa, rellena sus parámetros y declara sus desviaciones**, y de ahí salen los estados concretos. Especificar por fases es más sencillo y más uniforme que inventar cada estado — pero las fases viven solo en la spec: **el código generado no sabe de fases, solo de estados** (la máquina que se ejecuta sigue siendo la lista plana de estados).

## Ficheros de esta carpeta de plantillas

| Fichero | Qué es | Cómo se usa |
|---|---|---|
| `README.md` | **Esta guía**: el conjunto de ficheros, los apartados de cada uno, la clasificación de los elementos y las reglas de numeración. | Es la única referencia que el skill conoce por nombre; dirige las preguntas de la Fase 2 y las validaciones de la Fase 3. **MUST NOT** copiarse al output. |
| `specification.md` | **La plantilla del índice.** | Se reproduce **literalmente**, sustituyendo los placeholders por contenido real. Produce **un** fichero índice (el único con frontmatter `type: specification`). |
| `estados.md` | **La plantilla de la máquina de estados**: estados y transiciones. | Se instancia una vez por spec, como `estados.md`. |
| `fases.md` | **El catálogo de fases**: los grupos estándar de estados (`F_INICIO`, `F_ENTRADA`, `F_SALIDA`, `F_TERMINADO`) con sus estados, eventos, vistas, efectos y parámetros, las reglas de nomenclatura y ensamblado, y el concepto de **paso** (asistente dentro de un estado). | Es la **guía de alto nivel para componer la máquina**: se consulta ANTES de escribir `estados.md`. La spec instancia fases (apartado "Fases" del índice) y de ellas derivan los estados; las fases son guías — pueden modificarse si el negocio lo pide, declarando la desviación. **MUST NOT** copiarse al output. |
| `entity.md` | **La plantilla de los ficheros de modelo** (el expediente y sus entidades hija). | Se instancia una vez por modelo, una por `entity-<Nombre>.md`. |
| `vistas.md` | **La plantilla del fichero único de vistas por estado.** | Se instancia una vez por spec, como `vistas.md`. |
| `documento.md` | **La plantilla de las fichas de documento PDF** (incluidas sus firmas). | Se instancia una vez por documento, una por `documento-<slug>.md`. |
| `catalogos/` | **Carpeta de catálogos de referencia**, uno por barrido: `catalogo-historias-escenarios.md`, `catalogo-pasos-escenario.md`, `catalogo-validaciones.md` (`VAL-` + `RES-`, catálogo único), `catalogo-campos-calculados.md` (`CC-`), `catalogo-reglas-ui.md` (`RUI-`), `catalogo-estados-transiciones.md` (completitud de la máquina), `catalogo-acciones-transicion.md` (efectos `RN-` y subsistemas), `catalogo-firmas.md` (fichas `FIR-` y el algoritmo de decisión del mecanismo de firma). | Se consultan al rellenar cada apartado y son la referencia de los **barridos de completitud** (ver esa sección al final). **MUST NOT** copiarse al output. |
| `example/` | **Carpeta con un ejemplo completo** de spec terminada e instanciada. | Referencia del aspecto final. **MUST NOT** copiarse su contenido al output. |

## Ficheros que produce la especificación

La especificación **no es un único fichero**: es un conjunto de ficheros dentro de la carpeta de la iniciativa.

| Fichero | Plantilla | Qué contiene |
|---|---|---|
| `specification.md` | `specification.md` | El **índice**: el trámite, actores y perfiles, fases, historias de usuario con sus escenarios, las tablas de enlaces (modelos, documentos), el resumen de subsistemas y los apartados de recursos y fuera de alcance. Es el único con frontmatter `type: specification`. |
| `estados.md` | `estados.md` | La **máquina de estados**: la tabla de estados, la creación del expediente, y una ficha por transición (`TR-NNN`) con sus campos editables, validaciones (`VAL-`) y efectos (`RN-`). |
| `estados.puml` / `estados.png` | *(sin plantilla; PlantUML)* | El **diagrama de la máquina de estados**, y su imagen renderizada **siempre** a partir del `.puml` (nunca a mano). |
| `entity-<Nombre>.md` | `entity.md` | Un fichero **por cada modelo**: el expediente y, si las hay, sus entidades hija. Campos, restricciones (`RES-`) y campos calculados (`CC-`). |
| `model.puml` / `model.png` | *(sin plantilla; PlantUML)* | El **diagrama de clases** de los modelos (expediente, enums, entidades hija) y su imagen renderizada. |
| `vistas.md` | `vistas.md` | **Un único fichero** con todas las vistas: el almacén de paneles y, por cada estado, la vista del perfil que tiene el turno y la vista genérica, con sus botones y reglas de UI (`RUI-`). |
| `documento-<slug>.md` | `documento.md` | Un fichero **por cada documento PDF**: su contenido, su ciclo (generación, registro) y sus **firmas** (`FIR-`). |

- `<Nombre>` del modelo va en **PascalCase** (p. ej. `entity-ComisionServicio.md`, `entity-LineaGasto.md`); `<slug>` del documento en **kebab-case** (p. ej. `documento-solicitud-comision.md`). `estados.md`, `vistas.md` y los cuatro ficheros de diagrama son **nombres fijos** (uno por spec).
- Solo `specification.md` lleva frontmatter. Los demás `.md` empiezan directamente por su título (`# Estados y transiciones`, `# Modelo: …`, `# Vistas`, `# Documento: …`). Los `.puml` empiezan por `@startuml` y terminan por `@enduml`.

---

## Qué preguntar (guía de la entrevista)

Cuando el usuario trae un trámite nuevo —muy a menudo a partir de un **impreso oficial en PDF** y de un documento de instrucciones—, estas son las preguntas que destapan la spec. No es un formulario: se pregunta **conversando**, y solo lo que el material aportado no responda ya.

### Si hay un impreso oficial, léelo primero así

Los impresos oficiales siguen una estructura fija (apartados A, B, C, D… y cajetín de registro) que se traduce casi 1:1 a la plantilla:

| Apartado típico del impreso                                    | Se convierte en                                                                                                                                                                                                                                        |
|----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A — Datos de identificación del solicitante                    | Campos **precargados** de la ficha del usuario en la creación. Preguntar: ¿cuáles se precargan? ¿alguno no está en la ficha y hay que teclearlo?                                                                                                       |
| B — Expone (+ checkboxes de circunstancias)                    | Campos del expediente. Un grupo de checkboxes excluyentes es un **enum** (con su "otras: especificar" como campo de texto condicionado).                                                                                                               |
| C — Solicita (+ tablas de filas repetibles)                    | El objeto de la solicitud. Cada **tabla de filas** es una **entidad hija** maestro-detalle (preguntar: ¿mínimo de filas? ¿qué es obligatorio por fila?).                                                                                               |
| "Documentación que se aporta"                                  | **Adjuntos** justificativos. Preguntar: ¿obligatorios siempre o según la circunstancia? Ojo a las checklists **condicionadas**: qué documentos se exigen suele depender del enum elegido (según el colectivo o la causa, la lista de adjuntos cambia). |
| Fecha y firma del solicitante                                  | La **firma de presentación** (`FIR-`) y la transición de presentar con su **registro de entrada**.                                                                                                                                                     |
| D — Resolución (a rellenar por el centro)                      | Los campos de la resolución, sus **ramas** (admitir / no admitir / subsanar…) y la **firma** de quien resuelve. Si la resolución repite una tabla de filas del apartado C, sospecha **resolución parcial** (fila a fila) — preguntarlo.                |
| Cajetín "Registro de entrada"                                  | El registro de entrada de la plataforma: **gratis, no se especifica**.                                                                                                                                                                                 |
| "Ejemplar para la administración / para la persona interesada" | Irrelevante en digital: no se especifica.                                                                                                                                                                                                              |

Las **instrucciones del trámite** (plazos, condiciones) esconden casi siempre validaciones y reglas de negocio que el impreso no muestra (*"la renuncia a X implica la renuncia a Y"*, *"las razones deben justificarse siempre documentalmente"*): **REQUIRED** pedir ese documento si existe y minarlo.

### Las preguntas por apartado de la spec

- **Trámite:** ¿a quién va dirigido (en qué rama del árbol aparece)? Si pueden iniciarlo **dos tipos de usuario** (el interesado o su representante legal), ¿son dos trámites gemelos o uno? ¿público/privado? ¿Es la primera versión o evoluciona uno existente?
- **Perfiles — la pregunta que decide media máquina:** ¿quién **estudia** la solicitud y quién **firma** la resolución — son la misma persona? Si quien firma no la estudia (el director que firma lo que secretaría resuelve), la firma va al **portafirmas** con su estado de espera y transición automática; si resuelve y firma la misma persona, firma en su propia vista. ¿Y con qué ámbito ve cada perfil los expedientes?
- **Modelo — por cada campo del impreso:** ¿se **precarga** de la ficha del usuario, lo **teclea**, o se **elige de un catálogo** que el sistema ya conoce (sus matrículas, los ciclos del centro)? Texto libre solo cuando no hay catálogo posible.
- **Fases — la máquina se pregunta primero a alto nivel** (con [`fases.md`](fases.md) delante): ¿el usuario necesita una pantalla de ayuda antes de empezar (`F_INICIO`)? ¿quién revisa lo presentado, o no hay revisión (`hay_revision`)? ¿hay respuesta documental del centro (`F_SALIDA`) — una o varias (propuesta + resolución)? ¿cuántos finales distintos hay (`F_TERMINADO_ACEPTADO` / `RECHAZADO`…)? Con las fases elegidas, la mayoría de los estados ya están decididos; solo queda parametrizar y preguntar por lo que se desvía del catálogo.
- **Estados (el detalle que las fases no deciden):** ¿hay **subsanación** (el centro pide corregir y la solicitud vuelve)? ¿puede el solicitante **desistir** antes de la resolución? ¿la resolución es total o **parcial**? ¿hay **plazos** de presentación — los valida la aplicación (y entonces ¿dónde se configuran?) o solo se avisa al usuario?
- **Documentos y firmas:** si hay **impreso oficial, se usa ese PDF tal cual** (no se rediseña): preguntar de dónde se obtiene y qué campos del expediente rellenan cada hueco; el formato propio solo es para documentos sin impreso oficial. Por cada firma: ¿quién, qué relación tiene con el expediente y —si firma en pantalla— **tiene esa persona certificado digital** (si el solicitante tipo no lo tiene, hay que decidir cómo presenta)? ¿La resolución se emite por **registro de salida** y se **notifica por correo**? ¿A alguien más (el representante legal de un menor)?
- **Datos iniciales:** ¿existen ya los catálogos que los selectores necesitan? ¿qué perfiles nuevos hay que dar de alta?
- **Quién inicia:** ¿lo inicia el interesado (o su representante) o lo inicia **el centro de oficio** (una anulación por inasistencia, una amonestación)? Si es de oficio: el creador es un cargo del centro, el alumno es la persona interesada, y suele haber **trámite de audiencia** — un estado en que el turno pasa al interesado para alegar, con **plazo** que al vencer avanza solo.
- **Variantes del trámite:** ¿hay modalidades según régimen o colectivo (anulación completa vs por módulos solo para semipresencial)? ¿son trámites distintos o uno con una rama condicionada — y qué validación impide la variante a quien no le aplica?
- **Plazos y silencio administrativo:** además del plazo de presentación, ¿hay **plazo de resolución** con silencio administrativo (estimatorio o desestimatorio)? Un silencio es una transición **automática** al vencer el plazo, con su resultado — preguntarlo siempre que la normativa fije "resolverá en el plazo de un mes".
- **Informes intermedios:** ¿la resolución exige **oír antes a otros** (equipo docente, departamento de orientación)? Cada informe previo es un posible estado con su perfil.
- **Pasos externos a la aplicación:** ¿el trámite exige gestiones en otra web u organismo (generar y pagar una **tasa** en el portal de la administración)? La aplicación **no reimplementa** el paso externo: la spec lo modela pidiendo el **justificante** como adjunto (y sus condiciones: bonificaciones por familia numerosa o discapacidad, con su acreditación también adjunta) y validando su presencia.
- **Dónde acaba el expediente:** si el resultado final tarda años o lo entrega otro organismo (un título que llega a los 2-3 años), ¿el expediente se cierra al completar la parte del centro y lo posterior (la recogida, el recurso de alzada ante la administración) queda **fuera de alcance** o es **otro trámite** enlazado?

## Reglas transversales

### Identificadores numerados

Historias, escenarios, transiciones y reglas llevan IDs estables para que el diseño pueda comprobar que **ninguno se pierde**: cada regla del diseño declara de qué IDs de la spec proviene, y cada test E2E de qué escenario.

| Elemento | Formato | Ámbito de numeración | Dónde vive |
|---|---|---|---|
| Historias de usuario | `HU-NNN` | Global a la spec | `specification.md` |
| Escenarios | `ESC-NNN` | Global a la spec (no por historia) | `specification.md` |
| Transiciones | `TR-NNN` | Global a la spec | `estados.md` |
| Validaciones | `VAL-TR-<NNN>-<NNN>` | **Por transición** (cada transición arranca su cuenta en `001`) | `estados.md`, en la ficha de su transición |
| Efectos de transición | `RN-TR-<NNN>-<NNN>` | **Por transición** | `estados.md`, en la ficha de su transición |
| Efectos de entrada a estado | `RN-<ESTADO>-NNN` | Por estado | `estados.md`, en la ficha del estado |
| Restricciones | `RES-<Entidad>-NNN` | Por entidad | `entity-<Entidad>.md` |
| Campos calculados | `CC-<Entidad>-NNN` | Por entidad | `entity-<Entidad>.md` |
| Reglas de UI | `RUI-<ESTADO>-<PERFIL>-NNN` (vista genérica: `RUI-<ESTADO>-GENERAL-NNN`) | **Por vista** | `vistas.md` |
| Firmas | `FIR-<documento>-NNN` | Por documento | `documento-<documento>.md` |

- El ámbito de numeración es **local**: para añadir un elemento nuevo basta mirar su propio ámbito (la ficha de la transición, el fichero de la entidad, la vista) y tomar el siguiente número libre.
- `<Entidad>` es el nombre del modelo en **PascalCase**, exactamente el del fichero `entity-<Entidad>.md`. `<documento>` es el slug **kebab-case** del fichero `documento-<slug>.md`. `<ESTADO>` y `<PERFIL>` son los nombres UPPER_SNAKE del vocabulario del trámite (ver más abajo).
- Numeración desde `001`, **tres dígitos**, sin huecos al crear.
- **Los IDs no se renumeran nunca.** Al borrar un elemento su número se conserva como hueco (no se reutiliza), para no romper la trazabilidad con un diseño ya generado. Si se renombra una entidad, un estado, un perfil o el slug de un documento, sus IDs cambian con él — el renombrado **MUST** propagarse a toda la spec y al diseño si existe.
- **MUST NOT** usar otra taxonomía de reglas ni otros prefijos que los de esta tabla.
- ✅ CORRECTO: `TR-004`, `VAL-TR-004-001`, `RN-TR-004-002`, `RN-PENDIENTE_FIRMAS-001`, `RES-ComisionServicio-002`, `RUI-ENTRADA_DATOS-CREADOR-001`, `RUI-ACEPTADO-GENERAL-001`, `FIR-solicitud-comision-001`
- ❌ INCORRECTO: `VAL-ComisionServicio-001` (las validaciones se anclan a la transición, no a la entidad), `VAL-TR-4-1` (sin tres dígitos), `TR-PRESENTAR` (la transición se numera, el evento es un atributo suyo), `RUI-EntradaDatos-Creador-001` (estado y perfil no en UPPER_SNAKE), `EF-TR-004-001` (prefijo inventado: los efectos usan `RN-`).

### El vocabulario del trámite: fases, estados, eventos y perfiles

Los nombres de **fases**, **estados**, **eventos** y **perfiles** se escriben en `UPPER_SNAKE` **ya en la spec** (`F_ENTRADA`, `F_ENTRADA_DATOS`, `PRESENTAR_AUTOFIRMA`, `CREADOR`): son el vocabulario compartido del trámite — los usan negocio, los escenarios, el diseño y la implementación por igual. Es la **única** excepción de nomenclatura técnica admitida junto al concepto de "campos editables" (ver `estados.md`). Cada estado lleva además un **título** en lenguaje natural, que es lo que el usuario ve en pantalla.

- Las fases se codifican `F_<NOMBRE>` y **todo estado empieza por el código de su fase** (`F_ENTRADA_DATOS` pertenece a `F_ENTRADA`); las reglas completas de nomenclatura (eventos únicos, pasos `PASO_*`, instanciar dos veces una fase) están en [`fases.md`](fases.md) §2.
- Todo lo demás sigue prohibido: nada de clases, FQN, XML, JPQL ni nombres de método.
- ✅ CORRECTO: `F_SALIDA_PENDIENTE_RESOLUCION` (título: "Pendiente de resolución")
- ❌ INCORRECTO: `PendienteResolucion`, `pendiente-resolucion` (no es UPPER_SNAKE), `PENDIENTE_RESOLUCION` (no lleva el prefijo de su fase)

### Lo que da la plataforma (no se especifica)

La plataforma de expedientes aporta de serie una infraestructura que la spec **MUST NOT** volver a describir; solo se mencionan las **desviaciones**:

- El **árbol "Crear un nuevo expediente"** y las **bandejas/listados** de expedientes (pendientes, abiertos, cerrados, búsqueda). No hay menús ni listados que especificar: del árbol solo se redacta el **texto de ayuda** del trámite (en el índice).
- La **cabecera** de toda vista: quién creó el expediente, el estado actual, la fecha del último estado y el acceso al **historial de estados** con los justificantes de registro de entrada/salida de cada paso.
- El **panel de errores de validación** al pie y el evento **`EXIT`** ("Salir") de las vistas de consulta.
- Los campos comunes de todo expediente: número de expediente, nombre, centro, estado, historial, **persona solicitante** y **persona interesada**. Se usan en la spec por su nombre de negocio pero no se redeclaran en los modelos.
- El **guardado**: no existe un botón "Guardar" genérico — los datos solo entran al expediente al disparar un evento (ver "Campos editables" en `estados.md`).

### Lenguaje de negocio

¿Lo entendería un supervisor del centro sin formación técnica? Si **no**, no va en la spec (con la única excepción del vocabulario del trámite, arriba). Aplica a **todos** los ficheros: ni el índice ni los modelos, estados, vistas o documentos llevan tipos de dato, FQN, JPQL, atributos XML ni nombres de método.

### Lo obvio pesa tanto como lo complicado

**CRITICAL — al redactar y al revisar, presta a lo trivial la MISMA atención que a lo sutil.** El mayor riesgo de una spec no son las reglas difíciles —esas reciben atención de sobra— sino las **obvias**: se dan por sobreentendidas y nadie las escribe. Un campo que no puede quedar vacío, el estado inicial, quién puede borrar el expediente y hasta cuándo, el escenario del camino feliz, el camino de rechazo además del de aceptación. La spec vale por lo que **no deja implícito**, no por lo ingeniosa que es.

- **MUST** declarar una regla aunque sea evidente: si "no hace falta decirla" porque se sobreentiende, es justo la que más se olvida.
- ✅ CORRECTO: junto a la validación sofisticada de que el total de gastos cuadra, declarar también que el motivo del viaje es obligatorio.
- ❌ INCORRECTO: resolver con brillantez el circuito de firmas y dejar sin escribir que el IBAN no puede quedar vacío "porque es evidente".

---

# El índice — `specification.md`

## El trámite

**Qué va:** qué es el trámite en una frase; **a quién va dirigido** (en qué rama del árbol de crear expedientes aparece: profesores, alumnos, tutores, dirección, administrativos o conserjes); si es **público o privado**; el **texto de ayuda** que verá el usuario en el árbol (redactado, admite formato); y si es la **primera versión** de un trámite nuevo o una **versión nueva** de uno existente (y en ese caso, qué cambia respecto a la anterior).

**Qué NO va:** rutas de código, nombres de carpeta, mecánica de versionado.

## Actores y perfiles

Dos cosas, ligadas:

1. **Los actores**: quiénes intervienen y qué papel juegan, como en cualquier spec.
2. **La tabla de perfiles**: el reparto de turnos de la tramitación. Una fila por perfil con: su nombre `UPPER_SNAKE` (`CREADOR` y `RESPONSABLE` ya existen en la plataforma; cualquier otro es **nuevo**), **quién lo recibe** en lenguaje de negocio (un cargo, un tipo de usuario, "quien crea el expediente"…), su **ámbito** (`individual` — cada uno ve solo sus expedientes; `centro`; o `departamento`) y si es nuevo (un perfil nuevo **MUST** aparecer también en "Recursos y datos iniciales").

La seguridad de un expediente se deriva de aquí y de la máquina: **quién puede crear** expedientes del trámite (los que reciben `CREADOR`), y en cada estado **quién tiene el turno** (el perfil dueño edita y dispara eventos; los demás perfiles con acceso solo consultan). No hay un apartado de seguridad aparte: si un rol no aparece en esta tabla, no ve el expediente.

## Fases

**Apartado obligatorio: aquí se compone la máquina a alto nivel.** La spec declara qué fases del catálogo [`fases.md`](fases.md) instancia (en orden: `F_INICIO` opcional → `F_ENTRADA` → `F_SALIDA` 0..N → `F_TERMINADO`), y por cada fase instanciada: su objetivo en este trámite, los estados que aporta, el valor de sus **parámetros** y sus **desviaciones** respecto al catálogo (con su motivo — las fases son guías y pueden modificarse si el negocio lo pide, pero nunca en silencio).

Especificar aquí bien las fases es lo que hace "sencillo" el resto: los estados, eventos, vistas y efectos estándar ya vienen decididos por el catálogo y `estados.md`/`vistas.md` solo tienen que instanciarlos (con los detalles propios del trámite). La máquina detallada de `estados.md` **MUST** ser consistente con este apartado: mismos estados, cada uno con el prefijo de su fase.

## Historias de usuario

Usando los actores, los perfiles y el vocabulario del trámite. Cada historia es un encabezado `## HU-NNN — Como [Actor] quiero [feature] para [motivo]` y, **debajo de cada historia, van sus escenarios** `ESC-NNN`. Cada historia tiene **al menos un escenario** y cada escenario pertenece a exactamente una historia. Los escenarios deben cubrir el camino feliz, los alternativos y los errores/excepciones — y, entre todos, **recorrer todas las transiciones** de la máquina (una transición que ningún escenario ejercita es una transición sin probar).

**CRITICAL — claridad, especificidad y explicitud.** Cada escenario debe ser **muy claro, específico y explícito**: nombra los datos concretos que se usan (no "un profesor" sino "el profesor con la cuenta `profesor1@mislata.es`"), el valor exacto que se introduce en cada campo, la acción precisa que dispara cada paso (el botón que se pulsa, en qué estado está el expediente) y la respuesta literal del sistema (el texto del mensaje, el estado resultante). Nada se deja implícito ni a interpretación.

**CRITICAL — formato y autosuficiencia.** Cada `ESC-NNN` se convierte en el diseño en un test E2E que se ejecuta contra una aplicación recién arrancada, **sin estado previo**. Por eso **cada escenario se escribe SIEMPRE como una lista de pasos numerados** (un paso por línea); **MUST NOT** escribirse como varias frases separadas por «;» en una sola línea. La secuencia es **completa, verificable y autosuficiente**:

1. Empieza con el actor **iniciando sesión** en la aplicación.
2. Sigue con la **preparación**: crear el expediente desde el árbol de trámites y hacerlo avanzar (con los actores que haga falta, cada uno iniciando sesión dentro del escenario) hasta el estado que se quiere probar. El único estado previo admisible es el descrito en "Recursos y datos iniciales".
3. Realiza la **acción que se prueba** (normalmente: pulsar el botón de un evento).
4. Termina con la **respuesta del sistema** (el mensaje, el estado resultante, el documento generado, el correo enviado).

Entre medias puede haber más pasos y **ramas condicionales** (*"si \<condición\> el sistema hace X; si no, hace Y"*). Un escenario con ramas puede dar lugar a más de un test en el diseño. Cuando un paso lo realiza **otro perfil** (el responsable resuelve lo que el creador presentó), el escenario lo dice explícitamente: cierra sesión, inicia sesión el otro usuario, entra al expediente desde su bandeja.

**CRITICAL — usuarios y centros reales de los datos de demo.** Cuando un escenario nombre un usuario o un centro, **MUST** usar siempre los que están en `src/main/resources/data-demo/input/` (`centros-demo.xml` y `usuarios-demo.xml`); **MUST NOT** inventar centros, cuentas, logins ni DNI. Léelos antes de redactar los escenarios. De ahí salen:

- Los **centros**: `CIPFP Mislata` (código `46019660`) y `CIPFP Batoi` (código `03012165`). Cuando un escenario necesite un segundo centro (pruebas multicentro), usa esos dos.
- Las **cuentas de cada tipo de usuario y cargo**, cuyo **login es su correo** (p. ej. `supervisor1@mislata.es`, `profesor1@mislata.es`, `secretario@batoi.es`), todas con contraseña **`demo1234`**.
- Los **DNI** reales de cada cuenta (el atributo `documento` del usuario). Cuando un escenario use el DNI de una persona —y en los expedientes se usa: la firma con AutoFirma exige firmar con el DNI del expediente—, **MUST** ser el DNI real de una cuenta de demo.

La **única** identidad admitida que no está en esos ficheros es el **administrador global** `admin` / `admin`. Solo se admite inventar valores que **no identifican** a ningún usuario ni centro.

**Formato** (idéntico al de cualquier spec):

```
## HU-001 — Como [Actor] quiero [feature] para [motivo]

- ESC-001 — <Nombre corto>:
  1. <El actor inicia sesión.>
  2. <Crea el expediente desde el árbol de trámites y lo hace avanzar hasta el estado a probar.>
  3. <Pulsa el botón del evento que se prueba.>
  4. <El sistema responde: mensaje, estado resultante, efectos visibles.>
```

- ✅ CORRECTO (cambio de turno explícito):
  ```
  - ESC-005 — Resolución con subsanación:
    1. El profesor con la cuenta profesor1@mislata.es inicia sesión.
    2. Crea un expediente del trámite, rellena el motivo «Viaje a Berlín» y pulsa «Siguiente».
    3. Pulsa «Firmar con AutoFirma y Presentar la solicitud» y firma; el expediente pasa a PENDIENTE_RESOLUCION.
    4. Cierra sesión. El secretario con la cuenta secretario@mislata.es inicia sesión y abre el expediente desde su bandeja.
    5. Elige «Subsanar datos», escribe «Faltan las fechas del viaje» y pulsa «Resolver el expediente».
    6. El expediente vuelve a ENTRADA_DATOS y el profesor ve el motivo de la disconformidad al abrirlo.
  ```
- ❌ INCORRECTO (presupone estado): un `ESC` cuyo primer paso es *"El secretario abre un expediente pendiente de resolución"* — faltan el login y toda la preparación (crear y presentar el expediente), así que el test E2E no podría llegar a ese estado.

**Qué NO va:** nombres de clase, capas, comandos de testing, pasos Given/When/Then (eso es del diseño).

## Modelos

Una **tabla índice** con una fila por modelo: el enlace a su `entity-<Nombre>.md`, el nombre y una línea de qué representa. La primera fila es siempre **el expediente**; debajo, sus entidades hija si las hay. Bajo la tabla, las **relaciones** en lenguaje de negocio (el expediente tiene N líneas de gasto que se borran con él…).

## Estados y transiciones

Un párrafo con el ciclo de vida en dos o tres frases (de qué estado a cuáles se puede llegar y cómo acaba), el enlace a `estados.md` y la imagen `estados.png`. El detalle vive en `estados.md`.

## Vistas

El enlace a `vistas.md` con una línea por estado: qué ve quien tiene el turno. El detalle vive en `vistas.md`.

## Documentos

Una **tabla índice** con una fila por documento PDF: el enlace a su `documento-<slug>.md`, el nombre y una línea de qué es y cuándo aparece. Si el trámite no genera ni recibe documentos, `*(no aplica)*`.

## Subsistemas utilizados

Una **tabla resumen** de qué subsistemas usa el trámite y dónde: una fila por subsistema (documentos PDF, registro de entrada, registro de salida, firma en servidor, firma con AutoFirma, portafirmas, correos…) con los IDs (`TR-`/`RN-`/`FIR-`) donde se usa. Es una **vista de conjunto que no añade información nueva**: todo lo que contiene ya está en las fichas de transiciones y documentos. Si discrepan, **mandan las fichas** y hay que corregir la tabla.

## Recursos y datos iniciales

Recursos estáticos y datos que deben precargarse: **perfiles nuevos** (todo perfil de la tabla de actores que no sea `CREADOR`/`RESPONSABLE`), catálogos, plantillas, certificados. Si no hay, `*(no aplica)*`. Este apartado es el **único estado previo** que los escenarios pueden presuponer.

## Fuera de alcance

Lo que el negocio decide **no** hacer.

---

# La máquina de estados — `estados.md`

El corazón de la spec. Título `# Estados y transiciones`. Tres secciones: la tabla de estados, la creación del expediente y las fichas de transición.

La máquina **se escribe completa** (estados y transiciones uno a uno: tiene que poder leerse sin el catálogo delante, y el código que se genere de ella solo sabe de estados), pero **no se inventa**: se deriva de las fases instanciadas en el índice ([`fases.md`](fases.md)) — los estados, eventos y efectos estándar los da la fase, y aquí se instancian con los campos, validaciones y documentos propios del trámite, más las desviaciones ya declaradas.

## Estados

Una **tabla** con una fila por estado:

| Columna | Contenido |
|---|---|
| Estado | El nombre `UPPER_SNAKE`, **con el prefijo de su fase** (`F_ENTRADA_DATOS`) |
| Título | Lo que ve el usuario ("Pendiente de resolución") |
| Fase | La fase instanciada a la que pertenece — redundante con el prefijo del nombre a propósito: sirve de comprobación contra el apartado "Fases" del índice |
| Perfil con el turno | El perfil dueño, o `—` si nadie lo tiene (estados de solo consulta o de espera) |
| Inicial / Cerrado | Exactamente **un** estado inicial; los cerrados son terminales (el expediente queda cerrado pero se puede consultar) |
| ¿Se puede borrar? | Si el estado ofrece el borrado del expediente (borra sin validar, previa confirmación). Lo habitual: solo el inicial |

Debajo de la tabla, una **ficha breve por estado** que lo necesite: qué significa estar en él y, si al **entrar** en el estado ocurre algo automáticamente —entre por la transición que entre—, sus **efectos de entrada** `RN-<ESTADO>-NNN` (mismos atributos que los efectos de transición, ver abajo). Ejemplo: *RN-PENDIENTE_FIRMAS-001 — Al entrar en el estado, poner el documento de la comisión a firmar a todos los firmantes pendientes (FIR-solicitud-comision-002…)*.

- **Un estado de espera** (el expediente aguarda un suceso externo: firmas de terceros, un plazo) se modela como un estado **sin perfil con el turno** cuya salida es una transición **automática**.
- Ningún estado no-cerrado puede quedar **sin salida**, y todo estado debe ser **alcanzable** desde el inicial.

## Creación del expediente

Qué ocurre cuando el usuario crea el expediente desde el árbol de trámites: en qué estado nace (el inicial) y qué campos deja **precargados** el sistema (derivados del usuario que lo crea: la persona solicitante e interesada, el DNI con el que se exigirá firmar, el centro…). La creación **no recibe datos del usuario**: los datos se rellenan después, en el estado inicial, y entran al expediente con su primer evento.

## Transición: `TR-NNN — <EVENTO>: <ORIGEN> → <DESTINO>`

Una ficha por cada transición de la máquina, con este encabezado. Si el mismo evento lleva a **destinos distintos según una condición** (resolver puede aceptar, rechazar o pedir subsanación), es **una sola ficha** con sus **ramas**: `TR-NNN — RESOLVER: PENDIENTE_RESOLUCION → ACEPTADO | RECHAZADO | ENTRADA_DATOS`, y dentro se declara la condición de cada rama. "Volver atrás" no existe de serie: si un estado permite volver, es una transición más.

Cada ficha lleva, en este orden (omitiendo la etiqueta que no aplique):

**Disparador:** quién o qué dispara la transición:

- `botón` — la dispara el **perfil con el turno** pulsando un botón de su vista (lo normal). Si pide confirmación, se indica aquí el texto de la pregunta.
- `automática` — la dispara un **suceso**, no una persona: *"cuando todos los firmantes del documento X lo han firmado en el portafirmas"*, *"cuando vence el plazo de subsanación"*. **MUST** nombrar el suceso concreto; una transición automática sin suceso definido es un error de la spec.

**Ramas:** solo si hay más de un destino: la condición en lenguaje de negocio que decide cada uno (*"si la resolución es ACEPTAR → ACEPTADO; si es RECHAZAR → RECHAZADO; si es SUBSANAR_DATOS → ENTRADA_DATOS"*).

**Campos editables:** la lista **cerrada** de campos que el usuario puede enviar al disparar esta transición, o `(ninguna — <motivo>)`. Es el equivalente del `AllowProperties` de las specs de sistemas y la única otra etiqueta técnica admitida.

- **Por qué (seguridad):** es una defensa **anti mass-assignment**: el motor de tramitación copia al expediente **solo** los campos declarados aquí; cualquier otro campo que llegue del cliente se ignora. Los campos calculados, los precargados en la creación y los rellenados en transiciones anteriores no se repiten: un campo que no está en la lista **no lo puede tocar** el usuario en esta transición, aunque la vista lo muestre.
- Las propiedades listadas **MUST** existir en los "Campos" de un `entity-*.md` (del expediente o de una hija), con nombres funcionales.
- Un campo calculado (`CC-`) **MUST NOT** aparecer nunca.
- Si la transición permite editar una **entidad hija** (añadir/quitar líneas de gasto), se declara como *"las líneas de gasto (alta, edición y borrado)"* — y sus comprobaciones van en las Validaciones de esta misma transición.
- ✅ CORRECTO: `**Campos editables:** motivo del viaje, fechas del viaje, destino`
- ❌ INCORRECTO: `**Campos editables:** todos los del expediente` (la lista es siempre cerrada y explícita)

**Validaciones:** las comprobaciones **bloqueantes** de esta transición, `VAL-TR-<NNN>-<NNN>`. Si fallan, la transición no ocurre y no cambia nada. **CRITICAL — empieza SIEMPRE por la obligatoriedad, campo a campo:** recorre una por una las propiedades de "Campos editables" y pregúntate *"¿puede quedar vacío?"*; la obligatoriedad es la validación que más se olvida, precisamente por obvia. Después recorre el catálogo `catalogos/catalogo-validaciones.md` (campo propio, entre campos, entre registros, de negocio). **El texto es la aserción; `condición` es la guardia** (cuándo se evalúa): si la guardia repite lo que afirma el texto, la validación nunca falla y sobra. Nota: la precondición de estado no se declara — la garantiza la propia máquina (el evento solo existe en su estado origen).

Atributos opcionales por validación: `condición` (la guardia), `mensaje` (el error que ve el usuario), `rama` (si solo aplica a una rama de la transición).

**Efectos:** lo que el sistema hace automáticamente al confirmarse la transición, `RN-TR-<NNN>-<NNN>`. No bloquean, no deciden: actúan. **REQUIRED — identificación:** recorre el catálogo `catalogos/catalogo-acciones-transicion.md`, que reúne las acciones estándar de la plataforma:

- **generar un documento** PDF a partir de los datos del expediente (referencia a su `documento-<slug>.md`);
- **presentarlo por registro de entrada** (el usuario presenta; el sistema guarda el resguardo sellado);
- **emitirlo por registro de salida** (el centro emite; el sistema guarda el documento sellado);
- **firmar** un documento (referencia a la ficha `FIR-` correspondiente del documento);
- **enviar un correo** (a quién, con qué contenido y con qué adjuntos);
- **calcular o fijar campos** del expediente;
- cualquier otra acción de negocio, en lenguaje natural.

Atributos por efecto: `fase` (**obligatorio**: `antes_de_commit` — misma transacción, si falla se revierte todo; `después_de_commit` — un fallo no revierte la transición), y opcionales `condición` y `rama`.

**Ejemplo de ficha completa:**

```
## TR-004 — RESOLVER: PENDIENTE_RESOLUCION → ACEPTADO | RECHAZADO | ENTRADA_DATOS

**Disparador:** botón, con confirmación: "¿Está seguro que desea resolver el expediente? No podrá deshacer esta acción"

**Ramas:** si la resolución es ACEPTAR → ACEPTADO; si es RECHAZAR → RECHAZADO; si es SUBSANAR_DATOS → ENTRADA_DATOS

**Campos editables:** tipo de resolución, motivo del rechazo, datos a subsanar

**Validaciones:**

- VAL-TR-004-001 — El tipo de resolución está indicado
  - mensaje: "Debe elegir una resolución"
- VAL-TR-004-002 — El motivo del rechazo está indicado
  - condición: la resolución es RECHAZAR
  - mensaje: "El motivo del rechazo es obligatorio"

**Efectos:**

- RN-TR-004-001 — Generar el documento de resolución (documento-resolucion) y emitirlo por registro de salida
  - fase: antes_de_commit
  - rama: ACEPTADO y RECHAZADO
- RN-TR-004-002 — Enviar al solicitante un correo con la resolución adjunta
  - fase: después_de_commit
  - rama: ACEPTADO y RECHAZADO
```

## El diagrama — `estados.puml` y `estados.png`

Un diagrama de estados PlantUML, dibujado **antes** de escribir las fichas y mantenido coherente con ellas (si discrepan, manda el texto). Convenciones:

- `[*] --> <INICIAL>` para el estado inicial; `A --> B : EVENTO` por transición; guardas para las ramas (`RESOLVER[resolución=ACEPTAR]`).
- Los estados cerrados se anotan `<estado> : closed`; **MUST NOT** usar `--> [*]` para ellos (en estos diagramas `[*]` como destino significa borrado físico del expediente).
- Las transiciones automáticas se etiquetan con su suceso entre paréntesis: `PENDIENTE_FIRMAS --> FIRMADO : (todas las firmas recogidas)`.

`estados.png` **MUST** renderizarse siempre desde el `.puml` (ver "Render de los diagramas" al final), nunca dibujarse a mano.

---

# Los ficheros de modelo — `entity-<Nombre>.md`

Un fichero por cada modelo de la tabla "Modelos" del índice: el **expediente** y, si las hay, sus **entidades hija**. Título `# Modelo: <Nombre>`.

## Descripción

Qué representa, en lenguaje de negocio. El expediente no repite su ciclo de vida (vive en `estados.md`); una hija dice de quién cuelga y si se borra con su padre.

## Campos

Los campos **funcionalmente relevantes**, uno por viñeta, con su nombre conceptual, qué representa y **cuándo se rellena**: en qué transición lo aporta el usuario (debe cuadrar con los "Campos editables" de esa `TR-`), si lo precarga la creación, o si lo fija el sistema (→ probablemente sea un `CC-` o el resultado de un efecto `RN-`).

- Los **campos comunes** de todo expediente (número, centro, estado, solicitante, interesado, historial) **MUST NOT** redeclararse: se usan por su nombre cuando hagan falta.
- Cada **documento PDF que el expediente guarda** es un campo más, con referencia a su ficha: *"el documento de la solicitud (ver documento-solicitud-comision), en sus versiones original, firmada y sellada por el registro"*. Un documento que se firma con AutoFirma necesita el par original/firmado; uno que pasa por registro, además su versión sellada.
- **Un campo NO declara "obligatorio" ni "editable"** — la obligatoriedad es una `RES-` (si debe cumplirse siempre) o una `VAL-` de la transición correspondiente; la editabilidad la gobiernan los "Campos editables" de cada transición.
- Los **valores de un enum** sin ciclo de vida van en la descripción del campo. (El campo de estado no existe aquí: es común.)

**Qué NO va:** tipos de campo, campos técnicos, anotaciones.

## Restricciones

Invariantes de la entidad: condiciones que deben cumplirse **siempre**, se dispare la transición que se dispare. `RES-<Entidad>-NNN`. Regla de clasificación: si la condición debe cumplirse en toda transición, es una restricción; si solo al disparar una concreta, es una `VAL-` de esa transición. **REQUIRED:** recorre el catálogo `catalogos/catalogo-validaciones.md` campo a campo.

## Campos calculados

Valores que calcula el servidor, nunca los aporta el usuario. `CC-<Entidad>-NNN`, con los atributos `momento` (`lectura` | `escritura`), `sobreescribible` (`nunca` | lista de roles) y `cálculo` (en lenguaje de negocio). Un `CC-` **MUST NOT** aparecer en los "Campos editables" de ninguna transición. **REQUIRED:** recorre el catálogo `catalogos/catalogo-campos-calculados.md`.

*(A diferencia de la plantilla de sistemas, aquí NO hay secciones "Acción:" ni "Estados y transiciones": las validaciones y los campos editables viven en las fichas `TR-` de `estados.md`, y el ciclo de vida es la propia máquina.)*

## El diagrama de clases — `model.puml` y `model.png`

Un único `model.puml` por spec con el expediente, sus enums y sus entidades hija, y su `model.png` renderizado. Mismas reglas que en cualquier spec: campos sin tipo, sin campos técnicos ni métodos; composición (`*--`) para las hijas que se borran con el padre; enums enlazados con `-->`; multiplicidades entre comillas. No añade información nueva: si discrepa del texto, manda el texto. Se regenera cada vez que cambia cualquier entidad o relación.

---

# Las vistas — `vistas.md`

**Un único fichero** para todas las vistas del expediente (espejo de cómo se implementan). Título `# Vistas`. La estructura es fija: primero el **almacén de paneles**, luego **una sección por estado** con sus (normalmente dos) vistas.

## El modelo de vistas de un expediente

No hay listados ni menús que especificar (los da la plataforma). Lo que existe es, **por cada estado**:

- La **vista del perfil con el turno**: la que ve quien puede actuar. Muestra los paneles que le tocan (los suyos editables, los del pasado en lectura) y los **botones** de sus eventos.
- La **vista genérica**: la que ve cualquier otro perfil con acceso al expediente. Todo en lectura y un único botón "Salir" (que es de la plataforma y no se declara).

Un estado **sin** perfil con el turno (cerrados, de espera) solo tiene vista genérica. **Toda combinación alcanzable necesita su vista** — un estado sin vista es un error.

## Paneles

Los paneles se declaran **una sola vez** en esta sección y las vistas los **referencian**: así un mismo panel (los datos del viaje) aparece editable en un estado y en lectura en los demás sin duplicarlo. Una viñeta por panel: **nombre corto** en negrita (kebab-case, es su identificador dentro del fichero) y, tras un `—`, qué campos o contenido agrupa, en lenguaje de negocio. Tipos:

- **normal** — campos del expediente;
- **visor de documento** — muestra embebido un PDF del expediente (referencia al campo documento y a su `documento-*.md`);
- **maestro-detalle** — lista las entidades hija (las líneas de gasto) y abre el formulario de alta/edición de cada una; el **formulario de la hija** se describe como un panel más (los campos que pide);
- **ayuda** — un texto informativo.

Si la versión de **lectura** de un panel necesita mostrar otra cosa que la de edición (menos campos, otra disposición conceptual), se declaran dos paneles (`datos-viaje` y `datos-viaje-resumen`); si basta "lo mismo pero sin editar", es el mismo panel y cada vista indica el modo.

## Estado: `<ESTADO>`

Una sección por estado, en el orden de la tabla de `estados.md`. Dentro, una subsección por vista: `### Vista del perfil <PERFIL>` y `### Vista genérica`. Cada vista lleva:

- **Paneles (en orden):** la lista de paneles del almacén que muestra, cada uno con su modo (`edición` | `lectura`).
- **Botones:** una viñeta por botón: la **etiqueta** visible en negrita y, tras un `—`, la transición `TR-` que dispara. El botón de borrar (si el estado lo permite según `estados.md`) y el "Salir" de la genérica son estándar y no se enumeran; solo se declaran los botones de eventos. Un botón que **firma en pantalla** lo dice: *"**Firmar y presentar** — firma el documento de la solicitud (FIR-solicitud-comision-001) y dispara TR-003"*.
- **Pasos (opcional):** si la vista del turno es un **asistente por pasos** ([`fases.md`](fases.md) §6: sub-pantallas dentro del mismo estado — cambiar de paso no cambia el estado ni persiste nada), la tabla de pasos: cada `PASO_*` con su contenido y sus botones, distinguiendo los **de navegación** (llevan a otro paso; sin `TR-`) de los **de salida** (disparan un evento del estado). Los pasos se muestran/ocultan con las `RUI-` de esta misma vista.
- **Mensaje de ayuda (opcional):** el texto de ayuda que muestra la vista.
- **Reglas de UI:** las `RUI-<ESTADO>-<PERFIL>-NNN` (o `RUI-<ESTADO>-GENERAL-NNN`) de esta vista.

### Reglas de UI

Condiciones que cambian lo que se **ve** en una vista según el valor de un campo, el usuario o un evento de pantalla: mostrar/ocultar un campo o panel, marcar obligatorio visualmente, un valor por defecto. **Solo afectan a la pantalla — no bloquean ni escriben**: si bloquea es una `VAL-` (de su transición), si escribe es un `RN-`. **REQUIRED:** recorre el catálogo `catalogos/catalogo-reglas-ui.md` vista a vista. Atributos opcionales: `disparador`, `condición`, `actor`.

- ✅ CORRECTO: `RUI-ENTRADA_DATOS-CREADOR-001 — Los campos de hora solo se muestran si el tipo de jornada es parcial` (condición: tipo de jornada == JORNADA_PARCIAL)
- ❌ INCORRECTO: declarar como regla de UI que "el motivo es obligatorio al presentar" (eso es `VAL-` de la transición PRESENTAR; como mucho, la vista lo *marca* visualmente como obligatorio, y esa marca sí es una RUI espejo de la VAL).

**Qué NO va (en todo el fichero):** nombres técnicos de formularios o paneles del framework, atributos XML, condiciones en sintaxis de código.

---

# Los documentos — `documento-<slug>.md`

Un fichero por cada documento PDF de la tabla "Documentos" del índice. Título `# Documento: <Nombre>`. El documento es la **fuente de verdad de qué es y quién lo firma**; **cuándo** se genera, registra o envía lo dicen las transiciones (`RN-`), y aquí se referencia.

## Identidad

- **Qué es:** qué representa el documento y para qué sirve.
- **Ciclo:** en qué transición se genera (referencia a su `RN-TR-`), en qué campo del expediente se guarda cada versión (original / firmada / sellada), si pasa por **registro de entrada** (lo presenta el usuario) o **de salida** (lo emite el centro) y si se **envía por correo** a alguien — todo con referencia a los `RN-` correspondientes.

## Contenido

Primero, la **procedencia**: si el documento es un **impreso oficial** existente, se usa ese PDF tal cual — la ficha lo declara (`procedencia: impreso oficial`, con la referencia o URL de donde se obtiene) y entonces **no se especifican secciones**: se especifica el **mapeo**, qué dato del expediente rellena cada hueco del impreso. Solo los documentos **propios del centro** (`procedencia: propio`) describen sus secciones.

Para los propios: las **secciones** del documento en orden, y dentro de cada una sus campos o textos, en lenguaje de negocio: qué datos del expediente vuelca y qué textos fijos lleva. Los textos se redactan **en castellano**; la versión en valenciano se genera automáticamente y **MUST NOT** especificarse (solo se anota la excepción: un término que no debe traducirse). El título del documento, si no se dice otra cosa, es el nombre del trámite.

**Qué NO va:** maquetación (columnas, tamaños, tipografías) — es trabajo del diseño.

## Firmas

Una ficha `FIR-<slug>-NNN` por cada firma que el documento recibe, **en el orden en que se firman**. Cada ficha declara tres cosas, y de las dos primeras **se deriva** el resto:

1. **Firmante:** quién firma (un perfil, un cargo, una persona del expediente).
2. **Relación con el expediente** — el atributo que decide **dónde** se firma:
   - `tiene el turno` — el firmante es el perfil con el turno en el estado en que se firma → firma **en una vista del expediente** (un botón de firma en su vista).
   - `parte del expediente, sin el turno` — el firmante es parte del expediente pero no está actuando → se le da el turno para firmar: **un estado propio** cuyo perfil con el turno es el firmante, con su vista de firma (firmará cuando entre al expediente).
   - `ajeno al expediente` — el firmante no tiene relación con el expediente (el director que firma todas las comisiones sin conocerlas) → el documento **se envía al portafirmas** (subsistema de firmas); el expediente queda en un **estado de espera** y avanza con una transición **automática** cuando la firma se completa.
3. **Mecanismo** (solo cuando se firma en una vista del expediente): `en el servidor` — el certificado del firmante está en el servidor; la vista muestra el PDF y avisa de que se firmará en el servidor — o `AutoFirma` — la vista muestra el PDF y el botón lanza AutoFirma con el certificado del usuario (que debe corresponder al DNI del expediente). En el portafirmas **no se especifica**: el propio subsistema decide.

Cada ficha referencia el **estado y la transición** donde ocurre la firma (y a la inversa: la transición o el efecto que firma referencia su `FIR-`).

**Ejemplo:**

```
**Firmas:**

- FIR-solicitud-comision-001 — El profesor solicitante
  - relación: tiene el turno (estado PENDIENTE_PRESENTACION)
  - mecanismo: AutoFirma
  - dónde: botón «Firmar y presentar» de TR-003
- FIR-solicitud-comision-002 — El director
  - relación: ajeno al expediente
  - dónde: portafirmas, al entrar en PENDIENTE_FIRMA_DIRECTOR (RN-PENDIENTE_FIRMA_DIRECTOR-001); el expediente
    avanza con TR-006 (automática) cuando el director firma
```

**REQUIRED:** recorre el catálogo `catalogos/catalogo-firmas.md` — contiene el algoritmo de decisión y las preguntas que hay que hacerse por cada firma (¿quién?, ¿qué relación tiene?, ¿su certificado estará en el servidor?, ¿en qué orden firman?, ¿qué pasa si se niega a firmar?).

---

# Render de los diagramas

`estados.png` y `model.png` **MUST** obtenerse **siempre** renderizando su `.puml` con PlantUML, **nunca** dibujarse a mano. Se (re)renderizan cada vez que su `.puml` se crea o cambia. En la carpeta de la iniciativa:

```bash
PLANTUML_JAR=$(find ~/.m2/repository/net/sourceforge/plantuml/plantuml \
  -name 'plantuml-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | sort -V | tail -1)
java -Djava.awt.headless=true -Djava.io.tmpdir="${TMPDIR:-/tmp}" \
  -jar "$PLANTUML_JAR" -tpng estados.puml model.puml
```

- **CRITICAL — `-Djava.awt.headless=true`** es obligatorio: sin él PlantUML aborta buscando un servidor X11.
- **CRITICAL — `-Djava.io.tmpdir="${TMPDIR:-/tmp}"`** es obligatorio: ImageIO escribe caché temporal y `/tmp` puede estar en solo lectura (sandbox).
- Si PlantUML no está disponible o el render falla, **MUST NOT** fallar en silencio: conserva los `.puml` y avisa al usuario de que faltan los `.png`.

---

# Barridos de completitud (subagentes)

Tras (re)generar el borrador de la spec, el skill lanza **subagentes de barrido** que buscan **candidatas que falten**, cada uno con su catálogo. Esta tabla **declara** los barridos de esta plantilla; el skill es agnóstico y lanza los que aquí figuren.

Los barridos se ejecutan por **etapas en orden** (las candidatas aceptadas de una etapa modifican la spec que leen las siguientes); dentro de una misma etapa, todos en paralelo:

- **Etapa A — cobertura**: primero, porque una HU, un ESC, un estado o una transición aceptados dan material nuevo al resto.
- **Etapa B — calidad y reglas**: sobre la spec ya completada con lo aceptado en A.

| Etapa | Barrido | Una instancia por cada… | Sobre qué piensa (iteración interna del subagente) | Catálogo |
|---|---|---|---|---|
| A | **maquina-estados** | toda la spec (**instancia única**) | Recorre la máquina: ¿todo estado es alcanzable y todo no-cerrado tiene salida? ¿hay camino de rechazo además del de aceptación, y de subsanación/vuelta atrás si el negocio lo pide? ¿cada estado tiene perfil con el turno o es deliberadamente de espera/consulta? ¿cada transición automática nombra su suceso? ¿el borrado está decidido en cada estado? ¿cada combinación alcanzable tiene su vista en `vistas.md`? Y contra las **fases**: ¿la máquina de `estados.md` cuadra con las fases instanciadas en el índice (mismos estados, prefijos correctos, parámetros obligatorios con valor)? ¿toda diferencia respecto al catálogo está declarada como desviación con su motivo? Propone estados, transiciones o correcciones de la máquina. | `catalogos/catalogo-estados-transiciones.md` + `fases.md` |
| A | **historias-escenarios** | toda la spec (**instancia única**) | Cruza perfiles, estados, transiciones, documentos y firmas contra las HU/ESC existentes: qué declarado no lo ejercita ningún escenario — **cada transición** (incluidas sus ramas y las automáticas) debe recorrerla al menos un ESC. Propone la HU o el ESC que falta **con sus pasos redactados** (numerados, concretos, con usuarios/centros de demo y cambios de turno explícitos). | `catalogos/catalogo-historias-escenarios.md` |
| B | **pasos-escenarios** | historia de usuario (`HU-NNN`) | ESC a ESC de su historia y paso a paso: granularidad (ningún paso agrupa varias acciones), pasos que faltan (login, creación del expediente, cambios de sesión entre perfiles, valores concretos, respuesta literal), autosuficiencia (nada presupone estado previo fuera de "Recursos y datos iniciales" y los datos de demo). | `catalogos/catalogo-pasos-escenario.md` |
| B | **validaciones-transicion** | transición (`TR-NNN`) | **Primero** recorre campo a campo los "Campos editables" y comprueba la **obligatoriedad** (¿puede quedar vacío? si no → candidata), la validación más olvidada. Después, campo a campo, recorre las tablas del catálogo. Clasifica: si debe cumplirse siempre → `RES-` (de la entidad); si se ancla a esta transición → `VAL-TR-`. Propone `condición`/`mensaje`/`rama`. | `catalogos/catalogo-validaciones.md` |
| B | **restricciones** | fichero `entity-*.md` | Campo a campo de la entidad: invariantes que deben cumplirse en toda transición (unicidad, coherencia entre campos, cardinalidad de hijas). | `catalogos/catalogo-validaciones.md` |
| B | **campos-calculados** | fichero `entity-*.md` | Campo a campo: cuáles fija el servidor y no el usuario (cruza con los "Campos editables" de todas las transiciones: un campo que nadie edita pero aparece en vistas/documentos/escenarios es candidato). Propone `momento`/`sobreescribible`/`cálculo`. | `catalogos/catalogo-campos-calculados.md` |
| B | **efectos-transicion** | transición (`TR-NNN`) | Por la transición y por la entrada a su(s) estado(s) destino: ¿debería generar un documento? ¿registrarlo (entrada si presenta el usuario, salida si emite el centro)? ¿firmarse algo? ¿avisar a alguien por correo? ¿fijar algún campo? Propone la `fase` y, si aplican, `condición`/`rama`; distingue efecto de transición (`RN-TR-`) de efecto de entrada al estado (`RN-<ESTADO>-`). | `catalogos/catalogo-acciones-transicion.md` |
| B | **firmas-documentos** | fichero `documento-*.md` | Por el ciclo del documento: ¿se genera en alguna transición y se guarda en un campo? ¿le falta el registro? Y firma a firma: ¿están todas las que el negocio exige? ¿cada una tiene firmante, relación y (si toca) mecanismo? ¿las de terceros tienen su estado de espera y su transición automática? ¿está decidido el orden y el "se niega a firmar"? | `catalogos/catalogo-firmas.md` |
| B | **reglas-ui** | vista de `vistas.md` (cada `### Vista …`) | Sus paneles, campos y botones, considerando el perfil que la ve y el estado: qué se muestra u oculta según valores, qué valores por defecto, qué marcas de obligatorio espejo de las `VAL-` de sus transiciones. | `catalogos/catalogo-reglas-ui.md` |

Reglas de los barridos:

- Cada subagente **propone candidatas, no escribe la spec**: las candidatas solo entran cuando el usuario las acepta en la conversación.
- Cada subagente recibe su **elemento asignado**, su catálogo y la carpeta completa de la spec (para el contexto). **MUST** leer lo ya declarado en toda la spec y **MUST NOT** proponer una candidata que duplique algo existente (mismo efecto sobre el mismo campo/transición/vista/escenario, aunque esté redactado distinto).
- El catálogo es **solo una guía no exhaustiva**: el subagente puede y debe proponer también candidatas fuera de catálogo si el negocio de la spec las sugiere (indicando `(fuera de catálogo)` como fila de origen).
- **CRITICAL — lo obvio cuenta igual que lo complejo.** **MUST** proponer también las candidatas obvias (un campo obligatorio, el camino de rechazo, el borrado del estado inicial, el escenario del camino feliz): lo fácil se olvida precisamente por fácil.
- Toda candidata va en **lenguaje de negocio** (con el vocabulario UPPER_SNAKE del trámite como única excepción). Los pasos de escenario propuestos cumplen las reglas de "Historias de usuario", incluidos los usuarios y centros de demo.
- Una candidata **debe deducirse de lo que la spec ya cuenta** — el subagente **MUST NOT** inventar funcionalidad nueva (campos, estados, documentos o firmas que la spec no sugiere). Los barridos de la etapa A proponen elementos nuevos solo para **cubrir lo ya declarado** (una transición sin escenario, un estado sin salida, un actor con perfil sin historia…), no funcionalidades nuevas.
- Respetar la frontera entre familias: si una candidata bloquea → `VAL-`/`RES-`; si actúa/escribe → `RN-`; si solo cambia lo que se ve → `RUI-`; si es un valor que fija el servidor → `CC-`; si es quién/cómo firma → `FIR-`. Ante la duda, proponerla una sola vez en la familia de su **efecto real**.
- El formato exacto de respuesta de cada subagente lo fija el skill `sdd-specification` en su fase de barrido, no esta guía.
