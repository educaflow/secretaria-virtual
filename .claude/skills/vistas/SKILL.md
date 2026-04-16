---
name: vistas
description: Usa este skill cuando el usuario quiera crear una vista. Esto incluye crear grids, formularios (forms), acciones (actions). Lanzar cuando el usuario quiera crear una vista, o modificar una vista existente. O cuando diga de crear un XML de las vistas
---

Las vistas son ficheros XML que definen la interfaz de usuario que muestran de datos de Axelor.

Los ficheros de vistas incluyen la definición de diferentes tipos de vistas, como:

* Grid : Muestra una lista de registros en formato de tabla. Usar el skill de `/grid` para crear un grid.
* Formulario (form) : muestra un único registro en un formato de formulario. Usar el skill de `/formulario` para crear un formulario.
* Acciones (actions): Son las acciones disponibles para un objeto de una vista. Usar el skill de `/acciones` para crear acciones.
* Menús (menuitem): Son los menús de la aplicación. Usar el skill de `/menu` para crear menús.


Cada archivo XML de una vista debe tener **exactamente** la siguiente declaración inicial, con el namespace y el esquema correspondiente:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

<!-- Aquí van las definiciones de las vistas, como formularios, grids, acciones, menús, etc. -->
    

</object-views>
``` 