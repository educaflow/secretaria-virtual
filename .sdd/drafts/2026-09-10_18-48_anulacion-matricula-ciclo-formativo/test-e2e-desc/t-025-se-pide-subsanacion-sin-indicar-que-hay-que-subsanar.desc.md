---
type: test-e2e
id: T-025
---

# T-025 — Se pide subsanación sin indicar qué hay que subsanar

**Origen ESC:** ESC-011
**Perfil:** `SECRETARIO` (login `administrativo1@mislata.es`)
**Desde:** `REVISION` / `PENDIENTE_REVISION`
**Evento:** `SUBSANAR` — botón «Pedir subsanación al alumno»
**Hasta:** `REVISION` / `PENDIENTE_REVISION`
**Tipo:** error
**Manual:** no

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Vía |
|---|---|---|---|---|---|
| `alumno1@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | tramiteCode |
| `alumno2@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | tramiteCode |
| `alumno3@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | tramiteCode |
| `alumno5@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | tramiteCode |
| `alumno6@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR` | tramiteCode |
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

### Datos de demo

Estado previo del que parten **todos** los tests: la aplicación arrancada con los datos de demostración (centros, usuarios y perfiles), el trámite «Anulación de matrícula en ciclo formativo» publicado en el árbol de trámites del centro, el catálogo de ciclos formativos cargado —incluidos el curso de especialización «Inteligencia Artificial y Big Data» y los **ocho ciclos** que el entorno de pruebas da de alta, enumerados en «Aislamiento entre tests»— y el centro con la firma de su director disponible para que el sistema pueda firmar la resolución.

**CRITICAL — la premisa NO es «la base de datos está virgen».** Los 56 tests se ejecutan uno tras otro contra **una única** base de datos que nadie resetea entre ellos, y 54 de ellos no terminan borrando ni cerrando el expediente que crean: cada pasada deja expedientes **abiertos**. La premisa que sí se cumple pasada tras pasada, y en la que se apoyan todos los tests, es esta otra: **cada test trabaja sobre su propia tripleta (alumno, ciclo, curso académico), que ningún otro test usa**, así que el residuo de los demás —expedientes abiertos de otras tripletas— le es indiferente. Ningún test puede presuponer más estado que la carga de demo **más ese residuo ajeno**, y ninguno puede presuponer que la tripleta de otro esté libre. El reparto está en «Aislamiento entre tests», más abajo, y es de cumplimiento obligatorio.

**Localidad de los centros (aplica a los tests que leen el lugar impreso en los documentos, T-047 entre ellos): no hace falta ninguna precondición.** La línea de lugar y fecha de la solicitud y de la resolución lleva la **localidad del centro**, y los dos centros de demostración ya la tienen: CIPFP Mislata está en «Mislata» y CIPFP Batoi en «Alcoy/Alcoi». Se comprueba abriendo la ficha de cada centro en el mantenimiento de centros. El entorno de pruebas **MUST NOT** vaciar ni recargar de cero la base de datos por este motivo.

**Precondición del administrador (aplica a todo test en el que entra `admin`) — la pone la CARGA DE DEMO, no el entorno.** El usuario administrador necesita un centro activo, porque sin él la aplicación no le deja abrir ningún expediente y avisa de que su centro activo está vacío; ese centro se lo da **la propia carga de demo** (`src/main/resources/data-demo/input/usuarios-demo.xml`, Paso 15 del diseño), así que **NO es una precondición manual del entorno**, a diferencia de los ocho ciclos y de los certificados custodiados: quien prepare el entorno no tiene que hacer nada por este motivo. En estos tests su centro es **CIPFP Mislata**. Ese centro **no** cambia lo que ve en «Expedientes Esperando», «Expedientes Cerrados» ni en «Prueba Búsqueda»: en esas tres listas el administrador ve los expedientes de **todos** los centros. **Sí** cambia lo que ve en «Expedientes Pendientes», que a propósito le trata como a cualquier otro usuario: ahí solo ve los de CIPFP Mislata. **MUST NOT** asignarse ese centro a mano (ni por la pantalla de mantenimiento de usuarios ni tocando la base de datos): hacerlo produciría un **falso verde**: si alguien borrara por accidente la línea de `usuarios-demo.xml` —el entregable versionado del Paso 15—, T-038 y T-056 seguirían pasando gracias a la asignación manual y la pérdida quedaría enmascarada. Si el administrador aparece sin centro activo, lo que falta es esa línea versionada y se arregla ahí.

