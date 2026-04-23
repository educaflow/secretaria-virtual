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
<action-view name="subsysFirma.TareaFirma@pendiente-action" title="Documentos pendientes de firma" model="com.educaflow.subsystem.firmas.db.TareaFirma">
    <view type="grid" name="subsysFirma.TareaFirma@pendiente-grid"/>
    <view type="form" name="subsysFirma.TareaFirma@pendiente-form"/>
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

```xml
<action-method name="subsysSistemaEducativo.LeyEducativa@Main-Remote-validateSave-action" model="com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa">
    <call class="com.educaflow.subsystem.sistemaeducativo.controller.LeyEducativaController" method="validateSave" />
</action-method>
```

- El atributo `model` es opcional pero recomendable para facilitar la trazabilidad y el mantenimiento. Si se pone, se recomienda que sea el modelo principal sobre el que actúa la acción.
- El atributo `class` se refiere a la clase Java del controlador donde se encuentra el método a llamar, y el atributo `method` al método concreto que se quiere ejecutar. Ese método Java debe estar anotado con `@CallMethod` para que pueda ser llamado desde la acción.
- En este caso el método Java tendrá como argumentos `ActionRequest actionRequest, ActionResponse actionResponse`

```java
   @CallMethod
   public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
  
   }
```

```xml
<action-method name="subsysSistemaEducativo.LeyEducativa@Main-Remote-validateSave-action" model="com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa">
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
<action-attrs name="subsysSistemaEducativo.LeyEducativa@Main-set-orderDate.readonly-confirmed-action">
  <attribute for="orderDate" name="readonly" expr="true" />
</action-attrs>
```

- El atributo `for` indica el nombre del campo al que se le van a cambiar los atributos.
- El atributo `name` indica el atributo que se va a cambiar, en este caso `readonly`.
- El atributo `expr` indica la expresión que se va a evaluar para cambiar el atributo, en este caso el campo `orderDate` se volverá de solo lectura cuando el campo `confirmed` sea `true`.

### `<action-record>`

Asignar un valor a un campo

```xml
<action-record name="subsysFirma.TareaFirma@Pendiente-set-pasoActual-paso1Inicio-action" model="com.educaflow.subsystem.firmas.db.TareaFirma">
    <field name="pasoActual" expr="paso1Inicio" />
</action-record>
```

- El atributo `name` del `field` es el campo al que se le va a asignar el valor.
- El atributo `expr` del `field` es el valor que se le va a asignar al campo, en este caso el valor literal `paso1Inicio`.

```xml
<action-record name="subsysSistemaEducativo.Ciclo.Curso@Main-ciclo-parent-action" model="com.educaflow.subsystem.sistemaeducativo.db.Curso">
    <field name="ciclo" expr="eval: __parent__"/>
</action-record>

```
- En este otro ejemplo, el campo `ciclo` del curso se le asigna el valor del ciclo padre usando la expresión `eval: __parent__` que hace referencia al registro padre en la vista anidada.

### `<action-group>` — secuencia de acciones principales

```xml
<action-group name="subsysSistemaEducativo.LeyEducativa@Main-btnSave-action">
    <action name="subsysSistemaEducativo.LeyEducativa@Main-Local-validateSave-action"/>
    <action name="subsysSistemaEducativo.LeyEducativa@Main-Remote-validateSave-action"/>
    <action name="save"/>
</action-group>
```

- Simplemente se listan las acciones a ejecutar en orden. En este caso, primero se ejecuta la acción de validación local (`subsysSistemaEducativo.LeyEducativa@Main-Local-validateSave-action`) y si pasa sin errores, se ejecuta la acción de validación remota (`subsysSistemaEducativo.LeyEducativa@Main-Remote-validateSave-action`), finalmente se ejecuta la accion `save`.

Se usan estas acciones desde eventos como `onClick` de botones, `onSave` de formularios, `onChange` de campos, etc. para ejecutar una secuencia de acciones en un solo evento.

