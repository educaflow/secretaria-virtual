---
type: implementation-task
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
(sección «Clase: `com.educaflow.subsystem.criptografia.db.CertificadoDigital` — entidad de dominio generada (sección excepcional, ver Decisión 1)»,
método `Boolean getEnabled()`: 2 tests —
`getEnabled_entidadRecienCreadaSinIndicarValor_devuelveTrue`,
`getEnabled_valorNull_devuelveFalse`)
para la clase `com.educaflow.subsystem.criptografia.db.CertificadoDigital`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/criptografia/db/CertificadoDigitalTest.java`.
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

Contexto de la clase bajo test (verbatim de `design/test-unit-desc.md`): entidad JPA **generada** por Axelor desde `domains/CertificadoDigital.xml` (el dominio es una fila `Acción: Modificar`; la entidad generada se regenera en el build a partir del XML colocado por la Tarea 01 — la superficie preexistente de la entidad NO es motivo de BLOCKED). Sin lógica propia salvo el comportamiento **generado a partir del delta del diseño**: el valor por defecto `TRUE` del campo `enabled` (materialización declarativa de R-CertificadoDigital-001) y el colapso de NULL a FALSE en el getter (hecho verificado del Paso 1 en el que se apoya la condición del Paso 2). Solo se describen estos dos comportamientos; el resto de la entidad queda sin tests (POJO). Colaboradores a mockear: ninguno (entidad instanciada con `new`; sin mocks).
