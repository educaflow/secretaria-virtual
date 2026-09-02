---
type: implementation-task
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/firmas/controller/TareaFirmaController.java` | Modificar | k-sistemas (controladores.md) | Añade los `@CallMethod` `validateFirmarEnServidor` y `firmarEnServidor` |

Es `Acción: Modificar`: la clase **ya existe**. Se edita la clase existente añadiendo **solo** los dos
`@CallMethod` nuevos y **conservando** todo lo demás (los cuatro `@CallMethod` actuales, campos e imports).

## Texto del diseño (verbatim)

### Paso 6 — Controlador: los dos `@CallMethod` de la firma en servidor

**Fichero:** `src/main/java/com/educaflow/subsystem/firmas/controller/TareaFirmaController.java` (Modificar)

Se conservan los cuatro `@CallMethod` actuales (`firmarDocumentosConAutoFirma`, `marcarComoFirmada`,
`marcarComoRechazada`, `validarDocumentosFirmados`) sin ningún cambio. Delta:

```java
// Clase: com.educaflow.subsystem.firmas.controller.TareaFirmaController
@CallMethod
public void validateFirmarEnServidor(ActionRequest actionRequest, ActionResponse actionResponse);
//   Sin @Transactional: solo valida, no escribe.
//   Resuelve TareaFirmaService con modelServiceFactory.resolve(TareaFirma.class); construye
//   ActionRequestHelper<TareaFirma> y ActionResponseHelper; obtiene el original con getOriginalModel() y la
//   entidad con getModel(tareaFirmaService.allowPropertiesFirmarEnServidor()); delega en
//   tareaFirmaService.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal) y, si hay mensajes, los entrega
//   con actionResponseHelper.doResponseBusinessMessagesAsError(...).
//   Es la contrapartida de la acción de vista …-Remote-validateFirmarEnServidor-action: el nombre del método
//   coincide con el {nombreFuncionJava} embebido en el nombre de la acción (k-vistas/actions.md).

@CallMethod
@Transactional
public void firmarEnServidor(ActionRequest actionRequest, ActionResponse actionResponse);
//   Con @Transactional: escribe en BD (los MetaFile firmados, los DocumentoFirma y la propia tarea). Es lo que
//   hace que un fallo de firma revierta TODO (RN-TareaFirma-002).
//   Resuelve el servicio igual que arriba, obtiene original y entidad con la MISMA whitelist
//   allowPropertiesFirmarEnServidor() y llama a tareaFirmaService.firmarEnServidor(tareaFirma, tareaFirmaOriginal).
//   No monta ninguna respuesta: el cierre de la ventana lo hace el <action name="force-back"/> del
//   action-group de la vista, igual que ya ocurre con el flujo de AutoFirma.
//   MUST NOT capturar la excepción de negocio del servicio para reempaquetarla: si la firma falla, la
//   ValidationException que lanza BusinessMessages.throwIfInvalid llega al cliente como error, detiene la
//   cadena de acciones (el force-back no se ejecuta) y revierte la transacción — que es justo lo que piden
//   RUI-…-017 y RUI-…-018.
```

Los parámetros se llaman **`actionRequest`** y **`actionResponse`** (regla de `k-sistemas/controladores.md`).

**Verificación:** `grep -n "ActionRequest \|ActionResponse " src/main/java/com/educaflow/subsystem/firmas/controller/TareaFirmaController.java`
no debe mostrar ningún parámetro llamado `request`/`response`/`req`/`resp`.

### Frontera de confianza — AllowProperties por acción

### `TareaFirmaServiceImpl.firmarEnServidor` (invocado desde `TareaFirmaController.firmarEnServidor` y `TareaFirmaController.validateFirmarEnServidor`)

Entidad: `TareaFirma`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Firmar en el servidor` de `entity-TareaFirma.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `claveFirma` | cliente | sí | Único input del usuario en esta acción (`Input AllowProperties: clave de firma`). Es transitorio: no se persiste nunca y `fireActionRule_DescartarClaveFirma` lo anula al terminar. |
| `situacionFirma` | servidor | **NO** | Campo derivado (`CC-TareaFirma-001`, momento lectura): lo recalcula el getter en cada lectura desde el DNI del firmante y el subsistema de criptografía. Aunque el cliente lo mandara, el getter lo sobrescribe. |
| `estadoTareaFirma` | servidor | **NO** | Asignado en `firmarEnServidor` → `fireActionRule_ResolverComoFirmada` (incondicional). |
| `fechaResolucion` | servidor | **NO** | Asignado en `firmarEnServidor` → `fireActionRule_ResolverComoFirmada` (incondicional). |
| `documentosFirma` | servidor | **NO** | Las versiones firmadas las produce el servidor en `fireActionRule_FirmarDocumentosEnServidor`. Dejarlo fuera es lo que impide que el cliente cuele un PDF «firmado» propio. |
| `firmante` | servidor | **NO** | Lo fijó quien creó la tarea; la acción no lo toca. Que esté fuera es lo que evita que alguien se autoasigne la tarea de otro. |
| `fechaSolicitud`, `motivoFirma`, `motivoRechazo`, `firmaRapida` | servidor | **NO** | Fijados al crear la tarea; esta acción no los toca. |
| `fqcnFirmaNotifier`, `fqcnCallBackData`, `callBackData` | servidor | **NO** | Los fija quien encarga la firma. Son especialmente sensibles: `fqcnFirmaNotifier` acaba en un `Class.forName` + `Beans.get`, así que dejarlos fuera de la whitelist es obligatorio. |
| `x`, `y`, `width`, `height`, `page` | servidor | **NO** | El recuadro y la página de la firma los fija quien encarga la firma; la acción los lee, no los escribe. |

### Notas y supuestos aplicables

11. **Ambigüedad resuelta — nombre de la acción.** La spec llama a la acción «Firmar en el servidor» y al botón
    «Firmar todos los documentos y finalizar». El diseño nombra el método del servicio, el `@CallMethod` y las
    acciones de vista como `firmarEnServidor` (y `validateFirmarEnServidor`), que es el nombre de la **acción**
    del `entity-*.md`, no el rótulo del botón; así el par acción ↔ validador y el `Remote-{nombreFuncionJava}`
    quedan alineados sin ambigüedad.
