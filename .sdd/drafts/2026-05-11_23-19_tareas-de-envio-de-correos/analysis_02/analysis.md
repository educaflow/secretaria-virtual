---
type: analysis
---

## Análisis Funcional: Tareas de envío de correos

**Tipo:** subsistema
**Capa:** subsystem/correos
**Descripción:** Registro inmutable de todos los correos electrónicos enviados desde la aplicación, con envío asíncrono en segundo plano, reintento manual sobre fallos, visibilidad segmentada por rol y centro, y gráfica de actividad para administración.

### Entidades

- **`TareaCorreo`** — Registro de un correo enviado o pendiente de envío a un único destinatario. Inmutable salvo en los campos del ciclo de envío.

  **Identificación y contexto:**
  - `fechaCreacion` — fecha-hora, requerido. Instante de creación del registro.
  - `centro` — referencia M2O a `com.educaflow.subsystem.common.db.Centro`, opcional. Centro al que pertenece el correo; `null` para correos del sistema.
  - `expediente` — referencia M2O a `com.educaflow.subsystem.expedientes.db.Expediente`, opcional.

  **Remitente (snapshot):**
  - `de` — texto, requerido. Dirección remitente, copiada del parámetro global vigente en el momento de la creación.

  **Destinatario (snapshot):**
  - `dniDestinatario` — texto, requerido. DNI/NIE del destinatario. Se almacena por DNI porque el destinatario puede no existir aún como usuario del sistema.
  - `emailDestinatario` — texto, requerido. Email del destinatario en el momento de la creación.

  **Contenido (snapshot inmutable):**
  - `asunto` — texto, requerido.
  - `cuerpoHtml` — texto largo, requerido. Cuerpo en formato HTML; no hay versión texto plano.
  - `adjuntos` — colección M2M a `com.axelor.meta.db.MetaFile`. Cada elemento es una **copia inmutable** del MetaFile original aportado al crear el correo, de modo que un cambio o borrado del original no afecta al adjunto del correo.

  **Estado y reintentos:**
  - `estado` — dominio finito requerido: `PENDIENTE`, `ENVIADO`, `FALLIDO`. Valor inicial siempre `PENDIENTE`.
  - `fechaEnvioExitoso` — fecha-hora, opcional. Se rellena cuando el correo pasa a `ENVIADO` por primera vez.
  - `fechaUltimoIntento` — fecha-hora, opcional. Instante del último intento (exitoso o fallido).
  - `numeroIntentos` — entero, requerido, valor inicial 0. Contador acumulado de intentos.
  - `mensajeUltimoError` — texto largo, opcional. Descripción del último error de envío.

### Dependencias de otros subsistemas

- `subsystem/common` — `com.educaflow.subsystem.common.db.Centro` (referencia opcional); `com.axelor.auth.db.User` (para resolver DNI y centro activo del usuario logado en filtros de visibilidad).
- `subsystem/expedientes` — `com.educaflow.subsystem.expedientes.db.Expediente` (referencia opcional + panel embebido en su ficha).
- `base/infrastructure/mail` — `Mail`, `Attach`, `MailSender` para el envío SMTP real.
- `base/util/DniUtil` — validación de DNI/NIE.
- `base/util/MetaFileUtil` — lectura y duplicación de contenido de MetaFile.
- Configuración global de la aplicación — parámetros `mail.smtp.host`, `mail.smtp.user` (de éste último se copia `de`), `mail.smtp.password`.

No hay dependencias circulares: `correos` depende de `common` y `expedientes`, y ninguno depende de `correos`.

### Operaciones

