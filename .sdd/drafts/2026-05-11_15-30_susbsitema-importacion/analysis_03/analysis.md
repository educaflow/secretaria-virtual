---
type: analysis
---

## Análisis Funcional: Importación de usuarios

**Tipo:** subsistema
**Capa:** subsystem/importacion
**Descripción:** Permite a los administradores de la aplicación importar masivamente DNIs de usuarios autorizados a registrarse en un centro a partir de ficheros XML (PROFESOR/ALUMNO/FAMILIAR) o CSV (PROFESOR_EXTERNO). Cada importación queda registrada como una tarea inmutable con su fichero original y su log; tras una importación correcta el sistema actualiza automáticamente los tipos de usuario asignados a los usuarios ya registrados del centro (incluyendo el paso a "EX" para los que dejan de aparecer en imports posteriores del mismo curso).

### Entidades

#### `TareaImportacion` (nueva)

Registro de cada intento de importación (correcto o fallido). Inmutable tras su creación: no se admite editar ni borrar desde la UI ni desde la API.

| Campo | Tipo | Restricciones |
|---|---|---|
| `fechaImportacion` | LocalDateTime | Requerido. Momento en que se inicia el procesado. |
| `centro` | many-to-one a `Centro` | Requerido. Centro al que se importa (de `<centro codigo>` del XML, o centro activo del importador en CSV). |
| `tipoUsuario` | many-to-one a `TipoUsuario` | Requerido. Uno de PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO. |
| `nombreFichero` | String | Requerido. Nombre original del fichero subido. |
| `fichero` | many-to-one a `MetaFile` | Requerido. Fichero original tal cual se subió. |
| `usuarioImportador` | many-to-one a `User` | Requerido. Administrador que ejecutó la importación. |
| `curso` | Integer | Requerido. Curso académico al que aplica (del XML o `Centro.curso` en CSV). |
| `fechaExportacion` | LocalDateTime | Requerido. Fecha declarada en el XML o instante de la importación en CSV. |
| `estado` | Boolean | Requerido. `true` = correcta; `false` = fallida (en cualquier punto). |
| `log` | Text (largo) | Requerido. Texto humano con resumen numérico, errores por DNI, error global previo si lo hubo y motivo de la reversión si ocurrió. |

#### `UsuarioAutorizado` (existente en `subsystem/registrousuario`; esta iniciativa modifica su esquema)

Fila por cada DNI autorizado a registrarse en un centro con un tipo, curso y fecha de exportación concretos. Inmutable: solo se insertan filas; nunca se actualizan ni borran.

| Campo | Tipo | Restricciones |
|---|---|---|
| `centro` | many-to-one a `Centro` | Requerido. |
| `dni` | String | Requerido. DNI con formato válido. |
| `tipoUsuario` | many-to-one a `TipoUsuario` | Requerido. Uno de PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO. |
| `curso` | Integer | Requerido. *(cambio: pasa a ser obligatorio y entra en la unicidad)* |
| `fechaExportacion` | LocalDateTime | Requerido. *(cambio: pasa de date a datetime y entra en la unicidad)* |

Cambios sobre el esquema actual: se **elimina** la unique-constraint `(centro, dni, tipoUsuario)` y se **añade** la nueva `(centro, dni, tipoUsuario, curso, fechaExportacion)`.

### Dependencias de otros subsistemas

- `subsystem/common` — reutiliza `Centro`, `TipoUsuario`, `CentroUsuario`, `CentroUsuarioTipoUsuario`.
- `subsystem/registrousuario` — modifica el esquema de `UsuarioAutorizado` y es productor de sus registros.
- `base/util` — `DniUtil` para validar el formato de DNIs.

### Infraestructura disponible (a reutilizar por el diseño)

- Existen esquemas XSD en `resources/data-import/schemas/` que ya describen el formato esperado: `profesores.xsd`, `alumnos.xsd` y `familiares.xsd`. Cada XSD valida la raíz `<centro>` con atributos `codigo`, `curso`, `fechaExportacion` obligatorios, y exige al menos un elemento hijo dentro de la sección correspondiente (`<docente>`, `<alumno>` o `<familiar>`) con atributo `documento` requerido.
- Existe la utilidad de proyecto para validación de XML contra esquema (lectura y validación), que el diseño puede usar como mecanismo concreto para la validación de formato XML.

