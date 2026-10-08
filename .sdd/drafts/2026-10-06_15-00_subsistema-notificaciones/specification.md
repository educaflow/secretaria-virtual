---
type: specification
template: system
---

# Objetivo

Crear el **subsistema «Notificaciones»**, que reúne en un único sitio todos los avisos que la secretaría virtual envía a las personas (hoy correos electrónicos y SMS) y sustituye a los dos subsistemas actuales de correos y de SMS, que desaparecen.
Cada notificación es de un **canal** (correo o SMS) y el subsistema debe poder crecer con canales nuevos sin cambiar lo que ya existe.
Se consultan todas juntas en un listado común, con su canal, un **motivo** interno que explica de qué va y su **destino** (la dirección o el teléfono); al abrir una se ve el formulario propio de su canal.
Depende funcionalmente de la gestión de centros y usuarios (los centros, y los tipos de usuario y cargos de cada persona en cada centro) y de los expedientes (una notificación puede ir ligada al estado de un expediente).
El envío real del correo y del SMS lo siguen haciendo los servicios de envío que ya existen.

# Actores

- **Administrador**: administrador global de la aplicación. Ve todas las notificaciones de todos los centros, da de alta notificaciones a mano (correos y SMS) y reenvía las que han fallado.
- **Gestor del centro**: cualquier usuario que en un centro tenga el tipo de usuario **Supervisor** o **Administrativo**, o el cargo de **Director**, **Jefe de estudios** o **Secretario**. Ve las notificaciones de los centros en los que tiene ese tipo o cargo y reenvía las que han fallado.
- **Destinatario**: cualquier usuario (alumno, profesor, familiar, externo, exalumno…) cuyo DNI coincide con el DNI del destinatario de una notificación. Consulta las notificaciones que se le han enviado con éxito, de cualquier centro.
- **Sistema**: la propia aplicación, que crea notificaciones automáticamente durante la tramitación de expedientes (hoy, el aviso de subsanación de una solicitud) y realiza los envíos y sus reintentos.

# Historias de usuario

## HU-001 — Como Administrador quiero dar de alta un correo para avisar puntualmente a una persona por correo electrónico

- ESC-001 — Alta de un correo y resultado del envío:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Notificaciones» → «Todas» y pulsa «Nueva notificación».
  3. En la ventana de elección de canal elige «Correo» y pulsa «Continuar».
  4. En el formulario de alta del correo elige el centro «CIPFP Mislata», rellena el motivo «Aviso de prueba de correo», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Reunión de inicio de curso» y el cuerpo «La reunión será el lunes a las 10:00.».
  5. Pulsa «Guardar».
  6. El sistema vuelve al listado y muestra la notificación con el tipo «Correo», el motivo «Aviso de prueba de correo», el destino «alumno1@mislata.es» y el centro «CIPFP Mislata».
  7. Pasados unos segundos, el administrador recarga el listado.
  8. Si el envío ha funcionado: la notificación está en estado «Enviado» y tiene fecha de envío.
  9. Si no: la notificación está en estado «Fallido» y sin fecha de envío.
  10. Si está «Fallido»: el administrador pulsa la fila «Aviso de prueba de correo».
  11. Si está «Fallido»: el sistema muestra en el panel «Datos del envío» la descripción del último fallo rellena.
- ESC-002 — Alta de un correo con un adjunto:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena el centro «CIPFP Mislata», el motivo «Envío de horario», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
  4. En el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «horario.pdf», sube el fichero «horario.pdf» y pulsa «Guardar» en la ventana del adjunto.
  5. Pulsa «Guardar» en el formulario del correo.
  6. El sistema vuelve al listado y muestra la notificación «Envío de horario» con el tipo «Correo» y el destino «alumno1@mislata.es».
  7. El administrador pulsa la fila «Envío de horario».
  8. El sistema muestra el correo con el adjunto «horario.pdf» en el panel «Adjuntos».
- ESC-003 — Cancelar la elección de canal:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas» y pulsa «Nueva notificación».
  3. En la ventana de elección de canal pulsa «Cancelar».
  4. El sistema cierra la ventana, vuelve al listado y no crea ninguna notificación.
- ESC-004 — Alta de un correo sin motivo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena el centro «CIPFP Mislata», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Aviso» y el cuerpo «Texto», y deja el motivo vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «El motivo es obligatorio» y no crea la notificación.
- ESC-005 — Alta de un correo con un motivo demasiado largo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero con un motivo de 256 caracteres (la letra «a» repetida 256 veces).
  4. Pulsa «Guardar».
  5. El sistema muestra «El motivo no puede superar 255 caracteres» y no crea la notificación.
- ESC-006 — Alta de un correo con varias direcciones en el «para»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero con el «para» «alumno1@mislata.es, alumno2@mislata.es».
  4. Pulsa «Guardar».
  5. El sistema muestra «El «para» debe contener una sola dirección de correo; use «en copia» para añadir más destinatarios» y no crea la notificación.
- ESC-007 — Alta de un correo sin el DNI del destinatario:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero deja el DNI vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «El DNI del destinatario es obligatorio» y no crea la notificación.
- ESC-008 — Alta de un correo con el DNI del destinatario no válido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero con el DNI «86862719A» (letra incorrecta).
  4. Pulsa «Guardar».
  5. El sistema muestra «El DNI del destinatario no es válido; compruebe la letra» y no crea la notificación.
- ESC-009 — Alta de un correo sin el nombre:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero deja el nombre vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «El nombre es obligatorio» y no crea la notificación.
- ESC-010 — Alta de un correo sin los apellidos:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero deja los apellidos vacíos.
  4. Pulsa «Guardar».
  5. El sistema muestra «Los apellidos son obligatorios» y no crea la notificación.
- ESC-011 — Alta de un correo sin destinatario en el «para»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero deja el «para» vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «Debe indicar al menos un destinatario en el «para»» y no crea la notificación.
- ESC-012 — Alta de un correo con el «para» de formato no válido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero con el «para» «alumno1-mislata.es».
  4. Pulsa «Guardar».
  5. El sistema muestra «El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)» y no crea la notificación.
- ESC-013 — Alta de un correo con el «en copia» de formato no válido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001 y además el «en copia» «familiar1-mislata.es».
  4. Pulsa «Guardar».
  5. El sistema muestra «El «en copia» debe contener direcciones de correo válidas» y no crea la notificación.
- ESC-014 — Alta de un correo con el «en copia oculta» de formato no válido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001 y además el «en copia oculta» «familiar1-mislata.es».
  4. Pulsa «Guardar».
  5. El sistema muestra «El «en copia oculta» debe contener direcciones de correo válidas» y no crea la notificación.
- ESC-015 — Alta de un correo sin asunto:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero deja el asunto vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «El asunto es obligatorio» y no crea la notificación.
- ESC-016 — Alta de un correo con el asunto demasiado largo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero con un asunto de 256 caracteres (la letra «a» repetida 256 veces).
  4. Pulsa «Guardar».
  5. El sistema muestra «El asunto no puede superar 255 caracteres» y no crea la notificación.
