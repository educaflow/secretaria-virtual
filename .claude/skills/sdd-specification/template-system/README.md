# Guía de los ficheros de la especificación

Explica qué debe contener cada fichero de la especificación, cómo clasificar sus elementos y cómo numerarlos. Esta guía dirige la redacción y la revisión de la spec; **MUST NOT** copiarse ninguno de sus bloques explicativos al output.

Es **el único fichero de esta carpeta de plantillas que el skill `sdd-specification` conoce por nombre**: el skill lee este `README.md` y, a través de él, descubre y usa el resto. Por eso aquí se declara qué hay en la carpeta y cómo se usa cada cosa.

## Ficheros de esta carpeta de plantillas

| Fichero | Qué es | Cómo se usa |
|---|---|---|
| `README.md` | **Esta guía**: el conjunto de ficheros, los apartados de cada uno, la clasificación de los elementos y las reglas de numeración. | Es la única referencia que el skill conoce por nombre; dirige las preguntas de la Fase 2 y las validaciones de la Fase 3. **MUST NOT** copiarse al output. |
| `specification.md` | **La plantilla del índice.** | Se reproduce **literalmente**, sustituyendo los placeholders por contenido real. Produce **un** fichero índice (el único con frontmatter `type: specification`). |
| `entity.md` | **La plantilla de los ficheros de modelo.** | Se instancia tantas veces como modelos defina la spec, una por `entity-<Nombre>.md`. |
| `screen.md` | **La plantilla de los ficheros de pantalla.** | Se instancia tantas veces como pantallas defina la spec, una por `screen-<slug>.md`. |
| `catalogos/` | **Carpeta de catálogos de referencia**, uno por barrido: `catalogo-historias-escenarios.md` (cobertura de `HU-`/`ESC-`), `catalogo-pasos-escenario.md` (granularidad y autosuficiencia de los pasos), `catalogo-validaciones.md` (`VAL-` + `RES-`, catálogo único), `catalogo-reglas-negocio.md` (`RN-`), `catalogo-campos-calculados.md` (`CC-`), `catalogo-reglas-ui.md` (`RUI-`). | Se consultan al rellenar cada apartado y son la referencia de los **barridos de completitud** (ver esa sección al final). **MUST NOT** copiarse al output. |
| `example/` | **Carpeta con un ejemplo completo** de spec terminada e instanciada (índice + un `entity-*.md` por modelo + un `screen-*.md` por pantalla). | Referencia del aspecto final. **MUST NOT** copiarse su contenido al output. |

## Ficheros que produce la especificación

La especificación **no es un único fichero**: es un conjunto de ficheros dentro de la carpeta de la iniciativa.

| Fichero | Plantilla | Qué contiene                                                                                                                                                                                                                     |
|---|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `specification.md` | `specification.md` | El **índice**: objetivo, actores, historias de usuario con sus escenarios, las **tablas de enlaces** a los modelos y a las pantallas, seguridad, recursos y fuera de alcance. Es el único con frontmatter `type: specification`. |
| `entity-<Nombre>.md` | `entity.md` | Un fichero **por cada modelo**: su descripción, campos, estados, restricciones (`RES-`), campos calculados (`CC-`) y, por evento, validaciones (`VAL-`) y reglas de negocio (`RN-`).                                             |
| `screen-<slug>.md` | `screen.md` | Un fichero **por cada pantalla**: su identidad, menú, vistas, botones y reglas de UI (`RUI-`).                                                                                                                                   |
| `model.puml` | *(sin plantilla; PlantUML)* | Un **único** fichero por spec: el **diagrama de clases** en PlantUML de los modelos definidos en los `entity-*.md` y de sus relaciones. Es una vista de conjunto; no añade información nueva.                                       |
| `model.png` | *(sin plantilla; imagen)* | Un **único** fichero por spec: la **imagen** del diagrama, renderizada **siempre** a partir de `model.puml` (nunca a mano). Se (re)genera cada vez que se crea o cambia `model.puml`.                                                |

- `<Nombre>` del modelo va en **PascalCase** (p. ej. `entity-SolicitudCertificado.md`); `<slug>` de la pantalla en **kebab-case** (p. ej. `screen-mis-certificados.md`). `model.puml` y `model.png` son **nombres fijos** (uno por spec), sin sufijo variable.
- Solo `specification.md` lleva frontmatter. Los `entity-*.md` y `screen-*.md` empiezan directamente por su `# Modelo: …` / `# Pantalla: …`. `model.puml` empieza por `@startuml` y termina por `@enduml`.

---

## Reglas transversales

### Identificadores numerados

Historias, escenarios y reglas llevan IDs estables para que el diseño pueda comprobar que **ninguno se pierde**: cada regla del diseño declara de qué IDs de la spec proviene, y cada test E2E de qué escenario.

| Elemento | Formato | Ámbito de numeración | Dónde vive |
|---|---|---|---|
| Historias de usuario | `HU-NNN` | Global a la spec | `specification.md` |
| Escenarios | `ESC-NNN` | Global a la spec (no por historia) | `specification.md` |
| Restricciones | `RES-<Entidad>-NNN` | Por entidad | `entity-<Entidad>.md` |
| Validaciones | `VAL-<Entidad>-NNN` | Por entidad (no por acción: la cuenta continúa entre acciones) | `entity-<Entidad>.md` |
| Reglas de negocio | `RN-<Entidad>-NNN` | Por entidad | `entity-<Entidad>.md` |
| Reglas de UI | `RUI-<pantalla>-<vista>-NNN` | **Por vista** (cada vista arranca su cuenta en `001`) | `screen-<pantalla>.md` |
| Campos calculados | `CC-<Entidad>-NNN` | Por entidad | `entity-<Entidad>.md` |

- El ámbito de numeración es **local**: para añadir un elemento nuevo basta mirar su propio ámbito (el fichero de la entidad, o la sección `### Reglas de UI` de la vista) y tomar el siguiente número libre — **MUST NOT** hacer falta rastrear el resto de ficheros.
- `<Entidad>` es el nombre del modelo en **PascalCase**, exactamente el del fichero `entity-<Entidad>.md`.
- `<pantalla>` es el slug **kebab-case** del fichero `screen-<pantalla>.md`; `<vista>` es el **Slug** que declara la ficha de cada vista (ver «Vista: `<Nombre>`»).
- `HU-`/`ESC-` no llevan ámbito: viven solo en el índice y su numeración sí es global a la spec.
- Numeración desde `001`, **tres dígitos**, sin huecos al crear.
- **Los IDs no se renumeran nunca.** Al borrar un elemento su número se conserva como hueco (no se reutiliza), para no romper la trazabilidad con un diseño ya generado. Si se renombra una entidad, una pantalla o el slug de una vista, sus IDs cambian con ella — el renombrado **MUST** propagarse a toda la spec y al diseño si existe.
- **MUST NOT** usar otra taxonomía de reglas ni otros prefijos que los de esta tabla: la conversión a la taxonomía técnica de reglas es trabajo del diseño.
- ✅ CORRECTO: `HU-002`, `ESC-001`, `VAL-Adjunto-007`, `RN-Correo-012`, `RUI-correos-centro-formulario-001`
- ❌ INCORRECTO: `VAL-007` (sin entidad; la numeración por spec global ya no se usa), `VAL-adjunto-001` (entidad no en PascalCase), `VAL-Adjunto-1` (sin tres dígitos), `RUI-correos-centro-001` (falta el slug de la vista), `RUI-Correos-Centro-formulario-001` (pantalla no en kebab-case), `HU-Correo-001` (HU/ESC no llevan ámbito), `VALIDACION-Adjunto-001` (prefijo inventado, no es uno de la tabla), `ESC_001` (guión bajo), `HU-001-ESC-001` (el escenario no anida el ID de la historia; la pertenencia se expresa agrupándolo bajo ella).

### Lenguaje de negocio

¿Lo entendería un supervisor del centro sin formación técnica? Si **no**, no va en la spec. Cada apartado de esta guía indica qué SÍ y qué NO admite. Esto aplica **a los tres tipos de fichero**: ni el índice ni los ficheros de modelo ni los de pantalla llevan tipos de dato, FQN, JPQL, atributos XML ni nombres de método. La única excepción es el término `AllowProperties` (dentro de las acciones de cada modelo), que se nombra explícitamente como concepto de seguridad. Qué propiedad puede enviar la interfaz se expresa en lenguaje de negocio en ese apartado; el detalle técnico de cada campo es trabajo del diseño.

