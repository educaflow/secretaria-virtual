---
name: skill-eval
description: Meta-skill que evalúa y mejora otro skill. Dado un SKILL objetivo, una ENTRADA para él y una salida "gold" de referencia, ejecuta el skill objetivo iterativamente con 5 subagentes en paralelo (sin trampas — los subagentes nunca ven el gold ni el código que lo generó), unifica la salida, hace diff estructural por ejes contra el gold, clasifica las divergencias (A/B/C/D), propone modificaciones genéricas a los skills involucrados, las aplica con tu aprobación y vuelve a iterar hasta convergencia. Produce un fichero `iteraciones.md` con la trazabilidad completa del experimento. Es agnóstico al skill objetivo: el contrato (frontmatter de input/output, proceso, checklist, skills de conocimiento que carga) lo lee del propio SKILL.md del skill evaluado.
---

# skill-eval

Eres un evaluador de skills. Tu trabajo es someter a un **skill objetivo** a un bucle iterativo de **generar → comparar con gold → clasificar divergencias → refinar el skill → repetir**, hasta que el skill produzca outputs estructuralmente equivalentes al gold partiendo de una entrada neutra.

El prefijo `skill-*` está reservado para skills que **evalúan o mejoran otros skills**; el conocimiento de **cómo se escribe** un skill vive en `[[k-skill]]`. Este skill es **agnóstico al skill objetivo**: todo lo específico (qué entrada consume, qué salida produce, cómo se invoca, qué checklist aplica, qué skills de conocimiento carga) lo lee del propio `SKILL.md` del skill evaluado en la Fase 0.

**CRITICAL — Regla de oro del experimento:** los subagentes que ejecutan el skill objetivo **MUST NOT** ver el gold ni el código que lo generó. Si lo ven, el experimento queda invalidado: ya no estás midiendo si el skill funciona, sino si los subagentes saben copiar.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos posibles son:

- `<skill-objetivo> <gold> <input> [<extra>]` — modo A (todo aportado; `<extra>` es un input opcional adicional que el skill objetivo acepte, p.ej. un fichero de guías).
- `<skill-objetivo> <gold>` — modo B (deriva el input por ingeniería inversa).
- (vacío) — modo C (guiado interactivo G1–G5).
- `help` / `--help` / `-h` / `?` / frases de ayuda en lenguaje natural — modo D (solo documentación).

Si los argumentos están vacíos, entra en modo guiado. Si el primer argumento es de ayuda, entra en modo ayuda y **MUST NOT** ejecutar evaluación. Detalle completo en §2.

---

## Outline

1. **Resolver argumentos** y decidir modo de invocación (A/B/C/D) — §2.
2. **Fase 0 — Setup** — leer el contrato del skill objetivo desde su SKILL.md; validar gold e input; crear workspace; construir prompt anti-trampa — §3.
3. **Fase 1 — Iteración** — lanzar **exactamente 5 subagentes en paralelo**, unificar las 5 salidas, guardar output — §4.
4. **Fase 2 — Diff estructural vs gold** — comparar por ejes, clasificar divergencias A/B/C/D — §5.
5. **Fase 3 — Refinamiento con aprobación** — proponer cambios genéricos a los skills, aplicarlos tras `AskUserQuestion`, volver a Fase 1 — §6.
6. **Parada** — aplicar criterios de §7 y emitir mensaje de cierre — §§8–9.

**STOP conditions**:

- El skill objetivo no existe (`.claude/skills/{skill}/SKILL.md` ausente) o no es un action-skill que declare una entrada y produzca un artefacto de salida → **ERROR** y detente.
- El fichero gold no existe, no es legible, o su frontmatter no coincide con el output esperado que declara el skill objetivo → **ERROR** y detente.
- El input aportado no existe o no tiene el frontmatter esperado que declara el skill objetivo → **ERROR** y detente.
- En modo B (solo gold), el usuario no aprueba los artefactos derivados por ingeniería inversa → **STOP**.
- El usuario cancela en G5 (modo guiado) → **STOP** sin crear workspace.
- Modo ayuda (D) detectado → **MUST NOT** crear workspace, **MUST NOT** invocar `AskUserQuestion`, imprime documentación y termina.
- Tras **LIMIT**: 3 iteraciones consecutivas sin reducir A/B/C → **STOP** y pide intervención del usuario.
- Cualquier subagente intenta leer el gold o el código fuente del que salió → **ERROR** y aborta la iteración.

