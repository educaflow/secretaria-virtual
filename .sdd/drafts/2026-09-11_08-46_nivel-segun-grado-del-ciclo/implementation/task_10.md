---
type: implementation-task
template: system
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

**El XML ya está materializado en `design/views/Main-Ciclo.xml`.** **MUST** copiarlo **literalmente** (`cp`) a su ruta destino `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Ciclo.xml`. **MUST NOT** regenerarlo, reescribirlo desde el `design.md` ni reformatearlo.

**Acción `Modificar`: el destino YA EXISTE.** Antes de sobrescribirlo aplica la **comprobación de conservación** de `implementation.md` §3: todo elemento con nombre del fichero real actual (campo, panel, botón, acción) **MUST** estar presente en el XML del diseño, **salvo** los listados en la sección «Eliminaciones declaradas» del `design.md` que se reproduce más abajo. Si pasa, sobrescribe (es el comportamiento esperado, **no** es CONFLICT); si falla, reporta `CONFLICT`. **MUST NOT** fusionar los dos ficheros a mano.

De los tres bloques del fichero (`Ciclo`, `Ciclo.Curso` y `Ciclo.Curso.CursoModulo`) **solo el primero cambia**; los dos formularios modales quedan fuera del delta.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Ciclo.xml` | Modificar | k-vistas (grids.md, forms.md, actions.md) | Grid con grado y nivel; formulario con el nivel en panel condicional, filtro por grado y vaciado al cambiar de grado. |

## Paso del diseño que describe este fichero (verbatim)

### Paso 7 — Vistas: `views/Main-Ciclo.xml`

Fichero completo en `design/views/Main-Ciclo.xml` (base real + delta). El fichero contiene tres bloques: `Ciclo`, `Ciclo.Curso` y `Ciclo.Curso.CursoModulo`; **solo el primero cambia**.

- Preexistente (se conserva): los tres bloques con sus cinco PI, el `action-view`, los `action-group` de botones de los dos modales (`delete-modal`/`close`/`save-modal`), el `panel-related` de cursos, los dos formularios modales y sus `action-record` de referencia al padre.
- Delta en el `action-group` de `btnDelete` del form **principal**: antepone `remote-validationDelete-action` a `delete` (`vistas.md` §1.5). Los `btnDelete` de los dos modales siguen con `delete-modal` a secas.
- Delta en el `grid` del ciclo: columnas `grado` y `nivel` tras `familiaProfesional` → orden final código, nombre, familia profesional, grado, nivel (`ESC-009`).
- Delta en el `form` del ciclo:
  - `familiaProfesional` pasa a `colSpan="6"` **sin** `colOffset` y `grado` pierde su `colOffset="6"` **conservando su `colSpan="3"`**, de modo que comparten fila (columnas 1-6 y 7-9; antes cada uno abría fila con seis columnas vacías a la izquierda y el `nivel` colgaba a la derecha del grado). Es la recolocación **mínima** para que el nivel condicional quede alineado bajo el grado: **ningún campo preexistente se ensancha** y ningún otro campo se mueve. `grado` y `nivel` mantienen el mismo ancho (`colSpan="3"`) que tienen en el modal del Paso 9, de modo que los dos formularios que editan `Ciclo` hablan el mismo idioma de maqueta (`vistas.md` §1.8, mínima intrusión).
  - Se **elimina** el `domain="(self.code='D' OR self.code='E')"` del campo `grado`: el selector ofrece todos los grados del catálogo (`U-ciclos-005`, `ESC-008` paso 9).
  - `grado` gana `onChange="subsysSistemaEducativo.Main@Ciclo-onChange-grado-action"` (`U-ciclos-003`).
  - El campo `nivel` se muda a un panel anidado propio `nivelPanel` (`colSpan="12"`, `showFrame="false"`, `showIf="grado.admiteNivel"`), que es la ubicación de `U-ciclos-001`. Dentro, el campo lleva:
    - `required="true"` a secas (`U-ciclos-002`): la condición vive **una sola vez**, en el `showIf` del panel que lo envuelve. `useGetErrors` de `axelor-front` descarta los widgets cuyo padre está oculto, así que el obligatorio no bloquea el guardado de un ciclo sin nivel.
    - `depends="grado.admiteNivel"`: es lo que hace que el cliente pida al servidor el indicador del grado, tanto al abrir un ciclo guardado como justo después de elegir otro grado (ver `decisiones.md` D2).
    - `domain="self.grado = :grado"` (`U-ciclos-004`), que **sustituye** al filtro `(self.code='D' OR self.code='E')` heredado por error del campo `grado`. Parámetro nombrado enlazado al campo del formulario, nunca literal interpolado (`k-secure-coding` §5).
    - `colOffset="6"` + `colSpan="3"`: conserva su ancho preexistente y se alinea justo bajo el `grado` (borde 6|7), exactamente como en el modal del Paso 9.
    - `grid-view`/`form-view` de `Ref@Nivel` (se conservan).
- Delta en las acciones del bloque `Ciclo` (sección `<?sv-rules?>` y `<?sv-primary-actions?>`):
  - `action-group` `subsysSistemaEducativo.Main@Ciclo-onChange-grado-action` — evento de cambio del campo grado; los eventos siempre apuntan a un `action-group`, nunca a una acción suelta.
  - `action-record` `subsysSistemaEducativo.Main@Ciclo-set-nivel-null-action` — pone `nivel` a `null` (`U-ciclos-003`), mismo patrón que `Main-Centro.xml` con provincia/municipio. Así el usuario ve lo que va a quedar guardado: si el grado nuevo no admite nivel, el panel desaparece **ya vacío**; y si admite otros niveles, no se queda uno de otro grado.

ASCII Layout del panel `Ciclo` (dos estados, porque hay un panel condicional):

```
── estado A: el grado elegido admite nivel ─────────────────────
ccc...nnnnnn   ← code(3) + colOffset(3) + name(6)
ffffffggg...   ← familiaProfesional(6) + grado(3)   [relacionados: clasifican el ciclo]
   panel nivelPanel (colSpan 12, showIf="grado.admiteNivel"):
