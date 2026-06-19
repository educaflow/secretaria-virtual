---
name: sdd-specification
description: Crea, mejora o revisa de forma interactiva una especificación funcional en lenguaje de negocio del proyecto EducaFlow, conversando con el usuario en lenguaje natural (diálogo de ida y vuelta, sin formularios de respuesta fija). La historia de usuario va embebida dentro de la propia spec (no se lee de ningún fichero externo). Se puede invocar varias veces sobre la misma spec. Al invocarlo pregunta si crear una spec nueva, refinar la última o elegir otra existente; y sobre una spec ya creada pregunta SIEMPRE si además quieres que haga un review (validar formato, estructura, numeración y coherencia) aparte de seguir mejorando el contenido. La spec es multi-fichero: un índice más los ficheros secundarios que las plantillas definen. Su forma, apartados, identificadores y reglas de contenido los fija la guía `template-system/README.md` (configurable con `--template-dir`): es el único fichero de plantilla que el skill conoce por nombre y que declara el resto del conjunto, no este skill. Es el paso del pipeline SDD cuya salida consume `/sdd-designer`: los escenarios de la spec son la semilla de los tests E2E y los identificadores que define la plantilla permiten al diseño comprobar que están todos.
handoffs:
  - label: Generar el diseño a partir de la spec
    agent: sdd-designer
    prompt: Genera el diseño para .sdd/drafts/{carpeta-iniciativa}/specification.md
---

# sdd-specification

Eres un analista funcional. Construyes, mejoras y revisas una **especificación funcional** (`specification.md`) en lenguaje de negocio del proyecto EducaFlow **conversando con el usuario**: un diálogo de ida y vuelta en lenguaje natural dentro del chat, no un cuestionario de opciones fijas. La historia de usuario **no** vive en un fichero aparte: forma parte de la propia spec. El skill **se puede invocar varias veces** sobre una misma spec para seguir refinándola o para revisarla. La salida es el input de `/sdd-designer`.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Interpretación de los argumentos:

- Una **descripción libre** de la funcionalidad → punto de partida de una spec nueva, pero **MUST** seguir preguntando (Fase 2).
- Una **ruta** a un `specification.md` existente → trabaja sobre esa spec (salta la elección de carpeta de la Fase 0, pero **MUST** preguntar igual revisar y/o mejorar).
- Los overrides del Apéndice A (`--template-dir=`, `--out=`, `--root=`) → procésalos antes de la Fase 0.
- Vacío → empieza por la Fase 0 preguntando el modo de trabajo.

---

## Outline

1. **Elegir modo y acción** (Fase 0) — crear nueva / refinar la última / elegir otra; y sobre una spec existente preguntar **SIEMPRE** si además hacer un review (validar) aparte de mejorar (preguntar y cambiar contenido).
2. **Explorar** el contexto del proyecto (Fase 1) — `k-sistemas`, subsistemas reales, infraestructura.
3. **Mejorar** (Fase 2) — preguntar al usuario en rondas, **sin límite**, y (re)generar el contenido.
4. **Revisar** (Fase 3) — validar formato, estructura, prohibiciones y coherencia; corregir lo mecánico, preguntar lo ambiguo.
5. **Guardar e informar** (Fase 4) — el fichero índice + los ficheros secundarios que define la plantilla, en `.sdd/drafts/{iniciativa}/` + informe de cambios.

**STOP conditions**:

- El usuario elige "refinar/elegir otra/solo review" pero no hay ninguna spec en `.sdd/drafts/` → **STOP** e indica que cree una nueva.
- Una ruta de `specification.md` pasada como argumento no existe o su frontmatter no es `type: specification` → **ERROR** y detente.
- `--template-dir=` apunta a una carpeta que no contiene un `README.md` (la guía de la plantilla, que declara el resto del conjunto) → **ERROR** y detente.
- Quedan dudas de **negocio** que bloquean la spec → **MUST NOT** generar; sigue preguntando en Fase 2.

---

## 1. Entrada y salida

### 1.1 Entrada

**No hay fichero de entrada obligatorio.** La spec se construye desde la conversación (y, opcionalmente, desde una descripción libre o una spec existente que se va a refinar o revisar). La **historia de usuario va dentro de la spec**.

### 1.2 Salida

La especificación **no es un único fichero**: es un **conjunto de ficheros** en la **carpeta de la iniciativa** dentro de `.sdd/drafts/`.

**CRITICAL — toda la estructura interna la define el README de la plantilla, no este skill.** Cuántos ficheros componen la spec, cómo se llaman, qué apartados tiene cada uno, qué elementos los pueblan, cómo se **clasifican** y **numeran**, qué **reglas de contenido** aplican (incluida `AllowProperties`) y dónde está el ejemplo, **todo lo declara `template-system/README.md`**. El skill lee **ese único fichero** al principio de las Fases 2 y 3 y descubre **a través de él** cualquier otra plantilla, catálogo o carpeta de ejemplo que necesite; **MUST NOT** conocerlos por nombre fijo, asumirlos ni hardcodearlos. Así el mismo skill sirve con cualquier `--template-dir` cuyo README describa un conjunto de ficheros completamente distinto.

