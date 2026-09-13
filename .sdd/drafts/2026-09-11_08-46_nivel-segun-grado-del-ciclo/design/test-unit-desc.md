# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Aserciones con `org.junit.jupiter.api.Assertions` (`assertEquals`, `assertTrue`, `assertSame`, `assertThrows`…), **no** AssertJ, como el resto de `src/test/java/com/educaflow/...`.
- Las entidades de dominio (`Grado`, `Nivel`, `Ciclo`) se **instancian** (`new …()` + setters); **nunca** se mockean ni se toca la base de datos.
- En los tests de servicio, el `Repository<T>` del constructor se pasa como **mock** (`Mockito.mock(...)`): ninguna de las validaciones de este diseño lo consulta, pero el constructor de `DefaultModelService` lo exige.
- `I18n` se mockea estáticamente en modo **lenient** devolviendo el argumento (patrón de `AdjuntoServiceImplTest`), por si la implementación envuelve los literales en `I18n.get(...)` o toma el `label` con `Mapper.of(...).getProperty(...).getTitle()`. Las aserciones se hacen sobre `BusinessMessage.getMessage()` y `getFieldName()`, **nunca** sobre el `label`.
- Helper de lectura de mensajes, igual que en los tests de servicio existentes: dado el `Optional<BusinessMessages>`, se comprueba `isPresent()` y se leen `get(i).getMessage()` / `get(i).getFieldName()`.

**Decisiones tomadas ante ambigüedades del diseño** (documentadas aquí según el encargo):
1. **`CC-Grado-001` se testea sobre la entidad generada `Grado`.** El cálculo no vive en ningún servicio: el diseño lo escribe como cuerpo `<boolean name="admiteNivel" transient="true">` en `domains/Grado.xml`, y el generador de AOP lo emite como `computeAdmiteNivel()` + getter que recalcula. El cuerpo es **autoría de este diseño** (no código generado trivial), así que `Grado` **no** entra en la exención de «POJO generado sin lógica»: se describe su getter. El test es puro (`new Grado()` + `setNiveles(...)`), sin BD ni mocks.
2. **`Ciclo` con `nivel` cuyo `grado` es `null`.** El diseño no fija qué pasa (el modelo declara `Nivel.grado required="true"`, así que no debería existir). Se decide que `validateCoherenciaGradoNivel` **no lance `NullPointerException`** y trate ese nivel como «no pertenece al grado del ciclo» → `V-Ciclo-003`. Queda descrito como caso borde para que el implementador lo materialice así.
3. **Fixture del test del mapeador.** `BeanMapperModelTest` ya tiene su propio fixture (`FakeModelLoader`, `mapOf(...)`, y los modelos anidados `ParentModel`/`ChildModel`/`RefModel`/`UnsupportedHolderModel`). Los casos nuevos necesitan un **many-to-one** (el `ref` de `ParentModel` es `@OneToOne`) y un many-to-one a `MetaFile`, así que se añade **al propio test** un modelo anidado nuevo `MtoHolderModel extends Model` con `@ManyToOne RefModel ref` y `@ManyToOne MetaFile fichero`. Es fixture de test, **no** una clase del diseño; los modelos existentes **MUST NOT** modificarse para no alterar los tests ya escritos.
4. **Ámbito del test del mapeador.** Los casos se ejercen por la entrada pública `copyMapToEntity(Class, Map, Model, AllowProperties)` (la que ya usan los tests existentes), que es la que desemboca en la rama privada que el Paso 1 modifica.

---

## Clase: `com.educaflow.base.infrastructure.mapper.BeanMapperModel`  —  helper de infraestructura (se **modifica**)

**Responsabilidad:** copiar un mapa de valores del cliente sobre una entidad, respetando la whitelist `AllowProperties`. El Paso 1 del diseño cambia **una sola rama**: propiedad de tipo `Model` con `rawValue != null && valueDest != null`. Antes, la referencia solo se sustituía si el tipo era `MetaFile` y el id entrante difería; ahora se sustituye **siempre** que el id entrante difiera del actual (`MetaFile` deja de ser un caso especial y pasa a ser una instancia de la regla general).
**Colaboradores a mockear:** ninguno de Mockito — se usa el **doble de test ya existente** `FakeModelLoader` (implementación falsa de `ModelLoader` con `register(clase, id, modelo)` y `callCount(clase, id)`), inyectado por el constructor `new BeanMapperModel(loader)`. Sin base de datos, sin estáticos.
**Origen diseño:** Paso 1 («Infraestructura compartida: el mapeador debe sustituir la referencia cuando el cliente elige otra entidad») y la tabla de **casos mínimos (a)–(g)** de la sección `## Tests` del `design.md`, que este apartado cubre entera.
**Fixture nuevo (solo test):** `MtoHolderModel` con `@ManyToOne RefModel ref` y `@ManyToOne MetaFile fichero` (ver Decisión 3).
**Ficheros de test implicados:** los casos (a)–(g) e (i) van a `src/test/java/com/educaflow/base/infrastructure/mapper/BeanMapperModelTest.java`; el caso **(h)** va a `…/BeanMapperModelTest2.java` (reescribe un test que ya existe allí). Cada bullet indica su fichero cuando no es el primero.

### Método: `copyMapToEntity(Class<? extends Model> clazz, Map<String,Object> entityMap, Model entityDest, AllowProperties allowProperties)`

- **`copyMapToEntity_manyToOneConIdDistinto_sustituyeLaReferencia`** — Tipo: happy. Verifica: `—` (Paso 1, caso **(a)** de la tabla de casos mínimos).
  - **Arrange:** `MtoHolderModel` destino con `ref` = `RefModel(id=1, code="actual")`; `FakeModelLoader` con `RefModel(id=2, code="from-db")` registrado para `(RefModel.class, 2L)`; mapa de entrada `{"ref": {"id": 2, "code": "from-map"}}`; whitelist `createAllowProperties({"ref": {"code": true}})`.
  - **Act:** `copyMapToEntity(MtoHolderModel.class, mapa, destino, whitelist)`.
  - **Assert:** `destino.getRef()` es **la misma instancia** que la registrada en el loader (`assertSame`) y su id es `2`; el `RefModel(id=1)` anterior **conserva** `code == "actual"` (no se le copió nada); el loader se consultó una vez para `(RefModel.class, 2L)`.
