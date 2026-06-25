---
type: specification
---

# Objetivo

Permitir que la secretaría de un centro defina grupos de alumnos (ligados a un curso) y registre la nota final de cada alumno en cada módulo del grupo, basándose en las actas. Cada alumno dispone de su nota media en el grupo y puede consultar sus propias notas. Es un **sistema**. Depende funcionalmente del catálogo educativo (cursos y módulos) y de los usuarios y centros que ya gestiona la aplicación.

# Actores

- **Supervisor**: gestiona los grupos de su propio centro: los crea, añade y quita alumnos, pone las notas y cierra el grupo.
- **Administrador**: hace lo mismo que el supervisor pero sobre los grupos de cualquier centro, y además puede reabrir un grupo cerrado.
- **Alumno**: consulta los grupos a los que pertenece, sus notas por módulo y su nota media.

# Historias de usuario

## HU-001 — Como Supervisor quiero crear un grupo con sus alumnos para poder gestionar sus calificaciones

- ESC-001 — Crear un grupo con alumnos:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Abre «Grupos», pulsa «Nuevo grupo», escribe el nombre «1º DAM A» y elige el curso «1º DAM».
  3. Guarda el grupo.
  4. El sistema crea el grupo en estado ABIERTO, le fija el centro «CIPFP Mislata» y el curso académico del centro, y rellena automáticamente sus módulos con los del curso «1º DAM» («Programación» y «Bases de datos»).
  5. En el panel de alumnos del grupo, el supervisor añade a los alumnos «Alumno1 CIPFP Mislata» y «Alumno2 CIPFP Mislata», eligiéndolos entre los usuarios del centro.
  6. El sistema crea, para cada alumno añadido, una nota «No evaluado» en cada módulo del grupo, y muestra a cada alumno con nota media «Sin nota».
- ESC-002 — Nombre de grupo duplicado:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea y guarda un grupo «1º DAM A» con el curso «1º DAM».
  3. Pulsa «Nuevo grupo», escribe de nuevo el nombre «1º DAM A» con el curso «1º DAM» y guarda.
  4. El sistema muestra «Ya existe un grupo con ese nombre en este centro y curso académico» y no crea el grupo.
- ESC-003 — Crear un grupo sin nombre:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Pulsa «Nuevo grupo», elige el curso «1º DAM» pero deja el nombre vacío y guarda.
  3. El sistema muestra «El nombre del grupo es obligatorio» y no crea el grupo.
- ESC-004 — Un alumno no puede estar en dos grupos del mismo curso académico:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade al alumno «Alumno1 CIPFP Mislata».
  3. Crea el grupo «1º DAM B» con el curso «1º DAM».
  4. En el panel de alumnos de «1º DAM B» intenta añadir también a «Alumno1 CIPFP Mislata».
  5. El sistema muestra «El alumno ya pertenece a otro grupo de este curso académico» y no lo añade.
- ESC-005 — Quitar un alumno de un grupo abierto:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Alumno1 CIPFP Mislata» y «Alumno2 CIPFP Mislata».
  3. En el panel de alumnos quita a «Alumno2 CIPFP Mislata».
  4. El sistema elimina a «Alumno2 CIPFP Mislata» del grupo junto con todas sus notas, y «Alumno1 CIPFP Mislata» permanece.
- ESC-016 — Crear un grupo sin curso:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Abre «Grupos», pulsa «Nuevo grupo», escribe el nombre «1º DAM A» pero deja el curso vacío.
  3. Guarda el grupo.
  4. El sistema muestra «El curso es obligatorio» y no crea el grupo.
- ESC-017 — Renombrar un grupo a un nombre ya existente:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea y guarda un grupo «1º DAM A» con el curso «1º DAM».
  3. Crea y guarda un segundo grupo «1º DAM B» con el curso «1º DAM».
  4. Abre el grupo «1º DAM B», cambia su nombre a «1º DAM A» y guarda.
  5. El sistema muestra «Ya existe un grupo con ese nombre en este centro y curso académico» y no cambia el nombre.
- ESC-018 — Añadir un alumno sin elegir ninguno:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM».
  3. En el panel de alumnos del grupo pulsa «Añadir alumno» y, sin elegir ningún alumno, guarda.
  4. El sistema muestra «Debe elegir un alumno» y no añade nada al grupo.
