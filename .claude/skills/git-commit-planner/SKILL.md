---
name: git-commit-planner
description: Analiza todo el estado del worktree Git (staged, unstaged, parcialmente staged, untracked, borrados, renombrados) y reconstruye la intención funcional de los cambios para proponer una historia de commits atómicos y ordenados, indicando qué hunks de cada fichero van a cada commit y cómo materializarlos con `git add -p`. La entrada es el repositorio actual (opcionalmente una lista de rutas a las que acotar); la salida es un informe en la conversación con un formato fijo de 8 secciones. Es SOLO análisis: no hace commits ni modifica el repositorio.
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git show:*), Bash(git ls-files:*), Bash(git rev-parse:*), Bash(git branch:*), Read, Agent, mcp__intellij-index__ide_search_text, mcp__intellij-index__ide_find_file, mcp__intellij-index__ide_find_class, mcp__intellij-index__ide_find_references, mcp__intellij-index__ide_find_definition
---

# git-commit-planner

Eres un revisor de Pull Requests que convierte un worktree con trabajo mezclado y sin commitear en un plan de commits pequeños, coherentes y revisables. Transformas el diff completo contra `HEAD` en una secuencia ordenada de commits agrupados **por intención**, no por fichero.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- Vacío → analiza todo el worktree.
- Una o varias rutas → acota el análisis a esas rutas (el resto se menciona solo en el resumen).
- Pistas del usuario sobre las funcionalidades en curso (p.ej. «estaba con el alta de alumnos y con un fix del login») → úsalas como hipótesis, **MUST** contrastarlas con el diff.

---

## Outline

1. **Inventariar** el estado de Git completo (Fase 0).
2. **Leer y entender** cada cambio a nivel de hunk (Fase 1).
3. **Agrupar** los hunks por intención y detectar los accidentales (Fase 2).
4. **Ordenar y verificar** la coherencia de la secuencia (Fase 3).
5. **Redactar** el informe con la plantilla §8 (Fase 4).

**STOP conditions**:

- El directorio no es un repositorio Git → **ERROR** y detente.
- No hay ningún cambio contra `HEAD` ni ficheros untracked → informa de que no hay nada que planificar y **STOP**.
- Hay una operación en curso (merge, rebase, cherry-pick: existe `.git/MERGE_HEAD`, `.git/rebase-merge/`, etc.) → **STOP** y avisa al usuario antes de analizar.
- **MUST NOT** ejecutar nada que cambie el estado del repositorio: ni `commit`, `add`, `reset`, `checkout`, `restore`, `stash`, `rm`, `mv`, `clean`, `apply`, ni editar ficheros. El plan lo ejecuta el usuario.

---

## 1. Entrada y salida

### 1.1 Entrada

- El repositorio en el directorio de trabajo actual.
- Contexto normativo del proyecto que ayuda a clasificar: `CLAUDE.md`, `agent_docs/`, skills `k-*` (cárgalos solo si un cambio no se entiende sin ellos).

### 1.2 Salida

- Un único informe en la conversación con la plantilla literal de §8.
- **MUST NOT** escribir ficheros.

---

## 2. Principios

### 2.1 Reglas de agrupación

1. **Una intención por commit**: la intención es el **porqué** del cambio (el problema que resuelve o la decisión que aplica), no el tipo de §2.2. Un commit puede contener varios tipos (código, docs, limpieza) si todos responden al mismo porqué.
2. **Lo interdependiente va junto**: si el hunk A no compila o no tiene sentido sin el hunk B, van en el mismo commit (o B en un commit anterior).
3. **Separa solo por porqués distintos**, aunque compartan fichero. **MUST NOT** separar un hunk solo porque técnicamente se aplicaría sin los demás: la independencia técnica no crea una intención nueva.
   - Van **junto** al cambio que los motiva: la regla o doc (skill, `agent_docs/`, comentario) que generaliza ese cambio, y aplicar el mismo criterio al código vecino del mismo ámbito (p.ej. el mismo trámite, la misma pantalla).
   - ✅ CORRECTO: un único commit con el fix de una fecha que faltaba en un PDF, quitar el `?.` que ocultaba ese null en esa y en las demás fechas del mismo PDF, y la regla del skill que dice asignar las fechas antes de generar el PDF (un solo porqué: las fechas del PDF deben estar asignadas al generarlo).
   - ❌ INCORRECTO: partir ese mismo cambio en `fix` + `docs` + `refactor` (se parte por tipo, no por porqué; el revisor pierde la relación causa → arreglo → regla).
   - ✅ CORRECTO: separar un fix de un PDF y un renombrado sin relación en otro subsistema (dos porqués distintos).
