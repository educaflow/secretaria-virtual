# Controllers

The controllers are intermediary between views & services

```java
package com.axelor.contact.web;

import jakarta.inject.Inject;
import com.google.inject.servlet.RequestScoped;

import com.axelor.contact.db.Contact;
import com.axelor.contact.service.HelloService;

import com.axelor.meta.CallMethod;

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;

@RequestScope <1>
public class HelloController {

  @Inject private HelloService service; <2>

  public void say(ActionRequest request, ActionResponse response) { <3>

    Contact contact = request.getContext().asType(Contact.class); <4>
    String message = service.say(contact); <5>

    response.setFlash(message); <6>
  }

  @CallMethod <7>
  public Response validate(String email) { <8>

    Response response = new ActionResponse();

    // validate email & set response properties
    // logic can be moved to service layer

    if (email == null) {
      response.addError("email", "Email required");
    } else if (!email.matches("\w+@\w+")) {
      response.addError("email", "Invalid email.");
    }

    return response;
  }

    @CallMethod
    public int add(int a, int b) { <9>
        return a+b;
    } 
  
}
```

- `<1>` controller lifecycle
- `<2>` inject a service
- `<3>` controller method
- `<4>` get the view context and convert to business object
- `<5>` call service method
- `<6>` mark the response to flash the message on client
- `<7>` free form controller method should be annotated with `@CallMethod`
- `<8>` free form controller method
- `<9>` free form controller method , return a value that can be used in the view

The `ActionRequest` and `ActionResponse` are special classes to deal with action requests and responses. 

### Response Signals

`ActionResponse.setSignal(signal, data)` is used to send any arbitrary signal to the client. Here are a couple of them that might be of interest:

| Signal        | Description                                             |
|---------------|---------------------------------------------------------|
| `refresh-app` | refresh browser tab (send null data)                    |
| `refresh-tab` | refresh current tab in the application (send null data) |
| `back`        | In current tab go to the previous view  (send null data)|

The free form controller methods can accept any parameter. The views/actions
can pass the param values from the current context.

Controllers generally don't implement business logic, but deal with RPC requests only.

The controller methods can be used from XML actions and views:

```xml
<button name="greet" title="Greet" onClick="com.axelor.contact.web.HelloController:say" />
```

Or a free form controller method

```xml
<form name="contact-form" model="com.axelor.contact.db.Contact">
  ...
  <field name="email" onChange="com.axelor.contact.web.HelloController:validate(email)"/>
  ...
</form>
```

The format of using controller method is like this:

```text
<fqcn>:<method>[(var1,var2[,...])]
```

where `fqcn` is fully qualified class name of the controller, followed by a colon `:`
followed by `method` name and optionally parameter values from current context
if the method is a free form method.


