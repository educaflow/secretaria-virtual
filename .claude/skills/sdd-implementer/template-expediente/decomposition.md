# Contrato de descomposición — del `design.md` a las tareas

Lo lee el **descomponedor** (`README.md` §3.1). Define **cómo convertir el diseño de un tipo de expediente en una lista ordenada de tareas atómicas** escritas en `{iniciativa}/implementation/`, sin implementar nada todavía: solo escribir los ficheros de tarea, su índice y la copia de `test-e2e-desc.md`.

> Este artefacto **no genera tests propios** salvo la excepción que contempla `tests-code.md` §4: lee `tests-code.md` antes de terminar para saber si esa excepción aplica y qué se propaga.

Los placeholders (`<tramite>`, `<vN>`, `<Entidad>`, `<FASE>`, `<fase>`, `<doc>`, `<carpeta de versión>`…) son los de `README.md` §0.1.

---

## 1. Leer el `design.md` íntegro

1. Lee **todo** el `design.md`, de la sección 1 a la 15. **MUST NOT** leer solo la tabla de ficheros: las secciones §8, §9 y §10 son la **especificación quirúrgica** del código y sin ellas las tareas de `.java`/`.kt` quedan vacías de contenido.
2. Localiza la tabla de la sección **`## 6. Ficheros a crear o modificar`**. Cada fila tiene la forma:

   | Fichero | Acción | Skill | Descripción |
   |---------|--------|-------|-------------|

   Las rutas son **relativas a la raíz del proyecto**. Si esa tabla no existe, o no lista los ficheros obligatorios del inventario (§2.1) —salvo en una **iniciativa de modificación**, donde el inventario no aplica y la tabla lista solo los ficheros tocados (ver Reglas de instanciación)—, **MUST NOT** inventarla: indica el problema en tu respuesta (el motor lo trata como STOP).
3. Lee también los ficheros materializados de `design/` **solo para saber cuáles existen** (cuántas fases hay en `design/fases/`, cuántos documentos en `design/documentospdf/`). **MUST NOT** volcar su contenido dentro de las tareas: los XML se copian, no se transcriben.
4. Deriva de la sección `## 2. Identidad del trámite y del tipo` los valores concretos de `<tramite>`, `<vN>`, `<carpeta de versión>`, `<Entidad>` y `<basePackageName>`, y úsalos **resueltos** en las rutas de las tareas.

---

## 2. Orden obligatorio de las tareas

**CRITICAL — este orden es normativo y MUST NOT alterarse.** No es una preferencia de estilo: cada bloque **lee** lo que produjo el anterior. En particular, `CreateFilesTask` **lee** el `TipoExpedienteInstance.xml` para saber qué fases existen, y los `views.xml` de fase solo pueden incluir paneles que ya existan en el form plantilla de la raíz.

**Este orden es el de las TAREAS DE IMPLEMENTACIÓN, no el de los pasos del diseño.**
`design-contract.md` §9 numera los `### Paso N` en los que el diseño **se describe**; esta tabla numera las tareas en las que el código **se escribe**.
Son dos ordenaciones **independientes** y **MUST NOT** confundirse: no tienen por qué coincidir, y no coinciden.
El caso visible es `estados.puml`, que en el diseño es el paso 10 y aquí va en la tarea 2, junto al `TipoExpedienteInstance.xml` del que es proyección: se copia con él porque describe la misma máquina de estados y ningún otro fichero depende de él.
Por eso una tarea localiza el `### Paso N` del diseño **por su fichero**, nunca por su número (§4.1): el `N` del diseño y el `NN` de la tarea son numeraciones distintas.