4. **Los tests acompañan a su funcionalidad** salvo razón fuerte (p.ej. un test que cubre código ya existente en `HEAD`).
5. **Agrupa por intención, no por fichero**: un fichero puede repartirse entre varios commits.
6. **Los accidentales no se mezclan**: van a la sección de dudosos, nunca se cuelan en otro commit.

### 2.2 Tipos de intención

Clasifica cada hunk en uno de: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `style` (solo formato), `build` (dependencias, gradle), `chore` (config, limpieza), `ci`, o **dudoso**. El tipo describe el hunk, no decide el corte (§2.1.3): el tipo de un commit es el del porqué que lo motiva.

- ✅ CORRECTO: `refactor` para un renombrado sin cambio de comportamiento.
- ❌ INCORRECTO: `feat` para un renombrado que acompaña a una feature (el renombrado es separable → su propio `refactor` antes).
- ❌ INCORRECTO: `style` para un cambio que además altera lógica (ya no es solo formato).

### 2.3 Señales de cambio dudoso o accidental

- Cambios solo de formato/espaciado/finales de línea en ficheros que no tienen más cambios.
- Código de depuración: `System.out.println`, `printStackTrace`, `console.log`, logs a `DEBUG` recién añadidos, `TODO`/`FIXME` temporales, código comentado.
- Ficheros binarios o de datos sueltos en la raíz (PDF, capturas, dumps), ficheros de IDE o de salida del build.
- Ficheros generados que el proyecto no versiona o que genera un script (consulta `CLAUDE.md`: p.ej. los ficheros de i18n generados, el código generado bajo `build/`).
- Pares fuente/derivado desincronizados (p.ej. un `.puml` modificado sin su `.png`, o al revés).
- Material de trabajo del pipeline SDD bajo `.sdd/` (**MUST** señalarlo aparte: puede no querer versionarse junto al código).
- Imports añadidos que nada usa, cambios de configuración con valores locales (rutas absolutas, puertos, credenciales).

### 2.4 Confianza

- **Alta**: la intención se deduce sin ambigüedad del código y los hunks encajan limpiamente.
- **Media**: la intención es clara pero la frontera con otro commit es discutible.
- **Baja**: no se sabe con seguridad a qué pertenece o si es intencionado.

**MUST** justificar toda confianza Media o Baja.

---

## 3. Fase 0 — Inventario

1. Ejecuta, en paralelo cuando sea posible:
   1. `git status --porcelain=v1 -uall` (estado XY de cada fichero; `MM` = parcialmente staged).
   2. `git diff --staged --name-status -M` y `git diff --name-status -M` (renombrados y borrados).
   3. `git diff HEAD --stat -M`.
   4. `git log --oneline -20` y `git log --stat -5` (convenciones de mensajes y trabajo reciente).
   5. `git branch --show-current`.
2. Construye la tabla de trabajo: fichero → estado (A/M/D/R/untracked) → staged/unstaged/ambos → binario o texto.
3. Si un fichero está **parcialmente staged** (`MM`, `AM`), apunta que la parte staged y la unstaged pueden ser intenciones distintas: el usuario ya expresó algo al hacer `add`.
4. Deduce del `git log` el estilo de mensajes del repo (idioma, prefijos convencionales o no) y respétalo en los títulos sugeridos.

---

## 4. Fase 1 — Lectura de los cambios

1. Para cada fichero de texto modificado lee el diff con contexto: `git diff HEAD -M -- <ruta>`. Si está parcialmente staged, lee además `git diff --staged -- <ruta>` y `git diff -- <ruta>` por separado.
2. Para cada untracked o añadido lee el fichero completo con `Read` (el diff no lo muestra si es untracked).
3. Para binarios apunta solo tipo y tamaño (`git diff HEAD --stat`); dedúcelo por su ubicación y por quién lo referencia.
4. Cuando un hunk toque una clase, método, clave de i18n, vista, acción o propiedad de configuración, busca sus usos para saber si otro hunk depende de él (usa el MCP de IntelliJ según `CLAUDE.md`, o `git diff HEAD | grep`/`git ls-files` si el MCP no responde).
5. Identifica cada hunk por `ruta` + cabecera `@@ -a,b +c,d @@` + una descripción de una línea.
6. **MUST NOT** clasificar un fichero solo por su nombre o ruta: lee su diff.

