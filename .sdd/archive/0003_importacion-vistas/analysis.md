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
3. **Registrar una nueva importación**: desde el listado, el administrador pulsa "Importar nueva" para abrir el formulario de creación e indica obligatoriamente el tipo de fichero y adjunta el fichero. Sin esos dos datos no puede continuar. Al confirmar, el sistema persiste la importación asignando automáticamente el usuario logado y la fecha de creación, ejecuta el proceso de importación (que en esta iniciativa siempre falla) y rellena `estado` y `log` con el resultado.
4. **Ver el resultado de la importación recién creada**: tras crear la tarea, el mismo formulario pasa a mostrar los detalles del resultado (estado y log) en modo solo lectura. El administrador vuelve al listado pulsando "Aceptar".

No existen operaciones de edición ni de borrado: una importación, una vez creada, es inmutable.

### Vistas

1. **Listado de importaciones**: tabla con todas las importaciones del sistema. Columnas: fecha de importación, tipo de fichero, centro, curso, fecha de exportación, estado y usuario. El estado se muestra de forma legible ("Correcta" / "Fallida") y nunca como "true / false". Desde el listado se ofrece el botón "Importar nueva" para registrar una nueva importación y se accede al detalle pulsando una fila. No incluye acciones de edición ni borrado.

2. **Detalle de una importación (solo lectura)**: el mismo formulario de creación, una vez guardada la tarea (`id != null`), muestra todos los campos en modo solo lectura: usuario, centro, curso, fecha de importación, fecha de exportación, tipo de fichero, fichero subido, estado (legible) y log multilínea. Ningún campo es editable.

3. **Formulario de creación (inline, dos fases)**: se abre inline al crear una nueva importación desde el botón "Importar nueva" del listado (no es una ventana modal popup).
   - *Fase de entrada* (visible cuando `id == null`): pide tipo de fichero (selector cerrado con los cuatro valores válidos, widget SwitchSelect) y fichero a subir. Ambos son obligatorios para poder continuar.
   - *Fase de resultado* (visible cuando `id != null`): tras confirmar, el mismo formulario conmuta al panel de resultado mostrando el estado y el log de la importación recién creada, en modo solo lectura. El administrador vuelve al listado pulsando "Aceptar".

### Menús

La iniciativa **reutiliza una entrada de menú ya existente**, no crea menús nuevos:

- Ruta funcional: **Administración SV > Ficheros importación**.
- Visibilidad restringida al grupo de administradores.
- Destino funcional: el listado de importaciones descrito arriba.

### Seguridad

- **Acceso restringido**: solo los usuarios pertenecientes al grupo `admins` de Axelor pueden acceder al listado, al detalle y al formulario de creación. El resto de tipos de usuario no acceden a estas vistas ni ven el menú.
- **Multicentro**: aunque la aplicación es multicentro, los administradores ven todas las importaciones de todos los centros, sin filtrado por centro ni por usuario. El campo `centro` se almacena en cada importación cuando aplica, pero no se utiliza como criterio de visibilidad.
- **Inmutabilidad operativa**: ningún usuario, ni siquiera del grupo admins, puede modificar ni eliminar una importación ya registrada, sea cual sea su estado.
- **Trazabilidad de autoría**: el usuario que crea una importación queda registrado automáticamente y no puede asignarse ni cambiarse manualmente.

### Validaciones

| ID    | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario |
|-------|----------|------|--------|--------------------------|---------------------|
| V-001 | tipoFichero | Obligatoriedad | Modelo | Al crear una importación. | Cliente: "El tipo de fichero es obligatorio. Valores válidos: Profesor, Alumno, Familiar, Profesor externo". Servidor: "El tipo de fichero es obligatorio. Valores válidos: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO." |
| V-002 | tipoFichero | Dominio (lista cerrada) | Modelo | Al crear una importación, el valor debe pertenecer al enumerado. | Garantizado por el tipo Java enum y el widget SwitchSelect; no hay comprobación explícita adicional en el servidor. |
| V-003 | fichero | Obligatoriedad | Negocio (asumida)* | Al crear una importación. | Cliente y servidor: "El fichero es obligatorio." |
| V-004 | usuario, fechaImportacion, estado, log, fechaExportacion | Campos no asignables por el usuario | Negocio | El usuario no puede asignar ni modificar estos campos; los rellena el sistema al crear la tarea o tras procesar la importación. | Los campos son sobrescritos siempre por el servidor mediante `AllowProperties` + `fireActionRule_asignarCamposSistema`. |
| V-005 | (registro completo) | Edición prohibida | Negocio | Tras la creación, ningún campo de una importación puede modificarse. | "Las importaciones ya registradas no se pueden modificar." |
| V-006 | (registro completo) | Borrado prohibido | Negocio | Una importación no puede eliminarse. | "Las importaciones no se pueden eliminar." |
| V-007 | (acceso a vistas) | Autorización (acceso) | Negocio | Al intentar acceder al listado sin pertenecer al grupo `admins`. | Menú oculto; el menuitem lleva `groups="admins"`. |

### Campos calculados

- **usuario**: se rellena automáticamente al crear la importación con el usuario logado en ese momento. El administrador no puede asignarlo ni cambiarlo.
- **fechaImportacion**: se rellena automáticamente al crear la importación con la fecha y hora del sistema. No es editable.
- **estado**: lo calcula el sistema en función del resultado del proceso de importación. En esta iniciativa, al no estar implementada la lógica de importación, siempre toma valor "fallida" (`false`).
- **log**: lo escribe el sistema con el resultado del proceso de importación. En esta iniciativa contiene siempre el mensaje "@TODO: Importación no implementada todavía".
- **fechaExportacion**: lo rellenará el sistema cuando una importación termine correctamente (en una iniciativa posterior). En esta iniciativa siempre queda vacío.

### Asunciones a confirmar

- **A1\*** (V-003): confirmada. La entidad `TareaImportacion` incluye el campo `fichero` (`MetaFile`), obligatorio en creación.

## Notas de cierre (as-built)

Cambios aplicados respecto al draft original:
- **Vista 3**: se cambió de "ventana modal popup" a "formulario inline" (la conmutación entre fase entrada y fase resultado se hace mediante paneles con `showIf` dentro del mismo `@Main-form`; no existe ventana popup separada).
- **Operación 4**: "cerrar el modal" reemplazado por "volver al listado pulsando Aceptar" (botón hace `back`, no `close`).
- **V-001 mensaje**: el mensaje difiere entre capa cliente ("Valores válidos: Profesor, Alumno, ...") y servidor ("Valores válidos: PROFESOR, ALUMNO, ..."); se especifican ambos.
- **V-002**: no hay comprobación explícita adicional en servidor; la garantía la da el tipo enum Java y el widget SwitchSelect. Se eliminó la descripción de un mensaje de error específico.
- **V-003 mensaje**: simplificado a "El fichero es obligatorio." en lugar de "Fichero: seleccione el fichero que desea importar."
- **V-007 mensaje**: el acceso está controlado por ocultación de menú (`groups="admins"`), no por un mensaje de error mostrado al usuario.
- **A1\***: marcada como confirmada (el campo `fichero` se implementó como obligatorio).