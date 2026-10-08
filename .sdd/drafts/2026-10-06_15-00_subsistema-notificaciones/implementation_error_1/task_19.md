---
type: implementation-task
template: system
---

# Tarea 19 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Controlador NotificacionController

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/controller/NotificacionController.java` | Crear | k-sistemas (controladores.md) | Apertura polimórfica de forms, alta y `<domain>` de «Del centro» |

## Paso del diseño

### Paso 8 — Controladores y ayudante de respuesta

> Contexto (ayudante que usa, lo modifica la tarea 18):

```java
public void doResponseViewFormEnPopup(String viewName, Class<? extends Model> modelClass, Long id, String title);
```

**`NotificacionController`** (`…controller`, `@Inject` nada: no llama a servicios):

```java
@CallMethod public void abrirTodas(ActionRequest actionRequest, ActionResponse actionResponse);      // abrirEnPopup(..., "Main")
@CallMethod public void abrirDelCentro(ActionRequest actionRequest, ActionResponse actionResponse);  // abrirEnPopup(..., "Centro")
@CallMethod public void abrirRecibida(ActionRequest actionRequest, ActionResponse actionResponse);   // abrirEnPopup(..., "Mis")
@CallMethod public void continuarAlta(ActionRequest actionRequest, ActionResponse actionResponse);
//   tipo = tipoDelContexto(actionRequest); ActionResponseHelper.doResponseViewFormEnPopup(
//   nombreDelForm("Main", tipo.getClaseNotificacion()), tipo.getClaseNotificacion(), null, I18n.get("Notificación")).
@CallMethod public void prepararAlta(ActionRequest actionRequest, ActionResponse actionResponse);
//   onNew de Main@Correo-form / Main@Sms-form: actionResponse.setValue("tipoNotificacion",
//   TipoNotificacion.deClase(new ActionRequestHelper<>(actionRequest).getModelClass())). Solo presentación: el
//   valor que cuenta lo vuelve a asignar R-Notificacion-001 al guardar (U-notificaciones-todas-001/013).

/* header «Acciones de Validaciones» (vacío) */
/* header «Métodos privados» */
private void abrirEnPopup(ActionRequest actionRequest, ActionResponse actionResponse, String variante);
//   tipo = tipoDelContexto(actionRequest); id = new ActionRequestHelper<>(actionRequest).getId();
//   doResponseViewFormEnPopup(nombreDelForm(variante, tipo.getClaseNotificacion()), tipo.getClaseNotificacion(), id, I18n.get("Notificación")).
//   No carga la fila: el popup la pide por REST, donde Axelor aplica los permisos del usuario (sin IDOR).
private static TipoNotificacion tipoDelContexto(ActionRequest actionRequest);
//   TipoNotificacion.valueOf(String del context "tipoNotificacion"). Sin tipo → IllegalStateException
//   (todas las filas lo tienen; la elección de canal no deja continuar sin él, U-notificaciones-todas-002).
private static String nombreDelForm(String variante, Class<? extends Notificacion> clase);
//   "subsysNotificaciones." + variante + "@" + clase.getSimpleName() + "-form" — el ÚNICO sitio con la convención
//   variante + clase → form (la misma de k-vistas, que verifican VAR-2.1/2.3).
```

Verificación (al cerrar el bloque 1–9): compila; `./gradlew -q test --tests '*architecture*'` (C9, C15, C28).

## Trazabilidad Origen spec → V/R/U → ubicación

### U

| ID | Origen spec | Ubicación |
|---|---|---|
| U-notificaciones-todas-003 | RUI-notificaciones-todas-formulario-correo-001 | `Main@Correo-form` `tipoNotificacion` `readonly` + `onNew` → `Main@Correo-Remote-prepararAlta-action` (`NotificacionController.prepararAlta`) |

## Notas y supuestos

- **Riesgos del front a confirmar en `/sdd-debug-with-test-e2e-desc`** (D3): que `popup="reload"` refresque la pestaña de «Todas» tras guardar un alta abierta desde la elección de canal (popup sobre popup). Si `continuarAlta` + `close` cerrara también el alta, la alternativa es que el controlador responda `setCanClose(true)` después del `setView`.
- **`destino` en la base** (D2): `Notificacion.computeDestino()` lanza `IllegalStateException`. El único sitio donde existe una `Notificacion` «genérica» en memoria es el form de elección de canal, y `NotificacionController.continuarAlta` lee `tipoNotificacion` del contexto sin construir el bean ni pedir su destino; si en la depuración apareciera algún camino del framework que serializa ese registro nuevo completo, se resuelve en ese camino, no relajando la base a `return null`.
