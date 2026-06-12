# Entidad: TareaCorreo

Cada correo electrónico que la aplicación envía o intenta enviar. La tarea es inmutable: una vez creada, solo cambia su estado a través de las operaciones de envío o reintento.

## Modelo de datos

| Campo                | Tipo de dato | Relación                                          | Origen del valor | Notas                                                            |
|----------------------|--------------|---------------------------------------------------|------------------|------------------------------------------------------------------|
| estado               | enum         | valores: PENDIENTE, ENVIANDO, ENVIADO, FALLADO    | servidor         | estado actual de la tarea de envío                               |
| fechaCreacion        | fecha-hora   | —                                                 | servidor         | momento en que se crea la tarea                                  |
| fechaUltimoIntento   | fecha-hora   | —                                                 | servidor         | momento del último intento de envío                              |
| fechaEnvioOk         | fecha-hora   | —                                                 | servidor         | momento en que el envío se completó con éxito                    |
| numIntentos          | entero       | —                                                 | servidor         | número acumulado de intentos                                     |
| centro               | relación     | → Centro                                          | cliente          | centro al que pertenece la tarea (puede ser nulo si es global)   |
| de                   | texto        | —                                                 | servidor         | dirección remitente; asignada por el sistema desde la config SMTP|
| asunto               | texto        | —                                                 | cliente          | asunto del correo                                                |
| destinatarioDni      | texto        | —                                                 | cliente          | DNI/NIE opcional del destinatario                                |
| destinatarioEmail    | texto        | —                                                 | cliente          | dirección de email del destinatario                              |
| destinatarioNombre   | texto        | —                                                 | cliente          | nombre legible del destinatario                                  |
| cuerpoHtml           | HTML enriquecido | —                                             | cliente          | cuerpo del correo en HTML                                        |
| cuerpoTextoPlano     | texto largo  | —                                                 | cliente          | versión texto plano del cuerpo (opcional)                        |
| logErrores           | texto largo  | —                                                 | servidor         | trazas de los intentos fallidos                                  |
| historialExpediente  | relación     | → HistorialExpediente                             | servidor         | enlace opcional al expediente que originó el correo              |
| adjuntos             | lista        | → AdjuntoCorreo (uno a varios, hijos)             | cliente          | ficheros adjuntos al correo                                      |

## Validaciones (V-TareaCorreo-NNN)

| ID    | Campo(s)            | Descripción                                  | Condición | Mensaje al usuario                                | Origen spec |
|-------|---------------------|----------------------------------------------|-----------|---------------------------------------------------|-------------|
| V-TareaCorreo-001 | asunto              | El asunto debe estar relleno                 | Siempre   | "El asunto es obligatorio."                       | —           |
| V-TareaCorreo-002 | cuerpoHtml          | El cuerpo del correo debe estar relleno      | Siempre   | "El cuerpo del correo es obligatorio."            | —           |
| V-TareaCorreo-003 | destinatarioEmail   | El email del destinatario debe estar relleno | Siempre   | "El email del destinatario es obligatorio."       | —           |
| V-TareaCorreo-004 | destinatarioEmail   | El email debe tener formato `usuario@dominio.com` | Siempre | "El email '{email}' no tiene un formato válido." | —           |
| V-TareaCorreo-005 | destinatarioDni     | El DNI/NIE debe tener formato válido         | Si no es nulo | "El DNI/NIE '{dni}' no tiene un formato válido."| —           |

## Acciones

| Operación           | Cuándo se permite                                                   | Validaciones que aplican | Reglas que dispara |
|---------------------|---------------------------------------------------------------------|--------------------------|--------------------|
| Crear (insert)      | Siempre (manual desde la pantalla admin u otros sistemas)           | V-TareaCorreo-001..005             | R-TareaCorreo-001, R-TareaCorreo-005 |
| Modificar (update)  | Nunca — el correo es inmutable una vez creado                       | —                        | —                  |
| Borrar (remove)     | Nunca — el correo es inmutable una vez creado                       | —                        | —                  |
| Reintentar envío    | Solo si estado = FALLADO                                            | —                        | R-TareaCorreo-002              |
| Procesar pendientes | Solo si estado = PENDIENTE (lo ejecuta el proceso periódico)        | —                        | R-TareaCorreo-003, R-TareaCorreo-004       |

## Reglas de negocio (R-TareaCorreo-NNN)

| ID    | Descripción                                                                                          | Entidad     | Método              | Momento | Más información                                                       | Origen spec |
|-------|------------------------------------------------------------------------------------------------------|-------------|---------------------|---------|-----------------------------------------------------------------------|-------------|
| R-TareaCorreo-001 | Inicializa estado=PENDIENTE, fechaCreacion=ahora, numIntentos=0 y "de"=remitente SMTP configurado    | TareaCorreo | insert              | Antes   | Valores fijados por el sistema; el usuario nunca los elige            | —           |
| R-TareaCorreo-002 | Vuelve a poner el correo en estado PENDIENTE para que el proceso periódico lo reintente              | TareaCorreo | Reintentar envío    | Antes   | Escribe sobre el propio registro                                      | —           |
| R-TareaCorreo-003 | Intenta el envío SMTP; si sale bien, marca estado=ENVIADO y fechaEnvioOk=ahora                       | TareaCorreo | Procesar pendientes | Antes   | Solo proceso periódico; caso de éxito — escribe el resultado sobre el propio registro | —           |
| R-TareaCorreo-004 | Si el envío falla, marca estado=FALLADO, incrementa numIntentos, actualiza fechaUltimoIntento y logErrores | TareaCorreo | Procesar pendientes | Antes   | Solo proceso periódico; caso de fallo — escribe el resultado sobre el propio registro | —           |
| R-TareaCorreo-005 | Asigna el expediente que originó el correo cuando la tarea la crea el sistema de expedientes         | TareaCorreo | insert              | Antes   | Solo si el correo proviene de un expediente; el usuario nunca lo elige | —           |
