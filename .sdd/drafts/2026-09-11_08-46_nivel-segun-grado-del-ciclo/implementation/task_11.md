---
type: implementation-task
template: system
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

**El XML ya está materializado en `design/views/Ref-Ciclo.xml`.** **MUST** copiarlo **literalmente** (`cp`) a su ruta destino `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Ref-Ciclo.xml`. **MUST NOT** regenerarlo, reescribirlo desde el `design.md` ni reformatearlo.

**Acción `Modificar`: el destino YA EXISTE.** Antes de sobrescribirlo aplica la **comprobación de conservación** de `implementation.md` §3: todo elemento con nombre del fichero real actual (campo, panel, botón, acción) **MUST** estar presente en el XML del diseño, **salvo** los listados en la sección «Eliminaciones declaradas» del `design.md` que se reproduce más abajo. Si pasa, sobrescribe (es el comportamiento esperado, **no** es CONFLICT); si falla, reporta `CONFLICT`. **MUST NOT** fusionar los dos ficheros a mano.

Es la pantalla «Consulta de un ciclo»: el nivel se muda al panel condicional conservando su `readonly="true"`. Es un formulario de consulta con un único botón **Salir**; **MUST NOT** añadirle nada más.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Ref-Ciclo.xml` | Modificar | k-vistas (forms.md) | El nivel de la ficha de consulta pasa a panel condicional. |

## Paso del diseño que describe este fichero (verbatim)

### Paso 8 — Vistas: `views/Ref-Ciclo.xml`

Fichero completo en `design/views/Ref-Ciclo.xml` (base real + delta). Es la pantalla «Consulta de un ciclo», que se abre desde el formulario de curso al pulsar sobre el ciclo elegido.

- Preexistente (se conserva): el `Ref@Ciclo-grid`, el bloque de solo lectura con sus campos `readonly="true"`, el `buttons-panel` con el único botón **Salir** y su `action-group` con `close`, y las cinco PI.
- Delta: el campo `nivel` se muda al mismo panel condicional `nivelPanel` (`showIf="grado.admiteNivel"`), con `depends="grado.admiteNivel"` y conservando su `readonly="true"` (`U-consulta-ciclo-001`). Es exactamente el mismo idioma que en el formulario de mantenimiento: **el nivel vive siempre en su panel condicional, alineado bajo el grado**.

ASCII Layout del panel `Ciclo` (dos estados):

```
── estado A: el grado del ciclo consultado admite nivel ─────────
nnnnnnffffff   ← name(6) + familiaProfesional(6)
gggggg······   ← grado(6); las 6 libres son el hueco donde se alinea el nivel
   panel nivelPanel (colSpan 12, showIf="grado.admiteNivel"):
......llllll   ← colOffset(6) + nivel(6)   [borde 6|7, alineado con familiaProfesional y grado]

── estado B: el grado del ciclo no admite nivel ─────────────────
nnnnnnffffff   ← name(6) + familiaProfesional(6)
gggggg······   ← grado(6)
               (el panel nivelPanel colapsa entero)
```

ASCII Layout del `buttons-panel` (preexistente, sin cambios):

```
..........ss   ← colOffset(10) + btnCancel «Salir»(2)   [principal pegado al borde: 10+2=12]
```

**Verificación del paso:** arranca; desde un curso, al pulsar sobre un ciclo de «Curso de especialización» la ficha no muestra el campo Nivel, y sobre uno de «Ciclo formativo» sí.


## Reglas de UI aplicables (trazabilidad, verbatim)

### Reglas de UI `U-<slug-pantalla>-NNN`

| U | Origen spec | Ubicación | Mecanismo |
|---|---|---|---|
| `U-consulta-ciclo-001` | `RUI-consulta-ciclo-formulario-001` | `views/Ref-Ciclo.xml`, `<panel name="nivelPanel">` del form `subsysSistemaEducativo.Ref@Ciclo-form` | `showIf="grado.admiteNivel"` en el panel |
`U-ciclos-001`, `U-ciclos-002`, `U-consulta-ciclo-001` y `U-familias-profesionales-001`/`002` dependen de `Grado.admiteNivel`; el campo llega al cliente porque `<field name="nivel">` declara `depends="grado.admiteNivel"` en los tres formularios (en el modal de `Main-FamiliaProfesional.xml`, `depends="grado.admiteNivel,nivel.grado"`: el segundo campo relacionado es el que hace evaluable en cliente la tercera comprobación de su `Local-validate*`).
