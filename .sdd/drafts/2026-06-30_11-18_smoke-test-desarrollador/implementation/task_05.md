---
type: implementation-task
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de carga del permiso del subsistema |
| `subsystem/smoketest/data-init/input/auth-smoketest.xml` | Crear | k-datainit | Definición del permiso SmokeTest.all |
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Añadir **únicamente** la **asignación** del permiso `SmokeTest.all` al grupo `admins` (solo la referencia `<permission name="SmokeTest.all"/>`; la definición completa vive únicamente en `auth-smoketest.xml`) |

Rutas destino completas:
- `src/main/java/com/educaflow/subsystem/smoketest/data-init/input-config.xml`
- `src/main/java/com/educaflow/subsystem/smoketest/data-init/input/auth-smoketest.xml`
- `src/main/resources/data-init/input/auth.xml` (ya existe — solo añadir la asignación)

---

## Paso 5 — Seguridad: permiso SmokeTest.all

Crear los ficheros de data-init del subsistema:

- `src/main/java/com/educaflow/subsystem/smoketest/data-init/input-config.xml` — manifiesto con `priority="20"` que carga `auth-smoketest.xml`.
- `src/main/java/com/educaflow/subsystem/smoketest/data-init/input/auth-smoketest.xml` — define el permiso `SmokeTest.all` sobre `com.educaflow.subsystem.smoketest.db.SmokeTest` con `create/read/write/remove/export = true`.

Además, modificar `src/main/resources/data-init/input/auth.xml` para añadir **únicamente** la **asignación** del permiso `SmokeTest.all` al grupo `admins`. La **definición** del permiso (bloque `<permission name="SmokeTest.all" object="...">...<can .../>...</permission>`) vive exclusivamente en `subsystem/smoketest/data-init/input/auth-smoketest.xml`; incluirla también en el auth.xml global sería redundante y viola k-datainit §2 (CRITICAL).

Asignación al grupo `admins` (dentro del bloque `<group code="admins">` existente):

```xml
<permission name="SmokeTest.all"/>
```

Sin esta asignación, el grupo `admins` no tendrá acceso real a `SmokeTest` aunque el permiso quede definido en `auth-smoketest.xml`.

**Descripción del permiso en lenguaje natural:**
- `SmokeTest.all` → grupo `admins` → puede crear, leer, modificar y borrar cualquier registro de `SmokeTest`. Alcance global (sin filtro por centro).

**Verificar:** al arrancar, la tabla de permisos de Axelor tiene `SmokeTest.all` asignado al grupo `admins`.

---

## Seguridad

- **Rol con acceso:** solo el grupo `admins` (Administrador).
- **Alcance:** global. La entidad `SmokeTest` no tiene campo `centro` ni filtrado multicentro (la spec lo indica explícitamente).
- **Permisos:** `SmokeTest.all` — `create/read/write/remove/export=true` — asignado al grupo `admins`.
- **`SmokeTest.all` NO existe en auth.xml global (subsistema nuevo).** SmokeTest se crea de cero en este diseño. La **definición** del permiso `SmokeTest.all` vive ÚNICAMENTE en `subsystem/smoketest/data-init/input/auth-smoketest.xml` (k-datainit §2 CRITICAL: la definición de permisos de un subsistema NO va en el auth.xml global). El implementador DEBE añadir en `src/main/resources/data-init/input/auth.xml` ÚNICAMENTE la **asignación** (`<permission name="SmokeTest.all"/>` dentro del bloque `<group code="admins">`). Omitir la asignación dejaría el subsistema inaccesible aunque el permiso esté definido en `auth-smoketest.xml`.
