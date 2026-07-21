# Action (Acciones)

Las vistas (`form`, `grid`, `cards`, etc.) por sí solas solo muestran datos. Las acciones son lo que les da comportamiento real: validan, cambian atributos, ejecutan lógica, abren otras vistas y controlan la navegación.

## Tipos de acciones en Axelor

Las acciones más importantes y habituales son:

- `<action-view>`: abre una o varias vistas (`grid` y `form`) para un modelo.
- `<action-method>`: llama a un método Java de controlador (`@CallMethod`).
- `<action-attrs>`: cambia atributos de campos en tiempo real (`readonly`, `hidden`, `required`, `domain`, `value`, etc.).
- `<action-record>`: construye o completa un registro con valores por defecto o calculados.
- `<action-group>`: agrupa varias acciones y las ejecuta en secuencia.
- `<action-validate>`: lanza validaciones y mensajes generales (`error`, `alert`, `info`, `notify`).
- `<action-condition>`: valida condiciones sobre campos concretos y muestra errores bajo el campo.
- `<action-script>`: ejecuta lógica compleja mediante script (`js` o `groovy`).

### `<action-view>`

Acción para ser llamada desde los menús. Permite hacer un mantenimiento mostrando un grid y/o un formulario para un modelo.
Se pueden añadir parámetros para mostrar u ocultar toolbars, forzar edición, recargar el grid al guardar, etc.

```xml
<action-view name="subsysFirma.Pendiente@TareaFirma-action" title="Documentos pendientes de firma" model="com.educaflow.subsystem.firmas.db.TareaFirma">
    <view type="grid" name="subsysFirma.Pendiente@TareaFirma-grid"/>
    <view type="form" name="subsysFirma.Pendiente@TareaFirma-form"/>
    <view-param name="show-toolbar-grid" value="false"/>
    <view-param name="show-toolbar-form" value="false"/>
    <view-param name="forceEdit" value="true"/>
    <view-param name="reload-grid" value="true"/>
    <domain>self.estadoTareaFirma= :estadoTareaFirma and firmante.id= :firmanteId</domain>
    <context name="estadoTareaFirma" expr="PENDIENTE"/>
    <context name="firmanteId" expr="eval:__user__.id"/>
</action-view>
```

- Para indicar el grid a mostrar, indicar el atributo `<view type="grid" name="Nombre del grid"/>`
- Para indicar el formulario a mostrar, indicar el atributo `<view type="form" name="Nombre del formulario"/>`
- En caso de que sea un grid que llama a un form, es **obligatorio** que vaya primero el grid y después el formulario (form)
- Si el grid no va a tener el botón de nuevo, añadir `<view-param name="show-toolbar-grid" value="false"/>` para ocultar la toolbar del grid y evitar confusión al usuario
- Siempre añadir `<view-param name="show-toolbar-form" value="false"/>` para ocultar la toolbar del formulario
- Añadir `<view-param name="forceEdit" value="true"/>` para permitir que el formulario se abra en modo edición, para ello usar en el grid `canEditOnClick="true"` para que al hacer click en la fila se abra el formulario directamente en edición
- Para filtrar los registros que se muestran en el grid, usar el tag `<domain>` con la sintaxis de dominios de Axelor y pasar los parámetros necesarios con `<context name="param" expr="valor"/>`

### `<action-method>`

Llamada a controlador en Java

> **Validación remota de save/delete**: **MUST NOT** crear un `<action-method>` de validación por entidad para guardar/borrar — existen las acciones **globales** `remote-validationSave-action` y `remote-validationDelete-action`, definidas una única vez en `DefaultModelController.xml` (`base/infrastructure/controller`); no llevan atributo `model` porque resuelven la entidad por el `_model` del contexto. Los `<action-method>` propios de una entidad son para **operaciones custom**. Ver `k-validaciones/validaciones.md` §5.

```xml
<action-method name="subsysFirma.Pendiente@TareaFirma-Remote-marcarComoFirmada-action" model="com.educaflow.subsystem.firmas.db.TareaFirma">
    <call class="com.educaflow.subsystem.firmas.controller.TareaFirmaController" method="marcarComoFirmada" />
</action-method>
```

