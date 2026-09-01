# Contrato de build — verificar y corregir la compilación

Lo leen el **verificador-build** (`README.md` §3.3) y el **corrector-build** (`README.md` §3.4). Define **cómo compilar el proyecto**, qué cuenta como éxito, cómo reportar los errores y qué se puede y qué **no** se puede tocar al corregirlos. El motor orquesta el bucle; aquí va lo específico de este artefacto.

Los placeholders son los de `README.md` §0.1.

---

## 1. Comando de compilación

El verificador-build **MUST** compilar con:

```bash
./gradlew clean build
```

Ese comando, en un tipo de expediente, hace bastante más que compilar:

- compila el Java y el Kotlin;
- **genera** la clase `States` de cada tipo desde su `TipoExpedienteInstance.xml` (`GenerateStatesTask`);
- **genera** las clases de entidad y sus `Repository` desde cada `domains.xml` (`RichDomainXmlTask` / `generateCode`);
- **preprocesa** las vistas, combinando el `views.xml` de la raíz de la versión con el de cada fase;
- **compila** los `documentospdf/*.xml` a PDF;
- **genera** los `i18n_*.csv` y el data-init de trámites y tipos;
- **ejecuta los tests**, incluidos los de `com/educaflow/tiposexpedientes` y los de `com/educaflow/views`;
- regenera `estados.png` (`GenerateDocs` va enganchada con `finalizedBy`).

Reglas:

- **MUST NOT** usarse `./gradlew run` ni `--debug-jvm`: aquí solo se compila y se testea, no se arranca la aplicación.
- **MUST NOT** lanzarse `./run.sh` en este rol: arranca el servidor y bloquea. La verificación en runtime es un paso posterior, del usuario.
- El verificador-build **ejecuta él mismo** este comando con `Bash`. El motor **NUNCA** lo ejecuta.

### 1.1 CRITICAL — nota de entorno: el *sandbox* y Gradle

La herramienta `Bash` puede correr en **sandbox**, y el sandbox **impide escribir en `.gradle/` y en `build/`**. Cuando eso pasa, Gradle falla con un error de **creación de directorio**, no con un error del proyecto. Síntomas literales:

```
Cannot create directory '…/.gradle/…'
Failed to create parent directory '…/build/…'
```

**MUST**: ante **cualquiera** de esos errores, **reintentar el mismo comando** con `dangerouslyDisableSandbox: true` **antes de reportar nada**.

- **MUST NOT** reportarse un fallo de creación de directorio como un error del build: no es un fallo del proyecto ni del código generado, y reportarlo manda al corrector a «arreglar» código que está bien.
- **MUST NOT** cambiarse el comando, ni añadirse flags de Gradle, ni tocarse ningún fichero para sortear el sandbox.
- Solo si el reintento **sin** sandbox vuelve a fallar por la misma causa, repórtalo como un bloqueo de entorno en tu respuesta (no como JSONL de errores del proyecto).

- ✅ CORRECTO: `./gradlew clean build` → `Failed to create parent directory` → relanzar el mismo comando con `dangerouslyDisableSandbox: true` → `BUILD SUCCESSFUL` → responder `OK-COMPILA`.
- ❌ INCORRECTO: emitir `{"id":"E-001","tipo":"COMPILE",…,"mensaje":"Cannot create directory '.gradle/8.5'",…}` sin haber reintentado sin sandbox.

---

## 2. Criterio de éxito

- **Éxito** = `./gradlew clean build` termina con **`BUILD SUCCESSFUL`** —lo que **incluye** que pasen los tests de `src/test/java/com/educaflow/tiposexpedientes/` y los de `src/test/java/com/educaflow/views/`, que ese mismo build ejecuta— **Y** el chequeo de conformidad de superficie (§5) no encuentra nada. → responde **exactamente** `OK-COMPILA`.
- **Fallo** = cualquier error de compilación, cualquier test que falle, **o** cualquier hallazgo de §5. → responde con el JSONL de §3.