- **`copyMapToEntity_manyToOneConMismoId_conservaLaReferenciaYCopiaCamposPermitidos`** — Tipo: borde. Verifica: `—` (caso **(b)**: el comportamiento actual **no** cambia).
  - **Arrange:** destino con `ref` = `RefModel(id=1, code="actual")`; `FakeModelLoader` **vacío** (para probar que no se consulta la BD); mapa `{"ref": {"id": 1, "code": "from-map"}}`; whitelist `createAllowProperties({"ref": {"code": true}})`.
  - **Act:** `copyMapToEntity(MtoHolderModel.class, mapa, destino, whitelist)`.
  - **Assert:** `destino.getRef()` sigue siendo **la misma instancia** de partida (`assertSame`); su `code` pasó a `"from-map"` y su id sigue siendo `1` (lo restaura `copyValueToEntityAndNoChangeId`); `loader.callCount(RefModel.class, 1L) == 0`.
- **`copyMapToEntity_manyToOneConIdDistintoYMapaInternoVacio_sustituyeIgualmente`** — Tipo: borde. Verifica: `—` (caso **(c)**; es el caso real de `Ciclo.grado`, `Ciclo.nivel` y de `CertificadoDigital.dispositivoCriptografico`/`alias`).
  - **Arrange:** destino con `ref` = `RefModel(id=1, code="actual")`; loader con `RefModel(id=2, code="from-db")`; mapa `{"ref": {"id": 2, "code": "from-map"}}`; whitelist **escrita a mano con mapa interno vacío**: `createAllowProperties({"ref": Map.of()})` (deny-all hacia dentro, allow para la propiedad).
  - **Act:** `copyMapToEntity(MtoHolderModel.class, mapa, destino, whitelist)`.
  - **Assert:** `destino.getRef()` es la instancia cargada (id `2`) — **la sustitución ocurre pese al mapa interno vacío**, porque quien autoriza la propiedad es la entrada **externa** de la whitelist; el `code` de la entidad cargada sigue siendo `"from-db"` (el mapa interno vacío impide copiar `"from-map"` dentro); el `RefModel(id=1)` conserva `code == "actual"`.
- **`copyMapToEntity_rawValueSinId_conservaLaReferenciaActual`** — Tipo: borde. Verifica: `—` (caso **(d)**).
  - **Arrange:** destino con `ref` = `RefModel(id=1, code="actual")`; loader vacío; mapa `{"ref": {"code": "from-map"}}` (**sin** clave `id`); whitelist `createAllowProperties({"ref": {"code": true}})`.
  - **Act:** `copyMapToEntity(MtoHolderModel.class, mapa, destino, whitelist)`.
  - **Assert:** `destino.getRef()` es la misma instancia de partida (`assertSame`); `code == "from-map"`, id `1`; el loader no se consultó nunca.
- **`copyMapToEntity_metaFileConIdDistinto_sigueSustituyendoLaReferencia`** — Tipo: happy. Verifica: `—` (caso **(e)**: no-regresión del comportamiento que hoy está escrito a mano para `MetaFile`).
  - **Arrange:** destino con `fichero` = `MetaFile` con id `10`; loader con un `MetaFile` id `11` registrado para `(MetaFile.class, 11L)`; mapa `{"fichero": {"id": 11}}`; whitelist `createAllowProperties({"fichero": Map.of()})`.
  - **Act:** `copyMapToEntity(MtoHolderModel.class, mapa, destino, whitelist)`.
  - **Assert:** `destino.getFichero()` es **la instancia cargada** (`assertSame`), con id `11`; el `MetaFile` id `10` queda intacto. (El test debe seguir pasando aunque el `if` especial de `MetaFile` desaparezca del código: esa es su razón de ser.)
- **`copyMapToEntity_valueDestNulo_cargaLaReferenciaPorIdYCopiaCamposPermitidos`** — Tipo: borde. Verifica: `—` (caso **(f)**: la rama `rawValue != null && valueDest == null` **no se toca**).
  - **Arrange:** destino con `ref` a `null`; loader con `RefModel(id=77, code="from-db")`; mapa `{"ref": {"id": 77, "code": "from-map"}}`; whitelist `createAllowProperties({"ref": {"code": true}})`.
  - **Act:** `copyMapToEntity(MtoHolderModel.class, mapa, destino, whitelist)`.
  - **Assert:** `destino.getRef()` es la instancia cargada (`assertSame`), id `77`, `code == "from-map"` (en esta rama **sí** se copian los campos permitidos sobre la entidad cargada) y el loader se consultó **una** vez. Es el mismo comportamiento que ya fija `copyMapToEntity_shouldLoadExistingModelById_whenTargetRelationIsNull` sobre el `@OneToOne`, replicado sobre el many-to-one nuevo.
- **`copyMapToEntity_manyToOneConIdDistintoYWhitelistAllowAll_sustituyeYNoMutaLaReferenciaAnterior`** — Tipo: borde. Verifica: `—` (caso **(g)**: la forma en que la mayor parte del proyecto atraviesa la rama, toda entidad sin `ModelService` propio).
  - **Arrange:** destino con `ref` = `RefModel(id=1, code="actual")`; loader con `RefModel(id=2, code="from-db")`; mapa `{"ref": {"id": 2, "code": "from-map"}}`; whitelist `AllowProperties.createAllowAllProperties()`.
  - **Act:** `copyMapToEntity(MtoHolderModel.class, mapa, destino, whitelist)`.
  - **Assert:** `destino.getRef()` es la instancia cargada (id `2`); **el `RefModel(id=1)` anterior conserva `code == "actual"`** — hoy, con el mapa interno allow-all, se le habría escrito `"from-map"` (mass-assignment de rebote, `k-secure-coding` §3); y el cargado conserva `"from-db"` porque la rama de sustitución no copia campos.
