# Pantallas — Fase TRAMITACION (delta)

En los tres estados de esta fase el bloque de datos de la falta se muestra siempre en solo lectura, y cambia de la misma forma que en RECEPCION: se sustituyen los días del mes, el mes y el año por la fecha de inicio (con su título dinámico), y se muestran o no la fecha de fin, la hora de inicio y la hora de fin según el tipo de jornada faltada. El resto de cada pantalla (datos del profesor, documentos, bloque de resolución) no cambia.

## Estado PENDIENTE_RESOLUCION

### Pantalla del perfil TRAMITADOR

Qué cambia — qué solo puede consultar:

- El tipo de jornada faltada.
- La fecha de inicio (título dinámico).
- La fecha de fin, la hora de inicio y la hora de fin, según el tipo de jornada faltada.

Reglas de pantalla (RUI-):

- **RUI-PENDIENTE_RESOLUCION-TRAMITADOR-001** — Se muestra la fecha de fin. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Varios días (todos ellos completos)» o «Varios días pero del primer día solo faltó unas horas».
- **RUI-PENDIENTE_RESOLUCION-TRAMITADOR-002** — Se muestra la hora de inicio. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Unas horas de un único día» o «Varios días pero del primer día solo faltó unas horas».
- **RUI-PENDIENTE_RESOLUCION-TRAMITADOR-003** — Se muestra la hora de fin. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Unas horas de un único día».
- **RUI-PENDIENTE_RESOLUCION-TRAMITADOR-004** — El título de la fecha de inicio es «Fecha» en vez de «Fecha de Inicio». disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Un día completo» o «Unas horas de un único día».

### Pantalla de solo consulta (resto de perfiles)

Mismo cambio que la pantalla del perfil TRAMITADOR.

Reglas de pantalla (RUI-):

- **RUI-PENDIENTE_RESOLUCION-GENERICA-001** — Se muestra la fecha de fin. disparador: al abrir la pantalla. condición: la misma que RUI-PENDIENTE_RESOLUCION-TRAMITADOR-001.
- **RUI-PENDIENTE_RESOLUCION-GENERICA-002** — Se muestra la hora de inicio. disparador: al abrir la pantalla. condición: la misma que RUI-PENDIENTE_RESOLUCION-TRAMITADOR-002.
- **RUI-PENDIENTE_RESOLUCION-GENERICA-003** — Se muestra la hora de fin. disparador: al abrir la pantalla. condición: la misma que RUI-PENDIENTE_RESOLUCION-TRAMITADOR-003.
- **RUI-PENDIENTE_RESOLUCION-GENERICA-004** — El título de la fecha de inicio es «Fecha» en vez de «Fecha de Inicio». disparador: al abrir la pantalla. condición: la misma que RUI-PENDIENTE_RESOLUCION-TRAMITADOR-004.

## Estado ACEPTADO

Estado cerrado, sin perfil con turno: solo tiene la pantalla de solo consulta.

Reglas de pantalla (RUI-):

- **RUI-ACEPTADO-GENERICA-001** — Se muestra la fecha de fin. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Varios días (todos ellos completos)» o «Varios días pero del primer día solo faltó unas horas».
- **RUI-ACEPTADO-GENERICA-002** — Se muestra la hora de inicio. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Unas horas de un único día» o «Varios días pero del primer día solo faltó unas horas».
- **RUI-ACEPTADO-GENERICA-003** — Se muestra la hora de fin. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Unas horas de un único día».
- **RUI-ACEPTADO-GENERICA-004** — El título de la fecha de inicio es «Fecha» en vez de «Fecha de Inicio». disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Un día completo» o «Unas horas de un único día».

## Estado RECHAZADO

Estado cerrado, sin perfil con turno: solo tiene la pantalla de solo consulta.

Reglas de pantalla (RUI-):

- **RUI-RECHAZADO-GENERICA-001** — Se muestra la fecha de fin. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Varios días (todos ellos completos)» o «Varios días pero del primer día solo faltó unas horas».
- **RUI-RECHAZADO-GENERICA-002** — Se muestra la hora de inicio. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Unas horas de un único día» o «Varios días pero del primer día solo faltó unas horas».
- **RUI-RECHAZADO-GENERICA-003** — Se muestra la hora de fin. disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Unas horas de un único día».
- **RUI-RECHAZADO-GENERICA-004** — El título de la fecha de inicio es «Fecha» en vez de «Fecha de Inicio». disparador: al abrir la pantalla. condición: el tipo de jornada faltada es «Un día completo» o «Unas horas de un único día».
