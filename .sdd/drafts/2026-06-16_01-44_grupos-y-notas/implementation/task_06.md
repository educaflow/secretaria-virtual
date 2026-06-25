---
type: implementation-task
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el repositorio personalizado de `AlumnoGrupo`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/db/repo/AlumnoGrupoRepository.java` | Crear | k-sistemas (modelos.md) | Repository de AlumnoGrupo (finder por alumno + curso académico) |

Del diseño, Paso 3 — Repositorios. Repositorio personalizado (entidad con `repository="abstract"`). El finder se declara como `<finder-method>` en el dominio (ya materializado); el repositorio concreto hereda de `AbstractAlumnoGrupoRepository`:

```java
// com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository extends AbstractAlumnoGrupoRepository { }
//   Hereda findByAlumnoAndGrupoCursoAcademico (lista). Sin métodos extra.
```

**Verificar:** la entidad con repositorio personalizado lleva `repository="abstract"` (ya en el dominio).
