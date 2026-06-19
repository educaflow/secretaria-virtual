# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-app` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Dos centros: «IES Mislata» (curso académico 2024) e «IES Benicalap» (curso académico 2024), gestionados por el subsistema `common`.
- Catálogo educativo (subsistema `sistemaeducativo`): curso «1º DAM» con los módulos «Programación» y «Bases de datos»; curso «1º SMR» (con sus propios módulos del catálogo).
- Usuarios de prueba:
  - Un administrador (grupo `admins`).
  - Un supervisor del centro «IES Mislata» (tipo de usuario activo SUPERVISOR, centro activo «IES Mislata»).
  - Alumnos del centro «IES Mislata» (tipo ALUMNO): «Juan Pérez», «Ana López», «Luis Gil», «Marta Ruiz».
  - Un profesor del centro «IES Mislata» (tipo PROFESOR): «Pedro Sanz».
  - Una alumna del centro «IES Benicalap» (tipo ALUMNO): «Sara Vidal».
- No existe ningún Grupo, ModuloGrupo, AlumnoGrupo ni Nota al inicio (los crean los propios tests).

**Usuarios de acceso** (login y contraseña que `/sdd-debug-app` usará para iniciar sesión). Convención adoptada (el spec no fija credenciales): login = nombre en minúsculas sin tilde, con un punto entre nombre y apellido para los alumnos/profesores; contraseña común `educaflow` para todos.

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | educaflow | Administrador (grupo `admins`) | — (todos) |
| supervisor.mislata | educaflow | Supervisor (SUPERVISOR) | IES Mislata |
| juan.perez | educaflow | Alumno (ALUMNO) | IES Mislata |
| ana.lopez | educaflow | Alumno (ALUMNO) | IES Mislata |
| luis.gil | educaflow | Alumno (ALUMNO) | IES Mislata |
| marta.ruiz | educaflow | Alumno (ALUMNO) | IES Mislata |
| pedro.sanz | educaflow | Profesor (PROFESOR) | IES Mislata |
| sara.vidal | educaflow | Alumno (ALUMNO) | IES Benicalap |

---

## T-001 — Crear un grupo con alumnos y notas iniciales

**Origen ESC:** ESC-001
**Verifica:** R-Grupo-001, R-Grupo-002, R-AlumnoGrupo-001, U-grupos-supervisor-001, U-grupos-supervisor-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.

### Pasos
1. **Dado** que el usuario está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo".
3. **Y** escribe en el campo "Nombre" el valor «1º DAM A».
4. **Y** elige en el campo "Curso" el valor «1º DAM».
5. **Entonces** el campo "Centro" muestra «IES Mislata» en solo lectura y el campo "Curso académico" muestra el del centro en solo lectura.
6. **Cuando** pulsa "Guardar".
7. **Entonces** el grupo se crea en estado «Abierto», y en el panel "Módulos" aparecen «Programación» y «Bases de datos».
8. **Cuando** en el panel "Alumnos" pulsa "Añadir alumno", elige «Juan Pérez» y pulsa "Guardar".
9. **Y** repite para «Ana López».

### Resultado esperado
- El grupo «1º DAM A» queda en estado «Abierto» con centro «IES Mislata».
- Los módulos del grupo son «Programación» y «Bases de datos».
- «Juan Pérez» y «Ana López» aparecen en el panel "Alumnos" con nota media «Sin nota».
- Al entrar en el módulo «Programación», cada alumno tiene una nota «No evaluado»; lo mismo en «Bases de datos».

---

## T-002 — Nombre de grupo duplicado en alta

**Origen ESC:** ESC-002
**Verifica:** V-Grupo-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe un grupo «1º DAM A» (curso «1º DAM») en «IES Mislata» (creado por el propio test antes del segundo alta).

### Pasos
1. **Dado** que el usuario está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo", escribe «1º DAM A» en "Nombre", elige «1º DAM» en "Curso" y pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "Ya existe un grupo con ese nombre en este centro y curso académico".
- No se crea un segundo grupo «1º DAM A».

---

## T-003 — Crear un grupo sin nombre

**Origen ESC:** ESC-003
**Verifica:** V-Grupo-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.

### Pasos
1. **Dado** que el usuario está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo", elige «1º DAM» en "Curso", deja "Nombre" vacío y pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El nombre del grupo es obligatorio".
- No se crea el grupo.

---

## T-004 — Crear un grupo sin curso

**Origen ESC:** ESC-016
**Verifica:** V-Grupo-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.

### Pasos
1. **Dado** que el usuario está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo", escribe «1º DAM A» en "Nombre", deja "Curso" vacío y pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El curso es obligatorio".
- No se crea el grupo.

---

## T-005 — Renombrar un grupo a un nombre ya existente