- El atributo `model` es opcional pero recomendable para facilitar la trazabilidad y el mantenimiento. Si se pone, se recomienda que sea el modelo principal sobre el que actúa la acción.
- El atributo `class` se refiere a la clase Java del controlador donde se encuentra el método a llamar, y el atributo `method` al método concreto que se quiere ejecutar. Ese método Java debe estar anotado con `@CallMethod` para que pueda ser llamado desde la acción.
- En este caso el método Java tendrá como argumentos `ActionRequest actionRequest, ActionResponse actionResponse`

```java
   @CallMethod
   public void marcarComoFirmada(ActionRequest actionRequest, ActionResponse actionResponse) {
  
   }
```

```xml
<action-method name="subsysSistemaEducativo.Main@LeyEducativa-Remote-enviarCorreo-action" model="com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa">
    <call class="com.educaflow.subsystem.sistemaeducativo.controller.LeyEducativaController" method="enviarCorreo(id,nombre)" />
</action-method>
```

- En este otro ejemplo, el método Java `enviarCorreo` recibe como argumento el id y el nombre del registro actual. Para ello se usa la sintaxis `method="enviarCorreo(id, nombre)"` y el método Java entrará como argumentos `Long id` y `String nombre` :

```java
   public void enviarCorreo(Long id,String nombre) {
  
   }
```

### `<action-attrs>`

Asignar valores a atributos de campos en función de expresiones. Por ejemplo, para hacer un campo de solo lectura cuando el registro esté confirmado:

```xml
<action-attrs name="subsysSistemaEducativo.Main@LeyEducativa-set-orderDate.readonly-confirmed-action">
  <attribute for="orderDate" name="readonly" expr="true" />
</action-attrs>
```

- El atributo `for` indica el nombre del campo al que se le van a cambiar los atributos.
- El atributo `name` indica el atributo que se va a cambiar, en este caso `readonly`.
- El atributo `expr` indica la expresión que se va a evaluar para cambiar el atributo, en este caso el campo `orderDate` se volverá de solo lectura cuando el campo `confirmed` sea `true`.

### `<action-record>`

Asignar un valor a un campo

```xml
<action-record name="subsysFirma.Pendiente@TareaFirma-set-pasoActual-paso1Inicio-action" model="com.educaflow.subsystem.firmas.db.TareaFirma">
    <field name="pasoActual" expr="paso1Inicio" />
</action-record>
```

- El atributo `name` del `field` es el campo al que se le va a asignar el valor.
- El atributo `expr` del `field` es el valor que se le va a asignar al campo, en este caso el valor literal `paso1Inicio`.

```xml
<action-record name="subsysSistemaEducativo.Main@Ciclo.Curso-set-ciclo-parent-action" model="com.educaflow.subsystem.sistemaeducativo.db.Curso">
    <field name="ciclo" expr="eval: __parent__"/>
</action-record>

```
- En este otro ejemplo, el campo `ciclo` del curso se le asigna el valor del ciclo padre usando la expresión `eval: __parent__` que hace referencia al registro padre en la vista anidada.

### `<action-group>` — secuencia de acciones principales

```xml
<action-group name="subsysSistemaEducativo.Main@LeyEducativa-btnSave-action">
    <action name="subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-action"/>
    <action name="remote-validationSave-action"/>
    <action name="save"/>
    <action name="back"/>
</action-group>
```

- Simplemente se listan las acciones a ejecutar en orden. En este caso, primero se ejecuta la acción de validación local (`subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-action`) y si pasa sin errores, se ejecuta la validación remota con la acción global `remote-validationSave-action` (los `validate*` del servicio, vía `DefaultModelController`), se ejecuta la accion `save` y finalmente `back` para cerrar la ventana (**obligatorio** tras `save`: si no hubo cambios, `save` es un no-op y `canBackOnSave` no cierra; el `back` explícito sí — puede ser `force-back`). En el `btnDelete` la acción global equivalente es `remote-validationDelete-action` antes de `delete`. Esto aplica al form **principal**: en el form **modal** de un detalle (`save-modal`/`delete-modal`) **MUST NOT** usarse las acciones `remote-validation*` y la validación local debe ser lo más completa posible — ver `[[forms.md]]` §"Form modal".

