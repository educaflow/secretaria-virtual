---
type: specification
template: system
---

# Objetivo

Ofrecer a cualquier usuario una ventanilla desde la que crear un nuevo expediente mediante un asistente de tres pasos (centro → trámite → contexto del trámite) que solo le ofrece los centros y los trámites en los que realmente puede crear expedientes. Es un **sistema**. Depende funcionalmente de los centros y los usuarios de la aplicación, de los trámites y tipos de trámite, de los permisos que deciden quién puede iniciar cada trámite en cada centro, y de la tramitación de expedientes, que es quien crea el expediente y lo abre.

# Actores

- **Usuario**: cualquier persona con sesión iniciada, sea cual sea su tipo de usuario o su cargo (incluido el Administrador, que aquí se comporta como un usuario más, limitado a los centros a los que pertenece). Lo que puede hacer en la ventanilla lo deciden los permisos que tiene para iniciar trámites en cada uno de sus centros.
- **Interesado que presenta él mismo**: usuario que inicia un trámite dirigido a su tipo de usuario (p. ej. un alumno un trámite para el alumno, un profesor un trámite para el profesor).
- **Familiar**: usuario que inicia un trámite en representación de su hijo/a menor de edad o persona tutelada.
- **Quien registra en papel**: usuario del centro (p. ej. un administrativo o un jefe de estudios) que registra un trámite que otra persona ha entregado en papel.

# Historias de usuario

## HU-001 — Como Usuario quiero crear un expediente eligiendo solo entre los trámites que puedo iniciar en mi centro para no perder tiempo con trámites que no me corresponden

- ESC-001 — Alumno de un solo centro crea un expediente sin que se le pregunte nada:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Crear expediente».
  7. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata», presentado por el propio alumno (no registrado como presentado en papel) y para él mismo (no en representación de otra persona).
- ESC-002 — Profesor ve solo los trámites para el profesor:
  1. El director «director@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el profesor» y, dentro, por orden alfabético, «Justificación de falta del profesorado» y «Trámite de prueba»; no aparece «Trámites para el alumno»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
- ESC-003 — Usuario que no puede crear expedientes en ningún centro:
  1. El exalumno «exalumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no abre ninguna pantalla del asistente y muestra el aviso «No puede crear expedientes en ninguno de sus centros».
- ESC-004 — El Administrador se comporta como un usuario más:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. Como no puede iniciar ningún trámite en el único centro al que pertenece («CIPFP Mislata»), el sistema no abre ninguna pantalla del asistente, no le ofrece el centro «CIPFP Batoi» y muestra el aviso «No puede crear expedientes en ninguno de sus centros».

## HU-002 — Como Usuario que pertenece a varios centros quiero elegir primero el centro para que el expediente se cree en el centro correcto

- ESC-005 — Alumno de dos centros elige el centro:
  1. El alumno «alumnodoscentros@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema abre la ventana «Nuevo expediente: elija el centro» con dos filas, por orden alfabético: «CIPFP Batoi» y «CIPFP Mislata», y un botón «Cancelar».
  4. Pulsa la fila «CIPFP Batoi».
  5. El sistema abre «Nuevo expediente: elija el trámite» con el centro «CIPFP Batoi» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Atrás» (no hay botón «Cancelar»).
  6. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  7. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Batoi» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  8. Pulsa «Crear expediente».
  9. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Batoi», y no en «CIPFP Mislata», presentado por el propio alumno y para él mismo.
- ESC-006 — Cancelar en la elección de centro:
  1. El alumno «alumnodoscentros@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema abre la ventana «Nuevo expediente: elija el centro» con dos filas, por orden alfabético: «CIPFP Batoi» y «CIPFP Mislata», y un botón «Cancelar».
  4. Pulsa «Cancelar».
  5. El sistema cierra el asistente sin abrir el árbol de trámites ni ningún expediente.

## HU-003 — Como Usuario quiero poder volver al paso anterior del asistente para corregir una elección sin empezar de nuevo

