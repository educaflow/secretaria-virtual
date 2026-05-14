---
type: analysis
---

## Análisis Funcional: Subsistema Correos

**Tipo:** subsistema
**Capa:** `subsystem/correos`
**Descripción:** Registro inmutable y auditable de cada correo electrónico que la aplicación envía (o intenta enviar) a una persona, con su contenido, destinatario, fecha, adjuntos y estado, para responder a reclamaciones del tipo "no me ha llegado ningún correo" y para que los propios usuarios consulten lo que se les ha enviado.

### Entidades

#### TareaCorreo
Representa un correo encolado o ya procesado por el sistema. Hay una TareaCorreo por destinatario; si un correo lógico va a varias personas, se crean varias TareaCorreo independientes.

| Campo | Tipo | Notas |
|---|---|---|
| `centro` | Referencia a Centro | Opcional. Si es nulo, el correo es "del sistema" y solo lo ve el administrador. |
| `de` | Texto (email) | Remitente. Se asigna automáticamente con la dirección SMTP global del sistema al crear el registro. |
| `destinatarioDni` | Texto | DNI/NIE del destinatario como snapshot textual. Opcional. No es FK. |
| `destinatarioEmail` | Texto (email) | Email del destinatario como snapshot. Obligatorio. |
| `destinatarioNombre` | Texto | Nombre del destinatario como snapshot. Opcional. |
| `asunto` | Texto | Obligatorio. |
| `cuerpoHtml` | Texto largo (HTML) | Obligatorio. |
| `cuerpoTextoPlano` | Texto largo | Opcional. |
| `estado` | Enumerado | `PENDIENTE`, `ENVIANDO`, `ENVIADO`, `FALLADO`. Valor inicial PENDIENTE. |
| `fechaCreacion` | Fecha y hora | Asignada por el sistema al crear. |
| `fechaUltimoIntento` | Fecha y hora | Gestionada por el sistema. Opcional. |
| `numIntentos` | Entero | Gestionado por el sistema. Inicial 0. Acumulativo. |
| `fechaEnvioOk` | Fecha y hora | Gestionada por el sistema; solo se rellena al pasar a ENVIADO. |
| `logErrores` | Texto largo | Acumulado: cada intento fallido añade una línea con fecha y error. Nunca se reinicia. |
| `historialExpediente` | Referencia a HistorialExpediente | Opcional. Paso de expediente que originó el correo. |
| `adjuntos` | Colección 1-N a AdjuntoCorreo | Opcional. |

#### AdjuntoCorreo
Hijo de TareaCorreo. Copia persistente del fichero adjunto.

| Campo | Tipo | Notas |
|---|---|---|
| `tareaCorreo` | Referencia al padre | Obligatorio. |
| `nombre` | Texto | Nombre con el que se adjunta el fichero al correo. Obligatorio. |
| `fichero` | MetaFile | Obligatorio. |

### Dependencias de otros subsistemas

- `subsystem/common` — Centro y resolución del DNI del usuario logado.
- `subsystem/expedientes` — `HistorialExpediente`, dependencia opcional.
- `base/infrastructure/mail` — envío SMTP global.

### Operaciones

- **Encolar correo desde otro subsistema** — cualquier subsistema interno puede solicitar el envío de un correo proporcionando destinatario, asunto, cuerpo, adjuntos opcionales, centro opcional y referencia opcional al paso de expediente. El sistema crea la TareaCorreo en estado PENDIENTE.
- **Enviar correo manualmente** — el administrador rellena destinatario (DNI opcional, email, nombre), asunto, cuerpo HTML, cuerpo texto plano opcional, adjuntos opcionales, centro opcional y referencia opcional a expediente. El sistema crea el registro en PENDIENTE.
- **Procesar pendientes** — un proceso periódico (intervalo configurable) selecciona los correos en PENDIENTE, los pasa a ENVIANDO, intenta el envío vía SMTP global y los marca como ENVIADO o FALLADO según el resultado, actualizando `numIntentos`, `fechaUltimoIntento` y, según el caso, `fechaEnvioOk` o `logErrores`. No hay reintentos automáticos.
- **Reintentar correo fallido** — el administrador o el supervisor del centro al que pertenece el correo solicita el reintento. El sistema devuelve el correo a PENDIENTE sin resetear `numIntentos` ni `logErrores`. El proceso periódico lo recogerá.
- **Consultar correos** — listados y ficha de detalle según el rol, en solo lectura.
- **Buscar correos de una persona del centro** — la administrativa introduce un DNI y obtiene los correos de su centro cuyo destinatario tiene ese DNI.
- **Ver gráfica de correos** — el administrador consulta una gráfica del volumen de envíos por día y estado en un rango de fechas.

