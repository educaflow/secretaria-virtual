---
name: k-code-quality
description: Reglas de calidad técnica para código Java/Kotlin del proyecto — métodos, clases, idiomas Java modernos y convenciones específicas del stack (Axelor, Guice, JPA). Referenciado por code-reviewer para guiar auditorías y correcciones.
---

# k-code-quality

Este skill documenta las reglas de calidad que aplican al código Java/Kotlin implementado. No es un skill activo — es una referencia que cargan otros skills como `code-reviewer`.

## Ficheros

| Fichero | Contenido |
|---------|-----------|
| `metodos.md` | Descomposición, responsabilidad única, nombrado, tamaño y operaciones sobre colecciones |
| `clases.md` | SOLID, clases colaboradoras, coherencia interfaz/implementación, DTOs y utilidades estáticas |
| `java-idioms.md` | Optional, streams, records, pattern matching, var y colecciones inmutables |
| `proyecto.md` | Convenciones Axelor: controladores, capa de servicio, repositorios, DI/Guice y diseños SDD |

## Cómo usarlo

Este skill se pasa como argumento de conocimiento a `code-reviewer`:

```
/code-reviewer <ruta-del-código-o-diseño> k-code-quality
```

`code-reviewer` carga este skill y aplica las reglas de los cuatro ficheros como criterio de revisión y corrección.