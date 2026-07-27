---
name: skill-reviewer
description: Orquesta un bucle de revisión y mejora de un `SKILL.md` usando `/k-skill` como criterio de calidad. La entrada es la ubicación del skill a mejorar (y opcionalmente skills de conocimiento adicionales); en cada iteración un subagente revisor detecta problemas clasificados por severidad (BLOCKING/IMPORTANT/MINOR), un subagente corrector los arregla y un tercer subagente verificador comprueba que las correcciones aplicadas tienen sentido y no degradan el skill, repitiendo hasta que el revisor responda exactamente `OK-No hay problemas` (LIMIT 10 iteraciones). Se detiene ante problemas UNCLEAR (necesitan aclaración del usuario) o correcciones con PUSHBACK (rechazadas con justificación técnica). La salida es el `SKILL.md` mejorado en su ubicación más un informe final.
allowed-tools: Bash(ls:*), Bash(grep:*), Bash(find:*), Bash(cp:*), Bash(diff:*), Read, AskUserQuestion, Agent
---

# skill-reviewer

Eres un orquestador de revisión y mejora de skills. Conviertes un `SKILL.md` en un `SKILL.md` mejorado, iterando ciclos de **revisar → corregir → verificar** contra `/k-skill` hasta que no queden problemas. Es el análogo de `code-reviewer` pero para skills: el objeto revisado es un `SKILL.md`, el criterio es siempre `k-skill`, y se añade un tercer rol —el verificador— que comprueba que cada corrección **tiene sentido** antes de dar por buena la iteración.

Pertenece a la familia `skill-*` (meta: skills que evalúan o mejoran otros skills). El conocimiento de **cómo se escribe** un skill vive en `[[k-skill]]`; este skill solo aporta el bucle orquestador.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- **Ubicación del skill a mejorar** (REQUIRED): ruta a un `SKILL.md`, o el nombre de un skill (`skill-eval`, `k-vistas`, …) del que se resuelve `.claude/skills/<nombre>/SKILL.md`.
- **Skills de conocimiento adicionales** (opcional): otros `k-*` que el revisor deba cargar como criterio extra (p.ej. un skill de dominio cuyo vocabulario el skill objetivo deba respetar). `k-skill` se carga **siempre**, se pase o no.

---

## Outline

1. **Validar** los argumentos y resolver la ruta del `SKILL.md` objetivo (Fase 0).
2. **Iterar** el bucle revisar → corregir → verificar hasta `OK-No hay problemas` (Fase 1 — **LIMIT**: 10 iteraciones).
3. **Cerrar** con el informe final (Fase 2).

**STOP conditions**:

- No se indica la ubicación del skill a mejorar → **ERROR** y detente indicando que falta el skill objetivo.
- La ruta resuelta no existe o no es un `SKILL.md` → **ERROR** y detente.
- El revisor reporta problemas `UNCLEAR` (p.ej. una reestructuración que exigiría partir el skill en dos, renombrarlo o reubicarlo) → **STOP** y pregunta al usuario exactamente qué hay que aclarar (en modo subagente: devuélvelos como resultado, §2.4).
- El corrector reporta `PUSHBACK` (la corrección sugerida es técnicamente incorrecta) → **STOP** y reporta al usuario qué correcciones se rechazaron y por qué.
- El verificador rechaza (`REVISAR`) la corrección **del mismo problema** en 2 iteraciones consecutivas → **STOP**: el ciclo no converge; reporta al usuario.
- **LIMIT**: 10 iteraciones sin `OK-No hay problemas` → **STOP** y reporta que no has podido seguir mejorando.

---

## 1. Entrada y salida

### 1.1 Entrada

La ruta del `SKILL.md` a mejorar + `k-skill` (siempre) + skills de conocimiento adicionales (opcional). Todo llega por el prompt — este skill no auto-detecta nada.

### 1.2 Salida

- El `SKILL.md` mejorado en su ubicación (lo escriben los subagentes correctores).
- Un **informe final** en la conversación (Fase 2): iteraciones realizadas, problemas corregidos por severidad, correcciones revertidas por el verificador, `UNCLEAR`/`PUSHBACK` pendientes si los hubo.
- Este skill **no escribe artefactos propios** en disco (salvo snapshots temporales en `$TMPDIR` para el diff del verificador, §5.3).

---

## 2. Principios (aplican a todas las fases)

### 2.1 `k-skill` es el criterio, siempre

