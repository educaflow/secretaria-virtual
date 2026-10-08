# Log del pipeline sdd

**Iniciativa:** .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones
**Inicio:** 2026-10-07 14:47
**Pasos pedidos:** debug tests
**Argumentos:** (vacío) — pasos seleccionados en el TUI: debug tests

## Línea base (git status --porcelain)
```
 M .claude/skills/k-sistemas/controladores.md
 M .claude/skills/k-validaciones/validaciones.md
 M .claude/skills/k-vistas/actions.md
 M .claude/skills/k-vistas/forms.md
 M .claude/skills/sdd-designer/template-system/vistas.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design-guidelines.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/decisiones.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/design.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/domains/Adjunto.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/domains/Correo.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/domains/Notificacion.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/domains/Sms.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/log_best.txt
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/log_critica.txt
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/log_revision.txt
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/log_revision_unit-test.txt
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/menus.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/rules/R-Notificacion-003.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/test-e2e-desc.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/test-unit-desc.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Centro-Correo.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Centro-Notificacion.xml
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Centro-Sms.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/DefaultModelController.xml
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Eleccion-Notificacion.xml
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Main-Correo.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Main-Notificacion.xml
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Main-Sms.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Mis-Correo.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Mis-Notificacion.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Mis-Sms.xml
R  src/main/java/com/educaflow/subsystem/correos/views/Ref-Adjunto.xml -> .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/design/views/Ref-Adjunto.xml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/entity-Adjunto.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/entity-Correo.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/entity-Notificacion.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/entity-Sms.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/log_build.txt
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_01.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_02.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_03.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_04.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_05.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_06.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_07.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_08.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_09.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_10.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_11.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_12.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_13.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_14.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_15.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_16.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_17.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_18.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_19.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_20.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_21.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_22.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_23.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_24.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_25.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_26.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_27.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_28.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_29.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_30.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_31.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_32.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_33.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_34.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_35.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_36.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_37.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_38.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_39.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_40.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_41.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_42.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_43.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_44.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_45.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_46.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_47.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_48.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_49.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_50.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_51.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_52.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_53.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_54.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_55.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_56.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_57.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_58.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_59.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_60.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_61.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_62.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_63.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/tasks.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/test-e2e-desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/log_build.txt
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_01.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_02.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_03.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_04.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_05.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_06.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_07.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_08.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_09.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_10.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_11.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_12.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_13.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_14.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_15.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_16.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_17.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_18.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_19.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_20.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_21.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_22.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_23.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_24.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_25.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_26.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_27.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_28.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_29.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_30.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_31.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_32.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_33.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_34.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_35.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_36.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_37.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_38.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_39.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_40.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_41.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_42.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_43.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_44.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_45.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_46.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_47.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_48.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_49.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_50.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_51.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_52.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_53.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_54.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_55.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_56.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_57.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_58.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_59.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_60.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_61.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/task_62.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/tasks.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_1/test-e2e-desc.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/log_pipeline.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/log_pipeline/DEB-002/pregunta.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/log_pipeline/DEB-002/ronda-1-A.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/log_pipeline/DEB-002/ronda-1-B.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/model.png
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/model.puml
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/screen-adjunto-consulta.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/screen-notificaciones-centro.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/screen-notificaciones-recibidas.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/screen-notificaciones-todas.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/specification.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-001-alta-de-un-correo-y-resultado-del-envio.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-002-alta-de-un-correo-con-un-adjunto.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-003-cancelar-la-eleccion-de-canal.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-004-alta-de-un-correo-sin-motivo.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-005-alta-de-un-correo-con-un-motivo-demasiado-largo.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-006-alta-de-un-correo-con-varias-direcciones-en-el-para.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-007-alta-de-un-correo-sin-el-dni-del-destinatario.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-008-alta-de-un-correo-con-el-dni-no-valido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-009-alta-de-un-correo-sin-el-nombre.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-010-alta-de-un-correo-sin-los-apellidos.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-011-alta-de-un-correo-sin-destinatario-en-el-para.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-012-alta-de-un-correo-con-el-para-de-formato-no-valido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-013-alta-de-un-correo-con-el-en-copia-de-formato-no-valido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-014-alta-de-un-correo-con-el-en-copia-oculta-de-formato-no-valido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-015-alta-de-un-correo-sin-asunto.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-016-alta-de-un-correo-con-el-asunto-demasiado-largo.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-017-alta-de-un-correo-sin-cuerpo.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-018-alta-de-un-correo-sin-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-019-anadir-un-adjunto-sin-nombre-de-fichero.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-020-anadir-un-adjunto-sin-contenido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-021-alta-de-un-correo-con-dos-adjuntos-con-el-mismo-nombre.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-022-adjunto-cuyo-nombre-contiene-una-barra.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-023-la-eleccion-de-canal-no-deja-continuar-hasta-elegir-uno.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-024-formulario-de-alta-del-correo-y-cancelar-el-alta.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-025-descartar-y-quitar-adjuntos-durante-el-alta-del-correo.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-026-adjunto-de-mas-de-10-mb.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-027-alta-de-un-correo-con-varias-direcciones-en-copia-y-en-copia-oculta.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-028-alta-de-un-correo-ligado-al-estado-de-un-expediente-del-mismo-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-029-en-el-alta-solo-se-ofrecen-estados-de-expedientes-del-centro-elegido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-030-alta-de-un-sms-y-resultado-del-envio.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-031-alta-de-un-sms-con-el-telefono-ya-en-formato-internacional.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-032-alta-de-un-sms-sin-el-dni-del-destinatario.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-033-alta-de-un-sms-con-el-dni-no-valido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-034-alta-de-un-sms-sin-nombre-ni-apellidos.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-035-alta-de-un-sms-sin-telefono.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-036-alta-de-un-sms-con-un-telefono-que-no-es-un-movil-de-espana.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-037-alta-de-un-sms-sin-mensaje.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-038-mensaje-sin-acentos-en-el-limite-de-un-sms.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-039-mensaje-con-acentos-en-el-limite-de-un-sms.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-040-alta-de-un-sms-sin-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-041-formulario-de-alta-del-sms-y-cancelar-el-alta.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-042-alta-de-un-sms-con-un-mensaje-solo-de-espacios.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-043-listado-comun-con-correos-y-sms-de-varios-centros.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-044-al-abrir-una-fila-se-abre-el-formulario-de-su-canal.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-045-datos-del-envio-segun-el-estado.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-046-una-notificacion-ya-creada-no-se-puede-modificar-ni-borrar.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-047-buscar-solo-los-sms-en-el-listado.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-048-consultar-y-descargar-un-adjunto-de-un-correo-guardado.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-049-un-sms-ya-creado-no-se-puede-modificar-ni-borrar.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-050-el-listado-muestra-primero-las-notificaciones-mas-recientes.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-051-el-administrador-ve-recibidas-y-todas-pero-no-del-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-052-reenvio-de-un-correo-desde-todas.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-053-reenvio-de-un-sms-desde-todas.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-054-el-supervisor-solo-ve-las-notificaciones-de-su-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-055-el-administrativo-ve-las-notificaciones-de-su-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-056-el-director-ve-las-notificaciones-de-su-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-057-el-jefe-de-estudios-ve-las-notificaciones-de-su-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-058-el-secretario-ve-las-notificaciones-de-su-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-059-el-supervisor-de-dos-centros-ve-las-de-ambos-y-filtra-por-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-060-un-usuario-sin-tipo-ni-cargo-de-gestion-no-ve-del-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-061-el-gestor-del-centro-no-puede-dar-de-alta-notificaciones.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-062-el-gestor-abre-una-notificacion-con-sus-adjuntos-y-descarga-uno.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-063-el-supervisor-reenvia-un-correo-fallido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-064-el-director-reenvia-un-sms-fallido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-065-el-jefe-de-estudios-reenvia-un-correo-fallido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-066-el-secretario-reenvia-un-sms-fallido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-067-el-administrativo-reenvia-un-correo-fallido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-068-el-destinatario-ve-sus-correos-y-sms-enviados-sin-el-motivo.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-069-el-destinatario-abre-un-correo-recibido-y-descarga-su-adjunto.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-070-el-destinatario-no-ve-las-notificaciones-de-otra-persona.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-071-el-destinatario-ve-una-notificacion-enviada-desde-otro-centro.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-072-un-familiar-consulta-sus-notificaciones-recibidas.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-073-el-destinatario-ve-el-en-copia-pero-no-el-en-copia-oculta-ni-los-datos-del-envio.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-074-el-destinatario-abre-un-sms-recibido.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-075-el-aviso-de-subsanacion-queda-registrado-con-su-motivo.desc.md
A  .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-076-el-solicitante-ve-el-aviso-de-subsanacion-en-sus-recibidas.desc.md
AM .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/tests-e2e-desc.md
 M agent_docs/view-rules.md
A  plan_notificaciones.md
 M src/main/java/com/educaflow/base/infrastructure/axelorhelper/ActionResponseHelper.java
 M src/main/java/com/educaflow/base/infrastructure/controller/DefaultModelController.java
 M src/main/java/com/educaflow/base/infrastructure/controller/DefaultModelController.xml
 M src/main/java/com/educaflow/base/util/MetaFileUtil.java
 M src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
D  src/main/java/com/educaflow/subsystem/correos/data-init/input/auth-correos.xml
D  src/main/java/com/educaflow/subsystem/correos/domains/Adjunto.xml
D  src/main/java/com/educaflow/subsystem/correos/domains/Correo.xml
D  src/main/java/com/educaflow/subsystem/correos/module/CorreosModule.java
D  src/main/java/com/educaflow/subsystem/correos/module/MailSenderProvider.java
D  src/main/java/com/educaflow/subsystem/correos/service/AdjuntoService.java
D  src/main/java/com/educaflow/subsystem/correos/service/CorreoService.java
D  src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java
D  src/main/java/com/educaflow/subsystem/correos/views/Centro-Correo.xml
D  src/main/java/com/educaflow/subsystem/correos/views/Main-Correo.xml
D  src/main/java/com/educaflow/subsystem/correos/views/Mis-Correo.xml
RM src/main/java/com/educaflow/subsystem/correos/controller/CorreoController.java -> src/main/java/com/educaflow/subsystem/notificaciones/controller/CorreoController.java
A  src/main/java/com/educaflow/subsystem/notificaciones/controller/NotificacionController.java
RM src/main/java/com/educaflow/subsystem/sms/controller/SmsController.java -> src/main/java/com/educaflow/subsystem/notificaciones/controller/SmsController.java
R  src/main/java/com/educaflow/subsystem/correos/data-init/input-config.xml -> src/main/java/com/educaflow/subsystem/notificaciones/data-init/input-config.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/data-init/input/auth-notificaciones.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/domains/Adjunto.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/domains/Correo.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/domains/Notificacion.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/domains/Sms.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/module/NotificacionesModule.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/AdjuntoService.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/CorreoService.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/NotificacionCanalService.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/NotificacionService.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/SmsService.java
R  src/main/java/com/educaflow/subsystem/correos/service/impl/AdjuntoServiceImpl.java -> src/main/java/com/educaflow/subsystem/notificaciones/service/impl/AdjuntoServiceImpl.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/impl/CorreoServiceImpl.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/impl/NotificacionCanalServiceImpl.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/impl/NotificacionServiceImpl.java
A  src/main/java/com/educaflow/subsystem/notificaciones/service/impl/SmsServiceImpl.java
A  src/main/java/com/educaflow/subsystem/notificaciones/util/GestorNotificacionesUtil.java
AM src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Correo.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Notificacion.xml
AM src/main/java/com/educaflow/subsystem/notificaciones/views/Centro-Sms.xml
AM src/main/java/com/educaflow/subsystem/notificaciones/views/Eleccion-Notificacion.xml
AM src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Correo.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Notificacion.xml
AM src/main/java/com/educaflow/subsystem/notificaciones/views/Main-Sms.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/views/Mis-Correo.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/views/Mis-Notificacion.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/views/Mis-Sms.xml
A  src/main/java/com/educaflow/subsystem/notificaciones/views/Ref-Adjunto.xml
 M src/main/java/com/educaflow/subsystem/security/service/impl/MenuSecurityServiceImpl.java
D  src/main/java/com/educaflow/subsystem/sms/data-init/input-config.xml
D  src/main/java/com/educaflow/subsystem/sms/data-init/input/auth-sms.xml
D  src/main/java/com/educaflow/subsystem/sms/domains/Sms.xml
D  src/main/java/com/educaflow/subsystem/sms/module/SmsModule.java
D  src/main/java/com/educaflow/subsystem/sms/module/SmsSenderProvider.java
D  src/main/java/com/educaflow/subsystem/sms/service/SmsService.java
D  src/main/java/com/educaflow/subsystem/sms/service/impl/SmsServiceImpl.java
D  src/main/java/com/educaflow/subsystem/sms/views/Centro-Sms.xml
D  src/main/java/com/educaflow/subsystem/sms/views/Main-Sms.xml
D  src/main/java/com/educaflow/subsystem/sms/views/Mis-Sms.xml
 M src/main/java/com/educaflow/tramites/util/CLAUDE.md
 M src/main/java/com/educaflow/tramites/util/verificacion/VerificacionHelper.java
 M src/main/resources/data-init/input/auth.xml
 D src/test/e2e/subsystem/correos/t-001-alta-de-un-correo-que-se-envia-con-exito.desc.md
 D src/test/e2e/subsystem/correos/t-001-alta-de-un-correo-que-se-envia-con-exito.spec.ts
 D src/test/e2e/subsystem/correos/t-002-alta-de-un-correo-con-adjunto.desc.md
 D src/test/e2e/subsystem/correos/t-002-alta-de-un-correo-con-adjunto.spec.ts
 D src/test/e2e/subsystem/correos/t-003-alta-sin-el-dni-del-destinatario.desc.md
 D src/test/e2e/subsystem/correos/t-003-alta-sin-el-dni-del-destinatario.spec.ts
 D src/test/e2e/subsystem/correos/t-004-alta-sin-destinatario-en-el-para.desc.md
 D src/test/e2e/subsystem/correos/t-004-alta-sin-destinatario-en-el-para.spec.ts
 D src/test/e2e/subsystem/correos/t-005-alta-sin-asunto.desc.md
 D src/test/e2e/subsystem/correos/t-005-alta-sin-asunto.spec.ts
 D src/test/e2e/subsystem/correos/t-006-alta-sin-cuerpo.desc.md
 D src/test/e2e/subsystem/correos/t-006-alta-sin-cuerpo.spec.ts
 D src/test/e2e/subsystem/correos/t-007-alta-sin-centro.desc.md
 D src/test/e2e/subsystem/correos/t-007-alta-sin-centro.spec.ts
 D src/test/e2e/subsystem/correos/t-008-alta-sin-el-nombre.desc.md
 D src/test/e2e/subsystem/correos/t-008-alta-sin-el-nombre.spec.ts
 D src/test/e2e/subsystem/correos/t-009-alta-sin-los-apellidos.desc.md
 D src/test/e2e/subsystem/correos/t-009-alta-sin-los-apellidos.spec.ts
 D src/test/e2e/subsystem/correos/t-010-alta-con-adjunto-sin-nombre-de-fichero.desc.md
 D src/test/e2e/subsystem/correos/t-010-alta-con-adjunto-sin-nombre-de-fichero.spec.ts
 D src/test/e2e/subsystem/correos/t-011-alta-con-adjunto-sin-contenido.desc.md
 D src/test/e2e/subsystem/correos/t-011-alta-con-adjunto-sin-contenido.spec.ts
 D src/test/e2e/subsystem/correos/t-012-alta-con-para-de-formato-invalido.desc.md
 D src/test/e2e/subsystem/correos/t-012-alta-con-para-de-formato-invalido.spec.ts
 D src/test/e2e/subsystem/correos/t-013-alta-con-el-dni-del-destinatario-invalido.desc.md
 D src/test/e2e/subsystem/correos/t-013-alta-con-el-dni-del-destinatario-invalido.spec.ts
 D src/test/e2e/subsystem/correos/t-014-alta-con-dos-adjuntos-con-el-mismo-nombre-de-fichero.desc.md
 D src/test/e2e/subsystem/correos/t-014-alta-con-dos-adjuntos-con-el-mismo-nombre-de-fichero.spec.ts
 D src/test/e2e/subsystem/correos/t-015-alta-con-el-asunto-demasiado-largo.desc.md
 D src/test/e2e/subsystem/correos/t-015-alta-con-el-asunto-demasiado-largo.spec.ts
 D src/test/e2e/subsystem/correos/t-016-alta-con-en-copia-de-formato-invalido.desc.md
 D src/test/e2e/subsystem/correos/t-016-alta-con-en-copia-de-formato-invalido.spec.ts
 D src/test/e2e/subsystem/correos/t-017-alta-con-en-copia-oculta-de-formato-invalido.desc.md
 D src/test/e2e/subsystem/correos/t-017-alta-con-en-copia-oculta-de-formato-invalido.spec.ts
 D src/test/e2e/subsystem/correos/t-019-el-boton-reenviar-no-aparece-si-el-correo-no-ha-fallado.desc.md
 D src/test/e2e/subsystem/correos/t-019-el-boton-reenviar-no-aparece-si-el-correo-no-ha-fallado.spec.ts
 D src/test/e2e/subsystem/correos/t-020-el-supervisor-solo-ve-los-correos-de-su-centro.desc.md
 D src/test/e2e/subsystem/correos/t-020-el-supervisor-solo-ve-los-correos-de-su-centro.spec.ts
 D src/test/e2e/subsystem/correos/t-022-el-supervisor-descarga-el-adjunto-de-un-correo-de-su-centro.desc.md
 D src/test/e2e/subsystem/correos/t-022-el-supervisor-descarga-el-adjunto-de-un-correo-de-su-centro.spec.ts
 D src/test/e2e/subsystem/correos/t-023-el-destinatario-consulta-un-correo-enviado-con-exito-y-descarga-su-adjunto.desc.md
 D src/test/e2e/subsystem/correos/t-023-el-destinatario-consulta-un-correo-enviado-con-exito-y-descarga-su-adjunto.spec.ts
 D src/test/e2e/subsystem/correos/t-025-el-destinatario-no-ve-un-correo-enviado-con-exito-a-otra-persona.desc.md
 D src/test/e2e/subsystem/correos/t-025-el-destinatario-no-ve-un-correo-enviado-con-exito-a-otra-persona.spec.ts
 D src/test/e2e/subsystem/correos/t-026-el-supervisor-de-dos-centros-ve-los-correos-de-ambos.desc.md
 D src/test/e2e/subsystem/correos/t-026-el-supervisor-de-dos-centros-ve-los-correos-de-ambos.spec.ts
 D src/test/e2e/subsystem/sms/t-001-alta-de-un-sms-y-resultado-del-envio.desc.md
 D src/test/e2e/subsystem/sms/t-001-alta-de-un-sms-y-resultado-del-envio.spec.ts
 D src/test/e2e/subsystem/sms/t-002-alta-con-el-telefono-escrito-ya-en-formato-internacional.desc.md
 D src/test/e2e/subsystem/sms/t-002-alta-con-el-telefono-escrito-ya-en-formato-internacional.spec.ts
 D src/test/e2e/subsystem/sms/t-003-alta-sin-el-dni-del-destinatario.desc.md
 D src/test/e2e/subsystem/sms/t-003-alta-sin-el-dni-del-destinatario.spec.ts
 D src/test/e2e/subsystem/sms/t-004-alta-con-el-dni-con-la-letra-incorrecta.desc.md
 D src/test/e2e/subsystem/sms/t-004-alta-con-el-dni-con-la-letra-incorrecta.spec.ts
 D src/test/e2e/subsystem/sms/t-005-alta-sin-nombre-ni-apellidos.desc.md
 D src/test/e2e/subsystem/sms/t-005-alta-sin-nombre-ni-apellidos.spec.ts
 D src/test/e2e/subsystem/sms/t-006-alta-sin-telefono.desc.md
 D src/test/e2e/subsystem/sms/t-006-alta-sin-telefono.spec.ts
 D src/test/e2e/subsystem/sms/t-007-alta-con-un-telefono-que-no-es-un-movil-de-espana.desc.md
 D src/test/e2e/subsystem/sms/t-007-alta-con-un-telefono-que-no-es-un-movil-de-espana.spec.ts
 D src/test/e2e/subsystem/sms/t-008-alta-sin-mensaje.desc.md
 D src/test/e2e/subsystem/sms/t-008-alta-sin-mensaje.spec.ts
 D src/test/e2e/subsystem/sms/t-009-mensaje-sin-acentos-en-el-limite-de-un-sms.desc.md
 D src/test/e2e/subsystem/sms/t-009-mensaje-sin-acentos-en-el-limite-de-un-sms.spec.ts
 D src/test/e2e/subsystem/sms/t-010-mensaje-con-acentos-en-el-limite-de-un-sms.desc.md
 D src/test/e2e/subsystem/sms/t-010-mensaje-con-acentos-en-el-limite-de-un-sms.spec.ts
 D src/test/e2e/subsystem/sms/t-011-alta-sin-centro.desc.md
 D src/test/e2e/subsystem/sms/t-011-alta-sin-centro.spec.ts
 D src/test/e2e/subsystem/sms/t-012-el-administrador-consulta-los-sms-de-varios-centros.desc.md
 D src/test/e2e/subsystem/sms/t-012-el-administrador-consulta-los-sms-de-varios-centros.spec.ts
 D src/test/e2e/subsystem/sms/t-013-un-sms-ya-creado-no-se-puede-modificar-ni-borrar.desc.md
 D src/test/e2e/subsystem/sms/t-013-un-sms-ya-creado-no-se-puede-modificar-ni-borrar.spec.ts
 D src/test/e2e/subsystem/sms/t-014-el-supervisor-ve-solo-los-sms-de-su-centro.desc.md
 D src/test/e2e/subsystem/sms/t-014-el-supervisor-ve-solo-los-sms-de-su-centro.spec.ts
 D src/test/e2e/subsystem/sms/t-015-el-administrativo-ve-los-sms-de-su-centro.desc.md
 D src/test/e2e/subsystem/sms/t-015-el-administrativo-ve-los-sms-de-su-centro.spec.ts
 D src/test/e2e/subsystem/sms/t-016-reenvio-desde-del-centro-por-el-supervisor.desc.md
 D src/test/e2e/subsystem/sms/t-016-reenvio-desde-del-centro-por-el-supervisor.spec.ts
 D src/test/e2e/subsystem/sms/t-017-reenvio-desde-todos-por-el-administrador.desc.md
 D src/test/e2e/subsystem/sms/t-017-reenvio-desde-todos-por-el-administrador.spec.ts
 D src/test/e2e/subsystem/sms/t-018-el-destinatario-ve-sus-sms-enviados-y-no-los-de-otro.desc.md
 D src/test/e2e/subsystem/sms/t-018-el-destinatario-ve-sus-sms-enviados-y-no-los-de-otro.spec.ts
 D src/test/e2e/subsystem/sms/t-019-cancelar-el-alta-de-un-sms.desc.md
 D src/test/e2e/subsystem/sms/t-019-cancelar-el-alta-de-un-sms.spec.ts
 D src/test/e2e/subsystem/sms/t-020-datos-del-envio-segun-el-estado-en-el-detalle.desc.md
 D src/test/e2e/subsystem/sms/t-020-datos-del-envio-segun-el-estado-en-el-detalle.spec.ts
 D src/test/e2e/subsystem/sms/t-021-el-supervisor-de-dos-centros-ve-los-sms-de-ambos-y-filtra-por-centro.desc.md
 D src/test/e2e/subsystem/sms/t-021-el-supervisor-de-dos-centros-ve-los-sms-de-ambos-y-filtra-por-centro.spec.ts
 D src/test/e2e/subsystem/sms/t-022-el-supervisor-no-puede-dar-de-alta-sms.desc.md
 D src/test/e2e/subsystem/sms/t-022-el-supervisor-no-puede-dar-de-alta-sms.spec.ts
 D src/test/e2e/subsystem/sms/t-023-reenvio-por-el-administrativo.desc.md
 D src/test/e2e/subsystem/sms/t-023-reenvio-por-el-administrativo.spec.ts
 D src/test/e2e/subsystem/sms/t-024-el-destinatario-ve-un-sms-enviado-desde-otro-centro.desc.md
 D src/test/e2e/subsystem/sms/t-024-el-destinatario-ve-un-sms-enviado-desde-otro-centro.spec.ts
 D src/test/e2e/subsystem/sms/t-025-un-usuario-sin-cargo-de-gestion-no-ve-sms-de-los-centros.desc.md
 D src/test/e2e/subsystem/sms/t-025-un-usuario-sin-cargo-de-gestion-no-ve-sms-de-los-centros.spec.ts
 M src/test/java/com/educaflow/base/infrastructure/axelorhelper/ActionResponseHelperTest.java
A  src/test/java/com/educaflow/base/util/MetaFileUtilTest.java
D  src/test/java/com/educaflow/subsystem/correos/service/impl/AdjuntoServiceImplTest.java
D  src/test/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImplTest.java
AM src/test/java/com/educaflow/subsystem/notificaciones/controller/CorreoControllerTest.java
AM src/test/java/com/educaflow/subsystem/notificaciones/controller/NotificacionControllerTest.java
RM src/test/java/com/educaflow/subsystem/sms/controller/SmsControllerTest.java -> src/test/java/com/educaflow/subsystem/notificaciones/controller/SmsControllerTest.java
A  src/test/java/com/educaflow/subsystem/notificaciones/db/CorreoTest.java
A  src/test/java/com/educaflow/subsystem/notificaciones/db/NotificacionTest.java
A  src/test/java/com/educaflow/subsystem/notificaciones/db/SmsTest.java
A  src/test/java/com/educaflow/subsystem/notificaciones/db/TipoNotificacionTest.java
AM src/test/java/com/educaflow/subsystem/notificaciones/module/NotificacionesModuleTest.java
A  src/test/java/com/educaflow/subsystem/notificaciones/service/impl/AdjuntoServiceImplTest.java
A  src/test/java/com/educaflow/subsystem/notificaciones/service/impl/CorreoServiceImplTest.java
AM src/test/java/com/educaflow/subsystem/notificaciones/service/impl/NotificacionCanalServiceImplTest.java
A  src/test/java/com/educaflow/subsystem/notificaciones/service/impl/NotificacionServiceImplTest.java
A  src/test/java/com/educaflow/subsystem/notificaciones/service/impl/SmsServiceImplTest.java
AM src/test/java/com/educaflow/subsystem/notificaciones/util/GestorNotificacionesUtilTest.java
 M src/test/java/com/educaflow/subsystem/security/service/impl/MenuSecurityServiceImplTest.java
D  src/test/java/com/educaflow/subsystem/sms/module/SmsSenderProviderTest.java
D  src/test/java/com/educaflow/subsystem/sms/service/impl/SmsServiceImplTest.java
 M src/test/java/com/educaflow/tramites/util/verificacion/VerificacionHelperTest.java
 M src/test/java/com/educaflow/views/botones/Categoria7BotonesTest.java
 M src/test/java/com/educaflow/views/support/Index.java
?? .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_64.md
?? .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_65.md
?? .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation/task_66.md
?? .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/implementation_error_2/
?? src/test/java/com/educaflow/base/infrastructure/controller/
```

