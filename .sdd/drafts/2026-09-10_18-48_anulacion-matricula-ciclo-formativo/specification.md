---
type: specification
template: expediente
---

# Objetivo

Permitir que un alumno mayor de edad solicite la anulación de su matrícula en un ciclo formativo del centro, que el personal administrativo revise la solicitud y decida si procede anularla (o pida al alumno corregir los datos), que el director firme la resolución y que quede constancia oficial de la solicitud y de la resolución.

# El trámite

- **Nombre visible:** Anulación de matrícula en ciclo formativo
- **A qué colectivo va dirigido:** alumnado — es la categoría bajo la que el alumno lo encuentra en la lista de trámites.
- **Quién puede iniciarlo:** cualquier alumno del centro. Se da por supuesto que el alumno es mayor de edad y firma él mismo.
- **Para qué sirve:** un alumno matriculado en un ciclo formativo puede querer dejar sin efecto su matrícula (por ejemplo, porque va a trabajar, se traslada de centro o cambia de estudios). Este trámite recoge su solicitud con el impreso oficial de anulación de matrícula, la firma el propio alumno y la registra de entrada en el centro. El personal administrativo revisa los datos y decide si la anulación procede o no, o pide al alumno que los corrija; con esa decisión prepara la resolución, que el director firma. Al firmarla, la resolución se registra de salida y el expediente se cierra. Si el director no está conforme, la devuelve a secretaría para que decida de nuevo.
- **Texto de ayuda que ve el usuario antes de empezar:**

  > Con este trámite puedes solicitar la anulación de tu matrícula en un ciclo formativo de este centro. Tus datos personales aparecerán ya rellenos; comprueba que son correctos y completa los datos de contacto y del ciclo en el que estás matriculado. Al terminar tendrás que firmar la solicitud con tu certificado digital desde tu propio ordenador, así que necesitarás tenerlo instalado. El ciclo lo eliges de la lista de ciclos del sistema educativo: puedes buscarlo por su familia profesional, su grado, su nivel o su nombre. La secretaría del centro revisará tu solicitud y, si hay algún error en los datos, te pedirá que los corrijas en este mismo expediente. La resolución la firma el director del centro y la podrás consultar y descargar aquí cuando esté firmada. Ten en cuenta que la anulación surte efecto desde la fecha en que presentas la solicitud.

- **Versión:** la primera versión del trámite.

# Actores y perfiles

| Perfil | Qué papel juega en este trámite | Quién lo ostenta |
|---|---|---|
| CREADOR | Solicita la anulación: comprueba y completa sus datos, firma la solicitud y la presenta; si secretaría le pide subsanar, corrige los datos y vuelve a firmar y presentar. | El tipo de usuario Alumno. |
| SECRETARIO | Revisa la solicitud: comprueba los datos y decide si la anulación procede o no, o pide al alumno que subsane; con su decisión prepara la resolución y la manda a la firma del director. Si el director la devuelve, decide de nuevo. | El personal administrativo del centro (Administrativas). |
| DIRECTOR | Firma la resolución preparada por secretaría, o la devuelve a secretaría indicando por qué. | El cargo Director. |

El perfil SECRETARIO de este trámite lo ostenta el personal administrativo del centro, no el cargo Secretario del centro: son cosas distintas.

Además de los perfiles anteriores, cualquier otro usuario con acceso al expediente (el propio alumno cuando no tiene el turno, el secretario del centro, el supervisor o el administrador) lo ve en solo consulta, con un único botón «Salir».

# Historias de usuario

## HU-001 — Como Alumno quiero solicitar la anulación de mi matrícula para dejarla sin efecto

- ESC-001 — Solicitud completa, firmada y presentada:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, consulta la ayuda de «Anulación de matrícula en ciclo formativo» y pulsa crear un expediente nuevo.
  3. El sistema abre el expediente en el estado SOLICITUD / DATOS_SOLICITUD y muestra ya rellenos, sin poder cambiarlos, el nombre «Alumno1», los apellidos «CIPFP Mislata», el documento de identidad «86862719E», el curso académico «2024/2025» y el centro «CIPFP Mislata».
  4. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920».
  5. Abre la ventana de búsqueda del ciclo, filtra por la familia profesional «Informática y Comunicaciones» y el nivel «Ciclos Formativos de Grado Superior», y elige «Desarrollo de Aplicaciones Web».
  6. Pulsa «Siguiente».
  7. El sistema genera la solicitud de anulación de matrícula con esos datos, la muestra incrustada en la pantalla y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  8. Pulsa «Firmar y presentar la solicitud» y confirma el aviso «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla».
  9. Firma la solicitud con su certificado digital.
  10. El sistema deja constancia de la entrada de la solicitud firmada, guarda el justificante de presentación, anota la fecha y la hora de presentación y el expediente queda en REVISION / PENDIENTE_REVISION.
  11. El alumno ve ahora el expediente en solo consulta, con la fecha y la hora de presentación, el justificante de presentación incrustado en la pantalla, la solicitud firmada como descarga y un único botón «Salir».

- ESC-002 — Se pulsa «Siguiente» sin rellenar el NIA:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Borra el NIA si viniera ya relleno de su ficha, de modo que quede vacío, y rellena la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente».
  5. El sistema muestra «Debe indicar su NIA» y el expediente sigue en SOLICITUD / DATOS_SOLICITUD.

- ESC-003 — Teléfono y código postal con formato incorrecto:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «12345», la población con «Mislata», la provincia con «Valencia» y el código postal con «469», elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente».
  5. El sistema muestra «El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9» y «El código postal debe tener 5 dígitos», y el expediente sigue en SOLICITUD / DATOS_SOLICITUD.
  6. Corrige el teléfono a «612345678» y el código postal a «46920» y vuelve a pulsar «Siguiente».
  7. El sistema genera la solicitud y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.

- ESC-004 — Se pulsa «Siguiente» sin elegir el ciclo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», y deja el ciclo sin elegir.
  4. Pulsa «Siguiente».
  5. El sistema muestra «Debe indicar el ciclo formativo» y el expediente sigue en SOLICITUD / DATOS_SOLICITUD.

- ESC-005 — Se revisa el documento, se vuelve atrás y se corrige el ciclo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Multiplataforma».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA, con la solicitud incrustada en la que se lee «Desarrollo de Aplicaciones Multiplataforma».
  5. Pulsa «Atrás».
  6. El sistema devuelve el expediente a SOLICITUD / DATOS_SOLICITUD con todos los datos que había introducido.
  7. Cambia el ciclo a «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  8. El sistema vuelve a generar la solicitud, ahora con «Desarrollo de Aplicaciones Web», y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.

