# Plantilla de testing E2E de un tipo de expediente — guía e índice

Esta carpeta de plantillas define **todo lo específico de tomar la descripción de tests E2E de un trámite (`test-e2e-desc.md`) y ejecutarla contra la aplicación real, corrigiendo el código hasta que pase**. El skill `sdd-debug-with-test-e2e-desc` aporta solo el **flujo** (localizar la iniciativa, descomponer, arrancar la app, y por cada test el bucle ejecutar→corregir→reejecutar, reportar) y es **agnóstico**: no sabe qué es un test, cómo se descompone, cómo se ejecuta ni cómo se corrige. Lo lee todo de aquí.

Es la plantilla **hermana** de `template-system/`: mismo motor, mismos tres roles y misma gestión de la app; cambia **qué** se prueba (una máquina de estados recorrida por perfiles, no un CRUD de un sistema) y **qué** se corrige (las clases de la carpeta de versión, no los servicios de un subsistema).

Este `README.md` es **el único fichero que el skill conoce por nombre**. **Lo leen los tres subagentes** y, según su rol, cada uno tiene una tarea distinta:

- **descomponedor** — **lee `test-e2e-desc.md` y escribe la carpeta `test-e2e-desc/`** (un fichero autocontenido por test + índice) (§3.1).
- **ejecutor** — coge **un** test y lo **pilota en el navegador** contra la app real; devuelve `SUCCESS`/`FAIL` (§3.2).
- **corrector** — ante un `FAIL`, **analiza el problema, carga los skills necesarios y corrige el código Java/Kotlin** del tipo de expediente (§3.3).

Además, el **motor** (no un subagente) lee de aquí la sección **«Gestión de la app»** (§5): los comandos para arrancar/parar/sondear la aplicación bajo prueba.

A través de este README cada subagente descubre y lee **solo los ficheros de esta carpeta que su tarea necesita** (§3). **MUST NOT** copiar ningún bloque explicativo de esta plantilla a los ficheros de salida.

> **Contrato fijo (lo garantiza el skill, no lo cambia esta plantilla):** la entrada es `implementation/test-e2e-desc.md` (siempre bajo `implementation/`; `/sdd-designer` lo produce en `design/` y `/sdd-implementer` lo propaga a `implementation/`) y la salida vive en `{iniciativa}/test-e2e-desc/` y en el árbol del proyecto (`src/main/...`). Todo lo demás lo define esta plantilla.

---

## 0. REGLA DE GENERALIDAD — léela antes que nada

**CRITICAL.** Esta plantilla describe **el patrón**, nunca un trámite concreto.

- **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, evento, campo, perfil ni documento reales. Se usan los placeholders de §0.1.
- **MUST NOT** escribirse ninguna regla que solo valga para un número fijo de fases, estados, eventos, documentos PDF o perfiles. El patrón **MUST** funcionar con **1, 2, 3 o N fases**; con **0, 1 o N** documentos PDF; con firma **en cliente, en servidor, en ambas o en ninguna**.
- Todo ejemplo **MUST** ir encerrado en un bloque que empiece por `> **Ejemplo** (ilustrativo, NO normativo):`, con nombres inventados.

### 0.1 Placeholders

| Placeholder | Significado |
|---|---|
| `<tramite>` / `<Code>` | carpeta del trámite (`snake_case`) / su `code` (`UpperCamelCase`) |
| `<vN>` / `<VN>` | carpeta de versión (`v1`, `v2`…) / la versión en UpperCamel |
| `<Entidad>` | entidad JPA y code del tipo de expediente (`<Code><VN>`) |
| `<FASE>` / `<fase>` | `name` de una fase / su carpeta y paquete (en minúsculas) |
| `<ESTADO>` / `<EVENTO>` | `name` de un estado / de un evento (`UPPER_SNAKE_CASE`) |
| `<PERFIL>` | valor del enum `Profile`: `CREADOR`, `RESPONSABLE`, `SECRETARIO`, `DIRECTOR`, `AUDITOR` |

---

