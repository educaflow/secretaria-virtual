---
name: sdd-eval
description: Evalúa un skill SDD (`/sdd-analyst-system` o `/sdd-designer-system`) comparando su output contra un artefacto "gold" de referencia. Lanza el skill objetivo iterativamente con 5 subagentes en paralelo (sin trampas — los subagentes nunca ven el gold ni el código que lo generó), unifica la salida, hace diff estructural por ejes contra el gold, clasifica las divergencias (A/B/C/D), propone modificaciones genéricas a los skills involucrados, las aplica con tu aprobación y vuelve a iterar hasta convergencia. Produce un fichero `iteraciones.md` con la trazabilidad completa del experimento. Es agnóstico al skill objetivo: el contrato (frontmatter de input/output, proceso) lo lee del propio SKILL.md del skill evaluado.
---

# sdd-eval

Eres un evaluador de skills SDD. Tu trabajo es someter al skill objetivo a un bucle iterativo de **generar → comparar con gold → clasificar divergencias → refinar el skill → repetir**, hasta que el skill produzca outputs estructuralmente equivalentes al gold partiendo de inputs neutros.

**Regla de oro del experimento:** los subagentes que ejecutan el skill objetivo **nunca** deben ver el gold ni el código que lo generó. Si ven el gold, el experimento queda invalidado: ya no estás midiendo si el skill funciona, sino si los subagentes saben copiar.

---

## Skills objetivo soportados

| Skill objetivo | Input que consume | Output que produce | Ficheros opcionales |
|---|---|---|---|
| `/sdd-analyst-system` | `user-story.md` (`type: user-story`) | `analysis.md` (`type: analysis`) | — |
| `/sdd-designer-system` | `analysis.md` (`type: analysis`) | `design.md` (`type: design`) | `design-guidelines.md` (`type: design-guidelines`) |

El contrato exacto (folder layout, dónde guarda el output, cómo se invoca) se lee del propio `SKILL.md` del skill objetivo en la Fase 0.

---

## Argumentos

Cuatro formas de invocación:

**A) Con gold y ya tienes los inputs separados:**
```
/sdd-eval <skill-objetivo> <ruta-al-gold> <ruta-al-input> [<ruta-design-guidelines>]
```
Ejemplo:
```
/sdd-eval sdd-designer-system .sdd/specs/0001_xxx/design.md .sdd/specs/0001_xxx/analysis.md
/sdd-eval sdd-designer-system .sdd/specs/0001_xxx/design.md .sdd/specs/0001_xxx/analysis.md .sdd/specs/0001_xxx/design-guidelines.md
/sdd-eval sdd-analyst-system .sdd/specs/0001_xxx/analysis.md .sdd/specs/0001_xxx/user-story.md
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

## Modo guiado (sin argumentos)

Cuando se invoca `/sdd-eval` sin argumentos (forma C), sigue **estrictamente** este flujo. No empieces a configurar el workspace ni a leer ningún fichero hasta haber completado los pasos G1–G5.

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
> - "Tengo los inputs aportados, te paso las rutas" → pide la ruta del input principal (`user-story.md` para `/sdd-analyst-system`, `analysis.md` para `/sdd-designer-system`). Si el skill objetivo es `/sdd-designer-system`, pregunta también si hay `design-guidelines.md`. Caes en modo A.
> - "Derívalos por ingeniería inversa desde el gold" → caes en modo B. Avisa al usuario que tendrá que aprobar los artefactos derivados antes de empezar las iteraciones.

Tras la elección: **valida cada input** (existe, frontmatter correcto). Si falla, vuelve a preguntar.

### G5 — Resumen y confirmación

Antes de arrancar la evaluación, muestra un resumen al usuario:

```
Voy a evaluar:
  Skill objetivo:  /system-{analyst|designer}
  Gold:            <ruta>
  Input:           <ruta o "se derivará por ingeniería inversa">
  Guidelines:      <ruta o "ninguna">
  Workspace:       .sdd/drafts/{timestamp}_eval-{skill}-{nombre-corto}/