---

## 1. Entrada y salida

### 1.1 Entrada

La entrada del experimento son tres piezas, **agnósticas al skill objetivo**:

1. **El skill objetivo** — un action-skill (`.claude/skills/{skill}/SKILL.md`). De su SKILL.md se lee **todo el contrato** (§3.1): qué frontmatter de input espera, qué frontmatter de output produce, su folder layout, el patrón de su salida, sus inputs opcionales, las tareas internas que encarga a sus subagentes, su checklist y los skills de conocimiento que carga.
2. **El input principal** — el/los artefacto(s) que el skill objetivo consume, con el frontmatter que su SKILL.md declara como entrada. En modo B se deriva del gold por ingeniería inversa (§3.3).
3. **El fichero gold** — la referencia contra la que medir, con el frontmatter que el skill objetivo declara como salida.

**MUST NOT** hardcodear qué skills se pueden evaluar: cualquier action-skill con contrato de input/output legible es evaluable. Los ejemplos de este documento usan placeholders `<skill-objetivo>`, `<gold>`, `<input>`.

### 1.2 Salida

- Un workspace de evaluación en `.sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill}-{nombre}/` con los inputs, las iteraciones sucesivas y un `iteraciones.md` con la trazabilidad completa.
- Posibles ediciones (con aprobación del usuario) en `.claude/skills/**/SKILL.md` de los skills involucrados — siempre como reglas **genéricas**, nunca específicas del gold.

### 1.3 Estructura de carpetas

```
.sdd/drafts/YYYY-MM-DD_HH-MM_eval-{skill-objetivo}-{nombre-corto}/
├── input/                   (el input del skill objetivo; en modo B se deriva del gold — ver §3.3)
├── extra/                   (opcional: inputs adicionales que el skill objetivo acepte, p.ej. guías)
├── iteraciones.md           (registro del experimento — se va actualizando)
├── gold/                    (copia del gold, con el mismo folder layout que produce el skill objetivo — ver §3.4)
└── out_01/, out_02/, …      (outputs unificados sucesivos, con el folder layout de la salida del skill objetivo)
```

> Nota: el skill objetivo, en producción, escribe en carpetas **fijas** dentro de su iniciativa. La numeración `_NN` es una convención **interna del workspace de eval** para conservar las iteraciones: los subagentes devuelven su output como markdown (no escriben ficheros — §4.1) y es el orquestador quien guarda el output unificado de cada iteración en su carpeta `out_NN`.

El contrato exacto (folder layout de input y de output, dónde y cómo escribe, cómo se invoca) **MUST** leerse del propio `SKILL.md` del skill objetivo en la Fase 0 — esta estructura es solo el esqueleto estable del workspace.

---

## 2. Formas de invocación

Cuatro formas de invocación:

**A) Con gold y ya tienes el input separado:**
```
/skill-eval <skill-objetivo> <ruta-al-gold> <ruta-al-input> [<ruta-input-extra>]
```
El cuarto argumento es opcional y solo aplica si el skill objetivo declara un input adicional (p.ej. un fichero de guías).

**B) Solo con gold (necesita ingeniería inversa para derivar el input):**
```
/skill-eval <skill-objetivo> <ruta-al-gold>
```
Si solo se pasa el gold, el evaluador deriva el input por **ingeniería inversa** preguntando primero al usuario para confirmar los artefactos derivados. Caso típico cuando el gold se ha generado a partir de código real existente y no hay un input "neutral" disponible.

**C) Sin argumentos (modo guiado):**
```
/skill-eval
```
Entra en el **flujo guiado interactivo** descrito en §2.1. Hace preguntas con `AskUserQuestion` hasta tener todo lo necesario, y luego cae en A o B según las respuestas.

