---
name: formularios-knowledge
description: Estructura y patrones básicos de los formularios en el proyecto Axelor - plantilla, layout, campos, widgets y convención de nombres.
---

# Estructura y patrones básicos de los formularios en el proyecto

## Plantilla básica de un formulario

```xml
<form name="subsysSistemaEducativo.Ciclo@Main-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true">
    <panel name="Ciclo" title="Ciclo">
        <field name="code"/>
        <field name="name"/>
    </panel>

   <panel name="otroPanel" title="Otro panel">
      <field name="centro" form-view="subsysCentro.Centro@View-form" grid-view="subsysCentro.Centro@Search-grid"  />
      <field name="grado" colOffset="6" colSpan="4"              grid-view="subsysSistemaEducativo.Grado@Search-grid"  domain="(self.code='D' OR self.code='E')" />
      <field name="nivel" colSpan="4"                            grid-view="subsysSistemaEducativo.Nivel@Search-grid"  showIf="grado.code=='D'" requiredIf="grado.code=='D'" domain="(self.code='D' OR self.code='E')"/>       
   </panel>

    <panel-related name="modulos" field="modulos" title="Módulos" newButtonTitle="Añadir un nuevo módulo"
        grid-view="subsysSistemaEducativo.Ciclo.Curso.CursoModulo@Main-grid" form-view="subsysSistemaEducativo.Ciclo.Curso.CursoModulo@Main-form"
        colSpan="12" showFooter="false" canEdit="false" canRemove="false" forceEdit="true"
    />
   
    <panel name="buttons-panel" title="" colSpan="12" showFrame="false" >
        <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Ciclo@Main-btnDelete-action" css="btn-danger" colSpan="2"  outline="true" showIf="(id!=null) || (cid!=null)"/>
        <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Ciclo@Main-btnCancel-action"  colSpan="2" colOffset="6" outline="true"   />
        <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Ciclo@Main-btnSave-action"  colSpan="2"  />
    </panel>
    
</form>
```

IMPORTANTE:
 - En <form> deben estar todos los atributos que se han indicado en la plantilla (width, canAttach, canBack, canDelete, canNew, canSave, canMore, canBackOnSave) con los valores indicados. `canBackOnSave="true"` solo aplica al form principal (no al modal de entidad hija). El form modal **no lleva `canBackOnSave`** y **sí lleva `onNew`** para inyectar la referencia al padre.
 - En <panel-related> deben estar todos los atributos que se han indicado en la plantilla (newButtonTitle, colSpan, showFooter, canEdit, canRemove, forceEdit) con los valores indicados.
 - El botón Borrar debe tener `showIf="(id!=null) || (cid!=null)"` — `id` es el ID del registro ya guardado; `cid` es el ID temporal de un registro nuevo todavía no guardado.
 - Los nombres de los `onClick` de los botones siguen el patrón `{Prefijo}.{EntidadJerárquica}@Main-{btnXxx}-action`, donde `{EntidadJerárquica}` puede incluir la jerarquía de entidades separadas por punto (p.ej. `Ciclo.Curso`). Por ejemplo: `subsysSistemaEducativo.Ciclo@Main-btnDelete-action` para la entidad raíz, o `subsysSistemaEducativo.Ciclo.Curso@Main-btnDelete-action` para la entidad hija.
 - Los nombres de los paneles siguen el patrón del nombre de la entidad (p.ej. `name="Ciclo"`), no nombres genéricos como `nombrePanel1`.
 - En campos relacionales: `form-view` apunta al `@View-form` de la entidad (p.ej. `subsysCentro.Centro@View-form`) y `grid-view` apunta al `@Search-grid` (p.ej. `subsysCentro.Centro@Search-grid`).

## Form modal (entidad hija en `panel-related`)

Cuando una entidad hija se edita desde un `<panel-related>`, su formulario es un **modal** con diferencias importantes respecto al form principal:

```xml
<form name="subsysSistemaEducativo.Ciclo.Curso@Main-form" title="Curso" model="com.educaflow.subsystem.sistemaeducativo.db.Curso"
      width="large"
      onNew="subsysSistemaEducativo.Ciclo.Curso@Main-onNew-action"
      canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
    <panel name="Curso" title="">
        <field name="ciclo" showIf="false"/>   <!-- campo padre, oculto pero presente en el modelo -->
        <field name="code" colSpan="3"/>
        <field name="name" colSpan="6" colOffset="3"/>
    </panel>

    <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
        <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnDelete-action"
                css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
        <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnCancel-action"
                colSpan="2" colOffset="6" outline="true"/>
        <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnSave-action"
                colSpan="2"/>
    </panel>
</form>
```

Diferencias respecto al form principal:
- **Sin `canBackOnSave`** — el cierre del modal lo gestiona `save-modal` en el action-group del botón guardar.
- **Con `onNew`** — inyecta la referencia al padre cuando se crea un registro nuevo.
- **Campo padre con `showIf="false"`** — está en el modelo pero no es visible al usuario.

Los action-groups de los botones del form modal usan acciones específicas del framework:
- Botón Borrar: `<action name="delete-modal"/>` (no `delete`)
- Botón Cancelar: `<action name="close"/>` (no `back`)
- Botón Guardar: `<action name="save-modal"/>` (no `save`)

### Tabla comparativa: form principal vs form modal

| Aspecto                | Form principal | Form modal                                |
|------------------------|----------------|-------------------------------------------|
| `canBackOnSave`        | `true`         | ausente                                   |
| `onNew`                | ausente        | presente (inyecta el padre)               |
| Campo padre            | no existe      | `showIf="false"`                          |
| `<action-view>` propio | sí             | no (lo abre el `panel-related` del padre) |
| Botón Borrar acción    | `delete`       | `delete-modal`                            |
| Botón Cancelar acción  | `back`         | `close`                                   |
| Botón Guardar acción   | `save`         | `save-modal`                              |

## Botones principales y secundarios
- Los botones principales (guardar, cancelar, etc) están a la derecha del todo
- Las acciones secundarias (borrar, imprimir, etc.) están a la izquierda del todo
- Realmente no es necesario que estén exactamente estos botones sino que podría haber otros botones con otras acciones. Pero hay que distingir claramente las acciones principales de las secundarias y para eso se siguen estas pautas de colocación.
- El panel de botones siempre debe incluir Borrar, Cancelar y Guardar salvo que se indique lo contrario o haya algo en el negocio que te haga pensar que no es necesario.

## Nombre de los formularios
El nombre de las vistas de formularios es: `{Prefijo}.{Entidad}[.{EntidadHija}]*@[Main|View|otro nombre]-form`

Una excepción a esta convención es el caso de las vistas del framework de tipos de expediente, expedientes o trámites. En ese caso aun no se ha definido una convención de nombres específica, pero se ha decidido reservar el prefijo `exp-` para todas las vistas relacionadas con ese framework, de esa forma se pueden identificar fácilmente y no se solapan con las vistas de los subsistemas o sistemas funcionales. Por ejemplo, una vista de formulario para un tipo de expediente podría llamarse `exp-TipoExpediente@Main-form`.
Otra excepción es el caso de formularios del propio Axelor que se modifican para adecuarlos a las necesidades del proyecto, en ese caso se pueden mantener los nombres originales de Axelor. Un ejemplo es el formulario 'user-preferences-form'


### {Prefijos}
- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`
- Excepción: el prefijo `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente
- Las entidades se separan con `.` (punto) y los nombres de ese formulario o grid con `@`


| Caso                             | Patrón                                                 | Ejemplo                                             |
|----------------------------------|--------------------------------------------------------|-----------------------------------------------------|
| Pantalla principal               | `{Prefijo}.{Entidad}@Main-form`                        | `subsysSistemaEducativo.Ciclo@Main-form`            |
| Pantalla de Solo lectura         | `{Prefijo}.{Entidad}@View-form`                        | `subsysSistemaEducativo.Ciclo@View-form`             |
| Otra pantalla distinta           | `{Prefijo}.{Entidad}@{Nombre}-form`                    | `subsysSistemaEducativo.Ciclo@Pendiente-form`       |
| Entidad anidada                  | `{Prefijo}.{EntidadPadre}.{EntidadHija}@Main-form`     | `subsysSistemaEducativo.Ciclo.Curso@Main-form`      |
| Entidad anidada de otra pantalla | `{Prefijo}.{EntidadPadre}.{EntidadHija}@{Nombre}-form` | `subsysSistemaEducativo.Ciclo.Curso@Pendiente-form` |

