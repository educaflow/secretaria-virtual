# Modelo: Notificacion

Un aviso que la secretaría virtual envía (o tiene pendiente de enviar) a una persona por un canal concreto.
Reúne todo lo que tienen en común los canales: a quién va, de qué centro sale, por qué se envía (el motivo), a qué estado de expediente está ligado y cómo ha ido su envío.
Nunca se crea una notificación «genérica»: siempre se crea como una de sus clases, **Correo** o **Sms**, que añaden los datos de su canal; un canal nuevo será otra clase más.
Ciclo de vida: nace pendiente, el sistema intenta enviarla justo después de crearla y queda enviada o fallida; una fallida se puede reenviar. Una vez creada no se modifica ni se borra nunca.
Todas las reglas de este fichero se aplican a todas sus clases, además de las propias de cada canal.

## Campos

- **tipo de notificación** — el canal de la notificación; vale «Correo» o «SMS» y admite canales nuevos en el futuro
- **motivo** — texto interno que explica de qué va la notificación (p. ej. «Resolución expediente 12345/2025/4006123»); lo ve el centro y el administrador, no el destinatario
- **DNI del destinatario** — DNI de la persona a la que va dirigida; es lo que la relaciona con el usuario que la consulta como destinatario
- **nombre** — nombre de pila del destinatario
- **apellidos** — apellidos del destinatario
- **destino** — a dónde se envía, sea cual sea el canal: la dirección de correo o el número de teléfono
- **centro** — el centro que envía la notificación; decide qué gestores del centro la ven
- **estado del expediente** — opcional: el estado en el que se encontraba un expediente del mismo centro cuando se envió la notificación
- **expediente** — el nombre del expediente al que pertenece ese estado, para mostrarlo en los listados
- **estado** — situación del envío (sus valores, en «Estados y transiciones»)
- **fecha de creación** — cuándo se dio de alta
- **fecha del primer intento de envío** — cuándo se intentó enviar por primera vez
- **fecha del último intento de envío** — cuándo se intentó enviar por última vez
- **fecha de envío** — cuándo se envió con éxito; solo la tiene una notificación enviada
- **número de reintentos** — cuántas veces se ha intentado enviar
- **descripción del último fallo** — explicación técnica del último intento fallido; solo la tiene una notificación fallida

## Estados y transiciones

Los estados se muestran al usuario como «Pendiente», «Enviado» y «Fallido», iguales para todos los canales.

- Estado inicial: PENDIENTE (se da de alta y queda a la espera del envío).
- PENDIENTE → ENVIADO: el envío, que el sistema lanza justo después del alta, funciona.
- PENDIENTE → FALLIDO: el envío falla.
- FALLIDO → ENVIADO: tras pulsar «Reenviar», el nuevo intento funciona.
- FALLIDO → FALLIDO: tras pulsar «Reenviar», el nuevo intento vuelve a fallar.
- ENVIADO es terminal: una notificación enviada no se vuelve a enviar nunca.

## Restricciones

- RES-Notificacion-001 — El motivo está indicado
  - mensaje: "El motivo es obligatorio"
- RES-Notificacion-002 — El motivo no supera 255 caracteres
  - mensaje: "El motivo no puede superar 255 caracteres"
- RES-Notificacion-003 — El tipo de notificación corresponde siempre a la clase de la notificación (Correo para un correo, SMS para un SMS) y no cambia nunca después de crearla
- RES-Notificacion-004 — Una notificación ya creada no se modifica
  - mensaje: el de su canal: "El correo es inmutable tras su creación." / "El SMS es inmutable tras su creación."
- RES-Notificacion-005 — Una notificación no se borra nunca
  - mensaje: el de su canal: "Los correos no se pueden borrar." / "Los SMS no se pueden borrar."
- RES-Notificacion-006 — Solo una notificación en estado ENVIADO tiene fecha de envío
- RES-Notificacion-007 — Si la notificación está ligada a un estado de expediente, ese estado existe y su expediente es del mismo centro que la notificación
  - mensaje: el de su canal: "El historial de estado indicado no existe" / "El estado del expediente indicado no existe"
