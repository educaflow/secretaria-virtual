# Parte del diseño — los tests unitarios

Define el contrato de **`design/test-unit-desc.md`**, que el motor genera **siempre** (es una fase fija suya, con su propio bucle de coherencia).

Lo escribe el rol **test-unitarios**; lo verifica el **verificador-test-unitarios** (§4) y lo corrige el **corrector-test-unitarios** (plantilla §3). Ningún otro rol lo toca, y **el diseñador MUST NOT escribirlo**.

> **REGLA DE GENERALIDAD.** Este fichero describe **el patrón**. **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, evento, campo ni clase reales; se usan los placeholders de `design-contract.md` §0.1. Los ejemplos van en bloques `> **Ejemplo** (ilustrativo, NO normativo):` con nombres inventados.

---

## 1. CRITICAL — en un tipo de expediente NO se describen tests unitarios de clases

**Esta es la decisión de contrato de esta plantilla, y el motivo MUST quedar escrito en el propio `test-unit-desc.md`.** No es un olvido ni una simplificación: es la conclusión de dos hechos del proyecto.

### 1.1 La conformidad ya la dan tests existentes, escritos a mano

`src/test/java/com/educaflow/tiposexpedientes/` contiene tests **genéricos** que recorren automáticamente **todos** los tipos de expediente del árbol y comprueban que lo escrito a mano concuerda con el `TipoExpedienteInstance.xml` y con el `domains.xml`. Cubren, entre otras, estas familias de reglas:

| Familia | Qué comprueba |
|---|---|
| **H1** | Que cada `PhaseEventManagerImpl` / `StateEventValidatorImpl` vive en la subcarpeta de una fase **declarada** |
| **D1–D3** | Que existe el `estados.puml`, que dibuja **todos** los estados con alias `<FASE>_<ESTADO>` y que no hay alias fantasma |
| **I1–I2** | Que hay **exactamente un** `InitialEventManagerImpl` por tipo, en la raíz de la versión, con **exactamente un** `triggerInitialEvent` |
| **M1** | Que la entidad es la **primera** `<entity>` del `domains.xml` y que con ella se parametrizan los managers |
| **E0–E5** | Que cada fase tiene su manager, con un `@WhenEvent trigger<Evento>` por evento y un `@OnEnterState onEnter<Estado>` por estado, ninguno de más, y sin `triggerInitialEvent` |
| **A1** | Que ningún nombre de estado ni de evento produce un método que pise la API base |
| **V0–V2** | Que cada fase tiene su validador, con un `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>` por pareja (estado, evento) salvo `DELETE`, y ninguno de más |
| **S1–S5** | Que el XML maestro está bien formado semánticamente: fases y estados en orden, `events` siempre escrito, `profile` del enum `Profile`, exactamente un `initial` |
| **R1** | Que no se referencia la clase `States` de otro tipo o de otra versión |
| **T1** | Que el `<defaultTipoExpediente>` apunta al **nombre de una carpeta** de versión que existe |
| **X1–X3** | Que hay un `<form state>` genérico por estado, un `<form state profile>` en cada estado con perfil y eventos, y ningún `(state, profile)` duplicado |
| **Y1–Y3** | Que todo botón del footer es un evento de su estado o uno común, que todo evento tiene botón, y que todo `onClick` incluye `subsysExpedientes-event-action` |

Esos tests **se escriben A MANO** y los `.java` son la **fuente de verdad**. Por tanto:

- **MUST NOT** describirse ni proponerse ningún test nuevo bajo `src/test/java/com/educaflow/tiposexpedientes/`.
- **MUST NOT** proponerse modificarlos, ampliarlos ni «adaptarlos» al trámite diseñado: son genéricos y ya lo cubren por construcción.
- **MUST NOT** proponerse crear un `agent_docs/*-rules.md` ni un skill generador para ellos, ni regenerarlos con `/developer-create-arch-tests` ni con `/developer-create-view-tests`.
- **MUST NOT** proponerse tests de vistas bajo `com.educaflow.views`: las vistas de los tipos de expediente están **excluidas** de `agent_docs/view-rules.md` y de esos tests.

