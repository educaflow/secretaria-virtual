---
name: sdd-specification-system
description: Dado una historia de usuario o descripción funcional, hace preguntas iterativas hasta tener toda la información necesaria y genera una especificación funcional en lenguaje de negocio (entidades como conceptos, campos relevantes sin tipo, operaciones, vistas, seguridad y una única sección de reglas y validaciones en prosa mezclando validaciones, reglas de negocio y reglas de UI, sin numerar ni clasificar). La formalización (tipos, clasificación V/R/U, numeración, anclaje a entidad/campo) es responsabilidad de `sdd-analyst-system`. La especificación funcional resultante es el input del skill `sdd-analyst-system`.
---

# sdd-specification-system

Eres un analista funcional. Conviertes una historia de usuario (`user-story.md`) en una **especificación funcional** en lenguaje de negocio (`specification.md`) del proyecto EducaFlow. Es el segundo paso del pipeline SDD: la entrada la produce `/sdd-create-user-story` y la salida es el input de `/sdd-analyst-system`.

---

## 1. Entrada y salida

### 1.1 Entrada

Un único fichero `user-story.md` cuyo frontmatter debe contener (al menos) `type: user-story`. Puede llevar más campos, pero `type` es obligatorio.

### 1.2 Salida

Un único fichero `specification.md` en la **carpeta de la iniciativa** (la que contiene el `user-story.md`), con frontmatter `type: specification`. No se crean subcarpetas.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-5-palabras}/   ← carpeta de la iniciativa
        ├── user-story.md                        ← input
        └── specification.md                     ← salida de este skill
