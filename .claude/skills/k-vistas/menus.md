# Menús de Axelor

Los menús de Axelor se definen con la etiqueta `<menuitem>` dentro de ficheros XML.

El formato del fichero XML (namespace, schema) es el estándar de las vistas de Axelor. Ver skill `/k-vistas` para más detalles.

## Tipos de menuitems
Existen 3 tipos de menuitems:
- raiz: son secciones principales del menú, no llevan `action` ni `parent`, solo `title` y `order`.
- hoja: son entradas finales que abren una vista, llevan `action` apuntando a una `action-view` y `parent` apuntando al menuitem raíz o subsección al que pertenecen.
- de ocultación: NO declaran ningún menú de la secretaría virtual — **redefinen** uno que trae Axelor (`menu-team`, `menu-dms`, `menu-admin`…) para que no se muestre. Ver "Ocultar un menú de Axelor" más abajo.

## Ubicación de los menuitems
- **REGLA OBLIGATORIA — dos ficheros, y solo dos:** TODOS los `<menuitem>` del proyecto viven en la carpeta `src/main/java/com/educaflow/secretariavirtual/menus/`, repartidos según su tipo:
  - `menus.xml` — el árbol de menús de la aplicación (menuitems raíz y hoja). Los menús de subsistemas y sistemas nuevos se AÑADEN a este fichero existente.
  - `hide-menus.xml` — SOLO los menuitems de ocultación.
- **MUST NOT** crear ficheros nuevos como `menus-<subsistema>.xml`, `menus-<sistema>.xml` o cualquier otro fichero adicional para menuitems, ni en `secretariavirtual/views/` ni en cualquier otra carpeta. Si un diseño los lista como ficheros a crear, es un error del diseño que debe corregirse antes de implementar.
- **MUST NOT** declarar un menú normal en `hide-menus.xml` ni ocultar un menú desde `menus.xml`: cada fichero tiene su tipo de menuitem y no se mezclan.
- Los `<menuitem>` hoja se colocará justo debajo del `<menuitem>` raíz al que pertenece.

## Ocultar un menú de Axelor
Axelor trae menús propios que la secretaría virtual no usa (`menu-team` "Trabajo en equipo", `menu-dms` "Documentos", `menu-admin` "Administración"). Para quitarlos **NO se toca el código de AOP**: se redefinen desde este módulo en `hide-menus.xml`.

```xml
<menuitem id="secretariaVirtual-hide-menu-dms" name="menu-dms" title="Documents__!!" hidden="true"/>
```

Reglas de un menuitem de ocultación (**MUST**, las tres):
- `name` = el nombre EXACTO del menú de Axelor que se oculta (`menu-team`, `menu-dms`, `menu-admin`…). No lleva sufijo `-menuitem` porque el nombre lo eligió Axelor, no nosotros.
- `hidden="true"`.
- `id` que empieza por `secretariaVirtual-`. **CRITICAL**: sin `id` la ocultación NO funciona — `ViewLoader.importMenu` solo sube la prioridad sobre el menú de Axelor cuando los dos `xmlId` difieren, y el de Axelor es nulo; sin `id` se crea una fila con la MISMA prioridad y el menú sigue viéndose.

No lleva `order` ni `groups` (nunca se pinta, así que no tiene ni posición ni público), y el `title` es irrelevante: se pone el de Axelor con `__!!` para que el generador de i18n no pida traducirlo.

Cómo funciona: `ViewLoader.importMenu` busca la `MetaMenu` por `(name, module)`; como el módulo es distinto crea una fila nueva con `priority = la de axelor-core + 1`, y `MenuUtils.fetchMetaMenu` ordena por prioridad descendente y se queda con la primera de cada `name`. `MenuChecker.canShow` devuelve `false` si `hidden`, y `MenuNode` empareja padres **por nombre**, así que al ocultar el padre desaparece también todo su submenú.


## Etiqueta `<menuitem>`

### Atributos

| Atributo  | Descripción                                                                                                                                               | Obligatorio              |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| `name`    | Identificador único del menuitem (ver convención de nombres)                                                                                              | Sí                       |
| `title`   | Texto visible en el menú                                                                                                                                  | Sí                       |
| `order`   | Orden de aparición (número entero que empieza por 1). No se debe repetir dentro del mismo submenú                                                         | Sí                       |
| `parent`  | Nombre del menuitem padre (para subentradas)                                                                                                              | No (solo menuitems hoja) |
| `action`  | Nombre de la `action-view` que se abre al pulsar                                                                                                          | No (solo menuitems hoja) |
| `icon`    | Icono del menuitem                                                                                                                                        | No                       |
| `groups`  | Grupos que ven el menuitem: **obligatorio** y **exactamente** `admins`, `admins,users` o `users` (no otros roles ni `users,admins`) | Sí |
| `if`      | Expresión condicional de visibilidad                                                                                                                      | No                       |

### Reglas

