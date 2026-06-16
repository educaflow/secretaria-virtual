# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-app` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## T-001 — Crear un grupo con alumnos genera módulos, estado ABIERTO y notas «No evaluado»

**Origen ESC:** ESC-001
**Verifica:** R-Grupo-001, R-Grupo-002, R-Grupo-003, V-AlumnoGrupo-001, R-AlumnoGrupo-001, CC-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico.
- En el catálogo educativo existe el curso «1º DAM» con los módulos «Programación» y «Bases de datos».
- Existe un supervisor del centro «IES Mislata» y los alumnos «Juan Pérez» y «Ana López» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** abre la pantalla «Grupos».
3. **Cuando** pulsa «Nuevo grupo», escribe el nombre «1º DAM A» y elige el curso «1º DAM».
4. **Y** pulsa «Guardar».
5. **Y** en el panel «Alumnos» del grupo pulsa «Añadir alumno», elige a «Juan Pérez» y pulsa «Guardar».
6. **Y** repite la acción para añadir a «Ana López».
7. **Entonces** el grupo «1º DAM A» queda creado.

### Resultado esperado
- El grupo «1º DAM A» se crea en estado ABIERTO, con el centro «IES Mislata» y el curso académico del centro.
- El panel «Módulos» muestra «Programación» y «Bases de datos».
- El panel «Alumnos» muestra a «Juan Pérez» y «Ana López», ambos con nota media «Sin nota».
- Cada alumno tiene una nota «No evaluado» en cada módulo del grupo.

---

## T-002 — Nombre de grupo duplicado en alta

**Origen ESC:** ESC-002
**Verifica:** V-Grupo-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** en «Grupos» ha creado y guardado un grupo «1º DAM A» con el curso «1º DAM».
3. **Cuando** pulsa «Nuevo grupo», escribe de nuevo el nombre «1º DAM A» con el curso «1º DAM» y pulsa «Guardar».
4. **Entonces** el sistema rechaza el alta.

### Resultado esperado
- Se muestra el mensaje «Ya existe un grupo con ese nombre en este centro y curso académico».
- No se crea el segundo grupo.

---

## T-003 — Crear un grupo sin nombre

**Origen ESC:** ESC-003
**Verifica:** V-Grupo-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** abre «Grupos».
3. **Cuando** pulsa «Nuevo grupo», elige el curso «1º DAM», deja el nombre vacío y pulsa «Guardar».
4. **Entonces** el sistema rechaza el alta.

### Resultado esperado
- Se muestra el mensaje «El nombre del grupo es obligatorio».
- No se crea el grupo.

---

## T-004 — Crear un grupo sin curso

**Origen ESC:** ESC-016
**Verifica:** V-Grupo-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** abre «Grupos».
3. **Cuando** pulsa «Nuevo grupo», escribe el nombre «1º DAM A», deja el curso vacío y pulsa «Guardar».
4. **Entonces** el sistema rechaza el alta.

### Resultado esperado
- Se muestra el mensaje «El curso es obligatorio».
- No se crea el grupo.

---

## T-005 — Un alumno no puede estar en dos grupos del mismo curso académico

**Origen ESC:** ESC-004
**Verifica:** V-AlumnoGrupo-004
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade al alumno «Juan Pérez».
3. **Y** crea el grupo «1º DAM B» con el curso «1º DAM».
4. **Cuando** en el panel «Alumnos» de «1º DAM B» pulsa «Añadir alumno», elige a «Juan Pérez» y pulsa «Guardar».
5. **Entonces** el sistema rechaza el alta del alumno.

### Resultado esperado
- Se muestra el mensaje «El alumno ya pertenece a otro grupo de este curso académico».
- «Juan Pérez» no se añade al grupo «1º DAM B».

---

## T-006 — Quitar un alumno de un grupo abierto borra sus notas

**Origen ESC:** ESC-005
**Verifica:** V-AlumnoGrupo-005
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y los alumnos «Juan Pérez» y «Ana López» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Juan Pérez» y «Ana López».
3. **Cuando** en el panel «Alumnos» quita a «Ana López».
4. **Entonces** el grupo se actualiza.

