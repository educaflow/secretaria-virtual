---
name: k-vistas
description: Referencia completa de vistas Axelor — namespace, estructura de ficheros, convenciones de nombres, `<grid>`, `<form>`, `<chart>`, `<panel>`, `<panel-related>`, `<action-view>`, `<action-group>`, `<action-method>`, `<action-record>`, `<action-attrs>`, `<action-validate>`, `<action-condition>`, `<action-script>`, `<menuitem>`.
---

# Vistas de Axelor — referencia

## Ficheros de este skill

| Fichero | Contenido |
|---------|-----------|
| `grids.md` | Cómo definir grids: atributos, columnas, botones, convención de nombres `Main@…-grid` y `Ref@…-grid` |
| `forms.md` | Cómo definir formularios: layout de paneles, campos, widgets, `panel-related`, `panel-tabs` y convención `Main@…-form` y `Ref@…-form` |
| `actions.md` | Cómo definir acciones: `action-group`, `action-method`, `action-record`, `action-attrs`, `action-validate`, `action-condition` y `action-script` |
| `menus.md` | Cómo definir menús: `menuitem`, atributos, orden y convención de nombres |
| `tree.md` | Cómo definir vistas de árbol: `<tree>`, `<node>`, `<column>`, `<button>` y patrones de uso |
| `charts.md` | Cómo definir gráficas: `<chart>`, `<dataset>` (patrón por defecto `type="rpc"` → action-method → servicio; SQL/JPQL solo a petición), `<category>`, `<series>`, tipos de gráfica y convención de nombres |

---

Las vistas son ficheros XML que definen la interfaz de usuario de Axelor. Se ubican en la carpeta `views/` del sistema o subsistema correspondiente.

**IMPORTANTE: Toda vista debe seguir un modelo de dominio existente. Nunca se diseñan vistas sin modelo de dominio definido.**

## Namespace y declaración de fichero

Cada fichero XML de vistas debe tener **exactamente** esta declaración inicial:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

