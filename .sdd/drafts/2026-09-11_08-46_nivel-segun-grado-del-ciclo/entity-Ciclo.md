# Modelo: Ciclo

**Modelo existente:** sí

El ciclo es la enseñanza que imparte un centro, con su familia profesional, su grado y, cuando el grado lo admite, su nivel.
Lo que cambia es que la relación entre grado y nivel deja de ser una recomendación de la pantalla y pasa a ser una condición que el ciclo cumple siempre: si su grado admite nivel, el ciclo lo tiene; si no lo admite, el ciclo no lo tiene; y el nivel que tenga es siempre uno de los del grado del ciclo.
Estas tres condiciones valen para cualquier vía de entrada, no solo para el formulario de mantenimiento.

## Campos

*(el delta no añade ni cambia ningún campo: el grado y el nivel del ciclo ya existen)*

## Restricciones

- RES-Ciclo-001 — El ciclo tiene nivel indicado cuando su grado admite nivel
  - mensaje: «El nivel es obligatorio para el grado indicado»
- RES-Ciclo-002 — El ciclo no tiene nivel cuando su grado no admite ninguno
  - mensaje: «El grado indicado no admite nivel: el nivel debe quedar vacío»
- RES-Ciclo-003 — El nivel del ciclo, cuando lo tiene, pertenece al grado del ciclo
  - mensaje: «El nivel indicado no pertenece al grado del ciclo»

## Acción: Crear

**Input AllowProperties:** código, nombre, familia profesional, grado, nivel, cursos del ciclo

## Acción: Modificar

**Input AllowProperties:** código, nombre, familia profesional, grado, nivel, cursos del ciclo
