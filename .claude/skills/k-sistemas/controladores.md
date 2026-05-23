# Guia para desarrollar Controladores en Axelor

**NOTA: Aunque vamos a usar ejemplos de Systemas, todo lo explicado aquí es aplicable a cualquier subsistema.**

Un controlador es un fichero Java que expone métodos públicos con `@CallMethod` (de `com.axelor.meta.CallMethod`) para ser llamados desde las vistas Axelor desde un `<action-method>` en XML. El controlador es el punto de entrada desde las vistas Axelor a la lógica de negocio implementada en los servicios. El controlador recibe un `ActionRequest` con toda la información enviada por el cliente (contexto del formulario, filtros, campos, etc.) y un `ActionResponse` para configurar la respuesta que se enviará al cliente (p.ej. cerrar el formulario, mostrar mensajes, actualizar campos, etc.). El controlador debe llamar a los servicios para realizar la lógica de negocio y manejar cualquier error de negocio lanzado por los servicios para mostrarlo correctamente en la vista.

**El controlador no ejecuta lógica de negocio, solo es el punto de entrada desde las vistas y el encargado de llamar a los servicios. Toda la lógica de negocio (validaciones, cálculos, persistencia, etc.) debe estar en los servicios.**

Referencias:
 * La referencia a los métodos ActionRequest están en [ActionRequest](references/ActionRequest.md)
 * La referencia a los métodos ActionResponse están en [ActionResponse](references/ActionResponse.md)
 * La referencia a como llamar al controlador desde las vistas Axelor con `<action-method>` está en [ActionMethod](references/action-method.md)
 * La referencia a la estructura de un controlador está en [Controller](references/controller.md)

## Regla fundamental: un controlador por entidad

**Cada entidad tiene exactamente su propio controlador.** Un controlador solo contiene métodos para una única entidad. No se crea un controlador que agrupe métodos de varias entidades. El nombre del controlador siempre es `<NombreEntidad>Controller` (p.ej. `CertificadoDigitalController`, `DispositivoCriptograficoController`).

## Regla fundamental: NO exponer insert/update/remove desde el controlador

**MUST NOT** crear `@CallMethod` para `insert`, `update` o `remove` en el controlador propio. Esas tres acciones ya las expone Axelor automáticamente por su endpoint REST `/ws/rest/<FQN>`, que entra directamente al servicio (`DefaultModelService.insert/update/remove`) y ya aplica el patrón `validate → super` y la `allowProperties*` correspondiente.

Tener `@CallMethod` propios para insert/update/remove introduciría un segundo camino paralelo al endpoint REST automático, divergente y confuso. El controlador propio **solo** expone `@CallMethod` para acciones de negocio **propias del subsistema** (las que tienen `validateXxx`/`allowPropertiesXxx` declarados en el interface del servicio).

Desde el cliente (XML de vistas), las operaciones de guardar y borrar se hacen siempre con las acciones predefinidas del framework de Axelor: `save` y `delete` para el form principal, y `save-modal` y `delete-modal` para el form modal de entidades hijas dentro de un `<panel-related>`. Esas cuatro acciones ya disparan el endpoint REST `/ws/rest/<FQN>` que entra al servicio aplicando `validate → super` y `AllowProperties`. **MUST NOT** sustituirlas por un `<action-method>` (`Remote-…-action`) que llame a un controlador propio para guardar o borrar. Ver `[[forms.md]]` y `[[actions.md]]` del skill `k-vistas`.

## Anti-patrones prohibidos en el controlador

Cada uno de los siguientes patrones es un fallo de arquitectura o de seguridad. Si el controlador hace cualquiera de estas cosas, está mal; arréglalo moviendo la lógica al lugar correcto.

### MUST NOT — comprobaciones de autorización por rol en el controlador

```java
// ❌ MAL
@CallMethod
public void btnReenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
    if (!isUsuarioAdministrador(AuthUtils.getUser())) {
        actionResponse.setError(I18n.get("Solo el Administrador puede reenviar correos."));
        return;
    }
    // ...
    correoService.reenviar(correo);
}
```