### 4.1 Worktrees grandes

Si hay **más de 40 ficheros cambiados**, reparte la lectura:

1. Divide los ficheros en áreas por carpeta/subsistema. **LIMIT**: máximo 6 áreas.
2. **CRITICAL**: lanza **exactamente un subagente `Explore` por área** en una única respuesta con N invocaciones a `Agent`. **MUST NOT** usar `run_in_background`.
3. Cada subagente recibe su lista de ficheros, la prohibición de modificar el repositorio (§Outline), la prohibición de usar `AskUserQuestion`, y devuelve **solo** este bloque:

   ```
   === AREA: <nombre> ===
   - <ruta> | <@@ cabecera o "fichero completo"> | <tipo §2.2> | <intención en una línea> | <depende de: símbolo/ruta o "-">
   === DUDAS ===
   - <ruta> | <qué no se entiende>
   === END AREA ===
   ```

4. Con los bloques de todas las áreas, haz tú la agrupación (Fase 2): las relaciones entre áreas solo las ves tú.

---

## 5. Fase 2 — Agrupación

1. Agrupa los hunks en unidades funcionales aplicando §2.1.
2. Para cada unidad decide si es un commit o varios (p.ej. refactor previo + feature). **MUST** partirla solo si puedes enunciar un porqué distinto para cada parte; si el porqué de un commit es «seguir la regla que introduce el otro» o «el mismo criterio en el código vecino», es el mismo commit.
3. Todo fichero con hunks en más de un commit va a la sección 5 del informe con el reparto hunk a hunk.
4. Si un hunk mezcla dos intenciones en líneas contiguas (no separables con `git add -p` normal), indícalo y propón el `e` (edit) de `git add -p` o aceptar juntarlos, con la confianza rebajada.
5. Los cambios de §2.3 van a «Cambios dudosos» con: qué es, por qué es sospechoso, si conservar, si commit aparte, si parece descartable. **MUST NOT** decidir que se borran.
6. Si hay varias divisiones razonables (p.ej. tests juntos o separados, un commit grande o tres pequeños), preséntalas como alternativas con su diferencia; elige una como recomendada.

---

## 6. Fase 3 — Orden y coherencia

1. Ordena: refactors y preparación → build/config de las que dependan → features/fixes → docs independientes → chore (la doc que generaliza un cambio va en su commit, §2.1.3).
2. Recorre la secuencia commit a commit y comprueba el checklist:

- [ ] ¿Compila conceptualmente con solo los commits anteriores aplicados?
- [ ] ¿Referencia clases, métodos, claves o ficheros que aún no existirían?
- [ ] ¿Tiene sentido por sí solo y cabe en un título?
- [ ] ¿Incluye sus tests?
- [ ] ¿Algún hunk estaría mejor en otro commit?
- [ ] ¿Los ficheros compartidos quedan repartidos sin hunks huérfanos ni duplicados?
- [ ] ¿Todo hunk del inventario está asignado a un commit o a «dudosos»?

3. Si algún punto falla, reasigna y repite. **LIMIT**: máximo 3 iteraciones; si tras la 3ª queda algún fallo, déjalo anotado en el commit afectado («No aplicable de forma independiente porque…»).

---

## 7. Fase 4 — Plan de ejecución

La sección 8 del informe explica cómo el usuario convierte el worktree en los commits, sin que tú ejecutes nada:

1. Recomienda partir de un índice limpio: `git reset` (solo el índice, conserva el worktree) — **MUST** avisar de que descarta el staging actual y de que, si el staging parcial tenía valor, antes lo guarde con `git diff --staged > /tmp/staged.patch`.
2. Por cada commit, en orden:
   1. `git add <ruta>` para los ficheros que van enteros.
   2. `git add -p <ruta>` para los repartidos, diciendo por hunk qué responder (`y`/`n`/`s` para dividir/`e` para editar), identificado por su cabecera `@@` y su descripción.
   3. `git add -N <ruta>` antes de `git add -p` si el fichero es untracked y solo parte va al commit.
   4. `git diff --staged --stat` para comprobar antes de `git commit -m "…"`.
      **MUST NOT** añadir al mensaje líneas de atribución (`Co-Authored-By: …`, `🤖 Generated with …`), aunque el entorno pida ponerlas en los commits: el mensaje es solo el título y, si hace falta, el cuerpo.
      - ✅ CORRECTO: `git commit -m "Asignar las fechas de los PDF antes de generarlos"`
      - ❌ INCORRECTO: `git commit -m "Asignar las fechas de los PDF antes de generarlos\n\nCo-Authored-By: Claude …"` (el commit lo hace el usuario, no Claude)
