---
type: implementation-task
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno. La sección concreta es **"Clase: `com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl`"** de `design/test-unit-desc.md`.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/service/impl/GrupoServiceImplTest.java`.
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

> Convenciones (de `design/test-unit-desc.md`): JUnit 5 + Mockito (`MockitoExtension`); estáticos del stack con `Mockito.mockStatic` (`com.educaflow.base.util.SecurityUtil`, `com.axelor.auth.AuthUtils`, `com.axelor.inject.Beans`/`ModelServiceFactory`, `com.axelor.i18n.I18n` como identidad); aserciones con `org.junit.jupiter.api.Assertions`; helper `com.educaflow.base.infrastructure.junit.JUnitHelper.assertThrowsCause` para excepciones envueltas; nombres `metodo_condicion_resultadoEsperado`.