### Operaciones

**Op-1. Listar tareas de importación.** Cualquier administrador consulta el listado de todas las `TareaImportacion` de todos los centros, ordenado por fecha de importación descendente. Solo lectura.

**Op-2. Ver detalle de una tarea de importación.** El administrador abre una tarea concreta y ve todos sus datos y el log completo en modo solo lectura.

**Op-3. Descargar el fichero original de una importación.** Desde el detalle, el administrador descarga el `MetaFile` original tal como se subió.

**Op-4. Iniciar y procesar una nueva importación.** El administrador pulsa "Importar" en la barra del listado, lo que abre un asistente que pide tipo de usuario (uno de PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO) y fichero. Al confirmar, el sistema procesa la importación de forma **síncrona** siguiendo esta secuencia:

1. **Validaciones previas y parseo**: formato del fichero según tipo (contra el esquema XSD para XML, contra reglas de líneas de DNIs para CSV), coherencia tipo↔contenido XML, coincidencia centro XML↔centro activo (solo XML) y duplicidad de importación previa.
2. **Procesado de DNIs**: por cada DNI del fichero, los inválidos se omiten y se anotan en el log; los duplicados intra-fichero se insertan una sola vez y se anotan en el log; los válidos se insertan en `UsuarioAutorizado` con `(centro, dni, tipoUsuario, curso, fechaExportacion)` según el origen.
3. **Actualización de usuarios registrados** según las reglas detalladas más abajo.
4. **Cierre**: se persiste la `TareaImportacion` con `estado=true` y log con el resumen. Si alguna validación previa falla, se persiste con `estado=false` y el motivo en el log (sin insertar nada). Si la actualización (paso 3) lanza una excepción técnica, se revierten todos los `UsuarioAutorizado` insertados y los cambios sobre usuarios registrados; la `TareaImportacion` sí se persiste con `estado=false` y el error en el log.
5. **Apertura del detalle**: al terminar, el asistente abre automáticamente el detalle (solo lectura) de la `TareaImportacion` recién creada.

**Op-5. Listar usuarios autorizados.** El administrador consulta el listado de todos los `UsuarioAutorizado` de todos los centros en modo solo lectura, desde un menú propio.

### Origen de campos en la operación de importación

- **XML (PROFESOR/ALUMNO/FAMILIAR)**: `centro` = centro activo del importador (validado contra `<centro codigo>` del XML); `curso` = atributo `curso` del XML (se acepta cualquier valor; no se compara con `Centro.curso`); `fechaExportacion` = atributo `fechaExportacion` del XML (patrón `dd/MM/yyyy HH:mm:ss`).
- **CSV (PROFESOR_EXTERNO)**: `centro` = centro activo del importador; `curso` = `Centro.curso` del centro activo; `fechaExportacion` = instante del inicio del procesado. Si la primera línea del CSV NO es un DNI válido (según `DniUtil`), se asume cabecera y se descarta silenciosamente; si es DNI válido, se procesa como una línea más.

### Reglas de actualización de usuarios registrados

#### Para importaciones XML (PROFESOR / ALUMNO / FAMILIAR)

Sea `T` el tipo importado y `EX_T` su contrapartida (PROFESOR↔EXPROFESOR, ALUMNO↔EXALUMNO, FAMILIAR↔EXFAMILIAR).

**Universo a evaluar**: unión de (usuarios registrados del centro cuyo DNI aparece en el fichero) ∪ (usuarios registrados del centro que actualmente tienen el tipo `T` o el tipo `EX_T`).

Para cada usuario del universo:
- **Actual**: existe una fila `UsuarioAutorizado` con `(tipo=T, dni, centro)` cuya `fechaExportacion` sea la mayor registrada para esa terna y el curso del fichero.
- **Anterior**: existe una fila `UsuarioAutorizado` con `(tipo=T, dni, centro)` cuya `fechaExportacion` sea anterior a la mayor para esa terna y el curso del fichero.

