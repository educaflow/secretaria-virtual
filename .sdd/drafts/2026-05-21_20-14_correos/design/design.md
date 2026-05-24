---
type: design
---

# Diseño técnico: subsistema Correos

**Objetivo:** Registrar en BD cada correo que la aplicación envía o intenta enviar a un destinatario identificado por DNI, enviarlo de forma asíncrona vía `base.infrastructure.mail`, y ofrecer vistas de consulta según el rol (Todos, Mi centro, Mis correos, Gráfica).
**Capa:** `subsystem/correos` (paquete `com.educaflow.subsystem.correos`)
**Módulo de dominio:** `name="correos"` → `com.educaflow.subsystem.correos.db`
**Prefijo de vistas/acciones:** `subsysCorreos`
**Análisis de origen:** `.sdd/drafts/2026-05-21_20-14_correos/analysis/analysis.md`
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas

Entidades: `Correo` (1) ─ (N) `AdjuntoCorreo`; enum `EstadoCorreo` (PENDIENTE / ENVIADO / FALLIDO). El análisis es la fuente de verdad; no se inventa nada fuera de él.

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/domains/Correo.xml` | Crear | k-sistemas (modelos.md) | Entidad `Correo` + enum `EstadoCorreo` + finders. |
| `subsystem/correos/domains/AdjuntoCorreo.xml` | Crear | k-sistemas (modelos.md) | Entidad `AdjuntoCorreo` (hija de `Correo`). |
| `subsystem/correos/service/CorreoService.java` | Crear | k-sistemas (servicios.md) | Interfaz `extends ModelService<Correo>` + tripletas de acciones propias. |
| `subsystem/correos/service/CorreoInsertDTO.java` | Crear | k-sistemas, k-secure-coding | `record` DTO para el alta programática. |
| `subsystem/correos/service/impl/CorreoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Implementación `extends DefaultModelService<Correo>`. |
| `subsystem/correos/service/impl/CorreoMailFactory.java` | Crear | k-code-quality | Construye el `Mail`/`Attach` desde un `Correo` (ver `rules/R-Correo-006.md`). |
| `subsystem/correos/service/impl/ResultadoEnvio.java` | Crear | k-code-quality | `record` interno del resultado de un intento (ver `rules/R-Correo-006.md`). |
| `subsystem/correos/service/AdjuntoCorreoService.java` | Crear | k-sistemas | Interfaz `extends ModelService<AdjuntoCorreo>` (inmutabilidad). |
| `subsystem/correos/service/impl/AdjuntoCorreoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Inmutabilidad del adjunto. |
| `subsystem/correos/controller/CorreoController.java` | Crear | k-sistemas (controladores.md) | `@CallMethod` de `btnReenviar` y `onChangeDni`. |
| `subsystem/correos/module/CorreosModule.java` | Crear | k-sistemas | `extends AxelorModule`; binding `MailSender → MailSenderImpl`. |
| `subsystem/correos/jobs/EnviarCorreosPendientesJob.java` | Crear | k-scheduler | Job Quartz que dispara el envío (ver `rules/R-Correo-006.md`). |
| `subsystem/correos/views/Correo-Todos.xml` | Crear | k-vistas | `@Todos-action` + grid + **form compartido** `@Main-form` + sub-grid/form AdjuntoCorreo + acciones. |
| `subsystem/correos/views/Correo-MiCentro.xml` | Crear | k-vistas | `@MiCentro-action` + grid (reusa `@Main-form`). |
| `subsystem/correos/views/Correo-Mios.xml` | Crear | k-vistas | `@Mios-action` + grid (reusa `@Main-form`). |
| `subsystem/correos/views/Correo-GraficaDia.xml` | Crear | k-vistas (charts.md) | `@GraficaDia-action` + chart apilado por estado, granularidad día. |
| `subsystem/correos/views/Correo-GraficaSemana.xml` | Crear | k-vistas (charts.md) | `@GraficaSemana-action` + chart, granularidad semana. |
| `subsystem/correos/views/Correo-GraficaMes.xml` | Crear | k-vistas (charts.md) | `@GraficaMes-action` + chart, granularidad mes. |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | **Modificar** | k-vistas (menus.md) | Bloque de menús de Correos (ya presente; ajustar Gráfica a 3 subentradas). |
| `subsystem/common/domains/User.xml` | **Modificar** | k-sistemas (modelos.md) | Añadir `<finder-method name="findByDni" ...>` para resolver email por DNI (si no existe). |
| `data-init/input/MetaSchedule.xml` (o alta por UI) | Crear | k-scheduler | Registro `MetaSchedule` con cron desde `correos.envio.cron`. |

> `db/repo/` queda vacío (con `.gitkeep`): los `<finder-method>` del dominio bastan; **NO** se crea `CorreoRepository` personalizado y por tanto **NO** se pone `repository="abstract"` en `Correo` (regla k-sistemas).
> **NO** se crea `AdjuntoCorreoController`: la descarga del adjunto la da el widget `binary` de la columna `contenido` (endpoint nativo de `MetaFile`).

---

## Paso 1 — Dominios

`design/domains/Correo.xml` — entidad `Correo` + enum `EstadoCorreo`.
- **Campos cliente** (`required="true"`): `asunto`, `cuerpo` (HTML, `large`), `dniDestinatario`, `emailDestinatario`.
- **Campos servidor** (SIN `required`, los rellena el servicio tras el persist — k-sistemas/modelos.md): `fechaCreacion`, `fechaEnvio`, `estado` (enum), `numeroIntentos`, `fechaUltimoIntento`, `motivoUltimoFallo`, `centro` (→ `Centro`), `referenciaHistorialEstadoExpediente` (→ `com.educaflow.subsystem.expedientes.db.HistorialEstado`, opcional).
- `one-to-many adjuntos` (`mappedBy="correo"`).
- Finders: `findByEstado`, `findByCentro`, `findByDniDestinatario`.
- Enum `EstadoCorreo`: PENDIENTE / ENVIADO / FALLIDO.

`design/domains/AdjuntoCorreo.xml` — `nombreFichero` (cliente, required), `contenido` (`MetaFile`, cliente, required), `correo` (→ `Correo`, servidor, required; lo fija el `onNew __parent__` del modal hijo). Borrado en cascada **desde el padre** (R-Correo-008, materializado en `CorreoServiceImpl.remove`).

> **Decisión `contenido` = `MetaFile`** (no `<binary>`): descarga nativa con widget `binary`/`binary-link`, `fileName`/`fileType` ya gestionados, y construcción del `Attach` con `MetaFileUtil.downloadContent`. Es el patrón del proyecto (DocumentoFirma / RegistroSalida). La "copia inmutable" se garantiza por la inmutabilidad del `AdjuntoCorreo` (update prohibido).

---

## Paso 2 — Servicios

### `CorreoService` (interfaz) — `extends ModelService<Correo>`

```java
public interface CorreoService extends ModelService<Correo> {
    // Alta programática (otro subsistema). El DTO ES la whitelist (k-secure-coding §3.5).
    Correo insert(CorreoInsertDTO dto);
    Optional<BusinessMessages> validateInsert(CorreoInsertDTO dto);

