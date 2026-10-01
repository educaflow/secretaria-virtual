---
type: implementation-task
template: system
---

# Tarea 24 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/views/Mis-Sms.xml` | Crear | k-vistas (grids.md, forms.md) | Pantalla «Mis SMS» (cualquier usuario) |

**XML ya materializado:** el fichero está en `design/views/Mis-Sms.xml` y se debe **copiar literalmente** a `src/main/java/com/educaflow/subsystem/sms/views/Mis-Sms.xml`, **sin regenerarlo** ni reformatearlo (`implementation.md` §1). La fila es `Acción: Crear`.

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios de este diseño. Los `i18n_*.csv` **MUST NOT** crearse a mano.

### Paso 8 — Vistas

**Ficheros:** `subsystem/sms/views/Main-Sms.xml`, `Centro-Sms.xml`, `Mis-Sms.xml` (Crear) — XML completos en `design/views/`, válidos contra `object-views.xsd`. Un `<action-view>` por fichero, las cinco PI `sv-*` por bloque y en orden, `buttons-panel` explícito (nunca la toolbar nativa) y `save` → `force-back`.

#### `views/Mis-Sms.xml` — pantalla «Mis SMS» (cualquier usuario)

- `action-view` `subsysSms.Mis@Sms-action` con `<domain>self.dniDestinatario = :dniUsuarioActual and self.estado = :estadoEnviado</domain>` y los dos `<context>` (`eval: __user__?.dni` y `ENVIADO`): solo los SMS **enviados con éxito** a su DNI, de cualquier centro (ESC-024).
- `grid` `subsysSms.Mis@Sms-grid`: `canNew="false"`, `canViewOnClick="true"`, `orderBy="-fechaEnvio"`, columnas mensaje, telefono, nombreExpediente, fechaEnvio.
- `form` `subsysSms.Mis@Sms-form`: un solo panel con `readonly="true"` en el propio `<panel>`, y «Salir».

**ASCII Layout — panel «Datos del SMS»:**

```
tttfff······   ← telefono(3) + fechaEnvio(3); el colOffset(6) de mensaje completa la fila
mmmmmmmmmmmm   ← mensaje(12, widget Text)
```

El orden invierte el de `screen-mis-sms.md` (mensaje, fecha de envío, teléfono) para dejar el texto multilínea el último, en su fila propia, igual que en los otros dos formularios (ver «Notas y supuestos» 1).

**ASCII Layout — `buttons-panel`:**

```
··········ss   ← colOffset(10) + btnCancel «Salir»(2)
```


### Widget del teléfono y verificación del Paso 8

El teléfono usa en los tres formularios `widget="phone" x-only-countries="ES"` (U-sms-todos-007: el selector de país solo ofrece España). Es **comodidad**, no defensa: la garantía la da V-Sms-006 en el servidor.

**Verificar:** las tres vistas cargan sin error de metadatos al arrancar; `grep -nE '<form .*can(Back|Delete|Save)="true"' src/main/java/com/educaflow/subsystem/sms/views/*.xml` no devuelve nada; `grep -n "Remote-validateSave\|Remote-validateDelete" src/main/java/com/educaflow/subsystem/sms/views/*.xml` no devuelve nada.

### Trazabilidad — Reglas de UI de esta vista (de `design/design.md`)

| U | Origen spec | Ubicación |
|---|-------------|-----------|
| U-sms-todos-007 | RUI-sms-todos-formulario-007 | `views/Main-Sms.xml`, `<field name="telefono" widget="phone" x-only-countries="ES">` (mismo widget en `Centro-Sms.xml` y `Mis-Sms.xml`) |

### Notas y supuestos 1 (de `design/design.md`)

1. **Desviaciones del orden de campos del spec, todas por maquetación.** `screen-sms-todos.md`, `screen-sms-centro.md` y `screen-mis-sms.md` enumeran los campos de cada panel pero, a diferencia de las columnas del grid, no los declaran «(en orden)». Los campos de cada panel son exactamente los que pide el spec, ni uno más ni uno menos; lo que se desvía es el orden, y solo en estos tres puntos:
    - **«Datos del SMS» de `Mis-Sms.xml`** (spec: mensaje, fecha de envío, teléfono) — `mensaje` pasa del primer lugar al último por el mismo motivo que en el primer punto (multilínea, 12 columnas, fila propia al final), y `telefono` se adelanta a `fechaEnvio` para que la primera fila lleve primero el dato que identifica al destinatario y después la fecha, en el mismo orden relativo que el grid de la pantalla y que los otros dos formularios.
