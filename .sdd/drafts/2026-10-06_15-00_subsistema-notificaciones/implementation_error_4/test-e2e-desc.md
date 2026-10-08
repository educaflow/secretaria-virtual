# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

Carpeta de persistencia: `src/test/e2e/subsystem/notificaciones/` (no existe todavía: la numeración empieza en `T-001`).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Base de datos recién reseteada con los datos de demo: **ninguna notificación precargada** (cada test crea las suyas).
- Centros «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165).
- Usuarios de demo con sus tipos de usuario y cargos en cada centro (tabla de abajo).
- Permisos del subsistema cargados por su `data-init` (`auth-notificaciones.xml`) y referenciados desde el grupo `users`.
- Sin configuración real de correo ni de SMS: el envío puede acabar «Enviado» o «Fallido»; por eso los tests que dependen del resultado tienen ramas «si está Enviado / si está Fallido».
- **Ningún certificado de firma en el servidor precargado** para los usuarios de demo (los certificados de demo los aporta otra iniciativa, aún no fusionada).
  Sin certificado, la justificación solo ofrece «Firmar con AutoFirma y Presentar la solicitud», que necesita el cliente de escritorio AutoFirma y no se puede pilotar desde el navegador.
  Por eso los tests que presentan una justificación dan de alta antes, desde el navegador, el **Certificado de firma del director** (valor reutilizable de abajo), y entonces aparece «Firmar y Presentar la solicitud», que firma en el servidor.
- Trámite «Justificación de falta del profesorado» disponible para el profesor «director@mislata.es» en «CIPFP Mislata».

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro | DNI |
|---|---|---|---|---|
| admin | admin | Administrador | — (todos) | — |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata | 30647328N |
| supervisordoscentros@mislata.es | demo1234 | Supervisor | CIPFP Mislata y CIPFP Batoi | 39517284H |
| administrativo1@mislata.es | demo1234 | Administrativo | CIPFP Mislata | 97879650E |
| director@mislata.es | demo1234 | Profesor con cargo Director | CIPFP Mislata | 85432016B |
| jefeestudios1@mislata.es | demo1234 | Profesor con cargo Jefe de estudios | CIPFP Mislata | 15519084H |
| secretario@mislata.es | demo1234 | Profesor con cargo Secretario | CIPFP Mislata | 29050788V |
| vicesecretario@mislata.es | demo1234 | Profesor con cargo Vicesecretario | CIPFP Mislata | 56412323Q |
| profesor1@mislata.es | demo1234 | Profesor | CIPFP Mislata | 12345678Z |
| alumno1@mislata.es | demo1234 | Alumno | CIPFP Mislata | 86862719E |
| alumno2@mislata.es | demo1234 | Alumno | CIPFP Mislata | 03532821K |
| familiar1@mislata.es | demo1234 | Familiar | CIPFP Mislata | 43145636M |

**Valores de entrada reutilizables** (no son estado; los tests los citan por su nombre):

- **Correo de referencia**: centro «CIPFP Mislata», motivo «Aviso de prueba de correo», DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «alumno1@mislata.es», asunto «Reunión de inicio de curso», cuerpo «La reunión será el lunes a las 10:00.».
- **SMS de referencia**: centro «CIPFP Mislata», motivo «Aviso de prueba de SMS», DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», teléfono «600111222», mensaje «Mañana no hay clase».
- **Alta de un correo**: en «Notificaciones» → «Todas» pulsar «Nueva notificación», elegir «Correo», pulsar «Continuar», rellenar los datos indicados y pulsar «Guardar».
- **Alta de un SMS**: ídem eligiendo «SMS».
- **Certificado de firma del director**: el administrador inicia sesión, abre «Criptografía» → «Certificados digitales», pulsa nuevo, rellena el DNI «85432016B», el nombre «Director» y los apellidos «CIPFP Mislata», en «Tipo de certificado» elige «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Nueva contraseña» con «nadanada», deja el certificado habilitado, pulsa «Guardar» y cierra sesión.
- **Presentar una justificación**: con el **Certificado de firma del director** ya dado de alta en esa base de datos (si no lo está, se da de alta primero), el profesor «director@mislata.es» abre «Mis trámites» → «Nuevo trámite», elige «Justificación de falta del profesorado», crea un expediente en «CIPFP Mislata», en «Datos de la falta» elige «Un día completo», en «Fecha» pone **la fecha de hoy** (el trámite solo admite una fecha de los últimos 7 días y no futura, así que nunca una fecha fija), «Motivo falta» «Traslado de domicilio», adjunta «justificante.pdf» en «Foto o PDF del justificante», pulsa «Siguiente», pulsa «Firmar y Presentar la solicitud» (la firma en el servidor; si pide la clave del certificado, «nadanada»), confirma el aviso y anota el número del expediente, que queda en «Verificación» / «Pendiente de verificación».
  **Dependencia declarada:** si tras dar de alta el certificado sigue apareciendo solo «Firmar con AutoFirma y Presentar la solicitud», la precondición no es alcanzable desde el navegador: los tests que presentan una justificación (T-028, T-029, T-075 y T-076) dependen de la firma en el servidor y no son automatizables hasta que se fusione la iniciativa que aporta los certificados de demo; **MUST NOT** pulsarse el botón de AutoFirma.

---

## T-001 — Alta de un correo y resultado del envío

**Origen ESC:** ESC-001
**Verifica:** R-Notificacion-001, R-Notificacion-002, R-Notificacion-003, U-notificaciones-todas-005, U-notificaciones-todas-006, U-notificaciones-todas-007
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión con «admin»/«admin».
2. **Cuando** abre «Notificaciones» → «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
3. **Y** rellena el correo de referencia y pulsa «Guardar».
4. **Entonces** el sistema vuelve al listado y muestra la notificación con tipo «Correo», motivo «Aviso de prueba de correo», destino «alumno1@mislata.es» y centro «CIPFP Mislata».
5. **Y** pasados unos segundos recarga el listado.
6. **Y** si la notificación está «Fallido», pulsa su fila.

### Resultado esperado
- Si está «Enviado»: tiene fecha de envío.
- Si está «Fallido»: no tiene fecha de envío y, al abrirla, el panel «Datos del envío» muestra la descripción del último fallo rellena.

---

## T-002 — Alta de un correo con un adjunto

**Origen ESC:** ESC-002
**Verifica:** R-Correo-001, R-Adjunto-001, U-notificaciones-todas-015, U-notificaciones-todas-016
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión.
2. **Cuando** abre «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
3. **Y** rellena el correo de referencia con el motivo «Envío de horario», el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
4. **Y** en el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «horario.pdf», sube «horario.pdf» y pulsa «Guardar» en la ventana del adjunto.
5. **Y** pulsa «Guardar» en el formulario del correo.
6. **Y** pulsa la fila «Envío de horario».

