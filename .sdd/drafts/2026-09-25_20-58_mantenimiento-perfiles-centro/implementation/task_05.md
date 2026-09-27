---
type: implementation-task
template: system
---

# Tarea 05 a implementar

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
| `subsystem/security/views/Centro-AceProfileCentro.xml` | Crear | k-vistas (actions.md, grids.md, forms.md) | Pantalla del supervisor («Mi centro → Perfiles de trámites»). |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los fragmentos de `auth-security.xml`, `input-config.xml` y `CLAUDE.md` del Paso 7 se aplican **añadiendo** a lo que ya hay (no se borra nada).

**Instrucción de materialización (XML ya materializado):** el fichero ya está materializado en `design/views/Centro-AceProfileCentro.xml` y se **copia literalmente** a `src/main/java/com/educaflow/subsystem/security/views/Centro-AceProfileCentro.xml`, **sin regenerarlo** (ver `implementation.md` §1). La fila es `Acción: Crear`: el destino no debe existir (si existe → `CONFLICT`, `implementation.md` §3).

## Pasos

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

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh .sdd/drafts/<iniciativa>/design` → `VALIDACION-XML: OK`; `./gradlew test --tests 'com.educaflow.views.*'`.

## Trazabilidad Origen spec → V/R/U → ubicación (reglas U de esta pantalla)

### Reglas de UI (U)

| ID | Origen spec | Ubicación | Qué hace |
|----|-------------|-----------|----------|
| U-perfiles-tramites-mi-centro-001 | RUI-perfiles-tramites-mi-centro-formulario-001 | `views/Centro-AceProfileCentro.xml`: `domain` del `<field name="centro">` + `<context name="idsCentrosSupervisados">` de `subsysSecurity.Centro@AceProfileCentro-action` | El selector de centro solo ofrece los centros que supervisa. |
| U-perfiles-tramites-mi-centro-002 | RUI-perfiles-tramites-mi-centro-formulario-002 | `views/Centro-AceProfileCentro.xml`: `readonlyIf="id != null"` del `<field name="centro">` | Centro de solo lectura en una fila existente. |
| U-perfiles-tramites-mi-centro-003 | RUI-perfiles-tramites-mi-centro-formulario-003 | `views/Centro-AceProfileCentro.xml`: `domain` del `<field name="usuario">` | El selector de usuario solo ofrece usuarios del centro de la fila. |
| U-perfiles-tramites-mi-centro-004 | RUI-perfiles-tramites-mi-centro-formulario-004 | `views/Centro-AceProfileCentro.xml`: campos `centro`, `tramite`, `perfil` (obligatoriedad heredada del `required="true"` de `domains/AceProfileCentro.xml`) | Centro, trámite y perfil marcados como obligatorios. |
| U-perfiles-tramites-mi-centro-005 | RUI-perfiles-tramites-mi-centro-formulario-005 | `views/Centro-AceProfileCentro.xml`: `subsysSecurity.Centro@AceProfileCentro-onNew-action` (rama `if`) → `subsysSecurity.Centro@AceProfileCentro-set-centro-unicoSupervisado-action` | Si supervisa un único centro, el alta nace con ese centro. |

## Notas y supuestos (aplicables)

- **Nombres de las variantes de vista.** Se sigue a `subsystem/correos`, el hermano con la misma pareja de pantallas: `Centro-…` para la del supervisor («de mi centro») y `Main-…` para la del administrador («todos»).
- **El listado del supervisor no lleva `<domain>`.** Lo filtra el permiso `AceProfileCentro.supervisor`; repetir la condición en la vista sería una tercera copia (decisiones D1). El administrador no tiene grupo `users` ni ve el menú del supervisor.
- **El contexto del `<action-view>` llega al `domain` del campo.** axelor-front (`usePrepareContext`) mezcla el contexto del `action-view` en el contexto del formulario, que es el `_domainContext` que usan los selectores; es el mismo mecanismo por el que `sysVentanilla` usa `_centrosIds`.
- **Supervisor sin ningún centro.** Normalmente no ve el menú; si abre la acción por URL, el controlador devuelve el centinela `List.of(-1L)` (el mismo `NINGUNO` de `BandejaController`): el selector de centro sale vacío y V-AceProfileCentro-008 le rechaza cualquier alta. El prellenado (`idsCentrosSupervisados.size() == 1`) no cambia: con el centinela el único id es `-1`, `find(-1)` da `null` y es lo mismo que no prellenar.
- **Cambio de centro con usuario ya elegido (ESC-015).** No se vacía el usuario al cambiar el centro (la spec no lo pide): el error lo da V-006 al guardar.
- **Selectores sin vista de referencia.** `Tramite`, `TipoUsuario` y `Cargo` no tienen `Ref@…` en el proyecto; se usan sus selectores por defecto en vez de crear vistas que la spec no pide.
