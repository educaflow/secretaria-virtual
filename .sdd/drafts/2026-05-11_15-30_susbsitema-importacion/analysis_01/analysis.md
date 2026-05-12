---
type: analysis
---

## Análisis Funcional: Importación de Usuarios

**Tipo:** subsistema
**Capa:** subsystem/importacion
**Descripción:** Permite a los administradores importar ficheros XML o CSV con documentos de usuarios para poblar la tabla de usuarios autorizados y actualizar los tipos de los usuarios ya registrados en el centro activo.

---

### Entidades

#### `TareaImportacion` (nueva)

| Campo | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK, autogenerado |
| `fechaImportacion` | DateTime | required; asignado por el sistema al ejecutar la importación |
| `tipoUsuario` | ManyToOne → `TipoUsuario` | required; inmutable; solo códigos importables: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO |
| `fichero` | ManyToOne → `MetaFile` | required; inmutable; fichero original subido por el importador |
| `nombreFichero` | String | required; inmutable; nombre del fichero en el momento de la subida |
| `importador` | ManyToOne → `User` (Axelor) | required; inmutable; usuario que ejecuta la importación |
| `centro` | ManyToOne → `Centro` | required; inmutable; `centroActivo` del importador en el momento de importar |
| `curso` | Integer | required; inmutable; valor de `centro.curso` en el momento de importar |
| `fechaExportacion` | Date | nullable; inmutable; leída del atributo del fichero XML, o fecha actual si es CSV; nulo si el fichero falla antes de poderse parsear |
| `estado` | String enum (CORRECTO, FALLIDO) | required; inmutable; asignado por el sistema al final del proceso |
| `cabecera` | Text | nullable; inmutable; resumen libre generado por el sistema (resultado, errores globales) |
| `errores` | OneToMany → `ErrorImportacion` | colección de errores individuales de fila; puede ser vacía |

**Restricciones:** inmutable una vez creada. No hay `unique-constraint` en base de datos; la unicidad de importación es una regla de negocio (V-008) comprobada antes de crear.

**Integridad referencial:** al borrar una `TareaImportacion`, sus `ErrorImportacion` se borran en cascada (V-012). Al borrar un `Centro`, `TipoUsuario`, `User` o `MetaFile` referenciados, la operación queda bloqueada.

---

#### `ErrorImportacion` (nueva)

Modelo sin UI propia; se muestra embebido en el formulario de `TareaImportacion`. Inmutable desde su creación.

| Campo | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK, autogenerado |
| `tareaImportacion` | ManyToOne → `TareaImportacion` | required; inmutable |
| `numeroFila` | Integer | nullable; número de fila del fichero donde se encontró el error; null para errores globales (estructura, duplicado, etc.) |
| `documento` | String | nullable; valor del DNI/documento de esa fila; null si no se pudo extraer |
| `descripcion` | String | required; inmutable; descripción del error |

---

#### Entidades existentes (solo referencia, no recrear)

- `com.educaflow.subsystem.common.db.Centro`
- `com.educaflow.subsystem.common.db.TipoUsuario`
- `com.educaflow.subsystem.common.db.CentroUsuario`
- `com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario`
- `com.axelor.auth.db.User` (extendido: `centroActivo`, `dni`, `email`, `nombre`, `apellidos`)
- `com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado` (unique: `centro`+`dni`+`tipoUsuario`)
- `com.axelor.meta.db.MetaFile`

---

### Dependencias de otros subsistemas

| Subsistema | Entidad / Utilidad | Motivo |
|---|---|---|
| `subsystem/common` | `Centro`, `TipoUsuario`, `CentroUsuario`, `CentroUsuarioTipoUsuario` | Centro activo del importador, tipos de usuario, actualización de usuarios registrados |
| `subsystem/registrousuario` | `UsuarioAutorizado` | Escritura de usuarios importados y lectura del histórico para el recálculo |
| `base/util` | `DniUtil.isValid(String)` | Validación de formato DNI/NIE fila a fila |
| Axelor core | `MetaFile`, `User` | Almacenamiento del fichero original e identificación del importador |

No hay dependencias circulares: `importacion` depende de `common` y `registrousuario`; ninguno de ellos depende de `importacion`.

---

### Operaciones

#### OP-1: Listar importaciones

- **Actor:** administrador (grupo `admins`).
- **Descripción:** Muestra todas las `TareaImportacion` del sistema, ordenadas por `fechaImportacion` descendente.
- **Datos que muestra:** `fechaImportacion`, `tipoUsuario` (nombre), `nombreFichero`, `importador` (nombre completo), `centro` (nombre), `estado`.
- **Restricción:** Solo lectura.

#### OP-2: Ver detalle de una importación

