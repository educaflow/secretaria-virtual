---
type: specification
template: system
---

# Objetivo

Crear el **sistema** «expedientes», que reúne las pantallas con las que el usuario inicia un expediente («Nuevo expediente») y con las que el administrador consulta los tipos de expediente.
Hasta ahora esas dos pantallas vivían dentro del subsistema de expedientes, que es el tramitador; pasan al sistema nuevo para que el tramitador quede reducido a tramitar.
De cara al usuario, «Nuevo expediente» se comporta **exactamente igual que hoy**: lo único que cambia es que lo que se ve en la pantalla lo decide la propia pantalla a partir de los datos que recibe, y no quien le suministra esos datos.
A la vez, el tramitador deja de depender de esa pantalla: para iniciar un expediente recibe solo los datos mínimos (trámite, centro, perfil y si se presenta en representación) y comprueba él mismo que quien lo inicia puede hacerlo, entre por donde entre la petición.
«Tipos de expediente» pasa a ser una pantalla de solo consulta, visible únicamente para el Administrador.

Depende del subsistema de expedientes (el tramitador, que es quien crea y tramita el expediente), del de seguridad (qué perfiles tiene cada usuario sobre cada trámite en cada centro) y del común (los centros del usuario).

**Modifica:** subsystem/expedientes

# Actores

- **Usuario con perfil de creador**: cualquier usuario, sea cual sea su tipo o su cargo, al que se le ha concedido el perfil de creador sobre un trámite en alguno de sus centros. Presenta sus expedientes telemáticamente.
- **Usuario con perfil de tramitador**: cualquier usuario, sea cual sea su tipo o su cargo, al que se le ha concedido el perfil de tramitador sobre un trámite en alguno de sus centros. Registra en la aplicación las solicitudes que alguien ha entregado en papel en el centro. Tener un cargo (por ejemplo, director) no da este perfil: hay que tenerlo concedido.
- **Usuario sin perfil de inicio**: usuario que ve un trámite pero no tiene sobre él ni el perfil de creador ni el de tramitador en ninguno de sus centros.
- **Administrador**: consulta los tipos de expediente que hay dados de alta en la aplicación.

# Historias de usuario

## HU-001 — Como Usuario con perfil de creador quiero iniciar un expediente de un trámite desde «Nuevo expediente» para presentar mi solicitud telemáticamente

- ESC-001 — Alta con un único centro, sin ninguna pregunta:
  1. El alumno inicia sesión con usuario «alumno1@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. El sistema abre la ventana «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo» y su texto de ayuda en la cabecera.
  4. El sistema muestra el centro ya elegido con «CIPFP Mislata» y sin posibilidad de cambiarlo.
  5. El sistema no muestra el interruptor «Presentado el expediente a partir de un documento en papel» ni la pregunta «¿Para quién es el expediente?».
  6. El alumno pulsa «Crear expediente».
  7. El sistema cierra la ventana y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata» y presentado telemáticamente para el propio alumno.
- ESC-002 — Alta eligiendo entre dos centros:
  1. El alumno inicia sesión con usuario «alumnodoscentros@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. El sistema abre la ventana «Nuevo expediente» con el centro vacío y editable.
  4. El alumno despliega el campo centro.
  5. El sistema ofrece únicamente «CIPFP Batoi» y «CIPFP Mislata», en ese orden.
  6. El alumno elige «CIPFP Batoi» y pulsa «Crear expediente».
  7. El sistema cierra la ventana y abre el expediente recién creado en su primer estado, en el centro «CIPFP Batoi».
- ESC-003 — Intento de alta sin indicar el centro:
  1. El alumno inicia sesión con usuario «alumnodoscentros@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. Sin elegir ningún centro, pulsa «Crear expediente».
  4. El sistema no crea ningún expediente, mantiene la ventana abierta y señala que el centro es obligatorio.
- ESC-004 — Cancelar no crea nada:
  1. El alumno inicia sesión con usuario «alumno1@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. En la ventana «Nuevo expediente» pulsa «Cancelar».
  4. El sistema cierra la ventana sin crear ningún expediente.
  5. El alumno abre el menú Expedientes → Expedientes Pendientes.
  6. El sistema no muestra ningún expediente de «Anulación de matrícula en ciclo formativo».