- ESC-006 — Se intenta presentar la solicitud sin firmarla:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  5. Pulsa «Firmar y presentar la solicitud» y confirma el aviso «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla».
  6. Cancela la firma sin firmar el documento.
  7. El sistema muestra «Debe firmar la solicitud antes de presentarla» y el expediente sigue en SOLICITUD / PENDIENTE_FIRMA.

- ESC-007 — Se borra el expediente antes de presentarlo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Pulsa «Borrar el expediente» y confirma el aviso «Se va a eliminar el expediente y no podrá recuperarlo».
  4. El sistema elimina el expediente y vuelve a la lista; el expediente ya no aparece en su lista de expedientes.

- ESC-026 — Se pulsa «Siguiente» con los datos de contacto vacíos:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», elige el ciclo «Desarrollo de Aplicaciones Web», y deja vacíos la dirección, el teléfono, la población, la provincia y el código postal (borrando los que vinieran ya rellenos de su ficha).
  4. Pulsa «Siguiente».
  5. El sistema muestra «Debe indicar su dirección», «Debe indicar un teléfono de contacto», «Debe indicar su población», «Debe indicar su provincia» y «Debe indicar su código postal», y el expediente sigue en SOLICITUD / DATOS_SOLICITUD sin generar ninguna solicitud.

- ESC-027 — Se pulsa «Siguiente» con el NIA de 7 dígitos y la dirección demasiado corta:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «1234567», la dirección con «C/ A», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente».
  5. El sistema muestra «El NIA debe tener 8 dígitos», «La dirección debe tener entre 5 y 150 caracteres», y el expediente sigue en SOLICITUD / DATOS_SOLICITUD.
  6. Corrige el NIA a «12345678» y la dirección a «C/ Mayor, 12» y vuelve a pulsar «Siguiente».
  7. El sistema genera la solicitud y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.

- ESC-028 — Se firma la solicitud con un certificado que no corresponde al alumno (necesita una persona: se firma a mano con un segundo certificado de pruebas):
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  5. Pulsa «Firmar y presentar la solicitud» y confirma el aviso «Va a firmar y presentar la solicitud de anulación. Una vez presentada no podrá modificarla».
  6. Firma la solicitud con un certificado digital que no es el suyo: el que corresponde al documento de identidad «03532821K», el del alumno «alumno2@mislata.es».
  7. El sistema muestra «La firma no es válida o no corresponde a su documento de identidad», no deja constancia de ninguna entrada y el expediente sigue en SOLICITUD / PENDIENTE_FIRMA.

- ESC-033 — Se pulsa «Siguiente» teniendo ya otra solicitud en curso para el mismo ciclo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital; el expediente queda en REVISION / PENDIENTE_REVISION.
  3. Vuelve a la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un segundo expediente nuevo.
  4. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», y elige el ciclo «Desarrollo de Aplicaciones Web».
  5. Pulsa «Siguiente».
  6. El sistema muestra «Ya tiene una solicitud de anulación en curso para este ciclo» y el segundo expediente sigue en SOLICITUD / DATOS_SOLICITUD sin generar ninguna solicitud.
  7. Cambia el ciclo a «Desarrollo de Aplicaciones Multiplataforma» y vuelve a pulsar «Siguiente».
  8. El sistema genera la solicitud y el segundo expediente queda en SOLICITUD / PENDIENTE_FIRMA.

- ESC-034 — Se elige un ciclo de grado Medio tecleando su nombre y la solicitud y la resolución llevan «Grado Medio»:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920».
  4. Teclea «Microinform» en el campo del ciclo, sin abrir la ventana de búsqueda.
  5. El sistema le propone los ciclos del catálogo cuyo nombre contiene «Microinform»; con el catálogo cargado, solo «Sistemas Microinformáticos y Redes».
  6. Elige «Sistemas Microinformáticos y Redes» y pulsa «Siguiente».
  7. El sistema genera la solicitud y el expediente queda en SOLICITUD / PENDIENTE_FIRMA; en la solicitud incrustada se lee, en el bloque «Expone», «en el Ciclo Formativo de Grado Medio» y «denominado Sistemas Microinformáticos y Redes», y en la línea de lugar y fecha «Mislata» y la fecha de hoy con el día, el mes en letras y el año.
  8. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital; el expediente queda en REVISION / PENDIENTE_REVISION.
  9. El alumno cierra sesión.
  10. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige el sentido de la revisión «Aceptar la anulación», pulsa «Enviar a la firma del director» y confirma el aviso.
  11. La administrativa cierra sesión.
  12. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR.
  13. El sistema le muestra la resolución sin firmar incrustada, en la que la matrícula que se pide anular aparece como «Ciclo Formativo de Grado Medio» seguido de «Sistemas Microinformáticos y Redes», con el curso académico «2024/2025» y el centro «CIPFP Mislata».

- ESC-035 — Se elige un curso de especialización y la solicitud lleva «Curso de especialización» en lugar del grado:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920».
  4. Abre la ventana de búsqueda del ciclo y filtra por el grado «Curso de especialización».
  5. El sistema le muestra solo los ciclos de ese grado: «Inteligencia Artificial y Big Data», sin ninguno de los ciclos de grado «Ciclo formativo» (ni «Desarrollo de Aplicaciones Web» ni «Sistemas Microinformáticos y Redes»).
  6. Elige «Inteligencia Artificial y Big Data» y pulsa «Siguiente».
  7. El sistema genera la solicitud y el expediente queda en SOLICITUD / PENDIENTE_FIRMA; en la solicitud incrustada se lee, en el bloque «Expone», «en el Ciclo Formativo de Grado Curso de especialización» y «denominado Inteligencia Artificial y Big Data».

- ESC-036 — Se solicita de nuevo la anulación del mismo ciclo cuando la solicitud anterior ya está cerrada:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Rechazar la anulación», escribe el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, pulsa «Firmar la resolución» y confirma el aviso; el expediente queda cerrado en RESOLUCION / RECHAZADA.
  6. El director cierra sesión.
  7. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un segundo expediente nuevo.
  8. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», y elige el ciclo «Desarrollo de Aplicaciones Web».
  9. Pulsa «Siguiente».
  10. El sistema no muestra «Ya tiene una solicitud de anulación en curso para este ciclo», genera la solicitud y el segundo expediente queda en SOLICITUD / PENDIENTE_FIRMA.

