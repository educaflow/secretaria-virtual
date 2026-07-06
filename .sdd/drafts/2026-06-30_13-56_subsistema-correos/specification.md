---
type: specification
---

# Objetivo

Crear el **subsistema** `correos`, encargado de enviar los correos electrónicos en la secretaría virtual y de dejar **constancia permanente** de todo lo enviado. Cada correo va siempre dirigido a una persona (identificada por su DNI) y guarda tanto su contenido como el Nº de intentos de envío. El subsistema ofrece tanto una **forma programática** de enviar correos (que usan otras partes de la aplicación llamando al subsistema) como las **pantallas** para que el administrador cree y reenvíe correos, el supervisor vigile y reintente los de su centro, y cualquier usuario consulte los que se le han enviado con éxito. Reutiliza la capacidad de envío de correo ya existente en la infraestructura del proyecto y se apoya en la gestión de centros para el alcance multicentro.

# Actores

- **Administrador:** gestiona los correos de cualquier centro; crea correos nuevos y reenvía los que han fallado.
- **Supervisor:** gestiona un centro; consulta todos los correos de su propio centro y reintenta el envío de los que han fallado.
- **Destinatario:** cualquier usuario autenticado (alumno, profesor, familiar, etc.) que consulta, en solo lectura, los correos que se le han enviado con éxito, identificándose por su DNI.

# Historias de usuario

## HU-001 — Como Administrador quiero crear y enviar un correo a una persona para comunicarme con ella dejando constancia de lo enviado

- ESC-001 — Alta de un correo que se intenta enviar:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el destinatario con el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» con «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «Le esperamos el lunes a las 9:00», y elige el centro «CIPFP Mislata».
  4. Pulsa «Guardar».
  5. El sistema crea el correo en estado PENDIENTE, registra la fecha de creación e intenta enviarlo de forma asíncrona, registrando la fecha del primer intento y la del último intento, y poniendo el número de reintentos a 1.
  6. Si el envío se realiza correctamente: el correo pasa a estado SUCCESS y se registra la fecha de envío.
  7. Si el envío falla: el correo pasa a estado FAIL, se guarda la descripción del fallo y la fecha de envío queda vacía.
- ESC-002 — Alta con adjunto:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto» y el cuerpo «Adjunto el documento solicitado», y elige el centro «CIPFP Mislata».
  4. En el panel de adjuntos pulsa «Añadir adjunto».
  5. Rellena el nombre del fichero con «documento.pdf» y sube un fichero como contenido del adjunto.
  6. Pulsa «Guardar» del adjunto.
  7. Pulsa «Guardar» en el correo.
  8. El sistema crea el correo en estado PENDIENTE con un adjunto de nombre «documento.pdf» asociado, e intenta enviarlo de forma asíncrona con ese adjunto.
- ESC-003 — Alta sin el DNI del destinatario:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata», pero deja vacío el DNI del destinatario.
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El DNI del destinatario es obligatorio».
- ESC-010 — Alta sin destinatario en el «para»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata», pero deja vacío el «para».
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «Debe indicar al menos un destinatario en el «para»».
- ESC-011 — Alta sin asunto:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el cuerpo «texto» y elige el centro «CIPFP Mislata», pero deja vacío el asunto.
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El asunto es obligatorio».
- ESC-012 — Alta sin cuerpo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión» y elige el centro «CIPFP Mislata», pero deja vacío el cuerpo.
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El cuerpo es obligatorio».
- ESC-013 — Alta sin centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión» y el cuerpo «texto», pero no elige ningún centro.
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El centro es obligatorio».
- ESC-014 — Alta sin el nombre:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata», pero deja vacío el nombre.
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El nombre es obligatorio».
- ESC-015 — Alta sin los apellidos:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata», pero deja vacíos los apellidos.
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «Los apellidos son obligatorios».
- ESC-016 — Alta con adjunto sin nombre de fichero:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto» y el cuerpo «Adjunto el documento solicitado», y elige el centro «CIPFP Mislata».
  4. En el panel de adjuntos pulsa «Añadir adjunto».
  5. Sube un fichero como contenido del adjunto y deja vacío el nombre del fichero.
  6. Pulsa «Guardar» del adjunto.
  7. Pulsa «Guardar» en el correo.
  8. El sistema no crea el correo y muestra el error «El nombre del fichero es obligatorio».
