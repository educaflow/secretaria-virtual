# Validaciones (`V-XXX`)

## 1. ¿Qué es una validación?

Una **validación** (`V-XXX`) es una condición que un dato o un registro debe cumplir para que una operación sea aceptada. Si no se cumple, el sistema **impide** la operación y muestra un mensaje al usuario. Una validación nunca modifica el estado del sistema — solo bloquea.

Las validaciones se identifican con un código estable `V-XXX` que se asigna durante el análisis y se mantiene a lo largo del diseño y la implementación para garantizar la trazabilidad.

---

## 2. Cómo se documenta (durante el análisis)

Cada `V-XXX` se documenta en una tabla con la información mínima necesaria para implementarla. El analista identifica todas las validaciones aplicables a la entidad apoyándose en el catálogo de tipos de regla (ver `reference/validaciones.md`).

### 2.1 Tabla `V-XXX`

| ID    | Campo(s)  | Descripción                                      | Condición                   | Mensaje al usuario                                                     | Modelo XML            | Servidor (validate*)        | Cliente              |
|-------|-----------|--------------------------------------------------|-----------------------------|------------------------------------------------------------------------|-----------------------|-----------------------------|----------------------|
| V-001 | email     | Formato EMail                                    | Siempre                     | "El email debe tener el formato usuario@dominio.com"                   | —                     | `validateInsert/Update`     | `action-condition`   |
| V-002 | nif       | Formato NIF/DNI/NIE con dígito de control        | Siempre                     | "El NIF '{valor}' no es válido. Compruebe la letra verificadora"       | —                     | `validateInsert/Update`     | `action-condition`   |
| V-003 | fecha_fin | Debe ser mayor que fecha_inicio                  | Si fecha_inicio tiene valor | "La fecha de fin ({fin}) debe ser posterior a la de inicio ({inicio})" | —                     | `validateInsert/Update`     | `action-condition`   |
| V-004 | nif       | Unicidad                                         | Siempre                     | "Ya existe una persona con el NIF {valor}"                             | `unique="true"`       | —                           | —                    |
| V-005 | fecha_fin | Es requerida si fecha_inicio tiene valor         | Si fecha_inicio tiene valor | "La fecha de fin es requerida si existe la fecha de inicio"            | —                     | `validateInsert/Update`     | `requiredIf`         |
| V-006 | nombre    | Obligatorio siempre                              | Siempre                     | "El nombre es obligatorio"                                             | `required="true"`     | —                           | automático           |
| V-007 | cantidad  | Mínimo 1, máximo 999                             | Siempre                     | "La cantidad debe estar entre 1 y 999"                                 | `min="1" max="999"`   | —                           | `action-condition`   |

Las tres últimas columnas indican **en qué capa se implementa** cada validación. Una `V-XXX` va al menos en la capa **Modelo** o en la **Servidor** (principio "servidor es la fuente de verdad"); la capa **Cliente** es opcional y solo viable si la regla puede evaluarse sin BD.

### 2.2 Catálogo de tipos de validación

Para identificar todas las validaciones aplicables a un campo, recorrer el catálogo agrupado por ámbito en **`reference/validaciones.md`**:

- Validaciones **sobre el propio campo** (obligatorio, longitud, rango, formato, dígito de control, lista cerrada…).
- Validaciones **entre campos del mismo registro** (mayor/menor que, igual, distinto, mutuamente excluyentes, suma de líneas igual al total…).
- Validaciones **entre registros** (unicidad global o de ámbito, integridad referencial, cardinalidad de hijos, prerrequisitos…).
- Validaciones **de negocio** (operación no admitida según condición, requiere rol, ventana temporal, inmutabilidad por estado, transiciones de estado…).

### 2.3 Guías para redactar el mensaje

- Incluir el valor recibido y, si es posible, los valores válidos: *"El tipo de usuario 'Conseller' no es válido. Los posibles valores son 'Profesor' o 'Alumno'."*
- Empezar por el campo y el valor (no por "Error:"): *"El email debe tener el formato usuario@dominio.com"* en vez de *"Error: Formato inválido"*.
- No usar formato técnico sino cercano al usuario: *"El email debe tener el formato usuario@dominio.com"* en vez de *"El email debe cumplir `/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/`"*.
- Decir cómo debe ser, no cómo no debe ser: *"La fecha de fin (15/03/2024) debe ser posterior a la de inicio (20/03/2024)"* en vez de *"La fecha de fin no puede ser anterior a la de inicio"*.

---

## 3. Cómo se implementa

### 3.1 Principio fundamental: el servidor es la fuente de verdad