- ESC-017 — Alta de un correo sin cuerpo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero deja el cuerpo vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «El cuerpo es obligatorio» y no crea la notificación.
- ESC-018 — Alta de un correo sin centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001, pero no elige ningún centro.
  4. Pulsa «Guardar».
  5. El sistema muestra «El centro es obligatorio» y no crea la notificación.
- ESC-019 — Añadir un adjunto sin nombre de fichero:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. En el panel «Adjuntos» pulsa «Añadir adjunto», sube el fichero «horario.pdf» y deja vacío el nombre de fichero.
  4. Pulsa «Guardar» en la ventana del adjunto.
  5. El sistema muestra «El nombre del fichero es obligatorio» y no añade el adjunto.
- ESC-020 — Añadir un adjunto sin contenido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. En el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «horario.pdf» y no sube ningún fichero.
  4. Pulsa «Guardar» en la ventana del adjunto.
  5. El sistema muestra «Debe adjuntar el fichero» y no añade el adjunto.
- ESC-021 — Alta de un correo con dos adjuntos con el mismo nombre:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001.
  4. En el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «horario.pdf», sube el fichero «horario.pdf» y pulsa «Guardar» en la ventana del adjunto.
  5. Pulsa de nuevo «Añadir adjunto», escribe el nombre de fichero «horario.pdf», sube el fichero «horario.pdf» y pulsa «Guardar» en la ventana del adjunto.
  6. Pulsa «Guardar» en el formulario del correo.
  7. El sistema muestra «Ya existe un adjunto con ese nombre en el correo» y no crea la notificación.
- ESC-022 — Alta de un correo con un adjunto cuyo nombre contiene una barra:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-001.
  4. En el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «cursos/horario.pdf», sube el fichero «horario.pdf» y pulsa «Guardar» en la ventana del adjunto.
  5. El sistema muestra en la ventana del adjunto «El nombre del fichero no puede contener los caracteres / \ ni caracteres de control» y no añade el adjunto.
- ESC-058 — La elección de canal se abre sin canal y no deja continuar hasta elegir uno:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Notificaciones» → «Todas» y pulsa «Nueva notificación».
  3. El sistema abre la ventana de elección de canal sin ningún canal elegido y con el botón «Continuar» no disponible.
  4. El administrador elige «Correo».
  5. El sistema deja disponible el botón «Continuar».
  6. El administrador pulsa «Continuar».
  7. El sistema abre el formulario de alta del correo.
- ESC-059 — Formulario de alta del correo y cancelar el alta:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. El sistema abre el formulario de alta del correo con el tipo de notificación «Correo» en solo lectura, sin el panel «Datos del envío», con los botones «Guardar» y «Cancelar» y sin los botones «Salir» ni «Reenviar».
  4. El administrador elige el centro «CIPFP Mislata» y rellena el motivo «Correo cancelado», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Aviso cancelado» y el cuerpo «Texto».
  5. Pulsa «Cancelar».
  6. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Correo cancelado».
- ESC-061 — Descartar y quitar adjuntos durante el alta del correo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Correo con adjunto quitado», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
  4. En el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «borrador.pdf», sube el fichero «horario.pdf» y pulsa «Cancelar» en la ventana del adjunto.
  5. El sistema cierra la ventana y el panel «Adjuntos» no muestra «borrador.pdf».
  6. Pulsa «Añadir adjunto», escribe el nombre de fichero «horario.pdf», sube el fichero «horario.pdf» y pulsa «Guardar» en la ventana del adjunto.
  7. Pulsa «Añadir adjunto», escribe el nombre de fichero «notas.pdf», sube el fichero «horario.pdf» y pulsa «Guardar» en la ventana del adjunto.
  8. Pulsa la fila «notas.pdf» del panel «Adjuntos» y, en la ventana del adjunto, pulsa «Borrar».
  9. El panel «Adjuntos» muestra «horario.pdf» y no muestra «notas.pdf».
  10. Pulsa «Guardar» en el formulario del correo.
  11. En el listado pulsa la fila «Correo con adjunto quitado».
  12. El sistema muestra en el panel «Adjuntos» solo «horario.pdf» y no muestra el botón «Añadir adjunto».
- ESC-062 — Alta de un correo con un adjunto de más de 10 MB:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Adjunto grande», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
  4. En el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «grande.pdf», intenta subir un fichero «grande.pdf» de 11 MB.
  5. El sistema avisa de que no se puede subir un fichero de más de 10 MB y no lo adjunta.
- ESC-063 — Alta de un correo con varias direcciones en copia y en copia oculta:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Correo con copias», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el «en copia» «familiar1@mislata.es, familiar2@mislata.es», el «en copia oculta» «supervisor1@mislata.es», el asunto «Aviso con copias» y el cuerpo «Texto».
  4. Pulsa «Guardar».
  5. El sistema vuelve al listado y muestra «Correo con copias» con el destino «alumno1@mislata.es».
  6. El administrador pulsa la fila «Correo con copias».
  7. El sistema muestra el «en copia» «familiar1@mislata.es, familiar2@mislata.es» y el «en copia oculta» «supervisor1@mislata.es».
- ESC-070 — Alta de un correo ligado al estado de un expediente del mismo centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin», abre «Criptografía» → «Certificados digitales», da de alta el certificado de firma del director (DNI «85432016B», «Ruta classpath» «firma/mi_certificado.p12», contraseña «nadanada») y cierra sesión; después el profesor «director@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo en el centro «CIPFP Mislata».
  3. En el panel «Datos de la falta» elige el tipo de jornada faltada «Un día completo», rellena «Fecha» con la fecha de hoy y «Motivo falta» con «Traslado de domicilio», y adjunta el fichero «justificante.pdf» en «Foto o PDF del justificante».
  4. Pulsa «Siguiente».
  5. El sistema muestra la solicitud generada en PDF y la cabecera del expediente muestra «Entrada» y «Pendiente de presentación».
  6. Pulsa «Firmar y Presentar la solicitud» y confirma el aviso de que no podrá deshacer la acción; el sistema firma la solicitud en el servidor con el certificado del director.
  7. El sistema deja el expediente en «Verificación» / «Pendiente de verificación».
  8. El director anota el número del expediente.
  9. El director cierra sesión.
  10. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  11. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  12. Elige el centro «CIPFP Mislata» y rellena el motivo «Aviso ligado a expediente», el DNI «85432016B», el nombre «Director», los apellidos «CIPFP Mislata», el «para» «director@mislata.es», el asunto «Su expediente» y el cuerpo «Texto».
  13. En el estado del expediente elige el estado «Pendiente de verificación» del expediente con el número que anotó el director.
  14. Pulsa «Guardar».
  15. El sistema vuelve al listado y muestra «Aviso ligado a expediente» con el expediente del trámite que presentó el director.