**CRITICAL** — los tests de `com/educaflow/tiposexpedientes` son la **verificación de forma** de este artefacto: comprueban que lo escrito a mano en el tipo y en cada una de sus fases concuerda con su `TipoExpedienteInstance.xml` y con su `domains.xml`. Un fallo suyo **es** un fallo del trámite generado. **MUST NOT** darse el build por bueno si alguno falla. Qué exige cada uno está en `tests-code.md`.

---

## 3. Formato de reporte de errores (verificador-build)

Si el build falla, responde **únicamente** con líneas **JSONL**: **un error por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:

- `id` — correlativo `E-NNN` (`E-001`, `E-002`, …).
- `tipo` — `COMPILE` (error del compilador o de una tarea de generación del build) | `TEST` (test que falla) | `CONFORMANCE` (superficie o inventario no declarados, §5).
- `fichero` — ruta del fichero afectado, o `null`.
- `ubicacion` — línea / método / nombre del test; `null` si no aplica.
- `tarea` — la `task_NN.md` de `implementation/` de la que probablemente proviene (le dice al corrector qué skills aplican), o `null`.
- `mensaje` — el mensaje **literal** del compilador, de la tarea Gradle o del test.
- `correccion` — qué cambiar para resolverlo.

Cada línea **MUST** ser JSON válido en una sola línea (escapa los saltos como `\n`). **MUST NOT** añadirse comentarios ni texto fuera del JSONL.

> **Ejemplo** (ilustrativo, NO normativo), con nombres inventados:
>
> ```jsonl
> {"id":"E-001","tipo":"TEST","fichero":"src/main/java/com/educaflow/tramites/solicitud_de_ejemplo/v1/revision/PhaseEventManagerImpl.java","ubicacion":"PhaseEventManagerTest [E3]","tarea":"task_07.md","mensaje":"[E3] Falta el método onEnterPendiente anotado con @OnEnterState","correccion":"Añadir onEnterPendiente(SolicitudDeEjemploV1, EventContext) anotado @OnEnterState, con cuerpo vacío, según la lista de onEnter de design.md seccion 9."}
> ```

- ❌ INCORRECTO: `BUILD FAILED, hay 3 errores` (prosa, no JSONL); responder `OK-COMPILA` cuando un test de `tiposexpedientes` falla; emitir una línea por un error de creación de directorio del sandbox (§1.1).

---

## 4. Qué puede y qué NO puede tocar el corrector-build

El corrector-build resuelve cada línea JSONL. Reglas duras:

- **MUST** corregir el **código Java y Kotlin** del trámite (`InitialEventManagerImpl.java`, `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt`). Si el contrato de dominio lo aconseja, delega en `developer-code-implementer` cargando antes los skills de la tarea de origen (`tarea`).
- **CRITICAL — los XML del diseño ya colocados son contrato fijo.** `TramiteInstance.xml`, `TipoExpedienteInstance.xml`, `domains.xml`, los `views.xml` (raíz y fases), los `documentospdf/*.xml` y `estados.puml` son copias verbatim de `design/`. **MUST NOT** editarse para que cuadre el Java: se corrige el Java para que cuadre con ellos. Si un error apunta a que un XML del diseño está mal, responde en la **primera línea** `DESIGN-ERROR: {motivo detallado}` y termina.
- **CRITICAL — MUST NOT editarse ni «arreglarse» los tests de `src/test/java/com/educaflow/tiposexpedientes/` ni los de `src/test/java/com/educaflow/views/`.** Se escriben **a mano** y son la **fuente de verdad** de la conformidad; no son una proyección de ningún markdown y no se regeneran con ningún skill. Si uno falla, **el fallo está en el trámite generado**, nunca en el test. Debilitar, exonerar o excluir un test para que el build pase está **prohibido**.
- **MUST NOT** crearse `i18n_es.csv` ni `i18n_ca.csv` para «arreglar» nada: los genera el build y escribirlos a mano es un fallo bloqueante.
- **MUST NOT** editarse `States.java`, `estados.png`, el `<extra-code-model>` de un `domains.xml` ni ningún fichero bajo `build/`: son generados y se reescriben en cada compilación.
- **MUST NOT** legitimarse superficie no diseñada: ante un método, clase o acción que compila pero que **ninguna** tarea ni sección del `design.md` declara, la corrección es **eliminarlo**, no ampliar la especificación ni la interfaz para que encaje. Antes de eliminar nada, **MUST** comprobarse con `git diff` / `git log` si preexistía a la iniciativa: si preexistía, **MUST NOT** eliminarse — detente y repórtalo.
- **MUST NOT** usarse `AskUserQuestion`: ante un bloqueo del entorno, descríbelo en tu respuesta y termina.

