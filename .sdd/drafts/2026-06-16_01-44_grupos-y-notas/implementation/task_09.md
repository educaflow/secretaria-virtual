---
type: implementation-task
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el repositorio personalizado `GrupoRepository`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/db/repo/GrupoRepository.java` | Crear | k-sistemas (modelos.md) | Finder duplicado de nombre |

### Descripción del diseño (Paso 3 — Repositorios)

Repositorios personalizados en `system/gruposnotas/db/repo/` (las consultas JPA viven aquí, nunca inline en el servicio — k-sistemas). Heredan de la clase abstracta generada por Axelor (las entidades deben declarar `repository="abstract"`).

```java
// com.educaflow.system.gruposnotas.db.repo.GrupoRepository extends AbstractGrupoRepository
public Grupo findByNombreCentroCursoAcademico(String nombre, Centro centro, Integer cursoAcademico);
//   Generado por el finder-method del dominio. Devuelve el grupo homónimo en el centro+cursoAcademico o null.
//   Lo usan V-Grupo-003 (alta) y V-Grupo-005 (modificación, que además excluye el propio id).
```

Filtros JPQL con `:param` y bind (k-secure-coding §5).
