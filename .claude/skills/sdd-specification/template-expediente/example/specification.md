---
type: specification
template: expediente
---

# Objetivo

Permitir que un alumno pida prestado un equipo informático del centro, que la jefatura de estudios valore la petición y que, si se concede, quede constancia de la entrega del equipo.

# El trámite

- **Nombre visible:** Préstamo de equipo informático
- **A qué colectivo va dirigido:** alumnado — es la categoría bajo la que el alumno lo encuentra en la lista de trámites.
- **Quién puede iniciarlo:** cualquier alumno del centro.
- **Para qué sirve:** el centro presta portátiles y tabletas al alumnado que no dispone de equipo propio para trabajar en casa. Este trámite recoge la petición, la somete a la valoración de la jefatura de estudios y, cuando se concede, deja constancia firmada de qué equipo se entregó y en qué estado.
- **Texto de ayuda que ve el usuario antes de empezar:**

  > Con este trámite puedes pedir prestado un portátil o una tableta del centro para el curso actual. Antes de empezar, ten preparado un documento que acredite tu situación (por ejemplo, el informe de tu tutor o el justificante de la avería de tu equipo). Al terminar de rellenar la petición tendrás que firmarla con tu certificado digital desde tu propio ordenador, así que necesitarás tenerlo instalado. Una vez presentada, la jefatura de estudios la valorará y recibirás la respuesta en este mismo expediente. Si te la conceden, tendrás que pasar por conserjería a recoger el equipo y firmar el acta de entrega.

- **Versión:** la primera versión del trámite.

# Actores y perfiles

| Perfil | Qué papel juega en este trámite | Quién lo ostenta |
|---|---|---|
| CREADOR | Pide el préstamo: rellena los datos, firma la petición y la presenta. | El tipo de usuario Alumno. |
| RESPONSABLE | Valora la petición: la concede, la deniega o pide que se corrija. | El cargo Jefes de estudio. |
| SECRETARIO | Entrega el equipo y deja constancia de qué se entregó y en qué estado. | El cargo Administrativas. |

El perfil SECRETARIO de este trámite lo ostenta el cargo Administrativas, no el cargo Secretario del centro: son cosas distintas.

Además de los perfiles anteriores, cualquier otro usuario con acceso al expediente (el propio alumno cuando no tiene el turno, o el equipo directivo) lo ve en solo consulta, con un único botón «Salir».

# Historias de usuario

## HU-001 — Como Alumno quiero pedir prestado un equipo informático para poder trabajar en casa

- ESC-001 — Petición completa y presentada:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, consulta la ayuda de «Préstamo de equipo informático» y pulsa crear un expediente nuevo.
  3. El sistema abre el expediente en el estado SOLICITUD / DATOS_PETICION y muestra el curso académico en vigor y sus datos personales ya rellenos.
  4. Elige el equipo solicitado «Portátil», el motivo «No dispongo de equipo en casa», la fecha prevista de devolución «30/06/2026» y adjunta el fichero «informe-tutor.pdf».
  5. Pulsa «Siguiente».
  6. El sistema genera el documento de solicitud, lo muestra incrustado en la pantalla y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  7. Pulsa «Firmar y presentar» y confirma el aviso «Una vez presentada la petición no podrá modificarla».
  8. Firma el documento en su propio equipo con su certificado digital.
  9. El sistema deja constancia de la entrada de la solicitud, guarda el justificante de presentación y el expediente queda en VALORACION / PENDIENTE_VALORACION.

- ESC-002 — Se intenta continuar sin adjuntar el documento acreditativo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Préstamo de equipo informático» y crea un expediente nuevo.
  3. Elige el equipo solicitado «Tableta», el motivo «Avería de mi equipo» y la fecha prevista de devolución «30/06/2026», y no adjunta ningún fichero.
  4. Pulsa «Siguiente».
  5. El sistema muestra «Debe adjuntar el documento que acredita su situación» y el expediente sigue en SOLICITUD / DATOS_PETICION.

