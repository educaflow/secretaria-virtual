---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la **vista del paso 3** (contexto del trámite y creación) del asistente.

El fichero **ya está materializado y validado con `xmllint`** por el diseñador en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/views/Main-AsistenteNuevoExpediente.xml`.
**MUST** copiarse **literalmente** (byte a byte) a su ruta destino
`src/main/java/com/educaflow/system/ventanilla/views/Main-AsistenteNuevoExpediente.xml`.
**MUST NOT** regenerarlo, reescribirlo ni modificarlo. La acción es `Crear`: la ruta destino no existe todavía.

**MUST NOT** tocarse `subsystem/tramitador/controller/actions-tramitador.xml`: la acción de alta de esta
vista es propia del sistema y apunta al `TramitadorController.triggerInitialEvent` reutilizado sin modificar.


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
| `src/main/java/com/educaflow/system/ventanilla/views/Main-AsistenteNuevoExpediente.xml` | Crear | k-vistas (forms.md, actions.md) | Paso 3: contexto del trámite y creación |

## Nota del diseño para `/sdd-implementer`

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Cómo se encadenan los tres pasos (verbatim, leer antes de implementar)

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
3. **Cada valor arrastrado se lee de un sitio distinto: NO hay una forma única, y las expresiones no son intercambiables.** Una acción disparada desde una **fila** de un listado embebido recibe el contexto de la fila (`handleRowDoubleClickActionId`, `axelor-front/src/views/grid/builder/grid.tsx`: `{...processContextValues(record), selected, _signal, id}`) fundido sobre `getActionContext()` del `panel-related` (`one-to-many.tsx`), que mete el contexto del formulario padre **bajo `_parent`**, no en el primer nivel; `__parent__` resuelve a `Context.getParent()` (`axelor-core/.../ScriptBindings.java`) desde `_parent._model` y es la forma que el proyecto ya usa (`system/gestioncentro/views/gestion-centro-usuarios-autorizados.xml`). Por eso **qué lleva la fila** decide cada expresión, una por valor arrastrado:

   - **`_centroId` del paso 2 — `eval: centro?.id ?: id`: la FILA, no `__parent__`.** Desde el formulario del paso 3 («Atrás») el registro actual ya lleva el centro (`centro?.id`); desde una fila del listado del paso 1 la fila **es** el `Centro`, así que su `id` **es** el centro. **MUST NOT** cambiarse por `__parent__?.centro?.id`: el formulario padre es el del paso 1 y ahí `centro` está a **null** justo cuando hay varios centros —`prepararCentros` solo lo fija si hay exactamente uno—, que es el único caso en que se llega a pulsar una fila; el salto llegaría sin centro y sin ningún error (ESC-005, ESC-019).
   - **`_hayQueElegirCentro` del paso 2 — `eval: hayQueElegirCentro == null ? __parent__?.hayQueElegirCentro : hayQueElegirCentro`: test de nulidad contra `__parent__`.** No puede resolverse como el centro porque la fila pulsada es un `Centro` y **no lleva ese campo**: cuando el salto sale de una fila el valor solo está en el formulario padre, y cuando sale del formulario del paso 3 lo lleva el registro actual.
   - **`_centroId` y `_hayQueElegirCentro` del paso 3 — `eval: __parent__?.centro?.id` y `eval: __parent__?.hayQueElegirCentro`: los DOS con `__parent__`.** Aquí la fila pulsada es un `Tramite`, que no tiene ninguno de los dos, y el formulario padre es el del paso 2, que sí los tiene fijados por su `onNew`.
   - **`_tramiteId` del paso 3 — `eval: id`: la FILA.** El único salto al paso 3 sale de una fila que **es** el `Tramite`.

   `hayQueElegirCentro` lo calcula el paso 1 y los pasos 2 y 3 solo lo arrastran; ninguna acción del servidor vuelve a leerlo.

---

## Paso 8 del diseño — vista del paso 3 (verbatim)

### Paso 8 — Vistas

#### `views/Main-AsistenteNuevoExpediente.xml` — paso 3

Un solo bloque, `sysVentanilla.Main@AsistenteNuevoExpediente`: `<action-view>` **emergente** con `_centroId`, `_hayQueElegirCentro` y `_tramiteId`; `<form>` con el panel «Trámite» (nombre, centro y ayuda, todos en solo lectura, y **al final** los cuatro campos ocultos: `tramite`, los dos `hayQuePreguntar*` y `hayQueElegirCentro`), el panel «Presentación» (las dos preguntas) y la botonera; los `action-group` de los dos botones, del `onNew` y del `onChange` de «¿Cómo se presenta?»; el `<action-validate>` local de las dos preguntas obligatorias; el `<action-record>` que fija trámite, centro y `hayQueElegirCentro` desde el contexto; y los tres `<action-method>` (`recalcular`, `validateCrear` y `triggerInitialEvent`).

El último, `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`, es la acción de alta del `btnCrear`: **acción propia del sistema** —declarada en la sección `<?sv-remotes?>` de este fichero, con el contexto derivado de su ubicación (`VAR-2.1`), el marcador `Remote-` propio de una remota (`VAR-2.2`) y el nombre `-Remote-triggerInitialEvent-action` que coincide con el `method="triggerInitialEvent"` de su `<call>` (`VAR-2.4`)— que apunta al **controlador del tramitador reutilizado sin modificar**, `com.educaflow.subsystem.tramitador.controller.TramitadorController#triggerInitialEvent`. Lo que este sistema aporta es solo el **nombre local** de la acción, con el que `VAR-4.1` resuelve contra una acción declarada en el ámbito de las vistas; el `actions-tramitador.xml` del tramitador **no se toca** (`system/expedientes/views/Main-NuevoExpediente.xml` sigue usando su acción y esa pantalla no se retira en esta iniciativa). Esto es una **desviación declarada** de `design-guidelines.md`, que nombra `subsysTramitador-trigger-initial-event-action` entre las piezas a reutilizar tal cual: aquí se reutiliza el controlador, no la acción global. **MUST NOT** atribuirse al javadoc de `TramitadorController`: el javadoc de su método privado `getContextoTramitacion` reserva el patrón de «acción propia por vista» para los formularios que **no** tienen los campos `tramite`/`centro`/`presentadoEnPapel`/`presentadoEnRepresentacion` y que por eso los pasan como `<context>` fijo, y el formulario del paso 3 sí los tiene (por eso los nombra exactamente así) — o sea, este formulario es justo el caso para el que ese javadoc señala la acción global. El motivo de la desviación y su equivalencia funcional están en «Notas y supuestos» y en **D7** de `decisiones.md`.

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

