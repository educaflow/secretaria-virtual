# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-test-e2e` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- **Centros**: «CIPFP Mislata» (curso académico 2024) e «CIPFP Batoi» (curso académico 2024). El campo `curso` (curso académico, entero) de cada centro = `2024`.
- **Catálogo educativo** (subsistema `sistemaeducativo`):
  - Curso «1º DAM» con módulos «Programación» y «Bases de datos».
  - Curso «1º SMR» (con al menos un módulo).
- **Usuarios** (subsistema `common`, con su `CentroUsuario` + `CentroUsuarioTipoUsuario`):
  - Un **Administrador** (superusuario de Axelor, login `admin`/`admin`; se autoriza vía `isAdmin()`, no por un tipo de usuario).
  - Un **Supervisor** del centro «CIPFP Mislata» (tipo SUPERVISOR).
  - Alumnos del centro «CIPFP Mislata» (tipo ALUMNO): «Alumno1 CIPFP Mislata», «Alumno2 CIPFP Mislata», «Alumno3 CIPFP Mislata», «Alumno4 CIPFP Mislata».
  - Un Profesor del centro «CIPFP Mislata» (tipo PROFESOR): «Director CIPFP Mislata».
  - Una alumna del centro «CIPFP Batoi» (tipo ALUMNO): «Alumno1 CIPFP Batoi».

