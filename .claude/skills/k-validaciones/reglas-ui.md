# Reglas de UI (`U-XXX`)

## 1. ¿Qué es una regla de UI?

Una **regla de UI** (`U-XXX`) cambia el aspecto o el estado de un formulario en función del valor de uno o varios campos, del usuario actual, del registro padre o de un evento (`onNew`, `onLoad`, `onChange`). Solo afecta a lo que ve y puede editar el usuario en pantalla — **no bloquea operaciones ni modifica el estado del sistema**.

Regla mnemotécnica para distinguir las tres categorías:

- Validación (`V-XXX`) → "no puedes" (bloquea la operación).
- Regla de negocio (`R-XXX`) → "ahora hago esto" (escribe en BD o produce efectos colaterales).
- Regla de UI (`U-XXX`) → "ahora ves esto" (solo cambia el formulario).

Si una regla bloquea guardar, **es una `V-XXX`**, no una `U-XXX`. Si una regla escribe en BD o envía un correo, **es una `R-XXX`**, no una `U-XXX`.

Las reglas de UI se identifican con un código estable `U-XXX` que se asigna durante el análisis y se mantiene a lo largo del diseño y la implementación para garantizar la trazabilidad.

---

## 2. Cómo se documenta (durante el análisis)

Cada `U-XXX` se documenta en una tabla con la información mínima necesaria para implementarla.

### 2.1 Tabla `U-XXX`

| ID    | Disparador               | Efecto              | Campo/Panel afectado | Condición                        | Mecanismo                               |
|-------|--------------------------|---------------------|----------------------|----------------------------------|-----------------------------------------|
| U-001 | continuo                 | Ocultar panel       | `paso2Rechazado`     | `pasoActual != 'paso2Rechazado'` | `showIf` en el `<panel>`                |
| U-002 | continuo                 | Marcar solo lectura | `fechaResolucion`    | `estado == 'CERRADO'`            | `readonlyIf` en el `<field>`            |
| U-003 | `onNew`                  | Valor por defecto   | `centro`             | Siempre                          | `<action-record>` con `__user__.centro` |
| U-004 | `onChange:tipoDocumento` | Recalcular dominio  | `plantilla`          | Siempre                          | `<action-attrs>` con `name="domain"`    |
| U-005 | continuo                 | Marcar requerido    | `motivoRechazo`      | `estado == 'RECHAZADO'`          | `requiredIf` en el `<field>`            |
| U-006 | `onLoad`                 | Ocultar botón       | `btnPublicar`        | `__user__.group.code != 'admin'` | `<action-attrs>` sobre `hidden`         |

Significado de las columnas:

- **Disparador**: cuándo se aplica la regla. Puede ser un evento (`onNew`, `onLoad`, `onChange:campoX`) o `continuo` cuando se evalúa permanentemente mediante un atributo `*If`.
- **Efecto**: qué cambia el usuario en pantalla — mostrar/ocultar, marcar readonly, marcar requerido, fijar valor, filtrar dominio, cambiar título.
- **Campo/Panel afectado**: elemento de la vista sobre el que actúa la regla.
- **Condición**: expresión booleana sobre el registro, `__user__`, `__parent__` o `__config__`. Si es `Siempre`, no hay condición.
- **Mecanismo**: cómo se implementa en XML (ver sección 3).

### 2.2 Guías para redactar la regla

- Describir qué **ve** el usuario, no cómo se implementa: *"El campo `motivoRechazo` se muestra solo cuando el estado es `RECHAZADO`"* en vez de *"Añadir `showIf` al campo motivoRechazo"*.
- Si la regla **bloquea** una operación cuando no se cumple, **no es `U-XXX`** — es una `V-XXX`.
- Si la regla **escribe** en BD u otros registros, **no es `U-XXX`** — es una `R-XXX`.
- Una regla que combina varios efectos sobre campos distintos (p.ej. ocultar A y marcar B como requerido) suele dividirse en varias `U-XXX` separadas — mejora la trazabilidad al diseño.
- Los valores por defecto al crear (`onNew`) son `U-XXX`, no `R-XXX`, porque no se escribe nada hasta que el usuario pulsa Guardar.

---

## 3. Cómo se implementa

### 3.1 Tabla de decisión: qué mecanismo usar

