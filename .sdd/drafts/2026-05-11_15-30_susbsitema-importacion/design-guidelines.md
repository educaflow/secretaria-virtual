---
type: design-guidelines
---

# Guías de diseño — Subsistema importacion 

Este fichero recoge decisiones de diseño específicas del subsistema `importador` que no se deducen del análisis ni están cubiertas por los skills genéricos (`k-sistemas`, `k-vistas`, `k-seguridad`). Son convenciones que un diseñador independiente no escogería por defecto.

## 1. Nombres de tablas

- El nombre de la tabla para almacenar los usuarios importados se llamará `UsuarioAutorizado`.
- El nombre de la tabla para almacenar las importaciones se llamará `TareaImportacion`.

## 2. Campo `estado` de `TareaImportacion`

- El campo `estado` es un `Boolean`, no un enum: `true` = importación correcta, `false` = importación fallida.

## 3. Validaciones de dni

- Usar la clase `DniUtil` para validar los DNIs de los usuarios importados. Antes de validar, usar el método `clean()` de `DniUtil`.

## 4. Log de importación

- El log de la importación se guardará en un campo `log` de tipo `String` en la tabla `TareaImportacion`. No se usará una tabla separada para el log.
- Cuando se termine una importación y se guarde el resultado en la tabla `TareaImportacion`, se mostrará el log en la vista de detalles de esa importación sin cerrar la ventana. El log se mostrará en un área de texto de solo lectura debajo de los datos de la importación. No se abrirá una ventana emergente para mostrar el log.

## 5. ImportadorFichero

- La importación del tipo de fichero se hará a través de una interfaz `ImportadorFichero` con un método `importar()`.
- Se crearán dos implementaciones de `ImportadorFichero`: `ImportadorUsuarioXML` para los tipos de usuarios con formato XML (PROFESOR, ALUMNO, FAMILIAR) y `ImportadorUsuarioCSV` para el tipo de usuario con formato CSV (PROFESOR_EXTERNO).
- Se le pasará el fichero a importar y el tipo de usuario a importar en el constructor de cada implementación de `ImportadorFichero`. No se pasará el centro ni el curso, ya que se obtendrán del fichero o del contexto del importador según corresponda.
- El `ImportadorFichero` se encargará de validar el formato del fichero, extraer los datos necesarios (centro, curso, fechaExportacion, usuarios) y realizar las comprobaciones previas a la importación (existencia de importación previa con los mismos datos, coincidencia del centro con el centro activo del importador).
- `ImportadorFichero` devolverá un resultado con el número de usuarios importados, el número de errorres, un log con los errores encontrados y el centro y curso para añadirlo a `TareaImportacion`.
- `ImportadorFichero` no se encargará de insertar los usuarios en la tabla `UsuarioAutorizado` ni de actualizar los usuarios registrados; solo se encargará de procesar el fichero y devolver el resultado. La inserción de usuarios y la actualización de usuarios registrados se hará en el código del sistema de importación, que llamará a `ImportadorFichero` para obtener los datos procesados.
- `ImportadorFichero` no se encargará de registrar la importación en la tabla `TareaImportacion`; eso lo hará el código del sistema de importación después de obtener el resultado de `ImportadorFichero`.
- `ImportadorFichero` devolverá `ImportadorException` si el formato del fichero no es correcto o si el centro del fichero no coincide con el centro activo del importador. El código del sistema de importación capturará esta excepción y registrará la importación como fallida con el error en el log.
- Se creará una clase `ImportadorFicheroFactory` para obtener la implementación adecuada según el tipo de fichero. 

## 6. Tipo de fichero a importar

- El tipo de fichero a importar se modela como un enumerado Java `TipoFicheroImportacion` declarado en el dominio XML del subsistema `importacion`, con cuatro valores: `PROFESOR_XML`, `ALUMNO_XML`, `FAMILIAR_XML`, `PROFESOR_EXTERNO_CSV`.
- `TareaImportacion` almacena el tipo de fichero en un campo llamado `tipoFichero` de tipo `TipoFicheroImportacion`. Este campo reemplaza al antiguo campo `tipoUsuario` (M2O a `TipoUsuario`) que existía en iteraciones anteriores — `TareaImportacion` no tiene ningún campo M2O a `TipoUsuario`.
- En el asistente de importación, el campo `tipoFichero` se muestra con `widget="SwitchSelect"`. Al ser un campo de tipo enum, el widget renderiza exactamente los cuatro valores del enumerado sin consultar la base de datos.
- El servicio `TareaImportacionServiceImpl` es el responsable de resolver el `TipoUsuario` real (por código) a partir del valor del enumerado cuando lo necesite para insertar `UsuarioAutorizado` o actualizar usuarios registrados. `UsuarioAutorizado` sigue usando un M2O a `TipoUsuario` sin cambios.

