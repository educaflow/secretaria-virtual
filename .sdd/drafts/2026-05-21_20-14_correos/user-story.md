---
type: user-story
---

# Correos



Quiero que al enviar un correo quede registrado en la base de datos. Realmente lo que quiero es "añadir" una nueva fila a la base de datos y eso implique enviar un correo. 
Esto es para poder tener un registro de los correos que se han enviado.

## En una frase

**Como** Administrativa
**quiero** poder ver los correos que se han enviado a una persona de mi centro
**para** que no me digan que no les ha llegado ningún correo y pueda comprobar qué se les ha enviado.

**Como** Supervidor del centro
**quiero** poder ver los correos que se han enviado a una persona de mi centro
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

- **Correo**: Se genera cada vez que se envía un correo. Contiene el contenido del correo, destinatarios, fecha de envío, adjuntos, etc.
- **AdjuntoCorreo**: Se genera (si hay adjunto) cada vez que se envía un correo. Contiene uno de los adjuntos del correo, con una copia persistente del fichero adjunto en el momento de crear el correo, de modo que el adjunto registrado nunca cambia aunque el fichero original sea modificado o borrado.
- **Estado del correo**: Cada tarea de correo tiene un estado PENDIENTE, ENVIADO, FALLIDO Este estado se actualiza automáticamente PENDIENTE →  ENVIADO o FALLIDO y FALLIDO →  ENVIADO o FALLIDO
- **Referenciar a los destinatarios**: siempre se hace por DNI porque puede que no exista el usuario en el sistema o que exista a futuro.
- **Gráfica de correos enviados**: Un gráfico barras que muestra el número de correos enviados en el sistema a lo largo del tiempo, con filtros por fecha. Solo accesible para administradores. Debe poder mostrar apilados los 3 estados.
- **Referencia a HistorialEstado**: Debe ser opcional pero si el correo está relacionado con un expediente concreto, debe guardarse una referencia al HistorialEstado del expediente


## Fuera de alcance (opcional)

Enviar finalmente el correo desde Java ya está implementado en el módulo de infraestructura `base/infrastructure/mail` y no es parte de esta iniciativa. Esta iniciativa se centra en la creación de la entidad `TareaCorreo`, su registro inmutable, la gestión de estados y la visibilidad de los correos para los usuarios, pero no en la implementación del envío SMTP en sí.


## Restricciones que no pueden romperse

- Los datos del envío no pueden modificarse una vez creado el envio aunque si que puede modificarse el estado del envío o el número de intentos ,etc.

## Preguntas abiertas (opcional)

- ¿Como se guarda la información de los reenvíos? ¿La fecha de cada reintento o solo el número de reintentos?
- ¿Se guarda el motivo del fallo en caso de que el envío falle? Y si el reenvío también falla, ¿se guarda el motivo de cada fallo o solo el del último intento?