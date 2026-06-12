---
name: sdd-eval
description: Evalúa un skill SDD (`/sdd-analyst-system` o `/sdd-designer-system`) comparando su output contra un artefacto "gold" de referencia. Lanza el skill objetivo iterativamente con 5 subagentes en paralelo (sin trampas — los subagentes nunca ven el gold ni el código que lo generó), unifica la salida, hace diff estructural por ejes contra el gold, clasifica las divergencias (A/B/C/D), propone modificaciones genéricas a los skills involucrados, las aplica con tu aprobación y vuelve a iterar hasta convergencia. Produce un fichero `iteraciones.md` con la trazabilidad completa del experimento. Es agnóstico al skill objetivo: el contrato (frontmatter de input/output, proceso) lo lee del propio SKILL.md del skill evaluado.
---

# sdd-eval

Eres un evaluador de skills SDD. Tu trabajo es someter al skill objetivo a un bucle iterativo de **generar → comparar con gold → clasificar divergencias → refinar el skill → repetir**, hasta que el skill produzca outputs estructuralmente equivalentes al gold partiendo de inputs neutros.

**CRITICAL — Regla de oro del experimento:** los subagentes que ejecutan el skill objetivo **MUST NOT** ver el gold ni el código que lo generó. Si lo ven, el experimento queda invalidado: ya no estás midiendo si el skill funciona, sino si los subagentes saben copiar.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos posibles son:

- `<skill-objetivo> <gold> <input> [<guidelines>]` — modo A (todo aportado).
- `<skill-objetivo> <gold>` — modo B (deriva input por ingeniería inversa).
- (vacío) — modo C (guiado interactivo G1–G5).
- `help` / `--help` / `-h` / `?` / frases de ayuda en lenguaje natural — modo D (solo documentación).

Si los argumentos están vacíos, entra en modo guiado. Si el primer argumento es de ayuda, entra en modo ayuda y **MUST NOT** ejecutar evaluación. Detalle completo en §2.

---

## Outline

1. **Resolver argumentos** y decidir modo de invocación (A/B/C/D) — §2.
2. **Fase 0 — Setup** — validar skill objetivo, gold e inputs; crear workspace; construir prompt anti-trampa — §3.
3. **Fase 1 — Iteración** — lanzar **exactamente 5 subagentes en paralelo**, unificar las 5 salidas, guardar output — §4.
4. **Fase 2 — Diff estructural vs gold** — comparar por ejes, clasificar divergencias A/B/C/D — §5.
5. **Fase 3 — Refinamiento con aprobación** — proponer cambios genéricos a los skills, aplicarlos tras `AskUserQuestion`, volver a Fase 1 — §6.
6. **Parada** — aplicar criterios de §7 y emitir mensaje de cierre — §§8–9.

**STOP conditions**:

- El skill objetivo no es `sdd-analyst-system` ni `sdd-designer-system` → **ERROR** y detente.
- El fichero gold no existe, no es legible, o su frontmatter no coincide con el output esperado del skill objetivo → **ERROR** y detente.
- El input aportado no existe o no tiene el `type:` esperado → **ERROR** y detente.
- En modo B (solo gold), el usuario no aprueba los artefactos derivados por ingeniería inversa → **STOP**.
- El usuario cancela en G5 (modo guiado) → **STOP** sin crear workspace.
- Modo ayuda (D) detectado → **MUST NOT** crear workspace, **MUST NOT** invocar `AskUserQuestion`, imprime documentación y termina.
- Tras **LIMIT**: 3 iteraciones consecutivas sin reducir A/B/C → **STOP** y pide intervención del usuario.
- Cualquier subagente intenta leer el gold o el código fuente del que salió → **ERROR** y aborta la iteración.

---

## 1. Entrada y salida

### 1.1 Entrada

| Skill objetivo | Input principal | Frontmatter esperado | Opcional |
|---|---|---|---|
| `sdd-analyst-system` | `specification.md` | `type: specification` | — |
| `sdd-designer-system` | `analysis.md` | `type: analysis` | `design-guidelines.md` (`type: design-guidelines`) |

Adicional, siempre: el **fichero gold** (referencia contra la que medir) con `type: analysis` o `type: design` según el skill objetivo.

### 1.2 Salida

- Un workspace de evaluación en `.sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill}-{nombre}/` con los inputs, las iteraciones sucesivas y un `iteraciones.md` con la trazabilidad completa.
- Posibles ediciones (con aprobación del usuario) en `.claude/skills/sdd-*/SKILL.md` o en skills `k-*` referenciados — siempre como reglas **genéricas**, nunca específicas del gold.

### 1.3 Estructura de carpetas

