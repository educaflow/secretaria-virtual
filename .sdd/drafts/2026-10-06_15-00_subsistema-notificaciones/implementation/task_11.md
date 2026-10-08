---
type: implementation-task
template: system
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

## Servicio del canal SMS (SmsService + SmsServiceImpl)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/service/SmsService.java` | Crear | k-sistemas (servicios.md) | Servicio del canal SMS |
| `subsystem/notificaciones/service/impl/SmsServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Canal SMS (sustituye a `sms/service/impl/SmsServiceImpl`) |

### Paso 3 — Interfaces de servicio y base abstracta del canal (extracto)

**`SmsService extends NotificacionCanalService<Sms>`**: sin métodos propios.

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

Regla compleja citada: `design/rules/R-Notificacion-003.md` (el gancho `enviarPorCanal` participa en el envío).

## Frontera de confianza — AllowProperties por acción


### `SmsServiceImpl.reenviar` (invocado desde `SmsController.reenviar`) — heredado de `NotificacionCanalServiceImpl`

Entidad: `Sms`. **Forma elegida**: `createDenyAllProperties`. **Origen spec:** ídem. Misma tabla que el correo.

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

**`SmsServiceImpl.allowPropertiesInsert`**: comunes + `telefono`, `mensaje` (cliente; `telefono` normalizado por R-Sms-001); el resto igual que el correo.

**`NotificacionServiceImpl.allowPropertiesInsert/Update/Remove`** y **`*.allowPropertiesUpdate/Remove`** de los canales y del adjunto: `createDenyAllProperties` (inmutables, sin borrado; alta genérica imposible).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Sms-001 | VAL-Sms-005 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Main@Sms-Local-validateSave-action` |
| V-Sms-002 | VAL-Sms-006 | `SmsServiceImpl.validateDatosDelCanal` |
| V-Sms-003 | VAL-Sms-007 | `SmsServiceImpl.validateDatosDelCanal`; cortesía: `Local-validateSave` |
| V-Sms-004 | VAL-Sms-008 | `SmsServiceImpl.validateDatosDelCanal` |

### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Notificacion-003 | RN-Notificacion-003, RN-Notificacion-004, RN-Notificacion-005, RN-Notificacion-006, RN-Correo-001, RN-Correo-003, RN-Correo-004, RN-Sms-002 | `NotificacionCanalServiceImpl.enviar` + `fireActionRule_RegistrarIntentoEnvio/MarcarEnvioCorrecto/MarcarEnvioFallido` + `enviarPorCanal` de cada canal | Transacción propia. Detalle: `design/rules/R-Notificacion-003.md` |
| R-Sms-001 | RN-Sms-001, RES-Sms-002 | `SmsServiceImpl.fireActionRule_NormalizarTelefono` (gancho) | Antes de `repository.save` |

## Notas y supuestos (las que aplican a esta tarea)

- **Mensajes «de su canal»**: RES-Notificacion-007 tiene textos distintos por canal («El historial de estado indicado no existe» / «El estado del expediente indicado no existe») aunque el campo ahora es común y se titula «Estado del expediente»; se respeta la spec.