### Lo obvio pesa tanto como lo complicado

**CRITICAL — al redactar y al revisar, presta a lo trivial la MISMA atención que a lo sutil.** El mayor riesgo de una spec no son las reglas difíciles —esas reciben atención de sobra— sino las **obvias**: se dan por sobreentendidas y nadie las escribe. Un campo que no puede quedar vacío, un borrado en cascada evidente, un estado inicial, un filtro «solo veo lo mío», el escenario del camino feliz, el mensaje de error de lo que ya sabemos que falla. La spec vale por lo que **no deja implícito**, no por lo ingeniosa que es.

- **MUST** declarar una regla aunque sea evidente: si «no hace falta decirla» porque se sobreentiende, es justo la que más se olvida.
- **MUST NOT** saltarte una candidata trivial por obvia mientras resuelves con detalle las complejas — lo fácil se omite precisamente por fácil, y es tan necesario como lo difícil.
- ✅ CORRECTO: junto a las validaciones sofisticadas de permiso del padre de un adjunto, declarar también que su nombre y su contenido son obligatorios.
- ❌ INCORRECTO: resolver con brillantez las reglas de seguridad del padre y dejar sin escribir que el nombre del fichero no puede quedar vacío «porque es evidente».

---

# El índice — `specification.md`

## Objetivo

**Qué va:** una frase con lo que tiene que hacer; si es un **sistema** o un **subsistema**; las dependencias funcionales de subsistemas existentes, en lenguaje de negocio.

**Qué NO va:** rutas de código, nombres de paquete.

## Actores

Los actores que intervienen en la spec: quiénes son y qué papel juegan. (Los **modelos** ya no se describen aquí: cada uno tiene su `entity-*.md`; en el índice solo aparece la tabla de enlaces del apartado Modelos.)

## Historias de usuario

Usando los actores y el vocabulario de los modelos. Cada historia es un encabezado `## HU-NNN — Como [Actor] quiero [feature] para [motivo]` y, **debajo de cada historia, van sus escenarios** `ESC-NNN`: las secuencias de acciones que muestran cómo funciona esa historia. No hay un apartado de escenarios aparte — cada escenario vive bajo la historia a la que pertenece.

Cada historia tiene **al menos un escenario** y cada escenario pertenece a exactamente una historia (la que lo contiene). Los escenarios deben cubrir el camino feliz (happy path), los alternativos y los errores/excepciones.

**CRITICAL — claridad, especificidad y explicitud.** Cada escenario debe ser **muy claro, específico y explícito**: nombra los datos concretos que se usan (no «un alumno» sino «el alumno Juan Pérez»), el valor exacto que se introduce en cada campo, la acción precisa que dispara cada paso y la respuesta literal del sistema (el texto del mensaje, el estado resultante). Nada se deja implícito ni a interpretación: quien lo lea para construir el test E2E debe poder reproducir cada paso sin tener que adivinar ningún dato, condición ni resultado.

**CRITICAL — formato y autosuficiencia.** Cada `ESC-NNN` se convierte en el diseño en un test E2E que se ejecuta contra una aplicación recién arrancada, **sin estado previo**. Por eso **cada escenario se escribe SIEMPRE como una lista de pasos numerados** (un paso por línea); **MUST NOT** escribirse como varias frases separadas por «;» en una sola línea, aunque el escenario sea simple. La secuencia es **completa, verificable y autosuficiente**:

1. Empieza con el actor **iniciando sesión** en la aplicación.
2. Sigue con la **preparación**: el actor (u otro actor que también inicia sesión dentro del escenario) crea todos los datos que la prueba necesita, hasta llegar al estado que se quiere probar. El único estado previo admisible es el descrito en "Recursos y datos iniciales".
3. Realiza la **acción que se prueba**.
4. Termina con la **respuesta del sistema**.

Entre medias puede haber más pasos y **ramas condicionales** (*"si \<condición\> el sistema hace X; si no, hace Y y no hace Z"*). Un escenario con ramas puede dar lugar a **más de un test** en el diseño (uno por rama).

**CRITICAL — usuarios y centros reales de los datos de demo.** Cuando un escenario nombre un usuario (para iniciar sesión, como destinatario, etc.) o un centro, **MUST** usar siempre los que están en `src/main/resources/data-demo/input/` (`centros-demo.xml` y `usuarios-demo.xml`); **MUST NOT** inventar centros, cuentas, logins ni DNI. Léelos antes de redactar los escenarios. De ahí salen:

- Los **centros**: `CIPFP Mislata` (código `46019660`) y `CIPFP Batoi` (código `03012165`). Cuando un escenario necesite un **segundo centro** (pruebas multicentro), usa esos dos.
- Las **cuentas de cada tipo de usuario y cargo**, cuyo **login es su correo** (p. ej. `supervisor1@mislata.es`, `alumno1@mislata.es`, `secretario@batoi.es`), todas con contraseña **`demo1234`**.
- Los **DNI** reales de cada cuenta (el atributo `documento` del usuario; p. ej. `86862719E` para `alumno1@mislata.es`). Cuando un escenario use el DNI de una persona, **MUST** ser el DNI real de una cuenta de demo, no uno inventado.

La **única** identidad admitida que no está en esos ficheros es el **administrador global** `admin` / `admin` (login `admin`, contraseña `admin`) para el actor Administrador, porque en los datos de demo no hay un administrador por centro. Solo se admite inventar valores que **no identifican** a ningún usuario ni centro (p. ej. una dirección de correo deliberadamente inválida para forzar un error de envío).

**Formato:**

```
## HU-001 — Como [Actor] quiero [feature] para [motivo]

- ESC-001 — <Nombre corto>:
  1. <El actor inicia sesión.>
  2. <Prepara los datos que necesita la prueba.>
  3. <Realiza la acción que se prueba.>
  4. <El sistema responde.>
- ESC-002 — <Nombre corto>:
  1. <El actor inicia sesión.>
  2. <Prepara los datos que necesita la prueba.>
  3. <Realiza la acción que se prueba.>
  4. Si <condición>: <el sistema hace esto>.
  5. Si no: <el sistema hace esto otro y no hace aquello>.

## HU-002 — Como [Actor] quiero [feature] para [motivo]

- ESC-003 — <Nombre corto>:
  1. …
```

- ✅ CORRECTO (simple, pero igualmente en pasos numerados):
  ```
  - ESC-004 — Rechazo sin motivo:
    1. El secretario inicia sesión.
    2. Crea una solicitud de certificado para un alumno y la envía.
    3. Abre la solicitud recibida y pulsa Rechazar sin indicar motivo.
    4. El sistema muestra «El motivo es obligatorio».
  ```
- ✅ CORRECTO (con rama condicional):
  ```
  - ESC-007 — Emisión con y sin email del alumno:
    1. El alumno inicia sesión, crea una solicitud de «Certificado de matrícula» y la envía.
    2. El secretario inicia sesión, abre la solicitud pendiente y pulsa Emitir.
    3. El sistema comprueba que está PENDIENTE, genera el documento a partir de la plantilla y pasa la solicitud a EMITIDA.
    4. Si el alumno tiene email registrado: el sistema le envía un correo con el certificado adjunto.
    5. Si no tiene email: el sistema no envía ningún correo y la solicitud queda EMITIDA igualmente.
  ```
- ❌ INCORRECTO (varias frases en una sola línea): *"ESC-004 — Rechazo sin motivo: el secretario inicia sesión; crea una solicitud y la envía; abre la solicitud y pulsa Rechazar sin motivo; el sistema muestra «El motivo es obligatorio»."* (los pasos **MUST** ir uno por línea en una lista numerada, no como frases separadas por «;»).
- ❌ INCORRECTO (presupone estado): un `ESC` cuyo primer paso es *"El secretario abre una solicitud pendiente y la rechaza sin motivo"* presupone que ya existe esa solicitud y que hay sesión iniciada — faltan los pasos de login y preparación, así que el test E2E no podría llegar a ese estado.

**Qué NO va:** nombres de clase, pantallas técnicas, capas; en los escenarios, además, nombres técnicos de botón/campo/método, comandos de testing y pasos Given/When/Then (eso es del diseño).

## Modelos