## Paso 1 — /sdd-debug-with-test-e2e-desc

### Vuelta 1
- **Resultado:** FIN-DESIGN-ERROR
- **Resumen del ejecutor:** Log: test-e2e-desc/error_design.log (apartado como error_design_3.log). Detenido en T-028, corrector ciclo 1: el selector `historialEstado` solo muestra ids (requiere cambio de diseño); además, fecha fija caducada en el test y la presentación exige AutoFirma. Índice: 27 SUCCESS (T-001…T-027) / 49 sin ejecutar / 0 MANUAL.
- **Decisiones:** ninguna

### Vuelta 2 (tras las reentradas 1 y 2)
- **Resultado:** FIN-OK-CON-FALLOS
- **Resumen del ejecutor:** 72 SUCCESS / 4 FAIL / 0 MANUAL; FAIL: T-028, T-029, T-075, T-076 (precondición bloqueada por el truststore de demo; sin ciclos de corrección ni cambios de código). Descomposición rehecha con los [x] T-001..T-027 restaurados. Sin error_design.log nuevo. La app sigue en el 8080 con el código actual.
- **Decisiones:** DEB-003, DEB-004, FIJA-003

## Paso 2 — /sdd-create-tests-e2e

### Vuelta 1
- **Resultado:** FIN-OK
- **Resumen del ejecutor:** 72 SUCCESS / 0 FAIL / 0 MANUAL (T-028, T-029, T-075 y T-076 no se materializaron por estar en [ ]). Destino: src/test/e2e/subsystem/notificaciones/ (72 pares .desc.md + .spec.ts); _support/auth.ts AUTH-OK. Puerta de regresión: 185 passed / 1 skipped / 0 failed, sin REGRESIÓN. App del 8080 parada.
- **Decisiones:** ninguna

