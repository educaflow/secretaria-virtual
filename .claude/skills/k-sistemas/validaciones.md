---
name: k-sistemas/validaciones
description: Cómo implementar validaciones en Axelor — dos capas: cliente (action-validate, action-condition, atributos de campo showIf/requiredIf/readonlyIf) y servidor (validateInsert/validateUpdate/validateRemove + controlador). Patrón completo action-group con ambas capas.
---

# Validaciones en Axelor

Las validaciones en Axelor tienen dos capas complementarias. **Ambas son obligatorias**: la capa cliente nunca sustituye a la de servidor.

```
┌─────────────────────────────────────────────────────────────┐
│  CAPA CLIENTE (XML de vistas)                               │
│  Inmediata, sin llamada al servidor                         │
│  ① Atributos de campo: required, showIf, readonlyIf        │
│  ② action-condition: error debajo del campo concreto        │
│  ③ action-validate: error/alert a nivel de formulario       │
├─────────────────────────────────────────────────────────────┤
│  CAPA SERVIDOR (Java)                                       │
│  Requiere viaje al servidor; puede consultar BD             │
│  ④ validateInsert/validateUpdate/validateRemove en servicio │
│     llamados desde controlador vía action-method            │
└─────────────────────────────────────────────────────────────┘
```

---

## Cuándo usar cada capa

| Tipo de validación | Capa | Mecanismo |
|-------------------|------|-----------|
| Campo obligatorio simple (siempre) | Cliente | `required="true"` en el campo |
| Campo obligatorio condicional | Cliente | `requiredIf="expresion"` en el campo o `<action-condition>` |
| Campo visible/oculto según condición | Cliente | `showIf` / `hideIf` en campo o panel |
| Campo de solo lectura según condición | Cliente | `readonlyIf` en campo o panel |
| Formato, rango, consistencia entre campos | Cliente | `action-validate` o `action-condition` |
| Unicidad (¿ya existe en BD?) | Servidor | `validateInsert` / `validateUpdate` |
| Integridad referencial (¿existe la referencia?) | Servidor | `validateInsert` / `validateUpdate` |
| Reglas de negocio con consultas a BD | Servidor | `validateInsert` / `validateUpdate` |
| Validación de borrado (¿tiene hijos?) | Servidor | `validateRemove` |

---

## Capa 1 — Atributos de campo en el formulario

Los atributos más simples se ponen directamente en el `<field>` o `<panel>` del formulario. No requieren ninguna acción adicional.

### Obligatoriedad

```xml
<!-- Siempre obligatorio -->
<field name="nombre" required="true" colSpan="6"/>

<!-- Obligatorio solo si otro campo tiene cierto valor -->
<field name="motivoRechazo" requiredIf="estado == 'RECHAZADO'" colSpan="12"/>

<!-- Obligatorio si el campo está visible (combinación habitual) -->
<field name="representanteLegal" showIf="esMenorDeEdad" requiredIf="esMenorDeEdad" colSpan="6"/>
```

### Solo lectura condicional

```xml
<!-- Siempre de solo lectura -->
<field name="numeroExpediente" readonly="true" colSpan="4"/>

<!-- Solo lectura según el estado -->
<field name="descripcion" readonlyIf="estado != 'BORRADOR'" colSpan="12"/>

<!-- Panel completo de solo lectura según estado -->
<panel name="datosGenerales" readonlyIf="estado == 'ARCHIVADO'" colSpan="12">
    <field name="titulo" colSpan="8"/>
    <field name="fechaInicio" colSpan="4"/>
</panel>
```

### Mostrar / ocultar campos o paneles

```xml
<!-- Ocultar campo según condición -->
<field name="cif" showIf="tipoPersona == 'JURIDICA'" colSpan="4"/>
<field name="nif" hideIf="tipoPersona == 'JURIDICA'" colSpan="4"/>

<!-- Ocultar panel completo — preferible a ocultar campos sueltos para evitar huecos en el layout -->
<panel name="datosEmpresa" showIf="tipoPersona == 'JURIDICA'" colSpan="12">
    <field name="razonSocial" colSpan="8"/>
    <field name="cif" colSpan="4"/>
</panel>
```

