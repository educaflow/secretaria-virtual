# DEB-001 — Crítica BLOCKING residual tras 4 rondas (reentrada 1)

**Skill / fase / origen:** sdd-designer / §15 paso 4 · Fase 6 (§10) — bucle criticar/corregir, ronda 4 de 4 / STOP-LIMIT
**Pregunta:** En la ronda 4 de la crítica sigue abierta la crítica BLOCKING C-solid-021 (el paso 3 de design.md, línea ~282, manda poner en CorreoServiceImpl un comentario que cita R-Correo-001/002, y k-code-quality/comentarios.md prohíbe IDs en el código) y la IMPORTANT C-simplicidad-026 (notas 2, 7 y 9 duplican D6, paso 8 y paso 5). ¿Cómo sigue el designer?
**A:** Regenerar el diseño desde la especificación (§4.4 opción Regenerar): descartar el diseño actual y volver a generarlo con 5 diseñadores y torneo.
**B:** Relanzar el bucle criticar/corregir (4 rondas más) pasando al corrector el JSONL residual completo (C-solid-021 y C-simplicidad-026), y continuar con Fase 7, 8 y 9.
**Opciones descartadas al formular:** «Detener el skill sin dar el diseño por bueno» (abortar no es alternativa); «aceptar con problemas residuales» (no aplica: el residual contiene un BLOCKING, §7.1 del orquestador).
**Contexto:** .sdd/drafts/2026-09-30_19-35_subsistema-sms/specification.md | .sdd/drafts/2026-09-30_19-35_subsistema-sms/design/design.md | .sdd/drafts/2026-09-30_19-35_subsistema-sms/design/log_critica.txt | .sdd/drafts/2026-09-30_19-35_subsistema-sms/design/decisiones.md | .sdd/drafts/2026-09-30_19-35_subsistema-sms/implementation_error_1/error_design.log | .claude/skills/k-code-quality/SKILL.md | .claude/skills/k-code-quality/comentarios.md | .claude/skills/k-sistemas/SKILL.md