- ESC-071 — En el alta solo se ofrecen estados de expedientes del centro elegido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin», abre «Criptografía» → «Certificados digitales», da de alta el certificado de firma del director (DNI «85432016B», «Ruta classpath» «firma/mi_certificado.p12», contraseña «nadanada») y cierra sesión; después el profesor «director@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo en el centro «CIPFP Mislata».
  3. En el panel «Datos de la falta» elige el tipo de jornada faltada «Un día completo», rellena «Fecha» con la fecha de hoy y «Motivo falta» con «Traslado de domicilio», y adjunta el fichero «justificante.pdf» en «Foto o PDF del justificante».
  4. Pulsa «Siguiente».
  5. El sistema muestra la solicitud generada en PDF y la cabecera del expediente muestra «Entrada» y «Pendiente de presentación».
  6. Pulsa «Firmar y Presentar la solicitud» y confirma el aviso de que no podrá deshacer la acción; el sistema firma la solicitud en el servidor con el certificado del director.
  7. El sistema deja el expediente en «Verificación» / «Pendiente de verificación».
  8. El director anota el número del expediente.
  9. El director cierra sesión.
  10. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  11. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  12. Elige el centro «CIPFP Batoi» y rellena el motivo «Aviso mal ligado», el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el «para» «alumno1@batoi.es», el asunto «Aviso» y el cuerpo «Texto».
  13. Abre el selector del estado del expediente y busca el número que anotó el director.
  14. El sistema no ofrece ningún estado del expediente de «CIPFP Mislata» con ese número.
  15. El administrador cambia el centro a «CIPFP Mislata», abre de nuevo el selector y elige el estado «Pendiente de verificación» del expediente con el número anotado.
  16. El administrador cambia el centro a «CIPFP Batoi».
  17. El sistema vacía el estado del expediente elegido.

## HU-002 — Como Administrador quiero dar de alta un SMS para avisar puntualmente a una persona en su móvil

- ESC-023 — Alta de un SMS y resultado del envío:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas» y pulsa «Nueva notificación».
  3. En la ventana de elección de canal elige «SMS» y pulsa «Continuar».
  4. En el formulario de alta del SMS elige el centro «CIPFP Mislata», rellena el motivo «Aviso de prueba de SMS», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  5. Pulsa «Guardar».
  6. El sistema vuelve al listado y muestra la notificación con el tipo «SMS», el motivo «Aviso de prueba de SMS», el destino «+34600111222» (el teléfono en formato internacional) y el centro «CIPFP Mislata».
  7. Pasados unos segundos, el administrador recarga el listado.
  8. Si el envío ha funcionado: la notificación está en estado «Enviado» y tiene fecha de envío.
  9. Si no: la notificación está en estado «Fallido», sin fecha de envío, y al abrirla muestra la descripción del fallo.
- ESC-024 — Alta de un SMS con el teléfono escrito ya en formato internacional:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero con el teléfono «+34 600 111 222».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y el listado muestra el destino «+34600111222».
- ESC-025 — Alta de un SMS sin el DNI del destinatario:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero deja el DNI vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «El DNI del destinatario es obligatorio» y no crea la notificación.
  6. El administrador pulsa «Cancelar».
  7. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».
- ESC-026 — Alta de un SMS con el DNI del destinatario no válido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero con el DNI «86862719A».
  4. Pulsa «Guardar».
  5. El sistema muestra «El DNI del destinatario no es válido; compruebe la letra» y no crea la notificación.
  6. El administrador pulsa «Cancelar».
  7. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».
- ESC-027 — Alta de un SMS sin nombre ni apellidos:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero deja vacíos el nombre y los apellidos.
  4. Pulsa «Guardar».
  5. El sistema muestra «El nombre es obligatorio» y «Los apellidos son obligatorios», y no crea la notificación.
  6. El administrador pulsa «Cancelar».
  7. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».
- ESC-028 — Alta de un SMS sin teléfono:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero deja el teléfono vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «El teléfono es obligatorio» y no crea la notificación.
  6. El administrador pulsa «Cancelar».
  7. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».
- ESC-029 — Alta de un SMS con un teléfono que no es un móvil de España:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero con el teléfono «961234567» (un fijo).
  4. Pulsa «Guardar».
  5. El sistema muestra «El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)» y no crea la notificación.
  6. El administrador pulsa «Cancelar».
  7. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».
- ESC-030 — Alta de un SMS sin mensaje:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero deja el mensaje vacío.
  4. Pulsa «Guardar».
  5. El sistema muestra «El mensaje es obligatorio» y no crea la notificación.
  6. El administrador pulsa «Cancelar».
  7. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».
- ESC-031 — Mensaje sin acentos en el límite de un SMS:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero con un mensaje de 161 caracteres sin acentos (la letra «a» repetida 161 veces), y pulsa «Guardar».
  4. El sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no crea la notificación.
  5. El administrador borra un carácter del mensaje (queda la letra «a» repetida 160 veces) y pulsa «Guardar».
  6. El sistema guarda el SMS, vuelve al listado y muestra la notificación con el tipo «SMS», el motivo «Aviso de prueba de SMS», el destino «+34600111222» y el centro «CIPFP Mislata».
- ESC-032 — Mensaje con acentos en el límite de un SMS:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero con un mensaje de 71 caracteres con acentos (la letra «á» repetida 71 veces), y pulsa «Guardar».
  4. El sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no crea la notificación.
  5. El administrador borra un carácter del mensaje (queda la letra «á» repetida 70 veces) y pulsa «Guardar».
  6. El sistema guarda el SMS, vuelve al listado y muestra la notificación con el tipo «SMS», el motivo «Aviso de prueba de SMS», el destino «+34600111222» y el centro «CIPFP Mislata».
- ESC-033 — Alta de un SMS sin centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Rellena todos los datos como en ESC-023, pero no elige ningún centro.
  4. Pulsa «Guardar».
  5. El sistema muestra «El centro es obligatorio» y no crea la notificación.
  6. El administrador pulsa «Cancelar».
  7. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».
- ESC-060 — Formulario de alta del SMS y cancelar el alta:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. El sistema abre el formulario de alta del SMS con el tipo de notificación «SMS» en solo lectura, sin el panel «Datos del envío», con los botones «Guardar» y «Cancelar» y sin los botones «Salir» ni «Reenviar».
  4. El administrador elige el centro «CIPFP Mislata» y rellena el motivo «SMS cancelado», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  5. Pulsa «Cancelar».
  6. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «SMS cancelado».
- ESC-066 — Alta de un SMS con un mensaje solo de espacios:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Aviso de prueba de SMS», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y, como mensaje, cinco espacios en blanco.
  4. Pulsa «Guardar».
  5. El sistema muestra «El mensaje es obligatorio» y no crea la notificación.
  6. El administrador pulsa «Cancelar».
  7. El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

## HU-003 — Como Administrador quiero consultar en un único listado todas las notificaciones de todos los centros para supervisar los envíos

