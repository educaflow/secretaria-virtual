---
name: servicios
description: Skill para crear servicios de negocio y sus controladores en EducaFlow Secretaría Virtual. Un servicio es un par interfaz + implementación que encapsula lógica de negocio, validaciones y persistencia. El controlador expone los métodos del servicio a las vistas Axelor.
---

Un servicio de negocio en EducaFlow se compone de tres ficheros Java:
- **Interfaz** (`NombreService.java`) — define el contrato público con los métodos que lanza `BusinessException`.
- **Implementación** (`impl/NombreServiceImpl.java`) — implementa la interfaz; usa `@Inject` para inyectar repositorios y otros colaboradores.
- **Controlador** (`controllers/NombreController.java`) — puente entre las vistas Axelor y el servicio; gestiona `ActionRequest`/`ActionResponse`.

## Estructura de la interfaz

```java
package com.educaflow.subsystem.SUBSYSTEM.service;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.SUBSYSTEM.db.MiEntidad;

public interface MiEntidadService {

    MiEntidad insert(DatosMiEntidad datos) throws BusinessException;
    MiEntidad update(MiEntidad entidad, MiEntidad entidadOriginal) throws BusinessException;
}
```

- Los métodos lanzan `BusinessException` si hay errores de negocio.
- Los parámetros de entrada de tipo "datos de creación" se modelan como un `record` DTO en el mismo paquete (p.ej. `DatosMiEntidad`).
- El segundo parámetro `entidadOriginal` (cuando existe) recibe el estado anterior antes de modificaciones, para comparaciones o auditoría.
- La estructura de los métodos publicos del servicio son:
  - Llamar a la regla de negocio de validación (constraint rule) 1, 2, N... que validan el estado de la entidad y lanzan `BusinessException` si algo no es correcto. Estas reglas puedee o no necesitas el estado original para comparar.
  - Llamar a la regla de negocio de acción (action rule) 1, 2, N... que realizan efectos secundarios (notificaciones, callbacks, etc.) antes de persistir la entidad. Estas reglas pueden necesitar el estado original para comparar.
  - Guardar/actualizar/insertar la entidad con el repositorio.
  - Llamar a la regla de negocio de acción (action rule) 1, 2, N... que realizan efectos secundarios (notificaciones, callbacks, etc.) después de persistir la entidad. Estas reglas pueden necesitar el estado original para comparar.

## Estructura de la implementación

```java
package com.educaflow.subsystem.SUBSYSTEM.service.impl;

import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.SUBSYSTEM.db.MiEntidad;
import com.educaflow.subsystem.SUBSYSTEM.db.repo.MiEntidadRepository;
import com.educaflow.subsystem.SUBSYSTEM.service.MiEntidadService;
import jakarta.inject.Inject;

public class MiEntidadServiceImpl implements MiEntidadService {

    @Inject
    MiEntidadRepository miEntidadRepository;

    @Override
    public MiEntidad insert(DatosMiEntidad datos) throws BusinessException {
        MiEntidad entidad = new MiEntidad();
        // ... mapear datos → entidad

        entidad = miEntidadRepository.save(entidad);

        return entidad;
    }

    @Override
    public MiEntidad update(MiEntidad entidad, MiEntidad entidadOriginal) throws BusinessException {
        fireConstraintRule_{regla de negocio de validación1}(entidad);
        fireConstraintRule_{regla de negocio de validación2}(entidad,entidadOriginal);
        fireConstraintRule_{regla de negocion de validaciónN}(entidad);
        fireConstraintRule_{regla de negocion de validaciónM}(entidad,entidadOriginal);

        //Si todas las reglas de validación pasan sin lanzar excepción, se ejecutan las reglas de acción (efectos secundarios) antes de guardar
        fireActionRule_{Pre Regla de negocio de acción 1}(entidad);
        fireActionRule_{Pre Regla de negocio de acción 2}(entidad);
        fireActionRule_{Pre Regla de negocio de acción N}(entidad,entidadOriginal);
        fireActionRule_{Pre Regla de negocio de acción M}(entidad,entidadOriginal);

        entidad = miEntidadRepository.save(entidad);

        fireActionRule_{Post Regla de negocio de acción A}(entidad);
        fireActionRule_{Post Regla de acción B}(entidad);
        etc...

        return entidad;
    }


    /************************************************************************************/
    /********************************    Action Rules    ********************************/
    /************************************************************************************/

    private void fireActionRule_{regla de negocio de acción1}(MiEntidad entidad) {
        // efectos secundarios : notificaciones, callbacks, etc.
    }


    /****************************************************************************************/
    /********************************    Constraint Rules    ********************************/
    /****************************************************************************************/

    private void fireConstraintRule_{Regla de negocio de acción A}(MiEntidad entidad) throws BusinessException {
        if ({Regla de validación}) {
            throw new BusinessException("campo", "Es requerido", "Título del campo");
        }
    }

    private void fireConstraintRule_VariosErrores(MiEntidad entidad,MiEntidad entidadOriginal) throws BusinessException {
        BusinessMessages messages = new BusinessMessages();

        if (entidad.getCampoA() == null) {
            messages.add(new BusinessMessage("campoA", "Es requerido", "Título A"));
        }
        if (entidad.getCampoB() == null) {
            messages.add(new BusinessMessage("campoB", "Es requerido", "Título B"));
        }

        if (messages.size() > 0) {
            throw new BusinessException(messages);
        }
    }
}
```

