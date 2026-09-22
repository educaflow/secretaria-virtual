---
type: implementation-task
template: system
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas
- k-skill

Aplica la **enmienda normativa de vistas** que el diseño declara en «Cambios necesarios fuera del sistema»,
**punto 3**: el estrechamiento de `VAR-7.2` (la rama del asistente) y su reflejo en prosa. Son dos ficheros
`Modificar` que van en **una sola tarea** porque llevan el **mismo criterio literal** y `CLAUDE.md` exige
mantenerlos coherentes entre sí:

- `agent_docs/view-rules.md` — `VAR-7.2`: bloque **Decisión**, celda `btnCancel`/**maestro** de la tabla de
  **Verificación** y línea **Incorrecto** ❌.
- `.claude/skills/k-vistas/forms.md` — la misma conjunción en la tabla comparativa «form principal vs form
  modal» (fila «Botón Cancelar acción») y en la lista **IMPORTANTE** de «Plantilla básica de un formulario».

**CRITICAL — leer antes de editar:** los tres ficheros normativos y las tres clases de test de vistas **ya
llevan aplicada una versión anterior de esta enmienda**, cuyo discriminador era solo «el maestro no declara
`btnSave`». El texto del diseño describe el **estado final** que deben tener, no un delta contra el texto
original: **MUST** partirse del texto que hay **hoy** en el árbol. Lo del `<grid action="…">` **ya está
hecho** y **MUST NOT** volver a escribirse.

Después de editar el markdown, **MUST** re-ejecutarse `/developer-create-view-tests` para reproyectar los
tests: `agent_docs/view-rules.md` es su **fuente de verdad** y **MUST NOT** editarse a mano ningún `.java` de
`src/test/java/com/educaflow/views`.

**Criterio de aceptación**: la suite de vistas queda **entera en verde** — los tres forms del asistente pasan
con `close` y los tres forms de `correos`/`importacion` siguen pasando con `back`, sin tocarlos. **MUST NOT**
migrarse esas tres vistas a `close` ni añadirse `system/ventanilla` a los «Paquetes exentos» de
`agent_docs/view-rules.md`.

**ALCANCE**: de «Cambios necesarios fuera del sistema» esta tarea aplica **solo el punto 3**. El punto 1
(fusionar `menus.xml`) es de la **tarea 09** y el punto 2 (referenciar el permiso en
`src/main/resources/data-init/input/auth.xml`) es de la **tarea 10**: **MUST NOT** hacerse aquí, aunque el
texto del diseño copiado más abajo los enumere.

Al modificar un `SKILL.md` o cualquier fichero de `.claude/skills/`, **MUST** aplicarse el skill `k-skill`
(reglas, frontmatter y convenciones de redacción de los skills del proyecto).

## Filas de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `agent_docs/view-rules.md` | Modificar | — | `VAR-7.2`: rama «maestro sin `btnSave` **y** cuyo `action-view` no declara `grid`: su `btnCancel` contiene `close`» (ver «Cambios necesarios fuera del sistema») |
| `.claude/skills/k-vistas/forms.md` | Modificar | k-vistas | Reflejo de la rama de `VAR-7.2`: el `btnCancel` de un form principal que no persiste (sin `btnSave`) **y** cuyo `action-view` no declara ninguna `<view type="grid">` cierra con `close`, no con `back` (ver «Cambios necesarios fuera del sistema») |

## «Cambios necesarios fuera del sistema» (verbatim)

## Cambios necesarios fuera del sistema

Los dos primeros son ficheros de datos del proyecto; el **tercero** es **normativo** y se declara aquí porque el diseño usa una pieza real del proyecto que su documentación todavía no recoge: cómo cancela un formulario maestro que ni persiste ni tiene `grid` al que volver (D8 de `decisiones.md`). El `<grid action="…">` que usan los pasos 1 y 2 (capacidad del fork de AOP, D4) **no** genera ningún cambio normativo: `agent_docs/view-rules.md` y `.claude/skills/k-vistas/grids.md` **ya** lo recogen (ver la nota «Estado del árbol al empezar» del punto 3). **MUST NOT** aplicarlos el diseñador: los aplica `/sdd-implementer`.

1. **`src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`** — fusionar la porción de `design/menus.xml`.
2. **`src/main/resources/data-init/input/auth.xml`** — añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users` (solo la referencia, sin `<can>`).
3. **`agent_docs/view-rules.md` (`VAR-7.2`) + `.claude/skills/k-vistas/forms.md`** — añadir a la secuencia del botón estándar `btnCancel` la rama del **asistente**: el maestro que **ni persiste ni tiene `grid` al que volver**. Los pasos 1 y 2 son forms de clase **maestro** cuyo botón de salida se llama `btnCancelar` (el nombre correcto de un botón «Cancelar»; ver Paso 8) y contiene `close`, mientras `VAR-7.2` exige hoy `back` en el maestro. `back` hace **dos** cosas, y la rama nueva existe solo cuando ninguna de las dos tiene sentido: pregunta por los cambios pendientes —y un maestro sin `btnSave` no tiene cambios que perder— y vuelve del `form` al `grid` **de la misma pestaña** —y un maestro cuyo `<action-view>` declara **solo** `<view type="form">` no tiene `grid` al que volver—. Por eso el discriminador de la rama es **la conjunción de las dos condiciones**, nunca la primera sola:

   > **Rama del asistente (criterio literal, el mismo en los dos ficheros y en el test):** el form es de clase **maestro**, **no** declara ningún `<button>` cuyo `name` empiece por `btnSave`, **y** ningún `<action-view>` que lo abra (el que lo referencia con una `<view type="form">` con su `name`) declara una `<view type="grid">`. Cuando se cumplen **las dos**, su `btnCancel` **MUST** contener `close`; si falla cualquiera de las dos, sigue rigiendo `back`.

   Con ese criterio, `close` es además lo único que funciona: ESC-006 y ESC-008 exigen que el asistente **se cierre**, y `back` no cierra una pestaña. `system/ventanilla` **no** está en los paquetes exentos, así que sin esta enmienda **el test falla y `./run.sh` no pasa**. La enmienda es **estrecha**: no toca las demás filas ni las demás clases de form, y **ninguna vista ajena al asistente cambia de comportamiento**.

   **Qué vistas quedan dentro y fuera de la rama nueva** (parte del contrato para `/sdd-implementer`; **MUST** verificarse tras regenerar los tests):

   | Vista | ¿`btnSave`? | ¿su `action-view` declara `grid`? | Rama | Acción del `btnCancel` |
   |---|---|---|---|---|
   | `src/main/java/com/educaflow/system/ventanilla/views/ElegirCentro-AsistenteNuevoExpediente.xml` (`sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-form > btnCancelar`) | no | no (solo `<view type="form">`) | **dentro** | `close` |
   | `src/main/java/com/educaflow/system/ventanilla/views/ElegirTramite-AsistenteNuevoExpediente.xml` (`sysVentanilla.ElegirTramite@AsistenteNuevoExpediente-form > btnCancelar`) | no | no (solo `<view type="form">`) | **dentro** | `close` |
   | `src/main/java/com/educaflow/system/ventanilla/views/Main-AsistenteNuevoExpediente.xml` (`sysVentanilla.Main@AsistenteNuevoExpediente-form`) | no | no (solo `<view type="form">`) | **dentro** (sin efecto: no declara ningún `btnCancel*`; sus botones son `btnAtras` y `btnCrear`) | — |
   | `src/main/java/com/educaflow/subsystem/correos/views/Centro-Correo.xml` (`subsysCorreos.Centro@Correo-form > btnCancel`) | no | **sí** (`<view type="grid">` + `<view type="form">`) | **fuera** | `back` — **no se toca** |
   | `src/main/java/com/educaflow/subsystem/correos/views/Mis-Correo.xml` (`subsysCorreos.Mis@Correo-form > btnCancel`) | no | **sí** (`<view type="grid">` + `<view type="form">`) | **fuera** | `back` — **no se toca** |
   | `src/main/java/com/educaflow/subsystem/importacion/views/Main-TareaImportacion.xml` (`subsysImportacion.Main@TareaImportacion-form > btnCancelar`) | no | **sí** (`<view type="grid">` + `<view type="form">`) | **fuera** | `back` — **no se toca** |

   Las tres últimas son la razón exacta de la conjunción: son maestros **sin** `btnSave` (no persisten) pero cuyo `action-view` declara `grid` + `form`, así que su «Salir/Cancelar» sí tiene a dónde volver y hoy son correctas. **MUST NOT** migrarlas a `close`: cambiaría su comportamiento (hoy vuelven al listado en la misma pestaña; con `close` se cerraría la pestaña entera) y queda **fuera del alcance** de esta iniciativa. **MUST NOT**, tampoco, añadir `system/ventanilla` a los **«Paquetes exentos»** de `agent_docs/view-rules.md`: vía **descartada** — exime al sistema de **todas** las reglas de vistas, no solo de esta, y la regla dejaría de verificar el asistente entero.

   > **Estado del árbol al empezar (leer antes de editar):** `agent_docs/view-rules.md`, `.claude/skills/k-vistas/forms.md`, `.claude/skills/k-vistas/grids.md` y las tres clases de test `src/test/java/com/educaflow/views/{botones/Categoria7BotonesTest.java, grids/Categoria8GridsTest.java, integridad/Categoria4IntegridadTest.java}` **ya llevan aplicada una versión anterior de esta enmienda**, cuyo discriminador era solo «el maestro no declara `btnSave`» — el que rompe las tres vistas de `correos`/`importacion`. Lo que sigue describe el **estado final** que deben tener esos ficheros, no un delta contra el texto original: **MUST** partirse del texto que hay **hoy** en el árbol.
   >
   > **Lo del `<grid action="…">` ya está hecho, NO se vuelve a escribir:** `k-vistas/grids.md` ya tiene su sección «Clic sobre la fila: `action` y `actionSignal`» (con la tabla de atributos y los ✅/❌), y `agent_docs/view-rules.md` ya lleva la fila «`action` de un `<grid>`» en la tabla de `VAR-4.1`, la cita de esa referencia en el ✅ de `VAR-4.2` y la exención «grids con `action`» en `VAR-8.1`. **MUST NOT** añadirse una segunda copia de esas reglas (una regla con dos dueños dentro del mismo fichero) ni gastarse una re-ejecución de `/developer-create-view-tests` por ellas: lo **único** pendiente fuera del sistema es el estrechamiento de `VAR-7.2` que describe este punto.

   Dos ediciones, con el **mismo criterio literal** en los dos sitios para que sigan siendo coherentes:
   - `agent_docs/view-rules.md`, **`VAR-7.2`**:
     - bloque **Decisión** — donde hoy dice «…un maestro **sin `save`** (form de asistente que no persiste) no tiene cambios que perder ni `grid` al que volver, así que su `btnCancel` **MUST** contener `close`», debe quedar: «…un maestro **sin `save`** cuyo `<action-view>` **no** declara ninguna `<view type="grid">` (un asistente: ni persiste nada ni tiene `grid` al que volver) **MUST** contener `close` en su `btnCancel`; un maestro sin `save` cuyo `<action-view>` **sí** declara un `grid` sigue con `back`, porque el `grid` al que volver existe aunque no se guarde nada».
     - tabla de **Verificación**, celda `btnCancel` / **maestro** — donde hoy dice «contiene `back`; si el form maestro **no** declara `btnSave` (no persiste), contiene `close`», debe quedar: «contiene `back`; si el form maestro **no** declara `btnSave` **y** ningún `<action-view>` que lo abra (una `<view type="form">` con su `name`) declara una `<view type="grid">`, contiene `close`».
     - línea **Incorrecto** ❌ — donde hoy dice «o un `btnCancel` con `close` en un maestro **con** `btnSave`», debe quedar: «o un `btnCancel` con `close` en un maestro que no cumple las **dos** condiciones de la rama del asistente (tiene `btnSave`, o su `action-view` declara una `<view type="grid">`); o un `btnCancel` con `back` en un maestro sin `btnSave` cuyo `action-view` declara solo `<view type="form">`».
   - `.claude/skills/k-vistas/forms.md`, con la **misma** conjunción:
     - § **«Form modal»**, **tabla comparativa** «form principal vs form modal», fila **«Botón Cancelar acción»** — donde hoy dice «`back` (o `close` si el form principal no persiste: sin `btnSave`, como un asistente)», debe quedar: «`back` (o `close` **solo** si el form principal no persiste —sin `btnSave`— **y** su `action-view` no declara ninguna `<view type="grid">`: un asistente, que tampoco tiene `grid` al que volver)».
     - lista **IMPORTANTE** de § «Plantilla básica de un formulario» — donde hoy dice «`btnCancel` = `back`, o `close` si el form principal no lleva `btnSave` (no persiste nada: no hay cambios que perder ni `grid` al que volver)», debe quedar: «`btnCancel` = `back`, o `close` **solo** si el form principal no lleva `btnSave` **y** su `action-view` no declara ninguna `<view type="grid">` (un asistente: ni cambios que perder ni `grid` al que volver)».

   Después, re-ejecutar `/developer-create-view-tests` para reproyectar los tests: `agent_docs/view-rules.md` es su **fuente de verdad**, así que el `Categoria7BotonesTest` (que hoy discrimina solo por `btnSave`) se corrige **editando el markdown y regenerando**, **MUST NOT** editarse el `.java` a mano. Criterio de aceptación: la suite de vistas queda **entera en verde** —los tres forms del asistente pasan con `close` y los tres forms de `correos`/`importacion` siguen pasando con `back`, sin tocarlos—. Es el conocimiento que produjo el choque resuelto en **D8** y que hoy solo vive en `.sdd/`, material de trabajo que `CLAUDE.md` prohíbe usar como documentación del código; sin la enmienda, el siguiente asistente repite la arqueología entera o acaba poniéndole al botón un nombre que no es el suyo.


## Paso 8 del diseño — el botón de salida de los pasos 1 y 2 (verbatim)

**Ninguno de los tres formularios guarda nada:** el modelo no se persiste nunca (`persistable="false"`) y el spec lo declara explícitamente para el paso 3 («este formulario no lleva “Guardar”, “Cancelar” ni “Borrar” (la ficha no se guarda nunca)», `screen-nuevo-expediente.md`), así que en los tres no hay `btnSave`, ni `btnDelete`, ni `canBackOnSave`, ni `remote-validationSave-action`, ni `save` → `force-back`. Los cinco botones que sí existen (`btnCancelar` ×2, `btnAtras` ×2 y `btnCrear`) navegan o cierran con `close`, que es lo único que funciona en estas vistas: `back` solo conmuta entre el `grid` y el `form` de una misma pestaña, y el `<action-view>` de cada uno de los tres ficheros declara **solo** `<view type="form">`, sin ninguna `<view type="grid">`, así que no hay `grid` al que volver. Los `btnCancelar` son los de los pasos 1 y 2 —la salida del asistente, que el spec sí pide ahí— y no la «Cancelar» de la botonera de mantenimiento, que no existe en ninguno de los tres.

El botón de salida de los pasos 1 y 2 se llama **`btnCancelar`** —rótulo «Cancelar», el que comprueban los `ESC-`/`T-`— y su `action-group`, `…-btnCancelar-action` como exige `VAR-7.1`, contiene **`close`**, igual que los otros cuatro botones: ninguno de los tres forms persiste (ninguno declara `btnSave`) y ningún `<action-view>` de los tres declara una `<view type="grid">`, así que no hay ni cambios que perder ni `grid` al que volver. El criterio literal de la rama de `VAR-7.2` que eso obliga a declarar, el texto final de cada fichero normativo y qué vistas quedan dentro y fuera están en el **punto 3** de «Cambios necesarios fuera del sistema»; el choque y las alternativas, en **D8** de `decisiones.md`.

**Verificar al final:** la aplicación arranca sin errores de carga de vistas y `grep -n "canBackOnSave\|remote-validation" src/main/java/com/educaflow/system/ventanilla/views/*.xml` no devuelve nada.

