---
name: grids-steps
description: Este skill sirve para diseñar, generar o coregir dentro de ficheros XML de vistas de Axelor la etiqueta  `<grid>` de Axelor a partir de un modelo de dominio (entidad). 
---

# grids

Un grid es la vista tabular de Axelor para listar registros (filas) de un modelo. Se define con el tag `<grid>` y dentro contiene etiquetas `<field>` para mostrar los atributos del modelo.

## Grids en Axelor `<grid>`: diseño, generación y corrección
- Este skill sirve para diseñar, generar o corregir los grids `<grid>` que están en ficheros de vistas.
- Estos grids se crean a partir de un modelo de dominio.
- Se siguen las normas definidas en el skill `/grids-knowledge`

## Tareas a realizar.
- Decidir si se pueden crear nuevas entidades desde el grid y en caso afirmativo decifir el título del botón de creación. `newButtonTitle`
- Si se pueden editar las entidades desde el grid o si SOLO pueden ver las entidades desde el grid. 
- Decidir el valor de los atributos `allowSearchFields`, `orderBy`
- Decidir los campos a mostrar en el grid y el orden de esos campos. Es importante elegir bien el orden de los campos para que el grid sea fácil de entender y usar por el usuario. Normalmente el primer campo es un identificador o código, seguido del nombre y luego otros campos relevantes.

## En caso de tener que crear el grid
- Crear la etiqueta `<grid>` con el nombre correcto siguiendo la convención de nomenclatura y los atributos que se han indicado que siempre deben estar.
- Establecer el resto de atributos del grid como `newButtonTitle`, `orderBy` etc. según lo que se haya decidido en la fase de diseño.
- Crear los campos `<field>` dentro del grid con su atributo name y width.

## Si el grid ya existe pero hay que corregirlo
- Corregir la etiqueta `<grid>` con el nombre correcto siguiendo la convención de nomenclatura y los atributos que se han indicado que siempre deben estar.
- Corregir el resto de atributos del grid como `newButtonTitle`, `orderBy` etc. según lo que se haya decidido en la fase de diseño.
- Corregir los campos `<field>` dentro del grid con su atributo name y width.

## Revisión
- [ ] Revisar que los campos `<field>` dentro del grid tienen el atributo name correcto, un width adecuado y que existen en el modelo de dominio.
- [ ] Revisar que el grid creado sigue la convención de nomenclatura y las normas de diseño.
- [ ] Revisar que el valor de los atributos `newButtonTitle`, `orderBy` etc. es correcto según lo que se haya decidido en la fase de diseño.

