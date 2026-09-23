# Tests E2E — Justificación de falta del profesorado (`JustificacionFaltaProfesoradoV1`)

Tests en lenguaje de negocio, Given/When/Then, materializados a partir de los escenarios (`ESC-NNN`) de la especificación y de la tabla de transiciones del as-is (`actual/v1/TipoExpedienteInstance.xml`), que esta iniciativa no cambia. **Sin código Playwright y sin selectores.**

Esta iniciativa es una **modificación** de la versión `v1` existente, así que la cobertura se mide **solo sobre el delta**: el arranque del expediente, la acción `GUARDAR_DATOS` de `RECEPCION` / `ENTRADA_DATOS` con sus comprobaciones, la vuelta atrás desde `PENDIENTE_PRESENTACION`, y las pantallas de solo consulta que pasan a mostrar el periodo con el formato nuevo, y la de solo consulta de `PENDIENTE_PRESENTACION`, que gana el aviso del estado del expediente (`design.md` §14). **No** se describen tests de `DELETE` ni de las ramas de `RESOLVER` a subsanación, que el delta no toca.

## Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Origen del perfil |
|---|---|---|---|---|---|
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoTramite` (`CREADOR` / `PROFESOR` / `PROFESOR`) |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `AUDITOR` | `<aces>` del trámite (`AUDITOR` / cargo `DIRECTOR`) |
| `jefeestudios1@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `JEFE_ESTUDIOS` | CIPFP Mislata | `TRAMITADOR` | security `AceProfileTipoTramite` (`TRAMITADOR` / cargo `JEFE_ESTUDIOS` / `PROFESOR`) |

Los tres son del **mismo centro** (CIPFP Mislata), que es el del expediente: esta iniciativa no toca nada multicentro y no hay ningún test de aislamiento.

## Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y perfiles, más el trámite «Justificación de falta del profesorado» publicado en el árbol de trámites del centro, bajo la categoría del profesorado. Ningún test puede presuponer más estado que este.

La demo **no** carga ningún certificado digital, así que en `RECEPCION` / `PENDIENTE_PRESENTACION` la solicitud solo se puede firmar con AutoFirma en el equipo del profesor, y la resolución exige el certificado del director del centro instalado en el servidor. Los tests que atraviesan esos dos pasos van marcados `Manual: sí`.

### Juego de datos válido — fase `RECEPCION`, estado `ENTRADA_DATOS`

Un juego por cada tipo de jornada faltada; los tests del camino feliz usan el suyo.

| campo | «Un día completo» | «Varios días (todos ellos completos)» | «Unas horas de un único día» | «Varios días pero del primer día solo faltó unas horas» |
|---|---|---|---|---|
| «Tipo de jornada faltada» | Un día completo | Varios días (todos ellos completos) | Unas horas de un único día | Varios días pero del primer día solo faltó unas horas |
| «Fecha» / «Fecha de Inicio» | 10/09/2026 | 10/09/2026 | 10/09/2026 | 10/09/2026 |
| «Fecha de fin» | *(no se muestra)* | 12/09/2026 | *(no se muestra)* | 11/09/2026 |
| «Hora de inicio» | *(no se muestra)* | *(no se muestra)* | 09:00 | 12:00 |
| «Hora de fin» | *(no se muestra)* | *(no se muestra)* | 11:00 | *(no se muestra)* |
| «Motivo falta» | Traslado de domicilio | Enfermedad común | Asistencia a pruebas selectivas y exámenes | Deber inexcusable |
| «Foto o PDF del justificante» | `justificante.pdf` | `justificante.pdf` | `justificante.pdf` | `justificante.pdf` |

El justificante es un **PDF pequeño** (menos de 1 MB), llamado `justificante.pdf`. Vale igualmente una imagen PNG, JPEG o GIF de menos de 5 MB.

### Juego de datos válido — fase `TRAMITACION`, estado `PENDIENTE_RESOLUCION`

