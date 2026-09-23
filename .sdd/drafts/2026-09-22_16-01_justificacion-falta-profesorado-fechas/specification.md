---
type: specification
template: expediente
---

# Objetivo

Permitir que el profesorado de un centro justifique ante el centro los días o las horas en que ha faltado a su puesto de trabajo.

# El trámite

- **Nombre visible:** Justificación de falta del profesorado
- **A qué colectivo va dirigido:** profesorado — es la categoría bajo la que el profesor lo encuentra en la lista de trámites.
- **Quién puede iniciarlo:** cualquier profesor del centro.
- **Para qué sirve:** el profesorado presenta ante el centro la justificación de los días o las horas en que ha faltado a su puesto de trabajo, indicando el periodo exacto de la ausencia y el motivo, y el centro resuelve si la acepta o la rechaza.
- **Texto de ayuda que ve el usuario antes de empezar** *(sin cambios en esta iniciativa)*:

  > Este trámite permite justificar la falta del profesorado.
  > Es solo para profesores.
  > Y puedo poner algo en **negrita**.

- **Versión:** modifica la versión v1 existente del trámite. Cambia cómo se indica el periodo de la ausencia: sustituye los días sueltos de un mes y un año por una fecha de inicio y, cuando aplica, una fecha de fin; y amplía las combinaciones de jornada faltada de 2 a 4 opciones (un día completo, varios días completos, unas horas de un único día, y varios días en los que el primero fue parcial). El resto del trámite (fases, estados, transiciones, firma, documento de resolución, seguridad y datos iniciales) no cambia.

# Actores y perfiles

*(Sin cambios respecto a la versión actual: esta iniciativa no toca los perfiles CREADOR ni TRAMITADOR ni quién los ostenta.)*

# Historias de usuario

## HU-001 — Como profesor quiero indicar el periodo exacto de mi ausencia (un día, varios días, o solo unas horas) para justificarla con precisión ante el centro

- ESC-001 — Justifica un día completo:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. El sistema abre el expediente en el estado RECEPCION / ENTRADA_DATOS, con sus datos personales ya rellenos.
  4. Elige el tipo de jornada faltada «Un día completo». El sistema muestra únicamente el campo titulado «Fecha» (no muestra fecha de fin ni horas).
  5. Rellena «Fecha» con «10/09/2026», el motivo de la falta «Traslado de domicilio» y adjunta el fichero «justificante.pdf».
  6. Pulsa «Siguiente».
  7. El sistema guarda los datos y el expediente queda en RECEPCION / PENDIENTE_PRESENTACION.

- ESC-002 — Justifica varios días completos:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Varios días (todos ellos completos)». El sistema muestra los campos titulados «Fecha de Inicio» y «Fecha de fin» (no muestra horas).
  4. Rellena «Fecha de Inicio» con «10/09/2026», «Fecha de fin» con «12/09/2026», el motivo de la falta «Enfermedad común» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema guarda los datos y el expediente queda en RECEPCION / PENDIENTE_PRESENTACION.

- ESC-003 — Justifica unas horas de un único día:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Unas horas de un único día». El sistema muestra el campo titulado «Fecha», junto con «Hora de inicio» y «Hora de fin» (no muestra fecha de fin).
  4. Rellena «Fecha» con «10/09/2026», «Hora de inicio» con «09:00», «Hora de fin» con «11:00», el motivo de la falta «Asistencia a pruebas selectivas y exámenes» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema guarda los datos y el expediente queda en RECEPCION / PENDIENTE_PRESENTACION.

- ESC-004 — Justifica varios días en los que el primero faltó solo unas horas:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas». El sistema muestra los campos titulados «Fecha de Inicio», «Hora de inicio» y «Fecha de fin» (no muestra hora de fin).
  4. Rellena «Fecha de Inicio» con «10/09/2026», «Hora de inicio» con «12:00», «Fecha de fin» con «11/09/2026», el motivo de la falta «Deber inexcusable» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema guarda los datos y el expediente queda en RECEPCION / PENDIENTE_PRESENTACION.

