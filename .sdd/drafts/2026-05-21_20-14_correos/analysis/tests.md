# Tests E2E — Correos

Escenarios E2E Given/When/Then que materializan los flujos principales `F-NNN` del spec. Cada test es independiente y cita pantallas, botones, campos y mensajes tal y como aparecen en los ficheros `entity-*.md` y `screen-*.md`.

---

## T-001 — Alta manual de un Correo sin adjuntos
**Origen F:** F-001
**Verifica:** V-Correo-001, V-Correo-002, V-Correo-003, V-Correo-004, R-Correo-001, R-Correo-002
**Pantalla principal:** screen-todos.md
**Tipo:** happy
### Precondiciones
- El usuario logado es Administrador.
### Pasos
1. **Dado** que estoy en la pantalla "Todos los correos".
2. **Cuando** pulso el botón "Nuevo correo" del toolbar.
3. **Y** en el panel "Destinatario" escribo un DNI en el campo dniDestinatario y un email en el campo emailDestinatario.
4. **Y** en el panel "Mensaje" escribo un texto en el campo asunto y un texto en el campo cuerpo.
5. **Y** pulso el botón "Guardar".
6. **Entonces** el formulario pasa a modo detalle y el correo queda creado.
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
### Precondiciones
- El usuario logado es Administrador.
### Pasos
1. **Dado** que estoy en la pantalla "Todos los correos" y pulso "Nuevo correo".
2. **Cuando** relleno dniDestinatario, emailDestinatario, asunto y cuerpo con valores válidos.
3. **Y** en el panel "Adjuntos" pulso el botón "Añadir adjunto", escribo un nombre en el campo nombreFichero y aporto un fichero en el campo contenido.
4. **Y** pulso el botón "Guardar".
5. **Entonces** el correo queda creado en estado PENDIENTE y el formulario pasa a modo detalle.
### Resultado esperado
- En el grid "Adjuntos" aparece la fila del adjunto con su nombreFichero y su contenido.
- El adjunto queda vinculado al correo y puede descargarse con el botón "Descargar" de la columna contenido.

---

## T-003 — Alta manual sin DNI rechazada
**Origen F:** F-001
**Verifica:** V-Correo-001
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones
- El usuario logado es Administrador.
### Pasos
1. **Dado** que estoy en "Todos los correos" y pulso "Nuevo correo".
2. **Cuando** dejo el campo dniDestinatario vacío y relleno emailDestinatario, asunto y cuerpo con valores válidos.
3. **Y** pulso el botón "Guardar".
4. **Entonces** el sistema rechaza el alta y muestra el mensaje "El DNI del destinatario es obligatorio."
### Resultado esperado
- El correo no se crea y el formulario permanece en modo alta.

---

## T-004 — Alta manual sin email rechazada
**Origen F:** F-001
**Verifica:** V-Correo-002
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones
- El usuario logado es Administrador.
### Pasos
1. **Dado** que estoy en "Todos los correos" y pulso "Nuevo correo".
2. **Cuando** dejo el campo emailDestinatario vacío y relleno dniDestinatario, asunto y cuerpo con valores válidos.
3. **Y** pulso el botón "Guardar".
4. **Entonces** el sistema rechaza el alta y muestra el mensaje "El email del destinatario es obligatorio."
### Resultado esperado
- El correo no se crea y el formulario permanece en modo alta.

---

## T-005 — Alta manual sin asunto rechazada
**Origen F:** F-001
**Verifica:** V-Correo-003
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones
- El usuario logado es Administrador.
### Pasos
1. **Dado** que estoy en "Todos los correos" y pulso "Nuevo correo".
2. **Cuando** dejo el campo asunto vacío y relleno dniDestinatario, emailDestinatario y cuerpo con valores válidos.
3. **Y** pulso el botón "Guardar".
4. **Entonces** el sistema rechaza el alta y muestra el mensaje "El asunto es obligatorio."
### Resultado esperado
- El correo no se crea y el formulario permanece en modo alta.

---

## T-006 — Alta manual sin cuerpo rechazada
**Origen F:** F-001
**Verifica:** V-Correo-004
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones
- El usuario logado es Administrador.
### Pasos
1. **Dado** que estoy en "Todos los correos" y pulso "Nuevo correo".
2. **Cuando** dejo el campo cuerpo vacío y relleno dniDestinatario, emailDestinatario y asunto con valores válidos.
3. **Y** pulso el botón "Guardar".
4. **Entonces** el sistema rechaza el alta y muestra el mensaje "El cuerpo del correo es obligatorio."
### Resultado esperado
- El correo no se crea y el formulario permanece en modo alta.

---

