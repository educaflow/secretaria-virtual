# Guia para desarrollar Controladores en Axelor

**NOTA: Aunque vamos a usar ejemplos de Systemas, todo lo explicado aquí es aplicable a cualquier subsistema.**

Un controlador es un fichero Java que expone métodos públicos con `@CallMethod` para ser llamados desde las vistas Axelor desde un `<action-method>` en XML. El controlador es el punto de entrada desde las vistas Axelor a la lógica de negocio implementada en los servicios. El controlador recibe un `ActionRequest` con toda la información enviada por el cliente (contexto del formulario, filtros, campos, etc.) y un `ActionResponse` para configurar la respuesta que se enviará al cliente (p.ej. cerrar el formulario, mostrar mensajes, actualizar campos, etc.). El controlador debe llamar a los servicios para realizar la lógica de negocio y manejar cualquier error de negocio lanzado por los servicios para mostrarlo correctamente en la vista.

**El controlador no ejecuta lógica de negocio, solo es el punto de entrada desde las vistas y el encargado de llamar a los servicios. Toda la lógica de negocio (validaciones, cálculos, persistencia, etc.) debe estar en los servicios.**

Referencias:
 * La referencia a los métodos ActionRequest están en [ActionRequest](references/ActionRequest.md)
 * La referencia a los métodos ActionResponse están en [ActionResponse](references/ActionResponse.md)
 * La referencia a como llamar al controlador desde las vistas Axelor con `<action-method>` está en [ActionMethod](references/action-method.md)
 * La referencia a la estructura de un controlador está en [Controller](references/controller.md)

## Regla fundamental: un controlador por entidad

**Cada entidad tiene exactamente su propio controlador.** Un controlador solo contiene métodos para una única entidad. No se crea un controlador que agrupe métodos de varias entidades. El nombre del controlador siempre es `<NombreEntidad>Controller` (p.ej. `CertificadoDigitalController`, `DispositivoCriptograficoController`).

## Lista de tareas al desarrollar un controlador
Deberás hacer lo siguiente
1. Crear la clase Java del controlador con nombre `<NombreEntidad>Controller`.
2. Pensar, analizar y crear los métodos del controlador
3. Decidir si cada método usa `ActionRequest`/`ActionResponse` o parámetros de entrada "normales" de Java y que retornan un `Response` o un valor concreto (p.ej. `String`, `boolean`, etc.)
4. Decidir cuando usar `ActionRequestHelper` para simplificar el código (deberás analizar la clase `ActionRequestHelper` para ver que hace y cuando usarla en lugar de trabajar directamente con `ActionRequest`)
5. Decidir a que servicios llamar desde el controlador para realizar la lógica de negocio 
6. Implementar las llamadas al controlador con `<action-method>` desde las vistas Axelor (XML) para que el controlador se ejecute en los momentos necesarios (p.ej. al guardar un formulario, al cambiar un campo, etc.)
8. Crear casos de prueba para el controlador, incluyendo casos de éxito y casos de error 


## Estructura del controlador

Existen 3 tipos de métodos en un controlador, cada uno con su propia estructura y uso recomendado:

```java
public class HelloController {
  @CallMethod 
  public void type1(ActionRequest actionRequest, ActionResponse actionResponse) {
    Contact contact = actionRequest.getContext().asType(Contact.class);
    actionResponse.setFlash("Hello " + contact.getName()); 
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

> **REGLA DE NAMING — parámetros de los métodos type1:**
> Cuando un método del controlador recibe `ActionRequest` y/o `ActionResponse`, los parámetros se nombran **siempre** `actionRequest` y `actionResponse` (camelCase, completo, sin abreviar). Prohibido abreviar como `req`/`resp`/`request`/`response`.
>
> Razón: los nombres `request` y `response` colisionan con los nombres genéricos del paradigma HTTP/Servlet y otros frameworks (Spring `HttpServletRequest`, `HttpServletResponse`); usar el nombre completo deja inequívoco que se trata del par específico de Axelor y mejora la legibilidad cuando se mezclan con otras variables (`actionRequestHelper`, `actionResponseHelper`).
>
> Aplica también a las firmas que aparezcan en diseños (`/sdd-designer-system`) y a los ejemplos de cualquier skill.


## Obtener el servicio con ModelServiceFactory

Los servicios que heredan de `ModelService` (la arquitectura correcta) **no se inyectan directamente** con `@Inject`. Se obtienen a través de `ModelServiceFactory`, que es lo único que se inyecta:

```java
@Inject
private ModelServiceFactory modelServiceFactory;
```

Dentro de cada método, se resuelve el servicio justo antes de usarlo:

```java
// Forma correcta — siempre usar esta
final MiEntidadService miEntidadService = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class);
```

El cast al tipo de la interfaz del servicio es necesario porque `resolve` devuelve `ModelService`.

**NUNCA** crear un `Repository` explícito para pasárselo a `resolve`. Esta forma está **prohibida**:

```java
// MAL — no crear el Repository explícitamente
final Repository repository = JpaRepository.of(MiEntidad.class);
final MiEntidadService miEntidadService = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class, repository);
```


## ActionRequestHelper y ActionResponseHelper

La clase `ActionRequestHelper` simplifica el trabajo con `ActionRequest` en los controladores.
La clase `ActionResponseHelper` simplifica el trabajo con `ActionResponse`.

**Imports correctos:**
```java
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
```

```java
package com.educaflow.{layer}.{nombre}.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.SUBSYSTEM.db.MiEntidad;
import com.educaflow.subsystem.SUBSYSTEM.service.MiEntidadService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Map;
import java.util.Optional;

