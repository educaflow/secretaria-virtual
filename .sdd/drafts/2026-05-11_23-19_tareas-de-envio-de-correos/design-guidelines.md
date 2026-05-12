---
type: design-guidelines
---

En el diseño hay una cosa a tener en cuenta como crear un nuevo hilo para enviar el correo y no bloquear el hilo principal. 
Además se debe tener en cuenta que este código se ejecuta desde Tomcat y que necesitas al enviar el correo marcarlo como enviado o como error en el envio por lo que debes poder acceder al servicio o al repositorio para cambiar el estado
¿Has pensado como funciona JPA en caso de nuevos hilos?. 