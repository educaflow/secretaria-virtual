---
type: analysis
---

## Análisis Funcional: Firmas de Documentos

**Tipo:** subsistema
**Capa:** subsystem/firmas
**Descripción:** Permitir a otros sistemas de la aplicación solicitar a un usuario la firma digital de uno o varios documentos PDF, gestionar el ciclo de vida de la solicitud y devolver el resultado al sistema solicitante mediante un mecanismo de notificación.

### Entidades

- `TareaFirma` — solicitud de firma asignada a un usuario.
  - `firmante` → referencia al usuario de la aplicación (`com.axelor.auth.db.User`), obligatorio.
  - `documentosFirma` → colección uno-a-muchos de `DocumentoFirma`.
  - `fechaSolicitud` (datetime, obligatoria) — momento de la creación.
  - `fechaResolucion` (datetime, opcional) — momento en que pasa a un estado final.
  - `firmaRapida` (boolean, opcional) — marca para firma automática sin intervención del usuario si éste tiene certificado válido. Reservado para uso futuro; actualmente no se aplica en ningún flujo.
  - `estadoTareaFirma` (enum, obligatorio) — uno de `PENDIENTE`, `FIRMADO`, `RECHAZADO`.
  - `motivoFirma` (string, obligatorio) — razón por la que se solicita la firma; aportado por el sistema solicitante.
  - `motivoRechazo` (string largo, opcional) — explicación obligatoria cuando el usuario rechaza la firma.
  - `fqcnFirmaNotifier` (string, opcional) — nombre cualificado de la clase que el sistema solicitante implementa para recibir la notificación del resultado.
  - `fqcnCallBackData` (string, opcional) — nombre cualificado de la clase del objeto de callback adjunto.
  - `callBackData` (string largo, opcional) — datos de contexto que el solicitante adjunta y recupera al ser notificado.
  - `x`, `y`, `width`, `height` (decimal con 2 decimales, obligatorios) — coordenadas y tamaño del área donde se estampa la firma sobre el PDF.
  - `page` (entero, obligatorio) — número de página donde se estampa la firma.

- `DocumentoFirma` — cada uno de los PDF dentro de una tarea de firma.
  - `tareaFirma` → referencia a la `TareaFirma` padre, obligatorio.
  - `documentoOriginal` → referencia al fichero PDF original a firmar (`com.axelor.meta.db.MetaFile`), obligatorio.
  - `documentoFirmado` → referencia al fichero PDF firmado (`com.axelor.meta.db.MetaFile`), opcional (se rellena al pasar a FIRMADO).

### Dependencias de otros subsistemas

- `base/infrastructure/pdf` — utilidades de validación de firma sobre PDF y representación del área de firma (rectángulo).
- `base/infrastructure/autofirma` — integración con la aplicación desktop AutoFirma para la firma efectiva en el cliente.
- `base/infrastructure/validation/messages` — `BusinessMessages` y `BusinessException` para acumular y devolver errores al usuario.
- `base/infrastructure/metafile` — utilidades sobre MetaFile (clonado, comprobación de tipo PDF).
- `base/util` — `JsonUtil` (serialización del `callBackData`), `SecurityUtil` (acceso al usuario de sesión cuando lo necesite el cliente).

### Operaciones

