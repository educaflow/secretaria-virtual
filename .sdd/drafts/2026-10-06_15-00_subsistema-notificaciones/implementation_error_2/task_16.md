---
type: implementation-task
template: system
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Nota del descomponedor (decisión documentada): el diseño pide **mover/conservar** lógica de los antiguos `subsystem/correos` y `subsystem/sms`, que la Tarea 01 borra del árbol. Su código sigue disponible en el historial de Git (p. ej. `git show HEAD:src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java`); consúltalo por ahí, **solo** como origen de lo que el diseño dice que se conserva.

### Ficheros a crear o modificar (fila de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/controller/CorreoController.java` | Crear | k-sistemas (controladores.md) | Reenvío del correo (movido de `correos/controller`) |

### Paso 8 — Controladores y ayudante de respuesta

Es la única pieza de refresco del listado: la usan el `btnSave` y el `btnReenviar` de `Main@Correo-form`/`Main@Sms-form` (el `btnReenviar` de `Centro@` delega en el de `Main@`, Paso 10).

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

## Frontera de confianza — AllowProperties por acción (extracto)

### `CorreoServiceImpl.reenviar` (invocado desde `CorreoController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Correo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Reenviar` de `entity-Notificacion.md` (ninguna).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | — | **NO** | El reenvío actúa sobre `entidadOriginal` (BD); el estado lo cambia `enviar()` (R-Notificacion-003). |

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-014 | VAL-Notificacion-001 | `NotificacionCanalServiceImpl.validateReenviar`; expuesta por `CorreoController/SmsController.validateReenviar` (`Main@{Correo,Sms}-Remote-validateReenviar-action`) |

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-005 | RUI-notificaciones-centro-formulario-correo-004 | `CorreoController.reenviar` → `setNotify` |
| U-notificaciones-todas-011 | RUI-notificaciones-todas-formulario-correo-009 | `CorreoController.reenviar` → `setNotify` |

### Notas y supuestos (extracto)

  - Los controladores de reenvío siguen siendo dos (`CorreoController`, `SmsController`) porque su aviso es distinto por canal y las guías piden moverlos sin cambios; el código común de los servicios sí se extrae (D1).
- **Reenvío con el form en popup** (D3, resuelto al diseñar): `refresh-tab` despacha `tab:refresh` a la pestaña activa (el listado de debajo), no al popup, mientras que `setReload(true)` recarga el form del popup; por eso `CorreoController.reenviar`/`SmsController.reenviar` responden `setReload(true)` y el `btnReenviar` de `Main@` termina en `remote-refreshTab-action`, la misma acción que el alta (nota anterior); el de `Centro@` delega en él.
