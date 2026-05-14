---
type: design-guidelines
---

Para enviar los correos no se hace directamente cuando se crea la TareaCorreo sino que habrá un scheduler que cada minuto va a enviar correos.
Para saber como va el schedule se usa el skill de /k-scheduler El scheduler se encarga de ejecutar tareas cada cierto tiempo. Para esto se le asigna una tarea que se llama "Enviar Correos" y se le asigna un cron de cada minuto.
El scheduler debe cambiar el estado de la TareaCorreo según lo que haga.

La última duda es donde va a ir este código:
```java
    public MailSender provideMailSender() {
        String host = AppSettings.get().get("mail.smtp.host");
        String user = AppSettings.get().get("mail.smtp.user");
        String pass = AppSettings.get().get("mail.smtp.password");

        SmtpCredentialSimplePassword smtpCredentialSimplePassword = new SmtpCredentialSimplePassword(host, user, pass);

        return new MailSenderImpl(smtpCredentialSimplePassword);
    }
```
Podria haber un MailSenderProvider registrado en Guice que se encargue de esto, o podria ser parte del Scheduler. Lo importante es que el código quede organizado y fácil de mantener. Piensa alternativas antes de decidir y pregunta si hay dudas.