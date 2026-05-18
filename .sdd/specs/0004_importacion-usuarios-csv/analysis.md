---
type: analysis
---

## Análisis Funcional: Importación de usuarios autorizados desde CSV

**Tipo:** subsistema
**Capa:** subsystem/importacion (ampliación del subsistema existente)
**Descripción:** Permite a un usuario del grupo `admins` importar masivamente usuarios autorizados de tipo profesor externo a partir de un fichero CSV con un DNI por línea, asignándolos automáticamente al centro y curso activos del importador y dejando registro inmutable del resultado.

### Entidades

Esta iniciativa **no crea entidades nuevas**. Reutiliza dos entidades existentes y modifica una de ellas:

- `TareaImportacion` (subsystem/importacion, ya existente, **sin cambios estructurales**) — registra cada importación realizada. Campos relevantes: `usuario` (importador), `centro` (centro usado), `curso` (curso usado), `fechaImportacion` (instante de creación), `fechaExportacion` (instante de finalización del procesamiento), `tipoFichero` (enum `TipoFicheroImportacion` con valores `PROFESOR`, `ALUMNO`, `FAMILIAR`, `PROFESOR_EXTERNO`; en esta iniciativa siempre `PROFESOR_EXTERNO`), `fichero` (el CSV subido), `estado` (booleano: `true`=importación completada, `false`=abortada), `log` (texto multilínea con resumen y detalle). Es inmutable tras crearse.

- `UsuarioAutorizado` (subsystem/registrousuario, ya existente, **se modifica el unique-constraint y el tipo de `fechaExportacion`**) — usuario habilitado para registrarse en la aplicación. Campos: `centro` (Centro, requerido), `dni` (texto, requerido; se almacena normalizado), `tipoUsuario` (TipoUsuario, requerido), `curso` (entero, no requerido en el modelo; siempre relleno cuando se crea desde esta importación), `fechaExportacion` (**fecha-hora**).
  - **Cambio 1**: la restricción de unicidad pasa de `(centro, dni, tipoUsuario)` a `(centro, dni, tipoUsuario, curso)`. Permite que un mismo DNI exista para mismo centro y tipo de usuario en cursos distintos.
  - **Cambio 2**: el campo `fechaExportacion` pasa de tipo fecha a tipo fecha-hora, para registrar también la hora exacta de la importación.

- `Centro` (subsystem/common, ya existente, sólo lectura) — aporta el campo `curso` (entero), interpretado como el "curso activo" del centro.

- `TipoUsuario` (subsystem/common, ya existente, sólo lectura) — identificado por `codigo`. Para esta iniciativa interesa el código `PROFESOR_EXTERNO`.

### Dependencias de otros subsistemas

- `subsystem/registrousuario` — para consultar y crear `UsuarioAutorizado`.
- `subsystem/common` — para obtener el centro activo del importador, el curso activo del centro y el `TipoUsuario` por código.
- Utilidad transversal de normalización y validación de DNI/NIE/CIF (limpia formato y valida con dígito de control). Es infraestructura compartida.

Sin dependencias circulares.

### Operaciones

- **Importar usuarios autorizados desde CSV**: la dispara el importador (usuario del grupo `admins`) desde el formulario existente de creación de `TareaImportacion`, eligiendo el tipo de fichero `PROFESOR_EXTERNO` y adjuntando un fichero CSV de una sola columna sin cabecera con un DNI por línea. El sistema:
  1. Determina el centro activo del importador y el curso activo de ese centro.
  2. Si falta el centro activo, aborta el procesamiento sin leer el fichero, persiste la `TareaImportacion` con `estado=false` y log explicativo.
  3. Si el centro activo no tiene curso asignado, aborta el procesamiento sin leer el fichero, persiste la `TareaImportacion` con `estado=false` y log explicativo.
  4. Si el catálogo no contiene el `TipoUsuario` con código `PROFESOR_EXTERNO`, aborta como error técnico de configuración con `estado=false` y log explicativo.
  5. Si el fichero no se puede leer (codificación inválida o error de E/S), aborta con `estado=false` y log explicativo.
  6. En otro caso, procesa cada línea del CSV de forma independiente (un fallo en una línea no afecta a las demás): normaliza el DNI, lo valida con dígito de control, y crea o ignora un `UsuarioAutorizado` según corresponda.
  7. Al terminar, persiste la `TareaImportacion` con `estado=true`, centro y curso usados, `fechaExportacion` del instante actual y log en castellano con resumen y detalle de errores e ignorados.

