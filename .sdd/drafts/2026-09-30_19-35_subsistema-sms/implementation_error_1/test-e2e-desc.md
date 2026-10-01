# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

La numeración arranca en `T-001` porque `src/test/e2e/subsystem/sms/` no existe todavía (subsistema nuevo; la `**Capa:**` del diseño es `subsystem/sms`).

**Nota transversal sobre el proveedor de SMS:** el resultado de un intento de envío depende de un servicio externo, así que ningún test exige que un SMS acabe «Enviado». Lo que **sí** se exige siempre es que, pasados unos segundos, el SMS haya dejado de estar «Pendiente» y esté en «Enviado» **con** fecha de envío y **sin** descripción de fallo, o en «Fallido» **sin** fecha de envío y **con** descripción del fallo, con el número de reintentos correspondiente. Es exactamente como el spec redacta sus escenarios.

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas: gestión de centro y usuarios) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Centros: «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165).
- Usuario administrador global, con acceso a cualquier centro.
- Cuentas de gestión de «CIPFP Mislata»: un Supervisor (`supervisor1@mislata.es`) y un Administrativo (`administrativo1@mislata.es`).
- Cuenta de Supervisor de los dos centros a la vez: `supervisordoscentros@mislata.es`.
- Cuentas de alumno con DNI, sin ningún cargo de gestión: `alumno1@mislata.es` (DNI «86862719E», de «CIPFP Mislata») y `alumno2@mislata.es` (DNI «03532821K», de «CIPFP Mislata»). El DNI «65399546N» es el del alumno de «CIPFP Batoi».
- La aplicación tiene configurada (o no) una cuenta del proveedor de SMS: las tres propiedades `sms.credentials.twilio.*` / `sms.twilio.from` son de instalación y **no** son datos de la aplicación. Ningún test depende de que estén rellenas.
- No hay ningún SMS dado de alta: cada test crea los suyos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — (cualquier centro) |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata |
| administrativo1@mislata.es | demo1234 | Administrativo | CIPFP Mislata |
| supervisordoscentros@mislata.es | demo1234 | Supervisor | CIPFP Mislata y CIPFP Batoi |
| alumno1@mislata.es | demo1234 | Alumno, destinatario (DNI 86862719E) | CIPFP Mislata |

---

## T-001 — Alta de un SMS y resultado del envío

**Origen ESC:** ESC-001
**Verifica:** V-Sms-001, V-Sms-003, V-Sms-004, V-Sms-005, V-Sms-007, V-Sms-009, R-Sms-001, R-Sms-002, R-Sms-003, R-Sms-004, R-Sms-005, R-Sms-006, U-sms-todos-002, U-sms-todos-008
**Pantalla principal:** screen-sms-todos.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema guarda el SMS y vuelve al listado, donde aparece el SMS de «Alumno1» con el teléfono «+34600111222».
6. **Y** espera unos segundos y recarga el listado.
7. **Y** pulsa sobre el SMS de «Alumno1».

### Resultado esperado
- En el listado, el SMS aparece con el teléfono «+34600111222» (normalizado a formato internacional aunque se escribiera sin prefijo).
- Tras la espera, el SMS ya no está en estado "Pendiente": está en "Enviado" con fecha de envío, o en "Fallido" sin fecha de envío.
- Al abrir el detalle, el sistema lo muestra en solo lectura con el panel "Datos del envío" y 1 reintento; si está "Fallido", con la descripción del último fallo y sin fecha de envío.

---

## T-002 — Alta con el teléfono escrito ya en formato internacional

**Origen ESC:** ESC-002
**Verifica:** V-Sms-006, R-Sms-001
**Pantalla principal:** screen-sms-todos.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «+34600111222» y el mensaje «Recogida de notas el viernes».
4. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema guarda el SMS y vuelve al listado.
- El SMS «Recogida de notas el viernes» aparece con el teléfono «+34600111222» (sin duplicar ni alterar el prefijo).

---

## T-003 — Alta sin el DNI del destinatario

**Origen ESC:** ESC-003
**Verifica:** V-Sms-001
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", deja vacío el DNI del destinatario y escribe el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
4. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra «El DNI del destinatario es obligatorio».
- No se guarda el SMS: al volver al listado no aparece ningún SMS «Mañana no hay clase».

---

## T-004 — Alta con el DNI con la letra incorrecta

**Origen ESC:** ESC-004
**Verifica:** V-Sms-002
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719A», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
4. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra «El DNI del destinatario no es válido; compruebe la letra».
- No se guarda el SMS.

---

## T-005 — Alta sin nombre ni apellidos