## 1. Ficheros de esta carpeta de plantillas

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: contrato fijo, reparto por rol, estructura de entrada/salida, **la UI real del subsistema de expedientes** (§4), la **gestión de la app** (§5) y los principios comunes. | Los tres subagentes (es el contrato que el skill nombra) y el **motor** (§5). **MUST NOT** copiarse al output. |
| `decomposition.md` | **Cómo descomponer `test-e2e-desc.md`** en un fichero por test: qué es la cabecera común en un trámite (`Actores` + `Datos de demo`), los **siete campos** de cabecera de cada test, las plantillas exactas de `t-NNN-<slug>.desc.md` y del índice —con sus tres estados `[ ]`/`[x]`/`[-]`—, la numeración y el checklist. | El **descomponedor** (§3.1). |
| `execution.md` | **Cómo ejecutar un test** contra la app: cómo se crea un expediente, **por qué bandeja se abre cada perfil**, cómo se dispara un evento, dónde se lee la fase/estado de llegada, dónde salen los mensajes de validación, los errores recurrentes, el criterio de equivalencia semántica y el formato `SUCCESS`/`FAIL`. | El **ejecutor** (§3.2). |
| `correction.md` | **Cómo corregir el código** del tipo de expediente: el mapa síntoma → clase responsable, qué skills cargar, qué **MUST NOT** tocarse (los XML materializados por el diseño → `DESIGN-ERROR`) y el formato `CORREGIDO`/`BLOQUEADO`/`DESIGN-ERROR`. | El **corrector** (§3.3). |

---

## 2. Estructura de entrada y de salida

### 2.1 Entrada — `implementation/test-e2e-desc.md`

Un único fichero, producido por `/sdd-designer` con la plantilla de expediente, con esta forma:

```
# Tests E2E — <nombre visible del trámite> (`<Entidad>`)
<intro>

## Actores                                  ← tabla de credenciales (Login | Contraseña | Tipo/Cargo | Centro | Perfil | Vía)
## Datos de demo                            ← estado previo + un juego de datos válido por fase
## Cobertura de transiciones                ← tabla de control transición ↔ T-NNN

## T-001 — <nombre>                          ← un bloque por test
**Origen ESC:** …
**Perfil:** …
**Desde:** …
**Evento:** …
**Hasta:** …
**Tipo:** happy | error | solo-lectura
**Manual:** no | sí — <motivo>       ← si es `sí`, el test NO es automatizable (§4.4)
- **Given** … / **When** … / **Then** … / **And** …

## T-002 — <nombre>
…
```

- La **cabecera común** que el descomponedor copia en cada fichero de test son las secciones `## Actores` y `## Datos de demo` (§3.1 y `decomposition.md` §2). Sin ellas el ejecutor no puede hacer login ni saber qué datos teclear.
- `## Cobertura de transiciones` es una tabla de **control del diseño**: **MUST NOT** copiarse a los ficheros de test (es ruido para el ejecutor). El descomponedor solo la usa para comprobar que todo `T-NNN` que referencia existe.
- **MUST NOT** asumir que el fichero trae `## Precondiciones` / `## Pasos` / `## Resultado esperado`: en un trámite el test es un bloque de bullets `Given` / `When` / `Then` / `And` (o `Dado` / `Cuando` / `Entonces` / `Y`).

### 2.2 Salida — `test-e2e-desc/` y el árbol del proyecto

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── test-e2e-desc/
    ├── tests-e2e-desc.md        ← índice con un checkbox por test (type: test-e2e-index)
    ├── t-001-<slug>.desc.md … t-NNN-<slug>.desc.md   ← un fichero autocontenido por test (type: test-e2e)
    └── app.log                  ← log de la app (lo escribe el MOTOR; los subagentes lo ignoran)

