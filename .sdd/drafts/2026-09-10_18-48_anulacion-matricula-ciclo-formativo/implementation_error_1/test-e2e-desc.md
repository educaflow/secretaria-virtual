# Tests E2E — Anulación de matrícula en ciclo formativo (`AnulacionMatriculaCicloFormativoV1`)

Tests en lenguaje de negocio, Given/When/Then, materializados a partir de los escenarios (`ESC-NNN`) de la especificación y de la tabla de transiciones del diseño. **Sin código Playwright y sin selectores.**

## Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Vía |
|---|---|---|---|---|---|
| `alumno1@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | tramiteCode |
| `alumno2@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | tramiteCode |
| `alumno3@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | tramiteCode |
| `alumno1@batoi.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Batoi | `CREADOR` | tramiteCode |
| `exalumno1@mislata.es` | `demo1234` | tipo de usuario `EXALUMNO` | CIPFP Mislata | *(ninguno)* | — |
| `familiar1@mislata.es` | `demo1234` | tipo de usuario `FAMILIAR` | CIPFP Mislata | *(ninguno)* | — |
| `administrativo1@mislata.es` | `demo1234` | tipo de usuario `ADMINISTRATIVO` | CIPFP Mislata | `SECRETARIO` | tramiteCode |
| `administrativo1@batoi.es` | `demo1234` | tipo de usuario `ADMINISTRATIVO` | CIPFP Batoi | `SECRETARIO` | tramiteCode |
| `director@mislata.es` | `demo1234` | cargo `DIRECTOR` | CIPFP Mislata | `DIRECTOR` | tipoExpedienteCode |
| `secretario@mislata.es` | `demo1234` | cargo `SECRETARIO` | CIPFP Mislata | `RESPONSABLE` | tipoExpedienteCode |
| `vicesecretario@mislata.es` | `demo1234` | cargo `VICESECRETARIO` | CIPFP Mislata | `RESPONSABLE` | tipoExpedienteCode |
| `supervisor1@mislata.es` | `demo1234` | tipo de usuario `SUPERVISOR` | CIPFP Mislata | `RESPONSABLE` | tramiteCode |
| `supervisor1@batoi.es` | `demo1234` | tipo de usuario `SUPERVISOR` | CIPFP Batoi | `RESPONSABLE` | tramiteCode |
| `admin` | `admin` | administrador de la aplicación | todos | *(exento: el motor le exime de comprobar perfil)* | — |

## Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y perfiles, más el trámite «Anulación de matrícula en ciclo formativo» publicado en el árbol de trámites del centro, el catálogo del sistema educativo cargado (con el curso de especialización «Inteligencia Artificial y Big Data» que añade esta iniciativa) y el certificado del Director del centro disponible para la firma en servidor. Ningún test puede presuponer más estado que este.

**Localidad de los centros (aplica a los tests que leen el lugar impreso en los documentos, T-047 entre ellos): no hace falta ninguna precondición.** La línea de lugar y fecha de la solicitud y de la resolución imprime `localidadCentro`, que sale del **municipio** del centro, y los dos centros de demo **ya tienen municipio**: los siembra el catálogo de datos maestros `src/main/java/com/educaflow/subsystem/common/data-init/input/Centro.xml` con `municipio="46169"` (CIPFP Mislata) y `municipio="03009"` (CIPFP Batoi → «Alcoy/Alcoi»), con su bind raíz a `update="true"`, y las carpetas `data-init` se cargan antes que `data-demo`. El entorno de pruebas **MUST NOT** resetear la base de datos por este motivo: sobre la BD de desarrollo habitual, `select c.name, m.name from centro c left join municipio m on m.id = c.municipio` ya devuelve «Mislata» y «Alcoy/Alcoi» (§14 nota 16 del diseño).

**Precondición del administrador (aplica a todo test en el que entra `admin`).** El usuario `admin` lo crea el bootstrap de Axelor y **no tiene centro activo**; `ExpedienteController.getEventContext` lanza «El centro activo es null para el usuario: …» al abrir cualquier expediente sin él. La carga de demo **MUST** darle uno (la línea `<centroUsuario usuarioCode="admin" centroCode="46019660"/>` de `usuarios-demo.xml`, Paso 15 del diseño). El centro elegido (CIPFP Mislata) **no** afecta a lo que ve en «Expedientes Esperando», «Expedientes Cerrados» ni en la búsqueda: esas tres listas le exceptúan del filtro de centro, así que en ellas ve los expedientes de **todos** los centros. **Sí** afecta a «Expedientes Pendientes», que a propósito **no** le exceptúa: ahí solo ve los de CIPFP Mislata.

**Precondición de firma (aplica a todos los tests que presentan la solicitud).** Quién firma la solicitud y cómo lo decide la *situación de firma* del alumno, que el servidor recalcula de su DNI: si tiene un certificado custodiado dado de alta, la firma la hace el servidor al pulsar el botón (paso automatizable); si no lo tiene, el botón lanza la aplicación de firma del ciudadano en su equipo (paso **no** automatizable). El entorno de pruebas **MUST** dejar los certificados digitales custodiados (pantalla Administración → Certificados digitales) exactamente así, porque hay tests que dependen de **cada** una de las tres situaciones:

| Alumno | Certificado custodiado | Para qué |
|---|---|---|
| `alumno1@mislata.es` | **sí**, con su contraseña guardada | firma en servidor sin pedir nada: es el alumno del camino feliz |
| `alumno1@batoi.es` | **sí**, con su contraseña guardada | el camino feliz del otro centro |
| `alumno2@mislata.es` | **sí**, **sin** la contraseña guardada | la pantalla le pide la contraseña del certificado: es el único modo de probar una clave incorrecta (T-055) |
| `alumno3@mislata.es` | **no** | firma en el equipo del alumno con la aplicación de firma del ciudadano (T-019 y T-020, `Manual: sí`) |
| `alumno4@mislata.es` | **no** | no inicia sesión en ningún test; su documento de identidad «62584352H» es el del certificado ajeno con el que se firma en T-020 |

Los dos únicos tests que ejercitan a propósito la rama de firma en el equipo del alumno (T-019 y T-020) están marcados `Manual: sí` y **MUST** ejecutarse con `alumno3@mislata.es`, que esta tabla deja **sin** certificado custodiado. Es configuración del entorno, no un fichero del proyecto.

### Juego de datos válido — fase `SOLICITUD` (alumno de CIPFP Mislata)

| campo | valor |
|---|---|
| «NIA» | `12345678` |
| «Dirección» | `C/ Mayor, 12` |
| «Teléfono» | `612345678` |
| «Población» | `Mislata` |
| «Provincia» | `Valencia` |
| «Código postal» | `46920` |
| «Ciclo formativo» | `Desarrollo de Aplicaciones Web` |

### Juego de datos válido — fase `SOLICITUD` (alumno de CIPFP Batoi)

| campo | valor |
|---|---|
| «NIA» | `87654321` |
| «Dirección» | `C/ Sant Nicolau, 5` |
| «Teléfono» | `698765432` |
| «Población» | `Alcoi` |
| «Provincia» | `Alicante` |
| «Código postal» | `03800` |
| «Ciclo formativo» | `Sistemas Microinformáticos y Redes` |

### Juego de datos válido — fase `REVISION`

| campo | valor |
|---|---|
| «Sentido de la revisión» | `Aceptar la anulación` (o `Rechazar la anulación` / `Pedir subsanación` según el test) |
| «Motivo del rechazo» | `La solicitud se presenta fuera del plazo establecido para la anulación de matrícula` |
| «Qué hay que subsanar» | `Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma` |

### Juego de datos válido — fase `RESOLUCION`

| campo | valor |
|---|---|
| «Motivo de la devolución» | `El alumno ya ha superado el plazo de anulación; la solicitud debe rechazarse` |

## Cobertura de transiciones