- ESC-007 — Atrás desde el contexto del trámite y desde el árbol:
  1. El alumno «alumnodoscentros@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema abre la ventana «Nuevo expediente: elija el centro» con dos filas, por orden alfabético: «CIPFP Batoi» y «CIPFP Mislata», y un botón «Cancelar».
  4. Pulsa la fila «CIPFP Mislata».
  5. El sistema abre «Nuevo expediente: elija el trámite» con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Atrás» (no hay botón «Cancelar»).
  6. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  7. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  8. Pulsa «Atrás».
  9. El sistema vuelve a «Nuevo expediente: elija el trámite» con el mismo centro «CIPFP Mislata» visible encima del árbol, sin abrir ningún expediente.
  10. Pulsa «Atrás» debajo del árbol.
  11. El sistema vuelve a la ventana «Nuevo expediente: elija el centro» con las filas «CIPFP Batoi» y «CIPFP Mislata», sin abrir ningún expediente.
- ESC-008 — Cancelar en el árbol cuando no hubo elección de centro:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. Pulsa el botón «Cancelar» que hay debajo del árbol.
  5. El sistema cierra el asistente sin abrir la pantalla «Nuevo expediente» ni ningún expediente.
- ESC-019 — Volver al listado de centros, elegir otro centro y crear el expediente en él:
  1. El alumno «alumnodoscentros@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema abre la ventana «Nuevo expediente: elija el centro» con dos filas, por orden alfabético: «CIPFP Batoi» y «CIPFP Mislata», y un botón «Cancelar».
  4. Pulsa la fila «CIPFP Mislata».
  5. El sistema abre «Nuevo expediente: elija el trámite» con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Atrás» (no hay botón «Cancelar»).
  6. Pulsa «Atrás» debajo del árbol.
  7. El sistema vuelve a la ventana «Nuevo expediente: elija el centro» con las filas «CIPFP Batoi» y «CIPFP Mislata».
  8. Pulsa la fila «CIPFP Batoi».
  9. El sistema abre «Nuevo expediente: elija el trámite» con el centro «CIPFP Batoi» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Atrás» (no hay botón «Cancelar»).
  10. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  11. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Batoi» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  12. Pulsa «Crear expediente».
  13. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Batoi», y no en «CIPFP Mislata», presentado por el propio alumno y para él mismo.
- ESC-020 — Atrás desde el contexto del trámite cuando no hubo elección de centro:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Atrás».
  7. El sistema vuelve a «Nuevo expediente: elija el trámite» con el centro «CIPFP Mislata» visible encima del árbol, sin abrir ningún expediente; debajo del árbol sigue habiendo un único botón, «Cancelar» (no hay botón «Atrás»).
  8. Pulsa «Cancelar».
  9. El sistema cierra el asistente sin abrir ningún expediente.

## HU-004 — Como Quien registra en papel quiero indicar que estoy registrando un trámite recibido en papel para que el expediente quede registrado como presentado en papel

- ESC-009 — Administrativo que solo puede registrar en papel: no se le pregunta cómo se presenta:
  1. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?» (el sistema deduce que se está registrando un trámite recibido en papel); muestra la pregunta «¿Para quién es el expediente?» con las opciones «Para mí» y «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)», sin ninguna marcada; muestra los botones «Atrás» y «Crear expediente».
  6. Marca «Para mí» en la pregunta «¿Para quién es el expediente?».
  7. Pulsa «Crear expediente».
  8. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata», registrado como presentado en papel por el administrativo y para él mismo (no en representación de otra persona).
- ESC-010 — Usuario con las dos formas de presentar debe elegir:
  1. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el profesor» y, dentro, por orden alfabético, «Justificación de falta del profesorado» y «Trámite de prueba»; no aparece «Trámites para el alumno»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Justificación de falta del profesorado».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Justificación de falta del profesorado», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; muestra la pregunta «¿Cómo se presenta?» con las opciones «Lo presento yo mismo» y «Estoy registrando un trámite recibido en papel», sin ninguna marcada; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Sin marcar ninguna opción pulsa «Crear expediente».
  7. El sistema no crea el expediente, muestra «Debe indicar cómo se presenta el expediente» y la pantalla «Nuevo expediente» sigue abierta.
  8. Marca «Estoy registrando un trámite recibido en papel» en la pregunta «¿Cómo se presenta?».
  9. El sistema sigue sin mostrar la pregunta «¿Para quién es el expediente?» porque el trámite no admite representación.
  10. Pulsa «Crear expediente».
  11. El sistema cierra el asistente y abre el expediente recién creado de «Justificación de falta del profesorado» en su primer estado, en el centro «CIPFP Mislata», registrado como presentado en papel por el jefe de estudios y para él mismo (no en representación de otra persona).