- ESC-037 — Otro alumno del mismo centro solicita la anulación del mismo ciclo sin que le bloquee la solicitud en curso del primero:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital. El expediente queda en REVISION / PENDIENTE_REVISION.
  2. El alumno cierra sesión.
  3. El alumno «alumno2@mislata.es» inicia sesión con la contraseña «demo1234».
  4. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo; el sistema muestra ya rellenos el nombre «Alumno2», los apellidos «CIPFP Mislata» y el documento de identidad «03532821K».
  5. Rellena el NIA con «23456789», la dirección con «C/ Nueva, 3», el teléfono con «623456789», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», y elige el ciclo «Desarrollo de Aplicaciones Web».
  6. Pulsa «Siguiente».
  7. El sistema no muestra «Ya tiene una solicitud de anulación en curso para este ciclo», genera la solicitud y el expediente de «alumno2@mislata.es» queda en SOLICITUD / PENDIENTE_FIRMA.

- ESC-038 — El alumno vuelve atrás, sale del expediente y lo retoma en otra sesión sin cambiar el ciclo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA, con la solicitud incrustada.
  5. Pulsa «Atrás».
  6. El sistema devuelve el expediente a SOLICITUD / DATOS_SOLICITUD con todos los datos que había introducido.
  7. Vuelve a su lista de expedientes sin pulsar «Siguiente» ni «Borrar el expediente», y cierra sesión.
  8. El alumno «alumno1@mislata.es» inicia sesión de nuevo con la contraseña «demo1234» y abre el expediente desde su lista de expedientes.
  9. El sistema lo muestra en SOLICITUD / DATOS_SOLICITUD, en su pantalla editable, con el NIA «12345678», la dirección «C/ Mayor, 12», el teléfono «612345678», la población «Mislata», la provincia «Valencia», el código postal «46920» y el ciclo «Desarrollo de Aplicaciones Web» tal como los dejó, y con los botones «Siguiente» y «Borrar el expediente».
  10. Pulsa «Siguiente» sin cambiar nada.
  11. El sistema no muestra «Ya tiene una solicitud de anulación en curso para este ciclo» (el único expediente en curso para ese ciclo es este mismo), vuelve a generar la solicitud con los mismos datos y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.

- ESC-039 — En la pantalla de firma no se puede borrar el expediente: hay que volver atrás para borrarlo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  5. El sistema le muestra únicamente los botones «Atrás» y «Firmar y presentar la solicitud», sin ningún botón «Borrar el expediente».
  6. Pulsa «Atrás» y el expediente vuelve a SOLICITUD / DATOS_SOLICITUD.
  7. Pulsa «Borrar el expediente» y confirma el aviso «Se va a eliminar el expediente y no podrá recuperarlo».
  8. El sistema elimina el expediente y ya no aparece en su lista de expedientes.

- ESC-040 — Se pulsa «Siguiente» con la población y la provincia demasiado cortas:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «M», la provincia con «V» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente».
  5. El sistema muestra «La población debe tener entre 2 y 100 caracteres» y «La provincia debe tener entre 2 y 50 caracteres», y el expediente sigue en SOLICITUD / DATOS_SOLICITUD sin generar ninguna solicitud.
  6. Corrige la población a «Mislata» y la provincia a «Valencia» y vuelve a pulsar «Siguiente».
  7. El sistema genera la solicitud y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.

## HU-002 — Como Administrativa quiero revisar las solicitudes de anulación y decidir si proceden para preparar la resolución que firmará el director

- ESC-008 — Se propone aceptar la anulación y se manda a la firma del director:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en REVISION / PENDIENTE_REVISION, con la solicitud firmada incrustada.
  5. Elige el sentido de la revisión «Aceptar la anulación» y pulsa «Enviar a la firma del director», confirmando el aviso «Va a enviar la resolución a la firma del director».
  6. El sistema anota la fecha de la revisión y quién la hizo, genera la resolución sin firmar y el expediente queda en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR; la administrativa lo ve ya en solo consulta, con el aviso «La resolución está pendiente de la firma del director» y sin la resolución.
  7. La administrativa cierra sesión.
  8. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  9. El sistema le muestra el bloque «Decisión de secretaría» con el sentido «Aceptar la anulación», la fecha de la revisión de hoy y «Administrativo1 CIPFP Mislata» como quien la hizo, y la resolución sin firmar incrustada, en la que se lee «Se estima la solicitud y la matrícula queda sin efecto a partir de la fecha de presentación».

- ESC-009 — Se propone rechazar la anulación, sin motivo y después con motivo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Elige el sentido de la revisión «Rechazar la anulación», deja vacío el motivo del rechazo, pulsa «Enviar a la firma del director» y confirma el aviso «Va a enviar la resolución a la firma del director».
  6. El sistema muestra «Debe indicar el motivo del rechazo» y el expediente sigue en REVISION / PENDIENTE_REVISION.
  7. Escribe el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula» y vuelve a pulsar «Enviar a la firma del director», confirmando el aviso.
  8. El sistema anota la fecha de la revisión y quién la hizo, genera la resolución sin firmar y el expediente queda en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR; la administrativa lo ve ya en solo consulta, con el aviso «La resolución está pendiente de la firma del director» y sin la resolución ni el motivo del rechazo.
  9. La administrativa cierra sesión.
  10. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  11. El sistema le muestra el bloque «Decisión de secretaría» con el sentido «Rechazar la anulación», el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», la fecha de la revisión de hoy y «Administrativo1 CIPFP Mislata» como quien la hizo, y la resolución sin firmar incrustada, en la que se lee «Se desestima la solicitud» y, tras «Por el siguiente motivo:», ese mismo motivo.

- ESC-010 — Se pulsa «Enviar a la firma del director» sin elegir el sentido:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Pulsa «Enviar a la firma del director» sin elegir ningún sentido de la revisión y confirma el aviso «Va a enviar la resolución a la firma del director».
  6. El sistema muestra «Debe indicar el sentido de la revisión» y el expediente sigue en REVISION / PENDIENTE_REVISION.

- ESC-041 — Se pulsa «Enviar a la firma del director» con el sentido «Pedir subsanación»:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en REVISION / PENDIENTE_REVISION.
  5. Elige el sentido de la revisión «Pedir subsanación» y escribe en qué hay que subsanar «Está matriculado en Desarrollo de Aplicaciones Multiplataforma, no en Desarrollo de Aplicaciones Web».
  6. Pulsa «Enviar a la firma del director» y confirma el aviso «Va a enviar la resolución a la firma del director».
  7. El sistema muestra «Para pedir una subsanación use el botón «Pedir subsanación al alumno»», no genera ninguna resolución y el expediente sigue en REVISION / PENDIENTE_REVISION, con el sentido «Pedir subsanación» y el texto de la subsanación tal como los había escrito.

