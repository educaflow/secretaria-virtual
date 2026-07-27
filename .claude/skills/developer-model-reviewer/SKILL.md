---
name: developer-model-reviewer
description: Revisa y corrige los modelos de dominio Axelor (todo XML bajo `src/main/java/com/educaflow/**` cuya raíz sea `<domain-models>`: las carpetas `domains/`, los `domains.xml` de los tipos de expediente y los modelos auxiliares de pantalla como `views-models/`, que pese al nombre no son vistas) iterando ciclos revisor → corrector con subagentes. La entrada es la ubicación de los dominios (y opcionalmente skills adicionales y la descripción/requisitos de lo construido); la salida son los dominios corregidos más un informe final. El flujo (bucle, tokens, severidades, STOP ante UNCLEAR y PUSHBACK, LIMIT 30 iteraciones) lo aporta el motor `/skill-orquestador-reviewer`; qué se revisa exactamente —los ejes contra `k-sistemas`/`k-validaciones`/`k-secure-coding`, el chequeo de regresión XSD del corrector y el tratamiento de los cambios destructivos de esquema— lo declara `review-contract.md`, en esta misma carpeta. Para código Java/Kotlin usa `/developer-code-reviewer`; para vistas XML, `/developer-view-reviewer`.
allowed-tools: Bash(ls:*), Bash(grep:*), Bash(find:*), Bash(xmllint:*), Read, Skill, AskUserQuestion, Agent
---

# developer-model-reviewer

Eres el skill de revisión de **modelos de dominio Axelor**. No implementas el bucle de revisión: lo aporta el motor `/skill-orquestador-reviewer`. Tu trabajo es comprobar el alcance, delegar en el motor con el contrato de esta carpeta y presentar su informe.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- **Ubicación de los dominios** (REQUIRED): rutas o carpetas de los XML a revisar. Los dominios viven junto al código del sistema o subsistema dueño, en cualquier rama de `src/main/java/com/educaflow/**`, nunca en `resources`, y se reconocen por su elemento raíz `<domain-models>`, no por la carpeta. El `## Alcance` del contrato es la definición exacta.
- **Skills de conocimiento adicionales** (opcional): los obligatorios los declara el contrato; aquí solo van los extra (`k-datainit`, `k-i18n`, …).
- **Descripción y requisitos** (opcional): qué se ha construido y qué debe cumplir. Si se proporciona, es el criterio principal de revisión.

---

## Outline

1. **Validar** los argumentos y el alcance (Fase 0).
2. **Delegar** en `/skill-orquestador-reviewer` con `review-contract.md` (Fase 1).
3. **Presentar** el informe que devuelve el motor (Fase 2).

**STOP conditions**:

- No se indica la ubicación de los dominios → **ERROR** y detente indicando que faltan los dominios a revisar.
- La ubicación no encaja en el `## Alcance` de `review-contract.md` → **STOP** y remite **al destino que esa sección indique para ese caso concreto**, aplicando la exclusión **más específica** que case. **MUST NOT** deducir el destino por tu cuenta ni tomarlo de las menciones orientativas del `description`: el `## Alcance` es la única fuente.
- El motor se detiene por `UNCLEAR`, `PUSHBACK` o **LIMIT** → traslada su parada tal cual al usuario; **MUST NOT** continuar por tu cuenta. **CRITICAL**: los `UNCLEAR` marcados como cambio destructivo de esquema los decide el usuario, nunca tú.

---

## 1. Entrada y salida

### 1.1 Entrada

Ubicación de los dominios + skills adicionales opcionales + descripción/requisitos opcionales. Todo llega por el prompt — este skill no auto-detecta nada.

### 1.2 Salida

- Los dominios corregidos en su ubicación (los escriben los subagentes correctores del motor).
- El **informe final** del motor, más lo que añada la sección `## Informe` del contrato.
- Este skill **no escribe artefactos propios** en disco.

### 1.3 Estructura de carpetas

```
.claude/skills/developer-model-reviewer/
├── SKILL.md            ← este fichero: alcance y delegación
└── review-contract.md  ← qué se revisa exactamente (lo consume el motor)
```

---

## 2. Fase 0 — Validar argumentos y alcance

1. Si falta la ubicación de los dominios → **ERROR** (STOP condition).
2. Comprueba la ubicación contra el `## Alcance` de `review-contract.md`. Si no encaja → **STOP** con el destino que indique esa sección (STOP condition). **MUST** aplicar la exclusión **más específica** que case, no la primera.

---

## 3. Fase 1 — Delegar en el motor

1. Carga `/skill-orquestador-reviewer` con `Skill`.
2. Ejecuta su flujo con estos argumentos:
   - `--contract=.claude/skills/developer-model-reviewer/review-contract.md`
   - la ubicación de los dominios,
   - los skills adicionales, si el usuario indicó alguno (el motor añadirá los obligatorios del contrato),
   - la descripción/requisitos, si los hay.
3. **MUST NOT** revisar, corregir ni editar ficheros tú mismo: todo lo hacen los subagentes del motor.

---

## 4. Fase 2 — Informe final

Presenta el informe que devuelve el motor sin reinterpretarlo (el destacado de los cambios destructivos de esquema ya lo trae, porque lo pide la sección `## Informe` del contrato). **MUST NOT** afirmar que los modelos están correctos si el motor no terminó con `OK-No hay problemas`.

---

## Quick Guidelines

- Alcance: todo XML bajo `src/main/java/com/educaflow/**` con raíz `<domain-models>`, **por elemento raíz y no por ruta** (una carpeta llamada `views-models/` puede contener modelos); nunca las entidades generadas. Qué queda fuera y a dónde remitir cada caso lo dice el `## Alcance` de `review-contract.md`, que es la única fuente.
- El bucle no vive aquí: lo aporta `/skill-orquestador-reviewer`. Lo que se revisa —los ejes y los cambios de esquema— vive en `review-contract.md`, igual que el chequeo de regresión XSD del corrector. **MUST NOT** duplicar ninguno de los dos en este fichero.
- Sin ubicación → **ERROR**. Los skills obligatorios los pone el contrato, no la invocación.
- **CRITICAL**: un cambio destructivo de esquema puede perder datos; para el flujo y lo decide el usuario.
- **MUST NOT** editar ficheros tú mismo ni continuar cuando el motor para por `UNCLEAR`, `PUSHBACK` o **LIMIT**.