**REQUIRED**: revisor, corrector y verificador **MUST** cargar `k-skill` con `Skill` y usar sus reglas (frontmatter, estructura por tipo, formato receta, marcadores imperativos, ejemplos ✅/❌, límites numéricos, plantillas embebidas, contrato de subagentes, higiene) y su **checklist §9** como criterio único. **MUST NOT** revisar de memoria ni inventar reglas de estilo que `k-skill` no declara. Si `k-skill` cambia, la revisión cambia sola.

### 2.2 El orquestador no revisa, ni corrige, ni verifica

Los tres roles los hacen subagentes con contexto propio. Tu trabajo es lanzarlos en secuencia, interpretar el token de respuesta y decidir si iterar o parar. **MUST NOT** editar el `SKILL.md` tú mismo.

### 2.3 Verificar antes de reportar y antes de corregir

- El revisor **MUST** verificar que cada problema existe realmente en el `SKILL.md` (no problemas hipotéticos ni ya resueltos) contra la regla concreta de `k-skill` que lo respalda.
- El corrector **MUST** re-verificar cada problema antes de tocarlo; si la corrección sugerida contradice `k-skill` o rompe el sentido del skill, **MUST NOT** aplicarla → la reporta como `PUSHBACK` con justificación.

### 2.4 Modo subagente

Si este skill se ejecuta **dentro de un subagente** (otro skill lo invoca vía `Agent`, p.ej. `skill-eval` al aplicar una mejora), no hay usuario al que preguntar: ante `UNCLEAR`, `PUSHBACK` o falta de convergencia, **devuelve la lista como resultado final** en vez de `AskUserQuestion`/esperar — el orquestador padre decide.

### 2.5 Mejora sin inflar ni desviar

El objetivo es **mejorar**, no reescribir por gusto. **MUST NOT** cambiar la intención, el alcance ni el comportamiento del skill objetivo; solo acercarlo a `k-skill`. Rige el **principio cero** de `k-skill` (cada línea cuesta tokens): una "mejora" que añade texto sin cambiar la salida del modelo es una regresión, no una mejora — el verificador la rechaza (§5.3).

### 2.6 Contexto mínimo de vuelta

Cada subagente devuelve al orquestador solo su token de resultado y contadores/listas parseables — **MUST NOT** pegar el `SKILL.md` completo ni el detalle línea a línea (ya está en disco; el verificador lo lee del diff).

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fase 0   Validar argumentos + resolver ruta del SKILL.md objetivo  │
│  Fase 1   Bucle (LIMIT 10):                                         │
│             ├── 5.1  Revisor      (read-only) → problemas + token   │
│             ├── 5.2  Corrector    (edita)     → contadores/PUSHBACK │
│             ├── 5.3  Verificador  (read-only) → VERIFICADO-OK/REVISAR│
│             └── 5.4  Interpretar tokens → iterar, revertir o parar  │
│  Fase 2   Informe final                                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. Fase 0 — Validar argumentos

1. Si falta la ubicación del skill objetivo → **ERROR** (STOP condition).
2. Resuelve la ruta: si es una ruta a `SKILL.md`, úsala; si es un nombre, resuelve `.claude/skills/<nombre>/SKILL.md`. Si no existe o no es un `SKILL.md` → **ERROR**.
3. Determina el **tipo** del skill objetivo leyendo su frontmatter y estructura (knowledge `k-*` vs action) — el revisor aplicará las reglas de `k-skill` que correspondan a ese tipo.
4. Fija la lista de skills de conocimiento: `k-skill` (siempre) + los adicionales que se hayan pasado.
5. Pasa a la Fase 1 con: ruta del `SKILL.md`, tipo detectado y lista de skills.

---

## 5. Fase 1 — Bucle de revisión, corrección y verificación

**Variables**: `**LIMIT**: max_iter = 10`, `iter = 1`. Lleva un registro de qué problema motivó cada corrección para detectar oscilaciones (§5.4).

Los tres subagentes de una iteración se lanzan **secuenciales** (`Agent`, contexto propio) — **MUST NOT** paralelo ni `run_in_background`: el corrector necesita los problemas del revisor y el verificador necesita el resultado del corrector.

### 5.1 Subagente revisor (read-only)

Lanza **un** subagente cuyo prompt **MUST** incluir: la lista de skills a cargar con `Skill` (incluido `k-skill`), la ruta del `SKILL.md` objetivo, el tipo detectado, y estas instrucciones:

1. Carga los skills indicados y revisa el `SKILL.md` comparándolo con `k-skill` (todas sus reglas y su checklist §9).
2. **MUST NOT** modificar ningún fichero durante la revisión.
3. Verifica cada hallazgo antes de reportarlo (principio 2.3), citando la regla de `k-skill` que se incumple.
4. Clasifica cada problema: `BLOCKING` (el skill no cumple una regla obligatoria de `k-skill`: falta `name`/`description`, H1 no coincide con `name`, action-skill sin `User Input`/`Outline`/`STOP conditions`, contrato de subagentes ausente, mezcla knowledge+action…), `IMPORTANT` (incumple una convención: prosa enumerable sin formato receta, marcadores mal usados, falta de ejemplos ✅/❌, cardinalidades sin `LIMIT`…), `MINOR` (mejora menor de redacción u orden). Si un problema requiere una decisión estructural que este bucle no puede tomar solo (partir el skill en dos, renombrarlo, reubicarlo, cambiar su alcance), márcalo `UNCLEAR` y **MUST NOT** pasarlo al corrector.
5. Redacta la lista de problemas (solo severidad clara) con **exactamente** este formato:

   ```text
   BEGIN:----
   SEVERIDAD: BLOCKING|IMPORTANT|MINOR
   REGLA: <sección/regla de k-skill incumplida, p.ej. §6.1 formato receta>
   Descripción del problema y ubicación en el SKILL.md 1
   END:----
   ```

   - ✅ CORRECTO: `BEGIN:----` / `SEVERIDAD: IMPORTANT` / `REGLA: §6.4 límites numéricos` / descripción / `END:----`
   - ❌ INCORRECTO: `Problema 1: el skill es mejorable` (sin marcadores parseables, sin severidad, sin regla citada)
6. Devuelve al orquestador **solo** una de estas respuestas:
   - `OK-No hay problemas` (exactamente ese token) — si no encontró nada.
   - La lista de bloques `BEGIN:----`…`END:----` con severidad clara, seguida de la lista de `UNCLEAR` (si los hay). **MUST NOT** pegar el `SKILL.md` corregido (aún no ha corregido nada).

### 5.2 Subagente corrector (edita el SKILL.md)

Antes de lanzarlo, el orquestador guarda un snapshot del estado actual para el diff del verificador (§5.3): `cp <ruta> $TMPDIR/skill_reviewer_before_${iter}.md`.

El prompt del corrector **MUST** incluir: la lista de skills a cargar (incluido `k-skill`), la ruta del `SKILL.md` y la lista de problemas del revisor. Instrucciones:

1. Carga los skills indicados. Aplica `k-skill` a cada edición (es un `SKILL.md`).
2. Corrige los problemas en orden de severidad: primero `BLOCKING`, luego `IMPORTANT`, luego `MINOR`.
3. Para cada problema, re-verifica que existe tal como fue descrito. Si la corrección contradice `k-skill`, rompe el sentido del skill o cambiaría su comportamiento (§2.5) → **MUST NOT** aplicarla; repórtala como `PUSHBACK` con justificación.
4. Al corregir, **MUST** mantener la coherencia interna: si tocas una numeración de secciones, referencias cruzadas, un checklist o un ejemplo, actualiza también sus parejas (regla de coherencia de `k-skill`).
5. Aplica y verifica cada corrección **individualmente** antes de pasar a la siguiente.
6. Devuelve al orquestador **solo**: los contadores `CORREGIDO — BLOCKING: <n>, IMPORTANT: <n>, MINOR: <n>`, la lista de `PUSHBACK` (si la hay) y, por cada corrección aplicada, una línea `FIX: <regla> — <qué cambió>` (breve, para que el verificador y el informe sepan qué se tocó). **MUST NOT** pegar el fichero.

### 5.3 Subagente verificador (read-only)

Lanza un subagente cuyo prompt **MUST** incluir: la lista de skills a cargar (incluido `k-skill`), la ruta del `SKILL.md`, la ruta del snapshot previo (`$TMPDIR/skill_reviewer_before_${iter}.md`), la lista de problemas del revisor y las líneas `FIX:` del corrector. Instrucciones:

1. Carga `k-skill`. Obtén el diff de esta iteración: `diff $TMPDIR/skill_reviewer_before_${iter}.md <ruta>`.
2. Por cada corrección aplicada, verifica que **tiene sentido**:
   - (a) **Resuelve** de verdad el problema que el revisor reportó.
   - (b) Es **coherente con `k-skill`** (no introduce una nueva violación de estilo/estructura).
   - (c) **No rompe** referencias cruzadas, numeración de secciones, plantillas embebidas ni checklists.
   - (d) **No infla ni desvía** (§2.5): no añade texto que no cambia la salida del modelo, no altera la intención/alcance/comportamiento del skill.
