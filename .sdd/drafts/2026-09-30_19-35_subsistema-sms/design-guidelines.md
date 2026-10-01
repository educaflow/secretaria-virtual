---
type: design-guidelines
---

- El subsistema se llama `sms` (`subsystem/sms`) y MUST ser prácticamente igual que `subsystem/correos`: mirar cómo está hecho (dominio, servicio con envío asíncrono tras el commit, reenvío, controlador, vistas `Main`/`Centro`/`Mis`, permisos `auth-*.xml` del `data-init`, menú) y replicarlo, cambiando solo los campos que difieren (sin asunto, cc, bcc ni adjuntos; con teléfono y mensaje).
- El envío usa la infraestructura ya existente en `base/infrastructure/sms` (`SmsSender`, `Sms`, `TwilioCredential`, `SmsSenderFactory.getTwilioSmsSender`).
- Propiedades de configuración nuevas, en `src/main/resources/axelor-config.properties` (vacías; los valores reales van en la config privada): `sms.credentials.twilio.accountSid=`, `sms.credentials.twilio.authToken=` y `sms.twilio.from=`.
- Crear en `base.util` la clase `NumeroTelefono`, que recibe el teléfono como `String` en el constructor y tiene los métodos que hagan falta (validar, saber si es móvil, saber si es de España, obtener el formato E.164…), apoyándose en la librería `com.googlecode.libphonenumber:libphonenumber` (https://github.com/google/libphonenumber). País por defecto: España.
- `libphonenumber` solo la puede usar la clase `NumeroTelefono`: ninguna otra clase del proyecto la importa, y los métodos públicos de `NumeroTelefono` (parámetros, tipos de retorno y excepciones) MUST NOT exponer ningún tipo de `libphonenumber` (ni `PhoneNumber`, ni `PhoneNumberUtil`, ni `PhoneNumberType`, ni `NumberParseException`…). El objetivo es abstraerse de la librería subyacente para poder cambiarla sin tocar el resto del código.
- El teléfono se guarda siempre en formato E.164 (p. ej. `+34600111222`).
- En las vistas Axelor el teléfono usa el widget `phone` con solo España en el desplegable de países: `<field name="telefono" widget="phone" x-only-countries="ES"/>`.
- Algoritmo para saber si el mensaje cabe en un SMS (con un máximo de una parte, por lo que la concatenación no se usa): (1) comprobar si TODO el texto puede codificarse en GSM-7; (2) si sí, contar unidades GSM-7 (los caracteres de la tabla extendida cuentan 2) con límite 160 (153 si hubiera concatenación); (3) si no, codificación UCS-2 con límite 70 (67 si hubiera concatenación).
