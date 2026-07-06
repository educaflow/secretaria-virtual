# Reglas de UI (`RUI-`)

Una regla de UI (`RUI-`) cambia el aspecto o el estado de un formulario según el valor de uno o varios campos, el usuario actual, el registro padre o un evento (`onNew`, `onLoad`, `onChange`). Solo afecta a lo que ve y puede editar el usuario — **no bloquea operaciones ni modifica el estado del sistema**. Qué es y cómo se distingue de una `VAL-` o una `RN-` lo define la spec; aquí solo **cómo se implementa**.

---

## 1. Tabla de decisión: qué mecanismo usar

| Necesidad | Mecanismo |
|---|---|
| Mostrar/ocultar campo o panel según expresión simple | `showIf` / `hideIf` en el `<field>` o `<panel>` |
| Marcar readonly o required según expresión simple | `readonlyIf` / `requiredIf` en el `<field>` |
| Cambiar otro atributo (`domain`, `title`, `hidden`, …) | `<action-attrs>` referenciada desde `onChange`, `onLoad` u `onNew` |
| Fijar valor por defecto al crear | `<action-record>` referenciada desde `onNew` |
| Asignar valor a un campo cuando cambia otro | `<action-record>` referenciada desde el `onChange` del campo disparador |
| Filtrar opciones de un campo relacional | `<action-attrs>` con `name="domain"` sobre el campo relacional |

Regla práctica: si la condición es **una expresión booleana corta sobre el propio registro**, prefiere el atributo inline (`showIf`, `readonlyIf`, `requiredIf`). Si requiere **lógica condicional, varias asignaciones o filtros dinámicos**, usa `<action-attrs>` o `<action-record>` desde un evento.

El atributo `disparador` que la spec declara en cada `RUI-` determina el punto de enganche:

| `disparador` (spec) | Punto de enganche |
|---|---|
| `continuo` | Atributo inline `showIf`/`hideIf`/`readonlyIf`/`requiredIf` (se reevalúa solo, sin evento) |
| `al crear` | `<action-record>`/`<action-attrs>` desde el `onNew` del formulario |
| `al cargar` | `<action-attrs>` desde el `onLoad` del formulario |
| `al cambiar <campo>` | `<action-attrs>`/`<action-record>` desde el `onChange` del campo disparador |

---

## 2. Atributos inline (`showIf`, `hideIf`, `readonlyIf`, `requiredIf`)

Se evalúan continuamente y reaccionan automáticamente al cambio de cualquier campo referenciado en la expresión. No necesitan `onChange`.

```xml
<panel name="paso2Rechazado" title="Rechazar firmar el documento"
       showIf="pasoActual == 'paso2Rechazado'">
    ...
</panel>

<field name="fechaResolucion" readonlyIf="estado == 'CERRADO'"/>
<field name="motivoRechazo"  requiredIf="estado == 'RECHAZADO'"/>
```

---

## 3. `<action-attrs>` desde un evento

Para cambiar atributos que no tienen forma `*If` o cuando hace falta lógica imperativa. Cada `<attribute>` modifica un atributo del campo/panel indicado en `for`:

| `name` del atributo | Para qué |
|---|---|
| `hidden` | Ocultar/mostrar el campo, panel o botón (alternativa a `showIf`/`hideIf`) |
| `readonly` | Marcar solo lectura (alternativa a `readonlyIf`) |
| `required` | Marcar requerido (alternativa a `requiredIf`) |
| `domain` | Filtrar el dominio de un campo relacional |
| `value` | Asignar un valor (alternativa imperativa a `<action-record>`) |
| `value:add` / `:del` | Añadir/quitar elementos a un campo many-to-many o lista |
| `selection-in` | Restringir las opciones disponibles de un campo `<selection>` |
| `title` | Cambiar el título del campo |
| `active` | Activar (poner como tab visible) un `<panel>` dentro de un `<panel-tabs>` |

Atributos de `<attribute>`: `for` (campo destino), `name` (atributo a cambiar), `expr` (valor o expresión `eval:`), opcionalmente `if` (la asignación solo se aplica si la condición es cierta).

```xml
<!-- Filtrar dinámicamente el dominio de plantilla según el tipoDocumento elegido -->
<action-attrs name="subsysXxx.MiEntidad@Main-set-plantilla.domain-tipoDocumento-action">
    <attribute for="plantilla" name="domain"
               expr="eval: &quot;self.tipoDocumento.id = ${tipoDocumento?.id}&quot;"/>
</action-attrs>

<!-- Restringir las opciones de estado según un flag, con if= -->
<action-attrs name="subsysXxx.MiEntidad@Main-set-estado.selection-in-action">
    <attribute for="estado" name="selection-in" expr="eval: ['BORRADOR', 'ENVIADO']" if="!esAdmin"/>
    <attribute for="estado" name="selection-in" expr="eval: ['BORRADOR', 'ENVIADO', 'APROBADO']" if="esAdmin"/>
</action-attrs>
```

Y desde la vista se referencia en el evento adecuado:

```xml
<field name="tipoDocumento" onChange="subsysXxx.MiEntidad@Main-set-plantilla.domain-tipoDocumento-action"/>
```

---

## 4. `<action-record>` para valores por defecto y asignaciones

Para asignar valores iniciales (`onNew`) o derivados al cambiar otro campo (`onChange`):

```xml
<action-record name="subsysXxx.MiEntidad@Main-set-centro-currentUser-action" model="com.educaflow.subsystem.xxx.db.MiEntidad">
    <field name="centro" expr="eval: __user__.centro"/>
</action-record>
```

Referenciado desde el formulario mediante el evento `onNew`, que apunta a un `<action-group>` que incluye la asignación de valores por defecto (ver `k-vistas/actions.md` para los `<action-group>` de eventos).

> Un valor por defecto al crear es una `RUI-`, no una `RN-`: no se escribe nada en BD hasta que el usuario pulsa Guardar.

---

## 5. Referencias

- [`validaciones.md`](validaciones.md) — validaciones `VAL-` (bloquean). Distinguir de `RUI-` (solo UX).
- [`reglas-negocio.md`](reglas-negocio.md) — reglas de negocio `RN-` (modifican el estado del sistema). Distinguir de `RUI-` (solo formulario).
- `k-vistas/actions.md` — sintaxis completa de `<action-attrs>`, `<action-record>` y `<action-group>` con eventos (`onNew`, `onLoad`, `onChange`).
