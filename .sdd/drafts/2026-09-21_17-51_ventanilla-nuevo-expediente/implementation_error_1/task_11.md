---
type: implementation-task
template: system
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas
- k-skill

Aplica las **enmiendas normativas de vistas** que el diseño declara en «Cambios necesarios fuera del sistema»
(puntos 3, 4 y 5). Son tres ficheros `Modificar` que van en **una sola tarea** porque son un único componente
lógico —el catálogo de reglas de vistas y sus dos reflejos en prosa, que `CLAUDE.md` exige mantener
coherentes— y porque `agent_docs/view-rules.md` recibe las **dos** enmiendas (`VAR-4.1`/`VAR-4.2` y `VAR-7.2`)
y **MUST** estar cubierto por exactamente una tarea:

- `agent_docs/view-rules.md` — `VAR-4.1`/`VAR-4.2` (referencia `grid/@action`) **y** `VAR-7.2` (rama del
  maestro sin `save`), más la exención «grids con `action`» en la regla de `canEditOnClick`/`canViewOnClick`.
- `.claude/skills/k-vistas/grids.md` — los atributos `action`/`actionSignal` del `<grid>`.
- `.claude/skills/k-vistas/forms.md` — el `btnCancel` de un form principal que no persiste.

Las tres son **ediciones quirúrgicas sobre ficheros que ya existen**: **MUST NOT** reescribirse ni
regenerarse ninguno; se conserva íntegro el resto de su contenido y la enmienda es **estrecha** (no toca las
demás filas ni las demás clases de form).

**Al terminar las tres ediciones MUST re-ejecutarse `/developer-create-view-tests`** para reproyectar los
tests de vistas desde el markdown (los `.java` de `src/test/java/com/educaflow/views` **MUST NOT** editarse a
mano). Sin esta tarea, los tests de vistas fallan con las vistas de las tareas 05–07 y `./run.sh` no pasa.

## Filas de la tabla «Ficheros a crear o modificar»


| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `agent_docs/view-rules.md` | Modificar | — | `VAR-4.1`/`VAR-4.2`: admitir `grid/@action` como referencia a una acción; `VAR-7.2`: rama «maestro sin `save`: su `btnCancel` contiene `close`» (ver «Cambios necesarios fuera del sistema») |
| `.claude/skills/k-vistas/grids.md` | Modificar | k-vistas | Documentar los atributos `action`/`actionSignal` del `<grid>` (coherencia con `view-rules.md`) |
| `.claude/skills/k-vistas/forms.md` | Modificar | k-vistas | Reflejo de la rama de `VAR-7.2`: el `btnCancel` de un form principal que no persiste (sin `btnSave`) cierra con `close`, no con `back` (ver «Cambios necesarios fuera del sistema») |

## Texto del diseño — el párrafo del Paso 8 que origina la enmienda de `VAR-7.2`

**Ninguno de los tres formularios guarda nada:** el modelo no se persiste nunca (`persistable="false"`) y el spec lo declara explícitamente para el paso 3 («este formulario no lleva “Guardar”, “Cancelar” ni “Borrar” (la ficha no se guarda nunca)», `screen-nuevo-expediente.md`), así que en los tres no hay `btnSave`, ni `btnDelete`, ni `canBackOnSave`, ni `remote-validationSave-action`, ni `save` → `force-back`. Los cinco botones que sí existen (`btnCancelar` ×2, `btnAtras` ×2 y `btnCrear`) navegan o cierran con `close`, que es lo único que funciona en estas vistas: `back` solo conmuta entre el `grid` y el `form` de una misma pestaña, y estas vistas solo declaran un `form`. Los `btnCancelar` son los de los pasos 1 y 2 —la salida del asistente, que el spec sí pide ahí— y no la «Cancelar» de la botonera de mantenimiento, que no existe en ninguno de los tres.

El botón de salida de los pasos 1 y 2 se llama **`btnCancelar`**, con el rótulo «Cancelar» que comprueban los `ESC-`/`T-`, y su `action-group` contiene **`close`**. Como su `name` empieza por `btnCancel`, es un **botón estándar** para `agent_docs/view-rules.md` `VAR-7.2` (sujeto: los botones cuyo `name` empieza por `btnSave`/`btnDelete`/`btnCancel`), que hoy exige `back` en un form de clase **maestro**. Aquí `back` no vale: ESC-006 y ESC-008 exigen que el asistente **se cierre**, y `back` no cierra una pestaña. Y la razón es estructural, no propia de este sistema: un form maestro **sin `save`**, que no persiste nada, no tiene a dónde «volver» ni cambios que avisar de que se pierden, así que su cancelación es un cierre. Por eso el botón **no** se renombra —el nombre correcto de un botón «Cancelar» es `btnCancelar`— y lo que se declara es una **enmienda estrecha de `VAR-7.2`** y de su reflejo en `k-vistas/forms.md`: la rama «maestro sin `save` (form de asistente que no persiste): su `btnCancel` contiene `close`, no `back`», en el **punto 5** de «Cambios necesarios fuera del sistema». Sigue cumpliendo `VAR-7.1`: su `action-group` es `…-btnCancelar-action`. Ver **D8** de `decisiones.md`.