## T-007 — Autocompletado del email a partir del DNI
**Origen F:** F-001
**Verifica:** U-correo-001, R-Correo-004
**Pantalla principal:** screen-correo.md
**Tipo:** UI
### Precondiciones
- El usuario logado es Administrador.
- Existe un usuario en el sistema con un DNI conocido y un email asociado.
### Pasos
1. **Dado** que estoy en "Todos los correos" y pulso "Nuevo correo".
2. **Cuando** en el campo dniDestinatario escribo el DNI de ese usuario existente.
3. **Entonces** el campo emailDestinatario se rellena automáticamente con el email de ese usuario.
### Resultado esperado
- El email propuesto puede confirmarse o editarse a mano por el Administrador antes de guardar.
- Si el DNI introducido no corresponde a ningún usuario, el campo emailDestinatario queda vacío para que el Administrador lo escriba a mano.

---

## T-008 — El detalle de un correo ya creado es solo lectura
**Origen F:** F-001
**Verifica:** V-Correo-005, U-correo-002
**Pantalla principal:** screen-correo.md
**Tipo:** UI
### Precondiciones
- El usuario logado es Administrador.
- Existe al menos un Correo creado.
### Pasos
1. **Dado** que estoy en la pantalla "Todos los correos".
2. **Cuando** hago click sobre la fila de un correo existente.
3. **Entonces** se abre el "Formulario de Correo" en modo detalle.
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
### Precondiciones
- El usuario logado es Administrador.
- Existe un Correo en estado PENDIENTE con un email de destinatario que la infraestructura de correo puede entregar con éxito.
### Pasos
1. **Dado** que existe un Correo en estado PENDIENTE.
2. **Cuando** la tarea periódica de envío se ejecuta y entrega el correo con éxito.
3. **Y** abro ese correo desde la pantalla "Todos los correos".
4. **Entonces** en el panel "Seguimiento" el campo estado muestra ENVIADO.
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
### Precondiciones
- El usuario logado es Administrador.
- Existe un Correo en estado PENDIENTE cuyo envío la infraestructura de correo no puede completar (provoca un error de entrega).
### Pasos
1. **Dado** que existe un Correo en estado PENDIENTE que fallará al enviarse.
2. **Cuando** la tarea periódica de envío se ejecuta e intenta entregarlo y el intento termina con error.
3. **Y** abro ese correo desde la pantalla "Todos los correos".
4. **Entonces** en el panel "Seguimiento" el campo estado muestra FALLIDO.
### Resultado esperado
- El campo motivoUltimoFallo del panel "Seguimiento" muestra la descripción del error.
- El campo fechaUltimoIntento muestra el momento del intento y numeroIntentos se ha incrementado.
- El campo fechaEnvio sigue vacío.
- El correo no se reintenta automáticamente: en una nueva ejecución de la tarea periódica el numeroIntentos no cambia mientras el estado siga siendo FALLIDO.

---

## T-011 — Reenvío de un Correo FALLIDO
**Origen F:** F-004
**Verifica:** R-Correo-005, U-correo-005
**Pantalla principal:** screen-correo.md
**Tipo:** happy
### Precondiciones
- El usuario logado es Administrador.
- Existe un Correo en estado FALLIDO con un motivoUltimoFallo registrado.
### Pasos
1. **Dado** que estoy en "Todos los correos" y abro un correo en estado FALLIDO haciendo click en su fila.
2. **Cuando** consulto el panel "Seguimiento" y veo el motivo del fallo en el campo motivoUltimoFallo.
3. **Y** pulso el botón "Reenviar".
4. **Entonces** el estado del correo vuelve a PENDIENTE.
### Resultado esperado
- El botón "Reenviar" solo era visible porque el usuario es Administrador y el estado era FALLIDO.
- Tras el reenvío, el campo estado del panel "Seguimiento" muestra PENDIENTE, listo para que la próxima ejecución de la tarea periódica lo reintente.

---

## T-012 — No se puede reenviar un Correo que no está en FALLIDO
**Origen F:** F-004
**Verifica:** V-Correo-007, U-correo-005
**Pantalla principal:** screen-correo.md
**Tipo:** error
### Precondiciones
- El usuario logado es Administrador.
- Existe un Correo en estado PENDIENTE o ENVIADO.
### Pasos
1. **Dado** que estoy en "Todos los correos" y abro un correo cuyo estado es PENDIENTE o ENVIADO.
2. **Cuando** consulto el panel "Seguimiento".
3. **Entonces** el botón "Reenviar" no se muestra (solo es visible cuando el estado es FALLIDO).
### Resultado esperado
- No es posible solicitar el reenvío de un correo que no está en estado FALLIDO.
- Si la operación de reenvío llegara a invocarse sobre un correo en otro estado, el sistema la rechaza con el mensaje "Solo se pueden reenviar correos en estado FALLIDO; el correo está en estado '{valor}'."

