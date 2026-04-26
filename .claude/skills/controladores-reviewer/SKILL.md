---
name: controladores-reviewer
description: Revisa que los controladores Java de Axelor creados o modificados cumplen todas las reglas de /controladores-knowledge — estructura, inyecciones, firmas de método, AllowProperties, manejo de errores e imports.
---

# controladores-reviewer

## Propósito

Verificar que los controladores Java creados o modificados siguen las reglas definidas en `/controladores-knowledge`.

## Qué leer

1. El fichero Java del controlador a revisar.
2. Los ficheros XML de vistas que referencian ese controlador (los `<action-method>`).
3. El skill `/controladores-knowledge` para tener presentes todas las reglas.

## Estructura de la clase

- [ ] La clase NO tiene `@RequestScope`.
- [ ] La única inyección de campo es `@Inject private ModelServiceFactory modelServiceFactory`.
- [ ] No hay inyecciones directas de servicios con `@Inject MiServicio miServicio` — los servicios se resuelven dentro de cada método.

## Métodos públicos

- [ ] Cada método público lleva `@CallMethod`.
- [ ] Los métodos que escriben en base de datos llevan `@Transactional` (de `com.google.inject.persist`).
- [ ] Los métodos que solo leen datos NO llevan `@Transactional`.
- [ ] Ningún método contiene lógica de negocio (validaciones, cálculos, persistencia directa): solo obtiene el servicio y delega.

## Obtención del servicio

- [ ] Dentro de cada método se obtiene el servicio con `modelServiceFactory.resolve(MiEntidad.class)`.
- [ ] El resultado de `resolve` se castea al tipo de la interfaz del servicio.
- [ ] Cuando el servicio necesita un repositorio explícito, se usa `JpaRepository.of(MiEntidad.class)` y se pasa a `resolve(MiEntidad.class, repository)`.

## Uso de ActionRequestHelper

- [ ] En métodos que guardan datos, se usa `actionRequestHelper.getOriginalModel()` para obtener el estado original.
- [ ] En métodos que guardan datos, se usa `actionRequestHelper.getModel(allowProperties)` con un `AllowProperties` que lista solo los campos necesarios.
- [ ] Nunca se usa `getModel()` sin `AllowProperties` en métodos que persisten datos.

## Manejo de errores de negocio

- [ ] Los errores de negocio se muestran con `new ActionResponseHelper(actionResponse).doResponseBusinessMessagesAsError(businessMessages)` (modal) o `.doResponseBusinessMessages(businessMessages)` (inline).
- [ ] No existe ninguna llamada a `AxelorViewUtil.doResponseBusinessMessages(...)` — ese método está eliminado.
- [ ] Las excepciones inesperadas se relanzán como `RuntimeException`.

## Imports

- [ ] El import de `ActionRequestHelper` es `com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper`.
- [ ] El import de `ActionResponseHelper` es `com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper`.
- [ ] No hay imports legacy de rutas antiguas.

## Referencias en las vistas XML

- [ ] Cada método público del controlador que debe ser llamado desde una vista tiene su `<action-method>` correspondiente en el XML.
- [ ] El atributo `class` de cada `<action-method>` apunta a la clase Java correcta (FQCN completo).
- [ ] El atributo `method` coincide exactamente con el nombre del método Java.

## Checklist final

- [ ] No hay `@RequestScope` en la clase
- [ ] Solo `ModelServiceFactory` se inyecta; los servicios se resuelven en cada método
- [ ] Todos los métodos públicos llevan `@CallMethod`
- [ ] Los métodos que escriben en BD llevan `@Transactional`; los de solo lectura no
- [ ] `AllowProperties` se usa en todos los métodos que guardan datos
- [ ] Los errores de negocio usan `ActionResponseHelper` (nunca `AxelorViewUtil`)
- [ ] Los imports apuntan a `com.educaflow.base.infrastructure.axelorhelper.*`
- [ ] Los `<action-method>` en las vistas XML referencian correctamente clase y método

## Resultado

Si todos los checks del checklist final están bien, mostrar únicamente: **OK-No hay problemas**
