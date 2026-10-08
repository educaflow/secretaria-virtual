---
type: implementation-task
template: system
---

# Tarea 24 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice

## Módulo Guice NotificacionesModule

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/module/NotificacionesModule.java` | Crear | k-guice | Fusiona `CorreosModule`, `MailSenderProvider`, `SmsModule` y `SmsSenderProvider` (los providers pasan a métodos `@Provides`; los ficheros antiguos se eliminan con `subsystem/correos/**` y `subsystem/sms/**`) |

### Paso 9 (verbatim del design.md)

### Paso 9 — Módulo Guice

**`NotificacionesModule extends AxelorModule`** (`…module`): `configure()` sin bindings y dos métodos `@Provides MailSender mailSender()` y `@Provides SmsSender smsSender()` con el mismo cuerpo que los `get()` de los antiguos `MailSenderProvider` y `SmsSenderProvider` (leen `AppSettings`, k-guice §3.3), que desaparecen como clases (guía «se fusionan en `NotificacionesModule`»); sin `bind(...).toProvider(...)`. Los servicios siguen inyectando `Provider<MailSender>`/`Provider<SmsSender>` (Guice lo resuelve con el `@Provides`), así que el envío sigue leyendo la configuración dentro del envío (RN-Correo-003, RN-Sms-002). Ningún binding de `ModelService` (los descubre `ModelServiceFactory`). (Los módulos y providers antiguos se borraron en el Paso 1.) No se instala a mano en ningún sitio (Axelor descubre los `AxelorModule`).

Verificación: cierra el bloque 1–9: `./gradlew -q compileJava` en verde y la aplicación arranca sin `Guice/MissingConstructor`.

### Nota del design.md (`## Notas y supuestos`)

  - `CorreoServiceImpl` pasa a inyectar `Provider<MailSender>` (como ya hacía SMS con `SmsSender`), para que una instalación sin servidor de correo deje el correo FALLIDO (RN-Correo-003) en lugar de fallar al crear el servicio.

**Nota de la descomposición:** el cuerpo de los antiguos `MailSenderProvider.get()` y `SmsSenderProvider.get()` se toma del historial de git (`git show HEAD:src/main/java/com/educaflow/subsystem/correos/...` / `.../sms/...`), porque la tarea de eliminación de `subsystem/correos/**` y `subsystem/sms/**` los borra antes.
