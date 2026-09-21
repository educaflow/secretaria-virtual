---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-secure-coding
- k-validaciones
- k-code-quality

**Alcance de esta tarea:** fila `Modificar`. El fichero **ya existe**: **MUST** editar la clase existente aplicando **solo** el delta que el diseño declara (`crear` → `triggerInitialEvent`, `validateTriggerInitialEvent`, `getPerfilesDeInicio`, `getVistaExpediente`, campos `@Inject` nuevos, Javadoc de clase) y **conservando** todo lo demás (`triggerEvent`, `getExpediente`, `validateChild`, imports preexistentes; `implementation.md` §2). Es Java: se materializa delegando en `developer-code-implementer`. Usa `VistaExpediente` (tarea 06), `ContextoTramitacion` (03) y `FormaPresentacion` (02), ya en el árbol.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/services/ExpedienteService.java` | Modificar | k-secure-coding, k-validaciones | Puerta del motor: `crear` → `triggerInitialEvent` con validación, perfiles/centros de inicio y vista del expediente (Paso 3) |

**Del diseño — Paso 3, `ExpedienteService` (verbatim; `VistaExpediente` es de la tarea 06)**

### Paso 3 — La puerta del motor: `ExpedienteService`

**Modificar** `services/ExpedienteService.java` (solo el delta; `triggerEvent`, `getExpediente` y `validateChild` se conservan):

```java
// Clase: com.educaflow.subsystem.expedientes.services.ExpedienteService
// Campos @Inject nuevos: ExpedienteLocator expedienteLocator; ModelServiceFactory modelServiceFactory.
// PerfilesUsuarioService perfilesUsuarioService ya está inyectado (hoy sin uso): lo usa getPerfilesDeInicio.
// Javadoc de la clase: sustituir la referencia a la autorización del alta por la puerta real —
// triggerInitialEvent valida siempre, entre por donde entre la petición.

@Transactional
public Expediente triggerInitialEvent(ContextoTramitacion contextoTramitacion) throws BusinessException;
//   Renombrado de crear(ContextoTramitacion) (guías: lo que hace es disparar el evento inicial).
//   1. validateTriggerInitialEvent(contextoTramitacion).ifPresent(BusinessMessages::throwIfInvalid):
//      las validaciones de «Iniciar expediente» se cumplen aquí aunque quien llame no haya pedido antes la
//      validación (la ventana sí la pide; un alta programática o un cliente por /ws/action puede no hacerlo).
//   2. return tramitador.triggerInitialEvent(contextoTramitacion).

public Optional<BusinessMessages> validateTriggerInitialEvent(ContextoTramitacion contextoTramitacion);
//   Quien inicia es SIEMPRE SecurityUtil.getUser(), nunca un dato del cliente.
//   Acumula todos los fallos (la ventana los enseña juntos, U-nuevo-expediente-019), cada uno como
//   BusinessMessage sin nombre de campo para que salga como una línea de texto. Dos ramas independientes:
//   Rama centro/perfil (complementaria por construcción):
//     - V-ContextoTramitacion-001 (Origen spec: VAL-ContextoTramitacion-001) centro null → mensaje de
//       VAL-ContextoTramitacion-001 del spec. En esta rama no se evalúan 002/003: sin centro no hay perfiles
//       que comprobar.
//     - si hay centro, perfilesDeInicio = getPerfilesDeInicio(tramite, centro):
//       - V-ContextoTramitacion-002 (Origen spec: VAL-ContextoTramitacion-002) perfilesDeInicio vacío →
//         mensaje de VAL-ContextoTramitacion-002 (no puede crear expedientes de ese trámite en ese centro).
//       - si no, V-ContextoTramitacion-003 (Origen spec: VAL-ContextoTramitacion-003)
//         !FormaPresentacion.esPerfilDeInicio(profile) || !perfilesDeInicio.contains(profile)
//         → mensaje de VAL-ContextoTramitacion-003. El caso «perfil null» (y «no es creador ni tramitador») lo
//         rechaza explícitamente la primera condición (esPerfilDeInicio trata null como «no inicia»), evaluada
//         ANTES del contains: la rama no depende de qué implementación de Set devuelva getPerfilesDeInicio (un
//         Set.of()/Set.copyOf lanza NullPointerException con contains(null)). La segunda cubre «no es uno de los suyos».
//         Test unitario obligatorio (para test-unit-desc.md): con centro y perfilesDeInicio no vacío, profile null
//         → un único mensaje de VAL-ContextoTramitacion-003 y ninguna excepción; y lo mismo con
//         getPerfilesDeInicio devolviendo un Set inmutable (Set.of(CREADOR)).
//   Rama representación:
//     - V-ContextoTramitacion-004 (Origen spec: VAL-ContextoTramitacion-004) presentadoEnRepresentacion null →
//       mensaje de VAL-ContextoTramitacion-004.
//     - si no, V-ContextoTramitacion-005 (Origen spec: VAL-ContextoTramitacion-005) presentadoEnRepresentacion
//       true y tramite.getPermitidoPresentarEnRepresentacion() false → mensaje de VAL-ContextoTramitacion-005.
//   Mensajes: los literales de cada VAL- del spec, traducidos con I18n.get.

