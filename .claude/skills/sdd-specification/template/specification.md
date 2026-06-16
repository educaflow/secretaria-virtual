---
type: specification
---

# Objetivo

<Una frase con lo que tiene que hacer; si es un **sistema** o un **subsistema**; dependencias funcionales de subsistemas existentes.>

# Actores

- **<Actor>**: <quién es y qué papel juega>

# Historias de usuario

<!-- Una historia por cada `## HU-NNN`; debajo de cada una, sus escenarios `ESC-NNN`. Cada escenario va SIEMPRE como una lista de pasos numerados (un paso por línea), con ramas condicionales si hace falta — nunca como varias frases en una sola línea. -->

## HU-001 — Como <Actor> quiero <feature> para <motivo>

- ESC-001 — <Nombre corto>:
  1. <El actor inicia sesión.>
  2. <Prepara los datos que necesita la prueba.>
  3. <Realiza la acción que se prueba.>
  4. <El sistema responde.>
- ESC-002 — <Nombre corto>:
  1. <El actor inicia sesión.>
  2. <Prepara los datos que necesita la prueba.>
  3. <Realiza la acción que se prueba.>
  4. Si <condición>: <el sistema hace esto>.
  5. Si no: <el sistema hace esto otro y no hace aquello>.

## HU-002 — Como <Actor> quiero <feature> para <motivo>

- ESC-003 — <Nombre corto>:
  1. <El actor inicia sesión.>
  2. <Prepara los datos que necesita la prueba.>
  3. <Realiza la acción que se prueba.>
  4. <El sistema responde.>

# Modelos

<!-- Tabla índice: una fila por modelo. Cada modelo se describe a fondo en su propio fichero entity-<Nombre>.md (descripción, campos, estados, RES, CC y, por evento, VAL y RN). -->

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-<Nombre>.md](./entity-<Nombre>.md) | <Nombre> | <una línea: qué es> |

<Relaciones entre los modelos, en lenguaje de negocio: padre/hijo, borrado en cascada, referencias opcionales a entidades externas.>

# Pantallas

<!-- Tabla índice: una fila por pantalla. Cada pantalla se describe a fondo en su propio fichero screen-<slug>.md (identidad, menú, paneles, botones y RUI). -->

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-<slug>.md](./screen-<slug>.md) | <Nombre> | <una línea: qué muestra y a quién> |

# Seguridad

- **<Rol>:** <qué puede ver/crear/editar/borrar, indicando su alcance por centro: solo su centro / todos los centros / solo sus propios registros>
- **<Rol>:** <…>

# Recursos y datos iniciales

<Recursos estáticos (plantillas PDF, XSD, certificados…) y datos precargados al arrancar. Si no hay: *(no aplica)*>

# Fuera de alcance

- <Cosa que el negocio decide no hacer>