| # | fase origen | estado origen | evento | guarda | fase destino | estado destino | perfil | test |
|---|---|---|---|---|---|---|---|---|
| 1 | `[*]` | `[*]` | — | — | `SOLICITUD` | `DATOS_SOLICITUD` | `CREADOR` | T-001 |
| 2 | `SOLICITUD` | `DATOS_SOLICITUD` | `CONTINUAR` | — | `SOLICITUD` | `PENDIENTE_FIRMA` | `CREADOR` | T-002 |
| 3 | `SOLICITUD` | `DATOS_SOLICITUD` | `DELETE` | — | `[*]` | `[*]` | `CREADOR` | T-003 |
| 4 | `SOLICITUD` | `PENDIENTE_FIRMA` | `VOLVER` | — | `SOLICITUD` | `DATOS_SOLICITUD` | `CREADOR` | T-004 |
| 5 | `SOLICITUD` | `PENDIENTE_FIRMA` | `PRESENTAR` | — | `REVISION` | `PENDIENTE_REVISION` | `CREADOR` | T-005 |
| 6 | `REVISION` | `PENDIENTE_REVISION` | `ENVIAR_A_FIRMA` | — | `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `SECRETARIO` | T-006, T-007 |
| 7 | `REVISION` | `PENDIENTE_REVISION` | `SUBSANAR` | — | `SOLICITUD` | `DATOS_SOLICITUD` | `SECRETARIO` | T-008 |
| 8 | `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=ACEPTAR` | `RESOLUCION` | `ACEPTADA` | `DIRECTOR` | T-009 |
| 9 | `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=RECHAZAR` | `RESOLUCION` | `RECHAZADA` | `DIRECTOR` | T-010 |
| 10 | `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `DEVOLVER` | — | `REVISION` | `PENDIENTE_REVISION` | `DIRECTOR` | T-011 |

Tests de validación fallida: T-012, T-013, T-014, T-015, T-016, T-017, T-018, T-019, T-020, T-021, T-022, T-023, T-024, T-025, T-026, T-027, T-028, T-029, T-055.
Tests de vistas genéricas de solo lectura: T-030, T-031, T-032, T-033, T-034, T-035, T-036, T-037, T-038, T-039, T-040, T-041, T-042, T-043, T-056 (los cinco últimos son además los tests de aislamiento: comprueban que quien no debe ver el expediente —o no debe verlo con el perfil de otro, T-056— no lo ve en **ninguna** lista; llevan el mismo `Tipo: solo-lectura` que los demás).
Tests **manuales** (no automatizables, firma en el equipo del alumno): T-019, T-020.

---

## T-001 — El alumno crea el expediente y el sistema rellena sus datos de identidad y de matrícula

**Origen ESC:** ESC-001
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `[*]`
**Evento:** —
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha iniciado sesión con la contraseña «demo1234» y abre la lista de trámites disponibles del centro CIPFP Mislata.
- **When** consulta la ayuda del trámite «Anulación de matrícula en ciclo formativo» y pulsa crear un expediente nuevo.
- **Then** el sistema abre el expediente en la fase «Solicitud de anulación» y el estado «Datos de la solicitud».
- **And** en el bloque «Datos de identificación del alumno» ve ya rellenos y sin poder cambiarlos «Apellidos» = «CIPFP Mislata», «Nombre» = «Alumno1» y «DNI/NIE» = «86862719E»; en «Matrícula que se anula» ve «Curso académico» = «2024/2025» y «Centro» = «CIPFP Mislata», ambos sin poder cambiarlos, y «Ciclo formativo» vacío.
- **And** la pantalla ofrece los botones «Siguiente» y «Borrar el expediente», y el aviso «Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador».

---

## T-002 — El alumno completa los datos y el sistema genera la solicitud

**Origen ESC:** ESC-001
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite y lo tiene abierto en «Solicitud de anulación» / «Datos de la solicitud».
- **When** rellena el juego de datos válido de la fase `SOLICITUD` (Mislata), eligiendo el ciclo desde la ventana de búsqueda tras filtrar por la familia profesional «Informática y Comunicaciones» y el nivel «Ciclos Formativos de Grado Superior», y pulsa «Siguiente».
- **Then** el expediente pasa a la fase «Solicitud de anulación» y el estado «Pendiente de firma y presentación».
- **And** el sistema muestra la solicitud de anulación generada, incrustada en la pantalla, con los datos introducidos, y ofrece los botones «Atrás» y «Firmar y presentar la solicitud».

---

## T-003 — El alumno borra el expediente antes de presentarlo

**Origen ESC:** ESC-007
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `DELETE` — botón «Borrar el expediente»
**Hasta:** `[*]`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite y lo tiene abierto en «Solicitud de anulación» / «Datos de la solicitud».
- **When** pulsa «Borrar el expediente» y confirma el aviso «Se va a eliminar el expediente y no podrá recuperarlo».
- **Then** el sistema elimina el expediente y vuelve a la lista.
- **And** el expediente ya no aparece en la lista de expedientes del alumno.

---

## T-004 — El alumno vuelve atrás desde la pantalla de firma y corrige el ciclo

**Origen ESC:** ESC-005
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Evento:** `VOLVER` — botón «Atrás»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente, ha rellenado el juego de datos válido eligiendo el ciclo «Desarrollo de Aplicaciones Multiplataforma» y ha pulsado «Siguiente», de modo que el expediente está en «Solicitud de anulación» / «Pendiente de firma y presentación» con la solicitud incrustada en la que se lee «Desarrollo de Aplicaciones Multiplataforma».
- **When** pulsa «Atrás».
- **Then** el expediente vuelve a la fase «Solicitud de anulación» y el estado «Datos de la solicitud», con todos los datos que había introducido.
- **And** cambia el ciclo a «Desarrollo de Aplicaciones Web», vuelve a pulsar «Siguiente», el expediente queda otra vez en «Pendiente de firma y presentación» y en la solicitud incrustada se lee ahora «Desarrollo de Aplicaciones Web».

---

## T-005 — El alumno firma y presenta la solicitud

**Origen ESC:** ESC-001
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Evento:** `PRESENTAR` — botón «Firmar y presentar la solicitud»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es», que tiene un certificado digital custodiado dado de alta, ha creado un expediente, ha rellenado el juego de datos válido de la fase `SOLICITUD` (Mislata) y ha pulsado «Siguiente», de modo que el expediente está en «Solicitud de anulación» / «Pendiente de firma y presentación».
- **When** pulsa «Firmar y presentar la solicitud», confirma el aviso «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla» y, si se le pide, teclea la contraseña de su certificado.
- **Then** el expediente pasa a la fase «Revisión de la secretaría» y el estado «Pendiente de revisión».
- **And** el alumno ve el expediente en solo consulta, con la fecha y la hora de presentación (las de hoy), el justificante de presentación incrustado en la pantalla, la solicitud firmada como descarga y un único botón «Salir».
- **And** en «Ver el historial de estados» aparece el registro de entrada de la presentación.

---

## T-006 — La administrativa acepta la anulación y la envía a la firma del director

