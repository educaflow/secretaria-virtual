---
name: sdd-specification-system
description: Dado una historia de usuario o descripción funcional, hace preguntas iterativas hasta tener toda la información necesaria y genera una especificación funcional en lenguaje de negocio (entidades como conceptos, campos relevantes sin tipo, operaciones, vistas, seguridad, **flujos principales `F-NNN`** narrativos en 1-3 frases que sirven de semilla para los tests E2E, y los requisitos redactados con plantillas EARS — Ubicuos `E-UB`, Eventos `E-EV`, Estados `E-ST`, No deseado `E-UN`, Opcionales `E-OP` — numerados localmente por patrón, sin clasificar todavía en validaciones, reglas de negocio o reglas de UI). La clasificación V/R/U y el anclaje a entidad/campo siguen siendo responsabilidad de `sdd-analyst-system`. La especificación funcional resultante es el input del skill `sdd-analyst-system`.
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
- Detalles de capa ("en el servicio", "en el controlador", "en `validateInsert`").
- Campos técnicos que el usuario no ve (auditoría, IDs, FKs internas, versiones, flags de control).
- Escenarios de prueba detallados al estilo Given/When/Then con pasos numerados, nombres de pantalla, botones o mensajes literales. Los **flujos principales** (sección dedicada) son narrativos en 1-3 frases; los escenarios concretos los produce el análisis.

Cada sección se describe al nivel funcional adecuado:

| Sección | Qué SÍ va | Qué NO va |
|---------|-----------|-----------|
| **Entidades** | Nombre conceptual + descripción + campos funcionalmente relevantes sin tipo. | Tipos de campo, campos técnicos, FKs, IDs, anotaciones JPA. |
| **Operaciones** | Nombre funcional, quién la ejecuta, qué datos conceptuales necesita, qué efecto produce. | Nombres de clases, signaturas Java, tipos del framework, ubicación en capa. |
| **Flujos principales** | Descripción narrativa de un caso de uso completo de extremo a extremo, en 1-3 frases. Numerados `F-NNN`. Dicen **qué hace el usuario** y **qué hace el sistema** en respuesta. No son tests; son intención. | Pasos Given/When/Then (eso es análisis), nombres de pantallas concretas, botones, campos UI, mensajes literales, comandos `playwright-cli`. |
| **Pantallas** | Nombre funcional, quién la ve, filtro aplicado en lenguaje natural, modo (lectura/edición). | Nombres `@Main-action`, `@Search-grid`, `@View-form`, dominios JPQL. |
| **Menús** | Ítem de menú, ruta jerárquica, pantalla funcional destino, quién lo ve. | Nombres de acciones del framework. |
| **Seguridad** | Qué puede ver, crear, editar o borrar cada rol en lenguaje natural. Multicentro sí/no. | Reglas JPQL, condiciones del framework, nombres técnicos de permisos. |
| **Campos calculados** | Qué representa, lógica funcional de cálculo, de qué depende, cuándo se recalcula (funcionalmente). | Clases o métodos del framework. |
| **Requisitos (EARS)** | Frase con plantilla EARS (Ubicuo / Evento / Estado / No deseado / Opcional), numerada `E-XX-NNN`: trigger/condición funcional, sistema o entidad afectada, respuesta del sistema y, si aplica, mensaje al usuario. | Clasificación V/R/U, momento Antes/Después, capa, atributos XML. |

**Regla práctica ante una duda:** ¿el negocio cambiaría su decisión si el framework subyacente fuera distinto? Si la respuesta es **no**, va al diseño o al análisis (no a la especificación). Si es **sí**, va a la especificación.

### 2.4 Requisitos en formato EARS, agrupados por patrón y numerados