- ESC-034 — Listado común con correos y SMS de varios centros:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» con el motivo «Correo Mislata» y el «para» «alumno1@mislata.es».
  3. Da de alta, como en ESC-023, un SMS del centro «CIPFP Batoi» con el motivo «SMS Batoi», el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi» y el teléfono «600222333».
  4. Abre «Notificaciones» → «Todas».
  5. El sistema muestra las dos notificaciones en el mismo listado: «Correo Mislata» con el tipo «Correo», el destino «alumno1@mislata.es» y el centro «CIPFP Mislata»; y «SMS Batoi» con el tipo «SMS», el destino «+34600222333» y el centro «CIPFP Batoi».
- ESC-035 — Al abrir una fila se abre el formulario de su canal:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo con el motivo «Correo de prueba» y el asunto «Reunión de inicio de curso».
  3. Da de alta, como en ESC-023, un SMS con el motivo «SMS de prueba» y el mensaje «Mañana no hay clase».
  4. En «Notificaciones» → «Todas» pulsa la fila «Correo de prueba».
  5. El sistema abre el formulario del correo en solo lectura, que muestra el tipo de notificación «Correo», el motivo «Correo de prueba», el centro «CIPFP Mislata», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el «en copia» y el «en copia oculta» vacíos, el asunto «Reunión de inicio de curso», el cuerpo «La reunión será el lunes a las 10:00.» y el panel «Adjuntos» sin ningún adjunto.
  6. El administrador pulsa «Salir».
  7. El sistema vuelve al listado de «Todas».
  8. El administrador pulsa la fila «SMS de prueba».
  9. El sistema abre el formulario del SMS, que muestra el teléfono «+34600111222», el mensaje «Mañana no hay clase» y el motivo «SMS de prueba».
- ESC-036 — Datos del envío según el estado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-023, un SMS con el motivo «SMS estado».
  3. Pasados unos segundos, en «Notificaciones» → «Todas» pulsa la fila «SMS estado».
  4. El sistema muestra en «Datos del envío» el estado, el número de reintentos «1», la fecha de creación y las fechas del primer y del último intento.
  5. Si el estado es «Enviado»: muestra la fecha de envío y no muestra la descripción del último fallo.
  6. Si el estado es «Fallido»: muestra la descripción del último fallo y no muestra la fecha de envío.
- ESC-037 — Una notificación ya creada no se puede modificar ni borrar:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo con el motivo «Correo inmutable».
  3. En «Notificaciones» → «Todas» pulsa la fila «Correo inmutable».
  4. El sistema abre el formulario del correo con el motivo «Correo inmutable», el centro «CIPFP Mislata», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Reunión de inicio de curso» y el cuerpo «La reunión será el lunes a las 10:00.», todos en solo lectura.
  5. El formulario muestra el botón «Salir» y no muestra los botones «Guardar», «Cancelar» ni «Borrar».
- ESC-038 — Buscar solo los SMS en el listado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo con el motivo «Correo búsqueda».
  3. Da de alta, como en ESC-023, un SMS con el motivo «SMS búsqueda».
  4. En «Notificaciones» → «Todas» filtra la columna tipo por «SMS».
  5. El sistema muestra «SMS búsqueda» y no muestra «Correo búsqueda».
- ESC-067 — Consultar y descargar un adjunto de un correo guardado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Envío de horario», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
  4. En el panel «Adjuntos» pulsa «Añadir adjunto».
  5. En la ventana del adjunto escribe el nombre de fichero «horario.pdf».
  6. Sube el fichero «horario.pdf».
  7. Pulsa «Guardar» en la ventana del adjunto.
  8. Pulsa «Guardar» en el formulario del correo.
  9. El sistema vuelve al listado y muestra la notificación «Envío de horario».
  10. En el listado pulsa la fila «Envío de horario».
  11. El sistema abre el formulario del correo y muestra en el panel «Adjuntos» la fila «horario.pdf».
  12. En el panel «Adjuntos» pulsa la fila «horario.pdf».
  13. El sistema abre el adjunto en solo lectura con el nombre de fichero «horario.pdf» y su fichero, sin los botones «Guardar» ni «Borrar».
  14. El administrador pulsa el fichero y lo descarga.
  15. Pulsa «Salir».
  16. El sistema cierra el adjunto y vuelve al formulario del correo «Envío de horario».
- ESC-068 — Un SMS ya creado no se puede modificar ni borrar:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «SMS inmutable», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema vuelve al listado y muestra la notificación «SMS inmutable».
  6. En el listado pulsa la fila «SMS inmutable».
  7. El sistema muestra todos los datos del SMS en solo lectura, con el botón «Salir» y sin los botones «Guardar», «Cancelar» ni «Borrar».
- ESC-069 — El listado muestra primero las notificaciones más recientes:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Primer aviso», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Primero» y el cuerpo «Texto».
  4. Pulsa «Guardar».
  5. El sistema vuelve al listado y muestra la notificación «Primer aviso».
  6. Pulsa «Nueva notificación».
  7. En la ventana de elección de canal elige «Correo» y pulsa «Continuar».
  8. Elige el centro «CIPFP Mislata» y rellena el motivo «Segundo aviso», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Segundo» y el cuerpo «Texto».
  9. Pulsa «Guardar».
  10. El sistema muestra en el listado la fila «Segundo aviso» por encima de la fila «Primer aviso».
- ESC-076 — El administrador ve «Recibidas» y «Todas» pero no «Del centro»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Notificaciones».
  3. El sistema muestra los submenús «Recibidas» y «Todas» y no muestra «Del centro».

## HU-004 — Como Administrador quiero reenviar una notificación que ha fallado para que llegue a su destinatario

- ESC-039 — Reenvío de un correo desde «Todas»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo con el motivo «Correo a reenviar».
  3. Pasados unos segundos, en «Notificaciones» → «Todas» pulsa la fila «Correo a reenviar».
  4. Si el correo está en estado «Fallido»: el sistema muestra el botón «Reenviar».
  5. Si está en «Fallido»: el administrador pulsa «Reenviar».
  6. Si está en «Fallido»: el sistema muestra «El reenvío del correo se ha puesto en marcha.».
  7. Si está en «Fallido»: pasados unos segundos, el administrador recarga el formulario del correo «Correo a reenviar».
  8. Si está en «Fallido»: el correo muestra el número de reintentos «2» y está en estado «Enviado» con fecha de envío, o de nuevo en estado «Fallido» con la descripción del nuevo fallo.
  9. Si está en estado «Enviado»: el sistema no muestra el botón «Reenviar».
