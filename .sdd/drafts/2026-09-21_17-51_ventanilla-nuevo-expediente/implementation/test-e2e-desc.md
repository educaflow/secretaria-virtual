# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

La numeración arranca en `T-001` porque la carpeta destino `src/test/e2e/system/ventanilla/` no existe todavía (sistema nuevo).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Dos centros: «CIPFP Mislata» y «CIPFP Batoi».
- Dos tipos de trámite: «Trámites para el alumno» (dirigido al tipo de usuario Alumno) y «Trámites para el profesor» (dirigido al tipo de usuario Profesor).
- Tres trámites, todos con su texto de ayuda y activos en los dos centros:
  - «Anulación de matrícula en ciclo formativo», del tipo «Trámites para el alumno», **admite** presentarse en representación.
  - «Justificación de falta del profesorado», del tipo «Trámites para el profesor», **no** admite representación.
  - «Trámite de prueba», del tipo «Trámites para el profesor», **no** admite representación.
- Permisos por defecto para iniciar trámites (los que ya carga el data-init de `subsystem/security`): Profesor, Alumno y Familiar pueden iniciarlos **presentándolos ellos mismos**; Administrativo (sobre los trámites del alumno) y Jefe de estudios (sobre los del profesor) pueden iniciarlos **registrándolos en papel**.
- Los usuarios de la tabla de abajo, cada uno con sus tipos de usuario y cargos en el centro indicado.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador de la aplicación, sin ningún perfil de inicio | CIPFP Mislata |
| alumno1@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| alumnodoscentros@mislata.es | demo1234 | Alumno | CIPFP Mislata y CIPFP Batoi |
| exalumno1@mislata.es | demo1234 | Exalumno | CIPFP Mislata |
| director@mislata.es | demo1234 | Profesor con cargo de Director | CIPFP Mislata |
| jefeestudios1@mislata.es | demo1234 | Profesor con cargo de Jefe de estudios | CIPFP Mislata |
| administrativo1@mislata.es | demo1234 | Administrativo | CIPFP Mislata |
| administrativo2@mislata.es | demo1234 | Administrativo y Alumno | CIPFP Mislata |
| familiar1@mislata.es | demo1234 | Familiar | CIPFP Mislata |
| alumnofamiliar@mislata.es | demo1234 | Alumno y Familiar | CIPFP Mislata |

---

## T-001 — Alumno de un solo centro crea un expediente sin que se le pregunte nada

**Origen ESC:** ESC-001
**Verifica:** U-nuevo-expediente-001, U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-004, U-nuevo-expediente-005, U-nuevo-expediente-006, U-nuevo-expediente-008, U-nuevo-expediente-009, U-nuevo-expediente-011, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-003, R-AsistenteNuevoExpediente-004
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** el sistema no muestra el listado de centros y abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" encima del listado, que muestra únicamente el tipo de trámite "Trámites para el alumno" y, dentro, únicamente "Anulación de matrícula en ciclo formativo"; no aparece "Trámites para el profesor"; debajo hay un único botón, "Cancelar", y no hay botón "Atrás".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" con el nombre del trámite "Anulación de matrícula en ciclo formativo", su texto de ayuda y el centro "CIPFP Mislata" en solo lectura; no se ve "¿Cómo se presenta?" ni "¿Para quién es el expediente?"; se ven los botones "Atrás" y "Crear expediente".
6. **Cuando** pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra (no queda visible ninguna de sus tres pantallas) y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado.
- El expediente está en el centro "CIPFP Mislata", presentado por el propio alumno (no registrado como presentado en papel) y para él mismo (no en representación de otra persona).

---

## T-002 — El profesor solo ve los trámites para el profesor