| Actual | Anterior | Acción sobre el usuario registrado |
|---|---|---|
| No | No | Elimina el tipo `T` y el tipo `EX_T` si los tuviera *(caso defensivo, no debería ocurrir).* |
| No | Sí | Añade el tipo `EX_T` y elimina el tipo `T` si lo tenía. |
| Sí | No | Añade el tipo `T` y elimina el tipo `EX_T` si lo tenía. |
| Sí | Sí | Añade el tipo `T` y elimina el tipo `EX_T` si lo tenía. |

Si un DNI del fichero no se corresponde con ningún `User` registrado, no se hace nada sobre usuarios registrados (queda solo el rastro en `UsuarioAutorizado`).

Restricción transversal: un usuario nunca puede tener simultáneamente un tipo base y su `EX` correspondiente.

#### Para importaciones CSV (PROFESOR_EXTERNO)

Para cada DNI válido del fichero:
- Si existe un `User` con ese DNI y tiene `CentroUsuario` en el centro activo del importador, se le añade el tipo `PROFESOR_EXTERNO` si no lo tenía.
- Si existe un `User` con ese DNI pero no tiene `CentroUsuario` en el centro activo, se crea automáticamente el `CentroUsuario` y se le añade el tipo `PROFESOR_EXTERNO`.
- Si no existe ningún `User` con ese DNI, no se hace nada sobre usuarios registrados (queda el rastro en `UsuarioAutorizado`).

No se aplica la tabla XML, no se generan tipos `EX_PROFESOR_EXTERNO` ni se retira `PROFESOR_EXTERNO` a quienes ya no aparecen. La caducidad de `PROFESOR_EXTERNO` se gestiona en una historia futura "cambiar curso académico".

### Vistas

- **Listado de tareas de importación** — Tabla con columnas: `fechaImportacion`, `centro`, `tipoUsuario`, `nombreFichero`, `usuarioImportador`, `estado`. Ordenada por `fechaImportacion` descendente. Solo lectura, sin crear/editar/borrar; incluye un botón "Importar" en la barra que abre el asistente. La ven los administradores; muestra todos los centros sin filtro.
- **Asistente de importación** — Formulario emergente con dos campos requeridos: `tipoUsuario` (selector con 4 valores: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO) y `fichero`. Mientras alguno esté vacío, no se puede confirmar. Al confirmar, ejecuta el procesado síncrono y abre el detalle de la `TareaImportacion` resultante.
- **Detalle de tarea de importación** — Solo lectura. Muestra todos los campos de la tarea y el `log` completo en bloque amplio. Ofrece la acción "Descargar fichero original" para obtener el `MetaFile`. Se abre automáticamente tras una importación.
- **Listado de usuarios autorizados** — Tabla con columnas: `centro`, `dni`, `tipoUsuario`, `curso`, `fechaExportacion`. Solo lectura, sin crear/editar/borrar. La ven los administradores; muestra todos los centros sin filtro.

### Menús

- **Administración SV > Ficheros importación** (menuitem existente `administracionSv-importacion-menuitem`, grupo `admins`) → abre el listado de tareas de importación.
- **Administración SV > Usuarios autorizados** (nuevo menuitem bajo `administracionSv-menuitem`, grupo `admins`) → abre el listado de usuarios autorizados.

### Seguridad

- Solo los administradores de la aplicación (grupo `admins`) pueden acceder a los menús, listar tareas, ver detalle, descargar el fichero original, ejecutar una nueva importación y listar usuarios autorizados.
- Ningún otro tipo de usuario tiene acceso a este subsistema.
- **Multicentro**: no. Los listados muestran datos de todos los centros sin filtrado por centro activo. El centro de la importación se determina automáticamente (centro activo del importador para CSV, atributo del fichero para XML).
- `TareaImportacion` y `UsuarioAutorizado` son inmutables: ningún usuario (ni siquiera administrador) puede crearlos manualmente, editarlos ni eliminarlos, ni desde la UI ni desde la API; solo se producen mediante el flujo de importación.

### Validaciones

