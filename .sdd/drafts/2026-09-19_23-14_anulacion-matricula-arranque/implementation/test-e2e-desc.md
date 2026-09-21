# Tests E2E — Anulación de matrícula en ciclo formativo (`AnulacionMatriculaCicloFormativoV1`)

Tests en lenguaje de negocio, Given/When/Then. Cubren **el arranque del expediente en todos los casos de presentación**: quién entra, cómo presenta y de quién es la solicitud.
Cada test termina cuando el usuario está en la pantalla de **entrada de datos** (fase `SOLICITUD`, estado `DATOS_SOLICITUD`). No se rellenan los datos de la solicitud, no se presenta y no se firma nada.
**Sin código Playwright y sin selectores.**

## Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Origen del perfil |
|---|---|---|---|---|---|
| `alumno1@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoTramite` (`CREADOR` + `ALUMNO` + tipo de trámite `ALUMNO`) |
| `familiar1@mislata.es` | `demo1234` | tipo de usuario `FAMILIAR` | CIPFP Mislata | `CREADOR` | security `AceProfileTipoTramite` (`CREADOR` + `FAMILIAR` + tipo de trámite `ALUMNO`) |
| `administrativo1@mislata.es` | `demo1234` | tipo de usuario `ADMINISTRATIVO` | CIPFP Mislata | `TRAMITADOR` | security `AceProfileTipoTramite` (`TRAMITADOR` + `ADMINISTRATIVO` + tipo de trámite `ALUMNO`) |
| `administrativo2@mislata.es` | `demo1234` | tipos de usuario `ADMINISTRATIVO` **y** `ALUMNO` | CIPFP Mislata | `TRAMITADOR` **y** `CREADOR` | security `AceProfileTipoTramite` (las dos filas anteriores le alcanzan a la vez) |

El perfil con el que nace el expediente sale de cómo se presenta: telemáticamente actúa el `CREADOR`, y en papel el `TRAMITADOR`.
Por eso `administrativo2@mislata.es` es el único que tiene que **elegir**: al tener los dos perfiles de inicio en el centro, la pantalla de alta le muestra el interruptor de la forma de presentación.

## Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y tipos de usuario, más el data-init del trámite y de los perfiles.
Ningún test puede presuponer más estado que este.

Los cuatro actores pertenecen **solo** a CIPFP Mislata, así que en la pantalla de alta el campo «Centro» ya viene relleno con «CIPFP Mislata» y de solo lectura: no hay que elegir centro en ningún test.

### Configuración que los tests dan por hecha

| Qué | Dónde está | Valor que exigen estos tests |
|---|---|---|
| El trámite admite presentar en representación | `tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml` | `<permitidoPresentarEnRepresentacion>true</permitidoPresentarEnRepresentacion>` |
| El familiar puede crear expedientes de alumno | `subsystem/security/data-init/input/AceProfileTipoTramite.xml` | `<ace perfil="CREADOR" tipoUsuario="FAMILIAR" tipoTramite="ALUMNO"/>` |

Las dos cosas están en el árbol de fuentes, pero el data-init solo las lleva a la base de datos **al arrancar**: la aplicación tiene que haberse arrancado con `./run.sh` después de esos cambios, o los casos 2, 4 y 7 fallarán al no ofrecerse la opción «Para otra persona a la que represento».

### Juego de datos válido — fase `SOLICITUD`

| campo | valor |
|---|---|
| «Solicitud escaneada (PDF)» (estado `PENDIENTE_DOCUMENTO_ESCANEADO`) | un fichero PDF cualquiera de menos de 10 MB; su contenido es indiferente porque en estos tests nadie lo lee |

Ningún otro dato se introduce: los tests acaban nada más llegar a la pantalla de entrada de datos.

## Cobertura de transiciones

