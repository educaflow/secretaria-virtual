---
type: design
---

# Diseño: Subsistema Correos

**Objetivo:** Subsistema multicentro para registrar de forma inmutable y auditable los correos electrónicos que la aplicación envía (o intenta enviar) a personas, con envío asíncrono mediante scheduler periódico, reintento manual de fallidos y vistas diferenciadas por rol.
**Capa:** subsystem/correos
**Análisis de origen:** .sdd/drafts/2026-05-11_23-19_tareas-de-envio-de-correos/analysis_03/analysis.md
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad, k-scheduler

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/domains/TareaCorreo.xml` | Crear | k-sistemas | Dominio `TareaCorreo` + enum `EstadoTareaCorreo` |
| `src/main/java/com/educaflow/subsystem/correos/domains/AdjuntoCorreo.xml` | Crear | k-sistemas | Dominio `AdjuntoCorreo` |
| `src/main/java/com/educaflow/subsystem/correos/db/repo/TareaCorreoRepository.java` | Crear | k-sistemas | Finders para scheduler, búsqueda y gráfica |
| `src/main/java/com/educaflow/subsystem/correos/db/repo/AdjuntoCorreoRepository.java` | Crear | k-sistemas | Repositorio de adjuntos |
| `src/main/java/com/educaflow/subsystem/correos/service/EncolarCorreoDTO.java` | Crear | k-sistemas | Record DTO para la API de encolado |
| `src/main/java/com/educaflow/subsystem/correos/service/AdjuntoEncolarDTO.java` | Crear | k-sistemas | Record DTO de adjunto en el encolado |
| `src/main/java/com/educaflow/subsystem/correos/service/GraficaPuntoDTO.java` | Crear | k-sistemas | Record (fecha, estado, total) para la chart |
| `src/main/java/com/educaflow/subsystem/correos/service/TareaCorreoService.java` | Crear | k-sistemas | Interfaz `ModelService<TareaCorreo>` |
| `src/main/java/com/educaflow/subsystem/correos/service/AdjuntoCorreoService.java` | Crear | k-sistemas | Interfaz `ModelService<AdjuntoCorreo>` |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/DefaultTareaCorreoServiceImpl.java` | Crear | k-sistemas | Impl con `fireActionRule_*` y `validate*` |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/DefaultAdjuntoCorreoServiceImpl.java` | Crear | k-sistemas | Impl con clonado de MetaFile e inmutabilidad |
| `src/main/java/com/educaflow/subsystem/correos/controller/TareaCorreoController.java` | Crear | k-sistemas | Controlador con `@CallMethod` |
| `src/main/java/com/educaflow/subsystem/correos/module/CorreosModule.java` | Crear | k-sistemas | Módulo Guice (solo para `MailSender`) |
| `src/main/java/com/educaflow/subsystem/correos/module/MailSenderProvider.java` | Crear | k-sistemas | Provider de `MailSender` desde AppSettings |
| `src/main/java/com/educaflow/subsystem/correos/job/EnviarCorreosJob.java` | Crear | k-scheduler | Job Quartz que invoca `procesarPendientes()` |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo.xml` | Crear | k-vistas | `@Main` — Todos los correos (admin) |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-centro.xml` | Crear | k-vistas | `@Centro` — Correos del centro (supervisor) |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-buscar.xml` | Crear | k-vistas | `@Buscar` — Buscar correos de persona (administrativa) |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-mis.xml` | Crear | k-vistas | `@Mis` — Mis correos (usuario final) |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-grafica.xml` | Crear | k-vistas | `@Grafica` — Gráfica de correos (admin) |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-ref.xml` | Crear | k-vistas | `@Search-grid` + `@View-form` referenciables |
| `src/main/java/com/educaflow/subsystem/correos/views/AdjuntoCorreo-ref.xml` | Crear | k-vistas | Vistas referenciables de `AdjuntoCorreo` |
| `src/main/java/com/educaflow/secretariavirtual/views/menus-correos.xml` | Crear | k-vistas | `menuitem` para los 4 puntos de entrada + "Mis correos" |
| `src/main/resources/data-init/input/auth-correos.xml` | Crear | k-seguridad | Permisos Axelor por rol |
| `src/main/resources/data-init/input/meta-schedule-correos.xml` | Crear | k-scheduler | `MetaSchedule` `correos-enviar` |
| `src/main/resources/axelor-config.properties` | Modificar | k-sistemas | Añadir `correos.scheduler.*` y `mail.smtp.*` |
| `src/main/java/com/educaflow/secretariavirtual/startup/SecretariaVirtualModule.java` | Modificar | k-sistemas | `install(new CorreosModule())` |

---

## Paso 1 — Dominios (XML completo)

### `src/main/java/com/educaflow/subsystem/correos/domains/TareaCorreo.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="correos" package="com.educaflow.subsystem.correos.db"/>

    <entity name="TareaCorreo">
        <many-to-one name="centro" ref="com.educaflow.subsystem.common.db.Centro"
                     title="Centro"
                     help="Centro propietario del correo. Si es nulo, es un correo del sistema."/>
        <string name="de" title="De" required="true"
                help="Dirección remitente. Se asigna automáticamente desde la configuración SMTP global (R-001)."/>
        <string name="destinatarioDni" title="DNI destinatario"
                help="Snapshot textual del DNI del destinatario; no es FK."/>
        <string name="destinatarioEmail" title="Email destinatario" required="true"/>
        <string name="destinatarioNombre" title="Nombre destinatario"/>
        <string name="asunto" title="Asunto" required="true"/>
        <string name="cuerpoHtml" title="Cuerpo HTML" large="true" required="true"/>
        <string name="cuerpoTextoPlano" title="Cuerpo texto plano" large="true"/>
        <enum name="estado" ref="EstadoTareaCorreo" title="Estado" required="true" default="'PENDIENTE'"/>
        <datetime name="fechaCreacion" title="Fecha de creación" required="true"/>
        <datetime name="fechaUltimoIntento" title="Fecha del último intento"/>
        <integer name="numIntentos" title="Nº de intentos" required="true" default="0" min="0"/>
        <datetime name="fechaEnvioOk" title="Fecha de envío OK"/>
        <string name="logErrores" title="Log de errores" large="true"
                help="Histórico acumulativo de errores; nunca se reinicia."/>
        <many-to-one name="historialExpediente"
                     ref="com.educaflow.subsystem.expedientes.db.HistorialExpediente"
                     title="Historial de expediente"/>
        <one-to-many name="adjuntos"
                     ref="com.educaflow.subsystem.correos.db.AdjuntoCorreo"
                     mappedBy="tareaCorreo" title="Adjuntos"/>

        <track>
            <field name="estado"/>
            <field name="numIntentos"/>
            <field name="fechaUltimoIntento"/>
            <field name="fechaEnvioOk"/>
        </track>
    </entity>

    <enum name="EstadoTareaCorreo">
        <item name="PENDIENTE" title="Pendiente"/>
        <item name="ENVIANDO"  title="Enviando"/>
        <item name="ENVIADO"   title="Enviado"/>
        <item name="FALLADO"   title="Fallado"/>
    </enum>

