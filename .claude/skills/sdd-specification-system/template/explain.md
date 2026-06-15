# Guía de los ficheros de la especificación

Explica qué debe contener cada fichero de la especificación, cómo clasificar sus elementos y cómo numerarlos. Hay un ejemplo completo en la carpeta `template/example/`. Esta guía dirige la redacción y la revisión de la spec; **MUST NOT** copiarse ninguno de sus bloques explicativos al output.

La especificación **no es un único fichero**: es un conjunto de ficheros dentro de la carpeta de la iniciativa.

| Fichero | Plantilla | Qué contiene |
|---|---|---|
| `specification.md` | `template/specification.md` | El **índice**: objetivo, actores, historias de usuario con sus escenarios, las **tablas de enlaces** a los modelos y a las pantallas, seguridad, recursos y fuera de alcance. Es el único con frontmatter `type: specification`. |
| `entity-<Nombre>.md` | `template/entity.md` | Un fichero **por cada modelo**: su descripción, campos, estados, restricciones (`RES-`), campos calculados (`CC-`) y, por evento, validaciones (`VAL-`) y reglas de negocio (`RN-`). |
| `screen-<slug>.md` | `template/screen.md` | Un fichero **por cada pantalla**: su identidad, menú, paneles, botones y reglas de UI (`RUI-`). |

- `<Nombre>` del modelo va en **PascalCase** (p. ej. `entity-SolicitudCertificado.md`); `<slug>` de la pantalla en **kebab-case** (p. ej. `screen-mis-certificados.md`).
- Solo `specification.md` lleva frontmatter. Los `entity-*.md` y `screen-*.md` empiezan directamente por su `# Modelo: …` / `# Pantalla: …`.

---

## Reglas transversales

### Identificadores numerados

Historias, escenarios y reglas llevan IDs estables para que el diseño pueda comprobar que **ninguno se pierde**: cada regla del diseño declara de qué IDs de la spec proviene, y cada test E2E de qué escenario.

| Elemento | Prefijo | Ámbito de numeración | Dónde vive |
|---|---|---|---|
| Historias de usuario | `HU-NNN` | Global a la spec | `specification.md` |
| Escenarios | `ESC-NNN` | Global a la spec (no por historia) | `specification.md` |
| Restricciones | `RES-NNN` | Global a la spec (no por entidad) | `entity-*.md` |
| Validaciones | `VAL-NNN` | Global a la spec (no por evento) | `entity-*.md` |
| Reglas de negocio | `RN-NNN` | Global a la spec | `entity-*.md` |
| Reglas de UI | `RUI-NNN` | Global a la spec | `screen-*.md` |
| Campos calculados | `CC-NNN` | Global a la spec | `entity-*.md` |

- La numeración es **global a toda la spec**, no por fichero: el primer `VAL-` del proyecto es `VAL-001` esté en el modelo que esté, y el siguiente `VAL-` (aunque sea en otro `entity-*.md`) es `VAL-002`.
- Numeración desde `001`, **tres dígitos**, sin huecos al crear.
- **Los IDs no se renumeran nunca.** Al borrar un elemento su número se conserva como hueco (no se reutiliza), para no romper la trazabilidad con un diseño ya generado.
- **MUST NOT** usar otra taxonomía de reglas ni otros prefijos que los de esta tabla: la conversión a la taxonomía técnica de reglas es trabajo del diseño.
- ✅ CORRECTO: `HU-002`, `ESC-001`, `VAL-007`, `RN-012`
- ❌ INCORRECTO: `VAL-1` (sin tres dígitos), `VALIDACION-001` (prefijo inventado, no es uno de la tabla), `VAL-Pedido-001` (la numeración es global, sin entidad), `ESC_001` (guión bajo), `HU-001-ESC-001` (el escenario no anida el ID de la historia; la pertenencia se expresa agrupándolo bajo ella).

### Lenguaje de negocio

¿Lo entendería un supervisor del centro sin formación técnica? Si **no**, no va en la spec. Cada apartado de esta guía indica qué SÍ y qué NO admite. Esto aplica **a los tres tipos de fichero**: ni el índice ni los ficheros de modelo ni los de pantalla llevan tipos de dato, FQN, JPQL, atributos XML ni nombres de método. La única excepción es el término `AllowProperties` (dentro de las acciones de cada modelo), que se nombra explícitamente como concepto de seguridad. Qué propiedad puede enviar la interfaz se expresa en lenguaje de negocio en ese apartado; el detalle técnico de cada campo es trabajo del diseño.

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

