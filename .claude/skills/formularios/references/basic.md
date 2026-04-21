# Estructura y patrones básicos de los formularios en el proyecto

## Plantilla básica de un formulario

```xml
<form name="subsysSistemaEducativo.Ciclo@Main-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
    <panel name="nombrePanel1" title="Titulo1" colSpan="12">
        <field name="code"/>
        <field name="name"/>
    </panel>

   <panel name="nombrePanel2" title="Titulo2">
      <field name="centro" form-view="Nombre de formulario para ver todos los datos del centro. Un nombre típico suele ser sysCentro.Centro@View" grid-view="Nombre de grid para buscar un centro. Un nombre típico suele ser sysCentro.Centro@Search"  />
   </panel>

    <panel-related name="modulos" field="modulos"  title="Módulos" newButtonTitle="Añadir un nuevo módulo" grid-view="subsysSistemaEducativo.Ciclo.Curso.CursoModulo@Main-grid"  form-view="subsysSistemaEducativo.Ciclo.Curso.CursoModulo@Main-form"
        colSpan="12" showFooter="false"  canEdit="false" canRemove="false" forceEdit="true"
    />
   
    <panel name="buttons-panel" title="" colSpan="12" showFrame="false" >
        <button name="btnDelete" title="Borrar" onClick="accionesBtnDelete" css="btn-danger" colSpan="2"  outline="true" showIf="(id != null)"/>
        <button name="btnCancel" title="Cancelar" onClick="accionesBtnCancel"  colSpan="2" colOffset="6" outline="true"   />
        <button name="btnSave" title="Guardar" onClick="accionesBtnSave"  colSpan="2"  />
    </panel>
    
</form>
```

IMPORTANTE:
 - En <form> deben estar todos los atributos que se han indicado en la plantilla (width, canAttach, canBack, canDelete, canNew, canSave, canMore) con los valores indicados.
 - En <panel-related> deben estar todos los atributos que se han indicado en la plantilla (colSpan, showFooter, canEdit, canRemove, forceEdit) con los valores indicados.

## Botones principales y secundarios
- Los botones principales (guardar, cancelar, etc) están a la derecha del todo
- Las acciones secundarias (borrar, imprimir, etc.) están a la izquierda del todo
- Realmente no es necesario que estén exactamente estos botones sino que podría haber otros botones con otras acciones. Pero hay que distingir claramente las acciones principales de las secundarias y para eso se siguen estas pautas de colocación.

## Nombre de los formularios
El nombre de las vistas de formularios es: `{Prefijo}{Entidad}[.{EntidadHija}]*@[Main|View|otro nombre]-form`

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
| Pantalla de Solo lectura         | `{Prefijo}.{Entidad}@View-form`                        | `sysSistemaEducativo.Ciclo@View-form`               |
| Otra pantalla distinta           | `{Prefijo}.{Entidad}@{Nombre}-form`                    | `subsysSistemaEducativo.Ciclo@Pendiente-form`       |
| Entidad anidada                  | `{Prefijo}.{EntidadPadre}.{EntidadHija}@Main-form`     | `sysSistemaEducativo.Ciclo.Curso@Main-form`         |
| Entidad anidada de otra pantalla | `{Prefijo}.{EntidadPadre}.{EntidadHija}#{Nombre}-form` | `subsysSistemaEducativo.Ciclo.Curso@Pendiente-form` |


## Layout y diseño de formularios
    - El layout de los formularios se organiza principalmente con paneles (`<panel>`) y paneles relacionados (`<panel-related>`).
    - Dentro de los paneles se colocan los campos (`<field>`) y otros widgets (botones, secciones de ayuda, etc.).
    - Se sigue una maquetación de 12 columnas usando `colSpan` y `colOffset` para distribuir los campos en el espacio del panel.
    - Se usan condicionales (`showIf`, `hideIf`, `requiredIf`, `readonlyIf`) para mostrar, ocultar o modificar la interactividad de campos y paneles según el estado del formulario o los datos.
    - Se pueden incluir secciones de ayuda dentro de los paneles usando `<help variant="info|warning|...">` para guiar al usuario.
    - Los botones de acción se colocan dentro de un panel específico (normalmente al final del formulario) y se configuran con `onClick` para lanzar las acciones correspondientes.



## `<panel-related>`
Se usa para colecciones relacionales `<one-to-many>` del modelo y muestra una rejilla hija dentro del formulario padre, se acompaña con los atributos `grid-view` y `form-view` específicos.


colSpan="12" showFooter="false"  canEdit="false" canRemove="false" forceEdit="true"

## Campos (`field`) y widgets clave en este proyecto

`<field>` es la etiqueta **más importante** de un form y vincula un atributo del modelo al formulario. Además de `name`, aquí se define gran parte de la UX mediante atributos y widgets. Un campo siempre debe estar dentro de un panel

### Descarga/subida de ficheros `MetaFile`

- `widget="binary-link"`: para campos `MetaFile` permite cargar/descargar un fichero.
- `widget="binary"`: Para descargar directamente el `content` del  ̀MetaFile`.
- `x-accept`: para restringir tipos de fichero (por ejemplo PDF o imagen).

### colSpan/colOffset en field
- Para definir el tamaño de un campo se usa "colSpan" (número de columnas que ocupa) y "colOffset" (espacio "hueco" dejado a la izquierda).
- El proyecto sigue una maquetación de 12 columnas, por lo que un campo con `colSpan="6"` ocuparía la mitad del ancho del panel.
- Esto se usa para organizar campos en la misma línea
- Para centrar un campo en una linea se usaría  `colOffset="3"` y `colSpan="6"`.

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
Lo normal es que siempre esté todo alineado a la izquierda, pero en casos puntuales puede ser útil para centrar un campo o dejar espacio a la izquierda para mejorar la legibilidad.

Ejemplo puntual (No es normal) de no dejar algo a la izquierda para que quede más claro:
```xml
<panel title="Datos personales">
    <field name="nombre" colSpan="10"/>
    <field name="fechaInicio" colSpan="2"/>
    <field name="fechaFin" colSpan="2" colOffset="10" />    
</panel>
```
En el ejemplo anterior, el campo 'nombre' ocuparía la mayor parte de la línea, mientras que 'fechaInicio' y 'fechaFin' se mostrarían uno debajo del otro, con 'fechaFin' alineado respecto a 'fechaInicio' al `colOffset="10"`.

También es importante tener en cuenta que el uso de `colSpan` y `colOffset` para intentar alinear los campos con los de la fila anterior o siguiente.

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

Aunque no es una obligación estricta, es recomendable intentar alinear los campos con los de la fila anterior o siguiente para mejorar la legibilidad del formulario. Y ver como ponerlo para que quede claro y no se corte el texto.

### Enumerados

Para campos del modelo de tipo enum se usa especialmente `widget="SwitchSelect"` (horizontal o vertical con `x-direction`).



### Otros widgets/patrones frecuentes

- `widget="Text"` para textos largos (por ejemplo motivos de rechazo).
- `widget="suggest"` / selección asistida en campos relacionales con `domain`.
- `<viewer><![CDATA[...]]></viewer>` para render personalizado (por ejemplo incrustar PDF en `iframe` con URL de descarga).




