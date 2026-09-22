# R-AsistenteNuevoExpediente-005 — Las dos preguntas del paso 3, resueltas desde una sola matriz forma × destinatario

**Entidad:** AsistenteNuevoExpediente
**Origen spec:** — (regla propia del diseño; campos `hayQuePreguntarPresentacion`, `presentadoEnPapel`, `hayQuePreguntarParaQuien` y `presentadoEnRepresentacion`)
**Operación:** `recalcular` (acción propia del servicio, invocada desde `AsistenteNuevoExpedienteController.recalcular`)
**Momento:** Antes — asignación incondicional. «Antes» significa **antes de devolver el bean a la vista**, no antes de `repository.save`: el modelo es `persistable="false"` y en este sistema no hay ningún `save` al que anteponerse (ver la nota bajo «Reglas de negocio» en `design.md`).
**Servicio host:** com.educaflow.system.ventanilla.service.impl.AsistenteNuevoExpedienteServiceImpl
**Método host:** fireActionRule_AsignarPresentacion(AsistenteNuevoExpediente asistente)

## Análisis de la regla

**Qué se dispara y cuándo.** La regla es el único contenido de la acción `recalcular`, que la invoca **una sola vez** después de `validateRecalcular(asistente).ifPresent(BusinessMessages::throwIfInvalid)`. La vista del paso 3 (`Main-AsistenteNuevoExpediente.xml`) llama a `recalcular` en su `onNew` y en el `onChange` de `presentadoEnPapel`, así que la regla corre **antes** de cualquier `validateTriggerInitialEvent`.

**Qué resuelve.** El paso 3 tiene dos preguntas encadenadas — «¿Cómo se presenta?» (`presentadoEnPapel`) y «¿Para quién es?» (`presentadoEnRepresentacion`) — y por cada una hay que decidir dos cosas: si se enseña (`hayQuePreguntar*`) y con qué valor queda el campo cuando no se enseña. Las dos preguntas **no son separables**: la de «para quién es» solo puede contestarse sobre el `presentadoEnPapel` que la otra acaba de fijar. Por eso son **una sola regla** (`R-006` fue absorbida por esta) y por eso las dos salen de la **misma** matriz: así no pueden discrepar entre sí ni con lo que el servidor acepta en `validateTriggerInitialEvent`.

**Qué información lee y de dónde.** Solo `tramite` y `centro` del propio bean (ya fijados por los pasos 1 y 2, y comprobados por `validateRecalcular` con V-AsistenteNuevoExpediente-001 y V-AsistenteNuevoExpediente-009), más `presentadoEnPapel` cuando la primera pregunta se hace (es la respuesta del usuario, y está en `allowPropertiesRecalcular`). **No** lee `presentadoEnRepresentacion`: esta acción lo recalcula siempre, por eso ese campo no está en la whitelist de `recalcular`.

**Qué acciones realiza y en qué orden.**

1. **Calcular la matriz, una sola vez.** Las cuatro celdas de la matriz forma × destinatario se calculan **aquí y solo aquí**, preguntando al oráculo `admiteAlta(tramite, centro, presentadoEnPapel, presentadoEnRepresentacion)`. Es el **único** cálculo de la regla: cuatro llamadas, no seis.
2. **Asignar la primera pregunta** (`fireActionRule_AsignarFormaDePresentar`), que devuelve el valor con el que deja `presentadoEnPapel`.
3. **Asignar la segunda pregunta** (`fireActionRule_AsignarDestinatario`), sobre la **fila** de la matriz que corresponde a la forma que el paso anterior acaba de fijar, recibida como **parámetro** y no releída del bean: la dependencia entre los dos se lee en la firma, no en el orden de las líneas.

El cálculo (paso 1) está separado de las dos mutaciones del bean (pasos 2 y 3). **MUST NOT** deshacerse esto volviendo a **dos reglas** que `recalcular` invoque por separado —la regla es UNA y `recalcular` la invoca UNA sola vez, por su método host `fireActionRule_AsignarPresentacion`, que es el único que las dos mitades tienen por llamante— ni a seis llamadas a `admiteAlta`.

**Efectos colaterales y garantías.** El único efecto es la mutación de los cuatro campos del bean en memoria; no hay persistencia, ni llamadas a sistemas externos, ni transacción propia que coordinar, así que no se plantea idempotencia ni rollback parcial más allá de que dos ejecuciones seguidas con la misma entrada dejan el bean igual. Las asignaciones de `hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien` y `presentadoEnRepresentacion` son **incondicionales**; la de `presentadoEnPapel` lo es en la rama en la que la pregunta no se hace. Que `presentadoEnRepresentacion` se reasigne siempre —sin conservar la respuesta anterior del usuario— es justo lo que exige ESC-021 al cambiar la forma de presentar.