| # | fase origen | estado origen | evento | guarda | fase destino | estado destino | perfil | test |
|---|---|---|---|---|---|---|---|---|
| 1 | `[*]` | `[*]` | — | `presentadoEnPapel=false` | `SOLICITUD` | `DATOS_SOLICITUD` | `CREADOR` | T-001, T-002, T-005 |
| 2 | `[*]` | `[*]` | — | `presentadoEnPapel=true` | `SOLICITUD` | `PENDIENTE_DOCUMENTO_ESCANEADO` | `TRAMITADOR` | T-003, T-004, T-006, T-007 |
| 3 | `SOLICITUD` | `PENDIENTE_DOCUMENTO_ESCANEADO` | `CONTINUAR` | — | `SOLICITUD` | `DATOS_SOLICITUD` | `TRAMITADOR` | T-003, T-004, T-006, T-007 |

Tests de validación fallida: ninguno (estos tests solo cubren el arranque; las validaciones de `CONTINUAR` en `DATOS_SOLICITUD` quedan fuera).
Tests de vistas genéricas de solo lectura: ninguno.
Tests **manuales** (no automatizables): ninguno. La firma con AutoFirma__!! es posterior a la entrada de datos y ningún test llega hasta ella.

### Advertencia sobre los tests en papel (T-003, T-004, T-006 y T-007)

Esos cuatro tests describen **lo que debe pasar**, no lo que hoy pasa.
Quien presenta en papel actúa con perfil `TRAMITADOR`, y en la fase `SOLICITUD` no hay hoy ningún formulario de ese perfil: los tres estados solo tienen el del `CREADOR`.
Con eso, al crear el expediente se abriría la vista genérica de solo lectura, con el único botón «Salir» y sin sitio donde adjuntar la solicitud escaneada.
Si los tests fallan ahí, el fallo es del trámite y no de la descripción del test.

---

## T-001 — El alumno presenta su propia solicitud telemáticamente

**Origen ESC:** —
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `[*]`
**Evento:** — (alta: botón «Crear expediente»)
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** que `alumno1@mislata.es` (contraseña `demo1234`) es alumno de CIPFP Mislata y solo tiene ese centro, y que por ser alumno tiene el perfil `CREADOR` sobre los trámites de alumno.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la ventana «Nuevo expediente» con la tarjeta de ayuda del trámite, el campo «Centro» ya relleno con «CIPFP Mislata» y de solo lectura, y **sin** el interruptor de la forma de presentación (solo tiene el perfil `CREADOR`, así que no hay nada que elegir).
- **And** se muestra la pregunta «¿Para quién es el expediente?» con las opciones «Para mí» y «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)», ninguna marcada.
- **When** marca «Para mí» y pulsa «Crear expediente».
- **Then** se cierra la ventana y se abre el expediente en la fase `SOLICITUD`, estado `DATOS_SOLICITUD`, cuya cabecera muestra «Solicitud de anulación» y «Datos de la solicitud».
- **And** el aviso de la pantalla es «Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador».
- **And** el panel «Persona que presenta la solicitud» **no** aparece: solicitante e interesado son la misma persona.
- **And** en «Alumno/a al que se refiere la solicitud» los campos vienen rellenos con los datos de quien ha entrado —«Apellidos» = «CIPFP Mislata», «Nombre» = «Alumno1», «DNI/NIE» = «86862719E»— y los tres están bloqueados.
- **And** el panel «Datos del alumno/a» («NIA», «Teléfono», «Dirección», «Municipio», «CP») está vacío y editable, y el panel «Matrícula que se anula» muestra el curso académico y «CIPFP Mislata» de solo lectura, con «Ciclo» vacío.
- **And** el pie ofrece los botones «Borrar el expediente» y «Siguiente».

---

## T-002 — El familiar presenta la solicitud en representación de su hijo

