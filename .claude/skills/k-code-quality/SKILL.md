---
name: k-code-quality
description: Reglas de calidad técnica para código Java/Kotlin del proyecto — métodos, clases, idiomas Java modernos, convenciones específicas del stack (Axelor, Guice, JPA) y los olores de DISEÑO que distinguen un diseño senior de una chapuza (decisiones con varios dueños, reglas que se autocondicionan, retornos defensivos, conocimiento tácito). Referenciado por developer-code-reviewer para guiar auditorías y correcciones, y por sdd-designer (diseñador, juez, enriquecedor y verificador) como rasero de calidad del diseño.
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
| `metodos.md` | Descomposición, responsabilidad única, cálculo puro y efectos secundarios, nombrado, tamaño y operaciones sobre colecciones |
| `clases.md` | SOLID, composición frente a herencia, clases colaboradoras, coherencia interfaz/implementación, DTOs y utilidades estáticas |
| `java-idioms.md` | Optional, streams, records, pattern matching, switch expressions, var y colecciones inmutables |
| `proyecto.md` | Convenciones Axelor: controladores, fronteras entre subsistemas, capa de servicio (JPQL en el repositorio) y DI/Guice |
| `disenyo.md` | Olores de **diseño** (antes de que haya código): la prueba del segundo desarrollador, una decisión con varios dueños, reglas que deciden solas si aplican, retornos defensivos que delegan, piezas que se conocen entre sí, ramas no complementarias, defensa solo en la vista, patrones inventados sin declarar, complejidad heredada por incorporar ventajas |

## Cómo usarlo

Este skill se pasa como argumento de conocimiento a `developer-code-reviewer`:

```
/developer-code-reviewer <ruta-del-código> k-code-quality
```

`developer-code-reviewer` carga este skill y aplica las reglas de los cinco ficheros como criterio de revisión y corrección.

`sdd-designer` lo usa como rasero de diseño: el diseñador carga el skill entero (lo ordena el `README.md` de su plantilla) y además `disenyo.md` para elegir entre alternativas antes de escribir; juez, enriquecedor y verificador cargan **solo `disenyo.md`**: el juez como rasero de calidad entre diseños que cubren la spec, el enriquecedor para descartar ventajas que añadan un olor, y el verificador para reportar cada olor como problema.