---
type: implementation-task
template: system
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- (ninguno: la tabla del diseño no asigna skill a estos ficheros)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProvider.java` | Eliminar | — | Lo sustituye `EjecutorAsincronoProvider` (una sola lectura de `*.pool-size` en todo el proyecto) |
| `src/test/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProviderTest.java` | Eliminar | — | Su contenido vive ahora en `EjecutorAsincronoProviderTest` |



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `CorreoAsyncExecutorProvider` | `src/main/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProvider.java` | — (mismo motivo). Lo sustituye `EjecutorAsincronoProvider`, el único sitio del proyecto que lee un tamaño de pool. **Efecto de configuración:** la clave `mail.send.pool-size` deja de leerse y la sustituye `async.pool-size` (mismo default, 2); la clave se sustituye en `axelor-config.properties` (paso 1) |
| Clases de test `PostCommitRunnerTest`, `CorreoAsyncExecutorTest`, `CorreoAsyncExecutorProviderTest` y `CorreoEventObserverTest` | `src/test/java/com/educaflow/subsystem/correos/` | — (consecuencia de las anteriores). Sus casos se trasladan **íntegros** a `EjecutorAsincronoTest`, `EjecutorAsincronoProviderTest` y `AppEventObserverTest`; no se pierde ninguna comprobación |

> **Nota del descomponedor:** acción `Eliminar`. No hay código que escribir: el implementador borra los ficheros listados con `git rm` (o los da por hechos si ya no existen) y **MUST NOT** tocar ningún otro fichero. Las referencias que otras clases tengan a lo borrado las retiran las tareas que modifican esas clases (`CorreoServiceImpl`, `CorreosModule`, `CorreoServiceImplTest`); si quedara alguna, la resuelve el bucle de build. Decisión tomada porque `implementation.md` no define la materialización de `Eliminar`.