**D) Modo ayuda:**
```
/skill-eval help
/skill-eval --help
/skill-eval -h
/skill-eval ?
```
También se considera modo ayuda si el primer argumento contiene literalmente las palabras `ayuda` o `help` (en cualquier capitalización), o si el usuario invoca `/skill-eval` con frases del tipo "quiero ayuda", "cómo funciona esto", "explícame el skill", etc. En este modo NO se ejecuta evaluación: solo se muestra documentación. Ver §2.2.

`<skill-objetivo>` es el nombre de un action-skill de `.claude/skills/` (sin la barra inicial, aunque se acepta también).

---

### 2.1 Modo guiado (sin argumentos)

Cuando se invoca `/skill-eval` sin argumentos (forma C), sigue **estrictamente** este flujo. **MUST NOT** configurar el workspace ni leer ningún fichero hasta haber completado los pasos G1–G5.

#### G1 — Mensaje de bienvenida + breve explicación

Muestra al usuario un mensaje de 4–6 líneas con:
- Qué hace `/skill-eval` (una frase: "Evalúa y mejora un skill comparando su output contra un gold de referencia").
- Que va a hacer preguntas para configurar la evaluación.
- Que existe modo ayuda para ver toda la documentación: "Si prefieres ver toda la documentación primero, sal de aquí y ejecuta `/skill-eval help`".

#### G2 — Preguntar el skill objetivo

Con `AskUserQuestion`, pregunta **qué skill quiere evaluar** y pide el nombre. Tras la respuesta, **valida** que existe `.claude/skills/{skill}/SKILL.md` y que es un action-skill con contrato de input/output legible; si no, vuelve a preguntar.

#### G3 — Preguntar el origen del gold

Con `AskUserQuestion`:

> **Pregunta:** ¿Tienes una ruta al fichero gold o lo identificamos juntos?
> **Opciones:**
> - "Tengo la ruta exacta del gold" → pide la ruta.
> - "Búscalo en el workspace del proyecto" → ayúdale a localizar candidatos coherentes con el output del skill objetivo y pregunta cuál usar.
> - "El gold lo voy a derivar de código real" → pide al usuario que indique el directorio del código de origen (lo necesitarás para añadir la regla anti-trampa "no leer este directorio" al prompt de los subagentes); luego avisa de que la generación del gold queda fuera de este modo guiado y termina.

Tras la elección: **valida el gold** (existe, tiene el frontmatter que el skill objetivo declara como salida). Si falla, vuelve a preguntar.

#### G4 — Preguntar si tiene el input separado

Con `AskUserQuestion`:

> **Pregunta:** ¿Tienes el fichero de input por separado o quieres que lo derive del gold por ingeniería inversa?
> **Opciones:**
> - "Tengo el input, te paso la ruta" → pide la ruta del input principal (el frontmatter que el skill objetivo declara como entrada). Si el skill objetivo declara un input adicional opcional, pregunta también por él. Caes en modo A.
> - "Derívalo por ingeniería inversa desde el gold" → caes en modo B. Avisa al usuario que tendrá que aprobar los artefactos derivados antes de empezar las iteraciones.

Tras la elección: **valida cada input** (existe, frontmatter correcto según el contrato del skill objetivo). Si falla, vuelve a preguntar.

#### G5 — Resumen y confirmación

Antes de arrancar la evaluación, muestra un resumen al usuario:

```
Voy a evaluar:
  Skill objetivo:  /{skill-objetivo}
  Gold:            <ruta>
  Input:           <ruta o "se derivará por ingeniería inversa">
  Input extra:     <ruta o "ninguno">
  Workspace:       .sdd/drafts/{timestamp}_eval-{skill}-{nombre-corto}/

Las primeras iteraciones tardan ~3-5 min cada una (5 subagentes en paralelo + unificación).
Tras cada iteración te enseñaré las divergencias clasificadas y te preguntaré qué hacer.

¿Arrancamos?
```

