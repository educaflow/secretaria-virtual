---
type: specification
template: <nombre de la carpeta de la plantilla activa sin el prefijo `template-`, o el valor reservado `external` si esa carpeta es externa; lo fija el skill>
---

# Objetivo

<Una frase con lo que tiene que hacer la feature y para quién.>

# Contexto y alcance

**Por qué es una feature libre:** <una o dos frases: toca varias partes de la aplicación / es transversal / es de la base común / no es un sistema ni un tipo de expediente.>

**Partes de la aplicación afectadas** (en lenguaje de negocio, solo lo que cambia):

- **<Parte 1>**: <qué cambia en ella>
- **<Parte 2>**: <qué cambia en ella>
- **<Parte que NO cambia aunque lo parezca>**: no cambia.

**Dependencias funcionales:** <de qué funcionalidades existentes depende la feature, o *(ninguna)*.>

# Actores

- **<Actor>**: <quién es y qué papel juega>

<!-- Si la feature afecta a todos los usuarios por igual, dilo así en una línea en vez de enumerarlos, y declara aparte quién la administra. -->

# Historias de usuario

<!-- Una historia por cada `## HU-NNN`; debajo de cada una, sus escenarios `ESC-NNN`. Cada escenario va SIEMPRE como una lista de pasos numerados (un paso por línea), con usuarios y centros de los datos de demo. Un escenario no automatizable lleva ` *(manual: <motivo>)*` tras su nombre corto. -->

## HU-001 — Como <Actor> quiero <feature> para <motivo>

- ESC-001 — <Nombre corto>:
  1. <El actor inicia sesión.>
  2. <Prepara los datos que necesita la prueba.>
  3. <Realiza la acción que se prueba.>
  4. <El sistema responde.>
- ESC-002 — <Nombre corto> *(manual: <motivo>)*:
  1. <El actor inicia sesión.>
  2. <…>

## HU-002 — Como <Actor> quiero <feature> para <motivo>

- ESC-003 — <Nombre corto de no-regresión>:
  1. <El actor inicia sesión.>
  2. <Usa la parte existente que la feature roza, como antes.>
  3. <El sistema sigue respondiendo como antes.>

# Requisitos

<!-- Un REQ-NNN por viñeta: una frase afirmativa y comprobable. Sub-viñetas opcionales: etiqueta, condición, mensaje, actor. Incluye SIEMPRE los de datos existentes y los de fallo. Si hay demasiados, resume aquí y enlaza un anexo. -->

- REQ-001 — <Qué debe cumplirse.>
  - etiqueta: <comportamiento | seguridad | rendimiento | textos | compatibilidad | datos existentes | …>
- REQ-002 — <Qué debe cumplirse cuando algo falla.>
  - mensaje: «<texto literal que ve el usuario>»
- REQ-003 — <Qué pasa con lo que ya existe en la aplicación al desplegar la feature.>
  - etiqueta: datos existentes

# Seguridad

- **<Rol>:** <qué puede hacer, indicando su alcance por centro: solo su centro / todos los centros / solo sus propios registros>
- **<Rol>:** <…>

# Recursos y datos iniciales

<Recursos estáticos y datos precargados al arrancar. Si no hay: *(no aplica)*>

# Fuera de alcance

- <Cosa que el negocio decide no hacer>
- <Parte de la aplicación que NO se toca aunque pudiera parecer relacionada>
