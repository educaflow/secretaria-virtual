# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Datos de demo ya precargados por otros subsistemas (gestión de centro), de los que parten **todos** los tests:

- Centros: «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165).
- Usuario administrador global, con acceso a cualquier centro.
- Cuenta de supervisor de «CIPFP Mislata»: `supervisor1@mislata.es`.
- Cuentas de usuario con DNI: `alumno1@mislata.es` (DNI «86862719E», del centro «CIPFP Mislata») y `alumno2@mislata.es` (DNI «03532821K», del centro «CIPFP Mislata»).

**Usuarios de acceso**:

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — (cualquier centro) |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata |
| alumno1@mislata.es | demo1234 | Destinatario (DNI 86862719E) | CIPFP Mislata |
| alumno2@mislata.es | demo1234 | Destinatario (DNI 03532821K) | CIPFP Mislata |

---

## T-001 — Alta de un correo que se envía con éxito

**Origen ESC:** ESC-001
**Verifica:** V-Correo-001, V-Correo-003, V-Correo-004, V-Correo-005, V-Correo-009, V-Correo-011, V-Correo-012, R-Correo-001, R-Correo-003, R-Correo-004, U-correos-administracion-formulario-002, U-correos-administracion-formulario-005, U-correos-administracion-formulario-008
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** happy

### Precondiciones
- El administrador ha iniciado sesión (usuario «admin», contraseña «admin»).

### Pasos
1. **Dado** que el administrador está en la pantalla "Administración de correos".
2. **Cuando** pulsa "Nuevo correo".
3. **Y** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «Le esperamos el lunes a las 9:00» y elige el centro «CIPFP Mislata».
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema crea el correo y lo muestra en estado "Pendiente" (o ya "Enviado" si el envío asíncrono ha terminado), con la fecha de creación registrada.
6. **Y**, recargando el detalle del correo poco después, el correo aparece en estado "Enviado" ("SUCCESS"), con la fecha de envío rellena y el botón "Reenviar" ausente.

### Resultado esperado
- El correo «Convocatoria de reunión» queda en estado SUCCESS, con fecha de envío registrada y número de reintentos igual a 1.

---

## T-002 — Alta de un correo con adjunto

**Origen ESC:** ESC-002
**Verifica:** V-Adjunto-004, V-Adjunto-005, U-correos-administracion-listado-adjuntos-001, U-correos-administracion-formulario-adjunto-001, R-Correo-001
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** happy

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador está en la pantalla "Administración de correos" y pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto», el cuerpo «Adjunto el documento solicitado» y elige el centro «CIPFP Mislata».
3. **Y**, en el panel de adjuntos, pulsa "Añadir adjunto".
4. **Y** rellena el nombre del fichero con «documento.pdf» y sube un fichero como contenido.
5. **Y** pulsa "Guardar" del adjunto.
6. **Y** pulsa "Guardar" en el correo.
7. **Entonces** el sistema crea el correo con un adjunto llamado «documento.pdf» asociado.

### Resultado esperado
- El correo «Documento adjunto» existe con un adjunto «documento.pdf» y se intenta su envío asíncrono.

---

## T-003 — Alta sin el DNI del destinatario

**Origen ESC:** ESC-003
**Verifica:** V-Correo-001
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto», elige el centro «CIPFP Mislata» y deja vacío el DNI del destinatario.
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El DNI del destinatario es obligatorio" y no crea el correo.

---

## T-004 — Alta sin destinatario en el «para»

**Origen ESC:** ESC-010
**Verifica:** V-Correo-005
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el asunto «Convocatoria de reunión», el cuerpo «texto», elige el centro «CIPFP Mislata» y deja vacío el «para».
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "Debe indicar al menos un destinatario en el «para»" y no crea el correo.

---

## T-005 — Alta sin asunto

**Origen ESC:** ESC-011
**Verifica:** V-Correo-009
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el cuerpo «texto», elige el centro «CIPFP Mislata» y deja vacío el asunto.
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El asunto es obligatorio" y no crea el correo.

---