### 1.2 Las clases del tipo no son unitariamente testeables con sentido

Las tres clases que el diseño especifica —`InitialEventManagerImpl`, `PhaseEventManagerImpl` y `StateEventValidatorImpl`— **no tienen lógica propia aislable**:

- No deciden nada por sí mismas: el `Tramitador` es quien resuelve la clase, comprueba el perfil, valida el evento, filtra los campos que el cliente puede dictar, invoca el `trigger*`, persiste y llama al `onEnterState`.
- Todo lo que un `trigger*` hace pasa por el `EventContext` (`updateState`, `createRegistroEntrada`, `createRegistroSalida`), por la **persistencia** (el `<Entidad>Repository`, la cascada del `HistorialEstado`), por la **generación y firma de PDF** (`getDocumentoPdf`, inyectado por el build; `AlmacenClaveResolver`, que carga claves reales) o por la clase **`States` generada**, que no existe hasta que corre `GenerateStatesTask`.
- El `StateEventValidatorImpl` no ejecuta nada: **declara** reglas. Su efecto real —rechazar el evento y construir el `AllowProperties`— lo produce el `Tramitador` al interpretarlas.

Mockear todo eso verificaría **el mock**, no el trámite: el test pasaría igual con un `updateState` a un estado equivocado si el mock lo acepta. El comportamiento real —que la transición lleva al estado correcto, que el PDF sale relleno, que el registro se crea, que la validación impide avanzar— **se verifica E2E**, y para eso está `design/test-e2e-desc.md`.

**Conclusión normativa:** el contenido de `test-unit-desc.md` en este artefacto es una **declaración explícita y breve de que no procede**, con su motivo y sus remisiones. **MUST NOT** ser una lista de tests inventados para rellenar el fichero.

---

## 2. La excepción: una clase auxiliar propia con lógica de negocio aislable

Si el diseño introduce una **clase propia del trámite que no es ninguna de las tres anteriores** y que contiene lógica de negocio **aislable**, esa clase **SÍ** se describe, con la profundidad habitual de un test unitario.

**Criterio para distinguirla.** Se describe una clase **solo si cumple las cuatro condiciones**:

1. **El diseño la define.** Aparece en la tabla «Ficheros a crear o modificar» del `design.md` y tiene su propia especificación. **MUST NOT** describirse una clase que el diseño no declara.
2. **No es** `InitialEventManagerImpl`, `PhaseEventManagerImpl` ni `StateEventValidatorImpl` (ni una subclase suya).
3. **Tiene lógica propia**: un cálculo, una transformación, una decisión con ramas, un formato — algo que se puede ejercer pasándole entradas y comprobando la salida.
4. **Es aislable**: su comportamiento se puede comprobar sin `Tramitador`, sin `EventContext`, sin base de datos, sin generación de PDF y sin la clase `States` generada. Sus colaboradores, si los tiene, son mockeables sin que el test acabe verificando el mock.

Si alguna condición falla, la clase **MUST NOT** describirse: se declara en el apartado de exclusiones con su motivo.

- ✅ CORRECTO (se describe): un helper propio del trámite que, dados unos valores de la entidad, **calcula** un dato derivado con reglas de negocio propias, sin tocar BD ni PDF.
- ❌ INCORRECTO (no se describe): un `PhaseEventManagerImpl`; un servicio que solo delega en un subsistema; una clase cuyo test exigiría mockear el `EventContext` o el `<Entidad>Repository` para que el aserto tenga sentido.

Cuando la excepción aplica, se sigue la estructura opcional de la plantilla (§3, apartado «Clases auxiliares con lógica propia»), con **solo descripción**: nombre del test, tipo, propósito, `Arrange` (entradas y qué devuelve cada mock), `Act` y `Assert` (retorno, o excepción con su mensaje exacto). **MUST NOT** escribirse código Java en ningún caso.

---

