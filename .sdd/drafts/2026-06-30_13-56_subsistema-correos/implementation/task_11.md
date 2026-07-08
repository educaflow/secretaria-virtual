---
type: implementation-task
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml` | Crear | k-vistas (forms.md, grids.md) | Pantalla «Administración de correos» (`@Main`) + vistas embebidas de `Adjunto` en alta (`@Main`) |

## Instrucción de materialización — XML ya materializado, NO regenerar

El fichero **ya está completo y validado** en `design/views/Correo.xml` de esta iniciativa. **MUST** copiarlo **literalmente** (`cp`, sin reescribir ni reformatear) a `src/main/java/com/educaflow/subsystem/correos/views/Correo.xml` (crear la carpeta con `mkdir -p` si no existe). **MUST NOT** regenerarlo desde el resumen de abajo: el resumen es solo contexto, la fuente de verdad es el propio fichero XML de `design/`.

## Texto del diseño (verbatim, `design.md`, Paso 8 — Vistas, parte de `Correo.xml`)

Ficheros completos en `design/views/`.

- **`views/Correo.xml`** (resumen): `subsysCorreos.Correo@Main-action` + `@Main-grid` (listado con estado/dni/nombre/apellidos/asunto/para/centro/expediente/fechaCreación/fechaEnvío, orden por fecha de creación descendente, `canNew="true"`) + `@Main-form` (panel «Correo» editable solo en alta —`readonlyIf="(id != null) || (cid != null)"` en todos sus campos, siguiendo la convención real del proyecto para distinguir un registro ya guardado (`id`) de uno en curso de creación en el cliente (`cid`), ver `k-vistas/forms.md`—, dos `panel-related` de `adjuntos` mutuamente excluyentes según `(id == null) && (cid == null)` / `(id != null) || (cid != null)` —uno editable con el `@Main-form` de `Adjunto` para la alta, otro de solo lectura con el `@View-form` compartido para la consulta—, panel «Envío» solo visible con `(id != null) || (cid != null)`, `buttons-panel` con `btnReenviar` (`showIf="estado == 'FAIL'"`), `btnDelete` oculto (`showIf="false"`: RES-Correo-003 hace que nunca se pueda borrar, así que el botón no tiene ninguna combinación de estado en la que deba verse — se documenta oculto en vez de eliminado para que la plantilla estándar de `buttons-panel` — k-vistas/forms.md — se mantenga reconocible) y los pares `btnCancelAlta`/`btnCancelSalir` y `btnSave` (`showIf="(id == null) && (cid == null)"` / `showIf="(id != null) || (cid != null)"`) que materializan RUI-correos-administracion-formulario-003/004/005). Además, embebidas en el mismo fichero: `subsysCorreos.Correo.Adjunto@Main-grid`/`@Main-form` (edición durante la alta, con `onNew` que fija `correo` = `__parent__` — RUI-correos-administracion-formulario-adjunto-001— y validación local de obligatorios — RUI-...-005/006; sus botones `btnCancelAlta`/`btnCancelSalir`/`btnSave` usan deliberadamente solo `id == null`/`id != null`, sin `cid` — ver "Notas y supuestos" sobre por qué extender `cid` a este modal introduciría un fallo real, y solo `btnDelete` sí usa `(id!=null) || (cid!=null)`, que es correcto porque un `Adjunto` recién añadido a la colección **sí** recibe un `cid` inmediatamente).

**Verificar:** `bash validate.sh` valida los 4 ficheros contra `object-views.xsd`; ningún `<form>` tiene `can(Back|Delete|Save)="true"`; los `action-group` de `btnSave`/`btnDelete` del form principal (`@Main`) usan `remote-validationSave-action`/`remote-validationDelete-action`; los del modal de `Adjunto` (`save-modal`/`delete-modal`) no usan ninguna acción `remote-validation*`.

## Trazabilidad U- aplicable a este fichero (verbatim, `design.md`)

| U | Origen spec | Ubicación |
|---|---|---|
| U-correos-administracion-formulario-001 | RUI-correos-administracion-formulario-001 | `views/Correo.xml` — botón `btnReenviar`, `showIf="estado == 'FAIL'"` |
| U-correos-administracion-formulario-002 | RUI-correos-administracion-formulario-002 | `views/Correo.xml` — panel «Envío», `showIf="(id != null) || (cid != null)"` |
| U-correos-administracion-formulario-003 | RUI-correos-administracion-formulario-003 | `views/Correo.xml` — botón `btnCancelAlta`, `showIf="(id == null) && (cid == null)"` |
| U-correos-administracion-formulario-004 | RUI-correos-administracion-formulario-004 | `views/Correo.xml` — botón `btnCancelSalir`, `showIf="(id != null) || (cid != null)"` |
| U-correos-administracion-formulario-005 | RUI-correos-administracion-formulario-005 | `views/Correo.xml` — botón `btnSave`, `showIf="(id == null) && (cid == null)"` |
| U-correos-administracion-formulario-006 | RUI-correos-administracion-formulario-006 | `views/Correo.xml` — todos los campos del panel «Correo», `readonlyIf="(id != null) || (cid != null)"` |
| U-correos-administracion-formulario-007 | RUI-correos-administracion-formulario-007 | `views/Correo.xml` — campo `descripcionUltimoFallo`, `showIf="estado == 'FAIL'"` |
| U-correos-administracion-formulario-008 | RUI-correos-administracion-formulario-008 | `views/Correo.xml` — campo `fechaEnvio`, `showIf="estado == 'SUCCESS'"` |
| U-correos-administracion-listado-adjuntos-001 | RUI-correos-administracion-listado-adjuntos-001 | `views/Correo.xml` — `panel-related "adjuntos"` (editable) frente a `"adjuntosConsulta"` (solo lectura), `showIf="(id == null) && (cid == null)"` / `showIf="(id != null) || (cid != null)"` |
| U-correos-administracion-formulario-adjunto-001 | RUI-correos-administracion-formulario-adjunto-001 | `views/Correo.xml` — `action-record subsysCorreos.Correo.Adjunto@Main-set-correo-parent-action`, `onNew` |
| U-correos-administracion-formulario-adjunto-002 | RUI-correos-administracion-formulario-adjunto-002 | `views/Correo.xml` — botón `btnCancelAlta` del `Adjunto`, `showIf="id == null"` |
| U-correos-administracion-formulario-adjunto-003 | RUI-correos-administracion-formulario-adjunto-003 | `views/Correo.xml` — botón `btnCancelSalir` del `Adjunto`, `showIf="id != null"` (nunca se alcanza en la práctica: el `Adjunto@Main-form` solo se abre en alta; documentado para cumplir la regla del spec al pie de la letra) |
| U-correos-administracion-formulario-adjunto-004 | RUI-correos-administracion-formulario-adjunto-004 | `views/Correo.xml` — botón `btnSave` del `Adjunto`, `showIf="id == null"` |
| U-correos-administracion-formulario-adjunto-005 | RUI-correos-administracion-formulario-adjunto-005 | `views/Correo.xml` — `field name="nombreFichero" required="true"` |
| U-correos-administracion-formulario-adjunto-006 | RUI-correos-administracion-formulario-adjunto-006 | `views/Correo.xml` — `field name="contenido" required="true"` |
| U-correos-administracion-formulario-adjunto-007 | RUI-correos-administracion-formulario-adjunto-007 | `views/Correo.xml` — `panel-related "adjuntosConsulta"` usa el `@View-form` de solo lectura de `views/Adjunto-ref.xml` |

### Notas y supuestos aplicables (verbatim, `design.md`)

9. **Botón «Borrar» del formulario de administración.** RES-Correo-003 impide borrar un correo en cualquier circunstancia; el `buttons-panel` estándar del proyecto (k-vistas/forms.md) siempre incluye `btnDelete`, así que se mantiene en la vista con `showIf="false"` (nunca visible) en vez de eliminarlo, dejando constancia expresa de que la ausencia de borrado es intencionada y no un olvido.
12. **Convención `(id != null) || (cid != null)` aplicada al `@Main-form` de `Correo`, pero deliberadamente NO al modal de `Adjunto` (salvo su `btnDelete`, que ya la traía).** `k-vistas/forms.md` documenta esta convención explícitamente solo para el botón Borrar («`id` es el ID del registro ya guardado; `cid` es el ID temporal de un registro nuevo todavía no guardado»); el `Model.getCid()` real de Axelor (`axelor-core`, `com.axelor.db.Model`) confirma que `cid` es el **«collection id»**: se asigna a un registro cuando se añade a un widget de **colección** (un `panel-related` o/m2m editable) para poder identificarlo antes de tener `id`, no a un registro raíz abierto directamente desde un grid de nivel superior. Aplicarlo al `@Main-form` de `Correo` (raíz, nunca embebido en ninguna colección) es seguro pero inerte — su `cid` nunca estará poblado, igual que ocurre con el `btnDelete` de `Ciclo@Main-form`/`Curso@Main-form`/`Grado@Main-form` (raíces reales del proyecto que también lo llevan aunque su `cid` nunca varíe) — y se aplica por consistencia con el estilo documentado. **NO se aplica**, en cambio, a `btnCancelAlta`/`btnCancelSalir`/`btnSave` del modal `Correo.Adjunto@Main-form`: un `Adjunto` nuevo añadido vía "Añadir adjunto" a la colección `adjuntos` **sí** recibe un `cid` inmediatamente (es precisamente el caso para el que existe `cid`), así que `showIf="(id == null) && (cid == null)"` ocultaría el botón «Guardar» —y `showIf="(id != null) || (cid != null)"` mostraría «Salir» en vez de «Cancelar»— desde el instante en que se abre el modal para un adjunto nuevo, antes incluso de rellenarlo: un fallo funcional real, no solo de estilo. Por eso el modal conserva `id == null`/`id != null` (sin `cid`) en esos tres botones — el mismo patrón que usa `TareaImportacion.xml` (panel y botones visibles solo en alta) en el código real del proyecto — y solo `btnDelete` usa `(id!=null) || (cid!=null)`, correcto porque "Borrar" sobre un adjunto recién añadido y aún no guardado (quitarlo de la colección) es una acción válida y esperada.

**MUST NOT** crear ningún otro fichero de vista ni ninguna vista adicional no descrita aquí.
