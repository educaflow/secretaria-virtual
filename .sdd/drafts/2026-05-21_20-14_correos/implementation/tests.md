# Tests E2E — Correos

Escenarios E2E Given/When/Then que materializan los flujos principales `F-NNN` del spec.

**Convención de independencia.** Cada test es **autónomo y no asume nada sobre los datos de negocio existentes en la base de datos**: empieza siempre en la pantalla de login, crea por la interfaz todo lo que necesita para probarse y termina cerrando sesión. Lo único que se da por existente es la **semilla de identidad** cargada desde `src/main/resources/data-demo` (usuarios, centros y tipos), porque el primer usuario no puede crearse desde la pantalla de login.

**Estructura de cada test.** Se separan explícitamente:
- **Precondiciones (pasos de preparación):** los pasos que hay que ejecutar *antes* para dejar el sistema listo (login, alta de los datos que el test consultará, espera a la tarea periódica…). No son suposiciones sobre la BD: son acciones reales.
- **Pasos (objetivo del test):** lo que el test realmente verifica.
- **Resultado esperado:** lo que se observa.

**Fixture de identidad usado (de `data-demo`):**
- Administrador: usuario `admin` (grupo `admins`); ve todos los correos, crea y reenvía.
- Centros: `46019660` (CIPFP Mislata)

**Tarea periódica de envío:** se ejecuta cada minuto. Para los tests que dependen del envío, la preparación incluye **esperar ~70 s** (margen sobre el minuto del cron) a que la tarea procese el correo. Un correo con email de formato válido acaba en ENVIADO; un correo con un email malformado (p. ej. `a@@a.com`) acaba en FALLIDO.

---

## T-001 — Alta manual de un Correo sin adjuntos
**Origen F:** F-001
**Verifica:** V-Correo-001, V-Correo-002, V-Correo-003, V-Correo-004, R-Correo-001, R-Correo-002
**Pantalla principal:** screen-todos.md
**Tipo:** happy
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a la pantalla "Todos los correos".
### Pasos (objetivo del test)
1. **Cuando** pulso el botón "Nuevo correo" del toolbar.
2. **Y** en el panel "Destinatario" escribo un DNI en el campo dniDestinatario y un email en el campo emailDestinatario.
3. **Y** en el panel "Mensaje" escribo un texto en el campo asunto y un texto en el campo cuerpo.
4. **Y** pulso el botón "Guardar".
5. **Entonces** el formulario pasa a modo detalle y el correo queda creado.
6. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- En el panel "Seguimiento" el campo estado muestra PENDIENTE, numeroIntentos muestra 0, y fechaUltimoIntento, motivoUltimoFallo y fechaEnvio están vacíos.
- El campo fechaCreacion del panel "Seguimiento" muestra la fecha y hora actuales.
- El campo centro del panel "Seguimiento" está vacío (alta manual del Administrador).
- El nuevo correo aparece en el grid "Todos los correos".

---

## T-002 — Alta manual de un Correo con adjuntos
**Origen F:** F-001
**Verifica:** V-AdjuntoCorreo-001, V-AdjuntoCorreo-002, R-AdjuntoCorreo-001, R-Correo-001
**Pantalla principal:** screen-correo.md
**Tipo:** happy
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos" y pulso "Nuevo correo".
### Pasos (objetivo del test)
1. **Cuando** relleno dniDestinatario, emailDestinatario, asunto y cuerpo con valores válidos.
2. **Y** en el panel "Adjuntos" pulso el botón "Añadir adjunto", escribo un nombre en el campo nombreFichero y aporto un fichero en el campo contenido.
3. **Y** pulso el botón "Guardar".
4. **Entonces** el correo queda creado en estado PENDIENTE y el formulario pasa a modo detalle.
5. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- En el grid "Adjuntos" aparece la fila del adjunto con su nombreFichero y su contenido.
- El adjunto queda vinculado al correo y puede descargarse con el botón "Descargar" de la columna contenido.

---

