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

- El nombre empieza siempre con el prefijo `subsys` (para subsistemas) o `sys` (para sistemas), seguido directamente del nombre del subsistema/sistema en **PascalCase** y sin separador.
- A continuación, cada entidad (tabla) se separa con `.` (punto): `subsys{Subsistema}.{Entidad}` o `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}`.
- Después de la última entidad, el resto del nombre (estados, calificadores, tipo) usa `-` (guión): `subsys{Subsistema}.{Entidad}-{estado}-{tipo}`.
- Subsistemas conocidos (PascalCase): `Firma`, `SistemaEducativo`, `Common`, `RegistroEntradaSalida`, `PdfUtilities`, `Expedientes`, `Security`, `Importer`. Sistemas conocidos: `Tramites`, `Importar`.
- Siempre terminar con el sufijo del tipo de elemento: `-form`, `-grid`, `-action`, `-menuitem`
- Excepción: el prefijo `exp-` se reserva exclusivamente para las vistas del framework de tipos de expediente (templates)

### Forms (`-form`)

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| Pantalla principal editable | `subsys{Subsistema}.{Entidad}-form` | `subsysSistemaEducativo.Ciclo-form` |
| Solo lectura | `subsys{Subsistema}.{Entidad}-view-form` | `subsysSistemaEducativo.Ciclo-view-form` |
| Con estado | `subsys{Subsistema}.{Entidad}-{estado}-form` | `subsysFirma.TareaFirma-pendiente-form` |
| Entidad anidada | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-form` | `subsysSistemaEducativo.Ciclo.Curso-form` |
| Entidad anidada con estado | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-{estado}-form` | `subsysFirma.TareaFirma.DocumentoFirma-pendiente-form` |
| Sistema | `sys{Sistema}.{Entidad}-form` | `sysImportar.Importar-form` |
| Template expediente (framework) | `exp-{TipoExpediente}-Templates` | `exp-ComisionServicio-Templates` |

### Grids (`-grid`)

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| Grid principal | `subsys{Subsistema}.{Entidad}-grid` | `subsysSistemaEducativo.Ciclo-grid` |
| Selector/búsqueda embebida | `subsys{Subsistema}.{Entidad}-search-grid` | `subsysSistemaEducativo.Ciclo-search-grid` |
| Con estado | `subsys{Subsistema}.{Entidad}-{estado}-grid` | `subsysFirma.TareaFirma-pendiente-grid` |
| Entidad anidada | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-grid` | `subsysSistemaEducativo.Ciclo.Curso-grid` |
| Entidad anidada con estado | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-{estado}-grid` | `subsysFirma.TareaFirma.DocumentoFirma-pendiente-grid` |

### Actions (`-action`)

Todos los tipos de action terminan en `-action`. El tipo XML (`action-view`, `action-record`, `action-method`, `action-group`) se diferencia por el contenido, no por el nombre.

**Categoría 1 — Abrir vista** (`action-view`):

| Caso | Patrón | Ejemplo |
|------|--------|---------|
| CRUD completo (grid+form) | `subsys{Subsistema}.{Entidad}-mantenimiento-action` | `subsysSistemaEducativo.Ciclo-mantenimiento-action` |
| Con estado/filtro | `subsys{Subsistema}.{Entidad}-{estado}-action` | `subsysFirma.TareaFirma-pendiente-action` |

**Categoría 2 — Acciones de botón** (disparadas directamente por `onClick` de un `<button>`):

Llevan siempre el segmento `button` para identificar que son el handler directo de un botón. Pueden ser `action-group` (que orquesta otras acciones) o `action-method` (que llama al controller).

| Patrón | Ejemplo |
|--------|---------|
| `subsys{Subsistema}.{Entidad}-{estado}-form-button-{nombreBoton}-action` | `subsysFirma.TareaFirma-pendiente-form-button-paso1InicioFirmar-action` |

**Categoría 3 — Operaciones internas** (llamadas desde `onLoad`, `onSave`, `serial:`, u otras actions; no directamente por un botón):

Sin segmento `button`. Pueden ser `action-method`, `action-record` o `action-attrs`.

| Tipo | Patrón | Ejemplo |
|------|--------|---------|
| Operación de negocio | `subsys{Subsistema}.{Entidad}-{estado}-form-{operacion}-action` | `subsysFirma.TareaFirma-pendiente-form-firmar-action` |
| Evento de campo (`onNew`, `onChange`) | `subsys{Subsistema}.{EntidadPadre}.{EntidadHija}-{evento}-action` | `subsysSistemaEducativo.Ciclo.Curso-onNew-action` |
| Asignar a campo un valor | `subsys{Subsistema}.{Entidad}-{estado}-form-set-{campo}-{valor}-action` | `subsysFirma.TareaFirma-pendiente-form-set-pasoActual-paso1Inicio-action` |

### Menuitems (`-menuitem`)

El prefijo del menuitem es la **sección de navegación** (no el subsistema de la vista que abre). Si la sección corresponde a un subsistema o sistema, usa el mismo prefijo `subsys`/`sys` con PascalCase. Sufijo siempre `-menuitem`.

Regla para entidades en menuitems: si el nombre de entidad (PascalCase) aparece **directamente** tras el prefijo `subsys{Seccion}`, se fusiona sin separador. Si aparece **después de un calificador en minúsculas**, mantiene el `-`.

| Nivel | Patrón | Ejemplo |
|-------|--------|---------|
| Sección raíz | `subsys{Seccion}-menuitem` | `subsysFirma-menuitem` |
| Entrada directa (concepto en minúsculas) | `subsys{Seccion}-{concepto}-menuitem` | `subsysFirma-pendiente-menuitem` |
| Entrada directa (entidad PascalCase) | `subsys{Seccion}.{Entidad}-menuitem` | `subsysSistemaEducativo.Ciclo-menuitem` |
| Subsección | `subsys{Seccion}-{calificador}-menuitem` | `subsysSistemaEducativo-raw-menuitem` |
| Entrada en subsección (entidad tras calificador) | `subsys{Seccion}-{calificador}-{Entidad}-menuitem` | `subsysSistemaEducativo-raw-Ciclo-menuitem` |

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
