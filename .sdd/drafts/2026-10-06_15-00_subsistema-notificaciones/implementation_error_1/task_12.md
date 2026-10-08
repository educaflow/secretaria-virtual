---
type: implementation-task
template: system
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-validaciones

## Servicio del canal correo (CorreoService + Impl)

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/service/CorreoService.java` | Crear | k-sistemas (servicios.md) | Servicio del canal correo |
| `subsystem/notificaciones/service/impl/CorreoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Canal correo (sustituye a `correos/service/impl/CorreoServiceImpl`) |

## Pasos del diseño

### Paso 3 — Interfaces de servicio y base abstracta del canal

**`CorreoService extends NotificacionCanalService<Correo>`**: `List<Correo> listarCorreosEnFail();` ⏎⏎⏎ `Optional<BusinessMessages> validateListarCorreosEnFail();` (se conserva de `correos`, como piden las guías).

> Contexto (base que esta clase extiende, la implementa la tarea 11): firmas de `NotificacionCanalService` y ganchos de `NotificacionCanalServiceImpl`.

**`NotificacionCanalService`** (`com.educaflow.subsystem.notificaciones.service`):

```java
public interface NotificacionCanalService<T extends Notificacion> extends ModelService<T> {
    T reenviar(T entidad, T entidadOriginal);


    Optional<BusinessMessages> validateReenviar(T entidad, T entidadOriginal);


    AllowProperties allowPropertiesReenviar();
}
```

**`NotificacionCanalServiceImpl<T extends Notificacion>`** (`…service.impl`, `public abstract class … extends DefaultModelService<T> implements NotificacionCanalService<T>`) — patrón nuevo (D1). Cumple C16 (nombre `*ServiceImpl` en `service.impl`); `ModelServiceFactory` nunca la instancia porque no hay entidad `NotificacionCanal`.

