---
type: test-e2e
id: T-001
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-001-justifica-un-dia-completo.desc.md
     Iniciativa: 2026-09-22_16-01_justificacion-falta-profesorado-fechas
     Test: T-001  |  Origen ESC: ESC-001
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-001 — Justifica un día completo

**Origen ESC:** ESC-001
**Perfil:** `CREADOR` (login `director@mislata.es`) para rellenar y guardar, y `TRAMITADOR` (login `jefeestudios1@mislata.es`) para consultar la vista genérica de `PENDIENTE_PRESENTACION` en el último And
**Desde:** `[*]`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Tipo:** happy
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

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo; el sistema lo abre en la fase `RECEPCION`, estado `ENTRADA_DATOS`, con el panel «Datos del profesor interesado» ya relleno con sus apellidos, su nombre y su DNI, y con el panel «Datos de la falta» sin ningún dato del periodo precargado.
- **When** elige el tipo de jornada faltada «Un día completo», rellena «Fecha» con 10/09/2026 y «Motivo falta» con «Traslado de domicilio», adjunta `justificante.pdf` en «Foto o PDF del justificante» y pulsa «Siguiente».
- **Then** el expediente queda en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`, y la cabecera muestra «Recepción» y «Pendiente de presentación».
- **And** antes de pulsar «Siguiente», el panel «Datos de la falta» muestra el campo titulado «Fecha» y **no** muestra «Fecha de fin», ni «Hora de inicio», ni «Hora de fin».
- **And** la nueva pantalla muestra la solicitud generada en PDF y ofrece los botones «Atrás» y el de firmar y presentar.
- **And** tras cerrar sesión el profesor, `jefeestudios1@mislata.es` (contraseña `demo1234`) inicia sesión y abre ese expediente **entrando por la lista «Tramitación» → «Jefatura de estudios» → «Abiertos» (en `PENDIENTE_PRESENTACION` el expediente espera al profesor, así que no está en «Pendientes de mí»)**, que es la del perfil `TRAMITADOR`; como `PENDIENTE_PRESENTACION` no tiene pantalla para ese perfil, el sistema abre la vista genérica de solo consulta, que muestra la solicitud en PDF y el aviso «La solicitud está pendiente de que el profesor la firme y la presente»; no hay ningún campo editable, el único botón es «Salir» y el expediente **sigue** en `RECEPCION` / `PENDIENTE_PRESENTACION`.
