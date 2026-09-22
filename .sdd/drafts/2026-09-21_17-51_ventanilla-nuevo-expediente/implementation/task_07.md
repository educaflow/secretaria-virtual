---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la vista `Main-AsistenteNuevoExpediente.xml` del asistente.

El XML **ya está materializado** por el diseñador en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/views/Main-AsistenteNuevoExpediente.xml`.
**MUST** copiarse **literalmente** (`cp`, sin reescribirlo, sin reformatearlo y sin regenerarlo desde este texto)
a su ruta destino `src/main/java/com/educaflow/system/ventanilla/views/Main-AsistenteNuevoExpediente.xml`, creando antes la carpeta destino
con `mkdir -p`. El texto del diseño que va debajo es para **entender** la vista y para comprobar que el XML copiado
es el que describe, **no** para reescribirla.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/views/Main-AsistenteNuevoExpediente.xml` | Crear | k-vistas (forms.md, actions.md) | Paso 3: contexto del trámite y creación |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.
>
> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

## Diseño (verbatim)

## Cómo se encadenan los tres pasos (leer antes de implementar)

El asistente son **tres vistas** sobre el **mismo** modelo de pantalla, cada una con su propio `onNew` que la rellena desde el contexto que recibe:

```
menú «Ventanilla → Nuevo expediente»
  └─ sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action        (pestaña)
        ├─ 0 centros candidatos  → aviso y se cierra
        ├─ 1 centro candidato    → abre el paso 2 con ese centro y se cierra
        └─ 2 o más               → se queda y muestra el listado de centros
              └─ clic en una fila → sysVentanilla.ElegirTramite@…-action  (pestaña) y se cierra
                    └─ clic en un trámite → sysVentanilla.Main@…-action   (pestaña) y se cierra
                          ├─ «Atrás»            → reabre el paso 2 con el mismo centro y se cierra
                          └─ «Crear expediente» → sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action
                                                   (→ TramitadorController.triggerInitialEvent)
```

Tres reglas, y no hay más que recordar:

1. **Cada paso abre el siguiente y se cierra a sí mismo**: un `<action-group>` con el `<action-view>` del siguiente y `close` al final (el framework exige que `close` sea la última acción del grupo). Así nunca hay dos pasos abiertos a la vez y «Crear expediente» deja la pantalla limpia.
2. **Los TRES pasos son pestañas. Ninguno es una ventana emergente, y eso MUST NOT cambiarse:** **MUST NOT** añadir `<view-param name="popup">` ni `popup-save` a ninguno de los tres `<action-view>`. Por qué la topología es esa —el mecanismo de `axelor-front`, el código citado y las alternativas evaluadas— está en **D1** de `decisiones.md`, su **único dueño**.
3. **Cada valor arrastrado se lee de un sitio distinto: NO hay una forma única, y las expresiones no son intercambiables.** Una acción disparada desde una **fila** de un listado embebido recibe el contexto de la fila (`handleRowDoubleClickActionId`, `axelor-front/src/views/grid/builder/grid.tsx`: `{...processContextValues(record), selected, _signal, id}`) fundido sobre `getActionContext()` del `panel-related` (`one-to-many.tsx`), que mete el contexto del formulario padre **bajo `_parent`**, no en el primer nivel; `__parent__` resuelve a `Context.getParent()` (`axelor-core/.../ScriptBindings.java`) desde `_parent._model` y es la forma que el proyecto ya usa (`system/gestioncentro/views/gestion-centro-usuarios-autorizados.xml`). Por eso **qué lleva la fila** decide cada expresión, una por valor arrastrado:

   - **`_centroId` del paso 2 — `eval: centro?.id ?: id`: la FILA, no `__parent__`.** Desde el formulario del paso 3 («Atrás») el registro actual ya lleva el centro (`centro?.id`); desde una fila del listado del paso 1 la fila **es** el `Centro`, así que su `id` **es** el centro. **MUST NOT** cambiarse por `__parent__?.centro?.id`: el formulario padre es el del paso 1 y ahí `centro` está a **null** justo cuando hay varios centros —`prepararCentros` solo lo fija si hay exactamente uno—, que es el único caso en que se llega a pulsar una fila; el salto llegaría sin centro y sin ningún error (ESC-005, ESC-019).
   - **`_hayQueElegirCentro` del paso 2 — `eval: hayQueElegirCentro == null ? __parent__?.hayQueElegirCentro : hayQueElegirCentro`: test de nulidad contra `__parent__`.** No puede resolverse como el centro porque la fila pulsada es un `Centro` y **no lleva ese campo**: cuando el salto sale de una fila el valor solo está en el formulario padre, y cuando sale del formulario del paso 3 lo lleva el registro actual.
   - **`_centroId` y `_hayQueElegirCentro` del paso 3 — `eval: __parent__?.centro?.id` y `eval: __parent__?.hayQueElegirCentro`: los DOS con `__parent__`.** Aquí la fila pulsada es un `Tramite`, que no tiene ninguno de los dos, y el formulario padre es el del paso 2, que sí los tiene fijados por su `onNew`.
   - **`_tramiteId` del paso 3 — `eval: id`: la FILA.** El único salto al paso 3 sale de una fila que **es** el `Tramite`.

   `hayQueElegirCentro` lo calcula el paso 1 y lo transporta la vista (ver Paso 1).