| ID | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario |
|---|---|---|---|---|---|
| V-001 | `TareaImportacion.fechaImportacion` | Requerido | Modelo | Siempre al crear | "La fecha de importación es obligatoria." |
| V-002 | `TareaImportacion.centro` | Requerido | Modelo | Siempre al crear | "El centro es obligatorio." |
| V-003 | `TareaImportacion.tipoUsuario` | Requerido | Modelo | Siempre al crear | "El tipo de usuario es obligatorio." |
| V-004 | `TareaImportacion.nombreFichero` | Requerido | Modelo | Siempre al crear | "El nombre del fichero es obligatorio." |
| V-005 | `TareaImportacion.fichero` | Requerido | Modelo | Siempre al crear | "El fichero es obligatorio." |
| V-006 | `TareaImportacion.usuarioImportador` | Requerido | Modelo | Siempre al crear | "El usuario importador es obligatorio." |
| V-007 | `TareaImportacion.curso` | Requerido | Modelo | Siempre al crear | "El curso es obligatorio." |
| V-008 | `TareaImportacion.fechaExportacion` | Requerido | Modelo | Siempre al crear | "La fecha de exportación es obligatoria." |
| V-009 | `TareaImportacion.estado` | Requerido | Modelo | Siempre al crear | "El estado de la importación es obligatorio." |
| V-010 | `TareaImportacion.log` | Requerido | Modelo | Siempre al crear | "El log de la importación es obligatorio." |
| V-011 | `UsuarioAutorizado.centro` | Requerido | Modelo | Siempre al crear | "El centro del usuario autorizado es obligatorio." |
| V-012 | `UsuarioAutorizado.dni` | Requerido | Modelo | Siempre al crear | "El DNI del usuario autorizado es obligatorio." |
| V-013 | `UsuarioAutorizado.tipoUsuario` | Requerido | Modelo | Siempre al crear | "El tipo de usuario del usuario autorizado es obligatorio." |
| V-014 | `UsuarioAutorizado.curso` | Requerido | Modelo | Siempre al crear | "El curso del usuario autorizado es obligatorio." |
| V-015 | `UsuarioAutorizado.fechaExportacion` | Requerido | Modelo | Siempre al crear | "La fecha de exportación del usuario autorizado es obligatoria." |
| V-016 | `UsuarioAutorizado` (`centro`, `dni`, `tipoUsuario`, `curso`, `fechaExportacion`) | Unicidad | Modelo | Ámbito: combinación completa. La misma terna `(centro, dni, tipoUsuario)` puede repetirse si difiere en curso o en fecha de exportación. | "Ya existe un usuario autorizado con DNI '{dni}', tipo '{tipoUsuario}' en el centro '{centro}' para el curso '{curso}' y la fecha de exportación '{fechaExportacion}'." |
| V-017 | `UsuarioAutorizado.dni` | Formato | Catálogo (`DniUtil`) | Al procesar cada DNI del fichero. Los DNIs con formato inválido se omiten y se anotan en el log; la importación continúa con el resto. | "El DNI '{valor}' no tiene un formato válido y se ha omitido." |
| V-018 | Asistente: `tipoUsuario` | Requerido | Negocio (asumida)* | Al confirmar el asistente | "Debe seleccionar el tipo de usuario antes de continuar." |
| V-019 | Asistente: `tipoUsuario` | Dominio finito | Negocio (asumida)* | Al confirmar el asistente; debe ser uno de los 4 valores admitidos | "El tipo de usuario '{valor}' no es válido. Valores admitidos: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO." |
| V-020 | Asistente: `fichero` | Requerido | Negocio (asumida)* | Al confirmar el asistente | "Debe adjuntar un fichero antes de continuar." |
| V-021 | Fichero XML | Formato | Catálogo (esquemas XSD del proyecto) | Si `tipoUsuario` ∈ {PROFESOR, ALUMNO, FAMILIAR}, el contenido del fichero debe cumplir el esquema correspondiente (`profesores.xsd`/`alumnos.xsd`/`familiares.xsd`): raíz `<centro>` con atributos `codigo`, `curso`, `fechaExportacion`, y al menos un elemento hijo dentro de la sección esperada (`<docente>`/`<alumno>`/`<familiar>`) con atributo `documento`. La importación se persiste como fallida con el motivo en el log. | "El fichero no cumple el formato XML esperado para el tipo '{tipoUsuario}': {detalleValidación}." |
| V-022 | Fichero CSV | Formato | Negocio (asumida)* | Si `tipoUsuario` = PROFESOR_EXTERNO, el contenido debe ser legible como texto con líneas de DNIs (cabecera opcional). La importación se persiste como fallida con este mensaje en el log. | "El fichero no tiene el formato CSV esperado: una línea por DNI, con cabecera opcional." |
| V-023 | Coherencia tipo UI ↔ estructura XML | Coherencia | Negocio (asumida)* | Si el tipo seleccionado es PROFESOR y el XML no contiene `<docentes>`, ALUMNO y no contiene `<alumnos>`, o FAMILIAR y no contiene `<familiares>`. La importación se persiste como fallida. | "El tipo de usuario seleccionado '{tipoUsuario}' no coincide con el contenido del fichero." |
| V-024 | Centro del XML ↔ centro activo del importador | Coherencia | Negocio (asumida)* | Solo para XML. El atributo `codigo` de `<centro>` debe coincidir con el `code` del centro activo del importador. La importación se persiste como fallida. | "El centro del fichero '{codigoFichero}' no coincide con el centro activo del importador '{codigoCentroActivo}'." |
| V-025 | Importación (`centro`, `tipoUsuario`, `fechaExportacion`, `curso`) | Unicidad | Negocio (asumida)* | Antes de procesar. Ámbito: la combinación completa. La importación se persiste como fallida si ya existe otra `TareaImportacion` correcta con la misma combinación. | "Ya existe una importación correcta con la misma fecha de exportación '{fechaExportacion}', tipo '{tipoUsuario}' y curso '{curso}' para el centro '{centro}'." |
| V-026 | Autorización de ejecución y acceso | Autorización | Negocio (asumida)* | Cualquier intento de listar tareas o usuarios autorizados, abrir el asistente, ver detalle o descargar fichero por un usuario que no pertenezca al grupo `admins`. | "Solo los administradores de la aplicación pueden gestionar importaciones de usuarios." |
| V-027 | `TareaImportacion` (alta manual, edición, borrado) | Inmutabilidad | Negocio (asumida)* | Cualquier intento de crear manualmente, editar o borrar una `TareaImportacion`, vía UI o API. Solo se crea desde el flujo de importación. | "Las tareas de importación no pueden crearse, modificarse ni eliminarse manualmente." |
| V-028 | `UsuarioAutorizado` (alta manual, edición, borrado) | Inmutabilidad | Negocio (asumida)* | Cualquier intento de crear manualmente, editar o borrar un `UsuarioAutorizado`, vía UI o API. Solo se crea desde el flujo de importación. | "Los usuarios autorizados no pueden crearse, modificarse ni eliminarse manualmente." |
| V-029 | DNI duplicado dentro del mismo fichero | Información | Negocio (asumida)* | Si un DNI aparece más de una vez en el mismo fichero, se inserta una sola vez en `UsuarioAutorizado` y se anota en el log. | "El DNI '{valor}' aparece duplicado en el fichero; se ha registrado una sola vez." |
| V-030 | Persistencia de importación fallida (validación previa) | Trazabilidad | Negocio (asumida)* | Si V-021 a V-025 fallan, la `TareaImportacion` se persiste con `estado=false`, el fichero original adjunto y log con el motivo. No se insertan `UsuarioAutorizado` ni se modifican usuarios registrados. | (Se registra en el log de la propia tarea.) |
| V-031 | Reversión por excepción técnica en la actualización de registrados | Integridad transaccional | Negocio (asumida)* | Si durante la actualización de tipos de usuarios registrados se produce una excepción técnica, se revierten todos los `UsuarioAutorizado` insertados y los cambios en `CentroUsuario`/`CentroUsuarioTipoUsuario` de esta importación. La `TareaImportacion` se persiste con `estado=false` y el error en el log. | "La importación no se ha podido completar al actualizar los usuarios registrados: {motivo}. Los cambios se han revertido." |
| V-032 | Fichero válido con 0 DNIs válidos efectivos | Tolerancia | Negocio (asumida)* | (a) CSV con fichero vacío o solo cabecera ignorada. (b) CSV con todas las líneas con DNI inválido. (c) XML con al menos un elemento hijo (cumple V-021) pero todos los DNIs son inválidos según `DniUtil`. En estos casos la importación es correcta con 0 importados y la actualización de usuarios registrados se ejecuta normalmente. Un XML con la sección vacía dispara V-021, no V-032. | "Importación correcta con 0 usuarios importados." |
| V-033 | Caso XML (Actual=No, Anterior=No) | Coherencia | Negocio (asumida)* | Defensivo, no debería ocurrir; en caso de detectarse se eliminan los tipos `T` y `EX_T` del usuario si los tuviera. | (Se registra en el log.) |
| V-034 | Caso XML (Actual=No, Anterior=Sí) | Coherencia | Negocio (asumida)* | Se añade el tipo `EX_T` al usuario y se elimina `T` si lo tenía. | (Se registra en el log.) |
| V-035 | Caso XML (Actual=Sí, Anterior=No) | Coherencia | Negocio (asumida)* | Se añade el tipo `T` al usuario y se elimina `EX_T` si lo tenía. | (Se registra en el log.) |
| V-036 | Caso XML (Actual=Sí, Anterior=Sí) | Coherencia | Negocio (asumida)* | Se añade el tipo `T` al usuario y se elimina `EX_T` si lo tenía. | (Se registra en el log.) |
| V-037 | Mutua exclusión base ↔ EX en usuarios registrados | Coherencia | Negocio (asumida)* | Un usuario registrado en un centro no puede tener simultáneamente un tipo base (PROFESOR/ALUMNO/FAMILIAR) y su `EX` (EXPROFESOR/EXALUMNO/EXFAMILIAR). La regla se garantiza al aplicar V-033 a V-036. | "El usuario '{dni}' no puede tener simultáneamente los tipos '{tipoBase}' y '{tipoEx}' en el centro '{centro}'." |
| V-038 | Actualización CSV — User con `CentroUsuario` en el centro activo | Coherencia | Negocio (asumida)* | Para cada DNI del CSV cuyo `User` tiene `CentroUsuario` en el centro activo, se añade `PROFESOR_EXTERNO` si no lo tenía. | (Se registra en el log.) |
| V-039 | Actualización CSV — User sin `CentroUsuario` en el centro activo | Coherencia | Negocio (asumida)* | Para cada DNI del CSV cuyo `User` no tiene `CentroUsuario` en el centro activo, se crea el `CentroUsuario` y se añade `PROFESOR_EXTERNO`. | (Se registra en el log.) |
| V-040 | DNI del fichero sin `User` registrado | Coherencia | Negocio (asumida)* | Si un DNI del fichero no corresponde a ningún `User` registrado, solo se inserta el `UsuarioAutorizado` y no se actúa sobre usuarios registrados (sin nota en el log). | (Sin mensaje al usuario; comportamiento silencioso.) |

