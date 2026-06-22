---
type: implementation-task
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Materializa la pantalla "Grupos (administración)".

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/views/Grupo-Administracion.xml` | Crear | k-vistas | Pantalla "Grupos (administración)" |

El XML completo y ya validado con `xmllint` está en `design/views/Grupo-Administracion.xml`. **MUST** copiarlo **literalmente** a `src/main/java/com/educaflow/system/gruposnotas/views/Grupo-Administracion.xml`, **sin regenerarlo** ni reescribirlo (ver `implementation.md` §1). Es la fuente de verdad del diseño.

### Descripción del diseño (Paso 5 — Vistas)

- **`Grupo-Administracion.xml`** — `action-view sysGruposNotas.Grupo@Administracion-action` (pantalla "Grupos (administración)"). Sin `<domain>` de centro (el administrador ve todos). Añade columna y campos editables `centro` y `cursoAcademico` en el alta (readonlyIf id!=null). Botón adicional "Reabrir grupo" (showIf CERRADO → action-method `reabrir`). Resto análogo al supervisor con sufijo `@Administracion`.

### Reglas de UI (U) materializadas en esta vista

| U | Origen spec | Ubicación |
|---|---|---|
| U-grupos-administracion-001 | RUI-006 | Grupo-Administracion.xml: campos `centro` y `cursoAcademico` editables en alta (`readonlyIf="id != null"`) |
| U-grupos-administracion-002 | RUI-008 | Grupo-Administracion.xml: botón "Reabrir grupo" `showIf="estado == 'CERRADO'"` |
| U-grupos-administracion-003 | RUI-007 | Grupo-Administracion.xml: botón "Cerrar grupo" `showIf="estado == 'ABIERTO' && id != null"` |
| U-grupos-administracion-004 | RUI-009 | Grupo-Administracion.xml: paneles `readonlyIf="estado == 'CERRADO'"` (salvo "Reabrir grupo") |
| U-grupos-administracion-005 | RUI-010 | Grupo-Administracion.xml: campo `valor` de la nota `readonlyIf="moduloGrupo.grupo.estado == 'CERRADO'"` |

**Verificar:** `validate.sh` imprime `VALIDACION-XML: OK`.
