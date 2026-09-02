---
type: implementation-task
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/firmas/util/SituacionFirmaBuilder.java` | Crear | k-sistemas | Calcula la `SituacionFirma` de un firmante a partir de su DNI y del subsistema de criptografía |

## Texto del diseño (verbatim)

### Paso 4 — Calculadora de la situación de firma

**Fichero:** `src/main/java/com/educaflow/subsystem/firmas/util/SituacionFirmaBuilder.java` (Crear)

Clase de utilidad del subsistema `firmas`, **sin estado**, con un único método estático. Es la que invoca el
cuerpo del campo derivado `situacionFirma` del dominio (Paso 2). Sigue el mismo patrón que
`com.educaflow.subsystem.criptografia.util.DispositivoCriptograficoInfoBuilder`, que ya alimenta un campo
calculado de un dominio desde una carpeta `util/`.

```java
// Clase: com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder
public static SituacionFirma build(User firmante);
//   Implementa el cálculo de CC-TareaFirma-001 (campo `situacionFirma`, momento: lectura).
//   Lógica:
//     1. Si `firmante` es null, o su DNI es null/en blanco, o DniUtil.isValid(dni) es false → SIN_DNI.
//        (La comprobación previa es obligatoria: getTipoAlmacenClaveByDni valida el DNI y aborta con un
//        error de negocio si no es válido; aquí un DNI inválido NO es un error, es una situación.)
//     2. Los pasos 3 y 4 van DENTRO de un try { … } catch (Exception e) { … } que degrada a un valor seguro:
//        registra el error con el logger de la clase y el DNI **enmascarado** (nunca completo: k-secure-coding
//        §8 sobre datos personales en logs) y devuelve SIN_CERTIFICADO.
//        Por qué SIN_CERTIFICADO y no SIN_DNI: es el valor seguro. Deja al firmante el panel de AutoFirma y su
//        botón «Atrás» (una salida y una alternativa real de firma) y, sobre todo, NO habilita la firma en
//        servidor con una situación que no se ha podido determinar. Además, si aun así se intentara firmar,
//        V-TareaFirma-004 lo rechaza en el servidor.
//        Por qué hace falta el try/catch y no basta con «no lances excepciones»: el getter que genera AOP para
//        un campo virtual envuelve la llamada en `try { … } catch (NullPointerException e) { logger.error(…) }`
//        (axelor-tools, Property.createGetterMethod) — SOLO captura NullPointerException. Cualquier otra
//        excepción de getTipoAlmacenClaveByDni (fallo de BD, certificado ilegible, RuntimeException de
//        validación del DNI) se propagaría y rompería la carga del formulario de firma.
//     3. Resuelve CertificadoDigitalService con Beans.get(ModelServiceFactory.class).resolve(CertificadoDigital.class)
//        y llama a getTipoAlmacenClaveByDni(dni).
//        Se usa `Beans.get` porque este método es estático y lo invoca el getter de una entidad generada: no hay
//        punto de inyección. Está permitido: la regla C14 de architecture-rules.md solo prohíbe `Beans.get` en
//        `..controller..` y `..service.impl..`, y esta clase está en `..util..`.
//     4. Si devuelve null (no hay certificado habilitado para ese DNI) → SIN_CERTIFICADO.
//     5. En otro caso traduce el TipoAlmacenClave al SituacionFirma del mismo nombre:
//        DISPOSITIVO_CON_PIN, DISPOSITIVO_SIN_PIN, FICHERO_CON_CLAVE, FICHERO_SIN_CLAVE.
//        MUST hacerse con un `switch` EXHAUSTIVO sobre todos los valores de TipoAlmacenClave y SIN rama
//        `default`: así, el día que el subsistema de criptografía añada un valor nuevo al enumerado, la
//        traducción deja de compilar en vez de degradar en silencio a un valor equivocado. Es el blindaje de
//        la sincronía entre los dos enumerados que explica la nota 12 de «Notas y supuestos».
//   MUST NOT devolver null nunca: el valor null del campo dejaría la pantalla sin ningún panel de contenido
//   visible (el panel de botones sí seguiría apareciendo, ver el `showIf` por defecto de
//   `buttonsPaso2FirmarSinDni` en el Paso 7).
//   MUST NOT propagar ninguna excepción: el try/catch del paso 2 es lo que lo garantiza, no una mera promesa.
```

**Por qué no vive en el `*ServiceImpl`.** El cálculo lo dispara el **getter de la entidad**, y las entidades de
`..db..` no pueden depender de `..service..` (regla C13 de `architecture-rules.md`). Colocándolo en
`firmas.util` la entidad depende de `firmas.util` y es esa clase —fuera del sujeto de C13— la que habla con el
servicio de criptografía. Es exactamente la solución que ya usa `DispositivoCriptografico.info`.

**Verificación:** `./gradlew clean build` compila y arranca; abrir una tarea pendiente del director muestra el
panel correspondiente a su situación.

### Campos calculados (sección «Trazabilidad Origen spec → V/R/U → ubicación»)

### Campos calculados

| Campo | Origen spec | Ubicación | Notas |
|---|---|---|---|
| `TareaFirma.situacionFirma` | CC-TareaFirma-001 | `domains/TareaFirma.xml` (campo derivado `transient` con cuerpo) → `SituacionFirmaBuilder.build(User)` | `momento: lectura` ⇒ campo derivado de solo lectura, sin R que lo asigne (design-contract §3). `sobreescribible: nunca` ⇒ fuera de todas las whitelists y recalculado en cada lectura. |

### Notas y supuestos aplicables

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

5. **`Beans.get` en `SituacionFirmaBuilder`.** Es un método estático invocado desde el getter de una entidad
   generada, así que no hay punto de inyección posible. La regla C14 de `architecture-rules.md` solo prohíbe
   `Beans.get` en `..controller..` y `..service.impl..`; `..util..` queda fuera. Es el mismo compromiso que ya
   acepta el proyecto en `DispositivoCriptograficoInfoBuilder`.

12. **Desviación acotada de `design-guidelines.md` §1 — por qué se crea el enumerado `SituacionFirma`.** La guía
    dice que la situación de firma se obtiene «reutilizando lo que ya existe […] no hay que inventar otra forma
    de averiguarlo». El **mecanismo** se respeta al pie de la letra: el dato se sigue obteniendo con
    `getTipoAlmacenClaveByDni(dni)` de `CertificadoDigitalService` y con su enumerado `TipoAlmacenClave`, y no
    hay ninguna consulta alternativa. Lo que **sí** se añade es un enumerado propio del subsistema de firmas, y
    esa es la desviación que aquí se documenta. Motivos:
    - `TipoAlmacenClave` **no puede** expresar dos de los seis casos que pide `design-guidelines.md` §3:
      `SIN_CERTIFICADO` (el `null` que devuelve el método) y `SIN_DNI` (el firmante sin DNI, que ni siquiera
      llega a consultarse). Un enumerado de cuatro valores más dos `null` distintos no permite los seis
      `showIf` excluyentes que la guía exige.
    - El campo derivado `situacionFirma` del dominio necesita un `<enum ref="…">` **del propio módulo**
      `firmas`; apuntar al enumerado de `criptografia` acoplaría el modelo de dominio de firmas al de
      criptografía, que hoy solo se tocan a través del interfaz del servicio.

    **Riesgo asumido y cómo se blinda:** quedan dos enumerados que mantener en sincronía (los cuatro valores
    centrales se llaman igual a propósito, para que el mapeo se lea de un vistazo). El blindaje es el `switch`
    **exhaustivo y sin rama `default`** del Paso 4: si algún día `TipoAlmacenClave` gana un valor nuevo, la
    traducción **deja de compilar** y obliga a decidir su `SituacionFirma`, en vez de degradar en silencio.
    **MUST NOT** sustituir ese `switch` por un `if/else` con `default`, ni por un `SituacionFirma.valueOf(
    tipoAlmacenClave.name())`, que fallaría en runtime en vez de en compilación.
