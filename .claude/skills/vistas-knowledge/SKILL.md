---
name: vistas-knowledge
description: Referencia sobre los ficheros XML de vistas de Axelor — namespace, tipos de vista, estructura de fichero y convenciones del proyecto.
---

# Vistas de Axelor — referencia

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

| Tipo       | Etiqueta XML | Descripción                         | Skill de referencia      |
|------------|--------------|-------------------------------------|--------------------------|
| Grid       | `<grid>`     | Lista de registros en formato tabla | `/grids-knowledge`       |
| Formulario | `<form>`     | Detalle de un registro editable     | `/formularios-knowledge` |
| Acciones   | `<action-*>` | Lógica asociada a botones y eventos | `/actions-knowledge`     |
| Menú       | `<menuitem>` | Entradas de navegación              | `/menus-knowledge`       |
| Arbol      | `<tree>`     | Un arbol                            | `/tree-knowledge`        |

## Organización de ficheros

- Las vistas de una entidad van en `views/<NombreEntidad>.xml`
- Si hay muchas vistas se pueden agrupar por funcionalidad dentro de `views/`
- Los ficheros `i18n_es.csv` e `i18n_ca.csv` se generan automáticamente — **no crearlos a mano**
- Las vistas de menús van en `secretariavirtual/menus/`, no en `views/`

---

## Vistas de mantenimiento (`@Main`)

Para cada tabla del modelo de dominio siempre hay (salvo indicación contraria) un `<action-view>`, un `<grid>` y un `<form>` de mantenimiento, todos en `views/<NombreEntidad>.xml`.

### `<action-view>` — punto de entrada desde el menú

```xml
<action-view name="subsysSistemaEducativo.Ciclo@Main-action" title="Ciclos" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo">
    <view type="grid" name="subsysSistemaEducativo.Ciclo@Main-grid"/>
    <view type="form" name="subsysSistemaEducativo.Ciclo@Main-form"/>
    <view-param name="show-toolbar-form" value="false"/>
    <view-param name="forceEdit" value="true"/>
</action-view>
```

- El `<view type="grid">` siempre va antes que el `<view type="form">`.
- `show-toolbar-form="false"` — oculta la toolbar del form (siempre).
- `forceEdit="true"` — el form se abre directamente en modo edición.

### `@Main-grid` — listado principal

```xml
<grid name="subsysSistemaEducativo.Ciclo@Main-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      title="" orderBy="name" newButtonTitle="Añadir un nuevo ciclo" allowSearchFields="true"
      canAdvanceSearch="false" canRefresh="false" canNew="true"
      editable="false" edit-icon="false" x-selector="none"
      canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
>
    <field name="code" width="200px"/>
    <field name="name"/>
</grid>
```

Atributos **obligatorios** (ver `grids-knowledge`):
- `title=""` — el título lo da la `action-view`, no el grid.
- `canAdvanceSearch="false" canRefresh="false"` — siempre así.
- `editable="false" edit-icon="false" x-selector="none"` — siempre así.
- `canEdit="false" canDelete="false" canSave="false"` — siempre así.
- `canEditOnClick="true"` — al hacer clic en una fila se abre el form en edición.
- Si no se puede crear desde el grid: `canNew="false"` y quitar `newButtonTitle`.

### `@Main-form` — formulario principal (pantalla completa)

```xml
<form name="subsysSistemaEducativo.Ciclo@Main-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true">
    <panel name="Ciclo" title="Ciclo">
        <field name="code" colSpan="3"/>
        <field name="name" colSpan="6" colOffset="3"/>
        <!-- más campos -->
    </panel>

    <panel-related name="cursos" field="cursos" title="Cursos"
        grid-view="subsysSistemaEducativo.Ciclo.Curso@Main-grid"
        form-view="subsysSistemaEducativo.Ciclo.Curso@Main-form"
        colSpan="12" newButtonTitle="Añadir un nuevo curso"
        showFooter="false" canEdit="false" canRemove="false" forceEdit="true"
    />

    <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
        <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Ciclo@Main-btnDelete-action"
                css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
        <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Ciclo@Main-btnCancel-action"
                colSpan="2" colOffset="6" outline="true"/>
        <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Ciclo@Main-btnSave-action"
                colSpan="2"/>
    </panel>
</form>
```

Atributos **obligatorios** del `<form>` principal (ver skill /formularios-knowledge):
`width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true"`

