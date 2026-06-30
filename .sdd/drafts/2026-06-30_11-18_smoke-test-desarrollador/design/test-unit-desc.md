# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Estilo tomado de los tests existentes del proyecto: `NotaServiceImplTest` (servicio `ModelService`: `new <Impl>(<Entidad>.class, repository)`, `repository.save(any())` devuelve el argumento, `I18n` mockeado de forma `LENIENT` devolviendo la clave) y `GrupoControllerTest` (controlador: `ModelServiceFactory` inyectado por reflexión, `ActionRequestHelper`/`ActionResponseHelper` mockeados con `MockedConstruction`).
- Aserciones con `org.junit.jupiter.api.Assertions` (no AssertJ).

## Supuesto sobre el mensaje exacto de V-SmokeTest-001
El diseño deja el literal del mensaje a `/sdd-implementer`, pero el spec (ESC-005) y `test-e2e-desc.md` usan **«El texto es obligatorio»**. Estos tests asumen ese literal exacto. Si `/sdd-implementer` fija otro literal, debe ajustar tanto el código como el `assertEquals` del mensaje. Como en `NotaServiceImplTest`, se mockea `I18n.get(...)` de forma `LENIENT` para que devuelva la cadena recibida, de modo que el mensaje del `BusinessMessages` coincide con el literal pasado a `I18n.get`.

---

## Clase: `com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl`  —  servicio

**Responsabilidad:** servicio `ModelService<SmokeTest>` que (1) valida que el texto sea obligatorio (V-SmokeTest-001) en alta y modificación, (2) sella incondicionalmente las fechas de servidor (`fechaCreacion` en alta, `fechaUltimaModificacion` en alta y en cada modificación; restaura `fechaCreacion` inmutable desde `original` en update), y (3) expone las whitelists `AllowProperties` que dejan fuera ambas fechas (frontera de confianza). Persiste vía `repository.save(...)` sin llamar a `super.insert`/`super.update`.

**Colaboradores a mockear:** `Repository<SmokeTest>` (el repositorio generado por Axelor, p.ej. `SmokeTestRepository`): `save(any())` devuelve el argumento. `I18n` (estático, `mockStatic` `LENIENT`, devuelve la clave). No hay otros servicios, ni `SecurityUtil`, ni multicentro (el spec excluye centro). Entidades `SmokeTest` se instancian con `new SmokeTest()` y setters; nunca se mockean.

**Origen diseño:** `design.md` Paso 2 (firmas de `insert`, `update`, `validateInsert`, `validateUpdate`, `allowPropertiesInsert`, `allowPropertiesUpdate`, y los privados `fireActionRule_*`) + Trazabilidad V/R. Reglas: V-SmokeTest-001 (RES-001), R-SmokeTest-001 (CC-001), R-SmokeTest-002 (CC-002). Los métodos `fireActionRule_*` son privados: se ejercen **indirectamente** a través de `insert`/`update`.

### Método: `Optional<BusinessMessages> validateInsert(SmokeTest smokeTest)`

- **`validateInsert_textoValido_devuelveVacio`** — Tipo: happy. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `SmokeTest` con `texto = "Prueba de humo 1"`.
  - **Act:** `service.validateInsert(smokeTest)`.
  - **Assert:** el `Optional` está vacío (`isEmpty()`).
- **`validateInsert_textoNulo_devuelveError`** — Tipo: error. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `SmokeTest` con `texto = null`.
  - **Act:** `service.validateInsert(smokeTest)`.
  - **Assert:** el `Optional` está presente y el primer mensaje del `BusinessMessages` es exactamente «El texto es obligatorio».
- **`validateInsert_textoVacio_devuelveError`** — Tipo: borde. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `SmokeTest` con `texto = ""` (cadena vacía).
  - **Act:** `service.validateInsert(smokeTest)`.
  - **Assert:** `Optional` presente; mensaje exactamente «El texto es obligatorio».
- **`validateInsert_textoSoloEspacios_devuelveError`** — Tipo: borde. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `SmokeTest` con `texto = "   "` (solo espacios en blanco; la regla comprueba `isBlank`/trim).
  - **Act:** `service.validateInsert(smokeTest)`.
  - **Assert:** `Optional` presente; mensaje exactamente «El texto es obligatorio».

### Método: `Optional<BusinessMessages> validateUpdate(SmokeTest smokeTest, SmokeTest original)`

- **`validateUpdate_textoValido_devuelveVacio`** — Tipo: happy. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `texto = "Prueba de humo 3 editada"`; `original` con `texto = "Prueba de humo 3"`.
  - **Act:** `service.validateUpdate(smokeTest, original)`.
  - **Assert:** `Optional` vacío.