- ESC-011 — «¿Para quién es?» se recalcula al cambiar cómo se presenta:
  1. El administrativo «administrativo2@mislata.es» (que en «CIPFP Mislata» es además alumno) inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; muestra la pregunta «¿Cómo se presenta?» con las opciones «Lo presento yo mismo» y «Estoy registrando un trámite recibido en papel», sin ninguna marcada; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Marca «Lo presento yo mismo».
  7. El sistema sigue sin mostrar «¿Para quién es el expediente?» (es alumno y no es familiar: el expediente es para él mismo).
  8. Cambia a «Estoy registrando un trámite recibido en papel».
  9. El sistema muestra «¿Para quién es el expediente?» con las opciones «Para mí» y «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)», sin ninguna marcada.
  10. Sin marcar ninguna opción pulsa «Crear expediente».
  11. El sistema no crea el expediente, muestra «Debe indicar para quién es el expediente» y la pantalla «Nuevo expediente» sigue abierta.
  12. Marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)» en la pregunta «¿Para quién es el expediente?».
  13. Pulsa «Crear expediente».
  14. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata», registrado como presentado en papel por el administrativo y en representación de otra persona (no para él mismo).
- ESC-021 — Una respuesta a «¿Para quién es?» dada en papel no se conserva al pasar a «Lo presento yo mismo»:
  1. El administrativo «administrativo2@mislata.es» (que en «CIPFP Mislata» es además alumno) inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; muestra la pregunta «¿Cómo se presenta?» con las opciones «Lo presento yo mismo» y «Estoy registrando un trámite recibido en papel», sin ninguna marcada; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Marca «Estoy registrando un trámite recibido en papel».
  7. El sistema muestra «¿Para quién es el expediente?» sin ninguna opción marcada.
  8. Marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)».
  9. Cambia «¿Cómo se presenta?» a «Lo presento yo mismo».
  10. El sistema deja de mostrar «¿Para quién es el expediente?» (es alumno y no es familiar: el expediente es para él mismo).
  11. Pulsa «Crear expediente».
  12. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata», presentado por el propio usuario y para él mismo; no queda en representación de otra persona ni registrado como presentado en papel.

## HU-005 — Como Familiar quiero crear un expediente en representación de mi hijo/a o persona tutelada para tramitarlo en su nombre

- ESC-012 — Familiar que no es alumno: en representación sin preguntar:
  1. El familiar «familiar1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Crear expediente».
  7. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata», presentado por el propio familiar (no registrado como presentado en papel) y en representación de otra persona (no para él mismo).
- ESC-013 — Usuario que es alumno y familiar elige para quién es:
  1. El usuario «alumnofamiliar@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; muestra la pregunta «¿Para quién es el expediente?» con las opciones «Para mí» y «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)», sin ninguna marcada; muestra los botones «Atrás» y «Crear expediente».
  6. Sin marcar ninguna opción pulsa «Crear expediente».
  7. El sistema no crea el expediente, muestra «Debe indicar para quién es el expediente» y la pantalla «Nuevo expediente» sigue abierta.
  8. Marca «Para mí».
  9. Pulsa «Crear expediente».
  10. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata», presentado por el propio usuario (no registrado como presentado en papel) y para él mismo; no queda en representación de otra persona.
- ESC-022 — Usuario que es alumno y familiar crea el expediente en representación:
  1. El usuario «alumnofamiliar@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; muestra la pregunta «¿Para quién es el expediente?» con las opciones «Para mí» y «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)», sin ninguna marcada; muestra los botones «Atrás» y «Crear expediente».
  6. Marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)».
  7. Pulsa «Crear expediente».
  8. El sistema cierra el asistente y abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata», presentado por el propio usuario (no registrado como presentado en papel) y en representación de otra persona; no queda para el propio usuario.