</domain-models>
```

### `src/main/java/com/educaflow/subsystem/correos/domains/AdjuntoCorreo.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="correos" package="com.educaflow.subsystem.correos.db"/>

    <entity name="AdjuntoCorreo">
        <many-to-one name="tareaCorreo"
                     ref="com.educaflow.subsystem.correos.db.TareaCorreo"
                     required="true" title="Tarea de correo"/>
        <string name="nombre" title="Nombre" required="true"/>
        <many-to-one name="fichero" ref="com.axelor.meta.db.MetaFile"
                     required="true" title="Fichero"/>
    </entity>

</domain-models>
```

Cubre por declaración del modelo: **V-001**, **V-002**, **V-003**, **V-006**, **V-010**, **V-011**.

**Verificación del paso:** `./gradlew clean build --info` compila las entidades generadas.

---

## Paso 2 — DTOs y Servicios

### DTOs (records)

**`com.educaflow.subsystem.correos.service.EncolarCorreoDTO`**
```java
public record EncolarCorreoDTO(
    Centro centro,                              // opcional (nulo = correo del sistema)
    String destinatarioDni,                     // opcional (snapshot)
    String destinatarioEmail,                   // obligatorio
    String destinatarioNombre,                  // opcional (snapshot)
    String asunto,
    String cuerpoHtml,
    String cuerpoTextoPlano,                    // opcional
    HistorialExpediente historialExpediente,    // opcional
    List<AdjuntoEncolarDTO> adjuntos
) {}
```

**`com.educaflow.subsystem.correos.service.AdjuntoEncolarDTO`**
```java
public record AdjuntoEncolarDTO(String nombre, MetaFile fichero) {}
```

**`com.educaflow.subsystem.correos.service.GraficaPuntoDTO`**
```java
public record GraficaPuntoDTO(LocalDate fecha, EstadoTareaCorreo estado, long total) {}
```

### `TareaCorreoService` (interfaz)

```java
package com.educaflow.subsystem.correos.service;

public interface TareaCorreoService extends ModelService<TareaCorreo> {

    /** API interna para que otros subsistemas encolen un correo.
     *  Construye TareaCorreo + AdjuntoCorreo desde el DTO y delega en this.insert(...),
     *  disparando R-001 (de), R-002 (campos sistema), R-003 (snapshot), R-009 (copia adjunto). */
    TareaCorreo encolar(EncolarCorreoDTO dto);

    /** Punto de entrada del scheduler. Selecciona hasta `correos.scheduler.tamanoLote` tareas en
     *  PENDIENTE (ordenadas por fechaCreacion ASC) y para cada una llama enviarUna(t). R-004..R-008. */
    void procesarPendientes();

    /** Reintento manual: FALLADO → PENDIENTE preservando numIntentos y logErrores.
     *  Valida V-012 (estado actual FALLADO), V-013 (transición permitida) y V-018 (autorización
     *  supervisor: solo correos de su centro activo). Aplica R-007. */
    TareaCorreo reintentar(TareaCorreo tareaCorreo, TareaCorreo tareaCorreoOriginal);

    /** Pre-agregado para la chart `@Grafica`. Filtra por rango y estados ENVIADO/FALLADO,
     *  agrupa por (DATE(fechaCreacion), estado). */
    List<GraficaPuntoDTO> datosGrafica(LocalDate fechaDesde, LocalDate fechaHasta);

    @Override Optional<BusinessMessages> validateInsert(TareaCorreo entity);
    @Override Optional<BusinessMessages> validateUpdate(TareaCorreo entity, TareaCorreo original);
    @Override Optional<BusinessMessages> validateRemove(TareaCorreo entity);
}
```

### `DefaultTareaCorreoServiceImpl`

```java
package com.educaflow.subsystem.correos.service.impl;