- **`validateUpdate_textoNulo_devuelveError`** — Tipo: error. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `texto = null`; `original` con `texto = "Prueba de humo 3"`.
  - **Act:** `service.validateUpdate(smokeTest, original)`.
  - **Assert:** `Optional` presente; mensaje exactamente «El texto es obligatorio».
- **`validateUpdate_textoSoloEspacios_devuelveError`** — Tipo: borde. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `texto = "  "`; `original` con `texto = "Prueba de humo 3"`.
  - **Act:** `service.validateUpdate(smokeTest, original)`.
  - **Assert:** `Optional` presente; mensaje exactamente «El texto es obligatorio».

### Método: `SmokeTest insert(SmokeTest smokeTest)`

- **`insert_textoValido_sellaAmbasFechasYPersiste`** — Tipo: happy. Verifica: `R-SmokeTest-001`, `R-SmokeTest-002` (y, por estar antes, `V-SmokeTest-001` que no falla).
  - **Arrange:** `SmokeTest` con `texto = "Prueba de humo 1"` y ambas fechas a `null`; `repository.save(any())` devuelve su argumento.
  - **Act:** `service.insert(smokeTest)`.
  - **Assert:** `getFechaCreacion()` y `getFechaUltimaModificacion()` quedan **no nulas** (selladas por el servidor); ambas son cercanas a «ahora» (igual o posterior a un instante capturado antes del Act); `verify(repository).save(smokeTest)`.
- **`insert_clienteIntentaDictarFechas_servidorLasSobrescribe`** — Tipo: borde. Verifica: `R-SmokeTest-001`, `R-SmokeTest-002` (asignación incondicional, sin guarda `if (==null)`).
  - **Arrange:** `SmokeTest` con `texto = "x"`, `fechaCreacion` y `fechaUltimaModificacion` puestas a una fecha falsificada antigua (p.ej. `2000-01-01T00:00`); `repository.save(any())` devuelve el argumento; se captura `LocalDateTime` justo antes del Act.
  - **Act:** `service.insert(smokeTest)`.
  - **Assert:** ambas fechas resultan **distintas** de la fecha falsificada y son `>=` al instante capturado (el servidor las pisa de forma incondicional, no respeta el valor entrante); `verify(repository).save(smokeTest)`.
- **`insert_textoNulo_lanzaValidationExceptionYNoPersiste`** — Tipo: error. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `SmokeTest` con `texto = null`; `I18n.get` devuelve la clave. (No se programa `repository.save` porque no debe invocarse.)
  - **Act:** `service.insert(smokeTest)`.
  - **Assert:** lanza `jakarta.validation.ValidationException` con mensaje exactamente «El texto es obligatorio»; `verify(repository, never()).save(any())`.

### Método: `SmokeTest update(SmokeTest smokeTest, SmokeTest original)`

- **`update_textoValido_refrescaUltimaModificacionYMantieneCreacion`** — Tipo: happy. Verifica: `R-SmokeTest-002` (refresco) y `R-SmokeTest-001` (creación inmutable restaurada desde `original`).
  - **Arrange:** `original` con `texto = "Prueba de humo 3"`, `fechaCreacion = 2024-01-01T10:00`, `fechaUltimaModificacion = 2024-01-01T10:00`; `smokeTest` con `texto = "Prueba de humo 3 editada"`; se captura `LocalDateTime` antes del Act; `repository.save(any())` devuelve el argumento.
  - **Act:** `service.update(smokeTest, original)`.
  - **Assert:** `smokeTest.getFechaCreacion()` es **igual** a la del `original` (`2024-01-01T10:00`, inmutable, restaurada); `smokeTest.getFechaUltimaModificacion()` es **no nula** y `>=` al instante capturado (refrescada, posterior a la previa); `verify(repository).save(smokeTest)`.
- **`update_clienteManipulaFechaCreacion_seRestauraDesdeOriginal`** — Tipo: borde. Verifica: `R-SmokeTest-001` (restauración del inmutable).
  - **Arrange:** `original` con `fechaCreacion = 2024-01-01T10:00`; `smokeTest` con `texto = "x"` y `fechaCreacion` manipulada a `2099-12-31T23:59`; `repository.save(any())` devuelve el argumento.
  - **Act:** `service.update(smokeTest, original)`.
  - **Assert:** `smokeTest.getFechaCreacion()` es **igual** a la del `original` (`2024-01-01T10:00`), no a la manipulada (el servidor restaura el inmutable de forma incondicional); `verify(repository).save(smokeTest)`.