- ESC-040 — Reenvío de un SMS desde «Todas»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-023, un SMS con el motivo «SMS a reenviar».
  3. Pasados unos segundos, en «Notificaciones» → «Todas» pulsa la fila «SMS a reenviar».
  4. Si el SMS está en estado «Fallido»: el sistema muestra el botón «Reenviar».
  5. Si está en «Fallido»: el administrador pulsa «Reenviar».
  6. Si está en «Fallido»: el sistema pide confirmación con el aviso «Reenviar un SMS tiene coste. ¿Desea reenviarlo?» y el administrador pulsa «Aceptar».
  7. Si está en «Fallido»: el sistema muestra «El reenvío del SMS se ha puesto en marcha.».
  8. Si está en «Fallido»: pasados unos segundos, el administrador recarga el formulario del SMS «SMS a reenviar».
  9. Si está en «Fallido»: el SMS muestra el número de reintentos «2» y está en estado «Enviado» con fecha de envío, o de nuevo en estado «Fallido» con la descripción del nuevo fallo.
  10. Si está en estado «Enviado»: el sistema no muestra el botón «Reenviar».

## HU-005 — Como Gestor del centro quiero consultar las notificaciones de mis centros para saber qué avisos se han enviado y cuáles han fallado

- ESC-041 — El supervisor solo ve las notificaciones de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» con el motivo «Aviso Mislata».
  3. Da de alta, como en ESC-001, un correo del centro «CIPFP Batoi» con el motivo «Aviso Batoi», el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi» y el «para» «alumno1@batoi.es».
  4. El administrador cierra sesión.
  5. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  6. Abre «Notificaciones» → «Del centro».
  7. El sistema muestra la fila con el motivo «Aviso Mislata», el tipo «Correo», el destino «alumno1@mislata.es» y el centro «CIPFP Mislata», y no muestra ninguna fila con el motivo «Aviso Batoi».
- ESC-042 — El administrativo ve las notificaciones de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-023, un SMS del centro «CIPFP Mislata» con el motivo «SMS Mislata».
  3. El administrador cierra sesión.
  4. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  5. Abre «Notificaciones» → «Del centro».
  6. El sistema muestra «SMS Mislata».
- ESC-043 — El director ve las notificaciones de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» con el motivo «Aviso para dirección».
  3. Da de alta, como en ESC-041, un correo del centro «CIPFP Batoi» con el motivo «Aviso Batoi», el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi» y el «para» «alumno1@batoi.es».
  4. El administrador cierra sesión.
  5. El director «director@mislata.es» inicia sesión con contraseña «demo1234».
  6. Abre «Notificaciones» → «Del centro».
  7. El sistema muestra «Aviso para dirección» y no muestra «Aviso Batoi».
- ESC-044 — El jefe de estudios ve las notificaciones de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-023, un SMS del centro «CIPFP Mislata» con el motivo «Aviso para jefatura».
  3. El administrador cierra sesión.
  4. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  5. Abre «Notificaciones» → «Del centro».
  6. El sistema muestra «Aviso para jefatura».
- ESC-045 — El secretario ve las notificaciones de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» con el motivo «Aviso para secretaría».
  3. El administrador cierra sesión.
  4. El secretario «secretario@mislata.es» inicia sesión con contraseña «demo1234».
  5. Abre «Notificaciones» → «Del centro».
  6. El sistema muestra «Aviso para secretaría».
- ESC-046 — El supervisor de dos centros ve las de ambos y filtra por centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» con el motivo «Aviso Mislata».
  3. Da de alta, como en ESC-041, un correo del centro «CIPFP Batoi» con el motivo «Aviso Batoi», el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi» y el «para» «alumno1@batoi.es».
  4. El administrador cierra sesión.
  5. El supervisor «supervisordoscentros@mislata.es» inicia sesión con contraseña «demo1234».
  6. Abre «Notificaciones» → «Del centro».
  7. El sistema muestra «Aviso Mislata» con el centro «CIPFP Mislata» y «Aviso Batoi» con el centro «CIPFP Batoi».
  8. El supervisor filtra la columna centro por «CIPFP Batoi».
  9. El sistema muestra «Aviso Batoi» y no muestra «Aviso Mislata».
- ESC-047 — Un usuario sin tipo ni cargo de gestión no ve «Del centro»:
  1. El vicesecretario «vicesecretario@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Notificaciones».
  3. El sistema muestra el submenú «Recibidas» y no muestra «Del centro» ni «Todas».
  4. El vicesecretario cierra sesión.
  5. El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234».
  6. Abre el menú «Notificaciones».
  7. El sistema muestra el submenú «Recibidas» y no muestra «Del centro» ni «Todas».
- ESC-048 — El gestor del centro no puede dar de alta notificaciones:
  1. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Notificaciones».
  3. El sistema no muestra el submenú «Todas».
  4. El supervisor abre «Notificaciones» → «Del centro».
  5. El sistema no muestra el botón «Nueva notificación».
- ESC-049 — El gestor abre una notificación y ve el formulario de su canal con sus adjuntos:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-002, un correo del centro «CIPFP Mislata» con el motivo «Envío de horario» y el adjunto «horario.pdf».
  3. El administrador cierra sesión.
  4. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  5. Abre «Notificaciones» → «Del centro».
  6. Pulsa la fila con el motivo «Envío de horario».
  7. El sistema abre el formulario del correo en solo lectura, sin los botones «Guardar» ni «Borrar», con el motivo «Envío de horario», el centro «CIPFP Mislata», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Horario», el cuerpo «Le adjuntamos su horario.» y, en el panel «Adjuntos», la fila «horario.pdf».
  8. En el panel «Adjuntos» el supervisor pulsa la fila «horario.pdf».
  9. El sistema abre el adjunto en solo lectura con el nombre de fichero «horario.pdf» y su fichero, sin los botones «Guardar» ni «Borrar».
  10. El supervisor pulsa el fichero.
  11. El sistema descarga el fichero «horario.pdf».

## HU-006 — Como Gestor del centro quiero reenviar una notificación de mis centros que ha fallado para que llegue a su destinatario

- ESC-050 — El supervisor reenvía un correo fallido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» con el motivo «Correo a reenviar».
  3. El administrador cierra sesión.
  4. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  5. Espera unos segundos a que se haga el primer intento de envío.
  6. Abre «Notificaciones» → «Del centro».
  7. Pulsa la fila «Correo a reenviar».
  8. Si está en estado «Fallido»: el sistema muestra el botón «Reenviar».
  9. Si estaba en «Fallido»: el supervisor pulsa «Reenviar».
  10. Si estaba en «Fallido»: el sistema muestra «El reenvío del correo se ha puesto en marcha.».
  11. Si estaba en «Fallido»: pasados unos segundos, el supervisor recarga el formulario del correo.
  12. Si estaba en «Fallido»: el sistema muestra en «Datos del envío» el número de reintentos «2» y el estado «Enviado» (con fecha de envío) o, de nuevo, «Fallido» (con la descripción del nuevo fallo).
  13. Si está en estado «Enviado»: el sistema no muestra el botón «Reenviar».
