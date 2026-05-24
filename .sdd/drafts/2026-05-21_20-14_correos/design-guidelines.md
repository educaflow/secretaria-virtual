---
type: design-guidelines
---

Los valores del usuario, contraseña, servidor smpt se encuentran en el fichero de configuración de la aplicación

Para enviar los correos no se hace directamente cuando se crea la TareaCorreo sino que habrá un scheduler que cada minuto va a enviar correos.
Para saber como va el schedule se usa el skill de /k-scheduler El scheduler se encarga de ejecutar tareas cada cierto tiempo. Para esto se le asigna una tarea que se llama "Enviar Correos" y se le asigna un cron de cada minuto.
El scheduler debe cambiar el estado de la TareaCorreo según lo que ocurra.

Va a haber un provider de guice que se encargará de crear el MailSender con las credenciales necesarias para enviar los correos. 
```java
String host = AppSettings.get().get("mail.smtp.host");
String user = AppSettings.get().get("mail.smtp.user");
String pass = AppSettings.get().get("mail.smtp.password");

SmtpCredentialSimplePassword smtpCredentialSimplePassword = new SmtpCredentialSimplePassword(host, user, pass);

MailSender mailSender= new MailSenderImpl(smtpCredentialSimplePassword);
```

El `from` de los correos es el mismo que el `user` para enviar correos