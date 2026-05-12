---
type: analysis
---

## Análisis Funcional: Importación de Usuarios

**Tipo:** subsistema
**Capa:** subsystem/importacion
**Descripción:** Permite a los administradores de Axelor importar usuarios desde ficheros XML o CSV externos, almacenarlos como registros inmutables de usuarios autorizados, y actualizar automáticamente los tipos de usuario de los usuarios ya registrados en el sistema.

---

### Entidades

#### TareaImportacion (nueva)
Ubicación: `subsystem/importacion/domains/TareaImportacion.xml`

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | Long | PK, auto | Clave primaria |
| `fechaImportacion` | DateTime | required | Fecha y hora en que se realizó la importación |
| `tipoUsuario` | ManyToOne → TipoUsuario | required | Tipo importado (PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO) |
| `fichero` | ManyToOne → MetaFile | required | Fichero subido por el importador |
| `nombreFichero` | String | required | Nombre original del fichero |
| `usuario` | ManyToOne → User | required | Usuario de Axelor que realizó la importación |
| `centro` | ManyToOne → Centro | required | Centro sobre el que se realizó la importación |
| `curso` | Integer | required | Curso académico del centro en el momento de importar |
| `fechaExportacion` | DateTime | required | Fecha de exportación del fichero (para CSV: fecha de la importación) |
| `correcta` | Boolean | required | Indica si la importación fue exitosa |
| `log` | String (large/Text) | nullable | Resultado detallado del proceso |

Restricciones:
- Inmutable: no se puede modificar ningún campo ni eliminar un registro una vez creado.
- No existe unicidad compuesta en `TareaImportacion`: se permiten múltiples intentos fallidos para la misma combinación; la unicidad de importaciones exitosas se controla por validación de negocio (V-005).
- Integridad referencial al borrar el padre: `Centro` → RESTRICT; `TipoUsuario` → RESTRICT; `User` (importador) → RESTRICT; `MetaFile` → RESTRICT.

---

#### UsuarioAutorizado (rediseño)
Ubicación: `subsystem/registrousuario/domains/UsuarioAutorizado.xml`

Cambio respecto al diseño anterior: `unique-constraint` se cambia de `(centro, dni, tipoUsuario)` a `(centro, dni, tipoUsuario, curso, fechaExportacion)`. Se eliminan los campos `activo` y el anterior `curso` suelto (si existía), y se añaden `curso` y `fechaExportacion` como campos obligatorios.

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | Long | PK, auto | Clave primaria |
| `centro` | ManyToOne → Centro | required | Centro al que pertenece el usuario |
| `dni` | String | required | DNI/NIE del usuario (ya validado con `DniUtil`) |
| `tipoUsuario` | ManyToOne → TipoUsuario | required | Tipo de usuario (solo tipos base importables) |
| `curso` | Integer | required | Curso académico de la importación |
| `fechaExportacion` | DateTime | required | Fecha de exportación del fichero origen |

Restricciones:
- Unique: `(centro, dni, tipoUsuario, curso, fechaExportacion)` — ámbito global.
- Inmutable: no se puede modificar ningún campo ni eliminar un registro una vez creado.
- Solo se insertan registros; nunca se actualizan.
- Solo pueden aparecer tipos base: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO. Los tipos EX son exclusivos de `CentroUsuarioTipoUsuario`.
- Integridad referencial al borrar el padre: `Centro` → RESTRICT; `TipoUsuario` → RESTRICT.

---

#### Entidades existentes reutilizadas (sin modificación estructural salvo UsuarioAutorizado)

- `Centro` — `code` (String, unique), `name`, `curso` (Integer, curso académico activo)
- `CentroUsuario` — relación Centro ↔ User; unique (centro, usuario)
- `CentroUsuarioTipoUsuario` — pivote: `centroUsuario`, `tipoUsuario`; unique (centroUsuario, tipoUsuario). Aquí se almacenan los tipos activos de cada usuario registrado, incluyendo los tipos EX.
- `TipoUsuario` — catálogo: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO, EXPROFESOR, EXALUMNO, EXFAMILIAR
- `User` (Axelor) — campo `centroActivo` (Centro), campo `dni` (String, DNI normalizado del usuario)
- `MetaFile` (Axelor) — binario del fichero subido

