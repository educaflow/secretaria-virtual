---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

Crea el servicio de `Ciclo`: la interfaz y su implementación, que son un único componente lógico.

- `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/CicloService.java` (`Crear`)
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/impl/CicloServiceImpl.java` (`Crear`)

Los dominios XML ya están colocados en `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/` por las tareas 02-04 y son **contrato fijo**: las entidades JPA generadas mandan sobre el Java. **MUST NOT** editarlos ni regenerarlos. El dato «¿este grado admite nivel?» se pregunta SIEMPRE a `Grado.admiteNivel` (el campo calculado que la tarea 02 materializa), nunca contando niveles aquí.

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/CicloService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de `Ciclo`. |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/impl/CicloServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | `V-Ciclo-001`, `V-Ciclo-002`, `V-Ciclo-003` + whitelists de alta y modificación. |

## Paso 4 del diseño (verbatim)

### Paso 4 — Servicio de `Ciclo`

```java
// Clase: com.educaflow.subsystem.sistemaeducativo.service.CicloService
public interface CicloService extends ModelService<Ciclo>;
//   Sin métodos propios.

// Clase: com.educaflow.subsystem.sistemaeducativo.service.impl.CicloServiceImpl
//   extends DefaultModelService<Ciclo> implements CicloService

public CicloServiceImpl(Class<Ciclo> model, Repository<Ciclo> repository);
//   Constructor obligatorio: super(model, repository).

// Bloque 1 — acciones: ninguna. No se sobrescriben insert/update/remove.

// Bloque 2 — Métodos de Validación
@Override
public Optional<BusinessMessages> validateInsert(Ciclo ciclo);
//   Delega en validateCoherenciaGradoNivel(ciclo).

@Override
public Optional<BusinessMessages> validateUpdate(Ciclo ciclo, Ciclo cicloOriginal);
//   Delega en el MISMO validateCoherenciaGradoNivel(ciclo): las tres reglas son restricciones
//   (invariantes de la entidad), así que valen igual en el alta y en la modificación
//   (k-validaciones/restricciones.md §2). No se comparan con `cicloOriginal`: la spec no declara
//   ninguna regla de transición.

private Optional<BusinessMessages> validateCoherenciaGradoNivel(Ciclo ciclo);
//   Único sitio donde vive la coherencia grado↔nivel. Estructura (ver decisiones.md D4):
//
//   Rama previa — si el ciclo no tiene grado, no se evalúa ninguna de las tres reglas y se
//   devuelve Optional.empty(). No es un retorno defensivo que delegue en la nada: el invariante
//   «el ciclo tiene grado» tiene dueño propio y verificable, `Ciclo.grado required="true"`
//   (@NotNull + columna NOT NULL), así que sin grado la fila no puede llegar a existir; y sin
//   grado no hay dominio contra el que comparar el nivel.
//
//   Decisión con dos ramas COMPLEMENTARIAS sobre ciclo.getGrado().getAdmiteNivel() — todo caso
//   posible cae en exactamente una, y cada situación produce un solo mensaje:
//     - Rama «el grado admite nivel»:
//         · V-Ciclo-001 (Origen spec: RES-Ciclo-001) si el ciclo no tiene nivel. Mensaje: el
//           literal que declara RES-Ciclo-001 en entity-Ciclo.md (MUST copiarse tal cual).
//           Anclado al campo `nivel`.
//         · V-Ciclo-003 (Origen spec: RES-Ciclo-003) si el ciclo tiene nivel y el grado de ese
//           nivel no es el grado del ciclo. Mensaje: el literal de RES-Ciclo-003. Anclado a `nivel`.
//           La comparación es entre las dos referencias a Grado (por id de entidad), no por código.
//     - Rama «el grado NO admite nivel»:
//         · V-Ciclo-002 (Origen spec: RES-Ciclo-002) si el ciclo tiene nivel. Mensaje: el literal
//           de RES-Ciclo-002. Anclado a `nivel`. En esta rama V-Ciclo-003 NO se evalúa: si el grado
//           no admite nivel, CUALQUIER nivel sobra —da igual si está archivado y si pertenece o no
//           a ese grado—, y RES-Ciclo-002 es el mensaje correcto porque es el más informativo de
//           los dos («este grado no lleva nivel» explica el problema; «el nivel es de otro grado»
//           mandaría al usuario a buscar un nivel de ese grado que no debe poner). Ojo: con D7
//           (`admiteNivel` = «tiene al menos un nivel NO archivado») un grado sin admiteNivel SÍ
//           puede ser el grado del nivel recibido —si todos sus niveles están archivados—, así que
//           la razón para no evaluar V-Ciclo-003 aquí NO es que el caso sea imposible, sino que el
//           nivel sobra igualmente y el mensaje de V-Ciclo-002 es el que procede.
//
//   El dato «¿este grado admite nivel?» se pregunta SIEMPRE al único dueño, Grado.admiteNivel
//   (CC-Grado-001). MUST NOT contarse aquí los niveles del grado ni replicar el criterio.
//   Acumula los mensajes en un BusinessMessages y devuelve el Optional vacío cuando la validación
//   es válida y el BusinessMessages acumulado cuando no lo es (patrón canónico de k-validaciones §2).