## T-006 — Alta sin cuerpo

**Origen ESC:** ESC-012
**Verifica:** V-Correo-011
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», elige el centro «CIPFP Mislata» y deja vacío el cuerpo.
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El cuerpo es obligatorio" y no crea el correo.

---

## T-007 — Alta sin centro

**Origen ESC:** ESC-013
**Verifica:** V-Correo-012
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto» y no elige ningún centro.
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El centro es obligatorio" y no crea el correo.

---

## T-008 — Alta sin el nombre

**Origen ESC:** ESC-014
**Verifica:** V-Correo-003
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto», elige el centro «CIPFP Mislata» y deja vacío el nombre.
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El nombre es obligatorio" y no crea el correo.

---

## T-009 — Alta sin los apellidos

**Origen ESC:** ESC-015
**Verifica:** V-Correo-004
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto», elige el centro «CIPFP Mislata» y deja vacíos los apellidos.
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "Los apellidos son obligatorios" y no crea el correo.

---

## T-010 — Alta con adjunto sin nombre de fichero

**Origen ESC:** ESC-016
**Verifica:** V-Adjunto-004, U-correos-administracion-formulario-adjunto-005
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo" y rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto», el cuerpo «Adjunto el documento solicitado» y elige el centro «CIPFP Mislata».
2. **Cuando**, en el panel de adjuntos, pulsa "Añadir adjunto".
3. **Y** sube un fichero como contenido y deja vacío el nombre del fichero.
4. **Y** pulsa "Guardar" del adjunto.
5. **Y** pulsa "Guardar" en el correo.

### Resultado esperado
- El sistema muestra el mensaje "El nombre del fichero es obligatorio" y no crea el correo.

---

## T-011 — Alta con adjunto sin contenido

**Origen ESC:** ESC-017
**Verifica:** V-Adjunto-005, U-correos-administracion-formulario-adjunto-006
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo" y rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto», el cuerpo «Adjunto el documento solicitado» y elige el centro «CIPFP Mislata».
2. **Cuando**, en el panel de adjuntos, pulsa "Añadir adjunto".
3. **Y** rellena el nombre del fichero con «documento.pdf» y deja vacío el contenido (no sube ningún fichero).
4. **Y** pulsa "Guardar" del adjunto.
5. **Y** pulsa "Guardar" en el correo.

### Resultado esperado
- El sistema muestra el mensaje "Debe adjuntar el fichero" y no crea el correo.

---

## T-012 — Alta con «para» de formato inválido

**Origen ESC:** ESC-020
**Verifica:** V-Correo-006
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «direccion-sin-arroba», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata».
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)" y no crea el correo.

---

## T-013 — Alta con el DNI del destinatario inválido

**Origen ESC:** ESC-021
**Verifica:** V-Correo-002
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «12345678A» (letra de control incorrecta), el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata».
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El DNI del destinatario no es válido; compruebe la letra" y no crea el correo.

---

## T-014 — Alta con dos adjuntos con el mismo nombre de fichero

**Origen ESC:** ESC-022
**Verifica:** V-Adjunto-006
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo" y rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto», el cuerpo «texto» y elige el centro «CIPFP Mislata».
2. **Cuando**, en el panel de adjuntos, pulsa "Añadir adjunto", rellena el nombre del fichero «documento.pdf», sube un fichero como contenido y pulsa "Guardar" del adjunto.
3. **Y** pulsa de nuevo "Añadir adjunto", rellena otra vez el nombre del fichero «documento.pdf», sube un fichero como contenido y pulsa "Guardar" del adjunto.
4. **Y** pulsa "Guardar" en el correo.

### Resultado esperado
- El sistema muestra el mensaje "Ya existe un adjunto con ese nombre en el correo" y no crea el correo.

---

## T-015 — Alta con el asunto demasiado largo

**Origen ESC:** ESC-023
**Verifica:** V-Correo-010
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», un asunto con la letra «A» repetida 256 veces, el cuerpo «texto» y elige el centro «CIPFP Mislata».
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El asunto no puede superar 255 caracteres" y no crea el correo.

