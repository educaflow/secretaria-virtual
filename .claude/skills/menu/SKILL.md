---
name: menu
description: Usa este skill cuando el usuario quiera crear o modificar entradas de menú (menuitem) en Axelor. Los menús se definen en ficheros XML dentro de secretariavirtual/menus/.
---

# Menús de Axelor

Los menús de Axelor se definen con la etiqueta `<menuitem>` dentro de ficheros XML ubicados en:

```
src/main/java/com/educaflow/secretariavirtual/menus/
```

> El formato del fichero XML (namespace, schema) es el estándar de las vistas de Axelor. Ver skill `/vistas` para más detalles.

## Nombre del fichero

Los ficheros de menú globales van en `secretariavirtual/menus/` con prefijo numérico que indica el orden de aparición en el menú:

```
{NNN}_menuitem_{nombre}.xml
```

Ejemplos:
- `100_menuitem_tramite.xml`
- `300_menuitem_sistemaeducativo.xml`
- `550_menuitem_firma.xml`

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
- Los menuitems **hijo** llevan `parent` apuntando al nombre del menuitem raíz.
- Los menuitems **hoja** llevan `action` apuntando a una `action-view`. Para la convención de nombres de las acciones, ver skill `/actions`.

## Convención de nombres de menuitems

El nombre de los menuitems es: `{Prefijo}[-menuitem | .{Entidad}@{Vista}-menuitem | -{concepto}-menuitem]`

### Prefijos

- Subsistemas: `subsys{Subsistema}` (PascalCase sin separador), p.ej. `subsysFirma`, `subsysRegistroEntradaSalida`
- Sistemas: `sys{Sistema}` (PascalCase sin separador), p.ej. `sysImportar`

Las entidades se separan con `.` (punto) y el nombre de la vista con `@`, igual que en grids y formularios.

#### Ejemplos

| Caso                                | Patrón                                 | Ejemplo                                      |
|-------------------------------------|----------------------------------------|----------------------------------------------|
| Sección raíz (subsistema)           | `subsys{Seccion}-menuitem`             | `subsysFirma-menuitem`                       |
| Sección raíz (sistema)              | `sys{Seccion}-menuitem`                | `sysImportar-menuitem`                       |
| Entrada a entidad (vista principal) | `{Prefijo}.{Entidad}@Main-menuitem`    | `subsysSistemaEducativo.Ciclo@Main-menuitem` |
| Entrada a entidad (otra vista)      | `{Prefijo}.{Entidad}@{Vista}-menuitem` | `subsysFirma.TareaFirma@Pendiente-menuitem`  |

## Ejemplos completos

### Menú simple con subsección

```xml
<menuitem name="subsysSistemaEducativo-menuitem"                  title="Sistema educativo" groups="admins" order="300"/>
<menuitem name="subsysSistemaEducativo.Ciclo@Main-menuitem"       parent="subsysSistemaEducativo-menuitem" title="Ciclos"   action="subsysSistemaEducativo.Ciclo@Main-action"   groups="admins" order="2"/>
<menuitem name="subsysSistemaEducativo.Modulo@Main-menuitem"      parent="subsysSistemaEducativo-menuitem" title="Módulos"  action="subsysSistemaEducativo.Modulo@Main-action"  groups="admins" order="4"/>
```

### Menú con filtros por estado

```xml
<menuitem name="subsysFirma-menuitem"                      title="Firmar documentos" groups="admins" order="550"/>
<menuitem name="subsysFirma.TareaFirma@Todos-menuitem"     parent="subsysFirma-menuitem" title="Todos"      action="subsysFirma.TareaFirma@Todos-action"      groups="admins" order="1"/>
<menuitem name="subsysFirma.TareaFirma@Pendiente-menuitem" parent="subsysFirma-menuitem" title="Pendientes" action="subsysFirma.TareaFirma@Pendiente-action"  groups="admins" order="2"/>
<menuitem name="subsysFirma.TareaFirma@Firmado-menuitem"   parent="subsysFirma-menuitem" title="Firmados"   action="subsysFirma.TareaFirma@Firmado-action"    groups="admins" order="3"/>
```

## Relación con las `action-view`

- Los menuitems **no definen** las `action-view`, solo las referencian.
- Las `action-view` se definen en el fichero de vistas del subsistema/sistema correspondiente o excepcionalmente en el propio XML del menú.

## Cuándo crear un fichero de menú nuevo vs modificar uno existente

- **Modificar existente**: si el nuevo menuitem pertenece a una sección ya definida en un fichero existente.
- **Crear nuevo**: si se trata de una nueva sección raíz o de un subsistema completamente nuevo.
- Respetar el rango numérico del prefijo para mantener el orden visual en el menú.

## Referencia

Para detalle completo de atributos y elementos soportados, usar:

- `references/menu.md`