public Set<Profile> getPerfilesDeInicio(Tramite tramite, Centro centro);
//   Perfiles con los que el usuario autenticado (SecurityUtil.getUser()) puede iniciar expedientes del trámite
//   en el centro. Total por construcción, con dos ramas explícitas:
//     - centro null → Set.of() SIN llamar a PerfilesUsuarioService: sin centro no hay perfiles de inicio (regla
//       propia de esta función, documentada en su Javadoc; no se apoya en cómo trate el null otra pieza).
//     - centro con valor → perfilesUsuarioService.getPerfilesSobreTramite(tramite, usuario, centro) filtrados con
//       FormaPresentacion::esPerfilDeInicio.
//   Contrato del Set devuelto: nunca contiene null y NO garantiza admitir contains(null); quien consulte un
//   perfil que pueda ser null lo descarta antes (validateTriggerInitialEvent lo hace con esPerfilDeInicio).
//   Único dueño de «qué perfiles de inicio tiene este usuario aquí»: lo usan la validación y la ventana (D2).

public VistaExpediente getVistaExpediente(Expediente expediente, Profile profile);
//   Vista del expediente en su fase actual. Movido de ExpedienteController.triggerInitialEvent (que se borra); su
//   único llamador es NuevoExpedienteServiceImpl.fireActionRule_IniciarExpediente (→ NuevoExpedienteController.crear).
//   ExpedienteController.viewExpediente hace hoy una resolución equivalente y se queda como está (fuera de alcance,
//   D3, Notas §10).
//   EventContext(expediente, profile, modelServiceFactory); phaseEventManager =
//   expedienteLocator.getPhaseEventManager(expediente.getTipoExpediente(), expediente.getCodePhase());
//   viewName = phaseEventManager.getViewName(expediente, eventContext); devuelve
//   VistaExpediente(viewName, phaseEventManager.getModelClass(), expediente, profile).
```

El método `crear(ContextoTramitacion)` desaparece (renombrado); no queda ningún llamador.

Verificación: `grep -rn "expedienteService.crear\|db.ContextoTramitacion" src/main/java/com/educaflow/subsystem/expedientes/services` sin resultados (compilación en el Paso 6).

**Del diseño — Trazabilidad de validaciones (V) (verbatim)**

### Validaciones (V)

| V | Origen spec | Ubicación |
|---|---|---|
| V-ContextoTramitacion-001 | VAL-ContextoTramitacion-001 | `ExpedienteService.validateTriggerInitialEvent` (rama centro) |
| V-ContextoTramitacion-002 | VAL-ContextoTramitacion-002 | `ExpedienteService.validateTriggerInitialEvent` (rama centro, con centro) |
| V-ContextoTramitacion-003 | VAL-ContextoTramitacion-003 | `ExpedienteService.validateTriggerInitialEvent` (rama centro, con perfiles de inicio) |
| V-ContextoTramitacion-004 | VAL-ContextoTramitacion-004 | `ExpedienteService.validateTriggerInitialEvent` (rama representación) |
| V-ContextoTramitacion-005 | VAL-ContextoTramitacion-005 | `ExpedienteService.validateTriggerInitialEvent` (rama representación, con respuesta) |

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Método `crear(ContextoTramitacion)` (renombrado a `triggerInitialEvent`) | `subsystem/expedientes/services/ExpedienteService.java` | Guías («`ExpedienteService.crear` se renombra a `triggerInitialEvent`») |

**Del diseño — Notas y supuestos aplicables (verbatim)**

1. **Toques al motor (`subsystem/expedientes`).** El diseño toca el motor solo donde el spec y las guías lo piden (sacar la ventana, validar en la puerta, reducir el contexto) y en `TipoExpedienteService` (D5).
   Ninguno añade una capacidad nueva (ni punto de extensión, ni tipo de acción, ni capacidad de la máquina de estados): `ContextoTramitacion` sustituye al modelo anterior y `FormaPresentacion` a la equivalencia perfil ↔ papel que hoy está repartida; los métodos nuevos de `ExpedienteService` quedan en `validateTriggerInitialEvent`, `getPerfilesDeInicio` y `getVistaExpediente` (la validación de la puerta que piden las guías y lógica movida desde las clases que salen; lo que solo necesita la ventana, como la lista de centros, vive en el sistema), y `TipoExpedienteService` protege un dato del propio motor.
   Aun así, `TipoExpedienteService` crea un paquete `service/` (singular) en el motor junto al `services/` (plural): el humano debería confirmarlo antes de implementar (el `CLAUDE.md` del motor pide consultar cualquier ampliación).
10. **Deuda temporal deliberada: resolución de la vista y fórmula del título repetidas (D3).** `ExpedienteService.getVistaExpediente` / `VistaExpediente.title()` recogen solo lo que hacía `ExpedienteController.triggerInitialEvent` (que se borra) y solo los usa el alta desde «Nuevo expediente». `ExpedienteController.viewExpediente`, `triggerEvent` y `getTabName` se quedan **exactamente** como están, así que la fórmula del título (número + «-» + nombre traducido del tipo) vive a la vez en `VistaExpediente.title()` y en `getTabName`, y `viewExpediente` hace una resolución de vista equivalente a `getVistaExpediente`.
    Es **deliberado y no es un olor a corregir en esta iniciativa**: el spec deja fuera de alcance, de forma explícita, reorganizar las piezas del tramitador que atienden a las pantallas del expediente ya creado, y las guías piden tocar en `subsystem/expedientes` solo lo imprescindible. Unificar las dos copias (que `viewExpediente`/`triggerEvent` pinten con `VistaExpediente` y desaparezca `getTabName`) queda para la iniciativa de renombrado del subsistema a `tramitador`.
