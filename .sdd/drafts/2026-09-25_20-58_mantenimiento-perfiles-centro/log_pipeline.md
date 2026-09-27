# Log del pipeline sdd

**Iniciativa:** .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro
**Inicio:** 2026-09-25 21:10
**Pasos pedidos:** designer implementer debug tests
**Argumentos:** (vacío) — pasos seleccionados en el TUI: designer implementer debug tests

## Línea base (git status --porcelain)
```
 M .claude/skills/sdd-create-tests-e2e/template-expediente/README.md
 M .claude/skills/sdd-create-tests-e2e/template-expediente/generation.md
 M .claude/skills/sdd-create-tests-e2e/template-expediente/healing.md
 M .claude/skills/sdd-create-tests-e2e/template-expediente/verification.md
 M .claude/skills/sdd-debug-with-test-e2e-desc/template-expediente/README.md
 M .claude/skills/sdd-debug-with-test-e2e-desc/template-expediente/execution.md
A  plan.md
A  plan2.md
 M src/main/java/com/educaflow/base/infrastructure/metafile/MetaFileHelper.java
 M src/main/java/com/educaflow/base/infrastructure/pdf/DocumentoPdfFactory.java
A  src/main/java/com/educaflow/base/infrastructure/pdf/impl/helper/ImagenPdfHelper.java
 M src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
A  src/main/java/com/educaflow/secretariavirtual/menus/service/MenuVisibilidadService.java
A  src/main/java/com/educaflow/secretariavirtual/menus/service/impl/MenuVisibilidadServiceImpl.java
 M src/main/java/com/educaflow/secretariavirtual/module/SecretariaVirtualModule.java
 M src/main/java/com/educaflow/subsystem/correos/data-init/input/auth-correos.xml
 M src/main/java/com/educaflow/subsystem/expedientes/domains/Expediente.xml
 M src/main/java/com/educaflow/subsystem/expedientes/domains/i18n_ca.csv
 M src/main/java/com/educaflow/subsystem/expedientes/domains/i18n_es.csv
D  src/main/java/com/educaflow/subsystem/firmas/views/Todos-TareaFirma.xml
 M src/main/java/com/educaflow/subsystem/registroentradasalida/views/Main-RegistroEntrada.xml
 M src/main/java/com/educaflow/subsystem/registroentradasalida/views/Main-RegistroSalida.xml
 M src/main/java/com/educaflow/subsystem/security/data-init/input/AceProfileGlobal.xml
 M src/main/java/com/educaflow/subsystem/tramitador/tramitacion/util/ExpedienteUtil.java
D  src/main/java/com/educaflow/system/gestioncentro/views/gestion-centro-cargos.xml
 M src/main/java/com/educaflow/system/gestioncentro/views/gestion-centro-main.xml
 M src/main/java/com/educaflow/system/gestioncentro/views/gestion-centro-usuario.xml
A  src/main/java/com/educaflow/system/ventanilla/controller/BandejaController.java
D  src/main/java/com/educaflow/system/ventanilla/controller/BandejaPorPerfilController.java
A  src/main/java/com/educaflow/system/ventanilla/views/abrirexpediente/Abrir-Expediente.xml
D  src/main/java/com/educaflow/system/ventanilla/views/listarexpedientes/Cerrados-Expediente.xml
D  src/main/java/com/educaflow/system/ventanilla/views/listarexpedientes/Esperando-Expediente.xml
D  src/main/java/com/educaflow/system/ventanilla/views/listarexpedientes/Revision-Expediente.xml
A  src/main/java/com/educaflow/system/ventanilla/views/mistramites/MisEnTramitacion-Expediente.xml
R  src/main/java/com/educaflow/system/ventanilla/views/listarexpedientes/Pendiente-Expediente.xml -> src/main/java/com/educaflow/system/ventanilla/views/mistramites/MisFinalizados-Expediente.xml
A  src/main/java/com/educaflow/system/ventanilla/views/mistramites/MisPendientes-Expediente.xml
 M src/main/java/com/educaflow/system/ventanilla/views/nuevoexpediente/CLAUDE.md
A  src/main/java/com/educaflow/system/ventanilla/views/tramitacion/JefaturaAbiertos-Expediente.xml
A  src/main/java/com/educaflow/system/ventanilla/views/tramitacion/JefaturaCerrados-Expediente.xml
A  src/main/java/com/educaflow/system/ventanilla/views/tramitacion/PendientesDeMi-Expediente.xml
A  src/main/java/com/educaflow/system/ventanilla/views/tramitacion/SecretariaAbiertos-Expediente.xml
A  src/main/java/com/educaflow/system/ventanilla/views/tramitacion/SecretariaCerrados-Expediente.xml
R  src/main/java/com/educaflow/system/ventanilla/views/listarexpedientes/Firma-Expediente.xml -> src/main/java/com/educaflow/system/ventanilla/views/tramitacion/Todos-Expediente.xml
A  src/main/java/com/educaflow/system/ventanilla/views/tramitacion/TodosAbiertos-Expediente.xml
A  src/main/java/com/educaflow/system/ventanilla/views/tramitacion/TodosCerrados-Expediente.xml
 M src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/TipoExpedienteInstance.xml
 M src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/views.xml
 M src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/PhaseEventManagerImpl.java
 M src/main/java/com/educaflow/tramites/shared/template-views.xml
 M src/main/resources/axelor-config.properties
 M src/test/e2e/subsystem/correos/t-001-alta-de-un-correo-que-se-envia-con-exito.spec.ts
 M src/test/e2e/subsystem/correos/t-002-alta-de-un-correo-con-adjunto.spec.ts
 M src/test/e2e/subsystem/correos/t-003-alta-sin-el-dni-del-destinatario.spec.ts
 M src/test/e2e/subsystem/correos/t-004-alta-sin-destinatario-en-el-para.spec.ts
 M src/test/e2e/subsystem/correos/t-005-alta-sin-asunto.spec.ts
 M src/test/e2e/subsystem/correos/t-006-alta-sin-cuerpo.spec.ts
 M src/test/e2e/subsystem/correos/t-007-alta-sin-centro.spec.ts
 M src/test/e2e/subsystem/correos/t-008-alta-sin-el-nombre.spec.ts
 M src/test/e2e/subsystem/correos/t-009-alta-sin-los-apellidos.spec.ts
 M src/test/e2e/subsystem/correos/t-010-alta-con-adjunto-sin-nombre-de-fichero.spec.ts
 M src/test/e2e/subsystem/correos/t-011-alta-con-adjunto-sin-contenido.spec.ts
 M src/test/e2e/subsystem/correos/t-012-alta-con-para-de-formato-invalido.spec.ts
 M src/test/e2e/subsystem/correos/t-013-alta-con-el-dni-del-destinatario-invalido.spec.ts
 M src/test/e2e/subsystem/correos/t-014-alta-con-dos-adjuntos-con-el-mismo-nombre-de-fichero.spec.ts
 M src/test/e2e/subsystem/correos/t-015-alta-con-el-asunto-demasiado-largo.spec.ts
 M src/test/e2e/subsystem/correos/t-016-alta-con-en-copia-de-formato-invalido.spec.ts
 M src/test/e2e/subsystem/correos/t-017-alta-con-en-copia-oculta-de-formato-invalido.spec.ts
 M src/test/e2e/subsystem/correos/t-019-el-boton-reenviar-no-aparece-si-el-correo-no-ha-fallado.spec.ts
 M src/test/e2e/subsystem/correos/t-020-el-supervisor-solo-ve-los-correos-de-su-centro.spec.ts
 M src/test/e2e/subsystem/correos/t-022-el-supervisor-descarga-el-adjunto-de-un-correo-de-su-centro.spec.ts
 M src/test/e2e/subsystem/correos/t-023-el-destinatario-consulta-un-correo-enviado-con-exito-y-descarga-su-adjunto.spec.ts
 M src/test/e2e/subsystem/correos/t-025-el-destinatario-no-ve-un-correo-enviado-con-exito-a-otra-persona.spec.ts
 M src/test/e2e/system/ventanilla/t-001-alumno-de-un-solo-centro-crea-un-expediente-sin-que-se-le-pregunte-nada.spec.ts
 M src/test/e2e/system/ventanilla/t-002-el-profesor-solo-ve-los-tramites-para-el-profesor.spec.ts
 M src/test/e2e/system/ventanilla/t-003-usuario-que-no-puede-crear-expedientes-en-ningun-centro.spec.ts
 M src/test/e2e/system/ventanilla/t-004-el-administrador-se-comporta-como-un-usuario-mas.spec.ts
 M src/test/e2e/system/ventanilla/t-005-alumno-de-dos-centros-elige-el-centro.spec.ts
 M src/test/e2e/system/ventanilla/t-006-cancelar-en-la-eleccion-de-centro.spec.ts
 M src/test/e2e/system/ventanilla/t-007-atras-desde-el-contexto-del-tramite-y-desde-el-listado-de-tramites.spec.ts
 M src/test/e2e/system/ventanilla/t-008-cancelar-en-el-listado-de-tramites-cuando-no-hubo-eleccion-de-centro.spec.ts
 M src/test/e2e/system/ventanilla/t-010-usuario-con-las-dos-formas-de-presentar-debe-elegir.spec.ts
 M src/test/e2e/system/ventanilla/t-012-familiar-que-no-es-alumno-en-representacion-sin-preguntar.spec.ts
 M src/test/e2e/system/ventanilla/t-013-usuario-que-es-alumno-y-familiar-elige-para-quien-es.spec.ts
 M src/test/e2e/system/ventanilla/t-014-peticion-manipulada-con-un-centro-ajeno.spec.ts
 M src/test/e2e/system/ventanilla/t-015-peticion-manipulada-con-una-forma-de-presentar-que-no-tiene.spec.ts
 M src/test/e2e/system/ventanilla/t-016-peticion-manipulada-para-crear-el-expediente-para-uno-mismo-sin-poder.spec.ts
 M src/test/e2e/system/ventanilla/t-017-peticion-manipulada-para-crear-el-expediente-en-representacion-sin-poder.spec.ts
 M src/test/e2e/system/ventanilla/t-018-profesor-crea-un-expediente-de-un-tramite-que-no-admite-representacion-sin-que-se-le-pregunte-nada.spec.ts
 M src/test/e2e/system/ventanilla/t-020-volver-al-listado-de-centros-elegir-otro-centro-y-crear-el-expediente-en-el.spec.ts
 M src/test/e2e/system/ventanilla/t-021-atras-desde-el-contexto-del-tramite-cuando-no-hubo-eleccion-de-centro.spec.ts
 M src/test/e2e/system/ventanilla/t-023-usuario-que-es-alumno-y-familiar-crea-el-expediente-en-representacion.spec.ts
 M src/test/e2e/system/ventanilla/t-024-peticion-manipulada-con-un-tramite-que-el-usuario-no-puede-iniciar-en-su-centro.spec.ts
 M src/test/e2e/system/ventanilla/t-025-peticion-manipulada-en-representacion-en-un-tramite-que-no-la-admite.spec.ts
 M src/test/e2e/system/ventanilla/t-026-peticion-manipulada-sin-centro.spec.ts
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-001-el-alumno-presenta-su-propia-solicitud-telematicamente.desc.md
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-001-el-alumno-presenta-su-propia-solicitud-telematicamente.spec.ts
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-002-el-familiar-presenta-la-solicitud-en-representacion-de-su-hijo.desc.md
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-002-el-familiar-presenta-la-solicitud-en-representacion-de-su-hijo.spec.ts
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-003-el-administrativo-registra-en-papel-la-solicitud-que-entrega-un-alumno.desc.md
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-003-el-administrativo-registra-en-papel-la-solicitud-que-entrega-un-alumno.spec.ts
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-004-el-administrativo-registra-en-papel-la-solicitud-que-entrega-un-padre-por-su-hijo.desc.md
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-004-el-administrativo-registra-en-papel-la-solicitud-que-entrega-un-padre-por-su-hijo.spec.ts
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-005-el-administrativo-que-tambien-es-alumno-presenta-su-propia-solicitud-telematicamente.desc.md
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-005-el-administrativo-que-tambien-es-alumno-presenta-su-propia-solicitud-telematicamente.spec.ts
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-006-el-administrativo-que-tambien-es-alumno-registra-en-papel-la-solicitud-de-otro-alumno.desc.md
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-006-el-administrativo-que-tambien-es-alumno-registra-en-papel-la-solicitud-de-otro-alumno.spec.ts
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-007-el-administrativo-que-tambien-es-alumno-registra-en-papel-la-solicitud-que-entrega-un-padre-por-su-hijo.desc.md
 M src/test/e2e/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/t-007-el-administrativo-que-tambien-es-alumno-registra-en-papel-la-solicitud-que-entrega-un-padre-por-su-hijo.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-001-justifica-un-dia-completo.desc.md
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-001-justifica-un-dia-completo.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-002-justifica-varios-dias-completos.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-003-justifica-unas-horas-de-un-unico-dia.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-004-justifica-varios-dias-en-los-que-el-primero-falto-solo-unas-horas.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-005-no-se-puede-continuar-sin-elegir-el-tipo-de-jornada-faltada.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-006-no-se-puede-continuar-sin-indicar-la-fecha.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-007-no-se-puede-justificar-una-falta-de-hace-mas-de-un-ano.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-008-no-se-puede-justificar-una-falta-con-fecha-futura.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-009-no-se-pueden-justificar-varios-dias-completos-sin-indicar-la-fecha-de-fin.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-010-no-se-pueden-justificar-varios-dias-con-la-fecha-de-fin-igual-a-la-de-inicio.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-011-no-se-pueden-justificar-unas-horas-sin-indicar-la-hora-de-inicio.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-012-no-se-pueden-justificar-unas-horas-sin-indicar-la-hora-de-fin.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-013-no-se-pueden-justificar-unas-horas-con-la-hora-de-fin-anterior-a-la-de-inicio.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-014-al-volver-atras-se-conservan-la-fecha-y-la-hora-ya-introducidas.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-015-la-vista-de-solo-consulta-de-entrada-datos-muestra-el-periodo-sin-poder-tocarlo.desc.md
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-015-la-vista-de-solo-consulta-de-entrada-datos-muestra-el-periodo-sin-poder-tocarlo.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-016-el-profesor-presenta-la-solicitud-y-el-tramitador-ve-el-periodo-en-la-pantalla-de-resolucion.desc.md
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-016-el-profesor-presenta-la-solicitud-y-el-tramitador-ve-el-periodo-en-la-pantalla-de-resolucion.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-017-el-tramitador-acepta-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.desc.md
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-017-el-tramitador-acepta-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-018-el-tramitador-rechaza-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.desc.md
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-018-el-tramitador-rechaza-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.spec.ts
 M src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/t-019-al-cambiar-el-tipo-de-jornada-faltada-se-vacian-los-campos-que-el-nuevo-tipo-ya-no-necesita.spec.ts
 M src/test/java/com/educaflow/base/infrastructure/pdf/DocumentoPdfFactoryTest.java
AD src/test/java/com/educaflow/base/infrastructure/pdf/image_test.png
AD src/test/java/com/educaflow/base/infrastructure/pdf/image_test2.png
AD src/test/java/com/educaflow/base/infrastructure/pdf/imagen_test.jpeg
AD src/test/java/com/educaflow/base/infrastructure/pdf/imagen_test2.jpeg
AD src/test/java/com/educaflow/base/infrastructure/pdf/imagen_test_girada.jpeg
AD src/test/java/com/educaflow/base/infrastructure/pdf/imagen_test_girada2.jpeg
A  src/test/resources/com/educaflow/base/infrastructure/pdf/image_test.png
A  src/test/resources/com/educaflow/base/infrastructure/pdf/image_test2.png
A  src/test/resources/com/educaflow/base/infrastructure/pdf/imagen_test.jpeg
A  src/test/resources/com/educaflow/base/infrastructure/pdf/imagen_test2.jpeg
A  src/test/resources/com/educaflow/base/infrastructure/pdf/imagen_test_girada.jpeg
A  src/test/resources/com/educaflow/base/infrastructure/pdf/imagen_test_girada2.jpeg
?? .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/
```

