---
type: analysis
---

## Análisis Funcional: Importación de usuarios autorizados desde CSV

**Tipo:** subsistema (modificación del existente)
**Capa:** subsystem/importacion
**Descripción:** Implementa la lógica de importación masiva de usuarios autorizados a partir de un fichero CSV con un DNI por línea, creando los registros de usuario autorizado en el centro y curso activos del importador, y registrando un log con el resumen y los errores del proceso.

---

### Entidades

#### `TareaImportacion` (existente, sin cambios estructurales)

Registra cada ejecución de una importación. El registro se guarda siempre, incluso cuando la importación falla.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `usuario` | User | required | Usuario que realiza la importación; asignado por el sistema |
| `centro` | Centro | — | Centro activo del importador; asignado por el sistema |
| `curso` | Integer | — | Curso activo del centro; asignado por el sistema |
| `fechaImportacion` | DateTime | required | Fecha y hora de inicio; asignado por el sistema |
| `fechaExportacion` | DateTime | — | Fecha de procesamiento del fichero; asignado por el sistema |
| `tipoFichero` | TipoFicheroImportacion | required | Tipo de fichero elegido por el importador |
| `fichero` | MetaFile | required | Fichero CSV subido por el importador |
| `estado` | Boolean | — | `true` = correcta; `false` = fallida |
| `log` | String (large, multiline) | — | Resumen y detalle de errores; generado por el sistema |

Enum `TipoFicheroImportacion` (existente, sin cambios): `PROFESOR`, `ALUMNO`, `FAMILIAR`, `PROFESOR_EXTERNO`. El valor `PROFESOR_EXTERNO` activa el procesamiento CSV descrito en esta historia; en el futuro otros tipos podrían también mapearse a CSV.

---

#### `UsuarioAutorizado` (existente, modificación de constraint)

Representa a un usuario con permiso para registrarse en el sistema. La única modificación en esta historia es ampliar la constraint de unicidad para incluir el curso.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| `centro` | Centro | required | Centro al que pertenece el usuario autorizado |
| `dni` | String | required | DNI/NIF/NIE del usuario |
| `tipoUsuario` | TipoUsuario | required | Tipo de usuario asignado |
| `curso` | Integer | — | Curso académico activo del centro en el momento de la importación |
| `fechaExportacion` | Date | — | Fecha de exportación del registro |

**Cambio de constraint:** la combinación única pasa de `(centro, dni, tipoUsuario)` a `(centro, dni, tipoUsuario, curso)`. Un mismo DNI puede coexistir en varios registros si difieren en centro, tipo de usuario o curso.

---

### Dependencias de otros subsistemas

| Subsistema | Entidad / Concepto | Motivo |
|---|---|---|
| `subsystem/registrousuario` | `UsuarioAutorizado` | Se crean nuevos registros durante el procesamiento del CSV; se modifica su constraint de unicidad |
| `subsystem/common` | `Centro` | Se obtiene el centro activo del importador y el curso activo de ese centro |
| `subsystem/common` | `TipoUsuario` | Se asigna a cada usuario autorizado creado |
| `base/util` | Validación y normalización de DNI | Se normaliza y valida cada DNI leído del CSV |

No hay dependencias circulares: `importacion` depende de `registrousuario` y `common`, pero ninguno de estos depende de `importacion`.

---

### Operaciones

#### Ejecutar importación de usuarios autorizados desde CSV

**Actor:** Usuario del grupo `admins`.
**Disparador:** El importador selecciona el tipo de fichero, sube el CSV y confirma la importación.

**Flujo:**

1. El sistema registra el contexto de la tarea: usuario en sesión, fecha y hora actuales.
2. El sistema verifica si el importador tiene centro activo asignado:
   - Si **no tiene centro activo**: el sistema escribe en el log que no hay centro activo, marca la tarea como fallida y guarda el registro. El proceso termina aquí sin leer el fichero.