Una **tabla índice** con una fila por modelo: el enlace a su `entity-<Nombre>.md`, el nombre del modelo y una línea de qué representa. Debajo de la tabla, las **relaciones entre modelos** en lenguaje de negocio (padre/hijo, borrado en cascada, referencias opcionales a entidades externas).

- Cada modelo que aparezca en la tabla **MUST** tener su `entity-<Nombre>.md`, y al revés.
- **Qué NO va:** tipos de campo, anotaciones JPA, FQN. El detalle de cada modelo vive en su fichero.

## Pantallas

Una **tabla índice** con una fila por pantalla: el enlace a su `screen-<slug>.md`, el nombre de la pantalla y una línea de para qué sirve y a quién.

- Cada pantalla que aparezca en la tabla **MUST** tener su `screen-<slug>.md`, y al revés.
- Una **pantalla** (una fila de la tabla, un `screen-*.md`) normalmente se compone de **varias vistas** que se abren unas a otras: como mínimo suele haber un **listado** y su **formulario** de detalle, y a menudo además los formularios de sus hijos maestro-detalle. En la tabla del índice va **una fila por pantalla**, no por vista; las vistas se detallan dentro del `screen-*.md`.
- Si varios puntos de entrada **distintos** abren el **mismo** formulario en modo detalle, ese formulario se describe **una sola vez** como una pantalla compartida (un único `screen-*.md`), y los demás lo referencian; así se evita duplicarlo.
- **Qué NO va:** nombres técnicos de acciones o vistas del framework, dominios JPQL.

## Seguridad

Quién puede ver/crear/editar/borrar cada cosa, en lenguaje natural:

- **Declarar solo los roles que tienen algún acceso** (tipos de usuario y cargos de `CLAUDE.md`). La seguridad es **deny by default**: un rol que no se declara no tiene acceso, así que **no se listan los roles "sin acceso"**.
- Por cada rol con acceso, indicar **qué puede ver/crear/editar/borrar** y su **alcance por centro**: si solo ve lo de su propio centro, todos los centros o solo sus propios registros. No hay un flag global de "multicentro" — el alcance por centro forma parte de la descripción de cada rol.
- Aun así, hay que **considerar todos los roles del proyecto** al redactar, para no olvidar **conceder** acceso a uno que sí lo necesita; las dudas se resuelven preguntando, no escribiendo líneas "sin acceso".

## Recursos y datos iniciales

Recursos estáticos que necesita la funcionalidad (plantillas PDF, esquemas XSD, certificados…) y datos que deben precargarse al arrancar (catálogos, registros iniciales). Si no hay, indicar `*(no aplica)*`.

Este apartado es el **único estado previo** que los escenarios pueden presuponer.

## Fuera de alcance

Lo que el negocio decide **no** hacer.

---

# Los ficheros de modelo — `entity-<Nombre>.md`

Un fichero por cada modelo de la tabla "Modelos" del índice. Describe el modelo **como concepto del dominio** y aloja sus restricciones, campos calculados, las propiedades editables por acción (`AllowProperties`), validaciones y reglas de negocio. El título es `# Modelo: <Nombre>`.

## Descripción

Párrafo inicial bajo el título: qué representa el modelo, qué papel juega, su ciclo de vida resumido y si extiende o reutiliza algo existente, en lenguaje de negocio.

## Campos

Los campos **funcionalmente relevantes**, uno por viñeta, con su nombre conceptual y qué representa.

**Un campo NO declara “obligatorio” ni “inmutable”** — esos no son atributos del campo, son reglas, y duplicarlos aquí entra en conflicto con su sección propia:

- **Obligatorio** es una regla: si debe cumplirse en todo evento es una **restricción** (`RES-`); si solo en un evento concreto es una **validación** (`VAL-`). No se marca en el campo.
- **Inmutable / no editable** lo gobierna **AllowProperties**: un campo que no aparezca en la línea `AllowProperties` de la acción `Modificar` no lo puede cambiar el cliente. No se marca en el campo.
- **Valores** de un campo de estado van en «Estados y transiciones»; los de otro enum sin ciclo de vida, en la propia descripción.

**Qué NO va:** tipos de campo, campos técnicos (IDs, FKs internas, auditoría, flags, versiones), anotaciones JPA. Qué campos puede enviar la interfaz se expresa en la línea `AllowProperties` de la acción correspondiente, no campo a campo aquí.

## Estados y transiciones

Solo si el modelo tiene ciclo de vida: su estado inicial, las transiciones (qué acción o circunstancia lleva de un estado a otro) y cuál es terminal. Si no tiene ciclo de vida, se omite la sección.

## Restricciones

**Qué son:** invariantes de una entidad. Condiciones que deben cumplirse siempre, independientemente de la acción que se ejecute. Si se viola una restricción, el objeto está en un estado inválido.

**Cómo se asocian:** a la entidad, no a una acción concreta.

**Regla de clasificación:** si la condición debe cumplirse en todas las acciones de la entidad, es una restricción. Si solo aplica a una acción concreta, es una validación.

**REQUIRED — identificación:** para no olvidar ninguna restricción, recorre el catálogo `catalogos/catalogo-validaciones.md` (sobre todo las tablas "entre registros" y "de negocio": unicidad, cardinalidad de hijos, inmutabilidad por estado) y comprueba, campo a campo, cuáles aplican siempre (→ restricción) y cuáles solo en una acción (→ validación). El catálogo es una ayuda **no exhaustiva**: si el negocio necesita una restricción que no aparece en él, decláralo igualmente.

**Ejemplo:**

**Restricciones:**

- RES-Expediente-001 — El número de expediente es único en el sistema
- RES-Expediente-002 — La fecha de cierre no puede ser anterior a la fecha de apertura

## Campos calculados

**Qué son:** valores de la entidad que el sistema calcula automáticamente. Nunca los proporciona el cliente; siempre los calcula el servidor.

**REQUIRED — identificación:** para no olvidar ningún campo calculado, recorre el catálogo `catalogos/catalogo-campos-calculados.md` y comprueba, campo a campo, cuáles no los aporta el usuario sino el servidor. El catálogo es **solo una guía no exhaustiva**: se pueden declarar campos calculados que no aparezcan en él.

**Atributos obligatorios:**

| Atributo | Valores | Descripción |
|---|---|---|
| `momento` | `lectura` \| `escritura` | Cuándo se calcula |
| `sobreescribible` | `nunca` \| lista de roles | Quién puede forzar un valor manual |
| `cálculo` | descripción en lenguaje de negocio | Cómo se obtiene el valor (la fórmula o regla de derivación) |

- `momento: lectura` → el valor se deriva en memoria cada vez que se lee la entidad. No se persiste.
- `momento: escritura` → el valor se calcula antes de persistir y se guarda en base de datos.

**Convención global:** un campo calculado nunca se acepta del cliente. Si el cliente envía un valor para un campo calculado, se ignora, salvo que el rol del usuario esté en la lista `sobreescribible`.

**Ejemplo:**

**Campos calculados:**

- CC-Pedido-001 — total
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: suma de (cantidad × precio_unitario) de todas las líneas
- CC-Pedido-002 — descuento_especial
  - momento: escritura
  - sobreescribible: [ADMIN]
  - cálculo: 0 por defecto; el administrador puede indicar un valor distinto

## Acción: `<NombreAcción>`

Un encabezado `## Acción: <Nombre>` por cada acción de la entidad (Crear, Modificar, Emitir, Rechazar, …) que tenga algo que declarar. Dentro de cada acción, en este orden, las etiquetas en negrita `**Input AllowProperties:**`, `**Validaciones:**` y `**Reglas de negocio:**` (omite la que no aplique). Cada validación o regla lleva una viñeta y sus atributos como **sub-viñetas** (`key: valor`).

**Cuándo existe una acción:** tiene encabezado si declara propiedades editables, validaciones o reglas de negocio. **Crear** y **Modificar** se declaran **SIEMPRE** (al menos su línea de `AllowProperties`, aunque sea `(ninguna)`); las demás acciones aparecen solo si reciben datos del formulario o tienen validaciones o reglas.

### Input AllowProperties

**Qué es:** la lista de propiedades que la interfaz puede enviar al servidor en esa acción, en una línea `**Input AllowProperties:** <propiedades>` o `(ninguna — <motivo>)`. `AllowProperties` es el único término técnico que se nombra en la spec.

**Por qué (seguridad):** es una defensa **anti mass-assignment**. Cualquier propiedad que llegue desde la interfaz y **no** esté en la lista se ignora: los campos calculados, de estado, de auditoría y los inmutables solo los fija el servidor. Sin esta lista, el cliente podría modificar campos internos que no le corresponden.

