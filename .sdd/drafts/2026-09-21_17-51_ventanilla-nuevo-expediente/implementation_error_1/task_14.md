---
type: implementation-task
template: system
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.system.ventanilla.service.impl.AsistenteNuevoExpedienteServiceImpl`.

La sección concreta que describe esta clase es **«Clase: com.educaflow.system.ventanilla.service.impl.AsistenteNuevoExpedienteServiceImpl — servicio»** de
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/test-unit-desc.md`. Aplican además,
de ese mismo fichero, la sección **«Convenciones»**, la sección **«Fixtures comunes de
`AsistenteNuevoExpedienteServiceImpl` y del controlador»** (incluido el stub del oráculo del motor con
`thenAnswer`) y las **«Notas y supuestos»** del final.

- Son 61 tests sobre sus 13 métodos públicos. Los métodos privados NO llevan sección propia: se ejercen a través del método público que los invoca.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/system/ventanilla/service/impl/AsistenteNuevoExpedienteServiceImplTest.java`.
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
