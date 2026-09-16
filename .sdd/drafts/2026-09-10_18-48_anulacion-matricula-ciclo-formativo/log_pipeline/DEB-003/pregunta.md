# DEB-003 — Falta infraestructura del entorno (certificados custodiados y ciclos formativos) para los tests de firma

**Skill / fase / origen:** sdd-debug-with-test-e2e-desc / Fase 4 — bucle de corrección de T-005 (§9.2, ciclo 1 de 10), token `BLOQUEADO` del corrector / BLOCKED
**Pregunta:** El corrector devuelve `BLOQUEADO: T-005` porque falta un recurso del entorno —la tabla `criptografia_certificado_digital` está vacía (0 filas, verificado por SQL), ningún alumno tiene certificado custodiado, `getSituacionFirmaByDni` devuelve `SIN_CERTIFICADO` y la vista pinta correctamente la rama AutoFirma, que no es automatizable—, y además faltan los 8 ciclos formativos que el reparto de parejas del nuevo «Aislamiento entre tests» exige (solo hay los 8 de demostración, deberían ser 16); ambas cosas las declara el propio diseño como «configuración del entorno, no ficheros del proyecto», así que ¿cómo continúo?
**A:** Provisionar el entorno y reanudar en T-005: dar de alta por la aplicación los certificados custodiados con contraseña guardada de `alumno1@mislata.es` (86862719E), `alumno5@mislata.es` (69807058B), `alumno6@mislata.es` (80364400P) y `alumno1@batoi.es`; el de `alumno2@mislata.es` sin contraseña (lo exige T-055); dejar a `alumno3@mislata.es` sin ninguno; más los 8 ciclos formativos que faltan. Incluye resolver de dónde salen los PKCS#12: **generarlos como certificados de prueba autofirmados** con el DNI de cada alumno de demo —el único del repositorio con clave conocida, `src/main/resources/firma/mi_certificado.p12`, es de «Juan Garcia NIF:1234567Z» y usarlo falsearía la precondición y rompería T-020—.
**B:** Seguir la pasada saltando de momento los tests que dependen de una solicitud presentada (33 de los 50 pendientes por el campo `Desde`/`Evento`, y en la práctica alguno más cuyo `Given` también la exige), ejecutar solo los ~12-17 que se sostienen sin firma (T-012 a T-018, T-047, T-048, T-050, T-052 y los dudosos T-035, T-039, T-043, T-045, T-049, T-051) y reportar el resto como `FAIL` no ejecutado.
**Opciones descartadas al formular:** C — «abortar la pasada y tratar la provisión del entorno como prerrequisito antes de relanzar el skill entero»: abortar no es una alternativa que este debate pueda elegir (protocolo §8.1); si fuera lo correcto, corresponde al juez decirlo con `ESCALAR`.
**Contexto:**
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/specification.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/design/design.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/tests-e2e-desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/t-005-el-alumno-firma-y-presenta-la-solicitud.desc.md
- .sdd/drafts/2026-09-10_18-48_anulacion-matricula-ciclo-formativo/test-e2e-desc/app.log
- src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java
- src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/solicitud/views.xml
- .claude/skills/k-tipo-expediente/SKILL.md (recetas de firma)
- .claude/skills/k-datainit/SKILL.md (datos maestros/semilla y de dónde deben salir)
- .claude/skills/k-secure-coding/SKILL.md (manejo de secretos y material criptográfico)
- agent_docs/deploy.md (arranque, reseteo de BD y acceso con psql)