## T-003 — Alta manual sin DNI rechazada
**Origen F:** F-001
**Verifica:** V-Correo-001
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos" y pulso "Nuevo correo".
### Pasos (objetivo del test)
1. **Cuando** dejo el campo dniDestinatario vacío y relleno emailDestinatario, asunto y cuerpo con valores válidos.
2. **Y** pulso el botón "Guardar".
3. **Entonces** el sistema rechaza el alta y muestra el mensaje "El DNI del destinatario es obligatorio."
4. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El correo no se crea y el formulario permanece en modo alta.

---

## T-004 — Alta manual sin email rechazada
**Origen F:** F-001
**Verifica:** V-Correo-002
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos" y pulso "Nuevo correo".
### Pasos (objetivo del test)
1. **Cuando** dejo el campo emailDestinatario vacío y relleno dniDestinatario, asunto y cuerpo con valores válidos.
2. **Y** pulso el botón "Guardar".
3. **Entonces** el sistema rechaza el alta y muestra el mensaje "El email del destinatario es obligatorio."
4. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El correo no se crea y el formulario permanece en modo alta.

---

## T-005 — Alta manual sin asunto rechazada
**Origen F:** F-001
**Verifica:** V-Correo-003
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos" y pulso "Nuevo correo".
### Pasos (objetivo del test)
1. **Cuando** dejo el campo asunto vacío y relleno dniDestinatario, emailDestinatario y cuerpo con valores válidos.
2. **Y** pulso el botón "Guardar".
3. **Entonces** el sistema rechaza el alta y muestra el mensaje "El asunto es obligatorio."
4. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El correo no se crea y el formulario permanece en modo alta.

---

## T-006 — Alta manual sin cuerpo rechazada
**Origen F:** F-001
**Verifica:** V-Correo-004
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos" y pulso "Nuevo correo".
### Pasos (objetivo del test)
1. **Cuando** dejo el campo cuerpo vacío y relleno dniDestinatario, emailDestinatario y asunto con valores válidos.
2. **Y** pulso el botón "Guardar".
3. **Entonces** el sistema rechaza el alta y muestra el mensaje "El cuerpo del correo es obligatorio."
4. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El correo no se crea y el formulario permanece en modo alta.

---

## T-007 — Autocompletado del email a partir del DNI
**Origen F:** F-001
**Verifica:** U-correo-001, R-Correo-004
**Pantalla principal:** screen-correo.md
**Tipo:** UI
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos" y pulso "Nuevo correo".
### Pasos (objetivo del test)
1. **Cuando** en el campo dniDestinatario escribo `24362574P` (DNI del usuario demo existente).
2. **Entonces** el campo emailDestinatario se rellena automáticamente con `lorenzo.profesor@gmail.com`.
3. **Y** cuando borro el DNI y escribo `99999999R` (un DNI que no corresponde a ningún usuario), el campo emailDestinatario queda vacío.
4. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El email propuesto puede confirmarse o editarse a mano por el Administrador antes de guardar.
- Si el DNI introducido no corresponde a ningún usuario, el campo emailDestinatario queda vacío para que el Administrador lo escriba a mano.

---

## T-008 — El detalle de un correo ya creado es solo lectura
**Origen F:** F-001
**Verifica:** V-Correo-005, U-correo-002
**Pantalla principal:** screen-correo.md
**Tipo:** UI
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos", pulso "Nuevo correo", relleno dniDestinatario, emailDestinatario, asunto y cuerpo con valores válidos y pulso "Guardar" (queda un Correo creado).
3. Cierro el formulario y vuelvo a la pantalla "Todos los correos".
### Pasos (objetivo del test)
1. **Cuando** hago click sobre la fila del correo recién creado.
2. **Entonces** se abre el "Formulario de Correo" en modo detalle.
3. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- Los paneles "Destinatario", "Mensaje" y "Adjuntos" están en solo lectura (no se pueden modificar dniDestinatario, emailDestinatario, asunto, cuerpo ni los adjuntos).
- No se muestran los botones "Cancelar" ni "Guardar"; se muestra el botón "Cerrar".
- En el grid "Adjuntos" no aparece el botón "Añadir adjunto".

