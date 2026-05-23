# R-Correo-006 / R-Correo-007 — Envío asíncrono de correos pendientes

**Entidad:** Correo
**Operación:** enviarCorreosPendientes (acción propia, disparada por Job)
**Momento:** R-006 Antes del intento · R-007 Después del intento
**Servicio host:** com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl
**Método host:** void enviarCorreosPendientes()

## Análisis de la regla

### Qué se dispara y cuándo
- El disparo NO es una acción de usuario ni un evento `insert/update` sobre `Correo`. Es un proceso **batch periódico**: un Job de Quartz (`EnviarCorreosPendientesJob`) configurado vía `MetaSchedule` con un cron tomado de la propiedad de configuración `correos.envio.cron` (E-UB-012). Cada disparo del Job invoca una sola vez `CorreoService.enviarCorreosPendientes()`.
- `enviarCorreosPendientes()` recupera todos los `Correo` en estado `PENDIENTE` (`findByEstado(EstadoCorreo.PENDIENTE)`) y procesa **cada uno de forma aislada**.
- Por cada `Correo` PENDIENTE se ejecuta **un único intento** de envío. Las dos reglas se materializan como las dos mitades de ese intento:
  - **R-Correo-006** (Antes): `fireActionRule_RegistrarIntento(correo)` — entrega a la infraestructura va precedida de incrementar `numeroIntentos` y fijar `fechaUltimoIntento`.
  - **R-Correo-007** (Después): `fireActionRule_RegistrarResultadoEnvio(correo, exito, motivo)` — según el resultado marca `ENVIADO`+`fechaEnvio` o `FALLIDO`+`motivoUltimoFallo`.
- Un `Correo` que termina `FALLIDO` **no se reintenta** en disparos posteriores del Job (ya no está PENDIENTE). Solo vuelve a entrar al ciclo si una operación de reenvío lo devuelve a `PENDIENTE` (R-Correo-005, fuera de este fichero).

### Qué lee y de dónde
| Dato | Origen |
|------|--------|
| Lista de `Correo` a procesar | `correoRepository.findByEstado(EstadoCorreo.PENDIENTE)` (servidor, fuente de verdad) |
| `to` (destinatario) | `correo.getEmailDestinatario()` — del propio registro (destinatario único) |
| `subject` | `correo.getAsunto()` |
| `htmlBody` | `correo.getCuerpo()` (HTML enriquecido); `textBody` = null/derivado |
| Adjuntos (`nombreFichero`, `contenido` MetaFile) | colección `AdjuntoCorreo` del `Correo`; bytes vía `MetaFileUtil.downloadContent(metaFile)`; mimeType vía `metaFile.getFileType()` |
| `from` (remitente) | propiedad de configuración del subsistema mail (NO del cliente) — ver DUDAS al final |
| `numeroIntentos`, `fechaUltimoIntento`, `estado`, `fechaEnvio`, `motivoUltimoFallo` | campos servidor del `Correo`, escritos por las reglas |

### Qué hace y en qué orden (por cada Correo PENDIENTE)
1. `fireActionRule_RegistrarIntento(correo)`: `numeroIntentos = numeroIntentos + 1` y `fechaUltimoIntento = now` (INCONDICIONAL), y persiste. Queda constancia del intento aunque la JVM caiga durante el envío.
2. Construye el `Mail` (a partir de los campos del `Correo`) y los `Attach` (descargando los bytes de cada `AdjuntoCorreo`). Esta construcción puede fallar (adjunto ilegible) → se trata como fallo del intento.
3. `mailSender.send(mail)`: entrega a la infraestructura SMTP. Puede lanzar `RuntimeException` (timeout, destinatario rechazado, auth, etc.).
4. `fireActionRule_RegistrarResultadoEnvio(correo, exito, motivo)`:
   - éxito → `estado = ENVIADO`, `fechaEnvio = now`, `motivoUltimoFallo = null` (INCONDICIONAL).
   - fallo → `estado = FALLIDO`, `motivoUltimoFallo = <descripción saneada del error>` (INCONDICIONAL). `fechaEnvio` permanece null.
5. Persiste el resultado y **hace commit de ESE correo**.

### Efectos colaterales, transaccionalidad e idempotencia
- **Aislamiento por correo (CRÍTICO):** el bucle envuelve el procesamiento de cada `Correo` en su propio `try/catch` y su propia transacción. Un fallo (de envío, de construcción del Mail o de persistencia) de un correo NO debe abortar el procesamiento de los demás ni hacer rollback de los ya procesados. **NO** un único `@Transactional` global sobre `enviarCorreosPendientes()`: eso haría rollback de todo ante el primer fallo. En su lugar, una transacción por correo (`com.axelor.db.JPA.runInTransaction` por iteración, o un método `procesarCorreo(Correo)` `@Transactional` invocado por iteración).
- **Idempotencia parcial:** el incremento de `numeroIntentos` se hace ANTES del envío y se persiste; así, si el proceso muere tras enviar pero antes de marcar `ENVIADO`, el correo seguirá PENDIENTE y se reintentará en el siguiente disparo (posible doble envío, pero nunca un correo "perdido sin rastro"). Es el compromiso aceptado: preferimos posible duplicado a pérdida silenciosa, y `numeroIntentos` permite detectar bucles.
- **Efecto colateral externo:** el envío SMTP es irreversible y no transaccional; por eso el orden es intento→envío→resultado y nunca al revés.

