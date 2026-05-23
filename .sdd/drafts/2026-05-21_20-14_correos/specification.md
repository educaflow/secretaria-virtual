---
type: specification
---

## Especificación funcional: Correos

**Tipo:** subsistema
**Capa funcional:** correos
**Descripción:** Subsistema que registra en base de datos cada correo electrónico que la aplicación envía o intenta enviar a un destinatario identificado por DNI, gestiona su envío asíncrono apoyándose en la infraestructura de correo existente y ofrece distintas vistas de consulta según el rol del usuario.

### Entidades

- **Correo** — Registro de un correo electrónico que la aplicación envía o ha intentado enviar a un único destinatario identificado por DNI. Una vez creado, sus datos de envío son inmutables; solo evolucionan su estado, su contador de intentos, la fecha del último intento y el motivo del último fallo. Campos funcionalmente relevantes: asunto, cuerpo (texto enriquecido), DNI del destinatario, email del destinatario, fecha de creación, fecha de envío, estado, número de intentos, fecha del último intento, motivo del último fallo, centro, referencia opcional al historial de estado de un expediente, adjuntos. Estados: PENDIENTE, ENVIADO, FALLIDO.
- **AdjuntoCorreo** — Copia inmutable de un fichero adjunto asociado a un Correo, tomada en el momento de su creación. Pertenece a un único Correo. Campos funcionalmente relevantes: nombre del fichero, contenido del fichero, correo al que pertenece.

### Dependencias de otros subsistemas

- **expedientes** — Para la referencia opcional al historial de estado de un expediente cuando el Correo se origina desde su tramitación.
- **security** — Para identificar al usuario logado, su rol, su DNI y su centro activo.
- **registrousuario** — Para resolver, dado un DNI, el email del usuario correspondiente cuando exista, y así autocompletarlo al crear un Correo manualmente.
- **base/infrastructure/mail** — Infraestructura existente que realiza el envío SMTP real. El subsistema la invoca, pero no la implementa.

### Operaciones

- **Crear correo manualmente**: el Administrador da de alta un nuevo Correo indicando DNI del destinatario, asunto, cuerpo y, opcionalmente, adjuntos. Si existe un usuario con ese DNI, el sistema autocompleta su email; en caso contrario el Administrador lo escribe a mano. El Correo queda registrado en estado PENDIENTE y sin centro asociado (el Administrador no pertenece a un centro concreto).
- **Crear correo programáticamente**: otro subsistema solicita el alta de un Correo aportando destinatario, asunto, cuerpo, adjuntos, centro y, opcionalmente, la referencia al historial de estado de un expediente. El Correo queda registrado en estado PENDIENTE.
- **Enviar correos pendientes (asíncrono)**: una tarea periódica toma los Correos en estado PENDIENTE, los entrega a la infraestructura de correo, incrementa el contador de intentos, registra la fecha del último intento y marca el Correo como ENVIADO o FALLIDO según el resultado. Solo realiza un único intento automático por cada vez que un Correo entra en PENDIENTE. La frecuencia con la que se ejecuta esta tarea está parametrizada como una propiedad del fichero de configuración.
- **Reenviar correo fallido**: el Administrador, sobre un Correo en estado FALLIDO, solicita relanzarlo; el Correo vuelve a PENDIENTE para que la próxima ejecución de la tarea periódica lo intente de nuevo.
- **Consultar correos de mi centro**: el Supervisor o la Administrativa consultan, en modo solo lectura, los Correos cuyo centro coincide con el suyo.
- **Consultar todos los correos**: el Administrador consulta, en modo solo lectura, todos los Correos del sistema sin filtro por centro.
- **Consultar mis correos**: Profesor, Alumno, Exprofesor, Exalumno, Familiar o Externo consulta, en modo solo lectura, los Correos cuyo DNI de destinatario coincide con el suyo.
- **Visualizar gráfica de correos**: el Administrador consulta una gráfica de barras apiladas por estado entre dos fechas, eligiendo granularidad temporal entre día, semana y mes. Si la fecha final es anterior a la inicial, el sistema rechaza la consulta.

### Flujos principales