**Precondición de firma (aplica a todos los tests que presentan la solicitud).** Quién firma la solicitud y cómo depende de la situación de firma del alumno, que la aplicación deduce de su documento de identidad: si tiene un certificado custodiado dado de alta, la firma la hace la propia aplicación al pulsar el botón (paso automatizable); si no lo tiene, el botón abre la aplicación de firma del ciudadano en su equipo (paso **no** automatizable). El entorno de pruebas **MUST** dejar los certificados digitales custodiados (pantalla «Administración → Certificados digitales») exactamente así, porque hay tests que dependen de **cada** una de las tres situaciones:

| Alumno | Certificado custodiado | Para qué |
|---|---|---|
| `alumno1@mislata.es` | **sí**, con su contraseña guardada | firma en servidor sin pedir nada: es el alumno del camino feliz |
| `alumno5@mislata.es` | **sí**, con su contraseña guardada | segundo firmante automático: el reparto de tripletas necesita más de un alumno que pueda presentar sin intervención (ver «Aislamiento entre tests») |
| `alumno6@mislata.es` | **sí**, con su contraseña guardada | tercer firmante automático, por el mismo motivo |
| `alumno1@batoi.es` | **sí**, con su contraseña guardada | el camino feliz del otro centro |
| `alumno2@mislata.es` | **sí**, **sin** la contraseña guardada | la pantalla le pide la contraseña del certificado: es el único modo de probar una clave incorrecta (T-055) |
| `alumno3@mislata.es` | **no** | firma en el equipo del alumno con la aplicación de firma del ciudadano (T-019 y T-020, `Manual: sí`); el reparto le da además los tests que **no** llegan a presentar |
| `alumno4@mislata.es` | **no** | no inicia sesión en ningún test; su documento de identidad «62584352H» es el del certificado ajeno con el que se firma en T-020 |

Los dos únicos tests que ejercitan a propósito la rama de firma en el equipo del alumno (T-019 y T-020) están marcados `Manual: sí` y **MUST** ejecutarse con `alumno3@mislata.es`, que esta tabla deja **sin** certificado custodiado. Es configuración del entorno, no un fichero del proyecto: los certificados de `alumno5@mislata.es` y `alumno6@mislata.es` son **nuevos** y hay que darlos de alta con el documento de identidad de cada uno («69807058B» y «80364400P») antes de lanzar la suite.

#### Juego de datos válido — fase `SOLICITUD` (alumno de CIPFP Mislata)

| campo | valor |
|---|---|
| «NIA» | `12345678` |
| «Dirección» | `C/ Mayor, 12` |
| «Teléfono» | `612345678` |
| «Población» | `Mislata` |
| «Provincia» | `Valencia` |
| «Código postal» | `46920` |
| «Ciclo formativo» | `Desarrollo de Aplicaciones Web` **solo en T-001, T-002 y T-003**; en los demás tests, el ciclo que les reserva la tabla de «Aislamiento entre tests» |

Los seis primeros valores son comunes a **todos** los alumnos de CIPFP Mislata (ninguna regla exige que el NIA sea único). El séptimo, el ciclo, es la mitad de la tripleta que aísla cada test, así que **MUST** tomarse del reparto y nunca de esta tabla: cada test lo nombra explícitamente en su `Given` o en su `When`.

#### Juego de datos válido — fase `SOLICITUD` (alumno de CIPFP Batoi)

| campo | valor |
|---|---|
| «NIA» | `87654321` |
| «Dirección» | `C/ Sant Nicolau, 5` |
| «Teléfono» | `698765432` |
| «Población» | `Alcoi` |
| «Provincia» | `Alicante` |
| «Código postal» | `03800` |
| «Ciclo formativo» | `Sistemas Microinformáticos y Redes` |

#### Juego de datos válido — fase `REVISION`

| campo | valor |
|---|---|
| «Sentido de la revisión» | `Aceptar la anulación` (o `Rechazar la anulación` / `Pedir subsanación` según el test) |
| «Motivo del rechazo» | `La solicitud se presenta fuera del plazo establecido para la anulación de matrícula` |
| «Qué hay que subsanar» | `El ciclo formativo indicado no coincide con su matrícula; indique el ciclo en el que está matriculado` (no nombra ningún ciclo, para que valga con el que el reparto asigne a cada test) |

#### Juego de datos válido — fase `RESOLUCION`

