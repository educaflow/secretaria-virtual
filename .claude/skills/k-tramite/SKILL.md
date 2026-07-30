---
name: k-tramite
description: Cómo dar de alta un trámite nuevo en la secretaría virtual: la carpeta `tramites/<tramite>/`, el fichero maestro `TramiteInstance.xml` (code, name, tipoTramite, publico/privado, defaultTipoExpediente, help), los data-init que genera el build, la i18n del nombre y los permisos necesarios para poder crear expedientes del trámite. Las versiones del trámite (los tipos de expediente `v1`, `v2`…) son de `k-tipo-expediente`.
---

# k-tramite

Un trámite es lo que el usuario ve y elige en el árbol "Crear un nuevo expediente". Este skill cubre solo el alta y mantenimiento del trámite; la implementación de cada versión (carpetas `v1/`, `v2/`…) la cubre `k-tipo-expediente`.

---

## 1. Conceptos clave

- **Trámite** = el "producto" administrativo (p.ej. "Justificación de falta del profesorado"). Se define con un único fichero maestro `TramiteInstance.xml`.
- **Tipo de expediente** = una **versión** concreta de la implementación del trámite. Un trámite puede tener N versiones (carpetas `v1/`, `v2/`…) pero solo **una activa**: la que declara `<defaultTipoExpediente>`. El árbol de trámites crea siempre expedientes de la versión activa.
- Los trámites viven **fuera de `system/`** (no son sistemas Controller→Service→Repository), en `src/main/java/com/educaflow/tramites/`.

## 2. Estructura de carpetas

```
src/main/java/com/educaflow/tramites/<nombre_tramite>/   ← snake_case
├── TramiteInstance.xml          ← fichero maestro (lo escribes tú)
├── i18n_es.csv / i18n_ca.csv    ← i18n del nombre del trámite (los genera el build, MUST NOT crearlos a mano)
├── v1/                          ← primera versión (tipo de expediente) → k-tipo-expediente
└── v2/                          ← versiones siguientes → k-tipo-expediente (versionado.md)
```

## 3. `TramiteInstance.xml`

