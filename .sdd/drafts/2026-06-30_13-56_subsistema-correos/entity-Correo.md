# Modelo: Correo

Representa un correo electrónico dirigido a una persona, junto con el resultado de su envío. Es el registro que deja **constancia permanente** de toda comunicación por correo de la secretaría virtual: guarda a quién va (por su DNI), su contenido y el rastro de cada intento de envío. Nace en estado PENDIENTE cuando se crea y, tras intentar enviarse, pasa a SUCCESS (enviado) o FAIL (fallido); un correo en FAIL puede reintentarse. Una vez creado, sus datos de contenido son **inmutables**: solo cambian los campos relativos al resultado del envío, que fija el servidor. Un correo **nunca se borra**.

## Campos

- **dni del destinatario** — el DNI de la persona a la que va dirigido el correo. Es un dato de texto; no enlaza con ninguna ficha de persona, porque el correo puede dirigirse a alguien que aún no está dado de alta.
- **nombre** — el nombre de pila de la persona a la que va dirigido el correo. Es un dato de texto que teclea quien crea el correo; no enlaza con ninguna ficha de persona.
- **apellidos** — los apellidos de la persona a la que va dirigido el correo. Es un dato de texto que teclea quien crea el correo; no enlaza con ninguna ficha de persona.
- **para** — las direcciones de correo electrónico de los destinatarios principales (el «to»).
- **en copia** — las direcciones de correo en copia (el «cc»).
- **en copia oculta** — las direcciones de correo en copia oculta (el «bcc»).
- **asunto** — el asunto del correo.
- **cuerpo** — el contenido del mensaje.
- **centro** — el centro al que pertenece el correo.
- **historial de estado** — referencia opcional al estado del expediente en el que se encontraba el correo cuando se envió. Puede no tener valor.
- **adjuntos** — los ficheros adjuntos del correo.
- **estado** — la situación del envío del correo (ver «Estados y transiciones»).

## Estados y transiciones

- Estado inicial: PENDIENTE (al crearse el correo en la base de datos, antes de intentar enviarlo).
- PENDIENTE → SUCCESS: cuando se consigue enviar el correo.
- PENDIENTE → FAIL: cuando el intento de envío falla.
- FAIL → SUCCESS: cuando un reintento consigue enviar el correo.
- FAIL → FAIL: cuando un reintento vuelve a fallar (el correo permanece en FAIL).
- SUCCESS es terminal: un correo enviado con éxito ya no se reintenta ni cambia de estado.

## Restricciones

- RES-Correo-001 — El centro de un correo siempre referencia a un centro existente.
- RES-Correo-002 — La fecha de envío solo tiene valor cuando el estado es SUCCESS; en PENDIENTE y FAIL está vacía.
- RES-Correo-003 — Un correo nunca se puede borrar.

## Campos calculados

- CC-Correo-001 — fecha de creación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: el instante en que se crea el correo en la base de datos.
- CC-Correo-002 — fecha del primer intento de envío
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: el instante del primer intento de envío; se fija una sola vez, en el primer intento.
- CC-Correo-003 — fecha del último intento de envío
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: el instante del último intento de envío; se actualiza en cada intento (incluidos los reintentos).
- CC-Correo-004 — fecha de envío
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: el instante en que el correo se consigue enviar y pasa a SUCCESS; vacía mientras no se haya enviado.
- CC-Correo-005 — número de reintentos
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: empieza en 0; se incrementa en 1 con cada intento de envío.
- CC-Correo-006 — descripción del último fallo
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: el detalle del error del último intento de envío que falló; vacía si nunca ha fallado.
- CC-Correo-007 — nombre del expediente
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: el nombre del expediente al que pertenece el historial de estado referenciado por el correo; vacío si el correo no tiene historial de estado.

## Acción: Crear

**Input AllowProperties:** dni del destinatario, nombre, apellidos, para, en copia, en copia oculta, asunto, cuerpo, centro, historial de estado, adjuntos

**Validaciones:**

- VAL-Correo-001 — El DNI del destinatario está indicado
  - mensaje: "El DNI del destinatario es obligatorio"
- VAL-Correo-002 — Hay al menos una dirección en el «para»
  - mensaje: "Debe indicar al menos un destinatario en el «para»"
- VAL-Correo-003 — El asunto está indicado
  - mensaje: "El asunto es obligatorio"
- VAL-Correo-004 — El cuerpo está indicado
  - mensaje: "El cuerpo es obligatorio"
- VAL-Correo-005 — El centro está indicado
  - mensaje: "El centro es obligatorio"
- VAL-Correo-006 — El nombre está indicado
  - mensaje: "El nombre es obligatorio"
- VAL-Correo-007 — Los apellidos están indicados
  - mensaje: "Los apellidos son obligatorios"
- VAL-Correo-008 — El centro indicado es uno de los centros a los que pertenece el usuario
  - actor: cualquier rol distinto de Administrador (el Administrador puede elegir cualquier centro)
  - mensaje: "No puede crear correos para un centro que no es suyo"
- VAL-Correo-011 — Cada dirección indicada en el «para» tiene formato de correo electrónico válido
  - mensaje: "El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)"
- VAL-Correo-012 — Si se indica «en copia», cada dirección tiene formato de correo electrónico válido
  - condición: cuando «en copia» tiene valor
  - mensaje: "El «en copia» debe contener direcciones de correo válidas"
- VAL-Correo-013 — Si se indica «en copia oculta», cada dirección tiene formato de correo electrónico válido
  - condición: cuando «en copia oculta» tiene valor
  - mensaje: "El «en copia oculta» debe contener direcciones de correo válidas"
- VAL-Correo-014 — Si se indica el historial de estado, referencia un historial de estado de expediente existente
  - condición: cuando «historial de estado» tiene valor
  - mensaje: "El historial de estado indicado no existe"
- VAL-Correo-015 — El DNI del destinatario tiene formato válido y dígito de control correcto (DNI o NIE)
  - mensaje: "El DNI del destinatario no es válido; compruebe la letra"
- VAL-Correo-016 — El asunto no supera los 255 caracteres
  - mensaje: "El asunto no puede superar 255 caracteres"

**Reglas de negocio:**

- RN-Correo-001 — Tras crear el correo (estado PENDIENTE), intentar enviarlo de forma asíncrona y, según el resultado, pasarlo a SUCCESS (registrando la fecha de envío) o a FAIL (guardando la descripción del fallo), actualizando la fecha del primer y del último intento y el número de reintentos.
  - fase: después_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — los datos de un correo son inmutables una vez creado; los campos de resultado del envío los fija solo el servidor)

## Acción: Reenviar

**Validaciones:**

- VAL-Correo-009 — El correo está en estado FAIL
  - mensaje: "Solo se pueden reenviar correos que han fallado"
- VAL-Correo-010 — El usuario tiene permiso sobre el centro del correo
  - actor: cualquier rol distinto de Administrador (el Administrador puede reenviar correos de cualquier centro)
  - mensaje: "No puede reenviar correos de un centro que no es suyo"

**Reglas de negocio:**

- RN-Correo-002 — Volver a intentar el envío del correo de forma asíncrona y, según el resultado, pasarlo a SUCCESS (registrando la fecha de envío) o dejarlo en FAIL (actualizando la descripción del fallo), actualizando en todo caso la fecha del último intento e incrementando el número de reintentos.
  - fase: después_de_commit
  - estado: FAIL
