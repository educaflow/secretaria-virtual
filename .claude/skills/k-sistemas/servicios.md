# Guía para desarrollar servicios de negocio en EducaFlow Secretaría Virtual

**NOTA: Aunque vamos a usar ejemplos de Subsistemas, todo lo explicado aquí es aplicable a cualquier capa.**

Un servicio de negocio en EducaFlow se compone de dos ficheros Java:
- **Interfaz** (`NombreService.java`) — define el contrato público, extiende `ModelService<T>`
- **Implementación** (`impl/NombreServiceImpl.java`) — extiende `DefaultModelService<T>` e implementa la interfaz

## Descubrimiento automático — sin registro en módulo

`ModelServiceFactory` descubre la implementación por convención de paquetes. Para la entidad `com.pkg.db.MiEntidad` busca en orden:

1. `com.pkg.service.MiEntidadService`
2. `com.pkg.service.MiEntidadServiceImpl`
3. `com.pkg.service.impl.MiEntidadService`
4. `com.pkg.service.impl.MiEntidadServiceImpl`

**No hace falta ningún fichero de módulo ni binding explícito.** La implementación debe estar en uno de esos paquetes y tener el constructor obligatorio (ver abajo).

## Estructura de la interfaz

```java
package com.educaflow.subsystem.SUBSYSTEM.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.SUBSYSTEM.db.MiEntidad;

import java.util.Optional;

public interface MiEntidadService extends ModelService<MiEntidad> {

    // Métodos de validación — devuelven Optional<BusinessMessages>, nunca lanzan BusinessException
    Optional<BusinessMessages> validateInsert(MiEntidad entidad);
    Optional<BusinessMessages> validateUpdate(MiEntidad entidad, MiEntidad entidadOriginal);
    Optional<BusinessMessages> validateRemove(MiEntidad entidad);

    // Métodos de negocio adicionales (si los hay)
    MiEntidad hacerAlgoEspecial(MiEntidad entidad);
}
```

Los métodos declarados en `ModelService<T>` que ya hereda la interfaz son:
- `T insert(T entity)`
- `T update(T entity, T original)`
- `void remove(T entity)`
- `Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context)`

## Estructura de la implementación

```java
package com.educaflow.subsystem.SUBSYSTEM.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.SUBSYSTEM.db.MiEntidad;
import com.educaflow.subsystem.SUBSYSTEM.service.MiEntidadService;
import jakarta.inject.Inject;

import java.util.Optional;

public class MiEntidadServiceImpl extends DefaultModelService<MiEntidad> implements MiEntidadService {

    // Repositorios adicionales (NO servicios) se pueden inyectar como campos con @Inject
    @Inject
    OtroRepositorio otroRepositorio;

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public MiEntidadServiceImpl(Class<MiEntidad> model, Repository<MiEntidad> repository) {
        super(model, repository);
    }

    // --- Métodos CRUD (solo si necesitan lógica extra; si no, los hereda de DefaultModelService) ---

    @Override
    public MiEntidad insert(MiEntidad entidad) {
        fireActionRule_NombreRule1(entidad);

        entidad = super.insert(entidad);   // persiste con repository.save()

        fireActionRule_NombreRule2(entidad);
        return entidad;
    }

    @Override
    public MiEntidad update(MiEntidad entidad, MiEntidad entidadOriginal) {
        fireActionRule_NombreRule3(entidad, entidadOriginal);

        entidad = super.update(entidad, entidadOriginal);   // persiste con repository.save()

        fireActionRule_NombreRule4(entidad, entidadOriginal);
        fireActionRule_NombreRule5(entidad, entidadOriginal);
        return entidad;
    }

    @Override
    public void remove(MiEntidad entidad) {
        super.remove(entidad);   // elimina con repository.remove()
    }

    // --- Métodos de validación — devuelven Optional<BusinessMessages> ---

    @Override
    public Optional<BusinessMessages> validateInsert(MiEntidad entidad) {
        BusinessMessages messages = new BusinessMessages();

        if (entidad.getCampoA() == null) {
            messages.add(new BusinessMessage("campoA", "Es requerido"));
        }
        if (entidad.getCampoB() != null && entidad.getCampoB().isBlank()) {
            messages.add(new BusinessMessage("campoB", "No puede estar vacío"));
        }

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(MiEntidad entidad, MiEntidad entidadOriginal) {
        BusinessMessages messages = new BusinessMessages();

        // Validaciones de actualización...

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(MiEntidad entidad) {
        BusinessMessages messages = new BusinessMessages();

        // Validaciones de borrado...

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_NombreRule1(MiEntidad entidad) {

    }

    private void fireActionRule_NombreRule2(MiEntidad entidad) {

    }

    private void fireActionRule_NombreRule3(MiEntidad entidad, MiEntidad entidadOriginal) {

    }

    private void fireActionRule_NombreRule4(MiEntidad entidad, MiEntidad entidadOriginal) {

    }
    private void fireActionRule_NombreRule5(MiEntidad entidad, MiEntidad entidadOriginal) {

    }    
}
```

## Constructor obligatorio