3. El sistema obtiene el centro activo del importador y el curso activo de ese centro.
4. El sistema lee el fichero CSV línea a línea (una línea = un DNI, sin cabecera). Para cada línea:
   a. El sistema normaliza el valor (elimina espacios, convierte a mayúsculas).
   b. El sistema valida si el DNI normalizado es válido. Si no lo es, anota en el log el valor recibido y el motivo ("DNI no válido"), incrementa el contador de errores y continúa con la siguiente línea. El fallo de un registro no interrumpe el procesamiento del resto.
   c. Si no existe un usuario autorizado con la combinación exacta (centro, DNI normalizado, tipo de usuario, curso): el sistema crea un nuevo usuario autorizado con esos datos e incrementa el contador de importados.
   d. Si ya existe un usuario autorizado con esa combinación exacta: el sistema ignora el registro e incrementa el contador de ignorados.
5. Al finalizar el procesamiento, el sistema construye el log final con: resumen de contadores (importados, ignorados, errores) y listado de los registros fallidos (DNI + motivo). Los éxitos e ignorados no se listan individualmente.
6. El sistema guarda la tarea con: `estado=true`, el log generado, el centro y curso del importador, y la fecha de procesamiento.

**Garantías:** el fallo individual de un registro no interrumpe el proceso; la tarea se guarda siempre; no se modifican ni eliminan usuarios autorizados existentes, solo se crean nuevos.

---

### Vistas

Las vistas del subsistema ya están implementadas y **no se modifican** en esta historia:
- **Panel de entrada** (visible mientras el registro es nuevo): selector de tipo de fichero y campo de carga del CSV.
- **Panel de resultado** (visible una vez guardado el registro): log de la importación con el resumen y los errores detallados, en modo solo lectura.

El selector de tipo de fichero permanece en la vista; la asociación entre el tipo `PROFESOR_EXTERNO` y el procesador CSV ya existe.

---

### Menús

El menú de acceso a la funcionalidad de importación ya existe y **no se modifica**.

---

### Seguridad

- Solo los usuarios del grupo `admins` de Axelor pueden acceder a la funcionalidad de importación.
- El centro asignado a la importación es siempre el centro activo del importador; no puede elegirse otro.
- El curso asignado es siempre el curso activo del centro; no puede elegirse otro.
- Un administrador sin centro activo no puede completar una importación con éxito: la tarea se guarda como fallida.
- Multicentro: no (el proceso opera exclusivamente sobre el centro activo del importador).

---

### Validaciones (`V-XXX`)

| ID | Campo(s) | Descripción | Condición | Mensaje al usuario |
|---|---|---|---|---|
| V-001 | `tipoFichero` | El tipo de fichero es obligatorio | `tipoFichero` está vacío al confirmar | "El tipo de fichero es obligatorio." |
| V-002 | `fichero` | El fichero CSV es obligatorio | `fichero` está vacío al confirmar | "El fichero CSV es obligatorio." |

> V-001 y V-002 ya están implementadas en el flujo existente; se documentan por trazabilidad. No se añaden nuevas validaciones que bloqueen el guardado.
>
> "DNI inválido" y "sin centro activo" no son V-XXX: no bloquean el `insert` — la tarea se guarda igualmente. Se clasifican como R-XXX.

---

### Reglas de negocio (`R-XXX`)

