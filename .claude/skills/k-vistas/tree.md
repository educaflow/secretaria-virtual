# Vista `<tree>` — referencia

La vista `<tree>` muestra registros en estructura jerárquica de árbol con múltiples niveles (nodos padre–hijo). Se usa en los mismos sitios que un `<grid>` — como pantalla principal de listado desde menú, como dashlet, o como vista referenciada por un `<action-view>` — con la diferencia de que agrupa los registros en niveles jerárquicos en lugar de mostrarlos en una lista plana. Cuando haya que usar un grid, valorar si tiene sentido usar un tree para añadir agrupación visual.

---

## Estructura básica

```xml
<tree name="{prefijo}.{Nombre}@{Entidad}-tree" title="Título" showHeader="true|false">

    <column name="name" />
    <column name="campo1" width="200px" title="Columna 1"/>
    <column name="campo2" type="date" width="100px" title="Fecha"/>
    <column name="btnAccion" type="button" title="Acción"/>

    <!-- Nodo raíz (agrupador) — sin atributo parent -->
    <node model="com.educaflow.subsystem.xyz.db.ModeloPadre"
          domain="EXISTS (SELECT 1 FROM ModeloHijo h WHERE h.campoPadre=self)">
        <field name="name" as="name"/>
    </node>

    <!-- Nodo hoja (registros reales) — con atributo parent -->
    <node model="com.educaflow.subsystem.xyz.db.ModeloHijo"
          domain="filtroJPQL"
          parent="campoPadre"
          draggable="false"
          onClick="prefijo.Nombre@Entidad-event-action">
        <field name="name"/>
        <field name="campo1"/>
        <field name="campo2"/>
        <button name="btnAccion" onClick="prefijo.Nombre@Entidad-btn-action" icon="help" help="Descripción"/>
    </node>

</tree>
```

---

## Atributos de `<tree>`

| Atributo     | Tipo    | Obligatorio | Descripción                                              |
|--------------|---------|-------------|----------------------------------------------------------|
| `name`       | string  | sí          | Nombre único de la vista (sigue convención de nombres)   |
| `title`      | string  | sí          | Título que se muestra en la cabecera de la pantalla      |
| `showHeader` | boolean | no          | Si se muestran las cabeceras de columna. Default: `true` |
| `groups`     | string  | no          | Grupos Axelor autorizados (separados por coma)           |
| `css`        | string  | no          | Clases CSS adicionales                                   |
| `width`      | string  | no          | Ancho preferido: `mini`, `mid`, `large`, px, %           |

---

## Elemento `<column>`

Define las columnas visibles del árbol. Todos los nodos comparten el mismo conjunto de columnas.

```xml
<column name="name" />
<column name="numeroExpediente" width="100px" title="Número"/>
<column name="fechaUltimoEstado" type="date" width="100px" title="Fecha"/>
<column name="btnNuevo" type="button" title="Nuevo expediente"/>
```

| Atributo    | Tipo   | Descripción                                                                              |
|-------------|--------|------------------------------------------------------------------------------------------|
| `name`      | string | Nombre de la columna (debe coincidir con `as` del `<field>` o `name` del `<button>`)     |
| `title`     | string | Etiqueta visible en la cabecera. Si se omite, se usa el label del campo del modelo       |
| `type`      | enum   | Tipo de la columna: `string`, `integer`, `boolean`, `decimal`, `datetime`, `date`, `enum`, `reference`, `button` |
| `width`     | string | Ancho de la columna: valor en `px` o `%` (p.ej. `100px`, `30%`)                        |
| `target`    | string | Modelo destino para columnas de tipo `reference`                                         |
| `target-name` | string | Campo nombre para columnas de tipo `reference`                                         |
| `selection` | string | Nombre de la selección para columnas de tipo `enum`                                      |
| `widget`    | string | Widget de renderizado personalizado                                                      |

> **Nota:** `colSpan`, `multiple` y `required` están **prohibidos** en `<column>` de un tree.

---

## Elemento `<node>`

Define un nivel del árbol. Se declaran en orden de jerarquía: primero el nodo raíz (sin `parent`), luego los hijos (con `parent`). El árbol puede tener más de dos niveles añadiendo más nodos con sus respectivos `parent`.

```xml
<node model="com.educaflow.subsystem.expedientes.db.Tramite"
      domain="EXISTS (SELECT 1 FROM Expediente e WHERE e.tipoExpediente.tramite=self)"
      orderBy="name">
    <field name="name" as="name"/>
</node>

<node model="com.educaflow.subsystem.expedientes.db.Expediente"
      domain="abierto=true"
      parent="tipoExpediente.tramite"
      draggable="false"
      onClick="subsysExpedientes-event-view-action">
    <field name="name"/>
    <field name="numeroExpediente"/>
    <field name="createdBy"/>
    <field name="nameState"/>
    <field name="fechaUltimoEstado"/>
</node>
```

