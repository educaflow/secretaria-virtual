---
type: implementation-task
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El XML de esta vista YA está materializado y validado con `xmllint` por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/views/Correo-Mios.xml`. **Cópialo literalmente** (sin regenerarlo ni reformatearlo) a su ruta destino `src/main/java/com/educaflow/subsystem/correos/views/Correo-Mios.xml`. Si detectas que el XML está mal, DETENTE y notifica; no lo edites aquí.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/views/Correo-Mios.xml` | Crear | k-vistas | `@Mios-action` + grid (reusa `@Main-form`). |

Descripción de diseño (Paso 5 — Vistas):

`design/views/Correo-Mios.xml`: `action-view @Mios-action` con `<domain>self.dniDestinatario = :__user__.dni</domain>` (U-mis-001) → grid `@Mios-grid` (`canNew="false"`) → reusa `@Main-form` (resuelto por nombre global, está en `Correo-Todos.xml`).

Trazabilidad: U-mis-001 (filtro DNI) → `<domain>self.dniDestinatario = :__user__.dni</domain>` en `@Mios-action`.

Seguridad (Paso 8): Multi-centro / IDOR — `@Mios` filtra por `:__user__.dni`. JPQL/dominios: solo `:__user__.x`; cero concatenación.