## Reentrada 1 — /sdd-designer

### Vuelta 2
- **Resultado:** FIN-OK
- **Resumen del ejecutor:** Modo Revisar/Modificar. Se aplicaron U-001..U-003: el selector `historialEstado` muestra nombre (`target-name="nameState"` + `views/Ref-HistorialEstado.xml`, D8) y `test-e2e-desc.md` se ajustó en fecha y firma (D9); errata D9 aplicada a la spec (ESC-057/070/071/075). Crítica: 2 rondas. Verificar/corregir del diseño: 1 iteración. Tests unitarios: 2 iteraciones. `test-unit-desc.md` regenerado.
- **Decisiones:** FIJA-001

## Reentrada 1 — /sdd-implementer

### Vuelta 2
- **Resultado:** FIN-DESIGN-ERROR
- **Resumen del ejecutor:** Las 67 tareas DONE (sobrescrituras según DEB-001). La iteración 1 del build falla en VAR-1.2: `Ref-HistorialEstado.xml` está en subsystem/notificaciones/views pero HistorialEstado es de expedientes; arreglarlo revierte D8. Log: implementation/error_design.log (apartada como implementation_error_4/).
- **Decisiones:** DEB-001

## Reentrada 2 — /sdd-designer

### Vuelta 3
- **Resultado:** STOP → reanudado tras la decisión del usuario
- **Resumen del ejecutor:** DECISION-REQUERIDA en §15 Revisar/Modificar, paso 3 (CONFLICT con el MUST NOT de expedientes de la plantilla). Sin cambios en design/.
- **Decisiones:** FIJA-002, DEB-002
- **Resultado tras reanudar:** FIN-OK
- **Resumen del ejecutor:** Se aplicó U-001, arreglo (a): `subsysExpedientes.Ref@HistorialEstado-*` en subsystem/expedientes/views/Ref-HistorialEstado.xml, las 4 forms repuntadas, y D8 registra la excepción autorizada por el usuario. Crítica: 2 rondas. Verificar/corregir del diseño: 3 iteraciones, con erratas D9 de la spec aplicadas. Tests unitarios: 1 iteración. `test-unit-desc.md` regenerado.