Los requisitos del sistema (lo que antes era una sola lista plana en prosa) se escriben en una sección única titulada **"Requisitos (EARS)"**, dividida en **5 subsecciones, una por patrón EARS**. Cada bullet usa la plantilla del patrón y lleva un identificador `E-XX-NNN` con numeración **local por patrón**, empezando en 001 y sin huecos. **Sigue sin haber clasificación V/R/U** — esa es tarea del análisis.

**EARS** (Easy Approach to Requirements Syntax) es una técnica de redacción de requisitos. Su valor aquí es disciplinar el **trigger** y la **condición** de cada requisito: cada frase deja explícito si es un invariante, una reacción a un evento, un comportamiento sostenido mientras dura un estado, una respuesta a algo no deseado, o un comportamiento que solo aplica con cierta feature. Esto reduce la ambigüedad que el análisis tendría que adivinar después.

#### 2.4.1 Las cinco subsecciones y sus plantillas

| Subsección | Prefijo | Plantilla literal (en español) |
|---|---|---|
| Ubicuos | `E-UB` | `El <sistema/entidad> debe <respuesta>.` |
| Dirigidos por evento | `E-EV` | `Cuando <trigger>, el <sistema/entidad> debe <respuesta>.` |
| Dirigidos por estado | `E-ST` | `Mientras <estado>, el <sistema/entidad> debe <respuesta>.` |
| Comportamiento no deseado | `E-UN` | `Si <condición indeseada>, entonces el <sistema/entidad> debe <respuesta>.` |
| Características opcionales | `E-OP` | `Donde <feature>, el <sistema/entidad> debe <respuesta>.` |

#### 2.4.2 Cómo elegir patrón EARS

Árbol de decisión, en orden de precedencia (la primera que aplique gana):

1. ¿Es un **rechazo** o un **error** (el sistema dice "no", impide o avisa de un mal uso)? → `E-UN` (`Si … entonces …`).
2. ¿Aplica solo cuando una **feature/configuración opcional** está activa? → `E-OP` (`Donde …`).
3. ¿Es una **reacción a un evento puntual** (algo que ocurre y dispara una respuesta)? → `E-EV` (`Cuando …`).
4. ¿Es un comportamiento **sostenido mientras dura un estado**? → `E-ST` (`Mientras …`).
5. ¿Es un **invariante siempre activo**, sin condición ni trigger? → `E-UB`.

**Reglas complejas** (combinaciones, p.ej. `Mientras S, cuando E, el sistema debe Y`): van en la subsección del verbo dominante. Si combinan `Si … entonces …`, gana `E-UN`.

#### 2.4.3 Numeración

- Numeración **local por patrón**, empezando en `001`: `E-UB-001`, `E-EV-001`, `E-EV-002`, `E-ST-001`, `E-UN-001`, `E-OP-001`…
- Cuando se borra un requisito **se conserva el número como hueco** (no se reutiliza) para no romper referencias desde análisis ya producidos en iteraciones anteriores.
- Cada subsección lleva los requisitos en una lista plana, un bullet por requisito.

#### 2.4.4 Inferencias

Los requisitos que el spec deduce sin que el usuario los haya enunciado explícitamente se marcan con un `*` **antes del ID** (p.ej. `*E-EV-007`) y se listan también en "Asunciones a confirmar" al final del documento.

#### 2.4.5 Lo que NO se hace en el spec

- **NO** se generan tablas `V-XXX`, `R-XXX` ni `U-XXX`. **NO** se clasifica nada en validaciones / reglas de negocio / reglas de UI.
- **NO** se indica si un requisito es bloqueante, ni el momento `Antes`/`Después`, ni la capa cliente/servidor, ni atributos XML.
- **NO** se mezclan patrones dentro de la misma subsección.

#### 2.4.6 Ejemplos