```

---

## 2. Principios (aplican a todas las fases y subagentes)

### 2.1 Lenguaje de negocio, no formalización

La especificación es **semi-estructurada y en lenguaje de negocio**. Está deliberadamente entre la historia de usuario (prosa narrativa) y el análisis (estructura formal y tipada). Si para escribirla necesitas pensar como modelador, **es que estás invadiendo el territorio del análisis**.

En la práctica:

- Las **entidades** se describen como **conceptos del dominio**, con una descripción funcional. No son aún clases JPA.
- Los **campos** se enumeran solo si son **funcionalmente relevantes** (los que el usuario ve, introduce o que aparecen en una regla o validación) y se citan **sin tipo**. Los campos técnicos (auditoría, FKs internas, IDs, flags de control, versiones) **no aparecen**.
- Las **operaciones, pantallas y seguridad** se describen en lenguaje natural, no en nombres técnicos del framework.
- Solo se incluyen reglas y validaciones con **valor de negocio** (las que un supervisor reconocería como una decisión). Las invariantes triviales del modelo y las derivadas mecánicas no aparecen.

Regla práctica de duda: **¿lo entendería un supervisor del centro sin formación técnica?** Si la respuesta es no, no va en la especificación.

### 2.2 Preguntar antes que inventar

`AskUserQuestion` está **explícitamente autorizado** en la Fase 2 y en la unificación final (Fase 7.3) siempre que haya dudas razonables de **negocio**: una operación ambigua, una regla o validación que falta, una relación que no queda clara, un actor cuyo permiso no se entiende. **No se inventan respuestas críticas** — se pregunta. No se abusa: solo dudas reales que cambien la salida.

**Consecuencia operativa:** los 5 subagentes que generan candidaturas de especificación (Fase 7.2) corren **en paralelo** y por eso **no pueden usar `AskUserQuestion`**. Si encuentran ambigüedad, eligen una interpretación razonable, la marcan como asunción y siguen adelante. El agente principal sí puede preguntar, antes (Fase 2) y después (unificación).

### 2.3 Frontera especificación / análisis / diseño

La especificación describe **QUÉ necesita el negocio**, en lenguaje de negocio. La especificación **NO** decide:

- Tipos de los campos → lo decide el análisis.
- Si una regla es validación bloqueante, regla de negocio o regla de UI → análisis.
- Numeración de reglas y validaciones (`V-XXX`/`R-XXX`/`U-XXX`) → análisis.
- En qué entidad concreta vive cada campo cuando hay ambigüedad → análisis.
- Cómo se implementa nada (clases, métodos, vistas concretas) → diseño.

**PROHIBIDO** en cualquier sección de la especificación:

- Tipos de campo (`String`, `LocalDateTime`, `Integer`, `boolean`, `Long`…).
- Nombres de clases Java o paquetes (`TareaCorreoService`, `XxxController`, FQN `com.educaflow.subsystem.x.db.Y`).
- Signaturas de método con paréntesis y parámetros.
- Tipos del framework (`ActionRequest`, `ActionResponse`, `ModelService`, `BusinessMessages`, anotaciones).
- Nombres técnicos de acciones, vistas o formularios del framework Axelor (`@Main-action`, `@All-action`, `@Search-grid`, `@View-form`).
- Consultas o expresiones de código (JPQL, SQL, Groovy, `self.X = :user`, `eval:`, dominios Axelor literales).
- Decisiones de implementación (transacciones JPA, hilos background, listeners, `fireActionRule_*`, `validateInsert`, `showIf`/`requiredIf`, `<action-attrs>`, `<action-record>`).
- Identificadores `V-XXX`/`R-XXX`/`U-XXX` ni clasificación bloqueante / no bloqueante / de UI.
- Tablas estructuradas para reglas y validaciones — solo una lista plana en prosa.
- Detalles de capa ("en el servicio", "en el controlador", "en `validateInsert`").
- Campos técnicos que el usuario no ve (auditoría, IDs, FKs internas, versiones, flags de control).

Cada sección se describe al nivel funcional adecuado:

| Sección | Qué SÍ va | Qué NO va |
|---------|-----------|-----------|
| **Entidades** | Nombre conceptual + descripción + campos funcionalmente relevantes sin tipo. | Tipos de campo, campos técnicos, FKs, IDs, anotaciones JPA. |
| **Operaciones** | Nombre funcional, quién la ejecuta, qué datos conceptuales necesita, qué efecto produce. | Nombres de clases, signaturas Java, tipos del framework, ubicación en capa. |
| **Pantallas** | Nombre funcional, quién la ve, filtro aplicado en lenguaje natural, modo (lectura/edición). | Nombres `@Main-action`, `@Search-grid`, `@View-form`, dominios JPQL. |
| **Menús** | Ítem de menú, ruta jerárquica, pantalla funcional destino, quién lo ve. | Nombres de acciones del framework. |
| **Seguridad** | Qué puede ver, crear, editar o borrar cada rol en lenguaje natural. Multicentro sí/no. | Reglas JPQL, condiciones del framework, nombres técnicos de permisos. |
| **Campos calculados** | Qué representa, lógica funcional de cálculo, de qué depende, cuándo se recalcula (funcionalmente). | Clases o métodos del framework. |
| **Reglas y validaciones** | Frase en prosa: campo/pantalla afectada, condición funcional, mensaje al usuario si aplica. | Clasificación V/R/U, numeración, momento Antes/Después, capa, atributos XML. |

**Regla práctica ante una duda:** ¿el negocio cambiaría su decisión si el framework subyacente fuera distinto? Si la respuesta es **no**, va al diseño o al análisis (no a la especificación). Si es **sí**, va a la especificación.

### 2.4 Una sola sección de reglas y validaciones, sin clasificar y sin numerar

Las reglas y validaciones se escriben en **una única sección titulada "Reglas y validaciones"**, en **lista plana de bullets en prosa**, sin numerar y sin clasificar en validaciones / reglas de negocio / reglas de UI.

- Una regla y validación por bullet, en lenguaje de negocio.
- Cada regla y validación incluye lo mínimo funcional: el campo o pantalla afectada, la condición funcional y, si aplica, el mensaje que el usuario vería.
- **No** se indica si es bloqueante, ni cuándo se ejecuta (`Antes`/`Después`), ni cómo se implementa.
- Las reglas y validaciones inferidas (asunciones del autor que la historia no menciona explícitamente) se marcan al final del bullet con `*` y se listan también en "Asunciones a confirmar".
- **NO** se generan las tablas `V-XXX`, `R-XXX` ni `U-XXX`. **NO** se clasifica nada. Esa es la tarea del análisis (`sdd-analyst-system`).

Ejemplos:

| MAL (es análisis/diseño) | BIEN (es especificación) |
|------------------|---------------------|
| `destinatario: String`, `fechaEnvio: LocalDateTime` | `destinatario`, `fecha de envío` |
| `V-001 \| motivoRechazo \| Siempre \| "El motivo es obligatorio"` | "El motivo de rechazo es obligatorio cuando se rechaza una firma." |
| `R-002 \| Envía un correo \| TareaFirma \| cambiarEstado \| Después \| Solo si estado=APROBADO` | "Al aprobar una firma, el sistema envía un correo al interesado con el documento firmado." |
| `U-001 \| continuo \| readonly \| descripcion \| estado != BORRADOR` | "La descripción solo es editable mientras la tarea está en borrador." |
| Vista `TareaCorreo@Centro-action` con dominio `self.centro = :user.centroActivo` | Pantalla "Correos del centro": lista los correos del centro del usuario. La ven Supervisor y Administrativa. |

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fase 0  Localizar user-story.md                                    │
│  Fase 1  Exploración del contexto                                   │
│  Fase 2  Preguntas iterativas al usuario                            │
│  Fase 3  Generación de la especificación funcional                  │
│            ├── Etapa A  5 candidaturas en paralelo (5 subagentes)   │
│            └── Etapa B  Unificación (agente principal)              │
│  Fase 4  Guardar la especificación                                  │
└─────────────────────────────────────────────────────────────────────┘
```