## Reentrada 2 — /sdd-implementer

### Vuelta 3
- **Resultado:** FIN-OK
- **Resumen del ejecutor:** 63 tareas DONE (5 CONFLICT de vistas resueltos con DEB-001: task_25, 26, 28, 29, 32). Build OK-COMPILA en 2 iteraciones; el corrector borró la copia obsoleta subsystem/notificaciones/views/Ref-HistorialEstado.xml (VAR-1.2). Aviso: tasks.md tiene 56 [x] y 7 [ ] pese a que todas las tareas devolvieron DONE.
- **Decisiones:** DEB-001 (aplicada como decisión previa)

## Decisiones
- **FIJA-001 — Reentrada 1 por DESIGN-ERROR (debug, T-028)**
  - Skill / fase / origen: sdd-debug-with-test-e2e-desc / corrector T-028 / DESIGN-ERROR
  - Pregunta: ¿cómo corregir el diseño tras el error de diseño de T-028?
  - A: Regenerar desde la especificación
  - B: Revisar/Modificar el diseño existente
  - Decisión: B — Revisar/Modificar (enviada al ejecutor como «DECISION: A», porque el skill la ofrece como su opción A)
  - Decidido por: POLÍTICA FIJA
  - Por qué: reentrada 1 por DESIGN-ERROR. `implementation/` apartada como `implementation_error_3/` y el log como `test-e2e-desc/error_design_3.log` (los sufijos 1 y 2 ya existían de ejecuciones anteriores).
  - Transcripción: ninguna
