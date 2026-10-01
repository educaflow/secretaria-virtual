---
type: implementation-task
template: system
---

# Tarea 23 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/views/Centro-Sms.xml` | Crear | k-vistas (grids.md, forms.md) | Pantalla «SMS de mis centros» (Supervisor/Administrativo) |

**XML ya materializado:** el fichero está en `design/views/Centro-Sms.xml` y se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/sms/views/Centro-Sms.xml`, **sin regenerarlo** ni reformatearlo (`implementation.md` §1). La fila es `Acción: Crear`.

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño. Los `i18n_*.csv` **MUST NOT** crearse a mano.

### Paso 8 — Vistas

**Ficheros:** `subsystem/sms/views/Main-Sms.xml`, `Centro-Sms.xml`, `Mis-Sms.xml` (Crear) — XML completos en `design/views/`, válidos contra `object-views.xsd`. Un `<action-view>` por fichero, las cinco PI `sv-*` por bloque y en orden, `buttons-panel` explícito (nunca la toolbar nativa) y `save` → `force-back`.

#### `views/Centro-Sms.xml` — pantalla «SMS de mis centros» (Supervisor y Administrativo)

- `action-view` `subsysSms.Centro@Sms-action` con `<domain>` (antes del `<context>`, lo exige el XSD) que limita a los centros donde el usuario es `SUPERVISOR` o `ADMINISTRATIVO`, con `<context name="usuarioId" expr="eval: __user__?.id"/>`. **MUST NOT** usarse `:__user__.campo` con punto (no resuelve en Hibernate y el listado saldría vacío).
- `grid` `subsysSms.Centro@Sms-grid`: `canNew="false"`, `canViewOnClick="true"`, `orderBy="-fechaCreacion"`, columna `centro` en primer lugar para poder filtrar cuando el usuario tiene varios centros (ESC-021).
- `form` `subsysSms.Centro@Sms-form`: los mismos dos paneles, los dos con `readonly="true"` **en el `<panel>`** (ni un solo `readonly` de campo).
- Acciones: `-btnCancel-action` (`back`) y `-btnReenviar-action`, que **reutiliza** los dos `action-method` de `Main-Sms.xml` (el mismo `@CallMethod`, el mismo aviso).

**ASCII Layout — paneles «Datos del SMS» y «Datos del envío»:** idénticos a los de `Main-Sms.xml` (mismos `colSpan`/`colOffset`, mismo orden de campos). Las únicas diferencias: «Datos del SMS» lleva `readonly="true"` en el panel en lugar del `readonlyIf` (aquí nunca se edita) y el panel de envío no lleva `showIf` (aquí siempre hay SMS creado); «Datos del envío» ya es `readonly="true"` en el panel en los dos formularios. Los `showIf` de `fechaEnvio` (U-sms-centro-001) y `descripcionUltimoFallo` (U-sms-centro-002) son los mismos.

**ASCII Layout — `buttons-panel`** (panel plano; U-sms-centro-003):

```
estado == 'FALLIDO'
rr........ss   ← btnReenviar(2) + colOffset(8) + btnCancel «Salir»(2)

estado != 'FALLIDO'
··........ss   ← btnReenviar oculto reserva sus 2 columnas al borde IZQUIERDO, así que «Salir» sigue
                 pegado al derecho
```

No hace falta panel por estado: el condicional es el **primero** del panel y **sin `colOffset`**, la excepción explícita de `k-vistas/forms.md` §«Botones condicionales por estado».

### Widget del teléfono y verificación del Paso 8

El teléfono usa en los tres formularios `widget="phone" x-only-countries="ES"` (U-sms-todos-007: el selector de país solo ofrece España). Es **comodidad**, no defensa: la garantía la da V-Sms-006 en el servidor.

**Verificar:** las tres vistas cargan sin error de metadatos al arrancar; `grep -nE '<form .*can(Back|Delete|Save)="true"' src/main/java/com/educaflow/subsystem/sms/views/*.xml` no devuelve nada; `grep -n "Remote-validateSave\|Remote-validateDelete" src/main/java/com/educaflow/subsystem/sms/views/*.xml` no devuelve nada.

### Trazabilidad — Reglas de UI de esta vista (de `design/design.md`)

| U | Origen spec | Ubicación |
|---|-------------|-----------|
| U-sms-centro-001 | RUI-sms-centro-formulario-001 | `views/Centro-Sms.xml`, `<field name="fechaEnvio" showIf="estado == 'ENVIADO'">` |
| U-sms-centro-002 | RUI-sms-centro-formulario-002 | `views/Centro-Sms.xml`, `<field name="descripcionUltimoFallo" showIf="estado == 'FALLIDO'">` |
| U-sms-centro-003 | RUI-sms-centro-formulario-003 | `views/Centro-Sms.xml`, `<button name="btnReenviar" showIf="estado == 'FALLIDO'">` |

### Notas y supuestos 1 y 9 (de `design/design.md`)

1. **Desviaciones del orden de campos del spec, todas por maquetación.** `screen-sms-todos.md`, `screen-sms-centro.md` y `screen-mis-sms.md` enumeran los campos de cada panel pero, a diferencia de las columnas del grid, no los declaran «(en orden)». Los campos de cada panel son exactamente los que pide el spec, ni uno más ni uno menos; lo que se desvía es el orden, y solo en estos tres puntos:
    - **«Datos del SMS» de `Main-Sms.xml` y `Centro-Sms.xml`** (spec: centro, DNI, nombre, apellidos, teléfono, mensaje, estado del expediente) — `historialEstado` se adelanta a `mensaje`, para que comparta fila con `telefono` (4+8=12) y `mensaje` —que es multilínea y ocupa las 12 columnas— quede el último, en su fila propia. Con el orden literal, `historialEstado` quedaría solo en una última fila detrás del texto largo, con 4 columnas vacías a su derecha.
    - **«Datos del envío» de `Main-Sms.xml` y `Centro-Sms.xml`** (spec: estado, número de reintentos, fecha de creación, fecha de envío, fecha del primer intento, fecha del último intento, descripción del último fallo) — `fechaEnvio` baja de la 4.ª posición a la 6.ª, detrás de `fechaUltimoIntentoEnvio`, para que las cuatro fechas queden en la **misma fila y en su orden temporal** (creación → primer intento → último intento → envío), como exige `k-vistas/forms.md` §Agrupación semántica. De paso, `fechaEnvio` —el único condicional de esa fila (U-sms-todos-003 / U-sms-centro-001)— queda como último elemento de la fila, así que al ocultarse su hueco cae al borde derecho y no desplaza a nadie.
    - **«Datos del SMS» de `Mis-Sms.xml`** (spec: mensaje, fecha de envío, teléfono) — `mensaje` pasa del primer lugar al último por el mismo motivo que en el primer punto (multilínea, 12 columnas, fila propia al final), y `telefono` se adelanta a `fechaEnvio` para que la primera fila lleve primero el dato que identifica al destinatario y después la fecha, en el mismo orden relativo que el grid de la pantalla y que los otros dos formularios.
9. **Las acciones de reenvío se declaran una sola vez**, en `Main-Sms.xml`, y `Centro-Sms.xml` las reutiliza (igual que correos). Son el mismo `@CallMethod` sobre la misma entidad y el mismo mensaje de negocio: declararlas dos veces con nombres distintos daría dos rutas que mantener para una única operación. La regla «un `<action-view>` por fichero, con las acciones que **solo** usa él» se sigue respetando: estas dos no son de un solo `action-view`.
