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

## Organización de ficheros

- Un fichero por entidad: `views/<NombreEntidad>.xml`
- Si hay muchas vistas se pueden agrupar por funcionalidad dentro de `views/`
- Los ficheros `i18n_es.csv` e `i18n_ca.csv` se generan automáticamente — **no crearlos a mano**
- Las vistas de menús van en `secretariavirtual/menus/`, no en `views/`

## Vistas de Search y View
- El `<grid>`  de búsqueda (Search) se nombran con el sufijo `@Search-grid` y se usan para mostrar resultados de búsqueda.
- El `<form>` de solo lectura (View) se nombran con el sufijo `@View-form` y se usan para mostrar detalles de un registro sin permitir edición.
- Se usan en los `<field>` de las vistas principales para mostrar información relacionada o para mostrar resultados de búsqueda. 
  - El atributo `grid-view` de un `<field>` apunta a la vista de búsqueda (Search) que se usará para mostrar resultados de búsqueda relacionados con ese campo.
  - El atributo `form-view` de un `<field>` apunta a la vista de solo lectura (View) que se usará para mostrar detalles relacionados con ese campo.
- Este `<grid>`  de búsqueda (Search) y `<form>` de solo lectura (View) siempre debe ir en un fichero llamado `views/<NombreEntidad>-ref.xml`.

### Ejemplo de uso de Search y View
```xml
<field name="centro" colSpan="6" grid-view="subsysCommon.Centro@Search-grid" form-view="subsysCommon.Centro@View-form"/>
```
- En un formulario, un campo de búsqueda de Centro se mostraría con `subsysSistemaEducativo.Centro@Search-grid` y un campo de solo lectura de Centro se mostraría con `subsysSistemaEducativo.Centro@View-form`.
- En el ejemplo anterior, el `<grid>`  de búsqueda y `<form>` de solo lectura de Centro irían en `views/Centro-ref.xml` en el subsistema `subsysCommon`.


