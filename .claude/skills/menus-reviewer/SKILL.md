---
name: menus-reviewer
description: Revisa que los menús `<menuitem>` de Axelor creados o modificados cumplen todas las reglas de /menus-knowledge — nomenclatura, atributos, jerarquía, referencias a action-view y orden.
---

# menus-reviewer

## Propósito

Verificar que los menús (`<menuitem>`) creados o modificados siguen las reglas definidas en `/menus-knowledge`.

## Qué leer

1. El fichero XML de menús en  `src/main/java/com/educaflow/secretariavirtual/menus/`.
2. Los ficheros de vistas XML donde están definidas las `<action-view>` referenciadas.
3. El skill `/menus-knowledge` para tener presentes todas las reglas.

## Ubicación de los menuitems

- [ ] Los menuitems **raíz** están en `src/main/java/com/educaflow/secretariavirtual/menus/`.
- [ ] Los menuitems **hoja** (con `action`) está justo debajo de los menuitems raíz o subsección a los que pertenecen y con una identación

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
- [ ] Los menuitems **hoja** llevan `parent` apuntando al `name` del menuitem raíz o subsección.
- [ ] Los menuitems **hoja** llevan `action` apuntando a una `action-view` existente en el mismo fichero.

## Referencias a `<action-view>`

- [ ] Cada `action` referenciada en los menuitems hoja existe como `<action-view>` en algún fichero XML del proyecto.

## Orden (`order`)

- [ ] El valor de `order` del menuitem raíz nunca se repite. Y los menus item raiz está ordenador por ese número
- [ ] Los menuitems hijo dentro de la misma sección tienen valores de `order` nunca se repiten para ese menuitem raiz y están colocados en el mismo orden visual que el número indicado.

## Resultado

Si todos los checks del checklist final están bien, mostrar únicamente: **OK-No hay problemas**