**Origen ESC:** ESC-002
**Verifica:** U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-004, R-AsistenteNuevoExpediente-004
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el director `director@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".

### Resultado esperado
- El sistema no muestra el listado de centros y abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" encima del listado.
- El listado muestra únicamente el tipo de trámite "Trámites para el profesor" y, dentro y por orden alfabético, "Justificación de falta del profesorado" y "Trámite de prueba".
- No aparece "Trámites para el alumno".
- Debajo del listado hay un único botón, "Cancelar"; no hay botón "Atrás".

---

## T-003 — Usuario que no puede crear expedientes en ningún centro

**Origen ESC:** ESC-003
**Verifica:** U-nuevo-expediente-001, R-AsistenteNuevoExpediente-003
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el exalumno `exalumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".

### Resultado esperado
- El sistema muestra el aviso "No puede crear expedientes en ninguno de sus centros".
- No queda abierta ninguna pantalla del asistente: ni "Nuevo expediente: elija el centro", ni "Nuevo expediente: elija el trámite", ni "Nuevo expediente".

---

## T-004 — El Administrador se comporta como un usuario más

**Origen ESC:** ESC-004
**Verifica:** U-nuevo-expediente-001, R-AsistenteNuevoExpediente-003
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador ha iniciado sesión con el usuario `admin` y la contraseña `admin`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".

### Resultado esperado
- El sistema muestra el aviso "No puede crear expedientes en ninguno de sus centros".
- No queda abierta ninguna pantalla del asistente y en ningún momento se le ofrece el centro "CIPFP Batoi", al que no pertenece.

---

## T-005 — Alumno de dos centros elige el centro

**Origen ESC:** ESC-005
**Verifica:** U-nuevo-expediente-001, U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-004, U-nuevo-expediente-005, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-003, R-AsistenteNuevoExpediente-004
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumnodoscentros@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre "Nuevo expediente: elija el centro" con dos filas por orden alfabético, "CIPFP Batoi" y "CIPFP Mislata", y un botón "Cancelar".
4. **Cuando** pulsa la fila "CIPFP Batoi".
5. **Entonces** se abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Batoi" encima del listado, que muestra únicamente "Trámites para el alumno" y, dentro, únicamente "Anulación de matrícula en ciclo formativo"; no aparece "Trámites para el profesor"; debajo hay un único botón, "Atrás", y no hay botón "Cancelar".
6. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
7. **Entonces** se abre "Nuevo expediente" con el trámite "Anulación de matrícula en ciclo formativo", su ayuda y el centro "CIPFP Batoi" en solo lectura, sin las preguntas "¿Cómo se presenta?" ni "¿Para quién es el expediente?", y con los botones "Atrás" y "Crear expediente".
8. **Cuando** pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado.
- El expediente está en el centro "CIPFP Batoi" y no en "CIPFP Mislata", presentado por el propio alumno y para él mismo.

---

## T-006 — Cancelar en la elección de centro

**Origen ESC:** ESC-006
**Verifica:** U-nuevo-expediente-001
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumnodoscentros@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre "Nuevo expediente: elija el centro" con las filas "CIPFP Batoi" y "CIPFP Mislata" y un botón "Cancelar".
4. **Cuando** pulsa "Cancelar".

### Resultado esperado
- El asistente se cierra: no se abre "Nuevo expediente: elija el trámite" ni ningún expediente.

---

## T-007 — Atrás desde el contexto del trámite y desde el listado de trámites

**Origen ESC:** ESC-007
**Verifica:** U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumnodoscentros@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre "Nuevo expediente: elija el centro" con las filas "CIPFP Batoi" y "CIPFP Mislata" y un botón "Cancelar".
4. **Cuando** pulsa la fila "CIPFP Mislata".
5. **Entonces** se abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" encima del listado, únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Atrás".
6. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
7. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro "CIPFP Mislata" en solo lectura, sin las dos preguntas, y con los botones "Atrás" y "Crear expediente".
8. **Cuando** pulsa "Atrás".
9. **Entonces** vuelve a "Nuevo expediente: elija el trámite" con el mismo centro "CIPFP Mislata" encima del listado, sin abrir ningún expediente.
10. **Cuando** pulsa "Atrás" debajo del listado.

### Resultado esperado
- Vuelve a "Nuevo expediente: elija el centro" con las filas "CIPFP Batoi" y "CIPFP Mislata".
- No se ha abierto ningún expediente.