> **REGLA CRÍTICA — showIf en campos vs. paneles:** Cuando varios campos deben mostrarse/ocultarse juntos, usar `showIf`/`hideIf` en el `<panel>` que los contiene, **no en cada campo individual**. Los campos con `showIf` que se ocultan dejan un hueco en el grid CSS del formulario. Los paneles con `showIf` no dejan hueco.

---

## Capa 2 — `<action-condition>` (validación por campo)

Muestra el mensaje de error **justo debajo del campo** al que se refiere. Ideal para validaciones de un campo concreto.

```xml
<action-condition name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <!-- Sin atributo 'if': verifica que el campo no esté vacío -->
    <check field="motivoRechazo"/>

    <!-- Con 'if': verifica la condición sobre el campo -->
    <check field="fechaFin" if="fechaInicio != null &amp;&amp; fechaFin &lt; fechaInicio"
           error="La fecha de fin debe ser posterior a la fecha de inicio"/>

    <!-- Obligatoriedad condicional explícita -->
    <check field="email" if="telefono == null || telefono.isEmpty()"
           error="Debe indicar al menos un email o un teléfono de contacto"/>
</action-condition>
```

**Atributos de `<check>`:**
- `field` — nombre del campo al que se asocia el error (requerido)
- `if` — expresión booleana; si es `true` se muestra el error. Si se omite, comprueba que el campo no sea null/vacío.
- `error` — mensaje que se muestra debajo del campo. Si se omite, se muestra un mensaje genérico de campo requerido.

> **IMPORTANTE en expresiones XML:** los operadores `<`, `>`, `&&` se deben escapar como `&lt;`, `&gt;`, `&amp;&amp;` dentro de atributos XML.

---

## Capa 3 — `<action-validate>` (validación de formulario)

Muestra el mensaje como un **diálogo modal** o una notificación. Se usa para validaciones cruzadas o reglas que afectan a varios campos a la vez.

```xml
<action-validate name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <!-- error: bloquea la acción; el usuario debe corregir -->
    <error message="El nombre no puede estar vacío" if="nombre == null || nombre.isEmpty()"/>
    <error message="El descuento no puede superar el 100%" if="descuento > 100"/>

    <!-- alert: pide confirmación; el usuario puede continuar o cancelar -->
    <alert message="El descuento supera el 20%. ¿Desea continuar?" if="descuento > 20"/>

    <!-- info: mensaje informativo; el usuario lo acepta y continúa -->
    <info message="Recuerde adjuntar la documentación necesaria antes de enviar" if="estado == 'BORRADOR'"/>

    <!-- notify: notificación breve no bloqueante -->
    <notify message="Los datos se han guardado correctamente"/>
</action-validate>
```

**Tipos de mensaje:**
| Tipo | Comportamiento | Cuándo usar |
|------|---------------|------------|
| `error` | Bloquea; el usuario debe corregir | Regla que no se puede ignorar |
| `alert` | Pide confirmación: Continuar / Cancelar | Advertencia; el usuario puede tener razón |
| `info` | Muestra mensaje; el usuario lo acepta y sigue | Información útil no bloqueante |
| `notify` | Notificación breve en la esquina; no interrumpe | Feedback de éxito o información menor |

Si la acción produce un `error`, el resto de acciones del `action-group` **no se ejecutan**. Si produce `alert`, se detiene hasta que el usuario confirme o cancele.

---

## Capa 4 — Validaciones en el servidor (Java)

### En el servicio: `validateInsert` / `validateUpdate` / `validateRemove`

Los tres métodos acumulan errores en `BusinessMessages` y devuelven `Optional<BusinessMessages>`. **Nunca lanzan `BusinessException`**.

