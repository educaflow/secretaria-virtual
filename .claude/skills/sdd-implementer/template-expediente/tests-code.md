# Contrato de generación de código de tests

Lo leen el **descomponedor** (`README.md` §3.1) y el **implementador** (`README.md` §3.2).

---

## 1. CRITICAL — este artefacto no genera tests propios, salvo la excepción de §4

La conformidad de un tipo de expediente la dan **tests genéricos que ya existen** en `src/test/java/com/educaflow/tiposexpedientes/`, **escritos a mano**, que recorren automáticamente **todos** los tipos de expediente del árbol y comprueban que lo escrito a mano en cada tipo y en cada una de sus fases concuerda con su `TipoExpedienteInstance.xml` y con su `domains.xml`. Un tipo nuevo queda cubierto por ellos **por el mero hecho de existir**: no hay nada que registrar ni que añadir.

Por tanto:

- **MUST NOT** generarse ningún test para este artefacto, ni bajo `src/test/java/com/educaflow/tiposexpedientes/` ni en ninguna otra ubicación, **salvo la excepción de §4** (una clase auxiliar propia con lógica de negocio aislable, habilitada por `sdd-designer/template-expediente/tests-unitarios.md` §2).
- **MUST NOT** editarse, ampliarse, debilitarse, exonerarse ni excluirse ninguno de los tests existentes. Los `.java` son la **fuente de verdad** y solo se editan a mano, fuera de este pipeline. Si uno falla, **el fallo está en el trámite generado**.
- **MUST NOT** crearse un `agent_docs/*-rules.md` ni un skill generador para ellos: **no** son una proyección de markdown, a diferencia de `architecture-rules.md` y `view-rules.md`.
- **MUST NOT** regenerarse con `/developer-create-arch-tests` ni con `/developer-create-view-tests`. Las vistas de los tipos de expediente están **excluidas** de `agent_docs/view-rules.md` y de los tests de `com.educaflow.views`, porque tienen formato propio preprocesado.
- **MUST NOT** proponerse tests unitarios de los `PhaseEventManagerImpl`, `StateEventValidatorImpl` o `InitialEventManagerImpl`: sus dependencias son el `Tramitador`, la base de datos y la generación de PDF. Su verificación real es el build más el recorrido en runtime.
- **MUST NOT** crearse, por tanto, **ninguna tarea de tests** en la descomposición, ni escribirse **ningún** fichero bajo `src/test/...`, **salvo la excepción de §4**: es el **único** caso en que este artefacto crea una tarea de test y escribe bajo `src/test/java/...`.

En el caso normal el **descomponedor** no crea tareas de test y el **implementador** no materializa ninguna. No es un error ni una omisión: es lo que este artefacto prescribe. La única desviación posible es la de §4, y **MUST** cumplir sus condiciones para aplicarse.

---

## 2. Qué exigen los tests ya existentes

Esta es la lista de lo que se le va a exigir al tipo de expediente generado. El implementador la usa como criterio de «bien hecho»; el corrector-build, para entender un fallo (`build.md` §6). Los IDs son los que aparecen en el mensaje de error de cada test.

### 2.1 Estructura y ubicación

| ID | Qué exige |
|---|---|
| **H1** | Toda clase de fase (`PhaseEventManagerImpl`, `StateEventValidatorImpl`) **MUST** vivir en la subcarpeta de una fase **declarada**, con el nombre de la fase **en minúsculas**. En cualquier otra carpeta es código muerto que aparenta estar vivo |
| **T1** | El `<defaultTipoExpediente>` del `TramiteInstance.xml` **MUST** ser el **nombre de una carpeta de versión** que exista bajo el trámite (a cualquier profundidad) y contenga un `TipoExpedienteInstance.xml`, y **MUST** ser único. Omitirlo es la forma declarada de decir «aún no hay versión vigente» y **no** incumple nada |

### 2.2 Máquina de estados generada (`States`)