Las primeras iteraciones tardan ~3-5 min cada una (5 subagentes en paralelo + unificación).
Tras cada iteración te enseñaré las divergencias clasificadas y te preguntaré qué hacer.

¿Arrancamos?
```

Con `AskUserQuestion`: "Arrancar evaluación / Cancelar / Cambiar algo". Si "Arrancar", saltas a Fase 0.4 (la estructura ya está clara). Si "Cancelar", terminas. Si "Cambiar algo", vuelves al paso correspondiente.

### Reglas del modo guiado

- **Una pregunta por turno** (no hagas listas largas con `AskUserQuestion` multiSelect en un solo turno; el usuario quiere conversar, no rellenar formularios).
- **Recuérdale el modo ayuda** una sola vez en G1; no repitas.
- **No cargues skills, no crees carpetas, no escribas ficheros** hasta haber pasado G5 (confirmación). El usuario podría cancelar.
- **Si el usuario escribe una respuesta libre que no encaja en las opciones** (con `AskUserQuestion` siempre puede hacerlo), interpreta su intención y reformula la pregunta o procede según corresponda.

---

## Modo ayuda

Cuando se detecta el modo ayuda (forma D), **NO ejecutes evaluación**. Imprime la documentación completa estructurada como sigue, en un único mensaje (markdown). No uses `AskUserQuestion`. No leas ficheros del proyecto. No crees workspace.

### Estructura del mensaje de ayuda

````markdown
# /sdd-eval — Evaluador de skills SDD

## Qué hace

Somete un skill SDD (`/sdd-analyst-system` o `/sdd-designer-system`) a un bucle iterativo:

```
gold (referencia) ─┐
                   │
 input neutral ────┼──► /skill-objetivo (5 subagentes paralelos + unificación)
                   │              │
                   │              ▼
                   │         output generado
                   │              │
                   └──► diff por ejes ─► clasificar A/B/C/D
                                              │
                          ┌───────────────────┘
                          ▼
                  proponer cambios al skill (con tu aprobación)
                          │
                          ▼
                       repetir
```

Tras N iteraciones, el output generado debería converger al gold. Cada divergencia que no converge revela una regla que falta en el skill, una ambigüedad en el input, o una decisión legítima del subsistema (que entonces va a `design-guidelines.md`, no al skill).

## Cuándo usarlo

- Acabas de modificar un skill `/system-*` y quieres validar que sigue produciendo outputs equivalentes a casos previos.
- Tienes un caso real (subsistema implementado) y quieres comprobar si el skill SDD sería capaz de generar el mismo diseño desde cero.
- Quieres encontrar reglas que faltan en los skills `k-*` analizando huecos sistemáticos en lo que producen los subagentes.

NO uses `/sdd-eval` para:
- Generar diseños o análisis "de producción". Para eso usa los skills directamente. `/sdd-eval` crea workspaces de evaluación, no entregables.
- Comparar dos diseños que no estén producidos por el mismo skill — el experimento no tiene sentido.

Skills objetivo soportados: `/sdd-analyst-system` (consume `user-story.md`, produce `analysis.md`) y `/sdd-designer-system` (consume `analysis.md`, produce `design.md`; admite `design-guidelines.md` opcional).

## Formas de invocación

**A — Todo aportado:**
`/sdd-eval <skill> <gold> <input> [<guidelines>]`

**B — Solo gold (deriva input por ingeniería inversa):**
`/sdd-eval <skill> <gold>`

**C — Sin argumentos (modo guiado):**
`/sdd-eval` — hace preguntas hasta tener todo.

**D — Esta ayuda:**
`/sdd-eval help` (también `--help`, `-h`, `?`, o frases en lenguaje natural pidiendo ayuda).

## Ejemplos

```bash
# Evaluar /sdd-designer-system con un gold y su análisis
/sdd-eval sdd-designer-system .sdd/specs/0001_correos/design.md .sdd/specs/0001_correos/analysis.md

