# Catálogo de tipos de validación y restricción

Catálogo de referencia **único** para las **Validaciones** (`VAL-TR-`, en la ficha de su transición de `estados.md`) y las **Restricciones** (`RES-`, en el `entity-*.md`): los tipos de comprobación son los mismos y lo que las distingue es el ámbito — si debe cumplirse **siempre**, se dispare la transición que se dispare, es una `RES-`; si se ancla a **una transición concreta**, es una `VAL-TR-`. Recorre las tablas campo a campo y transición a transición. Las columnas de mensaje sirven de guía para redactar el `mensaje` en lenguaje de negocio.

Es una ayuda **no exhaustiva**: cubre los tipos más habituales, pero si el negocio necesita una validación o restricción que no figura aquí, decláralo igualmente.

> **REQUIRED — empieza por la obligatoriedad.** Antes de bajar a los tipos concretos, recorre **campo a campo** los «Campos editables» de cada transición y comprueba la primera fila: *«El campo A es obligatorio»*. Es la validación más trivial y por eso la más olvidada. Solo después pasa al resto.

> **La precondición de estado NO se declara**: el evento solo existe en su estado origen — la garantiza la máquina, no una `VAL-`.

## Típicas de un expediente

| Descripción de la regla | Cuándo se aplica | Ejemplo de mensaje |
|---|---|---|
| El justificante (adjunto) es obligatorio | Al presentar — siempre, o según la circunstancia | "Debe aportar el justificante" |
| Los adjuntos exigidos dependen del valor de un enum (colectivo, causa) | Al presentar, según el enum | "Como trabajador asalariado debe aportar el certificado de la Seguridad Social" |
| El texto "especificar" es obligatorio cuando el enum vale OTRAS | Si la circunstancia es OTRAS | "Debe especificar la circunstancia" |
| La tabla de <hijas> tiene al menos una fila | Al presentar | "Debe indicar al menos un módulo" |
| Cada fila de la tabla tiene al menos una opción marcada | Al presentar | "Cada módulo debe indicar al menos una convocatoria" |
| El plazo de presentación está abierto (fecha fija, o **relativa** a otra: "al menos dos meses antes del fin del periodo lectivo") | Al presentar | "El plazo de presentación finalizó el 30/04" |
| La modalidad elegida es admisible para el solicitante (una variante solo para cierto régimen o colectivo) | Al presentar | "La anulación por módulos solo se admite en régimen semipresencial" |
| El justificante del pago externo (la tasa) está aportado | Al presentar / entregar | "Debe aportar el justificante del pago de la tasa" |
| El motivo de la resolución es obligatorio en la rama que lo necesita | Al resolver, si la rama es rechazar/subsanar | "El motivo del rechazo es obligatorio" |
| La resolución parcial referencia solo elementos solicitados | Al resolver | "No puede conceder un módulo que no se solicitó" |

## Validaciones sobre el propio campo

| Descripción de la regla | Cuándo se aplica | Mensaje al usuario | Ejemplo de mensaje |
|---|---|---|---|
| El campo A es obligatorio | Siempre | El A es obligatorio | "El motivo del viaje es obligatorio" |
| El campo A es obligatorio si el campo B tiene valor | Si B tiene valor | El A es obligatorio cuando existe B | "La hora de fin es obligatoria si existe la de inicio" |
| El campo A es obligatorio si el campo B vale X | Si B = X | El A es obligatorio cuando B vale X | "Debe especificar la circunstancia si elige OTRAS" |
| El campo A debe tener una longitud mínima/máxima/exacta de X caracteres | Siempre | El A debe tener … caracteres | "El código postal debe tener exactamente 5 caracteres" |
| El campo A debe tener un valor mínimo/máximo/exacto de X, o estar entre X e Y | Siempre | El A debe … | "Las horas cursadas deben ser mayores que 0" |
| El campo A no puede ser negativo / admite como máximo N decimales | Siempre | El A no puede … | "El importe no puede ser negativo" |
| El campo A debe ser una fecha en el pasado / futuro / posterior a F / anterior a F / entre F1 y F2 | Siempre | El A debe ser una fecha … | "La fecha del viaje debe ser posterior a hoy" |
| El campo A debe cumplir el formato F | Siempre | El A debe tener el formato F | "El IBAN no tiene un formato válido" |
| El campo A debe tener un dígito de control válido | Siempre | El A no tiene un dígito de control válido | "El NIF '12345678X' no es válido. Compruebe la letra" |
| El campo A debe ser un valor de la lista cerrada {X, Y, Z} | Siempre | El A debe ser uno de {X, Y, Z} | "La convocatoria debe ser ordinaria o extraordinaria" |
| El campo A debe referenciar a un registro existente de la entidad E | Siempre | El A debe referenciar un E existente | "El ciclo indicado no existe" |
| El adjunto A debe ser de tipo T (PDF, imagen) y no superar un tamaño S | Siempre | El A debe ser un T de menos de S | "El justificante debe ser un PDF o una imagen" |