**Por qué es incorrecto:** la autorización es lógica de negocio. Si vive en el controlador, el endpoint REST automático `/ws/rest/<FQN>` (o cualquier otro entry point al servicio) se la salta. La UI puede ser bypassada con curl/Postman.

**Qué hacer en su lugar:** la comprobación va dentro del método del servicio (`reenviar`, `validateInsert`, etc.) usando `SecurityUtil.getUser()`. Así protege a **todos** los entry points. Ver `[[k-secure-coding]]` §4 (autorización multi-centro / IDOR).

### MUST NOT — enforcement de inmutabilidad de campos `servidor` con `if` en el controlador

```java
// ❌ MAL
Correo correo = actionRequestHelper.getModel(correoService.allowPropertiesInsert());
if (correo.getHistorialEstadoExpediente() != null) {
    actionResponse.setError(I18n.get("La referencia al historial no puede asignarse desde la UI."));
    return;
}
```

**Por qué es incorrecto:** la defensa contra mass-assignment es `AllowProperties` + asignación incondicional del campo `servidor` en `*ServiceImpl.insert/update`. Un `if` en el controlador no se aplica cuando el cliente entra por `/ws/rest/<FQN>` directamente.

**Qué hacer en su lugar:** **no** incluir el campo `servidor` en `allowPropertiesInsert()`/`allowPropertiesUpdate()` del servicio, y sobrescribir su valor de forma incondicional dentro de `*ServiceImpl.insert/update`. Ver `[[k-secure-coding]]` §1-§2.

### MUST NOT — un controlador con métodos de otra entidad

```java
// ❌ MAL — CorreoController con un @CallMethod sobre AdjuntoCorreo
public class CorreoController {
    @CallMethod
    public void descargarAdjunto(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper<AdjuntoCorreo> arh = new ActionRequestHelper<>(actionRequest, AdjuntoCorreo.class);
        // ...
    }
}
```

**Por qué es incorrecto:** rompe la regla §"Regla fundamental: un controlador por entidad". Hace ilegible el código y rompe la trazabilidad acción ↔ entidad.

**Qué hacer en su lugar:** crear `AdjuntoCorreoController` con los `@CallMethod` que actúan sobre `AdjuntoCorreo`.

### MUST NOT — lógica de negocio, I/O o acceso a BD en el controlador

```java
// ❌ MAL — el controlador hace I/O, llama a JpaRepository, sanea nombres de fichero…
@CallMethod
public void descargarAdjunto(ActionRequest actionRequest, ActionResponse actionResponse) {
    Long id = actionRequestHelper.getId();
    AdjuntoCorreo adjunto = JpaRepository.of(AdjuntoCorreo.class).find(id);
    String nombre = adjunto.getNombreFichero() != null ? adjunto.getNombreFichero() : "adjunto.bin";
    Path tmp = Files.createTempFile("correo-adjunto-", "-" + sanitizeFileName(nombre));
    Files.write(tmp, adjunto.getContenidoFichero());
    actionResponse.setExportFile(tmp.toString(), nombre);
}
```

**Por qué es incorrecto:** el controlador es solo un punto de entrada. La lógica (cargar la entidad, decidir el nombre, escribir el fichero) pertenece al servicio. Además, leer entidades con `JpaRepository.of(X.class).find(id)` desde el controlador es acceso directo a BD que se salta el servicio y por tanto la autorización del servicio.

**Qué hacer en su lugar:** método en el servicio (p.ej. `AdjuntoCorreoService.prepararDescarga(Long id) → DescargaAdjunto(rutaFichero, nombre)`); el controlador solo invoca al servicio y monta el `setExportFile`.

### MUST NOT — helpers privados que duplican utilidades de `base.util`

```java
// ❌ MAL — métodos privados que ya existen en base.util
private String sanitizeFileName(String name) { return name.replaceAll("[^A-Za-z0-9._-]", "_"); }
private LocalDate toLocalDate(Object value) { /* parsing inline */ }
```

