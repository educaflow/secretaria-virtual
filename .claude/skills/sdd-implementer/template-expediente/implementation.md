# Contrato de materialización — de una tarea al árbol del proyecto

Lo lee el **implementador** (`README.md` §3.2) y lo consulta el **corrector-build** (`README.md` §3.4) para saber qué puede tocar. Define **cómo materializar UNA tarea** en el árbol del proyecto. El implementador recibe la ruta de **una** `task_NN.md`; este contrato dice cómo ejecutarla según su naturaleza.

Los placeholders son los de `README.md` §0.1.

---

## 1. Principio rector — qué se copia y qué se escribe

| Artefacto | Qué hay en `design/` | Qué hace el implementador |
|---|---|---|
| **XML y `.puml`** | El fichero **final**, ya escrito y completo | Lo **copia literalmente** a su ruta destino |
| **`.java` y `.kt`** | **Nada**: solo la especificación quirúrgica en `design.md` §8, §9 y §10 | **Escribe el código** a partir de esa especificación, sobre el esqueleto de `CreateFilesTask`, delegando en `developer-code-implementer` (§4) |
| **`permisos.xml`** | Un **fragmento** con solo lo nuevo | Lo **fusiona** dentro de `permisos-demo.xml` (§5) |

**MUST NOT**:

- **MUST NOT** reescribir un XML del diseño desde el texto del `design.md`.
- **MUST NOT** reformatearlo al vuelo (indentación, orden de atributos, saltos de línea).
- **MUST NOT** «mejorarlo», completarlo ni corregirlo. Si un XML del diseño está mal, es un `DESIGN-ERROR` (§7), no algo que se arregle aquí.

Regenerar o retocar un XML pierde las correcciones que el diseñador ya validó e introduce divergencias silenciosas entre el diseño y el árbol.

---

## 2. Tareas de XML y `.puml` — mapeo origen → destino

Copia **literalmente** (con `cp`, creando la carpeta destino con `mkdir -p` si no existe):

| Origen en `design/` | Destino en el árbol |
|---|---|
| `design/TramiteInstance.xml` | `src/main/java/com/educaflow/tramites/<tramite>/TramiteInstance.xml` |
| `design/TipoExpedienteInstance.xml` | `<carpeta de versión>/TipoExpedienteInstance.xml` |
| `design/domains.xml` | `<carpeta de versión>/domains.xml` |
| `design/views.xml` | `<carpeta de versión>/views.xml` |
| `design/estados.puml` | `<carpeta de versión>/estados.puml` |
| `design/fases/<fase>/views.xml` | `<carpeta de versión>/<fase>/views.xml` |
| `design/documentospdf/<doc>.xml` | `<carpeta de versión>/documentospdf/<doc>.xml` |
| `design/documentospdf/_<fragmento>.xml` | `<carpeta de versión>/documentospdf/_<fragmento>.xml` |
| `design/permisos.xml` | **NO se copia**: se **fusiona** en `src/main/resources/data-demo/input/permisos-demo.xml` (§5) |

- **CRITICAL — el mapeo `design/fases/<fase>/views.xml` → `<carpeta de versión>/<fase>/views.xml` pierde el segmento `fases/`.** La carpeta destino es la de la fase **directamente bajo la carpeta de versión**, con el nombre de la fase **en minúsculas**. En cualquier otra ubicación el preprocesador no la encuentra y el `views.xml` queda muerto.
- La carpeta destino de un documento **MUST** llamarse `documentospdf`. `documentos/` renderiza pero **no** se escanea para el enum `TipoDocumentoPdf`: el documento queda muerto sin aviso.
- Verifica cada copia con un `diff` contra el fichero de origen: **MUST** ser vacío.

### 2.1 CRITICAL — copiar ENCIMA del esqueleto es lo esperado, no es un CONFLICT

`CreateFilesTask` (§3) es **idempotente** y **nunca pisa lo ya escrito**: si un fichero ya existe, lo deja intacto y no imprime nada. Consecuencia práctica:

- Los ficheros que la tarea generó (`domains.xml`, `views.xml` de la raíz, `InitialEventManagerImpl.java`, y por cada fase `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt` y `views.xml`) existen en el destino como **cáscaras vacías** cuando llega su tarea.
- Los XML del diseño se copian **encima** de esas cáscaras, **sobrescribiéndolas**. **Eso es el comportamiento esperado**: la fila de §6 del `design.md` dice `Crear` y el destino existe **porque lo acaba de crear `CreateFilesTask`**.
- **MUST NOT** reportarse `CONFLICT` por ello. **MUST NOT** intentarse «fusionar» el XML del diseño con la cáscara: la cáscara no aporta nada.

- ✅ CORRECTO: `cp design/fases/<fase>/views.xml <carpeta de versión>/<fase>/views.xml`, sobrescribiendo la cáscara con `<button name="">` sin rellenar.
- ❌ INCORRECTO: devolver `CONFLICT: … ya existe <carpeta de versión>/domains.xml` cuando ese fichero es el esqueleto que la tarea 3 acaba de generar.
- ❌ INCORRECTO: conservar del esqueleto un `<include-panels>` vacío o un `<button name="">` «por si acaso»: un `<button name="">` es una violación de la regla **Y1** y hace fallar el build.

Un `CONFLICT` **sí** procede cuando el destino existía **antes de esta iniciativa** con contenido propio y la fila del diseño dice `Crear` (p. ej. el trámite ya estaba dado de alta): entonces el diseño debería decir `Modificar`, y se decide con el usuario.

Para una fila con `Acción: Modificar` cuyo destino **no exista**, responde `BLOCKED: {tarea} — la base que el diseño asume no existe: {ruta destino}`.

---

## 3. La tarea de `CreateFilesTask`

Es una tarea que **no escribe ficheros a mano**: ejecuta la tarea Gradle que genera los esqueletos.

```bash
./gradlew -q CreateFilesTask -Ptipo=<carpeta de versión>
```

- **MUST** ejecutarse con `Bash` desde la raíz del proyecto, con la ruta **resuelta** de la carpeta de versión (la que la tarea trae ya escrita).
- **MUST** haberse copiado antes el `TipoExpedienteInstance.xml` **completo**: la tarea lo **lee** para saber qué subcarpetas de fase crear. Si falta, la tarea falla con mensaje explícito.
- **Verificación:** por cada fichero esperado, o bien aparece una línea `CREADO <ruta>`, o bien el fichero ya existía. Los ficheros esperados son:
  - en la raíz de la versión: `domains.xml`, `views.xml`, `InitialEventManagerImpl.java`;
  - en **cada** subcarpeta `<fase>/`: `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt`, `views.xml`.
- Si falta alguna subcarpeta de fase esperada → el `TipoExpedienteInstance.xml` no declara esa fase: es un `DESIGN-ERROR` si el diseño sí la declara en otras secciones, o un `BLOCKED` si el fallo es del entorno.
- **MUST NOT** usarse `-Pfase`, ni engancharse la tarea al build, ni ejecutarse `./gradlew run` o `--debug-jvm`.
- **MUST NOT** darse por hecho que la tarea sobrescribe: **nunca pisa lo ya escrito**. Si un fichero no aparece como `CREADO` y tampoco existe, algo va mal — repórtalo.
- Si esta tarea se relanza más adelante (por ejemplo, tras añadir una fase), es **seguro**: solo creará lo que falte.

---

## 4. Tareas de `.java` y `.kt` — rellenar sobre el esqueleto

**CRITICAL — el implementador MUST NOT escribir el código Java/Kotlin directamente.** Se delega en `developer-code-implementer`.

**OBLIGATORIO**, en este orden:

1. Lee la sección `## Skills a usar` de la tarea y **carga cada skill con la herramienta `Skill`** **antes** de implementar nada. **MUST NOT** empezar sin haberlos cargado.
2. Comprueba que el esqueleto existe en la ruta destino (lo dejó `CreateFilesTask`). Si no existe, `BLOCKED`.
3. **Invoca `developer-code-implementer`** pasándole el `<texto del prompt>` de la tarea **verbatim** — que ya incluye la especificación quirúrgica del `design.md` (§8 para el `InitialEventManagerImpl`; la subsección `### Fase <FASE>` de §9 y de §10 para una fase).

Al invocarlo, inclúyele además estas notas:

- **Se rellena SOBRE el esqueleto existente**, no se crea el fichero de cero: el paquete, el nombre de la clase, el supertipo y la ubicación ya vienen dados y **MUST NOT** cambiarse. La clase `PhaseEventManagerImpl` de una fase **MUST** vivir en la carpeta con el nombre de la fase **en minúsculas**; en cualquier otra es código muerto que aparenta estar vivo.
- **El `domains.xml` y los `views.xml` ya están colocados** y son **contrato fijo**: los nombres de los campos del Java **MUST** coincidir con los del dominio, y los nombres de los `trigger<Evento>` con los `name` de los botones de las vistas. **MUST NOT** editarlos para que cuadre el código.
- **La especificación del diseño es contrato fijo y su ORDEN es normativo**: la lista numerada de acciones de un `trigger*` se implementa **en ese orden**, y los argumentos literales del DSL del validador se escriben **tal cual**.
- **Superficie cerrada (CRITICAL)**: **MUST** implementarse **exactamente** los métodos que la especificación lista, ni uno más ni uno menos:
  - un `trigger<Evento>` por cada evento de la **unión sin repetir** de los `events` de todos los estados de la fase (incluido `DELETE` si está declarado), anotado `@WhenEvent`, con **DOS** parámetros de entidad y `EventContext`;
  - un `onEnter<Estado>` por **cada** estado de la fase, incluidos los que no tienen eventos y los `closed`, anotado `@OnEnterState`, con **UN** parámetro de entidad y `EventContext`;
  - un `getForState<Estado>InEvent<Evento>` por **cada pareja** (estado, evento) declarada en la fase **salvo las de `DELETE`**, anotado `@BeanValidationRulesForStateAndEvent`, con **cero** parámetros;
  - exactamente un `triggerInitialEvent` en el `InitialEventManagerImpl`, y **ninguno** en ningún `PhaseEventManagerImpl`.
  - **MUST NOT** crearse métodos, clases, campos, enums, constantes ni acciones que la especificación no liste. Si «haría falta» algo no listado → **parar y reportar** `BLOCKED: {tarea} — superficie insuficiente: {qué falta y por qué}`. Ampliar la superficie lo decide `/sdd-designer`.
  - **MUST NOT** clonarse el patrón de otro trámite del árbol: otro trámite enseña **cómo se escribe**, nunca **qué hay que escribir** (`README.md` §4.1).
  - **MUST NOT** factorizarse ningún `trigger*` / `onEnter*` / `getForState*InEvent*` en una **superclase compartida** entre fases o versiones: el dispatcher usa `getDeclaredMethods()`, así que un método heredado **no cuenta** ni en runtime ni en los tests. Si hay lógica común, se declara el método en cada fase delegando en un helper o servicio.
- **El `import` de `States` MUST ser el de la propia versión** (`<basePackageName>.States`). Referenciar el `States` de otro tipo o versión **compila** y revienta en runtime.
- **MUST NOT** usarse `System.out`: logger slf4j.
- Los errores de negocio se lanzan como `BusinessException`.
- La instrucción de **parar y reportar** ante cualquier bloqueo, sin adivinar.

---

## 5. Tarea de `permisos-demo.xml` — fusión, no copia

`design/permisos.xml` es un **fragmento**, con raíz `<datos>` y únicamente los bloques que llevan contenido nuevo. Se **fusiona** en `src/main/resources/data-demo/input/permisos-demo.xml`.

Procedimiento:

1. Lee el `permisos-demo.xml` real y el fragmento `design/permisos.xml`.
2. Por **cada** bloque del fragmento (`<perfiles>`, `<asignacionesTipoUsuario>`, `<asignacionesTipoUsuarioTipoExpediente>`, `<asignacionesCargoTipoExpediente>`, `<asignacionesCentroUsuario>`), **inserta sus hijos dentro del bloque del MISMO nombre** que ya existe en el fichero real, al final de ese bloque. Si el bloque no existe en el fichero real, créalo dentro de `<datos>`.
3. **MUST NOT** duplicarse un `<perfil name="...">` que ya exista en `<perfiles>`: si ya está, se omite esa línea (no es un error).
4. **MUST NOT** tocarse ninguna asignación de **otro trámite** ni de otra versión: se **añade**, nunca se reordena, se reescribe ni se borra nada preexistente.
5. **MUST NOT** sobrescribirse el fichero entero con el fragmento.
6. Verificación: el fichero resultante sigue siendo XML bien formado, contiene **todas** las líneas que tenía antes y **todas** las nuevas del fragmento.

- ✅ CORRECTO: insertar un `<asignacion tipoUsuarioCode="…" perfilName="…" tramiteCode="…"/>` al final del `<asignacionesTipoUsuario>` existente, dejando intacto el resto.
- ❌ INCORRECTO: `cp design/permisos.xml src/main/resources/data-demo/input/permisos-demo.xml` (borra las asignaciones de todos los demás trámites).
- ❌ INCORRECTO: volver a declarar un `<perfil>` que ya estaba.

---

## 6. Prohibiciones duras

- **CRITICAL — MUST NOT crearse JAMÁS `i18n_es.csv` ni `i18n_ca.csv`**, en ninguna carpeta (ni en la del trámite, ni en la de la versión, ni en las de fase). Los genera el build (`managei18nfiles`) a partir de los `title`/`help`/`name` del fuente. Escribirlos a mano está **prohibido** y es un fallo **bloqueante**. Tampoco se añaden ni se quitan filas a un CSV existente: lo único editable a mano es la columna `message` de una traducción automática mala, y eso no es trabajo de la implementación.
- **MUST NOT** escribirse `States.java`: la emite `GenerateStatesTask` en `build/src-gen-states/main/java` en cada build. No se versiona ni se edita.
- **MUST NOT** escribirse ni editarse `estados.png`: lo renderiza `GenerateDocs`.
- **MUST NOT** editarse el bloque `<extra-code-model>` de un `domains.xml` ya colocado: `RichDomainXmlTask` lo reescribe en cada build. Su contenido viene del diseño.
- **MUST NOT** escribirse a mano el data-init de trámites ni de tipos de expediente: lo generan `generateDataInitTramites` y `generateDataInitTiposExpedientes` en `build/`, y se recargan en cada arranque.
- **MUST NOT** crearse una carpeta `documentospdf/originales/` ni añadirse `.pdf` binarios: un impreso oficial de partida es material aportado a mano, no producto de la implementación.
- **MUST NOT** añadirse una `<permission name="<Entidad>.all">` a `auth-expedientes.xml` si el diseño no lo pide; y si lo pide, **MUST NOT** copiarse el patrón `create/read/write/remove` **sin `condition`**: es un agujero conocido documentado en `CLAUDE.md`.
- **MUST NOT** escribirse nada bajo `src/test/...` (`tests-code.md`).
- **MUST NOT** aplicarse a los `views.xml` de un tipo de expediente las reglas `VAR-` de `agent_docs/view-rules.md` ni las convenciones de `k-vistas`: estas vistas son **preprocesadas** y están excluidas.

### 6.1 `GenerateDocs` tras tocar un `.puml`

Al colocar (o cambiar) `<carpeta de versión>/estados.puml`, la tarea **MUST** regenerar su PNG:

```bash
./gradlew -q GenerateDocs
```

