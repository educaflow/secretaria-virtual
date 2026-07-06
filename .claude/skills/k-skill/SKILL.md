---
name: k-skill
description: Reglas y plantilla para escribir skills del proyecto EducaFlow en `.claude/skills/`. Define los dos tipos de skill (knowledge `k-*` y action `sdd-*`/`code-*`), el frontmatter obligatorio, la estructura mínima de un action-skill al estilo `github/spec-kit` (User Input, Outline, Phases, Quick Guidelines), las convenciones de redacción (cuerpo en español, palabras-clave imperativas en inglés MUST/REQUIRED/CRITICAL/MUST NOT/STOP/ERROR/LIMIT), el uso de ejemplos ✅/❌ inline, límites numéricos duros, plantillas embebidas literales y checklists con bucles de auto-validación. Usa este skill como referencia siempre que diseñes, revises o refactorices un SKILL.md.
---

# k-skill

Eres un autor de skills. Tu trabajo es producir o revisar `SKILL.md` en `.claude/skills/` siguiendo las convenciones del proyecto. Este skill **es meta**: documenta cómo se escriben los demás skills y se aplica a sí mismo como ejemplo.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- Una ruta a un `SKILL.md` existente que hay que revisar/refactorizar contra estas reglas.
- Un nombre de skill nuevo (`k-foo`, `sdd-bar`) y una descripción funcional para crearlo desde cero.
- Una pregunta puntual sobre convenciones (p.ej. "¿dónde van los hooks?", "¿cómo embebo una plantilla?").

Si los argumentos están vacíos, asume que el usuario pide la **referencia completa** y muestra el índice de secciones.

---

## Outline

1. **Decidir el tipo de skill** (§3) — knowledge (`k-*`) o action (`sdd-*`/`code-*`/imperativo).
2. **Escribir el frontmatter** (§4) — `name`, `description`, `allowed-tools` (opcional).
3. **Escribir el cuerpo** (§5) — H1 + intro + estructura según tipo.
4. **Aplicar las convenciones de estilo** (§6) — idioma, marcadores, ejemplos, límites, plantillas, checklists.
5. **Validar contra el checklist final** (§9) — **LIMIT**: máximo 3 iteraciones de corrección antes de dar por bueno el skill.

**STOP conditions**:

- El skill carece de `name` o `description` en el frontmatter → **ERROR** y detente.
- El nombre del fichero no es **exactamente** `SKILL.md` (en mayúsculas) dentro de una carpeta con el nombre del skill → **ERROR**.
- El skill mezcla tipo knowledge y action sin justificación → **STOP** y pregunta al usuario qué quiere.

---

## 1. Qué es un skill

Un skill es una unidad de conocimiento o de proceso que el modelo carga bajo demanda al invocarlo con `/<nombre-skill>` o cuando otro skill lo solicita por nombre. Vive en:

```
.claude/skills/<nombre-skill>/SKILL.md
```

**REQUIRED**:

- La carpeta **MUST** llamarse exactamente igual que el `name:` del frontmatter.
- El fichero **MUST** llamarse `SKILL.md` (mayúsculas, sin extensión adicional).
- **MUST NOT** existir más de un `SKILL.md` por skill.

Material auxiliar (plantillas, ejemplos, scripts) puede colgar de la misma carpeta — el `SKILL.md` los referencia por ruta relativa.

---

## 2. Principio cero — claridad sobre exhaustividad

Un skill se carga en contexto cada vez que se invoca. Cada línea **cuesta tokens**. Por tanto:

- **MUST** decir lo imprescindible para que el modelo haga su trabajo bien.
- **MUST NOT** repetir teoría general que el modelo ya sabe (p.ej. "qué es JPA", "cómo funciona git").
- **MUST NOT** convertirse en un manual de usuario: el skill se dirige al modelo, no a un humano que aprende.
- **REQUIRED**: si una regla puede expresarse como ejemplo ✅/❌, va como ejemplo y **no** como párrafo.
- **REQUIRED**: si un contenido es una secuencia de acciones o un conjunto de reglas, va en **formato receta** —lista numerada, guiones o fases (ver §6.1)— y **no** como prosa.

**Regla práctica de duda**: ¿esta línea cambiaría la salida del modelo si la quito? Si la respuesta es **no**, sobra.

---

## 3. Tipos de skill

