---
type: implementation-task
template: system
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-skill
- k-secure-coding

Aplica la **ampliación normativa** que el diseño deja pendiente: añadir `system/ventanilla` a la enumeración de
paquetes que **MUST NOT** usar `User.centroActivo`, en los dos ficheros normativos que hoy solo nombran
`subsystem/expedientes`, `subsystem/tramitador` y `tramites`.

> **Decisión del descomponedor (no del diseño):** este cambio **no** tiene fila en la tabla «Ficheros a crear o
> modificar» porque no es código del sistema, sino documentación normativa del proyecto. El diseño lo declara
> igualmente como trabajo de `/sdd-implementer` («**MUST NOT** aplicar ninguno de los tres el diseñador: los aplica
> `/sdd-implementer`»), así que se descompone como tarea propia y va la última de las tareas de producción, después
> de que el sistema al que la excepción se refiere ya exista en el árbol. Los dos ficheros **ya existen**: se edita
> **solo** la enumeración indicada y **MUST NOT** tocarse ninguna otra regla de ninguno de los dos.
>
> Al tocarse un `SKILL.md`, **MUST** aplicarse el skill `k-skill` (reglas de redacción y estructura de los skills del
> proyecto), tal y como exige el `CLAUDE.md` del proyecto.

## Diseño (verbatim)

## Cambios necesarios fuera del sistema

Los dos primeros son ficheros de datos del proyecto; el tercero es la única ampliación normativa pendiente. De las reglas de vistas no queda ninguna: tanto el `<grid action="…">` de los pasos 1 y 2 (D4) como la rama del asistente de `VAR-7.2` (D8) ya están recogidos en `agent_docs/view-rules.md` y en los skills `k-vistas`. **MUST NOT** aplicar ninguno de los tres el diseñador: los aplica `/sdd-implementer`.

3. **Ampliar a `system/ventanilla` la «Excepción — expedientes» de `.claude/skills/k-secure-coding/SKILL.md` §4 y el párrafo equivalente de `CLAUDE.md` § «La aplicación»** — los dos enumeran hoy solo `subsystem/expedientes`, `subsystem/tramitador` y `tramites` como los paquetes que **MUST NOT** usar `User.centroActivo`. Texto exacto de lo que hay que añadir y por qué: **D9** de `decisiones.md`. **MUST NOT** tocarse ninguna otra regla de esos dos ficheros.

### D9 de `design/decisiones.md` (verbatim) — único dueño del alcance y del texto a añadir

**Lo que hay que ampliar fuera del sistema** (recogido como punto 3 de «Cambios necesarios fuera del sistema» de `design.md`; lo aplica `/sdd-implementer`, **MUST NOT** aplicarlo el diseñador, y **MUST NOT** tocarse ninguna otra regla de esos ficheros):
1. `.claude/skills/k-secure-coding/SKILL.md` §4, párrafo «**Excepción — expedientes.**»: añadir `system/ventanilla` a la enumeración de paquetes que **MUST NOT** usar `centroActivo`, con el mismo motivo que los otros tres (el centro es el del expediente, elegido al crearlo entre los del usuario).
2. `CLAUDE.md`, párrafo «## La aplicación»: añadir `system/ventanilla` a la frase que hoy nombra `subsystem/expedientes`, `subsystem/tramitador` y `tramites` como los que **MUST NOT** usar `User.centroActivo`.

