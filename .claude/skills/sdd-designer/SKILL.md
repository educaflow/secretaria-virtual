---
name: sdd-designer
description: Segundo paso del pipeline SDD. Dada una especificación funcional (`specification.md`, `type: specification`) producida por `/sdd-specification`, genera un plan de DISEÑO en una carpeta `design/` (índice `design.md`, `type: design`) que consume `/sdd-implementer`: 5 diseñadores en paralelo, un juez por torneo que entre los que cumplen la spec elige por calidad, y un bucle verificar/corregir sobre el ganador. Es un MOTOR genérico y agnóstico al artefacto: aporta solo el flujo y delega TODO lo específico del diseño en el `README.md` de la carpeta de plantillas activa (`template-<nombre>/`, la que declare el frontmatter `template:` del `specification.md`, configurable con `--template-dir` y propagada al frontmatter del `design.md`), así que cambiar de plantilla cambia por completo qué y cómo se diseña sin tocar este skill.
handoffs:
  - label: Implementar el diseño
    agent: sdd-implementer
    prompt: Implementar el diseño recién generado en .sdd/drafts/{carpeta-iniciativa}/design/design.md
---

# sdd-designer

Eres un **motor de diseño** del pipeline SDD: transformas una **especificación funcional** en un **plan de diseño** (no una implementación). La entrada la produce `/sdd-specification` y la salida la consume `/sdd-implementer`.

**CRITICAL — eres agnóstico al artefacto.** Este `SKILL.md` define **solo el flujo y la orquestación de agentes**. **No sabe nada de qué se diseña** (ni qué ficheros tiene la spec más allá de su índice, ni qué reglas, taxonomías, capas, ficheros de salida, formatos o validaciones existen): **todo eso lo declara la guía `<plantilla-activa>/README.md`**, que los subagentes leen como contrato. **MUST NOT** asumir de memoria ningún detalle del diseño; **MUST NOT** nombrar ficheros, identificadores, taxonomías ni validaciones concretas en este skill (únicas excepciones: los contratos fijos del motor `specification.md`, `design-guidelines.md`, `design.md`, `decisiones.md` y `test-unit-desc.md` — §1.2). Así, apuntar `--template-dir` a otra carpeta de plantillas con un README distinto cambia por completo el diseño producido **sin tocar este skill**.

El skill tiene **dos modos** (se decide en la Fase 0, §4.4, según exista o no `design/design.md`):

- **Generar/Regenerar** (Fases 1-9): produce el `design/` desde cero (5 diseñadores en paralelo → torneo del juez → renombrar ganador → enriquecer → bucle verificar/corregir diseño → describir tests unitarios → bucle verificar/corregir tests unitarios). Modo por defecto cuando no hay `design/design.md`.
- **Revisar/Modificar** (§14): re-invocación sobre un `design/design.md` existente. **No regenera**: aplica los cambios puntuales que pida el usuario y pasa el bucle verificar/corregir, preservando las ediciones manuales.

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

1. **Fase 0 — Localizar** la iniciativa y su `specification.md`, las guías opcionales, y **decidir el modo** según exista o no `design/design.md` (§4.4).
2. **Fase 1 — Cargar** el contrato (`<plantilla-activa>/README.md`) y resolver las rutas de entrada que se pasarán a los subagentes. (Común a ambos modos.)
3. **Fase 2 — Diseñar**: lanzar **5 subagentes diseñadores en paralelo** con el modelo más capaz; cada uno escribe primero `decisiones.md` (decisiones difíciles con ≥2 alternativas) y luego un diseño completo en `design_<n>/`. (Solo Generar/Regenerar.)
4. **Fase 3 — Elegir**: un subagente **juez** decide por **torneo** (ganador acumulado vs siguiente diseño) hasta quedar uno, premiando la **calidad** entre los que cumplen la spec (`k-code-quality/disenyo.md`) y desempatando por `decisiones.md`, **justificando y mostrando por pantalla** en cada comparación por qué elige un diseño frente al otro. (Solo Generar/Regenerar.)
5. **Fase 4 — Seleccionar**: renombrar la carpeta ganadora a `design/`, mover `log_best.txt` y borrar el resto. (Solo Generar/Regenerar.)
6. **Fase 5 — Enriquecer y sanear**: un subagente **enriquecedor** revisa, a partir de `log_best.txt`, (a) qué ventajas de los diseños descartados faltan en el ganador y tienen sentido, y (b) qué **defectos/errores que el juez atribuyó al propio ganador** siguen presentes; reporta unas y otros como mejoras a implementar y un subagente **corrector** las aplica. (Solo Generar/Regenerar.)
7. **Fase 6 — Verificar/corregir**: bucle subagente verificador (que aplica la validación que prescriba la plantilla) → (si hay fallos) subagente corrector, hasta `OK-CORRECTO`. (Común a ambos modos.)
8. **Fase 7 — Tests unitarios**: un subagente **test-unitarios** describe en `design/test-unit-desc.md` los tests unitarios que declare el contrato de la plantilla activa (solo descripción, sin código). (Común a ambos modos.)
9. **Fase 8 — Verificar/corregir tests unitarios**: bucle subagente **verificador-test-unitarios** (comprueba que `test-unit-desc.md` es coherente con el diseño) → (si hay fallos) subagente **corrector-test-unitarios**, hasta `OK-CORRECTO`. (Común a ambos modos.)
10. **Fase 9 — Cerrar** con mensaje al usuario y handoff a `/sdd-implementer`.
11. **§14 — Modo Revisar/Modificar**: ruta alternativa desde la Fase 0.

**STOP conditions**:

- `--template-dir=` apunta a una carpeta que **no contiene `README.md`** (la guía que declara todo lo específico) → **ERROR** y detente.
- `--template-dir=` apunta a otra carpeta `template-*/` de este skill **distinta** de la que declara el frontmatter `template:` de la spec (o a cualquier `template-*/` interna cuando la spec declara `template: external`), o el `template:` de la spec no resuelve a ninguna carpeta de plantillas (§2.2) → **ERROR** y detente: **MUST NOT** mezclarse plantillas — sus arquitecturas no son compatibles.
- El frontmatter `template:` de la iniciativa vale `external` (se especificó con una plantilla externa) y **no** se pasa `--template-dir=` (§2.2) → **ERROR** y detente pidiéndolo. **MUST NOT** preguntar la plantilla ni caer a una carpeta interna.
- Frontmatter de `specification.md` no contiene `type: specification` → **ERROR** y detente.
- Sin ruta y sin ninguna iniciativa con `specification.md` en `.sdd/drafts/` (§4.2) → **STOP** y pide una ruta.
- Ya existe `design-guidelines.md` y se pasa texto de guías por el prompt en modo Generar/Regenerar (§4.3) → **ERROR** y detente sin tocar nada.
- `design-guidelines.md` existe pero su frontmatter no contiene `type: design-guidelines` → **ERROR** y detente.
- Existe `design/design.md` en la iniciativa → **STOP** y pregunta: Regenerar vs Revisar/Modificar (§4.4).
- En modo Revisar/Modificar, el frontmatter de `design.md` no es `type: design` → **ERROR** y detente (§14).
- En modo Revisar/Modificar, `design.md` declara un `template:` que no coincide con `{template}` resuelto en §2.2 → **ERROR** y detente (§14): **MUST NOT** mezclarse plantillas.
- Ningún diseñador produjo una carpeta `design_<n>/` válida (con contenido y `decisiones.md`, §6) → **ERROR** y detente.
- El juez no devuelve un token `GANADOR: design_<n>` válido tras 1 reintento → **STOP** y muestra el problema.
- Tras **10** iteraciones del bucle verificar/corregir del diseño (Fase 6) el verificador sigue sin responder `OK-CORRECTO` → **STOP** y muestra al usuario las líneas JSONL de los problemas residuales. **MUST NOT** dar el diseño por bueno.
- Tras **10** iteraciones del bucle verificar/corregir de los tests unitarios (Fase 8) el `verificador-test-unitarios` sigue sin responder `OK-CORRECTO` → **STOP** y muestra al usuario las líneas JSONL residuales. **MUST NOT** dar `test-unit-desc.md` por bueno.

---

## 1. Entrada y salida

### 1.1 Entrada

La **especificación** de la iniciativa, cuyo índice es `specification.md` (único fichero de entrada obligatorio con nombre fijo; debe contener `type: specification`). El índice enlaza otros ficheros en su carpeta — el skill **no asume cuáles son** (los define la plantilla de `/sdd-specification`); los subagentes los leen siguiendo el índice.

Opcionalmente, en la carpeta de la iniciativa puede existir `design-guidelines.md` (frontmatter `type: design-guidelines`) con guías técnicas que orientan el diseño. Si existe, se pasa **tal cual** a los subagentes. Si **no existe**, simplemente no se pasa.