- **Actor:** administrador.
- **Descripción:** Muestra todos los campos de una `TareaImportacion` más la tabla de `ErrorImportacion` asociados.
- **Datos que muestra:** todos los campos de `TareaImportacion` + `cabecera` + tabla de errores (`numeroFila`, `documento`, `descripcion`); el fichero original es descargable.
- **Restricción:** Solo lectura.

#### OP-3: Importar fichero (crear nueva `TareaImportacion`)

- **Actor:** administrador.
- **Datos de entrada:** `tipoUsuario` (selección obligatoria), `fichero` (adjunto obligatorio).
- **Datos implícitos:** `centroActivo` y `curso` del importador; fecha actual.

**Flujo (síncrono):**

**Fase 1 — Comprobaciones previas** (si alguna falla → `TareaImportacion` FALLIDO, `cabecera` con el motivo; sin cambios en datos):
1. Verificar que el importador tiene `centroActivo` configurado (V-004).
2. Verificar que `tipoUsuario` es uno de los tipos importables (V-003).
3. Para tipos XML: validar que el fichero es XML bien formado y tiene la estructura esperada para el tipo (V-005).
4. Para tipo CSV: validar que el fichero contiene al menos un documento (V-006).
5. Para tipos XML: validar que el atributo `codigo` del elemento `<centro>` del XML coincide con `centroActivo.code` del importador (V-007).
6. Verificar que no existe una `TareaImportacion` CORRECTA con la misma combinación `(centro, tipoUsuario, fechaExportacion, curso)` (V-008).

**Fase 2 — Procesamiento de filas** (si Fase 1 pasa):
- Para cada documento del fichero: validar DNI con `DniUtil.isValid()` (V-009). Si inválido: crear `ErrorImportacion` (con `numeroFila`, `documento`, `descripcion`) y continuar. Si válido: acumular en lista de `UsuarioAutorizado` a persistir.
- Persistir `UsuarioAutorizado` válidos (upsert: si existe `(centro, dni, tipoUsuario)`, actualizar `curso` y `fechaExportacion`; si no, insertar).

**Fase 3 — Actualización de usuarios registrados** (OP-4, misma transacción):
- Ejecutar el recálculo de `CentroUsuario`/`CentroUsuarioTipoUsuario` (ver OP-4).
- Si OP-4 falla: revertir la transacción completa (ni `UsuarioAutorizado` ni cambios en registrados se persisten). Crear `TareaImportacion` FALLIDO con el error en `cabecera`.
- Si OP-4 tiene éxito: crear `TareaImportacion` CORRECTO con `cabecera` de resumen (total procesados, total válidos, total errores de fila).

> **Nota técnica (asunción A8):** `TareaImportacion` debe guardarse en una transacción separada del procesamiento de datos para que el registro FALLIDO persista incluso cuando la transacción principal se revierta.

#### OP-4: Actualizar usuarios registrados (interna, invocada por OP-3)

**Para tipos XML (PROFESOR, ALUMNO, FAMILIAR):**

Para cada `CentroUsuario` del `centro` activo con un `User.dni` que aparezca en el universo de `UsuarioAutorizado` del mismo tipo y centro:

- **`UsuarioImportadoActual`**: existe `UsuarioAutorizado` con mismo `tipo`+`dni`+`centro` con la mayor `fechaExportacion` para el `curso` activo.
- **`UsuarioImportadoAnterior`**: existe `UsuarioAutorizado` con mismo `tipo`+`dni`+`centro` con `fechaExportacion` estrictamente anterior a la mayor, para el `curso` activo.

| UsuarioImportadoActual | UsuarioImportadoAnterior | Acción sobre `CentroUsuarioTipoUsuario` |
|:---:|:---:|---|
| No | No | Eliminar tipo base (caso defensivo) |
| No | Sí | Añadir tipo EX, eliminar tipo base |
| Sí | No | Añadir tipo base, eliminar tipo EX |
| Sí | Sí | Añadir tipo base, eliminar tipo EX |

Regla complementaria: un usuario registrado no puede tener simultáneamente tipo base y su EX. Añadir uno elimina el otro.

**Para CSV (PROFESOR_EXTERNO):**
Busca `CentroUsuario` del centro activo cuyo `User.dni` coincida con algún documento del CSV y le añade `PROFESOR_EXTERNO` si no lo tiene. No aplica la tabla anterior ni gestiona tipos EX.

---

### Vistas

#### V-1: `subsysImportacion.TareaImportacion@Main-action` — Listado principal
- **Tipo:** grid (solo lectura).
- **Actores:** grupo `admins`.
- **Columnas:** `fechaImportacion`, `tipoUsuario.nombre`, `nombreFichero`, `importador.name`, `centro.name`, `estado`.
- **Acciones:** botón "Nueva importación" abre el formulario de creación; clic en fila abre el formulario de detalle.

