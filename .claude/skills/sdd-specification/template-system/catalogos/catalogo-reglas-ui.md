# Catálogo de tipos de regla de UI

Catálogo de referencia para identificar, al rellenar las **Reglas de UI** (`RUI-`) de un `screen-*.md`, qué comportamientos de pantalla necesita cada vista. Recorre las tablas vista a vista (y, dentro de cada una, sus campos, paneles y botones) considerando los **roles** que la usan y los **estados** del registro. La columna "disparador típico" orienta el atributo `disparador`.

Es una ayuda **no exhaustiva**: si el negocio necesita una regla de UI que no figura aquí, decláralo igualmente. Todo en **lenguaje de negocio**: se describe qué ve el usuario, nunca cómo se implementa.

> Recuerda la frontera: una RUI **solo cambia lo que el usuario ve o puede editar** — no bloquea operaciones (eso es una `VAL-`) ni escribe en el sistema (eso es una `RN-`).

## Visibilidad

| Descripción de la regla                                                   | disparador típico | Ejemplo                                                              |
|-----------------------------------------------------------------------------|--------------------|------------------------------------------------------------------------|
| Un campo o panel solo se muestra cuando otro campo tiene cierto valor/estado | continuo          | "El motivo de rechazo solo se muestra cuando el estado es RECHAZADO"   |
| Un botón solo se muestra en ciertos estados del registro                   | continuo           | "El botón Reenviar solo aparece si el correo está en estado FAIL"      |
| Un campo, panel o botón solo lo ve cierto rol                              | al cargar (+ actor) | "El botón Publicar solo lo ve el administrador"                       |

## Edición

| Descripción de la regla                                                   | disparador típico | Ejemplo                                                              |
|-----------------------------------------------------------------------------|--------------------|------------------------------------------------------------------------|
| Un campo (o el formulario entero) pasa a solo lectura según el estado       | continuo           | "Tras emitirse, el formulario es de solo lectura"                      |
| Un campo se marca visualmente como obligatorio cuando otro campo tiene cierto valor | continuo    | "El CIF se marca obligatorio cuando el tipo es JURIDICA" *(el bloqueo real es la `VAL-` correspondiente)* |
| Un campo de un **hijo maestro-detalle** se marca obligatorio / con formato en su alta, reflejando su validación (que solo salta al guardar el padre) | continuo | "Al añadir un adjunto, el nombre de fichero se marca obligatorio" *(la defensa real es la `VAL-`/`RES-` del hijo; solo se reflejan las validaciones factibles en el cliente — la unicidad y demás comprobaciones entre registros NO)* |

## Valores por defecto

| Descripción de la regla                                                   | disparador típico | Ejemplo                                                              |
|-----------------------------------------------------------------------------|--------------------|------------------------------------------------------------------------|
| Al crear, un campo se rellena con un dato del contexto (usuario actual, su centro, la fecha de hoy) | al crear | "Al crear un expediente, el centro se rellena con el del usuario actual" |
| Al crear un hijo dentro del formulario de su padre, la referencia al padre se fija con ese padre | al crear | "Al añadir un curso desde el ciclo, el ciclo queda fijado" *(obligatoria en todo alta maestro-detalle; ver «Reglas de UI» de la guía)* |
| Al cambiar un campo, otro campo se rellena o se limpia en consecuencia      | al cambiar <campo> | "Al elegir el tipo de documento se propone su plantilla por defecto"    |

## Opciones disponibles

| Descripción de la regla                                                   | disparador típico | Ejemplo                                                              |
|-----------------------------------------------------------------------------|--------------------|------------------------------------------------------------------------|
| Las opciones de un campo se limitan según el valor de otro campo            | al cambiar <campo> | "En cursos solo se pueden elegir módulos del ciclo seleccionado"        |
| Las opciones de un campo se limitan según el rol o el centro del usuario    | al cargar          | "El supervisor solo puede elegir alumnos de su centro"                  |

## Avisos y confirmaciones

| Descripción de la regla                                                   | disparador típico | Ejemplo                                                              |
|-----------------------------------------------------------------------------|--------------------|------------------------------------------------------------------------|
| Antes de una acción se pide confirmación al usuario (puede cancelar) si se da una condición — o siempre, si la acción es delicada | al pulsar el botón | "Si la factura no tiene impuestos, al guardar se pregunta «¿Desea continuar?»" *(si en vez de poder continuar debe impedirse, es una `VAL-`)* |
| Al abrir un registro en cierto estado se muestra un aviso informativo       | al cargar          | "Al abrir un expediente archivado se informa de que no se puede modificar" |
| Tras completarse una acción se muestra una notificación breve de resultado  | al terminar la acción | "Tras reenviar el correo se notifica «Reenvío en curso»"             |