public class DefaultTareaCorreoServiceImpl
        extends DefaultModelService<TareaCorreo>
        implements TareaCorreoService {

    @Inject private MailSender mailSender;                  // del módulo Guice CorreosModule
    @Inject private AppSettings appSettings;                // para mail.smtp.* y correos.scheduler.tamanoLote
    @Inject private TareaCorreoRepository tareaCorreoRepository;

    @Inject
    public DefaultTareaCorreoServiceImpl(Class<TareaCorreo> model, TareaCorreoRepository repository) {
        super(model, repository);
    }

    // ====================== Validaciones ======================

    /** Valida en alta:
     *   - V-004: destinatarioEmail con formato de email válido. Mensaje debe transmitir el email recibido.
     *   - V-005: si destinatarioDni viene informado, comprobar con DniUtil. Mensaje debe transmitir el DNI recibido.
     *  V-001/V-002/V-003/V-006 ya las cubre el modelo XML (`required="true"`); aquí defensa en profundidad. */
    @Override
    public Optional<BusinessMessages> validateInsert(TareaCorreo entity);

    /** Valida en modificación:
     *   - V-007: los campos de contenido (asunto, cuerpoHtml, cuerpoTextoPlano, destinatarioDni/Email/Nombre,
     *            de, centro, historialExpediente, fechaCreacion) no pueden cambiar respecto a original.
     *            Mensaje debe transmitir cuál es el campo que se intentó modificar.
     *   - V-009: la colección `adjuntos` no puede cambiar de cardinalidad ni contenido respecto a original.
     *   - V-013: transición de estado pertenece a la matriz {PENDIENTE→ENVIANDO, ENVIANDO→ENVIADO,
     *            ENVIANDO→FALLADO, FALLADO→PENDIENTE}. Cualquier otra rechazada; mensaje debe transmitir
     *            la transición intentada y la lista de transiciones válidas. */
    @Override
    public Optional<BusinessMessages> validateUpdate(TareaCorreo entity, TareaCorreo original);

    /** V-008: el borrado de TareaCorreo está prohibido siempre.
     *  Mensaje fijo informando que el registro de correos es permanente. */
    @Override
    public Optional<BusinessMessages> validateRemove(TareaCorreo entity);

    // ====================== Override de operaciones CRUD ======================

    /** Pipeline de inserción:
     *   1) fireActionRule_asignarRemitente(entity)        — R-001
     *   2) fireActionRule_inicializarCamposSistema(entity) — R-002
     *   3) fireActionRule_snapshotDestinatario(entity)     — R-003
     *   4) super.insert(entity)                            — dispara validateInsert (V-001..V-006)
     *  La copia del fichero de cada adjunto (R-009) corre en DefaultAdjuntoCorreoServiceImpl.insert. */
    @Override
    public TareaCorreo insert(TareaCorreo entity);

    // ====================== API pública ======================

    /** Construye TareaCorreo a partir del DTO (campos snapshot literales),
     *  crea los AdjuntoCorreo a partir de adjuntos del DTO y delega en this.insert(...). */
    @Override
    public TareaCorreo encolar(EncolarCorreoDTO dto);

    /** Lee tamanoLote de AppSettings (default 50), llama tareaCorreoRepository.findPendientes(tamanoLote)
     *  y para cada elemento invoca enviarUna(t). Captura excepciones por correo individual para no
     *  abortar el lote completo. */
    @Override
    public void procesarPendientes();

    /** Valida V-012 (estadoOriginal==FALLADO), V-013 (transición permitida) y V-018 (supervisor
     *  solo en correos de su centroActivo). Aplica fireActionRule_reabrirParaReintento (R-007)
     *  y delega en super.update(tareaCorreo, tareaCorreoOriginal). */
    @Override
    public TareaCorreo reintentar(TareaCorreo tareaCorreo, TareaCorreo tareaCorreoOriginal);

    /** Llama a tareaCorreoRepository.contarPorDiaYEstado(fechaDesde, fechaHasta). */
    @Override
    public List<GraficaPuntoDTO> datosGrafica(LocalDate fechaDesde, LocalDate fechaHasta);

    // ====================== Reglas de negocio (fireActionRule_*) ======================

    /** R-001 — Antes de super.insert. Asigna entity.de leyendo `mail.smtp.user` de AppSettings.
     *  V-006 (required en modelo) verifica luego que el valor no quedó vacío. */
    private void fireActionRule_asignarRemitente(TareaCorreo entity);

    /** R-002 — Antes de super.insert. Inicializa:
     *    estado = PENDIENTE; numIntentos = 0; fechaCreacion = LocalDateTime.now();
     *    logErrores = ""; fechaUltimoIntento = null; fechaEnvioOk = null. */
    private void fireActionRule_inicializarCamposSistema(TareaCorreo entity);

    /** R-003 — Antes de super.insert. Garantiza que destinatarioDni/Email/Nombre son snapshots
     *  textuales: no se resuelve ninguna FK a User/Persona; los valores quedan literales. */
    private void fireActionRule_snapshotDestinatario(TareaCorreo entity);

    /** R-004 — Antes del envío SMTP (dentro de enviarUna). Cambia PENDIENTE → ENVIANDO y persiste
     *  inmediatamente para evitar reentrada concurrente de otro ciclo del scheduler. */
    private void fireActionRule_marcarComoEnviando(TareaCorreo entity);

    /** R-005 — Después de mailSender.send() OK. Cambia estado a ENVIADO, asigna fechaEnvioOk=now,
     *  fechaUltimoIntento=now, numIntentos++. Persiste. */
    private void fireActionRule_marcarComoEnviado(TareaCorreo entity);

    /** R-006 — Después de mailSender.send() KO. Cambia estado a FALLADO, asigna fechaUltimoIntento=now,
     *  numIntentos++. logErrores += línea con timestamp + clase de excepción + mensaje (stacktrace
     *  resumido). NUNCA reinicia logErrores. Persiste. */
    private void fireActionRule_marcarComoFallado(TareaCorreo entity, Throwable error);

    /** R-007 — Antes de super.update en reintentar. Estado=PENDIENTE. NO toca numIntentos
     *  ni logErrores ni fechaUltimoIntento. */
    private void fireActionRule_reabrirParaReintento(TareaCorreo entity);

    /** R-008 — Dentro de enviarUna. Construye Mail(to=[destinatarioEmail], from=de, subject=asunto,
     *  htmlBody=cuerpoHtml, textBody=cuerpoTextoPlano, attachs=lista derivada de adjuntos —
     *  cada Attach se obtiene leyendo bytes y mimeType del MetaFile clonado) y llama
     *  mailSender.send(mail). El contenido enviado coincide exactamente con el registrado. */
    private void enviarUna(TareaCorreo entity);
}
```

### `AdjuntoCorreoService` (interfaz)

```java
public interface AdjuntoCorreoService extends ModelService<AdjuntoCorreo> {
    @Override Optional<BusinessMessages> validateInsert(AdjuntoCorreo entity);
    @Override Optional<BusinessMessages> validateUpdate(AdjuntoCorreo entity, AdjuntoCorreo original);
    @Override Optional<BusinessMessages> validateRemove(AdjuntoCorreo entity);
}
```

### `DefaultAdjuntoCorreoServiceImpl`

```java
public class DefaultAdjuntoCorreoServiceImpl
        extends DefaultModelService<AdjuntoCorreo>
        implements AdjuntoCorreoService {

    @Inject
    public DefaultAdjuntoCorreoServiceImpl(Class<AdjuntoCorreo> model, AdjuntoCorreoRepository repository) {
        super(model, repository);
    }

    /** V-010 (nombre) y V-011 (fichero) ya cubiertos por modelo; defensa en profundidad. */
    @Override public Optional<BusinessMessages> validateInsert(AdjuntoCorreo entity);

    /** V-009 — Inmutabilidad: rechaza cualquier modificación de un adjunto persistido,
     *  excepto la asignación inicial del FK al padre. */
    @Override public Optional<BusinessMessages> validateUpdate(AdjuntoCorreo entity, AdjuntoCorreo original);

    /** V-008 / V-009 — Borrado prohibido. */
    @Override public Optional<BusinessMessages> validateRemove(AdjuntoCorreo entity);

    /** Override CRUD para invocar la copia del fichero antes de persistir. */
    @Override public AdjuntoCorreo insert(AdjuntoCorreo entity);

    /** R-009 — Antes de super.insert. Sustituye entity.fichero por una copia propia mediante
     *  MetaFileUtil.cloneMetaFile(entity.fichero), garantizando que el adjunto persistido es
     *  independiente del MetaFile origen aunque éste cambie o desaparezca. */
    private void fireActionRule_copiarFicheroAdjunto(AdjuntoCorreo entity);
}
```

**Verificación del paso:** `./gradlew clean build --info`; los servicios compilan y `ModelServiceFactory` los descubre por convención de nombre (no se registran en Guice).

---

## Paso 3 — Repositorios

### `TareaCorreoRepository`

```java
package com.educaflow.subsystem.correos.db.repo;