#### V-2: Formulario de `TareaImportacion` — creación
- **Tipo:** form (editable, solo en alta nueva).
- **Actores:** grupo `admins`.
- **Campos editables:** `tipoUsuario` (selector; solo los 4 tipos importables), `fichero` (widget de upload).
- **Campos ocultos/auto:** `fechaImportacion`, `importador`, `centro`, `curso`, `estado`, `fechaExportacion`, `cabecera` — los asigna el sistema.
- **Acción:** botón "Importar" dispara OP-3. Tras guardar redirige al formulario de detalle.

#### V-3: Formulario de `TareaImportacion` — detalle (solo lectura)
- **Tipo:** form (solo lectura).
- **Actores:** grupo `admins`.
- **Campos:** todos los de `TareaImportacion`; el campo `fichero` ofrece descarga del fichero original.
- **Panel de cabecera:** `cabecera` (área de texto no editable).
- **Panel de errores:** tabla de `ErrorImportacion` (`numeroFila`, `documento`, `descripcion`); solo visible si hay errores.
- **Sin acciones de edición ni borrado.**

---

### Menús

El menuitem ya existe y **no debe recrearse**:

```xml
<menuitem name="administracionSv-importacion-menuitem"
          parent="administracionSv-menuitem"
          title="Ficheros importación"
          action="subsysImportacion.TareaImportacion@Main-action"
          groups="admins" order="2" />
```

---

### Seguridad

| Actor | Acceso |
|---|---|
| Grupo `admins` | Creación y lectura de `TareaImportacion` y `ErrorImportacion`; sin edición ni borrado desde UI |
| Resto de usuarios | Sin acceso |

**Multicentro:** el sistema es multicentro. Cada `TareaImportacion` queda vinculada al `centroActivo` del importador en el momento de crear. Los administradores ven las importaciones de todos los centros en el listado. El importador siempre opera sobre su `centroActivo`; si no tiene uno configurado, la operación se bloquea (V-004).

---

### Validaciones

| ID | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario |
|---|---|---|---|---|---|
| V-001 | `tipoUsuario` | Obligatorio | Modelo | Al crear `TareaImportacion`, si `tipoUsuario` es nulo | "El tipo de usuario es obligatorio" |
| V-002 | `fichero` | Obligatorio | Modelo | Al crear `TareaImportacion`, si `fichero` es nulo | "El fichero es obligatorio" |
| V-003 | `tipoUsuario` | Dominio | Catálogo | Al crear `TareaImportacion`, si `tipoUsuario.codigo` no es uno de: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO | "El tipo de usuario «{valor}» no es importable. Los tipos válidos son: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO" |
| V-004 | `importador.centroActivo` | Requisito previo | Negocio* | Al crear `TareaImportacion`, si el importador no tiene `centroActivo` asignado | "No puede importar: su usuario no tiene un centro activo asignado" |
| V-005 | `fichero` | Formato | Negocio* | Para tipos XML (PROFESOR, ALUMNO, FAMILIAR): si el fichero no es XML bien formado o no contiene el elemento `<centro>` con los atributos `codigo`, `curso`, `fechaExportacion` y el subelemento de lista correspondiente al tipo (`<docentes>`, `<alumnos>`, `<familiares>`) | "El fichero «{nombreFichero}» no tiene la estructura XML esperada para el tipo «{tipoUsuario}»" |
| V-006 | `fichero` | Formato | Negocio* | Para tipo CSV (PROFESOR_EXTERNO): si el fichero no contiene ninguna fila con documento tras ignorar la posible cabecera | "El fichero «{nombreFichero}» no contiene ningún documento" |
| V-007 | `fichero` (atributo `codigo`) | Coherencia | Negocio* | Para tipos XML: si el atributo `codigo` del elemento `<centro>` del fichero no coincide con el `code` del `centroActivo` del importador | "El centro del fichero «{codigoFichero}» no coincide con el centro activo del importador «{codigoCentroActivo}»" |
| V-008 | `(centro, tipoUsuario, fechaExportacion, curso)` | Unicidad | Negocio* | Al crear `TareaImportacion`: si ya existe una `TareaImportacion` con estado CORRECTO con la misma combinación `centro` + `tipoUsuario` + `fechaExportacion` + `curso` | "Ya existe una importación correcta del tipo «{tipoUsuario}» para el centro «{centro.name}» con fecha de exportación «{fechaExportacion}» en el curso {curso}" |
| V-009 | `documento` (por fila del fichero) | Formato | Catálogo | Durante el procesamiento de cada fila del fichero: si `DniUtil.isValid(documento)` devuelve false | "Fila {numeroFila}: el documento «{documento}» no tiene un formato de DNI/NIE válido; la fila se omite" |
| V-010 | `TareaImportacion` (todos sus campos) | Inmutabilidad | Negocio* | Si se intenta modificar o eliminar cualquier campo de una `TareaImportacion` ya creada | "Las importaciones no pueden modificarse ni eliminarse una vez creadas" |
| V-011 | `UsuarioAutorizado` (todos sus campos) | Inmutabilidad | Negocio* | Si se intenta modificar o eliminar cualquier campo de un `UsuarioAutorizado` ya creado | "Los usuarios autorizados no pueden modificarse ni eliminarse una vez creados" |
| V-012 | `ErrorImportacion.tareaImportacion` | Integridad referencial (cascada) | Modelo | Al eliminar una `TareaImportacion`, sus `ErrorImportacion` se eliminan en cascada | — (invariante técnica; sin UI; la eliminación de `TareaImportacion` no está expuesta en UI) |
| V-013 | `ErrorImportacion.descripcion` | Obligatorio | Modelo | Al crear `ErrorImportacion`, si `descripcion` es nulo o vacío | Invariante técnica: toda entrada de error debe tener descripción; violación indica error interno del sistema |