**Qué errores puede encontrar y cómo tratarlos.** La regla **no puede lanzar** por una forma de presentar que el usuario no tenga: **MUST NOT** llamar a `perfilesUsuarioService.getPerfil` aquí. `presentadoEnPapel` está en `allowPropertiesRecalcular`, así que el cliente puede enviar una forma que no tiene, y `recalcular` corre en el `onNew`/`onChange`, **antes** de cualquier `validateTriggerInitialEvent`; `getPerfil` lanza `IllegalStateException` justo en ese caso y la ventanilla contestaría un 500 a una petición manipulada, cuando HU-006 exige un mensaje. El perfil lo deduce `validateAlta` de la forma de presentar (vía `perfilDeInicioPara`), y si el usuario no la tiene el motor devuelve mensaje: las celdas de esa fila quedan a `false`, la pregunta no se ve y `validateTriggerInitialEvent` rechaza con el literal del motor. Los casos de centro o trámite ausente/no evaluable no llegan aquí: los corta `validateRecalcular` antes, con los mismos literales que `validateTriggerInitialEvent`. Lo que queda son errores de programación (ver «Errores»).

**Entradas/salidas de cada colaborador.** El único colaborador de la regla es `admiteAlta` (privado del propio servicio): recibe `(Tramite, Centro, boolean presentadoEnPapel, boolean presentadoEnRepresentacion)` y devuelve `boolean` — `validateAlta(...).isEmpty()`. No recibe `Profile`: lo deduce `validateAlta`, así que preguntar por una forma que el usuario no tiene devuelve `false`, nunca una excepción. `fireActionRule_AsignarFormaDePresentar` y `fireActionRule_AsignarDestinatario` **no** vuelven a preguntar al oráculo: solo leen la `MatrizPresentacion` ya calculada.

## Diseño detallado

### Clases nuevas

Ninguna. La regla vive entera en el servicio host `AsistenteNuevoExpedienteServiceImpl`, con sus dos asignadores privados, que son las dos **mitades** de esta regla y por eso se declaran en «Action Rules» del Paso 3 de `design.md`, junto a su método host (`k-sistemas/servicios.md`, bloque 4):

- `com.educaflow.system.ventanilla.service.impl.AsistenteNuevoExpedienteServiceImpl` — añade a la clase ya diseñada:
  - `private Boolean fireActionRule_AsignarFormaDePresentar(AsistenteNuevoExpediente asistente, MatrizPresentacion matriz)` — asigna los dos campos de la primera pregunta y **devuelve** el valor con el que deja `presentadoEnPapel` (`null` incluido), que es lo que necesita la segunda. `hayQuePreguntarPresentacion = matriz.admiteAlgunDestinatario(false) && matriz.admiteAlgunDestinatario(true)` (asignación incondicional). `presentadoEnPapel`: si hay que preguntar se respeta lo que llegue del cliente (es su respuesta a la pregunta); en cualquier otro caso se sobrescribe **incondicionalmente** con `matriz.admiteAlgunDestinatario(true)`, que es exactamente la única forma que le sirve (o `false` si no le sirve ninguna, caso que la puerta 5 de `validateTriggerInitialEvent` rechaza con el literal que corresponda, el del motor o el del afinado). La condición **no** es una guarda de nulidad (`if (campo == null)`), que es lo que `k-secure-coding` §3.3 prohíbe: no mira si el cliente mandó algo, sino si la pregunta se hace, que es lo que decide de quién es el campo en esta acción. `presentadoEnPapel` es `cliente` (está en `allowPropertiesRecalcular` y en `allowPropertiesTriggerInitialEvent`) y solo pasa a ser `servidor` en la rama en la que la pregunta **no** se hace, donde se sobrescribe incondicionalmente. Es la misma doble clasificación de `centro`, descrita en el Paso 1.
  - `private void fireActionRule_AsignarDestinatario(AsistenteNuevoExpediente asistente, MatrizPresentacion matriz, Boolean presentadoEnPapel)` — asigna los dos campos de la segunda pregunta sobre la **fila** de la matriz que corresponde a la forma ya fijada, que recibe como parámetro. Si `presentadoEnPapel` no está contestado (se pregunta y el cliente aún no ha respondido), `hayQuePreguntarParaQuien` queda a `false` y `presentadoEnRepresentacion` sin valor: sin saber cómo se presenta no se puede decir para quién puede ser, y la pregunta no debe verse; es la **única** rama que deja el campo sin valor, y la rechaza la puerta 3 de `validateTriggerInitialEvent`, que exige `presentadoEnPapel` antes de mirar el destinatario. Si ya está contestado, `hayQuePreguntarParaQuien` = las dos celdas de su fila son ciertas (`matriz.admite(presentadoEnPapel, false) && matriz.admite(presentadoEnPapel, true)`), y `presentadoEnRepresentacion` = sin valor si hay que preguntar (la respuesta llega sin marcar), el único destinatario posible si solo una celda de la fila lo es, y `false` si **ninguna** celda de la fila lo es. Ese `false` es la **misma** convención que usa `fireActionRule_AsignarFormaDePresentar` para su propio «ninguna forma sirve»: el campo queda informado, la puerta 4 no dispara y es la puerta 5 quien rechaza con el literal que corresponda (el del motor o el del afinado), en vez de pedirle al usuario que rellene una pregunta que no ve. Las dos asignaciones son **incondicionales**: la respuesta anterior del usuario **no** se conserva, que es justo lo que exige ESC-021.

