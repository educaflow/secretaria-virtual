---
type: implementation-task
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Materializa el dominio `Grupo`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/Grupo.xml` | Crear | k-sistemas (modelos.md) | Entidad Grupo + enum EstadoGrupo |

El XML completo y ya validado con `xmllint` está en `design/domains/Grupo.xml`. **MUST** copiarlo **literalmente** a `src/main/java/com/educaflow/system/gruposnotas/domains/Grupo.xml`, **sin regenerarlo** ni reescribirlo (ver `implementation.md` §1). Es la fuente de verdad del diseño.

### Descripción del diseño (Paso 1 — Dominios)

Se crean cuatro ficheros de dominio en `system/gruposnotas/domains/` (módulo `gruposnotas`, paquete `com.educaflow.system.gruposnotas.db`).

- **`Grupo.xml`** — Entidad `Grupo`. Campos: `nombre` (string, namecolumn, required), `curso` (many-to-one → `com.educaflow.subsystem.sistemaeducativo.db.Curso`, required), `cursoAcademico` (integer, required), `centro` (many-to-one → `com.educaflow.subsystem.common.db.Centro`, required), `estado` (enum `EstadoGrupo`, required), `fechaCierre` (datetime), `modulosGrupo` (one-to-many → ModuloGrupo, mappedBy="grupo"), `alumnosGrupo` (one-to-many → AlumnoGrupo, mappedBy="grupo"). `unique-constraint(nombre,centro,cursoAcademico)` (RES-001). `finder-method findByNombreCentroCursoAcademico`. Enum `EstadoGrupo { ABIERTO, CERRADO }`.

> **Repositorios personalizados**: como se crean `GrupoRepository`, `AlumnoGrupoRepository` y `NotaRepository` a mano en `db/repo/`, las entidades `Grupo`, `AlumnoGrupo` y `Nota` llevan **ya** `repository="abstract"` en su `<entity>` dentro de los `domains/*.xml` del diseño.

**Composición / borrado en cascada** (modelado ya en el XML del diseño):
- `Grupo.modulosGrupo` y `Grupo.alumnosGrupo` → `cascade="all" orphanRemoval="true"`: al borrar el Grupo se borran sus ModuloGrupo y AlumnoGrupo.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh <design>` imprime `VALIDACION-XML: OK`.