```java
@Inject EjecutorAsincrono ejecutorAsincrono;

/** Los mensajes «de su canal» que pide la spec; cada canal construye el suyo con I18n.get("literal"). */
protected record TextosCanal(String centroAjeno, String estadoExpedienteInexistente, String inmutable,
                             String noBorrable, String soloFallidas, String reenvioCentroAjeno) {}

protected NotificacionCanalServiceImpl(Class<T> model, Repository<T> repository);   // super(model, repository)

// ---------- Acciones (antes del primer header) ----------
@Override public T insert(T notificacion);
//   validateInsert(notificacion).ifPresent(BusinessMessages::throwIfInvalid);
//   fireActionRule_AsignarValoresIniciales(notificacion)        R-Notificacion-001 (Antes)
//   aplicarReglasDelCanalAntesDeGuardar(notificacion)           gancho: R-Correo-001 / R-Sms-001 (Antes)
//   notificacion = repository.save(notificacion)               (los adjuntos se guardan en cascada: R-Adjunto-001)
//   fireActionRule_ProgramarEnvioAsincrono(notificacion)        R-Notificacion-002 (Después)
//   return notificacion. MUST NOT super.insert.

@Override public T update(T nueva, T original);
//   V-Notificacion-012 (§9.2 de k-secure-coding): sin flujo normal, siempre
//   throw new UnsupportedOperationException(textosDelCanal().inmutable()).

@Override public void remove(T notificacion);
//   V-Notificacion-013: siempre throw new UnsupportedOperationException(textosDelCanal().noBorrable()).

@Override public T reenviar(T entidad, T entidadOriginal);
//   validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);
//   fireActionRule_ProgramarEnvioAsincrono(entidadOriginal)     R-Notificacion-004 (Después del commit)
//   return entidadOriginal. MUST NOT repository.save: reenviar no cambia nada de forma síncrona;
//   el cambio de estado lo hace enviar() (R-Notificacion-003).

protected abstract void aplicarReglasDelCanalAntesDeGuardar(T notificacion);
//   Gancho: reglas del canal que transforman datos antes de guardar (Correo: copiar adjuntos; Sms:
//   normalizar teléfono). Abstracto a propósito: cada canal decide explícitamente, aunque sea «nada».

protected abstract void enviarPorCanal(T notificacion);
//   Gancho: envía por el canal; lanza RuntimeException si falla (R-Notificacion-003, paso 5).

protected abstract TextosCanal textosDelCanal();
//   Gancho: los seis mensajes del canal, construidos con I18n.get("literal") en cada llamada (para que
//   el script de i18n los encuentre y se traduzcan al idioma del usuario).

// ---------- Métodos de Validación ----------
@Override public Optional<BusinessMessages> validateInsert(T notificacion);
//   Solo la secuencia de llamadas sobre un BusinessMessages, en este orden: validarMotivo, validarDniDestinatario,
//   validarNombreYApellidos, validarCentro, validarHistorialEstado, validateDatosDelCanal(notificacion, messages).
//   Devuelve Optional.empty() si no hay mensajes.

// Privados (bloque de validación). Los cuatro últimos se MUEVEN aquí desde los privados homónimos de
// correos/CorreoServiceImpl y sms/SmsServiceImpl (validarDestinatario de SMS se funde en los dos primeros),
// ampliados con las V nuevas; validarMotivo es nuevo (campo nuevo):
private void validarMotivo(T notificacion, BusinessMessages messages);
//   V-Notificacion-001 (RES-Notificacion-001) name vacío o solo espacios → mensaje de motivo obligatorio;
//   si no, V-Notificacion-002 (RES-Notificacion-002) más de 255 caracteres → mensaje de longitud máxima del motivo.
private void validarDniDestinatario(T notificacion, BusinessMessages messages);
//   V-Notificacion-003 (RES-Notificacion-014) DNI vacío → mensaje de DNI obligatorio; si no, V-Notificacion-004
//   (VAL-Correo-002, VAL-Sms-002) DniUtil.isValid falso → mensaje de DNI no válido (letra).
private void validarNombreYApellidos(T notificacion, BusinessMessages messages);
//   V-Notificacion-005/006 (VAL-*-003/004) nombre / apellidos vacíos → sus mensajes de obligatorio; si no,
//   V-Notificacion-007/008 (VAL-Correo-015/016, VAL-Sms-011/012) más de 255 → sus mensajes de longitud.
private void validarCentro(T notificacion, BusinessMessages messages);
//   V-Notificacion-009 (RES-Notificacion-013) centro null → mensaje de centro obligatorio; si no, y el usuario
//   no es administrador (SecurityUtil.isAdmin(SecurityUtil.getUser())) y no pertenece al centro
//   (user.perteneceAlCentro(centro)) → V-Notificacion-010 (VAL-Correo-014, VAL-Sms-010) textosDelCanal().centroAjeno().
private void validarHistorialEstado(T notificacion, BusinessMessages messages);
//   V-Notificacion-011 (RES-Notificacion-007) solo si centro y historialEstado no son null: el historialEstado
//   sin id (cáscara del cliente), sin expediente, o con expediente de otro centro →
//   textosDelCanal().estadoExpedienteInexistente(). Sin releer por repositorio (JPA.edit ya resolvió la referencia).

@Override public Optional<BusinessMessages> validateUpdate(T nueva, T original);
//   V-Notificacion-012 (RES-Notificacion-004): siempre Optional.of(single(textosDelCanal().inmutable())).

@Override public Optional<BusinessMessages> validateRemove(T notificacion);
//   V-Notificacion-013 (RES-Notificacion-005): siempre Optional.of(single(textosDelCanal().noBorrable())).

@Override public Optional<BusinessMessages> validateReenviar(T entidad, T entidadOriginal);
//   Sobre entidadOriginal (el estado real en BD; entidad no trae nada: allowPropertiesReenviar es deny-all).
//   Empieza con Objects.requireNonNull(entidadOriginal, "entidadOriginal no puede ser null"): que falte es un error
//   de programación del llamante (assert, sin I18n ni regla), no un mensaje de negocio.
//     - V-Notificacion-014 (VAL-Notificacion-001) estado != FALLIDO → textosDelCanal().soloFallidas().
//     - V-Notificacion-015 (VAL-Notificacion-002) ni SecurityUtil.isAdmin(user) ni
//       GestorNotificacionesUtil.idsCentrosGestionados(user).contains(entidadOriginal.getCentro().getId())
//       → textosDelCanal().reenvioCentroAjeno().

protected abstract void validateDatosDelCanal(T notificacion, BusinessMessages messages);
//   Gancho: añade a messages las V propias del canal.

// ---------- AllowProperties ----------
@Override public AllowProperties allowPropertiesInsert();
//   createAllowProperties(campos comunes cliente {name, dniDestinatario, nombre, apellidos, centro, historialEstado}
//   + allowPropertiesDelCanal()). Fuera: tipoNotificacion, destino, nombreExpediente y todos los del envío.

@Override public AllowProperties allowPropertiesUpdate();   // createDenyAllProperties() — inmutable
@Override public AllowProperties allowPropertiesRemove();   // createDenyAllProperties() — no se borra
@Override public AllowProperties allowPropertiesReenviar(); // createDenyAllProperties() — actúa sobre lo guardado

protected abstract Map<String, Object> allowPropertiesDelCanal();
//   Gancho: los campos cliente propios del canal, en el formato de AllowProperties.createAllowProperties.

// ---------- Action Rules ----------
private void fireActionRule_AsignarValoresIniciales(T notificacion);
//   R-Notificacion-001 (Origen spec: RN-Notificacion-001, CC-Notificacion-001, CC-Notificacion-004,
//   RES-Notificacion-003, RES-Correo-001, RES-Sms-001). Momento: Antes de repository.save.
//   Asignaciones INCONDICIONALES (k-secure-coding §3.3; MUST NOT `if (campo == null)`: el cliente no puede dictar
//   ninguno de estos campos aunque lleguen rellenos por el REST genérico):
//     tipoNotificacion = TipoNotificacion.deClase(EntityHelper.getEntityClass(notificacion));
//     estado = PENDIENTE; fechaCreacion = LocalDateTime.now(Convert.defaultZoneId); numeroReintentos = 0;
//     fechaPrimerIntentoEnvio = fechaUltimoIntentoEnvio = fechaEnvio = descripcionUltimoFallo = null.

private void fireActionRule_ProgramarEnvioAsincrono(T notificacion);
//   R-Notificacion-002 / R-Notificacion-004 (Origen spec: RN-Notificacion-002, RN-Notificacion-007). Momento: Después.
//   ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviar(id)) pasando solo el id. Detalle: rules/R-Notificacion-003.md.

private void fireActionRule_RegistrarIntentoEnvio(T notificacion);     // R-Notificacion-003 (RN-Notificacion-004)
private void fireActionRule_MarcarEnvioCorrecto(T notificacion);       // R-Notificacion-003 (RN-Notificacion-005)
private void fireActionRule_MarcarEnvioFallido(T notificacion, RuntimeException excepcion); // R-Notificacion-003 (RN-Notificacion-006)
//   Detalle de las tres en design/rules/R-Notificacion-003.md (asignaciones incondicionales; la fecha del primer
//   intento se asigna cuando numeroReintentos == 0, antes de incrementarlo — sin `if (campo == null)`).

// ---------- Otras funciones ----------
void enviar(Long notificacionId);
//   Package-private (lo prueban los tests). Implementa R-Notificacion-003; diseño detallado en
//   design/rules/R-Notificacion-003.md (transacción propia, PESSIMISTIC_WRITE sobre `model`, idempotente con ENVIADO).
```

