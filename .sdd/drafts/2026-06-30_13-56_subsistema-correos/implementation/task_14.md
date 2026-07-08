---
type: implementation-task
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/views/Adjunto-ref.xml` | Crear | k-vistas (forms.md) | Vistas de solo lectura/referencia de `Adjunto` (`@Search-grid`/`@View-form`), compartidas por Centro y Mis |

## Instrucción de materialización — XML ya materializado, NO regenerar

El fichero **ya está completo y validado** en `design/views/Adjunto-ref.xml` de esta iniciativa. **MUST** copiarlo **literalmente** (`cp`, sin reescribir ni reformatear) a `src/main/java/com/educaflow/subsystem/correos/views/Adjunto-ref.xml` (crear la carpeta con `mkdir -p` si no existe). **MUST NOT** regenerarlo desde el resumen de abajo.

## Texto del diseño (verbatim, `design.md`, Paso 8 — Vistas, parte de `Adjunto-ref.xml`)

- **`views/Adjunto-ref.xml`** (resumen): `subsysCorreos.Adjunto@Search-grid`/`@View-form`, de solo lectura, compartidas por `Correo-Centro.xml` y `Correo-Mis.xml` (RUI-correos-centro-formulario-002/003 y la ausencia total de botones de acción en Mis correos). Nombre de fichero y de vistas **sin** el prefijo del padre `Correo.`, siguiendo la excepción de nomenclatura de `design-contract.md` §6 y el precedente real del proyecto (`Curso-ref.xml`, `CursoModulo-ref.xml`).

**Verificar:** `bash validate.sh` valida los 4 ficheros contra `object-views.xsd`.

## Trazabilidad U- aplicable a este fichero (verbatim, `design.md`)

| U | Origen spec | Ubicación |
|---|---|---|
| U-correos-administracion-formulario-adjunto-007 | RUI-correos-administracion-formulario-adjunto-007 | `views/Correo.xml` — `panel-related "adjuntosConsulta"` usa el `@View-form` de solo lectura de `views/Adjunto-ref.xml` |

**MUST NOT** crear ningún otro fichero de vista ni ninguna vista adicional no descrita aquí.
