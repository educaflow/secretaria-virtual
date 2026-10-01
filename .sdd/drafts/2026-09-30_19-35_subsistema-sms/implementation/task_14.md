---
type: implementation-task
template: system
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- (ninguno: la tabla del diseño no asigna skill a estos ficheros)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | Eliminar | — | Su función es ahora `EjecutorAsincrono.ejecutarTrasCommit` (ver «Eliminaciones declaradas») |
| `src/test/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunnerTest.java` | Eliminar | — | Su contenido vive ahora en `EjecutorAsincronoTest` |



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `PostCommitRunner` (y su método estático `runAfterCommit`) | `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | — (sin ID de spec: lo exige `CLAUDE.md` §`cpdCheck`, «un duplicado se arregla extrayendo el código común en su sitio»). Lo sustituye `EjecutorAsincrono.ejecutarTrasCommit`; ver `decisiones.md` D3 |
| Clases de test `PostCommitRunnerTest`, `CorreoAsyncExecutorTest`, `CorreoAsyncExecutorProviderTest` y `CorreoEventObserverTest` | `src/test/java/com/educaflow/subsystem/correos/` | — (consecuencia de las anteriores). Sus casos se trasladan **íntegros** a `EjecutorAsincronoTest`, `EjecutorAsincronoProviderTest` y `AppEventObserverTest`; no se pierde ninguna comprobación |



`grep -rn "PostCommitRunner\|CorreoAsyncExecutor\|CorreoEventObserver\|ProgramarReenvioAsincrono" src/` no devuelve nada;

> **Nota del descomponedor:** acción `Eliminar`. No hay código que escribir: el implementador borra los ficheros listados con `git rm` (o los da por hechos si ya no existen) y **MUST NOT** tocar ningún otro fichero. Las referencias que otras clases tengan a lo borrado las retiran las tareas que modifican esas clases (`CorreoServiceImpl`, `CorreosModule`, `CorreoServiceImplTest`); si quedara alguna, la resuelve el bucle de build. Decisión tomada porque `implementation.md` no define la materialización de `Eliminar`.
