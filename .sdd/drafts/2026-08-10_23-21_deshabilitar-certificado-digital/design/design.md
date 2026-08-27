---
type: design
---

# Diseño: Deshabilitar certificado digital

**Objetivo:** Añadir a la entrada de certificado digital un booleano `enabled` (por defecto habilitado) de modo que una entrada deshabilitada se comporte, al resolver el certificado de una persona por su DNI, exactamente igual que si no existiera.
**Capa:** subsystem/criptografia
**Especificación de origen:** .sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` | Modificar | k-sistemas (modelos.md) | Añadir el campo `enabled` (boolean, `default="true"`, título «Habilitado») |
| `src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` | Modificar | k-sistemas (servicios.md), k-validaciones (validaciones.md), k-secure-coding | V-CertificadoDigital-001: en `getAlmacenClaveByDni` una entrada deshabilitada se trata igual que una inexistente (mismo error) |
| `src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` | Modificar | k-vistas (grids.md, forms.md, actions.md) | Columna «Habilitado» en el grid, casilla «Habilitado» en el form y `onNew` que la marca al crear |
| `src/main/resources/com/educaflow/secretariavirtual/startup/database/V2__backfill_certificado_digital_enabled.sql` | Crear | — | Migración Flyway de backfill: pone a `TRUE` la columna `enabled` de las filas preexistentes (que quedan a NULL al añadir la columna) — ver Paso 1 |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; en esta iniciativa su único `<menuitem>` es el **preexistente sin cambios**, así que la fusión es un **no-op**: no duplicar ni modificar nada). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los ficheros `i18n_*.csv` **no se tocan**: los genera un script.

La única fila `Crear` es el script de migración de datos (backfill), que no existe en el árbol real; por lo demás la iniciativa solo modifica el subsistema existente `subsystem/criptografia` (mínima intrusión: se amplían el dominio, el servicio y la vista existentes; no se crea ninguna pieza paralela).

## Pasos

### Paso 1 — Dominio: campo `enabled` en `CertificadoDigital` + backfill de las filas existentes

Copiar verbatim `design/domains/CertificadoDigital.xml` sobre `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml`.

Resumen estructural de `domains/CertificadoDigital.xml`:

- **Preexistente (se conserva):** entidad `CertificadoDigital` con sus 8 campos (`dni` required+unique, `tipoCertificado` enum required, `fichero`, `password`, `dispositivoCriptografico`, `alias`, `rutaClasspath`, `rutaSistemaArchivos`), el `finder-method findByDni` y el enum `TipoUbicacionCertificado` (4 items).
- **Nuevo (delta):** campo `<boolean name="enabled" default="true" title="Habilitado" help="…"/>` — nombre `enabled` y default `true` prescritos por `design-guidelines.md`. Sin `required`. El `default="true"` es la materialización **declarativa** de R-CertificadoDigital-001 (ver Trazabilidad): inicializa el atributo Java a `TRUE`, de modo que un alta cuya interfaz no envía valor para «habilitado» (p.ej. un POST al endpoint REST genérico sin ese campo) se guarda habilitada.

**Hechos verificados sobre el mecanismo** (condicionan el resto del diseño):

1. El `default="true"` del dominio **solo** inicializa el atributo Java de las entidades **nuevas**; **NO** genera DDL que rellene las filas **preexistentes** en BD: tras añadir la columna, esas filas quedan con `enabled` a **NULL**.
2. El getter que genera AOP para un `<boolean>` con `default` **colapsa NULL a `Boolean.FALSE`** (patrón `enabled == null ? Boolean.FALSE : enabled`): a nivel de entidad no existe un tercer estado observable.

Combinados, sin más medidas las filas legacy (NULL) se **leerían como deshabilitadas**, rompiendo VAL-CertificadoDigital-001 («NULL cuenta como habilitada») y el «Fuera de alcance» del spec (los certificados ya configurados deben seguir funcionando). Por eso este paso incluye una **migración de datos**:

**Migración de datos (backfill) — a implementar por `/sdd-implementer`** (este diseño solo la documenta; no toca `src/**`):

- Crear el script Flyway `V2__backfill_certificado_digital_enabled.sql` en `src/main/resources/com/educaflow/secretariavirtual/startup/database/` (la ubicación classpath que ya tiene configurada `DataBaseStartup.executeMigrate`; `V2` porque `baselineOnMigrate=true` deja la baseline en `V1` — ajustar el número si al implementar ya existiera una migración con esa versión), con:

  ```sql
  UPDATE criptografia_certificado_digital SET enabled = TRUE WHERE enabled IS NULL;
  ```

- Se ejecuta automáticamente en el arranque: `AppEventObserver.onAppStart` (observador de `StartupEvent`, que Axelor dispara **después** de actualizar el esquema — la columna ya existe) llama a `DataBaseStartup.startup()`, que lanza `flyway.migrate()`. Es idempotente (Flyway la aplica una sola vez y el `WHERE enabled IS NULL` la hace inocua si se re-ejecutara).
- Tras el backfill **ninguna fila queda a NULL**, y la condición del Paso 2 («`enabled` a FALSE vía getter ⇒ tratar como inexistente») es correcta también para las filas legacy.

**Verificación:** `xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml`; `grep -n 'name="enabled"' src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` devuelve la línea del campo; y tras el primer arranque `SELECT count(*) FROM criptografia_certificado_digital WHERE enabled IS NULL` devuelve 0.

### Paso 2 — Servicio: entrada deshabilitada = entrada inexistente

Clase: `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl` — **Acción: Modificar**. Solo cambia el método siguiente; **el resto de la clase se conserva** (incluidos `validateInsert`/`validateUpdate`/`validateCertificado`, `remove`, `validateGetAlmacenClaveByDni` y la ausencia de sobrescritura de `insert`/`update`).

```java
// Método (la firma NO cambia; cambia el criterio de "entrada inexistente" de su cuerpo):
public AlmacenClave getAlmacenClaveByDni(String dni);
//   Aplica:
//     - V-CertificadoDigital-001 (Origen spec: VAL-CertificadoDigital-001): tras recuperar la
//       entrada con el finder existente findByDni(dni), la condición que hoy detecta
//       "no existe entrada" se amplía para tratar igual una entrada recuperada cuyo
//       getEnabled() devuelva FALSE. Hecho verificado (Paso 1): el getter generado por AOP
//       colapsa NULL a Boolean.FALSE, y el default="true" del dominio NO rellena las filas
//       preexistentes; gracias al backfill del Paso 1 ninguna fila legacy queda a NULL, por lo
//       que "getEnabled() == FALSE => tratar como inexistente" es correcto — ver Notas.
//       En ambos casos se lanza LA MISMA
//       RuntimeException que hoy lanza el caso "no existe", con el mismo mensaje, que debe
//       transmitir: que no existe certificado para el DNI recibido (sin revelar que la entrada
//       existe pero está deshabilitada — exigencia de la spec y de design-guidelines.md).
//     - El resto del método (validateGetAlmacenClaveByDni como primera línea y el switch por
//       TipoUbicacionCertificado que construye el AlmacenClave) se conserva tal cual.
```

Con esto `AlmacenClaveResolver.getByDNI` (que delega en este método) queda cubierto sin tocarlo; los demás métodos de `AlmacenClaveResolver` (`getDirector`, `getSecretario`, `getDummy`) **no se tocan** (design-guidelines.md).

**Verificación:** `./gradlew compileJava` compila; `grep -n "enabled" src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` muestra la condición dentro de `getAlmacenClaveByDni` y en ningún otro método.

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

### Paso 4 — Menús: sin cambios

El menú «Administración SV» → «Certificados digitales» ya existe (`administracionSv-certificadosDigitales-menuitem` en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`, `groups="admins"`, `order="11"`, apunta a `subsysCriptografia.Main@CertificadoDigital-action`) y el spec lo declara sin cambios. `design/menus.xml` contiene ese único `<menuitem>` **copiado verbatim del fichero real** (el XSD de `object-views` no admite un fichero sin elementos): la fusión es un **no-op** y el `menus.xml` del proyecto queda exactamente igual.

**Verificación:** `grep -n 'administracionSv-certificadosDigitales-menuitem' src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` sigue devolviendo exactamente una línea.

### Paso 5 — Seguridad: sin cambios

La pantalla es de administración global (sin centro): el `menuitem` existente ya restringe el acceso al grupo `admins` y la entidad no tiene campo `centro` (no aplica filtro multi-centro; el Administrador ve las entradas de toda la aplicación, como declara la spec). No se crean permisos, roles ni grupos nuevos y no se modifica ningún `data-init`.

**Verificación:** ninguna carpeta `data-init` modificada (`git status` sin cambios bajo `data-init/`).

### Paso 6 — Datos iniciales: sin cambios

El recurso que usan los escenarios (certificado `firma/mi_certificado.p12`, contraseña `nadanada`) ya está dentro del WAR (es el que usa `AlmacenClaveResolver.getDummy()`). No se cargan datos maestros nuevos.

### Paso 7 — Verificación final

```bash
./run.sh
```

Hace `./gradlew clean build` (compila y ejecuta los tests JUnit, que deben pasar sin errores) y **arranca** la aplicación en el puerto 8080 con la configuración privada. Confirmar que la aplicación arranca sin errores y que la pantalla «Administración SV» → «Certificados digitales» muestra la columna «Habilitado» en el listado y la casilla «Habilitado» en el formulario.

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (V)

| V | Origen spec | Ubicación | Lógica |
|---|---|---|---|
| V-CertificadoDigital-001 | VAL-CertificadoDigital-001 | `CertificadoDigitalServiceImpl.getAlmacenClaveByDni` (Paso 2) | La entrada del DNI buscado debe estar habilitada: una entrada con `enabled` a FALSE se trata exactamente igual que la inexistencia de entrada — misma excepción y mismo mensaje («no existe certificado para ese DNI»). Capa servidor: es la única capa (la operación no tiene vista; la invocan los procesos de firma/sellado vía `AlmacenClaveResolver.getByDNI`). |

### Reglas de negocio (R)

| R | Origen spec | Ubicación | Lógica |
|---|---|---|---|
| R-CertificadoDigital-001 | RN-CertificadoDigital-001 | `domains/CertificadoDigital.xml` → atributo `default="true"` del campo `enabled` (Paso 1) | Al crear una entrada, si la interfaz no envía valor para «habilitado», se guarda habilitada (fase antes_de_commit). Materialización **declarativa** en el modelo en lugar de un `fireActionRule_*`: ver justificación en Notas y supuestos. |

### Reglas de UI (U)

| U | Origen spec | Ubicación | Lógica |
|---|---|---|---|
| U-certificados-digitales-001 | RUI-certificados-digitales-formulario-001 | `views/Main-CertificadoDigital.xml` → `onNew` del form → action-group `…-onNew-action` → action-record `…-set-enabled-true-action` (Paso 3) | Al crear una entrada nueva la casilla «Habilitado» aparece marcada (disparador «al crear» → `<action-record>` desde `onNew`, según `k-validaciones/reglas-ui.md` §1 y §4). Solo UX: la garantía de persistencia la da R-CertificadoDigital-001. |

Cobertura inversa: las 3 reglas del spec (`VAL-CertificadoDigital-001`, `RN-CertificadoDigital-001`, `RUI-certificados-digitales-formulario-001`) están mapeadas; ninguna descartada.

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).
- **Tests E2E**: `design/test-e2e-desc.md` — T-001…T-006, uno por escenario ESC-001…ESC-006 del spec.

## Reglas del spec descartadas

Ninguna.

## Notas y supuestos

1. **Dónde vive V-CertificadoDigital-001 (cuerpo de `getAlmacenClaveByDni`, no `validateGetAlmacenClaveByDni`).** `design-guidelines.md` exige que una entrada deshabilitada produzca **el mismo error** que la inexistencia de entrada. La inexistencia se detecta hoy en el cuerpo del método (RuntimeException «No existe certificado para el DNI: …»), no en `validateGetAlmacenClaveByDni` (que devuelve `BusinessMessages` y hoy está vacío). Poner la comprobación en `validate*` produciría un error de tipo distinto (BusinessException) al del caso inexistente, violando la guía; por eso la condición de inexistencia del cuerpo se **amplía** para incluir la entrada deshabilitada. `validateGetAlmacenClaveByDni` se conserva sin cambios.
2. **Por qué no un finder «findByDni habilitado».** Se valoró añadir un finder que filtre `enabled = true` (la entrada deshabilitada ni se recuperaría). Se descarta porque `validateCertificado` (unicidad de DNI) debe seguir viendo también las entradas deshabilitadas — habría que mantener dos finders en paralelo — y porque el criterio «deshabilitada = inexistente» se decide de todas formas en el cuerpo de `getAlmacenClaveByDni` para lanzar **el mismo error** que el caso inexistente (Nota 1): el filtro en la consulta no ahorraría nada y duplicaría el punto de decisión (mínima intrusión). El grid **tampoco** filtra por `enabled`: las entradas deshabilitadas siguen listándose (con la columna «Habilitado» sin marcar) y pueden abrirse, editarse y borrarse, como exigen ESC-005 y ESC-006.
3. **Semántica de NULL: el mecanismo real es getter + backfill.** Hecho verificado (Paso 1): el getter generado por AOP colapsa `enabled` a NULL en `Boolean.FALSE`, así que a nivel de entidad **no existe** un tercer estado observable — un NULL persistido se **leería como deshabilitada**, no como habilitada. La semántica normativa «NULL/no indicado cuenta como habilitada» (RN-CertificadoDigital-001 y «Fuera de alcance» del spec) se garantiza por dos vías: para las entidades **nuevas**, el `default="true"` del dominio inicializa el atributo a `TRUE`; para las filas **preexistentes**, el backfill del Paso 1 pone a `TRUE` la columna donde estaba NULL. Tras el backfill ninguna fila queda a NULL, y la condición del Paso 2 («`enabled` a FALSE vía getter ⇒ tratar como inexistente») es correcta. Caso residual: un cliente REST que enviara **explícitamente** `"enabled": null` persistiría NULL y esa entrada se leería como deshabilitada; ninguna interfaz del spec lo produce (la casilla del form siempre envía true/false) y RN-CertificadoDigital-001 cubre el campo **ausente** (default), no el null explícito — se acepta como comportamiento fuera de las interfaces contempladas.
4. **Por qué R-CertificadoDigital-001 es declarativa (`default="true"`) y no un `fireActionRule_*`.** El efecto íntegro de la regla es un valor por defecto para un campo de **origen cliente** (`enabled` está en `Input AllowProperties` de Crear y Modificar). El contrato del diseño prohíbe que un campo `cliente` sea asignado por una R-Antes-de-Crear (lo convertiría implícitamente en `servidor`), y `k-sistemas/modelos.md` reserva los `fireActionRule_*` de inicialización para campos del sistema. El `default="true"` del dominio cubre la condición exacta de la RN («la interfaz no envía valor»): el bean se construye con `TRUE` y solo un valor enviado por el cliente lo cambia. Ventaja adicional: no obliga a sobrescribir `insert` en un servicio que hoy no lo sobrescribe (mínima intrusión).
5. **U-001 con `onNew` explícito además del `default` del dominio.** El disparador «al crear» de la RUI se materializa con el mecanismo canónico de `k-validaciones/reglas-ui.md` (`<action-record>` desde `onNew`), garantizando la casilla marcada en el formulario con independencia de cómo propague Axelor el `default` del dominio al registro nuevo del cliente. No es el antipatrón «inicialización de campos del sistema en `onNew`» de `k-sistemas/modelos.md`: `enabled` es un campo `cliente` y la garantía de persistencia no recae en la vista sino en R-CertificadoDigital-001 (dominio).
6. **`menus.xml` del diseño con el menuitem preexistente verbatim.** La estructura de salida exige el fichero y el XSD de `object-views` no admite un raíz sin hijos, pero el menú de la pantalla ya existe y el spec lo declara sin cambios; por eso `design/menus.xml` lleva el `<menuitem>` **preexistente copiado verbatim** (misma `name`, `parent`, `title`, `action`, `groups`, `order`) con un comentario que declara que la fusión es un no-op. No se añade fila `Modificar` del `menus.xml` del proyecto a la tabla de ficheros porque no hay delta que fusionar.
7. **Nombre del campo.** `enabled` (no `habilitado`) con `title="Habilitado"`, prescrito por `design-guidelines.md`. Los CSV de i18n no se crean ni editan (script automático).
8. **Sin «Eliminaciones declaradas» ni «Tests E2E supersedidos».** No se elimina ningún elemento preexistente y ningún test E2E archivado ejercita esta pantalla ni la resolución por DNI; el comportamiento previo (entrada existente ⇒ se usa) solo cambia para entradas explícitamente deshabilitadas, estado que antes no existía.
9. **Sin sección «Frontera de confianza — AllowProperties por acción».** El diseño no declara ningún `@CallMethod` (no existe `CertificadoDigitalController`; el alta/edición/borrado van por el flujo genérico `remote-validationSave-action`/`save`/`delete` de `DefaultModelController` + `ModelService`), y `design-contract.md` §8.3 manda omitir la sección en ese caso. La decisión de seguridad del flujo genérico queda cerrada aquí: se mantiene el `createAllowAllProperties` heredado de `DefaultModelService` (no se sobrescribe — mínima intrusión), admisible según `k-secure-coding` §3.2 porque los 9 campos persistentes de `CertificadoDigital` son todos de origen **cliente**, aparecen en las líneas `Input AllowProperties` tanto de `Crear` como de `Modificar` de `entity-CertificadoDigital.md`, y no hay ningún campo `servidor` ni inmutable — allow-all equivale a la whitelist completa.
10. **`btnDelete-action` preexistente sin `remote-validationDelete-action`.** El action-group del botón Borrar del form real solo contiene `delete` (sin la acción global de validación remota de borrado que prescribe la plantilla de `k-vistas/forms.md`). Se **conserva tal cual** por la regla de conservación/mínima intrusión de las iniciativas que modifican (`design-contract.md` §1.3): el delta del spec no toca el borrado, la entidad no tiene ninguna V- de `validateRemove` (el `validateRemove` heredado devuelve vacío, así que la acción remota sería un no-op) y ESC-006 solo exige que borrar siga funcionando. Normalizar ese action-group es una mejora fuera del alcance de esta iniciativa.