## Paso 1 — /sdd-designer

### Vuelta 1
- **Resultado:** FIN-OK
- **Resumen del ejecutor:** Modo Generar (template `system`). Ganador design_5 (torneo de 4 comparaciones). Enriquecedor: 5 mejoras. Críticos: 3 rondas, 7 críticas aplicadas. Verificar/corregir diseño: OK-CORRECTO en 1 it.; tests unitarios: OK-CORRECTO en 1 it. test-unit-desc.md generado: sí.
- **Decisiones:** ninguna

## Paso 2 — /sdd-implementer

### Vuelta 1
- **Resultado:** FIN-OK
- **Resumen del ejecutor:** 12 tareas implementadas (12 DONE), índice implementation/tasks.md. Build OK-COMPILA en la iteración 2 (la 1ª falló por un aviso LoopOverCharArray en base/infrastructure/pdfgenerator/impl/texto/Maquetador.java, código previo a la iniciativa, que el corrector arregló). Log implementation/log_build.txt. El motor marcó la casilla de la Tarea 10 en tasks.md. Sin probar: con BD nueva, puede que el grupo `users` no exista cuando se enlazan los permisos (tareas 08 y 09).
- **Decisiones:** ninguna

## Paso 3 — /sdd-debug-with-test-e2e-desc