- **Consultar el resultado de una importación**: cualquier usuario del grupo `admins` puede consultar el listado y el detalle de una `TareaImportacion` previa, ver su estado y el log con contadores y detalle de errores e ignorados. Operación de sólo lectura (la entidad es inmutable).

### Vistas

Esta iniciativa **no crea ni modifica vistas**. Reutiliza las vistas ya existentes del subsistema `importacion` (formulario de creación con subida del CSV y selección de tipo de fichero, formulario de detalle/resultado con el log en modo lectura, listado de tareas de importación). El cambio funcional respecto al estado anterior es que ahora el flujo `PROFESOR_EXTERNO` produce un resultado real en lugar del mensaje "no implementado".

### Menús

No se añaden ni modifican menús. El menú de "Importaciones" del subsistema, visible para el grupo `admins`, ya da acceso al formulario de creación y al listado.

### Seguridad

- **Administrador (grupo `admins` de Axelor)**: puede crear tareas de importación CSV y consultar el resultado de cualquier importación (incluso de otros centros).
- **Resto de tipos de usuario** (Supervisor, Profesor, Exprofesor, Alumno, Exalumno, Externo, Familiar): no acceden a esta operación ni a las tareas de importación.
- **Multicentro: sí**. El centro asignado a los `UsuarioAutorizado` creados y a la propia `TareaImportacion` es siempre el centro activo del importador en el momento de la importación; no es seleccionable por el usuario. Análogamente, el curso es el curso activo de ese centro. Un administrador con varios centros opera siempre sobre el que tenga activo.
- La operación de modificar o eliminar una `TareaImportacion` está prohibida (la entidad es inmutable; comportamiento heredado). Esta iniciativa no permite modificar ni eliminar `UsuarioAutorizado` desde el flujo de importación.

### Validaciones (`V-XXX`)

| ID | Campo(s) | Descripción | Condición | Mensaje al usuario |
|---|---|---|---|---|
| V-001 | `UsuarioAutorizado` (`centro`, `dni`, `tipoUsuario`, `curso`) | Unicidad ampliada por curso. **Ámbito: combinación de los cuatro campos.** | No pueden coexistir dos `UsuarioAutorizado` con la misma combinación de centro, DNI normalizado, tipo de usuario y curso. Sustituye al unique-constraint anterior `(centro, dni, tipoUsuario)`. | "Ya existe un usuario autorizado con DNI '{dni}' para el centro '{centro}', tipo '{tipoUsuario}' y curso '{curso}'." |
| V-002 | `UsuarioAutorizado.centro` | Centro obligatorio en usuario autorizado. | El campo no puede ser nulo. | "El centro del usuario autorizado es obligatorio." |
| V-003 | `UsuarioAutorizado.dni` | DNI obligatorio en usuario autorizado. | El campo no puede ser nulo ni vacío. | "El DNI del usuario autorizado es obligatorio." |
| V-004 | `UsuarioAutorizado.tipoUsuario` | Tipo de usuario obligatorio. | El campo no puede ser nulo. | "El tipo de usuario del usuario autorizado es obligatorio." |

> Notas:
> - V-001 es el único cambio respecto al estado anterior (amplía el unique-constraint añadiendo `curso`).
> - V-002 a V-004 son obligatoriedades del modelo `UsuarioAutorizado` ya existentes; se listan por trazabilidad porque esta iniciativa crea registros de esa entidad.
> - Las validaciones de `TareaImportacion` (obligatoriedad de `fichero` y `tipoFichero`, inmutabilidad tras crearse) ya están implementadas por la iniciativa anterior `0003_importacion-vistas` y siguen vigentes; no se redefinen aquí.
> - Las condiciones del flujo CSV que abortan la importación (importador sin centro activo, centro sin curso, fichero ilegible, `TipoUsuario` no configurado, DNI inválido en una línea) **no son `V-XXX`**: no rechazan la creación de la `TareaImportacion`, que se persiste igualmente (con `estado=false` cuando el aborto es global, o con `estado=true` y entradas en el log cuando son fallos por línea). Se modelan como `R-XXX`.