**Por qué es incorrecto:** duplica código, diverge de las versiones canónicas y suele ser menos seguro (p.ej. `TextUtil.sanitizeFileName` quita acentos, caracteres peligrosos, reservados de Windows y trunca a 255; el inline no).

**Qué hacer en su lugar:** usar las utilidades documentadas en el `CLAUDE.md` raíz — `TextUtil.sanitizeFileName`, `Convert.objectToLong`/`coerceToInt`, `JsonUtil`, `MetaFileUtil`, etc.

### MUST NOT — obtener servicios con `Beans.get(...)` cuando hay inyección

```java
// ❌ MAL
RegistroService registroService = com.axelor.inject.Beans.get(RegistroService.class);
```

**Por qué es incorrecto:** Service Locator antipattern; oculta dependencias, dificulta los tests y rompe el patrón de inyección del proyecto.

**Qué hacer en su lugar:** si el servicio hereda de `ModelService`, resolverlo con `modelServiceFactory.resolve(Entidad.class)`. Si no hereda de `ModelService`, inyectarlo con `@Inject` en el campo del controlador.

### MUST NOT — acceder a `actionRequest.getData().get("context")` con casts

```java
// ❌ MAL
Map<String, Object> context = (Map<String, Object>) actionRequest.getData().get("context");
if (context == null) return;
Object dniObj = context.get("dniDestinatario");
```

**Por qué es incorrecto:** cast inseguro, código frágil, y existe API tipada para esto.

**Qué hacer en su lugar:** `ActionRequestHelper` — `getModel(allowProperties)`, `getOriginalModel()`, `getId()`, accesores tipados al contexto.

### MUST NOT — usar `actionResponse.setError(String)` para errores de negocio

```java
// ❌ MAL — mensaje de validación de negocio como string crudo
actionResponse.setError(I18n.get("La fecha final no puede ser anterior a la fecha inicial."));
```

**Por qué es incorrecto:** se salta el sistema `BusinessMessages`/`BusinessException` del proyecto, no se localiza ni se agrupa con el resto de mensajes de la validación, y no respeta el contrato `validateXxx → Optional<BusinessMessages>` del servicio (ver `servicios.md`).

**Qué hacer en su lugar:** el servicio devuelve `Optional<BusinessMessages>` (o lanza `BusinessException` solo cuando es excepcional); el controlador lo entrega con `actionResponseHelper.doResponseBusinessMessagesAsError(...)` o `doResponseBusinessMessages(...)`. `actionResponse.setError` solo para errores **técnicos** no recuperables.

### MUST NOT — capturar `BusinessException` del servicio en el controlador para extraer los mensajes

```java
// ❌ MAL
try {
    correoService.reenviar(correo);
} catch (BusinessException e) {
    actionResponseHelper.doResponseBusinessMessagesAsError(e.getBusinessMessages());
    return;
}
```

**Por qué es incorrecto:** es un síntoma de que falta el `validateXxx` previo. El patrón correcto en el proyecto es: `validateXxx` devuelve `Optional<BusinessMessages>` **antes** de ejecutar la acción; el servicio solo lanza `BusinessException` para errores realmente excepcionales (no para validaciones esperables).

**Qué hacer en su lugar:** exponer `validateReenviar(...)` en el servicio y llamarlo desde el controlador antes de `reenviar(...)`. Ver `servicios.md` §"Patrón validate → action".

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