Toda validación se garantiza **siempre** en el servidor, en una de sus dos capas (Modelo XML o `validate*`). La capa Cliente es solo UX para feedback inmediato — si una validación solo está en cliente, no existe: la siguiente llamada por API la salta.

### 3.2 Las tres capas

| Capa             | Dónde vive                                                          | Para qué sirve                                                                                                                                     | Obligatoria              |
|------------------|---------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| **Modelo XML**   | Dominio Axelor (`*.xml` de entidad)                                 | Reglas declarativas simples sobre un campo o combinación de columnas (obligatorio, único, rango, longitud…).                                       | Sí (una de las dos)      |
| **Servidor**     | `validateInsert/Update/Remove` del `*ServiceImpl`                   | Todo lo que la capa Modelo no puede expresar: cruzadas entre campos, lookups en BD, dígitos de control, transiciones de estado, reglas de negocio. | Sí (una de las dos)      |
| **Cliente**      | XML de vista (atributos, `<action-condition>`, `<action-validate>`) | Solo UX: mostrar el error sin esperar al roundtrip. No sustituye al servidor.                                                                      | No                       |

### 3.3 Capa 1 — Modelo XML (declarativa)

Atributos del dominio Axelor que JPA/Hibernate aplica automáticamente al persistir:

| Atributo del modelo XML               | Aplica a                           | Regla que implementa                                                               |
|---------------------------------------|------------------------------------|------------------------------------------------------------------------------------|
| `required="true"`                     | cualquier campo                    | El campo es obligatorio siempre (no puede ser `null`).                             |
| `unique="true"`                       | cualquier campo                    | El valor del campo es único en toda la tabla.                                      |
| `<unique-constraint columns="A,B"/>`  | entidad                            | La combinación de columnas indicadas es única en la tabla.                         |
| `min="N"`                             | `<integer>`, `<decimal>`, `<long>` | Valor numérico mínimo permitido (inclusive).                                       |
| `max="N"`                             | `<integer>`, `<decimal>`, `<long>` | Valor numérico máximo permitido (inclusive).                                       |
| `min="N"`                             | `<string>`                         | Longitud mínima de la cadena.                                                      |
| `max="N"`                             | `<string>`                         | Longitud máxima de la cadena.                                                      |
| `precision="P"`                       | `<decimal>`                        | Número total de dígitos del decimal.                                               |
| `scale="S"`                           | `<decimal>`                        | Número de decimales del decimal.                                                   |
| `nullable="false"`                    | cualquier campo                    | Equivalente JPA a `required="true"` (no nulo a nivel BD).                          |

> Cuando un atributo del modelo cubre la regla, **no hace falta replicarla** en `validate*`. Solo replicarla en servidor si quieres un mensaje personalizado en lugar del genérico de JPA.
>
> El formulario hereda automáticamente estos atributos del modelo (p.ej. `required="true"` marca el campo como obligatorio en el formulario sin necesidad de declararlo en la vista).

### 3.4 Capa 2 — Servidor (`validate*`)

Tres métodos en el `*ServiceImpl`, uno por operación:

```java
Optional<BusinessMessages> validateInsert(T entity);
Optional<BusinessMessages> validateUpdate(T entity, T original);
Optional<BusinessMessages> validateRemove(T entity);
```

Devuelven `Optional<BusinessMessages>` y **nunca lanzan excepciones**. Son la fuente autorizada de "qué es válido".

> **Convención**: usar `BusinessMessages.isValid()` (semántico) para decidir si hay errores, no `isEmpty()`. Patrón canónico de retorno:
> ```java
> return messages.isValid() ? Optional.empty() : Optional.of(messages);
> ```

**Salvaguarda de `DefaultModelService`** — el padre que extienden todos los `*ServiceImpl` invoca automáticamente estos métodos antes de cada operación:

```java
@Override
public T insert(T entity) {
    this.validateInsert(entity).ifPresent(this::throwIfInvalid);   // ← salvaguarda
    return repository.save(entity);
}

@Override
public T update(T entity, T original) {
    this.validateUpdate(entity, original).ifPresent(this::throwIfInvalid);
    return repository.save(entity);
}

@Override
public void remove(T entity) {
    this.validateRemove(entity).ifPresent(this::throwIfInvalid);
    repository.remove(entity);
}
```

Si una validación detecta un problema en este punto, `throwIfInvalid` lanza una `ValidationException` y la operación se aborta. **En condiciones normales esta salvaguarda nunca debería dispararse**, porque las vistas ya han llamado a los mismos métodos `validate*` antes de pedir el `save`/`delete`. Solo actúa si algo evita el flujo normal (script externo, llamada por API directa, integración batch, etc.).

