# Modelo: Sms

Un SMS que la aplicación envía a una persona: a quién va (DNI, nombre y apellidos), a qué móvil, con qué texto, desde qué centro y, opcionalmente, en qué estado de un expediente se envió, junto con el resultado del envío. Es el equivalente, para SMS, del correo del subsistema de correos, sin asunto, sin destinatarios en copia y sin adjuntos. Se crea (a mano por el Administrador o automáticamente por un trámite u otro subsistema), el sistema intenta enviarlo justo después de crearlo y queda Enviado o Fallido; un SMS Fallido se puede reenviar. Una vez creado, nunca se modifica ni se borra: es el registro de lo que se ha enviado.

## Campos

- **DNI del destinatario** — documento de identidad de la persona a la que va el SMS; es el que decide quién lo ve en «Mis SMS». Es texto libre: no enlaza con la ficha de ningún usuario.
- **nombre** — nombre de la persona destinataria.
- **apellidos** — apellidos de la persona destinataria.
- **teléfono** — el móvil de España al que se envía el SMS, guardado siempre en formato internacional con el prefijo del país (por ejemplo, «+34600111222»).
- **mensaje** — el texto del SMS.
- **centro** — el centro que envía el SMS; decide qué Supervisores y Administrativos lo ven.
- **estado del expediente** — opcional: el estado de un expediente en el que se encontraba ese expediente cuando se envió el SMS.
- **nombre del expediente** — el nombre del expediente al que pertenece el estado del expediente indicado.
- **estado** — el resultado del envío (ver «Estados y transiciones»).
- **fecha de creación** — cuándo se dio de alta el SMS.
- **fecha del primer intento de envío** — cuándo se intentó enviar por primera vez.
- **fecha del último intento de envío** — cuándo se intentó enviar por última vez.
- **fecha de envío** — cuándo el proveedor de SMS aceptó el envío.
- **número de reintentos** — cuántos intentos de envío se han hecho.
- **descripción del último fallo** — el detalle del error del último intento de envío que falló.

## Estados y transiciones

- Estado inicial: PENDIENTE («Pendiente»), al darse de alta.
- PENDIENTE → ENVIADO («Enviado»): el proveedor de SMS acepta el envío.
- PENDIENTE → FALLIDO («Fallido»): el proveedor de SMS rechaza el envío o no se puede contactar con él.
- FALLIDO → ENVIADO: tras pulsar «Reenviar», el nuevo intento lo acepta el proveedor de SMS.
- FALLIDO → FALLIDO: tras pulsar «Reenviar», el nuevo intento vuelve a fallar.
- ENVIADO: terminal; un SMS enviado no se vuelve a enviar.

## Restricciones

- RES-Sms-001 — Un SMS no se puede modificar una vez creado
  - mensaje: "El SMS es inmutable tras su creación."
- RES-Sms-002 — Un SMS no se puede borrar
  - mensaje: "Los SMS no se pueden borrar."
- RES-Sms-003 — La fecha de envío solo tiene valor cuando el SMS está en estado ENVIADO
- RES-Sms-004 — El teléfono está siempre guardado en formato internacional con el prefijo de España («+34» seguido de los 9 dígitos)

## Campos calculados

- CC-Sms-001 — estado
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: PENDIENTE al darse de alta; ENVIADO si el proveedor de SMS acepta un intento de envío; FALLIDO si un intento de envío falla
- CC-Sms-002 — fecha de creación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: el momento en que se da de alta el SMS
- CC-Sms-003 — fecha del primer intento de envío
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: vacía al darse de alta; el momento del primer intento de envío, que ya no cambia en los reintentos
- CC-Sms-004 — fecha del último intento de envío
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: vacía al darse de alta; el momento de cada intento de envío, incluidos los reintentos
- CC-Sms-005 — número de reintentos
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: 0 al darse de alta; suma 1 en cada intento de envío
- CC-Sms-006 — fecha de envío
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: vacía al darse de alta y cuando un intento falla; el momento en que el proveedor de SMS acepta el envío
- CC-Sms-007 — descripción del último fallo
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: vacía al darse de alta y cuando el envío se acepta; el detalle completo del error cuando un intento de envío falla
- CC-Sms-008 — nombre del expediente
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: el nombre del expediente al que pertenece el estado del expediente del SMS; vacío si el SMS no tiene estado del expediente

