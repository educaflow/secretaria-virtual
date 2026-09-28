---
type: test-e2e
id: T-018
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot NO VERIFICADO: este test es MANUAL y no se ha ejecutado nunca de forma desatendida.
     Su .spec.ts lleva el tag @manual; se lanza con: E2E_MANUAL=1 npx playwright test --grep @manual --headed
     Fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-018-el-tramitador-rechaza-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.desc.md
     Iniciativa: 2026-09-22_16-01_justificacion-falta-profesorado-fechas
     Test: T-018  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-018 — El tramitador rechaza la justificación y el estado cerrado sigue mostrando el periodo

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `jefeestudios1@mislata.es`)
**Desde:** `TRAMITACION` / `PENDIENTE_RESOLUCION`
**Evento:** `RESOLVER` — botón «Resolver el expediente» (guarda `tipoResolucion=RECHAZAR`)
**Hasta:** `TRAMITACION` / `RECHAZADO`
**Tipo:** happy
**Manual:** sí — llegar a `PENDIENTE_RESOLUCION` exige presentar con AutoFirma (T-016) y resolver exige el certificado digital del director del centro instalado en el servidor; la carga de demo no trae ninguno de los dos.

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

- **Given** existe un expediente de «Justificación de falta del profesorado» del centro CIPFP Mislata en `TRAMITACION` / `PENDIENTE_RESOLUCION`, presentado con el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas», «Fecha de Inicio» 10/09/2026, «Hora de inicio» 12:00 y «Fecha de fin» 11/09/2026; `jefeestudios1@mislata.es` (contraseña `demo1234`) ha iniciado sesión, lo abre por la lista «Tramitación» → «Pendientes de mí» y el centro tiene instalado el certificado digital de su director.
- **When** elige «Tipo resolución» «Resolver negativamente(Rechazar)», rellena «Motivo del rechazo» con «Los días indicados no constan como falta», pulsa «Resolver el expediente» y confirma el aviso.
- **Then** el expediente pasa a la fase `TRAMITACION`, estado `RECHAZADO`, que es un estado cerrado.
- **And** la pantalla de `RECHAZADO` es la genérica de solo consulta: muestra el panel «Datos de la falta» en solo lectura con «Fecha de Inicio» a 10/09/2026, «Hora de inicio» a 12:00 y «Fecha de fin» a 11/09/2026, sin «Hora de fin»; muestra la resolución en PDF con el motivo del rechazo; no ofrece ningún evento y su único botón es «Salir».