**CRITICAL — formato y autosuficiencia.** Cada `ESC-NNN` se convierte en el diseño en un test E2E que se ejecuta contra una aplicación recién arrancada, **sin estado previo**. Por eso **cada escenario se escribe SIEMPRE como una lista de pasos numerados** (un paso por línea); **MUST NOT** escribirse como varias frases separadas por «;» en una sola línea, aunque el escenario sea simple. La secuencia es **completa, verificable y autosuficiente**:

1. Empieza con el actor **iniciando sesión** en la aplicación.
2. Sigue con la **preparación**: el actor (u otro actor que también inicia sesión dentro del escenario) crea todos los datos que la prueba necesita, hasta llegar al estado que se quiere probar. El único estado previo admisible es el descrito en "Recursos y datos iniciales".
3. Realiza la **acción que se prueba**.
4. Termina con la **respuesta del sistema**.

Entre medias puede haber más pasos y **ramas condicionales** (*"si \<condición\> el sistema hace X; si no, hace Y y no hace Z"*). Un escenario con ramas puede dar lugar a **más de un test** en el diseño (uno por rama).

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
- Si varios listados abren el **mismo** formulario en modo detalle, ese formulario se describe **una sola vez** como una pantalla compartida (un único `screen-*.md`), y los listados lo referencian; así se evita duplicarlo.
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

- **Obligatorio** es una regla: si debe cumplirse en todo evento es una **restricción** (`RES-NNN`); si solo en un evento concreto es una **validación** (`VAL-NNN`). No se marca en el campo.
- **Inmutable / no editable** lo gobierna **AllowProperties**: un campo que no aparezca en la línea `AllowProperties` de la acción `Modificar` no lo puede cambiar el cliente. No se marca en el campo.
- **Valores** de un campo de estado van en «Estados y transiciones»; los de otro enum sin ciclo de vida, en la propia descripción.

**Qué NO va:** tipos de campo, campos técnicos (IDs, FKs internas, auditoría, flags, versiones), anotaciones JPA. Qué campos puede enviar la interfaz se expresa en la línea `AllowProperties` de la acción correspondiente, no campo a campo aquí.

## Estados y transiciones

Solo si el modelo tiene ciclo de vida: su estado inicial, las transiciones (qué acción o circunstancia lleva de un estado a otro) y cuál es terminal. Si no tiene ciclo de vida, se omite la sección.

## Restricciones

**Qué son:** invariantes de una entidad. Condiciones que deben cumplirse siempre, independientemente de la acción que se ejecute. Si se viola una restricción, el objeto está en un estado inválido.

**Cómo se asocian:** a la entidad, no a una acción concreta.

**Regla de clasificación:** si la condición debe cumplirse en todas las acciones de la entidad, es una restricción. Si solo aplica a una acción concreta, es una validación.

**REQUIRED — identificación:** para no olvidar ninguna restricción, recorre el catálogo `template/catalogo-validaciones.md` (sobre todo las tablas "entre registros" y "de negocio": unicidad, cardinalidad de hijos, inmutabilidad por estado) y comprueba, campo a campo, cuáles aplican siempre (→ restricción) y cuáles solo en una acción (→ validación). El catálogo es una ayuda **no exhaustiva**: si el negocio necesita una restricción que no aparece en él, decláralo igualmente.

**Ejemplo:**

**Restricciones:**

- RES-001 — El número de expediente es único en el sistema
- RES-002 — La fecha de cierre no puede ser anterior a la fecha de apertura

## Campos calculados

**Qué son:** valores de la entidad que el sistema calcula automáticamente. Nunca los proporciona el cliente; siempre los calcula el servidor.

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

- CC-001 — total
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: suma de (cantidad × precio_unitario) de todas las líneas
- CC-002 — descuento_especial
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

### Validaciones

**Qué son:** comprobaciones bloqueantes asociadas a una acción concreta. Si fallan, la acción se cancela y no ocurre ningún cambio en el sistema. En la spec basta con clasificarlas bien; el pipeline las materializa más adelante.

**Cómo se asocian:** a una acción de una entidad.

**REQUIRED — identificación:** para identificar qué validaciones aplican a cada campo, recorre el catálogo de tipos de validación `template/catalogo-validaciones.md` (campo propio, entre campos del mismo registro, entre registros, de negocio); sus columnas de mensaje sirven de guía para redactar el `mensaje` en lenguaje de negocio. El catálogo es una ayuda **no exhaustiva**: si el negocio necesita una validación que no aparece en él, declárala igualmente.