| campo | valor |
|---|---|
| «Tipo resolución» | Resolver positivamente *(o «Resolver negativamente(Rechazar)»)* |
| «Motivo del rechazo» | Los días indicados no constan como falta *(solo al resolver negativamente)* |

## Cobertura de transiciones

Solo las filas del as-is que el delta atraviesa.

| # | fase origen | estado origen | evento | guarda | fase destino | estado destino | perfil | test |
|---|---|---|---|---|---|---|---|---|
| 1 | `[*]` | `[*]` | — | — | `RECEPCION` | `ENTRADA_DATOS` | `CREADOR` | T-001 (y el arranque de T-002..T-015) |
| 2 | `RECEPCION` | `ENTRADA_DATOS` | `GUARDAR_DATOS` | — | `RECEPCION` | `PENDIENTE_PRESENTACION` | `CREADOR` | T-001, T-002, T-003, T-004 |
| 3 | `RECEPCION` | `PENDIENTE_PRESENTACION` | `BACK` | — | `RECEPCION` | `ENTRADA_DATOS` | `CREADOR` | T-014 |
| 4 | `RECEPCION` | `PENDIENTE_PRESENTACION` | `PRESENTAR` | — | `TRAMITACION` | `PENDIENTE_RESOLUCION` | `CREADOR` | T-016 (no-regresión del camino que lleva a las pantallas modificadas) |
| 5 | `TRAMITACION` | `PENDIENTE_RESOLUCION` | `RESOLVER` | `tipoResolucion=ACEPTAR` | `TRAMITACION` | `ACEPTADO` | `TRAMITADOR` | T-017 |
| 6 | `TRAMITACION` | `PENDIENTE_RESOLUCION` | `RESOLVER` | `tipoResolucion=RECHAZAR` | `TRAMITACION` | `RECHAZADO` | `TRAMITADOR` | T-018 |

Transiciones del as-is **fuera** del delta y por tanto sin test: `ENTRADA_DATOS` → `[*]` (`DELETE`) y `RESOLVER` con la guarda `tipoResolucion=SUBSANAR_DATOS`. Ninguna de las dos toca el periodo ni ninguna pantalla modificada.

Tests de validación fallida: T-005, T-006, T-007, T-008, T-009, T-010, T-011, T-012, T-013.
Tests de vistas genéricas de solo lectura: T-001 (`PENDIENTE_PRESENTACION`, en su último And), T-015 (`ENTRADA_DATOS`), T-016 (`PENDIENTE_RESOLUCION`, en su último And), T-017 (`ACEPTADO`), T-018 (`RECHAZADO`).
Tests de comportamiento cliente (`onChange` de «Tipo de jornada faltada», sin disparar ningún evento): T-019.
Tests **manuales** (no automatizables, §4.1): T-016, T-017, T-018.

---

## T-001 — Justifica un día completo

