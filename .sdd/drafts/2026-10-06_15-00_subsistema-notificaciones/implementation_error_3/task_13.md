---
type: implementation-task
template: system
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

## Servicio del canal SMS (SmsService + SmsServiceImpl)

### Filas de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/service/SmsService.java` | Crear | k-sistemas (servicios.md) | Servicio del canal SMS |
| `subsystem/notificaciones/service/impl/SmsServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Canal SMS (sustituye a `sms/service/impl/SmsServiceImpl`) |

### Paso 3 (verbatim del design.md: contrato de la base y de sus ganchos)

### Paso 3 — Interfaces de servicio y base abstracta del canal

**`NotificacionCanalService`** (`com.educaflow.subsystem.notificaciones.service`):

```java
public interface NotificacionCanalService<T extends Notificacion> extends ModelService<T> {
    T reenviar(T entidad, T entidadOriginal);


    Optional<BusinessMessages> validateReenviar(T entidad, T entidadOriginal);


    AllowProperties allowPropertiesReenviar();
}
```

**`CorreoService extends NotificacionCanalService<Correo>`**: `List<Correo> listarCorreosEnFail();` ⏎⏎⏎ `Optional<BusinessMessages> validateListarCorreosEnFail();` (se conserva de `correos`, como piden las guías).
**`SmsService extends NotificacionCanalService<Sms>`**: sin métodos propios.
**`AdjuntoService extends ModelService<Adjunto>`**: sin métodos propios.

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

Verificación (al cerrar el bloque 1–9): compila; `awk` de headers de `k-sistemas/servicios.md` sobre el fichero (tres líneas iguales por header); `./gradlew -q test --tests '*ordenmetodos*'`.

### Paso 5 (verbatim del design.md)

### Paso 5 — Canal SMS

**`SmsServiceImpl extends NotificacionCanalServiceImpl<Sms> implements SmsService`**:

```java
@Inject Provider<SmsSender> smsSenderProvider;   // se resuelve dentro del envío (RN-Sms-002)

public SmsServiceImpl(Class<Sms> model, Repository<Sms> repository);

@Override protected void aplicarReglasDelCanalAntesDeGuardar(Sms sms);   // fireActionRule_NormalizarTelefono(sms)
@Override protected void enviarPorCanal(Sms sms);
//   smsSenderProvider.get().send(new com.educaflow.base.infrastructure.sms.Sms(sms.getTelefono(), sms.getMensaje()))
@Override protected TextosCanal textosDelCanal();
//   Ídem con los mensajes que la spec da para el SMS: centroAjeno = VAL-Sms-010; el resto, la versión SMS de
//   RES-Notificacion-007, RES-Notificacion-004, RES-Notificacion-005, VAL-Notificacion-001 y VAL-Notificacion-002.

@Override protected void validateDatosDelCanal(Sms sms, BusinessMessages messages);
//   Solo llama a los privados que se CONSERVAN de sms/SmsServiceImpl: validarTelefono y validarMensaje.
//   - V-Sms-001 (VAL-Sms-005) teléfono vacío; si no, V-Sms-002 (VAL-Sms-006) !new NumeroTelefono(t).esMovilDeEspana().
//   - V-Sms-003 (VAL-Sms-007) mensaje vacío o solo espacios (es además la guarda de MensajeSmsUtil, que no admite
//     blancos); si no, V-Sms-004 (VAL-Sms-008) !MensajeSmsUtil.cabeEnUnSms(mensaje).

@Override protected Map<String, Object> allowPropertiesDelCanal();   // {telefono, mensaje}

private void fireActionRule_NormalizarTelefono(Sms sms);
//   R-Sms-001 (Origen spec: RN-Sms-001, RES-Sms-002). Momento: Antes de repository.save. Incondicional:
//   telefono = new NumeroTelefono(telefono).enFormatoE164() (validateInsert ya garantizó que es un móvil válido).
```

Verificación (al cerrar el bloque 1–9): compila.

**Regla compleja referenciada:** `design/rules/R-Notificacion-003.md` (diseño detallado de `enviar`, `fireActionRule_RegistrarIntentoEnvio/MarcarEnvioCorrecto/MarcarEnvioFallido` y sus garantías). Léelo antes de implementar.

### Frontera de confianza — AllowProperties por acción (extracto verbatim)

### `CorreoServiceImpl.reenviar` (invocado desde `CorreoController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Correo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Reenviar` de `entity-Notificacion.md` (ninguna).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | — | **NO** | El reenvío actúa sobre `entidadOriginal` (BD); el estado lo cambia `enviar()` (R-Notificacion-003). |

### `SmsServiceImpl.reenviar` (invocado desde `SmsController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Sms`. **Forma elegida**: `createDenyAllProperties`. **Origen spec:** ídem. Misma tabla que el correo.

### `insert` de cada entidad (puerta REST `/ws/rest/<FQN>` y `save` de los forms)

No se invoca desde un `@CallMethod`, pero es la frontera real del alta y se documenta igual.

**`SmsServiceImpl.allowPropertiesInsert`**: comunes + `telefono`, `mensaje` (cliente; `telefono` normalizado por R-Sms-001); el resto igual que el correo.
**`NotificacionServiceImpl.allowPropertiesInsert/Update/Remove`** y **`*.allowPropertiesUpdate/Remove`** de los canales y del adjunto: `createDenyAllProperties` (inmutables, sin borrado; alta genérica imposible).

### Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

#### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Sms-001 | VAL-Sms-005 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Main@Sms-Local-validateSave-action` |
| V-Sms-002 | VAL-Sms-006 | `SmsServiceImpl.validateDatosDelCanal` |
| V-Sms-003 | VAL-Sms-007 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Sms-004 | VAL-Sms-008 | `SmsServiceImpl.validateDatosDelCanal` |

#### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Sms-001 | RN-Sms-001, RES-Sms-002 | `SmsServiceImpl.fireActionRule_NormalizarTelefono` (gancho) | Antes de `repository.save` |

### Nota del design.md (`## Notas y supuestos`)

- **Mensajes «de su canal»**: RES-Notificacion-007 tiene textos distintos por canal («El historial de estado indicado no existe» / «El estado del expediente indicado no existe») aunque el campo ahora es común y se titula «Estado del expediente»; se respeta la spec.