| # | Tarea | Ficheros que cubre | Por qué va aquí |
|---|---|---|---|
| 1 | **`TramiteInstance.xml`** | `src/main/java/com/educaflow/tramites/<tramite>/TramiteInstance.xml` | Raíz del árbol: nada depende de nada |
| 2 | **`TipoExpedienteInstance.xml` + `estados.puml`** | `<carpeta de versión>/TipoExpedienteInstance.xml`, `<carpeta de versión>/estados.puml` | El XML maestro es el **input** de `CreateFilesTask` y de `GenerateStatesTask`; el `.puml` es su proyección y se escribe con él |
| 3 | **Ejecutar `CreateFilesTask`** | (no escribe ficheros a mano: los **genera** la tarea Gradle) | Genera los esqueletos a partir del XML maestro **completo** |
| 4 | **`domains.xml`** | `<carpeta de versión>/domains.xml` | Define la entidad, de la que dependen todas las clases y las vistas |
| 5 | **`views.xml` de la raíz de la versión** | `<carpeta de versión>/views.xml` | El **almacén de paneles**: los `views.xml` de fase solo pueden incluir paneles que existan aquí |
| 6 | **`InitialEventManagerImpl.java`** | `<carpeta de versión>/InitialEventManagerImpl.java` | Necesita la entidad (paso 4); su parámetro de tipo es el **único** sitio donde el tipo la declara |
| 7 | **Una tarea POR FASE** | `<carpeta de versión>/<fase>/PhaseEventManagerImpl.java`, `<carpeta de versión>/<fase>/StateEventValidatorImpl.kt`, `<carpeta de versión>/<fase>/views.xml` | Necesitan la entidad y los paneles |
| 8 | **`documentospdf/`** | `<carpeta de versión>/documentospdf/<doc>.xml` y `<carpeta de versión>/documentospdf/_<fragmento>.xml` | Sus rutas `self.*` son campos de la entidad |
| 9 | **Asignaciones de perfil** | `src/main/resources/data-demo/input/permisos-demo.xml` (**Modificar**) | Referencian el `<Code>` del trámite y el `<Entidad>` del tipo |

Reglas de instanciación:

- El bloque **7 se instancia una vez por CADA fase** declarada en el `TipoExpedienteInstance.xml`, en el **orden en que las fases se declaran**. Con `F` fases hay `F` tareas de fase. **MUST NOT** fijarse un número a priori ni agrupar dos fases en una tarea. Cada tarea de fase cubre **los ficheros que §6 declare para esa fase**, ni uno más: en una fase completa, `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt` y `views.xml`; en una **iniciativa de MODIFICACIÓN**, solo los que el delta toque.
  El `views.xml` de una fase **MUST NOT** faltar en el diseño ni llegar vacío: si falta el de una fase declarada, o su `<object-views>` no tiene ningún hijo, es un **DESIGN-ERROR** (tumba el arranque — `build.md` §6).
- El bloque **8 es una sola tarea** que cubre **todos** los documentos y fragmentos. Si el tipo no genera ningún PDF, **la tarea no existe** (sin error).
- Los bloques **2, 3, 4, 5 y 6 son exactamente una tarea cada uno**: existen siempre.
- Los bloques **1 y 9 son condicionales**, con el mismo criterio que el 8: existen **si y solo si** la tabla §6 del `design.md` lista su fichero, y entonces son **exactamente una tarea cada uno**. El `TramiteInstance.xml` (bloque 1) solo aparece cuando el trámite es **nuevo**; una versión posterior de un trámite ya dado de alta no lo trae. Las asignaciones de perfil (bloque 9) solo aparecen cuando el diseño declara alguna que **no** esté ya concedida por una asignación por `tramiteCode` preexistente. **MUST NOT** fabricarse una tarea vacía para un bloque cuyo fichero §6 no liste, ni omitirse la tarea de un bloque cuyo fichero §6 sí liste.
- **Tarea de test — excepcional.** No hay ningún bloque de tests en esta tabla, y **MUST NOT** fabricarse uno, salvo en el único caso que contempla `tests-code.md` §4: que `design/test-unit-desc.md` describa una **clase auxiliar propia con lógica de negocio aislable**.
  Solo entonces se crea **una** tarea de test para esa clase, **al final de todo**, después del bloque 9, numerada correlativamente como una tarea más.