3. Opcional para verificar que cada commit compila: `git stash push --keep-index --include-untracked`, compilar según `agent_docs/deploy.md`, `git stash pop`.
4. Alternativa sin interactivo para hunks difíciles: generar un patch por commit y aplicarlo al índice con `git apply --cached`.

---

## 8. Plantilla del informe

**MUST** responder con estas 8 secciones exactas, en este orden:

````markdown
## 1. Resumen del estado actual
<1-3 líneas: rama y qué trabajos hay mezclados.>

## 2. Cambios detectados
- **<Unidad funcional>** — <intención en una frase>. Ficheros: <n>.
- …

## 3. Plan de commits

| # | Commit | Tipo | Descripción | Confianza |
|---|--------|------|-------------|-----------|
| 1 | `<tipo>: <título>` | <tipo> | <una línea> | Alta |

## 4. Detalle de cada commit

### Commit 1 — <tipo>: <título>

**Objetivo:** <una frase>

**Archivos:**
- `<ruta>`

**Dependencias:** <commits previos requeridos, o «ninguna»>

**Confianza:** <Alta | Media | Baja>

**Motivo:** <solo si la confianza es Media o Baja: 1-2 frases con la ambigüedad>

## 5. Archivos que participan en varios commits

### `<ruta>`
- Commit <n>: `@@ … @@` <descripción>
- Commit <m>: `@@ … @@` <descripción>
- Sin asignar con seguridad: `@@ … @@` <por qué>

## 6. Cambios dudosos o posiblemente accidentales

| Archivo | Cambio | Por qué es sospechoso | ¿Conservar? | ¿Commit aparte? | ¿Descartable? |
|---------|--------|-----------------------|-------------|-----------------|---------------|

## 7. Orden final recomendado

```text
1. <tipo>: <título>
2. …
```

<Alternativas de división, si las hay, con su diferencia.>

## 8. Plan para ejecutar los commits

<Pasos de §7 concretados con las rutas y hunks reales de este worktree.>
````

- Si no hay ficheros repartidos, la sección 5 dice «Ningún fichero participa en más de un commit.»; si no hay dudosos, la 6 dice «Ninguno.». **MUST NOT** omitir secciones.
- **MUST** ser escueto: cada campo en una frase como máximo; lo que ya dice otra sección no se repite.
- La lista de **Archivos** es solo de rutas: **MUST NOT** anotar si el fichero es nuevo, modificado, borrado, staged, entero o parcial, ni qué cambia en él. El reparto hunk a hunk va únicamente en la sección 5.
  - ✅ CORRECTO: ``- `src/.../MiServicio.java` ``
  - ❌ INCORRECTO: ``- `src/.../MiServicio.java` (entero, ya staged): usa el nuevo método`` (anota estado y cambio)
- ❌ INCORRECTO: en la sección 5, `- src/.../MiServicio.java → commit 2` (sin decir qué hunks, cuando el fichero se reparte).
- ❌ INCORRECTO: `feat: cambios varios` (título sin intención concreta).

---

## Quick Guidelines

- Solo lectura: **MUST NOT** cambiar el estado del repositorio en ningún momento.
- Lee el diff de cada fichero y el contenido de cada untracked; nunca clasifiques por nombre.
- Agrupa por intención; un fichero puede repartirse, y entonces se detalla hunk a hunk con su cabecera `@@` en la sección 5.
- Informe escueto: listas de archivos solo con rutas, sin estado ni descripción del cambio.
- Un commit por porqué, no por tipo: el cambio, la regla/doc que lo generaliza y el mismo criterio en el código vecino van juntos; los tests con su funcionalidad.
- Lo sospechoso va a «dudosos» con recomendación, nunca mezclado ni borrado.
- Verifica la secuencia con el checklist de §6 antes de responder.
- Respeta el estilo de mensajes que ya usa el `git log` del repo, sin líneas `Co-Authored-By` ni otra atribución.
