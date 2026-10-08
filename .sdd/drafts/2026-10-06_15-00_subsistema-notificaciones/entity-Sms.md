# Modelo: Sms

Una notificación cuyo canal es el SMS a un móvil de España.
Es una clase de **Notificacion**: tiene todos sus campos, estados, restricciones, campos calculados y reglas, y añade el teléfono y el mensaje.
Lo da de alta a mano el administrador. Cada SMS enviado (también cada reenvío) tiene coste. Una vez creado no se modifica ni se borra; solo se reenvía si ha fallado.

## Campos

- **teléfono** — el móvil de España al que se envía; se guarda siempre en formato internacional (por ejemplo, +34600111222)
- **mensaje** — el texto que recibe el destinatario; tiene que caber en un único SMS

Además, los heredados de Notificacion: tipo de notificación, motivo, DNI del destinatario, nombre, apellidos, destino, centro, estado del expediente, expediente, estado, fechas del envío, número de reintentos y descripción del último fallo.

## Restricciones

- RES-Sms-001 — El tipo de notificación de un SMS es siempre «SMS»
- RES-Sms-002 — El teléfono guardado está siempre en formato internacional

## Campos calculados

- CC-Sms-001 — destino
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: el teléfono, en formato internacional

## Acción: Crear

**Input AllowProperties:** motivo, DNI del destinatario, nombre, apellidos, teléfono, mensaje, centro, estado del expediente

**Validaciones:**

- VAL-Sms-002 — El DNI del destinatario es un DNI válido (su letra corresponde al número)
  - condición: el DNI del destinatario está indicado
  - mensaje: "El DNI del destinatario no es válido; compruebe la letra"
- VAL-Sms-003 — El nombre está indicado
  - mensaje: "El nombre es obligatorio"
- VAL-Sms-004 — Los apellidos están indicados
  - mensaje: "Los apellidos son obligatorios"
- VAL-Sms-005 — El teléfono está indicado
  - mensaje: "El teléfono es obligatorio"
- VAL-Sms-006 — El teléfono es un número de móvil de España (escrito con o sin prefijo internacional, con o sin espacios)
  - condición: el teléfono está indicado
  - mensaje: "El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)"
- VAL-Sms-007 — El mensaje está indicado (un mensaje solo con espacios cuenta como vacío)
  - mensaje: "El mensaje es obligatorio"
- VAL-Sms-008 — El mensaje cabe en un único SMS: como máximo 160 caracteres si solo usa los caracteres básicos, o 70 si contiene acentos u otros caracteres especiales
  - condición: el mensaje está indicado
  - mensaje: "El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales"
- VAL-Sms-010 — El centro es uno de los centros del usuario
  - condición: el centro está indicado y el usuario no es administrador
  - mensaje: "No puede crear SMS para un centro que no es suyo"
- VAL-Sms-011 — El nombre no supera 255 caracteres
  - condición: el nombre está indicado
  - mensaje: "El nombre no puede superar 255 caracteres"
- VAL-Sms-012 — Los apellidos no superan 255 caracteres
  - condición: los apellidos están indicados
  - mensaje: "Los apellidos no pueden superar 255 caracteres"

(Se aplican además las restricciones de Notificacion: el motivo obligatorio y de 255 caracteres como máximo, el centro y el DNI del destinatario obligatorios, y el estado del expediente, si se indica, del mismo centro.)

**Reglas de negocio:**

- RN-Sms-001 — Guardar el teléfono en formato internacional (p. ej. «600111222» o «+34 600 111 222» se guardan como «+34600111222»)
  - fase: antes_de_commit
- RN-Sms-002 — Al enviarse, el SMS sale al teléfono con el mensaje a través del proveedor de SMS configurado en la instalación; si la instalación no tiene configurado el proveedor, el envío falla y el SMS queda FALLIDO
  - fase: después_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — un SMS es inmutable tras su creación)
