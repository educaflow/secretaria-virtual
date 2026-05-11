---
name: k-vistas
description: Referencia completa de vistas Axelor — namespace, estructura de ficheros, convenciones de nombres, `<grid>`, `<form>`, `<chart>`, `<panel>`, `<panel-related>`, `<action-view>`, `<action-group>`, `<action-method>`, `<action-record>`, `<action-attrs>`, `<action-validate>`, `<action-condition>`, `<action-script>`, `<menuitem>`.
---

# Vistas de Axelor — referencia

## Ficheros de este skill

| Fichero | Contenido |
|---------|-----------|
| `grids.md` | Cómo definir grids: atributos, columnas, botones, convención de nombres `@Main-grid` y `@Search-grid` |
| `forms.md` | Cómo definir formularios: layout de paneles, campos, widgets, `panel-related`, `panel-tabs` y convención `@Main-form` y `@View-form` |
| `actions.md` | Cómo definir acciones: `action-group`, `action-method`, `action-record`, `action-attrs`, `action-validate`, `action-condition` y `action-script` |
| `menus.md` | Cómo definir menús: `menuitem`, atributos, orden y convención de nombres |
| `tree.md` | Cómo definir vistas de árbol: `<tree>`, `<node>`, `<column>`, `<button>` y patrones de uso |
| `charts.md` | Cómo definir gráficas: `<chart>`, `<dataset>`, `<category>`, `<series>`, tipos de gráfica, SQL/JPQL y convención de nombres |

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
| Gráfica    | `<chart>`    | Gráficas 2D (barras, líneas, tarta…) basadas en consultas SQL/JPQL | `charts.md` |

## Organización de ficheros

- Las vistas de una entidad van en `views/<NombreEntidad>.xml`
- Si hay muchas vistas se pueden agrupar por funcionalidad dentro de `views/`
- Los ficheros `i18n_es.csv` e `i18n_ca.csv` se generan automáticamente — **no crearlos a mano**
- Las vistas de menús van en `secretariavirtual/menus/`, no en `views/`

## Nombre de las vistas y acciones

El nombre de las vistas de acción es: `{Prefijo}.{Entidad}@[Main|otro nombre][-{mas cosas}]*-action`

El grid `{Prefijo}.{Entidad}@Search-grid` se usa como selector en campos many-to-one para abrir un grid de búsqueda específico en lugar del grid por defecto que se abre al pulsar la lupa.
El form `{Prefijo}.{Entidad}@View-form` se usa para abrir un form de solo lectura al hacer clic sobre el registro ya seleccionado en lugar del form por defecto que se abre al pulsar la lupa.

El grid `{Prefijo}.{Entidad}[.{EntidadHija}]*@Main-grid` se usa para la pantalla principal de listado de esa entidad.
El form `{Prefijo}.{Entidad}[.{EntidadHija}]*@Main-form` se usa para la pantalla principal de edición de esa entidad.
La acción `{Prefijo}.{Entidad}[.{EntidadHija}]*@Main-action` se usa para abrir la pantalla principal de esa entidad desde el menú o desde otras vistas.

### Prefijos

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`
- Excepción: el prefijo `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente

Las entidades se separan con `.` (punto) y los nombres de ese formulario o grid con `@`

Para las convenciones de nombre detalladas de cada tipo de vista, ver `grids.md`, `forms.md`, `actions.md` y `menus.md`.

---

## Vistas de mantenimiento (`@Main`)

Para cada tabla del modelo de dominio siempre hay (salvo indicación contraria) un `<action-view>`, un `<grid>` y un `<form>` de mantenimiento, todos en `views/<NombreEntidad>.xml`.

Ver `actions.md` para el `<action-view>`, `grids.md` para el `@Main-grid` y `forms.md` para el `@Main-form` y las pantallas modales.

**Nota sobre grids:** No poner nunca el atributo `archived` en los grid. Borrarlo si existe excepto si se dice explícitamente que tiene que estar.

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

## Jerarquía de modelos

Un fichero de vistas puede contener secciones para múltiples modelos relacionados padre–hijo–nieto. La notación jerárquica usa el **punto como separador**:

- `Ciclo` — nivel raíz (pantalla principal)
- `Ciclo.Curso` — nivel hijo (modal)
- `Ciclo.Curso.CursoModulo` — nivel nieto (modal de segundo nivel)