- **`canBackOnSave="true"`** — solo en el form principal. Hace que Axelor vuelva automáticamente al grid tras guardar, sin necesidad de `force-back` en el action-group.
- **`showIf="(id!=null) || (cid!=null)"`** en btnDelete — `id` es el ID del registro ya guardado; `cid` es el ID temporal de un registro nuevo todavía no guardado. El botón Borrar solo aparece cuando el registro existe (ya sea guardado o recién creado en esta sesión).

Atributos **obligatorios** del `<panel-related>` (ver skill /formularios-knowledge):
`colSpan="12" showFooter="false" canEdit="false" canRemove="false" forceEdit="true"`

### `action-group` de los botones del form **principal**

```xml
<action-group name="subsysSistemaEducativo.Ciclo@Main-btnDelete-action">
    <action name="delete"/>
</action-group>
<action-group name="subsysSistemaEducativo.Ciclo@Main-btnCancel-action">
    <action name="back"/>
</action-group>
<action-group name="subsysSistemaEducativo.Ciclo@Main-btnSave-action">
    <action name="save"/>
</action-group>
```

Acciones predefinidas del framework usadas en el form principal:
- `delete` — borra el registro y vuelve al grid.
- `back` — navega a la vista anterior.
- `save` — guarda; `canBackOnSave="true"` se encarga de volver al grid.

---

## Vistas de referencia (`@Search` y `@View`)

Se usan cuando otros formularios necesitan buscar o mostrar datos de esta entidad en un campo relacional. Van **siempre** en un fichero separado `views/<NombreEntidad>-ref.xml`.

### `@Search-grid` — grid de búsqueda

```xml
<grid name="subsysSistemaEducativo.Ciclo@Search-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      title="" orderBy="name"
      editable="false" edit-icon="false" x-selector="none"
      canNew="false" canEdit="false" canDelete="false" canSave="false" canViewOnClick="true"
>
    <field name="name"/>
    <field name="familiaProfesional"/>
</grid>
```

Diferencias respecto al `@Main-grid`:
- `canNew="false"` — no se puede crear desde aquí.
- `canViewOnClick="true"` en lugar de `canEditOnClick="true"` — al hacer clic se abre el `@View-form`.
- Sin `newButtonTitle`, `allowSearchFields`, `canAdvanceSearch`, `canRefresh` — solo los atributos necesarios.

### `@View-form` — formulario de solo lectura

```xml
<form name="subsysSistemaEducativo.Ciclo@View-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
      width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
    <panel name="Ciclo" title="">
        <field name="name"               colSpan="6" readonly="true"/>
        <field name="familiaProfesional" colSpan="6" readonly="true"/>
    </panel>
</form>
```

- Sin `canBackOnSave` — no es un form de edición.
- Todos los campos tienen `readonly="true"`.
- Sin panel de botones

### Uso en campos relacionales

```xml
<field name="ciclo" colSpan="6"
       grid-view="subsysSistemaEducativo.Ciclo@Search-grid"
       form-view="subsysSistemaEducativo.Ciclo@View-form"/>
```

- `grid-view` → abre el `@Search-grid` para buscar/seleccionar.
- `form-view` → abre el `@View-form` para ver los datos del registro seleccionado.

---

## Pantallas modales (entidades hijas en `panel-related`)

Cuando una entidad tiene hijos que se editan desde un `<panel-related>`, las vistas hijo son **formularios modales** sin `<action-view>` propio.

### `@Main-grid` del hijo — igual que el principal

```xml
<grid name="subsysSistemaEducativo.Ciclo.Curso@Main-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Curso"
      title="" orderBy="name" newButtonTitle="Añadir un nuevo curso" allowSearchFields="true"
      canAdvanceSearch="false" canRefresh="false" canNew="true"
      editable="false" edit-icon="false" x-selector="none"
      canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
>
    <field name="code" width="150px"/>
    <field name="name"/>
</grid>
```

### `@Main-form` del hijo — formulario modal

```xml
<form name="subsysSistemaEducativo.Ciclo.Curso@Main-form" title="Curso" model="com.educaflow.subsystem.sistemaeducativo.db.Curso"
      width="large"
      onNew="subsysSistemaEducativo.Ciclo.Curso@Main-onNew-action"
      canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
    <panel name="Curso" title="">
        <field name="ciclo" showIf="false"/>   <!-- campo padre, oculto pero presente en el modelo -->
        <field name="code" colSpan="3"/>
        <field name="name" colSpan="6" colOffset="3"/>
    </panel>

    <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
        <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnDelete-action"
                css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
        <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnCancel-action"
                colSpan="2" colOffset="6" outline="true"/>
        <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnSave-action"
                colSpan="2"/>
    </panel>
</form>
```

