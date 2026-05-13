---
type: user-story
---

# Importacion vistas

Muestra las vistas del sistema de importación de ficheros.

## En una frase

**Como** usuarios del grupo admins de Axelor,
**quiero** ver las vistas del sistema de importación de ficheros,
**para** gestionar la importación de ficheros.

## Quién interviene

- **[Actor 1]**: usuario del grupo admins de Axelor que quiere gestionar la importación de ficheros. Puede ver el listado de importaciones, los detalles de cada importación, y subir un nuevo fichero de importación. No se pueden borrar ni modificar importaciones existentes.

## Conceptos y datos clave

- **Fichero de importación**: Un fichero XML o CSV que el importador sube para importar datos.
- **Estado**: El estado de una importación, que puede ser "correcta" o "fallida".
- **Importador**: El usuario que sube el fichero de importación. Solo los usuarios del grupo admins de Axelor pueden ser importadores.

## Qué tiene que pasar

1. El usuario muestra el listado de importaciones
2. Si pincha en una importación, se muestran los detalles de esa importación
3. En el listado, existe un botón para subir un nuevo fichero de importación. Al pincharlo, se muestra un formulario para subir el fichero y seleccionar el tipo de usuarios que se están importando. Si no se indica el tipo o el fichero, no se puede continuar.
4. Al final de la importación, se muestra el detalle de la importación, incluyendo el estado y un log.

## Fuera de alcance (opcional)

- La lógica de importación de ficheros irá en otra iniciatva. 
- En principio, todas las importaciones de esta iniciativa serán fallidas, ya que el sistema de importación no estará implementado. Sin embargo, se deben mostrar los detalles de la importación, incluyendo el estado (que será "fallida") y un log con el error.

## Restricciones que no pueden romperse

- [Restricción 1]: Solo los usuarios del grupo admins de Axelor pueden acceder a las vistas de importación de ficheros de todos los cursos.
- [Resricción 3]: No se pueden borrar ni modificar importaciones existentes.
- [Restricción 4]: Todas las importaciones se deben guardar, aunque fallen. No se pueden descartar importaciones fallidas.

## Lo que aporta valor

- Los admins pueden gestionar la importación de ficheros, lo que les permite mantener actualizados los datos en el sistema.