### Vistas

- **Todos los correos** (administrador) — listado completo del sistema, con filtros por estado, fechas, centro, DNI, email. Detalle en solo lectura. Botón "Nuevo correo" para alta manual. Botón "Reintentar" en correos FALLADO.
- **Correos del centro** (supervisor) — listado restringido a los correos cuyo centro coincide con el centro activo del supervisor; nunca incluye correos sin centro. Detalle en solo lectura. Botón "Reintentar" en FALLADO.
- **Buscar correos de persona** (administrativa) — listado restringido al centro activo, con filtro obligatorio por DNI del destinatario. Sin DNI no se muestran resultados. Detalle en solo lectura. Sin reintento ni alta.
- **Mis correos** (profesor, alumno, exprofesor, exalumno, familiar, externo) — listado de correos cuyo DNI de destinatario coincide con el del usuario logado. Si el usuario no tiene DNI, la lista está vacía. Detalle en solo lectura. Sin reintento ni alta.
- **Gráfica de correos** (administrador) — gráfica de barras apiladas; eje X = día, eje Y = número de correos, barras apiladas por estado (ENVIADO / FALLADO). Filtros obligatorios: fecha desde, fecha hasta.

### Menús

- **Correos → Todos los correos** → vista "Todos los correos" (administrador).
- **Correos → Correos del centro** → vista "Correos del centro" (supervisor).
- **Correos → Buscar correos de persona** → vista "Buscar correos de persona" (administrativa).
- **Correos → Gráfica de correos** → vista "Gráfica de correos" (administrador).
- **Carpeta ciudadana → Mis correos** → vista "Mis correos" (profesor, alumno, exprofesor, exalumno, familiar, externo).

### Seguridad

Multicentro: **sí**.

- **Administrador:** ve todos los correos del sistema, incluidos los sin centro. Es el único que puede dar de alta correos manualmente. Puede reintentar cualquier correo FALLADO. Accede a la gráfica.
- **Supervisor:** ve los correos cuyo centro coincide con su centro activo; nunca ve correos sin centro ni de otros centros. Puede reintentar correos FALLADO de su centro. No crea correos.
- **Administrativa:** ve únicamente los correos de su centro activo filtrando obligatoriamente por DNI de una persona. No puede crear, modificar, borrar ni reintentar correos.
- **Profesor / Alumno / Exprofesor / Exalumno / Familiar / Externo:** ven exclusivamente los correos cuyo DNI de destinatario coincide con el suyo. No crean, no modifican, no borran, no reintentan. Si no tienen DNI registrado, no ven nada.
- Ningún rol puede modificar el contenido de un correo ya creado.
- Ningún rol puede borrar correos ni adjuntos.

### Validaciones (`V-XXX`)