Diferencias respecto al form principal:
- **Sin `canBackOnSave`** — el cierre del modal lo gestiona `save-modal`.
- **Con `onNew`** — inyecta la referencia al padre cuando se crea un registro nuevo.
- **Campo padre con `showIf="false"`** — está en el modelo pero no es visible al usuario.

### `action-group` de los botones del form **modal**

```xml
<action-group name="subsysSistemaEducativo.Ciclo.Curso@Main-btnDelete-action">
    <action name="delete-modal"/>
</action-group>
<action-group name="subsysSistemaEducativo.Ciclo.Curso@Main-btnCancel-action">
    <action name="close"/>
</action-group>
<action-group name="subsysSistemaEducativo.Ciclo.Curso@Main-btnSave-action">
    <action name="save-modal"/>
</action-group>
```

Acciones predefinidas del framework usadas en modales:
- `delete-modal` — borra y cierra el modal.
- `close` — cierra el modal sin guardar.
- `save-modal` — guarda y cierra el modal.

### Inyección del padre (`onNew`)

```xml
<!-- action-group en "Acciones de las tareas principales" -->
<action-group name="subsysSistemaEducativo.Ciclo.Curso@Main-onNew-action">
    <action name="subsysSistemaEducativo.Ciclo.Curso@Main-set-ciclo-parent-action"/>
</action-group>

<!-- action-record en "Acciones básicas que cambian campos simples" -->
<action-record name="subsysSistemaEducativo.Ciclo.Curso@Main-set-ciclo-parent-action"
               model="com.educaflow.subsystem.sistemaeducativo.db.Curso">
    <field name="ciclo" expr="eval: __parent__"/>
</action-record>
```

`__parent__` hace referencia al registro padre activo en la vista anidada.

---

## Tabla comparativa: form principal vs form modal

| Aspecto                | Form principal | Form modal                                |
|------------------------|----------------|-------------------------------------------|
| `canBackOnSave`        | `true`         | ausente                                   |
| `onNew`                | ausente        | presente (inyecta el padre)               |
| Campo padre            | no existe      | `showIf="false"`                          |
| `<action-view>` propio | sí             | no (lo abre el `panel-related` del padre) |
| Botón Borrar acción    | `delete`       | `delete-modal`                            |
| Botón Cancelar acción  | `back`         | `close`                                   |
| Botón Guardar acción   | `save`         | `save-modal`                              |

---

## Jerarquía de modelos

Un fichero de vistas puede contener secciones para múltiples modelos relacionados padre–hijo–nieto. La notación jerárquica usa el **punto como separador**:

- `Ciclo` — nivel raíz (pantalla principal)
- `Ciclo.Curso` — nivel hijo (modal)
- `Ciclo.Curso.CursoModulo` — nivel nieto (modal de segundo nivel)

Esta notación se usa de forma idéntica en cabeceras de comentarios y en **nombres de todos los elementos XML** (`<grid>`, `<form>`, `<action-group>`, `<action-record>`).

---

## Convención de comentarios

### Comentarios de cabecera de sección

Cada sección jerárquica comienza con un bloque de **tres líneas**:

```xml
<!-- *************************************************************************** -->
<!-- ****************************** Ciclo: Vistas ****************************** -->
<!-- *************************************************************************** -->
```

```xml
<!-- ********************************************************************************** -->
<!-- ****************************** Ciclo.Curso : Vistas ****************************** -->
<!-- ********************************************************************************** -->
```

- La línea 2: Se usan **exactamente 30 asteriscos** a ambos lados del texto. 30 asteriscos en el lado izquierdo, luego un espacio, el texto "Vistas de <NombreJerárquico>", otro espacio y 30 asteriscos a la derecha.
- MUY IMPORTANTE: **La línea 1 debe tener tantos asteriscos de forma que el "-->" acabe justo debajo del "-->" de la línea 2. Es decir que debes contar cuantos caracteres tiene la línea 2 hasta el "-->" y poner los asteriscos necesarios para que tenga los mismos caracteres que la línea 2 hasta el "-->".**
- MUY IMPORTANTE: **La línea 3 debe tener tantos asteriscos de forma que el "-->" acabe justo debajo del "-->" de la línea 2. Es decir que debes contar cuantos caracteres tiene la línea 2 hasta el "-->" y poner los asteriscos necesarios para que tenga los mismos caracteres que la línea 2 hasta el "-->".**
- Por favor, es muy importante que revises mediante alguna herramienta de Bash que las líneas 1 y 3 tienen el mismo número de caracteres que la línea 2 hasta el "-->". Y corrijas las líneas 1 y 3 para que tengan el mismo tamaño que la 2. Esto es fundamental para mantener la consistencia visual del código. 


