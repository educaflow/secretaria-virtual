# Playwright Agent CLI

CLI de Playwright pensada para que un agente de coding (Claude Code, Copilot) pilote un navegador desde **Bash**. Documentación oficial: https://playwright.dev/agent-cli/introduction.

Es **una integración alternativa** a los Playwright Test Agents (MCP). Conviven sin conflicto. Los criterios para elegir están en `when-to-use.md`.

## Cuándo usarla (ya está instalada en este repo)

El binario `playwright-cli` **ya está instalado** (compruébalo con `playwright-cli --help`) y sus docs de comandos viven en el skill `playwright-cli` (`.claude/skills/playwright-cli/`), **distinto de este `k-playwright`**: aquél lo añade `install --skills`, éste es la guía del proyecto.

Úsala solo para **pilotar el navegador en tareas de coding generales** (verificar una pantalla tras un cambio, depurar un bug, comprobar tráfico de red, capturar storage). Si solo quieres construir suite de tests E2E, los Test Agents bastan. **MUST NOT** usarla para escribir tests — no genera `.spec.ts` (ver `when-to-use.md`).

## Instalación (solo si falta en otro entorno)

```bash
# 1. Paquete (global recomendado)
npm install -g @playwright/cli@latest
playwright-cli --help

# 2. Navegadores + dependencias de sistema (Linux)
playwright-cli install-browser --with-deps

# 3. Skills para Claude Code (carga bajo demanda)
playwright-cli install --skills
```

`install --skills` añade documentación de cada grupo de comandos a `.claude/skills/` para que Claude los descubra sin saturar el contexto.

## Modelo de uso

Cada comando devuelve un **snapshot del árbol de accesibilidad** con `ref`s usables en el siguiente comando. El bucle es:

```
open → snapshot → interact (con refs) → snapshot fresco → repeat
```

Por defecto **headless** y output conciso (barato en tokens).

## Comandos principales

### Core / navegación

| Comando | Equivalente Playwright |
|---------|------------------------|
| `playwright-cli open <url>` | `page.goto(url)` y devuelve snapshot |
| `playwright-cli back` / `forward` / `reload` | Navegación histórica |
| `playwright-cli click <ref>` | Click sobre el elemento referenciado |
| `playwright-cli type "<text>"` | Escribir texto en el campo activo |
| `playwright-cli press <key>` | Tecla (Enter, Tab, Escape…) |
| `playwright-cli check <ref>` / `uncheck <ref>` | Checkbox/radio |
| `playwright-cli select <ref> "<value>"` | `<select>` |
| `playwright-cli hover <ref>` | Hover |
| `playwright-cli screenshot [--full-page]` | Captura PNG |

### Tabs y sesiones

| Comando | Para qué |
|---------|----------|
| `playwright-cli tabs list` / `new` / `close` / `switch <n>` | Gestión de pestañas |
| `PLAYWRIGHT_CLI_SESSION=<name> claude` | Sesión aislada (cada nombre = navegador independiente con su storage) |

### Storage

| Comando | Para qué |
|---------|----------|
| `playwright-cli cookies list` / `set` / `delete` | Cookies |
| `playwright-cli localstorage list` / `set` / `delete` | localStorage |
| `playwright-cli sessionstorage ...` | sessionStorage |

### Network

| Comando | Para qué |
|---------|----------|
| `playwright-cli requests list` | Tráfico HTTP capturado |
| `playwright-cli route "<pattern>" --mock <file>` | Mockear respuestas |
| `playwright-cli offline` / `online` | Estado de red |

### DevTools

| Comando | Para qué |
|---------|----------|
| `playwright-cli console messages` | Logs de la consola del navegador |
| `playwright-cli console clear` | Limpiar consola |
| `playwright-cli evaluate "<js>"` | Ejecutar JS en la página |
| `playwright-cli trace start` / `stop` | Trazado de Playwright |
| `playwright-cli video start` / `stop` | Grabar vídeo |

## Patrón de uso típico en este proyecto

Verificar visualmente que el login sigue funcionando tras un cambio:

```bash
playwright-cli open http://localhost:8080/#/login
# (Claude lee el snapshot, identifica refs de los inputs)
playwright-cli click <ref-input-usuario>
playwright-cli type "admin"
playwright-cli click <ref-input-password>
playwright-cli type "admin"
playwright-cli click <ref-boton-login>
playwright-cli console messages   # comprobar que no hay errores
```

Si el comportamiento es correcto, Claude reporta. **Nada de esto produce un test** — es solo verificación.

## Sesiones aisladas

Útil cuando una tarea larga necesita su propio navegador sin pisar a otra:

```bash
PLAYWRIGHT_CLI_SESSION=expedientes claude
# todo lo que ejecute esta instancia va contra el navegador "expedientes"
```

Las sesiones persisten cookies/storage entre comandos, lo que permite mantener el login una vez hecho.

## Diferencias con la integración MCP

| | Test Agents (MCP) | Agent CLI |
|---|---|---|
| Interfaz | Tools `mcp__playwright-test__*` | Comandos shell `playwright-cli ...` |
| Output | Tool result estructurado (verboso) | Texto de shell conciso |
| Modo navegador | Headed por defecto | Headless por defecto |
| Pensado para | Construir tests `.spec.ts` | Pilotar el navegador como herramienta |
| Coste en tokens | Alto | Bajo |

**Pueden coexistir** — la elección depende de la tarea, no son excluyentes.

## Reglas para Claude al usar Agent CLI

1. **No la uses para escribir tests** — para eso están los Test Agents. La Agent CLI no genera `.spec.ts`.
2. **Cada comando devuelve snapshot — léelo antes del siguiente comando.** No encadenes acciones sin observar el resultado intermedio.
3. **Para acciones repetitivas con la misma sesión**, exporta `PLAYWRIGHT_CLI_SESSION` una vez, no por comando.
4. **Comandos shell normales** — se ejecutan por `Bash`, no requieren tools MCP especiales.
