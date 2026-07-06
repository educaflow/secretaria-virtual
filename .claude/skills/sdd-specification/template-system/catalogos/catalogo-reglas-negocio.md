# Catálogo de tipos de regla de negocio

Catálogo de referencia para identificar, al rellenar las **Reglas de negocio** (`RN-`) de un `entity-*.md`, qué operaciones automáticas ejecuta el sistema como reacción a cada acción o transición de estado. Recorre las tablas acción a acción y transición a transición y comprueba cuáles aplican. La columna "fase típica" orienta el atributo `fase` (`antes_de_commit` / `después_de_commit`); la decisión final es del negocio.

Es una ayuda **no exhaustiva**: cubre los tipos más habituales, pero si el negocio necesita una regla que no figura aquí, decláralo igualmente.

> Recuerda la frontera: una RN **siempre actúa, nunca bloquea**. Si lo que se busca es impedir la operación, es una validación (`VAL-`); si solo cambia lo que el usuario ve en pantalla, es una regla de UI (`RUI-`); si es un valor que el servidor calcula para un campo, es un campo calculado (`CC-`).

## Sobre el propio registro

| Descripción de la regla                                                    | Cuándo se dispara            | fase típica       | Ejemplo                                                            |
|-----------------------------------------------------------------------------|-------------------------------|--------------------|----------------------------------------------------------------------|
| Asignar una numeración secuencial al registro                              | Al crear                      | antes_de_commit    | "Al crear el registro de salida se le asigna el número NNNNN/AAAA secuencial por centro y año" |
| Registrar la fecha (y/o el usuario) de un evento de negocio                | Al crear / al ejecutar la acción | antes_de_commit | "Al enviar el correo se registra la fecha del último intento"       |
| Fijar o cambiar el estado del registro como consecuencia de la acción      | Al crear / al ejecutar la acción | antes_de_commit | "Al crearse, el correo queda en estado PENDIENTE"                   |
| Recalcular un valor derivado del propio registro                           | Al crear / al modificar       | antes_de_commit    | "Al guardar se recalcula el total como suma de las líneas"          |
| Guardar una copia (snapshot) de datos de otra entidad tal y como están ahora | Al crear                    | antes_de_commit    | "Al crear la matrícula se copia el nombre del ciclo vigente en ese momento" |
| Incrementar un contador de intentos/usos                                   | Al ejecutar la acción         | antes_de_commit    | "Al reenviar se incrementa el número de reintentos"                 |

## Sobre otros registros

| Descripción de la regla                                                    | Cuándo se dispara            | fase típica       | Ejemplo                                                            |
|-----------------------------------------------------------------------------|-------------------------------|--------------------|----------------------------------------------------------------------|
| Crear automáticamente un registro relacionado                              | Al confirmarse la acción      | después_de_commit  | "Al emitir el certificado se crea su asiento en el registro de salida" |
| Actualizar o cancelar registros relacionados                               | Al confirmarse la acción      | después_de_commit  | "Al anular la matrícula se cancelan sus recibos pendientes"         |
| Propagar un cambio a los registros que dependen de este                    | Al modificar un dato compartido | después_de_commit | "Al cambiar el NIF de la persona se actualiza en sus expedientes abiertos" |
| Registrar la operación en un histórico o traza                             | Al confirmarse la acción      | después_de_commit  | "Cada reenvío queda anotado en el historial del correo"             |

## Con el exterior

| Descripción de la regla                                                    | Cuándo se dispara            | fase típica       | Ejemplo                                                            |
|-----------------------------------------------------------------------------|-------------------------------|--------------------|----------------------------------------------------------------------|
| Enviar una notificación o correo a una persona                             | Al confirmarse la acción      | después_de_commit  | "Al rechazar la solicitud se envía al alumno un correo con el motivo" |
| Generar un documento y asociarlo al registro                               | Al confirmarse la acción      | después_de_commit  | "Al emitir se genera el certificado en PDF y se asocia a la solicitud" |
| Poner un documento a la firma de una persona                               | Al confirmarse la acción      | después_de_commit  | "Al emitir el acta se pone a la firma del secretario"               |
| Reintentar una operación externa que falló, dejando constancia del resultado | Al pedir el reintento        | después_de_commit  | "Al pulsar Reenviar se vuelve a intentar el envío y se guarda el resultado" |

## Por transición de estado

| Descripción de la regla                                                    | Cuándo se dispara            | fase típica       | Ejemplo                                                            |
|-----------------------------------------------------------------------------|-------------------------------|--------------------|----------------------------------------------------------------------|
| Al entrar el registro en el estado X, ejecutar la operación O              | Al cambiar a X                | según la operación | "Al pasar a EMITIDA se registra la fecha de resolución"             |
| Si la operación externa tiene éxito el registro pasa a X; si falla pasa a Y guardando el motivo | Al terminar la operación | después_de_commit | "Si el envío funciona el correo pasa a SUCCESS; si falla pasa a FAIL y se guarda la descripción del fallo" |