## Acción: Crear

**Input AllowProperties:** DNI del destinatario, nombre, apellidos, teléfono, mensaje, centro, estado del expediente

**Validaciones:**

- VAL-Sms-001 — El DNI del destinatario está indicado
  - mensaje: "El DNI del destinatario es obligatorio"
- VAL-Sms-002 — El DNI del destinatario es válido (su letra corresponde a su número)
  - condición: el DNI del destinatario está indicado
  - mensaje: "El DNI del destinatario no es válido; compruebe la letra"
- VAL-Sms-003 — El nombre está indicado
  - mensaje: "El nombre es obligatorio"
- VAL-Sms-004 — Los apellidos están indicados
  - mensaje: "Los apellidos son obligatorios"
- VAL-Sms-005 — El teléfono está indicado
  - mensaje: "El teléfono es obligatorio"
- VAL-Sms-006 — El teléfono es un número de móvil de España válido; si se escribe sin prefijo de país se entiende que es de España
  - condición: el teléfono está indicado
  - mensaje: "El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)"
- VAL-Sms-007 — El mensaje está indicado
  - mensaje: "El mensaje es obligatorio"
- VAL-Sms-008 — El mensaje cabe en un solo SMS: si todos sus caracteres son del alfabeto básico de SMS (GSM-7), ocupa como máximo 160 unidades, contando como 2 unidades cada símbolo de la tabla extendida de ese alfabeto (como «€», «[», «]», «{», «}», «~», «^», «|» o «\»); si alguno no lo es (por ejemplo, «á», «ó» o «ú»), el mensaje ocupa como máximo 70 caracteres
  - condición: el mensaje está indicado
  - mensaje: "El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales"
- VAL-Sms-009 — El centro está indicado
  - mensaje: "El centro es obligatorio"
- VAL-Sms-010 — El centro es uno de los centros del usuario que crea el SMS
  - condición: el usuario que crea el SMS no es Administrador
  - mensaje: "No puede crear SMS para un centro que no es suyo"
- VAL-Sms-011 — El estado del expediente indicado existe
  - condición: el estado del expediente está indicado
  - mensaje: "El estado del expediente indicado no existe"

**Reglas de negocio:**

- RN-Sms-001 — Guardar el teléfono en formato internacional con el prefijo de España (por ejemplo, «600111222» se guarda como «+34600111222»)
  - fase: antes_de_commit
- RN-Sms-002 — Intentar enviar el SMS al proveedor de SMS en segundo plano, sin que quien lo crea tenga que esperar: en cada intento se actualizan las fechas del primer y del último intento y se suma un reintento; si el proveedor lo acepta, el SMS pasa a ENVIADO con su fecha de envío y sin descripción de fallo; si falla, pasa a FALLIDO con la descripción del fallo y sin fecha de envío
  - fase: después_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — el SMS es inmutable tras su creación)

## Acción: Reenviar

**Validaciones:**

- VAL-Sms-012 — El SMS está en estado FALLIDO
  - mensaje: "Solo se pueden reenviar SMS que han fallado"
- VAL-Sms-013 — El centro del SMS es uno de los centros del usuario que lo reenvía
  - condición: el usuario que lo reenvía no es Administrador
  - mensaje: "No puede reenviar SMS de un centro que no es suyo"

**Reglas de negocio:**

- RN-Sms-003 — Volver a intentar el envío del SMS al proveedor de SMS en segundo plano, de la misma forma que tras el alta (RN-Sms-002)
  - fase: después_de_commit