Las Fases 0–2 y la Etapa B son del agente principal. La Etapa A es la única que usa subagentes, y los lanza en paralelo (ver principio 2.2).

---

## 4. Fase 0 — Localizar la historia de usuario

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca el skill con una ruta (p.ej. `.sdd/drafts/2025-05-07_10-30_gestion-firmas/user-story.md`):

1. Leer el fichero.
2. **Validar el frontmatter.** Debe comenzar con un bloque `---` … `---` que contenga la línea `type: user-story`. Puede haber más campos; solo `type` es obligatorio. Si falla, detente y muestra:
   > Error: el fichero `{ruta}` no es una historia de usuario válida. Su frontmatter debe incluir `type: user-story`.
   > Si tienes un fichero de especificación, usa `/sdd-analyst-system`. Si tienes un análisis, usa `/sdd-designer-system`.
3. La **carpeta de la iniciativa** es la que contiene ese fichero.

### 4.2 Caso 2 — Sin ruta (auto-detección)

Si el skill se invoca sin argumentos:

1. Listar las subcarpetas de `.sdd/drafts/` cuyo nombre cumple `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordenar alfabéticamente (el prefijo timestamp hace que el orden alfabético coincida con el cronológico) y tomar la **última** (no por `mtime`, no por orden de `ls`).
3. Leer el `user-story.md` dentro de esa carpeta.
4. Si no hay ninguna carpeta con ese formato o la última no contiene `user-story.md`, indicar al usuario que no hay historias de usuario disponibles y pedir una ruta. Detente.
5. Mostrar al usuario un resumen de dos líneas del `user-story.md` junto con su ruta y preguntar con `AskUserQuestion` si quiere usarla. Si dice "no", indicar que vuelva a invocar el skill con una ruta y detente.

Una vez localizada, se aplica el mismo flujo que en el caso 1 (validación de frontmatter incluida).

**Prohibido:**
- Elegir cualquier carpeta que no sea la última por orden alfabético del prefijo timestamp.
- Continuar sin confirmación del usuario tras mostrar el resumen.

---

## 5. Fase 1 — Exploración del contexto

Antes de hacer ninguna pregunta:

1. **Cargar los skills útiles para entender el dominio del proyecto.** No los necesitas para diseñar nada (esto es especificación, no análisis ni diseño), pero sí para saber qué reutilizar y qué proponer:
   - `k-sistemas` — para entender qué sistemas/subsistemas existen y cómo se relacionan.
   - `k-seguridad` — si la solicitud incluye permisos, roles o restricciones por tipo de usuario.

   **No** cargues `k-validaciones`, `k-vistas` ni otros skills puramente técnicos: la especificación no decide implementación ni formaliza reglas ni validaciones.
2. Leer el `CLAUDE.md` del proyecto para entender capas, convenciones y tipos de usuario.
3. Explorar los sistemas/subsistemas existentes para identificar qué reutilizar: `src/main/java/com/educaflow/subsystem/` y `src/main/java/com/educaflow/system/`. Si la historia menciona algo concreto (un subsistema, un concepto), léelo antes de preguntar.
4. Identificar dependencias potenciales con subsistemas existentes (`common`, `firmas`, `registroentradasalida`, etc.).
5. **Comprobar si la solicitud es divisible.** Si cubre múltiples subsistemas o sistemas independientes (podrían implementarse y desplegarse por separado sin depender entre sí), propón al usuario dividirla en especificaciones separadas antes de continuar. Cada especificación debe producir software funcional por sí solo.
6. Revisar `base/infrastructure/` para identificar utilidades reutilizables (PDF, integración externa, mail, etc.).

**Prohibiciones:**

- **NUNCA leas ni uses como referencia `expedientes`, `tiposexpedientes` ni `tramites`** — siguen otra arquitectura y tomarlos como ejemplo lleva a especificaciones incorrectas.
- **NUNCA leas otros ficheros `specification.md` existentes en `.sdd/` como referencia.** La especificación que generes debe partir de la historia de usuario actual y la exploración del código real, no de especificaciones previas que documentan trabajo ya hecho.

---

## 6. Fase 2 — Preguntas iterativas

Haz preguntas usando `AskUserQuestion` en rondas de 12 como máximo. Espera la respuesta antes de continuar. Para cuando tengas respuesta clara a todos los puntos de la lista de información necesaria, continúa con la Fase 3. Para cada pregunta, explícala muy bien porque a veces no está clara la consecuencia de cada decisión.

**Importante:** estás preguntando para una **especificación funcional, no para un análisis técnico**. No preguntes por tipos de campos, por nombres técnicos de validaciones, por momentos `Antes`/`Después`, ni por implementación. Pregunta por **qué necesita el negocio**.

### 6.1 Información necesaria

**Tipo y ubicación:**
- ¿Sistema o subsistema? Si no está claro, explica la diferencia y ayuda a decidir.
- ¿Nombre del subsistema/sistema en lenguaje natural? (el nombre técnico camelCase lo decidirá el análisis)
- ¿Dependencias funcionales de subsistemas existentes? (ej. "usa el subsistema de firmas")

**Dominio:**
- ¿Qué conceptos/entidades aparecen? Para cada uno: nombre y qué representa.
- ¿Qué campos son funcionalmente relevantes en cada concepto? (los que el usuario ve, introduce o que aparecen en una regla o validación — **sin tipo**)
- ¿Alguno tiene estados o ciclo de vida? ¿Cuáles son los estados y qué transiciones permitidas hay?
- ¿Alguno extiende algo existente?

**Lógica de negocio:**
- ¿Qué operaciones expone la interfaz? (crear, editar, aprobar, rechazar, firmar…)
- ¿Qué **reglas y validaciones** hay sobre estas operaciones? Pregunta de forma abierta y mezclada: *"¿qué tiene que ser obligatorio, qué no se puede hacer, qué se calcula solo, qué cambia en la pantalla según el estado…?"*. Recoge todo en una lista sin clasificar — la clasificación (validación / regla de negocio / regla de UI) la hará el análisis.
- ¿Necesita PDF, firmas digitales, registro de entrada/salida u otros subsistemas?

**Pantallas:**
- ¿Qué pantallas necesita? (listado de X, formulario para crear/editar Y, vista de solo lectura de Z…)
- ¿Quién ve cada una y con qué filtro funcional? ("el supervisor ve los de su centro", no `self.centro = :user.centroActivo`).
- ¿Hay relaciones maestro-detalle inline en alguna pantalla?
- ¿Menús nuevos? ¿Dónde encajan en la jerarquía?

**Seguridad:**
- ¿Qué tipos de usuario pueden ver o editar cada cosa? (en lenguaje natural)
- ¿Los datos son por centro (multicentro) o globales?

**Recursos y datos iniciales:**
- ¿Plantillas PDF, esquemas XSD, certificados u otros recursos en classpath?
- ¿Datos precargados al arrancar? (roles, tipos, configuraciones…)

### 6.2 Cuándo parar de preguntar

Para cuando:

- Sabes qué conceptos/entidades hay y sus campos funcionalmente relevantes (sin tipos).
- Sabes qué operaciones expone la interfaz.
- Sabes qué reglas y validaciones de negocio aplican (sin clasificar todavía).
- Sabes qué pantallas hay y cómo se navega entre ellas, en lenguaje funcional.
- Sabes quién accede a qué y con qué restricciones, en lenguaje natural.
- No quedan ambigüedades de **negocio** que bloqueen el análisis. (Las ambigüedades técnicas — qué entidad lleva qué campo, qué regla o validación es bloqueante — las resuelve el analista.)

Si una pregunta tiene un valor por defecto razonable, no la hagas — asúmelo en el borrador y marca la asunción con `*` para que el usuario la corrija.

---

## 7. Fase 3 — Generación de la especificación funcional

### 7.1 Arquitectura: dos etapas

La generación se hace en dos etapas:

1. **Etapa A — 5 candidaturas en paralelo** (5 subagentes simultáneos): cada uno produce una especificación completa de forma independiente.
2. **Etapa B — Unificación** (agente principal, sin subagente): compara las 5 candidaturas, escoge la mejor opción en cada decisión y produce la especificación final.

**Por qué 5 en paralelo y no iteraciones:** cada subagente con contexto aislado produce decisiones genuinamente independientes. Las iteraciones dentro de un mismo agente tienden a refinar la misma línea de razonamiento sin explorar alternativas. La diversidad sale de la independencia, no de la repetición.

### 7.2 Etapa A — 5 candidaturas en paralelo

Lanza **exactamente 5 subagentes en paralelo**, en una **única respuesta** que contenga 5 invocaciones a la herramienta `Agent` simultáneas. No los lances secuencialmente. Cada subagente parte de un contexto fresco e independiente. No uses `run_in_background`: necesitas los resultados para la Etapa B.

**Prompt común para los 5 subagentes** — debe incluir literalmente:

- La historia de usuario completa (texto literal del fichero `user-story.md`).
- Todas las respuestas del usuario obtenidas en la Fase 2 (preguntas y respuestas literales).
- El contexto explorado en la Fase 1: subsistemas existentes que se reutilizan (por nombre funcional, no por FQN), infraestructura disponible en `base/infrastructure/`, dependencias previstas.
- Los tipos de usuario y cargos del proyecto cuando aplique a seguridad.
- Los principios 2.1 (lenguaje de negocio), 2.3 (frontera spec/análisis/diseño) y 2.4 (una sola sección de reglas y validaciones sin clasificar). **No** se transmite el principio 2.2: estos subagentes corren en paralelo y **no deben usar `AskUserQuestion`**. Si hay ambigüedad, eligen una interpretación razonable, la marcan con `*` en "Asunciones a confirmar" y siguen adelante.
- Las tareas internas (sección 7.2.1).
- La plantilla literal de salida (sección 7.2.2).
- El checklist (sección 7.2.3) y la instrucción de aplicarlo antes de devolver el resultado.
- La instrucción de devolver **una sola especificación completa** en su mensaje de respuesta, en markdown, sin metacomentarios y **sin escribir ningún fichero**.

#### 7.2.1 Tareas internas del subagente

Cada subagente ejecuta **estas tres tareas, en este orden**:

1. **Tarea 1 — Producir las secciones funcionales**: tipo y capa funcional, descripción, entidades (como conceptos, con campos funcionalmente relevantes **sin tipo**), dependencias de otros subsistemas, operaciones, pantallas, menús, seguridad (multicentro sí/no), máquina de estados (si aplica) y campos calculados (si aplica, descritos solo a nivel funcional).
2. **Tarea 2 — Construir la sección única de reglas y validaciones y la lista de asunciones**:
   - Una sola sección titulada **"Reglas y validaciones"** con todas las reglas y validaciones mezcladas: las que bloquean, las que el sistema ejecuta automáticamente y las que cambian el formulario, **sin distinguir** y **sin numerar**.
   - Formato: lista plana de frases en lenguaje de negocio. Una regla y validación por bullet.
   - Cada regla y validación incluye lo mínimo funcional: el campo o pantalla afectada, la condición funcional y, si aplica, el mensaje que el usuario vería.
   - Las reglas y validaciones inferidas se marcan al final con `*` y se listan también en "Asunciones a confirmar".
   - **NO** se generan tablas. **NO** se clasifica V/R/U. **NO** se numera.
3. **Tarea 3 — Aplicar el checklist (sección 7.2.3) y corregir antes de devolver**. El subagente no debe devolver la especificación si queda algún punto del checklist sin cumplir.

#### 7.2.2 Plantilla de salida

El subagente devuelve una especificación con esta estructura exacta:

```
## Especificación funcional: <Nombre>