### 1.2 Salida

Una **carpeta** `design/` dentro de la carpeta de la iniciativa.

**CRITICAL — la estructura interna de `design/` la define `<plantilla-activa>/README.md`, no este skill.** Qué ficheros y subcarpetas la componen, qué contiene cada uno y cómo se valida, **lo declara la guía**, que los subagentes leen. El skill **MUST NOT** asumir esos detalles de memoria; solo manipula la carpeta como una unidad (la crea cada diseñador, el juez la compara, el verificador la valida).

**Primer contrato fijo (no lo cambia `--template-dir`):** el índice de la salida se llama `design.md` y lleva frontmatter `type: design` **más `template: {template}`** (el valor resuelto en §2.2 pasos 1-2 —el de la spec cuando existe— y heredado según §2.2 paso 4). Es lo que el skill usa para **localizar y validar** un diseño existente (Fase 0 / §14) y lo que consume `/sdd-implementer`, que resuelve con esa clave su propia carpeta de plantillas.

**`decisiones.md` — el registro de decisiones del diseñador (§2.5).** Segundo contrato fijo, independiente de la plantilla: cada diseñador lo escribe **antes** que el diseño, en la raíz de su `design_<n>/`, con la plantilla literal de §6. Es contenido de diseño (lo lee el juez para comparar, y `/sdd-implementer` y el humano para entender por qué el diseño es como es), pero **no lo declara la plantilla**: su verificador **MUST** ignorarlo en el inventario de ficheros, igual que los logs.

**`test-unit-desc.md`, `design-guidelines.md` y `specification.md` — los otros tres contratos fijos.** El motor fija solo el **nombre**: `design/test-unit-desc.md` es la descripción de tests unitarios que escribe la Fase 7 (§11; su contenido lo declara la plantilla); `design-guidelines.md` son las guías opcionales de entrada y `specification.md` el índice de entrada (ambos en §1.1).

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
            ├── decisiones.md                    ← decisiones difíciles con alternativas, del diseñador (§2.5, §6)
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

1. `--template-dir=<ruta>` explícito → esa carpeta (válvula de testing). **ERROR** si apunta a otra carpeta `template-*/` de este skill **distinta** de la que declara la iniciativa, o a **cualquier** `template-*/` interna cuando la iniciativa declara `template: external` (mezcla de arquitecturas, STOP condition). **`{template}`** con el flag: si la ruta es una `template-<nombre>/` interna de este skill, `<nombre>`; si es externa, el `template:` de la spec cuando lo declara y `external` cuando no.
2. Sin flag → lee la clave `template:` del frontmatter del `specification.md` de la iniciativa y usa `template-<valor>/`. Si la clave falta (spec anterior a este contrato) → pregúntala con `AskUserQuestion` (una opción por carpeta `template-*/` del skill). El valor así obtenido —el de la spec o, si falta, el elegido en la pregunta— es **`{template}`**: el motor lo pasa a los subagentes (§6) y es lo que hereda el `design.md` (paso 4). Si `template-<valor>/` no existe en este skill → **ERROR** indicando las disponibles o que se pase `--template-dir=`.
3. **Valor reservado `template: external`** (la iniciativa se especificó con un `--template-dir` externo): la plantilla activa **solo** puede venir de `--template-dir=`, apuntando a la carpeta externa que corresponda a **este** skill. Si el flag no viene → **ERROR** y detente pidiéndolo; **MUST NOT** preguntar la plantilla, **MUST NOT** caer a una carpeta interna (mezclaría arquitecturas en silencio) y **MUST NOT** buscarse `template-external/`. `external` no es una ruta ni una clave ausente: cada skill tiene su propia carpeta de plantillas, así que la ruta externa de un skill no designa nada en otro.
4. **Herencia hacia abajo:** el `design.md` producido lleva en su frontmatter **`template: {template}`** —el valor resuelto en el paso 1 o 2: el de la spec copiado verbatim, incluido `external`, o el preguntado/deducido del flag si la spec no lo tenía—, para que los skills posteriores la resuelvan sin volver a la spec. **MUST NOT** sustituirse por la ruta del `--template-dir` de este skill: sería la ruta de la carpeta de plantillas **del designer**, que no designa nada en `/sdd-implementer` ni en los skills de tests.

**En todo el resto del skill, `<plantilla-activa>/README.md` denota «el `README.md` de la carpeta de plantillas activa» resuelta aquí.**

Todo lo específico del diseño (qué se produce, cómo se convierte el spec, qué contexto cargar, cómo se valida) lo define `<plantilla-activa>/README.md` y los ficheros que él referencie. Los subagentes los **leen de disco**; el skill **MUST NOT** asumirlos, restatarlos ni hardcodearlos aquí. El skill solo pasa a cada subagente **las rutas** de los ficheros de entrada y su rol.

**CRITICAL — `README.md` es el ÚNICO fichero de la plantilla que el motor conoce por nombre.** El skill **MUST NOT** nombrar, leer, resolver ni **ejecutar** ningún otro fichero de la plantilla (ni los documentos que el README referencie, ni ningún script de validación que la plantilla traiga). Esos ficheros los descubren y usan **los subagentes** leyendo el `README.md`. En particular:

- Si la plantilla prescribe una validación que se ejecuta como **comando o script** (p.ej. validar con una herramienta externa los artefactos generados), **la ejecuta el subagente verificador** —que lee la plantilla y la descubre—, **NUNCA el motor**.
- **MUST NOT** añadir "pasos de `Bash`" en este skill que corran validaciones, comprobaciones o herramientas específicas del diseño. El motor solo usa `Bash`/`Write` para orquestación **agnóstica** (listar `.sdd/drafts/`, `mv`/`rm` de carpetas `design_<n>/`, y escribir sus propios **logs de orquestación** `log_best.txt`/`log_revision.txt`/`log_revision_unit-test.txt` — §7/§10/§12), nunca para validar el contenido del diseño.
- Esos tres logs son artefactos **del motor**, no contenido de diseño ni ficheros que declare la plantilla: el verificador no los valida y `--template-dir` no los cambia.
- Único acoplamiento permitido por nombre: `README.md` (contrato de la plantilla) y los contratos fijos del motor `specification.md` / `design-guidelines.md` / `design.md` / `decisiones.md` / `test-unit-desc.md` (§1.2).

**REQUIRED — el README de la plantilla es leído por los 8 roles.** Cualquier `README.md` de plantilla (cualquier `template-<nombre>/` del skill o una externa apuntada con `--template-dir=`) **MUST** delimitar, por rol, qué tarea hace y qué ficheros de la plantilla le aplican: los ocho reciben las mismas rutas de entrada y leen el mismo README, pero cada uno necesita un subconjunto distinto. Un README que solo contemple al diseñador es **incompleto** para este skill. Los roles (orquestación en §2.3):

- **diseñador** — crea el diseño — §6
- **juez** — elige entre dos diseños — §7
- **enriquecedor** — detecta qué ventajas de los descartados incorporar y qué defectos del propio ganador sanear — §9
- **corrector** — corrige/incorpora en el diseño — §9, §10
- **verificador** — busca problemas en el diseño — §10
- **test-unitarios** — describe los tests unitarios — §11
- **verificador-test-unitarios** — comprueba que los tests unitarios son coherentes con el diseño — §12
- **corrector-test-unitarios** — corrige los tests unitarios — §12

### 2.3 Orquestación de subagentes

- Los **diseñadores** corren **en paralelo** (§6); **MUST NOT** usar `AskUserQuestion`. No devuelven bloque `=== DUDAS ===` (desviación deliberada de k-skill §6.8): las suposiciones quedan en `decisiones.md`, que el humano lee en la salida, y mostrarlas además por pantalla las duplicaría. El **juez**, el **enriquecedor**, el **verificador**, el **corrector**, el **test-unitarios**, el **verificador-test-unitarios** y el **corrector-test-unitarios** corren **de uno en uno** (cada uno depende del resultado del anterior).
- **MUST NOT** usar `run_in_background`: el skill necesita el resultado de cada subagente para continuar.
- Cada rol responde con un **token literal** que el skill parsea (definidos en cada fase). El skill compara por literal exacto. Los tres correctores (§9, §10, §12) cierran con `CORREGIDO`: el motor **no ramifica** sobre él, solo comprueba que llegó y continúa con el siguiente paso.
- **REQUIRED — modelo de los diseñadores y del juez.** Diseñar y elegir son los pasos donde más rinde pensar. Lanza los **5 diseñadores** y **cada comparación del juez** con el **modelo más capaz disponible**: pasa `model` en la invocación a `Agent` con el modelo del propio orquestador o uno superior. **MUST NOT** dejar que estos dos roles caigan en un modelo rápido o pequeño (ni por omisión ni por un modelo por defecto de subagentes configurado más bajo). El resto de roles pueden heredar el modelo por defecto.

