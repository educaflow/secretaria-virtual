---
type: implementation-task
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/firmas/domains/TareaFirma.xml` | Modificar | k-sistemas (modelos.md) | Añade el campo transitorio `claveFirma`, el campo derivado `situacionFirma` y el enum `SituacionFirma` |

## Cómo se materializa

El XML **ya está materializado** por el diseñador en `design/domains/TareaFirma.xml` (fichero completo
resultante: base real + delta) y es la **fuente de verdad**: **MUST** copiarse **literalmente** (`cp`) a
`src/main/java/com/educaflow/subsystem/firmas/domains/TareaFirma.xml`. **MUST NOT** regenerarlo desde el
`design.md`, ni reescribirlo, ni reformatearlo (indentación, orden de atributos).

La fila es `Acción: Modificar`: el fichero destino **ya existe**. Antes de sobrescribirlo **MUST** aplicarse la
**comprobación de conservación**: todo elemento con nombre del fichero real actual (campo, enum, item, finder)
tiene que estar presente en el XML del diseño, salvo lo declarado en la sección «Eliminaciones declaradas» del
`design.md` (que **no** declara ninguna eliminación en este fichero). Si pasa, se sobrescribe; si no, se
reporta `CONFLICT` sin fusionar a mano.

## Texto del diseño (verbatim)

### Paso 2 — Dominio: `TareaFirma` gana la clave de firma y la situación de firma

**Fichero:** `src/main/java/com/educaflow/subsystem/firmas/domains/TareaFirma.xml` (Modificar)
**XML del diseño:** `design/domains/TareaFirma.xml` (fichero completo resultante: base real + delta)

**Resumen estructural**

- **Preexistente (se conserva):** entidad `TareaFirma` con sus 16 campos actuales (`firmante`, `documentosFirma`,
  `fechaSolicitud`, `fechaResolucion`, `firmaRapida`, `estadoTareaFirma`, `motivoFirma`, `motivoRechazo`,
  `fqcnFirmaNotifier`, `fqcnCallBackData`, `callBackData`, `x`, `y`, `width`, `height`, `page`) y el enum
  `EstadoTareaFirma` con sus tres items. **Nada de esto cambia de semántica**, ni siquiera el campo `firmaRapida`
  (spec §Fuera de alcance: «se queda igual, sin uso»). Única salvedad, puramente sintáctica: en los tres items de
  `EstadoTareaFirma` el atributo `description="…"` pasa a `help="…"` con **el mismo texto**, porque
  `description` no lo admite el `domain-models.xsd` y el generador lo ignora (ver Notas y supuestos §1). No toca
  ni el enum generado, ni la BD, ni ninguna vista.
- **Delta (nuevo):**
  - `<string name="claveFirma" transient="true" password="true" title="Clave de firma">` — la clave que el
    firmante teclea. `transient="true"` ⇒ el generador emite `@Transient`: **nunca** hay columna ni fila en BD.
    `password="true"` ⇒ `Property.isPassword()`, y `Resource.toMap` **excluye** los campos password de la
    respuesta JSON: el valor viaja del cliente al servidor pero **nunca** vuelve al cliente.
  - `<enum name="situacionFirma" ref="SituacionFirma" transient="true">` con **cuerpo de cálculo** (CDATA) que
    delega en `SituacionFirmaBuilder.build(getFirmante())`. Al llevar cuerpo y ser `transient`, el generador
    emite `@Transient @VirtualColumn` y un getter que **recalcula en cada lectura**
    (`situacionFirma = computeSituacionFirma()`): es el campo derivado de solo lectura que exige
    `CC-TareaFirma-001` (`momento: lectura`), no se persiste y el cliente no lo puede dictar.
  - `<enum name="SituacionFirma">` con seis items: `SIN_DNI`, `SIN_CERTIFICADO`, `DISPOSITIVO_CON_PIN`,
    `DISPOSITIVO_SIN_PIN`, `FICHERO_CON_CLAVE`, `FICHERO_SIN_CLAVE`. Los cuatro últimos se llaman **igual** que
    los valores de `TipoAlmacenClave` del subsistema de criptografía, a propósito, para que el mapeo del
    `SituacionFirmaBuilder` sea trivial de leer.

**MUST NOT** poner `required="true"` en `claveFirma`: es un campo que solo existe durante la firma y que en el
resto de la vida de la tarea está vacío (`k-sistemas/modelos.md` §"Campos rellenados por el sistema").

**Verificación:** `./gradlew clean build` genera `build/src-gen/main/java/com/educaflow/subsystem/firmas/db/SituacionFirma.java`
y, en `TareaFirma.java`, un `@Transient @VirtualColumn private SituacionFirma situacionFirma;` con
`getSituacionFirma()` que llama a `computeSituacionFirma()`. `grep -n "claveFirma" build/src-gen/main/java/com/educaflow/subsystem/firmas/db/TareaFirma.java`
debe mostrar el campo anotado `@Transient`.

### Campos calculados (sección «Trazabilidad Origen spec → V/R/U → ubicación»)

### Campos calculados

| Campo | Origen spec | Ubicación | Notas |
|---|---|---|---|
| `TareaFirma.situacionFirma` | CC-TareaFirma-001 | `domains/TareaFirma.xml` (campo derivado `transient` con cuerpo) → `SituacionFirmaBuilder.build(User)` | `momento: lectura` ⇒ campo derivado de solo lectura, sin R que lo asigne (design-contract §3). `sobreescribible: nunca` ⇒ fuera de todas las whitelists y recalculado en cada lectura. |

### Notas y supuestos aplicables

1. **El XML de dominio del diseño no valida contra el XSD, igual que el fichero real.**
   El fichero real arrastra cinco incumplimientos del `domain-models.xsd`. De ellos, el diseño **conserva dos** y
   **corrige tres**:
   - **Se conservan (exención documentada):** los nombres de campo `x` e `y` (el XSD exige dos caracteres como
     mínimo). Renombrarlos cambiaría columnas de la BD y rompería
     `TareaFirmaController.firmarDocumentosConAutoFirma`, así que **MUST NOT** «arreglarlos» al implementar.
     Son los **dos únicos** incumplimientos que quedan en `design/domains/TareaFirma.xml`, y por eso `validate.sh`
     sigue dando `FAIL` sobre ese fichero: es esperado, no un defecto del diseño.
   - **Se corrigen:** el atributo `description` de los tres items de `EstadoTareaFirma` pasa a `help`, que es el
     atributo que el XSD acepta. Es un cambio **sin ningún efecto funcional** (el generador de dominios ignora
     `description`: no llega ni al enum generado ni a la BD ni al cliente) y reduce a dos los incumplimientos del
     fichero. El texto de los tres items se conserva palabra por palabra; solo cambia el nombre del atributo.
     Ver el delta del Paso 2.
   Los seis items nuevos de `SituacionFirma` usan `help` desde el principio, así que el delta **no añade** ningún
   incumplimiento nuevo.

3. **La situación de firma como campo derivado, no como acción.** `CC-TareaFirma-001` declara
   `momento: lectura`, y el contrato de diseño traduce eso a «campo derivado de solo lectura». Por eso se
   implementa como campo `transient` con cuerpo de cálculo (patrón ya usado por `DispositivoCriptografico.info`)
   y no como un `action-method` que rellene un campo dummy: así el valor está disponible en cuanto se abre el
   formulario, se recalcula en cada lectura y el servidor lo vuelve a calcular al validar la firma, sin
   ninguna vía por la que el cliente pueda dictarlo.

4. **Coste del campo derivado.** El punto de cálculo es **el getter**, no la vista, así que se paga en cada
   lectura de la propiedad y no solo cuando el `<field name="situacionFirma">` aparece pintado. En concreto lo
   atraviesan: (a) la serialización de la respuesta —cada `Resource.toMap` de la entidad recorre los getters—,
   (b) la obtención del original y de la entidad en `TareaFirmaController.validateFirmarEnServidor` y en
   `TareaFirmaController.firmarEnServidor` (`getOriginalModel()` / `getModel(...)`, que clonan el bean), y
   (c) las lecturas que hace el propio `validateFirmarEnServidor` (V-TareaFirma-003 … V-006 consultan
   `getSituacionFirma()`). Consecuencia: **una sola pulsación de «Firmar todos los documentos y finalizar»
   puede provocar varias consultas del certificado por DNI**, no una.
   Se acepta a propósito por dos motivos: es un finder indexado sobre un registro puntual (`findByDni`), y ese
   recálculo en cada lectura es justo lo que da la garantía que exige `entity-TareaFirma.md`
   §"Acción: Firmar en el servidor" — la situación que se valida es la **real en el momento de firmar**, no la
   que la pantalla tuviera pintada.
   **MUST NOT** añadirse `situacionFirma` a ningún `<grid>` ni a las vistas `Todos`/`Firmado`/`Rechazado`: ahí
   el coste pasaría a ser **por fila** y el orden de magnitud dejaría de ser aceptable.