- **Crear correo desde código** — Cualquier subsistema solicita el alta de una TareaCorreo aportando destinatario (DNI + email), asunto, cuerpo HTML, adjuntos opcionales, expediente opcional y centro opcional. El sistema fija estado inicial `PENDIENTE`, registra fecha de creación, captura el remitente del parámetro global como snapshot, duplica los adjuntos para garantizar inmutabilidad, y dispara el envío en segundo plano sin bloquear al solicitante.
- **Crear correo manualmente (Administrador)** — Desde la vista "Todos los correos" el administrador abre un formulario y rellena destinatario (DNI + email), asunto, cuerpo HTML, adjuntos opcionales, expediente opcional y centro opcional. Al guardar, el correo queda en `PENDIENTE` y sigue el mismo flujo que cualquier otro.
- **Envío en segundo plano** — Tras crearse una TareaCorreo en `PENDIENTE`, el sistema intenta el envío SMTP de forma asíncrona. Cada intento incrementa `numeroIntentos` y actualiza `fechaUltimoIntento`; si el envío tiene éxito el correo pasa a `ENVIADO` y se registra `fechaEnvioExitoso`; si falla pasa a `FALLIDO` y se registra `mensajeUltimoError`.
- **Reintentar correo fallido** — Sobre un correo en estado `FALLIDO`, un administrador o supervisor del centro puede ejecutar "Reintentar". El estado vuelve a `PENDIENTE` durante el reintento y termina en `ENVIADO` o `FALLIDO` según el resultado. No se crea un registro nuevo: se actualiza el existente.
- **Consultar correos según rol** — Cada usuario lista y abre en solo lectura los correos a los que tiene acceso (ver Seguridad), con filtros propios de la grid (fecha, asunto, estado, destinatario).
- **Ver gráfica de correos (Administrador)** — Visualización del número de correos por día entre dos fechas, agrupados por estado (`PENDIENTE`/`ENVIADO`/`FALLIDO`).
- **Ver correos asociados a un expediente** — Desde la ficha del Expediente, listado en solo lectura de los correos cuyo campo `expediente` apunta a ese expediente.
- **Descargar adjuntos** — Desde el detalle de un correo accesible, descarga de cualquiera de las copias inmutables almacenadas como adjuntos.

### Vistas

- **Mis correos** — Listado y detalle solo lectura de los correos cuyo `dniDestinatario` coincide con el DNI del usuario logado. Lo ven profesores, alumnos, exprofesores, exalumnos, familiares y externos. Sin acciones sobre el registro.
- **Correos del centro** — Listado y detalle solo lectura de los correos cuyo `centro` coincide con el centro activo del usuario logado. Lo ven los cargos del centro (supervisor, administrativa, etc.). La administrativa aplica manualmente un filtro adicional por DNI o destinatario en la propia grid cuando atiende una consulta. El detalle ofrece "Reintentar" únicamente si el estado es `FALLIDO` y solo al supervisor.
- **Todos los correos** — Listado y detalle solo lectura de todos los correos del sistema, sin filtro de centro ni destinatario. Lo ve únicamente el administrador. Incluye una acción "Nuevo correo" en la grid que abre el formulario de creación manual. El detalle ofrece "Reintentar" si el estado es `FALLIDO`.
- **Formulario "Nuevo correo"** — Formulario de creación manual accesible solo al administrador desde "Todos los correos". Campos editables solo durante la creación: DNI destinatario, email destinatario, asunto, cuerpo HTML, adjuntos, expediente (opcional), centro (opcional). Tras guardar, el registro queda en `PENDIENTE` y se vuelve solo lectura.
- **Detalle de TareaCorreo** — Ficha en solo lectura con todos los campos del correo: remitente, destinatario (DNI y email), centro, expediente, asunto, cuerpo HTML renderizado, adjuntos descargables, estado, fechas (creación, último intento, envío exitoso), número de intentos y mensaje del último error. La presencia del botón "Reintentar" depende del rol y del estado.
- **Gráfica de correos** — Visualización del número de correos por día entre dos fechas, agrupada por estado. Filtros: fecha desde y fecha hasta. Solo la ve el administrador.
- **Panel "Correos enviados" en ficha de Expediente** — Listado embebido en solo lectura de los correos cuyo `expediente` apunta al expediente actual.

### Menús

- **Notificaciones → Correos → Mis correos** → vista "Mis correos" (visible para profesores, alumnos, exprofesores, exalumnos, familiares, externos).
- **Notificaciones → Correos → Correos del centro** → vista "Correos del centro" (visible para cargos del centro: supervisor, administrativa, etc.).
- **Notificaciones → Correos → Todos los correos** → vista "Todos los correos" (visible solo para administrador).
- **Notificaciones → Correos → Gráfica de correos** → vista "Gráfica de correos" (visible solo para administrador).

