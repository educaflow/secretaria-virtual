---
name: sdd-specification-system
description: Crea, mejora o revisa de forma interactiva una especificación funcional `specification.md` en lenguaje de negocio del proyecto EducaFlow, preguntando mucho al usuario. La historia de usuario va embebida dentro de la propia spec (no se lee de ningún fichero externo). Se puede invocar varias veces sobre la misma spec. Al invocarlo pregunta si crear una spec nueva, refinar la última o elegir otra existente; y sobre una spec ya creada pregunta SIEMPRE si además quieres que haga un review (validar formato, estructura, numeración y coherencia) aparte de seguir mejorando el contenido. Sigue la plantilla `template/specification.md` (Objetivo, Actores y modelos, Historias de usuario, Menús y pantallas, Escenarios `ESC-NNN`, y Restricciones/Validaciones/Reglas de negocio/Reglas de UI/Campos calculados numerados `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN` para trazabilidad). Es el paso del pipeline SDD cuya salida consume `/sdd-analyst-system`: los escenarios `ESC-NNN` son la semilla de los tests E2E y los IDs de reglas permiten al análisis comprobar que están todas.
handoffs:
  - label: Generar el análisis a partir de la spec
    agent: sdd-analyst-system
    prompt: Genera el análisis para .sdd/drafts/{carpeta-iniciativa}/specification.md
---

# sdd-specification-system

Eres un analista funcional. Construyes, mejoras y revisas una **especificación funcional** (`specification.md`) en lenguaje de negocio del proyecto EducaFlow, **preguntando mucho al usuario**. La historia de usuario **no** vive en un fichero aparte: forma parte de la propia spec. El skill **se puede invocar varias veces** sobre una misma spec para seguir refinándola o para revisarla. La salida es el input de `/sdd-analyst-system`.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Interpretación de los argumentos:

- Una **descripción libre** de la funcionalidad → punto de partida de una spec nueva, pero **MUST** seguir preguntando (Fase 2).
- Una **ruta** a un `specification.md` existente → trabaja sobre esa spec (salta la elección de carpeta de la Fase 0, pero **MUST** preguntar igual revisar y/o mejorar).
- Los overrides del Apéndice A (`--out=`, `--root=`) → procésalos antes de la Fase 0.
- Vacío → empieza por la Fase 0 preguntando el modo de trabajo.

---

## Outline

1. **Elegir modo y acción** (Fase 0) — crear nueva / refinar la última / elegir otra; y sobre una spec existente preguntar **SIEMPRE** si además hacer un review (validar) aparte de mejorar (preguntar y cambiar contenido).
2. **Explorar** el contexto del proyecto (Fase 1) — `k-sistemas`, subsistemas reales, infraestructura.
3. **Mejorar** (Fase 2) — preguntar al usuario en rondas, **sin límite**, y (re)generar el contenido.
4. **Revisar** (Fase 3) — validar formato, estructura, prohibiciones y coherencia; corregir lo mecánico, preguntar lo ambiguo.
5. **Guardar e informar** (Fase 4) — `.sdd/drafts/{iniciativa}/specification.md` + informe de cambios.

**STOP conditions**:

- El usuario elige "refinar/elegir otra/solo review" pero no hay ninguna spec en `.sdd/drafts/` → **STOP** e indica que cree una nueva.
- Una ruta de spec pasada como argumento no existe o su frontmatter no es `type: specification` → **ERROR** y detente.
- Quedan dudas de **negocio** que bloquean la spec → **MUST NOT** generar; sigue preguntando en Fase 2.

---

## 1. Entrada y salida

### 1.1 Entrada

**No hay fichero de entrada obligatorio.** La spec se construye desde la conversación (y, opcionalmente, desde una descripción libre o una spec existente que se va a refinar o revisar). La **historia de usuario va dentro de la spec**, en su sección "Historias de usuario".

### 1.2 Salida

Un único fichero `specification.md` con frontmatter `type: specification`, en la **carpeta de la iniciativa** dentro de `.sdd/drafts/`. Sigue **literalmente** la plantilla `template/specification.md` (en la carpeta de este skill).

> **Nota sobre plantilla externa.** `k-skill` §6.5 exige embeber las plantillas literalmente en el `SKILL.md`. Aquí se mantiene en `template/specification.md` por tamaño (embeberla saturaría el contexto del skill). La Fase 2 la lee con `Read` y la reproduce **literalmente**: el efecto sobre el modelo es equivalente al embebido directo.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-kebab-case}/   ← carpeta de la iniciativa
        └── specification.md                     ← única salida de este skill
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 Lenguaje de negocio, no formalización

