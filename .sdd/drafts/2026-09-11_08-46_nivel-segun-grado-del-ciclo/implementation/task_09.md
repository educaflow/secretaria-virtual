---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

**El XML ya está materializado en `design/views/Main-Nivel.xml`.** **MUST** copiarlo **literalmente** (`cp`) a su ruta destino `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Nivel.xml`. **MUST NOT** regenerarlo, reescribirlo desde el `design.md` ni reformatearlo.

**Acción `Modificar`: el destino YA EXISTE.** Antes de sobrescribirlo aplica la **comprobación de conservación** de `implementation.md` §3: todo elemento con nombre del fichero real actual (campo, panel, botón, acción) **MUST** estar presente en el XML del diseño, **salvo** los listados en la sección «Eliminaciones declaradas» del `design.md` que se reproduce más abajo. Si pasa, sobrescribe (es el comportamiento esperado, **no** es CONFLICT); si falla, reporta `CONFLICT`. **MUST NOT** fusionar los dos ficheros a mano.

El campo `grado` de este formulario hereda del modelo (`domains/Nivel.xml`, tarea 03) la marca de obligatorio; el dominio ya está colocado y es contrato fijo.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Nivel.xml` | Modificar | k-vistas (grids.md, forms.md) | Columna y campo `grado`. |

## Paso del diseño que describe este fichero (verbatim)

### Paso 6 — Vistas: `views/Main-Nivel.xml`

Fichero completo en `design/views/Main-Nivel.xml` (base real + delta).

- Preexistente (se conserva): `action-view`, `grid` con `groups="admins"`, `hilite` por `archived`, columnas `code`/`name`/`archived`, form con `buttons-panel` y sus tres `action-group`, las cinco PI `sv-*`.
- Delta en el `action-group` de `btnDelete`: antepone `remote-validationDelete-action` a `delete`, como exige `vistas.md` §1.5 para todo form **principal** (acción global de `DefaultModelController`; ver «Notas y supuestos» 2).
- Delta en el `grid`: columna `grado` **entre** `name` y `archived` — «código, nombre, grado y detrás las demás columnas que el listado ya muestre» (`screen-niveles.md`).
- Delta en el `form`: campo `grado` en el panel `Nivel`, con `grid-view`/`form-view` apuntando a las vistas `Ref@Grado`. **Sin** atributo `domain`: el selector ofrece todo el catálogo, incluidos los grados que aún no tienen ningún nivel (`U-niveles-002`). La marca de obligatorio (`U-niveles-001`) la hereda del modelo (`Nivel.grado required="true"`); **MUST NOT** duplicarse como `required="true"` en la vista.

ASCII Layout del panel `Nivel`:

```
ccc...nnnnnn   ← code(3) + colOffset(3) + name(6)
gggggg······   ← grado(6): selector con nombres medios; no hay campo hermano con el que
                 compartir fila, así que las 6 columnas libres se dejan como están
                 (k-vistas/forms.md §«Un campo solo en una fila»)
```

ASCII Layout del `buttons-panel` (preexistente, sin cambios):

```
bb......ccgg   ← btnDelete(2) + colOffset(6) + btnCancel(2) + btnSave(2)
```

**Verificación del paso:** arranca; el listado de niveles muestra la columna Grado y el formulario pide el grado.


## Reglas de UI aplicables (trazabilidad, verbatim)

### Reglas de UI `U-<slug-pantalla>-NNN`

| U | Origen spec | Ubicación | Mecanismo |
|---|---|---|---|
| `U-niveles-001` | `RUI-niveles-formulario-001` | `views/Main-Nivel.xml`, `<field name="grado">`, marcado obligatorio que hereda de `domains/Nivel.xml` (`required="true"`) | atributo declarativo del modelo |
| `U-niveles-002` | `RUI-niveles-formulario-002` | `views/Main-Nivel.xml`, `<field name="grado">` | **ausencia** de `domain` |

## Notas y supuestos aplicables (verbatim)

2. **Borrados fuera de alcance, pero `btnDelete` alineado con la convención.** La spec deja sin decidir qué debe pasar al borrar un grado que todavía tiene niveles o un nivel que todavía usan ciclos. El diseño no añade ninguna `validateRemove`, y las relaciones se declaran sin borrado en cascada (`orphanRemoval="false"` explícito en `Grado.niveles`, ver Paso 2), así que la base de datos rechazará el borrado con su error de integridad. Los `action-group` de `btnDelete` de los formularios **principales** que este diseño reescribe (`Main-Ciclo.xml`, `Main-Nivel.xml` y `Main-FamiliaProfesional.xml`) sí anteponen `remote-validationDelete-action` a `delete`, porque `vistas.md` §1.5 lo exige literalmente y el fichero que `/sdd-implementer` copia verbatim al árbol quedaría incumpliéndolo; la acción es **global** de `DefaultModelController`, así que no añade ninguna pieza nueva ni conocimiento tácito, y hoy resuelve a un `validateRemove` sin reglas. Los formularios **modales** siguen con `delete-modal` a secas (`vistas.md` §1.6: un modal **MUST NOT** llevar `remote-validation*`).
3. **La columna `archived` del listado de niveles se conserva.** `screen-niveles.md` pide «código, nombre, grado — y detrás de ellas, las demás columnas que el listado ya muestre», así que se mantienen tanto la columna `archived` como el `hilite` que la acompaña, aunque `k-vistas` desaconseje `archived` en los grids: quitarla sería una eliminación no pedida por el delta.