    // Reenviar (FALLIDO -> PENDIENTE) — invocada desde @CallMethod del controlador.
    Correo reenviar(Correo correo, Correo correoOriginal);
    Optional<BusinessMessages> validateReenviar(Correo correo, Correo correoOriginal);
    AllowProperties allowPropertiesReenviar();

    // Envío asíncrono — invocada por el Job (NO desde @CallMethod -> sin allowProperties).
    void enviarCorreosPendientes();

    // Lectura: propone el email del User con ese DNI (campo cliente, editable). U-correo-001 / R-Correo-004.
    String proponerEmailPorDni(String dni);
}
```

`insert(Correo)` / `update` / `remove` / `validateInsert(Correo)` / `validateUpdate` / `allowPropertiesInsert` se **sobrescriben** en la `*Impl` (añaden lógica real); **NO** se re-declaran en la interfaz (vienen de `ModelService<T>`).

### `CorreoServiceImpl` — firmas + comentarios (SIN cuerpos)

Bloques en orden: (1) acciones, (2) validación, (3) allowProperties, (4) action rules, (5) otras.

```java
public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    @Inject private MailSender mailSender;                    // NO ModelService -> @Inject + binding en CorreosModule
    @Inject private CorreoMailFactory correoMailFactory;      // colaborador de construcción del Mail
    @Inject private ModelServiceFactory modelServiceFactory;  // por si hay que resolver otro ModelService

    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) { super(model, repository); }

    /* ===== (1) ACCIONES ===== */

    // insert(Correo) — alta MANUAL (Administrador, vía REST /ws/rest). Sobrescrito porque dispara R-Correo-001 y R-Correo-002.
    //   Cuerpo: validateInsert(correo).ifPresent(throwIfInvalid); fireActionRule_InicializarCorreo(correo); fireActionRule_AltaManualSinCentro(correo); return repository.save(correo);
    //   MUST validar como PRIMERA línea: al persistir con repository (NUNCA super.insert) nadie valida por ti.
    @Override public Correo insert(Correo correo);

    // insert(CorreoInsertDTO) — alta PROGRAMÁTICA. validateInsert(dto).ifPresent(throwIfInvalid) como primera línea, construye el Correo,
    //   dispara R-Correo-001 y R-Correo-003 (centro + referencia del invocador), y persiste con repository.save(correo).
    @Override public Correo insert(CorreoInsertDTO dto);

    // update(Correo,Correo) — INMUTABLE: lanza UnsupportedOperationException incondicional (k-secure-coding §9.2).
    @Override public Correo update(Correo correo, Correo correoOriginal);

    // remove(Correo) — R-Correo-008: validateRemove(correo).ifPresent(throwIfInvalid); borra en cascada sus AdjuntoCorreo (borrado de hijos) ANTES de repository.remove(correo).
    @Override public void remove(Correo correo);

    // reenviar — validateReenviar(...).ifPresent(throwIfInvalid) primera línea; dispara R-Correo-005 (estado=PENDIENTE) y persiste con repository.save.
    @Override public Correo reenviar(Correo correo, Correo correoOriginal);

    // enviarCorreosPendientes — envío asíncrono. R-Correo-006 + R-Correo-007.
    //   Diseño detallado en design/rules/R-Correo-006.md.
    //   Recupera findByEstado(PENDIENTE) y procesa cada correo AISLADO (transacción + try/catch por correo;
    //   NUNCA @Transactional global): fireActionRule_RegistrarIntento -> correoMailFactory.build -> mailSender.send
    //   -> fireActionRule_RegistrarResultadoEnvio. Logs sin datos sensibles (id+estado, CRLF saneado).
    @Override public void enviarCorreosPendientes();

    // proponerEmailPorDni — lectura: devuelve el email del User con ese DNI (o null). Sin persistencia.
    //   Resuelve vía finder findByDni del User (ver Paso 4 / modificación de common/User.xml).
    @Override public String proponerEmailPorDni(String dni);

    /******** Métodos de Validación ********/

    // validateInsert(Correo) — V-Correo-001..004 (campos cliente obligatorios) +
    //   V-Correo-006 (rechaza si referenciaHistorialEstadoExpediente llega NO nula por REST: solo asignable programáticamente).
    @Override public Optional<BusinessMessages> validateInsert(Correo correo);

    // validateInsert(CorreoInsertDTO) — validación de los campos del DTO del alta programática.
    @Override public Optional<BusinessMessages> validateInsert(CorreoInsertDTO dto);

    // validateUpdate(Correo,Correo) — V-Correo-005 + V-Correo-006: SIEMPRE rechaza (entidad inmutable, k-secure-coding §9.2).
    //   Mensaje debe transmitir: el correo ya creado no admite modificación de sus datos de envío.
    @Override public Optional<BusinessMessages> validateUpdate(Correo correo, Correo correoOriginal);

    // validateReenviar — V-Correo-007: rechaza si estado != FALLIDO.
    //   Mensaje debe transmitir: el estado actual recibido y que solo se reenvía en FALLIDO.
    //   Además comprueba autorización (solo Administrador) con SecurityUtil (defensa para Vía B; nunca en el controlador).
    @Override public Optional<BusinessMessages> validateReenviar(Correo correo, Correo correoOriginal);

    /******** AllowProperties ********/

    // allowPropertiesInsert — WHITELIST solo de campos cliente: asunto, cuerpo, dniDestinatario, emailDestinatario,
    //   adjuntos (con sub-whitelist nombreFichero, contenido). Los servidor quedan FUERA. Ver §Frontera de confianza.
    @Override public AllowProperties allowPropertiesInsert();

    // allowPropertiesReenviar — whitelist VACÍA (Map.of()): reenviar no acepta ningún campo del cliente.
    @Override public AllowProperties allowPropertiesReenviar();

    /******** Action Rules ********/

    // R-Correo-001: estado=PENDIENTE, fechaCreacion=now, numeroIntentos=0, fechaEnvio/fechaUltimoIntento/motivoUltimoFallo=null (INCONDICIONAL, sin if==null). k-secure-coding §3.3.
    private void fireActionRule_InicializarCorreo(Correo correo);
    // R-Correo-002: centro=null en alta manual (INCONDICIONAL).
    private void fireActionRule_AltaManualSinCentro(Correo correo);
    // R-Correo-003: centro=dto.centro(); referenciaHistorialEstadoExpediente=dto.referenciaHistorial() (INCONDICIONAL, alta programática).
    private void fireActionRule_AltaProgramaticaCentroYReferencia(Correo correo, CorreoInsertDTO dto);
    // R-Correo-005: estado=PENDIENTE (reenvío, INCONDICIONAL).
    private void fireActionRule_ReactivarCorreo(Correo correo);
    // R-Correo-006 / R-Correo-007 — ver design/rules/R-Correo-006.md.
    public void fireActionRule_RegistrarIntento(Correo correo);
    public void fireActionRule_RegistrarResultadoEnvio(Correo correo, boolean exito, String motivo);

    /******** Otras funciones ********/
    // (la construcción del Mail vive en CorreoMailFactory, no aquí)
}
```

> **R-Correo-004 (proponer email por DNI)** NO asigna un campo `servidor`: `emailDestinatario` es `cliente` (el Administrador lo confirma/edita). Se materializa como **U-correo-001** vía `CorreoController.onChangeDni` → `proponerEmailPorDni` → `setValue`. No tiene `fireActionRule_`.

`CorreoMailFactory` y `ResultadoEnvio`: ver `design/rules/R-Correo-006.md`.

### `AdjuntoCorreoService` / `AdjuntoCorreoServiceImpl`

```java
public interface AdjuntoCorreoService extends ModelService<AdjuntoCorreo> { /* solo sobrescribe heredados */ }