**Ninguno de los tres formularios guarda nada:** el modelo no se persiste nunca (`persistable="false"`) y el spec lo declara explícitamente para el paso 3 («este formulario no lleva “Guardar”, “Cancelar” ni “Borrar” (la ficha no se guarda nunca)», `screen-nuevo-expediente.md`), así que en los tres no hay `btnSave`, ni `btnDelete`, ni `canBackOnSave`, ni `remote-validationSave-action`, ni `save` → `force-back`. Los cinco botones que sí existen (`btnCancelar` ×2, `btnAtras` ×2 y `btnCrear`) navegan o cierran con `close`, que es lo único que funciona en estas vistas: `back` solo conmuta entre el `grid` y el `form` de una misma pestaña, y el `<action-view>` de cada uno de los tres ficheros declara **solo** `<view type="form">`, sin ninguna `<view type="grid">`, así que no hay `grid` al que volver. Los `btnCancelar` son los de los pasos 1 y 2 —la salida del asistente, que el spec sí pide ahí— y no la «Cancelar» de la botonera de mantenimiento, que no existe en ninguno de los tres.

El botón de salida de los pasos 1 y 2 se llama **`btnCancelar`** —rótulo «Cancelar», el que comprueban los `ESC-`/`T-`— y su `action-group`, `…-btnCancelar-action` como exige `VAR-7.1`, contiene **`close`**, igual que los otros cuatro botones: ninguno de los tres forms persiste (ninguno declara `btnSave`) y ningún `<action-view>` de los tres declara una `<view type="grid">`, así que no hay ni cambios que perder ni `grid` al que volver. El criterio literal de la rama de `VAR-7.2` que eso obliga a declarar, el texto final de cada fichero normativo y qué vistas quedan dentro y fuera están en el **punto 3** de «Cambios necesarios fuera del sistema»; el choque y las alternativas, en **D8** de `decisiones.md`.

**Verificar al final:** la aplicación arranca sin errores de carga de vistas y `grep -n "canBackOnSave\|remote-validation" src/main/java/com/educaflow/system/ventanilla/views/*.xml` no devuelve nada.


## Trazabilidad — Reglas de UI (verbatim)

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


## Notas y supuestos del diseño (verbatim)

## Notas y supuestos