**El texto es la aserción; `condición` es la guardia.** El **texto** de la validación es *lo que debe cumplirse* (la aserción que, si no se da, bloquea). El atributo `condición` es la **guardia**: *cuándo* se evalúa la validación. Son cosas distintas — si la guardia repite lo que ya afirma el texto, la validación nunca falla y sobra. Por eso una precondición de estado pura ("la solicitud está en estado PENDIENTE") va como **texto**, no como `condición`.

**Atributos opcionales:**

- `condición`: la guardia que decide **cuándo aplica** la validación, en lenguaje de negocio (un estado u otra circunstancia). Si no se cumple, la validación no se evalúa (no bloquea).
- `mensaje`: el error que se devuelve al cliente si falla.
- `actor`: el rol que dispara la acción (las validaciones pueden diferir por actor).

### Reglas de negocio

**Qué son:** operaciones que el sistema ejecuta automáticamente como reacción a una acción ya confirmada. No bloquean. No deciden si la acción ocurre. Solo actúan.

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

- VAL-004 — La solicitud está en estado PENDIENTE
  - mensaje: "Solo se pueden rechazar solicitudes pendientes"
- VAL-005 — El motivo de rechazo está indicado
  - mensaje: "El motivo es obligatorio"

**Reglas de negocio:**

- RN-005 — Enviar al alumno un correo con el motivo del rechazo
  - fase: después_de_commit
```

(`fecha de solicitud` es `CC` → nunca editable; `estado`, `fecha de resolución` y `documento emitido` los fija el servidor → fuera de toda línea de `AllowProperties`. La acción `Emitir` no recibe datos → no lleva línea de `AllowProperties`.)

---

# Los ficheros de pantalla — `screen-<slug>.md`

Un fichero por cada pantalla de la tabla "Pantallas" del índice. Describe la pantalla en lenguaje de negocio y aloja sus reglas de UI. El título es `# Pantalla: <Nombre>`.

## Identidad

- **Quién la usa:** los roles que ven o usan la pantalla y en qué modo cada uno.
- **Qué muestra:** qué presenta, sobre qué modelo, con qué filtro en lenguaje natural y en qué modo (lectura/edición), incluidas las relaciones maestro-detalle inline si las hay.

## Menú

El menú que da entrada a la pantalla: dónde cuelga en la jerarquía y quién lo ve. Si la pantalla no se abre desde un menú sino desde un listado (formulario de detalle), indícalo así.

## Paneles

Los bloques visibles de la pantalla, uno por viñeta, en lenguaje de negocio: el título del panel en negrita y, tras un `—`, qué campos o contenido agrupa. Para una pantalla que **no** es un formulario de una entidad (p. ej. una gráfica), describe aquí sus **parámetros de entrada** y qué representa, en vez de paneles de campos.

**Qué NO va:** nombres técnicos de vista o de campo del framework, atributos XML.

## Botones

Las acciones disponibles en la pantalla, una por viñeta: la etiqueta del botón en negrita y, tras un `—`, qué acción de negocio dispara y cuándo es visible. Si no hay botones, escribe `*(sin botones)*`.

## Reglas de UI

**Qué son:** condiciones que cambian el aspecto o el estado de un formulario en función del valor de uno o varios campos, del usuario actual, del registro padre o de un evento (al crear, al cargar, al cambiar un campo). Solo afectan a lo que **ve** y puede editar el usuario en pantalla — **no bloquean operaciones ni modifican el estado del sistema**.

**Cómo se asocian:** a una **pantalla** (formulario), **no** a una entidad — por eso viven en el `screen-*.md`, no en el `entity-*.md`. Cada regla pertenece a la pantalla en cuyo fichero está; si una misma conducta se necesita en otra pantalla, es otra regla de UI con su propio ID en ese otro `screen-*.md`.

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

**Ejemplo** (en `screen-formulario-expediente.md`):

**Reglas de UI:**

- RUI-001 — El campo motivo de rechazo solo se muestra cuando el estado es RECHAZADO
  - disparador: continuo
  - condición: estado == RECHAZADO
- RUI-002 — Al crear un expediente, el centro se rellena con el centro del usuario actual
  - disparador: al crear
  - condición: Siempre
- RUI-003 — El botón Publicar solo lo ve el administrador
  - disparador: al cargar
  - condición: Siempre
  - actor: [ADMIN]
