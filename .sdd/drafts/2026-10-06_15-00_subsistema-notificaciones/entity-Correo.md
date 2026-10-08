# Modelo: Correo

Una notificación cuyo canal es el correo electrónico.
Es una clase de **Notificacion**: tiene todos sus campos, estados, restricciones, campos calculados y reglas, y añade los datos propios de un correo y sus adjuntos.
Lo da de alta a mano el administrador o lo crea el sistema durante la tramitación de expedientes (el aviso de subsanación). Una vez creado no se modifica ni se borra; solo se reenvía si ha fallado.

## Campos

- **para** — la dirección de correo del destinatario principal; una sola dirección
- **en copia** — direcciones de correo en copia, separadas por comas
- **en copia oculta** — direcciones de correo en copia oculta, separadas por comas
- **asunto** — el asunto que recibe el destinatario
- **cuerpo** — el texto del correo que recibe el destinatario, en texto plano
- **adjuntos** — los ficheros que viajan con el correo

Además, los heredados de Notificacion: tipo de notificación, motivo, DNI del destinatario, nombre, apellidos, destino, centro, estado del expediente, expediente, estado, fechas del envío, número de reintentos y descripción del último fallo.

## Restricciones

- RES-Correo-001 — El tipo de notificación de un correo es siempre «Correo»
- RES-Correo-002 — No hay dos adjuntos con el mismo nombre de fichero en un mismo correo
  - mensaje: "Ya existe un adjunto con ese nombre en el correo"

## Campos calculados

- CC-Correo-001 — destino
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: la dirección del «para»

## Acción: Crear

**Input AllowProperties:** motivo, DNI del destinatario, nombre, apellidos, para, en copia, en copia oculta, asunto, cuerpo, centro, estado del expediente, adjuntos (de cada adjunto: nombre del fichero y contenido)

**Validaciones:**

- VAL-Correo-002 — El DNI del destinatario es un DNI válido (su letra corresponde al número)
  - condición: el DNI del destinatario está indicado
  - mensaje: "El DNI del destinatario no es válido; compruebe la letra"
- VAL-Correo-003 — El nombre está indicado
  - mensaje: "El nombre es obligatorio"
- VAL-Correo-004 — Los apellidos están indicados
  - mensaje: "Los apellidos son obligatorios"
- VAL-Correo-005 — El «para» tiene al menos una dirección
  - mensaje: "Debe indicar al menos un destinatario en el «para»"
- VAL-Correo-006 — El «para» contiene una sola dirección
  - condición: el «para» está indicado
  - mensaje: "El «para» debe contener una sola dirección de correo; use «en copia» para añadir más destinatarios"
- VAL-Correo-007 — La dirección del «para» tiene formato de correo válido
  - condición: el «para» contiene una sola dirección
  - mensaje: "El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)"
- VAL-Correo-008 — Todas las direcciones del «en copia» tienen formato de correo válido
  - condición: el «en copia» está indicado
  - mensaje: "El «en copia» debe contener direcciones de correo válidas"
- VAL-Correo-009 — Todas las direcciones del «en copia oculta» tienen formato de correo válido
  - condición: el «en copia oculta» está indicado
  - mensaje: "El «en copia oculta» debe contener direcciones de correo válidas"
- VAL-Correo-010 — El asunto está indicado (un asunto solo con espacios cuenta como vacío)
  - mensaje: "El asunto es obligatorio"
- VAL-Correo-011 — El asunto no supera 255 caracteres
  - condición: el asunto está indicado
  - mensaje: "El asunto no puede superar 255 caracteres"
- VAL-Correo-012 — El cuerpo está indicado (un cuerpo solo con espacios cuenta como vacío)
  - mensaje: "El cuerpo es obligatorio"
- VAL-Correo-014 — El centro es uno de los centros del usuario
  - condición: el centro está indicado y el usuario no es administrador
  - mensaje: "No puede crear correos para un centro que no es suyo"
- VAL-Correo-015 — El nombre no supera 255 caracteres
  - condición: el nombre está indicado
  - mensaje: "El nombre no puede superar 255 caracteres"
- VAL-Correo-016 — Los apellidos no superan 255 caracteres
  - condición: los apellidos están indicados
  - mensaje: "Los apellidos no pueden superar 255 caracteres"
- VAL-Correo-017 — El asunto no contiene saltos de línea ni caracteres de control
  - condición: el asunto está indicado
  - mensaje: "El asunto no puede contener saltos de línea ni caracteres de control"
- VAL-Correo-018 — Ninguna dirección se repite entre el «para», el «en copia» y el «en copia oculta», ni dentro de una misma lista
  - condición: el «en copia» o el «en copia oculta» están indicados
  - mensaje: "Una misma dirección de correo no puede aparecer más de una vez entre el «para», el «en copia» y el «en copia oculta»"
- VAL-Correo-019 — El tamaño total de todos los adjuntos del correo no supera 25 MB
  - condición: el correo tiene adjuntos
  - mensaje: "Los adjuntos del correo no pueden superar 25 MB en total"

(Se aplican además las restricciones de Notificacion: el motivo obligatorio y de 255 caracteres como máximo, el centro y el DNI del destinatario obligatorios, y el estado del expediente, si se indica, del mismo centro.)

**Reglas de negocio:**

- RN-Correo-001 — Al enviarse, el correo sale con el «para», las copias, el asunto, el cuerpo en texto plano y todos sus adjuntos, desde la dirección remitente configurada en la instalación
  - fase: después_de_commit
- RN-Correo-002 — Cuando en la verificación de un expediente se pide que se subsane, el sistema crea un correo para el solicitante con: el DNI, el nombre, los apellidos y, en el «para», la dirección de correo del solicitante; el centro del expediente; ligado al estado en que se encuentra el expediente en ese momento; el motivo «Subsanación del expediente <número del expediente>»; el asunto «Tiene que subsanar su solicitud del expediente <número del expediente>»; y un cuerpo que incluye el nombre de la solicitud, el número del expediente y el texto de subsanación que ha escrito quien verifica. Si ese correo no supera las validaciones del alta (por ejemplo, el solicitante no tiene dirección de correo), no se crea y la tramitación sigue igualmente.
  - fase: antes_de_commit
  - condición: la notificación la crea el sistema al pedir la subsanación de un expediente
- RN-Correo-003 — Si la instalación no tiene configurado el servidor de correo, el envío falla y el correo queda FALLIDO, con una descripción del fallo que lo explica
  - fase: después_de_commit
- RN-Correo-004 — Al enviar, se ignoran los espacios al principio y al final de cada dirección del «para», del «en copia» y del «en copia oculta»
  - fase: después_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — un correo es inmutable tras su creación)
