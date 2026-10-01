---
name: k-tramite
description: Cómo dar de alta o modificar un trámite en la secretaría virtual: la carpeta `tramites/<tramite>/`, el fichero maestro `TramiteInstance.xml` (code, name, tipoUsuario, unidadTramitadora, permitidoPresentarEnRepresentacion, defaultTipoExpediente, help, acl), lo que genera el build a partir de él, la i18n del nombre y los permisos necesarios para poder crear expedientes del trámite. Las versiones del trámite (los tipos de expediente `v1`, `v2`…) son de `k-tipo-expediente`.
---

# k-tramite

Un trámite es lo que el usuario ve y elige en el árbol «Crear un nuevo expediente». Este skill cubre solo el trámite; la implementación de cada versión (carpetas `v1/`, `v2/`…) es de `k-tipo-expediente`.

Los ejemplos usan el trámite inventado `MiTramite` (carpeta `mi_tramite/`), el mismo que `k-tipo-expediente`.

---

## 1. Conceptos clave

- **Trámite** = lo que se pide (una solicitud, una autorización, una renuncia…). Se define con un único fichero maestro `TramiteInstance.xml`.
- **Tipo de expediente** = una **versión** de la implementación del trámite. Un trámite puede tener N versiones (`v1/`, `v2/`…) pero solo **una activa**: la de `<defaultTipoExpediente>`. Los expedientes nuevos se crean siempre de la versión activa; los ya creados siguen siendo de la suya.
- Los trámites viven en `src/main/java/com/educaflow/tramites/`, fuera de `system/` y `subsystem/`.

## 2. Estructura de carpetas

```
src/main/java/com/educaflow/tramites/[<agrupacion>/…]<nombre_tramite>/   ← snake_case
├── TramiteInstance.xml          ← fichero maestro (lo escribes tú)
├── i18n_es.csv / i18n_ca.csv    ← los genera el build; MUST NOT crearlos a mano
├── [<agrupacion>/…]v1/          ← primera versión → k-tipo-expediente
└── [<agrupacion>/…]v2/          ← versiones siguientes → k-tipo-expediente (recetas/versionado.md)
```

- Los `[<agrupacion>/…]` son opcionales, de profundidad libre y sin significado: solo agrupan carpetas (`tramites/alumnos/mi_tramite/`, `mi_tramite/actual/v1/`).
- Un trámite **MUST NOT** estar dentro de otro.
- El nombre de cada carpeta de versión **MUST** ser único bajo su trámite (§4).

## 3. `TramiteInstance.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Tramite>
    <code>MiTramite</code>
    <name>Mi trámite</name>
    <tipoUsuario>PROFESOR</tipoUsuario>
    <unidadTramitadora>JEFATURA_ESTUDIOS</unidadTramitadora>
    <defaultTipoExpediente>v1</defaultTipoExpediente>
    <permitidoPresentarEnRepresentacion>false</permitidoPresentarEnRepresentacion>
    <help><![CDATA[
        Para qué sirve el trámite y qué necesita el usuario para presentarlo.<br>
        Admite <strong>HTML</strong>.
    ]]></help>
    <acl>
        <ace perfil="CREADOR">
            <usuario tipoUsuario="PROFESOR"/>
        </ace>
        <ace perfil="TRAMITADOR">
            <usuario cargo="JEFE_ESTUDIOS"/>
        </ace>
    </acl>