Se usan estas acciones desde eventos como `onClick` de botones, `onSave` de formularios, `onChange` de campos, etc. para ejecutar una secuencia de acciones en un solo evento.

Para las **operaciones custom**: si el grupo invoca una acción `Remote-{Op}-action` y existe su validación `Remote-validate{Op}-action` (mismo contexto), esta **MUST** ir **inmediatamente antes** de la operación, sin acciones intercaladas:

```xml
<action-group name="subsysCorreos.Main@Correo-btnReenviar-action">
    <action name="subsysCorreos.Main@Correo-Remote-validateReenviar-action"/>
    <action name="subsysCorreos.Main@Correo-Remote-reenviar-action"/>
</action-group>
```

### `<action-validate>`

Validaciones locales a nivel de todo el formulario, que no dependen de un campo concreto. Por ejemplo, para validar que el nombre y el código no sean "dd":

```xml
<action-validate name="subsysVentas.Main@Producto-Local-validateSave-action">
    <error message="Ni el nombre ni el código pueden ser dd" if="name=='dd' || code=='dd'"/>
    <error message="Si el estado está cancelado no es posible que el precio sea mayor que cero" if="state='CANCELADO' && precio>0"/>
</action-validate>
```

Mira en `references/actions.md` para sintaxis completa de validaciones porque hay validaciones de tipo error, alert, info y notify

### `<action-condition>`

Validaciones a nivel de campo concreto, que muestran el mensaje de error justo debajo del campo. Por ejemplo, Validar el campo `createDate` de forma que si `orderDate > createDate` se muestre el error "Order creation date is in the future."

```xml
<action-condition name="subsysFirma.Pendiente@TareaFirma-Local-validateMarcarComoRechazada-action">
  <check field="createDate" if="orderDate > createDate" error="Order creation date is in the future."/>
</action-condition>
```

- El atributo `field` indica el campo al que se le va a asociar la validación.
- El atributo `if` indica la expresión que se va a evaluar para validar el campo. Si la expresión es `true`, se muestra el mensaje de error.
- El atributo `error` indica el mensaje de error que se va a mostrar debajo del campo si la validación falla.
- Si no se indica el atributo `if` se comprueba que no esté vacío.

### Patrón: `Local-validateXXX-action` directo o como `<action-group>`

`<action-validate>` y `<action-condition>` son elementos XML distintos con gramáticas distintas: **no se pueden mezclar en el mismo elemento**.

- `<action-validate>` solo admite hijos `<error>`/`<alert>`/`<info>`/`<notify>` — mensajes a nivel de formulario.
- `<action-condition>` solo admite hijos `<check>` — errores pegados a un campo concreto.

Por eso, un nombre como `subsysXxx.Main@MiEntidad-Local-validateSave-action` se materializa de tres formas distintas según qué tipos de validación cliente necesite la entidad para ese evento:

| Tipos de validación que se necesitan         | Forma de `Local-validateSave-action`                                      |
|----------------------------------------------|---------------------------------------------------------------------------|
| Solo errores generales del formulario        | Un único `<action-validate>` con ese nombre                               |
| Solo errores pegados a campos concretos      | Un único `<action-condition>` con ese nombre                              |
| Se necesitan **los dos**                     | Un `<action-group>` con ese nombre que encadena los dos con sub-nombres   |

El `<action-group>` del botón (`Main-btnSave-action`) referencia siempre el mismo nombre `Main-Local-validateSave-action` — qué hay detrás (un único elemento o un grupo) es interno y puede cambiar sin tocar el botón.

**Caso 1 — Solo un tipo (un único elemento):**

```xml
<action-condition name="subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-action">
    <check field="name" if="name == null" error="El nombre es obligatorio"/>
    <check field="code" if="code == null" error="El código es obligatorio"/>
</action-condition>
```

**Caso 2 — Hace falta combinar `<action-validate>` con `<action-condition>`:**

`Local-validateSave-action` pasa a ser un `<action-group>`. Los componentes internos extienden el nombre con un sufijo que describe **qué validan** (no el tipo XML que usan):

