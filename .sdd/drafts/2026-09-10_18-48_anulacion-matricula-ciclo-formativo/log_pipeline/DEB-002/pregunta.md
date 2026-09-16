# DEB-002 — Entrada de tests cambiada: redescomponer desde cero o conservar los `[x]` de T-001..T-003

**Skill / fase / origen:** sdd-debug-with-test-e2e-desc / Fase 2 — Descomponer (§7), antes de lanzar al descomponedor / DUDA
**Pregunta:** La entrada `implementation/test-e2e-desc.md` ha cambiado (55 de 61 secciones: `## Actores`, `## Datos de demo`, 53 bloques de test y una sección nueva «Aislamiento entre tests») pero ya existe `test-e2e-desc/` con la descomposición de la vuelta 1 (56 `.desc.md`, 3 en `[x]`, 2 en `[-]`); el skill no tiene rama para una entrada que ha cambiado, así que ¿redescompongo desde cero perdiendo los 3 `[x]`, o conservo los `[x]` de los tests cuyo bloque no haya cambiado — teniendo en cuenta que los bloques de T-001/T-002/T-003 son idénticos byte a byte pero la cabecera común que los 56 ficheros embeben verbatim sí cambió (nuevos actores `alumno5`/`alumno6`, nuevas precondiciones de firma, fila del ciclo y texto de subsanación), de modo que bajo un criterio estricto de «`.desc.md` sin cambios» no califica ningún test?
**A:** Redescomponer desde cero: el descomponedor reescribe los 56 ficheros y el índice todo en `[ ]`/`[-]` según `decomposition.md` §3 (que prohíbe escribir `[x]` al crear el índice); se reejecutan T-001..T-003 y se pierden sus `[x]`. Coherente con que el código se reimplementó entero (18 tareas) después de que esos 3 pasaran.
**B:** Redescomponer conservando los `[x]` de T-001, T-002 y T-003 por tener su bloque de test idéntico, ignorando a efectos de progreso los cambios de la cabecera común embebida. Ahorra 3 reejecuciones, pero da por pasados 3 tests contra código que ya no es el que los hizo pasar.
**Opciones descartadas al formular:** C — «conservar `[x]` solo si el `.desc.md` resultante es idéntico byte a byte»: con los datos medidos por el ejecutor equivale exactamente a A (0 tests conservados), y su única aportación diferencial (dejar la regla escrita para futuras vueltas) sería un cambio del skill, fuera del alcance de esta decisión.
**Contexto:**
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/implementation/test-e2e-desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/implementation_error_1/test-e2e-desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/tests-e2e-desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/error_design_1.log
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/design/design.md
- .claude/skills/sdd-debug-with-test-e2e-desc/template-expediente/decomposition.md
- .claude/skills/sdd-debug-with-test-e2e-desc/SKILL.md
- .claude/skills/sdd-create-tests-e2e/SKILL.md (qué hace después con los `[x]` y los `[-]`)
- .claude/skills/k-playwright/SKILL.md
