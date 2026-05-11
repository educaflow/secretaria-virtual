---
type: design-guidelines
---

# Guías de diseño — Subsistema firmas

Este fichero recoge decisiones de diseño específicas del subsistema `firmas` que no se deducen del análisis ni están cubiertas por los skills genéricos (`k-sistemas`, `k-vistas`, `k-seguridad`). Son convenciones que un diseñador independiente no escogería por defecto.

## 1. Mecanismo de notificación al sistema solicitante: callback por FQCN + JSON, no referencia tipada

El servicio de creación de tarea de firma recibe **la clase** del notificador (`Class<? extends TareaFirmaNotifier>`) y un objeto opaco de callback (`Object`).

Lo que se persiste en la entidad `TareaFirma`:

- `fqcnFirmaNotifier` (string): nombre cualificado (FQCN) de la clase del notificador.
- `fqcnCallBackData` (string): nombre cualificado de la clase del objeto de callback (puede ser null).
- `callBackData` (string largo): el objeto de callback serializado a JSON (puede ser null).

Al notificar (transición a estado final):

1. Se resuelve la clase del notificador por reflexión: `Class.forName(fqcnFirmaNotifier)`.
2. Se obtiene la instancia del contenedor de DI: `Beans.get(notifierClass)` — el sistema solicitante debe registrar su notifier en Guice.
3. Si hay `fqcnCallBackData`, se reconstruye el objeto: `Class.forName(fqcnCallBackData)` + `JsonUtil.fromJson(callBackData, clase)`.
4. Se invoca `notifier.notify(tareaFirma, callBackData)`. Cualquier excepción del notifier se propaga (no se captura).

**Por qué no usar una referencia tipada o un event bus:**

- Una referencia tipada acoplaría el subsistema al tipo del solicitante, rompiendo el sentido de subsistema reutilizable.
- Un event bus exigiría que cada solicitante registre listeners filtrando por algún criterio (id de tarea, etc.), añadiendo complejidad y un punto de fallo más.
- Persistir FQCN+JSON permite que la notificación funcione aunque el solicitante haya redesplegado entre la creación y la resolución (mientras la clase y el contrato JSON sigan existiendo).

**Consecuencia para el diseño:** la transición a estado final debe persistir *antes* de invocar el callback. Si el callback falla, la transición ya está en BD (no se hace rollback). Esto es intencional: se prefiere consistencia del subsistema sobre el éxito del callback del cliente.

## 2. Clonado del PDF original al crear la tarea, no referencia directa

Al insertar una `TareaFirma`, los `MetaFile` del documento original que aporta el solicitante se **clonan** (mediante el helper de clonado de MetaFile) y es la copia la que se guarda en `documentoOriginal`. No se referencia el `MetaFile` original.

**Por qué:** el sistema solicitante podría modificar o borrar el `MetaFile` después de haber enviado la solicitud (entre la creación y la firma). Si se referenciara directamente, el firmante podría acabar firmando un documento distinto al que el solicitante envió. La copia inmutable garantiza que lo que el firmante ve y firma es exactamente lo que el solicitante envió en el momento de la creación.

**Consecuencia para el diseño:** el método `insert` del servicio recorre la lista de documentos del DTO y, para cada uno, clona el MetaFile antes de asignarlo al `DocumentoFirma`. Es un punto explícito a documentar en el comentario del cuerpo del método.

