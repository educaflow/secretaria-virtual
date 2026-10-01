---
type: implementation-task
template: system
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/views/Main-Sms.xml` | Crear | k-vistas (grids.md, forms.md, actions.md) | Pantalla «Administración de SMS» (Administrador) |

**XML ya materializado:** el fichero está en `design/views/Main-Sms.xml` y se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/sms/views/Main-Sms.xml`, **sin regenerarlo** ni reformatearlo (`implementation.md` §1). La fila es `Acción: Crear`.

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño. Los `i18n_*.csv` **MUST NOT** crearse a mano.

### Paso 8 — Vistas

**Ficheros:** `subsystem/sms/views/Main-Sms.xml`, `Centro-Sms.xml`, `Mis-Sms.xml` (Crear) — XML completos en `design/views/`, válidos contra `object-views.xsd`. Un `<action-view>` por fichero, las cinco PI `sv-*` por bloque y en orden, `buttons-panel` explícito (nunca la toolbar nativa) y `save` → `force-back`.

#### `views/Main-Sms.xml` — pantalla «Administración de SMS» (Administrador)

- `action-view` `subsysSms.Main@Sms-action` (grid + form, `show-toolbar-form=false`, `forceEdit=true`).
- `grid` `subsysSms.Main@Sms-grid`: `canNew="true"`, `newButtonTitle="Nuevo SMS"`, `orderBy="-fechaCreacion"` (del más reciente al más antiguo), `allowSearchFields="true"`, `canEditOnClick="true"`. Columnas en el orden del spec: estado, dniDestinatario, nombre, apellidos, telefono, mensaje, centro, nombreExpediente, fechaCreacion, fechaEnvio.
- `form` `subsysSms.Main@Sms-form`: dos paneles + `buttons-panel`. Sirve de alta (editable) y de detalle. La solo-lectura se declara **una sola vez, en el `<panel>`**, nunca campo a campo (`k-vistas/forms.md` admite los condicionales «de campos **y paneles**», y así lo hace ya `subsystem/importacion/views/Main-TareaImportacion.xml`): `<panel name="Sms" readonlyIf="(id != null) || (cid != null)">` para el detalle (U-sms-todos-001) y `<panel name="Envio" readonly="true">` **fijo**, porque ese panel solo aparece en el detalle (su `showIf`) y sus siete campos son `servidor`: el usuario no los edita nunca (ESC-013 y `screen-sms-todos.md`: «todos en solo lectura»). Un campo nuevo en cualquiera de los dos paneles hereda la decisión sin que nadie tenga que acordarse de repetir el atributo.
- Acciones: `-btnCancel-action` (`back`), `-btnSave-action` (`Local-validateSave` → `remote-validationSave-action` → `save` → `force-back`), `-btnReenviar-action` (`Remote-validateReenviar` → `Remote-reenviar`), el `action-condition` local de campos obligatorios y los dos `action-method` que llaman a `SmsController`.
- **No hay** `action-group` de borrado ni botón «Borrar»: desviación del estándar que declara `screen-sms-todos.md` (un SMS no se puede borrar).

**ASCII Layout — panel «Datos del SMS»** (idéntico en alta y en detalle; lo único que cambia es el `readonlyIf` del panel, U-sms-todos-001):

```
ccccccddd···   ← centro(6) + dniDestinatario(3); el colOffset(3) de nombre cierra la fila
nnnnnnaaaaaa   ← nombre(6) + apellidos(6)                                    [par natural]
tttthhhhhhhh   ← telefono(4, widget phone) + historialEstado(8)
mmmmmmmmmmmm   ← mensaje(12, widget Text)                                    [texto del SMS]
```

Dimensionado con la tabla de proporcionalidad de `k-vistas/forms.md`: `centro` es un selector de nombre medio (6), `dniDestinatario` un identificador (3), `nombre` y `apellidos` nombres cortos (6 cada uno, el par en su fila y en su orden natural), `telefono` un identificador con el selector de país del widget `phone` (4) y `historialEstado` una referencia de nombre largo (8). El orden es el de `screen-sms-todos.md` salvo en un punto: `historialEstado` se adelanta a `mensaje` (ver «Notas y supuestos» 1, que enumera todas las desviaciones de orden del diseño).

**ASCII Layout — panel «Datos del envío»** (solo en el detalle, U-sms-todos-002; el panel entero `readonly="true"`), un dibujo por estado:

