# Plantilla de testing E2E — guía e índice

Esta carpeta de plantillas define **todo lo específico de tomar una descripción de tests E2E (`test-e2e-desc.md`) y ejecutarla contra la aplicación real, corrigiendo el código hasta que pase**. El skill `sdd-debug-with-test-e2e-desc` aporta solo el **flujo** (localizar la iniciativa, descomponer, arrancar la app, y por cada test el bucle ejecutar→corregir→reejecutar, reportar) y es **agnóstico**: no sabe qué es un test, cómo se descompone, cómo se ejecuta ni cómo se corrige. Lo lee todo de aquí.

Este `README.md` es **el único fichero que el skill conoce por nombre**. **Lo leen los tres subagentes** y, según su rol, cada uno tiene una tarea distinta:

- **descomponedor** — **lee `test-e2e-desc.md` y escribe la carpeta `test-e2e-desc/`** (un fichero autocontenido por test + índice) (§2.1).
- **ejecutor** — coge **un** test y lo **pilota en el navegador** contra la app real; devuelve `SUCCESS`/`FAIL` (§2.2).
- **corrector** — ante un `FAIL`, **analiza el problema, carga los skills necesarios y corrige el código Java** (§2.3).

Además, el **motor** (no un subagente) lee de aquí la sección **«Gestión de la app»** (§4): los comandos para arrancar/parar/sondear la aplicación bajo prueba.

A través de este README cada subagente descubre y lee **solo los ficheros de esta carpeta que su tarea necesita** (§2). **MUST NOT** copiar ningún bloque explicativo de esta plantilla a los ficheros de salida.

> **Contrato fijo (lo garantiza el skill, no lo cambia esta plantilla):** la entrada es `implementation/test-e2e-desc.md` (siempre bajo `implementation/`; `/sdd-designer` lo produce en `design/` y `/sdd-implementer` lo propaga a `implementation/`) y la salida vive en `{iniciativa}/test-e2e-desc/` y en el árbol del proyecto (`src/main/...`). Todo lo demás (cómo se descompone, cómo se ejecuta cada test, cómo se corrige el código, cómo se gestiona la app) lo define esta plantilla.

---

## 1. Ficheros de esta carpeta de plantillas

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: contrato fijo, estructura de entrada/salida, gestión de la app y principios comunes a todos los roles. | Todos los subagentes (es el contrato que el skill nombra) y el **motor** (la sección «Gestión de la app»). **MUST NOT** copiarse al output. |
| `decomposition.md` | **Cómo descomponer `test-e2e-desc.md`** en un fichero por test: qué cabecera común copiar en cada uno para que sea autocontenido, las plantillas exactas de `t-NNN-<slug>.desc.md` y del índice `tests-e2e-desc.md` (con checkbox por test), la numeración y el checklist. | El **descomponedor** (§2.1). |
| `execution.md` | **Cómo ejecutar un test** contra la app: qué skill cargar para pilotar el navegador, la URL base, cómo interpretar Given/When/Then, los **errores recurrentes a evitar** (esperas, caché de la SPA, editores), el criterio de **equivalencia semántica** de los mensajes, qué recoger de la UI al fallar y el formato de salida `SUCCESS`/`FAIL`. | El **ejecutor** (§2.2). |
| `correction.md` | **Cómo corregir el código** para que un test pase: cómo localizar la causa (MCP de IntelliJ + log de la app), cómo decidir y cargar los skills de dominio necesarios, cómo delegar el código en `developer-code-implementer`, qué **MUST NOT** tocarse, y el formato de salida `CORREGIDO`/`BLOQUEADO`. | El **corrector** (§2.3). |

---

## 2. Tareas de los tres subagentes

El skill `sdd-debug-with-test-e2e-desc` lanza estos tres roles. Todos reciben este `README.md`; cada uno recibe además **su** entrada propia (la ruta de su fichero de test, o el `test-e2e-desc.md` en el caso del descomponedor) y **lee un subconjunto distinto de los ficheros de esta carpeta**.

> **Común a los tres:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** copiar ningún bloque explicativo de la plantilla a los ficheros de salida. **MUST NOT** usar `AskUserQuestion` (reportan el bloqueo con el token de su rol; el motor lleva la decisión al usuario). **MUST NOT** pegar en la respuesta contenido que ya está en disco.