</object-views>
```

## Tipos de vista

| Tipo       | Etiqueta XML | Descripción                                                      | Referencia   |
|------------|--------------|------------------------------------------------------------------|--------------|
| Grid       | `<grid>`     | Lista de registros en formato tabla                              | `grids.md`   |
| Formulario | `<form>`     | Detalle de un registro editable                                  | `forms.md`   |
| Acciones   | `<action-*>` | Lógica asociada a botones y eventos                              | `actions.md` |
| Menú       | `<menuitem>` | Entradas de navegación                                           | `menus.md`   |
| Árbol      | `<tree>`     | Registros en estructura jerárquica de árbol (nodos padre-hijo)   | `tree.md`    |
| Gráfica    | `<chart>`    | Gráficas 2D (barras, líneas, tarta…); dataset por defecto `type="rpc"` (acción→servicio) | `charts.md` |

## Organización de ficheros

- Cada fichero de vistas se llama `views/{Variante}-{Entidad}.xml` (dos palabras UpperCamelCase separadas por guion): `Main-Correo.xml`, `Mis-Correo.xml`, `Ref-Adjunto.xml`, `Pendiente-TareaFirma.xml`
- Cada variante de una entidad vive en **su propio fichero**; el fichero contiene el bloque maestro y, si aplica, los bloques de sus detalles
- Los ficheros `i18n_es.csv` e `i18n_ca.csv` se generan automáticamente — **no crearlos a mano**
- Las vistas de menús van en `secretariavirtual/menus/`, no en `views/`

## Nombre de las vistas y acciones

Un `name` se parte en dos por el `@`:

- **prefijo** — todo lo anterior al `@`. Se compone de:
    - **marcador de módulo** — `subsys{Subsistema}` o `sys{Sistema}`.
    - **variante** — `Main`, `Mis`, `Pendiente`, `Ref`…, que dice para qué sirve el bloque.
- **sufijo** — todo lo posterior al `@`. Empieza por la **ruta de entidad** (`{Entidad}[.{EntidadHija}]*`, de maestro a detalle) y sigue con el tipo de vista o el segmento de acción.

El nombre de las vistas de acción es: `{marcadorMódulo}.[Main|otra variante]@{Entidad}[-{mas cosas}]*-action`

El grid `{marcadorMódulo}.Ref@{Entidad}-grid` se usa como selector en campos many-to-one para abrir un grid de búsqueda específico en lugar del grid por defecto que se abre al pulsar la lupa.
El form `{marcadorMódulo}.Ref@{Entidad}-form` se usa para abrir un form de solo lectura al hacer clic sobre el registro ya seleccionado en lugar del form por defecto que se abre al pulsar la lupa.

El grid `{marcadorMódulo}.Main@{Entidad}[.{EntidadHija}]*-grid` se usa para la pantalla principal de listado de esa entidad.
El form `{marcadorMódulo}.Main@{Entidad}[.{EntidadHija}]*-form` se usa para la pantalla principal de edición de esa entidad.
La acción `{marcadorMódulo}.Main@{Entidad}[.{EntidadHija}]*-action` se usa para abrir la pantalla principal de esa entidad desde el menú o desde otras vistas.

### Marcador de módulo

El **marcador de módulo** es la cabecera del prefijo: el marcador de capa (`subsys`/`sys`) pegado al nombre del módulo/carpeta.

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`
- Excepción: el marcador `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente

Las entidades de la ruta de entidad se separan con `.` (punto), y el prefijo se separa del sufijo con `@`

Para las convenciones de nombre detalladas de cada tipo de vista, ver `grids.md`, `forms.md`, `actions.md` y `menus.md`.

---

## Vistas de mantenimiento (`Main@…`)

Para cada tabla del modelo de dominio siempre hay (salvo indicación contraria) un `<action-view>`, un `<grid>` y un `<form>` de mantenimiento, todos en `views/Main-{Entidad}.xml`.

Ver `actions.md` para el `<action-view>`, `grids.md` para el `Main@…-grid` y `forms.md` para el `Main@…-form` y las pantallas modales.

**Nota sobre grids:** No poner nunca el atributo `archived` en los grid. Borrarlo si existe excepto si se dice explícitamente que tiene que estar.

---

## Vistas de referencia (`Ref@…-grid` y `Ref@…-form`)

Se usan cuando otros formularios necesitan buscar o mostrar datos de esta entidad en un campo relacional. Van **siempre** en un fichero separado `views/Ref-{Entidad}.xml`.

### `Ref@…-grid` — grid de búsqueda

```xml
<grid name="subsysSistemaEducativo.Ref@Ciclo-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      title="" orderBy="name"
      editable="false" edit-icon="false" x-selector="none"
      canNew="false" canEdit="false" canDelete="false" canSave="false" canViewOnClick="true"
>
    <field name="name"/>
    <field name="familiaProfesional"/>
</grid>
```

Diferencias respecto al `Main@…-grid`:
- `canNew="false"` — no se puede crear desde aquí.
- `canViewOnClick="true"` en lugar de `canEditOnClick="true"` — al hacer clic se abre el `Ref@…-form`.
- Sin `newButtonTitle`, `allowSearchFields`, `canAdvanceSearch`, `canRefresh` — solo los atributos necesarios.

### `Ref@…-form` — formulario de solo lectura

```xml
<form name="subsysSistemaEducativo.Ref@Ciclo-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
    <panel name="Ciclo" title="">
        <field name="name"               colSpan="6" readonly="true"/>
        <field name="familiaProfesional" colSpan="6" readonly="true"/>
    </panel>
    <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
        <button name="btnCancel" title="Salir" onClick="subsysSistemaEducativo.Ref@Ciclo-btnCancel-action"
                colSpan="2" colOffset="8" outline="true"/>
    </panel>
</form>

<?sv-primary-actions?>
<action-group name="subsysSistemaEducativo.Ref@Ciclo-btnCancel-action">
    <action name="close"/>
</action-group>
<?sv-validations?>
<?sv-rules?>
<?sv-remotes?>
```

Un `Ref@…-form` es un bloque de **solo lectura**, pero su bloque lleva igualmente las **cinco PI** (las secciones sin contenido quedan vacías); la única con contenido suele ser `<?sv-primary-actions?>` (el botón Salir).

- Sin `canBackOnSave` — no es un form de edición.
- Todos los campos tienen `readonly="true"`.
- **MUST** llevar un `buttons-panel` con un único botón **Salir** (`btnCancel`, `outline="true"`, `colSpan="2" colOffset="8"`) que dispare un `action-group` con `<action name="close"/>`.

### Uso en campos relacionales

```xml
<field name="ciclo" colSpan="6"
       grid-view="subsysSistemaEducativo.Ref@Ciclo-grid"
       form-view="subsysSistemaEducativo.Ref@Ciclo-form"/>