### Seguridad

- **Administrador**: ve todos los correos del sistema sin filtro de centro. Puede crear correos manualmente. Puede reintentar correos en estado `FALLIDO`. Ve la gráfica.
- **Supervisor del centro**: ve los correos cuyo centro coincide con su centro activo. Puede reintentar correos en estado `FALLIDO` de su centro. No puede crear correos manualmente. No ve la gráfica.
- **Administrativa (y demás cargos del centro)**: ve los correos cuyo centro coincide con su centro activo. Filtra manualmente por persona en la grid. No puede reintentar, ni crear manualmente, ni ver la gráfica.
- **Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo**: ven exclusivamente los correos cuyo `dniDestinatario` coincide con su propio DNI. No pueden crear, reintentar ni ver la gráfica.
- **Modificación y borrado**: nadie (incluido el administrador) puede modificar el contenido de un correo creado ni borrarlo. Las únicas modificaciones permitidas son las transiciones de estado y campos del ciclo de envío descritas en la máquina de estados.
- **Correos sin centro** (centro = null): solo visibles para el administrador y para el destinatario por DNI; supervisores y administrativas no los ven aunque coincidan por DNI.
- **Multicentro**: sí. Supervisor y administrativa están acotados por `centroActivo` del usuario. Administrador no está acotado por centro. Usuarios finales se acotan por DNI, no por centro.

### Validaciones

