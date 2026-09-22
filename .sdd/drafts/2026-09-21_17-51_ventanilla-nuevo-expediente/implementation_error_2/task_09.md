---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Fusiona el **menú de la ventanilla** en el `menus.xml` único del proyecto.

La porción a fusionar **ya está materializada** por el diseñador en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/menus.xml`: son los dos `<menuitem>`
(el raíz «Ventanilla» y su hijo «Nuevo expediente»), que **MUST** copiarse **literalmente**, sin
reescribirlos ni renombrarlos.

La acción es `Modificar`: el destino `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`
**ya existe**. **MUST NOT** sobrescribirse el fichero: se **fusionan** los dos `<menuitem>` dentro del
`<object-views>` existente, conservando **todo** su contenido previo (comprobación de conservación:
`grep -c "<menuitem"` sobre el fichero debe crecer **exactamente en 2** respecto al valor previo, y ninguna
entrada anterior puede perderse). Tras la fusión, el XML debe seguir siendo válido.


## Estado del árbol al empezar (decisión del descomponedor, no del diseño)

Una ejecución anterior de `/sdd-implementer` se detuvo por un error del diseño (ver
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/implementation_error_1/error_design.log`) y dejó
**ya materializados** en el árbol los ficheros de esta tarea, escritos contra una versión anterior del
diseño. La **fuente de verdad es el diseño de hoy**: si el fichero ya existe, **MUST** comprobarse que
coincide exactamente con lo que este texto prescribe y, si no coincide, dejarlo como el diseño manda
(para los XML materializados, copia literal desde `design/`). Que el destino ya exista **NO** es un
`CONFLICT`: la acción `Crear` de la tabla se refiere al estado previo a la iniciativa.

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir el menú «Ventanilla» y su hijo «Nuevo expediente» |

## Nota del diseño para `/sdd-implementer`

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Paso 9 del diseño — Menús (verbatim)

### Paso 9 — Menús

Modificación del fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`: se añade la porción de `design/menus.xml`, un menú raíz «Ventanilla» (`order="5"`, delante de «Expedientes») con un único hijo, «Nuevo expediente», que apunta a `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action`. Los dos con `groups="admins,users"`, porque la ventanilla la ve cualquier usuario con sesión iniciada.

**Verificar al final:** `grep -n "ventanilla-menuitem\|ventanilla-nuevoExpediente-menuitem" src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` devuelve las dos líneas fusionadas (el raíz «Ventanilla» y su hijo «Nuevo expediente» con `action="sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action"`), y ninguna otra entrada del fichero se ha perdido (`grep -c "<menuitem" …/menus.xml` crece exactamente en 2 respecto al valor previo).


## «Cambios necesarios fuera del sistema», punto 1 (verbatim)

1. **`src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`** — fusionar la porción de `design/menus.xml`.
