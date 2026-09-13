# Modelo: Grado

**Modelo existente:** sí

El grado clasifica una enseñanza en el sistema educativo: hoy el catálogo tiene «Ciclo formativo» y «Curso de especialización».
Lo que cambia es que el grado deja de ser una etiqueta suelta y pasa a ser quien decide si un ciclo lleva nivel: como los niveles cuelgan de un grado, el grado sabe si admite nivel con solo mirar si tiene alguno.
Ese dato no se guarda ni se rellena a mano: el sistema lo deriva del catálogo de niveles cada vez que lee el grado, de modo que basta con crear o quitar niveles para que los ciclos de ese grado empiecen o dejen de pedirlo.

## Campos

*(el delta no añade ni cambia ningún campo guardado del grado)*

## Campos calculados

- CC-Grado-001 — admite nivel
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: cierto si el grado tiene al menos un nivel en el catálogo de niveles; falso si no tiene ninguno

## Acción: Crear

**Input AllowProperties:** código, nombre

## Acción: Modificar

**Input AllowProperties:** código, nombre