public class AdjuntoCorreoServiceImpl extends DefaultModelService<AdjuntoCorreo> implements AdjuntoCorreoService {
    public AdjuntoCorreoServiceImpl(Class<AdjuntoCorreo> model, Repository<AdjuntoCorreo> repository) { super(model, repository); }
    // validateInsert  -> V-AdjuntoCorreo-001 (nombreFichero), V-AdjuntoCorreo-002 (contenido)
    // validateUpdate  -> V-AdjuntoCorreo-003 (SIEMPRE rechaza: adjunto inmutable)
    // update          -> UnsupportedOperationException (k-secure-coding §9.2)
    // allowPropertiesInsert -> createAllowProperties(Map.of("nombreFichero", Map.of(), "contenido", Map.of()))  // 'correo' es servidor: fuera
}
```

### `CorreoInsertDTO` (alta programática)

```java
public record CorreoInsertDTO(
        String asunto, String cuerpo, String dniDestinatario, String emailDestinatario,
        Centro centro,                                      // servidor: lo dicta el invocador (R-Correo-003)
        HistorialEstado referenciaHistorial,                // opcional, externo (com.educaflow.subsystem.expedientes.db.HistorialEstado)
        List<AdjuntoCorreoInsertDTO> adjuntos               // opcional
) { /* requireNonNull de los obligatorios: asunto, cuerpo, dniDestinatario, emailDestinatario, centro */ }
```
El DTO **es** la whitelist (k-secure-coding §3.5): no pasa por REST ni por `AllowProperties`. `centro` y `referenciaHistorial` están justificados porque los aporta deliberadamente el subsistema invocador.

---

## Paso 3 — Repositorios

Sin repositorio personalizado. Los `<finder-method>` del dominio (`findByEstado`, `findByCentro`, `findByDniDestinatario`) generan los métodos en `AbstractCorreoRepository`. El job usa `findByEstado(PENDIENTE)`; los listados por centro/DNI los resuelve el `<domain>` de cada `<action-view>`. `Correo` NO lleva `repository="abstract"`.

---

## Paso 4 — Controladores

`controller/CorreoController.java` — `@Inject private ModelServiceFactory modelServiceFactory;`. Parámetros siempre `actionRequest` / `actionResponse`.

- `@CallMethod @Transactional btnReenviar(actionRequest, actionResponse)`: resuelve `CorreoService`; `arh.getOriginalModel()` + `arh.getModel(correoService.allowPropertiesReenviar())`; llama `validateReenviar` y, si OK, `reenviar`; `actionResponse.setSignal("refresh-tab", null)`. La autorización (solo Administrador) vive en el SERVICIO, no aquí (controladores.md, k-secure-coding §4).
- `@CallMethod onChangeDni(actionRequest, actionResponse)` (U-correo-001 / R-Correo-004): obtiene el DNI del contexto, llama `correoService.proponerEmailPorDni(dni)` y `actionResponse.setValue("emailDestinatario", email)` (queda editable por el Administrador).

> Para resolver el email por DNI, `proponerEmailPorDni` usa un finder `findByDni` sobre `User`. Se añade `<finder-method name="findByDni" using="String:dni" filter="self.dni = :dni"/>` a `subsystem/common/domains/User.xml` (espejo del patrón existente en `CertificadoDigital`), salvo que ya exista una vía equivalente en `registrousuario`.

> **NO** se crea `AdjuntoCorreoController`: la descarga del adjunto la da el widget `binary` de la columna `contenido` (endpoint nativo de `MetaFile`).
> **U-grafica-002** (fecha final < inicial) **NO** se implementa con un controlador: se resuelve por el filtro `BETWEEN` del dataset (rango invertido → gráfica vacía). Ver Notas de unificación.

---

## Paso 5 — Vistas

Convención "un `<action-view>` por fichero". El **form `subsysCorreos.Correo@Main-form` es compartido** alta+detalle y se ubica en `Correo-Todos.xml` (única pantalla con botón "Nuevo correo", del Administrador); `@MiCentro` y `@Mios` lo referencian por nombre en modo detalle (Axelor resuelve vistas por nombre global). El modo se discrimina con `id == null` (alta) / `id != null` (detalle).

| Fichero | Resumen estructural |
|---------|---------------------|
| `design/views/Correo-Todos.xml` | `action-view @Todos-action` (groups admins por menú, sin `<domain>`) → grid `@Todos-grid` (botón "Nuevo correo", orderBy `-fechaCreacion`) → **form `@Main-form`** (paneles Destinatario/Mensaje `readonlyIf id!=null`; `panel-related` adjuntos; panel Seguimiento `showIf id!=null`; botones Cancelar/Guardar `showIf id==null`, Cerrar `showIf id!=null`, Reenviar `showIf id!=null && estado=='FALLIDO'`) → sub-grid/form `Correo.AdjuntoCorreo@Main` (modal hijo, `onNew` asigna `correo=__parent__`) → action-groups y action-methods (`btnReenviar`→`CorreoController.btnReenviar`, `onChangeDni`→`CorreoController.onChangeDni`). |
| `design/views/Correo-MiCentro.xml` | `action-view @MiCentro-action` con `<domain>self.centro = :__user__.centroActivo</domain>` (U-mi-centro-001) → grid `@MiCentro-grid` (`canNew="false"`) → reusa `@Main-form`. |
| `design/views/Correo-Mios.xml` | `action-view @Mios-action` con `<domain>self.dniDestinatario = :__user__.dni</domain>` (U-mis-001) → grid `@Mios-grid` (`canNew="false"`) → reusa `@Main-form`. |
| `design/views/Correo-GraficaDia.xml` | `action-view @GraficaDia-action` (sin `model`) → `chart @GraficaDia-chart` `stacked="true"`, 2 search-fields (fechaInicial, fechaFinal), dataset SQL `DATE_TRUNC('day', fecha_creacion)` agrupando por intervalo y estado, `series groupBy="_estado"`. |
| `design/views/Correo-GraficaSemana.xml` | Igual con `DATE_TRUNC('week', ...)`. |
| `design/views/Correo-GraficaMes.xml` | Igual con `DATE_TRUNC('month', ...)`, category `type="month"`. |

Todos los XML validados con `xmllint` contra `object-views.xsd`. (El selector de granularidad U-grafica-001 se materializa como 3 entradas de menú — ver Notas de unificación, límite de 2 `search-fields` en `<chart>`.)

---

## Paso 6 — Menús

`design/menus.xml` — porción a fusionar (ya presente en el proyecto; se ajusta la Gráfica a 3 subentradas):

- `correos-menuitem` "Correos" (order 40)
  - `correos-todos-menuitem` "Todos los correos" → `@Todos-action` `groups="admins"` (E-UN-002/008 control de acceso por rol)
  - `correos-miCentro-menuitem` "Correos de mi centro" → `@MiCentro-action`
  - `correos-mios-menuitem` "Mis correos" → `@Mios-action`
  - `correos-grafica-menuitem` "Gráfica de correos" `groups="admins"` (sin acción, sub-parent)
    - `correos-graficaDia-menuitem` "Diaria" → `@GraficaDia-action`
    - `correos-graficaSemana-menuitem` "Semanal" → `@GraficaSemana-action`
    - `correos-graficaMes-menuitem` "Mensual" → `@GraficaMes-action`

En la tabla de ficheros: **Modificar** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

---

## Paso 7 — Envío asíncrono (E-UB-010 / E-UB-011 / E-UB-012)

Diseño detallado en `design/rules/R-Correo-006.md`. Resumen:
- `jobs/EnviarCorreosPendientesJob` (`implements org.quartz.Job`): resuelve `CorreoService` vía `Beans.get(ModelServiceFactory.class).resolve(Correo.class)` y llama `enviarCorreosPendientes()`; captura excepciones globales para no reventar el scheduler.
- `MetaSchedule` `name="correos-enviar-pendientes"`, `job=...jobs.EnviarCorreosPendientesJob`, `cron` desde la propiedad `correos.envio.cron` (default `0 */5 * * * ?`), `active=true`. Requiere `quartz.enable=true` en `axelor-config`.
- `CorreoModule` bindea `MailSender → MailSenderImpl`.

---

## Paso 8 — Seguridad

- **Multi-centro / IDOR (k-secure-coding §4):** `@MiCentro` filtra por `:__user__.centroActivo`; `@Mios` por `:__user__.dni`; `@Todos` sin filtro (solo `groups="admins"`). El `centro` nunca lo dicta el cliente: alta manual → `null` (R-Correo-002 incondicional); alta programática → del DTO del invocador (R-Correo-003 incondicional).
- **Autorización por rol:** "solo Administrador crea/reenvía/ve la gráfica" → menú `groups="admins"` y, para reenviar, **además** dentro de `validateReenviar`/`reenviar` con `SecurityUtil` (defensa para la Vía B). Nunca en el controlador.
- **Inmutabilidad (k-secure-coding §9.2):** `Correo.update` y `AdjuntoCorreo.update` lanzan `UnsupportedOperationException`; sus `validateUpdate` rechazan siempre. Las transiciones de estado son acciones propias (`reenviar`, `enviarCorreosPendientes`).
- **JPQL/dominios:** solo `:__user__.x` y `:param`; cero concatenación. SQL del chart con parámetros nombrados.
- **Adjuntos:** `contenido` es `MetaFile`; el `Attach` se monta con `MetaFileUtil.downloadContent`; nombre visible `nombreFichero`. Límite de tamaño: el de Axelor para `MetaFile`.
- **Logs:** solo `id`/estado del Correo; nunca cuerpo, email completo ni bytes del adjunto; CRLF saneado.

---

## Paso 9 — Verificación

`./gradlew clean build --info`

- Genera `Correo`, `AdjuntoCorreo`, `EstadoCorreo`, `AbstractCorreoRepository`, `AbstractAdjuntoCorreoRepository`.
- `ModelServiceFactory` descubre `CorreoServiceImpl` y `AdjuntoCorreoServiceImpl` por convención (sin módulo).
- `CorreosModule` registra `MailSender → MailSenderImpl`.
- `xmllint` de dominios contra `domain-models.xsd` y de vistas/menús contra `object-views.xsd` (los 9 ficheros validan).

---

## Frontera de confianza — AllowProperties por acción

Clasificación (columna "Origen del valor"):
- **Correo cliente (5):** `asunto`, `cuerpo`, `dniDestinatario`, `emailDestinatario`, `adjuntos`.
- **Correo servidor (8):** `fechaCreacion`, `fechaEnvio`, `estado`, `numeroIntentos`, `fechaUltimoIntento`, `motivoUltimoFallo`, `centro`, `referenciaHistorialEstadoExpediente`.
- **AdjuntoCorreo cliente (2):** `nombreFichero`, `contenido`. **AdjuntoCorreo servidor (1):** `correo`.

### `CorreoServiceImpl.insert(Correo)` (alta manual, vía REST `/ws/rest`)

Entidad `Correo`. **Forma elegida:** `createAllowProperties` (whitelist).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|---------------------------------------------|
| `asunto`, `cuerpo`, `dniDestinatario`, `emailDestinatario` | cliente | sí | Input del usuario. |
| `adjuntos` | cliente | sí (sub-whitelist `nombreFichero`, `contenido`) | Adjuntos aportados en el alta. |
| `estado`, `fechaCreacion`, `numeroIntentos`, `fechaEnvio`, `fechaUltimoIntento`, `motivoUltimoFallo` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_InicializarCorreo` (R-Correo-001). |
| `centro` | servidor | **NO** | Asignado `null` incondicionalmente en `fireActionRule_AltaManualSinCentro` (R-Correo-002). |
| `referenciaHistorialEstadoExpediente` | servidor | **NO** | El alta manual no la toca; `validateInsert` rechaza si llega no nula por REST (V-Correo-006). |