```

- `grid-view` → abre el `Ref@…-grid` para buscar/seleccionar.
- `form-view` → abre el `Ref@…-form` para ver los datos del registro seleccionado.

---

## Jerarquía de modelos

Un fichero de vistas puede contener secciones para múltiples modelos relacionados padre–hijo–nieto. La notación jerárquica usa el **punto como separador**:

- `Ciclo` — nivel raíz (pantalla principal)
- `Ciclo.Curso` — nivel hijo (modal)
- `Ciclo.Curso.CursoModulo` — nivel nieto (modal de segundo nivel)

Esta notación se usa de forma idéntica en los **nombres de todos los elementos XML** (`<grid>`, `<form>`, `<action-group>`, `<action-record>`).

---

## Marcadores de bloque y sección (Processing Instructions)

La estructura de cada fichero de vistas se marca con **Processing Instructions** (PI) XML `<?target …?>`, **NO con comentarios**.
Una PI es un nodo del DOM (a diferencia de un comentario), así que los tests de vistas la localizan y validan con JAXP+XPath igual que el resto de reglas (`agent_docs/view-rules.md`, Categoría 3).
Las PI son la **única fuente de verdad** de la estructura: **MUST NOT** rotular bloques ni secciones con banners de comentarios (corridas de asteriscos, `: Vistas`, `: Acciones …`).

Vocabulario **cerrado** (no existen otras `sv-*`):

| PI | Rol | Precede a |
|---|---|---|
| `<?sv-view?>` | Abre un **bloque** y hace de cabecera de sus vistas | el primer `action-view`/`grid`/`form`/`tree`/`chart` del bloque |
| `<?sv-primary-actions?>` | Sección: acciones principales | `action-group` de botón/evento |
| `<?sv-validations?>` | Sección: validaciones en local | `action-validate`/`action-condition` |
| `<?sv-rules?>` | Sección: reglas que cambian campos simples | `action-record`/`action-attrs` |
| `<?sv-remotes?>` | Sección: llamadas remotas al servidor | `action-method`/`action-script` |

Reglas:
- La **ruta de entidad** de un bloque es la parte del `name` de sus vistas **tras la `@`** (antes del primer `-`): `Ciclo`, `Ciclo.Curso`, `Correo.Adjunto`, `TareaFirma`.
- Un **bloque** es el tramo contiguo de elementos de alto nivel que comparten el mismo **contexto** (marcador de módulo + variante + ruta de entidad, es decir todo lo anterior al primer `-`); empieza en su `<?sv-view?>` y acaba en el siguiente. En un `Ref-*.xml`, `Ref@…-grid` y `Ref@…-form` comparten contexto → **un solo** `<?sv-view?>`.
- En **todo bloque** (mantenimiento, detalle, `Ref`, solo lectura — sin excepción) **MUST** aparecer las cinco PI **una vez cada una y en este orden**, aunque alguna sección quede vacía: `<?sv-view?>` → `<?sv-primary-actions?>` → `<?sv-validations?>` → `<?sv-rules?>` → `<?sv-remotes?>`.
- Cada acción va tras la PI de su sección. El criterio es la **sección**, no el tipo de elemento: un `action-group` que solo valida va bajo `<?sv-validations?>`.
- Dentro de `<?sv-primary-actions?>`: primero los `action-group` de los botones (en el orden en que están en el formulario) y, tras una línea en blanco, el resto de eventos (`onNew`, `onLoad`, …).

Los elementos de alto nivel cuyo `name` **no contiene `@`** (overrides del framework Axelor, p.ej. `user-preferences-form`) no forman bloque y **no** llevan `<?sv-view?>`.

Referencia normativa completa: `agent_docs/view-rules.md` → Categoría 3 (`VAR-3.1`…`VAR-3.6`).

## Ejemplo completo: fichero con un padre y un hijo

Este es el esqueleto XML completo que hay que seguir. Sustituir `Ciclo` y `Curso` por los nombres reales.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <?sv-view?>
    <action-view name="subsysSistemaEducativo.Main@Ciclo-action" title="Ciclos" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo">
        <view type="grid" name="subsysSistemaEducativo.Main@Ciclo-grid"/>
        <view type="form" name="subsysSistemaEducativo.Main@Ciclo-form"/>
        <view-param name="show-toolbar-form" value="false"/>
        <view-param name="forceEdit" value="true"/>
    </action-view>

    <grid name="subsysSistemaEducativo.Main@Ciclo-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
          title="" orderBy="name" newButtonTitle="Añadir un nuevo ciclo" allowSearchFields="true"
          canAdvanceSearch="false" canRefresh="false" canNew="true"
          editable="false" edit-icon="false" x-selector="none"
          canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
    >
        <field name="code" width="200px"/>
        <field name="name"/>
    </grid>

    <form name="subsysSistemaEducativo.Main@Ciclo-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
          width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true">
        <panel name="Ciclo" title="Ciclo">
            <field name="code" colSpan="3"/>
            <field name="name" colSpan="6" colOffset="3"/>
        </panel>

        <panel-related name="cursos" field="cursos" title="Cursos"
            grid-view="subsysSistemaEducativo.Main@Ciclo.Curso-grid"
            form-view="subsysSistemaEducativo.Main@Ciclo.Curso-form"
            colSpan="12" newButtonTitle="Añadir un nuevo curso"
            showFooter="false" canEdit="false" canRemove="false" forceEdit="true"
        />

        <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
            <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Main@Ciclo-btnDelete-action"
                    css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
            <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Main@Ciclo-btnCancel-action"
                    colSpan="2" colOffset="6" outline="true"/>
            <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Main@Ciclo-btnSave-action"
                    colSpan="2"/>
        </panel>
    </form>

    <?sv-primary-actions?>
    <!-- Form PRINCIPAL: validación remota global (DefaultModelController) antes de delete/save.
         Si hay validación local, va como primera acción del grupo (Local-validateSave-action). -->
    <action-group name="subsysSistemaEducativo.Main@Ciclo-btnDelete-action">
        <action name="remote-validationDelete-action"/>
        <action name="delete"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Main@Ciclo-btnCancel-action">
        <action name="back"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Main@Ciclo-btnSave-action">
        <action name="remote-validationSave-action"/>
        <action name="save"/>
        <action name="back"/>
    </action-group>

    <?sv-validations?>

    <?sv-rules?>

    <?sv-remotes?>


    <?sv-view?>
    <grid name="subsysSistemaEducativo.Main@Ciclo.Curso-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Curso"
          title="" orderBy="name" newButtonTitle="Añadir un nuevo curso" allowSearchFields="true"
          canAdvanceSearch="false" canRefresh="false" canNew="true"
          editable="false" edit-icon="false" x-selector="none"
          canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
    >
        <field name="code" width="150px"/>
        <field name="name"/>
    </grid>

    <form name="subsysSistemaEducativo.Main@Ciclo.Curso-form" title="Curso" model="com.educaflow.subsystem.sistemaeducativo.db.Curso"
          width="large"
          onNew="subsysSistemaEducativo.Main@Ciclo.Curso-onNew-action"
          canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
        <panel name="Curso" title="">
            <field name="ciclo" showIf="false"/>
            <field name="code" colSpan="3"/>
            <field name="name" colSpan="6" colOffset="3"/>
        </panel>

        <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
            <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Main@Ciclo.Curso-btnDelete-action"
                    css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
            <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Main@Ciclo.Curso-btnCancel-action"
                    colSpan="2" colOffset="6" outline="true"/>
            <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Main@Ciclo.Curso-btnSave-action"
                    colSpan="2"/>
        </panel>
    </form>

    <?sv-primary-actions?>
    <!-- Form MODAL de un detalle: MUST NOT usar remote-validation* (el maestro puede no existir
         aún en BD). La validación previa al cierre es SOLO la local de cliente, que debe ser lo
         más completa posible (aquí Curso no declara ninguna). Ver forms.md §Form modal. -->
    <action-group name="subsysSistemaEducativo.Main@Ciclo.Curso-btnDelete-action">
        <action name="delete-modal"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Main@Ciclo.Curso-btnCancel-action">
        <action name="close"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Main@Ciclo.Curso-btnSave-action">
        <action name="save-modal"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Main@Ciclo.Curso-onNew-action">
        <action name="subsysSistemaEducativo.Main@Ciclo.Curso-set-ciclo-parent-action"/>
    </action-group>

    <?sv-validations?>

    <?sv-rules?>
    <action-record name="subsysSistemaEducativo.Main@Ciclo.Curso-set-ciclo-parent-action"
                   model="com.educaflow.subsystem.sistemaeducativo.db.Curso">
        <field name="ciclo" expr="eval: __parent__"/>
    </action-record>

    <?sv-remotes?>

</object-views>
```