- La tarea recorre `src/` y genera un `<nombre>.png` junto a cada `<nombre>.puml`.
- Es **incremental por fecha**: omite el PNG que sea **más reciente** que su fuente. Por eso, si el `.puml` cambia y no se relanza, el PNG queda **desincronizado en silencio**.
- Va enganchada a `build` con `finalizedBy`, así que un build completo ya la ejecuta; lánzala suelta al terminar la tarea del `.puml` para no dejar el árbol inconsistente entre tareas.
- **MUST NOT** editarse el `.png` a mano ni invocarse el jar de PlantUML por separado.

---

## 7. Detenerse y reportar — qué token usar

Reportar (no adivinar) es la respuesta correcta ante un bloqueo. El token depende del **origen** del problema:

| Token | Cuándo | Ejemplos de este artefacto |
|---|---|---|
| `DONE: {tarea}` | La tarea quedó materializada correctamente | — |
| `CONFLICT: {tarea} — {qué destino ya existe}` | El destino **preexistía a la iniciativa** con contenido propio y la fila dice `Crear`; lo decide el usuario | La carpeta del trámite ya existía con su `TramiteInstance.xml`; ya existe una carpeta de versión con el mismo nombre |
| `BLOCKED: {tarea} — {motivo}` | Bloqueo del **entorno**, no culpa del diseño | `CreateFilesTask` falla porque Gradle no arranca; falta un esqueleto que la tarea 3 debía crear; una fila `Modificar` cuyo destino no existe; superficie insuficiente para implementar lo que la especificación pide |
| `DESIGN-ERROR: {tarea} — {motivo detallado}` | El problema está **en el diseño** y no se resuelve escribiendo código | Ver §7.1 |

**MUST NOT** pegar el código ni el XML en la respuesta (ya está en disco): solo el token + 1-2 líneas de resumen.

### 7.1 Qué es un `DESIGN-ERROR` en este artefacto

**MUST NOT** editarse el diseño para forzar que cuadre. Da el **máximo detalle**: qué fichero del diseño, qué es inconsistente o qué falta, y por qué no se puede resolver con código.

Casos típicos:

- Un XML de `design/` **mal formado**, incompleto o con restos de esqueleto (`TODO`, `...`, `<button name="">`).
- El `TipoExpedienteInstance.xml` declara una fase para la que **no hay** `design/fases/<fase>/views.xml`, o al revés.
- La especificación de §9 lista un `trigger<Evento>` para un evento que ningún `<state>` declara, o **falta** el trigger de un evento declarado.
- La especificación de §10 lista un método para una pareja (estado, evento) que no existe, o **falta** el método de una pareja declarada, o incluye un `getForState<Estado>InEventDelete` (prohibido).
- Un `UPDATE_STATE` apunta a un estado que el `TipoExpedienteInstance.xml` no declara, o contradice la tabla de transiciones.
- El `domains.xml` no define un campo que §8, §9 o §10 asignan o validan; o un `field(...)` del validador menciona un campo clasificado como `servidor`.
- El `<extra-code-model>` declara una constante `TipoDocumentoPdf` para un documento que no está en `design/documentospdf/`, o falta la constante de uno que sí está.
- Un `views.xml` de fase incluye un panel que el form plantilla de la raíz no declara.
- Dos reglas del diseño se contradicen entre secciones.

---

## 8. Marcar la tarea como completada en el índice

**Solo al devolver `DONE`**: **antes** de responder, marca **esta** tarea como completada en `{iniciativa}/implementation/tasks.md`, cambiando su línea de `- [ ] [Tarea NN](task_NN.md)` a `- [x] [Tarea NN](task_NN.md)` (con `Edit`).

- **MUST** marcarse **solo** la línea de la tarea recibida; **MUST NOT** tocarse las demás.
- **MUST NOT** marcarse ante `CONFLICT`, `BLOCKED` o `DESIGN-ERROR`: el checkbox refleja tareas realmente terminadas.
- Si el índice no existe, omite este paso sin error.