---

## T-009 — Envío automático con éxito
**Origen F:** F-002
**Verifica:** R-Correo-006, R-Correo-007
**Pantalla principal:** screen-todos.md
**Tipo:** happy
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos", pulso "Nuevo correo" y doy de alta un correo con un email de **formato válido y entregable** (p. ej. `lorenzo.profesor@gmail.com`), asunto y cuerpo válidos, y pulso "Guardar" (queda en PENDIENTE).
3. Espero ~70 s a que la tarea periódica de envío (que se ejecuta cada minuto) procese el correo.
### Pasos (objetivo del test)
1. **Cuando** abro ese correo desde la pantalla "Todos los correos".
2. **Entonces** en el panel "Seguimiento" el campo estado muestra ENVIADO.
3. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El campo fechaEnvio del panel "Seguimiento" muestra el momento del envío.
- El campo numeroIntentos se ha incrementado y fechaUltimoIntento muestra el momento del intento.
- El campo motivoUltimoFallo está vacío.

---

## T-010 — Envío automático con fallo
**Origen F:** F-003
**Verifica:** R-Correo-006, R-Correo-007
**Pantalla principal:** screen-todos.md
**Tipo:** error
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos", pulso "Nuevo correo" y doy de alta un correo con un email **malformado** `a@@a.com` (cuyo envío fallará seguro), asunto y cuerpo válidos, y pulso "Guardar" (queda en PENDIENTE).
3. Espero ~70 s a que la tarea periódica de envío procese el correo.
### Pasos (objetivo del test)
1. **Cuando** abro ese correo desde la pantalla "Todos los correos".
2. **Entonces** en el panel "Seguimiento" el campo estado muestra FALLIDO.
3. **Y** espero otros ~70 s (una nueva ejecución de la tarea) y vuelvo a abrir el correo.
4. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El campo motivoUltimoFallo del panel "Seguimiento" muestra la descripción del error.
- El campo fechaUltimoIntento muestra el momento del intento y numeroIntentos se ha incrementado.
- El campo fechaEnvio sigue vacío.
- El correo no se reintenta automáticamente: tras la nueva ejecución de la tarea periódica el numeroIntentos no cambia mientras el estado siga siendo FALLIDO.

---

## T-011 — Reenvío de un Correo FALLIDO
**Origen F:** F-004
**Verifica:** R-Correo-005, U-correo-005
**Pantalla principal:** screen-correo.md
**Tipo:** happy
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos", pulso "Nuevo correo" y doy de alta un correo con un email **malformado** `a@@a.com`, asunto y cuerpo válidos, y pulso "Guardar".
3. Espero ~70 s a que la tarea periódica procese el correo y lo deje en estado FALLIDO con un motivoUltimoFallo registrado.
### Pasos (objetivo del test)
1. **Cuando** abro ese correo en estado FALLIDO haciendo click en su fila.
2. **Y** consulto el panel "Seguimiento" y veo el motivo del fallo en el campo motivoUltimoFallo.
3. **Y** pulso el botón "Reenviar".
4. **Entonces** el estado del correo vuelve a PENDIENTE.
5. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El botón "Reenviar" solo era visible porque el usuario es Administrador y el estado era FALLIDO.
- Tras el reenvío, el campo estado del panel "Seguimiento" muestra PENDIENTE, listo para que la próxima ejecución de la tarea periódica lo reintente.

---

## T-012 — No se puede reenviar un Correo que no está en FALLIDO
**Origen F:** F-004
**Verifica:** V-Correo-007, U-correo-005
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos", pulso "Nuevo correo" y doy de alta un correo con email, asunto y cuerpo válidos, y pulso "Guardar" (queda en estado PENDIENTE).
### Pasos (objetivo del test)
1. **Cuando** abro ese correo (estado PENDIENTE) haciendo click en su fila.
2. **Y** consulto el panel "Seguimiento".
3. **Entonces** el botón "Reenviar" no se muestra (solo es visible cuando el estado es FALLIDO).
4. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- No es posible solicitar el reenvío de un correo que no está en estado FALLIDO.
- Si la operación de reenvío llegara a invocarse sobre un correo en otro estado, el sistema la rechaza con el mensaje "Solo se pueden reenviar correos en estado FALLIDO; el correo está en estado '{valor}'."

