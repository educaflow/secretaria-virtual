# DEB-004 — P-006 (MINOR) obliga a reescribir la precondición que los 56 `.desc.md` embeben: aplicarlo o dejarlo como residuo

**Skill / fase / origen:** sdd-designer / §14 paso 4 → Fase 6 (§10), bucle verificar/corregir, iteración 3 de 10 / CONFLICT
**Pregunta:** El verificador reporta P-006 (MINOR), que obliga a reescribir la sección «Precondición del administrador» de `design/test-e2e-desc.md` — texto que los 56 `.desc.md` embeben verbatim (56/56 comprobados), así que aplicarlo invalida los 30 `[x]` ya ganados (T-001..T-018, T-021..T-032); pero no aplicarlo impide que el verificador llegue nunca a `OK-CORRECTO` y agotará el LIMIT 10. ¿Se aplica P-006 o se deja como residuo declarado?
**A:** NO aplicar P-006 y preservar los 30 `[x]`: el corrector arregla P-001..P-005 (incluida la BLOCKING P-001) y P-006 queda anotado como **residuo conocido** documentado en `decisiones.md` / `design.md`.
**B:** Aplicar P-006 y corregir la contradicción de la precondición, asumiendo que los 30 `[x]` quedan invalidados y hay que reejecutar los 56 tests desde cero.
**Opciones descartadas al formular:** ninguna.

**Puntos que los defensores MUST resolver con evidencia, no de oído:**
1. **Qué dice exactamente P-006** (léelo en `design/log_revision.txt`): ¿describe una contradicción que haría **fallar o dar un falso verde** a algún test, o es una imprecisión redaccional sin efecto sobre la ejecución? De esto depende casi todo.
2. ¿Afecta P-006 a los 30 tests **ya ejecutados**, o solo a alguno de los 24 pendientes? Si el texto contradictorio no interviene en lo que esos 30 comprobaron, el riesgo de conservarlos es distinto.
3. El ejecutor propone, dentro de su opción A, **«cerrar la fase igualmente sin `OK-CORRECTO` sobre ese único punto»**. Valorad si eso es admisible según el skill o si es una desviación que habría que sustituir por la salida que el propio protocolo prevé (agotar el LIMIT y documentar el residuo).
4. Coste real de B: reejecutar 56 tests E2E contra la app real, con su bucle de corrección.

**Contexto:**
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/design/log_revision.txt (el JSONL con P-001..P-006)
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/design/test-e2e-desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/tests-e2e-desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/error_design_2.log
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/design/design.md
- .claude/skills/sdd-designer/SKILL.md
- .claude/skills/sdd-debug-with-test-e2e-desc/SKILL.md
- .claude/skills/sdd-create-tests-e2e/SKILL.md
