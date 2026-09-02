---
name: sdd-designer
description: Segundo paso del pipeline SDD. Dada una especificación funcional (`specification.md`, `type: specification`) producida por `/sdd-specification`, genera un plan de DISEÑO en una carpeta `design/` (índice `design.md`, `type: design`) que consume `/sdd-implementer`. El skill es un MOTOR genérico y agnóstico al artefacto: aporta solo el flujo (localizar la iniciativa, decidir modo Generar/Revisar, lanzar 5 subagentes diseñadores en paralelo que escriben un diseño completo cada uno, elegir el mejor con un juez por torneo, y verificar/corregir el ganador en bucle) y delega TODO lo específico del diseño en el `README.md` de la carpeta de plantillas activa, que los subagentes leen como contrato. El skill trae una carpeta `template-<nombre>/` por tipo de artefacto (no conoce sus nombres) y usa la que declare el frontmatter `template:` del `specification.md` de la iniciativa (configurable con `--template-dir`), propagándola al frontmatter del `design.md`; no sabe nada de cómo es el diseño, así que cambiar de plantilla cambia por completo qué y cómo se diseña sin tocar este skill.
handoffs:
  - label: Implementar el diseño
    agent: sdd-implementer
    prompt: Implementar el diseño recién generado en .sdd/drafts/{carpeta-iniciativa}/design/design.md
---

# sdd-designer

Eres un **motor de diseño** del pipeline SDD: transformas una **especificación funcional** en un **plan de diseño** (no una implementación). La entrada la produce `/sdd-specification` y la salida la consume `/sdd-implementer`.

**CRITICAL — eres agnóstico al artefacto.** Este `SKILL.md` define **solo el flujo y la orquestación de agentes**. **No sabe nada de qué se diseña** (ni qué ficheros tiene la spec más allá de su índice, ni qué reglas, taxonomías, capas, ficheros de salida, formatos o validaciones existen): **todo eso lo declara la guía `<plantilla-activa>/README.md`**, que los subagentes leen como contrato. **MUST NOT** asumir de memoria ningún detalle del diseño; **MUST NOT** nombrar ficheros, identificadores, taxonomías ni validaciones concretas en este skill. Así, apuntar `--template-dir` a otra carpeta de plantillas con un README distinto cambia por completo el diseño producido **sin tocar este skill**.

El skill tiene **dos modos** (se decide en la Fase 0, §4.4, según exista o no la carpeta `design/`):

- **Generar/Regenerar** (Fases 1-9): produce el `design/` desde cero (5 diseñadores en paralelo → torneo del juez → renombrar ganador → enriquecer → bucle verificar/corregir diseño → describir tests unitarios → bucle verificar/corregir tests unitarios). Modo por defecto cuando no hay `design/`.
- **Revisar/Modificar** (§14): re-invocación sobre un `design/` existente. **No regenera**: aplica los cambios puntuales que pida el usuario y pasa el bucle verificar/corregir, preservando las ediciones manuales.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- **Ruta a un `specification.md`** existente. El skill valida el frontmatter `type: specification` y procede.
- **Sin argumentos**: el skill pregunta con `AskUserQuestion` qué iniciativa de `.sdd/drafts/` usar — la última (recomendada) o elegir otra (§4.2).
- **Texto adicional tras la ruta**:
  - En modo **Generar/Regenerar**: se trata como guías de diseño y se persiste en `{iniciativa}/design-guidelines.md` (§4.3).
  - En modo **Revisar/Modificar**: es la **lista de cambios puntuales** a aplicar sobre el diseño existente (§14).
- Flags de override `--template-dir=`, `--in=`, `--out=`, `--root=` (Apéndice A).

---

## Outline

1. **Fase 0 — Localizar** la iniciativa y su `specification.md`, las guías opcionales, y **decidir el modo** según exista o no `design/` (§4.4).
2. **Fase 1 — Cargar** el contrato (`<plantilla-activa>/README.md`) y resolver las rutas de entrada que se pasarán a los subagentes. (Común a ambos modos.)
3. **Fase 2 — Diseñar**: lanzar **5 subagentes diseñadores en paralelo**, cada uno escribe un diseño completo en `design_<n>/`. (Solo Generar/Regenerar.)
4. **Fase 3 — Elegir**: un subagente **juez** decide por **torneo** (ganador acumulado vs siguiente diseño) hasta quedar uno, **justificando y mostrando por pantalla** en cada comparación por qué elige un diseño frente al otro. (Solo Generar/Regenerar.)
5. **Fase 4 — Seleccionar**: renombrar la carpeta ganadora a `design/`, mover `log_best.txt` y borrar el resto. (Solo Generar/Regenerar.)
6. **Fase 5 — Enriquecer y sanear**: un subagente **enriquecedor** revisa, a partir de `log_best.txt`, (a) qué ventajas de los diseños descartados faltan en el ganador y tienen sentido, y (b) qué **defectos/errores que el juez atribuyó al propio ganador** siguen presentes; reporta unas y otros como mejoras a implementar y un subagente **corrector** las aplica. (Solo Generar/Regenerar.)
7. **Fase 6 — Verificar/corregir**: bucle subagente verificador (que aplica la validación que prescriba la plantilla) → (si hay fallos) subagente corrector, hasta `OK-CORRECTO`. (Común a ambos modos.)
8. **Fase 7 — Tests unitarios**: un subagente **test-unitarios** describe en `design/test-unit-desc.md` los tests unitarios que declare el contrato de la plantilla activa (solo descripción, sin código). (Común a ambos modos.)
9. **Fase 8 — Verificar/corregir tests unitarios**: bucle subagente **verificador-test-unitarios** (comprueba que `test-unit-desc.md` es coherente con el diseño) → (si hay fallos) subagente **corrector-test-unitarios**, hasta `OK-CORRECTO`. (Común a ambos modos.)
10. **Fase 9 — Cerrar** con mensaje al usuario y handoff a `/sdd-implementer`.
11. **§14 — Modo Revisar/Modificar**: ruta alternativa desde la Fase 0.

**STOP conditions**:

- `--template-dir=` apunta a una carpeta que **no contiene `README.md`** (la guía que declara todo lo específico) → **ERROR** y detente.
- `--template-dir=` apunta a otra carpeta `template-*/` de este skill **distinta** de la que declara el frontmatter `template:` de la spec, o el `template:` de la spec no resuelve a ninguna carpeta de plantillas (§2.2) → **ERROR** y detente: **MUST NOT** mezclarse plantillas — sus arquitecturas no son compatibles.
- El frontmatter `template:` de la iniciativa vale `external` (se especificó con una plantilla externa) y **no** se pasa `--template-dir=` (§2.2) → **ERROR** y detente pidiéndolo. **MUST NOT** preguntar la plantilla ni caer a una carpeta interna.
- Frontmatter de `specification.md` no contiene `type: specification` → **ERROR** y detente.
- `design-guidelines.md` existe pero su frontmatter no contiene `type: design-guidelines` → **ERROR** y detente.
- Carpeta `design/` ya existe y no está vacía → **STOP** y pregunta: Regenerar vs Revisar/Modificar (§4.4).
- En modo Revisar/Modificar, el frontmatter de `design.md` no es `type: design` → **ERROR** y detente (§14).
- Ningún diseñador produjo una carpeta `design_<n>/` con contenido → **ERROR** y detente.
- El juez no devuelve un token `GANADOR: design_<n>` válido tras 1 reintento → **STOP** y muestra el problema.
- Tras **10** iteraciones del bucle verificar/corregir del diseño (Fase 6) el verificador sigue sin responder `OK-CORRECTO` → **STOP** y muestra al usuario las líneas JSONL de los problemas residuales. **MUST NOT** dar el diseño por bueno.
- Tras **10** iteraciones del bucle verificar/corregir de los tests unitarios (Fase 8) el `verificador-test-unitarios` sigue sin responder `OK-CORRECTO` → **STOP** y muestra al usuario las líneas JSONL residuales. **MUST NOT** dar `test-unit-desc.md` por bueno.

---

## 1. Entrada y salida

### 1.1 Entrada

La **especificación** de la iniciativa, cuyo índice es `specification.md` (único fichero de entrada con nombre fijo; debe contener `type: specification`). El índice enlaza otros ficheros en su carpeta — el skill **no asume cuáles son** (los define la plantilla de `/sdd-specification`); los subagentes los leen siguiendo el índice.

Opcionalmente, en la carpeta de la iniciativa puede existir `design-guidelines.md` (frontmatter `type: design-guidelines`) con guías técnicas que orientan el diseño. Si existe, se pasa **tal cual** a los subagentes. Si **no existe**, simplemente no se pasa.

### 1.2 Salida

Una **carpeta** `design/` dentro de la carpeta de la iniciativa.

**CRITICAL — la estructura interna de `design/` la define `<plantilla-activa>/README.md`, no este skill.** Qué ficheros y subcarpetas la componen, qué contiene cada uno y cómo se valida, **lo declara la guía**, que los subagentes leen. El skill **MUST NOT** asumir esos detalles de memoria; solo manipula la carpeta como una unidad (la crea cada diseñador, el juez la compara, el verificador la valida).

