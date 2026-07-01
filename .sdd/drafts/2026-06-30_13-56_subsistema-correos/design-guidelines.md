---
type: design-guidelines
---

- Es un **subsistema** nuevo: `com.educaflow.subsystem.correos`.
- Reutilizar la infraestructura de correo ya existente en `base/infrastructure/mail` (`MailSender`, `Mail`, `Attach`) para el envío real; no implementar un cliente SMTP propio.
- El envío del correo debe hacerse de forma **asíncrona** dentro de Tomcat para no bloquear la petición que lo origina. Analizar el mejor mecanismo en Java dentro de Tomcat (p. ej. un `ExecutorService` gestionado, en lugar de crear hilos a mano), eligiendo una opción robusta para el ciclo de vida de la aplicación. Debes dedicarle un tiempo a pensar la mejor opción para ésto. Aunque el volumen de correos va a ser bajo así que el rendimiento no es prioritario aunque lo importante es que no haya leaks de memoria.
- El servicio debe ofrecer una función a la que se le pasa el **id de un correo** y lo envía por primera vez o lo reenvía (la misma operación sirve para envío inicial y reintento), actualizando estado, fechas (`fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio`), número de reintentos y descripción del último fallo según el resultado.
- El servicio debe ofrecer una función que **devuelva todos los correos en estado FAIL** (pensada para poder reenviarlos en bloque más adelante).
- El subsistema debe poder invocarse **programáticamente** desde otras partes de la aplicación para crear y enviar un correo, además de desde las pantallas.
- La `descripción del último fallo` guarda la **traza de la excepción Java** del último intento fallido.
- Multicentro / IDOR: las comprobaciones de que el centro de un correo (al crear) y el centro del correo (al reenviar/añadir adjunto) pertenecen al usuario son **defensas de servidor**, no solo de UI; el Administrador es el único rol que puede operar sobre cualquier centro. Aplicar el skill `k-secure-coding` (mass-assignment vía `AllowProperties`, restauración de campos inmutables, multicentro).
- Existe ya una propiedad de configuración `correos.envio.cron` en `axelor-config.properties` preparada para un job de reenvío periódico; **no** implementar ese job ahora (está fuera de alcance).
