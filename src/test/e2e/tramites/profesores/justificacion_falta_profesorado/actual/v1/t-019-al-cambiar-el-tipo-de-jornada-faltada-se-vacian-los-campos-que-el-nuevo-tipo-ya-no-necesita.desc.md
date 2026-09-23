---
type: test-e2e
id: T-019
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-019-al-cambiar-el-tipo-de-jornada-faltada-se-vacian-los-campos-que-el-nuevo-tipo-ya-no-necesita.desc.md
     Iniciativa: 2026-09-22_16-01_justificacion-falta-profesorado-fechas
     Test: T-019  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-019 — Al cambiar el tipo de jornada faltada se vacían los campos que el nuevo tipo ya no necesita

**Origen ESC:** —
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** — (solo cambia el selector «Tipo de jornada faltada», sin pulsar ningún botón)
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** happy
**Manual:** no

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

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `RECEPCION` / `ENTRADA_DATOS`; en el panel «Datos de la falta» ha elegido el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas» y ha rellenado «Fecha de Inicio» con 10/09/2026, «Hora de inicio» con 12:00 y «Fecha de fin» con 11/09/2026, sin pulsar «Siguiente».
- **When** cambia el tipo de jornada faltada a «Un día completo».
- **Then** el panel «Datos de la falta» deja de mostrar «Fecha de fin» y «Hora de inicio», y solo muestra «Fecha» (con el título «Fecha», no «Fecha de Inicio»), que sigue mostrando 10/09/2026; el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS` sin haberse guardado nada.
- **And** vuelve a elegir el tipo de jornada faltada «Varios días pero del primer día solo faltó unas horas»: el panel vuelve a mostrar «Fecha de Inicio», «Hora de inicio» y «Fecha de fin»; «Fecha de Inicio» conserva 10/09/2026, pero «Hora de inicio» y «Fecha de fin» aparecen **vacías** — no conservan los valores 12:00 y 11/09/2026 que tenían antes del primer cambio.
