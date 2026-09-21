---
type: implementation-task
template: system
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

**Alcance de esta tarea:** crear el `record` `VistaExpediente` (fila `Crear`). Es Java: se materializa delegando en `developer-code-implementer` (`implementation.md` §2). **Superficie cerrada:** el record y su método calculado `title()`; el resto del «Paso 3» (`ExpedienteService`) lo cubre la tarea 07.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/services/VistaExpediente.java` | Crear | k-code-quality | `record` con lo necesario para abrir el formulario de un expediente (Paso 3) |

**Del diseño — Paso 3, `VistaExpediente` (verbatim; la parte `ExpedienteService` es de la tarea 07)**

### Paso 3 — La puerta del motor: `ExpedienteService`

**Crear** el record `com.educaflow.subsystem.expedientes.services.VistaExpediente`:

```java
// Clase: com.educaflow.subsystem.expedientes.services.VistaExpediente
public record VistaExpediente(String viewName, Class<? extends Expediente> modelClass, Expediente expediente, Profile profile) {

    public String title();
    //   Método calculado, no componente: título de la pestaña del expediente = número de expediente + "-" + nombre
    //   traducido (I18n.get) del tipo de expediente. Es la fórmula que hoy usaba ExpedienteController.triggerInitialEvent
    //   (que se borra) a través de getTabName.
    //   DEUDA TEMPORAL DELIBERADA (D3, Notas §10): ExpedienteController.getTabName se conserva con la misma fórmula,
    //   porque viewExpediente y triggerEvent están fuera del alcance del spec; las dos copias se unifican en la
    //   iniciativa de renombrado a tramitador. No es un olor a corregir en esta iniciativa.
}
//   Lo necesario para abrir el formulario de un expediente recién creado con un perfil; quien lo pinta
//   (NuevoExpedienteController.crear) se lo pasa tal cual a
//   ActionResponseHelper.doResponseViewForm(viewName, modelClass, expediente, title(), profile.name()) (D3).
```

**Del diseño — Notas y supuestos aplicables (verbatim)**

10. **Deuda temporal deliberada: resolución de la vista y fórmula del título repetidas (D3).** `ExpedienteService.getVistaExpediente` / `VistaExpediente.title()` recogen solo lo que hacía `ExpedienteController.triggerInitialEvent` (que se borra) y solo los usa el alta desde «Nuevo expediente». `ExpedienteController.viewExpediente`, `triggerEvent` y `getTabName` se quedan **exactamente** como están, así que la fórmula del título (número + «-» + nombre traducido del tipo) vive a la vez en `VistaExpediente.title()` y en `getTabName`, y `viewExpediente` hace una resolución de vista equivalente a `getVistaExpediente`.
    Es **deliberado y no es un olor a corregir en esta iniciativa**: el spec deja fuera de alcance, de forma explícita, reorganizar las piezas del tramitador que atienden a las pantallas del expediente ya creado, y las guías piden tocar en `subsystem/expedientes` solo lo imprescindible. Unificar las dos copias (que `viewExpediente`/`triggerEvent` pinten con `VistaExpediente` y desaparezca `getTabName`) queda para la iniciativa de renombrado del subsistema a `tramitador`.