- ESC-042 — Se propone rechazar con un motivo demasiado corto:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Elige el sentido de la revisión «Rechazar la anulación», escribe el motivo del rechazo «Plazo», pulsa «Enviar a la firma del director» y confirma el aviso.
  6. El sistema muestra «El motivo del rechazo debe tener entre 10 y 1000 caracteres» y el expediente sigue en REVISION / PENDIENTE_REVISION sin generar ninguna resolución.
  7. Corrige el motivo del rechazo a «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», vuelve a pulsar «Enviar a la firma del director» y confirma el aviso.
  8. El sistema genera la resolución sin firmar y el expediente queda en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR.

## HU-003 — Como Administrativa quiero pedir al alumno que corrija su solicitud para no tener que rechazarla por un error en los datos

- ESC-011 — Se pide subsanación sin indicar qué hay que subsanar:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Elige el sentido de la revisión «Pedir subsanación», pulsa «Pedir subsanación al alumno» sin escribir qué hay que subsanar y confirma el aviso «Va a devolver la solicitud al alumno para que la corrija».
  6. El sistema muestra «Debe indicar al alumno qué tiene que subsanar» y el expediente sigue en REVISION / PENDIENTE_REVISION.

- ESC-012 — Se pide subsanación, el alumno corrige y vuelve a presentar, y la solicitud corregida llega de nuevo a revisión:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Multiplataforma» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Elige el sentido de la revisión «Pedir subsanación», escribe en qué hay que subsanar «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma» y pulsa «Pedir subsanación al alumno», confirmando el aviso «Va a devolver la solicitud al alumno para que la corrija».
  6. El sistema devuelve el expediente a SOLICITUD / DATOS_SOLICITUD, sin generar ninguna resolución.
  7. La administrativa cierra sesión.
  8. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  9. El sistema le muestra, en solo lectura, el bloque «Qué hay que subsanar» con el texto «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma» y todos los datos que había introducido, editables.
  10. Cambia el ciclo a «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  11. El sistema vuelve a generar la solicitud, ahora con «Desarrollo de Aplicaciones Web», y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  12. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  13. El sistema deja constancia de la entrada de la nueva solicitud firmada, sustituye el justificante de presentación por el nuevo, borra el texto de qué hay que subsanar y el expediente queda en REVISION / PENDIENTE_REVISION.
  14. El alumno cierra sesión.
  15. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente: ve la solicitud corregida con el ciclo «Desarrollo de Aplicaciones Web», el sentido de la revisión sin elegir y ya no ve ningún texto de subsanación.

- ESC-013 — El alumno desiste tras la petición de subsanación y borra el expediente:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Multiplataforma» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en REVISION / PENDIENTE_REVISION.
  5. Elige el sentido de la revisión «Pedir subsanación», escribe en qué hay que subsanar «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma» y pulsa «Pedir subsanación al alumno», confirmando el aviso «Va a devolver la solicitud al alumno para que la corrija».
  6. El sistema devuelve el expediente a SOLICITUD / DATOS_SOLICITUD.
  7. La administrativa cierra sesión.
  8. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en SOLICITUD / DATOS_SOLICITUD.
  9. Pulsa «Borrar el expediente» y confirma el aviso «Se va a eliminar el expediente y no podrá recuperarlo».
  10. El sistema elimina el expediente y ya no aparece en su lista de expedientes.

- ESC-031 — Tras la devolución del director, secretaría pide subsanación y la solicitud corregida vuelve a revisión sin rastro de la decisión anterior:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Multiplataforma».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  5. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  6. El alumno cierra sesión.
  7. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  8. Elige el sentido de la revisión «Aceptar la anulación», pulsa «Enviar a la firma del director» y confirma el aviso.
  9. La administrativa cierra sesión.
  10. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  11. Escribe el motivo de la devolución «El ciclo no coincide con la matrícula del alumno; pida al alumno que lo corrija» y pulsa «Devolver a secretaría», confirmando el aviso «Va a devolver la resolución a secretaría sin firmarla».
  12. El director cierra sesión.
  13. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente: ve el bloque «Devuelto por el director» con ese motivo y el sentido «Aceptar la anulación» que había elegido.
  14. Cambia el sentido de la revisión a «Pedir subsanación», escribe en qué hay que subsanar «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma» y pulsa «Pedir subsanación al alumno», confirmando el aviso «Va a devolver la solicitud al alumno para que la corrija».
  15. El sistema devuelve el expediente a SOLICITUD / DATOS_SOLICITUD, borra el motivo de la devolución y la resolución sin firmar, y no deja constancia de ninguna salida.
  16. La administrativa cierra sesión.
  17. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  18. El sistema le muestra el bloque «Qué hay que subsanar» con el texto «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma» y todos sus datos editables; no le muestra nada de la devolución del director ni de la decisión de secretaría.
  19. Cambia el ciclo a «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  20. El sistema vuelve a generar la solicitud, ahora con «Desarrollo de Aplicaciones Web», y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  21. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  22. El sistema deja constancia de una nueva entrada de la solicitud firmada, sustituye el justificante de presentación y el expediente queda en REVISION / PENDIENTE_REVISION.
  23. El alumno cierra sesión.
  24. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente: ve la solicitud firmada con el ciclo «Desarrollo de Aplicaciones Web», el sentido de la revisión sin elegir, sin motivo del rechazo, sin texto de qué hay que subsanar y sin el bloque «Devuelto por el director».

- ESC-043 — Se pulsa «Pedir subsanación al alumno» con el sentido «Aceptar la anulación»:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en REVISION / PENDIENTE_REVISION.
  5. Elige el sentido de la revisión «Aceptar la anulación».
  6. Pulsa «Pedir subsanación al alumno» y confirma el aviso «Va a devolver la solicitud al alumno para que la corrija».
  7. El sistema muestra «Para pedir una subsanación elija el sentido «Pedir subsanación»» y el expediente sigue en REVISION / PENDIENTE_REVISION, sin volver al alumno.

- ESC-044 — Se pide subsanación con un texto demasiado corto:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Multiplataforma» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Elige el sentido de la revisión «Pedir subsanación», escribe en qué hay que subsanar «Ciclo», pulsa «Pedir subsanación al alumno» y confirma el aviso.
  6. El sistema muestra «El texto de la subsanación debe tener entre 10 y 1000 caracteres» y el expediente sigue en REVISION / PENDIENTE_REVISION.
  7. Corrige el texto a «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma», vuelve a pulsar «Pedir subsanación al alumno» y confirma el aviso.
  8. El sistema devuelve el expediente a SOLICITUD / DATOS_SOLICITUD.

