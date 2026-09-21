---
type: implementation-task
template: system
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

**Alcance de esta tarea:** fila `Modificar`. El fichero **ya existe**: **MUST** editar la clase existente aplicando **solo** el delta que el diseño declara (borrar `triggerInitialEvent(ActionRequest, ActionResponse)` y los imports que queden sin uso solo por ello) y **conservando** todo lo demás: **MUST NOT** tocar `viewExpediente`, `triggerEvent` ni `getTabName` (`implementation.md` §2). Es Java: se materializa delegando en `developer-code-implementer`. El controlador nuevo y el borrado de `ContextoTramitacionController` son de la tarea 12.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/controllers/ExpedienteController.java` | Modificar | k-sistemas (controladores.md) | Solo se borra `triggerInitialEvent` (pasa al sistema); `viewExpediente`, `triggerEvent` y `getTabName` se quedan exactamente como están (fuera de alcance del spec, D3) (Paso 6) |

**Del diseño — Paso 6, delta de `ExpedienteController` (verbatim; `NuevoExpedienteController` y el borrado de `ContextoTramitacionController` son de la tarea 12)**

### Paso 6 — Controladores

**Modificar** `subsystem/expedientes/controllers/ExpedienteController.java` (solo el delta; el resto de la clase se conserva):
- **Borrar** el método `triggerInitialEvent(ActionRequest, ActionResponse)`: la creación la atiende ahora `NuevoExpedienteController.crear`, y el motor ya no lee ninguna ventana.
- **MUST NOT** tocarse `viewExpediente`, `triggerEvent` ni `getTabName`: atienden a las pantallas del expediente ya creado, que el spec deja fuera de alcance («se quedan como están») y las guías piden tocar solo lo imprescindible del motor. Siguen construyendo su vista y su título como hoy (D3; la fórmula del título queda repetida con `VistaExpediente.title()` como deuda deliberada, Notas §10).
- Quitar el import de `com.educaflow.subsystem.expedientes.db.ContextoTramitacion`, que queda sin uso, y cualquier otro que quede sin uso **solo** por borrar `triggerInitialEvent`.

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Método `triggerInitialEvent(ActionRequest, ActionResponse)` | `subsystem/expedientes/controllers/ExpedienteController.java` | Objetivo del spec: el tramitador deja de depender de la ventana |

**Del diseño — Notas y supuestos aplicables (verbatim)**

10. **Deuda temporal deliberada: resolución de la vista y fórmula del título repetidas (D3).** `ExpedienteService.getVistaExpediente` / `VistaExpediente.title()` recogen solo lo que hacía `ExpedienteController.triggerInitialEvent` (que se borra) y solo los usa el alta desde «Nuevo expediente». `ExpedienteController.viewExpediente`, `triggerEvent` y `getTabName` se quedan **exactamente** como están, así que la fórmula del título (número + «-» + nombre traducido del tipo) vive a la vez en `VistaExpediente.title()` y en `getTabName`, y `viewExpediente` hace una resolución de vista equivalente a `getVistaExpediente`.
    Es **deliberado y no es un olor a corregir en esta iniciativa**: el spec deja fuera de alcance, de forma explícita, reorganizar las piezas del tramitador que atienden a las pantallas del expediente ya creado, y las guías piden tocar en `subsystem/expedientes` solo lo imprescindible. Unificar las dos copias (que `viewExpediente`/`triggerEvent` pinten con `VistaExpediente` y desaparezca `getTabName`) queda para la iniciativa de renombrado del subsistema a `tramitador`.