### 2.4 Confinamiento de escritura — nunca fuera de la carpeta de la iniciativa

El diseño es un **plan**, no una implementación. Ni el motor ni ningún subagente tocan el árbol real del proyecto.

- **CRITICAL — el motor y los 8 subagentes MUST NOT escribir, crear, editar, mover ni borrar NINGÚN fichero fuera de la carpeta de la iniciativa** (`{iniciativa}/design_<n>/` o `{iniciativa}/design/`, según la fase; con `--out=`, la carpeta de salida indicada). En particular **MUST NOT** tocar código fuente (`src/**`), ficheros de configuración (p.ej. `axelor-config.properties`, `build.gradle`, cualquier `*.properties`/`*.yml` o `*.xml` del proyecto real), datos iniciales, ni cualquier otro artefacto del árbol del proyecto.
- **REQUIRED — todo cambio fuera de la carpeta se DOCUMENTA, no se aplica.** Si el diseño **requiere** un cambio fuera de la carpeta de la iniciativa (una propiedad de configuración nueva, una clase existente que modificar, una dependencia, un script), ese cambio **MUST** quedar **descrito dentro del diseño** (en el fichero de `design/` que prescriba la plantilla) para que lo aplique `/sdd-implementer`. **MUST NOT** aplicarlo aquí.
- El único acceso de escritura del motor fuera del contenido de diseño son sus propios **logs de orquestación** dentro de la carpeta de la iniciativa (`log_best.txt`, `log_revision.txt`, `log_revision_unit-test.txt`), las operaciones `mv`/`rm` sobre las carpetas `design_<n>/`/`design/` y `log_best.txt` (§6, §8), la creación de `design-guidelines.md` a partir del prompt (§4.3) y añadir `template:` al frontmatter de `design.md` cuando le falta (§14). Nada más.

- ✅ CORRECTO: el diseño necesita `correos.reintentos.max=3` → se documenta como propiedad de configuración a añadir en el fichero de diseño que la plantilla destine a configuración; `/sdd-implementer` la escribirá en `axelor-config.properties`.
- ❌ INCORRECTO: un subagente edita `src/main/resources/axelor-config.properties` para añadir la propiedad (escritura fuera de la carpeta de la iniciativa; es trabajo de `/sdd-implementer`)

---

### 2.5 Calidad del diseño — pensar antes de escribir y elegir por calidad

Un diseño que cumple la especificación y el contrato **puede ser una chapuza**: completo, obediente y lleno de conocimiento tácito. Este motor lo evita con cuatro mecanismos, todos **agnósticos al artefacto**:

1. **El diseñador piensa antes de escribir.** Antes de producir el diseño, escribe `decisiones.md` (plantilla literal en §6): las decisiones difíciles, **al menos dos alternativas** por decisión con su coste, la elegida y por qué, y qué tendrá que recordar el siguiente desarrollador. Un diseño sin `decisiones.md`, o con decisiones sin alternativas, es un diseño escrito a la primera.
2. **El juez mide calidad, no solo cumplimiento.** Entre diseños que cubren la especificación, gana el que pasa mejor la **prueba del segundo desarrollador** (`k-code-quality/disenyo.md`): menos piezas, cada decisión con un solo dueño, ninguna regla que se calle confiando en otra, ramas que cubren todos los casos, y los patrones nuevos declarados en vez de improvisados. La completitud es condición de entrada; la calidad decide; si la calidad no decide, desempata comparar los `decisiones.md` (§7).
3. **El enriquecedor no hereda complejidad.** Al incorporar al ganador ventajas de los descartados, descarta las que añadan un olor de `k-code-quality/disenyo.md` (§9).
4. **El verificador reporta los olores.** Además de la validación de la plantilla, aplica `k-code-quality/disenyo.md` al diseño y reporta cada olor como problema, para que el corrector lo quite.

El rasero de los cuatro es el mismo fichero, `k-code-quality/disenyo.md`, para que el diseñador optimice exactamente lo que el juez premia, el enriquecedor respeta y el verificador exige.

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

**Placeholder `{iniciativa}`**: en todo el skill (prompts y comandos) denota la **ruta de la carpeta de la iniciativa** —la que contiene `specification.md`—, ya resuelta: bajo `.sdd/drafts/` por defecto o bajo `--root=` si se indicó (Apéndice A). Se fija en §4.1 paso 3.

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca con una ruta a un `specification.md`:

1. Leer el fichero.
2. **Validar el frontmatter**: debe contener `type: specification`. Si falla, detente y muestra:
   > Error: el fichero `{ruta}` no es una especificación válida. Su frontmatter debe incluir `type: specification`.
   > Para crear o mejorar una especificación, usa `/sdd-specification`.
3. La **carpeta de la iniciativa** (`{iniciativa}`) es la que contiene el `specification.md`.

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
   > Error: ya existe `{ruta}`. No se puede pasar guías por el prompt cuando el fichero ya existe — edita el fichero directamente. Razón: una única fuente de verdad y evitar pérdidas.
4. **Si NO hay prompt adicional**: continúa con la Fase 1.

### 4.4 Guard: ¿ya existe `design/design.md`? — elección de modo

Comprobar si **ya existe** `design/design.md` en la carpeta de la iniciativa (criterio único: una `design/` sin `design.md` cuenta como inexistente).

- Si **no existe**: modo **Generar/Regenerar**. Continúa con §4.3 y luego la Fase 1 → Fase 9.
- Si **existe**: **detener y preguntar con `AskUserQuestion`** entre:

1. **Revisar / modificar el diseño existente** (recomendado si se editó a mano o solo quieres cambios puntuales): **NO regenera**; entra en el **modo Revisar/Modificar (§14)**.
2. **Regenerar desde la especificación** (pisa el diseño actual): continúa con §4.3 y la Fase 1; el `design/` actual y los borradores residuales los borra la limpieza previa de la Fase 2 (§6) antes de lanzar los diseñadores.

Mensaje exacto al usuario:

> Ya existe `design/design.md` en `{iniciativa}`. ¿Qué quieres hacer?
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

**REQUIRED — limpieza previa.** Antes de lanzar nada, borra los restos de ejecuciones anteriores o abortadas: el `design/` que se regenera (§4.4 opción 2) y los `design_<n>/`/`log_best.txt` residuales (si quedaran, `mv` del ganador (§8) metería la carpeta dentro del `design/` viejo, y los residuos se mezclarían con la comprobación de contenido de abajo y con el append de §7):
```bash
rm -rf {iniciativa}/design {iniciativa}/design_[0-9]* {iniciativa}/log_best.txt
```
(Con `--out=`, en lugar de `design` borra la carpeta indicada.)

**CRITICAL**: lanza **exactamente 5 subagentes diseñadores** en una **única respuesta** con 5 invocaciones a `Agent` simultáneas, **cada una con `model` fijado al modelo más capaz disponible** (§2.3). **MUST NOT** lanzarlos secuencialmente. **MUST NOT** usar `run_in_background`. Numéralos `n = 1..5`; el diseñador `n` escribe su diseño en la carpeta `design_<n>/` de la iniciativa.

**Prompt de cada subagente diseñador `n`** (mismo para los 5 salvo el número de carpeta):

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que crear un diseño en base a una especificación, unas guías de diseño y unas reglas para el diseño.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y **todos los ficheros que referencie**. Son el contrato: define qué producir, cómo, con qué estructura y qué contexto del proyecto cargar. Síguelo al pie de la letra.
> - **Especificación**: lee `{ruta de specification.md}` y todos los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(esta línea solo si el fichero existe)*.
> - **Rasero de calidad**: lee `.claude/skills/k-code-quality/disenyo.md`. Es lo que el juez usará para elegir entre tu diseño y los demás, y lo que el verificador exigirá después: diseña para pasar la **prueba del segundo desarrollador**, no para marcar casillas.
> - **CRITICAL — piensa antes de escribir.** Antes de producir ningún otro fichero, escribe `{iniciativa}/design_<n>/decisiones.md` con la plantilla literal de abajo:
>   1. Identifica las **decisiones difíciles** del diseño: dónde hay más de una forma razonable de hacerlo, dónde la especificación pide algo que las guías o recetas no cubren, dónde una pieza va a repetirse en el siguiente caso parecido. **LIMIT**: entre 1 y 6 decisiones (normalmente 3-6; con una sola, incluye igualmente su alternativa descartada).
>   2. Por cada decisión da **al menos 2 alternativas**, con su coste y lo que el siguiente desarrollador tendría que recordar.
>   3. Elige una y di por qué, citando los olores de `disenyo.md` que evita.
>   4. Si una decisión crea un **patrón nuevo** (nada documentado lo cubre), márcalo como tal y di dónde debería vivir la pieza común.
>   5. Después escribe el diseño **coherente con lo decidido**.
> - **Salida**: escribe el **diseño completo y autosuficiente** en la carpeta `{iniciativa}/design_<n>/`, con la estructura exacta que define el README (incluido su índice `design.md` con el frontmatter fijo del motor: `type: design` más `template: {template}` — §1.2).
> - **CRITICAL — confinamiento de escritura**: **MUST NOT** escribir, crear, editar ni borrar ningún fichero fuera de `{iniciativa}/design_<n>/`: nada de código fuente (`src/**`), configuración (`axelor-config.properties`, `build.gradle`, …) ni otros artefactos del proyecto. El diseño es un **plan**. Si el diseño exige un cambio fuera de esa carpeta (p.ej. una propiedad de configuración nueva o una clase existente que modificar), **documéntalo dentro del diseño** para que lo aplique `/sdd-implementer`; **MUST NOT** aplicarlo tú.
> - **MUST NOT** usar `AskUserQuestion`. Ante una ambigüedad, toma la decisión más razonable y regístrala como una decisión más en `decisiones.md` (con la alternativa que descartaste); no la devuelvas en la respuesta.
> - Aplica el **checklist** que prescriba el README antes de terminar (**LIMIT**: 3 iteraciones de autocorrección).
> - Al terminar, responde **exactamente** `ESCRITO: design_<n>` y, opcionalmente, 1-2 líneas de notas. **MUST NOT** pegar el contenido del diseño en la respuesta (ya está en disco).

