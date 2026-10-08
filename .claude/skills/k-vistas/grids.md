# Qué es un grid en Axelor

Un grid es la vista tabular de Axelor para listar registros (filas) de un modelo. Se define con el tag `<grid>` y dentro contiene etiquetas `<field>` para mostrar los atributos del modelo.

## Para qué se usa

- Mostrar listados de entidades con sus campos más relevantes.
- Permitir ordenar, agrupar y buscar registros.
- Añadir acciones rápidas con `<button>`

## Ejemplo de grid

```xml
<grid name="subsysSistemaEducativo.Main@Ciclo.Curso-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Curso" canNew="true" newButtonTitle="Añadir un nuevo ciclo" allowSearchFields="true" orderBy="name" canEditOnClick="true"
      canAdvanceSearch="false" canRefresh="false" editable="false" edit-icon="false" x-selector="none" canEdit="false" canDelete="false" canSave="false"  title=""
>
    <field name="code" width="150px" />
    <field name="name" width="200px" title="Nombre" />
    <field name="leyEducativa"   />
</grid>
```
**IMPORTANTE:**
- En <grid> deben estar todos los atributos que se han indicado en la plantilla con los valores indicados.
- Excepciones:
  - Si se pueden crear nuevas entidades desde el grid, añadir `canNew="true" newButtonTitle="Nueva ley educativa"` 
  - Si no se pueden crear nuevas entidades desde el grid, añadir `canNew="false"` y no incluir el atributo `newButtonTitle`
  - `newButtonTitle` es una modificación nuestra de AOP: cambia el «+» por un botón `primary` con ese texto.
    Vale tanto en la toolbar del grid como en la cabecera de un `<panel-dashlet>` que muestra ese grid: allí el texto se toma del `newButtonTitle` del **grid**.
  - Si se pueden editar las entidades desde el grid, añadir `canEditOnClick="true"` y no incluir el atributo `canViewOnClick`
  - Si SOLO pueden ver las entidades desde el grid, añadir `canViewOnClick="true"` y no incluir el atributo `canEditOnClick`
  - Si el grid declara `action` (ver «Clic sobre la fila»), **MUST NOT** llevar ni `canEditOnClick` ni `canViewOnClick`
  - Normalmente el atributo `allowSearchFields` valdrá `true` pero se puede poner a `false`.
  - Normalmente el atributo `orderBy` valdrá `name` pero se puede valer otro campo relevante para ordenar los registros como alguna fecha.


## Clic sobre la fila: `action` y `actionSignal`

Extensión del fork de AOP: con `action`, un **clic simple** sobre una fila ejecuta esa acción con el **registro en el contexto**
(`handleCellClick` consulta `action` **antes** que `canViewOnClick`/`canEditOnClick`, así que estos dos no llegan a evaluarse);
`actionSignal` es el valor que viaja como `_signal` en ese contexto y, si se omite, vale el `name` del propio grid.
Por eso **un grid con `action` no declara `canViewOnClick` ni `canEditOnClick`**: serían declaraciones muertas
(excepción a la plantilla del `<grid>`; misma exención en `agent_docs/view-rules.md` → `VAR-8.1`).

| Atributo | Para qué sirve |
|---|---|
| `action` | Acción a ejecutar al hacer clic en la fila. Recibe el registro en el contexto. Resuelve a cualquier acción declarada (`agent_docs/view-rules.md` → `VAR-4.1`) |
| `actionSignal` | Valor de `_signal` que se envía en el contexto de esa acción. Si se omite, es el `name` del grid |

```xml
<grid name="sysVentanilla.Main@Tramite-grid" model="com.educaflow.subsystem.expedientes.db.Tramite"
      title="" orderBy="name" allowSearchFields="true"
      action="sysVentanilla.Main@Tramite-seleccionar-action"
      canAdvanceSearch="false" canRefresh="false" canNew="false"
      editable="false" edit-icon="false" x-selector="none"
      canEdit="false" canDelete="false" canSave="false"
>
    <field name="name"/>
</grid>
```