### Vuelta 1
- **Resultado:** FIN-OK-CON-FALLOS
- **Resumen del ejecutor:** 21 SUCCESS / 1 FAIL / 0 MANUAL — FAIL: T-016, un fallo de entorno: la precondición «tabla AceProfileCentro vacía» no se cumple por las filas que dejan T-001..T-015. Se dejó en FAIL (DEB-001 B) sin tocar la BD ni el código. Índice test-e2e-desc/tests-e2e-desc.md (21 [x], 1 [ ]).
- **Decisiones:** DEB-001
- **Continuación:** se encadena con `tests` (instrucción del usuario, ahora recogida en el skill /sdd §7.3).

## Paso 4 — /sdd-create-tests-e2e

### Vuelta 1
- **Resultado:** FIN-STOP-REGRESION
- **Resumen del ejecutor:** 21 SUCCESS / 0 FAIL / 0 MANUAL en src/test/e2e/subsystem/security/ (T-016 excluido; sin fail_create_tests.log). Puerta de regresión: 120 passed / 12 failed, ninguno de esta iniciativa. En REGRESIÓN: subsystem/correos t-020, t-022, t-023, t-025; subsystem/criptografia t-008, t-009, t-010, t-011; system/ventanilla t-009, t-011, t-019, t-022. No se retiraron ni se tocó código. Salida: scratchpad/gate.txt
- **Decisiones:** ESC-001

