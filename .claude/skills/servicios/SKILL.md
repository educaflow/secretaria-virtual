---
name: servicios
description: Skill para crear servicios de negocio en EducaFlow Secretaría Virtual. Un servicio es un par interfaz + implementación que encapsula lógica de negocio, validaciones y persistencia. 
---

# Guía para desarrollar servicios de negocio  en EducaFlow Secretaría Virtual

**NOTA: Aunque vamos a usar ejemplos de Systemas, todo lo explicado aquí es aplicable a cualquier subsistema.**

Un servicio de negocio en EducaFlow se compone de 3 ficheros Java:
- **Interfaz** (`NombreService.java`) — define el contrato público con los métodos que lanza `BusinessException`.
- **Implementación** (`impl/NombreServiceImpl.java`) — implementa la interfaz; usa `@Inject` para inyectar repositorios y otros colaboradores.
- **Módulo** (`module/NombreSystemaModule.java`) — Donde se registra la implementación del servicio en el módulo Guice del sistema.

## Lista de tareas al desarrollar un servicio
Deberás hacer lo siguiente
1. Pensar, analizar y crear la interfaz del servicio con métodos que lanzan `BusinessException`. 
2. Implementar la interfaz en una clase `Impl` con la lógica de negocio, validaciones y persistencia.
3. Registrar el servicio en el módulo Guice del subsistema.
6. Si es necesario llamar al servicio desde otro servicio o controlador, usar `@Inject` para inyectar el servicio y llamar a sus métodos.
7. Pensar y/o analizar si en el "insert" se necesita un DTO específico para la creación (p.ej. `DatosMiEntidad`) que no incluya campos calculados o campos de otras entidades relacionados, o si se puede usar directamente la entidad completa como parámetro de entrada.
8. Crear casos de prueba para el servicio, incluyendo casos de éxito y casos de error para validar que las reglas de negocio funcionan correctamente y que los errores se detectan


## Estructura de la interfaz


```java
package com.educaflow.system.NombreSystema.service;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.system.NombreSystema.db.MiEntidad;

public interface MiEntidadService {

    MiEntidad insert(MiEntidad miEntidad) throws BusinessException;
    MiEntidad update(MiEntidad entidad, MiEntidad entidadOriginal) throws BusinessException;
}
```

Si los datos a insertar son muy distintos a la entidad (p.ej. no incluyen campos calculados, o incluyen campos de otras entidades relacionados), se puede usar un DTO específico para la creación (`DatosMiEntidad`) en lugar de la entidad completa.

```java
package com.educaflow.subsystem.NombreSystema.service;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.system.NombreSystema.db.MiEntidad;

public interface MiEntidadService {

    MiEntidad insert(DatosMiEntidad datosMiEntidad) throws BusinessException;
    MiEntidad update(MiEntidad entidad, MiEntidad entidadOriginal) throws BusinessException;
}
```

- Los métodos lanzan `BusinessException` si hay errores de negocio.
- Los parámetros de entrada de tipo "datos de creación" se modelan como un `record` DTO en el mismo paquete que el interfaz del servicio(p.ej. `DatosMiEntidad`).
- El segundo parámetro `entidadOriginal` (cuando existe) recibe el estado anterior antes de modificaciones, para comparaciones o auditoría.
- La estructura de los métodos publicos del servicio son:
  - Llamar a la regla de negocio de validación (constraint rule) 1, 2, N... que validan el estado de la entidad y lanzan `BusinessException` si algo no es correcto. Estas reglas puede o no necesitar el estado original para comparar.
  - Llamar a la regla de negocio de acción (action rule) 1, 2, N... que realizan efectos secundarios (notificaciones, callbacks, etc.) antes de persistir la entidad. Estas reglas pueden o no necesitar el estado original para comparar.
  - Guardar/actualizar/insertar la entidad con el repositorio.
  - Llamar a la regla de negocio de acción (action rule) 1, 2, N... que realizan efectos secundarios (notificaciones, callbacks, etc.) después de persistir la entidad. Estas reglas pueden o no necesitar el estado original para comparar.

## Estructura de la implementación