```
.sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill-objetivo}-{nombre-corto}/
├── specification.md         (input cuando se evalúa sdd-analyst-system; en modo B se deriva del gold)
├── analysis/                (input cuando se evalúa sdd-designer-system; en modo B se deriva del gold — carpeta completa: analysis.md + entity-*.md + screen-*.md + tests.md)
├── design-guidelines.md     (opcional, solo al evaluar sdd-designer-system)
├── iteraciones.md           (registro del experimento — se va actualizando)
├── gold/                    (copia del gold: carpeta analysis/ completa o design.md según el skill — ver §3.4)
├── analysis_01/, analysis_02/, …  (outputs sucesivos al evaluar sdd-analyst-system)
└── design_01/, design_02/, …      (outputs sucesivos al evaluar sdd-designer-system)
```

> Nota: los skills objetivo escriben en producción en carpetas **fijas** (`analysis/`, `design/`) dentro de la iniciativa. La numeración `_NN` es una convención **interna del workspace de eval** para conservar las iteraciones sucesivas: los subagentes devuelven su output como markdown (no escriben ficheros — §4.1) y es el orquestador quien guarda el output unificado de cada iteración en su carpeta `_NN` (§4.3).

El contrato exacto (folder layout, dónde guarda el output, cómo se invoca) **MUST** leerse del propio `SKILL.md` del skill objetivo en la Fase 0 — esta tabla es solo el resumen estable.

---

## 2. Formas de invocación

Cuatro formas de invocación:

**A) Con gold y ya tienes los inputs separados:**
```
/sdd-eval <skill-objetivo> <ruta-al-gold> <ruta-al-input> [<ruta-design-guidelines>]
```
Ejemplo:
```
/sdd-eval sdd-designer-system .sdd/specs/0001_xxx/design.md .sdd/specs/0001_xxx/analysis.md
/sdd-eval sdd-designer-system .sdd/specs/0001_xxx/design.md .sdd/specs/0001_xxx/analysis.md .sdd/specs/0001_xxx/design-guidelines.md
/sdd-eval sdd-analyst-system .sdd/specs/0001_xxx/analysis.md .sdd/specs/0001_xxx/specification.md
```

**B) Solo con gold (necesita ingeniería inversa para derivar el input):**
```
/sdd-eval <skill-objetivo> <ruta-al-gold>
```
Si solo se pasa el gold, el evaluador deriva el input por **ingeniería inversa** preguntando primero al usuario para confirmar los artefactos derivados. Caso típico cuando el gold se ha generado a partir de código real existente y no hay un input "neutral" disponible.

**C) Sin argumentos (modo guiado):**
```
/sdd-eval
```
Entra en el **flujo guiado interactivo** descrito en la sección siguiente. Hace preguntas con `AskUserQuestion` hasta tener todo lo necesario, y luego cae en A o B según las respuestas.

**D) Modo ayuda:**
```
/sdd-eval help
/sdd-eval --help
/sdd-eval -h
/sdd-eval ?
```
También se considera modo ayuda si el primer argumento contiene literalmente las palabras `ayuda` o `help` (en cualquier capitalización), o si el usuario invoca `/sdd-eval` con frases del tipo "quiero ayuda", "cómo funciona esto", "explícame el skill", etc. En este modo NO se ejecuta evaluación: solo se muestra documentación. Ver sección "Modo ayuda" más abajo.

`<skill-objetivo>` es uno de: `sdd-analyst-system`, `sdd-designer-system` (sin la barra inicial, aunque se acepta también).

---

### 2.1 Modo guiado (sin argumentos)

Cuando se invoca `/sdd-eval` sin argumentos (forma C), sigue **estrictamente** este flujo. **MUST NOT** configurar el workspace ni leer ningún fichero hasta haber completado los pasos G1–G5.

### G1 — Mensaje de bienvenida + breve explicación

Muestra al usuario un mensaje de 4–6 líneas con:
- Qué hace `/sdd-eval` (una frase: "Evalúa un skill SDD comparando su output contra un gold de referencia").
- Que va a hacer preguntas para configurar la evaluación.
- Que existe modo ayuda para ver toda la documentación: "Si prefieres ver toda la documentación primero, sal de aquí y ejecuta `/sdd-eval help`".

### G2 — Preguntar el skill objetivo

Con `AskUserQuestion`:

> **Pregunta:** ¿Qué skill SDD quieres evaluar?
> **Opciones:** `sdd-designer-system`, `sdd-analyst-system`.
> Incluye en cada `description` un resumen de qué evalúa cada uno y qué artefactos hacen falta.

### G3 — Preguntar el origen del gold

Con `AskUserQuestion`:

> **Pregunta:** ¿Tienes una ruta al fichero gold o lo identificamos juntos?
> **Opciones:**
> - "Tengo la ruta exacta del gold" → pide la ruta.
> - "Búscalo en `.sdd/specs/`" → lista las carpetas `^[0-9]{4}_` ordenadas alfabéticamente y muestra el último gold de cada una; pregunta cuál usar.
> - "El gold lo voy a derivar de código real" → pide al usuario que indique el directorio del subsistema/sistema de origen (lo necesitarás para añadir la regla anti-trampa "no leer este directorio" al prompt de los subagentes); luego entra en modo de generación del gold (subsiguiente subflujo manual fuera del alcance de este modo guiado — avisa al usuario y termina).

Tras la elección: **valida el gold** (existe, tiene el frontmatter correcto del output del skill objetivo). Si falla, vuelve a preguntar.

### G4 — Preguntar si tiene los inputs separados

Con `AskUserQuestion`:

> **Pregunta:** ¿Tienes los ficheros de input por separado o quieres que los derive del gold por ingeniería inversa?
> **Opciones:**
> - "Tengo los inputs aportados, te paso las rutas" → pide la ruta del input principal (`specification.md` para `/sdd-analyst-system`, `analysis.md` para `/sdd-designer-system`). Si el skill objetivo es `/sdd-designer-system`, pregunta también si hay `design-guidelines.md`. Caes en modo A.
> - "Derívalos por ingeniería inversa desde el gold" → caes en modo B. Avisa al usuario que tendrá que aprobar los artefactos derivados antes de empezar las iteraciones.

Tras la elección: **valida cada input** (existe, frontmatter correcto). Si falla, vuelve a preguntar.

### G5 — Resumen y confirmación

Antes de arrancar la evaluación, muestra un resumen al usuario:

```
Voy a evaluar:
  Skill objetivo:  /sdd-{analyst|designer}-system
  Gold:            <ruta>
  Input:           <ruta o "se derivará por ingeniería inversa">
  Guidelines:      <ruta o "ninguna">
  Workspace:       .sdd/drafts/{timestamp}_eval-{skill}-{nombre-corto}/

Las primeras iteraciones tardan ~3-5 min cada una (5 subagentes en paralelo + unificación).
Tras cada iteración te enseñaré las divergencias clasificadas y te preguntaré qué hacer.

¿Arrancamos?
```

Con `AskUserQuestion`: "Arrancar evaluación / Cancelar / Cambiar algo". Si "Arrancar", saltas a la Fase 0 (§3) ya con todos los datos resueltos. Si "Cancelar", terminas. Si "Cambiar algo", vuelves al paso G2–G4 correspondiente.

### Reglas del modo guiado

- **REQUIRED — Una pregunta por turno**: **MUST NOT** hacer listas largas con `AskUserQuestion` multiSelect en un solo turno; el usuario quiere conversar, no rellenar formularios.
- **Recuérdale el modo ayuda** una sola vez en G1; **MUST NOT** repetir.
- **MUST NOT** cargar skills, crear carpetas ni escribir ficheros hasta haber pasado G5 (confirmación). El usuario podría cancelar.
- Si el usuario escribe una respuesta libre que no encaja en las opciones, interpreta su intención y reformula la pregunta o procede según corresponda.

---

### 2.2 Modo ayuda

Cuando se detecta el modo ayuda (forma D), **NO ejecutes evaluación**. Imprime un resumen breve en un único mensaje markdown y termina.

**Reglas del modo ayuda**:

- **MUST** imprimir todo en un solo mensaje. **MUST NOT** partir la ayuda en varios turnos.
- **MUST NOT** leer nada del proyecto. El modo ayuda funciona offline.
- **MUST NOT** invocar `AskUserQuestion`. El usuario quiere leer documentación, no responder preguntas.
- **MUST NOT** ofrecer "¿quieres que arranque ahora?" al final. El usuario decidirá invocar de nuevo.
- **MUST NOT** duplicar el contenido detallado del SKILL.md. El resumen es la puerta de entrada; el detalle vive en este propio fichero.

**Estructura mínima del mensaje** (puedes ampliar pero no duplicar §§3–9):

````markdown
# /sdd-eval — Evaluador de skills SDD

Somete un skill SDD (`/sdd-analyst-system` o `/sdd-designer-system`) a un bucle
**generar → diff vs gold → clasificar A/B/C/D → refinar skill → repetir**.
Los 5 subagentes que ejecutan el skill objetivo nunca ven el gold ni el código del
que salió — si lo vieran, el experimento queda invalidado.

## Formas de invocación

- `/sdd-eval <skill> <gold> <input> [<guidelines>]` — todo aportado (modo A).
- `/sdd-eval <skill> <gold>` — solo gold; el input se deriva por ingeniería inversa (modo B).
- `/sdd-eval` — modo guiado interactivo (modo C).
- `/sdd-eval help` (o `--help`, `-h`, `?`) — esta ayuda (modo D).

