---
type: implementation-task
template: system
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

**Alcance de esta tarea:** dos filas que son la misma pieza lógica (el controlador de la ventana sustituye al de `ContextoTramitacion`):

1. **Crear** `NuevoExpedienteController`: Java, se materializa delegando en `developer-code-implementer` (`implementation.md` §2). **Superficie cerrada:** solo los cinco métodos `@CallMethod` que el diseño lista. Las firmas **MUST** coincidir con las acciones `Remote-*` de la vista `Main-NuevoExpediente.xml` (tarea 13) y con `NuevoExpedienteService` (tarea 09), ya en el árbol.
2. **Borrar** `src/main/java/com/educaflow/subsystem/expedientes/controllers/ContextoTramitacionController.java` (eliminación declarada; la hace el implementador directamente con `rm`/`git rm`, sin código que escribir). **MUST NOT** borrar nada más.

Con esta tarea, junto con las 01-11, se cierra el punto en el que «todo el código compila» (Paso 6): la compilación global la verifica el motor al final.

**Del diseño — filas de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/controllers/ContextoTramitacionController.java` | Borrar | — | Sustituido por `NuevoExpedienteController` (Paso 6) |
| `src/main/java/com/educaflow/system/expedientes/controller/NuevoExpedienteController.java` | Crear | k-sistemas (controladores.md) | Controlador de la ventana (Paso 6) |

**Del diseño — Paso 6, controladores (verbatim; el delta de `ExpedienteController` es de la tarea 11)**

### Paso 6 — Controladores

**Borrar** `subsystem/expedientes/controllers/ContextoTramitacionController.java`.

**Crear** el controlador del sistema. Solo devuelve valores; **MUST NOT** fijar ningún atributo de vista (`hidden`, `readonly`, `domain`, `title`…): eso lo decide la vista (guías).

```java
// Clase: com.educaflow.system.expedientes.controller.NuevoExpedienteController
public class NuevoExpedienteController {

    // @Inject ModelServiceFactory modelServiceFactory;   única inyección.
    // Todos los métodos: servicio = (NuevoExpedienteService) modelServiceFactory.resolve(NuevoExpediente.class);
    // modelo = new ActionRequestHelper<>(actionRequest, NuevoExpediente.class).getModel(servicio.allowProperties<Acción>()).

    @CallMethod
    public void validatePreparar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delegación: servicio.validatePreparar(modelo); si hay mensajes →
    //   actionResponseHelper.doResponseBusinessMessagesAsError(mensajes) (sin título), como exige k-sistemas
    //   (controladores.md, MUST NOT usar actionResponse.setError(String) para errores de negocio). D7: se acepta el
    //   único cambio visual — el mensaje de ESC-006 sale como un elemento de lista (<ul><li>, con viñeta) en vez de
    //   en texto plano como hoy; el texto es el mismo. Acción de la vista: Remote-validatePreparar (al abrirse; ESC-006).

    @CallMethod
    public void preparar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delegación: resultado = servicio.preparar(modelo); responde con setValue de nombreTramite, ayudaTramite,
    //   centrosDisponibles (cada centro como mapa {id, name}, en el orden devuelto) y hayUnSoloCentroDisponible.

