---
type: implementation-task
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el repositorio personalizado `AlumnoGrupoRepository`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/db/repo/AlumnoGrupoRepository.java` | Crear | k-sistemas (modelos.md) | Finders de pertenencia |

### Descripción del diseño (Paso 3 — Repositorios)

Repositorios personalizados en `system/gruposnotas/db/repo/` (las consultas JPA viven aquí, nunca inline en el servicio — k-sistemas). Heredan de la clase abstracta generada por Axelor (las entidades deben declarar `repository="abstract"`).

```java
// com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository extends AbstractAlumnoGrupoRepository
public boolean existsOtroGrupoMismoCursoAcademico(User alumno, Centro centro, Integer cursoAcademico, Long excludeAlumnoGrupoId);
//   Cuenta los AlumnoGrupo del alumno cuyos grupos tienen el mismo centro+cursoAcademico, excluyendo el
//   registro con id = excludeAlumnoGrupoId cuando se pasa (si excludeAlumnoGrupoId es null, no excluye
//   ninguno: ese es el caso del alta, donde la pertenencia aún no tiene id). Filtro JPQL con :param y bind
//   (k-secure-coding §5). Lo usa V-AlumnoGrupo-004 (RES-004), que en el alta lo invoca con null.
```

> El dominio `AlumnoGrupo` declara además `finder-method findByGrupo` (generado por Axelor en la base abstracta); este repositorio lo hereda.
