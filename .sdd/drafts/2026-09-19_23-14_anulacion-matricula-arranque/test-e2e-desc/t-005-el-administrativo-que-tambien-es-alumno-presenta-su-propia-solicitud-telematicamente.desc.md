---
type: test-e2e
id: T-005
---

# T-005 — El administrativo que también es alumno presenta su propia solicitud telemáticamente

**Origen ESC:** —
**Perfil:** `CREADOR` (login `administrativo2@mislata.es`)
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

- **Given** que `administrativo2@mislata.es` (contraseña `demo1234`) es a la vez administrativo y alumno de CIPFP Mislata, de modo que tiene los dos perfiles de inicio (`TRAMITADOR` y `CREADOR`) sobre los trámites de alumno, y que quiere anular **su propia** matrícula.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la ventana «Nuevo expediente» y, a diferencia de los tests anteriores, **sí** se muestra el interruptor «Presentado el expediente a partir de un documento en papel», apagado: como tiene los dos perfiles, se le pregunta cómo presenta.
- **And** la pregunta de destinatario es «¿Para quién es el expediente?» (la forma telemática).
- **When** deja el interruptor apagado, marca «Para mí» y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `DATOS_SOLICITUD`, con la cabecera «Solicitud de anulación» / «Datos de la solicitud», exactamente igual que en T-001: el expediente nace como presentación telemática aunque quien entra sea administrativo.
- **And** el aviso es «Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador».
- **And** el panel «Persona que presenta la solicitud» **no** aparece, y en «Alumno/a al que se refiere la solicitud» los campos vienen rellenos con sus propios datos —«Apellidos» = «CIPFP Mislata», «Nombre» = «Administrativo2», «DNI/NIE» = «16493254T»— y bloqueados.
- **And** el pie ofrece «Borrar el expediente» y «Siguiente».
