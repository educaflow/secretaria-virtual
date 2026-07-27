---
name: developer-view-reviewer
description: Revisa y corrige las vistas Axelor (`**/views/*.xml` y `menus.xml`) iterando ciclos revisor → corrector con subagentes. La entrada es la ubicación de las vistas (y opcionalmente skills adicionales y la descripción/requisitos de lo construido); la salida son las vistas corregidas más un informe final. El flujo (puertas de tests, bucle, tokens, severidades, STOP ante UNCLEAR y PUSHBACK, LIMIT 30 iteraciones y 3 reentradas) lo aporta el motor `/skill-orquestador-reviewer`; qué se revisa exactamente —auditoría ASCII Layout de cada `<form>`, sensatez de acciones y condiciones y frontera de confianza, más el chequeo de regresión XSD del corrector y las puertas de tests— lo declara `review-contract.md`, en esta misma carpeta. Para código Java/Kotlin usa `/developer-code-reviewer`; para modelos de dominio, `/developer-model-reviewer`.
allowed-tools: Bash(ls:*), Bash(grep:*), Bash(find:*), Bash(xmllint:*), Bash(./gradlew:*), Read, Skill, AskUserQuestion, Agent
---

# developer-view-reviewer

Eres el skill de revisión de **vistas Axelor**. No implementas el bucle de revisión: lo aporta el motor `/skill-orquestador-reviewer`. Tu trabajo es comprobar el alcance, delegar en el motor con el contrato de esta carpeta y presentar su informe.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- **Ubicación de las vistas** (REQUIRED): rutas o carpetas de los XML a revisar (`**/views/*.xml`, `menus.xml`).
- **Skills de conocimiento adicionales** (opcional): los obligatorios los declara el contrato; aquí solo van los extra (`k-validaciones`, `k-i18n`, …).
- **Descripción y requisitos** (opcional): qué se ha construido y qué debe cumplir. Si se proporciona, es el criterio principal de revisión.

---

## Outline

1. **Validar** los argumentos y el alcance (Fase 0).
2. **Delegar** en `/skill-orquestador-reviewer` con `review-contract.md` (Fase 1).
3. **Presentar** el informe que devuelve el motor (Fase 2).

**STOP conditions**:

- No se indica la ubicación de las vistas → **ERROR** y detente indicando que faltan las vistas a revisar.
- La ubicación no encaja en el `## Alcance` de `review-contract.md` → **STOP** y remite **al destino que esa sección indique para ese caso concreto**, aplicando la exclusión **más específica** que case (unos `.java` bajo `src/test/java/com/educaflow/views` son tests de vistas, no código genérico). **MUST NOT** deducir el destino por tu cuenta ni tomarlo de las menciones orientativas del `description`: el `## Alcance` es la única fuente.
- El motor se detiene por `UNCLEAR`, `PUSHBACK` o **LIMIT** → traslada su parada tal cual al usuario; **MUST NOT** continuar por tu cuenta.

---

## 1. Entrada y salida

### 1.1 Entrada

Ubicación de las vistas + skills adicionales opcionales + descripción/requisitos opcionales. Todo llega por el prompt — este skill no auto-detecta nada.

### 1.2 Salida

- Las vistas corregidas en su ubicación (las escriben los subagentes correctores del motor).
- El **informe final** del motor, más lo que añada la sección `## Informe` del contrato.
- Este skill **no escribe artefactos propios** en disco.

### 1.3 Estructura de carpetas

```
.claude/skills/developer-view-reviewer/
├── SKILL.md            ← este fichero: alcance y delegación
└── review-contract.md  ← qué se revisa exactamente (lo consume el motor)
```

---

## 2. Fase 0 — Validar argumentos y alcance

1. Si falta la ubicación de las vistas → **ERROR** (STOP condition).
2. Comprueba la ubicación contra el `## Alcance` de `review-contract.md`. Si no encaja → **STOP** con el destino que indique esa sección (STOP condition). **MUST** aplicar la exclusión **más específica** que case, no la primera: unos `.java` bajo `src/test/java/com/educaflow/views` son tests de vistas, no código genérico.

---

## 3. Fase 1 — Delegar en el motor

1. Carga `/skill-orquestador-reviewer` con `Skill`.
2. Ejecuta su flujo con estos argumentos:
   - `--contract=.claude/skills/developer-view-reviewer/review-contract.md`
   - la ubicación de las vistas,
   - los skills adicionales, si el usuario indicó alguno (el motor añadirá los obligatorios del contrato),
   - la descripción/requisitos, si los hay.
3. **MUST NOT** revisar, corregir ni editar ficheros tú mismo: todo lo hacen los subagentes del motor.
4. **MUST NOT** ejecutar los tests de vistas por tu cuenta: son las **puertas** que declara el contrato y las ejecuta el motor en sus Fases 1 y 3.

---

## 4. Fase 2 — Informe final

Presenta el informe que devuelve el motor sin reinterpretarlo. **MUST NOT** afirmar que las vistas están correctas si el motor no terminó con `OK-No hay problemas` o si la puerta de salida quedó roja.

---

## Quick Guidelines

- Alcance: **vistas XML** (`**/views/*.xml`, `menus.xml`). Qué queda fuera y a dónde remitir cada caso lo dice el `## Alcance` de `review-contract.md`, que es la única fuente — ojo con los `.java` de test de vistas, que **no** van a `/developer-code-reviewer`.
- El bucle no vive aquí: lo aporta `/skill-orquestador-reviewer`. Lo que se revisa —ASCII Layout, acciones y condiciones, frontera de confianza— vive en `review-contract.md`, igual que el chequeo de regresión XSD del corrector y las puertas de tests. **MUST NOT** duplicar ninguno de los dos en este fichero.
- Sin ubicación → **ERROR**. Los skills obligatorios los pone el contrato, no la invocación.
- **MUST NOT** editar ficheros ni ejecutar las puertas tú mismo, ni continuar cuando el motor para por `UNCLEAR`, `PUSHBACK` o **LIMIT**.