| Necesidad                                                  | Mecanismo                                                              |
|------------------------------------------------------------|------------------------------------------------------------------------|
| Mostrar/ocultar campo o panel según expresión simple       | `showIf` / `hideIf` en el `<field>` o `<panel>`                        |
| Marcar readonly o required según expresión simple          | `readonlyIf` / `requiredIf` en el `<field>`                            |
| Cambiar otro atributo (`domain`, `title`, `hidden`, …)     | `<action-attrs>` referenciada desde `onChange`, `onLoad` u `onNew`     |
| Fijar valor por defecto al crear                           | `<action-record>` referenciada desde `onNew`                           |
| Asignar valor a un campo cuando cambia otro                | `<action-record>` referenciada desde el `onChange` del campo disparador|
| Filtrar opciones de un campo relacional                    | `<action-attrs>` con `name="domain"` sobre el campo relacional         |

Regla práctica: si la condición es **una expresión booleana corta sobre el propio registro**, preferir el atributo inline (`showIf`, `readonlyIf`, `requiredIf`). Si requiere **lógica condicional, varias asignaciones o filtros dinámicos**, usar `<action-attrs>` o `<action-record>` desde un evento.

### 3.2 Atributos inline (`showIf`, `hideIf`, `readonlyIf`, `requiredIf`)

Se evalúan continuamente y reaccionan automáticamente al cambio de cualquier campo referenciado en la expresión. No necesitan `onChange`.

```xml
<panel name="paso2Rechazado" title="Rechazar firmar el documento"
       showIf="pasoActual == 'paso2Rechazado'">
    ...
</panel>

<field name="fechaResolucion" readonlyIf="estado == 'CERRADO'"/>
<field name="motivoRechazo"  requiredIf="estado == 'RECHAZADO'"/>
```

### 3.3 `<action-attrs>` desde un evento

Para cambiar atributos que no tienen forma `*If` o cuando hace falta lógica imperativa. Cada `<attribute>` modifica un atributo del campo/panel indicado en `for`:

| `name` del atributo  | Para qué                                                                  |
|----------------------|---------------------------------------------------------------------------|
| `hidden`             | Ocultar/mostrar el campo, panel o botón (alternativa a `showIf`/`hideIf`) |
| `readonly`           | Marcar solo lectura (alternativa a `readonlyIf`)                          |
| `required`           | Marcar requerido (alternativa a `requiredIf`)                             |
| `domain`             | Filtrar el dominio de un campo relacional                                 |
| `value`              | Asignar un valor (alternativa imperativa a `<action-record>`)             |
| `value:add` / `:del` | Añadir/quitar elementos a un campo many-to-many o lista                   |
| `selection-in`       | Restringir las opciones disponibles de un campo `<selection>`             |
| `title`              | Cambiar el título del campo                                               |
| `active`             | Activar (poner como tab visible) un `<panel>` dentro de un `<panel-tabs>` |

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

### 3.4 `<action-record>` para valores por defecto y asignaciones

Para asignar valores iniciales (`onNew`) o derivados al cambiar otro campo (`onChange`):

```xml
<action-record name="subsysXxx.MiEntidad@Main-set-centro-currentUser-action" model="com.educaflow.subsystem.xxx.db.MiEntidad">
    <field name="centro" expr="eval: __user__.centro"/>
</action-record>
```

Referenciado desde el formulario mediante el evento `onNew`, que apunta a un `<action-group>` que incluye la asignación de valores por defecto (ver `k-vistas/actions.md` para los `<action-group>` de eventos).

---

## 4. Trazabilidad: del análisis al diseño

- Cada `U-XXX` del análisis aparece en al menos un paso del diseño.
- Cada paso del diseño que implementa reglas de UI lista qué `U-XXX` cubre y qué mecanismo usa. Ejemplos:
  - *"Paso 4 — `MiEntidad.xml` (vista), atributos `showIf`/`readonlyIf` en los campos. Cubre U-001, U-002, U-005."*
  - *"Paso 5 — `MiEntidad.xml` (vista), `<action-record>` referenciada desde `onNew`. Cubre U-003."*
  - *"Paso 6 — `MiEntidad.xml` (vista), `<action-attrs>` referenciada desde el `onChange` de `tipoDocumento`. Cubre U-004."*
- Antes de cerrar el diseño, construir la matriz `U-XXX → paso(s)`. Ninguna fila puede quedar vacía.

---

## 5. Referencias

- `validaciones.md` — validaciones `V-XXX` (bloquean operaciones). Distinguir de `U-XXX` (solo UX).
- `reglas-negocio.md` — reglas de negocio `R-XXX` (modifican el estado del sistema). Distinguir de `U-XXX` (solo formulario).
- `k-vistas/actions.md` — sintaxis completa de `<action-attrs>`, `<action-record>` y `<action-group>` con eventos (`onNew`, `onLoad`, `onChange`).