## Decisiones
- **Instrucción del usuario (2026-09-26):** si `debug` termina en FIN-OK-CON-FALLOS, se continúa igualmente con `tests`, que persiste solo los tests `[x]`/`[-]`, así que los que fallan se excluyen. Esto sustituye, para esta ejecución, la parada que marca el skill.

- **DEB-001 — T-016 bloqueado por datos residuales en AceProfileCentro**
  - Skill / fase / origen: sdd-debug-with-test-e2e-desc / Fase 4 §9.2 paso 3 (T-016, ciclo 1) / BLOCKED
  - Pregunta: T-016 exige que la tabla AceProfileCentro esté vacía, pero tiene 5 filas de T-001..T-015 (una coincide con la del paso 5 y la unicidad la rechaza), y el sistema de permisos denegó borrarlas. ¿Cómo se continúa?
  - A: Reiniciar la BD limpia (recrear el contenedor educaflow-db y rearrancar ./run.sh), reejecutar T-016 y seguir con T-017..T-022
  - B: Dejar T-016 en FAIL (sin marcar) y continuar con T-017..T-022 sobre la BD actual, sin tocar los datos
  - Decisión: B
  - Decidido por: CONSENSO (ronda 2)
  - Por qué: A cedió porque recrear educaflow-db borra la BD entera, que la iniciativa no declara suya, y el borrado ya estaba denegado: es destructiva y la regla de escalado impide elegirla. Condición: los FAIL por filas residuales se reportan como fallos de entorno, no del código.
  - Transcripción: log_pipeline/DEB-001/