**Origen ESC:** ESC-005
**Verifica:** V-Sms-003, V-Sms-004
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», deja vacíos el nombre y los apellidos y escribe el teléfono «600111222» y el mensaje «Mañana no hay clase».
4. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra a la vez «El nombre es obligatorio» y «Los apellidos son obligatorios».
- No se guarda el SMS.

---

## T-006 — Alta sin teléfono

**Origen ESC:** ESC-006
**Verifica:** V-Sms-005
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», deja vacío el teléfono y escribe el mensaje «Mañana no hay clase».
4. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra «El teléfono es obligatorio».
- No se guarda el SMS.

---

## T-007 — Alta con un teléfono que no es un móvil de España

**Origen ESC:** ESC-007
**Verifica:** V-Sms-006
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono fijo «963000000» y el mensaje «Mañana no hay clase».
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema muestra «El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)» y no guarda el SMS.
6. **Cuando** cambia el teléfono por el número incompleto «60011» y pulsa "Guardar".

### Resultado esperado
- El sistema vuelve a mostrar «El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)».
- No se guarda el SMS en ninguno de los dos intentos.

---

## T-008 — Alta sin mensaje

**Origen ESC:** ESC-008
**Verifica:** V-Sms-007
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y deja vacío el mensaje.
4. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra «El mensaje es obligatorio».
- No se guarda el SMS.

---

## T-009 — Mensaje sin acentos en el límite de un SMS

**Origen ESC:** ESC-009
**Verifica:** V-Sms-008
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y como mensaje la letra «a» repetida 161 veces.
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no guarda el SMS.
6. **Cuando** borra una letra para dejar el mensaje con la letra «a» repetida 160 veces y pulsa "Guardar".

### Resultado esperado
- El sistema guarda el SMS y vuelve al listado.
- El SMS aparece con el teléfono «+34600111222» y como mensaje la letra «a» repetida 160 veces.

---

## T-010 — Mensaje con acentos en el límite de un SMS

**Origen ESC:** ESC-010
**Verifica:** V-Sms-008
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y como mensaje la letra «ú» seguida de la letra «a» repetida 70 veces (71 caracteres en total).
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no guarda el SMS.
6. **Cuando** borra una letra «a» para dejar el mensaje en 70 caracteres (la «ú» seguida de 69 letras «a») y pulsa "Guardar".

### Resultado esperado
- El sistema guarda el SMS y vuelve al listado.
- El SMS aparece con el teléfono «+34600111222» y como mensaje la «ú» seguida de 69 letras «a».

---

## T-011 — Alta sin centro

**Origen ESC:** ESC-011
**Verifica:** V-Sms-009
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** no elige centro y escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
4. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra «El centro es obligatorio».
- No se guarda el SMS.

---

## T-012 — El administrador consulta los SMS de varios centros

**Origen ESC:** ESC-012
**Verifica:** V-Sms-010, R-Sms-002
**Pantalla principal:** screen-sms-todos.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata", escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata», y pulsa "Guardar".
3. **Y** pulsa "Nuevo SMS", elige el centro "CIPFP Batoi", escribe el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi», y pulsa "Guardar".

### Resultado esperado
- El sistema muestra los dos SMS en el listado, aunque sean de centros distintos.
- «Aviso Batoi», con el centro "CIPFP Batoi", aparece en la primera fila, y «Aviso Mislata», con el centro "CIPFP Mislata", en la siguiente: el listado va del más reciente al más antiguo.

---

## T-013 — Un SMS ya creado no se puede modificar ni borrar

**Origen ESC:** ESC-013
**Verifica:** V-Sms-012, V-Sms-013, U-sms-todos-001, U-sms-todos-002, U-sms-todos-005
**Pantalla principal:** screen-sms-todos.md
**Tipo:** UI

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata", escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase», y pulsa "Guardar".
3. **Y** espera unos segundos y recarga el listado.
4. **Y** pulsa sobre el SMS «Mañana no hay clase».

### Resultado esperado
- El sistema muestra el SMS con todos sus datos en solo lectura (no se puede escribir en ningún campo del panel "Datos del SMS").
- Muestra el panel "Datos del envío" con el estado, el número de reintentos y las fechas de creación, del primer intento y del último intento.
- No muestra el botón "Guardar" ni el botón "Borrar".

---

## T-014 — El supervisor ve solo los SMS de su centro

**Origen ESC:** ESC-014
**Verifica:** U-sms-centro-001, U-sms-centro-002
**Pantalla principal:** screen-sms-centro.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
3. **Y** da de alta otro SMS del centro "CIPFP Batoi" con el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
4. **Y** cierra sesión.
5. **Y** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú "SMS" → "Del centro".
6. **Y** pulsa sobre el SMS «Aviso Mislata».

