---
type: test-e2e
id: T-016
---

# T-016 — El profesor presenta la solicitud y el tramitador ve el periodo en la pantalla de resolución

**Origen ESC:** —
**Perfil:** `CREADOR` (login `director@mislata.es`) para presentar y para consultar la vista genérica, y `TRAMITADOR` (login `jefeestudios1@mislata.es`) para consultar su pantalla
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar con AutoFirma y Presentar la solicitud»
**Hasta:** `TRAMITACION` / `PENDIENTE_RESOLUCION`
**Tipo:** happy
**Manual:** sí — el botón «Firmar con AutoFirma y Presentar la solicitud» abre la aplicación de escritorio AutoFirma y exige el certificado digital del profesor instalado en su máquina; la carga de demo no trae ningún certificado.

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Origen del perfil |
|---|---|---|---|---|---|
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoTramite` (`CREADOR` / `PROFESOR` / `PROFESOR`) |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `AUDITOR` | `<aces>` del trámite (`AUDITOR` / cargo `DIRECTOR`) |
| `jefeestudios1@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `JEFE_ESTUDIOS` | CIPFP Mislata | `TRAMITADOR` | security `AceProfileTipoTramite` (`TRAMITADOR` / cargo `JEFE_ESTUDIOS` / `PROFESOR`) |

Los tres son del **mismo centro** (CIPFP Mislata), que es el del expediente: esta iniciativa no toca nada multicentro y no hay ningún test de aislamiento.

### Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y perfiles, más el trámite «Justificación de falta del profesorado» publicado en el árbol de trámites del centro, bajo la categoría del profesorado. Ningún test puede presuponer más estado que este.

La demo **no** carga ningún certificado digital, así que en `RECEPCION` / `PENDIENTE_PRESENTACION` la solicitud solo se puede firmar con AutoFirma en el equipo del profesor, y la resolución exige el certificado del director del centro instalado en el servidor. Los tests que atraviesan esos dos pasos van marcados `Manual: sí`.

#### Juego de datos válido — fase `RECEPCION`, estado `ENTRADA_DATOS`

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

#### Juego de datos válido — fase `TRAMITACION`, estado `PENDIENTE_RESOLUCION`

| campo | valor |
|---|---|
| «Tipo resolución» | Resolver positivamente *(o «Resolver negativamente(Rechazar)»)* |
| «Motivo del rechazo» | Los días indicados no constan como falta *(solo al resolver negativamente)* |

## Pasos

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) tiene un expediente de «Justificación de falta del profesorado» en `RECEPCION` / `PENDIENTE_PRESENTACION`, creado con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio» 10/09/2026, «Fecha de fin» 12/09/2026, «Motivo falta» «Enfermedad común» y `justificante.pdf` adjunto, y tiene AutoFirma instalado con un certificado válido cuyo DNI es el suyo.
- **When** pulsa «Firmar con AutoFirma y Presentar la solicitud», confirma el aviso de que no podrá deshacer la acción y firma en AutoFirma.
- **Then** el expediente pasa a la fase `TRAMITACION`, estado `PENDIENTE_RESOLUCION`.
- **And** en el PDF de la solicitud, el bloque «Declara que» tiene marcada la casilla «Desde el 10/09/2026 hasta el 12/09/2026» y las otras tres casillas de tipo de jornada están sin marcar.
- **And** al iniciar sesión `jefeestudios1@mislata.es` (contraseña `demo1234`) y abrir el expediente por la bandeja «Expedientes esperando a que otra persona realice una tarea», la pantalla del perfil `TRAMITADOR` muestra en solo lectura el panel «Datos de la falta» con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio» a 10/09/2026 y «Fecha de fin» a 12/09/2026, sin «Hora de inicio» ni «Hora de fin», y ofrece el botón «Resolver el expediente».
- **And** al iniciar sesión de nuevo `director@mislata.es` (contraseña `demo1234`) y abrir el expediente **entrando por la bandeja «Expedientes Pendientes»** («Listado de expedientes pendientes de que realices la tarea»), que es la del perfil `CREADOR`, como `PENDIENTE_RESOLUCION` no tiene pantalla para ese perfil el sistema abre la vista genérica de solo consulta: el panel «Datos de la falta» sale en solo lectura con el tipo de jornada faltada «Varios días (todos ellos completos)», el campo de la fecha titulado «Fecha de Inicio» (no «Fecha») a 10/09/2026 y «Fecha de fin» a 12/09/2026, sin «Hora de inicio» ni «Hora de fin»; no hay ningún campo editable, el único botón es «Salir» y el expediente **sigue** en `TRAMITACION` / `PENDIENTE_RESOLUCION`.
