---
type: test-e2e-index
---

# Tests E2E — Subsistema SMS

Índice de los tests E2E de esta iniciativa. Cada test vive en su propio fichero autocontenido. El checkbox se marca `[x]` cuando el test pasa contra la aplicación real (lo gestiona `/sdd-debug-with-test-e2e-desc`).

- [x] [T-001 — Alta de un SMS y resultado del envío](t-001-alta-de-un-sms-y-resultado-del-envio.desc.md)
- [x] [T-002 — Alta con el teléfono escrito ya en formato internacional](t-002-alta-con-el-telefono-escrito-ya-en-formato-internacional.desc.md)
- [x] [T-003 — Alta sin el DNI del destinatario](t-003-alta-sin-el-dni-del-destinatario.desc.md)
- [x] [T-004 — Alta con el DNI con la letra incorrecta](t-004-alta-con-el-dni-con-la-letra-incorrecta.desc.md)
- [x] [T-005 — Alta sin nombre ni apellidos](t-005-alta-sin-nombre-ni-apellidos.desc.md)
- [x] [T-006 — Alta sin teléfono](t-006-alta-sin-telefono.desc.md)
- [x] [T-007 — Alta con un teléfono que no es un móvil de España](t-007-alta-con-un-telefono-que-no-es-un-movil-de-espana.desc.md)
- [x] [T-008 — Alta sin mensaje](t-008-alta-sin-mensaje.desc.md)
- [x] [T-009 — Mensaje sin acentos en el límite de un SMS](t-009-mensaje-sin-acentos-en-el-limite-de-un-sms.desc.md)
- [x] [T-010 — Mensaje con acentos en el límite de un SMS](t-010-mensaje-con-acentos-en-el-limite-de-un-sms.desc.md)
- [x] [T-011 — Alta sin centro](t-011-alta-sin-centro.desc.md)
- [x] [T-012 — El administrador consulta los SMS de varios centros](t-012-el-administrador-consulta-los-sms-de-varios-centros.desc.md)
- [x] [T-013 — Un SMS ya creado no se puede modificar ni borrar](t-013-un-sms-ya-creado-no-se-puede-modificar-ni-borrar.desc.md)
- [x] [T-014 — El supervisor ve solo los SMS de su centro](t-014-el-supervisor-ve-solo-los-sms-de-su-centro.desc.md)
- [x] [T-015 — El administrativo ve los SMS de su centro](t-015-el-administrativo-ve-los-sms-de-su-centro.desc.md)
- [x] [T-016 — Reenvío desde «Del centro» por el supervisor](t-016-reenvio-desde-del-centro-por-el-supervisor.desc.md)
- [x] [T-017 — Reenvío desde «Todos» por el administrador](t-017-reenvio-desde-todos-por-el-administrador.desc.md)
- [x] [T-018 — El destinatario ve sus SMS enviados y no los de otro](t-018-el-destinatario-ve-sus-sms-enviados-y-no-los-de-otro.desc.md)
- [x] [T-019 — Cancelar el alta de un SMS](t-019-cancelar-el-alta-de-un-sms.desc.md)
- [x] [T-020 — Datos del envío según el estado, en el detalle](t-020-datos-del-envio-segun-el-estado-en-el-detalle.desc.md)
- [x] [T-021 — El supervisor de dos centros ve los SMS de ambos y filtra por centro](t-021-el-supervisor-de-dos-centros-ve-los-sms-de-ambos-y-filtra-por-centro.desc.md)
- [x] [T-022 — El supervisor no puede dar de alta SMS](t-022-el-supervisor-no-puede-dar-de-alta-sms.desc.md)
- [x] [T-023 — Reenvío por el administrativo](t-023-reenvio-por-el-administrativo.desc.md)
- [x] [T-024 — El destinatario ve un SMS enviado desde otro centro](t-024-el-destinatario-ve-un-sms-enviado-desde-otro-centro.desc.md)
- [x] [T-025 — Un usuario sin cargo de gestión no ve SMS de los centros](t-025-un-usuario-sin-cargo-de-gestion-no-ve-sms-de-los-centros.desc.md)
