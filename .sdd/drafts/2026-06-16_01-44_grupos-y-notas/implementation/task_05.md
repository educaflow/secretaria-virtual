---
type: implementation-task
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el repositorio personalizado de `Grupo`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/db/repo/GrupoRepository.java` | Crear | k-sistemas (modelos.md) | Repository de Grupo (finder duplicado de nombre) |

Del diseño, Paso 3 — Repositorios. Repositorio personalizado (entidad con `repository="abstract"`). Los finders se declaran como `<finder-method>` en el dominio (ya materializados); el repositorio concreto hereda de `AbstractGrupoRepository`:

```java
// com.educaflow.system.gruposnotas.db.repo.GrupoRepository extends AbstractGrupoRepository { }
//   Hereda findByNombreAndCentroAndCursoAcademico (generado del finder). Sin métodos extra.
```

**Verificar:** la entidad con repositorio personalizado lleva `repository="abstract"` (ya en el dominio).