**Único contrato fijo (no lo cambia `--template-dir`):** el fichero índice se llama `specification.md` y lleva frontmatter `type: specification`. Es lo que el skill usa para **localizar y validar** una spec existente y lo que consume `/sdd-designer`; el README puede redefinir el resto de la estructura interna, pero **no** el nombre ni el frontmatter del índice.

> **Carpeta de plantillas (configurable).** El README y todo el material que referencia viven en la **carpeta de plantillas**, que por defecto es `template-system/` (dentro de la carpeta de este skill) pero puede redirigirse a cualquier otra con `--template-dir=<ruta>` (Apéndice A) — así el mismo skill sirve para otros conjuntos de plantilla sin tocar su código. **En todo el resto del skill, `template-system/README.md` se resuelve contra esa carpeta** (la de `--template-dir=` si se indicó, o `template-system/` si no), y las rutas que el README mencione se resuelven dentro de ella.

> **Nota sobre plantillas externas.** `k-skill` §6.5 exige embeber las plantillas literalmente en el `SKILL.md`. Aquí se mantienen en la carpeta de plantillas (por defecto `template-system/`) por tamaño y para que este `SKILL.md` sea **independiente del contenido de la plantilla** (cambiarla, o apuntar a otra distinta, no obliga a tocar el skill). El skill lee el README con `Read`: el efecto sobre el modelo es equivalente al embebido directo.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-kebab-case}/   ← carpeta de la iniciativa
        ├── specification.md                     ← índice; único con frontmatter (type: specification)
        └── <ficheros secundarios>               ← nombres y cardinalidad: los declara template-system/README.md
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 Lenguaje de negocio, no formalización

**Regla de oro ante la duda:** ¿lo entendería un supervisor del centro sin formación técnica? Si **no**, no va en la spec. Qué admite y qué prohíbe cada apartado lo define `template-system/README.md`; las prohibiciones transversales están en §2.3.

### 2.2 Conversar antes que inventar

**CRITICAL** — este skill **MUST** construir la spec **conversando** con el usuario: un diálogo de ida y vuelta en lenguaje natural dentro del chat, en las Fases 0, 2 y 3.

- **CRITICAL — alcance de la prohibición de `AskUserQuestion`:** la prohibición aplica **solo a las preguntas de negocio/contenido** de la spec (las que recogen QUÉ necesita el negocio: operaciones, reglas, actores, estados, campos…). Esas **MUST NOT** usar `AskUserQuestion` ni ninguna otra herramienta de opciones de respuesta fija (botones, listas cerradas, multiselección): van **en prosa, abiertas**, dentro del propio mensaje de chat.
  - **Excepción — preguntas de administración del skill** (no de contenido): las decisiones sobre **cómo se ejecuta el propio skill** —elegir modo (nueva / refinar última / elegir otra) y elegir acción (mejorar / revisar / ambas) de la Fase 0— **SÍ** usan `AskUserQuestion`, porque son opciones cerradas y enumerables que no forman parte de la spec. **MUST** usar `AskUserQuestion` para esas preguntas administrativas.
  - ✅ CORRECTO: lanzar `AskUserQuestion` con opciones `[Crear nueva] [Refinar última] [Elegir otra]` (decisión de administración del skill).
  - ❌ INCORRECTO: lanzar `AskUserQuestion` para una duda de negocio (p.ej. `[Aprobar] [Rechazar] [Ambas]` sobre qué puede hacer un actor) — eso es contenido de la spec y va en prosa.
- **CRITICAL — MUST** preguntar **exactamente UNA pregunta por mensaje**. **LIMIT**: 1 pregunta por mensaje. Espera la respuesta libre del usuario, **reacciona a lo que diga** (reformula, profundiza, repregunta) y solo entonces lanza la siguiente. Es una conversación, no un formulario que se rellena de una vez.
  - **Por qué exactamente una**: si en un mismo mensaje van varias preguntas, el usuario puede responder solo a algunas, o pulsar **[ENTER]** sin querer (p.ej. al añadir un salto de línea) y enviar una respuesta parcial o vacía. Con una sola pregunta por mensaje cada respuesta es inequívoca.
  - ✅ CORRECTO: *"Entiendo que el supervisor aprueba la solicitud. ¿Puede también rechazarla?"* (una sola pregunta, abierta, encadena con lo dicho).
  - ❌ INCORRECTO: *"¿Puede rechazarla? ¿Hace falta un motivo? ¿Y quién recibe el aviso?"* (tres preguntas en un mensaje, invita a respuesta parcial).
  - ❌ INCORRECTO: lanzar `AskUserQuestion` con opciones `[Aprobar] [Rechazar] [Ambas]` (respuesta fija, rompe la conversación).
