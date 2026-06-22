# Modelo: Grupo

Grupo de alumnos ligado a un curso del catálogo educativo, dentro de un centro y un curso académico. Nace abierto, se le añaden alumnos y se le ponen las notas; cuando las calificaciones son definitivas se cierra, lo que bloquea cualquier cambio. Un grupo cerrado solo puede reabrirlo el administrador.

## Campos

- **nombre** — el nombre del grupo (por ejemplo «1º DAM A»)
- **curso** — el curso del catálogo educativo al que pertenece el grupo; de él derivan el ciclo y los módulos
- **curso académico** — el curso académico al que corresponde el grupo
- **centro** — el centro al que pertenece el grupo
- **estado** — situación del grupo (sus valores, en «Estados y transiciones»)
- **fecha de cierre** — cuándo se cerró el grupo (vacía mientras está abierto)
- **módulos del grupo** — los módulos que imparte el grupo, heredados del curso
- **alumnos del grupo** — los alumnos que forman el grupo, con su nota media

## Estados y transiciones

- Estado inicial: ABIERTO
- ABIERTO → CERRADO: el supervisor o el administrador cierra el grupo; se registra la fecha de cierre.
- CERRADO → ABIERTO: solo el administrador reabre el grupo; se borra la fecha de cierre.
- Mientras el grupo está CERRADO no se admite ningún cambio (ni notas, ni alumnos, ni datos del grupo), salvo la reapertura por el administrador.
- Ningún estado es terminal (un grupo cerrado puede reabrirse).

## Restricciones

- RES-001 — El nombre del grupo es único dentro del mismo centro y curso académico
- RES-002 — Los módulos del grupo coinciden siempre con los módulos del curso al que pertenece el grupo

## Acción: Crear

**Input AllowProperties:** nombre, curso, centro, curso académico, alumnos del grupo

**Validaciones:**

- VAL-001 — El nombre del grupo está indicado
  - mensaje: "El nombre del grupo es obligatorio"
- VAL-002 — El curso está indicado
  - mensaje: "El curso es obligatorio"
- VAL-003 — No existe otro grupo con el mismo nombre en el mismo centro y curso académico
  - mensaje: "Ya existe un grupo con ese nombre en este centro y curso académico"

**Reglas de negocio:**

- RN-001 — Crear los módulos del grupo a partir de los módulos del curso elegido
  - fase: antes_de_commit
- RN-002 — Cuando quien crea el grupo es el supervisor, fijar el centro y el curso académico a los del centro del usuario, ignorando lo que llegue de la interfaz
  - fase: antes_de_commit
  - actor: [SUPERVISOR]

## Acción: Modificar

**Input AllowProperties:** nombre

**Validaciones:**

- VAL-004 — El grupo está en estado ABIERTO
  - mensaje: "No se puede modificar un grupo cerrado"
- VAL-005 — No existe otro grupo con el mismo nombre en el mismo centro y curso académico
  - mensaje: "Ya existe un grupo con ese nombre en este centro y curso académico"

(El curso, el centro y el curso académico no se pueden cambiar tras crear el grupo, porque de ellos dependen los módulos y las notas; por eso no aparecen en la línea de propiedades editables.)

## Acción: Cerrar

**Validaciones:**

- VAL-006 — El grupo está en estado ABIERTO
  - mensaje: "El grupo ya está cerrado"

**Reglas de negocio:**

- RN-003 — Registrar la fecha de cierre y pasar el grupo a CERRADO
  - fase: antes_de_commit

## Acción: Reabrir

**Validaciones:**

- VAL-007 — El grupo está en estado CERRADO
  - mensaje: "El grupo ya está abierto"
- VAL-008 — Solo el administrador puede reabrir un grupo
  - actor: [ADMINISTRADOR]
  - mensaje: "No tiene permisos para reabrir el grupo"

**Reglas de negocio:**

- RN-004 — Borrar la fecha de cierre y pasar el grupo a ABIERTO
  - fase: antes_de_commit

## Acción: Borrar

**Validaciones:**

- VAL-009 — El grupo está en estado ABIERTO
  - mensaje: "No se puede borrar un grupo cerrado"