- Numera `01`, `02`, … de forma correlativa siguiendo esta tabla. El número final depende de `F` y de si hay documentos.
- **Iniciativa de MODIFICACIÓN de una versión existente** (el `design.md` lo declara con la fila «Modificación de» de su sección «Identidad del trámite y del tipo», y su tabla §6 lista solo los ficheros tocados por el delta): **TODOS los bloques pasan a ser condicionales** con el criterio de los bloques 1 y 9 — la tarea de un bloque existe **si y solo si** la tabla §6 lista alguno de sus ficheros —, manteniendo el orden relativo de la tabla. El bloque 3 (`CreateFilesTask`) existe **solo si** el delta añade fases nuevas (la tarea es idempotente: generará únicamente los esqueletos de las fases nuevas y dejará intacto todo lo demás).

> **Ejemplo** (ilustrativo, NO normativo): un tipo de un **trámite nuevo** con **3 fases**, documentos PDF y asignaciones de perfil propias genera 11 tareas (1 trámite + 1 maestro/puml + 1 CreateFilesTask + 1 domains + 1 views raíz + 1 initial + 3 de fase + 1 documentospdf + 1 permisos). Un tipo con **1 fase** y sin PDF genera 8.

### 2.1 La tarea 3 es una tarea PROPIA — `CreateFilesTask`

**CRITICAL — `CreateFilesTask` MUST ser una tarea por sí misma**, en la posición 3. **MUST NOT** esconderse como un paso dentro de la tarea del `TipoExpedienteInstance.xml` ni dentro de la del `domains.xml`: es el punto exacto en el que el árbol de ficheros del tipo pasa a existir, y su ejecución tiene que ser observable como un `DONE` propio.

El `<texto del prompt>` de esa tarea **MUST** declarar, además del texto verbatim del paso correspondiente del `design.md`:

- El comando exacto, con la ruta resuelta:

  ```
  ./gradlew -q CreateFilesTask -Ptipo=<carpeta de versión>
  ```

- Que **MUST** ejecutarse **después** de que el `TipoExpedienteInstance.xml` exista y esté **completo** (todas las fases, todos los estados, todos los `events`), porque la tarea lo **lee** para saber qué subcarpetas de fase crear.
- Que **MUST** ejecutarse **antes** de rellenar nada: los pasos 4 en adelante escriben **sobre** los esqueletos que esta tarea deja.
- Qué crea exactamente: en la raíz de la versión `domains.xml`, `views.xml` e `InitialEventManagerImpl.java`; en **cada** subcarpeta `<fase>/`, `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt` y `views.xml`.
- Que es **idempotente** y **nunca pisa lo ya escrito**: imprime una línea `CREADO <ruta>` por fichero **creado** y **nada** por los que ya existían.
- Que la **verificación** es que aparezca una línea `CREADO` por cada uno de esos ficheros (o que el fichero ya exista), y que la tarea falla con mensaje explícito si la ruta no corresponde a ningún tipo de expediente.
- **MUST NOT** usarse `-Pfase`: acota a una sola fase y entonces **no** genera los ficheros de la raíz de la versión. Si alguna vez se usara, **MUST** ir siempre junto con `-Ptipo`.
- **MUST NOT** engancharse al build: compilar no debe escribir en `src/main/java`.

---

## 3. Determinar los skills de cada tarea

Los skills salen de la columna `Skill` de la tabla `## 6` del `design.md` para los ficheros de esa tarea, **normalizados al nombre real del skill** (ignora las anotaciones entre paréntesis tipo `(modelo.md)`, pero **conserva** la referencia al fichero concreto dentro del texto de la tarea, porque le dice al implementador qué leer).

Mínimos obligatorios por tipo de tarea (añádelos aunque la tabla no los liste):

