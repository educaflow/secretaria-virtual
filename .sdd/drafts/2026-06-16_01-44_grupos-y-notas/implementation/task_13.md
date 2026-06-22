---
type: implementation-task
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el controlador `NotaController` (botón "Guardar" de la nota).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/controller/NotaController.java` | Crear | k-sistemas (controladores.md), k-secure-coding | Botón "Guardar" de la nota |

### Descripción del diseño (Paso 4 — Controladores)

#### `com.educaflow.system.gruposnotas.controller.NotaController`

```java
public class NotaController {

    @Inject private ModelServiceFactory modelServiceFactory;

    @CallMethod @Transactional
    public void guardarNota(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delega en NotaService.guardarNota. Extrae el bean con allowPropertiesGuardarNota() (whitelist: valor)
    //   y el original con getOriginalModel(). Parámetros nombrados actionRequest/actionResponse (k-sistemas).
}
```

El controlador NO accede a repositorios ni a `JpaRepository`; solo inyecta `ModelServiceFactory` y delega en `NotaService` (C9/C10/C14).
