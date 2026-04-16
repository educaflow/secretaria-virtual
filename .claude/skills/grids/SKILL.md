---
name: grids
description: Este skill sirve para diseñar y generar dentro de ficheros XML de vistas de Axelor la etiqueta  `<grid>` de Axelor a partir de un modelo de dominio (entidad).
---

## Qué es un grid en Axelor

Un grid es la vista tabular de Axelor para listar registros (filas) de un modelo. Se define con el tag `<grid>` y dentro contiene etiquetas `<field>` para mostrar los atributos del modelo.

## Para qué se usa

- Mostrar listados de entidades con sus campos más relevantes.
- Permitir ordenar, agrupar y buscar registros.
- Añadir acciones rápidas con `<button>`

## Ejemplo de grid

```xml
<grid name="subsysSistemaEducativo.LeyEducativa@Main-grid"  model="com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa"
      canNew="true" newButtonTitle="Nueva ley educativa"
      canRefresh="false" canAdvanceSearch="false" allowSearchFields="false"
      editable="false" edit-icon="false" x-selector="none"  canEdit="false" canDelete="false" canSave="false"
      canEditOnClick="true"
>
    <field name="code" width="200px" />
    <field name="name" />
    <button name="Nombre del botón" title="El título del botón" onClick="Nombre de la acción" />
</grid>
```

En este proyecto se siguen patrones específicos para grids:
- Elegir campos a mostrar en el grid usando `<field>` según los campos del modelo. Se deben mostrar pocos campos y que sean los más relevantes para identificar el registro y mostrar la información más relevante del modelo.
- Si tiene sentido añadir nuevas entidades incluir `canNew="true" newButtonTitle="Nueva ley educativa"` de esa forma el usuario puede crear nuevas entidades desde el grid. Pero no siempre es necesario, depende de la funcionalidad requerida. Si no se pueden crear nuevas entidades desde el grid, añadir `canNew="false"` para no mostrar el botón de creación.
- Añadir casi siempre `canRefresh="false" canAdvanceSearch="false" allowSearchFields="false"` para dejar más limpio el interfaz del grid y con menos opciones al usuario ya que mejora el UX.
- Añadir siempre `editable="false" edit-icon="false" x-selector="none"  canEdit="false" canDelete="false" canSave="false"` para evitar acciones de edición masiva en el propio grid.
- Añadir `canEditOnClick="true"` para permitir abrir el formulario con un solo click en la fila para editar el detalle
- Añadir `canViewOnClick="true"` para permitir abrir el formulario con un solo click en la fila para ver el detalle sin editar
- No se pueden poner a la vez `canEditOnClick` y `canViewOnClick` a `true` porque se solapan, hay que elegir uno u otro según la funcionalidad requerida.
- Definir el atributo `width` del field según el tipo de dato. Por ejemplo, los campos que son números suelen ser estrechos y las descripciones o nombres, más anchos.
- Si se solicita se puede crear un botón al final de cada registro para que haya alguna acción en concreto.

## Nombre de los grids

El nombre de las vistas de grids es:       `{Prefijo}{Entidad}[.{EntidadHija}]*@[Main|Search|otro nombre]-grid`

### Prefijos

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`
- Excepción: el prefijo `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente

Las entidades se separan con `.` (punto) y los nombres de ese formulario o grid con `@`

#### Ejemplos

| Caso                       | Patrón                                                          | Ejemplo                                                |
|----------------------------|-----------------------------------------------------------------|--------------------------------------------------------|
| Grid principal             | `subsys{Subsistema}.{Entidad}@Main-grid`                        | `subsysSistemaEducativo.Ciclo@Main-grid`               |
| Grid de busqueda           | `subsys{Subsistema}.{Entidad}@Search-grid`                      | `subsysSistemaEducativo.Ciclo@Search-grid`             |
| Grid con nombre            | `subsys{Subsistema}.{Entidad}@{Nombre}-grid`                    | `subsysSistemaEducativo.Ciclo@Pendiente-grid`          |
| Entidad anidada            | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}@Main-grid`     | `subsysSistemaEducativo.Ciclo.Curso@Main-grid`         |
| Entidad anidada con estado | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}@{Nombre}-grid` | `subsysSistemaEducativo.Ciclo.Curso@Pendiente-grid`    |


## Referencia

Para detalles completos de atributos y elementos soportados, usar:

- `references/grid.md`

