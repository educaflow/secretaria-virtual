---
type: implementation-task
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

El fichero de dominio ya está materializado en la carpeta `design/`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo: **cópialo verbatim** desde `design/domains/SmokeTest.xml` a su ruta destino.

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/domains/SmokeTest.xml` | Crear | k-sistemas (modelos.md) | Entidad SmokeTest |

Ruta destino completa: `src/main/java/com/educaflow/subsystem/smoketest/domains/SmokeTest.xml`

---

## Paso 1 — Dominio: entidad SmokeTest

Crear `src/main/java/com/educaflow/subsystem/smoketest/domains/SmokeTest.xml`.

El fichero materializado está en `design/domains/SmokeTest.xml`. **Resumen estructural:**

- Módulo `smoketest`, paquete `com.educaflow.subsystem.smoketest.db`.
- Entidad `SmokeTest` con tres campos:
  - `texto` (`<string large="true">`) — campo **cliente**. `large="true"` mapea la columna a tipo `TEXT` en la BD (sin límite de longitud). Sin `required="true"` en el dominio: la validación con el mensaje exacto del spec («El texto es obligatorio») vive en `validateInsert`/`validateUpdate` del servicio (V-SmokeTest-001). Esto evita que Bean Validation genere un mensaje genérico antes de que el servicio pueda producir el mensaje esperado por ESC-005.
  - `fechaCreacion` (`<datetime>`) — campo **servidor** (CC-001). Sin `required="true"` porque lo asigna el servidor en `insert` vía `fireActionRule_AsignarFechaCreacion`.
  - `fechaUltimaModificacion` (`<datetime>`) — campo **servidor** (CC-002). Sin `required="true"` porque lo asigna el servidor en `insert` y `update` vía `fireActionRule_ActualizarFechaUltimaModificacion`.
- Sin relaciones, sin enumerados, sin finders personalizados.

**Clasificación de campos:**

| Campo | Origen | AllowProperties insert | AllowProperties update |
|-------|--------|------------------------|------------------------|
| `texto` | cliente | sí | sí |
| `fechaCreacion` | servidor | **NO** — asignado incondicionalmente en `fireActionRule_AsignarFechaCreacion` | **NO** — inmutable tras la creación; restaurado desde `original` en `update` |
| `fechaUltimaModificacion` | servidor | **NO** — asignado incondicionalmente en `fireActionRule_ActualizarFechaUltimaModificacion` | **NO** — recalculado incondicionalmente en `fireActionRule_ActualizarFechaUltimaModificacion` |

**Verificar:** `grep -r "SmokeTest" src/main/java/com/educaflow/subsystem/smoketest/domains/`