El proyecto distingue **dos tipos**. Cada uno tiene una estructura mínima distinta.

### 3.1 Knowledge skills (`k-*`)

Encapsulan **conocimiento de dominio reutilizable**: convenciones, patrones, vocabulario, decisiones arquitectónicas. Otros skills (sobre todo los `sdd-*` y `code-*`) los cargan como referencia.

**Ejemplos**: `k-sistemas`, `k-vistas`, `k-validaciones`, `k-i18n`, `k-scheduler`, `k-code-quality`, `k-playwright`.

**No hacen cosas — describen cosas.** No tienen "fases" ni invocan subagentes.

**Estructura mínima**:

```markdown
---
name: k-<tema>
description: <una frase que diga qué cubre y cuándo aplicar este skill>
---

# k-<tema>

<Frase de entrada: qué cubre, a quién va dirigido (qué otros skills lo usan).>

## 1. Conceptos clave
<Vocabulario y modelo mental del dominio.>

## 2. Convenciones del proyecto
<Reglas específicas de EducaFlow, no del framework genérico.>

## 3. Patrones recomendados
<Plantillas literales de código/XML/markdown para copiar y adaptar.>

## 4. Ejemplos ✅/❌
<Casos correctos e incorrectos con explicación corta.>

## 5. Anti-patrones
<Qué NUNCA hacer y por qué.>
```

### 3.2 Action skills (`sdd-*`, `code-*`, imperativos)

Ejecutan un **proceso multi-fase**: leen artefactos, hacen preguntas, generan ficheros, invocan subagentes, validan resultados.

**Ejemplos**: `sdd-specification`, `sdd-analyst-system`, `sdd-designer`, `sdd-implementer`, `sdd-close`, `code-implementer`, `code-reviewer`.

**Estructura mínima OBLIGATORIA** (inspirada en `github/spec-kit`):

```markdown
---
name: <nombre-skill>
description: <una frase larga que diga qué hace, entrada esperada, salida producida>
---

# <nombre-skill>

<Frase de entrada: rol que asume el modelo + qué transforma en qué.>

## User Input
​```text
$ARGUMENTS
​```
<Cómo interpretar los argumentos.>

## Outline
1. **<Verbo>** … (Fase 0)
2. **<Verbo>** … (Fase 1)
…

**STOP conditions**:
- <Caso bloqueante 1> → **ERROR** / **STOP** / **MUST NOT**
- <Caso bloqueante 2> → …

## 1. Entrada y salida
### 1.1 Entrada
### 1.2 Salida
### 1.3 Estructura de carpetas

## 2. Principios (aplican a todas las fases)
### 2.1 …
### 2.2 …

## 3. Flujo general
<Diagrama ASCII de las fases.>

## 4..N. Fase 0..N — <Nombre de la fase>
<Detalle paso a paso de cada fase.>

## Quick Guidelines
- <5-7 bullets escaneables con los principios condensados.>

## Apéndice A — Overrides (opcional)
<Flags `--in=`, `--out=`, `--root=` para testing en sandbox.>
```

**REQUIRED — secciones que siempre aparecen** en un action-skill:

1. Frontmatter con `name` + `description`.
2. `## User Input` con bloque `$ARGUMENTS`.
3. `## Outline` con pasos numerados y **STOP conditions**.
4. Al menos una sección de detalle por fase del Outline.
5. `## Quick Guidelines` al final con los principios condensados.

---

## 4. Frontmatter

### 4.1 Campos obligatorios

| Campo | Obligatorio | Para qué sirve |
|-------|-------------|----------------|
| `name` | **MUST** | Identificador del skill. **MUST** coincidir con el nombre de la carpeta contenedora. |
| `description` | **MUST** | Frase larga (1-3 oraciones) que el orquestador lee para decidir si carga este skill. **MUST** mencionar qué hace, qué entrada espera y qué salida produce. |

### 4.2 Campos opcionales

| Campo | Cuándo se usa |
|-------|---------------|
| `allowed-tools` | En action-skills que quieren restringir las herramientas disponibles. Lista **separada por comas** de tools, con scoping opcional: `Bash(xmllint:*)`, `Write(.sdd/**)`, `Edit(tests/**)`, `Read`, `Skill`, `Agent`. **MUST** incluir todas las herramientas que el cuerpo del skill ordena usar (si el cuerpo manda editar un fichero, `Edit(...)` debe estar en la lista). |