### `CorreoServiceImpl.insert(CorreoInsertDTO)` (alta programática)

El **DTO es la whitelist** (k-secure-coding §3.5; no pasa por REST). `centro` y `referenciaHistorial` aparecen en el record porque los aporta el subsistema invocador (justificados, R-Correo-003). El resto de campos servidor (estado, fechas, intentos…) se asignan en `fireActionRule_InicializarCorreo` (R-Correo-001), no vienen del DTO.

### `CorreoServiceImpl.reenviar` (`@CallMethod CorreoController.btnReenviar`)

Entidad `Correo`. **Forma:** `createAllowProperties(Map.of())` (whitelist vacía). Ningún campo del cliente; `estado=PENDIENTE` se asigna incondicionalmente (R-Correo-005). Resto de campos servidor: fuera.

### `CorreoServiceImpl.update` / `AdjuntoCorreoServiceImpl.update`

Inmutables: `UnsupportedOperationException` + `validateUpdate` rechaza. Ningún campo aceptado.

### `AdjuntoCorreoServiceImpl.insert` (cascada del maestro)

**Forma:** `createAllowProperties(Map.of("nombreFichero", Map.of(), "contenido", Map.of()))`. `correo` (servidor) fuera de la whitelist; lo fija el `onNew __parent__` del modal hijo.

