---
type: implementation-task
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Esta tarea materializa el dominio `SmokeTest`. **El XML ya está materializado** en `design/domains/SmokeTest.xml`: **MUST** copiarlo **literalmente** (verbatim) a su ubicación final `src/main/java/com/educaflow/subsystem/smoketest/domains/SmokeTest.xml`. **MUST NOT** regenerarlo, reescribirlo ni modificarlo.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/domains/SmokeTest.xml` | Crear | k-sistemas (modelos.md) | Entidad `SmokeTest` (texto + 2 fechas servidor) |

> Raíz de los ficheros del subsistema: `src/main/java/com/educaflow/subsystem/smoketest/`. Las clases Java generadas a partir de `domains/SmokeTest.xml` (entidad `SmokeTest`, `SmokeTestRepository`) las produce el build en `db/` — no se escriben a mano.

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto, ver Paso 7). El código Java (servicio, impl, controlador) es lo único que se implementa a partir de las firmas y comentarios de este diseño. El contenido de `data-init` (Paso 8) está dado verbatim en este `design.md`.

### Paso 1 — Dominio `SmokeTest`

Fichero materializado: `design/domains/SmokeTest.xml` → copiar a `subsystem/smoketest/domains/SmokeTest.xml`.

Resumen estructural:
- `module name="smoketest" package="com.educaflow.subsystem.smoketest.db"`.
- `entity SmokeTest` con tres campos:
  - `texto` (`string`, `namecolumn="true"`) — **origen cliente**. RES-001 (texto obligatorio) **NO** se declara con `required="true"`: se valida en servidor (V-SmokeTest-001) para emitir el mensaje exacto y garantizar el rechazo del servidor (ver §Notas).
  - `fechaCreacion` (`datetime`) — **origen servidor** (CC-001). Sin `required` (campo rellenado por el sistema, ver `k-validaciones/modelos.md`).
  - `fechaUltimaModificacion` (`datetime`) — **origen servidor** (CC-002). Sin `required`.
- Sin relaciones (modelo independiente, sin centro/usuario/expediente — Fuera de alcance del spec).

Verificar: el build genera `com.educaflow.subsystem.smoketest.db.SmokeTest` y `SmokeTestRepository` sin errores (`./run.sh` compila).

### Paso 3 — Repositorios

No aplica: `SmokeTest` no tiene queries propias ni finders; usa el repositorio generado por Axelor. **MUST NOT** poner `repository="abstract"` en el dominio.

### Clasificación de campos (cliente/servidor)

| Campo | Origen | Respaldo |
|-------|--------|----------|
| `texto` | cliente | En whitelist `insert`/`update`; validado por V-SmokeTest-001. |
| `fechaCreacion` | servidor | CC-001 → R-SmokeTest-001 (Antes, alta). Fuera de whitelists. |
| `fechaUltimaModificacion` | servidor | CC-002 → R-SmokeTest-002 (Antes, alta y modificación). Fuera de whitelists. |