**Plantilla literal de `decisiones.md`** (el diseñador la copia y rellena; una sección `## D<k>` por decisión):

```markdown
# Decisiones de diseño

## D1 — <título corto de la decisión>

**Problema:** <qué hay que resolver y por qué admite más de una solución>

**Alternativas:**
- **A — <nombre>:** <en qué consiste>. Coste: <qué añade o complica>. El siguiente desarrollador tendrá que recordar: <lista o «nada»>.
- **B — <nombre>:** <en qué consiste>. Coste: <…>. El siguiente desarrollador tendrá que recordar: <…>.

**Elegida:** <A|B> — <por qué frente a la otra, citando los olores de `k-code-quality/disenyo.md` que evita>

**Patrón nuevo:** <NO | SÍ — pieza común en `<ruta>` y receta que faltaría escribir>
```

- ✅ CORRECTO: `**Elegida:** B — una sola rama por campo con la condición explícita; A obligaba a recordar tres reglas y escondía la condición dentro de ellas (olor «una regla que decide sola si aplica»)`
- ❌ INCORRECTO: `**Alternativas:** - A: la única razonable` (una decisión con una sola alternativa no es una decisión: no se ha pensado)
- ❌ INCORRECTO: un `decisiones.md` escrito **después** del diseño para justificarlo (el orden importa: se decide y luego se escribe)

Tras los 5: comprueba que cada `design_<n>/` existe, tiene contenido **y contiene `decisiones.md`** (si falta, descarta esa carpeta como si estuviera vacía: un diseño sin decisiones se escribió a la primera). Si **ninguna** se creó → **ERROR** (STOP condition). Si alguna falta o quedó vacía, descártala: el torneo opera solo sobre las carpetas válidas.

- ✅ CORRECTO (respuesta del diseñador): `ESCRITO: design_3`
- ❌ INCORRECTO: `He guardado el diseño 3` (token no parseable), pegar el `design.md` completo en la respuesta (gasta contexto, ya está en disco)

---

## 7. Fase 3 — Elegir el mejor diseño (torneo del juez)

El ganador se decide por **torneo acumulativo** con un subagente **juez** que compara **dos diseños cada vez**. Sea `D` la lista ordenada de carpetas válidas (`design_1`, `design_2`, …):

1. `ganador = D[0]`.
2. **Para cada** `design_i` siguiente de la lista: lanza el juez con (`ganador`, `design_i`), con `model` fijado al modelo más capaz (§2.3); su veredicto pasa a ser el nuevo `ganador`.
3. Al agotar la lista, `ganador` es el diseño elegido.

Cada invocación del juez es **secuencial** (depende del ganador anterior). Si solo hay **una** carpeta válida, no hay torneo: esa es el ganador.

**Prompt del subagente juez** (en cada comparación):

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que elegir entre 2 diseños en base a una especificación, unas guías de diseño y unas reglas para el diseño.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie.
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Rasero de calidad**: lee `.claude/skills/k-code-quality/disenyo.md`.
> - **Diseños a comparar**: la carpeta `{iniciativa}/{ganador}` (la llamo `<carpeta-A>`) y la carpeta `{iniciativa}/design_<i>` (la llamo `<carpeta-B>`), incluido el `decisiones.md` de cada una.
> - Elige cuál de los dos es **mejor diseño**, **detallando las ventajas concretas Y los defectos/errores concretos de CADA uno de los dos diseños** y con cuál te quedas. Juzga en este orden:
>   1. **Cumplimiento** de la especificación, las guías y las reglas: cobertura y coherencia. Es condición de entrada: un diseño que deja escenarios sin cubrir o contradice el contrato pierde aunque sea más elegante.
>   2. **Calidad**, entre diseños que cumplen: aplica la **prueba del segundo desarrollador** de `disenyo.md` a cada uno (¿cuántas cosas hay que recordar para reproducirlo en el siguiente caso parecido?) y busca sus olores: una decisión con varios dueños, reglas que deciden solas si aplican, retornos defensivos, piezas que se conocen entre sí, ramas que no cubren todos los casos, defensa solo en la vista, patrones nuevos improvisados sin declarar. Cada olor es un **defecto** con nombre. Gana el que **menos exige recordar**, no el que tiene más piezas ni el que más literalmente sigue el contrato.
>   3. **Decisiones**: compara los `decisiones.md`: ¿identifican las mismas decisiones difíciles? ¿las alternativas descartadas son reales o de paja? Un diseño que no vio una decisión que el otro sí vio, o que eligió sin alternativas, pierde en ese punto. Este nivel **solo desempata** si el nivel 2 no decide.
>   Un diseño más simple que cumple **MUST** ganar a uno más completo que añade piezas que la especificación no pide.
> - Responde con este formato **exacto** (seis bloques, en este orden):
>   - Primera línea: **exactamente** `GANADOR: <nombre-de-carpeta>` (una de las dos comparadas).
>   - Una línea **exactamente** `=== VENTAJAS <carpeta-A> ===` y debajo, en bullets (`- `), las **ventajas concretas** de ese diseño (qué hace bien, qué punto del spec/guías/reglas cubre mejor — no elogios genéricos). **LIMIT**: 2-6 bullets.
>   - Una línea **exactamente** `=== DEFECTOS <carpeta-A> ===` y debajo, en bullets (`- `), los **defectos/errores/carencias concretos** de ese diseño (qué hace mal, qué punto del spec/guías/reglas incumple o cubre peor, qué regla/escenario falta — no pegas genéricas; si de verdad no le ves ninguno, un único bullet `- Ninguno detectado`). Estos defectos se auditarán en la Fase 5 aunque este diseño acabe ganando. **LIMIT**: 1-6 bullets.
>   - Una línea **exactamente** `=== VENTAJAS <carpeta-B> ===` y debajo, igual, las ventajas concretas del otro diseño. **LIMIT**: 2-6 bullets.
>   - Una línea **exactamente** `=== DEFECTOS <carpeta-B> ===` y debajo, igual, los defectos/errores/carencias concretos del otro diseño (o `- Ninguno detectado`). **LIMIT**: 1-6 bullets.
>   - Una línea **exactamente** `=== JUSTIFICACIÓN ===` y debajo la justificación: **MUST** explicar **por qué** el ganador es mejor **frente al otro diseño**, citando las diferencias decisivas (qué hace mejor el ganador, en qué falla o se queda corto el perdedor) y, cuando aplique, contra qué punto de la especificación, las guías o las reglas, **y** el resultado de la prueba del segundo desarrollador en cada uno (qué habría que recordar para reproducir cada diseño). **LIMIT**: entre 3 y 8 líneas.
>   - `<carpeta-A>`/`<carpeta-B>` son los nombres reales de las dos carpetas comparadas (p.ej. `design_2`, `design_3`).

El skill parsea la primera línea `GANADOR: design_<n>`. Si el token no aparece, no es una de las dos carpetas comparadas, o falta alguno de los cinco bloques `=== … ===` (las dos `VENTAJAS`, los dos `DEFECTOS` y la `JUSTIFICACIÓN`), **reintenta esa comparación 1 vez**; si vuelve a fallar → **STOP** (STOP condition).

**REQUIRED — mostrar por pantalla y registrar en `log_best.txt`.** Tras cada comparación válida, antes de seguir el torneo, el skill **MUST**:

1. **Mostrar al usuario** el veredicto, las ventajas y los defectos de cada diseño y la justificación que devolvió el juez, con este formato:
   ```
   Comparación {k}/{total}: {carpeta-A} vs {carpeta-B} → gana design_<n>
   {bloques === VENTAJAS … ===, === DEFECTOS … === y === JUSTIFICACIÓN === literales del juez}
   ```
   **MUST NOT** ocultar ni resumir las ventajas/defectos/justificación hasta perder el detalle de la comparación.
2. **Añadir (append)** a `{iniciativa}/log_best.txt` una sección con esta comparación: la cabecera `### Comparación {k}: {carpeta-A} vs {carpeta-B} → gana design_<n>` y, debajo, **en este orden**: (a) los cuatro bloques `=== VENTAJAS … ===` y `=== DEFECTOS … ===` **literales** del juez (los dos de ventajas y los dos de defectos), y (b) **al final**, un bloque `=== JUSTIFICACION {carpeta-A} vs {carpeta-B} ===` con el **contenido literal** del bloque `=== JUSTIFICACIÓN ===` que devolvió el juez (la justificación del porqué se eligió un diseño frente al otro, basada en las ventajas y defectos de cada uno). Es un append acumulativo (una sección por comparación); su finalidad, en §1.2. Se escribe en la carpeta de la iniciativa y la Fase 4 lo mueve a la carpeta de salida (`design/log_best.txt`).

Si solo hay **una** carpeta válida (sin torneo), escribe en `{iniciativa}/log_best.txt` una única línea: `Un único diseño válido (sin torneo ni comparación de ventajas ni defectos).`

- ✅ CORRECTO (respuesta del juez): `GANADOR: design_2` + `=== VENTAJAS design_2 ===` + `=== DEFECTOS design_2 ===` + `=== VENTAJAS design_3 ===` + `=== DEFECTOS design_3 ===` + `=== JUSTIFICACIÓN ===`, cada bloque con sus bullets/líneas
- ❌ INCORRECTO: `Me quedo con el segundo` (sin token), `GANADOR: design_9` (carpeta que no estaba en la comparación), `GANADOR: design_2` sin los bloques `=== VENTAJAS … ===`/`=== DEFECTOS … ===` (no hay ventajas ni defectos que registrar en `log_best.txt`), ventaja tipo `design_2 está más completo` o defecto tipo `design_3 es peor` (genéricos, no concretan qué hace bien/mal)

---

## 8. Fase 4 — Seleccionar el ganador

Una vez conocido el ganador:

1. **Renombrar** la carpeta ganadora a `design/`:
   ```bash
   mv {iniciativa}/{ganador} {iniciativa}/design
   ```
2. **Borrar** el resto de carpetas de borrador:
   ```bash
   rm -rf {iniciativa}/design_[0-9]*
   ```
3. **Mover** el log de ventajas y defectos dentro de la carpeta de salida (el motor lo acumuló en la iniciativa durante el torneo, §7):
   ```bash
   mv {iniciativa}/log_best.txt {iniciativa}/design/log_best.txt
   ```
   (Si se indicó `--out=`, el destino es `{--out=}/log_best.txt`.)

Tras esto solo queda `design/` (más `--out=` si se indicó: en ese caso, el destino final es esa carpeta).

---

## 9. Fase 5 — Enriquecer el ganador con las ventajas de los descartados y sanear sus defectos

**Solo en modo Generar/Regenerar** (depende del torneo y de `log_best.txt`; en Revisar/Modificar no aplica — §14). Tras seleccionar el ganador, el diseño se **enriquece y sanea** en una sola pasada, a partir de `design/log_best.txt`:

- **(a) Enriquecer**: incorporar las **ventajas de los diseños descartados** que el ganador no tenga y que tengan sentido. Los diseños descartados ya no están en disco (la Fase 4 los borró): la fuente de esas ventajas es `log_best.txt`.
- **(b) Sanear**: corregir los **defectos/errores que el juez atribuyó al propio diseño ganador** en `log_best.txt` y que **sigan presentes** en él: los defectos que el juez detectó al ganador se corrigen, no se arrastran.

1. **Lanzar el subagente enriquecedor** (uno solo). Recibe todo el contexto + `log_best.txt`; comprueba (a) y (b) y **reporta** las mejoras (no las implementa).
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
> - **(a) Ventajas de los descartados**: para **cada ventaja** de un diseño **descartado** que aparezca en `log_best.txt`, comprueba (1) **si ya existe** en el diseño ganador, (2) **si tiene sentido aplicarla** (coherente con la especificación, las guías y las reglas, sin contradecir las decisiones del ganador ni su `decisiones.md`) y (3) **si no añade ningún olor** de `.claude/skills/k-code-quality/disenyo.md` (una ventaja que suma una pieza más que recordar no es una ventaja: «complejidad heredada por incorporar ventajas»). Reporta **solo** las que **faltan** en el ganador, tienen sentido **y** no empeoran la prueba del segundo desarrollador.
> - **(b) Defectos del propio ganador**: para **cada defecto** que el juez haya atribuido al **diseño que resultó ganador** (los bloques `=== DEFECTOS <ganador> ===` de `log_best.txt`), comprueba **en la carpeta `design/` actual** si ese defecto **sigue presente**. Reporta **solo** los defectos del ganador que **persisten** y que tiene sentido corregir (coherentes con spec/guías/reglas); descarta los que ya estén resueltos, los marcados `Ninguno detectado` o los que no procedan. **Ignora** los bloques `=== DEFECTOS … ===` de los diseños **descartados** (esos defectos murieron con su diseño; aquí solo importan los del ganador).
> - Reporta tanto (a) como (b) en la **misma** lista de mejoras de salida (un `tipo` distingue cuál es cuál).
> - **MUST NOT** modificar el diseño: solo **detecta y reporta** las mejoras (las aplicará el corrector).
>
> **Formato de salida (REQUIRED)**:
> - Si **no** hay ninguna mejora que incorporar ni ningún defecto del ganador que persista (el ganador ya tiene todas las ventajas relevantes y no arrastra defectos), responde **exactamente** y solo: `OK-SIN-MEJORAS`.
> - Si hay mejoras, responde **únicamente** con líneas **JSONL**: **una mejora por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:
>   - `id` — identificador correlativo, formato `M-NNN` (`M-001`, `M-002`, …).
>   - `tipo` — `VENTAJA` (ventaja de un descartado que se incorpora) o `DEFECTO-GANADOR` (defecto del propio ganador que se sanea).
>   - `origen` — de qué diseño/ventaja/defecto del `log_best.txt` procede (p.ej. `design_3: <ventaja del log>`, o `design_2 (ganador): <defecto del log>`).
>   - `fichero` — fichero del diseño donde aplicarla relativo a la iniciativa (p.ej. `design/<fichero-que-declare-la-plantilla>`), o `null`.
>   - `ubicacion` — sección, tabla, clase/método o vista concreta; `null` si no aplica.
>   - `mejora` — qué ventaja falta en el ganador y se quiere incorporar, o qué defecto del ganador hay que corregir.
>   - `justificacion` — contra qué punto del spec/guías/reglas va y, además, para `VENTAJA`: por qué no está ya en el ganador y por qué tiene sentido aplicarla; para `DEFECTO-GANADOR`: por qué el defecto que reportó el juez **sigue presente** en `design/`.
>   - `correccion` — qué cambio concreto hacer para incorporar la ventaja o resolver el defecto.
> - Cada línea **MUST** ser JSON válido en una sola línea (escapa saltos como `\n`). **MUST NOT** añadir comentarios ni texto fuera de las líneas JSONL.
>
> Ejemplo de salida con mejoras:
>
> ```jsonl
> {"id":"M-001","tipo":"VENTAJA","origen":"design_3: <ventaja concreta del log_best.txt>","fichero":"design/<fichero-que-declare-la-plantilla>","ubicacion":"<sección o elemento concreto>","mejora":"Incorporar <la ventaja> que el ganador no tiene.","justificacion":"Cubre <id-regla-del-spec>, que el ganador deja sin <lo que exige la plantilla>; no contradice ninguna decisión de decisiones.md ni añade olores de disenyo.md.","correccion":"Añadir <el elemento concreto> en <fichero>/<ubicacion>, manteniendo lo que ya cubre el ganador."}
> {"id":"M-002","tipo":"DEFECTO-GANADOR","origen":"design_2 (ganador): <defecto concreto del log_best.txt>","fichero":"design/design.md","ubicacion":"<sección o tabla concreta>","mejora":"Corregir el defecto que el juez detectó: <id-regla-del-spec> quedó sin <lo que exige la plantilla>.","justificacion":"El juez marcó este defecto en design_2 y sigue presente en design/: <evidencia concreta>; el spec exige <id-regla-del-spec>.","correccion":"<cambio concreto en fichero/ubicacion que resuelve el defecto>."}
> ```

