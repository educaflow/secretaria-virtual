---
type: implementation-task
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El XML de esta vista YA está materializado y validado con `xmllint` por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/views/Correo-Todos.xml`. **Cópialo literalmente** (sin regenerarlo ni reformatearlo) a su ruta destino `src/main/java/com/educaflow/subsystem/correos/views/Correo-Todos.xml`. Si detectas que el XML está mal, DETENTE y notifica; no lo edites aquí.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/views/Correo-Todos.xml` | Crear | k-vistas | `@Todos-action` + grid + **form compartido** `@Main-form` + sub-grid/form AdjuntoCorreo + acciones. |

Descripción de diseño (Paso 5 — Vistas):

Convención "un `<action-view>` por fichero". El **form `subsysCorreos.Correo@Main-form` es compartido** alta+detalle y se ubica en `Correo-Todos.xml` (única pantalla con botón "Nuevo correo", del Administrador); `@MiCentro` y `@Mios` lo referencian por nombre en modo detalle. El modo se discrimina con `id == null` (alta) / `id != null` (detalle).

`design/views/Correo-Todos.xml`: `action-view @Todos-action` (groups admins por menú, sin `<domain>`) → grid `@Todos-grid` (botón "Nuevo correo", orderBy `-fechaCreacion`) → **form `@Main-form`** (paneles Destinatario/Mensaje `readonlyIf id!=null`; `panel-related` adjuntos; panel Seguimiento `showIf id!=null`; botones Cancelar/Guardar `showIf id==null`, Cerrar `showIf id!=null`, Reenviar `showIf id!=null && estado=='FALLIDO'`) → sub-grid/form `Correo.AdjuntoCorreo@Main` (modal hijo, `onNew` asigna `correo=__parent__`) → action-groups y action-methods (`btnReenviar`→`CorreoController.btnReenviar`, `onChangeDni`→`CorreoController.onChangeDni`).

Trazabilidad V/R/U materializada en esta vista:
- R-AdjuntoCorreo-001 (vincular al padre): `subsysCorreos.Correo.AdjuntoCorreo@Main-set-correo-parent-action` (`<action-record>` con `__parent__`).
- U-correo-001 (autocompletar email onChange dni): `onChange="...@Main-onChange-dni-action"` → `<action-method>` → `CorreoController.onChangeDni`.
- U-correo-002 (detalle solo lectura): `readonlyIf="id != null"` en paneles Destinatario, Mensaje y `panel-related` adjuntos.
- U-correo-003 (referencia siempre readonly): `readonly="true"` fijo en `referenciaHistorialEstadoExpediente`.
- U-correo-004 (panel Seguimiento oculto en alta): `showIf="id != null"` en panel `Seguimiento`.
- U-correo-005 (Reenviar solo admin+FALLIDO): `showIf="id != null && estado == 'FALLIDO'"` en `btnReenviar`.
- U-correo-006 (Cancelar/Guardar solo alta): `showIf="id == null"` en `btnCancel`/`btnSave`.
- U-correo-007 (Cerrar solo detalle): `showIf="id != null"` en `btnCerrar`.