## Estructura del controlador

El controlador es el punto de entrada desde las vistas Axelor (`action-method` en XML). Cada método público lleva `@CallMethod` y recibe `(ActionRequest, ActionResponse)`.

```java
package com.educaflow.subsystem.SUBSYSTEM.controllers;

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
    MiEntidadRepository miEntidadRepository;
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

### Métodos sin transacción (solo lectura o delegación a AutoFirma)

Si el método no escribe en BD (p.ej. prepara datos para AutoFirma), no lleva `@Transactional`:

```java
@CallMethod
public void prepararAlgo(ActionRequest actionRequest, ActionResponse actionResponse) {
    ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
    MiEntidad entidad = miEntidadRepository.find(actionRequestHelper.getId());

    // ... construir respuesta sin guardar
    actionResponse.setValue("campo", valor);
}
```

## Convenciones clave

### Nombres de métodos privados en la implementación
- `fireConstraintRule_NombreRegla` — valida y lanza `BusinessException` si algo está mal. Se llama **antes** de guardar.
- `fireActionRule_NombreAccion` — efecto secundario (notificaciones, callbacks, etc.). Se llama **después** de guardar.

### Organización del fichero de implementación
Los métodos privados se separan en dos bloques con comentarios decorativos:
```java
/************************************************************************************/
/********************************    Action Rules    ********************************/
/************************************************************************************/

/****************************************************************************************/
/********************************    Constraint Rules    ********************************/
/****************************************************************************************/
```

### Inyección de dependencias
- `@Inject` en campos de instancia para repositorios y servicios colaboradores.
- `Beans.get(Clase.class)` para obtener instancias dentro de métodos (útil cuando la clase a obtener se conoce en tiempo de ejecución, p.ej. por FQCN).

### Errores de negocio
- Un solo error: `throw new BusinessException("campo", "Mensaje", "Título del campo")`
- Múltiples errores: acumular en `BusinessMessages` y lanzar al final.

### Registro del binding en el módulo Guice
Al crear un servicio nuevo hay que registrar el binding en la clase de módulo del subsistema:
```java
bind(MiEntidadService.class).to(MiEntidadServiceImpl.class);
```

## Ejemplo real: FirmaService + FirmarController

**Interfaz** (`subsystem/firmas/service/FirmaService.java`):
```java
public interface FirmaService {
    TareaFirma insert(DatosFirma datosFirma) throws BusinessException;
    TareaFirma update(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException;
    TareaFirma otroMetodo(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException;
}
```
