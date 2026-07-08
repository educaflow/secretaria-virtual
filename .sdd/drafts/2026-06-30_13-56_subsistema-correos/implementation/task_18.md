---
type: implementation-task
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.base.infrastructure.mail.impl.JavaMailHelper`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/base/infrastructure/mail/impl/JavaMailHelperTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita, o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más).
  **MUST NOT** "adaptar" los tests al código divergente.

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`). Estáticos del stack con `Mockito.mockStatic` (patrón ya usado en el proyecto para `I18n`: campo `MockedStatic<...>` + `@BeforeEach`/`@AfterEach`, con `Strictness.LENIENT` cuando no todos los tests de la clase recorren la rama que lo consume; para mocks estáticos puntuales de un solo test, try-with-resources local).
- Nombres de test: `metodo_condicion_resultadoEsperado`.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.base.infrastructure.mail.impl.JavaMailHelper`  —  helper

**Responsabilidad:** construir el `jakarta.mail.Message` a partir de un `Mail`, incluyendo (tras esta ampliación) las cabeceras `CC`/`BCC` cuando `Mail.cc()`/`Mail.bcc()` no vienen vacías.
**Colaboradores a mockear:** ninguno — se usa una `jakarta.mail.Session` real (`Session.getDefaultInstance(new Properties())`, sin conexión SMTP: `getMessage(...)` solo ensambla el objeto en memoria).
**Origen diseño:** `design.md` Paso 1 y `design/rules/R-Correo-001.md` "Notas de esta regla".

### Método: `static Message getMessage(Mail mail, Session session)`

- **`getMessage_conCcNoVacio_estableceCabeceraCc`** — Tipo: happy. Verifica: `—` (soporte técnico de R-Correo-001).
  - **Arrange:** `Mail` con `cc = List.of("copia@x.com")`, `to`/`from`/`subject` rellenos, `bcc = List.of()`, `attachs = List.of()`; sesión real sin autenticador.
  - **Act:** `JavaMailHelper.getMessage(mail, session)`.
  - **Assert:** `message.getRecipients(Message.RecipientType.CC)` contiene exactamente `copia@x.com` (una `InternetAddress`).
- **`getMessage_conBccNoVacio_estableceCabeceraBcc`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `Mail` con `bcc = List.of("oculto@x.com")`, `cc = List.of()`, resto relleno.
  - **Act:** `JavaMailHelper.getMessage(mail, session)`.
  - **Assert:** `message.getRecipients(Message.RecipientType.BCC)` contiene exactamente `oculto@x.com`.
- **`getMessage_conCcYBccVacios_noEstableceEsasCabeceras`** — Tipo: borde. Verifica: `—` (regresión: no romper el envío existente sin CC/BCC, único caso usado hoy por `RegistroSalidaServiceImpl`).
  - **Arrange:** `Mail` con `cc = List.of()` y `bcc = List.of()` (o `null` en ambos, dos variantes del mismo test).
  - **Act:** `JavaMailHelper.getMessage(mail, session)`.
  - **Assert:** `message.getRecipients(Message.RecipientType.CC)` es `null`; `message.getRecipients(Message.RecipientType.BCC)` es `null` (comportamiento nativo de `jakarta.mail` cuando no se han fijado destinatarios de ese tipo); `message.getRecipients(Message.RecipientType.TO)` sigue conteniendo los de `to` (no afectado por el cambio).