```
estado == 'ENVIADO'
eeerrr······   ← estado(3) + numeroReintentos(3); el colOffset(6) de fechaCreacion cierra la fila
cccpppuuuvvv   ← fechaCreacion(3) + fechaPrimerIntentoEnvio(3) + fechaUltimoIntentoEnvio(3)
                 + fechaEnvio(3)          [las cuatro fechas juntas, en orden temporal]
dddddddddddd   ← descripcionUltimoFallo oculta: fila propia, no desplaza a nadie

estado == 'FALLIDO'
eeerrr······
cccpppuuu···   ← fechaEnvio oculta (U-sms-todos-003): es el último elemento de su fila, así que su
                 hueco queda al borde derecho y no desplaza a nadie
dddddddddddd   ← descripcionUltimoFallo(12, widget Text; U-sms-todos-004): fila propia

estado == 'PENDIENTE'
eeerrr······
cccpppuuu···   ← fechaEnvio oculta (U-sms-todos-003): es el último elemento de su fila, así que su
                 hueco queda al borde derecho y no desplaza a nadie
dddddddddddd   ← descripcionUltimoFallo oculta: fila propia, no desplaza a nadie
```

Las cuatro fechas van en la **misma fila y en su orden natural** (creación → primer intento → último intento → envío), que es la secuencia temporal del envío: `k-vistas/forms.md` §Agrupación semántica lo exige, y por eso `fechaEnvio` baja de la 4.ª posición del spec a la 6.ª (ver «Notas y supuestos» 1), y la tabla de proporcionalidad da **3** a un campo de fecha, así que ninguna se infla a 6. Los dos condicionales llevan el `showIf` **en el propio campo** (uno al borde derecho de su fila, el otro en fila propia): no hace falta ningún panel anidado, y **MUST NOT** envolverlos en uno (un panel anidado abre fila nueva).

**ASCII Layout — `buttons-panel`** (dos paneles anidados con `showIf` mutuamente excluyentes que cubren todos los estados; U-sms-todos-005 y U-sms-todos-006):

```
buttonsAlta      (id == null) && (cid == null)
........ccgg   ← colOffset(8) + btnCancelAlta «Cancelar»(2) + btnSave «Guardar»(2)

buttonsDetalle   (id != null) || (cid != null)
  estado == 'FALLIDO'
rr........ss   ← btnReenviar «Reenviar»(2) + colOffset(8) + btnCancelSalir «Salir»(2)
  estado != 'FALLIDO'
··........ss   ← btnReenviar oculto reserva sus 2 columnas al borde IZQUIERDO, así que «Salir» sigue
                 pegado al derecho
```

Los principales quedan pegados al borde derecho (`colOffset + colSpan = 12`) y el único secundario («Reenviar») a la izquierda. No hace falta un panel por estado dentro del detalle: «Reenviar» es el **primer** botón del panel y **no lleva `colOffset`**, la excepción explícita de `k-vistas/forms.md` §«Botones condicionales por estado» — la misma que ya usa `Centro-Sms.xml`. Así hay **un** panel y **un** botón «Salir» en vez de dos gemelos con el mismo `action-group`. Los nombres de botón empiezan por el `btnXxx` de su `onClick`.

### Widget del teléfono y verificación del Paso 8

El teléfono usa en los tres formularios `widget="phone" x-only-countries="ES"` (U-sms-todos-007: el selector de país solo ofrece España). Es **comodidad**, no defensa: la garantía la da V-Sms-006 en el servidor.

**Verificar:** las tres vistas cargan sin error de metadatos al arrancar; `grep -nE '<form .*can(Back|Delete|Save)="true"' src/main/java/com/educaflow/subsystem/sms/views/*.xml` no devuelve nada; `grep -n "Remote-validateSave\|Remote-validateDelete" src/main/java/com/educaflow/subsystem/sms/views/*.xml` no devuelve nada.

### Trazabilidad — Reglas de UI de esta vista (de `design/design.md`)

