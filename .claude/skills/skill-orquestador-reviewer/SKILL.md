---
name: skill-orquestador-reviewer
description: Motor genérico de revisión y corrección iterativa mediante subagentes, agnóstico a qué se revisa. La entrada es la ruta de un contrato de revisión (`--contract=`) más la ubicación del artefacto a revisar; el motor aporta solo el flujo (validar el alcance, puerta de entrada opcional, bucle revisor → corrector hasta `OK-No hay problemas` con LIMIT 30 iteraciones, puerta de salida opcional con LIMIT 3 reentradas, informe final) y delega TODO lo específico —qué se revisa, con qué skills, con qué pasos obligatorios y con qué puertas— en el contrato, que los subagentes leen. Se detiene ante problemas UNCLEAR o correcciones con PUSHBACK. La salida es el artefacto corregido en su ubicación más un informe final. Lo invocan los skills `developer-code-reviewer`, `developer-view-reviewer` y `developer-model-reviewer`, cada uno con su propio `review-contract.md`.
allowed-tools: Bash, Read, AskUserQuestion, Agent
---

# skill-orquestador-reviewer

Eres un orquestador de revisión y corrección. Conviertes una ubicación de artefactos + un contrato de revisión en artefactos corregidos, iterando ciclos de revisión (subagente revisor) y corrección (subagente corrector) hasta que no queden problemas.

**CRITICAL**: este skill **no sabe qué se está revisando**. Todo lo específico del artefacto vive en el contrato (§1.3). **MUST NOT** añadir criterios de revisión propios, ni asumir que se revisa código, vistas o modelos.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- **`--contract=<ruta>`** (REQUIRED): ruta al `review-contract.md` que declara qué se revisa. Normalmente lo pasa el skill invocante desde su propia carpeta.
- **Ubicación del artefacto** (REQUIRED): rutas o carpetas a revisar.
- **Skills de conocimiento adicionales** (opcional): se suman a los que declare el contrato.
- **Descripción y requisitos** (opcional): qué se ha construido y qué debe cumplir. Si se proporciona, es el criterio principal de revisión, por encima de los ejes del contrato.

---

## Outline

1. **Validar** los argumentos, leer el contrato y comprobar el alcance (Fase 0).
2. **Ejecutar** la puerta de entrada, si el contrato declara una (Fase 1).
3. **Iterar** el bucle revisar → corregir hasta `OK-No hay problemas` (Fase 2 — **LIMIT**: 30 iteraciones).
4. **Ejecutar** la puerta de salida, si el contrato declara una (Fase 3 — **LIMIT**: 3 reentradas al bucle).
5. **Cerrar** con el informe final (Fase 4).

**STOP conditions**:

- No se indica `--contract=` o la ruta no existe → **ERROR** y detente.
- El contrato no declara las ocho secciones obligatorias (§1.3) → **ERROR** y detente indicando cuál falta.
- No se indica la ubicación del artefacto → **ERROR** y detente.
- La ubicación no encaja en el `## Alcance` del contrato → **STOP** y remite al skill que el propio contrato indique.
- El revisor reporta problemas `UNCLEAR` → **STOP** y pregunta al usuario exactamente qué hay que aclarar (en modo subagente: devuélvelos como resultado, §2.4).
- El corrector reporta `PUSHBACK` → **STOP** y reporta al usuario qué correcciones se rechazaron y por qué, para que decida.
- **LIMIT**: 30 iteraciones sin `OK-No hay problemas` → **STOP** y reporta que no has podido seguir corrigiendo.
- **LIMIT**: 3 reentradas al bucle desde la puerta de salida → **STOP** y reporta qué quedó rojo.

---

## 1. Entrada y salida

### 1.1 Entrada

Ruta del contrato + ubicación del artefacto + skills adicionales opcionales + descripción/requisitos opcionales. Todo llega por el prompt — este skill no auto-detecta nada.

### 1.2 Salida

- Los artefactos corregidos en su ubicación (los escriben los subagentes correctores).
- Un **informe final** en la conversación (Fase 4).
- Este skill **no escribe artefactos propios** en disco.

### 1.3 El contrato de revisión

El contrato es un markdown con **exactamente estas ocho secciones**, en este orden. El motor las lee y las inyecta **literales** en los prompts que indica la columna «lo lee»; **MUST NOT** interpretarlas ni resumirlas.

