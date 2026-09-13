---
type: implementation-task
template: system
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

**El XML ya está materializado en `design/views/Main-FamiliaProfesional.xml`.** **MUST** copiarlo **literalmente** (`cp`) a su ruta destino `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-FamiliaProfesional.xml`. **MUST NOT** regenerarlo, reescribirlo desde el `design.md` ni reformatearlo.

**Acción `Modificar`: el destino YA EXISTE.** Antes de sobrescribirlo aplica la **comprobación de conservación** de `implementation.md` §3: todo elemento con nombre del fichero real actual (campo, panel, botón, acción) **MUST** estar presente en el XML del diseño, **salvo** los listados en la sección «Eliminaciones declaradas» del `design.md` que se reproduce más abajo. Si pasa, sobrescribe (es el comportamiento esperado, **no** es CONFLICT); si falla, reporta `CONFLICT`. **MUST NOT** fusionar los dos ficheros a mano.

Es el **segundo** formulario del proyecto que edita un `Ciclo` (el modal del panel «Ciclos»). **CRITICAL**: los tres `<check>` del `Local-validateSave-action` **MUST** escribirse con la MISMA partición de ramas que `CicloServiceImpl.validateCoherenciaGradoNivel` (tarea 07). **MUST NOT** crearse `FamiliaProfesionalService`/`FamiliaProfesionalServiceImpl`: la omisión es deliberada (`decisiones.md` D8).

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-FamiliaProfesional.xml` | Modificar | k-vistas (forms.md, actions.md) | **Segundo formulario que edita `Ciclo`** (el modal del panel «Ciclos» de la familia profesional): mismo idioma que el principal — nivel en panel condicional, filtro por grado, vaciado al cambiar de grado — más el `Local-validateSave-action` que el modal necesita (`vistas.md` §1.6). |

## Paso del diseño que describe este fichero (verbatim)

### Paso 9 — Vistas: `views/Main-FamiliaProfesional.xml`

Fichero completo en `design/views/Main-FamiliaProfesional.xml` (base real + delta). Es la **segunda** pantalla del proyecto que edita un `Ciclo`: el formulario **modal** del panel «Ciclos» del mantenimiento de familias profesionales. No está descrita en el spec (por eso sus `U-` llevan `Origen spec` `—`), pero edita la misma entidad, así que el criterio de nivel tiene que ser **el mismo**: si se dejara como está, seguiría vivo el criterio viejo `grado.code=='D'` y el `domain` roto del nivel, es decir, una segunda copia de la decisión que `decisiones.md` D1 existe para evitar.

Además, con `CicloServiceImpl` creado la omisión sería una **regresión funcional real**: `FamiliaProfesional.ciclos` es una one-to-many **de composición** (bidireccional, sin `orphanRemoval="false"`), así que `ModelServiceValidationWalker` **sí** desciende por ella al guardar la familia profesional y ejecuta `CicloServiceImpl.validateUpdate` sobre cada ciclo. Un ciclo dado de alta desde ese modal con grado «Ciclo formativo» exigiría nivel, el selector no ofrecería ninguno (el `domain` heredado por error pide códigos `D`/`E`, que ningún nivel tiene) y el guardado del maestro quedaría bloqueado sin salida.

El fichero tiene dos bloques (`FamiliaProfesional` y `FamiliaProfesional.Ciclo`), cada uno con sus cinco PI `sv-*`.

- Preexistente (se conserva): el `action-view`, el grid y el form de familia profesional con su `panel-related` de ciclos y su `buttons-panel`; el grid del modal de ciclo; el `onNew` con el `action-record` que fija el padre `familiaProfesional`; los `action-group` de los tres botones del modal (`delete-modal` / `close` / `save-modal`).
- Delta en el form **principal** (`…Main@FamiliaProfesional-form`): el `action-group` de `btnDelete` antepone `remote-validationDelete-action` a `delete` (`vistas.md` §1.5). Nada más cambia en el maestro.
- Delta en el form **modal** (`…Main@FamiliaProfesional.Ciclo-form`), exactamente el mismo idioma que el formulario principal de ciclo del Paso 7:
  - Se **elimina** el `domain="(self.code='D' OR self.code='E')"` del campo `grado` (mismo motivo que en `Main-Ciclo.xml`).
  - `grado` gana `onChange="subsysSistemaEducativo.Main@FamiliaProfesional.Ciclo-onChange-grado-action"`, que apunta al `action-record` `…-set-nivel-null-action` (pone `nivel` a `null`).
  - El campo `nivel` se muda a un panel anidado propio `nivelPanel` (`colSpan="12"`, `showFrame="false"`, `showIf="grado.admiteNivel"`), con `required="true"` a secas dentro, `domain="self.grado = :grado"` (que **sustituye** al filtro `(self.code='D' OR self.code='E')` copiado por error del campo `grado`) y `depends="grado.admiteNivel,nivel.grado"`. Se eliminan sus `showIf="grado.code=='D'"` y `requiredIf="grado.code=='D'"`.
  - `depends` declara **dos** campos relacionados, uno más que en los formularios del Paso 7 y el Paso 8: `grado.admiteNivel` alimenta el `showIf` del panel y `nivel.grado` es lo que hace **evaluable en cliente** la tercera comprobación del `Local-validate*` (abajo).
  - **`Local-validateSave-action` nuevo** (`action-condition` `…Main@FamiliaProfesional.Ciclo-Local-validateSave-action`, primera acción del `action-group` de `btnSave`, antes de `save-modal`): tres `<check field="nivel" …>` que duplican en cliente `V-Ciclo-001`, `V-Ciclo-002` y `V-Ciclo-003` con los literales de `RES-Ciclo-001/002/003`. Es **REQUIRED** por `vistas.md` §1.6 y `design-contract.md` §5: `save-modal` no llama al servidor, el modal **MUST NOT** llevar `remote-validation*` y la validación de servidor del detalle solo corre al guardar el maestro, así que este es el único aviso al usuario antes de cerrar el modal.
  - **CRITICAL — los tres `check` MUST escribirse con la MISMA partición de ramas que el servidor** (`CicloServiceImpl.validateCoherenciaGradoNivel`, `decisiones.md` D4), porque son **la misma decisión** escrita dos veces y no pueden divergir en su estructura de ramas:

    | # | `if` del `<check>` | Rama equivalente del servidor |
    |---|---|---|
    | 1 | `grado != null && grado.admiteNivel && nivel == null` | rama «el grado admite nivel» → `V-Ciclo-001` |
    | 2 | `grado != null && !grado.admiteNivel && nivel != null` | rama «el grado NO admite nivel» → `V-Ciclo-002` |
    | 3 | `grado != null && grado.admiteNivel && nivel != null && nivel.grado != null && nivel.grado.id != grado.id` | rama «el grado admite nivel» → `V-Ciclo-003` |

    Las guardas `grado.admiteNivel` / `!grado.admiteNivel` hacen la partición **complementaria**: todo caso cae en exactamente una y el usuario ve **un solo mensaje**, igual que en el servidor. La guarda del check 3 **MUST NOT** omitirse: sin ella, un grado que no admite nivel con un nivel puesto dispararía a la vez el check 2 y el check 3 (un nivel de otro grado nunca cumple `nivel.grado.id == grado.id`), y el modal mostraría dos mensajes donde el servidor da uno.

ASCII Layout del panel `Ciclo` del modal (dos estados; `familiaProfesional` va oculto con `showIf="false"`, y un widget oculto **reserva sus columnas**, así que se dibuja):

```
── estado A: el grado elegido admite nivel ─────────────────────
ffffffccc...   ← familiaProfesional(6, oculto: reserva columnas) + code(3)
...nnnnnn...   ← colOffset(3) + name(6)
......ggg...   ← colOffset(6) + grado(3)
   panel nivelPanel (colSpan 12, showIf="grado.admiteNivel"):