**Prompt del subagente corrector** (para aplicar las mejoras del enriquecedor):

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que **incorporar al diseño** las mejoras indicadas (ventajas de otros diseños que faltan y defectos del propio diseño que hay que sanear).
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie.
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño a enriquecer y sanear**: la carpeta `{iniciativa}/design` — aplica las mejoras **en sitio** (`Edit`/`Write`), sin renombrar ni mover la carpeta, sin regenerar el diseño ni romper las decisiones del ganador que no estén en falta. Tras editar, aplica la validación que prescriba la plantilla.
> - **CRITICAL — confinamiento de escritura**: **MUST NOT** escribir, editar ni borrar ningún fichero fuera de `{iniciativa}/design/` (nada de `src/**`, `axelor-config.properties`, `build.gradle`, ni otros artefactos del proyecto). Si una mejora exige un cambio fuera de esa carpeta, **documéntalo dentro del diseño** para `/sdd-implementer`; **MUST NOT** aplicarlo tú.
> - **Mejoras a incorporar** (las reportó el enriquecedor, en formato JSONL, una por línea; el campo `tipo` indica si es una `VENTAJA` que incorporar o un `DEFECTO-GANADOR` que corregir): `{líneas JSONL literales del enriquecedor}`. Aplica cada `correccion` en el `fichero`/`ubicacion` indicados; mantén la trazabilidad y la coherencia que la plantilla exige. Si una mejora cambia una decisión registrada en `decisiones.md`, actualiza su sección `D<k>`.
> - Al terminar, responde **exactamente** `CORREGIDO` en la primera línea y debajo 1-3 líneas con las mejoras aplicadas (el motor no ramifica sobre este token: solo comprueba que llegó y continúa). **MUST NOT** pegar el diseño en la respuesta (ya está en disco).

- ✅ CORRECTO (respuesta del enriquecedor sin mejoras): `OK-SIN-MEJORAS`
- ✅ CON MEJORAS (una línea JSONL por mejora, sin texto alrededor): `{"id":"M-001","tipo":"VENTAJA","origen":"…","fichero":"…","ubicacion":"…","mejora":"…","justificacion":"…","correccion":"…"}` o `{"id":"M-002","tipo":"DEFECTO-GANADOR","origen":"…","fichero":"…","ubicacion":"…","mejora":"…","justificacion":"…","correccion":"…"}`
- ✅ CORRECTO (respuesta del corrector): `CORREGIDO` + 1-3 líneas de resumen
- ❌ INCORRECTO: `No hace falta nada ✅` o `He aplicado las mejoras` (tokens no exactos), reportar ventajas que el ganador **ya tiene** o defectos del ganador **ya resueltos** (la tarea es solo las ventajas que faltan y los defectos que persisten), reportar defectos de diseños **descartados** (solo importan los del ganador), o devolver las mejoras como prosa/array en vez de una línea JSONL por mejora.

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
> - **Rasero de calidad**: lee `.claude/skills/k-code-quality/disenyo.md`. Además de la validación de la plantilla, busca en el diseño **cada olor** que describe (una decisión con varios dueños, reglas que deciden solas si aplican, retornos defensivos que delegan, piezas que se conocen entre sí, ramas que no cubren todos los casos, defensa solo en la vista, patrones nuevos sin declarar en `decisiones.md`) y reporta cada uno como problema con `origen` `k-code-quality/disenyo.md § <olor>`, severidad `IMPORTANT` (o `BLOCKING` si deja un caso sin validar). En el inventario de ficheros de la plantilla ignora `decisiones.md` (contrato del motor, lo escribe el diseñador), los logs `log_*.txt` (los escribe el motor) y `test-unit-desc.md` (lo escribe/regenera la Fase 7 después de esta verificación): ninguno lo declara la plantilla.
> - **Diseño a verificar**: la carpeta `{iniciativa}/design`.
>
> **Formato de salida (REQUIRED)**:
> - Si **no** has encontrado nada que corregir, responde **exactamente** y solo: `OK-CORRECTO`.
> - Si has encontrado problemas, responde **únicamente** con líneas **JSONL** (JSON Lines): **un problema por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:
>   - `id` — identificador correlativo del problema, formato `P-NNN` (`P-001`, `P-002`, …).
>   - `severidad` — uno de `BLOCKING` | `IMPORTANT` | `MINOR`.
>   - `fichero` — ruta del fichero del diseño afectado relativa a la iniciativa (p.ej. `design/design.md`), o `null` si es transversal.
>   - `ubicacion` — sección, tabla, clase/método o línea concreta dentro de ese fichero; `null` si no aplica.
>   - `origen` — el identificador del spec/guía/regla que se incumple (p.ej. `<id-regla-del-spec>`, `<id-escenario-del-spec>` o el nombre de la regla de la plantilla), o `null`.
>   - `problema` — descripción clara y concreta del fallo/error/inconsistencia.
>   - `correccion` — qué hay que cambiar para resolverlo.
> - Cada línea **MUST** ser JSON válido en una sola línea (sin saltos de línea internos; escapa los que necesites como `\n`). **MUST NOT** añadir comentarios, numeración ni explicaciones fuera de las líneas JSONL.
>
> Ejemplo de salida con problemas:
>
> ```jsonl
> {"id":"P-001","severidad":"BLOCKING","fichero":"design/design.md","ubicacion":"<sección o tabla concreta>","origen":"<id-regla-del-spec>","problema":"La regla <id-regla-del-spec> del spec no está recogida en el diseño con la forma que exige la plantilla.","correccion":"Añadir <id-regla-del-spec> en <ubicacion> con lo que el contrato de la plantilla exija para ella."}
> {"id":"P-002","severidad":"IMPORTANT","fichero":"design/<fichero-que-declare-la-plantilla>","ubicacion":"<elemento concreto>","origen":"<id-escenario-del-spec>","problema":"El escenario <id-escenario-del-spec> del spec no tiene ningún elemento del diseño que lo materialice.","correccion":"Crear <el elemento que la plantilla prescriba> que cubra <id-escenario-del-spec> con su trazabilidad."}
> ```

**Prompt del subagente corrector**:

> Eres un experto arquitecto y diseñador en Java y el framework Axelor, que tienes que corregir los errores en el diseño en base a una especificación, unas guías de diseño y unas reglas para el diseño.
>
> - **Reglas para el diseño**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie.
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño a corregir**: la carpeta `{iniciativa}/design` — corrige **en sitio** (`Edit`/`Write` sobre sus ficheros), sin renombrar ni mover la carpeta.
> - **CRITICAL — confinamiento de escritura**: **MUST NOT** escribir, editar ni borrar ningún fichero fuera de `{iniciativa}/design/` (nada de `src/**`, `axelor-config.properties`, `build.gradle`, ni otros artefactos del proyecto). Si una corrección exige un cambio fuera de esa carpeta, **documéntalo dentro del diseño** para `/sdd-implementer`; **MUST NOT** aplicarlo tú.
> - **Problemas a corregir** (los reportó el verificador, en formato JSONL, un problema por línea): `{líneas JSONL literales del verificador}`. Resuelve cada línea (`id`/`severidad`/`fichero`/`ubicacion`/`origen`/`problema`/`correccion`); aplica la `correccion` en el `fichero`/`ubicacion` indicados. Si una corrección cambia una decisión registrada en `decisiones.md`, actualiza su sección `D<k>`.
> - Al terminar, responde **exactamente** `CORREGIDO` en la primera línea y debajo 1-3 líneas con los problemas corregidos (el motor no ramifica sobre este token: solo comprueba que llegó y continúa). **MUST NOT** pegar el diseño en la respuesta (ya está en disco).

- ✅ CORRECTO (respuesta del verificador sin problemas): `OK-CORRECTO`
- ✅ CON PROBLEMAS (una línea JSONL por problema, sin texto alrededor): `{"id":"P-001","severidad":"BLOCKING","fichero":"design/design.md","ubicacion":"…","origen":"<id-regla-del-spec>","problema":"…","correccion":"…"}`
- ✅ CORRECTO (respuesta del corrector): `CORREGIDO` + 1-3 líneas de resumen
- ❌ INCORRECTO: `Todo correcto ✅` o `Ya está arreglado` (tokens no exactos; el skill compara por literal), o devolver los problemas como prosa/array JSON en vez de una línea JSONL por problema.

---

## 11. Fase 7 — Tests unitarios (describirlos)

