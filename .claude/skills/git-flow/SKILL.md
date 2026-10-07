---
name: git-flow
description: Ejecuta UNA operación del flujo de ramas del proyecto (feature → `develop` → `release` → `master`/`main`, según cuál tenga el repositorio) con las órdenes Git exactas de la asignatura, sin `git pull` y protegiendo las ramas oficiales. Cada rama de feature vive en su propio worktree hermano del repositorio (`<repo>-wt-<rama>`), que `iniciar` crea y `finalizar` borra. La entrada es la operación elegida (por argumento o preguntada con casillas si no se indica); la salida son los comandos Git ejecutados y un resumen final, o un **STOP** con instrucciones si hay conflicto o el repositorio no está en condiciones.
allowed-tools: Bash(git status:*), Bash(git fetch:*), Bash(git switch:*), Bash(git branch:*), Bash(git merge:*), Bash(git rebase:*), Bash(git push:*), Bash(git commit:*), Bash(git log:*), Bash(git rev-parse:*), Bash(git rev-list:*), Bash(git diff:*), Bash(git remote:*), Bash(git worktree:*), Bash(git -C:*), Bash(test:*), AskUserQuestion
---

# git-flow

Eres el operador Git del equipo: traduces una intención del usuario ("empiezo una tarea", "acabo la tarea", "subo las ramas") a la secuencia exacta de órdenes del flujo de ramas del proyecto, las ejecutas y te detienes ante cualquier conflicto o estado anómalo en vez de improvisar.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- Vacío → pregunta la operación (Fase 0).
- Una de las claves de la tabla §1.1 (`iniciar`, `actualizar`, `finalizar`, `release`, `master`/`main`, `bajar`, `subir`), opcionalmente seguida de sus parámetros:
  - ✅ CORRECTO: `iniciar login`
  - ✅ CORRECTO: `finalizar login "feat(#45): pantalla de login"`
  - ✅ CORRECTO: `finalizar "feat(#45): pantalla de login"` (la rama se deduce del worktree actual, §1.3)
  - ✅ CORRECTO: `subir`
  - ❌ INCORRECTO: `iniciar y finalizar` (**LIMIT**: exactamente una operación por invocación)
  - ❌ INCORRECTO: `pull` (no es una operación del flujo; `git pull` está prohibido)

---

## Outline

1. **Elegir** la operación: una y solo una (Fase 0).
2. **Localizar** el repositorio principal y el worktree de la feature, y comprobar que están limpios (Fase 1).
3. **Ejecutar** la receta de la operación elegida, paso a paso (Fase 2).
4. **Informar** del resultado: comandos ejecutados y dónde queda el usuario (Fase 3).

**STOP conditions**:

- `git status --porcelain` no está vacío en `MAIN` o en `WT` → **STOP**: hay cambios sin commitear; pide al usuario que haga commit o `git stash` y vuelva a invocar. **MUST NOT** hacer `stash`, `commit` ni `checkout -- .` por tu cuenta.
- `MAIN` no está en `develop` → **STOP**: el repositorio principal **MUST** vivir en `develop`; pide al usuario que vuelva a ella.
- En `origin` no existe ni `master` ni `main`, o existen las dos → **ERROR**: no se puede saber cuál es la rama de producción `PROD` (§1.3); informa y detente.
- La operación necesita una rama de feature y es `main`, `master`, `release` o `develop` → **ERROR** y detente.
- Una orden `git merge`/`git rebase` devuelve conflicto → **STOP** con las instrucciones de §6 para ese tipo de merge. **MUST NOT** resolver el conflicto ni abortar el merge por tu cuenta.
- `git merge --ff-only` falla sobre `release` o `PROD` → **STOP**: alguien ha commiteado directamente en una rama oficial; informa y no sigas.
- Un `git push` es rechazado en una rama distinta de `develop` → **STOP** e informa (en `develop` se aplica el rebase de §5.7.1).
- El usuario pide una operación fuera de la tabla §1.1 → **ERROR**: lista las operaciones válidas y detente.