- ESC-003 — Se elige el motivo «Otros» sin explicarlo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Préstamo de equipo informático» y crea un expediente nuevo.
  3. Elige el equipo solicitado «Portátil» y el motivo «Otros», y deja vacía la explicación.
  4. Rellena la fecha prevista de devolución «30/06/2026» y adjunta el fichero «informe-tutor.pdf».
  5. Pulsa «Siguiente».
  6. El sistema muestra «Debe explicar el motivo cuando elige «Otros»» y el expediente sigue en SOLICITUD / DATOS_PETICION.

- ESC-004 — Se corrige la petición volviendo atrás antes de presentarla:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Préstamo de equipo informático» y crea un expediente nuevo.
  3. Elige el equipo solicitado «Portátil», el motivo «No dispongo de equipo en casa», la fecha prevista de devolución «30/06/2026» y adjunta el fichero «informe-tutor.pdf».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  5. Pulsa «Atrás».
  6. El sistema devuelve el expediente a SOLICITUD / DATOS_PETICION con todos los datos que había introducido.
  7. Cambia el equipo solicitado a «Tableta» y pulsa «Siguiente».
  8. El sistema vuelve a generar el documento de solicitud, ahora con «Tableta», y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.

- ESC-005 — Se abandona la petición y se borra el expediente:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Préstamo de equipo informático» y crea un expediente nuevo.
  3. Pulsa «Borrar el expediente» y confirma el aviso «Se va a eliminar el expediente y no podrá recuperarlo».
  4. El sistema elimina el expediente y vuelve a la lista de trámites; el expediente ya no aparece en su lista de expedientes.

## HU-002 — Como Jefe de estudios quiero valorar las peticiones de préstamo para repartir los equipos disponibles

- ESC-006 — Se concede el préstamo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Préstamo de equipo informático», elige el equipo «Portátil», el motivo «No dispongo de equipo en casa» y la fecha prevista de devolución «30/06/2026», adjunta «informe-tutor.pdf» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar», confirma el aviso y firma el documento con su certificado digital.
  3. El alumno cierra sesión.
  4. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en VALORACION / PENDIENTE_VALORACION.
  5. Elige el sentido de la valoración «Conceder el préstamo» y pulsa «Valorar la petición», confirmando el aviso «Va a resolver la petición y no podrá deshacerlo».
  6. El sistema pasa el expediente a ENTREGA / PENDIENTE_ENTREGA y no genera ningún documento de resolución.

- ESC-007 — Se deniega el préstamo, con y sin motivo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Préstamo de equipo informático», elige el equipo «Tableta», el motivo «Avería de mi equipo» y la fecha prevista de devolución «30/06/2026», adjunta «informe-tutor.pdf» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar», confirma el aviso y firma el documento con su certificado digital.
  3. El alumno cierra sesión.
  4. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Elige el sentido de la valoración «Denegar el préstamo» y pulsa «Valorar la petición» sin escribir el motivo.
  6. El sistema muestra «Debe indicar el motivo de la denegación» y el expediente sigue en VALORACION / PENDIENTE_VALORACION.
  7. Escribe el motivo «No quedan equipos disponibles este curso» y vuelve a pulsar «Valorar la petición», confirmando el aviso.
  8. El sistema genera el documento de resolución de denegación firmado por el centro, deja constancia de su salida y el expediente queda en VALORACION / DENEGADO.

- ESC-008 — Se pide al alumno que corrija su petición:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Préstamo de equipo informático», elige el equipo «Portátil», el motivo «Otros» con la explicación «Comparto el ordenador con mis hermanos» y la fecha prevista de devolución «30/06/2026», adjunta «informe-tutor.pdf» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar», confirma el aviso y firma el documento con su certificado digital.
  3. El alumno cierra sesión.
  4. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Elige el sentido de la valoración «Pedir que se corrija», escribe en qué hay que corregir «Adjunte el informe de su tutor, el documento aportado no lo acredita» y pulsa «Valorar la petición», confirmando el aviso.
  6. El sistema devuelve el expediente a SOLICITUD / DATOS_PETICION.
  7. El jefe de estudios cierra sesión.
  8. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  9. El sistema muestra, en solo lectura, el texto «Adjunte el informe de su tutor, el documento aportado no lo acredita» y le deja volver a rellenar la petición.