### Comentarios de grupos de acciones

- **Una sola línea** 
- Lo primero es <NombreJerárquico> seguido de dos puntos, un espacio y el texto del grupo de acciones.
- Se usan **exactamente 15 asteriscos** a cada lado del texto
- Los posibles valroes de los comentarios de grupo de acciones son lo siguientes (**siempre en este orden**):
    - `Acciones de las tareas principales` — `<action-group>` de botones y eventos.
    - `Acciones de Validaciones en local` — `<action-validate>` y `<action-condition>`.
    - `Acciones básicas que cambian campos simples` — `<action-record>` y `<action-attrs>`.
    - `Acciones de llamadas Remotas al servidor` — `<action-method>` y `<action-script>`.
- Si el comentario tiene arriba y/o abajo otra línea de asteriscos, se deben eliminar para que solo quede una línea.
- Dentro del bloque "Acciones de las tareas principales"
    - los `<action-group>` deben primero aparecer los de los botones y tras una línea en blanco el resto de eventos (onNew, onLoad, etc.)
    - Si hay varios botones deben aparecer en el orden en que están en el formulario
 
Ejemplos
```xml
<!-- *************** Ciclo : Acciones de las tareas principales *************** -->
<!-- *************** Ciclo : Acciones de Validaciones en local *************** -->
<!-- *************** Ciclo : Acciones básicas que cambian campos simples *************** -->
<!-- *************** Ciclo : Acciones de llamadas Remotas al servidor *************** -->
```
```xml
<!-- *************** Ciclo.Curso : Acciones de las tareas principales *************** -->
<!-- *************** Ciclo.Curso : Acciones de Validaciones en local *************** -->
<!-- *************** Ciclo.Curso : Acciones básicas que cambian campos simples *************** -->
<!-- *************** Ciclo.Curso : Acciones de llamadas Remotas al servidor *************** -->
```

## Ejemplo completo: fichero con un padre y un hijo