Esta notación se usa de forma idéntica en cabeceras de comentarios y en **nombres de todos los elementos XML** (`<grid>`, `<form>`, `<action-group>`, `<action-record>`).

---

## Convención de comentarios

- Todos los comentarios empiezan siempre por el nobmre jetarquico de la sección a la que se refieren, seguido de un espacio, dos puntos, un espacio y el texto del comentario.

### Comentarios de cabecera de sección

Cada sección jerárquica comienza con un bloque de **tres líneas**:

```xml
<!-- **************************************************************************** -->
<!-- ****************************** Ciclo : Vistas ****************************** -->
<!-- **************************************************************************** -->
```

```xml
<!-- ********************************************************************************** -->
<!-- ****************************** Ciclo.Curso : Vistas ****************************** -->
<!-- ********************************************************************************** -->
```
- El texto del comentario del ésto es "Vistas" y como ya hemos dijo, delante va el nombre jerarquico (y no al revés, eso era antes)
- La línea 2: Se usan **exactamente 30 asteriscos** a ambos lados del texto. 30 asteriscos en el lado izquierdo, luego un espacio, el texto "Vistas de <NombreJerárquico>", otro espacio y 30 asteriscos a la derecha.
- MUY IMPORTANTE: **La línea 1 debe tener tantos asteriscos de forma que el `-->` acabe justo debajo del `-->` de la línea 2. Es decir que debes contar cuantos caracteres tiene la línea 2 hasta el `-->` y poner los asteriscos necesarios para que tenga los mismos caracteres que la línea 2 hasta el `-->`.**
- MUY IMPORTANTE: **La línea 3 debe tener tantos asteriscos de forma que el `-->` acabe justo debajo del `-->` de la línea 2. Es decir que debes contar cuantos caracteres tiene la línea 2 hasta el `-->` y poner los asteriscos necesarios para que tenga los mismos caracteres que la línea 2 hasta el `-->`.**
- Por favor, es muy importante que revises mediante alguna herramienta de Bash que las líneas 1 y 3 tienen el mismo número de caracteres que la línea 2 hasta el `-->`. Y corrijas las líneas 1 y 3 para que tengan el mismo tamaño que la 2. Esto es fundamental para mantener la consistencia visual del código. 


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

    <!-- *************************************************************************** -->
    <!-- ****************************** Ciclo: Vistas ****************************** -->
    <!-- *************************************************************************** -->

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


    <!-- ********************************************************************************** -->
    <!-- ****************************** Ciclo.Curso : Vistas ****************************** -->
    <!-- ********************************************************************************** -->

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

---

## Conocimiento detallado por área

| Tema | Fichero | Descripción |
|------|---------|-------------|
| Grids | `grids.md` | Estructura, atributos, convención de nombres y ejemplos de grids |
| Formularios | `forms.md` | Plantilla, layout, campos, widgets y convención de nombres de formularios |
| Acciones | `actions.md` | Tipos de acción, estructura, atributos, convenciones de nombres y ejemplos |
| Menús | `menus.md` | Etiqueta menuitem, atributos, convención de nombres y ejemplos |
| Árbol | `tree.md` | Estructura jerárquica de árbol: `<tree>`, `<column>`, `<node>`, `<field>`, `<button>`, patrones de uso y convención de nombres |
| Gráficas | `charts.md` | Gráficas 2D con `<chart>`, dataset SQL/JPQL, `<category>`, `<series>`, tipos de gráfica y convención de nombres |

## Referencias detalladas

- `references/grid.md` — atributos y elementos completos del grid
- `references/form.md` — atributos y elementos completos del formulario
- `references/extensions.md` — tags de extensión (insert, replace, move, attribute)
- `references/widgets.md` — referencia de los 63+ widgets disponibles
- `references/actions.md` — sintaxis completa de todos los tipos de acción
- `references/menu.md` — atributos completos de menuitem
- `references/charts.md` — referencia completa de `<chart>`: atributos, tipos de gráfica, `<dataset>`, `<category>`, `<series>`, `<config>` y `<actions>`
- `references/object-views.xsd` — **schema XSD oficial de Axelor 8.1**: fuente de verdad para verificar qué atributos y etiquetas son válidos en cualquier elemento de vistas (`<grid>`, `<form>`, `<tree>`, `<action-*>`, `<panel>`, `<field>`, `<button>`, etc.) y si un fichero XML está bien formado. Consultar este fichero ante cualquier duda sobre si un atributo existe o cuáles son sus valores permitidos.