## HU-003 — Como Administrativa quiero registrar la entrega del equipo para que quede constancia de qué se prestó y en qué estado

- ESC-009 — Se registra la entrega del equipo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Préstamo de equipo informático», elige el equipo «Portátil», el motivo «No dispongo de equipo en casa» y la fecha prevista de devolución «30/06/2026», adjunta «informe-tutor.pdf», pulsa «Siguiente», pulsa «Firmar y presentar», confirma el aviso y firma el documento con su certificado digital.
  2. El alumno cierra sesión.
  3. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Conceder el préstamo», pulsa «Valorar la petición» y confirma el aviso.
  4. El jefe de estudios cierra sesión.
  5. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en ENTREGA / PENDIENTE_ENTREGA.
  6. Rellena el número de inventario del equipo entregado con «INV-2026-0184» y las observaciones con «Equipo en buen estado, con cargador».
  7. Pulsa «Registrar la entrega» y confirma el aviso «Va a dar por entregado el equipo y no podrá deshacerlo».
  8. El sistema genera el acta de entrega firmada por el centro y el expediente queda cerrado en ENTREGA / ENTREGADO.

- ESC-010 — Se intenta registrar la entrega sin número de inventario:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Préstamo de equipo informático», elige el equipo «Portátil», el motivo «No dispongo de equipo en casa» y la fecha prevista de devolución «30/06/2026», adjunta «informe-tutor.pdf», pulsa «Siguiente», pulsa «Firmar y presentar», confirma el aviso y firma el documento con su certificado digital.
  2. El alumno cierra sesión.
  3. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Conceder el préstamo», pulsa «Valorar la petición» y confirma el aviso.
  4. El jefe de estudios cierra sesión.
  5. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente y pulsa «Registrar la entrega» sin rellenar el número de inventario.
  6. El sistema muestra «Debe indicar el número de inventario del equipo entregado» y el expediente sigue en ENTREGA / PENDIENTE_ENTREGA.

## HU-004 — Como Alumno quiero consultar mi expediente en cualquier momento para saber en qué punto está mi petición

- ESC-011 — El alumno consulta su expediente mientras se valora:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Préstamo de equipo informático», elige el equipo «Portátil», el motivo «No dispongo de equipo en casa» y la fecha prevista de devolución «30/06/2026», adjunta «informe-tutor.pdf», pulsa «Siguiente», pulsa «Firmar y presentar», confirma el aviso y firma el documento con su certificado digital.
  2. Abre de nuevo el expediente desde su lista de expedientes.
  3. El sistema lo muestra en solo lectura, con la fase «Valoración de la jefatura de estudios» y el estado «Pendiente de valoración» en la cabecera, con el justificante de presentación a la vista y con un único botón «Salir».
  4. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

- ESC-012 — Un alumno de otro centro no ve el expediente:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Préstamo de equipo informático» en el centro «CIPFP Mislata», elige el equipo «Portátil», el motivo «No dispongo de equipo en casa» y la fecha prevista de devolución «30/06/2026», adjunta «informe-tutor.pdf», pulsa «Siguiente», pulsa «Firmar y presentar», confirma el aviso y firma el documento con su certificado digital.
  2. El alumno cierra sesión.
  3. El jefe de estudios «jefeestudios1@batoi.es», del centro «CIPFP Batoi», inicia sesión con la contraseña «demo1234».
  4. El sistema no le muestra ese expediente en su lista de expedientes pendientes de valorar.

# Fases y estados

