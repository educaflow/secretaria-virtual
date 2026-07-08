---
type: implementation-task
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menú «Correos» (2 hojas) y menú raíz «Mis correos» |

## Instrucción de materialización — fusión de `menus.xml`, NO regenerar

El fichero `design/menus.xml` de esta iniciativa contiene la **porción** de `<menuitem>` a fusionar (no un `menus.xml` completo). **MUST**: leer `design/menus.xml`, extraer sus `<menuitem>` e **insértalos** literalmente en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` justo antes de `</object-views>`, sin reescribirlos ni reformatearlos. Si ya existe un `<menuitem name="...">` con el mismo `name`, responde `CONFLICT`. Tras fusionar, **MUST** validar con `xmllint` (ver `implementation.md` §3) contra `object-views.xsd`.

## Texto del diseño (verbatim, `design.md`, Paso 9 — Menús)

Fichero completo en `design/menus.xml` (porción a fusionar en el `menus.xml` único del proyecto, ver tabla de ficheros). Añade:
- Menú raíz contenedor `misCorreos-menuitem` ("Mis correos", sin `action` ni `parent` — solo `title`/`order`, como todos los demás raíces del `menus.xml` real del proyecto) con un único hijo hoja `misCorreos-verMisCorreos-menuitem` (`parent="misCorreos-menuitem"`, `action="subsysCorreos.Correo@Mis-action"`) — visible para cualquier usuario autenticado (sin `groups`, igual que `registro-menuitem`/`firmarDocumentos-menuitem` del `menus.xml` real).
- Menú raíz `correos-menuitem` ("Correos", `groups="admins,users"`) con dos hijos: `correos-administracion-menuitem` (`groups="admins"`, acción `@Main`) y `correos-centro-menuitem` (`groups="users"`, acción `@Centro`).

**Verificar:** `grep -c "correos" design/menus.xml` ≥ 3; ningún `menuitem` sin `parent` lleva `action` (`grep -n 'menuitem name=".*-menuitem" title=.*action=' design/menus.xml | grep -v 'parent='` no debe encontrar coincidencias — así se excluyen las hojas legítimas que sí llevan `action` junto con `parent`, y solo quedan los raíces); los `order` (15 y 45) no colisionan con los ya existentes en el `menus.xml` real del proyecto (20, 30, 50, 60, 70, 90 a fecha de este diseño).

### Notas y supuestos aplicables (verbatim, `design.md`)

5. **"Mis correos" se anida como hoja bajo un menú raíz contenedor (corregido — ya NO es un raíz con `action` propio).** `k-vistas/menus.md` establece que un menuitem raíz **no** lleva `action` ni `parent` (solo `title`/`order`/`groups`); el `menus.xml` real del proyecto lo confirma sin excepción: sus 6 raíces activos (`expedientes-menuitem`, `sistemaEducativo-menuitem`, `administracionSv-menuitem`, `registro-menuitem`, `firmarDocumentos-menuitem`, `desarrollador-menuitem`) son todos contenedores puros, y todo `menuitem` con `action` lleva `parent`. Por eso `misCorreos-menuitem` es un raíz contenedor (sin `action`) con un único hijo hoja `misCorreos-verMisCorreos-menuitem` (`parent="misCorreos-menuitem"`, con la `action`), igual que hace `registro-menuitem`/`firmarDocumentos-menuitem` con sus propios hijos — sin `groups` en ninguno de los dos niveles, visible para cualquier usuario autenticado.
6. **El menú «Correos de mi centro» usa `groups="users"` (no restringe por `TipoUsuario`).** El modelo de grupos nativo de Axelor (`admins`/`users`) no distingue el `TipoUsuario` `SUPERVISOR` de otros tipos de usuario del mismo centro (es una limitación compartida por el resto del proyecto, p.ej. `subsysCommon.Centro.CentroUsuario` usa `groups="admins,users"` de forma similar). La visibilidad exacta del menú es solo UX; la **defensa real** que sí distingue Supervisor de cualquier otro usuario del centro está en la Tarea 16 (permiso `Correo.propio-centro-supervisor` con subconsulta sobre `TipoUsuario.codigo = 'SUPERVISOR'`), que protege también la Vía B.

**MUST NOT** añadir ningún otro `<menuitem>` distinto de los descritos aquí.