---

## T-016 — Alta con «en copia» de formato inválido

**Origen ESC:** ESC-024
**Verifica:** V-Correo-007
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el «en copia» «copia-sin-arroba», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata».
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El «en copia» debe contener direcciones de correo válidas" y no crea el correo.

---

## T-017 — Alta con «en copia oculta» de formato inválido

**Origen ESC:** ESC-025
**Verifica:** V-Correo-008
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** error

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el «en copia oculta» «oculta-sin-arroba», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata».
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El «en copia oculta» debe contener direcciones de correo válidas" y no crea el correo.

---

## T-018 — Reenvío de un correo en FAIL desde Administración

**Origen ESC:** ESC-004
**Verifica:** V-Correo-017, R-Correo-002, U-correos-administracion-formulario-001
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** happy

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo" y rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «destino-invalido@example.com», el asunto «Aviso», el cuerpo «texto» y elige el centro «CIPFP Mislata».
2. **Cuando** pulsa "Guardar".
3. **Y** recarga el listado de correos hasta que el correo «Aviso» aparezca en estado "Fallido".
4. **Y** abre el detalle del correo «Aviso», donde aparece el botón "Reenviar".
5. **Y** pulsa "Reenviar".

### Resultado esperado
- El correo permanece en estado FAIL, con el número de reintentos incrementado de 1 a 2 y la fecha del último intento actualizada.

---

## T-019 — El botón Reenviar no aparece si el correo no ha fallado

**Origen ESC:** ESC-005
**Verifica:** U-correos-administracion-formulario-001
**Pantalla principal:** screen-correos-administracion.md
**Tipo:** UI

### Precondiciones
- El administrador ha iniciado sesión.

### Pasos
1. **Dado** que el administrador pulsa "Nuevo correo" y rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Correo correcto», el cuerpo «texto» y elige el centro «CIPFP Mislata».
2. **Cuando** pulsa "Guardar".
3. **Y** recarga el listado de correos hasta que el correo «Correo correcto» aparezca en estado "Enviado".
4. **Y** abre el detalle de ese correo.

### Resultado esperado
- El sistema no muestra el botón "Reenviar".

---

## T-020 — El supervisor solo ve los correos de su centro

**Origen ESC:** ESC-006
**Verifica:** — (permiso `Correo.propio-centro-supervisor`, ver design.md paso 10)
**Pantalla principal:** screen-correos-centro.md
**Tipo:** happy

### Precondiciones
- Ninguna más allá del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador ha iniciado sesión, pulsa "Nuevo correo", rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Aviso Mislata», el cuerpo «texto», elige el centro «CIPFP Mislata» y pulsa "Guardar".
2. **Y** pulsa "Nuevo correo" de nuevo, rellena los mismos datos pero con el asunto «Aviso Batoi», elige el centro «CIPFP Batoi» y pulsa "Guardar".
3. **Y** cierra sesión.
4. **Cuando** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
5. **Y** abre la pantalla "Correos de mi centro".

### Resultado esperado
- El sistema muestra el correo «Aviso Mislata» y no muestra el correo «Aviso Batoi».

---

## T-021 — El supervisor reintenta un correo fallido de su centro

**Origen ESC:** ESC-007
**Verifica:** V-Correo-018, R-Correo-002, U-correos-centro-formulario-001, U-correos-centro-formulario-002, U-correos-centro-formulario-004
**Pantalla principal:** screen-correos-centro.md
**Tipo:** happy

### Precondiciones
- Ninguna más allá del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador ha iniciado sesión, pulsa "Nuevo correo", rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «destino-invalido@example.com», el asunto «Aviso», el cuerpo «texto», elige el centro «CIPFP Mislata» y pulsa "Guardar".
2. **Y** cierra sesión.
3. **Cuando** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre la pantalla "Correos de mi centro".
4. **Y** recarga el listado hasta ver el correo «Aviso» en estado "Fallido" y abre su detalle, donde ve la descripción del fallo, las fechas de intento y el botón "Reenviar".
5. **Y** pulsa "Reenviar".

