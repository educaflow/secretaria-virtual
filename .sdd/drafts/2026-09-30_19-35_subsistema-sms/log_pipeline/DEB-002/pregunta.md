# DEB-002 — Líneas Sms en el auth.xml global

**Skill / fase / origen:** sdd-implementer / Fase 4 — implementar (task_26, data-init de permisos SMS) / DUDA
**Pregunta:** El `src/main/resources/data-init/input/auth.xml` global tiene dos líneas sin commitear (añadidas por la vuelta 1 de este mismo pipeline, ahora apartada en implementation_error_1/; NO estaban en la línea base de git status) que asignan al grupo `users` los permisos `Sms.propio-destinatario` y `Sms.propio-centro-gestion`. El diseño dice que ese fichero no se toca y task_26 ya los asigna desde `subsystem/sms/data-init`. Duda del implementer: quizá sean lo único que los asigna en una BD vacía, porque el global es el que crea el grupo `users`. ¿Quitar esas dos líneas o dejarlas?
**A:** Quitar las dos líneas del auth.xml global (cumplir el diseño; la asignación queda en subsystem/sms/data-init según task_26).
**B:** Dejarlas tal cual y continuar.
**Opciones descartadas al formular:** ninguna
**Contexto:** .sdd/drafts/2026-09-30_19-35_subsistema-sms/specification.md | .sdd/drafts/2026-09-30_19-35_subsistema-sms/design/design.md | src/main/resources/data-init/input/auth.xml | src/main/java/com/educaflow/subsystem/sms/data-init/input-config.xml | src/main/java/com/educaflow/subsystem/sms/data-init/input/auth-sms.xml | .sdd/drafts/2026-09-30_19-35_subsistema-sms/implementation/task_26.md | .claude/skills/k-datainit/SKILL.md | .claude/skills/k-sistemas/SKILL.md | (orden de carga y cómo otros subsistemas, p. ej. src/main/java/com/educaflow/subsystem/correos/data-init/, asignan permisos al grupo `users`)