- ESC-019 — No se puede añadir dos veces el mismo alumno al grupo:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade al alumno «Alumno1 CIPFP Mislata».
  3. En el panel de alumnos vuelve a pulsar «Añadir alumno», elige otra vez a «Alumno1 CIPFP Mislata» y guarda.
  4. El sistema no lo añade de nuevo: «Alumno1 CIPFP Mislata» sigue apareciendo una sola vez en el grupo.
- ESC-020 — El selector de alumno solo ofrece alumnos del centro del grupo:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM».
  3. En el panel de alumnos pulsa «Añadir alumno» y abre el selector de alumno.
  4. El sistema ofrece a los alumnos de «CIPFP Mislata» («Alumno1 CIPFP Mislata», «Alumno2 CIPFP Mislata», «Alumno3 CIPFP Mislata», «Alumno4 CIPFP Mislata») pero no ofrece a «Director CIPFP Mislata» (profesor de «CIPFP Mislata») ni a «Alumno1 CIPFP Batoi» (alumna de «CIPFP Batoi»), de modo que ninguno de los dos se puede añadir al grupo.
- ESC-021 — El supervisor solo ve los grupos de su centro:
  1. El administrador «admin» inicia sesión, abre «Grupos (administración)», pulsa «Nuevo grupo» y crea el grupo «1º SMR A» con el centro «CIPFP Batoi», el curso académico «2024» y el curso «1º SMR».
  2. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión y crea el grupo «1º DAM A» con el curso «1º DAM».
  3. El supervisor abre «Grupos».
  4. El sistema muestra el grupo «1º DAM A» (de «CIPFP Mislata») pero no muestra el grupo «1º SMR A» (de «CIPFP Batoi»).

## HU-002 — Como Supervisor quiero poner y modificar la nota de cada alumno en cada módulo para reflejar las actas

- ESC-006 — Poner una nota numérica:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Alumno1 CIPFP Mislata».
  3. Entra en el grupo, abre el módulo «Programación», abre la fila del alumno «Alumno1 CIPFP Mislata» y pone la nota 8.
  4. Guarda la nota.
  5. El sistema deja la nota de «Alumno1 CIPFP Mislata» en «Programación» con valor 8, registra la fecha de calificación y actualiza la nota media de «Alumno1 CIPFP Mislata» a 8.
- ESC-007 — La nota media excluye los módulos no evaluados y cuenta la matrícula de honor como 10:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» (módulos «Programación» y «Bases de datos») y le añade a «Alumno1 CIPFP Mislata».
  3. Pone a «Alumno1 CIPFP Mislata» la nota «Matrícula de Honor» en «Programación» y deja «Bases de datos» en «No evaluado».
  4. El sistema calcula la nota media de «Alumno1 CIPFP Mislata» como 10 (la matrícula de honor cuenta como 10 y el módulo no evaluado no se cuenta).
  5. Si después pone «Bases de datos» en 7: la nota media pasa a 9 (media de 10 y 7, redondeada al entero más cercano).
- ESC-008 — Media «Sin nota» cuando no hay ningún módulo evaluado:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Alumno2 CIPFP Mislata» sin ponerle ninguna nota.
  3. Consulta el panel de alumnos del grupo.
  4. El sistema muestra a «Alumno2 CIPFP Mislata» con nota media «Sin nota».
- ESC-009 — Máximo tres matrículas de honor por módulo:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a cuatro alumnos: «Alumno1 CIPFP Mislata», «Alumno2 CIPFP Mislata», «Alumno3 CIPFP Mislata» y «Alumno4 CIPFP Mislata».
  3. En el módulo «Programación» pone «Matrícula de Honor» a «Alumno1 CIPFP Mislata», «Alumno2 CIPFP Mislata» y «Alumno3 CIPFP Mislata».
  4. Intenta poner «Matrícula de Honor» también a «Alumno4 CIPFP Mislata» en «Programación».
  5. El sistema muestra «No se pueden poner más de 3 matrículas de honor en un módulo» y no cambia la nota de «Alumno4 CIPFP Mislata».
- ESC-022 — Modificar una nota ya puesta rellena la fecha de última modificación:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Alumno1 CIPFP Mislata».
  3. Entra en el grupo, abre el módulo «Programación», abre la nota de «Alumno1 CIPFP Mislata», pone la nota 8 y guarda.
  4. El sistema deja la nota de «Alumno1 CIPFP Mislata» en «Programación» con valor 8 y la fecha de última modificación vacía.
  5. Vuelve a abrir esa misma nota, la cambia a 6 y guarda.
  6. El sistema deja la nota en 6 y rellena la fecha de última modificación con la fecha y hora del cambio.
