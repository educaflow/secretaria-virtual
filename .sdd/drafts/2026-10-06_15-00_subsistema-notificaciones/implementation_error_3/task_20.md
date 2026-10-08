---
type: implementation-task
template: system
---

# Tarea 20 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Controlador CorreoController

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/controller/CorreoController.java` | Crear | k-sistemas (controladores.md) | Reenvío del correo (movido de `correos/controller`) |

### Paso 8 (extracto verbatim del design.md: `CorreoController` y `SmsController`)

### Paso 8 — Controladores y ayudante de respuesta

**`CorreoController`** y **`SmsController`**: se mueven a `com.educaflow.subsystem.notificaciones.controller` con la misma lógica (`reenviar` y `validateReenviar` vía `ModelServiceFactory.resolve(Correo.class|Sms.class)`, `allowPropertiesReenviar`, aviso «El reenvío del correo/SMS se ha puesto en marcha.»). Cambian paquete e imports y una cosa más: como el form está abierto en popup (D3), tras el aviso `reenviar` responde `actionResponse.setReload(true)`, que recarga el form del popup (ESC-039/040/050/074: «recarga el formulario»), y **deja de** responder `setSignal("refresh-tab")`: el listado de la pestaña de debajo lo refresca `remote-refreshTab-action`, que va la última en el `btnReenviar` de `Main@` de cada canal (Paso 10). Implementan U-notificaciones-todas-011/031 y U-notificaciones-centro-005/011.

**`CorreoController`** (`com.educaflow.subsystem.notificaciones.controller`, Crear; ídem **`SmsController`** con `Sms`/`SmsService` y el aviso «El reenvío del SMS se ha puesto en marcha.»):

```java
@CallMethod @Transactional public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse);
//   resuelve CorreoService con ModelServiceFactory; reenviar(entidad, entidadOriginal) con allowPropertiesReenviar;
//   setNotify con el aviso del canal («El reenvío del correo se ha puesto en marcha.»); setReload(true);
//   sin setSignal("refresh-tab") (el listado lo refresca remote-refreshTab-action, D3).

/* header «Acciones de Validaciones» */
@CallMethod public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse);
//   delega en validateReenviar del servicio del canal (CorreoService) y responde sus mensajes.
```

Verificación (al cerrar el bloque 1–9): compila; `./gradlew -q test --tests '*architecture*'` (C9, C15, C28).

### Frontera de confianza — AllowProperties por acción (extracto verbatim)

### `CorreoServiceImpl.reenviar` (invocado desde `CorreoController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Correo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Reenviar` de `entity-Notificacion.md` (ninguna).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | — | **NO** | El reenvío actúa sobre `entidadOriginal` (BD); el estado lo cambia `enviar()` (R-Notificacion-003). |

### Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

#### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-014 | VAL-Notificacion-001 | `NotificacionCanalServiceImpl.validateReenviar`; expuesta por `CorreoController/SmsController.validateReenviar` (`Main@{Correo,Sms}-Remote-validateReenviar-action`) |

#### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-005 | RUI-notificaciones-centro-formulario-correo-004 | `CorreoController.reenviar` → `setNotify` |
| U-notificaciones-todas-011 | RUI-notificaciones-todas-formulario-correo-009 | `CorreoController.reenviar` → `setNotify` |

### Notas del design.md (`## Notas y supuestos`)

  - Los controladores de reenvío siguen siendo dos (`CorreoController`, `SmsController`) porque su aviso es distinto por canal y las guías piden moverlos sin cambios; el código común de los servicios sí se extrae (D1).
- **Refresco del listado tras el alta** (D3, resuelto tras la depuración de T-001): el alta se abre en popup **sobre** el popup de la elección de canal, así que el refresco de cierre de popup de Axelor iría a la elección (ya cerrada) y no al listado, y el respaldo `__onPopupReload` tampoco se instala porque hay un popup abierto. Por eso el `btnSave` de `Main@Correo-form`/`Main@Sms-form` lleva entre `save` y `close` la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`, `setSignal("refresh-tab")`, que en `axelor-front` va a `getActiveTabId(-1)`, la pestaña ignorando los popups). Un canal nuevo la reutiliza tal cual.
- **Reenvío con el form en popup** (D3, resuelto al diseñar): `refresh-tab` despacha `tab:refresh` a la pestaña activa (el listado de debajo), no al popup, mientras que `setReload(true)` recarga el form del popup; por eso `CorreoController.reenviar`/`SmsController.reenviar` responden `setReload(true)` y el `btnReenviar` de `Main@` termina en `remote-refreshTab-action`, la misma acción que el alta (nota anterior); el de `Centro@` delega en él.
