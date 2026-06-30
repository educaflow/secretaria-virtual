---
type: implementation-task
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-validaciones
- k-code-quality

Esta tarea implementa el servicio del subsistema: la **interfaz** `SmokeTestService` y su **implementación** `SmokeTestServiceImpl` (fuertemente acoplados → una sola tarea). Es código Java: se materializa a partir de las firmas y comentarios de este diseño.

Filas de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/service/SmokeTestService.java` | Crear | k-sistemas (servicios.md) | Interfaz `SmokeTestService extends ModelService<SmokeTest>` (sin métodos propios) |
| `subsystem/smoketest/service/impl/SmokeTestServiceImpl.java` | Crear | k-sistemas (servicios.md), k-secure-coding, k-validaciones | Sella fechas servidor, valida texto, whitelists |

> Raíz de los ficheros del subsistema: `src/main/java/com/educaflow/subsystem/smoketest/`.

### Paso 2 — Servicio `SmokeTestService` / `SmokeTestServiceImpl`

**Interfaz** `com.educaflow.subsystem.smoketest.service.SmokeTestService`:

```java
public interface SmokeTestService extends ModelService<SmokeTest> { }
```
- No declara métodos propios. `insert`/`update`/`remove`, sus `validateInsert/Update/Remove` y sus `allowPropertiesInsert/Update/Remove` se **heredan** de `ModelService<SmokeTest>` (defaults en `DefaultModelService`). **MUST NOT** re-declararlos (servicios.md).

**Implementación** `com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl extends DefaultModelService<SmokeTest> implements SmokeTestService`.

Orden de bloques: (1) acciones, (2) Métodos de Validación, (3) AllowProperties, (4) Action Rules.

```java
// Constructor obligatorio — lo invoca ModelServiceFactory por reflexión.
public SmokeTestServiceImpl(Class<SmokeTest> model, Repository<SmokeTest> repository);
//   super(model, repository);

// --- (1) Acciones ---

public SmokeTest insert(SmokeTest smokeTest);
//   Sobrescribe insert (genérico: lo invoca la acción `save`/endpoint REST /ws/rest/<FQN>).
//   1ª línea: validateInsert(smokeTest).ifPresent(BusinessMessages::throwIfInvalid)  → V-SmokeTest-001.
//   Aplica R-SmokeTest-001 vía fireActionRule_AsignarFechaCreacion(smokeTest) y
//   R-SmokeTest-002 vía fireActionRule_AsignarFechaUltimaModificacion(smokeTest) ANTES de persistir.
//   Persiste con repository.save(smokeTest). MUST NOT llamar a super.insert.

public SmokeTest update(SmokeTest smokeTest, SmokeTest original);
//   Sobrescribe update (genérico: lo invoca `save`/endpoint REST).
//   1ª línea: validateUpdate(smokeTest, original).ifPresent(BusinessMessages::throwIfInvalid)  → V-SmokeTest-001.
//   Aplica R-SmokeTest-002 + restauración del inmutable vía
//   fireActionRule_RefrescarFechaModificacion(smokeTest, original) ANTES de persistir.
//   Persiste con repository.save(smokeTest). MUST NOT llamar a super.update.

// remove: NO se sobrescribe (sin regla de borrado). Lo hereda de DefaultModelService.

// --- (2) Métodos de Validación ---

public Optional<BusinessMessages> validateInsert(SmokeTest smokeTest);
//   Aplica:
//     - V-SmokeTest-001 (Origen spec: RES-001) texto obligatorio: comprueba que `texto` no sea
//       null ni esté en blanco (trim). Mensaje debe transmitir: que el texto es obligatorio
//       (campo `texto`). [Literal exacto lo fija /sdd-implementer; el spec/test usa "El texto es obligatorio".]
//   Acumula en BusinessMessages y devuelve Optional.empty() si válido.

public Optional<BusinessMessages> validateUpdate(SmokeTest smokeTest, SmokeTest original);
//   Aplica:
//     - V-SmokeTest-001 (Origen spec: RES-001) texto obligatorio: misma comprobación que en
//       validateInsert (el texto sigue siendo obligatorio al modificar).

// --- (3) AllowProperties (frontera de confianza; ver §Frontera de confianza) ---

public AllowProperties allowPropertiesInsert();
//   Whitelist: createAllowProperties(Map.of("texto", Map.of())).
//   `fechaCreacion` y `fechaUltimaModificacion` quedan FUERA (campos servidor, ver k-secure-coding §3.2).

public AllowProperties allowPropertiesUpdate();
//   Whitelist: createAllowProperties(Map.of("texto", Map.of())).
//   Las dos fechas quedan FUERA: `fechaCreacion` es inmutable y `fechaUltimaModificacion` la
//   recalcula el servidor; el cliente no puede dictarlas ni por Vía A ni por Vía B.

// --- (4) Action Rules ---

private void fireActionRule_AsignarFechaCreacion(SmokeTest smokeTest);
//   Aplica R-SmokeTest-001 (Origen spec: CC-001, campo `fechaCreacion` clasificado servidor) en el alta:
//   asignación INCONDICIONAL `smokeTest.setFechaCreacion(LocalDateTime.now())`.
//   MUST NOT añadir guarda `if (... == null)`: permitiría colar una fecha falsificada por el
//   endpoint REST genérico (k-secure-coding §3.3). El cliente NO puede dictar este campo.

