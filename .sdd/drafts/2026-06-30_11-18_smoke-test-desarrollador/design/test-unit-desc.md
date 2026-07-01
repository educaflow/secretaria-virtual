# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.

---

## Clase: `com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl`  —  servicio

**Responsabilidad:** Implementación del servicio CRUD de SmokeTest. Valida que `texto` no sea nulo ni vacío/blank antes de persistir (V-SmokeTest-001). Asigna incondicionalmente `fechaCreacion` al insertar (R-SmokeTest-001) y `fechaUltimaModificacion` al insertar y al actualizar (R-SmokeTest-002). Restaura `fechaCreacion` desde `original` al actualizar para impedir que el cliente la manipule (defensa anti-mass-assignment). Define whitelists de campos cliente permitidos en `allowPropertiesInsert`/`allowPropertiesUpdate`.

**Colaboradores a mockear:**
- `SmokeTestRepository` (o `AbstractSmokeTestRepository`): mock de Mockito — `repository.save(any())` programado para devolver el mismo argumento (thenAnswer → invocation.getArgument(0)).
- `I18n.get(String)`: `Mockito.mockStatic(I18n.class, LENIENT)` abierto en `@BeforeEach` y cerrado en `@AfterEach` — responde devolviendo el propio string de entrada, de modo que los mensajes comparados en los asserts son el literal original del spec.

**Origen diseño:** design.md Paso 2 — métodos `allowPropertiesInsert`, `allowPropertiesUpdate`, `validateInsert`, `validateUpdate`, `insert`, `update`, y los métodos privados `fireActionRule_AsignarFechaCreacion` y `fireActionRule_ActualizarFechaUltimaModificacion` (testeados indirectamente a través de `insert` y `update`).

