---
type: test-e2e
id: T-002
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-002-el-familiar-presenta-la-solicitud-en-representacion-de-su-hijo.desc.md
     Iniciativa: 2026-09-19_23-14_anulacion-matricula-arranque
     Test: T-002  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-002 — El familiar presenta la solicitud en representación de su hijo

**Origen ESC:** —
**Perfil:** `CREADOR` (login `familiar1@mislata.es`)
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
Por eso `administrativo2@mislata.es` es el único que tiene que **elegir**: al tener los dos perfiles de inicio en el centro, la pantalla de alta le muestra el interruptor de la forma de presentación.

### Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y tipos de usuario, más el data-init del trámite y de los perfiles.
Ningún test puede presuponer más estado que este.

Los cuatro actores pertenecen **solo** a CIPFP Mislata, así que en la pantalla de alta el campo «Centro» ya viene relleno con «CIPFP Mislata» y de solo lectura: no hay que elegir centro en ningún test.

#### Configuración que los tests dan por hecha

| Qué | Dónde está | Valor que exigen estos tests |
|---|---|---|
| El trámite admite presentar en representación | `tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml` | `<permitidoPresentarEnRepresentacion>true</permitidoPresentarEnRepresentacion>` |
| El familiar puede crear expedientes de alumno | `subsystem/security/data-init/input/AceProfileTipoTramite.xml` | `<ace perfil="CREADOR" tipoUsuario="FAMILIAR" tipoTramite="ALUMNO"/>` |

Las dos cosas están en el árbol de fuentes, pero el data-init solo las lleva a la base de datos **al arrancar**: la aplicación tiene que haberse arrancado con `./run.sh` después de esos cambios, o los casos 2, 4 y 7 fallarán al no ofrecerse la opción «Para otra persona a la que represento».

#### Juego de datos válido — fase `SOLICITUD`

| campo | valor |
|---|---|
| «Solicitud escaneada (PDF)» (estado `PENDIENTE_DOCUMENTO_ESCANEADO`) | un fichero PDF cualquiera de menos de 10 MB; su contenido es indiferente porque en estos tests nadie lo lee |

Ningún otro dato se introduce: los tests acaban nada más llegar a la pantalla de entrada de datos.

## Pasos

- **Given** que `familiar1@mislata.es` (contraseña `demo1234`) es familiar en CIPFP Mislata, que el data-init de security le da el perfil `CREADOR` sobre los trámites de alumno y que el trámite admite presentar en representación.
- **And** que el hijo al que se refiere la solicitud **no** se elige en ninguna pantalla: la aplicación no guarda ningún vínculo entre el familiar y el alumno, y los datos del hijo se teclean después, en la propia entrada de datos.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la ventana «Nuevo expediente» con «Centro» = «CIPFP Mislata» de solo lectura, sin interruptor de forma de presentación, y con la pregunta «¿Para quién es el expediente?».
- **When** marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)» y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `DATOS_SOLICITUD`, con la cabecera «Solicitud de anulación» / «Datos de la solicitud».
- **And** aparece el panel «Persona que presenta la solicitud» con los datos del familiar que ha entrado —«Apellidos» = «de Alumno1 CIPFP Mislata», «Nombre» = «Familiar1», «DNI/NIE» = «43145636M»— y los tres campos bloqueados.
- **And** en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE» están **vacíos y editables**: es donde se identificará al hijo.
- **And** el pie ofrece «Borrar el expediente» y «Siguiente».
