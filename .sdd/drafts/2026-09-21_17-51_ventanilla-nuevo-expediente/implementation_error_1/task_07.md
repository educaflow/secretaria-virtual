---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la vista del paso 3 (contexto del trámite y creación) del asistente.

El fichero **ya está materializado y validado con `xmllint`** por el diseñador en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/views/Main-AsistenteNuevoExpediente.xml`.
**MUST** copiarse **literalmente** (byte a byte) a su ruta destino
`src/main/java/com/educaflow/system/ventanilla/views/Main-AsistenteNuevoExpediente.xml`.
**MUST NOT** regenerarlo, reescribirlo ni modificarlo. La acción es `Crear`: la ruta destino no existe todavía.

## Fila de la tabla «Ficheros a crear o modificar»


| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/views/Main-AsistenteNuevoExpediente.xml` | Crear | k-vistas (forms.md, actions.md) | Paso 3: contexto del trámite y creación |

## Nota del diseño sobre los XML ya materializados

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.
>
> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

---

## Cómo se encadenan los tres pasos (leer antes de implementar)

El asistente son **tres vistas** sobre el **mismo** modelo de pantalla, cada una con su propio `onNew` que la rellena desde el contexto que recibe:

```
menú «Ventanilla → Nuevo expediente»
  └─ sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action        (pestaña)
        ├─ 0 centros candidatos  → aviso y se cierra
        ├─ 1 centro candidato    → abre el paso 2 con ese centro y se cierra
        └─ 2 o más               → se queda y muestra el listado de centros
              └─ clic en una fila → sysVentanilla.ElegirTramite@…-action  (pestaña) y se cierra
                    └─ clic en un trámite → sysVentanilla.Main@…-action   (emergente) y se cierra
                          ├─ «Atrás»            → reabre el paso 2 con el mismo centro y se cierra
                          └─ «Crear expediente» → sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action
                                                   (→ TramitadorController.triggerInitialEvent)
```

Tres reglas, y no hay más que recordar:

1. **Cada paso abre el siguiente y se cierra a sí mismo**: un `<action-group>` con el `<action-view>` del siguiente y `close` al final (el framework exige que `close` sea la última acción del grupo). Así nunca hay dos pasos abiertos a la vez y «Crear expediente» deja la pantalla limpia.
2. **Los pasos 1 y 2 son pestañas; el paso 3 es la única ventana emergente.** No es decorativo: una acción disparada desde una **fila de un listado embebido** no puede cerrar una ventana emergente (`DefaultActionExecutor.#closeView()` se rinde si hay emergentes y el `FormActionHandler` del listado no tiene `closeHandler`), pero sí puede cerrar una pestaña. Los dos saltos que salen de una fila son precisamente 1→2 y 2→3. El paso 3 solo dispara acciones desde sus propios botones, donde `close` y `setCanClose` funcionan; ver **D1** de `decisiones.md`.
3. **El centro y `hayQueElegirCentro` viajan en el propio registro; al saltar desde una fila se leen con `__parent__`.** El `<context>` del paso 2 resuelve el centro siempre igual, `centro?.id ?: id`: el centro del registro actual y, si no lo hay, el de la fila pulsada (único caso en que el registro aún no lo tiene, y en el paso 1 la fila pulsada **es** el `Centro`). `hayQueElegirCentro` acompaña al centro por el mismo `<context>`, con la misma forma (`__parent__` cuando el salto sale de una fila): lo calculó el paso 1 y los pasos 2 y 3 solo lo arrastran. El paso 3 recibe `_tramiteId` de la fila pulsada (`eval: id`) y `_centroId` del **formulario** del paso 2, con `eval: __parent__?.centro?.id`: una acción disparada desde una fila de un listado embebido recibe el contexto de la fila (`handleRowDoubleClickActionId`, `axelor-front/src/views/grid/builder/grid.tsx`: `{...processContextValues(record), selected, _signal, id}`) fundido sobre `getActionContext()` del `panel-related` (`one-to-many.tsx`), que mete el contexto del formulario padre **bajo `_parent`**, no en el primer nivel. Como la fila es un `Tramite` y no tiene `centro`, `eval: centro?.id` daría null; `__parent__` resuelve a `Context.getParent()` (`axelor-core/.../ScriptBindings.java`) desde `_parent._model` y es la forma que el proyecto ya usa (`system/gestioncentro/views/gestion-centro-usuarios-autorizados.xml`).