## Validaciones entre campos del mismo registro

| Descripción de la regla | Cuándo se aplica | Mensaje al usuario | Ejemplo de mensaje |
|---|---|---|---|
| El campo A debe ser mayor / mayor o igual / menor / menor o igual que el campo B | Si B tiene valor | El A debe ser … que B | "La fecha de vuelta debe ser posterior a la de ida" |
| El campo A debe ser igual / distinto que el campo B | Si B tiene valor | El A debe … B | "El revisor debe ser distinto del solicitante" |
| Si existe el valor de A, debe existir también el de B | Si A tiene valor | Debe rellenar también B | "Si indica hora de inicio debe indicar la de fin" |
| Debe existir al menos uno de los campos {A, B, …} | Siempre | Debe rellenar al menos uno entre A, B, … | "Debe indicar al menos un identificador entre DNI y NIA" |
| No pueden existir a la vez los valores de A y B / si existe A no puede existir B | Si tienen valor | No puede rellenar a la vez A y B | "No puede marcar exención total y parcial a la vez" |
| La suma de A1…An debe ser igual al campo B | Siempre | La suma de A1…An debe coincidir con B | "La suma de las líneas de gasto debe coincidir con el total" |
| Si el campo A vale X, el campo B debe valer Y / es obligatorio / no admite valor | Si A = X | Cuando A vale X, B … | "Si la jornada es completa no debe indicar horas" |

## Validaciones entre registros

| Descripción de la regla | Cuándo se aplica | Mensaje al usuario | Ejemplo de mensaje |
|---|---|---|---|
| El campo A (o la combinación A, B, …) debe ser único en su ámbito | Al crear o modificar | Ya existe un registro con A | "Ya existe una renuncia de este alumno para ese módulo este curso" |
| El registro debe tener al menos N / entre N y M / exactamente N hijos de tipo H | Al presentar / cambiar de estado | Debe tener … H | "Debe indicar al menos un módulo" |
| No puede haber dos hijos iguales en la tabla de <hijas> | Al presentar | H repetido | "El módulo ya está en la lista" |
| Debe existir el registro maestro M antes de crear este | Al crear | Debe configurar previamente M | "El alumno no tiene matrícula en este centro" |
| No puede existir otro expediente **abierto** del mismo trámite para la misma persona (¿se permite o no? — preguntarlo) | Al crear / presentar | Ya tiene un expediente en curso | "Ya tiene una renuncia de convocatoria en tramitación" |

## Validaciones de negocio

| Descripción de la regla | Cuándo se aplica | Mensaje al usuario | Ejemplo de mensaje |
|---|---|---|---|
| No se puede ejecutar la transición T si se cumple la condición C | Al disparar T | No se puede T cuando C | "No se puede renunciar a un módulo ya calificado" |
| La transición T solo se admite entre las fechas F1 y F2 (o con antelación X respecto a la fecha F) | Al disparar T | T solo se admite … | "La solicitud debe presentarse al menos dos meses antes del fin del periodo lectivo" |
| La firma debe corresponder al DNI de firma del expediente | Al firmar | La firma no corresponde | "El certificado usado no corresponde al DNI del solicitante" |
| El campo A es inmutable a partir del estado X *(suele bastar con no incluirlo en los «Campos editables» de las transiciones posteriores — declarar la RES- solo si además debe comprobarse)* | Si estado ≥ X | A no se puede modificar tras X | "Los datos presentados no se pueden modificar" |
