---
type: implementation-task
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el repositorio personalizado `NotaRepository`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/db/repo/NotaRepository.java` | Crear | k-sistemas (modelos.md) | Contador de matrículas de honor por módulo |

### Descripción del diseño (Paso 3 — Repositorios)

Repositorios personalizados en `system/gruposnotas/db/repo/` (las consultas JPA viven aquí, nunca inline en el servicio — k-sistemas). Heredan de la clase abstracta generada por Axelor (las entidades deben declarar `repository="abstract"`).

```java
// com.educaflow.system.gruposnotas.db.repo.NotaRepository extends AbstractNotaRepository
public long countMatriculasHonorByModuloGrupo(ModuloGrupo moduloGrupo);
//   Método propio del repositorio (NO finder-method: un finder devuelve la primera entidad o un Query,
//   nunca un long). Cuenta las Notas con valor MATRICULA_HONOR del módulo (moduloGrupo) dado: filtra
//   por el módulo y por el enum ValorNota.MATRICULA_HONOR (se compara contra el enum, no contra el
//   literal), usando filtro con :param y bind, y termina la consulta del repositorio en .count().
//   Filtro JPQL con :param y bind (k-secure-coding §5). Lo usa V-Nota-003 (VAL-017).
```

`ModuloGrupo` no tiene repositorio personalizado; sus inserciones las hace `GrupoServiceImpl` a través del `Repository<ModuloGrupo>` genérico.
