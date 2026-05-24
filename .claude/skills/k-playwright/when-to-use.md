# Criterio de uso — qué herramienta para qué tarea

En este proyecto coexisten **tres formas** de interactuar con Playwright. Elegir la correcta evita malgastar tokens y produce resultados más rápidos.

## Modelo mental — dónde actúa la IA

La diferencia de fondo no es de capacidad, sino de **dónde actúa la IA**:

- Un `.spec.ts` lleva IA **solo al generarlo** (generator). Al ejecutarse con `npx playwright test` es código determinista: sin IA, sin tokens, repetible en CI. Si la UI cambia, **falla** — y eso es lo que quieres: un test existe para **detectar regresiones**.
- El Agent CLI lleva IA **en cada ejecución** y se adapta a cambios pequeños sobre la marcha. Esa adaptabilidad sirve para *explorar*, pero **MUST NOT** usarse como suite de tests: al improvisar puede pasar por encima de un bug real y **ocultar la regresión** que el test debería cazar.

**Tradeoff asumido a propósito**: construir la suite con el generator (MCP) cuesta ~4× más tokens que pilotar con el Agent CLI, pero produce un artefacto **determinista y barato de re-ejecutar**. El proyecto acepta ese coste a cambio de reproducibilidad; la resiliencia ante cambios legítimos de UI la aporta el **healer** bajo demanda, no la IA improvisando en cada run.

## Las tres opciones

| Herramienta | Cómo se usa | Documentada en |
|-------------|-------------|----------------|
| **Test Agents (MCP)** | Invocando subagentes `playwright-test-planner` / `generator` / `healer` | `test-agents.md` |
| **Agent CLI** | Ejecutando comandos `playwright-cli ...` por Bash | `agent-cli.md` |
| **`@playwright/test` directo** | `npx playwright test ...` por Bash | `conventions.md` |

## Árbol de decisión

```
¿Quieres construir o mantener tests E2E reutilizables?
├── Sí, diseñar plan de tests para una pantalla nueva
│   → planner (Test Agents)
├── Sí, convertir un plan ya hecho en .spec.ts
│   → generator (Test Agents)
├── Sí, los tests existentes fallan tras un cambio
│   → healer (Test Agents)
└── No, solo quiero pilotar el navegador puntualmente
    │
    ├── ¿Para verificar manualmente un comportamiento tras un cambio de código?
    │   → Agent CLI (`playwright-cli open ...`)
    ├── ¿Para depurar un bug de UI viendo consola/network?
    │   → Agent CLI (`console messages`, `requests list`)
    ├── ¿Para correr la suite existente y ver si pasa?
    │   → @playwright/test directo (`npx playwright test`)
    └── ¿Para abrir el informe HTML del último run?
        → @playwright/test directo (`npx playwright show-report`)
```

## Casos concretos

### "Crea tests para la pantalla de expedientes"

1. **planner** → `specs/expedientes.plan.md`.
2. Revisar el plan con el usuario.
3. **generator** por cada escenario → `tests/expedientes/*.spec.ts`.

### "Los tests de login se han roto"

1. `npx playwright test tests/login` para ver qué falla concretamente.
2. **healer** sobre los que fallan.
3. Re-ejecutar para validar.

### "Cambié el formulario de registro, ¿sigue funcionando?"

- Si **existe un test** que lo cubre → `npx playwright test tests/registro/`.
- Si **no existe test** y solo quieres verificar una vez → Agent CLI:
  ```
  playwright-cli open http://localhost:8080/#/registro
  playwright-cli ...
  ```
- Si **debería existir test** y aún no → planner + generator para crearlo.

### "Hay un error en la consola del navegador en producción"

- Agent CLI: `playwright-cli open <url> && playwright-cli console messages`.

### "¿Qué peticiones hace la app al cargar el dashboard?"

- Agent CLI: `playwright-cli requests list` después de navegar.

### "Quiero ver el informe del último run de CI"

- `@playwright/test`: `npx playwright show-report`.

## Errores comunes

| Error | Por qué es un error | Qué hacer en su lugar |
|-------|---------------------|------------------------|
| Usar planner para generar tests | El planner solo planifica, no escribe `.spec.ts` | Planner → generator |
| Editar `.spec.ts` a mano tras el generator | Pierdes la reproducibilidad y el log estructurado | Volver a invocar al generator con un escenario corregido |
| Usar Agent CLI para escribir tests | Sus comandos no producen ficheros `.spec.ts` | Test Agents (generator) |
| Healer arregla la app | El healer edita tests, no código de producción | Si el bug es de la app, fíjalo aparte y luego corre el test |
| Lanzar tests sin la app arrancada | Falla todo con timeouts | Arrancar Gradle primero, o configurar `webServer` en config |
| Commitear `.playwright-mcp/` | Son trazas temporales | Añadir a `.gitignore` |

## Reglas duras

1. **Un test = un fichero = un escenario.** No agrupar varios `test()` en un `.spec.ts`.
2. **El plan se revisa antes del generator.** No saltarse el paso del planner para escenarios complejos.
3. **El healer no oculta bugs.** Si un test falla por un bug real de la app, pararse y reportar.
4. **No mezclar herramientas en una sola tarea.** Si has decidido usar Test Agents, no te pases a Agent CLI a mitad.
5. **No commiteamos snapshots ni trazas.** `.playwright-mcp/`, `test-results/`, `playwright-report/` están fuera del repo.