| Tarea | Skills **MUST** |
|---|---|
| 1 — `TramiteInstance.xml` | `k-tramite` |
| 2 — `TipoExpedienteInstance.xml` + `estados.puml` | `k-tipo-expediente` |
| 3 — `CreateFilesTask` | `k-tipo-expediente` |
| 4 — `domains.xml` | `k-tipo-expediente`, `k-validaciones`, `k-secure-coding` |
| 5 — `views.xml` de la raíz | `k-tipo-expediente` |
| 6 — `InitialEventManagerImpl.java` | `k-tipo-expediente`, `k-secure-coding`, `k-code-quality` |
| 7 — cada fase | `k-tipo-expediente`, `k-validaciones`, `k-secure-coding`, `k-code-quality` |
| 8 — `documentospdf/` | `k-tipo-expediente` |
| 9 — `permisos-demo.xml` | `k-datainit` |

- **CRITICAL** — `k-secure-coding` va en **toda** tarea que toque la entidad, un `trigger*` o el validador: el `StateEventValidatorImpl` no es solo un validador, es la **lista de campos que el cliente puede dictar** en ese evento.
- Añade `k-i18n` a cualquier tarea cuyo texto del diseño incluya mensajes, títulos o textos visibles al usuario.
- Añade `k-tipo-expediente` (`versionado.md`) a las tareas de una versión `<vN>` con `N > 1`.
- **MUST NOT** añadirse `k-vistas` a las tareas de vistas de un tipo de expediente: sus vistas son **preprocesadas** y están **excluidas** de `k-vistas` y de `agent_docs/view-rules.md`. La referencia correcta es `k-tipo-expediente` (`vistas.md`).

---

## 4. Escribir cada `task_NN.md`

Por cada tarea, escribe `{iniciativa}/implementation/task_NN.md` con **exactamente** esta plantilla:

```
---
type: implementation-task
template: <valor copiado del design.md>
---

# Tarea NN a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- <skill A>
- <skill B>

<texto del prompt>
```

- `NN` es el número de **dos dígitos** (`01`, `02`, …).
- `template:` se **copia verbatim** del frontmatter del `design.md` de entrada (que la heredó de la spec), incluido el valor `external`.
  Aquí es **solo trazabilidad**: los skills que van detrás (`/sdd-debug-with-test-e2e-desc`, `/sdd-create-tests-e2e`) resuelven su plantilla leyendo la clave del `design/design.md`, nunca la de una tarea. Aun así **MUST NOT** escribirse de memoria el nombre de esta carpeta de plantillas — con un `--template-dir` externo sería un dato falso, y una tarea que se contradiga con el `design.md` despista a quien la audite.
  - ✅ CORRECTO: el `design.md` dice `template: expediente` → la tarea dice `template: expediente`.
  - ❌ INCORRECTO: el `design.md` dice `template: external` y la tarea dice `template: expediente` (inventa una plantilla interna que no se usó).
- La lista de skills es la determinada en §3.
- ✅ CORRECTO: `# Tarea 03 a implementar` con `type: implementation-task` a ras de margen.
- ❌ INCORRECTO: `# Tarea 3` (sin dos dígitos), o frontmatter `type: design`.

### 4.1 Qué texto del `design.md` va, VERBATIM, en cada tarea

**CRITICAL — el código Java y Kotlin NO está materializado en `design/`: solo existe como especificación quirúrgica en las secciones 8, 9 y 10 del `design.md`.** Una tarea de `.java` o `.kt` que no lleve esa especificación **copiada verbatim dentro de sí** es una tarea vacía: el implementador no tiene de dónde sacar los métodos, ni el orden de las acciones, ni los argumentos de las reglas. Este reparto es, por tanto, la parte más importante de la descomposición.

