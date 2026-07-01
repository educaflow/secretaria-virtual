---
type: implementation-task
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/service/SmokeTestService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio |
| `subsystem/smoketest/service/impl/SmokeTestServiceImpl.java` | Crear | k-sistemas (servicios.md) | Implementación con validate*, fireActionRule_*, allowProperties* |

Rutas destino completas:
- `src/main/java/com/educaflow/subsystem/smoketest/service/SmokeTestService.java`
- `src/main/java/com/educaflow/subsystem/smoketest/service/impl/SmokeTestServiceImpl.java`

---

## Paso 2 — Servicio: SmokeTestService / SmokeTestServiceImpl

Crear los dos ficheros Java:

- `src/main/java/com/educaflow/subsystem/smoketest/service/SmokeTestService.java`
- `src/main/java/com/educaflow/subsystem/smoketest/service/impl/SmokeTestServiceImpl.java`

No se crea módulo Guice: `ModelServiceFactory` descubre `SmokeTestServiceImpl` automáticamente por convención de nombre y paquete.

No se crea controlador: el CRUD es estándar (botones `save`/`delete`/`back` de Axelor) sin lógica de negocio adicional en `@CallMethod`.

#### Interfaz: `SmokeTestService.java`

FQN: `com.educaflow.subsystem.smoketest.service.SmokeTestService`

```
package com.educaflow.subsystem.smoketest.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.smoketest.db.SmokeTest;

public interface SmokeTestService extends ModelService<SmokeTest> {
    // Sin acciones propias adicionales.
    // El CRUD estándar (insert/update/remove) lo expone ModelService<T>.
    // Las sobrescrituras de validate*/allowProperties*/insert/update viven en la impl.
}
```

#### Implementación: `SmokeTestServiceImpl.java`

FQN: `com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl`

```java
// Clase: com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl
// Extiende DefaultModelService<SmokeTest>, implementa SmokeTestService.

@Inject
public SmokeTestServiceImpl(AbstractSmokeTestRepository repository);
//   Constructor obligatorio para inyección Guice.
//   Pasa el repository a super(repository) y lo guarda como campo.

@Override
public AllowProperties allowPropertiesInsert();
//   Forma elegida: createAllowProperties (whitelist).
//   Campos cliente aceptados en insert: ["texto"].
//   fechaCreacion y fechaUltimaModificacion EXCLUIDOS de la whitelist:
//     son campos servidor asignados incondicionalmente por fireActionRule_*.

@Override
public AllowProperties allowPropertiesUpdate();
//   Forma elegida: createAllowProperties (whitelist).
//   Campos cliente aceptados en update: ["texto"].
//   fechaCreacion EXCLUIDO: inmutable tras creación (restaurado desde original en update).
//   fechaUltimaModificacion EXCLUIDO: recalculado incondicionalmente por fireActionRule_*.

@Override
public Optional<BusinessMessages> validateInsert(SmokeTest entity);
//   Aplica V-SmokeTest-001 (Origen spec: RES-001):
//     Comprueba que entity.getTexto() no sea null ni cadena vacía/blank.
//     Mensaje debe transmitir: que el texto es obligatorio (campo requerido).
//   Devuelve Optional.empty() si la validación pasa; Optional.of(mensaje) si falla.

@Override
public Optional<BusinessMessages> validateUpdate(SmokeTest entity, SmokeTest original);
//   Aplica V-SmokeTest-001 (Origen spec: RES-001):
//     Misma comprobación que validateInsert: texto no nulo ni vacío.
//     Mensaje debe transmitir: que el texto es obligatorio.
//   Devuelve Optional.empty() si pasa; Optional.of(mensaje) si falla.

@Override
public SmokeTest insert(SmokeTest entity);
//   1ª línea: validateInsert(entity).ifPresent(BusinessMessages::throwIfInvalid).
//   Invoca fireActionRule_AsignarFechaCreacion(entity)            — R-SmokeTest-001 (Antes).
//   Invoca fireActionRule_ActualizarFechaUltimaModificacion(entity)— R-SmokeTest-002 (Antes).
//   Persiste con repository.save(entity).  MUST NOT llamar a super.insert.
//   Devuelve la entidad guardada.

@Override
public SmokeTest update(SmokeTest entity, SmokeTest original);
//   1ª línea: validateUpdate(entity, original).ifPresent(BusinessMessages::throwIfInvalid).
//   Restaura inmutable: entity.setFechaCreacion(original.getFechaCreacion()).
//     Aunque el cliente envíe fechaCreacion, se restaura siempre desde original
//     (defensa anti mass-assignment, k-secure-coding §3.3).
//   Invoca fireActionRule_ActualizarFechaUltimaModificacion(entity)— R-SmokeTest-002 (Antes).
//   Persiste con repository.save(entity).  MUST NOT llamar a super.update.
//   Devuelve la entidad guardada.

private void fireActionRule_AsignarFechaCreacion(SmokeTest entity);
//   Aplica R-SmokeTest-001 (Origen spec: CC-001, campo `fechaCreacion` clasificado `servidor`).
//   Asignación INCONDICIONAL: entity.setFechaCreacion(LocalDateTime.now()).
//   MUST NOT añadir guarda `if (entity.getFechaCreacion() == null)`: permitiría que un
//   atacante vía el endpoint REST genérico cuele una fecha falsificada (k-secure-coding §3.3).
//   Momento: Antes (escribe en el mismo registro, antes de repository.save).

private void fireActionRule_ActualizarFechaUltimaModificacion(SmokeTest entity);
//   Aplica R-SmokeTest-002 (Origen spec: CC-002, campo `fechaUltimaModificacion` clasificado `servidor`).
//   Asignación INCONDICIONAL: entity.setFechaUltimaModificacion(LocalDateTime.now()).
//   MUST NOT añadir guarda `if (entity.getFechaUltimaModificacion() == null)`.
//   Invocada tanto en insert como en update (el spec dice que se recalcula en cada modificación).
//   Momento: Antes (escribe en el mismo registro, antes de repository.save).
```

