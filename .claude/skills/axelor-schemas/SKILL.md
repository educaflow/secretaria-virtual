---
name: Axelor Schemas (XSD 8.1)
description: Consulta los schemas XSD de Axelor 8.1 para verificar atributos válidos, tipos enumerados y estructura de elementos en vistas (object-views) y modelos de dominio (domain-models).
---

Este skill sirve para verificar qué atributos y valores son válidos en los ficheros XML de Axelor 8.1, tanto en vistas (`views/`) como en modelos de dominio (`domains/`).

## Cuándo usarlo

- Antes de usar un valor en `color`, `background`, `tag-style` u otros atributos con enum restringido.
- Para confirmar qué atributos acepta un elemento (`<grid>`, `<form>`, `<panel-related>`, `<menuitem>`, `<entity>`, etc.).
- Para descubrir atributos no documentados que sí existen en el XSD (como `if` en `<menuitem>`).
- Para saber si un atributo es obligatorio (`use="required"`) o prohibido (`use="prohibited"`).

## Referencias disponibles

- `references/object-views.md` — vistas: menuitem, grid, form, panel-related, action-view, hilite, enums de color/estilo
- `references/domain-models.md` — modelos: entity, campos (string, integer, many-to-one, etc.), estrategias de herencia, repository

## Ficheros XSD (fuente de verdad)

Copiados en `references/` para no depender de `axelor-open-platform`:

- `references/object-views.xsd` — vistas (4688 líneas)
- `references/domain-models.xsd` — modelos de dominio (1275 líneas)
- `references/data-import.xsd` — importación de datos (355 líneas)

Si la referencia compacta no resuelve la duda, buscar en el XSD directamente con `grep -n` o `Read`.