---

## T-008 — Cancelar en el listado de trámites cuando no hubo elección de centro

**Origen ESC:** ESC-008
**Verifica:** U-nuevo-expediente-002
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" encima del listado y un único botón debajo, "Cancelar".
4. **Cuando** pulsa "Cancelar".

### Resultado esperado
- El asistente se cierra: no se abre "Nuevo expediente" ni ningún expediente.

---

## T-009 — Administrativo que solo puede registrar en papel: no se le pregunta cómo se presenta

**Origen ESC:** ESC-009
**Verifica:** U-nuevo-expediente-006, U-nuevo-expediente-008, U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrativo `administrativo1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro en solo lectura; no se ve "¿Cómo se presenta?"; sí se ve "¿Para quién es el expediente?" con las opciones "Para mí" y "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)", sin ninguna marcada; se ven los botones "Atrás" y "Crear expediente".
6. **Cuando** marca "Para mí" y pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda registrado como presentado en papel por el administrativo y para él mismo (no en representación de otra persona).

---

## T-010 — Usuario con las dos formas de presentar debe elegir

**Origen ESC:** ESC-010
**Verifica:** V-AsistenteNuevoExpediente-002, U-nuevo-expediente-006, U-nuevo-expediente-007, U-nuevo-expediente-009, U-nuevo-expediente-012, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el profesor" con "Justificación de falta del profesorado" y "Trámite de prueba" por orden alfabético, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Justificación de falta del profesorado".
5. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro en solo lectura; se ve "¿Cómo se presenta?" con "Lo presento yo mismo" y "Estoy registrando un trámite recibido en papel", sin ninguna marcada; no se ve "¿Para quién es el expediente?".
6. **Cuando** pulsa "Crear expediente" sin marcar ninguna opción.
7. **Entonces** el sistema muestra "Debe indicar cómo se presenta el expediente", no crea ningún expediente y "Nuevo expediente" sigue abierta.
8. **Cuando** marca "Estoy registrando un trámite recibido en papel".
9. **Entonces** sigue sin verse "¿Para quién es el expediente?", porque el trámite no admite representación.
10. **Cuando** pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Justificación de falta del profesorado" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda registrado como presentado en papel por el jefe de estudios y para él mismo.

---

## T-011 — «¿Para quién es?» se recalcula al cambiar cómo se presenta

**Origen ESC:** ESC-011
**Verifica:** V-AsistenteNuevoExpediente-003, U-nuevo-expediente-006, U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-011, U-nuevo-expediente-012, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrativo `administrativo2@mislata.es` (que en "CIPFP Mislata" es además alumno) ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" con "¿Cómo se presenta?" visible y sin marcar, y sin "¿Para quién es el expediente?".
6. **Cuando** marca "Lo presento yo mismo".
7. **Entonces** sigue sin verse "¿Para quién es el expediente?" (es alumno y no es familiar: el expediente es para él mismo).
8. **Cuando** cambia a "Estoy registrando un trámite recibido en papel".
9. **Entonces** se muestra "¿Para quién es el expediente?" con "Para mí" y "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)", sin ninguna marcada.
10. **Cuando** pulsa "Crear expediente" sin marcar ninguna opción.
11. **Entonces** el sistema muestra "Debe indicar para quién es el expediente", no crea ningún expediente y "Nuevo expediente" sigue abierta.
12. **Cuando** marca "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)" y pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda registrado como presentado en papel por el administrativo y en representación de otra persona (no para él mismo).

---

## T-012 — Familiar que no es alumno: en representación sin preguntar

**Origen ESC:** ESC-012
**Verifica:** U-nuevo-expediente-009, U-nuevo-expediente-011, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el familiar `familiar1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro en solo lectura, sin "¿Cómo se presenta?" ni "¿Para quién es el expediente?", y con los botones "Atrás" y "Crear expediente".
6. **Cuando** pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda presentado por el propio familiar (no registrado como presentado en papel) y en representación de otra persona (no para él mismo).

---