**Origen ESC:** ESC-008
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `ENVIAR_A_FIRMA` — botón «Enviar a la firma del director»
**Hasta:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud con el juego de datos válido y ha cerrado sesión, y la administrativa «administrativo1@mislata.es» ha iniciado sesión y abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`); lo ve en «Revisión de la secretaría» / «Pendiente de revisión», con la solicitud firmada incrustada.
- **When** elige el sentido de la revisión «Aceptar la anulación» y pulsa «Enviar a la firma del director», confirmando el aviso «Va a enviar la resolución a la firma del director».
- **Then** el expediente pasa a la fase «Resolución del centro» y el estado «Pendiente de la firma del director».
- **And** la administrativa lo ve ya en solo consulta, con el aviso «La resolución está pendiente de la firma del director», sin la resolución y con un único botón «Salir».
- **And** el director «director@mislata.es» abre después el expediente desde la bandeja «Anulaciones de matrícula pendientes de mi firma» (la que abre los expedientes con el perfil `DIRECTOR`) y ve el bloque «Decisión de secretaría» con «Aceptar la anulación», la fecha de la revisión de hoy y «Administrativo1 CIPFP Mislata» como quien la hizo, y la resolución sin firmar incrustada en la que se lee «Se estima la solicitud y la matrícula queda sin efecto a partir de la fecha de presentación».

---

## T-007 — La administrativa rechaza la anulación con motivo y la envía a la firma del director

**Origen ESC:** ESC-009
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `ENVIAR_A_FIRMA` — botón «Enviar a la firma del director»
**Hasta:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud con el juego de datos válido y ha cerrado sesión, y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido de la revisión «Rechazar la anulación», escribe el motivo del rechazo del juego de datos de la fase `REVISION` y pulsa «Enviar a la firma del director», confirmando el aviso.
- **Then** el expediente pasa a la fase «Resolución del centro» y el estado «Pendiente de la firma del director».
- **And** el director «director@mislata.es» abre después el expediente desde la bandeja «Anulaciones de matrícula pendientes de mi firma» (la que abre los expedientes con el perfil `DIRECTOR`) y ve el bloque «Decisión de secretaría» con «Rechazar la anulación» y ese motivo del rechazo, y la resolución sin firmar incrustada en la que se lee «Se desestima la solicitud» y, tras «Por el siguiente motivo:», ese mismo motivo.

---

## T-008 — La administrativa pide al alumno que subsane

**Origen ESC:** ESC-012
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `SUBSANAR` — botón «Pedir subsanación al alumno»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud eligiendo el ciclo «Desarrollo de Aplicaciones Multiplataforma» y ha cerrado sesión, y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido de la revisión «Pedir subsanación», escribe en «Qué hay que subsanar» el texto del juego de datos de la fase `REVISION` y pulsa «Pedir subsanación al alumno», confirmando el aviso «Va a devolver la solicitud al alumno para que la corrija».
- **Then** el expediente vuelve a la fase «Solicitud de anulación» y el estado «Datos de la solicitud», sin que se haya generado ninguna resolución.
- **And** el alumno «alumno1@mislata.es» abre después el expediente y ve, en solo lectura, el bloque «Qué hay que subsanar» con ese texto, y todos sus datos editables.

---

## T-009 — El director firma una resolución de aceptación y el expediente se cierra

**Origen ESC:** ESC-014
**Perfil:** `DIRECTOR` (login `director@mislata.es`)
**Desde:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Evento:** `FIRMAR` — botón «Firmar la resolución»
**Hasta:** `RESOLUCION` / `ACEPTADA`
**Tipo:** happy
**Manual:** no

- **Given** el alumno ha presentado su solicitud, la administrativa «administrativo1@mislata.es» ha elegido «Aceptar la anulación» y la ha enviado a la firma, y el director «director@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula pendientes de mi firma» (la que abre los expedientes con el perfil `DIRECTOR`), en «Resolución del centro» / «Pendiente de la firma del director», con la resolución sin firmar incrustada en la que se lee «Se estima».
- **When** pulsa «Firmar la resolución» y confirma el aviso «Va a firmar la resolución. Una vez firmada se registrará de salida y el expediente quedará cerrado».
- **Then** el expediente queda cerrado en la fase «Resolución del centro» y el estado «Anulación aceptada».
- **And** la pantalla muestra el bloque «Resolución» con la fecha de la resolución (la de hoy), «Director CIPFP Mislata» como quien la firmó y la resolución firmada incrustada, y el único botón es «Salir».
- **And** en «Ver el historial de estados» aparece el registro de salida de la resolución.

---

## T-010 — El director firma una resolución de rechazo y el expediente se cierra

**Origen ESC:** ESC-015
**Perfil:** `DIRECTOR` (login `director@mislata.es`)
**Desde:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Evento:** `FIRMAR` — botón «Firmar la resolución»
**Hasta:** `RESOLUCION` / `RECHAZADA`
**Tipo:** happy
**Manual:** no

- **Given** el alumno ha presentado su solicitud, la administrativa ha elegido «Rechazar la anulación» con el motivo del juego de datos de la fase `REVISION` y la ha enviado a la firma, y el director «director@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula pendientes de mi firma» (la que abre los expedientes con el perfil `DIRECTOR`), en «Resolución del centro» / «Pendiente de la firma del director», con la resolución sin firmar incrustada en la que se lee «Se desestima» y el motivo.
- **When** pulsa «Firmar la resolución» y confirma el aviso.
- **Then** el expediente queda cerrado en la fase «Resolución del centro» y el estado «Anulación rechazada».
- **And** la pantalla muestra la resolución firmada incrustada, en la que se lee «Se desestima la solicitud» y, tras «Por el siguiente motivo:», el motivo del rechazo; y el bloque «Resolución» con la fecha de la resolución y «Director CIPFP Mislata» como quien la firmó.

---

## T-011 — El director devuelve la resolución a secretaría y secretaría decide de nuevo

**Origen ESC:** ESC-017
**Perfil:** `DIRECTOR` (login `director@mislata.es`)
**Desde:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Evento:** `DEVOLVER` — botón «Devolver a secretaría»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** happy
**Manual:** no

- **Given** el alumno ha presentado su solicitud, la administrativa ha elegido «Aceptar la anulación» y la ha enviado a la firma, y el director «director@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula pendientes de mi firma» (la que abre los expedientes con el perfil `DIRECTOR`), en «Resolución del centro» / «Pendiente de la firma del director».
- **When** escribe el motivo de la devolución del juego de datos de la fase `RESOLUCION` y pulsa «Devolver a secretaría», confirmando el aviso «Va a devolver la resolución a secretaría sin firmarla».
- **Then** el expediente vuelve a la fase «Revisión de la secretaría» y el estado «Pendiente de revisión», sin firmar ni registrar nada.
- **And** la administrativa «administrativo1@mislata.es» abre después el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`) y ve el bloque «Devuelto por el director» con ese motivo y el sentido «Aceptar la anulación» que había elegido, editable.
- **And** cambia el sentido a «Rechazar la anulación», escribe el motivo del rechazo, pulsa «Enviar a la firma del director», y el expediente vuelve a «Resolución del centro» / «Pendiente de la firma del director» con la resolución regenerada en la que se lee «Se desestima» y sin el bloque «Devuelto por el director».

---

## T-012 — Se pulsa «Siguiente» sin rellenar el NIA

**Origen ESC:** ESC-002
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite.
- **When** deja el «NIA» vacío, rellena el resto del juego de datos válido (Mislata) y pulsa «Siguiente».
- **Then** el sistema muestra el mensaje «Debe indicar su NIA».
- **And** el expediente sigue en la fase «Solicitud de anulación» y el estado «Datos de la solicitud», y no se ha generado ninguna solicitud.

---

## T-013 — Se pulsa «Siguiente» con el teléfono y el código postal mal formados

**Origen ESC:** ESC-003
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite.
- **When** rellena el juego de datos válido (Mislata) pero con el «Teléfono» = «12345» y el «Código postal» = «469», y pulsa «Siguiente».
- **Then** el sistema muestra los mensajes «El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9» y «El código postal debe tener 5 dígitos», y el expediente sigue en «Solicitud de anulación» / «Datos de la solicitud».
- **And** al corregir el teléfono a «612345678» y el código postal a «46920» y volver a pulsar «Siguiente», el sistema genera la solicitud y el expediente pasa a «Pendiente de firma y presentación».

---

## T-014 — Se pulsa «Siguiente» sin elegir el ciclo

**Origen ESC:** ESC-004
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite.
- **When** rellena el juego de datos válido (Mislata) pero deja el «Ciclo formativo» sin elegir y pulsa «Siguiente».
- **Then** el sistema muestra el mensaje «Debe indicar el ciclo formativo».
- **And** el expediente sigue en la fase «Solicitud de anulación» y el estado «Datos de la solicitud».

---

## T-015 — Se pulsa «Siguiente» con los datos de contacto vacíos

**Origen ESC:** ESC-026
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite.
- **When** rellena el «NIA» con «12345678», elige el ciclo «Desarrollo de Aplicaciones Web», deja vacíos la «Dirección», el «Teléfono», la «Población», la «Provincia» y el «Código postal», y pulsa «Siguiente».
- **Then** el sistema muestra los mensajes «Debe indicar su dirección», «Debe indicar un teléfono de contacto», «Debe indicar su población», «Debe indicar su provincia» y «Debe indicar su código postal».
- **And** el expediente sigue en «Solicitud de anulación» / «Datos de la solicitud» sin generar ninguna solicitud.

---

## T-016 — Se pulsa «Siguiente» con el NIA de 7 dígitos y la dirección demasiado corta

