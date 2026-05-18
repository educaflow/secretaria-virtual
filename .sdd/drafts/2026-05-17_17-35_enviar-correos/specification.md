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

### Reglas y validaciones

- Al crear una TareaCorreo, son obligatorios el asunto, el cuerpo, el DNI del destinatario y la dirección de correo del destinatario; si falta alguno se avisa al usuario indicando el dato que falta.
- El DNI del destinatario se registra aunque no exista un usuario dado de alta con ese DNI: no se exige que el destinatario sea un usuario del sistema.
- La dirección de correo del destinatario debe tener forma de correo electrónico válida; en caso contrario se avisa al usuario de que la dirección no es válida.*
- El centro de la TareaCorreo es opcional y lo fija quien crea el correo; si se deja vacío, el correo se considera "del sistema" y solo lo ven el Administrador y el propio destinatario en "Mis correos".
- La referencia al historial de estado de un expediente es opcional; cuando se indica, debe corresponder a un evento de historial existente y enlaza la TareaCorreo con ese cambio de estado concreto (no con el expediente "en general").
- Los adjuntos son opcionales; cuando se adjunta un fichero, el sistema guarda una copia propia, de modo que el contenido registrado coincide siempre con el contenido enviado, aunque el fichero original cambie o se borre después.
- Una vez creada, una TareaCorreo no se puede modificar en ninguno de sus datos funcionales (asunto, cuerpo, destinatario, dirección de correo, centro, expediente relacionado, adjuntos); si se intenta, se avisa de que los correos enviados son un registro histórico y no pueden modificarse.
- Una TareaCorreo no se puede borrar nunca; si se intenta, se avisa con el mismo motivo.
- El envío al servidor de correo no es síncrono: cuando el Administrador pulsa enviar, el sistema confirma la creación inmediatamente y entrega el correo en segundo plano.
- El estado de una TareaCorreo solo lo cambia el propio sistema durante el procesamiento de envío; ningún usuario lo edita a mano.
- La acción de reenviar solo está disponible cuando la TareaCorreo está en estado FALLADO; si el usuario intenta reenviar un correo en PENDIENTE, ENVIANDO o ENVIADO, se le indica que el correo no está en un estado que permita reenvío.
- Solo el Administrador puede crear correos nuevos y solo el Administrador puede reenviar correos fallidos; si otro usuario lo intenta, se le indica que no tiene permiso para esta acción.
- Al reenviar, se reutiliza la misma TareaCorreo: no se crea una nueva, su estado pasa a PENDIENTE, se actualiza la fecha del último intento y se incrementa el número de intentos.
- Cuando una TareaCorreo pasa a FALLADO, se registra el motivo del fallo devuelto por el envío SMTP para que el Administrador pueda diagnosticarlo.
- Un Supervisor o Administrativa nunca ve correos de otro centro distinto del suyo, ni los correos sin centro asignado; tampoco puede acceder al detalle de un correo fuera de su centro por acceso directo.
- Un usuario destinatario (Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo) solo ve los correos cuyo DNI de destinatario coincide con el suyo, sin filtro adicional por centro; nunca ve correos dirigidos a otras personas.
- En la pantalla "Todos los correos" el Administrador ve todos los correos, con y sin centro.
- En la gráfica de envíos, las dos fechas son obligatorias y la fecha inicial no puede ser posterior a la fecha final; si lo es, se avisa al usuario de que el rango de fechas no es válido.*
- En el detalle, los adjuntos son siempre descargables, también cuando el correo está fallado o pendiente.*
- Cuando la TareaCorreo está asociada a un expediente, desde el detalle del correo se puede navegar al expediente correspondiente.*

### Asunciones a confirmar

- **Formato del correo electrónico del destinatario** se valida en el momento de crear la TareaCorreo (no se pospone al envío SMTP). Justificación: es mejor avisar al usuario en el momento de introducir el dato.
- **Correos sin centro**: solo el Administrador los ve en sus listados; el propio destinatario los sigue viendo en "Mis correos". Supervisor y Administrativa no los ven aunque el destinatario pertenezca a su centro, porque el criterio acordado es el campo "centro" **explícito** y no la deducción a partir del DNI.
- **Rango de fechas de la gráfica**: ambas fechas son obligatorias y la inicial no puede ser posterior a la final. Justificación: interpretación natural de "una gráfica entre dos fechas".
- **Adjuntos descargables en cualquier estado**: incluso si el correo está FALLADO o aún PENDIENTE; útil para el Administrador y para el destinatario.
- **Navegación al expediente desde el detalle** cuando hay referencia a historial de estado: facilita la trazabilidad pero no concede permisos adicionales sobre el expediente.
- **Número de intentos y fecha del último intento** como datos visibles en el detalle: útiles para diagnosticar reenvíos sucesivos
