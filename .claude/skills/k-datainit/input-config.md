# input-config.xml — formato del manifiesto de binding (XML data-import de Axelor)

Referencia detallada del **formato** del fichero `input-config.xml` que usa `/k-datainit`. El `SKILL.md` cubre **dónde** vive la carpeta `data-init` y las convenciones del proyecto; **este** fichero documenta **cómo se escribe** el manifiesto: la sintaxis de `<xml-inputs>`, `<input>`, `<bind>`, el binding por XPath y los atributos `search`/`create`/`update`/`eval`/`if`/`adapter`.

> Fuente: XSD oficial `data-import.xsd` (versión `8.1`) y la documentación `dev-guide/data-import/xml-import.adoc` + `scripting.adoc` de axelor-open-platform.

---

## 1. Modelo conceptual

El data-import de Axelor toma un **fichero de datos XML** (en `input/`) y, guiado por un **manifiesto de binding** (`input-config.xml`), crea o actualiza filas en la base de datos. El manifiesto NO contiene datos: solo describe **a qué entidad/atributo se mapea cada nodo** del fichero de datos.

- `input-config.xml` → raíz `<xml-inputs>` (manifiesto de binding).
- Cada `<input>` enlaza **un fichero de datos** (`file=`) con su elemento raíz (`root=`).
- Cada `<bind node="…">` localiza nodos vía **XPath** y los mapea a campos del modelo (`to=`).

Hay dos familias de manifiesto en Axelor: `<xml-inputs>` (datos en XML, lo que usa este proyecto) y `<csv-inputs>` (datos en CSV). Aquí solo se documenta `<xml-inputs>`.

---

## 2. Cabecera y elemento raíz `<xml-inputs>`

```xml
<?xml version="1.0"?>
<xml-inputs priority="10" xmlns="http://axelor.com/xml/ns/data-import"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://axelor.com/xml/ns/data-import
  https://axelor.com/xml/ns/data-import/data-import_8.0.xsd">

  <!-- cero o más <adapter> y uno o más <input> -->
  <input file="Cosas.xml" root="datos">
    <bind node="cosas/cosa" type="com.educaflow.system.gruposnotas.db.Cosa"
          search="self.code = :code" create="true" update="true">
      <bind node="@code" to="code"/>
    </bind>
  </input>

</xml-inputs>
```

- **`priority`** (`int`, default `0`): ordena la carga **entre distintos `input-config.xml`** del proyecto. **A MAYOR valor, se carga ANTES** (lo dice el propio XSD: *"La mayor valor, se carga primero"*). Si dos manifiestos tienen la misma prioridad, **el orden NO está garantizado**. Úsalo para que los datos referenciados existan antes de ser referenciados (p.ej. `TipoTramite` con `priority="10"` carga antes que `Tramite` con `priority="1"`).
- **Contenido**: cero o más `<adapter>` seguidos de **uno o más `<input>`** (al menos uno es obligatorio).
- El namespace **MUST** ser `http://axelor.com/xml/ns/data-import`. Por convención todos los `input-config.xml` del proyecto referencian `data-import_8.0.xsd` en el `schemaLocation`.
- **El `schemaLocation` es solo una pista** para validadores externos; en runtime Axelor resuelve el XSD por **namespace** desde el jar (el XSD empaquetado del fork, declarado `version="8.1"`), no descargando esa URL. **CRITICAL**: el atributo `priority` es un **añadido del fork** y **no existe** en el XSD público de `axelor.com`; por eso un fichero con `priority=` solo valida contra el XSD local del fork, no contra la URL pública.

---

## 3. `<input>` — enlazar un fichero de datos con una entidad

```xml
<input file="tiposUsuario.xml" root="datos">
    <bind node="tiposUsuario/tipoUsuario"
          type="com.educaflow.subsystem.common.db.TipoUsuario"
          search="self.codigo = :codigo" create="true" update="true">
        <bind node="@codigo" to="codigo"/>
        <bind node="@nombre" to="nombre"/>
    </bind>
</input>
```