| Tarea | Secciones del `design.md` que se copian **verbatim** en su `<texto del prompt>` |
|---|---|
| 1 — `TramiteInstance.xml` | La(s) fila(s) de §6 de sus ficheros; el `### Paso N` correspondiente; la tabla entera de **§2 Identidad del trámite y del tipo** |
| 2 — `TipoExpedienteInstance.xml` + `estados.puml` | Sus filas de §6; los `### Paso N` de ambos ficheros; **§3 Máquina de estados completa** (las tablas por fase **y** la tabla de transiciones) |
| 3 — `CreateFilesTask` | El `### Paso N` de `CreateFilesTask` **íntegro**, más lo que exige §2.1 de este contrato |
| 4 — `domains.xml` | Su fila de §6; su `### Paso N`; **§4 Modelo completa** (tabla de campos con la columna «quién lo rellena», tabla de enums, bloque `<extra-code-model>`); **§5 Documentos PDF** (para saber qué constantes lleva el enum `TipoDocumentoPdf`); las filas de **§11 Reparto de reglas** que ubiquen una regla en el modelo |
| 5 — `views.xml` de la raíz | Su fila de §6; su `### Paso N` **con el resumen estructural de paneles** que el diseño incluye ahí |
| 6 — `InitialEventManagerImpl.java` | Su fila de §6; su `### Paso N`; **§8 Especificación del InitialEventManagerImpl, ÍNTEGRA** (la tabla ordenada de asignaciones, las dependencias a inyectar y las reglas explícitas que §8 declare: la de lo que el `Tramitador` ya rellena y, **cuando apliquen**, la de `dniFirmaDocumentoEntrada` —solo si el tipo firma algún documento **en cliente**— y la de `personaSolicitante`/`personaInteresada` —solo si algún `trigger*` crea registro de entrada—; si el tipo no hace ninguna de las dos cosas, §8 lo dice explícitamente y esa ausencia **MUST NOT** leerse como diseño incompleto); las filas de §11 que ubiquen una regla en el `triggerInitialEvent` |
| 7 — cada fase `<FASE>` | **Todas** las filas de §6 de esa fase, sean las que sean (las tres de una fase completa; menos en una **iniciativa de MODIFICACIÓN**, donde §6 lista solo los ficheros que el delta toca); los `### Paso N` de **cada uno** de esos ficheros; la subsección **`### Fase <FASE>` de §9 (Especificación de los PhaseEventManagerImpl), ÍNTEGRA** —cabecera, lista de `trigger<Evento>` con sus **listas numeradas de acciones en orden**, lista de `onEnter<Estado>` y **lista de cobertura**—; la subsección **`### Fase <FASE>` de §10 (Especificación de los StateEventValidatorImpl), ÍNTEGRA** —tabla de cobertura y el contenido de cada método con sus `field(...)` y **argumentos literales**—; el resumen estructural `(estado, perfil) → paneles → botones` de esa fase; la **tabla de transiciones de §3** filtrada a las filas cuyo origen sea un estado de esa fase; las filas de §11 que apliquen |
| 8 — `documentospdf/` | Sus filas de §6; su `### Paso N`; **§5 Documentos PDF completa** |
| 9 — `permisos-demo.xml` | Su fila de §6 (con `Acción: Modificar`); su `### Paso N`; **§12 Asignación de perfiles completa** |

Reglas de relleno del `<texto del prompt>`:

- **MUST** conservarse la columna `Acción` (`Crear` / `Modificar`) de cada fila de §6: el implementador la usa para decidir cómo materializar.
- **MUST NOT** resumirse, reescribirse ni parafrasearse el texto del diseño. Se copia **verbatim** — el diseño es el contrato.
- **MUST NOT** inventarse pasos, campos, estados, eventos, métodos, paneles, botones ni ficheros que el `design.md` no declare.
- **MUST NOT** duplicarse una misma subsección `### Fase <FASE>` en dos tareas distintas: la de §9 y la de §10 de una fase van **solo** en la tarea de esa fase.
- Para una tarea de **XML o `.puml` ya materializado**, el `<texto del prompt>` **MUST** indicar explícitamente el fichero de origen en `design/...`, la **ruta destino resuelta** y que se **copia literalmente**, **sobrescribiendo** el esqueleto que dejó `CreateFilesTask` si lo hay, **sin regenerarlo** (`implementation.md` §2).
- Para la tarea de **`permisos-demo.xml`**, el texto **MUST** decir que es una **fusión**, no una copia, y remitir a `implementation.md` §5.
- **MUST** incluirse en toda tarea de `.java` / `.kt` la frase de que la especificación del diseño es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ningún método, clase, campo ni acción que la especificación no liste.