- ESC-051 — El director reenvía un SMS fallido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-023, un SMS del centro «CIPFP Mislata» con el motivo «SMS a reenviar».
  3. El administrador cierra sesión.
  4. El director «director@mislata.es» inicia sesión con contraseña «demo1234».
  5. Espera unos segundos a que se haga el primer intento de envío.
  6. Abre «Notificaciones» → «Del centro».
  7. Pulsa la fila «SMS a reenviar».
  8. Si está en estado «Fallido»: el sistema muestra el botón «Reenviar».
  9. Si estaba en «Fallido»: el director pulsa «Reenviar».
  10. Si está en «Fallido»: el sistema pide confirmación con el aviso «Reenviar un SMS tiene coste. ¿Desea reenviarlo?» y el director pulsa «Aceptar».
  11. Si estaba en «Fallido»: el sistema muestra «El reenvío del SMS se ha puesto en marcha.».
  12. Si estaba en «Fallido»: pasados unos segundos, el director recarga el formulario del SMS.
  13. Si estaba en «Fallido»: el sistema muestra en «Datos del envío» el número de reintentos «2» y el estado «Enviado» (con fecha de envío) o, de nuevo, «Fallido» (con la descripción del nuevo fallo).
  14. Si está en estado «Enviado»: el sistema no muestra el botón «Reenviar».
- ESC-072 — El jefe de estudios reenvía un correo fallido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Correo para jefatura», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Aviso» y el cuerpo «Texto».
  4. Pulsa «Guardar».
  5. El administrador cierra sesión.
  6. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  7. Espera unos segundos a que se haga el primer intento de envío.
  8. Abre «Notificaciones» → «Del centro».
  9. Pulsa la fila «Correo para jefatura».
  10. Si está en estado «Fallido»: el sistema muestra el botón «Reenviar».
  11. Si estaba en «Fallido»: el jefe de estudios pulsa «Reenviar».
  12. Si estaba en «Fallido»: el sistema muestra «El reenvío del correo se ha puesto en marcha.».
  13. Si estaba en «Fallido»: pasados unos segundos, el jefe de estudios recarga el formulario del correo.
  14. Si estaba en «Fallido»: el sistema muestra en «Datos del envío» el número de reintentos «2» y el estado «Enviado» (con fecha de envío) o, de nuevo, «Fallido» (con la descripción del nuevo fallo).
  15. Si está en estado «Enviado»: el sistema no muestra el botón «Reenviar».
- ESC-073 — El secretario reenvía un SMS fallido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «SMS para secretaría», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El administrador cierra sesión.
  6. El secretario «secretario@mislata.es» inicia sesión con contraseña «demo1234».
  7. Espera unos segundos a que se haga el primer intento de envío.
  8. Abre «Notificaciones» → «Del centro».
  9. Pulsa la fila «SMS para secretaría».
  10. Si está en estado «Fallido»: el sistema muestra el botón «Reenviar».
  11. Si estaba en «Fallido»: el secretario pulsa «Reenviar».
  12. Si está en «Fallido»: el sistema pide confirmación con el aviso «Reenviar un SMS tiene coste. ¿Desea reenviarlo?» y el secretario pulsa «Aceptar».
  13. Si estaba en «Fallido»: el sistema muestra «El reenvío del SMS se ha puesto en marcha.».
  14. Si estaba en «Fallido»: pasados unos segundos, el secretario recarga el formulario del SMS.
  15. Si estaba en «Fallido»: el sistema muestra en «Datos del envío» el número de reintentos «2» y el estado «Enviado» (con fecha de envío) o, de nuevo, «Fallido» (con la descripción del nuevo fallo).
  16. Si está en estado «Enviado»: el sistema no muestra el botón «Reenviar».
- ESC-074 — El administrativo reenvía un correo fallido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Correo para administración», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Aviso» y el cuerpo «Texto».
  4. Pulsa «Guardar».
  5. El administrador cierra sesión.
  6. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  7. Espera unos segundos a que se haga el primer intento de envío.
  8. Abre «Notificaciones» → «Del centro».
  9. Pulsa la fila «Correo para administración».
  10. Si está en estado «Fallido»: el sistema muestra el botón «Reenviar».
  11. Si estaba en «Fallido»: el administrativo pulsa «Reenviar».
  12. Si estaba en «Fallido»: el sistema muestra «El reenvío del correo se ha puesto en marcha.».
  13. Si estaba en «Fallido»: pasados unos segundos, el administrativo recarga el formulario del correo.
  14. Si estaba en «Fallido»: el sistema muestra en «Datos del envío» el número de reintentos «2» y el estado «Enviado» (con fecha de envío) o, de nuevo, «Fallido» (con la descripción del nuevo fallo).
  15. Si está en estado «Enviado»: el sistema no muestra el botón «Reenviar».

## HU-007 — Como Destinatario quiero consultar las notificaciones que he recibido para tener a mano los avisos del centro

- ESC-052 — El destinatario ve sus correos y SMS enviados, sin el motivo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» para el DNI «86862719E» con el motivo «Motivo interno correo» y el asunto «Reunión de inicio de curso».
  3. Da de alta, como en ESC-023, un SMS del centro «CIPFP Mislata» para el DNI «86862719E» con el motivo «Motivo interno SMS».
  4. El administrador cierra sesión.
  5. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre «Notificaciones» → «Recibidas».
  6. Para cada una de las dos notificaciones: si está en estado «Enviado», el sistema la muestra en «Recibidas»; el correo con el tipo «Correo», el destino «alumno1@mislata.es», el expediente vacío y su fecha de envío; y el SMS con el tipo «SMS», el destino «+34600111222», el expediente vacío y su fecha de envío.
  7. Si alguna de las dos está en estado «Fallido»: el sistema no la muestra en «Recibidas».
  8. El sistema no muestra en ninguna fila los textos «Motivo interno correo» ni «Motivo interno SMS».
- ESC-053 — El destinatario abre un correo recibido y descarga su adjunto:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-002, un correo del centro «CIPFP Mislata» para el DNI «86862719E» con el motivo «Envío de horario», el asunto «Horario» y el adjunto «horario.pdf».
  3. El administrador cierra sesión.
  4. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre «Notificaciones» → «Recibidas».
  5. Si el correo está en estado «Enviado»: el alumno pulsa la fila del correo en «Recibidas».
  6. Si está «Enviado»: el sistema abre el formulario del correo en solo lectura con el asunto «Horario», el cuerpo «Le adjuntamos su horario.», el «para» «alumno1@mislata.es», sin el campo «en copia» (el correo no tiene copias), la fecha de envío y el adjunto «horario.pdf» en el panel «Adjuntos», sin el texto «Envío de horario» y sin el botón «Reenviar».
  7. Si está «Enviado»: el alumno pulsa la fila «horario.pdf» del panel «Adjuntos» y descarga el fichero «horario.pdf».
  8. Si no: el sistema no muestra el correo en «Recibidas».
