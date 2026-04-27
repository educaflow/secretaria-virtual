---
name: actions-steps
description: Dado un fichero de vistas de Axelor en XML, permite añadir acciones (actions) a las vistas. Los tag de las acciones son: '<action-view>','<action-method>','<action-attrs>','<action-record>','<action-group>','<action-validate>','<action-condition>' y '<action-script>'.
---

# actions 
Este skill sirve para diseñar y generar acciones de Axelor en ficheros XML de vistas

## Actions de axelor
- Este skill sirve para diseñar, generar o corregir las acciones que están en ficheros de vistas.
- Estas acciones se crean a partir de la lógica de los menús, grids y formularios (forms) además del significado del propio dominio.
- Las etiquetas que se usan para definir acciones son: `<action-view>`, `<action-method>`, `<action-attrs>`, `<action-record>`, `<action-group>`, `<action-validate>`, `<action-condition>` y `<action-script>`.
- Se siguen las normas definidas en el skill `/actions-knowledge`

## Tareas a realizar.


- Analizar las acciones que son necesarias: A partir de los menús, grids y formularios (forms) además del significado del propio dominio.
- Para cada acción decidir 
  - El tipo de acción que es: `<action-view>`, `<action-method>`, `<action-attrs>`, `<action-record>`, `<action-group>`, `<action-validate>`, `<action-condition>`, `<action-script>`
  - Su nombre siguiendo la convención de nomenclatura que se ha creado para las acciones.(**Es especialmente importante seguir la nomenclatura**)
  - Bajo que evento se ejecuta (onSave, onChange, onNew, onLoad, onClick de botones, etc). Aqui se incluye el que algunas son llamadas en vez de ser llamadas desde un evento son solo llamadas por otras acciones de tipo group `<action-group>` 
  - Para las acciones de tipo view `<action-view>`, decidir el tipo de vista (grid y/o form) que se muestran
  - Para las acciones de tipo method `<action-method>`, decidir el nombre de la clase java/método que se ejecuta en Java.
  - Para las acciones de tipo group `<action-group>`, decidir que acciones forman parte del grupo.
  - Para las acciones de tipo validate `<action-validate>`, decidir que reglas se crean y sobre que campos.
  - Para las acciones de tipo condition `<action-condition>`, decidir que reglas se crean y sobre que campos.
  - Para las acciones de tipo record `<action-record>`, decidir que reglas se crean, sobre que campos y que valores tendrán esos campos.
  - Para las acciones de tipo attrs `<action-attrs>`, decidir que atributos se modifican, sobre que campos y que valores tendrán esos atributos.
  - Analizar en que posición del XML se deben crear las acciones. Normalmente las acciones se suelen colocar al final del XML, justo antes de la etiqueta de cierre `</object-views>`, pero a veces puede ser necesario colocarlas en otro lugar dependiendo de la lógica de la vista.



## Comentarios
- Analizar donde van los comentarios de las acciones que ya están definidos para cada conjunto de acciones.
- Añadir los bloques de comentarios. Estos comentarios deben ser exactamente iguales a los que ya están definidos para mantener la consistencia y facilitar la lectura del código.
- Si los comentarios no están definidos, añadir un bloque de comentarios al principio de la sección de acciones siguiendo el formato de los comentarios ya definidos en otras vistas.
- Si fuera necesario cambiar la posición de los comentarios para que queden mejor organizados, hacerlo siguiendo el formato de los comentarios ya definidos.

## Si la acción hay que crearla
- Crear la etiqueta de acción correspondiente al tipo de acción que se ha decidido: `<action-view>`, `<action-method>`, `<action-attrs>`, `<action-record>`, `<action-group>`, `<action-validate>`, `<action-condition>` o `<action-script>`
- Establecer el nombre de la acción siguiendo la convención de nomenclatura que se ha creado para las acciones.(**Es especialmente importante seguir la nomenclatura**)
- Establecer el resto de atributos de la acción según lo que se haya decidido en la fase de diseño.
- Si es necesario establecer otras etiquetas que van dentro de cada acción, como por ejemplo las etiquetas de vista `<view type="grid|form" name="..."/>` para las acciones de tipo view `<action-view>`, o las etiquetas de acción `<action name="..."/>` para las acciones de tipo group `<action-group>`, etc. 
- Al crear la etiqueta que sea en la posición en el fichero que se ha establecido
- Actualizar los menús, grids o formularios (forms) para referenciar las acciones creadas en los eventos correspondientes (onSave, onChange, onNew, onLoad, onClick de botones, etc).

## Si la acción ya existe y hay que corregirla
- Actualizar la etiqueta de acción correspondiente al tipo de acción que se ha decidido y en caso necesario la posición en el fichero que se ha establecido y los atributos necesarios (o etiquetas interiores)
- En caso necesario actualizar el nombre por el nombre correcto siguiendo la convención de nomenclatura
- Actualizar los menús, grids o formularios (forms) para referenciar las acciones creadas en los eventos correspondientes (onSave, onChange, onNew, onLoad, onClick de botones, etc).


## Revisión
- [ ] Revisar que todos los nombre de las acciones sigue las convenciones de nomenclatura
- [ ] Revisar que el tipo de acción es el correcto según lo que se ha decidido en la fase de diseño.
- [ ] Revisar que todas las referencias a acciones que existen son llamadas por alguien
- [ ] Revisar que todas las referencias a acciones apuntan a acciones que existen.
- [ ] Revisar que todas las acciones están en la posición correcta dentro del fichero XML.
- [ ] Revisar que los comentarios de las acciones están bien organizados y siguen el formato de los comentarios definidos.


