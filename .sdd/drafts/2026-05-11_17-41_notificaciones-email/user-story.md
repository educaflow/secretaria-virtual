---
type: user-story
---

# Notificaciones email

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

## Qué tiene que pasar

1. [Primer paso del camino principal — qué hace alguien, qué ve, qué decide]
2. [Segundo paso — incluye ramas si las hay: "si elige X… si elige Y…"]
3. [...]

> Numera los pasos en el orden en que ocurren. Cubre el camino principal y las
> ramas alternativas relevantes. No describas pantallas concretas ni botones
> exactos — describe lo que el usuario consigue.
>
> Si hay transiciones de estado (ej. "pendiente → firmada"), menciónalas explícitamente
> en el paso donde ocurren.

## Restricciones que no pueden romperse

- [Restricción 1: visibilidad, integridad, estados finales, propiedad de datos…]
- [Restricción 2]
- [...]

> Aquí van las reglas duras que el diseño tiene que respetar sí o sí. Ejemplos típicos:
> privacidad entre usuarios ("un usuario solo ve lo suyo"), datos que no se pueden
> modificar después de cierto punto, estados que no se pueden revertir, campos
> obligatorios, validaciones criptográficas u otras que no son negociables.

## Lo que aporta valor

- [Beneficio 1 para algún actor]
- [Beneficio 2]
- [...]

> Por qué merece la pena hacer esto. Útil para el análisis posterior, ayuda a
> entender qué es lo verdaderamente importante si hay que priorizar o decidir entre
> alternativas.