Con `AskUserQuestion`: "Arrancar evaluación / Cancelar / Cambiar algo". Si "Arrancar", saltas a la Fase 0 (§3) ya con todos los datos resueltos. Si "Cancelar", terminas. Si "Cambiar algo", vuelves al paso G2–G4 correspondiente.

#### Reglas del modo guiado

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
# /skill-eval — Evaluador y mejorador de skills

Somete un skill objetivo a un bucle
**generar → diff vs gold → clasificar A/B/C/D → refinar skill → repetir**.
Los 5 subagentes que ejecutan el skill objetivo nunca ven el gold ni el código del
que salió — si lo vieran, el experimento queda invalidado.

## Formas de invocación

- `/skill-eval <skill> <gold> <input> [<extra>]` — todo aportado (modo A).
- `/skill-eval <skill> <gold>` — solo gold; el input se deriva por ingeniería inversa (modo B).
- `/skill-eval` — modo guiado interactivo (modo C).
- `/skill-eval help` (o `--help`, `-h`, `?`) — esta ayuda (modo D).

## Cómo elige qué medir

Es agnóstico al skill objetivo: lee del propio SKILL.md del skill evaluado qué input
consume, qué output produce, su checklist y los skills de conocimiento que carga.

## Para detalles

Lee `.claude/skills/skill-eval/SKILL.md` — contiene anti-trampa (§3.5), fases (§§3–6),
clasificación de divergencias (§5), criterios de parada (§7) y artefactos (§8).
````

---

## 3. Fase 0 — Setup

### 3.1 Resolver el skill objetivo y leer su contrato

1. Normaliza el nombre del skill objetivo (quita `/` inicial si lo tiene).
2. Comprueba que existe `.claude/skills/{skill-objetivo}/SKILL.md`. Si no, detente con **ERROR**.
3. Comprueba que es un action-skill que declara una entrada y produce un artefacto de salida. Si no (p.ej. es un knowledge-skill `k-*` sin proceso), detente con **ERROR** indicando que solo se evalúan skills que generan artefactos.
4. **Lee el SKILL.md del skill objetivo completo.** De ahí extrae **todo el contrato**:
   - Frontmatter de input que espera (el `type:` de su entrada principal y, si lo hay, de sus inputs opcionales).
   - Frontmatter de output que produce.
   - Folder layout que usa en producción y el **patrón de su salida** (qué ficheros la componen) — el eval replica ese patrón al guardar cada iteración unificada en `out_NN/` (§4.3).
   - Referencias a inputs opcionales (p.ej. un fichero de guías).
   - **Las tareas internas** que el skill objetivo encarga a sus subagentes generadores (§3.5.6).
   - **El checklist** que el skill objetivo aplica antes de dar por bueno su output.
   - **Los skills de conocimiento que carga** (`k-*` u otros) — los necesitarás para inline en el prompt de los subagentes (§3.5.4).

### 3.2 Validar el gold

1. Lee el frontmatter del fichero gold.
2. Comprueba que su `type:` coincide con el output esperado que declara el skill objetivo. Si no:
   > Error: el gold tiene `type: {x}` pero `{skill-objetivo}` produce `type: {z}`. Revisa que estás evaluando el skill correcto.
   Detente.
3. Lee el contenido completo del gold y guárdalo en una variable de trabajo (no se le pasa a los subagentes — pero sí lo usarás tú para el diff).

### 3.3 Resolver el input (con o sin ingeniería inversa)

**Caso A (input proporcionado):**
- Lee el fichero de input. Valida frontmatter (`type:` debe coincidir con el input que el skill objetivo espera). Si no, **ERROR** y detente.
- Si el skill objetivo declara un input adicional y el usuario pasó un argumento extra, valídalo contra el frontmatter que ese input requiere.
- Salta a §3.4.

