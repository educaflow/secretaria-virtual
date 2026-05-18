# R-TareaCorreo-005 — Envío asíncrono de correos

## Resumen

El envío SMTP no se ejecuta durante la creación de la TareaCorreo. Un job Quartz toma periódicamente las tareas en estado `PENDIENTE` y las procesa una a una, delegando en `TareaCorreoService.procesarEnvio(...)`. El control vuelve enseguida al usuario que creó la tarea.

## Componentes

### 1. `MailSenderProvider` (Guice)

Provider Guice que construye un `MailSender` leyendo los parámetros SMTP de `AppSettings`:

```java
// com.educaflow.subsystem.correos.config.MailSenderProvider
@Provides
@Singleton
public MailSender mailSenderProvider() {
    String host = AppSettings.get().get("mail.smtp.host");
    String user = AppSettings.get().get("mail.smtp.user");
    String pass = AppSettings.get().get("mail.smtp.password");
    SmtpCredentialSimplePassword cred = new SmtpCredentialSimplePassword(host, user, pass);
    return new MailSenderImpl(cred);
}
```

El binding se declara en el módulo Guice del subsistema (`CorreosModule extends AbstractModule`), no en un módulo de `ModelService`.

### 2. `ProcesadorCorreosJob` (Quartz)

Clase `com.educaflow.subsystem.correos.job.ProcesadorCorreosJob implements org.quartz.Job`.

```java
public class ProcesadorCorreosJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        // 1. Obtener pendientes mediante TareaCorreoService.obtenerPendientes()
        //    (delega en TareaCorreoRepository.findPendientes()).
        // 2. Para cada TareaCorreo pendiente:
        //      a. Llamar a TareaCorreoService.procesarEnvio(tareaCorreo).
        //      b. Capturar excepciones de forma aislada por tarea (un fallo en una no aborta el resto).
        //    Cada procesarEnvio() corre dentro de su propia transacción (JPA::runInTransaction).
    }
}
```

Dependencias inyectadas vía `Beans.get(...)`:
- `TareaCorreoService` para `obtenerPendientes()` y `procesarEnvio(...)`.
- `MailSender` (a través del provider) — se usa dentro de `procesarEnvio` del servicio, no aquí.

### 3. Secuencia dentro de `procesarEnvio(TareaCorreo)`

Implementada en `TareaCorreoServiceImpl.procesarEnvio(...)`:

1. **Antes** — `fireActionRule_marcarEnviando(tareaCorreo)` (R-TareaCorreo-004):
   - `estado = ENVIANDO`, `numeroIntentos += 1`, `fechaUltimoIntento = LocalDateTime.now()`.
   - Persistir con `super.update(...)` y `JPA.flush()` para que otro tick del job no la re-tome.
2. **Acción** — construir el `Mail` (record) con `to`, `from`, `subject=asunto`, `htmlBody=cuerpo`, `attachs` mapeados de `adjuntos` (`Attach(nombre, bytes/MetaFile)`).
3. **Llamada** — `mailSender.send(mail)` (puede lanzar excepción).
4. **Después — éxito** — `fireActionRule_marcarEnviado(tareaCorreo)` (R-TareaCorreo-006): `estado = ENVIADO` y persistir.
5. **Después — fallo** — `try/catch` sobre `mailSender.send`: en catch, `fireActionRule_marcarFallado(tareaCorreo, ex.getMessage())` (R-TareaCorreo-007): `estado = FALLADO`, `motivoFallo = ex.getMessage()`, persistir. La excepción NO se propaga al job (el job sigue con la siguiente tarea).

### 4. Registro del job en `MetaSchedule`

Fichero de datos iniciales: `src/main/resources/data-init/input/correos-MetaSchedule.xml`.

Contenido orientativo:
- `name = "enviar-correos-job"`
- `cron = "0/30 * * * * ?"` (cada 30 segundos, para testing; en producción ajustar a `0 0/1 * * * ?` cada minuto).
- `jobClass = "com.educaflow.subsystem.correos.job.ProcesadorCorreosJob"`
- `active = true`
- `description = "Envía las tareas de correo en estado PENDIENTE."`

### 5. Concurrencia y reentrada

- La marca `ENVIANDO` al inicio (R-TareaCorreo-004) evita que dos ticks del job tomen la misma tarea: `findPendientes()` filtra por `estado = PENDIENTE`.
- Si el proceso muere entre `ENVIANDO` y `ENVIADO/FALLADO`, la tarea queda en `ENVIANDO` huérfana. La estrategia inicial es detectarlo manualmente (Administrador). Mejora futura: barrido de tareas en `ENVIANDO` antiguas que vuelvan a `PENDIENTE` (no incluido en este diseño).

## Trazabilidad

- R-TareaCorreo-005 (esta regla): ubicación principal = `ProcesadorCorreosJob.execute`.
- R-TareaCorreo-004/006/007: ubicación = `TareaCorreoServiceImpl.fireActionRule_marcarEnviando/Enviado/Fallado` invocados desde `procesarEnvio`.