- **DEB-001 — CONFLICT con ficheros de notificaciones creados en una vuelta anterior**
  - Skill / fase / origen: sdd-implementer / Fase 4 — Implementar, task_05.md / CONFLICT
  - Pregunta: Ya existe subsystem/notificaciones/domains/Notificacion.xml (vuelta anterior, en la línea base). ¿Sobrescribir o mantener? Vale para todos los CONFLICT de ficheros que el diseño declara como de esta iniciativa (task_05–36, 46–62).
  - A: Sobrescribir
  - B: Mantener y saltar el fichero
  - Decisión: A
  - Decidido por: CONSENSO (ronda 1)
  - Por qué: B cedió porque mantener las vistas antiguas deja sin aplicar el arreglo D8 del DESIGN-ERROR de T-028 (design.md l. 607/633/669/693/1016) y el contrato del implementer exige copiar los XML verbatim. A no destruye nada ajeno: los ficheros son `A` de esta iniciativa y los arreglos sin stage ya están en el diseño revisado.
  - Transcripción: log_pipeline/DEB-001/
- **FIJA-002 — Reentrada 2 por DESIGN-ERROR (implementer, VAR-1.2)**
  - Skill / fase / origen: sdd-implementer / bucle de build / DESIGN-ERROR
  - Pregunta: ¿cómo corregir el diseño tras el error VAR-1.2 de Ref-HistorialEstado.xml?
  - A: Regenerar desde la especificación
  - B: Revisar/Modificar el diseño existente
  - Decisión: B
  - Decidido por: POLÍTICA FIJA
  - Por qué: reentrada 2 por DESIGN-ERROR; `implementation/` apartada como `implementation_error_4/`
  - Transcripción: ninguna
