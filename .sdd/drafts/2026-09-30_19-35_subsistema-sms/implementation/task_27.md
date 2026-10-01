---
type: implementation-task
template: system
---

# Tarea 27 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.base.util.NumeroTelefono`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/base/util/NumeroTelefonoTest.java`.
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

Sección de `design/test-unit-desc.md` que describe esta clase: **«Clase: `com.educaflow.base.util.NumeroTelefono`»** (con todos sus `### Método:`). Aplican además las secciones generales «Convenciones» y «Decisiones tomadas ante ambigüedades del diseño» del mismo fichero.

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline). Clases con lógica que testear: `NumeroTelefono`, `MensajeSmsUtil`, `TextUtil` (solo el método nuevo `trazaCompleta`), `EjecutorAsincrono`, `EjecutorAsincronoProvider`, `AppEventObserver`, `SmsServiceImpl`, `SmsController`, `SmsSenderProvider`. Para cubrir `SmsServiceImpl.enviarSms` (privado) el test mockea `EjecutorAsincrono`, **captura el `Runnable`** que recibe `ejecutarTrasCommit` y lo ejecuta con `JPA` mockeado estáticamente, igual que ya hacen los tests de correos. De `NumeroTelefono` y `MensajeSmsUtil` solo se testean sus métodos **públicos** (`esMovilDeEspana`/`enFormatoE164` y `cabeEnUnSms`): los privados quedan cubiertos a través de ellos.
