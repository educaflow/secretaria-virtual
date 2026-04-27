---
name: menus-knowledge
description: Referencia básica de menús Axelor - etiqueta menuitem, atributos, convención de nombres y ejemplos.
---

Menús de Axelor

Los menús de Axelor se definen con la etiqueta `<menuitem>` dentro de ficheros XML.

El formato del fichero XML (namespace, schema) es el estándar de las vistas de Axelor. Ver skill `/vistas-knowledge` para más detalles.

## Tipos de menuitems
Existen 2 tipos de menuitems: 
- raiz: son secciones principales del menú, no llevan `action` ni `parent`, solo `title` y `order`.
- hoja: son entradas finales que abren una vista, llevan `action` apuntando a una `action-view` y `parent` apuntando al menuitem raíz o subsección al que pertenecen.

## Ubicación de los menuitems
Los `<menuitem>` se colocan siempre en el fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`
Como un `<menuitem>` puede depender de otro `<menuitem>` el `<menuitem>` se colocará justo debajo.


## Etiqueta `<menuitem>`

### Atributos

| Atributo  | Descripción                                                                                                                             | Obligatorio              |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| `name`    | Identificador único del menuitem (ver convención de nombres)                                                                            | Sí                       |
| `title`   | Texto visible en el menú                                                                                                                | Sí                       |
| `order`   | Orden de aparición (número entero). Siempre debe coincidir con el número del nombre el fichero y no se debe repetir en el mismo submenu | Sí                       |
| `parent`  | Nombre del menuitem padre (para subentradas)                                                                                            | No (solo menuitems hijo) |
| `action`  | Nombre de la `action-view` que se abre al pulsar                                                                                        | No (solo menuitems hoja) |
| `groups`  | Grupos de usuarios que pueden ver el menuitem                                                                                           | No                       |

### Reglas

- El menuitem **raíz** (sección) no lleva `action` ni `parent`, solo `title` y `order`. 
- Los menuitems **hoja** llevan `action` apuntando a una `action-view`. 

## Convención de nombres de menuitems raiz:

El nombre de los menuitems es: `{Prefijo}[-menuitem | .{Entidad}@{Vista}-menuitem | -{concepto}-menuitem]`

### Prefijos

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`

Las entidades se separan con `.` (punto) y el nombre de la vista con `@`, igual que en grids y formularios.

#### Ejemplos

| Caso                                | Patrón                                 | Ejemplo                                      |
|-------------------------------------|----------------------------------------|----------------------------------------------|
| Sección raíz (subsistema)           | `subsys{NombreSubsistema}-menuitem`    | `subsysFirma-menuitem`                       |
| Sección raíz (sistema)              | `sys{NombreSistema}-menuitem`          | `sysImportar-menuitem`                       |
| Entrada a entidad (vista principal) | `{Prefijo}.{Entidad}@Main-menuitem`    | `subsysSistemaEducativo.Ciclo@Main-menuitem` |
| Entrada a entidad (otra vista)      | `{Prefijo}.{Entidad}@{Vista}-menuitem` | `subsysFirma.TareaFirma@Pendiente-menuitem`  |

## Ejemplos completos

### Menú raíz — fichero en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`

```xml
<menuitem name="subsysSistemaEducativo-menuitem" title="Sistema educativo" order="10"/>
    <menuitem name="subsysSistemaEducativo.Ciclo@Main-menuitem" parent="subsysSistemaEducativo-menuitem" title="Ciclos" action="subsysSistemaEducativo.Ciclo@Main-action"  order="1"/>
    <menuitem name="subsysSistemaEducativo.Centro@Main-menuitem" parent="subsysSistemaEducativo-menuitem" title="Centro" action="subsysSistemaEducativo.Ciclo@Main-action"  order="2"/>
<menuitem name="subsysFirma-menuitem"                  title="Firmar documentos"    order="20"/>
    <menuitem name="subsysFirma.TareaFirma@Todos-menuitem" parent="subsysFirma-menuitem" title="Todos" action="subsysFirma.TareaFirma@Todos-action"  order="1"/>
    <menuitem name="subsysFirma.TareaFirma@Pendiente-menuitem" parent="subsysFirma-menuitem" title="Pendientes" action="subsysFirma.TareaFirma@Pendiente-action"  order="2"/>
    <menuitem name="subsysFirma.TareaFirma@Firmado-menuitem" parent="subsysFirma-menuitem" title="Firmados" action="subsysFirma.TareaFirma@Firmado-action"  order="3"/>
    <menuitem name="subsysFirma.TareaFirma@Rechazado-menuitem" parent="subsysFirma-menuitem" title="Rechazados" action="subsysFirma.TareaFirma@Rechazado-action"  order="4"/>
```

Un detalle, los menuitem hoja, llevan una identación debajo del menuitem raíz al que pertenecen de esa forma se visualiza mejor la jerarquía.

## Referencia
Para detalle completo de atributos y elementos soportados:

- `references/menu.md`