**El controlador no debe construir repositorios.** Si el método del servicio que vas a llamar necesita un repositorio, eso es asunto del servicio: `DefaultModelService` ya recibe el suyo por inyección en su constructor. El controlador solo llama a métodos del servicio.


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
import com.axelor.db.modelservice.BusinessMessages;
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
    public void hacerAlgoEspecial(ActionRequest actionRequest, ActionResponse actionResponse) {
        final MiEntidadService miEntidadService = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class);

        ActionRequestHelper<MiEntidad> actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        MiEntidad entidadOriginal = actionRequestHelper.getOriginalModel();
        // `AllowProperties` es la lista blanca de los campos que el cliente puede mandar en
        // el JSON del request. **MUST** pedirla al servicio invocando
        // `miEntidadService.allowPropertiesHacerAlgoEspecial()` — la whitelist pertenece
        // a la acción y vive en el servicio (ver `servicios.md` §"Estructura de la
        // implementación", bloque AllowProperties).
        //
        // **MUST NOT** construirla inline en el controlador con
        // `AllowProperties.createAllowProperties(Map.of(...))` — eso duplica la lógica y
        // permite que controlador y servicio diverjan sobre qué campos acepta cada acción.
        //
        // **MUST** consultar `[[k-secure-coding]]` §2 sobre qué campos pueden estar en
        // esa whitelist (regla clave: ningún campo `servidor` del `entity-*.md`).
        MiEntidad entidad = actionRequestHelper.getModel(miEntidadService.allowPropertiesHacerAlgoEspecial());

        miEntidadService.hacerAlgoEspecial(entidad, entidadOriginal);

        actionResponse.setSignal("back", null);
    }

    @CallMethod
    public void validarAntesDeBorrar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final MiEntidadService miEntidadService = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class);

        ActionRequestHelper<MiEntidad> actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        // Para `validateRemove` se usa la `allowPropertiesRemove()` heredada de
        // DefaultModelService (devuelve un default razonable). NO se construye
        // `AllowProperties` inline en el controlador.
        //
        // Si en una acción puntual hace falta una whitelist distinta de la canónica del
        // servicio (caso raro y normalmente síntoma de mal diseño), exponer un
        // `allowPropertiesXxx()` específico en el servicio y llamarlo desde aquí —
        // nunca definir el `Map.of(...)` en el controlador.
        MiEntidad entidad = actionRequestHelper.getModel(miEntidadService.allowPropertiesRemove());

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
- `miEntidadService.allowPropertiesMiAccion()` — fuente canónica del `AllowProperties` para la acción. **MUST** llamarlo desde el controlador en lugar de construir el `AllowProperties` inline con `createAllowProperties(Map.of(...))`. La whitelist vive en el servicio (ver `servicios.md` §"Estructura de la implementación", bloque AllowProperties; reglas de elección en `[[k-secure-coding]]` §3).
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


## Checklist de controladores

Checklist única para desarrollar y revisar `*Controller`. Cada ítem es un tipo de problema concreto observado en revisiones reales o una regla obligatoria del patrón.

### Estructura básica

- [ ] El controlador tiene nombre `<NombreEntidad>Controller` y solo contiene métodos para esa única entidad. **NO** mezcla `@CallMethod` sobre otras entidades (p.ej. `CorreoController` con `descargarAdjunto` sobre `AdjuntoCorreo` → debe existir `AdjuntoCorreoController`).
- [ ] Cada método público lleva la anotación `@CallMethod`.
- [ ] Los métodos que escriben en BD llevan `@Transactional`; los de solo lectura, no.
- [ ] Inyecta `ModelServiceFactory` (no inyecta el `ModelService` directamente con `@Inject`) y resuelve el servicio con `modelServiceFactory.resolve(MiEntidad.class)` en cada método que lo usa.
- [ ] **NO** construye `Repository` explícitamente con `JpaRepository.of(MiEntidad.class)` para pasarlo a `resolve`. El servicio ya tiene su repositorio inyectado.
- [ ] Imports usan `com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper` y `ActionResponseHelper` (no rutas legacy). No quedan imports de `AllowProperties` ni de `java.util.Map` si tras el refactor el controlador ya no los usa.

### Acciones expuestas y delegación al servicio