---

### Paso 8 — Vistas

#### `views/Main-AsistenteNuevoExpediente.xml` — paso 3

Un solo bloque, `sysVentanilla.Main@AsistenteNuevoExpediente`: `<action-view>` **de pestaña** —igual que los otros dos: **sin** `popup` ni `popup-save`, por lo que explica la regla 2 de «Cómo se encadenan los tres pasos» y **D1** de `decisiones.md`— con `_centroId`, `_hayQueElegirCentro` y `_tramiteId`; `<form>` con el panel «Trámite» (nombre, centro y ayuda, todos en solo lectura, y **al final** los cuatro campos ocultos: `tramite`, los dos `hayQuePreguntar*` y `hayQueElegirCentro`), el panel «Presentación» (las dos preguntas) y la botonera; los `action-group` de los dos botones, del `onNew` y del `onChange` de «¿Cómo se presenta?»; el `<action-validate>` local de las dos preguntas obligatorias; el `<action-record>` que fija trámite, centro y `hayQueElegirCentro` desde el contexto; y los tres `<action-method>` (`recalcular`, `validateTriggerInitialEvent` y `triggerInitialEvent`).

El último, `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`, es la acción de alta del `btnCrear`: la declara la sección `<?sv-remotes?>` de **este** fichero y llama, **sin `<context>` propio**, a `com.educaflow.subsystem.tramitador.controller.TramitadorController#triggerInitialEvent`. El `actions-tramitador.xml` del tramitador **MUST NOT** tocarse. Motivo de la desviación respecto a `design-guidelines.md` y su equivalencia funcional: **D7** de `decisiones.md`.

`hayQueElegirCentro` está aquí solo para devolverlo: el `btnAtras` reabre el paso 2 con el mismo `<context>`, así que el botón de salida de aquel recupera el rótulo que le corresponde (lo calcula el paso 1 y lo transporta la vista, ver Paso 1).

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

**Ninguno de los tres formularios guarda nada:** el modelo no se persiste nunca (`persistable="false"`) y el spec lo declara explícitamente para el paso 3 («este formulario no lleva “Guardar”, “Cancelar” ni “Borrar” (la ficha no se guarda nunca)», `screen-nuevo-expediente.md`), así que en los tres no hay `btnSave`, ni `btnDelete`, ni `canBackOnSave`, ni `remote-validationSave-action`, ni `save` → `force-back`. Los cinco botones que sí existen (`btnCancelar` ×2, `btnAtras` ×2 y `btnCrear`) navegan o cierran con `close`, que es lo único que funciona en estas vistas: `back` solo conmuta entre el `grid` y el `form` de una misma pestaña, y el `<action-view>` de cada uno de los tres ficheros declara **solo** `<view type="form">`, sin ninguna `<view type="grid">`, así que no hay `grid` al que volver. Los `btnCancelar` son los de los pasos 1 y 2 —la salida del asistente, que el spec sí pide ahí— y no la «Cancelar» de la botonera de mantenimiento, que no existe en ninguno de los tres.

