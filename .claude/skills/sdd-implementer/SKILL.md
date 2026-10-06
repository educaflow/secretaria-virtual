---
name: sdd-implementer
description: Tercer paso del pipeline SDD: dado un `design.md` (`type: design`) producido por `/sdd-designer`, lo convierte en código real con un subagente descomponedor que escribe las tareas, un implementador por tarea y un bucle verificar/corregir la compilación; si una errata pequeña del diseño impide implementarlo tal cual, la corrige en el propio diseño y sigue. Es un MOTOR genérico y agnóstico al artefacto: aporta solo el flujo y delega TODO lo específico en el `README.md` de la carpeta de plantillas activa (`template-<nombre>/`, la que declare el frontmatter `template:` del `design.md`, heredado de la spec; configurable con `--template-dir`), así que cambiar de plantilla cambia por completo qué y cómo se implementa sin tocar este skill. La salida es el código real en el árbol del proyecto —lo que declare la plantilla activa— más las tareas en `implementation/`, listo para `/sdd-close`.
handoffs:
  - label: Cerrar la iniciativa
    agent: sdd-close
    prompt: Cerrar la iniciativa recién implementada en .sdd/drafts/{carpeta-iniciativa}/ — archivar el draft verbatim en .sdd/archive/.
---

# sdd-implementer

Eres un **motor de implementación** del pipeline SDD: transformas un **plan de diseño** en **código real** dentro del proyecto. La entrada la produce `/sdd-designer` y la salida la cierra `/sdd-close`.

**CRITICAL — eres agnóstico al artefacto.** Este `SKILL.md` define **solo el flujo y la orquestación de agentes**. **No sabe nada de cómo es el diseño ni de cómo se implementa** (ni qué ficheros contiene `design/`, ni cómo se agrupan las tareas, ni cómo se materializa un XML o un `.java`, ni con qué comando se compila): **todo eso lo declara la guía `<plantilla-activa>/README.md`**, que los subagentes leen como contrato. **MUST NOT** asumir de memoria ningún detalle de la implementación; **MUST NOT** nombrar ficheros, plantillas, comandos ni rutas concretas de la implementación en este skill. Así, apuntar `--template-dir` a otra carpeta de plantillas con un README distinto cambia por completo lo que se implementa **sin tocar este skill**.

El skill lanza **cuatro roles** de subagente (todos leen el mismo `README.md`, cada uno hace una tarea distinta):

- **descomponedor** — lee el diseño y escribe la lista de tareas en `implementation/` (§7).
- **implementador** — coge **una** tarea y la materializa en el árbol del proyecto (§9).
- **verificador-build** — compila el proyecto y reporta si pasa o los errores (§10).
- **corrector-build** — corrige los errores de compilación que el verificador-build reportó (§10).

Y un quinto, **editor-diseño** (§9.2), que corrige en `design/` las erratas pequeñas que encuentren el implementador o el corrector-build. **No** lee el README de la plantilla (trabaja sobre el diseño, no sobre la implementación) y los README **MUST NOT** delimitarlo.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- **Ruta a un `design.md`** existente (p.ej. `.sdd/drafts/2026-05-21_20-14_correos/design/design.md`). El skill valida el frontmatter `type: design` y procede.
- **Sin argumentos**: el skill auto-detecta la **última** iniciativa de `.sdd/drafts/` (por orden alfabético del prefijo timestamp) que contenga `design/design.md` y pide confirmación (§4.2).
- Flags de override `--template-dir=`, `--in=`, `--root=` (Apéndice A).

---

## Outline

1. **Fase 0 — Localizar** la iniciativa y su `design.md`, y **confirmar** la ruta detectada (§4).
2. **Fase 1 — Validar** el frontmatter `type: design`, **cargar** el contrato (`<plantilla-activa>/README.md`), resolver las rutas de entrada y **apartar** el `implementation/` de una ejecución anterior (§5).
3. **Fase 2 — Descomponer**: lanzar el subagente **descomponedor**, que escribe las tareas en `implementation/` (§7).
4. **Fase 3 — Informar**: mostrar el resumen de tareas (**informativo, sin bloquear**) y continuar automáticamente (§8).
5. **Fase 4 — Implementar**: lanzar **un subagente implementador por tarea**, en orden, que materializa cada tarea en el árbol del proyecto (§9).
   - **§9.2 — Erratas del diseño**: si un implementador o un corrector-build devuelve `DESIGN-ERRATA`, el **editor-diseño** corrige el diseño y el motor relanza ese mismo subagente sobre el diseño corregido (§2.4).
6. **Fase 5 — Verificar/corregir el build**: bucle subagente **verificador-build** → (si falla) subagente **corrector-build**, hasta `OK-COMPILA` (**LIMIT** 20 iteraciones) (§10).
7. **Fase 6 — Cerrar** con mensaje al usuario y handoff a `/sdd-close` (§11).

**STOP conditions**:

- `--template-dir=` apunta a una carpeta que **no contiene `README.md`** (la guía que declara todo lo específico) → **ERROR** y detente.
- `--template-dir=` apunta a otra carpeta `template-*/` de este skill **distinta** de la que declara el frontmatter `template:` de la iniciativa, o ese `template:` no resuelve a ninguna carpeta de plantillas (§2.2) → **ERROR** y detente: **MUST NOT** mezclarse plantillas — sus arquitecturas no son compatibles.
- El frontmatter `template:` de la iniciativa vale `external` (se especificó con una plantilla externa) y **no** se pasa `--template-dir=` (§2.2) → **ERROR** y detente pidiéndolo. **MUST NOT** preguntar la plantilla ni caer a una carpeta interna.
- La ruta explícita a `design.md` no existe o no está dentro de `{raíz de drafts}/{iniciativa}/design/` (Fase 0 caso 1; la raíz de drafts es `.sdd/drafts/` o la de `--root=`) → **ERROR** y detente.
- Frontmatter de `design.md` no contiene `type: design` → **ERROR** y detente sin escribir nada.
- Sin ruta, ninguna iniciativa de `{raíz de drafts}` tiene `design/design.md` (Fase 0 caso 2) → **STOP** y pide la ruta.
- El usuario no confirma la ruta auto-detectada (Fase 0 caso 2) → **STOP** y pide la ruta.
- Sin `--template-dir=`, ni el `design.md` ni el `specification.md` traen `template:` (§2.2) → `AskUserQuestion` con una opción por carpeta `template-*/` del skill antes de continuar.
- El **descomponedor** no devuelve el token `ESCRITO: implementation/` con su lista de tareas tras 1 reintento → **STOP** y muestra el problema.
- Un **implementador** devuelve `CONFLICT` (fichero/elemento destino ya existe) → **STOP** y `AskUserQuestion` (sobrescribir / mantener / abortar); relanza el implementador con la decisión.
- Un **implementador**, un **verificador-build** o un **corrector-build** devuelve `BLOCKED` (dependencia externa inexistente, recurso no disponible, instrucción ambigua que no es culpa del diseño) → **STOP** y pregunta al usuario. **MUST NOT** adivinar.
- Un **implementador** o un **corrector-build** devuelve `DESIGN-ERROR` (el problema está en el diseño y **no se puede resolver con código**: hay que volver a `/sdd-designer`) → el motor escribe `implementation/error_design.log` con la explicación detallada y **detiene el skill** (**STOP**): no pregunta al usuario, no relanza subagentes, no pasa a la siguiente fase (§9.1). **MUST NOT** editar el diseño para forzar que cuadre.
- El **editor-diseño** devuelve alguna línea `RECHAZADO:` (la errata no es pequeña), no devuelve `APLICADO: <n>` tras 1 reintento, o se supera el **LIMIT** de 5 erratas aplicadas → se trata como un `DESIGN-ERROR` (§9.1): `error_design.log` y **STOP**.
- Tras **20** iteraciones del bucle de build (Fase 5) el verificador-build sigue sin responder `OK-COMPILA`, o el conjunto de pares (`fichero`, `mensaje`) de su JSONL es igual al de la iteración anterior (fallo persistente) → **STOP** y `AskUserQuestion`. **MUST NOT** dar la implementación por buena.