- **`copyMapToEntity_idEntranteInexistente_lanzaRuntimeException`** — Tipo: error. Verifica: `—` (borde de la rama nueva: la sustitución resuelve la entidad por id contra el `ModelLoader`).
  - **Arrange:** destino con `ref` = `RefModel(id=1, code="actual")`; loader **vacío** (no encuentra `(RefModel.class, 999L)`); mapa `{"ref": {"id": 999, "code": "x"}}`; whitelist `createAllowProperties({"ref": {"code": true}})`.
  - **Act:** `copyMapToEntity(MtoHolderModel.class, mapa, destino, whitelist)`.
  - **Assert:** lanza `RuntimeException` cuyo mensaje contiene el nombre de `MtoHolderModel` (el envoltorio por clase que ya hace el mapeador) y cuya causa encadenada menciona el id `999`; el destino no queda con una referencia a medias. Mismo patrón que el test existente `copyMapToEntity_shouldThrow_whenLoaderDoesNotFindModelForId` (puede apoyarse en `JUnitHelper.assertThrowsCause`).

- **`idDistintoEnElMapa_sustituyeLaReferenciaYLaAnteriorQuedaIntacta`** — Tipo: happy. Verifica: `—` (caso **(h)**: **reescritura** del test preexistente `PreserveIdTests.originalId_isPreservedAfterUpdate` de `BeanMapperModelTest2.java:1008-1050`, que codifica el **contrato antiguo** y con el delta queda en rojo). **Fichero: `src/test/java/com/educaflow/base/infrastructure/mapper/BeanMapperModelTest2.java`**, dentro de la clase anidada `PreserveIdTests` y **en el sitio** del test antiguo (se reescriben su `@DisplayName` —«Cuando el mapa trae un id distinto, la referencia se sustituye y la entidad anterior queda intacta»— y su nombre de método; **MUST NOT** quedarse un test con nombre `originalId_isPreservedAfterUpdate` afirmando lo contrario de lo que hace el código).
  - **Arrange:** `AddressModel` existente con id `100` y `street` «Antigua», asignada al `address` del `PersonWithAddressModel` destino; **se registra en el loader del fixture el id `999`**: `mockLoader.getModel(AddressModel.class, 999L)` → otra `AddressModel` con id `999` y `street` «De BD» (el fixture de `Test2` usa un `ModelLoader` mockeado con Mockito, no el `FakeModelLoader` del otro fichero). Mapa de entrada `{"address": {"id": 999, "street": "Actualizada"}}` y whitelist `allowWithNested("address", {"street": null, "id": null})` — se conservan **tal cual** los del test antiguo, incluido el `id` dentro del mapa interno, porque parte de lo que el caso fija es que ni con `id` permitido se edita la entidad anterior. Estáticos: se mantienen `ScalarMapper.isScalarType(AddressModel.class)` → `false` y los stubs de `BeanMapperUtil` (`isOneToOne(any(), eq("address"))` → `true`, `isManyToOne(any(), any())` → `false`) que hacen entrar la propiedad en la rama de `Model`; **se eliminan** los stubs `ScalarMapper.getScalarFromObject(...)` → «Actualizada» / `999L` y `isScalarType(String.class)`/`isScalarType(Long.class)`, que con el comportamiento nuevo **ya no se invocan** (la rama de sustitución no copia campos dentro de la entidad referenciada) y harían fallar el test por `UnnecessaryStubbingException` con los strict stubs de `MockitoExtension`.
  - **Act:** `copyMapToEntity(PersonWithAddressModel.class, mapa, destino, whitelist)`.
  - **Assert:** (1) la referencia se **sustituye**: `assertNotSame` entre la `AddressModel` de partida y `destino.getAddress()`, y `assertEquals(999L, destino.getAddress().getId())` — el id **nuevo**, no el `100` restaurado que exigía el contrato antiguo; (2) **la aserción más fuerte, la que preserva la garantía de seguridad**: la entidad **anteriormente referenciada conserva sus datos** — `assertEquals("Antigua", ...)` sobre el `street` de la instancia de partida, que **no** pasó a «Actualizada». El mapa del cliente **no edita** una entidad que solo **referencia**: es el mass-assignment de rebote que `k-secure-coding` §3 prohíbe, y es la razón por la que este caso es una mejora y no una regresión del test que sustituye. Complementa: la instancia cargada conserva su `street` «De BD» (la rama de sustitución no copia campos) y el loader se consultó una vez con `(AddressModel.class, 999L)`.
- **`copyMapToEntity_oneToOneRealConIdDistinto_sustituyeLaReferenciaYNoMutaLaAnterior`** — Tipo: happy. Verifica: `—` (caso **(i)**: gemelo del caso (a) sobre el **`@OneToOne` real** del proyecto de test, hoy **sin cobertura** de esta rama). **Fichero: `src/test/java/com/educaflow/base/infrastructure/mapper/BeanMapperModelTest.java`**, junto a los demás tests de `copyMapToEntity` de ese fichero. Razón de ser: el fixture de `Test2` **simula** el tipo de relación con `mockStatic(BeanMapperUtil)` sobre un POJO sin anotaciones, mientras que `ParentModel.ref` (`BeanMapperModelTest.java:432-433`) está anotado **`@OneToOne`** de verdad y `BeanMapperUtil` se ejerce **sin mockear**; sin este caso, la rama nueva no tendría ni un test sobre un one-to-one anotado.
  - **Arrange:** `ParentModel` destino con `ref` = `RefModel(id=1, code="actual")`; `FakeModelLoader` con `RefModel(id=2, code="from-db")` registrado para `(RefModel.class, 2L)`; mapa de entrada `mapOf("ref", mapOf("id", 2L, "code", "from-map"))`; whitelist `createAllowProperties(mapOf("ref", mapOf("code", true)))`. Sin mocks de Mockito ni estáticos: se usa el fixture propio del fichero.
  - **Act:** `copyMapToEntity(ParentModel.class, mapa, destino, whitelist)`.
  - **Assert:** `destino.getRef()` es **la instancia cargada** (`assertSame` con la registrada en el loader) y `assertNotSame` respecto al `RefModel(id=1)` de partida; su id es `2` y su `code` sigue siendo `"from-db"` (la rama de sustitución **no** copia `"from-map"`); el `RefModel(id=1)` anterior **conserva** `code == "actual"`; `loader.callCount(RefModel.class, 2L) == 1`.

