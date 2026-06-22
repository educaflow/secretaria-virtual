---
name: k-datainit
description: Explica cómo se cargan los datos iniciales (datos maestros / semilla) de la secretaría virtual sobre Axelor mediante carpetas `data-init` (un `input-config.xml` que describe el binding + una carpeta `input/` con los ficheros de datos y de permisos `auth-<sistema>.xml`). Define el formato exacto de cada fichero, dónde va la carpeta (en la raíz del sistema/subsistema **dueño** de la tabla que usa esos datos) y la regla de que cada sistema/subsistema que necesite datos iniciales obligatorios **MUST** crear su propia carpeta `data-init`. Lo consultan `/k-sistemas` y el pipeline SDD al diseñar o implementar datos iniciales.
---

# k-datainit

Eres responsable de los **datos iniciales** de la aplicación: los registros que deben existir en la base de datos al arrancar (tipos maestros, cargos, permisos, roles, datos semilla de un sistema). Este skill describe **cómo** se declaran y **dónde** viven. Lo usan `/k-sistemas` (estructura de un sistema/subsistema) y el pipeline SDD cuando un diseño exige datos iniciales.

---

## 1. Conceptos clave

- **`data-init`** es una carpeta que carga datos en la BD vía el **data-import de Axelor** al arrancar. La escanea `DataLoader` (Axelor busca el directorio de nombre `data-init` en cada módulo) y sus datos se cargan **siempre** que se cargan datos —son datos iniciales/obligatorios—, **NO** dependen de la propiedad `data.import.demo-data`.
- **No confundir con `data-demo`** (carpeta hermana que escanea `DemoLoader`): esos sí son **datos de demo** y solo se cargan si `data.import.demo-data = true` en `axelor-config.properties`. En `ModuleManager.installOne()`: `dataLoader.load(...)` corre incondicionalmente y `demoLoader.load(...)` solo `if (withDemo)`. Este skill cubre **solo `data-init`** (datos obligatorios), no `data-demo`.
- Una carpeta `data-init` contiene **siempre** dos cosas, con **nombres fijos**:
  - `input-config.xml` — el **manifiesto de binding** (`<xml-inputs>`): por cada fichero de datos declara a qué entidad/atributo se mapea cada nodo XML.
  - `input/` — la subcarpeta con los **ficheros de datos** reales (`*.xml`) y, si aplica, los `i18n_*.csv`.
- **Nombres obligatorios** (son constantes hardcodeadas en `DataLoader`: `DATA_DIR_NAME="data-init"`, `INPUT_CONFIG_NAME="input-config.xml"`, `INPUT_DIR_NAME="input"`, **no** son configurables): la carpeta **MUST** llamarse `data-init`, el manifiesto **MUST** llamarse `input-config.xml`, y los datos **MUST** colgar de `input/`. Si el fichero `data-init/input-config.xml` no existe, esa carpeta se ignora por completo. El tipo (XML vs CSV) se detecta por el **contenido** de la primera línea (`<xml-inputs` o `<csv-inputs`), no por nada del nombre.
- **Dos ubicaciones posibles**:
  1. **Junto al código de un sistema/subsistema**: `src/main/java/com/educaflow/<layer>/<nombre>/data-init/`. El build (`copyDataInit` en `build.gradle`) copia estas carpetas a `build/resources/main/java-data-init` automáticamente — basta con crearlas.
  2. **Global del proyecto**: `src/main/resources/data-init/`. Reservada **solo** para datos verdaderamente transversales que no pertenecen a ningún sistema/subsistema concreto.
- **`priority`** (atributo de `<xml-inputs>`) ordena la carga **entre** los distintos `input-config.xml` del proyecto. `DataLoader.sortDataInitByPriority()` lee el atributo `priority` del elemento raíz (default `0`) y ordena **descendente**: **mayor `priority` se carga antes** (p.ej. el `data-init` que crea `TipoTramite` tiene `priority="10"` y carga antes que el que crea `Tramite`, que lo referencia, con `priority="1"`). Úsalo para que las dependencias de datos existan antes de ser referenciadas.
  - **Matiz**: ese orden solo aplica entre manifiestos **XML** (`<xml-inputs>`). Todos los manifiestos XML se cargan **antes** que cualquier `<csv-inputs>`, sea cual sea su `priority`. Como en este proyecto todos los `input-config.xml` son XML, el orden lo gobierna `priority` sin más.

---

## 2. Convenciones del proyecto

- **CRITICAL — cada sistema/subsistema es dueño de sus datos iniciales.** Si un sistema/subsistema necesita datos iniciales obligatorios, **MUST** crear su **propia** carpeta `data-init` en su raíz (junto a `domains/`, `service/`, …). Los datos viven con el sistema/subsistema que **define la tabla principal** que los usa, no en la carpeta global.
- **Si no es un sistema/subsistema**, la carpeta `data-init` va donde tenga sentido: junto al código que define o consume esos datos.
- **CRITICAL — permisos por sistema/subsistema.** La seguridad (permisos de los objetos de un sistema/subsistema) va en un fichero `input/auth-<nombresistema>.xml` **dentro del `data-init` de ese sistema/subsistema**, no en la carpeta global. El nombre **MUST** seguir el patrón `auth-<nombresistema>.xml`.
- **Cada fichero de `input/` MUST tener su `<input>`** en el `input-config.xml` de su carpeta (con el `root`, el `type` de entidad y los `bind` de cada nodo/atributo).
- **`search` define la identidad** para upsert: usa la clave natural del registro (`self.code = :code`, `self.name = :name`, `self.codigo = :codigo`) con `create="true" update="true"` para que recargar no duplique.
- **Referencias a otras entidades**: con `search` por su clave natural y `create="false"` (no se crean al vuelo; deben existir ya — de ahí el `priority`). Ej.: `<bind node="@tipoUsuario" to="tipoUsuario" search="self.codigo = :tipoUsuario" create="false" update="false"/>`.
- **i18n**: los `input/i18n_es.csv` e `input/i18n_ca.csv` se **generan automáticamente** por el build — **MUST NOT** crearlos a mano (ver la regla i18n del `CLAUDE.md` del proyecto y `k-i18n`).