src/main/java/com/educaflow/tramites/<tramite>/…/<vN>/   ← correcciones de código del tipo de expediente
```

El índice es la fuente del **progreso reanudable**: un test que pasa se marca `[x]`. Tiene **tres** estados: `[ ]` pendiente, `[x]` pasado y `[-]` **no automatizable** (§4.4), que el motor salta por defecto.

---

## 3. Tareas de los tres subagentes

> **Común a los tres:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** copiar ningún bloque explicativo de la plantilla a los ficheros de salida. **MUST NOT** usar `AskUserQuestion` (reportan el bloqueo con el token de su rol; el motor lleva la decisión al usuario). **MUST NOT** pegar en la respuesta contenido que ya está en disco.

| Rol | Qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **descomponedor** (§3.1) | **Lee `test-e2e-desc.md` y escribe los ficheros de test** | la ruta del `test-e2e-desc.md` | `decomposition.md` | `{iniciativa}/test-e2e-desc/` con el índice y un fichero por test |
| **ejecutor** (§3.2) | **Pilota un test** en el navegador | la ruta de **un** `t-NNN-<slug>.desc.md` | `execution.md` (+ contexto §4) | `SUCCESS {id}` o `FAIL {id}` + `=== FALLO ===` (no toca código) |
| **corrector** (§3.3) | **Corrige el código** ante un `FAIL` | la ruta del test + el problema + el log de la app | `correction.md` (+ contexto §4) | el árbol corregido en `src/main/...` + `CORREGIDO`/`BLOQUEADO`/`DESIGN-ERROR` |

### 3.1 descomponedor — lee `test-e2e-desc.md` y escribe los ficheros de test

**Tarea:** leer el `test-e2e-desc.md` íntegro y **escribir la carpeta `{iniciativa}/test-e2e-desc/`**: un `t-NNN-<slug>.desc.md` **autocontenido** por test (cabecera común + bloque del test) y el índice `tests-e2e-desc.md` con un checkbox sin marcar por test.

- **Lee de esta plantilla:** `decomposition.md`.
- **MUST NOT** ejecutar tests ni materializar código. **MUST NOT** reescribir, resumir ni renumerar los tests: es un troceador, no un autor.
- **MUST NOT** dar la descomposición por terminada sin pasar el checklist de `decomposition.md`.

### 3.2 ejecutor — pilota un test en el navegador

**Tarea:** dado **un** `t-NNN-<slug>.desc.md`, **ejecutarlo contra la app real** (`http://localhost:8080`, ya levantada por el motor) y reportar `SUCCESS`/`FAIL`.

- **Lee de esta plantilla:** `execution.md` y el contexto de §4 (la UI del subsistema de expedientes).
- **Premisa:** la app YA está levantada (la arrancó el motor). **MUST NOT** arrancarla, pararla ni recompilarla. **MUST NOT** modificar ficheros del proyecto.
- **CRITICAL — el perfil no se elige, se entra por él**: la vista que ve el usuario depende de la **bandeja** por la que abre el expediente (§4.2). Abrir por la bandeja equivocada da la vista de otro perfil y el test falla por un motivo falso.

### 3.3 corrector — corrige el código

**Tarea:** dado un test que falla, su problema observado y el log de la app, **analizar la causa, cargar los skills necesarios y corregir el código** para que el test pase.

- **Lee de esta plantilla:** `correction.md` y el contexto de §4.
- **OBLIGATORIO:** decide qué skills de dominio necesitas y **cárgalos con `Skill` antes de corregir**; `k-tipo-expediente` es **siempre** uno de ellos.
- **MUST NOT** tocar el contrato: ni `test-e2e-desc.md`, ni los ficheros de `test-e2e-desc/`, ni los XML materializados por el diseño (`TipoExpedienteInstance.xml`, `domains.xml`, los `views.xml`, los `documentospdf/`). Si la corrección lo exigiera → `DESIGN-ERROR` (`correction.md` §5).

---

## 4. Contexto del proyecto — la UI del subsistema de expedientes

Lo leen el **ejecutor** y el **corrector**. Es lo que un trámite tiene y un sistema no.

**CRITICAL — a diferencia de `template-system/`, aquí el código real de `src/main/java/com/educaflow/tramites/` y de `src/main/java/com/educaflow/subsystem/expedientes/` SÍ es referencia legítima**: los trámites existentes siguen exactamente esta arquitectura. **MUST NOT**, en cambio, copiar sus nombres, estados ni campos: el test lo dicta su `.desc.md`.

### 4.1 Cómo se crea un expediente

Menú **«Expedientes» → «Trámites»**: un **árbol** de trámites disponibles agrupados por tipo de trámite. Pulsar el nodo del trámite (o su botón «Nuevo expediente») dispara `triggerInitialEvent` y **abre directamente el formulario del estado inicial**, con el expediente ya creado. No hay un botón «Nuevo» del grid ni un alta previa que guardar.

- Un trámite que el usuario **no ve en el árbol** es un problema de **permisos** (asignación de perfil por `tramiteCode`), no de la máquina de estados.