### Resultado esperado
- El sistema muestra un aviso breve de que el reenvío está en curso; el correo permanece en estado FAIL con el número de reintentos incrementado de 1 a 2.

---

## T-022 — El supervisor descarga el adjunto de un correo de su centro

**Origen ESC:** ESC-018
**Verifica:** U-correos-administracion-formulario-adjunto-007
**Pantalla principal:** screen-correos-centro.md
**Tipo:** happy

### Precondiciones
- Ninguna más allá del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador ha iniciado sesión, pulsa "Nuevo correo", rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Circular con adjunto», el cuerpo «texto» y elige el centro «CIPFP Mislata».
2. **Y**, en el panel de adjuntos, pulsa "Añadir adjunto", rellena el nombre del fichero «circular.pdf», sube un fichero como contenido y pulsa "Guardar" del adjunto.
3. **Y** pulsa "Guardar" en el correo y cierra sesión.
4. **Cuando** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre la pantalla "Correos de mi centro".
5. **Y** recarga el listado hasta ver el correo «Circular con adjunto» en estado "Enviado" y abre su detalle; en el panel de adjuntos aparece «circular.pdf».
6. **Y** pulsa la fila del adjunto «circular.pdf» para abrir su formulario de detalle.
7. **Y** descarga el fichero del campo contenido.

### Resultado esperado
- El sistema descarga el fichero «circular.pdf».

---

## T-023 — El destinatario consulta un correo enviado con éxito y descarga su adjunto

**Origen ESC:** ESC-008
**Verifica:** — (permiso `Correo.propio-destinatario`/`Adjunto.propio-destinatario`, ver design.md paso 10)
**Pantalla principal:** screen-mis-correos.md
**Tipo:** happy

### Precondiciones
- Ninguna más allá del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador ha iniciado sesión y crea un correo dirigido al DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «alumno1@mislata.es», asunto «Tu certificado», cuerpo «Adjunto tu certificado», un adjunto llamado «certificado.pdf» y centro «CIPFP Mislata», y cierra sesión.
2. **Cuando** el usuario «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
3. **Y** abre la pantalla "Mis correos".
4. **Y** pulsa la fila del correo «Tu certificado» para abrir su detalle.
5. **Y**, en el panel de adjuntos, descarga el adjunto «certificado.pdf».

### Resultado esperado
- El sistema muestra el correo «Tu certificado» en solo lectura, con su asunto, cuerpo y fecha de envío, y descarga el fichero «certificado.pdf».

---

## T-024 — El destinatario no ve los correos no enviados

**Origen ESC:** ESC-009
**Verifica:** — (permiso `Correo.propio-destinatario`, condición `estado = 'SUCCESS'`)
**Pantalla principal:** screen-mis-correos.md
**Tipo:** error

### Precondiciones
- Ninguna más allá del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador ha iniciado sesión y crea un correo dirigido al DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «destino-invalido@example.com», asunto «No entregado» y cuerpo «texto», y cierra sesión.
2. **Cuando** el usuario «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
3. **Y** abre la pantalla "Mis correos".

### Resultado esperado
- El sistema no muestra el correo «No entregado».

---

## T-025 — El destinatario no ve un correo enviado con éxito a otra persona

**Origen ESC:** ESC-019
**Verifica:** — (permiso `Correo.propio-destinatario`, condición `dniDestinatario = __user__.dni`)
**Pantalla principal:** screen-mis-correos.md
**Tipo:** error

### Precondiciones
- Ninguna más allá del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador ha iniciado sesión y crea un correo dirigido al DNI «03532821K», nombre «Alumno2», apellidos «CIPFP Mislata», «para» «alumno2@mislata.es», asunto «Aviso para Alumno2» y cuerpo «texto», en el centro «CIPFP Mislata», y cierra sesión.
2. **Cuando** el usuario «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
3. **Y** abre la pantalla "Mis correos".

### Resultado esperado
- El sistema no muestra el correo «Aviso para Alumno2», porque su DNI de destinatario no coincide con el del usuario.
