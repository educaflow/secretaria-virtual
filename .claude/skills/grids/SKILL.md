---
name: Grids de Axelor
description: En un fichero XML de vistas para Axelor crea el tag <grid> para crear la típica tabla en HTML de la vista. El grid se crea partir de una descripción en lenguaje natural o de un modelo de axelor
---

Este skill sirve para diseñar y generar dentro de ficheros de vistas la etiqueta  `<grid>` de Axelor a partir de una descripción funcional o de un modelo de dominio.

## Qué es un grid en Axelor

Un grid es la vista tabular de Axelor para listar registros de una entidad. Se define con el tag `<grid>` y normalmente contiene columnas `<field>`, además de opciones de acciones y comportamiento de edición.

Es la típica vista de "listado" que se usa para mostrar múltiples registros de una entidad, con funcionalidades de ordenación, búsqueda, agrupación y edición inline. Y genera el HTML para una app web con tablas.

## Para qué se usa

- Mostrar listados de entidades con sus campos más relevantes.
- Permitir ordenar, agrupar y buscar registros.
- Habilitar edición inline (`editable="true"`) cuando aplica.
- Añadir acciones rápidas con `<button>`, `<toolbar>` y `<menubar>`.
- Resaltar filas o celdas con reglas `<hilite>`.

## Ejemplo de grid

```xml
<grid name="contact-grid" title="Contacts" model="com.axelor.contact.db.Contact" editable="true">
  <toolbar>
	<button name="btnExport" title="Export" onClick="action.contact.export"/>
  </toolbar>

  <hilite background="warning" if="$contains(email, 'gmeil.com')"/>

  <field name="fullName"/>
  <field name="email"/>
  <field name="phone"/>
  <field name="dateOfBirth"/>

  <button name="btnGreet" title="Greet" onClick="action.contact.greet"/>
</grid>
```

## Peculiaridades importantes

- Con `editable="true"`, pulsar `Enter` confirma la fila actual y, en la última, puede crear una nueva.
- Si usas `canMove="true"`, `orderBy` debe apuntar a un único campo entero para secuenciar correctamente.
- En grids hijos (embebidos en formulario o dashlet), se muestran menos botones/menús que en un grid principal.
- En `widget="tree-grid"` hay limitaciones adicionales (por ejemplo, `onSave` no aplica en subitems).
- Las búsquedas avanzadas sobre campos `o2m`/`m2m` pueden devolver duplicados.

## Referencia

Para detalles completos de atributos y elementos soportados, usar:

- `references/grid.md`