### Resultado esperado
- El listado muestra «Envío de horario» con tipo «Correo» y destino «alumno1@mislata.es».
- El formulario del correo muestra el adjunto «horario.pdf» en el panel «Adjuntos».

---

## T-003 — Cancelar la elección de canal

**Origen ESC:** ESC-003
**Verifica:** —
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre «Notificaciones» → «Todas».
2. **Cuando** pulsa «Nueva notificación».
3. **Y** en la ventana de elección de canal pulsa «Cancelar».

### Resultado esperado
- La ventana se cierra, se ve el listado y no se ha creado ninguna notificación (el listado sigue vacío).

---

## T-004 — Alta de un correo sin motivo

**Origen ESC:** ESC-004
**Verifica:** V-Notificacion-001, U-notificaciones-todas-010
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
2. **Cuando** rellena el correo de referencia con el asunto «Aviso» y el cuerpo «Texto», y deja el motivo vacío.
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El motivo es obligatorio» y no crea la notificación.

---

## T-005 — Alta de un correo con un motivo demasiado largo

**Origen ESC:** ESC-005
**Verifica:** V-Notificacion-002
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia con un motivo de 256 caracteres (la letra «a» repetida 256 veces).
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El motivo no puede superar 255 caracteres» y no crea la notificación.

---

## T-006 — Alta de un correo con varias direcciones en el «para»

**Origen ESC:** ESC-006
**Verifica:** V-Correo-002
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia con el «para» «alumno1@mislata.es, alumno2@mislata.es».
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El «para» debe contener una sola dirección de correo; use «en copia» para añadir más destinatarios» y no crea la notificación.

---

## T-007 — Alta de un correo sin el DNI del destinatario

**Origen ESC:** ESC-007
**Verifica:** V-Notificacion-003
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia y deja el DNI vacío.
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El DNI del destinatario es obligatorio» y no crea la notificación.

---

## T-008 — Alta de un correo con el DNI no válido

**Origen ESC:** ESC-008
**Verifica:** V-Notificacion-004
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia con el DNI «86862719A».
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El DNI del destinatario no es válido; compruebe la letra» y no crea la notificación.

---

## T-009 — Alta de un correo sin el nombre

**Origen ESC:** ESC-009
**Verifica:** V-Notificacion-005
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia y deja el nombre vacío.
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El nombre es obligatorio» y no crea la notificación.

---

## T-010 — Alta de un correo sin los apellidos

**Origen ESC:** ESC-010
**Verifica:** V-Notificacion-006
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia y deja los apellidos vacíos.
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «Los apellidos son obligatorios» y no crea la notificación.

---

## T-011 — Alta de un correo sin destinatario en el «para»

**Origen ESC:** ESC-011
**Verifica:** V-Correo-001
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia y deja el «para» vacío.
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «Debe indicar al menos un destinatario en el «para»» y no crea la notificación.

---

## T-012 — Alta de un correo con el «para» de formato no válido

**Origen ESC:** ESC-012
**Verifica:** V-Correo-003
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia con el «para» «alumno1-mislata.es».
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)» y no crea la notificación.

---

## T-013 — Alta de un correo con el «en copia» de formato no válido

**Origen ESC:** ESC-013
**Verifica:** V-Correo-004
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia y además el «en copia» «familiar1-mislata.es».
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El «en copia» debe contener direcciones de correo válidas» y no crea la notificación.

---

## T-014 — Alta de un correo con el «en copia oculta» de formato no válido

**Origen ESC:** ESC-014
**Verifica:** V-Correo-005
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia y además el «en copia oculta» «familiar1-mislata.es».
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El «en copia oculta» debe contener direcciones de correo válidas» y no crea la notificación.

---

## T-015 — Alta de un correo sin asunto

**Origen ESC:** ESC-015
**Verifica:** V-Correo-006
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia y deja el asunto vacío.
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El asunto es obligatorio» y no crea la notificación.

---

## T-016 — Alta de un correo con el asunto demasiado largo

**Origen ESC:** ESC-016
**Verifica:** V-Correo-007
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia con un asunto de 256 caracteres (la letra «a» repetida 256 veces).
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El asunto no puede superar 255 caracteres» y no crea la notificación.

---

## T-017 — Alta de un correo sin cuerpo

**Origen ESC:** ESC-017
**Verifica:** V-Correo-009
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia y deja el cuerpo vacío.
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El cuerpo es obligatorio» y no crea la notificación.

---

## T-018 — Alta de un correo sin centro

**Origen ESC:** ESC-018
**Verifica:** V-Notificacion-009
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia sin elegir ningún centro.
3. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra «El centro es obligatorio» y no crea la notificación.

---

## T-019 — Añadir un adjunto sin nombre de fichero

**Origen ESC:** ESC-019
**Verifica:** V-Adjunto-002, U-notificaciones-todas-017
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** en el panel «Adjuntos» pulsa «Añadir adjunto», sube «horario.pdf» y deja vacío el nombre de fichero.
3. **Y** pulsa «Guardar» en la ventana del adjunto.

### Resultado esperado
- El sistema muestra «El nombre del fichero es obligatorio» y no añade el adjunto (la ventana sigue abierta).

---

## T-020 — Añadir un adjunto sin contenido

**Origen ESC:** ESC-020
**Verifica:** V-Adjunto-003, U-notificaciones-todas-018
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** en el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «horario.pdf» y no sube ningún fichero.
3. **Y** pulsa «Guardar» en la ventana del adjunto.

### Resultado esperado
- El sistema muestra «Debe adjuntar el fichero» y no añade el adjunto.

---

## T-021 — Alta de un correo con dos adjuntos con el mismo nombre

**Origen ESC:** ESC-021
**Verifica:** V-Adjunto-013
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre el alta de un correo y rellena el correo de referencia.
2. **Cuando** añade un adjunto con nombre «horario.pdf» y el fichero «horario.pdf» y pulsa «Guardar» en su ventana.
3. **Y** añade otro adjunto con nombre «horario.pdf» y el fichero «horario.pdf» y pulsa «Guardar» en su ventana.
4. **Y** pulsa «Guardar» en el formulario del correo.

### Resultado esperado
- El sistema muestra «Ya existe un adjunto con ese nombre en el correo» y no crea la notificación.

---

## T-022 — Adjunto cuyo nombre contiene una barra

**Origen ESC:** ESC-022
**Verifica:** V-Adjunto-009, U-notificaciones-todas-020
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre el alta de un correo y rellena el correo de referencia.
2. **Cuando** en el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «cursos/horario.pdf» y sube «horario.pdf».
3. **Y** pulsa «Guardar» en la ventana del adjunto.

