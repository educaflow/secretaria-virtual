# Entidad: TareaCorreo

Representa un correo electrónico que el sistema debe enviar a un destinatario o que ya ha sido enviado. Una vez creada, su contenido es inmutable; solo cambian su estado y los datos asociados al intento de envío, gestionados por el propio sistema.

## Modelo de datos

| Campo | Tipo de dato | Relación | Notas |
|-------|--------------|----------|-------|
| asunto | texto | — | Asunto del correo. |
| cuerpo | HTML enriquecido | — | Cuerpo del mensaje. |
| dniDestinatario | texto | — | DNI del destinatario. Se registra aunque no exista un usuario asociado. |
| emailDestinatario | texto | — | Dirección de correo del destinatario. |
| centro | relación | → Centro | Opcional. Lo fija quien crea la tarea. Vacío significa correo de ámbito global. |
| historialEstado | relación | → HistorialEstado | Opcional. Vincula el correo a un cambio de estado de un expediente. |
| fechaCreacion | fecha-hora | — | Momento en que se creó la tarea. |
| fechaUltimoIntento | fecha-hora | — | Momento del último intento de envío. Vacío hasta el primer intento. |
| numeroIntentos | entero | — | Número de intentos realizados. Inicial 0. |
| motivoFallo | texto largo | — | Motivo del último fallo. Vacío si no ha fallado o tras reenviar. |
| estado | enum | valores: PENDIENTE, ENVIANDO, ENVIADO, FALLADO | Solo lo cambia el sistema. |
| adjuntos | lista | → AdjuntoCorreo (uno a varios, hijos) | Copia propia e inmutable de los ficheros adjuntos. |

## Validaciones (V-TareaCorreo-NNN)

| ID | Campo(s) | Descripción | Condición | Mensaje al usuario |
|----|----------|-------------|-----------|--------------------|
| V-TareaCorreo-001 | asunto | El asunto es obligatorio. | asunto vacío | El asunto es obligatorio. |
| V-TareaCorreo-002 | cuerpo | El cuerpo es obligatorio. | cuerpo vacío | El cuerpo del correo es obligatorio. |
| V-TareaCorreo-003 | dniDestinatario | El DNI del destinatario es obligatorio. | dniDestinatario vacío | El DNI del destinatario es obligatorio. |
| V-TareaCorreo-004 | emailDestinatario | La dirección de correo del destinatario es obligatoria. | emailDestinatario vacío | La dirección de correo del destinatario es obligatoria. |
| V-TareaCorreo-005 | emailDestinatario | La dirección de correo debe tener formato válido. | emailDestinatario no respeta el formato de un email | La dirección de correo '{valor}' no tiene un formato válido. |
| V-TareaCorreo-006 | historialEstado | Si se indica un historial de estado, debe existir. | historialEstado referencia un registro inexistente | El historial de estado indicado no existe. |
| V-TareaCorreo-007 | centro | Si se indica un centro, debe existir. | centro referencia un registro inexistente | El centro indicado no existe. |
| V-TareaCorreo-008 | estado | El estado solo puede tomar uno de los valores admitidos. | estado fuera de {PENDIENTE, ENVIANDO, ENVIADO, FALLADO} | El estado '{valor}' no es válido. Valores admitidos: PENDIENTE, ENVIANDO, ENVIADO, FALLADO. |

## Acciones

| Operación | Cuándo se permite | Validaciones que aplican | Reglas que dispara |
|-----------|-------------------|--------------------------|--------------------|
| Crear (insert) | Solo Administrador. | V-TareaCorreo-001, V-TareaCorreo-002, V-TareaCorreo-003, V-TareaCorreo-004, V-TareaCorreo-005, V-TareaCorreo-006, V-TareaCorreo-007 | R-TareaCorreo-001, R-TareaCorreo-002, R-TareaCorreo-003 |
| Modificar (update) | Nunca — la tarea de correo es inmutable salvo por los cambios de estado y datos de intento que ejecuta el propio sistema. | — | — |
| Borrar (remove) | Nunca — las tareas de correo deben conservarse como traza permanente del envío. | — | — |
| Procesar envío | El sistema, en segundo plano, sobre tareas en estado PENDIENTE. | V-TareaCorreo-008 | R-TareaCorreo-004, R-TareaCorreo-005, R-TareaCorreo-006, R-TareaCorreo-007 |
| Reenviar | Solo Administrador, y solo si el estado actual es FALLADO. | V-TareaCorreo-008 | R-TareaCorreo-008 |
| Consultar | Cada rol según sus filtros de visibilidad. Solo lectura. | — | — |

## Reglas de negocio (R-TareaCorreo-NNN)

| ID | Descripción | Entidad | Método | Momento | Más información |
|----|-------------|---------|--------|---------|-----------------|
| R-TareaCorreo-001 | Al crear, el estado inicial es PENDIENTE, el número de intentos 0, la fecha del último intento y el motivo de fallo vacíos. | TareaCorreo | Crear | Antes | Garantiza el punto de partida de la máquina de estados. |
| R-TareaCorreo-002 | Al crear, si el usuario que crea pertenece a un centro, se asigna ese centro automáticamente. | TareaCorreo | Crear | Antes | Si el creador es Administrador y no indica centro, queda vacío (ámbito global). |
| R-TareaCorreo-003 | Al crear, se generan los AdjuntoCorreo como copia propia e inmutable de los ficheros aportados. | TareaCorreo | Crear | Después | La copia desacopla el correo de los ficheros originales. |
| R-TareaCorreo-004 | Al iniciar el procesamiento, el estado pasa de PENDIENTE a ENVIANDO, se incrementa en 1 el número de intentos y se actualiza la fecha del último intento. | TareaCorreo | Procesar envío | Antes | Marca el inicio del intento de envío. |
| R-TareaCorreo-005 | El envío del correo se realiza de forma asíncrona, sin bloquear al usuario que creó la tarea. | TareaCorreo | Procesar envío | Después | El control vuelve enseguida al usuario tras crear. |
| R-TareaCorreo-006 | Si el envío tiene éxito, el estado pasa de ENVIANDO a ENVIADO. ENVIADO es un estado final. | TareaCorreo | Procesar envío | Después | No se permiten transiciones desde ENVIADO. |
| R-TareaCorreo-007 | Si el envío falla, el estado pasa de ENVIANDO a FALLADO y se registra el motivo del fallo. | TareaCorreo | Procesar envío | Después | El motivo queda disponible para diagnóstico y para mostrarlo al Administrador. |
| R-TareaCorreo-008 | Al reenviar, el estado pasa de FALLADO a PENDIENTE y se limpia el motivo del fallo previo. El contenido del correo no se altera. | TareaCorreo | Reenviar | Antes | El nuevo intento será contabilizado por R-TareaCorreo-004 cuando el sistema procese la tarea. |