### Resultado esperado
- El listado muestra el SMS «Aviso Mislata» y no muestra el SMS «Aviso Batoi».
- El detalle se muestra en solo lectura, con el panel "Datos del SMS" (centro "CIPFP Mislata", DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», teléfono «+34600111222» y mensaje «Aviso Mislata») y el panel "Datos del envío" con el estado y el número de reintentos.
- Muestra el botón "Salir" y no muestra los botones "Guardar" ni "Borrar".

---

## T-015 — El administrativo ve los SMS de su centro

**Origen ESC:** ESC-015
**Verifica:** —
**Pantalla principal:** screen-sms-centro.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
3. **Y** da de alta otro SMS del centro "CIPFP Batoi" con el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
4. **Y** cierra sesión.
5. **Y** el administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú "SMS" → "Del centro".

### Resultado esperado
- El listado muestra el SMS «Aviso Mislata».
- El listado no muestra el SMS «Aviso Batoi».

---

## T-016 — Reenvío desde «Del centro» por el supervisor

**Origen ESC:** ESC-016
**Verifica:** V-Sms-014, V-Sms-015, R-Sms-003, R-Sms-004, U-sms-centro-003
**Pantalla principal:** screen-sms-centro.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata», y comprueba en el listado que aparece con el teléfono «+34600111222».
3. **Y** cierra sesión.
4. **Y** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre el menú "SMS" → "Del centro".
5. **Y** pulsa sobre el SMS «Aviso Mislata».

### Resultado esperado
- Si el SMS está en estado "Fallido": el sistema muestra el botón "Reenviar"; al pulsarlo muestra «El reenvío del SMS se ha puesto en marcha.» y, pasados unos segundos, al recargar, el SMS muestra 2 reintentos y está en "Enviado" (con fecha de envío) o de nuevo en "Fallido" (con la descripción del nuevo fallo).
- Si el SMS está en estado "Enviado": el sistema no muestra el botón "Reenviar".

---

## T-017 — Reenvío desde «Todos» por el administrador

**Origen ESC:** ESC-017
**Verifica:** V-Sms-014, R-Sms-003, U-sms-todos-006
**Pantalla principal:** screen-sms-todos.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Batoi", escribe el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi», y pulsa "Guardar".
3. **Y** comprueba que en el listado aparece con el teléfono «+34600333444».
4. **Y** espera unos segundos, recarga el listado y pulsa sobre el SMS «Aviso Batoi».

### Resultado esperado
- Si el SMS está en estado "Fallido": el sistema muestra el botón "Reenviar" y, al pulsarlo, muestra «El reenvío del SMS se ha puesto en marcha.».
- Si el SMS está en estado "Enviado": el sistema no muestra el botón "Reenviar".

---

## T-018 — El destinatario ve sus SMS enviados y no los de otro

**Origen ESC:** ESC-018
**Verifica:** —
**Pantalla principal:** screen-mis-sms.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso para Alumno1».
3. **Y** da de alta otro SMS del centro "CIPFP Mislata" con el DNI «03532821K», el nombre «Alumno2», los apellidos «CIPFP Mislata», el teléfono «600555666» y el mensaje «Aviso para Alumno2».
4. **Y** espera unos segundos, recarga el listado y anota el estado del SMS «Aviso para Alumno1».
5. **Y** cierra sesión.
6. **Y** el alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú "SMS" → "Recibidos".

### Resultado esperado
- Si el SMS «Aviso para Alumno1» estaba en "Enviado": el listado lo muestra con el teléfono «+34600111222», el mensaje «Aviso para Alumno1» y su fecha de envío, y al pulsarlo se abre en solo lectura.
- Si estaba en "Fallido": el listado no lo muestra.
- En ningún caso el listado muestra el SMS «Aviso para Alumno2».

---

## T-019 — Cancelar el alta de un SMS

**Origen ESC:** ESC-019
**Verifica:** U-sms-todos-002, U-sms-todos-005
**Pantalla principal:** screen-sms-todos.md
**Tipo:** UI

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Entonces** el sistema muestra el formulario con los botones "Guardar" y "Cancelar", sin el botón "Salir" y sin el panel "Datos del envío".
4. **Cuando** elige el centro "CIPFP Mislata", escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Alta cancelada».
5. **Y** pulsa "Cancelar".

### Resultado esperado
- El sistema vuelve al listado.
- En el listado no aparece ningún SMS con el mensaje «Alta cancelada».

---

## T-020 — Datos del envío según el estado, en el detalle

