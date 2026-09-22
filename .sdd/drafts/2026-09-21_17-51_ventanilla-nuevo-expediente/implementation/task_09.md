---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Añade el menú de la ventanilla al `menus.xml` **único** del proyecto.

La porción de menú **ya está materializada** por el diseñador en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/menus.xml`. **MUST NOT** reescribirse ni regenerarse:
se **fusiona verbatim** en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`, que es un fichero
**que ya existe** (`Acción: Modificar`) y cuyo contenido previo **MUST** conservarse íntegro — se insertan sus
`<menuitem>` justo antes de `</object-views>` y no se toca ninguna otra entrada. Si ya existiera un `<menuitem>`
con alguno de esos `name`, responde `CONFLICT`. Ninguna entrada preexistente del fichero puede perderse
(comprobación de conservación). Tras fusionar, **MUST** validarse el fichero con `xmllint` contra
`object-views.xsd`.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir el menú «Ventanilla» y su hijo «Nuevo expediente» |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.
>
> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

## Diseño (verbatim)

### Paso 9 — Menús

Modificación del fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`: se añade la porción de `design/menus.xml`, un menú raíz «Ventanilla» (`order="5"`, delante de «Expedientes») con un único hijo, «Nuevo expediente», que apunta a `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action`. Los dos con `groups="admins,users"`, porque la ventanilla la ve cualquier usuario con sesión iniciada.

**Verificar al final:** `grep -n "ventanilla-menuitem\|ventanilla-nuevoExpediente-menuitem" src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` devuelve las dos líneas fusionadas (el raíz «Ventanilla» y su hijo «Nuevo expediente» con `action="sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action"`), y ninguna otra entrada del fichero se ha perdido (`grep -c "<menuitem" …/menus.xml` crece exactamente en 2 respecto al valor previo).

---

### Cambios necesarios fuera del sistema (punto 1)

Los dos primeros son ficheros de datos del proyecto; el tercero es la única ampliación normativa pendiente. De las reglas de vistas no queda ninguna: tanto el `<grid action="…">` de los pasos 1 y 2 (D4) como la rama del asistente de `VAR-7.2` (D8) ya están recogidos en `agent_docs/view-rules.md` y en los skills `k-vistas`. **MUST NOT** aplicar ninguno de los tres el diseñador: los aplica `/sdd-implementer`.

1. **`src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`** — fusionar la porción de `design/menus.xml`.
