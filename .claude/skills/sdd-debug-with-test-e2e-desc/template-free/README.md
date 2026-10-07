# Plantilla de testing E2E de una feature libre («free») — guía e índice

Esta carpeta de plantillas define **todo lo específico de tomar la descripción de tests E2E de una feature libre (`test-e2e-desc.md`) y ejecutarla contra la aplicación real, corrigiendo el código hasta que pase**. El skill `sdd-debug-with-test-e2e-desc` aporta solo el **flujo** y es **agnóstico**: lo lee todo de aquí.

Una feature libre puede tocar cualquier parte de la aplicación (`base`, varios sistemas, configuración, vistas, jobs…), así que esta plantilla no presupone qué pantalla se pilota ni qué clase se corrige: **lo dicta cada test y el `design.md`**. Lo que sí fija es el rigor del bucle y que hay **tests manuales** (`Manual: sí`) que el motor salta y entrega a una persona.

Este `README.md` es **el único fichero que el skill conoce por nombre**. **Lo leen los tres subagentes** (tabla de §2).

Además, el **motor** lee de aquí la sección **«Gestión de la app»** (§4).

> **Contrato fijo (lo garantiza el skill):** la entrada es `implementation/test-e2e-desc.md` y la salida vive en `{iniciativa}/test-e2e-desc/` y en el árbol del proyecto. Todo lo demás lo define esta plantilla.

---

## 1. Ficheros de esta carpeta de plantillas

| Fichero | Qué define | Quién lo lee |
|---|---|---|
| `README.md` | **Esta guía/índice**: roles, entrada/salida, tests manuales, gestión de la app, contexto. | Los tres subagentes y el **motor** («Gestión de la app»). **MUST NOT** copiarse al output. |
| `decomposition.md` | **Cómo descomponer `test-e2e-desc.md`** en un fichero autocontenido por test, la cabecera común, las plantillas de `t-NNN-<slug>.desc.md` y del índice (con `- [ ]` o `- [-]` según `Manual`), y el checklist. | **Descomponedor**. |
| `execution.md` | **Cómo ejecutar un test**: skill de pilotaje, URL base, Given/When/Then, errores recurrentes (esperas, caché SPA, editores, crons), equivalencia semántica de mensajes, qué recoger al fallar, formato `SUCCESS`/`FAIL`. | **Ejecutor**. |
| `correction.md` | **Cómo corregir el código**: localizar la causa (IntelliJ MCP + log), decidir y cargar los skills según **qué parte** de la aplicación falla, delegar en `developer-code-implementer`, qué **MUST NOT** tocarse, cuándo un test es `MANUAL`, y los tokens `CORREGIDO`/`MANUAL`/`DESIGN-ERROR`/`BLOQUEADO`. | **Corrector**. |

---

## 2. Tareas de los tres subagentes

> **Común a los tres:** **MUST** leer este `README.md` y seguir desde él a los ficheros que su tarea necesite. **MUST NOT** copiar bloques explicativos al output. **MUST NOT** usar `AskUserQuestion`. **MUST NOT** pegar en la respuesta contenido que ya está en disco.

| Rol | Qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **descomponedor** (§2.1) | **Escribe los ficheros de test** | la ruta del `test-e2e-desc.md` | `decomposition.md` | `{iniciativa}/test-e2e-desc/` con índice y un fichero por test |
| **ejecutor** (§2.2) | **Pilota un test** | la ruta de **un** `t-NNN-<slug>.desc.md` | `execution.md` | `SUCCESS {id}` o `FAIL {id}` + `=== FALLO ===` |
| **corrector** (§2.3) | **Corrige el código** ante un `FAIL` | la ruta del test + `=== FALLO ===` + extracto del log | `correction.md` | código corregido + `CORREGIDO`/`MANUAL`/`DESIGN-ERROR`/`BLOQUEADO` |

### 2.1 descomponedor

**Tarea:** leer `test-e2e-desc.md` íntegro y escribir `test-e2e-desc/`: un `t-NNN-<slug>.desc.md` **autocontenido** por test y el índice `tests-e2e-desc.md`, con `- [-]` para los `Manual: sí` y `- [ ]` para los demás.

- **Lee de esta plantilla:** `decomposition.md`.
- **MUST NOT** ejecutar tests ni tocar código. **MUST NOT** terminar sin pasar el checklist.

### 2.2 ejecutor

**Tarea:** ejecutar **un** test contra `http://localhost:8080` (ya levantada) y reportar.

- **Lee de esta plantilla:** `execution.md`.
- **Premisa:** la app YA está levantada. **MUST NOT** arrancarla, pararla ni recompilarla.
- Nunca recibe un test `Manual: sí` (el motor lo salta); si lo recibiera, aplica la red de seguridad de `execution.md` §1.

### 2.3 corrector

**Tarea:** ante un `FAIL`, analizar la causa, cargar los skills que **esa parte** de la aplicación requiera y corregir el código.

