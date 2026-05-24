---
type: implementation-task
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-scheduler

Esta tarea registra el **MetaSchedule** (cron) que dispara el Job de envío.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `data-init/input/MetaSchedule.xml` (o alta por UI) | Crear | k-scheduler | Registro `MetaSchedule` con cron desde `correos.envio.cron`. |

Descripción de diseño (Paso 7 — Envío asíncrono):

- `MetaSchedule` `name="correos-enviar-pendientes"`, `job=...jobs.EnviarCorreosPendientesJob`, `cron` desde la propiedad `correos.envio.cron` (default `0 */5 * * * ?`), `active=true`. Requiere `quartz.enable=true` en `axelor-config`.

Detalle (de `design/rules/R-Correo-006.md`): `MetaSchedule` `name="correos-enviar-pendientes"`, `job=...jobs.EnviarCorreosPendientesJob`, `cron` desde la propiedad de configuración `correos.envio.cron` (E-UB-012).

Comprueba el patrón existente de `MetaSchedule` / data-init en el proyecto (ubicación real del fichero `MetaSchedule.xml` y cómo se referencia desde `data-init`) antes de crearlo, y sigue ese patrón.
