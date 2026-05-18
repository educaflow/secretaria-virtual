---
type: analysis
---

## Análisis Funcional: Importación de usuarios autorizados por CSV

**Tipo:** subsistema
**Capa:** `subsystem/importacion` (extiende el subsistema existente)
**Descripción:** Permite a un usuario del grupo `admins` de Axelor importar masivamente usuarios autorizados al sistema cargando un fichero CSV con una lista de DNIs, asignándolos al centro activo del importador, al curso activo de ese centro y al tipo de usuario derivado del tipo de fichero seleccionado.

### Entidades

- **Tarea de importación** (entidad existente, se reutiliza sin cambios estructurales)
  - `usuario` — referencia al usuario que ejecuta la importación. Obligatorio. Asignado por el sistema.
  - `centro` — centro al que pertenecen los UAs creados. Obligatorio. Asignado por el sistema (centro activo del importador).
  - `curso` — entero, año académico al que se asocian los UAs creados. Asignado por el sistema (curso activo del centro). Puede quedar sin asignar si la importación falla globalmente antes de resolverlo.
  - `fechaImportacion` — fecha y hora de ejecución. Obligatorio. Asignado por el sistema.
  - `fechaExportacion` — fecha y hora opcional, gestionada por otro proceso.
  - `tipoFichero` — uno de `PROFESOR`, `ALUMNO`, `FAMILIAR`, `PROFESOR_EXTERNO`. Obligatorio. Lo elige el importador.
  - `fichero` — adjunto con el CSV. Obligatorio. Lo aporta el importador.
  - `estado` — booleano. Obligatorio. `true` si el bucle de procesamiento llegó al final (aunque haya DNIs fallidos individuales); `false` ante un fallo global.
  - `log` — texto largo con resumen y detalle de errores. Asignado por el sistema.
  - Inmutable y no borrable una vez creada.

- **Usuario autorizado** (entidad existente, se modifica una restricción)
  - `centro` — obligatorio.
  - `dni` — obligatorio (normalizado: mayúsculas, sin espacios, ceros corregidos).
  - `tipoUsuario` — referencia al catálogo de tipos de usuario. Obligatorio.
  - `curso` — entero, opcional a nivel de modelo pero siempre informado en altas creadas por esta operación.
  - `fechaExportacion` — fecha. En altas creadas por esta operación se fija al día de la importación.
  - **Cambio de restricción de unicidad**: pasa de `(centro, dni, tipoUsuario)` a `(centro, dni, tipoUsuario, curso)`. Permite coexistir el mismo DNI con el mismo tipo en el mismo centro pero distinto curso.

- **Centro** (existente, solo lectura) — aporta el `curso` activo del centro.
- **Tipo de usuario** (catálogo existente, solo lectura) — se consulta por `codigo` para resolver el tipo a aplicar.

### Dependencias de otros subsistemas

- `subsystem/registrousuario` — para crear y consultar la existencia de `UsuarioAutorizado`.
- `subsystem/common` — para el centro activo del importador, el curso activo del centro y el catálogo de tipos de usuario.
- `base/util` — para validar y normalizar DNIs/NIEs/NIFs.

### Operaciones

- **Lanzar importación de usuarios autorizados por CSV**: el importador selecciona el tipo de fichero y adjunta un CSV. El sistema deduce automáticamente importador, centro (centro activo del importador), curso (curso activo del centro) y fecha. Procesa el fichero línea a línea siguiendo las reglas de procesamiento por DNI y guarda la tarea con su log final y su estado. La tarea siempre se guarda, incluso ante fallos globales. La ejecuta un usuario del grupo `admins`.

- **Consultar resultado de una importación**: muestra en solo lectura los datos identificativos, el estado (correcta/fallida) y el log completo (resumen + detalle de DNIs fallidos). La tarea no puede modificarse ni eliminarse.

- **Listar importaciones**: rejilla en solo lectura con el histórico de importaciones, ordenado por fecha descendente. Permite abrir el detalle o iniciar una nueva.

### Vistas

