---
type: specification
---

## Especificación funcional: Correos

**Tipo:** subsistema
**Capa funcional:** Notificaciones (Correos)
**Descripción:** Registra y envía correos electrónicos de la secretaría virtual, dejando trazabilidad inmutable de qué se envió, a quién, cuándo y con qué resultado. Cada usuario puede consultar los correos que le corresponden según su rol y centro. El envío SMTP en sí lo proporciona la infraestructura ya existente.

### Entidades

- **TareaCorreo** — Representa un correo electrónico que el sistema debe enviar o ya ha enviado a un destinatario concreto. Su contenido es inmutable una vez creada; lo único que cambia con el tiempo es su estado y los datos asociados al intento de envío. Campos funcionalmente relevantes: asunto, cuerpo, DNI del destinatario, dirección de correo del destinatario, centro al que pertenece el correo (opcional, fijado por quien lo crea), referencia opcional al historial de estado de un expediente, fecha de creación, fecha del último intento de envío, número de intentos, motivo del fallo (cuando aplica), estado, lista de adjuntos. Estados: PENDIENTE, ENVIANDO, ENVIADO, FALLADO.
- **AdjuntoCorreo** — Fichero que acompaña a una TareaCorreo. El subsistema guarda una copia propia del fichero, de modo que aunque el original cambie o se borre, lo adjuntado al correo permanece tal cual se envió. Campos funcionalmente relevantes: nombre del fichero, contenido del fichero.

### Dependencias de otros subsistemas

- **Infraestructura de correo (mail)** — Realiza el envío SMTP efectivo; el servidor, usuario y contraseña vienen de la configuración de la aplicación. Este subsistema solo lo invoca, no lo reimplementa.
- **Expedientes** — Para permitir asociar opcionalmente una TareaCorreo a un cambio de estado concreto (historial de estado) de un expediente.
- **Gestión de centro / usuarios** — Para conocer el centro del usuario conectado (filtrado de visibilidad) y para identificar el DNI del destinatario.

### Operaciones

- **Crear y enviar un correo**: el Administrador rellena asunto, cuerpo, DNI del destinatario, dirección de correo, centro opcional, referencia opcional a un historial de estado de expediente y adjuntos opcionales. La TareaCorreo queda registrada en estado PENDIENTE y el control vuelve al usuario inmediatamente, sin esperar al envío.
- **Procesar el envío en segundo plano**: el sistema toma las TareaCorreo PENDIENTE, las pasa a ENVIANDO, intenta entregarlas por la infraestructura de correo y las deja en ENVIADO o en FALLADO según el resultado, registrando el motivo del fallo cuando lo hay.
- **Reenviar un correo fallido**: el Administrador, sobre una TareaCorreo en estado FALLADO, dispara el reenvío. Se reutiliza la misma TareaCorreo: su estado vuelve a PENDIENTE para que el procesamiento asíncrono la intente de nuevo y se incrementa el número de intentos. El contenido (asunto, cuerpo, destinatario, dirección de correo, adjuntos, centro, expediente relacionado) no cambia.
- **Consultar correos**: cada usuario ve la lista de correos que le corresponden según su rol, en modo solo lectura, y puede abrir el detalle de cualquiera de ellos.
- **Consultar la gráfica de envíos**: el Administrador elige dos fechas y obtiene una serie diaria del número de correos creados en ese rango, desglosada por estado.

### Flujos principales

- F-001 — El Administrador consulta el listado completo de correos del sistema y abre el detalle de uno cualquiera para revisar su contenido, destinatario, fechas y adjuntos.
- F-002 — El Administrador crea un correo nuevo con sus adjuntos; tras la operación el correo queda registrado y aparece en el listado de todos los correos con todos sus datos.
- F-003 — El Administrador crea un correo asociado a un centro concreto, y tanto el Supervisor como la Administrativa de ese centro lo ven en el listado de correos de su centro y pueden abrir su detalle completo.
- F-004 — El Administrador crea un correo dirigido a un DNI concreto, y el usuario titular de ese DNI lo ve en su listado personal de correos y puede abrir su detalle completo.

### Pantallas

