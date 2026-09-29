---
type: test-e2e
id: T-015
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-015-la-vista-de-solo-consulta-de-entrada-datos-muestra-el-periodo-sin-poder-tocarlo.desc.md
     Iniciativa: 2026-09-22_16-01_justificacion-falta-profesorado-fechas
     Test: T-015  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-015 — La vista de solo consulta de ENTRADA_DATOS muestra el periodo sin poder tocarlo

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `jefeestudios1@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** — (solo se abre la pantalla)
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** solo-lectura
**Manual:** no

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Origen del perfil |
|---|---|---|---|---|---|
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoUsuarioTramite` (`CREADOR` / `PROFESOR` / `PROFESOR`) |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `AUDITOR` | `<acl>` del trámite (`AUDITOR` / cargo `DIRECTOR`) |
| `jefeestudios1@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `JEFE_ESTUDIOS` | CIPFP Mislata | `TRAMITADOR` | security `AceProfileTipoUsuarioTramite` (`TRAMITADOR` / cargo `JEFE_ESTUDIOS` / `PROFESOR`) |

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

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y, en `RECEPCION` / `ENTRADA_DATOS`, ha elegido el tipo de jornada faltada «Unas horas de un único día» con «Fecha» 10/09/2026, «Hora de inicio» 09:00 y «Hora de fin» 11:00, «Motivo falta» «Asistencia a pruebas selectivas y exámenes» y `justificante.pdf` adjunto; ha pulsado «Siguiente» (el expediente pasa a `RECEPCION` / `PENDIENTE_PRESENTACION`) y después «Atrás», con lo que el expediente vuelve a `RECEPCION` / `ENTRADA_DATOS` con esos datos ya guardados, y cierra sesión; después `jefeestudios1@mislata.es` (contraseña `demo1234`) inicia sesión y abre ese expediente **entrando por la lista «Tramitación» → «Jefatura de estudios» → «Abiertos» (en `ENTRADA_DATOS` el expediente espera al profesor, así que no está en «Pendientes de mí»)**; como su perfil sobre el expediente es `TRAMITADOR` y `ENTRADA_DATOS` no tiene pantalla para ese perfil, el sistema abre la vista genérica de solo consulta.
- **When** mira la pantalla.
- **Then** el expediente **sigue** en `RECEPCION` / `ENTRADA_DATOS` y la cabecera muestra «Recepción» y «Entrada de datos».
- **And** el panel «Datos de la falta» muestra, en solo lectura, el tipo de jornada faltada «Unas horas de un único día», «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00, y **no** muestra «Fecha de fin»; no hay ningún campo editable y el único botón es «Salir».
