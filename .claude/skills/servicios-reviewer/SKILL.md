---
name: servicios-reviewer
description: Revisa que los servicios Java (interfaz + implementación) creados o modificados cumplen todas las reglas de /servicios-knowledge — herencia, constructor, validaciones, métodos CRUD, DTOs y descubrimiento automático.
---

# servicios-reviewer

## Propósito

Verificar que los servicios Java (interfaz + implementación) creados o modificados siguen las reglas definidas en `/servicios-knowledge`.

## Qué leer

1. El fichero de la interfaz del servicio (`service/MiEntidadService.java`).
2. El fichero de la implementación (`service/impl/MiEntidadServiceImpl.java`).
3. El DTO de inserción si existe (`service/MiEntidadInsertDTO.java`).
4. El skill `/servicios-knowledge` para tener presentes todas las reglas.

## Interfaz del servicio

- [ ] La interfaz extiende `ModelService<MiEntidad>` del paquete `com.axelor.db.modelservice`.
- [ ] Los métodos de validación declarados en la interfaz devuelven `Optional<BusinessMessages>`:
  - `Optional<BusinessMessages> validateInsert(MiEntidad entidad)`
  - `Optional<BusinessMessages> validateUpdate(MiEntidad entidad, MiEntidad entidadOriginal)`
  - `Optional<BusinessMessages> validateRemove(MiEntidad entidad)`
- [ ] Ningún método de la interfaz declara que lanza `BusinessException`.
- [ ] Si el insert necesita parámetros especiales, está declarado con el DTO: `MiEntidad insert(MiEntidadInsertDTO dto)`.

## Implementación del servicio

### Herencia y ubicación

- [ ] La clase extiende `DefaultModelService<MiEntidad>` e implementa la interfaz.
- [ ] La implementación está en el paquete `com.educaflow.{layer}.{subsistema}.service.impl` para que `ModelServiceFactory` la descubra sin registro explícito.
- [ ] No hay fichero de módulo Guice ni binding explícito solo para este servicio.

### Constructor obligatorio

- [ ] Existe el constructor `public MiEntidadServiceImpl(Class<MiEntidad> model, Repository<MiEntidad> repository)` que llama a `super(model, repository)` 
- [ ] `Repository` en el constructor lleva el tipo genérico: `Repository<MiEntidad>`, nunca `Repository` sin tipo.
- [ ] El constructor NO tiene `@Inject` (salvo caso excepcional con dependencias de constructor — poco habitual).
- [ ] Los repositorios adicionales se declaran como campos `@Inject`, no como parámetros del constructor.
- [ ] **No existe ningún campo `@Inject OtroServicio`** — los servicios adicionales se obtienen con `modelServiceFactory.resolve(OtraEntidad.class)`, nunca se inyectan directamente.

### Uso del repository

- [ ] Los métodos del servicio usan `repository.all()`, `repository.find()`, etc. para consultar la propia entidad — **no** `JpaRepository.of(MiEntidad.class)`.
- [ ] `JpaRepository.of(OtraEntidad.class)` solo aparece cuando se necesita consultar una entidad **diferente** a la que gestiona el servicio.

### Métodos CRUD

- [ ] Si se sobreescribe `insert`, llama a `super.insert(entidad)` para persistir.
- [ ] Si se sobreescribe `update`, llama a `super.update(entidad, entidadOriginal)` para persistir. No llama a `repository.save()` directamente.
- [ ] Si se sobreescribe `remove`, llama a `super.remove(entidad)`.

### Métodos de validación

- [ ] Los métodos `validate*` acumulan mensajes en `BusinessMessages` y devuelven `Optional<BusinessMessages>`.
- [ ] Ningún método `validate*` lanza `BusinessException`.
- [ ] El patrón de retorno es: `return messages.isEmpty() ? Optional.empty() : Optional.of(messages)`.

### Métodos de efectos secundarios

- [ ] Los efectos secundarios están en métodos privados con nombre `fireActionRule_{NombreAccion}(...)`.
- [ ] Estos métodos se llaman desde `insert`, `update` o `remove` antes o después de la llamada a `super`.
- [ ] La implementación está organizada con el bloque de comentario decorativo:
  ```
  /*************************************************************************************/
  /********************************    Action Rules    *********************************/
  /*************************************************************************************/
  ```

## DTO de inserción (si existe)

- [ ] El DTO es un `record` Java (no una clase convencional).
- [ ] El DTO está en el paquete del servicio (`service/`), no en `service/impl/`.
- [ ] El constructor compacto del record valida con `Objects.requireNonNull` cada parámetro obligatorio.

## Checklist final

- [ ] La interfaz extiende `ModelService<T>` de `com.axelor.db.modelservice`
- [ ] La implementación extiende `DefaultModelService<T>` e implementa la interfaz
- [ ] El constructor tiene `Repository<T>` (con tipo genérico) y llama a `super(model, repository)` 
- [ ] Los métodos usan `repository.*` para la propia entidad, no `JpaRepository.of(MiEntidad.class)`
- [ ] Los repositorios adicionales son campos `@Inject`, no parámetros del constructor. Los servicios adicionales **nunca** son `@Inject` — se resuelven con `modelServiceFactory.resolve()`
- [ ] Los métodos CRUD sobreescritos llaman a `super.*()` para persistir
- [ ] Los métodos `validate*` devuelven `Optional<BusinessMessages>` y nunca lanzan `BusinessException`
- [ ] Los efectos secundarios están en métodos `fireActionRule_*` privados
- [ ] La implementación está en `service.impl.*ServiceImpl` para descubrimiento automático
- [ ] Si hay DTO de inserción, es un `record` con validaciones de nulos en el constructor compacto
- [ ] No existe módulo Guice ni binding explícito para registrar este servicio

## Resultado

Si todos los checks del checklist final están bien, mostrar únicamente: **OK-No hay problemas**