**Origen ESC:** ESC-001
**Perfil:** `CREADOR` (login `director@mislata.es`) para rellenar y guardar, y `TRAMITADOR` (login `jefeestudios1@mislata.es`) para consultar la vista genérica de `PENDIENTE_PRESENTACION` en el último And
**Desde:** `[*]`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Tipo:** happy
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo; el sistema lo abre en la fase `RECEPCION`, estado `ENTRADA_DATOS`, con el panel «Datos del profesor interesado» ya relleno con sus apellidos, su nombre y su DNI, y con el panel «Datos de la falta» sin ningún dato del periodo precargado.
- **When** elige el tipo de jornada faltada «Un día completo», rellena «Fecha» con 10/09/2026 y «Motivo falta» con «Traslado de domicilio», adjunta `justificante.pdf` en «Foto o PDF del justificante» y pulsa «Siguiente».
- **Then** el expediente queda en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`, y la cabecera muestra «Recepción» y «Pendiente de presentación».
- **And** antes de pulsar «Siguiente», el panel «Datos de la falta» muestra el campo titulado «Fecha» y **no** muestra «Fecha de fin», ni «Hora de inicio», ni «Hora de fin».
- **And** la nueva pantalla muestra la solicitud generada en PDF y ofrece los botones «Atrás» y el de firmar y presentar.
- **And** tras cerrar sesión el profesor, `jefeestudios1@mislata.es` (contraseña `demo1234`) inicia sesión y abre ese expediente **entrando por la bandeja «Expedientes esperando a que otra persona realice una tarea»**, que es la del perfil `TRAMITADOR`; como `PENDIENTE_PRESENTACION` no tiene pantalla para ese perfil, el sistema abre la vista genérica de solo consulta, que muestra la solicitud en PDF y el aviso «La solicitud está pendiente de que el profesor la firme y la presente»; no hay ningún campo editable, el único botón es «Salir» y el expediente **sigue** en `RECEPCION` / `PENDIENTE_PRESENTACION`.

---

## T-002 — Justifica varios días completos

**Origen ESC:** ESC-002
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Tipo:** happy
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Varios días (todos ellos completos)», rellena «Fecha de Inicio» con 10/09/2026 y «Fecha de fin» con 12/09/2026, «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el expediente queda en `RECEPCION` / `PENDIENTE_PRESENTACION`.
- **And** al elegir el tipo, el panel «Datos de la falta» pasa a mostrar «Fecha de Inicio» y «Fecha de fin», y **no** muestra ni «Hora de inicio» ni «Hora de fin».

---

## T-003 — Justifica unas horas de un único día

**Origen ESC:** ESC-003
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Tipo:** happy
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Unas horas de un único día», rellena «Fecha» con 10/09/2026, «Hora de inicio» con 09:00 y «Hora de fin» con 11:00, «Motivo falta» con «Asistencia a pruebas selectivas y exámenes», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el expediente queda en `RECEPCION` / `PENDIENTE_PRESENTACION`.
- **And** al elegir el tipo, el panel «Datos de la falta» muestra «Fecha», «Hora de inicio» y «Hora de fin», y **no** muestra «Fecha de fin».

---

## T-004 — Justifica varios días en los que el primero faltó solo unas horas

**Origen ESC:** ESC-004
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Tipo:** happy
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas», rellena «Fecha de Inicio» con 10/09/2026, «Hora de inicio» con 12:00 y «Fecha de fin» con 11/09/2026, «Motivo falta» con «Deber inexcusable», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el expediente queda en `RECEPCION` / `PENDIENTE_PRESENTACION`.
- **And** al elegir el tipo, el panel «Datos de la falta» muestra «Fecha de Inicio», «Hora de inicio» y «Fecha de fin», y **no** muestra «Hora de fin».

---

## T-005 — No se puede continuar sin elegir el tipo de jornada faltada

**Origen ESC:** ESC-010
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** sin elegir ningún tipo de jornada faltada, rellena «Motivo falta» con «Traslado de domicilio», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «Debe indicar el tipo de jornada faltada» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.
- **And** mientras no hay tipo de jornada faltada elegido, el panel «Datos de la falta» muestra el campo «Fecha de Inicio», vacío y marcado como obligatorio, y **no** muestra «Fecha de fin», ni «Hora de inicio», ni «Hora de fin».

---

## T-006 — No se puede continuar sin indicar la fecha

**Origen ESC:** ESC-005
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Un día completo», deja «Fecha» vacía, rellena «Motivo falta» con «Traslado de domicilio», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «Debe indicar la fecha» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.

---

## T-007 — No se puede justificar una falta de hace más de un año

**Origen ESC:** ESC-008
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Un día completo», rellena «Fecha» con 01/01/2020, «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «La fecha debe ser de los últimos 12 meses» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.

---

## T-008 — No se puede justificar una falta con fecha futura

**Origen ESC:** ESC-009
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Un día completo», rellena «Fecha» con 01/01/2030, «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «La fecha no puede ser posterior a hoy» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.

---

## T-009 — No se pueden justificar varios días completos sin indicar la fecha de fin

**Origen ESC:** ESC-011
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Varios días (todos ellos completos)», rellena «Fecha de Inicio» con 10/09/2026, deja «Fecha de fin» vacía, rellena «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «Debe indicar la fecha de fin» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.

---

## T-010 — No se pueden justificar varios días con la fecha de fin igual a la de inicio

**Origen ESC:** ESC-006
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Varios días (todos ellos completos)», rellena «Fecha de Inicio» y «Fecha de fin» con la misma fecha, 10/09/2026, rellena «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «La fecha de fin debe ser posterior a la fecha de inicio» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.

---

## T-011 — No se pueden justificar unas horas sin indicar la hora de inicio

**Origen ESC:** ESC-012
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Unas horas de un único día», rellena «Fecha» con 10/09/2026, deja «Hora de inicio» vacía, rellena «Hora de fin» con 11:00 y «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «Debe indicar la hora de inicio» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.

---

## T-012 — No se pueden justificar unas horas sin indicar la hora de fin

**Origen ESC:** ESC-013
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Unas horas de un único día», rellena «Fecha» con 10/09/2026 y «Hora de inicio» con 09:00, deja «Hora de fin» vacía, rellena «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «Debe indicar la hora de fin» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.

---

## T-013 — No se pueden justificar unas horas con la hora de fin anterior a la de inicio

**Origen ESC:** ESC-007
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Unas horas de un único día», rellena «Fecha» con 10/09/2026, «Hora de inicio» con 11:00, «Hora de fin» con 09:00 y «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «La hora de fin debe ser posterior a la hora de inicio» y el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS`.

