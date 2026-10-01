---
type: implementation-task
template: system
---

# Tarea 33 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-guice

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/test/java/com/educaflow/secretariavirtual/startup/AppEventObserverTest.java` | Crear | — | Que `onAppShutdown` llame a `ejecutorAsincrono.detener()` (caso trasladado de `CorreoEventObserverTest`) |

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.secretariavirtual.startup.AppEventObserver` (solo el delta de `onAppShutdown`).

- Sección concreta de `design/test-unit-desc.md` que describe esta clase: «## Clase: `com.educaflow.secretariavirtual.startup.AppEventObserver`» (líneas 257-271). Aplican además, a todos los tests, los apartados «## Convenciones» y «### Decisiones tomadas ante ambigüedades del diseño» del principio de ese mismo fichero.
- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/secretariavirtual/startup/AppEventObserverTest.java`.
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

### Paso 3 — tests (de `design/design.md`)

- Tests: crear `src/test/java/com/educaflow/base/infrastructure/async/EjecutorAsincronoTest.java` con los
  casos de los dos tests que se eliminan (commit ejecuta la tarea / rollback no la ejecuta / hilos daemon
  con nombre / una tarea que lanza no rompe el pool / `detener` para el pool) **más el caso nuevo de la
  tercera entrada del contrato: sin transacción activa, `IllegalStateException` y ninguna tarea enviada
  al pool**;
  `EjecutorAsincronoProviderTest` recoge el caso de `CorreoAsyncExecutorProviderTest` (lee `async.pool-size`,
  por defecto 2); `AppEventObserverTest` recoge el de `CorreoEventObserverTest` (`onAppShutdown` llama a
  `detener()`); y en `CorreoServiceImplTest` se sustituyen los cuatro `Mockito.mockStatic(PostCommitRunner.class)`
  por `verify(ejecutorAsincrono).ejecutarTrasCommit(any())` (y `never()` donde el test verifica que no se
  programa envío). No se pierde ninguna comprobación.

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline). Clases con lógica que testear: `NumeroTelefono`, `MensajeSmsUtil`, `TextUtil` (solo el método nuevo `trazaCompleta`), `EjecutorAsincrono`, `EjecutorAsincronoProvider`, `AppEventObserver`, `SmsServiceImpl`, `SmsController`, `SmsSenderProvider`. Para cubrir `SmsServiceImpl.enviarSms` (privado) el test mockea `EjecutorAsincrono`, **captura el `Runnable`** que recibe `ejecutarTrasCommit` y lo ejecuta con `JPA` mockeado estáticamente, igual que ya hacen los tests de correos. De `NumeroTelefono` y `MensajeSmsUtil` solo se testean sus métodos **públicos** (`esMovilDeEspana`/`enFormatoE164` y `cabeEnUnSms`): los privados quedan cubiertos a través de ellos.
- **Tests E2E**: `design/test-e2e-desc.md` (T-001…T-025, uno por cada `ESC-NNN` del spec).
- **Regresión obligatoria del refactor del paso 3**: los tests unitarios de correos y los E2E ya persistidos en `src/test/e2e/subsystem/correos/` **MUST** seguir pasando sin cambios de comportamiento.
