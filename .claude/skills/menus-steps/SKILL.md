---
name: menus-steps
description: Usa este skill cuando el usuario quiera crear o modificar entradas de menú (menuitem) en Axelor. Los menús raíz se definen en ficheros XML dentro de secretariavirtual/menus/; los menuitems hoja van en el mismo fichero que la action-view que referencian.
---

# menu

Un menu es una entrada en el menú de la aplicación que puede ser una sección raíz o una entrada final que abre una vista.
Se definen con la etiqueta `<menuitem>` siguiendo las normas definidas en el skill `/menus-knowledge`.

## Ubicación
- Los `<menuitem>` siempre van en el fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`

## Tareas a realizar

- Identificar las `<action-view>` del subsistema a las que apuntarán los menuitems.
- Decidir el nombre y título del menú raíz (si procede) y su orden

## En caso de crear el menú raíz
- Decidir el título del menuitem raiz, que debe ser claro para el usuario.
- Decidir el número de orden del menuitem raíz, que debe ser un número entero que empieza por 1 y se incrementa de 1 en uno y no repetirse con otros menuitems raíz.
- Decidir el nombre del menuitem raíz siguiendo la convención de nomenclatura
- Añadir el `<menuitem>` con todos los atributos en la posición correcta dentro del fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`, respetando el orden indicado por el número de orden.

## En caso de crear menuitems hoja
- Decidir el título del menuitem raiz, que debe ser claro para el usuario.
- Decidir el número de orden del menuitem raíz, que debe ser un número entero y no repetirse con otros menuitems raíz.
- Decidir el nombre del menuitem raíz siguiendo la convención de nomenclatura
- Decidir el nombre de la `action-view` a la que apuntará el menuitem, que debe existir en algún fichero XML del proyecto.
- Decidir el `parent` del menuitem, que debe ser el `name` de un menuitem raíz
- Añadir el `<menuitem>` con todos los atributos en la posición correcta debajo de su menú raiz, respetando el orden indicado por el número de orden y con la identación

## Si el menú ya existe pero hay que corregirlo

- Comprobar que los menuitems raíz están en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`
- Comprobar que los menuitems hoja están debajo del menuitem raiz al que pertenecen
- Comprobar nombres, atributos y orden.

## Revisión

- [ ] Revisar que todos los nombres de las entradas de menú siguen las convenciones de nomenclatura.
- [ ] Revisar que todos los menuitems están en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` 
- [ ] Revisar que todas las `<action-view>` referenciadas en un menuitem existen.
- [ ] Revisar que para los menuitem raiz el número de orden es correcto y no se repite entre los menus raiz
- [ ] Revisar que el orden físico en el fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` coincide con el número de orden indicado en cada menuitem.
- [ ] Revisar que los menuitems raíz no tienen `action` ni `parent`, y los hoja sí tienen `action` y `parent`.
- [ ] Revisar que el título de cada entrada es claro para el usuario.