# Vista `<chart>` — referencia

La vista `<chart>` muestra datos agregados como gráficas 2D (barras, líneas, tarta, etc.) impulsadas por Apache ECharts. A diferencia de `<grid>` o `<form>`, **no está ligada a un modelo de dominio concreto**: sus datos provienen de un `<dataset>` que define el desarrollador (por defecto en el proyecto, una acción Java vía `type="rpc"`; ver §*Dataset*). Se usa principalmente en pantallas de estadísticas y dashboards.

La referencia técnica completa de todos los atributos está en `references/charts.md`.

---

## Estructura básica

```xml
<action-view name="{prefijo}.{Nombre}@{Entidad}-action"
             title="Título de la pantalla">
    <view type="chart" name="{prefijo}.{Nombre}@{Entidad}-chart"/>
</action-view>

<!-- Patrón por defecto: dataset type="rpc" → action-method → servicio -->
<chart name="{prefijo}.{Nombre}@{Entidad}-chart" title="Título de la gráfica">
    <dataset type="rpc">action-{entidad}-{nombre}</dataset>
    <category key="_eje_x" type="text" title="Etiqueta eje X"/>
    <series   key="_valor" type="bar"  title="Etiqueta eje Y"/>
</chart>

<action-method name="action-{entidad}-{nombre}">
    <call class="com.educaflow.subsys.{subsistema}.service.{Entidad}ChartService"
          method="{nombre}"/>
</action-method>
```

El método del servicio obtiene y devuelve los datos (ver §*Dataset* → *type="rpc"* para el método Java completo).

---

## Diferencias clave respecto a `<grid>` y `<form>`

- El `<action-view>` **no lleva atributo `model`** — la gráfica no está ligada a una entidad JPA.
- La `<view>` dentro del `<action-view>` lleva `type="chart"` en lugar de `type="grid"` o `type="form"`.
- Los datos se obtienen mediante el `<dataset>` — por defecto una acción Java (`type="rpc"`), o SQL/JPQL si se pide expresamente — no hay un ORM gestionando el acceso.

---

## Dataset: SQL, JPQL y RPC

El `<dataset>` admite tres tipos (enumerados en `axelor-core/src/main/resources/object-views.xsd`): `sql` y `jpql` resuelven los datos con una consulta declarativa, y `rpc` delega en una **acción Java** que devuelve los datos por código (útil cuando hay que validar los `search-fields` o ejecutar lógica antes de pintar la gráfica).

**REQUIRED — patrón por defecto del proyecto.** Salvo indicación expresa en contrario, toda gráfica sigue el patrón `chart → <action-method> → servicio`:

- **MUST** usar `type="rpc"` en el `<dataset>`, apuntando a un `<action-method>`.
- El controlador (`<action-method>`) **MUST** limitarse a delegar en un método de servicio; el servicio es quien obtiene y devuelve los datos.
- **MUST NOT** usar `type="sql"` ni `type="jpql"` salvo que el usuario lo pida de forma expresa.

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

### type="rpc" — datos calculados por una acción Java

Cuando el tipo es `rpc`, el contenido del `<dataset>` **es el nombre de una acción Axelor** (típicamente un `<action-method>`), **no** una consulta ni un `FQN.Clase:metodo`. `MetaService.getChart(...)` (`axelor-core/src/main/java/com/axelor/meta/service/MetaService.java`, ~línea 483) la ejecuta vía `ActionExecutor`, le pasa los `search-fields` dentro de `request.getData().get("context")` y pinta lo que la acción devuelva en `response.setData(...)`:

```java
if ("rpc".equals(chart.getDataSet().getType())) {
    ActionRequest req = new ActionRequest();
    ActionResponse res = new ActionResponse();
    Map<String, Object> reqData = new HashMap<>();
    reqData.put("context", context);     // search-fields van aquí
    req.setModel(ScriptBindings.class.getName());
    req.setData(reqData);
    req.setAction(string);               // texto del <dataset>
    res = actionExecutor.execute(req);
    data.put("dataset", res.getData());  // el chart pinta esto
}
```

La acción debe devolver una `List<Map<String,Object>>` cuyas claves coincidan **exactamente** con los `key` de `<category>` y `<series>` (incluido el guion bajo si lo usas). El contexto incluye además `__user__`, `__userId__`, `__userCode__`, añadidos por `MetaService`.

**1. Vista del chart** — el `<dataset>` apunta a la acción por su nombre:

```xml
<chart name="subsysCorreos.GraficaDia@Correo-chart" title="Correos por estado (por día)" stacked="true">
    <search-fields>
        <field type="date" name="fechaInicial" title="Fecha inicial"/>
        <field type="date" name="fechaFinal"   title="Fecha final"/>
    </search-fields>
    <dataset type="rpc">action-correo-grafica-dia</dataset>
    <category key="_intervalo" type="date" title="Día"/>
    <series   key="_total"     type="bar"  title="Correos" groupBy="_estado"/>
</chart>
```

