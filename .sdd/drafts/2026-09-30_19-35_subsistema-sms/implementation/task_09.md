---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/module/EjecutorAsincronoProvider.java` | Crear | k-guice | Construye el `EjecutorAsincrono` leyendo `async.pool-size` (por defecto 2) |

### Paso 3 — `base/infrastructure/async/EjecutorAsincrono`: **un solo** ejecutor para toda la aplicación

**Ficheros:** `base/infrastructure/async/EjecutorAsincrono.java` (Crear), `base/infrastructure/CLAUDE.md` (Modificar), `secretariavirtual/module/EjecutorAsincronoProvider.java` (Crear), `secretariavirtual/module/SecretariaVirtualModule.java` (Modificar), `secretariavirtual/startup/AppEventObserver.java` (Modificar), `correos/infrastructure/PostCommitRunner.java`, `correos/infrastructure/CorreoAsyncExecutor.java`, `correos/infrastructure/CorreoEventObserver.java` y `correos/module/CorreoAsyncExecutorProvider.java` (Eliminar), `correos/module/CorreosModule.java` y `correos/service/impl/CorreoServiceImpl.java` (Modificar), y los ficheros de test del final de la tabla.

Motivo: extrae el mecanismo de correos a un único ejecutor compartido; ver `decisiones.md` D3.

#### Cableado del ejecutor único

```java
// Clase: com.educaflow.secretariavirtual.module.EjecutorAsincronoProvider implements Provider<EjecutorAsincrono>
public EjecutorAsincrono get();

```

- **`EjecutorAsincronoProvider.get()`** — construye un `EjecutorAsincrono` nuevo con el tamaño de pool que lee
  de la propiedad de configuración `async.pool-size` (vía `AppSettings`), con 2 como valor por defecto si no
  está definida. Es el **único** sitio del proyecto que
  lee un tamaño de pool (antes había uno por subsistema). Hace falta `Provider` (no basta `bind(...).to(...)`)
  porque el tamaño viene de configuración y no de otro bean (`k-guice` §3.3/§4.2).



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `CorreoAsyncExecutorProvider` | `src/main/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProvider.java` | — (mismo motivo). Lo sustituye `EjecutorAsincronoProvider`, el único sitio del proyecto que lee un tamaño de pool. **Efecto de configuración:** la clave `mail.send.pool-size` deja de leerse y la sustituye `async.pool-size` (mismo default, 2); la clave se sustituye en `axelor-config.properties` (paso 1) |
