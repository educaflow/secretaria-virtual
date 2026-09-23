---
type: test-e2e-index
---

# Tests E2E — Justificación de falta del profesorado

Índice de los tests E2E de esta iniciativa. Cada test vive en su propio fichero autocontenido. El checkbox se marca `[x]` cuando el test pasa contra la aplicación real (lo gestiona `/sdd-debug-with-test-e2e-desc`).

Estados: `[ ]` pendiente · `[x]` pasado · `[-]` no automatizable (requiere atención manual; se salta por defecto).

- [x] [T-001 — Justifica un día completo](t-001-justifica-un-dia-completo.desc.md)
- [x] [T-002 — Justifica varios días completos](t-002-justifica-varios-dias-completos.desc.md)
- [x] [T-003 — Justifica unas horas de un único día](t-003-justifica-unas-horas-de-un-unico-dia.desc.md)
- [x] [T-004 — Justifica varios días en los que el primero faltó solo unas horas](t-004-justifica-varios-dias-en-los-que-el-primero-falto-solo-unas-horas.desc.md)
- [x] [T-005 — No se puede continuar sin elegir el tipo de jornada faltada](t-005-no-se-puede-continuar-sin-elegir-el-tipo-de-jornada-faltada.desc.md)
- [x] [T-006 — No se puede continuar sin indicar la fecha](t-006-no-se-puede-continuar-sin-indicar-la-fecha.desc.md)
- [x] [T-007 — No se puede justificar una falta de hace más de un año](t-007-no-se-puede-justificar-una-falta-de-hace-mas-de-un-ano.desc.md)
- [x] [T-008 — No se puede justificar una falta con fecha futura](t-008-no-se-puede-justificar-una-falta-con-fecha-futura.desc.md)
- [x] [T-009 — No se pueden justificar varios días completos sin indicar la fecha de fin](t-009-no-se-pueden-justificar-varios-dias-completos-sin-indicar-la-fecha-de-fin.desc.md)
- [x] [T-010 — No se pueden justificar varios días con la fecha de fin igual a la de inicio](t-010-no-se-pueden-justificar-varios-dias-con-la-fecha-de-fin-igual-a-la-de-inicio.desc.md)
- [x] [T-011 — No se pueden justificar unas horas sin indicar la hora de inicio](t-011-no-se-pueden-justificar-unas-horas-sin-indicar-la-hora-de-inicio.desc.md)
- [x] [T-012 — No se pueden justificar unas horas sin indicar la hora de fin](t-012-no-se-pueden-justificar-unas-horas-sin-indicar-la-hora-de-fin.desc.md)
- [x] [T-013 — No se pueden justificar unas horas con la hora de fin anterior a la de inicio](t-013-no-se-pueden-justificar-unas-horas-con-la-hora-de-fin-anterior-a-la-de-inicio.desc.md)
- [x] [T-014 — Al volver atrás se conservan la fecha y la hora ya introducidas](t-014-al-volver-atras-se-conservan-la-fecha-y-la-hora-ya-introducidas.desc.md)
- [x] [T-015 — La vista de solo consulta de ENTRADA_DATOS muestra el periodo sin poder tocarlo](t-015-la-vista-de-solo-consulta-de-entrada-datos-muestra-el-periodo-sin-poder-tocarlo.desc.md)
- [-] [T-016 — El profesor presenta la solicitud y el tramitador ve el periodo en la pantalla de resolución](t-016-el-profesor-presenta-la-solicitud-y-el-tramitador-ve-el-periodo-en-la-pantalla-de-resolucion.desc.md) — manual: el botón «Firmar con AutoFirma y Presentar la solicitud» abre la aplicación de escritorio AutoFirma y exige el certificado digital del profesor instalado en su máquina; la carga de demo no trae ningún certificado.
- [-] [T-017 — El tramitador acepta la justificación y el estado cerrado sigue mostrando el periodo](t-017-el-tramitador-acepta-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.desc.md) — manual: llegar a `PENDIENTE_RESOLUCION` exige presentar con AutoFirma (T-016) y resolver exige el certificado digital del director del centro instalado en el servidor; la carga de demo no trae ninguno de los dos.
- [-] [T-018 — El tramitador rechaza la justificación y el estado cerrado sigue mostrando el periodo](t-018-el-tramitador-rechaza-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.desc.md) — manual: llegar a `PENDIENTE_RESOLUCION` exige presentar con AutoFirma (T-016) y resolver exige el certificado digital del director del centro instalado en el servidor; la carga de demo no trae ninguno de los dos.
- [x] [T-019 — Al cambiar el tipo de jornada faltada se vacían los campos que el nuevo tipo ya no necesita](t-019-al-cambiar-el-tipo-de-jornada-faltada-se-vacian-los-campos-que-el-nuevo-tipo-ya-no-necesita.desc.md)
