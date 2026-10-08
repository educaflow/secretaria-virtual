---
type: implementation-task
template: system
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- ninguno (la columna Skill del diseño indica «—»: no cargar ningún skill)

## Dependencia de tramites/util en su CLAUDE.md

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `tramites/util/CLAUDE.md` | Modificar | — | «Depende de `subsystem/correos`» → «Depende de `subsystem/notificaciones`» |

## Paso del diseño

### Paso 7 — Consumidor externo: aviso de subsanación

`tramites/util/CLAUDE.md`: la línea «Depende de `subsystem/correos`.» pasa a «Depende de `subsystem/notificaciones`.».

**Materialización:** edición de texto en `src/main/java/com/educaflow/tramites/util/CLAUDE.md`: solo se cambia esa línea; el resto del fichero se conserva. No es código Java.