### Resultado esperado
- «Ana López» deja de aparecer en el panel «Alumnos» del grupo y se eliminan todas sus notas.
- «Juan Pérez» permanece en el grupo.

---

## T-007 — Renombrar un grupo a un nombre ya existente

**Origen ESC:** ESC-017
**Verifica:** V-Grupo-005
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea y guarda el grupo «1º DAM A» con el curso «1º DAM».
3. **Y** crea y guarda el grupo «1º DAM B» con el curso «1º DAM».
4. **Cuando** abre el grupo «1º DAM B», cambia su nombre a «1º DAM A» y pulsa «Guardar».
5. **Entonces** el sistema rechaza el cambio.

### Resultado esperado
- Se muestra el mensaje «Ya existe un grupo con ese nombre en este centro y curso académico».
- El nombre del grupo «1º DAM B» no cambia.

---

## T-008 — Añadir un alumno sin elegir ninguno

**Origen ESC:** ESC-018
**Verifica:** V-AlumnoGrupo-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM».
3. **Cuando** en el panel «Alumnos» pulsa «Añadir alumno» y, sin elegir ningún alumno, pulsa «Guardar».
4. **Entonces** el sistema rechaza el alta.

### Resultado esperado
- Se muestra el mensaje «Debe elegir un alumno».
- No se añade nada al grupo.

---

## T-009 — No se puede añadir dos veces el mismo alumno al grupo

**Origen ESC:** ESC-019
**Verifica:** —
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade al alumno «Juan Pérez».
3. **Cuando** en el panel «Alumnos» vuelve a pulsar «Añadir alumno», elige otra vez a «Juan Pérez» y pulsa «Guardar».
4. **Entonces** el sistema no lo añade de nuevo.

### Resultado esperado
- «Juan Pérez» sigue apareciendo una sola vez en el panel «Alumnos» del grupo.

---

## T-010 — El selector de alumno solo ofrece alumnos del centro del grupo

**Origen ESC:** ESC-020
**Verifica:** V-AlumnoGrupo-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** UI

### Precondiciones
- Existen los centros «IES Mislata» y «IES Benicalap» con sus cursos académicos y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata», los alumnos «Juan Pérez», «Ana López», «Luis Gil» y «Marta Ruiz» de «IES Mislata», el profesor «Pedro Sanz» de «IES Mislata» y la alumna «Sara Vidal» de «IES Benicalap».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM».
3. **Cuando** en el panel «Alumnos» pulsa «Añadir alumno» y abre el selector de alumno.
4. **Entonces** el selector lista a los alumnos de «IES Mislata».

### Resultado esperado
- El selector ofrece a «Juan Pérez», «Ana López», «Luis Gil» y «Marta Ruiz».
- El selector no ofrece a «Pedro Sanz» (profesor de «IES Mislata») ni a «Sara Vidal» (alumna de «IES Benicalap»), de modo que ninguno de los dos puede añadirse al grupo.

---

## T-011 — El supervisor solo ve los grupos de su centro

**Origen ESC:** ESC-021
**Verifica:** R-Grupo-002, U-grupos-administrador-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- Existen los centros «IES Mislata» y «IES Benicalap» con sus cursos académicos.
- En el catálogo educativo existen los cursos «1º DAM» y «1º SMR».
- Existen un administrador, un supervisor del centro «IES Mislata», y el curso académico «2024/2025» de «IES Benicalap».

### Pasos
1. **Dado** que el administrador ha iniciado sesión, abre «Grupos (administración)», pulsa «Nuevo grupo» y crea el grupo «1º SMR A» con el centro «IES Benicalap», el curso académico «2024/2025» y el curso «1º SMR».
2. **Y** el supervisor del centro «IES Mislata» inicia sesión y crea el grupo «1º DAM A» con el curso «1º DAM».
3. **Cuando** el supervisor abre «Grupos».
4. **Entonces** el listado solo muestra los grupos de su centro.