- F-001 — El Administrador da de alta un nuevo Correo indicando DNI, asunto, cuerpo y, opcionalmente, adjuntos; el sistema lo registra como pendiente, sin centro asociado, a la espera de envío.
- F-002 — De forma periódica, el sistema toma cada Correo pendiente y lo entrega a la infraestructura de correo; si el envío tiene éxito, el Correo pasa a enviado con su fecha de envío.
- F-003 — El sistema intenta enviar un Correo pendiente y el envío falla; el Correo queda como fallido, registra el motivo, la fecha del último intento e incrementa el contador, y no vuelve a intentarse automáticamente.
- F-004 — El Administrador localiza un Correo fallido, consulta el motivo del último fallo y solicita reenviarlo; el sistema lo devuelve a pendiente para que la próxima ejecución de la tarea periódica lo reintente.
- F-005 — Otro subsistema, al producirse un evento de negocio (por ejemplo un cambio relevante en un expediente), solicita programáticamente el alta de un Correo asociado opcionalmente al historial de estado del expediente; el Correo queda pendiente y sigue el ciclo normal de envío.
- F-006 — Un Supervisor o una Administrativa consulta los Correos enviados desde su centro para comprobar qué se ha enviado a una persona concreta.
- F-007 — Un Profesor, Alumno, Exprofesor, Exalumno, Familiar o Externo consulta el listado de Correos que la aplicación le ha enviado, identificándose por su propio DNI.
- F-008 — El Administrador consulta la gráfica de Correos, indica fechas inicial y final y elige la granularidad temporal, y obtiene una representación agregada del volumen de Correos por estado a lo largo del rango.

### Pantallas

- **Todos los correos**: listado de todos los Correos del sistema sin filtro de centro. Desde esta pantalla el Administrador puede dar de alta un nuevo Correo (botón "Nuevo correo") y abrir el detalle de cualquier Correo. El botón de reenviar está dentro del detalle, no en la lista. La ve el Administrador.
- **Formulario de Correo (alta y detalle)**: **una única pantalla** que se usa en dos modos. En modo **alta** (al pulsar "Nuevo correo" desde "Todos los correos") es editable y pide DNI del destinatario, email (autocompletado si el DNI corresponde a un usuario existente; editable a mano si no), asunto, cuerpo enriquecido y adjuntos opcionales; al guardar, el Correo queda en PENDIENTE y la pantalla pasa a modo detalle. En modo **detalle** (al abrir un Correo existente desde cualquier listado) la pantalla es solo lectura y, si el usuario es Administrador y el Correo está en FALLIDO, muestra la acción "Reenviar". El modo alta solo lo ve el Administrador; el modo detalle lo ven todos los roles que pueden abrir Correos desde su listado correspondiente.
- **Correos de mi centro**: listado en modo solo lectura de los Correos cuyo centro coincide con el centro del usuario logado. La ven Supervisor y Administrativa. Al abrir un Correo se muestra el Formulario de Correo en modo detalle.
- **Mis correos**: listado en modo solo lectura de los Correos cuyo DNI de destinatario coincide con el DNI del usuario logado. La ven Profesor, Alumno, Exprofesor, Exalumno, Familiar y Externo. Al abrir un Correo se muestra el Formulario de Correo en modo detalle.
- **Gráfica de correos**: gráfica de barras apiladas por estado entre dos fechas, con selector de granularidad temporal (día, semana o mes). La ve el Administrador.

### Menús

- Correos → (raíz)
- Correos / Todos los correos → Todos los correos (Administrador)
- Correos / Correos de mi centro → Correos de mi centro (Supervisor, Administrativa)
- Correos / Gráfica de correos → Gráfica de correos (Administrador)
- Correos / Mis correos → Mis correos (Profesor, Alumno, Exprofesor, Exalumno, Familiar, Externo)

### Seguridad

- **Administrador**: puede ver todos los Correos sin filtro por centro, crear Correos manualmente, reenviar Correos fallidos y ver la gráfica.
- **Supervisor**: puede ver los Correos cuyo centro coincide con su centro, en modo solo lectura. No crea ni reenvía.
- **Administrativa**: puede ver los Correos cuyo centro coincide con su centro, en modo solo lectura. No crea ni reenvía.
- **Profesor, Alumno, Exprofesor, Exalumno, Familiar, Externo**: puede ver únicamente los Correos cuyo DNI de destinatario coincide con su propio DNI, en modo solo lectura. No crea ni reenvía.
- Multicentro: sí. Cada Correo pertenece a un único centro o a ninguno y los listados de Supervisor y Administrativa están restringidos a su centro.