| MAL (es análisis/diseño) | BIEN (es especificación EARS) |
|---|---|
| `destinatario: String`, `fechaEnvio: LocalDateTime` | `destinatario`, `fecha de envío` |
| `V-001 \| motivoRechazo \| Siempre \| "El motivo es obligatorio"` | `E-UN-001 — Si una TareaFirma se rechaza sin motivo, entonces el sistema debe rechazar la operación con error "El motivo de rechazo es obligatorio".` |
| `R-002 \| Envía un correo \| TareaFirma \| cambiarEstado \| Después \| Solo si estado=APROBADO` | `E-EV-001 — Cuando una TareaFirma pasa al estado APROBADO, el sistema debe enviar un correo al interesado con el documento firmado.` |
| `U-001 \| continuo \| readonly \| descripcion \| estado != BORRADOR` | `E-ST-001 — Mientras una TareaFirma no está en estado BORRADOR, la pantalla Detalle debe mostrar la descripción en modo lectura.` |
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

### 4.3 Guard: ¿ya existe `specification.md`?

Antes de pasar a la Fase 1, comprobar si **ya existe** un `specification.md` en la carpeta de la iniciativa (`.sdd/drafts/{carpeta}/specification.md`). Si **no existe**, continuar normalmente con la Fase 1.

Si **sí existe**, **detener el flujo y preguntar al usuario con `AskUserQuestion`** entre dos opciones:

1. **Revisar el spec existente** (recomendado si el spec se editó a mano y solo quieres validar formato/numeración/plantillas EARS): el skill se **detiene** aquí e indica al usuario que lance `/sdd-specification-system-review` sobre el mismo fichero, preservando sus ediciones.
2. **Regenerar desde la historia de usuario** (pisa el spec actual): el skill **continúa** con la Fase 1 y siguientes; el `specification.md` antiguo será sustituido íntegramente en la Fase 4.

Mensaje exacto al usuario:

> Ya existe `specification.md` en `{carpeta}`. ¿Qué quieres hacer?
> - **Revisar el spec existente**: preserva tus ediciones, valida formato, plantillas EARS y numeración. Lanza `/sdd-specification-system-review` por separado.
> - **Regenerar desde la historia de usuario**: descarta el spec actual y vuelve a generarlo desde cero a partir de `user-story.md`.

Si el usuario elige "Revisar", responder literalmente:

```
Para revisar el spec existente sin perder tus ediciones, ejecuta:
  /sdd-specification-system-review .sdd/drafts/{carpeta}/specification.md
```

Y **detente**. No lances `/sdd-specification-system-review` tú mismo.

Si el usuario elige "Regenerar", continuar con la Fase 1.

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
- ¿Cuáles son los **flujos principales** del sistema (los casos de uso de extremo a extremo que un usuario debería poder completar)? Pregunta abierta. Cada flujo se describe en 1-3 frases narrativas: qué hace el usuario y qué hace el sistema en respuesta, **sin** mencionar pantallas, botones, campos UI ni mensajes concretos. Estos flujos son la semilla de los tests E2E que el análisis materializará después.
- ¿Qué **requisitos** hay sobre estas operaciones? Pregunta de forma abierta y mezclada: *"¿qué tiene que ser obligatorio, qué no se puede hacer, qué se calcula solo, qué cambia en la pantalla según el estado…?"*. Recoge todo y, al redactar, **encaja cada requisito en uno de los 5 patrones EARS** (Ubicuo / Evento / Estado / No deseado / Opcional) según el árbol de decisión §2.4.2. La clasificación V/R/U (validación / regla de negocio / regla de UI) la hará el análisis.
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
- Sabes cuáles son los flujos principales (casos de uso de extremo a extremo) del sistema.
- Sabes qué requisitos de negocio aplican (cada uno encajable en un patrón EARS; sin clasificar todavía en V/R/U).
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
- Los principios 2.1 (lenguaje de negocio), 2.3 (frontera spec/análisis/diseño) y 2.4 (requisitos en formato EARS, 5 subsecciones numeradas, sin clasificar V/R/U). **No** se transmite el principio 2.2: estos subagentes corren en paralelo y **no deben usar `AskUserQuestion`**. Si hay ambigüedad, eligen una interpretación razonable, la marcan con `*` antes del ID del requisito y la añaden a "Asunciones a confirmar".
- Las tareas internas (sección 7.2.1).
- La plantilla literal de salida (sección 7.2.2).
- El checklist (sección 7.2.3) y la instrucción de aplicarlo antes de devolver el resultado.
- La instrucción de devolver **una sola especificación completa** en su mensaje de respuesta, en markdown, sin metacomentarios y **sin escribir ningún fichero**.

