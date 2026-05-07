# Menuitem

## Atributos

| Nombre            | Descripción                                                                 |
|-------------------|-----------------------------------------------------------------------------|
| **name**          | Nombre único del menuitem                                                   |
| parent            | Nombre del menuitem padre                                                   |
| **title**         | Texto visible en el menú                                                    |
| icon              | Nombre del icono a mostrar                                                  |
| icon-background   | Color de fondo del icono (predefinido o código hex HTML)                    |
| action            | Acción a ejecutar al hacer clic (debe ser una `action-view`)                |
| order             | Número de orden de aparición en el menú                                     |
| groups            | Lista separada por comas de grupos de usuarios que pueden ver el menuitem   |
| top               | Si se muestra en la barra superior                                          |
| left              | Si se muestra en el panel izquierdo                                         |
| hidden            | Si se oculta el menuitem                                                    |
| tag               | Etiqueta estática a mostrar sobre el menuitem                               |
| tag-count         | Si se usa el conteo de registros de la acción como etiqueta                 |
| tag-get           | Método a llamar para obtener el valor de la etiqueta                        |
| tag-style         | Estilo visual de la etiqueta: `default`, `important`, `success`, `warning`, `inverse`, `info` |

## Reglas de visibilidad

- Los menuitems **raíz** (sin `parent`) están **restringidos por defecto**: requieren `groups` o `roles` para mostrarse.
- Los menuitems **hijo** (con `parent`) son **visibles para todos por defecto** salvo que se indique `groups` o `roles`.

## Ejemplo completo

```xml
<!-- Menú raíz -->
<menuitem name="subsysFirma-menuitem"
          title="Firmar documentos"
          groups="admins"
          order="550"/>

<!-- Entradas hijo con acción -->
<menuitem name="subsysFirma-pendiente-menuitem"
          parent="subsysFirma-menuitem"
          title="Pendientes"
          action="subsysFirma.TareaFirma@Pendiente-action"
          groups="admins"
          order="1"/>

<!-- Entrada con tag dinámico -->
<menuitem name="subsysFirma-pendiente-menuitem"
          parent="subsysFirma-menuitem"
          title="Pendientes"
          action="subsysFirma.TareaFirma@Pendiente-action"
          tag-count="true"
          tag-style="warning"
          order="1"/>
```