// Bloque 3 — AllowProperties
@Override
public AllowProperties allowPropertiesInsert();
//   Devuelve allowPropertiesEditables().

@Override
public AllowProperties allowPropertiesUpdate();
//   Devuelve allowPropertiesEditables(): entity-Ciclo.md declara la misma lista en Crear y Modificar.

private AllowProperties allowPropertiesEditables();
//   Único dueño de la whitelist de Ciclo (ver «Frontera de confianza» para el detalle y el porqué
//   de cada entrada): `code`, `name`, `familiaProfesional` (mapa interno vacío), `grado` (vacío),
//   `nivel` (vacío) y `cursos` con el subárbol que el panel maestro-detalle edita
//   (`code`, `name`, `ciclo`, `leyEducativa` y `modulos` → `curso`, `modulo`).
//   CRITICAL: si `cursos` se dejara fuera o con mapa interno vacío, el panel de cursos del
//   formulario de ciclo dejaría de guardar sus cursos (ESC-011) y sus módulos (ESC-015).
```

**Verificación del paso:** compila; guardar un ciclo de grado «Ciclo formativo» sin nivel devuelve el mensaje de `RES-Ciclo-001` y no crea la fila.


## Frontera de confianza — AllowProperties por acción (verbatim)


El diseño no declara ninguna acción propia con `@CallMethod`. Las whitelists que siguen son las de `insert`/`update`, y se aplican en los dos caminos por los que puede llegar un guardado **de esa misma entidad**:

- **Vía A (botón de la UI):** `remote-validationSave-action` → `DefaultModelController.validateSave` (que es un `@CallMethod`) → `ActionRequestHelper.getModel(allowPropertiesInsert()|allowPropertiesUpdate())`.
- **Vía B (endpoint REST genérico `POST /ws/rest/<FQN>`):** `Resource.save` → `ModelService.validate(json, context)` → `AllowProperties.filter(json, allowPropertiesInsert()|allowPropertiesUpdate())`.

Convención de esta sección: un campo relacional que se acepta **solo como referencia** lleva mapa interno **vacío**; con él, `AllowProperties.filter` deja pasar únicamente `id`/`version` y `BeanMapperModel` no copia ningún campo dentro de la entidad apuntada.

### `CicloServiceImpl.insert` y `CicloServiceImpl.update`

Entidad: `Ciclo`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de las acciones `Crear` y `Modificar` de `entity-Ciclo.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `code` | cliente | sí | Input directo del usuario. |
| `name` | cliente | sí | Input directo del usuario. |
| `familiaProfesional` | cliente | sí (mapa interno vacío) | Referencia elegida en el formulario; no se copian sus campos. |
| `grado` | cliente | sí (mapa interno vacío) | Referencia elegida en el formulario; la validan `V-Ciclo-001`/`V-Ciclo-002`/`V-Ciclo-003`. |
| `nivel` | cliente | sí (mapa interno vacío) | Referencia elegida en el formulario; la validan `V-Ciclo-001`/`V-Ciclo-002`/`V-Ciclo-003`. El `showIf`/`domain` de la vista **NO** es defensa: la pareja grado↔nivel se comprueba siempre en el servidor. |
| `cursos` | cliente | sí (subárbol acotado) | «Cursos del ciclo» está en la línea `Input AllowProperties` del spec y el panel maestro-detalle los guarda con el ciclo. Subárbol permitido: `code`, `name`, `ciclo`, `leyEducativa` (vacío) y `modulos` → `curso`, `modulo` (vacíos). Solo estas propiedades: cualquier otra del curso o del módulo se descarta **cuando el guardado entra por `Ciclo`** (Vías A y B). Si el guardado entra por la familia profesional, gobierna la whitelist del maestro — ver la limitación conocida al final de esta sección. |