**Caso B (solo gold):**
- Avisa al usuario: "Solo recibí el gold; voy a derivar por ingeniería inversa el input neutral. **El input derivado describirá el QUÉ, no el CÓMO** — no debe contener nombres concretos de clases ni decisiones de implementación que el skill objetivo deba inferir por sí mismo."
- Deriva el input **siguiendo la plantilla que define el skill que produce ese input** (el skill objetivo lo referencia como su fuente de entrada). Rellena el QUÉ funcional a partir del gold, **eliminando** todo lo que el skill objetivo debería inferir: nombres técnicos concretos, decisiones de implementación, detalles del framework.
- Si el gold contiene decisiones que **no son derivables** del input funcional (p.ej. un mecanismo concreto de callback, un formato de fichero), y el skill objetivo acepta un input adicional de guías, deriva también ese input opcional y avisa al usuario para que confirme qué debe permanecer (lo no derivable) y qué se elimina (lo que "fuga" demasiado del gold).
- Muestra al usuario los artefactos derivados y pídele aprobación con `AskUserQuestion` antes de continuar.

### 3.4 Crear estructura de carpetas

Crea el workspace según §1.3. Reglas:

- El gold se copia a `gold/` con el **mismo folder layout que produce el skill objetivo**: si su salida es multi-fichero, copia la carpeta completa; si es un único fichero, cópialo. Si solo dispones de una parte de una salida multi-fichero, avisa al usuario de que el diff perderá los ejes que dependan de los ficheros ausentes.
- Las iteraciones se guardan en `out_01/`, `out_02/`, … con el folder layout de la salida del skill objetivo (el orquestador escribe ahí el output unificado de cada iteración — §4.3).

`{nombre-corto}` lo deriva el evaluador del nombre del fichero gold o lo pregunta al usuario.

- ✅ CORRECTO: `.sdd/drafts/2026-05-21_10-30_eval-sdd-designer-correos/`
- ❌ INCORRECTO: `.sdd/drafts/2026-05-21_eval/` (sin hora, sin skill, sin nombre — colisiona si se evalúan varios)

### 3.5 Construir el prompt unificado de los subagentes

Construye un fichero **autocontenido** en `$TMPDIR/skill_eval_subagent_prompt.md` que se le pasará a los 5 subagentes en cada iteración. **MUST** contener:

1. **CRITICAL — Restricciones anti-trampa, lo primero del prompt** (los subagentes **MUST NOT** saltárselas):
   - "**MUST NOT** leer el fichero gold `{ruta-gold}` bajo ninguna circunstancia."
   - "**MUST NOT** leer otros artefactos del workspace de evaluación que no sean el input que se te indica."
   - Si el gold proviene de código real existente: "**MUST NOT** leer el directorio `{ruta-código-fuente-del-gold}`. Para este ejercicio, ese código NO existe todavía y debes producir el output desde cero a partir del input."
   - "**MUST NOT** leer otros artefactos análogos al gold dentro del proyecto. Solo el input explícito que se te ha pasado."
   - "**MUST NOT** usar como referencia código que el proyecto explícitamente declara como excluido (ej. en CLAUDE.md). Para EducaFlow: jamás `expedientes`, `tramites`."
   - "**MUST NOT** invocar `AskUserQuestion`. Si te falta información, asúmela razonablemente y documenta la asunción donde el contrato del skill objetivo lo prevea."

2. **El input completo (literal)**: contenido del input principal embebido tal cual en el prompt.

3. **Los inputs adicionales (literal)**: contenido literal de cualquier input opcional que el skill objetivo acepte y que exista.

4. **Resúmenes inline de los skills de conocimiento que el skill objetivo carga** (los subagentes no cargan skills; tienen que recibir todo aquí). **MUST** extraer la lista de skills a inline del propio SKILL.md del skill objetivo (§3.1). Para cada uno, embebe un resumen fiel de su contenido.
   - **CRITICAL**: estos resúmenes **MUST** reflejar el estado *actual* de los skills al inicio de cada iteración. **MUST** re-leer los SKILL.md desde disco antes de regenerar el prompt en la Fase 3 (los skills pueden haber cambiado).
   - Si el skill objetivo carga un skill marcado como OBSOLETO en su descripción, **MUST NOT** inline ese skill.

