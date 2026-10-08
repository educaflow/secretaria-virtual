# DEB-002 — Mensaje del adjunto vacío distinto de la spec

**Skill / fase / origen:** sdd-implementer / Fase 4 — Implementar (§9), task_49 / BLOCKED
**Pregunta:** AdjuntoServiceImpl.validarTamanoContenido (escrito en task_13) emite «El adjunto no puede estar vacío», mientras la spec (entity-Adjunto.md VAL-Adjunto-007), la vista del diseño (Main-Correo.xml:230) y test-unit-desc.md piden «El fichero adjunto está vacío»; la tarea 49 prohíbe adaptar el test al código. ¿Cómo seguimos?
**A:** Relanzar el implementador de task_13 indicándole que corrija solo ese mensaje a «El fichero adjunto está vacío» (como dice la spec) y después relanzar task_49.
**B:** Mantener el mensaje del código («El adjunto no puede estar vacío») y relanzar task_49 documentando en la tarea la discrepancia, para que el test siga lo implementado.
**Opciones descartadas al formular:** abortar la implementación (no es una forma de continuar).
**Contexto:** .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/specification.md | .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/entity-Adjunto.md | .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/design.md | .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/test-unit-desc.md | .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_13.md | .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_49.md | src/main/java/com/educaflow/subsystem/notificaciones/service/impl/AdjuntoServiceImpl.java | .claude/skills/k-validaciones/SKILL.md | .claude/skills/k-code-quality/SKILL.md
