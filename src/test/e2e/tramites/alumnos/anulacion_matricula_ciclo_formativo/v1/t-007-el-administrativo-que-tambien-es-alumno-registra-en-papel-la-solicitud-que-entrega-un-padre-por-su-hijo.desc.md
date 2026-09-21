---
type: test-e2e
id: T-007
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-007-el-administrativo-que-tambien-es-alumno-registra-en-papel-la-solicitud-que-entrega-un-padre-por-su-hijo.desc.md
     Iniciativa: 2026-09-19_23-14_anulacion-matricula-arranque
     Test: T-007  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-007 — El administrativo que también es alumno registra en papel la solicitud que entrega un padre por su hijo

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `administrativo2@mislata.es`)
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

- **Given** que `administrativo2@mislata.es` (contraseña `demo1234`) tiene los dos perfiles de inicio en CIPFP Mislata, y que quien ha entregado la solicitud en ventanilla es el padre de un alumno, que la presenta en representación de su hijo.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno», pulsa sobre «Anulación de matrícula en ciclo formativo» y enciende el interruptor «Presentado el expediente a partir de un documento en papel».
- **Then** la pregunta de destinatario es «¿Para quién es la solicitud? («Para mí» es para la persona que la ha entregado)».
- **When** marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)» y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `PENDIENTE_DOCUMENTO_ESCANEADO`, con el panel «Solicitud entregada en papel» y su campo «Solicitud escaneada (PDF)».
- **When** adjunta un PDF de menos de 10 MB y pulsa «Siguiente».
- **Then** el expediente pasa al estado `DATOS_SOLICITUD`, con la cabecera «Solicitud de anulación» / «Datos de la solicitud».
- **And** aparece el panel «Persona que presenta la solicitud» con los tres campos **vacíos y editables** (el padre que entregó el papel), y en «Alumno/a al que se refiere la solicitud» los tres campos están también **vacíos y editables** (el hijo).
- **And** el pie ofrece «Borrar el expediente», «Atrás» y «Presentar la solicitud».