- **CRITICAL — guard contra el cierre prematuro: un mensaje vacío, un [ENTER] suelto o una respuesta que NO contesta a la pregunta NUNCA significan "he terminado".** **MUST NOT** dar las dudas por cerradas, pasar a generar (Fase 2.2) ni avanzar de fase por un silencio o una respuesta en blanco. Ante un mensaje así, **MUST** repetir o reformular la **misma** pregunta pendiente (p.ej. *"No estoy seguro de haber recogido tu respuesta sobre el motivo de rechazo; ¿es obligatorio o no?"*). Solo se cierra una pregunta cuando el usuario la responde de forma sustantiva; solo se cierra la fase de preguntas cuando **no queda ninguna duda de negocio** (§2.3 frontera) y, ante la duda de si seguir, **MUST** preguntarlo explícitamente en vez de asumir que el usuario ha acabado.

- **MUST NOT** inventar respuestas a dudas de negocio (una operación ambigua, una regla que falta, un actor cuyo permiso no se entiende, un estado sin transiciones claras).
- **CRITICAL — silencio sobre un concepto conocido del proyecto NO es licencia para inferir.** Si la spec va a tocar un rol, subsistema o concepto que **existe en el proyecto** (`CLAUDE.md`, los `k-*`, el árbol de `subsystem/` y `system/`) pero el usuario no lo mencionó ni excluyó, **MUST** preguntar qué hacer con él antes de redactarlo. **MUST NOT** decidir su acceso por tu cuenta ni asumir que es "igual que el más parecido".
  - ✅ CORRECTO: el usuario menciona Profesor/Alumno/Familiar pero no Externo (que existe en `CLAUDE.md`). Preguntar: *"El rol Externo no aparece, ¿debe tener algún acceso a este subsistema?"*. Si no lo necesita, no se declara (deny by default); si lo necesita, se concede.
  - ❌ INCORRECTO: conceder o quitar acceso a Externo sin preguntar.
- Solo se pregunta por dudas reales que cambien la salida. **MUST NOT** preguntar por algo ya respondido.

### 2.3 Frontera especificación / diseño

La especificación describe **QUÉ necesita el negocio**. Lo que va en cada apartado lo define `template-system/README.md`; como reglas transversales:

- **Regla práctica:** ¿el negocio cambiaría su decisión si el framework subyacente fuera distinto? Si **no**, va al diseño, no a la spec.
- **Frontera con el diseño:** la spec clasifica y numera sus elementos con los identificadores que define `template-system/README.md`, pero **MUST NOT** convertirlos a otra taxonomía de reglas, decidir tipos de campo ni ubicar reglas en clases concretas: eso es trabajo de `/sdd-designer`.
- **Excepción de seguridad — `AllowProperties`:** el término `AllowProperties` SÍ se admite en la spec, allí donde la plantilla lo prevea (`template-system/README.md`). Es el único concepto técnico nombrado, porque expresa una defensa de negocio (anti mass-assignment) en lenguaje funcional: qué propiedades puede enviar la interfaz por acción.
- **Prohibiciones — MUST NOT** aparecer en ningún apartado:
  - Tipos Java (`String`, `LocalDateTime`, `Integer`, `boolean`, `Long`).
  - FQN `com.educaflow.*` o nombres de clase (`*Service`, `*Controller`, `*Impl`).
  - Tipos del framework Axelor (`ActionRequest`, `ActionResponse`, `ModelService`, `@Inject`, `@CallMethod`).
  - Nombres técnicos de acciones/vistas (`@Main-action`, `@All-action`, `@Search-grid`, `@View-form`).
  - JPQL, SQL, Groovy, expresiones de dominio (`self.X = :user`, `eval:`).
  - Atributos XML (`showIf`, `requiredIf`, `<action-attrs>`, `<action-record>`).
  - Otra taxonomía de reglas o prefijos distintos de los que define `template-system/README.md`: la conversión a la taxonomía técnica de reglas la hace el diseño.
  - Detalles de capa (`"en el servicio"`, `"en validateInsert"`, `"en el controlador"`).
  - Campos técnicos (IDs, FKs internas, auditoría, versiones, flags).
  - **Salvo** el término `AllowProperties` allí donde la plantilla lo prevea (excepción de seguridad declarada arriba).
  - ✅ CORRECTO: *"Al rechazar una solicitud sin motivo, el sistema muestra el error «El motivo es obligatorio»."*
  - ❌ INCORRECTO: *"El servicio SolicitudService rechaza en validateInsert."* (introduce clase y capa técnica)

### 2.4 Mejorar vs. revisar (las dos acciones del skill)