---

## 1. Entrada y salida

### 1.1 Entrada

El **diseño** de la iniciativa, cuyo índice es `design.md` (único fichero de entrada con nombre fijo; debe contener `type: design`). En su misma carpeta `design/`, el diseñador ya dejó el resto del diseño (XML materializados, descripciones de tests, reglas complejas, tests E2E) — el skill **no asume cuáles son** (los define la plantilla de `/sdd-designer`); los subagentes los leen siguiendo el contrato.

### 1.2 Salida

Este skill produce salida en tres sitios:

- En `{raíz de drafts}/{iniciativa}/implementation/`: la lista de tareas y los ficheros de contrato hacia abajo (los consumen `/sdd-debug-with-test-e2e-desc` y `/sdd-close`). **Su estructura interna la define la plantilla**, no este skill.
- En el **árbol del proyecto**: el código real que la plantilla activa declare (bajo `src/main/...` y, si esa plantilla lo prevé, `src/test/...`), que escriben los subagentes implementadores.
- En la conversación: un mensaje final indicando que la implementación está completa y el siguiente paso (`/sdd-close`).

**CRITICAL — la estructura interna de `implementation/` y la del árbol de salida las define `<plantilla-activa>/README.md`, no este skill.** El skill **MUST NOT** asumir esos detalles de memoria; solo orquesta los subagentes que las producen.

**Contrato fijo (no lo cambia `--template-dir`):** la entrada es `design.md` (`type: design`) y la salida vive en `implementation/` (dentro de la iniciativa) y en el árbol del proyecto. Es lo que el skill usa para **localizar y validar** el diseño de entrada.

**Logs de orquestación del motor.** Además de la estructura que define la plantilla, el motor escribe en `implementation/` su propio **log** (no es contenido de la plantilla; los subagentes lo ignoran):

- `log_build.txt` — la salida **JSONL literal de cada verificador-build** de la Fase 5 (§10), una sección por iteración, para auditar qué errores encontró cada pasada.
- `log_erratas_diseno.txt` — la respuesta **literal** de cada editor-diseño (§9.2): qué erratas del diseño se corrigieron, en qué fichero y por qué. **Solo** se escribe si hubo alguna.
- `error_design.log` — explicación detallada de un **error de diseño irresoluble** detectado por un implementador o un corrector-build (§9.1). **Solo** se escribe en ese caso; su presencia indica que la implementación se detuvo porque el diseño está mal y hay que volver a `/sdd-designer`.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-kebab-case}/   ← carpeta de la iniciativa
        ├── design/                              ← entrada (la produce /sdd-designer)
        │   ├── design.md                        ← índice de entrada (type: design)
        │   └── …                                ← resto del diseño (lo define la plantilla del designer)
        ├── implementation_previa_<n>/           ← ejecución anterior apartada (§5)
        └── implementation/                      ← salida del descomponedor (Fase 2)
            ├── …                                ← tareas e índice (los define <plantilla-activa>/README.md)
            ├── log_build.txt                    ← log del motor: JSONL de cada verificador-build (§10)
            ├── log_erratas_diseno.txt           ← log del motor: erratas del diseño corregidas (§9.2)
            └── error_design.log                 ← log del motor: solo si hay un error de diseño irresoluble (§9.1)

