---
type: implementation-task
template: system
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/startup/AppEventObserver.java` | Modificar | k-guice | Su `onAppShutdown` **ya existente** para el ejecutor (`detener()`); no hace falta ningún observer nuevo |

#### Cableado del ejecutor único

```java

// Clase: com.educaflow.secretariavirtual.startup.AppEventObserver  (Modificar — delta)
public void onAppShutdown(@Observes ShutdownEvent event);
```

- **`AppEventObserver.onAppShutdown(...)`** — **delta:** se le inyecta el `EjecutorAsincrono` (`@Inject`) y su
  `onAppShutdown` —que **ya existe** y ya se ejecuta al apagar— añade `ejecutorAsincrono.detener()` antes del log
  de despedida. **MUST NOT** crearse ningún observer nuevo: la pieza que escucha el `ShutdownEvent` de la
  aplicación ya está escrita y es esta.



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `CorreoEventObserver` | `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoEventObserver.java` | — (mismo motivo). Su única responsabilidad real (`detener()` al `ShutdownEvent`) pasa al `AppEventObserver` que la aplicación **ya tiene**; su `onAppStart` solo escribía una línea de log |
