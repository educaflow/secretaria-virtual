---
type: implementation-task
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Fichero de esta tarea (de la tabla "Ficheros a crear o modificar" del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` | Modificar | k-vistas (grids.md, forms.md, actions.md) | Columna «Habilitado» en el grid, casilla «Habilitado» en el form y `onNew` que la marca al crear |

**XML ya materializado:** el fichero está en `design/views/Main-CertificadoDigital.xml` (dentro de la carpeta de la iniciativa `.sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/design/`) y se debe **copiar literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml`, **sin regenerarlo** (ver `implementation.md` §1). La fila es `Acción: Modificar`: el destino **ya existe** y antes de sobrescribir se aplica la **comprobación de conservación** de `implementation.md` §3.

**Menús (Paso 4 del diseño — sin cambios, sin tarea propia):** `design/menus.xml` contiene únicamente el `<menuitem>` `administracionSv-certificadosDigitales-menuitem` **preexistente copiado verbatim del fichero real**; la fusión con `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` es un **no-op**: no duplicar ni modificar nada (el diseño no incluye fila del `menus.xml` del proyecto en la tabla de ficheros porque no hay delta que fusionar).

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; en esta iniciativa su único `<menuitem>` es el **preexistente sin cambios**, así que la fusión es un **no-op**: no duplicar ni modificar nada). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los ficheros `i18n_*.csv` **no se tocan**: los genera un script.

## Texto del diseño (verbatim)

### Paso 3 — Vistas: columna, casilla y valor por defecto al crear

Copiar verbatim `design/views/Main-CertificadoDigital.xml` sobre `src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml`.

Resumen estructural de `views/Main-CertificadoDigital.xml` (un único bloque `Main@CertificadoDigital`, cinco PI `sv-*` en orden):

- **Preexistente (se conserva):** `<action-view>` `subsysCriptografia.Main@CertificadoDigital-action` (grid+form, `show-toolbar-form=false`, `forceEdit=true`); grid con columnas `dni`, `tipoCertificado`; form con panel `CertificadoDigital` (dni, tipoCertificado y los 4 paneles condicionales por tipo), `buttons-panel` canónico (`btnDelete`/`btnCancel`/`btnSave`); action-groups de los tres botones (btnSave = `remote-validationSave-action` → `save` → `back`), action-group `onChange-dispositivoCriptografico` y action-record `set-alias-null`.
- **Nuevo (delta):**
  - Grid: columna `<field name="enabled"/>` **al final** de las existentes (screen-certificados-digitales.md).
  - Form: `<field name="enabled" colSpan="3" colOffset="1"/>` en el panel `CertificadoDigital`, junto a DNI y tipo de certificado (la fila completa las 12 columnas — vistas.md §1.7).
  - Form: atributo `onNew="subsysCriptografia.Main@CertificadoDigital-onNew-action"` + action-group `…-onNew-action` (**al final** de la sección `sv-primary-actions`, tras el action-group `onChange-dispositivoCriptografico` preexistente) + action-record `…-set-enabled-true-action` (**al final** de la sección `sv-rules`, tras el action-record `set-alias-null` preexistente) que pone `enabled = true` — materializa U-certificados-digitales-001 (casilla «Habilitado» marcada al crear). Los elementos nuevos van **después** de sus hermanos preexistentes: no se reordena nada del fichero base (mínima superficie de cambio, design-contract.md §1.3).

ASCII Layout del form resultante (panel `CertificadoDigital`; un dibujo por estado de `tipoCertificado`, los paneles condicionales son bloques anidados con `showIf` en el panel):

```
ddtttttt·eee   ← dni(2) + tipoCertificado(6) + colOffset(1) + enabled(3) = 12   [delta: enabled; dni y tipo preexistentes, no se recolocan]
── tipoCertificado == 'FICHERO_BD' (panelFicheroBD, preexistente) ──────────
ppppffff····   ← password(4) + fichero(4)
── tipoCertificado == 'DISPOSITIVO_PKCS11' (panelPkcs11, preexistente) ─────
qqqqaaaaaaaa   ← dispositivoCriptografico(4) + alias(8)
── tipoCertificado == 'CLASSPATH' (panelClasspath, preexistente) ───────────
ppppcccccccc   ← password(4) + rutaClasspath(8)
── tipoCertificado == 'SISTEMA_ARCHIVOS' (panelSistemaArchivos, preexistente)
ppppssssssss   ← password(4) + rutaSistemaArchivos(8)
```

