---
name: Vistas de Axelor
description: Crear un fichero XML de modelo de datos para Axelor a partir de una descripción en lenguaje natural o según un PDF con un formulario de datos
---

Las vistas son ficheros XML que definen la interfaz de usuario que representan los modelos de datos de Axelor.

Los ficheros de vistas incluyen la definición de diferentes tipos de vistas, como:


* Formulario : muestra un único registro en un formato de formulario.
* Grid : muestra una lista de registros en formato de tabla.
* Acciones : Son las acciones disponibles para un objeto de una vista.

Las vistas de grid y formulario son las principales.

Al igual que los modelos de objetos, las vistas también se definen utilizando el formato XML.

Cada archivo debe tener la siguiente declaración inicial, con el namespace y el esquema correspondiente:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

  <!-- views definitions here -->

</object-views>
``` 