> **Importante** — esta salvaguarda automática **solo** existe mientras **no** sobrescribas `insert`/`update`/`remove` en tu `*ServiceImpl`. En cuanto los sobrescribes (para añadir reglas de negocio, decorar el bean…), **MUST NOT** llamar a `super.insert/update/remove`: persistes con `repository.save/remove` y **eres tú** quien pone `validateXxx(...).ifPresent(throwIfInvalid)` como primera línea. Ver `[[k-sistemas]]` §"Persistir: siempre `repository`, nunca `super.*`".

### 3.5 Capa 3 — Cliente (opcional)

Solo UX. Duplica validaciones del servidor para que el usuario vea el error sin esperar al roundtrip.

**Cuándo duplicar en cliente local:**

| Tipo de validación                              | Servidor (obligatorio) | Duplicar en cliente local |
|-------------------------------------------------|------------------------|---------------------------|
| Campo obligatorio o de formato (sin BD)         | Sí                     | Recomendado               |
| Comparación entre campos del mismo registro     | Sí                     | Recomendado               |
| Unicidad / integridad referencial (requiere BD) | Sí                     | No es posible             |
| Reglas de negocio con consulta a BD             | Sí                     | No es posible             |

**Mecanismos disponibles en cliente:**

- Atributos del campo en el formulario: `required`, `requiredIf`, `readonlyIf`, `showIf`.
- `<action-condition>` con `<check>` — error pegado a un campo concreto.
- `<action-validate>` con `<error>` / `<alert>` / `<info>` / `<notify>` — diálogos a nivel de formulario.

**Cómo elegir el patrón:**

| Necesidad                                             | Patrón                                        |
|-------------------------------------------------------|-----------------------------------------------|
| Error pegado a un campo concreto                      | `<action-condition>` (P1–P6)                  |
| El error es de "todo el formulario"                   | `<action-validate>` + `<error>` (P7–P11)      |
| Confirmación del usuario, puede cancelar              | `<action-validate>` + `<alert>` (P12–P14)     |
| Solo informar y seguir                                | `<action-validate>` + `<info>` (P15)          |
| Notificación discreta en esquina                      | `<action-validate>` + `<notify>` (P16)        |
| Tras el error hay que limpiar el campo                | añadir `action=` al `<error>` (P17)           |
| Varios campos obligatorios a la vez                   | varios `<check>` en `<action-condition>` (P6) |
| Una regla con varias condiciones independientes       | varios `<error>` en `<action-validate>` (P7)  |
| Comparar con padre en `panel-related`                 | `__parent__.campo` (P4)                       |
| Comparar con la fecha del servidor                    | `__config__.date` (P5)                        |
| Usar el grupo del usuario actual                      | `__user__.group` (P11)                        |
| Interpolar valores del registro en el mensaje         | `${campo}` (P14)                              |

→ Ver **`examples/ejemplos-validaciones.md`** para el catálogo completo de los 17 patrones (P1–P17) con XML adaptable.

**Combinar varios patrones a la vez** — si una entidad necesita a la vez errores pegados a campos (P1–P6) y errores generales del formulario (P7–P11), no se pueden meter en el mismo elemento (gramáticas distintas). En ese caso `Local-validateSave-action` pasa a ser un `<action-group>` que encadena un `<action-condition>` y un `<action-validate>`:

```xml
<action-group name="subsysXxx.MiEntidad@Main-Local-validateSave-action">
    <action name="subsysXxx.MiEntidad@Main-Local-validateSave-requiredFields-action"/>
    <action name="subsysXxx.MiEntidad@Main-Local-validateSave-dateRange-action"/>
</action-group>
```

El detalle de cuándo aplicar este patrón compuesto está en `k-vistas/actions.md` → sección "Patrón: `Local-validateXXX-action` directo o como `<action-group>`".

### 3.6 Flujo completo: el patrón `action-group` Local → Remote → save

El botón Guardar (o el `onSave` del formulario) dispara un `<action-group>` que encadena las tres etapas en este orden fijo:

```xml
<action-group name="subsysXxx.MiEntidad@Main-btnSave-action">
    <action name="subsysXxx.MiEntidad@Main-Local-validateSave-action"/>    <!-- 1. cliente XML -->
    <action name="subsysXxx.MiEntidad@Main-Remote-validateSave-action"/>   <!-- 2. servidor por controlador -->
    <action name="save"/>                                                   <!-- 3. persiste -->
</action-group>
```

Si la etapa 1 emite un `error`, la 2 y la 3 no se ejecutan. Si la 2 emite un error, la 3 no se ejecuta. La etapa 3 (`save`) volverá a pasar por la salvaguarda de `DefaultModelService`, así que **no se puede colar nada que la validación no apruebe**.