#### 7.2.1 Tareas internas del subagente

Cada subagente ejecuta **estas tres tareas, en este orden**:

1. **Tarea 1 — Producir las secciones funcionales**: tipo y capa funcional, descripción, entidades (como conceptos, con campos funcionalmente relevantes **sin tipo**), dependencias de otros subsistemas, operaciones, pantallas, menús, seguridad (multicentro sí/no), máquina de estados (si aplica) y campos calculados (si aplica, descritos solo a nivel funcional).
2. **Tarea 1bis — Identificar los flujos principales del sistema**:
   - Un **flujo principal** representa un caso de uso completo de extremo a extremo (lo que un usuario hace de principio a fin para conseguir un objetivo de negocio: enviar un correo, rechazar una solicitud, firmar un documento…).
   - Se numera `F-001`, `F-002`… local al spec, sin huecos. Cuando se borra un flujo se conserva el número como hueco (no se reutiliza).
   - Cada flujo es una frase narrativa de **1 a 3 oraciones** que dice **qué hace el usuario** y **qué hace el sistema** en respuesta, **sin** mencionar pantallas, botones, campos UI ni mensajes literales.
   - Los flujos inferidos por el subagente (no enunciados explícitamente por el usuario) se marcan con `*` antes del ID y se listan también en "Asunciones a confirmar".
   - **NO** son tests Given/When/Then; son intención narrativa. La materialización en tests concretos la hace el análisis (`tests.md`) usando estos flujos + las pantallas + las V/R/U.
3. **Tarea 2 — Construir la sección "Requisitos (EARS)" y la lista de asunciones**:
   - Una sola sección titulada **"Requisitos (EARS)"** con **5 subsecciones**, una por patrón: Ubicuos (`E-UB`), Dirigidos por evento (`E-EV`), Dirigidos por estado (`E-ST`), Comportamiento no deseado (`E-UN`), Características opcionales (`E-OP`). Cada subsección lleva sus bullets numerados localmente desde `001`.
   - Cada bullet sigue **literalmente** la plantilla EARS de su patrón (ver §2.4.1) y empieza por su ID (`E-UB-001 — El sistema debe …`).
   - Para elegir patrón, aplicar el árbol de decisión §2.4.2 (gana `E-UN` ante rechazos/errores, luego `E-OP`, luego `E-EV`, luego `E-ST`, luego `E-UB`). Las reglas complejas van en la subsección del verbo dominante manteniendo plantilla compuesta.
   - Cada requisito incluye lo mínimo funcional: el trigger/estado/condición, el sistema o entidad afectada, la respuesta del sistema y, si aplica, el mensaje que el usuario vería.
   - Los requisitos inferidos se marcan con `*` **antes del ID** (p.ej. `*E-EV-007`) y se listan también en "Asunciones a confirmar".
   - **NO** se clasifica V/R/U. **NO** se mezclan patrones dentro de una subsección. **NO** se reutilizan números de huecos eliminados.
4. **Tarea 3 — Aplicar el checklist (sección 7.2.3) y corregir antes de devolver**. El subagente no debe devolver la especificación si queda algún punto del checklist sin cumplir.

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

### Flujos principales
- F-001 — <Una a tres frases narrativas: el usuario hace X, el sistema responde Y, ambos completan el caso de uso de extremo a extremo>.
- *F-002 — <Flujo inferido por el spec sin que el usuario lo haya enunciado explícitamente>.  *(inferido)*

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

