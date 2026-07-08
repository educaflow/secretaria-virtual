---
type: implementation-task
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/axelor-config.properties` | Modificar | — | Añadir `correos.envio.from` y `correos.envio.pool-size` (ver Notas y supuestos). `correos.envio.cron` ya existía y **no** se usa en este diseño (fuera de alcance el job periódico). |
| `src/main/java/com/educaflow/base/infrastructure/mail/Mail.java` | Modificar | k-code-quality | Ampliar el record con `cc`/`bcc` reales (constructor de compatibilidad de 6 argumentos) — ver Paso 1 y `design/rules/R-Correo-001.md` |
| `src/main/java/com/educaflow/base/infrastructure/mail/impl/JavaMailHelper.java` | Modificar | k-code-quality | Añadir las cabeceras MIME `Message.RecipientType.CC`/`BCC` cuando `mail.cc()`/`mail.bcc()` no vienen vacías — ver Paso 1 |
| `src/main/java/com/educaflow/base/infrastructure/mail/impl/MailSenderImpl.java` | Modificar | k-code-quality | Enviar a `message.getAllRecipients()` en vez de solo `RecipientType.TO`, para que CC/BCC se entreguen de verdad por SMTP — ver Paso 1 |

## Texto del diseño (verbatim, `design.md`)

### Paso 1 — Configuración e infraestructura de correo (cc/bcc reales)

Añadir a `axelor-config.properties` (junto a `correos.envio.cron`, ya existente):

```properties
correos.envio.from = secretariavirtual@fpmislata.com
correos.envio.pool-size = 2
```

**Ampliar `base/infrastructure/mail` para soportar `cc`/`bcc` reales** (en vez de fusionar `para`+`enCopia`+`enCopiaOculta` en la única lista `Mail.to()`, que expondría cada destinatario "en copia oculta" a todos los demás en la cabecera `To` — ver `design/rules/R-Correo-001.md`, sección "Notas de esta regla", para el análisis completo). Cambios:

```java
// Fichero: src/main/java/com/educaflow/base/infrastructure/mail/Mail.java
package com.educaflow.base.infrastructure.mail;

public record Mail(java.util.List<String> to, java.util.List<String> cc, java.util.List<String> bcc,
                    String from, String subject, String htmlBody, String textBody,
                    java.util.List<Attach> attachs) {

    // Constructor de compatibilidad (firma de 6 argumentos, sin cc/bcc) — delega en el canónico
    // con cc=List.of() y bcc=List.of(). Preserva sin cambios el único llamador real existente
    // (com.educaflow.subsystem.registroentradasalida.service.impl.RegistroSalidaServiceImpl,
    // que sigue construyendo Mail con new Mail(to, from, subject, body, body, attachs)).
    public Mail(java.util.List<String> to, String from, String subject, String htmlBody,
                String textBody, java.util.List<Attach> attachs) {
        this(to, java.util.List.of(), java.util.List.of(), from, subject, htmlBody, textBody, attachs);
    }
}
```

```java
// Fichero: src/main/java/com/educaflow/base/infrastructure/mail/impl/JavaMailHelper.java
// Método: getMessage(Mail mail, Session session) [firma existente, sin cambios]
//   Tras la línea existente message.setRecipients(Message.RecipientType.TO, getAddresses(mail.to())),
//   añadir (mismo helper getAddresses(List<String>) ya usado para TO):
//     if (mail.cc() != null && !mail.cc().isEmpty()) {
//         message.setRecipients(jakarta.mail.Message.RecipientType.CC, getAddresses(mail.cc()));
//     }
//     if (mail.bcc() != null && !mail.bcc().isEmpty()) {
//         message.setRecipients(jakarta.mail.Message.RecipientType.BCC, getAddresses(mail.bcc()));
//     }
```

```java
// Fichero: src/main/java/com/educaflow/base/infrastructure/mail/impl/MailSenderImpl.java
// Método: send(Mail mail) [firma existente, sin cambios]
//   Cambiar la línea real de envío SMTP:
//     ANTES:    transport.sendMessage(message, message.getRecipients(jakarta.mail.Message.RecipientType.TO));
//     DESPUÉS:  transport.sendMessage(message, message.getAllRecipients());
//   MUST cambiar esta línea: el envío efectivo por SMTP usa la lista de destinatarios pasada a
//   sendMessage(...), no las cabeceras del Message — si no se cambia, cc/bcc quedarían escritos
//   en las cabeceras MIME pero NUNCA se entregarían de verdad a esos destinatarios.
```

**Verificar:** `grep -c "correos.envio" src/main/resources/axelor-config.properties` devuelve `3`; `grep -n "cc, java.util.List<String> bcc" src/main/java/com/educaflow/base/infrastructure/mail/Mail.java` encuentra la firma ampliada; `grep -n "getAllRecipients" src/main/java/com/educaflow/base/infrastructure/mail/impl/MailSenderImpl.java` confirma el cambio de envío.

### Notas y supuestos aplicables (verbatim, `design.md`)

2. **`Mail` (base/infrastructure/mail) se amplía con `cc`/`bcc` reales (corregido — ya NO se fusiona en `Mail.to()`).** Por directriz de diseño no se reimplementa el cliente SMTP (`MailSender`/`JavaMailHelper` se reutilizan, ampliados en vez de sustituidos). El Paso 1 añade los componentes `cc`/`bcc` al record `Mail` (con un constructor de compatibilidad de 6 argumentos para no romper al único llamador real existente, `RegistroSalidaServiceImpl`), añade las cabeceras MIME `CC`/`BCC` en `JavaMailHelper.getMessage(...)` cuando vienen no vacías, y cambia `MailSenderImpl.send(...)` para enviar a `message.getAllRecipients()` en vez de solo `RecipientType.TO` (si no, CC/BCC quedarían en las cabeceras pero no se entregarían de verdad por SMTP). `construirMail(Correo)` pasa `para`→`to`, `enCopia`→`cc`, `enCopiaOculta`→`bcc`, cada uno a su propia cabecera real: se preserva la confidencialidad de "en copia oculta" que exigen `VAL-Correo-012`/`013` y la propia semántica de `entity-Correo.md` (antes, fusionar los tres en `to` exponía cada destinatario bcc a todos los demás en la cabecera `To` de cada correo entregado). Ver el análisis completo en `design/rules/R-Correo-001.md`, sección "Notas de esta regla".
3. **Se añade el primer binding real de `MailSender` del proyecto.** La búsqueda en el código existente confirma que `MailSender` no tiene hoy ningún binding de Guice (aunque `RegistroSalidaServiceImpl` ya lo inyecta con `@Inject`, lo que sugiere una carencia previa no relacionada con este subsistema). El `Provider` de `CorreosModule` es, de hecho, el primer binding real de `MailSender` en toda la aplicación — como efecto colateral positivo, también arregla ese hueco para `registroentradasalida`. Si en el futuro se decide que ese binding debe vivir en un módulo más genérico (p.ej. `base/infrastructure`), basta moverlo sin tocar `correos`.

**Referencia adicional (no copiar contenido entero):** el análisis completo de por qué `cc`/`bcc` deben ir a cabeceras MIME reales está en `design/rules/R-Correo-001.md`, sección "Notas de esta regla".