- **`update_textoNulo_lanzaValidationExceptionYNoPersiste`** — Tipo: error. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `texto = null`; `original` con `texto = "Prueba de humo 3"`; `I18n.get` devuelve la clave. (No se programa `repository.save`.)
  - **Act:** `service.update(smokeTest, original)`.
  - **Assert:** lanza `jakarta.validation.ValidationException` con mensaje exactamente «El texto es obligatorio»; `verify(repository, never()).save(any())`.

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_incluyeSoloTexto`** — Tipo: happy. Verifica: `—` (frontera de confianza, k-secure-coding §3; respalda V/R al excluir campos servidor).
  - **Arrange:** ninguno (no necesita mocks; método puro sobre la whitelist).
  - **Act:** `service.allowPropertiesInsert()`.
  - **Assert:** `allowProperty("texto")` es `true`; `allowProperty("fechaCreacion")` y `allowProperty("fechaUltimaModificacion")` son `false`.

### Método: `AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_incluyeSoloTexto`** — Tipo: happy. Verifica: `—` (frontera de confianza; `fechaCreacion` inmutable y `fechaUltimaModificacion` recalculada quedan fuera).
  - **Arrange:** ninguno.
  - **Act:** `service.allowPropertiesUpdate()`.
  - **Assert:** `allowProperty("texto")` es `true`; `allowProperty("fechaCreacion")` y `allowProperty("fechaUltimaModificacion")` son `false`.

> Los métodos privados `fireActionRule_AsignarFechaCreacion`, `fireActionRule_AsignarFechaUltimaModificacion` y `fireActionRule_RefrescarFechaModificacion` no se testean directamente (son privados): su efecto (R-SmokeTest-001 y R-SmokeTest-002) queda cubierto por los tests de `insert` y `update` anteriores.

---

## Clase: `com.educaflow.subsystem.smoketest.controller.SmokeTestController`  —  controlador

**Responsabilidad:** `@CallMethod validateSave` que pre-valida el guardado antes de la acción `save`. Resuelve `SmokeTestService` vía `ModelServiceFactory.resolve(SmokeTest.class)`, construye `ActionRequestHelper`/`ActionResponseHelper`, decide alta vs. modificación según `actionRequestHelper.getId()`, llama a `validateInsert` (alta) o `validateUpdate` (modificación) con la whitelist correspondiente y, si hay error, responde con `doResponseBusinessMessagesAsError`. No persiste.

**Colaboradores a mockear:** `ModelServiceFactory` (inyectado por reflexión en el campo `modelServiceFactory`, como en `GrupoControllerTest`); `SmokeTestService` (mock; `modelServiceFactory.resolve(SmokeTest.class)` lo devuelve); `ActionRequest`/`ActionResponse` (mocks); `ActionRequestHelper` y `ActionResponseHelper` mockeados con `Mockito.mockConstruction` (programando `getId()`, `getOriginalModel()`, `getModel(any())` en el request-helper; verificando `doResponseBusinessMessagesAsError` en el response-helper). Las whitelists `allowPropertiesInsert()`/`allowPropertiesUpdate()` del servicio mockeado pueden devolver `null` (solo se pasan a `getModel(any())`, que está stubbeado con `any()`).

**Origen diseño:** `design.md` Paso 4 (`validateSave`). No hay `validateDelete`. Reglas que ejerce: V-SmokeTest-001 (delega en `validateInsert`/`validateUpdate` del servicio).

### Método: `void validateSave(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`validateSave_altaTextoValido_noRespondeError`** — Tipo: happy. Verifica: `V-SmokeTest-001` (rama alta sin error).
  - **Arrange:** request-helper construido con `getId()` → `null` (alta), `getOriginalModel()` → `null`, `getModel(any())` → un `SmokeTest` con texto válido; `smokeTestService.validateInsert(smokeTest)` → `Optional.empty()`.
  - **Act:** `controller.validateSave(actionRequest, actionResponse)`.
  - **Assert:** `verify(smokeTestService).validateInsert(smokeTest)`; el response-helper construido **nunca** invoca `doResponseBusinessMessagesAsError(any())` (`never()`); `verify(smokeTestService, never()).validateUpdate(any(), any())`.
- **`validateSave_altaTextoVacio_respondeBusinessMessagesError`** — Tipo: error. Verifica: `V-SmokeTest-001` (rama alta con error).
  - **Arrange:** request-helper con `getId()` → `null`, `getModel(any())` → `SmokeTest` con texto vacío; `businessMessages` mock; `smokeTestService.validateInsert(smokeTest)` → `Optional.of(businessMessages)`.
  - **Act:** `controller.validateSave(actionRequest, actionResponse)`.
  - **Assert:** `verify(responseHelper).doResponseBusinessMessagesAsError(businessMessages)`.
- **`validateSave_modificacionTextoValido_noRespondeError`** — Tipo: happy. Verifica: `V-SmokeTest-001` (rama modificación sin error).
  - **Arrange:** request-helper con `getId()` → un id no nulo (p.ej. `1L`), `getOriginalModel()` → `original` (`SmokeTest`), `getModel(any())` → `smokeTest` con texto válido; `smokeTestService.validateUpdate(smokeTest, original)` → `Optional.empty()`.
  - **Act:** `controller.validateSave(actionRequest, actionResponse)`.
  - **Assert:** `verify(smokeTestService).validateUpdate(smokeTest, original)`; response-helper **nunca** llama `doResponseBusinessMessagesAsError(any())`; `verify(smokeTestService, never()).validateInsert(any())`.
- **`validateSave_modificacionTextoVacio_respondeBusinessMessagesError`** — Tipo: error. Verifica: `V-SmokeTest-001` (rama modificación con error).
  - **Arrange:** request-helper con `getId()` → `1L`, `getOriginalModel()` → `original`, `getModel(any())` → `smokeTest` con texto vacío; `businessMessages` mock; `smokeTestService.validateUpdate(smokeTest, original)` → `Optional.of(businessMessages)`.
  - **Act:** `controller.validateSave(actionRequest, actionResponse)`.
  - **Assert:** `verify(responseHelper).doResponseBusinessMessagesAsError(businessMessages)`; `verify(smokeTestService, never()).validateInsert(any())`.

---

## Clase: `com.educaflow.subsystem.smoketest.service.SmokeTestService` — sin lógica testable
**Motivo:** interfaz de marcador (`extends ModelService<SmokeTest>`) sin métodos propios (design.md Paso 2). No hay comportamiento que testear.

## Clase: `com.educaflow.subsystem.smoketest.db.SmokeTest` — sin lógica testable
**Motivo:** entidad de dominio generada por Axelor a partir de `domains/SmokeTest.xml` (POJO con getters/setters de `texto`, `fechaCreacion`, `fechaUltimaModificacion`); sin lógica propia ni campos calculados con cálculo en la entidad (el sellado lo hace el servicio). Su `SmokeTestRepository` también es generado, sin queries propias.

---

## Cobertura
- Clases con lógica descritas: 2 (`SmokeTestServiceImpl`, `SmokeTestController`).
- Clases omitidas (sin lógica): `SmokeTestService` (interfaz marcador), `SmokeTest` (POJO de dominio generado por Axelor) y su `SmokeTestRepository` (repositorio generado, sin queries).
- Reglas server-side cubiertas (`V`/`R`/`CC`):
  - `V-SmokeTest-001` — `validateInsert_*`, `validateUpdate_*`, `insert_textoNulo_*`, `update_textoNulo_*`, y las cuatro ramas de `validateSave_*`.
  - `R-SmokeTest-001` (CC-001, `fechaCreacion`) — `insert_textoValido_sellaAmbasFechasYPersiste`, `insert_clienteIntentaDictarFechas_servidorLasSobrescribe`, `update_textoValido_refrescaUltimaModificacionYMantieneCreacion`, `update_clienteManipulaFechaCreacion_seRestauraDesdeOriginal`.
  - `R-SmokeTest-002` (CC-002, `fechaUltimaModificacion`) — `insert_textoValido_sellaAmbasFechasYPersiste`, `insert_clienteIntentaDictarFechas_servidorLasSobrescribe`, `update_textoValido_refrescaUltimaModificacionYMantieneCreacion`.
  - Frontera de confianza (whitelists, k-secure-coding §3) — `allowPropertiesInsert_incluyeSoloTexto`, `allowPropertiesUpdate_incluyeSoloTexto` (excluyen ambas fechas, respaldando R-SmokeTest-001/002).
- Reglas solo-cliente excluidas (E2E en test-e2e-desc.md):
  - `U-smoke-test-001` (RUI-001) — `readonly="true"` de `fechaCreacion`/`fechaUltimaModificacion` en el form (capa vista, no testable en JUnit).
  - `U-smoke-test-002` (Ordenación por defecto) — `orderBy="-fechaCreacion"` del grid (capa vista, no testable en JUnit).