Reglas aplicadas (k-secure-coding §3): ninguna whitelist enumera un campo `servidor` ✔; todo campo servidor que la acción fija se asigna incondicional (sin `if==null`) ✔; no se usa `createAllowAllProperties()` ✔; `centro`/`referenciaHistorialEstadoExpediente` solo entran por el DTO programático ✔.

---

## Trazabilidad V/R/U → ubicación

### Validaciones (V-)
| ID | Ubicación |
|----|-----------|
| V-Correo-001 (dni obligatorio) | `CorreoServiceImpl.validateInsert(Correo)` + `required="true"` en `dniDestinatario`. |
| V-Correo-002 (email obligatorio) | `CorreoServiceImpl.validateInsert(Correo)` + `required="true"` en `emailDestinatario`. |
| V-Correo-003 (asunto obligatorio) | `CorreoServiceImpl.validateInsert(Correo)` + `required="true"` en `asunto`. |
| V-Correo-004 (cuerpo obligatorio) | `CorreoServiceImpl.validateInsert(Correo)` + `required="true"` en `cuerpo`. |
| V-Correo-005 (inmutable tras crear) | `CorreoServiceImpl.validateUpdate` (rechaza siempre) + `update` lanza `UnsupportedOperationException`. |
| V-Correo-006 (referencia no asignable desde UI) | `referenciaHistorialEstadoExpediente` fuera de `allowPropertiesInsert` + `validateInsert(Correo)` rechaza si llega no nula; `readonly` en la vista (UX). |
| V-Correo-007 (solo reenvía FALLIDO) | `CorreoServiceImpl.validateReenviar`. |
| V-AdjuntoCorreo-001 (nombre obligatorio) | `AdjuntoCorreoServiceImpl.validateInsert` + `required="true"` en `nombreFichero`. |
| V-AdjuntoCorreo-002 (contenido obligatorio) | `AdjuntoCorreoServiceImpl.validateInsert` + `required="true"` en `contenido`. |
| V-AdjuntoCorreo-003 (adjunto inmutable) | `AdjuntoCorreoServiceImpl.validateUpdate` (rechaza siempre) + `update` lanza `UnsupportedOperationException`. |