Tras corregir, el motor relanza el verificador-build. Si los mismos errores se repiten entre iteraciones, el motor para y pregunta al usuario.

---

## 5. Chequeo de conformidad (verificador-build)

Compilar y pasar los tests **no** detecta que se haya escrito **de más**: un método extra, un panel extra o un fichero que ninguna tarea pidió compilan sin problema, y el bucle de build tiende a **legitimar** el invento. Por eso, **solo cuando el build termina en `BUILD SUCCESSFUL`**, el verificador-build **MUST** ejecutar este chequeo **antes** de responder `OK-COMPILA`.

### 5.1 Conformidad de superficie de las clases

1. Reúne la **superficie declarada**: la unión de lo que declaran las secciones §8, §9 y §10 del `design.md` (copiadas verbatim en las `implementation/task_NN.md`) — la lista de `trigger<Evento>`, la lista de `onEnter<Estado>` y las tablas de cobertura de cada fase, y el único `triggerInitialEvent`.
2. Reúne la **superficie real** de las clases del trámite generado: los métodos anotados con `@WhenEvent`, `@OnEnterState` y `@BeanValidationRulesForStateAndEvent` de cada clase, más los métodos y clases públicos añadidos.
3. Compara y **MUST** reportar como `CONFORMANCE`:
   - un `@WhenEvent`, `@OnEnterState` o `@BeanValidationRulesForStateAndEvent` que **sobra** (su evento, estado o pareja no está declarado en el diseño para **esa** fase);
   - uno que **falta** (el diseño lo declara y la clase no lo tiene);
   - un método de la especificación **renombrado** o con **firma distinta** de la declarada;
   - cualquier clase, método público, campo o constante **añadido** que ninguna sección del `design.md` liste.
4. Los **métodos preexistentes a la iniciativa** (los que ya estaban en la versión base del fichero, no añadidos por su `git diff`) **MUST NOT** reportarse: no son un invento.

### 5.2 Conformidad de inventario de ficheros

Compara el conjunto de ficheros **reales** de la carpeta del trámite con la **tabla de ficheros** de la sección `## 6` del `design.md` (que las tareas traen copiada). **MUST** reportar como `CONFORMANCE` cualquier fichero **de más** y cualquiera **de menos**.

**Exclusiones** (no se comparan en ninguna dirección):

| Excluido | Motivo |
|---|---|
| `**/estados.png` | Lo genera `GenerateDocs`. Ni su presencia ni su ausencia son hallazgo. El `.puml` **sí** se compara |
| `documentospdf/originales/**` | Material de entrada aportado a mano, no producto de la cadena |
| `build/**` y todo lo generado (`States.java`, data-init de trámites/tipos, vistas preprocesadas, PDFs compilados) | No están en `src/` |

**CRITICAL — excepción de los `i18n_*.csv`.** Están excluidos de la comparación por nombre, **pero** si aparece un `i18n_es.csv` o un `i18n_ca.csv` **escrito a mano** dentro de la carpeta del trámite generado (es decir, presente en el `git diff` de esta iniciativa bajo `src/main/java/com/educaflow/tramites/…`), **MUST** reportarse como `CONFORMANCE` **bloqueante**, con la corrección de **borrarlo**: crearlos a mano está prohibido.