## 3. Plantilla literal de `design/test-unit-desc.md`

El rol **test-unitarios** escribe un fichero con **esta estructura exacta**, sustituyendo los `<…>` por los valores reales del diseño. Los dos últimos apartados son **opcionales** y solo aparecen si la excepción de §2 se cumple.

```markdown
# Tests unitarios — <nombre visible del trámite> (`<Entidad>`)

## No aplican tests unitarios de clases

Para este artefacto **no se describe ningún test unitario** de las clases del tipo de expediente, y **no se añade ningún test nuevo** al proyecto. No es una omisión: es una decisión de contrato, por dos motivos.

**1. La conformidad ya la cubren tests existentes, escritos a mano.** Los tests genéricos de `src/test/java/com/educaflow/tiposexpedientes/` recorren automáticamente todos los tipos de expediente del árbol, así que cubren este tipo por construcción, sin tocar nada. Para este diseño comprueban:

- que cada fase declarada tiene su `PhaseEventManagerImpl` y su `StateEventValidatorImpl` en la carpeta de la fase en minúsculas;
- que hay exactamente un `InitialEventManagerImpl` en la raíz de la versión, con un único `triggerInitialEvent`, parametrizado con la entidad del `domains.xml`;
- que en cada fase hay un `trigger<Evento>` por cada evento de la fase y un `onEnter<Estado>` por cada estado, ninguno de más;
- que en cada fase hay un `getForState<Estado>InEvent<Evento>` por cada pareja (estado, evento) salvo las de `DELETE`, ninguno de más;
- que el `TipoExpedienteInstance.xml` tiene exactamente un estado inicial, el `events` escrito en todos los estados y perfiles válidos;
- que el `estados.puml` dibuja todos los estados con el alias `<FASE>_<ESTADO>` y sin alias fantasma;
- que los `views.xml` de cada fase tienen el form genérico de cada estado, el form con perfil donde procede, sin duplicados, y que todo evento tiene su botón;
- que no se referencia la clase `States` de otro tipo ni de otra versión, y que el `<defaultTipoExpediente>` apunta a una carpeta de versión que existe.

Esos tests **se escriben a mano** y los `.java` son su fuente de verdad: este diseño **no propone crearlos, modificarlos, ampliarlos ni regenerarlos**.

**2. Las clases del tipo no son unitariamente testeables con sentido.** `InitialEventManagerImpl`, `PhaseEventManagerImpl` y `StateEventValidatorImpl` no tienen lógica propia aislable: dependen del `Tramitador`, del `EventContext`, de la persistencia, de la generación y firma de PDF y de la clase `States` generada por el build. Un test unitario tendría que mockear todo eso y acabaría verificando el mock, no el trámite.

**Dónde se verifica el comportamiento real.** End-to-end, en `test-e2e-desc.md`: cada transición de la máquina de estados, cada perfil que la dispara, cada validación que debe impedir avanzar y cada estado de solo lectura. Y en el paso final del diseño, con `./run.sh` y el recorrido en runtime de todos los estados.

## Clases del tipo — excluidas y por qué

| Clase | Motivo de la exclusión |
|---|---|
| `<basePackageName>.InitialEventManagerImpl` | Sin lógica aislable: inicializa la entidad y su efecto lo ejerce el `Tramitador`. Cubierta por los tests existentes (forma) y por `test-e2e-desc.md` (comportamiento). |
| `<basePackageName>.<fase>.PhaseEventManagerImpl` | Sin lógica aislable: depende del `Tramitador`, del `EventContext`, de la persistencia y del PDF. (una fila por fase) |
| `<basePackageName>.<fase>.StateEventValidatorImpl` | Declarativa: no ejecuta nada; sus reglas las interpreta el `Tramitador`. (una fila por fase) |

## Tests nuevos a crear

**Ninguno.** Este diseño no añade ningún fichero bajo `src/test/`.
```

Cuando —y **solo** cuando— la excepción de §2 se cumple, se añaden al final estos dos apartados:

```markdown
## Clases auxiliares con lógica propia

### Clase: `<FQN de la clase auxiliar>`

**Responsabilidad:** <qué hace, según el diseño>
**Por qué sí se testea:** <cumple las cuatro condiciones de §2: la define el diseño, no es uno de los tres managers, tiene lógica propia y es aislable>
**Colaboradores a mockear:** <lista, o «ninguno»>

#### Método: `<firma del método según el diseño>`

- **`<nombre_del_test>`** — Tipo: happy | error | borde.
  - **Arrange:** <entradas; mocks programados y qué devuelve cada uno>.
  - **Act:** <invocación>.
  - **Assert:** <retorno esperado, o excepción esperada con su mensaje exacto>.

## Cobertura

- Clases auxiliares descritas: <N>.
- Clases del tipo excluidas: <lista>.
- Tests nuevos a crear: <N ficheros bajo src/test/java/…>.
```

Reglas de forma:

- El fichero **MUST** empezar por el apartado `## No aplican tests unitarios de clases`. Es lo primero que lee el verificador.
- El apartado `## Tests nuevos a crear` **MUST** existir siempre, y decir `**Ninguno.**` cuando la excepción de §2 no aplica.
- La tabla de exclusiones **MUST** listar el `InitialEventManagerImpl` y, **por cada fase del diseño**, su `PhaseEventManagerImpl` y su `StateEventValidatorImpl`. Los nombres se toman del `design.md`.
- **MUST NOT** contener el fichero ningún bloque de código Java: ni `@Test`, ni `import`, ni cuerpo de método, ni aserción en código.
- **MUST NOT** superar lo necesario: el objetivo es una declaración **breve y explícita**, no un documento largo.

---

## 4. Comprobaciones de coherencia (verificador-test-unitarios)

Esta sección es la referencia del **verificador-test-unitarios**. La **fuente de verdad** es el diseño (`design.md`): si algo no cuadra, se corrige `test-unit-desc.md`, **nunca** el diseño.

**CRITICAL — qué NO se comprueba aquí.** En este artefacto el diseño **no** define reglas con identificadores `V-`/`R-`/`CC-`: las reglas viven en el DSL del validador y en la lista de acciones de los `trigger*`. Por tanto **MUST NOT** reportarse como incoherencia que `test-unit-desc.md` no referencie ninguna `V-`/`R-`/`CC-`, ni exigirse una «cobertura de reglas server-side». Tampoco se exige una sección por clase Java del diseño: la ausencia de tests de clases **es** el contrato.

