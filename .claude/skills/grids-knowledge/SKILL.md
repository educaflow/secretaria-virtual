---
name: grids-knowledge
description: Referencia básica de grids en Axelor - estructura, atributos, convención de nombres y ejemplos.
---

# Qué es un grid en Axelor

Un grid es la vista tabular de Axelor para listar registros (filas) de un modelo. Se define con el tag `<grid>` y dentro contiene etiquetas `<field>` para mostrar los atributos del modelo.

## Para qué se usa

- Mostrar listados de entidades con sus campos más relevantes.
- Permitir ordenar, agrupar y buscar registros.
- Añadir acciones rápidas con `<button>`

## Ejemplo de grid

```xml
<grid name="subsysSistemaEducativo.Ciclo.Curso@Main-grid" model="com.educaflow.subsystem.sistemaeducativo.db.Curso" canNew="true" newButtonTitle="Añadir un nuevo ciclo" allowSearchFields="true" orderBy="name" canEditOnClick="true" canViewOnClick="true"
      canAdvanceSearch="false" canRefresh="false" editable="false" edit-icon="false" x-selector="none" canEdit="false" canDelete="false" canSave="false"  title=""
>
    <field name="code" width="150px" />
    <field name="name" width="200px" title="Nombre" />
    <field name="leyEducativa"   />
</grid>
```
**IMPORTANTE:**
- En <form> deben estar todos los atributos que se han indicado en la plantilla con los valores indicados.
- Excepciones:
  - Si se pueden crear nuevas entidades desde el grid, añadir `canNew="true" newButtonTitle="Nueva ley educativa"` 
  - Si no se pueden crear nuevas entidades desde el grid, añadir `canNew="false"` y no incluir el atributo `newButtonTitle`
  - Si se pueden editar las entidades desde el grid, añadir `canEditOnClick="true"` y no incluir el atributo `canViewOnClick`
  - Si SOLO pueden ver las entidades desde el grid, añadir `canViewOnClick="true"` y no incluir el atributo `canEditOnClick`
  - Normalmente el atributo `allowSearchFields` valdrá `true` pero se puede poner a `false`.
  - Normalmente el atributo `orderBy` valdrá `name` pero se puede valer otro campo relevante para ordenar los registros como alguna fecha.


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