### 5.3 Formato del hallazgo

Cada hallazgo es una línea JSONL de §3 con `tipo: CONFORMANCE`, `tarea` = la tarea más cercana (o `null`) y `correccion` apuntando a eliminar lo que sobra o a añadir lo que falta según el diseño; si de verdad falta en el diseño, a volver a `/sdd-designer`.

> **Ejemplo** (ilustrativo, NO normativo), con nombres inventados:
>
> ```jsonl
> {"id":"E-004","tipo":"CONFORMANCE","fichero":"src/main/java/com/educaflow/tramites/solicitud_de_ejemplo/v1/revision/PhaseEventManagerImpl.java","ubicacion":"triggerArchivar","tarea":"task_07.md","mensaje":"Metodo @WhenEvent triggerArchivar cuyo evento ARCHIVAR no esta declarado en ningun <state> de la fase REVISION ni en design.md seccion 9.","correccion":"Eliminar triggerArchivar: es superficie inventada (build.md seccion 4)."}
> ```

Si §5 no encuentra nada **y** el build pasó → responde **exactamente** `OK-COMPILA`.

---

## 6. Catálogo de errores típicos de este artefacto

Los fallos más frecuentes al generar un tipo de expediente, con su corrección. La columna «lo caza» indica qué lo detecta.

