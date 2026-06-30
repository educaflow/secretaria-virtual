---
type: implementation-task
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Esta tarea materializa los menús. **La porción de `<menuitem>` ya está materializada** en `design/menus.xml`: **MUST** **fusionarla** en el `menus.xml` único del proyecto `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`, siguiendo las acciones de fusión descritas abajo. **MUST NOT** regenerar el `menus.xml` global ni reescribir la porción del diseño: se fusiona verbatim.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir «Desarrollador» + «Smoke test»; reparentar y restringir «Utilidades de PDF» |

### Paso 7 — Menús (modificar el `menus.xml` único del proyecto)

Fichero materializado: `design/menus.xml` (porción a fusionar) → fusionar en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

Acciones de la fusión:
1. **Añadir** el menú de primer nivel `desarrollador-menuitem` (title "Desarrollador", `order="90"`, `groups="admins"`) y su hijo `desarrollador-smokeTest-menuitem` (title "Smoke test", `action="subsysSmokeTest.SmokeTest@Main-action"`, `groups="admins"`, `order="1"`).
2. **Sustituir** el bloque existente de "Utilidades de PDF" (hoy de primer nivel: `utilidadesPdf-menuitem order="80"` **sin** `groups`, con sus 3 hijos sin `groups`) por la versión del fichero: `utilidadesPdf-menuitem` pasa a `parent="desarrollador-menuitem"`, `order="2"`, `groups="admins"`; y sus 3 hijos (Información, Posiciones Firma, Posición Autofirma__!!) reciben `groups="admins"`. Sus `action` (`subsysPdfUtilities.PdfUtilities@*-action`) **no cambian** (las pantallas de PDF no se tocan, solo ubicación y acceso).

Verificar: con el usuario `admin` aparece el menú "Desarrollador" con "Smoke test" y "Utilidades de PDF" colgando; "Utilidades de PDF" ya no está en primer nivel; un usuario del grupo `users` no ve ninguno de los dos.
