---
type: implementation-task
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Materializa el dominio `AlumnoGrupo` (entidad + campo transient `notaMedia` con getter computado INLINE, CC-001).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/AlumnoGrupo.xml` | Crear | k-sistemas (modelos.md) | Entidad AlumnoGrupo + transient notaMedia |

Este fichero **ya está materializado** en `design/domains/AlumnoGrupo.xml`. **MUST** copiarlo **literalmente** (verbatim) a su ubicación destino `src/main/java/com/educaflow/system/gruposnotas/domains/AlumnoGrupo.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Resumen estructural (del diseño, Paso 1):

- **`domains/AlumnoGrupo.xml`** — entidad `AlumnoGrupo` (`repository="abstract"`). Campos: `grupo` (m2o Grupo, **cliente**, inmutable, SIN `required`), `alumno` (m2o User, **cliente**, inmutable, SIN `required`), `notas` (o2m Nota, `orphanRemoval`), `notaMedia` (string `transient`, **servidor**, CC-001 momento lectura: getter computado **INLINE** que recorre `this.getNotas()` referenciando solo `Nota`/`ValorNota` de `..db..` (sin `Beans.get` ni dependencia de `..service..`); devuelve "Sin nota" si no hay módulos evaluados). `unique-constraint(grupo,alumno)` (RES-005). `finder-method findByAlumnoAndGrupoCursoAcademico` (RES-004/VAL-013).

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh` → `VALIDACION-XML: OK`.

Notas y supuestos relevantes (del diseño):

4. **`notaMedia` (CC-001, momento lectura)** es un campo **`transient`** de `AlumnoGrupo` (no se persiste): un getter computado del dominio calcula la media **INLINE**, recorriendo `this.getNotas()` y referenciando **solo** entidades/enums de `..db..` (`Nota`, `ValorNota`). **NO** usa `Beans.get` ni referencia a `..service..`, de modo que la entidad POJO no depende de la capa de servicio (cumple C13 "las entidades de dominio son POJOs" y C14 "`Beans.get` prohibido"). La relación se invierte respecto al delegado clásico: `AlumnoGrupoService.calcularNotaMedia(this)` **delega en `getNotaMedia()`** del dominio (única fuente de verdad), no al revés. Cálculo: media redondeada al entero más cercano de las notas evaluadas (MH=10, `NOTA_n`→`n` vía el ordinal del item del enum, con `NO_EVALUADO` en el ordinal 0), excluyendo las `NO_EVALUADO`; si no hay ninguna evaluada devuelve la cadena **"Sin nota"**. Al ser `String`, "Sin nota" se muestra tal cual y los valores numéricos como su texto.
9. **`orphanRemoval`** en `AlumnoGrupo.notas`: al quitar un alumno se borran sus notas (ESC-005).
