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
    MiEntidad marcarComoXxx(MiEntidad entidad, MiEntidad entidadOriginal) throws BusinessException;
}
```

- Los métodos lanzan `BusinessException` si hay errores de negocio.
- Los parámetros de entrada de tipo "datos de creación" se modelan como un `record` DTO en el mismo paquete (p.ej. `DatosMiEntidad`).
- El segundo parámetro `entidadOriginal` (cuando existe) recibe el estado anterior antes de modificaciones, para comparaciones o auditoría.

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
    public MiEntidad marcarComoXxx(MiEntidad entidad, MiEntidad entidadOriginal) throws BusinessException {
        fireConstraintRule_AlgoRequerido(entidad);

        // ... lógica de negocio

        entidad = miEntidadRepository.save(entidad);

        fireActionRule_NotificarAlgo(entidad);

        return entidad;
    }


    /************************************************************************************/
    /********************************    Action Rules    ********************************/
    /************************************************************************************/

    private void fireActionRule_NotificarAlgo(MiEntidad entidad) {
        // efectos secundarios tras guardar: notificaciones, callbacks, etc.
    }


    /****************************************************************************************/
    /********************************    Constraint Rules    ********************************/
    /****************************************************************************************/

    private void fireConstraintRule_AlgoRequerido(MiEntidad entidad) throws BusinessException {
        if (entidad.getCampo() == null || entidad.getCampo().isBlank()) {
            throw new BusinessException("campo", "Es requerido", "Título del campo");
        }
    }

    private void fireConstraintRule_VariosErrores(MiEntidad entidad) throws BusinessException {
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
    public void marcarComoXxx(ActionRequest actionRequest, ActionResponse actionResponse) {
        try {
            ActionRequestHelper<MiEntidad> actionRequestHelper = new ActionRequestHelper(actionRequest, MiEntidad.class);
            MiEntidad entidadOriginal = actionRequestHelper.getOriginalModel();
            AllowProperties allowProperties = AllowProperties.createAllowProperties(
                Map.of("campoPermitido", Map.of())
            );
            MiEntidad entidad = actionRequestHelper.getModel(allowProperties);

            miEntidadService.marcarComoXxx(entidad, entidadOriginal);

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
- `AllowProperties.createAllowProperties(Map.of(...))` — define qué campos (y sub-campos) se pueden copiar. La clave es el nombre del campo; el valor es un `Map` con sus sub-campos (vacío `Map.of()` si es un campo simple).
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
    TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException;
    TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException;
}
```

**Implementación** (`subsystem/firmas/service/impl/FirmaServiceImpl.java`):
- `insert` mapea `DatosFirma` (record) a `TareaFirma` y guarda.
- `marcarComoFirmada` valida documentos (constraint), guarda, y luego notifica via callback (action).
- `marcarComoRechazada` valida motivo (constraint), guarda, y notifica (action).
- El callback usa `Class.forName(fqcn)` + `Beans.get()` + `JsonUtil.fromJson()` para invocar el `FirmaNotifier` registrado.

**Controlador** (`subsystem/firmas/controllers/FirmarController.java`):
- `firmarDocumentosConAutoFirma` — sin `@Transactional`; construye el objeto `AutoFirma` con la posición y NIF del firmante, añade los pares origen→destino de cada documento, y delega en `AutoFirma.sendToActionResponse`.
- `marcarComoFirmada` — `@Transactional`; permite solo el campo `documentosFirma.documentoFirmado` via `AllowProperties` (no permite que la vista modifique otros campos); llama al servicio y hace `setSignal("back")`.
- `marcarComoRechazada` — `@Transactional`; permite solo `motivoRechazo`; llama al servicio y hace `setSignal("back")`.