### 4.3 Ejemplos ✅/❌

- ✅ CORRECTO:
  ```yaml
  ---
  name: sdd-analyst-system
  description: Dado un fichero `specification.md`, genera los artefactos de análisis (analysis.md + entity-*.md + screen-*.md + tests.md) con trazabilidad `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` → V/R/U. La entrada la produce `/sdd-specification` y la salida la consume `/sdd-designer`.
  ---
  ```

- ❌ INCORRECTO:
  ```yaml
  ---
  name: SDD Analyst
  description: Analyzes specs.
  ---
  ```
  Razones: `name` con espacios y mayúsculas (no coincide con nombre de carpeta), `description` demasiado corta y sin mencionar entrada/salida.

- ❌ INCORRECTO:
  ```yaml
  ---
  description: ...
  ---
  ```
  Razones: falta `name`. **ERROR**.

---

## 5. Cuerpo del skill

### 5.1 Idioma

- **MUST** escribir el cuerpo del skill en **español** (es el idioma del proyecto EducaFlow).
- **MUST** mantener las **palabras-clave imperativas en inglés**: `MUST`, `MUST NOT`, `REQUIRED`, `CRITICAL`, `STOP`, `ERROR`, `LIMIT`. Razones: (a) son palabras-señal visuales reconocibles, (b) el modelo las trata como tokens imperativos con peso especial, (c) coinciden con el estilo de `github/spec-kit` y otros frameworks de prompts.
- **MUST NOT** mezclar prosa en inglés con prosa en español dentro del mismo párrafo (salvo las palabras-clave anteriores).

### 5.2 Tono

- Directo, técnico, sin adornos. El modelo no necesita motivación ni introducciones literarias.
- Imperativo: "lee", "valida", "guarda" — **no** "podrías leer", "sería buena idea validar".
- Segunda persona del singular ("haz", "valida", "lanza"). El destinatario del skill es el modelo.

### 5.3 Encabezados

- H1 (`#`) **MUST** coincidir con el `name:` del frontmatter.
- H2 (`##`) para secciones principales (User Input, Outline, Fases, Quick Guidelines).
- H3 (`###`) para subdivisiones de fase.
- **MUST NOT** anidar más allá de H4. Si necesitas H5, replantéate la estructura.

---

## 6. Convenciones de estilo (transversales)

### 6.1 Formato receta sobre prosa

**CRITICAL**: un skill es una **receta para el modelo**, no un ensayo. Cualquier contenido que sea una secuencia de acciones o un conjunto de reglas **MUST** expresarse en **formato receta** (estructura escaneable), **no** como párrafos.

Vale **cualquier** estructura tipo receta, la que mejor encaje con el contenido:

1. **Lista numerada jerárquica** (`1.`, `1.1`, `1.2`, `2.`…) — para un proceso con orden y sub-pasos.
2. **Lista con guiones** (`-`) — para un conjunto de reglas o elementos sin orden estricto.
3. **Fases** (`## N. Fase N — <Nombre>`) — para procesos largos multi-etapa (patrón de los `sdd-*`).

Reglas:

1. **MUST** convertir cada párrafo que describa "qué hacer" o "qué reglas aplican" en una de las estructuras anteriores (un ítem = una acción o una regla).
2. **MUST NOT** dejar un párrafo de más de ~3 líneas cuando su contenido es enumerable.
3. **REQUIRED**: la prosa solo se reserva para una frase de contexto que introduce la lista o la fase.
4. Al **revisar** un skill existente: detecta los párrafos enumerables y reescríbelos en formato receta; mantén el sentido, no el formato.
5. La elección entre numeración / guiones / fases es de estilo — lo **MUST** es que no sea prosa; lo concreto es libre.

**Ejemplos**:

- ✅ CORRECTO (pasos numerados):
  ```markdown
  1. Lee el fichero de entrada.
     1.1 Valida el frontmatter.
     1.2 Si falta `name` → **ERROR** y detente.
  2. Genera el artefacto y aplica el checklist §N.
  ```
- ✅ CORRECTO (guiones para reglas):
  ```markdown
  - **MUST** validar el frontmatter antes de generar nada.
  - **MUST NOT** sobrescribir un fichero existente sin preguntar.
  ```