| ID | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario |
|----|----------|------|--------|--------------------------|---------------------|
| V-001 | `dniDestinatario` | Obligatorio | Modelo | Siempre al crear. | "El DNI del destinatario es obligatorio." |
| V-002 | `dniDestinatario` | Formato | Catálogo (DniUtil) | Siempre al crear. Debe ser un DNI o NIE español con letra de control válida. | "El DNI del destinatario '{valor}' no es un DNI o NIE español válido." |
| V-003 | `emailDestinatario` | Obligatorio | Modelo | Siempre al crear. | "El email del destinatario es obligatorio." |
| V-004 | `emailDestinatario` | Formato | Catálogo (regex estándar) | Siempre al crear. Debe cumplir el patrón `usuario@dominio`. | "El email del destinatario '{valor}' no tiene un formato válido." |
| V-005 | `de` | Obligatorio | Modelo | Siempre al crear. Se rellena automáticamente desde el parámetro global `mail.smtp.user`; si la configuración no está presente, no se permite crear correos. | "La dirección del remitente es obligatoria. No se ha podido obtener de la configuración del sistema." |
| V-006 | `asunto` | Obligatorio | Modelo | Siempre al crear. | "El asunto del correo es obligatorio." |
| V-007 | `cuerpoHtml` | Obligatorio | Modelo | Siempre al crear. | "El cuerpo del correo es obligatorio." |
| V-008 | `fechaCreacion` | Obligatorio | Modelo | Siempre al crear. Se rellena automáticamente con la fecha-hora actual; no editable por el usuario. | "La fecha de creación del correo es obligatoria." |
| V-009 | `estado` | Obligatorio + Dominio | Modelo | Siempre. Valores válidos: `PENDIENTE`, `ENVIADO`, `FALLIDO`. | "El estado '{valor}' no es válido. Los valores válidos son: PENDIENTE, ENVIADO, FALLIDO." |
| V-010 | `estado` | Valor inicial | Negocio (asumida)* | Al crear un registro nuevo (desde código o manual). El estado inicial debe ser `PENDIENTE`. | "El estado inicial de un correo solo puede ser PENDIENTE. Estado recibido: '{valor}'." |
| V-011 | `estado` | Transición | Negocio (asumida)* | Solo se permiten las transiciones: `PENDIENTE → ENVIADO`, `PENDIENTE → FALLIDO`, `FALLIDO → PENDIENTE` (vía Reintentar). Desde `ENVIADO` no se permite ninguna transición. | "No se puede cambiar el estado del correo de '{estadoActual}' a '{estadoNuevo}'. Transiciones permitidas: PENDIENTE→ENVIADO, PENDIENTE→FALLIDO, FALLIDO→PENDIENTE." |
| V-012 | `numeroIntentos` | Rango | Negocio (asumida)* | Siempre. Debe ser un entero ≥ 0. Se inicializa a 0 al crear y solo lo incrementa el sistema en cada intento de envío. | "El número de intentos '{valor}' no es válido. Debe ser un entero mayor o igual que 0." |
| V-013 | `fechaEnvioExitoso` | Coherencia con estado | Negocio (asumida)* | Solo puede tener valor cuando `estado = ENVIADO`. Debe ser nula en `PENDIENTE` y `FALLIDO`. | "La fecha de envío exitoso solo puede estar informada cuando el correo está en estado ENVIADO." |
| V-014 | `fechaUltimoIntento`, `numeroIntentos` | Coherencia entre campos | Negocio (asumida)* | Si `numeroIntentos > 0`, `fechaUltimoIntento` debe estar informada. Si `numeroIntentos = 0`, `fechaUltimoIntento` debe ser nula. | "El número de intentos y la fecha del último intento no son coherentes entre sí." |
| V-015 | `mensajeUltimoError` | Coherencia con histórico | Negocio (asumida)* | Solo puede tener valor si en algún intento previo se produjo un fallo (es decir, si `numeroIntentos > 0` y alguno acabó en `FALLIDO`). Se sobrescribe en cada nuevo intento fallido y se conserva tras un eventual `ENVIADO` posterior como histórico del último fallo. | "El mensaje del último error solo puede informarse cuando se ha producido al menos un intento fallido." |
| V-016 | Registro completo (todos los campos salvo los del ciclo de envío: `estado`, `numeroIntentos`, `fechaUltimoIntento`, `fechaEnvioExitoso`, `mensajeUltimoError`) | Inmutabilidad | Negocio (asumida)* | Tras la creación, ningún campo distinto de los gestionados por la máquina de envío puede modificarse, ni siquiera por el administrador. | "El campo '{campo}' no puede modificarse una vez creado el correo." |
| V-017 | Registro completo | No borrable | Negocio (asumida)* | Siempre. Nadie puede borrar un correo registrado. | "Los correos no pueden borrarse. Una vez registrado, un correo queda como histórico permanente." |
| V-018 | Acción "Reintentar" | Precondición de estado | Negocio (asumida)* | Solo se permite ejecutar "Reintentar" sobre correos en estado `FALLIDO`. | "Solo se pueden reintentar correos en estado FALLIDO. El correo está actualmente en estado '{estadoActual}'." |
| V-019 | Acción "Reintentar" | Autorización | Negocio (asumida)* | Solo el administrador y el supervisor del centro al que pertenece el correo pueden ejecutar "Reintentar". | "No tiene permiso para reintentar el envío de correos." |
| V-020 | Acción "Nuevo correo" | Autorización | Negocio (asumida)* | Solo el administrador puede crear correos manualmente. | "Solo el administrador puede crear correos manualmente." |
| V-021 | Visibilidad de registro | Autorización de lectura | Negocio (asumida)* | Cada usuario solo puede consultar los correos que su rol le permite ver: administrador todos; supervisor y administrativa los de su centro activo; usuarios finales aquellos cuyo `dniDestinatario` coincida con su DNI. | "No tiene permiso para ver este correo." |
| V-022 | Acción "Ver gráfica" | Autorización | Negocio (asumida)* | Solo el administrador puede acceder a la gráfica. | "Solo el administrador puede ver la gráfica de correos." |
| V-023 | Gráfica: rango de fechas | Obligatorios + coherencia | Negocio (asumida)* | Tanto "fecha desde" como "fecha hasta" son obligatorias. La fecha "desde" no puede ser posterior a la fecha "hasta". | "Debe indicar fecha desde y fecha hasta. La fecha desde '{desde}' no puede ser posterior a la fecha hasta '{hasta}'." |
| V-024 | `expediente` | Integridad referencial al borrar | Negocio (asumida)* | Documentada en el padre `Expediente`: al intentar borrar un Expediente con correos asociados se impide el borrado (RESTRICT). | "No se puede eliminar el expediente '{valor}' porque tiene correos asociados." |
| V-025 | `centro` | Integridad referencial al borrar | Negocio (asumida)* | Documentada en el padre `Centro`: al intentar borrar un Centro con correos asociados se impide el borrado (RESTRICT). | "No se puede eliminar el centro '{valor}' porque tiene correos asociados." |