### Reglas de negocio (R-)
| ID | Ubicación |
|----|-----------|
| R-Correo-001 (init estado/fecha/intentos) | `fireActionRule_InicializarCorreo` (antes de `repository.save`, en ambos `insert`). |
| R-Correo-002 (alta manual sin centro) | `fireActionRule_AltaManualSinCentro` (en `insert(Correo)`). |
| R-Correo-003 (alta programática centro+referencia) | `fireActionRule_AltaProgramaticaCentroYReferencia` (en `insert(CorreoInsertDTO)`). |
| R-Correo-004 (proponer email por DNI) | UI: `CorreoController.onChangeDni` + `CorreoService.proponerEmailPorDni` + `setValue` (campo cliente). Ver U-correo-001. |
| R-Correo-005 (reenvío → PENDIENTE) | `fireActionRule_ReactivarCorreo` (en `reenviar`). |
| R-Correo-006 (registrar intento) | `fireActionRule_RegistrarIntento` (antes del intento). **Detalle: `design/rules/R-Correo-006.md`**. |
| R-Correo-007 (registrar resultado) | `fireActionRule_RegistrarResultadoEnvio` (después del intento). **Detalle: `design/rules/R-Correo-006.md`**. |
| R-Correo-008 (cascada borrado adjuntos) | `CorreoServiceImpl.remove` (borra hijos antes de `repository.remove`); `one-to-many adjuntos mappedBy="correo"`. |
| R-AdjuntoCorreo-001 (vincular al padre) | `subsysCorreos.Correo.AdjuntoCorreo@Main-set-correo-parent-action` (`<action-record>` con `__parent__`). |