**Origen ESC:** —
**Perfil:** `CREADOR` (login `familiar1@mislata.es`)
**Desde:** `[*]`
**Evento:** — (alta: botón «Crear expediente»)
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** que `familiar1@mislata.es` (contraseña `demo1234`) es familiar en CIPFP Mislata, que el data-init de security le da el perfil `CREADOR` sobre los trámites de alumno y que el trámite admite presentar en representación.
- **And** que el hijo al que se refiere la solicitud **no** se elige en ninguna pantalla: la aplicación no guarda ningún vínculo entre el familiar y el alumno, y los datos del hijo se teclean después, en la propia entrada de datos.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la ventana «Nuevo expediente» con «Centro» = «CIPFP Mislata» de solo lectura, sin interruptor de forma de presentación, y con la pregunta «¿Para quién es el expediente?».
- **When** marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)» y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `DATOS_SOLICITUD`, con la cabecera «Solicitud de anulación» / «Datos de la solicitud».
- **And** aparece el panel «Persona que presenta la solicitud» con los datos del familiar que ha entrado —«Apellidos» = «de Alumno1 CIPFP Mislata», «Nombre» = «Familiar1», «DNI/NIE» = «43145636M»— y los tres campos bloqueados.
- **And** en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE» están **vacíos y editables**: es donde se identificará al hijo.
- **And** el pie ofrece «Borrar el expediente» y «Siguiente».

---

## T-003 — El administrativo registra en papel la solicitud que entrega un alumno

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `administrativo1@mislata.es`)
**Desde:** `[*]`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

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

---

## T-004 — El administrativo registra en papel la solicitud que entrega un padre por su hijo

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `administrativo1@mislata.es`)
**Desde:** `[*]`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** que `administrativo1@mislata.es` (contraseña `demo1234`) es administrativo de CIPFP Mislata con el perfil `TRAMITADOR`, y que quien ha entregado la solicitud en ventanilla es el padre de un alumno, que la presenta en representación de su hijo.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno», pulsa sobre «Anulación de matrícula en ciclo formativo», marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)» en la pregunta «¿Para quién es la solicitud? («Para mí» es para la persona que la ha entregado)» y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `PENDIENTE_DOCUMENTO_ESCANEADO`, con el panel «Solicitud entregada en papel» y su campo «Solicitud escaneada (PDF)».
- **When** adjunta un PDF de menos de 10 MB y pulsa «Siguiente».
- **Then** el expediente pasa al estado `DATOS_SOLICITUD`, con la cabecera «Solicitud de anulación» / «Datos de la solicitud».
- **And** aparece el panel «Persona que presenta la solicitud» con «Apellidos», «Nombre» y «DNI/NIE» **vacíos y editables**: es donde se identificará al padre que entregó el papel.
- **And** en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE» están también **vacíos y editables**: es donde se identificará al hijo.
- **And** el pie ofrece «Borrar el expediente», «Atrás» y «Presentar la solicitud».

---

## T-005 — El administrativo que también es alumno presenta su propia solicitud telemáticamente

**Origen ESC:** —
**Perfil:** `CREADOR` (login `administrativo2@mislata.es`)
**Desde:** `[*]`
**Evento:** — (alta: botón «Crear expediente»)
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** que `administrativo2@mislata.es` (contraseña `demo1234`) es a la vez administrativo y alumno de CIPFP Mislata, de modo que tiene los dos perfiles de inicio (`TRAMITADOR` y `CREADOR`) sobre los trámites de alumno, y que quiere anular **su propia** matrícula.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la ventana «Nuevo expediente» y, a diferencia de los tests anteriores, **sí** se muestra el interruptor «Presentado el expediente a partir de un documento en papel», apagado: como tiene los dos perfiles, se le pregunta cómo presenta.
- **And** la pregunta de destinatario es «¿Para quién es el expediente?» (la forma telemática).
- **When** deja el interruptor apagado, marca «Para mí» y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `DATOS_SOLICITUD`, con la cabecera «Solicitud de anulación» / «Datos de la solicitud», exactamente igual que en T-001: el expediente nace como presentación telemática aunque quien entra sea administrativo.
- **And** el aviso es «Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador».
- **And** el panel «Persona que presenta la solicitud» **no** aparece, y en «Alumno/a al que se refiere la solicitud» los campos vienen rellenos con sus propios datos —«Apellidos» = «CIPFP Mislata», «Nombre» = «Administrativo2», «DNI/NIE» = «16493254T»— y bloqueados.
- **And** el pie ofrece «Borrar el expediente» y «Siguiente».