- RES-Notificacion-008 — Solo una notificación en estado FALLIDO tiene descripción del último fallo
- RES-Notificacion-009 — Una notificación en estado PENDIENTE no tiene ningún intento: su número de reintentos es 0 y no tiene fecha del primer ni del último intento de envío
- RES-Notificacion-010 — Una notificación en estado ENVIADO o FALLIDO tiene al menos un intento: número de reintentos de 1 o más y fechas del primer y del último intento de envío indicadas
- RES-Notificacion-011 — La fecha del primer intento de envío no es anterior a la fecha de creación, y la del último intento no es anterior a la del primer intento
- RES-Notificacion-012 — El estado solo cambia por las transiciones declaradas en «Estados y transiciones»; una notificación ENVIADO no cambia nunca de estado
- RES-Notificacion-013 — El centro está indicado
  - mensaje: "El centro es obligatorio"
- RES-Notificacion-014 — El DNI del destinatario está indicado
  - mensaje: "El DNI del destinatario es obligatorio"

## Campos calculados

- CC-Notificacion-001 — tipo de notificación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: «Correo» al crear un correo y «SMS» al crear un SMS; lo fija el sistema según la clase de notificación que se crea, aunque el cliente envíe otro valor
- CC-Notificacion-002 — destino
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: en un correo, la dirección del «para»; en un SMS, el teléfono (ya en formato internacional); cada canal nuevo declara de qué dato sale su destino
- CC-Notificacion-003 — expediente
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: el nombre del expediente al que pertenece el estado del expediente ligado; vacío si la notificación no está ligada a ninguno
- CC-Notificacion-004 — fecha de creación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: fecha y hora actuales en el momento del alta

## Acción: Crear

Una notificación solo se crea como Correo o como Sms: las propiedades que la interfaz puede enviar y las validaciones de cada alta se declaran en la acción Crear de cada canal.

**Input AllowProperties:** (ninguna — una notificación no se da de alta directamente, solo como Correo o Sms)

**Reglas de negocio:**

- RN-Notificacion-001 — Asignar los valores iniciales del envío: estado PENDIENTE, número de reintentos 0 y sin fechas de intento ni de envío ni descripción de fallo
  - fase: antes_de_commit
- RN-Notificacion-002 — Lanzar el envío de la notificación por su canal
  - fase: después_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — una notificación es inmutable tras su creación)

## Acción: Enviar

Acción interna del sistema, que se lanza tras el alta y tras cada reenvío; no la pulsa ningún usuario ni recibe datos del formulario.

**Reglas de negocio:**

- RN-Notificacion-003 — Si la notificación ya está ENVIADO, no hacer nada (nunca se envía dos veces, aunque se hayan pedido dos reenvíos a la vez)
  - fase: antes_de_commit
  - estado: ENVIADO
- RN-Notificacion-004 — Registrar el intento: la fecha del último intento pasa a ser la actual, la del primer intento se rellena si estaba vacía y el número de reintentos aumenta en uno
  - fase: antes_de_commit
- RN-Notificacion-005 — Si el envío funciona, pasar a ENVIADO, guardar la fecha de envío y vaciar la descripción del último fallo
  - fase: antes_de_commit
- RN-Notificacion-006 — Si el envío falla, pasar a FALLIDO, guardar la descripción del fallo y dejar vacía la fecha de envío
  - fase: antes_de_commit

## Acción: Reenviar

**Input AllowProperties:** (ninguna — el reenvío actúa sobre la notificación guardada y no recibe datos del formulario)

**Validaciones:**

- VAL-Notificacion-001 — La notificación está en estado FALLIDO
  - mensaje: el de su canal: "Solo se pueden reenviar correos que han fallado" / "Solo se pueden reenviar SMS que han fallado"
- VAL-Notificacion-002 — El usuario es administrador, o en el centro de la notificación tiene el tipo de usuario Supervisor o Administrativo, o el cargo de Director, Jefe de estudios o Secretario
  - mensaje: el de su canal: "No puede reenviar correos de un centro que no es suyo" / "No puede reenviar SMS de un centro que no es suyo"

**Reglas de negocio:**

- RN-Notificacion-007 — Lanzar de nuevo el envío de la notificación por su canal (el cambio de estado lo hace la acción Enviar)
  - fase: después_de_commit