---

## Fichero `Ref-{Entidad}.xml` (`Ref@…-grid` y `Ref@…-form`)

Las vistas `Ref@…-grid` y `Ref@…-form` van **siempre** en `views/Ref-{Entidad}.xml`, separadas del fichero principal.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <?sv-view?>
    <grid name="subsysSistemaEducativo.Ref@Ciclo-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
          title="" orderBy="name"
          editable="false" edit-icon="false" x-selector="none"
          canNew="false" canEdit="false" canDelete="false" canSave="false" canViewOnClick="true"
    >
        <field name="name"/>
        <field name="familiaProfesional"/>
    </grid>

    <form name="subsysSistemaEducativo.Ref@Ciclo-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
          width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
        <panel name="Ciclo" title="">
            <field name="name"               colSpan="6" readonly="true"/>
            <field name="familiaProfesional" colSpan="6" readonly="true"/>
        </panel>
        <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
            <button name="btnCancel" title="Salir" onClick="subsysSistemaEducativo.Ref@Ciclo-btnCancel-action"
                    colSpan="2" colOffset="8" outline="true"/>
        </panel>
    </form>

    <?sv-primary-actions?>
    <action-group name="subsysSistemaEducativo.Ref@Ciclo-btnCancel-action">
        <action name="close"/>
    </action-group>

    <?sv-validations?>

    <?sv-rules?>

    <?sv-remotes?>

