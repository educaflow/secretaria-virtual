# Method Action

The `<action-method>` can be used to call a controller method.

```xml
<action-method name="act.hello">
  <call class="com.axelor.contact.web.HelloController" method="say"/>
</action-method>
```

## Attributes

| Name | Description |
| --- | --- |
| **name** | name of the action |

The method action requires following items:

- `<call>` - define a call
  - `class` - fully qualified name of the target class
  - `method` - method name

`action-method` can also be called from any objects with arbitrary arguments.
The result of the method can be assigned to any field.

Suppose the following controller:

```java
import com.axelor.meta.CallMethod;

public class Hello {

  @CallMethod
  public String say(String what) {
    return "About: " + what;
  }
}
```

We can call this method with action-method like this:

```xml
<action-method name="act.hello">
  <call class="com.axelor.contact.web.HelloController" method="say(fullName)"/>
</action-method>
```