......lll...   ← colOffset(6) + nivel(3)   [alineado justo bajo grado, borde 6|7]

── estado B: el grado elegido no admite nivel (o no hay grado) ──
ccc...nnnnnn   ← code(3) + colOffset(3) + name(6)
ffffffggg...   ← familiaProfesional(6) + grado(3)
               (el panel nivelPanel colapsa entero: ni hueco ni fila fantasma)
```

Los bordes de columna caen siempre en 6|7 y ningún campo preexistente cambia de ancho: `grado` y `nivel` conservan su `colSpan="3"`, así que las tres últimas columnas de esas dos filas quedan libres, igual que ocurre en el modal del Paso 9. El `panel-related` de cursos ocupa su propia fila completa (`colSpan="12"`).

ASCII Layout del `buttons-panel` del ciclo (preexistente, sin cambios):

```
bb......ccgg   ← btnDelete(2) + colOffset(6) + btnCancel(2) + btnSave(2)
```

ASCII Layout de los dos formularios modales (preexistentes, **fuera del delta**; se reconstruyen aquí para dejar constancia de la auditoría):

Criterio de wrap aplicado (el mismo que en el Paso 9): un widget salta a la fila siguiente cuando `colOffset + colSpan` no cabe en las columnas que quedan libres de la fila en curso, y **el `colOffset` se cuenta siempre desde la columna 1 de la fila en la que el widget acaba cayendo**.

```
Ciclo.Curso — panel «Curso»:
iiiiiiccc···   ← ciclo(6, oculto con showIf="false") + code(3)          [libres: 10-12]
···nnnnnn···   ← name(colOffset 3 + colSpan 6 = 9 > 3 libres → salta;
                  el offset se aplica desde la columna 1 → cols 4-9)
llllll······   ← leyEducativa(6): tras name quedan 3 libres → salta; cols 1-6
bb......ccgg   ← buttons-panel