- ESC-045 — La administrativa consulta en solo lectura el expediente devuelto al alumno para subsanar:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Multiplataforma» y pulsa «Siguiente».
  2. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  3. El alumno cierra sesión.
  4. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  5. Elige el sentido de la revisión «Pedir subsanación», escribe en qué hay que subsanar «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma», pulsa «Pedir subsanación al alumno» y confirma el aviso.
  6. El sistema devuelve el expediente a SOLICITUD / DATOS_SOLICITUD y vuelve a la lista.
  7. La administrativa abre de nuevo el expediente desde la lista de expedientes de su centro.
  8. El sistema lo muestra en solo lectura, con la fase «Solicitud de anulación» y el estado «Datos de la solicitud» en la cabecera, con el bloque «Qué hay que subsanar» y el texto «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma», los datos del alumno y de la matrícula sin poder cambiarlos, y un único botón «Salir»; no le ofrece «Siguiente» ni «Borrar el expediente».
  9. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

- ESC-046 — Tras un rechazo devuelto por el director, secretaría pide subsanación y el motivo del rechazo no reaparece:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Multiplataforma», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Rechazar la anulación», escribe el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, escribe el motivo de la devolución «El ciclo no coincide con la matrícula del alumno; pida al alumno que lo corrija» y pulsa «Devolver a secretaría», confirmando el aviso.
  6. El director cierra sesión.
  7. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente: ve el bloque «Devuelto por el director», el sentido «Rechazar la anulación» y el motivo del rechazo que había escrito.
  8. Cambia el sentido a «Pedir subsanación», escribe en qué hay que subsanar «Está matriculado en Desarrollo de Aplicaciones Web, no en Desarrollo de Aplicaciones Multiplataforma», pulsa «Pedir subsanación al alumno» y confirma el aviso.
  9. El sistema devuelve el expediente a SOLICITUD / DATOS_SOLICITUD y borra el motivo del rechazo, el motivo de la devolución y la resolución sin firmar.
  10. La administrativa cierra sesión.
  11. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente: ve el bloque «Qué hay que subsanar».
  12. Cambia el ciclo a «Desarrollo de Aplicaciones Web» y pulsa «Siguiente».
  13. El sistema vuelve a generar la solicitud, ahora con «Desarrollo de Aplicaciones Web», y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  14. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital; el expediente queda en REVISION / PENDIENTE_REVISION.
  15. El alumno cierra sesión.
  16. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente: ve el sentido de la revisión sin elegir y sin el bloque «Devuelto por el director».
  17. Elige el sentido «Rechazar la anulación» y el sistema le muestra el motivo del rechazo vacío, sin el texto «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula» que había escrito en el ciclo de revisión anterior.

## HU-004 — Como Director quiero firmar la resolución preparada por secretaría, o devolvérsela si no estoy conforme, para que la anulación tenga efectos oficiales

- ESC-014 — El director firma una resolución de aceptación:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Aceptar la anulación», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, que está en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR, con la resolución sin firmar incrustada, en la que se lee «Se estima».
  6. Pulsa «Firmar la resolución» y confirma el aviso «Va a firmar la resolución. Una vez firmada se registrará de salida y el expediente quedará cerrado».
  7. El sistema firma la resolución con la firma del Director del centro, deja constancia de su salida, anota la fecha de la resolución y el expediente queda cerrado en RESOLUCION / ACEPTADA, con la resolución firmada incrustada.

- ESC-015 — El director firma una resolución de rechazo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Rechazar la anulación», escribe el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, con la resolución sin firmar incrustada, en la que se lee «Se desestima» y el motivo.
  6. Pulsa «Firmar la resolución» y confirma el aviso «Va a firmar la resolución. Una vez firmada se registrará de salida y el expediente quedará cerrado».
  7. El sistema anota la fecha de la resolución (la de hoy) y quién la firmó, firma la resolución con la firma del Director del centro, deja constancia de su salida y el expediente queda cerrado en RESOLUCION / RECHAZADA, con la resolución firmada incrustada, en la que se lee «Se desestima la solicitud» y, tras «Por el siguiente motivo:», el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula».

- ESC-016 — El director devuelve la resolución a secretaría sin indicar el motivo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Aceptar la anulación», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  6. Pulsa «Devolver a secretaría» sin escribir el motivo de la devolución y confirma el aviso «Va a devolver la resolución a secretaría sin firmarla».
  7. El sistema muestra «Debe indicar a secretaría por qué devuelve la resolución» y el expediente sigue en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR.

- ESC-017 — El director devuelve la resolución y secretaría decide de nuevo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Aceptar la anulación», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  6. Escribe el motivo de la devolución «El alumno ya ha superado el plazo de anulación; la solicitud debe rechazarse» y pulsa «Devolver a secretaría», confirmando el aviso «Va a devolver la resolución a secretaría sin firmarla».
  7. El sistema devuelve el expediente a REVISION / PENDIENTE_REVISION sin firmar ni registrar nada.
  8. El director cierra sesión.
  9. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  10. El sistema le muestra el bloque «Devuelto por el director» con el texto «El alumno ya ha superado el plazo de anulación; la solicitud debe rechazarse» y el sentido de la revisión que había elegido, «Aceptar la anulación», editable.
  11. Cambia el sentido a «Rechazar la anulación», escribe el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», pulsa «Enviar a la firma del director» y confirma el aviso.
  12. El sistema vuelve a generar la resolución, ahora con «Se desestima» y el motivo, borra el motivo de la devolución y el expediente queda en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR.

- ESC-047 — El director devuelve la resolución con un motivo demasiado corto:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Aceptar la anulación», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  6. Escribe el motivo de la devolución «Revisar» y pulsa «Devolver a secretaría», confirmando el aviso «Va a devolver la resolución a secretaría sin firmarla».
  7. El sistema muestra «El motivo de la devolución debe tener entre 10 y 1000 caracteres» y el expediente sigue en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR.

