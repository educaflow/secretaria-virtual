---
type: implementation-task
template: system
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-secure-coding

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.base.infrastructure.mapper.BeanMapperModel`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Sección concreta de `design/test-unit-desc.md` que describe esta clase: `## Clase: `com.educaflow.base.infrastructure.mapper.BeanMapperModel`  —  helper de infraestructura (se **modifica**)` (con todos sus sub-apartados `### Método: …`). Aplica además la sección `## Convenciones` del mismo fichero.
- Ubicación de salida — **son DOS ficheros, y esta tarea es dueña de los dos**:
  - `src/test/java/com/educaflow/base/infrastructure/mapper/BeanMapperModelTest.java` → los casos **(a)–(g)** y el caso **(i)** (nuevo: el `@OneToOne` real `ParentModel.ref`) más el caso borde de id inexistente.
  - `src/test/java/com/educaflow/base/infrastructure/mapper/BeanMapperModelTest2.java` → el caso **(h)**: la **reescritura** del test preexistente `PreserveIdTests.originalId_isPreservedAfterUpdate`, que codifica el contrato **antiguo** del mapeador y con el delta de la tarea 01 queda en **rojo**. Se reescribe **en su sitio** (misma clase anidada `PreserveIdTests`), con `@DisplayName` y nombre de método nuevos: **MUST NOT** quedarse un test llamado `originalId_isPreservedAfterUpdate` afirmando lo contrario de lo que hace el código. **MUST NOT** tocarse ningún otro test de ese fichero.
- **Este test en rojo NO es un fallo del código ni motivo de BLOCKED.** Es el único test preexistente que el delta invalida, la decisión de actualizarlo es firme (`decisiones.md` D3 y la sección `## Tests` del `design.md`) y **esta tarea es su dueña**. **MUST NOT** revertirse el delta de `BeanMapperModel.java`, ni acotarse a many-to-one, ni tocarse código de producción para que ese test pase.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita (p.ej. la descripción dice
      `insert(X)` y el código tiene `guardarX(X, Long)`), o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más). En una clase
      que el diseño **modifica** (fila `Acción: Modificar` — ya existía antes de la iniciativa), este criterio se
      acota a la superficie **nueva/cambiada**: los métodos públicos **preexistentes** de la clase NO son motivo
      de BLOCKED (puedes leer el fichero real, o su `git diff`, para distinguirlos).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.

**CRITICAL — es infraestructura compartida.** `BeanMapperModel` es la clase que la tarea 01 modifica; el Paso 1 le cambia la semántica, así que el cambio **MUST NOT** entrar sin los tests que la fijan. La tabla de casos mínimos (a)–(g) del `design.md` es obligatoria y la sección de `test-unit-desc.md` la cubre entera. La clase es `Acción: Modificar`, así que sus métodos públicos preexistentes no son superficie de más.

La descripción declara un **doble de test ya existente** (`FakeModelLoader`) y un **fixture nuevo solo de test** (`MtoHolderModel`): créalos/úsalos tal como la descripción los define, y **MUST NOT** tocar código de producción para que un test pase.

## Tabla de casos mínimos del `design.md` (sección «Tests», verbatim)

**CRITICAL — el `test-unit-desc.md` MUST cubrir `BeanMapperModel`.** Es infraestructura compartida y el Paso 1 le cambia la semántica, así que el cambio no debe entrar sin tests que la fijen. Casos mínimos, todos sobre la rama «propiedad de tipo `Model`, `rawValue != null && valueDest != null`»:

| # | Caso | Comportamiento esperado |
|---|---|---|
| a | many-to-one con **id distinto** del de la entidad referenciada actual | la referencia se **sustituye** (se carga la otra entidad por id) |
| b | many-to-one con el **mismo id** | comportamiento actual: no se sustituye, se copian los campos permitidos y se restaura el id |
| c | many-to-one a una **entidad de negocio** con id distinto y **mapa interno vacío** (deny-all) dentro de una whitelist escrita a mano | **SÍ** se sustituye: la entrada externa de la whitelist es la que autoriza la propiedad. Es el caso real de `Ciclo.grado` y el de `CertificadoDigital.dispositivoCriptografico`/`alias`, donde hace que la validación de la pareja («el nivel pertenece al grado», «el alias pertenece al dispositivo») se evalúe sobre lo que el usuario acaba de elegir |
| d | `rawValue` **sin `id`** | comportamiento actual (no se sustituye) |
| e | `MetaFile` con id distinto | **sin regresión**: se sigue sustituyendo, ahora por la regla general |
| f | `valueDest == null` | sin cambios respecto al comportamiento actual |
| g | many-to-one a una **entidad de negocio** con id distinto y **whitelist externa allow-all** (`createAllowAllProperties`, el caso de toda entidad sin `ModelService` propio y el de `CorreoServiceImpl` en modificación) | la referencia se **sustituye** y **NO** se copia ningún campo dentro de la entidad referenciada anterior (hoy, con mapa interno allow-all, sí se copiaban: mass-assignment de rebote, `k-secure-coding` §3) |


## Corrección del diseño sobre los tests preexistentes del mapeador (sección «Tests» del `design.md`, verbatim)