Ciclo.Curso.CursoModulo — panel «CursoModulo»:
ccccccmmmmmm   ← curso(6, oculto con showIf="false") + modulo(6)
bb......ccgg   ← buttons-panel
```

Los dos modales quedan **fuera del delta**: el dibujo es la auditoría del estado actual, no una recolocación propuesta.

**Verificación del paso:** arranca; al elegir «Ciclo formativo» aparece el campo Nivel con asterisco y el selector solo ofrece los niveles de ese grado; al elegir «Curso de especialización» el campo desaparece y queda vacío.


## Reglas de UI aplicables (trazabilidad, verbatim)

### Reglas de UI `U-<slug-pantalla>-NNN`

| U | Origen spec | Ubicación | Mecanismo |
|---|---|---|---|
| `U-ciclos-001` | `RUI-ciclos-formulario-001` | `views/Main-Ciclo.xml`, `<panel name="nivelPanel">` del form `subsysSistemaEducativo.Main@Ciclo-form` | `showIf="grado.admiteNivel"` en el panel |
| `U-ciclos-002` | `RUI-ciclos-formulario-002` | `views/Main-Ciclo.xml`, `<field name="nivel">` dentro de `nivelPanel` | `required="true"` dentro del panel condicional |
| `U-ciclos-003` | `RUI-ciclos-formulario-003` | `views/Main-Ciclo.xml`, `onChange` de `<field name="grado">` → `…Main@Ciclo-onChange-grado-action` → `…Main@Ciclo-set-nivel-null-action` | `action-group` + `action-record` |
| `U-ciclos-004` | `RUI-ciclos-formulario-004` | `views/Main-Ciclo.xml`, atributo `domain` de `<field name="nivel">` | `domain="self.grado = :grado"` |
| `U-ciclos-005` | `RUI-ciclos-formulario-005` | `views/Main-Ciclo.xml`, `<field name="grado">` del form principal | **ausencia** de `domain` (se elimina el filtro `code='D' OR code='E'`) |
`U-ciclos-001`, `U-ciclos-002`, `U-consulta-ciclo-001` y `U-familias-profesionales-001`/`002` dependen de `Grado.admiteNivel`; el campo llega al cliente porque `<field name="nivel">` declara `depends="grado.admiteNivel"` en los tres formularios (en el modal de `Main-FamiliaProfesional.xml`, `depends="grado.admiteNivel,nivel.grado"`: el segundo campo relacionado es el que hace evaluable en cliente la tercera comprobación de su `Local-validate*`).

## Eliminaciones declaradas para este fichero (verbatim)


| Elemento preexistente eliminado | Fichero | ID de spec que lo justifica |
|---|---|---|
| Atributo `domain="(self.code='D' OR self.code='E')"` del `<field name="grado">` del form `subsysSistemaEducativo.Main@Ciclo-form` | `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Ciclo.xml` | `RUI-ciclos-formulario-005` (el selector de grado ofrece todos los grados del catálogo) y `ESC-008` paso 9 |
| Atributo `colOffset="6"` del `<field name="familiaProfesional">` del form `subsysSistemaEducativo.Main@Ciclo-form` (pasa a `colSpan="6"` sin `colOffset`, abriendo fila en la columna 1; su `colSpan` **no** cambia) | `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Ciclo.xml` | `RUI-ciclos-formulario-001` — recolocación **mínima** para que el panel condicional del nivel quede alineado bajo el grado (razonada en el Paso 7) |
| Atributo `colOffset="6"` del `<field name="grado">` del mismo form (conserva su `colSpan="3"` y pasa a compartir fila con `familiaProfesional`, columnas 7-9) | `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Ciclo.xml` | `RUI-ciclos-formulario-001` — misma recolocación mínima; el `colOffset="6"` se traslada al `<field name="nivel">` dentro de `nivelPanel`, que es el que debe quedar alineado bajo el grado (borde 6\|7) |
| Atributo `domain="(self.code='D' OR self.code='E')"` del `<field name="nivel">` del mismo form (filtro copiado por error del campo `grado`: ningún nivel tiene esos códigos, así que el selector no ofrecía nada) | `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Ciclo.xml` | `RUI-ciclos-formulario-004` (el selector de nivel ofrece los niveles del grado elegido), que lo sustituye |
| Atributos `showIf="grado.code=='D'"` y `requiredIf="grado.code=='D'"` del `<field name="nivel">` del mismo form (la condición pasa a ser «el grado admite nivel», no «el grado es el de código D») | `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Ciclo.xml` | `RUI-ciclos-formulario-001` y `RUI-ciclos-formulario-002` |

## Notas y supuestos aplicables (verbatim)

2. **Borrados fuera de alcance, pero `btnDelete` alineado con la convención.** La spec deja sin decidir qué debe pasar al borrar un grado que todavía tiene niveles o un nivel que todavía usan ciclos. El diseño no añade ninguna `validateRemove`, y las relaciones se declaran sin borrado en cascada (`orphanRemoval="false"` explícito en `Grado.niveles`, ver Paso 2), así que la base de datos rechazará el borrado con su error de integridad. Los `action-group` de `btnDelete` de los formularios **principales** que este diseño reescribe (`Main-Ciclo.xml`, `Main-Nivel.xml` y `Main-FamiliaProfesional.xml`) sí anteponen `remote-validationDelete-action` a `delete`, porque `vistas.md` §1.5 lo exige literalmente y el fichero que `/sdd-implementer` copia verbatim al árbol quedaría incumpliéndolo; la acción es **global** de `DefaultModelController`, así que no añade ninguna pieza nueva ni conocimiento tácito, y hoy resuelve a un `validateRemove` sin reglas. Los formularios **modales** siguen con `delete-modal` a secas (`vistas.md` §1.6: un modal **MUST NOT** llevar `remote-validation*`).
4. **`required` dentro del panel condicional.** `U-ciclos-002` se materializa como `required="true"` dentro del panel que ya lleva la condición, en vez de repetir la expresión en un `requiredIf` (ver `decisiones.md` D6). Se apoya en que `useGetErrors` de `axelor-front` descarta los widgets cuyo padre está oculto. Si al ejecutar los tests E2E se observara que un ciclo sin nivel no deja guardar con el panel oculto, la vuelta atrás es sustituir ese `required="true"` por `requiredIf="grado.admiteNivel"` en el mismo campo, sin tocar nada más.