### Reglas de UI (U-)
| ID | Ubicación |
|----|-----------|
| U-correo-001 (autocompletar email onChange dni) | `onChange="...@Main-onChange-dni-action"` → `<action-method>` → `CorreoController.onChangeDni`. |
| U-correo-002 (detalle solo lectura) | `readonlyIf="id != null"` en paneles Destinatario, Mensaje y `panel-related` adjuntos. |
| U-correo-003 (referencia siempre readonly) | `readonly="true"` fijo en `referenciaHistorialEstadoExpediente`. |
| U-correo-004 (panel Seguimiento oculto en alta) | `showIf="id != null"` en panel `Seguimiento`. |
| U-correo-005 (Reenviar solo admin+FALLIDO) | `showIf="id != null && estado == 'FALLIDO'"` en `btnReenviar` + `groups` del menú; defensa en `validateReenviar`. |
| U-correo-006 (Cancelar/Guardar solo alta) | `showIf="id == null"` en `btnCancel`/`btnSave`. |
| U-correo-007 (Cerrar solo detalle) | `showIf="id != null"` en `btnCerrar`. |
| U-mi-centro-001 (filtro centro) | `<domain>self.centro = :__user__.centroActivo</domain>` en `@MiCentro-action`. |
| U-mis-001 (filtro DNI) | `<domain>self.dniDestinatario = :__user__.dni</domain>` en `@Mios-action`. |
| U-grafica-001 (granularidad día/semana/mes) | 3 charts/action-views (`@GraficaDia`/`@GraficaSemana`/`@GraficaMes`) con `DATE_TRUNC('day'|'week'|'month', ...)`, expuestos como 3 subentradas de menú. Ver Notas de unificación. |
| U-grafica-002 (fecha final >= inicial) | Filtro `BETWEEN` (`fecha_creacion >= :fechaInicial AND <= :fechaFinal`) en el dataset de cada chart: un rango invertido devuelve la gráfica vacía. Ver Notas de unificación (divergencia con T-017). |