**Único contrato fijo (no lo cambia `--template-dir`):** el índice de la salida se llama `design.md` y lleva frontmatter `type: design` **más la clave `template:` heredada del `specification.md`** (§2.2 paso 3). Es lo que el skill usa para **localizar y validar** un diseño existente (Fase 0 / §14) y lo que consume `/sdd-implementer`, que resuelve con esa clave su propia carpeta de plantillas.

**Logs de orquestación del motor.** Además de la estructura que define la plantilla, el motor escribe en la carpeta de salida sus propios ficheros de **log** (no son contenido de diseño ni los define la plantilla; los verificadores los ignoran):

- `log_best.txt` — las **ventajas y los defectos de cada diseño** y la **justificación** de la elección en cada comparación del torneo (§7), para poder auditar después tanto si el diseño ganador cumple las ventajas reclamadas, como si arrastra alguno de los defectos que el juez le detectó (para corregirlo en la Fase 5), como el criterio por el que se eligió un diseño frente a otro. Solo en modo Generar/Regenerar (en Revisar/Modificar no hay torneo).
- `log_revision.txt` — la salida **JSONL literal de cada subagente verificador** de la Fase 6 (§10), una sección por iteración. En ambos modos.
- `log_revision_unit-test.txt` — la salida **JSONL literal de cada `verificador-test-unitarios`** de la Fase 8 (§12), una sección por iteración. En ambos modos.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-kebab-case}/   ← carpeta de la iniciativa
        ├── specification.md                     ← índice de entrada (type: specification)
        ├── <ficheros que enlace specification.md> ← input (los define la plantilla del spec)
        ├── design-guidelines.md                 ← opcional (input)
        ├── design_1/ … design_5/                ← borradores de cada diseñador (efímeros)
        └── design/                              ← ganador renombrado (salida final)
            ├── design.md                        ← índice (type: design)
            ├── …                                ← ficheros y carpetas que declare <plantilla-activa>/README.md
            ├── log_best.txt                     ← log del motor: ventajas, defectos y justificación de cada comparación (§7, solo Generar)
            ├── log_revision.txt                 ← log del motor: JSONL de cada verificador del diseño (§10)
            └── log_revision_unit-test.txt       ← log del motor: JSONL de cada verificador-test-unitarios (§12)
