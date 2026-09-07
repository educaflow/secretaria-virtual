---
name: k-code-quality
description: Reglas de calidad técnica para código Java/Kotlin del proyecto — métodos, clases, idiomas Java modernos y convenciones específicas del stack (Axelor, Guice, JPA). Referenciado por developer-code-reviewer para guiar auditorías y correcciones.
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
| `java-idioms.md` | Optional, streams, records, pattern matching, var y colecciones inmutables |
| `proyecto.md` | Convenciones Axelor: controladores, capa de servicio, repositorios, DI/Guice y diseños SDD |

## Cómo usarlo

Este skill se pasa como argumento de conocimiento a `developer-code-reviewer`:

```
/developer-code-reviewer <ruta-del-código-o-diseño> k-code-quality
```

`developer-code-reviewer` carga este skill y aplica las reglas de los cuatro ficheros como criterio de revisión y corrección.