private void fireActionRule_AsignarFechaUltimaModificacion(SmokeTest smokeTest);
//   Aplica R-SmokeTest-002 (Origen spec: CC-002, campo `fechaUltimaModificacion` servidor) en el alta:
//   asignación INCONDICIONAL `smokeTest.setFechaUltimaModificacion(LocalDateTime.now())`.
//   MUST NOT añadir guarda `if (... == null)`: permitiría colar una fecha falsificada por el
//   endpoint REST genérico (k-secure-coding §3.3). El cliente NO puede dictar este campo.

private void fireActionRule_RefrescarFechaModificacion(SmokeTest smokeTest, SmokeTest original);
//   Aplica R-SmokeTest-002 (Origen spec: CC-002) en la modificación:
//   asignación INCONDICIONAL `smokeTest.setFechaUltimaModificacion(LocalDateTime.now())`.
//   Restaura el inmutable `fechaCreacion`: `smokeTest.setFechaCreacion(original.getFechaCreacion())`
//   (R-SmokeTest-001 fija la fecha de creación solo en el alta; en update NO se recalcula).
//   Asignaciones INCONDICIONALES, sin `if`. El cliente NO puede dictar ninguna de las dos fechas.
```

> **MUST NOT** crear módulo Guice: `SmokeTestServiceImpl` es un `ModelService` y `ModelServiceFactory` lo descubre por convención de nombre/paquete (servicios.md). No hay repositorio personalizado (sin queries propias) → sin `db/repo/`.

Verificar: `ModelServiceFactory.resolve(SmokeTest.class)` devuelve la impl (nombre exacto `SmokeTestServiceImpl` en `service.impl`). `./run.sh` compila.

### Paso 5 — Módulos Guice

No aplica (solo hay un `ModelService`, descubierto por la factoría). Sin `module/`.

## Frontera de confianza — AllowProperties por acción

El único `@CallMethod` del diseño es `SmokeTestController.validateSave`, que pre-valida el guardado consumiendo `allowPropertiesInsert()` (rama alta) y `allowPropertiesUpdate()` (rama modificación) del servicio. Esas mismas whitelists son la defensa del flujo de guardado genérico (`save` / `POST /ws/rest/<FQN>`), que filtra el JSON entrante con ellas antes de llegar a `insert`/`update`. Reglas aplicadas: `k-secure-coding` §3.

### `SmokeTestServiceImpl.insert` (whitelist consumida por `SmokeTestController.validateSave`, rama alta)

Entidad: `SmokeTest`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-SmokeTest.md` → `texto`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|---------------------------------------------|
| `texto` | cliente | sí | Input directo del usuario (en `Input AllowProperties` de `Crear`). |
| `fechaCreacion` | servidor | **NO** | CC-001. Asignada incondicionalmente en `insert` → `fireActionRule_AsignarFechaCreacion`. |
| `fechaUltimaModificacion` | servidor | **NO** | CC-002. Asignada incondicionalmente en `insert` → `fireActionRule_AsignarFechaUltimaModificacion`. |

### `SmokeTestServiceImpl.update` (whitelist consumida por `SmokeTestController.validateSave`, rama modificación)

Entidad: `SmokeTest`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-SmokeTest.md` → `texto`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|---------------------------------------------|
| `texto` | cliente | sí | Input directo del usuario (en `Input AllowProperties` de `Modificar`). |
| `fechaCreacion` | servidor | **NO** | Inmutable (no aparece en `Modificar`). Restaurada desde `original` en `update` → `fireActionRule_RefrescarFechaModificacion`. |
| `fechaUltimaModificacion` | servidor | **NO** | CC-002. Recalculada incondicionalmente en `update` → `fireActionRule_RefrescarFechaModificacion`. |

## Trazabilidad Origen spec → V/R que implementa esta tarea

### Validaciones (V)

| V | Origen spec | Ubicación | Lógica / Mensaje |
|---|-------------|-----------|------------------|
| V-SmokeTest-001 | RES-001 | `SmokeTestServiceImpl.validateInsert` y `.validateUpdate` | `texto` no null ni en blanco; mensaje transmite que el texto es obligatorio (campo `texto`). Servidor = fuente de verdad (ESC-005). |

### Reglas de negocio / campos calculados (R)

| R | Origen spec | Ubicación | Momento / Efecto |
|---|-------------|-----------|------------------|
| R-SmokeTest-001 | CC-001 | `SmokeTestServiceImpl.fireActionRule_AsignarFechaCreacion` (desde `insert`) | Antes de `repository.save`. Asigna INCONDICIONALMENTE `fechaCreacion = now` solo en el alta; en `update` se restaura desde `original` (inmutable). Campo servidor. |
| R-SmokeTest-002 | CC-002 | `SmokeTestServiceImpl.fireActionRule_AsignarFechaUltimaModificacion` (insert) y `fireActionRule_RefrescarFechaModificacion` (update) | Antes de `repository.save`. Asigna INCONDICIONALMENTE `fechaUltimaModificacion = now` en alta y en cada modificación. Campo servidor. |

## Notas relevantes (del diseño)

1. **RES-001 implementada en servidor, no como `required="true"`.** El spec (ESC-005) exige que el alta sin texto sea **rechazada por el servidor** con el mensaje exacto «El texto es obligatorio». `required="true"` en el modelo produce el mensaje **estándar** de Axelor (no el literal) y actúa como validación de cliente. Para honrar el literal y la frase "rechazada por el servidor" se implementa como `V-SmokeTest-001` en `validateInsert`/`validateUpdate` (k-validaciones permite el camino `validate*` cuando la restricción no se puede declarar con el mensaje requerido). `validateSave` del controlador la muestra como modal antes del `save`, y además la valida el flujo genérico (Vía B) — defensa en profundidad (k-secure-coding §9).
