---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

Nota del descomponedor (decisión documentada): el diseño pide **mover/conservar** lógica de los antiguos `subsystem/correos` y `subsystem/sms`, que la Tarea 01 borra del árbol. Su código sigue disponible en el historial de Git (p. ej. `git show HEAD:src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java`); consúltalo por ahí, **solo** como origen de lo que el diseño dice que se conserva.

Regla compleja citada: `design/rules/R-Notificacion-003.md` (el gancho `enviarPorCanal` es su paso 5).

### Ficheros a crear o modificar (filas de esta tarea)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/service/CorreoService.java` | Crear | k-sistemas (servicios.md) | Servicio del canal correo |
| `subsystem/notificaciones/service/impl/CorreoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Canal correo (sustituye a `correos/service/impl/CorreoServiceImpl`) |

### Paso 3 — Interfaces de servicio y base abstracta del canal

**`CorreoService extends NotificacionCanalService<Correo>`**: `List<Correo> listarCorreosEnFail();` ⏎⏎⏎ `Optional<BusinessMessages> validateListarCorreosEnFail();` (se conserva de `correos`, como piden las guías).

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

## Frontera de confianza — AllowProperties por acción (extracto)

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

**`NotificacionServiceImpl.allowPropertiesInsert/Update/Remove`** y **`*.allowPropertiesUpdate/Remove`** de los canales y del adjunto: `createDenyAllProperties` (inmutables, sin borrado; alta genérica imposible).

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-010 | VAL-Correo-014, VAL-Sms-010 | `NotificacionCanalServiceImpl.validateInsert` (texto `centroAjeno` del canal) |
| V-Notificacion-011 | RES-Notificacion-007 | `NotificacionCanalServiceImpl.validateInsert` (texto `estadoExpedienteInexistente` del canal) |
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
| R-Notificacion-003 | RN-Notificacion-003, RN-Notificacion-004, RN-Notificacion-005, RN-Notificacion-006, RN-Correo-001, RN-Correo-003, RN-Correo-004, RN-Sms-002 | `NotificacionCanalServiceImpl.enviar` + `fireActionRule_RegistrarIntentoEnvio/MarcarEnvioCorrecto/MarcarEnvioFallido` + `enviarPorCanal` de cada canal | Transacción propia. Detalle: `design/rules/R-Notificacion-003.md` |
| R-Correo-001 | RN-Adjunto-001, RN-Adjunto-003 | `CorreoServiceImpl.fireActionRule_CopiarAdjuntos` (gancho `aplicarReglasDelCanalAntesDeGuardar`) | Antes de `repository.save` |

### Eliminaciones declaradas (fila de esta tarea)

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `CorreoServiceImpl` (antiguo) | `validateEnviarCorreo(Long)` (privado, siempre vacío) | Sin regla: `enviar` no recibe datos del cliente (R-Notificacion-003) |

### Notas y supuestos (extracto)

  - `CorreoServiceImpl` pasa a inyectar `Provider<MailSender>` (como ya hacía SMS con `SmsSender`), para que una instalación sin servidor de correo deje el correo FALLIDO (RN-Correo-003) en lugar de fallar al crear el servicio.
- **Mensajes «de su canal»**: RES-Notificacion-007 tiene textos distintos por canal («El historial de estado indicado no existe» / «El estado del expediente indicado no existe») aunque el campo ahora es común y se titula «Estado del expediente»; se respeta la spec.
- **Duplicados de direcciones** (V-Correo-010): se comparan tras `trim` y en minúsculas (una misma dirección escrita con otra capitalización es la misma).
- **`listarCorreosEnFail`** no tiene uso en producción; se conserva porque lo piden las guías.
- **Frozen store de ArchUnit**: al desaparecer `correos.service.CorreoService.enviarCorreo`, la violación congelada de C23 desaparece sola.