---

## T-006 — El administrativo que también es alumno registra en papel la solicitud de otro alumno

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `administrativo2@mislata.es`)
**Desde:** `[*]`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** que `administrativo2@mislata.es` (contraseña `demo1234`) tiene los dos perfiles de inicio en CIPFP Mislata, y que otro alumno le ha entregado en ventanilla su solicitud de anulación firmada en papel.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno» y pulsa sobre «Anulación de matrícula en ciclo formativo».
- **Then** se abre la ventana «Nuevo expediente» con el interruptor «Presentado el expediente a partir de un documento en papel» apagado y la pregunta «¿Para quién es el expediente?».
- **When** enciende el interruptor.
- **Then** la pregunta de destinatario cambia a «¿Para quién es la solicitud? («Para mí» es para la persona que la ha entregado)» y queda sin marcar.
- **When** marca «Para mí» —la solicitud es del alumno que la ha entregado, no suya— y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `PENDIENTE_DOCUMENTO_ESCANEADO`, con el panel «Solicitud entregada en papel» y su campo «Solicitud escaneada (PDF)».
- **When** adjunta un PDF de menos de 10 MB y pulsa «Siguiente».
- **Then** el expediente pasa al estado `DATOS_SOLICITUD`, con la cabecera «Solicitud de anulación» / «Datos de la solicitud» y el aviso «Copie los datos de la solicitud entregada en papel que ha adjuntado escaneada. Al presentarla se registrará su entrada».
- **And** el panel «Persona que presenta la solicitud» **no** aparece, y en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE» están **vacíos y editables**: no se han precargado los suyos, porque el interesado es el otro alumno.
- **And** el pie ofrece «Borrar el expediente», «Atrás» y «Presentar la solicitud».

---

## T-007 — El administrativo que también es alumno registra en papel la solicitud que entrega un padre por su hijo

**Origen ESC:** —
**Perfil:** `TRAMITADOR` (login `administrativo2@mislata.es`)
**Desde:** `[*]`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** que `administrativo2@mislata.es` (contraseña `demo1234`) tiene los dos perfiles de inicio en CIPFP Mislata, y que quien ha entregado la solicitud en ventanilla es el padre de un alumno, que la presenta en representación de su hijo.
- **When** inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno», pulsa sobre «Anulación de matrícula en ciclo formativo» y enciende el interruptor «Presentado el expediente a partir de un documento en papel».
- **Then** la pregunta de destinatario es «¿Para quién es la solicitud? («Para mí» es para la persona que la ha entregado)».
- **When** marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)» y pulsa «Crear expediente».
- **Then** se abre el expediente en la fase `SOLICITUD`, estado `PENDIENTE_DOCUMENTO_ESCANEADO`, con el panel «Solicitud entregada en papel» y su campo «Solicitud escaneada (PDF)».
- **When** adjunta un PDF de menos de 10 MB y pulsa «Siguiente».
- **Then** el expediente pasa al estado `DATOS_SOLICITUD`, con la cabecera «Solicitud de anulación» / «Datos de la solicitud».
- **And** aparece el panel «Persona que presenta la solicitud» con los tres campos **vacíos y editables** (el padre que entregó el papel), y en «Alumno/a al que se refiere la solicitud» los tres campos están también **vacíos y editables** (el hijo).
- **And** el pie ofrece «Borrar el expediente», «Atrás» y «Presentar la solicitud».