- El menuitem **raíz**:no lleva `action` ni `parent`, solo `title` y `order`. 
- Los menuitems **hoja**: llevan `action` apuntando a una `action-view`. 
- **MUST** — `groups` es **obligatorio** en todo `<menuitem>` y su valor es **exactamente** uno de `admins`, `admins,users` o `users`. No se admite ningún otro rol ni la variante desordenada `users,admins` (equivalente a `admins,users`).

### Formato del XML

- **MUST** escribir cada `<menuitem>` en **una única línea**, sin saltos de línea entre atributos.
- **MUST** escribir los atributos siempre en este orden: `name`, `parent`, `title`, `action`, `icon`, `groups`, `if`, `order` — con `order` **SIEMPRE** al final. Los atributos que no apliquen se omiten sin alterar el orden del resto.
- **MUST** separar los atributos con un único espacio (sin alinear en columnas con espacios extra).
- **MUST** indentar según la jerarquía: menuitems raíz SIN indentar (0 espacios), hijos a 4, nietos a 8 (4 espacios más por cada nivel de `parent`).

**Ejemplos**:

- ✅ CORRECTO: `<menuitem name="registro-entrada-menuitem" parent="registro-menuitem" title="Entrada" action="subsysRegistroEntradaSalida.Main@RegistroEntrada-action" groups="admins,users" order="1"/>`
- ❌ INCORRECTO: `<menuitem name="registro-entrada-menuitem" order="1" parent="registro-menuitem" title="Entrada" action="..."/>` (`order` no está al final)
- ❌ INCORRECTO: `<menuitem name="x-menuitem"    parent="y-menuitem"   title="X"   order="1"/>` (espacios extra para alinear en columnas)
- ❌ INCORRECTO:
  ```xml
  <menuitem name="x-menuitem"
        parent="y-menuitem"
            title="X"
            order="1"/>
  ```
  (atributos partidos en varias líneas)

## Convención de nombres de menuitems raiz:

- El menuitem **raíz**: se llama como el título del menú en formato camelCase más el sufijo `-menuitem`.
- Los menuitems **hoja**: se llaman como el nombre del menú padre (sin su sufijo `-menuitem`), un guion, el título del menú en formato camelCase y el sufijo `-menuitem`.


#### Ejemplos

| Titulo del menú         | parent                         | Nombre                                      |
|-------------------------|--------------------------------|---------------------------------------------|
| Expedientes             |                                | `expedientes-menuitem`                      |
| Abiertos                | `expedientes-menuitem`         | `expedientes-abiertos-menuitem`             |
| Cerrados por el cliente | `expedientes-menuitem`         | `expedientes-cerradosCliente-menuitem` |
| Registro                |                                | `registro-menuitem`                         |
| Entrada                 | `registro-menuitem`            | `registro-entrada-menuitem`                 |
| Firmar documentos       |                                | `firmarDocumentos-menuitem`                 |
| Todos                   | `firmarDocumentos-menuitem`    | `firmarDocumentos-todos-menuitem`           |
| Pendientes              | `firmarDocumentos-menuitem`    | `firmarDocumentos-pendientes-menuitem`      |
| Firmados                | `firmarDocumentos-menuitem`    | `firmarDocumentos-firmados-menuitem`        |
| Rechazados              | `firmarDocumentos-menuitem`    | `firmarDocumentos-rechazados-menuitem`      |


## Ejemplos completos

### Menú raíz — fichero en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`

```xml
<menuitem name="sistemaEducativo-menuitem" title="Sistema educativo" groups="admins" order="1"/>
    <menuitem name="sistemaEducativo-ciclos-menuitem" parent="sistemaEducativo-menuitem" title="Ciclos" action="subsysSistemaEducativo.Main@Ciclo-action" groups="admins" order="1"/>
    <menuitem name="sistemaEducativo-centro-menuitem" parent="sistemaEducativo-menuitem" title="Centro" action="subsysSistemaEducativo.Main@Ciclo-action" groups="admins" order="2"/>

<menuitem name="firmarDocumentos-menuitem" title="Firmar documentos" groups="admins,users" order="2"/>
    <menuitem name="firmarDocumentos-todos-menuitem" parent="firmarDocumentos-menuitem" title="Todos" action="subsysFirma.Todos@TareaFirma-action" groups="admins,users" order="1"/>
    <menuitem name="firmarDocumentos-pendientes-menuitem" parent="firmarDocumentos-menuitem" title="Pendientes" action="subsysFirma.Pendiente@TareaFirma-action" groups="admins,users" order="2"/>
    <menuitem name="firmarDocumentos-firmados-menuitem" parent="firmarDocumentos-menuitem" title="Firmados" action="subsysFirma.Firmado@TareaFirma-action" groups="admins,users" order="3"/>
    <menuitem name="firmarDocumentos-rechazados-menuitem" parent="firmarDocumentos-menuitem" title="Rechazados" action="subsysFirma.Rechazado@TareaFirma-action" groups="admins,users" order="4"/>
```

- Los menuitems hoja van indentados un nivel más que su raíz para visualizar la jerarquía (ver "Formato del XML").
- Se deja una línea en blanco entre cada grupo de menú raíz.

## Referencia
Para detalle completo de atributos y elementos soportados:

- `references/menu.md`