---

### Dependencias de otros subsistemas

| Subsistema | Entidad / Clase | Motivo |
|---|---|---|
| `subsystem/common` | `Centro`, `CentroUsuario`, `CentroUsuarioTipoUsuario`, `TipoUsuario` | Modelo de centros y tipos de usuario |
| `subsystem/registrousuario` | `UsuarioAutorizado` | Tabla de usuarios importados (rediseñada) |
| `base/util` | `DniUtil` | Validación de formato y dígito de control de DNI/NIE |
| `base/util` | `MetaFileUtil` | Descarga del contenido del fichero como byte[] |
| `base/util` | `XmlUtil` | Parseo de ficheros XML |
| `base/infrastructure/validation` | `BusinessMessages`, `BusinessException` | Errores de negocio en el servicio |
| Axelor | `MetaFile`, `User` | Fichero adjunto y usuario autenticado |

No hay dependencias circulares: `subsystem/importacion` depende de `subsystem/registrousuario` y `subsystem/common`; ninguno de estos depende de `importacion`.

---

### Operaciones

#### OP-01: Listar importaciones
- **Actor:** Admin de Axelor
- **Descripción:** Muestra el listado de `TareaImportacion` con campos: `fechaImportacion`, `tipoUsuario`, `nombreFichero`, `usuario`, `centro`, `correcta`. Listado de solo lectura, ordenado por `fechaImportacion` descendente.
- **Vista global (admin):** Sin filtro de centro — ve todas las importaciones.
- **Vista gestión por centro:** Filtrada por `centro = centroActivo del usuario`.

#### OP-02: Ver detalle de importación
- **Actor:** Admin de Axelor
- **Descripción:** Muestra todos los campos de una `TareaImportacion` incluyendo el `log` completo. Solo lectura.

#### OP-03: Crear nueva importación
- **Actor:** Admin de Axelor
- **Descripción:** El importador selecciona el `tipoUsuario` y adjunta el `fichero`. El sistema lanza el proceso de importación.
- **Flujo principal:**
  1. Validar campos obligatorios de la UI: `tipoUsuario` (V-001) y `fichero` (V-002).
  2. Determinar `centroActivo` del importador; si es null → error (V-010).
  3. Determinar `curso` del `centroActivo`; si es null → error (V-011).
  4. Crear registro `TareaImportacion` (en transacción independiente) con `fechaImportacion = ahora`, `usuario`, `centro`, `curso`, `correcta = false` (inicial).
  5. Parsear el fichero:
     - XML: extraer `codigoCentro`, `curso` y `fechaExportacion` del nodo `<centro>`.
     - CSV: `centro` y `curso` del paso 3; `fechaExportacion` = `fechaImportacion`.
  6. Comprobar que el formato del fichero es válido (V-006 o V-007); si no → guardar TareaImportacion con error en log, fin.
  7. Para tipos XML: verificar que `codigoCentro` del fichero coincide con `centroActivo.code` (V-004); si no → guardar TareaImportacion con error en log, fin.
  8. Comprobar que no existe TareaImportacion correcta con la misma `(fechaExportacion, tipoUsuario, centro, curso)` (V-005); si existe → guardar TareaImportacion con error en log, fin.
  9. Procesar registros del fichero uno a uno: validar DNI con `DniUtil.isValid()` (V-008). Los inválidos se omiten y se añade línea al log. Los válidos se preparan para inserción en `UsuarioAutorizado`.
  10. Dentro de **una única transacción**:
      a. Insertar `UsuarioAutorizado` para cada DNI válido. Si ya existe la combinación exacta, se omite silenciosamente (idempotente).
      b. Ejecutar OP-04 (XML) u OP-05 (CSV).
      c. Si falla → rollback completo de la transacción; guardar TareaImportacion como fallida con error en log (V-009), fin.
  11. Si todo correcto → actualizar TareaImportacion con `correcta = true` y log de resumen (n.º importados, n.º errores de DNI).

> **Nota arquitectónica:** La persistencia de `TareaImportacion` (éxito o fallo) debe ocurrir en una transacción **separada e independiente** del bloque UsuarioAutorizado + CentroUsuarioTipoUsuario, para que el registro del intento de importación sobreviva siempre, incluso si la transacción principal se revierte.