### Casos de error y su tratamiento
- **Fallo de un correo individual** (SMTP timeout, destinatario inválido, auth, adjunto ilegible, construcción de Mail): NO se propaga fuera del procesamiento de ese correo. Se captura, el correo se marca `FALLIDO` con el motivo, y el bucle continúa con el siguiente.
- **Fallo al persistir el resultado de un correo** o cualquier excepción inesperada en su iteración: se captura, se loguea (id + estado, sin datos sensibles) y se continúa con el siguiente correo. No se propaga al Job.
- **Lo único que se propaga fuera de `enviarCorreosPendientes()`**: errores de infraestructura globales previos al bucle (p.ej. fallo al obtener la lista PENDIENTE). El Job los captura y loguea para que Quartz registre el disparo como fallido sin reventar el scheduler.

## Diseño detallado

### Clases nuevas

**1. `com.educaflow.subsystem.correos.jobs.EnviarCorreosPendientesJob`** — punto de entrada del scheduler.
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

**2. `com.educaflow.subsystem.correos.service.impl.CorreoMailFactory`** — colaborador que traduce un `Correo` (entidad de dominio) al `Mail`/`Attach` (DTO de infraestructura). Justificación: separa la responsabilidad de "leer la entidad y sus adjuntos y materializar bytes" de la orquestación del envío; mantiene `CorreoServiceImpl` legible y permite probar la construcción del Mail aislada. Se inyecta en `CorreoServiceImpl`.
```java
package com.educaflow.subsystem.correos.service.impl;

import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.subsystem.correos.db.Correo;

/**
 * Construye el DTO Mail de infraestructura a partir de un Correo de dominio.
 * Lee destinatario (emailDestinatario), asunto y cuerpo del Correo, resuelve el 'from' desde
 * la configuración del subsistema mail, y por cada AdjuntoCorreo descarga los bytes del MetaFile
 * (MetaFileUtil.downloadContent) creando un Attach(nombreFichero, bytes, mimeType).
 */
public class CorreoMailFactory {

    /**
     * Devuelve el Mail listo para entregar a MailSender.
     * Puede lanzar RuntimeException si un adjunto es ilegible (MetaFile sin fichero/IO);
     * ese fallo lo captura CorreoServiceImpl y marca el Correo como FALLIDO.
     * El 'from' NO procede del cliente: se toma de la config del subsistema mail.
     */
    public Mail build(Correo correo);
}
```

### Interfaces
No se introduce interfaz nueva propia. Se reutilizan:
- `com.educaflow.base.infrastructure.mail.MailSender` (interfaz ya existente; `send(Mail)`), inyectada con `@Inject` y bindeada en `CorreosModule` a `MailSenderImpl`.
- `org.quartz.Job` (implementada por el Job).

`CorreoMailFactory` se modela como clase concreta inyectable; no aporta tener interfaz (un solo impl, sin polimorfismo previsto).

### Tipos propios
**`ResultadoEnvio`** — record interno que encapsula el desenlace de un intento, para no pasar `(boolean, String)` sueltos.
```java
package com.educaflow.subsystem.correos.service.impl;

/**
 * Resultado de un intento de envío de un Correo.
 * exito=true -> motivo es null. exito=false -> motivo lleva la descripción saneada del fallo.
 */
public record ResultadoEnvio(boolean exito, String motivo) {
    public static ResultadoEnvio ok();                  // new ResultadoEnvio(true, null)
    public static ResultadoEnvio fallo(String motivo);  // new ResultadoEnvio(false, motivo)
}
```
(Su uso es interno en el bucle para construir los argumentos de `fireActionRule_RegistrarResultadoEnvio(Correo, boolean, String)` de forma legible.)

### Diagrama de secuencia
1. Quartz dispara `EnviarCorreosPendientesJob.execute(ctx)` según `correos.envio.cron`.
2. El Job resuelve `CorreoService` vía `Beans.get(ModelServiceFactory.class).resolve(Correo.class)` y llama `enviarCorreosPendientes()`.
3. `CorreoServiceImpl.enviarCorreosPendientes()` → `findByEstado(PENDIENTE)` devuelve la lista.
4. **Por cada `Correo` (iteración aislada, transacción propia, try/catch):**
   1. `fireActionRule_RegistrarIntento(correo)` → `numeroIntentos++`, `fechaUltimoIntento = now`, persiste.
   2. `mail = correoMailFactory.build(correo)` → por cada `AdjuntoCorreo`: `MetaFileUtil.downloadContent(adjunto.contenido)` → `new Attach(nombreFichero, bytes, mimeType)`.
   3. `mailSender.send(mail)` → entrega SMTP.
   4. Si 4.2 o 4.3 lanzan → `resultado = fallo(descripcionSaneada)`; si no → `resultado = ok()`.
   5. `fireActionRule_RegistrarResultadoEnvio(correo, resultado.exito(), resultado.motivo())` → marca ENVIADO+fechaEnvio (motivoUltimoFallo=null) o FALLIDO+motivoUltimoFallo, persiste.
   6. `catch`: loguea (id+estado, sin datos sensibles, CRLF saneado) y continúa con el siguiente correo.