**IMPORTANTE: Es obligatorio seguir esta convención de nombres para facilitar la trazabilidad, la lectura y el mantenimiento del código.**

## Layout y diseño de formularios
    - El layout de los formularios se organiza principalmente con paneles (`<panel>`) y paneles relacionados (`<panel-related>`).
    - Dentro de los paneles se colocan los campos (`<field>`) y otros widgets (botones, secciones de ayuda, etc.).
    - Se sigue una maquetación de 12 columnas usando `colSpan` y `colOffset` para distribuir los campos en el espacio del panel.
    - Se usan condicionales (`showIf`, `hideIf`, `requiredIf`, `readonlyIf`) para mostrar, ocultar o modificar la interactividad de campos y paneles según el estado del formulario o los datos.
    - Se pueden incluir secciones de ayuda dentro de los paneles usando `<help variant="info|warning|...">` para guiar al usuario.
    - Los botones de acción se colocan dentro de un panel específico (normalmente al final del formulario) y se configuran con `onClick` para lanzar las acciones correspondientes.



## `<panel-related>`
Se usa para colecciones relacionales `<one-to-many>` del modelo y muestra una rejilla hija dentro del formulario padre, se acompaña con los atributos `grid-view` y `form-view` específicos.


## Campos (`field`) y widgets clave en este proyecto

`<field>` es la etiqueta **más importante** de un form y vincula un atributo del modelo al formulario. Además de `name`, aquí se define gran parte de la UX mediante atributos y widgets. Un campo siempre debe estar dentro de un panel

### Atributos
- `domain`:Permite restringir los valores disponibles en campos de selección (por ejemplo, campos relacionales o enums) usando expresiones booleanas que hacen referencia a los atributos del campo. Por ejemplo, para mostrar un campo solo si el código es 'D' o 'E', se usaría: `domain="(self.code='D' OR self.code='E')"`
- `showIf`: Permite mostrar un campo solo si se cumple una condición. Por ejemplo, para mostrar un campo solo si el código es 'D', se usaría: `showIf="grado.code=='D'"` (se referencia directamente el campo del formulario, sin prefijo `self.`)
- `widget="binary-link"`: para campos `MetaFile` permite cargar/descargar un fichero.
- `widget="binary"`: Para descargar directamente el `content` del  ̀MetaFile`.
- `x-accept`: para restringir tipos de fichero (por ejemplo PDF o imagen).
- `widget="SwitchSelect"`: Para campos del modelo de tipo enum (horizontal o vertical con `x-direction`).
- `widget="Text"` para textos largos (por ejemplo motivos de rechazo).
- `widget="SuggestBox"` / selección asistida en campos relacionales con `domain`.
- `readonly="true"` para mostrar un campo como solo lectura.
- `colSpan`: Para definir el tamaño del campo. Vease más abajo para entenderlo mejor.
- `colOffset`: Para dejar un espacio a la izquierda del campo. Vease más abajo para entenderlo mejor.
- 
### HTML personalizado para mostrar el contenido de un campo
- Para mostrar el contenido de un campo de forma personalizada (por ejemplo, mostrar un PDF incrustado en el formulario), se puede usar la etiqueta `<viewer>` dentro del `<field>`.

```xml
<field name="new" showTitle="false" readonly="true" colSpan="12">
    <viewer depends="documentoOriginal"><![CDATA[
        <>
        <Box as="iframe" height="500" border="0" src={`ws/rest/com.axelor.meta.db.MetaFile/${documentoOriginal.id}/content/download?inline=true&name=${documentoOriginal.fileName}`}></Box>
        </>
    ]]></viewer>
