---
name: developer-code-reviewer
description: Revisa y corrige código Java/Kotlin del proyecto iterando ciclos revisor → corrector con subagentes. La entrada es la ubicación del código más uno o más skills de conocimiento (y opcionalmente la descripción y requisitos de lo construido); la salida es el código corregido en su ubicación más un informe final. El flujo (bucle, tokens, severidades BLOCKING/IMPORTANT/MINOR, STOP ante UNCLEAR y PUSHBACK, LIMIT 30 iteraciones) lo aporta el motor `/skill-orquestador-reviewer`; qué se revisa exactamente lo declara `review-contract.md`, en esta misma carpeta. Para vistas XML usa `/developer-view-reviewer`; para modelos de dominio, `/developer-model-reviewer`.
allowed-tools: Bash(ls:*), Bash(grep:*), Bash(find:*), Read, Skill, AskUserQuestion, Agent
---

# developer-code-reviewer

Eres el skill de revisión de **código Java/Kotlin**. No implementas el bucle de revisión: lo aporta el motor `/skill-orquestador-reviewer`. Tu trabajo es comprobar el alcance, delegar en el motor con el contrato de esta carpeta y presentar su informe.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- **Skills de conocimiento** (REQUIRED): uno o más nombres de skills (`k-code-quality`, `k-sistemas`, `k-guice`, …) que los subagentes cargarán como criterio. A diferencia de sus skills hermanos, aquí el criterio principal lo aporta el usuario: sin skills no hay revisión posible.
- **Ubicación del código** (REQUIRED): rutas o paquetes del código Java/Kotlin a revisar.
- **Descripción y requisitos** (opcional): qué se ha construido y qué debe cumplir. Si se proporciona, es el criterio principal de revisión.

---

## Outline

1. **Validar** los argumentos y el alcance (Fase 0).
2. **Delegar** en `/skill-orquestador-reviewer` con `review-contract.md` (Fase 1).
3. **Presentar** el informe que devuelve el motor (Fase 2).

**STOP conditions**:

- No se indica ningún skill de conocimiento → **ERROR** y detente indicando que falta el skill a usar.
- No se indica la ubicación del código → **ERROR** y detente indicando que falta el código a revisar.
- La ubicación no encaja en el `## Alcance` de `review-contract.md` → **STOP** y remite **al destino que esa sección indique para ese caso concreto**, aplicando la exclusión **más específica** que case. **MUST NOT** deducir el destino por tu cuenta ni tomarlo de las menciones orientativas del `description`: el `## Alcance` es la única fuente.
- El motor se detiene por `UNCLEAR`, `PUSHBACK` o **LIMIT** → traslada su parada tal cual al usuario; **MUST NOT** continuar por tu cuenta.

---

## 1. Entrada y salida

### 1.1 Entrada

Skills de conocimiento + ubicación del código + descripción/requisitos opcionales. Todo llega por el prompt — este skill no auto-detecta nada.

### 1.2 Salida

- El código corregido en su ubicación (lo escriben los subagentes correctores del motor).
- El **informe final** del motor, más lo que añada la sección `## Informe` del contrato.
- Este skill **no escribe artefactos propios** en disco.

### 1.3 Estructura de carpetas

```
.claude/skills/developer-code-reviewer/
├── SKILL.md            ← este fichero: alcance y delegación
└── review-contract.md  ← qué se revisa exactamente (lo consume el motor)
```

---

## 2. Fase 0 — Validar argumentos y alcance

1. Si falta la lista de skills → **ERROR** (STOP condition).
2. Si falta la ubicación del código → **ERROR** (STOP condition).
3. Comprueba la ubicación contra el `## Alcance` de `review-contract.md`. Si no encaja → **STOP** con el destino que indique esa sección (STOP condition). **MUST** aplicar la exclusión **más específica** que case, no la primera.

---

## 3. Fase 1 — Delegar en el motor

1. Carga `/skill-orquestador-reviewer` con `Skill`.
2. Ejecuta su flujo con estos argumentos:
   - `--contract=.claude/skills/developer-code-reviewer/review-contract.md`
   - la ubicación del código,
   - los skills de conocimiento que indicó el usuario (el motor añadirá los obligatorios del contrato),
   - la descripción/requisitos, si los hay.
3. **MUST NOT** revisar, corregir ni editar ficheros tú mismo: todo lo hacen los subagentes del motor.
4. **MUST NOT** alterar el flujo del motor (fases, tokens, límites): lo específico de este skill vive en el contrato, no aquí.

---

## 4. Fase 2 — Informe final

Presenta el informe que devuelve el motor sin reinterpretarlo. **MUST NOT** afirmar que el código está correcto si el motor no terminó con `OK-No hay problemas`.

---

## Quick Guidelines

- Alcance: **código Java/Kotlin**. Qué queda fuera y a dónde remitir cada caso lo dice el `## Alcance` de `review-contract.md`, que es la única fuente.
- El bucle no vive aquí: lo aporta `/skill-orquestador-reviewer`. Lo que se revisa vive en `review-contract.md`. **MUST NOT** duplicar ninguno de los dos en este fichero.
- Sin skills de conocimiento o sin ubicación → **ERROR**. Aquí el criterio lo aporta el usuario, no una lista fija.
- **MUST NOT** editar ficheros tú mismo ni continuar cuando el motor para por `UNCLEAR`, `PUSHBACK` o **LIMIT**.
