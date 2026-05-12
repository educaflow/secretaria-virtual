# Vista `<chart>` — referencia

La vista `<chart>` muestra datos agregados como gráficas 2D (barras, líneas, tarta, etc.) impulsadas por Apache ECharts. A diferencia de `<grid>` o `<form>`, **no está ligada a un modelo de dominio concreto**: sus datos provienen de una consulta SQL o JPQL que define el desarrollador. Se usa principalmente en pantallas de estadísticas y dashboards.

La referencia técnica completa de todos los atributos está en `references/charts.md`.

---

## Estructura básica

```xml
<action-view name="{prefijo}.{Entidad}@{Nombre}-action"
             title="Título de la pantalla">
    <view type="chart" name="{prefijo}.{Entidad}@{Nombre}-chart"/>
</action-view>

<chart name="{prefijo}.{Entidad}@{Nombre}-chart" title="Título de la gráfica">
    <dataset type="sql">
        <![CDATA[
            SELECT
                campo_agrupacion  AS _eje_x,
                SUM(campo_valor)  AS _valor
            FROM
                nombre_tabla self
            WHERE
                condicion = :parametro
            GROUP BY
                campo_agrupacion
            ORDER BY
                campo_agrupacion
        ]]>
    </dataset>
    <category key="_eje_x" type="text" title="Etiqueta eje X"/>
    <series   key="_valor" type="bar"  title="Etiqueta eje Y"/>
</chart>
```

---

## Diferencias clave respecto a `<grid>` y `<form>`

- El `<action-view>` **no lleva atributo `model`** — la gráfica no está ligada a una entidad JPA.
- La `<view>` dentro del `<action-view>` lleva `type="chart"` en lugar de `type="grid"` o `type="form"`.
- Los datos se obtienen únicamente mediante el `<dataset>` con SQL o JPQL — no hay un ORM gestionando el acceso.

---

## Dataset: SQL vs JPQL

### type="sql" — consulta directa a la base de datos

Usa los nombres reales de las tablas y columnas PostgreSQL. Ventajas: joins complejos, funciones de agregación avanzadas, mejor rendimiento en grandes volúmenes.

```xml
<dataset type="sql">
    <![CDATA[
        SELECT
            SUM(self.amount)   AS amount,
            status.name        AS _sale_stage
        FROM
            crm_opportunity self
        INNER JOIN
            crm_opportunity_status AS status ON status.id = self.opportunity_status
        JOIN
            auth_user AS _user ON _user.id = :__user__
        GROUP BY
            status.name, status.sequence
        ORDER BY
            status.sequence
    ]]>
</dataset>
```

### type="jpql" — consulta orientada a objetos

Usa los nombres de las entidades y campos JPA. Permite navegar relaciones con notación de punto.

```xml
<dataset type="jpql">
    <![CDATA[
        SELECT
            SUM(self.totalAmount) AS amount,
            MONTH(self.orderDate) AS month
        FROM
            Order self
        WHERE
            YEAR(self.orderDate) = YEAR(current_date)
            AND self.orderDate > :fromDateTime
        GROUP BY
            MONTH(self.orderDate)
        ORDER BY
            month
    ]]>
</dataset>
```

### Parámetros del dataset

Los parámetros se pasan con `:nombreParam`. Parámetros especiales del sistema:
- `:__user__` — ID del usuario autenticado (SQL) / objeto User (JPQL)
- `:__group__` — ID del grupo del usuario
- `:__date__` — fecha actual
- `:id` — ID del registro padre cuando la gráfica se embebe dentro de un formulario (ej. `self.timesheet.id = :id`)
- `:_id` — variante de `:id` usada en algunos contextos (ej. `self.forecastRecap.id = :_id`)

Los parámetros personalizados provienen de los `<search-fields>` de la gráfica o del contexto del `<action-view>`.

---

## Campos de búsqueda (`<search-fields>`)

Permiten al usuario filtrar la gráfica mediante inputs. Los valores se usan como parámetros en el dataset.

```xml
<chart name="..." title="...">
    <search-fields>
        <field type="date"      name="fromDate" title="Desde"/>
        <field type="date"      name="toDate"   title="Hasta"/>
        <field type="reference" name="centro"   title="Centro"
               target="com.educaflow.subsystem.gestioncentro.db.Centro"
               domain="..." x-required="true"/>
    </search-fields>
    <dataset type="sql">
        <![CDATA[
            SELECT ... FROM ... WHERE fecha BETWEEN :fromDate AND :toDate
        ]]>
    </dataset>
    ...
</chart>
```

---

## `<category>` — eje X

Define cómo se categoriza (agrupa) la información en el eje horizontal.

| Atributo | Obligatorio | Descripción |
|----------|-------------|-------------|
| `key`    | sí          | Alias del campo en el SELECT que actúa como eje X |
| `type`   | sí          | Tipo: `text`, `numeric`, `date`, `time`, `month`, `year` |
| `title`  | no          | Etiqueta del eje X |

```xml
<category key="_sale_stage" type="text"  title="Estado"/>
<category key="month"       type="month" title="Mes"/>
<category key="fecha"       type="date"  title="Fecha"/>
```

---

## `<series>` — eje Y / datos

Define los valores que se representan gráficamente.

