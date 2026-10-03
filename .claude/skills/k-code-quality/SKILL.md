---
name: k-code-quality
description: Reglas de calidad técnica para código Java/Kotlin del proyecto — métodos, clases, idiomas Java modernos, convenciones específicas del stack (Axelor, Guice, JPA), cuándo se comenta y cuándo no, cómo se escriben los tests (sin copiar los valores del código que prueban), y los olores de DISEÑO que distinguen un diseño senior de una chapuza (decisiones con varios dueños, reglas que se autocondicionan, retornos defensivos, conocimiento tácito). Referenciado por developer-code-reviewer para guiar auditorías y correcciones, y por sdd-designer (diseñador, juez, enriquecedor y críticos) como rasero de calidad del diseño.
---

# k-code-quality

Este skill documenta las reglas de calidad que aplican al código Java/Kotlin implementado. No es un skill activo — es una referencia que cargan otros skills como `developer-code-reviewer`.

## Objetivos, por orden de prioridad

Cuando dos reglas de este skill entren en conflicto, desempata por este orden:

1. **Mantenibilidad** — el código se puede cambiar sin romper partes no relacionadas.
2. **Testabilidad** — la lógica de negocio se puede probar sin montar la E/S ni la interfaz de usuario.
3. **Determinismo** — con las mismas entradas produce el mismo resultado, sin depender de estado global.
4. **Separación de responsabilidades** — dominio, infraestructura y presentación claramente separados.

## Ficheros

| Fichero | Contenido |
|---------|-----------|
| `metodos.md` | Descomposición, cuándo **no** extraer un método (envoltorios que solo reenvían, idiomas del proyecto), responsabilidad única, cálculo puro y efectos secundarios, nombrado, tamaño, complejidad ciclomática y CRAP (el build falla por encima de `crapUmbral`) y operaciones sobre colecciones |
| `clases.md` | SOLID, composición frente a herencia, clases colaboradoras, coherencia interfaz/implementación, DTOs y utilidades estáticas |
| `java-idioms.md` | Optional, `Objects.requireNonNull`/`TextUtil.requireNonBlank`, streams, records, pattern matching, switch expressions, var y colecciones inmutables |
| `proyecto.md` | Convenciones Axelor: controladores, fronteras entre subsistemas, capa de servicio (JPQL en el repositorio), DI/Guice, zona horaria (siempre `Convert.defaultZoneId`) y Error Prone |
| `comentarios.md` | Cuándo se comenta y cuándo no: el código se explica solo, el único comentario que se escribe (el *por qué* que leer el código no revela) y los separadores de bloque, que sí se mantienen |
| `tests.md` | Cómo se escribe un test: no copia los valores del código que prueba (constantes, medidas, umbrales), los toma de producción y comprueba relaciones; qué valores sí van literales |
| `disenyo.md` | Olores de **diseño** (antes de que haya código): la prueba del segundo desarrollador, una decisión con varios dueños, reglas que deciden solas si aplican, retornos defensivos que delegan, piezas que se conocen entre sí, ramas no complementarias, defensa solo en la vista, patrones inventados sin declarar, complejidad heredada por incorporar ventajas |

## Cómo usarlo

Este skill se pasa como argumento de conocimiento a `developer-code-reviewer`:

```
/developer-code-reviewer <ruta-del-código> k-code-quality
```

`developer-code-reviewer` carga este skill y aplica las reglas de los siete ficheros como criterio de revisión y corrección.

`sdd-designer` lo usa como rasero de diseño: el diseñador carga el skill entero (lo ordena el `README.md` de su plantilla) y además `disenyo.md` para elegir entre alternativas antes de escribir; el juez carga `disenyo.md` más `clases.md` y `metodos.md` como rasero de calidad entre diseños que cubren la spec; el enriquecedor carga **solo `disenyo.md`**, para descartar ventajas que añadan un olor; y el crítico de la lente que la plantilla dedique a la calidad de clases carga el skill entero y reporta cada olor y cada violación de SOLID como crítica. El verificador ya **no** lo carga: solo comprueba cumplimiento.