---
type: implementation-task
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/CorreoEventObserver.java` | Crear | k-guice | Observador de arranque/parada de la aplicación (ciclo de vida del executor) |

## Texto del diseño (verbatim, `design.md`, Paso 6 — Módulo Guice)

#### `com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver`

```java
package com.educaflow.subsystem.correos.infrastructure;

public class CorreoEventObserver {

    @jakarta.inject.Inject
    private CorreoAsyncExecutor correoAsyncExecutor;

    public void onAppStart(@com.axelor.event.Observes com.axelor.events.StartupEvent event);
    //   log.info("Executor de envío de correos listo"); (el pool ya existe: lo crea el Provider la
    //   primera vez que Guice construye el CorreoAsyncExecutor)

    public void onAppShutdown(@com.axelor.event.Observes com.axelor.events.ShutdownEvent event);
    //   correoAsyncExecutor.detener();
}
```

**Verificar:** `./run.sh` arranca sin `Guice/MissingConstructor` ni errores de bindeo; el log de arranque no muestra excepciones de `CorreosModule`.

## Diseño detallado de referencia (verbatim, `design/rules/R-Correo-001.md` — sección "Diseño detallado", clase `CorreoEventObserver`)

- `com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver` — observador de los eventos de ciclo de vida de Axelor (mismo mecanismo que ya usa `com.educaflow.secretariavirtual.startup.AppEventObserver`, pero autocontenido dentro del subsistema en vez de ampliar la clase global).
  - `public void onAppStart(@com.axelor.event.Observes com.axelor.events.StartupEvent event)` — log informativo (el pool ya se ha creado de forma perezosa por el `Provider` la primera vez que algo lo inyecta; no necesita lógica adicional).
  - `public void onAppShutdown(@com.axelor.event.Observes com.axelor.events.ShutdownEvent event)` — `correoAsyncExecutor.detener()`.

## Superficie cerrada

**MUST** crear únicamente la clase `CorreoEventObserver` con el campo `@Inject private CorreoAsyncExecutor correoAsyncExecutor` y exactamente los dos métodos observadores listados (`onAppStart`, `onAppShutdown`). `CorreoAsyncExecutor` (Tarea 07) ya está en el árbol y es contrato fijo. **MUST NOT** añadir el binding de Guice de esta clase aquí (va en la Tarea 10, `CorreosModule.configure()` con `bind(CorreoEventObserver.class)`). Si detectas que hace falta algo no listado, **detente y reporta** `BLOCKED`.
