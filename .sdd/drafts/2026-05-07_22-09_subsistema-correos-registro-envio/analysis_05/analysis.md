---
type: analysis
---

## Análisis Funcional: Correos

**Tipo:** subsistema
**Capa:** `subsystem/correos`
**Descripción:** Subsistema que envía correos electrónicos vía SMTP global de forma síncrona y registra cada envío con su contenido completo, destinatario, resultado, vínculos opcionales a expediente/usuario y permite reintentar manualmente los fallidos.

### Entidades

- **`Correo`** — Registro inmutable de un envío de email.
  - `centro` — many-to-one a `com.educaflow.subsystem.common.db.Centro` — **requerido**. Lo aporta el llamador.
  - `expediente` — many-to-one a `com.educaflow.subsystem.expedientes.db.Expediente` — opcional.
  - `email` — string (255) — **requerido**. Email del único destinatario.
  - `dniDestinatario` — string (16) — opcional. DNI/NIE del destinatario.
  - `usuarioDestinatario` — many-to-one a `com.axelor.auth.db.User` — opcional. Calculado en el servicio al crear, buscando `auth.User` por `dniDestinatario`.
  - `asunto` — string (255) — **requerido**.
  - `htmlBody` — text — opcional.
  - `textBody` — text — opcional. Al menos uno de `{htmlBody, textBody}`.
  - `adjuntos` — one-to-many a `com.axelor.meta.db.MetaFile` — opcional.
  - `estado` — enum `EstadoCorreo` ∈ {`ENVIADO`, `FALLIDO`} — **requerido** (asignado por el servicio).
  - `fechaUltimoIntento` — datetime — **requerido** (asignado por el servicio).
  - `numIntentos` — integer — **requerido**, valor inicial 1.
  - `mensajeError` — text — opcional, presente solo si `estado = FALLIDO`.

### Dependencias de otros subsistemas

- `subsystem/common` — FK obligatoria a `Centro` (multicentro).
- `subsystem/expedientes` — FK opcional a `Expediente` (decisión explícita del usuario, asume el acoplamiento).
- `base/infrastructure/mail` — reutiliza `Mail`, `MailSender`, `MailSenderImpl`, `Attach` para el envío SMTP.
- `com.axelor.auth.db.User` — FK opcional al usuario destinatario.
- `com.axelor.meta.db.MetaFile` — adjuntos.

### Operaciones (servicio `CorreoService`)

- **`enviarCorreo(Centro centro, Expediente expediente, String email, String dniDestinatario, String asunto, String htmlBody, String textBody, List<MetaFile> adjuntos): Correo`**
  1. Aplica V-001..V-006 sobre los argumentos.
  2. Resuelve `usuarioDestinatario` consultando `auth.User` por `dniDestinatario` (vía repositorio personalizado). Si no existe, queda `null`.
  3. Construye `Mail` con `from = mail.smtp.from` (constante SMTP global), `to = [email]`, asunto, cuerpos y adjuntos convertidos a `Attach`.
  4. Invoca `MailSender.send(mail)` síncronamente.
  5. **Si éxito:** persiste `Correo` con `estado = ENVIADO`, `fechaUltimoIntento = now()`, `numIntentos = 1`, `mensajeError = null`.
  6. **Si SMTP falla:** captura la excepción, persiste `Correo` con `estado = FALLIDO`, `fechaUltimoIntento = now()`, `numIntentos = 1`, `mensajeError = <mensaje humano de la excepción, sin stacktrace>`. **NO hace rollback.**
  7. Devuelve el `Correo` persistido.

- **`reenviarCorreo(Long correoId): Correo`**
  1. Carga el `Correo` por id.
  2. Aplica V-007 (estado debe ser `FALLIDO`).
  3. Reconstruye `Mail` con los datos persistidos (asunto, cuerpos, email, adjuntos, `from` actual de configuración).
  4. Invoca `MailSender.send(mail)` síncronamente.
  5. Incrementa `numIntentos += 1` y actualiza `fechaUltimoIntento = now()`.
  6. Si éxito: `estado = ENVIADO`, `mensajeError = null`. Si fallo: `estado` permanece `FALLIDO`, `mensajeError` se actualiza. Sin rollback.
  7. Devuelve el `Correo` actualizado.

### Vistas

- **`correos.Correo@Main-grid`** — Listado para Administrador/Supervisor. Columnas: `fechaUltimoIntento`, `email`, `dniDestinatario`, `asunto`, `estado`, `centro`, `expediente`, `numIntentos`. Solo lectura.
- **`correos.Correo@Main-form`** — Formulario para Administrador/Supervisor. Solo lectura sobre todos los campos. Paneles: *Destinatario* (`email`, `dniDestinatario`, `usuarioDestinatario`), *Contenido* (`asunto`, `htmlBody`, `textBody`, `adjuntos`), *Contexto* (`centro`, `expediente`), *Envío* (`estado`, `fechaUltimoIntento`, `numIntentos`, `mensajeError`). Botón **"Reenviar"** visible solo si `estado = FALLIDO` y el usuario tiene permiso.
- **`correos.Correo@CarpetaCiudadana-grid`** — Listado en Carpeta Ciudadana, solo lectura. Filtrado por `dniDestinatario = DNI del usuario actual`. Sin botón "Reenviar".
- **`correos.Correo@CarpetaCiudadana-form`** — Formulario en Carpeta Ciudadana, solo lectura. Sin botón "Reenviar".