```java
package com.educaflow.system.NombreSystema.service.impl;

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

    private void fireConstraintRule_{Regla de negocio de acción 1}(MiEntidad entidad) throws BusinessException {
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


## Convenciones clave

### Nombres de métodos privados en la implementación
- `fireConstraintRule_NombreRegla` — valida y lanza `BusinessException` si algo está mal. Se llama **antes** de persistir.
- `fireActionRule_NombreAccion` — efecto secundario (notificaciones, callbacks, etc.). Se llama **antes** y **después** de persistir.

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

El módulo se hace en un paquete llamado "module" y en una clase llama {Nombre systema o subsistema}Module.java, p.ej. `FirmasModule.java` para el subsistema de firmas.

```java
import com.axelor.app.AxelorModule;

public class {System}Module extends AxelorModule {

    @Override
    protected void configure() {
        bind({InterfazServicio}.class).to({ImplementacionServicio}.class);
    }
}
``` 


## Ejemplo real: FirmaService

**Interfaz** (`subsystem/firmas/service/FirmaService.java`):
```java
public interface FirmaService {
    TareaFirma insert(DatosFirma datosFirma) throws BusinessException;
    TareaFirma update(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException;
    TareaFirma otroMetodo(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException;
}
```



## Checklist de tareas de desarrollo de servicios y controladores
Deberás comprobasr antes de terminar 
- [ ] Que has creado el interfaz del servicio y la implementación
- [ ] Que has registrado el servicio en el módulo Guice del subsistema.
- [ ] Que todas las reglas de negocio de validación (constraint rules) lanzan `BusinessException` con mensajes claros. Y que no tienen efectos laterales (solo validan, no modifican estado ni hacen efectos secundarios).
- [ ] Que todas las reglas de negocio de acción (action rules) realizan los efectos secundarios necesarios antes y después de persistir.
- [ ] Que todas las reglas de validación están agrupadas en métodos privados `fireConstraintRule_NombreRegla` y antes está el bloque de comentarios decorativo `Constraint Rules` para mantener el código organizado.
- [ ] Que todas las reglas de acción en están agrupadas en métodos privados `fireActionRule_NombreAccion` y antes está el bloque de comentarios decorativo `Action Rules` para mantener el código organizado.
- [ ] Que has probado el servicio  con casos de éxito y casos de error para validar que las reglas de negocio funcionan correctamente y que los errores se capturan como `BusinessException` con los mensajes esperados.
- [ ] Que has revisado el código para asegurarte de que sigue las convenciones de nombres, organización y manejo de errores explicadas en esta guía.
- [ ] Que has documentado cualquier decisión importante o complejidad en el código con comentarios claros para facilitar el mantenimiento futuro.
- [ ] Que has actualizado cualquier documentación relevante (p.ej. diagramas de arquitectura, documentación de servicios, etc.) para reflejar el nuevo servicio.
- [ ] Que has verificado que el nuevo servicio no introduce errores o regresiones en otras partes del sistema mediante pruebas automatizadas.
- [ ] Que has verificado que el nuevo servicio  cumple con los requisitos funcionales y no funcionales definidos para la funcionalidad que implementan.
- [ ] Que has asegurado que el nuevo servicio  sigue las mejores prácticas de desarrollo de software, incluyendo principios SOLID, patrones de diseño adecuados, y un código limpio y legible.
- [ ] Que has considerado la seguridad y el rendimiento en el diseño e implementación del nuevo servicio, aplicando las medidas necesarias para proteger los datos y optimizar las operaciones.
- [ ] Que has validado que el nuevo servicio  se integra correctamente con otras partes del sistema, incluyendo otros servicios, controladores, vistas, etc., y que no causan conflictos o problemas de compatibilidad.
- [ ] Que has realizado pruebas de integración para asegurar que el nuevo servicio  funciona correctamente en conjunto con otros componentes del sistema y que cumplen con los flujos de trabajo esperados.
- [ ] Que has actualizado cualquier prueba automatizada (unitarias, de integración, etc.) para cubrir el nuevo servicio , asegurando una buena cobertura de código y la detección temprana de posibles errores en el futuro.
- [ ] Que has documentado cualquier cambio en la base de datos (p.ej. nuevas tablas, cambios en tablas existentes, etc.) que el nuevo servicio pueda requerir