### Resultado esperado
- El listado «Grupos» muestra el grupo «1º DAM A» (de «IES Mislata»).
- El listado «Grupos» no muestra el grupo «1º SMR A» (de «IES Benicalap»).

---

## T-012 — Poner una nota numérica registra fecha de calificación y nota media

**Origen ESC:** ESC-006
**Verifica:** V-Nota-002, R-Nota-001, CC-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Juan Pérez».
3. **Cuando** entra en el grupo, abre el módulo «Programación», abre la fila del alumno «Juan Pérez», pone la nota 8 y pulsa «Guardar».
4. **Entonces** la nota se registra.

### Resultado esperado
- La nota de «Juan Pérez» en «Programación» queda con valor 8 y registra la fecha de calificación.
- La nota media de «Juan Pérez» en el grupo es 8.

---

## T-013 — La nota media excluye no evaluados y cuenta matrícula de honor como 10

**Origen ESC:** ESC-007
**Verifica:** CC-001, V-Nota-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM» (módulos «Programación» y «Bases de datos»).
- Existe un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Juan Pérez».
3. **Cuando** pone a «Juan Pérez» la nota «Matrícula de Honor» en «Programación» y deja «Bases de datos» en «No evaluado».
4. **Entonces** la nota media de «Juan Pérez» es 10.
5. **Y cuando** después pone «Bases de datos» en 7.
6. **Entonces** la nota media de «Juan Pérez» pasa a 9.

### Resultado esperado
- Con «Matrícula de Honor» en «Programación» y «Bases de datos» «No evaluado», la nota media es 10 (la matrícula cuenta como 10 y el módulo no evaluado no se cuenta).
- Con «Programación» en «Matrícula de Honor» y «Bases de datos» en 7, la nota media es 9 (media de 10 y 7 redondeada al entero más cercano).

---

## T-014 — Media «Sin nota» cuando no hay ningún módulo evaluado

**Origen ESC:** ESC-008
**Verifica:** CC-001, R-AlumnoGrupo-001
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y la alumna «Ana López» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Ana López» sin ponerle ninguna nota.
3. **Cuando** consulta el panel «Alumnos» del grupo.
4. **Entonces** ve la nota media de «Ana López».

### Resultado esperado
- «Ana López» aparece con nota media «Sin nota».

---

## T-015 — Máximo tres matrículas de honor por módulo

**Origen ESC:** ESC-009
**Verifica:** V-Nota-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y los alumnos «Juan Pérez», «Ana López», «Luis Gil» y «Marta Ruiz» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Juan Pérez», «Ana López», «Luis Gil» y «Marta Ruiz».
3. **Y** en el módulo «Programación» pone «Matrícula de Honor» a «Juan Pérez», «Ana López» y «Luis Gil».
4. **Cuando** intenta poner «Matrícula de Honor» también a «Marta Ruiz» en «Programación» y pulsa «Guardar».
5. **Entonces** el sistema rechaza la cuarta matrícula.

### Resultado esperado
- Se muestra el mensaje «No se pueden poner más de 3 matrículas de honor en un módulo».
- La nota de «Marta Ruiz» en «Programación» no cambia.

---

## T-016 — Modificar una nota ya puesta rellena la fecha de última modificación

**Origen ESC:** ESC-022
**Verifica:** R-Nota-001, R-Nota-002, V-Nota-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Juan Pérez».
3. **Cuando** entra en el grupo, abre el módulo «Programación», abre la nota de «Juan Pérez», pone la nota 8 y pulsa «Guardar».
4. **Entonces** la nota queda en 8 con la fecha de última modificación vacía.
5. **Y cuando** vuelve a abrir esa misma nota, la cambia a 6 y pulsa «Guardar».
6. **Entonces** la nota queda en 6 y se rellena la fecha de última modificación.