### Resultado esperado
- La ventana del adjunto muestra «El nombre del fichero no puede contener los caracteres / \ ni caracteres de control» y no añade el adjunto.

---

## T-023 — La elección de canal no deja continuar hasta elegir uno

**Origen ESC:** ESC-058
**Verifica:** U-notificaciones-todas-001, U-notificaciones-todas-002
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre «Notificaciones» → «Todas».
2. **Cuando** pulsa «Nueva notificación».
3. **Entonces** la ventana de elección de canal aparece sin ningún canal elegido y con «Continuar» no disponible.
4. **Cuando** elige «Correo».
5. **Entonces** «Continuar» queda disponible.
6. **Cuando** pulsa «Continuar».

### Resultado esperado
- El sistema abre el formulario de alta del correo.

---

## T-024 — Formulario de alta del correo y cancelar el alta

**Origen ESC:** ESC-059
**Verifica:** U-notificaciones-todas-003, U-notificaciones-todas-005, U-notificaciones-todas-008
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre «Todas», pulsa «Nueva notificación», elige «Correo» y pulsa «Continuar».
2. **Entonces** el formulario muestra el tipo de notificación «Correo» en solo lectura, no muestra el panel «Datos del envío», muestra «Guardar» y «Cancelar» y no muestra «Salir» ni «Reenviar».
3. **Cuando** rellena el correo de referencia con el motivo «Correo cancelado» y el asunto «Aviso cancelado» y el cuerpo «Texto».
4. **Y** pulsa «Cancelar».

### Resultado esperado
- El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Correo cancelado».

---

## T-025 — Descartar y quitar adjuntos durante el alta del correo

**Origen ESC:** ESC-061
**Verifica:** U-notificaciones-todas-015, U-notificaciones-todas-021
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre el alta de un correo y rellena el correo de referencia con el motivo «Correo con adjunto quitado», el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
2. **Cuando** pulsa «Añadir adjunto», escribe «borrador.pdf», sube «horario.pdf» y pulsa «Cancelar» en la ventana del adjunto.
3. **Entonces** la ventana se cierra y el panel «Adjuntos» no muestra «borrador.pdf».
4. **Cuando** añade el adjunto «horario.pdf» (fichero «horario.pdf») y el adjunto «notas.pdf» (fichero «horario.pdf»), pulsando «Guardar» en cada ventana.
5. **Y** pulsa la fila «notas.pdf» y, en la ventana del adjunto, pulsa «Borrar».
6. **Entonces** el panel «Adjuntos» muestra «horario.pdf» y no «notas.pdf».
7. **Cuando** pulsa «Guardar» en el formulario del correo y pulsa la fila «Correo con adjunto quitado» del listado.

### Resultado esperado
- El panel «Adjuntos» muestra solo «horario.pdf» y no muestra el botón «Añadir adjunto».

---

## T-026 — Adjunto de más de 10 MB

**Origen ESC:** ESC-062
**Verifica:** U-notificaciones-todas-019, V-Adjunto-008
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.
- Un fichero «grande.pdf» de 11 MB disponible para subir.

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre el alta de un correo y rellena el correo de referencia con el motivo «Adjunto grande», el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
2. **Cuando** en el panel «Adjuntos» pulsa «Añadir adjunto», escribe el nombre de fichero «grande.pdf» e intenta subir «grande.pdf» (11 MB).

### Resultado esperado
- La plataforma avisa de que no se puede subir un fichero de más de 10 MB y el fichero no queda adjunto (cambio de spec APLICADO de D5: el aviso sale al subir, con el texto de la plataforma).

---

## T-027 — Alta de un correo con varias direcciones en copia y en copia oculta

**Origen ESC:** ESC-063
**Verifica:** V-Correo-004, V-Correo-005, V-Correo-010
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** rellena el correo de referencia con el motivo «Correo con copias», el «en copia» «familiar1@mislata.es, familiar2@mislata.es», el «en copia oculta» «supervisor1@mislata.es», el asunto «Aviso con copias» y el cuerpo «Texto», y pulsa «Guardar».
3. **Y** pulsa la fila «Correo con copias».

### Resultado esperado
- El listado muestra «Correo con copias» con el destino «alumno1@mislata.es».
- El formulario muestra el «en copia» «familiar1@mislata.es, familiar2@mislata.es» y el «en copia oculta» «supervisor1@mislata.es».

---

## T-028 — Alta de un correo ligado al estado de un expediente del mismo centro

**Origen ESC:** ESC-070
**Verifica:** V-Notificacion-011, U-notificaciones-todas-012
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el «Certificado de firma del director» y que el profesor «director@mislata.es» presenta una justificación (valores reutilizables), anota el número del expediente y cierra sesión.
2. **Cuando** el administrador inicia sesión y abre el alta de un correo.
3. **Y** elige el centro «CIPFP Mislata» y rellena el motivo «Aviso ligado a expediente», el DNI «85432016B», el nombre «Director», los apellidos «CIPFP Mislata», el «para» «director@mislata.es», el asunto «Su expediente» y el cuerpo «Texto».
4. **Y** pulsa la lupa del campo «Estado del expediente» y, en el buscador (columnas «Num. Exped.», «Expediente», «Fase», «Estado» y «Fecha»), elige la fila del número anotado con fase «Verificación» y estado «Pendiente de verificación»; el campo muestra «Pendiente de verificación».
5. **Y** pulsa «Guardar».

### Resultado esperado
- El listado muestra «Aviso ligado a expediente» y, en la columna «Expediente», el expediente de «Justificación de falta del profesorado» presentado por el director.

---

## T-029 — En el alta solo se ofrecen estados de expedientes del centro elegido

**Origen ESC:** ESC-071
**Verifica:** U-notificaciones-todas-012, U-notificaciones-todas-013
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el «Certificado de firma del director» y que el profesor «director@mislata.es» presenta una justificación en «CIPFP Mislata» (valores reutilizables), anota su número y cierra sesión.
2. **Cuando** el administrador inicia sesión, abre el alta de un correo, elige el centro «CIPFP Batoi» y rellena el motivo «Aviso mal ligado», el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el «para» «alumno1@batoi.es», el asunto «Aviso» y el cuerpo «Texto».
3. **Y** pulsa la lupa del campo «Estado del expediente» y, en el buscador, filtra por el número anotado en la columna «Num. Exped.».
4. **Entonces** no se ofrece ningún estado del expediente de «CIPFP Mislata» con ese número.
5. **Cuando** cierra el buscador, cambia el centro a «CIPFP Mislata», pulsa de nuevo la lupa del campo «Estado del expediente» y elige la fila del número anotado con estado «Pendiente de verificación».
6. **Y** cambia el centro a «CIPFP Batoi».