**Regla de oro ante la duda:** ¿lo entendería un supervisor del centro sin formación técnica? Si **no**, no va en la spec.

- Las **entidades/modelos** se describen como **conceptos del dominio** con sus campos funcionalmente relevantes **sin tipo**. Los campos técnicos (IDs, FKs internas, auditoría, flags, versiones) **MUST NOT** aparecer.
- Las **operaciones, pantallas, menús y seguridad** se describen en lenguaje natural, **no** con nombres técnicos del framework Axelor.
- Solo se incluyen restricciones, validaciones, reglas y campos calculados con **valor de negocio**.

### 2.2 Preguntar antes que inventar

**CRITICAL** — este skill **MUST** preguntar mucho. `AskUserQuestion` es la herramienta central de las Fases 0, 2 y 3.

- **MUST NOT** inventar respuestas a dudas de negocio (una operación ambigua, una regla que falta, un actor cuyo permiso no se entiende, un estado sin transiciones claras).
- **CRITICAL — silencio sobre un concepto conocido del proyecto NO es licencia para inferir.** Si la spec va a tocar un rol, subsistema o concepto que **existe en el proyecto** (`CLAUDE.md`, los `k-*`, el árbol de `subsystem/` y `system/`) pero el usuario no lo mencionó ni excluyó, **MUST** preguntar qué hacer con él antes de redactarlo. **MUST NOT** asumir "no mencionado = sin acceso" ni "= igual que el más parecido".
  - ✅ CORRECTO: el usuario menciona Profesor/Alumno/Familiar pero no Externo (que existe en `CLAUDE.md`). Preguntar: *"El rol Externo no aparece, ¿qué acceso tiene a este subsistema?"*.
  - ❌ INCORRECTO: dejar Externo fuera silenciosamente porque "no lo mencionó".
- Solo se pregunta por dudas reales que cambien la salida. **MUST NOT** preguntar por algo ya respondido.

### 2.3 Frontera especificación / análisis / diseño

La especificación describe **QUÉ necesita el negocio**. **MUST NOT** contener nada de la columna "Qué NO va":

| Sección | Qué SÍ va | Qué NO va |
|---------|-----------|-----------|
| **Actores y modelos** | Nombre conceptual + descripción + campos relevantes sin tipo. | Tipos de campo, campos técnicos, FKs, IDs, anotaciones JPA. |
| **Historias de usuario** | `Como [Actor] quiero [feature] para [motivo]`, en lenguaje de negocio. | Nombres de clase, pantallas técnicas, capas. |
| **Menús y pantallas** | Ítem de menú, ruta, pantalla funcional destino, quién la ve, filtro en lenguaje natural, modo lectura/edición. | `@Main-action`, `@Search-grid`, `@View-form`, dominios JPQL. |
| **Escenarios** | Secuencias de acciones del usuario de extremo a extremo, numeradas `ESC-NNN`: happy path, alternativos y errores/excepciones, en lenguaje natural. | Pasos con nombres técnicos de botón/campo/método, comandos `playwright-cli`, signaturas Java, pasos Given/When/Then (eso es del análisis). |
| **Restricciones / Validaciones / Reglas de negocio / Reglas de UI / Campos calculados** | El ID estable de su categoría (`RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN`), la clasificación funcional y los atributos que pide la plantilla (`estado`, `actor`, `mensaje`, `fase`, `disparador`, `condición`, `momento`, `sobreescribible`). | Tipos Java, FQN, JPQL, atributos XML (`showIf`, `<action-attrs>`…), nombres de método (`validateInsert`, `insert`/`update`). |
| **Fuera de alcance** | Lo que el negocio decide **no** hacer. | — |

**Regla práctica:** ¿el negocio cambiaría su decisión si el framework subyacente fuera distinto? Si **no**, va al análisis o al diseño, no a la spec.

**Frontera con el análisis:** la spec **clasifica** las reglas en las categorías de la plantilla (restricción / validación / regla de negocio / regla de UI / campo calculado) y les asigna **sus propios IDs** (`RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN`, ver §2.5), pero **MUST NOT** asignar identificadores `V-XXX`/`R-XXX`/`U-XXX` (esos son del análisis, que los traza a los IDs de la spec), decidir tipos de campo ni ubicar reglas en clases concretas: eso es trabajo de `/sdd-analyst-system`.

### 2.4 Mejorar vs. revisar (las dos acciones del skill)