**Verificar al final:** la aplicación arranca sin errores de carga de vistas y `grep -n "canBackOnSave\|remote-validation" src/main/java/com/educaflow/system/ventanilla/views/*.xml` no devuelve nada.

## Texto del diseño — «Cambios necesarios fuera del sistema», puntos 3, 4 y 5

Los dos primeros son ficheros de datos del proyecto; los **cuatro últimos** son **normativos** y se declaran aquí porque el diseño usa piezas y alcances reales del proyecto que su documentación todavía no recoge: una capacidad del fork de AOP (D4 de `decisiones.md`), cómo cancela un formulario maestro que no persiste (D8) y el alcance de la excepción multicentro de los expedientes. **MUST NOT** aplicarlos el diseñador: los aplica `/sdd-implementer`.

3. **`agent_docs/view-rules.md`** — `VAR-4.1`: añadir a la tabla de referencias la fila «`action` de `<grid>` → cualquier acción declarada»; `VAR-4.2`: aceptar esa misma referencia como origen válido para que un `action-group` invocado desde un `<grid action="…">` no se cuente como huérfano; y, en la regla que exige que todo `<grid>` declare exactamente uno de `canEditOnClick`/`canViewOnClick`, añadir la **exención «grids con `action`»**, porque en ellos `handleCellClick` consulta `action` antes y cualquiera de los dos sería una declaración muerta. Después, re-ejecutar `/developer-create-view-tests` para reproyectar los tests.
4. **`.claude/skills/k-vistas/grids.md`** — documentar los atributos `action` y `actionSignal` del `<grid>` (extensión del fork: un clic simple sobre la fila ejecuta la acción con el registro en el contexto; `handleCellClick` los consulta antes que `canViewOnClick`/`canEditOnClick`) y, en el mismo párrafo, la excepción de la plantilla del `<grid>`: **un grid con `action` no declara `canViewOnClick` ni `canEditOnClick`**. Así el skill y `view-rules.md` siguen siendo coherentes.

5. **`agent_docs/view-rules.md` (`VAR-7.2`) + `.claude/skills/k-vistas/forms.md`** — añadir a la secuencia del botón estándar `btnCancel` la rama del **maestro que no persiste**. Los pasos 1 y 2 son forms de clase **maestro** cuyo botón de salida se llama `btnCancelar` (el nombre correcto de un botón «Cancelar»; ver Paso 8) y contiene `close`, mientras `VAR-7.2` exige hoy `back` en el maestro. `back` pregunta por los cambios pendientes y vuelve del `form` al `grid` de la misma pestaña; un maestro **sin `save`**, que no guarda nada, no tiene ni cambios que perder ni `grid` al que volver, así que su cancelación solo puede ser un cierre (ESC-006 y ESC-008 exigen que el asistente se cierre). `system/ventanilla` **no** está en los paquetes exentos, así que sin esta enmienda **el test falla y `./run.sh` no pasa**. La enmienda es **estrecha**: no toca las demás filas ni las demás clases de form. Dos ediciones, con el mismo criterio en los dos sitios para que sigan siendo coherentes:
   - `agent_docs/view-rules.md`, **`VAR-7.2`**: en el bloque **Decisión**, añadir tras la frase del cierre del maestro «`back` es el cierre del `btnCancel` de un maestro **que guarda**, donde preguntar por los cambios sí es lo correcto; un maestro **sin `save`** (form de asistente que no persiste) no tiene cambios que perder ni `grid` al que volver, así que su `btnCancel` **MUST** contener `close`»; en la tabla de **Verificación**, sustituir la celda `btnCancel` / maestro por «contiene `back`; si el form maestro **no** declara `btnSave` (no persiste), contiene `close`»; y en la línea **Incorrecto ❌**, acotar «o un `btnCancel` con `close`» a «o un `btnCancel` con `close` en un maestro **con** `btnSave`».
   - `.claude/skills/k-vistas/forms.md`, § **«Form modal»**, **tabla comparativa** «form principal vs form modal», fila **«Botón Cancelar acción»**: cambiar la celda del form principal de «`back`» a «`back` (o `close` si el form principal no persiste: sin `btnSave`, como un asistente)», y, para que el skill no se contradiga a sí mismo, la misma acotación en la línea «✅ CORRECTO (principal): … `btnCancel` = `back`» de la lista **IMPORTANTE** de § «Plantilla básica de un formulario» (`btnCancel` = `back`, o `close` si el form principal no lleva `btnSave`).

   Después, re-ejecutar `/developer-create-view-tests` para reproyectar los tests. Es el conocimiento que produjo el choque resuelto en **D8** y que hoy solo vive en `.sdd/`, material de trabajo que `CLAUDE.md` prohíbe usar como documentación del código; sin la enmienda, el siguiente asistente repite la arqueología entera o acaba poniéndole al botón un nombre que no es el suyo.