### Máquina de estados

No aplica como máquina de estados clásica. `TareaImportacion.estado` es un booleano terminal asignado una sola vez al cierre del intento de importación y la entidad es inmutable a partir de ese momento. Dos valores: `true` (correcta) o `false` (fallida por cualquier motivo previo o por fallo de actualización con reversión).

### Campos calculados

- **`TareaImportacion.fechaImportacion`** — instante en que se inicia el procesado.
- **`TareaImportacion.usuarioImportador`** — usuario autenticado que ejecuta la operación de subida.
- **`TareaImportacion.centro`** — para XML, derivado del atributo `codigo` del `<centro>` del fichero, resolviéndolo contra el centro activo del importador (cuyos códigos deben coincidir). Para CSV, directamente el centro activo del importador.
- **`TareaImportacion.curso`** — para XML, atributo `curso` del `<centro>`. Para CSV, `Centro.curso` del centro activo.
- **`TareaImportacion.fechaExportacion`** — para XML, atributo `fechaExportacion` del `<centro>` (patrón `dd/MM/yyyy HH:mm:ss`). Para CSV, instante del inicio del procesado.
- **`TareaImportacion.estado`** — `true` si y solo si las validaciones previas pasaron, las inserciones en `UsuarioAutorizado` se realizaron sin excepción técnica y la actualización de usuarios registrados terminó sin excepción técnica. `false` en cualquier otro caso.
- **`TareaImportacion.log`** — texto humano compuesto durante el procesado con: resumen numérico (DNIs leídos, válidos insertados, omitidos por formato inválido, omitidos por duplicado intra-fichero, contadores por cada caso de actualización), detalle por DNI inválido o duplicado, motivo global del fallo de validación previa si aplica, y motivo del fallo de la actualización con reversión si aplica.
- **Mapeo tipo base ↔ tipo EX para XML**: PROFESOR↔EXPROFESOR, ALUMNO↔EXALUMNO, FAMILIAR↔EXFAMILIAR.

