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

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/controller/CorreoController.java` | Crear | k-sistemas (controladores.md) | Reenvío del correo (movido de `correos/controller`) |

## Paso del diseño

### Paso 8 — Controladores y ayudante de respuesta

**`CorreoController`** y **`SmsController`**: se mueven a `com.educaflow.subsystem.notificaciones.controller` con la misma lógica (`reenviar` y `validateReenviar` vía `ModelServiceFactory.resolve(Correo.class|Sms.class)`, `allowPropertiesReenviar`, aviso «El reenvío del correo/SMS se ha puesto en marcha.»). Cambian paquete e imports y una cosa más: como el form está abierto en popup (D3), tras el aviso `reenviar` responde `actionResponse.setReload(true)`, que recarga el form del popup (ESC-039/040/050/074: «recarga el formulario»), y conserva `setSignal("refresh-tab")`, que refresca el listado de la pestaña de debajo (en `axelor-front`, `refresh-tab` despacha `tab:refresh` a la pestaña activa, no al popup; `reload` refresca el propio form). Implementan U-notificaciones-todas-009/021 y U-notificaciones-centro-004/011.

Verificación (al cerrar el bloque 1–9): compila; `./gradlew -q test --tests '*architecture*'` (C9, C15, C28).

## Frontera de confianza — AllowProperties por acción

### `CorreoServiceImpl.reenviar` (invocado desde `CorreoController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Correo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Reenviar` de `entity-Notificacion.md` (ninguna).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | — | **NO** | El reenvío actúa sobre `entidadOriginal` (BD); el estado lo cambia `enviar()` (R-Notificacion-003). |

## Trazabilidad Origen spec → V/R/U → ubicación

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-014 | VAL-Notificacion-001 | `NotificacionCanalServiceImpl.validateReenviar`; expuesta por `CorreoController/SmsController.validateReenviar` (`Main@{Correo,Sms}-Remote-validateReenviar-action`) |

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-centro-005 | RUI-notificaciones-centro-formulario-correo-004 | `CorreoController.reenviar` → `setNotify` |
| U-notificaciones-todas-011 | RUI-notificaciones-todas-formulario-correo-009 | `CorreoController.reenviar` → `setNotify` |

## Notas y supuestos

- **Cómo se añade un canal** (prueba del segundo desarrollador): entidad `X extends Notificacion` con `computeDestino()`; ítem `X` y su `case` en `TipoNotificacion`; `XServiceImpl extends NotificacionCanalServiceImpl<X>` (el compilador pide los cinco ganchos) y `XService extends NotificacionCanalService<X>`; forms `Main@X-form`, `Centro@X-form`, `Mis@X-form`; `XController` con `reenviar`/`validateReenviar` como `CorreoController` (incluidos `@Transactional`, `setReload(true)` y `setSignal("refresh-tab")`, Paso 8); permisos `X.propio-*`. Los listados, la elección de canal, el envío, el reenvío y la apertura de forms no se tocan.
  - Los controladores de reenvío siguen siendo dos (`CorreoController`, `SmsController`) porque su aviso es distinto por canal y las guías piden moverlos sin cambios; el código común de los servicios sí se extrae (D1).
- **Reenvío con el form en popup** (D3, resuelto al diseñar): `refresh-tab` despacha `tab:refresh` a la pestaña activa (el listado de debajo), no al popup, mientras que `setReload(true)` recarga el form del popup; por eso `CorreoController.reenviar`/`SmsController.reenviar` responden las dos cosas (Paso 8).

> **Nota del descomponedor (decisión ante ambigüedad):** el Paso 1 del diseño borra `subsystem/correos/**`, `subsystem/sms/**` y sus tests (tareas 05–07) antes de esta tarea. Donde el diseño dice que algo «se conserva», «se mueve» o tiene «el mismo cuerpo» que en `correos`/`sms`, el código original se lee del último commit con `git show HEAD:<ruta>` (p.ej. `git show HEAD:src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`). Solo sirve de referencia: **MUST NOT** restaurar esos ficheros.