---

## 1. Entrada y salida

### 1.1 Operaciones

**LIMIT**: exactamente una operación por invocación.

| Clave | Operación | Parámetros |
|-------|-----------|------------|
| `iniciar` | Iniciar tarea: crear rama de feature desde `develop`, su worktree `WT` y subirla | nombre de la rama |
| `actualizar` | Mergear `develop` en la rama de feature (en `WT`) | rama (deducible) |
| `finalizar` | Finalizar tarea: `--squash` en `develop`, push, borrar worktree y rama | rama (deducible), mensaje de commit |
| `release` | Mergear `develop` en `release` (`--ff-only`) y push | — |
| `master` (alias `main`) | Mergear `release` en `PROD` (`--ff-only`) y push | — |
| `bajar` | Bajar de GitHub `develop`, `release` y `PROD` | — |
| `subir` | Subir a GitHub `PROD`, `release`, `develop` y la rama de feature actual (si la hay) | — |

### 1.2 Salida

- Los comandos Git ejecutados, en el orden de la receta.
- Un resumen en la conversación (Fase 3). El skill **no escribe ficheros** del proyecto (solo crea/borra el directorio del worktree a través de `git worktree`).

### 1.3 Repositorio principal y worktrees

- **`MAIN`**: el worktree principal, primera línea de `git worktree list`. Ahí viven `develop`, `release` y `PROD`; **MUST** estar en `develop` siempre que no se esté ejecutando una receta.
- **`PROD`**: la rama de producción del repositorio, que es `master` **o** `main` según el proyecto (nunca las dos). La resuelve la Fase 1 mirando qué rama existe en `origin`; en las recetas de §5 escribe siempre `PROD` y sustitúyelo por el nombre real al ejecutar.
  - ✅ CORRECTO: existe `origin/main` y no `origin/master` → `PROD=main`
  - ✅ CORRECTO: existe `origin/master` y no `origin/main` → `PROD=master`
  - ❌ INCORRECTO: dar por hecho `master` sin comprobarlo (en los proyectos nuevos de GitHub la rama es `main`)
  - ❌ INCORRECTO: existen las dos → **ERROR**, no elijas una por tu cuenta
- **`WT`** de una feature `F`: carpeta hermana de `MAIN` llamada `<basename MAIN>-wt-<F>`. Ahí vive la rama `F` y es donde el usuario trabaja.
  - ✅ CORRECTO: `MAIN=/home/u/dev/secretaria-virtual`, `F=login` → `WT=/home/u/dev/secretaria-virtual-wt-login`
  - ❌ INCORRECTO: `/home/u/dev/secretaria-virtual/wt-login` (dentro del repo, no hermano)
  - ❌ INCORRECTO: `/home/u/dev/login` (sin el prefijo `<repo>-wt-`)
- Deducción de `F` cuando no viene por argumento: si el directorio actual (`git rev-parse --show-toplevel`) es un worktree `…-wt-<F>` → `F` es su rama (`git branch --show-current`). Si el directorio actual es `MAIN` → pregunta `F` mostrando `git worktree list`.
- **MUST** ejecutar todas las órdenes con `git -C <MAIN>` o `git -C <WT>` según la rama que tocan, para que el skill funcione igual desde cualquiera de los dos directorios.

### 1.4 Ramas protegidas

`main`, `master`, `release` y `develop` son **ramas oficiales** (`main` y `master` lo son las dos, sea cual sea `PROD`):

- **MUST NOT** crearlas, borrarlas, crearles worktree ni commitear directamente en ellas (el único commit permitido es el del `--squash` de `finalizar` sobre `develop`).
- **MUST NOT** aceptarlas como nombre de rama de feature.
- **MUST** actualizarlas desde `origin` (§2.2) antes de cualquier operación que las toque.

---

## 2. Principios

### 2.1 Nunca `git pull`

