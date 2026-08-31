---
name: k-tramite
description: Cómo dar de alta un trámite nuevo en la secretaría virtual: la carpeta `tramites/<tramite>/`, el fichero maestro `TramiteInstance.xml` (code, name, tipoTramite, publico/privado, defaultTipoExpediente, help), los data-init que genera el build, la i18n del nombre y los permisos necesarios para poder crear expedientes del trámite. Las versiones del trámite (los tipos de expediente `v1`, `v2`…) son de `k-tipo-expediente`.
---

# k-tramite

Un trámite es lo que el usuario ve y elige en el árbol "Crear un nuevo expediente". Este skill cubre solo el alta y mantenimiento del trámite; la implementación de cada versión (carpetas `v1/`, `v2/`…) la cubre `k-tipo-expediente`.

---

## 1. Conceptos clave

- **Trámite** = el "producto" administrativo (lo que el ciudadano pide: una solicitud, una autorización, una renuncia…). Se define con un único fichero maestro `TramiteInstance.xml`.
- **Tipo de expediente** = una **versión** concreta de la implementación del trámite. Un trámite puede tener N versiones (carpetas `v1/`, `v2/`…) pero solo **una activa**: la que declara `<defaultTipoExpediente>`. El árbol de trámites crea siempre expedientes de la versión activa.
- Los trámites viven **fuera de `system/`** (no son sistemas Controller→Service→Repository), en `src/main/java/com/educaflow/tramites/`.

## 2. Estructura de carpetas

```
src/main/java/com/educaflow/tramites/[<agrupacion>/…]<nombre_tramite>/   ← snake_case
├── TramiteInstance.xml          ← fichero maestro (lo escribes tú)
├── i18n_es.csv / i18n_ca.csv    ← i18n del nombre del trámite (los genera el build, MUST NOT crearlos a mano)
├── [<agrupacion>/…]v1/          ← primera versión (tipo de expediente) → k-tipo-expediente
└── [<agrupacion>/…]v2/          ← versiones siguientes → k-tipo-expediente (versionado.md)
```

Los dos `[<agrupacion>/…]` son opcionales y de profundidad libre: son **solo carpetas de agrupación**, sin significado para el generador, que busca los `TramiteInstance.xml` y las carpetas de versión **recursivamente** (§4).
  Un trámite puede colgar de una carpeta temática (`tramites/<agrupacion>/mi_tramite/`) y sus versiones pueden estar anidadas dentro del trámite (`mi_tramite/actual/v1/`, `mi_tramite/futuro/v2/`).
  La única restricción es que un trámite **MUST NOT** estar dentro de otro, y que el nombre de la carpeta de la versión activa sea **único** bajo el trámite (§4).

## 3. `TramiteInstance.xml`