- [ ] **NO** tiene `@CallMethod` para `insert`, `update` ni `remove` — Axelor ya los expone vía el endpoint REST automático `/ws/rest/<FQN>`. Desde el cliente se usan las acciones predefinidas `save`/`delete` (form principal) y `save-modal`/`delete-modal` (form modal); **nunca** un `<action-method>` propio.
- [ ] **NO** llama manualmente a `validateInsert/Update/Remove` del servicio antes de `insert/update/remove`: lo hace Axelor automáticamente.
- [ ] Los métodos del controlador llaman al servicio para la lógica de negocio. **NO** contienen lógica de negocio (lectura de ficheros, parsing de fechas, cálculos, queries JPA).
- [ ] **NO** valida inline con `throw new BusinessException` ni con `actionResponse.addError(...)` reglas que pertenecen al servicio. La validación se delega a `validateMiAccion(...)` del servicio.
- [ ] **NO** hace comprobaciones de autorización por rol en el controlador (`if (!isAdmin(...)) ...`). La autorización vive en el servicio para que también proteja al endpoint REST automático. Ver `[[k-secure-coding]]` §4.
- [ ] **NO** hace `if (entidad.getCampoServidor() != null) actionResponse.setError(...)` para impedir asignaciones desde la UI. La defensa correcta es no incluir el campo en `allowPropertiesXxx()` + asignación incondicional en `*ServiceImpl.insert/update`. Ver `[[k-secure-coding]]` §1-§2.
- [ ] **NO** lee entidades con `JpaRepository.of(X.class).find(id)` desde el controlador. Cargar la entidad es responsabilidad del servicio.
- [ ] **NO** hace I/O (lectura de ficheros, `Files.createTempFile`, `Files.write`, etc.) ni lógica de negocio en el controlador. El servicio prepara los datos; el controlador solo invoca y monta la respuesta (`setExportFile`, `setValue`, `setSignal`, …).
- [ ] **NO** captura `BusinessException` del servicio para extraer sus `BusinessMessages`. Si el patrón está bien hecho, existe un `validateXxx` previo que devuelve `Optional<BusinessMessages>` y `BusinessException` se reserva para errores excepcionales que se relanzan como `RuntimeException`.
- [ ] Ningún `@CallMethod` tiene el cuerpo vacío o totalmente comentado (acción muerta expuesta a la UI).

### Parámetros, `AllowProperties` y respuesta

- [ ] Los parámetros de los métodos `@CallMethod` tipo 1 se llaman exactamente `actionRequest` y `actionResponse` (camelCase completo). **NO** se usan `request`/`response`/`req`/`resp`/`ar`.
- [ ] Si existe `ActionRequest`, usa `ActionRequestHelper.getOriginalModel()` para el estado original y `ActionRequestHelper.getModel(miEntidadService.allowPropertiesMiAccion())` pidiendo el `AllowProperties` **al servicio** (NO construido inline con `AllowProperties.createAllowProperties(Map.of(...))` ni `createAllowAllProperties()` directamente).
- [ ] Errores de negocio se manejan con `actionResponseHelper.doResponseBusinessMessagesAsError(...)` o `actionResponseHelper.doResponseBusinessMessages(...)`; cualquier otro error se relanza como `RuntimeException`. **NO** se usa `actionResponse.setError(String)` para mensajes de validación de negocio (eso se salta la localización y el contrato `BusinessMessages`).
- [ ] **NO** accede al contexto con casts crudos del estilo `(Map<String, Object>) actionRequest.getData().get("context")`. Para el modelo se usa `ActionRequestHelper.getModel(allowProperties)` / `getOriginalModel()` / `getId()`.
- [ ] **NO** obtiene servicios con `com.axelor.inject.Beans.get(...)`. Si el servicio hereda de `ModelService`, se resuelve con `modelServiceFactory.resolve(Entidad.class)`; si no, se inyecta con `@Inject`.
- [ ] **NO** define helpers privados (`sanitizeFileName`, `toLocalDate`, conversiones, parseo, hashes…) que duplican lo que ya hay en `com.educaflow.base.util` (`TextUtil`, `Convert`, `JsonUtil`, `CryptoUtil`, `MetaFileUtil`, …). Ver el `CLAUDE.md` raíz §"Utilidades de `base.util`".
- [ ] Usa `actionResponse.setSignal("{señal}", null)` cuando es necesario (`back` para cerrar el formulario, `refresh-tab` para recargar la pestaña, etc.).
- [ ] Usa los métodos adecuados de `actionResponse` para configurar la respuesta (`setValue` para actualizar campos, `setFlash` para mensajes, etc.).
- [ ] Probado con casos de éxito y de error.
