---
type: implementation-task
template: system
---

# Tarea 20 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/controller/SmsController.java` | Crear | k-sistemas (controladores.md) | `@CallMethod` `validateReenviar` y `reenviar` |

### Paso 6 — Controlador `SmsController`

**Fichero:** `subsystem/sms/controller/SmsController.java` (Crear). Un controlador por entidad; **MUST NOT** exponer `insert`/`update`/`remove` ni `validateSave`/`validateDelete` (los dan el endpoint REST automático y las acciones globales `remote-validation*`).

```java
// Clase: com.educaflow.subsystem.sms.controller.SmsController
//   @Inject private ModelServiceFactory modelServiceFactory;   (única inyección)

@CallMethod
public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse);

@CallMethod
@Transactional   // el reenvío programa la tarea sobre la transacción actual: su commit es lo que la dispara
public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse);
```

Los dos métodos resuelven el servicio con `modelServiceFactory.resolve(Sms.class)` y no tienen lógica de negocio
propia. El nombre de cada uno **MUST** coincidir con el `{nombreFuncionJava}` de su acción y con el método del
servicio, y los parámetros se llaman **siempre** `actionRequest` y `actionResponse`.

- **`validateReenviar`** — invocado por `subsysSms.Main@Sms-Remote-validateReenviar-action` (`Main-Sms.xml`),
  antes de reenviar. Obtiene `entidadOriginal = actionRequestHelper.getOriginalModel()` y
  `entidad = actionRequestHelper.getModel(smsService.allowPropertiesReenviar())` —la **misma** whitelist que usa
  la operación, **pedida** al servicio y **MUST NOT** construida inline
  (`k-sistemas/controladores.md` §«Parámetros, AllowProperties y respuesta»): la ruta de validación y la de
  ejecución tienen que ver exactamente los mismos campos, así que quien mañana añada uno a
  `allowPropertiesReenviar()` lo verá también en el validador; con la whitelist vacía de hoy el comportamiento no
  cambia—. Llama a `smsService.validateReenviar(entidad, entidadOriginal)` y, si hay mensajes, los entrega con
  `actionResponseHelper.doResponseBusinessMessagesAsError(...)`.
  Pasa `entidadOriginal` tal cual; **MUST NOT** añadir guarda (la tiene el servicio, paso 5).
- **`reenviar`** — invocado por `subsysSms.Main@Sms-Remote-reenviar-action` (`Main-Sms.xml` y `Centro-Sms.xml`).
  Obtiene `entidadOriginal` y `entidad` igual que el anterior (whitelist **pedida** al servicio, **MUST NOT**
  construirse inline), llama a `smsService.reenviar(entidad, entidadOriginal)` y responde con
  `actionResponse.setNotify(I18n.get("El reenvío del SMS se ha puesto en marcha."))` y
  `actionResponse.setSignal("refresh-tab", null)`, que recarga la pestaña para ver los reintentos.
  Lleva `@Transactional` porque el reenvío programa la tarea sobre la transacción actual: sin él,
  `EjecutorAsincrono.ejecutarTrasCommit` falla con `IllegalStateException`, que es su tercera entrada declarada
  (paso 3). **MUST NOT** comprobar roles aquí: V-Sms-015 vive en el servicio.

**Verificar:** `./gradlew -q compileJava`; `grep -n "actionRequest\|actionResponse" …/SmsController.java` confirma que los parámetros se llaman así; `grep -n "createAllowProperties" …/SmsController.java` no devuelve nada; `grep -c "getModel(smsService.allowPropertiesReenviar())" …/SmsController.java` devuelve **2** (la whitelist es la misma en la ruta de validación y en la de ejecución).



### `SmsServiceImpl.reenviar` (invocado desde `SmsController.reenviar`)

Entidad: `Sms`. **Forma elegida**: `createAllowProperties` con un mapa **vacío**.
**Origen spec:** acción `Reenviar` de `entity-Sms.md` (no declara ninguna `Input AllowProperties`).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| *(ninguno)* | — | **NO** | El reenvío no acepta ningún dato del cliente: solo el `id`, que `ActionRequestHelper` resuelve al margen de la whitelist. Todo lo que se valida (estado FALLIDO y centro del usuario) se lee de `entidadOriginal`, es decir de BD, no del JSON. |



### Validaciones (`V-Sms-NNN`)

| V | Origen spec | Ubicación |
|---|-------------|-----------|
| V-Sms-014 | VAL-Sms-012 | `SmsServiceImpl.validateReenviar` (estado != FALLIDO), invocada por `SmsController.validateReenviar` desde `subsysSms.Main@Sms-Remote-validateReenviar-action` |
| V-Sms-015 | VAL-Sms-013 | `SmsServiceImpl.validateReenviar` (administrador, o SUPERVISOR/ADMINISTRATIVO del centro del SMS según `User.tieneTipoUsuario`) |
