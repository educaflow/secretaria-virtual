---
type: analysis
---

## Análisis Funcional: Vistas del sistema de importación

**Tipo:** subsistema
**Capa:** subsystem/importacion
**Descripción:** Vistas para que los administradores consulten el listado histórico de importaciones de ficheros, vean el detalle de cada una y registren una nueva importación subiendo un fichero, dejando registrada toda importación aunque falle.

### Entidades

**TareaImportacion** — Registro de una operación de importación de un fichero realizada por un administrador. Una vez creada, queda persistida de forma permanente con su resultado y un log explicativo.

| Campo | Tipo conceptual | Obligatorio en creación | Nulo permitido | Referencia |
|-------|-----------------|--------------------------|----------------|------------|
| usuario | Referencia a usuario | Sí (automático: usuario logado) | No | Usuario del sistema |
| centro | Referencia a centro educativo | No | Sí | Centro educativo |
| curso | Entero (año académico, ej. 2024) | No | Sí | — |
| fechaImportacion | Fecha y hora | Sí (automático: momento de creación) | No | — |
| fechaExportacion | Fecha y hora | No (siempre nulo en esta iniciativa) | Sí | — |
| tipoFichero | Enumerado `TipoFicheroImportacion` | Sí (introducido por el usuario) | No | — |
| fichero | Fichero adjunto (A1*) | Sí (introducido por el usuario) | No | Fichero del sistema |
| estado | Booleano (true=correcta, false=fallida) | Sí (automático: resultado del proceso) | No | — |
| log | Texto largo multilínea | No | Sí | — |

**Enum `TipoFicheroImportacion`** (lista cerrada): `PROFESOR`, `ALUMNO`, `FAMILIAR`, `PROFESOR_EXTERNO`.

### Dependencias de otros subsistemas

- **subsystem/common** — entidad Centro (referencia opcional en `TareaImportacion.centro`).
- **Usuario del sistema (`com.axelor.auth.db.User`)** — referencia obligatoria en `TareaImportacion.usuario`.
- **Fichero adjunto del sistema (`com.axelor.meta.db.MetaFile`)** — referencia obligatoria en `TareaImportacion.fichero` (A1*).

Sin dependencias circulares.

### Operaciones

1. **Listar importaciones**: el administrador accede al listado completo de importaciones registradas en el sistema, sin filtrado por centro ni por usuario. Se presenta ordenado por fecha de importación descendente (las más recientes primero).
2. **Consultar detalle de una importación**: al seleccionar una importación del listado, el administrador ve sus datos completos en modo solo lectura, incluyendo el estado (de forma legible) y el log con saltos de línea preservados.
3. **Registrar una nueva importación**: desde el listado, el administrador abre el formulario de subida e indica obligatoriamente el tipo de fichero y adjunta el fichero. Sin esos dos datos no puede continuar. Al confirmar, el sistema persiste la importación asignando automáticamente el usuario logado y la fecha de creación, ejecuta el proceso de importación (que en esta iniciativa siempre falla) y rellena `estado` y `log` con el resultado.
4. **Ver el resultado de la importación recién creada**: tras crear la tarea, el mismo formulario pasa a mostrar los detalles del resultado (estado y log) sin cerrarse automáticamente. El administrador cierra la vista pulsando "Aceptar".

No existen operaciones de edición ni de borrado: una importación, una vez creada, es inmutable.

### Vistas

1. **Listado de importaciones**: tabla con todas las importaciones del sistema. Columnas: fecha de importación, tipo de fichero, centro, curso, fecha de exportación, estado y usuario. El estado se muestra de forma legible ("correcta" / "fallida") y nunca como "true / false". Desde el listado se ofrece la acción de registrar una nueva importación y se accede al detalle pulsando una fila. No incluye acciones de edición ni borrado.

2. **Detalle de una importación (solo lectura)**: formulario que muestra todos los campos de la importación seleccionada: usuario, centro, curso, fecha de importación, fecha de exportación, tipo de fichero, fichero subido, estado (legible) y log multilínea. Ningún campo es editable.

3. **Formulario de subida (modal, dos fases)**: ventana modal abierta desde el listado.
   - *Fase de entrada*: pide tipo de fichero (lista desplegable cerrada con los cuatro valores válidos) y fichero a subir. Ambos son obligatorios para poder continuar. No se aplica restricción de extensión: cualquier fichero se acepta y la lógica de importación validará el formato más tarde.
   - *Fase de resultado*: tras confirmar, el mismo modal cambia a vista de resultado mostrando el estado y el log de la importación recién creada, en modo solo lectura. El modal no se cierra automáticamente; el administrador lo cierra pulsando "Aceptar".