| campo | valor |
|---|---|
| «Motivo de la devolución» | `El alumno ya ha superado el plazo de anulación; la solicitud debe rechazarse` |

### Aislamiento entre tests — reparto de la tripleta (alumno, ciclo, curso académico)

**Por qué hace falta.** El sistema no permite a un alumno tener **dos solicitudes de anulación en curso para el mismo ciclo y el mismo curso académico**: al pulsar «Siguiente» avisa con «Ya tiene una solicitud de anulación en curso para este ciclo». El curso académico lo pone la aplicación a partir del centro y es el mismo para todos, así que la tripleta se reduce en la práctica a la pareja **(alumno, ciclo)**. Como los tests comparten una única base de datos que nadie vacía entre ellos y casi ninguno borra ni cierra lo que crea, dos tests que usen la misma pareja se bloquean entre sí: el segundo en ejecutarse no puede pasar de «Datos de la solicitud».

**Estrategia elegida: repartir la pareja.** Cada test recibe su propia pareja (alumno, ciclo), que ningún otro usa. Es la opción que **menos complejidad añade**: no cambia ni un paso de ningún test —solo qué alumno inicia sesión y qué ciclo elige— y deja la suite entera reproducible sin tocar la base de datos. Las otras dos vías se descartaron: exigir a cada test una **limpieza final** obliga, para los que acaban en revisión o en la firma del director, a una coreografía de tres actores (el director devuelve, la administrativa pide subsanación, el alumno borra) que triplica el test y puede fallar **después** de que sus comprobaciones hayan pasado; y **declarar el estado heredado** convierte 56 tests autosuficientes en una cadena en la que el orden de ejecución pasa a formar parte del contrato, que es justo lo que ningún test puede permitirse aquí.

**Qué gasta una pareja y qué no.**

- La gasta un expediente que queda **abierto** con ese ciclo: da igual en qué fase esté (`SOLICITUD`, `REVISION` o `RESOLUCION`).
- **No** la gasta un expediente **cerrado** (`RESOLUCION`/`ACEPTADA` o `RESOLUCION`/`RECHAZADA`): el aviso solo mira los que siguen en curso. Por eso los tests que terminan con el expediente cerrado o borrado pueden repetirse indefinidamente sobre su propia pareja.
- **No** la gasta un expediente **sin ciclo** (T-001, T-003 y T-014 lo dejan vacío): sin ciclo no hay duplicado posible.

**Saneamiento previo (solo si una pasada anterior dejó residuo).** Como la pareja es exclusiva de un test, cualquier expediente en curso que la ocupe solo puede venir de una **ejecución anterior de ese mismo test** (por ejemplo, una que falló a medias). Antes de empezar, el test **MUST** dejarlo sin efecto por la propia aplicación: el alumno lo abre desde su lista de expedientes, pulsa «Atrás» si está en la pantalla de firma y «Borrar el expediente»; si ya hubiera pasado a revisión, la administrativa le pide subsanación —lo que lo devuelve a «Datos de la solicitud»— y el alumno lo borra entonces. **MUST NOT** tocarse la base de datos a mano para esto.

**Lo que hay que preparar en el entorno de pruebas para que el reparto quepa.** Las dos cosas son **configuración del entorno**, se hacen **por la propia aplicación** y **no** son ficheros de este proyecto:

- **Ocho ciclos formativos más**, dados de alta en el mantenimiento «Sistema educativo → Ciclos» antes de lanzar la suite: «Administración y Finanzas» (grado Ciclo formativo, nivel Superior), «Gestión Administrativa» (Medio), «Educación Infantil» (Superior), «Integración Social» (Superior), «Cocina y Gastronomía» (Medio), «Electromecánica de Vehículos Automóviles» (Medio), «Sistemas Electrotécnicos y Automatizados» (Superior) y «Comercio Internacional» (Superior). Ninguno es de la familia profesional «Informática y Comunicaciones» ni un curso de especialización, para no alterar lo que ven T-002, T-047 y T-048. Con los ocho que la demostración ya trae —los siete de siempre más «Inteligencia Artificial y Big Data»—, el catálogo ofrece **16** ciclos.
- **Dos certificados custodiados más**, los de `alumno5@mislata.es` y `alumno6@mislata.es` (tabla de la precondición de firma). Los dos alumnos ya existen en los datos de demostración, que traen seis por centro.

