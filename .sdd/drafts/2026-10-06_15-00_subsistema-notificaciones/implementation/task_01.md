---
type: implementation-task
template: system
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- (ninguno: la tabla del diseño no asigna skill a estas filas, columna `Skill` = «—»)

## Retirar los subsistemas correos y sms

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/**` | Eliminar | — | Subsistema retirado (todo su árbol) |
| `subsystem/sms/**` | Eliminar | — | Subsistema retirado (todo su árbol) |
| `src/test/java/com/educaflow/subsystem/correos/**`, `src/test/java/com/educaflow/subsystem/sms/**` | Eliminar | — | Sustituidos por los tests de `notificaciones` (`test-unit-desc.md`) |
| `src/test/e2e/subsystem/correos/**`, `src/test/e2e/subsystem/sms/**` | Eliminar | — | Supersedidos (D6, `## Tests E2E supersedidos`) |

**Instrucción de materialización (eliminación):** borrar los árboles completos `src/main/java/com/educaflow/subsystem/correos/`, `src/main/java/com/educaflow/subsystem/sms/`, `src/test/java/com/educaflow/subsystem/correos/`, `src/test/java/com/educaflow/subsystem/sms/`, `src/test/e2e/subsystem/correos/` y `src/test/e2e/subsystem/sms/`. No se crea ni se edita nada más en esta tarea. Desde aquí el proyecto no compila hasta cerrar el bloque de los Pasos 1–9 (lo dice el Paso 1): es lo esperado.

### Paso 1 — Dominios (y retirada de los dominios antiguos)

Copiar `design/domains/{Notificacion,Correo,Sms,Adjunto}.xml` a `subsystem/notificaciones/domains/` y **borrar** en el mismo paso los árboles completos `subsystem/correos/` y `subsystem/sms/` (si conviven, Axelor ve dos entidades `Correo` y dos `Adjunto`; todo lo que contienen lo sustituyen los Pasos 3–13).
En el mismo paso se borran sus tests unitarios (`src/test/java/com/educaflow/subsystem/correos/` y `…/sms/`; los nuevos los describe `test-unit-desc.md`) y sus E2E supersedidos (`src/test/e2e/subsystem/correos/` y `…/sms/`, ver `## Tests E2E supersedidos`).
Desde aquí el proyecto no compila hasta terminar el Paso 9 (las referencias externas se arreglan en los Pasos 7 y 12): los Pasos 1–9 se verifican juntos con `./gradlew -q compileJava` al final del 9.

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `subsystem/correos/**` | Subsistema completo (dominios `Correo`, `Adjunto`, enum `EstadoCorreo`, servicios, controlador, módulo, vistas, data-init) | `specification.md` § Objetivo («sustituye a los dos subsistemas actuales… que desaparecen») |
| `subsystem/sms/**` | Subsistema completo (dominio `Sms`, enum `EstadoSms`, servicio, controlador, módulo, vistas, data-init) | § Objetivo |

## Tests E2E supersedidos

Todos los de las dos carpetas: sus menús («Correos», «SMS»), pantallas y grids desaparecen (§ Objetivo) y cada comportamiento lo cubre un escenario de esta spec (D6).

- `src/test/e2e/subsystem/correos/t-001-alta-de-un-correo-que-se-envia-con-exito.spec.ts` — ESC-001
- `src/test/e2e/subsystem/correos/t-002-alta-de-un-correo-con-adjunto.spec.ts` — ESC-002
- `src/test/e2e/subsystem/correos/t-003-alta-sin-el-dni-del-destinatario.spec.ts` — ESC-007
- `src/test/e2e/subsystem/correos/t-004-alta-sin-destinatario-en-el-para.spec.ts` — ESC-011
- `src/test/e2e/subsystem/correos/t-005-alta-sin-asunto.spec.ts` — ESC-015
- `src/test/e2e/subsystem/correos/t-006-alta-sin-cuerpo.spec.ts` — ESC-017
- `src/test/e2e/subsystem/correos/t-007-alta-sin-centro.spec.ts` — ESC-018
- `src/test/e2e/subsystem/correos/t-008-alta-sin-el-nombre.spec.ts` — ESC-009
- `src/test/e2e/subsystem/correos/t-009-alta-sin-los-apellidos.spec.ts` — ESC-010
- `src/test/e2e/subsystem/correos/t-010-alta-con-adjunto-sin-nombre-de-fichero.spec.ts` — ESC-019
- `src/test/e2e/subsystem/correos/t-011-alta-con-adjunto-sin-contenido.spec.ts` — ESC-020
- `src/test/e2e/subsystem/correos/t-012-alta-con-para-de-formato-invalido.spec.ts` — ESC-012
- `src/test/e2e/subsystem/correos/t-013-alta-con-el-dni-del-destinatario-invalido.spec.ts` — ESC-008
- `src/test/e2e/subsystem/correos/t-014-alta-con-dos-adjuntos-con-el-mismo-nombre-de-fichero.spec.ts` — ESC-021
- `src/test/e2e/subsystem/correos/t-015-alta-con-el-asunto-demasiado-largo.spec.ts` — ESC-016
- `src/test/e2e/subsystem/correos/t-016-alta-con-en-copia-de-formato-invalido.spec.ts` — ESC-013
- `src/test/e2e/subsystem/correos/t-017-alta-con-en-copia-oculta-de-formato-invalido.spec.ts` — ESC-014
- `src/test/e2e/subsystem/correos/t-019-el-boton-reenviar-no-aparece-si-el-correo-no-ha-fallado.spec.ts` — ESC-039
- `src/test/e2e/subsystem/correos/t-020-el-supervisor-solo-ve-los-correos-de-su-centro.spec.ts` — ESC-041
- `src/test/e2e/subsystem/correos/t-022-el-supervisor-descarga-el-adjunto-de-un-correo-de-su-centro.spec.ts` — ESC-049
- `src/test/e2e/subsystem/correos/t-023-el-destinatario-consulta-un-correo-enviado-con-exito-y-descarga-su-adjunto.spec.ts` — ESC-053
- `src/test/e2e/subsystem/correos/t-025-el-destinatario-no-ve-un-correo-enviado-con-exito-a-otra-persona.spec.ts` — ESC-054
- `src/test/e2e/subsystem/correos/t-026-el-supervisor-de-dos-centros-ve-los-correos-de-ambos.spec.ts` — ESC-046
- `src/test/e2e/subsystem/sms/t-001-alta-de-un-sms-y-resultado-del-envio.spec.ts` — ESC-023
- `src/test/e2e/subsystem/sms/t-002-alta-con-el-telefono-escrito-ya-en-formato-internacional.spec.ts` — ESC-024
- `src/test/e2e/subsystem/sms/t-003-alta-sin-el-dni-del-destinatario.spec.ts` — ESC-025
- `src/test/e2e/subsystem/sms/t-004-alta-con-el-dni-con-la-letra-incorrecta.spec.ts` — ESC-026
- `src/test/e2e/subsystem/sms/t-005-alta-sin-nombre-ni-apellidos.spec.ts` — ESC-027
- `src/test/e2e/subsystem/sms/t-006-alta-sin-telefono.spec.ts` — ESC-028
- `src/test/e2e/subsystem/sms/t-007-alta-con-un-telefono-que-no-es-un-movil-de-espana.spec.ts` — ESC-029
- `src/test/e2e/subsystem/sms/t-008-alta-sin-mensaje.spec.ts` — ESC-030
- `src/test/e2e/subsystem/sms/t-009-mensaje-sin-acentos-en-el-limite-de-un-sms.spec.ts` — ESC-031
- `src/test/e2e/subsystem/sms/t-010-mensaje-con-acentos-en-el-limite-de-un-sms.spec.ts` — ESC-032
- `src/test/e2e/subsystem/sms/t-011-alta-sin-centro.spec.ts` — ESC-033
- `src/test/e2e/subsystem/sms/t-012-el-administrador-consulta-los-sms-de-varios-centros.spec.ts` — ESC-034
- `src/test/e2e/subsystem/sms/t-013-un-sms-ya-creado-no-se-puede-modificar-ni-borrar.spec.ts` — ESC-068
- `src/test/e2e/subsystem/sms/t-014-el-supervisor-ve-solo-los-sms-de-su-centro.spec.ts` — ESC-041
- `src/test/e2e/subsystem/sms/t-015-el-administrativo-ve-los-sms-de-su-centro.spec.ts` — ESC-042
- `src/test/e2e/subsystem/sms/t-016-reenvio-desde-del-centro-por-el-supervisor.spec.ts` — ESC-050
- `src/test/e2e/subsystem/sms/t-017-reenvio-desde-todos-por-el-administrador.spec.ts` — ESC-040
- `src/test/e2e/subsystem/sms/t-018-el-destinatario-ve-sus-sms-enviados-y-no-los-de-otro.spec.ts` — ESC-052, ESC-054
- `src/test/e2e/subsystem/sms/t-019-cancelar-el-alta-de-un-sms.spec.ts` — ESC-060
- `src/test/e2e/subsystem/sms/t-020-datos-del-envio-segun-el-estado-en-el-detalle.spec.ts` — ESC-036
- `src/test/e2e/subsystem/sms/t-021-el-supervisor-de-dos-centros-ve-los-sms-de-ambos-y-filtra-por-centro.spec.ts` — ESC-046
- `src/test/e2e/subsystem/sms/t-022-el-supervisor-no-puede-dar-de-alta-sms.spec.ts` — ESC-048
- `src/test/e2e/subsystem/sms/t-023-reenvio-por-el-administrativo.spec.ts` — ESC-074
- `src/test/e2e/subsystem/sms/t-024-el-destinatario-ve-un-sms-enviado-desde-otro-centro.spec.ts` — ESC-055
- `src/test/e2e/subsystem/sms/t-025-un-usuario-sin-cargo-de-gestion-no-ve-sms-de-los-centros.spec.ts` — ESC-047

(Con ellas se borran sus `.desc.md` hermanos.)

## Notas y supuestos (las que aplican a esta tarea)

  - Los E2E de `correos` y `sms` no se mueven: se supersedan (D6).
- **Frozen store de ArchUnit**: al desaparecer `correos.service.CorreoService.enviarCorreo`, la violación congelada de C23 desaparece sola.