</field>
``` 

### Layout de los campos: colSpan/colOffset
- Para definir el tamaño de un campo se usa "colSpan" (número de columnas que ocupa) y "colOffset" (espacio "hueco" dejado a la izquierda).
- El proyecto sigue una maquetación de 12 columnas, por lo que un campo con `colSpan="6"` ocuparía la mitad del ancho del panel.
- Esto se usa para organizar campos en la misma línea
- Para centrar un campo en una línea se usaría  `colOffset="3"` y `colSpan="6"`.

Te pongo el siguiente ejemplo para que lo veas más claro:

```xml
<panel title="Datos personales">
    <field name="campo1" colSpan="6"/>
    <field name="campo2" colSpan="6"/>
    <field name="campo3" colSpan="6" colOffset="6" />    
</panel>
```

En el ejemplo 'campo1' y 'campo2' se mostrarían en la misma línea ocupando cada uno la mitad del ancho del panel, mientras que 'campo3' se mostraría en una nueva línea y estaría en la segunda mitad del ancho del panel, dejando un espacio vacío a su izquierda gracias al `colOffset="6"`.

Es importante usar `colSpan` y `colOffset` de manera coherente para lograr una maquetación clara y organizada en el formulario. Se debe pensar en el colSpan para que quepa todo el texto.
Si el texto es largo, se puede usar `colSpan="12"` para que ocupe toda la línea y evitar que se corte. Por ejemplo para campos de fechas sobra con colSpan="2".
Tambien hay que ver que pones en la misma linea, normalmente son campos relacionados, por ejemplo fecha de inicio y fecha de fin, o nombre y apellidos.

**Distribución proporcional al contenido real del campo**
No hay que dividir el espacio equitativamente entre campos de la misma fila: hay que asignar más espacio al campo cuyo valor ocupa más texto visualmente.

Ejemplo incorrecto (reparto igual sin considerar el contenido):
```xml
<field name="centro"         colSpan="4"/>
<field name="numeroRegistro" colSpan="4"/>
<field name="fecha"          colSpan="4"/>
```
"centro" muestra un nombre largo, mientras que "numeroRegistro" y "fecha" suelen ser valores cortos. Con `colSpan="4"` los tres, "centro" se quedará estrecho y los otros dos tendrán espacio de sobra.

Ejemplo correcto (espacio proporcional al contenido esperado):
```xml
<field name="centro"         colSpan="6"/>
<field name="numeroRegistro" colSpan="3"/>
<field name="fecha"          colSpan="3"/>
```

También es importante tener en cuenta que el uso de `colSpan` y `colOffset` para intentar alinear los campos con los de la fila superior e inferior.

En el ejemplo siguiente se hace mal la alineación de campos ya que ninguno de los campos está alineado con el de arriba.:
```xml
<panel title="Datos personales">
    <field name="campo1" colSpan="4"  />
    <field name="campo2" colSpan="2"  />
    <field name="campo3" colSpan="6"  />    
    <field name="campo4" colSpan="2"  />    
    <field name="campo5" colSpan="6"  />    
    <field name="campo6" colSpan="4"  />    
</panel>
```
Fíjate que en el ejemplo anterior, "campo1" no está alineado con ningún campo de la fila inferior, "campo2" no está alineado con ningún campo de la fila inferior, "campo3" no está alineado con ningún campo de la fila inferior, "campo4" no está alineado con ningún campo de la fila superior, "campo5" no está alineado con ningún campo de la fila superior y "campo6" no está alineado con ningún campo de la fila superior. Esto hace que el formulario se vea desorganizado y dificulta la lectura.

Una mejor forma de hacerlo sería:
```xml
<panel title="Datos personales">
    <field name="campo1" colSpan="4"  />
    <field name="campo2" colSpan="3"  />
    <field name="campo3" colSpan="5"  />    
    <field name="campo4" colSpan="4"  />    
    <field name="campo5" colSpan="3"  />    
    <field name="campo6" colSpan="5"  />    
</panel>
```
En este ejemplo, "campo1" está alineado con "campo4", "campo2" está alineado con "campo5" y "campo3" está alineado con "campo6". Esto hace que el formulario se vea más organizado y facilita la lectura.

## Referencias
Para una referencia completa de todo lo relacionado con formularios , puedes consultar los siguientes documentos:
- `references/form.md`
- `references/widgets.md`
- `references/extensions.md`