**Común a ambos modos** (Generar/Regenerar y Revisar/Modificar): una vez el diseño está conforme (`OK-CORRECTO`), un subagente **test-unitarios** escribe `design/test-unit-desc.md` con lo que el **contrato de tests unitarios** de la plantilla activa declare para este artefacto. **Solo descripción, sin código**: qué hay que cubrir ahí, y qué se hace después con `test-unit-desc.md`, lo declara la plantilla activa.

1. **Lanzar el subagente test-unitarios** (uno solo). Produce `design/test-unit-desc.md` siguiendo el contrato que la plantilla prescribe para los tests unitarios (lo descubre vía el README). Ante una `test-unit-desc.md` previa, la **regenera** para reflejar el diseño actual.
2. Cuando responda **exactamente** `ESCRITO: test-unit-desc.md`, continúa con la Fase 8.
3. Si no devuelve el token o no produce `test-unit-desc.md`, **reintenta 1 vez**. Si vuelve a fallar, avísalo al usuario (**MUST NOT** bloquear el flujo por esto) y:
   - si `test-unit-desc.md` **existe** (solo faltó el token) → continúa con la Fase 8;
   - si **no existe** → **salta la Fase 8** y ve a la Fase 9, reflejando en el cierre que no se generó (§13).

**Prompt del subagente test-unitarios**:

> Eres un experto en **tests unitarios** del stack del proyecto. Tu tarea es **describir** —no implementar— los tests unitarios que el contrato de la plantilla declare para un diseño, con el alcance, la forma y la trazabilidad que ese contrato fije.
>
> - **Reglas para el diseño y para los tests unitarios**: lee `{ruta de <plantilla-activa>/README.md}` y **los ficheros que referencie** —en particular el contrato de los **tests unitarios**—. Define el alcance de los tests, la plantilla exacta de `test-unit-desc.md`, la trazabilidad y el checklist. Síguelo al pie de la letra.
> - **Especificación**: lee `{ruta de specification.md}` y todos los ficheros que enlace (para los mensajes/semántica exactos de cada regla).
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño**: lee la carpeta `{iniciativa}/design` —sobre todo `design.md`— como **fuente de verdad** de lo que el contrato de tests unitarios de la plantilla declare que hay que cubrir. **CRITICAL**: en esta fase **todavía no existe el código** que el diseño planifica (lo creará `/sdd-implementer`); toma **del diseño** todo lo que describas, no del árbol de fuentes. Para lo que el diseño **modifica** (ya existente) o para las piezas de las que dependa, puedes explorar el código real.
> - **Salida**: escribe `{iniciativa}/design/test-unit-desc.md` **según la plantilla exacta del contrato**. **MUST NOT** escribir código de tests (ni `@Test`, ni imports, ni cuerpos): solo la descripción, con los apartados, los campos y la trazabilidad que el contrato declare.
> - **CRITICAL — confinamiento de escritura**: **MUST NOT** escribir, editar ni borrar ningún fichero fuera de `{iniciativa}/design/` (nada de `src/**` ni tests reales en el árbol del proyecto: explorar el código es solo lectura). Describes; implementa `/sdd-implementer`.
> - **MUST NOT** usar `AskUserQuestion`. Ante una ambigüedad, decide lo más razonable y documéntalo en `test-unit-desc.md`.
> - Aplica el **checklist** del contrato antes de terminar (**LIMIT**: 3 iteraciones de autocorrección).
> - Al terminar, responde **exactamente** `ESCRITO: test-unit-desc.md` y, opcionalmente, 1-2 líneas de notas (cobertura). **MUST NOT** pegar el contenido de `test-unit-desc.md` en la respuesta (ya está en disco).

- ✅ CORRECTO (respuesta del subagente test-unitarios): `ESCRITO: test-unit-desc.md`
- ❌ INCORRECTO: `He creado los tests` (token no parseable), pegar `test-unit-desc.md` en la respuesta (gasta contexto, ya está en disco), o incluir código de tests en `test-unit-desc.md` (la fase solo describe)

---

## 12. Fase 8 — Verificar y corregir los tests unitarios (bucle, LIMIT 10)

**Común a ambos modos** (Generar/Regenerar y Revisar/Modificar). Una vez `test-unit-desc.md` existe (Fase 7; si no se generó, esta fase se salta — §11 paso 3), comprueba **en bucle** que es **coherente con el diseño** aplicando las **comprobaciones de coherencia** que declare la plantilla activa (cuáles son es propio de cada artefacto: el motor **MUST NOT** enumerarlas aquí). Sobre la carpeta `design/`, repite este bucle **como máximo 10 veces** (**LIMIT**: 10 iteraciones); lleva un contador de iteración `{k}` empezando en 1:

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
>   - `origen` — la clase/método/regla del diseño que se incumple (p.ej. `Clase <clase-del-diseño>`, `método <método-del-diseño>`, `<id-regla-del-spec>`), o `null`.
>   - `problema` — descripción clara de la incoherencia (p.ej. clase/método inexistente en el diseño, regla inexistente, cobertura que no cuadra).
>   - `correccion` — qué hay que cambiar en `test-unit-desc.md` para resolverlo.
> - Cada línea **MUST** ser JSON válido en una sola línea (escapa los saltos como `\n`). **MUST NOT** añadir comentarios ni texto fuera de las líneas JSONL.
>
> Ejemplo de salida con problemas:
>
> ```jsonl
> {"id":"P-001","severidad":"BLOCKING","fichero":"design/test-unit-desc.md","ubicacion":"Clase <clase-del-diseño>","origen":"Clase <clase-del-diseño>","problema":"Se describen tests para <clase-del-diseño>, que no existe en el diseño (design.md no la define).","correccion":"Eliminar la sección de <clase-del-diseño> o sustituirla por la clase real del diseño que cubre esa responsabilidad."}
> {"id":"P-002","severidad":"IMPORTANT","fichero":"design/test-unit-desc.md","ubicacion":"<clase-del-diseño>, test <test-descrito>","origen":"método <método-del-diseño>","problema":"El test <test-descrito> ejerce <método-del-diseño>, que el diseño no define para <clase-del-diseño>.","correccion":"Reasignar <test-descrito> al método que sí define el diseño o eliminarlo si la regla no aplica ahí."}
> ```

**Prompt del subagente corrector-test-unitarios**:

> Eres un experto en **tests unitarios** del stack del proyecto, que tienes que corregir las incoherencias detectadas en la **descripción de los tests unitarios**.
>
> - **Reglas para el diseño y para los tests unitarios**: lee `{ruta de <plantilla-activa>/README.md}` y los ficheros que referencie (el contrato de los tests unitarios).
> - **Especificación**: lee `{ruta de specification.md}` y los ficheros que enlace.
> - **Guías de diseño**: lee `{ruta de design-guidelines.md}` *(solo si existe)*.
> - **Diseño**: la carpeta `{iniciativa}/design` —sobre todo `design.md`— es la **fuente de verdad**; **MUST NOT** modificar el diseño para que cuadre con los tests: corrige los tests para que cuadren con el diseño.
> - **Fichero a corregir**: `{iniciativa}/design/test-unit-desc.md` — corrige **en sitio** (`Edit`/`Write`), respetando la plantilla del contrato; no toques otros ficheros del diseño.
> - **CRITICAL — confinamiento de escritura**: **MUST NOT** escribir, editar ni borrar ningún fichero fuera de `{iniciativa}/design/` (nada de `src/**` ni tests reales en el árbol del proyecto: explorar el código es solo lectura).
> - **Problemas a corregir** (los reportó el verificador-test-unitarios, en formato JSONL, un problema por línea): `{líneas JSONL literales del verificador}`. Aplica cada `correccion` en la `ubicacion` indicada.
> - Al terminar, responde **exactamente** `CORREGIDO` en la primera línea y debajo 1-3 líneas con las incoherencias corregidas (el motor no ramifica sobre este token: solo comprueba que llegó y continúa). **MUST NOT** pegar `test-unit-desc.md` en la respuesta (ya está en disco).

- ✅ CORRECTO (respuesta del verificador-test-unitarios sin problemas): `OK-CORRECTO`
- ✅ CON PROBLEMAS (una línea JSONL por problema, sin texto alrededor): `{"id":"P-001","severidad":"BLOCKING","fichero":"design/test-unit-desc.md","ubicacion":"…","origen":"…","problema":"…","correccion":"…"}`
- ✅ CORRECTO (respuesta del corrector-test-unitarios): `CORREGIDO` + 1-3 líneas de resumen
- ❌ INCORRECTO: `Todo correcto ✅` o `Corregido todo 👍` (tokens no exactos; el skill compara por literal), o devolver los problemas como prosa/array JSON en vez de una línea JSONL por problema.

---

## 13. Fase 9 — Mensaje de cierre al usuario