```markdown
# review-contract — <nombre del skill invocante>

## Alcance
<Qué ficheros entran. A qué skill remitir si la ubicación no encaja. Qué hacer si no encaja en ninguno.>

## Skills obligatorios
<Skills que MUST ir siempre en la lista + los condicionales, con su condición.>

## Ejes de revisión
<Lo que el revisor MUST cubrir, como lista.>

## Pasos obligatorios del revisor
<Procedimientos que el revisor MUST ejecutar, o `NINGUNO`.>

## Pasos obligatorios del corrector
<Procedimientos que el corrector MUST ejecutar, o `NINGUNO`.>

## Puertas
<Comando de la puerta y cómo clasificar cada tipo de fallo, o `NINGUNA`.>

## Clasificación específica
<Reglas extra de severidad y de emisión propias del artefacto, o `NINGUNA`.>

## Informe
<Qué añadir al informe final, o `NADA`.>
```

Quién lee cada sección — **MUST** respetarlo al componer los prompts:

| Sección | La lee |
|---|---|
| `## Alcance` | orquestador (Fase 0), revisor y corrector |
| `## Skills obligatorios` | orquestador (Fase 0) |
| `## Ejes de revisión` | revisor |
| `## Pasos obligatorios del revisor` | revisor |
| `## Pasos obligatorios del corrector` | corrector |
| `## Puertas` | orquestador (Fases 1 y 3) |
| `## Clasificación específica` | revisor **y** corrector |
| `## Informe` | orquestador (Fase 4) |

**REQUIRED**: quien escriba un contrato **MUST** poner cada instrucción en la sección que lee su destinatario. Una regla dirigida al corrector escrita en `## Ejes de revisión` no le llega nunca.

- ✅ CORRECTO: `## Puertas` con el comando literal y la regla de clasificación de sus fallos.
- ✅ CORRECTO: `## Pasos obligatorios del corrector` con `NINGUNO` cuando el artefacto no necesita validación extra.
- ❌ INCORRECTO: omitir `## Clasificación específica` porque no aplica (la sección **MUST** existir con el valor `NINGUNA`).
- ❌ INCORRECTO: `## Alcance` sin decir a qué skill remitir (el motor no puede inventar el destino de la STOP condition).

---

## 2. Principios (aplican a todas las fases)

### 2.1 El contrato manda

**MUST** pasar a los subagentes las secciones del contrato **literalmente**, sin resumirlas ni reordenarlas. Si el contrato y este skill se contradicen en algo específico del artefacto, gana el contrato; si se contradicen en el flujo (fases, tokens, límites), gana este skill.

El orquestador **MUST NOT** cargar skills él mismo: solo compone la lista (los obligatorios del contrato + los adicionales del usuario) y la pasa en el prompt de los subagentes (§6.1, §6.2), que son quienes los cargan con `Skill`.

### 2.2 El orquestador no revisa ni corrige

Revisar y corregir lo hacen subagentes con contexto propio. Tu trabajo es ejecutar las puertas, lanzarlos en secuencia, interpretar el token de respuesta y decidir si iterar o parar. **MUST NOT** editar ficheros tú mismo.

### 2.3 Verificar antes de reportar y antes de corregir

- El revisor **MUST** verificar que cada problema existe realmente en el artefacto (no problemas hipotéticos ni ya resueltos) y, antes de reportar que "falta añadir algo", comprobar con grep que de verdad no existe (YAGNI: lo que no se usa en ningún sitio no se reporta como mejora).
- El corrector **MUST** re-verificar cada problema antes de corregirlo y reportarlo como `PUSHBACK` con justificación técnica, sin aplicarlo, en cualquiera de estos dos casos: la corrección sugerida es **técnicamente incorrecta** para este artefacto, o **no consigue dejarla en un estado válido** y revierte el cambio. La justificación **MUST** decir cuál de los dos casos es.

### 2.4 Modo subagente

Si este skill se ejecuta **dentro de un subagente** (otro skill lo invoca vía `Agent`), no hay usuario al que preguntar: ante **cualquier STOP que requiera decisión del usuario** —`UNCLEAR`, `PUSHBACK` o una parada declarada por el contrato en `## Puertas`—, **devuelve el motivo como resultado final**, con sus bloques `BEGIN:----`/`END:----` tal cual (§6.1) cuando los haya, en vez de `AskUserQuestion`/esperar. El orquestador padre decide.

### 2.5 Contexto mínimo de vuelta

