---
name: Formularios de Axelor
description: Dentro de un fichero XML de vistas para Axelor crea los fomurlarios a partir de una descripción en lenguaje natural o un modelo de axelor
---

Este skill sirve para diseñar dentro de ficheros de vistas un formulario o etiqueta `<form>` de Axelor a partir de una necesidad funcional o de un modelo de dominio.

## Qué es un formulario en Axelor

Un formulario (`<form>`) es la vista principal para ver y editar un único registro.
En esta vista se organiza la información en paneles, se definen acciones (`onLoad`, `onSave`, botones) y se controla el comportamiento de campos (visibilidad, validación y solo lectura).

Estructura mínima:

```xml
<form name="mi-form" title="Mi entidad" model="com.miapp.db.MiEntidad">
  <panel title="Datos">
	<field name="code"/>
	<field name="name"/>
  </panel>
</form>
```

## Paneles: lo más importante

### `panel`

Es el contenedor básico de campos dentro del formulario.
Se usa para agrupar bloques funcionales (datos generales, resolución, adjuntos, etc.).

Patrones importantes en el proyecto:

- Maquetación con `colSpan` y `colOffset` para distribuir contenido en 12 columnas.
- Condicionales con `showIf`, `hideIf`, `requiredIf`, `readonlyIf`.
- Secciones de ayuda con `<help variant="info|warning|...">`.
- Botones dentro del panel para lanzar acciones (`onClick`).


### `panel-related`

Se usa para colecciones relacionales (`one-to-many` / `many-to-many`) y muestra una rejilla hija dentro del formulario padre.
Normalmente se acompaña con `grid-view` y `form-view` específicos.

Patrones importantes en el proyecto:

- Edición de hijos por subformulario (`form-view`) y listado (`grid-view`).
- Inicialización de relación padre-hijo usando `action-record` con `expr="eval: __parent__"` en `onNew` del formulario hijo.
- Uso intensivo para anexos y líneas relacionadas (usuarios, cursos, módulos, documentos...).


## Campos (`field`) y widgets clave en este proyecto

`<field>` vincula un atributo del modelo al formulario. Además de `name`, aquí se define gran parte de la UX mediante atributos y widgets.
Un campo siempre debe estar dentro de un panel o panel-related.

### Descarga/subida de ficheros (muy usado)

- `widget="binary-link"`: para campos `MetaFile` o binarios enlazados; permite cargar/descargar fácilmente.
- `widget="binary"`: usado en formularios de anexos para botón directo de descarga (`content`).
- `x-accept` o `file-type`: para restringir tipos de fichero (por ejemplo PDF o imagen).

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

### SELECT para enumerados

En el proyecto, para campos enum se usa especialmente `widget="SwitchSelect"` (horizontal o vertical con `x-direction`).
Cuando no se indica widget, Axelor usa el editor por defecto del tipo de campo.


### Otros widgets/patrones frecuentes

- `widget="Text"` para textos largos (por ejemplo motivos de rechazo).
- `widget="suggest"` / selección asistida en campos relacionales con `domain`.
- `<viewer><![CDATA[...]]></viewer>` para render personalizado (por ejemplo incrustar PDF en `iframe` con URL de descarga).


## Extensiones de formularios (`extension="true"`)

Cuando no se quiere duplicar una vista base, se crea una vista de extensión con el mismo `name`, un `id` único y `extension="true"`.
Después se aplican cambios con `<extend target="...">` usando operaciones:

- `<insert position="before|after|inside">`
- `<replace>`
- `<move position="..." source="..."/>`
- `<attribute name="..." value="..."/>`

Patrón real del proyecto:

- Ocultar campos existentes con `<attribute name="showIf" value="1=0"/>`.
- Insertar nuevos campos antes/después de un campo existente.
- Ajustar `colSpan`/`colOffset` sin tocar la vista original.



## Checklist al generar un formulario

- Definir `name`, `model`, `title` y permisos (`groups`) coherentes.
- Separar bien datos principales (`panel`) y colecciones (`panel-related`).
- Priorizar `showIf/requiredIf/readonlyIf` frente a lógica duplicada.
- Para ficheros, elegir `binary-link` o `binary` según caso de uso.
- Para enums, valorar `SwitchSelect` cuando se quiera selección más guiada.
- Si hay que modificar una vista estándar, preferir extensión (`extension="true"`) en lugar de copia completa.

## Referencias

- `references/form.md`
- `references/widgets.md`
- `references/extensions.md`