- ✅ CORRECTO: la tarea de la fase `<FASE>` lleva pegada, palabra por palabra, la subsección `### Fase <FASE>` de §9 con sus listas numeradas de acciones y la de §10 con sus `field(...)`.
- ❌ INCORRECTO: la tarea dice «implementa los triggers de la fase `<FASE>` según el diseño» y remite al `design.md` sin copiar nada (el implementador se queda sin especificación).
- ❌ INCORRECTO: pegar en la tarea el contenido de `design/fases/<fase>/views.xml` (el XML se **copia**, no se transcribe en la tarea).

---

## 5. Escribir el índice `tasks.md` y propagar `test-e2e-desc.md`

1. Escribe `{iniciativa}/implementation/tasks.md` con **exactamente** esta plantilla, una línea por tarea generada, en orden, cada una con un **checkbox sin marcar**:

```
---
type: implementation-tasks
template: <valor copiado del design.md>
---

# Lista de tareas a implementar
- [ ] [Tarea 01](task_01.md)
- [ ] [Tarea 02](task_02.md)
```

- Un enlace por cada `task_NN.md` creado, en orden, precedido de `- [ ]`.
- El texto del enlace es `Tarea NN`; el destino es `task_NN.md`.
- `template:` se copia verbatim del `design.md`, igual que en `task_NN.md` (§4).
- Todos los checkboxes se escriben **sin marcar**: marcarlos es responsabilidad del implementador al completar cada tarea (`implementation.md` §8). **MUST NOT** marcarlos al crear el índice.
- ✅ CORRECTO: `- [ ] [Tarea 01](task_01.md)`.
- ❌ INCORRECTO: `- [Tarea 01](task_01.md)` (sin checkbox), `- [x] [Tarea 01](task_01.md)` (marcado al crear), `- [ ] [Tarea 1](tarea_01.md)` (número sin dos dígitos y fichero que no existe).

2. **Copia literalmente** `{iniciativa}/design/test-e2e-desc.md` a `{iniciativa}/implementation/test-e2e-desc.md`. Es **contrato fijo hacia abajo**: lo ejecuta `/sdd-debug-with-test-e2e-desc` contra la aplicación real. **MUST NOT** modificarlo, resumirlo, renumerarlo ni ejecutarlo aquí. Si no existe, no pasa nada.

3. **MUST NOT** crearse ninguna tarea de tests, **salvo** la excepción de `tests-code.md` §4: que `design/test-unit-desc.md` describa una clase auxiliar propia con lógica de negocio aislable, en cuyo caso —y solo en ese— se crea una tarea de test para esa clase (§2, reglas de instanciación).
   La conformidad de un tipo de expediente la dan, por lo demás, los tests **ya existentes y escritos a mano** de `src/test/java/com/educaflow/tiposexpedientes/`, que recorren automáticamente todos los tipos del árbol.
   Lee `tests-code.md` para el detalle y para el tratamiento de `design/test-unit-desc.md`.

---

## 6. Token de salida

Tras escribir todo, responde con el formato que define el skill:

- Primera línea **exactamente** `ESCRITO: implementation/`.
- Una línea **exactamente** `=== TAREAS ===` y, debajo, **una línea por tarea** en orden, con el formato `task_NN.md | {título} | {ficheros que cubre}`.
- **MUST NOT** pegar el contenido de las tareas en la respuesta (ya está en disco).

> **Ejemplo** (ilustrativo, NO normativo) de las primeras líneas del bloque, con nombres inventados:
>
> ```
> ESCRITO: implementation/
> === TAREAS ===
> task_01.md | Trámite | tramites/solicitud_de_ejemplo/TramiteInstance.xml
> task_02.md | Máquina de estados | v1/TipoExpedienteInstance.xml, v1/estados.puml
> task_03.md | Generar esqueletos (CreateFilesTask) | (tarea Gradle)
> task_07.md | Fase REVISION | v1/revision/PhaseEventManagerImpl.java, v1/revision/StateEventValidatorImpl.kt, v1/revision/views.xml
> ```