# Evaluar /sdd-designer-system derivando el análisis del gold
/sdd-eval sdd-designer-system .sdd/drafts/2026-05-10_17-00_firmas/analysis_01/design_01.md

# Evaluar /sdd-analyst-system con todo aportado
/sdd-eval sdd-analyst-system .sdd/specs/0001_correos/analysis.md .sdd/specs/0001_correos/user-story.md

# Modo guiado interactivo
/sdd-eval

# Esta ayuda
/sdd-eval help
```

## Regla de oro: anti-trampa

Los 5 subagentes que ejecutan el skill objetivo **nunca ven el gold ni el código del que salió**. Si lo ven, el experimento queda invalidado.

El gold lo usa solo el orquestador (yo) para hacer el diff de la Fase 2.

Concretamente, el prompt que recibe cada subagente prohíbe explícitamente:
- Leer la ruta del gold.
- Leer el directorio del código fuente del que se generó el gold (si aplica).
- Leer otros artefactos del workspace de evaluación.
- Leer otros `analysis*.md`, `design*.md`, `user-story*.md` en `.sdd/`.
- Leer código que el proyecto excluye explícitamente (en EducaFlow: `expedientes`, `tiposexpedientes`, `tramites`).

## Qué pasa en cada iteración

1. **Lanzamiento (Fase 1):** 5 invocaciones a `Agent` en paralelo, mismo prompt autocontenido para los 5. Cada subagente devuelve un output completo.
2. **Unificación (Fase 1, parte 2):** el orquestador combina las 5 salidas usando el algoritmo de unificación que el propio skill objetivo define en su SKILL.md (Tarea 2). Aplica el checklist completo del skill objetivo sobre el resultado.
3. **Diff vs gold (Fase 2):** comparación por 12 ejes (frontmatter, cabecera, tabla de ficheros, dominios, servicios, controladores, vistas, seguridad, reglas `V-XXX`/`R-XXX`/`U-XXX`, matriz de trazabilidad, asunciones, notas). Cada divergencia significativa se clasifica:
   - **A** — falta de instrucción en el skill.
   - **B** — conocimiento técnico ausente en `k-*`.
   - **C** — ambigüedad en el input.
   - **D** — alternativa legítima (no requiere acción).
4. **Refinamiento (Fase 3):** para cada A/B/C te pregunto con `AskUserQuestion` qué hacer (aplicar / posponer / descartar). Si apruebas, modifico el skill correspondiente con una regla **genérica** (nunca específica del gold). Cualquier divergencia que solo se resolvería con una regla específica del subsistema va a `design-guidelines.md` del workspace, no al skill.

## Criterios de parada

- Cobertura ≥ 95% (solo quedan D y diferencias cosméticas).
- Tú decides parar.
- 3 iteraciones consecutivas sin reducir el número de A/B/C.
- Una A/B/C resulta ser D enmascarada (no se puede resolver sin una regla específica del gold).

## Artefactos generados

Workspace en `.sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill}-{nombre}/` con los inputs, los outputs de cada iteración y un `iteraciones.md` que registra la trazabilidad completa del experimento. Estructura detallada en la sección "Artefactos generados" al final de este SKILL.md.

## Coste y duración estimados

- Cada iteración: ~3–5 minutos (5 subagentes en paralelo + unificación + diff + decisiones del usuario).
- Workspace típico: 3–5 iteraciones para converger en un caso "sano"; más si se descubren huecos importantes en los skills.
- Cada subagente consume ~50–60 K tokens. Total por iteración: ~300 K tokens.

## Lo que el skill NO hace (lista explícita)

- No lee el gold para construir el prompt de los subagentes.
- No modifica el gold.
- No se salta la unificación (1 sola salida no es representativa).
- No aplica cambios a los skills sin aprobación del usuario.
- No añade reglas específicas del gold a los skills (eso va a `design-guidelines.md`).
- No usa `run_in_background` para los 5 subagentes (necesita las salidas para unificar).
- No abrevia el prompt del subagente asumiendo contexto previo.
- No ejecuta los skills "de producción" tras evaluar.

---

Para empezar, lanza:
- `/sdd-eval` (modo guiado), o
- `/sdd-eval <skill> <gold>` (deriva input por ingeniería inversa), o
- `/sdd-eval <skill> <gold> <input>` (todo aportado).
````

### Reglas del modo ayuda

- **Imprimir todo en un solo mensaje.** No partas la ayuda en varios turnos.
- **No leer nada del proyecto.** El modo ayuda funciona offline; no necesitas inspeccionar el workspace ni los skills.
- **No invocar `AskUserQuestion`.** El usuario quiere leer la documentación, no responder preguntas.
- **No ofrecer "¿quieres que arranque ahora?"** al final. El usuario decidirá invocar de nuevo.

---

## Fase 0 — Setup

### 0.1 Resolver y validar el skill objetivo

1. Normaliza el nombre del skill objetivo (quita `/` inicial si lo tiene).
2. Comprueba que existe `/.claude/skills/{skill-objetivo}/SKILL.md`. Si no, detente con error.
3. Comprueba que el skill objetivo es uno de los soportados (`sdd-analyst-system` o `sdd-designer-system`). Si no, detente con error indicando los soportados.
4. **Lee el SKILL.md del skill objetivo completo.** De ahí extrae:
   - Frontmatter de input que espera (`type: user-story` o `type: analysis`).
   - Frontmatter de output que produce (`type: analysis` o `type: design`).
   - Folder layout que usa (`.sdd/drafts/{iniciativa}/...`).
   - Patrón de nombre del output (ej. `analysis.md` único en `analysis_NN/`, o `design_NN.md` numerado dentro de `analysis_NN/`).
   - Referencias a guías opcionales (ej. `design-guidelines.md` para `/sdd-designer-system`).
   - **Toda regla genérica que el skill aplica** (reglas obligatorias, checklists, convenciones de naming) — la necesitarás para construir el prompt de los subagentes.

### 0.2 Validar el gold

1. Lee el frontmatter del fichero gold.
2. Comprueba que `type:` coincide con el output esperado del skill objetivo. Si no:
   > Error: el gold tiene `type: {x}` pero `/system-{y}` produce `type: {z}`. Revisa que estás evaluando el skill correcto.
   Detente.
3. Lee el contenido completo del gold y guárdalo en una variable de trabajo (no se le pasa a los subagentes — pero sí lo usarás tú para el diff).

### 0.3 Resolver el input (con o sin ingeniería inversa)

**Caso A (input proporcionado):**
- Lee el fichero de input. Valida frontmatter (`type:` debe coincidir con el input que el skill objetivo espera). Si no, error y detente.
- Si el skill objetivo es `/sdd-designer-system` y el usuario pasó un cuarto argumento, valídalo como `design-guidelines.md` (`type: design-guidelines`).
- Salta a 0.4.

**Caso B (solo gold):**
- Avisa al usuario: "Solo recibí el gold; voy a derivar por ingeniería inversa el input neutral. **El input derivado describirá el QUÉ, no el CÓMO** — no debe contener nombres concretos de clases ni decisiones de implementación que el skill objetivo deba inferir."
- Para `/sdd-analyst-system`: deriva `user-story.md` desde el `analysis.md` gold. La user-story debe ser un relato de usuario realista, sin tablas de reglas (V/R/U), sin nombres de entidades en formato técnico, sin asunciones marcadas. Solo el problema funcional desde el punto de vista del usuario y las restricciones que no pueden romperse.
- Para `/sdd-designer-system`: deriva `analysis.md` desde el `design.md` gold. El análisis debe usar el formato `type: analysis` (entidades, operaciones, vistas, seguridad, tablas `V-XXX`/`R-XXX`/`U-XXX`, asunciones), pero **no debe mencionar nombres de clases Java, métodos concretos del gold ni detalles de XML de vistas** — solo el QUÉ funcional. Si el gold tiene decisiones de diseño que no son derivables del análisis funcional (ej. mecanismo de callback FQCN+JSON, clonado de PDF), opcionalmente derívalas también a un `design-guidelines.md` (`type: design-guidelines`) y avisa al usuario para que confirme cuáles deben permanecer (cosas no derivables) y cuáles se eliminan (cosas que "fugan" demasiado).
- Muestra al usuario los artefactos derivados y pídele aprobación con `AskUserQuestion` antes de continuar.

### 0.4 Crear estructura de carpetas

Convención del workspace de evaluación (sigue el folder layout del skill objetivo):

```
.sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill-objetivo}-{nombre-corto}/
├── user-story.md            (siempre, derivado o aportado)
├── design-guidelines.md     (solo para evaluar /sdd-designer-system si existe/se deriva)
├── iteraciones.md           (registro del experimento — se va actualizando)
└── analysis_01/
    ├── analysis.md          (siempre — input para /sdd-designer-system o output gold para /sdd-analyst-system)
    ├── design_01.md         (gold, solo cuando se evalúa /sdd-designer-system)
    ├── design_02.md, ...    (outputs sucesivos cuando se evalúa /sdd-designer-system)
    └── ...