- **MUST NOT** ejecutar `git pull` en ninguna de sus formas (tampoco `git pull --ff-only`).
- Para actualizar una rama desde el remoto usa **siempre** `git fetch --prune` + `git merge --ff-only origin/<rama>` (§2.2).
- Motivo: así decides tú qué pasa cuando las historias han divergido, en vez de que lo decida la configuración `pull.*` de la máquina.

### 2.2 Receta "actualizar rama oficial desde GitHub"

Se usa en todas las operaciones. Para una rama `R` ∈ {`develop`, `release`, `PROD`}, siempre en `MAIN`:

```bash
git -C MAIN fetch --prune
git -C MAIN switch R          # si R no existe en local, switch la crea con track a origin/R
git -C MAIN merge --ff-only origin/R
```

- Si el `--ff-only` falla en `release` o `PROD` → **STOP** (hay commits locales que no deberían existir).
- Si el `--ff-only` falla en `develop` → solo es aceptable dentro de `subir` (§5.7.1), donde se resuelve con `rebase`. En cualquier otra operación → **STOP** y recomienda ejecutar `subir` primero.

### 2.3 Qué merge para qué salto

| Salto | Orden | Por qué |
|-------|-------|---------|
| feature → `develop` | `git merge --squash F` + `git commit` | los microcommits de la feature se funden en uno |
| `develop` → feature | `git merge develop` | los conflictos se resuelven en la feature, da igual crear commits |
| `develop` → `release`, `release` → `PROD`, `origin/R` → `R` | `git merge --ff-only` | nunca debe hacer falta un commit nuevo; si lo hace, algo va mal |
| `origin/develop` → `develop` con commits locales | `git rebase origin/develop` | reordena los commits locales detrás de los remotos |

### 2.4 Ejecución literal y visible

- **MUST** ejecutar los comandos uno a uno, en el orden de la receta, y parar en el primero que falle.
- **MUST NOT** encadenar toda la receta con `&&` en una sola llamada: si algo falla a mitad necesitas saber en qué paso.
- **MUST NOT** añadir opciones no listadas (`--force`, `--no-verify`, `-X theirs`, `worktree remove --force`, …).

---

## 3. Fase 0 — Elegir la operación

1. Si `$ARGUMENTS` trae una clave de §1.1 → úsala y salta a la Fase 1.
2. Si no, pregunta con `AskUserQuestion` en **dos pasos** (la herramienta admite 4 opciones por pregunta):
   - Pregunta 1 — "¿Qué quieres hacer?":
     - **Tarea** (iniciar / actualizar mi rama / finalizar)
     - **Promocionar** (`develop`→`release` / `release`→`PROD`, mostrando el nombre real `master` o `main`)
     - **Sincronizar con GitHub** (bajar / subir)
   - Pregunta 2 — las operaciones del grupo elegido, con la descripción de §1.1.
3. Pide los parámetros que falten, **LIMIT**: 1 pregunta por parámetro:
   - `iniciar` sin nombre → pregunta el nombre. Sugiere el formato `<usuario>_<nº issue>` (p.ej. `lorenzo_45`).
   - `actualizar`/`finalizar` sin rama y no deducible (§1.3) → pregunta cuál, listando `git worktree list`.
   - `finalizar` sin mensaje → pregunta el mensaje. Si la rama acaba en `_<número>`, propón `feat(#<número>): <descripción>`.

---

## 4. Fase 1 — Localizar y comprobar

```bash
git worktree list                 # 1ª línea = MAIN
git -C MAIN branch --show-current # MUST ser develop
git -C MAIN status --porcelain    # MUST estar vacío
git -C MAIN remote get-url origin # MUST existir
git -C MAIN fetch --prune
git -C MAIN branch -r --list origin/master origin/main   # MUST devolver exactamente una línea → PROD
```

