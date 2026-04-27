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
- Las vistas de menú globales van en `secretariavirtual/menus/`, no en `views/`

## Convenciones de nomenclatura

Ver el skill `/sistemas-knowledge` para la referencia completa de nombres. Resumen:

| Elemento          | Patrón                            | Ejemplo                                    |
|-------------------|-----------------------------------|--------------------------------------------|
| Grid principal    | `{Prefijo}.{Entidad}@Main-grid`   | `subsysSistemaEducativo.Ciclo@Main-grid`   |
| Form principal    | `{Prefijo}.{Entidad}@Main-form`   | `subsysSistemaEducativo.Ciclo@Main-form`   |
| Action principal  | `{Prefijo}.{Entidad}@Main-action` | `subsysSistemaEducativo.Ciclo@Main-action` |
| Grid selector     | `{Prefijo}.{Entidad}@Search-grid` | `subsysSistemaEducativo.Ciclo@Search-grid` |
| Form solo lectura | `{Prefijo}.{Entidad}@View-form`   | `subsysSistemaEducativo.Ciclo@View-form`   |

Prefijos: `subsys{Subsistema}` para subsistemas, `sys{Sistema}` para sistemas.