src/main/…  (y src/test/… si la plantilla lo prevé)   ← código real (lo escriben los implementadores)
```

---

## 2. Principios

### 2.1 El diseño es la fuente de verdad

El diseño es la fuente de verdad — **MUST NOT** interpretar ni ampliar más allá de lo que dice. El código **MUST NOT** apartarse de él en silencio: si el diseño tiene una errata pequeña, se corrige **el diseño** (§2.4) y el código lo sigue corregido. Los subagentes leen `design.md` y todos los ficheros que el contrato indique. **MUST NOT** usar otros `design.md` de otras iniciativas de `.sdd/` como referencia.

### 2.2 El README es el contrato único

**Carpeta de plantillas activa (por frontmatter `template:`).** Este skill trae una carpeta `template-<nombre>/` por cada tipo de artefacto implementable; **no conoce sus nombres** (crear un tipo nuevo = crear su carpeta, sin tocar este skill). Cada plantilla define una arquitectura distinta y **MUST NOT** mezclarse. La activa se resuelve así, **antes** de cargar el contrato (Fase 1):

1. `--template-dir=<ruta>` explícito → esa carpeta (válvula de testing). **ERROR** si apunta a otra carpeta `template-*/` de este skill **distinta** de la que declara la iniciativa (mezcla de arquitecturas, STOP condition).
2. Sin flag → lee la clave `template:` del frontmatter del **`design.md` de entrada** (el diseño la hereda de la spec) y usa `template-<valor>/`; si el `design.md` no la trae (diseño anterior a este contrato), léela del `specification.md` de la carpeta de la iniciativa; si tampoco → pregúntala con `AskUserQuestion` (una opción por carpeta `template-*/` del skill). Si `template-<valor>/` no existe en este skill → **ERROR** indicando las disponibles o que se pase `--template-dir=`.
3. **Valor reservado `template: external`** (la iniciativa se especificó con un `--template-dir` externo): la plantilla activa **solo** puede venir de `--template-dir=`, apuntando a la carpeta externa que corresponda a **este** skill. Si el flag no viene → **ERROR** y detente pidiéndolo; **MUST NOT** preguntar la plantilla, **MUST NOT** caer a una carpeta interna (mezclaría arquitecturas en silencio) y **MUST NOT** buscarse `template-external/`. `external` no es una ruta ni una clave ausente: cada skill tiene su propia carpeta de plantillas, así que la ruta externa de un skill no designa nada en otro.

**En todo el resto del skill, `<plantilla-activa>/README.md` denota «el `README.md` de la carpeta de plantillas activa» resuelta aquí.**

Todo lo específico de la implementación (qué contiene el diseño, cómo se descompone en tareas, cómo se materializa cada tarea, cómo se compila y se corrige) lo define `<plantilla-activa>/README.md` y los ficheros que él referencie. Los subagentes los **leen de disco**; el skill **MUST NOT** asumirlos, restatarlos ni hardcodearlos aquí. El skill solo pasa a cada subagente **las rutas** de los ficheros de entrada y su rol.

**CRITICAL — `README.md` es el ÚNICO fichero de la plantilla que el motor conoce por nombre.** El skill **MUST NOT** nombrar, leer, resolver ni **ejecutar** ningún otro fichero de la plantilla (ni los documentos que el README referencie, ni ningún comando de compilación o validación que la plantilla traiga). Esos los descubren y usan **los subagentes** leyendo el `README.md`. En particular:

- Si la plantilla prescribe compilar o validar con un **comando o script** (p.ej. `./gradlew clean build`, `xmllint`), **lo ejecuta el subagente** (el verificador-build o el implementador) —que lee la plantilla y lo descubre—, **NUNCA el motor**.
- **MUST NOT** añadir "pasos de `Bash`" en este skill que compilen, copien XML al árbol o corran herramientas específicas de la implementación. El motor solo usa `Bash`/`Write` para orquestación **agnóstica** (listar la raíz de drafts, comprobar que existe `design/design.md`, apartar con `mv` el `implementation/` de una ejecución anterior —§5— y escribir sus propios **logs** `log_build.txt`, `log_erratas_diseno.txt` y `error_design.log` — §10, §9.2, §9.1), nunca para materializar ni compilar el código.
- Único acoplamiento permitido por nombre: `README.md` (contrato de la plantilla), el contrato fijo de entrada `design.md` (`type: design`) y el `specification.md` de la iniciativa (solo para resolver `template:` en §2.2 y para que el editor-diseño compruebe que una errata no cambia el comportamiento, §9.2).

**REQUIRED — el README de la plantilla es leído por los cuatro roles.** Este skill lanza **cuatro** subagentes con tareas distintas: **descomponedor** (escribe las tareas), **implementador** (materializa una tarea), **verificador-build** (compila y reporta) y **corrector-build** (corrige errores de compilación) — ver §2.3, §7, §9, §10. Los cuatro reciben las mismas rutas de entrada y **leen el mismo `README.md`**, pero cada uno hace una cosa distinta y necesita un subconjunto distinto de sus ficheros. Por tanto, **cualquier `README.md` de plantilla** (cualquier `template-<nombre>/` del skill o una externa apuntada con `--template-dir=`) **MUST** estar redactado teniendo en cuenta esos cuatro roles: debe delimitar, por rol, qué tarea hace y qué ficheros de la plantilla le aplican. Un README que solo contemple al descomponedor es **incompleto** para este skill.

### 2.3 Orquestación de subagentes

- El **descomponedor** corre **una vez** (§7). Los **implementadores** corren **de uno en uno y en orden** (§9): cada tarea puede depender del código de las anteriores. El **verificador-build** y el **corrector-build** corren **de uno en uno** dentro del bucle (§10). El **editor-diseño** corre solo, entre el subagente que encontró la errata y su relanzamiento (§9.2).
- **MUST NOT** lanzar subagentes en paralelo: la implementación es secuencial (las firmas Java dependen de los XML ya colocados; las tareas posteriores dependen de las previas).
- **MUST NOT** usar `run_in_background`: el skill necesita el resultado de cada subagente para continuar.
- **MUST NOT** invocar `developer-code-implementer` tú mismo: el motor solo lanza los subagentes de §2.3 con `Agent`; es el **implementador** (o el corrector-build) quien, dentro de su contexto, invoca `developer-code-implementer` si el contrato se lo indica.
- Cada rol responde con un **token literal** que el skill parsea (definidos en cada fase). El skill compara por literal exacto.
- No define su propio modo subagente (desviación deliberada de k-skill §6.10.4): cuando lo ejecuta `/sdd`, su ejecutor convierte las preguntas (`AskUserQuestion`) y paradas (`STOP`) de este skill en `DECISION-REQUERIDA`.

### 2.4 Erratas del diseño — se corrigen en el diseño, sin volver atrás

Al implementar puede salir algo pequeño del diseño que no se puede implementar tal cual o que no tiene sentido. Si es una **errata pequeña**, se corrige en el propio diseño y la implementación sigue; **MUST NOT** detener el skill ni mandar al usuario a `/sdd-designer`.

- **Errata pequeña** (se corrige aquí): un detalle del diseño que el framework o el stack no admiten tal cual, una incoherencia de nombres o tipos entre ficheros del diseño, una referencia a algo que el diseño define con otro nombre, un detalle que falta y se deduce sin ambigüedad del resto del diseño. Su corrección es **local** y **no cambia el comportamiento** que pide la especificación.
- **No es una errata** (`DESIGN-ERROR`, §9.1): añadir o quitar piezas con peso (entidades, pantallas, estados, reglas, servicios), cambiar el comportamiento que pide la spec, o rehacer una parte del diseño.
- **Prioridad sobre la plantilla**: cuando el contrato de la plantilla clasifica un caso como `DESIGN-ERROR`, el subagente **MUST** comprobar antes si es una errata pequeña; si lo es, devuelve `DESIGN-ERRATA`.
- **Quién la propone**: el implementador (§9) y el corrector-build (§10), con el token `DESIGN-ERRATA`. **MUST NOT** editar el diseño ellos.
- **Quién la aplica**: solo el **editor-diseño** (§9.2), que corrige `design/` y las copias del texto afectado en `implementation/`. El motor relanza después el subagente que la encontró.
- **LIMIT**: 5 erratas aplicadas por ejecución del skill. Más que eso ya no son erratas sueltas, sino un diseño mal hecho: la 6ª se trata como `DESIGN-ERROR`.

- ✅ CORRECTO: el diseño pide un atributo de vista que el framework no tiene y el mismo efecto se consigue con otro atributo → `DESIGN-ERRATA` y se corrige el fichero de vista del diseño.
- ✅ CORRECTO: un fichero del diseño llama a un campo con un nombre y el modelo del diseño lo define con otro → `DESIGN-ERRATA` para unificar el nombre.
- ❌ INCORRECTO: el implementador edita el fichero de `design/` y sigue (un solo rol aplica las erratas y deja constancia en `log_erratas_diseno.txt`)
- ❌ INCORRECTO: `DESIGN-ERRATA` para añadir una entidad que el diseño no tiene (no es una errata, es rediseñar: `DESIGN-ERROR`)

### 2.5 Hacer solo lo que pide la tarea

Cada subagente se limita al alcance exacto de su tarea: lo que no pide, aunque parezca conveniente, puede chocar con otro proceso en curso o con trabajo de otros sin commitear.

- **MUST NOT** tocar nada fuera de su tarea: ni refactorizar código vecino, ni «arreglar» lo que no es suyo, ni revertir cambios ajenos del árbol.
- Si cree que hace falta algo más, **MUST** añadir tras su token una línea `SUGERENCIA: <qué y por qué>` por cada cosa, sin hacerla.
- El motor acumula las líneas `SUGERENCIA:` y las muestra en el mensaje de cierre (§11). **MUST NOT** aplicarlas él ni relanzar a nadie por ellas: decide el usuario.

- ✅ CORRECTO: `DONE: task_03` + `SUGERENCIA: CorreoServiceImpl.insert repite la misma consulta que GrupoServiceImpl; podría extraerse.`
- ❌ INCORRECTO: el implementador de `task_03` extrae él mismo la consulta repetida a una clase común (fuera del alcance de su tarea)

---

## 3. Flujo general

```
┌────────────────────────────────────────────────────────────────────────┐
│  Fase 0  Localizar la iniciativa + design.md (+ confirmar ruta)        │
│  Fase 1  Validar type, cargar el contrato (README), resolver rutas     │
│            apartar implementation/ previa → implementation_previa_<n>  │
│  Fase 2  descomponedor(design) → implementation/ (tareas + índice)     │
│  Fase 3  Mostrar resumen de tareas (informativo, sin bloquear)         │
│  Fase 4  Para cada tarea en orden:                                     │
│            implementador(tarea) → DONE → siguiente tarea               │
│              └ CONFLICT | BLOCKED → STOP + preguntar al usuario        │
│              └ DESIGN-ERROR → error_design.log + STOP                  │
│              └ DESIGN-ERRATA → editor-diseño → relanzar implementador  │
│  Fase 5  Bucle (LIMIT 20), vuelca cada respuesta a log_build.txt:      │
│            verificador-build()                                         │
│              └ OK-COMPILA → Fase 6                                     │
│              └ BLOCKED → STOP + preguntar al usuario                   │
│              └ JSONL → corrector-build(errores)                        │
│                  └ CORREGIDO → repetir                                 │
│                  └ BLOCKED → STOP + preguntar al usuario               │
│                  └ DESIGN-ERROR → error_design.log + STOP              │
│                  └ DESIGN-ERRATA → editor-diseño → relanzar            │
│            LIMIT agotado | mismos errores → STOP + AskUserQuestion     │
│  Fase 6  Mensaje de cierre al usuario (handoff a /sdd-close)           │
└────────────────────────────────────────────────────────────────────────┘
```

Las fases se ejecutan **estrictamente en orden**: la Fase 4 no empieza hasta que el descomponedor ha escrito todas las tareas (Fase 2) y se ha mostrado el resumen (Fase 3). **CRITICAL — salvo la confirmación inicial de qué diseño implementar (Fase 0), el flujo no pide aprobación**: la lista de tareas (Fase 3) no se aprueba, se implementa automáticamente. El skill se detiene:

- **A preguntar**: en esa confirmación inicial (Fase 0), si ni el `design.md` ni el `specification.md` declaran `template:` (Fase 1, §2.2), ante un `CONFLICT` o `BLOCKED` (Fase 4, §9) y ante un `BLOCKED` del verificador-build o del corrector-build o un build que no compila tras el **LIMIT** (Fase 5, §10).
- **Sin preguntar**: ante un `ERROR` de configuración/entrada (Fase 0, Fase 1) y ante un `DESIGN-ERROR` (Fase 4 o Fase 5), en cuyo caso escribe `implementation/error_design.log` y termina (§9.1).

---

## 4. Fase 0 — Localizar la iniciativa

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca con una ruta a un `design.md`:

1. Comprueba que el fichero existe y está dentro de `{raíz de drafts}/{iniciativa}/design/` (la raíz de drafts es `.sdd/drafts/` o la de `--root=`); si no → **ERROR** y detente.
2. La **carpeta de la iniciativa** es la que contiene la subcarpeta `design/`.
3. Pasa a la Fase 1 con esa ruta.

### 4.2 Caso 2 — Sin ruta (auto-detección)

1. Listar las subcarpetas de la raíz de drafts (`.sdd/drafts/` o la de `--root=`) cuyo nombre cumple `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`:
   ```bash
   ls -d {raíz de drafts}/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordenar alfabéticamente (el prefijo timestamp = orden cronológico) y tomar la **última** que contenga `design/design.md`.
