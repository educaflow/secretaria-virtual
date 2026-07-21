# Validaciones (`VAL-`)

Una validación (`VAL-`) es una comprobación bloqueante anclada a **una acción** de la entidad: si falla, la operación se cancela. Qué es y cómo se clasifica frente a una restricción o una regla de negocio lo define la spec; aquí solo **cómo se implementa**.

> Una comprobación que debe cumplirse en **toda** operación no es una `VAL-` sino una **restricción** (`RES-`), que vive en el modelo XML: ver [`restricciones.md`](restricciones.md).

---

## 1. Principio fundamental: el servidor es la fuente de verdad

Toda `VAL-` se garantiza **siempre** en el servidor (`validate*`). La capa cliente es solo UX para feedback inmediato — si una validación solo está en cliente, no existe: la siguiente llamada por API la salta.

| Capa | Dónde vive | Para qué sirve | Obligatoria |
|---|---|---|---|
| **Servidor** | `validateInsert/Update/Remove` del `*ServiceImpl` | Cruzadas entre campos, lookups en BD, dígitos de control, transiciones de estado, reglas con consulta. | **Sí** |
| **Cliente** | XML de vista (atributos, `<action-condition>`, `<action-validate>`) | Solo UX: mostrar el error sin esperar al roundtrip. No sustituye al servidor. | No |

---

## 2. Capa servidor — `validate*`

Tres métodos en el `*ServiceImpl`, uno por operación:

```java
Optional<BusinessMessages> validateInsert(T entity);
Optional<BusinessMessages> validateUpdate(T entity, T original);
Optional<BusinessMessages> validateRemove(T entity);
```

Devuelven `Optional<BusinessMessages>` y **nunca lanzan excepciones**. Son la fuente autorizada de "qué es válido".

> **Convención**: usa `BusinessMessages.isValid()` (semántico) para decidir si hay errores, no `isEmpty()`. Patrón canónico de retorno:
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

Si una validación detecta un problema, `throwIfInvalid` lanza una `ValidationException` y la operación se aborta. **En condiciones normales esta salvaguarda nunca debería dispararse**, porque las vistas ya han llamado a los mismos `validate*` antes de pedir el `save`/`delete`. Solo actúa si algo evita el flujo normal (script externo, llamada por API directa, integración batch).

> Una `VAL-` con `actor:` en la spec (la operación requiere un rol) también se comprueba en `validate*`, consultando el usuario/rol actual: ocultar el botón en la vista es solo UX, no autorización.

> **Maestro-detalle**: los `validate*` de un **detalle** de composición también los ejecuta la plataforma al guardar el **maestro** — `ModelServiceValidationWalker` (clase de `com.axelor.db.modelservice` en la versión propia de AOP, invocada desde `Resource.save`/`remove`) recorre los detalles recursivamente y llama a su `validateInsert`/`validateUpdate`/`validateRemove`. Las validaciones del detalle se escriben una vez, en el servicio del detalle. Si un `validate*` del detalle depende de campos que fija el maestro al guardarse, protégelo con `if (padre.getId() == null) return Optional.empty();`.

> **Importante** — esta salvaguarda automática **solo** existe mientras **no** sobrescribas `insert`/`update`/`remove`. En cuanto los sobrescribes (para añadir reglas de negocio…), **MUST NOT** llamar a `super.insert/update/remove`: persistes con `repository.save/remove` y **eres tú** quien pone `validateXxx(...).ifPresent(throwIfInvalid)` como primera línea. Ver `[[k-sistemas]]` §"Persistir: siempre `repository`, nunca `super.*`".

---

## 3. Capa cliente (opcional — salvo en el form modal de un detalle)

Solo UX. Duplica validaciones del servidor para que el usuario vea el error sin esperar al roundtrip.

**Cuándo duplicar en cliente local:**

| Tipo de validación                              | Servidor (obligatorio) | Duplicar en cliente local |
|-------------------------------------------------|------------------------|---------------------------|
| Campo obligatorio o de formato (sin BD)         | Sí                     | Recomendado               |
| Comparación entre campos del mismo registro     | Sí                     | Recomendado               |
| Unicidad / integridad referencial (requiere BD) | Sí                     | No es posible             |
| Reglas de negocio con consulta a BD             | Sí                     | No es posible             |