- **Todos los correos**: lista todas las TareaCorreo del sistema con asunto, DNI del destinatario, centro, fecha de creación y estado. La ve únicamente el Administrador. Permite abrir el detalle, crear un correo nuevo y reenviar uno fallido.
- **Correos del centro**: lista las TareaCorreo cuyo centro coincide con el centro del usuario que consulta. La ven Supervisor y Administrativa del centro, en modo solo lectura.
- **Mis correos**: lista las TareaCorreo cuyo DNI de destinatario coincide con el DNI del usuario que consulta. La ven Profesor, Exprofesor, Alumno, Exalumno, Familiar y Externo, en modo solo lectura.
- **Detalle de correo**: muestra el contenido completo de una TareaCorreo (asunto, cuerpo, DNI y dirección de correo del destinatario, centro, fecha de creación, fecha del último intento, número de intentos, estado, motivo del fallo si lo hay, adjuntos descargables y enlace al historial de estado del expediente cuando aplica). Modo solo lectura para todos los usuarios. El Administrador dispone aquí de la acción "Reenviar" cuando el estado es FALLADO.
- **Nuevo correo**: formulario que permite al Administrador introducir asunto, cuerpo, DNI del destinatario, dirección de correo, centro opcional, referencia opcional al historial de estado de un expediente y adjuntos.
- **Gráfica de correos enviados**: pantalla con selector de fecha inicial y fecha final y una gráfica diaria desglosada por estado (PENDIENTE, ENVIANDO, ENVIADO, FALLADO). Solo visible para el Administrador.

### Menús

- Correos → Todos los correos (Administrador)
- Correos → Correos del centro (Supervisor, Administrativa)
- Correos → Gráfica de correos enviados (Administrador)
- Carpeta ciudadana → Mis correos (Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo)

### Seguridad

- **Administrador**: puede ver todos los correos del sistema sin filtro, crear correos nuevos, reenviar correos fallidos y consultar la gráfica.
- **Supervisor del centro**: puede ver los correos cuyo centro coincide con su propio centro. No puede crear, modificar, borrar ni reenviar.
- **Administrativa**: igual que Supervisor del centro.
- **Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo**: pueden ver únicamente los correos cuyo DNI de destinatario coincide con el suyo. No pueden crear, modificar, borrar ni reenviar.
- Nadie, en ningún rol, puede modificar ni borrar una TareaCorreo ni sus adjuntos.
- Multicentro: sí. Un Supervisor o Administrativa solo ve los correos de su centro; los correos sin centro asignado los ve únicamente el Administrador (además del propio destinatario en su "Mis correos").

### Máquina de estados

- Estado inicial al crear la TareaCorreo: **PENDIENTE**.
- **PENDIENTE → ENVIANDO**: cuando el procesamiento asíncrono toma la tarea y comienza el envío.
- **ENVIANDO → ENVIADO**: cuando el envío termina con éxito.
- **ENVIANDO → FALLADO**: cuando el envío no se ha podido completar; se registra el motivo.
- **FALLADO → PENDIENTE**: cuando el Administrador solicita reenviar el correo fallido. A partir de aquí sigue el ciclo normal.
- **ENVIADO** es estado final: no admite transiciones.

### Campos calculados

- **Número de intentos**: número de veces que la TareaCorreo ha pasado por ENVIANDO. Se incrementa en cada nueva transición a ENVIANDO.
- **Fecha del último intento**: se actualiza en cada transición a ENVIANDO.
- **Series de la gráfica de correos**: para cada día del rango elegido, número de TareaCorreo creadas ese día desglosado por estado.

### Requisitos (EARS)

#### Ubicuos (E-UB)

- E-UB-001 — El sistema debe aceptar como DNI del destinatario de una TareaCorreo cualquier identificador, exista o no como usuario dado de alta.
- E-UB-002 — El sistema debe permitir crear una TareaCorreo sin centro asignado; en ese caso la TareaCorreo se considera de ámbito global y solo la ven el Administrador y el propio destinatario en "Mis correos".
- E-UB-003 — El sistema debe permitir crear una TareaCorreo sin referencia a un historial de estado de expediente.
- E-UB-004 — El sistema debe permitir crear una TareaCorreo sin adjuntos.
- E-UB-005 — El sistema debe procesar el envío SMTP de las TareaCorreo de forma asíncrona, confirmando la creación al usuario inmediatamente y entregando el correo en segundo plano.
- E-UB-006 — El sistema debe ser el único que modifica el estado de una TareaCorreo; ningún usuario puede editarlo manualmente.
- E-UB-007 — El sistema debe restringir a Supervisor y Administrativa la visibilidad de las TareaCorreo a aquellas cuyo centro coincide con su propio centro, excluyendo además las TareaCorreo sin centro asignado.
- E-UB-008 — El sistema debe restringir a Profesor, Exprofesor, Alumno, Exalumno, Familiar y Externo la visibilidad de las TareaCorreo a aquellas cuyo DNI del destinatario coincide con el suyo, sin filtro adicional por centro.
- E-UB-009 — El sistema debe permitir al Administrador ver en la pantalla "Todos los correos" la totalidad de las TareaCorreo, con y sin centro asignado.
- *E-UB-010 — La pantalla "Gráfica de correos enviados" debe exigir la fecha inicial y la fecha final como obligatorias.
- *E-UB-011 — El sistema debe permitir descargar los adjuntos de una TareaCorreo desde la pantalla Detalle en cualquier estado del correo (PENDIENTE, ENVIANDO, ENVIADO o FALLADO).

