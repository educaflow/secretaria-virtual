---
name: sdd-create-user-story
description: Crea una nueva iniciativa SDD — genera la carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_{nombre-kebab-case}/` y dentro un `user-story.md` con frontmatter `type: user-story` (plantilla a rellenar) y un `design-guidelines.md` con frontmatter `type: design-guidelines` (esqueleto mínimo, opcional de rellenar). Es el primer paso del pipeline SDD; el output sirve de input a `/sdd-analyst-system`.
---

# sdd-create-user-story

Eres el paso de **arranque** del pipeline SDD. Tu única tarea es crear la carpeta de una iniciativa nueva en `.sdd/drafts/` con dos ficheros plantillados:

- `user-story.md` con frontmatter `type: user-story` — esqueleto detallado a rellenar.
- `design-guidelines.md` con frontmatter `type: design-guidelines` — esqueleto mínimo, **opcional**: solo se rellena si esta iniciativa concreta tiene desviaciones del default de los skills `k-*` (k-sistemas, k-vistas, k-seguridad…). Si no hay ninguna excepción, el usuario puede borrar el fichero.

**NO rellenas tú ninguno de los dos ficheros** — solo dejas los esqueletos para que el usuario los complete después.

## Argumentos aceptados

```
/sdd-create-user-story [nombre]
```

- `nombre` (opcional): nombre corto descriptivo de la iniciativa, en cualquier formato (palabras sueltas con espacios, kebab-case, etc.). Si se omite, se pide al usuario con `AskUserQuestion`.

## Qué hacer

### 1. Obtener el nombre

- Si el usuario pasó nombre como argumento, úsalo.
- Si no, pregunta con `AskUserQuestion`: "¿Qué nombre corto le pones a esta iniciativa? (3–6 palabras descriptivas, por ejemplo `firma documentos`, `gestión de cargos`)". Acepta texto libre.

### 2. Normalizar el nombre a kebab-case

- A minúsculas.
- Quita acentos (`á`→`a`, `ñ`→`n`, etc.).
- Espacios y caracteres no alfanuméricos → `-`.
- Colapsa guiones consecutivos a uno solo y recorta guiones de los extremos.
- Si el resultado queda vacío, pide otro nombre.

Ejemplos:
- `Firma de documentos` → `firma-de-documentos`
- `Gestión de cargos` → `gestion-de-cargos`
- `subsistema correos` → `subsistema-correos`

### 3. Generar el timestamp

Ejecuta:
```bash
date +%Y-%m-%d_%H-%M
```

Obtienes algo como `2026-05-11_18-45`. Ese es el prefijo de la carpeta.

### 4. Verificar que la carpeta no exista

Calcula la ruta destino: `.sdd/drafts/{timestamp}_{nombre-kebab}/`.

Si esa carpeta ya existe (caso muy raro — colisión de timestamp), espera 1 minuto y vuelve a generar el timestamp, o pide al usuario que reintente.

### 5. Crear la carpeta y los ficheros plantillados

Crea la carpeta `.sdd/drafts/{timestamp}_{nombre-kebab}/` y dentro **dos ficheros**:

#### 5.1. `user-story.md`

Con **exactamente** este contenido (sustituyendo `{TÍTULO DESCRIPTIVO}` por una capitalización legible del nombre dado por el usuario — por ejemplo de `firma-de-documentos` → `Firma de documentos`):

```markdown
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
>
> De estos pasos derivará el analista los **criterios de aceptación** del análisis
> funcional. Cuanto más claros estén, menos preguntas hará después.

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
```

#### 5.2. `design-guidelines.md`

En la misma carpeta, crea un fichero `design-guidelines.md` con **exactamente** este contenido (literal — no lo adaptes al nombre de la iniciativa, no añadas secciones, no rellenes ejemplos concretos):

```markdown
---
type: design-guidelines
---

<!--
Guías de diseño específicas de esta iniciativa.

Este fichero es OPCIONAL. Solo escribe aquí desviaciones del default que marcan
los skills `k-*` (k-sistemas, k-vistas, k-seguridad, k-validaciones, …). Las
convenciones normales (estructura de paquetes, ModelService, vistas en
`src/main/java/.../views/`, patrón de controladores, etc.) ya las conocen esos
skills y NO hay que repetirlas aquí.

Si esta iniciativa NO tiene ninguna excepción respecto al default, borra este
fichero entero — su ausencia es perfectamente válida.

Ejemplos de cosas que SÍ van aquí:
- "Esta iniciativa NO usa ModelService porque [razón concreta]."
- "Las vistas de este subsistema viven en un paquete distinto al estándar porque [razón]."
- "La validación X se aplica en cliente en vez de servidor porque [razón]."
- "Se reutiliza la entidad Y de otro subsistema en vez de crear una nueva porque [razón]."

Formato libre: lista con viñetas, prosa corta, o lo que mejor exprese la guía.
Cada punto debería incluir el QUÉ se desvía y el PORQUÉ.
-->
```

### 6. Mensaje final al usuario

Tras crear el fichero, indica al usuario:

```
Iniciativa creada: .sdd/drafts/{timestamp}_{nombre-kebab}/
Ficheros generados:
  - user-story.md          (obligatorio rellenar)
  - design-guidelines.md   (opcional — borrar si esta iniciativa no se desvía del default)

Próximos pasos:
  1. Abre el `user-story.md` y rellena las secciones marcadas con [corchetes].
  2. Si esta iniciativa tiene desviaciones del default de los skills `k-*`,
     anótalas en `design-guidelines.md`. Si no, borra el fichero.
  3. Cuando esté listo, lanza `/sdd-analyst-system` para producir el análisis funcional.
```

Sustituye `{timestamp}_{nombre-kebab}` por los valores reales.

## Cuándo parar y pedir ayuda

- Si la carpeta destino ya existe y un reintento de timestamp tampoco soluciona la colisión.
- Si el nombre normalizado queda vacío tras la limpieza (todos los caracteres eran no alfanuméricos).
- Si no se puede escribir en `.sdd/drafts/` (problema de permisos).

## Qué NO hacer

- **No rellenes la historia de usuario tú mismo.** La plantilla es para el usuario, no para ti. Si el usuario te dio una descripción más larga al invocar, igualmente deja la plantilla vacía y dile al usuario que la rellene él.
- **No rellenes el `design-guidelines.md` tú mismo.** Tampoco infieras desviaciones de los skills `k-*` a partir del nombre o la descripción. Deja el comentario plantillado tal cual; el usuario decidirá si lo rellena o lo borra.
- **No leas otros `user-story.md` ni otros `design-guidelines.md`** ni otros artefactos SDD como inspiración. Cada iniciativa empieza desde cero con las plantillas literales.
- **No invoques `/sdd-analyst-system`** automáticamente al terminar. El usuario decide cuándo lanzarlo.
- **No modifiques las plantillas** según el nombre dado. Las plantillas son siempre las mismas — las secciones son genéricas y se aplican a cualquier iniciativa.