#### OP-04: Actualizar CentroUsuarioTipoUsuario — importación XML (interna)
- **Actor:** Sistema (llamado desde OP-03 para tipos PROFESOR, ALUMNO, FAMILIAR)
- **Descripción:** Para cada `CentroUsuario` del mismo centro cuyo usuario tenga el tipo base o su EX correspondiente, aplica la tabla de decisión:
  - `UsuarioImportadoActual`: existe `UsuarioAutorizado` con mismo tipo, dni, centro, con la **última** `fechaExportacion` para el curso activo de ese centro.
  - `UsuarioImportadoAnterior`: existe `UsuarioAutorizado` con mismo tipo, dni, centro, con `fechaExportacion` **anterior** a la última para el curso activo de ese centro.

| UsuarioImportadoActual | UsuarioImportadoAnterior | Acción |
|:---:|:---:|:---|
| No | No | Elimina el tipo base (caso defensivo) |
| No | Sí | Añade tipo EX; elimina tipo base si lo tenía |
| Sí | No | Añade tipo base; elimina tipo EX si lo tenía |
| Sí | Sí | Añade tipo base; elimina tipo EX si lo tenía |

EX_MAPPING: PROFESOR→EXPROFESOR, ALUMNO→EXALUMNO, FAMILIAR→EXFAMILIAR

#### OP-05: Actualizar CentroUsuarioTipoUsuario — importación CSV (interna)
- **Actor:** Sistema (llamado desde OP-03 para tipo PROFESOR_EXTERNO)
- **Descripción:** Para cada DNI válido del CSV, busca `CentroUsuario` cuyo `User.dni` coincida y pertenezca al mismo centro. Si existe y no tiene ya el tipo PROFESOR_EXTERNO, le añade `CentroUsuarioTipoUsuario` con tipo PROFESOR_EXTERNO. No se elimina este tipo ni se generan tipos EX.

---

### Vistas

Todas en `subsystem/importacion/views/` con prefijo `subsysImportacion`.

| Nombre | Tipo | Descripción |
|---|---|---|
| `subsysImportacion.TareaImportacion@Main-action` | action-view | Vista global sin filtro de centro |
| `sysGestion.Importacion@SecretariaVirtualModule-action` | action-view | Vista filtrada por `centroActivo` del usuario |
| `subsysImportacion.TareaImportacion@Main-grid` | grid | Listado de importaciones: fechaImportacion, tipoUsuario, nombreFichero, usuario, centro, correcta. Solo lectura. Botón "Nueva importación" abre `@New-form`. |
| `subsysImportacion.TareaImportacion@Main-form` | form | Detalle de importación, todos los campos en solo lectura incluyendo `log`. Sin botones de guardar/editar. |
| `subsysImportacion.TareaImportacion@New-form` | form | Formulario de nueva importación: solo campos `tipoUsuario` (selector filtrado a tipos importables) y `fichero` (upload). Botón "Importar" llama al controlador que lanza OP-03. Validaciones en cliente: V-001 y V-002. |

---

### Menús

| ID menú | Título | Action-view | Grupos | Visibilidad |
|---|---|---|---|---|
| `administracionSv-importacion-menuitem` | "Ficheros importación" | `subsysImportacion.TareaImportacion@Main-action` | `admins` | Solo grupo admins — ya existe en `menus.xml` |
| (existente en menú gestión de centro) | "Ficheros de importación" | `sysGestion.Importacion@SecretariaVirtualModule-action` | `admins` | Solo grupo admins — ya existe en `menus.xml` |

Ambos menús ya existen; no se crean nuevos. Solo se verificará que apuntan a las action-views correctas.

---

### Seguridad

- Solo el grupo `admins` de Axelor tiene acceso a todas las vistas y operaciones del subsistema.
- Las vistas son de solo lectura para registros existentes. La única acción de escritura es la creación vía OP-03.
- `UsuarioAutorizado` no tiene vista propia en este subsistema (se gestiona exclusivamente por el servicio interno).
- La vista `sysGestion.Importacion@SecretariaVirtualModule-action` filtra por `centroActivo` del usuario, pero también está restringida al grupo `admins`.
- Multicentro: la vista admin global muestra importaciones de todos los centros; la vista de gestión muestra solo las del `centroActivo` del importador.

