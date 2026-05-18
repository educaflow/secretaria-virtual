---
type: user-story
---

# Importacion usuarios csv

Permite importar usuarios autorizados (no de Axelor) a través de un fichero CSV, lo que facilita la incorporación masiva de usuarios autorizados al sistema sin necesidad de hacerlo manualmente uno por uno.

## En una frase

**Como** usuario del grupo `admins` de Axelor,
**quiero** importar usuarios autorizados a través de un fichero CSV,
**para** facilitar la incorporación masiva de usuarios autorizados al sistema sin necesidad de hacerlo manualmente uno por uno.

## Quién interviene

- **[Importador]**: Usuario del grupo admins de Axelor. Puede importar usuarios autorizados a través de un fichero CSV. Puede ver el resultado de la importación, incluyendo los usuarios que se han importado correctamente y los que han fallado.
- **[Sistema]**: Procesa el fichero CSV.

## Conceptos y datos clave

- **Importador**: usuario que realiza la importación. Debe ser del grupo `admins` de Axelor.
- **Fichero CSV**: archivo con los datos a importar. Contiene un listado de dnis.
- **Usuarios autorizados**: usarios que tienen permitido registrarse en la aplicación.
- **Centro**: Centro al que se asignan los usuario autorizados. Será el centro activo del importador **SIEMPRE**.
- **Tipo de usuario**: Tipo de usuario que se asigna a los usuarios autorizados.
- **Curso**: Curso al que se asignan los usuarios autorizados. Será el curso activo del centro **SIEMPRE**.
- **Log de la importación**: Registro detallado de cada paso del proceso de importación, incluyendo los resultados de cada registro (importado correctamente, ignorado o fallido) y cualquier error o incidencia que haya ocurrido durante el proceso.

## Qué tiene que pasar

1. El importador elige el tipo de usuario que quiere importar y el fichero CSV con el listado de dnis a importar.
2. Si el importado no tiene centro asignado, el sistema marca la importación como fallida, lo indica en el log, guarda la importación y termina el proceso.
3. Si el centro asignado al importador no tiene ningún curso, el sistema marca la importación como fallida, lo indica en el log, guarda la importación y termina el proceso.
4. El sistema procesa el fichero CSV y para cada dni:
   1. El sistema comprueba la validez del dni. Si el dni no es válido, se indica en el log que el registro ha fallado por dni no válido y se continúa con el siguiente registro.
   2. Si no existe un usuario autorizado con ese dni, se crea un nuevo usuario autorizado en ese centro, curso y tipo de usuario seleccionado.
   3. Si existe un usuario autorizadado con es dni pero distinto tipo de usuario, centro o curso, se crea un nuevo usuario autorizado añadiendo el centro, curso y tipo de usuario seleccionado.
   4. Si existe un usuario autorizado con ese dni, tipo de usuario, centro y curso, se ignora el registro y se continúa con el siguiente registro.
5. El sistema guarda la importación y muestra el log de la importación, incluyendo el número de usuarios autorizados nuevos creados, el número de usuarios autorizados ignorados y los posibles errores que hayan ocurrido durante el proceso de importación.

## Fuera de alcance (opcional)

- Este proceso de importación sólo se puede realizar a través de un fichero CSV. Existirá otro proceso similar para ficheros XML.
- Por ahora, el único tipo de usuario que se puede importar mediante este proceso es el de profesor externo, aunque **EN UN FUTURO PUEDE SER NECESARIO IMPORTAR OTROS TIPOS DE USUARIO**.
- No se permite modificar ni eliminar usuarios autorizados a través de este proceso de importación. Si un usuario autorizado ya existe, se ignora el registro y se continúa con el siguiente registro.

## Restricciones que no pueden romperse

- El importador debe ser un usuario del grupo `admins` de Axelor.
- El centro al que se asignan los usuarios autorizados debe ser el centro activo del importador.
- El curso al que se asignan los usuarios autorizados debe ser el curso activo del centro.
- El sistema debe procesar el fichero CSV de forma que si un registro falla, no afecte al procesamiento de los demás registros. Es decir, el sistema debe ser capaz de manejar errores individuales sin interrumpir el proceso de importación completo.
- No se pueden eliminar ni modificar usuarios autorizados a través de este proceso de importación, sólo crear nuevos.


## Lo que aporta valor

- Facilita la incorporación masiva de usuarios autorizados al sistema sin necesidad de hacerlo manualmente uno por uno, lo que ahorra tiempo y reduce el riesgo de errores humanos.