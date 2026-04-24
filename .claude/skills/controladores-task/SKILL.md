---
name: controladores-task
description: Skill para crear o corregir controladores de Axelor. El controlador expone métodos con @CallMethod a las vistas Axelor y delega toda la lógica de negocio en los servicios.
---

# controladores-task

## Controladores de Axelor: diseño, generación y corrección
- Este skill sirve para diseñar, generar o corregir controladores Java de Axelor.
- Los controladores se crean a partir de las necesidades de las vistas (acciones, botones, eventos de formulario).
- Se siguen las normas definidas en el skill `/controladores-knowledge`

## Tareas a realizar

- Analizar qué acciones de las vistas necesitan un método en el controlador (botones, onSave, onChange, onLoad, etc.)
- Para cada método decidir:
  - Su nombre siguiendo las convenciones Java (camelCase, verbo + sustantivo)
  - El tipo de firma: `type1` (ActionRequest/ActionResponse), `type2` (parámetros Java + Response) o `type3` (parámetros Java + valor de retorno)
  - Si necesita `@Transactional` (solo si escribe en base de datos)
  - Qué campos se permiten copiar desde la request con `AllowProperties` (solo en métodos que guardan datos)
  - A qué servicio o servicios llama para ejecutar la lógica de negocio
  - Qué señal o respuesta envía al cliente (`setSignal("back")`, `setValue(...)`, `setFlash(...)`, etc.)
- Decidir el paquete donde se ubicará el controlador: `com.educaflow.{layer}.{subsistema}.controllers`

## En caso de tener que crear el controlador

- Crear la clase Java **sin** `@RequestScope`
- Inyectar `ModelServiceFactory` con `@Inject` (es lo único que se inyecta para obtener servicios que heredan de `ModelService`)
- En cada método que necesite un servicio, resolverlo con `modelServiceFactory.resolve(MiEntidad.class)`
- Crear cada método con `@CallMethod` y la firma decidida
- En métodos que guardan datos:
  - Añadir `@Transactional`
  - Usar `ActionRequestHelper.getOriginalModel()` para el estado original
  - Usar `ActionRequestHelper.getModel(allowProperties)` con `AllowProperties` para los campos permitidos
- Para mostrar errores de negocio:
  - Usar `new ActionResponseHelper(actionResponse).doResponseBusinessMessagesAsError(businessMessages)` para errores modales
  - Usar `new ActionResponseHelper(actionResponse).doResponseBusinessMessages(businessMessages)` para errores inline
- Relanzar cualquier otra excepción como `RuntimeException`
- Crear o actualizar los `<action-method>` en las vistas XML para referenciar el controlador y sus métodos

## Si el controlador ya existe pero hay que corregirlo

- Eliminar cualquier uso de `AxelorViewUtil.doResponseBusinessMessages(...)` — ese método ya no existe
- Sustituirlo por `new ActionResponseHelper(actionResponse).doResponseBusinessMessagesAsError(...)` o `.doResponseBusinessMessages(...)`
- Eliminar inyecciones directas de servicios con `@Inject MiServicio miServicio` y reemplazarlas por `@Inject ModelServiceFactory modelServiceFactory` + resolución en cada método
- Corregir imports de `ActionRequestHelper` a `com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper`
- Añadir import de `ActionResponseHelper` en `com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper`
- Corregir la firma del método (tipo, parámetros, anotaciones) según lo decidido
- Corregir el uso de `AllowProperties` para que solo permita los campos necesarios
- Corregir el manejo de errores según lo descrito arriba
- Corregir las referencias en los `<action-method>` de las vistas XML

## Revisión
- [ ] Revisar que todos los métodos públicos del controlador llevan `@CallMethod`
- [ ] Revisar que ningún método contiene lógica de negocio (validaciones, cálculos, persistencia): solo llama a servicios
- [ ] Revisar que el controlador inyecta `ModelServiceFactory` y no servicios directamente
- [ ] Revisar que los métodos que escriben en BD llevan `@Transactional` y los que no, no lo llevan
- [ ] Revisar que los métodos que guardan datos usan `AllowProperties` con los campos exactos necesarios
- [ ] Revisar que NO hay ninguna llamada a `AxelorViewUtil.doResponseBusinessMessages(...)` (método legacy eliminado)
- [ ] Revisar que los errores de negocio se muestran con `ActionResponseHelper.doResponseBusinessMessagesAsError(...)` o `.doResponseBusinessMessages(...)`
- [ ] Revisar que los imports son los correctos: `com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper` y `ActionResponseHelper`
- [ ] Revisar que las señales de respuesta (`setSignal`, `setValue`, `setFlash`) son correctas para cada caso
- [ ] Revisar que los `<action-method>` en las vistas XML apuntan al controlador y método correctos