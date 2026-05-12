---
type: analysis
---

## Análisis Funcional: TareaCorreo (Subsistema correos)

**Tipo:** subsistema
**Capa:** subsystem/correos
**Descripción:** Subsistema que registra de forma inmutable cada correo electrónico enviado por la plataforma, permitiendo auditar envíos, gestionar fallos mediante reintentos y proporcionar trazabilidad por tipo de usuario y centro.

---

### Entidades

#### TareaCorreo

Registro histórico inmutable de un correo enviado o en proceso de envío. Una vez creado, su contenido (asunto, cuerpos, destinatario, adjuntos, centro, expediente) no puede modificarse ni borrarse. El sistema actualiza exclusivamente: `estado`, `de`, `enviadoEn`, `ultimoFalloEn`, `numIntentos`.

| Campo | Tipo XML | Restricciones | Descripción |
|---|---|---|---|
| `centro` | `many-to-one` → `Centro` | required | Centro educativo al que pertenece el correo (pivot de acceso multicentro) |
| `para` | `string` | required | Dirección de email del destinatario tal como se envió |
| `de` | `string` | opcional | Dirección de email remitente (asignada automáticamente desde las credenciales SMTP) |
| `dni` | `string` | opcional | DNI del destinatario para vinculación con usuario del sistema |
| `usuario` | `many-to-one` → `User` | opcional | Usuario del sistema resuelto automáticamente desde `dni` si existe |
| `asunto` | `string` | required | Asunto del correo tal como se envió |
| `cuerpoHtml` | `string` (large) | opcional | Cuerpo HTML del correo tal como se envió |
| `cuerpoTexto` | `string` (large) | opcional | Cuerpo en texto plano del correo tal como se envió |
| `expediente` | `many-to-one` → `Expediente` | opcional | Referencia al expediente relacionado |
| `estado` | `enum` → `EstadoTareaCorreo` | required, inicial: `PENDIENTE` | Estado actual del envío |
| `creadoEn` | `datetime` | required, asignado por servicio | Fecha y hora de creación del registro |
| `enviadoEn` | `datetime` | opcional | Fecha y hora del envío exitoso; nulo si nunca tuvo éxito |
| `ultimoFalloEn` | `datetime` | opcional | Fecha y hora del último fallo; nulo si nunca ha fallado |
| `numIntentos` | `integer` | required, inicial: `0` | Número de intentos de envío realizados; se incrementa en cada intento |
| `adjuntos` | `many-to-many` → `MetaFile` | opcional | Ficheros adjuntos enviados con el correo |

#### EstadoTareaCorreo (enum)

| Valor | Descripción |
|---|---|
| `PENDIENTE` | Creada, pendiente de envío |
| `ENVIADO` | Enviado correctamente (estado terminal) |
| `ERROR` | El último intento de envío falló; puede reintentarse |

---

### Dependencias de otros subsistemas

| Dependencia | Motivo |
|---|---|
| `subsystem/common` — `Centro`, `CentroUsuario` | Campo `centro` en `TareaCorreo`; resolución del centro activo del usuario para filtros de seguridad |
| `base/infrastructure/mail` — `Mail`, `MailSender`, `Attach`, `SmtpCredentialSimplePassword` | Envío real del correo SMTP y obtención del remitente |
| `com.axelor.auth.db.User` | Campo `usuario`; campo `dni` para resolución automática; `centroActivo` para filtros |
| `com.axelor.meta.db.MetaFile` | Almacenamiento de adjuntos |
| `subsystem/expedientes` — `Expediente` | Referencia opcional al expediente relacionado |

---

### Operaciones

| Operación | Quién la ejecuta | Descripción funcional |
|---|---|---|
| **Enviar correo (desde otro sistema)** | Cualquier sistema o subsistema | Solicitar el envío de un correo indicando destinatario (email), asunto, cuerpo (HTML y/o texto), adjuntos opcionales, expediente opcional y centro. El sistema crea un registro inmutable de la tarea, intenta el envío y refleja el resultado en el estado del registro. |
| **Enviar correo (desde la interfaz)** | Administrador | Equivalente al anterior, iniciado desde un formulario en la aplicación. Solo el administrador puede usarlo. El centro se asigna automáticamente al centro activo del usuario. |
| **Reenviar correo** | Administrador, Supervisor, Administrativa | Volver a intentar el envío de un correo que está en estado ERROR. Incrementa el contador de intentos. Si el correo no está en estado ERROR, la operación se rechaza con un mensaje al usuario. |
| **Consultar correos** | Cada rol según su filtro de visibilidad | Listar y abrir el detalle de los correos visibles según el rol (todos, del centro o propios). |
| **Visualizar gráfica de envíos** | Administrador | Ver el volumen de correos creados agrupado por día en un rango de fechas. |

Adicionalmente, el sistema realiza de forma automática (sin intervención del usuario) las siguientes transiciones de estado del correo en cada intento de envío: marca el resultado como `ENVIADO` si tiene éxito o como `ERROR` si falla, actualiza las fechas correspondientes e incrementa el contador de intentos.

---

### Vistas