- ESC-023 — Poner un valor de nota inválido:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade a «Alumno1 CIPFP Mislata».
  3. Entra en el grupo, abre el módulo «Programación», abre la nota de «Alumno1 CIPFP Mislata», pone el valor 11 y guarda.
  4. El sistema muestra «La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor» y no cambia la nota.

## HU-003 — Como Supervisor quiero cerrar un grupo para que sus notas queden definitivas

- ESC-010 — Cerrar un grupo bloquea sus notas y sus alumnos:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM», le añade a «Alumno1 CIPFP Mislata» y le pone un 8 en «Programación».
  3. Pulsa «Cerrar grupo».
  4. El sistema pasa el grupo a CERRADO y registra la fecha de cierre.
  5. El supervisor intenta cambiar la nota de «Alumno1 CIPFP Mislata» en «Programación».
  6. El sistema muestra «No se pueden modificar las notas de un grupo cerrado» y no cambia la nota; tampoco permite añadir ni quitar alumnos.
- ESC-011 — El supervisor no puede reabrir un grupo cerrado:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y lo cierra con «Cerrar grupo».
  3. Vuelve a abrir el grupo cerrado.
  4. El sistema no muestra al supervisor el botón «Reabrir grupo».
- ESC-024 — No se puede modificar el nombre de un grupo cerrado:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y pulsa «Cerrar grupo».
  3. Con el grupo en estado CERRADO, intenta cambiar el nombre a «1º DAM B» y guarda.
  4. El sistema muestra «No se puede modificar un grupo cerrado» y no cambia el nombre.
- ESC-025 — No se puede borrar un grupo cerrado:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y pulsa «Cerrar grupo».
  3. Con el grupo en estado CERRADO, intenta borrarlo.
  4. El sistema muestra «No se puede borrar un grupo cerrado» y el grupo «1º DAM A» sigue existiendo.

## HU-004 — Como Administrador quiero gestionar grupos de cualquier centro y reabrir grupos cerrados

- ESC-012 — El administrador crea un grupo en otro centro eligiendo centro y curso académico:
  1. El administrador «admin» inicia sesión.
  2. Abre «Grupos (administración)», pulsa «Nuevo grupo», escribe el nombre «1º SMR A», elige el centro «CIPFP Batoi», el curso académico «2024» y el curso «1º SMR».
  3. Guarda el grupo.
  4. El sistema crea el grupo en estado ABIERTO en el centro «CIPFP Batoi» con el curso académico «2024» y sus módulos.
- ESC-013 — El administrador reabre un grupo cerrado:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión, crea el grupo «1º DAM A» con el curso «1º DAM», le añade a «Alumno1 CIPFP Mislata» y lo cierra.
  2. El administrador «admin» inicia sesión, abre «Grupos (administración)» y entra en el grupo «1º DAM A» (que está CERRADO).
  3. Pulsa «Reabrir grupo».
  4. El sistema pasa el grupo a ABIERTO y borra la fecha de cierre, y vuelve a permitir modificar las notas.

## HU-005 — Como Alumno quiero consultar mis notas y mi nota media

- ESC-014 — El alumno consulta sus notas y su nota media:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión, crea el grupo «1º DAM A» con el curso «1º DAM», le añade al alumno «Alumno1 CIPFP Mislata» y le pone un 8 en «Programación» y un 6 en «Bases de datos».
  2. El alumno «Alumno1 CIPFP Mislata» inicia sesión y abre «Mis notas».
  3. El sistema muestra su grupo «1º DAM A» con la nota media 7.
  4. Entra en el grupo y ve sus módulos con sus notas (8 en «Programación» y 6 en «Bases de datos»), en solo lectura.
- ESC-015 — El alumno no puede modificar sus notas:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión, crea el grupo «1º DAM A» con el curso «1º DAM», le añade a «Alumno1 CIPFP Mislata» y le pone un 8 en «Programación».
  2. El alumno «Alumno1 CIPFP Mislata» inicia sesión, abre «Mis notas», entra en su grupo y abre la nota de «Programación».
  3. El sistema muestra la nota 8 en solo lectura, sin opción de cambiarla.