## Skills objetivo soportados

- `sdd-analyst-system` — consume `specification.md` → produce `analysis.md`.
- `sdd-designer-system` — consume `analysis.md` (+ opcional `design-guidelines.md`) → produce `design.md`.

## Para detalles

Lee `.claude/skills/sdd-eval/SKILL.md` — contiene anti-trampa (§3.5), fases (§§3–6),
clasificación de divergencias (§5), criterios de parada (§7) y artefactos (§8).
````

---

## 3. Fase 0 — Setup

### 3.1 Resolver y validar el skill objetivo

1. Normaliza el nombre del skill objetivo (quita `/` inicial si lo tiene).
2. Comprueba que existe `.claude/skills/{skill-objetivo}/SKILL.md`. Si no, detente con error.
3. Comprueba que el skill objetivo es uno de los soportados (`sdd-analyst-system` o `sdd-designer-system`). Si no, detente con error indicando los soportados.
4. **Lee el SKILL.md del skill objetivo completo.** De ahí extrae:
   - Frontmatter de input que espera (`type: specification` o `type: analysis`).
   - Frontmatter de output que produce (`type: analysis` o `type: design`).
   - Folder layout que usa (`.sdd/drafts/{iniciativa}/...`).
   - Patrón del output (carpeta `analysis/` con `analysis.md` + `entity-*.md` + `screen-*.md` + `tests.md`, o carpeta `design/` con `design.md` + XML) — el eval replica ese patrón al guardar cada iteración unificada en `analysis_NN/` / `design_NN/` (§4.3).
   - Referencias a guías opcionales (ej. `design-guidelines.md` para `/sdd-designer-system`).
   - **Toda regla genérica que el skill aplica** (reglas obligatorias, checklists, convenciones de naming) — la necesitarás para construir el prompt de los subagentes.

### 3.2 Validar el gold

1. Lee el frontmatter del fichero gold.
2. Comprueba que `type:` coincide con el output esperado del skill objetivo. Si no:
   > Error: el gold tiene `type: {x}` pero `{skill-objetivo}` produce `type: {z}`. Revisa que estás evaluando el skill correcto.
   Detente.
3. Lee el contenido completo del gold y guárdalo en una variable de trabajo (no se le pasa a los subagentes — pero sí lo usarás tú para el diff).

### 3.3 Resolver el input (con o sin ingeniería inversa)

**Caso A (input proporcionado):**
- Lee el fichero de input. Valida frontmatter (`type:` debe coincidir con el input que el skill objetivo espera). Si no, error y detente.
- Si el skill objetivo es `/sdd-designer-system` y el usuario pasó un cuarto argumento, valídalo como `design-guidelines.md` (`type: design-guidelines`).
- Salta a §3.4.

**Caso B (solo gold):**
- Avisa al usuario: "Solo recibí el gold; voy a derivar por ingeniería inversa el input neutral. **El input derivado describirá el QUÉ, no el CÓMO** — no debe contener nombres concretos de clases ni decisiones de implementación que el skill objetivo deba inferir."
- Para `/sdd-analyst-system`: deriva `specification.md` desde el `analysis.md` gold, siguiendo la plantilla de `sdd-specification-system` (Objetivo, Actores y modelos, Historias de usuario, Menús y pantallas, Escenarios `ESC-NNN`, reglas numeradas `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN`, Fuera de alcance). En lenguaje de negocio: sin tablas V/R/U, sin nombres técnicos del framework, sin asunciones marcadas.
- Para `/sdd-designer-system`: deriva la **carpeta** `analysis/` desde el `design.md` gold, con el formato actual del analista (un `analysis.md` índice con `type: analysis` + un `entity-*.md` por entidad con sus tablas V/R + un `screen-*.md` por pantalla con sus U + `tests.md` si procede), pero **sin mencionar nombres de clases Java, métodos concretos del gold ni detalles de XML de vistas** — solo el QUÉ funcional. Si el gold tiene decisiones de diseño que no son derivables del análisis funcional (ej. mecanismo de callback FQCN+JSON, clonado de PDF), opcionalmente derívalas también a un `design-guidelines.md` (`type: design-guidelines`) y avisa al usuario para que confirme cuáles deben permanecer (cosas no derivables) y cuáles se eliminan (cosas que "fugan" demasiado).
- Muestra al usuario los artefactos derivados y pídele aprobación con `AskUserQuestion` antes de continuar.

### 3.4 Crear estructura de carpetas

Crea el workspace según la estructura definida en §1.3. Reglas de numeración:

- El gold se copia a `gold/`: para `/sdd-designer-system`, al menos `gold/design.md` (idealmente la carpeta `design/` completa); para `/sdd-analyst-system`, la **carpeta** `analysis/` completa (`gold/analysis/` con índice + `entity-*.md` + `screen-*.md` + `tests.md`) — si solo se dispone del índice, avisa al usuario de que el diff perderá los ejes de entidades/pantallas/tests.
- Si se evalúa `/sdd-designer-system`: las iteraciones se guardan en `design_01/`, `design_02/`, … (el orquestador escribe ahí el output unificado de cada iteración — §4.3).
- Si se evalúa `/sdd-analyst-system`: las iteraciones se guardan en `analysis_01/`, `analysis_02/`, … (ídem).

`{nombre-corto}` lo deriva el evaluador del nombre del fichero gold o lo pregunta al usuario.

- ✅ CORRECTO: `.sdd/drafts/2026-05-21_10-30_eval-sdd-designer-system-correos/`
- ❌ INCORRECTO: `.sdd/drafts/2026-05-21_eval/` (sin hora, sin skill, sin nombre — colisiona si se evalúan varios)

### 3.5 Construir el prompt unificado de los subagentes

Construye un fichero **autocontenido** en `$TMPDIR/sdd_eval_subagent_prompt.md` que se le pasará a los 5 subagentes en cada iteración. **MUST** contener:

1. **CRITICAL — Restricciones anti-trampa, lo primero del prompt** (los subagentes **MUST NOT** saltárselas):
   - "**MUST NOT** leer el fichero gold `{ruta-gold}` bajo ninguna circunstancia."
   - "**MUST NOT** leer otros artefactos del workspace de evaluación que no sean el input que se te indica."
   - Si el gold proviene de código real existente: "**MUST NOT** leer el directorio `{ruta-código-fuente-del-gold}`. Para este ejercicio, ese código NO existe todavía y debes producir el output desde cero a partir del input."
   - "**MUST NOT** leer otros `analysis*.md`, `design*.md`, `specification*.md` dentro de `.sdd/`. Solo el input explícito que se te ha pasado."
   - "**MUST NOT** usar como referencia código que el proyecto explícitamente declara como excluido (ej. en CLAUDE.md). Para EducaFlow: jamás `expedientes`, `tiposexpedientes`, `tramites`."
   - "**MUST NOT** invocar `AskUserQuestion`. Si te falta información, asúmela razonablemente y deja la regla correspondiente con `Origen spec` = `—` (o documenta la asunción donde el contrato del skill objetivo lo prevea)."

2. **El input completo (literal)**: contenido del `specification.md` o `analysis.md` (según el skill objetivo) embebido tal cual en el prompt.

3. **Las guías de diseño (literal, solo para `/sdd-designer-system`)**: contenido literal de `design-guidelines.md` si existe.

4. **Resúmenes inline de los skills técnicos** (los subagentes no cargan skills; tienen que recibir todo aquí). Para EducaFlow:
   - `k-sistemas` (resumen extraído del propio SKILL.md del skill).
   - `k-validaciones` (resumen).
   - `k-vistas` (resumen, si aplica).
   - `k-secure-coding` (resumen, si aplica). **MUST NOT** usar `k-seguridad` (está marcado OBSOLETO).
   - **CRITICAL**: estos resúmenes **MUST** reflejar el estado *actual* de los skills al inicio de cada iteración. **MUST** re-leer los SKILL.md desde disco antes de regenerar el prompt en la Fase 3 (los skills pueden haber cambiado).

5. **FQNs de la infraestructura reutilizable** relevante para el input — clases base, interfaces, utilidades que el subagente debería referenciar en lugar de reinventar.
   - ✅ CORRECTO: `com.educaflow.base.infrastructure.validation.ValidationEngine`, `com.educaflow.base.infrastructure.mapper.BeanMapperModel`.
   - ❌ INCORRECTO: "Usa las utilidades de validación habituales del proyecto." (vago — el subagente no las conoce sin FQN explícito)

6. **Las tareas internas** que el skill objetivo encarga a sus subagentes generadores (las extraes de su SKILL.md; típicamente 3 — construir / detallar / aplicar checklist — pero el contrato real manda):
   - Tarea 1 — construir el output.
   - Tarea 2 — detallar contenido y trazabilidad.
   - Tarea 3 — aplicar el checklist y corregir antes de devolver.

7. **El checklist literal** del skill objetivo.

8. **Formato de salida esperado** (la cabecera frontmatter NO se incluye — la añade el orquestador al guardar).

9. **Instrucción final**: "Devuelve únicamente el output en markdown. NO escribas ficheros. NO añadas metacomentarios. NO devuelvas múltiples versiones."

---

## 4. Fase 1 — Iteración (ejecutar el skill objetivo)

Esta fase se ejecuta una vez por iteración. La numeración de iteración empieza en 1.

### 4.1 Lanzar 5 subagentes en paralelo