**Verificar:** `./gradlew compileJava` sin errores.

---

## Trazabilidad Origen spec → V/R/U → ubicación

### V — Validaciones

| ID | Origen spec | Capa | Ubicación | Descripción |
|----|-------------|------|-----------|-------------|
| V-SmokeTest-001 | RES-001 | Servidor + Cliente | `SmokeTestServiceImpl.validateInsert` / `SmokeTestServiceImpl.validateUpdate`; `subsysSmokeTest.SmokeTest@Main-btnSave-validate-action` (XML views/SmokeTest.xml) | `texto` no puede ser null ni vacío/blank. Mensaje transmite: que el texto es obligatorio. |

### R — Reglas de negocio

| ID | Origen spec | Momento | Ubicación | Descripción |
|----|-------------|---------|-----------|-------------|
| R-SmokeTest-001 | CC-001 | Antes (insert) | `SmokeTestServiceImpl.fireActionRule_AsignarFechaCreacion` — invocada desde `SmokeTestServiceImpl.insert` antes de `repository.save` | Asignación INCONDICIONAL de `fechaCreacion = LocalDateTime.now()`. Campo servidor; no condicionada con `if`. |
| R-SmokeTest-002 | CC-002 | Antes (insert y update) | `SmokeTestServiceImpl.fireActionRule_ActualizarFechaUltimaModificacion` — invocada desde `SmokeTestServiceImpl.insert` y `SmokeTestServiceImpl.update` antes de `repository.save` | Asignación INCONDICIONAL de `fechaUltimaModificacion = LocalDateTime.now()`. Campo servidor; no condicionada con `if`. |

---

## Seguridad

- **Sin `@CallMethod`:** no hay sección «Frontera de confianza — AllowProperties por acción» porque el diseño no declara ninguna acción invocada desde un `@CallMethod`. La defensa anti mass-assignment se implementa en `allowPropertiesInsert`/`allowPropertiesUpdate` del servicio (whitelists explícitas) y en la asignación incondicional de los campos servidor dentro de `fireActionRule_*`.
- **Sin multi-centro:** no se aplica ningún filtro `centroActivo`. No se usa `AuthUtils.getUser().getCentroActivo()` en queries (la entidad no tiene campo `centro`).