| ID | Descripción | Entidad | Método | Momento | Más información |
|---|---|---|---|---|---|
| R-001 | El sistema asigna los campos de contexto: usuario en sesión, fecha y hora actuales, y estado inicial `false` | `TareaImportacion` | insert | Antes | Ya implementado. Incluye la inicialización del log a nulo. |
| R-002 | Si el importador no tiene centro activo, el sistema registra el fallo global en el log, asigna `estado=false` y finaliza la importación sin procesar el fichero | `TareaImportacion` | insert | Después | La tarea se guarda igualmente con `estado=false`. Ningún registro del CSV se procesa. |
| R-003 | El sistema lee el fichero CSV línea a línea y normaliza cada valor (elimina espacios, convierte a mayúsculas). Las líneas vacías se descartan silenciosamente | `TareaImportacion` | insert | Después | El CSV no tiene cabecera; cada línea contiene exactamente un DNI. |
| R-004 | Para cada DNI candidato, el sistema valida su formato; si no es válido, anota en el log el valor recibido y el motivo ("DNI no válido"), incrementa el contador de errores y continúa con el siguiente | `TareaImportacion` | insert | Después | El fallo de un registro individual no detiene el procesamiento del resto. |
| R-005 | Si no existe un usuario autorizado con la combinación exacta (centro, DNI normalizado, tipo de usuario, curso), el sistema crea un nuevo registro de usuario autorizado con esos valores e incrementa el contador de importados | `TareaImportacion` | insert | Después | Aplica tanto si el DNI no existe en absoluto como si existe con otra combinación (distinto centro, tipo o curso). |
| R-006 | Si ya existe un usuario autorizado con la combinación exacta (centro, DNI normalizado, tipo de usuario, curso), el sistema contabiliza el registro como ignorado y continúa sin crear nada | `TareaImportacion` | insert | Después | No se crea ni modifica ningún usuario autorizado. |
| R-007 | Al finalizar el procesamiento, el sistema construye el log final (resumen de contadores + listado de errores con DNI y motivo), asigna `estado=true`, el centro y curso del importador, y la fecha de procesamiento | `TareaImportacion` | insert | Después | `estado=true` si el proceso llegó a procesar el fichero, aunque haya errores parciales. `estado=false` solo si hubo fallo global (R-002). Los éxitos e ignorados no se listan individualmente. |

---

### Reglas de UI (`U-XXX`)

| ID | Disparador | Efecto | Campo/Panel afectado | Condición |
|---|---|---|---|---|
| U-001 | continuo | El panel de entrada (selector de tipo y campo de fichero) se muestra en modo editable | Panel de entrada | El registro no tiene identificador (aún no guardado) |
| U-002 | continuo | El panel de resultado (log, estado, fechas) se muestra en modo solo lectura | Panel de resultado | El registro tiene identificador (ya guardado) |

> U-001 y U-002 ya están implementadas. Se documentan por trazabilidad. No hay nuevas reglas de UI en esta historia.

---

### Asunciones a confirmar

1. **R-003\*** — Las líneas vacías del CSV se descartan silenciosamente, sin contabilizarlas como error ni como ignorado. Si deben contabilizarse como error, hay que añadir una regla específica.
2. **R-007\*** — El `estado` de la tarea se marca como `true` aunque haya DNIs inválidos o registros ignorados, siempre que el proceso haya podido ejecutarse (importador con centro activo). `false` queda reservado exclusivamente para el fallo global (R-002). Si un procesamiento donde el 100% de los registros falla individualmente también deba resultar en `estado=false`, debe confirmarse.
3. **R-005\*** — El tipo de usuario a asignar al `UsuarioAutorizado` se determina a partir del `tipoFichero` de la tarea (existe un mapeo entre el tipo de fichero y la entidad `TipoUsuario`). Si ese mapeo no está previamente definido, el proceso no puede completarse.
4. **R-003\*** — Si el `curso` del centro es nulo (no configurado), el campo `curso` del usuario autorizado creado queda nulo. La constraint unique trata `null` como un valor distinguible: dos registros con `(centro, dni, tipoUsuario, null)` serían duplicados entre sí. Si la base de datos trata `null` de otra manera, debe validarse con el equipo técnico.
5. **R-003\*** — La codificación esperada del fichero CSV es UTF-8. Si el sistema detecta un problema de lectura (codificación inesperada, fichero corrupto), la importación completa se marca como fallida con el motivo en el log.
