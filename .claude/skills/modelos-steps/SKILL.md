---
name: modelos-steps
description: Skill para crear o corregir ficheros XML de modelo de datos para Axelor a partir de una descripción en lenguaje natural o de un PDF con un formulario de datos.
---

# modelos-steps

## Modelos de dominio Axelor: diseño, generación y corrección
- Este skill sirve para diseñar, generar o corregir ficheros XML de modelo de datos de Axelor.
- Los modelos se crean a partir de una descripción en lenguaje natural, un PDF con formulario de datos, o una entidad Java existente que hay que modelar.
- Se siguen las normas definidas en el skill `/modelos-knowledge`

## Tareas a realizar

- Identificar las entidades que hay que modelar y sus relaciones
- Para cada entidad decidir:
  - Su nombre en PascalCase siguiendo las convenciones Java
  - El paquete donde se ubicará: `com.educaflow.{layer}.{subsistema}.db`
  - Si hereda de otra entidad (`extends`) y cuál
  - Sus atributos: nombre en camelCase, tipo XML (`string`, `integer`, `decimal`, `boolean`, `date`, `datetime`, `many-to-one`, `one-to-many`, `many-to-many`, etc.), título, ayuda, validaciones (`required`, `min`, `max`, etc.)
  - Los enumerados (`<enum>`) necesarios con sus items (nombre, título, descripción)
  - Si necesita finders personalizados (`<finder>`) para consultas frecuentes
  - Si necesita código extra en el repositorio (`<extra-code>`) o en el dominio (`<extra-code-model>`)
- Si la entrada es un PDF: analizar cada campo del formulario para extraer nombre, tipo, título, validaciones y relaciones

## En caso de tener que crear el modelo

- Crear el fichero XML con la cabecera `<domain-models>` y los namespaces correctos
- Crear el elemento `<module name="..." package="..."/>` con el nombre del módulo y el paquete
- Para cada entidad crear la etiqueta `<entity name="...">` con los atributos necesarios (`extends` si corresponde)
- Crear los atributos de la entidad con su tipo, nombre, título, ayuda y validaciones
- Crear los `<enum>` necesarios con sus `<item>`
- Añadir `<finder>` si se necesitan consultas personalizadas
- Ubicar el fichero en la carpeta `domains/` del subsistema o sistema correspondiente

## Si el modelo ya existe pero hay que corregirlo

- Corregir los atributos de la entidad (tipo, nombre, título, validaciones) según lo decidido
- Corregir las relaciones (`many-to-one`, `one-to-many`, `mappedBy`, `ref`) para que sean consistentes entre entidades
- Corregir o añadir los enumerados y sus items
- Corregir el paquete o la herencia si es necesario
- Actualizar los finders o extra-code si la lógica ha cambiado

## Revisión
- [ ] Revisar que todos los nombres de entidades y atributos siguen camelCase/PascalCase según corresponda
- [ ] Revisar que los tipos XML son correctos para cada atributo (string, integer, decimal, boolean, date, datetime, many-to-one, etc.)
- [ ] Revisar que las relaciones bidireccionales (`one-to-many` / `many-to-one`) tienen el `mappedBy` correcto
- [ ] Revisar que los atributos `required` están marcados en los campos obligatorios
- [ ] Revisar que los `<enum>` referenciados en la entidad están definidos en el mismo fichero o importados correctamente
- [ ] Revisar que el paquete del `<module>` es coherente con la ubicación del fichero en el proyecto
- [ ] Revisar que la herencia (`extends`) apunta a la clase correcta con su FQCN si está en otro paquete