- ESC-009 — El expediente recién creado aparece en «Expedientes Pendientes» y se puede reabrir:
  1. El alumno inicia sesión con usuario «alumno1@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. En la ventana «Nuevo expediente», con el centro «CIPFP Mislata» ya elegido, pulsa «Crear expediente».
  4. El sistema cierra la ventana y abre el expediente recién creado en su primer estado.
  5. El alumno abre el menú Expedientes → Expedientes Pendientes.
  6. El sistema muestra una única fila del trámite «Anulación de matrícula en ciclo formativo», en la fase «Solicitud de anulación» y en el estado «Datos de la solicitud».
  7. El alumno pulsa esa fila.
  8. El sistema abre el mismo expediente, en el estado «Datos de la solicitud» y en el centro «CIPFP Mislata».
- ESC-010 — Un alumno de otro centro solo puede crear el expediente en su centro:
  1. El alumno inicia sesión con usuario «alumno1@batoi.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. El sistema abre la ventana «Nuevo expediente» con el centro ya elegido con «CIPFP Batoi» y sin posibilidad de cambiarlo.
  4. El sistema no ofrece «CIPFP Mislata» en ningún momento.
  5. El alumno pulsa «Crear expediente».
  6. El sistema cierra la ventana y abre el expediente recién creado en su primer estado, en el centro «CIPFP Batoi».
- ESC-011 — Usuario con dos tipos de usuario en el mismo centro: el centro aparece una sola vez y no se pregunta nada:
  1. El usuario inicia sesión con usuario «alumnofamiliar@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. El sistema abre la ventana «Nuevo expediente» con el centro ya elegido con «CIPFP Mislata» y sin posibilidad de cambiarlo, aunque el usuario tiene el perfil de creador en ese centro por dos vías (como alumno y como familiar).
  4. El sistema no muestra el interruptor «Presentado el expediente a partir de un documento en papel», porque el usuario solo tiene el perfil de creador.
  5. El sistema no muestra la pregunta «¿Para quién es el expediente?», porque el trámite no admite presentar en representación.
  6. El usuario pulsa «Crear expediente».
  7. El sistema cierra la ventana y abre el expediente recién creado en su primer estado, en el centro «CIPFP Mislata» y presentado telemáticamente para el propio usuario.
- ESC-012 — Cambiar de centro antes de crear: el expediente se crea en el último centro elegido:
  1. El alumno inicia sesión con usuario «alumnodoscentros@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. En la ventana «Nuevo expediente» elige el centro «CIPFP Batoi».
  4. El sistema sigue sin mostrar el interruptor «Presentado el expediente a partir de un documento en papel» ni la pregunta «¿Para quién es el expediente?».
  5. El alumno cambia el centro a «CIPFP Mislata».
  6. El sistema sigue sin mostrar el interruptor ni la pregunta.
  7. El alumno pulsa «Crear expediente».
  8. El sistema cierra la ventana y abre el expediente recién creado en su primer estado, en el centro «CIPFP Mislata» y presentado telemáticamente para el propio alumno.
- ESC-013 — Alta de otro trámite: la ventana muestra el nombre y la ayuda del trámite elegido:
  1. El profesor inicia sesión con usuario «vicedirector@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Justificación de falta del profesorado».
  3. El sistema abre la ventana «Nuevo expediente» con el nombre del trámite «Justificación de falta del profesorado» en la cabecera y, debajo, su texto de ayuda.
  4. El sistema muestra el centro ya elegido con «CIPFP Mislata» y sin posibilidad de cambiarlo.
  5. El sistema no muestra el interruptor «Presentado el expediente a partir de un documento en papel» ni la pregunta «¿Para quién es el expediente?».
  6. El profesor pulsa «Crear expediente».
  7. El sistema cierra la ventana y abre el expediente recién creado de «Justificación de falta del profesorado» en su primer estado, en el centro «CIPFP Mislata» y presentado telemáticamente para el propio profesor.

## HU-002 — Como Usuario con perfil de tramitador quiero registrar desde «Nuevo expediente» una solicitud entregada en papel para que quede tramitada en la aplicación

- ESC-005 — Alta como tramitador, deducida sin preguntar:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. El sistema abre la ventana «Nuevo expediente» con el centro ya elegido con «CIPFP Mislata» y sin posibilidad de cambiarlo.
  4. El sistema no muestra el interruptor «Presentado el expediente a partir de un documento en papel», porque el supervisor solo tiene el perfil de tramitador, ni la pregunta de para quién es, porque el trámite no admite presentar en representación.
  5. El supervisor pulsa «Crear expediente».
  6. El sistema cierra la ventana y abre el expediente recién creado en su primer estado, en el centro «CIPFP Mislata», marcado como presentado en papel.

## HU-003 — Como Usuario sin perfil de inicio quiero que el sistema me avise de que no puedo iniciar un trámite para no empezar una solicitud que no podré presentar