| Vista | Quién la ve | Filtro de datos | Modo | Descripción |
|---|---|---|---|---|
| `TareaCorreo@All-action` | admins | Sin filtro | Solo lectura + creación + reenvío | Lista todos los correos del sistema. Permite al admin crear nuevos correos (formulario editable para nuevos registros). |
| `TareaCorreo@Centro-action` | center-admins | `self.centro = :centroActivo` | Solo lectura + reenvío | Lista correos del centro activo del usuario. Supervisor y Administrativa tienen exactamente la misma vista. |
| `TareaCorreo@Propios-action` | users | `self.usuario = :user` | Solo lectura | Lista los correos recibidos por el usuario logado. No muestra campos internos de control ni botón de reenvío. |
| `TareaCorreo@Grafica-action` | admins | Rango de fechas (desde/hasta sobre `creadoEn`) | Solo visualización | Gráfico de barras con número de `TareaCorreo` creadas por día. |

**Formulario de detalle** (`@Main-form`):
- Panel "Datos del correo" (readonly): `para`, `de`, `dni`, `usuario`, `asunto`, `centro`, `creadoEn`, `expediente`
- Panel "Cuerpo" (readonly): `cuerpoHtml` (widget html), `cuerpoTexto`
- Panel "Adjuntos" (readonly): grid de `MetaFile`
- Panel "Control de envío" (readonly, solo admins y center-admins): `estado`, `enviadoEn`, `ultimoFalloEn`, `numIntentos`
- Botón "Reenviar": visible solo cuando `estado == ERROR` y usuario en grupo admins o center-admins
- Botón "Volver": siempre visible en modo detalle

**Formulario de creación** (solo para admins, en el mismo `@Main-form`):
- Campos editables al crear: `para`, `asunto`, `cuerpoHtml`, `cuerpoTexto`, `expediente`, adjuntos
- `centro` se asigna automáticamente desde `centroActivo` del usuario
- Al guardar: el servicio persiste el registro y lanza el envío; el registro pasa a ser inmutable

---

### Menús

| Ítem | Ruta | Acción | Grupo | Nota |
|---|---|---|---|---|
| Todos los correos | Notificaciones > Correos | `subsysCorreos.TareaCorreo@All-action` | admins | Existente (actualizar acción) |
| Correos del centro | Notificaciones > Correos | `subsysCorreos.TareaCorreo@Centro-action` | center-admins | Existente (actualizar acción) |
| Mis correos | Notificaciones > Correos | `subsysCorreos.TareaCorreo@Propios-action` | users | Existente (actualizar acción) |
| Gráfica de correos | Notificaciones > Correos | `subsysCorreos.TareaCorreo@Grafica-action` | admins | **Nuevo** |

---

### Seguridad

**Multicentro:** sí. El campo `centro` de `TareaCorreo` es el eje de control de acceso.

| Grupo / Rol | Regla JPQL | Puede crear | Puede reenviar | Puede ver |
|---|---|---|---|---|
| `admins` (Administrador) | Sin filtro | Sí (desde UI) | Sí | Todo el contenido |
| `center-admins` (Supervisor, Administrativa) | `self.centro = :centroActivo` | No | Sí | Todo el contenido de su centro |
| `users` (Profesor, Alumno, Familiar, Ex*) | `self.usuario = :user` | No | No | Solo asunto, cuerpo, adjuntos, estado, fechas; no ve `numIntentos`, `ultimoFalloEn`, `de` ni datos de control interno |

---

### Validaciones

| ID | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario |
|---|---|---|---|---|---|
| V-001 | `centro` | Requerido | Modelo | Al crear: `centro` es nulo | El centro es obligatorio |
| V-002 | `para` | Requerido | Modelo | Al crear: `para` es nulo o vacío | La dirección de correo del destinatario es obligatoria |
| V-003 | `para` | Formato email | Catálogo | Al crear: `para` no cumple formato email RFC 5321 | La dirección "{para}" no tiene un formato de correo electrónico válido |
| V-004 | `asunto` | Requerido | Modelo | Al crear: `asunto` es nulo o vacío | El asunto es obligatorio |
| V-005 | `cuerpoHtml`, `cuerpoTexto` | Al menos uno requerido | Negocio (asumida)* | Al crear: ambos son nulos o vacíos | El correo debe tener contenido: indica el cuerpo HTML o el texto plano |
| V-006 | `dni` | Formato DNI | Catálogo | Al crear: `dni` no es nulo y no cumple formato DNI español (8 dígitos + letra) o NIE (letra + 7 dígitos + letra) | El DNI "{dni}" no tiene un formato válido |
| V-007 | `estado` | Requerido | Modelo | Invariante del servicio: `estado` es nulo al persistir | Invariante violado: el campo estado es obligatorio en TareaCorreo |
| V-008 | `creadoEn` | Requerido | Modelo | Invariante del servicio: `creadoEn` es nulo al persistir | Invariante violado: el campo creadoEn es obligatorio en TareaCorreo |
| V-009 | `numIntentos` | Requerido | Modelo | Invariante del servicio: `numIntentos` es nulo al persistir | Invariante violado: el campo numIntentos es obligatorio en TareaCorreo |
| V-010 | `estado` | Transición: reenvío | Negocio (asumida)* | Al invocar reenviar: estado ≠ ERROR | Solo se puede reenviar un correo en estado ERROR. El estado actual es "{estado}"; los valores válidos son: ERROR |
| V-011 | `estado` | Transición: terminal | Negocio (asumida)* | Al intentar cambiar el estado de un registro con estado ENVIADO | Un correo ya enviado no puede cambiar de estado. El estado actual es "ENVIADO" |
| V-012 | (registro completo) | Inmutabilidad de contenido | Negocio (asumida)* | Al intentar modificar: `para`, `de`, `dni`, `asunto`, `cuerpoHtml`, `cuerpoTexto`, `centro`, `expediente`, `creadoEn` o adjuntos en un registro ya creado | Los registros de correo son históricos y no pueden modificarse una vez creados |
| V-013 | (registro completo) | Borrado prohibido | Negocio (asumida)* | Al intentar borrar cualquier `TareaCorreo` | Los registros de correo son históricos y no pueden eliminarse |
| V-014 | `TareaCorreo` (en `Centro`) | Integridad referencial al borrar | Negocio (asumida)* | Al intentar borrar un `Centro` que tiene `TareaCorreo` asociadas | No se puede eliminar el centro porque tiene correos registrados asociados |