| Síntoma | Causa | Lo caza | Corrección |
|---|---|---|---|
| `No existe el panel con nombre: <panel>` | Un `<include-panels>` de un `views.xml` de fase referencia un panel que el form plantilla de la raíz no declara | El build (falla al preprocesar) | **DESIGN-ERROR**: los dos `views.xml` son contrato fijo del diseño y son incoherentes entre sí. **MUST NOT** añadirse el panel a mano |
| `The content of element 'object-views' is not complete` al arrancar, y la app queda **sin vistas, sin menús y sin data-init** | Un `views.xml` de fase quedó **vacío** (sin ningún hijo; los comentarios XML no cuentan). El XSD lo valida el `ViewLoader` **al arrancar**, no en build | Nadie en build | Una fase sin forms de estado **MUST** omitir el fichero entero, no dejar un `<object-views>` vacío. Si el diseño lo trae vacío → **DESIGN-ERROR** |
| `EXIT` aparece en el atributo `events` de un `<state>` | `ExpedienteController` intercepta `EXIT` **antes** del `Tramitador`: nunca llega al manager ni al validador, así que declararlo genera código muerto y un `trigger`/validador que nadie llama | Y1/Y2 indirectamente; conformidad §5.1 | **DESIGN-ERROR**: quitarlo del `events` es cambiar el `TipoExpedienteInstance.xml`, que es contrato fijo. `EXIT` es un **botón puro de UI**, solo va en el `<footer>` |
| `<button name="">` en un `views.xml` de fase | Cáscara de `CreateFilesTask` que no se sobrescribió con el XML del diseño | **Y1** | Recopiar el `views.xml` del diseño encima (`implementation.md` §2.1) |
| `IllegalArgumentException` en runtime al transicionar; en build, referencia a `States` de otro paquete | El `import` de `States` apunta a **otro tipo o versión** (típico al duplicar una versión y no actualizarlo). **Compila** y revienta en runtime | **R1** | Cambiar el `import` al `States` de la **propia** versión (`<basePackageName>.States`) |
| Enum del `domains.xml` que colisiona con el de otro tipo | Todos los tipos comparten el paquete `com.educaflow.subsystem.expedientes.db`, así que un enum sin el sufijo `<Entidad>` choca con el homónimo de otro tipo | El compilador | **DESIGN-ERROR**: el `domains.xml` es contrato fijo; el sufijo lo decide el diseño |
| Falta `getDocumentoPdf` / falta el enum `TipoDocumentoPdf` en la entidad | El `domains.xml` usa `<extra-code>` en vez de `<extra-code-model>`: el primero inyecta en el `Repository`, el segundo en la **entidad** | El compilador, al usarlo desde un `trigger*` | **DESIGN-ERROR**: es el `domains.xml` del diseño |
| El PDF sale con campos vacíos, sin ningún error | Los `colspan` de una `<fila>` no suman 12 (o un múltiplo), o una expresión Groovy `self.*` apunta a un campo inexistente. **Los fallos de evaluación son SILENCIOSOS**: log + campo vacío | **Nadie** en build | Se detecta solo revisando el PDF en runtime. Si el `documentospdf/*.xml` del diseño está mal → **DESIGN-ERROR** |
| Un texto sale solo en castellano, o mal traducido | `<valenciano>` puesto **vacío** (→ solo castellano) frente a **omitido** (→ lo traduce el build). Ponerlos como **atributos** en vez de elementos hijos también los pierde | **Nadie** en build | Es contenido del `documentospdf/*.xml` del diseño → **DESIGN-ERROR** si está mal |
| `No se ha encontrado el método: getForState<Estado>InEvent<Evento>` en runtime | Falta un método del validador: se recorre **estado a estado**, así que un mismo evento en tres estados son **tres** métodos (mientras que en el manager es **un solo** trigger) | **V1** | Añadir el método que falta según la tabla de cobertura de `design.md` §10 |
| `onEnter<Estado>` o `trigger<Evento>` en la clase de **otra** fase | El trigger de un evento va en el manager de la fase del estado **desde el que se dispara**; el `onEnter` va en el manager de la fase del estado **al que se llega** | **E2** / **E4** | Mover el método a la clase de la fase que le corresponde |
| `triggerInitialEvent` declarado en un `PhaseEventManagerImpl` | El evento inicial es **del tipo**, no de una fase: allí no se llamaría nunca | **E5** | Eliminarlo; el único va en `InitialEventManagerImpl`, en la **raíz** de la versión |
| El botón de un evento no aparece, o el expediente se atasca sin error | Falta el `<form state profile>` de un estado con `profile` y eventos, o falta el botón de un evento declarado | **X2** / **Y2** | El `views.xml` de fase es contrato fijo → **DESIGN-ERROR** |
| Dos forms con el mismo `(state, profile)` | Producen el mismo nombre de vista y Axelor se queda con **la última**; las demás no se pintan nunca | **X3** | **DESIGN-ERROR** |
| Un botón del footer no dispara nada | Su `onClick` no incluye `subsysExpedientes-event-action` (una cadena `serial:` **MUST** terminar en ella) | **Y3** | **DESIGN-ERROR** |
| El trámite revienta al abrirlo con «No existe el tipo de expediente para el tramite» | `<defaultTipoExpediente>` no apunta al **nombre de la carpeta** de versión; el data-init deja la columna a `null` y **nadie avisa en build** | **T1** | **DESIGN-ERROR**: es el `TramiteInstance.xml` |
| `RuntimeException` al firmar en cliente | `dniFirmaDocumentoEntrada` sin rellenar en el `triggerInitialEvent`. **Nada lo verifica en build** | **Nadie** | Si `design.md` §8 lo declara y falta en el código → corregir el Java; si §8 no lo declara y el tipo firma en cliente → **DESIGN-ERROR** |
| **NPE** al crear el registro de entrada | `personaSolicitante` o `personaInteresada` a `null`. **Nada lo verifica en build** | **Nadie** | Igual que la fila anterior |
| Falta el `estados.png`, o está desincronizado con el `.puml` | `GenerateDocs` es **incremental por fecha**: omite el PNG más reciente que su fuente | Nadie | Relanzar `./gradlew -q GenerateDocs`. **MUST NOT** editarse el PNG |
| Aparecen `i18n_es.csv` / `i18n_ca.csv` en la carpeta del trámite | Alguien los escribió a mano | Conformidad §5.2 | **Borrarlos**. Los genera el build |
