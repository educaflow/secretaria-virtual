---
type: test-e2e
id: T-006
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-006-el-administrativo-que-tambien-es-alumno-registra-en-papel-la-solicitud-de-otro-alumno.desc.md
     Iniciativa: 2026-09-19_23-14_anulacion-matricula-arranque
     Test: T-006  |  Origen ESC: —
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-006 — El administrativo que también es alumno registra en papel la solicitud de otro alumno

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `administrativo2@mislata.es`)
**Desde:** `[*]`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `ENTRADA` / `ENTRADA_DATOS`
**Tipo:** happy
**Manual:** no

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Origen del perfil |
|---|---|---|---|---|---|
| `alumno1@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoUsuarioTramite` (`CREADOR` + `ALUMNO` + trámites de `ALUMNO`) |
| `familiar1@mislata.es` | `demo1234` | tipo de usuario `FAMILIAR` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoUsuarioTramite` (`CREADOR` + `FAMILIAR` + trámites de `ALUMNO`) |
| `administrativo1@mislata.es` | `demo1234` | tipo de usuario `ADMINISTRATIVO` | CIPFP Mislata | `TRAMITADOR` | security `AceProfileTipoUsuarioTramite` (`TRAMITADOR` + `ADMINISTRATIVO` + trámites de `ALUMNO`) |
| `administrativo2@mislata.es` | `demo1234` | tipos de usuario `ADMINISTRATIVO` **y** `ALUMNO` | CIPFP Mislata | `TRAMITADOR` **y** `CREADOR` | security `AceProfileTipoUsuarioTramite` (las dos filas anteriores le alcanzan a la vez) |

El perfil con el que nace el expediente sale de cómo se presenta: telemáticamente actúa el `CREADOR`, y en papel el `TRAMITADOR`.
La forma de presentar la fija la entrada de menú por la que se abre el asistente, que nunca pregunta «¿Cómo se presenta?»: «Mis trámites» → «Nuevo trámite» es la del propio usuario (`CREADOR`) y «Tramitación» → «Nuevo trámite» la de registrar en papel (`TRAMITADOR`).
Por eso `administrativo2@mislata.es`, que tiene los dos perfiles de inicio en el centro, presenta lo suyo por «Mis trámites» y registra lo que le entregan en papel por «Tramitación».
La pregunta «¿Para quién es el expediente?» solo se hace cuando, para la forma de presentar, valen las dos respuestas: registrando en papel siempre; presentándolo uno mismo, nunca a estos actores (el alumno solo puede «Para mí» y el familiar solo «en representación», y el asistente lo fija sin preguntar).
Cómo funciona el asistente está en `src/main/java/com/educaflow/system/ventanilla/views/nuevoexpediente/CLAUDE.md`.

### Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y tipos de usuario, más el data-init del trámite y de los perfiles.
Ningún test puede presuponer más estado que este.

Los cuatro actores pertenecen **solo** a CIPFP Mislata, así que el asistente («Mis trámites» o «Tramitación» → «Nuevo trámite») se salta la elección de centro y en la pantalla de alta el campo «Centro» ya viene relleno con «CIPFP Mislata» y de solo lectura: no hay que elegir centro en ningún test.

#### Configuración que los tests dan por hecha

| Qué | Dónde está | Valor que exigen estos tests |
|---|---|---|
| El trámite admite presentar en representación | `tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml` | `<permitidoPresentarEnRepresentacion>true</permitidoPresentarEnRepresentacion>` |
| El familiar puede crear expedientes de alumno | `subsystem/security/data-init/input/AceProfileTipoUsuarioTramite.xml` | `<ace perfil="CREADOR"><usuario tipoUsuario="FAMILIAR"/><tramite tipoUsuario="ALUMNO"/></ace>` |

Las dos cosas están en el árbol de fuentes, pero el data-init solo las lleva a la base de datos **al arrancar**: la aplicación tiene que haberse arrancado con `./run.sh` después de esos cambios, o los casos 2, 4 y 7 fallarán al no admitir el asistente el expediente en representación.

#### Juego de datos válido — fase `ENTRADA`

| campo | valor |
|---|---|
| «Solicitud escaneada (PDF)» (estado `PENDIENTE_DOCUMENTO_ESCANEADO`) | un fichero PDF cualquiera de menos de 10 MB; su contenido es indiferente porque en estos tests nadie lo lee |

Ningún otro dato se introduce: los tests acaban nada más llegar a la pantalla de entrada de datos.

## Pasos

- **Given** que `administrativo2@mislata.es` (contraseña `demo1234`) tiene los dos perfiles de inicio en CIPFP Mislata, y que otro alumno le ha entregado en ventanilla su solicitud de anulación firmada en papel.
- **When** inicia sesión, abre «Tramitación» → «Nuevo trámite», despliega «Trámites para el alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la pantalla «Nuevo expediente» **sin** la pregunta «¿Cómo se presenta?» (aunque tiene los dos perfiles, la entrada «Tramitación» ya fija que se registra en papel) y con la pregunta «¿Para quién es el expediente?» («Para mí» es para la persona que ha entregado el papel), sin ninguna opción marcada.
- **When** marca «Para mí» —la solicitud es del alumno que la ha entregado, no suya— y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `ENTRADA`, estado `PENDIENTE_DOCUMENTO_ESCANEADO`, con el panel «Solicitud entregada en papel» y su campo «Solicitud escaneada (PDF)».
- **When** adjunta un PDF de menos de 10 MB y pulsa «Siguiente».
- **Then** el expediente pasa al estado `ENTRADA_DATOS`, con la cabecera «Entrada» / «Entrada de datos» y el aviso «Copie los datos de la solicitud entregada en papel que ha adjuntado escaneada. Al presentarla se registrará su entrada».
- **And** el panel «Persona que presenta la solicitud» **no** aparece, y en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE» están **vacíos y editables**: no se han precargado los suyos, porque el interesado es el otro alumno.
- **And** el pie ofrece «Borrar el expediente», «Atrás» y «Presentar la solicitud».
