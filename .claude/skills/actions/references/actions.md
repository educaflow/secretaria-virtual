# Actions

## Table of Contents

- [Concepts](#concepts)
- [Events](#events)
- [Context](#context)
- [Expressions](#expressions)
  - [Helper functions](#helper-functions)
- [Built-ins variables](#built-ins-variables)
- [Special actions](#special-actions)
- [Attrs Action](#attrs-action)
- [Condition Action](#condition-action)
- [Export Action](#export-action)
- [Group Action](#group-action)
- [Import Action](#import-action)
- [Method Action](#method-action)
- [Record Action](#record-action)
- [Script Action](#script-action)
- [Validate Action](#validate-action)
- [Actions & Menus](#actions--menus)
  - [View Action](#view-action)
  - [Application Menu](#application-menu)
- [WebService Action](#webservice-action)

---

In previous chapter we have checked about the object views. The views however
requires actions to do something useful other than just CRUD operations.

The actions are also defined using xml and can be used to change views, objects,
call controllers or doing some specific tasks like sending email, importing
remote data etc.

In this chapter we'll see different kind of actions.

---

## Concepts

Actions can be used to perform some advanced operations on the view, including:

- providing default values
- changing form values
- changing field attributes
- calling controller methods
- validating current record

Actions are re-usable. So each action should be given a unique name.

---

## Events

Actions can be performed on various events. This includes:

- `onNew` - when creating new record
- `onLoad` - when record is loaded in form view
- `onSave` - when record is about to save
- `onChange` - when a field value is changed
- `onSelect` - when select record button is clicked (relational fields only)
- `onClick` - when a field/button is clicked

---

## Context

The context is a map of key-value pair of the current object being edited.
The context may contain some extra information like `__parent__` context.
Also, the values of multi-value fields (O2M/M2M) are marked with `selected` flag
to check whether the record is selected.

These extra attributes are available:

- `_viewName` - name of the current view
- `_viewType` - type of the current view
- `_views` - all the views defined by current action-view
- `_source` - name of the action source (field name)

---

## Expressions

The xml actions uses dynamic expressions to access object values or conditional
test expressions to enable/disable actions.

An expression is either a:

- groovy expression - prefixed with `eval:`
- select expression - JPQL select queries, prefixed with `select:`
- action expression - another action, prefixed with `action:`
- call expression - call a method, prefixed with `call:`
- constant expression - the expression is considered as constant value

These expressions can be defined in [groovy](http://www.groovy-lang.org/) or [Java EL](https://docs.oracle.com/javaee/7/tutorial/jsf-el.htm)
syntax. The [Java EL](https://docs.oracle.com/javaee/7/tutorial/jsf-el.htm) support has been added in v4.0.

We can mix both expression syntax in same action definition. The rules are:

1. the `eval:` expressions are considered groovy expressions
2. the `select:` expressions are considered Java EL expression
3. expression wrapped inside `#{...}` are considered Java EL expression

The rules in details:

- `expr="eval: ..."` - use groovy
- `expr="select: ..."` - use EL
- `expr="#{...}"` - use EL
- `expr="..."` - return as it is (constant)
- `search="..."` - search expression on `action-record` are handled with EL
- `if="#{...}"` - use EL
- `if="eval: ..."` - use groovy
- `if="select: ..."` - use EL
- `if="..."` - use groovy

### Helper functions

The following helper functions are available in all supported scripting languages:

- `__repo__(modelClass)` - Get a repository for a given model class.
- `__bean__(beanClass)` - Get a bean instance of a given type.

Additionally, the following are available in EL:

- `is(Object, Class)` - check whether the given object is instance of the class
- `as(Object, Class)` - cast the object as the given class (generally, casting is automatic in EL but useful to convert context to a model class)
- `str(Object)` - convert object to string (if null, returns empty string "")
- `imp(String)` - import a class by name
- `fmt:text(String, Object...)` - string format helper

Some examples:

```xml
<action-attrs name="action-test">
  <attribute ... if="code == 'some'" expr="eval: __self__?.customer?.fullName" /> <!-- 1 -->
  <attribute ... if="#{code == 'some'}" expr="#{ __self__.customer.fullName }" /> <!-- 2 -->
  <attribute ... expr="call: com.axelor.contact.SomeController:method" /> <!-- 3 -->
  <attribute ... expr="select: s.fullName from Contact s where s.code = :code" /> <!-- 4 -->
</action-attrs>
```

1. standard groovy expressions
2. Java EL expression needs to be wrapped inside `#{...}`, also no need of null value check
3. `call:` expressions are handled with JavaEL
4. `select:` expressions are handled with JavaEL

We are using Java EL 3.0 (from tomcat8). See the [Java EL](https://docs.oracle.com/javaee/7/tutorial/jsf-el.htm)
documentation for more details.

---

## Built-ins variables

Some built-in variables are available to be used with expressions. This includes:

- `__date__` - current date as `LocalDate`
- `__time__` - current datetime as `LocalDateTime`
- `__datetime__` - current datetime as `ZonedDateTime`
- `__user__` - current user
- `__this__` - the record being edited (representing form values)
- `__self__` - the corresponding record from the database
- `__parent__` - the parent record
- `__ref__` - the first selected record in multi-object search view
- `__id__` - ID of the current record
- `__ids__` - list of IDs of the selected records
- `__config__` - global context configuration

---

## Special actions

The following special actions can be used to perform some special operations:

- `save` - to save record, can be used anywhere
- `new` - start a new record, can be used at the end only
- `close` - close current view, can be used at the end only
- `validate` - validate current form, can be used anywhere

For example:

```xml
<form ...>
  ...
  <!-- save current form before executing some-action,
       and save again at the end -->
  <field name="some" onChange="save,some-action,another-action,save" />
  ...
  <!-- close current view after action is complete -->
  <field name="some" onChange="some-action,close" />
</form>
```

---

## Attrs Action

The `<action-attrs>` is used to define attrs action which is used to change
attributes of view items.

```xml
<action-attrs name="order.on-confirm">
  <attribute for="orderDate" name="readonly" expr="confirmed" /> <!-- 1 -->
  <attribute for="name" name="hidden" expr="!id" /> <!-- 2 -->
</action-attrs>
```

1. make the `orderDate` field readonly if order is `confirmed`
2. make the `name` field hidden if the order is not saved yet

| Name | Description |
|---|---|
| **name** | name of the action |

The attrs action requires following elements:

- `<attribute>` - specify the attribute to update
  - `for` - comma-separated list of target field names
  - `name` - the name of the attribute to change
  - `expr` - groovy expression to calculate attribute value
  - `if` - a groovy boolean expression against the current context

The following attributes can be changed:

- `required` - mark the field as required or not
- `readonly` - mark the field as readonly or not
- `hidden` - show/hide the field
- `domain` - change the domain filter of relational field
- `title` - change the title of the field
- `value` or `value:set` - the value to set
- `value:add` - add a new item to the multi-valued relational field
- `value:del` - remove an item from the multi-valued relational field value
- `collapse` - collapsed state of a panel
- `precision` - total number of digits of a decimal value
- `scale` - number of digits in decimal part of a decimal value
- `css` - CSS of an element
- `icon` - icon of an element
- `refresh` - refresh a dashlet
- `selection-in` - The filter on the selection
- `focus` - focus the field
- `active` - open the given panel if included in a panel-tabs
- `url` or `url:set` - change the iframe url in dashlet
- `prompt` - change prompt message of button
- `link` - change the link url of button

---

## Condition Action

The `<action-condition>` is used to check the validity of a field and show a
message under the field.

```xml
<action-condition name="check-order-dates">
  <check field="orderDate"/>
  <check field="createDate"/>
  <check field="createDate" if="orderDate &gt; createDate"
    error="Order creation date is in the future."/>
</action-condition>
```

The condition action requires following items:

- `<check>` - define a check
  - `field` - name of the field
  - `error` - error message
  - `if` - a boolean expression against the current context

If `if` is not specified, it checks if the field is null.

If `error` is not specified, a default message is displayed.

---

## Export Action

The `<action-export>` can be used to export records.

```xml
<action-export name="export.sale.order">
  <export name="${name}.xml"
    template="data/ws-test/export-sale-order.tmpl"
    engine="groovy"/>
  <export name="${name}-customer-copy.xml"
    template="data/ws-test/export-sale-order.tmpl"
    engine="groovy"/>
</action-export>
```

| Name | Description |
|---|---|
| **name** | name of the action |
| attachment | whether to attach the exported file to current record |

> **Note:** Export file will be directly downloaded by default.
> If `attachment` is set to `true`, the export file will first be attached to the current record,
> then the user will be prompted if they wish to download the attachement.

The export action requires one or more `<export>` tasks:

- `<export>` - specify an export task
  - `name` - output file name
  - `template` - the template to be used to generate output file
  - `engine` - the template engine to use (groovy, ST)

---

## Group Action

The `<action-group>` can be used to gather actions.

```xml
<action-group name="action-validate-invoice">
  <action name="act1"/>
  <action name="act2"/>
  <action name="act3" if="invoiceDate"/>
</action-group>
```

| Name | Description |
|---|---|
| **name** | name of the action |

The group action requires following items:

- `<action>` - define an action
  - `name` - name of the action
  - `if` - a boolean expression against the current context

---

## Import Action

The `<action-import>` can be used to perform data import from xml data stream.

```xml
<action-import name="data.import.1" config="ws-data/xml-config.xml">
  <import file="ws-data.xml" provider="ws.1" name="titles" />
</action-import>
```

| Name | Description |
|---|---|
| **name** | name of the action |
| **config** | path to data import config |

The import action requires one or more `<import>` tasks:

- `<import>` - specify an import task
  - `file` - input file name as given in the config file.
  - `provider` - the stream provider (reference to an `action-ws`)
  - `name` - put the data as the given name in the result map

---

## Method Action

The `<action-method>` can be used to call a controller method.

```xml
<action-method name="act.hello">
  <call class="com.axelor.contact.web.HelloController" method="say"/>
</action-method>
```

| Name | Description |
|---|---|
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

---

## Record Action

The `<action-record>` can be used to construct an object with some values.

```xml
<action-record name="default-order-record" model="com.axelor.sale.db.Order">
  <field name="customer" expr="action:default-customer-record" if="!_contact"/>
  <field name="customer" expr="eval: _contact" if="_contact"/>
  <field name="orderDate" expr="eval: __date__"/>
  <field name="createDate" expr="eval: __date__"/>
  <field name="items" expr="action:default-orderline-record"/>
</action-record>
```

| Name | Description |
|---|---|
| **name** | name of the action |
| **model** | the domain model to construct the object of |
| search | search for existing record before creating new |
| ref | reference to the existing record from context, gets preference over `search` |
| copy | if record is found, whether to create a copy of it |
| saveIf | save if the given expression is true and `id` is `null` or `version` value is provided. |

The action required `<field>` items to set object properties.

- `<field>` - define a field to update
  - `name` - name of the field
  - `expr` - expression to execute to get the value
  - `if` - a groovy boolean expression against the current context
  - `copy` - if the expression return a model object, whether to copy it

The `expr` has following format:

- `eval: ...` - evaluate as groovy expression
- `call: ...` - call a given controller method
- `action: ...` - call a given action
- `select: ...` - execute a select query and return first matched record
- `select[]: ...` - execute a select query and return all matched records
- `...` - if none of the above, consider the expression as static value

---

## Script Action

The `<action-script>`, introduced in v5, can be used to create complex actions using scripting languages.

```xml
<action-script
  name="create.invoice" <!-- 1 -->
  model="com.axelor.sale.db.Order" <!-- 2 -->
  >
  <script
    language="js" <!-- 3 -->
    transactional="true" <!-- 4 -->
    >
  <![CDATA[
  var req = $request; <!-- 5 -->
  var res = $response; <!-- 6 -->
  var so = req.context;
  var invoice = new Invoice();
  invoice.date = so.confirmDate;
  // prepare invoice lines from sale order
  //TODO: invoice.invoiceLines = listOf(...);

  // if you want to save invoice
  //invoice.saleOrder = em.find(Order.class, so.id);
  //return $em.persist(invoice);

  res.setValue('invoice', invoice);
  res.setReadonly('customer', true);
  // and so on...
  ]]>
  </script>
</action-script>
```

1. the name of the action (required)
2. the name of the context model
3. scripting language to use (required, currently `js` and `groovy` only)
4. whether the code is transactional
5. the `ActionRequest` is available as `$request`
6. the `ActionResponse` is available as `$response`

The `action-script` is nothing but the controller method
dynamically created using a scripting language. The `$request` and `$response` variables are nothing
but the `ActionRequest` and `ActionResponse` parameters of controller method.

Following variables are available in script execution context:

| Name | Description |
|---|---|
| `$request` | the `ActionRequest` |
| `$response` | the `ActionResponse` |
| `$em` | the `EntityManager` if script is `transactional` |
| `$json` | instance of `MetaJsonRecordRepository` to work with custom models |

The `action-script` can be used for custom models too. Here is an example:

```xml
<action-script name="create.hello" model="com.axelor.meta.db.MetaJsonRecord">
  <script language="js" transactional="true">
  <![CDATA[
    var hello = $json.create('hello'); <!-- 1 -->
    hello.name = "Hello!!!";           <!-- 2 -->

    var world = $json.all('world').by('name', '=', 'World!!!').fetchOne(); <!-- 3 -->
    if (world == null) {
        world = $json.create('world');
        world.name = "World!!!";
        world = $json.save(world); <!-- 4 -->
        // now we can't update world, as it's converted to real instance
    }

    hello.world = world;  <!-- 5 -->

    // return as response values
    $response.values = hello;

  ]]>
  </script>
</action-script>
```

1. create a new empty context for `MetaJsonRecord` for the `hello` model
2. the context allows seamless access to custom field values
3. find a `world` model record by field `name`
4. record(s) intended for relational field values must be saved manually
5. set relational value (m2o)

---

## Validate Action

The `<action-validate>` is used to validate a record. It will display a dialog or a brief notification.

```xml
<action-validate name="action-sale-order-validate">
  <error message="Create Date is in future." if="confirmed &amp;&amp; createDate &gt; __date__"/>
  <error message="Order Date is in future." if="confirmed &amp;&amp; orderDate &gt; __date__"/>
  <alert message="No Sale Order Items. Would you like to continue?" if="confirmed &amp;&amp; !items"/>
</action-validate>
```

| Name | Description |
|---|---|
| **name** | name of the action |

The validate action requires following items:

- `<error>` - define an error condition
  - `if` - a boolean expression against the current context
  - `message` - the message to show if condition failed
  - `action` - an action to be executed to make corrective measures
  - `title` - title of the modal/notification, default to `Error`
  - `confirm-btn-title` - title of the confirm button, default to `Ok`
- `<alert>` - define an alert condition
  - `if` - a boolean expression against the current context
  - `message` - the message to show if condition failed
  - `action` - an action to be executed to make corrective measures
  - `title` - title of the modal/notification, default to `Warning`
  - `confirm-btn-title` - title of the confirm button, default to `Ok`
  - `cancel-btn-title` - title of the cancel button, default to `Cancel`
- `<info>` - define an information condition
  - `if` - a boolean expression against the current context
  - `message` - the message to show if condition failed
  - `title` - title of the modal/notification, default to `Information`
  - `confirm-btn-title` - title of the confirm button, default to `Ok`
- `<notify>` - define a notification condition
  - `if` - a boolean expression against the current context
  - `message` - the message to show if condition failed

If the action results in an `error`, further action processing is terminated,
and an error message is shown to the user.
If provided, error action is executed to make corrective measures when error dialog is closed.

If the action results in an `alert`, further action processing is halted,
and a confirmation message is shown to the user.
If confirmed, the pending actions are executed.
If provided, alert action is executed to make corrective measures when alert dialog is canceled.

If the action results in an `info`, a message is shown to the user,
then pending actions are executed when info dialog is closed.

If the action results in a `notify`, pending actions will be executed and a
brief notification popup is shown at the bottom right corner.
The notification disappears automatically after 5 seconds if not manually closed.

---

## Actions & Menus

The view action is used to open object views. The view actions are used to
define application menu.

### View Action

The `<action-view>` is used to define the action views.

| Name | Description |
|---|---|
| **name** | name of the action |
| title | override the view title |
| model | fully qualified name of the model object |
| icon | icon displayed on top-level navigation tab |

The action view requires the following elements:

- `<view>` - specify the view to use
  - `type` - the view type
  - `name` - the view name
- `<view-param>` - define additional view parameter
  - `name` - parameter name
  - `value` - parameter value
- `<domain>` - specify a domain filter to restrict search (jpql where clause)
- `<context>` - define the base context for the action
  - `name` - context variable name
  - `expr` - context variable value expression

The `<view-param>` parameter name accepts the following options:

- `forceEdit` - `true` to force to open in editable mode
- `forceTitle` - `true` to force to use action title instead of view title
- `showArchived` - `true` to include archived records
- `details-view` - `true` to show grid and form views side by side
- `search-filters` - name of custom search filters
- `default-search-filters` - comma-separated list of search filter names to apply by default (used in conjunction with `search-filters`)
- `limit` - maximum number of records per page (grid/cards view) or per column (kanban view)
- `popup` - `true` to open view as popup, `reload` to reload parent upon closing the popup ([details](#popup-reload))
- `popup-save` - `false` to hide OK button used to save record upon closing the popup
- `popup.maximized` - `true` to open as maximized popup
- `popup.show-header` - `false` to hide popup header
- `popup.show-footer` - `false` to hide popup footer
- `show-toolbar` - `false` to hide form toolbar in grid view and from view
- `show-toolbar-grid` - `false` to hide form toolbar only in grid view
- `show-toolbar-form` - `false` to hide form toolbar only in form view
- `show-confirm` - `false` to disable dirty check
- `reload-dotted` - `true` to refresh the grid when switching back from form view
- `download` - `true` to mark view as pointing to a downloadable link
- `kanban-hide-columns` - hide specific columns in kanban (comma separated list of names)
- `kanban-column-width` - desired kanban column width
- `auto-reload` - enable view auto-reloading with value specified in seconds
- `hideActions` - `true` to hide the actions in the search view
- `target` - in case of html view, it opens the link in a new tab. `_blank` opens the url in new browser tab.

<a id="popup-reload"></a>

> **Note:**
> When `<view-param>` `popup` is set to `reload` on a dashlet,
> there is a save confirmation dialog before opening the popup.
> The parent record is then reloaded if the popup is closed via the OK button or close action.

Special context variable names

- `_showRecord` - show the record by given id in form view
- `_showSingle` - `true` to show the only record in form view
- `__check_version`- `true` to check record version in form view when tab is active

Examples:

```xml
<action-view name="contact.all" title="Contacts"
  model="com.axelor.contact.db.Contact"> <!-- 1 -->
  <view type="grid" name="contact-grid"/> <!-- 2 -->
  <view type="form" name="contact-form"/> <!-- 3 -->
</action-view>

<action-view name="contact.friends" title="My Friends"
  model="com.axelor.contact.db.Contact">
  <view type="grid" name="contact-grid"/>
  <view type="form" name="contact-form"/>
  <domain>self.circle.code = :circleCode</domain> <!-- 4 -->
  <context name="circleCode" expr="friend"/> <!-- 5 -->
</action-view>
```

1. define an `action-view` for the given object
2. use the `contact-grid` view defined for the `grid` view
3. use the `contact-form` view defined for the `form` view
4. define a domain filter (jpql where clause)
5. define a context variable

You can see the `<domain>` filter uses named parameters. These parameters are
evaluated against the context.

### Application Menu

In order to access object views, we need application menu. The menu is also
defined using xml syntax along with views & view actions.

The `<menuitem>` is used to define a menu item. The application menu is
hierarchical so menu items can be organized as parent child.

| Name | Description |
|---|---|
| **name** | name of the menu item |
| parent | name of the parent menu item |
| **title** | display title |
| icon | display icon name |
| icon-background | icon background color (predefined or html hex color) |
| action | the action to be executed on menu item click |
| order | menu item display order sequence |
| groups | comma-separated list of user groups who can see this menu item |
| top | whether to show this menu on top |
| left | whether to show this menu on left |
| hidden | whether to hide this menu |
| tag | specify a tag to show on menu item |
| tag-count | specify whether to use count of menu action records as tag |
| tag-get | specify a method call to get tag value |
| tag-style | specify the tag display style |

example:

```xml
<menuitem name="menu-contact-book"
  title="Address Book" /> <!-- 1 -->

<menuitem name="menu-contact-friends"
  parent="menu-contact-book"
  title="All Contact"
  action="contact.all"/> <!-- 2 --> <!-- 3 --> <!-- 4 -->

<menuitem name="menu-mail-inbox"
  parent="menu-mail"
  title="Inbox"
  action="mail.inbox"
  tag-get="com.axelor.mail.web.MailController:inboxMenuTag()"
  tag-style="warning"/> <!-- 5 -->

  <menuitem name="menu-mail-important"
    parent="menu-mail"
    title="Important"
    action="mail.important"
    tag="Important"
    tag-style="important"/> <!-- 6 -->
```

1. define a top-level menu with no parent
2. define a child menu item with parent
3. the display text of the menu item
4. the action (of type action-view) to execute
5. the get tag value from the given method
6. user the given static tag

The `tag-style` can be one of the:

- `default`
- `important`
- `success`
- `warning`
- `inverse`
- `info`

The menus are displayed to users with the following rules:

- Don't allow access to root menus by default: top menus are restricted by default, so roles/groups are needed in order to be displayed to users.
- Allow access to all non-root menus by default: if submenus have no roles nor groups assigned, they are available to all. Or else, submenus are displayed to users belonging to the given groups/roles.

---

## WebService Action

The `<action-ws>` can be used to call a SOAP web services. This action is
generally used as a `provider` action to the `<action-import>`.

```xml
<action-ws name="ws.1" service="http://localhost/ws/soap/SomeService.asmx">
  <action name="SoapServiceName" template="data/ws/ws-login.tmpl" engine="groovy"/>
</action-ws>
```

| Name | Description |
|---|---|
| **name** | name of the action |
| service | service url or reference to another `action-ws` with service is set to some url. In that case, the referenced action is called prior to this one. This allows to perform some initial actions like `login`. |
| connect-timeout | connection timeout in seconds (default 60 seconds) |
| read-timeout | read timeout in seconds (default 300 seconds). |

More than one SOAP action can be called in sequence. The result is returned as
a collection string values returned by each action respectively.

The SOAP actions can be specified using:

- `<action>` - specify a soap action to call
  - `name` - name of the SOAP action
  - `template` - a template to transform the result to another format
  - `engine` - template engine (groovy, ST)

