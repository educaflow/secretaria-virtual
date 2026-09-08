---
type: implementation-task
template: system
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- Ninguno aplica: es un script SQL de migración Flyway, no código Java/Kotlin ni XML de Axelor. Referencia documental del proyecto: `agent_docs/deploy.md`.

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/com/educaflow/secretariavirtual/startup/database/V2__certificado_digital_dni_no_unico.sql` | Crear | — (Flyway, ver `agent_docs/deploy.md`) | Migración defensiva que elimina la restricción UNIQUE sobre la columna `dni` de la tabla del certificado digital (Hibernate `ddl=update` no la retira solo). **El número de versión MUST ser > 1** (ver Paso 1) |

## Texto del diseño (verbatim)

### Paso 1 — Migración de esquema: retirar la unicidad del DNI

**Fichero:** `src/main/resources/com/educaflow/secretariavirtual/startup/database/V2__certificado_digital_dni_no_unico.sql` (**Crear**).

Es el único recurso estático de la iniciativa. `DataBaseStartup.executeMigrate()` ejecuta Flyway en cada arranque sobre `classpath:com/educaflow/secretariavirtual/startup/database` (hoy la carpeta está vacía), así que este es el sitio del proyecto para una corrección de esquema que Hibernate no hace por su cuenta.

**CRITICAL — el número de versión MUST ser mayor que 1 (por eso `V2__`, no `V1__`).** `Flyway.configure()…locations("classpath:com/educaflow/secretariavirtual/startup/database").baselineOnMigrate(true)` se configura **sin** `baselineVersion`, así que en la primera ejecución sobre una base de datos **ya poblada** (la tabla existe porque la creó Hibernate) Flyway crea el baseline en la **versión 1** y marca como «ya aplicadas», sin ejecutarlas, todas las migraciones con versión ≤ 1. Un script llamado `V1__…` no correría **nunca** justo en el escenario para el que existe: una instalación con datos donde la restricción UNIQUE sigue viva. Con `V2__` el script sí se aplica sobre el baseline.

Qué debe hacer el script:

- Localizar, en el esquema actual, **cualquier** restricción de tipo `UNIQUE` definida exactamente sobre la columna `dni` de la tabla de la entidad `CertificadoDigital` (`criptografia_certificado_digital`, según la convención `<módulo>_<entidad_en_snake_case>` de Axelor) y eliminarla con `ALTER TABLE … DROP CONSTRAINT …`.
- Buscar la restricción por **catálogo** (`pg_constraint`/`information_schema`), no por un nombre literal: el nombre lo generó Hibernate y no es estable entre entornos.
- Ser **idempotente y defensivo**: en una base de datos nueva la tabla todavía no existe cuando Flyway corre, así que el script **MUST NOT** fallar si no encuentra ni la tabla ni la restricción (bloque `DO $$ … $$` con las comprobaciones de existencia).

Motivo: al quitar `unique="true"` del modelo, Hibernate con `ddl = update` deja de declarar la restricción pero **no la borra** de una base de datos ya creada; sin esta migración, RES-CertificadoDigital-002 no se cumpliría en entornos existentes (el alta del segundo certificado del mismo DNI reventaría con un error de integridad en vez de guardarse).

**Verificar:** tras arrancar, `psql` → `\d criptografia_certificado_digital` no muestra ninguna restricción única sobre `dni`; y `SELECT dni, count(*) FROM criptografia_certificado_digital GROUP BY dni` admite valores repetidos.

### Notas y supuestos que aplican a esta tarea (verbatim)

5. **Migración de esquema.** Quitar `unique="true"` no basta en una base de datos ya creada: Hibernate con `ddl = update` nunca borra restricciones. De ahí el paso 1 con Flyway, que el proyecto ya tiene cableado en `DataBaseStartup` y cuya carpeta de migraciones está hoy vacía. **El script se llama `V2__certificado_digital_dni_no_unico.sql`, no `V1__`**: `DataBaseStartup.executeMigrate` configura `baselineOnMigrate(true)` **sin** `baselineVersion`, así que sobre una base de datos ya poblada Flyway hace baseline en la versión 1 y da por aplicadas —sin ejecutarlas— todas las migraciones ≤ 1; un `V1__` no correría nunca justo donde hace falta. Cualquier migración futura de este proyecto MUST seguir numerando por encima de 1 mientras esa configuración no cambie. El script se describe (no se escribe) porque el diseño no materializa código; debe buscar la restricción por catálogo y no fallar en una base de datos nueva donde la tabla aún no existe. Alternativa descartada: resetear la base de datos en cada entorno (`agent_docs/deploy.md`), porque no serviría en un entorno con datos reales.

6. **Nombre de la tabla.** Se asume `criptografia_certificado_digital`, por la convención de Axelor `<módulo>_<entidad en snake_case>` con `<module name="criptografia">`. No se ha podido confirmar contra una base de datos viva (el esquema local está vacío), y por eso el script del paso 1 debe localizar la restricción por catálogo y tolerar que la tabla no exista.

14. **Ficheros fuera de `design/` que la implementación debe tocar.** Este diseño no escribe en el árbol del proyecto: todo lo que hay que crear o modificar está en la tabla «Ficheros a crear o modificar» y lo materializa `/sdd-implementer`. En particular, el `V2__certificado_digital_dni_no_unico.sql` **no** existe todavía en `src/main/resources/...` y la carpeta de migraciones sigue vacía a día de hoy.
