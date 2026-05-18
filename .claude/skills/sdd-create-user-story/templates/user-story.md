---
type: user-story
---

# {TÍTULO DESCRIPTIVO}

<!--
Plantilla de historia de usuario para el pipeline SDD.

Rellena cada sección con texto en lenguaje natural y vocabulario del usuario.
NO uses formato técnico (nombres de clases Java, tablas de validaciones formales,
XML…) — todo eso lo derivará después `/sdd-analyst-system`.

Si una sección no aplica a tu iniciativa, déjala vacía o bórrala.
Si tienes dudas sobre algún detalle, déjalo abierto — el analista te preguntará
durante `/sdd-analyst-system` para resolver lo que falte.

Cuando termines, lanza `/sdd-analyst-system` para producir el análisis funcional.
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

## Cómo se comporta y cómo se prueba

Lista de escenarios que describen cómo se comporta la iniciativa y cómo
verificarla. Cada escenario es autocontenido: una breve narrativa del recorrido
y, debajo, el criterio observable de verificación.

### Escenario 1 — [nombre corto: camino principal]

[2–4 líneas en prosa: cómo arranca este caso, qué pasos atraviesa el actor,
cómo termina. Menciona transiciones de estado relevantes si las hay
(ej. "pendiente → firmada"). No describas pantallas concretas ni botones —
solo lo que el actor consigue.]

**Verificación**: dado [contexto / estado inicial], cuando [acción del actor],
entonces [resultado observable que se puede comprobar].

### Escenario 2 — [nombre corto: rama alternativa]

[Narrativa breve del caso.]

**Verificación**: dado [contexto / estado inicial], cuando [acción del actor],
entonces [resultado observable que se puede comprobar].

### Escenario 3 — [nombre corto: error o restricción dura]

[Narrativa breve del caso de error.]

**Verificación**: dado [...], cuando [...], entonces [mensaje, bloqueo o
resultado esperado].

> Cubre el camino principal, las ramas relevantes y al menos un caso de error o
> restricción dura (de los que aparecen en "Restricciones que no pueden romperse").
> Añade tantos escenarios como necesites — la numeración es solo para identificarlos.
>
> La **verificación** debe ser observable: el resultado tiene que poder verse en
> una pantalla, en un correo, en el estado de una entidad, en un mensaje de error…
> No describas comprobaciones internas de implementación (consultas SQL, logs,
> nombres de métodos). Si no se puede observar desde fuera, no es un criterio
> de prueba útil.
>
> Estos escenarios son la base de los **criterios de aceptación** del análisis
> y la guía para validar manualmente la implementación al final del pipeline.

## Lo que aporta valor

- [Beneficio 1 para algún actor]
- [Beneficio 2]
- [...]

> Por qué merece la pena hacer esto. Útil para el análisis posterior, ayuda a
> entender qué es lo verdaderamente importante si hay que decidir entre alternativas.

## Preguntas abiertas (opcional)

- [Duda sin resolver — el analista la abordará en su fase de preguntas]
- [...]

> Si tienes dudas concretas sobre cómo debe comportarse algo, anótalas aquí en
> vez de inventarte una respuesta. El analista las usará como punto de partida
> para preguntarte. Si no hay dudas, borra esta sección entera.