> **Tests preexistentes que siguen verdes y MUST NOT tocarse:** las ramas `rawValue == null && valueDest != null` (se anula la referencia), `rawValue == null && valueDest == null`, la reconciliación de one-to-many, el filtrado por whitelist y el error del tipo de relación no soportado ya tienen test en `BeanMapperModelTest`; el Paso 1 no las toca. En concreto siguen pasando **sin tocarse**: `BeanMapperModelTest2.sourceNotNull_destNotNull_updatesInPlace` (`:961-999`, mapa **sin** `id` → sigue actualizando in-place, caso (d)), `BeanMapperModelTest2.PreserveIdTests.listItemOriginalId_isPreservedAfterUpdate` (`:1052-1100`, ítem de lista: el delta no toca la rama de listas) y sus dos correspondientes de `BeanMapperModelTest`: `copyMapToEntity_shouldMapScalarsAndReconcileOneToManyList` (`:222-269`) y `copyMapToEntity_shouldLoadExistingModelById_whenTargetRelationIsNull` (`:272-297`, rama `valueDest == null` sobre el `@OneToOne` real, caso (f)).
>
> **CORRECCIÓN — no todos los tests preexistentes del mapeador siguen verdes.** Exactamente **uno** queda en **rojo** con el delta: `BeanMapperModelTest2.PreserveIdTests.originalId_isPreservedAfterUpdate` (`src/test/java/com/educaflow/base/infrastructure/mapper/BeanMapperModelTest2.java:1008-1050`), que codifica el **contrato antiguo**. **Se actualiza** al nuevo — es el caso **(h)** de aquí arriba, y la tarea 15 es su dueña. **MUST NOT** revertirse ni acotarse el delta a many-to-one para mantenerlo verde: el alcance (one-to-one **y** many-to-one) es firme y está razonado, con la evidencia de `Property.java` y de la `Persona` compartida del expediente, en `decisiones.md` D3 y en la sección `## Tests` del `design.md`.

---

## Clase: `com.educaflow.subsystem.sistemaeducativo.service.impl.NivelServiceImpl`  —  servicio

**Responsabilidad:** validar que un nivel indica siempre el grado al que pertenece (`V-Nivel-001`) y declarar la whitelist de propiedades editables de `Nivel` en alta y modificación.
**Colaboradores a mockear:** `Repository<Nivel>` (mock, no se consulta); `I18n` estático en modo lenient devolviendo el argumento. No usa `SecurityUtil`, `Beans` ni `ModelServiceFactory`.
**Origen diseño:** Paso 3 — `validateInsert`, `validateUpdate`, el helper privado `validateGradoIndicado`, `allowPropertiesInsert`, `allowPropertiesUpdate` y el helper `allowPropertiesEditables`. Regla: `V-Nivel-001` (origen `RES-Nivel-001`).

### Método: `validateInsert(Nivel nivel)`

- **`validateInsert_nivelConGrado_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Nivel-001`.
  - **Arrange:** `Nivel` con `code`, `name` y `grado` = `new Grado()` (id `1`).
  - **Act:** `validateInsert(nivel)`.
  - **Assert:** el `Optional<BusinessMessages>` devuelto está **vacío** (`assertTrue(resultado.isEmpty())`).
- **`validateInsert_nivelSinGrado_devuelveMensajeGradoObligatorio`** — Tipo: error. Verifica: `V-Nivel-001`.
  - **Arrange:** `Nivel` con `code` y `name` rellenos y `grado` a `null`.
  - **Act:** `validateInsert(nivel)`.
  - **Assert:** el `Optional` está presente, contiene **exactamente un** `BusinessMessage`, con `getMessage()` igual al literal exacto **«El grado es obligatorio»** y `getFieldName()` igual a **`"grado"`** (anclado al campo, para que el error salga pegado a él en el formulario).

### Método: `validateUpdate(Nivel nivel, Nivel nivelOriginal)`

- **`validateUpdate_nivelConGrado_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Nivel-001`.
  - **Arrange:** `nivel` con `grado` = `Grado(id=1)`; `nivelOriginal` con `grado` = `Grado(id=1)` y otro `name`.
  - **Act:** `validateUpdate(nivel, nivelOriginal)`.
  - **Assert:** `Optional` vacío.
- **`validateUpdate_nivelSinGrado_devuelveMensajeGradoObligatorio`** — Tipo: error. Verifica: `V-Nivel-001`.
  - **Arrange:** `nivel` con `grado` a `null`; `nivelOriginal` con `grado` = `Grado(id=1)`.
  - **Act:** `validateUpdate(nivel, nivelOriginal)`.
  - **Assert:** un único `BusinessMessage` con mensaje exacto **«El grado es obligatorio»** y `fieldName` `"grado"` — la restricción vale igual en la modificación que en el alta (mismo helper).
- **`validateUpdate_cambioDeGradoAOtroGradoValido_devuelveOptionalVacio`** — Tipo: borde. Verifica: `V-Nivel-001`.
  - **Arrange:** `nivel` con `grado` = `Grado(id=2)`; `nivelOriginal` con `grado` = `Grado(id=1)` (cambio de grado de un nivel ya existente).
  - **Act:** `validateUpdate(nivel, nivelOriginal)`.
  - **Assert:** `Optional` vacío — cambiar el grado de un nivel que ya usan ciclos está **fuera de alcance** en la spec: el servicio no compara con el original ni añade ninguna otra comprobación.

### Método: `allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteCodeNameYGrado`** — Tipo: happy. Verifica: `—` (frontera de confianza, `Input AllowProperties` de `entity-Nivel.md`).
  - **Arrange:** servicio construido con `Nivel.class` y el repositorio mock.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty("code")`, `allowProperty("name")` y `allowProperty("grado")` son `true`.
- **`allowPropertiesInsert_noPermitePropiedadesFueraDeLaLista`** — Tipo: error. Verifica: `—`.
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty("archived")` y `allowProperty("createdBy")` son `false` (la whitelist es cerrada, no allow-all).
- **`allowPropertiesInsert_gradoLlevaMapaInternoVacio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesInsert().innerAllowProperties("grado")`.
  - **Assert:** el `AllowProperties` interno **no permite ninguna propiedad** del grado apuntado (`allowProperty("code")` y `allowProperty("name")` son `false`): la referencia se acepta, pero el JSON no puede modificar de rebote el grado.