</object-views>
```

Uso en un campo relacional de otro formulario:

```xml
<field name="ciclo" colSpan="6"
       grid-view="subsysSistemaEducativo.Ref@Ciclo-grid"
       form-view="subsysSistemaEducativo.Ref@Ciclo-form"/>
```

---

## Conocimiento detallado por área

| Tema | Fichero | Descripción |
|------|---------|-------------|
| Grids | `grids.md` | Estructura, atributos, convención de nombres y ejemplos de grids |
| Formularios | `forms.md` | Plantilla, layout, campos, widgets y convención de nombres de formularios |
| Acciones | `actions.md` | Tipos de acción, estructura, atributos, convenciones de nombres y ejemplos |
| Menús | `menus.md` | Etiqueta menuitem, atributos, convención de nombres y ejemplos |
| Árbol | `tree.md` | Estructura jerárquica de árbol: `<tree>`, `<column>`, `<node>`, `<field>`, `<button>`, patrones de uso y convención de nombres |
| Gráficas | `charts.md` | Gráficas 2D con `<chart>`, dataset por defecto `type="rpc"` (action-method→servicio; SQL/JPQL solo a petición), `<category>`, `<series>`, tipos de gráfica y convención de nombres |

## Referencias detalladas

- `references/grid.md` — atributos y elementos completos del grid
- `references/form.md` — atributos y elementos completos del formulario
- `references/extensions.md` — tags de extensión (insert, replace, move, attribute)
- `references/widgets.md` — referencia de los 63+ widgets disponibles
- `references/actions.md` — sintaxis completa de todos los tipos de acción
- `references/menu.md` — atributos completos de menuitem
- `references/charts.md` — referencia completa de `<chart>`: atributos, tipos de gráfica, `<dataset>`, `<category>`, `<series>`, `<config>` y `<actions>`
- `../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd` — **schema XSD oficial de Axelor 8.1**: fuente de verdad para verificar qué atributos y etiquetas son válidos en cualquier elemento de vistas (`<grid>`, `<form>`, `<tree>`, `<action-*>`, `<panel>`, `<field>`, `<button>`, etc.) y si un fichero XML está bien formado. Consultar este fichero ante cualquier duda sobre si un atributo existe o cuáles son sus valores permitidos.