| Rol | Qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **descomponedor** (§2.1) | **Lee `test-e2e-desc.md` y escribe los ficheros de test** | la ruta del `test-e2e-desc.md` | `decomposition.md` | `{iniciativa}/test-e2e-desc/` con el índice y un fichero por test |
| **ejecutor** (§2.2) | **Pilota un test** en el navegador | la ruta de **un** `t-NNN-<slug>.desc.md` | `execution.md` | `SUCCESS {id}` o `FAIL {id}` + `=== FALLO ===` (no toca código) |
| **corrector** (§2.3) | **Corrige el código** ante un `FAIL` | la ruta del test + el problema + el log de la app | `correction.md` | el árbol corregido en `src/main/...` + `CORREGIDO`/`BLOQUEADO` |

### 2.1 descomponedor — lee `test-e2e-desc.md` y escribe los ficheros de test

**Tarea:** leer el `test-e2e-desc.md` íntegro y **escribir la carpeta `{iniciativa}/test-e2e-desc/`**: un fichero `t-NNN-<slug>.desc.md` **autocontenido** por test (cabecera común + bloque del test) y el índice `tests-e2e-desc.md` con un checkbox sin marcar por test.

- **Lee de esta plantilla:** `decomposition.md` (cómo trocear, qué cabecera copiar, las plantillas exactas, la numeración y el checklist).
- **Entrada propia:** la ruta del `test-e2e-desc.md`.
- **MUST NOT** ejecutar tests ni materializar código. **MUST NOT** dar la descomposición por terminada sin pasar el checklist de `decomposition.md`.

### 2.2 ejecutor — pilota un test en el navegador

**Tarea:** dado **un** `t-NNN-<slug>.desc.md`, **ejecutarlo contra la app real** (`http://localhost:8080`, ya levantada por el motor) y reportar `SUCCESS`/`FAIL`.

- **Lee de esta plantilla:** `execution.md` (qué skill cargar, la URL base, cómo interpretar los pasos, los errores recurrentes a evitar, la equivalencia semántica de mensajes, qué recoger al fallar y el formato de salida).
- **Entrada propia:** la ruta de **su** `t-NNN-<slug>.desc.md` (autocontenido: trae estado inicial + credenciales + el test).
- **Premisa:** la app YA está levantada (la arrancó el motor). **MUST NOT** arrancarla, pararla ni recompilarla. **MUST NOT** modificar ficheros del proyecto.

### 2.3 corrector — corrige el código

**Tarea:** dado un test que falla, su problema observado y el log de la app, **analizar la causa, cargar los skills necesarios y corregir el código Java** para que el test pase.

- **Lee de esta plantilla:** `correction.md` (cómo localizar la causa, cómo decidir y cargar los skills de dominio, cómo delegar en `developer-code-implementer`, qué no tocar y el formato de salida).
- **Entrada propia:** la ruta del `t-NNN-<slug>.desc.md`, el bloque `=== FALLO ===` del ejecutor y el extracto del log de la app.
- **OBLIGATORIO:** decide qué skills de dominio necesitas y **cárgalos con `Skill` antes de corregir**; delega el código en `developer-code-implementer` si `correction.md` lo indica.
- **MUST NOT** tocar el contrato: ni `test-e2e-desc.md`, ni los ficheros de `test-e2e-desc/`, ni el XML/contrato del diseño. Si la corrección lo exigiera → `BLOQUEADO`.

---

## 3. Estructura de entrada y de salida

### 3.1 Entrada — `implementation/test-e2e-desc.md`

El fichero de entrada se lee **siempre** de `implementation/test-e2e-desc.md` (lo propaga `/sdd-implementer` desde `design/`). Es un único fichero con esta forma:

```
# Tests E2E
<intro>

## Estado inicial de la base de datos        ← cabecera común (datos maestros + credenciales)
- <datos maestros previos a todos los tests>
**Usuarios de acceso** … | Login | Contraseña | Rol | Centro |

## T-001 — <nombre>                           ← un bloque por test
**Origen ESC:** …  **Verifica:** …  **Pantalla principal:** …  **Tipo:** …
### Precondiciones / ### Pasos / ### Resultado esperado

## T-002 — <nombre>
…
```

La sección `## Estado inicial de la base de datos` (incluida la tabla de credenciales) es la **cabecera común** que el descomponedor copia en cada fichero de test (§2.5 del SKILL).

### 3.2 Salida — `test-e2e-desc/` y el árbol del proyecto

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── test-e2e-desc/
    ├── tests-e2e-desc.md         ← índice con un checkbox por test (type: test-e2e-index)
    ├── t-001-<slug>.desc.md … t-NNN-<slug>.desc.md  ← un fichero autocontenido por test (type: test-e2e)
    └── app.log                   ← log de la app (lo escribe el MOTOR; los subagentes lo ignoran)

