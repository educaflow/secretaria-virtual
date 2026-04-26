---
name: grids-reviewer
description: Revisa que los grids XML `<grid>` de Axelor creados o modificados cumplen todas las reglas de /grids-knowledge — atributos obligatorios, nomenclatura y campos.
---

# grids-reviewer

## Propósito

Verificar que los grids (`<grid>`) creados o modificados siguen las reglas definidas en `/grids-knowledge`.

## Qué leer

1. El fichero XML de vistas que contiene el grid a revisar.
2. El fichero XML de dominio de la entidad para verificar que los campos existen.
3. El skill `/grids-knowledge` para tener presentes todas las reglas.

## Nomenclatura del grid

Patrón: `{Prefijo}.{Entidad}[.{EntidadHija}]*@[Main|Search|{Nombre}]-grid`

- [ ] El prefijo es `subsys{Subsistema}` para subsistemas o `sys{Sistema}` para sistemas (PascalCase sin separador).
- [ ] El nombre de la entidad coincide exactamente con el nombre de la clase Java.
- [ ] El identificador `@Main`, `@Search` u otro nombre refleja el contexto correcto.
- [ ] El grid termina con el sufijo `-grid`.
- [ ] Las entidades anidadas se separan con `.` (punto): `subsys{X}.{Padre}.{Hijo}@Main-grid`.

## Atributos obligatorios de `<grid>`

La plantilla exige todos estos atributos. Los valores por defecto indicados pueden cambiarse solo si el negocio lo justifica:

- [ ] `allowSearchFields` está presente (habitualmente `"true"`).
- [ ] `orderBy` está presente y tiene un valor lógico para la entidad.
- [ ] `canEditOnClick` O `canViewOnClick` está presente (no ambos):
  - Si se puede editar: `canEditOnClick="true"` (sin `canViewOnClick`).
  - Si es de solo lectura: `canViewOnClick="true"` (sin `canEditOnClick`).
- [ ] `canAdvanceSearch="false"`.
- [ ] `canRefresh="false"`.
- [ ] `editable="false"`.
- [ ] `edit-icon="false"`.
- [ ] `x-selector="none"`.
- [ ] `canEdit="false"`.
- [ ] `canDelete="false"`.
- [ ] `canSave="false"`.
- [ ] `title=""`.
- [ ] Si se pueden crear nuevas entidades: `canNew="true"` y `newButtonTitle="..."`.
- [ ] Si no se pueden crear: `canNew="false"` y NO hay atributo `newButtonTitle`.

## Campos (`<field>`) dentro del grid

- [ ] Cada campo tiene `name` que coincide con un atributo existente en el modelo de dominio.
- [ ] Los campos que lo necesitan tienen `width` con unidad CSS (`px`, `em`, `%`).
- [ ] Hay al menos un campo sin `width` (habitualmente el campo de texto largo, p.ej. `name`) para que se ajuste al espacio disponible.
- [ ] El orden de los campos es lógico: identificador/código → nombre → otros campos relevantes.
- [ ] El `title` de los campos es opcional; si se usa, debe ser más descriptivo que el título del atributo del modelo.

## Checklist final

- [ ] El nombre del grid sigue el patrón `{Prefijo}.{Entidad}@{Nombre}-grid`
- [ ] Todos los atributos obligatorios de la plantilla están presentes
- [ ] `canNew` y `newButtonTitle` son coherentes entre sí
- [ ] Exactamente uno de `canEditOnClick` o `canViewOnClick` está presente
- [ ] Todos los campos `<field>` existen en el modelo de dominio
- [ ] El orden de los campos es lógico para el usuario (código/nombre primero)

## Resultado

Si todos los checks del checklist final están bien, mostrar únicamente: **OK-No hay problemas**