1. Calcula `MAIN` y, si la operación tiene rama de feature `F`, `WT = $(dirname MAIN)/$(basename MAIN)-wt-F`.
2. `MAIN` fuera de `develop` o sucio → **STOP** (ver STOP conditions).
3. Resuelve `PROD` (§1.3): `origin/master` → `PROD=master`; `origin/main` → `PROD=main`; ninguna o las dos → **ERROR**. Este `fetch --prune` sustituye al primero de la receta de §5 (no lo repitas).
4. Si hay `WT` (`actualizar`, `finalizar`, `subir` desde un worktree):
   - `test -d WT` **MUST** existir y `git -C WT branch --show-current` **MUST** ser `F`; si no → **ERROR**: el worktree no sigue la convención §1.3, el usuario lo arregla a mano.
   - `git -C WT status --porcelain` **MUST** estar vacío; si no → **STOP**.
5. En `iniciar`, valida el nombre: **MUST NOT** ser una rama protegida, ni existir ya la rama (`git branch --list F` y `git branch -r --list origin/F` vacíos), ni existir la carpeta `WT` (`test -e WT` falso).

---

## 5. Fase 2 — Ejecutar la receta

### 5.1 `iniciar F`

```bash
git -C MAIN fetch --prune
git -C MAIN switch develop
git -C MAIN merge --ff-only origin/develop
git -C MAIN worktree add -b F WT develop    # crea la rama F desde develop y su worktree hermano
git -C WT push --set-upstream origin F
```

En el informe final indica al usuario que trabaje en `WT` (`cd WT`): `MAIN` se queda en `develop`.

### 5.2 `actualizar [F]` (develop → feature)

```bash
git -C MAIN fetch --prune
git -C MAIN merge --ff-only origin/develop
git -C WT merge develop
```

- Conflicto en `git merge develop` → **STOP** con §6.4.

### 5.3 `finalizar [F] "<mensaje>"`

```bash
git -C MAIN fetch --prune
git -C MAIN merge --ff-only origin/develop
git -C WT merge develop                     # los conflictos se resuelven en la feature, no en develop
git -C MAIN merge --squash F
git -C MAIN commit -am "<mensaje>"
git -C MAIN push
git -C MAIN push origin --delete F
git -C MAIN worktree remove WT              # antes que borrar la rama local: no se puede borrar una rama con worktree
git -C MAIN branch --delete --force F       # --force porque tras un --squash git no considera la rama mergeada
```

- Conflicto en `git merge develop` → **STOP** con §6.4 (y el usuario relanza `finalizar` al resolverlo).
- Conflicto en `git merge --squash` → **STOP** con §6.2. No debería ocurrir tras el paso anterior.
- `git push` rechazado (`fetch first`) → aplica §5.7.1 (rebase) y repite el `git push`.
- **MUST NOT** borrar el worktree ni la rama hasta que el `git push` de `develop` haya terminado bien.
- `worktree remove` falla por ficheros sin seguimiento → **STOP** y lista los ficheros; **MUST NOT** usar `--force`.
- Si el directorio actual era `WT`, al acabar ya no existe: dilo explícitamente en el informe y pide al usuario `cd MAIN`.

### 5.4 `release` (develop → release)

```bash
git -C MAIN fetch --prune
git -C MAIN merge --ff-only origin/develop
git -C MAIN switch release
git -C MAIN merge --ff-only origin/release
git -C MAIN merge --ff-only develop
git -C MAIN push
git -C MAIN switch develop
```

### 5.5 `master` (release → PROD)

```bash
git -C MAIN fetch --prune
git -C MAIN switch release
git -C MAIN merge --ff-only origin/release
git -C MAIN switch PROD
git -C MAIN merge --ff-only origin/PROD
git -C MAIN merge --ff-only release
git -C MAIN push
git -C MAIN switch develop
```

### 5.6 `bajar`

```bash
git -C MAIN fetch --prune
git -C MAIN merge --ff-only origin/develop
git -C MAIN switch release && git -C MAIN merge --ff-only origin/release
git -C MAIN switch PROD    && git -C MAIN merge --ff-only origin/PROD
git -C MAIN switch develop
```

(Aquí sí es aceptable un `&&` por rama: `switch` + `merge` de la misma rama son un solo paso lógico.)