### Resultado esperado
- Tras la primera puesta, la nota es 8 y la fecha de última modificación está vacía.
- Tras el cambio, la nota es 6 y la fecha de última modificación contiene la fecha y hora del cambio.

---

## T-017 — Poner un valor de nota inválido

**Origen ESC:** ESC-023
**Verifica:** V-Nota-002
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Juan Pérez».
3. **Cuando** entra en el grupo, abre el módulo «Programación», abre la nota de «Juan Pérez», pone el valor 11 y pulsa «Guardar».
4. **Entonces** el sistema rechaza el valor.

### Resultado esperado
- Se muestra el mensaje «La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor».
- La nota de «Juan Pérez» no cambia.

---

## T-018 — Cerrar un grupo bloquea sus notas y sus alumnos

**Origen ESC:** ESC-010
**Verifica:** R-Grupo-004, V-Grupo-006, V-Nota-001, V-AlumnoGrupo-002, V-AlumnoGrupo-005
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» del mismo centro.

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM», le añade a «Juan Pérez» y le pone un 8 en «Programación».
3. **Cuando** pulsa «Cerrar grupo».
4. **Entonces** el grupo pasa a CERRADO y registra la fecha de cierre.
5. **Y cuando** intenta cambiar la nota de «Juan Pérez» en «Programación».
6. **Entonces** el sistema lo impide.

### Resultado esperado
- El grupo «1º DAM A» queda en estado CERRADO con la fecha de cierre registrada.
- Al intentar cambiar la nota se muestra «No se pueden modificar las notas de un grupo cerrado» y la nota no cambia.
- No se permite añadir ni quitar alumnos del grupo cerrado.

---

## T-019 — El supervisor no puede reabrir un grupo cerrado

**Origen ESC:** ESC-011
**Verifica:** V-Grupo-008, U-grupos-supervisor-003
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** UI

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y lo cierra con «Cerrar grupo».
3. **Cuando** vuelve a abrir el grupo cerrado «1º DAM A».
4. **Entonces** la pantalla del supervisor no le ofrece reabrirlo.

### Resultado esperado
- El formulario de grupo del supervisor no muestra el botón «Reabrir grupo».

---

## T-020 — No se puede modificar el nombre de un grupo cerrado

**Origen ESC:** ESC-024
**Verifica:** V-Grupo-004
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y pulsa «Cerrar grupo».
3. **Cuando** con el grupo en estado CERRADO intenta cambiar el nombre a «1º DAM B» y pulsa «Guardar».
4. **Entonces** el sistema rechaza el cambio.

### Resultado esperado
- Se muestra el mensaje «No se puede modificar un grupo cerrado».
- El nombre del grupo no cambia.

---

## T-021 — No se puede borrar un grupo cerrado

**Origen ESC:** ESC-025
**Verifica:** V-Grupo-009
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** error

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existe un supervisor del centro «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y pulsa «Cerrar grupo».
3. **Cuando** con el grupo en estado CERRADO intenta borrarlo con «Borrar».
4. **Entonces** el sistema rechaza el borrado.

### Resultado esperado
- Se muestra el mensaje «No se puede borrar un grupo cerrado».
- El grupo «1º DAM A» sigue existiendo.

---

## T-022 — El administrador crea un grupo en otro centro eligiendo centro y curso académico

**Origen ESC:** ESC-012
**Verifica:** U-grupos-administrador-001, R-Grupo-001, R-Grupo-003
**Pantalla principal:** screen-grupos-administrador.md
**Tipo:** happy

### Precondiciones
- Existen los centros «IES Mislata» y «IES Benicalap», con el curso académico «2024/2025» de «IES Benicalap».
- En el catálogo educativo existe el curso «1º SMR».
- Existe un administrador.

### Pasos
1. **Dado** que el administrador ha iniciado sesión.
2. **Y** abre «Grupos (administración)».
3. **Cuando** pulsa «Nuevo grupo», escribe el nombre «1º SMR A», elige el centro «IES Benicalap», el curso académico «2024/2025» y el curso «1º SMR», y pulsa «Guardar».
4. **Entonces** el grupo se crea en el centro elegido.

