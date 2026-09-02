# agent_docs — índice

Documentación general del proyecto **cargada bajo demanda** (progressive disclosure).
El `CLAUDE.md` raíz solo mantiene lo imprescindible y apunta aquí; carga el fichero
que necesites para la tarea concreta, **no** todos.

## Índice tarea → documento

| Cuándo lo necesitas                                                                                       | Documento                                        |
|-----------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| Saber versiones/librerías del proyecto (Java, Axelor, PostgreSQL, etc.)                                   | [`tech-stack.md`](tech-stack.md)                 |
| Entender cómo está organizado el proyecto (paquetes, sistemas/subsistemas)                                 | [`architecture.md`](architecture.md)             |
| Entender la arquitectura de **expedientes** y trámites                                                    | skills `k-tramite` y `k-tipo-expediente`         |
| Reglas de arquitectura que se verifican con ArchUnit (capas, Controller→Service→Repository, nomenclatura) | [`architecture-rules.md`](architecture-rules.md) |
| Reglas de las vistas Axelor que se verifican con JUnit sobre los XML (nombres, botones, action-groups, modales, grids) | [`view-rules.md`](view-rules.md) |
| Trabajar con el pipeline SDD (`/sdd-*`): qué hace cada skill, en qué orden y con qué familia de plantillas (sistema / expediente) | [`sdd-workflow.md`](sdd-workflow.md) |
| Usar los MCP del proyecto (IntelliJ, PostgreSQL, Playwright, IDE)                                         | [`mcp.md`](mcp.md)                               |
| Compilar, probar tests, arrancar la app, reiniciar/acceder a la BD (`psql`)                               | [`deploy.md`](deploy.md)                         |

## Convención

- Cada documento cubre **un tema** y es autocontenido.
- Para que un documento sea descubrible, **MUST** estar enlazado desde este índice
  y/o referenciado por un puntero en `CLAUDE.md` o en el skill que lo consume: un
  fichero suelto en `agent_docs/` no se carga solo.
- Al añadir un documento nuevo, añade su fila en la tabla de arriba.