......lll...   ← colOffset(6) + nivel(3)   [alineado justo bajo grado, borde 6|7]

── estado B: el grado elegido no admite nivel (o no hay grado) ──
ffffffccc...   ← familiaProfesional(6, oculto) + code(3)
...nnnnnn...   ← colOffset(3) + name(6)
......ggg...   ← colOffset(6) + grado(3)
               (el panel nivelPanel colapsa entero: ni hueco ni fila fantasma)
```

`grado` y `nivel` conservan su `colSpan="3"` original: el delta no los redimensiona, solo baja el nivel a su panel y lo alinea bajo el grado (borde 6|7 en las tres filas). Es el **mismo** ancho que tienen en el formulario principal del Paso 7, así que las dos vistas que editan `Ciclo` quedan con el mismo idioma de maqueta.

ASCII Layout de los dos `buttons-panel` del fichero (preexistentes, sin cambios):

```
bb......ccgg   ← btnDelete(2) + colOffset(6) + btnCancel(2) + btnSave(2)
```

**Verificación del paso:** arranca; en `Sistema educativo → Familia profesional`, abrir una familia y pulsar «Añadir un nuevo ciclo»: al elegir «Ciclo formativo» aparece el campo Nivel con asterisco y el selector ofrece los tres niveles de ese grado (antes no ofrecía ninguno); al elegir «Curso de especialización» el campo desaparece y queda vacío; guardar el modal sin nivel con grado «Ciclo formativo» muestra «El nivel es obligatorio para el grado indicado» sin cerrar el modal.

**Este paso NO crea `FamiliaProfesionalService`/`FamiliaProfesionalServiceImpl`:** la familia sigue resolviendo a `DefaultModelService` (allow-all), y eso es **deliberado**. Razón y evidencia en `decisiones.md` **D8** (y el resumen en «Frontera de confianza → Limitación conocida»). **MUST NOT** añadirse ese servicio como arreglo de paso al ver el allow-all.


## Reglas de UI aplicables (trazabilidad, verbatim)

### Reglas de UI `U-<slug-pantalla>-NNN`

| U | Origen spec | Ubicación | Mecanismo |
|---|---|---|---|
| `U-familias-profesionales-001` | `—` | `views/Main-FamiliaProfesional.xml`, `<panel name="nivelPanel">` del form modal `subsysSistemaEducativo.Main@FamiliaProfesional.Ciclo-form` | `showIf="grado.admiteNivel"` en el panel |
| `U-familias-profesionales-002` | `—` | `views/Main-FamiliaProfesional.xml`, `<field name="nivel">` dentro de `nivelPanel` | `required="true"` dentro del panel condicional |
| `U-familias-profesionales-003` | `—` | `views/Main-FamiliaProfesional.xml`, `onChange` de `<field name="grado">` → `…Main@FamiliaProfesional.Ciclo-onChange-grado-action` → `…-set-nivel-null-action` | `action-group` + `action-record` |
| `U-familias-profesionales-004` | `—` | `views/Main-FamiliaProfesional.xml`, atributo `domain` de `<field name="nivel">` | `domain="self.grado = :grado"` |
| `U-familias-profesionales-005` | `—` | `views/Main-FamiliaProfesional.xml`, `<field name="grado">` del form modal | **ausencia** de `domain` (se elimina el filtro `code='D' OR code='E'`) |
Las cinco `U-familias-profesionales-*` llevan `Origen spec` `—` porque la pantalla «Familias profesionales» no está descrita en el spec: no nacen de ninguna `RUI-`, sino de la obligación de `design-contract.md` §1.3 (una vista que edita la misma entidad no puede quedarse con el criterio viejo) y de la regresión funcional que describe el Paso 9. Son la **misma** decisión que `U-ciclos-001..005`, aplicada al segundo formulario; el dueño de la clasificación sigue siendo único (`Grado.admiteNivel`).
`U-ciclos-001`, `U-ciclos-002`, `U-consulta-ciclo-001` y `U-familias-profesionales-001`/`002` dependen de `Grado.admiteNivel`; el campo llega al cliente porque `<field name="nivel">` declara `depends="grado.admiteNivel"` en los tres formularios (en el modal de `Main-FamiliaProfesional.xml`, `depends="grado.admiteNivel,nivel.grado"`: el segundo campo relacionado es el que hace evaluable en cliente la tercera comprobación de su `Local-validate*`).

## Eliminaciones declaradas para este fichero (verbatim)


| Elemento preexistente eliminado | Fichero | ID de spec que lo justifica |
|---|---|---|
| Atributo `domain="(self.code='D' OR self.code='E')"` del `<field name="grado">` del form modal `subsysSistemaEducativo.Main@FamiliaProfesional.Ciclo-form` | `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-FamiliaProfesional.xml` | `U-familias-profesionales-005` (misma decisión que `RUI-ciclos-formulario-005` aplicada al segundo formulario que edita `Ciclo`) |
| Atributo `domain="(self.code='D' OR self.code='E')"` del `<field name="nivel">` del mismo form modal (filtro copiado por error del campo `grado`: ningún nivel tiene esos códigos, así que el selector no ofrecía nada) | `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-FamiliaProfesional.xml` | `U-familias-profesionales-004`, que lo sustituye por `self.grado = :grado` |
| Atributos `showIf="grado.code=='D'"` y `requiredIf="grado.code=='D'"` del `<field name="nivel">` del mismo form modal | `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-FamiliaProfesional.xml` | `U-familias-profesionales-001` y `U-familias-profesionales-002` |

## Capa cliente de las V-Ciclo-* en el modal (trazabilidad, verbatim)

En el formulario **modal** de ciclo de `Main-FamiliaProfesional.xml` es distinto y la capa cliente es **REQUIRED** (`vistas.md` §1.6, `design-contract.md` §5): `save-modal` no llama al servidor, el modal **MUST NOT** llevar `remote-validation*` y la validación de servidor del detalle solo corre al guardar el maestro (`ModelServiceValidationWalker`), así que las tres `V-Ciclo-*` se duplican ahí en el `action-condition` `…Main@FamiliaProfesional.Ciclo-Local-validateSave-action` con los literales de `RES-Ciclo-001/002/003` y con **la misma partición de ramas** que `validateCoherenciaGradoNivel` (checks 1 y 3 bajo la guarda `grado.admiteNivel`, check 2 bajo `!grado.admiteNivel`; ver la tabla del Paso 9): son la misma decisión escrita dos veces, así que producen el mismo mensaje único por situación. El dato `grado.admiteNivel` y la pareja `nivel.grado` llegan al cliente por el `depends` del campo `nivel`. Los otros dos modales del diseño (los de `Curso` y `CursoModulo`, en `Main-Ciclo.xml`) no tienen validaciones nuevas que duplicar.

## Notas y supuestos aplicables (verbatim)

2. **Borrados fuera de alcance, pero `btnDelete` alineado con la convención.** La spec deja sin decidir qué debe pasar al borrar un grado que todavía tiene niveles o un nivel que todavía usan ciclos. El diseño no añade ninguna `validateRemove`, y las relaciones se declaran sin borrado en cascada (`orphanRemoval="false"` explícito en `Grado.niveles`, ver Paso 2), así que la base de datos rechazará el borrado con su error de integridad. Los `action-group` de `btnDelete` de los formularios **principales** que este diseño reescribe (`Main-Ciclo.xml`, `Main-Nivel.xml` y `Main-FamiliaProfesional.xml`) sí anteponen `remote-validationDelete-action` a `delete`, porque `vistas.md` §1.5 lo exige literalmente y el fichero que `/sdd-implementer` copia verbatim al árbol quedaría incumpliéndolo; la acción es **global** de `DefaultModelController`, así que no añade ninguna pieza nueva ni conocimiento tácito, y hoy resuelve a un `validateRemove` sin reglas. Los formularios **modales** siguen con `delete-modal` a secas (`vistas.md` §1.6: un modal **MUST NOT** llevar `remote-validation*`).
9. **Comportamiento del walker con `orphanRemoval="false"`.** Con el atributo declarado a `false` en `Grado.niveles`, `ModelServiceValidationWalker` (`:253-261`) **no** desciende por esa colección al guardar un grado, que es lo correcto: los niveles no son detalles de composición del grado, se mantienen desde su propia pantalla. Lo contrario ocurre —y se aprovecha— en `FamiliaProfesional.ciclos`, que sí es una colección de composición: por ella el walker desciende y ejecuta `CicloServiceImpl.validate*` sobre cada ciclo del modal (ver Paso 9).