**Notas sobre V-007, V-008, V-009:** Son invariantes del servicio. El servicio los asigna automáticamente en la creación; no hay UI que los solicite al usuario. Los mensajes son técnicos para el desarrollador.

**Nota sobre V-014:** La integridad referencial se documenta en el padre (`Centro`). Como `Centro` pertenece a `subsystem/common`, esta restricción debe coordinarse con ese subsistema.

---

### Máquina de estados

```
[Creación por servicio]
        │
        ▼
   PENDIENTE
        │
        ├── Envío en background exitoso ──► ENVIADO (terminal)
        │
        └── Envío en background fallido ──► ERROR
                                               │
                                        Reenvío (admin/supervisor/administrativa)
                                               │
                                               ▼
                                           PENDIENTE → (se repite el ciclo)
```

**Transiciones válidas:**

| Desde | Hacia | Disparador |
|---|---|---|
| `PENDIENTE` | `ENVIADO` | Hilo background: `MailSender.send()` exitoso |
| `PENDIENTE` | `ERROR` | Hilo background: excepción en `MailSender.send()` |
| `ERROR` | `PENDIENTE` | Usuario autorizado solicita reenvío |

Cada vez que se intenta un envío (inicial o reintento), `numIntentos` se incrementa y `ultimoFalloEn` se actualiza si falla.

---

### Campos calculados

| Campo | Resolución | Cuándo |
|---|---|---|
| `usuario` | El servicio busca en `User` por email igual a `para` (o por `dni` si se proporciona). Si encuentra coincidencia, asigna la referencia; si no, queda nulo. | En la creación del registro, antes de persistir |
| `de` | Asignado automáticamente desde `SmtpCredentialSimplePassword.userName()` | En la creación del registro |

Ningún campo se calcula en tiempo de consulta. Todas las actualizaciones posteriores (`estado`, `enviadoEn`, `ultimoFalloEn`, `numIntentos`) las realiza el servicio en el hilo background.

---

### Asunciones a confirmar

- **V-005\*** — Se asume que un correo sin ningún cuerpo (ni HTML ni texto plano) es inválido. Si el negocio acepta correos de solo asunto (p.ej., para adjuntos), eliminar esta regla.
- **V-010\*** — Se asume que solo se puede reenviar desde estado ERROR. Si se quisiera permitir "forzar reenvío" desde PENDIENTE (correos atascados), revisar esta restricción.
- **V-011\*** — Se asume que ENVIADO es un estado terminal absoluto. No se contempla marcar un correo enviado como error retroactivamente.
- **V-012\*** — La inmutabilidad cubre todos los campos de contenido. Solo el sistema puede actualizar los campos de control (`estado`, `enviadoEn`, `ultimoFalloEn`, `numIntentos`, `de`). Esta distinción debe implementarse en el servicio, no solo en la UI.
- **V-013\*** — El borrado está prohibido para todos los roles, incluidos los administradores. Si se necesitara un mecanismo de limpieza administrativa, revisar con una operación de purga controlada.
- **V-014\*** — Se asume que eliminar un Centro con correos registrados debe estar restringido. Dado que Centro pertenece a `subsystem/common`, esta restricción debe coordinarse con ese subsistema.
- **Resolución de `usuario`\*** — Se asume que la resolución del usuario se hace por email (`para`) en primer lugar, y si se proporciona `dni`, se usa ese como criterio alternativo. Si el mismo DNI tiene varios emails en el sistema, se toma el primero encontrado.
- **Acceso `users` a correos con `usuario` nulo** — Si el destinatario no tiene cuenta en el sistema, `usuario` queda nulo y esa persona no puede ver el correo en "Mis correos" hasta que se registre con el mismo email o DNI. Se asume como comportamiento correcto.