---

## T-013 — Alta programática por otro subsistema
**Origen F:** F-005
**Verifica:** R-Correo-003, V-Correo-006
**Pantalla principal:** screen-todos.md
**Tipo:** happy
### Precondiciones
- El usuario logado es Administrador (para poder consultar el resultado en "Todos los correos").
- Existe un centro y, opcionalmente, un historial de estado de expediente al que referenciar.
### Pasos
1. **Dado** que otro subsistema necesita notificar a un destinatario.
2. **Cuando** ese subsistema solicita el alta de un Correo indicando dniDestinatario, emailDestinatario, asunto, cuerpo, el centro y, opcionalmente, la referencia al historial de estado de expediente.
3. **Y** abro ese correo desde la pantalla "Todos los correos".
4. **Entonces** el correo queda en estado PENDIENTE y entra en el ciclo normal de envío.
### Resultado esperado
- En el panel "Seguimiento" el campo estado muestra PENDIENTE y el campo centro muestra el centro indicado por el subsistema invocador.
- La referencia al historial de estado de expediente queda fijada con el valor aportado por el subsistema (no editable desde la interfaz).

---

## T-014 — Consulta de Correos del propio centro por Supervisor/Administrativa
**Origen F:** F-006
**Verifica:** U-mi-centro-001
**Pantalla principal:** screen-mi-centro.md
**Tipo:** happy
### Precondiciones
- El usuario logado es Supervisor o Administrativa, con un centro activo.
- Existen Correos del centro activo del usuario y Correos de otros centros.
### Pasos
1. **Dado** que estoy en la pantalla "Correos de mi centro".
2. **Cuando** consulto el grid "Correos de mi centro".
3. **Entonces** veo únicamente los Correos cuyo centro coincide con mi centro activo.
### Resultado esperado
- Los Correos de otros centros no aparecen en el listado.
- El grid muestra las columnas asunto, dniDestinatario, emailDestinatario, estado, fechaCreacion y fechaEnvio.
- No existe botón "Nuevo correo" ni botón "Reenviar" en esta pantalla.
- Al buscar por estado, destinatario o fechas, los resultados siguen restringidos al centro activo.

---

## T-015 — Consulta de los propios correos por su destinatario
**Origen F:** F-007
**Verifica:** U-mis-001
**Pantalla principal:** screen-mis.md
**Tipo:** happy
### Precondiciones
- El usuario logado es Profesor, Alumno, Exprofesor, Exalumno, Familiar o Externo, con un DNI conocido.
- Existen Correos dirigidos al DNI del usuario logado y Correos dirigidos a otros DNI.
### Pasos
1. **Dado** que estoy en la pantalla "Mis correos".
2. **Cuando** consulto el grid "Mis correos".
3. **Entonces** veo únicamente los Correos cuyo dniDestinatario coincide con mi DNI.
### Resultado esperado
- Los Correos dirigidos a otros DNI no aparecen en el listado.
- El grid muestra las columnas asunto, estado, fechaCreacion y fechaEnvio.
- No existe botón "Nuevo correo" ni botón "Reenviar".
- Al buscar por asunto, estado o fechas, los resultados siguen restringidos a los correos del propio usuario.

---

## T-016 — Consulta de la gráfica con rango de fechas y granularidad
**Origen F:** F-008
**Verifica:** U-grafica-001
**Pantalla principal:** screen-grafica.md
**Tipo:** happy
### Precondiciones
- El usuario logado es Administrador.
- Existen Correos en distintos estados (PENDIENTE, ENVIADO, FALLIDO) creados en distintas fechas.
### Pasos
1. **Dado** que estoy en la pantalla "Gráfica de correos".
2. **Cuando** indico una fecha inicial y una fecha final coherentes (fecha final no anterior a la inicial).
3. **Y** selecciono una granularidad (día, semana o mes).
4. **Entonces** la gráfica de barras apiladas muestra el número de Correos por estado y por intervalo temporal dentro del rango indicado.
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
### Precondiciones
- El usuario logado es Administrador.
### Pasos
1. **Dado** que estoy en la pantalla "Gráfica de correos".
2. **Cuando** indico una fecha final anterior a la fecha inicial.
3. **Entonces** el sistema rechaza la consulta y muestra el mensaje "La fecha final no puede ser anterior a la fecha inicial."
### Resultado esperado
- La consulta no se ejecuta y la gráfica no se actualiza hasta corregir las fechas.

---