**Los tests ya escritos del mapeador NO siguen todos verdes sin cambios: uno codifica el contrato antiguo y se actualiza.** El delta del Paso 1 deja en **rojo** exactamente **un** test preexistente, `BeanMapperModelTest2.PreserveIdTests.originalId_isPreservedAfterUpdate` (`src/test/java/com/educaflow/base/infrastructure/mapper/BeanMapperModelTest2.java:1008-1050`), que fija el comportamiento **antiguo**: mapa entrante con `id` distinto (`999`) del de la entidad referenciada (`100`) → se conserva la instancia destino y se le **restaura** su id. Ese test **se actualiza al contrato nuevo** (la reescritura está descrita como caso **(h)** de `test-unit-desc.md`, y la tarea 15 es su dueña). **MUST NOT** revertirse el delta, ni acotarse a many-to-one, para mantenerlo verde: el alcance —one-to-one **y** many-to-one, sin filtro por tipo de relación— es **firme** y está razonado en `decisiones.md` D3. Las tres razones verificadas:

1. **No hay ninguna diferencia de persistencia entre one-to-one y many-to-one en la que apoyar una asimetría.** En `axelor-tools .../code/entity/model/Property.java`, `$one2one()` (`:1203-1220`) y `$many2one()` (`:1222-1229`) llaman ambos a `applyCascade(...)`, que resuelve a `DEFAULT_CASCADE` (`:1168-1170`) = `cascade={PERSIST, MERGE}` y **sin** `orphanRemoval` (en `$one2one()` solo se añade si el dominio lo declara explícitamente, cosa que no hace ninguno del proyecto). Sustituir la referencia **no** deja huérfana la fila anterior, ni en un caso ni en el otro.
2. **Los dos únicos `<one-to-one>` reales del proyecto no son composición exclusiva.** `Expediente.personaSolicitante` y `Expediente.personaInteresada` (`src/main/java/com/educaflow/subsystem/expedientes/domains/Expediente.xml:21-22`) apuntan a la **misma** entidad compartida `Persona`: `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/InitialEventManagerImpl.java:28-33` crea **una** `Persona` y la asigna a los dos campos. Con el comportamiento antiguo, elegir otra persona en uno de ellos **renombraría a la actual** (le copiaría encima los campos del mapa) en vez de cambiar la referencia — que es justo el mass-assignment de rebote que el delta cierra.
3. **El test que queda en rojo no codifica ningún contrato sobre one-to-one.** Su fixture está documentado como «relación Model (OneToOne / ManyToOne)» (`BeanMapperModelTest2.java:87`), `AddressModel` es un POJO **sin ninguna anotación JPA**, y en todo el fichero **no existe ni un solo** mock `isManyToOne(...) → true`: las **9** parejas de stubs `isOneToOne(any(), eq("address")) → true` van siempre acompañadas de `isManyToOne(any(), any()) → false` (`:807-808`, `:830-831`, `:854-855`, `:890-891`, `:919-920`, `:971-972`, `:1021-1022`, `:1361-1362`, `:1386-1387`; la décima pareja, `:946-947`, pone **ambos** a `false` para el caso de relación no soportada). Esos stubs son solo la forma de pasar la guarda de `BeanMapperModel.java:236-238`, no una afirmación de que la relación sea un one-to-one.

**Cobertura positiva sobre un `@OneToOne` real — caso (i).** Con el delta, la rama nueva se quedaría **sin** ningún test sobre un `@OneToOne` anotado de verdad (el fixture de `Test2` no lleva anotaciones JPA y el `MtoHolderModel` nuevo es `@ManyToOne`). Lo cubre el caso **(i)** de `test-unit-desc.md`: el gemelo del caso (a) sobre `ParentModel.ref` de `BeanMapperModelTest.java:432-433`, que **sí** está anotado `@OneToOne`.

| # | Caso | Comportamiento esperado |
|---|---|---|
| h | reescritura de `BeanMapperModelTest2.PreserveIdTests.originalId_isPreservedAfterUpdate` (one-to-one simulado, id distinto) | la referencia se **sustituye** (`assertNotSame` + id nuevo) y —aserción más fuerte, la que preserva la garantía de seguridad— la entidad **anteriormente referenciada conserva sus datos** (`street` sigue siendo «Antigua»): el mapa del cliente **no edita** una entidad que solo referencia (`k-secure-coding` §3) |
| i | `@OneToOne` **real** (`ParentModel.ref`, `@OneToOne` de verdad) con id distinto, en `BeanMapperModelTest.java` | la referencia se **sustituye** por la cargada del loader y la anterior queda **intacta**; es la cobertura positiva de la rama nueva sobre un one-to-one anotado |

**Tests preexistentes que siguen verdes y MUST NOT tocarse** (no entran en la rama modificada, porque el mapa no trae `id` distinto): `BeanMapperModelTest2.sourceNotNull_destNotNull_updatesInPlace` (`:961-999`, mapa **sin** `id` → sigue actualizando in-place), `BeanMapperModelTest2.PreserveIdTests.listItemOriginalId_isPreservedAfterUpdate` (`:1052-1100`, ítem de lista: el delta no toca la rama de listas) y sus dos correspondientes de `BeanMapperModelTest`: `copyMapToEntity_shouldMapScalarsAndReconcileOneToManyList` (`:222-269`, reconciliación de la lista con preservación del id del ítem) y `copyMapToEntity_shouldLoadExistingModelById_whenTargetRelationIsNull` (`:272-297`, rama `valueDest == null` sobre el `@OneToOne` real, que el delta no modifica).
