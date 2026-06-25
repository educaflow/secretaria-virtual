# MCP — secretaría virtual

Servidores MCP disponibles en este proyecto y cómo usarlos.

## MCP de IntelliJ (`mcp__intellij-index__`) — el principal

Tienes disponible el MCP de IntelliJ. **Debes usarlo siempre que sea posible** en lugar
de `grep`, `find`, `Glob` o búsquedas manuales con `Bash`.

Estos tools son de solo lectura o refactor seguro a través del índice de IntelliJ y **no
tienen riesgo**, por lo que **debes invocarlos directamente sin pedir confirmación previa
al usuario** y sin anunciar que vas a usarlos: simplemente úsalos. Están preautorizados en
`.claude/settings.local.json` para que no aparezcan prompts de permiso.

Reglas concretas de sustitución:
- Buscar texto en el código → `ide_search_text` (NO `grep`/`rg`/`Bash`).
- Localizar un fichero por nombre → `ide_find_file` (NO `find`/`ls`/`Glob`).
- Localizar una clase → `ide_find_class` (NO buscar el `.java` con grep).
- Ir a la definición de un símbolo → `ide_find_definition`.
- Encontrar usos de un símbolo → `ide_find_references` (NO `grep` por el nombre).
- Renombrar, mover o borrar símbolos/ficheros → `ide_refactor_rename`, `ide_move_file`, `ide_refactor_safe_delete` (NO `mv`/`sed`/edición manual).
- Antes de asumir que el índice está disponible, si dudas, comprueba con `ide_index_status`.

Solo se admite recurrir a `grep`/`find`/`Bash` si el MCP de IntelliJ no está disponible o el
caso queda fuera de lo que ofrece (por ejemplo, búsqueda en ficheros fuera del proyecto indexado).

Los tools disponibles son:

- `ide_find_class` — buscar una clase por nombre
- `ide_find_file` — buscar un fichero por nombre
- `ide_find_definition` — ir a la definición de un símbolo
- `ide_find_references` — encontrar todos los usos de un símbolo
- `ide_find_implementations` — encontrar implementaciones de una interfaz o clase abstracta
- `ide_find_super_methods` — encontrar métodos padre
- `ide_call_hierarchy` / `ide_type_hierarchy` — jerarquías de llamadas y tipos
- `ide_search_text` — búsqueda de texto en el proyecto
- `ide_diagnostics` — diagnósticos y errores del IDE
- `ide_index_status` — estado del índice de IntelliJ
- `ide_refactor_rename` — renombrar símbolo de forma segura
- `ide_refactor_safe_delete` — eliminar símbolo de forma segura
- `ide_move_file` — mover fichero de forma segura
- `ide_sync_files` — sincronizar ficheros con el IDE

Usar estos tools garantiza que las búsquedas y refactorizaciones son correctas y tienen en
cuenta el índice real del proyecto.

## Otros MCP disponibles

- **PostgreSQL** (`mcp__postgres__query`) — Ejecuta consultas SQL **de solo lectura** contra
  la base de datos `educaflow` para inspeccionar datos. Para acceso interactivo con `psql`,
  arrancar/reiniciar la BD o resetearla, ver [`deploy.md`](deploy.md).
- **Playwright** (`mcp__playwright-test__`) — Automatización de navegador para los tests E2E
  contra la app real. El criterio de cuándo usar el MCP vs el CLI y las convenciones de tests
  los define el skill `/k-playwright`; la ejecución E2E del pipeline la orquesta `/sdd-test-e2e`.
- **IDE** (`mcp__ide__getDiagnostics`) — Diagnósticos/errores del IDE para los ficheros abiertos.