---

## Notas de unificación

1. **`referenciaHistorialEstadoExpediente` → `com.educaflow.subsystem.expedientes.db.HistorialEstado`.** Se verificó en el código real que la entidad existe (`HistorialEstado.xml` en `subsystem/expedientes`); los candidatos habían propuesto un FQN inexistente (`HistorialEstadoExpediente`). El campo es opcional y solo asignable programáticamente (V-Correo-006 / E-UN-009), así que compila sin problemas.
2. **`repository="abstract"` eliminado** de `Correo`: no hay repositorio personalizado (los finders bastan).
3. **`U-grafica-001` (selector de granularidad) → 3 entradas de menú.** Una `<chart>` admite como máximo 2 `search-fields` (`maxOccurs="2"` en `object-views.xsd`); las dos fechas los ocupan, así que el selector día/semana/mes no cabe como 3.er campo. Decisión confirmada con el usuario: 3 gráficas (Diaria/Semanal/Mensual), una por granularidad, expuestas como 3 subentradas del menú "Gráfica de correos".
4. **`U-grafica-002` (fecha final < inicial) → filtro `BETWEEN` en el dataset.** Decisión confirmada con el usuario: una `<chart>` no valida sus `search-fields` de forma declarativa; un rango invertido produce una gráfica vacía (sin mensaje explícito). **Divergencia con el test `T-017`**, que espera el mensaje literal "La fecha final no puede ser anterior a la fecha inicial.": al ejecutar los tests E2E, `T-017` deberá reinterpretarse como "la gráfica queda vacía" o ajustarse vía `/sdd-analyst-system` (el diseñador no modifica `tests.md`, contrato fijo).
5. **`R-Correo-006`/`R-Correo-007` (envío asíncrono)** se documentan en detalle en `design/rules/R-Correo-006.md` (Job Quartz, `CorreoMailFactory`, `ResultadoEnvio`, aislamiento transaccional por correo, tabla de errores). Defaults de implementación (from por config, mimeType del MetaFile, destinatario único, transacción por correo) recogidos allí.
6. **Resolución email por DNI**: `CorreoService.proponerEmailPorDni` usa un finder `findByDni` sobre `User` (a añadir en `subsystem/common/domains/User.xml`, espejo de `CertificadoDigital.findByDni`), salvo que `registrousuario` ofrezca una vía equivalente.