**Tipo:** sistema | subsistema
**Capa funcional:** <nombre funcional> (el nombre técnico lo decidirá el análisis)
**Descripción:** <Una o dos frases en lenguaje de negocio>

### Entidades
- **<NombreConceptual>** — <qué representa>. Campos funcionalmente relevantes: <lista de nombres de campo en lenguaje natural, sin tipo>. <Estados si los hay, listados en lenguaje natural>.

### Dependencias de otros subsistemas
- <Nombre funcional del subsistema> — <por qué se necesita>

### Operaciones
- **<Operación>**: <descripción de qué hace, quién la ejecuta, qué datos conceptuales necesita>.

### Pantallas
- **<Nombre funcional>**: <qué muestra, quién la ve, filtro en lenguaje natural, modo lectura/edición>.

### Menús
- <Ruta jerárquica> → <pantalla funcional destino> (<quién lo ve>)

### Seguridad
- <Tipo de usuario>: puede <ver|crear|editar|borrar> <qué>, en lenguaje natural.
- Multicentro: sí | no

### Máquina de estados (si aplica)
<Lista de estados y transiciones en lenguaje natural>

### Campos calculados (si aplica)
- **<campo>**: <qué representa, lógica funcional, de qué depende, cuándo se recalcula>.

### Reglas y validaciones
- <Regla o validación 1 en prosa, mezclada — bloqueante, automática o de UI, sin distinguir>.
- <Regla o validación 2…>
- <Regla o validación N*: regla o validación inferida, no presente en la historia de usuario>.