3. Si no hay ninguna con ese formato y un `design/design.md`, indica que no hay diseños disponibles y pide una ruta. Detente.
4. **Mostrar al usuario la ruta detectada** y preguntar con `AskUserQuestion` si quiere usar ese diseño:
   - Sí → continuar con la Fase 1.
   - No → pedir la ruta del diseño a implementar. Detente.

**MUST NOT**:

- **MUST NOT** elegir una iniciativa que no sea la última por orden alfabético del prefijo timestamp.
- **MUST NOT** usar `mtime` o cualquier criterio distinto del orden alfabético del timestamp.
- **MUST NOT** continuar sin confirmación del usuario tras mostrar la ruta detectada.

---

## 5. Fase 1 — Cargar el contrato y validar el diseño

1. **Validar el frontmatter del diseño** (antes de resolver la plantilla). Lee el `design.md`. Debe comenzar con un bloque `---` … `---` que contenga la línea `type: design` (puede haber más campos). Si no lo contiene, **STOP** y muestra este **ERROR**, sin continuar:

   > Error: el fichero `{ruta}` no es un diseño válido. Debe contener en el frontmatter:
   > ```
   > ---
   > type: design
   > ---
   > ```
   > Para generar un diseño, usa `/sdd-designer`.
2. **REQUIRED — lee con `Read` la guía `<plantilla-activa>/README.md`** (resuelta según §2.2: el frontmatter `template:` del `design.md` de entrada, o `--template-dir`): confirma que existe (si no → **ERROR**, STOP condition) y entiende, a alto nivel, qué rol pide a cada subagente. **No** necesitas memorizar su contenido: los subagentes la leerán de disco. Es el **único fichero que el skill conoce por nombre**; el README referencia los demás ficheros de la plantilla, que los subagentes seguirán.
3. **Resolver las rutas de entrada** que se pasarán a los subagentes (no su contenido):
   - la ruta de la guía `<plantilla-activa>/README.md` (las **reglas para la implementación**),
   - la ruta de `design.md` (el **diseño**) y, por extensión, su carpeta `design/`.
4. **Apartar la ejecución anterior**: si existe `{iniciativa}/implementation/`, muévela con `mv` a `{iniciativa}/implementation_previa_<n>/` (`<n>` = el primer entero libre empezando en 1) y díselo al usuario. **MUST NOT** borrar nada: así no quedan un `error_design.log` viejo, logs mezclados ni tareas antiguas.

No hay más preparación: el skill no carga skills técnicos ni explora el código — eso lo hace cada subagente leyendo el README (que indica qué contexto cargar).

---

## 6. Fases con subagentes — patrón común del prompt

Lanza **todos** los subagentes de este skill (los cuatro roles y el editor-diseño) con `subagent_type: claude`. Cada prompt de los cuatro roles (§7, §9, §10 — no el del editor-diseño de §9.2, que tiene el suyo propio) **MUST** pasar, además de su tarea específica:

- **Reglas para la implementación**: `lee {ruta de <plantilla-activa>/README.md} y todos los ficheros que referencie. Es el contrato: define qué hacer, cómo y con qué estructura. Síguelo al pie de la letra.`
- **Diseño**: `lee la carpeta {iniciativa}/design (sobre todo design.md) y los ficheros que el contrato indique.`
- **Alcance**: `haz solo lo que pide tu tarea. Si crees que hace falta algo más, no lo hagas: añade tras tu token una línea "SUGERENCIA: <qué y por qué>" por cada cosa.` (§2.5)
- **MUST NOT** usar `AskUserQuestion`: el implementador, el verificador-build y el corrector-build reportan la duda que no puedan resolver con el token de bloqueo de su rol (el motor lleva la decisión al usuario); el descomponedor resuelve la ambigüedad y la documenta en la tarea (§7).

---

## 7. Fase 2 — Descomponer (subagente descomponedor)

**Lanza un subagente descomponedor** (uno solo). Su tarea: leer el diseño y **escribir la lista de tareas** en `{iniciativa}/implementation/` siguiendo el contrato (qué tareas crear, cómo agruparlas, qué skills lleva cada una, qué texto del diseño copia, y la propagación de los ficheros de contrato hacia abajo). **MUST NOT** implementar nada todavía: solo escribir los ficheros de tarea.

**Prompt del subagente descomponedor**:

> Eres un experto arquitecto en Java y el framework Axelor, que tienes que **descomponer un diseño en una lista de tareas de implementación** siguiendo unas reglas.
>
> - **Reglas para la descomposición**: lee `{ruta de <plantilla-activa>/README.md}` y **todos los ficheros que referencie** —en particular el contrato de **descomposición**—. Define qué tareas crear, cómo agrupar los ficheros, qué skills lleva cada tarea, qué texto del diseño copiar **verbatim** en cada una, la propagación de los ficheros de contrato hacia abajo, las plantillas exactas de cada fichero a escribir y el checklist. Síguelo al pie de la letra.
> - **Diseño**: lee la carpeta `{iniciativa}/design` —sobre todo `design.md` y los ficheros que el contrato indique (XML materializados, descripciones de tests, reglas complejas, tests E2E)—.
> - **Salida**: escribe en `{iniciativa}/implementation/` la lista de tareas, su índice y los ficheros propagados, con la estructura exacta que define el contrato. **MUST NOT** materializar código en `src/...` (eso es de los implementadores).
> - **MUST NOT** usar `AskUserQuestion`. Ante una ambigüedad, toma la decisión más razonable y documéntala dentro de la propia tarea.
> - Aplica el **checklist** del contrato antes de terminar (**LIMIT**: 3 iteraciones de autocorrección).
> - Al terminar, responde con este formato **exacto**:
>   - Primera línea: **exactamente** `ESCRITO: implementation/`.
>   - Una línea **exactamente** `=== TAREAS ===` y, debajo, **una línea por tarea** en el orden de implementación, con el formato `{ruta-relativa-de-la-tarea} | {título} | {ficheros que cubre}` (p.ej. `task_02.md | Servicio Bar | service/BarService.java (+impl, DTO)`).
>   - **MUST NOT** pegar el contenido de las tareas en la respuesta (ya está en disco).

El skill parsea la primera línea `ESCRITO: implementation/` y, tras `=== TAREAS ===`, **la lista ordenada de tareas**: usa la primera columna (la ruta) para iterar en la Fase 4 (en ese mismo orden) y el resto para el resumen de la Fase 3. Si el token no aparece o no hay ninguna línea de tarea, **reintenta 1 vez**; si vuelve a fallar → **STOP** (STOP condition).

