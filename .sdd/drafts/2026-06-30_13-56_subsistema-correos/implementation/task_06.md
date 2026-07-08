---
type: implementation-task
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java` | Crear | k-sistemas (controladores.md) | `@CallMethod` de `reenviar` |

## Texto del diseño (verbatim, `design.md`, Paso 5 — Controladores)

#### `com.educaflow.subsystem.correos.controller.CorreoController`

```java
package com.educaflow.subsystem.correos.controller;

public class CorreoController {

    @com.google.inject.Inject
    private com.axelor.db.modelservice.ModelServiceFactory modelServiceFactory;

    @com.axelor.meta.CallMethod
    public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Resuelve CorreoService; ActionRequestHelper<Correo> con getOriginalModel(); llama a
    //   correoService.validateReenviar(entidadOriginal, entidadOriginal) [se compara consigo misma:
    //   no hay "nuevo" distinto de "original" en esta acción, ver k-sistemas/controladores.md patrón
    //   type1]; si Optional.isPresent(), actionResponseHelper.doResponseBusinessMessagesAsError(...).
    //   Delega en CorreoService.validateReenviar — NO valida inline (k-sistemas/controladores.md
    //   "NO valida inline con throw new BusinessException").

    @com.axelor.meta.CallMethod
    @com.google.inject.persist.Transactional
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);
    //   ActionRequestHelper<Correo> actionRequestHelper = new ActionRequestHelper(actionRequest, Correo.class);
    //   Correo entidadOriginal = actionRequestHelper.getOriginalModel();
    //   Correo entidad = actionRequestHelper.getModel(correoService.allowPropertiesReenviar());
    //   correoService.reenviar(entidad, entidadOriginal);
    //   actionResponse.setNotify(I18n.get("El reenvío del correo se ha puesto en marcha."));
    //     (satisface RUI-correos-centro-formulario-004; no molesta en la pantalla de administración,
    //      que no exige nada al respecto)
    //   actionResponse.setSignal("refresh-tab", null);
}
```

No hay `AdjuntoController`: `Adjunto` no tiene ninguna acción propia más allá de las cubiertas por otras tareas (su alta la cubre el endpoint REST automático dentro del alta en cascada del `Correo`; su descarga la resuelve directamente el widget `binary-link` contra el endpoint de `MetaFile`, sin pasar por ningún controlador propio — ver `k-sistemas/modelos.md` y las vistas de la Tarea 11).

**Verificar:** `grep -n "actionRequest, ActionResponse actionResponse" src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java` — los parámetros se llaman siempre `actionRequest`/`actionResponse`.

## Trazabilidad U- aplicable a este controlador (verbatim, `design.md`)

| U | Origen spec | Ubicación |
|---|---|---|
| U-correos-centro-formulario-004 | RUI-correos-centro-formulario-004 | `CorreoController.reenviar` — `actionResponse.setNotify(...)` |

## Superficie cerrada

**MUST** crear únicamente `CorreoController` con exactamente los dos métodos `@CallMethod` listados (`validateReenviar`, `reenviar`). **MUST NOT** crear `AdjuntoController` ni ningún otro método/endpoint no listado. Los XML de vistas de los que depende esta tarea (`views/Correo.xml`, que invoca estas acciones) son contrato fijo: si la firma de acción de la vista no coincide con estos métodos, **detente y reporta** `BLOCKED`.
