---
name: k-playwright
description: Testing E2E con Playwright en la secretaría virtual — guía de las dos integraciones disponibles para Claude (Playwright Test Agents vía MCP con los subagentes planner/generator/healer, y Playwright Agent CLI vía comandos shell), referencia de comandos, convenciones del proyecto sobre estructura de tests, y criterio de cuándo usar cada herramienta.
---

# k-playwright — Testing E2E en la secretaría virtual

Este skill documenta cómo se hacen tests end-to-end de la aplicación con **Playwright** y las dos formas que tiene Claude Code de pilotarlo en este proyecto.

## Ficheros de este skill

| Fichero | Contenido |
|---------|-----------|
| `test-agents.md` | Los **Playwright Test Agents** (vía MCP): subagentes `planner`, `generator`, `healer` instalados en `.claude/agents/`. Flujo plan → test → heal. |
| `agent-cli.md` | La **Playwright Agent CLI** (`playwright-cli`): comandos shell para pilotar el navegador desde Bash. Más barato en tokens, headless por defecto. |
| `conventions.md` | Convenciones del proyecto: estructura `tests/<area>/*.spec.ts`, `seed.spec.ts`, `specs/*.plan.md`, baseURL, login común, fixtures. |
| `when-to-use.md` | Criterio de decisión: cuándo usar Test Agents, cuándo Agent CLI, cuándo no testear con Playwright. |

## Contexto rápido

La aplicación es una secretaría virtual sobre Axelor 8.1 que se sirve por defecto en `http://localhost:8080/`. La ruta de login es `http://localhost:8080/#/login`.

Hay **dos integraciones de Playwright con Claude Code** instaladas en este repo:

1. **Playwright Test Agents (MCP)** — ya instalados. Tres subagentes especializados que producen y mantienen una suite de tests `.spec.ts`. Idóneo para construir cobertura E2E estructurada.
2. **Playwright Agent CLI** — ya instalado (binario `playwright-cli`; sus docs de comandos están en el skill `playwright-cli`, distinto de éste). Comandos shell para pilotar el navegador durante tareas de coding generales (verificar una UI, depurar un bug, **no escribir tests** — no genera `.spec.ts`).

Ambas conviven sin conflicto. La elección depende de la tarea — ver `when-to-use.md`.

## Punto de entrada

Si la tarea es:

- **"Crear tests de la pantalla X"** → `test-agents.md` (planner + generator).
- **"Los tests fallan"** → `test-agents.md` (healer).
- **"Abre la app y comprueba Y"** → `agent-cli.md`.
- **"¿Dónde va este fichero / cómo se llama?"** → `conventions.md`.
- **Duda sobre qué herramienta usar** → `when-to-use.md`.

## Documentación oficial

- Playwright Test Agents: https://playwright.dev/docs/test-agents
- Playwright Agent CLI: https://playwright.dev/agent-cli/introduction