- ESC-048 — El director devuelve un rechazo, secretaría lo cambia por la aceptación y el motivo del rechazo desaparece de la resolución:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Rechazar la anulación», escribe el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente, con la resolución sin firmar incrustada en la que se lee «Se desestima» y el motivo.
  6. Escribe el motivo de la devolución «El alumno presentó la solicitud dentro del plazo; la anulación procede» y pulsa «Devolver a secretaría», confirmando el aviso «Va a devolver la resolución a secretaría sin firmarla».
  7. El sistema anota la fecha de la devolución y quién la hizo, y devuelve el expediente a REVISION / PENDIENTE_REVISION sin firmar ni registrar nada.
  8. El director cierra sesión.
  9. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente: ve el bloque «Devuelto por el director» con ese motivo, el sentido «Rechazar la anulación» y el motivo del rechazo que había escrito.
  10. Cambia el sentido a «Aceptar la anulación» y el motivo del rechazo deja de mostrarse.
  11. Pulsa «Enviar a la firma del director» y confirma el aviso.
  12. El sistema vuelve a generar la resolución, ahora con «Se estima», borra el motivo del rechazo y el motivo de la devolución, y el expediente queda en RESOLUCION / PENDIENTE_FIRMA_DIRECTOR.
  13. La administrativa cierra sesión.
  14. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente: en el bloque «Decisión de secretaría» ve el sentido «Aceptar la anulación» sin ningún motivo del rechazo, y en la resolución sin firmar incrustada se lee «Se estima la solicitud y la matrícula queda sin efecto a partir de la fecha de presentación» y no aparece «Por el siguiente motivo:».
  15. Pulsa «Firmar la resolución» y confirma el aviso «Va a firmar la resolución. Una vez firmada se registrará de salida y el expediente quedará cerrado».
  16. El sistema firma la resolución, deja constancia de su salida y el expediente queda cerrado en RESOLUCION / ACEPTADA, con la resolución firmada incrustada sin ningún motivo.

## HU-005 — Como Alumno quiero consultar mi expediente en cualquier momento para saber en qué punto está y descargar mi justificante y mi resolución

- ESC-018 — El alumno consulta su expediente mientras está en revisión:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. Abre de nuevo el expediente desde su lista de expedientes.
  3. El sistema lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión» en la cabecera, con el aviso «La solicitud está pendiente de revisión por la secretaría del centro», el justificante de presentación incrustado, la solicitud firmada como descarga y un único botón «Salir».
  4. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

- ESC-019 — El alumno consulta un expediente cerrado con la anulación aceptada:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, elige «Aceptar la anulación», pulsa «Enviar a la firma del director» y confirma el aviso.
  4. La administrativa cierra sesión.
  5. El director «director@mislata.es» inicia sesión con la contraseña «demo1234», abre el expediente, pulsa «Firmar la resolución» y confirma el aviso.
  6. El director cierra sesión.
  7. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  8. El sistema lo muestra en solo lectura, con la fase «Resolución del centro» y el estado «Anulación aceptada» en la cabecera; en el bloque «Presentación» ve la fecha y la hora de presentación (las de hoy), desde la que surte efecto la anulación, y el justificante de presentación; en el bloque «Resolución» ve la fecha de la resolución (la de hoy), quién la firmó («Director CIPFP Mislata») y la resolución firmada incrustada, en la que se lee «Se estima la solicitud y la matrícula queda sin efecto a partir de la fecha de presentación» y sin ningún motivo; la solicitud firmada y el justificante de presentación como descarga; el aviso «El expediente está cerrado» y un único botón «Salir», sin ningún botón de acción.
  9. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

- ESC-029 — El alumno consulta su expediente mientras está pendiente de la firma del director y no ve la decisión de secretaría:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  5. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  6. El alumno cierra sesión.
  7. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  8. Elige el sentido de la revisión «Rechazar la anulación», escribe el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», pulsa «Enviar a la firma del director» y confirma el aviso.
  9. La administrativa cierra sesión.
  10. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  11. El sistema lo muestra en solo lectura, con la fase «Resolución del centro» y el estado «Pendiente de la firma del director» en la cabecera, con el aviso «La resolución está pendiente de la firma del director», el justificante de presentación incrustado y la solicitud firmada como descarga; no muestra el sentido de la revisión, ni el motivo del rechazo, ni la resolución sin firmar, y el único botón es «Salir».
  12. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

- ESC-030 — El alumno consulta un expediente cerrado con la anulación rechazada y lee el motivo del rechazo:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  5. Pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  6. El alumno cierra sesión.
  7. La administrativa «administrativo1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  8. Elige el sentido de la revisión «Rechazar la anulación», escribe el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», pulsa «Enviar a la firma del director» y confirma el aviso.
  9. La administrativa cierra sesión.
  10. El director «director@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  11. Pulsa «Firmar la resolución» y confirma el aviso «Va a firmar la resolución. Una vez firmada se registrará de salida y el expediente quedará cerrado».
  12. El director cierra sesión.
  13. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente.
  14. El sistema lo muestra en solo lectura, con la fase «Resolución del centro» y el estado «Anulación rechazada» en la cabecera; en el bloque «Resolución» ve el motivo del rechazo «La solicitud se presenta fuera del plazo establecido para la anulación de matrícula», la fecha de la resolución (la de hoy), quién la firmó («Director CIPFP Mislata») y la resolución firmada incrustada, en la que se lee «Se desestima la solicitud» y, tras «Por el siguiente motivo:», ese mismo motivo; la solicitud firmada y el justificante de presentación como descarga; el aviso «El expediente está cerrado» y un único botón «Salir», sin ningún botón de acción.
  15. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

## HU-006 — Como Secretario quiero consultar las anulaciones de matrícula de mi centro para tenerlas en cuenta en la gestión de la matrícula

- ESC-020 — El secretario consulta un expediente de su centro:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. El secretario «secretario@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente desde la lista de expedientes de su centro.
  4. El sistema lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión» en la cabecera, con los datos del alumno y de la matrícula, la fecha y la hora de presentación, el justificante de presentación incrustado y la solicitud firmada como descarga, y un único botón «Salir»; no le ofrece «Enviar a la firma del director» ni «Pedir subsanación al alumno», ni le muestra el sentido de la revisión.
  5. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

- ESC-032 — El secretario consulta un expediente que el alumno todavía no ha firmado ni presentado:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Anulación de matrícula en ciclo formativo» y crea un expediente nuevo.
  3. Rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920» y elige el ciclo «Desarrollo de Aplicaciones Web».
  4. Pulsa «Siguiente» y el expediente queda en SOLICITUD / PENDIENTE_FIRMA.
  5. El alumno cierra sesión.
  6. El secretario «secretario@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente desde la lista de expedientes de su centro.
  7. El sistema lo muestra en solo lectura, con la fase «Solicitud de anulación» y el estado «Pendiente de firma y presentación» en la cabecera, con los datos del alumno y de la matrícula, la solicitud de anulación generada y sin firmar como descarga, y un único botón «Salir»; no le ofrece «Atrás», «Firmar y presentar la solicitud» ni «Borrar el expediente».
  8. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