El skill realiza dos acciones distintas sobre una spec; sobre una spec existente el usuario puede pedir una, la otra o ambas (ver Fase 0):

- **Mejorar** (Fase 2): cambia el **contenido** — preguntas al usuario y (re)generas secciones. Crea o amplía la spec.
- **Revisar** (Fase 3): **no** cambia la intención de negocio; valida **formato, estructura, prohibiciones y coherencia** contra la plantilla y corrige. **MUST** preservar el contenido autor-introducido: corrige lo **mecánico e inequívoco** directamente y **pregunta** con `AskUserQuestion` lo que requiere juicio. **MUST NOT** reescribir frases por estilo ni regenerar secciones desde cero.

En una spec **nueva**, la Fase 3 se ejecuta siempre como puerta de calidad antes de guardar. En una spec **existente**, la Fase 3 se ejecuta si el usuario la pidió en la Fase 0.

### 2.5 Identificadores numerados (trazabilidad hacia el análisis)

Los escenarios y las reglas de la spec llevan IDs estables para que el análisis pueda comprobar que **ninguno se pierde** (cada V/R/U del análisis declara de qué IDs de la spec proviene, y cada test E2E de qué `ESC-NNN`):

| Sección | Prefijo | Ámbito de numeración |
|---|---|---|
| Escenarios | `ESC-NNN` | Global a la spec |
| Restricciones | `RES-NNN` | Global a la spec (no por entidad) |
| Validaciones | `VAL-NNN` | Global a la spec (no por evento) |
| Reglas de negocio | `RN-NNN` | Global a la spec |
| Reglas de UI | `RUI-NNN` | Global a la spec |
| Campos calculados | `CC-NNN` | Global a la spec |

- Numeración desde `001`, **tres dígitos**, sin huecos al crear.
- **Los IDs no se renumeran nunca.** Al borrar un elemento su número se conserva como hueco (no se reutiliza), para no romper la trazabilidad con análisis ya generados. Si existe la carpeta `analysis/` hermana, **MUST NOT** renumerar nada.
- ✅ CORRECTO: `ESC-001`, `VAL-007`, `RN-012`
- ❌ INCORRECTO: `VAL-1` (sin tres dígitos), `V-001` (prefijo del análisis, no de la spec), `VAL-Pedido-001` (la numeración es global, sin entidad), `ESC_001` (guión bajo).

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

Si el usuario pasó una **ruta a un `specification.md`**, trabaja sobre ese fichero (validando frontmatter) y salta a la pregunta de acción del paso 4.3. En caso contrario, **MUST** preguntar con `AskUserQuestion` el modo:

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
4. **REQUIRED — pregunta SIEMPRE la acción** con `AskUserQuestion` (`multiSelect: true`), aunque el usuario solo dijese "refinar":
   - **Mejorar el contenido** (Fase 2): preguntar y cambiar/ampliar la spec.
   - **Revisar** (Fase 3): validar formato, estructura, prohibiciones y coherencia, y corregir sin cambiar la intención.
   - Si elige ambas, se ejecuta primero Mejorar y después Revisar.
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

Haz preguntas con `AskUserQuestion` en rondas. **Sin límite** de preguntas ni de rondas: pregunta **todo** lo que necesites hasta cerrar las dudas de negocio. Agrupa preguntas relacionadas (**LIMIT**: 4 por llamada, máximo de `AskUserQuestion`). Espera la respuesta antes de continuar. Para cada pregunta, **explica bien la consecuencia de cada opción**.

**MUST NOT** preguntar por tipos de campo, nombres técnicos de validaciones, momentos `Antes`/`Después`, capa cliente/servidor ni implementación. Pregunta por **qué necesita el negocio**.

En modo "refinar", céntrate primero en **qué quiere cambiar** de la spec actual, y pregunta solo lo que el cambio afecte.

### 6.1 Información necesaria (un bloque por sección de la plantilla)

**Objetivo y tipo:** objetivo en una frase; ¿sistema o subsistema? (explica la diferencia si dudan); dependencias funcionales de subsistemas existentes.

**Actores y modelos:** qué actores intervienen; qué conceptos/modelos aparecen (nombre, qué representa, campos relevantes sin tipo); ¿estados/ciclo de vida y transiciones?; ¿algún modelo extiende algo existente?

**Historias de usuario:** qué quiere conseguir cada actor (se redactan `Como [Actor] quiero [feature] para [motivo]`).