- ESC-017 — Alta con adjunto sin contenido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto» y el cuerpo «Adjunto el documento solicitado», y elige el centro «CIPFP Mislata».
  4. En el panel de adjuntos pulsa «Añadir adjunto».
  5. Rellena el nombre del fichero con «documento.pdf» y deja vacío el contenido (no subas ningún fichero).
  6. Pulsa «Guardar» del adjunto.
  7. Pulsa «Guardar» en el correo.
  8. El sistema no crea el correo y muestra el error «Debe adjuntar el fichero».
- ESC-020 — Alta con «para» de formato inválido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «direccion-sin-arroba», el asunto «Convocatoria de reunión» y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)».
- ESC-021 — Alta con el DNI del destinatario inválido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «12345678A» (con letra de control incorrecta), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión» y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El DNI del destinatario no es válido; compruebe la letra».
- ESC-022 — Alta con dos adjuntos con el mismo nombre de fichero:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto» y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. En el panel de adjuntos pulsa «Añadir adjunto», rellena el nombre del fichero «documento.pdf», sube un fichero como contenido y pulsa «Guardar» del adjunto.
  5. Pulsa de nuevo «Añadir adjunto», rellena otra vez el nombre del fichero «documento.pdf», sube un fichero como contenido y pulsa «Guardar» del adjunto.
  6. Pulsa «Guardar» en el correo.
  7. El sistema no crea el correo y muestra el error «Ya existe un adjunto con ese nombre en el correo».
- ESC-023 — Alta con el asunto demasiado largo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», un asunto con la letra «A» repetida 256 veces (más de 255 caracteres) y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El asunto no puede superar 255 caracteres».
- ESC-024 — Alta con «en copia» de formato inválido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el «en copia» «copia-sin-arroba», el asunto «Convocatoria de reunión» y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El «en copia» debe contener direcciones de correo válidas».
- ESC-025 — Alta con «en copia oculta» de formato inválido:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el «en copia oculta» «oculta-sin-arroba», el asunto «Convocatoria de reunión» y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. Pulsa «Guardar».
  5. El sistema no crea el correo y muestra el error «El «en copia oculta» debe contener direcciones de correo válidas».

## HU-002 — Como Administrador quiero reenviar un correo que ha fallado para volver a intentar la comunicación

- ESC-004 — Reenvío de un correo en FAIL:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «destino-invalido@example.com», el asunto «Aviso» y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. Pulsa «Guardar».
  5. El sistema crea el correo en estado PENDIENTE e intenta enviarlo de forma asíncrona; al fallar el envío a «destino-invalido@example.com», el correo pasa a estado FAIL con número de reintentos 1.
  6. Recarga el listado de correos hasta que el correo «Aviso» aparezca en estado FAIL.
  7. Abre el detalle del correo «Aviso», donde aparece el botón «Reenviar».
  8. Pulsa «Reenviar».
  9. El sistema vuelve a intentar el envío de forma asíncrona y, como el «para» sigue siendo inválido, el correo permanece en estado FAIL, actualiza la fecha del último intento, incrementa el número de reintentos a 2 y actualiza la descripción del fallo.
- ESC-005 — El botón Reenviar no aparece si el correo no ha fallado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Correo correcto» y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. Pulsa «Guardar».
  5. El sistema crea el correo e intenta enviarlo de forma asíncrona; el envío se realiza correctamente y el correo queda en estado SUCCESS.
  6. Recarga el listado de correos hasta que el correo «Correo correcto» aparezca en estado SUCCESS.
  7. Abre el detalle de ese correo.
  8. El sistema no muestra el botón «Reenviar».

## HU-003 — Como Supervisor quiero consultar los correos de mi centro y reintentar los fallidos para asegurar que las comunicaciones de mi centro se entregan

- ESC-006 — El supervisor solo ve los correos de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Aviso Mislata» y el cuerpo «texto», elige el centro «CIPFP Mislata» y pulsa «Guardar».
  4. Pulsa «Nuevo correo», rellena los mismos datos (DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «alumno1@mislata.es», cuerpo «texto») pero con el asunto «Aviso Batoi», elige el centro «CIPFP Batoi» y pulsa «Guardar».
  5. El administrador cierra sesión.
  6. El supervisor «supervisor1@mislata.es» (del centro «CIPFP Mislata») inicia sesión con contraseña «demo1234».
  7. Abre la pantalla de correos de su centro.
  8. El sistema muestra el correo «Aviso Mislata» y no muestra el correo «Aviso Batoi».