---

## 7. Checklist del descomponedor

Antes de devolver el token, **MUST** recorrer este checklist. Si algo falla, corrige y repite. **LIMIT**: máximo 3 iteraciones de corrección; si al cabo de 3 sigue fallando algo, anótalo en tu respuesta.

**Lectura y cobertura**

- [ ] ¿Se leyó el `design.md` **íntegro**, incluidas las secciones 8, 9 y 10?
- [ ] ¿Se localizó la tabla `## 6. Ficheros a crear o modificar` y **cada fila** está cubierta por **exactamente una** tarea?
- [ ] ¿Se contó el número real de fases (`design/fases/`) y de documentos (`design/documentospdf/`) en vez de asumir uno?

**Orden**

- [ ] ¿El orden de las tareas es exactamente el de §2: trámite → maestro+puml → **CreateFilesTask** → domains → views raíz → initial → una por fase → documentospdf → permisos?
- [ ] ¿`CreateFilesTask` es una **tarea propia** en la posición 3, con su comando exacto y la ruta resuelta, y **no** un paso escondido dentro de otra?
- [ ] ¿Hay **una tarea por cada fase** declarada, en el orden de declaración, agrupando **todos los ficheros que §6 declare para ella** (en una fase completa: `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt` y `views.xml`; en una iniciativa de MODIFICACIÓN, solo los que el delta toque)?
- [ ] ¿La tarea de `documentospdf/` existe si y solo si el tipo genera algún documento?

**Contenido de las tareas**

- [ ] ¿Cada `task_NN.md` tiene `type: implementation-task`, su lista de skills y el texto del diseño **verbatim**?
- [ ] ¿La tarea del `InitialEventManagerImpl` lleva **§8 íntegra**?
- [ ] ¿Cada tarea de fase lleva **su** subsección `### Fase <FASE>` de **§9** y **su** subsección `### Fase <FASE>` de **§10**, íntegras, con las listas numeradas de acciones y los argumentos literales del DSL?
- [ ] ¿La tarea del `domains.xml` lleva §4 completa con la columna «quién lo rellena»?
- [ ] ¿Ninguna tarea transcribe el contenido de un XML de `design/` en vez de mandar copiarlo?
- [ ] ¿Cada tarea de XML/`.puml` indica origen en `design/...`, **ruta destino resuelta** y «cópialo literalmente, sobrescribiendo el esqueleto»?
- [ ] ¿La tarea de `permisos-demo.xml` dice **fusión**, no copia, y su fila lleva `Acción: Modificar`?

**Skills**

- [ ] ¿Toda tarea que toca la entidad, un `trigger*` o el validador lleva `k-secure-coding`?
- [ ] ¿Toda tarea de `.java`/`.kt` lleva `k-code-quality`?
- [ ] ¿Ninguna tarea de vistas lleva `k-vistas` (las vistas de tipo de expediente están excluidas)?

**Salida**

- [ ] ¿Existe `implementation/tasks.md` con `type: implementation-tasks` y, por tarea, un checkbox **sin marcar** + enlace correcto, en orden?
- [ ] ¿Se copió `design/test-e2e-desc.md` a `implementation/test-e2e-desc.md` **sin modificarlo**?
- [ ] ¿**No** se creó ninguna tarea de tests ni ningún fichero bajo `src/test/...`, salvo la excepción de `tests-code.md` §4 si `design/test-unit-desc.md` describe una clase auxiliar aislable?
- [ ] ¿**No** se creó ninguna tarea para un `i18n_*.csv`, un `estados.png`, un `States.java`, un data-init generado ni nada bajo `build/`?
- [ ] ¿La respuesta lleva `ESCRITO: implementation/` + el bloque `=== TAREAS ===` con una línea por tarea?
