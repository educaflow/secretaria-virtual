---
type: implementation-task
template: system
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la vista `ElegirCentro-AsistenteNuevoExpediente.xml` del asistente.

El XML **ya está materializado** por el diseñador en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/views/ElegirCentro-AsistenteNuevoExpediente.xml`.
**MUST** copiarse **literalmente** (`cp`, sin reescribirlo, sin reformatearlo y sin regenerarlo desde este texto)
a su ruta destino `src/main/java/com/educaflow/system/ventanilla/views/ElegirCentro-AsistenteNuevoExpediente.xml`, creando antes la carpeta destino
con `mkdir -p`. El texto del diseño que va debajo es para **entender** la vista y para comprobar que el XML copiado
es el que describe, **no** para reescribirla.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/views/ElegirCentro-AsistenteNuevoExpediente.xml` | Crear | k-vistas (forms.md, grids.md, actions.md) | Paso 1: listado de centros candidatos |

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

#### `views/ElegirCentro-AsistenteNuevoExpediente.xml` — paso 1

Dos bloques.

**Bloque maestro** `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente`: un `<action-view>` (pestaña, sin `popup`) que abre el `<form>`; el `<form>` tiene tres paneles (los campos técnicos ocultos, el listado y los botones) y un `onNew` que llama a `prepararCentros` y decide el arranque; un `<action-validate>` con el `<info>` del aviso cuando no hay ningún centro candidato; y el `<action-method>` de `prepararCentros`.

El `action-group` del `onNew` es la **única** pieza que decide cómo arranca el asistente, y cada una de sus condiciones pregunta por **lo que significa** a quien lo decidió, sin recomponerlo desde otros dos valores. Los tres estados alcanzables del arranque y las ramas que los cubren:

| Estado (lo que devolvió el servidor) | Condición | Qué hace |
|---|---|---|
| 0 centros candidatos (`centrosDisponibles` vacía, `hayQueElegirCentro` `false`, `centro` `null`) | `centrosDisponibles.isEmpty()` | muestra el aviso «No puede crear expedientes en ninguno de sus centros» |
| 1 centro candidato (`centro` fijado, `hayQueElegirCentro` `false`) | `centro != null` | abre el paso 2 con ese centro |
| 0 o 1 centro candidato (las dos ramas anteriores terminan aquí) | `!hayQueElegirCentro` | cierra este paso |
| 2 o más centros candidatos (`hayQueElegirCentro` `true`, `centro` `null`) | `hayQueElegirCentro` (ninguna de las anteriores) | no hace nada: el paso se queda y muestra el listado |

`centrosDisponibles` es el **único dueño** de «no hay ningún centro candidato»: `prepararCentros` la asigna incondicionalmente en esa misma respuesta y de ella deriva el servidor los otros dos valores, así que la vista la lee tal cual en vez de rehacer la conjunción `!hayQueElegirCentro && centro == null`, que quedaría desincronizada en silencio si `fireActionRule_AsignarArranqueDelAsistente` se afinara.

ASCII Layout de los paneles no triviales:

```
estadoPanel
············   ← centro(6, oculto) + hayQueElegirCentro(6, oculto)
                 Los dos únicos items del panel son ocultos, así que la fila queda vacía entera y
                 no desplaza nada de los paneles siguientes

centrosPanel (visible solo si hayQueElegirCentro)
pppppppppppp   ← panel-related centrosDisponibles(12)

buttons-panel (visible solo si hayQueElegirCentro)
..........cc   ← colOffset(10) + btnCancelar(2)     [único botón, principal, al borde derecho]
                 El `showIf` va en el propio `buttons-panel`: con un solo botón no hay estados
                 que particionar, así que no hace falta ningún panel de estado anidado y el
                 panel entero colapsa cuando no hay que elegir centro
```

**Bloque de detalle** `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente.Centro`: el `<grid>` de `Centro` con una sola columna (el nombre), ordenado por nombre, y su `action-group` de clic de fila, que abre el paso 2 y cierra este. El `<grid>` no tiene `canNew`/`canEdit`/`canDelete`: del listado solo se elige.

### Trazabilidad — Reglas de UI (filas que le corresponden)

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-001 | RUI-nuevo-expediente-listado-centros-001 | `ElegirCentro-AsistenteNuevoExpediente.xml`: `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-onNew-action` (las tres ramas) + `…-Local-avisoSinCentros-action` + `showIf="hayQueElegirCentro"` del panel `centrosPanel` |
