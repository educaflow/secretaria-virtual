---
type: implementation-task
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/smoketest/service/impl/SmokeTestServiceImplTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita (p.ej. la descripción dice
      `insert(X)` y el código tiene `guardarX(X, Long)`), o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.

---

## Referencia a la descripción de tests

La descripción completa de los tests a implementar está en `design/test-unit-desc.md`, sección:

**Clase: `com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl` — servicio**

Tests a implementar (15 en total):

- `allowPropertiesInsert_incluyeSoloTexto`
- `allowPropertiesUpdate_incluyeSoloTexto`
- `validateInsert_textoValido_devuelveVacio`
- `validateInsert_textoNulo_devuelveError`
- `validateInsert_textoVacio_devuelveError`
- `validateInsert_textoSoloEspacios_devuelveError`
- `validateUpdate_textoValido_devuelveVacio`
- `validateUpdate_textoNulo_devuelveError`
- `validateUpdate_textoSoloEspacios_devuelveError`
- `insert_textoValido_sellaAmbasFechasYPersiste`
- `insert_clienteIntentaDictarFechas_servidorLasSobrescribe`
- `insert_textoNulo_lanzaValidationExceptionYNoPersiste`
- `update_textoValido_refrescaUltimaModificacionYMantieneCreacion`
- `update_clienteManipulaFechaCreacion_seRestauraDesdeOriginal`
- `update_textoNulo_lanzaValidationExceptionYNoPersiste`

**Colaboradores a mockear:**
- `SmokeTestRepository` (o `AbstractSmokeTestRepository`): mock de Mockito — `repository.save(any())` programado para devolver el mismo argumento (thenAnswer → invocation.getArgument(0)).
- `I18n.get(String)`: `Mockito.mockStatic(I18n.class, LENIENT)` abierto en `@BeforeEach` y cerrado en `@AfterEach` — responde devolviendo el propio string de entrada, de modo que los mensajes comparados en los asserts son el literal original del spec.