| ID | Qué exige |
|---|---|
| **S1** | `States` tiene **las fases del XML**, con su código y su nombre, **en orden de declaración** |
| **S2** | Cada fase de `States` tiene **los estados de esa fase** en el XML, **en orden de declaración** |
| **S3** | Cada estado lleva el `name`, el `profile`, los `events` (en su orden literal), el `initial` y el `closed` del XML. Un `state/@profile` **MUST** ser un valor del enum `Profile` |
| **S4** | El estado inicial de `States` es el **único** que el XML marca con `initial="true"` — **exactamente uno en todo el tipo**, no uno por fase |
| **S5** | Las constantes `CODE` y `NAME` de `States` son el `code` y el `name` del tipo de expediente |
| **R1** | Ninguna clase del tipo **MUST** referenciar la clase `States` de **otro** tipo de expediente. Es lo que queda al duplicar una versión y no actualizar el `import`: **compila** y revienta en runtime |

### 2.3 Modelo

| ID | Qué exige |
|---|---|
| **M1** | **MUST** existir el `domains.xml`, y la entidad del tipo es **la primera `<entity>`** del fichero en orden de documento. Las clases del tipo (`InitialEventManagerImpl`, `PhaseEventManagerImpl`) **MUST** estar parametrizadas con **esa** entidad, nunca en crudo |

### 2.4 `InitialEventManagerImpl`

| ID | Qué exige |
|---|---|
| **I1** | **MUST** existir **exactamente uno por tipo**, con FQCN `<basePackageName>.InitialEventManagerImpl`, **en la raíz de la carpeta de versión, NO en una subcarpeta de fase**, implementando `InitialEventManager<<Entidad>>` |
| **I2** | **MUST** declarar **exactamente un** `triggerInitialEvent(<Entidad>, EventContext): void`. **No** lleva anotación |

### 2.5 `PhaseEventManagerImpl`

| ID | Qué exige |
|---|---|
| **E0** | **MUST** existir la clase `<basePackageName>.<fase>.PhaseEventManagerImpl` **compilada**, extendiendo `PhaseEventManager`. Su mensaje de fallo pregunta si falta compilar o si la carpeta de la fase no se llama como la fase en minúsculas |
| **E1** | Por cada evento de la fase (**la unión sin repetir de los `events` de todos sus estados**, incluido `DELETE` si está declarado) **MUST** haber **exactamente un** `trigger<Evento>`, anotado **`@WhenEvent`**, con firma `(<Entidad>, <Entidad>, EventContext): void` — **DOS** parámetros de entidad |
| **E2** | **MUST NOT** sobrar ningún `@WhenEvent` cuyo nombre no corresponda a un evento de **la propia fase** |
| **E3** | Por cada estado de la fase **MUST** haber **exactamente un** `onEnter<Estado>`, anotado **`@OnEnterState`**, con firma `(<Entidad>, EventContext): void` — **UN** parámetro de entidad. **Incluidos los estados sin eventos y los `closed`** |
| **E4** | **MUST NOT** sobrar ningún `@OnEnterState` que no corresponda a un estado de la propia fase |
| **E5** | **MUST NOT** declararse `triggerInitialEvent` (ninguna firma) en un `PhaseEventManagerImpl` |
| **A1** | **MUST NOT** nombrarse un estado o un evento de forma que el método derivado (`onEnter<Estado>`, `trigger<Evento>`, `getForState<Estado>InEvent<Evento>`) coincida con el nombre de un método público de `PhaseEventManager`, `StateEventValidator` o `InitialEventManager`. Se compara **solo el nombre, sin firmas** |

> **CRITICAL** — el nombre del estado en `onEnter<Estado>` va **SIN la fase**: la clase ya vive en el paquete de su fase. Lo mismo en `getForState<Estado>InEvent<Evento>`.

### 2.6 `StateEventValidatorImpl`

| ID | Qué exige |
|---|---|
| **V0** | **MUST** existir `<basePackageName>.<fase>.StateEventValidatorImpl` compilada e implementar `StateEventValidator` (interfaz **marcador**, sin métodos) |
| **V1** | Por cada **pareja (estado, evento)** declarada en la fase, **salvo las del evento `DELETE`**, **MUST** haber **exactamente un** `getForState<Estado>InEvent<Evento>`, anotado **`@BeanValidationRulesForStateAndEvent`**, con **cero parámetros**, devolviendo `BeanValidationRules`. Se recorre **estado a estado**: un mismo evento en tres estados son **tres** métodos (mientras que en el manager es **un solo** trigger) |
| **V2** | **MUST NOT** sobrar ningún método anotado cuya pareja no esté declarada en la propia fase. Asimetría deliberada: `getForState<Estado>InEventDelete` **no es obligatorio pero se tolera** si su pareja está en el XML |

