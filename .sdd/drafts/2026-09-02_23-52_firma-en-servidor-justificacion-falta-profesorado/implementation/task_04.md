---
type: implementation-task
template: expediente
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-sistemas`
- `k-code-quality`

Refactoriza `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl` para que sus cuatro métodos privados deleguen en `FirmaEnServidorHelper`, **sin cambio de comportamiento**.

## Nota del descomponedor (decisiones de esta descomposición)

Esta iniciativa es un **delta** sobre una versión existente y su tabla §6 lista, además de los ficheros del tipo de expediente, varios ficheros de **subsistema** (las «piezas compartidas» de §9.1 y las reglas de §10.1) que no corresponden a ningún bloque de la tabla de tareas del contrato. Decisión tomada: **una tarea por fichero de subsistema**, en el orden de los pasos del diseño, y **todas antes** de la tarea de la fase `RECEPCION`, que es la que las consume (inyecta el servicio y usa las reglas). Así cada fila de §6 queda cubierta por exactamente una tarea.

## Fichero de esta tarea — fila verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java` | Modificar | `k-sistemas`, `k-code-quality` | Especificado en §9.1 — sus cuatro métodos privados pasan a **delegar** en `FirmaEnServidorHelper`, sin cambiar comportamiento ni literales |

## Paso del diseño (verbatim del `design.md` §7)

### Paso 4 — `TareaFirmaServiceImpl.java`

- Fichero: `src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java`; FQCN `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl`.
- Especificación quirúrgica: **§9.1**, apartado «`TareaFirmaServiceImpl`».
- **Verificación:** compila; los cuatro métodos privados (`isClaveIncorrecta`, `motivoFirmaFallida`, `isClaveCertificadoIncorrecta`, `mensajeClaveCertificadoIncorrecta`) son ahora **una sola línea** que delega en `FirmaEnServidorHelper`; **ningún** literal de mensaje ni la constante `LIMITE_CAUSAS_A_RECORRER` quedan duplicados en la clase; `validateFirmarEnServidor` y `firmarDocumentosEnMemoria` **no cambian** de comportamiento ni de mensajes (V-TareaFirma-001..008 intactas).

## Especificación quirúrgica (verbatim del `design.md` §9.1)

### 9.1 Piezas compartidas del subsistema que usan los `trigger*`

Son el **mecanismo reutilizable**: esta iniciativa las escribe (o las extrae de donde ya estaban) una sola vez y quedan disponibles para **cualquier** tipo de expediente que quiera firmar en el servidor su documento de entrada. Ninguna conoce este trámite.

Dos de ellas son **de-duplicación**, no funcionalidad nueva: `FirmaEnServidorHelper` extrae lo que hoy vive dentro de `TareaFirmaServiceImpl`, y `TareaFirmaServiceImpl` pasa a delegar en él. Sin ese par, la firma en servidor del documento de entrada y la regla del DSL serían la segunda y la tercera copia del mismo criterio y de los mismos literales (§14).


#### `TareaFirmaServiceImpl` — `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl`

Refactor **sin cambio de comportamiento**: es lo que convierte a `FirmaEnServidorHelper` en la única fuente de verdad en vez de en una cuarta copia.

- Sus cuatro métodos privados pasan a ser **una sola línea** que delega:
  - `isClaveIncorrecta(ex)` → `FirmaEnServidorHelper.isClaveIncorrecta(ex)`
  - `motivoFirmaFallida(ex, situacionFirma)` → `FirmaEnServidorHelper.motivoFirmaFallida(ex, situacionFirma)`
  - `isClaveCertificadoIncorrecta(tareaFirma)` → `FirmaEnServidorHelper.isClaveCertificadoIncorrecta(tareaFirma.getFirmante().getDni(), tareaFirma.getClaveFirma(), tareaFirma.getSituacionFirma())` — `getFirmante()` no puede ser nulo en ese punto: `validateFirmarEnServidor` solo llega a esta comprobación si V-TareaFirma-002 ya ha confirmado que el firmante es el usuario autenticado
  - `mensajeClaveCertificadoIncorrecta(situacionFirma)` → `FirmaEnServidorHelper.mensajeClaveCertificadoIncorrecta(situacionFirma)`
- **MUST** borrarse de la clase la constante `LIMITE_CAUSAS_A_RECORRER`, y **MUST NOT** quedar en ella ningún literal de los cuatro mensajes ni ningún `import` que solo sirviera para esa lógica (`UnrecoverableKeyException`, `LoginException`, `AlmacenClaveFichero`).
- **MUST NOT** tocarse nada más: `validateFirmarEnServidor` (V-TareaFirma-001..008), `firmarDocumentosEnMemoria`, los `AllowProperties` ni el `finally` que descarta la clave siguen exactamente igual, con los mismos mensajes.
- **Única diferencia observable**, y es de traza, no de negocio: el `warn` de «no se ha podido comprobar la clave del certificado» pasa a escribirlo el helper con el **DNI enmascarado** en lugar del `id` de la tarea. Se acepta a sabiendas: ninguna comprobación, ningún mensaje de usuario y ningún test dependen de esa línea.

## Notas y supuestos que aplican a esta tarea (verbatim del `design.md` §14)

- **Una sola fuente de verdad para «la clave no es correcta» (`FirmaEnServidorHelper`).** El criterio (recorrer la cadena de causas buscando `UnrecoverableKeyException`/`LoginException`, con un límite de niveles), la comprobación previa con `isPasswordValid()` solo en certificados en fichero y los textos que explican el fallo ya existían **dentro** de `TareaFirmaServiceImpl`. Copiarlos aquí habría dejado **tres** copias (la tarea de firma, la firma del documento de entrada y la regla del DSL) del mismo `switch` y de los mismos literales, justo lo que las guías prohíben («MUST reutilizarse ese mecanismo en vez de inventar uno nuevo»).
  Por eso esta iniciativa **extrae** esa lógica a `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper` y **refactoriza `TareaFirmaServiceImpl` para que delegue en ella** (§9.1): es un cambio en código ya en producción, pero sin cambio de comportamiento ni de mensajes, y es la única forma de que la de-duplicación sea real y no una copia más. La única diferencia observable es una línea de `warn` que pasa a llevar el DNI enmascarado en vez del `id` de la tarea.

## Instrucciones

- Acción de la fila §6: **Modificar**. Se toca **solo** este fichero.
- Es un refactor de **delegación**: **MUST NOT** cambiar ningún comportamiento, ningún literal de mensaje ni ninguna validación (`V-TareaFirma-001..008` intactas).
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