| ID | Qué se comprueba | Qué es incoherencia | Severidad | Corrección esperada |
|---|---|---|---|---|
| **U-01** | Que `design/test-unit-desc.md` **existe** y no está vacío | No existe, o no tiene contenido | `BLOCKING` | Crearlo con la plantilla §3 |
| **U-02** | Que declara explícitamente que **no aplica** | Falta el apartado `## No aplican tests unitarios de clases`, o el fichero describe tests de clases del tipo como si procedieran | `BLOCKING` | Reescribirlo con la plantilla §3 |
| **U-03** | Que da el **motivo** | El apartado existe pero no explica los dos motivos (los tests existentes escritos a mano; las clases no aislables) | `IMPORTANT` | Añadir los dos motivos |
| **U-04** | Que **no** contiene código Java | Aparece `@Test`, un `import`, un cuerpo de método o una aserción en código | `BLOCKING` | Borrar el código; dejar solo descripción |
| **U-05** | Que **no** inventa clases | Se nombra una clase que el `design.md` **no** define (ni crea ni modifica) | `BLOCKING` | Quitarla o sustituirla por la clase real del diseño |
| **U-06** | Que **no** inventa métodos | Se nombra un método que no existe en esa clase según el `design.md` | `BLOCKING` | Reasignarlo al método real o quitarlo |
| **U-07** | Que **no** propone tocar los tests de `tiposexpedientes` | Propone crear, modificar, ampliar o regenerar algo bajo `src/test/java/com/educaflow/tiposexpedientes/`, o bajo `com.educaflow.views`, o crear un `agent_docs/*-rules.md` o un skill generador para ellos | `BLOCKING` | Quitar la propuesta y dejar la remisión a los tests existentes |
| **U-08** | Que remite a los tests existentes y a `test-e2e-desc.md` | Falta la remisión a `src/test/java/com/educaflow/tiposexpedientes/`, o la remisión a `design/test-e2e-desc.md` como sitio donde se verifica el comportamiento real | `IMPORTANT` | Añadir la remisión que falte |
| **U-09** | La tabla de exclusiones frente al diseño | Falta el `InitialEventManagerImpl`, falta el `PhaseEventManagerImpl` o el `StateEventValidatorImpl` de **alguna fase** del diseño, o se lista una fase que el diseño no declara | `IMPORTANT` | Sincronizar la tabla con las fases del `design.md` |
| **U-10** | El apartado `## Tests nuevos a crear` | No existe; o dice `Ninguno` habiendo descrito una clase auxiliar; o enumera ficheros de test sin que la excepción de §2 se cumpla | `IMPORTANT` | Ajustarlo al contenido real del fichero |
| **U-11** | La excepción de §2, si se usó | Se describe una clase auxiliar que **no** cumple las cuatro condiciones (no la define el diseño, es uno de los tres managers, no tiene lógica propia, o no es aislable sin `Tramitador`/`EventContext`/BD/PDF/`States`) | `BLOCKING` | Moverla a la tabla de exclusiones con su motivo |
| **U-12** | La forma de los tests de la excepción, si los hay | Un test sin `Arrange`, sin `Act` o sin `Assert`, o cuyo `Assert` de error no da el mensaje exacto | `IMPORTANT` | Completar el test según la plantilla §3 |
| **U-13** | La estructura general | No sigue la plantilla §3 (apartados con otros títulos, o en otro orden) | `MINOR` | Ajustar títulos y orden |

**Tarea del corrector-test-unitarios:** aplicar **en sitio** sobre `design/test-unit-desc.md` cada incoherencia reportada, ajustándose a la plantilla §3. **MUST NOT** modificar `design.md` ni ningún otro fichero del diseño. **MUST NOT** introducir clases, métodos ni tests que el diseño no defina.

---

## 5. Checklist del rol test-unitarios

Se aplica antes de responder `ESCRITO: test-unit-desc.md` (**LIMIT**: 3 pasadas de autocorrección). El objetivo es que el bucle de coherencia del motor pase **a la primera**.

- [ ] ¿El fichero existe y empieza por `## No aplican tests unitarios de clases`?
- [ ] ¿Declara explícitamente que **no se añade ningún test nuevo**, con los **dos** motivos?
- [ ] ¿Remite a los tests existentes de `src/test/java/com/educaflow/tiposexpedientes/` y a `design/test-e2e-desc.md`?
- [ ] ¿La tabla de exclusiones lista el `InitialEventManagerImpl` y, **por cada fase del diseño**, su `PhaseEventManagerImpl` y su `StateEventValidatorImpl`, con los nombres del `design.md`?
- [ ] ¿El apartado `## Tests nuevos a crear` existe y dice `**Ninguno.**` (salvo que la excepción de §2 aplique)?
- [ ] ¿**No** hay nada de código Java (ni `@Test`, ni imports, ni cuerpos, ni aserciones en código)?
- [ ] ¿**No** se nombra ninguna clase ni ningún método que el `design.md` no defina?
- [ ] ¿**No** se propone crear, modificar ni regenerar nada bajo `src/test/java/com/educaflow/tiposexpedientes/` ni `com.educaflow.views`, ni un `agent_docs/*-rules.md`, ni un skill generador?
- [ ] Si se ha descrito alguna clase auxiliar: ¿cumple **las cuatro** condiciones de §2, y cada uno de sus tests lleva `Arrange`, `Act` y `Assert` con el mensaje exacto en los de error?
- [ ] ¿Se ha mantenido breve, sin inventar contenido para rellenar?
