# Modelo: Nota

Nota final de un alumno en un módulo del grupo. Hay exactamente una nota por alumno y módulo. El sistema la crea automáticamente como «No evaluado» cuando se añade el alumno al grupo; después el supervisor o el administrador le pone su valor. Cada nota guarda cuándo se registró y cuándo se modificó por última vez. Solo se puede cambiar mientras el grupo está abierto.

## Campos

- **módulo del grupo** — el módulo del grupo al que corresponde la nota
- **alumno del grupo** — el alumno al que corresponde la nota
- **valor** — la calificación: «No evaluado», un número entero del 1 al 10, o «Matrícula de Honor»
- **fecha de calificación** — cuándo se registró la nota (campo calculado)
- **fecha de última modificación** — cuándo se cambió la nota por última vez (campo calculado)

## Restricciones

- RES-006 — Hay una sola nota por alumno del grupo y módulo del grupo

## Campos calculados

- CC-002 — fecha de calificación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: fecha y hora en que se pone valor a la nota por primera vez (cuando deja de estar en «No evaluado»); vacía mientras la nota sigue en «No evaluado»
- CC-003 — fecha de última modificación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: fecha y hora de la última vez que se cambió el valor de la nota; vacía mientras no se haya modificado desde que se registró

## Acción: Crear

**Input AllowProperties:** (ninguna — las notas las crea el sistema al añadir el alumno, siempre como «No evaluado»)

## Acción: Modificar

**Input AllowProperties:** valor

**Validaciones:**

- VAL-015 — El grupo está en estado ABIERTO
  - mensaje: "No se pueden modificar las notas de un grupo cerrado"
- VAL-016 — El valor es «No evaluado», un número entero del 1 al 10, o «Matrícula de Honor»
  - mensaje: "La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor"
- VAL-017 — En el módulo no hay ya 3 notas con «Matrícula de Honor»
  - condición: el valor que se pone es «Matrícula de Honor»
  - mensaje: "No se pueden poner más de 3 matrículas de honor en un módulo"
