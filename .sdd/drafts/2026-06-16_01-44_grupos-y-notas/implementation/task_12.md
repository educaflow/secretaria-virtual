---
type: implementation-task
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el controlador de `Grupo` con los botones "Cerrar grupo" / "Reabrir grupo".

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/controller/GrupoController.java` | Crear | k-sistemas (controladores.md) | Botones "Cerrar grupo" / "Reabrir grupo" |

Del diseño, Paso 4 — Controladores. Un único controlador para los botones de `Grupo` (Cerrar / Reabrir). Las notas y alumnos se guardan con `save-modal` (endpoint REST), sin controlador.

```java
// Clase: com.educaflow.system.gruposnotas.controller.GrupoController
// @Inject private ModelServiceFactory modelServiceFactory;

@CallMethod @Transactional
public void cerrarGrupo(ActionRequest actionRequest, ActionResponse actionResponse);
//   Resuelve GrupoService con modelServiceFactory.resolve(Grupo.class).
//   getOriginalModel() (estado actual) y getModel(grupoService.allowPropertiesCerrarGrupo()).
//   Llama grupoService.validateCerrarGrupo(grupo, original); si hay errores,
//   actionResponseHelper.doResponseBusinessMessagesAsError(...). Si no, grupoService.cerrarGrupo(grupo, original)
//   y actionResponse.setSignal("refresh", null) / reload para reflejar estado y fechaCierre.

@CallMethod @Transactional
public void reabrirGrupo(ActionRequest actionRequest, ActionResponse actionResponse);
//   Análogo con validateReabrirGrupo + reabrirGrupo. La validación de rol ADMINISTRADOR (V-Grupo-008) vive
//   en el SERVICIO (no en el controlador), para proteger también el endpoint REST.
```

> Parámetros **MUST** llamarse `actionRequest`/`actionResponse`. El controlador no contiene lógica de negocio ni comprobaciones de rol (van en el servicio).
