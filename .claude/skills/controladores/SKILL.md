---
name: controladores
description: Skill para crear controladores de Axelor. El controlador expone métodos a las vistas Axelor.
---

# Guia para desarrollar Controladores en Axelor

**NOTA: Aunque vamos a usar ejemplos de Systemas, todo lo explicado aquí es aplicable a cualquier subsistema.**

Un controlador es un fichero Java que expone métodos públicos con `@CallMethod` para ser llamados desde las vistas Axelor desde un `<action-method>` en XML. El controlador es el punto de entrada desde las vistas Axelor a la lógica de negocio implementada en los servicios. El controlador recibe un `ActionRequest` con toda la información enviada por el cliente (contexto del formulario, filtros, campos, etc.) y un `ActionResponse` para configurar la respuesta que se enviará al cliente (p.ej. cerrar el formulario, mostrar mensajes, actualizar campos, etc.). El controlador debe llamar a los servicios para realizar la lógica de negocio y manejar cualquier error de negocio lanzado por los servicios para mostrarlo correctamente en la vista.

**El controlador no ejecuta lógica de negocio, solo es el punto de entrada desde las vistas y el encargado de llamar a los servicios. Toda la lógica de negocio (validaciones, cálculos, persistencia, etc.) debe estar en los servicios.**

Referencias:
 * La referencia a los métodos ActionRequest están en [ActionRequest](references/ActionRequest.md)
 * La referencia a los métodos ActionResponse están en [ActionResponse](references.ActionResponse.md)
 * La referencia a como llamar al controlador desde las vistas Axelor con `<action-method>` está en [ActionMethod](references/action-method.md)
 * La referencia a la estructura de un controlador está en [Controller](references/controller.md)

## Lista de tareas al desarrollar un controlador
Deberás hacer lo siguiente
1. Crear la clase Java del controlador.
2. Pensar, analizar y crear los métodos del controlador
3. Decidir si cada método usa `ActionRequest`/`ActionResponse` o parámetros de entrada "normales" de Java y que retornan un `Response` o un valor concreto (p.ej. `String`, `boolean`, etc.)
4. Decidir cuando usar `ActionRequestHelper` para simplificar el código (deberás analizar la clase `ActionRequestHelper` para ver que hace y cuando usarla en lugar de trabajar directamente con `ActionRequest`)
5. Decidir a que servicios llamar desde el controlador para realizar la lógica de negocio 
6. Implementar las llamadas al controlador con `<action-method>` desde las vistas Axelor (XML) para que el controlador se ejecute en los momentos necesarios (p.ej. al guardar un formulario, al cambiar un campo, etc.)
8. Crear casos de prueba para el controlador, incluyendo casos de éxito y casos de error 


## Estructura del controlador

Existen 3 tipos de métodos en un controlador, cada uno con su propia estructura y uso recomendado:

```java
@RequestScope 
public class HelloController {
  @CallMethod 
  public void type1(ActionRequest request, ActionResponse response) {
    Contact contact = request.getContext().asType(Contact.class);
    response.setFlash("Hello " + contact.getName()); 
  }

  @CallMethod 
  public Response type2(String email) { 
    Response response = new ActionResponse();
    response.addError("email", "Email required");
    return response;
  }

  @CallMethod
  public int type3(int a, int b) { 
    return a+b;
  } 
  
}
```
 * **type1**: método con `ActionRequest` y `ActionResponse`. Es el tipo más común de método en controladores, recomendado para acciones que necesitan acceder al contexto del formulario, a los datos enviados por el cliente, o que necesitan configurar una respuesta compleja (p.ej. cerrar el formulario, mostrar mensajes, actualizar campos, etc.)
 * **type2**: método con parámetros de entrada "normales" de Java y que retorna un `Response`. Es recomendado para acciones que no necesitan acceder al contexto del formulario ni a los datos enviados por el cliente, pero que necesitan retornar un `Response` para mostrar mensajes de error o éxito en la vista.
 * **type3**: método con parámetros de entrada "normales" de Java y que retorna un valor concreto (p.ej. `String`, `boolean`, etc.). Es recomendado para acciones que no necesitan acceder al contexto del formulario ni a los datos enviados por el cliente, y que solo necesitan retornar un valor concreto para actualizar un campo en la vista sin necesidad de recargar toda la entidad.


## ActionRequestHelper

La clase `ActionRequestHelper` es una utilidad que simplifica el trabajo con `ActionRequest` en los controladores. 
Permite obtener la entidad original antes de las modificaciones del usuario, y obtener la entidad modificada con solo los campos permitidos copiados desde la request, evitando tener que escribir código manual para copiar cada campo y manejar las relaciones. 
Es especialmente útil en métodos de controlador que guardan datos en la base de datos, ya que permite obtener la entidad modificada de forma segura sin riesgo de sobrescribir campos no permitidos o perder datos de relaciones.  

```java
package com.educaflow.system.NombreSystema.controllers;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.ActionRequestHelper;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.base.util.AxelorViewUtil;
import com.educaflow.subsystem.SUBSYSTEM.db.MiEntidad;
import com.educaflow.subsystem.SUBSYSTEM.db.repo.MiEntidadRepository;
import com.educaflow.subsystem.SUBSYSTEM.service.MiEntidadService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Map;

public class MiEntidadController {
    
    @Inject
    MiEntidadService miEntidadService;

    @CallMethod
    @Transactional
    public void update(ActionRequest actionRequest, ActionResponse actionResponse) {
        try {
            ActionRequestHelper<MiEntidad> actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
            MiEntidad entidadOriginal = actionRequestHelper.getOriginalModel();
            AllowProperties allowProperties = AllowProperties.createAllowProperties(
                Map.of(
                    "campoSimple", Map.of(),                                    // campo escalar
                    "coleccion", Map.of("subcampo", Map.of())                   // colección con sub-campo
                )
            );
            MiEntidad entidad = actionRequestHelper.getModel(allowProperties);

            miEntidadService.update(entidad, entidadOriginal);

            actionResponse.setSignal("back", null);
        } catch (BusinessException ex) {
            AxelorViewUtil.doResponseBusinessMessages(actionResponse, ex.getBusinessMessages());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
```