- **Estado en el que nace el expediente:** DATOS_PETICION (fase SOLICITUD)
- **Estados que cierran el expediente:** DENEGADO (fase VALORACION) y ENTREGADO (fase ENTREGA)
- **Desde qué estado se puede borrar el expediente:** SOLICITUD / DATOS_PETICION, y solo el perfil CREADOR

El ciclo de vida completo —fases, estados, acciones, comprobaciones, efectos y transiciones— está en [estados.md](./estados.md).

| Fase | Título que ve el usuario | Estados | Pantallas |
|---|---|---|---|
| SOLICITUD | Solicitud del préstamo | DATOS_PETICION, PENDIENTE_FIRMA | [pantallas-solicitud.md](./pantallas-solicitud.md) |
| VALORACION | Valoración de la jefatura de estudios | PENDIENTE_VALORACION, DENEGADO | [pantallas-valoracion.md](./pantallas-valoracion.md) |
| ENTREGA | Entrega del equipo | PENDIENTE_ENTREGA, ENTREGADO | [pantallas-entrega.md](./pantallas-entrega.md) |

# Pantallas

| Fichero | Fase | Qué pantallas contiene |
|---|---|---|
| [pantallas-solicitud.md](./pantallas-solicitud.md) | SOLICITUD | La pantalla del alumno y la de solo consulta para DATOS_PETICION y para PENDIENTE_FIRMA. |
| [pantallas-valoracion.md](./pantallas-valoracion.md) | VALORACION | La pantalla del jefe de estudios y la de solo consulta para PENDIENTE_VALORACION; la de solo consulta para DENEGADO. |
| [pantallas-entrega.md](./pantallas-entrega.md) | ENTREGA | La pantalla de la administrativa y la de solo consulta para PENDIENTE_ENTREGA; la de solo consulta para ENTREGADO. |

# Documentos

El trámite genera 3 documentos. Su contenido, cuándo se genera cada uno, quién lo firma y si se registra está en [documentos.md](./documentos.md).

# Registros de entrada y salida, y notificaciones

- **Registros de entrada:** uno, al presentar la petición. El documento principal es la solicitud de préstamo firmada por el alumno y va con el documento acreditativo que el alumno adjuntó como anexo.
- **Registros de salida:** uno, al denegar el préstamo. El documento principal es la resolución de denegación firmada por el centro, sin anexos. Si el préstamo se concede o se pide una corrección no se registra ninguna salida.
- **Avisos que se envían:** *(ninguno)* — el alumno se entera del resultado consultando su propio expediente.

# Seguridad

- **Alumno:** crea expedientes de este trámite y ve solo los suyos, en su centro.
- **Exalumno:** ve solo los suyos, en su centro; no puede crear ninguno nuevo.
- **Cargo Jefes de estudio:** ve y valora los expedientes de su centro.
- **Cargo Administrativas:** ve los expedientes de su centro y registra la entrega de los concedidos.
- **Cargo Director y cargo Secretario:** ven los expedientes de su centro, en solo consulta.
- **Supervisor:** ve los expedientes de su centro, en solo consulta.
- **Administrador:** ve los expedientes de todos los centros, en solo consulta.

# Datos iniciales

- **Asignación de perfiles:** el perfil CREADOR al tipo de usuario Alumno, para todo el trámite; el perfil RESPONSABLE al cargo Jefes de estudio, para todo el trámite; el perfil SECRETARIO al cargo Administrativas, para todo el trámite.
- **Categoría del trámite:** la categoría del alumnado, que ya existe.
- **Otros datos maestros que el trámite necesita:** los certificados del centro para las firmas del Director y del Secretario, que ya existen.

# Fuera de alcance

- La devolución del equipo y el control de los préstamos que se retrasan: este trámite termina con la entrega.
- El inventario de equipos disponibles: el número de inventario se teclea a mano, el trámite no consulta ningún catálogo de equipos.
- Los avisos por correo al alumno cuando se resuelve su petición.
- La firma del alumno en el acta de entrega: el acta la firma solo el centro.
