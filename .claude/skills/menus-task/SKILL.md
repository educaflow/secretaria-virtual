---
name: menus-task
description: Usa este skill cuando el usuario quiera crear o modificar entradas de menú (menuitem) en Axelor. Los menús se definen en ficheros XML dentro de secretariavirtual/menus/.
---

# menu

Un menu es una entrada en el menú de la aplicación que puede ser una sección raíz, una subsección o una entrada final que abre una vista. 
Se definen con la etiqueta `<menuitem>` dentro de ficheros XML ubicados en `src/main/java/com/educaflow/secretariavirtual/menus/`.

## Menús en Axelor `<menuitem>`: diseño, generación y corrección
- Este skill sirve para diseñar, generar o corregir los menús `<menuitem>` que están en ficheros de vistas.
- Estos menús se crean a partir de un modelo de dominio y de las posibles vistas que puede haber de un mismo modelo de dominio.
- Se siguen las normas definidas en el skill `/menus`

## Tareas a realizar.
- Saber las `<action-view>` que hay en ese subsistema para saber a qué vistas se pueden apuntar desde los menús.  
- Decidir el nombre y título del menú principal, siguiendo la convención de nombres y teniendo en cuenta el orden visual en el menú. Y decidir el número de orden que se le asigna a ese menú, teniendo en cuenta el orden visual en el menú y respetando el rango numérico del prefijo.
- Decidir el nombre y título de las entradas de submenú siguiendo la convención de nombres
- Decidir a qué `<action-view>` apuntan las entradas de menú final (hoja) y hacer que el menú apunte a esa acción.
- Decidir los grupos de usuarios que pueden ver cada entrada de menú.

## En caso de tener que crear el menú
- Crear el fichero XML con el nombre correcto siguiendo la convención de nomenclatura y respetando el rango numérico del prefijo para mantener el orden visual en el menú.
- Crear la etiqueta `<menuitem>` del menú principal con el nombre correcto siguiendo la convención de nomenclatura y los atributos.
- Crear la etiqueta `<menuitem>` de los submenus con el nombre correcto siguiendo la convención de nomenclatura y los atributos.
- Establecer para cada `<menuitem>` la acción a la que apunta, si es una entrada de menú final (hoja), o el menú padre al que apunta, si es una entrada de submenú.
- Establecer los grupos de usuarios que pueden ver cada entrada de menú.


## Si el menú ya existe pero hay que corregirlo
- Combrueba que la etiqueta `<menuitem>` del menú principal tiene el nombre correcto siguiendo la convención de nomenclatura y los atributos.
- Combrueba que la etiqueta `<menuitem>` de los submenus con el nombre correcto siguiendo la convención de nomenclatura y los atributos.
- Combrueba que para cada `<menuitem>` la acción a la que apunta, si es una entrada de menú final (hoja), o el menú padre al que apunta, si es una entrada de submenú.
- Combrueba  los grupos de usuarios que pueden ver cada entrada de menú.


## Revisión
- [ ] Revisar que todos los nombre de las entradas de menú sigue las convenciones de nomenclatura
- [ ] Revisar que todas <action-view> a las que apuntan las entradas de menú final (hoja) existen y son correctas.
- [ ] Revisar que todas las referencias a acciones apuntan a acciones que existen.
- [ ] Revisar que todas las entradas de submenú apuntan a un menú padre que existe.
- [ ] Revisar que los grupos de usuarios que pueden ver cada entrada de menú son los correctos según lo que se ha decidido en la fase de diseño.
- [ ] Revisar que el número de orden de cada entrada de menú es correcto según lo que se ha decidido en la fase de diseño y que respeta el rango numérico del prefijo para mantener el orden visual en el menú.
- [ ] Revisar que el título de cada entrada de menú es correcto y claro para el usuario.