public class MiEntidadController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void guardar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final MiEntidadService miEntidadService = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class);

        ActionRequestHelper<MiEntidad> actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        MiEntidad entidadOriginal = actionRequestHelper.getOriginalModel();
        AllowProperties allowProperties = AllowProperties.createAllowProperties(
            Map.of(
                "campoSimple", Map.of(),
                "coleccion", Map.of("subcampo", Map.of())
            )
        );
        MiEntidad entidad = actionRequestHelper.getModel(allowProperties);

        miEntidadService.update(entidad, entidadOriginal);

        actionResponse.setSignal("back", null);
    }

    @CallMethod
    public void validarAntesDeBorrar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final MiEntidadService miEntidadService = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class);

        ActionRequestHelper<MiEntidad> actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        MiEntidad entidad = actionRequestHelper.getModel(allowProperties);

        Optional<BusinessMessages> validationResult = miEntidadService.validateRemove(entidad);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }
}
```

### Reglas del controlador

- `@CallMethod` — obligatorio en cada método público que llame una vista.
- `@Transactional` — obligatorio en métodos que escriben en base de datos.
- `@Inject ModelServiceFactory modelServiceFactory` — única inyección necesaria para obtener servicios que heredan de `ModelService`.
- `modelServiceFactory.resolve(MiEntidad.class)` — obtiene el servicio asociado a la entidad.
- `ActionRequestHelper.getOriginalModel()` — obtiene el estado original de la entidad antes de las modificaciones del usuario.
- `ActionRequestHelper.getModel(allowProperties)` — obtiene la entidad con solo los campos permitidos copiados desde la request. **Nunca** usar `getModel()` sin `AllowProperties` en métodos que guardan datos.
- `AllowProperties.createAllowProperties(Map.of(...))` — define qué campos (y sub-campos) se pueden copiar.
- `actionResponseHelper.doResponseBusinessMessagesAsError(businessMessages)` — muestra los errores de negocio como diálogo de error modal en la vista.
- `actionResponseHelper.doResponseBusinessMessages(businessMessages)` — almacena los mensajes de negocio en la respuesta para mostrarlos inline en el formulario.
- `actionResponse.setSignal("back", null)` — cierra el formulario y vuelve al grid tras guardar con éxito.
- Errores no esperados se relanzán como `RuntimeException` — Axelor los mostrará como error genérico.

### Retorno de valores

En caso de que el controlador necesite devolver un valor a la vista (p.ej. para actualizar un campo calculado sin recargar toda la entidad), se puede usar `actionResponse.setValue("nombreCampo", valor)`:

```java
@CallMethod
public void prepararAlgo(ActionRequest actionRequest, ActionResponse actionResponse) {
    ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
    MiEntidad entidad = JpaRepository.of(MiEntidad.class).find(actionRequestHelper.getId());
    
    actionResponse.setValue("campo", valor);
}
```


## Checklist de tareas de desarrollo de controladores
Deberás comprobar antes de terminar
- [ ] Que el controlador tiene nombre `<NombreEntidad>Controller` y solo contiene métodos para esa única entidad (no agrupar varias entidades en un mismo controlador).
- [ ] Que has creado el controlador con métodos que llevan la anotación `@CallMethod`
- [ ] Que los métodos del controlador llaman a los servicios para realizar la lógica de negocio, y que no contienen lógica de negocio por sí mismos (solo llaman a los servicios).
- [ ] Que el controlador inyecta `ModelServiceFactory` y obtiene el servicio con `modelServiceFactory.resolve(MiEntidad.class)` en cada método que lo necesite (sin crear un `Repository` explícito).
- [ ] Que los métodos del controlador manejan los errores de negocio usando `actionResponseHelper.doResponseBusinessMessagesAsError(...)` o `actionResponseHelper.doResponseBusinessMessages(...)`, y relanzan cualquier otro error como `RuntimeException`.
- [ ] Que en el controlador, los métodos que escriben en BD llevan `@Transactional` y los que no escriben no lo llevan.
- [ ] Que en el controlador, si existe el ActionRequest usas `ActionRequestHelper.getOriginalModel()` para obtener el estado original de la entidad y `ActionRequestHelper.getModel(allowProperties)` con `AllowProperties` para obtener solo los campos permitidos.
- [ ] Que en el controlador, usa `actionResponse.setSignal("{señal}", null)` cuando es necesario enviar una señal al cliente (p.ej. `back` para cerrar el formulario, `refresh-tab` para recargar la pestaña, etc.)
- [ ] Que el controlador ha llamado a los métodos de `actionResponse` adecuados para configurar la respuesta al cliente (p.ej. `setValue` para actualizar campos, `setFlash` para mostrar mensajes, etc.)
- [ ] Que has probado el controlador con casos de éxito y casos de error para validar que funciona correctamente
- [ ] Que has revisado que los imports usan `com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper` y `com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper` (no rutas legacy).