5. Vuelta al Job; cualquier excepción global previa al bucle se captura y loguea en `execute`.

### Errores
| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| SMTP timeout / host no responde | `MailSender.send` (RuntimeException) | Capturada en la iteración → `FALLIDO`, `motivoUltimoFallo` = mensaje saneado. No aborta el bucle. |
| Destinatario inválido / rechazado | `MailSender.send` | `FALLIDO` + motivo. |
| Fallo de autenticación SMTP | `MailSender.send` | `FALLIDO` + motivo genérico (sin incluir credenciales). |
| Adjunto ilegible (MetaFile sin fichero / IO) | `CorreoMailFactory.build` → `MetaFileUtil.downloadContent` | Capturada → `FALLIDO` + motivo. No se envía un correo incompleto. |
| Correo sin destinatario / datos mínimos ausentes | `CorreoMailFactory.build` | `FALLIDO` + motivo (no se intenta el `send`). |
| Excepción al persistir el resultado de un correo | `JPA`/repo en la iteración | Capturada → log (id+estado) → continúa con el siguiente. |
| Fallo al obtener la lista PENDIENTE | `findByEstado` antes del bucle | Se propaga fuera de `enviarCorreosPendientes()`; el Job lo captura y loguea sin reventar el scheduler. |
| JVM cae entre intento y resultado | — | El correo queda PENDIENTE (intento ya persistido); se reintenta en el siguiente disparo. `numeroIntentos` deja rastro. |

### Contenido de los métodos (firmas + comentario, SIN cuerpo)
```java
// ====== com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl ======

/**
 * Orquesta el envío batch de todos los Correo en estado PENDIENTE.
 * Recupera findByEstado(PENDIENTE) y procesa CADA correo de forma AISLADA: cada uno en su
 * propia transacción y su propio try/catch, de modo que el fallo de uno NO aborta ni hace
 * rollback de los demás (NO usar un @Transactional global).
 * Por correo: fireActionRule_RegistrarIntento -> CorreoMailFactory.build -> mailSender.send
 * -> fireActionRule_RegistrarResultadoEnvio. Logs sin datos sensibles (id+estado, CRLF saneado).
 * Disparado por EnviarCorreosPendientesJob (cron 'correos.envio.cron', E-UB-012).
 */
@Override
public void enviarCorreosPendientes();

/**
 * R-Correo-006 (Antes del intento). numeroIntentos = numeroIntentos + 1 y fechaUltimoIntento = now,
 * de forma INCONDICIONAL (el servidor es la fuente de verdad; sin if==null; nada procede del cliente).
 * Persiste el cambio para que quede rastro del intento aun si el envío posterior se interrumpe.
 */
public void fireActionRule_RegistrarIntento(Correo correo);

/**
 * R-Correo-007 (Después del intento). Registra el desenlace de forma INCONDICIONAL:
 *  - exito=true  -> estado = ENVIADO, fechaEnvio = now, motivoUltimoFallo = null.
 *  - exito=false -> estado = FALLIDO, motivoUltimoFallo = motivo (descripción saneada del error).
 * Persiste el resultado. No reintenta: un FALLIDO solo vuelve a PENDIENTE por un reenvío explícito (R-Correo-005).
 */
public void fireActionRule_RegistrarResultadoEnvio(Correo correo, boolean exito, String motivo);
```

## Decisiones de implementación pendientes (resueltas con defaults razonables)
- **`from`**: se toma de una propiedad de configuración del subsistema mail (p.ej. `correos.envio.from`); si en el futuro cada centro tuviera remitente propio, `CorreoMailFactory` lo resolvería desde `correo.getCentro()`.
- **`mimeType` de adjuntos**: `adjunto.getContenido().getFileType()` (del `MetaFile`); fallback `application/octet-stream`.
- **`textBody`**: `Correo` guarda el cuerpo HTML (`cuerpo`); `textBody` se deja vacío/derivado.
- **Destinatario**: `Correo` tiene un único `emailDestinatario` → `to = List.of(emailDestinatario)`.
- **Transacción por correo**: `com.axelor.db.JPA.runInTransaction` por iteración (o método `@Transactional` por correo). Lo importante: NO una transacción única global del Job.
- **Motivo del fallo**: `MailSenderImpl.send` envuelve la causa en `RuntimeException`; el `motivoUltimoFallo` será el `getMessage()` saneado y truncado (no se distingue programáticamente timeout de destinatario inválido).
