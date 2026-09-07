---
type: implementation-task
template: expediente
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-sistemas`
- `k-secure-coding`
- `k-code-quality`

Modifica `com.educaflow.subsystem.expedientes.controllers.ExpedienteController` para que, al cancelarse un evento por una validación, devuelva la clave de firma vacía (RUI-011).

## Nota del descomponedor (decisiones de esta descomposición)

Esta iniciativa es un **delta** sobre una versión existente y su tabla §6 lista, además de los ficheros del tipo de expediente, varios ficheros de **subsistema** (las «piezas compartidas» de §9.1 y las reglas de §10.1) que no corresponden a ningún bloque de la tabla de tareas del contrato. Decisión tomada: **una tarea por fichero de subsistema**, en el orden de los pasos del diseño, y **todas antes** de la tarea de la fase `RECEPCION`, que es la que las consume (inyecta el servicio y usa las reglas). Así cada fila de §6 queda cubierta por exactamente una tarea.

## Fichero de esta tarea — fila verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/controllers/ExpedienteController.java` | Modificar | `k-sistemas`, `k-secure-coding` | Especificado en §9.1 — al cancelarse un evento por una validación, devuelve la clave de firma vacía usando la constante del servicio (RUI-011) |

## Paso del diseño (verbatim del `design.md` §7)

### Paso 7 — `ExpedienteController.java`

- Fichero: `src/main/java/com/educaflow/subsystem/expedientes/controllers/ExpedienteController.java`; FQCN `com.educaflow.subsystem.expedientes.controllers.ExpedienteController`.
- Especificación quirúrgica: **§9.1**, apartado «`ExpedienteController`».
- **Verificación:** compila; la rama `catch (BusinessException)` de `triggerEvent` devuelve además el valor vacío de la clave de firma **usando la constante** `FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA`, sin ningún literal de nombre de campo en el fichero; el resto del método no cambia.

## Especificación quirúrgica (verbatim del `design.md` §9.1)

### 9.1 Piezas compartidas del subsistema que usan los `trigger*`

Son el **mecanismo reutilizable**: esta iniciativa las escribe (o las extrae de donde ya estaban) una sola vez y quedan disponibles para **cualquier** tipo de expediente que quiera firmar en el servidor su documento de entrada. Ninguna conoce este trámite.

Dos de ellas son **de-duplicación**, no funcionalidad nueva: `FirmaEnServidorHelper` extrae lo que hoy vive dentro de `TareaFirmaServiceImpl`, y `TareaFirmaServiceImpl` pasa a delegar en él. Sin ese par, la firma en servidor del documento de entrada y la regla del DSL serían la segunda y la tercera copia del mismo criterio y de los mismos literales (§14).


#### `ExpedienteController` — `com.educaflow.subsystem.expedientes.controllers.ExpedienteController`

Un único cambio, en la rama `catch (BusinessException ex)` de `triggerEvent`: además de `actionResponseHelper.doResponseBusinessMessages(...)`, devolver el valor **vacío** de la clave de firma con `response.setValue(FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA, null)`.

- **MUST NOT** escribirse el nombre del campo como literal en este fichero: el controlador es genérico y el nombre del campo es contrato del subsistema, declarado en una sola constante (§4.1).

- Es lo que hace RUI-011 («tras un intento cancelado el campo de la clave vuelve a mostrarse vacío») y la parte visible de RN-006 («la clave se descarta también cuando la acción se cancela»).
- Es genérico: para un tipo de expediente que no declare ese campo, el valor simplemente no se aplica a ningún widget.
- **MUST NOT** tocarse nada más del método: ni el camino de éxito, ni el de `EXIT`/`DELETE`, ni el `catch (UnauthorizedException)`, que **MUST** seguir sin envolver.

## Reparto de reglas — filas que aplican (verbatim del `design.md` §11)

| Regla | Capa | Dónde |
|---|---|---|
| RN-006 — descartar la clave al terminar `PRESENTAR` | `trigger*` + controlador | el `finally` de `triggerPresentar` que **envuelve las acciones 2–6** (§9.2) y el `response.setValue(FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA, null)` de `ExpedienteController` cuando la acción se cancela |
| RUI-…-CREADOR-011 — la clave vuelve vacía tras un intento cancelado | controlador | `response.setValue(FirmaDocumentoEntradaService.CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA, null)` en `ExpedienteController` (§9.1) |

## Instrucciones

- Acción de la fila §6: **Modificar**. Se toca **solo** este fichero, y dentro de él **solo** la rama `catch (BusinessException ex)` de `triggerEvent`.
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