```
Diseño guardado en {iniciativa}/design/

  - design.md
  - {resto de ficheros y carpetas según la estructura que define la plantilla}
  - test-unit-desc.md (lo que la plantilla activa declare para los tests unitarios)

Verificación del diseño: OK-CORRECTO (tras {N} iteración(es) de verificar/corregir).
Tests unitarios: descritos en design/test-unit-desc.md (coherencia con el diseño: OK-CORRECTO).

Si quieres iterar sobre este diseño, puedes:
  1. Editar (o crear) {iniciativa}/design-guidelines.md con guías
     adicionales. Debe empezar con:
       ---
       type: design-guidelines
       ---
  2. Re-ejecutar:
     /sdd-designer {iniciativa}/specification.md
     (preguntará Regenerar o Revisar/Modificar; pasa los cambios puntuales
      como texto tras la ruta).

Para implementar este diseño tal cual ejecuta:
  /sdd-implementer {iniciativa}/design/design.md
```

`{iniciativa}` se imprime como ruta real (§4); el `handoffs` del frontmatter la nombra `{carpeta-iniciativa}` —placeholder que exige `k-skill` §9.4— con el mismo significado. Ajusta la lista de ficheros a la estructura real que define la plantilla. Si `test-unit-desc.md` no se generó (§11 paso 3), quítalo de la lista y sustituye la línea `Tests unitarios: …` por `Tests unitarios: test-unit-desc.md NO se generó (el subagente test-unitarios falló tras 1 reintento); re-ejecuta en modo Revisar/Modificar para describirlos.` **MUST NOT** lanzar `/sdd-implementer` tú mismo: el usuario decide cuándo.

---

## 14. Modo Revisar/Modificar (`design/design.md` existente)

Ruta alternativa desde la Fase 0 (§4.4) cuando `design/design.md` ya existe y el usuario elige "Revisar / modificar". **No regenera** (no lanza diseñadores ni torneo, **ni enriquece** — la Fase 5 es solo de Generar/Regenerar): aplica los cambios puntuales que pida el usuario, pasa el bucle verificar/corregir del diseño y regenera **y verifica** los tests unitarios, **preservando las ediciones manuales**.

1. Ejecutar la **Fase 1 (§5)**: leer `<plantilla-activa>/README.md` y resolver las rutas de entrada (spec, guías si existen).
2. Leer `design.md`. Si su frontmatter no es `type: design` → **ERROR** y detente. Si le falta `template:`, añade `template: {template}` (resuelto en §2.2): §1.2 lo exige y `/sdd-implementer` lo necesita. Si lo declara y **no coincide** con `{template}` → **ERROR** y detente (STOP condition): el diseño se hizo con otra plantilla y **MUST NOT** mezclarse.
3. **Aplicar los cambios pedidos** (si el usuario pasó texto de cambios en el prompt): envuelve cada cambio en una línea JSONL con los mismos campos que el verificador (§10) — `id` correlativo `U-NNN`, `severidad` `IMPORTANT`, `fichero`/`ubicacion` los que indique el usuario o `null`, `origen` `usuario`, `problema` y `correccion` con su texto — y lanza el subagente **corrector** (§10) pasándole esas líneas como los "problemas a corregir" sobre la carpeta `design/`; corrige **en sitio**. Si no hubo cambios pedidos, salta este paso.
   - ✅ CORRECTO: `{"id":"U-001","severidad":"IMPORTANT","fichero":null,"ubicacion":null,"origen":"usuario","problema":"<texto del cambio pedido por el usuario>","correccion":"<texto del cambio pedido por el usuario>"}`
   - ❌ INCORRECTO: pasar al corrector el texto libre del usuario tal cual (no cumple el formato JSONL que su prompt espera)
4. **Pasar la Fase 6 (§10)**: bucle verificar/corregir sobre `design/` (**LIMIT** 10) hasta `OK-CORRECTO`.
5. **Pasar la Fase 7 (§11, Tests unitarios)**: lanza el subagente **test-unitarios** para (re)generar `design/test-unit-desc.md` reflejando el diseño ya modificado.
6. **Pasar la Fase 8 (§12)**: bucle `verificador-test-unitarios` → `corrector-test-unitarios` sobre `design/test-unit-desc.md` (**LIMIT** 10) hasta `OK-CORRECTO`.
7. **Cerrar** con un mensaje análogo al de la Fase 9, indicando los cambios aplicados y el resultado de la verificación. Si nada hubo que tocar y el verificador respondió `OK-CORRECTO` a la primera: `El diseño ya estaba conforme; solo se ha regenerado test-unit-desc.md.`

**MUST NOT** reconstruir el diseño desde el spec en este modo. Si el verificador detecta que falta una pieza estructural completa, repórtalo al usuario en el cierre; **MUST NOT** regenerar el diseño entero.

---

## Quick Guidelines

- **CRITICAL — agnosticismo** (§2.2): este SKILL es un **motor de flujo**; todo lo específico del diseño lo define `<plantilla-activa>/README.md`, que leen los subagentes. **MUST NOT** nombrar aquí ficheros, identificadores, taxonomías ni validaciones del diseño; contratos fijos del motor: `specification.md`, `design-guidelines.md`, `design.md`, `decisiones.md` y `test-unit-desc.md` (§1.2).
- **Dos modos** (§4.4): sin `design/design.md` → Generar (Fases 1-9); con él → preguntar Regenerar (pisa) vs **Revisar/Modificar** (§14: cambios puntuales + verificar, sin diseñadores, torneo ni enriquecedor).
- **Calidad, no solo cumplimiento** (§2.5): `decisiones.md` antes del diseño; el juez elige por la prueba del segundo desarrollador entre los que cumplen y desempata por `decisiones.md`; enriquecedor y verificador aplican el mismo rasero, `k-code-quality/disenyo.md`.
- **Diseñar** (§6): **CRITICAL** exactamente 5 diseñadores en **una única respuesta**, con `model` al más capaz (§2.3); **MUST NOT** `AskUserQuestion` ni `run_in_background`. Una carpeta sin `decisiones.md` se descarta.
- **Elegir** (§7): torneo acumulativo de dos en dos, secuencial, juez con `model` al más capaz; el motor **MUST** mostrar por pantalla y acumular en `log_best.txt` las ventajas, los defectos y la justificación de cada comparación.
- **Seleccionar y enriquecer** (§8-§9, solo Generar): renombrar el ganador a `design/`, mover `log_best.txt`, borrar el resto; el enriquecedor reporta las ventajas de los descartados que faltan y los defectos del ganador que persisten, y el corrector las aplica.
- **Verificar/corregir el diseño** (§10): bucle verificador → corrector hasta `OK-CORRECTO` (**LIMIT** 10, luego **STOP**); las validaciones de la plantilla las ejecuta el verificador, nunca el motor (§2.2); el motor muestra el JSONL al usuario y lo vuelca a `log_revision.txt`.
- **Tests unitarios** (§11-§12, ambos modos): `design/test-unit-desc.md` según la plantilla, solo descripción y a partir del diseño (aún no hay código); luego bucle verificador-test-unitarios → corrector-test-unitarios hasta `OK-CORRECTO` (**LIMIT** 10), volcado a `log_revision_unit-test.txt`.
- **CRITICAL — confinamiento de escritura** (§2.4): nadie escribe fuera de la carpeta de la iniciativa; todo cambio al árbol real se **documenta en el diseño** para `/sdd-implementer`.
- **Contrato de tokens** (§2.3): comparación por literal exacto — `ESCRITO: design_<n>`, `GANADOR: design_<n>`, `OK-SIN-MEJORAS`, `OK-CORRECTO`, `ESCRITO: test-unit-desc.md` y `CORREGIDO` (cierre de los tres correctores; el motor no ramifica sobre él); los subagentes **MUST NOT** pegar el diseño en la respuesta. **MUST NOT** lanzar `/sdd-implementer` tú mismo: indica el comando y **STOP**.

---

## Apéndice A — Override de rutas (para testing y versatilidad)

- `--template-dir=<ruta>` — **carpeta de plantillas** alternativa a la resuelta por el frontmatter `template:`; **obligatorio** con `template: external`. Prioridad, mezcla de plantillas prohibida, `README.md` obligatorio y redactado para los 8 roles: ver §2.2.
- `--in=<ruta>` — fichero `specification.md` de entrada explícito. **Desactiva la elección de iniciativa** de la Fase 0 caso 2. La "carpeta de la iniciativa" es la que lo contiene.
- `--out=<ruta>` — **carpeta** donde queda el diseño final (sustituye a `{iniciativa}/design/` en las Fases 4-8). Los borradores `design_<n>/` se crean junto a `specification.md`; el ganador se mueve a `--out=`.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Las rutas relativas se resuelven contra esta raíz.

En uso normal no se especifican: se usa la carpeta de plantillas resuelta por el frontmatter `template:`, la carpeta de la iniciativa y `.sdd/drafts/`.
