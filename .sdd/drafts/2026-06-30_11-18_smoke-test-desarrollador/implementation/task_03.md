---
type: implementation-task
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Esta tarea implementa el controlador `SmokeTestController`. Es código Java: se materializa a partir de las firmas y comentarios de este diseño.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/controller/SmokeTestController.java` | Crear | k-sistemas (controladores.md) | `validateSave` (pre-valida antes del `save`) |

> Raíz de los ficheros del subsistema: `src/main/java/com/educaflow/subsystem/smoketest/`.

### Paso 4 — Controlador `SmokeTestController`

`com.educaflow.subsystem.smoketest.controller.SmokeTestController` (un controlador por entidad). Inyecta `@Inject private ModelServiceFactory modelServiceFactory;`.

```java
@CallMethod
public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);
//   Pre-validación del guardado para mostrar el error de negocio como modal ANTES de la acción `save`.
//   - Resuelve el servicio: (SmokeTestService) modelServiceFactory.resolve(SmokeTest.class).
//   - ActionRequestHelper<SmokeTest> sobre actionRequest; ActionResponseHelper sobre actionResponse.
//   - original = actionRequestHelper.getOriginalModel().
//   - Si actionRequestHelper.getId()==null  (alta):
//       smokeTest = actionRequestHelper.getModel(smokeTestService.allowPropertiesInsert());
//       validationResult = smokeTestService.validateInsert(smokeTest);
//     Si no (modificación):
//       smokeTest = actionRequestHelper.getModel(smokeTestService.allowPropertiesUpdate());
//       validationResult = smokeTestService.validateUpdate(smokeTest, original);
//   - Si validationResult.isPresent(): actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get()).
//   Sin @Transactional (solo lee y valida; no persiste). Parámetros nombrados actionRequest/actionResponse.
```

> **MUST NOT** exponer `@CallMethod` para `insert`/`update`/`remove`: el guardado y el borrado usan las acciones de framework `save` y `delete` (controladores.md). `validateSave` es solo un hook de validación previo, igual que `LeyEducativaController.validateSave`. No se necesita `validateDelete` (el borrado no tiene reglas).

Verificar: la `<action-method>` de la vista referencia exactamente `com.educaflow.subsystem.smoketest.controller.SmokeTestController#validateSave`. `./run.sh` compila.

## Frontera de confianza — AllowProperties por acción

El único `@CallMethod` del diseño es `SmokeTestController.validateSave`, que pre-valida el guardado consumiendo `allowPropertiesInsert()` (rama alta) y `allowPropertiesUpdate()` (rama modificación) del servicio. Esas mismas whitelists son la defensa del flujo de guardado genérico (`save` / `POST /ws/rest/<FQN>`), que filtra el JSON entrante con ellas antes de llegar a `insert`/`update`. Reglas aplicadas: `k-secure-coding` §3.