**2. Acción que enlaza el `dataset` con el método Java:**

```xml
<action-method name="action-correo-grafica-dia">
    <call class="com.educaflow.subsys.correos.service.CorreoChartService"
          method="graficaDia"/>
</action-method>
```

**3. Método Java** — lee y valida los `search-fields` desde `context` y devuelve la lista de filas:

```java
package com.educaflow.subsys.correos.service;

import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CorreoChartService {

    @SuppressWarnings("unchecked")
    public void graficaDia(ActionRequest request, ActionResponse response) {

        Map<String, Object> ctx =
            (Map<String, Object>) request.getData().get("context");

        LocalDate fechaInicial = toDate(ctx.get("fechaInicial"));
        LocalDate fechaFinal   = toDate(ctx.get("fechaFinal"));

        // Validación de los search-fields
        if (fechaInicial != null && fechaFinal != null
                && fechaInicial.isAfter(fechaFinal)) {
            response.setError(
                I18n.get("La fecha inicial no puede ser posterior a la fecha final."));
            return;
        }

        String sql =
            "SELECT DATE_TRUNC('day', c.fecha_creacion) AS _intervalo, " +
            "       c.estado                            AS _estado, " +
            "       COUNT(*)                            AS _total " +
            "  FROM correos_correo c " +
            " WHERE (CAST(:fechaInicial AS date) IS NULL " +
            "        OR c.fecha_creacion >= CAST(:fechaInicial AS date)) " +
            "   AND (CAST(:fechaFinal AS date) IS NULL " +
            "        OR c.fecha_creacion <= CAST(:fechaFinal AS date)) " +
            " GROUP BY _intervalo, c.estado " +
            " ORDER BY _intervalo";

        Query q = JPA.em().createNativeQuery(sql);
        q.setParameter("fechaInicial", fechaInicial);
        q.setParameter("fechaFinal",   fechaFinal);

        List<Object[]> rows = q.getResultList();
        List<Map<String, Object>> data = rows.stream()
            .map(r -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("_intervalo", r[0]);
                m.put("_estado",    r[1]);
                m.put("_total",     r[2]);
                return m;
            })
            .toList();

        response.setData(data);
    }

    private static LocalDate toDate(Object v) {
        if (v == null || "".equals(v)) return null;
        if (v instanceof LocalDate d) return d;
        return LocalDate.parse(v.toString());
    }
}
```

Trampas a tener en cuenta con `rpc`:

- Las claves del `Map` deben coincidir **exactamente** con los `key` de `<category>`/`<series>`; un alias mal escrito hace que la serie salga vacía sin error.
- Para abortar el render y mostrar un diálogo de error en el cliente, usa `response.setError(mensaje)` y `return`.
- Si el método pertenece a un servicio Guice, el `<action-method>` lo instancia con inyección estándar; no hace falta gestión manual.
- El mismo mecanismo `dataset type="rpc"` funciona en **report-box / informes** (`MetaService.getDataSet(...)`, ~línea 600).

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

- **Chart:** `{prefijo}.{Nombre}@{Entidad}-chart`
  - Ejemplos: `subsysNotificaciones.EnviadosPorDia@Correo-chart`, `subsysRegistroEntradaSalida.PorTipo@Registro-chart`
- **Action-view:** `{prefijo}.{Nombre}@{Entidad}-action` (igual que siempre)

Cuando la gráfica es un dashlet sin entidad clara, puede usarse solo el prefijo:
- `subsysNotificaciones@EstadisticasCorreos-chart`

---

## Ejemplo completo: barras de correos enviados por día

> Este ejemplo ilustra la variante `type="sql"`. Recuerda que el **patrón por defecto del proyecto es `type="rpc"`** (`chart → <action-method> → servicio`, ver la subsección *type="rpc"*); usa SQL solo si se pide explícitamente.

```xml
<action-view name="subsysNotificaciones.EnviadosPorDia@Correo-action"
             title="Correos enviados por día">
    <view type="chart" name="subsysNotificaciones.EnviadosPorDia@Correo-chart"/>
</action-view>

<chart name="subsysNotificaciones.EnviadosPorDia@Correo-chart"
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
<action-view name="subsysNotificaciones.EnviadosPorDia@Correo-action"
             title="Correos enviados">
    <view type="chart" name="subsysNotificaciones.EnviadosPorDia@Correo-chart"/>
</action-view>
```

- **No** lleva atributo `model` en el `<action-view>`.
- **No** lleva `<view type="grid">` ni `<view type="form">` junto al chart — solo el chart.
- Si la pantalla necesita también un grid, se usan dos `<action-view>` separados.