### 2.7 Vistas del tipo

| ID | Qué exige |
|---|---|
| **X1** | **MUST** existir un `<form state="<ESTADO>">` **sin `profile`** por **cada** estado de la fase. Es la vista de solo lectura y la red de seguridad: sin ella, navegar al estado lanza «No existe la vista en el expediente» |
| **X2** | **MUST** existir `<form state="<ESTADO>" profile="<PERFIL>">` en **todo estado que tenga `profile` y al menos un evento**. Sin ella el usuario cae en la genérica de solo lectura y **el expediente se queda atascado sin ningún error** |
| **X3** | **MUST NOT** haber dos forms de la misma fase con el mismo `(state, profile)`: producen el mismo nombre de vista y **Axelor se queda con la última** |
| **Y1** | El `name` de **todo** `<button>` del `<footer>` **MUST** ser un evento declarado en **ese** estado, o uno de los comunes (`DELETE`, `EXIT`). Un `<button name="">` es **violación** |
| **Y2** | **Todo** evento declarado en un `<state>` **MUST** tener un botón que lo dispare en **alguno** de los forms de ese estado (se mira la **unión** genérico + perfil, no form a form) |
| **Y3** | El `onClick` de **todo** botón del footer **MUST** incluir `subsysExpedientes-event-action` (una cadena `serial:` **MUST** terminar en ella) |

### 2.8 Diagrama de estados

| ID | Qué exige |
|---|---|
| **D1** | **MUST** existir un `estados.puml` en la raíz de la carpeta de versión, junto al `TipoExpedienteInstance.xml` |
| **D2** | **MUST NOT** usarse ningún alias que no sea un estado del XML. En PlantUML un identificador sin declarar **no da error: crea un nodo nuevo en silencio** |
| **D3** | **MUST** aparecer **todo** estado del XML, con el alias `<FASE>_<ESTADO>` (el identificador de PlantUML es **global**; dos estados homónimos en fases distintas se fundirían en un único nodo) |

> **Lo que D1–D3 NO comprueban**: las **transiciones**. Su destino no está en el XML, así que **nadie verifica** que un evento lleve al estado dibujado. Mantenerlo fiel a la tabla de transiciones del diseño es responsabilidad del implementador.

---

## 3. `design/test-e2e-desc.md` — se propaga VERBATIM

- El descomponedor **MUST** copiarlo **literalmente** a `{iniciativa}/implementation/test-e2e-desc.md` (`decomposition.md` §5).
- Es **contrato fijo hacia abajo**: **MUST NOT** modificarse, resumirse, renumerarse ni reescribirse.
- **NO se ejecuta aquí**: lo ejecuta `/sdd-debug-with-test-e2e-desc` contra la aplicación real, en un paso posterior. Este artefacto no arranca la aplicación.
- Si no existe, no pasa nada.

---

## 4. `design/test-unit-desc.md` — normalmente no hay nada que implementar

Ese fichero contiene, para este artefacto, una **declaración de cobertura**: qué comprueba de este tipo cada familia de tests ya existentes (§2), y la constatación explícita de que **no se añade ningún test nuevo**, con su motivo.

- Cuando declara que el tipo de expediente **no lleva tests unitarios de clases**, **no hay nada que implementar**: **MUST NOT** crearse ninguna tarea ni ningún fichero, y **no es un error ni una omisión**.
- **Única excepción a §1.** Si —excepcionalmente— describiera el test de una pieza de **lógica de negocio pura y aislable** que **no** vive en el `PhaseEventManagerImpl`, el `StateEventValidatorImpl` ni el `InitialEventManagerImpl`, entonces sí se crea una tarea de test para esa clase, ubicada en el **mismo paquete** de la clase bajo `src/test/java/...`, con `k-code-quality` entre sus skills, y se materializa delegando en `developer-code-implementer` con el texto de la tarea verbatim.
- **MUST NOT** convertirse en tests las reglas de UI: se verifican como E2E en `test-e2e-desc.md`.