public class TareaCorreoRepository extends JpaRepository<TareaCorreo> {

    public TareaCorreoRepository() { super(TareaCorreo.class); }

    /** Devuelve hasta `limit` TareaCorreo en estado PENDIENTE, ordenadas por fechaCreacion ASC.
     *  Usado por procesarPendientes(). */
    public List<TareaCorreo> findPendientes(int limit);

    /** Query base con filtro `self.centro IS NOT NULL AND self.centro = :centro`.
     *  Usado por la vista @Centro (R-010). */
    public Query<TareaCorreo> findForCentro(Centro centro);

    /** Filtro `self.centro IS NOT NULL AND self.centro = :centro AND self.destinatarioDni = :dni`.
     *  Usado por la vista @Buscar (R-010 + V-014). */
    public Query<TareaCorreo> findForBusquedaDniEnCentro(Centro centro, String dni);

    /** SELECT date(fechaCreacion) AS dia, estado, count(*) FROM TareaCorreo
     *   WHERE fechaCreacion BETWEEN :desde AND :hasta AND estado IN (ENVIADO, FALLADO)
     *   GROUP BY 1, 2 ORDER BY 1.
     *  Devuelve una lista de GraficaPuntoDTO. Usado por la vista @Grafica. */
    public List<GraficaPuntoDTO> contarPorDiaYEstado(LocalDate desde, LocalDate hasta);
}
```

### `AdjuntoCorreoRepository`

```java
public class AdjuntoCorreoRepository extends JpaRepository<AdjuntoCorreo> {
    public AdjuntoCorreoRepository() { super(AdjuntoCorreo.class); }
}
```

**Verificación del paso:** compila y Axelor reconoce los repositorios.

---

## Paso 4 — Controlador

### `TareaCorreoController`

```java
package com.educaflow.subsystem.correos.controller;

public class TareaCorreoController {

    @Inject private ModelServiceFactory modelServiceFactory;

    /** Acción del botón "Reintentar" del formulario admin/supervisor.
     *  Resuelve el TareaCorreoService, recupera el TareaCorreo desde ActionRequestHelper
     *  (con su original via getOriginalModel()) y delega en service.reintentar(...).
     *  Aplica indirectamente V-012, V-013, V-018 y R-007. Si validación falla, devuelve
     *  los BusinessMessages como error en la actionResponse; si OK, fuerza reload. */
    @Transactional
    @CallMethod
    public void reintentar(ActionRequest actionRequest, ActionResponse actionResponse);

    /** Acción que alimenta la chart de @Grafica. Lee fechaDesde/fechaHasta del context,
     *  defensa servidor V-016/V-017, llama service.datosGrafica(...) y empaqueta los puntos
     *  como series para el chart de Axelor. */
    @CallMethod
    public void datosGrafica(ActionRequest actionRequest, ActionResponse actionResponse);

    /** Acción de búsqueda por DNI desde @Buscar. Valida en servidor V-014/V-015 (defensa)
     *  y devuelve la lista filtrada de TareaCorreo (centro activo + DNI). Si DNI vacío o
     *  inválido devuelve lista vacía (U-006). */
    @CallMethod
    public void buscarPorDni(ActionRequest actionRequest, ActionResponse actionResponse);
}
```

> Todos los parámetros son `actionRequest` / `actionResponse` (regla obligatoria de `k-sistemas/controladores.md`).

**Verificación del paso:** compila; al arrancar la app, las `<action-method>` de las vistas resuelven correctamente este controlador.

---

## Paso 5 — Módulo Guice + `MailSenderProvider`

### `MailSenderProvider`

```java
package com.educaflow.subsystem.correos.module;

public class MailSenderProvider implements com.google.inject.Provider<MailSender> {

    @Inject private AppSettings appSettings;

    /** Lee `mail.smtp.host`, `mail.smtp.user`, `mail.smtp.password` de AppSettings
     *  y devuelve new MailSenderImpl(new SmtpCredentialSimplePassword(host, user, pass)). */
    @Override public MailSender get();
}
```

### `CorreosModule`

```java
package com.educaflow.subsystem.correos.module;

public class CorreosModule extends AbstractModule {

    /** Vincula MailSender → MailSenderProvider (singleton).
     *  NO se vinculan los ModelService: los descubre ModelServiceFactory automáticamente. */
    @Override
    protected void configure() {
        bind(MailSender.class).toProvider(MailSenderProvider.class).in(Singleton.class);
    }
}
```

**Verificación del paso:** compila; al arrancar, `MailSender` se inyecta correctamente en `DefaultTareaCorreoServiceImpl`. `CorreosModule` extiende `AxelorModule` y Axelor lo descubre automáticamente — no se registra en `SecretariaVirtualModule`.

---

## Paso 6 — Job del scheduler

### `EnviarCorreosJob`

```java
package com.educaflow.subsystem.correos.job;