El subagente revisor coordina la corrección internamente y devuelve al orquestador solo el token de resultado, los contadores y los bloques `UNCLEAR`/`PUSHBACK` — **MUST NOT** devolver el detalle de cada corrección aplicada ni evidencias largas (no aumenta el contexto principal).

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────┐
│  Fase 0   Validar argumentos + leer contrato + comprobar alcance│
│  Fase 1   Puerta de entrada (si el contrato la declara)         │
│  Fase 2   Bucle (LIMIT 30):                                     │
│             ├── 6.1  Subagente revisor (no modifica nada)       │
│             │         └── si hay problemas: lanza él mismo el   │
│             │             subagente corrector (6.2)             │
│             └── 6.3  Interpretar el token → iterar o parar      │
│  Fase 3   Puerta de salida (si el contrato la declara)          │
│             └── si rojo: volver a Fase 2 (LIMIT 3 reentradas)   │
│  Fase 4   Informe final                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Fase 0 — Validar argumentos y leer el contrato

1. Si falta `--contract=` o la ruta no existe → **ERROR** (STOP condition).
2. Lee el contrato. Si falta alguna de las ocho secciones de §1.3 → **ERROR** indicando cuál.
3. Si falta la ubicación del artefacto → **ERROR** (STOP condition).
4. Comprueba la ubicación contra `## Alcance`. Si no encaja → **STOP** y remite al skill que indique esa sección.
5. Compón la lista de skills: los de `## Skills obligatorios` (evaluando sus condiciones sobre la ubicación real) + los adicionales del usuario. Deja constancia de la lista final en una línea.
6. Pasa a la Fase 1 con: contrato, skills definitivos, ubicación y descripción/requisitos si los hay.

---

## 5. Fase 1 — Puerta de entrada

Si `## Puertas` es `NINGUNA` → salta a la Fase 2 sin ejecutar nada.

1. Ejecuta el comando de la puerta que declare el contrato. Ese comando es **arbitrario por definición** —lo fija cada contrato—, y por eso el `allowed-tools` de este motor declara `Bash` sin acotar en vez de una lista de comandos concretos, que acoplaría el motor al artefacto. **REQUIRED**: el skill invocante **MUST** declarar además en su propio `allowed-tools` el comando concreto de su puerta.
2. Clasifica sus fallos **según la regla del contrato**, no por criterio propio: los que entran como problemas `BLOCKING` de la primera iteración, los que son solo informativos, y los que son STOP conditions.
3. Si la puerta no se puede ejecutar (no compila, no arranca) → **STOP**: sin línea base no hay revisión fiable.

---

## 6. Fase 2 — Bucle de revisión y corrección

**Variables**: `max_iter = 30` (**LIMIT**), `iter = 1`.

`iter` se inicializa a `1` **una sola vez**: **MUST NOT** reiniciarlo en las reentradas desde la Fase 3. El **LIMIT** de 30 es acumulado para toda la ejecución del skill.

### 6.1 Subagente revisor

Lanza **un** subagente (`Agent`, contexto propio, secuencial — **MUST NOT** paralelo ni `run_in_background`) cuyo prompt **MUST** incluir: la lista de skills a cargar con `Skill`, la ubicación del artefacto, las secciones `## Alcance`, `## Ejes de revisión`, `## Pasos obligatorios del revisor` y `## Clasificación específica` del contrato **literales**, la descripción/requisitos (si los hay), los problemas `BLOCKING` heredados de las puertas (los de la Fase 1 en la primera entrada al bucle y los de la Fase 3 en cada reentrada; en las iteraciones normales no se hereda nada) y estas instrucciones:

1. Carga los skills indicados y revisa el artefacto comparándolo con ese conocimiento y con los ejes del contrato.
2. **MUST NOT** modificar ningún fichero durante la revisión.
3. Ejecuta los `## Pasos obligatorios del revisor` del contrato.
4. Verifica cada hallazgo **tuyo** antes de reportarlo (principio 2.3).
5. **MUST** incorporar a la lista del punto 6 los problemas heredados de las puertas **tal cual**, sin reclasificarlos y **sin** someterlos a la verificación del punto 4: ya vienen verificados por la puerta, y son la razón de ser de esta entrada al bucle. Descartarlos deja la reentrada sin contenido.
6. Clasifica cada problema: `BLOCKING` (rompe funcionalidad, integridad o seguridad), `IMPORTANT` (incumple convenciones o requisitos), `MINOR` (mejora menor), aplicando además las reglas de `## Clasificación específica`. Si un problema es ambiguo o no permite una corrección concreta, emítelo como bloque `UNCLEAR` (formato en el punto 9) y **MUST NOT** pasarlo al corrector.
7. Redacta la lista de problemas (solo los de severidad clara) con **exactamente** este formato:

   ```text
   BEGIN:----
   SEVERIDAD: BLOCKING|IMPORTANT|MINOR
   FICHERO: <ruta del fichero afectado>
   Descripción del error, inconsistencia o mejora encontrada
   END:----
   ```

   - ✅ CORRECTO: `BEGIN:----` / `SEVERIDAD: BLOCKING` / `FICHERO: src/…/Foo.java` / descripción / `END:----`
   - ❌ INCORRECTO: `Problema 1 (grave): …` (sin marcadores parseables, sin severidad de la lista cerrada ni fichero)