**Origen ESC:** ESC-020
**Verifica:** R-Sms-004, R-Sms-005, R-Sms-006, U-sms-todos-003, U-sms-todos-004
**Pantalla principal:** screen-sms-todos.md
**Tipo:** UI

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata", escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso detalle», y pulsa "Guardar".
3. **Y** espera unos segundos, recarga el listado y pulsa sobre el SMS «Aviso detalle».

### Resultado esperado
- El panel "Datos del envío" muestra 1 reintento y las fechas de creación, del primer intento de envío y del último intento de envío.
- Si el SMS está en estado "Enviado": muestra la fecha de envío y no muestra la descripción del último fallo.
- Si está en estado "Fallido": muestra la descripción del último fallo y no muestra la fecha de envío.

---

## T-021 — El supervisor de dos centros ve los SMS de ambos y filtra por centro

**Origen ESC:** ESC-021
**Verifica:** —
**Pantalla principal:** screen-sms-centro.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
3. **Y** da de alta otro SMS del centro "CIPFP Batoi" con el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
4. **Y** cierra sesión.
5. **Y** el supervisor «supervisordoscentros@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú "SMS" → "Del centro".
6. **Y** escribe «CIPFP Batoi» en el buscador de la columna "centro" del listado y pulsa Intro.

### Resultado esperado
- Antes de filtrar, el listado muestra los dos SMS: «Aviso Mislata» con el centro "CIPFP Mislata" y «Aviso Batoi» con el centro "CIPFP Batoi".
- Después de filtrar, el listado muestra solo el SMS «Aviso Batoi».

---

## T-022 — El supervisor no puede dar de alta SMS

**Origen ESC:** ESC-022
**Verifica:** —
**Pantalla principal:** screen-sms-centro.md
**Tipo:** UI

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el supervisor «supervisor1@mislata.es» ha iniciado sesión con contraseña «demo1234».
2. **Cuando** despliega el menú "SMS".
3. **Y** abre el menú "SMS" → "Del centro".

### Resultado esperado
- El menú "SMS" no muestra la entrada "Todos".
- El listado de "Del centro" se muestra sin el botón "Nuevo SMS".

---

## T-023 — Reenvío por el administrativo

**Origen ESC:** ESC-023
**Verifica:** V-Sms-014, V-Sms-015, R-Sms-003, R-Sms-004, U-sms-centro-003
**Pantalla principal:** screen-sms-centro.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata», y comprueba que en el listado aparece con el teléfono «+34600111222».
3. **Y** cierra sesión.
4. **Y** el administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre el menú "SMS" → "Del centro".
5. **Y** pulsa sobre el SMS «Aviso Mislata».

### Resultado esperado
- Si el SMS está en estado "Fallido": el sistema muestra el botón "Reenviar"; al pulsarlo muestra «El reenvío del SMS se ha puesto en marcha.» y, pasados unos segundos, al recargar, el SMS muestra 2 reintentos.
- Si el SMS está en estado "Enviado": el sistema no muestra el botón "Reenviar".

---

## T-024 — El destinatario ve un SMS enviado desde otro centro

**Origen ESC:** ESC-024
**Verifica:** —
**Pantalla principal:** screen-mis-sms.md
**Tipo:** happy

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Batoi", escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso desde Batoi», y pulsa "Guardar".
3. **Y** comprueba que en el listado aparece con el teléfono «+34600111222».
4. **Y** espera unos segundos, recarga el listado y anota el estado del SMS «Aviso desde Batoi».
5. **Y** cierra sesión.
6. **Y** el alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú "SMS" → "Recibidos".

### Resultado esperado
- Si el SMS «Aviso desde Batoi» estaba en "Enviado": el listado lo muestra con el teléfono «+34600111222», su mensaje y su fecha de envío, aunque lo haya enviado un centro en el que el alumno no está.
- Si estaba en "Fallido": el listado no lo muestra.

---

## T-025 — Un usuario sin cargo de gestión no ve SMS de los centros

**Origen ESC:** ESC-025
**Verifica:** —
**Pantalla principal:** screen-sms-centro.md
**Tipo:** UI

### Precondiciones
- Solo el «Estado inicial de la base de datos».

### Pasos
1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata", escribe el DNI «03532821K», el nombre «Alumno2», los apellidos «CIPFP Mislata», el teléfono «600555666» y el mensaje «Aviso para Alumno2», y pulsa "Guardar".
3. **Y** comprueba que el SMS «Aviso para Alumno2» aparece en el listado, y cierra sesión.
4. **Y** el alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234» y despliega el menú "SMS".
5. **Y** abre el menú "SMS" → "Del centro".

### Resultado esperado
- El menú "SMS" no muestra la entrada "Todos".
- El listado de "Del centro" no muestra ningún SMS; en particular, no muestra «Aviso para Alumno2».