- ✅ CORRECTO (respuesta del descomponedor): `ESCRITO: implementation/` + `=== TAREAS ===` + `task_01.md | Dominio Bar | domains/Bar.xml`
- ❌ INCORRECTO: `He generado 5 tareas` (token no parseable), pegar el contenido de las `task_NN.md` en la respuesta (gasta contexto, ya está en disco), materializar código en `src/...` (eso es de la Fase 4)

---

## 8. Fase 3 — Mostrar el resumen de tareas (informativo)

1. Muestra al usuario un **resumen**: número de tareas generadas y, por cada una, su título y los ficheros que cubre (a partir de la lista `=== TAREAS ===` del descomponedor). Indícale la ruta de `{iniciativa}/implementation/` por si quiere consultar las tareas.
2. **Continúa automáticamente** a la Fase 4. El resumen es **solo informativo**: si el descomponedor terminó bien (token `ESCRITO: implementation/`), **MUST NOT** pedir aprobación ni detener el flujo.
3. **MUST NOT** usar `AskUserQuestion` en esta fase. El flujo solo se interrumpe más adelante ante una **excepción** real: `CONFLICT`/`BLOCKED` en la Fase 4 (§9), `BLOCKED` o un build que no compila tras el **LIMIT** en la Fase 5 (§10), o un `DESIGN-ERROR` (incluido el rechazo del editor-diseño) en cualquiera de las dos (§9.1).

---

## 9. Fase 4 — Implementar las tareas (un subagente por tarea)

Recorre la lista ordenada de tareas (la que devolvió el descomponedor en `=== TAREAS ===`). Para **cada** tarea, **en orden**, lanza **un subagente implementador**. **MUST NOT** lanzarlos en paralelo ni con `run_in_background`: necesitas el resultado de cada uno antes de pasar al siguiente.

**Prompt del subagente implementador** (mismo para todas las tareas salvo la ruta de la tarea):

> Eres un experto arquitecto en Java y el framework Axelor, que tienes que **materializar una tarea de implementación** en el árbol del proyecto siguiendo unas reglas.
>
> - **Reglas para la implementación**: lee `{ruta de <plantilla-activa>/README.md}` y **todos los ficheros que referencie** —en particular el contrato de **materialización** (cómo colocar los XML ya materializados, cómo fusionar/validar, y cómo delegar el código Java y, si la plantilla lo prevé, sus tests en `developer-code-implementer` cargando antes los skills de la tarea)—. Síguelo al pie de la letra.
> - **Diseño**: la carpeta `{iniciativa}/design` (los XML materializados de los que dependa esta tarea son **contrato fijo**: si el contrato manda colocarlos, se copian/fusionan **tal cual**, **NO** se regeneran).
> - **Tarea a implementar**: `{ruta de la tarea, p.ej. {iniciativa}/implementation/task_03.md}`. Léela entera (skills a usar + texto del diseño verbatim) y materialízala según el contrato.
> - **OBLIGATORIO**: si la tarea lista skills en su sección de skills, **cárgalos con la herramienta `Skill` antes de implementar nada** y, si el contrato lo indica, **invoca `developer-code-implementer`** pasándole el texto de la tarea **verbatim**. **MUST NOT** empezar a implementar sin haber cargado esos skills.
> - **MUST NOT** usar `AskUserQuestion`. Ante un bloqueo **MUST NOT** adivinar: repórtalo con el token que corresponda según su **origen**. Si el problema está en el **diseño** —un XML materializado del diseño mal formado o inconsistente, el diseño referencia una entidad/campo/acción que él mismo no define, dos reglas del diseño se contradicen, o falta información imprescindible que el diseño debería aportar— es un `DESIGN-ERROR`: **MUST NOT** editar el diseño para forzar que cuadre. Pero si es una **errata pequeña** del diseño (§2.4: corrección local que no cambia el comportamiento que pide la spec), devuelve `DESIGN-ERRATA` en vez de `DESIGN-ERROR`, aunque el contrato de la plantilla lo llame `DESIGN-ERROR`. Si el problema es del **entorno** —dependencia externa inexistente, recurso no disponible, instrucción ambigua que no es culpa del diseño— es un `BLOCKED`.
> - Al terminar, responde con **exactamente uno** de estos tokens en la primera línea, y 1-2 líneas de resumen:
>   - `DONE: {ruta de la tarea}` — la tarea quedó materializada en el árbol del proyecto.
>   - `CONFLICT: {ruta de la tarea} — {qué fichero o elemento destino ya existe}` — hay un conflicto al sobrescribir que requiere decisión del usuario.
>   - `BLOCKED: {ruta de la tarea} — {motivo}` — un bloqueo del entorno que impide continuar (no es culpa del diseño).
>   - `DESIGN-ERROR: {ruta de la tarea} — {motivo detallado}` — el diseño está mal y no se puede implementar sin volver a `/sdd-designer` (§9.1). Da el **máximo detalle**: qué fichero del diseño, qué es inconsistente o falta, y por qué no se puede resolver escribiendo código.
>   - `DESIGN-ERRATA: {ruta de la tarea}` — el diseño tiene una errata pequeña (§2.4). Debajo, una línea por errata con el formato exacto `ERRATA: <fichero-del-diseño> § <ubicación>: dice «…»; debe decir «…». Motivo: <por qué no se puede implementar tal cual>`. **MUST NOT** editar el diseño.
>   - **MUST NOT** pegar el código implementado en la respuesta (ya está en disco).

El skill parsea la primera línea:

- `DONE: …` → pasa a la siguiente tarea.
- `CONFLICT: …` → **STOP** y `AskUserQuestion` (**Sobrescribir** / **Mantener** / **Abortar**). Según la respuesta, **relanza el implementador** de esa misma tarea indicándole la decisión (sobrescribir o saltar el fichero en conflicto), o aborta. **MUST NOT** sobrescribir sin preguntar.
- `BLOCKED: …` → **STOP** y muestra el motivo al usuario; pregunta cómo proceder (STOP condition). **MUST NOT** continuar con las tareas siguientes.
- `DESIGN-ERROR: …` → el motor **escribe `implementation/error_design.log`** con el motivo detallado y **detiene el skill** (§9.1). **MUST NOT** continuar con las tareas siguientes, **MUST NOT** preguntar al usuario y **MUST NOT** pasar a la Fase 5.
- `DESIGN-ERRATA: …` → ejecuta §9.2 con sus líneas `ERRATA:` y, si se aplicaron, **relanza el implementador de esa misma tarea**.
- Si la primera línea no es ninguno de esos tokens → **reintenta 1 vez** el implementador de esa tarea; si vuelve a fallar, trátalo como `BLOCKED`.

Implementa las tareas en orden. **MUST NOT** pasar a la Fase 5 hasta que todas las tareas estén materializadas (o se haya detenido por un conflicto/bloqueo/error de diseño no resuelto).

- ✅ CORRECTO (respuesta del implementador): `DONE: task_03.md` + 1 línea de resumen
- ✅ CORRECTO (errata del diseño): `DESIGN-ERRATA: task_03.md` + `ERRATA: <fichero-del-diseño> § <ubicación>: dice «<nombre A>»; debe decir «<nombre B>». Motivo: el modelo del diseño define el campo como <nombre B>`
- ✅ CORRECTO (error de diseño): `DESIGN-ERROR: task_03.md — domains/Bar.xml referencia el campo centro que el diseño no define en ninguna entidad` + 1 línea de resumen
- ❌ INCORRECTO: `Tarea hecha` (token no parseable), pegar el `.java` generado en la respuesta, sobrescribir un fichero existente sin devolver `CONFLICT`, devolver `BLOCKED` cuando el problema es del diseño (debe ser `DESIGN-ERROR`)

### 9.1 Error de diseño irresoluble (`DESIGN-ERROR` → `error_design.log`)

Un **error de diseño** es un problema cuyo origen está en el propio diseño (`design/`) y que **NO se puede resolver implementando ni corrigiendo código**: el diseño está mal, **no es una errata pequeña** (§2.4) y hay que volver a `/sdd-designer`. Ejemplos: un XML materializado del diseño mal formado o inconsistente, el diseño referencia una entidad/campo/acción que él mismo no define, dos reglas del diseño se contradicen, o falta información imprescindible que el diseño debería aportar.

