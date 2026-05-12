---
type: user-story
---

# Tareas de envio de correos

La tarea de envio permite generar un "log" con todos los correos que se han enviado a los usuarios, incluyendo el contenido completo del correo, el destinatario, la fecha de envío, los adjuntos etc. 
Esto es útil sobre todo para cuando llama un usuario como un alumno o un titulado y dice que no le ha llegado ningún correo.

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

**Como** Profesor o alumno o exalumno o exprofesor o familiar
**quiero** poder saber los correos que me han enviado
**para** poder comprobar qué me han enviado y no decir que no me ha llegado ningún correo

## Quién interviene

- **Administrador**: Verlo todo, sin filtro. Puede ver el contenido completo de cada correo, los destinatarios, la fecha de envío, los adjuntos, etc. Puede enviar correos y que quede constancia de ello.
- **Supervisor del centro**: Ver lo que se ha enviado en su centro, sin filtro. Puede ver el contenido completo de cada correo, los destinatarios, la fecha de envío, los adjuntos, etc. No puede enviar correos.
- **Administrativa**: Ver lo que se ha enviado a una persona concreta de su centro, filtrado por esa persona. Puede ver el contenido completo de cada correo, los destinatarios, la fecha de envío, los adjuntos, etc. No puede enviar correos.
- **Profesore o alumnos**: Ven lo que se les ha enviado


## Conceptos y datos clave

- **TareaCorreo**: Se genera cada vez que se envía un correo. Contiene el contenido del correo, destinatarios, fecha de envío, adjuntos, etc.
- **Estado del correo**: Cada tarea de correo tiene un estado que indica si el correo se ha enviado correctamente, si ha fallado, etc. Este estado se actualiza automáticamente según el resultado del envío.
- **Referenciar a los destinatarios**: siempre se hace por DNI porque puede que no exista el usuarioen el sistema o que exista a futuro.
- **Gráfica de correos enviados**: Un gráfico que muestra el número de correos enviados en el sistema a lo largo del tiempo, con filtros por fecha. Solo accesible para administradores.
- **Adjuntos**: Si el correo tiene adjuntos, se guarda una copia de los mismos.
- *+Referencia a expedientes**: Debe ser opcional pero si el correo está relacionado con un expediente concreto, debe guardarse una referencia a ese expediente para poder ver los correos enviados desde el expediente


## Qué tiene que pasar

1. Debe existir un menú diferente para cada tipo de usuario: "Mis correos" para profesores, alumnos, etc. "Correos del centro" para supervisores, "Todos los correos" para administradores. 
2. Desde código debe ser posible facilmente crear una tarea de correo, indicando el contenido del correo, destinatarios, fecha de envío, adjuntos, etc.
3. Los administradores o los supervisores debe poder renviar un correo si este había fallado. Hay que almacenar la fecha del correo si tiene existo , la fecha del útlimo intento de envío, el número de intentos, etc. para poder controlar esto.


## Fuera de alcance (opcional)




## Restricciones que no pueden romperse

- Es importante que nadie vea correos que no le correspondan: cada usuario solo puede ver lo que se le ha enviado a él (o a su centro, en el caso de supervisores), pero no lo que se ha enviado a otros usuarios ni a otros centros.
- No se pueden modificar ni borrar las tareas de correo una vez creadas — son registros históricos inmutables.
- El contenido del correo que se registra debe ser exactamente el que se ha enviado, sin modificaciones posteriores.

## Lo que aporta valor

- Que sea sencillo saber que se ha enviado
- Poder gestionar el fallo de los envíos y volver a intentarlo


## Preguntas abiertas (opcional)

- ¿Debería guardarse cada uno de los intentos de envío?
- ¿Debería guardase el fallo concreto que ha ocurrido en cada intento de envío o solo el último?
- ¿La referencia debería ser al expediente o al historial de expediente? 