**Origen ESC:** ESC-027
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite.
- **When** rellena el juego de datos válido (Mislata) pero con el «NIA» = «1234567» y la «Dirección» = «C/ A», y pulsa «Siguiente».
- **Then** el sistema muestra los mensajes «El NIA debe tener 8 dígitos» y «La dirección debe tener entre 5 y 150 caracteres», y el expediente sigue en «Solicitud de anulación» / «Datos de la solicitud».
- **And** al corregir el NIA a «12345678» y la dirección a «C/ Mayor, 12» y volver a pulsar «Siguiente», el sistema genera la solicitud y el expediente pasa a «Pendiente de firma y presentación».

---

## T-017 — Se pulsa «Siguiente» con la población y la provincia demasiado cortas

**Origen ESC:** ESC-040
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite.
- **When** rellena el juego de datos válido (Mislata) pero con la «Población» = «M» y la «Provincia» = «V», y pulsa «Siguiente».
- **Then** el sistema muestra los mensajes «La población debe tener entre 2 y 100 caracteres» y «La provincia debe tener entre 2 y 50 caracteres», y el expediente sigue en «Solicitud de anulación» / «Datos de la solicitud» sin generar ninguna solicitud.
- **And** al corregir la población a «Mislata» y la provincia a «Valencia» y volver a pulsar «Siguiente», el sistema genera la solicitud y el expediente pasa a «Pendiente de firma y presentación».

---

## T-018 — Se pulsa «Siguiente» teniendo ya otra solicitud en curso para el mismo ciclo

**Origen ESC:** ESC-033
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ya ha presentado una solicitud con el ciclo «Desarrollo de Aplicaciones Web» que está en «Revisión de la secretaría» / «Pendiente de revisión», y ha creado un segundo expediente nuevo del mismo trámite.
- **When** rellena en el segundo expediente el juego de datos válido (Mislata), con el mismo ciclo «Desarrollo de Aplicaciones Web», y pulsa «Siguiente».
- **Then** el sistema muestra el mensaje «Ya tiene una solicitud de anulación en curso para este ciclo».
- **And** el segundo expediente sigue en «Solicitud de anulación» / «Datos de la solicitud» sin generar ninguna solicitud; al cambiar el ciclo a «Desarrollo de Aplicaciones Multiplataforma» y volver a pulsar «Siguiente», el sistema genera la solicitud y el expediente pasa a «Pendiente de firma y presentación».

---

## T-019 — Se intenta presentar la solicitud sin firmarla

**Origen ESC:** ESC-006
**Perfil:** `CREADOR` (login `alumno3@mislata.es`)
**Desde:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Evento:** `PRESENTAR` — botón «Firmar y presentar la solicitud»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** error
**Manual:** sí — el paso «Firmar y presentar la solicitud» abre la aplicación de firma del ciudadano en el equipo del alumno, y el test consiste precisamente en cancelarla ahí; exige además un alumno **sin** certificado custodiado.

- **Given** el alumno «alumno3@mislata.es», que la precondición de firma deja **sin** certificado digital custodiado, ha creado un expediente, ha rellenado el juego de datos válido (Mislata) y ha pulsado «Siguiente», de modo que el expediente está en «Solicitud de anulación» / «Pendiente de firma y presentación».
- **When** pulsa «Firmar y presentar la solicitud», confirma el aviso «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla» y cancela la firma sin firmar el documento.
- **Then** el sistema muestra el mensaje «Debe firmar la solicitud antes de presentarla».
- **And** el expediente sigue en la fase «Solicitud de anulación» y el estado «Pendiente de firma y presentación», y no se ha dejado constancia de ninguna entrada.

---

## T-020 — Se firma la solicitud con un certificado que no es el del alumno

**Origen ESC:** ESC-028
**Perfil:** `CREADOR` (login `alumno3@mislata.es`)
**Desde:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Evento:** `PRESENTAR` — botón «Firmar y presentar la solicitud»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** error
**Manual:** sí — hay que firmar a mano, en el equipo, con un segundo certificado de pruebas que corresponde a otra persona.

- **Given** el alumno «alumno3@mislata.es», que la precondición de firma deja **sin** certificado digital custodiado, ha creado un expediente, ha rellenado el juego de datos válido (Mislata) y ha pulsado «Siguiente», de modo que el expediente está en «Solicitud de anulación» / «Pendiente de firma y presentación».
- **When** pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con el certificado digital que corresponde al documento de identidad «62584352H» (el del alumno «alumno4@mislata.es», que la precondición de firma también deja sin certificado custodiado).
- **Then** el sistema muestra el mensaje «La firma no es válida o no corresponde a su documento de identidad».
- **And** el expediente sigue en la fase «Solicitud de anulación» y el estado «Pendiente de firma y presentación», y no se ha dejado constancia de ninguna entrada.

---

## T-021 — Se pulsa «Enviar a la firma del director» sin elegir el sentido

**Origen ESC:** ESC-010
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `ENVIAR_A_FIRMA` — botón «Enviar a la firma del director»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** pulsa «Enviar a la firma del director» sin elegir ningún sentido de la revisión y confirma el aviso «Va a enviar la resolución a la firma del director».
- **Then** el sistema muestra el mensaje «Debe indicar el sentido de la revisión».
- **And** el expediente sigue en la fase «Revisión de la secretaría» y el estado «Pendiente de revisión», sin generar ninguna resolución.

---

## T-022 — Se propone rechazar la anulación sin escribir el motivo

**Origen ESC:** ESC-009
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `ENVIAR_A_FIRMA` — botón «Enviar a la firma del director»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido de la revisión «Rechazar la anulación», deja vacío el «Motivo del rechazo», pulsa «Enviar a la firma del director» y confirma el aviso.
- **Then** el sistema muestra el mensaje «Debe indicar el motivo del rechazo».
- **And** el expediente sigue en «Revisión de la secretaría» / «Pendiente de revisión» sin generar ninguna resolución.

---

## T-023 — Se propone rechazar con un motivo demasiado corto

**Origen ESC:** ESC-042
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `ENVIAR_A_FIRMA` — botón «Enviar a la firma del director»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido «Rechazar la anulación», escribe el motivo del rechazo «Plazo», pulsa «Enviar a la firma del director» y confirma el aviso.
- **Then** el sistema muestra el mensaje «El motivo del rechazo debe tener entre 10 y 1000 caracteres» y el expediente sigue en «Revisión de la secretaría» / «Pendiente de revisión» sin generar ninguna resolución.
- **And** al corregir el motivo al del juego de datos de la fase `REVISION` y volver a enviar, el sistema genera la resolución sin firmar y el expediente pasa a «Resolución del centro» / «Pendiente de la firma del director».

---

## T-024 — Se pulsa «Enviar a la firma del director» con el sentido «Pedir subsanación»

**Origen ESC:** ESC-041
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `ENVIAR_A_FIRMA` — botón «Enviar a la firma del director»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido de la revisión «Pedir subsanación», escribe en «Qué hay que subsanar» el texto del juego de datos de la fase `REVISION`, pulsa «Enviar a la firma del director» y confirma el aviso.
- **Then** el sistema muestra el mensaje «Para pedir una subsanación use el botón «Pedir subsanación al alumno»».
- **And** no se genera ninguna resolución, el expediente sigue en «Revisión de la secretaría» / «Pendiente de revisión» y conserva en pantalla el sentido «Pedir subsanación» y el texto de la subsanación tal como los había escrito.

---

## T-025 — Se pide subsanación sin indicar qué hay que subsanar

**Origen ESC:** ESC-011
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `SUBSANAR` — botón «Pedir subsanación al alumno»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido de la revisión «Pedir subsanación», pulsa «Pedir subsanación al alumno» sin escribir qué hay que subsanar y confirma el aviso «Va a devolver la solicitud al alumno para que la corrija».
- **Then** el sistema muestra el mensaje «Debe indicar al alumno qué tiene que subsanar».
- **And** el expediente sigue en la fase «Revisión de la secretaría» y el estado «Pendiente de revisión».

---

## T-026 — Se pide subsanación con un texto demasiado corto

**Origen ESC:** ESC-044
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `SUBSANAR` — botón «Pedir subsanación al alumno»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud eligiendo el ciclo «Desarrollo de Aplicaciones Multiplataforma» y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido «Pedir subsanación», escribe en «Qué hay que subsanar» el texto «Ciclo», pulsa «Pedir subsanación al alumno» y confirma el aviso.
- **Then** el sistema muestra el mensaje «El texto de la subsanación debe tener entre 10 y 1000 caracteres» y el expediente sigue en «Revisión de la secretaría» / «Pendiente de revisión».
- **And** al corregir el texto al del juego de datos de la fase `REVISION` y volver a pulsar «Pedir subsanación al alumno», el expediente vuelve a «Solicitud de anulación» / «Datos de la solicitud».

