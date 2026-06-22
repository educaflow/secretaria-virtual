---
type: implementation-task
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Materializa el dominio `AlumnoGrupo`, incluido el campo calculado `notaMedia` (CC-001).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/AlumnoGrupo.xml` | Crear | k-sistemas (modelos.md) | Entidad AlumnoGrupo + campo calculado `notaMedia` (CC-001, propiedad transient con cuerpo CDATA) |

El XML completo y ya validado con `xmllint` está en `design/domains/AlumnoGrupo.xml`. **MUST** copiarlo **literalmente** a `src/main/java/com/educaflow/system/gruposnotas/domains/AlumnoGrupo.xml`, **sin regenerarlo** ni reescribirlo (incluido el cuerpo CDATA de `notaMedia`, que es la única fuente de verdad del algoritmo). Ver `implementation.md` §1.

### Descripción del diseño (Paso 1 — Dominios)

- **`AlumnoGrupo.xml`** — Entidad `AlumnoGrupo`. Campos: `grupo` (many-to-one → Grupo, required), `alumno` (many-to-one → `com.axelor.auth.db.User`, required), `notas` (one-to-many → Nota, mappedBy="alumnoGrupo"), `centro` (many-to-one → Centro, **transient**, auxiliar UI para filtrar el selector de alumno). **CC-001 (nota media; momento lectura)**: se declara como **propiedad del dominio** — un `<string name="notaMedia" transient="true">` con **el algoritmo completo inline en el cuerpo CDATA del propio campo** (patrón de `Persona.nombreApellidos` / `Centro.administradores`, que computan inline sin utilidades externas). La entidad de dominio es un POJO y **NO** puede depender de `..service..` (C13 `entidadesDominioSonPojos`), por lo que el cálculo **no** se extrae a ninguna clase de `service.impl`: vive en el CDATA y solo referencia `Nota` y `ValorNota`, del mismo paquete `..db..`. Al ser una propiedad declarada, el `<field name="notaMedia"/>` de grids/forms la resuelve; `transient="true"` hace que no se persista y se calcule en memoria al leer, sin onLoad ni llamadas de servicio por fila. Axelor genera el getter desde el cuerpo CDATA: **MUST NOT** añadir además un getter manual del mismo nombre (no compilaría) ni dejarlo como getter suelto en `extra-code-model` (no quedaría registrado como propiedad). `unique-constraint(grupo,alumno)` (RES-005). `finder-method findByGrupo`.

> `AlumnoGrupo` lleva **ya** `repository="abstract"` en su `<entity>` (tiene repo personalizado).

**CC-001 (nota media), momento lectura.** El cuerpo devuelve `String` (para poder devolver "Sin nota" o el entero) **calculando** la media con el algoritmo completo inline en el propio CDATA (única fuente de verdad), referenciando solo `Nota` y `ValorNota` del paquete `..db..`. El cálculo: excluye `NO_EVALUADO`, `MATRICULA_HONOR`=10, media redondeada al entero más cercano (`Math.round`), "Sin nota" si no hay ninguna evaluada (trata `notas == null` como «sin notas evaluadas», sin NPE).

**Composición / borrado en cascada** (modelado ya en el XML del diseño):
- `AlumnoGrupo.notas` → `cascade="all" orphanRemoval="true"`: AlumnoGrupo es el **padre propietario** de la Nota; al quitar un alumno del grupo se borran sus Notas.

**Verificar:** `validate.sh` imprime `VALIDACION-XML: OK`.
