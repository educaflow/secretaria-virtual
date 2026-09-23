# Ciclo de vida — delta

Esta iniciativa modifica la versión v1 existente del trámite. El ciclo de vida completo (fases, estados, transiciones, resto de acciones) no cambia respecto a la versión actual — se conserva tal cual está hoy. Este fichero documenta **solo** lo que cambia: cómo se rellena el dato de "cuándo se produjo la falta" al crear el expediente, y el detalle de la acción GUARDAR_DATOS del estado RECEPCION / ENTRADA_DATOS.

## Al crear el expediente

Antes, el sistema rellenaba automáticamente el año en curso en el dato que indicaba cuándo se había producido la falta. Esto desaparece: ahora, al crear el expediente, el sistema **no** rellena ningún dato sobre cuándo se produjo la falta. La fecha de inicio nace vacía y es el profesor quien la rellena en la pantalla de entrada de datos.

(El resto de lo que ocurre al crear el expediente —a qué estado nace, quién queda como interesado— no cambia.)

## Fase RECEPCION

### Estado ENTRADA_DATOS

Estado sin cambios salvo el detalle de la acción GUARDAR_DATOS, documentado abajo. La acción de borrar el expediente (DELETE) no cambia.

#### Acción GUARDAR_DATOS

- **Quién la lanza:** el perfil CREADOR *(sin cambios)*.
- **Pide confirmación:** no *(sin cambios)*.
- **Datos que el usuario envía al lanzarla** (lista completa, no solo lo que cambia — sustituye a la lista anterior, que incluía los días del mes, el mes y el año):
  - El tipo de jornada faltada.
  - La fecha de inicio de la ausencia.
  - La fecha de fin de la ausencia (solo cuando el tipo de jornada faltada la necesita).
  - La hora de inicio de la ausencia (solo cuando el tipo de jornada faltada la necesita).
  - La hora de fin de la ausencia (solo cuando el tipo de jornada faltada la necesita).
  - El motivo de la falta.
  - La explicación del motivo, cuando el motivo elegido es «Otros».
  - El justificante adjunto (foto o PDF).

- **Comprobaciones que deben pasar (VAL-):**
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-001** — El tipo de jornada faltada no puede quedar vacío. mensaje: «Debe indicar el tipo de jornada faltada».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-002** — La fecha de inicio no puede quedar vacía. mensaje: «Debe indicar la fecha».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-003** — La fecha de inicio no puede ser anterior a hace un año. mensaje: «La fecha debe ser de los últimos 12 meses».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-004** — La fecha de inicio no puede ser posterior a hoy. mensaje: «La fecha no puede ser posterior a hoy».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-005** — La fecha de fin no puede quedar vacía. condición: el tipo de jornada faltada es «Varios días (todos ellos completos)» o «Varios días pero del primer día solo faltó unas horas». mensaje: «Debe indicar la fecha de fin».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-006** — La fecha de fin debe ser posterior a la fecha de inicio. condición: la misma que la comprobación anterior. mensaje: «La fecha de fin debe ser posterior a la fecha de inicio».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-007** — La fecha de fin no puede ser posterior a hoy. condición: la misma que la comprobación anterior. mensaje: «La fecha de fin no puede ser posterior a hoy».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-008** — La hora de inicio no puede quedar vacía. condición: el tipo de jornada faltada es «Unas horas de un único día» o «Varios días pero del primer día solo faltó unas horas». mensaje: «Debe indicar la hora de inicio».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-009** — La hora de fin no puede quedar vacía. condición: el tipo de jornada faltada es «Unas horas de un único día». mensaje: «Debe indicar la hora de fin».
  - **VAL-ENTRADA_DATOS-GUARDAR_DATOS-010** — La hora de fin debe ser posterior a la hora de inicio. condición: la misma que la comprobación anterior *(esta comprobación ya existía y no cambia)*. mensaje: «La hora de fin debe ser posterior a la hora de inicio».
  - *(Se conservan sin cambios las comprobaciones sobre el motivo de la falta, sobre la explicación cuando el motivo es «Otros», y sobre el justificante adjunto.)*

- **Lo que produce (RN-):**
  - **RN-001** — Se limpian del expediente los valores de fecha de fin, hora de inicio y hora de fin que no correspondan al tipo de jornada faltada elegido en el momento de guardar (por ejemplo, si el tipo es «Un día completo» se limpian los tres; si es «Unas horas de un único día» se limpia la fecha de fin). condición: solo afecta a los campos que el tipo de jornada faltada vigente no necesita; los que sí corresponden se guardan con el valor introducido. *(Nuevo: evita que quede en el expediente un valor de una elección de tipo de jornada anterior que el profesor cambió antes de guardar.)*
- **A qué estado lleva:** RECEPCION / PENDIENTE_PRESENTACION *(sin cambios)*.

## Fase TRAMITACION

*(Sin cambios: ninguno de sus estados, acciones, comprobaciones ni transiciones se toca en esta iniciativa. Solo cambia cómo se presentan estos datos en sus pantallas de solo consulta, ver [pantallas-tramitacion.md](./pantallas-tramitacion.md).)*
