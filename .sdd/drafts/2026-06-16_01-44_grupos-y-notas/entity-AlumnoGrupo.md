# Modelo: AlumnoGrupo

Representa que un alumno forma parte de un grupo. El supervisor lo crea eligiendo un alumno entre los usuarios del centro; al añadirlo, el sistema le crea automáticamente una nota «No evaluado» en cada módulo del grupo. Guarda la nota media del alumno en ese grupo. Se puede quitar mientras el grupo está abierto.

## Campos

- **grupo** — el grupo al que pertenece el alumno
- **alumno** — el usuario del centro, de tipo Alumno, que forma parte del grupo
- **nota media** — la nota media del alumno en el grupo (campo calculado)

## Restricciones

- RES-004 — Un alumno no puede pertenecer a más de un grupo del mismo curso académico
- RES-005 — Un mismo alumno no aparece dos veces en el mismo grupo

## Campos calculados

- CC-001 — nota media
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: media, redondeada al entero más cercano, de las notas del alumno en los módulos del grupo, contando la «Matrícula de Honor» como 10 y excluyendo los módulos en «No evaluado»; si el alumno no tiene ningún módulo evaluado, el valor es «Sin nota»

## Acción: Crear

**Input AllowProperties:** alumno, grupo

(El grupo lo rellena la interfaz a partir del grupo padre desde el que se añade el alumno —el formulario de alumno se abre dentro del formulario de su grupo—, por lo que llega como un dato más del cliente; el servidor lo valida antes de aceptarlo.)

**Validaciones:**

- VAL-010 — El alumno está indicado
  - mensaje: "Debe elegir un alumno"
- VAL-011 — El grupo está en estado ABIERTO
  - mensaje: "No se pueden añadir alumnos a un grupo cerrado"
- VAL-012 — El alumno elegido es un usuario del centro del grupo de tipo Alumno
  - mensaje: "El alumno debe ser un usuario de tipo Alumno del centro del grupo"
- VAL-013 — El alumno no pertenece ya a otro grupo del mismo curso académico
  - mensaje: "El alumno ya pertenece a otro grupo de este curso académico"
- VAL-018 — El grupo está indicado
  - mensaje: "El grupo es obligatorio"
- VAL-019 — El grupo pertenece al centro del usuario
  - actor: [SUPERVISOR]
  - mensaje: "El grupo no pertenece a su centro"

**Reglas de negocio:**

- RN-005 — Crear para el alumno una nota «No evaluado» en cada módulo del grupo
  - fase: antes_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — el alumno de una pertenencia no se cambia; se quita y se añade otro)

## Acción: Borrar

**Validaciones:**

- VAL-014 — El grupo está en estado ABIERTO
  - mensaje: "No se pueden quitar alumnos de un grupo cerrado"