ASCII Layout del `buttons-panel` (preexistente, sin cambios):

```
bb......ccgg   ← btnDelete(2) + colOffset(6) + btnCancel(2) + btnSave(2)
```

`enabled` es una casilla con label corto («Habilitado») → `colSpan="3"` con `colOffset="1"`, de modo que la fila suma exactamente 12 (dni 2 + tipoCertificado 6 + offset 1 + enabled 3 — vistas.md §1.7); el offset además separa visualmente la casilla del selector de tipo. Se coloca en la fila de los datos generales (agrupación semántica exigida por el screen delta); los campos preexistentes no se recolocan — mínima intrusión. Los bordes de los paneles condicionales (split 4|8) son preexistentes y no cambian.

**Verificación:** `xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` y `grep -c 'enabled' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` ≥ 4 (columna, campo, onNew-group referencia y action-record).

### Paso 4 — Menús: sin cambios (verbatim del diseño)

El menú «Administración SV» → «Certificados digitales» ya existe (`administracionSv-certificadosDigitales-menuitem` en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`, `groups="admins"`, `order="11"`, apunta a `subsysCriptografia.Main@CertificadoDigital-action`) y el spec lo declara sin cambios. `design/menus.xml` contiene ese único `<menuitem>` **copiado verbatim del fichero real** (el XSD de `object-views` no admite un fichero sin elementos): la fusión es un **no-op** y el `menus.xml` del proyecto queda exactamente igual.

**Verificación:** `grep -n 'administracionSv-certificadosDigitales-menuitem' src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` sigue devolviendo exactamente una línea.

### Trazabilidad Origen spec → V/R/U → ubicación (fila que aplica a este fichero)

| U | Origen spec | Ubicación | Lógica |
|---|---|---|---|
| U-certificados-digitales-001 | RUI-certificados-digitales-formulario-001 | `views/Main-CertificadoDigital.xml` → `onNew` del form → action-group `…-onNew-action` → action-record `…-set-enabled-true-action` (Paso 3) | Al crear una entrada nueva la casilla «Habilitado» aparece marcada (disparador «al crear» → `<action-record>` desde `onNew`, según `k-validaciones/reglas-ui.md` §1 y §4). Solo UX: la garantía de persistencia la da R-CertificadoDigital-001. |

### Notas y supuestos que aplican (verbatim del diseño)

5. **U-001 con `onNew` explícito además del `default` del dominio.** El disparador «al crear» de la RUI se materializa con el mecanismo canónico de `k-validaciones/reglas-ui.md` (`<action-record>` desde `onNew`), garantizando la casilla marcada en el formulario con independencia de cómo propague Axelor el `default` del dominio al registro nuevo del cliente. No es el antipatrón «inicialización de campos del sistema en `onNew`» de `k-sistemas/modelos.md`: `enabled` es un campo `cliente` y la garantía de persistencia no recae en la vista sino en R-CertificadoDigital-001 (dominio).

6. **`menus.xml` del diseño con el menuitem preexistente verbatim.** La estructura de salida exige el fichero y el XSD de `object-views` no admite un raíz sin hijos, pero el menú de la pantalla ya existe y el spec lo declara sin cambios; por eso `design/menus.xml` lleva el `<menuitem>` **preexistente copiado verbatim** (misma `name`, `parent`, `title`, `action`, `groups`, `order`) con un comentario que declara que la fusión es un no-op. No se añade fila `Modificar` del `menus.xml` del proyecto a la tabla de ficheros porque no hay delta que fusionar.

10. **`btnDelete-action` preexistente sin `remote-validationDelete-action`.** El action-group del botón Borrar del form real solo contiene `delete` (sin la acción global de validación remota de borrado que prescribe la plantilla de `k-vistas/forms.md`). Se **conserva tal cual** por la regla de conservación/mínima intrusión de las iniciativas que modifican (`design-contract.md` §1.3): el delta del spec no toca el borrado, la entidad no tiene ninguna V- de `validateRemove` (el `validateRemove` heredado devuelve vacío, así que la acción remota sería un no-op) y ESC-006 solo exige que borrar siga funcionando. Normalizar ese action-group es una mejora fuera del alcance de esta iniciativa.
