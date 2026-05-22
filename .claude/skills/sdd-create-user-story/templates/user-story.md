---
type: user-story
---

# {TÍTULO DESCRIPTIVO}

<!--
Plantilla de historia de usuario para el pipeline SDD.

Rellena cada sección con texto en lenguaje natural y vocabulario del usuario.
NO uses formato técnico (nombres de clases Java, tablas de validaciones formales,
XML…) — todo eso lo derivará después `/sdd-specification-system` y los skills
posteriores del pipeline.

Si una sección no aplica a tu iniciativa, déjala vacía o bórrala.
Si tienes dudas sobre algún detalle, déjalo abierto — se resolverán durante
`/sdd-specification-system` con preguntas iterativas.

Cuando termines, lanza `/sdd-specification-system` para producir la
especificación funcional.
-->

[Párrafo introductorio de 2–4 líneas: en qué consiste esta iniciativa, qué problema
resuelve, en qué contexto de la secretaría virtual encaja. Evita detalles de
implementación.]

## En una frase

**Como** [rol / actor principal],
**quiero** [resultado o capacidad que busca],
**para** [beneficio / objetivo mayor].

> Esta frase resume la intención de la iniciativa. Si te cuesta escribirla porque
> mezclas varios objetivos, probablemente conviene partir la iniciativa en dos
> historias separadas.

## Quién interviene

- **[Actor 1]**: [quién es, qué papel cumple, qué necesita o aporta]
- **[Actor 2]**: [...]
- **[Actor 3, opcional]**: [...]

> Los actores pueden ser humanos (firmante, administrador, profesor, alumno, familiar…)
> o no humanos (otro sistema que invoca, un proceso programado, un servicio externo…).
> Indica los que aparezcan; no obligues a tener uno de cada.
>
> Para cada actor, deja claro **qué puede ver y qué puede hacer** (ej. "solo ve sus
> propias solicitudes", "puede crear pero no borrar"). Esto guía la parte de seguridad
> del análisis.

## Conceptos y datos clave

- **[Concepto 1]**: [qué es en una línea, qué información lleva consigo]
- **[Concepto 2]**: [...]
- **[Estado, si aplica]**: [valores posibles, p. ej. "pendiente, firmada, rechazada"]

> Aquí enumera las "cosas" que aparecen en tu iniciativa con el nombre que les das
> tú (solicitud de firma, motivo de rechazo, expediente, cargo…). No definas tipos
> de datos ni estructuras — solo el concepto, qué datos lleva y, si tiene estados,
> cuáles son. El analista usará este vocabulario para nombrar entidades sin inventar.

## Fuera de alcance (opcional)

- [Algo que parece encajar pero NO entra en esta iniciativa]
- [...]

> Si hay zonas grises (cosas adyacentes que un lector podría suponer que entran),
> lístalas aquí para evitar malentendidos en el análisis posterior. Si todo está
> claro, borra esta sección entera.

## Restricciones que no pueden romperse

- [Restricción 1: visibilidad, integridad, estados finales, propiedad de datos…]
- [Restricción 2]
- [...]

> Aquí van las reglas duras que el diseño tiene que respetar sí o sí. Ejemplos típicos:
> privacidad entre usuarios ("un usuario solo ve lo suyo"), datos que no se pueden
> modificar después de cierto punto, estados que no se pueden revertir, campos
> obligatorios, validaciones criptográficas u otras que no son negociables.



## Preguntas abiertas (opcional)

- [Duda sin resolver — En la especificación se abordará en su fase de preguntas]
- [...]

> Si tienes dudas concretas sobre cómo debe comportarse algo, anótalas aquí en
> vez de inventarte una respuesta. En la especificación se usará como punto de partida
> para preguntarte. Si no hay dudas, borra esta sección entera.
