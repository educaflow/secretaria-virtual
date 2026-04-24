# Object Views — Referencia XSD Axelor 8.1

Fuente: `object-views.xsd` (namespace `http://axelor.com/xml/ns/object-views`)

---

## Tipos enumerados

### HiliteStyle (atributo `color` y `background` en `<hilite>`)

Valores semánticos:
- `default`, `primary`, `warning`, `success`, `danger`, `info`

Valores de color por nombre (también válidos):
- `red`, `pink`, `purple`, `deeppurple`, `indigo`, `blue`, `lightblue`, `cyan`, `teal`, `green`, `lightgreen`, `lime`, `yellow`, `amber`, `orange`, `deeporange`, `brown`, `grey`, `bluegrey`, `black`, `white`, `olive`, `violet`

> ⚠️ `muted` NO existe — provoca SAXParseException.

### SelectorType (atributo `x-selector` en `<grid>` y `<panel-related>`)

- `checkbox` (por defecto)
- `none`

### LabelStyle (atributo `tag-style` en `<menuitem>`)

- `default`, `important`, `success`, `warning`, `inverse`, `info`

### ColorStyle (atributo `icon-background`, etc.)

Igual que `ColorNameStyle` + hex `#RRGGBB` o `#RRGGBBAA`.

---

## AbstractWidget (base de todos los widgets)

Todos los elementos de vista heredan de `AbstractWidget`, que tiene:

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | string | Identificador único |
| `if` | string | Expresión Groovy — solo usar el widget si es `true` |
| `if-module` | string | Solo usar si el módulo está instalado |

> ℹ️ El atributo `if` está en el XSD pero **no aparece en la documentación oficial** de Axelor 8.1. Funciona en `<menuitem>` para visibilidad dinámica.

---

## `<menuitem>`

Extiende `AbstractWidget` (hereda `if`, `if-module`).

| Atributo | Oblig. | Descripción |
|---|---|---|
| `name` | ✓ | Identificador del menú |
| `title` | ✓ | Texto visible |
| `parent` | | Nombre del menú padre |
| `action` | | Acción a ejecutar al hacer clic |
| `order` | | Orden de aparición (número) |
| `groups` | | Grupos autorizados (CSV). ⚠️ Si se omite puede no ser visible para usuarios normales |
| `icon` | | Imagen del menú |
| `icon-background` | | Color de fondo del icono (ColorStyle) |
| `left` | boolean | Mostrar en navegación izquierda |
| `mobile` | boolean | Mostrar en menú móvil |
| `hidden` | boolean | Ocultar el menú |
| `tag` | string | Etiqueta fija sobre el menú |
| `tag-count` | boolean | Mostrar contador como etiqueta |
| `tag-get` | string | Acción para obtener etiqueta dinámica |
| `tag-style` | LabelStyle | Estilo de la etiqueta |

**Ejemplo con visibilidad dinámica:**
```xml
<menuitem name="configuracioncentro-menu" title="Configuración Centro"
    groups="admins,users" order="400"
    if="__user__?.group?.code == 'admins' || com.axelor.db.Query.of(com.educaflow.subsystem.security.db.CentroUsuarioTipoUsuario.class).filter(&quot;self.centroUsuario.usuario = :user AND self.tipoUsuario.code = 'ADMINISTRADOR'&quot;).bind(&quot;user&quot;, __user__).count() &gt; 0"/>
```

---

## `<grid>` (GridView)

Hereda de `AbstractGridView` + `ObjectViewAttributes` + `AdvanceSearchAttributes`.

### Atributos principales

| Atributo | Tipo | Descripción |
|---|---|---|
| `name` | string ✓ | Nombre de la vista |
| `model` | string ✓ | FQN del modelo |
| `title` | string | Título de la tabla |
| `editable` | boolean | Edición inline |
| `orderBy` | string | Campos de ordenación (CSV) |
| `groupBy` | string | Campos de agrupación (CSV) |
| `edit-icon` | boolean | Mostrar icono de edición (defecto: true) |
| `canNew` | string | Permitir crear registros (expr. booleana) |
| `canEdit` | string | Permitir editar registros |
| `canDelete` | boolean | Permitir borrar registros |
| `canSave` | boolean | Permitir guardar |
| `canMove` | boolean | Arrastrar para reordenar |
| `canArchive` | boolean | Permitir archivar/desarchivar |
| `canViewOnClick` | boolean | Abrir en vista al hacer clic |
| `x-selector` | SelectorType | Selector de fila: `checkbox` o `none` |
| `freeSearch` | string | Búsqueda libre: `all`, `none`, o campos CSV |
| `customSearch` | boolean | Búsqueda avanzada personalizada |
| `widget` | string | `expandable` o `tree-grid` |