---




## T-015 — Consulta de los propios correos por su destinatario
**Origen F:** F-007
**Verifica:** U-mis-001
**Pantalla principal:** screen-mis.md
**Tipo:** happy
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos" y doy de alta un correo dirigido al DNI del usuario demo  (dniDestinatario `24362574P`, emailDestinatario `lorenzo.profesor@gmail.com`), con asunto y cuerpo válidos, y pulso "Guardar".
3. Doy de alta un segundo correo dirigido a OTRO DNI distinto, el del usuario demo `alumno1.cipfpmislata` (dniDestinatario `20000001T`), con asunto y cuerpo válidos, y pulso "Guardar".
4. Cierro la sesión del Administrador.
### Pasos (objetivo del test)
1. **Cuando** inicio sesión como `admin` (contraseña `admin`) y navego a la pantalla "Mis correos" y consulto el grid.
2. **Entonces** veo únicamente el Correo cuyo dniDestinatario es `24362574P` (el mío).
3. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- El Correo dirigido al DNI `24362574P` aparece en el listado.
- El grid muestra las columnas asunto, estado, fechaCreacion y fechaEnvio.
- No existe botón "Nuevo correo" ni botón "Reenviar".
- Al buscar por asunto, estado o fechas, los resultados siguen restringidos a los correos del propio usuario.

---

## T-016 — Consulta de la gráfica con rango de fechas y granularidad
**Origen F:** F-008
**Verifica:** U-grafica-001
**Pantalla principal:** screen-grafica.md
**Tipo:** happy
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
2. Navego a "Todos los correos" y doy de alta tres correos (mismo día de hoy) para tener distintos estados:
   - uno con email de formato válido (acabará en ENVIADO),
   - uno con email malformado `a@@a.com` (acabará en FALLIDO),
   - uno recién creado justo antes de consultar la gráfica (quedará en PENDIENTE).
3. Espero ~70 s a que la tarea periódica procese los dos primeros (uno pasa a ENVIADO y otro a FALLIDO).
   > NOTA: la fechaCreacion la fija el servidor a "ahora", por lo que todos los correos del test caen en la fecha de hoy; el rango y la granularidad se eligen para cubrir el día de hoy.
### Pasos (objetivo del test)
1. **Cuando** navego a la pantalla "Gráfica de correos".
2. **Y** indico una fecha inicial y una fecha final coherentes (fecha final no anterior a la inicial) que incluyan el día de hoy.
3. **Y** selecciono una granularidad (día) y luego la cambio a semana y a mes.
4. **Entonces** la gráfica de barras apiladas muestra el número de Correos por estado y por intervalo temporal dentro del rango indicado.
5. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- Las barras están apiladas por estado (PENDIENTE, ENVIADO, FALLIDO).
- El eje temporal se agrupa según la granularidad elegida.
- Al cambiar la granularidad, el agrupamiento temporal de las barras se recalcula.

---

## T-017 — Gráfica con fecha final anterior a la inicial rechazada
**Origen F:** F-008
**Verifica:** U-grafica-002
**Pantalla principal:** screen-grafica.md
**Tipo:** error
### Precondiciones (pasos de preparación)
1. Inicio sesión como Administrador (usuario `admin`).
### Pasos (objetivo del test)
1. **Cuando** navego a la pantalla "Gráfica de correos".
2. **Y** indico una fecha final anterior a la fecha inicial.
3. **Entonces** el sistema rechaza la consulta y muestra el mensaje "La fecha final no puede ser anterior a la fecha inicial."
4. **Y** cierro sesión y salgo de la aplicación.
### Resultado esperado
- La consulta no se ejecuta y la gráfica no se actualiza hasta corregir las fechas.