---

## 3. Patrones recomendados

### 3.1 Estructura de la carpeta

```
src/main/java/com/educaflow/<layer>/<nombre>/      ← raíz del sistema/subsistema
└── data-init/
    ├── input-config.xml                           ← manifiesto de binding (nombre fijo)
    └── input/                                      ← datos (nombre fijo)
        ├── <Datos>.xml                             ← datos de negocio del sistema
        └── auth-<nombre>.xml                       ← permisos del sistema/subsistema
```

### 3.2 `input-config.xml`

> Para la **referencia completa del formato** de `input-config.xml` (todos los atributos de `<xml-inputs>`/`<input>`/`<bind>`, binding por XPath, `search`/`create`/`update`, scripting Groovy `eval`/`if`/`call`, adaptadores y anti-patrones) consulta [`input-config.md`](input-config.md).

```xml
<?xml version="1.0"?>
<xml-inputs priority="10" xmlns="http://axelor.com/xml/ns/data-import"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://axelor.com/xml/ns/data-import
  https://axelor.com/xml/ns/data-import/data-import_8.0.xsd">

    <!-- Datos de negocio del sistema (sustituye system.gruposnotas por tu <layer>.<nombre>) -->
    <input file="Cosas.xml" root="datos">
        <bind node="cosas/cosa" type="com.educaflow.system.gruposnotas.db.Cosa"
              search="self.code = :code" create="true" update="true">
            <bind node="@code" to="code"/>
            <bind node="@name" to="name"/>
        </bind>
    </input>

    <!-- Permisos del sistema (auth-<nombre>.xml, root="auth") -->
    <input file="auth-gruposnotas.xml" root="auth">
        <bind node="permission" type="com.axelor.auth.db.Permission"
              search="self.name = :name" create="true" update="true">
            <bind node="@name" to="name"/>
            <bind node="@object" to="object"/>
            <bind node="can/@create" to="canCreate"/>
            <bind node="can/@read" to="canRead"/>
            <bind node="can/@write" to="canWrite"/>
            <bind node="can/@remove" to="canRemove"/>
            <bind node="can/@export" to="canExport"/>
        </bind>
    </input>

</xml-inputs>
```

### 3.3 Fichero de datos (`input/Cosas.xml`)

El elemento raíz **MUST** coincidir con el `root` declarado en el `<input>`:

```xml
<?xml version="1.0"?>
<datos>
    <cosas>
        <cosa code="X" name="Equis"/>
        <cosa code="Y" name="Igriega"/>
    </cosas>
</datos>
```

### 3.4 Fichero de permisos (`input/auth-<nombre>.xml`)

Un `<permission>` por objeto del sistema/subsistema. El nombre del permiso por convención es `<Entidad>.all`:

```xml
<?xml version="1.0"?>
<auth>
  <permission name="Cosa.all" object="com.educaflow.system.gruposnotas.db.Cosa">
    <can create="true" read="true" write="true" remove="true" export="true"/>
  </permission>
</auth>
```

---

## 4. Ejemplos ✅/❌

- ✅ CORRECTO: `Cargo` y `TipoUsuario` (entidades de `subsystem.common.db`) tienen sus `cargos.xml` / `tiposUsuario.xml` en `subsystem/common/data-init/input/`, porque `common` es el subsistema dueño de esas tablas.
- ❌ INCORRECTO: esos mismos `cargos.xml` / `tiposUsuario.xml` en la carpeta global `src/main/resources/data-init/input/` (los datos no viven con su dueño).
- ✅ CORRECTO: los permisos de `gruposnotas` en `system/gruposnotas/data-init/input/auth-gruposnotas.xml`.
- ❌ INCORRECTO: `auth-gruposnotas.xml` en `src/main/resources/data-init/input/` (la seguridad de un sistema no va en la global).
- ✅ CORRECTO: carpeta `data-init/` y fichero `input-config.xml` con esos nombres exactos.
- ❌ INCORRECTO: `datainit/`, `data_init/`, `initial-data/` o `config.xml` (nombres que el data-import no reconoce).

---

## 5. Anti-patrones

- **MUST NOT** acumular en `src/main/resources/data-init/input/` los datos y permisos de todos los sistemas/subsistemas: cada uno lleva su propio `data-init`. La global solo guarda lo verdaderamente transversal.
- **MUST NOT** renombrar la carpeta (`data-init`) ni el manifiesto (`input-config.xml`): el descubrimiento es por convención de nombre.
- **MUST NOT** poner registros con `search`/`create`/`update` que dupliquen al recargar (sin clave natural en `search`).
- **MUST NOT** referenciar con `create="true"` entidades que deben existir ya; usa `create="false"` y ordena con `priority`.
- **MUST NOT** crear a mano los `i18n_es.csv` / `i18n_ca.csv` del `input/` (los genera el build).