---

### Validaciones (tabla V-XXX)

| ID | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario / al log |
|---|---|---|---|---|---|
| V-001 | `tipoUsuario` | Required | Modelo | Al crear nueva importación sin seleccionar tipo de usuario | "El tipo de usuario es obligatorio para iniciar la importación." |
| V-002 | `fichero` | Required | Modelo | Al crear nueva importación sin adjuntar fichero | "El fichero es obligatorio para iniciar la importación." |
| V-003 | `tipoUsuario` | Dominio finito | Catálogo | `tipoUsuario` seleccionado no pertenece a {PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO} | "El tipo '{valor}' no es válido para importación. Tipos permitidos: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO." |
| V-004 | `codigoCentro` del XML vs `centroActivo` | Negocio* | Negocio | Solo XML: el atributo `codigo` del nodo `<centro>` no coincide con el `code` del `centroActivo` del importador | [LOG] "El centro del fichero ('{codigoFichero}') no coincide con el centro activo del importador ('{codigoActivo}'). Importación cancelada." |
| V-005 | `(fechaExportacion, tipoUsuario, centro, curso)` | Unicidad — combinación, ámbito global, solo importaciones correctas* | Negocio | Ya existe `TareaImportacion` con `correcta = true` para la misma combinación (fechaExportacion, tipoUsuario, centro, curso activo) | [LOG] "Ya existe una importación correcta para el tipo '{tipo}', centro '{centro}', curso '{curso}' y fechaExportacion '{fechaExportacion}'. Importación cancelada." |
| V-006 | `fichero` (formato XML) | Formato | Catálogo | El fichero XML no es parseable o le faltan el nodo `<centro>` o sus atributos `codigo`, `curso`, `fechaExportacion`, o el nodo de colección del tipo | [LOG] "El fichero '{nombreFichero}' no tiene el formato XML válido para el tipo '{tipo}'. Importación cancelada." |
| V-007 | `fichero` (formato CSV) | Formato | Catálogo | El fichero CSV de PROFESOR_EXTERNO no puede leerse como texto plano con una entrada por línea | [LOG] "El fichero '{nombreFichero}' no tiene un formato CSV válido. Importación cancelada." |
| V-008 | `dni` (por registro del fichero) | Formato | Catálogo | El documento individual no supera `DniUtil.isValid()` | [LOG] "El documento '{dni}' no tiene formato DNI/NIE válido. Usuario omitido." |
| V-009 | Proceso de actualización de usuarios registrados | Negocio* | Negocio | OP-04 u OP-05 lanzan excepción no controlada | [LOG] "Error al actualizar los tipos de usuario registrados: '{mensajeError}'. La importación completa ha sido revertida." |
| V-010 | `centroActivo` del importador | Negocio* | Negocio | El `centroActivo` del importador es null al iniciar la importación | [LOG] "El importador '{usuario}' no tiene un centro activo asignado. Importación cancelada." |
| V-011 | `curso` del centro activo | Negocio* | Negocio | `centroActivo.curso` es null al procesar importación CSV o al comprobar unicidad | [LOG] "El centro activo '{centro}' no tiene curso académico activo configurado. Importación cancelada." |
| V-012 | `TareaImportacion` — inmutabilidad | Negocio* | Negocio | Intento de modificar cualquier campo de una `TareaImportacion` ya persistida | Invariante técnica: el servicio no expone operación de actualización; la UI no tiene controles de edición. Sin permiso de `update` para el grupo `admins`. |
| V-013 | `UsuarioAutorizado` — inmutabilidad | Negocio* | Negocio | Intento de modificar cualquier campo de un `UsuarioAutorizado` ya persistido | Invariante técnica: el servicio no expone operación de actualización; sin permiso de `update`. |
| V-014 | `(centro, dni, tipoUsuario, curso, fechaExportacion)` | Unicidad — combinación de 5 campos, ámbito global | Modelo | Al insertar `UsuarioAutorizado` con combinación ya existente | Silencioso: el registro duplicado se omite sin error (comportamiento idempotente garantizado por índice único en BD). |
| V-015 | `CentroUsuarioTipoUsuario` — exclusividad tipo base / EX | Negocio* | Negocio | Un `CentroUsuario` no puede tener simultáneamente tipo base (PROFESOR/ALUMNO/FAMILIAR) y su tipo EX correspondiente | Invariante técnica: OP-04 elimina el tipo contrario antes de añadir el nuevo. No es error visible al usuario; es la acción esperada del proceso. |
| V-016 | Integridad referencial: eliminar `Centro` | RESTRICT | Modelo | Intento de eliminar un `Centro` referenciado en `TareaImportacion` o `UsuarioAutorizado` | "No se puede eliminar el centro '{nombre}' porque tiene importaciones o usuarios importados asociados." |
| V-017 | Integridad referencial: eliminar `TipoUsuario` | RESTRICT | Modelo | Intento de eliminar un `TipoUsuario` referenciado en `TareaImportacion` o `UsuarioAutorizado` | "No se puede eliminar el tipo '{codigo}' porque tiene importaciones o usuarios importados asociados." |
| V-018 | Integridad referencial: eliminar `User` (importador) | RESTRICT | Modelo | Intento de eliminar un `User` referenciado como importador en `TareaImportacion` | "No se puede eliminar el usuario '{nombre}' porque tiene importaciones realizadas." |