- ESC-007 — El supervisor reintenta un correo fallido de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «destino-invalido@example.com», el asunto «Aviso» y el cuerpo «texto», elige el centro «CIPFP Mislata» y pulsa «Guardar».
  4. El sistema intenta enviarlo de forma asíncrona; al fallar, el correo queda en estado FAIL con número de reintentos 1.
  5. El administrador cierra sesión.
  6. El supervisor «supervisor1@mislata.es» (del centro «CIPFP Mislata») inicia sesión con contraseña «demo1234» y abre la pantalla de correos de su centro.
  7. Recarga el listado hasta ver el correo «Aviso» en estado FAIL y abre su detalle, donde ve la descripción del fallo y las fechas de intento, y aparece el botón «Reenviar».
  8. Pulsa «Reenviar».
  9. El sistema vuelve a intentar el envío de forma asíncrona y, como el destinatario sigue siendo inválido, el correo permanece en estado FAIL, actualiza la fecha del último intento e incrementa el número de reintentos de 1 a 2.
- ESC-018 — El supervisor descarga el adjunto de un correo de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y pulsa «Nuevo correo».
  3. Rellena el DNI «86862719E» (el del usuario «alumno1@mislata.es»), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Circular con adjunto» y el cuerpo «texto», y elige el centro «CIPFP Mislata».
  4. En el panel de adjuntos pulsa «Añadir adjunto», rellena el nombre del fichero «circular.pdf», sube un fichero como contenido y pulsa «Guardar» del adjunto.
  5. Pulsa «Guardar» en el correo.
  6. El sistema intenta enviarlo de forma asíncrona; el envío se realiza con éxito y el correo queda en estado SUCCESS.
  7. El administrador cierra sesión.
  8. El supervisor «supervisor1@mislata.es» (del centro «CIPFP Mislata») inicia sesión con contraseña «demo1234» y abre la pantalla de correos de su centro.
  9. Recarga el listado hasta ver el correo «Circular con adjunto» en estado SUCCESS y abre su detalle; en el panel de adjuntos aparece «circular.pdf».
  10. Pulsa la fila del adjunto «circular.pdf» para abrir su formulario de detalle.
  11. Descarga el fichero del campo contenido.
  12. El sistema descarga el fichero «circular.pdf».

## HU-004 — Como usuario destinatario quiero ver los correos que se me han enviado con éxito para consultarlos y descargar sus adjuntos

- ESC-008 — El destinatario consulta un correo enviado con éxito y descarga su adjunto:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin» y crea un correo dirigido al DNI «86862719E» (el del usuario «alumno1@mislata.es»), nombre «Alumno1», apellidos «CIPFP Mislata», con «para» «alumno1@mislata.es», asunto «Tu certificado», cuerpo «Adjunto tu certificado», un adjunto llamado «certificado.pdf», en el centro «CIPFP Mislata», cuyo envío se realiza con éxito y queda en estado SUCCESS.
  2. El administrador cierra sesión.
  3. El usuario «alumno1@mislata.es» (cuyo DNI es «86862719E») inicia sesión con contraseña «demo1234».
  4. Abre la pantalla «Mis correos».
  5. Pulsa la fila del correo «Tu certificado» para abrir su detalle.
  6. El sistema muestra el correo «Tu certificado» en solo lectura, con su asunto, cuerpo y fecha de envío.
  7. En el panel de adjuntos, el usuario descarga el adjunto «certificado.pdf».
  8. El sistema descarga el fichero «certificado.pdf».
- ESC-009 — El destinatario no ve los correos no enviados:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin» y crea un correo dirigido al DNI «86862719E» (el del usuario «alumno1@mislata.es»), nombre «Alumno1», apellidos «CIPFP Mislata», «para» «destino-invalido@example.com», asunto «No entregado» y cuerpo «texto», cuyo envío falla y queda en estado FAIL.
  2. El administrador cierra sesión.
  3. El usuario «alumno1@mislata.es» (cuyo DNI es «86862719E») inicia sesión con contraseña «demo1234» y abre la pantalla «Mis correos».
  4. El sistema no muestra el correo «No entregado».
