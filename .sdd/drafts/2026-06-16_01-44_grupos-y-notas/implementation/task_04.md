---
type: implementation-task
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Materializa el dominio `Nota`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/Nota.xml` | Crear | k-sistemas (modelos.md) | Entidad Nota + enum ValorNota |

El XML completo y ya validado con `xmllint` está en `design/domains/Nota.xml`. **MUST** copiarlo **literalmente** a `src/main/java/com/educaflow/system/gruposnotas/domains/Nota.xml`, **sin regenerarlo** ni reescribirlo (ver `implementation.md` §1). Es la fuente de verdad del diseño.

### Descripción del diseño (Paso 1 — Dominios)

- **`Nota.xml`** — Entidad `Nota`. Campos: `moduloGrupo` (many-to-one → ModuloGrupo, required), `alumnoGrupo` (many-to-one → AlumnoGrupo, required), `valor` (enum `ValorNota`, required), `fechaCalificacion` (datetime), `fechaUltimaModificacion` (datetime). `unique-constraint(moduloGrupo,alumnoGrupo)` (RES-006). El contador de matrículas de honor por módulo (V-Nota-003) **NO** es un `finder-method` (un finder de Axelor devuelve la primera entidad o un `Query`, nunca un `long`): se implementa como método propio de `NotaRepository` con `all().filter(...).bind(...).count()` (ver Paso 3). Enum `ValorNota { NO_EVALUADO, NOTA_01..NOTA_10, MATRICULA_HONOR }`.

> `Nota` lleva **ya** `repository="abstract"` en su `<entity>` (tiene repo personalizado).

**Representación del valor de la nota.** `<enum name="ValorNota">` con items `NO_EVALUADO`, `NOTA_01`..`NOTA_10` (títulos "1".."10") y `MATRICULA_HONOR`. Los items numéricos llevan prefijo `NOTA_` porque los nombres de item de enum deben ser identificadores Java válidos (no pueden empezar por dígito).

**Verificar:** `validate.sh` imprime `VALIDACION-XML: OK`.
