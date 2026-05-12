# Catálogo de patrones de validación en cliente

Este fichero recopila patrones concretos de validación cliente (`<action-condition>` y `<action-validate>`) extraídos de las vistas de **axelor-open-suite** (Invoice, SaleOrder, Move, Employee, StockMove, Company, etc.) y reformulados a las convenciones del proyecto.

Se usa en la fase de diseño: identificar el patrón aplicable, adaptarlo al campo concreto y redactar el mensaje siguiendo las guías de redacción de `validaciones.md` § 2.3.

> **Recordatorio:** todos estos ejemplos son **duplicación opcional** de validaciones que ya existen en el servidor (`validateInsert`/`validateUpdate`/`validateRemove` o constraints declarativos en el dominio XML). Aquí solo se replican las que son evaluables sin BD para feedback inmediato.

## `<action-condition>` — error pegado a un campo

Se usa cuando el mensaje pertenece a un campo concreto y debe aparecer justo bajo él. Va dentro del `action-group` Local → Remote → save (ver `validaciones.md` § 3.6).

**P1. Campo obligatorio (referencia o select)**

```xml
<action-condition name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <check field="motivoRechazo"
           if="motivoRechazo == null"
           error="Debe indicar un motivo de rechazo"/>
</action-condition>
```

Para selects o enums numéricos también puede ser `== 0`:

```xml
<check field="estado" if="estado == null || estado == 0"
       error="Debe seleccionar un estado"/>
```

**P2. Comparación entre dos fechas del mismo registro**

```xml
<action-condition name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <check field="fechaFin"
           if="fechaInicio != null &amp;&amp; fechaFin != null &amp;&amp; fechaFin &lt; fechaInicio"
           error="La fecha de fin debe ser posterior a la de inicio"/>
</action-condition>
```

**P3. Validación cruzada de dos campos (ambos o ninguno)**

Patrón "height/width": si uno tiene valor, el otro también. Se hace con **dos `<check>`**, cada uno apuntando a su propio campo, para que el mensaje aparezca en el campo que falta:

```xml
<action-condition name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <check field="alto"  if="ancho &gt; 0 &amp;&amp; alto == 0"
           error="Si indica el ancho debe indicar también el alto"/>
    <check field="ancho" if="alto &gt; 0 &amp;&amp; ancho == 0"
           error="Si indica el alto debe indicar también el ancho"/>
</action-condition>
```

**P4. Comparación con un campo del registro padre (`__parent__`)**

Útil en formularios `panel-related` donde el hijo se valida contra un campo del padre (p.ej. un importe que no puede superar el total del padre):

```xml
<action-condition name="subsysXxx.MiEntidad.Linea@Main-Local-validateSave-action">
    <check field="importe"
           if="__parent__.totalSinImpuestos == null || importe &gt; __parent__.totalSinImpuestos"
           error="El importe de la línea no puede superar el total del pedido"/>
</action-condition>
```

**P5. Fecha en el futuro**

`__config__.date` es la fecha actual del servidor:

```xml
<action-condition name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <check field="fechaNacimiento"
           if="fechaNacimiento?.isAfter(__config__.date)"
           error="La fecha de nacimiento no puede estar en el futuro"/>
</action-condition>
```

**P6. Varios campos obligatorios agrupados**

Es habitual encadenar varios `<check>` con `field` distinto en un mismo `<action-condition>`. Cada uno marca su propio campo si falla:

```xml
<action-condition name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <check field="nombre"   if="nombre == null || nombre.isEmpty()"
           error="El nombre es obligatorio"/>
    <check field="email"    if="email == null || email.isEmpty()"
           error="El email es obligatorio"/>
    <check field="telefono" if="telefono == null"
           error="El teléfono es obligatorio"/>
</action-condition>
```

> Atajo: si solo se trata de "no debe estar vacío", se puede omitir `if` y `error` — Axelor usa los del campo por defecto: `<check field="nombre"/>`.

---

## `<action-validate>` — diálogos a nivel de formulario