| Atributo    | Obligatorio | Descripción |
|-------------|-------------|-------------|
| `key`       | sí          | Alias del campo en el SELECT que contiene el valor numérico |
| `type`      | sí          | Tipo de gráfica: `bar`, `hbar`, `line`, `area`, `pie`, `donut`, `radar`, `gauge`, `scatter`, `funnel` |
| `groupBy`   | no          | Alias del campo por el que agrupar series (genera una serie por valor distinto) |
| `title`     | no          | Etiqueta del eje Y o de la serie en la leyenda |
| `side`      | no          | Lado del eje Y: `left` (default) o `right` |
| `aggregate` | no          | Función de agregación aplicada al renderizar: `sum`, `avg`, etc. |

```xml
<series key="amount"  type="bar"  title="Importe"/>
<series key="total"   type="line" title="Total" side="right"/>
<series key="importe" type="bar"  title="Por estado" groupBy="_estado"/>
```

---

## Tipos de gráfica disponibles

| Tipo      | Descripción |
|-----------|-------------|
| `bar`     | Barras verticales |
| `hbar`    | Barras horizontales |
| `line`    | Líneas |
| `area`    | Área (línea rellena) |
| `pie`     | Tarta |
| `donut`   | Rosquilla (tarta con hueco central) |
| `funnel`  | Embudo (similar a pie pero en forma de pirámide) |
| `radar`   | Radar / araña |
| `gauge`   | Velocímetro |
| `scatter` | Dispersión |

---

## Atributos del elemento `<chart>`

| Atributo  | Descripción |
|-----------|-------------|
| `name`    | Nombre único de la gráfica |
| `title`   | Título visible en la cabecera |
| `stacked` | `true` para apilar las series en lugar de mostrarlas lado a lado |
| `onInit`  | Acción o lista de acciones separadas por coma que se ejecutan al cargar la gráfica. Útil para pre-rellenar los `<search-fields>` con valores por defecto |

```xml
<!-- onInit con una acción -->
<chart name="..." title="..." onInit="action.hr.chart.set.period.last.month">

<!-- onInit con varias acciones encadenadas -->
<chart name="..." title="..." onInit="action.crm.chart.set.date,action.chart.crm.chart.set.active.team">

<!-- Barras apiladas -->
<chart name="..." title="..." stacked="true">
```

La acción `onInit` recibe el contexto de los `<search-fields>` y puede rellenar sus valores iniciales mediante un `<action-record>`. Ejemplo:

```xml
<action-record model="com.axelor.apps.crm.db.Lead" name="action.crm.chart.set.date">
    <field name="toDateT"   expr="eval:LocalDate.now().atStartOfDay().withHour(23).withMinute(59)"/>
    <field name="fromDateT" expr="eval:LocalDate.now().withDayOfMonth(1).withMonth(1).atStartOfDay()"/>
</action-record>
```

---

## Convención de nombres

Las gráficas no tienen modelo de dominio, pero siguen la misma convención del proyecto adaptada:

- **Chart:** `{prefijo}.{Entidad}@{Nombre}-chart`
  - Ejemplos: `subsysNotificaciones.Correo@EnviadosPorDia-chart`, `subsysRegistroEntradaSalida.Registro@PorTipo-chart`
- **Action-view:** `{prefijo}.{Entidad}@{Nombre}-action` (igual que siempre)

Cuando la gráfica es un dashlet sin entidad clara, puede usarse solo el prefijo:
- `subsysNotificaciones@EstadisticasCorreos-chart`

---

## Ejemplo completo: barras de correos enviados por día

```xml
<action-view name="subsysNotificaciones.Correo@EnviadosPorDia-action"
             title="Correos enviados por día">
    <view type="chart" name="subsysNotificaciones.Correo@EnviadosPorDia-chart"/>
</action-view>

<chart name="subsysNotificaciones.Correo@EnviadosPorDia-chart"
       title="Correos enviados por día">
    <search-fields>
        <field type="date" name="fromDate" title="Desde"/>
        <field type="date" name="toDate"   title="Hasta"/>
    </search-fields>
    <dataset type="sql">
        <![CDATA[
            SELECT
                DATE(self.fecha_envio)   AS _dia,
                COUNT(*)                  AS _total
            FROM
                subsys_notificaciones_correo self
            JOIN
                auth_user AS _user ON _user.id = :__user__
            WHERE
                (:fromDate IS NULL OR self.fecha_envio >= :fromDate)
                AND (:toDate IS NULL OR self.fecha_envio <= :toDate)
            GROUP BY
                DATE(self.fecha_envio)
            ORDER BY
                _dia
        ]]>
    </dataset>
    <category key="_dia"   type="date" title="Día"/>
    <series   key="_total" type="bar"  title="Correos enviados"/>
</chart>
```

---

## Integración con `<action-view>`

```xml
<action-view name="subsysNotificaciones.Correo@EnviadosPorDia-action"
             title="Correos enviados">
    <view type="chart" name="subsysNotificaciones.Correo@EnviadosPorDia-chart"/>
</action-view>
```

- **No** lleva atributo `model` en el `<action-view>`.
- **No** lleva `<view type="grid">` ni `<view type="form">` junto al chart — solo el chart.
- Si la pantalla necesita también un grid, se usan dos `<action-view>` separados.