- **ESC-001 — create-tests: borrado de datos de prueba denegado por el sistema de permisos**
  - Skill / fase / origen: sdd-create-tests-e2e / Fase 4 §9.1 (generador de T-001) / BLOCKED
  - Pregunta: la limpieza previa y el teardown de los .spec.ts borran filas de prueba de la BD, y el clasificador de permisos lo deniega como «Irreversible Deletion». ¿Se autoriza el borrado?
  - A: el usuario autoriza el borrado de datos de prueba y se relanza el generador de T-001
  - B: aplicar §2.7, que registra T-001 en fail_create_tests.log, borra su .spec.ts y sigue (probablemente fallen los 21)
  - Decisión: B (no se autoriza el borrado; se registra T-001 en fail_create_tests.log, se borra su .spec.ts y se sigue)
  - Decidido por: USUARIO (tras escalado del orquestador). Se aplica igual a cualquier otro test bloqueado por la misma denegación.
  - Transcripción: ninguna
  - Actualización (usuario): tras entender la causa, el usuario pidió vaciar security_ace_profile_centro (10 filas, todas creadas por los tests E2E de esta iniciativa); se ejecutó `DELETE 10` por petición expresa suya. Se reanuda create-tests reintentando T-001, T-002, T-003 y T-005 y verificando desde T-006.