### 5.7 `subir`

1. Para cada rama `R` de `PROD`, `release`, `develop`, en ese orden (así `MAIN` acaba en `develop`):

   ```bash
   git -C MAIN switch R
   git -C MAIN push
   ```
2. Si el directorio actual es un worktree `WT` de una feature `F`: `git -C WT push`.

#### 5.7.1 Push rechazado en `develop`

```bash
git -C MAIN fetch --prune
git -C MAIN rebase origin/develop
git -C MAIN push
```

- Conflicto en el `rebase` → **STOP** con §6.1.

#### 5.7.2 Push rechazado en `release`, `PROD` o `F`

- `release`/`PROD` → **STOP**: alguien ha subido commits a una rama oficial que no tienes; informa y recomienda `bajar`.
- `F` (feature) → **STOP**: la rama tiene commits remotos que no tienes (otro equipo, otro ordenador); informa y deja que el usuario decida.

---

## 6. Instrucciones ante conflicto (lo que le dices al usuario en el STOP)

**MUST** mostrar la salida de `git status` del directorio afectado (`MAIN` o `WT`) y, según la orden que falló, exactamente este bloque:

### 6.1 Conflicto en `git rebase origin/develop` (en `MAIN`)

```bash
# edita cada fichero marcado con <<<<<<< ======= >>>>>>>
git add <fichero>
git rebase --continue      # repetir si hay más commits en conflicto
git push
# para dejarlo como estaba: git rebase --abort
```

### 6.2 Conflicto en `git merge --squash F` (en `MAIN`)

```bash
# edita cada fichero en conflicto
git commit -am "<mensaje>"  # el commit que de todas formas iba a hacerse
git push
```

### 6.3 Conflicto en `git merge --ff-only`

Un fast-forward **nunca** produce conflicto. Si falla es porque la rama destino tiene commits propios → **STOP** y explica cuál (`git log origin/R..R --oneline`).

### 6.4 Conflicto en `git merge develop` (en `WT`)

```bash
# edita cada fichero en conflicto
git add <fichero>
git merge --continue
git push
# para dejarlo como estaba: git merge --abort
```

---

## 7. Fase 3 — Informar

Responde con:

1. La operación realizada y la lista de comandos ejecutados.
2. Dónde queda el usuario: `MAIN` en `develop`, y el `WT` creado (tras `iniciar`) o borrado (tras `finalizar`, con el aviso de `cd MAIN` si estaba dentro).
3. Si hubo **STOP**: el paso exacto en que se paró, el directorio (`MAIN`/`WT`), el bloque de §6 que aplica y qué operación relanzar después.

---

## Quick Guidelines

- **Una** operación por invocación; sin argumento, se pregunta en dos pasos con casillas.
- `PROD` es `master` o `main`, la que exista en `origin` (ninguna o las dos → **ERROR**); nunca des por hecho una de ellas.
- `MAIN` (1ª línea de `git worktree list`) vive siempre en `develop`; cada feature `F` vive en el worktree hermano `<repo>-wt-F`, que `iniciar` crea y `finalizar` borra. Toda orden va con `git -C MAIN` o `git -C WT`.
- Nunca `git pull`: siempre `git fetch --prune` + `git merge --ff-only origin/<rama>`.
- `main`/`master`/`release`/`develop` están protegidas: solo se tocan por las recetas de §5 y siempre tras actualizarlas desde `origin`.
- feature→`develop` con `--squash` + commit; `develop`→feature con `merge`; `develop`→`release`→`PROD` y `origin/R`→`R` con `--ff-only`; `origin/develop` con commits locales → `rebase`.
- Worktree sucio, `MAIN` fuera de `develop`, conflicto o push rechazado fuera de `develop` → **STOP** con las instrucciones literales de §6; **MUST NOT** resolver conflictos, hacer `stash`, `--force` ni abortar merges por tu cuenta.
- Comandos uno a uno, en orden, parando en el primero que falla.
