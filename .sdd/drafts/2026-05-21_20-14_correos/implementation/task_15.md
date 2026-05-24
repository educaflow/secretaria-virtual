---
type: implementation-task
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Los `<menuitem>` de esta tarea YA están materializados por el diseñador en `.sdd/drafts/2026-05-21_20-14_correos/design/menus.xml`. **Fusiónalos literalmente** (sin regenerarlos) en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`, insertando los `<menuitem>` justo antes de `</object-views>`. Si ya existe un `<menuitem name="...">` con el mismo `name`, DETENTE y pregunta (sobrescribir / mantener / abortar). Tras fusionar, valida con `xmllint` contra `object-views.xsd`.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | **Modificar** | k-vistas (menus.md) | Bloque de menús de Correos (ya presente; ajustar Gráfica a 3 subentradas). |

Descripción de diseño (Paso 6 — Menús):

`design/menus.xml` — porción a fusionar (ya presente en el proyecto; se ajusta la Gráfica a 3 subentradas):

- `correos-menuitem` "Correos" (order 40)
  - `correos-todos-menuitem` "Todos los correos" → `@Todos-action` `groups="admins"` (E-UN-002/008 control de acceso por rol)
  - `correos-miCentro-menuitem` "Correos de mi centro" → `@MiCentro-action`
  - `correos-mios-menuitem` "Mis correos" → `@Mios-action`
  - `correos-grafica-menuitem` "Gráfica de correos" `groups="admins"` (sin acción, sub-parent)
    - `correos-graficaDia-menuitem` "Diaria" → `@GraficaDia-action`
    - `correos-graficaSemana-menuitem` "Semanal" → `@GraficaSemana-action`
    - `correos-graficaMes-menuitem` "Mensual" → `@GraficaMes-action`

Nota de unificación 3: **`U-grafica-001` → 3 entradas de menú.** Una `<chart>` admite como máximo 2 `search-fields`; las dos fechas los ocupan, así que el selector día/semana/mes se materializa como 3 subentradas del menú "Gráfica de correos".

Seguridad (Paso 8): "solo Administrador crea/ve la gráfica" → menú `groups="admins"`.
