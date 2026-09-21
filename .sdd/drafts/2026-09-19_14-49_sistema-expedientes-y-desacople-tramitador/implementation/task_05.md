---
type: implementation-task
template: system
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-secure-coding

**Alcance de esta tarea:** fila `Modificar`. El fichero **ya existe**: **MUST** editar la clase existente aplicando **solo** el delta que el diseño declara (`triggerInitialEvent`) y **conservando** todo lo demás (personas, nombre, número, `InitialEventManager`, historial, `onEnter`, `JPA.save`, imports preexistentes; `implementation.md` §2). Es Java: se materializa delegando en `developer-code-implementer`. `k-secure-coding` se añade porque la clase toca entidades.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/core/Tramitador.java` | Modificar | k-code-quality | Lee el contexto nuevo y deduce el papel del perfil (Paso 2) |

**Del diseño — Paso 2, delta de `Tramitador` (verbatim)**

**Modificar** `tramitacion/core/Tramitador.java` (solo el delta; el resto de la clase se conserva):

```java
// Clase: com.educaflow.subsystem.expedientes.tramitacion.core.Tramitador
public Expediente triggerInitialEvent(ContextoTramitacion contextoTramitacion) throws BusinessException;
//   Misma firma; cambia el import al record de tramitacion.eventmanager y la lectura del contexto:
//     - tipoExpediente = contextoTramitacion.tramite().getDefaultTipoExpediente()
//     - centro = Objects.requireNonNull(contextoTramitacion.centro(), <mensaje: el contexto debe llegar validado por ExpedienteService>)
//     - presentadoEnPapel = contextoTramitacion.isPresentadoEnPapel()        ← R-ContextoTramitacion-001
//     - presentadoEnRepresentacion = Objects.requireNonNull(contextoTramitacion.presentadoEnRepresentacion(), <mismo mensaje>)
//     - el EventContext del alta se construye con contextoTramitacion.profile()
//   Precondición: solo lo invoca ExpedienteService.triggerInitialEvent, que ya ha validado el contexto
//   (V-ContextoTramitacion-001…005). Un centro o una representación null aquí es un error de programación:
//   los requireNonNull llevan un mensaje que lo dice, en vez de desempaquetar un null sin explicación.
//   Todo lo demás (personas, nombre, número, InitialEventManager, historial, onEnter, JPA.save) se conserva.
```

Verificación: `grep -rn "db.ContextoTramitacion" src/main/java/com/educaflow/subsystem/expedientes/tramitacion` sin resultados (compilación en el Paso 6).

**Del diseño — Trazabilidad de reglas de negocio (R) (verbatim)**

### Reglas de negocio (R)

| R | Origen spec | Ubicación |
|---|---|---|
| R-ContextoTramitacion-001 | RN-ContextoTramitacion-001 | `ContextoTramitacion.isPresentadoEnPapel` (equivalencia en `FormaPresentacion.fromProfile`), aplicado en `Tramitador.triggerInitialEvent` (Antes de `JPA.save`) |
