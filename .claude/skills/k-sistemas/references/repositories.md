# Repositories

## Table of Contents

- [Finders](#finders)
- [Extra Code](#extra-code)

---

The repository classes provides data access and manipulation methods for the domain
models.

The `repository` attribute on `<entity>` can be used to customize the repository
class generation.

- `default` - generate default repository class (it's default behavior)
- `abstract` - generate abstract repository class (module may provide concrete implementation)
- `none` - do not generate repository class (if not required or module provides custom repository class)

Repository classes are further customized with `<finder>` and `<extra-code>` sections.
However, it's always advisable to generate only required repositories and if extra
features are required generate abstract repository and provide concrete implementation
with more features.

Modules can provide different implementation of a repository and configure them
using Guice module (however, only one implementation can be bound).

---

## Finders

The `<finder-method>` tag can be used to define finder methods for the repository classes.

| Attribute | Description |
|---|---|
| **`name`** | method name, as a convention use `findBy` prefix |
| **`using`** | fields to be used as method params, see bellow for custom params |
| `filter` | provide custom filter expression (jpql where clause) |
| `all` | whether to return all values (default false) |
| `orderBy` | name of the field to order the result |
| `cacheable` | whether the select query used is cacheable |
| `flush` | whether the query flush mode is auto (default true) |

The finder method parameters are specified by the `using` attribute value
(comma-separated list). A parameter can be a field name defined above or/and
a definition in the format `type:name` where `type` can be either of the
following type:

- `int`, `long`, `double`, `boolean`
- `Integer`, `Long`, `Double`, `Boolean`
- `String`, `BigDecimal`
- `LocalDate`, `LocalTime`, `LocalDateTime`, `DateTime`

If all the parameters are field names and `filter` is not given then an
ANDed filter on the given names is generated.

examples:

```xml
<finder-method name="findByName" using="fullName" />
<finder-method name="findByNameOrEmail" using="fullName,email" />

<finder-method name="findByCountry" using="String:country"
  filter="self.country.code = :country" all="true" />

<finder-method name="findByAnyOf" using="long:id,email,String:country"
  filter="self.id = :id or self.email = :email or self.country.code = :country"
  all="true" />
```

---

## Extra Code

The `<extra-import>` and `<extra-code>` tags are used to define additional code
to be included in generated repository classes.

Don't use this feature extensively. Use abstract repository class and provide
more features using concrete implementation. This is more readable and better
supported in IDE code editors.

example:

```xml
<extra-imports>
<![CDATA[
import org.slf4j.*;
import java.util.*;
]]>
</extra-imports>

<extra-code>
<![CDATA[
protected final Logger log = LoggerFactory.getLogger(getClass());

public List<String> getFooNames() {
  final List<String> names = new ArrayList<>();
  names.add("foo");
  names.add("bar");
  return names;
}

public Title saveAndLog(Title title) {
  log.info("saving Title instance: " + title.getCode());
  try {
    title = this.save(title);
  } catch (Exception e) {
    log.error("error saving Title");
  }
  return title;
}
]]>
</extra-code>
```

However, the same thing can be achieved like this:

```xml
<entity name="Title" repository="abstract">
...
</entity>
```

and a concrete implementation of generated `AbstractTitleRepository` like this:

```java
package com.axelor.contact.db.repo;

import org.slf4j.*;
import java.util.*;

public class TitleRepository extends AbstractTitleRepository {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  public List<String> getFooNames() {
    final List<String> names = new ArrayList<>();
    names.add("foo");
    names.add("bar");
    return names;
  }

  public Title saveAndLog(Title title) {
    log.info("saving Title instance: " + title.getCode());
    try {
      title = this.save(title);
    } catch (Exception e) {
      log.error("error saving Title");
    }
    return title;
  }
}
```