### `<action-validate>`

Validaciones locales a nivel de todo el formulario, que no dependen de un campo concreto. Por ejemplo, para validar que el nombre y el código no sean "dd":

```xml
<action-validate name="subsysVentas.Producto@Main-Local-validateSave-action">
    <error message="Ni el nombre ni el código pueden ser dd" if="name=='dd' || code=='dd'"/>
    <error message="Si el estado está cancelado no es posible que el precio sea mayor que cero" if="state='CANCELADO' && precio>0"/>
</action-validate>
```

Mira en `references/actions.md` para sintaxis completa de validaciones porque hay validaciones de tipo error, alert, info y notify

### `<action-condition>`

Validaciones a nivel de campo concreto, que muestran el mensaje de error justo debajo del campo. Por ejemplo, Validar el campo `createDate` de forma que si `orderDate > createDate` se muestre el error "Order creation date is in the future."

```xml
<action-condition name="subsysFirma.TareaFirma@Pendiente-Local-validateMarcarComoRechazada-action">
  <check field="createDate" if="orderDate > createDate" error="Order creation date is in the future."/>
</action-condition>
```

- El atributo `field` indica el campo al que se le va a asociar la validación.
- El atributo `if` indica la expresión que se va a evaluar para validar el campo. Si la expresión es `true`, se muestra el mensaje de error.
- El atributo `error` indica el mensaje de error que se va a mostrar debajo del campo si la validación falla.
- Si no se indica el atributo `if` se comprueba que no esté vacío.

### `<action-script>`

Permite ejecutar acciones complejas mediante un script en `js` o `groovy`. Se utilizan en vez de crear un controlador en Java.

```xml
<action-script name="subsysVentas.Factura@Pendiente-Remote-guardarFactura-action" model="com.axelor.sale.db.Order" >
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
{Prefijo}.{Entidad}[.{Entidad}]*@{Vista}-[Local-|Remote-|set-]{explicacion}-action
```


| Parte           | Descripción                                                                                | Ejemplo                                                                    |
| --------------- |--------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| `{Prefijo}`     | `subsys{Subsistema}` para subsistemas, `sys{Sistema}` para sistemas                        | `subsysSistemaEducativo`, `subsysFirma`                                    |
| `{Entidad}`     | Nombre exacto de la clase Java                                                             | `LeyEducativa`, `TareaFirma`                                               |
| `@{Vista}`      | Identificador del contexto de vista                                                        | `@Main`, `@Pendiente`, `@Firmado`                                          |
| `Local-`        | Prefijo para validaciones client-side (`action-validate` o `action-condition`)             | `Local-validateSave`                                                       |
| `Remote-`       | Prefijo para llamadas al servidor (`action-method` o `action-script`)                      | `Remote-validateSave`                                                      |
| `set-`          | Prefijo para asignar valores a campos o a atributos (`<action-record>` o `<action-attrs>`) | `set-btnClose.title-Cerrar`                                                |
| `{explicacion}` | Descripción de la accion. Este valor depende de la acción concreta                         | `validateSave`, `marcarComoFirmada`, `nombre.readonly-true`, `centro-null` |
| `-action`       | Sufijo fijo siempre al final                                                               |                                                                            |

### Ejemplos de nombres

- **`action-view`**
  `subsysSistemaEducativo.LeyEducativa@Main-action`
  `subsysFirma.TareaFirma.DocumentoFirmado@Pendiente-action`
- **`action-group`** — orquestador público, sin prefijo `Local`/`Remote`:
  `subsysSistemaEducativo.LeyEducativa@Main-btnSave-action`
  `subsysSistemaEducativo.LeyEducativa@Main-onNew-action`
  `subsysSistemaEducativo.LeyEducativa@Main-btnCancel-action`
  `subsysSistemaEducativo.LeyEducativa@Main-onLoad-action`