    @CallMethod
    public void recalcular(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delegación: servicio.recalcular(modelo); setValue de hayQuePreguntarPresentacion,
    //   presentadoEnPapelDeducido, presentadoEnPapelVigente, sePuedeCrearParaMi, sePuedeCrearEnRepresentacion y
    //   hayQuePreguntarParaQuien.

    @CallMethod
    public void validateCrear(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delegación: servicio.validateCrear(modelo); si hay mensajes →
    //   actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get(<título de RUI-019>), mensajes)
    //   (U-nuevo-expediente-019: un aviso de error con ese título y un mensaje por línea).

    @CallMethod
    @Transactional
    public void crear(ActionRequest actionRequest, ActionResponse actionResponse);
    //   La transacción abarca también la resolución de la vista: si esta falla, el alta se deshace.
    //   Delegación: vista = servicio.crear(modelo); actionResponseHelper.doResponseViewForm(vista.viewName(),
    //   vista.modelClass(), vista.expediente(), vista.title(), vista.profile().name()); actionResponse.setCanClose(true)
    //   (cierra la ventana y abre el expediente en su primer estado, como hoy).
    //   Manejo de errores (Notas §11). Hoy NUNCA llega aquí una BusinessException:
    //   - Si falla la validación de la puerta (alta que se salta validateCrear, o algo cambió entre validateCrear
    //     y crear), ExpedienteService.triggerInitialEvent lanza jakarta.validation.ValidationException (unchecked:
    //     es lo que lanza BusinessMessages.throwIfInvalid de AOP), que se propaga SIN envolver y Axelor la muestra
    //     como error de validación.
    //   - La BusinessException del InitialEventManager de cada tipo no llega como tal: Tramitador.triggerInitialEvent
    //     (Paso 2, sin cambios en su try/catch) envuelve cualquier Exception salvo UnauthorizedException en
    //     RuntimeException.
    //   - UnauthorizedException se propaga sin envolver para que Axelor responda un error de acceso.
    //   La BusinessException de la firma de servicio.crear solo existe porque la declara Tramitador. El controlador
    //   solo satisface el checked relanzándola envuelta en RuntimeException (MUST NOT capturarla para extraer sus
    //   mensajes, k-sistemas/controladores.md). MUST NOT capturar UnauthorizedException ni ValidationException.
}
```

Verificación: `./gradlew compileJava` (primer punto en el que todo el código compila); `grep -rn "ContextoTramitacionController\|ContextoTramitacionService\|prepararContextoTramitacion" src/main` sin resultados.

**Del diseño — Frontera de confianza — AllowProperties por acción (verbatim)**

## Frontera de confianza — AllowProperties por acción

### `NuevoExpedienteServiceImpl.preparar` y `validatePreparar` (invocados desde `NuevoExpedienteController.preparar` y `.validatePreparar`)

Entidad: `NuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Preparar` de `entity-NuevoExpediente.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `tramite` | cliente | sí (solo `id`) | Input de la acción; lo fija la vista con el trámite elegido en «Trámites» (U-014). |
| `centro`, `presentadoEnPapel`, `presentadoEnRepresentacion` | cliente | **NO** | No son entrada de `Preparar`. |
| `nombreTramite`, `ayudaTramite` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_AsignarDatosTramite` (R-NuevoExpediente-002). |
| `centrosDisponibles`, `hayUnSoloCentroDisponible` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_AsignarCentrosDisponibles` (R-NuevoExpediente-003). |
| `hayQuePreguntarPresentacion`, `presentadoEnPapelDeducido`, `presentadoEnPapelVigente`, `sePuedeCrearParaMi`, `sePuedeCrearEnRepresentacion`, `hayQuePreguntarParaQuien` | servidor | **NO** | No los usa `Preparar`; los calcula `Recalcular`. |

### `NuevoExpedienteServiceImpl.recalcular` (invocado desde `NuevoExpedienteController.recalcular`)

Entidad: `NuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Recalcular` de `entity-NuevoExpediente.md` (trámite, centro, presentado en papel).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `tramite` | cliente | sí (solo `id`) | Input de `Recalcular`. |
| `centro` | cliente | sí (solo `id`) | Input de `Recalcular`; no se confía en él: solo sirve para calcular, y la puerta del motor lo vuelve a validar al crear. |
| `presentadoEnPapel` | cliente | sí | Input de `Recalcular`; cuando no hay forma deducida, de él sale el perfil con el que se pregunta a la puerta del motor (CC-005/006). Cuando la hay, se ignora y manda la deducida (`getFormaPresentarVigente`, que usan R-NuevoExpediente-004/005). |
| `presentadoEnRepresentacion` | cliente | **NO** | No es entrada de `Recalcular`. |
| `hayQuePreguntarPresentacion`, `presentadoEnPapelDeducido`, `presentadoEnPapelVigente` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_AsignarFormaDePresentar` (R-NuevoExpediente-004). |
| `sePuedeCrearParaMi`, `sePuedeCrearEnRepresentacion`, `hayQuePreguntarParaQuien` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_AsignarOpcionesParaQuien` (R-NuevoExpediente-005). |
| `nombreTramite`, `ayudaTramite`, `centrosDisponibles`, `hayUnSoloCentroDisponible` | servidor | **NO** | No los toca esta acción. |

### `NuevoExpedienteServiceImpl.crear` y `validateCrear` (invocados desde `NuevoExpedienteController.crear` y `.validateCrear`)

Entidad: `NuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-NuevoExpediente.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `tramite` | cliente | sí (solo `id`) | Input de `Crear`; lo revalida la puerta del motor (V-ContextoTramitacion-002). |
| `centro` | cliente | sí (solo `id`) | Input de `Crear`; lo revalida la puerta del motor (V-ContextoTramitacion-001/002). |
| `presentadoEnPapel` | cliente | sí | Input de `Crear`; solo sirve para deducir el perfil en `toContextoTramitacion` (R-NuevoExpediente-001), que la puerta revalida (V-ContextoTramitacion-003). |
| `presentadoEnRepresentacion` | cliente | sí | Input de `Crear`; lo revalida la puerta del motor (V-ContextoTramitacion-004/005). |
| perfil | servidor | **NO existe en el modelo** | Lo decide `toContextoTramitacion` a partir de la forma de presentar; la ventana no tiene campo por el que enviarlo (RN-NuevoExpediente-001). |
| Campos calculados (CC-001…007), `hayUnSoloCentroDisponible`, `hayQuePreguntarParaQuien` | servidor | **NO** | `Crear` no los lee. |

`allowPropertiesInsert()` y `allowPropertiesUpdate()` devuelven `AllowProperties.createDenyAllProperties()` en `NuevoExpedienteServiceImpl` (Origen spec: Input AllowProperties «ninguna» de `Modificar` en `entity-NuevoExpediente.md`: el modelo no se guarda) y en `TipoExpedienteServiceImpl` (Origen spec: Input AllowProperties «ninguna» de `Crear` y `Modificar` en `entity-TipoExpediente.md`), así que el endpoint REST automático no acepta ningún campo de ninguna de las dos entidades.
No es el parche de `Expediente` que `CLAUDE.md` prohíbe reintroducir: son una entidad de pantalla y una de catálogo, fuera de la tramitación.

**Del diseño — Trazabilidad de reglas de UI (U) que se resuelven en el servidor (verbatim)**

### Reglas de UI (U) — pantalla `nuevo-expediente`

Todas en `views/Main-NuevoExpediente.xml`; los nombres de acción omiten el prefijo `sysExpedientes.Main@NuevoExpediente-`.

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-019 | RUI-nuevo-expediente-formulario-019 | `NuevoExpedienteController.validateCrear` → `ActionResponseHelper.doResponseBusinessMessagesAsError(título, mensajes)` (lista `<ul>`, un mensaje por línea); `.crear` no captura `BusinessException` (Notas §11). Único U fuera del XML: el aviso de error lo compone el servidor al devolver los mensajes. |

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Clase completa | `subsystem/expedientes/controllers/ContextoTramitacionController.java` | Objetivo del spec + guías («de él salen ContextoTramitacionController…») |

**Del diseño — Notas y supuestos aplicables (verbatim)**

11. **`NuevoExpedienteController.crear` no captura `BusinessException`** (`k-sistemas/controladores.md`, MUST NOT): la relanza envuelta en `RuntimeException`.
    Hoy `ExpedienteController.triggerInitialEvent` la captura y la pinta con el título de RUI-019, pero esa captura ya es código muerto para los errores del evento inicial: `Tramitador.triggerInitialEvent` envuelve cualquier `Exception` (incluida la `BusinessException` del `InitialEventManager`) en `RuntimeException`, salvo `UnauthorizedException`.
    Hoy no llega al controlador ninguna `BusinessException`: la `BusinessException` de la firma de `crear` solo existe porque la declara `Tramitador`, y el controlador solo satisface el checked relanzándola envuelta.
    Si falla la validación de la puerta (un alta que se salta `validateCrear`), `ExpedienteService.triggerInitialEvent` lanza `jakarta.validation.ValidationException` (unchecked, la que lanza `BusinessMessages.throwIfInvalid` de AOP), que se propaga sin envolver y Axelor muestra como error de validación; en la ventana ese caso no se da porque `validateCrear` ya enseña antes los mismos mensajes con el título de RUI-019.
    El controlador **MUST NOT** capturar `UnauthorizedException` ni `ValidationException`.
    Que los errores de negocio del evento inicial salieran con el aviso de RUI-019 exigiría que `Tramitador` dejara pasar `BusinessException` sin envolverla: es un cambio del motor que no forma parte del alcance y habría que decidirlo aparte.
16. **Error de «Nuevo expediente» sin centros (ESC-006) con viñeta — PUNTO A CONFIRMAR POR EL HUMANO (D7).** `NuevoExpedienteController.validatePreparar` responde con `actionResponseHelper.doResponseBusinessMessagesAsError(mensajes)`, como exige `k-sistemas/controladores.md` (MUST NOT `setError(String)` para errores de negocio) y piden las guías («sigue al 100 % el estándar de `k-sistemas`»).
    Coste aceptado: el mensaje «No puede crear expedientes de este trámite en ninguno de sus centros» sale como un elemento de lista con viñeta, cuando hoy sale en texto plano; el texto no cambia. Choca con la guía «el comportamiento visible MUST conservarse exactamente igual»: si el humano prefiere conservar el texto plano, la alternativa (`setError(texto)`) es una desviación declarada del MUST NOT de `k-sistemas` (ver D7).
