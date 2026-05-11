---
type: analysis
---

## Análisis Funcional: Correos

**Tipo:** subsistema
**Capa:** subsystem/correos
**Descripción:** Registro persistente de correos electrónicos enviados por la aplicación a destinatarios externos. Otros sistemas crean registros `Correo` por API interna, el servicio envía el mensaje de forma síncrona en el momento de inserción y persiste el resultado. Proporciona vistas de consulta para administradores, supervisores y carpeta ciudadana.

---

### Entidades

#### `Correo`

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| `asunto` | String | Sí | Asunto del correo |
| `emailDestinatario` | String | Sí | Dirección email del destinatario |
| `dniDestinatario` | String | No | DNI en texto libre; para filtrado en carpeta ciudadana |
| `cuerpoHtml` | String (largo) | Sí | Cuerpo en HTML |
| `cuerpoTexto` | String (largo) | No | Versión en texto plano (alternativa al HTML) |
| `fechaEnvio` | DateTime | Auto | Lo fija el servicio al intentar el envío |
| `estado` | Enum | Auto | `ENVIADO` / `ERROR_ENVIO`; lo fija el servicio |
| `mensajeError` | String (largo) | Auto | Error técnico si el envío falla; lo fija el servicio |
| `usuario` | FK → User (Axelor) | Sí | Usuario autenticado que disparó el envío |
| `centro` | FK → Centro | Sí | Centro al que pertenece el correo (multicentro) |
| `tramite` | FK → Tramite | No | Trámite relacionado (opcional); ver A-1 |
| `adjuntos` | Many-to-Many → MetaFile | No | Archivos adjuntos |

---

### Dependencias de otros subsistemas

- `subsystem/common` — FK a `Centro` (multicentro)
- `subsystem/expedientes` — FK opcional a `Tramite` (**ver asunción A-1**)
- `base/infrastructure/mail` — uso de `MailSender` para el envío real

---

### Operaciones

**OP-1 — `insert(CorreoInsertDTO)`**
- Lo llaman otros sistemas por código Java (no hay vista de creación en UI).
- Flujo: validar → construir `Mail` → llamar `MailSender.send()` → persistir con `estado = ENVIADO` o `estado = ERROR_ENVIO` según resultado. **No lanza excepción al llamador** por fallo de envío (persiste el error para reintento manual).

**OP-2 — `reenviar(correoId)`**
- Acción de controlador. Solo disponible para Admin y Supervisor desde el formulario.
- Precondición: `estado = ERROR_ENVIO` (V-005).
- Si tiene éxito: `estado = ENVIADO`, limpia `mensajeError`. Si falla: actualiza `mensajeError`, permanece en `ERROR_ENVIO`.

---

### Vistas

- **`Correo@Main-grid`** — Admin/Supervisor. Columnas: asunto, emailDestinatario, fechaEnvio, estado, centro, usuario. Ordenado por fechaEnvio desc.
- **`Correo@Main-form`** — Admin/Supervisor. Solo lectura. Muestra todos los campos. Panel de adjuntos (solo descarga). Botón "Reenviar" visible solo si `estado = ERROR_ENVIO`. Campo `mensajeError` visible solo si `estado = ERROR_ENVIO`.
- **`Correo@View-grid`** — Carpeta ciudadana. Filtrado a los correos del usuario en sesión. Sin botón de reenvío.
- **`Correo@View-form`** — Carpeta ciudadana. Solo lectura. No muestra mensajeError, usuario, centro, tramite.

---

### Menús

- **Administración → Notificaciones → Correos enviados** → `Correo@Main-grid` (Admin, Supervisor)
- **Carpeta ciudadana → Mis correos** → `Correo@View-grid` (todos)

---

### Seguridad

| Rol | Permisos | Condición JPQL |
|---|---|---|
| Admin | Lectura total + reenvío | Sin restricción |
| Supervisor | Lectura de su centro + reenvío | `self.centro = :userCentro` |
| Cualquier usuario | Solo lectura de sus correos | `self.emailDestinatario = :currentUserEmail OR self.dniDestinatario = :currentUserDni` |
| Ningún rol | Creación / edición / borrado por UI | No existe vista de creación ni edición |

---

### Validaciones

Las reglas V-001 a V-004 son invariantes de servicio (modelo sin UI, mensajes técnicos para el desarrollador). V-005 corresponde a la acción de reenvío con UI.

| ID | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje |
|---|---|---|---|---|---|
| V-001 | `asunto` | Obligatoriedad | Modelo | Al insertar (OP-1) | `[Correo] El asunto es obligatorio.` |
| V-002 | `emailDestinatario` | Obligatoriedad | Modelo | Al insertar (OP-1) | `[Correo] El emailDestinatario es obligatorio.` |
| V-003 | `emailDestinatario` | Formato email | Catálogo | Al insertar (OP-1) | `[Correo] El emailDestinatario '{valor}' no tiene formato válido (usuario@dominio.com).` |
| V-004 | `cuerpoHtml` | Obligatoriedad | Modelo | Al insertar (OP-1) | `[Correo] El cuerpoHtml es obligatorio.` |
| V-005 | `estado` | Estado requerido | Negocio (asumida)* | Al ejecutar reenvío (OP-2) | "Solo se pueden reenviar correos con error de envío. El estado actual del correo es {estado}." |

---

### Máquina de estados

| Estado | Descripción | ¿Final? |
|---|---|---|
| `ENVIADO` | Correo enviado correctamente | Sí |
| `ERROR_ENVIO` | Envío fallido | No (reintentable) |

| Origen | Destino | Condición | Actor | Acción posterior |
|---|---|---|---|---|
| _(inserción)_ | `ENVIADO` | send() exitoso | Servicio | `fechaEnvio = ahora()` |
| _(inserción)_ | `ERROR_ENVIO` | send() lanza excepción | Servicio | `fechaEnvio = ahora()`, `mensajeError = exc.getMessage()` |
| `ERROR_ENVIO` | `ENVIADO` | Reenvío exitoso | Admin / Supervisor | `fechaEnvio = ahora()`, limpia `mensajeError` |
| `ERROR_ENVIO` | `ERROR_ENVIO` | Reenvío fallido | Admin / Supervisor | Actualiza `mensajeError` |
| `ENVIADO` | — | Intentar reenvío | Admin / Supervisor | **Rechazado por V-005** |

---

### Asunciones a confirmar

| ID | Asunción |
|---|---|
| A-1 | La FK de `Correo` a `Tramite` puede crear dependencia circular si `subsystem/expedientes` también usa `CorreoService`. **Alternativa:** guardar solo `tramiteId: Long` en lugar de FK real. Requiere decisión antes del diseño. |
| A-2* | (V-005) Se asume que un correo `ENVIADO` no puede reenviarse (evitar duplicados al destinatario). Confirmar si algún caso de negocio justifica reenviar un correo ya entregado. |