**Excepción — form modal de un detalle (maestro-detalle): la capa cliente deja de ser opcional.** `save-modal`/`delete-modal` no llaman al servidor y **MUST NOT** usarse `remote-validation*` en el modal (el maestro puede no existir en BD y el `validate*` del detalle fallaría espuriamente); las validaciones de servidor del detalle solo corren al guardar el maestro (`ModelServiceValidationWalker`). Por tanto en el form modal el `Local-validate*` **MUST** duplicar **todas** las validaciones del detalle evaluables en cliente — es la única forma de avisar al usuario antes de cerrar el modal, en vez de con un error del maestro al guardar al final. Ver `k-vistas/forms.md` §"Form modal".

**Mecanismos disponibles en cliente:**

- Atributos del campo en el formulario: `required`, `requiredIf`, `readonlyIf`, `showIf`.
- `<action-condition>` con `<check>` — error pegado a un campo concreto.
- `<action-validate>` con `<error>` / `<alert>` / `<info>` / `<notify>` — diálogos a nivel de formulario.

**Cómo elegir el patrón:**

| Necesidad | Patrón |
|---|---|
| Error pegado a un campo concreto | `<action-condition>` (P1–P6) |
| El error es de "todo el formulario" | `<action-validate>` + `<error>` (P7–P11) |
| Confirmación del usuario, puede cancelar | `<action-validate>` + `<alert>` (P12–P14) |
| Solo informar y seguir | `<action-validate>` + `<info>` (P15) |
| Notificación discreta en esquina | `<action-validate>` + `<notify>` (P16) |
| Tras el error hay que limpiar el campo | añadir `action=` al `<error>` (P17) |
| Varios campos obligatorios a la vez | varios `<check>` en `<action-condition>` (P6) |
| Una regla con varias condiciones independientes | varios `<error>` en `<action-validate>` (P7) |
| Comparar con padre en `panel-related` | `__parent__.campo` (P4) |
| Comparar con la fecha del servidor | `__config__.date` (P5) |
| Usar el grupo del usuario actual | `__user__.group` (P11) |
| Interpolar valores del registro en el mensaje | `${campo}` (P14) |

→ Catálogo completo de los 17 patrones (P1–P17) con XML adaptable: [`examples/ejemplos-validaciones.md`](examples/ejemplos-validaciones.md).

**Combinar varios patrones a la vez** — si una entidad necesita a la vez errores pegados a campos (P1–P6) y errores generales del formulario (P7–P11), no se pueden meter en el mismo elemento (gramáticas distintas). En ese caso `Local-validateSave-action` pasa a ser un `<action-group>` que encadena un `<action-condition>` y un `<action-validate>`:

```xml
<action-group name="subsysXxx.Main@MiEntidad-Local-validateSave-action">
    <action name="subsysXxx.Main@MiEntidad-Local-validateSave-requiredFields-action"/>
    <action name="subsysXxx.Main@MiEntidad-Local-validateSave-dateRange-action"/>
</action-group>
```

El detalle de cuándo aplicar este patrón compuesto está en `k-vistas/actions.md` → sección "Patrón: `Local-validateXXX-action` directo o como `<action-group>`".

---

## 4. Flujo completo: el patrón `action-group` Local → Remote → save

El botón Guardar dispara un `<action-group>` que encadena las tres etapas en este orden fijo:

```xml
<action-group name="subsysXxx.Main@MiEntidad-btnSave-action">
    <action name="subsysXxx.Main@MiEntidad-Local-validateSave-action"/>    <!-- 1. cliente XML (opcional) -->
    <action name="remote-validationSave-action"/>                          <!-- 2. servidor (acción GLOBAL) -->
    <action name="save"/>                                                  <!-- 3. persiste -->
</action-group>
```

Si la etapa 1 emite un `error`, la 2 y la 3 no se ejecutan. Si la 2 emite un error, la 3 no se ejecuta. La etapa 3 (`save`) volverá a pasar por la salvaguarda de `DefaultModelService`, así que **no se puede colar nada que la validación no apruebe**.

El paso 2 (validación remota) **siempre está presente en el form principal** y es la acción **global** `remote-validationSave-action` (§5): invoca `validateInsert`/`validateUpdate` del servicio sin guardar todavía y permite mostrar errores que dependen de la BD antes de pulsar `save`. La única decisión opcional es la del paso 1: ¿duplico la validación también en cliente local para no esperar al roundtrip? En el form **modal** de un detalle el paso 2 **no existe** y el paso 1 pasa a ser obligatorio y lo más completo posible (§3, §5).

Para el botón Borrar el patrón es análogo, con la acción global `remote-validationDelete-action`:

```xml
<action-group name="subsysXxx.Main@MiEntidad-btnDelete-action">
    <action name="remote-validationDelete-action"/>
    <action name="delete"/>
</action-group>
```