### Atributos de `<node>`

| Atributo    | Tipo    | Obligatorio | Descripción                                                                                  |
|-------------|---------|-------------|----------------------------------------------------------------------------------------------|
| `model`     | string  | sí          | Nombre completo de la clase del modelo (FQCN)                                                |
| `domain`    | string  | no          | Filtro JPQL para los registros de este nodo. Puede usar parámetros de contexto (`:param`)    |
| `parent`    | string  | no          | Campo de relación hacia el modelo padre. Ausente en el nodo raíz                             |
| `draggable` | boolean | no          | Si el nodo se puede arrastrar. Si hay `parent` y `draggable=true`, permite cambiar el padre  |
| `onClick`   | string  | no          | Acción a ejecutar al hacer clic sobre el nodo (recibe el registro del nodo como contexto)    |
| `onMove`    | string  | no          | Acciones separadas por coma a ejecutar al mover el nodo (drag & drop)                        |
| `orderBy`   | string  | no          | Campo por el que ordenar los registros de este nodo                                          |

---

## Elemento `<field>` dentro de `<node>`

Mapea un campo del modelo a una columna del árbol.

```xml
<field name="name" as="name"/>
<field name="numeroExpediente"/>
<field name="nameState" selection="estado-selection"/>
```

| Atributo    | Tipo   | Descripción                                                                 |
|-------------|--------|-----------------------------------------------------------------------------|
| `name`      | string | Nombre del campo en el modelo                                               |
| `as`        | string | Nombre de la columna del tree a la que se mapea. Si coincide, se puede omitir |
| `selection` | string | Nombre de la selección para renderizar el valor como etiqueta               |

> **Importante:** El nodo raíz (agrupador) normalmente solo necesita un `<field name="name" as="name"/>` para rellenar la primera columna. El nodo hoja debe mapear todos los campos que corresponden a columnas del tree.

---

## Elemento `<button>` dentro de `<node>`

Añade un botón en una columna de tipo `button`. El `name` debe coincidir con el `name` de la `<column type="button">` correspondiente.

```xml
<button name="ayuda" onClick="sysTramites-mostrar-ayuda-action" icon="help" help="Descripción del trámite"/>
```

| Atributo  | Tipo   | Obligatorio | Descripción                                                              |
|-----------|--------|-------------|--------------------------------------------------------------------------|
| `name`    | string | sí          | Debe coincidir con el `name` de la `<column type="button">`              |
| `onClick` | string | sí          | Acción a ejecutar al hacer clic                                          |
| `icon`    | string | no          | Icono del botón (nombre de icono Material o ruta de imagen)              |
| `help`    | string | no          | Texto de tooltip que aparece al pasar el ratón                           |
| `prompt`  | string | no          | Mensaje de confirmación antes de ejecutar la acción                      |

> **Nota:** En botones dentro de `<node>`, los atributos `iconHover`, `link`, `outline`, `size`, `showTitle`, `widget`, `css`, `height`, `width`, `depends`, `colSpan`, `colOffset`, `rowSpan` y `rowOffset` están **prohibidos**.

---

## Integración con `<action-view>`

Para abrir una vista tree desde un menú o acción se usa `type="tree"`:

```xml
<action-view name="subsysExpedientes.Expediente@Esperando-action"
             title="Expedientes esperando"
             model="com.educaflow.subsystem.expedientes.db.Expediente">
    <view type="tree" name="subsysExpedientes.Expediente@Esperando-tree"/>
    <context name="_profile" expr="RESPONSABLE"/>
</action-view>
```

- El atributo `model` del `<action-view>` puede omitirse si el tree tiene nodos de distintos modelos.
- El atributo `home="true"` hace que la vista sea la pantalla de inicio al entrar al menú.
- Los `<context>` del action-view se pasan como parámetros accesibles en los `domain` de los nodos mediante `:nombreParam`.

---

## Patrones de uso habituales

### Árbol de dos niveles: agrupador + registros

El patrón más común. El nodo raíz actúa como agrupador usando un `EXISTS` para filtrar solo los grupos que tienen hijos. El nodo hijo filtra los registros reales.

