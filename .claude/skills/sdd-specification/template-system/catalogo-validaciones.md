# Catálogo de tipos de validación

Catálogo de referencia para identificar, al rellenar las **Validaciones** (`VAL-`) y las **Restricciones** (`RES-`) de un `entity-*.md`, qué comprobaciones aplican a cada campo. Recorre las cuatro tablas por ámbito y comprueba, campo a campo, cuáles aplican. Las columnas "Mensaje al usuario" y "Ejemplo de mensaje" sirven de guía para redactar el `mensaje` de cada validación en lenguaje de negocio.

Es una ayuda **no exhaustiva**: cubre los tipos más habituales, pero si el negocio necesita una validación o una restricción que no figura aquí, decláralo igualmente.

## Validaciones sobre el propio campo

| Descripción de la regla                                                   | Cuándo se aplica            | Mensaje al usuario                          | Ejemplo de mensaje                                                  |
|---------------------------------------------------------------------------|-----------------------------|---------------------------------------------|---------------------------------------------------------------------|
| El campo A es obligatorio                                                 | Siempre                     | El A es obligatorio                         | "El email es obligatorio"                                           |
| El campo A es obligatorio si el campo B tiene valor                       | Si B tiene valor            | El A es obligatorio cuando existe B         | "La fecha de fin es obligatoria si existe la fecha de inicio"       |
| El campo A es obligatorio si el campo B vale X                            | Si B = X                    | El A es obligatorio cuando B vale X         | "El CIF es obligatorio si el tipo es JURIDICA"                      |
| El campo A no admite valor                                                | Siempre                     | El A no admite valor                        | "El NIE no se admite en este formulario"                            |
| El campo A debe tener una longitud mínima de X caracteres                 | Siempre                     | El A debe tener al menos X caracteres       | "El nombre debe tener al menos 2 caracteres"                        |
| El campo A debe tener una longitud máxima de X caracteres                 | Siempre                     | El A no puede superar X caracteres          | "El asunto no puede superar 255 caracteres"                         |
| El campo A debe tener una longitud exacta de X caracteres                 | Siempre                     | El A debe tener exactamente X caracteres    | "El código postal debe tener exactamente 5 caracteres"              |
| El campo A debe tener un valor mínimo de X                                | Siempre                     | El A debe ser mayor o igual que X           | "La cantidad debe ser mayor o igual que 1"                          |
| El campo A debe tener un valor máximo de X                                | Siempre                     | El A no puede superar X                     | "El descuento no puede superar el 100%"                             |
| El campo A debe tener un valor exacto de X                                | Siempre                     | El A debe valer exactamente X               | "El número de copias debe ser 1"                                    |
| El campo A debe estar entre X e Y                                         | Siempre                     | El A debe estar entre X e Y                 | "La cantidad debe estar entre 1 y 999"                              |
| El campo A no puede ser negativo                                          | Siempre                     | El A no puede ser negativo                  | "El importe no puede ser negativo"                                  |
| El campo A admite como máximo N decimales                                 | Siempre                     | El A admite hasta N decimales               | "El precio admite hasta 2 decimales"                                |
| El campo A debe ser una fecha en el pasado                                | Siempre                     | El A debe ser una fecha pasada              | "La fecha de nacimiento debe ser anterior a hoy"                    |
| El campo A debe ser una fecha en el futuro                                | Siempre                     | El A debe ser una fecha futura              | "La fecha de cita debe ser posterior a hoy"                         |
| El campo A debe ser una fecha posterior a F                               | Siempre                     | El A debe ser posterior a F                 | "La fecha de matrícula debe ser posterior al 01/09/2024"            |
| El campo A debe ser una fecha anterior a F                                | Siempre                     | El A debe ser anterior a F                  | "La fecha de pago debe ser anterior al 31/12/2024"                  |
| El campo A debe estar entre las fechas F1 y F2                            | Siempre                     | El A debe estar entre F1 y F2               | "La fecha de matrícula debe estar entre 01/09/2024 y 30/09/2024"    |
| El campo A debe cumplir el formato F *(ver catálogo)*                     | Siempre                     | El A debe tener el formato F                | "El email debe tener el formato usuario@dominio.com"                |
| El campo A debe tener un dígito de control válido                         | Siempre                     | El A no tiene un dígito de control válido   | "El NIF '12345678X' no es válido. Compruebe la letra verificadora"  |
| El campo A solo admite caracteres del conjunto C                          | Siempre                     | El A solo admite los caracteres C           | "El código solo admite letras y dígitos"                            |
| El campo A debe ser un valor de la lista cerrada {X, Y, Z}                | Siempre                     | El A debe ser uno de {X, Y, Z}              | "El estado debe ser uno de: BORRADOR, ENVIADO, APROBADO"            |
| El campo A debe referenciar a un registro existente de la entidad E       | Siempre                     | El A debe referenciar un E existente        | "El centro indicado no existe"                                      |

---

## Validaciones entre campos del mismo registro