---

## Texto del diseño — Paso 8 (Vistas)

### Paso 8 — Vistas

#### `views/Main-AsistenteNuevoExpediente.xml` — paso 3

Un solo bloque, `sysVentanilla.Main@AsistenteNuevoExpediente`: `<action-view>` **emergente** con `_centroId`, `_hayQueElegirCentro` y `_tramiteId`; `<form>` con el panel «Trámite» (nombre, centro y ayuda, todos en solo lectura, y **al final** los cuatro campos ocultos: `tramite`, los dos `hayQuePreguntar*` y `hayQueElegirCentro`), el panel «Presentación» (las dos preguntas) y la botonera; los `action-group` de los dos botones, del `onNew` y del `onChange` de «¿Cómo se presenta?»; el `<action-validate>` local de las dos preguntas obligatorias; el `<action-record>` que fija trámite, centro y `hayQueElegirCentro` desde el contexto; y los tres `<action-method>` (`recalcular`, `validateCrear` y `triggerInitialEvent`).

El último, `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`, es la acción de alta del `btnCrear`: **acción propia del sistema** —declarada en la sección `<?sv-remotes?>` de este fichero, con el contexto derivado de su ubicación (`VAR-2.1`), el marcador `Remote-` propio de una remota (`VAR-2.2`) y el nombre `-Remote-triggerInitialEvent-action` que coincide con el `method="triggerInitialEvent"` de su `<call>` (`VAR-2.4`)— que apunta al **controlador del tramitador reutilizado sin modificar**, `com.educaflow.subsystem.tramitador.controller.TramitadorController#triggerInitialEvent`. Lo que este sistema aporta es solo el **nombre local** de la acción, con el que `VAR-4.1` resuelve contra una acción declarada en el ámbito de las vistas; el `actions-tramitador.xml` del tramitador **no se toca** (`system/expedientes/views/Main-NuevoExpediente.xml` sigue usando su acción y esa pantalla no se retira en esta iniciativa). El propio javadoc de `TramitadorController.triggerInitialEvent` documenta este patrón de acción por vista.

`hayQueElegirCentro` está aquí solo para devolverlo: el `btnAtras` reabre el paso 2 con el mismo `<context>`, así que el botón de salida de aquel recupera el rótulo que le corresponde sin que el servidor tenga que recalcular nada.

ASCII Layout:

```
tramitePanel — trámite CON ayuda
nnnnnncccccc   ← nombreTramite(6) + centro(6)          [los dos son nombres cortos y van juntos]
aaaaaaaaaaaa   ← ayudaTramite(12)                      [texto largo con HTML → fila propia]
············   ← tramite(6) + hayQuePreguntarPresentacion(6)   [ocultos]
············   ← hayQuePreguntarParaQuien(6) + hayQueElegirCentro(6)   [ocultos]

tramitePanel — trámite SIN ayuda
nnnnnncccccc   ← nombreTramite(6) + centro(6)
············   ← la fila de ayudaTramite, vacía: ocupa las 12 columnas él solo, así que su showIf
                 va en el propio campo y al ocultarse no desplaza nada
············   ← tramite(6) + hayQuePreguntarPresentacion(6)
············   ← hayQuePreguntarParaQuien(6) + hayQueElegirCentro(6)

presentacionPanel — se preguntan las dos
pppppppppppp   ← presentadoEnPapel(12)                 [boolean-radio con dos opciones largas]
rrrrrrrrrrrr   ← presentadoEnRepresentacion(12)        [boolean-radio con dos opciones largas]

presentacionPanel — solo «¿Cómo se presenta?»
pppppppppppp

presentacionPanel — solo «¿Para quién es el expediente?»
rrrrrrrrrrrr

presentacionPanel — ninguna de las dos: el panel entero no se ve

buttons-panel
.......aaccc   ← colOffset(7) + btnAtras(2) + btnCrear(3)
```