</Tramite>
```

| Tag | Obligatorio | Contenido |
|---|---|---|
| `code` | **MUST** | UpperCamel, identificador Java **sin** guiones ni underscores: es el prefijo de la entidad de cada versión (`<code>V1`) y de los nombres de vista |
| `name` | **MUST** | Nombre visible (se traduce, §5). Es también el título por defecto de los documentos PDF de sus versiones |
| `tipoUsuario` | **MUST** | A quién va dirigido: un valor del enum `TipoUsuarioCodigo` (`subsystem/common/domains/TipoUsuarioCodigo.xml`). Agrupa el árbol de «Nuevo trámite» y decide qué perfiles da `AceProfileTipoUsuarioTramite` (§6) |
| `unidadTramitadora` | **MUST** | Quién lo tramita: un valor del enum `UnidadTramitadoraCodigo` (`subsystem/expedientes/domains/UnidadTramitadoraCodigo.xml`). Decide en qué bandeja de «Tramitación» salen sus expedientes |
| `permitidoPresentarEnRepresentacion` | Opcional (`false`) | Si `true`, al crear el expediente el usuario puede elegir «para otra persona»: entonces `personaInteresada` nace vacía y la versión **MUST** pedirla y validarla (`k-tipo-expediente` → `modelo.md` §2.1) |
| `defaultTipoExpediente` | Opcional | La **versión activa**: el **nombre de la carpeta** de la versión (`v1`). Sin este tag no se pueden crear expedientes del trámite (§4) |
| `help` | Recomendado | Ayuda del árbol «Crear un nuevo expediente», en CDATA, admite HTML. **MUST NOT** contener `]]>` |
| `acl` | Opcional | Perfiles que da **este** trámite en todos los centros (§6) |
| `publico` | Opcional | Se guarda en BD pero hoy no tiene ningún efecto: **MUST NOT** usarse |

- **MUST NOT** usar `FAMILIAR` en `tipoUsuario` para «lo presenta el familiar»: eso es `permitidoPresentarEnRepresentacion`.
- Un valor nuevo de `tipoUsuario`, `unidadTramitadora` o `cargo` **MUST** añadirse a la vez al enum y a su fichero de data-init (`subsystem/common/data-init/input/tiposUsuario.xml` y `cargos.xml`, `subsystem/expedientes/data-init/input/UnidadesTramitadoras.xml`); lo vigilan los tests `*CodigoTest`.
- Cada `<ace>` lleva `perfil` (constante del enum `Profile`) y un hijo `<usuario>` con **exactamente uno** de `tipoUsuario` o `cargo` (un valor del enum `CargoCodigo`). El trámite no se escribe: es el del propio fichero. Si falta algo, el build aborta.

- ✅ CORRECTO: `<code>MiTramite</code>`
- ❌ INCORRECTO: `<code>mi_tramite</code>` (el snake_case es para la **carpeta**, no para el `code`: rompe la entidad y las vistas)
- ✅ CORRECTO: `<defaultTipoExpediente>v1</defaultTipoExpediente>` con la carpeta `v1/` existente
- ❌ INCORRECTO: `<defaultTipoExpediente>MiTramiteV1</defaultTipoExpediente>` (es el `code` del tipo, no el nombre de su carpeta: el test T1 lo rechaza)
- ✅ CORRECTO: `<ace perfil="TRAMITADOR"><usuario cargo="JEFE_ESTUDIOS"/></ace>`
- ❌ INCORRECTO: `<ace perfil="TRAMITADOR" cargo="JEFE_ESTUDIOS"/>` (formato antiguo, sin `<usuario>`: el build aborta)
- ❌ INCORRECTO: `<ace perfil="TRAMITADOR"><usuario cargo="JEFE_ESTUDIOS" tipoUsuario="PROFESOR"/></ace>` (dos sujetos: el build aborta)

## 4. Qué genera el build

- A partir del `TramiteInstance.xml`, el build genera en `build/` (**nunca** en `src`) el data-init del trámite: la fila `Tramite`, sus `<acl>` y la asignación de la versión activa. Se refrescan en **cada arranque**, así que quitar un `<ace>` del XML lo quita de la BD.
- **MUST NOT** escribir a mano ningún data-init del trámite.
- **CRITICAL**: si `<defaultTipoExpediente>` nombra una carpeta que no existe, ni el build ni el arranque avisan: el trámite queda sin versión activa y revienta al abrirlo. Lo caza el test **T1** (`./gradlew test`, y por tanto `./run.sh`), que exige que nombre **exactamente una** carpeta bajo el trámite con su `TipoExpedienteInstance.xml` dentro.

## 5. i18n del nombre

- El build genera y mantiene `i18n_es.csv`/`i18n_ca.csv` en la carpeta del trámite, con traducción automática castellano→valenciano. **MUST NOT** crearlos a mano (`CLAUDE.md`).
- Las palabras que no deban traducirse llevan sufijo `__!!`.
- Una traducción mala se corrige editando **solo** la columna `message` del `i18n_ca.csv`; esa corrección se conserva.

## 6. Permisos

Qué perfiles tiene un usuario sobre un trámite o un expediente lo calcula `subsystem/security` a partir de las tablas `AceProfile*` (su `CLAUDE.md` explica cuáles hay). Desde un trámite solo se escriben dos:

- El `<acl>` del `TramiteInstance.xml` (§3) → vale para **todas** las versiones.
- El `<acl>` del `TipoExpedienteInstance.xml` de una versión (`k-tipo-expediente` §2) → vale **solo** para esa versión.

Reglas:

1. Para **crear** expedientes, el usuario necesita `CREADOR` (presenta telemáticamente) o `TRAMITADOR` (registra en papel) sobre el trámite en ese centro. **SHOULD** darse en el `<acl>` del trámite, que sobrevive a las versiones.
2. Cada `profile` que use algún estado de la versión **MUST** estar asignado a alguien; si no, los expedientes se atascan en ese estado.
3. Antes de añadir un `<ace>`, lee `subsystem/security/data-init/input/AceProfileGlobal.xml` y `AceProfileTipoUsuarioTramite.xml`: un perfil que ya se da globalmente o por el `tipoUsuario` del trámite **MUST NOT** repetirse en el trámite.
4. **MUST NOT** tocar esos dos ficheros desde un trámite: afectan a todos los trámites.
5. Sin ningún perfil sobre el trámite, el usuario **no lo ve**.
6. Un `<ace perfil="CREADOR">` solo da derecho a **crear**: sobre un expediente ya creado es `CREADOR` únicamente quien lo registró.
7. No se puede dar un perfil a un usuario concreto desde el fichero maestro: eso es `AceProfileCentro`, que se rellena en ejecución.

- ✅ CORRECTO: `<ace perfil="DIRECTOR"><usuario cargo="DIRECTOR"/></ace>` en el `<acl>` del trámite cuando un estado tiene `profile="DIRECTOR"`
- ❌ INCORRECTO: `<ace perfil="TRAMITADORA"><usuario cargo="DIRECTOR"/></ace>` (`perfil` no es una constante de `Profile`: la carga del data-init falla)
- ❌ INCORRECTO: `<ace perfil="CREADOR"><usuario tipoUsuario="PROFESOR"/></ace>` en un trámite de `tipoUsuario` `PROFESOR` cuando `AceProfileTipoUsuarioTramite.xml` ya lo da (duplicado)

## 7. Checklist de alta de un trámite

1. Crea `src/main/java/com/educaflow/tramites/[<agrupacion>/]<nombre_tramite>/` (snake_case).
2. Escribe `TramiteInstance.xml` con `code`, `name`, `tipoUsuario`, `unidadTramitadora`, `help` y, si aplica, `permitidoPresentarEnRepresentacion`. **Sin** `<defaultTipoExpediente>` todavía.
3. Añade al `<acl>` los perfiles que no dependan de la versión (§6).
4. Crea la primera versión en `v1/` siguiendo `k-tipo-expediente`.
5. Activa la versión: `<defaultTipoExpediente>v1</defaultTipoExpediente>`, y compila y arranca con `./run.sh`.

## 8. Anti-patrones

- **MUST NOT** crear los `i18n_*.csv` a mano ni añadir/quitar filas.
- **MUST NOT** escribir data-init del trámite en `src`.
- **MUST NOT** usar un `code` con guiones, underscores o espacios.
- **MUST NOT** declarar `<defaultTipoExpediente>` antes de que exista la carpeta de la versión, ni poner en él el `code` del tipo en vez del nombre de su carpeta (§4).

## Quick Guidelines

- Un trámite = una carpeta `tramites/<nombre_tramite>/` + un `TramiteInstance.xml`; sus versiones son tipos de expediente → `k-tipo-expediente`.
- `code` en UpperCamel sin `-`/`_`: es el prefijo de la entidad y de las vistas de cada versión.
- `tipoUsuario` = a quién va dirigido; `unidadTramitadora` = quién lo tramita; `permitidoPresentarEnRepresentacion` = si se puede presentar para otra persona (y entonces la versión pide al interesado).
- `<defaultTipoExpediente>` = nombre de la carpeta de la versión activa (`v1`), nunca el `code`; si la carpeta no existe solo lo detecta el test T1.
- Perfiles en el `<acl>` del trámite (sobreviven a las versiones); todo perfil que use un estado **MUST** estar asignado a alguien.
- Data-init e i18n los genera el build; **MUST NOT** escribirlos a mano.