El paso 2 (validación remota) **siempre está presente**: invoca `validateInsert`/`validateUpdate` del servicio sin guardar todavía y permite mostrar errores que dependen de la BD antes de pulsar `save`. La única decisión opcional es la del paso 1: ¿duplico la validación también en cliente local para no esperar al roundtrip?

Para el botón Borrar el patrón es análogo:

```xml
<action-group name="subsysXxx.MiEntidad@Main-btnDelete-action">
    <action name="subsysXxx.MiEntidad@Main-Local-validateDelete-action"/>
    <action name="subsysXxx.MiEntidad@Main-Remote-validateDelete-action"/>
    <action name="delete"/>
</action-group>
```

Y para operaciones custom (`btnAprobar`, `btnRechazar`…) la etapa 3 también es remota porque el método del servicio no es `save`/`delete` genérico:

```xml
<action-group name="subsysXxx.MiEntidad@Main-btnAprobar-action">
    <action name="subsysXxx.MiEntidad@Main-Local-validateAprobar-action"/>
    <action name="subsysXxx.MiEntidad@Main-Remote-validateAprobar-action"/>
    <action name="subsysXxx.MiEntidad@Main-Remote-aprobar-action"/>
</action-group>
```

### 3.7 El controlador: puente entre la vista y el servicio

El `<action-method>` llama a un método `@CallMethod` del controlador, cuyo único trabajo es **decidir si es insert o update y delegar al servicio**. No contiene lógica de validación:

```java
@CallMethod
public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
    final MiEntidadService service = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class);

    ActionRequestHelper<MiEntidad> requestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
    ActionResponseHelper responseHelper = new ActionResponseHelper(actionResponse);

    Optional<BusinessMessages> result;
    if (requestHelper.getId() == null) {
        MiEntidad entidad = requestHelper.getModel(service.allowPropertiesInsert());
        result = service.validateInsert(entidad);
    } else {
        MiEntidad entidad = requestHelper.getModel(service.allowPropertiesUpdate());
        MiEntidad original = requestHelper.getOriginalModel();
        result = service.validateUpdate(entidad, original);
    }
    if (result.isPresent()) {
        responseHelper.doResponseBusinessMessagesAsError(result.get());
    }
}
```

El `action-method` correspondiente en XML:

```xml
<action-method name="subsysXxx.MiEntidad@Main-Remote-validateSave-action" model="com.educaflow.subsystem.xxx.db.MiEntidad">
    <call class="com.educaflow.subsystem.xxx.controller.MiEntidadController" method="validateSave"/>
</action-method>
```

---

## 4. Trazabilidad: del análisis al diseño

- Cada `V-XXX` del análisis aparece en al menos un paso del diseño.
- Una `V-XXX` con varias capas marcadas en la tabla (p.ej. `Modelo XML` + `Cliente`, o `Servidor` + `Cliente`) aparece en **varios pasos del diseño** — uno por capa.
- Cada paso del diseño que implementa validaciones lista qué `V-XXX` cubre y en qué capa. Ejemplos:
  - *"Paso 3 — `MiEntidad.xml` (dominio). Cubre V-004 (`unique="true"`) y V-006 (`required="true"`)."*
  - *"Paso 5 — `MiEntidadServiceImpl.validateInsert/Update`. Cubre V-002, V-003."*
  - *"Paso 7 — `MiEntidad.xml` (vista), `<action-condition>` `Local-validateSave-...`. Cubre V-001, V-002, V-003."*
- Antes de cerrar el diseño, construir la matriz `V-XXX → paso(s)`. Ninguna fila puede quedar vacía y cada capa marcada en la tabla `V-XXX` debe tener su paso correspondiente.

---

## 5. Referencias

- `reference/validaciones.md` — catálogo de tipos de validación por ámbito (campo propio, mismo registro, entre registros, de negocio).
- `examples/ejemplos-validaciones.md` — catálogo de 17 patrones XML cliente (P1–P17) listos para copiar y adaptar.
- `k-sistemas/modelos.md` — sintaxis completa del dominio XML.
- `k-sistemas/servicios.md` — estructura del servicio, `BusinessMessages`, descubrimiento por `ModelServiceFactory`.
- `k-sistemas/controladores.md` — estructura del controlador, `@CallMethod`, helpers (`ActionRequestHelper`, `ActionResponseHelper`, `AllowProperties`).
- `k-vistas/actions.md` — sintaxis completa de `<action-validate>`, `<action-condition>`, `<action-method>`, `<action-group>`.