### 4.2 Cómo se abre un expediente ya creado — y por qué importa el perfil

El expediente se abre desde una de las **bandejas** del menú «Expedientes», y **cada bandeja fija el perfil con el que se pinta la vista** (contexto `_profile` de su `action-view`):

| Menú | Qué lista | Perfil con el que abre |
|---|---|---|
| «Expedientes Pendientes» | grid de expedientes abiertos | `CREADOR` |
| «Expedientes Esperando» | árbol de expedientes abiertos por trámite | `RESPONSABLE` |
| «Expedientes Cerrados» | expedientes con `abierto=false` | `RESPONSABLE` |

- **CRITICAL**: el runtime elige la vista `exp-<Code>-<FASE>-<ESTADO>-<PERFIL>-form` si existe para el perfil de la bandeja, y si no, la **genérica** `exp-<Code>-<FASE>-<ESTADO>-form` (solo lectura). Por eso «he entrado y está todo en solo lectura» casi siempre significa **bandeja equivocada**, no un fallo del código.
- **CRITICAL — ver los botones no es poder disparar el evento.** Lo que elige la vista es el perfil de la **bandeja**; los perfiles que tenga el usuario **no** intervienen ahí (el servidor solo comprueba que ese perfil lo use algún estado del tipo). El perfil **real** del usuario se comprueba al **disparar** el evento, y si no lo tiene la app responde un **error de acceso** y el expediente **no** transiciona.
  Consecuencias para el ejecutor: un usuario **sin** el perfil del estado que entre por la bandeja de ese perfil verá la vista completa **con** sus botones —eso es lo esperado, no un fallo—; y un usuario **con** el perfil que entre por otra bandeja verá la genérica de solo lectura.
- Si no existe **ninguna** de las dos vistas, la app lanza «No existe la vista en el expediente»: eso **sí** es un fallo real (falta el `<form state=…>` genérico).
- Un perfil distinto de esos tres (p.ej. `SECRETARIO`, `DIRECTOR`) no tiene bandeja propia: se llega por la pantalla que el propio trámite declare. Si el test lo exige y no hay por dónde entrar, es un fallo de diseño.

### 4.3 Qué se ve dentro de un expediente

- **Cabecera** (panel global «Información general», salvo `header="false"`): los campos **«Creado por»**, **«Fase»**, **«Estado»** y **«Fecha último estado»**, más el botón **«Ver el historial de estados»** (popup con una fila por estado recorrido: evento, fase, estado, fecha y sus registros de entrada/salida).
  - **REQUIRED — es aquí donde se comprueba a qué fase y estado llega el expediente.** Los textos son el `title` de la fase y el del estado, no sus `name` en `UPPER_SNAKE_CASE`.
- **Footer**: los botones del estado. **El `title` del botón es lo que se pulsa; su `name` es el `<EVENTO>` que dispara.** El `.desc.md` da los dos.
- **Mensajes de validación**: el footer pinta un **recuadro rojo** (`alert-danger`) con una lista de mensajes, cada uno con el título del campo en negrita seguido del texto. **MUST NOT** buscarlos como un toast ni como un diálogo modal.
- Los eventos `EXIT` y `DELETE` responden con `refresh-app`: **la aplicación entera se recarga** y se vuelve al inicio; no se navega a otra vista.

### 4.4 Qué NO se puede automatizar — los tests **manuales**

- **La firma en cliente con AutoFirma** exige un certificado en la máquina del usuario y una aplicación de escritorio: **MUST NOT** intentar pilotarla en el navegador, ni simularla, ni saltársela declarando `SUCCESS`.

**Un test así se marca, no se pelea con él.** El diseño lo declara con `**Manual:** sí — <motivo>` en la cabecera del test; el descomponedor lo escribe como `- [-]` en el índice (`decomposition.md` §3) y el motor **lo salta** y lo reporta aparte, en vez de atascar el resto de la depuración.

Qué hace cada rol cuando aun así se topa con uno (el diseño olvidó la marca):

- **ejecutor** — reporta `FAIL {T-NNN}` con el motivo **literal** «paso no automatizable (AutoFirma)» (`execution.md` §8). **MUST NOT** declarar `SUCCESS`.
- **corrector** — devuelve el token `MANUAL: {T-NNN} — {motivo}` (`correction.md` §5), **no** `BLOQUEADO`: no falta ningún recurso del entorno ni hay nada que corregir en el código.
  El motor lo pasa a `[-]` en el índice y **sigue con los demás tests** de forma autónoma.