Se usa cuando el mensaje no pertenece a un campo concreto, o cuando hace falta un `alert`/`info`/`notify` en lugar de error inline. Va también dentro del `action-group` Local → Remote → save.

**P7. Varios errores en un mismo `action-validate`**

Se evalúan los `if` independientemente. Útil para validar varios prerrequisitos antes de una acción:

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <error message="Debe rellenar los datos bancarios de la empresa."
           if="datosBancariosEmpresa == null"/>
    <error message="Debe rellenar la fecha de pago."
           if="fechaPago == null"/>
</action-validate>
```

**P8. Cantidad fuera de rango**

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <error message="La cantidad no puede ser negativa."
           if="cantidad &lt; 0"/>
    <error message="La cantidad no puede superar la cantidad real."
           if="cantidad &gt; cantidadReal"/>
</action-validate>
```

**P9. Lista o colección vacía (al menos uno)**

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <error message="Debe seleccionar al menos una factura"
           if="facturasParaFusionar == null || facturasParaFusionar.size() == 0"/>
</action-validate>
```

**P10. Estado no admite la operación**

Bloquea acciones según el estado actual:

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-validateAprobar-action">
    <error message="No es posible volver al estado VALIDADO desde una factura contabilizada."
           if="estado == 'CONTABILIZADA'"/>
</action-validate>
```

**P11. Permiso por grupo de usuario**

`__user__.group` da acceso al grupo del usuario actual. Útil para bloquear acciones en cliente sin ir al servidor (aunque la autorización real va en el servidor):

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-validateEnviar-action">
    <error message="No está autorizado a realizar esta acción"
           if="__user__.group?.code == 'cliente_externo'"/>
</action-validate>
```

**P12. `alert` para confirmación (continuar o cancelar)**

Se usa cuando técnicamente la operación es válida pero el usuario debería confirmarla antes:

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-confirmar-action">
    <alert message="Esta factura no tiene impuestos. ¿Desea continuar?"
           if="lineasImpuestos == null || lineasImpuestos.isEmpty()"/>
</action-validate>
```

**P13. `alert` sin `if` — confirmación obligatoria siempre**

Útil para operaciones potencialmente destructivas:

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-confirmRegenerar-action">
    <alert message="Regenerar el documento puede generar copias en conflicto. ¿Desea continuar?"/>
</action-validate>
```

**P14. `alert` con interpolación de valores**

`${campo}` interpola el valor actual del registro en el mensaje:

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-validateGuardar-action">
    <alert message="Ya existe una factura con el número ${numeroFactura} y año para el proveedor ${proveedor.nombreCompleto}. ¿Desea continuar?"
           if="duplicadoMismoAnio"/>
</action-validate>
```

**P15. `info` — informativo no bloqueante**

Muestra un diálogo de información que el usuario solo acepta para continuar (no decide nada). Apropiado para explicar un cambio que se acaba de hacer o un estado del registro. Suele invocarse desde `onLoad` a través de un `action-group`:

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-showArchivedInfo-action">
    <info message="Este expediente está archivado. No se puede modificar."
          if="estado == 'ARCHIVADO'"/>
</action-validate>
```

**P16. `notify` — notificación breve no intrusiva**

Aparece en una esquina sin interrumpir el flujo. Apropiado para feedback de éxito o avisos secundarios:

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-checkEmpresaActiva-action">
    <notify message="Seleccione la empresa activa"
            if="__repo__(Empresa).all().count() != 1"/>
</action-validate>
```

**P17. `error` con acción correctiva (`action=`)**

El atributo `action` ejecuta una acción adicional cuando se dispara el error. Patrón habitual: limpiar el campo inválido para que el usuario lo vuelva a introducir:

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-validateFechaHasta-action">
    <error message="La fecha hasta no puede ser anterior a la fecha desde"
           if="fechaHasta != null &amp;&amp; fechaHasta &lt; fechaDesde"
           action="subsysXxx.MiEntidad@Main-set-fechaHasta-null-action"/>
</action-validate>
```

---

→ Ver **`validaciones.md`** § 3.5 ("Cómo elegir el patrón" y "Combinar varios patrones a la vez").