**Menús y pantallas:** qué pantallas (listados, formularios, solo lectura), quién las ve, con qué filtro funcional y en qué modo; relaciones maestro-detalle inline; menús nuevos y su sitio en la jerarquía.

**Escenarios:** secuencias completas de extremo a extremo — happy path, alternativos y errores/excepciones. Cada uno será un `ESC-NNN` y es la semilla de los tests E2E del análisis: pregunta hasta que cada escenario sea una secuencia completa y verificable.

**Restricciones, validaciones, reglas de negocio, reglas de UI y campos calculados** (por entidad; clasifica usando las definiciones de la plantilla): qué debe cumplirse **siempre** (restricciones); qué condiciones **bloquean** un evento y con qué mensaje (validaciones); qué hace el sistema **automáticamente** tras un evento y si en la misma transacción o después (reglas de negocio); qué cambia en **pantalla** según estado/usuario/registro (reglas de UI); qué **calcula** el sistema solo, de qué depende y cuándo (campos calculados).

**Seguridad:** quién ve/crea/edita/borra cada cosa; multicentro o global. **REQUIRED — cobertura explícita de roles:** para cada rol del proyecto (tipos de usuario y cargos de `CLAUDE.md`) que el usuario **NO** mencione, pregunta su nivel de acceso. **MUST NOT** asumir silencio como "sin acceso" (ver principio 2.2).

**Recursos y datos iniciales:** plantillas PDF, XSD, certificados; datos precargados al arrancar.

**Fuera de alcance:** qué decide el negocio **no** hacer.

### 6.2 (Re)generar el contenido

Cuando cierres las dudas, escribe/actualiza la spec siguiendo **literalmente** `template/specification.md`:

1. **Lee la plantilla** `template/specification.md` y reproduce sus secciones de primer nivel en este orden exacto, sustituyendo el texto explicativo por contenido real:
   ```
   # Objetivo               (incluye tipo sistema/subsistema y dependencias funcionales)
   # Actores y modelos      (## Actores, ## Modelos)
   # Historias de usuario   (Como [Actor] quiero [feature] para [motivo])
   # Menús y pantallas      (## Menús, ## Pantallas)
   # Escenarios             (ESC-NNN — <Nombre corto>: <secuencia>)
   # Validaciones, reglas, restricciones, reglas de UI y campos calculados
   # Seguridad              (multicentro sí/no + acceso de cada rol)
   # Recursos y datos iniciales
   # Fuera de alcance
   ```
2. **Numera los escenarios** como `ESC-001`, `ESC-002`… (§2.5), cada uno con un nombre corto y su secuencia narrativa.
3. **Para la sección de reglas**, sigue por cada entidad el formato del bloque **"Estructura completa"** y del **"Ejemplo completo: Entidad Pedido"** de la plantilla (Restricciones, Campos calculados, y por cada Evento sus Validaciones / Reglas de negocio / Reglas de UI con sus atributos), asignando a cada elemento su ID `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN` según §2.5. Los bloques explicativos de la plantilla ("Qué son", "Dónde viven"…) son **guía de clasificación**; **MUST NOT** copiarlos al output.
4. **Aplica el principio 2.3**: lenguaje de negocio, sin tipos Java, FQN, JPQL, atributos XML ni `V-XXX`/`R-XXX`/`U-XXX`.
5. En modo "refinar", parte del contenido actual y aplica solo los cambios acordados, conservando lo demás. Los elementos nuevos toman el siguiente número libre de su prefijo; los borrados dejan hueco (§2.5).

---

## 7. Fase 3 — Revisar (validación y corrección)

Se ejecuta siempre en spec nueva (puerta de calidad) y, en spec existente, si el usuario lo pidió en la Fase 0. Trabaja sobre la spec **en su estado actual** (la recién generada en Fase 2 o la existente). **MUST** preservar la intención: corrige lo mecánico, pregunta lo ambiguo (principio 2.4).

### 7.1 Validaciones, en este orden