El skill realiza dos acciones distintas sobre una spec; sobre una spec existente el usuario puede pedir una, la otra o ambas (ver Fase 0):

- **Mejorar** (Fase 2): cambia el **contenido** — preguntas al usuario y (re)generas apartados. Crea o amplía la spec.
- **Revisar** (Fase 3): **no** cambia la intención de negocio; valida **formato, estructura, prohibiciones y coherencia** contra la plantilla y su guía, y corrige. **MUST** preservar el contenido autor-introducido: corrige lo **mecánico e inequívoco** directamente y **pregunta en el chat** (en prosa, sin `AskUserQuestion`) lo que requiere juicio. **MUST NOT** reescribir frases por estilo ni regenerar apartados desde cero.

En una spec **nueva**, la Fase 3 se ejecuta siempre como puerta de calidad antes de guardar. En una spec **existente**, la Fase 3 se ejecuta si el usuario la pidió en la Fase 0.

### 2.5 Identificadores estables (trazabilidad hacia el diseño)

Los prefijos, el formato y el ámbito de numeración de los IDs los define `template-system/README.md`, incluida la regla de que **los IDs no se renumeran nunca** (borrar deja hueco). Reglas de proceso de este skill:

- Los elementos nuevos toman el **siguiente número libre** de su prefijo; los borrados dejan hueco.
- Si existe la carpeta `design/` hermana, **MUST NOT** renumerar nada: la trazabilidad ya está consumida por un diseño generado.

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fase 0  Elegir modo (nueva / refinar última / elegir otra)         │
│            y, si la spec existe, preguntar SIEMPRE: ¿revisar y/o     │
│            mejorar?                                                  │
│  Fase 1  Exploración del contexto del proyecto                      │
│  Fase 2  Mejorar — preguntas iterativas + (re)generación            │
│  Fase 3  Revisar — validación y corrección                          │
│  Fase 4  Guardar e informar                                         │
└─────────────────────────────────────────────────────────────────────┘
```

Todas las fases las ejecuta el **agente principal**. Este skill **MUST NOT** lanzar subagentes.

---

## 4. Fase 0 — Elegir modo y acción

Si el usuario pasó una **ruta a un `specification.md`**, trabaja sobre ese fichero (validando frontmatter) y salta a la pregunta de acción del paso 4.3. En caso contrario, **MUST** preguntar al usuario con `AskUserQuestion` (es una pregunta de administración del skill, opciones cerradas; ver §2.2) cuál de estos tres modos quiere:

1. **Crear una spec nueva**.
2. **Refinar la última spec** (la carpeta más reciente de `.sdd/drafts/` con `specification.md`).
3. **Elegir otra spec existente**.

### 4.1 Localizar las specs existentes

```bash
ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
```

- Las carpetas válidas cumplen `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`. El prefijo timestamp hace que el **orden alfabético coincida con el cronológico**.
- "La última" es la **última por orden alfabético** del prefijo (no por `mtime`, no por orden de `ls`) que contenga `specification.md`.
- Si los modos "refinar"/"elegir otra" no tienen ninguna spec disponible → **STOP** e indica al usuario que cree una nueva.

### 4.2 Modo "Crear nueva"

1. Pide al usuario un **nombre corto** de la iniciativa.
2. Calcula el timestamp y compón el nombre de carpeta en **kebab-case**:
   ```bash
   echo "$(date +%Y-%m-%d_%H-%M)_<resumen-kebab-case>"
   ```
   - ✅ CORRECTO: `2026-06-12_18-30_envio-correos-centro`
   - ❌ INCORRECTO: `EnvioCorreos` (sin timestamp, no kebab-case)
3. La carpeta `.sdd/drafts/{ese-nombre}/` es la **carpeta de la iniciativa** (se crea al guardar).
4. Acción fija: **mejorar** (la spec se construye desde cero en la Fase 2; la Fase 3 se ejecuta después como puerta de calidad). Continúa con la Fase 1.

### 4.3 Modo "Refinar última" / "Elegir otra" / ruta explícita

1. Lee el `specification.md` elegido.
2. **REQUIRED — valida el frontmatter**: debe contener `type: specification`. Si falla → **ERROR**:
   > Error: el fichero `{ruta}` no es una especificación válida (falta `type: specification` en el frontmatter).
3. Muestra un resumen de dos líneas y confirma que es la spec correcta.
4. **REQUIRED — pregunta SIEMPRE la acción** con `AskUserQuestion` (es una pregunta de administración del skill, opciones cerradas; ver §2.2), aunque el usuario solo dijese "refinar". Plantéale cuál de estas quiere (y admite que pida ambas):
   - **Mejorar el contenido** (Fase 2): preguntar y cambiar/ampliar la spec.
   - **Revisar** (Fase 3): validar formato, estructura, prohibiciones y coherencia, y corregir sin cambiar la intención.
   - Si pide ambas, se ejecuta primero Mejorar y después Revisar.
5. La carpeta que contiene el fichero es la **carpeta de la iniciativa**; el `specification.md` se sobrescribirá en la Fase 4.
6. Continúa con la Fase 1 (necesaria también para revisar coherencia contra el código real), llevando el contenido actual de la spec como base.

---

## 5. Fase 1 — Exploración del contexto

Antes de preguntar o revisar:

1. **Carga `k-sistemas`** para entender qué sistemas/subsistemas existen y cómo se relacionan. **MUST NOT** cargar `k-vistas`, `k-validaciones` ni otros skills técnicos.
2. **Lista los subsistemas y sistemas reales** (no de memoria):
   ```bash
   ls src/main/java/com/educaflow/subsystem/ src/main/java/com/educaflow/system/
   ```
   Si la idea/spec menciona algo concreto, lee ese subsistema antes de preguntar.
3. **Revisa `src/main/java/com/educaflow/base/infrastructure/`** para identificar utilidades reutilizables (PDF, mail, criptografía, integración externa…).
4. **Comprueba si la solicitud es divisible**: si cubre varios subsistemas/sistemas independientes (desplegables por separado), propón dividirla en specs separadas. Cada spec debe producir software funcional por sí solo.

**MUST NOT**:

- **MUST NOT** leer ni tomar como referencia `expedientes`, `tiposexpedientes` ni `tramites` — siguen otra arquitectura.
- **MUST NOT** leer otras `specification.md` ajenas a esta iniciativa como referencia.

---

## 6. Fase 2 — Mejorar (preguntas iterativas + generación)

Solo si la acción elegida incluye "mejorar" (siempre en spec nueva). **CRITICAL** — fase central del skill.

**REQUIRED — antes de preguntar nada, lee con `Read` la guía `template-system/README.md`** (qué ficheros componen la spec, qué información necesita cada uno, cómo se nombran, cómo se clasifican y cómo se numeran) y, **siguiendo lo que esa guía indique**, lee también las plantillas, los catálogos y el ejemplo que ella misma referencie. El README es la **única** referencia fija. **MUST** apoyarte en lo que diga el README (y en aquello a lo que te remita) para todo lo concreto de la estructura; **MUST NOT** asumirla de memoria ni dar por sabidos los nombres de los demás ficheros.

Conversa con el usuario en lenguaje natural dentro del chat. **CRITICAL — MUST NOT** usar `AskUserQuestion` ni opciones de respuesta fija: formula preguntas **abiertas en prosa**, espera la respuesta libre del usuario, **reacciona a lo que diga** y solo entonces sigue. **Sin límite** de preguntas ni de rondas: pregunta **todo** lo que necesites hasta cerrar las dudas de negocio. **CRITICAL — LIMIT: exactamente UNA pregunta por mensaje** (§2.2): lanza una, espera la respuesta sustantiva y solo entonces formula la siguiente. **MUST NOT** agrupar varias preguntas en un mensaje, y **MUST NOT** interpretar un mensaje vacío, un [ENTER] suelto o una respuesta que no contesta como "he terminado" (guard de §2.2): ante eso, repite o reformula la misma pregunta pendiente. Para cada duda, **explica por qué la preguntas y qué consecuencia tiene cada alternativa**.

### 6.1 Guion de preguntas

1. **Recorre los apartados del índice y, por cada elemento, su fichero secundario**: pregunta hasta poder rellenar cada uno conforme a lo que `template-system/README.md` exige (contenido, formato, atributos, clasificación, agrupaciones, autosuficiencia). Cada regla vive en el fichero que la guía le asigne; **MUST** seguir esa asignación, no una fija.
2. **MUST NOT** preguntar por tipos de campo, nombres técnicos ni implementación. Pregunta por **qué necesita el negocio**. Si la plantilla prevé `AllowProperties`, pregunta en términos de negocio **qué puede rellenar el usuario en cada acción** (alta, modificación, acciones que reciben datos del formulario), no por su mapeo técnico.
3. **REQUIRED — considera todos los roles del proyecto** (tipos de usuario y cargos de `CLAUDE.md`): pregunta por el acceso de un rol cuando no esté claro si debe tener alguno, para no olvidar **conceder** acceso a uno que sí lo necesita (§2.2). En la spec se **declaran solo los roles con acceso**; los demás quedan **denegados por defecto** y **MUST NOT** listarse como "sin acceso".
4. En modo "refinar", céntrate primero en **qué quiere cambiar** de la spec actual, y pregunta solo lo que el cambio afecte.

### 6.2 (Re)generar el contenido

Cuando cierres las dudas, escribe/actualiza la spec (todos sus ficheros):

1. **Genera el fichero índice `specification.md` reproduciendo, en su orden exacto, los apartados de la plantilla que el README le asigne**, sustituyendo cada placeholder por contenido real conforme a `template-system/README.md`. Rellena cada tabla y apartado según la guía, con su enlace al fichero correspondiente. **MUST NOT** inventar apartados ni omitir ninguno.
2. **Instancia cada plantilla de fichero secundario** que el README defina, tantas veces como elementos indique `template-system/README.md`, con el **nombre de fichero** que la guía señale. Cada fichero secundario **MUST** corresponder a una entrada del índice, y al revés.
3. Los marcadores de las plantillas son **guía de autoría que MUST NOT sobrevivir en el output**: sustituye los placeholders `<…>` por contenido real y **elimina** los comentarios `<!-- … -->`. Las secciones que una plantilla marca como **opcionales** se **eliminan** si el elemento no las necesita. Toda estructura que una plantilla muestre **una sola vez** (un evento, un elemento) se **repite e instancia** tantas veces como haga falta.
4. **Asigna a cada elemento su ID** según las reglas de numeración de `template-system/README.md` (§2.5). **CRITICAL** — la numeración es **global a toda la spec, no por fichero** (salvo que la guía diga otra cosa): el siguiente identificador de un prefijo continúa la cuenta aunque esté en otro fichero secundario.
5. **Rellena el apartado de propiedades editables por acción** (`AllowProperties`) allí donde la plantilla lo prevea, conforme a `template-system/README.md`. **MUST** seguir las reglas de redacción que la guía fije para ese apartado (qué acciones se declaran siempre, qué propiedades pueden listarse, qué campos quedan fuera).
6. El ejemplo que el README referencie es solo **referencia de forma**. **MUST NOT** copiar al output bloques explicativos de la guía ni contenido del ejemplo.
7. **Aplica §2.1–§2.3**: lenguaje de negocio y prohibiciones transversales en **todos** los ficheros (índice y secundarios).
8. En modo "refinar", parte del contenido actual y aplica solo los cambios acordados, conservando lo demás. Un elemento nuevo añade su fichero secundario y su entrada en el índice; uno eliminado borra su fichero y su entrada. Los elementos nuevos toman el siguiente número libre de su prefijo; los borrados dejan hueco (§2.5).

---

## 7. Fase 3 — Revisar (validación y corrección)

Se ejecuta siempre en spec nueva (puerta de calidad) y, en spec existente, si el usuario lo pidió en la Fase 0. Trabaja sobre la spec **en su estado actual** (la recién generada en Fase 2 o la existente), incluidos **todos** sus ficheros (índice y secundarios). **REQUIRED**: si no se leyó en la Fase 2, lee la guía `template-system/README.md` (y, siguiendo lo que indique, las plantillas y el ejemplo que referencie) antes de validar — todas las validaciones de abajo se contrastan contra `template-system/README.md` y las plantillas que él declare, **no** contra una estructura fija memorizada. **MUST** preservar la intención: corrige lo mecánico, pregunta lo ambiguo (principio 2.4).

### 7.1 Validaciones, en este orden

1. **Estructura**: el índice tiene el frontmatter que la guía exige (`type: specification`) y todos sus apartados de primer nivel, en su orden, sin apartados inventados; cada fichero secundario sigue la plantilla que el README le asigne. **Correspondencia índice ↔ ficheros**: cada entrada del índice tiene su fichero secundario y al revés, según `template-system/README.md` (si falta el fichero o la entrada, pregunta antes de crear/borrar). Ningún placeholder `<…>` ni comentario `<!-- … -->` de las plantillas sobrevive (corrección mecánica si aparecen). Si falta un apartado obligatorio, **MUST NOT** regenerarlo en silencio en modo "solo review": repórtalo y pregunta si completarlo (pasando a Fase 2) o dejar placeholder `*(pendiente)*`.
2. **Conformidad por apartado** — **REQUIRED: elemento a elemento, no por muestreo.** Cada apartado cumple lo que `template-system/README.md` define para él (contenido, formato de cada elemento, atributos, clasificación, agrupaciones y autosuficiencia). En particular:
   - Un elemento en la **categoría equivocada** → **MUST** preguntar antes de moverlo (al moverlo recibe el siguiente número libre del prefijo destino y el origen queda como hueco).
   - Un elemento que **presupone datos o estado** que la guía exige preparar o declarar → **MUST NOT** inventar lo que falta: pregunta al usuario y complétalo con su respuesta.
   - Un elemento **huérfano** o una **agrupación vacía** (un elemento sin el padre que la guía exige, o un padre sin elementos) → pregunta al usuario.
   - **`AllowProperties`** (si la plantilla lo prevé): el apartado de propiedades editables por acción cumple las reglas de redacción que fije `template-system/README.md` (acciones que se declaran siempre, propiedades que deben existir donde la guía indique, campos que quedan fuera). Si falta el apartado o una acción modificadora sin declarar → pregunta antes de completar.
3. **Identificadores**: conformes a las reglas de numeración de `template-system/README.md`. La comprobación es **global a toda la spec**, mirando todos los ficheros a la vez (la numeración no se reinicia por fichero).
   - **IDs malformados**: corrígelos al formato canónico (mecánico).
   - **Duplicados** (dos elementos con el mismo ID, aunque estén en ficheros distintos): pregunta si son el mismo (fusionar) o distintos (renumerar el segundo al siguiente libre).
   - **Huecos**: pregunta si son intencionados (elemento borrado, se conserva) o errata. Si existe la carpeta `design/` hermana, **MUST NOT** renumerar — los huecos se conservan y se documentan.
   - **Elementos sin ID**: asígnales el siguiente número libre de su prefijo (mecánico).
4. **Prohibiciones** (§2.3) — busca cada una y corrige (si es inequívoco) o reporta.
5. **Coherencia interna** (también **entre ficheros**):
   - Toda **referencia cruzada** se resuelve dentro de la spec: cada elemento mencionado **MUST** existir donde la guía lo define (incluidos los estados de un elemento, si los declara). En particular: cada elemento citado en escenarios, seguridad o en otro fichero tiene su fichero secundario; cada estado que use una regla está declarado donde la guía exija declararlo; cada fichero secundario está enlazado desde el índice y al revés.
   - Cada rol mencionado **MUST** coincidir con los tipos de usuario y cargos de `CLAUDE.md`. Si aparece un rol no listado, pregunta si es nuevo o errata.
   - El **alcance por centro** declarado para cada rol en Seguridad **MUST** ser coherente con las reglas de visibilidad (un rol que solo ve lo de su centro no puede aparecer accediendo a otros centros). No hay flag global de multicentro: el alcance va en la descripción de cada rol.

### 7.2 Checklist final

- [ ] ¿Están todos los apartados del índice que define `template-system/README.md`, en el mismo orden, y ninguno inventado?
- [ ] ¿Cada entrada del índice tiene su fichero secundario y al revés (sin ficheros huérfanos), según `template-system/README.md`?
- [ ] ¿Cada fichero secundario sigue la plantilla que le asigne `template-system/README.md`?
- [ ] ¿No sobrevive ningún placeholder `<…>` ni comentario `<!-- … -->` de las plantillas en ningún fichero?
- [ ] ¿Cada elemento está agrupado bajo el padre que `template-system/README.md` exige, con el anidamiento que la guía describa (sin agrupaciones que la guía no contemple)?
- [ ] ¿Cada apartado es conforme, **elemento a elemento**, a `template-system/README.md` (contenido, formato, atributos, clasificación, agrupaciones, autosuficiencia)?
- [ ] ¿Cada elemento tiene su ID conforme a la guía (prefijo, formato, ámbito), sin duplicados ni huecos no justificados, y sin renumeraciones (§2.5)?
- [ ] ¿El apartado de propiedades editables por acción (`AllowProperties`), si la plantilla lo prevé, cumple las reglas de redacción de `template-system/README.md`?
- [ ] ¿La spec está libre de las prohibiciones de §2.3?
- [ ] ¿La sección Seguridad declara **solo los roles con acceso** (sin listar "sin acceso"), cada uno con su **alcance por centro**, sin que falte ninguno que el negocio sí necesita?
- [ ] ¿No hay dependencias circulares entre sistemas/subsistemas?
- [ ] ¿Coherencia interna (§7.1.5) correcta?
- [ ] ¿No queda ninguna ambigüedad de **negocio** sin resolver? (Si la hay, vuelve a la Fase 2.)

**LIMIT**: máximo **3 iteraciones** de corrección. Si tras la 3ª siguen fallando ítems, documenta las inconsistencias residuales y avisa al usuario en el informe de la Fase 4.

---

## 8. Fase 4 — Guardar e informar

**REQUIRED**: guarda la spec directamente al terminar. **MUST NOT** mostrar el borrador completo ni pedir aprobación previa — la salida son los ficheros, que el usuario revisará y, si quiere, volverá a refinar/revisar invocando otra vez este skill.

> **REGLA OBLIGATORIA — ruta:** todos los ficheros se guardan **directamente en la carpeta de la iniciativa** (el índice y los ficheros secundarios, con los nombres que define `template-system/README.md`). En modo "nueva", crea la carpeta `.sdd/drafts/{nombre-fechado}/` calculada en la Fase 0. **No** se crean subcarpetas. **Nunca** en la raíz del proyecto ni en otra ubicación.

1. Escribe cada fichero con `Write` (sobrescribe si existía; **MUST NOT** conservar copias previas). Si en modo "refinar/review" un elemento se eliminó, borra su fichero secundario. Si la acción fue "solo review" y no hubo ningún cambio en un fichero, **MUST NOT** reescribirlo.
2. **Solo el fichero índice** lleva frontmatter; **MUST** empezar así, seguido del contenido del índice:

```
---
type: specification
---