**Reglas que NO se documentan** (las cubre el framework):
- Parseo de tipos (fechas, enteros, enumerados).
- Existencia del registro referenciado en `expediente`, `centro` y `adjuntos`.
- Longitud máxima estándar de columnas de tipo texto del ORM.

**Decisiones explícitas de NO validar:**
- No se valida que el DNI exista como usuario del sistema (el destinatario puede no estar dado de alta).
- No se valida límite máximo de adjuntos ni tamaño total (ver A2).
- No se valida número máximo de intentos: el reintento es manual y no hay reintento automático (ver A6).
- No se sanitiza el HTML del cuerpo (ver A11).

### Máquina de estados

**Estados:**
- `PENDIENTE` — estado inicial. El correo está registrado y a la espera de un intento de envío (o vuelve aquí temporalmente durante un reintento).
- `ENVIADO` — estado terminal exitoso. El correo se entregó al servidor SMTP correctamente.
- `FALLIDO` — estado terminal reversible. El último intento de envío falló; solo el administrador o supervisor pueden reactivarlo vía "Reintentar".

**Transiciones permitidas:**

| Origen | Destino | Disparador | Condición | Acción asociada |
|--------|---------|------------|-----------|------------------|
| (nuevo) | `PENDIENTE` | Creación (desde código o manual) | Validaciones V-001..V-009 cumplidas. | Fija `fechaCreacion`, `numeroIntentos=0`, `de` (snapshot), `dniDestinatario`/`emailDestinatario` (snapshot); duplica adjuntos como copias propias. |
| `PENDIENTE` | `ENVIADO` | Intento de envío exitoso | Envío SMTP terminado sin error. | Incrementa `numeroIntentos`, actualiza `fechaUltimoIntento`, fija `fechaEnvioExitoso`. |
| `PENDIENTE` | `FALLIDO` | Intento de envío fallido | Envío SMTP terminado con error. | Incrementa `numeroIntentos`, actualiza `fechaUltimoIntento`, sobrescribe `mensajeUltimoError`. |
| `FALLIDO` | `PENDIENTE` | Acción "Reintentar" | V-018 (estado=FALLIDO) y V-019 (rol Admin/Supervisor) cumplidas. | Cambia estado a `PENDIENTE` y dispara nuevo envío. |

**Transiciones inválidas** (todas amparadas por V-011):
- `ENVIADO → cualquier estado`.
- `FALLIDO → ENVIADO` directo sin pasar por reintento.
- `FALLIDO → FALLIDO` o `PENDIENTE → PENDIENTE` (sin intento intermedio).

**Editabilidad de campos por estado** (E = editable por usuario, R = solo lectura, N = no aplica, Auto = lo gestiona el sistema):

| Campo                | Alta manual (admin) | PENDIENTE  | ENVIADO       | FALLIDO               |
|----------------------|---------------------|------------|---------------|-----------------------|
| `dniDestinatario`    | E                   | R          | R             | R                     |
| `emailDestinatario`  | E                   | R          | R             | R                     |
| `de`                 | Auto                | R          | R             | R                     |
| `asunto`             | E                   | R          | R             | R                     |
| `cuerpoHtml`         | E                   | R          | R             | R                     |
| `adjuntos`           | E                   | R          | R             | R                     |
| `expediente`         | E                   | R          | R             | R                     |
| `centro`             | E                   | R          | R             | R                     |
| `fechaCreacion`      | Auto                | R          | R             | R                     |
| `estado`             | Auto (=PENDIENTE)   | Auto       | R             | Auto (vía Reintentar) |
| `numeroIntentos`     | Auto (=0)           | Auto       | Auto          | Auto                  |
| `fechaUltimoIntento` | N                   | Auto       | Auto          | Auto                  |
| `fechaEnvioExitoso`  | N                   | N          | Auto          | N                     |
| `mensajeUltimoError` | N                   | N          | R (histórico) | Auto                  |

