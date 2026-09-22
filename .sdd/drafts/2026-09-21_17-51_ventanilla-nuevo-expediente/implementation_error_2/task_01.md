---
type: implementation-task
template: system
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

Materializa el **modelo de dominio** del asistente.

El fichero **ya está materializado y validado** por el diseñador en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/domains/AsistenteNuevoExpediente.xml`.
**MUST** copiarse **literalmente** (byte a byte) a su ruta destino
`src/main/java/com/educaflow/system/ventanilla/domains/AsistenteNuevoExpediente.xml`.
**MUST NOT** regenerarlo, reescribirlo ni modificarlo. La acción es `Crear`: la ruta destino no existe
todavía (hay que crear la carpeta `system/ventanilla/domains/`).


## Estado del árbol al empezar (decisión del descomponedor, no del diseño)

Una ejecución anterior de `/sdd-implementer` se detuvo por un error del diseño (ver
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/implementation_error_1/error_design.log`) y dejó
**ya materializados** en el árbol los ficheros de esta tarea, escritos contra una versión anterior del
diseño. La **fuente de verdad es el diseño de hoy**: si el fichero ya existe, **MUST** comprobarse que
coincide exactamente con lo que este texto prescribe y, si no coincide, dejarlo como el diseño manda
(para los XML materializados, copia literal desde `design/`). Que el destino ya exista **NO** es un
`CONFLICT`: la acción `Crear` de la tabla se refiere al estado previo a la iniciativa.

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/domains/AsistenteNuevoExpediente.xml` | Crear | k-sistemas (modelos.md) | Modelo de pantalla del asistente, `persistable="false"` |

## Notas del diseño

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.
> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

## Paso 1 del diseño (verbatim)

### Paso 1 — Modelo de dominio `AsistenteNuevoExpediente`

Fichero: `design/domains/AsistenteNuevoExpediente.xml` → `src/main/java/com/educaflow/system/ventanilla/domains/AsistenteNuevoExpediente.xml`.

**Resumen estructural:** una sola entidad, `AsistenteNuevoExpediente`, con `persistable="false"` y `<module name="ventanilla" package="com.educaflow.system.ventanilla.db"/>`. Once campos en cuatro grupos:

| Grupo | Campos | Origen |
|---|---|---|
| Elección del usuario | `centro` (m2o `Centro`), `tramite` (m2o `Tramite`), `presentadoEnPapel` (boolean **nullable**), `presentadoEnRepresentacion` (boolean **nullable**) | `cliente` |
| Instantánea del trámite | `nombreTramite` (string `transient`), `ayudaTramite` (string `large` `transient`) | `servidor` (derivados de solo lectura) |
| Candidatos de cada paso | `centrosDisponibles` (m2m `Centro`), `tramitesDisponibles` (m2m `Tramite`), `hayQueElegirCentro` (boolean) | `servidor` |
| Qué preguntas se hacen | `hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien` (boolean) | `servidor` |

- Los nombres `tramite`, `centro`, `presentadoEnPapel` y `presentadoEnRepresentacion` son **literales obligatorios**: `TramitadorController.triggerInitialEvent` —al que llama la acción `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`— los lee del formulario por esos nombres.
- `presentadoEnPapel` es **nullable**: el interruptor booleano del sistema antiguo siempre vale `false` y no distingue «no contestado» de «lo presento yo mismo», que es justo lo que el asistente necesita (`VAL-AsistenteNuevoExpediente-003`).
- **`nombreTramite` y `ayudaTramite` son campos DERIVADOS, no campos que una regla asigne.** `CC-AsistenteNuevoExpediente-001` y `-002` son de `momento: lectura` y `sobreescribible: nunca`, y ese es exactamente el mapeo «campo derivado de solo lectura» de la conversión spec → V/R/U: se declaran `transient="true"` con cuerpo (el generador emite `computeX()` y el getter hace `x = computeX(); return x;`), así que el getter los recalcula siempre desde `tramite` y ninguna acción del servicio los asigna ni los acepta del cliente. No necesitan ninguna `R-` que los escriba. La garantía **no** es «no tienen setter» (`Property.createSetterMethod()` lo genera igual), sino que el getter recalcula y que los dos campos están fuera de **todas** las `AllowProperties`. El cuerpo lee el **campo** `tramite`, nunca `getTramite()` (`Mapper.findComputeDependencies` solo registra las dependencias que ve como `GETFIELD`); mismo motivo documentado en `subsystem/sistemaeducativo/domains/Grado.xml`.
- Ningún campo lleva `required="true"`: los que rellena el servidor no pueden llevarlo (`k-sistemas/modelos.md`) y los dos que contesta el usuario se exigen en el servidor, no con Bean Validation.
- **`hayQuePreguntarPresentacion` y `hayQuePreguntarParaQuien` son el oráculo que la vista consulta** (D2 de `decisiones.md`): el servidor publica la decisión «¿hay que preguntar esto?» ya tomada, no las mitades con las que se toma, así que la vista nunca compone una condición y la clasificación tiene un único dueño. Son los mismos dos nombres que ya usa `system/expedientes/domains/NuevoExpediente.xml`. Semántica: `true` → se pregunta y la respuesta llega sin marcar; `false` → no se pregunta, el servidor fija el único valor posible (o ninguno si no hay ninguno) y el campo no se muestra.
- **`hayQueElegirCentro` lo calcula el servidor en el paso 1 y de ahí en adelante lo transporta la vista.** Los pasos 2 y 3 lo reciben en el `<context>` de su `<action-view>` y lo fijan en su registro con el `<action-record>` del `onNew`; ninguna acción del servidor vuelve a calcularlo ni lo lee (está fuera de **todas** las `AllowProperties`), así que manipularlo solo cambia el rótulo del botón de salida del paso 2, nunca lo que el servidor ofrece o acepta.
- **Dos campos tienen doble clasificación, y es deliberado.** No hay ambigüedad porque las whitelists son **por acción**: en la acción (o en la rama) en la que el servidor dicta el valor, el cliente no puede tocarlo.
  - **`centro`** es `cliente` en `prepararTramites`, `recalcular` y `validateCrear` (es lo que el usuario eligió en el paso 1, y está en la línea `Input AllowProperties` de «Crear expediente»), y `servidor` en `prepararCentros`, que es la **única** acción que lo decide (lo fija su regla `fireActionRule_AsignarArranqueDelAsistente` cuando el usuario tiene un único centro candidato). `prepararCentros` es deny-all, así que ahí el cliente no lo puede dictar.
  - **`presentadoEnPapel`** es `cliente` en `recalcular` y en `validateCrear` —es la respuesta del usuario a «¿Cómo se presenta?» y está en las dos whitelists, como dicen las tablas de «Frontera de confianza»— y `servidor` **solo en la rama en la que la pregunta no se hace**: ahí `asignarFormaDePresentar` lo sobrescribe **incondicionalmente** con la única forma posible, así que lo que mandara el cliente se descarta. La condición que separa las dos ramas mira si la pregunta se hace, **nunca** si el campo llegó a null: no es la guarda de nulidad que `k-secure-coding` §3.3 prohíbe.

**Verificar al final:** `./gradlew compileJava` genera `com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente` con los once getters.


## Trazabilidad — Campos calculados (verbatim)

### Campos calculados

| CC del spec | Momento / sobreescribible | Ubicación |
|---|---|---|
| CC-AsistenteNuevoExpediente-001 (nombre del trámite) | lectura / nunca | campo derivado `nombreTramite` (`transient="true"` con cuerpo) de `domains/AsistenteNuevoExpediente.xml`: el getter devuelve `I18n.get("value:" + tramite.getName())` —la clave real con la que el build registra el nombre de un trámite, la misma que usa `Tramites.xml` con `_t("value:"+name)`—, o null sin trámite. Fuera de todas las `AllowProperties`. |
| CC-AsistenteNuevoExpediente-002 (ayuda del trámite) | lectura / nunca | campo derivado `ayudaTramite` (`transient="true"` con cuerpo) de `domains/AsistenteNuevoExpediente.xml`: el getter devuelve `tramite.getHelp()`, o null sin trámite. Fuera de todas las `AllowProperties`. |