### Interfaces

Ninguna. La regla no necesita ningún contrato nuevo: su único colaborador (`admiteAlta`) es un método privado del propio servicio, y por debajo de él el motor de tramitación se usa por la interfaz ya existente `TramitadorService`.

### Tipos propios

- `com.educaflow.system.ventanilla.service.impl.AsistenteNuevoExpedienteServiceImpl.MatrizPresentacion` (`private record`) — campos: `boolean yoMismoParaMi`, `boolean yoMismoEnRepresentacion`, `boolean enPapelParaMi`, `boolean enPapelEnRepresentacion`. **Semántica:** las cuatro celdas de la matriz forma × destinatario **ya calculadas** (cada celda es el veredicto de `admiteAlta` para esa pareja), y lo **único** que reciben las dos asignaciones: ninguna vuelve a preguntar al oráculo. Correspondencia celda ↔ pareja:

  | Celda | `presentadoEnPapel` | `presentadoEnRepresentacion` |
  |---|---|---|
  | `yoMismoParaMi` | `false` | `false` |
  | `yoMismoEnRepresentacion` | `false` | `true` |
  | `enPapelParaMi` | `true` | `false` |
  | `enPapelEnRepresentacion` | `true` | `true` |

  Dos métodos de lectura, **sin más lógica**:
  - `admite(forma, destinatario)` → la celda de esa pareja.
  - `admiteAlgunDestinatario(forma)` → alguna de las dos celdas de esa fila.

### Diagrama de secuencia

```
fireActionRule_AsignarPresentacion(asistente)
  ├─ admiteAlta(tramite, centro, false, false) → boolean  yoMismoParaMi
  ├─ admiteAlta(tramite, centro, false, true)  → boolean  yoMismoEnRepresentacion
  ├─ admiteAlta(tramite, centro, true,  false) → boolean  enPapelParaMi
  ├─ admiteAlta(tramite, centro, true,  true)  → boolean  enPapelEnRepresentacion
  │     └─ (cada una) validateAlta(...).isEmpty()
  │            ├─ perfilDeInicioPara(presentadoEnPapel) → Profile
  │            ├─ tramitadorService.validateTriggerInitialEvent(contexto) → Optional<BusinessMessages>
  │            └─ validateAfinadoDestinatario(contexto) → Optional<BusinessMessages>   (solo si el motor acepta)
  ├─ MatrizPresentacion matriz = las cuatro celdas anteriores
  ├─ fireActionRule_AsignarFormaDePresentar(asistente, matriz) → Boolean presentadoEnPapel
  │     ├─ matriz.admiteAlgunDestinatario(false) / (true) → hayQuePreguntarPresentacion
  │     └─ asistente.setHayQuePreguntarPresentacion(...) / setPresentadoEnPapel(...)
  └─ fireActionRule_AsignarDestinatario(asistente, matriz, presentadoEnPapel)
        ├─ matriz.admite(presentadoEnPapel, false) / (presentadoEnPapel, true) → hayQuePreguntarParaQuien
        └─ asistente.setHayQuePreguntarParaQuien(...) / setPresentadoEnRepresentacion(...)
```

### Errores

| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| Centro ausente, o trámite ausente o no evaluable | `AsistenteNuevoExpedienteServiceImpl.validateRecalcular` | Corta **antes** de la regla: `recalcular` empieza por `validateRecalcular(asistente).ifPresent(BusinessMessages::throwIfInvalid)`, con los literales "Debe indicar el centro" (V-001) y "Debe indicar el trámite" (V-009). La regla puede dar por hecho que los dos están y que el trámite es evaluable. |
| El cliente envía una forma de presentar (`presentadoEnPapel`) para la que no tiene perfil de inicio | `AsistenteNuevoExpedienteServiceImpl.admiteAlta` → `validateAlta` | **No es error**: las celdas de esa fila quedan a `false`, la pregunta no se ve y la puerta 5 de `validateTriggerInitialEvent` rechaza con el literal del motor. La regla **MUST NOT** llamar a `perfilesUsuarioService.getPerfil`, que lanzaría `IllegalStateException` (un 500 donde HU-006 exige un mensaje). |
| Ninguna pareja de la matriz es admisible (las cuatro celdas a `false`) | `AsistenteNuevoExpedienteServiceImpl.fireActionRule_AsignarFormaDePresentar` / `fireActionRule_AsignarDestinatario` | **No es error**: los dos `hayQuePreguntar*` quedan a `false` y los dos campos quedan informados con `false`, para que sea la puerta 5 de `validateTriggerInitialEvent` quien rechace con el literal que corresponda (el del motor o el del afinado) y no la puerta 3 ni la 4. |
| El enum `Profile` deja de tener correspondencia 1:1 con la forma de presentar (cero o más de un perfil) | `AsistenteNuevoExpedienteServiceImpl.perfilDeInicioPara` (bajo `admiteAlta`) | `IllegalStateException`: es un error de programación, no una entrada del usuario. **MUST NOT** devolver el primero ni `null`. |
| El usuario no pertenece al centro al llegar al afinado del destinatario | `AsistenteNuevoExpedienteServiceImpl.tieneTipoUsuario` (bajo `admiteAlta` → `validateAfinadoDestinatario`) | `IllegalStateException`: en el único punto desde el que se llega ahí el motor ya ha aceptado el contexto, y sin pertenecer al centro no habría perfiles de inicio; devolver `false` convertiría un error de programación en un veredicto de negocio. |

### Contenido del método `fireActionRule_*`

```java
// Firma:
private void fireActionRule_AsignarPresentacion(AsistenteNuevoExpediente asistente);
//   Implementa R-AsistenteNuevoExpediente-005 (Origen spec: —; campos `hayQuePreguntarPresentacion`,
//   `presentadoEnPapel`, `hayQuePreguntarParaQuien` y `presentadoEnRepresentacion`).
//   Diseño detallado en design/rules/R-AsistenteNuevoExpediente-005.md.
//   Sigue siendo UNA sola regla y `recalcular` la invoca UNA sola vez. Tres líneas, en este orden:
//   Secuencia:
//     1. MatrizPresentacion matriz = las CUATRO celdas de la matriz forma x destinatario, calculadas
//        aquí y solo aquí preguntando al oráculo admiteAlta (el único cálculo de la regla, y una
//        sola vez): las dos preguntas del formulario salen de la MISMA matriz, así que no pueden
//        discrepar entre sí ni con lo que el servidor acepta.
//          yoMismoParaMi           = admiteAlta(tramite, centro, false, false)
//          yoMismoEnRepresentacion = admiteAlta(tramite, centro, false, true)
//          enPapelParaMi           = admiteAlta(tramite, centro, true,  false)
//          enPapelEnRepresentacion = admiteAlta(tramite, centro, true,  true)
//     2. Boolean presentadoEnPapel = fireActionRule_AsignarFormaDePresentar(asistente, matriz);
//     3. fireActionRule_AsignarDestinatario(asistente, matriz, presentadoEnPapel);
//   El cálculo (paso 1) está separado de las dos mutaciones del bean (pasos 2 y 3), y el
//   destinatario recibe como PARÁMETRO la forma que el paso anterior acaba de fijar en vez de
//   releerla del bean: la dependencia entre los dos se lee en la firma, no en el orden de las líneas.
//   MUST NOT deshacer esto volviendo a DOS reglas que recalcular invoque por separado (la regla es
//   una y recalcular la invoca una sola vez, por este método host) ni a seis llamadas a admiteAlta.
//   MUST NOT llamar a perfilesUsuarioService.getPerfil aquí (ver «Errores» de este fichero).
```