- ESC-005 — Se intenta continuar sin indicar la fecha:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Un día completo» y deja vacío el campo «Fecha».
  4. Rellena el motivo de la falta «Traslado de domicilio» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «Debe indicar la fecha» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-006 — Se intenta justificar varios días con la fecha de fin igual a la de inicio:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Varios días (todos ellos completos)».
  4. Rellena «Fecha de Inicio» con «10/09/2026» y «Fecha de fin» también con «10/09/2026», el motivo de la falta «Enfermedad común» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «La fecha de fin debe ser posterior a la fecha de inicio» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-007 — Se intenta justificar unas horas con la hora de fin anterior a la hora de inicio:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Unas horas de un único día».
  4. Rellena «Fecha» con «10/09/2026», «Hora de inicio» con «11:00», «Hora de fin» con «09:00», el motivo de la falta «Enfermedad común» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «La hora de fin debe ser posterior a la hora de inicio» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-008 — Se intenta justificar una falta de hace más de un año:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Un día completo» y rellena «Fecha» con «01/01/2020».
  4. Rellena el motivo de la falta «Enfermedad común» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «La fecha debe ser de los últimos 12 meses» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-009 — Se intenta justificar una falta con fecha futura:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Un día completo» y rellena «Fecha» con «01/01/2030».
  4. Rellena el motivo de la falta «Enfermedad común» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «La fecha no puede ser posterior a hoy» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-010 — Se intenta continuar sin elegir el tipo de jornada faltada:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Sin elegir ningún tipo de jornada faltada, rellena el motivo de la falta «Traslado de domicilio» y adjunta el fichero «justificante.pdf».
  4. Pulsa «Siguiente».
  5. El sistema muestra «Debe indicar el tipo de jornada faltada» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-011 — Se intenta justificar varios días completos sin indicar la fecha de fin:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Varios días (todos ellos completos)».
  4. Rellena «Fecha de Inicio» con «10/09/2026», deja vacío «Fecha de fin», rellena el motivo de la falta «Enfermedad común» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «Debe indicar la fecha de fin» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-012 — Se intenta justificar unas horas de un único día sin indicar la hora de inicio:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Unas horas de un único día».
  4. Rellena «Fecha» con «10/09/2026», deja vacío «Hora de inicio», rellena «Hora de fin» con «11:00», el motivo de la falta «Enfermedad común» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «Debe indicar la hora de inicio» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-013 — Se intenta justificar unas horas de un único día sin indicar la hora de fin:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Unas horas de un único día».
  4. Rellena «Fecha» con «10/09/2026», «Hora de inicio» con «09:00», deja vacío «Hora de fin», el motivo de la falta «Enfermedad común» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «Debe indicar la hora de fin» y el expediente sigue en RECEPCION / ENTRADA_DATOS.

- ESC-014 — Se corrige el periodo volviendo atrás y los datos de fecha y hora ya introducidos se conservan:
  1. El profesor «director@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Elige el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas». El sistema muestra los campos «Fecha de Inicio», «Hora de inicio» y «Fecha de fin».
  4. Rellena «Fecha de Inicio» con «10/09/2026», «Hora de inicio» con «12:00», «Fecha de fin» con «11/09/2026», el motivo de la falta «Deber inexcusable» y adjunta el fichero «justificante.pdf».
  5. Pulsa «Siguiente».
  6. El expediente queda en RECEPCION / PENDIENTE_PRESENTACION.
  7. Pulsa «Atrás».
  8. El sistema devuelve el expediente a RECEPCION / ENTRADA_DATOS mostrando ya elegido el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas», con «Fecha de Inicio» «10/09/2026», «Hora de inicio» «12:00» y «Fecha de fin» «11/09/2026» ya rellenos.

# Fases y estados

- **Estado en el que nace el expediente:** RECEPCION / ENTRADA_DATOS *(sin cambios)*
- **Estados que cierran el expediente:** TRAMITACION / ACEPTADO y TRAMITACION / RECHAZADO *(sin cambios)*
- **Desde qué estado se puede borrar el expediente:** RECEPCION / ENTRADA_DATOS *(sin cambios)*

El ciclo de vida (fases, estados y transiciones) no cambia respecto a la versión actual. Lo único que cambia es el detalle de la acción GUARDAR_DATOS del estado RECEPCION / ENTRADA_DATOS (los datos que introduce el profesor y sus comprobaciones), documentado en [estados.md](./estados.md).

| Fase | Título que ve el usuario | Estados | Pantallas |
|---|---|---|---|
| RECEPCION | Recepción | ENTRADA_DATOS, PENDIENTE_PRESENTACION | [pantallas-recepcion.md](./pantallas-recepcion.md) |
| TRAMITACION | Tramitación | PENDIENTE_RESOLUCION, ACEPTADO, RECHAZADO | [pantallas-tramitacion.md](./pantallas-tramitacion.md) |

# Pantallas

| Fichero | Fase | Qué pantallas contiene |
|---|---|---|
| [pantallas-recepcion.md](./pantallas-recepcion.md) | RECEPCION | Lo que cambia de la pantalla del profesor y de la de solo consulta del estado ENTRADA_DATOS. |
| [pantallas-tramitacion.md](./pantallas-tramitacion.md) | TRAMITACION | Lo que cambia de las pantallas de solo consulta de los estados PENDIENTE_RESOLUCION, ACEPTADO y RECHAZADO. |

# Documentos

El trámite sigue generando los mismos documentos (la solicitud y, si se rechaza, la resolución); de ellos cambia únicamente el contenido de la solicitud, que ahora presenta el periodo de la ausencia con fechas en vez de con los días sueltos de un mes y un año. Detalle en [documentos.md](./documentos.md).

# Registros de entrada y salida, y notificaciones

*(Sin cambios respecto a la versión actual: esta iniciativa no modifica cuándo se registra la entrada o la salida del trámite, ni los avisos que se envían.)*

# Seguridad

*(Sin cambios respecto a la versión actual: esta iniciativa no modifica quién puede ver o tramitar los expedientes de este trámite.)*

# Datos iniciales

*(Sin cambios respecto a la versión actual: no se necesita ningún dato maestro nuevo para este cambio.)*

# Fuera de alcance

- Marcar varios días sueltos no consecutivos dentro de un mismo periodo (lo que hoy permite el dato de los días del mes): es un cambio de comportamiento intencionado — el nuevo modelo solo admite un periodo continuo.
- Migrar los expedientes ya existentes de esta versión: se asume que la base de datos se vacía antes de desplegar este cambio.
- Cualquier otro aspecto del trámite no mencionado en esta especificación (fases, estados, transiciones, firma, registro, seguridad, datos iniciales): no cambia respecto a la versión actual.