- ✅ CORRECTO (fases): `## 4. Fase 1 — Generar artefactos`
- ❌ INCORRECTO: `Primero lee el fichero, y una vez leído conviene validar el frontmatter; si por casualidad falta el name entonces es un error y debes detenerte antes de generar nada.` (prosa enumerable → **MUST** ser formato receta)

### 6.2 Marcadores imperativos en inglés

Usa estos marcadores **solo** para instrucciones realmente bloqueantes o críticas. Si los usas en todo el documento pierden su peso visual.

| Marcador | Cuándo usarlo |
|----------|---------------|
| `**MUST**` | Obligación absoluta. Si no se cumple, el resultado es incorrecto. |
| `**MUST NOT**` | Prohibición absoluta. |
| `**REQUIRED**` | Equivalente más suave a MUST, para describir un requisito previo o una sección obligatoria. |
| `**CRITICAL**` | Refuerza algo que el modelo suele equivocar (p.ej. lanzar subagentes en paralelo en una única respuesta). |
| `**STOP**` | El skill **debe detenerse** y esperar al usuario o salir limpiamente. |
| `**ERROR**` | Condición de error que aborta el flujo con un mensaje al usuario. |
| `**LIMIT**` | Límite numérico duro (máx N preguntas, máx N iteraciones, exactamente N subagentes). |

**Ejemplos**:

- ✅ CORRECTO: `**CRITICAL**: Lanza **exactamente 5 subagentes en paralelo** en una única respuesta. **MUST NOT** lanzarlos secuencialmente.`
- ✅ CORRECTO: `**LIMIT**: máximo 3 iteraciones de corrección; si tras la 3ª siguen fallando ítems, avisa al usuario.`
- ❌ INCORRECTO: `**MUST** leer el fichero. **MUST** validar el frontmatter. **MUST** continuar con la Fase 1. **MUST** preguntar al usuario.` (uso excesivo, pierden peso)
- ❌ INCORRECTO: `Es muy importante que recuerdes leer el fichero antes de continuar.` (prosa débil; el imperativo debería ir como `**MUST** leer el fichero antes de continuar`)

### 6.3 Ejemplos ✅/❌ inline

Para formatos rígidos (plantillas, IDs, identificadores, frontmatters, comandos), **MUST** dar ejemplos ✅ correctos y ❌ incorrectos con anotación corta del fallo. Un ejemplo vale más que tres párrafos de descripción.

**Patrón**:

```markdown
- ✅ CORRECTO: `<ejemplo>`
- ✅ CORRECTO: `<otro ejemplo válido>`
- ❌ INCORRECTO: `<ejemplo malo>` (<razón corta>)
- ❌ INCORRECTO: `<otro ejemplo malo>` (<razón corta>)
```

**REQUIRED**: cada ❌ **MUST** llevar una razón entre paréntesis.

### 6.4 Límites numéricos duros

Cuando una instrucción admita cardinalidad ("haz preguntas", "itera la validación", "lanza subagentes"), **MUST** expresarla como **LIMIT** numérico explícito, no como adjetivo vago.

- ✅ CORRECTO: `**LIMIT**: máximo 12 preguntas por ronda. **LIMIT**: máximo 3 rondas antes de proceder con asunciones marcadas con *.`
- ✅ CORRECTO: `**REQUIRED**: exactamente 5 subagentes, ni más ni menos.`
- ❌ INCORRECTO: `Haz preguntas hasta tener suficiente información, sin abusar.` (sin número)
- ❌ INCORRECTO: `Itera el checklist varias veces si hace falta.` (sin número)

### 6.5 Plantillas embebidas literalmente

Cuando un skill produce un fichero con estructura fija (`specification.md`, `analysis.md`, …), **MUST** embeber la plantilla literal del fichero a generar dentro del propio skill — el modelo copia y rellena, no improvisa.

**Patrón**:

````markdown
#### N.N.N Plantilla de salida

El subagente devuelve un fichero con esta estructura exacta:

```
## <Título>

**Campo:** <valor>

### <Sección>
- <bullet>
```
````

**MUST NOT** sustituir la plantilla por una descripción ("la salida tiene un título, un campo y una sección de bullets") — el modelo entonces inventa formato.

### 6.6 Checklists con bucle de auto-validación

Si un skill genera un artefacto, **MUST** incluir un checklist explícito de calidad al final de la fase de generación, y un bucle de auto-corrección con **LIMIT** numérico.