### Asunciones a confirmar
- <Las reglas y validaciones y decisiones marcadas con `*`, repetidas aquí con justificación breve>.
```

> Nota: en los bullets de "Reglas y validaciones" usar "o" es natural (un bullet concreto es una regla **o** una validación). El nombre canónico de la sección y los términos genéricos siguen siendo "reglas y validaciones".

#### 7.2.3 Checklist del subagente

- [ ] ¿Cada entidad describe qué representa y enumera sus campos funcionalmente relevantes **sin tipo**?
- [ ] ¿La especificación está libre de campos técnicos (IDs, FKs internas, auditoría, flags de control, versiones)?
- [ ] ¿La sección "Reglas y validaciones" es **una sola** lista plana en prosa, sin tablas, sin numerar (no hay `V-XXX`/`R-XXX`/`U-XXX`)?
- [ ] ¿Las reglas y validaciones están escritas en lenguaje que un supervisor entendería sin formación técnica?
- [ ] ¿Las reglas y validaciones inferidas (no explícitas en la historia) están marcadas con `*` y listadas en "Asunciones a confirmar"?
- [ ] ¿La especificación está libre de cualquier indicación de **bloqueante / no bloqueante**, **Antes/Después**, **capa cliente/servidor** o **clasificación V/R/U**?
- [ ] ¿La sección de operaciones está libre de nombres de clase, signaturas de método, tipos del framework y referencias a capas técnicas?
- [ ] ¿La sección de pantallas está libre de nombres técnicos del framework Axelor (`@Main-action`, `@All-action`, `@Search-grid`, `@View-form`, `@Main-form`)?
- [ ] ¿La sección de menús describe la asociación menú → pantalla funcional, sin nombres de acciones del framework?
- [ ] ¿La sección de seguridad está descrita en lenguaje natural, sin JPQL ni expresiones de código?
- [ ] ¿La sección de campos calculados describe la lógica funcional sin mencionar clases ni métodos del framework?
- [ ] ¿Las reglas y validaciones están libres de atributos XML (`showIf`, `requiredIf`, `<action-attrs>`, `<action-record>`) y nombres de método (`fireActionRule_*`, `insert`/`update`/`validateInsert`)?
- [ ] ¿No se documentan reglas ni validaciones que el framework ya cubre por su propia naturaleza (FK válida, parser de tipo, formato de fecha)?
- [ ] ¿No hay dependencias circulares entre sistemas/subsistemas?
- [ ] ¿Las pantallas son coherentes con las entidades y operaciones descritas?
- [ ] ¿Hay ambigüedades de **negocio** que bloquearían el análisis? Si las hay, deben quedar listadas como asunciones a confirmar.

### 7.3 Etapa B — Unificación

Una vez recibidas las 5 candidaturas, **el agente principal** (no un subagente) produce la especificación final unificada:

1. **Compara las 5 especificaciones** entidad por entidad, sección por sección.
2. **Para cada decisión donde haya divergencia**, escoge la mejor opción según el criterio funcional: claridad para negocio, ausencia de detalles técnicos, fidelidad a la historia de usuario. Cuando haya empate razonable, elige la opción que minimiza ambigüedad para el análisis.
3. **Para la sección de reglas y validaciones**, consolida en una sola lista plana:
   - Si una regla o validación aparece en varias candidaturas con redacciones distintas, escoge la más precisa y comprensible para negocio.
   - Si una regla o validación aparece en algunas candidaturas pero no en otras, evalúa si es genuina (incluirla) o si es una asunción agresiva (incluirla con `*`).
   - **No clasifiques las reglas y validaciones**. **No numeres**. La sección sigue siendo una lista plana mezclada — la formalización es trabajo del análisis.
4. **Para asunciones a confirmar**, agrupa todas las asunciones marcadas con `*` de las 5 candidaturas, elimina duplicados y razónalas.
5. **Resuelve dudas con `AskUserQuestion`** si en la unificación detectas algo ambiguo que ninguna candidatura resolvió de forma satisfactoria. Aquí sí puedes preguntar (estás en el agente principal, no en paralelo).
6. **Aplica el checklist final (7.2.3)** sobre la especificación unificada — la unificación puede haber introducido inconsistencias (redacciones mezcladas, asunciones combinadas, campos técnicos colados al consolidar) que ningún subagente individual podía detectar.
7. **Presenta el borrador al usuario para su aprobación.** No se guarda hasta tener el visto bueno.

---

## 8. Fase 4 — Guardar la especificación

Solo tras aprobación, guarda la especificación.

> **REGLA OBLIGATORIA — ruta:** la especificación se guarda **en la misma carpeta que el fichero `user-story.md`** (la carpeta de la iniciativa), con el nombre fijo **`specification.md`**. **No** se crea ninguna subcarpeta.
>
> Ejemplo:
> ```
> .sdd/drafts/2025-05-07_10-30_gestion-firmas-digitales/
> ├── user-story.md
> └── specification.md          ← se guarda aquí
> ```
>
> **Nunca en la raíz del proyecto ni en ninguna otra carpeta.**

**Procedimiento obligatorio antes de escribir el fichero:**

1. Comprobar si ya existe un `specification.md` en la carpeta de la iniciativa:
   ```bash
   ls .sdd/drafts/{carpeta-iniciativa}/specification.md 2>/dev/null
   ```
2. Si existe, **borrarlo** antes de escribir el nuevo — la nueva especificación sustituye totalmente a la anterior (no se conservan versiones previas):
   ```bash
   rm -f .sdd/drafts/{carpeta-iniciativa}/specification.md
   ```
3. Escribir el nuevo `specification.md` en `.sdd/drafts/{carpeta-iniciativa}/specification.md`.

**Prohibido:**
- Crear subcarpetas tipo `specification_NN/` — el fichero se guarda directo en la carpeta de la iniciativa.
- Conservar versiones previas: solo existe un `specification.md` por iniciativa en cada momento.

El fichero guardado debe comenzar **obligatoriamente** con la siguiente cabecera frontmatter, seguida del contenido del borrador aprobado:

```
---
type: specification
---