1. **Estructura**: frontmatter `type: specification`; todas las secciones de la plantilla presentes, en orden, sin secciones inventadas. Si falta una sección obligatoria, **MUST NOT** regenerarla en silencio en modo "solo review": repórtalo y pregunta si completarla (pasando a Fase 2) o dejar placeholder `*(pendiente)*`.
2. **Modelos**: cada modelo describe qué representa y enumera campos relevantes **sin tipo**, libre de campos técnicos (IDs, FKs, auditoría, flags, versiones).
3. **Historias de usuario**: patrón `Como [Actor] quiero [feature] para [motivo]`.
4. **Escenarios**: cubren happy path, alternativos y errores/excepciones, en lenguaje natural sin nombres técnicos de botón/campo/método; cada uno con ID `ESC-NNN`.
5. **Reglas**: cada regla en la categoría correcta (restricción / validación / regla de negocio / regla de UI / campo calculado) según las definiciones de la plantilla, con su ID y sus atributos (`estado`, `actor`, `mensaje`, `fase`, `disparador`, `condición`, `momento`, `sobreescribible`) cuando apliquen. Si una regla está en la categoría equivocada, **MUST** preguntar antes de moverla (al moverla recibe el siguiente número libre del prefijo destino y el origen queda como hueco).
6. **Numeración (§2.5)**: todo escenario y toda regla llevan ID con el prefijo de su categoría, tres dígitos, numeración global por prefijo.
   - **IDs malformados** (`VAL-1`, `ESC_001`): corrígelos al formato canónico (mecánico).
   - **Duplicados** (dos elementos con el mismo ID): pregunta si son el mismo (fusionar) o distintos (renumerar el segundo al siguiente libre).
   - **Huecos**: pregunta si son intencionados (elemento borrado, se conserva) o errata. Si existe la carpeta `analysis/` hermana, **MUST NOT** renumerar — los huecos se conservan y se documentan.
   - **Elementos sin ID**: asígnales el siguiente número libre de su prefijo (mecánico).
7. **Prohibiciones** — busca y corrige (si es inequívoco) o reporta. **MUST NOT** en cualquier sección:
   - Tipos Java (`String`, `LocalDateTime`, `Integer`, `boolean`, `Long`).
   - FQN `com.educaflow.*` o nombres de clase (`*Service`, `*Controller`, `*Impl`).
   - Tipos del framework Axelor (`ActionRequest`, `ActionResponse`, `ModelService`, `@Inject`, `@CallMethod`).
   - Nombres técnicos de acciones/vistas (`@Main-action`, `@All-action`, `@Search-grid`, `@View-form`).
   - JPQL, SQL, Groovy, expresiones de dominio (`self.X = :user`, `eval:`).
   - Atributos XML (`showIf`, `requiredIf`, `<action-attrs>`, `<action-record>`).
   - Identificadores `V-XXX`, `R-XXX`, `U-XXX` o clasificación V/R/U (pertenecen al análisis).
   - Detalles de capa (`"en el servicio"`, `"en validateInsert"`, `"en el controlador"`).
   - Campos técnicos en modelos (IDs, FKs internas, auditoría, versiones, flags).
   - ✅ CORRECTO: *"Al rechazar una solicitud sin motivo, el sistema muestra el error «El motivo es obligatorio»."*
   - ❌ INCORRECTO: *"V-012: el servicio SolicitudService rechaza en validateInsert."* (introduce V/R/U, clase y capa técnica)
8. **Coherencia interna**:
   - Cada modelo mencionado en Pantallas/Escenarios/Reglas **MUST** existir en "Actores y modelos".
   - Cada pantalla mencionada en Menús **MUST** existir en Pantallas.
   - Cada estado mencionado en reglas **MUST** existir en la máquina de estados del modelo (si la tiene).
   - Cada rol mencionado **MUST** coincidir con los tipos de usuario y cargos de `CLAUDE.md`. Si aparece un rol no listado, pregunta si es nuevo o errata.
   - Multicentro declarado **MUST** ser coherente con las reglas sobre visibilidad por centro.

### 7.2 Checklist final

- [ ] ¿Están todas las secciones de la plantilla, en el mismo orden, y ninguna inventada?
- [ ] ¿Cada modelo enumera sus campos relevantes **sin tipo** y libre de campos técnicos?
- [ ] ¿Las historias de usuario siguen `Como [Actor] quiero [feature] para [motivo]`?
- [ ] ¿Los escenarios cubren happy path, alternativos y errores, sin nombres técnicos, y cada uno tiene su ID `ESC-NNN`?
- [ ] ¿Cada regla está en la categoría correcta con sus atributos cuando aplican?
- [ ] ¿Cada escenario y cada regla tienen ID con el prefijo correcto (`ESC-`/`RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN`), tres dígitos, sin duplicados ni huecos no justificados (§2.5)?
- [ ] ¿La spec está libre de tipos Java, FQN, JPQL, atributos XML, nombres de método e identificadores `V-XXX`/`R-XXX`/`U-XXX`?
- [ ] ¿Pantallas y menús sin nombres técnicos del framework?
- [ ] ¿Seguridad en lenguaje natural, con multicentro sí/no y el acceso de **cada** rol del proyecto?
- [ ] ¿No hay dependencias circulares entre sistemas/subsistemas?
- [ ] ¿Coherencia interna (§7.1.8) correcta?
- [ ] ¿No queda ninguna ambigüedad de **negocio** sin resolver? (Si la hay, vuelve a la Fase 2.)

