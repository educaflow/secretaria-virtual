# R-Correo-002 — Reintento asíncrono del envío de un correo fallido

**Entidad:** Correo
**Origen spec:** RN-Correo-002
**Operación:** reenviar
**Momento:** Después de repository.save
**Servicio host:** com.educaflow.subsystem.correos.service.impl.CorreoServiceImpl
**Método host:** fireActionRule_ProgramarReenvioAsincrono(Correo correo)

## Análisis de la regla

Esta regla es la **misma mecánica de envío asíncrono** que `R-Correo-001` (mismas clases: `CorreoAsyncExecutor`, `PostCommitRunner`, mismo método compartido `CorreoServiceImpl.enviarCorreo(Long correoId)`), disparada desde un punto distinto: la acción `reenviar` en vez del alta. Se describe aquí solo lo que **cambia** respecto a `R-Correo-001`; para el mecanismo común (por qué hace falta `PostCommitRunner`, por qué `JPA.runInTransaction`, cómo se construye el `Mail`, cómo se trata el error) ver `design/rules/R-Correo-001.md`.

Qué se dispara y cuándo: cuando el Administrador (cualquier centro) o el Supervisor (su propio centro) pulsa «Reenviar» sobre un `Correo` en estado `FAIL`. La acción `reenviar` valida (`VAL-Correo-009`, `VAL-Correo-010`) y, si es válida, programa el mismo mecanismo de envío asíncrono que el alta — **la función `enviarCorreo(Long correoId)` es idéntica y ya incrementa `numeroReintentos`, actualiza `fechaUltimoIntentoEnvio` y resuelve `SUCCESS`/`FAIL`** exactamente igual que en el primer intento (design-guidelines: "la misma operación sirve para envío inicial y reintento").

Diferencia clave con `R-Correo-001`: `reenviar(Correo entidad, Correo entidadOriginal)` **no modifica ningún campo de forma síncrona** — todo el cambio de estado (incremento de `numeroReintentos`, fechas, `estado`, `descripcionUltimoFallo`) ocurre dentro de `enviarCorreo`, en el hilo del executor, igual que en el alta. Por eso `reenviar` no necesita llamar a `repository.save` con cambios propios: valida y programa el envío, sin más. (Ver "Notas y supuestos" del `design.md` — es una excepción documentada al patrón general "toda acción persiste con `repository.save`", justificada porque no hay ningún campo que esta acción cambie por sí misma antes de programar el envío).

Qué información lee: el `Correo` completo (para reconstruir el mismo `Mail` que en el primer intento) y su estado/centro (para las validaciones).

Garantías de transaccionalidad/idempotencia: iguales a `R-Correo-001`. Además, como la validación (`VAL-Correo-009`) exige que el `Correo` esté en `FAIL`, y `FAIL` no es terminal, no hace falta ninguna guarda extra de idempotencia distinta de la ya presente en `enviarCorreo` (`estado == SUCCESS → return`).

Qué errores puede encontrar: los mismos que `R-Correo-001` (fallo de `MailSender.send`, con el mismo tratamiento).

## Diseño detallado

### Clases nuevas / Interfaces / Tipos propios

Ninguno adicional — reutiliza íntegramente `CorreoAsyncExecutor`, `PostCommitRunner` y `CorreoServiceImpl.enviarCorreo(Long correoId)` ya descritos en `design/rules/R-Correo-001.md`.

### Diagrama de secuencia

```
CorreoController.reenviar(actionRequest, actionResponse)
  ├─ correoService = modelServiceFactory.resolve(Correo.class)
  ├─ entidadOriginal = actionRequestHelper.getOriginalModel()
  ├─ entidad = actionRequestHelper.getModel(correoService.allowPropertiesReenviar())   → whitelist vacía
  └─ correoService.reenviar(entidad, entidadOriginal)

CorreoServiceImpl.reenviar(entidad, entidadOriginal)
  ├─ validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid)
  │     → V-Correo-017 (VAL-Correo-009: estado == FAIL)
  │     → V-Correo-018 (VAL-Correo-010: permiso sobre el centro, salvo Administrador)
  ├─ fireActionRule_ProgramarReenvioAsincrono(entidadOriginal)
  │     └─ PostCommitRunner.runAfterCommit(() -> correoAsyncExecutor.submit(() -> this.enviarCorreo(correoId)))
  └─ return entidadOriginal   (sin cambios síncronos; el cambio real llega más tarde, vía enviarCorreo)

……tiempo después, la transacción del @CallMethod hace COMMIT……

CorreoServiceImpl.enviarCorreo(correoId)   → idéntico al de R-Correo-001 (incrementa numeroReintentos,
                                              actualiza fechaUltimoIntentoEnvio, resuelve SUCCESS/FAIL)
```

### Errores

Idéntica tabla que `R-Correo-001` (mismo método `enviarCorreo` subyacente). Adicionalmente:

| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| Se pulsa «Reenviar» sobre un correo que no está en `FAIL` (p.ej. doble clic mientras el primer reenvío ya está en curso) | `CorreoServiceImpl.validateReenviar` | Rechazado con el mensaje de `VAL-Correo-009` ("Solo se pueden reenviar correos que han fallado"); no se programa un segundo envío |
| Un Supervisor intenta reenviar un correo de otro centro (Vía B / IDOR) | `CorreoServiceImpl.validateReenviar` | Rechazado con el mensaje de `VAL-Correo-010` |

### Contenido del método `fireActionRule_*`

```java
// Firma:
private void fireActionRule_ProgramarReenvioAsincrono(Correo correo);
//   Implementa R-Correo-002 (Origen spec: RN-Correo-002). Diseño detallado en design/rules/R-Correo-002.md
//   (mecanismo compartido con R-Correo-001, ver design/rules/R-Correo-001.md).
//   Secuencia:
//     1. Captura correo.getId() en una variable final.
//     2. PostCommitRunner.runAfterCommit(() -> correoAsyncExecutor.submit(() -> this.enviarCorreo(correoId)));
```
