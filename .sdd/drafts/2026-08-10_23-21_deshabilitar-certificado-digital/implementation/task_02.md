---
type: implementation-task
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- (ninguno — la tabla del diseño no asigna skill a este fichero: es un script SQL de migración Flyway, sin código Java ni XML de Axelor)

> **Decisión documentada (descomponedor):** la columna `Skill` de la fila de la tabla es `—`, por lo que esta tarea no lista skills. No aplica `k-secure-coding`/`k-code-quality` porque no hay código Java que toque entidades, servicios ni controladores: el fichero es SQL puro ejecutado por Flyway.

## Fichero de esta tarea (de la tabla "Ficheros a crear o modificar" del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/com/educaflow/secretariavirtual/startup/database/V2__backfill_certificado_digital_enabled.sql` | Crear | — | Migración Flyway de backfill: pone a `TRUE` la columna `enabled` de las filas preexistentes (que quedan a NULL al añadir la columna) — ver Paso 1 |

La única fila `Crear` es el script de migración de datos (backfill), que no existe en el árbol real; por lo demás la iniciativa solo modifica el subsistema existente `subsystem/criptografia` (mínima intrusión: se amplían el dominio, el servicio y la vista existentes; no se crea ninguna pieza paralela).

## Texto del diseño (verbatim — parte de migración del Paso 1)

**Hechos verificados sobre el mecanismo** (condicionan el resto del diseño):

1. El `default="true"` del dominio **solo** inicializa el atributo Java de las entidades **nuevas**; **NO** genera DDL que rellene las filas **preexistentes** en BD: tras añadir la columna, esas filas quedan con `enabled` a **NULL**.
2. El getter que genera AOP para un `<boolean>` con `default` **colapsa NULL a `Boolean.FALSE`** (patrón `enabled == null ? Boolean.FALSE : enabled`): a nivel de entidad no existe un tercer estado observable.

Combinados, sin más medidas las filas legacy (NULL) se **leerían como deshabilitadas**, rompiendo VAL-CertificadoDigital-001 («NULL cuenta como habilitada») y el «Fuera de alcance» del spec (los certificados ya configurados deben seguir funcionando). Por eso este paso incluye una **migración de datos**:

**Migración de datos (backfill) — a implementar por `/sdd-implementer`** (este diseño solo la documenta; no toca `src/**`):

- Crear el script Flyway `V2__backfill_certificado_digital_enabled.sql` en `src/main/resources/com/educaflow/secretariavirtual/startup/database/` (la ubicación classpath que ya tiene configurada `DataBaseStartup.executeMigrate`; `V2` porque `baselineOnMigrate=true` deja la baseline en `V1` — ajustar el número si al implementar ya existiera una migración con esa versión), con:

  ```sql
  UPDATE criptografia_certificado_digital SET enabled = TRUE WHERE enabled IS NULL;
  ```

- Se ejecuta automáticamente en el arranque: `AppEventObserver.onAppStart` (observador de `StartupEvent`, que Axelor dispara **después** de actualizar el esquema — la columna ya existe) llama a `DataBaseStartup.startup()`, que lanza `flyway.migrate()`. Es idempotente (Flyway la aplica una sola vez y el `WHERE enabled IS NULL` la hace inocua si se re-ejecutara).
- Tras el backfill **ninguna fila queda a NULL**, y la condición del Paso 2 («`enabled` a FALSE vía getter ⇒ tratar como inexistente») es correcta también para las filas legacy.

**Verificación:** tras el primer arranque `SELECT count(*) FROM criptografia_certificado_digital WHERE enabled IS NULL` devuelve 0.

### Notas y supuestos que aplican (verbatim del diseño)

3. **Semántica de NULL: el mecanismo real es getter + backfill.** Hecho verificado (Paso 1): el getter generado por AOP colapsa `enabled` a NULL en `Boolean.FALSE`, así que a nivel de entidad **no existe** un tercer estado observable — un NULL persistido se **leería como deshabilitada**, no como habilitada. La semántica normativa «NULL/no indicado cuenta como habilitada» (RN-CertificadoDigital-001 y «Fuera de alcance» del spec) se garantiza por dos vías: para las entidades **nuevas**, el `default="true"` del dominio inicializa el atributo a `TRUE`; para las filas **preexistentes**, el backfill del Paso 1 pone a `TRUE` la columna donde estaba NULL. Tras el backfill ninguna fila queda a NULL, y la condición del Paso 2 («`enabled` a FALSE vía getter ⇒ tratar como inexistente») es correcta. Caso residual: un cliente REST que enviara **explícitamente** `"enabled": null` persistiría NULL y esa entrada se leería como deshabilitada; ninguna interfaz del spec lo produce (la casilla del form siempre envía true/false) y RN-CertificadoDigital-001 cubre el campo **ausente** (default), no el null explícito — se acepta como comportamiento fuera de las interfaces contempladas.
