# Modelo: Nivel

**Modelo existente:** sí

El nivel precisa dentro de un grado de qué tipo de enseñanza se trata: básico, medio o superior.
Lo que cambia es que deja de ser un catálogo independiente y pasa a colgar de un grado: cada nivel dice a qué grado pertenece, y un grado que no tenga ninguno es un grado cuyos ciclos no llevan nivel.

## Campos

- **grado** — el grado al que pertenece el nivel; es lo que permite ofrecer en un ciclo solo los niveles que le corresponden a su grado

## Restricciones

- RES-Nivel-001 — El nivel tiene indicado el grado al que pertenece
  - mensaje: «El grado es obligatorio»

## Acción: Crear

**Input AllowProperties:** código, nombre, grado

## Acción: Modificar

**Input AllowProperties:** código, nombre, grado
