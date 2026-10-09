---
type: test-e2e
id: T-017
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-017-el-tramitador-acepta-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.desc.md
     Iniciativa: 2026-09-22_16-01_justificacion-falta-profesorado-fechas
     Test: T-017  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-017 — La dirección acepta la justificación y el estado cerrado sigue mostrando el periodo

**Origen ESC:** —
**Perfil:** `DIRECTOR` (login `director@mislata.es`) para resolver; `CREADOR` (el mismo `director@mislata.es`) y `TRAMITADOR` (login `jefeestudios1@mislata.es`) solo para dejar el expediente en el estado de partida
**Desde:** `RESOLUCION` / `PENDIENTE_RESOLUCION`
**Evento:** `RESOLVER` — botón «Resolver el expediente» (guarda `tipoResolucion=ACEPTAR`)
**Hasta:** `RESOLUCION` / `ACEPTADO`
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

El servidor tiene instalados los certificados digitales de los DNI de los usuarios, así que en `ENTRADA` / `PENDIENTE_PRESENTACION` la solicitud la firma el propio servidor con el certificado del profesor, y la resolución la firma el servidor con el certificado del director del centro, sin intervención manual.

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

- **Given** existe un expediente de «Justificación de falta del profesorado» del centro CIPFP Mislata en `RESOLUCION` / `PENDIENTE_RESOLUCION`, presentado (firmado por el servidor con el certificado del profesor) con el tipo de jornada faltada «Unas horas de un único día», «Fecha» 10/09/2026, «Hora de inicio» 09:00 y «Hora de fin» 11:00, y verificado como correcto por la jefatura de estudios (`jefeestudios1@mislata.es`, que lo abre por «Tramitación» → «Pendientes de mí», elige «La solicitud es correcta» y pulsa «Siguiente»); `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, lo abre por la lista «Tramitación» → «Pendientes de mí» y el servidor tiene instalado el certificado digital de su director.
- **When** elige «Tipo resolución» «Resolver positivamente», pulsa «Resolver el expediente» y confirma el aviso de que no podrá deshacer la acción.
- **Then** el expediente pasa a la fase `RESOLUCION`, estado `ACEPTADO`, que es un estado cerrado: deja de estar en «Tramitación» → «Pendientes de mí» y aparece en «Tramitación» → «Jefatura de estudios» → «Cerrados».
- **And** la pantalla de `ACEPTADO` es la genérica de solo consulta: muestra el panel «Datos de la falta» en solo lectura con «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00, sin «Fecha de fin»; muestra la resolución en PDF y el bloque «Resolución»; no hay ningún campo editable (salvo «Nueva nota» del panel de notas internas); no ofrece ningún evento y su único botón es «Salir».
