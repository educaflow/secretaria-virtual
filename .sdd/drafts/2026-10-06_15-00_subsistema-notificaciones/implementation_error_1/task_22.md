---
type: implementation-task
template: system
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-code-quality

## Módulo Guice NotificacionesModule

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/module/NotificacionesModule.java` | Crear | k-guice | Fusiona `CorreosModule`, `MailSenderProvider`, `SmsModule` y `SmsSenderProvider` (los providers pasan a métodos `@Provides`; los ficheros antiguos se eliminan con `subsystem/correos/**` y `subsystem/sms/**`) |

## Paso del diseño

### Paso 9 — Módulo Guice

**`NotificacionesModule extends AxelorModule`** (`…module`): `configure()` sin bindings y dos métodos `@Provides MailSender mailSender()` y `@Provides SmsSender smsSender()` con el mismo cuerpo que los `get()` de los antiguos `MailSenderProvider` y `SmsSenderProvider` (leen `AppSettings`, k-guice §3.3), que desaparecen como clases (guía «se fusionan en `NotificacionesModule`»); sin `bind(...).toProvider(...)`. Los servicios siguen inyectando `Provider<MailSender>`/`Provider<SmsSender>` (Guice lo resuelve con el `@Provides`), así que el envío sigue leyendo la configuración dentro del envío (RN-Correo-003, RN-Sms-002). Ningún binding de `ModelService` (los descubre `ModelServiceFactory`). (Los módulos y providers antiguos se borraron en el Paso 1.) No se instala a mano en ningún sitio (Axelor descubre los `AxelorModule`).

Verificación: cierra el bloque 1–9: `./gradlew -q compileJava` en verde y la aplicación arranca sin `Guice/MissingConstructor`.

## Notas y supuestos

  - `CorreoServiceImpl` pasa a inyectar `Provider<MailSender>` (como ya hacía SMS con `SmsSender`), para que una instalación sin servidor de correo deje el correo FALLIDO (RN-Correo-003) en lugar de fallar al crear el servicio.

> **Nota del descomponedor (decisión ante ambigüedad):** el Paso 1 del diseño borra `subsystem/correos/**`, `subsystem/sms/**` y sus tests (tareas 05–07) antes de esta tarea. Donde el diseño dice que algo «se conserva», «se mueve» o tiene «el mismo cuerpo» que en `correos`/`sms`, el código original se lee del último commit con `git show HEAD:<ruta>` (p.ej. `git show HEAD:src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`). Solo sirve de referencia: **MUST NOT** restaurar esos ficheros.