Atributos de `<input>`:

| Atributo | Oblig. | Descripción |
|----------|--------|-------------|
| `file`   | **sí** | Nombre del fichero de datos dentro de `input/` (p.ej. `tiposUsuario.xml`). |
| `root`   | **sí** | Nombre del **elemento raíz** del fichero de datos. **MUST** coincidir exactamente con la etiqueta raíz del XML de `input/` (`<datos>` → `root="datos"`, `<auth>` → `root="auth"`). |

- Un `<input>` contiene cero o más `<adapter>` y **uno o más `<bind>`** de primer nivel.
- Cada `<bind>` de primer nivel suele ser el **bind de la entidad** (lleva `type=`): localiza el nodo repetido de cada registro y declara la clase JPA destino.

---

## 4. `<bind>` — el corazón del mapeo

Un `<bind>` hace una de dos cosas según dónde esté:

1. **Bind de entidad** (primer nivel, lleva `type=`): por cada nodo que casa con `node=`, instancia/busca un objeto de la clase `type=` y lo persiste. Sus `<bind>` hijos rellenan los campos.
2. **Bind de campo** (anidado, lleva `to=`): mapea el valor de un nodo/atributo XPath al campo `to=` del objeto padre.

### 4.1 Atributos de `<bind>` (XMLBind)

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| `node` | string | **XPath** que localiza el nodo (elemento, atributo o ruta relativa con `/`). Relativo al nodo padre o al `root`. |
| `to` | string | Nombre del campo del modelo destino. |
| `type` | string | FQN de la clase del objeto a bindear. Para el bind de entidad y para campos relacionales "dummy" cuando la relación no existe en el objeto actual. |
| `json-model` | string | Nombre de un modelo custom (en vez de `type`). |
| `alias` | string | Si `node` es una ruta relativa, nombre simple con el que el valor queda disponible en el contexto (para usarlo en `eval`/`if`/`search`). |
| `search` | string | Cláusula **JPQL** `where` para buscar un registro existente (upsert). |
| `create` | boolean | `false` para **impedir crear** el registro si no se encuentra (referencias a datos que deben existir ya). |
| `update` | boolean | `true` para **permitir actualizar** un registro existente. |
| `eval` | string | Expresión **Groovy** que transforma el valor. |
| `if` | string | Expresión Groovy booleana: solo bindea si es verdadera. |
| `if-empty` | boolean | Solo actualiza el campo destino si está vacío/null. |
| `adapter` | string | Adaptador de tipo, con argumento opcional separado por `\|`. |
| `call` | string | Llama a un método una vez poblado el objeto (`FQN:metodo`). Recibe `(Object bean, Map values)` (ver §6). |

- Los `<bind>` pueden **anidarse** para mapear campos relacionales (uno-a-muchos / muchos-a-uno).

> **CRITICAL — `check` / `check-message` NO funcionan en import XML.** El XSD los declara en el `complexType Bind` base (por eso un fichero con `check=` **valida** sin error), pero la clase real `XMLBind.java` **no tiene** esos campos y `XMLBinder` **nunca los lee**: en `<xml-inputs>` se **ignoran en silencio**. Solo están implementados para `<csv-inputs>` (`CSVBind`/`CSVBinder`). **MUST NOT** usar `check`/`check-message` en estos manifiestos esperando que validen nada; para condicionar el binding usa `if`, y para post-validar el objeto usa `call`.

### 4.2 Semántica de `search` / `create` / `update` (upsert)

Esta tripleta gobierna el comportamiento al recargar datos:

- **`search`** define la **identidad** del registro mediante su clave natural. Los parámetros `:nombre` del JPQL se resuelven con campos ya bindeados o valores del contexto. Ej.: `search="self.codigo = :codigo"`.
- **`create="true"` `update="true"`** → upsert idempotente: si no existe, lo crea; si existe, lo actualiza. Recargar **no duplica**. Es lo habitual para los **datos propios** del sistema.
- **`create="false"` `update="false"`** → **referencia**: busca un registro que **debe existir ya** (no lo crea ni lo modifica). Es lo habitual para apuntar a entidades de otro sistema; combínalo con `priority` para garantizar el orden.