## HU-006 — Como Usuario quiero que el sistema vuelva a comprobar en el servidor todo lo que el asistente calculó para que nadie pueda crear, manipulando la pantalla, expedientes que no le corresponden en mi centro

- ESC-014 — Petición manipulada con un centro ajeno:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Crear expediente» con la petición manipulada para que el centro enviado sea «CIPFP Batoi», al que no pertenece; el resto de lo enviado queda como lo fijó la pantalla: el trámite «Anulación de matrícula en ciclo formativo», presentado por el propio alumno y para él mismo.
  7. El sistema no crea ningún expediente y muestra, bajo el título «No es posible crear el expediente», únicamente el mensaje «No puede crear expedientes de este trámite en el centro indicado»; no muestra además «No puede presentar el expediente de esa forma en el centro indicado».
  8. El asistente no se cierra: la pantalla «Nuevo expediente» sigue abierta con el trámite «Anulación de matrícula en ciclo formativo» y el centro «CIPFP Mislata», y no se abre ningún expediente.
- ESC-015 — Petición manipulada con una forma de presentar que no tiene:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Crear expediente» con la petición manipulada para indicar que está registrando un trámite recibido en papel; el resto de lo enviado queda como lo fijó la pantalla: el centro «CIPFP Mislata», el trámite «Anulación de matrícula en ciclo formativo» y el expediente para él mismo.
  7. El sistema no crea ningún expediente y muestra, bajo el título «No es posible crear el expediente», el mensaje «No puede presentar el expediente de esa forma en el centro indicado».
  8. El asistente no se cierra: la pantalla «Nuevo expediente» sigue abierta con el trámite «Anulación de matrícula en ciclo formativo» y el centro «CIPFP Mislata», y no se abre ningún expediente.
- ESC-016 — Petición manipulada con un «para quién» que la ventanilla no ofrece:
  1. El familiar «familiar1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Crear expediente» con la petición manipulada para indicar que el expediente es para él mismo; el resto de lo enviado queda como lo fijó la pantalla: el centro «CIPFP Mislata», el trámite «Anulación de matrícula en ciclo formativo» y presentado por el propio familiar.
  7. El sistema no crea ningún expediente y muestra, bajo el título «No es posible crear el expediente», el mensaje «No puede crear este expediente para usted mismo en el centro indicado».
  8. El asistente no se cierra: la pantalla «Nuevo expediente» sigue abierta con el trámite «Anulación de matrícula en ciclo formativo» y el centro «CIPFP Mislata», y no se abre ningún expediente.
  9. El familiar cierra sesión.
  10. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  11. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  12. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  13. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  14. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  15. Pulsa «Crear expediente» con la petición manipulada para indicar que el expediente es en representación de otra persona; el resto de lo enviado queda como lo fijó la pantalla: el centro «CIPFP Mislata», el trámite «Anulación de matrícula en ciclo formativo» y presentado por el propio alumno.
  16. El sistema no crea ningún expediente y muestra, bajo el título «No es posible crear el expediente», el mensaje «No puede crear este expediente en representación de otra persona en el centro indicado».
  17. El asistente no se cierra: la pantalla «Nuevo expediente» sigue abierta con el trámite «Anulación de matrícula en ciclo formativo» y el centro «CIPFP Mislata», y no se abre ningún expediente.
- ESC-023 — Petición manipulada con un trámite que el usuario no puede iniciar en su centro:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Crear expediente» con la petición manipulada para que el trámite enviado sea «Justificación de falta del profesorado», que el árbol no le ofrece; el resto de lo enviado queda como lo fijó la pantalla: el centro «CIPFP Mislata», presentado por el propio alumno y para él mismo.
  7. El sistema no crea ningún expediente y muestra, bajo el título «No es posible crear el expediente», únicamente el mensaje «No puede crear expedientes de este trámite en el centro indicado»; no muestra además «No puede presentar el expediente de esa forma en el centro indicado».
  8. El asistente no se cierra: la pantalla «Nuevo expediente» sigue abierta con el trámite «Anulación de matrícula en ciclo formativo» y el centro «CIPFP Mislata», y no se abre ningún expediente. La pantalla sigue mostrando el trámite que el alumno eligió en el árbol, no el que se envió manipulado.