### Reglas de negocio (`R-XXX`)

| ID | Descripción | Entidad | Método | Momento | Más información |
|---|---|---|---|---|---|
| R-001 | Cuando se procesa una importación de tipo `PROFESOR_EXTERNO`, el sistema determina el centro a usar como el centro activo del importador. Si no existe centro activo, dispara R-002. | TareaImportacion | Procesamiento del CSV | Antes | Centro no es seleccionable por el usuario. |
| R-002 | Si el importador no tiene centro activo asignado, el sistema aborta el procesamiento sin leer el fichero, no crea ningún usuario autorizado, marca la tarea con `estado=false` y registra en el log "Importación abortada: el importador no tiene centro activo asignado." | TareaImportacion | Procesamiento del CSV | Antes | La `TareaImportacion` se persiste igualmente como fallida. |
| R-003 | Si hay centro activo, el sistema determina el curso a usar como el curso activo de ese centro. Si el centro no tiene curso asignado, aborta el procesamiento sin leer el fichero, marca la tarea con `estado=false` y registra en el log "Importación abortada: el centro '{centro}' no tiene curso activo asignado." | TareaImportacion | Procesamiento del CSV | Antes | La `TareaImportacion` se persiste igualmente como fallida. |
| R-004 | El sistema resuelve el `TipoUsuario` usando la convención `tipoFichero.name()` como código. Si el código no existe en BD, aborta el procesamiento, marca la tarea con `estado=false` y registra en el log "Importación abortada: error de configuración, no existe el tipo de usuario con código '{tipoFichero.name()}'." | TareaImportacion | Procesamiento del CSV | Antes | El mapeo por convención hace extensible el flujo CSV: cualquier nuevo `TipoFicheroImportacion` enrutado al importador CSV resuelve su `TipoUsuario` automáticamente. |
| R-005 | Si el fichero CSV no se puede leer (codificación inválida o error de E/S), el sistema aborta el procesamiento, marca la tarea con `estado=false` y registra en el log "No se ha podido leer el fichero CSV. Motivo: {detalle}." | TareaImportacion | Procesamiento del CSV | Antes | Ningún `UsuarioAutorizado` se crea. |
| R-006 | El sistema recorre el CSV línea a línea, manteniendo el número de línea físico del fichero (1-based, las líneas en blanco cuentan para la numeración aunque se descarten para el procesamiento). | TareaImportacion | Procesamiento del CSV | Durante | Permite al administrador localizar errores en su fichero original. |
| R-007 | Las líneas vacías o que sólo contienen espacios se ignoran silenciosamente: no entran en ningún contador (creados, ignorados o errores) y no aparecen en el log. | TareaImportacion | Procesamiento del CSV | Durante | El número de línea sí avanza. |
| R-008 | Para cada línea no vacía, el sistema normaliza el DNI leído (mayúsculas, recorte de espacios, eliminación de ceros prefijados de DNI/NIE) antes de validarlo o usarlo. El DNI persistido en `UsuarioAutorizado.dni` es siempre el normalizado. | TareaImportacion | Procesamiento del CSV | Durante | — |
| R-009 | Tras normalizar, el sistema valida el DNI con dígito de control. Si no es válido, la línea cuenta como error, se añade al log una entrada con número de línea, DNI leído (forma original tras trim) y motivo "DNI no válido", y se continúa con la siguiente línea sin abortar. | TareaImportacion | Procesamiento del CSV | Durante | Garantía explícita de la historia: un fallo individual no detiene el proceso. |
| R-010 | Para cada DNI normalizado y válido, si no existe ningún `UsuarioAutorizado` con la combinación exacta (centro a usar, DNI normalizado, `TipoUsuario` resuelto, curso a usar), el sistema crea uno nuevo con esos cuatro datos y `fechaExportacion` = instante actual de la importación (fecha-hora completa). La línea cuenta como creado. | UsuarioAutorizado | insert | Antes | Cubre tanto "no existe ningún registro con ese DNI" como "existe con ese DNI pero distinta combinación de centro/tipoUsuario/curso": en ambos casos se crea un registro nuevo porque la búsqueda se hace por la combinación completa. Las creaciones no se listan individualmente en el log. |
| R-011 | Para cada DNI normalizado y válido, si ya existe un `UsuarioAutorizado` con la misma combinación exacta (centro, DNI, tipoUsuario, curso), el sistema no lo modifica ni lo borra. La línea cuenta como ignorado y se añade al log una entrada con número de línea, DNI leído y motivo "Ya existe". | TareaImportacion | Procesamiento del CSV | Durante | Cubre tanto duplicados internos del propio CSV (la primera ocurrencia crea, las siguientes caen aquí) como registros preexistentes en BD. |
| R-012 | Si el procesamiento de una línea individual produce un fallo inesperado (excepción no contemplada al crear el `UsuarioAutorizado` u otro error puntual), el sistema lo captura, cuenta la línea como error con motivo "Error inesperado: {detalle}", y continúa con la siguiente sin abortar la importación. | TareaImportacion | Procesamiento del CSV | Durante | Salvaguarda explícita de la garantía "un registro que falla no afecta a los demás". |
| R-013 | Al terminar el procesamiento del CSV, el sistema compone el log textual en castellano. Empieza con tres contadores ("Creados: {n}", "Ignorados: {n}", "Errores: {n}") y a continuación lista una línea por cada error y cada ignorado, en orden de aparición en el CSV, indicando número de línea, DNI leído y motivo. Las creaciones exitosas no se listan individualmente. | TareaImportacion | Procesamiento del CSV | Después | El log se devuelve junto con los tres contadores numéricos (creados, ignorados, errores), el centro y el curso usados. |
| R-014 | Si el procesamiento del CSV se completa (aunque haya errores o ignorados en líneas individuales), el sistema considera la importación exitosa: asigna `estado=true`, `centro` y `curso` con los valores efectivamente usados, `fechaExportacion` con el instante actual y `log` con el contenido compuesto. | TareaImportacion | insert | Antes | Una importación con 0 creados y 10 errores sigue siendo "exitosa" en sentido global; el detalle está en el log. |
| R-015 | Si la importación se aborta por causa global (R-002, R-003, R-004 o R-005), el sistema persiste la `TareaImportacion` con `estado=false`, sin `fechaExportacion`, y `centro`/`curso` rellenos sólo si llegaron a determinarse antes del aborto. El log contiene exclusivamente el mensaje explicativo de la causa. | TareaImportacion | insert | Antes | — |
| R-016 | Heredada del subsistema existente: al crear una `TareaImportacion`, el sistema asigna como `usuario` al importador autenticado y como `fechaImportacion` el instante actual. | TareaImportacion | insert | Antes | Comportamiento ya implementado por la iniciativa anterior; se relista para trazabilidad. |

