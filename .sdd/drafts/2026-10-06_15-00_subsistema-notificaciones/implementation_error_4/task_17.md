---
type: implementation-task
template: system
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `tramites/util/verificacion/VerificacionHelper.java` | Modificar | k-sistemas | Usa `NotificacionService.createCorreo()`, rellena el motivo y liga el estado actual del expediente |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

## Diseño — Paso 7

### Paso 7 — Consumidor externo: aviso de subsanación

**`VerificacionHelper`** (`tramites/util/verificacion`, Modificar; el resto de la clase se conserva):

```java
public boolean avisarDeSubsanacion(Expediente expediente, String textoSubsanacion);
//   Mismo contrato. Resuelve NotificacionService y CorreoService con modelServiceFactory; correo = crearCorreoSubsanacion(...);
//   si CorreoService.validateInsert(correo) devuelve mensajes → log.info (CRLF saneados, como ahora) y false;
//   si no → CorreoService.insert(correo) y true. R-Correo-002 (Origen spec: RN-Correo-002).

private Correo crearCorreoSubsanacion(NotificacionService notificacionService, Expediente expediente, String textoSubsanacion);
//   (deja de ser static: usa la factoría) correo = notificacionService.createCorreo(); rellena como ahora dni, nombre,
//   apellidos, para (email del solicitante), centro, asunto y cuerpo, y además:
//     name = I18n.get("Subsanación del expediente %s").formatted(numeroExpediente)
//     historialEstado = estadoActual(expediente)

private static HistorialEstado estadoActual(Expediente expediente);
//   El HistorialEstado más reciente (máximo por fecha) de expediente.getHistorialEstados(): el del estado en que
//   está el expediente cuando se pide la subsanación, porque el triggerVerificar llama a este helper antes de que
//   el Tramitador añada el historial del estado nuevo.
```

Verificación (al cerrar el bloque 1–9): compila; `grep -rn "subsystem.correos" src/main/java` vacío.

## Frontera de confianza — AllowProperties por acción

### DTO de alta programática

No hay DTO: la alta programática (`VerificacionHelper`) parte de `NotificacionService.createCorreo()` y pasa por el mismo `CorreoService.validateInsert/insert`, donde R-Notificacion-001 vuelve a fijar todos los campos servidor.

## Trazabilidad Origen spec → V/R/U → ubicación (filas que aplican a esta tarea)

### R

| ID | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Correo-002 | RN-Correo-002 | `VerificacionHelper.avisarDeSubsanacion` / `crearCorreoSubsanacion` / `estadoActual` (tramites/util) | En la transacción del evento de verificación |