{contenido del índice}
```

3. Los ficheros secundarios **MUST NOT** llevar frontmatter: empiezan directamente por su título, como indique `template-system/README.md`.

### 8.1 Informe de cierre

Si hubo review, incluye el resumen de cambios:

```
Especificación funcional guardada en .sdd/drafts/{carpeta-iniciativa}/
  - {índice}
  - {ficheros secundarios} (N)

Review:
  - Correcciones mecánicas aplicadas (N): <lista corta>
  - Decisiones tras preguntar al usuario (N): <lista corta>
  - Puntos del checklist aún abiertos (N): <lista corta con motivo>

Para generar el diseño (estructura de clases, vistas, acciones y tests E2E) ejecuta:
  /sdd-designer .sdd/drafts/{carpeta-iniciativa}/specification.md
```

Si fue "solo review" y la spec ya estaba conforme:

```
specification.md ya está conforme. No se ha modificado nada.
```

**MUST NOT** lanzar `/sdd-designer` tú mismo. El usuario decide cuándo ejecutarlo.

---

## 9. Quick Guidelines

- **Focus on WHAT** necesita el negocio, no en CÓMO se implementa. Regla de oro: ¿lo entendería un supervisor sin formación técnica?
- La **historia de usuario va embebida** en la spec; **no** se lee de ningún fichero externo. El skill **se puede invocar varias veces** sobre la misma spec.
- Al invocar, **MUST** preguntar el modo (nueva / refinar última / elegir otra) y, sobre una spec existente, **SIEMPRE** preguntar si **revisar** además de **mejorar** (Fase 0). Estas son preguntas de **administración del skill** (opciones cerradas): **MUST** usar `AskUserQuestion`. La prohibición de `AskUserQuestion` (§2.2) aplica **solo** a las preguntas de negocio/contenido.
- **CRITICAL — conversar mucho** (Fase 2): diálogo en lenguaje natural dentro del chat, **nunca** `AskUserQuestion` ni respuestas fijas; **LIMIT: exactamente una pregunta por mensaje** y reacciona a la respuesta antes de la siguiente; **guard**: un mensaje vacío, un [ENTER] suelto o una respuesta que no contesta NO significan "he terminado" — repregunta, no cierres ni avances de fase; sin límite de preguntas ni rondas; explica por qué preguntas y la consecuencia de cada alternativa; nunca inventes dudas de negocio ni el acceso de un rol no mencionado.
- **Revisar** (Fase 3) preserva la intención: corrige lo mecánico, pregunta lo ambiguo; **MUST NOT** reescribir por estilo ni regenerar apartados. **LIMIT**: 3 iteraciones.
- La spec es **multi-fichero**: un índice (`specification.md`, único con frontmatter) más los ficheros secundarios. **CRITICAL — toda la estructura la define `template-system/README.md`, no este skill**: número y nombres de fichero, apartados, prefijos y ámbito de los identificadores, clasificación de reglas, `AllowProperties` y el ejemplo se leen del README (y de lo que el README referencie); el skill lee **solo** el README por nombre y **MUST NOT** asumir ni dar por sabidos los demás ficheros, para servir con cualquier `--template-dir`. **MUST NOT** copiar al output la guía ni el ejemplo, ni inventar apartados.
- **IDs estables** (§2.5): nunca se renumeran; los huecos se conservan; con `design/` hermana, prohibido renumerar.
- **MUST NOT** incluir las prohibiciones de §2.3 (tipos, FQN, JPQL, XML, métodos, otra taxonomía de reglas — la conversión técnica es del diseño). Única excepción técnica admitida: `AllowProperties`, allí donde la plantilla lo prevea.
- **Generación por agente único**: el agente principal escribe la spec directamente, **sin subagentes**, y la guarda sin pedir aprobación.

---

## 10. Apéndice A — Override de rutas (versatilidad y testing)

- `--template-dir=<ruta>` — **carpeta de plantillas** alternativa a `template-system/`. **MUST** contener un `README.md` (la guía, que declara el resto del conjunto de plantillas); si falta → **ERROR** y detente (STOP condition del Outline). El skill resuelve `template-system/README.md` contra esta carpeta y descubre los demás ficheros **a través del README**. Permite reutilizar el mismo skill con otro conjunto de plantilla (otro formato o dominio) sin tocar su código.
- `--out=<ruta>` — **carpeta** de salida explícita donde se escriben el índice y los ficheros secundarios. Si se indica, se usa esa carpeta en vez de la carpeta de la iniciativa.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas se resuelven contra esta raíz.

En uso normal no se especifican: se usa la carpeta `template-system/` del skill, la carpeta de la iniciativa y `.sdd/drafts/`.
