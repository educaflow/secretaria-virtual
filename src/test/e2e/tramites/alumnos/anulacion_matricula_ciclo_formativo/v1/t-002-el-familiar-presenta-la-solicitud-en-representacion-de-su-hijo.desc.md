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
La pregunta «¿Para quién es el expediente?» solo se hace cuando, para la forma de presentar, valen las dos respuestas: registrando en papel siempre; presentándolo uno mismo, nunca a estos actores (el alumno solo puede «Para la persona que lo presenta» y el familiar solo «en representación», y el asistente lo fija sin preguntar).
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

- **Given** que `familiar1@mislata.es` (contraseña `demo1234`) es familiar en CIPFP Mislata, que el data-init de security le da el perfil `CREADOR` sobre los trámites de alumno y que el trámite admite presentar en representación.
- **And** que el hijo al que se refiere la solicitud **no** se elige en ninguna pantalla: la aplicación no guarda ningún vínculo entre el familiar y el alumno, y los datos del hijo se teclean después, en la propia entrada de datos.
- **When** inicia sesión, abre «Mis trámites» → «Nuevo trámite», despliega «Trámites para el alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la pantalla «Nuevo expediente» con «Centro» = «CIPFP Mislata» de solo lectura, **sin** la pregunta «¿Cómo se presenta?» (la forma la fija la entrada de menú) y **sin** la pregunta «¿Para quién es el expediente?»: es familiar y no alumno, así que el expediente solo puede ser en representación y el asistente lo fija sin preguntar.
- **When** pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `ENTRADA`, estado `ENTRADA_DATOS`, con la cabecera «Entrada» / «Entrada de datos».
- **And** aparece el panel «Persona que presenta la solicitud» con los datos del familiar que ha entrado —«Apellidos» = «de Alumno1 CIPFP Mislata», «Nombre» = «Familiar1», «DNI/NIE» = «90923322K»— y los tres campos bloqueados.
- **And** en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE» están **vacíos y editables**: es donde se identificará al hijo.
- **And** el pie ofrece «Borrar el expediente» y «Siguiente».