## 7. Vistas de TareaImportacion

Las vistas se almacenan en la carpeta `views` dentro del módulo `importacion`.

### 7.1 Arquitectura: formulario único para wizard y detalle

No se usa un popup wizard separado. Existe un único formulario `@Main-form` que actúa como wizard (registro nuevo) y como vista de detalle (registro existente) según si `id` es nulo o no. El grid tiene `canNew="true"` y abre directamente ese formulario.

- Campos y botones del **wizard** (sólo cuando `id == null`): `tipoFichero` (SwitchSelect), `fichero`, botón "Cancelar" (`onClick="back"`), botón "Importar" (`onClick` → acción de guardado).
- Campos y botones del **detalle** (sólo cuando `id != null`): `centro`, `curso`, `usuario`, fecha de importación, log; botón "Aceptar" (`onClick="back"`).
- Se usa `showIf="id != null"` / `hideIf="id != null"` para controlar qué elementos son visibles en cada estado.

### 7.2 Ocultación de la barra de Axelor

- El `<action-view>` debe incluir `<view-param name="show-toolbar-form" value="false"/>` y `<view-param name="forceEdit" value="true"/>` para ocultar la barra superior de Axelor (botón "Close", breadcrumbs, recarga) y forzar siempre el modo edición.
- El `<form>` debe incluir `canBack="false"` para eliminar el botón de volver nativo de Axelor. La navegación la gestionan exclusivamente los botones explícitos del formulario.

### 7.3 Atributos obligatorios en grids

Todos los grids deben incluir `canAdvanceSearch="false"`, `canRefresh="false"` y `allowSearchFields="false"` para ocultar la cabecera de búsqueda avanzada, el botón de recarga y la barra de campos de búsqueda de Axelor.

## 8. Cuándo crear servicio y repositorio para una entidad

No toda entidad necesita su propio servicio o repositorio. La regla es:

- **Crear servicio y repositorio** solo cuando la entidad tiene identidad propia, datos adicionales más allá de las claves foráneas, o lógica de negocio propia. Ejemplo: `CentroUsuario` tiene significado por sí mismo (la pertenencia de un usuario a un centro).
- **No crear servicio ni repositorio** para entidades que actúan únicamente como tabla de enlace entre otras dos (many-to-many sin datos propios). Ejemplo: `CentroUsuarioTipoUsuario` es solo una asociación — su gestión debe ir a través del servicio de la entidad principal (`CentroUsuarioService`), no a través de un servicio o repositorio propio.

## 9. Comunicación entre subsistemas

- Cuando el subsistema `importacion` necesita datos o acciones de otro subsistema (p.ej. `registrousuario`, `common`), nunca usa directamente los repositorios de ese subsistema. Siempre delega en el servicio correspondiente (`UsuarioAutorizadoService`, `CentroService`, etc.). Los repositorios son un detalle de implementación interno de cada subsistema.

## 10. Calidad del código

- Los métodos no mezclan responsabilidades. Cuando un método hace más de una cosa, delega en métodos privados con nombre descriptivo antes de añadir lógica inline.
- Se crean clases colaboradoras cuando una responsabilidad es suficientemente compleja o cohesiva como para merecer su propia clase (p.ej. la lógica de actualización de tipos de usuario registrados tras una importación XML es un buen candidato). La decisión la toma el diseñador según la complejidad real de cada responsabilidad.

## 11. Alcance estricto

- Implementar únicamente lo que el análisis describe explícitamente. Si una funcionalidad, entidad, vista o regla no está mencionada en el análisis, no se crea. No se añaden características "de paso", no se anticipa funcionalidad futura ni se generalizan casos no pedidos.