### Método: `allowPropertiesUpdate()`

- **`allowPropertiesUpdate_permiteLaMismaListaQueElAlta`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty("code")`, `allowProperty("name")` y `allowProperty("grado")` son `true`; `allowProperty("archived")` es `false`; y el interno de `grado` sigue sin permitir nada (`entity-Nivel.md` declara la misma lista en Crear y Modificar, y el diseño lo resuelve con un único helper).

---

## Clase: `com.educaflow.subsystem.sistemaeducativo.service.impl.CicloServiceImpl`  —  servicio

**Responsabilidad:** validar la coherencia grado↔nivel de un ciclo (`V-Ciclo-001`, `V-Ciclo-002`, `V-Ciclo-003`) en alta y modificación, y declarar la whitelist de propiedades editables de `Ciclo`, incluido el subárbol del panel maestro-detalle de cursos.
**Colaboradores a mockear:** `Repository<Ciclo>` (mock, no se consulta: el dato «¿el grado admite nivel?» se pregunta al propio `Grado` del bean, nunca a la BD); `I18n` estático lenient. `Grado`, `Nivel` y `Ciclo` se instancian.
**Origen diseño:** Paso 4 — `validateInsert`, `validateUpdate`, el helper privado `validateCoherenciaGradoNivel`, `allowPropertiesInsert`, `allowPropertiesUpdate` y `allowPropertiesEditables`. Reglas: `V-Ciclo-001` (`RES-Ciclo-001`), `V-Ciclo-002` (`RES-Ciclo-002`), `V-Ciclo-003` (`RES-Ciclo-003`).

> **Arrange común de estos tests:** un `Grado` «admite nivel» se construye como `new Grado()` con id y con `setNiveles(List.of(nivelNoArchivado))`; un `Grado` «no admite nivel» se construye con `niveles` vacío o `null`. No se mockea `getAdmiteNivel()`: se ejerce el cálculo real (`CC-Grado-001`), que es un método de la entidad instanciada.

### Método: `validateInsert(Ciclo ciclo)`

- **`validateInsert_gradoQueAdmiteNivelConNivelDeEseGrado_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Ciclo-001`, `V-Ciclo-003`.
  - **Arrange:** `gradoCF` (id `1`) con un nivel no archivado en `niveles`; `nivelSuperior` (id `10`) con `grado` = `gradoCF`; `Ciclo` con `code`, `name`, `familiaProfesional`, `grado` = `gradoCF` y `nivel` = `nivelSuperior`.
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** `Optional` vacío.
- **`validateInsert_gradoQueAdmiteNivelSinNivel_devuelveMensajeNivelObligatorio`** — Tipo: error. Verifica: `V-Ciclo-001`.
  - **Arrange:** `gradoCF` con un nivel no archivado en `niveles`; `Ciclo` con `grado` = `gradoCF` y `nivel` a `null`.
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** un **único** `BusinessMessage` con mensaje exacto **«El nivel es obligatorio para el grado indicado»** y `fieldName` `"nivel"`.
- **`validateInsert_gradoQueAdmiteNivelConNivelDeOtroGrado_devuelveMensajeNivelDeOtroGrado`** — Tipo: error. Verifica: `V-Ciclo-003`.
  - **Arrange:** `gradoA` (id `1`) con un nivel propio no archivado; `gradoB` (id `2`); `nivelDeB` (id `20`) con `grado` = `gradoB`; `Ciclo` con `grado` = `gradoA` y `nivel` = `nivelDeB`.
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** un **único** `BusinessMessage` con mensaje exacto **«El nivel indicado no pertenece al grado del ciclo»** y `fieldName` `"nivel"`. La comparación es por **id de entidad** entre los dos `Grado`, no por código.
- **`validateInsert_gradoQueNoAdmiteNivelSinNivel_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Ciclo-002`.
  - **Arrange:** `gradoCE` (id `3`) con `niveles` vacío; `Ciclo` con `grado` = `gradoCE` y `nivel` a `null` (el curso de especialización de `ESC-002`).
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** `Optional` vacío.
- **`validateInsert_gradoQueNoAdmiteNivelConNivel_devuelveMensajeGradoSinNivel`** — Tipo: error. Verifica: `V-Ciclo-002`.
  - **Arrange:** `gradoCE` (id `3`) con `niveles` vacío; `nivelAjeno` (id `10`) con `grado` = otro grado; `Ciclo` con `grado` = `gradoCE` y `nivel` = `nivelAjeno`.
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** un **único** `BusinessMessage` con mensaje exacto **«El grado indicado no admite nivel: el nivel debe quedar vacío»** y `fieldName` `"nivel"`. **MUST NOT** aparecer además el mensaje de `V-Ciclo-003`: las dos ramas son complementarias y cada situación produce un solo mensaje.
- **`validateInsert_gradoConTodosSusNivelesArchivadosYNivelDeEseGrado_devuelveMensajeGradoSinNivel`** — Tipo: borde. Verifica: `V-Ciclo-002`.
  - **Arrange:** `gradoArchivado` (id `4`) cuyo único nivel tiene `archived = true`; `nivelArchivado` con `grado` = `gradoArchivado`; `Ciclo` con `grado` = `gradoArchivado` y `nivel` = `nivelArchivado`.
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** un **único** mensaje, el de `V-Ciclo-002` («El grado indicado no admite nivel: el nivel debe quedar vacío»), **no** el de `V-Ciclo-003` — aunque el nivel **sí** pertenece a ese grado. Es el caso que hace que la razón de no evaluar `V-Ciclo-003` en esa rama no sea la imposibilidad, sino que el nivel sobra igualmente (D7).
- **`validateInsert_cicloSinGrado_devuelveOptionalVacio`** — Tipo: borde. Verifica: `V-Ciclo-001`, `V-Ciclo-002`, `V-Ciclo-003` (rama previa: ninguna se evalúa).
  - **Arrange:** `Ciclo` con `grado` a `null` y `nivel` a `null`.
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** `Optional` vacío y **no** se lanza `NullPointerException`: sin grado no hay dominio contra el que comparar, y el invariante «el ciclo tiene grado» tiene dueño propio (`Ciclo.grado required="true"`).
- **`validateInsert_cicloSinGradoYConNivel_devuelveOptionalVacio`** — Tipo: borde. Verifica: `V-Ciclo-002` (no se evalúa sin grado).
  - **Arrange:** `Ciclo` con `grado` a `null` y `nivel` = un `Nivel` cualquiera.
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** `Optional` vacío y sin excepción.
- **`validateInsert_nivelSinGrado_devuelveMensajeNivelDeOtroGrado`** — Tipo: borde. Verifica: `V-Ciclo-003` (ver Decisión 2 de las Convenciones).
  - **Arrange:** `gradoCF` con un nivel no archivado en `niveles`; `nivelHuerfano` con `grado` a `null`; `Ciclo` con `grado` = `gradoCF` y `nivel` = `nivelHuerfano`.
  - **Act:** `validateInsert(ciclo)`.
  - **Assert:** un único mensaje **«El nivel indicado no pertenece al grado del ciclo»** con `fieldName` `"nivel"`, **sin** `NullPointerException`.

### Método: `validateUpdate(Ciclo ciclo, Ciclo cicloOriginal)`

- **`validateUpdate_gradoQueAdmiteNivelConNivelDeEseGrado_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Ciclo-001`, `V-Ciclo-003`.
  - **Arrange:** `ciclo` (id `100`) con `grado` = `gradoCF` (con nivel no archivado) y `nivel` = `nivelSuperior` (de `gradoCF`); `cicloOriginal` con los mismos grado y nivel y otro `name` (el caso de `ESC-013`).
  - **Act:** `validateUpdate(ciclo, cicloOriginal)`.
  - **Assert:** `Optional` vacío.
- **`validateUpdate_cambioAGradoQueNoAdmiteNivelConNivelHeredado_devuelveMensajeGradoSinNivel`** — Tipo: error. Verifica: `V-Ciclo-002`.
  - **Arrange:** `cicloOriginal` con `grado` = `gradoCF` y `nivel` = `nivelSuperior`; `ciclo` con `grado` = `gradoCE` (sin niveles) y `nivel` = `nivelSuperior` (el nivel que el cliente **no** vació, escenario `ESC-004` si la vista fallara).
  - **Act:** `validateUpdate(ciclo, cicloOriginal)`.
  - **Assert:** un único `BusinessMessage` con mensaje exacto **«El grado indicado no admite nivel: el nivel debe quedar vacío»** y `fieldName` `"nivel"`.
- **`validateUpdate_cambioAGradoQueNoAdmiteNivelConNivelVaciado_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-Ciclo-002`.
  - **Arrange:** `cicloOriginal` con `grado` = `gradoCF` y `nivel` = `nivelSuperior`; `ciclo` con `grado` = `gradoCE` (sin niveles) y `nivel` a `null` (lo que deja `U-ciclos-003` al cambiar el grado).
  - **Act:** `validateUpdate(ciclo, cicloOriginal)`.
  - **Assert:** `Optional` vacío — es el guardado que `ESC-004` espera que pase.
- **`validateUpdate_nivelReasignadoAOtroGrado_devuelveMensajeNivelDeOtroGrado`** — Tipo: error. Verifica: `V-Ciclo-003`.
  - **Arrange:** el caso de `ESC-012`: `gradoAvanzado` (id `1`) que conserva un nivel propio no archivado; `nivelReasignado` (id `10`) cuyo `grado` pasó a ser `gradoExperto` (id `2`); `ciclo` con `grado` = `gradoAvanzado` y `nivel` = `nivelReasignado`; `cicloOriginal` igual salvo el `name`.
  - **Act:** `validateUpdate(ciclo, cicloOriginal)`.
  - **Assert:** un único `BusinessMessage` con mensaje exacto **«El nivel indicado no pertenece al grado del ciclo»** y `fieldName` `"nivel"` — aunque lo único que el usuario cambió sea el nombre.
- **`validateUpdate_gradoQueAdmiteNivelSinNivel_devuelveMensajeNivelObligatorio`** — Tipo: error. Verifica: `V-Ciclo-001`.
  - **Arrange:** el caso de `ESC-008`: `cicloOriginal` guardado con `grado` = `gradoCP` cuando aún no tenía niveles; ahora `gradoCP` tiene un nivel no archivado; `ciclo` con ese `grado` y `nivel` a `null`.
  - **Act:** `validateUpdate(ciclo, cicloOriginal)`.
  - **Assert:** un único `BusinessMessage` con mensaje exacto **«El nivel es obligatorio para el grado indicado»** y `fieldName` `"nivel"`.
- **`validateUpdate_originalConGradoDistintoPeroCicloCoherente_devuelveOptionalVacio`** — Tipo: borde. Verifica: `V-Ciclo-001`, `V-Ciclo-002`, `V-Ciclo-003`.
  - **Arrange:** `cicloOriginal` con `grado` = `gradoCE` (sin niveles) y `nivel` a `null`; `ciclo` con `grado` = `gradoCF` (con nivel no archivado) y `nivel` = `nivelSuperior` (de `gradoCF`).
  - **Act:** `validateUpdate(ciclo, cicloOriginal)`.
  - **Assert:** `Optional` vacío: la validación **no** compara con `cicloOriginal` (la spec no declara ninguna regla de transición); decide solo sobre el bean entrante.

### Método: `allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteLosSeisCamposEditables`** — Tipo: happy. Verifica: `—` (frontera de confianza, `Input AllowProperties` de `entity-Ciclo.md`).
  - **Arrange:** servicio construido con `Ciclo.class` y el repositorio mock.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** son `true` `allowProperty` de `"code"`, `"name"`, `"familiaProfesional"`, `"grado"`, `"nivel"` y `"cursos"`.
- **`allowPropertiesInsert_noPermitePropiedadesFueraDeLaLista`** — Tipo: error. Verifica: `—`.
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty("archived")` y `allowProperty("createdBy")` son `false`.
- **`allowPropertiesInsert_referenciasConMapaInternoVacio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesInsert().innerAllowProperties("grado")`, ídem para `"nivel"` y `"familiaProfesional"`.
  - **Assert:** ninguno de los tres permite propiedad alguna (`allowProperty("code")`, `allowProperty("name")` → `false`): solo se acepta la referencia por id.