---

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_incluyeSoloTexto`** — Tipo: happy. Verifica: `—` (defensa anti-mass-assignment; excluye campos servidor de la whitelist de insert).
  - **Arrange:** instancia del servicio con el repository mockeado (sin programar comportamiento adicional).
  - **Act:** `service.allowPropertiesInsert()`.
  - **Assert:** `allowProperties.allowProperty("texto")` → `true`; `allowProperties.allowProperty("fechaCreacion")` → `false`; `allowProperties.allowProperty("fechaUltimaModificacion")` → `false`.

---

### Método: `AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_incluyeSoloTexto`** — Tipo: happy. Verifica: `—` (defensa anti-mass-assignment; excluye campos servidor de la whitelist de update).
  - **Arrange:** instancia del servicio con el repository mockeado.
  - **Act:** `service.allowPropertiesUpdate()`.
  - **Assert:** `allowProperties.allowProperty("texto")` → `true`; `allowProperties.allowProperty("fechaCreacion")` → `false`; `allowProperties.allowProperty("fechaUltimaModificacion")` → `false`.

---

### Método: `Optional<BusinessMessages> validateInsert(SmokeTest entity)`

- **`validateInsert_textoValido_devuelveVacio`** — Tipo: happy. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `new SmokeTest()` con `setTexto("Prueba de humo 1")`.
  - **Act:** `service.validateInsert(smokeTest)`.
  - **Assert:** el `Optional` devuelto está vacío (`isEmpty()` → `true`).

- **`validateInsert_textoNulo_devuelveError`** — Tipo: error. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `new SmokeTest()` con `setTexto(null)`. Mock estático de `I18n.get(any())` → devuelve el mismo string (ya configurado en `@BeforeEach`).
  - **Act:** `service.validateInsert(smokeTest)`.
  - **Assert:** el `Optional` está presente; el mensaje del primer `BusinessMessage` es `"El texto es obligatorio"` (exactamente como define el spec ESC-005 y V-SmokeTest-001).

- **`validateInsert_textoVacio_devuelveError`** — Tipo: borde. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `new SmokeTest()` con `setTexto("")`.
  - **Act:** `service.validateInsert(smokeTest)`.
  - **Assert:** el `Optional` está presente; mensaje → `"El texto es obligatorio"`.

- **`validateInsert_textoSoloEspacios_devuelveError`** — Tipo: borde. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `new SmokeTest()` con `setTexto("   ")`.
  - **Act:** `service.validateInsert(smokeTest)`.
  - **Assert:** el `Optional` está presente; mensaje → `"El texto es obligatorio"`.

---

### Método: `Optional<BusinessMessages> validateUpdate(SmokeTest entity, SmokeTest original)`

- **`validateUpdate_textoValido_devuelveVacio`** — Tipo: happy. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `setTexto("Prueba de humo 3 editada")`; `original` con `setTexto("Prueba de humo 3")`.
  - **Act:** `service.validateUpdate(smokeTest, original)`.
  - **Assert:** el `Optional` está vacío.

- **`validateUpdate_textoNulo_devuelveError`** — Tipo: error. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `setTexto(null)`; `original` con `setTexto("Prueba de humo 3")`.
  - **Act:** `service.validateUpdate(smokeTest, original)`.
  - **Assert:** el `Optional` está presente; mensaje → `"El texto es obligatorio"`.

- **`validateUpdate_textoSoloEspacios_devuelveError`** — Tipo: borde. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `setTexto("  ")`; `original` con `setTexto("Prueba de humo 3")`.
  - **Act:** `service.validateUpdate(smokeTest, original)`.
  - **Assert:** el `Optional` está presente; mensaje → `"El texto es obligatorio"`.

---

### Método: `SmokeTest insert(SmokeTest entity)`

- **`insert_textoValido_sellaAmbasFechasYPersiste`** — Tipo: happy. Verifica: `R-SmokeTest-001`, `R-SmokeTest-002`.
  - **Arrange:** `smokeTest` con `setTexto("Prueba de humo 1")`. `repository.save(any())` → devuelve el mismo argumento. Anotar `LocalDateTime antes = LocalDateTime.now()` justo antes de la invocación.
  - **Act:** `service.insert(smokeTest)`.
  - **Assert:** `smokeTest.getFechaCreacion()` no es nulo y no es anterior a `antes` (asignado incondicionalmente por R-SmokeTest-001); `smokeTest.getFechaUltimaModificacion()` no es nulo y no es anterior a `antes` (asignado incondicionalmente por R-SmokeTest-002); `verify(repository).save(smokeTest)`.

- **`insert_clienteIntentaDictarFechas_servidorLasSobrescribe`** — Tipo: borde (seguridad anti-mass-assignment). Verifica: `R-SmokeTest-001`, `R-SmokeTest-002`.
  - **Arrange:** `smokeTest` con `setTexto("x")`, `setFechaCreacion(LocalDateTime.of(2000,1,1,0,0))`, `setFechaUltimaModificacion(LocalDateTime.of(2000,1,1,0,0))`. `repository.save(any())` → devuelve el mismo argumento. Anotar `antes = LocalDateTime.now()`.
  - **Act:** `service.insert(smokeTest)`.
  - **Assert:** `smokeTest.getFechaCreacion()` no es igual a la fecha falsa (2000-01-01T00:00) y no es anterior a `antes` — el servidor la sobrescribió incondicionalmente; `smokeTest.getFechaUltimaModificacion()` ídem; `verify(repository).save(smokeTest)`.

- **`insert_textoNulo_lanzaValidationExceptionYNoPersiste`** — Tipo: error. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `setTexto(null)`.
  - **Act:** `service.insert(smokeTest)`.
  - **Assert:** lanza `ValidationException` con mensaje `"El texto es obligatorio"`; `verify(repository, never()).save(any())` — el repositorio no se invoca cuando la validación falla.

---

### Método: `SmokeTest update(SmokeTest entity, SmokeTest original)`

- **`update_textoValido_refrescaUltimaModificacionYMantieneCreacion`** — Tipo: happy. Verifica: `R-SmokeTest-002`.
  - **Arrange:** `original` con `setTexto("Prueba de humo 3")` y `setFechaCreacion(LocalDateTime.of(2024,1,1,10,0))`; `smokeTest` con `setTexto("Prueba de humo 3 editada")`. `repository.save(any())` → devuelve el mismo argumento. Anotar `antes = LocalDateTime.now()`.
  - **Act:** `service.update(smokeTest, original)`.
  - **Assert:** `smokeTest.getFechaCreacion()` igual a `2024-01-01T10:00` (restaurada desde `original`, no modificada); `smokeTest.getFechaUltimaModificacion()` no es nulo y no es anterior a `antes` (recalculada incondicionalmente por R-SmokeTest-002); `verify(repository).save(smokeTest)`.

- **`update_clienteManipulaFechaCreacion_seRestauraDesdeOriginal`** — Tipo: borde (seguridad; campo inmutable). Verifica: `—` (defensa anti-mass-assignment sobre el campo inmutable `fechaCreacion`; restauración inline en `update`, sin regla `R-` nombrada en el diseño).
  - **Arrange:** `original` con `setFechaCreacion(LocalDateTime.of(2024,1,1,10,0))` y `setTexto("Prueba de humo 3")`; `smokeTest` con `setTexto("x")` y `setFechaCreacion(LocalDateTime.of(2099,12,31,23,59))` (fecha falsa inyectada por el cliente). `repository.save(any())` → devuelve el mismo argumento.
  - **Act:** `service.update(smokeTest, original)`.
  - **Assert:** `smokeTest.getFechaCreacion()` igual a la del `original` (`2024-01-01T10:00`) — la restauración incondicional descarta la fecha falsificada; `verify(repository).save(smokeTest)`.

- **`update_textoNulo_lanzaValidationExceptionYNoPersiste`** — Tipo: error. Verifica: `V-SmokeTest-001`.
  - **Arrange:** `smokeTest` con `setTexto(null)`; `original` con `setTexto("Prueba de humo 3")`.
  - **Act:** `service.update(smokeTest, original)`.
  - **Assert:** lanza `ValidationException` con mensaje `"El texto es obligatorio"`; `verify(repository, never()).save(any())`.

---

## Clase: `com.educaflow.subsystem.smoketest.service.SmokeTestService` — sin lógica testable
**Motivo:** Interfaz pura que extiende `ModelService<SmokeTest>`. No declara métodos adicionales con lógica propia; las sobrescrituras con lógica viven en `SmokeTestServiceImpl`.

---

## Clase: `com.educaflow.subsystem.smoketest.db.SmokeTest` — sin lógica testable
**Motivo:** POJO de dominio generado por Axelor a partir de `domains/SmokeTest.xml`. No contiene métodos con lógica de negocio propia: solo getters/setters generados y los campos `texto`, `fechaCreacion` y `fechaUltimaModificacion`. Los campos calculados (R-SmokeTest-001 y R-SmokeTest-002) se testan en `SmokeTestServiceImpl`, donde reside la lógica de asignación.

---

## Cobertura
- Clases con lógica descritas: 1 (`SmokeTestServiceImpl`).
- Clases omitidas (sin lógica): `SmokeTestService` (interfaz sin lógica), `SmokeTest` (POJO de dominio Axelor).
- Reglas server-side cubiertas (`V`/`R`/`CC`): `V-SmokeTest-001` (tests `validateInsert_*`, `validateUpdate_*`, `insert_textoNulo_*`, `update_textoNulo_*`); `R-SmokeTest-001` (tests `insert_textoValido_*`, `insert_clienteIntentaDictar_*`); `R-SmokeTest-002` (tests `insert_textoValido_*`, `insert_clienteIntentaDictar_*`, `update_textoValido_*`).
- Reglas solo-cliente excluidas (E2E en test-e2e-desc.md): `U-smoke-test-001` (campos `fechaCreacion` y `fechaUltimaModificacion` con `readonly="true"` en el formulario — regla de UI exclusivamente de la capa XML/cliente, no testeable con JUnit).
