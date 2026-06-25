---
type: implementation-task
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el repositorio personalizado de `Nota`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/db/repo/NotaRepository.java` | Crear | k-sistemas (modelos.md) | Repository de Nota (contar MH por módulo del grupo) |

Del diseño, Paso 3 — Repositorios. Repositorio personalizado (entidad con `repository="abstract"`). El finder se declara como `<finder-method>` en el dominio (ya materializado); el repositorio concreto hereda de `AbstractNotaRepository`:

```java
// com.educaflow.system.gruposnotas.db.repo.NotaRepository extends AbstractNotaRepository { }
//   Hereda countMatriculasHonorByModuloGrupo (lista; el servicio cuenta el tamaño). Sin métodos extra.
```

**Verificar:** la entidad con repositorio personalizado lleva `repository="abstract"` (ya en el dominio).