- **DEB-002 — Dónde poner las vistas de selección de HistorialEstado (DESIGN-ERROR VAR-1.2)**
  - Skill / fase / origen: sdd-designer / §15 Revisar/Modificar, paso 3 (U-001) / CONFLICT
  - Pregunta: E-001 ofrece dos arreglos y el (a) choca con template-system/README.md §4.2/§2.9 («MUST NOT proponer crear ni modificar nada dentro de subsystem/expedientes»). ¿Qué arreglo aplica el corrector como U-001?
  - A: crear subsystem/expedientes/views/Ref-HistorialEstado.xml (subsysExpedientes.Ref@HistorialEstado-*), revisar D8 y repuntar las cuatro forms
  - B: llevar las vistas como bloque de detalle Main@Notificacion.HistorialEstado-* en Main-Notificacion.xml, sin tocar expedientes
  - Decisión: A (tras ESCALAR)
  - Decidido por: USUARIO (2026-10-07), tras escalado del orquestador; antes, CONSENSO en A en la ronda 2
  - Por qué: B incumple VAR-6.3 (los `<field>` con grid-view/form-view deben apuntar a `Ref@…`), así que rompería el build. A es la única vía que cumple VAR-1.2 y VAR-6.3, pero contradice el MUST NOT literal de template-system/README.md (l. 149, 202) y revierte D8: el defensor que cedió la marcó expresamente como fuera del alcance declarado de la iniciativa.
  - Transcripción: log_pipeline/DEB-002/