Cada pregunta ocupa su fila entera y es el único elemento de esa fila, así que su `showIf` va en el propio campo y no hace falta ningún panel anidado.

Los cuatro campos ocultos de `tramitePanel` van **al final**, detrás de `ayudaTramite`, por la misma razón que el oculto de `centroPanel` en el paso 2: un campo oculto **consume sus columnas igual** (`computeLayout` asigna `gridColumnStart/End` a todos los items y `GridItem` envuelve el widget en un div con ese `gridColumn` aunque no pinte nada), así que delante de los visibles partiría la fila «Trámite + Centro» en dos mitades desalineadas. Detrás, sus filas quedan vacías al final del panel y no desplazan nada.

**Ninguno de los tres formularios guarda nada:** el modelo no se persiste nunca (`persistable="false"`) y el spec lo declara explícitamente para el paso 3 («este formulario no lleva “Guardar”, “Cancelar” ni “Borrar” (la ficha no se guarda nunca)», `screen-nuevo-expediente.md`), así que en los tres no hay `btnSave`, ni `btnDelete`, ni `canBackOnSave`, ni `remote-validationSave-action`, ni `save` → `force-back`. Los cinco botones que sí existen (`btnCancelar` ×2, `btnAtras` ×2 y `btnCrear`) navegan o cierran con `close`, que es lo único que funciona en estas vistas: `back` solo conmuta entre el `grid` y el `form` de una misma pestaña, y estas vistas solo declaran un `form`. Los `btnCancelar` son los de los pasos 1 y 2 —la salida del asistente, que el spec sí pide ahí— y no la «Cancelar» de la botonera de mantenimiento, que no existe en ninguno de los tres.

El botón de salida de los pasos 1 y 2 se llama **`btnCancelar`**, con el rótulo «Cancelar» que comprueban los `ESC-`/`T-`, y su `action-group` contiene **`close`**. Como su `name` empieza por `btnCancel`, es un **botón estándar** para `agent_docs/view-rules.md` `VAR-7.2` (sujeto: los botones cuyo `name` empieza por `btnSave`/`btnDelete`/`btnCancel`), que hoy exige `back` en un form de clase **maestro**. Aquí `back` no vale: ESC-006 y ESC-008 exigen que el asistente **se cierre**, y `back` no cierra una pestaña. Y la razón es estructural, no propia de este sistema: un form maestro **sin `save`**, que no persiste nada, no tiene a dónde «volver» ni cambios que avisar de que se pierden, así que su cancelación es un cierre. Por eso el botón **no** se renombra —el nombre correcto de un botón «Cancelar» es `btnCancelar`— y lo que se declara es una **enmienda estrecha de `VAR-7.2`** y de su reflejo en `k-vistas/forms.md`: la rama «maestro sin `save` (form de asistente que no persiste): su `btnCancel` contiene `close`, no `back`», en el **punto 5** de «Cambios necesarios fuera del sistema». Sigue cumpliendo `VAR-7.1`: su `action-group` es `…-btnCancelar-action`. Ver **D8** de `decisiones.md`.

**Verificar al final:** la aplicación arranca sin errores de carga de vistas y `grep -n "canBackOnSave\|remote-validation" src/main/java/com/educaflow/system/ventanilla/views/*.xml` no devuelve nada.

## Trazabilidad — Reglas de UI

