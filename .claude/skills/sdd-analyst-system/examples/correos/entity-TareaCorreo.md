# Entidad: TareaCorreo

Cada correo electrónico que la aplicación envía o intenta enviar. La tarea es inmutable: una vez creada, solo cambia su estado a través de las operaciones de envío o reintento.

## Modelo de datos

| Campo                | Tipo de dato | Relación                                          | Notas                                                            |
|----------------------|--------------|---------------------------------------------------|------------------------------------------------------------------|
| estado               | enum         | valores: PENDIENTE, ENVIANDO, ENVIADO, FALLADO    | estado actual de la tarea de envío                               |
| fechaCreacion        | fecha-hora   | —                                                 | momento en que se crea la tarea                                  |
| fechaUltimoIntento   | fecha-hora   | —                                                 | momento del último intento de envío                              |
| fechaEnvioOk         | fecha-hora   | —                                                 | momento en que el envío se completó con éxito                    |
| numIntentos          | entero       | —                                                 | número acumulado de intentos                                     |
| centro               | relación     | → Centro                                          | centro al que pertenece la tarea (puede ser nulo si es global)   |
| de                   | texto        | —                                                 | dirección remitente; asignada por el sistema desde la config SMTP|
| asunto               | texto        | —                                                 | asunto del correo                                                |
| destinatarioDni      | texto        | —                                                 | DNI/NIE opcional del destinatario                                |
| destinatarioEmail    | texto        | —                                                 | dirección de email del destinatario                              |
| destinatarioNombre   | texto        | —                                                 | nombre legible del destinatario                                  |
| cuerpoHtml           | HTML enriquecido | —                                             | cuerpo del correo en HTML                                        |
| cuerpoTextoPlano     | texto largo  | —                                                 | versión texto plano del cuerpo (opcional)                        |
| logErrores           | texto largo  | —                                                 | trazas de los intentos fallidos                                  |
| historialExpediente  | relación     | → HistorialExpediente                             | enlace opcional al expediente que originó el correo              |
| adjuntos             | lista        | → AdjuntoCorreo (uno a varios, hijos)             | ficheros adjuntos al correo                                      |

## Validaciones (V-XXX)

| ID    | Campo(s)            | Descripción                                  | Condición | Mensaje al usuario                                |
|-------|---------------------|----------------------------------------------|-----------|---------------------------------------------------|
| V-001 | asunto              | El asunto debe estar relleno                 | Siempre   | "El asunto es obligatorio."                       |
| V-002 | cuerpoHtml          | El cuerpo del correo debe estar relleno      | Siempre   | "El cuerpo del correo es obligatorio."            |
| V-003 | destinatarioEmail   | El email del destinatario debe estar relleno | Siempre   | "El email del destinatario es obligatorio."       |
| V-004 | destinatarioEmail   | El email debe tener formato `usuario@dominio.com` | Siempre | "El email '{email}' no tiene un formato válido." |
| V-005 | destinatarioDni     | El DNI/NIE debe tener formato válido         | Si no es nulo | "El DNI/NIE '{dni}' no tiene un formato válido."|

## Acciones

| Operación           | Cuándo se permite                                                   | Validaciones que aplican | Reglas que dispara |
|---------------------|---------------------------------------------------------------------|--------------------------|--------------------|
| Crear (insert)      | Siempre (manual desde la pantalla admin u otros sistemas)           | V-001..V-005             | R-001              |
| Modificar (update)  | Nunca — el correo es inmutable una vez creado                       | —                        | —                  |
| Borrar (remove)     | Nunca — el correo es inmutable una vez creado                       | —                        | —                  |
| Reintentar envío    | Solo si estado = FALLADO                                            | —                        | R-002              |
| Procesar pendientes | Solo si estado = PENDIENTE (lo ejecuta el proceso periódico)        | —                        | R-003, R-004       |

## Reglas de negocio (R-XXX)

| ID    | Descripción                                                                                          | Entidad     | Método              | Momento | Más información                                                       |
|-------|------------------------------------------------------------------------------------------------------|-------------|---------------------|---------|-----------------------------------------------------------------------|
| R-001 | Inicializa estado=PENDIENTE, fechaCreacion=ahora, numIntentos=0 y "de"=remitente SMTP configurado    | TareaCorreo | insert              | Antes   | Valores fijados por el sistema; el usuario nunca los elige            |
| R-002 | Vuelve a poner el correo en estado PENDIENTE para que el proceso periódico lo reintente              | TareaCorreo | Reintentar envío    | Antes   | Escribe sobre el propio registro                                      |
| R-003 | Intenta el envío SMTP; si sale bien, marca estado=ENVIADO y fechaEnvioOk=ahora                       | TareaCorreo | Procesar pendientes | —       | Solo proceso periódico; caso de éxito                                 |
| R-004 | Si el envío falla, marca estado=FALLADO, incrementa numIntentos, actualiza fechaUltimoIntento y logErrores | TareaCorreo | Procesar pendientes | —       | Solo proceso periódico; caso de fallo                                 |