**Reanudación:** el usuario acepta A («acepto la opción A, crea la vista en expedientes»). Se reanuda el mismo ejecutor del designer (reentrada 2).
- **DEB-003 — Cómo tratar test-e2e-desc/ existente al relanzar debug**
  - Skill / fase / origen: sdd-debug-with-test-e2e-desc / Fase 2 — Descomponer / DUDA
  - Pregunta: el descomponedor reescribe el índice entero en [ ] y borraría los 27 [x] (T-001..T-027). ¿Cómo se trata la carpeta existente?
  - A: Relanzar el descomponedor sobre toda la entrada y volver a marcar [x] T-001..T-027; quedan pendientes T-028..T-076
  - B: Relanzar el descomponedor y aceptar el índice entero en [ ] (se vuelven a ejecutar los 76)
  - Decisión: A
  - Decidido por: CONSENSO (ronda 3)
  - Por qué: §7.7 paso 2 del orquestador conserva test-e2e-desc/ porque «sus [x] son progreso reanudable», aun contando con que el implementer reescriba código; reiniciarlos equivale a un --fresh no pedido (debug §2.6 y Apéndice A). El riesgo de D8 sobre T-018 lo cubre la puerta de regresión de create-tests.
  - Transcripción: log_pipeline/DEB-003/
