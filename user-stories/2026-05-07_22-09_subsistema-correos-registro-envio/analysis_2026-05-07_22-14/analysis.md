---
type: analysis
---

## Análisis Funcional: Correos

**Tipo:** subsistema
**Capa:** `subsystem/correos`
**Descripción:** Registra y envía correos electrónicos, manteniendo historial completo con estado de envío, adjuntos y trazabilidad hacia expedientes y usuarios del sistema.

---

### Entidades

**`Correo`**
| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `para` | String | Sí | Dirección email del destinatario |
| `asunto` | String | Sí | Asunto del correo |
| `cuerpoHtml` | String (large) | No* | Cuerpo en HTML |
| `cuerpoTexto` | String (large) | No* | Cuerpo en texto plano |
| `estado` | Enum | Sí | `PENDIENTE`, `ENVIADO`, `ERROR` |
| `fechaEnvio` | DateTime | No | Fecha del último intento de envío |
| `errorMensaje` | String | No | Descripción del error si `estado = ERROR` |
| `dniDestinatario` | String | No | DNI del destinatario (texto libre, sin FK) |
| `expedienteId` | Long | No | ID del expediente asociado (sin FK, sin integridad referencial) |
| `usuario` | many-to-one `User` | No | Usuario del sistema al que pertenece este correo (para "mis correos") |
| `centro` | many-to-one `Centro` | Sí | Centro al que pertenece (multicentro) |
| `adjuntos` | many-to-many `MetaFile` | No | Ficheros adjuntos del correo |

_(*) Al menos uno de los dos cuerpos debe estar informado._

El campo `de` (remitente) no se almacena en BD — se toma de la configuración SMTP en tiempo de envío.

---

### Dependencias de otros subsistemas

- `subsystem/common` — entidad `Centro` para multicentro

---

### Operaciones

- **`enviar(CorreoInsertDTO)`**: crea el registro en estado `PENDIENTE`, intenta el envío SMTP. Si tiene éxito → `ENVIADO` + `fechaEnvio = ahora`. Si falla → `ERROR` + `errorMensaje`. Llamado **exclusivamente por código** desde otros subsistemas/sistemas; no tiene UI para crearlo manualmente.

- **`reenviar(correoId)`**: reintenta el envío de un correo en estado `ERROR`. Si tiene éxito → `ENVIADO` + `fechaEnvio = ahora`. Si falla → `errorMensaje` actualizado (estado sigue `ERROR`). Disparado por botón en el formulario de la UI.

---

### Vistas

- **Listado de correos** (grid, solo lectura): columnas `para`, `asunto`, `estado`, `fechaEnvio`, `dniDestinatario`, `centro`. Filtros por `estado` y `fechaEnvio`. Sin botón de crear.
- **Formulario de correo** (solo lectura): todos los campos + panel de adjuntos inline + botón "Reintentar envío" visible únicamente cuando `estado = ERROR`.

---

### Menús

- Administración → Correos enviados → abre el listado

---

### Seguridad

- **Administrador**: ve todos los correos (sin filtro de centro ni de usuario)
- **Supervisor, JefeEstudios, Director, Secretario, Administrativo**: ven correos de su centro → `self.centro IN (centros del usuario actual)`
- **Resto de usuarios**: ven solo sus propios correos → `self.usuario = currentUser`
- Multicentro: sí

---

### Validaciones

#### Operación `enviar` (llamada por código, sin UI)

No hay validaciones de cliente (no hay UI para crearlos).

**Validaciones de servidor:**

- `para` — obligatorio.
  Mensaje: `"El campo 'para' es obligatorio."`
- `para` — formato email válido (contiene `@` y dominio).
  Mensaje: `"El email '{para}' no tiene un formato válido."`
- `asunto` — obligatorio.
  Mensaje: `"El campo 'asunto' es obligatorio."`
- `cuerpoHtml` y `cuerpoTexto` — al menos uno debe estar informado.
  Mensaje: `"Debe informar al menos el cuerpo HTML o el cuerpo en texto plano."`
- `centroId` — obligatorio y debe existir en BD.
  Mensaje: `"El centro con ID '{centroId}' no existe."`

#### Operación `reenviar` (botón en UI)

No hay validaciones de cliente — el botón solo es visible cuando `estado = ERROR`.

**Validaciones de servidor:**

- `estado` — debe ser `ERROR`.
  Mensaje: `"Solo se puede reintentar el envío de un correo en estado ERROR. Estado actual: '{estado}'."`

---

#### Ciclo de vida (`estado`)

```
PENDIENTE → ENVIADO   (envío exitoso)
PENDIENTE → ERROR     (fallo de envío)
ERROR     → ENVIADO   (reintento exitoso)
ERROR     → ERROR     (reintento fallido, actualiza errorMensaje)
```

Campos editables por estado: la entidad es **inmutable desde la UI** en todos los estados. Solo el botón "Reintentar" actúa sobre un correo en `ERROR`.

---

### Asunciones tomadas

- El remitente (`de`) se toma siempre de `mail.smtp.user` de la configuración SMTP — no se pasa en el DTO.
- Un `Correo` = un destinatario. Si hay múltiples destinatarios, el llamante crea múltiples registros.
- Los adjuntos se pasan como lista de `MetaFile` ya existentes en BD (el llamante los crea/sube previamente).
- El `centro` se pasa explícitamente en el DTO — el subsistema de correos no lo infiere.
- No hay reintentos automáticos, solo manuales desde la UI.
- El menú "Correos enviados" se ubica bajo Administración, visible para todos los perfiles con acceso.