### Reglas de UI (`U-XXX`)

Esta iniciativa **no añade ni modifica reglas de UI**. El comportamiento del formulario de creación de `TareaImportacion`, su transición a modo solo lectura tras crear, la presentación del log y la ocultación de campos calculados ya están establecidos por la iniciativa anterior `0003_importacion-vistas` y no se tocan.

| ID | Disparador | Efecto | Campo/Panel afectado | Condición |
|---|---|---|---|---|

*(sin entradas nuevas en esta iniciativa)*

### Máquina de estados

No aplica. `TareaImportacion` tiene un único atributo booleano `estado` (éxito/fallo) que se fija una sola vez al crearla y no transita. La entidad es inmutable.

### Campos calculados

Los siguientes campos de `TareaImportacion` se rellenan automáticamente por el sistema al crear la tarea (no los introduce el usuario). Cada uno está cubierto por una R-XXX:

- `usuario` — importador autenticado en el momento de crear la tarea (R-016).
- `fechaImportacion` — instante actual al crear la tarea (R-016).
- `centro` — centro activo del importador (R-001, R-014 / R-015).
- `curso` — curso activo del centro (R-003, R-014 / R-015).
- `fechaExportacion` — instante actual al completar el procesamiento (R-014).
- `estado` — `true` si el procesamiento se completó; `false` si se abortó por causa global (R-014, R-015).
- `log` — texto compuesto al final del procesamiento (R-013) o mensaje de causa global de aborto (R-002, R-003, R-004, R-005).

