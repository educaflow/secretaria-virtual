# Modelo: ModuloGrupo

Cada uno de los módulos que imparte un grupo. No los crea ni los elige el usuario: se generan automáticamente al crear el grupo, a partir de los módulos del curso al que pertenece. Es el contenedor de las notas de ese módulo (una por cada alumno del grupo).

## Campos

- **grupo** — el grupo al que pertenece este módulo
- **módulo** — el módulo del catálogo educativo que se imparte
- **notas del módulo** — la nota de cada alumno del grupo en este módulo

## Restricciones

- RES-003 — Un mismo módulo no aparece dos veces en el mismo grupo

## Acción: Crear

**Input AllowProperties:** (ninguna — los módulos del grupo los crea el sistema a partir del curso, la interfaz no los envía)

## Acción: Modificar

**Input AllowProperties:** (ninguna — los módulos del grupo no se editan)