```

- Si se evalúa `/sdd-designer-system`: el gold es `analysis_01/design_01.md`; las iteraciones se guardan como `analysis_01/design_02.md`, `design_03.md`, etc.
- Si se evalúa `/sdd-analyst-system`: el gold es `analysis_01/analysis.md`; las iteraciones se guardan como `analysis_02/analysis.md`, `analysis_03/analysis.md`, etc. (cada ejecución de `/sdd-analyst-system` crea su propia subcarpeta `analysis_NN`).

`{nombre-corto}` lo deriva el evaluador del nombre del fichero gold o lo pregunta al usuario.

### 0.5 Construir el prompt unificado de los subagentes

Construye un fichero **autocontenido** en `$TMPDIR/sdd_eval_subagent_prompt.md` que se le pasará a los 5 subagentes en cada iteración. Contenido obligatorio del prompt:

1. **Restricciones específicas del experimento (anti-trampa) — lo primero del prompt**:
   - "NO leas el fichero gold `{ruta-gold}` bajo ninguna circunstancia."
   - "NO leas otros artefactos del workspace de evaluación que no sean el input que se te indica."
   - Si el gold proviene de código real existente: "NO leas el directorio `{ruta-código-fuente-del-gold}`. Para este ejercicio, ese código NO existe todavía y debes producir el output desde cero a partir del input."
   - "NO leas otros `analysis*.md`, `design*.md`, `user-story*.md` dentro de `.sdd/`. Solo el input explícito que se te ha pasado."
   - "NO uses como referencia código que el proyecto explícitamente declara como excluido (ej. en CLAUDE.md). Para EducaFlow: jamás `expedientes`, `tiposexpedientes`, `tramites`."

2. **El input completo (literal)**: contenido del `user-story.md` o `analysis.md` (según el skill objetivo) embebido tal cual en el prompt.

3. **Las guías de diseño (literal, solo para `/sdd-designer-system`)**: contenido literal de `design-guidelines.md` si existe.

4. **Resúmenes inline de los skills técnicos** (los subagentes no cargan skills; tienen que recibir todo aquí). Para EducaFlow:
   - `k-sistemas` (resumen extraído del propio SKILL.md del skill).
   - `k-validaciones` (resumen).
   - `k-vistas` (resumen, si aplica).
   - `k-seguridad` (resumen, si aplica).
   - **Importante:** estos resúmenes deben reflejar el estado *actual* de los skills, no una versión congelada. Re-leelos en cada iteración por si se han modificado en la Fase 3.

5. **FQNs de la infraestructura reutilizable** que aparezca relevante en el input.

6. **Las 3 tareas internas** que el skill objetivo encarga al subagente (las extraes de su SKILL.md):
   - Tarea 1 — construir el output.
   - Tarea 2 — detallar contenido y trazabilidad.
   - Tarea 3 — aplicar el checklist y corregir antes de devolver.

7. **El checklist literal** del skill objetivo.

8. **Formato de salida esperado** (la cabecera frontmatter NO se incluye — la añade el orquestador al guardar).

9. **Instrucción final**: "Devuelve únicamente el output en markdown. NO escribas ficheros. NO añadas metacomentarios. NO devuelvas múltiples versiones."

---

## Fase 1 — Iteración (ejecutar el skill objetivo)

Esta fase se ejecuta una vez por iteración. La numeración de iteración empieza en 1.

### 1.1 Lanzar 5 subagentes en paralelo

**REGLA CRÍTICA:** los 5 subagentes se lanzan en una **única respuesta** con 5 invocaciones a `Agent` simultáneas. Mismo prompt para los 5 (apuntando al fichero del paso 0.5). No `run_in_background` — necesitas las salidas para la unificación.

Prompt de cada subagente: "Lee el fichero `$TMPDIR/sdd_eval_subagent_prompt.md` completo y sigue al pie de la letra las instrucciones que contiene. Eres uno de 5 subagentes ejecutándose en paralelo con contexto fresco — produce decisiones genuinamente independientes. Devuelve únicamente el output en markdown como mensaje final, sin escribir ficheros, sin metacomentarios. Respeta las restricciones anti-trampa."

### 1.2 Unificar las 5 salidas (Tarea 2 del propio skill objetivo)

**Tú** (no un subagente) produces la salida unificada aplicando el algoritmo de unificación que el skill objetivo describe en su SKILL.md. Resumen genérico:

1. Compara las 5 salidas sección por sección.
2. Para cada divergencia, escoge la mejor opción según los principios de los skills técnicos. En empate, escoge la que minimiza ambigüedad para el siguiente consumidor (`/sdd-designer-system` o `/sdd-implementer-system`).
3. Construye una matriz de trazabilidad consolidada (si aplica al output).
4. Renumera consecutivamente sin huecos.
5. Aplica el checklist completo del skill objetivo sobre el output unificado.

### 1.3 Guardar el output

- Para `/sdd-designer-system`: guarda como `analysis_01/design_NN.md` con `NN = max(design_*.md existentes) + 1` y frontmatter `---type: design---`.
- Para `/sdd-analyst-system`: guarda como `analysis_NN/analysis.md` con `NN = max(analysis_NN/ existentes) + 1` y frontmatter `---type: analysis---`.

---

## Fase 2 — Diff estructural vs gold

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
| Vistas (si aplica) | Granularidad de ficheros, nombres de vistas, acciones | Lista por fichero |
| Seguridad (si aplica) | Permisos, condiciones JPQL, granularidad de `<can>` | Lista de permisos |
| Validaciones (`V-XXX` si aplica) | IDs, capas, mensajes | Tabla y diff |
| Reglas de negocio (`R-XXX` si aplica) | IDs, entidad, operación, momento (Antes/Después) | Tabla y diff |
| Reglas de UI (`U-XXX` si aplica) | IDs, disparador, efecto, campo/panel afectado | Tabla y diff |
| Matriz de trazabilidad (si aplica) | Cobertura: cada `V-XXX`/`R-XXX`/`U-XXX` ubicado en clase+método o fichero+acción | Recorrido fila por fila |
| Asunciones / notas | Lista de asunciones marcadas con `*` | Lista y diff |

Para cada eje produce un veredicto: **OK**, **diferencia cosmética**, o **divergencia significativa**.

### Clasificación de divergencias significativas (obligatorio)

Cada divergencia significativa se clasifica en una de estas categorías:

- **A — Falta de instrucción en el skill**: el skill objetivo (o un skill técnico que aplica) no le dice al subagente que produzca/respete eso. **Acción:** modificar el skill correspondiente con una regla genérica.

- **B — Conocimiento técnico ausente**: el subagente no sabe qué patrón aplicar porque falta en los skills `k-*`. **Acción:** añadir el patrón al skill `k-*` correspondiente.

- **C — Ambigüedad en el input**: el `analysis.md` o `user-story.md` no aporta suficiente información para que el subagente decida bien. **Acción:** mejorar el skill que produce ese input (o añadir guía a `design-guidelines.md` si es decisión local del subsistema).

- **D — Alternativa legítima**: dos opciones igualmente válidas. El gold escogió una; el unificado escogió otra; ambas son aceptables. **Acción:** ninguna (documentar y pasar).

---

## Fase 3 — Refinamiento (con aprobación del usuario)

Para cada divergencia A/B/C, propone al usuario con `AskUserQuestion` qué hacer:

- Texto exacto del cambio a aplicar al skill (path al fichero + sección + diff propuesto).
- Justificación: qué divergencia resuelve y por qué la solución es genérica (no específica del gold actual).
- Opciones: aplicar / posponer / descartar.

**Reglas críticas para los cambios al skill:**

1. **Genéricos, jamás específicos del gold.** Si el cambio menciona el nombre del subsistema/sistema concreto del gold, está mal — replantéalo. Por ejemplo, NO añadas a `k-sistemas/SKILL.md` "para firmas, usa 4 ficheros"; SÍ añade "cuando hay múltiples `<action-view>` por estado/perfil/caso de uso, cada uno va en su propio fichero".

2. **Razonamiento incluido.** Cada regla nueva debe incluir el porqué (en formato `> **Razón:** ...` o equivalente), para que el lector futuro entienda y pueda decidir en casos límite.

3. **Coherencia con el resto del skill.** Si el cambio entra en un checklist, también debe haber un bullet en las "reglas obligatorias" o sección equivalente. Si el cambio modifica un ejemplo, también deben actualizarse los ejemplos paralelos.

4. **No tocar `/sdd-implementer-system`** salvo que el problema sea claramente suyo. Es el ejecutor final, no el productor de los outputs que se evalúan aquí.

5. **Si la divergencia se debe a una regla local del subsistema** (no genérica), el cambio NO va al skill — va a `design-guidelines.md` del workspace de evaluación. Y avisas al usuario que esa regla quedará guardada como "decisión local de este caso", no como regla del framework.

Tras aplicar los cambios:
- Re-construye el prompt del subagente (paso 0.5) con los skills actualizados.
- **Limpia el caché de outputs anteriores** (no los borres, simplemente apunta a la nueva iteración).
- Vuelve a Fase 1 con `iteración = N+1`.

---

## Criterios de parada

Detén el bucle cuando se cumpla **alguno** de estos:

1. La cobertura semántica vs gold es ≥ 95% (todas las divergencias significativas son D, o solo quedan diferencias cosméticas).
2. El usuario decide cerrar (`AskUserQuestion` lo permite).
3. **3 iteraciones consecutivas sin reducción del número de divergencias A/B/C** → el skill no converge; pide intervención del usuario para revisar el caso de prueba o el algoritmo de unificación.
4. Una divergencia A/B/C **no es resoluble sin una regla específica del gold** → es D enmascarada; clasifícala como D y continúa.

---

## Artefactos generados

Al final del experimento, el workspace contiene:

```
.sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill}-{nombre}/
├── user-story.md (o analysis.md como input según el skill)
├── design-guidelines.md (opcional)
├── iteraciones.md
└── analysis_NN/
    ├── analysis.md
    ├── design_01.md (gold) | analysis_01/analysis.md (gold)
    ├── design_02.md, design_03.md, … (iteraciones para sdd-designer-system)
    └── (analysis_02/, analysis_03/, … para sdd-analyst-system)
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

## Mensaje de transición tras parar

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

NO ejecutes el skill objetivo de forma "de producción" tras evaluar — los workspaces de eval son experimentos, no entregables.
