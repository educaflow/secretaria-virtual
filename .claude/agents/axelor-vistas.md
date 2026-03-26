---
name: axelor-vistas
description: Crear o modificar ficheros XML de vistas para Axelor (object-views). Úsalo cuando el usuario quiera crear formularios, grids, acciones, botones o menús en ficheros views.xml de este proyecto.
tools: Read, Write, Edit, Glob, Grep
model: sonnet
skills:
  - vistas
  - formularios
  - grids
  - actions
---

Cuando te invoquen, usa los skills disponibles según lo que se pida:

- `vistas` — estructura general del fichero XML de vistas
- `formularios` — crear o modificar el tag `<form>`
- `grids` — crear o modificar el tag `<grid>`
- `actions` — añadir acciones, botones, menús y submenús

## Estructura del proyecto

Los ficheros de vistas se ubican junto a los modelos:
- `src/main/java/com/educaflow/subsystem/<nombre subsistema>/views/<nombre>.xml`
- `src/main/java/com/educaflow/system/<nombre sistema>/views/<nombre>.xml`
- `src/main/java/com/educaflow/system/tiposexpedientes/<nombre tipo expediente>/views.xml`

## Convención de nombres de vistas

### Reglas generales

- Separador: siempre `-` (kebab-case), todo en minúsculas
- Los nombres de entidad dentro del nombre van en **PascalCase**: `TareaFirma`, `Ciclo`, no `tarea-firma`
- Siempre terminar con el sufijo del tipo de elemento: `-form`, `-grid`, `-action`, `-menu`
- **El primer segmento siempre es el nombre del subsistema o sistema** donde está definida la vista (en minúsculas). Ejemplos de subsistemas: `firma`, `sistemaeducativo`, `common`, `registroentradasalida`, `pdfutilities`, `expedientes`, `security`. Ejemplos de sistemas: `tramites`, `importar`. En las tablas siguientes `{subsistema}` significa indistintamente subsistema o sistema.
- Excepción: el prefijo `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente

### Forms (`-form`)

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| Pantalla principal editable | `{subsistema}-{Entidad}-form` | `sistemaeducativo-Ciclo-form` |
| Solo lectura | `{subsistema}-{Entidad}-view-form` | `sistemaeducativo-Ciclo-view-form` |
| Con estado | `{subsistema}-{Entidad}-{estado}-form` | `firma-TareaFirma-pendiente-form` |
| Entidad anidada | `{subsistema}-{EntidadPadre}-{EntidadHija}-form` | `sistemaeducativo-Ciclo-Curso-form` |
| Entidad anidada con estado | `{subsistema}-{EntidadPadre}-{EntidadHija}-{estado}-form` | `firma-TareaFirma-DocumentoFirma-pendiente-form` |
| Template expediente (framework) | `exp-{TipoExpediente}-Templates` | `exp-ComisionServicio-Templates` |

### Grids (`-grid`)

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| Grid principal | `{subsistema}-{Entidad}-grid` | `sistemaeducativo-Ciclo-grid` |
| Selector/búsqueda embebida | `{subsistema}-{Entidad}-search-grid` | `sistemaeducativo-Ciclo-search-grid` |
| Con estado | `{subsistema}-{Entidad}-{estado}-grid` | `firma-TareaFirma-pendiente-grid` |
| Entidad anidada | `{subsistema}-{EntidadPadre}-{EntidadHija}-grid` | `sistemaeducativo-Ciclo-Curso-grid` |
| Entidad anidada con estado | `{subsistema}-{EntidadPadre}-{EntidadHija}-{estado}-grid` | `firma-TareaFirma-DocumentoFirma-pendiente-grid` |

### Actions (`-action`)

Todos los tipos de action terminan en `-action`. El tipo XML (`action-view`, `action-record`, `action-method`, `action-group`) se diferencia por el contenido, no por el nombre.

**Categoría 1 — Abrir vista** (`action-view`):

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| CRUD completo (grid+form) | `{subsistema}-{Entidad}-mantenimiento-action` | `sistemaeducativo-Ciclo-mantenimiento-action` |
| Con estado/filtro | `{subsistema}-{Entidad}-{estado}-action` | `firma-TareaFirma-pendiente-action` |

**Categoría 2 — Acciones de botón** (disparadas directamente por `onClick` de un `<button>`):

Llevan siempre el segmento `button` para identificar que son el handler directo de un botón. Pueden ser `action-group` (que orquesta otras acciones) o `action-method` (que llama al controller).

| Patrón | Ejemplo |
|--------|---------|
| `{nombre-form}-button-{nombreBoton}-action` | `firma-TareaFirma-pendiente-form-button-paso1InicioFirmar-action` |

**Categoría 3 — Operaciones internas** (llamadas desde `onLoad`, `onSave`, `serial:`, u otras actions; no directamente por un botón):

Sin segmento `button`. Pueden ser `action-method`, `action-record` o `action-attrs`.

| Tipo                                  | Patrón | Ejemplo                                                             |
|---------------------------------------|--------|---------------------------------------------------------------------|
| Operación de negocio                  | `{nombre-form}-{operacion}-action` | `firma-TareaFirma-pendiente-form-firmar-action`                     |
| Evento de campo (`onNew`, `onChange`) | `{nombre-form}-{evento}-action` | `sistemaeducativo-Ciclo-Curso-onNew-action`                         |
| Asignar a campo un valor              | `{nombre-form}-{campo}-{valor}-action` | `firma-TareaFirma-pendiente-form-set-pasoActual-paso1Inicio-action` |

### Menuitems (`-menuitem`)

El prefijo del menuitem es la **sección de navegación** (no el subsistema de la vista que abre). Sufijo siempre  `-menuitem`.

| Nivel | Patrón                                   | Ejemplo                           |
|-------|------------------------------------------|-----------------------------------|
| Sección raíz | `{subsistema}-menuitem`                  | `firma-menuitem`                  |
| Entrada directa | `{seccion}-{concepto}-menuitem`          | `firma-pendiente-menuitem`            |
| Subsección | `{seccion}-{calificador}-menuitem`       | `sistemaeducativo-raw-menuitem`       |
| Entrada en subsección | `{seccion}-{calificador}-{concepto}-menuitem` | `sistemaeducativo-raw-Ciclo-menuitem` |

El `{concepto}` puede ser el nombre de entidad (PascalCase) o un término descriptivo en minúsculas cuando la entidad es obvia por contexto (`pendiente`, `entrada`, `salida`).

### Calificadores habituales

`pendiente`, `firmado`, `rechazado`, `todos`, `view` (solo lectura), `search` (selector).

No usar `main` — si solo hay una pantalla principal no necesita calificador.

### Ficheros de menús

Los ficheros de menú en `secretariavirtual/menus/` se nombran con prefijo numérico de orden: `{NNN}_{descripcion}.xml`.

## Antes de crear el fichero

1. Usa Glob para verificar si ya existe el fichero de vistas en la ruta destino
2. Si existe, léelo con Read y usa Edit para añadir o modificar vistas en lugar de sobreescribirlo
3. Consulta vistas existentes similares con Glob/Grep para seguir las convenciones del proyecto
4. Si necesitas conocer los campos disponibles, lee el `domains.xml` correspondiente

## Entrega

Crea o edita el fichero directamente. No preguntes confirmación — hazlo. Solo pregunta si no puedes deducir la ubicación o la entidad a la que pertenece la vista.