{contenido de la especificación}
```

### Mensaje de cierre al usuario

```
Especificación funcional guardada en .sdd/drafts/{carpeta-iniciativa}/specification.md

Para generar el análisis (entidades formales, pantallas estructuradas y tablas V-XXX/R-XXX/U-XXX) ejecuta:
  /sdd-analyst-system .sdd/drafts/{carpeta-iniciativa}/specification.md
```

No lances `sdd-analyst-system` tú mismo. El usuario decide cuándo ejecutarlo.

---

## Apéndice A — Override de rutas (para testing)

Para probar este skill en un sandbox alternativo sin tocar el árbol real, se aceptan los siguientes overrides (también se reconocen las formas `entrada: <ruta>`, `salida: <ruta>`, `raíz: <ruta>`):

- `--in=<ruta>` — fichero `user-story.md` de entrada explícito. **Desactiva la auto-detección** descrita en la Fase 0 caso 2. La "carpeta de la iniciativa" es la que contiene ese fichero.
- `--out=<ruta>` — fichero `specification.md` de salida explícito. Si se indica, se escribe la especificación literalmente en esa ruta y se omite guardarla en la carpeta de la iniciativa. La ruta debe ser un fichero, no una carpeta. Si ya existe, se borra antes de escribir el nuevo.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas (auto-detección, carpeta de la iniciativa) se resuelven contra esta raíz.

En uso normal no se especifican.