---

### Máquina de estados

`TareaImportacion` tiene dos estados finales, asignados en el momento de creación. No hay transiciones entre ellos.

```
[OP-3 lanzada]
    │
    ├─ Fase 1 falla ──────────────────────────────→ FALLIDO (cabecera con motivo; sin datos)
    │
    └─ Fase 1 OK → Fase 2 → Fase 3 (OP-4)
                                   │
                                   ├─ OP-4 falla → ROLLBACK datos → FALLIDO (cabecera con error)
                                   │
                                   └─ OP-4 OK ──────────────────→ CORRECTO (cabecera con resumen)
```

**CORRECTO:** el proceso terminó sin errores críticos (puede haber `ErrorImportacion` de filas con DNI inválido).
**FALLIDO:** alguna comprobación previa falló, o OP-4 lanzó un error que provocó la reversión de los datos.

---

### Asunciones a confirmar

Las reglas marcadas con `*` en la columna Origen son decisiones de negocio asumidas. Deben confirmarse antes del diseño:

1. **A1* (V-005) — Estructura XML exacta:** Se asume que la validación de formato comprueba que el elemento raíz es `<centro>` con los atributos `codigo`, `curso`, `fechaExportacion` y el subelemento de lista correspondiente al tipo. ¿Existe un XSD oficial o alguna validación adicional de estructura?

2. **A2* (V-006) — Cabecera del CSV:** Se asume que si la primera línea del CSV no es un DNI/NIE válido se ignora (detección automática de cabecera). ¿O siempre se salta la primera línea independientemente?

3. **A3* (V-007) — Case sensitivity del código de centro:** Se asume que la comparación `codigoFichero` vs `centroActivo.code` es sensible a mayúsculas. ¿Debe ser insensible?

4. **A4* (V-008) — Ámbito de unicidad de importación:** Se asume que solo las `TareaImportacion` con estado CORRECTO bloquean (las FALLIDAS no impiden un reintento). ¿Es correcto?

5. **A5* (V-010) — Inmutabilidad total:** Se asume que ningún campo de `TareaImportacion` puede modificarse tras la creación. ¿Hay algún campo administrativo editable?

6. **A6* — CORRECTO con errores parciales:** Una importación es CORRECTO aunque haya filas omitidas por DNI inválido, siempre que al menos un usuario válido se haya procesado y OP-4 tenga éxito. ¿Si todas las filas son inválidas (cero usuarios guardados), el estado es CORRECTO o FALLIDO?

7. **A7* — `fechaExportacion` en CSV (PROFESOR_EXTERNO):** Se asume que es la fecha actual del sistema en el momento de ejecutar la importación (campo `Date`, sin componente horario). ¿O debe ser fecha+hora?

8. **A8* — Transaccionalidad cuando OP-4 falla:** El enunciado implica que `TareaImportacion` FALLIDO se guarda incluso cuando la transacción de datos se revierte. Esto requiere que `TareaImportacion` se persista en una transacción separada después del rollback de datos. ¿Es correcto?

9. **A9* — Selector de `tipoUsuario` en formulario nuevo:** El selector debe mostrar solo los 4 tipos importables (excluir EXPROFESOR, EXALUMNO, EXFAMILIAR). ¿Hay algún otro criterio de filtrado?

10. **A10* — Caso defensivo No/No en tabla XML:** Cuando un usuario registrado no tiene ni tipo base ni tipo EX, la acción "eliminar tipo base" no tiene efecto práctico. ¿El comportamiento esperado es ninguna acción? ¿O debe asignarse el tipo EX en ese caso?