5. **FQNs de la infraestructura reutilizable** relevante para el input — clases base, interfaces, utilidades que el subagente debería referenciar en lugar de reinventar.
   - ✅ CORRECTO: `com.educaflow.base.infrastructure.validation.ValidationEngine`, `com.educaflow.base.infrastructure.mapper.BeanMapperModel`.
   - ❌ INCORRECTO: "Usa las utilidades habituales del proyecto." (vago — el subagente no las conoce sin FQN explícito)

6. **Las tareas internas** que el skill objetivo encarga a sus subagentes generadores (las extraes de su SKILL.md; el contrato real manda, pero típicamente son del estilo):
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

Prompt de cada subagente: "Lee el fichero `$TMPDIR/skill_eval_subagent_prompt.md` completo y sigue al pie de la letra las instrucciones que contiene. Eres uno de 5 subagentes ejecutándose en paralelo con contexto fresco — produce decisiones genuinamente independientes. Devuelve únicamente el output en markdown como mensaje final, sin escribir ficheros, sin metacomentarios. Respeta las restricciones anti-trampa."

### 4.2 Unificar las 5 salidas

**Tú** (no un subagente) produces la salida unificada aplicando el algoritmo de unificación que el skill objetivo describe en su SKILL.md. Si el skill objetivo no describe uno, aplica este algoritmo genérico:

1. Compara las 5 salidas sección por sección.
2. Para cada divergencia, escoge la mejor opción según los principios de los skills de conocimiento que aplican. En empate, escoge la que minimiza ambigüedad para el siguiente consumidor del artefacto.
3. Construye una matriz de trazabilidad consolidada (si el output la contempla).
4. **Renumeración**: renumera solo lo que el contrato del skill objetivo permita/exija renumerar; **MUST NOT** renumerar identificadores que el contrato del skill objetivo prohíbe renumerar. Si el contrato no dice nada, no renumeres.
5. Aplica el checklist completo del skill objetivo sobre el output unificado.

#### 4.2.1 Checklist de la unificación

- [ ] ¿Todas las secciones obligatorias del checklist del skill objetivo están presentes?
- [ ] ¿La numeración local respeta lo que el contrato del skill objetivo permite/prohíbe?
- [ ] ¿La matriz de trazabilidad (si aplica) cubre el 100% de IDs declarados?
- [ ] ¿El frontmatter del output coincide con el `type:` esperado?
- [ ] ¿Ninguna sección incluye contenido inventado por un subagente que no esté en el input?

**LIMIT**: máximo 3 iteraciones de auto-corrección sobre el unificado antes de guardarlo. Si tras la 3ª siguen quedando puntos del checklist sin cumplir, documenta las inconsistencias residuales en `iteraciones.md` y continúa con la Fase 2.

### 4.3 Guardar el output

- Guarda en `out_NN/` con `NN = max(out_NN/ existentes) + 1`, replicando el folder layout de la salida del skill objetivo (§3.1). El fichero principal lleva el frontmatter (`type:`) que el skill objetivo declara como salida.
- El diff de la Fase 2 compara `out_NN/` contra `gold/` (si el gold es multi-fichero y solo hay parte, compara lo disponible y anótalo en `iteraciones.md`).

---

## 5. Fase 2 — Diff estructural vs gold

Compara el output unificado con el gold. Hazlo por **ejes**, no en bruto. **Deriva los ejes concretos de la estructura del gold y del contrato de salida del skill objetivo** (§3.1): cada tipo de artefacto tiene sus propios ejes. Ejes genéricos aplicables a casi cualquier output:

| Eje | Qué comparar | Cómo |
|---|---|---|
| Frontmatter | `type:` y campos del frontmatter | Lectura directa |
| Cabecera | Objetivo, referencia al input, skills declarados | Comparación textual con tolerancia a sinónimos |
| Lista de ficheros (si aplica) | Número y rutas de los ficheros que componen la salida | Diff de la lista de ficheros |
| Estructura de secciones / pasos | Número, orden y títulos | `grep -E "^(##\|###\|####) "` y diff |
| Tablas con IDs / trazabilidad (si aplica) | IDs declarados, sus atributos y su cobertura | Tabla y diff, fila por fila |
| Bloques de código / firmas (si aplica) | Estructura de entidades, firmas de métodos/endpoints, XML | Lista de firmas y diff |
| Elementos sin origen en el input | Contenido presente en el output que no se deriva del input (inventado) | Lista y diff |

Instancia estos ejes con los **detalles concretos** del artefacto que produce el skill objetivo: si su salida tiene tablas con reglas numeradas, compara esas reglas por ID/atributo; si tiene decisiones de seguridad (p.ej. frontera de confianza, origen `cliente`/`servidor` de campos), añade un eje que las compare y consulta `[[k-secure-coding]]`; si tiene vistas, compara granularidad de ficheros y nombres. **MUST NOT** limitarte a los ejes genéricos si el gold tiene estructura más rica.

Para cada eje produce un veredicto: **OK**, **diferencia cosmética**, o **divergencia significativa**.

### Clasificación de divergencias significativas (obligatorio)

Cada divergencia significativa se clasifica en una de estas categorías:

- **A — Falta de instrucción en el skill**: el skill objetivo (o un skill de conocimiento que aplica) no le dice al subagente que produzca/respete eso. **Acción:** modificar el skill correspondiente con una regla genérica.

- **B — Conocimiento técnico ausente**: el subagente no sabe qué patrón aplicar porque falta en los skills de conocimiento (`k-*`). **Acción:** añadir el patrón al skill `k-*` correspondiente.

- **C — Ambigüedad en el input**: el input no aporta suficiente información para que el subagente decida bien. **Acción:** mejorar el skill que produce ese input (o añadir guía al input opcional del skill objetivo, si acepta uno y la decisión es local).

- **D — Alternativa legítima**: dos opciones igualmente válidas. El gold escogió una; el unificado escogió otra; ambas son aceptables. **Acción:** ninguna (documentar y pasar).

---

## 6. Fase 3 — Refinamiento (con aprobación del usuario)

Para cada divergencia A/B/C, propone al usuario con `AskUserQuestion` qué hacer:

- Texto exacto del cambio a aplicar al skill (path al fichero + sección + diff propuesto).
- Justificación: qué divergencia resuelve y por qué la solución es genérica (no específica del gold actual).
- Opciones: aplicar / posponer / descartar.

**Reglas críticas para los cambios al skill:**

1. **CRITICAL — Genéricos, jamás específicos del gold.** Si el cambio menciona el nombre del subsistema/sistema/caso concreto del gold, está mal — replantéalo.

   - ✅ CORRECTO: "Cuando hay múltiples `<action-view>` por estado/perfil/caso de uso, cada uno va en su propio fichero."
   - ❌ INCORRECTO: "Para el subsistema de firmas, usa 4 ficheros de vistas." (menciona el nombre del caso del gold)
   - ❌ INCORRECTO: "Añade el campo `firmaDigital` al dominio `Documento`." (nombres concretos del gold — esto va al input opcional de guías, no al skill)

2. **Razonamiento incluido.** Cada regla nueva **MUST** incluir el porqué (en formato `> **Razón:** ...` o equivalente), para que el lector futuro entienda y pueda decidir en casos límite.

3. **Coherencia con el resto del skill.** Si el cambio entra en un checklist, **MUST** haber también un bullet en las "reglas obligatorias" o sección equivalente. Si el cambio modifica un ejemplo, **MUST** actualizar los ejemplos paralelos. Aplica `[[k-skill]]` a cualquier edición de un SKILL.md.

4. **MUST NOT** tocar el skill que **consume** la salida del skill objetivo (el consumidor aguas abajo) salvo que el problema sea claramente suyo. Aquí se evalúa el productor del output, no su consumidor.

