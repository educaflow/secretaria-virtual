---
type: implementation-task
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-code-quality

Esta tarea implementa el **módulo Guice** del subsistema.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/module/CorreosModule.java` | Crear | k-sistemas | `extends AxelorModule`; binding `MailSender → MailSenderImpl`. |

---

## Paso 7 — Envío asíncrono (binding del módulo)

- `CorreoModule` bindea `MailSender → MailSenderImpl`.

`CorreosModule.java` — `extends AxelorModule`; registra el binding `MailSender → MailSenderImpl` (la interfaz `com.educaflow.base.infrastructure.mail.MailSender` ya existe; se inyecta con `@Inject` en `CorreoServiceImpl`).

## Paso 9 — Verificación (aplicable)
- `CorreosModule` registra `MailSender → MailSenderImpl`.