```java
@Override
public Optional<BusinessMessages> validateInsert(MiEntidad entidad) {
    BusinessMessages messages = new BusinessMessages();

    // Validación de unicidad
    if (((MiEntidadRepository) repository).findByNif(entidad.getNif()) != null) {
        messages.add(new BusinessMessage("nif",
            "Ya existe un registro con el NIF '" + entidad.getNif() + "'. " +
            "Verifique que no está duplicando el registro."));
    }

    // Regla de negocio con consulta a BD
    List<String> tiposValidos = tipoRepository.findActivos().stream()
        .map(Tipo::getCodigo).collect(Collectors.toList());
    if (!tiposValidos.contains(entidad.getTipo())) {
        messages.add(new BusinessMessage("tipo",
            "El tipo '" + entidad.getTipo() + "' no es válido. " +
            "Los tipos disponibles son: " + String.join(", ", tiposValidos)));
    }

    return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
}

@Override
public Optional<BusinessMessages> validateUpdate(MiEntidad entidad, MiEntidad entidadOriginal) {
    BusinessMessages messages = new BusinessMessages();

    // Validación de unicidad excluyendo el propio registro
    MiEntidad existente = ((MiEntidadRepository) repository).findByNif(entidad.getNif());
    if (existente != null && !existente.getId().equals(entidad.getId())) {
        messages.add(new BusinessMessage("nif",
            "Ya existe otro registro con el NIF '" + entidad.getNif() + "'."));
    }

    return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
}

@Override
public Optional<BusinessMessages> validateRemove(MiEntidad entidad) {
    BusinessMessages messages = new BusinessMessages();

    // Validación de integridad referencial
    long hijos = hijoRepository.countByPadre(entidad);
    if (hijos > 0) {
        messages.add(new BusinessMessage("id",
            "No se puede eliminar '" + entidad.getNombre() + "' porque tiene " +
            hijos + " registros asociados. Elimínelos primero."));
    }

    return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
}
```

**Reglas para los mensajes de error en servidor:**
1. **Incluir el valor incorrecto recibido** — "El NIF '12345678Z' no existe" mejor que "El NIF no existe"
2. **Incluir los valores válidos** cuando se pueden obtener de BD — "Los tipos disponibles son: A, B, C"
3. **Wrap en try/catch** cuando los valores válidos vienen de BD, para que un error de conectividad no bloquee la validación:

```java
// Si la consulta de valores válidos puede fallar
try {
    List<String> tiposValidos = tipoRepository.findActivos().stream()
        .map(Tipo::getCodigo).collect(Collectors.toList());
    if (!tiposValidos.contains(entidad.getTipo())) {
        messages.add(new BusinessMessage("tipo",
            "El tipo '" + entidad.getTipo() + "' no es válido. " +
            "Los tipos disponibles son: " + String.join(", ", tiposValidos)));
    }
} catch (Exception e) {
    // Si no podemos obtener los valores válidos, al menos indicar que el tipo no es válido
    messages.add(new BusinessMessage("tipo",
        "El tipo '" + entidad.getTipo() + "' no es válido."));
}
```

### El controlador: patrón estándar

El controlador es el puente entre las acciones XML y el servicio. Siempre sigue este mismo patrón:

```java
public class MiEntidadController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        final Repository repository = JpaRepository.of(MiEntidad.class);
        final MiEntidadService service = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class, repository);

        ActionRequestHelper<MiEntidad> requestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
        ActionResponseHelper responseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        MiEntidad entidad = requestHelper.getModel(allowProperties);

        Optional<BusinessMessages> result;
        if (requestHelper.getId() == null) {
            result = service.validateInsert(entidad);
        } else {
            MiEntidad original = requestHelper.getOriginalModel(allowProperties);
            result = service.validateUpdate(entidad, original);
        }

        if (result.isPresent()) {
            responseHelper.doResponseBusinessMessagesAsError(result.get());
        }
    }

    @CallMethod
    public void validateDelete(ActionRequest actionRequest, ActionResponse actionResponse) {
        final Repository repository = JpaRepository.of(MiEntidad.class);
        final MiEntidadService service = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class, repository);

        ActionRequestHelper<MiEntidad> requestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
        ActionResponseHelper responseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        MiEntidad entidad = requestHelper.getModel(allowProperties);

        Optional<BusinessMessages> result = service.validateRemove(entidad);
        if (result.isPresent()) {
            responseHelper.doResponseBusinessMessagesAsError(result.get());
        }
    }
}
```