- **`allowPropertiesInsert_cursosPermiteElSubarbolDelPanelMaestroDetalle`** — Tipo: borde. Verifica: `—` (lo exige `ESC-011` y `ESC-015`: sin este subárbol el panel de cursos dejaría de guardar).
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesInsert().innerAllowProperties("cursos")` y, sobre él, `innerAllowProperties("modulos")` e `innerAllowProperties("leyEducativa")`.
  - **Assert:** el interno de `cursos` permite `"code"`, `"name"`, `"ciclo"`, `"leyEducativa"` y `"modulos"`, y **no** permite `"archived"`; el interno de `modulos` permite `"curso"` y `"modulo"` y no permite nada más (p.ej. `"archived"` → `false`); el interno de `leyEducativa` no permite ninguna propiedad.

### Método: `allowPropertiesUpdate()`

- **`allowPropertiesUpdate_permiteLaMismaListaQueElAlta`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** mismas aserciones que `allowPropertiesInsert_permiteLosSeisCamposEditables` y `allowPropertiesInsert_noPermitePropiedadesFueraDeLaLista`, más el subárbol de `cursos` con `"code"`, `"name"`, `"ciclo"`, `"leyEducativa"` y `"modulos"` permitidos (`entity-Ciclo.md` declara la misma lista en Crear y Modificar; el diseño la centraliza en un único helper).

---

## Clase: `com.educaflow.subsystem.sistemaeducativo.service.impl.GradoServiceImpl`  —  servicio

**Responsabilidad:** cerrar el allow-all de `Grado` declarando la whitelist `{code, name}` en alta y modificación. No añade ninguna validación (la spec no declara ninguna regla de `Grado` aparte de `CC-Grado-001`, que es un campo calculado del modelo).
**Colaboradores a mockear:** `Repository<Grado>` (mock, no se consulta). No usa `I18n`, `SecurityUtil` ni `Beans`.
**Origen diseño:** Paso 5 — `allowPropertiesInsert`, `allowPropertiesUpdate` y el helper `allowPropertiesEditables`.

### Método: `allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteSoloCodeYName`** — Tipo: happy. Verifica: `—` (frontera de confianza, `Input AllowProperties` de `entity-Grado.md`).
  - **Arrange:** servicio construido con `Grado.class` y el repositorio mock.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty("code")` y `allowProperty("name")` son `true`.