- **DEB-004 — Falta el truststore de demo para la firma en servidor**
  - Skill / fase / origen: sdd-debug-with-test-e2e-desc / Fase 4 — T-028, ciclo 1 / BLOCKED
  - Pregunta: la config privada apunta a firma/demo/almacen/truststore-demo.jks y crls-demo.xml, que solo existen en develop (e8cb3ab); la firma en servidor falla al presentar y la precondición de T-028/029/075/076 no se alcanza. ¿Cómo se continúa?
  - A: Traer a esta rama firma/demo/** de e8cb3ab, rearrancar y reejecutar T-028
  - B: Dejar T-028, T-029, T-075 y T-076 en FAIL y seguir con el resto
  - Decisión: B
  - Decidido por: JUEZ
  - Por qué: Nivel 2: la spec (L751) deja los certificados de demo para otra iniciativa (e8cb3ab, solo en develop). Con A, si se versionan, se importa esa iniciativa sin ConfiguracionEntornosTest; si no se versionan, quedan regresiones que fallan en limpio (FIN-STOP-REGRESION).
  - Transcripción: log_pipeline/DEB-004/
- **FIJA-003 — T-029 bloqueado por permisos del modo auto**
  - Skill / fase / origen: sdd-debug-with-test-e2e-desc / Fase 4 — T-029, ejecutor / FALLO-MECANICO
  - Pregunta: el clasificador de permisos del modo auto («PII Data Handling») bloqueó al ejecutor de T-029 leer snapshots y consultas REST. ¿Cómo se sigue?
  - A: Relanzar el ejecutor de T-029
  - B: Dejar T-029 en FAIL y seguir
  - Decisión: B
  - Decidido por: POLÍTICA FIJA (aplicación de DEB-004)
  - Por qué: T-029 comparte la precondición bloqueada por el truststore que DEB-004 resolvió dejando en FAIL; un bloqueo de permisos no se esquiva, y si se repite en otro test se devuelve al usuario.
  - Transcripción: ninguna

**Fin:** COMPLETADO CON FALLOS EN DEBUG — T-028, T-029, T-075, T-076