Y al crear un `UsuarioAutorizado` desde esta importación:
- `centro`, `tipoUsuario`, `curso`, `dni` (normalizado), `fechaExportacion` (instante actual, fecha-hora completa) — todos los rellena el flujo de importación (R-010).

Los contadores numéricos del resultado (creados, ignorados, errores) se calculan durante el procesamiento y se reflejan en el resumen del `log`; no se persisten como columnas separadas.

### Asunciones a confirmar

- **A1** (R-004): el mapeo entre los valores del enum `TipoFicheroImportacion` y los códigos de `TipoUsuario` se obtiene por convención: `tipoFichero.name()`. Funciona mientras los códigos de `TipoUsuario` coincidan en nombre exacto con los valores del enum. Cualquier nuevo tipo enrutado a `ImportadorUsuarioCSV` resuelve automáticamente su `TipoUsuario` sin modificaciones.
- **A2** (R-005): la codificación esperada del CSV es UTF-8. Si llega en otra codificación y no se puede decodificar, la importación se aborta como fichero ilegible.
- **A3** (R-006, R-009, R-011): la numeración de líneas reportada en el log es 1-based sobre el fichero físico, incluyendo las líneas en blanco en el conteo (aunque éstas se descarten para el procesamiento).
- **A4** (R-009): el DNI que se muestra en el log para errores e ignorados es el "leído" del CSV (tras trim básico), no el normalizado, para que el administrador pueda localizarlo en su fichero original.
- **A5** (entidad `UsuarioAutorizado`): el cambio del unique-constraint de `(centro, dni, tipoUsuario)` a `(centro, dni, tipoUsuario, curso)` es estrictamente menos restrictivo (más permisivo), por lo que los datos existentes son compatibles. No requiere migración de datos.
- **A6** (formato del CSV): "una columna sin cabecera, un DNI por línea". No se hace parseo CSV estricto con separador: si una línea contiene comas u otros separadores, el contenido completo (tras trim) se trata como un DNI candidato y caerá previsiblemente como error en R-009 ("DNI no válido").
- **A7** (R-013): el log se almacena en un único campo de texto multilínea. No hay un modelo estructurado de "entradas de log" (no se persisten como entidades separadas).
- **A8** (validez de DNI como invariante de modelo): la validación de formato del DNI **no** se añade como invariante de modelo de `UsuarioAutorizado` en esta iniciativa. El proceso de importación garantiza que los DNIs creados son válidos, pero la entidad en sí no impone esa validación a otras vías de creación.
- **A9** (multicentro): el "centro activo del importador" se interpreta como el centro asociado al usuario logado (ya resuelto por `subsystem/common`). El "curso activo del centro" es el campo `curso` (entero) de la entidad `Centro` (no hay lógica adicional de selección de curso).
- **A10** (rendimiento): no hay límite explícito de líneas del CSV. El procesamiento se ejecuta de forma síncrona dentro de la creación de la `TareaImportacion`.

## Notas de cierre (as-built)

Cambios aplicados respecto al draft original:
- **R-004**: Actualizado — el mapeo enum→código ya no es explícito. La implementación usa `tipoFichero.name()` por convención, por lo que el mensaje de error del log también es dinámico (`'...código '" + tipoFichero.name() + "'."`). Eliminada la nota "constante técnica de esta iniciativa".
- **A1** (antes A1*): Actualizado — la asunción "No se obtiene por convención automática de nombre" quedó invalidada por la implementación. Se corrigió para reflejar que el mapeo SÍ es por convención `tipoFichero.name()`.
