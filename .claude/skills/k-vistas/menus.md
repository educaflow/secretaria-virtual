# Menús de Axelor

Los menús de Axelor se definen con la etiqueta `<menuitem>` dentro de ficheros XML.

El formato del fichero XML (namespace, schema) es el estándar de las vistas de Axelor. Ver skill `/k-vistas` para más detalles.

## Tipos de menuitems
Existen 2 tipos de menuitems: 
- raiz: son secciones principales del menú, no llevan `action` ni `parent`, solo `title` y `order`.
- hoja: son entradas finales que abren una vista, llevan `action` apuntando a una `action-view` y `parent` apuntando al menuitem raíz o subsección al que pertenecen.

## Ubicación de los menuitems
- **REGLA OBLIGATORIA — fichero único:** TODOS los `<menuitem>` del proyecto se colocan en el ÚNICO fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. Esto aplica también a los menús de subsistemas y sistemas nuevos: sus entradas se AÑADEN a ese fichero existente.
- **MUST NOT** crear ficheros nuevos como `menus-<subsistema>.xml`, `menus-<sistema>.xml` o cualquier otro fichero adicional para menuitems, ni en `secretariavirtual/views/` ni en cualquier otra carpeta. Si un diseño los lista como ficheros a crear, es un error del diseño que debe corregirse antes de implementar.
- Los `<menuitem>` hoja se colocará justo debajo del `<menuitem>` raíz al que pertenece.


## Etiqueta `<menuitem>`

### Atributos

| Atributo  | Descripción                                                                                                                                               | Obligatorio              |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| `name`    | Identificador único del menuitem (ver convención de nombres)                                                                                              | Sí                       |
| `title`   | Texto visible en el menú                                                                                                                                  | Sí                       |
| `order`   | Orden de aparición (número entero que empieza por 1). Siempre debe coincidir con el número del nombre el fichero y no se debe repetir en el mismo submenu | Sí                       |
| `parent`  | Nombre del menuitem padre (para subentradas)                                                                                                              | No (solo menuitems hoja) |
| `action`  | Nombre de la `action-view` que se abre al pulsar                                                                                                          | No (solo menuitems hoja) |
| `icon`    | Icono del menuitem                                                                                                                                        | No                       |
| `groups`  | Grupos de usuarios que ven el menuitem (p.ej. `admins`, `users,admins`)                                                                                   | No                       |
| `if`      | Expresión condicional de visibilidad                                                                                                                      | No                       |

### Reglas

- El menuitem **raíz**:no lleva `action` ni `parent`, solo `title` y `order`. 
- Los menuitems **hoja**: llevan `action` apuntando a una `action-view`. 

### Formato del XML

- **MUST** escribir cada `<menuitem>` en **una única línea**, sin saltos de línea entre atributos.
- **MUST** escribir los atributos siempre en este orden: `name`, `parent`, `title`, `action`, `icon`, `groups`, `if`, `order` — con `order` **SIEMPRE** al final. Los atributos que no apliquen se omiten sin alterar el orden del resto.
- **MUST** separar los atributos con un único espacio (sin alinear en columnas con espacios extra).
- **MUST** indentar según la jerarquía: menuitems raíz a 4 espacios, hijos a 8, nietos a 12 (un nivel más de 4 espacios por cada nivel de `parent`).

**Ejemplos**:

- ✅ CORRECTO: `<menuitem name="registro-entrada-menuitem" parent="registro-menuitem" title="Entrada" action="subsysRegistroEntradaSalida.RegistroEntrada@Main-action" order="1"/>`
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

- El menuitem **raíz**: Se llamará como el título del menú en minúscula pero en formato camelCase y el sufijo `-menuitem`.
- Los menuitems **hoja**: Se llamará como el nobmre del menú padre, un guión, el título del menú en formato calCase y el sufijo `-menuitem`.


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
    <menuitem name="sistemaEducativo-menuitem" title="Sistema educativo" order="1"/>
        <menuitem name="sistemaEducativo-ciclos-menuitem" parent="sistemaEducativo-menuitem" title="Ciclos" action="subsysSistemaEducativo.Ciclo@Main-action" order="1"/>
        <menuitem name="sistemaEducativo-centro-menuitem" parent="sistemaEducativo-menuitem" title="Centro" action="subsysSistemaEducativo.Ciclo@Main-action" order="2"/>

    <menuitem name="firmarDocumentos-menuitem" title="Firmar documentos" order="2"/>
        <menuitem name="firmarDocumentos-todos-menuitem" parent="firmarDocumentos-menuitem" title="Todos" action="subsysFirma.TareaFirma@Todos-action" order="1"/>
        <menuitem name="firmarDocumentos-pendientes-menuitem" parent="firmarDocumentos-menuitem" title="Pendientes" action="subsysFirma.TareaFirma@Pendiente-action" order="2"/>
        <menuitem name="firmarDocumentos-firmados-menuitem" parent="firmarDocumentos-menuitem" title="Firmados" action="subsysFirma.TareaFirma@Firmado-action" order="3"/>
        <menuitem name="firmarDocumentos-rechazados-menuitem" parent="firmarDocumentos-menuitem" title="Rechazados" action="subsysFirma.TareaFirma@Rechazado-action" order="4"/>
```

- Los menuitems hoja van indentados un nivel más que su raíz para visualizar la jerarquía (ver "Formato del XML").
- Se deja una línea en blanco entre cada grupo de menú raíz.

## Referencia
Para detalle completo de atributos y elementos soportados:

- `references/menu.md`