### Resultado esperado
- El estado del expediente elegido se vacía.

---

## T-030 — Alta de un SMS y resultado del envío

**Origen ESC:** ESC-023
**Verifica:** R-Sms-001, R-Notificacion-001, R-Notificacion-003, U-notificaciones-todas-025, U-notificaciones-todas-026
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión.
2. **Cuando** abre «Todas», pulsa «Nueva notificación», elige «SMS», pulsa «Continuar», rellena el SMS de referencia y pulsa «Guardar».
3. **Entonces** el listado muestra la notificación con tipo «SMS», motivo «Aviso de prueba de SMS», destino «+34600111222» y centro «CIPFP Mislata».
4. **Cuando** pasados unos segundos recarga el listado.

### Resultado esperado
- Si está «Enviado»: tiene fecha de envío.
- Si está «Fallido»: no tiene fecha de envío y, al abrirla, muestra la descripción del fallo.

---

## T-031 — Alta de un SMS con el teléfono ya en formato internacional

**Origen ESC:** ESC-024
**Verifica:** R-Sms-001, V-Sms-002
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia con el teléfono «+34 600 111 222» y pulsa «Guardar».

### Resultado esperado
- El SMS se guarda y el listado muestra el destino «+34600111222».

---

## T-032 — Alta de un SMS sin el DNI del destinatario

**Origen ESC:** ESC-025
**Verifica:** V-Notificacion-003, U-notificaciones-todas-029
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia, deja el DNI vacío y pulsa «Guardar».
3. **Entonces** el sistema muestra «El DNI del destinatario es obligatorio» y no crea la notificación.
4. **Cuando** pulsa «Cancelar».

### Resultado esperado
- El sistema vuelve al listado y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

---

## T-033 — Alta de un SMS con el DNI no válido

**Origen ESC:** ESC-026
**Verifica:** V-Notificacion-004
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia con el DNI «86862719A» y pulsa «Guardar».
3. **Entonces** el sistema muestra «El DNI del destinatario no es válido; compruebe la letra» y no crea la notificación.
4. **Cuando** pulsa «Cancelar».

### Resultado esperado
- El listado no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

---

## T-034 — Alta de un SMS sin nombre ni apellidos

**Origen ESC:** ESC-027
**Verifica:** V-Notificacion-005, V-Notificacion-006
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia, deja vacíos el nombre y los apellidos y pulsa «Guardar».
3. **Entonces** el sistema muestra «El nombre es obligatorio» y «Los apellidos son obligatorios», y no crea la notificación.
4. **Cuando** pulsa «Cancelar».

### Resultado esperado
- El listado no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

---

## T-035 — Alta de un SMS sin teléfono

**Origen ESC:** ESC-028
**Verifica:** V-Sms-001
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia, deja el teléfono vacío y pulsa «Guardar».
3. **Entonces** el sistema muestra «El teléfono es obligatorio» y no crea la notificación.
4. **Cuando** pulsa «Cancelar».

### Resultado esperado
- El listado no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

---

## T-036 — Alta de un SMS con un teléfono que no es un móvil de España

**Origen ESC:** ESC-029
**Verifica:** V-Sms-002
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia con el teléfono «961234567» y pulsa «Guardar».
3. **Entonces** el sistema muestra «El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)» y no crea la notificación.
4. **Cuando** pulsa «Cancelar».

### Resultado esperado
- El listado no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

---

## T-037 — Alta de un SMS sin mensaje

**Origen ESC:** ESC-030
**Verifica:** V-Sms-003
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia, deja el mensaje vacío y pulsa «Guardar».
3. **Entonces** el sistema muestra «El mensaje es obligatorio» y no crea la notificación.
4. **Cuando** pulsa «Cancelar».

### Resultado esperado
- El listado no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

---

## T-038 — Mensaje sin acentos en el límite de un SMS

**Origen ESC:** ESC-031
**Verifica:** V-Sms-004
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia con un mensaje de 161 letras «a» y pulsa «Guardar».
3. **Entonces** el sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no crea la notificación.
4. **Cuando** borra un carácter (quedan 160 letras «a») y pulsa «Guardar».

### Resultado esperado
- El sistema guarda el SMS y el listado muestra tipo «SMS», motivo «Aviso de prueba de SMS», destino «+34600111222» y centro «CIPFP Mislata».

---

## T-039 — Mensaje con acentos en el límite de un SMS

**Origen ESC:** ESC-032
**Verifica:** V-Sms-004
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia con un mensaje de 71 letras «á» y pulsa «Guardar».
3. **Entonces** el sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no crea la notificación.
4. **Cuando** borra un carácter (quedan 70 letras «á») y pulsa «Guardar».

### Resultado esperado
- El sistema guarda el SMS y el listado muestra tipo «SMS», motivo «Aviso de prueba de SMS», destino «+34600111222» y centro «CIPFP Mislata».

---

## T-040 — Alta de un SMS sin centro

**Origen ESC:** ESC-033
**Verifica:** V-Notificacion-009
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia sin elegir centro y pulsa «Guardar».
3. **Entonces** el sistema muestra «El centro es obligatorio» y no crea la notificación.
4. **Cuando** pulsa «Cancelar».

### Resultado esperado
- El listado no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

---

## T-041 — Formulario de alta del SMS y cancelar el alta

**Origen ESC:** ESC-060
**Verifica:** U-notificaciones-todas-022, U-notificaciones-todas-024, U-notificaciones-todas-027
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre «Todas», pulsa «Nueva notificación», elige «SMS» y pulsa «Continuar».
2. **Entonces** el formulario muestra el tipo de notificación «SMS» en solo lectura, sin el panel «Datos del envío», con «Guardar» y «Cancelar» y sin «Salir» ni «Reenviar».
3. **Cuando** rellena el SMS de referencia con el motivo «SMS cancelado» y pulsa «Cancelar».

### Resultado esperado
- El sistema vuelve al listado y no muestra ninguna notificación con el motivo «SMS cancelado».

---

## T-042 — Alta de un SMS con un mensaje solo de espacios

**Origen ESC:** ESC-066
**Verifica:** V-Sms-003
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y abre el alta de un SMS.
2. **Cuando** rellena el SMS de referencia con un mensaje de cinco espacios y pulsa «Guardar».
3. **Entonces** el sistema muestra «El mensaje es obligatorio» y no crea la notificación.
4. **Cuando** pulsa «Cancelar».

### Resultado esperado
- El listado no muestra ninguna notificación con el motivo «Aviso de prueba de SMS».

