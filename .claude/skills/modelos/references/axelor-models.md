# Models — Axelor Open Platform

> Documentación extraída de: https://docs.axelor.com/adk/8.0/dev-guide/models/models.html

---

## Tabla de contenidos

- [Introducción](#introducción)
- [Fields](#fields)
  - [Atributos comunes](#atributos-comunes)
  - [Atributos de campos no relacionales](#atributos-de-campos-no-relacionales)
  - [Tipos de campo](#tipos-de-campo)
    - [String](#string)
    - [Boolean](#boolean)
    - [Integer](#integer)
    - [Long](#long)
    - [Decimal](#decimal)
    - [Date](#date)
    - [Time](#time)
    - [DateTime](#datetime)
    - [Enum](#enum)
    - [Binary](#binary)
    - [ManyToOne](#manytoone)
    - [OneToOne](#onetoone)
    - [OneToMany](#onetomany)
    - [ManyToMany](#manytomany)
  - [Otros usos](#otros-usos)
    - [Formula](#formula)
- [Index](#index)
- [Unique Constraint](#unique-constraint)
- [Field Encryption](#field-encryption)
- [Entity Listeners](#entity-listeners)

---

## Introducción

Las clases de entidad representan modelos de dominio y se definen en formato XML.

Cada archivo debe tener la siguiente declaración:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://axelor.com/xml/ns/domain-models
  https://axelor.com/xml/ns/domain-models/domain-models_8.0.xsd">

  <!-- entity definitions here -->

</domain-models>
```

> ⚠️ Las palabras reservadas de Java no pueden usarse como nombres de campo. Las palabras reservadas SQL (PostgreSQL, MySQL y Oracle) no pueden usarse como nombres de columna.

### Ejemplo completo

`axelor-contact/src/main/resources/domains/Address.xml`

```xml
<module name="contact" package="com.axelor.contact.db" />

<entity name="Contact">
  <many-to-one name="title" ref="Title"/>            <!-- (1) -->
  <string name="firstName" required="true" />         <!-- (2) -->
  <string name="lastName" required="true" />

  <string name="fullName" namecolumn="true" search="firstName,lastName"> <!-- (3) -->
    <![CDATA[
    if (firstName == null && lastName == null)
        return null;
    if (title == null)
        return firstName + " " + lastName;
    return title.getName() + " " + firstName + " " + lastName;
  ]]></string>

  <date name="dateOfBirth"/>

  <string name="email" required="true" unique="true" max="100" />
  <string name="phone" max="20" massUpdate="true"/>
  <string name="notes" title="About me" large="true" />

  <one-to-many name="addresses" ref="Address" mappedBy="contact"/>  <!-- (4) -->

  <finder-method name="findByName" using="fullName" />   <!-- (5) -->
  <finder-method name="findByEmail" using="email" />
</entity>
```

| # | Descripción |
|---|-------------|
| 1 | Define un campo `many-to-one` llamado `title` que referencia al objeto `Title` |
| 2 | Define un campo string `firstName` |
| 3 | Define un campo string calculado `fullName` |
| 4 | Define un campo `one-to-many` llamado `addresses` que referencia al objeto `Address` |
| 5 | Define un método finder personalizado `findByName` |

### Atributos del tag `<entity>`

Un modelo de dominio se define usando el tag `<entity>`, que soporta los siguientes atributos:

| Atributo | Descripción |
|----------|-------------|
| `name` | Nombre de la entidad (debe comenzar con mayúscula) |
| `cacheable` | Si la entidad debe ser cacheable (por defecto `false`) |
| `repository` | Cómo generar la clase repositorio: `none`, `default` o `abstract` |
| `table` | Nombre de la tabla para la entidad |
| `logUpdates` | Si habilitar el registro de actualizaciones (por defecto `true`) |
| `extends` | Clase base de herencia |
| `implements` | Lista de interfaces a implementar |
| `persistable` | Si la entidad es persistible en base de datos |
| `strategy` | Estrategia de herencia: `SINGLE`, `JOINED` o `CLASS` (por defecto `SINGLE`) |
| `equalsIncludeAll` | Si incluir todos los campos simples no funcionales en el test de igualdad (por defecto `false`) |
| `jsonAttrs` | Si habilitar/deshabilitar la generación del campo JSON `attrs` |

### Estrategias de herencia (`strategy`)

El atributo `strategy` solo se puede usar en la entidad base. Cada estrategia produce una estructura de base de datos diferente con ventajas e inconvenientes específicos:

- **`SINGLE`**: Crea una única tabla para toda la jerarquía de clases. Es el valor por defecto si no se especifica explícitamente.
- **`JOINED`**: Crea una tabla separada para cada clase, conteniendo únicamente sus campos específicos. La tabla hija se enlaza a la tabla padre mediante una clave foránea. Es el enfoque más normalizado.
- **`CLASS`**: Crea tablas separadas e independientes para cada clase concreta. Cada tabla contiene todas las columnas (heredadas y específicas).

> ⚠️ **Elige tu estrategia con cuidado.** Cambiar la estrategia de herencia una vez en producción requiere una migración de datos compleja.
>
> - **Recomendado:** Usa `SINGLE` para jerarquías simples o `JOINED` para modelos de datos complejos que requieran integridad referencial estricta.
> - **Precaución con `CLASS`:** Aunque puede parecer conveniente, tiene inconvenientes de rendimiento significativos: las consultas sobre la entidad padre pueden volverse extremadamente lentas a medida que crecen los datos (debido a operaciones `UNION ALL`).

### Entidades no persistibles

El atributo `persistable` puede usarse para definir una clase de entidad no persistible anotada con `@MappedSuperclass`, que puede ser utilizada como clase base de otras entidades.

### Tag `<module>`

El tag `<module>` puede usarse para definir los nombres de paquete de las entidades y repositorios generados:

```xml
<!-- comportamiento por defecto -->
<module name="contact"
  package="com.axelor.contact.db"
  repo-package="com.axelor.contact.db.repo"
  table-prefix="contact" />

<!-- comportamiento personalizado -->
<module name="contact"
  package="my.models"
  repo-package="my.repos"
  table-prefix="my" />
```

| Atributo | Descripción |
|----------|-------------|
| `name` | Requerido. Agrupa las entidades en un módulo lógico |
| `package` | Requerido. Nombre del paquete Java de la clase de entidad generada |
| `repo-package` | Opcional. Nombre del paquete Java de la clase repositorio generada. Por defecto es `<package>.repo` |
| `table-prefix` | Opcional. Prefijo del nombre de tabla. Por defecto es el `name` del módulo |

> 💡 Si el nombre del paquete termina en `.db`, la penúltima parte del nombre de paquete se usa como prefijo de tabla por defecto. Por ejemplo, si el paquete es `com.axelor.sale.db`, se usará `sale` como prefijo de tabla por defecto.

---

## Fields

Los campos de distintos tipos se usan para definir las propiedades del modelo.

### Atributos comunes

Los siguientes atributos son comunes a todos los tipos de campo:

| Atributo | Descripción |
|----------|-------------|
| **`name`** | Nombre del campo (requerido) |
| `title` | Título de visualización del campo |
| `help` | Cadena de ayuda detallada |
| `column` | Nombre de columna en base de datos (si el nombre del campo es una palabra reservada) |
| `index` | Si se debe generar un índice para este campo |
| `default` | Valor por defecto del campo |
| `required` | Si el valor del campo es requerido |
| `readonly` | Si el valor del campo es de solo lectura |
| `unique` | Si el valor del campo es único (define un constraint de unicidad) |
| `insertable` | Si la columna se incluye en los `INSERT` SQL generados por el proveedor de persistencia |
| `updatable` | Si la columna se incluye en los `UPDATE` SQL generados por el proveedor de persistencia |
| `hidden` | Si el campo está oculto por defecto en las interfaces de usuario |
| `transient` | Si el campo es transitorio (no puede guardarse en base de datos) |
| `initParam` | Si usar el campo como parámetro del constructor |
| `massUpdate` | Si permitir actualización masiva sobre este campo |

### Atributos de campos no relacionales

Los campos no relacionales tienen los siguientes atributos adicionales:

| Atributo | Descripción |
|----------|-------------|
| `nullable` | Permite almacenar valor nulo en campos que por defecto usan su valor de sistema cuando no se proporciona valor |
| `selection` | Nombre de clave de selección |
| `equalsInclude` | Si el campo se incluye en el test de igualdad |
| `formula` | Si es un campo de fórmula SQL nativa |

---

## Tipos de campo

### String

El campo `<string>` se usa para definir campos de datos textuales.

Atributos adicionales:

| Atributo | Descripción |
|----------|-------------|
| `min` | Longitud mínima del texto |
| `max` | Longitud máxima del texto |
| `large` | Si usar tipo texto largo |
| `search` | Lista separada por comas de nombres de campo usados por el componente de autocompletado para buscar |
| `sequence` | Usa el generador de secuencia personalizado especificado |
| `multiline` | Si el string es texto multilínea (usado por componentes de UI) |
| `translatable` | Si el valor del campo es traducible |
| `password` | Si el campo almacena texto de contraseña |
| `encrypted` | Si el campo está cifrado ([ver más](#field-encryption)) |
| `json` | Si el campo se usa para almacenar datos JSON |
| `namecolumn` | Si es una columna de nombre (usada por componentes de UI para mostrar el registro) |

**Ejemplo:**

```xml
<string name="firstName" min="1" />
<string name="lastName"/>
<string name="notes" large="true" multiline="true"/>
```

#### Campo traducible (`translatable`)

El atributo `translatable` puede usarse para marcar los valores del campo como traducibles. Por ejemplo:

```xml
<entity name="Product">
  <string name="name" translatable="true" />
</entity>
```

Los valores traducidos se almacenan en la misma tabla de traducción general (sin guardar contexto).

#### Campo cifrado (`encrypted`)

Los valores de campos `encrypted` se almacenan en base de datos usando cifrado AES-256. La contraseña debe proporcionarse desde el archivo de configuración de la aplicación usando la clave `encryption.password`.

---

### Boolean

El campo `<boolean>` se usa para definir campos de tipo booleano.

**Ejemplo:**

```xml
<boolean name="active" />
```

---

### Integer

El campo `<integer>` se usa para definir campos numéricos sin decimales.

| Atributo | Descripción |
|----------|-------------|
| `min` | Valor mínimo (inclusive) |
| `max` | Valor máximo (inclusive) |

**Ejemplo:**

```xml
<integer name="quantity" min="1" max="100"/>
<integer name="count"/>
```

---

### Long

El campo `<long>` se usa para definir campos numéricos sin decimales cuando el valor no puede representarse con el tipo `integer`.

> ⚠️ Evita usar este tipo de campo ya que algunos SGBD (como Oracle) solo permiten una columna `long` por tabla (y ya existe una para la columna `id`).

| Atributo | Descripción |
|----------|-------------|
| `min` | Valor mínimo (inclusive) |
| `max` | Valor máximo (inclusive) |

**Ejemplo:**

```xml
<long name="counter"/>
```

---

### Decimal

El campo `<decimal>` se usa para definir campos de tipo decimal usando el tipo Java `java.math.BigDecimal`.

| Atributo | Descripción |
|----------|-------------|
| `min` | Valor mínimo (inclusive) |
| `max` | Valor máximo (inclusive) |
| `precision` | Precisión del valor decimal (número total de dígitos) |
| `scale` | Escala del valor decimal (número total de dígitos en la parte decimal) |

**Ejemplo:**

```xml
<decimal name="price" precision="8" scale="2" />
```

---

### Date

El campo `<date>` se usa para definir campos que almacenan fechas usando el tipo Java `java.time.LocalDate`.

**Ejemplo:**

```xml
<date name="orderDate" />
```

---

### Time

El campo `<time>` se usa para definir campos que almacenan valores de hora usando el tipo Java `java.time.LocalTime`.

**Ejemplo:**

```xml
<time name="duration" />
```

---

### DateTime

El campo `<datetime>` se usa para definir campos que almacenan valores de fecha y hora usando el tipo Java `java.time.LocalDateTime`.

| Atributo | Descripción |
|----------|-------------|
| `tz` | Si usar información de zona horaria |

Cuando `tz` es `true`, el tipo Java es `java.time.ZonedDateTime`.

**Ejemplo:**

```xml
<datetime name="startsOn" />
<datetime name="startsOn" tz="true"/>
```

---

### Enum

El campo `<enum>` se usa para definir campos con tipo enumeración Java.

| Atributo | Descripción |
|----------|-------------|
| `ref` | Nombre completamente cualificado del tipo de enumeración |

**Ejemplo:**

```xml
<enum name="status" ref="OrderStatus" />
```

La enumeración `OrderStatus` debe definirse usando XML de dominio:

**Enum con valores por defecto:**

```xml
<enum name="OrderStatus">
  <item name="DRAFT" />
  <item name="OPEN" />
  <item name="CLOSED" />
  <item name="CANCELED" />
</enum>
```

**Enum con valores string personalizados:**

```xml
<enum name="OrderStatus">
  <item name="DRAFT" value="draft" />
  <item name="OPEN" value="open" />
  <item name="CLOSED" value="closed" />
  <item name="CANCELED" value="canceled" />
</enum>
```

**Enum con valores numéricos personalizados:**

```xml
<enum name="OrderStatus" numeric="true">
  <item name="DRAFT" value="1" />
  <item name="OPEN" value="2" />
  <item name="CLOSED" value="3" />
  <item name="CANCELED" value="4" />
</enum>
```

#### Consultas JPQL con campos `enum`

Para consultas JPQL sobre campos `enum`, siempre se deben usar parámetros de consulta:

```java
// Forma CORRECTA
TypedQuery<Order> query = em.createQuery(
  "SELECT s FROM Order s WHERE s.status = :status");
query.setParameter("status", OrderStatus.OPEN);

// Forma INCORRECTA
TypedQuery<Order> query = em.createQuery(
  "SELECT s FROM Order s WHERE s.status = 'OPEN'");

// Usando la API de consultas ADK
Query<Order> q = Query.of(Order.class)
  .filter("self.status = :status")
  .bind("status", "OPEN");

// o con el enum directamente
Query<Order> q = Query.of(Order.class)
  .filter("self.status = :status")
  .bind("status", OrderStatus.OPEN);

// o como argumentos posicionales
Query<Order> q = Query.of(Order.class)
  .filter("self.status = ?1 OR self.status = ?2", "DRAFT", OrderStatus.OPEN);
```

#### Enum en expresiones de scripting

En expresiones de scripting, el `enum` debe referenciarse usando su nombre de tipo. Por ejemplo:

```xml
<check
  field="confirmDate"
  if="status == OrderStatus.OPEN &amp;&amp; confirmDate == null"
  error="Invalid value..." />
```

---

### Binary

El campo `<binary>` se usa para almacenar blobs binarios.

| Atributo | Descripción |
|----------|-------------|
| `image` | Si el campo está destinado a almacenar datos de imagen |
| `encrypted` | Si el campo está cifrado |
| `large` | Si los datos son muy grandes |

> ⚠️ Usa este campo solo para datos binarios pequeños o no reutilizables. Para otros casos, prefiere usar un `many-to-one` a `com.axelor.meta.db.MetaFile`.

Por defecto, un campo binario se mapea al tipo de base de datos `bytea`. Al usar `large="true"`, se mapea al tipo `oid`, que referencia a un objeto grande almacenado en la tabla del sistema `pg_largeobject`.

**Consideraciones al usar `large="true"`:**
- **Limpieza manual necesaria:** Eliminar una fila no elimina el Large Object. Es necesario llamar a `lo_unlink()` o usar la extensión `lo` con triggers para evitar datos huérfanos.
- **Backup/restore más complicado:** Dado que los OIDs se almacenan en una tabla del sistema global (`pg_largeobject`), pueden complicar los backups.

**Ejemplo:**

```xml
<binary name="photo" image="true" />
<binary name="report" />
```

---

### ManyToOne

El campo `<many-to-one>` se usa para definir un campo de referencia de valor único usando una relación muchos-a-uno.

| Atributo | Descripción |
|----------|-------------|
| `ref` | Nombre de la clase de entidad referenciada (FQN si no está en el mismo paquete) |
| `table` | Especifica el nombre de la tabla de unión |
| `column2` | Nombre de la columna de clave foránea en la tabla de base de datos que referencia a la tabla no propietaria |

**Ejemplo:**

```xml
<many-to-one name="customer" ref="com.axelor.contact.db.Contact" />
```

---

### OneToOne

El campo `<one-to-one>` se usa para definir un campo de referencia de valor único usando una relación uno-a-uno.

| Atributo | Descripción |
|----------|-------------|
| `ref` | Nombre de la clase de entidad referenciada (FQN si no está en el mismo paquete) |
| `mappedBy` | Para campos bidireccionales, nombre del campo del lado propietario |
| `orphanRemoval` | Especifica si eliminar los registros huérfanos si se eliminan de la relación |
| `table` | Especifica el nombre de la tabla de unión |
| `column2` | Nombre de la columna de clave foránea en la tabla que referencia a la tabla no propietaria |

**Ejemplo:**

```xml
<!-- definido en el objeto Engine -->
<one-to-one name="car" ref="com.axelor.cars.db.Car" />

<!-- definido en el objeto Car -->
<one-to-one name="engine" ref="com.axelor.cars.db.Engine" mappedBy="car"/>
```

---

### OneToMany

El campo `<one-to-many>` se usa para definir campos de múltiples valores usando una relación uno-a-muchos.

| Atributo | Descripción |
|----------|-------------|
| `ref` | Nombre de la clase de entidad referenciada (FQN si no está en el mismo paquete) |
| `mappedBy` | Para campos bidireccionales, nombre del campo `many-to-one` inverso |
| `orphanRemoval` | Si eliminar los registros huérfanos (por defecto `true`) |
| `orderBy` | Especifica el ordenamiento de la colección por el campo indicado |
| `table` | Especifica el nombre de la tabla de unión |
| `column2` | Nombre de la columna de clave foránea que referencia a la tabla no propietaria |

**Ejemplo:**

```xml
<one-to-many name="items" ref="OrderItem" mappedBy="order" />
<one-to-many name="addresses" ref="Address" mappedBy="contact" />
```

---

### ManyToMany

El campo `<many-to-many>` se usa para definir campos de múltiples valores usando una relación muchos-a-muchos.

| Atributo | Descripción |
|----------|-------------|
| `ref` | Nombre de la clase de entidad referenciada (FQN si no está en el mismo paquete) |
| `mappedBy` | Para campos bidireccionales, nombre del campo del lado propietario |
| `orderBy` | Especifica el ordenamiento de la colección por el campo indicado |
| `table` | Especifica el nombre de la tabla de unión |
| `column2` | Nombre de la columna de clave foránea que referencia a la tabla no propietaria |

**Ejemplo:**

```xml
<many-to-many name="taxes" ref="Tax" />
```

---

## Otros usos

### Formula

El atributo `formula="true"` en un campo se usa para definir un fragmento SQL nativo (fórmula) en lugar de mapear una propiedad a una columna. Este tipo de campo es de solo lectura (su valor es calculado por el fragmento de fórmula) y no se crea ni guarda en base de datos.

El fragmento SQL puede ser tan complejo como se necesite, e incluso puede incluir subconsultas.

> ⚠️ Ten en cuenta que el uso de campos fórmula requiere una cláusula SQL nativa que puede afectar la portabilidad entre bases de datos.

**Ejemplos:**

```xml
<string name="fullName" namecolumn="true" search="firstName,lastName" formula="true">
  <![CDATA[
        CASE
            WHEN title IS NULL THEN first_name || ' ' || last_name
            ELSE (SELECT contact_title.name FROM contact_title WHERE contact_title.id = title) || ' ' || first_name || ' ' || last_name
        END
  ]]>
</string>

<string name="owner" formula="true">
  <![CDATA[
        ( SELECT CASE WHEN c.type = 'owner' THEN c.firstname + ' ' + c.lastname END FROM contacts c where c.folder_id = id )
  ]]>
</string>
```

---

## Index

El tag `<index>` puede usarse para definir un índice compuesto.

| Atributo | Descripción |
|----------|-------------|
| `columns` | Lista de nombres de campo separados por comas (también puede usarse el nombre de columna de la base de datos del atributo `column` del campo) |
| `name` | Nombre opcional para el índice |

**Ejemplo:**

```xml
<index columns="firstName,lastName,fullName" name="idx_names"/>
```

También puede definirse un índice en un campo usando el atributo `index`. Se puede proporcionar un nombre de índice personalizado (comenzando con el prefijo `idx_`); de lo contrario, se genera un nombre de índice por defecto usando el nombre de la tabla y el nombre de la columna.

> 💡 Por defecto, todos los campos de referencia, `namecolumn`, `name` y `code` se indexan automáticamente.

**Ejemplo:**

```xml
<string name="firstName" required="true" index="true"/>
<string name="lastName" required="true" index="idx_contact_last_name"/>
```

---

## Unique Constraint

El tag `<unique-constraint>` puede usarse para definir un constraint de unicidad compuesto.

| Atributo | Descripción |
|----------|-------------|
| `columns` | Lista de nombres de campo separados por comas (también puede usarse el nombre de columna de la base de datos del atributo `column` del campo) |
| `name` | Nombre opcional para el constraint de unicidad |

**Ejemplo:**

```xml
<unique-constraint columns="firstName,lastName" />
<unique-constraint columns="firstName,lastName" name="uni_contact_first_name_last_name"/>
```

---

## Field Encryption

A partir de la versión 5.0, es posible cifrar campos sensibles. Para usar esta funcionalidad, se requieren las siguientes configuraciones de la aplicación:

```properties
# Encryption
# ~~~~~
# Set encryption password
encryption.password = MySuperSecretKey

# Set encryption algorithm (CBC or GCM)
#encryption.algorithm = CBC
```

Los campos `<string>` y `<binary>` pueden marcarse como cifrados de la siguiente manera:

```xml
<string name="myEmail" encrypted="true" />
<binary name="myPicture" encrypted="true" />
```

> ⚠️ Los valores cifrados serán más largos que los valores originales, por lo que debes asegurarte de que el tamaño del campo sea suficientemente grande para almacenar el valor cifrado en la base de datos.

---

## Entity Listeners

Se pueden usar uno o más tags `<entity-listener>` para definir [entity listeners](https://javaee.github.io/javaee-spec/javadocs/javax/persistence/EntityListeners.html). Esto añade una anotación `@EntityListeners` a la clase de entidad generada:

```xml
<entity name="Contact">
  ...
  <entity-listener class="com.axelor.contact.db.repo.ContactListener"/>
</entity>
```

| Atributo | Descripción |
|----------|-------------|
| `class` | Nombre completamente cualificado de la clase entity listener |

A continuación puedes definir tus propias clases entity listener con métodos de callback anotados con las anotaciones de eventos del ciclo de vida:

```java
public class ContactListener {

  // Invocado en eventos PostPersist o PostUpdate sobre objetos Contact.
  @PostPersist
  @PostUpdate
  private void onPostPersistOrUpdate(Contact contact) {
    System.out.println("Contact saved");
  }
}
```

### Anotaciones de eventos del ciclo de vida

| Anotación | Descripción |
|-----------|-------------|
| `@PrePersist` | Antes de persistir la entidad |
| `@PostPersist` | Después de persistir la entidad |
| `@PreRemove` | Antes de eliminar la entidad |
| `@PostRemove` | Después de eliminar la entidad |
| `@PreUpdate` | Antes de actualizar la entidad |
| `@PostUpdate` | Después de actualizar la entidad |
| `@PostLoad` | Después de cargar la entidad |

---