- **Crear tarea de firma**: la realiza un sistema cliente (no un usuario directamente) pasando: el firmante, la lista de PDF a firmar, el motivo, el área de firma (rectángulo) y la clase del notificador con sus datos de callback opcionales. La tarea se crea en estado `PENDIENTE` con la fecha de solicitud actual; los PDF originales se clonan (no se referencian) para que el sistema solicitante no pueda alterarlos posteriormente.
- **Listar tareas pendientes del usuario**: el firmante ve sus propias tareas en estado `PENDIENTE` para resolverlas.
- **Listar tareas firmadas del usuario**: el firmante puede consultar de solo lectura sus tareas en estado `FIRMADO`.
- **Listar tareas rechazadas del usuario**: el firmante puede consultar de solo lectura sus tareas en estado `RECHAZADO`.
- **Listar todas las tareas**: vista global, sin filtrar por estado ni por firmante. Pensada para administradores.
- **Resolver una tarea: rechazar la firma**: el firmante introduce el motivo de rechazo y confirma; la tarea pasa a `RECHAZADO` con la fecha de resolución actual y se notifica al sistema solicitante.
- **Resolver una tarea: firmar los documentos**: el firmante invoca AutoFirma, que firma los PDF originales con su certificado dentro del área indicada y devuelve los PDF firmados; el servidor valida que cada PDF firmado se corresponde con su original y está firmado con el DNI del firmante; si la validación pasa, la tarea pasa a `FIRMADO` con la fecha de resolución actual y se notifica al sistema solicitante.
- **Notificar al sistema solicitante**: efecto secundario común a las dos transiciones finales. Se invoca el callback con la tarea y los datos de contexto que el solicitante adjuntó al crearla.

### Vistas

- **Pendientes**: listado y formulario editable de las tareas en `PENDIENTE` del firmante actual. El formulario permite avanzar por un flujo guiado: panel inicial con dos opciones (rechazar / firmar); panel de rechazo con campo de motivo; panel de firma con explicación y disparador de AutoFirma. Solo es editable lo necesario en cada paso (motivo de rechazo o documentos firmados).
- **Firmados**: listado y formulario solo lectura de las tareas en `FIRMADO` del firmante actual. Muestra los PDF firmados.
- **Rechazados**: listado y formulario solo lectura de las tareas en `RECHAZADO` del firmante actual. Muestra los PDF originales y el motivo de rechazo.
- **Todos**: listado y formulario solo lectura de todas las tareas, sin filtrar por estado ni firmante. Muestra los PDF originales y, si existen, los firmados.

### Menús

- "Firmar documentos" (raíz) → "Todos", "Pendientes", "Firmados", "Rechazados" (en este orden).

### Seguridad

- Cada usuario solo ve y edita sus propias `TareaFirma` (filtrado por filas: `firmante = usuario actual`).
- Cada usuario solo ve y edita sus propias `DocumentoFirma` (a través de la `tareaFirma` padre cuyo firmante es el usuario actual).
- La creación de tareas no la hace el usuario directamente sino el sistema solicitante mediante el servicio (no se expone "crear" en la vista).
- El borrado de tareas no está permitido por la vista para ningún usuario.
- La vista "Todos" es global y está pensada para administradores.
- Multicentro: no — las tareas no se filtran por centro; se filtran por firmante.

### Validaciones

| ID    | Campo(s) | Tipo | Origen | Condición de aplicación | Mensaje al usuario |
|-------|----------|------|--------|--------------------------|---------------------|
| V-001 | `firmante` | Obligatoriedad | Modelo | Siempre | "Indique el firmante de la tarea." |
| V-002 | `fechaSolicitud` | Obligatoriedad | Modelo | Siempre (asignada por el sistema en la creación) | "La fecha de solicitud es obligatoria." |
| V-003 | `estadoTareaFirma` | Obligatoriedad + Dominio finito | Modelo | Siempre. Valores válidos: PENDIENTE, FIRMADO, RECHAZADO | "El estado '{valor}' no es válido. Valores permitidos: PENDIENTE, FIRMADO, RECHAZADO." |
| V-004 | `motivoFirma` | Obligatoriedad + No-blanco | Modelo | Siempre (lo aporta el sistema solicitante) | "Introduzca el motivo de la firma." |
| V-005 | `motivoRechazo` | Obligatoriedad condicional | Negocio* | Al rechazar la firma | "Indique el motivo del rechazo." |
| V-006 | `documentosFirma[*].documentoFirmado` | Validez de la firma criptográfica | Negocio* | Al firmar (antes de pasar a FIRMADO) | "El documento '{fileName}' no está firmado correctamente: {causa devuelta por el validador, p.ej. firma inválida, contenido alterado, DNI no coincide}." |
| V-007 | `TareaFirma` (filas) | Autorización por filas | Negocio* | Siempre, en lecturas y escrituras sobre la entidad | (no aplica mensaje de UI; el sistema simplemente no muestra los registros que no son del firmante actual) |
| V-008 | `DocumentoFirma` (filas) | Autorización por filas | Negocio* | Siempre, en lecturas y escrituras sobre la entidad | (no aplica mensaje de UI; análogo a V-007 pero a través de la `tareaFirma` padre) |