---

## T-043 — Listado común con correos y SMS de varios centros

**Origen ESC:** ESC-034
**Verifica:** R-Notificacion-001
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión.
2. **Cuando** da de alta un correo (correo de referencia con el motivo «Correo Mislata»).
3. **Y** da de alta un SMS del centro «CIPFP Batoi» con el motivo «SMS Batoi», DNI «65399546N», nombre «Alumno1», apellidos «CIPFP Batoi», teléfono «600222333» y mensaje «Mañana no hay clase».
4. **Y** abre «Notificaciones» → «Todas».

### Resultado esperado
- El listado muestra «Correo Mislata» con tipo «Correo», destino «alumno1@mislata.es» y centro «CIPFP Mislata», y «SMS Batoi» con tipo «SMS», destino «+34600222333» y centro «CIPFP Batoi».

---

## T-044 — Al abrir una fila se abre el formulario de su canal

**Origen ESC:** ESC-035
**Verifica:** U-notificaciones-todas-004, U-notificaciones-todas-023
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y da de alta el correo de referencia con el motivo «Correo de prueba» y el SMS de referencia con el motivo «SMS de prueba».
2. **Cuando** en «Todas» pulsa la fila «Correo de prueba».
3. **Entonces** se abre el formulario del correo en solo lectura con tipo «Correo», motivo «Correo de prueba», centro «CIPFP Mislata», DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «alumno1@mislata.es», «en copia» y «en copia oculta» vacíos, asunto «Reunión de inicio de curso», cuerpo «La reunión será el lunes a las 10:00.» y el panel «Adjuntos» sin adjuntos.
4. **Cuando** pulsa «Salir».
5. **Entonces** vuelve al listado de «Todas».
6. **Cuando** pulsa la fila «SMS de prueba».

### Resultado esperado
- Se abre el formulario del SMS con teléfono «+34600111222», mensaje «Mañana no hay clase» y motivo «SMS de prueba».

---

## T-045 — Datos del envío según el estado

**Origen ESC:** ESC-036
**Verifica:** U-notificaciones-todas-024, U-notificaciones-todas-025, U-notificaciones-todas-026, R-Notificacion-003
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y da de alta el SMS de referencia con el motivo «SMS estado».
2. **Cuando** pasados unos segundos pulsa en «Todas» la fila «SMS estado».

### Resultado esperado
- «Datos del envío» muestra el estado, el número de reintentos «1», la fecha de creación y las fechas del primer y del último intento.
- Si el estado es «Enviado»: muestra la fecha de envío y no la descripción del último fallo.
- Si el estado es «Fallido»: muestra la descripción del último fallo y no la fecha de envío.

---

## T-046 — Una notificación ya creada no se puede modificar ni borrar

**Origen ESC:** ESC-037
**Verifica:** V-Notificacion-012, V-Notificacion-013, U-notificaciones-todas-004, U-notificaciones-todas-008
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y da de alta el correo de referencia con el motivo «Correo inmutable».
2. **Cuando** en «Todas» pulsa la fila «Correo inmutable».

### Resultado esperado
- El formulario muestra motivo «Correo inmutable», centro «CIPFP Mislata», DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «alumno1@mislata.es», asunto «Reunión de inicio de curso» y cuerpo «La reunión será el lunes a las 10:00.», todos en solo lectura.
- Muestra el botón «Salir» y no los botones «Guardar», «Cancelar» ni «Borrar».

---

## T-047 — Buscar solo los SMS en el listado

**Origen ESC:** ESC-038
**Verifica:** —
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y da de alta el correo de referencia con el motivo «Correo búsqueda» y el SMS de referencia con el motivo «SMS búsqueda».
2. **Cuando** en «Todas» filtra la columna tipo por «SMS».

### Resultado esperado
- El listado muestra «SMS búsqueda» y no «Correo búsqueda».

---

## T-048 — Consultar y descargar un adjunto de un correo guardado

**Origen ESC:** ESC-067
**Verifica:** R-Correo-001, U-notificaciones-todas-015
**Pantalla principal:** screen-adjunto-consulta.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre el alta de un correo y rellena el correo de referencia con el motivo «Envío de horario», el asunto «Horario» y el cuerpo «Le adjuntamos su horario.».
2. **Cuando** en «Adjuntos» pulsa «Añadir adjunto», escribe «horario.pdf», sube «horario.pdf», pulsa «Guardar» en la ventana del adjunto y «Guardar» en el correo.
3. **Y** en el listado pulsa la fila «Envío de horario».
4. **Entonces** el panel «Adjuntos» muestra la fila «horario.pdf».
5. **Cuando** pulsa la fila «horario.pdf».
6. **Entonces** se abre el adjunto en solo lectura con el nombre de fichero «horario.pdf» y su fichero, sin «Guardar» ni «Borrar».
7. **Cuando** pulsa el fichero y lo descarga, y después pulsa «Salir».

### Resultado esperado
- El fichero descargado se llama «horario.pdf».
- El adjunto se cierra y vuelve a verse el formulario del correo «Envío de horario».

---

## T-049 — Un SMS ya creado no se puede modificar ni borrar

**Origen ESC:** ESC-068
**Verifica:** V-Notificacion-012, V-Notificacion-013, U-notificaciones-todas-023, U-notificaciones-todas-027
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y da de alta el SMS de referencia con el motivo «SMS inmutable».
2. **Cuando** en el listado pulsa la fila «SMS inmutable».

### Resultado esperado
- Todos los datos del SMS aparecen en solo lectura, con el botón «Salir» y sin «Guardar», «Cancelar» ni «Borrar».

---

## T-050 — El listado muestra primero las notificaciones más recientes

**Origen ESC:** ESC-069
**Verifica:** —
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión.
2. **Cuando** da de alta el correo de referencia con el motivo «Primer aviso», el asunto «Primero» y el cuerpo «Texto».
3. **Y** desde el mismo listado pulsa «Nueva notificación», elige «Correo», pulsa «Continuar» y da de alta el correo de referencia con el motivo «Segundo aviso», el asunto «Segundo» y el cuerpo «Texto».

### Resultado esperado
- El listado muestra la fila «Segundo aviso» por encima de «Primer aviso».

---

## T-051 — El administrador ve «Recibidas» y «Todas» pero no «Del centro»

**Origen ESC:** ESC-076
**Verifica:** —
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión.
2. **Cuando** abre el menú «Notificaciones».

### Resultado esperado
- Muestra los submenús «Recibidas» y «Todas» y no muestra «Del centro».

---

## T-052 — Reenvío de un correo desde «Todas»

