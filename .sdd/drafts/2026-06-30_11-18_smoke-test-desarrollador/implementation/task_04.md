---
type: implementation-task
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

El snippet del nuevo menuitem ya está materializado en la carpeta `design/`. **MUST NOT** modificarlo ni regenerarlo: **fusiónalo** (añade el `<menuitem>` de `design/menus.xml`) en el fichero de menús del proyecto `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir ítem «Smoke test» bajo «Desarrollador» (order=1) |

---

## Paso 4 — Menú: añadir «Smoke test» bajo «Desarrollador»

Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

El menuitem `desarrollador-menuitem` (`title="Desarrollador"`, `groups="admins"`, `order="90"`) **ya existe** en el fichero de menús (explorado en el análisis). El submenú «Utilidades de PDF» ya cuelga de él con `groups="admins"` y `order="2"`. Solo hay que fusionar el snippet de `design/menus.xml` (el nuevo `smoketest-menuitem` con `order="1"`):

```xml
<menuitem name="smoketest-menuitem"
          parent="desarrollador-menuitem"
          title="Smoke test"
          action="subsysSmokeTest.SmokeTest@Main-action"
          groups="admins"
          order="1"/>
```

**Verificar:** al arrancar, el menú «Desarrollador» → «Smoke test» es visible para `admin` y no visible para usuarios no-administradores.