```xml
<tree name="subsysExpedientes.Expediente@Esperando-tree" title="Expedientes">

    <column name="name"/>
    <column name="numeroExpediente" width="100px" title="Número Expediente"/>
    <column name="nameState" width="30%" title="Estado"/>
    <column name="fechaUltimoEstado" type="date" width="100px" title="Fecha"/>

    <!-- Nodo agrupador: agrupa por tipo de trámite -->
    <node model="com.educaflow.subsystem.expedientes.db.Tramite"
          domain="EXISTS (SELECT 1 FROM Expediente e WHERE e.abierto=true AND e.tipoExpediente.tramite=self)">
        <field name="name" as="name"/>
    </node>

    <!-- Nodo hoja: los expedientes reales -->
    <node model="com.educaflow.subsystem.expedientes.db.Expediente"
          domain="abierto=true"
          parent="tipoExpediente.tramite"
          draggable="false"
          onClick="subsysExpedientes-event-view-action">
        <field name="name"/>
        <field name="numeroExpediente"/>
        <field name="nameState"/>
        <field name="fechaUltimoEstado"/>
    </node>

</tree>
```

### Árbol con columnas de botones

Se define una `<column type="button">` en el árbol y un `<button>` en el nodo hoja con el mismo `name`. El nodo raíz no necesita declarar el botón (aparecerá vacío en esa columna para los agrupadores).

```xml
<tree name="sysTramites-nuevo-tree" title="Trámites disponibles" showHeader="false">

    <column name="name"/>
    <column name="nuevo" type="button" title="Nuevo expediente"/>
    <column name="ayuda" type="button" title="Ayuda"/>

    <!-- Agrupador por tipo de trámite -->
    <node model="com.educaflow.subsystem.expedientes.db.TipoTramite"
          domain="EXISTS (SELECT 1 FROM Tramite t WHERE t.tipoTramite=self)">
        <field name="name" as="name"/>
    </node>

    <!-- Trámites con botones de acción -->
    <node model="com.educaflow.subsystem.expedientes.db.Tramite"
          parent="tipoTramite"
          draggable="false"
          onClick="subsysExpedientes-event-new-action">
        <field name="name" as="name"/>
        <button name="ayuda" onClick="sysTramites-mostrar-ayuda-action" icon="help" help="Descripción del trámite"/>
    </node>

</tree>
```

### Árbol con parámetros de búsqueda (desde dashboard)

Cuando el tree se usa dentro de un dashlet con `<search-fields>`, los parámetros de búsqueda se pasan al `domain` de los nodos mediante `:nombreParam`:

```xml
<tree name="subsysExpedientes.Expediente@Buscar-tree" title="Expedientes">

    <column name="name" title="Nombre"/>
    <column name="numeroExpediente" width="100px" title="Número"/>
    <column name="fechaUltimoEstado" type="date" width="100px" title="Fecha"/>

    <node model="com.educaflow.subsystem.expedientes.db.Tramite"
          domain="EXISTS (SELECT 1 FROM Expediente e
                          WHERE e.abierto=:estado
                          AND ((:anyo is null) OR (:anyo=0) OR (YEAR(e.fechaUltimoEstado)=:anyo))
                          AND e.tipoExpediente.tramite=self)">
        <field name="name" as="name"/>
    </node>

    <node model="com.educaflow.subsystem.expedientes.db.Expediente"
          domain="(self.abierto=:estado) AND ((:anyo is null) OR (:anyo=0) OR (YEAR(self.fechaUltimoEstado)=:anyo))"
          parent="tipoExpediente.tramite"
          draggable="false"
          onClick="subsysExpedientes-event-view-action">
        <field name="name"/>
        <field name="numeroExpediente"/>
        <field name="fechaUltimoEstado"/>
    </node>

</tree>
```

---

## Convención de nombres

Las vistas tree siguen la misma convención general del proyecto:

- `{Prefijo}.{Nombre}@{Entidad}-tree` para trees de mantenimiento o funcionales — **variante antes** de la `@`, **entidad después**.
- Ejemplo en formato nuevo: para la variante `Esperando` de la entidad `Solicitud` en un subsistema `X` → `subsysX.Esperando@Solicitud-tree`.
- Ejemplos:
  - `subsysExpedientes.Expediente@Esperando-tree`
  - `subsysExpedientes.Expediente@PruebaSearch-buscar-tree`
  - `sysTramites-nuevo-tree` (tree de sistema sin jerarquía de entidad)

> **Nota:** los ejemplos `subsysExpedientes.*` de este fichero conservan **a propósito** el formato legado `{Entidad}@{Variante}` (entidad antes de la `@`), porque el framework de expedientes no se ha migrado. **No los tomes como plantilla**: en un módulo NO-expedientes se aplica el formato nuevo `{Prefijo}.{Variante}@{Entidad}-tree` de arriba.

La action-view que abre el tree sigue la convención normal: `{Prefijo}.{Nombre}@{Entidad}-action`.