- ✅ CORRECTO: `<grid action="…-seleccionar-action">` **sin** `canEditOnClick` ni `canViewOnClick`.
- ❌ INCORRECTO: `<grid action="…-seleccionar-action" canViewOnClick="true">` (el `action` gana, `canViewOnClick` nunca se evalúa: declaración muerta).
- ❌ INCORRECTO: un grid **sin** `action` y sin `canEditOnClick`/`canViewOnClick` (fila muerta; la exención es solo para los grids con `action`).

## Mensaje de ayuda (`<help>`)

Un grid admite opcionalmente un hijo `<help>` con un mensaje de ayuda que se muestra al usuario sobre el listado. Es una **etiqueta hija**, no un atributo del `<grid>`, y debe ir la **primera**, antes de los `<field>`.

```xml
<grid name="subsysSistemaEducativo.Main@Ciclo-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Ciclo" ...>
    <help>Aquí se listan todos los ciclos que hay en el sistema</help>
    <field name="code" width="200px" />
    <field name="name" />
</grid>
```

- ✅ CORRECTO: `<help>...</help>` como primer hijo dentro de `<grid>`.
- ❌ INCORRECTO: poner el texto en un atributo `help="..."` del `<grid>` (en el grid es etiqueta hija, no atributo; `help` como atributo solo existe en los campos).

## Nombre de los grids

El nombre de las vistas de grids es:       `{marcadorMódulo}.[Main|Ref|otra variante]@{Entidad}[.{EntidadHija}]*-grid`

### Marcador de módulo

El **marcador de módulo** es la cabecera del prefijo (todo lo anterior al `@`): el marcador de capa (`subsys`/`sys`) pegado al nombre del módulo/carpeta.

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`
- Excepción: el marcador `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente

Las entidades de la ruta de entidad se separan con `.` (punto), y el prefijo se separa del sufijo con `@`

#### Ejemplos

| Caso                       | Patrón                                                          | Ejemplo                                                |
|----------------------------|-----------------------------------------------------------------|--------------------------------------------------------|
| Grid principal             | `subsys{Subsistema}.Main@{Entidad}-grid`                        | `subsysSistemaEducativo.Main@Ciclo-grid`               |
| Grid de busqueda           | `subsys{Subsistema}.Ref@{Entidad}-grid`                      | `subsysSistemaEducativo.Ref@Ciclo-grid`             |
| Grid con nombre            | `subsys{Subsistema}.{Variante}@{Entidad}-grid`                    | `subsysSistemaEducativo.Pendiente@Ciclo-grid`          |
| Entidad anidada            | `subsys{Subsistema}.Main@{EntidadPadre}.{EntidadHija}-grid`     | `subsysSistemaEducativo.Main@Ciclo.Curso-grid`         |
| Entidad anidada con estado | `subsys{Subsistema}.{Variante}@{EntidadPadre}.{EntidadHija}-grid` | `subsysSistemaEducativo.Pendiente@Ciclo.Curso-grid`    |

**IMPORTANTE: Es obligatorio seguir esta convención de nombres para facilitar la trazabilidad, la lectura y el mantenimiento del código.**

## Field
Dentro del grid, cada campo se define con la etiqueta `<field>` 

### Atributos de `<field>`
 - `name`: Indica el nombre del atributo del modelo que se va a mostrar en ese campo. Es obligatorio.
 - `width`: Establece un ancho fijo para ese campo en la tabla. Es opcional, pero recomendable para mejorar la legibilidad del grid. Se puede usar cualquier unidad de medida CSS como `px`, `em`, `%`, etc. Si no se establece un ancho, el campo se ajustará automáticamente al contenido. Es normal dejar un campo sin width para que se ajuste al contenido, especialmente si es un campo de texto largo como el nombre de una entidad.
 - `title`: Establece un título para ese campo que se muestra en la cabecera del grid. Es opcional, si no se establece se mostrará el título del atributo del modelo.

## Referencia
Para detalles completos de atributos y elementos soportados:

- `references/grid.md`
