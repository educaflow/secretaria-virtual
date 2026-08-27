---
type: implementation-task
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Fichero de esta tarea (de la tabla "Ficheros a crear o modificar" del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` | Modificar | k-sistemas (modelos.md) | Añadir el campo `enabled` (boolean, `default="true"`, título «Habilitado») |

**XML ya materializado:** el fichero está en `design/domains/CertificadoDigital.xml` (dentro de la carpeta de la iniciativa `.sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/design/`) y se debe **copiar literalmente** a su ruta destino `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml`, **sin regenerarlo** (ver `implementation.md` §1). La fila es `Acción: Modificar`: el destino **ya existe** y antes de sobrescribir se aplica la **comprobación de conservación** de `implementation.md` §3.

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto; en esta iniciativa su único `<menuitem>` es el **preexistente sin cambios**, así que la fusión es un **no-op**: no duplicar ni modificar nada). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los ficheros `i18n_*.csv` **no se tocan**: los genera un script.

## Texto del diseño (verbatim)

### Paso 1 — Dominio: campo `enabled` en `CertificadoDigital` + backfill de las filas existentes

Copiar verbatim `design/domains/CertificadoDigital.xml` sobre `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml`.

Resumen estructural de `domains/CertificadoDigital.xml`:

- **Preexistente (se conserva):** entidad `CertificadoDigital` con sus 8 campos (`dni` required+unique, `tipoCertificado` enum required, `fichero`, `password`, `dispositivoCriptografico`, `alias`, `rutaClasspath`, `rutaSistemaArchivos`), el `finder-method findByDni` y el enum `TipoUbicacionCertificado` (4 items).
- **Nuevo (delta):** campo `<boolean name="enabled" default="true" title="Habilitado" help="…"/>` — nombre `enabled` y default `true` prescritos por `design-guidelines.md`. Sin `required`. El `default="true"` es la materialización **declarativa** de R-CertificadoDigital-001 (ver Trazabilidad): inicializa el atributo Java a `TRUE`, de modo que un alta cuya interfaz no envía valor para «habilitado» (p.ej. un POST al endpoint REST genérico sin ese campo) se guarda habilitada.

**Hechos verificados sobre el mecanismo** (condicionan el resto del diseño):

1. El `default="true"` del dominio **solo** inicializa el atributo Java de las entidades **nuevas**; **NO** genera DDL que rellene las filas **preexistentes** en BD: tras añadir la columna, esas filas quedan con `enabled` a **NULL**.
2. El getter que genera AOP para un `<boolean>` con `default` **colapsa NULL a `Boolean.FALSE`** (patrón `enabled == null ? Boolean.FALSE : enabled`): a nivel de entidad no existe un tercer estado observable.

Combinados, sin más medidas las filas legacy (NULL) se **leerían como deshabilitadas**, rompiendo VAL-CertificadoDigital-001 («NULL cuenta como habilitada») y el «Fuera de alcance» del spec (los certificados ya configurados deben seguir funcionando). Por eso este paso incluye una **migración de datos** (el script de esa migración, `V2__backfill_certificado_digital_enabled.sql`, se materializa en la **Tarea 02**; esta tarea solo coloca el XML del dominio).

**Verificación:** `xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml`; `grep -n 'name="enabled"' src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` devuelve la línea del campo.

### Trazabilidad Origen spec → V/R/U → ubicación (fila que aplica a este fichero)

| R | Origen spec | Ubicación | Lógica |
|---|---|---|---|
| R-CertificadoDigital-001 | RN-CertificadoDigital-001 | `domains/CertificadoDigital.xml` → atributo `default="true"` del campo `enabled` (Paso 1) | Al crear una entrada, si la interfaz no envía valor para «habilitado», se guarda habilitada (fase antes_de_commit). Materialización **declarativa** en el modelo en lugar de un `fireActionRule_*`: ver justificación en Notas y supuestos. |

### Notas y supuestos que aplican (verbatim del diseño)

3. **Semántica de NULL: el mecanismo real es getter + backfill.** Hecho verificado (Paso 1): el getter generado por AOP colapsa `enabled` a NULL en `Boolean.FALSE`, así que a nivel de entidad **no existe** un tercer estado observable — un NULL persistido se **leería como deshabilitada**, no como habilitada. La semántica normativa «NULL/no indicado cuenta como habilitada» (RN-CertificadoDigital-001 y «Fuera de alcance» del spec) se garantiza por dos vías: para las entidades **nuevas**, el `default="true"` del dominio inicializa el atributo a `TRUE`; para las filas **preexistentes**, el backfill del Paso 1 pone a `TRUE` la columna donde estaba NULL. Tras el backfill ninguna fila queda a NULL, y la condición del Paso 2 («`enabled` a FALSE vía getter ⇒ tratar como inexistente») es correcta. Caso residual: un cliente REST que enviara **explícitamente** `"enabled": null` persistiría NULL y esa entrada se leería como deshabilitada; ninguna interfaz del spec lo produce (la casilla del form siempre envía true/false) y RN-CertificadoDigital-001 cubre el campo **ausente** (default), no el null explícito — se acepta como comportamiento fuera de las interfaces contempladas.

4. **Por qué R-CertificadoDigital-001 es declarativa (`default="true"`) y no un `fireActionRule_*`.** El efecto íntegro de la regla es un valor por defecto para un campo de **origen cliente** (`enabled` está en `Input AllowProperties` de Crear y Modificar). El contrato del diseño prohíbe que un campo `cliente` sea asignado por una R-Antes-de-Crear (lo convertiría implícitamente en `servidor`), y `k-sistemas/modelos.md` reserva los `fireActionRule_*` de inicialización para campos del sistema. El `default="true"` del dominio cubre la condición exacta de la RN («la interfaz no envía valor»): el bean se construye con `TRUE` y solo un valor enviado por el cliente lo cambia. Ventaja adicional: no obliga a sobrescribir `insert` en un servicio que hoy no lo sobrescribe (mínima intrusión).

7. **Nombre del campo.** `enabled` (no `habilitado`) con `title="Habilitado"`, prescrito por `design-guidelines.md`. Los CSV de i18n no se crean ni editan (script automático).