#### Dirigidos por evento (E-EV)

- E-EV-001 — Cuando se adjunta un fichero a una TareaCorreo en su creación, el sistema debe guardar una copia propia del contenido, desacoplada del fichero original.
- E-EV-002 — Cuando el Administrador reenvía una TareaCorreo en estado FALLADO, el sistema debe reutilizar la misma TareaCorreo pasándola a PENDIENTE, actualizando la fecha del último intento e incrementando el número de intentos, sin alterar el resto de su contenido.
- E-EV-003 — Cuando el envío SMTP de una TareaCorreo falla, el sistema debe pasar la tarea al estado FALLADO y registrar el motivo del fallo devuelto por el envío.

#### Dirigidos por estado (E-ST)

- E-ST-001 — Mientras una TareaCorreo no esté en estado FALLADO, la pantalla Detalle del correo debe ocultar al Administrador la acción "Reenviar".
- *E-ST-002 — Mientras una TareaCorreo tenga referencia a un historial de estado de expediente, la pantalla Detalle del correo debe ofrecer un enlace para navegar al expediente correspondiente, sin conceder permisos adicionales sobre él.

#### Comportamiento no deseado (E-UN)

- E-UN-001 — Si se intenta crear una TareaCorreo sin asunto, sin cuerpo, sin DNI del destinatario o sin dirección de correo del destinatario, entonces el sistema debe rechazar la creación avisando al usuario del dato concreto que falta.
- *E-UN-002 — Si la dirección de correo del destinatario no tiene un formato válido de correo electrónico, entonces el sistema debe rechazar la creación de la TareaCorreo con el mensaje "La dirección de correo '{valor}' no tiene un formato válido".
- E-UN-003 — Si la referencia a historial de estado indicada al crear una TareaCorreo no corresponde a un evento existente, entonces el sistema debe rechazar la creación.
- E-UN-004 — Si se intenta modificar cualquier dato funcional (asunto, cuerpo, destinatario, dirección de correo, centro, expediente relacionado, adjuntos) de una TareaCorreo ya creada, entonces el sistema debe rechazar la operación avisando de que los correos enviados son un registro histórico y no pueden modificarse.
- E-UN-005 — Si se intenta borrar una TareaCorreo, entonces el sistema debe rechazar la operación avisando de que los correos enviados son un registro histórico y no pueden borrarse.
- E-UN-006 — Si el Administrador intenta reenviar una TareaCorreo que no está en estado FALLADO (PENDIENTE, ENVIANDO o ENVIADO), entonces el sistema debe rechazar la operación indicando que el correo no está en un estado que permita reenvío.
- E-UN-007 — Si un usuario que no es Administrador intenta crear una TareaCorreo nueva o reenviar una TareaCorreo fallida, entonces el sistema debe rechazar la operación indicando que no tiene permiso para esta acción.
- E-UN-008 — Si un Supervisor o Administrativa intenta acceder por cualquier vía (listado o acceso directo) al detalle de una TareaCorreo cuyo centro no coincide con el suyo, o que no tiene centro asignado, entonces el sistema debe rechazar el acceso.
- *E-UN-009 — Si en la pantalla "Gráfica de correos enviados" la fecha inicial seleccionada es posterior a la fecha final, entonces el sistema debe avisar al usuario de que el rango de fechas no es válido.

### Asunciones a confirmar

- **`*E-UN-002` — Formato del correo electrónico del destinatario**: se valida en el momento de crear la TareaCorreo (no se pospone al envío SMTP). Justificación: es mejor avisar al usuario en el momento de introducir el dato.
- **`E-UB-002` y `E-UB-007` — Correos sin centro**: solo el Administrador los ve en sus listados; el propio destinatario los sigue viendo en "Mis correos". Supervisor y Administrativa no los ven aunque el destinatario pertenezca a su centro, porque el criterio acordado es el campo "centro" **explícito** y no la deducción a partir del DNI.
- **`*E-UB-010` y `*E-UN-009` — Rango de fechas de la gráfica**: ambas fechas son obligatorias y la inicial no puede ser posterior a la final. Justificación: interpretación natural de "una gráfica entre dos fechas".
- **`*E-UB-011` — Adjuntos descargables en cualquier estado**: incluso si el correo está FALLADO o aún PENDIENTE; útil para el Administrador y para el destinatario.
- **`*E-ST-002` — Navegación al expediente desde el detalle** cuando hay referencia a historial de estado: facilita la trazabilidad pero no concede permisos adicionales sobre el expediente.
- **Número de intentos y fecha del último intento** como datos visibles en el detalle: útiles para diagnosticar reenvíos sucesivos.
