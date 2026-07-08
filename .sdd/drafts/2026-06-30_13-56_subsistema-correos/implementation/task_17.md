---
type: implementation-task
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.base.infrastructure.mail.Mail`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/base/infrastructure/mail/MailTest.java`.
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

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`). Estáticos del stack con `Mockito.mockStatic` (patrón ya usado en el proyecto para `I18n`: campo `MockedStatic<...>` + `@BeforeEach`/`@AfterEach`, con `Strictness.LENIENT` cuando no todos los tests de la clase recorren la rama que lo consume; para mocks estáticos puntuales de un solo test, try-with-resources local).
- Nombres de test: `metodo_condicion_resultadoEsperado`.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.base.infrastructure.mail.Mail`  —  helper (record de datos)

**Responsabilidad:** transportar los datos de un correo a enviar (destinatarios `to`/`cc`/`bcc`, remitente, asunto, cuerpo y adjuntos) hacia `MailSender`.
**Colaboradores a mockear:** ninguno (record puro, sin dependencias).
**Origen diseño:** `design.md` Paso 1 — constructor de compatibilidad de 6 argumentos que delega en el canónico de 8.

### Método: `Mail(List<String> to, String from, String subject, String htmlBody, String textBody, List<Attach> attachs)` (constructor de compatibilidad)

- **`constructorCompatibilidad_seisArgumentos_delegaConCcYBccVacios`** — Tipo: happy. Verifica: `—` (soporte técnico de R-Correo-001, sin regla propia).
  - **Arrange:** una lista `to` con una dirección, `from`/`subject`/`htmlBody`/`textBody` con texto, una lista `attachs` con un `Attach`.
  - **Act:** invocar el constructor de 6 argumentos.
  - **Assert:** `mail.cc()` es una lista vacía; `mail.bcc()` es una lista vacía; `mail.to()`/`mail.from()`/`mail.subject()`/`mail.htmlBody()`/`mail.textBody()`/`mail.attachs()` son exactamente los valores pasados (delegación sin pérdida al constructor canónico de 8 argumentos).

**Ninguna otra clase se testea en esta tarea.** Los tests de `JavaMailHelper` (que también depende del Paso 1) van en la Tarea 18.