**Reglas de redacción:**

- **Crear (alta)** y **Modificar** llevan su línea de `AllowProperties` **SIEMPRE**, aunque sea `(ninguna — <motivo>)`. **CRITICAL**: no omitir ninguna de las dos.
- El resto de acciones lleva la línea **solo si recibe datos** del formulario; una acción que no recibe datos (p.ej. Emitir) **MUST NOT** incluir la línea.
- Un campo **calculado (`CC-`)** o **inmutable** no aparece en la acción que no lo permite: un campo inmutable va en **Crear** pero **MUST NOT** ir en **Modificar**; un `CC` con `sobreescribible: nunca` **MUST NOT** ir en ninguna acción.
- Las propiedades listadas **MUST** existir en "Campos" o "Campos calculados" de la entidad.
- Lenguaje de negocio: nombres funcionales de propiedad, no tipos ni atributos técnicos.
- **MUST NOT** usar `todas`: la lista es siempre cerrada (propiedades explícitas o `(ninguna)`).
- **CRITICAL — referencia al padre en un alta dentro de un formulario hijo maestro-detalle.** Si la entidad se da de alta **dentro del formulario de su padre** (el formulario del hijo se abre embebido en un panel maestro-detalle del padre — ver «Una pantalla casi siempre tiene varias vistas»), la **referencia al padre SÍ va en `AllowProperties` de Crear**: la interfaz la rellena con el registro padre y la envía, porque el servidor que procesa el alta del hijo no puede deducir por su cuenta cuál es el padre. Al convertirse en un dato que dicta el cliente, **MUST** acompañarse **siempre** de las otras dos piezas: (a) una **regla de UI** que fije esa referencia desde el padre (ver «Reglas de UI») y (b) una **validación** que compruebe el padre recibido (ver «Validaciones»). La regla de UI **no es la defensa** —un cliente puede saltársela—; la defensa es la validación.

### Validaciones

**Qué son:** comprobaciones bloqueantes asociadas a una acción concreta. Si fallan, la acción se cancela y no ocurre ningún cambio en el sistema. En la spec basta con clasificarlas bien; el pipeline las materializa más adelante.

**Cómo se asocian:** a una acción de una entidad.

**REQUIRED — identificación:** para identificar qué validaciones aplican a cada campo, recorre el catálogo de tipos de validación `catalogos/catalogo-validaciones.md` (campo propio, entre campos del mismo registro, entre registros, de negocio); sus columnas de mensaje sirven de guía para redactar el `mensaje` en lenguaje de negocio. El catálogo es una ayuda **no exhaustiva**: si el negocio necesita una validación que no aparece en él, declárala igualmente.

**CRITICAL — empieza SIEMPRE por la obligatoriedad, campo a campo.** La obligatoriedad es la validación que más se olvida, precisamente por obvia. Antes que nada, recorre **una por una** las propiedades de la línea `AllowProperties` de cada acción (los campos que el usuario rellena) y pregúntate de forma explícita: *«¿puede quedar vacío este campo?»*. Si la respuesta es no, declara su obligatoriedad (`RES-` si debe cumplirse siempre, `VAL-` si solo en esa acción). **MUST NOT** darla por sobreentendida ni saltártela por trivial.
- ✅ CORRECTO: un adjunto se rellena con nombre de fichero y contenido → declara ambos obligatorios, no solo las validaciones «interesantes» del padre.
- ❌ INCORRECTO: declarar las validaciones de permiso/estado del padre y omitir que el nombre y el contenido no pueden quedar vacíos.

**REQUIRED — referencia al padre recibida del cliente:** si una propiedad que **referencia al padre** llega del cliente porque el alta ocurre dentro de un formulario hijo maestro-detalle (está en la línea `AllowProperties` de Crear por la regla anterior), declara validaciones que comprueben ese padre: que **está indicado**, que el usuario **tiene permiso sobre él** (su centro o su alcance), y que el **estado del padre admite** la operación. Sin estas validaciones, un cliente manipulado podría apuntar a un padre ajeno (de otro centro, o ya cerrado) saltándose la regla de UI que lo rellena — el padre es un dato del cliente, no una verdad del servidor.

**REQUIRED — reflejo en la UI del hijo maestro-detalle:** las validaciones de una entidad que **se da de alta dentro del formulario de su padre** (un hijo maestro-detalle) no saltan al confirmar el formulario del hijo, sino al **guardar el padre**. Por eso, las que sean **factibles en el cliente** (obligatoriedad, formato, longitud, rango) **MUST** reflejarse **además** como reglas de UI en la vista del hijo (ver «Reglas de UI»), para dar feedback en el propio alta. La validación sigue siendo la defensa; la regla de UI solo adelanta el aviso. Las validaciones que necesitan mirar otros registros (unicidad, cardinalidad) **no** se reflejan como regla de UI.

**El texto es la aserción; `condición` es la guardia.** El **texto** de la validación es *lo que debe cumplirse* (la aserción que, si no se da, bloquea). El atributo `condición` es la **guardia**: *cuándo* se evalúa la validación. Son cosas distintas — si la guardia repite lo que ya afirma el texto, la validación nunca falla y sobra. Por eso una precondición de estado pura ("la solicitud está en estado PENDIENTE") va como **texto**, no como `condición`.

**Atributos opcionales:**

- `condición`: la guardia que decide **cuándo aplica** la validación, en lenguaje de negocio (un estado u otra circunstancia). Si no se cumple, la validación no se evalúa (no bloquea).
- `mensaje`: el error que se devuelve al cliente si falla.
- `actor`: el rol que dispara la acción (las validaciones pueden diferir por actor).

### Reglas de negocio

**Qué son:** operaciones que el sistema ejecuta automáticamente como reacción a una acción ya confirmada. No bloquean. No deciden si la acción ocurre. Solo actúan.

**REQUIRED — identificación:** para no olvidar ninguna regla de negocio, recorre el catálogo `catalogos/catalogo-reglas-negocio.md` acción a acción y transición a transición. El catálogo es **solo una guía no exhaustiva**: se pueden declarar reglas que no aparezcan en él.

**Atributo obligatorio** — `fase`:

- `antes_de_commit` → se ejecuta en la misma transacción. Si falla, hace rollback.
- `después_de_commit` → se ejecuta una vez confirmada la transacción. Un fallo no revierte la acción principal.

**Atributos opcionales:**

- `estado`: el estado de la entidad para que la regla aplique.
- `condición`: cualquier otra condición adicional.

**Ejemplo completo** (una acción que reúne las tres etiquetas, de SolicitudCertificado):

```
## Acción: Rechazar

**Input AllowProperties:** motivo de rechazo

**Validaciones:**

- VAL-SolicitudCertificado-004 — La solicitud está en estado PENDIENTE
  - mensaje: "Solo se pueden rechazar solicitudes pendientes"
- VAL-SolicitudCertificado-005 — El motivo de rechazo está indicado
  - mensaje: "El motivo es obligatorio"

**Reglas de negocio:**

- RN-SolicitudCertificado-005 — Enviar al alumno un correo con el motivo del rechazo
  - fase: después_de_commit
```

(`fecha de solicitud` es `CC` → nunca editable; `estado`, `fecha de resolución` y `documento emitido` los fija el servidor → fuera de toda línea de `AllowProperties`. La acción `Emitir` no recibe datos → no lleva línea de `AllowProperties`.)

---

# El diagrama de clases — `model.puml` y `model.png`

Un **único** fichero `model.puml` por spec con un diagrama de clases en **PlantUML** que representa visualmente los modelos definidos en los `entity-<Nombre>.md` y cómo se relacionan, más su imagen renderizada `model.png`. Es una **vista de conjunto**: de un vistazo se ven todas las entidades de la spec y sus relaciones.

**No añade información nueva.** Todo lo que contiene ya está en los `entity-*.md` (campos, estados) y en el apartado «Modelos» del índice (relaciones). Si el diagrama y el texto discrepan, **manda el texto** y hay que corregir el diagrama. Por eso `model.puml` (y su `model.png`) se **regeneran** cada vez que se crea la spec o cambia **cualquier entidad o relación** (un campo, un estado, una entidad nueva o borrada, una multiplicidad). En modo "refinar", si ninguna entidad ni relación cambió, **MUST NOT** tocarlos.

