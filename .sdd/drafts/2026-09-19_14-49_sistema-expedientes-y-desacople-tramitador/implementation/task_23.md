---
type: implementation-task
template: system
---

# Tarea 23 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

**Alcance de esta tarea:** tarea de **tests unitarios** (`tests-code.md`): un fichero de test por clase de producción. El código de los tests es Java: se materializa delegando en `developer-code-implementer` (`implementation.md` §2 y `tests-code.md` §3). **MUST NOT** modificar el código de producción para que un test pase.

**Sección de la descripción que esta tarea implementa:** en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design/test-unit-desc.md`, la sección «## Clase: com.educaflow.subsystem.expedientes.services.VistaExpediente» (líneas 172-183) y, como marco común, la sección `## Convenciones` (líneas 5-27: JUnit 5 + Mockito, nombres `metodo_condicion_resultadoEsperado`, mocks estáticos de `I18n` y `SecurityUtil`, mensajes exactos del spec y decisiones ante ambigüedades). **Lee ambas íntegras** antes de escribir el test.

**Instrucciones de generación (plantilla de `tests-code.md` §2)**

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.expedientes.services.VistaExpediente`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/expedientes/services/VistaExpedienteTest.java`.
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
