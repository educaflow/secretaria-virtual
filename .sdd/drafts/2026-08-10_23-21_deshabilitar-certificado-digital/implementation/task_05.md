---
type: implementation-task
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
(sección «Clase: `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl` — servicio»,
método `AlmacenClave getAlmacenClaveByDni(String dni)`: 4 tests —
`getAlmacenClaveByDni_entradaDeshabilitada_lanzaMismaExcepcionQueInexistente`,
`getAlmacenClaveByDni_entradaInexistente_lanzaExcepcionNoExisteCertificado`,
`getAlmacenClaveByDni_entradaHabilitada_devuelveAlmacenClave`,
`getAlmacenClaveByDni_entradaConEnabledPorDefecto_devuelveAlmacenClave`)
para la clase `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImplTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita (p.ej. la descripción dice
      `insert(X)` y el código tiene `guardarX(X, Long)`), o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más). En una clase
      que el diseño **modifica** (fila `Acción: Modificar` — ya existía antes de la iniciativa), este criterio se
      acota a la superficie **nueva/cambiada**: los métodos públicos **preexistentes** de la clase NO son motivo
      de BLOCKED (puedes leer el fichero real, o su `git diff`, para distinguirlos).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.

Contexto de la clase bajo test (verbatim de `design/test-unit-desc.md`): la clase bajo test es una clase que el diseño **modifica** (`Acción: Modificar` — Paso 2 del diseño); solo se testea el delta de `getAlmacenClaveByDni`. Colaboradores a mockear: `CertificadoDigitalRepository` (mock de Mockito; **debe ser del tipo concreto** `CertificadoDigitalRepository`, no `Repository`, porque el método hace el cast `((CertificadoDigitalRepository) repository).findByDni(dni)`). Se inyecta por el constructor existente: `new CertificadoDigitalServiceImpl(CertificadoDigital.class, repositoryMock)`. No hacen falta estáticos (`I18n`, `SecurityUtil`, `Beans`): el mensaje de la excepción está literal en el método y `validateGetAlmacenClaveByDni` (que devuelve `Optional.empty()`) se ejecuta real sin colaboradores.