Plantilla (con un trámite inventado, `MiTramite`; los ejemplos de este skill y de `/k-tipo-expediente` usan siempre ese mismo):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Tramite>
    <code>MiTramite</code>
    <name>Mi trámite</name>
    <tipoTramite>PROFESOR</tipoTramite>
    <defaultTipoExpediente>v1</defaultTipoExpediente>
    <help><![CDATA[
        Descripción de para qué sirve el trámite.<br>
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
| `defaultTipoExpediente` | Opcional | La **versión activa**. **MUST** ser el **nombre de la carpeta** del tipo (`v1`, `v2`…), y esa carpeta **MUST** existir bajo el trámite con su `TipoExpedienteInstance.xml` dentro (lo vigila el test T1, §4). El generador admite además el `code` completo del tipo, pero **MUST NOT** usarse: un code escrito a mano no se distingue de una errata hasta que revienta en runtime. Sin este tag no se genera la asignación de tipo activo y no se pueden crear expedientes del trámite. |
| `help` | Opcional (recomendado) | Ayuda que se muestra en el árbol "Crear un nuevo expediente", en CDATA, admite HTML. Si no se declara, el data-init se genera con un CDATA vacío. Su texto **MUST NOT** contener `]]>` (cerraría el CDATA): el generador lanza `RuntimeException` si lo encuentra. |

- ✅ CORRECTO: `<code>MiTramite</code>`
- ❌ INCORRECTO: `<code>mi_tramite</code>` (los underscores rompen el patrón de vistas `exp-<Code>-Templates` y el nombre de la entidad; el snake_case es para la **carpeta**, no para el `code`)
- ❌ INCORRECTO: `<defaultTipoExpediente>v2</defaultTipoExpediente>` cuando la única carpeta de versión es `v1` (nada casa en el data-init, la columna queda a `null` y el trámite revienta al abrirlo)
- ❌ INCORRECTO: `<defaultTipoExpediente>MiTramiteV1</defaultTipoExpediente>` (es el `code` del tipo, no el nombre de su carpeta: el generador lo acepta, el test T1 lo rechaza)

## 4. Qué genera el build (y qué queda en BD)

La tarea gradle `generateDataInitTramites` es un `JavaExec` que solo invoca la herramienta `createdatainittramite.Main` de `EducaFlowBuildTools` (toda la lógica está ahí, nada en el `build.gradle`). Busca cada `TramiteInstance.xml` **a cualquier profundidad bajo el paquete raíz de los trámites** (`com.educaflow.tramites`, el último argumento de la tarea; las carpetas intermedias son solo de agrupación y un trámite **MUST NOT** estar dentro de otro) y genera en `build/resources/main/tramites/<Code>/` (**nunca en `src`**).
Antes del bucle borra de una vez `build/resources/main/tramites/` **entera** (los data-init de **todos** los trámites, no solo los del trámite que se regenera), para que no sobreviva el data-init de un trámite que ya no existe en las fuentes.
Por cada trámite genera:

1. `definicion/data-init/` (`priority="1"`) — crea/actualiza la fila `Tramite` en BD (bind por `code`, se refresca en cada arranque) con `name`, `tipoTramite` (resuelto por `code` contra la tabla `TipoTramite`), `help` y `publico`/`privado` **solo si están declarados**.
2. `tipo_expediente_activo/data-init/` (`priority="-1"`, `update` sin `create`) — solo si hay `<defaultTipoExpediente>` y no está en blanco; resuelve `v1` → code del tipo (el `<code>` declarado en su `TipoExpedienteInstance.xml` o, por defecto, `code del trámite + V1`) buscando la carpeta `v1` **recursivamente** bajo la del trámite.
   - Más de una carpeta con ese nombre → falla por ambigüedad.
   - **Ninguna** carpeta con ese nombre → **CRITICAL**: no falla. El generador asume que el valor ya es un `code` y lo emite tal cual; el bind del data-init es `search` + `create="false"`, así que el import tampoco falla y deja la columna `defaultTipoExpediente` a `null`. El error aparece solo en runtime, al abrir el trámite: `No existe el tipo de expediente para el tramite con idTramite: N`.
   - Ese silencio lo tapa el test **T1** (`src/test/java/com/educaflow/tiposexpedientes/tramite/TipoExpedienteActivoTest.java`), que exige que el `<defaultTipoExpediente>` nombre **exactamente una** carpeta bajo el trámite con su `TipoExpedienteInstance.xml` dentro, y falla `./gradlew test` (y por tanto `./run.sh`) si no.
     Denuncia por igual los dos extremos: **ninguna** carpeta (el silencio del punto anterior) y **más de una** (la ambigüedad, que el generador sí caza pero solo al compilar y con un mensaje del build, no del fichero que hay que editar).
     Que el nombre de carpeta de versión sea único bajo el trámite es, por tanto, una regla verificada, no una convención.

El orden de carga lo gobierna la `priority`: primero el trámite (`1`), luego los `TipoExpediente` (`0`) y por último la asignación del activo (`-1`), que ya encuentra ambos en BD.

## 5. i18n del nombre

- El `name` es texto traducible: el build genera y mantiene `i18n_es.csv`/`i18n_ca.csv` **en la raíz de la carpeta del trámite**, con traducción automática castellano→valenciano.
- **MUST NOT** crear esos CSV a mano (regla de `CLAUDE.md`); las palabras que no deban traducirse llevan sufijo `__!!`.
- Una traducción automática mala se corrige editando **solo** la columna `message` del `i18n_ca.csv`; esa corrección se conserva.

## 6. Permisos

- Para que un usuario pueda **crear** expedientes del trámite necesita una asignación del perfil `CREADOR` **por `tramiteCode`**; las asignaciones de demo viven en `src/main/resources/data-demo/input/permisos-demo.xml`.
- Prefiere asignar por `tramiteCode` y no por `tipoExpedienteCode` cuando el permiso sea conceptualmente del trámite: la asignación por trámite **sobrevive a las versiones**; la asignación por tipo hay que duplicarla en cada versión nueva.
- El perfil `RESPONSABLE` de cada estado se asigna según convenga (por cargo, tipo de usuario o usuario concreto) — leer el modelo real en `subsystem/security/`.

## 7. Checklist de alta de un trámite

1. Crea `src/main/java/com/educaflow/tramites/<nombre_tramite>/` (snake_case), opcionalmente bajo una o varias carpetas de agrupación (`tramites/<agrupacion>/<nombre_tramite>/`), pero nunca dentro de otro trámite.
2. Escribe `TramiteInstance.xml` con `code`, `name`, `tipoTramite` y `help`. **Sin** `<defaultTipoExpediente>` todavía.
3. Compila (`./gradlew clean build`): se genera el data-init y los CSV de i18n; al arrancar, el trámite aparece en el árbol.
4. Añade los permisos de `CREADOR` por `tramiteCode` (§6).
5. Crea la primera versión en la carpeta `v1/` siguiendo `/k-tipo-expediente`.
6. Activa la versión: `<defaultTipoExpediente>v1</defaultTipoExpediente>` — el **nombre de la carpeta**, no el `code` — y recompila.

## 8. Anti-patrones

- **MUST NOT** crear los `i18n_*.csv` a mano ni añadir/quitar filas (solo editar la columna `message` de una traducción mala).
- **MUST NOT** escribir data-init del trámite a mano en `src`: el fichero maestro es `TramiteInstance.xml` y el data-init se genera en `build/`.
- **MUST NOT** usar un `code` con guiones, underscores o espacios.
- **MUST NOT** declarar `<defaultTipoExpediente>` antes de que exista la carpeta de la versión: ni el generador ni el data-init avisan, la columna queda a `null` y el trámite revienta al abrirlo (§4). El test T1 lo caza al compilar.
- **MUST NOT** poner en `<defaultTipoExpediente>` el `code` del tipo en vez del nombre de su carpeta, aunque el generador lo admita: solo el nombre de carpeta es verificable.

## Quick Guidelines

- Un trámite = una carpeta `tramites/<nombre_tramite>/` + un `TramiteInstance.xml`; sus versiones (`v1/`, `v2/`…) son tipos de expediente → `/k-tipo-expediente`.
- `code` en UpperCamel sin `-`/`_`: es el prefijo de la entidad de cada versión.
- `tipoTramite` = code de la entidad `TipoTramite` (fuente de verdad: su data-init en `subsystem/expedientes`).
- `<defaultTipoExpediente>` = nombre de la carpeta de la versión activa (`v1`), nunca el `code`; sin él no se pueden crear expedientes del trámite, y si la carpeta no existe nadie avisa hasta runtime (lo caza el test T1).
- Los data-init del trámite y los CSV de i18n los genera el build; **MUST NOT** escribirlos a mano.
- Permisos de creación por `tramiteCode` (mejor que por `tipoExpedienteCode`: sobreviven a las versiones).
