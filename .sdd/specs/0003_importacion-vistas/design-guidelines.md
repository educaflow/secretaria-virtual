---
type: design-guidelines
---

# Guías de diseño — importacion-vistas

Este fichero recoge decisiones de diseño específicas de la iniciativa de vistas del sistema de importación de ficheros. 

## 1. Entidades

- La entidad principal para almacenar las importaciones se llamará `TareaImportacion` con los campos:
  - `usuario`: El usuario que realiza la importación.
  - `centro`: El centro al que se refiere la importación. Puede ser nulo.
  - `curso`: El curso al que se refiere la importación. Puede ser nulo.
  - `fechaImportacion`: La fecha en que se realiza la importación.
  - `fechaExportacion`: La fecha de exportación del fichero. Puede ser nulo.
  - `tipoFichero`: El tipo de fichero importado, de tipo `TipoFicheroImportacion`.
  - `estado`: El estado de la importación (correcta o fallida). Boolean, no enum.
  - `log`: Un campo de texto para almacenar el log de la import
- TipoFicheroImportacion: Un enumerado con los valores: 
  - `PROFESOR`
  - `ALUMNO`
  - `FAMILIAR`
  - `PROFESOR_EXTERNO`.
- ImportadorException: Una excepción personalizada para errores relacionados con la importación de ficheros.

## 5. ImportadorFichero

- La importación del tipo de fichero se hará a través de una interfaz `ImportadorFichero` con un método `importar()`.
- Se crearán dos implementaciones de `ImportadorFichero`: `ImportadorUsuarioXML` para los tipos de usuarios con formato XML y `ImportadorUsuarioCSV` para el tipo de usuario con formato CSV.
- Se le pasará el fichero a importar y el tipo de usuario a importar en el constructor de cada implementación de `ImportadorFichero`. No se pasará el centro ni el curso, ya que se obtendrán del fichero o del contexto del importador según corresponda.
- `ImportadorFichero` devolverá un resultado con el número de usuarios importados, el número de errorres, un log con los errores encontrados y el centro y curso para añadirlo a `TareaImportacion`.
- `ImportadorFichero` no se encargará de registrar la importación en la tabla `TareaImportacion`; eso lo hará el código del sistema de importación después de obtener el resultado de `ImportadorFichero`.
- `ImportadorFichero` devolverá `ImportadorException` si existe algún error en la importación.
- Se creará una clase `ImportadorFicheroFactory` para obtener la implementación adecuada según el tipo de fichero:
  - `ImportadorUsuarioXML` para `PROFESOR`, `ALUMNO` y `FAMILIAR`.
  - `ImportadorUsuarioCSV` para `PROFESOR_EXTERNO`.

## 6. Tipo de fichero a importar

- En el asistente de importación, el campo `tipoFichero` se muestra con `widget="SwitchSelect"`. Al ser un campo de tipo enum, el widget renderiza exactamente los cuatro valores del enumerado sin consultar la base de datos.

## 7. Vistas de TareaImportacion

- Usar el skill `k-vistas` para crear las vistas de `TareaImportacion` siguiendo las convenciones de ese skill. 
- Las vistas se almacenan en la carpeta `views` dentro del módulo `importacion`.

### 7.1 Detalle del formulario

- El formulario para ver los detalles o para crear una nueva importación, se deberá abrir como un modal.
- El formulario con los detalles tendrá todos sus campos en modo solo lectura.
- Cuando se añada una nueva importación, el formulario no se cerrará automáticamente después de guardar, sino que se mostrarán los detalles de la importación recién creada en el mismo formulario, sin recargar la página ni abrir un nuevo modal. El usuario podrá revisar el log de la importación y, cuando termine, cerrar el modal con el botón "Aceptar".

### 7.2 Ocultación de la barra de Axelor

- El `<action-view>` debe incluir `<view-param name="show-toolbar-form" value="false"/>` y `<view-param name="forceEdit" value="true"/>` para ocultar la barra superior de Axelor (botón "Close", breadcrumbs, recarga) y forzar siempre el modo edición.
- El `<form>` debe incluir `canBack="false"` para eliminar el botón de volver nativo de Axelor. La navegación la gestionan exclusivamente los botones explícitos del formulario.

### 7.3 Atributos obligatorios en grids

Todos los grids deben incluir `canAdvanceSearch="false"`, `canRefresh="false"` y `allowSearchFields="false"` para ocultar la cabecera de búsqueda avanzada, el botón de recarga y la barra de campos de búsqueda de Axelor.

## 8. Implementación de la lógica de importación

- Los métodos import() de las implementaciones de `ImportadorFichero` devolverán siempre `ImportadorException` con el mensaje `@TODO: Importación no implementada todavía`.

## 9. Alcance estricto

- Implementar únicamente lo que el análisis describe explícitamente. Si una funcionalidad, entidad, vista o regla no está mencionada en el análisis, no se crea. No se añaden características "de paso", no se anticipa funcionalidad futura ni se generalizan casos no pedidos.