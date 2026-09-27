---
type: implementation-task
template: system
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

# Diseño: Mantenimiento de perfiles de trámites por centro

**Objetivo:** que el supervisor (en los centros que supervisa) y el administrador (en cualquier centro) puedan ver, crear, modificar y borrar las filas de `AceProfileCentro`, con las reglas que garantizan que cada fila dice sin ambigüedad a quién se da el perfil.
**Capa:** subsystem/security
**Especificación de origen:** .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-validaciones, k-code-quality, k-secure-coding, k-vistas, k-datainit

Las decisiones difíciles y sus alternativas están en `decisiones.md` (D1–D6); este documento es coherente con ellas.

## Ficheros a crear o modificar

Rutas relativas a `src/main/java/com/educaflow/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/security/views/Main-AceProfileCentro.xml` | Crear | k-vistas (actions.md, grids.md, forms.md) | Pantalla del administrador («Administración → Perfiles de trámites por centro»). |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los fragmentos de `auth-security.xml`, `input-config.xml` y `CLAUDE.md` del Paso 7 se aplican **añadiendo** a lo que ya hay (no se borra nada).

**Instrucción de materialización (XML ya materializado):** el fichero ya está materializado en `design/views/Main-AceProfileCentro.xml` y se **copia literalmente** a `src/main/java/com/educaflow/subsystem/security/views/Main-AceProfileCentro.xml`, **sin regenerarlo** (ver `implementation.md` §1). La fila es `Acción: Crear`: el destino no debe existir (si existe → `CONFLICT`, `implementation.md` §3).

## Pasos

La pantalla del administrador se describe por diferencias con la del supervisor; se copia la sección completa del Paso 5 como referencia.

### Paso 5 — Vistas

#### `views/Centro-AceProfileCentro.xml` (pantalla del supervisor) — Crear

- `<action-view>` `subsysSecurity.Centro@AceProfileCentro-action` («Perfiles de trámites»): grid + form, `show-toolbar-form=false`, `forceEdit=true`. **Sin `<domain>`**: las filas visibles las filtra el permiso `AceProfileCentro.supervisor`. `<context name="idsCentrosSupervisados">` con `call:` al controlador (U-…-mi-centro-001 y -005, decisiones D1).
- `<grid>` `subsysSecurity.Centro@AceProfileCentro-grid`: columnas centro, trámite, perfil, tipo de usuario, cargo, usuario; `orderBy="centro.name,tramite.name,perfil"`; búsqueda por columnas (`allowSearchFields="true"`); botón «Nuevo» (`canNew="true"`, `newButtonTitle="Nuevo"`); clic en fila abre el form en edición.
- `<form>` `subsysSecurity.Centro@AceProfileCentro-form` con `onNew` → `…-onNew-action`:
  - panel `AceProfileCentro` («Perfil»): `centro` con `readonlyIf="id != null"` (U-002), `domain="self.id IN (:idsCentrosSupervisados)"` (U-001) y vistas `subsysCommon.Ref@Centro-*`; `tramite`; `perfil`.
  - panel `destinatario` («A quién se da»): `tipoUsuario`; `cargo`; `usuario` con `domain` de los usuarios con `CentroUsuario` en el `centro` de la fila (U-003) y `form-view` `subsysCommon.Ref@Usuario-form`; sin `grid-view`, porque `Ref@Usuario-grid` es `groups="admins"` y el supervisor no la resolvería.
  - `buttons-panel` estándar Borrar / Cancelar / Guardar.
- Acciones:
  - `…-btnDelete-action`: `remote-validationDelete-action` → `delete`.
  - `…-btnCancel-action`: `back`.
  - `…-btnSave-action`: `remote-validationSave-action` → `save` → `force-back`.
  - `…-onNew-action`: ejecuta `…-set-centro-unicoSupervisado-action` **solo** con `if="idsCentrosSupervisados.size() == 1"` (U-005; la condición en la rama).
  - `…-set-centro-unicoSupervisado-action` (`<action-record>`): `centro` = el `Centro` cuyo id es el único de `idsCentrosSupervisados`.