- ESC-019 — El destinatario no ve un correo enviado con éxito a otra persona:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de administración de correos y crea un correo dirigido al DNI «03532821K» (el del usuario «alumno2@mislata.es»), nombre «Alumno2», apellidos «CIPFP Mislata», «para» «alumno2@mislata.es», asunto «Aviso para Alumno2» y cuerpo «texto», en el centro «CIPFP Mislata», cuyo envío se realiza con éxito y queda en estado SUCCESS.
  3. El administrador cierra sesión.
  4. El usuario «alumno1@mislata.es» (cuyo DNI es «86862719E») inicia sesión con contraseña «demo1234».
  5. Abre la pantalla «Mis correos».
  6. El sistema no muestra el correo «Aviso para Alumno2», porque su DNI de destinatario no coincide con el del usuario.

# Modelos

| Fichero                                  | Modelo  | Qué representa                                                                                                        |
| ---------------------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------- |
| [entity-Correo.md](./entity-Correo.md)   | Correo  | Un correo electrónico enviado (o por enviar) a una persona, con su contenido y el resultado de cada intento de envío. |
| [entity-Adjunto.md](./entity-Adjunto.md) | Adjunto | Un fichero adjunto de un correo, guardado como copia independiente.                                                   |

Relaciones entre los modelos:

- Un **Correo** tiene cero o varios **Adjuntos** (relación padre/hijo): los adjuntos pertenecen al correo y se crean junto con él. Como un correo nunca se borra, sus adjuntos tampoco.
- Un **Correo** pertenece a un **centro** (referencia a un centro existente de la gestión de centros).
- Un **Correo** puede tener, **opcionalmente** (puede no tener valor), una referencia al **historial de estado** del expediente en el que se encontraba cuando se envió (entidad externa de otro subsistema).

# Pantallas

| Fichero                                                                | Pantalla                  | Para qué sirve                                                                                                    |
| ---------------------------------------------------------------------- | ------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| [screen-correos-administracion.md](./screen-correos-administracion.md) | Administración de correos | El administrador crea correos para cualquier centro, consulta todos los correos y reenvía los fallidos.           |
| [screen-correos-centro.md](./screen-correos-centro.md)                 | Correos del centro        | El supervisor consulta los correos de su centro en cualquier estado y reintenta los fallidos.                     |
| [screen-mis-correos.md](./screen-mis-correos.md)                       | Mis correos               | Cualquier usuario consulta, en solo lectura, los correos que se le han enviado con éxito y descarga sus adjuntos. |

# Seguridad

- **Administrador:** ve, crea y reenvía correos de **todos los centros**. Puede crear correos para cualquier centro y descargar sus adjuntos. No puede editar los datos de un correo ya creado ni borrar correos.
- **Supervisor:** ve los correos de **su propio centro** (en cualquier estado), con sus datos de fallo, y puede reintentar el envío de los que están en FAIL y descargar sus adjuntos. No puede crear, editar ni borrar correos, ni ver los de otros centros.
- **Destinatario (cualquier usuario autenticado):** ve únicamente **sus propios** correos —aquellos cuyo DNI de destinatario coincide con el suyo— y solo los que están en estado SUCCESS, en solo lectura, pudiendo descargar sus adjuntos. No ve los correos en estado PENDIENTE ni FAIL, ni sus datos de fallo, y no puede crear, editar, reenviar ni borrar.

# Recursos y datos iniciales

Este subsistema no aporta datos iniciales propios. Se apoya en los datos de demo existentes que se precargan al arrancar: el usuario **administrador** global (login «admin», contraseña «admin»), los **centros** «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165), las **cuentas de supervisor** de cada centro (p. ej. «supervisor1@mislata.es», contraseña «demo1234») y las **cuentas de usuario** con su DNI (p. ej. «alumno1@mislata.es», con DNI «86862719E», contraseña «demo1234»). El envío real de correo depende de la configuración de correo saliente del entorno.

# Fuera de alcance

- El **reenvío automático periódico** de los correos fallidos o pendientes: se abordará más adelante.
- La gestión de centros, cuentas de usuario y sus DNI (pertenece a la gestión de centro).
- Otros canales de notificación distintos del correo electrónico.
