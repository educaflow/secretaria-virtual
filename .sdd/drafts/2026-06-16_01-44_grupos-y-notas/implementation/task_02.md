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

El XML completo y ya validado con `xmllint` está en `design/domains/ModuloGrupo.xml`. **MUST** copiarlo **literalmente** a `src/main/java/com/educaflow/system/gruposnotas/domains/ModuloGrupo.xml`, **sin regenerarlo** ni reescribirlo (ver `implementation.md` §1). Es la fuente de verdad del diseño.

### Descripción del diseño (Paso 1 — Dominios)

- **`ModuloGrupo.xml`** — Entidad `ModuloGrupo`. Campos: `grupo` (many-to-one → Grupo, required), `modulo` (many-to-one → `com.educaflow.subsystem.sistemaeducativo.db.Modulo`, required), `notas` (one-to-many → Nota, mappedBy="moduloGrupo"). `unique-constraint(grupo,modulo)` (RES-003).

> `ModuloGrupo` no tiene repo propio (sus finders/contadores viven en otras entidades), así que **NO** lleva `repository="abstract"`.

**Composición / borrado en cascada** (modelado ya en el XML del diseño):
- `ModuloGrupo.notas` → **navegación inversa SOLO** (sin cascade/orphanRemoval): la Nota tiene un único padre propietario (AlumnoGrupo) para no declarar dos padres con `orphanRemoval` sobre la misma Nota. Un ModuloGrupo solo se elimina al borrar su Grupo, y en ese caso sus Notas ya se borran por la cadena Grupo→AlumnoGrupo→Nota.

**Verificar:** `validate.sh` imprime `VALIDACION-XML: OK`.