Notas sobre el subárbol de `cursos` **cuando el guardado entra por `Ciclo`** (las dos vías se comportan igual y ninguna permite reparentar; si el guardado entra por la familia profesional, la whitelist que se aplica es la del maestro — ver la limitación conocida al final de esta sección):

- El campo padre `ciclo` del curso lo clasifica `k-secure-coding` §3.6 como `cliente` (lo rellena la regla de UI `set-ciclo-parent` y viaja en el JSON), por eso está en la whitelist. Pero **quien manda es el framework**: en la Vía B `JPA.edit` borra la clave `mappedBy` del mapa del hijo y fija la asociación al maestro, y en la Vía A `BeanMapperModel` asigna el padre desde el maestro, no desde el mapa. Un `ciclo` falsificado en el JSON no reparenta nada.
- Lo mismo vale para `curso` dentro de `modulos`.

### Limitación conocida — la vía maestro `FamiliaProfesional.ciclos`

Las Vías A y B de arriba son las de un guardado **DE `Ciclo`**. Hay un **tercer** camino: el guardado **DE `FamiliaProfesional`** con su panel «Ciclos» (el que toca el Paso 9). Ahí la whitelist que se aplica es la del **maestro**, y `FamiliaProfesional` no tiene `ModelService` propio: cae en `DefaultModelService`, que es **allow-all** (`createAllowAllProperties()`, el mapa `{"*": null}`), y `AllowProperties.innerAllowProperties` propaga ese allow-all hacia abajo — `ciclos` → `cursos` → `modulos`. La whitelist de `CicloServiceImpl` **no gobierna esa vía**.

Lo que sí corre por esa vía son las **validaciones**: `validateSaveTree` (`Resource.java:1313`) desciende por la one-to-many de composición y ejecuta `CicloServiceImpl.validateInsert`/`validateUpdate` sobre cada ciclo del panel, así que `V-Ciclo-001`, `V-Ciclo-002` y `V-Ciclo-003` se cumplen también ahí.

La superficie extra que abre el allow-all del maestro se limita, en la práctica, a `archived` (ninguna de estas entidades tiene campos `servidor`, y los demás heredados llevan el setter `private`), y está acotada por `checkRelationalPermissions` (`Resource.java:1296` y `:1346-1387`), que autoriza el subárbol **entidad por entidad** con el `JpaSecurity` real. La decisión de documentar la limitación en vez de crear `FamiliaProfesionalService`, con las cuatro evidencias que la sostienen, está en `decisiones.md` **D8**.


## Trazabilidad V/R/U → ubicación (filas aplicables, verbatim)

### Validaciones `V-<Entidad>-NNN`

| V | Origen spec | Ubicación | Capa |
|---|---|---|---|
| `V-Ciclo-001` | `RES-Ciclo-001` | `CicloServiceImpl.validateInsert` y `.validateUpdate` → `validateCoherenciaGradoNivel`, rama «el grado admite nivel» | servidor (`validate*`) |
| `V-Ciclo-002` | `RES-Ciclo-002` | `CicloServiceImpl.validateInsert` y `.validateUpdate` → `validateCoherenciaGradoNivel`, rama «el grado no admite nivel» | servidor (`validate*`) |
| `V-Ciclo-003` | `RES-Ciclo-003` | `CicloServiceImpl.validateInsert` y `.validateUpdate` → `validateCoherenciaGradoNivel`, rama «el grado admite nivel» | servidor (`validate*`) |

En el formulario **principal** de ciclo (`Main-Ciclo.xml`) las tres `V-Ciclo-*` no se duplican en cliente: dependen de un dato del grado que vive en la base de datos, no del registro que hay en el formulario (`k-validaciones/validaciones.md` §3), y ahí la validación de servidor sí corre antes de guardar (`remote-validationSave-action`). Su capa cliente es la que aportan las reglas de UI (`U-ciclos-001`, `U-ciclos-002` y `U-ciclos-004`), que evitan que el usuario llegue siquiera a esa situación.

