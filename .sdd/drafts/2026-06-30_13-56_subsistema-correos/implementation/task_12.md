---
type: implementation-task
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/views/Correo-Centro.xml` | Crear | k-vistas (forms.md, grids.md) | Pantalla «Correos de mi centro» (`@Centro`) |

## Instrucción de materialización — XML ya materializado, NO regenerar

El fichero **ya está completo y validado** en `design/views/Correo-Centro.xml` de esta iniciativa. **MUST** copiarlo **literalmente** (`cp`, sin reescribir ni reformatear) a `src/main/java/com/educaflow/subsystem/correos/views/Correo-Centro.xml` (crear la carpeta con `mkdir -p` si no existe). **MUST NOT** regenerarlo desde el resumen de abajo.

## Texto del diseño (verbatim, `design.md`, Paso 8 — Vistas, parte de `Correo-Centro.xml`)

- **`views/Correo-Centro.xml`** (resumen): `subsysCorreos.Correo@Centro-action` con `<domain>self.centro = :centroActivoUsuario</domain>` (multi-centro real, k-secure-coding §4) + `@Centro-grid` (mismas columnas menos «centro», sin botón nuevo) + `@Centro-form` (100% de solo lectura, mismo panel «Envío», `btnReenviar` igual que en `@Main`, reutilizando el mismo `@CallMethod` — `CorreoController.reenviar` siempre emite el aviso de RUI-correos-centro-formulario-004).

**Verificar:** `bash validate.sh` valida los 4 ficheros contra `object-views.xsd`; ningún `<form>` tiene `can(Back|Delete|Save)="true"`; los `action-group` de `btnSave`/`btnDelete` del form principal (`@Main`) usan `remote-validationSave-action`/`remote-validationDelete-action`; los del modal de `Adjunto` (`save-modal`/`delete-modal`) no usan ninguna acción `remote-validation*`.

## Trazabilidad U- aplicable a este fichero (verbatim, `design.md`)

| U | Origen spec | Ubicación |
|---|---|---|
| U-correos-centro-formulario-001 | RUI-correos-centro-formulario-001 | `views/Correo-Centro.xml` — botón `btnReenviar`, `showIf="estado == 'FAIL'"` |
| U-correos-centro-formulario-002 | RUI-correos-centro-formulario-002 | `views/Correo-Centro.xml` — campo `descripcionUltimoFallo`, `showIf="estado == 'FAIL'"` |
| U-correos-centro-formulario-003 | RUI-correos-centro-formulario-003 | `views/Correo-Centro.xml` — campo `fechaEnvio`, `showIf="estado == 'SUCCESS'"` |
| U-correos-centro-formulario-004 | RUI-correos-centro-formulario-004 | `CorreoController.reenviar` — `actionResponse.setNotify(...)` (ya materializado en la Tarea 06; se cita aquí solo por trazabilidad) |

### Nota y supuesto aplicable (verbatim, `design.md`)

14. **`btnCancel` ("Salir") en `Correo@Mis-form` (`views/Correo-Mis.xml`) y en `Correo@Centro-form` (`views/Correo-Centro.xml`), pese a que `screen-mis-correos.md` describe ambos formularios como "de solo lectura: sin botones" (desviación deliberada, no un olvido).** Ambas vistas se abren con `canBack="false"` (siguiendo el patrón estándar del proyecto para formularios de detalle de solo consulta) y con `view-param name="show-toolbar-form" value="false"` (oculta la toolbar nativa de Axelor, que es la que normalmente ofrece el botón atrás/cerrar). Sin la toolbar y sin `canBack`, un formulario "sin botones" tal cual pide el spec dejaría al usuario sin ninguna forma de volver al listado salvo el botón "atrás" del navegador, lo que se considera peor UX que añadir un botón explícito. Por eso se añade `btnCancel`/"Salir" (`action-group` con `<action name="back"/>`) en el `buttons-panel` de ambos formularios: es la única acción del panel, no introduce edición ni cambia el carácter de "solo lectura" de la vista, y sustituye estrictamente a la navegación que la toolbar nativa habría dado.

**MUST NOT** crear ningún otro fichero de vista ni ninguna vista adicional no descrita aquí.