> Las vistas descritas a continuación **ya existen** en el subsistema `importacion` (formulario con doble panel y listado principal). Se reutilizan sin modificaciones en el alcance de esta iniciativa.

- **Formulario de importación** *(existente)*: pantalla única con dos paneles excluyentes.
  - **Panel de entrada** (visible mientras la tarea no está guardada): permite elegir tipo de fichero y adjuntar el CSV. Botón para lanzar la importación.
  - **Panel de resultado** (visible cuando la tarea ya está guardada): muestra en solo lectura importador, centro, curso, fecha, tipo de fichero, fichero original, estado y log completo.
- **Listado de importaciones** *(existente)*: rejilla en solo lectura, ordenada por fecha descendente, con columnas fecha, tipo de fichero, centro, curso, fecha de exportación, estado e importador. Permite abrir el detalle o crear una nueva; no permite editar ni borrar.

### Menús

> El menú de importación **ya existe** y queda restringido al grupo `admins`. Se reutiliza sin cambios.

- Importación → Tareas de importación → Listado de importaciones (lo ve solo el grupo `admins`). Desde el listado se accede tanto a abrir una importación existente como a crear una nueva.

### Seguridad

- **Importador (grupo `admins`)**: puede crear importaciones, ver el listado y consultar el detalle de cualquier importación. No puede modificar ni eliminar tareas (son inmutables).
- **Resto de usuarios**: no ven el menú ni acceden a las vistas.
- El control de pertenencia al grupo `admins` se ejerce **únicamente** mediante el atributo de grupos del menú/vista; no se reverifica en servidor.
- **Multicentro**: sí. Las importaciones quedan ligadas al centro activo del importador.

### Validaciones (V-XXX)

| ID | Campo(s) | Descripción | Condición | Mensaje al usuario |
|---|---|---|---|---|
| V-001 | `tipoFichero` (TareaImportacion) | Tipo de fichero obligatorio al crear la importación. | `tipoFichero` está vacío al lanzar la importación. | "El tipo de fichero es obligatorio. Valores válidos: Profesor, Alumno, Familiar, Profesor externo." |
| V-002 | `fichero` (TareaImportacion) | Fichero CSV obligatorio al crear la importación. | `fichero` está vacío al lanzar la importación. | "El fichero es obligatorio." |
| V-003 | TareaImportacion (registro completo) | Las importaciones son inmutables. | Se intenta modificar una `TareaImportacion` ya guardada. | "Las importaciones no se pueden modificar una vez creadas." |
| V-004 | TareaImportacion (registro completo) | Las importaciones no se pueden borrar. | Se intenta eliminar una `TareaImportacion`. | "Las importaciones no se pueden eliminar." |
| V-005 | `centro`, `dni`, `tipoUsuario`, `curso` (UsuarioAutorizado) | Unicidad combinada. Ámbito: combinación global de las cuatro columnas. | Se intenta crear un `UsuarioAutorizado` cuya combinación `(centro, dni, tipoUsuario, curso)` ya existe. | "Ya existe un usuario autorizado con DNI '{dni}', tipo de usuario '{tipoUsuario}', centro '{centro}' y curso '{curso}'." |

Notas:
- Los required del modelo (`tipoFichero`, `fichero`, `usuario`, `centro`, `fechaImportacion`, `estado` en TareaImportacion; `centro`, `dni`, `tipoUsuario` en UsuarioAutorizado) que ya cubre el framework no se documentan por separado salvo V-001/V-002, que son los únicos que el importador rellena directamente.
- Los fallos por DNI inválido, duplicado en el fichero, ausencia de centro/curso del importador o excepciones de procesamiento **no son `V-XXX`** porque no bloquean el guardado: la tarea se guarda igualmente y el motivo aparece en el log. Se modelan como `R-XXX`.

### Reglas de negocio (R-XXX)