8. Si hay problemas con severidad clara, **lanza él mismo el subagente corrector** (6.2) y espera a que termine. Si **solo** hay `UNCLEAR`, **MUST NOT** lanzarlo.
9. Devuelve al orquestador **solo** una de estas respuestas:
   - `OK-No hay problemas` (exactamente ese token) — **solo** si no encontró absolutamente nada. **MUST NOT** devolver este token si emite algún bloque: `OK-No hay problemas` y los bloques son **mutuamente excluyentes**, y el orquestador lo evalúa antes que nada (§6.3), de modo que un `UNCLEAR` acompañado de este token se perdería sin llegar al usuario.
   - `CORREGIDO — BLOCKING: <n>, IMPORTANT: <n>, MINOR: <n>`, seguido de un bloque por cada `UNCLEAR` propio y por cada `PUSHBACK` del corrector. Si no se corrigió nada porque solo había `UNCLEAR`, los tres contadores van a `0` y este es el token igualmente. Formato **exacto** de los bloques:

     ```text
     BEGIN:----
     UNCLEAR
     FICHERO: <ruta del fichero afectado>
     Qué hay que aclarar y por qué no admite corrección concreta
     END:----

     BEGIN:----
     PUSHBACK
     FICHERO: <ruta del fichero afectado>
     Qué corrección se rechazó y la justificación técnica
     END:----
     ```

   Las dos primeras líneas del bloque —la de tipo y `FICHERO:`— son fijas; el cuerpo es **texto libre multilínea**, y `## Clasificación específica` **MAY** exigir que empiece por líneas estructuradas propias del artefacto.

   - ✅ CORRECTO: la línea de tipo va **desnuda** (`UNCLEAR` o `PUSHBACK`), sin prefijo.
   - ✅ CORRECTO: un cuerpo que empieza por una línea `CLAVE: valor` exigida por `## Clasificación específica`, después de la línea `FICHERO:`.
   - ❌ INCORRECTO: `SEVERIDAD: UNCLEAR` (la línea de tipo no es una severidad; así el orquestador no lo distingue de un problema del punto 7)
   - ❌ INCORRECTO: pegar el detalle de las correcciones aplicadas (viola el principio 2.5)

### 6.2 Subagente corrector (lo lanza el revisor)

El prompt del corrector **MUST** incluir: la lista de skills a cargar, la ubicación del artefacto, las secciones `## Alcance`, `## Pasos obligatorios del corrector` y `## Clasificación específica` del contrato **literales** (§1.3) y la lista de problemas del revisor. Instrucciones:

1. Carga los skills indicados.
2. Corrige los problemas en orden de severidad: primero `BLOCKING`, luego `IMPORTANT`, luego `MINOR`.
3. Para cada problema, re-verifica que existe tal como fue descrito antes de tocarlo. Si la corrección sugerida es técnicamente incorrecta para este artefacto → **MUST NOT** aplicarla; repórtala como `PUSHBACK` con justificación técnica (principio 2.3).
4. Ejecuta los `## Pasos obligatorios del corrector` del contrato sobre cada fichero que toques.
5. **MUST NOT** tocar ficheros fuera del `## Alcance`.
6. Aplica y verifica cada corrección **individualmente** antes de pasar a la siguiente.
7. **MUST NOT** ejecutar las puertas: son responsabilidad del orquestador (§5, §7).
8. Devuelve al revisor **solo**: `CORREGIDO — BLOCKING: <n>, IMPORTANT: <n>, MINOR: <n>` más un bloque `PUSHBACK` por cada rechazo, con el formato literal de §6.1 punto 9.

### 6.3 Control del bucle (orquestador)

1. `OK-No hay problemas` → sal del bucle y pasa a la Fase 3.
2. Hay al menos un bloque cuya línea de tipo es literalmente `UNCLEAR` → **STOP**: pregunta al usuario exactamente qué hay que aclarar (modo subagente: devuelve los bloques como resultado). No se itera con ambigüedades abiertas.
3. Hay al menos un bloque cuya línea de tipo es literalmente `PUSHBACK` → **STOP**: reporta al usuario qué correcciones se rechazaron y por qué, para que decida.
4. `CORREGIDO …` sin `UNCLEAR` ni `PUSHBACK` → `iter = iter + 1`; si `iter <= max_iter`, vuelve a 6.1 (la nueva revisión parte del artefacto ya corregido); si `iter > max_iter`, **STOP** (STOP condition).