**CRITICAL — `MANUAL` no es una vía de escape.** Solo vale para un paso que **ninguna** automatización puede ejecutar. Un locator que no se encuentra, un timing, una vista que no abre o un mensaje que no coincide **MUST** tratarse como lo que son: un fallo a corregir.

Cómo se ejecuta de verdad un test manual: el motor solo lo entrega a una persona si se le pide expresamente (flag del skill); ya persistido como regresión, se lanza con el tag `@manual` de la suite Playwright, que está **excluido por defecto** (también en CI/CD).

---

## 5. Gestión de la app — la ejecuta el MOTOR

> **CRITICAL:** esta sección la ejecuta el **motor** (`sdd-debug-with-test-e2e-desc`), **no** un subagente. La app es un recurso compartido por todos los ejecutores secuenciales y **MUST** sobrevivir entre subagentes; por eso la arranca el orquestador como **tarea tracked en segundo plano** (un proceso lanzado por un subagente muere al cerrarse su contexto).

### 5.1 Comprobar si está levantada (idempotencia)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
```

**MUST NOT** arrancar una segunda instancia si ya responde `200`.

### 5.2 Limpiar el puerto 8080 de verdad (antes de arrancar)

**CRITICAL:** si una instancia previa sigue en 8080, el connector Tomcat **falla el bind en silencio** (el proceso existe y loga "Running at…", pero `curl`/`ss` nunca ven el `200`):

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
# Si persiste, mata por cmdline los runners de Gradle/Tomcat de la app,
# EXCLUYENDO IntelliJ y similares (idea|intellij|jetbrains|fsnotifier|mcp):
pkill -f 'TomcatRunner|GradleWrapperMain.*run' 2>/dev/null
ss -ltn | grep ':8080' || echo "8080 libre"
```

El contenedor Docker `secretaria-virtual-dev` **NO** ocupa el 8080 del host. **MUST NOT** parar ni matar procesos del contenedor Docker.

### 5.3 Arrancar (tracked, en segundo plano)

Usa **siempre** `./run.sh`. **MUST NOT** invocar `gradlew run` a mano ni añadir `--debug-jvm`.

- Lánzalo con `Bash`, `run_in_background: true` y `dangerouslyDisableSandbox: true` (`run.sh` escribe en `~/.gradle`, fuera del sandbox):
  ```bash
  exec ./run.sh > .sdd/drafts/{iniciativa}/test-e2e-desc/app.log 2>&1
  ```
  **MUST NOT** añadir `&`/`nohup`.
- **Sondea** hasta `200` con margen amplio: repite el `curl` de §5.1 con `Monitor`/reintentos, **LIMIT** de sondeo ~420 s por ventana, varias ventanas si hace falta.
- **CRITICAL — el arranque de un tipo de expediente compila más cosas que el de un sistema**: `GenerateStatesTask` proyecta la clase `States` desde el `TipoExpedienteInstance.xml` y `viewProcessorTask` preprocesa los `views.xml` de cada fase. Un fallo de cualquiera de los dos aparece como `BUILD FAILED` en el log, **antes** de que la app llegue a levantar; trátalo como fallo de compilación, no como app caída.
- **CRITICAL — un `<object-views>` sin hijos tumba el arranque sin `BUILD FAILED`**: el síntoma es «The content of element 'object-views' is not complete» en el log y una app en pie **sin vistas, sin menús y sin data-init**. Si el log muestra eso, el fallo está en un `views.xml` vacío o con todos sus forms comentados.
- Si tras el sondeo no da `200` → el motor hace **STOP** y `AskUserQuestion`.

### 5.4 Parar

Siempre **por puerto**, nunca por handle de proceso:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
```

### 5.5 Rearrancar tras una corrección

Tras un `CORREGIDO`, el motor **para** (§5.4) y **arranca de nuevo** (§5.2 + §5.3): el arranque recompila el fix con `clean build` y regenera `States`. Si el log muestra `BUILD FAILED`, es un error de compilación (no de test): trátalo como fallo del ciclo y vuelve a corregir.