```xml
<!-- Dato propio: upsert por clave natural -->
<bind node="cargos/cargo" type="...db.Cargo"
      search="self.code = :code" create="true" update="true">
    <bind node="@code" to="code"/>
    <bind node="@name" to="name"/>
    <!-- Referencia a otra entidad por su clave natural; NO se crea -->
    <bind node="@tipoUsuario" to="tipoUsuario"
          search="self.codigo = :tipoUsuario" update="false" create="false"/>
</bind>
```

---

## 5. Binding por XPath (`node`)

El valor de `node` es una expresión XPath **relativa** al nodo padre (o al `root` del `<input>` para el bind de entidad).

- **Atributos**: `@nombreAtributo` → `<bind node="@code" to="code"/>`.
- **Texto del nodo**: `text()` → `<bind node="text()" to="name"/>`.
- **Ruta relativa**: separa niveles con `/` → `node="tiposUsuario/tipoUsuario"`, `node="can/@create"`.
- **Filtro por atributo**: `name[@type='F']` selecciona el nodo `name` cuyo atributo `type` vale `F`.
- **Subir de nivel**: `../` navega al padre → `node="../../../@location"`.

```xml
<bind node="@code" to="code"/>                 <!-- atributo -->
<bind node="text()" to="name"/>                <!-- texto del nodo -->
<bind node="name[@type='F']" to="firstName"/>  <!-- nodo filtrado por atributo -->
<bind node="name[@type='L']" to="lastName"/>
<bind node="can/@create" to="canCreate"/>      <!-- atributo de un subnodo -->
```

`alias` permite capturar un valor de una ruta relativa y reutilizarlo después en `eval`/`if`/`search`:

```xml
<bind node="city/@country" alias="city_country"/>
<bind to="country" search="self.code = :country"
      eval="city_country" if="city_country != null"/>
```

---

## 6. Scripting Groovy: `eval`, `if`, `if-empty`, `call`

El data-import evalúa expresiones Groovy con el contexto del registro actual (nombre de campo/alias → valor).

- **`eval`** transforma el valor antes de asignarlo. Soporta interpolación `${...}`:
  ```xml
  <bind to="email"
        eval='"${firstName}.${lastName}@gmail.com".toLowerCase()'
        if="email == null || email.empty"/>
  ```
- **`if`** condiciona el binding (solo bindea si es `true`). Cuidado con `&&` en XML → `&amp;&amp;`:
  ```xml
  <bind to="country" search="self.code = :country" eval="'FR'"
        if="location_contact == null &amp;&amp; city_country == null"/>
  ```
- **`if-empty="true"`** solo actualiza si el campo destino está vacío/null (no pisa datos ya presentes).
- **`call="FQN:metodo"`** post-procesa el objeto ya poblado, antes de persistir; útil para validar o derivar campos. **CRITICAL**: en runtime se resuelve con `klass.getMethod(metodo, Object.class, Map.class)`, así que el método **MUST** tener **exactamente dos parámetros** `(Object bean, Map values)` —el bean poblado y el mapa de valores bindeados— y devolver el bean. Con un solo parámetro lanza `NoSuchMethodException`. `FQN` es el nombre completo de la clase y se instancia vía `Beans.get(...)` (es un bean Guice). Firma Groovy correcta:
  ```groovy
  def validateSaleOrder(Object bean, Map values) { ... ; return bean }
  ```
- **`eval="call: FQN:metodo(arg1, arg2)"`** es una vía distinta de `call`: invoca un método con **argumentos arbitrarios** (evaluados desde el contexto) y usa su retorno como valor del campo. Úsala cuando necesites pasar parámetros concretos; el atributo `call` (de arriba) siempre recibe `(bean, values)` fijos.

---

## 7. `<adapter>` — adaptadores de tipo

