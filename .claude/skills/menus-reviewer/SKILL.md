---
name: menus-reviewer
description: Revisa que los menús `<menuitem>` de Axelor creados o modificados cumplen todas las reglas de /menus-knowledge — nomenclatura, atributos, jerarquía, referencias a action-view y orden.
---

# menus-reviewer

## Propósito

Verificar que los menús (`<menuitem>`) creados o modificados siguen las reglas definidas en `/menus-knowledge`.

## Qué leer

1. El fichero XML de menú a revisar (en `secretariavirtual/menus/`).
2. Los ficheros de vistas XML donde están definidas las `<action-view>` referenciadas.
3. El skill `/menus-knowledge` para tener presentes todas las reglas.

## Nombre y ubicación del fichero

- [ ] El fichero está en `src/main/java/com/educaflow/secretariavirtual/menus/`.
- [ ] El nombre sigue el patrón `{NNN}_menuitem_{nombreSistemaOSubsistema}.xml`.
- [ ] El prefijo numérico `{NNN}` indica el orden de aparición y es coherente con los menús existentes.

## Nomenclatura de los `<menuitem>`

Patrón: `{Prefijo}[-menuitem | .{Entidad}@{Vista}-menuitem | -{concepto}-menuitem]`

- [ ] El prefijo es `subsys{Subsistema}` para subsistemas o `sys{Sistema}` para sistemas (PascalCase sin separador).
- [ ] El menuitem raíz (sección) sigue el patrón `subsys{Seccion}-menuitem` o `sys{Seccion}-menuitem`.
- [ ] Los menuitems de entrada a una entidad siguen `{Prefijo}.{Entidad}@{Vista}-menuitem`.
- [ ] Todos los menuitems terminan con el sufijo `-menuitem`.

## Atributos obligatorios de `<menuitem>`

- [ ] `name` — identificador único, sigue la convención de nomenclatura.
- [ ] `title` — texto visible en el menú, claro y descriptivo para el usuario.
- [ ] `order` — número entero que define el orden visual; no se repite dentro del mismo submenú.

## Jerarquía y relaciones

- [ ] El menuitem **raíz** NO tiene atributo `action` ni `parent`.
- [ ] Los menuitems **hijo** llevan `parent` apuntando al `name` del menuitem raíz o subsección.
- [ ] Los menuitems **hoja** (que abren una vista) llevan `action` apuntando a una `action-view` existente.
- [ ] Ningún menuitem hoja tiene hijos.
- [ ] El atributo `parent` de cada menuitem hijo apunta a un menuitem que existe en el mismo u otro fichero.

## Referencias a `<action-view>`

- [ ] Cada `action` referenciada en los menuitems hoja existe como `<action-view>` en algún fichero XML de vistas del proyecto.
- [ ] Las `<action-view>` no están definidas dentro del fichero de menú — están en los ficheros `views/` del sistema/subsistema.

## Grupos de usuarios

- [ ] El atributo `groups` está definido cuando procede (para restringir visibilidad).
- [ ] Los grupos asignados corresponden a los grupos de usuarios que deben ver esa entrada.

## Orden (`order`)

- [ ] El valor de `order` del menuitem raíz coincide con el prefijo numérico `{NNN}` del nombre del fichero.
- [ ] Los menuitems hijo dentro de la misma sección tienen valores de `order` únicos y en progresión lógica.

## Checklist final

- [ ] El fichero está en `secretariavirtual/menus/` con nombre `{NNN}_menuitem_{nombre}.xml`
- [ ] Todos los menuitems siguen la convención de nomenclatura con sufijo `-menuitem`
- [ ] El menuitem raíz no tiene `action` ni `parent`
- [ ] Los menuitems hoja tienen `action` apuntando a una `action-view` que existe
- [ ] Ninguna `action-view` está definida dentro del fichero de menú
- [ ] Los valores de `order` son únicos dentro de cada nivel y son coherentes con el orden visual deseado
- [ ] Los títulos son claros y comprensibles para el usuario final

## Resultado

Si todos los checks del checklist final están bien, mostrar únicamente: **OK-No hay problemas**