## Qué contiene

- Una **clase** por cada modelo de la tabla «Modelos» del índice (una por `entity-*.md`).
- Dentro de cada clase, sus **campos** funcionalmente relevantes (los de la sección «Campos» de su `entity-*.md`), uno por línea.
- Un **enum** por cada campo de estado con ciclo de vida, con sus valores (los de «Estados y transiciones»), enlazado a la clase que lo usa.
- Las **relaciones entre modelos** descritas bajo la tabla «Modelos» del índice: **composición** para el maestro-detalle con borrado en cascada, **asociación** para una referencia, con su **multiplicidad**.

## Qué NO va

- **Tipos de dato, FQN, anotaciones JPA, visibilidad técnica:** igual que en los `entity-*.md`, los campos van **sin tipo** — solo el nombre de negocio (PlantUML permite omitirlo: `+ tipo de certificado`).
- **Campos técnicos** (IDs, FKs internas, auditoría, versiones), que tampoco aparecen en los `entity-*.md`.
- **Métodos:** una entidad de la spec no declara métodos.

## Sintaxis y convenciones

- El fichero empieza por `@startuml` y termina por `@enduml`.
- Nombre de clase en **PascalCase**, idéntico al `<Nombre>` de su `entity-*.md`.
- Relaciones (la flecha sale del padre):
  - `*--` **composición** para maestro-detalle con borrado en cascada (los hijos no viven sin el padre);
  - `-->` **asociación** para una referencia a otra entidad independiente (opcional o no);
  - **multiplicidad** entre comillas en los extremos (`"1"`, `"0..*"`, `"many"`).
- Un enum se declara con `enum <Nombre>` y se enlaza con `-->` a la clase que lo usa.

**Ejemplo** (coherente con `entity-SolicitudCertificado.md`, `entity-AdjuntoSolicitud.md` y `entity-TipoCertificado.md` del ejemplo):

```plantuml
@startuml

class SolicitudCertificado {
  + alumno solicitante
  + tipo de certificado
  + fecha de solicitud
  + estado
  + motivo de rechazo
  + fecha de resolución
  + documento emitido
}

enum EstadoSolicitud {
  PENDIENTE
  EMITIDA
  RECHAZADA
}

class AdjuntoSolicitud {
  + documento
}

class TipoCertificado {
  + nombre
  + descripción
}

SolicitudCertificado --> EstadoSolicitud
SolicitudCertificado "1" *-- "0..*" AdjuntoSolicitud : adjuntos
SolicitudCertificado "many" --> "1" TipoCertificado : tipo
@enduml
```

## Render a `model.png`

`model.png` es la imagen de `model.puml` y **MUST** obtenerse **siempre** renderizando el `.puml` con PlantUML, **nunca** dibujarse a mano. Se (re)renderiza cada vez que `model.puml` se crea o cambia.

- En la **carpeta de la iniciativa** (donde está `model.puml`), resuelve la **última** versión del jar de PlantUML del repositorio Maven local (`~/.m2`, ya presente porque `EducaFlowBuildTools` lo usa) y renderiza:
  ```bash
  PLANTUML_JAR=$(find ~/.m2/repository/net/sourceforge/plantuml/plantuml \
    -name 'plantuml-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | sort -V | tail -1)
  java -Djava.awt.headless=true -Djava.io.tmpdir="${TMPDIR:-/tmp}" \
    -jar "$PLANTUML_JAR" -tpng model.puml
  ```
  - **CRITICAL — `-Djava.awt.headless=true`** es obligatorio: sin él PlantUML aborta buscando un servidor X11 (`Can't connect to X11 window server`).
  - **CRITICAL — `-Djava.io.tmpdir="${TMPDIR:-/tmp}"`** es obligatorio: ImageIO escribe caché temporal y `/tmp` puede estar en solo lectura (sandbox) → `Can't create cache file!`.
- Si PlantUML no está disponible (`PLANTUML_JAR` vacío) o el render falla, **MUST NOT** fallar en silencio: conserva `model.puml` y **avisa al usuario** de que falta `model.png`.

---

# Los ficheros de pantalla — `screen-<slug>.md`

Un fichero por cada pantalla de la tabla "Pantallas" del índice. Describe la pantalla en lenguaje de negocio y aloja sus reglas de UI. El título es `# Pantalla: <Nombre>`.

## Una pantalla casi siempre tiene varias vistas

Lo que el usuario percibe como **una pantalla** casi nunca es una sola vista. Lo **mínimo habitual** es un **listado** (grid) y el **formulario** de detalle que ese listado abre; y muy a menudo hay más vistas que se van **abriendo unas a otras**:

- un **listado** que, al pulsar una fila o con «Nuevo», abre el **formulario de detalle** de ese registro;
- un **formulario** con un panel **maestro-detalle**: ese panel es a su vez un **listado** (grid) de los hijos embebido en el formulario y, al pulsar una fila (o «Añadir»), abre el **formulario del hijo**;
- un **botón** que abre otra vista.

Por eso, al bajar por la jerarquía, las vistas **alternan listado y formulario**: un listado abre un formulario, ese formulario contiene un listado de hijos (el panel maestro-detalle), que abre el formulario del hijo, que contiene otro listado, y así sucesivamente (**grid → formulario → grid → formulario → …**).

Todas las vistas alcanzables **en línea** desde un mismo punto de entrada se describen en **un único** `screen-*.md` (no en uno por vista). Ejemplo: una pantalla «Ciclo» encadena listado de ciclos → formulario de ciclo → listado de cursos (panel «Cursos») → formulario de curso → listado de módulos (panel «Módulos») → formulario de módulo: seis vistas que alternan grid y formulario, **un solo** fichero de pantalla. La regla de «formulario compartido» sigue valiendo: si distintos puntos de entrada abren el **mismo** formulario, ese formulario es una pantalla compartida con su propio `screen-*.md`.

**CRITICAL — un nodo del árbol es un hijo maestro-detalle, NO un selector de campo.** Acota la regla anterior: «vista alcanzable en línea» se refiere a los **hijos maestro-detalle** (un panel que lista los hijos que *pertenecen* al registro y abre el formulario de cada hijo), no a los selectores de un campo. Distingue:

- **SÍ es un nodo del árbol** — un panel **maestro-detalle**: lista los registros hijos que pertenecen al registro padre y abre el formulario del hijo. En «Ciclo», los paneles «Cursos» y «Módulos» son nodos.
- **NO es un nodo del árbol** — el **selector de un campo que referencia otra entidad**: el desplegable o popup con el que el usuario **elige** un registro ya existente de *otra* entidad independiente (no lo crea ni lo edita aquí). Esa otra entidad tiene su propia pantalla en otro sitio; en esta solo aparece como **un campo más** de su panel, sin sección `## Vista` propia. En «Ciclo», los campos `familia profesional`, `grado` y `nivel` son selectores → **no** son nodos del árbol, solo campos.

Por eso un `screen-*.md` describe **siempre** sus vistas de la misma forma, haya una o varias: tras `Identidad` y `Menú`, una sección `## Estructura jerárquica de las vistas` con el árbol y luego una sección `## Vista: <Nombre>` por cada vista, cada una con su ficha breve y sus `### Paneles`, `### Botones` y `### Reglas de UI` (ver los ejemplos `screen-mis-solicitudes.md` y `screen-solicitudes-centro.md`). **No hay un formato aparte para el caso de una sola vista:** si la pantalla es un único formulario (un asistente, un formulario de configuración) o un grid suelto, el árbol es un único nodo y hay una sola sección `## Vista` — exactamente la misma estructura.

## Modelo CRUD de una pantalla de mantenimiento

**CRITICAL — la spec MUST NOT describir botones que contradigan el modelo CRUD de `/k-vistas`.** El par listado + formulario de una pantalla de mantenimiento reparte las operaciones CRUD de forma **fija**: cada operación tiene un hogar y **no se coloca en otro sitio**. Describe las pantallas respetando esta tabla:

| Operación | Dónde vive (fijo en `/k-vistas`) | Cómo se describe en la spec |
|---|---|---|
| **Crear (alta)** | Botón **«Nuevo»** de la barra superior del listado (en un hijo maestro-detalle, **«Añadir»**) | Un botón de barra superior en `### Botones` del listado |
| **Consultar (lista)** | El propio listado | La ficha y las `### Propiedades` del listado |
| **Consultar (detalle) / Modificar** | El **formulario**, que se abre al pulsar una fila del listado | `Al pulsar una fila abre` (listado) + `Modo` (formulario) |
| **Borrar** | Botón **«Borrar» del formulario** — **nunca** del listado | Estándar implícito; solo se menciona la **desviación** (que NO se pueda borrar) |
| **Cancelar** / **Guardar** | Botones **del formulario** | Estándar implícito |