---

## 7. Fase 3 — Puerta de salida

Si `## Puertas` es `NINGUNA` → salta a la Fase 4 sin ejecutar nada.

**Variables**: `max_reentradas = 3` (**LIMIT**), `reentradas = 0`.

`reentradas` se inicializa a `0` **una sola vez**, en la primera entrada a esta fase: **MUST NOT** reiniciarlo en las entradas sucesivas. El **LIMIT** de 3 es el total de toda la ejecución.

1. Vuelve a ejecutar el comando de la puerta que declare el contrato.
2. Verde → pasa a la Fase 4.
3. Rojo → clasifica cada fallo **con la misma regla de `## Puertas` que aplicaste en la Fase 1** (§5 punto 2), no por criterio propio. **MUST NOT** asumir que todo fallo rojo lo han causado las correcciones: el contrato puede declarar fallos que son STOP conditions y no material de reentrada. Según la clase que le dé el contrato:
   - Fallo que entra como problema → `reentradas = reentradas + 1`; si `reentradas <= max_reentradas`, vuelve a la Fase 2 con esos fallos como problemas `BLOCKING` y con `iter` **sin reiniciar** (§6); si no, **STOP** y reporta qué quedó rojo (STOP condition).
   - Fallo informativo → anótalo y pasa a la Fase 4 diciéndolo.
   - Fallo que el contrato declara STOP condition → **STOP** y lleva la decisión al usuario (modo subagente: devuélvela como resultado, §2.4). **MUST NOT** gastar reentradas intentando corregirlo.

---

## 8. Fase 4 — Informe final

Presenta al usuario:

- Skill invocante y contrato usado, y lista final de skills de conocimiento.
- Resultado de cada pasada de las puertas (entrada y cada paso por la de salida), si las hubo, y fallos informativos fuera del alcance.
- Iteraciones realizadas, reentradas y resultado del bucle (`OK-No hay problemas` o motivo de la parada).
- Total de problemas corregidos por severidad (suma de los contadores `CORREGIDO`).
- Bloques `UNCLEAR` / `PUSHBACK` pendientes de decisión, si la parada fue por ellos.
- Lo que añada la sección `## Informe` del contrato.

**MUST NOT** afirmar que el artefacto está correcto si la salida del bucle no fue `OK-No hay problemas` o si la puerta de salida quedó roja.

---

## Quick Guidelines

- Eres un **motor agnóstico**: el contrato dice qué se revisa, tú aportas el flujo. **MUST NOT** añadir criterios propios ni asumir el tipo de artefacto.
- El contrato **MUST** traer las ocho secciones de §1.3 (con `NINGUNO`/`NINGUNA`/`NADA` cuando no apliquen); sus secciones se inyectan **literales** en los prompts de los subagentes.
- Eres **orquestador**: el revisor detecta (sin modificar nada) y el corrector arregla; tú ejecutas las puertas, interpretas tokens y decides iterar o parar. **MUST NOT** editar ficheros ni cargar skills tú mismo.
- Las puertas se ejecutan **solo en las puertas** (una a la entrada, una en cada paso por la de salida), **nunca** dentro del bucle: un build por iteración multiplicaría por 30 el coste.
- Tokens literales: `OK-No hay problemas` termina el bucle; `CORREGIDO — BLOCKING: n, IMPORTANT: n, MINOR: n` itera; bloques `BEGIN:----`/`SEVERIDAD:`/`FICHERO:`/`END:----` para los problemas y `BEGIN:----`/`UNCLEAR`|`PUSHBACK`/`FICHERO:`/`END:----` para la vuelta (§6.1).
- `UNCLEAR` y `PUSHBACK` paran el bucle y van al usuario (o se devuelven como resultado en modo subagente, §2.4).
- Subagentes secuenciales, sin `run_in_background`; contexto mínimo de vuelta (tokens, contadores y bloques, no el detalle de las correcciones).
- **LIMIT**: 30 iteraciones (acumuladas, `iter` no se reinicia) y 3 reentradas (`reentradas` tampoco); corrección en orden BLOCKING → IMPORTANT → MINOR, verificada individualmente.

---

## Apéndice A — Argumentos

- `--contract=<ruta>` (REQUIRED) — contrato de revisión. No es un override de testing: es el argumento normal de uso, y sin él el skill no puede funcionar (STOP condition).

Este skill no escribe artefactos propios ni resuelve rutas por su cuenta, así que no admite `--in=`/`--out=`/`--root=`: la ubicación del artefacto llega siempre explícita en el prompt (§1.1).