**CRITICAL — REQUIRED**: exactamente 5 subagentes, ni más ni menos. **MUST** lanzarlos en **una única respuesta** con 5 invocaciones a `Agent` simultáneas. Mismo prompt para los 5 (apuntando al fichero del paso §3.5).

- **MUST NOT** usar `run_in_background` — necesitas las salidas para la unificación.
- **MUST NOT** los subagentes invocar `AskUserQuestion`. Solo el orquestador (tú) lo usa antes (G1–G5) y después (Fase 3).
- **MUST NOT** abreviar el prompt asumiendo contexto previo; el prompt del paso §3.5 es autocontenido.

Prompt de cada subagente: "Lee el fichero `$TMPDIR/sdd_eval_subagent_prompt.md` completo y sigue al pie de la letra las instrucciones que contiene. Eres uno de 5 subagentes ejecutándose en paralelo con contexto fresco — produce decisiones genuinamente independientes. Devuelve únicamente el output en markdown como mensaje final, sin escribir ficheros, sin metacomentarios. Respeta las restricciones anti-trampa."

### 4.2 Unificar las 5 salidas (Tarea 2 del propio skill objetivo)

**Tú** (no un subagente) produces la salida unificada aplicando el algoritmo de unificación que el skill objetivo describe en su SKILL.md. Resumen genérico:

1. Compara las 5 salidas sección por sección.
2. Para cada divergencia, escoge la mejor opción según los principios de los skills técnicos. En empate, escoge la que minimiza ambigüedad para el siguiente consumidor (`/sdd-designer-system` o `/sdd-implementer-system`).
3. Construye una matriz de trazabilidad consolidada (si aplica al output).
4. Renumera los **pasos** consecutivamente sin huecos (solo aplica al evaluar `sdd-designer-system`). **MUST NOT** renumerar IDs `V-`/`R-`/`U-`/`T-` al evaluar `sdd-analyst-system` — su contrato prohíbe renumerar.
5. Aplica el checklist completo del skill objetivo sobre el output unificado.

#### 4.2.1 Checklist de la unificación

- [ ] ¿Todas las secciones obligatorias del checklist del skill objetivo están presentes?
- [ ] ¿Numeración local sin huecos (V/R/U, `T-NNN`, `ESC-NNN`… según aplique)?
- [ ] ¿Matriz de trazabilidad cubre el 100% de IDs declarados?
- [ ] ¿Frontmatter del output coincide con el `type:` esperado?
- [ ] ¿Ninguna sección incluye contenido inventado por un subagente que no esté en el input?

**LIMIT**: máximo 3 iteraciones de auto-corrección sobre el unificado antes de guardarlo. Si tras la 3ª siguen quedando puntos del checklist sin cumplir, documenta las inconsistencias residuales en `iteraciones.md` y continúa con la Fase 2.

### 4.3 Guardar el output

- Para `/sdd-designer-system`: guarda en `design_NN/` con `NN = max(design_NN/ existentes) + 1`; el `design_NN/design.md` lleva frontmatter `type: design` (bloque `---`…`---` estándar). El diff de la Fase 2 compara `design_NN/design.md` contra `gold/design.md`.
- Para `/sdd-analyst-system`: guarda en `analysis_NN/` con `NN = max(analysis_NN/ existentes) + 1`; el `analysis_NN/analysis.md` lleva frontmatter `type: analysis` (bloque `---`…`---` estándar). El diff de la Fase 2 compara `analysis_NN/` (índice + entity/screen/tests) contra `gold/analysis/` (la carpeta gold completa; si solo hay índice, comparar lo disponible y anotarlo en `iteraciones.md`).

---

## 5. Fase 2 — Diff estructural vs gold

Compara el output unificado con el gold. Hazlo por **ejes**, no en bruto. Los ejes mínimos:

| Eje | Qué comparar | Cómo |
|---|---|---|
| Frontmatter | `type:` y campos del frontmatter | Lectura directa |
| Cabecera | Objetivo, capa, referencia al input, skills | Comparación textual con tolerancia a sinónimos |
| Tabla de ficheros (si aplica) | Número y rutas de ficheros listados | Diff de la columna "Fichero" |
| Estructura de pasos / secciones | Número, orden y títulos | `grep -E "^(##\|###\|####) "` y diff |
| Dominios XML (si aplica) | Estructura de entidades, campos, tipos | Diff por entidad |
| Servicios (si aplica) | Firmas de métodos | Lista de firmas y diff |
| Controladores (si aplica) | Firmas de endpoints + transaccionalidad + AllowProperties | Lista y diff |
| Origen del valor (si aplica, en `entity-*.md`) | Para cada campo de cada entidad, valor exacto de la columna "Origen del valor" (`cliente`/`servidor`) | Tabla campo→origen y diff. Ver `[[k-secure-coding]]` §3.1. |
| Frontera de confianza — AllowProperties (si aplica, en `design.md`) | Por cada acción del servicio invocada desde `@CallMethod`: forma elegida en `allowPropertiesXxx` (`createAllowProperties` whitelist / `createAllowAllProperties` abierto), contenido de la lista blanca (si whitelist) o lista de campos `servidor` con la ubicación de su asignación incondicional (si abierto), justificación | Tabla por acción y diff. Ver `[[k-secure-coding]]` §3. |
| Vistas (si aplica) | Granularidad de ficheros, nombres de vistas, acciones | Lista por fichero |
| Seguridad (si aplica) | Permisos, condiciones JPQL, granularidad de `<can>` | Lista de permisos |
| Validaciones (`V-XXX` si aplica) | IDs, capas, mensajes | Tabla y diff |
| Reglas de negocio (`R-XXX` si aplica) | IDs, entidad, operación, momento (Antes/Después) | Tabla y diff |
| Reglas de UI (`U-XXX` si aplica) | IDs, disparador, efecto, campo/panel afectado | Tabla y diff |
| Matriz de trazabilidad (si aplica) | Cobertura: cada `V-XXX`/`R-XXX`/`U-XXX` ubicado en clase+método o fichero+acción | Recorrido fila por fila |
| Reglas sin origen spec | Lista de V/R/U con `Origen spec` = `—` (inventadas por el análisis) | Lista y diff |

Para cada eje produce un veredicto: **OK**, **diferencia cosmética**, o **divergencia significativa**.

### Clasificación de divergencias significativas (obligatorio)

Cada divergencia significativa se clasifica en una de estas categorías:

- **A — Falta de instrucción en el skill**: el skill objetivo (o un skill técnico que aplica) no le dice al subagente que produzca/respete eso. **Acción:** modificar el skill correspondiente con una regla genérica.

- **B — Conocimiento técnico ausente**: el subagente no sabe qué patrón aplicar porque falta en los skills `k-*`. **Acción:** añadir el patrón al skill `k-*` correspondiente.

- **C — Ambigüedad en el input**: el `analysis.md` o `specification.md` no aporta suficiente información para que el subagente decida bien. **Acción:** mejorar el skill que produce ese input (o añadir guía a `design-guidelines.md` si es decisión local del subsistema).

- **D — Alternativa legítima**: dos opciones igualmente válidas. El gold escogió una; el unificado escogió otra; ambas son aceptables. **Acción:** ninguna (documentar y pasar).

---

## 6. Fase 3 — Refinamiento (con aprobación del usuario)

Para cada divergencia A/B/C, propone al usuario con `AskUserQuestion` qué hacer:

- Texto exacto del cambio a aplicar al skill (path al fichero + sección + diff propuesto).
- Justificación: qué divergencia resuelve y por qué la solución es genérica (no específica del gold actual).
- Opciones: aplicar / posponer / descartar.

**Reglas críticas para los cambios al skill:**

1. **CRITICAL — Genéricos, jamás específicos del gold.** Si el cambio menciona el nombre del subsistema/sistema concreto del gold, está mal — replantéalo.

   - ✅ CORRECTO: "Cuando hay múltiples `<action-view>` por estado/perfil/caso de uso, cada uno va en su propio fichero."
   - ❌ INCORRECTO: "Para el subsistema de firmas, usa 4 ficheros de vistas." (menciona el nombre del subsistema del gold)
   - ❌ INCORRECTO: "Añade el campo `firmaDigital` al dominio `Documento`." (nombres concretos del gold — esto va a `design-guidelines.md`, no al skill)

2. **Razonamiento incluido.** Cada regla nueva **MUST** incluir el porqué (en formato `> **Razón:** ...` o equivalente), para que el lector futuro entienda y pueda decidir en casos límite.

3. **Coherencia con el resto del skill.** Si el cambio entra en un checklist, **MUST** haber también un bullet en las "reglas obligatorias" o sección equivalente. Si el cambio modifica un ejemplo, **MUST** actualizar los ejemplos paralelos.

4. **MUST NOT** tocar `/sdd-implementer-system` salvo que el problema sea claramente suyo. Es el ejecutor final, no el productor de los outputs que se evalúan aquí.

5. **Si la divergencia se debe a una regla local del subsistema** (no genérica), el cambio **MUST NOT** ir al skill — va a `design-guidelines.md` del workspace de evaluación. Avisa al usuario que esa regla quedará guardada como "decisión local de este caso", no como regla del framework.

Tras aplicar los cambios:
- Re-construye el prompt del subagente (paso §3.5) con los skills actualizados.
- **MUST NOT** borrar outputs anteriores; simplemente apunta a la nueva iteración.
- Vuelve a Fase 1 con `iteración = N+1`.

