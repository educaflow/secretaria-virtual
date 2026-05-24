---
type: implementation-task
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El XML de esta vista YA está materializado y validado con `xmllint` por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/views/Correo-MiCentro.xml`. **Cópialo literalmente** (sin regenerarlo ni reformatearlo) a su ruta destino `src/main/java/com/educaflow/subsystem/correos/views/Correo-MiCentro.xml`. Si detectas que el XML está mal, DETENTE y notifica; no lo edites aquí.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/views/Correo-MiCentro.xml` | Crear | k-vistas | `@MiCentro-action` + grid (reusa `@Main-form`). |

Descripción de diseño (Paso 5 — Vistas):

`design/views/Correo-MiCentro.xml`: `action-view @MiCentro-action` con `<domain>self.centro = :__user__.centroActivo</domain>` (U-mi-centro-001) → grid `@MiCentro-grid` (`canNew="false"`) → reusa `@Main-form` (resuelto por nombre global, está en `Correo-Todos.xml`).

Trazabilidad: U-mi-centro-001 (filtro centro) → `<domain>self.centro = :__user__.centroActivo</domain>` en `@MiCentro-action`.

Seguridad (Paso 8): Multi-centro / IDOR — `@MiCentro` filtra por `:__user__.centroActivo`. JPQL/dominios: solo `:__user__.x`; cero concatenación.