**Reglas duras que se derivan de la tabla:**

- **El borrado y la edición son SIEMPRE acciones del formulario, NUNCA del listado.** El listado no borra ni edita en línea: abrir la fila lleva al formulario, y ahí están «Borrar» y el guardado. **MUST NOT** describir un botón de fila «Eliminar», «Borrar» o «Editar» en un listado.
- **El panel «Guardar + Cancelar + Borrar» es el estándar** que el diseño añade siempre al formulario. Por eso **no se enumera**; solo se enumeran los botones de **dominio** (Enviar, Emitir, Rechazar…) y las **desviaciones** del estándar (que no se pueda borrar, o que el formulario sea de solo lectura).
- **Los botones de grid son EXCEPCIONALES.** El único botón normal del listado es «Nuevo». Cualquier **otro** botón de la barra superior y **cualquier** botón de fila/columna (Descargar, Imprimir, Ver un documento…) **solo** se incluye si el usuario lo **pide explícitamente**, o si se le **pregunta explícitamente y lo acepta**. **MUST NOT** inventarlos.

## Identidad

- **Quién la usa:** los roles que ven o usan la pantalla y en qué modo cada uno.
- **Qué muestra:** qué presenta el conjunto, sobre qué modelo(s), con qué filtro en lenguaje natural y en qué modo (lectura/edición), incluidas las relaciones maestro-detalle inline si las hay.

## Menú

El menú que da entrada a la pantalla: dónde cuelga en la jerarquía y quién lo ve. El menú lleva a la **vista de entrada** (la raíz del árbol, normalmente el listado). Si la pantalla no cuelga de ningún menú sino que se abre desde otra pantalla (p. ej. un formulario compartido), indícalo así.

## Estructura jerárquica de las vistas

Siempre presente. Un árbol con las vistas que componen la pantalla y, **entre paréntesis, cómo se llega de cada vista padre a su hija**: al pulsar una fila del listado o con «Nuevo», desde un panel maestro-detalle concreto, al pulsar un botón. La raíz es la vista de entrada. Si la pantalla tiene una sola vista, el árbol es ese único nodo.

Recuerda que un panel **maestro-detalle** es a la vez un nodo del árbol (un **listado** de hijos) y un panel de la vista padre: por eso el árbol alterna listado y formulario.

```
Listado de ciclos
└── Formulario de ciclo   (se abre al pulsar una fila o con «Nuevo»)
    └── Listado de cursos   (panel maestro-detalle «Cursos» del formulario de ciclo)
        └── Formulario de curso   (se abre al pulsar una fila del listado de cursos o con «Añadir»)
            └── Listado de módulos   (panel maestro-detalle «Módulos» del formulario de curso)
                └── Formulario de módulo   (se abre al pulsar una fila del listado de módulos o con «Añadir»)
```

## Vista: `<Nombre>`

Una sección `## Vista: <Nombre>` por cada vista del árbol, en el mismo orden (al menos una). Toda vista empieza por la **misma ficha** y luego trae **solo las subsecciones propias de su tipo**: un **listado** (grid) y un **formulario** no se describen igual — un listado no tiene paneles, y un formulario no tiene columnas. La ficha común:

- **Slug:** identificador corto de la vista en **kebab-case**, **único dentro de la pantalla** (convención: `listado`, `formulario` para las vistas principales; `listado-<hijos>`, `formulario-<hijo>` para las de un maestro-detalle). Forma parte del ID de sus reglas de UI: `RUI-<pantalla>-<slug>-NNN`.
- **Tipo:** `listado` | `formulario` | `gráfica` | …
- **Qué muestra:** sobre qué modelo, con qué filtro en lenguaje natural y en qué modo (lectura/edición).
- **Se abre desde:** cómo se llega a esta vista:
  - un **formulario** se abre desde su listado: «el listado de cursos, al pulsar una fila o «Añadir»»;
  - un **listado embebido** (un panel maestro-detalle) no se abre, va dentro de su formulario padre: «embebido como panel «Cursos» en el formulario de ciclo»;
  - la **raíz** del árbol: «es la vista de entrada de la pantalla».

Tras la ficha, **usa solo las subsecciones del tipo de la vista**, como se detalla a continuación. La subsección `### Reglas de UI` es común a cualquier tipo y va siempre la última.

### Subsecciones de un listado (grid)

Un listado **no tiene paneles**. Lleva una subsección `### Propiedades` con, una por viñeta y en lenguaje de negocio:

- **Columnas (en orden):** los campos que se ven como columnas, en el orden en que aparecen.
- **Ordenación por defecto:** por qué campo y en qué sentido se ordenan las filas (o «sin orden definido»).
- **Búsqueda / filtros:** si el usuario puede filtrar la lista y por qué campos (o «no»).
- **Al pulsar una fila abre:** qué formulario abre el listado (o «no abre detalle»). Es la arista del árbol que sale de este listado.
- **Mensaje de ayuda (opcional):** un texto de ayuda que el listado muestra al usuario (p. ej. «Aquí se listan todos los ciclos que hay en el sistema»). Solo si lo hay; si no, se omite la viñeta.

Y una subsección `### Botones`. El **único botón normal** de un listado es **«Nuevo»** (o **«Añadir»** en un hijo maestro-detalle) en la **barra superior**: crea un registro. Todo lo demás es excepcional (ver «Modelo CRUD de una pantalla de mantenimiento»):

- **MUST NOT** un botón de fila «Eliminar», «Borrar» o «Editar»: el borrado y la edición son acciones del **formulario**, nunca del listado.
- Cualquier **otro** botón —de barra superior distinto de «Nuevo», o de **fila/columna** (`Descargar`, `Imprimir`, `Ver…`)— es **EXCEPCIONAL**: solo se incluye si el usuario lo **pide explícitamente** o si se le **pregunta y lo acepta**; nunca se inventa. Márcalo entre paréntesis como `(barra superior)` o `(acción de fila)`.

Si el listado solo permite crear, lista solo «Nuevo». Si tampoco permite crear, `*(sin botones)*`.

### Subsecciones de un formulario

Un formulario lleva `### Propiedades`, `### Paneles` y `### Botones`.

`### Propiedades`, una viñeta:

- **Modo:** cuándo el formulario es editable y cuándo es de solo lectura (p. ej. «editable en el alta; en detalle, solo lectura»). Si siempre es editable o siempre de solo lectura, dilo.
- **Mensaje de ayuda (opcional):** un texto de ayuda que el formulario muestra al usuario. Solo si lo hay; si no, se omite la viñeta.

`### Paneles` — los bloques visibles del formulario, uno por viñeta: el **título** del panel en negrita, su **tipo** entre paréntesis y, tras un `—`, qué campos o contenido agrupa, en lenguaje de negocio. Tipos de panel:

- **normal** — campos del propio modelo;
- **maestro-detalle → «<vista listado hija>»** — lista los hijos que pertenecen al registro y abre su formulario; es a la vez un nodo del árbol;
- **botonera** — solo botones (que se enumeran igualmente en `### Botones`).

Un campo que **referencia otra entidad** (el usuario elige un registro existente con un selector) se lista como **un campo más** del panel, no como una vista. Por defecto basta con el nombre del campo: el diseño ya sabe con qué vista se elige. **Solo si** hace falta una vista distinta de la por defecto, anótalo entre paréntesis en lenguaje de negocio (p. ej. `nivel (se elige del catálogo de niveles de grado superior)`).

`### Botones` — las acciones del formulario, una por viñeta: la etiqueta en negrita y, tras un `—`, qué acción de negocio dispara y cuándo es visible. El panel **«Guardar + Cancelar + Borrar» es el estándar** que el diseño añade siempre (por `/k-vistas`) y aquí el **borrado** tiene su hogar (nunca en el listado). Por eso **no se enumera** ese trío; solo se enumeran: (a) los botones de **dominio** (Enviar, Emitir, Rechazar, Aprobar…), y (b) las **desviaciones** del estándar (p. ej. «no se puede borrar» → sin Borrar; formulario de solo lectura → sin Guardar/Borrar, reflejado también en `Modo`). Si el formulario solo lleva el panel estándar, indícalo con `*(solo los botones estándar: Guardar, Cancelar, Borrar)*`.

