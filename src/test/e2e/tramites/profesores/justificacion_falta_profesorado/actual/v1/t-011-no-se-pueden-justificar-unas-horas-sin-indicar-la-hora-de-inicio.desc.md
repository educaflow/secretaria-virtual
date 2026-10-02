---
type: test-e2e
id: T-011
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-011-no-se-pueden-justificar-unas-horas-sin-indicar-la-hora-de-inicio.desc.md
     Iniciativa: 2026-09-22_16-01_justificacion-falta-profesorado-fechas
     Test: T-011  |  Origen ESC: ESC-012
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-011 — No se pueden justificar unas horas sin indicar la hora de inicio

**Origen ESC:** ESC-012
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `ENTRADA` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `ENTRADA` / `ENTRADA_DATOS`
**Tipo:** error
**Manual:** no

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

La demo **no** carga ningún certificado digital, así que en `ENTRADA` / `PENDIENTE_PRESENTACION` la solicitud solo se puede firmar con AutoFirma en el equipo del profesor, y la resolución exige el certificado del director del centro instalado en el servidor. Los tests que atraviesan esos dos pasos van marcados `Manual: sí`.

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

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene abierto en `ENTRADA` / `ENTRADA_DATOS`.
- **When** elige el tipo de jornada faltada «Unas horas de un único día», rellena «Fecha» con 10/09/2026, deja «Hora de inicio» vacía, rellena «Hora de fin» con 11:00 y «Motivo falta» con «Enfermedad común», adjunta `justificante.pdf` y pulsa «Siguiente».
- **Then** el sistema muestra el error «Es requerido» y el expediente **sigue** en `ENTRADA` / `ENTRADA_DATOS`.