**Origen ESC:** ESC-039
**Verifica:** V-Notificacion-014, V-Notificacion-015, R-Notificacion-004, U-notificaciones-todas-009, U-notificaciones-todas-011
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y da de alta el correo de referencia con el motivo «Correo a reenviar».
2. **Cuando** pasados unos segundos pulsa en «Todas» la fila «Correo a reenviar».
3. **Y** si está «Fallido», pulsa «Reenviar».
4. **Y** pasados unos segundos vuelve a abrir el correo «Correo a reenviar».

### Resultado esperado
- Si estaba «Fallido»: se ve el botón «Reenviar»; al pulsarlo aparece «El reenvío del correo se ha puesto en marcha.»; al volver a abrirlo, el número de reintentos es «2» y está «Enviado» con fecha de envío, o de nuevo «Fallido» con la descripción del nuevo fallo.
- Si estaba «Enviado»: no se ve el botón «Reenviar».

---

## T-053 — Reenvío de un SMS desde «Todas»

**Origen ESC:** ESC-040
**Verifica:** V-Notificacion-014, R-Notificacion-004, U-notificaciones-todas-028, U-notificaciones-todas-031, U-notificaciones-todas-035
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión y da de alta el SMS de referencia con el motivo «SMS a reenviar».
2. **Cuando** pasados unos segundos pulsa en «Todas» la fila «SMS a reenviar».
3. **Y** si está «Fallido», pulsa «Reenviar».
4. **Entonces** el sistema pide confirmación con «Reenviar un SMS tiene coste. ¿Desea reenviarlo?».
5. **Cuando** pulsa «Aceptar» y, pasados unos segundos, vuelve a abrir el SMS.

### Resultado esperado
- Si estaba «Fallido»: aparece «El reenvío del SMS se ha puesto en marcha.»; al volver a abrirlo, el número de reintentos es «2» y está «Enviado» con fecha de envío, o «Fallido» con la descripción del nuevo fallo.
- Si estaba «Enviado»: no se ve el botón «Reenviar».

---

## T-054 — El supervisor solo ve las notificaciones de su centro

**Origen ESC:** ESC-041
**Verifica:** —
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Aviso Mislata» y otro del centro «CIPFP Batoi» con el motivo «Aviso Batoi», DNI «65399546N», nombre «Alumno1», apellidos «CIPFP Batoi» y «para» «alumno1@batoi.es», y cierra sesión.
2. **Cuando** «supervisor1@mislata.es» inicia sesión y abre «Notificaciones» → «Del centro».

### Resultado esperado
- Se ve la fila con motivo «Aviso Mislata», tipo «Correo», destino «alumno1@mislata.es» y centro «CIPFP Mislata».
- No se ve ninguna fila con el motivo «Aviso Batoi».

---

## T-055 — El administrativo ve las notificaciones de su centro

**Origen ESC:** ESC-042
**Verifica:** —
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el SMS de referencia con el motivo «SMS Mislata» y cierra sesión.
2. **Cuando** «administrativo1@mislata.es» inicia sesión y abre «Notificaciones» → «Del centro».

### Resultado esperado
- Se ve «SMS Mislata».

---

## T-056 — El director ve las notificaciones de su centro

**Origen ESC:** ESC-043
**Verifica:** —
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Aviso para dirección» y otro de «CIPFP Batoi» con el motivo «Aviso Batoi», DNI «65399546N», nombre «Alumno1», apellidos «CIPFP Batoi» y «para» «alumno1@batoi.es», y cierra sesión.
2. **Cuando** «director@mislata.es» inicia sesión y abre «Notificaciones» → «Del centro».

### Resultado esperado
- Se ve «Aviso para dirección» y no «Aviso Batoi».

---

## T-057 — El jefe de estudios ve las notificaciones de su centro

**Origen ESC:** ESC-044
**Verifica:** —
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el SMS de referencia con el motivo «Aviso para jefatura» y cierra sesión.
2. **Cuando** «jefeestudios1@mislata.es» inicia sesión y abre «Notificaciones» → «Del centro».

### Resultado esperado
- Se ve «Aviso para jefatura».

---

## T-058 — El secretario ve las notificaciones de su centro

**Origen ESC:** ESC-045
**Verifica:** —
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Aviso para secretaría» y cierra sesión.
2. **Cuando** «secretario@mislata.es» inicia sesión y abre «Notificaciones» → «Del centro».

### Resultado esperado
- Se ve «Aviso para secretaría».

---

## T-059 — El supervisor de dos centros ve las de ambos y filtra por centro

**Origen ESC:** ESC-046
**Verifica:** —
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Aviso Mislata» y otro de «CIPFP Batoi» con el motivo «Aviso Batoi», DNI «65399546N», nombre «Alumno1», apellidos «CIPFP Batoi» y «para» «alumno1@batoi.es», y cierra sesión.
2. **Cuando** «supervisordoscentros@mislata.es» inicia sesión y abre «Notificaciones» → «Del centro».
3. **Entonces** se ven «Aviso Mislata» con centro «CIPFP Mislata» y «Aviso Batoi» con centro «CIPFP Batoi».
4. **Cuando** filtra la columna centro por «CIPFP Batoi».

### Resultado esperado
- Se ve «Aviso Batoi» y no «Aviso Mislata».

---

## T-060 — Un usuario sin tipo ni cargo de gestión no ve «Del centro»

**Origen ESC:** ESC-047
**Verifica:** —
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que «vicesecretario@mislata.es» ha iniciado sesión.
2. **Cuando** abre el menú «Notificaciones».
3. **Entonces** ve «Recibidas» y no ve «Del centro» ni «Todas».
4. **Cuando** cierra sesión, «profesor1@mislata.es» inicia sesión y abre el menú «Notificaciones».

### Resultado esperado
- Ve «Recibidas» y no ve «Del centro» ni «Todas».

---

## T-061 — El gestor del centro no puede dar de alta notificaciones

**Origen ESC:** ESC-048
**Verifica:** —
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que «supervisor1@mislata.es» ha iniciado sesión.
2. **Cuando** abre el menú «Notificaciones».
3. **Entonces** no ve el submenú «Todas».
4. **Cuando** abre «Notificaciones» → «Del centro».

### Resultado esperado
- No se ve el botón «Nueva notificación».

---

## T-062 — El gestor abre una notificación con sus adjuntos y descarga uno