### Subsecciones de una gráfica u otra vista no-formulario

Lleva solo `### Propiedades` describiendo sus **parámetros de entrada** y qué **representa** (qué datos agrega y cómo), en vez de paneles.

**Qué NO va (en cualquier tipo):** nombres técnicos de vista, de campo o de acción del framework, atributos XML, dominios JPQL.

### Reglas de UI

**Qué son:** condiciones que cambian el aspecto o el estado de un formulario en función del valor de uno o varios campos, del usuario actual, del registro padre o de un evento (al crear, al cargar, al cambiar un campo). Solo afectan a lo que **ve** y puede editar el usuario en pantalla — **no bloquean operaciones ni modifican el estado del sistema**.

**Cómo se asocian:** a una **vista** (un formulario), **no** a una entidad — por eso viven en el `screen-*.md`, no en el `entity-*.md`, en el `### Reglas de UI` de la vista a la que afectan. Si una misma conducta se necesita en otra vista, es otra regla de UI con su propio ID. El ID es `RUI-<pantalla>-<vista>-NNN` (pantalla = slug del fichero, vista = el `Slug` de su ficha) y la numeración es **por vista**: cada vista arranca su cuenta en `001`, así que para añadir una regla basta mirar el `### Reglas de UI` de esa vista.

**REQUIRED — identificación:** para no olvidar ninguna regla de UI, recorre el catálogo `catalogos/catalogo-reglas-ui.md` vista a vista (campos, paneles y botones, considerando roles y estados). El catálogo es **solo una guía no exhaustiva**: se pueden declarar reglas de UI que no aparezcan en él.

**Regla mnemotécnica para distinguirlas:**

- Validación → "no puedes" (bloquea la operación). Si la regla impide guardar, es una **validación** (va en el `entity-*.md`), no una regla de UI.
- Regla de negocio → "ahora hago esto" (escribe en BD o produce efectos colaterales). Si la regla escribe en BD o envía un correo, es una **regla de negocio** (va en el `entity-*.md`), no una regla de UI.
- Regla de UI → "ahora ves esto" (solo cambia el formulario).

**Atributos opcionales:**

- `disparador`: cuándo se aplica — un evento (al crear, al cargar, al cambiar un campo concreto) o `continuo` cuando se evalúa permanentemente.
- `condición`: la expresión que decide si el efecto se aplica. Si es `Siempre`, no hay condición.
- `actor`: el rol del usuario, cuando el efecto depende de quién mira la pantalla.

**Efectos típicos:** mostrar/ocultar un campo o panel, marcar un campo como solo lectura u obligatorio, fijar un valor por defecto al crear, filtrar las opciones de un campo relacional, cambiar el título de un campo.

**Convención de redacción:** describir qué **ve** el usuario, no cómo se implementa. Un valor por defecto al crear es una regla de UI (no una regla de negocio), porque no se escribe nada hasta que el usuario pulsa Guardar. Si una regla combina varios efectos sobre campos distintos, separarla en varias reglas de UI.

**CRITICAL — fijar el padre en el alta de un formulario hijo maestro-detalle.** Cuando un formulario es el **alta de un hijo embebido en el formulario de su padre** (un panel maestro-detalle), declara **siempre** una regla de UI que **fije la referencia al padre** con el registro padre desde el que se abre el formulario (un valor por defecto al crear). Es el origen del dato que viaja en la línea `AllowProperties` de Crear del hijo (ver «Input AllowProperties»). Recuerda que esta regla de UI **no es una defensa**: solo rellena el campo en pantalla; que ese padre sea legítimo lo garantiza la **validación** del hijo (ver «Validaciones»), no la regla de UI. Si el hijo cuelga de varios niveles (padre, abuelo…), declara una regla de UI por cada referencia que el formulario deba fijar.

**CRITICAL — reflejar como reglas de UI las validaciones factibles del alta de un hijo maestro-detalle.** El formulario de un hijo maestro-detalle se rellena **embebido** en el formulario de su padre y no se guarda solo: sus validaciones (`VAL-`/`RES-` del `entity-*.md` del hijo) **no se comprueban al confirmar el formulario del hijo, sino al guardar el padre** — el error llega tarde y referido al padre. Por eso, las validaciones del hijo que sean **factibles en el cliente** (obligatoriedad, formato, longitud, rango… lo comprobable sin mirar otros registros) **MUST** declararse **además** como reglas de UI en la vista del formulario del hijo (marcar el campo obligatorio, avisar del formato…), para que el usuario tenga feedback **en el propio alta** del hijo. Reglas:

- La regla de UI **no es la defensa**: solo adelanta el aviso en pantalla. La defensa real es la `VAL-`/`RES-` del hijo, que sigue saltando al guardar el padre. Toda regla de UI espejo **MUST** tener detrás su `VAL-`/`RES-` en el `entity-*.md` del hijo.
- Solo se reflejan las **factibles en el cliente**. Las que necesitan mirar otros registros (unicidad, cardinalidad de hijos, existencia de un registro) **MUST NOT** declararse como regla de UI: se quedan solo como `VAL-`/`RES-` y saltan al guardar el padre.
- ✅ CORRECTO: el adjunto exige nombre de fichero (`RES-`) → en el formulario del adjunto, una regla de UI que lo marca obligatorio al añadirlo.
- ❌ INCORRECTO: reflejar «no puede haber dos adjuntos con el mismo nombre» como regla de UI (unicidad entre registros: no es factible en el cliente; se queda solo como `RES-`).

**Ejemplo** (en `screen-expedientes.md`, dentro de la vista con `Slug: formulario`):

**Reglas de UI:**

- RUI-expedientes-formulario-001 — El campo motivo de rechazo solo se muestra cuando el estado es RECHAZADO
  - disparador: continuo
  - condición: estado == RECHAZADO
- RUI-expedientes-formulario-002 — Al crear un expediente, el centro se rellena con el centro del usuario actual
  - disparador: al crear
  - condición: Siempre
- RUI-expedientes-formulario-003 — El botón Publicar solo lo ve el administrador
  - disparador: al cargar
  - condición: Siempre
  - actor: [ADMIN]

---

## Ejemplo de pantalla de varias vistas

Esqueleto de un `screen-*.md` con seis vistas que **alternan listado y formulario** (maestro-detalle de dos niveles, como `Ciclo.xml`), abreviado. El árbol de «Estructura jerárquica de las vistas» va, en el fichero real, en su propio bloque de código:

````
# Pantalla: Ciclo

## Identidad
- **Quién la usa:** Administrador, en edición.
- **Qué muestra:** el listado de ciclos formativos y, al entrar en uno, su configuración con los cursos y, dentro de cada curso, sus módulos.

## Menú
- Configuración → Ciclos — lo ve el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas
Listado de ciclos
└── Formulario de ciclo   (se abre al pulsar una fila o con «Nuevo»)
    └── Listado de cursos   (panel maestro-detalle «Cursos» del formulario de ciclo)
        └── Formulario de curso   (se abre al pulsar una fila del listado de cursos o con «Añadir»)
            └── Listado de módulos   (panel maestro-detalle «Módulos» del formulario de curso)
                └── Formulario de módulo   (se abre al pulsar una fila del listado de módulos o con «Añadir»)

## Vista: Listado de ciclos
- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** los ciclos formativos, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.
### Propiedades
- **Columnas (en orden):** código, nombre, familia profesional
- **Ordenación por defecto:** por nombre, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de ciclo
- **Mensaje de ayuda (opcional):** «Aquí se listan todos los ciclos que hay en el sistema»
### Botones
- **Nuevo ciclo** (barra superior) — Abre el formulario de alta de un ciclo.

## Vista: Formulario de ciclo
- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos del ciclo, en edición.
- **Se abre desde:** el listado de ciclos, al pulsar una fila o «Nuevo».
### Propiedades
- **Modo:** editable.
### Paneles
- **Ciclo** (normal) — código, nombre, familia profesional, grado, nivel
- **Cursos** (maestro-detalle → «Listado de cursos») — los cursos del ciclo
### Botones
*(solo los botones estándar: Guardar, Cancelar, Borrar)*