- ESC-054 — El destinatario no ve las notificaciones enviadas a otra persona:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» para el DNI «03532821K» (el de «alumno2@mislata.es») con el nombre «Alumno2», el «para» «alumno2@mislata.es» y el asunto «Aviso para Alumno2».
  3. El administrador cierra sesión.
  4. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre «Notificaciones» → «Recibidas».
  5. El sistema no muestra el correo «Aviso para Alumno2».
  6. El alumno «alumno1@mislata.es» cierra sesión.
  7. El alumno «alumno2@mislata.es» inicia sesión con contraseña «demo1234» y abre «Notificaciones» → «Recibidas».
  8. Si el correo está en estado «Enviado»: el sistema muestra el correo con el tipo «Correo» y el destino «alumno2@mislata.es».
  9. Si no: el sistema no lo muestra.
- ESC-055 — El destinatario ve una notificación enviada desde otro centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-023, un SMS del centro «CIPFP Batoi» para el DNI «86862719E» (el de «alumno1@mislata.es») con el motivo «SMS desde Batoi».
  3. El administrador cierra sesión.
  4. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre «Notificaciones» → «Recibidas».
  5. Si el SMS está en estado «Enviado»: el sistema lo muestra con el tipo «SMS» y el destino «+34600111222».
  6. Si no: el sistema no lo muestra.
- ESC-056 — Un familiar consulta sus notificaciones recibidas:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Da de alta, como en ESC-001, un correo del centro «CIPFP Mislata» para el DNI «43145636M» con el nombre «Familiar1», los apellidos «CIPFP Mislata», el «para» «familiar1@mislata.es» y el asunto «Aviso a familias».
  3. El administrador cierra sesión.
  4. El familiar «familiar1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre «Notificaciones» → «Recibidas».
  5. Si el correo está en estado «Enviado»: el sistema lo muestra con el tipo «Correo» y el destino «familiar1@mislata.es».
  6. Si no: el sistema no lo muestra.
- ESC-064 — El destinatario ve el «en copia» pero no el «en copia oculta» ni los datos del envío:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Correo con copia oculta», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el «en copia» «familiar1@mislata.es», el «en copia oculta» «supervisor1@mislata.es», el asunto «Aviso con copias» y el cuerpo «Texto», y pulsa «Guardar».
  4. El administrador cierra sesión.
  5. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre «Notificaciones» → «Recibidas».
  6. Si el correo está en estado «Enviado»: el alumno pulsa su fila y el sistema abre el formulario del correo con el asunto «Aviso con copias», el «para» «alumno1@mislata.es» y el «en copia» «familiar1@mislata.es», sin mostrar el «en copia oculta» ni el texto «supervisor1@mislata.es», ni el estado, el número de reintentos o la descripción del último fallo.
  7. Si no: el sistema no muestra el correo en «Recibidas».
- ESC-065 — El destinatario abre un SMS recibido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
  3. Elige el centro «CIPFP Mislata» y rellena el motivo «Motivo interno SMS abierto», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase», y pulsa «Guardar».
  4. El administrador cierra sesión.
  5. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre «Notificaciones» → «Recibidas».
  6. Si el SMS está en estado «Enviado»: el alumno pulsa su fila y el sistema abre el formulario del SMS en solo lectura con el teléfono «+34600111222», la fecha de envío y el mensaje «Mañana no hay clase», sin el texto «Motivo interno SMS abierto», sin el estado ni el número de reintentos y sin el botón «Reenviar»; el alumno pulsa «Salir» y el sistema vuelve al listado de notificaciones recibidas.
  7. Si no: el sistema no muestra el SMS en «Recibidas».

## HU-008 — Como Sistema quiero avisar por correo al solicitante cuando su solicitud tiene que subsanarse para que la corrija

- ESC-057 — El aviso de subsanación queda registrado con su motivo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin», abre «Criptografía» → «Certificados digitales», da de alta el certificado de firma del director (DNI «85432016B», «Ruta classpath» «firma/mi_certificado.p12», contraseña «nadanada») y cierra sesión; después el profesor «director@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo en el centro «CIPFP Mislata».
  3. En el panel «Datos de la falta» elige el tipo de jornada faltada «Un día completo», rellena «Fecha» con la fecha de hoy y «Motivo falta» con «Traslado de domicilio», y adjunta el fichero «justificante.pdf» en «Foto o PDF del justificante».
  4. Pulsa «Siguiente».
  5. El sistema muestra la solicitud generada en PDF y la cabecera del expediente muestra «Entrada» y «Pendiente de presentación».
  6. Pulsa «Firmar y Presentar la solicitud» y confirma el aviso de que no podrá deshacer la acción; el sistema firma la solicitud en el servidor con el certificado del director.
  7. El sistema deja el expediente en «Verificación» / «Pendiente de verificación».
  8. El director anota el número del expediente.
  9. El director cierra sesión.
  10. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  11. Abre «Tramitación» → «Pendientes de mí» y pulsa la fila del expediente con el número anotado.
  12. En «Resultado de la verificación» elige «Pedir subsanación» y rellena «Qué hay que subsanar» con «Falta el justificante firmado».
  13. Pulsa «Siguiente».
  14. El sistema devuelve el expediente a «Entrada» / «Entrada de datos».
  15. El jefe de estudios cierra sesión.
  16. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  17. El supervisor abre «Notificaciones» → «Del centro».
  18. El sistema muestra en el listado una notificación con el centro «CIPFP Mislata», el tipo «Correo», el DNI del destinatario «85432016B», el motivo «Subsanación del expediente <número anotado>», el destino «director@mislata.es» y, en la columna «expediente», el expediente de «Justificación de falta del profesorado» creado al principio del escenario.
  19. El supervisor pulsa la fila de esa notificación.
  20. El sistema abre el formulario de correo en solo lectura con el «para» «director@mislata.es», el asunto «Tiene que subsanar su solicitud del expediente <número anotado>» y un cuerpo que incluye el texto «Falta el justificante firmado».