**Origen ESC:** ESC-049
**Verifica:** R-Correo-001
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Envío de horario», el asunto «Horario», el cuerpo «Le adjuntamos su horario.» y el adjunto «horario.pdf», y cierra sesión.
2. **Cuando** «supervisor1@mislata.es» inicia sesión, abre «Del centro» y pulsa la fila «Envío de horario».
3. **Entonces** se abre el formulario del correo en solo lectura, sin «Guardar» ni «Borrar», con motivo «Envío de horario», centro «CIPFP Mislata», DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «alumno1@mislata.es», asunto «Horario», cuerpo «Le adjuntamos su horario.» y la fila «horario.pdf» en «Adjuntos».
4. **Cuando** pulsa la fila «horario.pdf».
5. **Entonces** se abre el adjunto en solo lectura con su nombre y su fichero, sin «Guardar» ni «Borrar».
6. **Cuando** pulsa el fichero.

### Resultado esperado
- Se descarga el fichero «horario.pdf».

---

## T-063 — El supervisor reenvía un correo fallido

**Origen ESC:** ESC-050
**Verifica:** V-Notificacion-015, U-notificaciones-centro-004, U-notificaciones-centro-005
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Correo a reenviar» y cierra sesión.
2. **Cuando** «supervisor1@mislata.es» inicia sesión, espera unos segundos, abre «Del centro» y pulsa la fila «Correo a reenviar».
3. **Y** si está «Fallido», pulsa «Reenviar» y, pasados unos segundos, vuelve a abrir el correo.

### Resultado esperado
- Si estaba «Fallido»: se ve «Reenviar», aparece «El reenvío del correo se ha puesto en marcha.» y, al volver a abrirlo, «Datos del envío» muestra reintentos «2» y «Enviado» (con fecha de envío) o «Fallido» (con la descripción del nuevo fallo).
- Si estaba «Enviado»: no se ve «Reenviar».

---

## T-064 — El director reenvía un SMS fallido

**Origen ESC:** ESC-051
**Verifica:** V-Notificacion-015, U-notificaciones-centro-010, U-notificaciones-centro-011, U-notificaciones-centro-012
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el SMS de referencia con el motivo «SMS a reenviar» y cierra sesión.
2. **Cuando** «director@mislata.es» inicia sesión, espera unos segundos, abre «Del centro» y pulsa la fila «SMS a reenviar».
3. **Y** si está «Fallido», pulsa «Reenviar».
4. **Entonces** el sistema pide confirmación con «Reenviar un SMS tiene coste. ¿Desea reenviarlo?».
5. **Cuando** pulsa «Aceptar» y, pasados unos segundos, vuelve a abrir el SMS.

### Resultado esperado
- Si estaba «Fallido»: aparece «El reenvío del SMS se ha puesto en marcha.» y, al volver a abrirlo, reintentos «2» y «Enviado» o «Fallido» con la descripción del nuevo fallo.
- Si estaba «Enviado»: no se ve «Reenviar».

---

## T-065 — El jefe de estudios reenvía un correo fallido

**Origen ESC:** ESC-072
**Verifica:** V-Notificacion-015, U-notificaciones-centro-004
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Correo para jefatura», el asunto «Aviso» y el cuerpo «Texto», y cierra sesión.
2. **Cuando** «jefeestudios1@mislata.es» inicia sesión, espera unos segundos, abre «Del centro» y pulsa la fila «Correo para jefatura».
3. **Y** si está «Fallido», pulsa «Reenviar» y, pasados unos segundos, vuelve a abrir el correo.

### Resultado esperado
- Si estaba «Fallido»: se ve «Reenviar», aparece «El reenvío del correo se ha puesto en marcha.» y después reintentos «2» y «Enviado» o «Fallido».
- Si estaba «Enviado»: no se ve «Reenviar».

---

## T-066 — El secretario reenvía un SMS fallido

**Origen ESC:** ESC-073
**Verifica:** V-Notificacion-015, U-notificaciones-centro-012
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el SMS de referencia con el motivo «SMS para secretaría» y cierra sesión.
2. **Cuando** «secretario@mislata.es» inicia sesión, espera unos segundos, abre «Del centro» y pulsa la fila «SMS para secretaría».
3. **Y** si está «Fallido», pulsa «Reenviar», confirma con «Aceptar» el aviso «Reenviar un SMS tiene coste. ¿Desea reenviarlo?» y, pasados unos segundos, vuelve a abrir el SMS.

### Resultado esperado
- Si estaba «Fallido»: aparece «El reenvío del SMS se ha puesto en marcha.» y después reintentos «2» y «Enviado» o «Fallido».
- Si estaba «Enviado»: no se ve «Reenviar».

---

## T-067 — El administrativo reenvía un correo fallido

**Origen ESC:** ESC-074
**Verifica:** V-Notificacion-015
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Correo para administración», el asunto «Aviso» y el cuerpo «Texto», y cierra sesión.
2. **Cuando** «administrativo1@mislata.es» inicia sesión, espera unos segundos, abre «Del centro» y pulsa la fila «Correo para administración».
3. **Y** si está «Fallido», pulsa «Reenviar» y, pasados unos segundos, vuelve a abrir el correo.

### Resultado esperado
- Si estaba «Fallido»: se ve «Reenviar», aparece «El reenvío del correo se ha puesto en marcha.» y después reintentos «2» y «Enviado» o «Fallido».
- Si estaba «Enviado»: no se ve «Reenviar».

---

## T-068 — El destinatario ve sus correos y SMS enviados, sin el motivo

**Origen ESC:** ESC-052
**Verifica:** —
**Pantalla principal:** screen-notificaciones-recibidas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Motivo interno correo» y el SMS de referencia con el motivo «Motivo interno SMS», y cierra sesión.
2. **Cuando** «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Notificaciones» → «Recibidas».

### Resultado esperado
- Cada una de las dos que esté «Enviado» aparece: el correo con tipo «Correo», destino «alumno1@mislata.es», expediente vacío y su fecha de envío; el SMS con tipo «SMS», destino «+34600111222», expediente vacío y su fecha de envío.
- Las que estén «Fallido» no aparecen.
- Ninguna fila muestra «Motivo interno correo» ni «Motivo interno SMS».

---

## T-069 — El destinatario abre un correo recibido y descarga su adjunto

**Origen ESC:** ESC-053
**Verifica:** U-notificaciones-recibidas-002, R-Correo-001
**Pantalla principal:** screen-notificaciones-recibidas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Envío de horario», el asunto «Horario», el cuerpo «Le adjuntamos su horario.» y el adjunto «horario.pdf», y cierra sesión.
2. **Cuando** «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».
3. **Y** si el correo está «Enviado», pulsa su fila y después la fila «horario.pdf» del panel «Adjuntos», y descarga el fichero.

### Resultado esperado
- Si está «Enviado»: el formulario se abre en solo lectura con asunto «Horario», cuerpo «Le adjuntamos su horario.», «para» «alumno1@mislata.es», sin el campo «en copia» (el correo no tiene copias), la fecha de envío y el adjunto «horario.pdf», sin el texto «Envío de horario» y sin «Reenviar»; se descarga «horario.pdf».
- Si no: el correo no aparece en «Recibidas».