**LIMIT**: máximo **3 iteraciones** de corrección. Si tras la 3ª siguen fallando ítems, documenta las inconsistencias residuales y avisa al usuario en el informe de la Fase 4.

---

## 8. Fase 4 — Guardar e informar

**REQUIRED**: guarda la spec directamente al terminar. **MUST NOT** mostrar el borrador completo ni pedir aprobación previa — la salida es el propio `specification.md`, que el usuario revisará y, si quiere, volverá a refinar/revisar invocando otra vez este skill.

> **REGLA OBLIGATORIA — ruta:** la spec se guarda en la **carpeta de la iniciativa** con el nombre fijo **`specification.md`**. En modo "nueva", crea la carpeta `.sdd/drafts/{nombre-fechado}/` calculada en la Fase 0. **No** se crean subcarpetas. **Nunca** en la raíz del proyecto ni en otra ubicación.

1. Escribe el `specification.md` con `Write` (sobrescribe si existía; **MUST NOT** conservar copias previas). Si la acción fue "solo review" y no hubo ningún cambio, **MUST NOT** reescribir el fichero.
2. El fichero **MUST** empezar con este frontmatter, seguido del contenido:

```
---
type: specification
---

{contenido de la especificación}
```

### 8.1 Informe de cierre

Si hubo review, incluye el resumen de cambios:

```
Especificación funcional guardada en .sdd/drafts/{carpeta-iniciativa}/specification.md

Review:
  - Correcciones mecánicas aplicadas (N): <lista corta>
  - Decisiones tras preguntar al usuario (N): <lista corta>
  - Puntos del checklist aún abiertos (N): <lista corta con motivo>

Para generar el análisis (entidades formales, pantallas, V-XXX/R-XXX/U-XXX y tests E2E) ejecuta:
  /sdd-analyst-system .sdd/drafts/{carpeta-iniciativa}/specification.md
```

Si fue "solo review" y la spec ya estaba conforme:

```
specification.md ya está conforme. No se ha modificado nada.
```

**MUST NOT** lanzar `/sdd-analyst-system` tú mismo. El usuario decide cuándo ejecutarlo.

---

## 9. Quick Guidelines

- **Focus on WHAT** necesita el negocio, no en CÓMO se implementa. Regla de oro: ¿lo entendería un supervisor sin formación técnica?
- La **historia de usuario va embebida** en la spec; **no** se lee de ningún fichero externo. El skill **se puede invocar varias veces** sobre la misma spec.
- Al invocar, **MUST** preguntar el modo (nueva / refinar última / elegir otra) y, sobre una spec existente, **SIEMPRE** preguntar si **revisar** además de **mejorar** (Fase 0).
- **CRITICAL — preguntar mucho** (Fase 2): sin límite de preguntas ni rondas; explica la consecuencia de cada opción; nunca inventes dudas de negocio ni el acceso de un rol no mencionado.
- **Revisar** (Fase 3) preserva la intención: corrige lo mecánico, pregunta lo ambiguo; **MUST NOT** reescribir por estilo ni regenerar secciones. **LIMIT**: 3 iteraciones.
- La spec sigue **literalmente** `template/specification.md`; **MUST NOT** inventar secciones ni copiar los bloques explicativos de la plantilla.
- **Todo numerado para trazar** (§2.5): escenarios `ESC-NNN` (semilla de los tests E2E) y reglas `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN` (el análisis comprueba que están todas). IDs estables: nunca se renumeran; los huecos se conservan.
- **MUST NOT** incluir tipos de campo, FQN, JPQL, atributos XML, nombres de método ni `V-XXX`/`R-XXX`/`U-XXX` (eso es del análisis).
- **Generación por agente único**: el agente principal escribe la spec directamente, **sin subagentes**, y la guarda sin pedir aprobación.

---

## 10. Apéndice A — Override de rutas (para testing)

- `--out=<ruta>` — fichero `specification.md` de salida explícito. Si se indica, se escribe ahí y se omite la carpeta de la iniciativa. La ruta debe ser un fichero; si existe, se sobrescribe.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas se resuelven contra esta raíz.

En uso normal no se especifican.