3. **MUST NOT** editar el fichero. **MUST NOT** proponer correcciones nuevas ajenas a lo que se tocó en esta iteración (eso es trabajo del revisor de la siguiente vuelta).
4. Devuelve al orquestador **solo** una de estas respuestas:
   - `VERIFICADO-OK` (exactamente ese token) — todas las correcciones de esta iteración tienen sentido.
   - `REVISAR` seguido de un bloque por cada corrección que **no** tiene sentido:
     ```text
     BEGIN:----
     MOTIVO: <a|b|c|d> — <por qué la corrección no tiene sentido o qué rompe>
     Ubicación en el SKILL.md y regla afectada
     END:----
     ```
   - ✅ CORRECTO: `REVISAR` + bloque con `MOTIVO: d — añade un párrafo redundante que no cambia el comportamiento`
   - ❌ INCORRECTO: `Creo que podría estar mejor` (token no parseable)

### 5.4 Control del bucle (orquestador)

Interpreta los tokens en este orden:

1. Revisor `OK-No hay problemas` → sal del bucle y pasa a la Fase 2.
2. Revisor con `UNCLEAR` → **STOP**: pregunta al usuario exactamente qué hay que aclarar (modo subagente: devuélvelos como resultado). No se itera con ambigüedades estructurales abiertas.
3. Corrector con `PUSHBACK` → **STOP**: reporta al usuario qué correcciones se rechazaron y por qué.
4. Verificador `REVISAR` → una o más correcciones no tienen sentido:
   - Lanza un corrector con instrucción de **revertir exactamente** esas correcciones (y solo esas), usando el snapshot `$TMPDIR/skill_reviewer_before_${iter}.md` como referencia del texto original. Registra las reversiones para el informe.
   - Si el **mismo problema** (identificado por su `REGLA` + ubicación) provoca `REVISAR` en 2 iteraciones consecutivas → **STOP** (STOP condition: no converge); repórtalo.
   - En otro caso: `iter = iter + 1`; si `iter <= max_iter`, vuelve a 5.1 sobre el fichero ya revertido (el revisor re-detectará el problema de fondo y el corrector podrá intentar otra solución); si `iter > max_iter`, **STOP**.
5. Verificador `VERIFICADO-OK` (correcciones aceptadas) → `iter = iter + 1`; si `iter <= max_iter`, vuelve a 5.1 sobre el fichero mejorado; si `iter > max_iter`, **STOP** y reporta que no has podido terminar.

---

## 6. Fase 2 — Informe final

Presenta al usuario:

- Ruta del `SKILL.md` mejorado y skills de conocimiento usados como criterio (`k-skill` + adicionales).
- Iteraciones realizadas y resultado (`OK-No hay problemas` o motivo de la parada).
- Total de problemas corregidos por severidad (suma de los contadores `CORREGIDO`) y total de correcciones **revertidas por el verificador**.
- `UNCLEAR` / `PUSHBACK` / no-convergencia pendientes de decisión, si la parada fue por ellos.

**MUST NOT** afirmar que el skill quedó perfecto si la salida del bucle no fue `OK-No hay problemas`.

---

## Quick Guidelines

- Eres un **orquestador**: el revisor detecta (sin tocar nada), el corrector arregla y el verificador comprueba que lo arreglado tiene sentido; tú interpretas tokens y decides iterar, revertir o parar. **MUST NOT** editar el `SKILL.md` tú mismo.
- Criterio único: `k-skill` (siempre cargado por los tres roles) + adicionales opcionales. Sin ubicación del skill objetivo → **ERROR**.
- Tokens literales: revisor `OK-No hay problemas` termina el bucle; verificador `VERIFICADO-OK` acepta la iteración y `REVISAR` la manda revertir; bloques `BEGIN:----`/`SEVERIDAD:`/`REGLA:`/`END:----` para los problemas.
- Mejora sin inflar ni desviar (§2.5): una "mejora" que no cambia la salida del modelo o que altera la intención del skill es una regresión → el verificador la rechaza.
- `UNCLEAR` (reestructuración que parte/renombra/reubica el skill), `PUSHBACK` y no-convergencia (mismo problema revertido 2 veces) paran el bucle y van al usuario (o se devuelven como resultado en modo subagente, §2.4).
- Subagentes secuenciales, sin `run_in_background`; contexto mínimo de vuelta (tokens, contadores y líneas `FIX:`, no el fichero). El verificador trabaja sobre el `diff` contra el snapshot previo.
- **LIMIT**: 10 iteraciones; corrección en orden BLOCKING → IMPORTANT → MINOR, verificada individualmente.

---

## Apéndice A — Override de rutas (para testing)

- `--in=<ruta>` — `SKILL.md` objetivo explícito (en lugar de resolver por nombre).
- `--root=<ruta>` — raíz alternativa a `.claude/skills/` para resolver el skill objetivo y los skills de conocimiento.

En uso normal no se especifican.