- Obligatorios marcados (U-004): heredados del `required="true"` del modelo en `centro`, `tramite` y `perfil`.

ASCII Layout (idéntico en las dos pantallas; sin `showIf` salvo `btnDelete`, que va al principio de su fila sin `colOffset` y no desplaza a los demás):

```
Panel «Perfil» (AceProfileCentro)
cccttttttppp   ← centro(3) + tramite(6) + perfil(3)                 [qué se da y dónde → misma fila]

Panel «A quién se da» (destinatario)
aaabbbuuuuuu   ← tipoUsuario(3) + cargo(3) + usuario(6)             [los tres destinatarios excluyentes → misma fila, primer borde alineado con la fila de arriba]

buttons-panel
Alta:     ........ccgg ← Borrar oculto (colSpan 2, primera posición, sin offset) + offset(6) + Cancelar(2) + Guardar(2)
Edición:  dd......ccgg ← Borrar(2) + offset(6) + Cancelar(2) + Guardar(2)
```

(Reparto proporcional al contenido: el trámite y el usuario muestran nombres largos —«Justificación de falta del profesorado», «Profesor1 CIPFP Mislata»— y llevan 6 columnas; el perfil es un enum de una palabra, el centro un nombre corto y el tipo de usuario y el cargo códigos cortos, 3 columnas cada uno.)

#### `views/Main-AceProfileCentro.xml` (pantalla del administrador) — Crear

Igual estructura que la del supervisor, con estas diferencias:

- `<action-view>` `subsysSecurity.Main@AceProfileCentro-action` («Perfiles de trámites por centro») sin `<domain>` ni `<context>`: el administrador ve y elige cualquier centro.
- `centro` sin `domain` y **sin** `onNew` en el form (no hay prellenado en esta pantalla).
- `centro` con `readonlyIf="id != null"` (U-perfiles-tramites-todos-centros-001); `usuario` con el mismo `domain` por centro (U-…-todos-centros-002) y, además del `form-view`, `grid-view` `subsysCommon.Ref@Usuario-grid` (el administrador sí la resuelve); obligatorios heredados del modelo (U-…-todos-centros-003).
- Mismo ASCII Layout que la pantalla del supervisor.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh .sdd/drafts/<iniciativa>/design` → `VALIDACION-XML: OK`; `./gradlew test --tests 'com.educaflow.views.*'`.

## Trazabilidad Origen spec → V/R/U → ubicación (reglas U de esta pantalla)

### Reglas de UI (U)

| U-perfiles-tramites-todos-centros-001 | RUI-perfiles-tramites-todos-centros-formulario-001 | `views/Main-AceProfileCentro.xml`: `readonlyIf="id != null"` del `<field name="centro">` | Centro de solo lectura en una fila existente. |
| U-perfiles-tramites-todos-centros-002 | RUI-perfiles-tramites-todos-centros-formulario-002 | `views/Main-AceProfileCentro.xml`: `domain` del `<field name="usuario">` | El selector de usuario solo ofrece usuarios del centro de la fila. |
| U-perfiles-tramites-todos-centros-003 | RUI-perfiles-tramites-todos-centros-formulario-003 | `views/Main-AceProfileCentro.xml`: campos `centro`, `tramite`, `perfil` (obligatoriedad heredada del modelo) | Centro, trámite y perfil marcados como obligatorios. |

## Notas y supuestos (aplicables)

- **Nombres de las variantes de vista.** Se sigue a `subsystem/correos`, el hermano con la misma pareja de pantallas: `Centro-…` para la del supervisor («de mi centro») y `Main-…` para la del administrador («todos»).
- **Cambio de centro con usuario ya elegido (ESC-015).** No se vacía el usuario al cambiar el centro (la spec no lo pide): el error lo da V-006 al guardar.
- **Selectores sin vista de referencia.** `Tramite`, `TipoUsuario` y `Cargo` no tienen `Ref@…` en el proyecto; se usan sus selectores por defecto en vez de crear vistas que la spec no pide.
