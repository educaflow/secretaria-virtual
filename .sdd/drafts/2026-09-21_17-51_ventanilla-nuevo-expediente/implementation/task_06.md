---
type: implementation-task
template: system
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la vista `ElegirTramite-AsistenteNuevoExpediente.xml` del asistente.

El XML **ya está materializado** por el diseñador en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/views/ElegirTramite-AsistenteNuevoExpediente.xml`.
**MUST** copiarse **literalmente** (`cp`, sin reescribirlo, sin reformatearlo y sin regenerarlo desde este texto)
a su ruta destino `src/main/java/com/educaflow/system/ventanilla/views/ElegirTramite-AsistenteNuevoExpediente.xml`, creando antes la carpeta destino
con `mkdir -p`. El texto del diseño que va debajo es para **entender** la vista y para comprobar que el XML copiado
es el que describe, **no** para reescribirla.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/views/ElegirTramite-AsistenteNuevoExpediente.xml` | Crear | k-vistas (forms.md, grids.md, actions.md) | Paso 2: listado de trámites agrupado por tipo de trámite |

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

#### `views/ElegirTramite-AsistenteNuevoExpediente.xml` — paso 2

Dos bloques.

**Bloque maestro** `sysVentanilla.ElegirTramite@AsistenteNuevoExpediente`: `<action-view>` (pestaña) con los dos valores que trae del paso 1, `_centroId` (`eval: centro?.id ?: id`) y `_hayQueElegirCentro` (`eval: hayQueElegirCentro == null ? __parent__?.hayQueElegirCentro : hayQueElegirCentro` — el registro actual cuando el salto sale de un formulario, y el del padre cuando sale de una fila, que es un `Centro` y no lo lleva); `<form>` con el centro en solo lectura encima del listado, el listado y el botón; `onNew` que fija los dos en el registro con `…-set-centroYHayQueElegirCentro-recibidos-action` y llama a `prepararTramites`; el `<action-method>` de `prepararTramites`.

El servidor **no** recalcula `hayQueElegirCentro` en este paso: lo calcula el paso 1 y lo transporta la vista (ver Paso 1).

ASCII Layout:

```
centroPanel
cccccc······   ← centro(6) + hayQueElegirCentro(6, oculto)
                 El oculto va DETRÁS del visible: un campo oculto consume igualmente sus columnas
                 (computeLayout asigna gridColumnStart/End a todos los items), así que delante
                 empujaría el centro a la mitad derecha de la fila. Detrás, el hueco queda al borde
                 derecho y el centro se queda en las columnas 1-6: un nombre de centro no necesita
                 las 12 (k-vistas/forms.md, «los campos no tienen que rellenar las 12 columnas»)

tramitesPanel
pppppppppppp   ← panel-related tramitesDisponibles(12)

buttons-panel → buttonsConEleccionDeCentro (hayQueElegirCentro)
..........aa   ← colOffset(10) + btnAtras(2)

buttons-panel → buttonsSinEleccionDeCentro (!hayQueElegirCentro)
..........cc   ← colOffset(10) + btnCancelar(2)
```

Los dos botones van en paneles anidados excluyentes porque llevan `colOffset`: dos gemelos con `colOffset` en un panel plano no renderizan bien (`k-vistas/forms.md`, «botones condicionales por estado»). Las dos condiciones son complementarias, así que siempre se ve **un** botón y solo uno.

**Bloque de detalle** `sysVentanilla.ElegirTramite@AsistenteNuevoExpediente.Tramite`: el `<grid>` de `Tramite` con una sola columna **visible** (el nombre del trámite; el tipo de trámite se pinta solo como cabecera de grupo, que es el nivel que pide el spec, como hacen los dos grids con `groupBy` del repo), `groupBy="tipoTramite.name"` y `orderBy="tipoTramite.name,name"` — el primer nivel es el grupo (el nombre del tipo de trámite) y el segundo las filas (los trámites), y Axelor pinta los grupos ya abiertos. La clave de agrupar es la **misma ruta punteada** con la que empieza la de ordenar, que es como agrupan los dos precedentes del repo (`system/expedientes/views/Abierto-Expediente.xml` y `Cerrado-Expediente.xml`): `useGridSortBy` (`axelor-front/src/views/grid/builder/utils.ts`) compone el `sortBy` con las columnas de `groupBy` más las de `orderBy` que no estén ya en `groupBy`, así que agrupar por el m2o `tipoTramite` dejaría tres claves y la primera ordenaría por un objeto. Su `action-group` de clic de fila abre el paso 3 y cierra este.

**La columna por la que se agrupa va declarada, oculta:** `<field name="tipoTramite.name" title="Tipo de trámite" hidden="true"/>`. No es decorativo ni redundante con el `groupBy`: la **proyección** que el listado pide al servidor la compone `useGridColumnNames` (`axelor-front/src/views/grid/builder/scope.tsx`) recorriendo `view.items` y las columnas de `orderBy`, y de estas últimas **solo conserva las que tienen metadatos de campo** (`const field = fields?.[item.name!]; … return names;`), que una ruta punteada no declarada como `<field>` no tiene; el `groupBy` no entra en esa cuenta en ningún caso. Sin esta columna el `POST …/Tramite/search` viaja con `"fields":["name"]`, `tipoTramite.name` no llega en los registros y **la cabecera de grupo se pinta vacía** (se ve `": (1 elementos)"` y el `aria-label` queda en `"Collapse group undefined: "`), aunque los datos en base de datos sean correctos. `hidden="true"` apaga solo el pintado —`columnProps.visible = !hidden` en `axelor-front/src/views/grid/builder/grid.tsx`—, **no** quita el nombre de la proyección, así que el tipo de trámite sigue sin ser una columna y solo se ve como cabecera de grupo, que es lo que pide `RUI-nuevo-expediente-arbol-tramites-003`. El `title` es obligatorio por la i18n del proyecto (`k-i18n`): el script de traducción lo recoge del XML aunque la columna no se pinte.

### Trazabilidad — Reglas de UI (filas que le corresponden)

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-002 | RUI-nuevo-expediente-arbol-tramites-001 | `ElegirTramite-…xml`: paneles `buttonsConEleccionDeCentro` / `buttonsSinEleccionDeCentro` con `showIf` excluyentes sobre `hayQueElegirCentro`, que llega del paso 1 en el `<context>` `_hayQueElegirCentro` y lo fija `…-set-centroYHayQueElegirCentro-recibidos-action` |
| U-nuevo-expediente-003 | RUI-nuevo-expediente-arbol-tramites-002 | `ElegirTramite-…xml`: `<field name="centro" readonly="true">` del panel `centroPanel`, encima del listado |
| U-nuevo-expediente-004 | RUI-nuevo-expediente-arbol-tramites-003 | `ElegirTramite-…xml`: `groupBy="tipoTramite.name"` + `orderBy="tipoTramite.name,name"` del grid `…@AsistenteNuevoExpediente.Tramite-grid`, más la columna `<field name="tipoTramite.name" hidden="true"/>` que mete esa ruta en la proyección para que la cabecera de grupo tenga texto (misma clave para agrupar y ordenar; Axelor pinta los grupos abiertos) |