**Patrón**:

```markdown
#### N.N.N Checklist del subagente

- [ ] ¿<verificación 1>?
- [ ] ¿<verificación 2>?
…

El subagente **MUST NOT** devolver el artefacto si queda algún punto del checklist sin cumplir.

**LIMIT**: máximo 3 iteraciones de corrección. Si tras la 3ª siguen fallando ítems, documenta las inconsistencias residuales y avísalo al usuario.
```

### 6.7 STOP conditions explícitas

El `## Outline` **MUST** terminar con una subsección **STOP conditions** que enumere los casos en que el skill aborta antes de completar.

- ✅ CORRECTO:
  ```markdown
  **STOP conditions**:
  - Frontmatter de entrada inválido → **ERROR** y detente.
  - El fichero de salida ya existe → **STOP** y preguntar al usuario antes de sobrescribir.
  - El usuario no aprueba el borrador → **MUST NOT** guardar nada.
  ```

### 6.8 Subagentes: paralelos vs secuenciales

Si el skill lanza subagentes en paralelo (patrón usado en `sdd-analyst-system`, `sdd-designer`, `sdd-eval`, …):

- **MUST** indicar el número exacto (`**REQUIRED**: exactamente N subagentes`).
- **MUST** decir explícitamente que se lancen en **una única respuesta** con N invocaciones a `Agent`.
- **MUST NOT** usar `run_in_background` si necesitas los resultados para una fase posterior.
- **MUST** indicar que los subagentes en paralelo **MUST NOT** usar `AskUserQuestion` (el agente principal sí puede, antes y después); sus dudas se registran en un bloque parseable (p.ej. `=== DUDAS ===`) que el agente principal lleva al usuario.

Si los subagentes **pueden necesitar preguntar al usuario**, **MUST** lanzarse secuencialmente (uno detrás de otro): dos preguntas concurrentes no son aceptables.

### 6.9 Overrides para testing (Apéndice A)

Los action-skills que escriben ficheros **RECOMENDADO**: añadir un Apéndice A con flags `--in=`, `--out=`, `--root=` para poder ejecutarlos en un sandbox alternativo sin tocar el árbol real. Patrón estándar:

```markdown
## Apéndice A — Override de rutas (para testing)

- `--in=<ruta>` — fichero de entrada explícito. Desactiva la auto-detección.
- `--out=<ruta>` — fichero de salida explícito.
- `--root=<ruta>` — raíz alternativa para resolver rutas relativas.

En uso normal no se especifican.
```

### 6.10 Contrato de respuesta de los subagentes

Cuando un action-skill lanza subagentes y necesita interpretar su resultado, el contrato de respuesta **MUST** quedar definido en el propio skill:

1. **Estados enumerados con tokens literales**: la respuesta del subagente empieza por un estado de una lista cerrada (p.ej. `DONE` / `BLOCKED`, `PASS {id}` / `FAIL {id}`, `OK-No hay problemas`). **MUST** escribir los tokens exactos en el skill — el orquestador compara por literal, no por intención.
   - ✅ CORRECTO: `Si no encuentra problemas responde exactamente: **OK-No hay problemas**`
   - ❌ INCORRECTO: `Si todo está bien, que lo indique de alguna forma` (el orquestador no puede comparar "de alguna forma")
2. **Marcadores parseables**: si el orquestador extrae bloques de la respuesta, los delimitadores van **literales** en el skill (p.ej. `=== FILE: x ===` … `=== END FILE ===`, `BEGIN:----` … `END:----`). **MUST NOT** variar los marcadores entre el ejemplo y el texto.
3. **Contexto mínimo de vuelta**: el subagente devuelve solo lo que el orquestador necesita (estado + resumen corto, o `escrito: <ruta>` por fichero). **MUST NOT** pegar contenido completo que ya está en disco — gasta el contexto del orquestador y arriesga divergencia.
4. **Modo subagente**: si el propio skill puede ejecutarse **dentro de un subagente** (lo invocan otros skills vía `Agent`), **MUST** definir el comportamiento sin usuario: ante un bloqueo, **devolver el estado de bloqueo como resultado final** en vez de `AskUserQuestion`/esperar — el orquestador padre decide.

---

## 7. Lo que un skill NUNCA debe hacer

**MUST NOT**:

- **MUST NOT** lanzar otros skills tú mismo si el flujo del proyecto dice que el usuario decide cuándo. Indica al usuario el comando exacto a ejecutar (`/sdd-foo …`) y **STOP**.
- **MUST NOT** suponer que el usuario ha leído el SKILL.md. El skill se dirige al **modelo**, no al humano.
- **MUST NOT** repetir convenciones generales del proyecto que ya están en `CLAUDE.md`. Referénciate a ellas, no las copies.
- **MUST NOT** mezclar tipo knowledge y action en el mismo skill. Si necesitas ambos, sepáralos en dos skills (`k-tema` + `sdd-tema`).
- **MUST NOT** documentar contenido derivable del código (estructuras de clases, listas de subsistemas) sin un pointer "leer en tiempo real desde `src/main/java/...`". El skill se desactualiza si congelas el contenido.
- **MUST NOT** depender de información volátil (fechas, versiones, IDs de ticket) en el cuerpo del skill.

---

## 8. Quick Guidelines

- Cuerpo en español, palabras-clave en inglés (`MUST`, `MUST NOT`, `REQUIRED`, `CRITICAL`, `STOP`, `ERROR`, `LIMIT`).
- **Formato receta sobre prosa** (§6.1): todo lo enumerable va como lista numerada, guiones o fases —no como párrafo—; la estructura concreta es libre.
- Frontmatter mínimo: `name` + `description` larga. `handoffs` si hay siguiente paso en el pipeline.
- Tipo knowledge (`k-*`): documenta convenciones y patrones. Tipo action (`sdd-*`, `code-*`): ejecuta proceso con fases numeradas.
- Estructura mínima de action-skill: `User Input` → `Outline` (con `STOP conditions`) → fases numeradas → `Quick Guidelines`.
- Plantillas literales embebidas para todo fichero de salida. **MUST NOT** describirlas en prosa.
- Ejemplos ✅/❌ inline con razón corta entre paréntesis para cada ❌.
- Límites numéricos duros (máx N preguntas, máx N iteraciones, exactamente N subagentes) en vez de adjetivos vagos.
- Contrato de subagentes (§6.10): tokens de estado literales, marcadores parseables exactos, contexto mínimo de vuelta, y modo subagente (devolver bloqueos, no esperar) si otros skills lo invocan vía `Agent`.
- **CRITICAL**: cada línea cuesta tokens. Si quitarla no cambia la salida del modelo, sobra.

---

## 9. Checklist final del skill

Aplica este checklist a cualquier `SKILL.md` que escribas o revises. **LIMIT**: máximo 3 iteraciones de corrección.

### 9.1 Frontmatter

- [ ] ¿Tiene `name`? ¿Coincide con el nombre de la carpeta contenedora?
- [ ] ¿Tiene `description` de 1-3 oraciones que diga qué hace, qué entrada espera y qué salida produce?
- [ ] Si es un action-skill con siguiente paso claro: ¿tiene `handoffs`?

### 9.2 Estructura

- [ ] ¿El H1 coincide con `name:`?
- [ ] ¿Hay una frase de entrada que diga qué rol asume el modelo y qué transforma en qué?
- [ ] Si es action-skill: ¿tiene `## User Input` con bloque `$ARGUMENTS`?
- [ ] Si es action-skill: ¿tiene `## Outline` con pasos numerados y `**STOP conditions**`?
- [ ] Si es action-skill: ¿hay al menos una sección de detalle por fase del Outline?
- [ ] ¿Tiene `## Quick Guidelines` al final del cuerpo (bullets escaneables, orientativo 5-10), antes de checklists finales y apéndices?
- [ ] Si escribe ficheros: ¿tiene `## Apéndice A — Override de rutas` con `--in=`/`--out=`/`--root=`?

### 9.3 Estilo

- [ ] ¿El cuerpo está en español?
- [ ] ¿Las palabras-clave imperativas (`MUST`, `MUST NOT`, `REQUIRED`, `CRITICAL`, `STOP`, `ERROR`, `LIMIT`) están en inglés y se usan solo en instrucciones realmente bloqueantes?
- [ ] ¿Las instrucciones están en imperativo de segunda persona ("lee", "valida", "lanza")?
- [ ] ¿El contenido enumerable está en formato receta —lista numerada, guiones o fases— y no como prosa (§6.1)? ¿No queda ningún párrafo de más de ~3 líneas reescribible como pasos/reglas?