### Asunciones a confirmar

- **A1\*** (V-018 a V-020): los dos campos del asistente son obligatorios en el cliente; mientras alguno esté vacío, el botón de confirmar permanece bloqueado.
- **A2\*** (V-021 a V-024): cualquier desviación estructural del XML/CSV produce una `TareaImportacion` fallida con un único motivo global en el log; no se intenta extraer datos parciales.
- **A3\*** (V-025): la duplicidad de importación se evalúa solo contra `TareaImportacion` previas en `estado=true`; importaciones previas fallidas con la misma combinación no impiden reintentar.
- **A4\*** (V-026): el control de acceso se ejerce exclusivamente por pertenencia al grupo Axelor `admins`; no se contempla otro mecanismo (cargos, perfiles, etc.).
- **A5\*** (V-027, V-028): la inmutabilidad cubre tanto la UI como la API REST/JPA y los intentos rechazados muestran el mensaje al usuario.
- **A6\*** (V-031): la reversión cubre exclusivamente los efectos del paso 3 (inserts en `UsuarioAutorizado` y cambios en `CentroUsuario`/`CentroUsuarioTipoUsuario`); la `TareaImportacion` siempre se persiste con su log.
- **A7\*** (V-033): el caso defensivo (Actual=No, Anterior=No) elimina del usuario tanto `T` como `EX_T` si los tuviera; el evento se anota en el log para diagnóstico.
- **A8\*** (V-040): un DNI del fichero sin `User` registrado solo deja rastro en `UsuarioAutorizado`; no se anota en el log porque no es un error.
- **A9\*** (Apertura del detalle tras importar): tras procesar (correcta o fallida) el asistente abre automáticamente la ficha de detalle de la `TareaImportacion` recién creada; no se vuelve al listado.
- **A10\*** (Migración del esquema de `UsuarioAutorizado`): los registros preexistentes con la unicidad antigua deben quedar compatibles con la nueva (curso y fechaExportacion obligatorios; `fechaExportacion` migrada de `date` a `datetime`).
- **A11\*** (Visibilidad de los listados): los administradores ven `TareaImportacion` y `UsuarioAutorizado` de todos los centros, sin filtro por centro activo, en esta historia.
- **A12\*** ("Última fechaExportacion" en la regla XML): se entiende como la mayor `fechaExportacion` registrada en `UsuarioAutorizado` para la terna `(centro, dni, tipo)` y el `curso` del fichero, tras aplicar los inserts de la importación en curso.
- **A13\*** (Cabecera CSV): si la primera línea del CSV no es un DNI válido, se descarta silenciosamente como cabecera; no se anota en el log.
- **A14\*** (Curso usado en las reglas): tanto la duplicidad de importación como la determinación de "Actual/Anterior" usan el `curso` declarado en el fichero (en CSV es `Centro.curso`), no el curso activo del centro en el momento de la consulta.
- **A15\*** (V-021): la validación de formato XML se hace contra los esquemas XSD existentes (`profesores.xsd`, `alumnos.xsd`, `familiares.xsd`); cualquier mensaje del validador se incorpora íntegro al log de la `TareaImportacion` fallida.
