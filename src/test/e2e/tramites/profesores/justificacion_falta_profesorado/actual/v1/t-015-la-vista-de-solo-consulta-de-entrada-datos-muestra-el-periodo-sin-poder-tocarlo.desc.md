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
**Perfil:** `TRAMITADOR` (login `jefeestudios1@mislata.es`) para comprobar que su pantalla no le deja hacer nada, y `SECRETARIO` (login `secretario@mislata.es`) para consultar la vista genérica
**Desde:** `ENTRADA` / `ENTRADA_DATOS`
**Evento:** — (solo se abre la pantalla)
**Hasta:** `ENTRADA` / `ENTRADA_DATOS`
**Tipo:** solo-lectura
**Manual:** no

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Origen del perfil |
|---|---|---|---|---|---|
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoUsuarioTramite` (`CREADOR` / `PROFESOR` / `PROFESOR`) |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `AUDITOR` | `<acl>` del trámite (`AUDITOR` / cargo `DIRECTOR`) |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `DIRECTOR` | security `AceProfileGlobal` (`DIRECTOR` / cargo `DIRECTOR`) |
| `jefeestudios1@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `JEFE_ESTUDIOS` | CIPFP Mislata | `TRAMITADOR` | security `AceProfileTipoUsuarioTramite` (`TRAMITADOR` / cargo `JEFE_ESTUDIOS` / `PROFESOR`) |
| `secretario@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `SECRETARIO` | CIPFP Mislata | `SECRETARIO` | security `AceProfileGlobal` (`SECRETARIO` / cargo `SECRETARIO`) |

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

- **Given** el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, ha creado un expediente nuevo de «Justificación de falta del profesorado» y, en `ENTRADA` / `ENTRADA_DATOS`, ha elegido el tipo de jornada faltada «Unas horas de un único día» con «Fecha» 10/09/2026, «Hora de inicio» 09:00 y «Hora de fin» 11:00, «Motivo falta» «Asistencia a pruebas selectivas y exámenes» y `justificante.pdf` adjunto; ha pulsado «Siguiente» (el expediente pasa a `ENTRADA` / `PENDIENTE_PRESENTACION`) y después «Atrás», con lo que el expediente vuelve a `ENTRADA` / `ENTRADA_DATOS` con esos datos ya guardados, y cierra sesión; después `jefeestudios1@mislata.es` (contraseña `demo1234`) inicia sesión y abre ese expediente **entrando por la lista «Tramitación» → «Jefatura de estudios» → «Abiertos» (en `ENTRADA_DATOS` el expediente espera al profesor, así que no está en «Pendientes de mí»)**; su perfil sobre el expediente es `TRAMITADOR` y `ENTRADA_DATOS` tiene una pantalla para ese perfil —la de registrar una solicitud entregada en papel—, así que el sistema abre esa pantalla y no la vista genérica.
- **When** mira la pantalla.
- **Then** el expediente **sigue** en `ENTRADA` / `ENTRADA_DATOS` y la cabecera muestra «Entrada» y «Entrada de datos».
- **And** el panel «Datos de la falta» muestra el tipo de jornada faltada «Unas horas de un único día», «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00, y **no** muestra «Fecha de fin»; como la solicitud es telemática, la pantalla muestra el aviso «La solicitud está pendiente de que el profesor complete o corrija los datos y la presente» y **no ofrece ningún botón** (ni «Siguiente», ni «Presentar la solicitud», ni «Atrás», ni «Borrar el expediente»), así que no puede guardar ningún cambio.
- **And** tras cerrar sesión la jefatura, `secretario@mislata.es` (contraseña `demo1234`) inicia sesión y abre ese expediente entrando por la misma lista «Tramitación» → «Jefatura de estudios» → «Abiertos»; como su perfil sobre el expediente es `SECRETARIO` y `ENTRADA_DATOS` no tiene pantalla para ese perfil, el sistema abre la vista genérica de solo consulta: el panel «Datos de la falta» muestra, en solo lectura, el tipo de jornada faltada (grupo de radios con «Unas horas de un único día» marcado, sin que pulsar otra opción cambie la selección), «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00, y **no** muestra «Fecha de fin»; se ve el aviso «La solicitud está pendiente de que el profesor complete y guarde los datos de la falta», no hay ningún campo editable (salvo «Nueva nota» del panel de notas internas, que no es un dato del expediente), el único botón es «Salir» y el expediente **sigue** en `ENTRADA` / `ENTRADA_DATOS`.
