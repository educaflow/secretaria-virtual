---
name: Acciones (actions) de Axelor
description: Dado un fichero de vistas de Axelor en XML, permite añadir acciones (actions) a las vistas, como botones, menús, etc., y menus y submenus (menuitem) a partir de una descripción en lenguaje natural
---

Este skill sirve para diseñar y generar acciones de Axelor en XML y conectarlas con vistas, botones y navegación.

## Por qué son importantes las acciones

Las vistas (`form`, `grid`, `cards`, etc.) por sí solas solo muestran datos. Las acciones son lo que les da comportamiento real: validan, cambian atributos, ejecutan lógica, abren otras vistas, importan/exportan datos y controlan la navegación.

## Qué es un menú y qué es un menuitem

En Axelor, la navegación de la aplicación se define con `menuitem`:

- Un `menuitem` sin `parent` actúa como menú raíz.
- Un `menuitem` con `parent` actúa como submenú (hijo).
- El `menuitem` suele enlazar con una acción (normalmente `action-view`) mediante el atributo `action`.

Ejemplo:

```xml
<menuitem name="menu-academico" title="Académico" />

<menuitem name="menu-expedientes"
  parent="menu-academico"
  title="Expedientes"
  action="expediente.all" />
```

## Tipos de acciones en Axelor

Las acciones más importantes y habituales son:

- `action-view`: abre una o varias vistas (`grid`, `form`, etc.) para un modelo.
- `action-method`: llama a un método Java de controlador (`@CallMethod`).
- `action-attrs`: cambia atributos de campos en tiempo real (`readonly`, `hidden`, `required`, `domain`, `value`, etc.).
- `action-record`: construye o completa un registro con valores por defecto o calculados.
- `action-group`: agrupa varias acciones y las ejecuta en secuencia.
- `action-validate`: lanza validaciones y mensajes (`error`, `alert`, `info`, `notify`).
- `action-condition`: valida condiciones sobre campos y muestra errores bajo el campo.
- `action-script`: ejecuta lógica compleja mediante script (`js` o `groovy`).
- `action-export`: exporta datos usando plantillas.
- `action-import`: importa datos desde una configuración de importación.
- `action-ws`: consume servicios web SOAP (habitualmente como proveedor para importaciones).

## Eventos habituales donde se usan

Estas acciones se suelen disparar desde eventos de vista:

- `onNew`, `onLoad`, `onSave`
- `onChange`, `onSelect`, `onClick`

## Reglas importantes del action-view

- **El orden de los `<view>` dentro de `action-view` determina la vista inicial.** Axelor abre la primera vista listada al entrar al menú. Siempre poner `grid` antes que `form`:

```xml
<action-view name="..." title="..." model="...">
    <view type="grid" name="..."/>   <!-- primero el grid -->
    <view type="form" name="..."/>   <!-- después el form -->
</action-view>
```

## Referencia

Para detalle completo de atributos, sintaxis y ejemplos avanzados, consultar:

- `references/actions.md`