---

## T-027 — Se pulsa «Pedir subsanación al alumno» con el sentido «Aceptar la anulación»

**Origen ESC:** ESC-043
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `SUBSANAR` — botón «Pedir subsanación al alumno»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido de la revisión «Aceptar la anulación», pulsa «Pedir subsanación al alumno» y confirma el aviso.
- **Then** el sistema muestra el mensaje «Para pedir una subsanación elija el sentido «Pedir subsanación»».
- **And** el expediente sigue en la fase «Revisión de la secretaría» y el estado «Pendiente de revisión», sin volver al alumno.

---

## T-028 — El director devuelve la resolución sin indicar el motivo

**Origen ESC:** ESC-016
**Perfil:** `DIRECTOR` (login `director@mislata.es`)
**Desde:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Evento:** `DEVOLVER` — botón «Devolver a secretaría»
**Hasta:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Tipo:** error
**Manual:** no

- **Given** el alumno ha presentado su solicitud, la administrativa ha elegido «Aceptar la anulación» y la ha enviado a la firma, y el director «director@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula pendientes de mi firma» (la que abre los expedientes con el perfil `DIRECTOR`), en «Resolución del centro» / «Pendiente de la firma del director».
- **When** pulsa «Devolver a secretaría» sin escribir el motivo de la devolución y confirma el aviso «Va a devolver la resolución a secretaría sin firmarla».
- **Then** el sistema muestra el mensaje «Debe indicar a secretaría por qué devuelve la resolución».
- **And** el expediente sigue en la fase «Resolución del centro» y el estado «Pendiente de la firma del director».

---

## T-029 — El director devuelve la resolución con un motivo demasiado corto

**Origen ESC:** ESC-047
**Perfil:** `DIRECTOR` (login `director@mislata.es`)
**Desde:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Evento:** `DEVOLVER` — botón «Devolver a secretaría»
**Hasta:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Tipo:** error
**Manual:** no

- **Given** el alumno ha presentado su solicitud, la administrativa ha elegido «Aceptar la anulación» y la ha enviado a la firma, y el director «director@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula pendientes de mi firma» (la que abre los expedientes con el perfil `DIRECTOR`), en «Resolución del centro» / «Pendiente de la firma del director».
- **When** escribe el motivo de la devolución «Revisar» y pulsa «Devolver a secretaría», confirmando el aviso.
- **Then** el sistema muestra el mensaje «El motivo de la devolución debe tener entre 10 y 1000 caracteres».
- **And** el expediente sigue en la fase «Resolución del centro» y el estado «Pendiente de la firma del director».

---

## T-030 — El alumno consulta su expediente mientras está en revisión

**Origen ESC:** ESC-018
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud, que está en «Revisión de la secretaría» / «Pendiente de revisión»; entra por la bandeja «Expedientes Pendientes», que abre los expedientes con el perfil `CREADOR`; en este estado **no hay form para ese perfil**, así que cae en la pantalla de solo lectura del estado.
- **When** abre el expediente desde esa lista.
- **Then** el sistema lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión» en la cabecera y el aviso «La solicitud está pendiente de revisión por la secretaría del centro».
- **And** ve el justificante de presentación incrustado y la solicitud firmada como descarga; el único botón es «Salir», y al pulsarlo vuelve a la lista sin que el expediente cambie de estado.

---

## T-031 — El alumno consulta su expediente pendiente de la firma del director y no ve la decisión de secretaría

**Origen ESC:** ESC-029
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Evento:** —
**Hasta:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud, la administrativa «administrativo1@mislata.es» ha elegido «Rechazar la anulación» con el motivo del juego de datos de la fase `REVISION` y la ha enviado a la firma; el alumno entra por la bandeja «Expedientes Pendientes», que abre los expedientes con el perfil `CREADOR`; en este estado **no hay form para ese perfil**, así que cae en la pantalla de solo lectura del estado.
- **When** abre el expediente desde esa lista.
- **Then** el sistema lo muestra en solo lectura, con la fase «Resolución del centro» y el estado «Pendiente de la firma del director» en la cabecera y el aviso «La resolución está pendiente de la firma del director».
- **And** ve el justificante de presentación incrustado y la solicitud firmada como descarga, y **no** ve el sentido de la revisión, ni el motivo del rechazo, ni la resolución sin firmar; el único botón es «Salir».

---

## T-032 — El alumno consulta un expediente cerrado con la anulación aceptada

**Origen ESC:** ESC-019
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `RESOLUCION` / `ACEPTADA`
**Evento:** —
**Hasta:** `RESOLUCION` / `ACEPTADA`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el expediente del alumno «alumno1@mislata.es» ha recorrido el camino completo hasta que el director firmó una resolución de aceptación, y está cerrado en «Resolución del centro» / «Anulación aceptada»; el alumno entra por la bandeja «Expedientes Cerrados», que abre los expedientes con el perfil `RESPONSABLE`; en este estado **no hay form para ese perfil**, así que cae en la pantalla de solo lectura del estado.
- **When** abre el expediente desde esa lista.
- **Then** el sistema lo muestra en solo lectura, con la fase «Resolución del centro» y el estado «Anulación aceptada» en la cabecera y el aviso «El expediente está cerrado».
- **And** en «Presentación» ve la fecha y la hora de presentación (las de hoy) y el justificante como descarga; en «Resolución» ve la fecha de la resolución (la de hoy), «Director CIPFP Mislata» como quien la firmó y la resolución firmada incrustada, en la que se lee «Se estima la solicitud y la matrícula queda sin efecto a partir de la fecha de presentación» y no aparece ningún motivo; el único botón es «Salir».

---

## T-033 — El alumno consulta un expediente cerrado con la anulación rechazada y lee el motivo

**Origen ESC:** ESC-030
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `RESOLUCION` / `RECHAZADA`
**Evento:** —
**Hasta:** `RESOLUCION` / `RECHAZADA`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el expediente del alumno «alumno1@mislata.es» ha recorrido el camino completo hasta que el director firmó una resolución de rechazo con el motivo del juego de datos de la fase `REVISION`, y está cerrado en «Resolución del centro» / «Anulación rechazada»; el alumno entra por la bandeja «Expedientes Cerrados», que abre los expedientes con el perfil `RESPONSABLE`; en este estado **no hay form para ese perfil**, así que cae en la pantalla de solo lectura del estado.
- **When** abre el expediente desde esa lista.
- **Then** el sistema lo muestra en solo lectura, con la fase «Resolución del centro» y el estado «Anulación rechazada» en la cabecera y el aviso «El expediente está cerrado».
- **And** en «Resolución» ve el motivo del rechazo, la fecha de la resolución, «Director CIPFP Mislata» como quien la firmó y la resolución firmada incrustada, en la que se lee «Se desestima la solicitud» y, tras «Por el siguiente motivo:», ese mismo motivo; el único botón es «Salir».

---

## T-034 — El secretario del centro consulta un expediente en revisión