- Tanto el **implementador** (§9) como el **corrector-build** (§10) lo señalan con el token `DESIGN-ERROR: {motivo detallado}` en la primera línea, **en vez** de `BLOCKED` o de seguir corrigiendo. **MUST NOT** editar el diseño para forzar que cuadre.
- Cuando el motor recibe `DESIGN-ERROR: …` (de cualquiera de los dos roles, en Fase 4 o Fase 5), **MUST**:
  1. Escribir con `Write` el fichero `{iniciativa}/implementation/error_design.log` con la plantilla de abajo, volcando **verbatim** el motivo detallado del token.
  2. **Detener el skill inmediatamente** (**STOP**): **MUST NOT** preguntar al usuario, **MUST NOT** relanzar ningún subagente, **MUST NOT** continuar con tareas siguientes ni pasar a la Fase 5/6.
  3. Mostrar al usuario la ruta de `error_design.log` y avisar de que el diseño tiene un error: hay que corregirlo con `/sdd-designer` y relanzar `/sdd-implementer`. **MUST NOT** lanzar `/sdd-designer` tú mismo.

**Plantilla de `error_design.log`** (la escribe el motor, literal; rellena los `{…}`):

```
# Error de diseño — implementación detenida

Iniciativa: {carpeta-iniciativa}
Detectado por: {implementador {ruta-tarea} | corrector-build iteración {k}}
Fase: {Fase 4 — implementar | Fase 5 — build}

## Problema
{motivo detallado del token DESIGN-ERROR, verbatim}

## Por qué no se puede resolver implementando
{por qué es un fallo del diseño y no del código: el diseño es contrato fijo y corregir el código no lo arregla}

## Acción requerida
Corregir el diseño con /sdd-designer y volver a lanzar /sdd-implementer.
```

### 9.2 Erratas del diseño (`DESIGN-ERRATA` → editor-diseño)

Cuando un implementador (§9) o un corrector-build (§10) devuelve `DESIGN-ERRATA` (§2.4), el motor **MUST**:

1. Comprobar el **LIMIT** de §2.4: si con estas erratas se pasaría de 5 aplicadas en esta ejecución → trátalo como `DESIGN-ERROR` (§9.1) con las líneas `ERRATA:` como motivo.
2. **Lanzar el subagente editor-diseño** (uno solo) con las líneas `ERRATA:` literales.
3. **Añadir (append)** su respuesta literal a `implementation/log_erratas_diseno.txt`, bajo la cabecera `# Erratas — {tarea | corrector-build iteración {k}}`, y **mostrarla al usuario**.
4. Según su respuesta:
   - Alguna línea `RECHAZADO:` → `DESIGN-ERROR` (§9.1), con esas líneas como motivo.
   - Empieza por `APLICADO: <n>` sin rechazos → **relanza el subagente que la encontró**: el implementador de la misma tarea, o el corrector-build con el mismo JSONL de errores. Añade a su prompt: «El diseño se ha corregido por erratas (ver `implementation/log_erratas_diseno.txt`). Lo que el árbol ya tenga copiado tal cual de los ficheros corregidos del diseño **MUST** volver a cuadrar con ellos».
   - Ni lo uno ni lo otro → **reintenta 1 vez**; si vuelve a fallar → `DESIGN-ERROR` (§9.1).

El código de tareas anteriores que use lo corregido lo arregla después el bucle de build (Fase 5): el editor-diseño **MUST NOT** tocar el árbol del proyecto.

**Prompt del subagente editor-diseño**:

> Eres el editor de un diseño de software. Al implementarlo se han encontrado erratas y tienes que corregirlas **en el propio diseño**, con el cambio mínimo.
>
> - **Diseño**: la carpeta `{iniciativa}/design` (sobre todo `design.md`).
> - **Especificación**: `{iniciativa}/specification.md` y los ficheros que enlace, para comprobar que no cambias el comportamiento que pide.
> - **Erratas**: `{líneas ERRATA: literales}`.
> - Por **cada** errata:
>   1. Comprueba que es una **errata pequeña**: corrección local que no cambia el comportamiento que pide la spec ni añade o quita piezas con peso (entidades, pantallas, estados, reglas, servicios).
>   2. Si lo es: corrige **en sitio** (`Edit`) el fichero del diseño indicado, respetando su formato. Arrastra el cambio a todo fichero de `design/` que nombre lo mismo. Busca después en `{iniciativa}/implementation/` todas las copias del texto corregido (las tareas copian el diseño literal) y actualízalas igual.
>   3. Si no lo es: **MUST NOT** tocar nada.
> - **CRITICAL — confinamiento de escritura**: solo puedes editar ficheros de `{iniciativa}/design/` y de `{iniciativa}/implementation/` (salvo sus `log_*.txt` y `error_design.log`). **MUST NOT** tocar `src/**` ni nada del proyecto.
> - **MUST NOT** usar `AskUserQuestion`.
> - **Formato de salida (REQUIRED)**: primera línea **exactamente** `APLICADO: <n>`; debajo, una línea por errata aplicada `<fichero-del-diseño> § <ubicación>: <cambio en una frase> (+ <n> copias en implementation/)`; y por cada errata que no sea pequeña, una línea **exactamente** `RECHAZADO: <fichero-del-diseño> § <ubicación> — <por qué no es una errata pequeña>`. **MUST NOT** pegar el diseño en la respuesta.

- ✅ CORRECTO (respuesta del editor-diseño): `APLICADO: 1` + `<fichero-del-diseño> § <ubicación>: el campo pasa a llamarse <nombre B> (+ 2 copias en implementation/)`
- ✅ CORRECTO (con un rechazo): `APLICADO: 0` + `RECHAZADO: design.md § <sección> — exige un servicio nuevo: es rediseñar, no una errata`
- ❌ INCORRECTO: `He corregido el diseño` (token no exacto; el motor no sabe si rechazó alguna)
- ❌ INCORRECTO: corregir `design/` sin actualizar las tareas de `implementation/` que copian ese texto (los implementadores siguientes leerían la versión vieja)

---

## 10. Fase 5 — Verificar y corregir el build (bucle, LIMIT 20)

Tras materializar todas las tareas, verifica que el proyecto compila (y que sus tests de compilación pasan) y entra en un bucle de auto-corrección si falla. Repite este bucle **como máximo 20 veces** (**LIMIT**: 20 iteraciones); lleva un contador de iteración `{k}` empezando en 1, y guarda el JSONL de errores de la iteración previa para detectar fallos persistentes.

1. **Lanzar el subagente verificador-build** (uno solo).
2. **Volcar su respuesta a `implementation/log_build.txt`**: añade (append) la respuesta **literal** —sus líneas JSONL, `OK-COMPILA` o `BLOCKED: …`— precedida de la cabecera `# Build — iteración {k}`. Es un append acumulativo (una sección por iteración).
3. Si respondió:
   - **Exactamente** `OK-COMPILA` → el proyecto compila: sal del bucle y ve a la Fase 6.
   - `BLOCKED: …` → **STOP**, muestra el motivo al usuario y pregunta cómo proceder (como con el `BLOCKED` del corrector-build). **MUST NOT** incrementar `{k}` ni volver al paso 1 sin su respuesta.
   - Ni lo uno ni lo otro, y alguna línea no es un objeto JSON válido con los campos del formato → **reintenta 1 vez** el verificador-build (sin incrementar `{k}`); si vuelve a fallar, trátalo como `BLOCKED`.