| ID | Descripción | Entidad | Método | Momento | Más información |
|---|---|---|---|---|---|
| R-001 | Asigna `usuario` de la tarea al usuario autenticado. | TareaImportacion | insert | Antes | Asignación automática del contexto de sesión. |
| R-002 | Asigna `centro` de la tarea al centro activo del importador. | TareaImportacion | insert | Antes | Si el importador no tiene centro activo, ver R-006. |
| R-003 | Asigna `curso` de la tarea al curso activo del centro. | TareaImportacion | insert | Antes | Si el centro no tiene curso activo, ver R-007. |
| R-004 | Asigna `fechaImportacion` con la fecha y hora actuales. | TareaImportacion | insert | Antes | — |
| R-005 | Dispara el procesamiento del fichero al crear la tarea. | TareaImportacion | insert | Después | El procesamiento construye el log y fija `estado`. |
| R-006 | Si el importador no tiene centro activo, la importación queda `estado=false`, se anota el motivo en el log y no se procesa el fichero. | TareaImportacion | insert | Después | Mensaje del log: "El importador no tiene centro activo asignado." |
| R-007 | Si el centro asignado no tiene curso activo, la importación queda `estado=false`, se anota el motivo en el log y no se procesa el fichero. | TareaImportacion | insert | Después | Mensaje del log: "El centro '{centro}' no tiene curso activo configurado." |
| R-008 | Lee el fichero como UTF-8 y descarta la marca BOM si está presente. | TareaImportacion | insert | Después | Parámetro `encoding` con valor por defecto `UTF-8`. |
| R-009 | Ignora silenciosamente líneas vacías o compuestas solo de espacios: no se cuentan ni aparecen en el log. | TareaImportacion | insert | Después | — |
| R-010 | Normaliza cada DNI antes de validarlo y antes de comparar (mayúsculas, espacios, ceros a la izquierda). | TareaImportacion | insert | Después | Reutiliza la utilidad de normalización de DNI/NIE/NIF existente. |
| R-011 | Si el DNI normalizado no es válido, registra un fallo individual en el log con motivo "DNI no válido" y continúa con la siguiente línea. | TareaImportacion | insert | Después | Mensaje del log: "Línea {n}, DNI '{valor recibido}': no es un DNI/NIE/NIF válido." |
| R-012 | Si el DNI normalizado ya apareció antes en el mismo fichero, cada repetición se registra como fallo individual con motivo "DNI duplicado en el fichero" y continúa. | TareaImportacion | insert | Después | La primera aparición se procesa con normalidad; a partir de la segunda, fallo. Mensaje del log: "Línea {n}, DNI '{dni}': duplicado en el fichero." |
| R-013 | Mapea el `tipoFichero` recibido al `TipoUsuario` correspondiente del catálogo. Actualmente `PROFESOR_EXTERNO` → `TipoUsuario` con código `PROFESOR_EXTERNO`. | TareaImportacion | insert | Después | El mapeo se modela de forma que en el futuro otros valores del enum enrutados a CSV declaren su correspondencia. (\*) |
| R-014 | Si no existe `UsuarioAutorizado` con la combinación `(centro de la tarea, DNI normalizado, tipoUsuario resuelto, curso de la tarea)`, crea uno nuevo con esos cuatro datos y `fechaExportacion = fecha actual`. Suma 1 a "creados". | UsuarioAutorizado | insert | Después | Si para ese DNI ya existen UAs con distinta combinación (otro tipo, otro curso, otro centro), se crea de todos modos uno nuevo: la unicidad se evalúa sobre las 4 columnas. |
| R-015 | Si ya existe `UsuarioAutorizado` con exactamente esa combinación, no hace nada y suma 1 a "ignorados". | UsuarioAutorizado | insert | Antes | El registro existente no se modifica. La línea no aparece en el detalle de fallidos. |
| R-016 | Al finalizar el procesamiento, compone el `log` con un bloque de resumen (totales de creados/ignorados/fallidos) y el detalle línea a línea de los fallidos (número de línea, DNI recibido, motivo). | TareaImportacion | insert | Después | Creados e ignorados solo se cuentan, no se listan. |
| R-017 | Si el bucle de procesamiento llega al final, `estado=true` aunque haya DNIs fallidos individuales. | TareaImportacion | insert | Después | `estado=false` queda reservado a fallos globales (R-006, R-007, R-018). |
| R-018 | Si ocurre una excepción no esperada durante el procesamiento (fichero ilegible, error de E/S…), la tarea queda con `estado=false` y se registra el motivo en el log. | TareaImportacion | insert | Después | Fallo global no controlado. |

