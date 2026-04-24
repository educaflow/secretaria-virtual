---
name: servicios-task
description: Skill para crear o corregir servicios de negocio en EducaFlow. Un servicio es una interfaz que extiende ModelService<T> y una implementación que extiende DefaultModelService<T>.
---

# servicios-task

## Servicios de Axelor: diseño, generación y corrección
- Este skill sirve para diseñar, generar o corregir servicios Java de EducaFlow.
- Los servicios encapsulan toda la lógica de negocio, validaciones y persistencia.
- Se siguen las normas definidas en el skill `/servicios-knowledge`.

## Tareas a realizar al diseñar el servicio

- Identificar la entidad sobre la que opera el servicio
- Decidir qué métodos de negocio se necesitan más allá del CRUD básico
- Decidir si el `insert` estándar es suficiente o necesita un DTO propio (ver `/servicios-knowledge`)
- Decidir qué validaciones aplican a `validateInsert`, `validateUpdate` y `validateRemove`
- Identificar efectos secundarios (acciones): notificaciones, numeradores, firmas, callbacks — cada uno se convierte en un `fireActionRule_*`
- Identificar dependencias adicionales que necesitará el servicio (`@Inject` como campos)
- Ubicar los ficheros en el paquete correcto para que `ModelServiceFactory` los descubra sin registro explícito:
  - Interfaz: `com.educaflow.{layer}.{subsistema}.service.MiEntidadService`
  - Implementación: `com.educaflow.{layer}.{subsistema}.service.impl.MiEntidadServiceImpl`

## En caso de tener que crear el servicio

### 1. Crear la interfaz

- Extender `ModelService<MiEntidad>` de `com.axelor.db.modelservice`
- Declarar los métodos de validación que devuelven `Optional<BusinessMessages>`:
  - `validateInsert(MiEntidad entidad)`
  - `validateUpdate(MiEntidad entidad, MiEntidad entidadOriginal)`
  - `validateRemove(MiEntidad entidad)`
- Declarar métodos de negocio adicionales si los hay
- Si el insert necesita un DTO, declarar `MiEntidad insert(MiEntidadInsertDTO dto)` 

### 2. Crear el DTO de inserción (solo si es necesario)

- Crear un `record` Java en el mismo paquete que la interfaz
- Añadir validaciones de nulos con `Objects.requireNonNull` en el constructor compacto

### 3. Crear la implementación

- Extender `DefaultModelService<MiEntidad>` e implementar la interfaz
- Añadir el constructor obligatorio **sin `@Inject`** (salvo que haya que inyectar dependencias adicionales por constructor, que es poco habitual):
  ```java
  public MiEntidadServiceImpl(Class<MiEntidad> model, Repository repository) {
      super(model, repository);
  }
  ```
- Declarar dependencias adicionales como campos `@Inject` (Guice las inyecta tras construir)
- Implementar `insert` / `update` / `remove` solo si tienen lógica extra; llamar siempre a `super.*()` para persistir
- Implementar los métodos `validate*` acumulando en `BusinessMessages` y devolviendo `Optional`
- Añadir métodos privados `fireActionRule_NombreAccion(...)` para efectos secundarios
- Organizar el fichero en secciones con comentarios decorativos:
  ```java
  /*************************************************************************************/
  /********************************    Action Rules    *********************************/
  /*************************************************************************************/
  ```

## Si el servicio ya existe pero hay que corregirlo

- Verificar que la interfaz extiende `ModelService<T>` (no alguna interfaz antigua)
- Verificar que la implementación extiende `DefaultModelService<T>`
- Verificar que existe el constructor `(Class<T> model, Repository repository)` llamando a `super`
- Si los métodos de validación lanzan `BusinessException` en lugar de devolver `Optional<BusinessMessages>`, refactorizarlos para que acumulen y devuelvan
- Si `update` llama a `repository.save()` directamente, sustituir por `super.update(entidad, entidadOriginal)`
- Si hay dependencias en el constructor en lugar de en campos `@Inject`, moverlas a campos
- Asegurarse de que los paquetes siguen la convención para que `ModelServiceFactory` los descubra

## Revisión
- [ ] La interfaz extiende `ModelService<T>` de `com.axelor.db.modelservice`
- [ ] La implementación extiende `DefaultModelService<T>` e implementa la interfaz
- [ ] Existe el constructor `(Class<T> model, Repository repository)` que llama a `super(model, repository)`
- [ ] Las dependencias adicionales son campos `@Inject`, no parámetros del constructor
- [ ] Los métodos `insert` / `update` / `remove` llaman a `super.*()` para persistir
- [ ] Los métodos `validate*` devuelven `Optional<BusinessMessages>` y no lanzan `BusinessException`
- [ ] Los efectos secundarios están en métodos privados `fireActionRule_*`
- [ ] La implementación está en `service.impl.MiEntidadServiceImpl` para el descubrimiento automático
- [ ] Si se necesita un DTO de inserción, es un `record` Java en el paquete del servicio con validaciones de nulos
- [ ] No existe ningún fichero de módulo ni binding explícito para registrar el servicio