| ID | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario |
|---|---|---|---|---|---|
| V-001 | `asunto` | Obligatorio | Modelo | Al crear una TareaCorreo, si `asunto` está vacío o solo contiene espacios. | "El asunto del correo es obligatorio." |
| V-002 | `cuerpoHtml` | Obligatorio | Modelo | Al crear una TareaCorreo, si `cuerpoHtml` está vacío. | "El cuerpo HTML del correo es obligatorio." |
| V-003 | `destinatarioEmail` | Obligatorio | Modelo | Al crear una TareaCorreo, si `destinatarioEmail` está vacío. | "El email del destinatario es obligatorio." |
| V-004 | `destinatarioEmail` | Formato | Negocio | Al crear una TareaCorreo, si el valor no cumple el formato estándar de correo electrónico. | "El email del destinatario '{destinatarioEmail}' no tiene un formato válido." |
| V-005 | `destinatarioDni` | Formato | Negocio | Al crear una TareaCorreo, si se aporta DNI y no cumple el formato válido de DNI/NIE español. | "El DNI del destinatario '{destinatarioDni}' no es un DNI o NIE español válido." |
| V-006 | `de` | Obligatorio | Modelo | Al crear una TareaCorreo, si tras la asignación automática el remitente queda vacío. | "No se ha podido determinar la dirección remitente del sistema." |
| V-007 | Todos los campos de contenido (`asunto`, `cuerpoHtml`, `cuerpoTextoPlano`, `destinatarioDni`, `destinatarioEmail`, `destinatarioNombre`, `de`, `centro`, `historialExpediente`, `adjuntos`, `fechaCreacion`) | Inmutabilidad | Negocio | Al actualizar una TareaCorreo existente, si alguno de estos campos cambia respecto al valor almacenado. | "No se puede modificar el contenido de un correo ya registrado." |
| V-008 | Registro completo | Borrado prohibido | Negocio | Al intentar borrar una TareaCorreo. | "No se puede borrar un correo: el registro de correos es permanente." |
| V-009 | Registro completo (AdjuntoCorreo) | Inmutabilidad | Negocio | Al intentar añadir, modificar o borrar un AdjuntoCorreo de una TareaCorreo ya existente. | "No se pueden modificar ni borrar los adjuntos de un correo ya registrado." |
| V-010 | `nombre` (AdjuntoCorreo) | Obligatorio | Modelo | Al añadir un AdjuntoCorreo. | "El nombre del adjunto es obligatorio." |
| V-011 | `fichero` (AdjuntoCorreo) | Obligatorio | Modelo | Al añadir un AdjuntoCorreo. | "El fichero del adjunto es obligatorio." |
| V-012 | `estado` | Transición de estado | Negocio | Al reintentar manualmente, si el estado actual no es `FALLADO`. | "Solo se pueden reintentar correos en estado FALLADO. Estado actual: '{estado}'. Estados válidos para reintento: FALLADO." |
| V-013 | `estado` | Transición de estado | Negocio | Al actualizar el estado, si la transición no es una de las permitidas en la máquina de estados. | "Transición de estado no permitida: '{estadoAnterior}' → '{estadoNuevo}'. Transiciones válidas: PENDIENTE→ENVIANDO, ENVIANDO→ENVIADO, ENVIANDO→FALLADO, FALLADO→PENDIENTE." |
| V-014 | `destinatarioDni` (filtro búsqueda) | Obligatorio | Negocio | En la vista "Buscar correos de persona", al ejecutar búsqueda sin DNI informado. | "Debe indicar el DNI de la persona cuyos correos desea consultar." |
| V-015 | `destinatarioDni` (filtro búsqueda) | Formato | Negocio | En la vista "Buscar correos de persona", si el DNI introducido no cumple formato DNI/NIE. | "El DNI '{dni}' no tiene un formato válido de DNI o NIE español." |
| V-016 | `fechaDesde`, `fechaHasta` (gráfica) | Obligatorio | Negocio | En la vista "Gráfica de correos", si falta alguna de las dos fechas. | "Debe indicar fecha desde y fecha hasta para consultar la gráfica." |
| V-017 | `fechaDesde`, `fechaHasta` (gráfica) | Rango | Negocio (asumida)* | En la vista "Gráfica de correos", si `fechaDesde` es posterior a `fechaHasta`. | "La fecha desde '{fechaDesde}' no puede ser posterior a la fecha hasta '{fechaHasta}'." |
| V-018 | Operación de reintento | Autorización | Negocio | Si el supervisor intenta reintentar un correo cuyo centro no es el suyo o un correo sin centro. | "No puede reintentar correos de otro centro ni correos del sistema." |

### Reglas de negocio (`R-XXX`)