`ModelServiceFactory` instancia el servicio **por reflexión** buscando exactamente este constructor:

```java
public MiEntidadServiceImpl(Class<MiEntidad> model, Repository<MiEntidad> repository) {
    super(model, repository);
}
```

Reglas del constructor:
- `Repository` **siempre** lleva el tipo genérico: `Repository<MiEntidad>` (nunca `Repository` sin tipo).
- El `super()` recibe el parámetro `model` y el `repository`.
- Si el constructor no existe, la factoría lanza `IllegalStateException`.
- Las dependencias adicionales **no van en el constructor**: se declaran como campos `@Inject` y Guice las inyecta después de construir el objeto.

## Usar `repository` en los métodos

El `repository` pasado al constructor queda disponible como campo protegido heredado de `DefaultModelService`. **Úsalo directamente** — no vuelvas a crearlo con `JpaRepository.of(MiEntidad.class)`:

```java
// MAL — crear otro repository para la misma entidad
List<MiEntidad> todos = JpaRepository.of(MiEntidad.class).all().fetch();

// BIEN — usar el repository heredado
List<MiEntidad> todos = repository.all().fetch();
MiEntidad una = repository.all().filter("self.campo = :v").bind("v", valor).fetchOne();
```

`JpaRepository.of(OtraEntidad.class)` sí es válido cuando necesitas consultar una entidad **diferente** a la que gestiona el servicio.

## DTO de inserción (cuando el insert necesita datos especiales)

Cuando la creación necesita parámetros que no coinciden exactamente con la entidad, se usa un `record` DTO en el mismo paquete del servicio:

```java
// MiEntidadInsertDTO.java — junto a MiEntidadService.java
package com.educaflow.subsystem.SUBSYSTEM.service;

import java.util.Objects;

public record MiEntidadInsertDTO(String campo1, OtraEntidad relacion) {

    public MiEntidadInsertDTO {
        Objects.requireNonNull(campo1, "campo1 no puede ser null");
        Objects.requireNonNull(relacion, "relacion no puede ser null");
    }
}
```

La interfaz del servicio sobrescribe `insert` con el DTO:

```java
public interface MiEntidadService extends ModelService<MiEntidad> {
    MiEntidad insert(MiEntidadInsertDTO dto);
}
```

La implementación construye la entidad a partir del DTO y llama a `super.insert()`:

```java
@Override
public MiEntidad insert(MiEntidadInsertDTO dto) {
    MiEntidad entidad = new MiEntidad();
    entidad.setCampo1(dto.campo1());
    entidad.setRelacion(dto.relacion());
    // ...
    return super.insert(entidad);
}
```

## Convenciones clave

### Nombres de métodos privados
- `fireActionRule_NombreAccion(...)` — efecto secundario (asignar datos, notificar, callback). Se llama antes o después de persistir.
- Los métodos de validación van en la interfaz con nombre `validateInsert` / `validateUpdate` / `validateRemove` y devuelven `Optional<BusinessMessages>`.

### Errores de negocio
Los métodos de validación **nunca lanzan `BusinessException`**: acumulan en `BusinessMessages` y devuelven `Optional`. El controlador decide cómo mostrar los errores.

Un solo error:
```java
messages.add(new BusinessMessage("campo", "Mensaje del error"));
```

Varios errores:
```java
messages.add(new BusinessMessage("campoA", "Es requerido"));
messages.add(new BusinessMessage("campoB", "No puede estar vacío"));
return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
```

### Obtener otro servicio desde un servicio

**NUNCA** inyectar un servicio con `@Inject` — ni dentro de otro servicio ni en un controlador. Siempre se usa `ModelServiceFactory`:

```java
// MAL — prohibido
@Inject
OtroServicio otroServicio;

// BIEN — inyectar ModelServiceFactory y resolver en el método
@Inject
ModelServiceFactory modelServiceFactory;

// Dentro del método que lo necesite:
final OtroServicio otroServicio = (OtroServicio) modelServiceFactory.resolve(OtraEntidad.class);
```

## Checklist de desarrollo de servicios
- [ ] La interfaz extiende `ModelService<T>` del paquete `com.axelor.db.modelservice`
- [ ] La implementación extiende `DefaultModelService<T>` e implementa la interfaz
- [ ] El constructor tiene la firma `(Class<T> model, Repository<T> repository)` y llama a `super(model, repository)` 
- [ ] Los repositorios adicionales van como campos `@Inject`, no en el constructor. Los servicios adicionales **nunca** se inyectan con `@Inject` — se obtienen con `modelServiceFactory.resolve(OtraEntidad.class)`
- [ ] Los métodos `insert` / `update` / `remove` llaman a `super.*()` para persistir — nunca llaman directamente a `repository.save()`
- [ ] Los métodos de validación devuelven `Optional<BusinessMessages>` — no lanzan `BusinessException`
- [ ] La implementación está en `service.impl.MiEntidadServiceImpl` para que la factoría la descubra sin registro explícito
- [ ] Si el insert necesita parámetros especiales, se crea un `record` DTO en el paquete del servicio
