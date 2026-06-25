---
type: implementation-task
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Materializa el dominio `Grupo` (entidad + enum `EstadoGrupo`).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/Grupo.xml` | Crear | k-sistemas (modelos.md) | Entidad Grupo + enum EstadoGrupo |

Este fichero **ya está materializado** en `design/domains/Grupo.xml`. **MUST** copiarlo **literalmente** (verbatim) a su ubicación destino `src/main/java/com/educaflow/system/gruposnotas/domains/Grupo.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Resumen estructural (del diseño, Paso 1):

- **`domains/Grupo.xml`** — entidad `Grupo` (`repository="abstract"`). Campos: `nombre` (string, `required`, `namecolumn`, **cliente**), `curso` (m2o Curso, `required`, **cliente**, inmutable), `centro` (m2o Centro, **cliente para admin / servidor para supervisor**, inmutable, SIN `required`), `cursoAcademico` (integer, **cliente para admin / servidor para supervisor**, inmutable, SIN `required`), `estado` (enum `EstadoGrupo`, **servidor**, SIN `required`), `fechaCierre` (datetime, **servidor**, SIN `required`), `modulosGrupo` (o2m ModuloGrupo, `orphanRemoval`), `alumnosGrupo` (o2m AlumnoGrupo, `orphanRemoval`). `unique-constraint(nombre,centro,cursoAcademico)` (RES-001). `finder-method findByNombreAndCentroAndCursoAcademico` (VAL-003/005). Enum `EstadoGrupo {ABIERTO, CERRADO}`. El ciclo del grupo se muestra en grids con el path `curso.ciclo` (no se persiste; RES-002 derivación).

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh` → `VALIDACION-XML: OK`.

Notas y supuestos relevantes (del diseño):

1. **`cursoAcademico` es un `integer`** (año de inicio, p.ej. 2024), coherente con `Centro.curso` (entero, "Curso académico").
2. **`estado`** es un enum `EstadoGrupo {ABIERTO, CERRADO}`; **`fechaCierre`** es `datetime` servidor. Ningún estado es terminal.
9. **`orphanRemoval`** en las colecciones (`Grupo.modulosGrupo`, `Grupo.alumnosGrupo`) materializa la composición del spec: al borrar el grupo se borran módulos, alumnos y notas (ESC-005).