---

## El patrón completo: action-group encadenando ambas capas

El patrón estándar del proyecto encadena siempre en este orden: validaciones locales → validación remota → acción de sistema (`save`/`delete`).

### Para el botón Guardar

```xml
<!-- 1. El botón del formulario llama al action-group -->
<button name="btnGuardar" title="Guardar"
        onClick="subsysXxx.MiEntidad@Main-btnGuardar-action"
        type="default"/>

<!-- 2. action-group: orquesta la secuencia -->
<action-group name="subsysXxx.MiEntidad@Main-btnGuardar-action">
    <action name="subsysXxx.MiEntidad@Main-Local-validateSave-action"/>   <!-- cliente -->
    <action name="subsysXxx.MiEntidad@Main-Remote-validateSave-action"/>  <!-- servidor -->
    <action name="save"/>                                                   <!-- persiste -->
</action-group>

<!-- 3. Validaciones locales (cliente, sin viaje al servidor) -->
<action-condition name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <check field="nombre"/>
    <check field="email" if="email != null &amp;&amp; !email.contains('@')"
           error="El email debe tener el formato usuario@dominio.com"/>
    <check field="fechaFin"
           if="fechaInicio != null &amp;&amp; fechaFin != null &amp;&amp; fechaFin &lt; fechaInicio"
           error="La fecha de fin debe ser posterior a la fecha de inicio"/>
</action-condition>

<!-- 4. Validación remota (servidor, puede consultar BD) -->
<action-method name="subsysXxx.MiEntidad@Main-Remote-validateSave-action"
               model="com.educaflow.subsystem.xxx.db.MiEntidad">
    <call class="com.educaflow.subsystem.xxx.controller.MiEntidadController"
          method="validateSave"/>
</action-method>
```

### Para el botón Eliminar

```xml
<action-group name="subsysXxx.MiEntidad@Main-btnEliminar-action">
    <action name="subsysXxx.MiEntidad@Main-Remote-validateDelete-action"/>
    <action name="delete-modal"/>
</action-group>

<action-method name="subsysXxx.MiEntidad@Main-Remote-validateDelete-action"
               model="com.educaflow.subsystem.xxx.db.MiEntidad">
    <call class="com.educaflow.subsystem.xxx.controller.MiEntidadController"
          method="validateDelete"/>
</action-method>
```

### Para el onSave del formulario (alternativa sin botón explícito)

Si el formulario usa el `save` nativo de Axelor (sin botón personalizado), se puede conectar al evento `onSave`:

```xml
<form name="subsysXxx.MiEntidad@Main-form"
      onSave="subsysXxx.MiEntidad@Main-onSave-action"
      ...>
```

```xml
<action-group name="subsysXxx.MiEntidad@Main-onSave-action">
    <action name="subsysXxx.MiEntidad@Main-Local-validateSave-action"/>
    <action name="subsysXxx.MiEntidad@Main-Remote-validateSave-action"/>
</action-group>
```

---

## Cuándo usar `action-condition` vs. `action-validate`

| Criterio | `action-condition` | `action-validate` |
|----------|--------------------|-------------------|
| El error pertenece a un campo concreto | Sí | No (error general) |
| El mensaje aparece debajo del campo | Sí | No (diálogo modal) |
| Validaciones de varios campos a la vez | No ideal | Sí |
| Se quiere mostrar `alert` (con confirmación) | No | Sí |
| Se quiere mostrar `info` o `notify` | No | Sí |