| Descripción de la regla                                                   | Cuándo se aplica            | Mensaje al usuario                          | Ejemplo de mensaje                                                  |
|---------------------------------------------------------------------------|-----------------------------|---------------------------------------------|---------------------------------------------------------------------|
| El campo A debe ser mayor que el campo B                                  | Si B tiene valor            | El A debe ser mayor que B                   | "La fecha de fin debe ser posterior a la de inicio"                 |
| El campo A debe ser mayor o igual que el campo B                          | Si B tiene valor            | El A debe ser mayor o igual que B           | "La fecha de cierre debe ser mayor o igual que la de apertura"      |
| El campo A debe ser menor que el campo B                                  | Si B tiene valor            | El A debe ser menor que B                   | "El descuento aplicado debe ser menor que el precio"                |
| El campo A debe ser menor o igual que el campo B                          | Si B tiene valor            | El A debe ser menor o igual que B           | "La fecha de aviso debe ser menor o igual que la de vencimiento"    |
| El campo A debe ser igual que el campo B                                  | Si B tiene valor            | El A debe coincidir con B                   | "La confirmación de la contraseña debe coincidir con la contraseña" |
| El campo A debe ser distinto del campo B                                  | Si B tiene valor            | El A debe ser distinto de B                 | "El revisor debe ser distinto del solicitante"                      |
| Si existe el valor del campo A, debe existir también el valor del campo B | Si A tiene valor            | Debe rellenar también B                     | "Si indica fecha de inicio debe indicar también fecha de fin"       |
| Debe existir al menos uno de los campos {A, B, …}                         | Siempre                     | Debe rellenar al menos uno entre A, B, …    | "Debe indicar al menos un identificador entre DNI, NIA o NRP"       |
| No pueden existir a la vez los valores de A y B                           | Si A y B tienen valor       | No puede rellenar a la vez A y B            | "No puede indicar NIF y CIF a la vez"                               |
| Si existe el valor del campo A, no puede existir el valor del campo B     | Si A tiene valor            | No puede rellenar B si existe A             | "Si ha indicado NIE no puede indicar NIF"                           |
| La suma de A1, A2, …, An debe ser igual al campo B                        | Siempre                     | La suma de A1…An debe coincidir con B       | "La suma de las líneas (450,00 €) debe coincidir con el total (500,00 €)" |
| Si el campo A vale X, el campo B debe valer Y                             | Si A = X                    | Cuando A vale X, B debe valer Y             | "Si el tipo es JURIDICA, la forma jurídica debe ser SA, SL o CB"    |
| Si el campo A vale X, el campo B es obligatorio                           | Si A = X                    | Cuando A vale X, B es obligatorio           | "Si el tipo es JURIDICA el CIF es obligatorio"                      |
| Si el campo A vale X, el campo B no admite valor                          | Si A = X                    | Cuando A vale X, B no se admite             | "Si el tipo es FISICA no debe indicar CIF"                          |

---

## Validaciones entre registros

| Descripción de la regla                                                   | Cuándo se aplica            | Mensaje al usuario                          | Ejemplo de mensaje                                                  |
|---------------------------------------------------------------------------|-----------------------------|---------------------------------------------|---------------------------------------------------------------------|
| El campo A debe ser único globalmente                                     | Al crear o modificar A      | Ya existe un registro con A                 | "Ya existe una persona con NIF '12345678Z'"                         |
| El campo A debe ser único dentro del ámbito C                             | Al crear o modificar A      | Ya existe un registro con A en C            | "Ya existe un aula con código 'A-101' en el centro IES Levante"     |
| La combinación (A, B, …) debe ser única                                   | Al crear o modificar A o B  | Ya existe un registro con esa combinación   | "Ya existe una matrícula del alumno '12345678Z' en el curso 2024/2025" |
| No se puede borrar el registro padre si tiene registros hijos *(RESTRICT)* | Al borrar el padre          | No se puede borrar P porque tiene H asociados | "No se puede borrar la familia profesional 'Informática' porque tiene 3 ciclos asociados" |
| Al borrar el padre se borran también los registros hijos *(CASCADE)*      | Al borrar el padre          | Se borrarán también los H asociados         | "Se borrarán también las N líneas del pedido"                       |
| Al borrar el padre los hijos pierden la referencia *(SET NULL)*           | Al borrar el padre          | Los H quedarán sin P asignado               | "Los alumnos del aula quedarán sin aula asignada"                   |
| El registro debe tener al menos N hijos de tipo H                         | Al guardar / cambiar estado | Debe tener al menos N H                     | "El ciclo debe tener al menos 1 módulo asociado"                    |
| El registro debe tener entre N y M hijos de tipo H                        | Al guardar / cambiar estado | Debe tener entre N y M H                    | "El tribunal debe tener entre 3 y 5 miembros"                       |
| El registro debe tener exactamente N hijos de tipo H                      | Al guardar / cambiar estado | Debe tener exactamente N H                  | "El expediente debe tener exactamente 1 solicitante"                |
| Debe existir el registro maestro M antes de crear este                    | Al crear                    | Debe configurar previamente M               | "Debe configurar el plan de estudios antes de crear matrículas"     |

---

## Validaciones de negocio

| Descripción de la regla                                                   | Cuándo se aplica            | Mensaje al usuario                          | Ejemplo de mensaje                                                  |
|---------------------------------------------------------------------------|-----------------------------|---------------------------------------------|---------------------------------------------------------------------|
| No se puede ejecutar la operación O si se cumple la condición C           | Al ejecutar O               | No se puede O cuando C                      | "No se puede matricular a un alumno con recibos pendientes de pago" |
| La operación O requiere el rol R                                          | Al ejecutar O               | Solo R puede ejecutar O                     | "Solo el director puede aprobar descuentos superiores al 20%"       |
| La operación O solo se admite entre F1 y F2                               | Al ejecutar O               | O solo se admite entre F1 y F2              | "La matrícula solo se admite entre el 01/09/2024 y el 30/09/2024"   |
| El campo A es inmutable a partir del estado X                             | Si estado ≥ X               | A no se puede modificar tras X              | "No se puede modificar el NIA tras la matriculación"                |
| El registro completo es inmutable en el estado final F                    | Si estado = F               | El registro no se puede modificar en F      | "El expediente FINALIZADO no admite modificaciones"                 |
| Solo se admite la transición de estado X a Y                              | Al cambiar de estado        | No se admite pasar de X a Z                 | "No se admite pasar de BORRADOR a APROBADO sin enviar antes"        |