- ESC-024 — Petición manipulada en representación en un trámite que no la admite:
  1. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el profesor» y, dentro, por orden alfabético, «Justificación de falta del profesorado» y «Trámite de prueba»; no aparece «Trámites para el alumno»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Justificación de falta del profesorado».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Justificación de falta del profesorado», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; muestra la pregunta «¿Cómo se presenta?» con las opciones «Lo presento yo mismo» y «Estoy registrando un trámite recibido en papel», sin ninguna marcada; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Marca «Estoy registrando un trámite recibido en papel».
  7. El sistema sigue sin mostrar la pregunta «¿Para quién es el expediente?» porque el trámite no admite representación.
  8. Pulsa «Crear expediente» con la petición manipulada para indicar que el expediente es en representación de otra persona; el resto de lo enviado queda como está en la pantalla: el centro «CIPFP Mislata», el trámite «Justificación de falta del profesorado» y registrado como recibido en papel.
  9. El sistema no crea ningún expediente y muestra, bajo el título «No es posible crear el expediente», el mensaje «Este trámite no permite presentar la solicitud en representación de otra persona».
  10. El asistente no se cierra: la pantalla «Nuevo expediente» sigue abierta con el trámite «Justificación de falta del profesorado» y el centro «CIPFP Mislata», y no se abre ningún expediente. «Estoy registrando un trámite recibido en papel» sigue marcado.
- ESC-025 — Petición manipulada sin centro:
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el alumno» y, dentro, únicamente el trámite «Anulación de matrícula en ciclo formativo»; no aparece «Trámites para el profesor»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Anulación de matrícula en ciclo formativo».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Crear expediente» con la petición manipulada para que no se envíe ningún centro; el resto de lo enviado queda como lo fijó la pantalla: el trámite «Anulación de matrícula en ciclo formativo», presentado por el propio alumno y para él mismo.
  7. El sistema no crea ningún expediente y muestra, bajo el título «No es posible crear el expediente», únicamente el mensaje «Debe indicar el centro»; no muestra además «No puede crear expedientes de este trámite en el centro indicado» ni «No puede presentar el expediente de esa forma en el centro indicado».
  8. El asistente no se cierra: la pantalla «Nuevo expediente» sigue abierta con el trámite «Anulación de matrícula en ciclo formativo» y el centro «CIPFP Mislata», y no se abre ningún expediente.

## HU-007 — Como Interesado que presenta él mismo quiero crear un expediente de un trámite dirigido a mi tipo de usuario sin que se me pregunte lo que el sistema ya puede deducir para presentarlo con los mínimos pasos

- ESC-017 — Profesor crea un expediente de un trámite que no admite representación sin que se le pregunte nada:
  1. El director «director@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el profesor» y, dentro, por orden alfabético, «Justificación de falta del profesorado» y «Trámite de prueba»; no aparece «Trámites para el alumno»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Justificación de falta del profesorado».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Justificación de falta del profesorado», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; no muestra la pregunta «¿Cómo se presenta?»; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Pulsa «Crear expediente».
  7. El sistema cierra el asistente y abre el expediente recién creado de «Justificación de falta del profesorado» en su primer estado, en el centro «CIPFP Mislata», presentado por el propio director (no registrado como presentado en papel) y para él mismo.