| ID | Descripción | Entidad | Operación | Momento | Origen | Más información |
|---|---|---|---|---|---|---|
| R-001 | Asignar el remitente con la dirección SMTP global del sistema. | TareaCorreo | Crear | Antes | Negocio | El usuario nunca lo introduce, ni siquiera el administrador. |
| R-002 | Inicializar `estado = PENDIENTE`, `numIntentos = 0`, `fechaCreacion = ahora`, `logErrores` vacío, `fechaUltimoIntento` y `fechaEnvioOk` nulos. | TareaCorreo | Crear | Antes | Negocio | Valores de arranque del ciclo de vida. |
| R-003 | Guardar los datos del destinatario (DNI, email, nombre) como copia textual y no actualizarlos posteriormente aunque la persona original cambie. | TareaCorreo | Crear | Antes | Negocio | Snapshot sin FK; garantiza fidelidad histórica. |
| R-004 | Periódicamente, seleccionar los correos en estado PENDIENTE y pasarlos a ENVIANDO antes de intentar el envío. | TareaCorreo | Procesar pendientes | Antes | Negocio | Intervalo configurable `correos.scheduler.intervaloMinutos`, valor por defecto propuesto 5 minutos. |
| R-005 | Tras un intento de envío correcto, pasar el correo a ENVIADO, fijar `fechaEnvioOk = ahora`, actualizar `fechaUltimoIntento` e incrementar `numIntentos`. | TareaCorreo | Procesar pendientes | Después | Negocio | — |
| R-006 | Tras un intento de envío fallido, pasar el correo a FALLADO, actualizar `fechaUltimoIntento`, incrementar `numIntentos` y añadir al final de `logErrores` una línea con la fecha y el mensaje de error. | TareaCorreo | Procesar pendientes | Después | Negocio | El log es acumulativo y nunca se reinicia. No hay reintentos automáticos. |
| R-007 | Al reintentar manualmente, cambiar el estado a PENDIENTE sin modificar `numIntentos` ni `logErrores`. | TareaCorreo | Reintentar | Antes | Negocio | El proceso periódico recogerá el correo de nuevo. |
| R-008 | Realizar el envío real utilizando el contenido HTML, el texto plano si existe y los adjuntos registrados, tal cual figuran en el registro. | TareaCorreo | Procesar pendientes | Después | Negocio | El contenido enviado debe ser idéntico al registrado. |
| R-009 | Guardar una copia propia del fichero de cada adjunto en el momento de crear el correo. | AdjuntoCorreo | Crear | Antes | Negocio | Garantiza la inmutabilidad del contenido enviado aunque el fichero original cambie o desaparezca. |
| R-010 | Excluir los correos sin centro de las vistas filtradas por centro ("Correos del centro" y "Buscar correos de persona"). | TareaCorreo | Listar | Antes | Negocio | Los correos del sistema solo los ve el administrador (más, en su caso, el destinatario en "Mis correos"). |

### Reglas de UI (`U-XXX`)

| ID | Disparador | Efecto | Campo/Panel afectado | Condición | Origen |
|---|---|---|---|---|---|
| U-001 | continuo | Solo lectura | Todos los campos de contenido (`asunto`, `cuerpoHtml`, `cuerpoTextoPlano`, destinatario, `de`, `centro`, `historialExpediente`, adjuntos) | El registro ya existe (no es alta nueva). | Negocio |
| U-002 | continuo | Solo lectura | Campos gestionados por el sistema (`estado`, `numIntentos`, `fechaUltimoIntento`, `fechaEnvioOk`, `logErrores`, `fechaCreacion`, `de`) | Siempre. | Negocio |
| U-003 | continuo | Ocultar | Botón "Reintentar" | El estado del correo no es FALLADO, o el usuario no tiene permiso de reintento (no es administrador ni supervisor del centro del correo). | Negocio |
| U-004 | continuo | Ocultar | Botón "Nuevo correo" | El usuario no es administrador, o la vista no es "Todos los correos". | Negocio |
| U-005 | continuo | Obligatorio | Filtro DNI en "Buscar correos de persona" | Siempre en esta vista. | Negocio |
| U-006 | continuo | Listado vacío | Resultados de "Buscar correos de persona" | DNI de filtro no informado o con formato inválido. | Negocio |
| U-007 | continuo | Obligatorios | Filtros `fechaDesde` y `fechaHasta` en "Gráfica de correos" | Siempre en esta vista. | Negocio |
| U-008 | continuo | Ocultar | Panel "Log de errores" | `logErrores` está vacío. | Negocio (asumida)* |
| U-009 | continuo | Ocultar | Panel "Expediente relacionado" | `historialExpediente` no está informado. | Negocio (asumida)* |
| U-010 | onLoad | Renderizar HTML saneado | Panel "Cuerpo del correo" | Siempre que se abre el detalle. | Negocio (asumida)* |