---

## T-014 — Al volver atrás se conservan la fecha y la hora ya introducidas

**Origen ESC:** ESC-014
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `BACK` — botón «Atrás»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** happy
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado», en `RECEPCION` / `ENTRADA_DATOS` ha elegido el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas» con «Fecha de Inicio» 10/09/2026, «Hora de inicio» 12:00 y «Fecha de fin» 11/09/2026, «Motivo falta» «Deber inexcusable» y `justificante.pdf` adjunto, y ha pulsado «Siguiente», de modo que el expediente está en `RECEPCION` / `PENDIENTE_PRESENTACION`.
- **When** pulsa «Atrás».
- **Then** el expediente vuelve a `RECEPCION` / `ENTRADA_DATOS`.
- **And** el panel «Datos de la falta» muestra ya elegido el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas», con «Fecha de Inicio» a 10/09/2026, «Hora de inicio» a 12:00 y «Fecha de fin» a 11/09/2026.

---

## T-015 — La vista de solo consulta de ENTRADA_DATOS muestra el periodo sin poder tocarlo

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `jefeestudios1@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** — (solo se abre la pantalla)
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y, en `RECEPCION` / `ENTRADA_DATOS`, ha elegido el tipo de jornada faltada «Unas horas de un único día» con «Fecha» 10/09/2026, «Hora de inicio» 09:00 y «Hora de fin» 11:00, «Motivo falta» «Asistencia a pruebas selectivas y exámenes» y `justificante.pdf` adjunto; ha pulsado «Siguiente» (el expediente pasa a `RECEPCION` / `PENDIENTE_PRESENTACION`) y después «Atrás», con lo que el expediente vuelve a `RECEPCION` / `ENTRADA_DATOS` con esos datos ya guardados, y cierra sesión; después `jefeestudios1@mislata.es` (contraseña `demo1234`) inicia sesión y abre ese expediente **entrando por la bandeja «Expedientes esperando a que otra persona realice una tarea»**, que es la del perfil `TRAMITADOR`; como `ENTRADA_DATOS` no tiene pantalla para ese perfil, el sistema abre la vista genérica de solo consulta.
- **When** mira la pantalla.
- **Then** el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS` y la cabecera muestra «Recepción» y «Entrada de datos».
- **And** el panel «Datos de la falta» muestra, en solo lectura, el tipo de jornada faltada «Unas horas de un único día», «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00, y **no** muestra «Fecha de fin»; no hay ningún campo editable y el único botón es «Salir».

---