```xml
<!-- El nombre genérico ahora es un action-group -->
<action-group name="subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-action">
    <action name="subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-requiredFields-action"/>
    <action name="subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-codeFormat-action"/>
</action-group>

<!-- Errores pegados a cada campo -->
<action-condition name="subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-requiredFields-action">
    <check field="name" if="name == null" error="El nombre es obligatorio"/>
    <check field="code" if="code == null" error="El código es obligatorio"/>
</action-condition>

<!-- Errores generales del formulario -->
<action-validate name="subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-codeFormat-action">
    <error message="El código no puede empezar por número"
           if="code != null &amp;&amp; code ==~ /^[0-9].*/"/>
    <alert message="El código contiene caracteres especiales. ¿Continuar?"
           if="code != null &amp;&amp; code ==~ /.*[^a-zA-Z0-9].*/"/>
</action-validate>
```

El botón Guardar no cambia entre el Caso 1 y el Caso 2:

```xml
<action-group name="subsysSistemaEducativo.Main@LeyEducativa-btnSave-action">
    <action name="subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-action"/>
    <action name="remote-validationSave-action"/>
    <action name="save"/>
    <action name="back"/>
</action-group>
```

**Convención para los sub-nombres del Caso 2:**

Los sub-nombres siguen el patrón `Local-{validacion}-{subPropósito}-action`, donde `{subPropósito}` describe **qué se valida** en cada bloque, no el tipo XML utilizado. Ejemplos válidos:

- `Local-validateSave-requiredFields-action` (campos obligatorios)
- `Local-validateSave-codeFormat-action` (formato del código)
- `Local-validateSave-dateRange-action` (consistencia de fechas)
- `Local-validateSave-amountLimit-action` (límites de importe)
- `Local-validateSave-businessRules-action` (reglas cruzadas)

Evitar sub-nombres que repitan el tipo XML (`-condition`, `-validate`) — no aportan información sobre qué hace cada bloque.

### `<action-script>`

Permite ejecutar acciones complejas mediante un script en `js` o `groovy`. Se utilizan en vez de crear un controlador en Java.