### 9.4 Contenido

- [ ] ¿Cada formato rígido (plantilla, ID, frontmatter) lleva ejemplos ✅/❌ inline con razón corta para cada ❌?
- [ ] ¿Las cardinalidades están expresadas como `**LIMIT**: N` numérico explícito, no como adjetivos vagos?
- [ ] ¿Las plantillas de fichero de salida están embebidas **literalmente** dentro del skill?
- [ ] Si genera artefactos: ¿hay un checklist explícito y un bucle de auto-corrección con `**LIMIT**: máximo 3 iteraciones`?
- [ ] Si lanza subagentes en paralelo: ¿está el número exacto, la instrucción de "una única respuesta con N invocaciones a `Agent`", la prohibición de `run_in_background` y la prohibición de `AskUserQuestion` en los subagentes?
- [ ] Si lanza subagentes: ¿el contrato de respuesta está definido (§6.10) — tokens de estado literales con ejemplos ✅/❌, marcadores parseables exactos, contexto mínimo de vuelta?
- [ ] Si el skill puede ejecutarse dentro de un subagente: ¿define el modo subagente (§6.10.4) — devolver el bloqueo como resultado en vez de esperar al usuario?
- [ ] Si tiene `handoffs`: ¿cada `prompt` incluye la ruta del artefacto (o el placeholder `{carpeta-iniciativa}`)?

### 9.5 Higiene

- [ ] ¿No repite teoría general que el modelo ya sabe (qué es JPA, qué es git, qué es EARS)?
- [ ] ¿No repite convenciones ya documentadas en `CLAUDE.md` (las referencia, no las copia)?
- [ ] ¿No mezcla tipo knowledge y tipo action en el mismo fichero?
- [ ] ¿No depende de información volátil (fechas, versiones, IDs)?
- [ ] ¿Si quito cualquier línea, cambiaría la salida del modelo? Si la respuesta para alguna es **no**, esa línea sobra.

---

## Apéndice A — Override de rutas (para testing)

Para revisar este skill sobre un fichero alternativo sin tocar el árbol real:

- `--in=<ruta>` — fichero `SKILL.md` a revisar/refactorizar explícito.
- `--out=<ruta>` — destino alternativo si se reescribe el skill.
- `--root=<ruta>` — raíz alternativa a `.claude/skills/`.

En uso normal no se especifican.

---

## Apéndice B — Plantilla copy-paste para un action-skill nuevo

Copia este bloque, sustituye los `<placeholders>` y bórralos. Cumple por construcción el checklist §9.

````markdown
---
name: <nombre-skill>
description: <Una a tres oraciones: qué hace, qué entrada espera, qué salida produce, en qué pipeline encaja.>
handoffs:
  - label: <Acción siguiente legible>
    agent: <nombre del skill siguiente>
    prompt: <prompt sugerido>
---

# <nombre-skill>

<Frase de entrada: rol que asume el modelo + qué transforma en qué + posición en el pipeline.>

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). <Explicación de qué argumentos espera.>

---

## Outline

1. **<Verbo>** … (Fase 0)
2. **<Verbo>** … (Fase 1)
3. **<Verbo>** … (Fase 2)
4. **<Verbo>** … (Fase 3)

**STOP conditions**:

- <Caso bloqueante 1> → **ERROR** y detente.
- <Caso bloqueante 2> → **STOP** y pregunta al usuario.
- <Caso bloqueante 3> → **MUST NOT** continuar sin aprobación.

---

## 1. Entrada y salida

### 1.1 Entrada
<Fichero(s) que lee.>

### 1.2 Salida
<Fichero(s) que escribe.>

### 1.3 Estructura de carpetas
<Diagrama ASCII.>

---

## 2. Principios

### 2.1 <Principio 1>
### 2.2 <Principio 2>

---

## 3. Fase 0 — <Nombre>
<Pasos detallados.>

## 4. Fase 1 — <Nombre>
<Pasos detallados.>

## 5. Fase N — <Nombre>
<Pasos detallados.>

---

## Quick Guidelines

- <Principio 1 condensado.>
- <Principio 2 condensado.>
- <…>

---

## Apéndice A — Override de rutas (para testing)

- `--in=<ruta>` — …
- `--out=<ruta>` — …
- `--root=<ruta>` — …
````