- ESC-049 — El vicesecretario consulta un expediente de su centro en solo lectura:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. El vicesecretario «vicesecretario@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente desde la lista de expedientes de su centro «CIPFP Mislata».
  4. El sistema lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión» en la cabecera, con los datos del alumno y de la matrícula, la fecha y la hora de presentación, el justificante de presentación incrustado y la solicitud firmada como descarga, y un único botón «Salir»; no le ofrece «Enviar a la firma del director» ni «Pedir subsanación al alumno», ni le muestra el sentido de la revisión.
  5. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

## HU-007 — Como Director quiero que solo se vean los expedientes del propio centro para que la información de cada centro quede protegida

- ESC-021 — La administrativa de otro centro no ve el expediente:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo» en el centro «CIPFP Mislata», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. La administrativa «administrativo1@batoi.es», del centro «CIPFP Batoi», inicia sesión con la contraseña «demo1234».
  4. La administrativa abre la lista de expedientes pendientes de revisar de su centro.
  5. El sistema no le muestra ese expediente ni en esa lista ni en ninguna otra lista de expedientes que la administrativa de «CIPFP Batoi» pueda abrir.

- ESC-022 — Un familiar del alumno no ve el expediente:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. El familiar «familiar1@mislata.es» inicia sesión con la contraseña «demo1234».
  4. El familiar abre la lista de trámites disponibles.
  5. El sistema no le ofrece el trámite «Anulación de matrícula en ciclo formativo» entre los que puede iniciar.
  6. El familiar abre su lista de expedientes.
  7. El sistema no le muestra el expediente del alumno «alumno1@mislata.es» en ninguna lista.

- ESC-050 — Otro alumno del mismo centro no ve el expediente:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. El alumno «alumno2@mislata.es», del mismo centro «CIPFP Mislata», inicia sesión con la contraseña «demo1234».
  4. Abre su lista de expedientes.
  5. El sistema no le muestra el expediente del alumno «alumno1@mislata.es» en ninguna lista de expedientes que pueda abrir: solo puede ver los expedientes que él mismo haya creado.

## HU-008 — Como Exalumno quiero consultar los expedientes de anulación que abrí cuando era alumno para conservar mi justificante y mi resolución, sin poder iniciar ninguno nuevo

- ESC-023 — El exalumno no puede iniciar el trámite:
  1. El exalumno «exalumno1@mislata.es» inicia sesión con la contraseña «demo1234».
  2. Abre la lista de trámites disponibles.
  3. El sistema no le ofrece el trámite «Anulación de matrícula en ciclo formativo» entre los que puede iniciar, así que no puede crear ningún expediente nuevo de este trámite.

## HU-009 — Como Supervisor quiero consultar en solo lectura los expedientes de anulación de mi centro para hacer seguimiento de la gestión sin intervenir en ellos

- ESC-024 — El supervisor consulta un expediente de su centro y el supervisor del otro centro no lo ve:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. El supervisor «supervisor1@mislata.es» inicia sesión con la contraseña «demo1234» y abre el expediente desde la lista de expedientes de su centro «CIPFP Mislata».
  4. El sistema lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión» en la cabecera, con los datos del alumno y de la matrícula, la fecha y la hora de presentación, el justificante de presentación incrustado, la solicitud firmada como descarga y un único botón «Salir»; no le ofrece «Enviar a la firma del director» ni «Pedir subsanación al alumno», ni le muestra el sentido de la revisión.
  5. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.
  6. El supervisor cierra sesión.
  7. El supervisor «supervisor1@batoi.es», del centro «CIPFP Batoi», inicia sesión con la contraseña «demo1234».
  8. Abre la lista de expedientes de su centro «CIPFP Batoi».
  9. El sistema le muestra esa lista sin el expediente del alumno «alumno1@mislata.es»: el expediente no aparece en ninguna lista de expedientes que el supervisor de «CIPFP Batoi» pueda abrir.

## HU-010 — Como Administrador quiero consultar en solo lectura los expedientes de anulación de todos los centros para supervisar el uso del trámite sin intervenir en ellos

- ESC-025 — El administrador ve los expedientes de los dos centros en solo lectura:
  1. El alumno «alumno1@mislata.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo» en el centro «CIPFP Mislata», rellena el NIA con «12345678», la dirección con «C/ Mayor, 12», el teléfono con «612345678», la población con «Mislata», la provincia con «Valencia» y el código postal con «46920», elige el ciclo «Desarrollo de Aplicaciones Web», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  2. El alumno cierra sesión.
  3. El alumno «alumno1@batoi.es» inicia sesión con la contraseña «demo1234», crea un expediente de «Anulación de matrícula en ciclo formativo» en el centro «CIPFP Batoi», rellena el NIA con «87654321», la dirección con «C/ Sant Nicolau, 5», el teléfono con «698765432», la población con «Alcoi», la provincia con «Alicante» y el código postal con «03800», elige el ciclo «Sistemas Microinformáticos y Redes», pulsa «Siguiente», pulsa «Firmar y presentar la solicitud», confirma el aviso y firma la solicitud con su certificado digital.
  4. El alumno cierra sesión.
  5. El administrador «admin» inicia sesión con la contraseña «admin».
  6. Abre la lista de expedientes de «Anulación de matrícula en ciclo formativo».
  7. El sistema le muestra los dos expedientes: el del centro «CIPFP Mislata» y el del centro «CIPFP Batoi».
  8. Abre el expediente del alumno «Alumno1 CIPFP Batoi».
  9. El sistema lo muestra en solo lectura, con la fase «Revisión de la secretaría» y el estado «Pendiente de revisión» en la cabecera, con los datos del alumno y de la matrícula, la fecha y la hora de presentación, el justificante de presentación incrustado, la solicitud firmada como descarga y un único botón «Salir»; no le ofrece «Enviar a la firma del director» ni «Pedir subsanación al alumno», ni le muestra el sentido de la revisión.
  10. Pulsa «Salir» y el sistema vuelve a la lista sin cambiar nada.

# Fases y estados

- **Estado en el que nace el expediente:** DATOS_SOLICITUD (fase SOLICITUD)
- **Estados que cierran el expediente:** ACEPTADA y RECHAZADA (fase RESOLUCION)
- **Desde qué estado se puede borrar el expediente:** SOLICITUD / DATOS_SOLICITUD, y solo el perfil CREADOR