```xml
<action-script name="subsysVentas.Pendiente@Factura-Remote-guardarFactura-action" model="com.axelor.sale.db.Order" >
  <script
          language="js"
          transactional="true"
  >
    <![CDATA[
  var req = $request; 
  var res = $response; 
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

- El atributo `language` indica el lenguaje del script, que puede ser `js` para JavaScript o `groovy` para Groovy.
- El atributo `transactional` indica si el script se ejecuta dentro de una transacción. Si es `true`, cualquier operación de base de datos realizada en el script se ejecutará dentro de una transacción y se revertirá si ocurre un error.
- Dentro del script, se pueden acceder a la solicitud y respuesta mediante las variables `$request` y `$response`, y al modelo actual mediante `req.context`. Y mediante `$em` se puede acceder al EntityManager para realizar operaciones de base de datos.
- Se pueden realizar operaciones complejas, como crear registros, modificar campos, llamar a servicios, etc.

## Convenciones de nombres para las acciones

```
{marcadorMódulo}.{Variante}@{Entidad}[.{Entidad}]*-[{evento}|Local-{nombreValidacion}|Remote-{nombreFuncionJava}|set-{asignacion}]-action
```


| Parte                            | Descripción                                                                     | Ejemplo                                         |
|----------------------------------|---------------------------------------------------------------------------------|-------------------------------------------------|
| `{marcadorMódulo}`                      | `subsys{Subsistema}` para subsistemas, `sys{Sistema}` para sistemas             | `subsysSistemaEducativo`, `subsysFirma`         |
| `{Entidad}`                      | Nombre exacto de la clase Java                                                  | `LeyEducativa`, `TareaFirma`                    |
| `{Variante}@`                       | Variante: para qué sirve el bloque                                             | `Main@…`, `Pendiente@…`, `Firmado@…`               |
| `{evento}`                       | Sin marcador: evento o botón para `action-group` (`on{Evento}` o `btn{Nombre}`)  | `onLoad`, `btnSave`                             |
| `Local-{nombreValidacion}`       | Validación cliente: `action-validate`, `action-condition` o `action-group`      | `Local-validateSave`                            |
| `Remote-{nombreFuncionJava}`     | Llamada a función Java en el servidor (`action-method` o `action-script`)       | `Remote-marcarComoFirmada`                      |
| `set-{campo}-{valor}`            | Asigna un valor a un campo (`<action-record>`)                                  | `set-centro-null`                               |
| `set-{campo}.{atributo}-{valor}` | Modifica un atributo de un campo (`<action-attrs>`)                             | `set-apellidos.readonly-true`                   |
| `-action`                        | Terminación fija siempre al final                                               |                                                 |

### Ejemplos de nombres

- **`action-view`**
  `subsysSistemaEducativo.Main@LeyEducativa-action`
  `subsysFirma.Pendiente@TareaFirma.DocumentoFirmado-action`
- **`action-group`** — orquestador público, tiene el nombre del evento que ocurre (onSave, onNew, onLoad, etc.) o del botón que lo dispara (btnSave, btnCancel, etc.):
  `subsysSistemaEducativo.Main@LeyEducativa-btnSave-action`
  `subsysSistemaEducativo.Main@LeyEducativa-onNew-action`
  `subsysSistemaEducativo.Main@LeyEducativa-btnCancel-action`
  `subsysSistemaEducativo.Main@LeyEducativa-onLoad-action`
- **`action-validate` o `action-condition`** — siempre con marcador `Local-` ya que son validaciones que se hacen en el cliente sin llamada al servidor
  `subsysSistemaEducativo.Main@LeyEducativa-Local-validateSave-action`
- **`action-method`** — siempre con marcador `Remote-` ya que son llamadas a métodos Java en el servidor
  `subsysFirma.Pendiente@TareaFirma-Remote-marcarComoFirmada-action`
- **`action-script`** — siempre con marcador `Remote-` ya que son scripts Groovy ejecutados en el servidor
  `subsysSistemaEducativo.Main@LeyEducativa-Remote-insertarFactura-action`
- **`action-record`** — describe campo y valor con `set-{campo}-{valor}`:
  `subsysFirma.Pendiente@TareaFirma-set-nombre-Juan-action`
- **`action-attrs`** — describe campo y valor con `set-{campo}.{atributo}-{valor}`:
  `subsysFirma.Pendiente@TareaFirma-set-nombre.readonly-true-action`

**IMPORTANTE: Es obligatorio seguir esta convención de nombres para facilitar la trazabilidad, la lectura y el mantenimiento del código.**

> Excepción: las acciones **globales** de la plataforma (`remote-validationSave-action`, `remote-validationDelete-action`) no siguen esta convención porque no pertenecen a ninguna entidad — se definen una única vez en `DefaultModelController.xml` y se referencian tal cual.


## Eventos habituales donde se usan
Estas acciones se suelen disparar desde eventos de vista:

- `onNew`, `onLoad`, `onSave`, `onChange`, `onSelect`, `onClick`

Estos eventos deben ogliatoriamente referenciar a acciones de tipo `<action-group>` que agrupen la secuencia de acciones a ejecutar. No se deben llamar acciones individuales directamente desde los eventos, sino siempre a través de un `<action-group>`.
Si algunos de estos eventos llama a una accion individual directamente, crear una acción de tipo `<action-group>` que tenga como única acción esa acción individual y referenciar a ese `<action-group>` desde el evento.
La única excepción es que antes de la acción de grupo haya que ejecutar una acción remota relacionada con AutoFirma y en ese caso se pondrá `serial:accion-remota-autofirma,accion-de-grupo` para que se ejecuten en secuencia. Esto se hace así porque firmar con AutoFirma es algo externo a las acciones de Axelor

## Acciones predefinidas del framework de Axelor
Además de las acciones definidas por el desarrollador, el framework de Axelor tiene una serie de acciones predefinidas que se pueden usar directamente sin necesidad de definirlas. Estas acciones predefinidas son:
- `save`: guarda el registro actual.
- `validate`: ejecuta las validaciones definidas en el formulario.
- `close`: cierra la vista actual.
- `back`: navega a la vista anterior.
- `force-back`: navega a la vista anterior sin ejecutar las validaciones.
- `delete`: elimina el registro actual sin mostrar un modal de confirmación.
- `delete-modal`: en el form modal de un `<panel-related>`, pide confirmación y quita el registro de la colección en memoria del form padre (solo en cliente; el borrado en BD llega al guardar el maestro).
- `save-modal`: en el form modal de un `<panel-related>`, confirma el registro en la colección en memoria del form padre y cierra el modal (solo en cliente; no llama al servidor).
- `new`: crea un nuevo registro.

`save`/`delete` (form principal) y `save-modal`/`delete-modal` (form modal de entidades hijas en `<panel-related>`) son las únicas formas correctas de persistir y borrar registros desde el cliente, pero funcionan de forma distinta:

- `save`/`delete` disparan el endpoint REST automático `/ws/rest/<FQN>` del modelo del form, que entra al `ModelService` de esa entidad aplicando `validate` y `AllowProperties`.
- `save-modal`/`delete-modal` son acciones **solo de cliente**: confirman o quitan el registro en la colección en memoria del form padre y cierran el modal, sin llamar al servidor. La persistencia real de los detalles ocurre cuando el `save` del form raíz envía el árbol completo al endpoint REST **del maestro**: la persistencia y las reglas de negocio son las del `ModelService` del maestro (los `insert`/`update` y `fireActionRule_*` del detalle no se invocan por esta vía; los `validate*` del detalle **SÍ** — `ModelServiceValidationWalker` los ejecuta recursivamente al guardar el maestro), los detalles se persisten por cascada JPA y el filtrado `AllowProperties` que cubre el árbol anidado es el del maestro.

**MUST NOT** sustituirlas por un `<action-method>` (`Remote-…-action`) que llame a un controlador propio para guardar o borrar. Ver `[[controladores.md]]` del skill `k-sistemas`.


## Orden de las acciones en el código:

El orden de los elementos dentro de cada bloque es importante para facilitar la lectura y el mantenimiento y es el siguiente (el `<menuitem>` que abre el `<action-view>` **no** va aquí: vive en el fichero único `menus.xml`, ver `menus.md`):

1. La acción de tipo `<action-view>` que abre las vistas
2. El grid `<grid>`
3. El formulario `<form>`
4. Las acciones de las tareas principales (`<action-group>`) que suelen ser las tareas principales que se disparan desde botones o eventos importantes como `onNew`
5. Las acciones de validación en local (`<action-validate>` y `<action-condition>`)
6. Las acciones básicas que cambian campos simples (`<action-record>` y `<action-attrs>`)
7. Las acciones de llamadas remotas al servidor (`<action-method>` y `<action-script>`)


Es obligatorio respetar este orden para facilitar la lectura y el mantenimiento del código, ya que las acciones suelen estar relacionadas entre sí y es importante que estén agrupadas de forma lógica.

### Marcadores para separar cada sección de acciones
Es **obligatorio** delimitar cada sección de acciones con su **Processing Instruction** (PI), no con comentarios. Formato completo y reglas en `SKILL.md` § *Marcadores de bloque y sección (Processing Instructions)*.

- ✅ CORRECTO: `<?sv-primary-actions?>` (antes de los `action-group` de botón/evento)
- ❌ INCORRECTO: `<!-- *************** Ciclo : Acciones de las tareas principales *************** -->` (banner de comentario: prohibido)

Las cuatro PI de sección aparecen en **todo bloque** (también en los `Ref` y los de solo lectura), **siempre en este orden** y aunque alguna sección quede vacía:
1. `<?sv-primary-actions?>` — acciones principales (`action-group` de botón/evento)
2. `<?sv-validations?>` — validaciones en local (`action-validate`/`action-condition`)
3. `<?sv-rules?>` — reglas que cambian campos simples (`action-record`/`action-attrs`)
4. `<?sv-remotes?>` — llamadas remotas al servidor (`action-method`/`action-script`)

## Referencia
Para detalle completo de atributos, sintaxis y ejemplos avanzados:

- `references/actions.md`