- **`action-validate` o `action-condition`** — siempre con prefijo `Local-` ya que son validaciones que se hacen en el cliente sin llamada al servidor
  `subsysSistemaEducativo.LeyEducativa@Main-Local-validateSave-action`
- **`action-method`** — siempre con prefijo `Remote-` ya que son llamadas a métodos Java en el servidor
  `subsysSistemaEducativo.LeyEducativa@Main-Remote-validateSave-action`
- **`action-script`** — siempre con prefijo `Remote-` ya que son llamadas a métodos en el servidor
  `subsysSistemaEducativo.LeyEducativa@Main-Remote-insertarFactura-action`
- **`action-record`** — describe campo y valor con `set-{campo}-{valor}`:
  `subsysFirma.TareaFirma@Pendiente-set-nombre-Juan-action`
- **`action-attrs`** — describe campo y valor con `set-{campo}.{atributo}-{valor}`:
  `subsysFirma.TareaFirma@Pendiente-set-nombre.readonly-true-action`

**IMPORTANTE: Es obligatorio seguir esta convención de nombres para facilitar la trazabilidad, la lectura y el mantenimiento del código.**


## Eventos habituales donde se usan

Estas acciones se suelen disparar desde eventos de vista:

- `onNew`, `onLoad`, `onSave`, `onChange`, `onSelect`, `onClick`

## Acciones predefinidas del framework de Axelor
Además de las acciones definidas por el desarrollador, el framework de Axelor tiene una serie de acciones predefinidas que se pueden usar directamente sin necesidad de definirlas. Estas acciones predefinidas son:
- `save`: guarda el registro actual.
- `validate`: ejecuta las validaciones definidas en el formulario.
- `close`: cierra la vista actual.
- `back`: navega a la vista anterior.
- `force-back`: navega a la vista anterior sin ejecutar las validaciones.
- `delete`: elimina el registro actual sin mostrar un modal de confirmación.
- `delete-modal`: elimina el registro actual mostrando un modal de confirmación.
- `save-modal`: guarda el registro actual mostrando un modal de confirmación.
- `new`: crea un nuevo registro.


## Orden de las acciones en el código:

El orden de las acciones en el código es importante para facilitar la lectura y el mantenimiento y es el siguiente:

1. Las acciones de tipo `<action-view>` que abren vistas
2. Los grids `<grid>`
3. Los formularios `<form>`
4. Las acciones de las tareas principales (`<action-group>`) que suelen ser las tareas principales que se disparan desde botones o eventos importantes como `onSave`
5. Las acciones de validación en local (`<action-validate>` y `<action-condition>`)
6. Las acciones básicas que cambian campos simples (`<action-record>` y `<action-attrs>`) 
7. Las acciones de llamadas remotas al servidor (`<action-method>` y `<action-script>`) 


Es obligatorio respetar este orden para facilitar la lectura y el mantenimiento del código, ya que las acciones suelen estar relacionadas entre sí y es importante que estén agrupadas de forma lógica.

### Comentarios para separar cada sección de acciones
Por último, es **obligatorio** añadir estos comentarios antes de ciertas sección de acciones para indicar qué tipo de acciones se encuentran a continuación

```
    <!-- *********************************************************************************************  -->
    <!-- ***************************** Acciones de las tareas principales ****************************  -->
    <!-- *********************************************************************************************  -->
```

```
    <!-- *********************************************************************************************  -->
    <!-- ***************************** Acciones de Validaciones en local  ****************************  -->
    <!-- *********************************************************************************************  -->
```

```
    <!-- *********************************************************************************************  -->
    <!-- ************************ Acciones básicas que cambian campos simples ************************  -->
    <!-- *********************************************************************************************  -->
```

```
    <!-- *********************************************************************************************  -->
    <!-- ************************** Acciones de llamadas Remotas al servidor *************************  -->
    <!-- *********************************************************************************************  -->
```
