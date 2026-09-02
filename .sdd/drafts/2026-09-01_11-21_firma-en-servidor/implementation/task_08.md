---
type: implementation-task
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | **Sin cambios**: el menú «Firmar documentos → Pendientes» ya existe y no se toca (ver `menus.xml` del diseño) |

## Cómo se materializa

El fichero del diseño es `design/menus.xml` y se **fusiona** (no se copia encima) en el `menus.xml` único del
proyecto, `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. **MUST NOT** regenerarlo ni
reescribirlo.

En esta iniciativa la fusión es **vacía**: `design/menus.xml` reproduce verbatim la rama «Firmar documentos»
que el fichero real **ya tiene** con esos mismos `name`, así que **MUST NOT** duplicarse ningún `<menuitem>`.
El resultado esperado es que `git diff --stat` del `menus.xml` del proyecto **no muestre ningún cambio**.

## Texto del diseño (verbatim)

### Paso 8 — Menús

**Fichero:** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (Modificar — **sin cambios**)
**XML del diseño:** `design/menus.xml`

`screen-documentos-pendientes-de-firma.md` §Menú lo dice explícitamente: «Firmar documentos → Pendientes […]
No cambia». El `design/menus.xml` reproduce verbatim la rama «Firmar documentos» del fichero único del proyecto
para dejar constancia del estado esperado y para que el fichero valide contra el XSD; **MUST NOT** duplicar esos
`<menuitem>` al fusionar: ya existen con esos mismos `name`.

**Verificación:** `git diff --stat src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` no debe
mostrar ningún cambio.
