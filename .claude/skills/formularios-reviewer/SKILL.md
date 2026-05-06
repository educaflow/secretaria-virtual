---
name: formularios-reviewer
description: Revisa que los formularios XML `<form>` de Axelor creados o modificados cumplen todas las reglas de /formularios-knowledge — plantilla, atributos obligatorios, layout, campos, panel-related, botones y nomenclatura.
---

# formularios-reviewer

## Propósito

Verificar que los formularios (`<form>`) creados o modificados siguen las reglas definidas en `/formularios-knowledge`.

## Qué leer

1. El fichero XML de vistas que contiene el formulario a revisar.
2. El fichero XML de dominio de la entidad para verificar que los campos existen.
3. El skill `/formularios-knowledge` para tener presentes todas las reglas.

## Nomenclatura del formulario

Patrón: `{Prefijo}.{Entidad}[.{EntidadHija}]*@[Main|View|{Nombre}]-form`

- [ ] El prefijo es `subsys{Subsistema}` para subsistemas o `sys{Sistema}` para sistemas (PascalCase sin separador).
- [ ] El nombre de la entidad coincide exactamente con el nombre de la clase Java.
- [ ] El identificador `@Main`, `@View` u otro nombre refleja el contexto correcto.
- [ ] El formulario termina con el sufijo `-form`.

## Atributos obligatorios de `<form>`

La plantilla exige todos estos atributos con los valores indicados, salvo que el negocio justifique otro valor:

- [ ] `width="large"`
- [ ] `canAttach="false"`
- [ ] `canBack="false"`
- [ ] `canDelete="false"`
- [ ] `canNew="false"`
- [ ] `canSave="false"`
- [ ] `canMore="false"`
- [ ] `title` está definido y es descriptivo para el usuario.

## Campos (`<field>`)

- [ ] Cada campo tiene `name` que coincide con un atributo existente en el modelo de dominio.
- [ ] El `widget` elegido es apropiado para el tipo del campo:
  - `MetaFile` → `widget="binary-link"` o `widget="binary"`
  - Campos largos/multiline → `widget="Text"`
  - Enum horizontal/vertical → `widget="SwitchSelect"`
  - Relacionales con búsqueda guiada → `widget="suggest"` con `domain`
- [ ] Los campos con `many-to-one` tienen `grid-view` (selector `@Search-grid`) si se necesita selector específico.
- [ ] Los campos con `showIf`/`hideIf`/`requiredIf`/`readonlyIf` usan sintaxis correcta de expresiones Axelor.

## Layout (colSpan / colOffset)

- [ ] Ninguna fila supera 12 columnas en total (`colSpan` + `colOffset` de todos los campos en la misma fila ≤ 12).
- [ ] No hay filas con espacio vacío innecesario (los `colSpan` se ajustan al contenido real del campo).
- [ ] Los campos de la misma fila están agrupados temáticamente.
- [ ] Los campos en filas consecutivas tienen alineación coherente: cuando es posible, las filas siguientes (incluidos los paneles anidados condicionales) usan el mismo split de columnas que la primera fila. Si el split difiere, verificar que hay una razón semántica o de proporcionalidad que lo justifique; si no la hay, corregirlo.
- [ ] Los campos con texto largo (`name`, `descripcion`, `asunto`) tienen `colSpan` mayor que los campos cortos (`fecha`, `código`).
- [ ] **Campos condicionales sin huecos**: si en la misma fila hay campos con `showIf` mutuamente excluyentes, se usan paneles anidados con `showIf` en el panel (no en los campos individuales). Un campo oculto dentro de un panel sigue ocupando espacio en el grid; un panel oculto no deja hueco.
- [ ] **Sin campos solos en filas anchas**: si un campo queda solo en una fila con más de 6 columnas vacías a la derecha, revisar si debe agruparse con otro campo relacionado o si hay un hueco causado por un campo oculto.
- [ ] **Proporcionalidad**: rutas/paths usan `colSpan` ≥ 9; widgets compactos (binary-link, slot) usan ≤ 4; PINs/contraseñas usan 4–6.

### Ejemplo de error de hueco a detectar:
```xml
<!-- ERROR: cuando 'fichero' está oculto, 'password' aparece desplazado a la derecha -->
<field name="fichero"  colSpan="6" showIf="tipo == 'FICHERO'"/>
<field name="password" colSpan="6" showIf="tipo != 'PKCS11'"/>
```
Solución: agrupar en paneles anidados según el tipo, poniendo `showIf` en el panel.

## Paneles (`<panel>`)

- [ ] Cada panel tiene `name` en camelCase.
- [ ] El `title` del panel es descriptivo.
- [ ] El `colSpan` del panel es correcto (normalmente `12`).

## Panel de botones

- [ ] Existe un panel de botones al final del formulario con `showFrame="false"`.
- [ ] Incluye al menos Borrar, Cancelar y Guardar, salvo que el negocio justifique no incluirlos.
- [ ] Los botones de acción secundaria (Borrar) están a la izquierda.
- [ ] Los botones principales (Cancelar, Guardar) están a la derecha.
- [ ] Los botones de destrucción llevan `css="btn-danger"`.
- [ ] Los botones secundarios llevan `outline="true"`.

## `<panel-related>`

- [ ] Cada `<panel-related>` tiene `grid-view` y `form-view` especificados.
- [ ] Incluye los atributos obligatorios de la plantilla: `colSpan`, `showFooter="false"`, `canEdit="false"`, `canRemove="false"`, `forceEdit="true"`.
- [ ] El `grid-view` y `form-view` referenciados existen en algún fichero XML del proyecto.

## Checklist final

- [ ] El nombre del formulario sigue el patrón `{Prefijo}.{Entidad}@{Nombre}-form`
- [ ] Todos los atributos obligatorios de `<form>` están presentes
- [ ] Todos los campos `<field>` existen en el modelo de dominio
- [ ] Ninguna fila supera 12 columnas; el espacio es proporcional al contenido
- [ ] Los campos de filas consecutivas tienen alineación coherente
- [ ] El panel de botones existe, con la disposición correcta de botones primarios y secundarios
- [ ] Los `<panel-related>` llevan todos los atributos obligatorios y sus referencias existen
- [ ] Las referencias a grids, forms y actions desde los campos y botones apuntan a elementos que existen

## Resultado

Si todos los checks del checklist final están bien, mostrar únicamente: **OK-No hay problemas**