---

## T-070 — El destinatario no ve las notificaciones de otra persona

**Origen ESC:** ESC-054
**Verifica:** —
**Pantalla principal:** screen-notificaciones-recibidas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el DNI «03532821K», el nombre «Alumno2», el «para» «alumno2@mislata.es» y el asunto «Aviso para Alumno2», y cierra sesión.
2. **Cuando** «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».
3. **Entonces** no aparece «Aviso para Alumno2».
4. **Cuando** cierra sesión y «alumno2@mislata.es» inicia sesión y abre «Recibidas».

### Resultado esperado
- Si el correo está «Enviado»: aparece con tipo «Correo» y destino «alumno2@mislata.es». Si no, no aparece.

---

## T-071 — El destinatario ve una notificación enviada desde otro centro

**Origen ESC:** ESC-055
**Verifica:** —
**Pantalla principal:** screen-notificaciones-recibidas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el SMS de referencia del centro «CIPFP Batoi» (DNI «86862719E») con el motivo «SMS desde Batoi», y cierra sesión.
2. **Cuando** «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».

### Resultado esperado
- Si el SMS está «Enviado»: aparece con tipo «SMS» y destino «+34600111222». Si no, no aparece.

---

## T-072 — Un familiar consulta sus notificaciones recibidas

**Origen ESC:** ESC-056
**Verifica:** —
**Pantalla principal:** screen-notificaciones-recibidas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el DNI «43145636M», el nombre «Familiar1», los apellidos «CIPFP Mislata», el «para» «familiar1@mislata.es» y el asunto «Aviso a familias», y cierra sesión.
2. **Cuando** «familiar1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».

### Resultado esperado
- Si el correo está «Enviado»: aparece con tipo «Correo» y destino «familiar1@mislata.es». Si no, no aparece.

---

## T-073 — El destinatario ve el «en copia» pero no el «en copia oculta» ni los datos del envío

**Origen ESC:** ESC-064
**Verifica:** U-notificaciones-recibidas-001
**Pantalla principal:** screen-notificaciones-recibidas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el correo de referencia con el motivo «Correo con copia oculta», el «en copia» «familiar1@mislata.es», el «en copia oculta» «supervisor1@mislata.es», el asunto «Aviso con copias» y el cuerpo «Texto», y cierra sesión.
2. **Cuando** «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».
3. **Y** si el correo está «Enviado», pulsa su fila.

### Resultado esperado
- Si está «Enviado»: el formulario muestra asunto «Aviso con copias», «para» «alumno1@mislata.es» y «en copia» «familiar1@mislata.es», y no muestra el «en copia oculta», el texto «supervisor1@mislata.es», el estado, el número de reintentos ni la descripción del último fallo.
- Si no: el correo no aparece en «Recibidas».

---

## T-074 — El destinatario abre un SMS recibido

**Origen ESC:** ESC-065
**Verifica:** —
**Pantalla principal:** screen-notificaciones-recibidas.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el SMS de referencia con el motivo «Motivo interno SMS abierto», y cierra sesión.
2. **Cuando** «alumno1@mislata.es» inicia sesión, espera unos segundos y abre «Recibidas».
3. **Y** si el SMS está «Enviado», pulsa su fila y después «Salir».

### Resultado esperado
- Si está «Enviado»: el formulario del SMS se abre en solo lectura con teléfono «+34600111222», la fecha de envío y el mensaje «Mañana no hay clase», sin el texto «Motivo interno SMS abierto», sin estado ni número de reintentos y sin «Reenviar»; «Salir» vuelve al listado de recibidas.
- Si no: el SMS no aparece en «Recibidas».

---

## T-075 — El aviso de subsanación queda registrado con su motivo

**Origen ESC:** ESC-057
**Verifica:** R-Correo-002, R-Notificacion-001
**Pantalla principal:** screen-notificaciones-centro.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el «Certificado de firma del director» y que el profesor «director@mislata.es» presenta una justificación (valores reutilizables), anota su número y cierra sesión.
2. **Cuando** «jefeestudios1@mislata.es» inicia sesión, abre «Tramitación» → «Pendientes de mí», pulsa la fila del expediente anotado, en «Resultado de la verificación» elige «Pedir subsanación», rellena «Qué hay que subsanar» con «Falta el justificante firmado» y pulsa «Siguiente».
3. **Entonces** el expediente vuelve a «Entrada» / «Entrada de datos».
4. **Cuando** el jefe de estudios cierra sesión, «supervisor1@mislata.es» inicia sesión y abre «Notificaciones» → «Del centro».
5. **Entonces** el listado muestra una notificación con centro «CIPFP Mislata», tipo «Correo», DNI «85432016B», motivo «Subsanación del expediente <número anotado>», destino «director@mislata.es» y, en «Expediente», el de «Justificación de falta del profesorado» presentado al principio.
6. **Cuando** pulsa esa fila.

### Resultado esperado
- Se abre el formulario del correo en solo lectura con «para» «director@mislata.es», asunto «Tiene que subsanar su solicitud del expediente <número anotado>» y un cuerpo que incluye «Falta el justificante firmado».

---

## T-076 — El solicitante ve el aviso de subsanación en sus recibidas

**Origen ESC:** ESC-075
**Verifica:** R-Correo-002
**Pantalla principal:** screen-notificaciones-recibidas.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador da de alta el «Certificado de firma del director» y que el profesor «director@mislata.es» presenta una justificación (valores reutilizables), anota su número y cierra sesión.
2. **Cuando** «jefeestudios1@mislata.es» pide la subsanación de ese expediente con el texto «Falta el justificante firmado» (como en T-075, paso 2) y el expediente vuelve a «Entrada» / «Entrada de datos».
3. **Y** el jefe de estudios espera unos segundos, abre «Notificaciones» → «Del centro», recarga y anota el estado de la notificación con motivo «Subsanación del expediente <número anotado>», y cierra sesión.
4. **Y** «director@mislata.es» inicia sesión y abre «Notificaciones» → «Recibidas».
5. **Y** si el estado anotado es «Enviado», pulsa la fila del aviso.

### Resultado esperado
- Si el estado anotado es «Enviado»: el listado muestra una fila con tipo «Correo», destino «director@mislata.es», en «Expediente» el de «Justificación de falta del profesorado» y fecha de envío; el formulario se abre en solo lectura con asunto «Tiene que subsanar su solicitud del expediente <número anotado>», un cuerpo que incluye «Falta el justificante firmado» y sin el texto «Subsanación del expediente <número anotado>».
- Si es «Fallido»: el aviso no aparece en «Recibidas».