## Vista: Listado de cursos
- **Slug:** listado-cursos
- **Tipo:** listado
- **Qué muestra:** los cursos del ciclo, en lectura.
- **Se abre desde:** embebido como panel «Cursos» en el formulario de ciclo.
### Propiedades
- **Columnas (en orden):** código, nombre, ley educativa
- **Ordenación por defecto:** por nombre, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de curso
### Botones
- **Añadir un nuevo curso** (barra superior) — Abre el formulario de alta de un curso.

## Vista: Formulario de curso
- **Slug:** formulario-curso
- **Tipo:** formulario
- **Qué muestra:** los datos de un curso del ciclo, en edición.
- **Se abre desde:** el listado de cursos, al pulsar una fila o «Añadir un nuevo curso».
### Propiedades
- **Modo:** editable.
### Paneles
- **Curso** (normal) — código, nombre, ley educativa
- **Módulos** (maestro-detalle → «Listado de módulos») — los módulos del curso
### Botones
*(solo los botones estándar: Guardar, Cancelar, Borrar)*

## Vista: Listado de módulos
- **Slug:** listado-modulos
- **Tipo:** listado
- **Qué muestra:** los módulos del curso, en lectura.
- **Se abre desde:** embebido como panel «Módulos» en el formulario de curso.
### Propiedades
- **Columnas (en orden):** módulo
- **Ordenación por defecto:** por nombre del módulo, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de módulo
### Botones
- **Añadir un nuevo módulo** (barra superior) — Abre el formulario de alta de un módulo.

## Vista: Formulario de módulo
- **Slug:** formulario-modulo
- **Tipo:** formulario
- **Qué muestra:** el módulo asociado al curso, en edición.
- **Se abre desde:** el listado de módulos, al pulsar una fila o «Añadir un nuevo módulo».
### Propiedades
- **Modo:** editable.
### Paneles
- **Módulo** (normal) — módulo
### Botones
*(solo los botones estándar: Guardar, Cancelar, Borrar)*
````

(Las reglas de UI, si las hubiera, van en el `### Reglas de UI` de cada vista, y **cada vista numera las suyas desde 001** con su propio slug: `RUI-ciclo-formulario-curso-001`, `RUI-ciclo-formulario-curso-002`, `RUI-ciclo-listado-001`, …)

---

# Barridos de completitud (subagentes)

Tras (re)generar el borrador de la spec, el skill lanza **subagentes de barrido** que buscan **candidatas que falten** (historias, escenarios, pasos y reglas), cada uno con su catálogo. Esta tabla **declara** los barridos de esta plantilla; el skill es agnóstico y lanza los que aquí figuren (otra plantilla puede declarar otros, o ninguno).

Los barridos se ejecutan por **etapas en orden** (las candidatas aceptadas de una etapa modifican la spec que leen las etapas siguientes); dentro de una misma etapa, todos en paralelo:

- **Etapa A — cobertura**: primero, porque una HU o un ESC aceptados dan material nuevo al resto de barridos.
- **Etapa B — calidad y reglas**: sobre la spec ya completada con lo aceptado en A.

| Etapa | Barrido | Una instancia por cada… | Sobre qué piensa (iteración interna del subagente) | Catálogo |
|---|---|---|---|---|
| A | **historias-escenarios** | toda la spec (**instancia única**) | Cruza Actores, Pantallas, acciones y estados de los `entity-*.md`, Seguridad y `VAL-`/`RUI-` contra las HU/ESC existentes: qué declarado no lo ejercita ningún escenario. Propone la HU o el ESC que falta **con sus pasos redactados** (conforme a las reglas de «Historias de usuario»: numerados, concretos, autosuficientes, con usuarios/centros de demo). | `catalogos/catalogo-historias-escenarios.md` |
| B | **pasos-escenarios** | historia de usuario (`HU-NNN`) del índice | ESC a ESC de su historia y, dentro de cada uno, paso a paso: (1) **granularidad** — ningún paso agrupa varias acciones consecutivas ni deja datos sin concretar; (2) **pasos que faltan** — inicio de sesión, preparación, valores concretos, pulsación, respuesta literal; (3) **autosuficiencia** — nada presupone estado previo de la BD fuera de «Recursos y datos iniciales» y los datos de demo. Propone los pasos concretos que sustituyen o se insertan. | `catalogos/catalogo-pasos-escenario.md` |
| B | **validaciones-restricciones** | fichero `entity-*.md` | **Primero** recorre campo a campo las propiedades de cada `AllowProperties` y comprueba la **obligatoriedad** (¿puede quedar vacío? si no → candidata), la validación más olvidada. **Después**, campo a campo × acción a acción, recorre las cuatro tablas del catálogo. Clasifica cada candidata: si debe cumplirse siempre → `RES-`; si se ancla a una acción → `VAL-`. Propone `condición`/`mensaje`/`actor` cuando apliquen. | `catalogos/catalogo-validaciones.md` |
| B | **reglas-negocio** | fichero `entity-*.md` | Acción a acción y transición a transición (sección «Estados y transiciones»): qué hace el sistema automáticamente al confirmarse cada una. Propone la `fase` y, si aplican, `estado`/`condición`. | `catalogos/catalogo-reglas-negocio.md` |
| B | **campos-calculados** | fichero `entity-*.md` | Campo a campo: cuáles dicta el servidor y no el usuario (cruza con las líneas `Input AllowProperties`: un campo que nadie envía pero que aparece en pantallas/escenarios es candidato). Propone `momento`/`sobreescribible`/`cálculo`. | `catalogos/catalogo-campos-calculados.md` |
| B | **reglas-ui** | fichero `screen-*.md` | Vista a vista del árbol y, dentro de cada una, sus campos, paneles y botones, considerando los roles que usan la pantalla y los estados del registro. Propone el `disparador` y, si aplican, `condición`/`actor`. | `catalogos/catalogo-reglas-ui.md` |

Reglas de los barridos:

- Cada subagente **propone candidatas, no escribe la spec**: las candidatas solo entran en la spec cuando el usuario las acepta en la conversación (el skill las presenta y pregunta).
- Cada subagente recibe su **elemento asignado** (un fichero, una historia de usuario o la spec entera, según su ámbito), su catálogo y la carpeta completa de la spec (para el contexto: índice, resto de entidades/pantallas). **MUST** leer lo ya declarado en toda la spec y **MUST NOT** proponer una candidata que duplique algo existente (mismo efecto sobre el mismo campo/acción/vista/escenario, aunque esté redactado distinto).
- El catálogo es **solo una guía no exhaustiva**: el subagente **puede y debe** proponer también candidatas que no figuren en el catálogo si el negocio de la spec las sugiere (indicando `(fuera de catálogo)` como fila de origen).
- **CRITICAL — lo obvio cuenta igual que lo complejo.** Un subagente tiende a proponer lo llamativo y a saltarse lo trivial por evidente. **MUST** proponer también las candidatas **obvias** (un campo obligatorio, un borrado en cascada, un estado inicial, el escenario del camino feliz, un filtro «solo veo lo mío»): lo fácil se olvida precisamente por fácil y es tan necesario como lo difícil (ver «Lo obvio pesa tanto como lo complicado»).
- Toda candidata va en **lenguaje de negocio** (aplican las prohibiciones de la sección «Lenguaje de negocio»): nada de tipos, clases, capas ni XML. Los pasos de escenario propuestos cumplen las reglas de «Historias de usuario», incluidos los **usuarios y centros de demo** (**MUST NOT** inventar cuentas, logins ni DNI).
- Una candidata **debe deducirse de lo que la spec ya cuenta** (sus actores, campos, estados, escenarios, pantallas, seguridad) — el subagente **MUST NOT** inventar funcionalidad nueva (campos, acciones o pantallas que la spec no tiene). El barrido **historias-escenarios** propone HU/ESC nuevos, pero solo los que **cubren lo ya declarado** (una pantalla sin escenario, un actor con acceso sin historia, una validación sin escenario de error…), no funcionalidades nuevas.
- Respetar la frontera entre familias: si una candidata bloquea → es del barrido validaciones-restricciones; si actúa/escribe → reglas-negocio; si solo cambia lo que se ve → reglas-ui; si es un valor que fija el servidor → campos-calculados. Ante la duda, proponerla una sola vez en la familia de su **efecto real**.
- El formato exacto de respuesta de cada subagente (token de "sin candidatas" y líneas JSONL, distinto por barrido) lo fija el skill `sdd-specification` en su fase de barrido, no esta guía.