### Requisitos (EARS)

#### Ubicuos (E-UB)
- E-UB-001 — El <sistema/entidad> debe <respuesta>.
- *E-UB-002 — El <sistema/entidad> debe <respuesta>.  *(requisito inferido)*

#### Dirigidos por evento (E-EV)
- E-EV-001 — Cuando <trigger>, el <sistema/entidad> debe <respuesta>.

#### Dirigidos por estado (E-ST)
- E-ST-001 — Mientras <estado>, el <sistema/entidad> debe <respuesta>.

#### Comportamiento no deseado (E-UN)
- E-UN-001 — Si <condición indeseada>, entonces el <sistema/entidad> debe <respuesta>.

#### Características opcionales (E-OP)
- E-OP-001 — Donde <feature>, el <sistema/entidad> debe <respuesta>.

### Asunciones a confirmar
- <Los flujos `*F-NNN` y los requisitos `*E-XX-NNN` marcados como inferidos, y otras decisiones marcadas con `*`, repetidos aquí con justificación breve>.
```

> Nota: si una subsección EARS no tiene ningún requisito en esta especificación, se omite la subsección entera (no se deja vacía).

#### 7.2.3 Checklist del subagente

- [ ] ¿Cada entidad describe qué representa y enumera sus campos funcionalmente relevantes **sin tipo**?
- [ ] ¿La especificación está libre de campos técnicos (IDs, FKs internas, auditoría, flags de control, versiones)?
- [ ] ¿La sección **"Flujos principales"** lista al menos un flujo `F-NNN` y cada uno se describe en 1-3 frases narrativas?
- [ ] ¿Los flujos principales están libres de nombres de pantalla concretos, botones, campos UI, mensajes literales y pasos Given/When/Then? (Esa materialización es del análisis.)
- [ ] ¿Los flujos inferidos están marcados con `*` antes del ID y listados también en "Asunciones a confirmar"?
- [ ] ¿La numeración `F-NNN` es local al spec, empieza en `001` y no reutiliza huecos eliminados?
- [ ] ¿La sección "Requisitos (EARS)" tiene exactamente las 5 subsecciones (Ubicuos / Eventos / Estados / No deseado / Opcionales), omitiendo solo las que no contienen ningún requisito?
- [ ] ¿Cada requisito empieza con un ID `E-UB-NNN` / `E-EV-NNN` / `E-ST-NNN` / `E-UN-NNN` / `E-OP-NNN`, con numeración local por patrón desde 001 y sin huecos en esta especificación?
- [ ] ¿Cada requisito sigue **literalmente** la plantilla de su patrón (`El … debe …`, `Cuando …, el … debe …`, `Mientras …, el … debe …`, `Si …, entonces el … debe …`, `Donde …, el … debe …`)?
- [ ] ¿Cada requisito está en la subsección correcta según el árbol de decisión §2.4.2 (rechazos/errores en `E-UN`, opcionales en `E-OP`, eventos en `E-EV`, estados en `E-ST`, invariantes en `E-UB`)?
- [ ] ¿Los requisitos están escritos en lenguaje que un supervisor entendería sin formación técnica?
- [ ] ¿Los requisitos inferidos (no explícitos en la historia) están marcados con `*` antes del ID y listados en "Asunciones a confirmar"?
- [ ] ¿La especificación está libre de cualquier indicación de **bloqueante / no bloqueante**, **Antes/Después**, **capa cliente/servidor** o **clasificación V/R/U**? La clasificación V/R/U es responsabilidad del análisis.
- [ ] ¿La sección de operaciones está libre de nombres de clase, signaturas de método, tipos del framework y referencias a capas técnicas?
- [ ] ¿La sección de pantallas está libre de nombres técnicos del framework Axelor (`@Main-action`, `@All-action`, `@Search-grid`, `@View-form`, `@Main-form`)?
- [ ] ¿La sección de menús describe la asociación menú → pantalla funcional, sin nombres de acciones del framework?
- [ ] ¿La sección de seguridad está descrita en lenguaje natural, sin JPQL ni expresiones de código?
- [ ] ¿La sección de campos calculados describe la lógica funcional sin mencionar clases ni métodos del framework?
- [ ] ¿Los requisitos están libres de atributos XML (`showIf`, `requiredIf`, `<action-attrs>`, `<action-record>`) y nombres de método (`fireActionRule_*`, `insert`/`update`/`validateInsert`)?
- [ ] ¿No se documentan requisitos que el framework ya cubre por su propia naturaleza (FK válida, parser de tipo, formato de fecha)?
- [ ] ¿No hay dependencias circulares entre sistemas/subsistemas?
- [ ] ¿Las pantallas son coherentes con las entidades y operaciones descritas?
- [ ] ¿Hay ambigüedades de **negocio** que bloquearían el análisis? Si las hay, deben quedar listadas como asunciones a confirmar.

### 7.3 Etapa B — Unificación

Una vez recibidas las 5 candidaturas, **el agente principal** (no un subagente) produce la especificación final unificada:

1. **Compara las 5 especificaciones** entidad por entidad, sección por sección.
2. **Para cada decisión donde haya divergencia**, escoge la mejor opción según el criterio funcional: claridad para negocio, ausencia de detalles técnicos, fidelidad a la historia de usuario. Cuando haya empate razonable, elige la opción que minimiza ambigüedad para el análisis.
3. **Para la sección "Flujos principales"**, consolida los flujos de las 5 candidaturas:
   - Si un mismo flujo aparece redactado de distintas formas, elige la versión más narrativa y libre de tecnicismos.
   - Si una candidatura propone un flujo que las demás no, evalúa si es genuino (incluirlo) o si es una invención (incluirlo con `*` antes del ID y añadirlo a "Asunciones a confirmar").
   - **Renumera** `F-001`, `F-002`… sin huecos. Los IDs que vinieran de los subagentes no son vinculantes: el ID final lo asignas tú al consolidar.
4. **Para la sección "Requisitos (EARS)"**, consolida en las 5 subsecciones EARS:
   - Si un requisito equivalente aparece en varias candidaturas con redacciones distintas (o incluso clasificado en distintos patrones EARS), elige el patrón correcto según el árbol §2.4.2 y la redacción más precisa.
   - Si un requisito aparece en algunas candidaturas pero no en otras, evalúa si es genuino (incluirlo) o si es una asunción agresiva (incluirlo con `*` antes del ID).
   - **Renumera** dentro de cada patrón empezando en `001` y sin huecos. Los IDs que vinieran de los subagentes no son vinculantes: el ID final lo asignas tú al consolidar.
   - **No clasifiques en V/R/U**. La clasificación V/R/U sigue siendo trabajo del análisis.
5. **Para asunciones a confirmar**, agrupa todas las asunciones marcadas con `*` de las 5 candidaturas, elimina duplicados y razónalas.
6. **Resuelve dudas con `AskUserQuestion`** si en la unificación detectas algo ambiguo que ninguna candidatura resolvió de forma satisfactoria. Aquí sí puedes preguntar (estás en el agente principal, no en paralelo).
7. **Aplica el checklist final (7.2.3)** sobre la especificación unificada — la unificación puede haber introducido inconsistencias (redacciones mezcladas, asunciones combinadas, campos técnicos colados al consolidar) que ningún subagente individual podía detectar.
8. **Presenta el borrador al usuario para su aprobación.** No se guarda hasta tener el visto bueno.

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

Para generar el análisis (entidades formales, pantallas estructuradas, tablas V-XXX/R-XXX/U-XXX con trazabilidad a los requisitos EARS, y tests E2E `tests.md` materializados a partir de los flujos principales) ejecuta:
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