### Paso 4 — Canal correo y adjunto

**`CorreoServiceImpl extends NotificacionCanalServiceImpl<Correo> implements CorreoService`**:

```java
@Inject Provider<MailSender> mailSenderProvider;   // Provider: se resuelve dentro del envío (RN-Correo-003)

public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository);   // super(model, repository)

// ---------- Acciones ----------
@Override public List<Correo> listarCorreosEnFail();
//   validateListarCorreosEnFail().ifPresent(throwIfInvalid); ((CorreoRepository) repository).findByEstado(FALLIDO).fetch().

@Override protected void aplicarReglasDelCanalAntesDeGuardar(Correo correo);   // fireActionRule_CopiarAdjuntos(correo)
@Override protected void enviarPorCanal(Correo correo);                        // mailSenderProvider.get().send(construirMail(correo))
@Override protected TextosCanal textosDelCanal();
//   new TextosCanal(...) con I18n.get de los mensajes que la spec da para el correo, en este orden:
//   centroAjeno = VAL-Correo-014; estadoExpedienteInexistente = RES-Notificacion-007 (versión correo);
//   inmutable = RES-Notificacion-004 (correo); noBorrable = RES-Notificacion-005 (correo);
//   soloFallidas = VAL-Notificacion-001 (correo); reenvioCentroAjeno = VAL-Notificacion-002 (correo).
//   Literales tal cual los da la spec (son los que hoy usa correos), dentro de I18n.get("…") para el script de i18n.

// ---------- Métodos de Validación ----------
@Override public Optional<BusinessMessages> validateListarCorreosEnFail();   // sin condiciones: Optional.empty()

@Override protected void validateDatosDelCanal(Correo correo, BusinessMessages messages);
//   Solo llama a los privados que se CONSERVAN de correos/CorreoServiceImpl, ampliados con las V nuevas:
//   validarDirecciones (V-Correo-001..005 y la nueva V-Correo-010), validarAsuntoYCuerpo (V-Correo-006/007/009 y la
//   nueva V-Correo-008) y uno nuevo, validarTamanoTotalAdjuntos (V-Correo-011). Reparto de las V:
//   - V-Correo-001 (VAL-Correo-005) separarDirecciones(para) vacía → mensaje de «para» sin destinatario.
//   - V-Correo-002 (VAL-Correo-006) más de una dirección en el «para» → mensaje de una sola dirección (use «en copia»).
//   - V-Correo-003 (VAL-Correo-007) exactamente una y !EMailUtil.isValid → mensaje de formato del «para».
//   - V-Correo-004 / V-Correo-005 (VAL-Correo-008/009) «en copia» / «en copia oculta» con alguna dirección no válida.
//   - V-Correo-010 (VAL-Correo-018) solo si «en copia» o «en copia oculta» no están en blanco: alguna dirección
//     repetida en la unión de las tres listas (comparación tras trim y en minúsculas) → mensaje de dirección repetida.
//   - V-Correo-006 (VAL-Correo-010) asunto vacío o solo espacios; si no, V-Correo-007 (VAL-Correo-011) > 255
//     y V-Correo-008 (VAL-Correo-017) contiene \p{Cntrl} (incluye saltos de línea) → sus mensajes.
//   - V-Correo-009 (VAL-Correo-012) cuerpo vacío o solo espacios.
//   - V-Correo-011 (VAL-Correo-019) solo si hay adjuntos: suma de MetaFileUtil.fileSizeOnDisk(contenido) de los
//     adjuntos con contenido > 25 MB → mensaje de tamaño total. Se mide en disco: fileSize del MetaFile lo dicta el cliente.

// ---------- AllowProperties ----------
@Override protected Map<String, Object> allowPropertiesDelCanal();
//   {para, enCopia, enCopiaOculta, asunto, cuerpo, adjuntos: {nombreFichero, contenido}}
//   (el `correo` del adjunto no: lo fija el mappedBy al guardar en cascada).

// ---------- Action Rules ----------
private void fireActionRule_CopiarAdjuntos(Correo correo);
//   R-Correo-001 (Origen spec: RN-Adjunto-001, RN-Adjunto-003). Momento: Antes de repository.save.
//   Para cada adjunto: copia = MetaFileUtil.cloneMetaFile(contenido); copia.setFileName(nombreFichero.trim());
//   adjunto.setContenido(copia). Así lo que viaja y se descarga no cambia si el original se toca, y la descarga
//   usa el nombre del adjunto.

// ---------- Otras funciones ----------
private List<String> separarDirecciones(String direcciones);   // split por comas, trim, sin vacíos (RN-Correo-004)
private Mail construirMail(Correo correo);
//   RN-Correo-001: to/cc/bcc = separarDirecciones(...), from = AppSettings "mail.address.from", asunto, cuerpo como
//   texto plano (html null), adjuntos como Fichero(nombreFichero, MetaFileUtil.downloadContent, fileType).
```