### Menús

- `Administración → Correos` → `action-correos.Correo-all` (admin/supervisor). Filtrado por centro automático para Supervisor.
- `Carpeta Ciudadana → Mis correos` → `action-correos.Correo-carpetaCiudadana` (filtrado por DNI del usuario).

### Seguridad

- **Multicentro:** sí. El campo `centro` es obligatorio y filtra todas las vistas no-administrador.
- **Administrador:** ve, crea manualmente, borra y reenvía correos de cualquier centro.
- **Supervisor:** ve y reenvía correos de su centro. **NO crea, NO borra** manualmente.
- **Profesor / Exprofesor / Alumno / Exalumno:** lectura solo de los correos donde `dniDestinatario = su DNI`, vía Carpeta Ciudadana. Sin reenviar/crear/borrar.
- **Externo / Familiar:** sin acceso al subsistema (A4*).
- La creación programática vía `CorreoService.enviarCorreo` desde otros subsistemas no está sujeta a permisos de UI.
- Las restricciones por rol se implementan con permisos Axelor (Permission/Group) + reglas JPQL del subsistema `security`; no se duplican como filas en la tabla V-XXX.

### Validaciones

| ID    | Campo(s)              | Tipo                  | Origen             | Condición de aplicación                                              | Mensaje al usuario                                                                                          |
|-------|-----------------------|-----------------------|--------------------|----------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| V-001 | `centro`              | Obligatorio           | Modelo             | Siempre                                                              | "El centro es obligatorio."                                                                                 |
| V-002 | `email`               | Obligatorio           | Modelo             | Siempre                                                              | "El email del destinatario es obligatorio."                                                                 |
| V-003 | `email`               | Formato               | Catálogo           | `email` no vacío                                                     | "El email '{email}' no tiene un formato válido."                                                            |
| V-004 | `dniDestinatario`     | Formato (DNI/NIE ES)  | Catálogo           | `dniDestinatario` no vacío                                           | "El DNI/NIE '{dni}' no es válido. Compruebe el formato y la letra de control."                              |
| V-005 | `asunto`              | Obligatorio           | Modelo             | Siempre                                                              | "El asunto es obligatorio."                                                                                 |
| V-006 | `htmlBody`, `textBody`| Cruzada (al menos uno)| Negocio            | `htmlBody` vacío y `textBody` vacío                                  | "Debe proporcionar al menos un cuerpo del mensaje (HTML o texto plano)."                                    |
| V-007 | `estado`              | Transición de estado  | Negocio            | Al invocar `reenviarCorreo` con `estado != FALLIDO`                  | "Solo se pueden reenviar correos en estado FALLIDO. El correo está en estado '{estado}'."                   |
| V-008 | `expediente`, `centro`| Coherencia            | Negocio (asumida)* | `expediente` no nulo y `expediente.centro != centro`                 | "El expediente '{expediente}' pertenece a un centro distinto del indicado en el correo."                    |
| V-009 | (cualquier campo de entrada) | Inmutabilidad  | Negocio            | Modificación post-creación de `email`, `dniDestinatario`, `usuarioDestinatario`, `asunto`, `htmlBody`, `textBody`, `adjuntos`, `centro`, `expediente` | "Los correos no son editables una vez registrados; el campo '{campo}' no se puede modificar." |

> **Notas excluidas como reglas:** existencia de FK (FW), longitud por defecto Axelor 255 (no impuesta por negocio), validación del catálogo enum (parser), `required` de `estado`/`fechaUltimoIntento`/`numIntentos` (los asigna siempre el servicio, son invariantes técnicas no expuestas al cliente). Permisos por rol → sección Seguridad. Coherencia `mensajeError ↔ estado` → invariante interna del servicio.

### Máquina de estados

**Estados:** `ENVIADO`, `FALLIDO`. La fila nace directamente en uno de los dos según el resultado del intento SMTP en `enviarCorreo` (sin estado intermedio `PENDIENTE`).

**Transiciones permitidas:**

| Desde     | Hacia    | Disparador          | Condición                          | Acción posterior                                                                          |
|-----------|----------|---------------------|------------------------------------|-------------------------------------------------------------------------------------------|
| (nuevo)   | ENVIADO  | `enviarCorreo`      | SMTP responde sin excepción        | `fechaUltimoIntento = now()`, `numIntentos = 1`, `mensajeError = null`                    |
| (nuevo)   | FALLIDO  | `enviarCorreo`      | SMTP lanza excepción               | `fechaUltimoIntento = now()`, `numIntentos = 1`, `mensajeError = <texto humano>`          |
| FALLIDO   | ENVIADO  | `reenviarCorreo`    | SMTP responde sin excepción        | `fechaUltimoIntento = now()`, `numIntentos += 1`, `mensajeError = null`                   |
| FALLIDO   | FALLIDO  | `reenviarCorreo`    | SMTP vuelve a fallar               | `fechaUltimoIntento = now()`, `numIntentos += 1`, `mensajeError = <nuevo texto>`          |

