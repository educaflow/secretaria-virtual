---
type: implementation-task
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-scheduler
- k-secure-coding
- k-code-quality

Esta tarea implementa el **Job de Quartz** que dispara el envío asíncrono.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/jobs/EnviarCorreosPendientesJob.java` | Crear | k-scheduler | Job Quartz que dispara el envío (ver `rules/R-Correo-006.md`). |

---

## Paso 7 — Envío asíncrono (E-UB-010 / E-UB-011 / E-UB-012)

Diseño detallado en `design/rules/R-Correo-006.md`. Resumen:
- `jobs/EnviarCorreosPendientesJob` (`implements org.quartz.Job`): resuelve `CorreoService` vía `Beans.get(ModelServiceFactory.class).resolve(Correo.class)` y llama `enviarCorreosPendientes()`; captura excepciones globales para no reventar el scheduler.

Detalle de la clase (de `design/rules/R-Correo-006.md`):

**`com.educaflow.subsystem.correos.jobs.EnviarCorreosPendientesJob`** — punto de entrada del scheduler.
Responsabilidad: ser el `org.quartz.Job` que Quartz instancia en cada disparo del cron; resolver el `CorreoService` y delegar en `enviarCorreosPendientes()`. No contiene lógica de negocio. Captura cualquier excepción global para no reventar el scheduler.
```java
package com.educaflow.subsystem.correos.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Job de Quartz que dispara el envío de los correos PENDIENTE.
 * Registrado vía MetaSchedule con cron tomado de la propiedad 'correos.envio.cron' (E-UB-012).
 * No contiene lógica de negocio: resuelve el servicio y delega.
 */
public class EnviarCorreosPendientesJob implements Job {

    /**
     * Resuelve CorreoService vía Beans.get(ModelServiceFactory.class).resolve(Correo.class)
     * y llama enviarCorreosPendientes(). Envuelve la llamada en try/catch para que un fallo
     * global se loguee (id/estado, sin datos sensibles) sin propagar y reventar el scheduler.
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException;
}
```

> Lo único que se propaga fuera de `enviarCorreosPendientes()`: errores de infraestructura globales previos al bucle (p.ej. fallo al obtener la lista PENDIENTE). El Job los captura y loguea para que Quartz registre el disparo como fallido sin reventar el scheduler.

**Logs:** solo `id`/estado; nunca cuerpo, email completo ni bytes; CRLF saneado.
