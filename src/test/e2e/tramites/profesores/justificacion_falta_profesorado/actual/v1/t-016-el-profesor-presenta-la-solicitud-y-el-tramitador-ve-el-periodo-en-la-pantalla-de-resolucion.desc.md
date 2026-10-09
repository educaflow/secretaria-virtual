---
type: test-e2e
id: T-016
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-016-el-profesor-presenta-la-solicitud-y-el-tramitador-ve-el-periodo-en-la-pantalla-de-resolucion.desc.md
     Iniciativa: 2026-09-22_16-01_justificacion-falta-profesorado-fechas
     Test: T-016  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-016 — El profesor presenta la solicitud, la jefatura la verifica y la dirección ve el periodo en la pantalla de resolución

**Origen ESC:** —
**Perfil:** `CREADOR` (login `director@mislata.es`) para presentar, `TRAMITADOR` (login `jefeestudios1@mislata.es`) para consultar su pantalla, verificar y consultar la vista genérica, y `DIRECTOR` (el mismo `director@mislata.es`) para consultar la pantalla de resolución
**Desde:** `ENTRADA` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar y Presentar la solicitud» (la firma la hace el servidor con el certificado del profesor); después, para llegar a la pantalla de resolución, `VERIFICAR` — botón «Siguiente» de la jefatura (guarda `resultadoVerificacion=CORRECTO`)
**Hasta:** `VERIFICACION` / `PENDIENTE_VERIFICACION` (tras `PRESENTAR`) y `RESOLUCION` / `PENDIENTE_RESOLUCION` (tras `VERIFICAR`)
**Tipo:** happy

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Origen del perfil |
|---|---|---|---|---|---|
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoUsuarioTramite` (`CREADOR` / `PROFESOR` / `PROFESOR`) |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `AUDITOR` | `<acl>` del trámite (`AUDITOR` / cargo `DIRECTOR`) |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `DIRECTOR` | security `AceProfileGlobal` (`DIRECTOR` / cargo `DIRECTOR`) |
| `jefeestudios1@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `JEFE_ESTUDIOS` | CIPFP Mislata | `TRAMITADOR` | security `AceProfileTipoUsuarioTramite` (`TRAMITADOR` / cargo `JEFE_ESTUDIOS` / `PROFESOR`) |

Todos son del **mismo centro** (CIPFP Mislata), que es el del expediente: esta iniciativa no toca nada multicentro y no hay ningún test de aislamiento.

### Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y perfiles, más el trámite «Justificación de falta del profesorado» publicado en el árbol de trámites del centro, bajo la categoría del profesorado. Ningún test puede presuponer más estado que este.

El servidor tiene instalados los certificados digitales de los DNI de los usuarios, así que en `ENTRADA` / `PENDIENTE_PRESENTACION` la solicitud la firma el propio servidor con el certificado del profesor, sin intervención manual.

#### Juego de datos válido — fase `ENTRADA`, estado `ENTRADA_DATOS`

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

#### Juego de datos válido — fase `VERIFICACION`, estado `PENDIENTE_VERIFICACION`

| campo | valor |
|---|---|
| «Resultado de la verificación» | La solicitud es correcta *(o «Pedir subsanación»)* |
| «Qué hay que subsanar» | Falta la segunda página del justificante *(solo al pedir subsanación)* |

#### Juego de datos válido — fase `RESOLUCION`, estado `PENDIENTE_RESOLUCION`

| campo | valor |
|---|---|
| «Tipo resolución» | Resolver positivamente *(o «Resolver negativamente(Rechazar)» o «Devolver a jefatura de estudios»)* |
| «Motivo del rechazo» | Los días indicados no constan como falta *(solo al resolver negativamente)* |
| «Motivo de la devolución» | El justificante no corresponde a los días indicados *(solo al devolver a jefatura de estudios)* |

## Pasos

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) tiene un expediente de «Justificación de falta del profesorado» en `ENTRADA` / `PENDIENTE_PRESENTACION`, creado con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio» 10/09/2026, «Fecha de fin» 12/09/2026, «Motivo falta» «Enfermedad común» y `justificante.pdf` adjunto, y el servidor tiene el certificado digital de su DNI (la pantalla muestra el panel «Firma de la solicitud» y el botón «Firmar y Presentar la solicitud»).
- **When** pulsa «Firmar y Presentar la solicitud» y confirma el aviso de que no podrá deshacer la acción; el servidor firma la solicitud.
- **Then** el expediente pasa a la fase `VERIFICACION`, estado `PENDIENTE_VERIFICACION`, y la cabecera muestra «Verificación» y «Pendiente de verificación».
- **And** en el PDF de la solicitud, el bloque «Declara que» solo pinta la opción del tipo de jornada elegido, con su casilla marcada («Desde el 10/09/2026 hasta el 12/09/2026»), y las otras tres opciones de tipo de jornada no aparecen.
- **And** al iniciar sesión `jefeestudios1@mislata.es` (contraseña `demo1234`) y abrir el expediente por la lista «Tramitación» → «Pendientes de mí», la pantalla del perfil `TRAMITADOR` —la de verificar la solicitud— muestra en solo lectura el panel «Datos de la falta» con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio» a 10/09/2026 y «Fecha de fin» a 12/09/2026, sin «Hora de inicio» ni «Hora de fin», y ofrece el panel «Verificación de la solicitud» y el botón «Siguiente».
- **And** la jefatura elige «La solicitud es correcta» y pulsa «Siguiente»: el expediente pasa a la fase `RESOLUCION`, estado `PENDIENTE_RESOLUCION` (la cabecera muestra «Resolución de la dirección» y «Pendiente de resolución»); como ese estado es del perfil `DIRECTOR` y no tiene pantalla para el `TRAMITADOR`, lo que le queda a la jefatura es la vista genérica de solo consulta: el panel «Datos de la falta» sale en solo lectura con el tipo de jornada faltada «Varios días (todos ellos completos)», el campo de la fecha titulado «Fecha de Inicio» (no «Fecha») a 10/09/2026 y «Fecha de fin» a 12/09/2026, sin «Hora de inicio» ni «Hora de fin»; no hay ningún campo editable de la solicitud (salvo «Nueva nota» del panel de notas internas) y el único botón del pie es «Salir».
- **And** al iniciar sesión de nuevo `director@mislata.es` (contraseña `demo1234`) y abrir el expediente **entrando por la lista «Tramitación» → «Pendientes de mí»**, como ostenta el perfil `DIRECTOR` del estado (además del de `CREADOR`) el sistema abre la pantalla de resolver: muestra en solo lectura el panel «Datos de la falta» con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio» a 10/09/2026 y «Fecha de fin» a 12/09/2026, sin «Hora de inicio» ni «Hora de fin», y ofrece el panel «Resolver expediente» y el botón «Resolver el expediente»; el expediente **sigue** en `RESOLUCION` / `PENDIENTE_RESOLUCION`.