### Reglas de UI

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-001 | RUI-nuevo-expediente-listado-centros-001 | `ElegirCentro-AsistenteNuevoExpediente.xml`: `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-onNew-action` (las tres ramas) + `…-Local-avisoSinCentros-action` + `showIf="hayQueElegirCentro"` del panel `centrosPanel` |
| U-nuevo-expediente-002 | RUI-nuevo-expediente-arbol-tramites-001 | `ElegirTramite-…xml`: paneles `buttonsConEleccionDeCentro` / `buttonsSinEleccionDeCentro` con `showIf` excluyentes sobre `hayQueElegirCentro`, que llega del paso 1 en el `<context>` `_hayQueElegirCentro` y lo fija `…-set-centroYHayQueElegirCentro-recibidos-action` |
| U-nuevo-expediente-003 | RUI-nuevo-expediente-arbol-tramites-002 | `ElegirTramite-…xml`: `<field name="centro" readonly="true">` del panel `centroPanel`, encima del listado |
| U-nuevo-expediente-004 | RUI-nuevo-expediente-arbol-tramites-003 | `ElegirTramite-…xml`: `groupBy="tipoTramite.name"` + `orderBy="tipoTramite.name,name"` del grid `…@AsistenteNuevoExpediente.Tramite-grid` (misma clave para agrupar y ordenar; Axelor pinta los grupos abiertos) |
| U-nuevo-expediente-005 | RUI-nuevo-expediente-formulario-001 | `Main-…xml`: `…-set-tramiteCentroYHayQueElegirCentro-recibidos-action` desde `onNew` (fija trámite, centro y `hayQueElegirCentro` desde el contexto), más `readonly="true"` en `nombreTramite`, `centro` y `ayudaTramite` |
| U-nuevo-expediente-006 | RUI-nuevo-expediente-formulario-002 | `Main-…xml`: `showIf="hayQuePreguntarPresentacion"` del campo `presentadoEnPapel`, con el booleano calculado por `…fireActionRule_AsignarPresentacion` → `asignarFormaDePresentar` |
| U-nuevo-expediente-007 | RUI-nuevo-expediente-formulario-003 | `…fireActionRule_AsignarPresentacion` → `asignarFormaDePresentar` deja `presentadoEnPapel` sin valor cuando hay que preguntar; el `<error>` de `…-Local-validateCrear-action` y la puerta 3 de `validateCrear` la hacen obligatoria |
| U-nuevo-expediente-008 | RUI-nuevo-expediente-formulario-004 | `…fireActionRule_AsignarPresentacion` → `asignarFormaDePresentar` sobrescribe `presentadoEnPapel` con la única forma posible cuando no se pregunta |
| U-nuevo-expediente-009 | RUI-nuevo-expediente-formulario-005 | `Main-…xml`: `showIf="hayQuePreguntarParaQuien"` del campo `presentadoEnRepresentacion`, con el booleano calculado por `…fireActionRule_AsignarPresentacion` → `asignarDestinatario` y recalculado en el `onChange` de `presentadoEnPapel` |
| U-nuevo-expediente-010 | RUI-nuevo-expediente-formulario-006 | `…fireActionRule_AsignarPresentacion` → `asignarDestinatario` deja `presentadoEnRepresentacion` en null cuando hay que preguntar; el `<error>` de `…-Local-validateCrear-action` y la puerta 4 de `validateCrear` la hacen obligatoria |
| U-nuevo-expediente-011 | RUI-nuevo-expediente-formulario-007 | `…fireActionRule_AsignarPresentacion` → `asignarDestinatario` fija `presentadoEnRepresentacion` con el único valor posible cuando no se pregunta (y con `false` si ninguno lo es, para que rechace la puerta 5 de `validateCrear` con el literal correcto y no la puerta 4) |
| U-nuevo-expediente-012 | RUI-nuevo-expediente-formulario-008 | `…fireActionRule_AsignarPresentacion` → `asignarDestinatario`: con `presentadoEnPapel` sin contestar deja `hayQuePreguntarParaQuien` a false y el campo a null, así que la pregunta no se ve y no tiene valor |
| U-nuevo-expediente-013 | RUI-nuevo-expediente-formulario-009 | `Main-…xml`: `showIf="hayQuePreguntarPresentacion || hayQuePreguntarParaQuien"` del panel `presentacionPanel` |
| U-nuevo-expediente-014 | RUI-nuevo-expediente-formulario-010 | `Main-…xml`: `showIf="ayudaTramite"` del campo `ayudaTramite` |
| U-nuevo-expediente-015 | RUI-nuevo-expediente-formulario-011 | `AsistenteNuevoExpedienteController.validateCrear` → `doResponseBusinessMessagesAsError("No es posible crear el expediente", …)`; los avisos de pregunta sin contestar salen del `<action-validate>` local, sin título. En los dos casos el error corta el `action-group` y el formulario sigue abierto |