- ESC-006 — Usuario que ve el trámite pero no puede iniciarlo:
  1. El director inicia sesión con usuario «director@mislata.es» y contraseña «demo1234».
  2. Abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
  3. El sistema muestra el error «No puede crear expedientes de este trámite en ninguno de sus centros» y no permite crear el expediente.

## HU-004 — Como Administrador quiero consultar los tipos de expediente dados de alta para saber qué versiones de cada trámite hay en la aplicación

- ESC-007 — Consulta en solo lectura:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Expedientes → Tipos Expedientes.
  3. El sistema muestra el listado de tipos de expediente con las columnas código, nombre y trámite, sin botón «Nuevo».
  4. El administrador pulsa la fila cuyo código es «AnulacionMatriculaCicloFormativoV1».
  5. El sistema abre el formulario con el código, el nombre y el trámite en solo lectura, sin botones de guardar ni de borrar.
- ESC-008 — Quien no es administrador no ve la pantalla:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Despliega el menú Expedientes.
  3. El sistema no muestra la opción «Tipos Expedientes».

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-NuevoExpediente.md](./entity-NuevoExpediente.md) | NuevoExpediente | Lo que el usuario contesta en la ventana «Nuevo expediente» antes de empezar, junto con los datos que la ventana necesita para saber qué preguntarle. No se guarda. |
| [entity-ContextoTramitacion.md](./entity-ContextoTramitacion.md) | ContextoTramitacion | Los datos mínimos con los que el tramitador inicia un expediente: trámite, centro, perfil y si se presenta en representación. No se guarda. |
| [entity-TipoExpediente.md](./entity-TipoExpediente.md) | TipoExpediente | Cada versión de un trámite que hay dada de alta en la aplicación. |

Al pulsar «Crear expediente», lo contestado en un NuevoExpediente se convierte en un ContextoTramitacion, que es lo único que recibe el tramitador: el tramitador no conoce la ventana ni sus datos.
Un ContextoTramitacion puede construirse también sin pasar por la ventana.
NuevoExpediente y ContextoTramitacion referencian un trámite y un centro, que son entidades ya existentes fuera de esta especificación.
TipoExpediente referencia el trámite del que es versión.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-nuevo-expediente.md](./screen-nuevo-expediente.md) | Nuevo expediente | La ventana previa al alta de un expediente: pregunta al usuario en qué centro lo crea, cómo lo presenta y para quién es. |
| [screen-tipos-de-expediente.md](./screen-tipos-de-expediente.md) | Tipos de expediente | Consulta, en solo lectura y solo para el Administrador, de los tipos de expediente dados de alta. |

# Seguridad

- **Cualquier usuario con perfil de creador o de tramitador sobre un trámite** (sea cual sea su tipo de usuario o su cargo): puede abrir «Nuevo expediente» para ese trámite e iniciar el expediente. Alcance: solo en aquellos de sus centros en los que tiene concedido ese perfil sobre ese trámite, y solo de la forma que su perfil permite (el creador, telemáticamente; el tramitador, registrando una solicitud en papel). No ve ni puede elegir centros ajenos.
- **Administrador**: puede ver el listado y el detalle de todos los tipos de expediente, que no pertenecen a ningún centro. No puede crear, modificar ni borrar ninguno.

# Recursos y datos iniciales

*(no aplica)* — Los tipos de expediente y los trámites los registra ya la aplicación al arrancar; esta iniciativa no añade recursos ni datos.

# Fuera de alcance

- Cambiar el nombre del subsistema de expedientes por uno que refleje que es el tramitador: se hará en una iniciativa posterior.
- Reorganizar o renombrar el resto de piezas del tramitador (las que atienden a las pantallas de cada tipo de expediente una vez el expediente existe, y la firma de documentos): se quedan como están.
- Las pantallas para elegir trámite y para buscar y consultar expedientes (Trámites, Expedientes Pendientes, Esperando, Cerrados, búsqueda): todavía no está decidido cómo serán; pasarán al sistema «expedientes» en otra iniciativa. De ellas solo se toca lo imprescindible para que «Trámites» siga abriendo la ventana «Nuevo expediente».
- Crear expedientes sin pasar por la ventana (con valores predefinidos): el tramitador queda preparado para ello, pero no se construye ningún caso.
- Cualquier mantenimiento (alta, modificación o borrado) de trámites, tipos de trámite o tipos de expediente.
- Proteger el expediente frente a modificaciones directas de sus datos por fuera de la tramitación.
- Añadir datos de demostración para los casos que hoy no se pueden reproducir con ellos (usuario con los dos perfiles a la vez; trámite que admite presentar en representación).