**Usuarios de acceso** (login y contraseña que `/sdd-test-e2e` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | (todos) |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata |
| alumno1@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| alumno2@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| alumno3@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| alumno4@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| director@mislata.es | demo1234 | Profesor | CIPFP Mislata |
| alumno1@batoi.es | demo1234 | Alumno | CIPFP Batoi |

> Convención adoptada por el diseño (el spec no fija credenciales): los logins son el `code`/email de cada usuario en `usuarios-demo.xml` y la contraseña común es `demo1234` (el administrador es `admin`/`admin`). Documentado aquí para que `/sdd-test-e2e` pueda iniciar sesión.

---

## T-001 — Crear un grupo con sus alumnos

**Origen ESC:** ESC-001
**Verifica:** R-Grupo-001, R-Grupo-002, R-AlumnoGrupo-001, U-grupos-supervisor-001, U-grupos-supervisor-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo".
3. **Y** escribe el nombre "1º DAM A".
4. **Y** elige el curso "1º DAM".
5. **Y** observa que el campo "Centro" muestra "CIPFP Mislata" en solo lectura y el "Curso académico" muestra "2024" en solo lectura.
6. **Y** pulsa "Guardar".
7. **Y** en el panel "Alumnos" pulsa "Añadir alumno", elige "Alumno1 CIPFP Mislata" y guarda; repite con "Alumno2 CIPFP Mislata".

### Resultado esperado
- El grupo "1º DAM A" se crea en estado "Abierto", con centro "CIPFP Mislata" y curso académico "2024".
- El panel "Módulos" muestra "Programación" y "Bases de datos".
- El panel "Alumnos" muestra "Alumno1 CIPFP Mislata" y "Alumno2 CIPFP Mislata", cada uno con nota media "Sin nota".
- Al entrar en el módulo "Programación", cada alumno tiene una nota "No evaluado".

---

## T-002 — Nombre de grupo duplicado al crear

**Origen ESC:** ESC-002
**Verifica:** V-Grupo-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.
- Existe un grupo "1º DAM A" (curso "1º DAM") en "CIPFP Mislata".

### Pasos
1. **Dado** que el supervisor está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo", escribe "1º DAM A", elige el curso "1º DAM".
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra "Ya existe un grupo con ese nombre en este centro y curso académico".
- No se crea el grupo.

---

## T-003 — Crear un grupo sin nombre

**Origen ESC:** ESC-003
**Verifica:** V-Grupo-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo", elige el curso "1º DAM" y deja el nombre vacío.
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra "El nombre del grupo es obligatorio".
- No se crea el grupo.

---

## T-004 — Un alumno no puede estar en dos grupos del mismo curso académico

**Origen ESC:** ESC-004
**Verifica:** V-AlumnoGrupo-005
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata".
2. **Y** crea el grupo "1º DAM B" (curso "1º DAM").
3. **Cuando** en el panel "Alumnos" de "1º DAM B" pulsa "Añadir alumno", elige a "Alumno1 CIPFP Mislata" y guarda.

### Resultado esperado
- El sistema muestra "El alumno ya pertenece a otro grupo de este curso académico".
- "Alumno1 CIPFP Mislata" no se añade a "1º DAM B".

---

## T-005 — Quitar un alumno de un grupo abierto

**Origen ESC:** ESC-005
**Verifica:** V-AlumnoGrupo-006
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata" y "Alumno2 CIPFP Mislata".
2. **Cuando** en el panel "Alumnos" quita a "Alumno2 CIPFP Mislata".
3. **Y** guarda el grupo.

### Resultado esperado
- "Alumno2 CIPFP Mislata" desaparece del grupo junto con sus notas.
- "Alumno1 CIPFP Mislata" permanece en el grupo.

---

## T-006 — Poner una nota numérica

**Origen ESC:** ESC-006
**Verifica:** R-Nota-001, U-grupos-supervisor-005
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata".
2. **Cuando** entra en el grupo, abre el módulo "Programación", abre la nota de "Alumno1 CIPFP Mislata", pone el valor "8".
3. **Y** pulsa "Guardar".

### Resultado esperado
- La nota de "Alumno1 CIPFP Mislata" en "Programación" queda en "8".
- Se registra la "Fecha de calificación".
- En el panel "Alumnos", la nota media de "Alumno1 CIPFP Mislata" pasa a "8".

---

## T-007 — La media excluye no evaluados y cuenta la matrícula de honor como 10

**Origen ESC:** ESC-007
**Verifica:** R-Nota-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM", módulos "Programación" y "Bases de datos") y le añade a "Alumno1 CIPFP Mislata".
2. **Cuando** pone a "Alumno1 CIPFP Mislata" la nota "Matrícula de Honor" en "Programación" y deja "Bases de datos" en "No evaluado".
3. **Entonces** la nota media de "Alumno1 CIPFP Mislata" es "10".
4. **Y cuando** pone "Bases de datos" en "7".
5. **Entonces** la nota media de "Alumno1 CIPFP Mislata" pasa a "9".

### Resultado esperado
- Con MH en "Programación" y "Bases de datos" no evaluado, la nota media es "10".
- Tras poner "7" en "Bases de datos", la nota media es "9" (media de 10 y 7 redondeada).

---

## T-008 — Media «Sin nota» cuando no hay ningún módulo evaluado

**Origen ESC:** ESC-008
**Verifica:** —
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno2 CIPFP Mislata" sin ponerle notas.
2. **Cuando** consulta el panel "Alumnos" del grupo.

### Resultado esperado
- "Alumno2 CIPFP Mislata" aparece con nota media "Sin nota".

---

## T-009 — Máximo tres matrículas de honor por módulo

**Origen ESC:** ESC-009
**Verifica:** V-Nota-004
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata", "Alumno2 CIPFP Mislata", "Alumno3 CIPFP Mislata" y "Alumno4 CIPFP Mislata".
2. **Y** en el módulo "Programación" pone "Matrícula de Honor" a "Alumno1 CIPFP Mislata", "Alumno2 CIPFP Mislata" y "Alumno3 CIPFP Mislata".
3. **Cuando** intenta poner "Matrícula de Honor" a "Alumno4 CIPFP Mislata" en "Programación" y guarda.

### Resultado esperado
- El sistema muestra "No se pueden poner más de 3 matrículas de honor en un módulo".
- La nota de "Alumno4 CIPFP Mislata" no cambia.

---

## T-010 — Cerrar un grupo bloquea sus notas y sus alumnos

**Origen ESC:** ESC-010
**Verifica:** R-Grupo-003, V-Nota-002, V-AlumnoGrupo-003, V-AlumnoGrupo-006, U-grupos-supervisor-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM"), le añade a "Alumno1 CIPFP Mislata" y le pone un "8" en "Programación".
2. **Cuando** pulsa "Cerrar grupo".
3. **Y** intenta cambiar la nota de "Alumno1 CIPFP Mislata" en "Programación".

### Resultado esperado
- El grupo pasa a "Cerrado" y se registra la "Fecha de cierre".
- El sistema muestra "No se pueden modificar las notas de un grupo cerrado" al intentar cambiar la nota.
- No permite añadir ni quitar alumnos del grupo cerrado.

---

## T-011 — El supervisor no puede reabrir un grupo cerrado

**Origen ESC:** ESC-011
**Verifica:** U-grupos-supervisor-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** UI

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y pulsa "Cerrar grupo".
2. **Cuando** vuelve a abrir el grupo cerrado.

### Resultado esperado
- La pantalla del supervisor no muestra el botón "Reabrir grupo".

---

## T-012 — El administrador crea un grupo en otro centro eligiendo centro y curso académico

**Origen ESC:** ESC-012
**Verifica:** R-Grupo-001, R-Grupo-002, U-grupos-administrador-001
**Pantalla principal:** screen-grupos-administrador.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.

### Pasos
1. **Dado** que el administrador está en la pantalla "Grupos (administración)".
2. **Cuando** pulsa "Nuevo grupo", escribe "1º SMR A", elige el centro "CIPFP Batoi", el curso académico "2024" y el curso "1º SMR".
3. **Y** pulsa "Guardar".

### Resultado esperado
- El grupo "1º SMR A" se crea en estado "Abierto" en el centro "CIPFP Batoi" con curso académico "2024".
- El panel "Módulos" muestra los módulos del curso "1º SMR".

---

## T-013 — El administrador reabre un grupo cerrado

**Origen ESC:** ESC-013
**Verifica:** V-Grupo-008, R-Grupo-004, U-grupos-administrador-003
**Pantalla principal:** screen-grupos-administrador.md
**Tipo:** happy

### Precondiciones
- El usuario `supervisor1@mislata.es` y el usuario `admin` figuran en la tabla de acceso.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM"), le añade a "Alumno1 CIPFP Mislata" y lo cierra; después cierra sesión.
2. **Cuando** el administrador inicia sesión, abre "Grupos (administración)" y entra en "1º DAM A" (está "Cerrado").
3. **Y** pulsa "Reabrir grupo".

### Resultado esperado
- El grupo pasa a "Abierto" y la "Fecha de cierre" queda vacía.
- Vuelve a permitir modificar las notas.

---

## T-014 — El alumno consulta sus notas y su nota media

**Origen ESC:** ESC-014
**Verifica:** —
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor1@mislata.es` y el alumno `alumno1@mislata.es` figuran en la tabla de acceso.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM"), le añade a "Alumno1 CIPFP Mislata" y le pone un "8" en "Programación" y un "6" en "Bases de datos"; después cierra sesión.
2. **Cuando** el alumno "Alumno1 CIPFP Mislata" inicia sesión y abre "Mis notas".
3. **Y** entra en el grupo "1º DAM A".

### Resultado esperado
- El listado de mis grupos muestra "1º DAM A" con nota media "7".
- Dentro del grupo, las notas son "8" en "Programación" y "6" en "Bases de datos", en solo lectura.

---

## T-015 — El alumno no puede modificar sus notas

**Origen ESC:** ESC-015
**Verifica:** —
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** UI

### Precondiciones
- El supervisor `supervisor1@mislata.es` y el alumno `alumno1@mislata.es` figuran en la tabla de acceso.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM"), le añade a "Alumno1 CIPFP Mislata" y le pone un "8" en "Programación"; después cierra sesión.
2. **Cuando** el alumno "Alumno1 CIPFP Mislata" inicia sesión, abre "Mis notas", entra en su grupo y abre la nota de "Programación".

### Resultado esperado
- La nota "8" se muestra en solo lectura, sin opción de cambiarla.

---

## T-016 — Crear un grupo sin curso

**Origen ESC:** ESC-016
**Verifica:** V-Grupo-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo", escribe "1º DAM A" y deja el curso vacío.
3. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema muestra "El curso es obligatorio".
- No se crea el grupo.

---

## T-017 — Renombrar un grupo a un nombre ya existente

**Origen ESC:** ESC-017
**Verifica:** V-Grupo-005
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea y guarda "1º DAM A" (curso "1º DAM").
2. **Y** crea y guarda "1º DAM B" (curso "1º DAM").
3. **Cuando** abre "1º DAM B", cambia su nombre a "1º DAM A" y guarda.

### Resultado esperado
- El sistema muestra "Ya existe un grupo con ese nombre en este centro y curso académico".
- No se cambia el nombre.

---

## T-018 — Añadir un alumno sin elegir ninguno

**Origen ESC:** ESC-018
**Verifica:** V-AlumnoGrupo-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM").
2. **Cuando** en el panel "Alumnos" pulsa "Añadir alumno" y, sin elegir ningún alumno, guarda.

### Resultado esperado
- El sistema muestra "Debe elegir un alumno".
- No se añade nada al grupo.

---

## T-019 — No se puede añadir dos veces el mismo alumno al grupo

**Origen ESC:** ESC-019
**Verifica:** V-AlumnoGrupo-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata".
2. **Cuando** vuelve a pulsar "Añadir alumno", elige otra vez a "Alumno1 CIPFP Mislata" y guarda.

### Resultado esperado
- "Alumno1 CIPFP Mislata" sigue apareciendo una sola vez en el grupo (no se añade de nuevo).

---

## T-020 — El selector de alumno solo ofrece alumnos del centro del grupo

**Origen ESC:** ESC-020
**Verifica:** V-AlumnoGrupo-004, U-grupos-supervisor-006
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** UI

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM").
2. **Cuando** en el panel "Alumnos" pulsa "Añadir alumno" y abre el selector de alumno.

### Resultado esperado
- El selector ofrece a "Alumno1 CIPFP Mislata", "Alumno2 CIPFP Mislata", "Alumno3 CIPFP Mislata" y "Alumno4 CIPFP Mislata" (alumnos de "CIPFP Mislata").
- El selector NO ofrece a "Director CIPFP Mislata" (profesor de "CIPFP Mislata") ni a "Alumno1 CIPFP Batoi" (alumna de "CIPFP Batoi").

---

## T-021 — El supervisor solo ve los grupos de su centro

**Origen ESC:** ESC-021
**Verifica:** —
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` y el usuario `supervisor1@mislata.es` figuran en la tabla de acceso.

### Pasos
1. **Dado** que el administrador crea el grupo "1º SMR A" en el centro "CIPFP Batoi" (curso académico "2024", curso "1º SMR") desde "Grupos (administración)"; después cierra sesión.
2. **Y** el supervisor de "CIPFP Mislata" inicia sesión y crea el grupo "1º DAM A" (curso "1º DAM").
3. **Cuando** el supervisor abre "Grupos".

### Resultado esperado
- El listado muestra "1º DAM A" (de "CIPFP Mislata").
- El listado NO muestra "1º SMR A" (de "CIPFP Batoi").

---

## T-022 — Modificar una nota ya puesta rellena la fecha de última modificación

**Origen ESC:** ESC-022
**Verifica:** R-Nota-001, R-Nota-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata".
2. **Cuando** abre la nota de "Alumno1 CIPFP Mislata" en "Programación", pone "8" y guarda.
3. **Entonces** la nota queda en "8" y la "Fecha de última modificación" está vacía.
4. **Y cuando** vuelve a abrir esa nota, la cambia a "6" y guarda.

### Resultado esperado
- Tras el primer guardado: la nota es "8" y la "Fecha de última modificación" está vacía.
- Tras el segundo guardado: la nota es "6" y la "Fecha de última modificación" muestra la fecha y hora del cambio.

---

## T-023 — Poner un valor de nota inválido

**Origen ESC:** ESC-023
**Verifica:** V-Nota-003
**Pantalla principal:** screen-grupos-supervisor.md (parte UI) + endpoint REST genérico `/ws/rest/com.educaflow.system.gruposnotas.db.Nota` (parte servidor)
**Tipo:** error

> **Nota de ejecutabilidad (VAL-016 / V-Nota-003):** `valor` se modela como enum `ValorNota` (No evaluado / 1..10 / Matrícula de Honor), por lo que la UI lo presenta como un **selector que solo ofrece valores válidos**: desde la pantalla es **imposible** introducir "11". El selector impide por construcción el valor inválido (eso es lo que se comprueba en la parte UI). La defensa servidor V-Nota-003 (VAL-016), que protege ante un valor crudo fuera del enum, solo es alcanzable por la **Vía B** (endpoint REST genérico `/ws/rest`), no por la UI; por eso la comprobación de dominio inválido se ejerce vía petición REST directa.

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata".
2. **Cuando** abre la nota de "Alumno1 CIPFP Mislata" en "Programación" en la pantalla y despliega el selector de "Valor".
3. **Entonces** el selector solo ofrece "No evaluado", "1".."10" y "Matrícula de Honor" — no existe la opción "11" (la UI impide por construcción un valor fuera del dominio).
4. **Y cuando** se envía directamente al endpoint REST genérico (Vía B, `POST /ws/rest/com.educaflow.system.gruposnotas.db.Nota` con la acción de guardado) un `update` de esa misma nota con un `valor` fuera del enum `ValorNota`.

### Resultado esperado
- En la UI, el selector de "Valor" no ofrece "11" ni ningún valor fuera del dominio (No evaluado / 1..10 / Matrícula de Honor).
- En la Vía B (REST), el servidor rechaza el `update` con el mensaje "La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor" (V-Nota-003 / VAL-016) y la nota no cambia.

---

## T-024 — No se puede modificar el nombre de un grupo cerrado

**Origen ESC:** ESC-024
**Verifica:** V-Grupo-004, U-grupos-supervisor-004
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y pulsa "Cerrar grupo".
2. **Cuando** con el grupo "Cerrado" intenta cambiar el nombre a "1º DAM B" y guarda.

### Resultado esperado
- El sistema muestra "No se puede modificar un grupo cerrado".
- El nombre no cambia.

---

## T-025 — No se puede borrar un grupo cerrado

**Origen ESC:** ESC-025
**Verifica:** V-Grupo-009
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y pulsa "Cerrar grupo".
2. **Cuando** con el grupo "Cerrado" intenta borrarlo.

### Resultado esperado
- El sistema muestra "No se puede borrar un grupo cerrado".
- El grupo "1º DAM A" sigue existiendo.

---

## T-026 — El alumno no ve los grupos a los que no pertenece

**Origen ESC:** ESC-026
**Verifica:** —
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** happy

### Precondiciones
- El supervisor `supervisor1@mislata.es` y el alumno `alumno1@mislata.es` figuran en la tabla de acceso.

### Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata".
2. **Y** crea el grupo "1º DAM B" (curso "1º DAM") y le añade a "Alumno2 CIPFP Mislata" (sin añadir a "Alumno1 CIPFP Mislata"); después cierra sesión.
3. **Cuando** el alumno "Alumno1 CIPFP Mislata" inicia sesión y abre "Mis notas".

### Resultado esperado
- El listado muestra "1º DAM A" (al que pertenece).
- El listado NO muestra "1º DAM B" (al que no pertenece).