### Máquina de estados

Estados del Correo: PENDIENTE, ENVIADO, FALLIDO.

- Estado inicial al crear el Correo (manual o programáticamente): PENDIENTE.
- PENDIENTE → ENVIADO: cuando el intento automático de envío termina con éxito.
- PENDIENTE → FALLIDO: cuando el intento automático de envío termina con error.
- FALLIDO → PENDIENTE: cuando el Administrador solicita reenviar el Correo manualmente.
- ENVIADO: estado terminal, sin transiciones salientes.
- Por cada vez que el Correo entra en PENDIENTE, la tarea periódica realiza un único intento automático; si falla, el Correo queda en FALLIDO y solo el Administrador puede relanzarlo manualmente.

### Campos calculados

- **email del destinatario (en el alta manual)**: si existe un usuario con el DNI introducido, se propone su email; en caso contrario queda vacío para que el Administrador lo escriba a mano. Una vez creado el Correo, el email queda fijo y no se recalcula.
- **centro del Correo**: en el alta manual queda sin centro (el Administrador no pertenece a un centro concreto); en el alta programática recibe el centro que indique el subsistema invocador.
- **fecha de creación**: se asigna automáticamente al registrar el Correo.
- **número de intentos**: se incrementa en uno cada vez que la tarea periódica ejecuta un intento de envío sobre el Correo.
- **fecha del último intento**: se actualiza con el momento del último intento de envío.
- **motivo del último fallo**: se actualiza con la descripción del error cuando un intento termina en FALLIDO.
- **datos agregados de la gráfica**: número de Correos agrupados por estado y por intervalo temporal (día, semana o mes según la granularidad elegida), dentro del rango de fechas indicado.

### Requisitos (EARS)

#### Ubicuos (E-UB)

- E-UB-001 — El sistema debe registrar en la base de datos cada Correo antes de intentar enviarlo.
- E-UB-002 — El Correo debe identificar a su destinatario mediante un DNI obligatorio.
- E-UB-003 — El Correo debe conservar el email del destinatario tal como quedó registrado en el momento de su creación.
- E-UB-004 — El Correo debe tener asunto y cuerpo, ambos obligatorios.
- E-UB-005 — El Correo debe admitir cuerpo redactado como texto enriquecido.
- E-UB-006 — El Correo debe poder tener cero o más adjuntos.
- E-UB-007 — El AdjuntoCorreo debe ser una copia inmutable del fichero adjunto en el momento de crear el Correo.
- E-UB-008 — El Correo debe estar asociado a cero o un centro.
- E-UB-009 — El Correo debe registrar su estado, su número de intentos, la fecha del último intento y, si aplica, el motivo del último fallo.
- E-UB-010 — El subsistema Correos debe enviar los Correos de forma asíncrona respecto a su creación, sin bloquear al usuario o al subsistema que los origina.
- E-UB-011 — El subsistema Correos debe delegar el envío SMTP real en la infraestructura de correo existente.
- E-UB-012 — El subsistema Correos debe leer la frecuencia con la que se ejecuta la tarea periódica de envío desde una propiedad del fichero de configuración.

#### Dirigidos por evento (E-EV)

- E-EV-001 — Cuando el Administrador da de alta un Correo desde la pantalla de creación manual, el subsistema Correos debe registrarlo en estado PENDIENTE sin centro asociado y guardar los adjuntos como AdjuntoCorreo.
- E-EV-002 — Cuando otro subsistema solicita programáticamente el alta de un Correo, el subsistema Correos debe registrarlo en estado PENDIENTE con el centro y la referencia opcional al historial de estado del expediente que indique el subsistema invocador.
- E-EV-003 — Cuando el Administrador introduce el DNI del destinatario en el alta manual y existe un usuario con ese DNI, el subsistema Correos debe autocompletar el email del destinatario con el email de ese usuario.
- E-EV-004 — Cuando la tarea periódica de envío toma un Correo en estado PENDIENTE, el subsistema Correos debe intentar enviarlo una sola vez a través de la infraestructura de correo, incrementar el número de intentos y actualizar la fecha del último intento.
- E-EV-005 — Cuando el intento de envío de un Correo termina con éxito, el subsistema Correos debe pasarlo al estado ENVIADO y registrar la fecha de envío.
- E-EV-006 — Cuando el intento de envío de un Correo termina con error, el subsistema Correos debe pasarlo al estado FALLIDO y registrar el motivo del fallo.
- E-EV-007 — Cuando el Administrador solicita reenviar un Correo en estado FALLIDO, el subsistema Correos debe devolverlo al estado PENDIENTE para que la próxima ejecución de la tarea periódica lo reintente.
- E-EV-008 — Cuando el Administrador consulta la gráfica indicando un rango de fechas y una granularidad temporal, el subsistema Correos debe mostrar el número de Correos por estado agrupados según la granularidad indicada.
- E-EV-009 — Cuando el Administrador introduce el DNI del destinatario en el alta manual y no existe ningún usuario con ese DNI, el subsistema Correos debe dejar vacío el email del destinatario para que el Administrador lo introduzca manualmente.

