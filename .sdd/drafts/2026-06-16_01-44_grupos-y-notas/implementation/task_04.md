---
type: implementation-task
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Materializa el dominio `Nota` (entidad + enum `ValorNota`).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/Nota.xml` | Crear | k-sistemas (modelos.md) | Entidad Nota + enum ValorNota |

Este fichero **ya está materializado** en `design/domains/Nota.xml`. **MUST** copiarlo **literalmente** (verbatim) a su ubicación destino `src/main/java/com/educaflow/system/gruposnotas/domains/Nota.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Resumen estructural (del diseño, Paso 1):

- **`domains/Nota.xml`** — entidad `Nota` (`repository="abstract"`). Campos: `moduloGrupo` (m2o ModuloGrupo, `required`, **servidor**), `alumnoGrupo` (m2o AlumnoGrupo, `required`, **servidor**), `valor` (enum `ValorNota`, **cliente** en Modificar, inicial NO_EVALUADO, SIN `required`), `fechaCalificacion` (datetime, **servidor**, CC-002), `fechaUltimaModificacion` (datetime, **servidor**, CC-003). `unique-constraint(moduloGrupo,alumnoGrupo)` (RES-006). `finder-method countMatriculasHonorByModuloGrupo` (VAL-017). Enum `ValorNota {NO_EVALUADO, NOTA_1..NOTA_10, MATRICULA_HONOR}`.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh` → `VALIDACION-XML: OK`.

Notas y supuestos relevantes (del diseño):

3. **`valor` de Nota** se modela como un **enum `ValorNota`** con items `NO_EVALUADO`, `NOTA_1`..`NOTA_10`, `MATRICULA_HONOR`. Justificación: representa exactamente el dominio del spec (No evaluado / 1..10 / Matrícula de Honor) con un único campo, hace VAL-016 trivialmente cierto desde la UI (el selector solo ofrece valores válidos) pero mantiene VAL-016 como validación servidor (defensa ante un valor crudo por `/ws/rest`), y permite VAL-017 (contar `valor = 'MATRICULA_HONOR'`) y CC-001 (mapear `MATRICULA_HONOR`→10, `NOTA_n`→n, excluir `NO_EVALUADO`). La nota inicial es `NO_EVALUADO` (RN-005).
5. **CC-002 (`fechaCalificacion`) y CC-003 (`fechaUltimaModificacion`)** son `datetime` servidor, asignadas en `NotaServiceImpl.update`.
