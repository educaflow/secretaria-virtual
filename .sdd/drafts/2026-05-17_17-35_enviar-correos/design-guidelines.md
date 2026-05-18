---
type: design-guidelines
---

Los valores del usuario, contraseña, servidor smpt se encuentran en el fichero de configuración de la aplicación, y se pueden modificar fácilmente para cambiar el servidor de correo o las credenciales.

Para enviar los correos no se hace directamente cuando se crea la TareaCorreo sino que habrá un scheduler que cada minuto va a enviar correos.
Para saber como va el schedule se usa el skill de /k-scheduler El scheduler se encarga de ejecutar tareas cada cierto tiempo. Para esto se le asigna una tarea que se llama "Enviar Correos" y se le asigna un cron de cada minuto.
El scheduler debe cambiar el estado de la TareaCorreo según lo que haga.

Va a haber una clase llamada 
```java
    public MailSender MailSenderProvider() {
        String host = AppSettings.get().get("mail.smtp.host");
        String user = AppSettings.get().get("mail.smtp.user");
        String pass = AppSettings.get().get("mail.smtp.password");

        SmtpCredentialSimplePassword smtpCredentialSimplePassword = new SmtpCredentialSimplePassword(host, user, pass);

        return new MailSenderImpl(smtpCredentialSimplePassword);
    }
```
Esta clase se podrá injectar usando Guice en el Scheduler para enviar los correos. De esta forma el código queda organizado y fácil de mantener, y si en el futuro queremos cambiar la forma de enviar los correos (por ejemplo, usando una API externa en lugar de SMTP) solo tendríamos que modificar esta clase sin afectar al resto del código.