### Resultado esperado
- El grupo «1º SMR A» se crea en estado ABIERTO en el centro «IES Benicalap» con el curso académico «2024/2025».
- El panel «Módulos» muestra los módulos del curso «1º SMR».

---

## T-023 — El administrador reabre un grupo cerrado

**Origen ESC:** ESC-013
**Verifica:** R-Grupo-005, V-Grupo-007, V-Grupo-008, U-grupos-administrador-003
**Pantalla principal:** screen-grupos-administrador.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existen un supervisor del centro «IES Mislata», un administrador y el alumno «Juan Pérez» de «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión, crea el grupo «1º DAM A» con el curso «1º DAM», le añade a «Juan Pérez» y lo cierra con «Cerrar grupo».
2. **Y** el administrador inicia sesión, abre «Grupos (administración)» y entra en el grupo «1º DAM A» (que está CERRADO).
3. **Cuando** pulsa «Reabrir grupo».
4. **Entonces** el grupo se reabre.

### Resultado esperado
- El grupo «1º DAM A» pasa a estado ABIERTO y se borra la fecha de cierre.
- Las notas del grupo vuelven a ser modificables.

---

## T-024 — El alumno consulta sus notas y su nota media

**Origen ESC:** ESC-014
**Verifica:** CC-001, U-grupos-supervisor-005
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM» (módulos «Programación» y «Bases de datos»).
- Existen un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» de «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión, crea el grupo «1º DAM A» con el curso «1º DAM», le añade a «Juan Pérez» y le pone un 8 en «Programación» y un 6 en «Bases de datos».
2. **Y** el alumno «Juan Pérez» inicia sesión y abre «Mis notas».
3. **Cuando** entra en su grupo «1º DAM A».
4. **Entonces** ve sus notas por módulo en solo lectura.

### Resultado esperado
- El listado «Mis notas» muestra el grupo «1º DAM A» con la nota media 7.
- Dentro del grupo ve sus módulos con sus notas: 8 en «Programación» y 6 en «Bases de datos», en solo lectura.

---

## T-025 — El alumno no puede modificar sus notas

**Origen ESC:** ESC-015
**Verifica:** U-grupos-supervisor-005
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** UI

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existen un supervisor del centro «IES Mislata» y el alumno «Juan Pérez» de «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión, crea el grupo «1º DAM A» con el curso «1º DAM», le añade a «Juan Pérez» y le pone un 8 en «Programación».
2. **Y** el alumno «Juan Pérez» inicia sesión, abre «Mis notas» y entra en su grupo «1º DAM A».
3. **Cuando** abre la nota de «Programación».
4. **Entonces** la nota se muestra sin posibilidad de edición.

### Resultado esperado
- El formulario de mi nota muestra la nota 8 en solo lectura, sin opción de cambiarla.

---

## T-026 — El alumno no ve los grupos a los que no pertenece

**Origen ESC:** ESC-026
**Verifica:** CC-001
**Pantalla principal:** screen-mis-notas-alumno.md
**Tipo:** happy

### Precondiciones
- Existe el centro «IES Mislata» con su curso académico y el curso «1º DAM».
- Existen un supervisor del centro «IES Mislata» y los alumnos «Juan Pérez» y «Ana López» de «IES Mislata».

### Pasos
1. **Dado** que el supervisor del centro «IES Mislata» ha iniciado sesión.
2. **Y** crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Juan Pérez».
3. **Y** crea el grupo «1º DAM B» con el curso «1º DAM» y le añade a «Ana López» (sin añadir a «Juan Pérez»).
4. **Cuando** el alumno «Juan Pérez» inicia sesión y abre «Mis notas».
5. **Entonces** solo ve sus propios grupos.

### Resultado esperado
- El listado «Mis notas» muestra el grupo «1º DAM A» (al que pertenece).
- El listado «Mis notas» no muestra el grupo «1º DAM B» (al que no pertenece).