El ciclo de vida completo —fases, estados, acciones, comprobaciones, efectos y transiciones— está en [estados.md](./estados.md).

| Fase | Título que ve el usuario | Estados | Pantallas |
|---|---|---|---|
| SOLICITUD | Solicitud de anulación | DATOS_SOLICITUD, PENDIENTE_FIRMA | [pantallas-solicitud.md](./pantallas-solicitud.md) |
| REVISION | Revisión de la secretaría | PENDIENTE_REVISION | [pantallas-revision.md](./pantallas-revision.md) |
| RESOLUCION | Resolución del centro | PENDIENTE_FIRMA_DIRECTOR, ACEPTADA, RECHAZADA | [pantallas-resolucion.md](./pantallas-resolucion.md) |

# Pantallas

| Fichero | Fase | Qué pantallas contiene |
|---|---|---|
| [pantallas-solicitud.md](./pantallas-solicitud.md) | SOLICITUD | La pantalla del alumno y la de solo consulta para DATOS_SOLICITUD y para PENDIENTE_FIRMA. |
| [pantallas-revision.md](./pantallas-revision.md) | REVISION | La pantalla de la administrativa y la de solo consulta para PENDIENTE_REVISION. |
| [pantallas-resolucion.md](./pantallas-resolucion.md) | RESOLUCION | La pantalla del director y la de solo consulta para PENDIENTE_FIRMA_DIRECTOR; la de solo consulta para ACEPTADA y para RECHAZADA. |

# Documentos

El trámite genera 2 documentos (la solicitud de anulación de matrícula y la resolución). Su contenido, cuándo se genera cada uno, quién lo firma y si se registra está en [documentos.md](./documentos.md). Además, el expediente guarda el justificante de presentación que produce el registro de entrada del centro, que no es un documento diseñado por este trámite.

# Registros de entrada y salida, y notificaciones

- **Registros de entrada:** uno por cada presentación de la solicitud: la primera vez y cada vez que el alumno vuelve a presentarla tras una petición de subsanación. El documento principal es la solicitud de anulación de matrícula firmada por el alumno, sin anexos. El registro produce el justificante de presentación, que se guarda en el expediente (si venía de una subsanación, sustituye al anterior).
- **Registros de salida:** uno, cuando el director firma la resolución (tanto si acepta como si rechaza la anulación). El documento principal es la resolución firmada por el centro, sin anexos. Si se pide una subsanación o el director devuelve la resolución a secretaría no se registra ninguna salida.
- **Avisos que se envían:** *(ninguno)* — el alumno se entera de la petición de subsanación y de la resolución consultando su propio expediente; la administrativa y el director ven lo que tienen pendiente en sus listas de expedientes.

# Seguridad

- **Alumno:** crea expedientes de este trámite y ve solo los suyos, en su centro.
- **Exalumno:** ve solo los suyos (los que creó cuando era alumno), en su centro; no puede crear ninguno nuevo.
- **Administrativas (personal administrativo del centro):** ve y revisa los expedientes de su centro.
- **Cargo Director:** ve los expedientes de su centro y firma o devuelve sus resoluciones.
- **Cargo Secretario y cargo Vicesecretario:** ven los expedientes de su centro, en solo consulta.
- **Supervisor:** ve los expedientes de su centro, en solo consulta.
- **Administrador:** ve los expedientes de todos los centros, en solo consulta.

# Datos iniciales

- **Asignación de perfiles:** el perfil CREADOR al tipo de usuario Alumno, para todo el trámite (quien deja de ser alumno y pasa a Exalumno ya no puede iniciar el trámite, pero **MUST** seguir viendo en solo consulta los expedientes que creó siendo alumno: el acceso del interesado a su propio expediente es por autoría, no por el tipo de usuario que tenga en cada momento); el perfil SECRETARIO al personal administrativo del centro (Administrativas), para todo el trámite; el perfil DIRECTOR al cargo Director, para todo el trámite.
- **Categoría del trámite:** la categoría del alumnado, que ya existe.
- **Otros datos maestros que el trámite necesita:** el certificado del centro para la firma del Director, que ya existe; y el centro debe tener informado su curso académico en vigor, porque se imprime en la solicitud y en la resolución (los centros de demo lo tienen). Y el catálogo del sistema educativo (familias profesionales, grados, niveles y ciclos), que ya existe y ya viene cargado: el alumno elige su ciclo de ahí, así que el ciclo en el que está matriculado tiene que figurar en él. El catálogo cargado hoy solo tiene ciclos de grado «Ciclo formativo»; para que el trámite cubra también los cursos de especialización **MUST** cargarse además el curso de especialización «Inteligencia Artificial y Big Data», de la familia profesional «Informática y Comunicaciones», de grado «Curso de especialización» y sin nivel, que hoy no existe en el catálogo.

# Fuera de alcance

- La baja efectiva de la matrícula en el sistema de gestión académica: el trámite termina con la resolución firmada; el centro la traslada a la gestión académica por sus propios medios.
- La devolución de tasas o precios públicos derivada de la anulación.
- Los alumnos menores de edad y sus tutores o representantes: el solicitante es siempre el propio alumno, mayor de edad, que firma él mismo.
- La firma en papel y la presentación presencial de la solicitud.
- La anulación parcial de la matrícula (de módulos sueltos): se anula la matrícula completa del ciclo.
- Comprobar automáticamente que el alumno está realmente matriculado en el ciclo que elige: la aplicación no guarda las matrículas, así que el trámite se fía del ciclo que el alumno elige y es secretaría quien lo verifica al revisar.
- Los avisos por correo al alumno, a secretaría o al director en cada cambio de turno.
- Los plazos de resolución y el silencio administrativo.
- Que el director pueda cambiar por sí mismo el sentido de la resolución: si no está conforme la devuelve a secretaría, que es quien decide.
- Actualizar la ficha del alumno con los datos de contacto que corrija en la solicitud: las correcciones se quedan solo en el expediente; mantener la ficha es cosa de la gestión de centro.
- Impedir que un alumno vuelva a solicitar la anulación de un ciclo cuya solicitud anterior ya se cerró (aceptada o rechazada): solo se impide tener dos solicitudes **en curso** para el mismo ciclo en el mismo curso académico.
- El mantenimiento del catálogo de ciclos: si el ciclo del alumno no está en la lista, hay que darlo de alta en el catálogo del sistema educativo, que es común a todos los centros y no forma parte de este trámite.