public class EnviarCorreosJob implements org.quartz.Job {

    /** Job ejecutado por Quartz según el MetaSchedule `correos-enviar`.
     *  Resuelve TareaCorreoService vía
     *    (TareaCorreoService) Beans.get(ModelServiceFactory.class).resolve(TareaCorreo.class)
     *  y llama a service.procesarPendientes(). Captura cualquier excepción global y la loguea,
     *  pero NO la re-lanza como JobExecutionException porque los fallos por correo individual
     *  ya quedan registrados en logErrores de cada TareaCorreo (R-006). */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException;
}
```

> Servicios obtenidos con `Beans.get(...)` (Quartz instancia el job sin Guice). Sigue la convención de `k-scheduler`.

**Verificación del paso:** compila. Tras registrar el `MetaSchedule` (Paso 9), el log mostrará la ejecución cada 5 min.

---

## Paso 7 — Vistas (un `<action-view>` por fichero)

Convención de namespace: `subsysCorreo.TareaCorreo@<Discriminador>-(grid|form|chart|action|...)`.

### `views/TareaCorreo.xml` — `@Main` (admin: "Todos los correos")

Por regla arquitectónica (`k-sistemas`), la vista "principal" sin discriminador usa el sufijo `@Main` y vive en `<NombreEntidad>.xml`.

Vistas declaradas:
- `<action-view name="subsysCorreo.TareaCorreo@Main-action">` — sin `domain` (admin ve todo, incluido centro nulo).
- `<grid name="subsysCorreo.TareaCorreo@Main-grid">` — columnas: `estado`, `fechaCreacion`, `centro`, `destinatarioEmail`, `destinatarioDni`, `asunto`, `numIntentos`, `fechaEnvioOk`. Filtros: estado, fechaCreacion, centro, destinatarioDni, destinatarioEmail. Sin acciones de borrado en fila.
- `<form name="subsysCorreo.TareaCorreo@Main-form">`:
  - Panel "Datos generales": `centro`, `de` (readonly), `asunto`.
  - Panel "Destinatario": `destinatarioDni`, `destinatarioEmail`, `destinatarioNombre`.
  - Panel "Contenido": `cuerpoHtml` (widget HTML render), `cuerpoTextoPlano`.
  - Panel-related "Adjuntos" sobre `adjuntos` → vistas `subsysCorreo.AdjuntoCorreo@Ref-grid`/`@Ref-form`.
  - Panel "Estado y trazabilidad": `estado`, `fechaCreacion`, `fechaUltimoIntento`, `numIntentos`, `fechaEnvioOk` — todo readonly (**U-002**).
  - Panel "Log de errores" con `hideIf="!logErrores"` (**U-008**): muestra `logErrores` readonly.
  - Panel "Expediente relacionado" con `hideIf="!historialExpediente"` (**U-009**): muestra `historialExpediente`.
  - Campos de contenido y destinatario `readonlyIf="id != null"` (**U-001**).
  - Botón toolbar "Nuevo correo" — presente solo en este fichero (admin/@Main → **U-004**).
  - Botón "Reintentar" — `showIf="estado == 'FALLADO'"` (**U-003**) → onClick `subsysCorreo.TareaCorreo@Main-reintentar-action`.

Acciones declaradas:
- `action-method subsysCorreo.TareaCorreo@Main-reintentar-action` — invoca `TareaCorreoController:reintentar`.
- `action-attrs subsysCorreo.TareaCorreo@Main-onLoad-action` — referenciada desde `onLoad` del form: aplica los `readonlyIf` programáticos para campos de contenido/sistema (**U-001/U-002**) y desencadena el render saneado del HTML (**U-010**).
- `action-validate subsysCorreo.TareaCorreo@Main-validateEmail-action` — sobre `onChange` de `destinatarioEmail`: comprueba formato (**V-004**); mensaje debe transmitir el email recibido.
- `action-validate subsysCorreo.TareaCorreo@Main-validateDni-action` — sobre `onChange` de `destinatarioDni` cuando no vacío: comprueba con DniUtil (**V-005**); mensaje debe transmitir el DNI recibido.

### `views/TareaCorreo-centro.xml` — `@Centro` (supervisor)

- `<action-view name="subsysCorreo.TareaCorreo@Centro-action">` con `domain="self.centro IS NOT NULL AND self.centro = :__user__.centroActivo"` (**R-010**).
- Grid y form reutilizan estructura del `@Main` con nombres `@Centro-grid`/`@Centro-form` (las acciones se duplican con nombre `@Centro-...`).
- NO incluye botón "Nuevo correo" (**U-004**).
- Botón "Reintentar" igual que `@Main` (**U-003**). La autorización V-018 se aplica en el servicio.

### `views/TareaCorreo-buscar.xml` — `@Buscar` (administrativa)

- `<action-view name="subsysCorreo.TareaCorreo@Buscar-action">` con `domain="self.centro IS NOT NULL AND self.centro = :__user__.centroActivo AND self.destinatarioDni = :_filtroDni"` (**R-010** + **V-014**).
- `<search-fields>` con campo `dni` `required="true"` (**U-005** + **V-014**).
- `action-validate subsysCorreo.TareaCorreo@Buscar-validateDni-action` — comprueba dni no vacío (**V-014**) y formato con DniUtil (**V-015**); mensaje debe transmitir el DNI recibido.
- `action-attrs subsysCorreo.TareaCorreo@Buscar-listadoVacio-action` — si dni no informado o inválido, fija dominio imposible para que la grid quede vacía (**U-006**).
- Grid y form sin botones de alta ni reintento (solo lectura).

### `views/TareaCorreo-mis.xml` — `@Mis` (profesor, alumno, exprofesor, exalumno, familiar, externo)

- `<action-view name="subsysCorreo.TareaCorreo@Mis-action">` con `domain="self.destinatarioDni = :__user__.dni AND :__user__.dni IS NOT NULL"`.
- Grid columnas: `fechaCreacion`, `asunto`, `estado`, `fechaEnvioOk`. Form solo lectura.
- `action-attrs subsysCorreo.TareaCorreo@Mis-onLoad-action` — render saneado del HTML del cuerpo (**U-010**); hideIf de paneles "Log de errores" (**U-008**) y "Expediente" (**U-009**).
- Sin botones "Nuevo" ni "Reintentar".

### `views/TareaCorreo-grafica.xml` — `@Grafica` (admin)

- `<action-view name="subsysCorreo.TareaCorreo@Grafica-action">` con vista `<chart>`.
- `<chart name="subsysCorreo.TareaCorreo@Grafica-chart" stacked="true">` — barras apiladas; eje X = día; serie = `estado` (solo `ENVIADO` y `FALLADO`); valor = count. Datos provistos por `TareaCorreoController:datosGrafica`.
- `<search-fields>` `fechaDesde` y `fechaHasta` ambos `required="true"` (**U-007** + **V-016**).
- `action-validate subsysCorreo.TareaCorreo@Grafica-validateFechas-action` — comprueba ambas fechas no vacías (**V-016**) y `fechaDesde <= fechaHasta` (**V-017**); mensaje debe transmitir las fechas recibidas.
- `action-method subsysCorreo.TareaCorreo@Grafica-cargar-action` — `TareaCorreoController:datosGrafica` (alimenta el chart).

### `views/TareaCorreo-ref.xml` — referencia (excepción permitida)

- `<grid name="subsysCorreo.TareaCorreo@Search-grid">` — columnas: `destinatarioEmail`, `asunto`, `estado`, `fechaCreacion`.
- `<form name="subsysCorreo.TareaCorreo@View-form">` — ficha mínima readonly (asunto, destinatarioEmail, estado).
- Estas dos vistas conviven en el mismo fichero por excepción explícita de la regla "un action-view por fichero".

### `views/AdjuntoCorreo-ref.xml`

- `<grid name="subsysCorreo.AdjuntoCorreo@Ref-grid">` — columnas: `nombre`, `fichero`.
- `<form name="subsysCorreo.AdjuntoCorreo@Ref-form">` — campos `nombre`, `fichero` (readonly cuando el padre TareaCorreo ya está persistido — refuerzo UI de **V-009**).

**Verificación del paso:** la app arranca sin errores XSD. Cada `<action-view>` resuelve sus grid/form/chart.

---

## Paso 8 — Menús

### `secretariavirtual/views/menus-correos.xml`

Entradas (la visibilidad por rol la asegura la seguridad del Paso 9; aquí se asocian a sus action-views):

| `menuitem` (name)                  | parent                              | Título                      | Acción                                    | Roles objetivo                                            |
|------------------------------------|-------------------------------------|-----------------------------|-------------------------------------------|-----------------------------------------------------------|
| `menu-correos`                     | (raíz)                              | "Correos"                   | —                                         | administrador, supervisor, administrativa                 |
| `menu-correos-todos`               | `menu-correos`                      | "Todos los correos"         | `subsysCorreo.TareaCorreo@Main-action`    | administrador                                             |
| `menu-correos-centro`              | `menu-correos`                      | "Correos del centro"        | `subsysCorreo.TareaCorreo@Centro-action`  | supervisor                                                |
| `menu-correos-buscar`              | `menu-correos`                      | "Buscar correos de persona" | `subsysCorreo.TareaCorreo@Buscar-action`  | administrativa                                            |
| `menu-correos-grafica`             | `menu-correos`                      | "Gráfica de correos"        | `subsysCorreo.TareaCorreo@Grafica-action` | administrador                                             |
| `menu-carpetaCiudadana-misCorreos` | `menu-carpetaCiudadana` (existente) | "Mis correos"               | `subsysCorreo.TareaCorreo@Mis-action`     | profesor, alumno, exprofesor, exalumno, familiar, externo |

**Verificación del paso:** los menús aparecen para el rol correspondiente tras login.

---

## Paso 9 — Seguridad

### `src/main/resources/data-init/input/auth-correos.xml`

Permisos Axelor (`<permission>`) por rol con condición JPQL en lenguaje natural:

| Permiso (name)                       | Modelo        | Roles                                                     | canRead | canCreate | canWrite                                       | canRemove | Condición                                                                                  |
|--------------------------------------|---------------|-----------------------------------------------------------|---------|-----------|------------------------------------------------|-----------|--------------------------------------------------------------------------------------------|
| `perm.correos.tarea.administrador`   | TareaCorreo   | administrador                                             | ✓       | ✓         | ✓                                              | ✗ (V-008) | sin condición (todo)                                                                       |
| `perm.correos.tarea.supervisor`      | TareaCorreo   | supervisor                                                | ✓       | ✗         | ✓ (solo estado, autorizado por servicio V-018) | ✗         | `self.centro IS NOT NULL AND self.centro = :__user__.centroActivo`                         |
| `perm.correos.tarea.administrativa`  | TareaCorreo   | administrativa                                            | ✓       | ✗         | ✗                                              | ✗         | `self.centro IS NOT NULL AND self.centro = :__user__.centroActivo`                         |
| `perm.correos.tarea.ciudadano`       | TareaCorreo   | profesor, alumno, exprofesor, exalumno, familiar, externo | ✓       | ✗         | ✗                                              | ✗         | `self.destinatarioDni = :__user__.dni AND :__user__.dni IS NOT NULL`                       |
| `perm.correos.adjunto.administrador` | AdjuntoCorreo | administrador                                             | ✓       | ✓         | ✗                                              | ✗         | sin condición                                                                              |
| `perm.correos.adjunto.scopeCentro`   | AdjuntoCorreo | supervisor, administrativa                                | ✓       | ✗         | ✗                                              | ✗         | `self.tareaCorreo.centro IS NOT NULL AND self.tareaCorreo.centro = :__user__.centroActivo` |
| `perm.correos.adjunto.ciudadano`     | AdjuntoCorreo | profesor, alumno, exprofesor, exalumno, familiar, externo | ✓       | ✗         | ✗                                              | ✗         | `self.tareaCorreo.destinatarioDni = :__user__.dni AND :__user__.dni IS NOT NULL`           |

Notas:
- La inmutabilidad de contenido (V-007/V-009) se garantiza en `validateUpdate`; los `canWrite=false` son defensa en profundidad.
- El reintento (operación servidor) cambia `estado`. El permiso `canWrite=true` para admin/supervisor permite el flujo del botón; la autorización V-018 final se aplica en `service.reintentar(...)`.

**Verificación del paso:** logins con cada rol filtran correctamente los listados.

---

## Paso 10 — Datos iniciales

### `src/main/resources/data-init/input/meta-schedule-correos.xml`

Registro `MetaSchedule`:
- `name`: `correos-enviar`
- `job`: `com.educaflow.subsystem.correos.job.EnviarCorreosJob`
- `cron`: `0 0/5 * * * ?` (cada 5 minutos)
- `active`: `true`
- `description`: "Procesa TareaCorreo en estado PENDIENTE."

### Parámetros añadidos en `src/main/resources/axelor-config.properties`

```
correos.scheduler.intervaloMinutos = 5
correos.scheduler.tamanoLote       = 50
mail.smtp.host                     = (a configurar por entorno)
mail.smtp.user                     = (a configurar por entorno)
mail.smtp.password                 = (a configurar por entorno)
quartz.enable                      = true
```

> `correos.scheduler.intervaloMinutos` es informativo / documental; la cadencia efectiva la fija el `cron` del `MetaSchedule`. Si se quiere cambiar la frecuencia, se actualiza el `cron` desde la UI de Administración → Jobs → Schedules (Axelor recarga el `MetaSchedule` sin reinicio).

**Verificación del paso:** al arrancar, en logs aparece el `MetaSchedule` `correos-enviar` activo.

---

## Paso 11 — Verificación final

1. **Compilar:** `./gradlew clean build --info` — sin errores.
2. **Arrancar:** `./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /`.
3. Comprobar en logs: `MetaSchedule correos-enviar` activo; `MailSender` resuelto por Guice; `CorreosModule` instalado.
4. Encolar correo (test) → registro PENDIENTE; tras ≤ 5 min pasa a ENVIADO o FALLADO según resultado SMTP.
5. Pulsar "Reintentar" sobre FALLADO → vuelve a PENDIENTE preservando `numIntentos` y `logErrores`.
6. Intentar `UPDATE` del asunto vía API → rechazado por **V-007**.
7. Intentar `DELETE` → rechazado por **V-008**.
8. Login como cada rol → verificar menús, filtros JPQL, botones disponibles y forzar V-018 (supervisor sobre otro centro).
9. Vista @Buscar: sin DNI → listado vacío (U-006); con DNI inválido → mensaje V-015.
10. Vista @Grafica: sin fechas → V-016; `fechaDesde > fechaHasta` → V-017.

---

## Matriz de trazabilidad

### Validaciones (V-XXX)

| ID    | Descripción                                    | Capa                         | Ubicación                                                                                                                                           |
|-------|------------------------------------------------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| V-001 | `asunto` obligatorio                           | Modelo                       | `domains/TareaCorreo.xml` `<string name="asunto" required="true"/>`                                                                                 |
| V-002 | `cuerpoHtml` obligatorio                       | Modelo                       | `domains/TareaCorreo.xml` `<string name="cuerpoHtml" required="true" large="true"/>`                                                                |
| V-003 | `destinatarioEmail` obligatorio                | Modelo                       | `domains/TareaCorreo.xml` `<string name="destinatarioEmail" required="true"/>`                                                                      |
| V-004 | `destinatarioEmail` formato                    | Cliente + Servidor (defensa) | Cliente: `views/TareaCorreo.xml` action-validate `@Main-validateEmail-action`. Servidor: `DefaultTareaCorreoServiceImpl.validateInsert`             |
| V-005 | `destinatarioDni` formato si aportado          | Cliente + Servidor           | Cliente: `views/TareaCorreo.xml` action-validate `@Main-validateDni-action`. Servidor: `DefaultTareaCorreoServiceImpl.validateInsert` (DniUtil)     |
| V-006 | `de` obligatorio post-asignación               | Modelo                       | `domains/TareaCorreo.xml` `<string name="de" required="true"/>` (se valida tras `fireActionRule_asignarRemitente`)                                  |
| V-007 | Inmutabilidad de contenido en update           | Servidor                     | `DefaultTareaCorreoServiceImpl.validateUpdate`                                                                                                      |
| V-008 | Borrado prohibido                              | Servidor + Modelo            | `DefaultTareaCorreoServiceImpl.validateRemove` + `DefaultAdjuntoCorreoServiceImpl.validateRemove`; refuerzo `canRemove=false` en `auth-correos.xml` |
| V-009 | Inmutabilidad de adjuntos                      | Servidor                     | `DefaultTareaCorreoServiceImpl.validateUpdate` (colección) + `DefaultAdjuntoCorreoServiceImpl.validateUpdate`/`validateRemove`                      |
| V-010 | `AdjuntoCorreo.nombre` obligatorio             | Modelo                       | `domains/AdjuntoCorreo.xml` `<string name="nombre" required="true"/>`                                                                               |
| V-011 | `AdjuntoCorreo.fichero` obligatorio            | Modelo                       | `domains/AdjuntoCorreo.xml` `<many-to-one name="fichero" required="true"/>`                                                                         |
| V-012 | Reintento solo desde FALLADO                   | Servidor                     | `DefaultTareaCorreoServiceImpl.reintentar` (precondición sobre `tareaCorreoOriginal.estado`)                                                        |
| V-013 | Transiciones de estado válidas                 | Servidor                     | `DefaultTareaCorreoServiceImpl.validateUpdate` (matriz PENDIENTE↔ENVIANDO, ENVIANDO→ENVIADO/FALLADO, FALLADO→PENDIENTE)                             |
| V-014 | Búsqueda DNI obligatorio                       | Cliente                      | `views/TareaCorreo-buscar.xml` action-validate `@Buscar-validateDni-action`                                                                         |
| V-015 | Búsqueda DNI formato                           | Cliente                      | `views/TareaCorreo-buscar.xml` action-validate `@Buscar-validateDni-action` (DniUtil)                                                               |
| V-016 | Gráfica fechas obligatorias                    | Cliente                      | `views/TareaCorreo-grafica.xml` action-validate `@Grafica-validateFechas-action`                                                                    |
| V-017 | `fechaDesde <= fechaHasta`                     | Cliente                      | `views/TareaCorreo-grafica.xml` action-validate `@Grafica-validateFechas-action`                                                                    |
| V-018 | Supervisor solo reintenta correos de su centro | Servidor                     | `DefaultTareaCorreoServiceImpl.reintentar` (chequea rol + `entity.centro` vs `centroActivo`)                                                        |

### Reglas de negocio (R-XXX)

| ID    | Operación                      | Momento              | Ubicación                                                                                                                                                                           |
|-------|--------------------------------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R-001 | insert TareaCorreo             | Antes super.insert   | `DefaultTareaCorreoServiceImpl.fireActionRule_asignarRemitente`                                                                                                                     |
| R-002 | insert TareaCorreo             | Antes super.insert   | `DefaultTareaCorreoServiceImpl.fireActionRule_inicializarCamposSistema`                                                                                                             |
| R-003 | insert TareaCorreo             | Antes super.insert   | `DefaultTareaCorreoServiceImpl.fireActionRule_snapshotDestinatario`                                                                                                                 |
| R-004 | procesarPendientes / enviarUna | Antes del envío SMTP | `DefaultTareaCorreoServiceImpl.fireActionRule_marcarComoEnviando`                                                                                                                   |
| R-005 | enviarUna                      | Después de send() OK | `DefaultTareaCorreoServiceImpl.fireActionRule_marcarComoEnviado`                                                                                                                    |
| R-006 | enviarUna                      | Después de send() KO | `DefaultTareaCorreoServiceImpl.fireActionRule_marcarComoFallado`                                                                                                                    |
| R-007 | reintentar                     | Antes super.update   | `DefaultTareaCorreoServiceImpl.fireActionRule_reabrirParaReintento`                                                                                                                 |
| R-008 | enviarUna                      | Durante el envío     | `DefaultTareaCorreoServiceImpl.enviarUna` (construye `Mail` + `MailSender.send`)                                                                                                    |
| R-009 | insert AdjuntoCorreo           | Antes super.insert   | `DefaultAdjuntoCorreoServiceImpl.fireActionRule_copiarFicheroAdjunto`                                                                                                               |
| R-010 | Listados @Centro y @Buscar     | En query             | `views/TareaCorreo-centro.xml` y `views/TareaCorreo-buscar.xml` (`domain="self.centro IS NOT NULL AND ..."`) + `TareaCorreoRepository.findForCentro` / `findForBusquedaDniEnCentro` |

### Reglas de UI (U-XXX)

| ID    | Vista                                        | Ubicación / mecanismo                                                                                                                                       |
|-------|----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| U-001 | `@Main`, `@Centro`, `@Mis`, `@Buscar` (form) | `readonlyIf="id != null"` aplicado a campos de contenido + destinatario, y `action-attrs` `@Main-onLoad-action` (y análogas)                                |
| U-002 | Todas las forms                              | `readonly="true"` en panel "Estado y trazabilidad" sobre `estado`, `fechaCreacion`, `fechaUltimoIntento`, `numIntentos`, `fechaEnvioOk`, `logErrores`, `de` |
| U-003 | `@Main`, `@Centro`                           | Botón "Reintentar" con `showIf="estado == 'FALLADO'"` + visibilidad ligada al rol vía permisos del menú; comportamiento de servidor refuerza V-018          |
| U-004 | `@Main`                                      | Botón "Nuevo correo" declarado únicamente en `views/TareaCorreo.xml`; ausente en `-centro`/`-buscar`/`-mis`/`-grafica`                                      |
| U-005 | `@Buscar`                                    | `<search-fields>` con campo `dni` `required="true"` + action-validate `@Buscar-validateDni-action`                                                          |
| U-006 | `@Buscar`                                    | `action-attrs` `@Buscar-listadoVacio-action` fija dominio imposible si `dni` vacío o inválido                                                               |
| U-007 | `@Grafica`                                   | `<search-fields>` con `fechaDesde` y `fechaHasta` `required="true"` + action-validate `@Grafica-validateFechas-action`                                      |
| U-008 | Forms `@Main`/`@Centro`/`@Mis`               | Panel "Log de errores" con `hideIf="!logErrores"`                                                                                                           |
| U-009 | Forms `@Main`/`@Centro`/`@Mis`               | Panel "Expediente relacionado" con `hideIf="!historialExpediente"`                                                                                          |
| U-010 | Todas las forms con cuerpo                   | `action-attrs` `@Main-onLoad-action` (y análogas `@Centro/@Mis/@Buscar`) que renderiza `cuerpoHtml` saneado en widget HTML readonly                         |

---

## Conflictos detectados con guías

Ninguno (las dos discrepancias del pre-flight quedaron resueltas antes de generar el diseño):

- **Cadencia del scheduler**: la guía decía "cada minuto", el análisis dice 5 minutos configurables. Se adoptó la versión del análisis: cron por defecto `0 0/5 * * * ?` y parámetro `correos.scheduler.intervaloMinutos = 5` documental en `axelor-config.properties`. La guía debería actualizarse manualmente.
- **Ubicación del `provideMailSender`**: la guía pedía decidir entre Provider Guice o dentro del scheduler. Se adoptó un `MailSenderProvider` Guice registrado en `CorreosModule` (única excepción al "no Guice para ModelService" — `MailSender` es infraestructura, no `ModelService`).

## Notas de unificación

- Cada `<action-view>` vive en su propio fichero; la única excepción explícita es `TareaCorreo-ref.xml`, que reúne `@Search-grid` y `@View-form` por convención del skill.
- El servicio `TareaCorreoService` expone una API interna `encolar(EncolarCorreoDTO)` para que otros subsistemas (registroentradasalida, expedientes, firmas…) puedan encolar correos sin acoplarse a la entidad ni al CRUD.
- `TareaCorreoController.buscarPorDni` y `datosGrafica` son `@CallMethod` auxiliares para la UI; el cliente puede usar también el `domain=` declarativo de las vistas, pero estos métodos centralizan la lógica defensiva (V-014/V-015/V-016/V-017 en servidor) y la transformación de resultados para la chart.