### Elementos hijo

- `<field>` — columna (atributos: `name` ✓, `title`, + atributos de form)
- `<button>` — botón por fila (atributos: `name` ✓, `title`, `icon`, `onClick` ✓, `prompt`, `css`)
- `<toolbar>` — barra de botones (requiere al menos un `<button>` hijo — ⚠️ no puede estar vacío)
- `<menubar>` — menú desplegable en toolbar
- `<hilite>` — resaltado de filas o celdas
- `<summary-bar>` — barra de totales al pie

---

## `<hilite>`

| Atributo | Oblig. | Descripción |
|---|---|---|
| `if` | ✓ | Condición booleana |
| `color` | | Color del texto (HiliteStyle) |
| `background` | | Color de fondo (HiliteStyle) |
| `strong` | | Texto en negrita (boolean) |

---

## `<panel-related>`

Extiende `AbstractPanel` (hereda `title`, `colSpan`, `hidden`, `readonly`, `if`, etc.).

| Atributo | Oblig. | Descripción |
|---|---|---|
| `field` | ✓ | Nombre del campo relacional (o2m/m2m) |
| `editable` | boolean | Edición inline en el grid |
| `orderBy` | string | Ordenación |
| `groupBy` | string | Agrupación |
| `x-selector` | SelectorType | Selector de fila |
| `domain` | string | Filtro JPQL por defecto |
| `form-view` | string | Vista form para registros |
| `grid-view` | string | Vista grid para el listado |
| `canNew` | string | Crear nuevos (expr. booleana) |
| `canEdit` | string | Editar existentes |
| `canView` | string | Ver el registro |
| `canRemove` | string | Eliminar del listado |
| `canMove` | boolean | Reordenar |
| `canSuggest` | boolean | Sugerencias automáticas |
| `canSelect` | string | Seleccionar existentes |
| `onNew` | string | Acción al crear |
| `onChange` | string | Acción al cambiar |
| `onDelete` | string | Acción al borrar |
| `widget` | string | Widget alternativo |

> ℹ️ `canDelete` y `canSave` en `panel-related` se controlan a través del grid embebido (AbstractGridView), no directamente en el PanelRelated. Usar `canEdit=false` + `editable=false` para grids de solo lectura.

**Atributos prohibidos** en `panel-related`: `cols`, `colWidths`, `itemSpan`, `stacked`, `showTitle`.

### Elementos hijo de `panel-related`

- `<field>` — columna del grid embebido
- `<button>` — botón por fila

---

## `<action-view>`

| Elemento/Atributo | Descripción |
|---|---|
| `name` | Nombre de la acción (atributo) |
| `title` | Título de la vista (atributo ✓) |
| `model` | FQN del modelo (atributo) |
| `<view type="grid" name="..."/>` | Vista grid a mostrar |
| `<view type="form" name="..."/>` | Vista form a mostrar |
| `<view-param name="forceEdit" value="true"/>` | Abre en modo edición |
| `<domain>` | Filtro JPQL (elemento, no atributo) — soporta `:__user__` |
| `<context name="..." expr="..."/>` | Variables de contexto |

---

## RelationalAttributes (grupo reutilizable)

Disponible en campos relacionales de formulario y `panel-related`:

`target`, `target-name`, `domain`, `edit-window` (`self`/`blank`/`popup`), `form-view`, `grid-view`, `summary-view`, `onSelect`, `canSuggest`, `canSelect`, `canNew`, `canView`, `canEdit`, `canRemove`, `x-can-reload`

---

## AbstractPanel (base de `<panel>`, `<panel-related>`, etc.)

Hereda de `AbstractContainer` que a su vez hereda de `SimpleWidget` → `AbstractWidget`.

| Atributo | Descripción |
|---|---|
| `showFrame` | Mostrar borde del panel (defecto: true) |
| `sidebar` | Mostrar en sidebar |
| `stacked` | Apilar elementos en columna |
| `attached` | Unir con el panel anterior |
| `onTabSelect` | Acción al seleccionar pestaña (si está en panel-tabs) |
| `colSpan` | Columnas que ocupa (ResponsiveNumber) |
| `title` | Título del panel |
| `hidden` | Ocultar (boolean) |
| `readonly` | Solo lectura (boolean) |
| `if` | Condición de visibilidad (heredada de AbstractWidget) |