## T-016 — El profesor presenta la solicitud y el tramitador ve el periodo en la pantalla de resolución

**Origen ESC:** —
**Perfil:** `CREADOR` (login `director@mislata.es`) para presentar y para consultar la vista genérica, y `TRAMITADOR` (login `jefeestudios1@mislata.es`) para consultar su pantalla
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar con AutoFirma y Presentar la solicitud»
**Hasta:** `TRAMITACION` / `PENDIENTE_RESOLUCION`
**Tipo:** happy
**Manual:** sí — el botón «Firmar con AutoFirma y Presentar la solicitud» abre la aplicación de escritorio AutoFirma y exige el certificado digital del profesor instalado en su máquina; la carga de demo no trae ningún certificado.

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) tiene un expediente de «Justificación de falta del profesorado» en `RECEPCION` / `PENDIENTE_PRESENTACION`, creado con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio» 10/09/2026, «Fecha de fin» 12/09/2026, «Motivo falta» «Enfermedad común» y `justificante.pdf` adjunto, y tiene AutoFirma instalado con un certificado válido cuyo DNI es el suyo.
- **When** pulsa «Firmar con AutoFirma y Presentar la solicitud», confirma el aviso de que no podrá deshacer la acción y firma en AutoFirma.
- **Then** el expediente pasa a la fase `TRAMITACION`, estado `PENDIENTE_RESOLUCION`.
- **And** en el PDF de la solicitud, el bloque «Declara que» tiene marcada la casilla «Desde el 10/09/2026 hasta el 12/09/2026» y las otras tres casillas de tipo de jornada están sin marcar.
- **And** al iniciar sesión `jefeestudios1@mislata.es` (contraseña `demo1234`) y abrir el expediente por la bandeja «Expedientes esperando a que otra persona realice una tarea», la pantalla del perfil `TRAMITADOR` muestra en solo lectura el panel «Datos de la falta» con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio» a 10/09/2026 y «Fecha de fin» a 12/09/2026, sin «Hora de inicio» ni «Hora de fin», y ofrece el botón «Resolver el expediente».
- **And** al iniciar sesión de nuevo `director@mislata.es` (contraseña `demo1234`) y abrir el expediente **entrando por la bandeja «Expedientes Pendientes»** («Listado de expedientes pendientes de que realices la tarea»), que es la del perfil `CREADOR`, como `PENDIENTE_RESOLUCION` no tiene pantalla para ese perfil el sistema abre la vista genérica de solo consulta: el panel «Datos de la falta» sale en solo lectura con el tipo de jornada faltada «Varios días (todos ellos completos)», el campo de la fecha titulado «Fecha de Inicio» (no «Fecha») a 10/09/2026 y «Fecha de fin» a 12/09/2026, sin «Hora de inicio» ni «Hora de fin»; no hay ningún campo editable, el único botón es «Salir» y el expediente **sigue** en `TRAMITACION` / `PENDIENTE_RESOLUCION`.

---

## T-017 — El tramitador acepta la justificación y el estado cerrado sigue mostrando el periodo

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `jefeestudios1@mislata.es`)
**Desde:** `TRAMITACION` / `PENDIENTE_RESOLUCION`
**Evento:** `RESOLVER` — botón «Resolver el expediente» (guarda `tipoResolucion=ACEPTAR`)
**Hasta:** `TRAMITACION` / `ACEPTADO`
**Tipo:** happy
**Manual:** sí — llegar a `PENDIENTE_RESOLUCION` exige presentar con AutoFirma (T-016) y resolver exige el certificado digital del director del centro instalado en el servidor; la carga de demo no trae ninguno de los dos.