| U | Origen spec | Ubicación |
|---|-------------|-----------|
| U-sms-todos-001 | RUI-sms-todos-formulario-001 | `views/Main-Sms.xml`: `<panel name="Sms" readonlyIf="(id != null) || (cid != null)">` (una sola vez, en el panel) |
| U-sms-todos-002 | RUI-sms-todos-formulario-002 | `views/Main-Sms.xml`, `<panel name="Envio" showIf="(id != null) || (cid != null)" readonly="true">` (el `readonly` en el panel) |
| U-sms-todos-003 | RUI-sms-todos-formulario-003 | `views/Main-Sms.xml`, `<field name="fechaEnvio" showIf="estado == 'ENVIADO'">` |
| U-sms-todos-004 | RUI-sms-todos-formulario-004 | `views/Main-Sms.xml`, `<field name="descripcionUltimoFallo" showIf="estado == 'FALLIDO'">` |
| U-sms-todos-005 | RUI-sms-todos-formulario-005 | `views/Main-Sms.xml`, paneles `buttonsAlta` (Cancelar/Guardar) y `buttonsDetalle` (Salir), con `showIf` excluyentes |
| U-sms-todos-006 | RUI-sms-todos-formulario-006 | `views/Main-Sms.xml`, `<button name="btnReenviar" showIf="estado == 'FALLIDO'">` dentro de `buttonsDetalle` |
| U-sms-todos-007 | RUI-sms-todos-formulario-007 | `views/Main-Sms.xml`, `<field name="telefono" widget="phone" x-only-countries="ES">` (mismo widget en `Centro-Sms.xml` y `Mis-Sms.xml`) |
| U-sms-todos-008 | RUI-sms-todos-formulario-008 | `views/Main-Sms.xml`, `action-group subsysSms.Main@Sms-btnSave-action`: `<action name="force-back"/>` tras `save` (+ `canBackOnSave="true"` en el `<form>`) |

### Notas y supuestos 1 y 9 (de `design/design.md`)

1. **Desviaciones del orden de campos del spec, todas por maquetación.** `screen-sms-todos.md`, `screen-sms-centro.md` y `screen-mis-sms.md` enumeran los campos de cada panel pero, a diferencia de las columnas del grid, no los declaran «(en orden)». Los campos de cada panel son exactamente los que pide el spec, ni uno más ni uno menos; lo que se desvía es el orden, y solo en estos tres puntos:
    - **«Datos del SMS» de `Main-Sms.xml` y `Centro-Sms.xml`** (spec: centro, DNI, nombre, apellidos, teléfono, mensaje, estado del expediente) — `historialEstado` se adelanta a `mensaje`, para que comparta fila con `telefono` (4+8=12) y `mensaje` —que es multilínea y ocupa las 12 columnas— quede el último, en su fila propia. Con el orden literal, `historialEstado` quedaría solo en una última fila detrás del texto largo, con 4 columnas vacías a su derecha.
    - **«Datos del envío» de `Main-Sms.xml` y `Centro-Sms.xml`** (spec: estado, número de reintentos, fecha de creación, fecha de envío, fecha del primer intento, fecha del último intento, descripción del último fallo) — `fechaEnvio` baja de la 4.ª posición a la 6.ª, detrás de `fechaUltimoIntentoEnvio`, para que las cuatro fechas queden en la **misma fila y en su orden temporal** (creación → primer intento → último intento → envío), como exige `k-vistas/forms.md` §Agrupación semántica. De paso, `fechaEnvio` —el único condicional de esa fila (U-sms-todos-003 / U-sms-centro-001)— queda como último elemento de la fila, así que al ocultarse su hueco cae al borde derecho y no desplaza a nadie.
    - **«Datos del SMS» de `Mis-Sms.xml`** (spec: mensaje, fecha de envío, teléfono) — `mensaje` pasa del primer lugar al último por el mismo motivo que en el primer punto (multilínea, 12 columnas, fila propia al final), y `telefono` se adelanta a `fechaEnvio` para que la primera fila lleve primero el dato que identifica al destinatario y después la fecha, en el mismo orden relativo que el grid de la pantalla y que los otros dos formularios.
9. **Las acciones de reenvío se declaran una sola vez**, en `Main-Sms.xml`, y `Centro-Sms.xml` las reutiliza (igual que correos). Son el mismo `@CallMethod` sobre la misma entidad y el mismo mensaje de negocio: declararlas dos veces con nombres distintos daría dos rutas que mantener para una única operación. La regla «un `<action-view>` por fichero, con las acciones que **solo** usa él» se sigue respetando: estas dos no son de un solo `action-view`.