En el formulario **modal** de ciclo de `Main-FamiliaProfesional.xml` es distinto y la capa cliente es **REQUIRED** (`vistas.md` §1.6, `design-contract.md` §5): `save-modal` no llama al servidor, el modal **MUST NOT** llevar `remote-validation*` y la validación de servidor del detalle solo corre al guardar el maestro (`ModelServiceValidationWalker`), así que las tres `V-Ciclo-*` se duplican ahí en el `action-condition` `…Main@FamiliaProfesional.Ciclo-Local-validateSave-action` con los literales de `RES-Ciclo-001/002/003` y con **la misma partición de ramas** que `validateCoherenciaGradoNivel` (checks 1 y 3 bajo la guarda `grado.admiteNivel`, check 2 bajo `!grado.admiteNivel`; ver la tabla del Paso 9): son la misma decisión escrita dos veces, así que producen el mismo mensaje único por situación. El dato `grado.admiteNivel` y la pareja `nivel.grado` llegan al cliente por el `depends` del campo `nivel`. Los otros dos modales del diseño (los de `Curso` y `CursoModulo`, en `Main-Ciclo.xml`) no tienen validaciones nuevas que duplicar.

## Notas y supuestos aplicables (verbatim)

7. **Niveles archivados.** `CC-Grado-001` dice literalmente «cierto si el grado tiene al menos un nivel en el catálogo de niveles». El diseño lo **ajusta** a «al menos un nivel **no archivado**» (ver `decisiones.md` D7). Motivo: el selector de nivel filtra por `domain="self.grado = :grado"` y Axelor no ofrece registros archivados, así que un grado con **todos** sus niveles archivados daría `admiteNivel = true`, mostraría el panel con el nivel marcado obligatorio, `V-Ciclo-001` lo exigiría y el usuario no tendría ninguno que elegir: un estado sin salida en el que ningún ciclo de ese grado se podría guardar. El ajuste es coherente con la intención de la regla («que el grado sepa si sus ciclos pueden llevar nivel»), no con su letra.
9. **Comportamiento del walker con `orphanRemoval="false"`.** Con el atributo declarado a `false` en `Grado.niveles`, `ModelServiceValidationWalker` (`:253-261`) **no** desciende por esa colección al guardar un grado, que es lo correcto: los niveles no son detalles de composición del grado, se mantienen desde su propia pantalla. Lo contrario ocurre —y se aprovecha— en `FamiliaProfesional.ciclos`, que sí es una colección de composición: por ella el walker desciende y ejecuta `CicloServiceImpl.validate*` sobre cada ciclo del modal (ver Paso 9).

## Clasificación `cliente` / `servidor` de los campos (filas aplicables, verbatim)

| Entidad | Campo | Origen | Motivo |
|---|---|---|---|
| `Ciclo` | `code` | cliente | En `Input AllowProperties` de Crear y Modificar (`entity-Ciclo.md`). |
| `Ciclo` | `name` | cliente | En `Input AllowProperties` de Crear y Modificar. |
| `Ciclo` | `familiaProfesional` | cliente | En `Input AllowProperties` de Crear y Modificar. |
| `Ciclo` | `grado` | cliente | En `Input AllowProperties` de Crear y Modificar; lo validan `V-Ciclo-001`, `V-Ciclo-002` y `V-Ciclo-003`. |
| `Ciclo` | `nivel` | cliente | En `Input AllowProperties` de Crear y Modificar; lo validan `V-Ciclo-001`, `V-Ciclo-002` y `V-Ciclo-003`. |
| `Ciclo` | `cursos` | cliente | «Cursos del ciclo» está en `Input AllowProperties` de Crear y Modificar: el panel maestro-detalle del formulario los guarda junto con el ciclo. |

Ningún campo `servidor` del delta se asigna en una acción, luego **no hay ninguna `R-` en este diseño**: los dos son derivados de solo lectura, el caso que `design-contract.md` §3 exime expresamente de tener una `R-Antes`. Tampoco hay reglas `RN-` en la especificación.