4. Si respondió líneas JSONL válidas (los errores): **MUST** mostrar al usuario por pantalla, tal cual, las líneas JSONL (bloque ` ```jsonl `), antes de continuar.
   - **Detectar fallos persistentes**: si el conjunto de pares (`fichero`, `mensaje`) del JSONL es **igual** al de la iteración anterior (sin contar `id`, `tarea` ni `correccion`), el corrector no está progresando. Trata el caso como `k == 20` (paso 5).
   - Si `k < 20`: **lanza el subagente corrector-build** pasándole esas mismas líneas JSONL, para que corrija **en sitio**.
     - Si el corrector-build responde en su primera línea `DESIGN-ERROR: …` → el error solo se puede resolver cambiando el diseño: el motor escribe `implementation/error_design.log` y **detiene el skill** (§9.1). **MUST NOT** incrementar `{k}` ni volver al paso 1.
     - Si responde `DESIGN-ERRATA: …` → ejecuta §9.2; si las erratas se aplicaron, el motor relanza el corrector-build con el mismo JSONL y trata su respuesta con estas mismas viñetas del paso 4.
     - Si responde `BLOCKED: …` → **STOP**, muestra el motivo al usuario y pregunta cómo proceder (como en §9). **MUST NOT** incrementar `{k}` ni volver al paso 1 sin su respuesta.
     - Si responde `CORREGIDO` → incrementa `{k}` y vuelve al paso 1.
     - Si la primera línea no es ninguno de esos tokens → **reintenta 1 vez** el corrector-build; si vuelve a fallar, trátalo como `BLOCKED`.
5. Si tras la 20ª iteración el verificador-build sigue sin responder `OK-COMPILA` (o se detectaron fallos persistentes) → **STOP** y `AskUserQuestion` con estas opciones; actúa según la respuesta:
   1. **Dejar el reporte** (`log_build.txt`) para investigación manual → Fase 6 con el aviso **CRITICAL** de §11.
   2. **Revisar el diseño** → **STOP** indicando al usuario que lo corrija con `/sdd-designer` y relance `/sdd-implementer`. **MUST NOT** lanzar `/sdd-designer` tú mismo (§9.1).
   3. **Continuar sin compilación limpia** (no recomendado) → Fase 6 con el aviso **CRITICAL** de §11.

   **MUST NOT** dar la implementación por buena.

**Prompt del subagente verificador-build**:

> Eres un experto en build de proyectos Java/Gradle sobre el framework Axelor, que tienes que **compilar el proyecto y reportar el resultado**.
>
> - **Reglas para el build**: lee `{ruta de <plantilla-activa>/README.md}` y **los ficheros que referencie** —en particular el contrato de **build**: con qué comando se compila, qué cuenta como éxito y cómo reportar los errores—. **Ejecuta tú mismo** el comando de compilación que la plantilla prescriba.
> - **Diseño**: lee la carpeta `{iniciativa}/design` (sobre todo `design.md`) y los ficheros que el contrato indique.
> - **MUST NOT** corregir nada: solo **compilas y reportas**.
> - **MUST NOT** usar `AskUserQuestion`: si no puedes ejecutar la compilación (comando inexistente, entorno no disponible), usa el token `BLOCKED`. **MUST NOT** inventar un JSONL.
>
> **Formato de salida (REQUIRED)**:
> - Si la compilación **pasa**, responde **exactamente** y solo: `OK-COMPILA`.
> - Si **no puedes compilar**, responde una sola línea `BLOCKED: {motivo}`.
> - Si **falla**, responde **únicamente** con líneas **JSONL** (JSON Lines): **un error por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:
>   - `id` — identificador correlativo, formato `E-NNN` (`E-001`, `E-002`, …).
>   - `tipo` — la categoría del fallo: **exactamente** uno de los valores que define el contrato de build de la plantilla (p.ej. `COMPILE`).
>   - `fichero` — ruta del fichero afectado (p.ej. `src/main/java/com/educaflow/.../BarServiceImpl.java`), o `null`.
>   - `ubicacion` — línea/método/test concreto; `null` si no aplica.
>   - `tarea` — la tarea de `implementation/` de la que probablemente proviene el error (p.ej. `task_03.md`), o `null`.
>   - `mensaje` — el mensaje **literal** del compilador o del test que falla.
>   - `correccion` — qué hay que cambiar para resolverlo.
> - Cada línea **MUST** ser JSON válido en una sola línea (escapa los saltos como `\n`). **MUST NOT** añadir comentarios ni texto fuera de las líneas JSONL.
>
> Ejemplo de salida con errores:
>
> ```jsonl
> {"id":"E-001","tipo":"COMPILE","fichero":"src/main/java/com/educaflow/system/bar/service/BarServiceImpl.java","ubicacion":"línea 42","tarea":"task_03.md","mensaje":"cannot find symbol: method getNombre()","correccion":"Usar getName() según el dominio Bar.xml ya materializado, o añadir el campo nombre."}
> ```

**Prompt del subagente corrector-build**:

> Eres un experto arquitecto en Java y el framework Axelor, que tienes que **corregir los errores de compilación** reportados. Deberás indicar de la forma más clara posible los errores que has corregido.
>
> - **Reglas para el build y la implementación**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie (en particular qué puedes y qué **no** puedes tocar al corregir).
> - **Diseño**: la carpeta `{iniciativa}/design` —los XML materializados son **contrato fijo**: **MUST NOT** editarlos para que cuadre el Java; corrige el Java para que cuadre con ellos. Si el error **solo** se puede resolver cambiando el diseño (un XML del diseño está mal o es inconsistente, el diseño referencia algo que él mismo no define, reglas contradictorias…), **MUST NOT** editar el diseño ni adivinar: responde en la **primera línea** con `DESIGN-ERROR: {motivo detallado}` (qué fichero del diseño, qué es inconsistente y por qué no se puede arreglar con código) y termina (§9.1). Pero si es una **errata pequeña** del diseño (§2.4: corrección local que no cambia el comportamiento que pide la spec), responde en la **primera línea** `DESIGN-ERRATA: corrector-build` y debajo una línea `ERRATA: <fichero-del-diseño> § <ubicación>: dice «…»; debe decir «…». Motivo: <…>` por errata, aunque el contrato de la plantilla lo llame `DESIGN-ERROR`. En ambos casos **MUST NOT** editar el diseño.
> - **Errores a corregir** (los reportó el verificador-build, en JSONL, un error por línea): `{líneas JSONL literales del verificador-build}`. Resuelve cada línea; si el contrato lo indica, delega la corrección del código Java en `developer-code-implementer` cargando antes los skills de dominio aplicables.
> - **MUST NOT** usar `AskUserQuestion`: ante un bloqueo del entorno usa el token `BLOCKED`; ante un error del diseño, `DESIGN-ERROR` (arriba).
> - Al terminar, responde con **exactamente uno** de estos tokens en la primera línea:
>   - `CORREGIDO` — y debajo, una línea por error corregido (`E-NNN: <qué cambiaste>`).
>   - `BLOCKED: {motivo}` — un bloqueo del entorno (dependencia externa inexistente, recurso no disponible) que impide corregir; no es culpa del diseño.
>   - `DESIGN-ERROR: {motivo detallado}` — ver arriba.
>   - `DESIGN-ERRATA: corrector-build` — ver arriba.
>   - **MUST NOT** pegar el código corregido en la respuesta (ya está en disco).

- ✅ CORRECTO (respuesta del verificador-build cuando compila): `OK-COMPILA`
- ✅ CON ERRORES (una línea JSONL por error, sin texto alrededor): `{"id":"E-001","tipo":"COMPILE","fichero":"…","ubicacion":"…","tarea":"task_03.md","mensaje":"…","correccion":"…"}`
- ✅ CORRECTO (verificador-build que no puede compilar): `BLOCKED: el comando de compilación que prescribe la plantilla no existe en el proyecto`
- ✅ CORRECTO (corrector-build tras corregir): `CORREGIDO` + `E-001: BarServiceImpl usa getName() como define Bar.xml`
- ✅ CORRECTO (corrector-build ante un error de diseño): `DESIGN-ERROR: el servicio debe llamar a Bar.getCentro() pero el dominio Bar.xml del diseño no define el campo centro; no se puede arreglar en el Java`
- ✅ CORRECTO (corrector-build ante un bloqueo del entorno): `BLOCKED: la dependencia que pide la plantilla no está en el repositorio de artefactos`
- ❌ INCORRECTO: `Compila bien ✅` (token no exacto; el skill compara por literal), devolver los errores como prosa/array JSON en vez de una línea JSONL por error, o que el corrector-build **edite un XML del diseño** en vez de devolver `DESIGN-ERROR`
- ❌ INCORRECTO: `No he podido corregirlo porque falta una librería` (bloqueo sin token `BLOCKED`: el motor no lo lleva al usuario)

---

## 11. Fase 6 — Mensaje de cierre al usuario

```
Implementación completada en {raíz de drafts}/{carpeta-iniciativa}/

  - implementation/ : tareas e índice (+ ficheros de contrato propagados según la plantilla)
  - código real escrito en el árbol del proyecto (src/main/... y, si la plantilla lo prevé, src/test/...)

Tareas generadas e implementadas: N.
Build: {OK-COMPILA tras M iteración(es) | NO limpio tras M iteración(es) — ver implementation/log_build.txt}.
Erratas del diseño: {ninguna | {E} corregida(s) en design/, ver implementation/log_erratas_diseno.txt}.
Sugerencias no aplicadas: {ninguna | una línea por cada SUGERENCIA de los subagentes (§2.5)}.

Los artefactos del draft se mantienen en {raíz de drafts}/{carpeta-iniciativa}/ — no se ha archivado nada en .sdd/archive/.
Si la plantilla propagó tests E2E a implementation/, puedes ejecutarlos contra la aplicación real con /sdd-debug-with-test-e2e-desc.

Para cerrar la iniciativa (archivar el draft verbatim en .sdd/archive/) ejecuta:
  /sdd-close
```

Ajusta la lista de ficheros a la estructura real que define la plantilla.

**CRITICAL**: si la Fase 5 acabó sin compilación limpia (bug irresoluble o elección del usuario), **MUST** decirlo explícitamente:

> Atención: el proyecto no compila limpio tras {M} iteración(es). Revisa `implementation/log_build.txt` antes de lanzar `/sdd-close`, o relanza este skill tras corregir el diseño.

**MUST NOT** lanzar `/sdd-close` tú mismo: el usuario decide cuándo.

---

## Quick Guidelines

- **CRITICAL — agnosticismo**: este SKILL es un **motor de flujo**; **no sabe nada de cómo se descompone ni se materializa el diseño**. Todo lo específico lo define `<plantilla-activa>/README.md` (configurable con `--template-dir`), que **leen los subagentes** de disco. **MUST NOT** nombrar aquí ficheros, plantillas, comandos ni rutas de la implementación. Único contrato fijo: entrada `design.md` (`type: design`); salida en `implementation/` y en el árbol del proyecto.
- **Localizar** (§4): ruta explícita, o auto-detectar la **última** iniciativa de `{raíz de drafts}` con `design/design.md` y **confirmar** con `AskUserQuestion`. **MUST NOT** usar `mtime`.
- **Validar y cargar** (§5): primero valida `type: design` en el frontmatter (si no → **ERROR**); luego lee solo `<plantilla-activa>/README.md`; si ya hay un `implementation/`, apártalo con `mv` a `implementation_previa_<n>/` sin borrar nada.
- **Descomponer** (§7): **un** subagente descomponedor escribe `implementation/`; responde `ESCRITO: implementation/` + bloque `=== TAREAS ===` (una línea por tarea, en orden). **MUST NOT** materializar código.
- **Informar** (§8): muestra el resumen de tareas y **continúa automáticamente**; si todo va bien **MUST NOT** pedir aprobación. Solo se interrumpe ante excepciones (`CONFLICT`/`BLOCKED`, `DESIGN-ERROR`, build que no compila).
- **Implementar** (§9): **un subagente implementador por tarea, en orden** (nunca en paralelo ni `run_in_background`). Cada uno carga los skills de su tarea y, si el contrato lo dice, invoca `developer-code-implementer`. Responde `DONE` / `CONFLICT` / `BLOCKED` / `DESIGN-ERROR` / `DESIGN-ERRATA`; el motor lleva `CONFLICT`/`BLOCKED` al usuario.
- **Error de diseño** (§9.1): si un implementador o un corrector-build devuelve `DESIGN-ERROR` (el problema está en el diseño y no se arregla con código), el motor escribe `implementation/error_design.log` con la explicación detallada y **detiene el skill sin preguntar**. **MUST NOT** editar el diseño para forzar que cuadre; corregirlo es trabajo de `/sdd-designer`.
- **Erratas del diseño** (§2.4, §9.2): una errata pequeña **no** detiene el skill; implementador o corrector-build devuelven `DESIGN-ERRATA` (sin editar el diseño), el editor-diseño corrige `design/` y las copias de `implementation/`, y el motor relanza al que la encontró. Si no es pequeña, o pasa de 5 (**LIMIT**), es `DESIGN-ERROR`.
- **Verificar/corregir el build** (§10): bucle verificador-build → corrector-build hasta `OK-COMPILA` (**LIMIT** 20; tras la 20ª o si los errores se repiten, **STOP** y `AskUserQuestion`). El verificador-build compila (comando de la plantilla) y reporta en **JSONL** (`id`/`tipo`/`fichero`/`ubicacion`/`tarea`/`mensaje`/`correccion`); el motor lo vuelca a `implementation/log_build.txt`. Si el verificador-build o el corrector-build devuelven `BLOCKED`, **STOP** y se pregunta al usuario; si devuelve `DESIGN-ERROR`, se aplica §9.1. El motor **MUST NOT** compilar él mismo (§2.2).
- **Contrato de tokens** (§2.3): el skill compara por literal exacto — `ESCRITO: implementation/`, `DONE`/`CONFLICT`/`BLOCKED`/`DESIGN-ERROR`/`DESIGN-ERRATA`, `APLICADO: <n>`/`RECHAZADO:`, `OK-COMPILA`/`BLOCKED` (verificador-build), `CORREGIDO`/`BLOCKED`/`DESIGN-ERROR`/`DESIGN-ERRATA` (corrector-build). Los subagentes **MUST NOT** pegar el código en su respuesta (ya está en disco).
- **Solo lo pedido** (§2.5): cada subagente se limita a su tarea; lo que crea que falta lo devuelve como `SUGERENCIA:` sin hacerlo, y el motor lo lista en el cierre sin aplicarlo.
- **MUST NOT** invocar `developer-code-implementer` tú mismo ni lanzar `/sdd-close`: el código lo escriben los implementadores; el cierre lo decide el usuario.

---

## Apéndice A — Override de rutas (para testing y versatilidad)

- `--template-dir=<ruta>` — **carpeta de plantillas** alternativa a la resuelta en §2.2. Tiene prioridad, salvo la mezcla de plantillas prohibida (§2.2 → **ERROR**). Es además **obligatorio** cuando la iniciativa declara `template: external` (§2.2): sin él → **ERROR**. **MUST** contener un `README.md` (la guía, que declara todo lo específico y referencia los demás ficheros); si falta → **ERROR** y detente. El skill resuelve `README.md` contra esta carpeta y pasa esa ruta a los subagentes; **MUST NOT** resolver ni ejecutar ningún otro fichero de la carpeta (cualquier comando de build/validación lo descubre y ejecuta el subagente vía el README — §2.2). Ese `README.md` **MUST** estar redactado para los **cuatro roles** (descomponedor, implementador, verificador-build, corrector-build). Permite usar el mismo flujo con otro tipo de artefacto sin tocar el código del skill.
- `--in=<ruta>` — fichero `design.md` de entrada explícito. **Desactiva la auto-detección** de la Fase 0 caso 2. La "carpeta de la iniciativa" es la que contiene la subcarpeta `design/` de ese fichero.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Las rutas relativas se resuelven contra esta raíz.

En uso normal no se especifican: se usa la carpeta de plantillas resuelta por el frontmatter `template:`, la carpeta de la iniciativa y `.sdd/drafts/`.