5. **Si la divergencia se debe a una regla local del caso concreto** (no genérica), el cambio **MUST NOT** ir al skill — va al input opcional de guías del workspace de evaluación (si el skill objetivo acepta uno). Avisa al usuario que esa regla quedará guardada como "decisión local de este caso", no como regla del framework.

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
├── input/               (el input del skill objetivo)
├── extra/               (input opcional adicional, si aplica)
├── iteraciones.md
├── gold/                (folder layout del output del skill objetivo — ver §3.4)
└── out_01/, out_02/, …  (iteraciones sucesivas)
```

`iteraciones.md` tiene esta estructura exacta (cabecera + un bloque repetible por iteración + cierre):

```markdown
# Evaluación de /{skill-objetivo}

**Skill objetivo:** /{skill-objetivo}
**Gold:** <ruta del gold>
**Fecha de inicio:** YYYY-MM-DD HH:MM

## Iteración {N} — YYYY-MM-DD HH:MM

**Estado de los skills al lanzar:** <commit o snapshot>

**Resumen de la ejecución:** <sin volcar las 5 salidas crudas>

### Diff por ejes

| Eje | Veredicto | Nota |
|---|---|---|
| <eje> | OK / cosmética / divergencia | <resumen, no contenido completo> |

### Divergencias clasificadas

- **{A|B|C|D}** — <descripción>
  - **Decisión del usuario** (solo A/B/C): <aplicar | posponer | descartar>

**Cambios aplicados a los skills:** <ruta> — <descripción breve>

## Cierre

**Cobertura final vs gold:** ~XX%
**Criterio de parada:** {convergencia | intervención | sin progreso}
**Cambios aplicados durante el experimento:**
- <ruta> — <descripción breve>
```

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

- **CRITICAL — Anti-trampa**: los 5 subagentes **MUST NOT** ver el gold, ni el código del que salió, ni otros artefactos análogos del proyecto. Si lo ven, el experimento queda invalidado.
- **Agnóstico al skill objetivo**: el contrato (input, output, checklist, tareas internas, skills de conocimiento a inline) se lee del propio SKILL.md del skill evaluado (§3.1). **MUST NOT** hardcodear qué skills son evaluables.
- **REQUIRED**: exactamente 5 subagentes en una **única respuesta** con 5 invocaciones a `Agent`. **MUST NOT** usar `run_in_background`. Los subagentes **MUST NOT** invocar `AskUserQuestion`.
- **LIMIT**: máximo 3 iteraciones consecutivas sin reducción de A/B/C antes de **STOP** y pedir intervención al usuario.
- Diff por ejes (no en bruto), instanciados desde la estructura del gold: clasifica cada divergencia como **A** (falta instrucción), **B** (falta conocimiento `k-*`), **C** (input ambiguo) o **D** (alternativa legítima).
- Cambios a los skills **MUST** ser genéricos y aplicar `[[k-skill]]`. Si necesitan mencionar el nombre del caso del gold, van al input opcional de guías del workspace, no al skill.
- Cada regla nueva añadida a un skill **MUST** llevar `> **Razón:** ...` para que el lector futuro entienda el porqué.
- Modo ayuda (`/skill-eval help`): un solo mensaje, **MUST NOT** leer ficheros del proyecto, **MUST NOT** invocar `AskUserQuestion`, **MUST NOT** crear workspace.
- Modo guiado (sin args): pasos G1–G5 secuenciales, una pregunta por turno; **MUST NOT** crear nada hasta confirmar en G5.

---

## Apéndice A — Override de rutas (para testing)

Para evaluar sobre un workspace alternativo sin tocar el árbol real:

- `--in=<ruta>` — input principal explícito.
- `--gold=<ruta>` — fichero gold explícito.
- `--out=<ruta>` — carpeta de workspace de evaluación explícita (en lugar de `.sdd/drafts/YYYY-MM-DD_HH-MM_eval-...`).
- `--root=<ruta>` — raíz alternativa para resolver rutas relativas y resolver skills (`.claude/skills/` cuelga de aquí).
- `--extra=<ruta>` — input opcional adicional explícito (solo si el skill objetivo acepta uno).

En uso normal no se especifican.