**Origen ESC:** ESC-017
**Verifica:** V-Grupo-004, V-Grupo-005
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existen los grupos «1º DAM A» y «1º DAM B» (curso «1º DAM») en «IES Mislata» (creados por el test).

### Pasos
1. **Dado** que el usuario está en la pantalla "Grupos".
2. **Cuando** abre el grupo «1º DAM B», cambia "Nombre" a «1º DAM A» y pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "Ya existe un grupo con ese nombre en este centro y curso académico".
- El grupo sigue llamándose «1º DAM B».

---

## T-006 — Un alumno no puede estar en dos grupos del mismo curso académico

**Origen ESC:** ESC-004
**Verifica:** V-AlumnoGrupo-004
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» con «Juan Pérez» añadido y el grupo «1º DAM B», ambos curso «1º DAM» en «IES Mislata» (creados por el test).

### Pasos
1. **Dado** que el usuario está en la pantalla "Grupos".
2. **Cuando** abre «1º DAM B», en el panel "Alumnos" pulsa "Añadir alumno", elige «Juan Pérez» y pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "El alumno ya pertenece a otro grupo de este curso académico".
- «Juan Pérez» no se añade a «1º DAM B».

---

## T-007 — Añadir un alumno sin elegir ninguno

**Origen ESC:** ESC-018
**Verifica:** V-AlumnoGrupo-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») en «IES Mislata» (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A».
2. **Cuando** en el panel "Alumnos" pulsa "Añadir alumno" y, sin elegir alumno, pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "Debe elegir un alumno".
- No se añade nada al grupo.

---

## T-008 — No se puede añadir dos veces el mismo alumno

**Origen ESC:** ESC-019
**Verifica:** V-AlumnoGrupo-005
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» con «Juan Pérez» ya añadido (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A».
2. **Cuando** en el panel "Alumnos" pulsa "Añadir alumno", elige otra vez «Juan Pérez» y pulsa "Guardar".

### Resultado esperado
- «Juan Pérez» sigue apareciendo una sola vez en el grupo (no se duplica).

---

## T-009 — El selector de alumno solo ofrece alumnos del centro del grupo

**Origen ESC:** ESC-020
**Verifica:** U-grupos-supervisor-005, V-AlumnoGrupo-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** UI

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») en «IES Mislata» (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A».
2. **Cuando** en el panel "Alumnos" pulsa "Añadir alumno" y abre el selector del campo "Alumno".

### Resultado esperado
- El selector ofrece «Juan Pérez», «Ana López», «Luis Gil» y «Marta Ruiz».
- El selector NO ofrece a «Pedro Sanz» (profesor de «IES Mislata») ni a «Sara Vidal» (alumna de «IES Benicalap»).

---

## T-010 — El supervisor solo ve los grupos de su centro

**Origen ESC:** ESC-021
**Verifica:** R-Grupo-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador `admin` ha iniciado sesión.
2. **Cuando** abre "Grupos (administración)", pulsa "Nuevo grupo", escribe «1º SMR A», elige el centro «IES Benicalap», el curso académico «2024» y el curso «1º SMR», y pulsa "Guardar".
3. **Y** cierra sesión.
4. **Cuando** el supervisor `supervisor.mislata` inicia sesión, abre "Grupos", pulsa "Nuevo grupo", crea «1º DAM A» con el curso «1º DAM» y pulsa "Guardar".
5. **Y** vuelve al listado "Grupos".

### Resultado esperado
- El listado muestra el grupo «1º DAM A» (de «IES Mislata»).
- El listado NO muestra el grupo «1º SMR A» (de «IES Benicalap»).

---

## T-011 — Poner una nota numérica actualiza la media

**Origen ESC:** ESC-006
**Verifica:** V-Nota-002, R-Nota-001, R-Nota-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») con «Juan Pérez» añadido (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A».
2. **Cuando** abre el módulo «Programación», abre la fila del alumno «Juan Pérez», pone en "Valor" el valor «8» y pulsa "Guardar".

### Resultado esperado
- La nota de «Juan Pérez» en «Programación» queda con valor «8».
- El campo "Fecha de calificación" de esa nota queda relleno.
- En el panel "Alumnos" del grupo, «Juan Pérez» muestra nota media «8».

---

## T-012 — La media excluye no evaluados y cuenta la matrícula como 10

**Origen ESC:** ESC-007
**Verifica:** R-AlumnoGrupo-001 (cálculo CC-001), V-Nota-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM», módulos «Programación» y «Bases de datos») con «Juan Pérez» añadido (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A».
2. **Cuando** pone a «Juan Pérez» el valor «Matrícula de Honor» en «Programación» y deja «Bases de datos» en «No evaluado».
3. **Entonces** la nota media de «Juan Pérez» es «10».
4. **Cuando** pone a «Juan Pérez» el valor «7» en «Bases de datos».

### Resultado esperado
- Tras el paso 2-3, la nota media de «Juan Pérez» es «10».
- Tras el paso 4, la nota media de «Juan Pérez» es «9» (media de 10 y 7 redondeada).

---

## T-013 — Media «Sin nota» cuando no hay ningún módulo evaluado

**Origen ESC:** ESC-008
**Verifica:** R-AlumnoGrupo-001 (cálculo CC-001)
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») con «Ana López» añadida y sin notas puestas (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A».
2. **Cuando** consulta el panel "Alumnos".

### Resultado esperado
- «Ana López» aparece con nota media «Sin nota».

---

## T-014 — Máximo tres matrículas de honor por módulo

**Origen ESC:** ESC-009
**Verifica:** V-Nota-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») con «Juan Pérez», «Ana López», «Luis Gil» y «Marta Ruiz» añadidos (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A» y entra en el módulo «Programación».
2. **Cuando** pone «Matrícula de Honor» a «Juan Pérez», «Ana López» y «Luis Gil».
3. **Y** intenta poner «Matrícula de Honor» a «Marta Ruiz» y pulsa "Guardar".

### Resultado esperado
- El sistema muestra el mensaje "No se pueden poner más de 3 matrículas de honor en un módulo".
- La nota de «Marta Ruiz» en «Programación» no cambia.

---

## T-015 — Modificar una nota rellena la fecha de última modificación

**Origen ESC:** ESC-022
**Verifica:** R-Nota-001 (CC-002), R-Nota-002 (CC-003)
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») con «Juan Pérez» añadido (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A», entra en «Programación» y abre la nota de «Juan Pérez».
2. **Cuando** pone "Valor" «8» y pulsa "Guardar".
3. **Entonces** la nota queda en «8» y "Fecha de última modificación" está vacía.
4. **Cuando** vuelve a abrir esa nota, la cambia a «6» y pulsa "Guardar".

### Resultado esperado
- Tras el paso 2-3, la nota es «8» y "Fecha de última modificación" está vacía.
- Tras el paso 4, la nota es «6» y "Fecha de última modificación" queda rellena.

---

## T-016 — Poner un valor de nota inválido

**Origen ESC:** ESC-023
**Verifica:** V-Nota-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») con «Juan Pérez» añadido (creado por el test).

### Pasos
1. **Dado** que el usuario abre la nota de «Juan Pérez» en «Programación».
2. **Cuando** intenta poner el valor «11» y guardar.

### Resultado esperado
- El sistema muestra el mensaje "La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor" (o el selector de "Valor" no ofrece el valor «11»).
- La nota no cambia.

---

## T-017 — Quitar un alumno de un grupo abierto

**Origen ESC:** ESC-005
**Verifica:** V-AlumnoGrupo-006
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») con «Juan Pérez» y «Ana López» añadidos (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A».
2. **Cuando** en el panel "Alumnos" quita a «Ana López» y guarda.

### Resultado esperado
- «Ana López» ya no aparece en el grupo (sus notas se han borrado con ella).
- «Juan Pérez» permanece en el grupo.

---

## T-018 — Cerrar un grupo bloquea notas y alumnos

**Origen ESC:** ESC-010
**Verifica:** R-Grupo-003, V-Nota-002, V-AlumnoGrupo-002, V-AlumnoGrupo-006, U-grupos-supervisor-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») con «Juan Pérez» añadido y con un «8» en «Programación» (creado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A».
2. **Cuando** pulsa "Cerrar grupo".
3. **Entonces** el grupo pasa a «Cerrado» y "Fecha de cierre" queda rellena.
4. **Cuando** intenta cambiar la nota de «Juan Pérez» en «Programación».

### Resultado esperado
- El grupo queda en estado «Cerrado» con "Fecha de cierre" rellena.
- Al intentar cambiar la nota, el sistema muestra "No se pueden modificar las notas de un grupo cerrado" y no la cambia.
- El botón "Añadir alumno" no permite añadir alumnos al grupo cerrado.

---

## T-019 — No se puede modificar el nombre de un grupo cerrado

**Origen ESC:** ESC-024
**Verifica:** V-Grupo-004, U-grupos-supervisor-004
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») ya cerrado (creado y cerrado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A» (en estado «Cerrado»).
2. **Cuando** intenta cambiar "Nombre" a «1º DAM B» y guardar.

### Resultado esperado
- El sistema muestra el mensaje "No se puede modificar un grupo cerrado".
- El nombre del grupo sigue siendo «1º DAM A».

---

## T-020 — No se puede borrar un grupo cerrado

**Origen ESC:** ESC-025
**Verifica:** V-Grupo-006
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») ya cerrado (creado y cerrado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A» (en estado «Cerrado»).
2. **Cuando** intenta borrarlo.

### Resultado esperado
- El sistema muestra el mensaje "No se puede borrar un grupo cerrado".
- El grupo «1º DAM A» sigue existiendo.

---

## T-021 — El supervisor no puede reabrir un grupo cerrado

**Origen ESC:** ESC-011
**Verifica:** U-grupos-supervisor-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** UI

### Precondiciones
- El supervisor `supervisor.mislata` ha iniciado sesión.
- Existe el grupo «1º DAM A» (curso «1º DAM») ya cerrado (creado y cerrado por el test).

### Pasos
1. **Dado** que el usuario abre el grupo «1º DAM A» (en estado «Cerrado»).
2. **Cuando** observa la barra de botones del formulario.

### Resultado esperado
- No se muestra ningún botón "Reabrir grupo" (la pantalla del supervisor no lo tiene).

---

## T-022 — El administrador crea un grupo en otro centro

**Origen ESC:** ESC-012
**Verifica:** R-Grupo-001, U-grupos-administracion-001
**Pantalla principal:** screen-grupos-administrador.md
**Tipo:** happy

### Precondiciones
- El administrador `admin` ha iniciado sesión.

### Pasos
1. **Dado** que el usuario está en la pantalla "Grupos (administración)".
2. **Cuando** pulsa "Nuevo grupo", escribe «1º SMR A», elige el centro «IES Benicalap», el curso académico «2024» y el curso «1º SMR», y pulsa "Guardar".

### Resultado esperado
- El grupo «1º SMR A» se crea en estado «Abierto» en el centro «IES Benicalap» con curso académico «2024» y con los módulos del curso «1º SMR».

---

## T-023 — El administrador reabre un grupo cerrado

**Origen ESC:** ESC-013
**Verifica:** V-Grupo-007, V-Grupo-008, R-Grupo-004, U-grupos-administracion-002
**Pantalla principal:** screen-grupos-administrador.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos.

### Pasos
1. **Dado** que el supervisor `supervisor.mislata` inicia sesión, crea el grupo «1º DAM A» (curso «1º DAM»), le añade a «Juan Pérez», lo cierra con "Cerrar grupo" y cierra sesión.
2. **Cuando** el administrador `admin` inicia sesión, abre "Grupos (administración)" y entra en «1º DAM A» (en estado «Cerrado»).
3. **Y** pulsa "Reabrir grupo".

### Resultado esperado
- El grupo «1º DAM A» pasa a estado «Abierto» y "Fecha de cierre" queda vacía.
- Vuelve a ser posible modificar las notas del grupo.

---

## T-024 — El alumno consulta sus notas y su nota media

**Origen ESC:** ESC-014
**Verifica:** U-mis-notas-alumno (acceso de rol), R-AlumnoGrupo-001 (CC-001)
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos.

### Pasos
1. **Dado** que el supervisor `supervisor.mislata` inicia sesión, crea «1º DAM A» (curso «1º DAM»), le añade a «Juan Pérez», le pone un «8» en «Programación» y un «6» en «Bases de datos», y cierra sesión.
2. **Cuando** el alumno `juan.perez` inicia sesión y abre "Mis notas".
3. **Y** entra en el grupo «1º DAM A».

### Resultado esperado
- El listado "Mis notas" muestra el grupo «1º DAM A» con nota media «7».
- Al entrar en el grupo, ve «8» en «Programación» y «6» en «Bases de datos», en solo lectura.

---

## T-025 — El alumno no puede modificar sus notas

**Origen ESC:** ESC-015
**Verifica:** U-mis-notas-alumno (solo lectura)
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** UI

### Precondiciones
- El estado inicial de la base de datos.

### Pasos
1. **Dado** que el supervisor `supervisor.mislata` inicia sesión, crea «1º DAM A» (curso «1º DAM»), le añade a «Juan Pérez», le pone un «8» en «Programación» y cierra sesión.
2. **Cuando** el alumno `juan.perez` inicia sesión, abre "Mis notas", entra en su grupo y abre la nota de «Programación».

### Resultado esperado
- El sistema muestra la nota «8» en solo lectura.
- No hay opción de cambiar el valor de la nota.

---

## T-026 — El alumno no ve los grupos a los que no pertenece

**Origen ESC:** ESC-026
**Verifica:** U-mis-notas-alumno (acceso de rol)
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos.

### Pasos
1. **Dado** que el supervisor `supervisor.mislata` inicia sesión, crea «1º DAM A» (curso «1º DAM») y le añade a «Juan Pérez».
2. **Y** crea «1º DAM B» (curso «1º DAM») y le añade a «Ana López» (sin añadir a «Juan Pérez»), y cierra sesión.
3. **Cuando** el alumno `juan.perez` inicia sesión y abre "Mis notas".

### Resultado esperado
- El listado muestra el grupo «1º DAM A» (al que pertenece).
- El listado NO muestra el grupo «1º DAM B» (al que no pertenece).
