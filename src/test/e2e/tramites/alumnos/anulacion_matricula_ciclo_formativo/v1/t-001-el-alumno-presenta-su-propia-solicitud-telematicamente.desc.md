---
type: test-e2e
id: T-001
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-001-el-alumno-presenta-su-propia-solicitud-telematicamente.desc.md
     Iniciativa: 2026-09-19_23-14_anulacion-matricula-arranque
     Test: T-001  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-001 — El alumno presenta su propia solicitud telemáticamente

**Origen ESC:** —
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `[*]`
**Evento:** — (alta: botón «Crear expediente»)
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Origen del perfil |
|---|---|---|---|---|---|
| `alumno1@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoTramite` (`CREADOR` + `ALUMNO` + tipo de trámite `ALUMNO`) |
| `familiar1@mislata.es` | `demo1234` | tipo de usuario `FAMILIAR` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoTramite` (`CREADOR` + `FAMILIAR` + tipo de trámite `ALUMNO`) |
| `administrativo1@mislata.es` | `demo1234` | tipo de usuario `ADMINISTRATIVO` | CIPFP Mislata | `TRAMITADOR` | security `AceProfileTipoTramite` (`TRAMITADOR` + `ADMINISTRATIVO` + tipo de trámite `ALUMNO`) |
| `administrativo2@mislata.es` | `demo1234` | tipos de usuario `ADMINISTRATIVO` **y** `ALUMNO` | CIPFP Mislata | `TRAMITADOR` **y** `CREADOR` | security `AceProfileTipoTramite` (las dos filas anteriores le alcanzan a la vez) |

El perfil con el que nace el expediente sale de cómo se presenta: telemáticamente actúa el `CREADOR`, y en papel el `TRAMITADOR`.
Por eso `administrativo2@mislata.es` es el único que tiene que **elegir**: al tener los dos perfiles de inicio en el centro, la pantalla de alta le hace la pregunta «¿Cómo se presenta?».
La pregunta «¿Para quién es el expediente?» solo se hace cuando, para la forma de presentar, valen las dos respuestas: registrando en papel siempre; presentándolo uno mismo, nunca a estos actores (el alumno solo puede «Para mí» y el familiar solo «en representación», y el asistente lo fija sin preguntar).
Cómo funciona el asistente está en `src/main/java/com/educaflow/system/ventanilla/views/nuevoexpediente/CLAUDE.md`.

### Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y tipos de usuario, más el data-init del trámite y de los perfiles.
Ningún test puede presuponer más estado que este.

Los cuatro actores pertenecen **solo** a CIPFP Mislata, así que el asistente «Mis trámites» → «Nuevo trámite» se salta la elección de centro y en la pantalla de alta el campo «Centro» ya viene relleno con «CIPFP Mislata» y de solo lectura: no hay que elegir centro en ningún test.

#### Configuración que los tests dan por hecha

| Qué | Dónde está | Valor que exigen estos tests |
|---|---|---|
| El trámite admite presentar en representación | `tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml` | `<permitidoPresentarEnRepresentacion>true</permitidoPresentarEnRepresentacion>` |
| El familiar puede crear expedientes de alumno | `subsystem/security/data-init/input/AceProfileTipoTramite.xml` | `<ace perfil="CREADOR" tipoUsuario="FAMILIAR" tipoTramite="ALUMNO"/>` |

Las dos cosas están en el árbol de fuentes, pero el data-init solo las lleva a la base de datos **al arrancar**: la aplicación tiene que haberse arrancado con `./run.sh` después de esos cambios, o los casos 2, 4 y 7 fallarán al no admitir el asistente el expediente en representación.

#### Juego de datos válido — fase `SOLICITUD`

| campo | valor |
|---|---|
| «Solicitud escaneada (PDF)» (estado `PENDIENTE_DOCUMENTO_ESCANEADO`) | un fichero PDF cualquiera de menos de 10 MB; su contenido es indiferente porque en estos tests nadie lo lee |

Ningún otro dato se introduce: los tests acaban nada más llegar a la pantalla de entrada de datos.

## Pasos

- **Given** que `alumno1@mislata.es` (contraseña `demo1234`) es alumno de CIPFP Mislata y solo tiene ese centro, y que por ser alumno tiene el perfil `CREADOR` sobre los trámites de alumno.
- **When** inicia sesión, abre «Mis trámites» → «Nuevo trámite», despliega «Trámites para el alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la pantalla «Nuevo expediente» con la ayuda del trámite, el campo «Centro» ya relleno con «CIPFP Mislata» y de solo lectura, **sin** la pregunta «¿Cómo se presenta?» (solo tiene el perfil `CREADOR`, así que no hay nada que elegir) y **sin** la pregunta «¿Para quién es el expediente?» (es alumno y no familiar, así que el expediente solo puede ser para él mismo).
- **When** pulsa «Crear expediente».
- **Then** se cierra la pantalla «Nuevo expediente» y se abre el expediente en la fase `SOLICITUD`, estado `DATOS_SOLICITUD`, cuya cabecera muestra «Solicitud de anulación» y «Datos de la solicitud».
- **And** el aviso de la pantalla es «Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador».
- **And** el panel «Persona que presenta la solicitud» **no** aparece: solicitante e interesado son la misma persona.
- **And** en «Alumno/a al que se refiere la solicitud» los campos vienen rellenos con los datos de quien ha entrado —«Apellidos» = «CIPFP Mislata», «Nombre» = «Alumno1», «DNI/NIE» = «86862719E»— y los tres están bloqueados.
- **And** el panel «Datos del alumno/a» («NIA», «Teléfono», «Dirección», «Municipio», «CP») está vacío y editable, y el panel «Matrícula que se anula» muestra el curso académico y «CIPFP Mislata» de solo lectura, con «Ciclo» vacío.
- **And** el pie ofrece los botones «Borrar el expediente» y «Siguiente».