En la práctica, lo más habitual es combinar ambas:
- `action-condition` para campos obligatorios y validaciones de campo concreto
- `action-validate` para reglas cruzadas y advertencias que requieren confirmación del usuario

---

## Ejemplo completo de un fichero de vistas con validaciones

Estructura del fichero XML de vistas siguiendo las convenciones del proyecto:

```xml
<object-views ...>

    <!-- ============================================================ -->
    <!-- ===================== action-view ========================== -->
    <!-- ============================================================ -->
    <action-view name="subsysXxx.MiEntidad@Main-action" title="Mi Entidad"
                 model="com.educaflow.subsystem.xxx.db.MiEntidad">
        <view type="grid" name="subsysXxx.MiEntidad@Main-grid"/>
        <view type="form" name="subsysXxx.MiEntidad@Main-form"/>
        <view-param name="show-toolbar-form" value="false"/>
    </action-view>

    <!-- ============================================================ -->
    <!-- ======================== grid ============================== -->
    <!-- ============================================================ -->
    <grid name="subsysXxx.MiEntidad@Main-grid" title="Mi Entidad" ...>
        ...
    </grid>

    <!-- ============================================================ -->
    <!-- ======================== form ============================== -->
    <!-- ============================================================ -->
    <form name="subsysXxx.MiEntidad@Main-form" title="Mi Entidad"
          onSave="subsysXxx.MiEntidad@Main-onSave-action"
          model="com.educaflow.subsystem.xxx.db.MiEntidad">
        <panel colSpan="12">
            <!-- Atributos de campo: validaciones simples directas -->
            <field name="nombre"        colSpan="6"  required="true"/>
            <field name="tipo"          colSpan="6"  required="true"/>
            <field name="email"         colSpan="6"/>
            <field name="fechaInicio"   colSpan="3"/>
            <field name="fechaFin"      colSpan="3"/>
            <!-- Condicional: solo visible si tipo = ESPECIAL -->
            <field name="motivoEspecial" colSpan="12"
                   showIf="tipo == 'ESPECIAL'" requiredIf="tipo == 'ESPECIAL'"/>
        </panel>
    </form>

    <!-- *********************************************************************************************  -->
    <!-- ***************************** Acciones de las tareas principales ****************************  -->
    <!-- *********************************************************************************************  -->
    <action-group name="subsysXxx.MiEntidad@Main-onSave-action">
        <action name="subsysXxx.MiEntidad@Main-Local-validateSave-action"/>
        <action name="subsysXxx.MiEntidad@Main-Remote-validateSave-action"/>
    </action-group>

    <!-- *********************************************************************************************  -->
    <!-- ***************************** Acciones de Validaciones en local  ****************************  -->
    <!-- *********************************************************************************************  -->
    <action-condition name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
        <check field="nombre"/>
        <check field="email"
               if="email != null &amp;&amp; !email.contains('@')"
               error="El email debe tener el formato usuario@dominio.com"/>
        <check field="fechaFin"
               if="fechaInicio != null &amp;&amp; fechaFin != null &amp;&amp; fechaFin &lt; fechaInicio"
               error="La fecha de fin debe ser posterior a la fecha de inicio"/>
    </action-condition>

    <!-- *********************************************************************************************  -->
    <!-- ************************ Acciones básicas que cambian campos simples ************************  -->
    <!-- *********************************************************************************************  -->
    <!-- (action-record y action-attrs aquí si los hay) -->

    <!-- *********************************************************************************************  -->
    <!-- ************************** Acciones de llamadas Remotas al servidor *************************  -->
    <!-- *********************************************************************************************  -->
    <action-method name="subsysXxx.MiEntidad@Main-Remote-validateSave-action"
                   model="com.educaflow.subsystem.xxx.db.MiEntidad">
        <call class="com.educaflow.subsystem.xxx.controller.MiEntidadController"
              method="validateSave"/>
    </action-method>

</object-views>
```