El botón de salida de los pasos 1 y 2 se llama **`btnCancelar`** —rótulo «Cancelar», el que comprueban los `ESC-`/`T-`— y su `action-group`, `…-btnCancelar-action` como exige `VAR-7.1`, contiene **`close`**, igual que los otros cuatro botones: ninguno de los tres forms persiste (ninguno declara `btnSave`) y ningún `<action-view>` de los tres declara una `<view type="grid">`, así que no hay ni cambios que perder ni `grid` al que volver. Es la **rama del asistente** de `VAR-7.2`, que ya es norma vigente en `agent_docs/view-rules.md`, en `k-vistas/forms.md` y en el test generado; el choque que la produjo y las alternativas, en **D8** de `decisiones.md`.

**Verificar al final:** la aplicación arranca sin errores de carga de vistas y `grep -n "canBackOnSave\|remote-validation" src/main/java/com/educaflow/system/ventanilla/views/*.xml` no devuelve nada.

### Trazabilidad — Reglas de UI (filas que le corresponden)

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-005 | RUI-nuevo-expediente-formulario-001 | `Main-…xml`: `…-set-tramiteCentroYHayQueElegirCentro-recibidos-action` desde `onNew` (fija trámite, centro y `hayQueElegirCentro` desde el contexto), más `readonly="true"` en `nombreTramite`, `centro` y `ayudaTramite` |
| U-nuevo-expediente-006 | RUI-nuevo-expediente-formulario-002 | `Main-…xml`: `showIf="hayQuePreguntarPresentacion"` del campo `presentadoEnPapel`, con el booleano calculado por `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarFormaDePresentar` |
| U-nuevo-expediente-009 | RUI-nuevo-expediente-formulario-005 | `Main-…xml`: `showIf="hayQuePreguntarParaQuien"` del campo `presentadoEnRepresentacion`, con el booleano calculado por `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarDestinatario` y recalculado en el `onChange` de `presentadoEnPapel` |
| U-nuevo-expediente-013 | RUI-nuevo-expediente-formulario-009 | `Main-…xml`: `showIf="hayQuePreguntarPresentacion || hayQuePreguntarParaQuien"` del panel `presentacionPanel` |
| U-nuevo-expediente-014 | RUI-nuevo-expediente-formulario-010 | `Main-…xml`: `showIf="ayudaTramite"` del campo `ayudaTramite` |
| U-nuevo-expediente-015 | RUI-nuevo-expediente-formulario-011 | `AsistenteNuevoExpedienteController.validateTriggerInitialEvent` → `doResponseBusinessMessagesAsError("No es posible crear el expediente", …)`; los avisos de pregunta sin contestar salen del `<action-validate>` local, sin título. En los dos casos el error corta el `action-group` y el formulario sigue abierto |

### Trazabilidad — Reglas de negocio (fila que le corresponde)

| R | Origen spec | Ubicación | Momento | Detalle |
|---|---|---|---|---|
| R-AsistenteNuevoExpediente-001 | RN-AsistenteNuevoExpediente-001 | `views/Main-AsistenteNuevoExpediente.xml`, `sysVentanilla.Main@AsistenteNuevoExpediente-btnCrear-action` → `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action` (`TramitadorController.triggerInitialEvent` → `TramitadorService.triggerInitialEvent`) | antes de commit; crea el expediente, cierra el asistente y abre el expediente | — |