src/main/java/com/educaflow/…     ← correcciones de código Java (las escribe el corrector vía developer-code-implementer)
```

El índice es la fuente del **progreso reanudable**: un test que pasa se marca `[x]`.

---

## 4. Gestión de la app — la ejecuta el MOTOR

> **CRITICAL:** esta sección la ejecuta el **motor** (`sdd-debug-with-test-e2e-desc`), **no** un subagente. La app es un recurso compartido por todos los ejecutores secuenciales y **MUST** sobrevivir entre subagentes; por eso la arranca el orquestador como **tarea tracked en segundo plano** (un proceso lanzado por un subagente muere al cerrarse su contexto). Ver memorias `run-sh-arranque-orquestador-tracked` y `run-sh-canonico-y-sandbox`.

### 4.1 Comprobar si está levantada (idempotencia)

La app se considera levantada si responde `200`:

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
```

**MUST NOT** arrancar una segunda instancia si ya responde `200`.

### 4.2 Limpiar el puerto 8080 de verdad (antes de arrancar)

**CRITICAL:** si una instancia previa sigue en 8080, el connector Tomcat **falla el bind en silencio** (el proceso existe y loga "Running at…", pero `curl`/`ss` nunca ven el `200`). Antes de arrancar, mata de verdad lo que escuche en 8080 y confirma que queda libre:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
# Si persiste, mata por cmdline los runners de Gradle/Tomcat de la app,
# EXCLUYENDO IntelliJ y similares (idea|intellij|jetbrains|fsnotifier|mcp):
pkill -f 'TomcatRunner|GradleWrapperMain.*run' 2>/dev/null
ss -ltn | grep ':8080' || echo "8080 libre"
```

El contenedor Docker `secretaria-virtual-dev` **NO** ocupa el 8080 del host: el 8080 del host es siempre una instancia de `./run.sh`. **MUST NOT** parar ni matar procesos del contenedor Docker.

### 4.3 Arrancar (tracked, en segundo plano)

Usa **siempre** `./run.sh` (hace `./gradlew clean build` y arranca en el 8080 con la config correcta). **MUST NOT** invocar `gradlew run` a mano ni añadir `--debug-jvm` (suspende la JVM esperando un depurador; la app nunca da `200`).

- Lánzalo con `Bash`, `run_in_background: true` y `dangerouslyDisableSandbox: true` (`run.sh` escribe en `~/.gradle`, fuera del sandbox), redirigiendo el log al fichero del motor:
  ```bash
  exec ./run.sh > .sdd/drafts/{iniciativa}/test-e2e-desc/app.log 2>&1
  ```
  **MUST NOT** añadir `&`/`nohup`: con `run_in_background` el harness mantiene la tarea viva entre subagentes; un `&`/`nohup` o un arranque desde subagente muere al cerrarse el job.
- **Sondea** hasta `200` con margen amplio (el `clean build` + el bind del connector pueden tardar varios minutos): repite `curl` con `Monitor`/reintentos, **LIMIT** de sondeo ~420 s por ventana, varias ventanas si hace falta. Verifica también `lsof -p <pid> -a -iTCP -sTCP:LISTEN` o `ss -ltn | grep :8080`.
- Si tras el sondeo no da `200` → el motor hace **STOP** y `AskUserQuestion`.

### 4.4 Parar

Siempre **por puerto**, nunca por handle de proceso (los contextos del subagente y del motor son distintos):

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
```

### 4.5 Rearrancar tras una corrección

Tras un `CORREGIDO` del corrector, el motor **para** (§4.4) y **arranca de nuevo** (§4.2 + §4.3): el arranque recompila el fix con `clean build`. Si el log muestra `BUILD FAILED`, es un error de compilación (no de test): trátalo como fallo del ciclo y vuelve a corregir.

---

## 5. Contexto del proyecto

- Ningún rol carga skills "por defecto": el **ejecutor** carga el skill de pilotaje del navegador que indique `execution.md`; el **corrector** decide y carga **en runtime** los skills de dominio que el fallo requiera (`correction.md`).
- Referencia de arquitectura, tipos de usuario y subsistemas: el `CLAUDE.md` del proyecto y los de cada carpeta. Los subagentes los consultan cuando el contrato lo pide.
- **MUST NOT** usar como referencia el código de `expedientes`/`tiposexpedientes`/`tramites` (otra arquitectura).
