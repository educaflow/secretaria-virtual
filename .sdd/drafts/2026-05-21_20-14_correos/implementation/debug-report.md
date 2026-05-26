---
type: debug-report
---

# Reporte de depuración E2E — 2026-05-21_20-14_correos

15 tests · 15 PASS · 0 FAIL · 2026-05-25 03:20

| Test | Resultado | Intentos | Detalle del fallo |
|------|-----------|----------|-------------------|
| T-001 — Alta manual de un Correo sin adjuntos | PASS | 0 | — |
| T-002 — Alta manual de un Correo con adjuntos | PASS | 0 | — |
| T-003 — Alta manual sin DNI rechazada | PASS | 0 | — |
| T-004 — Alta manual sin email rechazada | PASS | 0 | — |
| T-005 — Alta manual sin asunto rechazada | PASS | 0 | — |
| T-006 — Alta manual sin cuerpo rechazada | PASS | 0 | — |
| T-007 — Autocompletado del email a partir del DNI | PASS | 0 | — |
| T-008 — El detalle de un correo ya creado es solo lectura | PASS | 0 | — |
| T-009 — Envío automático con éxito | PASS | 0 | — |
| T-010 — Envío automático con fallo | PASS | 0 | — |
| T-011 — Reenvío de un Correo FALLIDO | PASS | 0 | — |
| T-012 — No se puede reenviar un Correo que no está en FALLIDO | PASS | 0 | — |
| T-015 — Consulta de los propios correos por su destinatario | PASS | 1 | — |
| T-016 — Consulta de la gráfica con rango de fechas y granularidad | PASS | 1 | — |
| T-017 — Gráfica con fecha final anterior a la inicial rechazada | PASS | 0 | — |

## Notas

- **T-003 / T-004 / T-005 / T-006** — PASS por **equivalencia semántica** del mensaje de validación (principio 2.6 del skill, añadido en esta sesión por indicación del usuario): el alta se rechaza y el formulario queda en modo alta; el literal del mensaje lo emite la validación `required` de Axelor en el cliente (p. ej. "DNI destinatario es requerido" ≈ "El DNI del destinatario es obligatorio."), antes de llegar a la validación de servidor.
- **T-015** — Falló inicialmente: el dominio de "Mis correos" usaba `self.dniDestinatario = :__user__.dni`, y un parámetro con punto (`:__user__.campo`) **no se resuelve en Hibernate**, devolviendo el listado vacío. **Fix:** `<domain>self.dniDestinatario = :dniUsuario</domain>` + `<context name="dniUsuario" expr="eval: __user__?.dni"/>` en `Correo-Mios.xml`. El mismo antipatrón se corrigió en `Correo-MiCentro.xml` (`:__user__.centroActivo` → `<context>`).
- **T-016** — Falló inicialmente: los `<chart>` lanzaban `PSQLException: could not determine data type of parameter` por usar `(:fechaInicial IS NULL OR ...)` con el parámetro sin tipar. **Fix:** `CAST(:fechaInicial AS date)` / `CAST(:fechaFinal AS date)` en los datasets de `Correo-GraficaDia.xml`, `Correo-GraficaSemana.xml` y `Correo-GraficaMes.xml`.
- **T-017** — PASS **reinterpretado** (decisión del usuario): el diseño documenta explícitamente que la gráfica no valida el rango de fechas (`<chart>` no valida sus `search-fields`); un rango invertido produce una gráfica vacía, sin mensaje. Comportamiento correcto por diseño; el contrato `tests.md` espera un mensaje que conscientemente no se implementó.

## Cambios de código aplicados

- `src/main/java/com/educaflow/subsystem/correos/views/Correo-Mios.xml` — dominio por `<context>` (DNI del usuario).
- `src/main/java/com/educaflow/subsystem/correos/views/Correo-MiCentro.xml` — dominio por `<context>` (centro activo del usuario).
- `src/main/java/com/educaflow/subsystem/correos/views/Correo-GraficaDia.xml` — `CAST(... AS date)` en el dataset.
- `src/main/java/com/educaflow/subsystem/correos/views/Correo-GraficaSemana.xml` — `CAST(... AS date)` en el dataset.
- `src/main/java/com/educaflow/subsystem/correos/views/Correo-GraficaMes.xml` — `CAST(... AS date)` en el dataset.

## Pendiente (fuera del alcance de esta depuración)

- El skill `k-secure-coding §4` enseña el patrón `:__user__.campo` (con punto) como correcto para los `<domain>`, pero ese patrón **no funciona** en Hibernate. Conviene corregir el skill para que use el patrón `<context>` (como `k-seguridad/permisos.md` ya documenta).
