---
type: user-story
---

# Enviar correos

Se necesita la funcionalidad de envío y registro de correos electrónicos para que los usuarios puedan consultar los correos que se les han enviado, y para que los administradores y supervisores puedan controlar la actividad de envío de correos en el sistema. 
Esta iniciativa resuelve el problema de falta de visibilidad sobre los correos enviados, lo que puede generar confusión y reclamaciones por parte de los usuarios. 
Encaja en el contexto de la secretaría virtual como una funcionalidad transversal que afecta a múltiples tipos de usuarios y casos de uso relacionados con la comunicación por correo electrónico.

## En una frase

**Como** Administrativa
**quiero** poder ver los correos que se han enviado a una persona de mi centro
**para** que no me digan que no les ha llegado ningún correo y pueda comprobar qué se les ha enviado.

**Como** Supervidor del centro
**quiero** poder ver los correos que se han enviado en el centro
**para** controlar todo lo que ocurre en mi centro

**Como** Administrador
**quiero** poder ver los correos que se han enviado en todo el sistema
**para** controlar todo lo que ocurre en el sistema

**Como** Administrador
**quiero** poder ver una gráfica con los correos que se han enviado en el sistema a lo largo de dos fechas
**para** controlar todo lo que ocurre en el sistema

**Como** Administrador
**quiero** poder enviar un correo y que quede registrado
**para** poder enviar correos a quien quiera y que quede constancia de ello

**Como** Administrador
**quiero** poder reenviar un correo si ha fallado el envío 
**para** poder asegurarme de que el correo llega a su destinatario

**Como** Administrador
**quiero** que al enviar un correo no tenga que esperar hasta que finalmente se envíe, sino que el sistema lo procese de forma asíncrona y me permita seguir trabajando sin interrupciones
**para** no perder tiempo esperando a que se envíe el correo, especialmente si el envío falla y hay que reintentar


**Como** Profesor o alumno o exalumno o exprofesor o familiar
**quiero** poder saber los correos que me han enviado
**para** poder comprobar qué me han enviado y no decir que no me ha llegado ningún correo



## Quién interviene

- **Administrador**: Verlo todo, sin filtro. Puede ver el contenido completo de cada correo, los destinatarios, la fecha de envío, los adjuntos, etc. Puede enviar correos y que quede constancia de ello. También reenviar correos si falla el envío 
- **Supervisor del centro**: Ver lo que se ha enviado en su centro, sin filtro. Puede ver el contenido completo de cada correo, los destinatarios, la fecha de envío, los adjuntos, etc. No puede enviar correos.
- **Administrativa**: Lo mismo que el Supervisor del centro
- **Profesore o alumnos**: Ven lo que se les ha enviado



## Conceptos y datos clave

- **TareaCorreo**: Se genera cada vez que se envía un correo. Contiene el contenido del correo, destinatarios, fecha de envío, adjuntos, etc.
- **Estado del correo**: Cada tarea de correo tiene un estado PENDIENTE, ENVIANDO, ENVIADO, FALLADO Este estado se actualiza automáticamente
- **Referenciar a los destinatarios**: siempre se hace por DNI porque puede que no exista el usuario en el sistema o que exista a futuro.
- **Gráfica de correos enviados**: Un gráfico que muestra el número de correos enviados en el sistema a lo largo del tiempo, con filtros por fecha. Solo accesible para administradores.
- **Adjuntos**: Si el correo tiene adjuntos, se guarda una copia de los mismos.
- **Referencia a expedientes**: Debe ser opcional pero si el correo está relacionado con un expediente concreto, debe guardarse una referencia a ese expediente es el estado concreto para ello se referenciará a HistorialEstado


## Fuera de alcance (opcional)

Enviar finalmente el correo desde Java ya está implementado en el módulo de infraestructura `base/infrastructure/mail` y no es parte de esta iniciativa. Esta iniciativa se centra en la creación de la entidad `TareaCorreo`, su registro inmutable, la gestión de estados y la visibilidad de los correos para los usuarios, pero no en la implementación del envío SMTP en sí.

## Restricciones que no pueden romperse

- Es importante que nadie vea correos que no le correspondan: cada usuario solo puede ver lo que se le ha enviado a él (o a su centro, en el caso de supervisores), pero no lo que se ha enviado a otros usuarios ni a otros centros.
- No se pueden modificar ni borrar las tareas de correo una vez creadas — son registros históricos inmutables.
- El contenido del correo que se registra debe ser exactamente el que se ha enviado, sin modificaciones posteriores.