Solo para operaciones custom (`btnAprobar`, `btnRechazar`…) hay acciones remotas **por entidad**, porque el método del servicio no es `save`/`delete` genérico:

```xml
<action-group name="subsysXxx.Main@MiEntidad-btnAprobar-action">
    <action name="subsysXxx.Main@MiEntidad-Local-validateAprobar-action"/>
    <action name="subsysXxx.Main@MiEntidad-Remote-validateAprobar-action"/>
    <action name="subsysXxx.Main@MiEntidad-Remote-aprobar-action"/>
</action-group>
```

---

## 5. La validación remota: acciones globales de `DefaultModelController`

La validación remota de `save`/`delete` **no se crea por entidad**: existen dos acciones **globales**, definidas una única vez en `src/main/java/com/educaflow/base/infrastructure/controller/DefaultModelController.xml`, válidas para cualquier formulario:

| Acción global | Qué invoca | Se usa antes de |
|---|---|---|
| `remote-validationSave-action` | `validateInsert` (registro sin `id`) o `validateUpdate` (con `id`) | `save` (form principal) |
| `remote-validationDelete-action` | `validateRemove` | `delete` (form principal) |

> **MUST NOT** usarse en el form **modal** de un detalle (`save-modal`/`delete-modal`): el maestro puede no existir todavía en BD. Allí la validación previa al cierre es solo cliente (§3) y la de servidor llega al guardar el maestro (`ModelServiceValidationWalker`).

Funcionan para cualquier entidad porque `DefaultModelController` resuelve la clase del modelo por el `_model` del contexto del request (por eso las acciones no llevan atributo `model`), obtiene su `ModelService` vía `ModelServiceFactory`, construye el bean con el `allowProperties*` de la operación y llama al `validate*` correspondiente — sin persistir nada.

- **MUST NOT** crear métodos `validateSave`/`validateDelete` en el controlador de una entidad ni `<action-method>` de validación por vista para `save`/`delete`: las dos acciones globales los sustituyen.
- Las **operaciones custom** (`aprobar`, `rechazar`…) sí llevan su `@CallMethod` en el controlador propio de la entidad (`Remote-validate<Operacion>-action` + `Remote-<operacion>-action`), siguiendo `k-sistemas/controladores.md`: resolver el servicio, construir el bean con el `allowProperties` de esa acción y delegar en el `validate<Operacion>`/`<operacion>` del servicio.

- ✅ CORRECTO: `<action name="remote-validationSave-action"/>` en el `action-group` de `btnSave` de cualquier entidad.
- ❌ INCORRECTO: `<action-method name="subsysXxx.Main@MiEntidad-Remote-validateSave-action">` que llama a `MiEntidadController.validateSave` (patrón sustituido por la acción global; duplica `DefaultModelController`).

---

## 6. Guías para redactar el mensaje

El `mensaje` de cada `VAL-` lo fija la spec, pero al materializarlo respeta estas guías:

- Incluir el valor recibido y, si es posible, los valores válidos: *"El tipo de usuario 'Conseller' no es válido. Los posibles valores son 'Profesor' o 'Alumno'."*
- Empezar por el campo y el valor (no por "Error:"): *"El email debe tener el formato usuario@dominio.com"* en vez de *"Error: Formato inválido"*.
- No usar formato técnico sino cercano al usuario: *"El email debe tener el formato usuario@dominio.com"* en vez de *"El email debe cumplir `/^[a-zA-Z0-9._%+-]+@…$/`"*.
- Decir cómo debe ser, no cómo no debe ser: *"La fecha de fin (15/03/2024) debe ser posterior a la de inicio (20/03/2024)"* en vez de *"La fecha de fin no puede ser anterior a la de inicio"*.

---

## 7. Referencias

- [`restricciones.md`](restricciones.md) — restricciones (`RES-`) en el modelo XML; cuándo una comprobación es `RES-` y no `VAL-`.
- [`examples/ejemplos-validaciones.md`](examples/ejemplos-validaciones.md) — 17 patrones XML cliente (P1–P17).
- [`../k-sistemas/references/models.md`](../k-sistemas/references/models.md) — sintaxis del modelo XML del dominio.
- `k-sistemas/servicios.md` — estructura del servicio, `BusinessMessages`, descubrimiento por `ModelServiceFactory`.
- `k-sistemas/controladores.md` — estructura del controlador, `@CallMethod`, helpers (`ActionRequestHelper`, `ActionResponseHelper`, `AllowProperties`).
- `k-vistas/actions.md` — sintaxis completa de `<action-validate>`, `<action-condition>`, `<action-method>`, `<action-group>`.