### Máquina de estados

Estados de `TareaCorreo.estado`: `PENDIENTE`, `ENVIANDO`, `ENVIADO`, `FALLADO`.

| Estado origen | Estado destino | Disparador |
|---|---|---|
| (alta) | PENDIENTE | Crear correo (alta manual o encolado interno). |
| PENDIENTE | ENVIANDO | El proceso periódico lo selecciona para enviar. |
| ENVIANDO | ENVIADO | El envío SMTP termina con éxito. |
| ENVIANDO | FALLADO | El envío SMTP termina con error. |
| FALLADO | PENDIENTE | Reintento manual por administrador o supervisor del centro. |

- ENVIADO es estado final: no admite más transiciones.
- FALLADO es terminal salvo por reintento manual.
- No hay paso directo PENDIENTE→ENVIADO/FALLADO sin pasar por ENVIANDO.
- No hay reintento automático.

### Campos calculados

- `fechaEnvioOk` se rellena al pasar a ENVIADO (R-005); el usuario nunca lo introduce.
- `fechaUltimoIntento` y `numIntentos` se actualizan tras cada intento (R-005, R-006).
- `logErrores` se acumula tras cada intento fallido (R-006).
- `de` se asigna al crear desde la configuración SMTP global (R-001).
- "Correo del sistema" no es un campo: es una propiedad derivada de `centro == nulo`.

### Constantes técnicas / parámetros configurables

- `correos.scheduler.intervaloMinutos` — periodicidad del proceso de envío. Valor por defecto propuesto: 5 minutos.
- `correos.scheduler.tamañoLote` — máximo de correos procesados por ciclo. Valor por defecto propuesto: 50.

### Asunciones a confirmar

- **A1\* (V-004)**: el formato de email se valida con un patrón estándar de correo, sin verificar que el buzón realmente exista.
- **A2\* (V-017)**: cuando `fechaDesde > fechaHasta` en la gráfica, el sistema bloquea la consulta en lugar de invertir las fechas automáticamente.
- **A3\* (U-008)**: el panel "Log de errores" solo se muestra cuando hay errores acumulados.
- **A4\* (U-009)**: el panel "Expediente relacionado" solo se muestra si el correo está vinculado a un paso de expediente.
- **A5\* (U-010)**: el cuerpo HTML se renderiza saneado al mostrarlo en el detalle (sin ejecutar scripts) para evitar XSS desde el log.
- **A6**: parámetros `correos.scheduler.intervaloMinutos` (5) y `correos.scheduler.tamañoLote` (50) como configuración global, con esos valores por defecto.
- **A7**: el "centro activo" del supervisor y de la administrativa está disponible en el contexto del usuario logado (mismo mecanismo que en los demás subsistemas multicentro).
- **A8**: un correo enviado sin centro pero con DNI del destinatario sí aparece en "Mis correos" del destinatario; nunca aparece en "Correos del centro" ni en "Buscar correos de persona", aunque el DNI coincida.
- **A9**: el rol "Externo" se comporta en "Mis correos" igual que profesor/alumno/familiar (filtrado por su DNI).
- **A10**: el "Familiar" ve solo los correos cuyo DNI de destinatario coincide con el suyo propio, **no** los del alumno tutelado.
- **A11**: no hay límite específico de tamaño ni número de adjuntos; se confía en los límites genéricos de MetaFile y SMTP.
- **A12**: la gráfica agrega cada TareaCorreo por su `fechaCreacion` (los estados PENDIENTE y ENVIANDO no se grafican, solo ENVIADO y FALLADO).