Convierte cadenas a tipos concretos (fechas, booleanos, etc.). Se declara a nivel de `<xml-inputs>` o de `<input>` y se referencia con el atributo `adapter` de un `<bind>`:

```xml
<adapter name="LocalDate" type="com.axelor.data.adapter.JavaTimeAdapter">
    <option name="format" value="dd/MM/yyyy"/>
</adapter>
...
<bind node="@fecha" to="fecha" adapter="LocalDate"/>
```

El argumento opcional del `adapter` del `<bind>` se separa con `|` (p.ej. `adapter="LocalDate|dd-MM-yyyy"`).

---

## 8. Ejemplo completo: permisos (`auth-<sistema>.xml`)

Manifiesto que mapea un fichero `auth-common.xml` (raíz `<auth>`) a `com.axelor.auth.db.Permission`:

```xml
<input file="auth-common.xml" root="auth">
    <bind node="permission" type="com.axelor.auth.db.Permission"
          search="self.name = :name" create="true" update="true">
        <bind node="@name" to="name"/>
        <bind node="@object" to="object"/>
        <bind node="@condition" to="condition"/>
        <bind node="@conditionParams" to="conditionParams"/>
        <bind node="can/@create" to="canCreate"/>
        <bind node="can/@read" to="canRead"/>
        <bind node="can/@write" to="canWrite"/>
        <bind node="can/@remove" to="canRemove"/>
        <bind node="can/@export" to="canExport"/>
    </bind>
</input>
```

Fichero de datos `input/auth-common.xml` (la raíz `<auth>` coincide con `root="auth"`):

```xml
<?xml version="1.0"?>
<auth>
  <permission name="Cosa.all" object="com.educaflow.system.gruposnotas.db.Cosa">
    <can create="true" read="true" write="true" remove="true" export="true"/>
  </permission>
</auth>
```

---

## 9. Checklist de validación del manifiesto

- [ ] La raíz es `<xml-inputs>` con el namespace `http://axelor.com/xml/ns/data-import`.
- [ ] Hay al menos un `<input>`, cada uno con `file=` (existe en `input/`) y `root=` (= etiqueta raíz del fichero de datos).
- [ ] Cada bind de entidad lleva `type=` (FQN válido) y `search=` con clave natural.
- [ ] Datos propios → `create="true" update="true"`; referencias → `create="false" update="false"`.
- [ ] Las dependencias de datos se ordenan con `priority` (mayor = antes).
- [ ] Los `&&`/`<`/`>` dentro de `if`/`eval` van escapados (`&amp;&amp;`, `&lt;`, `&gt;`).
- [ ] Los `node` XPath casan con la estructura real del fichero de datos.
- [ ] NO se usan `check`/`check-message` (no funcionan en XML; usar `if` o `call`).
- [ ] Si hay `call=`, el método tiene firma `(Object bean, Map values)` y devuelve el bean.
- [ ] Los `i18n_*.csv` de `input/` NO se crean a mano (los genera el build).

---

## 10. Anti-patrones del formato

- **MUST NOT** poner el `root` distinto de la etiqueta raíz del fichero de datos (no casa y no carga nada).
- **MUST NOT** omitir `search`: sin clave natural, recargar **duplica** registros.
- **MUST NOT** usar `create="true"` en referencias a entidades que deben existir ya (crearía duplicados huérfanos); usa `create="false"` + `priority`.
- **MUST NOT** confiar en el orden de carga entre manifiestos con la misma `priority` (no está garantizado).
- **MUST NOT** usar `check`/`check-message` en `<xml-inputs>`: validan en el XSD pero el binder XML los ignora (solo funcionan en CSV). Usa `if` para condicionar o `call` para post-validar.
- **MUST NOT** declarar el método de `call` con un solo parámetro: requiere `(Object bean, Map values)` o falla en runtime con `NoSuchMethodException`.
- **MUST NOT** dejar `&&` / `<` / `>` sin escapar en atributos `if`/`eval`/`search` (XML inválido).