**Reparto.** «Deja en curso» dice qué pareja queda ocupada al terminar el test; una pareja marcada `—` queda libre (el expediente acaba cerrado, borrado o sin ciclo).

| Test | Alumno | Ciclo(s) que usa | Deja en curso |
|---|---|---|---|
| T-001 | `alumno1@mislata.es` | *(ninguno: no llega a elegir ciclo)* | — |
| T-002 | `alumno1@mislata.es` | Desarrollo de Aplicaciones Web | sí |
| T-003 | `alumno1@mislata.es` | *(ninguno: borra el expediente)* | — |
| T-004 | `alumno3@mislata.es` | Desarrollo de Aplicaciones Multiplataforma → Administración de Sistemas Informáticos en Red | sí (el segundo) |
| T-005 | `alumno1@mislata.es` | Gestión de Alojamientos Turísticos | sí |
| T-006 | `alumno1@mislata.es` | Guía, Información y Asistencia Turística | sí |
| T-007 | `alumno1@mislata.es` | Agencia de Viajes y Gestión de Eventos | sí |
| T-008 | `alumno1@mislata.es` | Administración y Finanzas | sí |
| T-009 | `alumno1@mislata.es` | Educación Infantil | — (se cierra) |
| T-010 | `alumno1@mislata.es` | Integración Social | — (se cierra) |
| T-011 | `alumno1@mislata.es` | Cocina y Gastronomía | sí |
| T-012 | `alumno3@mislata.es` | Desarrollo de Aplicaciones Web | sí |
| T-013 | `alumno3@mislata.es` | Gestión de Alojamientos Turísticos | sí |
| T-014 | `alumno3@mislata.es` | *(ninguno: el test consiste en no elegirlo)* | — |
| T-015 | `alumno3@mislata.es` | Guía, Información y Asistencia Turística | sí |
| T-016 | `alumno3@mislata.es` | Agencia de Viajes y Gestión de Eventos | sí |
| T-017 | `alumno3@mislata.es` | Administración y Finanzas | sí |
| T-018 | `alumno1@mislata.es` | Electromecánica de Vehículos Automóviles (la solicitud ya presentada) + Sistemas Electrotécnicos y Automatizados (el segundo expediente) | sí (las dos) |
| T-019 | `alumno3@mislata.es` | Educación Infantil | sí |
| T-020 | `alumno3@mislata.es` | Integración Social | sí |
| T-021 | `alumno1@mislata.es` | Comercio Internacional | sí |
| T-022 | `alumno1@mislata.es` | Gestión Administrativa | sí |
| T-023 | `alumno1@mislata.es` | Sistemas Microinformáticos y Redes | sí |
| T-024 | `alumno1@mislata.es` | Inteligencia Artificial y Big Data | sí |
| T-025 | `alumno1@mislata.es` | Desarrollo de Aplicaciones Multiplataforma | sí |
| T-026 | `alumno1@mislata.es` | Administración de Sistemas Informáticos en Red | sí |
| T-027 | `alumno5@mislata.es` | Desarrollo de Aplicaciones Web | sí |
| T-028 | `alumno5@mislata.es` | Desarrollo de Aplicaciones Multiplataforma | sí |
| T-029 | `alumno5@mislata.es` | Administración de Sistemas Informáticos en Red | sí |
| T-030 | `alumno5@mislata.es` | Sistemas Microinformáticos y Redes | sí |
| T-031 | `alumno5@mislata.es` | Gestión de Alojamientos Turísticos | sí |
| T-032 | `alumno5@mislata.es` | Guía, Información y Asistencia Turística | — (se cierra) |
| T-033 | `alumno5@mislata.es` | Agencia de Viajes y Gestión de Eventos | — (se cierra) |
| T-034 | `alumno5@mislata.es` | Inteligencia Artificial y Big Data | sí |
| T-035 | `alumno3@mislata.es` | Cocina y Gastronomía | sí |
| T-036 | `alumno5@mislata.es` | Administración y Finanzas | sí |
| T-037 | `alumno5@mislata.es` | Gestión Administrativa | sí |
| T-038 | `alumno5@mislata.es` (Mislata) y `alumno1@batoi.es` (Batoi) | Educación Infantil (Mislata) + Sistemas Microinformáticos y Redes (Batoi) | sí (las dos) |
| T-039 | `alumno5@mislata.es` | Integración Social | sí |
| T-040 | `alumno5@mislata.es` | Cocina y Gastronomía | sí |
| T-041 | `alumno5@mislata.es` | Electromecánica de Vehículos Automóviles | sí |
| T-042 | `alumno5@mislata.es` (el que presenta; `alumno2@mislata.es` solo mira) | Sistemas Electrotécnicos y Automatizados | sí |
| T-043 | `exalumno1@mislata.es` | *(ninguno: no puede crear expedientes)* | — |
| T-044 | `alumno6@mislata.es` | Desarrollo de Aplicaciones Multiplataforma → Desarrollo de Aplicaciones Web | sí (el segundo) |
| T-045 | `alumno5@mislata.es` | Comercio Internacional | — (se borra) |
| T-046 | `alumno6@mislata.es` | Administración de Sistemas Informáticos en Red → Gestión de Alojamientos Turísticos | sí (el segundo) |
| T-047 | `alumno6@mislata.es` | Sistemas Microinformáticos y Redes *(de nivel Medio y único nombre con «Microinform»: lo exige el test)* | sí |
| T-048 | `alumno3@mislata.es` | Inteligencia Artificial y Big Data *(el único curso de especialización: lo exige el test)* | sí |
| T-049 | `alumno6@mislata.es` | Guía, Información y Asistencia Turística *(el mismo ciclo dos veces: el primer expediente acaba cerrado)* | sí (el segundo) |
| T-050 | `alumno6@mislata.es` y `alumno2@mislata.es` | Agencia de Viajes y Gestión de Eventos *(el mismo ciclo para los dos alumnos: es lo que el test comprueba)* | sí (las dos) |
| T-051 | `alumno3@mislata.es` | Sistemas Electrotécnicos y Automatizados | sí |
| T-052 | `alumno3@mislata.es` | Comercio Internacional | — (se borra) |
| T-053 | `alumno6@mislata.es` | Inteligencia Artificial y Big Data → Administración y Finanzas | sí (el segundo) |
| T-054 | `alumno6@mislata.es` | Gestión Administrativa | — (se cierra) |
| T-055 | `alumno2@mislata.es` | Desarrollo de Aplicaciones Web | sí |
| T-056 | `alumno6@mislata.es` | Educación Infantil | sí |

