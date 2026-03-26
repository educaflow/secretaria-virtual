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