- ESC-026 — El alumno no ve los grupos a los que no pertenece:
  1. El supervisor «Supervisor1 CIPFP Mislata» del centro «CIPFP Mislata» inicia sesión.
  2. Crea el grupo «1º DAM A» con el curso «1º DAM» y le añade al alumno «Alumno1 CIPFP Mislata».
  3. Crea un segundo grupo «1º DAM B» con el curso «1º DAM» y le añade a la alumna «Alumno2 CIPFP Mislata» (sin añadir a «Alumno1 CIPFP Mislata»).
  4. El alumno «Alumno1 CIPFP Mislata» inicia sesión y abre «Mis notas».
  5. El sistema muestra el grupo «1º DAM A» (al que pertenece) pero no muestra el grupo «1º DAM B» (al que no pertenece).

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-Grupo.md](./entity-Grupo.md) | Grupo | Grupo de alumnos ligado a un curso, dentro de un centro y un curso académico, con su ciclo de apertura y cierre. |
| [entity-ModuloGrupo.md](./entity-ModuloGrupo.md) | ModuloGrupo | Cada uno de los módulos que imparte el grupo, heredado del curso. |
| [entity-AlumnoGrupo.md](./entity-AlumnoGrupo.md) | AlumnoGrupo | Pertenencia de un alumno a un grupo, con su nota media en él. |
| [entity-Nota.md](./entity-Nota.md) | Nota | Nota final de un alumno en un módulo del grupo, con sus fechas. |

Un Grupo referencia un único curso del catálogo educativo (de él derivan el ciclo y los módulos) y pertenece a un único centro. Un Grupo contiene varios ModuloGrupo y varios AlumnoGrupo (composición: al borrar el grupo se borran sus módulos, sus alumnos y sus notas). Cada AlumnoGrupo referencia a un usuario del centro de tipo Alumno. Una Nota pertenece a un ModuloGrupo y referencia a un AlumnoGrupo (composición: al quitar un alumno del grupo se borran sus notas; al borrar un módulo del grupo se borran sus notas). Los módulos de un grupo coinciden siempre con los módulos del curso del grupo.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-grupos-supervisor.md](./screen-grupos-supervisor.md) | Grupos (supervisor) | Gestión por el supervisor de los grupos de su centro: alta, alumnos, notas y cierre. |
| [screen-grupos-administrador.md](./screen-grupos-administrador.md) | Grupos (administración) | Gestión por el administrador de los grupos de cualquier centro, incluida la reapertura. |
| [screen-mis-notas-alumno.md](./screen-mis-notas-alumno.md) | Mis notas (alumno) | Consulta por el alumno de sus grupos, sus notas por módulo y su nota media, en solo lectura. |

# Seguridad

- **Supervisor:** gestiona los grupos de **su propio centro** (los crea, edita el nombre, añade y quita alumnos, pone y modifica notas y cierra el grupo). Al crear, el centro y el curso académico se fijan automáticamente a los de su centro. No puede reabrir un grupo cerrado. Solo ve y gestiona los grupos de su centro.
- **Administrador:** gestiona los grupos de **todos los centros** con las mismas operaciones que el supervisor y, además, puede **reabrir** un grupo cerrado. Al crear, elige el centro y el curso académico.
- **Alumno:** consulta en **solo lectura** únicamente sus propios grupos, sus notas por módulo y su nota media; no crea ni modifica nada.

# Recursos y datos iniciales

Estado previo del que parten los escenarios (datos maestros gestionados por otros subsistemas de la aplicación):

- Al menos dos centros de prueba: «CIPFP Mislata» y «CIPFP Batoi», cada uno con su curso académico.
- En el catálogo educativo, los cursos «1º DAM» (con los módulos «Programación» y «Bases de datos») y «1º SMR».
- Usuarios de prueba con acceso: un administrador (usuario «admin», contraseña «admin»), un supervisor del centro «CIPFP Mislata» («Supervisor1 CIPFP Mislata»), varios usuarios de tipo Alumno del centro «CIPFP Mislata» («Alumno1 CIPFP Mislata», «Alumno2 CIPFP Mislata», «Alumno3 CIPFP Mislata», «Alumno4 CIPFP Mislata»), un usuario de tipo Profesor del centro «CIPFP Mislata» («Director CIPFP Mislata») y un usuario de tipo Alumno del centro «CIPFP Batoi» («Alumno1 CIPFP Batoi»).

# Fuera de alcance

- Que el profesor ponga las notas (las pone la secretaría a partir de las actas).
- Que los familiares consulten las notas del alumno.
- Varias evaluaciones por módulo (solo se registra la nota final).
- Generación de actas o boletines en PDF, exportaciones y avisos al alumno al publicar las notas.
