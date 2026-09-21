---
type: implementation-task
template: system
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

**Alcance de esta tarea:** fila `Modificar`. El fichero **ya existe**: **MUST** editar la clase existente aplicando **solo** el delta que el diseño declara (abajo) y **conservando** todo lo demás (métodos, campos, imports preexistentes; `implementation.md` §2). Es Java: se materializa delegando en `developer-code-implementer`. El record `ContextoTramitacion` (mismo paquete) ya lo creó la tarea 03.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/eventmanager/InitialEventContext.java` | Modificar | k-code-quality | Usa el nuevo `ContextoTramitacion` (mismo paquete) (Paso 2) |

**Del diseño — Paso 2, delta de `InitialEventContext` (verbatim)**

**Modificar** `tramitacion/eventmanager/InitialEventContext.java` (solo el delta; el resto de la clase se conserva):
- Quitar el import de `com.educaflow.subsystem.expedientes.db.ContextoTramitacion`: el record está en el mismo paquete.
- Javadoc de la clase: el contexto dice en qué centro, con qué perfil (del que se deduce si es en papel) y si se presenta en representación.