Este es el esqueleto XML completo que hay que seguir. Sustituir `Ciclo` y `Curso` por los nombres reales.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <!-- ***************************************************************************** -->
    <!-- ****************************** Vistas de Ciclo ****************************** -->
    <!-- ***************************************************************************** -->

    <action-view name="subsysSistemaEducativo.Ciclo@Main-action" title="Ciclos" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo">
        <view type="grid" name="subsysSistemaEducativo.Ciclo@Main-grid"/>
        <view type="form" name="subsysSistemaEducativo.Ciclo@Main-form"/>
        <view-param name="show-toolbar-form" value="false"/>
        <view-param name="forceEdit" value="true"/>
    </action-view>

    <grid name="subsysSistemaEducativo.Ciclo@Main-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
          title="" orderBy="name" newButtonTitle="Añadir un nuevo ciclo" allowSearchFields="true"
          canAdvanceSearch="false" canRefresh="false" canNew="true"
          editable="false" edit-icon="false" x-selector="none"
          canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
    >
        <field name="code" width="200px"/>
        <field name="name"/>
    </grid>

    <form name="subsysSistemaEducativo.Ciclo@Main-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
          width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true">
        <panel name="Ciclo" title="Ciclo">
            <field name="code" colSpan="3"/>
            <field name="name" colSpan="6" colOffset="3"/>
        </panel>

        <panel-related name="cursos" field="cursos" title="Cursos"
            grid-view="subsysSistemaEducativo.Ciclo.Curso@Main-grid"
            form-view="subsysSistemaEducativo.Ciclo.Curso@Main-form"
            colSpan="12" newButtonTitle="Añadir un nuevo curso"
            showFooter="false" canEdit="false" canRemove="false" forceEdit="true"
        />

        <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
            <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Ciclo@Main-btnDelete-action"
                    css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
            <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Ciclo@Main-btnCancel-action"
                    colSpan="2" colOffset="6" outline="true"/>
            <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Ciclo@Main-btnSave-action"
                    colSpan="2"/>
        </panel>
    </form>

    <!-- *************** Ciclo : Acciones de las tareas principales *************** -->
    <action-group name="subsysSistemaEducativo.Ciclo@Main-btnDelete-action">
        <action name="delete"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Ciclo@Main-btnCancel-action">
        <action name="back"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Ciclo@Main-btnSave-action">
        <action name="save"/>
    </action-group>

    <!-- *************** Ciclo : Acciones de Validaciones en local *************** -->

    <!-- *************** Ciclo : Acciones básicas que cambian campos simples *************** -->

    <!-- *************** Ciclo : Acciones de llamadas Remotas al servidor *************** -->


    <!-- *********************************************************************************** -->
    <!-- ****************************** Vistas de Ciclo.Curso ****************************** -->
    <!-- *********************************************************************************** -->

    <grid name="subsysSistemaEducativo.Ciclo.Curso@Main-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Curso"
          title="" orderBy="name" newButtonTitle="Añadir un nuevo curso" allowSearchFields="true"
          canAdvanceSearch="false" canRefresh="false" canNew="true"
          editable="false" edit-icon="false" x-selector="none"
          canEdit="false" canDelete="false" canSave="false" canEditOnClick="true"
    >
        <field name="code" width="150px"/>
        <field name="name"/>
    </grid>

    <form name="subsysSistemaEducativo.Ciclo.Curso@Main-form" title="Curso" model="com.educaflow.subsystem.sistemaeducativo.db.Curso"
          width="large"
          onNew="subsysSistemaEducativo.Ciclo.Curso@Main-onNew-action"
          canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
        <panel name="Curso" title="">
            <field name="ciclo" showIf="false"/>
            <field name="code" colSpan="3"/>
            <field name="name" colSpan="6" colOffset="3"/>
        </panel>

        <panel name="buttons-panel" title="" colSpan="12" showFrame="false">
            <button name="btnDelete" title="Borrar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnDelete-action"
                    css="btn-danger" colSpan="2" outline="true" showIf="(id!=null) || (cid!=null)"/>
            <button name="btnCancel" title="Cancelar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnCancel-action"
                    colSpan="2" colOffset="6" outline="true"/>
            <button name="btnSave" title="Guardar" onClick="subsysSistemaEducativo.Ciclo.Curso@Main-btnSave-action"
                    colSpan="2"/>
        </panel>
    </form>

    <!-- *************** Ciclo.Curso : Acciones de las tareas principales *************** -->
    <action-group name="subsysSistemaEducativo.Ciclo.Curso@Main-btnDelete-action">
        <action name="delete-modal"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Ciclo.Curso@Main-btnCancel-action">
        <action name="close"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Ciclo.Curso@Main-btnSave-action">
        <action name="save-modal"/>
    </action-group>
    <action-group name="subsysSistemaEducativo.Ciclo.Curso@Main-onNew-action">
        <action name="subsysSistemaEducativo.Ciclo.Curso@Main-set-ciclo-parent-action"/>
    </action-group>

    <!-- *************** Ciclo.Curso : Acciones de Validaciones en local *************** -->

    <!-- *************** Ciclo.Curso : Acciones básicas que cambian campos simples *************** -->
    <action-record name="subsysSistemaEducativo.Ciclo.Curso@Main-set-ciclo-parent-action"
                   model="com.educaflow.subsystem.sistemaeducativo.db.Curso">
        <field name="ciclo" expr="eval: __parent__"/>
    </action-record>

    <!-- *************** Ciclo.Curso : Acciones de llamadas Remotas al servidor *************** -->

</object-views>
```

---

## Fichero `-ref.xml` (Search y View)

Las vistas `@Search-grid` y `@View-form` van **siempre** en `views/<NombreEntidad>-ref.xml`, separadas del fichero principal.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://axelor.com/xml/ns/object-views
https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <grid name="subsysSistemaEducativo.Ciclo@Search-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
          title="" orderBy="name"
          editable="false" edit-icon="false" x-selector="none"
          canNew="false" canEdit="false" canDelete="false" canSave="false" canViewOnClick="true"
    >
        <field name="name"/>
        <field name="familiaProfesional"/>
    </grid>

    <form name="subsysSistemaEducativo.Ciclo@View-form" title="Ciclo" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo"
          width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false">
        <panel name="Ciclo" title="">
            <field name="name"               colSpan="6" readonly="true"/>
            <field name="familiaProfesional" colSpan="6" readonly="true"/>
        </panel>
    </form>

</object-views>
```

Uso en un campo relacional de otro formulario:

```xml
<field name="ciclo" colSpan="6"
       grid-view="subsysSistemaEducativo.Ciclo@Search-grid"
       form-view="subsysSistemaEducativo.Ciclo@View-form"/>
```