- **`allowPropertiesInsert_noPermiteNivelesNiAdmiteNivel`** — Tipo: error. Verifica: `—`.
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty("niveles")` es `false` (el lado inverso queda fuera: dejarlo entrar permitiría arrastrar hijos por la cascada `PERSIST`/`MERGE`), `allowProperty("admiteNivel")` es `false` (campo derivado) y `allowProperty("archived")` es `false` — es decir, **no** es allow-all, que es la razón de existir de esta clase.

### Método: `allowPropertiesUpdate()`

- **`allowPropertiesUpdate_permiteLaMismaListaQueElAlta`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ídem.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty("code")` y `allowProperty("name")` son `true`; `allowProperty("niveles")`, `allowProperty("admiteNivel")` y `allowProperty("archived")` son `false`.

---

## Clase: `com.educaflow.subsystem.sistemaeducativo.db.Grado`  —  entidad con campo calculado

**Responsabilidad:** `CC-Grado-001` — el getter `getAdmiteNivel()` recalcula en cada lectura si el grado tiene **al menos un nivel no archivado**. El cuerpo del cálculo lo escribe este diseño en `domains/Grado.xml` (`<boolean name="admiteNivel" transient="true">`), y el generador de AOP lo emite como `computeAdmiteNivel()` + getter que llama al cálculo antes de devolver. Es el único dueño de «¿este grado admite nivel?»: `CicloServiceImpl` y las vistas lo consultan, nadie replica el criterio.
**Colaboradores a mockear:** ninguno. `Grado` y `Nivel` se instancian y se rellena `niveles` con setters; sin BD, sin Mockito.
**Origen diseño:** Paso 2 (`domains/Grado.xml`), `CC-Grado-001` de la tabla de campos calculados, y la Nota 7 / `decisiones.md` D7 (niveles archivados).

### Método: `getAdmiteNivel()`

- **`getAdmiteNivel_sinNiveles_devuelveFalse`** — Tipo: borde. Verifica: `CC-Grado-001`.
  - **Arrange:** `new Grado()` con `niveles` a `null` (grado recién instanciado, como el «Certificado profesional» de `ESC-008` antes de tener niveles).
  - **Act:** `getAdmiteNivel()`.
  - **Assert:** devuelve `Boolean.FALSE` (y **no** lanza `NullPointerException`).
- **`getAdmiteNivel_listaDeNivelesVacia_devuelveFalse`** — Tipo: borde. Verifica: `CC-Grado-001`.
  - **Arrange:** `Grado` con `setNiveles(new ArrayList<>())`.
  - **Act:** `getAdmiteNivel()`.
  - **Assert:** devuelve `Boolean.FALSE` — es lo que hace que los ciclos del grado «Curso de especialización» no lleven nivel.
- **`getAdmiteNivel_conUnNivelNoArchivado_devuelveTrue`** — Tipo: happy. Verifica: `CC-Grado-001`.
  - **Arrange:** `Grado` con `niveles` = lista de un `Nivel` con `archived` a `false`.
  - **Act:** `getAdmiteNivel()`.
  - **Assert:** devuelve `Boolean.TRUE`.
- **`getAdmiteNivel_conNivelSinArchivedInformado_devuelveTrue`** — Tipo: borde. Verifica: `CC-Grado-001`.
  - **Arrange:** `Grado` con un `Nivel` cuyo `archived` es `null` (lo habitual en un registro recién creado).
  - **Act:** `getAdmiteNivel()`.
  - **Assert:** devuelve `Boolean.TRUE`: `archived` nulo cuenta como **no archivado**.
