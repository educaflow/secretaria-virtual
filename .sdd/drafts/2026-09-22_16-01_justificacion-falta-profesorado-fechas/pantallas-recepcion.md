# Pantallas — Fase RECEPCION (delta)

Solo cambia el estado ENTRADA_DATOS, y solo el bloque de datos de la falta. El resto de la fase (estado PENDIENTE_PRESENTACION, y el resto de bloques de ENTRADA_DATOS: datos del profesor, adjuntar justificante) no cambia.

## Estado ENTRADA_DATOS

### Pantalla del perfil CREADOR

Qué cambia — qué puede rellenar (sustituye a los días del mes, el mes y el año):

- El tipo de jornada faltada.
- La fecha de inicio (su título cambia según el tipo de jornada faltada, ver regla RUI-ENTRADA_DATOS-CREADOR-004 abajo).
- La fecha de fin (solo se muestra en algunos casos).
- La hora de inicio (solo se muestra en algunos casos).
- La hora de fin (solo se muestra en algunos casos).

*(El resto del bloque de datos de la falta —motivo, explicación de «Otros»— no cambia.)*

Reglas de pantalla (RUI-):

- **RUI-ENTRADA_DATOS-CREADOR-001** — Se muestra la fecha de fin. disparador: al cambiar el tipo de jornada faltada. condición: el tipo de jornada faltada es «Varios días (todos ellos completos)» o «Varios días pero del primer día solo faltó unas horas».
- **RUI-ENTRADA_DATOS-CREADOR-002** — Se muestra la hora de inicio. disparador: al cambiar el tipo de jornada faltada. condición: el tipo de jornada faltada es «Unas horas de un único día» o «Varios días pero del primer día solo faltó unas horas».
- **RUI-ENTRADA_DATOS-CREADOR-003** — Se muestra la hora de fin. disparador: al cambiar el tipo de jornada faltada. condición: el tipo de jornada faltada es «Unas horas de un único día».
- **RUI-ENTRADA_DATOS-CREADOR-004** — El título de la fecha de inicio es «Fecha» en vez de «Fecha de Inicio». disparador: al cambiar el tipo de jornada faltada. condición: el tipo de jornada faltada es «Un día completo» o «Unas horas de un único día».
- **RUI-ENTRADA_DATOS-CREADOR-005** — El tipo de jornada faltada se marca visualmente como obligatorio. disparador: al abrir la pantalla. condición: siempre.
- **RUI-ENTRADA_DATOS-CREADOR-006** — La fecha de inicio se marca visualmente como obligatoria. disparador: al abrir la pantalla. condición: siempre.
- **RUI-ENTRADA_DATOS-CREADOR-007** — La fecha de fin se marca visualmente como obligatoria. disparador: al cambiar el tipo de jornada faltada. condición: la misma que RUI-ENTRADA_DATOS-CREADOR-001.
- **RUI-ENTRADA_DATOS-CREADOR-008** — La hora de inicio se marca visualmente como obligatoria. disparador: al cambiar el tipo de jornada faltada. condición: la misma que RUI-ENTRADA_DATOS-CREADOR-002.
- **RUI-ENTRADA_DATOS-CREADOR-009** — La hora de fin se marca visualmente como obligatoria. disparador: al cambiar el tipo de jornada faltada. condición: la misma que RUI-ENTRADA_DATOS-CREADOR-003.
- **RUI-ENTRADA_DATOS-CREADOR-010** — Al cambiar el tipo de jornada faltada, si la fecha de fin ya tenía un valor y el nuevo tipo deja de necesitarla, se limpia su valor. disparador: al cambiar el tipo de jornada faltada. condición: el nuevo tipo de jornada faltada es «Un día completo» o «Unas horas de un único día» y la fecha de fin tenía ya un valor.
- **RUI-ENTRADA_DATOS-CREADOR-011** — Al cambiar el tipo de jornada faltada, si la hora de inicio ya tenía un valor y el nuevo tipo deja de necesitarla, se limpia su valor. disparador: al cambiar el tipo de jornada faltada. condición: el nuevo tipo de jornada faltada es «Un día completo» o «Varios días (todos ellos completos)» y la hora de inicio tenía ya un valor.
- **RUI-ENTRADA_DATOS-CREADOR-012** — Al cambiar el tipo de jornada faltada, si la hora de fin ya tenía un valor y el nuevo tipo deja de necesitarla, se limpia su valor. disparador: al cambiar el tipo de jornada faltada. condición: el nuevo tipo de jornada faltada no es «Unas horas de un único día» y la hora de fin tenía ya un valor.

### Pantalla de solo consulta (resto de perfiles)

Qué cambia — qué solo puede consultar (mismo contenido que la pantalla del perfil CREADOR, mostrado en solo lectura):

- El tipo de jornada faltada.
- La fecha de inicio (mismo título dinámico que en la pantalla editable).
- La fecha de fin, la hora de inicio y la hora de fin, en los mismos casos que en la pantalla editable.

Reglas de pantalla (RUI-):

- **RUI-ENTRADA_DATOS-GENERICA-001** — Se muestra la fecha de fin. disparador: al abrir la pantalla. condición: la misma que RUI-ENTRADA_DATOS-CREADOR-001.
- **RUI-ENTRADA_DATOS-GENERICA-002** — Se muestra la hora de inicio. disparador: al abrir la pantalla. condición: la misma que RUI-ENTRADA_DATOS-CREADOR-002.
- **RUI-ENTRADA_DATOS-GENERICA-003** — Se muestra la hora de fin. disparador: al abrir la pantalla. condición: la misma que RUI-ENTRADA_DATOS-CREADOR-003.
- **RUI-ENTRADA_DATOS-GENERICA-004** — El título de la fecha de inicio es «Fecha» en vez de «Fecha de Inicio». disparador: al abrir la pantalla. condición: la misma que RUI-ENTRADA_DATOS-CREADOR-004.

*(El resto de la pantalla —datos del profesor, motivo, justificante— y el botón «Salir» no cambian.)*