### Máquina de estados

- **Estados**: `PENDIENTE` (inicial), `FIRMADO` (final), `RECHAZADO` (final).
- **Transiciones permitidas**:
  - (creación) → `PENDIENTE`. Disparador: petición desde un sistema cliente. Acción posterior: ninguna.
  - `PENDIENTE` → `FIRMADO`. Disparador: firmante completa la firma con AutoFirma desde el formulario de pendientes. Condición previa: V-006 (firma criptográfica válida). Acción posterior: notificar al sistema solicitante (callback) y registrar `fechaResolucion`.
  - `PENDIENTE` → `RECHAZADO`. Disparador: firmante introduce motivo y confirma rechazo desde el formulario de pendientes. Condición previa: V-005 (motivo de rechazo informado). Acción posterior: notificar al sistema solicitante y registrar `fechaResolucion`.
- **Transiciones inválidas**: cualquier otra (FIRMADO/RECHAZADO son finales). No hay flujo definido para revertir un estado final.
- **Campos editables por estado**:

| Campo | PENDIENTE | FIRMADO | RECHAZADO |
|-------|-----------|---------|-----------|
| `motivoRechazo` | E (sólo al rechazar) | R | R |
| `documentosFirma[*].documentoFirmado` | E (sólo al firmar) | R | R |
| `fechaResolucion` | Auto (al pasar a final) | R | R |
| Resto de campos | R (los rellena el sistema cliente al crear) | R | R |

### Asunciones a confirmar

- **A1*** (V-005): cuando el usuario rechaza, el campo `motivoRechazo` es obligatorio y la validación se ejecuta en cliente como bloqueo previo a invocar al servidor.
- **A2*** (V-006): la validación criptográfica de la firma (que el PDF firmado se corresponde con el original y está firmado con el DNI del firmante) se ejecuta en servidor antes de pasar la tarea a `FIRMADO`.
- **A3*** (V-007/V-008): la autorización por filas se aplica tanto a lectura como a escritura. Crear y borrar nunca están permitidos por permiso (la creación pasa por el servicio del subsistema; el borrado no se contempla).
- **A4*** (operación "Todos"): la vista global "Todos" no aplica filtro por filas adicional — el resto de permisos del usuario son los que limitan lo visible. Si un usuario no administrador puede acceder a este menú es decisión separada del subsistema.
- **A5***: el `motivoFirma` lo aporta el sistema solicitante en la creación y se muestra al firmante; no es editable por el firmante en ningún momento.
- **A6***: los PDF originales se clonan al crear la tarea (no se referencian) para impedir que el sistema solicitante los modifique después de haberlos enviado a firmar.
- **A7***: el callback al sistema solicitante se invoca *después* de persistir la transición a estado final; si el callback falla, la transición ya está registrada en BD (no se hace rollback).
- **A8***: el campo `firmaRapida` se declara en el modelo pero no se usa en ningún flujo en esta versión; queda reservado para una operación futura de firma automática.
- **A9***: el campo `page` (página de estampado) es obligatorio en el modelo; se asume que el sistema solicitante lo aporta al crear la tarea por una vía no detallada en este análisis.

### Notas de diseño relevantes (no son reglas, son decisiones de arquitectura)

- El subsistema expone su funcionalidad a los sistemas clientes a través de un servicio público y de una interfaz de notificador (callback). Los sistemas clientes implementan la interfaz y el subsistema invoca el callback resolviendo la implementación dinámicamente desde el contenedor de DI.
- Los datos de callback (`callBackData`) se persisten serializados en JSON junto al nombre cualificado de su clase, para poder reconstruir el objeto al notificar sin acoplar el subsistema a tipos concretos del solicitante.