- ESC-018 — Usuario con las dos formas de presentar elige «Lo presento yo mismo»:
  1. El jefe de estudios «jefeestudios1@mislata.es» inicia sesión con contraseña «demo1234».
  2. Abre el menú «Ventanilla» y pulsa «Nuevo expediente».
  3. El sistema no muestra el listado de centros y abre directamente «Nuevo expediente: elija el trámite», con el centro «CIPFP Mislata» visible encima del árbol; el árbol muestra únicamente el tipo de trámite «Trámites para el profesor» y, dentro, por orden alfabético, «Justificación de falta del profesorado» y «Trámite de prueba»; no aparece «Trámites para el alumno»; debajo del árbol hay un único botón, «Cancelar» (no hay botón «Atrás»).
  4. En el árbol pulsa el trámite «Justificación de falta del profesorado».
  5. El sistema abre la pantalla «Nuevo expediente» con el nombre del trámite «Justificación de falta del profesorado», su texto de ayuda y el centro «CIPFP Mislata» en solo lectura; muestra la pregunta «¿Cómo se presenta?» con las opciones «Lo presento yo mismo» y «Estoy registrando un trámite recibido en papel», sin ninguna marcada; no muestra la pregunta «¿Para quién es el expediente?»; muestra los botones «Atrás» y «Crear expediente».
  6. Marca «Lo presento yo mismo».
  7. El sistema sigue sin mostrar la pregunta «¿Para quién es el expediente?».
  8. Pulsa «Crear expediente».
  9. El sistema cierra el asistente y abre el expediente recién creado de «Justificación de falta del profesorado» en su primer estado, en el centro «CIPFP Mislata», presentado por el propio jefe de estudios (no registrado como presentado en papel) y para él mismo.

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-AsistenteNuevoExpediente.md](./entity-AsistenteNuevoExpediente.md) | AsistenteNuevoExpediente | La ficha del último paso del asistente: el centro y el trámite elegidos, cómo se presenta y para quién es el expediente. No se guarda: solo sirve para crear el expediente. |

AsistenteNuevoExpediente referencia a un centro y a un trámite que ya existen en la aplicación (no los crea ni los modifica). No tiene hijos ni se conserva: cuando el expediente se crea, o el usuario abandona el asistente, la ficha desaparece.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-nuevo-expediente.md](./screen-nuevo-expediente.md) | Nuevo expediente | El asistente de tres pasos (centro → trámite → contexto del trámite) con el que cualquier usuario crea un expediente en uno de sus centros. |

# Seguridad

- **Cualquier usuario con sesión iniciada (todos los tipos de usuario y cargos, incluido el Administrador):** ve el menú «Ventanilla → Nuevo expediente» y puede usar el asistente. Alcance por centro: solo los centros a los que pertenece y, dentro de cada uno, solo los trámites que puede iniciar en ese centro; nunca ve ni puede elegir centros ajenos. No puede ver, modificar ni borrar nada desde la ventanilla: solo crear un expediente, y la creación vuelve a comprobarse entera en el servidor (centro, forma de presentar y para quién es) sin fiarse de lo que la pantalla ocultó o calculó. El Administrador no tiene aquí ningún alcance especial sobre otros centros.

# Recursos y datos iniciales

- Los permisos necesarios para que cualquier usuario con sesión pueda usar la ventanilla.
- No hay recursos estáticos ni otros datos propios. La ventanilla se apoya en datos que ya existen en la aplicación: los tipos de trámite («Trámites para el alumno», dirigido al Alumno; «Trámites para el profesor», dirigido al Profesor), los trámites («Anulación de matrícula en ciclo formativo», que admite representación; «Justificación de falta del profesorado» y «Trámite de prueba», que no la admiten) y los permisos por defecto para iniciar trámites (Profesor, Alumno y Familiar los presentan ellos mismos; Administrativo y Jefe de estudios los registran en papel).

# Fuera de alcance

- Retirar la creación de expedientes actual («Expedientes → Trámites»): por ahora conviven las dos. Se asume el riesgo de que la entrada antigua no filtra por centro ni por lo que el usuario puede iniciar, así que lo que la ventanilla oculta sigue siendo alcanzable por esa otra entrada.
- Cualquier otro caso de uso de la ventanilla distinto de crear un expediente.
- Un botón «Ayuda» en cada trámite del árbol: la ayuda del trámite solo se ve en el último paso.
- Que el Administrador cree expedientes en centros a los que no pertenece.
- Cambiar las comprobaciones que ya hace la tramitación al crear un expediente, o quién puede iniciar cada trámite.