- **`getAdmiteNivel_conTodosLosNivelesArchivados_devuelveFalse`** — Tipo: borde. Verifica: `CC-Grado-001`.
  - **Arrange:** `Grado` con dos `Nivel`, ambos con `archived` a `true`.
  - **Act:** `getAdmiteNivel()`.
  - **Assert:** devuelve `Boolean.FALSE` — el ajuste de D7 sobre la letra de `CC-Grado-001`: si el selector de nivel no ofrece archivados, exigir nivel dejaría al usuario sin ninguno que elegir.
- **`getAdmiteNivel_conNivelesArchivadosYUnoActivo_devuelveTrue`** — Tipo: borde. Verifica: `CC-Grado-001`.
  - **Arrange:** `Grado` con tres `Nivel`: los dos primeros con `archived` a `true` y el tercero a `false`.
  - **Act:** `getAdmiteNivel()`.
  - **Assert:** devuelve `Boolean.TRUE` — basta con uno no archivado, y el recorrido no se detiene en el primer archivado.
- **`getAdmiteNivel_trasAnadirUnNivel_recalculaEnCadaLectura`** — Tipo: borde. Verifica: `CC-Grado-001`.
  - **Arrange:** `Grado` con `niveles` vacío; se lee `getAdmiteNivel()`; después se le asigna una lista con un `Nivel` no archivado.
  - **Act:** `getAdmiteNivel()` una segunda vez.
  - **Assert:** la primera lectura devuelve `Boolean.FALSE` y la segunda `Boolean.TRUE`: el valor no se cachea (`momento: lectura`, `sobreescribible: nunca`), que es lo que hace que un grado empiece sin exigir nivel y pase a exigirlo en cuanto se le crea uno (`ESC-008`).

---

## Clase: `com.educaflow.subsystem.sistemaeducativo.service.NivelService` — sin lógica testable
**Motivo:** interfaz sin métodos propios (`extends ModelService<Nivel>`); sin comportamiento que testear.

## Clase: `com.educaflow.subsystem.sistemaeducativo.service.CicloService` — sin lógica testable
**Motivo:** interfaz sin métodos propios (`extends ModelService<Ciclo>`); sin comportamiento que testear.

## Clase: `com.educaflow.subsystem.sistemaeducativo.service.GradoService` — sin lógica testable
**Motivo:** interfaz sin métodos propios (`extends ModelService<Grado>`); sin comportamiento que testear.

## Clase: `com.educaflow.subsystem.sistemaeducativo.db.Nivel` — sin lógica testable
**Motivo:** entidad generada por Axelor desde `domains/Nivel.xml`. Su delta es un `many-to-one grado required="true"`: getter/setter generados y una restricción **declarativa** (`@NotNull` + columna `NOT NULL`), sin cuerpo de cálculo ni lógica propia. Su equivalente en servidor, `V-Nivel-001`, sí está cubierto en `NivelServiceImpl`.

## Clase: `com.educaflow.subsystem.sistemaeducativo.db.Ciclo` — sin lógica testable
**Motivo:** entidad generada por Axelor; el diseño **no** le cambia ningún campo (`domains/Ciclo.xml` se copia verbatim) y no tiene campos calculados.

---

## Cobertura
- **Clases con lógica descritas: 5** — `com.educaflow.base.infrastructure.mapper.BeanMapperModel` (modificada), `com.educaflow.subsystem.sistemaeducativo.service.impl.NivelServiceImpl`, `…impl.CicloServiceImpl`, `…impl.GradoServiceImpl` y `com.educaflow.subsystem.sistemaeducativo.db.Grado` (campo calculado `admiteNivel`, cuerpo escrito por el diseño en `domains/Grado.xml`).
- **Clases omitidas (sin lógica):** `NivelService`, `CicloService`, `GradoService` (interfaces sin métodos propios); `com.educaflow.subsystem.sistemaeducativo.db.Nivel` y `…db.Ciclo` (entidades generadas sin lógica propia). Los XML del diseño (`domains/`, `views/`, `menus.xml`), el `data-init` y `domains/tablas.plantuml` no son clases Java y no se testean aquí.
- **Reglas server-side cubiertas (`V`/`R`/`CC`):**
  - `V-Nivel-001` → `NivelServiceImpl.validateInsert` (happy + error) y `.validateUpdate` (happy + error + borde).
  - `V-Ciclo-001` → `CicloServiceImpl.validateInsert` (happy + error) y `.validateUpdate` (error de `ESC-008`).
  - `V-Ciclo-002` → `CicloServiceImpl.validateInsert` (happy + error + borde de niveles archivados) y `.validateUpdate` (error + happy de `ESC-004`).
  - `V-Ciclo-003` → `CicloServiceImpl.validateInsert` (error + borde de nivel sin grado) y `.validateUpdate` (error de `ESC-012`).
  - `CC-Grado-001` → los siete tests de `Grado.getAdmiteNivel()`, y de rebote todos los de `CicloServiceImpl` (que ejercen el cálculo real, sin mockearlo).
  - **Reglas `R-`: ninguna.** El diseño no declara ninguna (`## Reglas de negocio` del `design.md`: los dos campos `servidor` son derivados de solo lectura).
  - Sin regla asociada (`Verifica: —`) pero exigidos por el diseño: la **frontera de confianza** (`allowProperties*` de los tres servicios) y los **nueve casos (a)–(i)** del mapeador de la sección `## Tests` del `design.md`, cubiertos uno a uno más un caso borde de id inexistente. Los casos (a)–(g) e (i) se escriben en `BeanMapperModelTest.java`; el (h) **reescribe** el test preexistente `PreserveIdTests.originalId_isPreservedAfterUpdate` de `BeanMapperModelTest2.java` (único test ya escrito que el delta deja en rojo).
- **Reglas solo-cliente excluidas (E2E en `test-e2e-desc.md`):** `U-ciclos-001`, `U-ciclos-002`, `U-ciclos-003`, `U-ciclos-004`, `U-ciclos-005`, `U-niveles-001`, `U-niveles-002`, `U-consulta-ciclo-001`, `U-familias-profesionales-001`, `U-familias-profesionales-002`, `U-familias-profesionales-003`, `U-familias-profesionales-004`, `U-familias-profesionales-005`. Todas viven en atributos de vista (`showIf`, `required`, `domain`, `onChange`/`action-record`) o en el `action-condition` local del modal, no son alcanzables con JUnit.