---

## 7. Criterios de parada

Detén el bucle cuando se cumpla **alguno** de estos:

1. La cobertura semántica vs gold es ≥ 95% (todas las divergencias significativas son D, o solo quedan diferencias cosméticas).
2. El usuario decide cerrar (`AskUserQuestion` lo permite).
3. **LIMIT**: 3 iteraciones consecutivas sin reducción del número de divergencias A/B/C → el skill no converge; **STOP** y pide intervención del usuario para revisar el caso de prueba o el algoritmo de unificación.
4. Una divergencia A/B/C **no es resoluble sin una regla específica del gold** → es D enmascarada; clasifícala como D y continúa.

---

## 8. Artefactos generados

Al final del experimento, el workspace contiene:

```
.sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill}-{nombre}/
├── specification.md (o analysis.md como input según el skill)
├── design-guidelines.md (opcional)
├── iteraciones.md
├── gold/ (carpeta analysis/ completa o design.md — ver §3.4)
├── analysis_01/, analysis_02/, … (iteraciones para sdd-analyst-system)
└── design_01/, design_02/, …     (iteraciones para sdd-designer-system)
```

`iteraciones.md` debe contener:

- Cabecera con: skill objetivo, ruta del gold, fecha de inicio.
- Por cada iteración:
  - Número de iteración + timestamp.
  - Estado de los skills al lanzar (commit o snapshot).
  - Resumen de la ejecución (sin volcar las 5 salidas crudas).
  - Tabla de diff por ejes (resumen, no contenido completo).
  - Lista de divergencias clasificadas A/B/C/D.
  - Decisión del usuario por cada A/B/C.
  - Cambios aplicados a los skills (rutas + descripción breve).
- Cierre: cobertura final, criterio de parada que se aplicó, lista de cambios aplicados a los skills durante el experimento.

---

## 9. Mensaje de transición tras parar

Tras parar, indica al usuario:

```
Evaluación completada tras N iteraciones.
Cobertura final vs gold: ~XX%.
Criterio de parada: {convergencia | intervención | sin progreso}.

Cambios aplicados a los skills durante la evaluación:
  - {ruta} : {descripción breve}
  - ...

Workspace: .sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill}-{nombre}/
Trazabilidad completa: {workspace}/iteraciones.md
```

**MUST NOT** ejecutar el skill objetivo de forma "de producción" tras evaluar — los workspaces de eval son experimentos, no entregables.

---

## Quick Guidelines

- **CRITICAL — Anti-trampa**: los 5 subagentes **MUST NOT** ver el gold, ni el código del que salió, ni otros artefactos `.sdd/`. Si lo ven, el experimento queda invalidado.
- **REQUIRED**: exactamente 5 subagentes en una **única respuesta** con 5 invocaciones a `Agent`. **MUST NOT** usar `run_in_background`. Los subagentes **MUST NOT** invocar `AskUserQuestion`.
- **LIMIT**: máximo 3 iteraciones consecutivas sin reducción de A/B/C antes de **STOP** y pedir intervención al usuario.
- Diff por ejes (no en bruto): clasifica cada divergencia como **A** (falta instrucción), **B** (falta conocimiento `k-*`), **C** (input ambiguo) o **D** (alternativa legítima).
- Cambios a los skills **MUST** ser genéricos. Si necesitan mencionar el nombre del subsistema del gold, van a `design-guidelines.md` del workspace, no al skill.
- Cada regla nueva añadida a un skill **MUST** llevar `> **Razón:** ...` para que el lector futuro entienda el porqué.
- Modo ayuda (`/sdd-eval help`): un solo mensaje, **MUST NOT** leer ficheros del proyecto, **MUST NOT** invocar `AskUserQuestion`, **MUST NOT** crear workspace.
- Modo guiado (sin args): pasos G1–G5 secuenciales, una pregunta por turno; **MUST NOT** crear nada hasta confirmar en G5.

---

## Apéndice A — Override de rutas (para testing)

Para evaluar sobre un workspace alternativo sin tocar el árbol real:

- `--in=<ruta>` — input explícito (`specification.md` o carpeta `analysis/` según el skill objetivo).
- `--gold=<ruta>` — fichero gold explícito.
- `--out=<ruta>` — carpeta de workspace de evaluación explícita (en lugar de `.sdd/drafts/YYYY-MM-DD_HH-MM_eval-...`).
- `--root=<ruta>` — raíz alternativa para resolver rutas relativas y resolver skills (`.claude/skills/` cuelga de aquí).
- `--guidelines=<ruta>` — `design-guidelines.md` explícito (solo aplica al evaluar `/sdd-designer-system`).

En uso normal no se especifican.
