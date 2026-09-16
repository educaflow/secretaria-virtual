# DEB-001 — BD acumulativa entre tests E2E: restaurar línea base o devolver el juego de tests al diseñador

**Skill / fase / origen:** sdd-debug-with-test-e2e-desc / Fase 4 — bucle de corrección de T-004, ciclo 1/10 (el corrector devolvió BLOQUEADO) / BLOCKED
**Pregunta:** T-004 falla porque la BD acumula expedientes de tests anteriores (00002/2026 abierto con ciclo DAW creado por T-002) y la regla de «solicitud en curso duplicada» lo rechaza legítimamente; el corrector afirma que no hay bug de código y que T-005 caerá igual — ¿restauro la línea base de demo (borrar los expedientes residuales del trámite) antes de cada test que cree una solicitud, o se acepta la BD acumulativa y el conflicto vuelve a /sdd-designer?
**A:** Restaurar la línea base de demo: borrar los expedientes residuales de `anulacion_matricula_ciclo_formativo_v1` antes de cada test que cree una solicitud, y reintentar T-004 (y aplicarlo a los siguientes tests que creen solicitud).
**B:** Aceptar que la BD es acumulativa y considerar que el juego de tests se contradice a sí mismo: devolver a `/sdd-designer` el `test-e2e-desc.md` para que rediseñe los escenarios de forma que no colisionen entre sí.
**Opciones descartadas al formular:** ninguna (el skill ofrecía exactamente estas dos)
**Contexto:**
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/specification.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/design/design.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/tests-e2e-desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/t-004-el-alumno-vuelve-atras-desde-la-pantalla-de-firma-y-corrige-el-ciclo.desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/app.log
- src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatricula.kt
- .claude/skills/k-playwright/SKILL.md
- .claude/skills/k-tipo-expediente/SKILL.md
- .claude/skills/k-datainit/SKILL.md
- .claude/skills/sdd-create-tests-e2e/SKILL.md (qué se espera del test persistido como regresión)
- agent_docs/deploy.md (cómo se resetea la BD en este proyecto)