#### Dirigidos por estado (E-ST)

- E-ST-001 — Mientras un Correo está en estado PENDIENTE, la tarea periódica debe considerarlo elegible para un único intento automático de envío.
- E-ST-002 — Mientras un Correo está en estado FALLIDO, el subsistema Correos debe permitir al Administrador solicitar su reenvío manual y no debe reintentarlo automáticamente.
- E-ST-003 — Mientras un Correo está en estado ENVIADO, el subsistema Correos no debe permitir ninguna transición de estado posterior ni ningún reenvío.
- E-ST-004 — Mientras un Correo existe en el sistema, el subsistema Correos no debe permitir modificar su DNI de destinatario, su email, su asunto, su cuerpo, sus adjuntos, su centro ni su referencia al historial de estado del expediente.
- E-ST-005 — Mientras un usuario consulta "Mis correos", el subsistema Correos debe mostrarle únicamente los Correos cuyo DNI de destinatario coincide con su propio DNI.
- E-ST-006 — Mientras un Supervisor o una Administrativa consultan "Correos de mi centro", el subsistema Correos debe mostrarles únicamente los Correos cuyo centro coincide con su centro.

#### Comportamiento no deseado (E-UN)

- E-UN-001 — Si el Administrador intenta crear un Correo sin DNI del destinatario, sin email del destinatario, sin asunto o sin cuerpo, entonces el subsistema Correos debe rechazar la creación.
- E-UN-002 — Si un usuario distinto del Administrador intenta crear un Correo manualmente, entonces el subsistema Correos debe rechazar la operación.
- E-UN-003 — Si un usuario distinto del Administrador intenta reenviar un Correo, entonces el subsistema Correos debe rechazar la operación.
- E-UN-004 — Si el Administrador intenta reenviar un Correo que no está en estado FALLIDO, entonces el subsistema Correos debe rechazar la operación.
- E-UN-005 — Si alguien intenta modificar el DNI, el email, el asunto, el cuerpo, los adjuntos, el centro o la referencia al historial de estado de un Correo ya creado, entonces el subsistema Correos debe rechazar la modificación.
- E-UN-006 — Si un Supervisor o una Administrativa intentan acceder a Correos de un centro distinto al suyo, entonces el subsistema Correos debe impedir el acceso.
- E-UN-007 — Si un Profesor, Alumno, Exprofesor, Exalumno, Familiar o Externo intenta acceder a un Correo cuyo DNI de destinatario no coincide con el suyo, entonces el subsistema Correos debe impedir el acceso.
- E-UN-008 — Si un usuario distinto del Administrador intenta acceder a la gráfica de correos, entonces el subsistema Correos debe denegar el acceso.
- E-UN-009 — Si la referencia al historial de estado del expediente se intenta asignar o modificar desde la interfaz, entonces el subsistema Correos debe rechazar la operación (solo es asignable programáticamente).
- E-UN-010 — Si en la consulta de la gráfica la fecha final es anterior a la fecha inicial, entonces el subsistema Correos debe rechazar la consulta.

#### Características opcionales (E-OP)

- E-OP-001 — Donde un Correo se origina desde un expediente, el subsistema Correos debe permitir conservar la referencia al historial de estado correspondiente.
- E-OP-002 — Donde un Correo se crea con adjuntos (ya sea manualmente o programáticamente), el subsistema Correos debe guardarlos como copia inmutable asociada al Correo.
- E-OP-003 — Donde el Administrador consulta la gráfica de Correos, el subsistema Correos debe permitirle elegir la granularidad temporal entre día, semana y mes.

### Asunciones a confirmar

*(ningún elemento inferido)*