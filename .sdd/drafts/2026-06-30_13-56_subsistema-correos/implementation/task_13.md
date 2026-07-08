---
type: implementation-task
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/views/Correo-Mis.xml` | Crear | k-vistas (forms.md, grids.md) | Pantalla «Mis correos» (`@Mis`) |

## Instrucción de materialización — XML ya materializado, NO regenerar

El fichero **ya está completo y validado** en `design/views/Correo-Mis.xml` de esta iniciativa. **MUST** copiarlo **literalmente** (`cp`, sin reescribir ni reformatear) a `src/main/java/com/educaflow/subsystem/correos/views/Correo-Mis.xml` (crear la carpeta con `mkdir -p` si no existe). **MUST NOT** regenerarlo desde el resumen de abajo.

## Texto del diseño (verbatim, `design.md`, Paso 8 — Vistas, parte de `Correo-Mis.xml`)

- **`views/Correo-Mis.xml`** (resumen): `subsysCorreos.Correo@Mis-action` con `<domain>self.dniDestinatario = :dniUsuarioActual and self.estado = :estadoExitoso</domain>` + `@Mis-grid` (asunto/para/expediente/fechaEnvío) + `@Mis-form` (solo asunto/cuerpo/para/enCopia/fechaEnvío, sin ningún dato de fallo ni botón de acción, panel de adjuntos de solo lectura).

**Verificar:** `bash validate.sh` valida los 4 ficheros contra `object-views.xsd`; ningún `<form>` tiene `can(Back|Delete|Save)="true"`.

### Nota y supuesto aplicable (verbatim, `design.md`)

14. **`btnCancel` ("Salir") en `Correo@Mis-form` (`views/Correo-Mis.xml`) y en `Correo@Centro-form` (`views/Correo-Centro.xml`), pese a que `screen-mis-correos.md` describe ambos formularios como "de solo lectura: sin botones" (desviación deliberada, no un olvido).** Ambas vistas se abren con `canBack="false"` (siguiendo el patrón estándar del proyecto para formularios de detalle de solo consulta) y con `view-param name="show-toolbar-form" value="false"` (oculta la toolbar nativa de Axelor, que es la que normalmente ofrece el botón atrás/cerrar). Sin la toolbar y sin `canBack`, un formulario "sin botones" tal cual pide el spec dejaría al usuario sin ninguna forma de volver al listado salvo el botón "atrás" del navegador, lo que se considera peor UX que añadir un botón explícito. Por eso se añade `btnCancel`/"Salir" (`action-group` con `<action name="back"/>`) en el `buttons-panel` de ambos formularios: es la única acción del panel, no introduce edición ni cambia el carácter de "solo lectura" de la vista, y sustituye estrictamente a la navegación que la toolbar nativa habría dado.

**MUST NOT** crear ningún otro fichero de vista ni ninguna vista adicional no descrita aquí.