**Origen ESC:** ESC-020
**Perfil:** `RESPONSABLE` (login `secretario@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y el secretario «secretario@mislata.es» entra por la bandeja «Expedientes Esperando», que abre los expedientes de su centro con el perfil `RESPONSABLE`; en este estado **no hay form para ese perfil**, así que cae en la pantalla de solo lectura del estado.
- **When** abre el expediente desde esa lista.
- **Then** el sistema lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión» en la cabecera.
- **And** ve los datos del alumno y de la matrícula, la fecha y la hora de presentación, el justificante de presentación incrustado y la solicitud firmada como descarga; **no** se le ofrecen «Enviar a la firma del director» ni «Pedir subsanación al alumno», ni se le muestra el sentido de la revisión, y el único botón es «Salir».

---

## T-035 — El secretario del centro consulta un expediente que el alumno todavía no ha firmado

**Origen ESC:** ESC-032
**Perfil:** `RESPONSABLE` (login `secretario@mislata.es`)
**Desde:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Evento:** —
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente, ha rellenado el juego de datos válido (Mislata) y ha pulsado «Siguiente», dejándolo en «Solicitud de anulación» / «Pendiente de firma y presentación»; el secretario «secretario@mislata.es» entra por la bandeja «Expedientes Esperando», que abre los expedientes de su centro con el perfil `RESPONSABLE`.
- **When** abre el expediente desde esa lista.
- **Then** el sistema lo muestra en solo lectura, con la fase «Solicitud de anulación» y el estado «Pendiente de firma y presentación» en la cabecera.
- **And** ve los datos del alumno y de la matrícula y la solicitud generada y sin firmar como descarga; **no** se le ofrecen «Atrás», «Firmar y presentar la solicitud» ni «Borrar el expediente», y el único botón es «Salir».

---

## T-036 — El vicesecretario consulta un expediente de su centro en solo lectura

**Origen ESC:** ESC-049
**Perfil:** `RESPONSABLE` (login `vicesecretario@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y el vicesecretario «vicesecretario@mislata.es» entra por la bandeja «Expedientes Esperando», que abre los expedientes de su centro CIPFP Mislata con el perfil `RESPONSABLE`.
- **When** abre el expediente desde esa lista.
- **Then** el sistema lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión» en la cabecera.
- **And** ve los datos del alumno y de la matrícula, la fecha y la hora de presentación, el justificante incrustado y la solicitud firmada como descarga; **no** se le ofrecen «Enviar a la firma del director» ni «Pedir subsanación al alumno», ni se le muestra el sentido de la revisión, y el único botón es «Salir».

---

## T-037 — El supervisor consulta un expediente de su centro y el supervisor del otro centro no lo ve

**Origen ESC:** ESC-024
**Perfil:** `RESPONSABLE` (logins `supervisor1@mislata.es` y `supervisor1@batoi.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y el supervisor «supervisor1@mislata.es» entra por la bandeja «Expedientes Esperando», que abre los expedientes de su centro CIPFP Mislata con el perfil `RESPONSABLE`.
- **When** abre el expediente desde esa lista y después pulsa «Salir».
- **Then** el sistema se lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión», los datos del alumno y de la matrícula, la fecha y la hora de presentación, el justificante incrustado y la solicitud firmada como descarga, sin «Enviar a la firma del director» ni «Pedir subsanación al alumno» ni el sentido de la revisión, y vuelve a la lista sin cambiar nada.
- **And** el supervisor «supervisor1@batoi.es», del centro CIPFP Batoi, abre la lista de expedientes de su centro y ese expediente **no** aparece en ninguna lista de expedientes que él pueda abrir.

---

## T-038 — El administrador ve los expedientes de los dos centros en solo lectura

**Origen ESC:** ESC-025
**Perfil:** *(administrador, exento de perfil)* (login `admin`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado una solicitud en CIPFP Mislata con el juego de datos válido (Mislata) y el alumno «alumno1@batoi.es» ha presentado otra en CIPFP Batoi con el juego de datos válido (Batoi), y el administrador «admin» —que tiene centro activo asignado por la carga de demo— ha iniciado sesión con la contraseña «admin».
- **When** abre la bandeja «Expedientes Esperando», que abre los expedientes con el perfil `RESPONSABLE`, y entra desde ella en el expediente del alumno «Alumno1 CIPFP Batoi».
- **Then** la lista le muestra los dos expedientes, el de CIPFP Mislata y el de CIPFP Batoi, y el expediente de Batoi se abre en solo lectura con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión».
- **And** ve los datos del alumno y de la matrícula, la fecha y la hora de presentación, el justificante incrustado y la solicitud firmada como descarga; **no** se le ofrecen «Enviar a la firma del director» ni «Pedir subsanación al alumno», ni se le muestra el sentido de la revisión; al pulsar «Salir» vuelve a la lista sin cambiar nada.
- **And** al abrir después «Expedientes Pendientes» esa lista le muestra **solo** el expediente de CIPFP Mislata y **no** el de CIPFP Batoi: esa bandeja filtra por su centro activo también para él, a diferencia de las tres de consulta.

---

## T-039 — La administrativa consulta en solo lectura un expediente devuelto al alumno para subsanar

**Origen ESC:** ESC-045
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** —
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud con el ciclo «Desarrollo de Aplicaciones Multiplataforma», la administrativa «administrativo1@mislata.es» le ha pedido subsanación con el texto del juego de datos de la fase `REVISION` y el expediente ha vuelto a «Solicitud de anulación» / «Datos de la solicitud»; la administrativa entra por la bandeja «Anulaciones de matrícula de mi centro», que abre los expedientes con el perfil `SECRETARIO`; en este estado **no hay form para ese perfil**, así que cae en la pantalla de solo lectura del estado.
- **When** abre de nuevo el expediente desde esa lista y después pulsa «Salir».
- **Then** el sistema lo muestra en solo lectura, con la fase «Solicitud de anulación» y el estado «Datos de la solicitud» en la cabecera, el bloque «Qué hay que subsanar» con ese texto, y los datos del alumno y de la matrícula sin poder cambiarlos.
- **And** el único botón es «Salir»; no se le ofrecen «Siguiente» ni «Borrar el expediente», y al salir vuelve a la lista sin cambiar nada.

---

## T-040 — La administrativa de otro centro no ve el expediente

**Origen ESC:** ESC-021
**Perfil:** `SECRETARIO` (login `administrativo1@batoi.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud en el centro CIPFP Mislata y ha cerrado sesión, y la administrativa «administrativo1@batoi.es», del centro CIPFP Batoi, ha iniciado sesión.
- **When** abre la bandeja «Anulaciones de matrícula de mi centro» y, después, el resto de listas de expedientes que puede abrir («Expedientes Pendientes», «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda de expedientes).
- **Then** el sistema no le muestra ese expediente en la lista de expedientes de su centro.
- **And** tampoco aparece en ninguna otra lista de expedientes que la administrativa de CIPFP Batoi pueda abrir.

---

## T-041 — Un familiar del alumno no puede iniciar el trámite ni ve el expediente

**Origen ESC:** ESC-022
**Perfil:** *(ninguno)* (login `familiar1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y ha cerrado sesión, y el familiar «familiar1@mislata.es» ha iniciado sesión.
- **When** abre la lista de trámites disponibles y después su lista de expedientes.
- **Then** el sistema no le ofrece el trámite «Anulación de matrícula en ciclo formativo» entre los que puede iniciar.
- **And** no le muestra el expediente del alumno «alumno1@mislata.es» en ninguna lista.

---

## T-042 — Otro alumno del mismo centro no ve el expediente

**Origen ESC:** ESC-050
**Perfil:** `CREADOR` (login `alumno2@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud y ha cerrado sesión, y el alumno «alumno2@mislata.es», del mismo centro CIPFP Mislata, ha iniciado sesión.
- **When** abre su lista de expedientes.
- **Then** el sistema no le muestra el expediente del alumno «alumno1@mislata.es».
- **And** en ninguna lista de expedientes que pueda abrir aparecen expedientes que no haya creado él mismo.

---

## T-043 — El exalumno no puede iniciar el trámite pero conserva el acceso a los suyos

**Origen ESC:** ESC-023
**Perfil:** *(ninguno para crear)* (login `exalumno1@mislata.es`)
**Desde:** `[*]`
**Evento:** —
**Hasta:** `[*]`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el exalumno «exalumno1@mislata.es» ha iniciado sesión con la contraseña «demo1234».
- **When** abre la lista de trámites disponibles.
- **Then** el sistema no le ofrece el trámite «Anulación de matrícula en ciclo formativo» entre los que puede iniciar, así que no puede crear ningún expediente nuevo de este trámite.
- **And** su lista de expedientes sigue mostrando los expedientes que él hubiera creado, si los tuviera.

---

## T-044 — El alumno subsana, vuelve a firmar y la solicitud corregida llega de nuevo a revisión

**Origen ESC:** ESC-012
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Evento:** `PRESENTAR` — botón «Firmar y presentar la solicitud»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» presentó su solicitud con el ciclo «Desarrollo de Aplicaciones Multiplataforma», la administrativa «administrativo1@mislata.es» le pidió subsanación con el texto del juego de datos de la fase `REVISION`, el expediente volvió a «Solicitud de anulación» / «Datos de la solicitud» con el bloque «Qué hay que subsanar» visible y, ya allí, el alumno cambió el ciclo a «Desarrollo de Aplicaciones Web» y pulsó «Siguiente», de modo que el expediente está en «Solicitud de anulación» / «Pendiente de firma y presentación» con la solicitud corregida incrustada.
- **When** pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud.
- **Then** el expediente queda en la fase «Revisión de la secretaría» y el estado «Pendiente de revisión».
- **And** el justificante de presentación es el nuevo y ya no se muestra ningún texto de «Qué hay que subsanar».
- **And** la administrativa «administrativo1@mislata.es» abre después el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`) y ve la solicitud corregida con el ciclo «Desarrollo de Aplicaciones Web», el sentido de la revisión sin elegir y ningún texto de subsanación.

---

## T-045 — El alumno desiste tras la petición de subsanación y borra el expediente

**Origen ESC:** ESC-013
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `DELETE` — botón «Borrar el expediente»
**Hasta:** `[*]`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» presentó su solicitud con el ciclo «Desarrollo de Aplicaciones Multiplataforma», la administrativa le pidió subsanación y el expediente está en «Solicitud de anulación» / «Datos de la solicitud»; el alumno lo abre desde su lista de expedientes.
- **When** pulsa «Borrar el expediente» y confirma el aviso «Se va a eliminar el expediente y no podrá recuperarlo».
- **Then** el sistema elimina el expediente.
- **And** el expediente ya no aparece en la lista de expedientes del alumno.

---

## T-046 — Tras la devolución del director, secretaría pide subsanación y no queda rastro de la decisión anterior

**Origen ESC:** ESC-031
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `SUBSANAR` — botón «Pedir subsanación al alumno»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» presentó su solicitud con el ciclo «Desarrollo de Aplicaciones Multiplataforma», la administrativa eligió «Aceptar la anulación» y la envió a la firma, el director «director@mislata.es» la devolvió con el motivo «El ciclo no coincide con la matrícula del alumno; pida al alumno que lo corrija», y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`) y ve el bloque «Devuelto por el director» con ese motivo y el sentido «Aceptar la anulación».
- **When** cambia el sentido a «Pedir subsanación», escribe en «Qué hay que subsanar» el texto del juego de datos de la fase `REVISION` y pulsa «Pedir subsanación al alumno», confirmando el aviso.
- **Then** el expediente vuelve a la fase «Solicitud de anulación» y el estado «Datos de la solicitud», sin dejar constancia de ninguna salida.
- **And** el alumno abre el expediente y ve el bloque «Qué hay que subsanar», y no ve nada de la devolución del director ni de la decisión de secretaría.
- **And** tras corregir el ciclo a «Desarrollo de Aplicaciones Web», presentar de nuevo y firmar, la administrativa ve la solicitud firmada con el ciclo corregido, el sentido de la revisión sin elegir, sin motivo del rechazo, sin texto de subsanación y sin el bloque «Devuelto por el director».

---

## T-047 — Se elige un ciclo de grado Medio tecleando su nombre y los documentos llevan «Medio»

**Origen ESC:** ESC-034
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite y ha rellenado el juego de datos válido (Mislata) salvo el ciclo.
- **When** teclea «Microinform» en el campo «Ciclo formativo» sin abrir la ventana de búsqueda, elige de las propuestas «Sistemas Microinformáticos y Redes» y pulsa «Siguiente».
- **Then** el sistema propone únicamente «Sistemas Microinformáticos y Redes» al teclear, genera la solicitud y el expediente pasa a «Solicitud de anulación» / «Pendiente de firma y presentación».
- **And** en la solicitud incrustada se lee, en el bloque «Expone», «en el Ciclo Formativo de Grado Medio» y «denominado Sistemas Microinformáticos y Redes», y en la línea de lugar y fecha «Mislata» y la fecha de hoy con el día, el mes en letras y el año.
- **And** tras presentar y firmar, la administrativa elige «Aceptar la anulación» y envía a la firma; el director «director@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula pendientes de mi firma» (la que abre los expedientes con el perfil `DIRECTOR`), en «Resolución del centro» / «Pendiente de la firma del director» y en la resolución sin firmar incrustada la matrícula aparece como «Ciclo Formativo de Grado Medio» seguido de «Sistemas Microinformáticos y Redes», con el curso académico «2024/2025» y el centro «CIPFP Mislata».

---

## T-048 — Se elige un curso de especialización y la solicitud lleva «Curso de especialización»

**Origen ESC:** ESC-035
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente nuevo del trámite y ha rellenado el juego de datos válido (Mislata) salvo el ciclo.
- **When** abre la ventana de búsqueda del ciclo, filtra por el grado «Curso de especialización», elige «Inteligencia Artificial y Big Data» y pulsa «Siguiente».
- **Then** la ventana de búsqueda le muestra solo los ciclos de ese grado —«Inteligencia Artificial y Big Data»— y ninguno de grado «Ciclo formativo» (ni «Desarrollo de Aplicaciones Web» ni «Sistemas Microinformáticos y Redes»).
- **And** el sistema genera la solicitud, el expediente pasa a «Solicitud de anulación» / «Pendiente de firma y presentación» y en la solicitud incrustada se lee, en el bloque «Expone», «en el Ciclo Formativo de Grado Curso de especialización» y «denominado Inteligencia Artificial y Big Data».

---

## T-049 — Se solicita de nuevo la anulación del mismo ciclo cuando la anterior ya está cerrada

**Origen ESC:** ESC-036
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» tiene un expediente del trámite con el ciclo «Desarrollo de Aplicaciones Web» ya cerrado en «Resolución del centro» / «Anulación rechazada» (la administrativa lo rechazó con el motivo del juego de datos de la fase `REVISION` y el director firmó la resolución), y ha creado un segundo expediente nuevo del mismo trámite.
- **When** rellena el juego de datos válido (Mislata), con el mismo ciclo «Desarrollo de Aplicaciones Web», y pulsa «Siguiente».
- **Then** el sistema **no** muestra «Ya tiene una solicitud de anulación en curso para este ciclo», genera la solicitud y el segundo expediente pasa a «Solicitud de anulación» / «Pendiente de firma y presentación».
- **And** el primer expediente sigue cerrado, sin cambios.

---

## T-050 — Otro alumno del mismo centro puede solicitar la anulación del mismo ciclo

**Origen ESC:** ESC-037
**Perfil:** `CREADOR` (login `alumno2@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado una solicitud con el ciclo «Desarrollo de Aplicaciones Web» que está en «Revisión de la secretaría» / «Pendiente de revisión», y el alumno «alumno2@mislata.es» ha iniciado sesión y ha creado un expediente nuevo del trámite, en el que ve ya rellenos «Nombre» = «Alumno2», «Apellidos» = «CIPFP Mislata» y «DNI/NIE» = «03532821K».
- **When** rellena el «NIA» con «23456789», la «Dirección» con «C/ Nueva, 3», el «Teléfono» con «623456789», la «Población» con «Mislata», la «Provincia» con «Valencia» y el «Código postal» con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
- **Then** el sistema **no** muestra «Ya tiene una solicitud de anulación en curso para este ciclo» y genera la solicitud.
- **And** el expediente de «alumno2@mislata.es» pasa a «Solicitud de anulación» / «Pendiente de firma y presentación».

---

## T-051 — El alumno retoma en otra sesión un expediente al que había vuelto atrás

**Origen ESC:** ESC-038
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `DATOS_SOLICITUD`
**Evento:** `CONTINUAR` — botón «Siguiente»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» creó un expediente, rellenó el juego de datos válido (Mislata), pulsó «Siguiente», pulsó «Atrás» para volver a «Solicitud de anulación» / «Datos de la solicitud», volvió a su lista de expedientes sin pulsar «Siguiente» ni «Borrar el expediente» y cerró sesión; ahora vuelve a iniciar sesión y abre el expediente desde su lista.
- **When** comprueba que los datos están como los dejó y pulsa «Siguiente» sin cambiar nada.
- **Then** el sistema lo muestra en «Solicitud de anulación» / «Datos de la solicitud», en su pantalla editable, con el NIA «12345678», la dirección «C/ Mayor, 12», el teléfono «612345678», la población «Mislata», la provincia «Valencia», el código postal «46920» y el ciclo «Desarrollo de Aplicaciones Web», y con los botones «Siguiente» y «Borrar el expediente».
- **And** al pulsar «Siguiente» **no** muestra «Ya tiene una solicitud de anulación en curso para este ciclo» (el único expediente en curso para ese ciclo es este mismo), vuelve a generar la solicitud y el expediente pasa a «Pendiente de firma y presentación».

---

## T-052 — En la pantalla de firma no se puede borrar el expediente

**Origen ESC:** ESC-039
**Perfil:** `CREADOR` (login `alumno1@mislata.es`)
**Desde:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Evento:** `VOLVER` — botón «Atrás»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha creado un expediente, ha rellenado el juego de datos válido (Mislata) y ha pulsado «Siguiente», de modo que está en «Solicitud de anulación» / «Pendiente de firma y presentación».
- **When** comprueba los botones que se le ofrecen y pulsa «Atrás».
- **Then** en la pantalla de firma solo se le ofrecían «Atrás» y «Firmar y presentar la solicitud», sin ningún botón «Borrar el expediente», y el expediente vuelve a «Solicitud de anulación» / «Datos de la solicitud».
- **And** allí pulsa «Borrar el expediente», confirma el aviso «Se va a eliminar el expediente y no podrá recuperarlo» y el expediente desaparece de su lista.

---

## T-053 — Tras un rechazo devuelto, secretaría pide subsanación y el motivo del rechazo no reaparece

**Origen ESC:** ESC-046
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `SUBSANAR` — botón «Pedir subsanación al alumno»
**Hasta:** `SOLICITUD` / `DATOS_SOLICITUD`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» presentó su solicitud con el ciclo «Desarrollo de Aplicaciones Multiplataforma», la administrativa eligió «Rechazar la anulación» con el motivo del juego de datos de la fase `REVISION` y la envió a la firma, el director la devolvió con el motivo «El ciclo no coincide con la matrícula del alumno; pida al alumno que lo corrija», y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`) y ve el bloque «Devuelto por el director», el sentido «Rechazar la anulación» y ese motivo del rechazo.
- **When** cambia el sentido a «Pedir subsanación», escribe en «Qué hay que subsanar» el texto del juego de datos de la fase `REVISION` y pulsa «Pedir subsanación al alumno», confirmando el aviso.
- **Then** el expediente vuelve a la fase «Solicitud de anulación» y el estado «Datos de la solicitud», y se han borrado el motivo del rechazo, el motivo de la devolución y la resolución sin firmar.
- **And** tras corregir el alumno el ciclo a «Desarrollo de Aplicaciones Web», presentar de nuevo y firmar, la administrativa ve el sentido de la revisión sin elegir y sin el bloque «Devuelto por el director»; al elegir «Rechazar la anulación» el «Motivo del rechazo» aparece vacío, sin el texto que había escrito en el ciclo de revisión anterior.

---

## T-054 — El director devuelve un rechazo, secretaría lo cambia por la aceptación y el motivo desaparece de la resolución

**Origen ESC:** ESC-048
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `ENVIAR_A_FIRMA` — botón «Enviar a la firma del director»
**Hasta:** `RESOLUCION` / `PENDIENTE_FIRMA_DIRECTOR`
**Tipo:** happy
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» presentó su solicitud, la administrativa eligió «Rechazar la anulación» con el motivo del juego de datos de la fase `REVISION` y la envió a la firma, el director «director@mislata.es» la devolvió con el motivo «El alumno presentó la solicitud dentro del plazo; la anulación procede», y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`) y ve el bloque «Devuelto por el director» con ese motivo, el sentido «Rechazar la anulación» y el motivo del rechazo.
- **When** cambia el sentido a «Aceptar la anulación», comprueba que el motivo del rechazo deja de mostrarse, pulsa «Enviar a la firma del director» y confirma el aviso.
- **Then** el expediente pasa a la fase «Resolución del centro» y el estado «Pendiente de la firma del director», con la resolución regenerada y sin el motivo del rechazo ni el motivo de la devolución.
- **And** el director abre el expediente y ve en «Decisión de secretaría» el sentido «Aceptar la anulación» sin ningún motivo del rechazo, y en la resolución sin firmar incrustada se lee «Se estima la solicitud y la matrícula queda sin efecto a partir de la fecha de presentación» y no aparece «Por el siguiente motivo:».
- **And** al pulsar «Firmar la resolución» y confirmar el aviso, el expediente queda cerrado en «Resolución del centro» / «Anulación aceptada», con la resolución firmada incrustada y sin ningún motivo.

---

## T-055 — Se intenta presentar la solicitud con la contraseña del certificado incorrecta

**Origen ESC:** —
**Perfil:** `CREADOR` (login `alumno2@mislata.es`)
**Desde:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Evento:** `PRESENTAR` — botón «Firmar y presentar la solicitud»
**Hasta:** `SOLICITUD` / `PENDIENTE_FIRMA`
**Tipo:** error
**Manual:** no

- **Given** el alumno «alumno2@mislata.es», que según la precondición de firma tiene un certificado digital custodiado dado de alta **sin** la contraseña guardada, ha creado un expediente, ha rellenado el juego de datos válido de la fase `SOLICITUD` (Mislata) y ha pulsado «Siguiente», de modo que el expediente está en «Solicitud de anulación» / «Pendiente de firma y presentación» y el bloque «Firma de la solicitud» le pide la «Contraseña» de su certificado.
- **When** escribe en «Contraseña» un valor que no es el de su certificado, pulsa «Firmar y presentar la solicitud» y confirma el aviso «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla».
- **Then** el sistema muestra el mensaje «No es posible firmar la solicitud: la contraseña indicada no es correcta».
- **And** el expediente sigue en la fase «Solicitud de anulación» y el estado «Pendiente de firma y presentación», sin solicitud firmada y sin que se haya dejado constancia de ninguna entrada.
- **And** al escribir la contraseña correcta de su certificado y volver a pulsar «Firmar y presentar la solicitud», el expediente pasa a «Revisión de la secretaría» / «Pendiente de revisión» con su justificante de presentación.

---

## T-056 — Las bandejas de secretaría y de firma no listan el expediente a quien no ostenta ese perfil

**Origen ESC:** ESC-020, ESC-029
**Perfil:** `CREADOR` (login `alumno1@mislata.es`) y `RESPONSABLE` (logins `secretario@mislata.es` y `supervisor1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** —
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** solo-lectura
**Manual:** no

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud con el juego de datos válido (Mislata), el expediente está en «Revisión de la secretaría» / «Pendiente de revisión» y el propio alumno, que puede leerlo por ser quien lo creó, ha iniciado sesión.
- **When** abre desde el menú de expedientes la bandeja «Anulaciones de matrícula de mi centro» y después la bandeja «Anulaciones de matrícula pendientes de mi firma».
- **Then** las dos entradas de menú están visibles, pero **ninguna de las dos listas muestra su expediente**: aparecen vacías, así que no puede abrirlo con el perfil `SECRETARIO` ni con el perfil `DIRECTOR`.
- **And** desde «Expedientes Pendientes» sí ve su expediente y lo abre en la pantalla de solo lectura del estado, sin el sentido de la revisión, sin el motivo del rechazo y sin ninguna resolución.
- **And** lo mismo le ocurre al secretario del centro «secretario@mislata.es» y al supervisor «supervisor1@mislata.es», que también pueden leer el expediente: las dos bandejas nuevas les salen vacías, y al entrar por «Expedientes Esperando» ven el expediente en solo lectura, sin la decisión de secretaría y sin que se les ofrezcan «Enviar a la firma del director», «Pedir subsanación al alumno» ni «Firmar la resolución».
- **And** la administrativa «administrativo1@mislata.es» sí ve el expediente en «Anulaciones de matrícula de mi centro» y lo abre en su pantalla editable de revisión; y, una vez enviado a la firma, el director «director@mislata.es» sí lo ve en «Anulaciones de matrícula pendientes de mi firma» y lo abre en la suya.
- **And** al administrador «admin», que no tiene ningún perfil asignado, las dos bandejas nuevas también le salen **vacías**, mientras que en «Expedientes Esperando» sigue viendo los expedientes de los dos centros en solo lectura (T-038).