La columna "Alta manual (admin)" se refiere exclusivamente al momento de la creación en el formulario "Nuevo correo"; tras guardar, todos los campos pasan a su estado correspondiente.

### Campos calculados

No hay campos cuyo valor se derive de una fórmula recalculable. Todos los snapshots (`de`, `dniDestinatario`, `emailDestinatario`) se fijan una sola vez en la creación; los campos del ciclo de envío (`estado`, `numeroIntentos`, `fechaUltimoIntento`, `fechaEnvioExitoso`, `mensajeUltimoError`) los actualiza el motor de envío como parte de la máquina de estados. Los filtros de visibilidad ("Mis correos", "Correos del centro") son condiciones evaluadas en cada consulta, no campos persistidos.

### Asunciones a confirmar

- **A1\*** — Si la aplicación se reinicia mientras hay correos en estado `PENDIENTE`, esos correos quedan en `PENDIENTE` indefinidamente y solo se recuperarán si un usuario autorizado los gestiona manualmente. No hay scheduler ni proceso de arranque que retome los pendientes automáticamente.
- **A2\*** — No se establece un límite máximo de adjuntos por correo ni de tamaño total. Si en el futuro se decidiera fijar uno, sería un parámetro configurable (propuesta: `correos.max.adjuntos.total.mb`, valor por defecto 25 MB).
- **A3\*** — El remitente `de` es único y global para toda la aplicación, leído del parámetro `mail.smtp.user`. No hay remitente por centro ni por usuario.
- **A4\*** — Las copias de adjuntos (`MetaFile` duplicados) se conservan indefinidamente; no hay política de retención ni purga.
- **A5\*** — El "centro" de un correo creado desde código lo decide quien invoca la operación (puede ser `null` para correos del sistema); no se calcula automáticamente a partir del DNI del destinatario ni del expediente asociado.
- **A6\*** — El reintento es estrictamente manual. No hay reintento automático tras fallo, ni número máximo de reintentos. El contador `numeroIntentos` puede crecer indefinidamente mientras el correo siga reactivándose.
- **A7\*** — La gráfica cuenta los correos por `fechaCreacion`, no por `fechaEnvioExitoso`; la pregunta de negocio es "cuántos correos se han generado al día", no "cuántos se han entregado al día".
- **A8\*** — La gráfica agrupa por **día natural** y por **estado actual** del correo (no por estado en cada día). Incluye los correos sin centro.
- **A9\*** — Para los usuarios finales, la regla de visibilidad cruza el DNI del usuario logado con `dniDestinatario`. Si un usuario no tiene DNI registrado en su cuenta, no ve ningún correo aunque coincidan otros atributos. Si un mismo DNI pertenece a varios roles (p. ej. exalumno y familiar), el usuario verá sus correos desde cualquiera de los menús a los que tenga acceso.
- **A10\*** — Los correos sin centro (`centro = null`, correos del sistema) solo son visibles para el administrador y para el destinatario por DNI; supervisores y administrativas no los ven aunque coincidan por DNI, salvo que el destinatario sea el propio supervisor o administrativa actuando como usuario final.
- **A11\*** — El `cuerpoHtml` se almacena tal cual sin sanitización adicional. Si en el futuro se requiriese sanitización HTML, sería una decisión aparte sin impacto en este análisis.
- **A12\*** — El panel "Correos enviados" en la ficha de un Expediente respeta la seguridad por rol del usuario logado: un alumno solo ve allí los correos cuyo `dniDestinatario` sea el suyo; un supervisor/administrativa los del centro; un administrador todos.
- **A13\*** — El rol "Externo" se trata como usuario final con visibilidad por DNI. Si en el futuro hubiera externos sin DNI registrado, no verán ningún correo.
- **A14\*** — El envío manual del administrador permite asociar opcionalmente un `expediente` y un `centro` sin forzar coherencia entre ellos (no se valida que el expediente pertenezca al centro indicado).