```

---

## 2. Principios

### 2.1 La especificación es la fuente de verdad

La especificación es la fuente de verdad — **MUST NOT** interpretar ni ampliar más allá de lo que dice. Los subagentes leen `specification.md` y todos los ficheros que enlace. **MUST NOT** usar otros `design.md` o diseños previos de `.sdd/` como plantilla — **salvo lectura** de las iniciativas archivadas que `design-guidelines.md` cite explícitamente (solo para respetar sus decisiones, nunca como plantilla de estructura).

### 2.2 El README es el contrato único

**Carpeta de plantillas activa (por frontmatter `template:`).** Este skill trae una carpeta `template-<nombre>/` por cada tipo de artefacto diseñable; **no conoce sus nombres** (crear un tipo nuevo = crear su carpeta, sin tocar este skill). Cada plantilla define una arquitectura distinta y **MUST NOT** mezclarse. La activa se resuelve así, **antes** de cargar el contrato (Fase 1):

1. `--template-dir=<ruta>` explícito → esa carpeta (válvula de testing). **ERROR** si apunta a otra carpeta `template-*/` de este skill **distinta** de la que declara la iniciativa (mezcla de arquitecturas, STOP condition).
2. Sin flag → lee la clave `template:` del frontmatter del `specification.md` de la iniciativa y usa `template-<valor>/`. Si la clave falta (spec anterior a este contrato) → pregúntala con `AskUserQuestion` (una opción por carpeta `template-*/` del skill). Si `template-<valor>/` no existe en este skill → **ERROR** indicando las disponibles o que se pase `--template-dir=`.
3. **Valor reservado `template: external`** (la iniciativa se especificó con un `--template-dir` externo): la plantilla activa **solo** puede venir de `--template-dir=`, apuntando a la carpeta externa que corresponda a **este** skill. Si el flag no viene → **ERROR** y detente pidiéndolo; **MUST NOT** preguntar la plantilla, **MUST NOT** caer a una carpeta interna (mezclaría arquitecturas en silencio) y **MUST NOT** buscarse `template-external/`. `external` no es una ruta ni una clave ausente: cada skill tiene su propia carpeta de plantillas, así que la ruta externa de un skill no designa nada en otro.
4. **Herencia hacia abajo:** el `design.md` producido lleva en su frontmatter **la misma clave `template:` que la spec, copiada verbatim** —incluido el valor `external`—, para que los skills posteriores la resuelvan sin volver a la spec. **MUST NOT** sustituirse por la ruta del `--template-dir` de este skill: sería la ruta de la carpeta de plantillas **del designer**, que no designa nada en `/sdd-implementer` ni en los skills de tests.

**En todo el resto del skill, `<plantilla-activa>/README.md` denota «el `README.md` de la carpeta de plantillas activa» resuelta aquí.**

Todo lo específico del diseño (qué se produce, cómo se convierte el spec, qué contexto cargar, cómo se valida) lo define `<plantilla-activa>/README.md` y los ficheros que él referencie. Los subagentes los **leen de disco**; el skill **MUST NOT** asumirlos, restatarlos ni hardcodearlos aquí. El skill solo pasa a cada subagente **las rutas** de los ficheros de entrada y su rol.

**CRITICAL — `README.md` es el ÚNICO fichero de la plantilla que el motor conoce por nombre.** El skill **MUST NOT** nombrar, leer, resolver ni **ejecutar** ningún otro fichero de la plantilla (ni los documentos que el README referencie, ni ningún script de validación que la plantilla traiga). Esos ficheros los descubren y usan **los subagentes** leyendo el `README.md`. En particular:

- Si la plantilla prescribe una validación que se ejecuta como **comando o script** (p.ej. validar con una herramienta externa los artefactos generados), **la ejecuta el subagente verificador** —que lee la plantilla y la descubre—, **NUNCA el motor**.
- **MUST NOT** añadir "pasos de `Bash`" en este skill que corran validaciones, comprobaciones o herramientas específicas del diseño. El motor solo usa `Bash`/`Write` para orquestación **agnóstica** (listar `.sdd/drafts/`, `mv`/`rm` de carpetas `design_<n>/`, y escribir sus propios **logs de orquestación** `log_best.txt`/`log_revision.txt`/`log_revision_unit-test.txt` — §7/§10/§12), nunca para validar el contenido del diseño.
- Esos tres logs son artefactos **del motor**, no contenido de diseño ni ficheros que declare la plantilla: el verificador no los valida y `--template-dir` no los cambia.
- Único acoplamiento permitido por nombre: `README.md` (contrato de la plantilla) y el contrato fijo de I/O `specification.md` / `design.md`.

**REQUIRED — el README de la plantilla es leído por los 8 roles.** Este skill lanza **ocho** subagentes con tareas distintas sobre el mismo diseño: **diseñador** (crea), **juez** (elige entre dos), **enriquecedor** (detecta qué ventajas de los descartados incorporar y qué defectos del propio ganador sanear), **verificador** (busca problemas en el diseño), **corrector** (corrige/incorpora en el diseño), **test-unitarios** (describe los tests unitarios), **verificador-test-unitarios** (comprueba que los tests unitarios son coherentes con el diseño) y **corrector-test-unitarios** (corrige los tests unitarios) — ver §2.3, §6–§12. Los ocho reciben las mismas rutas de entrada y **leen el mismo `README.md` de la plantilla**, pero cada uno hace una cosa distinta y necesita un subconjunto distinto de sus ficheros. Por tanto, **cualquier `README.md` de plantilla** (cualquier `template-<nombre>/` del skill o una externa apuntada con `--template-dir=`) **MUST** estar redactado teniendo en cuenta esos 8 roles: debe delimitar, por rol, qué tarea hace y qué ficheros de la plantilla le aplican. Un README que solo contemple al diseñador es **incompleto** para este skill.

### 2.3 Orquestación de subagentes

- Los **diseñadores** corren **en paralelo** (§6); **MUST NOT** usar `AskUserQuestion`. El **juez**, el **enriquecedor**, el **verificador**, el **corrector**, el **test-unitarios**, el **verificador-test-unitarios** y el **corrector-test-unitarios** corren **de uno en uno** (cada uno depende del resultado del anterior).
- **MUST NOT** usar `run_in_background`: el skill necesita el resultado de cada subagente para continuar.
- Cada rol responde con un **token literal** que el skill parsea (definidos en cada fase). El skill compara por literal exacto.

### 2.4 Confinamiento de escritura — nunca fuera de la carpeta de la iniciativa

El diseño es un **plan**, no una implementación. Ni el motor ni ningún subagente tocan el árbol real del proyecto.

- **CRITICAL — el motor y los 8 subagentes MUST NOT escribir, crear, editar, mover ni borrar NINGÚN fichero fuera de la carpeta de la iniciativa** (`{iniciativa}/design_<n>/` o `{iniciativa}/design/`, según la fase; con `--out=`, la carpeta de salida indicada). En particular **MUST NOT** tocar código fuente (`src/**`), ficheros de configuración (p.ej. `axelor-config.properties`, `build.gradle`, cualquier `*.properties`/`*.yml` o `*.xml` del proyecto real), datos iniciales, ni cualquier otro artefacto del árbol del proyecto.
- **REQUIRED — todo cambio fuera de la carpeta se DOCUMENTA, no se aplica.** Si el diseño **requiere** un cambio fuera de la carpeta de la iniciativa (una propiedad de configuración nueva, una clase existente que modificar, una dependencia, un script), ese cambio **MUST** quedar **descrito dentro del diseño** (en el fichero de `design/` que prescriba la plantilla) para que lo aplique `/sdd-implementer`. **MUST NOT** aplicarlo aquí.
- El único acceso de escritura del motor fuera del contenido de diseño son sus propios **logs de orquestación** dentro de la carpeta de la iniciativa (`log_best.txt`, `log_revision.txt`, `log_revision_unit-test.txt`) y las operaciones `mv`/`rm` sobre las carpetas `design_<n>/`/`design/` (§8). Nada más.

- ✅ CORRECTO: el diseño necesita `correos.reintentos.max=3` → se documenta como propiedad de configuración a añadir en el fichero de diseño que la plantilla destine a configuración; `/sdd-implementer` la escribirá en `axelor-config.properties`.
- ❌ INCORRECTO: un subagente edita `src/main/resources/axelor-config.properties` para añadir la propiedad (escritura fuera de la carpeta de la iniciativa; es trabajo de `/sdd-implementer`)

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fase 0  Localizar la iniciativa + specification.md + guías + modo  │
│  Fase 1  Cargar el contrato (README) y resolver rutas de entrada    │
│  Fase 2  5 diseñadores en paralelo → design_1/ … design_5/          │
│  Fase 3  Torneo del juez:  g=design_1                               │
│            para i=2..N:  g = juez(g, design_i)                      │
│            (muestra ventajas+defectos+justificación; acumula        │
│             ventajas y defectos en log_best.txt para auditar luego  │
│             al ganador)                                             │
│  Fase 4  Renombrar el ganador a design/ ; mover log_best.txt;       │
│            borrar el resto                                          │
│  Fase 5  enriquecedor(design/, log_best.txt) → mejoras a aplicar    │
│            → corrector(design/, mejoras)  (ventajas de los          │
│              descartados que faltan en el ganador + defectos que    │
│              el juez detectó en el propio ganador y siguen ahí)     │
│  Fase 6  Bucle (LIMIT 10):                                          │
│            verificador(design/) → OK-CORRECTO ?  (vuelca su JSONL   │
│              sí  → fin                           a log_revision.txt)│
│              no  → corrector(design/, fallos) → repetir             │
│  Fase 7  test-unitarios(design/) → design/test-unit-desc.md         │
│            (descripción de tests unitarios; solo descripción)       │
│  Fase 8  Bucle (LIMIT 10):  coherencia tests unitarios ↔ diseño    │
│            verificador-test-unitarios(design/) → OK-CORRECTO ?      │
│              sí  → fin    (vuelca JSONL a log_revision_unit-test.txt)│
│              no  → corrector-test-unitarios(design/, fallos) → rep. │
│  Fase 9  Mensaje de cierre al usuario                               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. Fase 0 — Localizar la iniciativa y decidir modo

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca con una ruta a un `specification.md`:

1. Leer el fichero.
2. **Validar el frontmatter**: debe contener `type: specification`. Si falla, detente y muestra:
   > Error: el fichero `{ruta}` no es una especificación válida. Su frontmatter debe incluir `type: specification`.
   > Para crear o mejorar una especificación, usa `/sdd-specification`.
3. La **carpeta de la iniciativa** es la que contiene el `specification.md`.

### 4.2 Caso 2 — Sin ruta (elección de iniciativa)

1. Listar las subcarpetas de `.sdd/drafts/` cuyo nombre cumple `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Quedarte con las que contienen `specification.md` y ordenarlas alfabéticamente (el prefijo timestamp = orden cronológico); la **última** es la recomendada.
3. Si no hay ninguna, indica que no hay especificaciones disponibles y pide una ruta. Detente.
4. **Preguntar con `AskUserQuestion`** (administración del skill, opciones cerradas): **Usar la última** `{nombre}` (recomendado) o **Elegir otra** (muestra el resto y deja seleccionar una distinta de la última).
5. Leer `specification.md` dentro de la carpeta elegida y aplicar el flujo del caso 1.

### 4.3 Guías de diseño opcionales desde el prompt

**Orden**: el guard §4.4 se evalúa **antes** que este apartado, que **solo** aplica en modo Generar/Regenerar. En modo Revisar/Modificar el texto adicional es la lista de cambios (§14), no guías.

Tras confirmar el modo Generar/Regenerar, si en los argumentos queda texto adicional:

1. Determinar la ruta `{iniciativa}/design-guidelines.md`.
2. **Si NO existe el fichero y hay prompt adicional**: créalo con el contenido literal del prompt precedido de:
   ```
   ---
   type: design-guidelines
   ---

   {texto del prompt tal cual}
   ```
   Indica: `Guías de diseño guardadas en {ruta}`. Continúa con la Fase 1.
3. **Si YA existe y hay prompt adicional**: detente sin tocar nada:
   > Error: ya existe `{ruta}/design-guidelines.md`. No se puede pasar guías por el prompt cuando el fichero ya existe — edita el fichero directamente. Razón: una única fuente de verdad y evitar pérdidas.
4. **Si NO hay prompt adicional**: continúa con la Fase 1.

### 4.4 Guard: ¿ya existe la carpeta `design/`? — elección de modo

Comprobar si **ya existe** una carpeta `design/` no vacía en la carpeta de la iniciativa.

- Si **no existe** o está vacía: modo **Generar/Regenerar**. Continúa con §4.3 y luego la Fase 1 → Fase 6.
- Si **sí existe** (y contiene al menos `design.md`): **detener y preguntar con `AskUserQuestion`** entre:

1. **Revisar / modificar el diseño existente** (recomendado si se editó a mano o solo quieres cambios puntuales): **NO regenera**; entra en el **modo Revisar/Modificar (§14)**.
2. **Regenerar desde la especificación** (pisa el diseño actual): continúa con §4.3 y la Fase 1; los borradores `design_<n>/` y `design/` se rehacen.

Mensaje exacto al usuario:

> Ya existe `design/` en `{carpeta}`. ¿Qué quieres hacer?
> - **Revisar / modificar el diseño existente**: preserva tus ediciones, aplica los cambios que indiques y pasa el verificador. No regenera.
> - **Regenerar desde la especificación**: descarta el diseño actual y vuelve a generarlo desde cero a partir del spec.

---

## 5. Fase 1 — Cargar el contrato y resolver rutas de entrada

1. **REQUIRED — lee con `Read` la guía `<plantilla-activa>/README.md`** (resuelta según §2.2: la familia del frontmatter `template:` de la spec, o `--template-dir`): confirma que existe (si no → **ERROR**, STOP condition) y entiende, a alto nivel, qué rol pide a cada subagente. **No** necesitas memorizar su contenido: los subagentes la leerán de disco. Es el **único fichero que el skill conoce por nombre**; el README referencia los demás ficheros de la plantilla, que los subagentes seguirán.
2. **Resolver las rutas de entrada** que se pasarán a los subagentes (no su contenido):
   - la ruta de la guía `<plantilla-activa>/README.md` (las **reglas para el diseño**),
   - la ruta de `specification.md` (la **especificación**),
   - la ruta de `design-guidelines.md` (las **guías de diseño**) **solo si el fichero existe**; si no, no se pasa.
3. Si existe `design-guidelines.md`, **validar** su frontmatter `type: design-guidelines`; si no lo tiene → **ERROR**:
   > Error: el fichero `{ruta}` no es un fichero de guías de diseño válido. Debe empezar con `---` / `type: design-guidelines` / `---`.

No hay más preparación: el skill no carga skills técnicos ni explora el código — eso lo hace cada subagente leyendo el README (que indica qué contexto cargar).

---

## 6. Fase 2 — Diseñar (5 subagentes en paralelo)

**CRITICAL**: lanza **exactamente 5 subagentes diseñadores** en una **única respuesta** con 5 invocaciones a `Agent` simultáneas. **MUST NOT** lanzarlos secuencialmente. **MUST NOT** usar `run_in_background`. Numéralos `n = 1..5`; el diseñador `n` escribe su diseño en la carpeta `design_<n>/` de la iniciativa.

> Si en el futuro se quisiera otro número de diseñadores, basta lanzar `N` y numerarlos `1..N`; el resto del flujo (torneo, selección) opera sobre los `design_<n>/` que existan, sea cual sea `N`.

**Prompt de cada subagente diseñador `n`** (mismo para los 5 salvo el número de carpeta):

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que crear un diseño en base a una especificación, unas guías de diseño y unas reglas para el diseño.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y **todos los ficheros que referencie**. Son el contrato: define qué producir, cómo, con qué estructura y qué contexto del proyecto cargar. Síguelo al pie de la letra.
> - **Especificación**: lee `{ruta de specification.md}` y todos los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(esta línea solo si el fichero existe)*.
> - **Salida**: escribe el **diseño completo y autosuficiente** en la carpeta `{iniciativa}/design_<n>/`, con la estructura exacta que define el README (incluido su índice `design.md` con el frontmatter que el contrato define: `type: design` más la clave `template:` copiada verbatim del `specification.md` — §2.2 paso 4).
> - **CRITICAL — confinamiento de escritura**: **MUST NOT** escribir, crear, editar ni borrar ningún fichero fuera de `{iniciativa}/design_<n>/`: nada de código fuente (`src/**`), configuración (`axelor-config.properties`, `build.gradle`, …) ni otros artefactos del proyecto. El diseño es un **plan**. Si el diseño exige un cambio fuera de esa carpeta (p.ej. una propiedad de configuración nueva o una clase existente que modificar), **documéntalo dentro del diseño** para que lo aplique `/sdd-implementer`; **MUST NOT** aplicarlo tú.
> - **MUST NOT** usar `AskUserQuestion`. Ante una ambigüedad, toma la decisión más razonable y documéntala dentro del propio diseño.
> - Al terminar, responde **exactamente** `ESCRITO: design_<n>` y, opcionalmente, 1-2 líneas de notas. **MUST NOT** pegar el contenido del diseño en la respuesta (ya está en disco).

Tras los 5: comprueba que cada `design_<n>/` existe y tiene contenido. Si **ninguna** se creó → **ERROR** (STOP condition). Si alguna falta o quedó vacía, descártala: el torneo opera solo sobre las carpetas válidas.

- ✅ CORRECTO (respuesta del diseñador): `ESCRITO: design_3`
- ❌ INCORRECTO: `He guardado el diseño 3` (token no parseable), pegar el `design.md` completo en la respuesta (gasta contexto, ya está en disco)

---

## 7. Fase 3 — Elegir el mejor diseño (torneo del juez)

El ganador se decide por **torneo acumulativo** con un subagente **juez** que compara **dos diseños cada vez**. Sea `D` la lista ordenada de carpetas válidas (`design_1`, `design_2`, …):

1. `ganador = D[0]`.
2. **Para cada** `design_i` siguiente de la lista: lanza el juez con (`ganador`, `design_i`); su veredicto pasa a ser el nuevo `ganador`.
3. Al agotar la lista, `ganador` es el diseño elegido.

Cada invocación del juez es **secuencial** (depende del ganador anterior). Si solo hay **una** carpeta válida, no hay torneo: esa es el ganador.

**Prompt del subagente juez** (en cada comparación):

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que elegir entre 2 diseños en base a una especificación, unas guías de diseño y unas reglas para el diseño.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie.
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseños a comparar**: la carpeta `{iniciativa}/{ganador}` (la llamo `<carpeta-A>`) y la carpeta `{iniciativa}/design_<i>` (la llamo `<carpeta-B>`).
> - Elige cuál de los dos cumple mejor la especificación, las guías y las reglas, **detallando las ventajas concretas Y los defectos/errores concretos de CADA uno de los dos diseños** y con cuál te quedas.
> - Responde con este formato **exacto** (seis bloques, en este orden):
>   - Primera línea: **exactamente** `GANADOR: <nombre-de-carpeta>` (una de las dos comparadas).
>   - Una línea **exactamente** `=== VENTAJAS <carpeta-A> ===` y debajo, en bullets (`- `), las **ventajas concretas** de ese diseño (qué hace bien, qué punto del spec/guías/reglas cubre mejor — no elogios genéricos). **LIMIT**: 2-6 bullets.
>   - Una línea **exactamente** `=== DEFECTOS <carpeta-A> ===` y debajo, en bullets (`- `), los **defectos/errores/carencias concretos** de ese diseño (qué hace mal, qué punto del spec/guías/reglas incumple o cubre peor, qué regla/escenario falta — no pegas genéricas; si de verdad no le ves ninguno, un único bullet `- Ninguno detectado`). Estos defectos se auditarán en la Fase 5 aunque este diseño acabe ganando. **LIMIT**: 1-6 bullets.
>   - Una línea **exactamente** `=== VENTAJAS <carpeta-B> ===` y debajo, igual, las ventajas concretas del otro diseño. **LIMIT**: 2-6 bullets.
>   - Una línea **exactamente** `=== DEFECTOS <carpeta-B> ===` y debajo, igual, los defectos/errores/carencias concretos del otro diseño (o `- Ninguno detectado`). **LIMIT**: 1-6 bullets.
>   - Una línea **exactamente** `=== JUSTIFICACIÓN ===` y debajo la justificación: **MUST** explicar **por qué** el ganador es mejor **frente al otro diseño**, citando las diferencias decisivas (qué hace mejor el ganador, en qué falla o se queda corto el perdedor) y, cuando aplique, contra qué punto de la especificación, las guías o las reglas. **LIMIT**: entre 3 y 8 líneas.
>   - `<carpeta-A>`/`<carpeta-B>` son los nombres reales de las dos carpetas comparadas (p.ej. `design_2`, `design_3`).

El skill parsea la primera línea `GANADOR: design_<n>`. Si el token no aparece, no es una de las dos carpetas comparadas, o falta alguno de los cinco bloques `=== … ===` (las dos `VENTAJAS`, los dos `DEFECTOS` y la `JUSTIFICACIÓN`), **reintenta esa comparación 1 vez**; si vuelve a fallar → **STOP** (STOP condition).

**REQUIRED — mostrar por pantalla y registrar en `log_best.txt`.** Tras cada comparación válida, antes de seguir el torneo, el skill **MUST**:

1. **Mostrar al usuario** el veredicto, las ventajas y los defectos de cada diseño y la justificación que devolvió el juez, con este formato:
   ```
   Comparación {k}/{total}: {carpeta-A} vs {carpeta-B} → gana design_<n>
   {bloques === VENTAJAS … ===, === DEFECTOS … === y === JUSTIFICACIÓN === literales del juez}
   ```
   **MUST NOT** ocultar ni resumir las ventajas/defectos/justificación hasta perder el detalle de la comparación.
2. **Añadir (append)** a `{iniciativa}/log_best.txt` una sección con esta comparación: la cabecera `### Comparación {k}: {carpeta-A} vs {carpeta-B} → gana design_<n>` y, debajo, **en este orden**: (a) los cuatro bloques `=== VENTAJAS … ===` y `=== DEFECTOS … ===` **literales** del juez (los dos de ventajas y los dos de defectos), y (b) **al final**, un bloque `=== JUSTIFICACION {carpeta-A} vs {carpeta-B} ===` con el **contenido literal** del bloque `=== JUSTIFICACIÓN ===` que devolvió el juez (la justificación del porqué se eligió un diseño frente al otro, basada en las ventajas y defectos de cada uno). Es un append acumulativo (una sección por comparación). Razón: este log recoge tanto las ventajas reclamadas de cada diseño para **auditar después si el ganador realmente las cumple**, los **defectos detectados** —incluidos los del propio ganador— para que la Fase 5 los **corrija automáticamente**, y la **justificación** de la elección de cada comparación para poder auditar después el criterio del torneo. Se escribe en la carpeta de la iniciativa y la Fase 4 lo mueve a la carpeta de salida (`design/log_best.txt`).

Si solo hay **una** carpeta válida (sin torneo), escribe en `{iniciativa}/log_best.txt` una única línea: `Un único diseño válido (sin torneo ni comparación de ventajas ni defectos).`

- ✅ CORRECTO (respuesta del juez): `GANADOR: design_2` + `=== VENTAJAS design_2 ===` + `=== DEFECTOS design_2 ===` + `=== VENTAJAS design_3 ===` + `=== DEFECTOS design_3 ===` + `=== JUSTIFICACIÓN ===`, cada bloque con sus bullets/líneas
- ❌ INCORRECTO: `Me quedo con el segundo` (sin token), `GANADOR: design_9` (carpeta que no estaba en la comparación), `GANADOR: design_2` sin los bloques `=== VENTAJAS … ===`/`=== DEFECTOS … ===` (no hay ventajas ni defectos que registrar en `log_best.txt`), ventaja tipo `design_2 está más completo` o defecto tipo `design_3 es peor` (genéricos, no concretan qué hace bien/mal)

---

## 8. Fase 4 — Seleccionar el ganador

Una vez conocido el ganador:

1. **Renombrar** la carpeta ganadora a `design/`:
   ```bash
   mv .sdd/drafts/{iniciativa}/{ganador} .sdd/drafts/{iniciativa}/design
   ```
   (Si el ganador ya fuera `design`, no aplica.)
2. **Borrar** el resto de carpetas de borrador:
   ```bash
   rm -rf .sdd/drafts/{iniciativa}/design_[0-9]*
   ```
3. **Mover** el log de ventajas y defectos dentro de la carpeta de salida (el motor lo acumuló en la iniciativa durante el torneo, §7):
   ```bash
   mv .sdd/drafts/{iniciativa}/log_best.txt .sdd/drafts/{iniciativa}/design/log_best.txt
   ```
   (Si se indicó `--out=`, el destino es `{--out=}/log_best.txt`.)

Tras esto solo queda `design/` (más `--out=` si se indicó: en ese caso, el destino final es esa carpeta).

---

## 9. Fase 5 — Enriquecer el ganador con las ventajas de los descartados y sanear sus defectos

**Solo en modo Generar/Regenerar** (depende del torneo y de `log_best.txt`; en Revisar/Modificar no aplica — §14). Tras seleccionar el ganador, el diseño se **enriquece y sanea** en una sola pasada, a partir de `design/log_best.txt`:

- **(a) Enriquecer**: incorporar las **ventajas de los diseños descartados** que el ganador no tenga y que tengan sentido. Los diseños descartados ya no están en disco (la Fase 4 los borró): la fuente de esas ventajas es `log_best.txt`.
- **(b) Sanear**: corregir los **defectos/errores que el juez atribuyó al propio diseño ganador** en `log_best.txt` y que **sigan presentes** en él. El objetivo de este punto es exactamente lo que pidió el usuario: que, al elegir un ganador, no se arrastren sin más los fallos que el juez ya le había detectado, sino que se corrijan automáticamente.

1. **Lanzar el subagente enriquecedor** (uno solo). Recibe todo el contexto + `log_best.txt`; **comprueba** (a) qué ventajas de los diseños descartados faltan en el ganador y procede aplicar, y (b) qué defectos que el juez le atribuyó al ganador siguen presentes, y **reporta** unas y otros como mejoras (no las implementa).
2. **Mostrar al usuario** la respuesta del enriquecedor (las mejoras a implementar, o que no hay ninguna).
3. Si respondió **exactamente** `OK-SIN-MEJORAS` → no hay nada que incorporar ni sanear: ve directamente a la Fase 6.
4. Si respondió líneas **JSONL** de mejoras: **lanza el subagente corrector** pasándole esas mismas líneas, para que las aplique en sitio sobre `design/`. Luego ve a la Fase 6.
5. Si la respuesta no es ni `OK-SIN-MEJORAS` ni JSONL parseable, **reintenta 1 vez**; si vuelve a fallar, trata el enriquecimiento como vacío (avísalo al usuario) y continúa con la Fase 6. **MUST NOT** bloquear el diseño por esto.

**Prompt del subagente enriquecedor**:

> Eres un experto arquitecto y diseñador en Java y el framework Axelor. Tienes un diseño **ganador** de un torneo y el registro `log_best.txt` con las **ventajas** y los **defectos/errores** que el juez atribuyó a cada diseño comparado (incluidos los **descartados** y el **propio ganador**). Tu tarea tiene dos partes: **(a)** decidir qué ventajas de los diseños descartados **conviene incorporar** al ganador, y **(b)** detectar qué **defectos que el juez le atribuyó al propio ganador siguen presentes** en él, para que se corrijan.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie.
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño ganador**: la carpeta `{iniciativa}/design`.
> - **Ventajas y defectos de cada diseño**: lee `{iniciativa}/design/log_best.txt` (bloques `=== VENTAJAS … ===` y `=== DEFECTOS … ===` de cada comparación).
> - **(a) Ventajas de los descartados**: para **cada ventaja** de un diseño **descartado** que aparezca en `log_best.txt`, comprueba (1) **si ya existe** en el diseño ganador, y (2) **si tiene sentido aplicarla** (coherente con la especificación, las guías y las reglas, sin contradecir las decisiones del ganador). Reporta **solo** las que **faltan** en el ganador **y** tienen sentido incorporar.
> - **(b) Defectos del propio ganador**: para **cada defecto** que el juez haya atribuido al **diseño que resultó ganador** (los bloques `=== DEFECTOS <ganador> ===` de `log_best.txt`), comprueba **en la carpeta `design/` actual** si ese defecto **sigue presente**. Reporta **solo** los defectos del ganador que **persisten** y que tiene sentido corregir (coherentes con spec/guías/reglas); descarta los que ya estén resueltos, los marcados `Ninguno detectado` o los que no procedan. **Ignora** los bloques `=== DEFECTOS … ===` de los diseños **descartados** (esos defectos murieron con su diseño; aquí solo importan los del ganador).
> - Reporta tanto (a) como (b) en la **misma** lista de mejoras de salida (un `tipo` distingue cuál es cuál).
> - **MUST NOT** modificar el diseño: solo **detecta y reporta** las mejoras (las aplicará el corrector).
>
> **Formato de salida (REQUIRED)**:
> - Si **no** hay ninguna mejora que incorporar ni ningún defecto del ganador que persista (el ganador ya tiene todas las ventajas relevantes y no arrastra defectos), responde **exactamente** y solo: `OK-SIN-MEJORAS`.
> - Si hay mejoras, responde **únicamente** con líneas **JSONL**: **una mejora por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:
>   - `id` — identificador correlativo, formato `M-NNN` (`M-001`, `M-002`, …).
>   - `tipo` — `VENTAJA` (ventaja de un descartado que se incorpora) o `DEFECTO-GANADOR` (defecto del propio ganador que se sanea).
>   - `origen` — de qué diseño/ventaja/defecto del `log_best.txt` procede (p.ej. `design_3: validación de cliente para VAL-Grupo-001/002/010`, o `design_2 (ganador): falta validación de servidor para VAL-Grupo-005`).
>   - `fichero` — fichero del diseño donde aplicarla relativo a la iniciativa (p.ej. `design/views/Grupo-Supervisor.xml`), o `null`.
>   - `ubicacion` — sección, tabla, clase/método o vista concreta; `null` si no aplica.
>   - `mejora` — qué ventaja falta en el ganador y se quiere incorporar, o qué defecto del ganador hay que corregir.
>   - `justificacion` — para `VENTAJA`: por qué no está ya en el ganador y por qué tiene sentido aplicarla; para `DEFECTO-GANADOR`: por qué el defecto que reportó el juez **sigue presente** en `design/` y contra qué punto del spec/guías/reglas va. En ambos casos, contra qué punto del spec/guías/reglas.
>   - `correccion` — qué cambio concreto hacer para incorporar la ventaja o resolver el defecto.
> - Cada línea **MUST** ser JSON válido en una sola línea (escapa saltos como `\n`). **MUST NOT** añadir comentarios ni texto fuera de las líneas JSONL.
>
> Ejemplo de salida con mejoras:
>
> ```jsonl
> {"id":"M-001","tipo":"VENTAJA","origen":"design_3: validación de cliente para VAL-Grupo-001/002/010","fichero":"design/views/Grupo-Supervisor.xml","ubicacion":"action-validate al guardar","mejora":"Añadir validación de cliente (UX) además de la de servidor para nombre/curso/alumno.","justificacion":"El ganador solo valida en servidor; design-contract §5 recomienda también la capa cliente para VAL de campo. No contradice ninguna decisión del ganador.","correccion":"Añadir <action-validate>/<action-condition> en la vista para VAL-Grupo-001/002/010, manteniendo la validación de servidor."}
> {"id":"M-002","tipo":"DEFECTO-GANADOR","origen":"design_2 (ganador): RN-Grupo-004 sin asignación de capa servidor","fichero":"design/design.md","ubicacion":"Tabla de reglas de negocio, fila R-Grupo-004","mejora":"Corregir el defecto que el juez detectó: RN-Grupo-004 quedó sin método de servidor que la aplique.","justificacion":"El juez marcó este defecto en design_2 y sigue presente en design/: la fila R-Grupo-004 no referencia ningún validate*; el spec exige aplicar RN-Grupo-004 en servidor.","correccion":"Asignar RN-Grupo-004 al método de servidor correspondiente y reflejarlo en la tabla y en la clase del diseño."}
> ```

**Prompt del subagente corrector** (para aplicar las mejoras del enriquecedor):

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que **incorporar al diseño** las mejoras indicadas (ventajas de otros diseños que faltan y defectos del propio diseño que hay que sanear). Deberás indicar de la forma más clara posible las mejoras que has aplicado.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie.
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño a enriquecer y sanear**: la carpeta `{iniciativa}/design` — aplica las mejoras **en sitio** (`Edit`/`Write`), sin renombrar ni mover la carpeta, sin regenerar el diseño ni romper las decisiones del ganador que no estén en falta. Tras editar cualquier XML, asegúrate de que sigue validando contra su XSD.
> - **CRITICAL — confinamiento de escritura**: **MUST NOT** escribir, editar ni borrar ningún fichero fuera de `{iniciativa}/design/` (nada de `src/**`, `axelor-config.properties`, `build.gradle`, ni otros artefactos del proyecto). Si una mejora exige un cambio fuera de esa carpeta, **documéntalo dentro del diseño** para `/sdd-implementer`; **MUST NOT** aplicarlo tú.
> - **Mejoras a incorporar** (las reportó el enriquecedor, en formato JSONL, una por línea; el campo `tipo` indica si es una `VENTAJA` que incorporar o un `DEFECTO-GANADOR` que corregir): `{líneas JSONL literales del enriquecedor}`. Aplica cada `correccion` en el `fichero`/`ubicacion` indicados; mantén la trazabilidad y la coherencia (matriz, frontera de confianza, tests) que la plantilla exige.

- ✅ CORRECTO (respuesta del enriquecedor sin mejoras): `OK-SIN-MEJORAS`
- ✅ CON MEJORAS (una línea JSONL por mejora, sin texto alrededor): `{"id":"M-001","tipo":"VENTAJA","origen":"…","fichero":"…","ubicacion":"…","mejora":"…","justificacion":"…","correccion":"…"}` o `{"id":"M-002","tipo":"DEFECTO-GANADOR","origen":"…","fichero":"…","ubicacion":"…","mejora":"…","justificacion":"…","correccion":"…"}`
- ❌ INCORRECTO: `No hace falta nada ✅` (token no exacto), reportar ventajas que el ganador **ya tiene** o defectos del ganador **ya resueltos** (la tarea es solo las ventajas que faltan y los defectos que persisten), reportar defectos de diseños **descartados** (solo importan los del ganador), o devolver las mejoras como prosa/array en vez de una línea JSONL por mejora.

---

## 10. Fase 6 — Verificar y corregir (bucle, LIMIT 10)

Sobre la carpeta `design/`, repite este bucle **como máximo 10 veces** (**LIMIT**: 10 iteraciones); lleva un contador de iteración `{k}` empezando en 1:

1. **Lanzar el subagente verificador** (uno solo).
2. **Volcar su respuesta a `design/log_revision.txt`**: añade (append) la respuesta **literal** del verificador —sus líneas JSONL, o `OK-CORRECTO`— precedida de la cabecera `# Verificación — iteración {k}`. Es un append acumulativo (una sección por iteración). Razón: `log_revision.txt` guarda literalmente el JSONL de cada subagente verificador para revisar después qué encontró cada pasada.
3. Si el verificador respondió **exactamente** `OK-CORRECTO` → el diseño está conforme: sal del bucle y ve a la Fase 7.
4. Si respondió **cualquier otra cosa** (las líneas JSONL de problemas): **MUST** mostrar al usuario por pantalla, tal cual, las líneas JSONL que devolvió el verificador (bloque ` ```jsonl `), antes de continuar; luego **lanza el subagente corrector** pasándole esas mismas líneas, para que corrija en sitio sobre `design/`.
5. Incrementa `{k}` y vuelve al paso 1.

Si tras la 10ª iteración el verificador sigue sin responder `OK-CORRECTO` → **STOP** (STOP condition): muestra al usuario las líneas JSONL de los problemas residuales que reportó el verificador y **MUST NOT** dar el diseño por bueno.

**Prompt del subagente verificador**:

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que verificar si hay algún error en el diseño en base a una especificación, unas guías de diseño y unas reglas para el diseño.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie (incluida la validación que prescriban — **aplícala tal cual, ejecutando los comandos o scripts de validación que la plantilla indique**, p.ej. validar los artefactos generados).
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño a verificar**: la carpeta `{iniciativa}/design`.
>
> **Formato de salida (REQUIRED)**:
> - Si **no** has encontrado nada que corregir, responde **exactamente** y solo: `OK-CORRECTO`.
> - Si has encontrado problemas, responde **únicamente** con líneas **JSONL** (JSON Lines): **un problema por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:
>   - `id` — identificador correlativo del problema, formato `P-NNN` (`P-001`, `P-002`, …).
>   - `severidad` — uno de `BLOCKING` | `IMPORTANT` | `MINOR`.
>   - `fichero` — ruta del fichero del diseño afectado relativa a la iniciativa (p.ej. `design/design.md`), o `null` si es transversal.
>   - `ubicacion` — sección, tabla, clase/método o línea concreta dentro de ese fichero; `null` si no aplica.
>   - `origen` — el identificador del spec/guía/regla que se incumple (p.ej. `VAL-Grupo-003`, `RUI-mis-grupos-formulario-002`, `ESC-009`, o el nombre de la regla de la plantilla), o `null`.
>   - `problema` — descripción clara y concreta del fallo/error/inconsistencia.
>   - `correccion` — qué hay que cambiar para resolverlo.
> - Cada línea **MUST** ser JSON válido en una sola línea (sin saltos de línea internos; escapa los que necesites como `\n`). **MUST NOT** añadir comentarios, numeración ni explicaciones fuera de las líneas JSONL.
>
> Ejemplo de salida con problemas:
>
> ```jsonl
> {"id":"P-001","severidad":"BLOCKING","fichero":"design/design.md","ubicacion":"Tabla de validaciones, fila V-Grupo-003","origen":"VAL-Grupo-003","problema":"La validación VAL-Grupo-003 del spec no está mapeada a ninguna regla V-/R-/U- en el diseño.","correccion":"Añadir la fila V-Grupo-003 en la tabla de validaciones con su clasificación y método validate*."}
> {"id":"P-002","severidad":"IMPORTANT","fichero":"design/test-e2e-desc.md","ubicacion":"Test T-009","origen":"ESC-009","problema":"El escenario ESC-009 del spec no tiene ningún test E2E que lo materialice.","correccion":"Crear un test Given/When/Then que cubra ESC-009 con su trazabilidad Origen ESC."}
> ```

**Prompt del subagente corrector**:

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que corregir los errores en el diseño en base a una especificación, unas guías de diseño y unas reglas para el diseño. Deberás indicar de la forma más clara posible los fallos/errores/inconsistencias que has corregido.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie.
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño a corregir**: la carpeta `{iniciativa}/design` — corrige **en sitio** (`Edit`/`Write` sobre sus ficheros), sin renombrar ni mover la carpeta.
> - **CRITICAL — confinamiento de escritura**: **MUST NOT** escribir, editar ni borrar ningún fichero fuera de `{iniciativa}/design/` (nada de `src/**`, `axelor-config.properties`, `build.gradle`, ni otros artefactos del proyecto). Si una corrección exige un cambio fuera de esa carpeta, **documéntalo dentro del diseño** para `/sdd-implementer`; **MUST NOT** aplicarlo tú.
> - **Problemas a corregir** (los reportó el verificador, en formato JSONL, un problema por línea): `{líneas JSONL literales del verificador}`. Resuelve cada línea (`id`/`severidad`/`fichero`/`ubicacion`/`origen`/`problema`/`correccion`); aplica la `correccion` en el `fichero`/`ubicacion` indicados.

- ✅ CORRECTO (respuesta del verificador sin problemas): `OK-CORRECTO`
- ✅ CON PROBLEMAS (una línea JSONL por problema, sin texto alrededor): `{"id":"P-001","severidad":"BLOCKING","fichero":"design/design.md","ubicacion":"…","origen":"VAL-Grupo-003","problema":"…","correccion":"…"}`
- ❌ INCORRECTO: `Todo correcto ✅` (token no exacto; el skill compara por literal), o devolver los problemas como prosa/array JSON en vez de una línea JSONL por problema.

---

## 11. Fase 7 — Tests unitarios (describirlos)

**Común a ambos modos** (Generar/Regenerar y Revisar/Modificar): una vez el diseño está conforme (`OK-CORRECTO`), un subagente **test-unitarios** escribe `design/test-unit-desc.md` con lo que el **contrato de tests unitarios** de la plantilla activa declare para este artefacto. **Solo descripción, sin código**: qué hay que cubrir ahí, y qué se hace después con `test-unit-desc.md`, lo declara la plantilla activa.

1. **Lanzar el subagente test-unitarios** (uno solo). Produce `design/test-unit-desc.md` siguiendo el contrato que la plantilla prescribe para los tests unitarios (lo descubre vía el README). Ante una `test-unit-desc.md` previa, la **regenera** para reflejar el diseño actual.
2. Cuando responda **exactamente** `ESCRITO: test-unit-desc.md`, continúa con la Fase 8.
3. Si no produce `test-unit-desc.md` o no devuelve el token, **reintenta 1 vez**; si vuelve a fallar, avísalo al usuario y continúa con la Fase 8 (**MUST NOT** bloquear el flujo por esto).

**Prompt del subagente test-unitarios**:

> Eres un experto en **tests unitarios** del stack del proyecto. Tu tarea es **describir** —no implementar— los tests unitarios que el contrato de la plantilla declare para un diseño, con el alcance, la forma y la trazabilidad que ese contrato fije.
>
> - **Reglas para el diseño y para los tests unitarios**: lee `{ruta de <plantilla-activa>/README.md}` y **los ficheros que referencie** —en particular el contrato de los **tests unitarios**—. Define el alcance de los tests, la plantilla exacta de `test-unit-desc.md`, la trazabilidad y el checklist. Síguelo al pie de la letra.
> - **Especificación**: lee `{ruta de specification.md}` y todos los ficheros que enlace (para los mensajes/semántica exactos de cada regla).
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño**: lee la carpeta `{iniciativa}/design` —sobre todo `design.md`— como **fuente de verdad** de lo que el contrato de tests unitarios de la plantilla declare que hay que cubrir. **CRITICAL**: en esta fase **todavía no existe el código** que el diseño planifica (lo creará `/sdd-implementer`); toma **del diseño** todo lo que describas, no del árbol de fuentes. Para lo que el diseño **modifica** (ya existente) o para las piezas de las que dependa, puedes explorar el código real.
> - **Salida**: escribe `{iniciativa}/design/test-unit-desc.md` **según la plantilla exacta del contrato**. **MUST NOT** escribir código de tests (ni `@Test`, ni imports, ni cuerpos): solo la descripción, con los apartados, los campos y la trazabilidad que el contrato declare.
> - **MUST NOT** usar `AskUserQuestion`. Ante una ambigüedad, decide lo más razonable y documéntalo en `test-unit-desc.md`.
> - Aplica el **checklist** del contrato antes de terminar (**LIMIT**: 3 iteraciones de autocorrección).
> - Al terminar, responde **exactamente** `ESCRITO: test-unit-desc.md` y, opcionalmente, 1-2 líneas de notas (cobertura). **MUST NOT** pegar el contenido de `test-unit-desc.md` en la respuesta (ya está en disco).

- ✅ CORRECTO (respuesta del subagente test-unitarios): `ESCRITO: test-unit-desc.md`
- ❌ INCORRECTO: `He creado los tests` (token no parseable), pegar `test-unit-desc.md` en la respuesta, o incluir código de tests en `test-unit-desc.md` (la fase solo describe)

---

## 12. Fase 8 — Verificar y corregir los tests unitarios (bucle, LIMIT 10)

**Común a ambos modos** (Generar/Regenerar y Revisar/Modificar). Una vez `test-unit-desc.md` está escrito (Fase 7), comprueba **en bucle** que es **coherente con el diseño** aplicando las **comprobaciones de coherencia** que declare la plantilla activa (cuáles son es propio de cada artefacto: el motor **MUST NOT** enumerarlas aquí). Sobre la carpeta `design/`, repite este bucle **como máximo 10 veces** (**LIMIT**: 10 iteraciones); lleva un contador de iteración `{k}` empezando en 1:

1. **Lanzar el subagente verificador-test-unitarios** (uno solo).
2. **Volcar su respuesta a `design/log_revision_unit-test.txt`**: añade (append) la respuesta **literal** —sus líneas JSONL, o `OK-CORRECTO`— precedida de la cabecera `# Verificación tests unitarios — iteración {k}`. Es un append acumulativo (una sección por iteración).
3. Si respondió **exactamente** `OK-CORRECTO` → `test-unit-desc.md` es coherente con el diseño: sal del bucle y ve a la Fase 9 (cierre).
4. Si respondió **cualquier otra cosa** (las líneas JSONL de problemas): **MUST** mostrar al usuario por pantalla, tal cual, las líneas JSONL que devolvió (bloque ` ```jsonl `), antes de continuar; luego **lanza el subagente corrector-test-unitarios** pasándole esas mismas líneas, para que corrija en sitio sobre `design/test-unit-desc.md`.
5. Incrementa `{k}` y vuelve al paso 1.

Si tras la 10ª iteración el verificador sigue sin responder `OK-CORRECTO` → **STOP** (STOP condition): muestra al usuario las líneas JSONL residuales y **MUST NOT** dar `test-unit-desc.md` por bueno.

**Prompt del subagente verificador-test-unitarios**:

> Eres un experto en **tests unitarios** del stack del proyecto, que tienes que verificar si la **descripción de los tests unitarios** ya escrita es **coherente con el diseño**. **MUST NOT** regenerar ni completar los tests: solo **detectas y reportas** incoherencias.
>
> - **Reglas para el diseño y para los tests unitarios**: lee `{ruta de <plantilla-activa>/README.md}` y **los ficheros que referencie** —en particular el contrato de los **tests unitarios** y, dentro de él, sus **comprobaciones de coherencia con el diseño**—. Aplícalas tal cual.
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño**: lee la carpeta `{iniciativa}/design` —sobre todo `design.md`— como **fuente de verdad** frente a la que se comprueba la coherencia. Para lo que el diseño **modifica** (ya existente) puedes explorar el código real.
> - **Fichero a verificar**: `{iniciativa}/design/test-unit-desc.md`.
>
> **Formato de salida (REQUIRED)**:
> - Si **no** has encontrado nada incoherente, responde **exactamente** y solo: `OK-CORRECTO`.
> - Si has encontrado problemas, responde **únicamente** con líneas **JSONL** (JSON Lines): **un problema por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:
>   - `id` — identificador correlativo, formato `P-NNN` (`P-001`, `P-002`, …).
>   - `severidad` — uno de `BLOCKING` | `IMPORTANT` | `MINOR`.
>   - `fichero` — siempre `design/test-unit-desc.md`, o `null` si es transversal.
>   - `ubicacion` — la clase/método/test concreto dentro del fichero; `null` si no aplica.
>   - `origen` — la clase/método/regla del diseño que se incumple (p.ej. `Clase NotaServiceImpl`, `método validateInsert`, `V-Nota-003`), o `null`.
>   - `problema` — descripción clara de la incoherencia (p.ej. clase/método inexistente en el diseño, regla inexistente, cobertura que no cuadra).
>   - `correccion` — qué hay que cambiar en `test-unit-desc.md` para resolverlo.
> - Cada línea **MUST** ser JSON válido en una sola línea (escapa los saltos como `\n`). **MUST NOT** añadir comentarios ni texto fuera de las líneas JSONL.
>
> Ejemplo de salida con problemas:
>
> ```jsonl
> {"id":"P-001","severidad":"BLOCKING","fichero":"design/test-unit-desc.md","ubicacion":"Clase NotaCalculator","origen":"Clase NotaCalculator","problema":"Se describen tests para la clase NotaCalculator, que no existe en el diseño (design.md no la define).","correccion":"Eliminar la sección de NotaCalculator o sustituirla por la clase real del diseño que hace ese cálculo."}
> {"id":"P-002","severidad":"IMPORTANT","fichero":"design/test-unit-desc.md","ubicacion":"NotaServiceImpl, test validateUpdate_…","origen":"método validateUpdate","problema":"El test ejerce validateUpdate, pero el diseño solo define validateInsert para NotaServiceImpl.","correccion":"Reasignar el test al método real o eliminarlo si la regla no aplica en update."}
> ```

**Prompt del subagente corrector-test-unitarios**:

> Eres un experto en **tests unitarios** del stack del proyecto, que tienes que corregir las incoherencias detectadas en la **descripción de los tests unitarios**. Deberás indicar de la forma más clara posible las incoherencias que has corregido.
>
> - **Reglas para el diseño y para los tests unitarios**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie (el contrato de los tests unitarios).
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño**: la carpeta `{iniciativa}/design` —sobre todo `design.md`— es la **fuente de verdad**; **MUST NOT** modificar el diseño para que cuadre con los tests: corrige los tests para que cuadren con el diseño.
> - **Fichero a corregir**: `{iniciativa}/design/test-unit-desc.md` — corrige **en sitio** (`Edit`/`Write`), respetando la plantilla del contrato; no toques otros ficheros del diseño.
> - **Problemas a corregir** (los reportó el verificador-test-unitarios, en formato JSONL, un problema por línea): `{líneas JSONL literales del verificador}`. Aplica cada `correccion` en la `ubicacion` indicada.

- ✅ CORRECTO (respuesta del verificador-test-unitarios sin problemas): `OK-CORRECTO`
- ✅ CON PROBLEMAS (una línea JSONL por problema, sin texto alrededor): `{"id":"P-001","severidad":"BLOCKING","fichero":"design/test-unit-desc.md","ubicacion":"…","origen":"…","problema":"…","correccion":"…"}`
- ❌ INCORRECTO: `Todo correcto ✅` (token no exacto; el skill compara por literal), o devolver los problemas como prosa/array JSON en vez de una línea JSONL por problema.

---

## 13. Fase 9 — Mensaje de cierre al usuario

```
Diseño guardado en .sdd/drafts/{carpeta-iniciativa}/design/

  - design.md
  - {resto de ficheros y carpetas según la estructura que define la plantilla}
  - test-unit-desc.md (lo que la plantilla activa declare para los tests unitarios)

Verificación del diseño: OK-CORRECTO (tras {N} iteración(es) de verificar/corregir).
Tests unitarios: descritos en design/test-unit-desc.md (coherencia con el diseño: OK-CORRECTO).

Si quieres iterar sobre este diseño, puedes:
  1. Editar (o crear) .sdd/drafts/{carpeta-iniciativa}/design-guidelines.md con guías
     adicionales. Debe empezar con:
       ---
       type: design-guidelines
       ---
  2. Re-ejecutar:
     /sdd-designer .sdd/drafts/{carpeta-iniciativa}/specification.md
     (se volverá a generar desde cero).

Para implementar este diseño tal cual ejecuta:
  /sdd-implementer .sdd/drafts/{carpeta-iniciativa}/design/design.md
```

Ajusta la lista de ficheros a la estructura real que define la plantilla. **MUST NOT** lanzar `/sdd-implementer` tú mismo: el usuario decide cuándo.

---

## 14. Modo Revisar/Modificar (`design/` existente)

Ruta alternativa desde la Fase 0 (§4.4) cuando el `design/` ya existe y el usuario elige "Revisar / modificar". **No regenera** (no lanza diseñadores ni torneo, **ni enriquece** — la Fase 5 es solo de Generar/Regenerar): aplica los cambios puntuales que pida el usuario, pasa el bucle verificar/corregir del diseño y regenera **y verifica** los tests unitarios, **preservando las ediciones manuales**.

1. Ejecutar la **Fase 1 (§5)**: leer `<plantilla-activa>/README.md` y resolver las rutas de entrada (spec, guías si existen).
2. Leer `design.md`. Si su frontmatter no es `type: design` → **ERROR** y detente.
3. **Aplicar los cambios pedidos** (si el usuario pasó texto de cambios en el prompt): lánzalos como tarea del subagente **corrector**, pasándole los cambios como la "lista a corregir" y la carpeta `design/`; corrige **en sitio**. Si no hubo cambios pedidos, salta este paso.
4. **Pasar la Fase 6 (§10)**: bucle verificar/corregir sobre `design/` (**LIMIT** 10) hasta `OK-CORRECTO`.
5. **Pasar la Fase 7 (§11, Tests unitarios)**: lanza el subagente **test-unitarios** para (re)generar `design/test-unit-desc.md` reflejando el diseño ya modificado.
6. **Pasar la Fase 8 (§12)**: bucle `verificador-test-unitarios` → `corrector-test-unitarios` sobre `design/test-unit-desc.md` (**LIMIT** 10) hasta `OK-CORRECTO`.
7. **Cerrar** con un mensaje análogo al de la Fase 9, indicando los cambios aplicados y el resultado de la verificación. Si nada hubo que tocar y el verificador respondió `OK-CORRECTO` a la primera: `La carpeta design/ ya está conforme. No se ha modificado nada.`

**MUST NOT** reconstruir el diseño desde el spec en este modo. Si el verificador detecta que falta una pieza estructural completa, repórtalo al usuario en el cierre; **MUST NOT** regenerar el diseño entero.

---

## Quick Guidelines

- **CRITICAL — agnosticismo**: este SKILL es un **motor de flujo**; **no sabe nada de cómo es el diseño**. Todo lo específico lo define `<plantilla-activa>/README.md` (configurable con `--template-dir`), que **leen los subagentes** de disco. **MUST NOT** nombrar aquí ficheros, identificadores, taxonomías ni validaciones del diseño. Único contrato fijo: entrada `specification.md` (`type: specification`), salida carpeta `design/` con `design.md` (`type: design`).
- **Dos modos** (§4.4): sin `design/` → Generar (Fases 1-9). Con `design/` → preguntar Regenerar (pisa) vs **Revisar/Modificar** (§14: aplica cambios puntuales + verifica el diseño + regenera y verifica tests unitarios, **sin regenerar ni enriquecer**).
- **Diseñar** (§6): **CRITICAL** exactamente 5 subagentes diseñadores en **una única respuesta**, cada uno escribe `design_<n>/` completo; **MUST NOT** `AskUserQuestion` ni `run_in_background`. Responden `ESCRITO: design_<n>`.
- **Elegir** (§7): torneo acumulativo de un juez de dos en dos (`ganador = juez(ganador, design_i)`), **secuencial**; el juez responde `GANADOR: design_<n>` + `=== VENTAJAS <carpeta-A> ===` + `=== DEFECTOS <carpeta-A> ===` + `=== VENTAJAS <carpeta-B> ===` + `=== DEFECTOS <carpeta-B> ===` + `=== JUSTIFICACIÓN ===`. **REQUIRED**: el motor **MUST** mostrar por pantalla las ventajas y los defectos de cada diseño y la justificación tras cada comparación, y **MUST** acumular (append) en `{iniciativa}/log_best.txt` los bloques de ventajas y defectos **más** un bloque `=== JUSTIFICACION <carpeta-A> vs <carpeta-B> ===` con el contenido de la justificación (para auditar luego si el ganador cumple las ventajas, para sanear en la Fase 5 los defectos que el juez le detectó, y para dejar registrado por qué se eligió cada diseño).
- **Seleccionar** (§8): renombrar el ganador a `design/`, mover `log_best.txt` dentro de la salida, borrar el resto.
- **Enriquecer y sanear** (§9, solo Generar): un subagente **enriquecedor** lee `log_best.txt` y reporta (en JSONL `M-NNN` con campo `tipo` `VENTAJA`/`DEFECTO-GANADOR`, o `OK-SIN-MEJORAS`) qué ventajas de los descartados faltan en el ganador **y** qué defectos que el juez le atribuyó al propio ganador siguen presentes; el motor las muestra y un **corrector** las aplica (incorpora las ventajas y corrige los defectos). Luego sigue la Fase 6.
- **Verificar/corregir el diseño** (§10): bucle verificador → corrector hasta `OK-CORRECTO` (**LIMIT** 10; tras la 10ª, **STOP**). El verificador valida los artefactos como prescriba la plantilla (incluido ejecutar los scripts de validación que ella indique). El motor **MUST NOT** ejecutar esas validaciones él mismo (§2.2). El verificador reporta los problemas en **JSONL** (un problema por línea, campos `id`/`severidad`/`fichero`/`ubicacion`/`origen`/`problema`/`correccion`); el motor **MUST** mostrárselos al usuario en cada iteración con problemas y **MUST** volcar la respuesta literal de cada verificador a `design/log_revision.txt` (una sección por iteración).
- **Tests unitarios** (§11, ambos modos): un subagente **test-unitarios** produce `design/test-unit-desc.md` con el contenido que declare la plantilla activa — **solo descripción, sin código**. Se apoya en el **diseño**, no en el árbol de fuentes (aún no hay `.java`); responde `ESCRITO: test-unit-desc.md`.
- **Verificar/corregir tests unitarios** (§12, ambos modos): bucle `verificador-test-unitarios` → `corrector-test-unitarios` hasta `OK-CORRECTO` (**LIMIT** 10; tras la 10ª, **STOP**). Comprueba que `test-unit-desc.md` es **coherente con el diseño** aplicando las comprobaciones de coherencia que declare la plantilla activa; JSONL con los campos `id`/`severidad`/`fichero`/`ubicacion`/`origen`/`problema`/`correccion`; vuelca el JSONL a `design/log_revision_unit-test.txt`.
- **CRITICAL — confinamiento de escritura** (§2.4): el diseño es un **plan**. Ni el motor ni ningún subagente escriben/editan/borran fuera de la carpeta de la iniciativa (`design_<n>/`, `design/`, más los logs de orquestación); **MUST NOT** tocar `src/**`, `axelor-config.properties`, `build.gradle` ni ningún artefacto del proyecto real. Todo cambio fuera de la carpeta se **documenta dentro del diseño** para que lo aplique `/sdd-implementer`, nunca se aplica aquí.
- **Contrato de tokens** (§2.3): el skill compara por literal exacto — `ESCRITO: design_<n>`, `GANADOR: design_<n>`, `OK-CORRECTO`. Los subagentes **MUST NOT** pegar el diseño en su respuesta (ya está en disco).
- **MUST NOT** lanzar `/sdd-implementer` tú mismo: indica el comando y **STOP**.

---

## Apéndice A — Override de rutas (para testing y versatilidad)

- `--template-dir=<ruta>` — **carpeta de plantillas** alternativa a la resuelta en §2.2. Tiene prioridad, salvo la mezcla de plantillas prohibida (§2.2 → **ERROR**). Es además **obligatorio** cuando la iniciativa declara `template: external` (§2.2): sin él → **ERROR**. **MUST** contener un `README.md` (la guía, que declara todo lo específico y referencia los demás ficheros); si falta → **ERROR** y detente. El skill resuelve `README.md` contra esta carpeta y pasa esa ruta a los subagentes; **MUST NOT** resolver ni ejecutar ningún otro fichero de la carpeta (cualquier script de validación lo descubre y ejecuta el verificador vía el README — §2.2). Ese `README.md` **MUST** estar redactado para los **8 roles** que lanza este skill (diseñador, juez, enriquecedor, verificador, corrector, test-unitarios, verificador-test-unitarios, corrector-test-unitarios — ver §2.2), no solo para el diseñador. Permite usar el mismo flujo con otro tipo de artefacto sin tocar el código del skill.
- `--in=<ruta>` — fichero `specification.md` de entrada explícito. **Desactiva la elección de iniciativa** de la Fase 0 caso 2. La "carpeta de la iniciativa" es la que lo contiene.
- `--out=<ruta>` — **carpeta** donde queda el diseño final (sustituye a `{carpeta-iniciativa}/design/` en las Fases 4-8). Los borradores `design_<n>/` se crean junto a `specification.md`; el ganador se mueve a `--out=`.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Las rutas relativas se resuelven contra esta raíz.

En uso normal no se especifican: se usa la carpeta de plantillas resuelta por el frontmatter `template:`, la carpeta de la iniciativa y `.sdd/drafts/`.