## T-013 — Usuario que es alumno y familiar elige para quién es

**Origen ESC:** ESC-013
**Verifica:** V-AsistenteNuevoExpediente-003, U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el usuario `alumnofamiliar@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" sin "¿Cómo se presenta?" y con "¿Para quién es el expediente?" visible con "Para mí" y "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)", sin ninguna marcada.
6. **Cuando** pulsa "Crear expediente" sin marcar ninguna opción.
7. **Entonces** el sistema muestra "Debe indicar para quién es el expediente", no crea ningún expediente y "Nuevo expediente" sigue abierta.
8. **Cuando** marca "Para mí" y pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda presentado por el propio usuario (no registrado como presentado en papel) y para él mismo, no en representación de otra persona.

---

## T-014 — Petición manipulada con un centro ajeno

**Origen ESC:** ESC-014
**Verifica:** V-AsistenteNuevoExpediente-004, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Anulación de matrícula en ciclo formativo" en el centro "CIPFP Mislata" (menú "Ventanilla" → "Nuevo expediente" → fila del trámite).
3. **Cuando** pulsa "Crear expediente" con la petición manipulada para que el centro enviado sea "CIPFP Batoi", al que no pertenece, dejando el resto como lo fijó la pantalla: el trámite "Anulación de matrícula en ciclo formativo", presentado por el propio alumno y para él mismo.

### Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra **únicamente** el mensaje "No puede crear expedientes de este trámite en el centro indicado"; no muestra además "No puede presentar el expediente de esa forma en el centro indicado".
- La pantalla "Nuevo expediente" sigue abierta con el trámite "Anulación de matrícula en ciclo formativo" y el centro "CIPFP Mislata", y no se abre ningún expediente.

---

## T-015 — Petición manipulada con una forma de presentar que no tiene

**Origen ESC:** ESC-015
**Verifica:** V-AsistenteNuevoExpediente-005, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Anulación de matrícula en ciclo formativo" en el centro "CIPFP Mislata", sin que se le pregunte "¿Cómo se presenta?".
3. **Cuando** pulsa "Crear expediente" con la petición manipulada para indicar que está registrando un trámite recibido en papel, dejando el resto como lo fijó la pantalla: el centro "CIPFP Mislata", el trámite "Anulación de matrícula en ciclo formativo" y el expediente para él mismo.

### Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra el mensaje "No puede presentar el expediente de esa forma en el centro indicado".
- La pantalla "Nuevo expediente" sigue abierta con el trámite "Anulación de matrícula en ciclo formativo" y el centro "CIPFP Mislata", y no se abre ningún expediente.

---

## T-016 — Petición manipulada para crear el expediente para uno mismo sin poder

**Origen ESC:** ESC-016
**Verifica:** V-AsistenteNuevoExpediente-007, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el familiar `familiar1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Anulación de matrícula en ciclo formativo" en el centro "CIPFP Mislata", sin que se le pregunte nada.
3. **Cuando** pulsa "Crear expediente" con la petición manipulada para indicar que el expediente es para él mismo, dejando el resto como lo fijó la pantalla: el centro "CIPFP Mislata", el trámite "Anulación de matrícula en ciclo formativo" y presentado por el propio familiar.

### Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra el mensaje "No puede crear este expediente para usted mismo en el centro indicado".
- La pantalla "Nuevo expediente" sigue abierta con el trámite "Anulación de matrícula en ciclo formativo" y el centro "CIPFP Mislata", y no se abre ningún expediente.

---

## T-017 — Petición manipulada para crear el expediente en representación sin poder

**Origen ESC:** ESC-016
**Verifica:** V-AsistenteNuevoExpediente-008, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Anulación de matrícula en ciclo formativo" en el centro "CIPFP Mislata", sin que se le pregunte nada.
3. **Cuando** pulsa "Crear expediente" con la petición manipulada para indicar que el expediente es en representación de otra persona, dejando el resto como lo fijó la pantalla: el centro "CIPFP Mislata", el trámite "Anulación de matrícula en ciclo formativo" y presentado por el propio alumno.

### Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra el mensaje "No puede crear este expediente en representación de otra persona en el centro indicado".
- La pantalla "Nuevo expediente" sigue abierta con el trámite "Anulación de matrícula en ciclo formativo" y el centro "CIPFP Mislata", y no se abre ningún expediente.

---

## T-018 — Profesor crea un expediente de un trámite que no admite representación sin que se le pregunte nada

**Origen ESC:** ESC-017
**Verifica:** U-nuevo-expediente-006, U-nuevo-expediente-008, U-nuevo-expediente-009, U-nuevo-expediente-011, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el director `director@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el profesor" con "Justificación de falta del profesorado" y "Trámite de prueba" por orden alfabético, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Justificación de falta del profesorado".
5. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro en solo lectura, sin "¿Cómo se presenta?" ni "¿Para quién es el expediente?", y con los botones "Atrás" y "Crear expediente".
6. **Cuando** pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Justificación de falta del profesorado" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda presentado por el propio director (no registrado como presentado en papel) y para él mismo.

---

## T-019 — Usuario con las dos formas de presentar elige «Lo presento yo mismo»

**Origen ESC:** ESC-018
**Verifica:** U-nuevo-expediente-006, U-nuevo-expediente-009, U-nuevo-expediente-011, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el profesor" con "Justificación de falta del profesorado" y "Trámite de prueba" por orden alfabético, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Justificación de falta del profesorado".
5. **Entonces** se abre "Nuevo expediente" con "¿Cómo se presenta?" visible y sin marcar, y sin "¿Para quién es el expediente?".
6. **Cuando** marca "Lo presento yo mismo".
7. **Entonces** sigue sin verse "¿Para quién es el expediente?".
8. **Cuando** pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Justificación de falta del profesorado" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda presentado por el propio jefe de estudios (no registrado como presentado en papel) y para él mismo.

---

## T-020 — Volver al listado de centros, elegir otro centro y crear el expediente en él

**Origen ESC:** ESC-019
**Verifica:** U-nuevo-expediente-001, U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-005, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-004
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumnodoscentros@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre "Nuevo expediente: elija el centro" con las filas "CIPFP Batoi" y "CIPFP Mislata" y un botón "Cancelar".
4. **Cuando** pulsa la fila "CIPFP Mislata".
5. **Entonces** se abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" encima del listado y un único botón debajo, "Atrás".
6. **Cuando** pulsa "Atrás" debajo del listado.
7. **Entonces** vuelve a "Nuevo expediente: elija el centro" con las filas "CIPFP Batoi" y "CIPFP Mislata".
8. **Cuando** pulsa la fila "CIPFP Batoi".
9. **Entonces** se abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Batoi" encima del listado, únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Atrás".
10. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
11. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro "CIPFP Batoi" en solo lectura, sin las dos preguntas.
12. **Cuando** pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado.
- El expediente está en el centro "CIPFP Batoi" y no en "CIPFP Mislata", presentado por el propio alumno y para él mismo.

---

## T-021 — Atrás desde el contexto del trámite cuando no hubo elección de centro

**Origen ESC:** ESC-020
**Verifica:** U-nuevo-expediente-002, U-nuevo-expediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" encima del listado y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro en solo lectura, sin las dos preguntas, y con los botones "Atrás" y "Crear expediente".
6. **Cuando** pulsa "Atrás".
7. **Entonces** vuelve a "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" encima del listado, sin abrir ningún expediente, y debajo del listado sigue habiendo un único botón, "Cancelar" (no hay "Atrás").
8. **Cuando** pulsa "Cancelar".

### Resultado esperado
- El asistente se cierra sin abrir ningún expediente.

---

## T-022 — Una respuesta a «¿Para quién es?» dada en papel no se conserva al pasar a «Lo presento yo mismo»

**Origen ESC:** ESC-021
**Verifica:** U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-011, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrativo `administrativo2@mislata.es` (que en "CIPFP Mislata" es además alumno) ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" con "¿Cómo se presenta?" visible y sin marcar, y sin "¿Para quién es el expediente?".
6. **Cuando** marca "Estoy registrando un trámite recibido en papel".
7. **Entonces** se muestra "¿Para quién es el expediente?" sin ninguna opción marcada.
8. **Cuando** marca "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)".
9. **Y** cambia "¿Cómo se presenta?" a "Lo presento yo mismo".
10. **Entonces** deja de verse "¿Para quién es el expediente?" (es alumno y no es familiar: el expediente es para él mismo).
11. **Cuando** pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda presentado por el propio usuario y para él mismo: no queda en representación de otra persona ni registrado como presentado en papel.

---

## T-023 — Usuario que es alumno y familiar crea el expediente en representación

**Origen ESC:** ESC-022
**Verifica:** U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el usuario `alumnofamiliar@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Ventanilla" y pulsa "Nuevo expediente".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" sin "¿Cómo se presenta?" y con "¿Para quién es el expediente?" visible y sin marcar.
6. **Cuando** marca "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)" y pulsa "Crear expediente".

### Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda presentado por el propio usuario (no registrado como presentado en papel) y en representación de otra persona, no para el propio usuario.

---

## T-024 — Petición manipulada con un trámite que el usuario no puede iniciar en su centro

**Origen ESC:** ESC-023
**Verifica:** V-AsistenteNuevoExpediente-004, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Anulación de matrícula en ciclo formativo" en el centro "CIPFP Mislata".
3. **Cuando** pulsa "Crear expediente" con la petición manipulada para que el trámite enviado sea "Justificación de falta del profesorado", que el listado no le ofrece, dejando el resto como lo fijó la pantalla: el centro "CIPFP Mislata", presentado por el propio alumno y para él mismo.

### Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra **únicamente** el mensaje "No puede crear expedientes de este trámite en el centro indicado"; no muestra además "No puede presentar el expediente de esa forma en el centro indicado".
- La pantalla "Nuevo expediente" sigue abierta y sigue mostrando el trámite que el alumno eligió, "Anulación de matrícula en ciclo formativo", y el centro "CIPFP Mislata"; no se abre ningún expediente.

---

## T-025 — Petición manipulada en representación en un trámite que no la admite

**Origen ESC:** ESC-024
**Verifica:** V-AsistenteNuevoExpediente-006, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Justificación de falta del profesorado" en el centro "CIPFP Mislata", con "¿Cómo se presenta?" visible.
3. **Cuando** marca "Estoy registrando un trámite recibido en papel".
4. **Entonces** sigue sin verse "¿Para quién es el expediente?", porque el trámite no admite representación.
5. **Cuando** pulsa "Crear expediente" con la petición manipulada para indicar que el expediente es en representación de otra persona, dejando el resto como está en la pantalla: el centro "CIPFP Mislata", el trámite "Justificación de falta del profesorado" y registrado como recibido en papel.

### Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra el mensaje "Este trámite no permite presentar la solicitud en representación de otra persona".
- La pantalla "Nuevo expediente" sigue abierta con el trámite "Justificación de falta del profesorado" y el centro "CIPFP Mislata", con "Estoy registrando un trámite recibido en papel" todavía marcado; no se abre ningún expediente.

---

## T-026 — Petición manipulada sin centro

**Origen ESC:** ESC-025
**Verifica:** V-AsistenteNuevoExpediente-001, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Anulación de matrícula en ciclo formativo" en el centro "CIPFP Mislata".
3. **Cuando** pulsa "Crear expediente" con la petición manipulada para que no se envíe ningún centro, dejando el resto como lo fijó la pantalla: el trámite "Anulación de matrícula en ciclo formativo", presentado por el propio alumno y para él mismo.

### Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra **únicamente** el mensaje "Debe indicar el centro"; no muestra además "No puede crear expedientes de este trámite en el centro indicado" ni "No puede presentar el expediente de esa forma en el centro indicado".
- La pantalla "Nuevo expediente" sigue abierta con el trámite "Anulación de matrícula en ciclo formativo" y el centro "CIPFP Mislata", y no se abre ningún expediente.