---

### Asunciones a confirmar

Las siguientes reglas están marcadas con `*` (Negocio asumida) y requieren confirmación:

1. **(V-004)** La comparación de centro en XML usa el atributo `codigo` del nodo `<centro>` comparado con `Centro.code`. Si el código del XML no existe en BD, la comparación con `centroActivo.code` ya lo descarta como no coincidente — no se necesita validación adicional de existencia.

2. **(V-005)** La unicidad de importaciones exitosas se comprueba solo sobre `TareaImportacion` con `correcta = true`. Múltiples intentos fallidos para la misma combinación están permitidos.

3. **(V-009 / arquitectónica)** La persistencia de `TareaImportacion` (éxito o fallo) ocurre en una transacción **separada e independiente** del bloque UsuarioAutorizado + CentroUsuarioTipoUsuario. Si la transacción principal hace rollback, el registro de la tarea de importación sobrevive.

4. **(V-012 / V-013)** La inmutabilidad se garantiza únicamente a nivel de aplicación (sin permiso de update/delete en el grupo admins + sin operaciones en el servicio). No hay restricción DDL adicional en BD.

5. **(V-015)** La exclusión mutua tipo base / EX se aplica en OP-04 de forma silenciosa. Si por algún bug previo coexistieran ambos tipos, OP-04 los corrige.

6. **Campo `User.dni`**: Para OP-05 (CSV PROFESOR_EXTERNO), la búsqueda de `CentroUsuario` por documento se realiza comparando los DNIs del fichero con el campo `dni` del `User` de Axelor. Se asume que este campo existe y está normalizado (confirmado por el CLAUDE.md de `registrousuario`: "code = email, dni = DNI normalizado"). No se requiere confirmación adicional.

7. **Detección de cabecera CSV**: Si la primera línea del CSV no supera `DniUtil.isValid()`, se considera cabecera y se omite; si sí es válida, se procesa como DNI. Requiere confirmación del criterio exacto.

8. **`fechaExportacion` en CSV**: Para PROFESOR_EXTERNO, la `fechaExportacion` almacenada en `UsuarioAutorizado` y en `TareaImportacion` es la `fechaImportacion` (LocalDateTime.now() en el momento del proceso). Dos importaciones CSV del mismo centro en el mismo instante serían bloqueadas por V-005; en la práctica esto es improbable, pero podría relajarse usando solo la fecha sin hora para la comprobación de unicidad.

9. **OP-04 opera sobre todos los CentroUsuario del centro**: El proceso de actualización de usuarios registrados evalúa TODOS los `CentroUsuario` del centro que tengan el tipo base o su EX correspondiente, no solo los que aparecen en el fichero. Esto garantiza que usuarios que desaparecen del fichero reciben el tipo EX o pierden el tipo base.

10. **`curso` en TareaImportacion para XML**: El campo `curso` de `TareaImportacion` se toma del atributo `curso` del XML (no de `Centro.curso`). Para CSV, se toma de `Centro.curso`. Ambos podrían diferir si el fichero XML es de un curso anterior al activo.
