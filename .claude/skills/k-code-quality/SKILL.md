---
name: k-code-quality
description: Reglas de calidad técnica para el código Java/Kotlin y XML (domains, views) del proyecto — la máxima (legible para el humano y modificable por un LLM, por eso la repetición pequeña gana a la abstracción), métodos, clases, idiomas Java modernos, convenciones específicas del stack (Axelor, Guice, JPA), formato de código (una unidad sintáctica por línea), cuándo se comenta y cuándo no, cómo se escriben los tests (sin copiar los valores del código que prueban), y los olores de DISEÑO que distinguen un diseño senior de una chapuza (decisiones con varios dueños, reglas que se autocondicionan, retornos defensivos, conocimiento tácito). Referenciado por developer-code-reviewer para guiar auditorías y correcciones, y por sdd-designer (diseñador, juez, enriquecedor y críticos) como rasero de calidad del diseño.
---

# k-code-quality

Este skill documenta las reglas de calidad del código Java/Kotlin y XML del proyecto, de sus tests y del diseño antes de que haya código. No es un skill activo — es una referencia que cargan otros skills como `developer-code-reviewer` y `sdd-designer`.

## Objetivos, por orden de prioridad

Cuando dos reglas de este skill entren en conflicto, desempata por este orden:

1. **Mantenibilidad** — el código se puede cambiar sin romper partes no relacionadas.
2. **Testabilidad** — la lógica de negocio se puede probar sin montar la E/S ni la interfaz de usuario.
3. **Determinismo** — con las mismas entradas produce el mismo resultado, sin depender de estado global.
4. **Separación de responsabilidades** — dominio, infraestructura y presentación claramente separados.

## La máxima: legible para el humano, modificable por el LLM

**CRITICAL**: el código se escribe para que **un humano lo entienda al leerlo** y para que **un LLM lo pueda modificar sin equivocarse**. Las dos cosas a la vez, y en ese orden.

Consecuencias, que desarrollan los ficheros de abajo:

- Un humano se olvida de que hay dos sitios que tocar; un LLM no. Por eso **la repetición pequeña y visible es aceptable** y la indirección que la evita (un genérico, un método «para no repetir dos líneas») no lo es.
- Antes de abstraer, **dimensiona por el dominio real**: cuántos casos puede haber de verdad, no cuántos podría haber en teoría. Con pocos casos (**LIMIT**: hasta ~4) gana un método lineal por caso.
- Lo que un lector no puede deducir leyendo (una invariante, una decisión) va en el **modelo** o en el **`CLAUDE.md` de la carpeta**, nunca en un comentario que repita el código.

## Ficheros

| Fichero | Contenido |
|---------|-----------|
| `metodos.md` | Descomposición (el método público cuenta la historia entera; trocear por cosa, no por fase; pocos casos → un método lineal por caso), ¿hace falta el método? (buscar primero en el dueño del concepto; CPD no justifica un método), cuándo **no** extraer un método (envoltorios que solo reenvían, idiomas del proyecto, genéricos para ahorrar dos líneas), responsabilidad única, cálculo puro y efectos secundarios, nombrado, tamaño, complejidad ciclomática y CRAP (el build falla por encima de `crapUmbral`), operaciones sobre colecciones y la guarda de cortesía (leer el dato una vez, `return` temprano, sin log) |
| `clases.md` | SOLID, composición frente a herencia, clases colaboradoras, coherencia interfaz/implementación, DTOs y records (solo lo homogéneo; nombrar por naturaleza, no por consumidor), utilidades estáticas y por qué los privados de una clase inyectable no se marcan `static` |
| `java-idioms.md` | Optional, `Objects.requireNonNull`/`TextUtil.requireNonBlank`, `TextUtil.isNullOrBlank` en las guardas, records, y los idiomas modernos obligatorios (pattern matching, switch expressions, var, colecciones inmutables) |
| `proyecto.md` | Convenciones Axelor: formato de código (una unidad sintáctica por línea, en XML y en Java/Kotlin), controladores, fronteras entre subsistemas, capa de servicio (JPQL en el repositorio), DI/Guice, las invariantes del modelo se declaran en el `domains.xml` (`orderBy`…), zona horaria (siempre `Convert.defaultZoneId`) y Error Prone |
| `comentarios.md` | Cuándo se comenta y cuándo no: el código se explica solo, el único comentario que se escribe (el *por qué* que leer el código no revela) y los separadores de bloque, que sí se mantienen |
| `tests.md` | Cómo se escribe un test: no copia los valores del código que prueba (constantes, medidas, umbrales), los toma de producción y comprueba relaciones; qué valores sí van literales |
| `disenyo.md` | Olores de **diseño** (antes de que haya código): la prueba del segundo desarrollador, una decisión con varios dueños, dónde va un método (cuatro preguntas: ¿existe?, ¿es del dueño del concepto?, ¿es decisión del llamador?, ¿segundo llamador?), reglas que deciden solas si aplican, retornos defensivos que delegan (y en qué se distingue de la guarda de cortesía), la validación del servicio usada como control de flujo, piezas que se conocen entre sí, ramas no complementarias, defensa solo en la vista, patrones inventados sin declarar, complejidad heredada por incorporar ventajas |

## Cómo usarlo

Este skill se pasa como argumento de conocimiento a `developer-code-reviewer`:

```
/developer-code-reviewer <ruta-del-código> k-code-quality
```

`developer-code-reviewer` carga este skill y aplica las reglas de los siete ficheros como criterio de revisión y corrección.

`sdd-designer` lo usa como rasero de diseño (lo ordena el `README.md` de su plantilla); qué carga cada rol:

- **Diseñador**: el skill entero, y `disenyo.md` para elegir entre alternativas antes de escribir.
- **Juez**: `disenyo.md`, `clases.md` y `metodos.md`, como rasero de calidad entre diseños que cubren la spec.
- **Enriquecedor**: **solo `disenyo.md`**, para descartar ventajas que añadan un olor.
- **Crítico** de la lente de calidad de clases: el skill entero; reporta cada olor y cada violación de SOLID como crítica.
- **Verificador**: **no** lo carga; solo comprueba cumplimiento.