(\*) marca asunción a confirmar (ver "Asunciones a confirmar").

### Reglas de UI (U-XXX)

> Todas las reglas `U-XXX` siguientes **ya están implementadas** en la vista existente. Se documentan aquí únicamente como descripción del comportamiento esperado de la UI; no requieren trabajo de implementación en esta iniciativa.

| ID | Disparador | Efecto | Campo/Panel afectado | Condición |
|---|---|---|---|---|
| U-001 | continuo | Mostrar el panel de entrada (`tipoFichero` y `fichero` editables, botón "Importar"). | Panel "Entrada" | La tarea aún no está guardada (sin identificador). |
| U-002 | continuo | Ocultar el panel de entrada. | Panel "Entrada" | La tarea ya está guardada. |
| U-003 | continuo | Mostrar el panel de resultado en solo lectura (importador, centro, curso, fecha, tipo de fichero, fichero, estado, log). | Panel "Resultado" | La tarea ya está guardada. |
| U-004 | continuo | Ocultar el panel de resultado. | Panel "Resultado" | La tarea aún no está guardada. |
| U-005 | continuo | Marcar como obligatorios visualmente. | Campos `tipoFichero` y `fichero` | La tarea aún no está guardada. |

### Máquina de estados

No aplica. `TareaImportacion.estado` es un booleano que se fija al final del procesamiento (correcta / fallida) y permanece inmutable.

### Campos calculados

- **`estado`** de la tarea: lo calcula el sistema al final del procesamiento (cubierto por R-006, R-007, R-017, R-018).
- **`log`** de la tarea: lo compone el sistema durante y al final del procesamiento (cubierto por R-006, R-007, R-011, R-012, R-016, R-018).
- **`usuario`, `centro`, `curso`, `fechaImportacion`** de la tarea: derivados del contexto del importador en el momento del alta (R-001 a R-004).
- **`fechaExportacion`** del UsuarioAutorizado creado: fecha actual en el momento de la creación (R-014).

### Asunciones a confirmar

- **A1\* (R-013)**: el mapeo `tipoFichero → código de TipoUsuario` es 1:1 por igualdad de código (p. ej. `PROFESOR_EXTERNO` → `PROFESOR_EXTERNO`). Cuando en el futuro otros valores del enum se enruten a CSV, declararán su correspondencia siguiendo el mismo criterio. Confirmar que esta convención es aceptable.
- **A2\* (V-005, migración)**: el cambio de unicidad de `(centro, dni, tipoUsuario)` a `(centro, dni, tipoUsuario, curso)` se aplica sobre la base de datos existente. Se asume que los datos preexistentes no violan la nueva restricción y que no es necesaria una migración previa. Confirmar.
- **A3\* (curso activo del centro)**: "no tiene curso activo" significa que `Centro.curso` está vacío/sin valor (`null`). Confirmar.
- **A4\* (centro activo del importador)**: "no tiene centro activo" significa que `User.centroActivo` está vacío/sin valor. Confirmar.
- **A5\* (listado y visibilidad multicentro)**: el listado de importaciones se filtra por centro activo del importador, pero el administrador global puede ver importaciones de cualquier centro. Confirmar si esto se resuelve en esta historia o se difiere.
- **A6\* (formato del log)**: el log es texto plano con líneas legibles por humanos, prefijadas por número de línea del CSV; los DNIs creados e ignorados sólo se cuentan, no se listan. Confirmar si se quiere un formato estructurado distinto (JSON, secciones).
- **A7\* (DNI mostrado en errores de duplicado)**: en el detalle de fallidos del log se muestra el DNI tal como aparecía en el fichero (no el normalizado) para facilitar localizar la línea original. Confirmar.
