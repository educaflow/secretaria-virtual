---
name: actions-reviewer
description: Revisa que las acciones XML de Axelor creadas o modificadas cumplen todas las reglas de /actions-knowledge — nomenclatura, tipos, orden en fichero y comentarios obligatorios.
---

# actions-reviewer

## Propósito

Verificar que las acciones (`<action-view>`, `<action-method>`, `<action-attrs>`, `<action-record>`, `<action-group>`, `<action-validate>`, `<action-condition>`, `<action-script>`) creadas o modificadas siguen las reglas definidas en `/actions-knowledge`.

## Qué leer

1. Los ficheros XML de vistas que contienen las acciones a revisar.
2. El skill `/actions-knowledge` para tener presentes todas las reglas.

## Nomenclatura de acciones

Patrón obligatorio: `{Prefijo}.{Entidad}[.{Entidad}]*@{Vista}-[Local-|Remote-|set-]{explicacion}-action`

- [ ] El prefijo es `subsys{Subsistema}` para subsistemas o `sys{Sistema}` para sistemas (PascalCase sin separador).
- [ ] El nombre de la entidad coincide exactamente con el nombre de la clase Java.
- [ ] El identificador de vista (`@{Vista}`) refleja el contexto correcto (`@Main`, `@Pendiente`, etc.).
- [ ] Las `<action-validate>` y `<action-condition>` llevan prefijo `Local-` (son validaciones client-side).
- [ ] Las `<action-method>` y `<action-script>` llevan prefijo `Remote-` (son llamadas al servidor).
- [ ] Las `<action-record>` y `<action-attrs>` llevan prefijo `set-` con la forma `set-{campo}-{valor}` o `set-{campo}.{atributo}-{valor}`.
- [ ] Las `<action-group>` y `<action-view>` no llevan ningún prefijo `Local-`/`Remote-`/`set-`.
- [ ] Todas las acciones terminan con el sufijo `-action`.

## Tipos y estructura interna

- [ ] `<action-view>`: incluye primero `<view type="grid" .../>` y luego `<view type="form" .../>` (en ese orden si hay ambas).
- [ ] `<action-view>`: lleva `<view-param name="show-toolbar-form" value="false"/>` siempre.
- [ ] `<action-view>`: si el grid no tiene botón nuevo, lleva `<view-param name="show-toolbar-grid" value="false"/>`.
- [ ] `<action-method>`: el atributo `class` apunta a una clase Java real con el método indicado en `method`.
- [ ] `<action-method>`: el método Java referenciado lleva `@CallMethod`.
- [ ] Todas las acciones referenciadas desde botones, eventos (`onSave`, `onChange`, `onLoad`, `onNew`) u otras acciones existen en algún fichero XML del proyecto.
- [ ] Todas las acciones definidas son referenciadas por alguien (no hay acciones huérfanas).

## Orden de acciones en el fichero

El orden obligatorio dentro del fichero XML es:

1. `<action-view>`
2. `<grid>`
3. `<form>`
4. `<action-group>` (tareas principales)
5. `<action-validate>` y `<action-condition>` (validaciones locales)
6. `<action-record>` y `<action-attrs>` (cambios simples de campos)
7. `<action-method>` y `<action-script>` (llamadas remotas)

- [ ] Las acciones respetan este orden.

## Comentarios separadores obligatorios

- [ ] Existe el bloque de comentario antes de los `<action-group>` de tareas principales:
  `<!-- ***** Acciones de las tareas principales ***** -->`
- [ ] Existe el bloque de comentario antes de las validaciones locales:
  `<!-- ***** Acciones de Validaciones en local ***** -->`
- [ ] Existe el bloque de comentario antes de las acciones básicas:
  `<!-- ***** Acciones básicas que cambian campos simples ***** -->`
- [ ] Existe el bloque de comentario antes de las llamadas remotas:
  `<!-- ***** Acciones de llamadas Remotas al servidor ***** -->`

## Checklist final

- [ ] Todos los nombres de acciones siguen el patrón `{Prefijo}.{Entidad}[.{Entidad}]*@{Vista}-[prefijo]{explicacion}-action`
- [ ] Los prefijos `Local-`, `Remote-` y `set-` se usan solo en los tipos de acción que corresponde
- [ ] El tipo de acción elegido (etiqueta XML) es correcto para la funcionalidad implementada
- [ ] El orden de acciones en el fichero respeta el orden obligatorio definido en `/actions-knowledge`
- [ ] Existen los cuatro bloques de comentarios separadores donde corresponde
- [ ] Todas las referencias a acciones apuntan a acciones que existen
- [ ] Ninguna acción queda sin ser llamada desde ningún sitio

## Resultado

Si todos los checks del checklist final están bien, mostrar únicamente: **OK-No hay problemas**