La columna «Deja en curso» es **conservadora**: en los tests de validación fallida el rechazo no llega a guardar el ciclo, así que en rigor su pareja queda libre, pero la tabla se la reserva igual —la ocupan mientras el test se ejecuta y reservarla no cuesta nada—. Ninguna pareja (alumno, ciclo) se repite en dos tests, salvo donde el propio test exige que se repita y lo hace con **alumnos distintos** (T-050). Los tres alumnos que firman sin intervención —`alumno1`, `alumno5` y `alumno6`— llevan los tests que **presentan** la solicitud; `alumno3`, que no tiene certificado custodiado, lleva los que se quedan en la fase `SOLICITUD` sin presentar, además de los dos manuales (T-019 y T-020); y `alumno2`, cuyo certificado no guarda la contraseña, lleva T-055 y hace de segundo alumno en T-050.

**Nota sobre T-001, T-002 y T-003 (progreso ya ejecutado).** Los tres conservan su numeración, su alumno, su ciclo y todos sus pasos: el reparto resuelve el conflicto moviendo a los tests posteriores, no a ellos. Lo único que cambia a su alrededor es esta cabecera común —dos alumnos más en la tabla de actores, dos certificados más en la precondición de firma, esta sección y el matiz del ciclo en el juego de datos válido—, que no altera lo que hacen ni lo que comprueban.

## Pasos

- **Given** el alumno «alumno1@mislata.es» ha presentado su solicitud con el ciclo que le reserva el reparto, «Desarrollo de Aplicaciones Multiplataforma», y la administrativa «administrativo1@mislata.es» abre el expediente desde la bandeja «Anulaciones de matrícula de mi centro» (la que abre los expedientes con el perfil `SECRETARIO`), en «Revisión de la secretaría» / «Pendiente de revisión».
- **When** elige el sentido de la revisión «Pedir subsanación», pulsa «Pedir subsanación al alumno» sin escribir qué hay que subsanar y confirma el aviso «Va a devolver la solicitud al alumno para que la corrija».
- **Then** el sistema muestra el mensaje «Debe indicar al alumno qué tiene que subsanar».
- **And** el expediente sigue en la fase «Revisión de la secretaría» y el estado «Pendiente de revisión».
