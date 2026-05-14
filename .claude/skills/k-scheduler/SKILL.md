---
name: k-scheduler
description: Crear y configurar jobs del scheduler de Axelor (Quartz). Usa esto cuando el usuario pida crear una tarea programada, un job recurrente, un cron job, programar la ejecución periódica de código Java, o configurar una entrada de MetaSchedule.
---

# k-scheduler — Crear jobs programados en Axelor

Axelor Open Platform integra **Quartz Scheduler** para ejecutar jobs periódicamente. Los jobs se ejecutan en un proceso aparte bajo sesión admin, con acceso transaccional a la base de datos.

## 1. Habilitar el scheduler

En `axelor-config.properties` (o equivalente):

```properties
quartz.enable = true
quartz.thread-count = 3

# Opcional: persistir jobs en BD (requerido para multi-instancia)
quartz.job-store.class = org.quartz.impl.jdbcjobstore.JobStoreTX
```

Sin `quartz.enable = true` los jobs NO se ejecutan, aunque estén en `MetaSchedule`.

## 2. Implementar la clase Job

Crear una clase que implemente `org.quartz.Job`. Recibe los parámetros vía `JobDataMap`.

```java
package com.miempresa.miapp.jobs;

import com.axelor.inject.Beans;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiTareaJob implements Job {

  private static final Logger log = LoggerFactory.getLogger(MiTareaJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      JobDataMap data = context.getJobDetail().getJobDataMap();
      String param1 = data.getString("param1");

      // Servicios via Guice
      MiServicio servicio = Beans.get(MiServicio.class);
      servicio.hacerAlgo(param1);

      log.info("Job ejecutado: {}", context.getJobDetail().getKey().getName());
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }
}
```

### Notas importantes
- `Beans.get(...)` obtiene servicios Guice (no `@Inject` directo: Quartz instancia la clase).
- El job corre en una sesión admin con su propia transacción.
- Lanzar `JobExecutionException` para errores: Quartz lo registra y maneja reintentos.
- Si necesitas patrón "thread separado", revisa `ThreadedJob` y `BatchJob` en axelor-open-suite (`axelor-base/src/main/java/com/axelor/apps/base/job/`).

## 3. Registrar el job en la base de datos

Hay dos formas:

### A) Vía UI: `Administración → Jobs → Schedules`
Crear un `MetaSchedule` con:
- **name** — identificador único
- **job** — FQN de la clase, ej. `com.miempresa.miapp.jobs.MiTareaJob`
- **cron** — expresión cron Quartz (ver §4)
- **active** — `true` para activarlo
- **params** — lista clave→valor que llega al `JobDataMap`

### B) Vía data-init XML
En `src/main/resources/data-init/input-config.xml` cargar registros `MetaSchedule` desde CSV/XML.

## 4. Expresiones cron (Quartz)

Quartz usa **6 o 7 campos**: `seg min hora día mes díaSemana [año]`. NO es el cron Unix de 5 campos.

| Cron | Significado |
|------|-------------|
| `0 0 2 * * ?` | Cada día a las 02:00 |
| `0 */15 * * * ?` | Cada 15 minutos |
| `0 0 9 ? * MON-FRI` | Lun–Vie a las 09:00 |
| `0 0 0 1 * ?` | El día 1 de cada mes a medianoche |
| `0 30 23 ? * SUN` | Domingo a las 23:30 |

Usar `?` en uno de los campos día-mes / día-semana (no se permite definir ambos).

Documentación: https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html

## 5. Acceso a parámetros desde el Job

Los `params` del `MetaSchedule` llegan como `JobDataMap`:

```java
JobDataMap data = context.getJobDetail().getJobDataMap();
String valor = data.getString("miClave");
```

Si necesitas recuperar el `MetaSchedule` completo desde el job (p. ej. para campos extra añadidos al modelo):

```java
String name = context.getJobDetail().getKey().getName();
MetaSchedule schedule = Beans.get(MetaScheduleRepository.class).findByName(name);
```

## 6. Control programático del scheduler

`com.axelor.quartz.JobRunner` expone:
- `init()` — arranca el scheduler.
- `shutdown()` — lo para.
- `restart()` — recarga jobs del tenant actual.
- `update(MetaSchedule)` — actualiza un job sin reiniciar todo.
- `remove(MetaSchedule)` — desprograma uno.

El scheduler arranca automáticamente en `AppStartup` si `quartz.enable=true`.

## 7. Multi-tenant y clustering

- En modo multi-tenant los jobs se agrupan por tenant ID.
- Para clustering: usar `JobStoreTX` + `application.cache.provider = redisson` (o `redisson-native`).
- Tuning: `quartz.job-store.cluster-checkin-interval`.

## 8. Checklist al crear un nuevo job

1. ¿`quartz.enable = true` en config?
2. Clase Java `implements org.quartz.Job` con `execute()` envuelto en try/catch → `JobExecutionException`.
3. Servicios obtenidos con `Beans.get(...)`, NO `@Inject`.
4. Registro `MetaSchedule` con `name`, `job` (FQN), `cron`, `active=true`.
5. Cron Quartz de 6 campos (con `?` en un día).
6. Si necesita parámetros: añadirlos como `MetaScheduleParam` y leerlos vía `JobDataMap`.
7. Probar primero con un cron frecuente (p. ej. `0/30 * * * * ?` cada 30 s) y luego ajustar.

## Ejemplos en código existente

- `axelor-core/src/main/java/com/axelor/mail/service/MailFetchJob.java` — job de fetch de correo (en la plataforma).
- `axelor-open-suite/axelor-base/src/main/java/com/axelor/apps/base/job/CurrencyConversionJob.java` — job simple inyectando un service factory.
- `axelor-open-suite/axelor-supplychain/src/main/java/com/axelor/apps/supplychain/job/MrpJob.java` — lee un parámetro del `MetaSchedule` (campo extra `mrpSeq`) y dispara un servicio.
- `axelor-open-suite/axelor-base/src/main/java/com/axelor/apps/base/job/BatchJob.java` — patrón genérico para invocar `AbstractBatchService` desde un job, mapeando params con Groovy.
- `axelor-open-suite/axelor-gdpr/src/main/java/com/axelor/apps/gdpr/job/ProcessingRegisterJob.java` — otro ejemplo de job de negocio.

## Documentación de referencia
- `documentation/modules/dev-guide/pages/modules/scheduler.adoc` — guía oficial en este repo.
- `axelor-core/src/main/java/com/axelor/quartz/` — implementación completa (`JobRunner`, `SchedulerProvider`, factories).
- `axelor-core/src/main/resources/domains/MetaSchedule.xml` — modelo de `MetaSchedule` y `MetaScheduleParam`.