- ESC-075 — El solicitante ve el aviso de subsanación en sus notificaciones recibidas:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin», abre «Criptografía» → «Certificados digitales», da de alta el certificado de firma del director (DNI «85432016B», «Ruta classpath» «firma/mi_certificado.p12», contraseña «nadanada») y cierra sesión; después el profesor «director@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo en el centro «CIPFP Mislata».
  3. En el panel «Datos de la falta» elige el tipo de jornada faltada «Un día completo», rellena «Fecha» con la fecha de hoy y «Motivo falta» con «Traslado de domicilio», y adjunta el fichero «justificante.pdf» en «Foto o PDF del justificante».
  4. Pulsa «Siguiente».
  5. El sistema muestra la solicitud generada en PDF y la cabecera del expediente muestra «Entrada» y «Pendiente de presentación».
  6. Pulsa «Firmar y Presentar la solicitud» y confirma el aviso de que no podrá deshacer la acción; el sistema firma la solicitud en el servidor con el certificado del director.
  7. El sistema deja el expediente en «Verificación» / «Pendiente de verificación».
  8. El director anota el número del expediente.
  9. El director cierra sesión.
  10. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  11. Abre «Tramitación» → «Pendientes de mí» y pulsa la fila del expediente con el número anotado.
  12. En «Resultado de la verificación» elige «Pedir subsanación» y rellena «Qué hay que subsanar» con «Falta el justificante firmado».
  13. Pulsa «Siguiente».
  14. El sistema devuelve el expediente a «Entrada» / «Entrada de datos».
  15. El jefe de estudios espera unos segundos, abre «Notificaciones» → «Del centro» y recarga el listado.
  16. Anota el estado («Enviado» o «Fallido») de la notificación con el motivo «Subsanación del expediente <número anotado>».
  17. El jefe de estudios cierra sesión.
  18. El director «director@mislata.es» inicia sesión con contraseña «demo1234».
  19. Abre «Notificaciones» → «Recibidas».
  20. Si el estado anotado es «Enviado»: el sistema muestra en el listado una fila con el tipo «Correo», el destino «director@mislata.es», en la columna «expediente» el expediente de «Justificación de falta del profesorado» creado al principio del escenario, y con fecha de envío.
  21. Si el estado anotado es «Enviado»: el director pulsa esa fila.
  22. Si el estado anotado es «Enviado»: el sistema abre el formulario de correo en solo lectura con el asunto «Tiene que subsanar su solicitud del expediente <número anotado>», un cuerpo que incluye «Falta el justificante firmado» y sin el texto «Subsanación del expediente <número anotado>».
  23. Si el estado anotado es «Fallido»: el sistema no muestra el aviso en «Recibidas».

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-Notificacion.md](./entity-Notificacion.md) | Notificacion | Un aviso enviado (o por enviar) a una persona por cualquier canal; reúne lo común a todos los canales |
| [entity-Correo.md](./entity-Correo.md) | Correo | Una notificación cuyo canal es el correo electrónico |
| [entity-Sms.md](./entity-Sms.md) | Sms | Una notificación cuyo canal es el SMS |
| [entity-Adjunto.md](./entity-Adjunto.md) | Adjunto | Un fichero que viaja adjunto a un correo |

- **Correo** y **Sms** son dos **clases de Notificacion**: todo correo y todo SMS es una notificación y tiene todos sus datos comunes, más los propios de su canal. Un canal nuevo será otra clase de notificación, sin cambiar las existentes.
- Una **Notificacion** pertenece siempre a un **centro** (de la gestión de centros) y puede ir ligada, de forma opcional, al **estado de un expediente** en el que se encontraba el expediente cuando se envió.
- Un **Correo** tiene cero o más **Adjuntos**. Los adjuntos pertenecen al correo (maestro-detalle): solo existen dentro de él y solo se añaden al darlo de alta. Los adjuntos cuelgan del correo, no de la notificación en general.
- Ni las notificaciones ni sus adjuntos se borran nunca, así que no hay borrado en cascada que aplicar.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-notificaciones-recibidas.md](./screen-notificaciones-recibidas.md) | Notificaciones recibidas | El destinatario consulta las notificaciones que se le han enviado con éxito, de cualquier canal y centro |
| [screen-notificaciones-centro.md](./screen-notificaciones-centro.md) | Notificaciones del centro | El gestor del centro consulta las notificaciones de sus centros y reenvía las fallidas |
| [screen-notificaciones-todas.md](./screen-notificaciones-todas.md) | Todas las notificaciones | El administrador consulta las notificaciones de todos los centros, da de alta correos y SMS y reenvía las fallidas |
| [screen-adjunto-consulta.md](./screen-adjunto-consulta.md) | Adjunto en consulta | Formulario compartido, en lectura, con el que cualquiera que vea un correo guardado consulta y descarga uno de sus adjuntos |

# Seguridad

- **Administrador:** ve todas las notificaciones (correos y SMS) y sus adjuntos de **todos los centros**, en cualquier estado; da de alta correos y SMS de cualquier centro; reenvía las fallidas de cualquier centro. No puede modificarlas ni borrarlas una vez creadas.
- **Gestor del centro** (Supervisor, Administrativo, Director, Jefe de estudios o Secretario): ve las notificaciones y sus adjuntos de los **centros en los que tiene ese tipo de usuario o ese cargo**, en cualquier estado, y reenvía las fallidas de esos centros. Si tiene el tipo o el cargo en varios centros, ve las de todos ellos; en un centro donde no lo tiene, no ve nada. No da de alta, no modifica ni borra notificaciones.
- **Destinatario** (cualquier usuario autenticado): ve **solo sus propias** notificaciones —las que llevan su DNI como DNI del destinatario— y solo cuando están en estado «Enviado», **de cualquier centro**, junto con los adjuntos de esos correos. No ve el motivo en pantalla (el motivo no es confidencial, solo no se le muestra). No da de alta, no modifica, no borra ni reenvía.
- **Sistema:** crea notificaciones durante la tramitación de expedientes, en nombre del usuario que tramita, con las mismas validaciones que un alta manual (incluida la de que el centro sea uno de los suyos).
- El **canal** de una notificación lo fija siempre el sistema según la clase de notificación que se crea; ningún usuario puede elegirlo ni cambiarlo como dato.

# Recursos y datos iniciales

- Los permisos de acceso del subsistema (los de los roles de «Seguridad»), que se cargan al arrancar y sustituyen a los de los antiguos subsistemas de correos y de SMS.
- La configuración de envío de correo y de SMS de la instalación (servidor de correo, cuenta del proveedor de SMS). No son datos de la aplicación y ningún escenario depende de que estén rellenas: sin ellas el envío falla y la notificación queda «Fallido».
- Los certificados digitales de los usuarios de demo para firmar en el servidor: hoy los datos de demo no los traen (los aportará otra iniciativa). Mientras tanto, los escenarios que presentan una «Justificación de falta del profesorado» (ESC-070, ESC-071, ESC-057, ESC-075) dan de alta antes, desde «Criptografía» → «Certificados digitales», el certificado del director, y entonces firman la solicitud en el servidor, sin AutoFirma.
- No hay ninguna notificación precargada: cada escenario crea las suyas. Los centros y usuarios son los de los datos de demo.

# Fuera de alcance

- Migrar los correos y SMS que existan hoy: se arranca con la base de datos vacía.
- Que el motivo sea confidencial frente al destinatario: solo no se le muestra en pantalla.
- Modificar o borrar una notificación (o un adjunto) una vez creada.
- Que el gestor del centro dé de alta notificaciones a mano.
- Que el Administrador tenga el submenú «Del centro»: le basta con «Todas».
- Que el Vicesecretario, el Vicedirector, el Conserje u otros cargos tengan acceso a «Del centro».
- Enviar a varias direcciones en el «para» de un correo: los destinatarios adicionales van en «en copia» o «en copia oculta».
- Canales distintos del correo y del SMS (el subsistema debe admitirlos en el futuro, pero esta iniciativa no añade ninguno).
- Cambiar cómo se realiza técnicamente el envío del correo o del SMS.
- Relanzar automáticamente las notificaciones que se queden en «Pendiente» porque su envío no llegó a lanzarse (por ejemplo, si la aplicación se detiene justo después de guardarlas).
