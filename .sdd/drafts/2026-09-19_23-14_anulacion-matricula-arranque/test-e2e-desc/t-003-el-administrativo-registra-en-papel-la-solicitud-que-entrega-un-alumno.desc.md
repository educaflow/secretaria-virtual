---
type: test-e2e
id: T-003
---

# T-003 — El administrativo registra en papel la solicitud que entrega un alumno

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `administrativo1@mislata.es`)
**Desde:** `[*]`
**Evento:** `CONTINUAR` — botón «Siguiente»
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

- **Given** que `administrativo1@mislata.es` (contraseña `demo1234`) es administrativo de CIPFP Mislata y solo tiene el perfil `TRAMITADOR` sobre los trámites de alumno, y que un alumno le ha entregado en ventanilla su solicitud de anulación firmada en papel.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la ventana «Nuevo expediente» con «Centro» = «CIPFP Mislata» de solo lectura y **sin** el interruptor de la forma de presentación: al tener solo el perfil `TRAMITADOR`, la presentación en papel se da por deducida.
- **And** la pregunta que se muestra es «¿Para quién es la solicitud? («Para mí» es para la persona que la ha entregado)», con las mismas dos opciones y ninguna marcada.
- **When** marca «Para mí» —la solicitud es del propio alumno que la ha entregado— y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `PENDIENTE_DOCUMENTO_ESCANEADO`, con la cabecera «Solicitud de anulación» / «Pendiente de adjuntar la solicitud en papel escaneada».
- **And** el aviso es «Adjunte escaneada en PDF la solicitud que ha entregado firmada la persona que la presenta. En el paso siguiente copiará sus datos», y el panel «Solicitud entregada en papel» ofrece el campo «Solicitud escaneada (PDF)».
- **When** adjunta en «Solicitud escaneada (PDF)» un PDF de menos de 10 MB y pulsa «Siguiente».
- **Then** el expediente pasa al estado `DATOS_SOLICITUD` de la misma fase, con la cabecera «Solicitud de anulación» / «Datos de la solicitud».
- **And** el aviso pasa a ser «Copie los datos de la solicitud entregada en papel que ha adjuntado escaneada. Al presentarla se registrará su entrada», y el panel «Solicitud entregada en papel» muestra el PDF adjuntado, de solo lectura.
- **And** el panel «Persona que presenta la solicitud» **no** aparece (la solicitud es de quien la entregó), y en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE» están **vacíos y editables**: el administrativo aún no ha copiado los datos del alumno.
- **And** el pie ofrece «Borrar el expediente», «Atrás» y «Presentar la solicitud».