Plantilla (ejemplo real: `tramites/justificacion_falta_profesorado/TramiteInstance.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Tramite>
    <code>JustificacionFaltaProfesorado</code>
    <name>Justificación de falta del profesorado</name>
    <tipoTramite>PROFESOR</tipoTramite>
    <defaultTipoExpediente>v1</defaultTipoExpediente>
    <help><![CDATA[
        Este trámite permite justificar la falta del profesorado.<br>
        Admite <strong>HTML</strong>.
    ]]></help>
</Tramite>
```

| Tag | Obligatorio | Contenido |
|---|---|---|
| `code` | **MUST** | Identificador único del trámite. **MUST** ser UpperCamel y un identificador Java válido **sin** guiones ni underscores: es el prefijo del nombre de la entidad de cada versión (`<code>V1`) y del patrón de nombres de las vistas. |
| `name` | **MUST** | Nombre visible (se traduce vía i18n, ver §5). Es también el título por defecto de los documentos PDF de sus versiones. |
| `tipoTramite` | **MUST** | `code` de la entidad `TipoTramite` (agrupa el árbol por tipo de usuario). Los valores vigentes se leen del data-init `subsystem/expedientes/data-init/input/TipoTramites.xml` (hoy: PROFESOR, ALUMNO, TUTOR, DIRECCION, ADMINISTRATIVO, CONSERJE — la fuente de verdad es ese fichero, no esta lista). |
| `publico` / `privado` | Opcionales | Flags booleanos del `Tramite` (ver `subsystem/expedientes/domains/Tramite.xml`). Solo se emiten al data-init si se declaran. |
| `defaultTipoExpediente` | Opcional | La **versión activa**: el nombre de la carpeta del tipo (`v1`, `v2`…). También admite el `code` completo de un tipo. Sin este tag no se genera la asignación de tipo activo y no se pueden crear expedientes del trámite. |
| `help` | **MUST** | Ayuda que se muestra en el árbol "Crear un nuevo expediente", en CDATA, admite HTML. |

- ✅ CORRECTO: `<code>RenunciaConvocatoriaAlumno</code>`
- ❌ INCORRECTO: `<code>renuncia_convocatoria</code>` (los underscores rompen el patrón de vistas `exp-<Code>-Templates` y el nombre de la entidad)
- ❌ INCORRECTO: `<defaultTipoExpediente>JustificacionFaltaProfesorado</defaultTipoExpediente>` apuntando a un code que no existe en ninguna carpeta de versión (el data-init no casará y el trámite queda sin tipo activo)

## 4. Qué genera el build (y qué queda en BD)

La tarea gradle `generateDataInitTramites` busca cada `TramiteInstance.xml` bajo `src/main/java` y genera en `build/resources/main/tramites/<Code>/` (**nunca en `src`**):

1. `definicion/data-init/` (`priority="1"`) — crea/actualiza la fila `Tramite` en BD (bind por `code`, se refresca en cada arranque) con `name`, `tipoTramite` (resuelto por `code` contra la tabla `TipoTramite`), `help` y `publico`/`privado` **solo si están declarados**.
2. `tipo_expediente_activo/data-init/` (`priority="-1"`, `update` sin `create`) — solo si hay `<defaultTipoExpediente>`; resuelve `v1` → code del tipo (el `<code>` declarado en su `TipoExpedienteInstance.xml` o, por defecto, `code del trámite + V1`).

El orden de carga lo gobierna la `priority`: primero el trámite (`1`), luego los `TipoExpediente` (`0`) y por último la asignación del activo (`-1`), que ya encuentra ambos en BD.

## 5. i18n del nombre

- El `name` es texto traducible: el build genera y mantiene `i18n_es.csv`/`i18n_ca.csv` **en la raíz de la carpeta del trámite**, con traducción automática castellano→valenciano.
- **MUST NOT** crear esos CSV a mano (regla de `CLAUDE.md`); las palabras que no deban traducirse llevan sufijo `__!!`.
- Una traducción automática mala se corrige editando **solo** la columna `message` del `i18n_ca.csv`; esa corrección se conserva.

## 6. Permisos

- Para que un usuario pueda **crear** expedientes del trámite necesita una asignación del perfil `CREADOR` **por `tramiteCode`** (ejemplo real: `src/main/resources/data-demo/input/permisos-demo.xml`).
- Prefiere asignar por `tramiteCode` y no por `tipoExpedienteCode` cuando el permiso sea conceptualmente del trámite: la asignación por trámite **sobrevive a las versiones**; la asignación por tipo hay que duplicarla en cada versión nueva.
- El perfil `RESPONSABLE` de cada estado se asigna según convenga (por cargo, tipo de usuario o usuario concreto) — leer el modelo real en `subsystem/security/`.

## 7. Checklist de alta de un trámite

1. Crea `src/main/java/com/educaflow/tramites/<nombre_tramite>/` (snake_case).
2. Escribe `TramiteInstance.xml` con `code`, `name`, `tipoTramite` y `help`. **Sin** `<defaultTipoExpediente>` todavía.
3. Compila (`./gradlew clean build`): se genera el data-init y los CSV de i18n; al arrancar, el trámite aparece en el árbol.
4. Añade los permisos de `CREADOR` por `tramiteCode` (§6).
5. Crea la primera versión en la carpeta `v1/` siguiendo `/k-tipo-expediente`.
6. Activa la versión: `<defaultTipoExpediente>v1</defaultTipoExpediente>` y recompila.

## 8. Anti-patrones

- **MUST NOT** crear los `i18n_*.csv` a mano ni añadir/quitar filas (solo editar la columna `message` de una traducción mala).
- **MUST NOT** escribir data-init del trámite a mano en `src`: el fichero maestro es `TramiteInstance.xml` y el data-init se genera en `build/`.
- **MUST NOT** usar un `code` con guiones, underscores o espacios.
- **MUST NOT** declarar `<defaultTipoExpediente>` antes de que exista la carpeta de la versión: el data-init generado no casará con ningún tipo en BD.

## Quick Guidelines

- Un trámite = una carpeta `tramites/<nombre_tramite>/` + un `TramiteInstance.xml`; sus versiones (`v1/`, `v2/`…) son tipos de expediente → `/k-tipo-expediente`.
- `code` en UpperCamel sin `-`/`_`: es el prefijo de la entidad de cada versión.
- `tipoTramite` = code de la entidad `TipoTramite` (fuente de verdad: su data-init en `subsystem/expedientes`).
- `<defaultTipoExpediente>` = nombre de la carpeta de la versión activa (`v1`); sin él no se pueden crear expedientes del trámite.
- Los data-init del trámite y los CSV de i18n los genera el build; **MUST NOT** escribirlos a mano.
- Permisos de creación por `tramiteCode` (mejor que por `tipoExpedienteCode`: sobreviven a las versiones).