- **Lee de esta plantilla:** `correction.md`.
- **MUST** cargar los skills con `Skill` antes de corregir; delegar el código en `developer-code-implementer`.
- Qué **MUST NOT** tocar y cuándo devolver `DESIGN-ERROR` / `BLOQUEADO` / `MANUAL`: `correction.md` §4.

---

## 3. Estructura de entrada y de salida

### 3.1 Entrada — `implementation/test-e2e-desc.md`

```
# Tests E2E
<intro>

## Estado inicial de la base de datos        ← cabecera común (datos + credenciales)
- <datos previos a todos los tests>
**Usuarios de acceso** … | Login | Contraseña | Rol / Tipo | Centro |

## T-001 — <nombre>                           ← un bloque por test
**Origen ESC:** …  **Verifica:** …  **Pantalla principal:** …  **Tipo:** …  **Manual:** no | sí — <motivo>
### Precondiciones / ### Pasos / ### Resultado esperado
```

La cabecera común se copia **verbatim** en cada fichero de test.

### 3.2 Salida — `test-e2e-desc/` y el árbol del proyecto

```
.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen}/
└── test-e2e-desc/
    ├── tests-e2e-desc.md         ← índice (type: test-e2e-index)
    ├── t-001-<slug>.desc.md …    ← un fichero autocontenido por test (type: test-e2e)
    ├── app.log                   ← lo escribe el MOTOR
    └── error_design.log          ← solo si hubo DESIGN-ERROR (lo escribe el MOTOR)

src/main/…  src/main/resources/…   ← correcciones (donde el design.md puso el código)
```

**Tests manuales en esta familia.** Los tres estados del índice:

| Marca | Quién la escribe | Qué hace el motor |
|---|---|---|
| `- [ ]` | pendiente: el descomponedor (`decomposition.md` §3) | lo ejecuta |
| `- [-]` | `Manual: sí` del diseño: el descomponedor (`decomposition.md` §3); o el motor ante el token `MANUAL` del corrector (`correction.md` §4) | lo salta y lo reporta como `MANUAL`; solo se entrega a una persona con `--manuales` |
| `- [x]` | pasó contra la app real: el motor | lo descarta al reinvocar |

---

## 4. Gestión de la app — la ejecuta el MOTOR

> **CRITICAL:** esta sección la ejecuta el **motor**, no un subagente. La app es un recurso compartido por los ejecutores secuenciales y **MUST** sobrevivir entre subagentes: la arranca el orquestador como **tarea tracked en segundo plano**.

### 4.1 Comprobar si está levantada (idempotencia)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
```

`200` = levantada. **MUST NOT** arrancar una segunda instancia.

### 4.2 Limpiar el puerto 8080 de verdad (antes de arrancar)

**CRITICAL:** una instancia previa en 8080 hace que el connector falle el bind **en silencio**. Limpia y confirma, **excluyendo** IntelliJ y similares:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
pkill -f 'TomcatRunner|GradleWrapperMain.*run' 2>/dev/null
ss -ltn | grep ':8080' || echo "8080 libre"
```

El contenedor Docker `secretaria-virtual-dev` **NO** ocupa el 8080 del host: **MUST NOT** tocarlo.

### 4.3 Arrancar (tracked, en segundo plano)

Usa **siempre** `./run.sh` (ver `CLAUDE.md`).

- `Bash` con `run_in_background: true` y `dangerouslyDisableSandbox: true`, redirigiendo el log:
  ```bash
  exec ./run.sh > .sdd/drafts/{iniciativa}/test-e2e-desc/app.log 2>&1
  ```
  **MUST NOT** añadir `&`/`nohup`.
- **Sondea** hasta `200` (`clean build` + bind tardan minutos). **LIMIT**: 420 s por ventana, máximo 3 ventanas.
- Si el log muestra `BUILD FAILED`, el build no pasó (tests unitarios, ArchUnit, vistas, CPD, CRAP…): es un fallo del ciclo de corrección, no de la app.
- Si agotadas las ventanas no da `200` → el motor hace **STOP** y `AskUserQuestion`.

### 4.4 Parar

Siempre **por puerto**:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
```

### 4.5 Rearrancar tras una corrección

Tras un `CORREGIDO`, el motor **para** (§4.4) y **arranca de nuevo** (§4.2 + §4.3).

---

## 5. Contexto del proyecto

- Ningún rol carga skills «por defecto»: el **ejecutor** carga el de pilotaje que indique `execution.md`; el **corrector** decide en runtime según la parte de la aplicación que falla (`correction.md` §2).
- El `design.md` de la iniciativa (`**Ubicación del código:**`, «Piezas») es la referencia del corrector sobre **dónde** está el código de la feature y **qué** se diseñó: lo lee, nunca lo escribe.
- `CLAUDE.md` del proyecto y los de cada carpeta (`base/`, `subsystem/tramitador/`, `subsystem/expedientes/`, `tramites/util/`) cuando el fallo cae ahí.
- Si la feature roza expedientes, el código de `tramites/` **sí** es referencia legítima (sigue `k-tipo-expediente`); si no, no hace falta mirarlo.