- **Given** existe un expediente de «Justificación de falta del profesorado» del centro CIPFP Mislata en `TRAMITACION` / `PENDIENTE_RESOLUCION`, presentado con el tipo de jornada faltada «Unas horas de un único día», «Fecha» 10/09/2026, «Hora de inicio» 09:00 y «Hora de fin» 11:00; `jefeestudios1@mislata.es` (contraseña `demo1234`) ha iniciado sesión, lo abre por la bandeja «Expedientes esperando a que otra persona realice una tarea» y el centro tiene instalado el certificado digital de su director.
- **When** elige «Tipo resolución» «Resolver positivamente», pulsa «Resolver el expediente» y confirma el aviso de que no podrá deshacer la acción.
- **Then** el expediente pasa a la fase `TRAMITACION`, estado `ACEPTADO`, que es un estado cerrado.
- **And** la pantalla de `ACEPTADO` es la genérica de solo consulta: muestra el panel «Datos de la falta» en solo lectura con «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00, sin «Fecha de fin»; muestra la resolución en PDF y el bloque «Resolución»; no ofrece ningún evento y su único botón es «Salir».

---

## T-018 — El tramitador rechaza la justificación y el estado cerrado sigue mostrando el periodo

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `jefeestudios1@mislata.es`)
**Desde:** `TRAMITACION` / `PENDIENTE_RESOLUCION`
**Evento:** `RESOLVER` — botón «Resolver el expediente» (guarda `tipoResolucion=RECHAZAR`)
**Hasta:** `TRAMITACION` / `RECHAZADO`
**Tipo:** happy
**Manual:** sí — llegar a `PENDIENTE_RESOLUCION` exige presentar con AutoFirma (T-016) y resolver exige el certificado digital del director del centro instalado en el servidor; la carga de demo no trae ninguno de los dos.

- **Given** existe un expediente de «Justificación de falta del profesorado» del centro CIPFP Mislata en `TRAMITACION` / `PENDIENTE_RESOLUCION`, presentado con el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas», «Fecha de Inicio» 10/09/2026, «Hora de inicio» 12:00 y «Fecha de fin» 11/09/2026; `jefeestudios1@mislata.es` (contraseña `demo1234`) ha iniciado sesión, lo abre por la bandeja «Expedientes esperando a que otra persona realice una tarea» y el centro tiene instalado el certificado digital de su director.
- **When** elige «Tipo resolución» «Resolver negativamente(Rechazar)», rellena «Motivo del rechazo» con «Los días indicados no constan como falta», pulsa «Resolver el expediente» y confirma el aviso.
- **Then** el expediente pasa a la fase `TRAMITACION`, estado `RECHAZADO`, que es un estado cerrado.
- **And** la pantalla de `RECHAZADO` es la genérica de solo consulta: muestra el panel «Datos de la falta» en solo lectura con «Fecha de Inicio» a 10/09/2026, «Hora de inicio» a 12:00 y «Fecha de fin» a 11/09/2026, sin «Hora de fin»; muestra la resolución en PDF con el motivo del rechazo; no ofrece ningún evento y su único botón es «Salir».

---

## T-019 — Al cambiar el tipo de jornada faltada se vacían los campos que el nuevo tipo ya no necesita

**Origen ESC:** —
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** — (solo cambia el selector «Tipo de jornada faltada», sin pulsar ningún botón)
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** happy
**Manual:** no

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`; en el panel «Datos de la falta» ha elegido el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas» y ha rellenado «Fecha de Inicio» con 10/09/2026, «Hora de inicio» con 12:00 y «Fecha de fin» con 11/09/2026, sin pulsar «Siguiente».
- **When** cambia el tipo de jornada faltada a «Un día completo».
- **Then** el panel «Datos de la falta» deja de mostrar «Fecha de fin» y «Hora de inicio», y solo muestra «Fecha» (con el título «Fecha», no «Fecha de Inicio»), que sigue mostrando 10/09/2026; el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS` sin haberse guardado nada.
- **And** vuelve a elegir el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas»: el panel vuelve a mostrar «Fecha de Inicio», «Hora de inicio» y «Fecha de fin»; «Fecha de Inicio» conserva 10/09/2026, pero «Hora de inicio» y «Fecha de fin» aparecen **vacías** — no conservan los valores 12:00 y 11/09/2026 que tenían antes del primer cambio.
