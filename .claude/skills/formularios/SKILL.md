---
name: formularios
description: Este skill sirve para diseñar, generar o coregir formularios dentro de ficheros XML de vistas de Axelor con la etiqueta  `<form>`  a partir de un modelo de dominio (entidad). 
---

# formularios

## Formularios en Axelor `<form>`: diseño, generación y corrección
  - Este skill sirve para diseñar, generar o corregir los formularios `<form>` que están en ficheros de vistas. 
  - Estos formularios se crean a partir de un modelo de dominio. 
  - Se siguen las normas definidas en `references/basic.md` 

## Tareas a realizar.

  - Diseñar un formulario: A partir de un modelo de dominio, se diseña un formulario que contiene los campos de la entidad.
  - Decidir los campos a incluir y como será cada uno de ellos (tipo de widget, atributos, grid y form relacionados, etc.)
  - Decidir los paneles que habrá y qué campos contienen
  - Decidir si hay grids dentro con `<panel-related>` . En ese caso crear los grids relacionados y referenciarlos correctamente. Decidir el valor de los atributos `title`y `newButtonTitle` de forma clara para que sean entendidos por el usuario.
  - Decidir que acciones (botones) va a haber.
  - Decidir el nombre del formulario siguiendo la convención `{Prefijo}.{Entidad}@{Vista}-form`
  - Decidir el título del formulario con el atributo `title`, sabiendo que el título es lo que se muestra en la cabecera del formulario y es pensando en el usuario

## Layout del formulario
 - Decidir el layout de los paneles es decir su colOffet y colSpan. Normalmente se suele usar un colSpan de 12 columnas.
 - Decidir el layout de los campos dentro de cada panel, es decir su colOffet y colSpan. Esta es la parte más compleja de todo.

## En caso de tener que tener que crear el formulario
 - Crear la etiqueta `<form>` con el nombre correcto siguiendo la convención de nomenclatura y los atributos que se han indicado que siempre deben estar.
 - Crear los paneles dentro del formulario con su layout (colOffset y colSpan) y los campos que contienen.
 - Crear los campos dentro de cada panel con su layout (colOffset y colSpan) y sus atributos (tipo de widget, grid y form relacionados, etc.)
 - Crear los `<panel-related>` con su layout (colOffset y colSpan) y sus atributos (grid-view, form-view, etc.)
 - Crear los grid y form de los `<panel-related>`
 - Crear los botones con sus acciones (onClick) y su title
 - 
## Si el formulario ya existe pero hay que corregirlo
- Corregir la etiqueta `<form>` con el nombre correcto siguiendo la convención de nomenclatura y los atributos que se han indicado que siempre deben estar.
- Corregir los paneles dentro del formulario con su layout (colOffset y colSpan) y los campos que contienen.
- Corregir los campos dentro de cada panel con su layout (colOffset y colSpan) y sus atributos (tipo de widget, grid y form relacionados, etc.)
- Corregir los `<panel-related>` con su layout (colOffset y colSpan) y sus atributos (grid-view, form-view, etc.)
- Corregir los grid y form de los `<panel-related>`
- Corregir los botones con sus acciones (onClick) y su title

## Revisión
- [ ] Revisar que todos los campos existen en el modelo de dominio y que su tipo de widget es correcto para el tipo de campo.
- [ ] Revisar que el formulario creado sigue la convención de nomenclatura y las normas de diseño.
- [ ] Revisar que todas las referencias a acciones, grids y forms son correctas y apuntan a elementos que existen.
- [ ] Revisar que el layout de los paneles y campos es correcto y no hay solapamientos ni espacios vacíos innecesarios. Sobre todo que no pasan de 12 columnas en total en cada fila.


## Referencias
Para una referencia completa de todo lo relacionado con formularios en Axelor, puedes consultar los siguientes documentos, aunque no suelen ser necesarios para tareas básicas de diseño o generación de formularios, sí pueden ser útiles para tareas de corrección o para entender mejor el contexto:

- `references/form.md`
- `references/widgets.md`
- `references/extensions.md`