Verificación (al cerrar el bloque 1–9): compila.

## Regla compleja referenciada

`design/rules/R-Notificacion-003.md` (el gancho `enviarPorCanal` es su paso 5).

## Frontera de confianza — AllowProperties por acción

### `CorreoServiceImpl.reenviar` (invocado desde `CorreoController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Correo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Reenviar` de `entity-Notificacion.md` (ninguna).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | — | **NO** | El reenvío actúa sobre `entidadOriginal` (BD); el estado lo cambia `enviar()` (R-Notificacion-003). |

### `insert` de cada entidad (puerta REST `/ws/rest/<FQN>` y `save` de los forms)

No se invoca desde un `@CallMethod`, pero es la frontera real del alta y se documenta igual.

**`CorreoServiceImpl.allowPropertiesInsert`** (heredado; común + `allowPropertiesDelCanal`). Forma: `createAllowProperties`. Origen spec: `Input AllowProperties` de `Crear` de `entity-Correo.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `name`, `dniDestinatario`, `nombre`, `apellidos`, `centro`, `historialEstado` | cliente | sí | Comunes (base); `centro` e `historialEstado` validados en `validateInsert` (V-Notificacion-010/011). |
| `para`, `enCopia`, `enCopiaOculta`, `asunto`, `cuerpo` | cliente | sí | `allowPropertiesDelCanal` de Correo. |
| `adjuntos` → `nombreFichero`, `contenido` | cliente | sí (anidado) | Validados por `AdjuntoServiceImpl.validateInsert` en cascada; copiados por R-Correo-001. |
| `tipoNotificacion` | servidor | **NO** | R-Notificacion-001 (incondicional, desde `TipoNotificacion.deClase`). |
| `estado`, `fechaCreacion`, `numeroReintentos`, `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`, `descripcionUltimoFallo` | servidor | **NO** | R-Notificacion-001 en el alta; R-Notificacion-003 en el envío. |
| `destino`, `nombreExpediente` | servidor (lectura) | **NO** | Campo función / fórmula: el getter ignora cualquier valor asignado. |

## Trazabilidad Origen spec → V/R/U → ubicación

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Correo-001 | VAL-Correo-005 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Main@Correo-Local-validateSave-action` |
| V-Correo-002 | VAL-Correo-006 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-003 | VAL-Correo-007 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-004 | VAL-Correo-008 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-005 | VAL-Correo-009 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-006 | VAL-Correo-010 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Correo-007 | VAL-Correo-011 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-008 | VAL-Correo-017 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-009 | VAL-Correo-012 | `CorreoServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Correo-010 | VAL-Correo-018 | `CorreoServiceImpl.validateDatosDelCanal` |
| V-Correo-011 | VAL-Correo-019 | `CorreoServiceImpl.validateDatosDelCanal` |

### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Correo-001 | RN-Adjunto-001, RN-Adjunto-003 | `CorreoServiceImpl.fireActionRule_CopiarAdjuntos` (gancho `aplicarReglasDelCanalAntesDeGuardar`) | Antes de `repository.save` |

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `CorreoServiceImpl` (antiguo) | `validateEnviarCorreo(Long)` (privado, siempre vacío) | Sin regla: `enviar` no recibe datos del cliente (R-Notificacion-003) |

## Notas y supuestos

  - `CorreoServiceImpl` pasa a inyectar `Provider<MailSender>` (como ya hacía SMS con `SmsSender`), para que una instalación sin servidor de correo deje el correo FALLIDO (RN-Correo-003) en lugar de fallar al crear el servicio.
- **Mensajes «de su canal»**: RES-Notificacion-007 tiene textos distintos por canal («El historial de estado indicado no existe» / «El estado del expediente indicado no existe») aunque el campo ahora es común y se titula «Estado del expediente»; se respeta la spec.
- **Duplicados de direcciones** (V-Correo-010): se comparan tras `trim` y en minúsculas (una misma dirección escrita con otra capitalización es la misma).
- **`listarCorreosEnFail`** no tiene uso en producción; se conserva porque lo piden las guías.

> **Nota del descomponedor (decisión ante ambigüedad):** el Paso 1 del diseño borra `subsystem/correos/**`, `subsystem/sms/**` y sus tests (tareas 05–07) antes de esta tarea. Donde el diseño dice que algo «se conserva», «se mueve» o tiene «el mismo cuerpo» que en `correos`/`sms`, el código original se lee del último commit con `git show HEAD:<ruta>` (p.ej. `git show HEAD:src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`). Solo sirve de referencia: **MUST NOT** restaurar esos ficheros.