### Reglas del controlador

- `@CallMethod` — obligatorio en cada método público que llame una vista.
- `@Transactional` — obligatorio en métodos que escriben en base de datos.
- `ActionRequestHelper.getOriginalModel()` — obtiene el estado original de la entidad antes de las modificaciones del usuario.
- `ActionRequestHelper.getModel(allowProperties)` — obtiene la entidad con solo los campos permitidos copiados desde la request. **Nunca** usar `getModel()` sin `AllowProperties` en métodos que guardan datos.
- `AllowProperties.createAllowProperties(Map.of(...))` — define qué campos (y sub-campos) se pueden copiar. La clave es el nombre del campo; el valor es un `Map` con sus sub-campos (vacío `Map.of()` si es un campo simple o una relación entera). Para colecciones con sub-campos: `Map.of("coleccion", Map.of("subcampo", Map.of()))`.
- `actionResponse.setSignal("back", null)` — cierra el formulario y vuelve al grid tras guardar con éxito.
- `AxelorViewUtil.doResponseBusinessMessages(...)` — convierte `BusinessException` en errores visibles en la vista.
- Errores no esperados se relanzán como `RuntimeException` — Axelor los mostrará como error genérico.

### Retorno de valores

En caso de que el controlador necesite devolver un valor a la vista (p.ej. para actualizar un campo calculado sin recargar toda la entidad), se puede usar `actionResponse.setValue("nombreCampo", valor)` para actualizar el valor de un campo específico en la vista sin necesidad de recargar toda la entidad. En este caso, el método del controlador no necesita ser `@Transactional` si no se están guardando datos en la base de datos.

```java
@CallMethod
public void prepararAlgo(ActionRequest actionRequest, ActionResponse actionResponse) {
    ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
    MiEntidad entidad = miEntidadRepository.find(actionRequestHelper.getId());
    
    actionResponse.setValue("campo", valor);
}
```








## Checklist de tareas de desarrollo de controladores
Deberás comprobasr antes de terminar
- [ ] Que has creado el controlador con métodos que llevan la anotación `@CallMethod`
- [ ] Que los métodos del controlador llaman a los servicios para realizar la lógica de negocio, y que no contienen lógica de negocio por sí mismos (solo llaman a los servicios).
- [ ] Que los métodos del controlador manejan los errores de negocio lanzados por los servicios (capturando `BusinessException` y usando `AxelorViewUtil.doResponseBusinessMessages(...)` para mostrar los errores en la vista) y relanzan cualquier otro error como `RuntimeException` para que Axelor lo maneje como error genérico.
- [ ] Que en el controlador, los métodos que escriben en BD llevan `@Transactional` y los que no escriben no lo llevan.
- [ ] Que en el controlador, si existe el ActionRequest usas `ActionRequestHelper.getOriginalModel()` para obtener el estado original de la entidad y `ActionRequestHelper.getModel(allowProperties)` con `AllowProperties` para obtener solo los campos permitidos.
- [ ] Que en el controlador, usa `actionResponse.setSignal("{señal}", null)` cuando es necesario enviar una señal al cliente (p.ej. `back` para cerrar el formulario, `refresh-tab` para recargar la pestaña, etc.)
- [ ]  Que el controlador ha llamado a los métodos de `actionResponse` adecuados para configurar la respuesta al cliente (p.ej. `setValue` para actualizar campos, `setFlash` para mostrar mensajes, etc.)
- [ ] Que has probado el controlador con casos de éxito y casos de error para validar que funciona correctamente 
- [ ] Que has revisado el código para asegurarte de que sigue las convenciones de nombres, organización y manejo de errores explicadas en esta guía.
- [ ] Que has documentado cualquier decisión importante o complejidad en el código con comentarios claros para facilitar el mantenimiento futuro.
- [ ] Que has actualizado cualquier documentación relevante (p.ej. diagramas de arquitectura, documentación de controladores, etc.) para reflejar el nuevo controlador.
- [ ] Que has verificado que el nuevo controlador no introduce errores o regresiones en otras partes del sistema mediante pruebas manuales o automatizadas.
- [ ] Que has verificado que el nuevo controlador cumple con los requisitos funcionales y no funcionales definidos para la funcionalidad que implementan.
- [ ] Que has asegurado que el nuevo controlador siguen las mejores prácticas de desarrollo de software, incluyendo principios SOLID, patrones de diseño adecuados, y un código limpio y legible.
- [ ] Que has considerado la seguridad y el rendimiento en el diseño e implementación del nuevo controlador, aplicando las medidas necesarias para proteger los datos y optimizar las operaciones.
- [ ] Que has validado que el nuevo controlador se integran correctamente con otras partes del sistema, incluyendo otros servicios, controladores, vistas, etc., y que no causan conflictos o problemas de compatibilidad.
- [ ] Que has realizado pruebas de integración para asegurar que el nuevo controlador funcionan correctamente en conjunto con otros componentes del sistema y que cumplen con los flujos de trabajo esperados.
- [ ] Que has actualizado cualquier prueba automatizada (unitarias, de integración, etc.) para cubrir el nuevo controlador, asegurando una buena cobertura de código y la detección temprana de posibles errores en el futuro.
