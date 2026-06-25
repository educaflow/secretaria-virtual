---
type: implementation-task
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Materializa el dominio `ModuloGrupo`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/ModuloGrupo.xml` | Crear | k-sistemas (modelos.md) | Entidad ModuloGrupo |

Este fichero **ya está materializado** en `design/domains/ModuloGrupo.xml`. **MUST** copiarlo **literalmente** (verbatim) a su ubicación destino `src/main/java/com/educaflow/system/gruposnotas/domains/ModuloGrupo.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Resumen estructural (del diseño, Paso 1):

- **`domains/ModuloGrupo.xml`** — entidad `ModuloGrupo` (`repository="abstract"`). Campos: `grupo` (m2o Grupo, `required`, **servidor**), `modulo` (m2o Modulo, `required`, **servidor**), `notas` (o2m Nota, `orphanRemoval`). `unique-constraint(grupo,modulo)` (RES-003).

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh` → `VALIDACION-XML: OK`.

Notas y supuestos relevantes (del diseño):

9. **`orphanRemoval`** en `ModuloGrupo.notas` materializa la composición del spec.
