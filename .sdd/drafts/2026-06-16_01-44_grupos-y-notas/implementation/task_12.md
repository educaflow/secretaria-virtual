---
type: implementation-task
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el controlador `GrupoController` (botones "Cerrar grupo" y "Reabrir grupo").

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/controller/GrupoController.java` | Crear | k-sistemas (controladores.md), k-secure-coding | Botones "Cerrar grupo" y "Reabrir grupo" |

### Descripción del diseño (Paso 4 — Controladores)

#### `com.educaflow.system.gruposnotas.controller.GrupoController`

```java
public class GrupoController {

    @Inject private ModelServiceFactory modelServiceFactory;

    @CallMethod @Transactional
    public void cerrar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delega en GrupoService.cerrar. Extrae el bean con allowPropertiesCerrar() (denyAll) y el original.
    //   Patrón TareaFirmaController: getOriginalModel() + getModel(allowProperties).

    @CallMethod @Transactional
    public void reabrir(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delega en GrupoService.reabrir. Extrae el bean con allowPropertiesReabrir() (denyAll) y el original.
    //   La V-Grupo-008 (solo administrador) se valida en el servicio, no solo aquí.
}
```

Parámetros nombrados `actionRequest`/`actionResponse` (k-sistemas). El controlador NO accede a repositorios ni a `JpaRepository`; solo inyecta `ModelServiceFactory` y delega en `GrupoService` (C9/C10/C14).