- **El paso 1 se abre como pestaña, no como ventana emergente.** El spec describe «Listado de centros» como ventana emergente. Con una emergente, el clic en una fila del listado no puede cerrarla (`DefaultActionExecutor.#closeView()` se rinde si hay emergentes y el `FormActionHandler` del listado embebido no tiene `closeHandler`), así que el asistente se quedaría a medio cerrar al crear el expediente. El paso 3, que es donde el spec vuelve a pedir ventana emergente, sí lo es. Detalle y alternativas en **D1** de `decisiones.md`.
- **Mientras el paso 1 decide, no muestra nada.** El listado de centros y el botón «Cancelar» solo se ven si `hayQueElegirCentro`, así que en los casos «ningún centro» y «un solo centro» el usuario **nunca** ve el listado de centros, como exigen ESC-001 y ESC-003: la ventana existe un instante, pero vacía, y se cierra sola.
- **El modelo vive en `domains/` y no en `views-models/`.** Motivo y alternativa en **D6** de `decisiones.md`.
- **`hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien`, `centrosDisponibles`, `tramitesDisponibles` y `hayQueElegirCentro` no están en `entity-AsistenteNuevoExpediente.md`.** No son campos de negocio: son la forma de que el servidor —único que conoce los perfiles del usuario— conteste a las reglas de UI que el propio spec exige (`RUI-…-formulario-002` a `-009` y `RUI-…-arbol-tramites-001`). Sin ellos esas reglas no se podrían evaluar en la vista, o habría que replicar la clasificación en JavaScript (**D2** y **D3** de `decisiones.md`).
- **Las dos preguntas no llevan `requiredIf`.** Con `requiredIf`, la acción `validate` del framework cortaría antes con su mensaje genérico y el usuario nunca vería «Debe indicar cómo se presenta el expediente» / «Debe indicar para quién es el expediente», que el spec fija literalmente. La obligatoriedad se expresa con el `<action-validate>` local y, sobre todo, con las puertas 3 y 4 de `validateCrear`.
- **El `btnCrear` NO invoca la acción global `subsysTramitador-trigger-initial-event-action`, pese a que `design-guidelines.md` pide reutilizarla tal cual.** En su lugar declara en la sección `<?sv-remotes?>` de `views/Main-AsistenteNuevoExpediente.xml` una acción propia, `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`, que llama al **mismo** `TramitadorController.triggerInitialEvent` sin `<context>` propio: la cadena es funcionalmente equivalente —el controlador lee del formulario los mismos cuatro campos `tramite`, `centro`, `presentadoEnPapel` y `presentadoEnRepresentacion`, que por eso se llaman exactamente así—, y lo único que la ventanilla aporta es el **nombre local**. El motivo es de ámbito, no de comportamiento: `VAR-4.1` exige que el `<action name>` de un `action-group` resuelva contra una acción declarada en el ámbito de las vistas del sistema, y la global vive en `subsystem/tramitador/controller/actions-tramitador.xml`. Ese fichero **MUST NOT** tocarse: sigue siendo el que usa `system/expedientes/views/Main-NuevoExpediente.xml`, pantalla que esta iniciativa no retira. La desviación afecta al nombre de la acción, **no** al controlador ni al servicio del motor, que se reutilizan tal cual. Detalle del choque con `view-rules.md` en **D8** de `decisiones.md`.
- **El alta la ejecuta el controlador del tramitador, y V-007/V-008 no la defienden — riesgo aceptado.** El afinado propio de la ventanilla (V-007 y V-008) solo se evalúa en `…-Remote-validateCrear-action`, que es una llamada previa e independiente dentro del `action-group` del botón; quien invoque directamente `TramitadorController.triggerInitialEvent` (por la acción de esta vista, por la del tramitador o por su `@CallMethod`) crea el expediente sin pasar por él. El afinado no se traslada al motor. Motivo, alternativa descartada y qué **MUST NOT** hacerse como parche puntual: **D7** de `decisiones.md`.
- **Las cuatro acciones propias declaran su `validate<Accion>`, y las consultan los dos lados.** También `prepararCentros`, cuyo validador no tiene hoy ninguna precondición: el par acción + validador es el contrato público mínimo de toda acción (`k-sistemas/servicios.md`). El **controlador** consulta el validador y contesta sus mensajes con `doResponseBusinessMessagesAsError` antes de invocar la acción, y la **acción** empieza igualmente por `validate<Accion>(...).ifPresent(BusinessMessages::throwIfInvalid)` como red de seguridad de cualquier otro llamante. Sin el tramo del controlador el mensaje llegaría al usuario como `ValidationException` cruda, que es el mismo 500 disfrazado que se quería evitar al sustituir `IllegalStateException`.
- **La ficha no se guarda porque no hay dónde guardarla.** `persistable="false"` genera el modelo como `@MappedSuperclass`: sin `@Entity` ni `@Table`, no tiene tabla ni endpoint de guardado utilizable, y ninguna de las tres vistas declara `save` ni `delete`. La única defensa que hace falta —y la única que se ejecuta— es el deny-all de `allowPropertiesInsert/Update`, que corre en `modelService.validate`.
- **El perfil de inicio NO se obtiene llamando a `PerfilesUsuarioService.getPerfil(tramite, user, centro, presentadoEnPapel)`, pese a que `design-guidelines.md` pide reutilizarla tal cual.** `getPerfil` lanza `IllegalStateException` precisamente en los dos casos que HU-006 exige rechazar con un mensaje de negocio, y `recalcular` —que es quien necesita el perfil— corre en el `onNew`/`onChange`, antes de cualquier validación: llamarla ahí convertiría una petición manipulada en un 500 en vez del mensaje que pide HU-006. En su lugar, `perfilDeInicioPara(boolean)` deriva el perfil de inicio preguntando a los predicados del propio enum `Profile` (`puedeCrearExpediente()` y `permitePresentacionEnPapel()`), sin consultar los perfiles del usuario. Detalle y justificación completa en **D5** de `decisiones.md`.