### Menús

La iniciativa **reutiliza una entrada de menú ya existente**, no crea menús nuevos:

- Ruta funcional: **Administración SV > Ficheros importación**.
- Visibilidad restringida al grupo de administradores.
- Destino funcional: el listado de importaciones descrito arriba.

### Seguridad

- **Acceso restringido**: solo los usuarios pertenecientes al grupo `admins` de Axelor pueden acceder al listado, al detalle y al formulario de subida. El resto de tipos de usuario (Supervisor, Profesor, Exprofesor, Alumno, Exalumno, Externo, Familiar) no acceden a estas vistas ni ven el menú.
- **Multicentro**: aunque la aplicación es multicentro, los administradores ven todas las importaciones de todos los centros, sin filtrado por centro ni por usuario. El campo `centro` se almacena en cada importación cuando aplica (puede quedar vacío, por ejemplo en `PROFESOR_EXTERNO`), pero no se utiliza como criterio de visibilidad.
- **Inmutabilidad operativa**: ningún usuario, ni siquiera del grupo admins, puede modificar ni eliminar una importación ya registrada, sea cual sea su estado.
- **Trazabilidad de autoría**: el usuario que crea una importación queda registrado automáticamente y no puede asignarse ni cambiarse manualmente.

### Validaciones

| ID    | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario |
|-------|----------|------|--------|--------------------------|---------------------|
| V-001 | tipoFichero | Obligatoriedad | Modelo | Al crear una importación. | "Tipo de fichero: indique el tipo de fichero a importar. Valores válidos: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO." |
| V-002 | tipoFichero | Dominio (lista cerrada) | Modelo | Al crear una importación, el valor debe pertenecer al enumerado. | "Tipo de fichero: el valor '{valor}' no es válido. Valores válidos: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO." |
| V-003 | fichero | Obligatoriedad | Negocio (asumida)* | Al crear una importación. | "Fichero: seleccione el fichero que desea importar." |
| V-004 | usuario, fechaImportacion, estado, log, fechaExportacion | Campos no asignables por el usuario | Negocio | El usuario no puede asignar ni modificar estos campos en ningún momento; los rellena el sistema automáticamente al crear la tarea o tras procesar la importación. | "{campo}: este campo lo gestiona automáticamente el sistema y no puede asignarse desde la interfaz." |
| V-005 | (registro completo) | Edición prohibida | Negocio | Tras la creación, ningún campo de una importación puede modificarse. | "No se pueden modificar las importaciones ya registradas." |
| V-006 | (registro completo) | Borrado prohibido | Negocio | Una importación no puede eliminarse, sea su estado correcta o fallida. | "No se pueden borrar las importaciones registradas." |
| V-007 | (acceso a vistas) | Autorización (acceso) | Negocio | Al intentar acceder al listado, al detalle o al formulario de subida sin pertenecer al grupo `admins`. | "No tiene permiso para acceder a las vistas de importación. Esta funcionalidad está restringida al grupo de administradores." |

### Campos calculados

- **usuario**: se rellena automáticamente al crear la importación con el usuario logado en ese momento. El administrador no puede asignarlo ni cambiarlo.
- **fechaImportacion**: se rellena automáticamente al crear la importación con la fecha y hora del sistema. No es editable.
- **estado**: lo calcula el sistema en función del resultado del proceso de importación. En esta iniciativa, al no estar implementada la lógica de importación, siempre toma valor "fallida" (`false`).
- **log**: lo escribe el sistema con el resultado del proceso de importación. En esta iniciativa contiene siempre el mensaje "@TODO: Importación no implementada todavía".
- **fechaExportacion**: lo rellenará el sistema cuando una importación termine correctamente (en una iniciativa posterior). En esta iniciativa siempre queda vacío porque la importación nunca llega a completarse con éxito.

### Asunciones a confirmar

- **A1\*** (V-003): la entidad `TareaImportacion` incluye un campo `fichero` (tipo `MetaFile`) para almacenar de forma persistente el fichero subido por el administrador, y ese campo es obligatorio en la creación. La asunción se basa en la exigencia de la historia "todas las importaciones se deben guardar, aunque fallen": para que el registro sea auditable es necesario conservar el fichero original. Las design-guidelines no listan este campo de forma explícita, por lo que se marca como asunción a confirmar antes de la fase de diseño.