**Transiciones inválidas:**

| Desde    | Hacia    | Mensaje                                                                                  |
|----------|----------|------------------------------------------------------------------------------------------|
| ENVIADO  | *        | "No se puede reenviar un correo ya enviado." (regla V-007)                               |

**Campos editables por estado** (`E` editable, `R` readonly, `Auto` asignado por el servicio, `N` no aplica):

| Campo                  | (creación)   | ENVIADO   | FALLIDO                              |
|------------------------|--------------|-----------|--------------------------------------|
| `centro`               | E (servicio) | R         | R                                    |
| `expediente`           | E (servicio) | R         | R                                    |
| `email`                | E (servicio) | R         | R                                    |
| `dniDestinatario`      | E (servicio) | R         | R                                    |
| `usuarioDestinatario`  | Auto         | R         | R                                    |
| `asunto`               | E (servicio) | R         | R                                    |
| `htmlBody`             | E (servicio) | R         | R                                    |
| `textBody`             | E (servicio) | R         | R                                    |
| `adjuntos`             | E (servicio) | R         | R                                    |
| `estado`               | Auto         | R         | Auto (vía `reenviarCorreo`)          |
| `fechaUltimoIntento`   | Auto         | R         | Auto (vía `reenviarCorreo`)          |
| `numIntentos`          | Auto         | R         | Auto (vía `reenviarCorreo`)          |
| `mensajeError`         | Auto         | R (null)  | Auto (vía `reenviarCorreo`)          |

### Campos calculados

- **`usuarioDestinatario`** — Calculado en `enviarCorreo`: se busca un `auth.User` cuyo DNI coincida con `dniDestinatario`. Si no hay coincidencia o `dniDestinatario` es nulo, queda `null`. **No** se recalcula en `reenviarCorreo` (la asociación queda fijada al crear).
- **`estado`, `fechaUltimoIntento`, `numIntentos`, `mensajeError`** — asignados por el servicio según el resultado del intento SMTP.
- **`from` (del `Mail` enviado, no del modelo)** — leído de las propiedades estándar de Axelor (`mail.smtp.from` / equivalente). No se persiste en `Correo`.

### Invariantes técnicas del servicio (no son validaciones V-XXX)

- **I-1** — Si `MailSender.send` lanza excepción, el servicio captura la excepción, marca el correo como `FALLIDO` con `mensajeError`, y la transacción confirma. Nunca rollback.
- **I-2** — `mensajeError` se construye con el mensaje humano de la excepción (`Throwable.getMessage()` o equivalente legible), nunca con stacktrace.
- **I-3** — Las consultas JPA (resolución de User por DNI, filtrado por centro/usuario) viven en `CorreoRepository` personalizado, nunca inline en el servicio.
- **I-4** — Sin numeración correlativa de negocio: la identidad es el id de BD.
- **I-5** — Sin scheduler ni cola; envío síncrono inmediato en cada llamada.

### Asunciones a confirmar

- **A1*** — `usuarioDestinatario` se calcula automáticamente buscando `auth.User` por `dniDestinatario` solo en `enviarCorreo`; no se recalcula en `reenviarCorreo`.
- **A2*** — El `from` no se persiste en la entidad `Correo`; se lee siempre de configuración SMTP (`mail.smtp.from`). Alternativa rechazada: persistirlo para trazabilidad histórica.
- **A3*** — El filtro de Carpeta Ciudadana es por `dniDestinatario = DNI del User actual`, no por `usuarioDestinatario` (porque al crear un correo el destinatario podría no estar registrado como User aún).
- **A4*** — Tipos con acceso a Carpeta Ciudadana: Profesor, Exprofesor, Alumno, Exalumno. Externo y Familiar **sin acceso**. Confirmar.
- **A5*** — La validación de DNI/NIE incluye dígito de control (módulo 23), reutilizando `com.educaflow.base.util.DniUtil`.
- **A6*** — La validación de email aplica una regex RFC 5322 simplificada estándar (no se hace resolución MX).
- **A7*** — Coherencia `expediente.centro = centro` al crear (V-008): el Q&A no lo dijo, pero es coherente con la política multicentro.
- **A8*** — Las propiedades SMTP estándar de Axelor a usar son `mail.smtp.host`, `mail.smtp.port`, `mail.smtp.user`, `mail.smtp.password`, `mail.smtp.from`. Si no existen en `application.properties`, el envío fallará y quedará FALLIDO con mensaje de configuración.
- **A9*** — Los adjuntos se transforman a `Attach` leyendo el binario completo del `MetaFile` al construir el `Mail` (en memoria; sin streaming). Si un `MetaFile` ha sido borrado entre el envío original y un reenvío, `reenviarCorreo` falla con mensaje de adjunto perdido.
