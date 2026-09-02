---
type: design
---

# Diseño: Firma en servidor de las tareas de firma

**Objetivo:** que la pantalla de documentos pendientes de firma decida por sí sola, según la situación del certificado digital del firmante, si le ofrece AutoFirma o firma sus documentos en el propio servidor.
**Capa:** subsystem/firmas
**Especificación de origen:** .sdd/drafts/2026-09-01_11-21_firma-en-servidor/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones, k-datainit

Esta iniciativa **modifica** el subsistema existente `subsystem/firmas` y **amplía** (sin romper nada) el subsistema `subsystem/criptografia`. No crea ningún subsistema nuevo, ninguna entidad nueva y ningún menú nuevo.

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-demo/input/documento_ejemplo_firma.pdf` | Crear | k-datainit | PDF de ejemplo (1 página) que usan las tareas de firma de demo |
| `src/main/java/com/educaflow/subsystem/firmas/domains/TareaFirma.xml` | Modificar | k-sistemas (modelos.md) | Añade el campo transitorio `claveFirma`, el campo derivado `situacionFirma` y el enum `SituacionFirma` |
| `src/main/java/com/educaflow/subsystem/firmas/util/SituacionFirmaBuilder.java` | Crear | k-sistemas | Calcula la `SituacionFirma` de un firmante a partir de su DNI y del subsistema de criptografía |
| `src/main/java/com/educaflow/subsystem/criptografia/service/CertificadoDigitalService.java` | Modificar | k-sistemas (servicios.md) | Añade `getAlmacenClaveByDni(dni, claveAcceso)` y su `validate…` |
| `src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` | Modificar | k-sistemas (servicios.md) | Implementa el nuevo overload; el de un argumento pasa a delegar en él |
| `src/main/java/com/educaflow/subsystem/firmas/service/TareaFirmaService.java` | Modificar | k-sistemas (servicios.md) | Añade `firmarEnServidor`, `validateFirmarEnServidor` y `allowPropertiesFirmarEnServidor` |
| `src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java` | Modificar | k-sistemas (servicios.md), k-secure-coding, k-validaciones | Implementa la acción de firma en servidor, sus validaciones, sus reglas y cierra la frontera de confianza de `insert`/`update` |
| `src/main/java/com/educaflow/subsystem/firmas/controller/TareaFirmaController.java` | Modificar | k-sistemas (controladores.md) | Añade los `@CallMethod` `validateFirmarEnServidor` y `firmarEnServidor` |
| `src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml` | Modificar | k-vistas (forms.md, actions.md) | Sustituye el panel único de firma por los seis paneles excluyentes y añade los botones y acciones de la firma en servidor |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | **Sin cambios**: el menú «Firmar documentos → Pendientes» ya existe y no se toca (ver `menus.xml` del diseño) |
| `src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoNotifier.java` | Crear | k-sistemas | `TareaFirmaNotifier` sin efectos para las tareas de firma de demo |
| `src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoLoader.java` | Crear | k-datainit | Callback `call=` del data-import de demo: crea los `DocumentoFirma` con el PDF de ejemplo |
| `src/main/resources/data-demo/input/firmas-demo.xml` | Crear | k-datainit | Las ocho tareas de firma precargadas |
| `src/main/resources/data-demo/input-config.xml` | Modificar | k-datainit | Añade el `<input>` de `firmas-demo.xml` |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto — y en esta iniciativa no aporta ningún `<menuitem>` nuevo, ver su cabecera). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

---

## Pasos

### Paso 1 — Recurso estático: el PDF de ejemplo a firmar

**Fichero:** `src/main/resources/data-demo/input/documento_ejemplo_firma.pdf` (Crear)

Documento PDF de **una sola página**, tamaño A4 (595 × 842 puntos), con un título corto en la parte superior
(p. ej. «Documento de ejemplo para pruebas de firma») y el resto de la página en blanco. **MUST** dejar libre la
banda donde las tareas de demo colocan el recuadro de la firma: `x=75`, `y=200`, `width=400`, `height=60`
(coordenadas PDF, origen abajo-izquierda), página `1`.

**MUST NOT** llevar campos de formulario, ni firmas previas, ni conformancia PDF/A (el firmado se hace en modo
*append*, así que un PDF plano es lo más seguro).

Va **junto a los datos de demo** (`src/main/resources/data-demo/input/`, la misma carpeta que `usuarios-demo.xml`),
porque `design-guidelines.md` lo dice literalmente: «las tareas de firma precargadas **y el PDF de ejemplo** son
datos de demo, así que su sitio natural es `src/main/resources/data-demo/`, no la `data-init` del subsistema».
Es el único consumidor que tiene: lo lee el `TareaFirmaDemoLoader` del Paso 10 y nadie más. **MUST NOT** ponerlo
en `src/main/resources/firma/` (donde vive `mi_certificado.p12`, que sí es un recurso del programa usado en
producción) ni en la `data-init` de `subsystem/firmas`.

Al colgar de `src/main/resources`, su **ruta de classpath** es `data-demo/input/documento_ejemplo_firma.pdf`, que
es la que usa el Paso 10.3.

**Verificación:** `ls src/main/resources/data-demo/input/documento_ejemplo_firma.pdf` y abrirlo: una página, sin firmas.

---

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

---

### Paso 3 — Criptografía: obtener el almacén de claves usando también una clave tecleada

**Ficheros:**
`src/main/java/com/educaflow/subsystem/criptografia/service/CertificadoDigitalService.java` (Modificar)
`src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` (Modificar)

**Por qué hace falta.** `getAlmacenClaveByDni(dni)` construye el `AlmacenClaveFichero` con la contraseña
**guardada** en el `CertificadoDigital`. Cuando esa contraseña está vacía (situación `FICHERO_SIN_CLAVE`, la de
ESC-002/003/004/005/013) el constructor de `AlmacenClaveFichero` rechaza el `null`, así que **con el método
actual esa situación no se puede firmar**. El diseño añade un **overload** que acepta la clave tecleada, y deja
el método de un argumento intacto para sus llamadores actuales (`AlmacenClaveResolver.getByDNI`).

**Delta del interface `CertificadoDigitalService`** (el resto de la interfaz se conserva):

```java
// Clase: com.educaflow.subsystem.criptografia.service.CertificadoDigitalService
AlmacenClave getAlmacenClaveByDni(String dni, String claveAcceso);
//   Devuelve el almacén de claves del certificado habilitado del DNI, usando `claveAcceso` SOLO cuando el
//   certificado no tiene guardada su propia clave. `claveAcceso` puede ser null.

Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni, String claveAcceso);
//   Validador de la acción anterior (regla C23 de architecture-rules.md: misma lista de tipos de parámetros).
```

**MUST NOT** declarar `allowPropertiesGetAlmacenClaveByDni…`: la acción recibe escalares, no la entidad
construida desde el request (`k-sistemas/servicios.md` §"`allowPropertiesXxx` y campos `servidor`").

**Delta de `CertificadoDigitalServiceImpl`** (el resto de la clase se conserva; los métodos preexistentes
`remove`, `getTipoAlmacenClaveByDni`, `validateInsert`, `validateUpdate`, `validateCertificado` y
`validateGetTipoAlmacenClaveByDni` **no cambian**):

```java
// Clase: com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl
@Override
public AlmacenClave getAlmacenClaveByDni(String dni);
//   CAMBIA: su cuerpo pasa a ser una única delegación `return getAlmacenClaveByDni(dni, null);`.
//   El comportamiento observable para sus llamadores actuales es idéntico (con clave guardada el segundo
//   argumento se ignora; sin clave guardada seguía siendo imposible construir el almacén).

@Override
public AlmacenClave getAlmacenClaveByDni(String dni, String claveAcceso);
//   Aplica:
//     - Primera línea: validateGetAlmacenClaveByDni(dni, claveAcceso).ifPresent(BusinessMessages::throwIfInvalid).
//     - Busca el certificado con ((CertificadoDigitalRepository) repository).findByDni(dni) (finder ya existente).
//     - Guarda de código (no validación de usuario): si no hay certificado o está deshabilitado, lanza
//       RuntimeException indicando el DNI — mismo comportamiento y mismo mensaje que hoy.
//     - Según `tipoCertificado` construye el AlmacenClave:
//         * FICHERO_BD / CLASSPATH / SISTEMA_ARCHIVOS → AlmacenClaveFichero(streamDelCertificado, clave), donde
//           `clave` es la guardada (`certificado.getPassword()`) si NO está en blanco y, solo si lo está, el
//           `claveAcceso` recibido. Esto materializa RN-TareaFirma-006: la clave guardada gana siempre y la
//           tecleada se ignora cuando existe la guardada.
//         * DISPOSITIVO_PKCS11 → AlmacenClaveDispositivo(slot, alias), exactamente como hoy. El `claveAcceso`
//           recibido se DESCARTA sin usarlo: AlmacenClaveDispositivo no tiene forma de recibir un PIN y el PIN
//           efectivo lo aporta EntornoCriptografico.getDispositivoCriptografico(slot), que lo lee de la
//           configuración del servidor. Esto es una desviación consciente de RN-TareaFirma-006 en la rama de
//           dispositivo, sin efecto observable hoy (DISPOSITIVO_SIN_PIN es inalcanzable): ver §Notas y
//           supuestos, nota 13. MUST NOT «arreglarse» aquí inventando un PIN en el AlmacenClave.
//     - El código de construcción del stream por tipo (MetaFileUtil.downloadContent, getResourceAsStream,
//       Files.newInputStream) es el que ya existe: se extrae a un método privado del bloque «Otras funciones»
//       para que los dos overloads no lo dupliquen.

@Override
public Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni, String claveAcceso);
//   Aplica la misma comprobación que el validador de un argumento: delega en
//   validateGetAlmacenClaveByDni(dni). `claveAcceso` no se valida aquí (puede ser null por diseño: significa
//   «no me han tecleado ninguna»); que sea obligatoria o no depende de la situación de firma y eso se valida
//   en V-TareaFirma-005 / V-TareaFirma-006, dentro del subsistema de firmas.
//   MUST NOT incluir el valor de `claveAcceso` en ningún mensaje ni en ningún log (k-secure-coding §6, §8).
```

**Verificación:** `./gradlew clean build` compila; `grep -n "getAlmacenClaveByDni" src/main/java/com/educaflow/subsystem/criptografia/service/CertificadoDigitalService.java`
muestra los dos overloads y sus dos validadores.

---

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

---

### Paso 5 — Servicio de firmas: la acción `firmarEnServidor`

**Ficheros:**
`src/main/java/com/educaflow/subsystem/firmas/service/TareaFirmaService.java` (Modificar)
`src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java` (Modificar)

#### 5.1 Delta del interface `TareaFirmaService`

El resto de la interfaz se conserva tal cual (`insert(DTO)`, `marcarComoFirmada`, `marcarComoRechazada`,
`validarDocumentosFirmados` y sus validadores y `allowProperties*`).

```java
// Clase: com.educaflow.subsystem.firmas.service.TareaFirmaService
TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Firma en el servidor TODOS los documentos de la tarea con el certificado digital del firmante y deja la
//   tarea resuelta como FIRMADO.

Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Validador de la acción (V-TareaFirma-001 .. V-TareaFirma-007).

AllowProperties allowPropertiesFirmarEnServidor();
//   Whitelist del bind cliente→entidad de la acción. Se declara porque la acción recibe la entidad construida
//   desde el request (`ActionRequestHelper.getModel(...)`).
```

#### 5.2 Delta de `TareaFirmaServiceImpl`

Se conserva todo lo que no se menciona aquí: el constructor, `insert(TareaFirmaInsertDTO)`,
`marcarComoRechazada`, `validarDocumentosFirmados`, los cuatro `validate*` preexistentes, los tres
`allowProperties*` preexistentes y `fireActionRule_NotificarFirmaResuelta`.

**Campo nuevo de la clase:**

```java
// Dependencia nueva del *ServiceImpl:
@Inject
private ModelServiceFactory modelServiceFactory;
//   Es la forma canónica de obtener otro ModelService desde un ModelService
//   (k-sistemas/servicios.md §"Obtener otro servicio desde un servicio"): el CertificadoDigitalService se
//   resuelve dentro del método que lo necesita, NUNCA se inyecta con @Inject.
```

**Bloque (1) Acciones:**

```java
@Override
public TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Cuerpo (patrón validate + throw, sin cuerpos implementados aquí):
//     try {
//       1. validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal).ifPresent(BusinessMessages::throwIfInvalid);
//          — primera línea ejecutable del método.
//       2. fireActionRule_FirmarDocumentosEnServidor(tareaFirma);   // R-TareaFirma-001, Antes
//       3. fireActionRule_ResolverComoFirmada(tareaFirma);          // R-TareaFirma-002, Antes
//       4. tareaFirma = repository.save(tareaFirma);                // NUNCA super.update
//     } finally {
//       5. fireActionRule_DescartarClaveFirma(tareaFirma);          // R-TareaFirma-003, siempre
//     }
//       6. fireActionRule_NotificarFirmaResuelta(tareaFirma);       // R-TareaFirma-004, Después
//       7. return tareaFirma;
//   El try/finally envuelve también la validación a propósito: RN-TareaFirma-008 exige descartar la clave
//   también cuando la acción termina sin firmar (validación rechazada o firma fallida).

@Override
public TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   CAMBIA (delta mínimo): las dos asignaciones inline de estado y fecha de resolución se sustituyen por una
//   llamada a fireActionRule_ResolverComoFirmada(tareaFirma), la misma regla que usa firmarEnServidor.
//   El resto del método (validate + throw, repository.save, fireActionRule_NotificarFirmaResuelta) no cambia
//   y su comportamiento observable es idéntico. Motivo: RN-TareaFirma-003 queda ubicada en un único sitio.
```

**Bloque (2) Métodos de Validación:**

```java
@Override
public Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Acumula en un BusinessMessages y devuelve `messages.isValid() ? Optional.empty() : Optional.of(messages)`.
//   CRITICAL — todas las comprobaciones se hacen sobre el estado REAL del servidor, nunca sobre lo que la
//   pantalla tuviera pintado: el estado y el firmante se leen de la entidad cargada de BD y la situación de
//   firma se obtiene de tareaFirma.getSituacionFirma(), un getter que RECALCULA en cada llamada
//   (campo derivado del Paso 2). Es lo que exige entity-TareaFirma.md §"Acción: Firmar en el servidor".
//   Aplica:
//     - V-TareaFirma-001 (Origen spec: VAL-TareaFirma-001) estado de la tarea: comprueba que
//       estadoTareaFirma es PENDIENTE. Mensaje: el literal de la spec «Solo se pueden firmar las tareas
//       pendientes de firmar».
//     - V-TareaFirma-002 (Origen spec: VAL-TareaFirma-002) titularidad: comprueba que el firmante de la tarea
//       es el usuario autenticado, obtenido del servidor con AuthUtils.getUser(). Mensaje: el literal de la
//       spec «Solo puede firmar los documentos la persona a la que se le han encargado».
//       Es la defensa real de HU-004: el <domain> del action-view es solo UX (k-secure-coding §1).
//     - V-TareaFirma-003 (Origen spec: VAL-TareaFirma-003) DNI del firmante: comprueba que la situación de
//       firma NO es SIN_DNI. Mensaje: el literal de la spec «No es posible firmar los documentos porque su
//       usuario no tiene un DNI. Póngase en contacto con el administrador.».
//     - V-TareaFirma-004 (Origen spec: VAL-TareaFirma-004) certificado dado de alta: comprueba que la
//       situación de firma NO es SIN_CERTIFICADO. Mensaje: el literal de la spec «No es posible firmar en el
//       servidor porque no tiene un certificado digital dado de alta».
//     - V-TareaFirma-005 (Origen spec: VAL-TareaFirma-005) PIN obligatorio: si la situación es
//       DISPOSITIVO_SIN_PIN, comprueba que claveFirma no está vacía. Mensaje: el literal de la spec
//       «El PIN es obligatorio».
//     - V-TareaFirma-006 (Origen spec: VAL-TareaFirma-006) contraseña obligatoria: si la situación es
//       FICHERO_SIN_CLAVE, comprueba que claveFirma no está vacía. Mensaje: el literal de la spec
//       «La contraseña es obligatoria».
//     - V-TareaFirma-007 (Origen spec: VAL-TareaFirma-007) documentos: comprueba que la tarea tiene al menos
//       un DocumentoFirma. Mensaje: el literal de la spec «La tarea de firma no tiene ningún documento que
//       firmar».
//   MUST NOT incluir el valor de claveFirma en ningún mensaje (k-secure-coding §6).
//   MUST NOT usar el parámetro tareaFirmaOriginal para nada más que la simetría de firma con el resto de
//   acciones del servicio: la acción no compara con el original.
```

**Bloque (3) AllowProperties:**

```java
@Override
public AllowProperties allowPropertiesFirmarEnServidor();
//   return AllowProperties.createAllowProperties(Map.of("claveFirma", Map.of()));
//   Whitelist con UN solo campo: es el único `cliente` de la acción (entity-TareaFirma.md §"Acción: Firmar en
//   el servidor" → Input AllowProperties: clave de firma). Todo lo demás (estado, fechas, firmante, recuadro,
//   documentos, situación de firma) lo dicta el servidor y queda FUERA de la whitelist.

@Override
public AllowProperties allowPropertiesInsert();
//   return AllowProperties.createDenyAllProperties();
//   entity-TareaFirma.md §"Acción: Crear" declara «Input AllowProperties: (ninguna)»: las tareas de firma no
//   se dan de alta desde la interfaz, solo con el DTO programático insert(TareaFirmaInsertDTO). Cerrar la
//   whitelist impide que el endpoint REST automático /ws/rest/<FQN> cuele campos (k-secure-coding §3.2).

@Override
public AllowProperties allowPropertiesUpdate();
//   return AllowProperties.createDenyAllProperties();
//   entity-TareaFirma.md §"Acción: Modificar" declara «Input AllowProperties: (ninguna)»: la tarea solo cambia
//   mediante sus acciones propias (marcarComoFirmada, marcarComoRechazada, firmarEnServidor), nunca guardando
//   el formulario. Ninguna vista de TareaFirma usa `save`, así que no hay flujo legítimo que esto rompa.
```

**Bloque (4) Action Rules:**

```java
private void fireActionRule_FirmarDocumentosEnServidor(TareaFirma tareaFirma);
//   Implementa R-TareaFirma-001 (Origen spec: RN-TareaFirma-001, RN-TareaFirma-002, RN-TareaFirma-006,
//   RN-TareaFirma-007). Momento: Antes de repository.save.
//   Diseño detallado en design/rules/R-TareaFirma-001.md (incluye la regla CRITICAL de pedir un AlmacenClave
//   nuevo por cada documento y el esquema en dos fases que garantiza el «todo o nada»).
//   Resumen de la secuencia: resolver CertificadoDigitalService con modelServiceFactory; construir el
//   CampoFirma con el recuadro y la página de la propia tarea (MUST convertir x/y/width/height con
//   .floatValue(): son BigDecimal y Rectangulo es un record de cuatro `float`, igual que hace
//   TareaFirmaController.firmarDocumentosConAutoFirma); firmar TODOS los documentos en memoria; y solo
//   si todos han salido bien, crear sus MetaFile y asignarlos a cada DocumentoFirma. Cualquier fallo se
//   convierte en un error de negocio cuyo mensaje empieza por «No se han podido firmar los documentos: ».

private void fireActionRule_ResolverComoFirmada(TareaFirma tareaFirma);
//   Implementa R-TareaFirma-002 (Origen spec: RN-TareaFirma-003; campos `estadoTareaFirma` y `fechaResolucion`
//   clasificados `servidor`). Momento: Antes de repository.save.
//   Asignación INCONDICIONAL: tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO) y
//   tareaFirma.setFechaResolucion(LocalDateTime.now()).
//   MUST NOT añadir guardas `if (tareaFirma.getEstadoTareaFirma() == null)` ni
//   `if (tareaFirma.getFechaResolucion() == null)`: permitirían al cliente dictar el estado o falsificar la
//   fecha de resolución por el endpoint REST genérico (ver k-secure-coding §3.3).
//   Lo llaman las dos acciones que resuelven una tarea como firmada: marcarComoFirmada y firmarEnServidor.

private void fireActionRule_DescartarClaveFirma(TareaFirma tareaFirma);
//   Implementa R-TareaFirma-003 (Origen spec: RN-TareaFirma-004, RN-TareaFirma-008). Momento: siempre, en el
//   `finally` de firmarEnServidor — tanto si la firma se completó como si la abortó una validación o un fallo.
//   Asignación INCONDICIONAL: tareaFirma.setClaveFirma(null).
//   El campo es `transient`, así que nunca llega a BD; esta regla se ocupa del resto del rastro: la referencia
//   en memoria se suelta en cuanto termina la acción.
//   MUST NOT loguear la clave, ni entera ni truncada, ni devolverla en el ActionResponse (k-secure-coding §6, §8).
//   El campo lleva `password="true"` en el dominio, así que Resource.toMap tampoco la serializa de vuelta.

private void fireActionRule_NotificarFirmaResuelta(TareaFirma tareaFirma);
//   PREEXISTENTE, no cambia. Implementa R-TareaFirma-004 (Origen spec: RN-TareaFirma-005). Momento: Después de
//   repository.save. firmarEnServidor lo invoca igual que marcarComoFirmada y marcarComoRechazada, de modo que
//   el proceso que encargó la firma se entera exactamente igual que con AutoFirma.
```

**Verificación:** `./gradlew clean build`; `grep -n "if *(.*== *null).*set" src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java`
no debe devolver nada.

---

### Paso 6 — Controlador: los dos `@CallMethod` de la firma en servidor

**Fichero:** `src/main/java/com/educaflow/subsystem/firmas/controller/TareaFirmaController.java` (Modificar)

Se conservan los cuatro `@CallMethod` actuales (`firmarDocumentosConAutoFirma`, `marcarComoFirmada`,
`marcarComoRechazada`, `validarDocumentosFirmados`) sin ningún cambio. Delta:

```java
// Clase: com.educaflow.subsystem.firmas.controller.TareaFirmaController
@CallMethod
public void validateFirmarEnServidor(ActionRequest actionRequest, ActionResponse actionResponse);
//   Sin @Transactional: solo valida, no escribe.
//   Resuelve TareaFirmaService con modelServiceFactory.resolve(TareaFirma.class); construye
//   ActionRequestHelper<TareaFirma> y ActionResponseHelper; obtiene el original con getOriginalModel() y la
//   entidad con getModel(tareaFirmaService.allowPropertiesFirmarEnServidor()); delega en
//   tareaFirmaService.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal) y, si hay mensajes, los entrega
//   con actionResponseHelper.doResponseBusinessMessagesAsError(...).
//   Es la contrapartida de la acción de vista …-Remote-validateFirmarEnServidor-action: el nombre del método
//   coincide con el {nombreFuncionJava} embebido en el nombre de la acción (k-vistas/actions.md).

@CallMethod
@Transactional
public void firmarEnServidor(ActionRequest actionRequest, ActionResponse actionResponse);
//   Con @Transactional: escribe en BD (los MetaFile firmados, los DocumentoFirma y la propia tarea). Es lo que
//   hace que un fallo de firma revierta TODO (RN-TareaFirma-002).
//   Resuelve el servicio igual que arriba, obtiene original y entidad con la MISMA whitelist
//   allowPropertiesFirmarEnServidor() y llama a tareaFirmaService.firmarEnServidor(tareaFirma, tareaFirmaOriginal).
//   No monta ninguna respuesta: el cierre de la ventana lo hace el <action name="force-back"/> del
//   action-group de la vista, igual que ya ocurre con el flujo de AutoFirma.
//   MUST NOT capturar la excepción de negocio del servicio para reempaquetarla: si la firma falla, la
//   ValidationException que lanza BusinessMessages.throwIfInvalid llega al cliente como error, detiene la
//   cadena de acciones (el force-back no se ejecuta) y revierte la transacción — que es justo lo que piden
//   RUI-…-017 y RUI-…-018.
```

Los parámetros se llaman **`actionRequest`** y **`actionResponse`** (regla de `k-sistemas/controladores.md`).

**Verificación:** `grep -n "ActionRequest \|ActionResponse " src/main/java/com/educaflow/subsystem/firmas/controller/TareaFirmaController.java`
no debe mostrar ningún parámetro llamado `request`/`response`/`req`/`resp`.

---

### Paso 7 — Vista: los seis paneles del paso de firmar

**Fichero:** `src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml` (Modificar)
**XML del diseño:** `design/views/Pendiente-TareaFirma.xml` (fichero completo resultante: base real + delta)

**Resumen estructural**

- **Preexistente (se conserva):** el `<action-view>` `subsysFirmas.Pendiente@TareaFirma-action` con su
  `<domain>` por estado y firmante; el `<grid>` maestro; el `<panel-related>` «Documentos a firmar»; el panel
  `paso2Rechazado`; los paneles de botones `buttonsPaso1Inicio` y `buttonsPaso2Rechazado`; todos los
  `action-group`, el `action-condition` de rechazo, los tres `action-record` de `pasoActual` y los cuatro
  `action-method` existentes; y el bloque completo del detalle `DocumentoFirma` (grid + form con los dos
  `<viewer>` de PDF).
- **Delta (nuevo/cambiado):**
  - En el panel `tareaFirmaInsertDTO`: campo `situacionFirma` con `showIf="false"` (junto al `pasoActual` que
    ya estaba). Es lo que mete el valor en el registro del formulario para que los `showIf` lo puedan leer.
  - **Seis paneles excluyentes** que **sustituyen** al panel único `paso2Firmar`, todos con el mismo título
    visible «Firmar el documento» y con `showIf` sobre `pasoActual=='paso2Firmar'` **y** el valor de
    `situacionFirma`: `paso2FirmarSinCertificado`, `paso2FirmarDispositivoConPin`,
    `paso2FirmarDispositivoSinPin`, `paso2FirmarFicheroConClave`, `paso2FirmarFicheroSinClave`,
    `paso2FirmarSinDni`.
  - **Tres paneles de botones excluyentes** que **sustituyen** a `buttonsPaso2Firmar`:
    `buttonsPaso2FirmarAutoFirma` (solo `SIN_CERTIFICADO`), `buttonsPaso2FirmarServidor` (las cuatro
    situaciones de firma en servidor) y `buttonsPaso2FirmarSinDni`, que es el **caso por defecto**: su `showIf`
    **no** compara con `SIN_DNI`, sino que **niega** los cinco códigos que sí llevan botón de firmar
    (`!(SIN_CERTIFICADO || DISPOSITIVO_CON_PIN || DISPOSITIVO_SIN_PIN || FICHERO_CON_CLAVE || FICHERO_SIN_CLAVE)`).
    Así los tres siguen siendo mutuamente excluyentes **y cubren todo el dominio**: si `situacionFirma` llegara
    vacía o con un valor desconocido, el firmante sigue viendo el «Atrás» y no se queda en un paso sin salida
    (RUI-…-015, condición «Siempre»). El panel **de contenido** `paso2FirmarSinDni` **sí** conserva la
    comparación con `SIN_DNI` (RUI-…-006): su mensaje solo es cierto para ese caso.
  - Un `action-record` nuevo `…-set-claveFirma-null-action`, encadenado en el `onLoad-action` y en el
    `btnPaso1InicioFirmar-action`.
  - Un `action-group` nuevo `…-btnPaso2FirmarServidorGuardar-action`, un `action-validate` nuevo
    (`…-Local-validateFirmarEnServidor-action`) y dos `action-method` nuevos
    (`…-Remote-validateFirmarEnServidor-action` y `…-Remote-firmarEnServidor-action`).

**Por qué paneles anidados y no `showIf` en los campos.** Un elemento oculto con `showIf` **reserva sus
columnas** (k-vistas/forms.md §"Campos condicionales y el problema de los huecos"). Con seis variantes del paso
de firmar, la única maquetación que no deja huecos ni desplaza botones es un panel por variante con el `showIf`
**en el panel**. Y los `showIf` de los tres paneles de botones son mutuamente excluyentes por construcción,
porque los seis valores de `SituacionFirma` particionan el dominio.

#### ASCII Layout

Panel `tareaFirmaInsertDTO` (readonly). Tiene un único elemento condicional visible, `fechaResolucion`
(`showIf="fechaResolucion!=null"`), así que se dibuja **un ASCII Layout por estado**. En esta vista la tarea
está siempre pendiente, de modo que el estado real es el primero; el segundo se dibuja por completitud.

Leyenda: `m` = `motivoFirma` (8), `e` = `estadoTareaFirma` (4), `s` = `fechaSolicitud` (4),
`r` = `fechaResolucion` (4), `p` = `pasoActual` (6, `colSpan` por defecto), `q` = `situacionFirma` (6,
`colSpan` por defecto), `·` = columna vacía.

```
── fechaResolucion oculta (el caso de esta vista: la tarea está pendiente) ──
mmmmmmmmeeee   ← motivoFirma(8) + estadoTareaFirma(4)
ssss········   ← fechaSolicitud(4) + 8 columnas vacías (4 las reserva fechaResolucion, oculta)
ppppppqqqqqq   ← pasoActual(6) + situacionFirma(6)

── fechaResolucion visible ──
mmmmmmmmeeee   ← motivoFirma(8) + estadoTareaFirma(4)
ssssrrrr····   ← fechaSolicitud(4) + fechaResolucion(4) + 4 columnas vacías
ppppppqqqqqq   ← pasoActual(6) + situacionFirma(6)
```

`pasoActual` y `situacionFirma` llevan `showIf="false"`: son campos técnicos que meten su valor en el registro
del formulario para que los `showIf` de los demás paneles los puedan leer, y **nunca se pintan**. Su fila
aparece en el dibujo porque **reserva** sus doce columnas (un elemento oculto sigue consumiendo celdas,
k-vistas/forms.md §"Campos condicionales y el problema de los huecos"), pero visualmente el panel termina en la
fila de las fechas. Al ocupar la fila entera entre los dos, no dejan ningún hueco que empuje a otro campo.

Paneles del paso de firmar — **un dibujo por estado**, ya que son excluyentes:

```
── situacionFirma == SIN_CERTIFICADO → paso2FirmarSinCertificado ──
hhhhhhhhhhhh   ← help(12) con el aviso y el enlace de AutoFirma

── situacionFirma == DISPOSITIVO_CON_PIN → paso2FirmarDispositivoConPin ──
hhhhhhhhhhhh   ← help(12) «Los documentos se firmarán en el servidor…»

── situacionFirma == DISPOSITIVO_SIN_PIN → paso2FirmarDispositivoSinPin ──
hhhhhhhhhhhh   ← help(12) «…Introduzca el PIN de su dispositivo criptográfico.»
cccc········   ← claveFirma título «PIN» (4): mismo ancho que en el otro panel que la pide

── situacionFirma == FICHERO_CON_CLAVE → paso2FirmarFicheroConClave ──
hhhhhhhhhhhh   ← help(12) «Los documentos se firmarán en el servidor…»

── situacionFirma == FICHERO_SIN_CLAVE → paso2FirmarFicheroSinClave ──
hhhhhhhhhhhh   ← help(12) «…Introduzca la contraseña de su certificado.»
cccc········   ← claveFirma título «Contraseña» (4): es el título más largo de los dos y el que fija el ancho

── situacionFirma == SIN_DNI → paso2FirmarSinDni ──
hhhhhhhhhhhh   ← help(12) variant="warning" con el aviso de que no se puede firmar
```

La clave queda sola en su fila con hueco a la derecha: es correcto, no hay ningún campo semánticamente
relacionado con el que agruparla (k-vistas/forms.md §"Un campo solo en una fila es una señal de alerta").
Los dos paneles que la piden son variantes del **mismo** paso, así que el campo lleva **el mismo `colSpan="4"`**
en los dos: empieza en la columna 1 y termina en la 4 tanto con el título «PIN» como con el título «Contraseña»,
de modo que **sus dos bordes** —izquierdo y derecho— quedan alineados entre estados y el campo **no cambia de
tamaño ni de posición** al cambiar la situación de firma (`k-vistas/forms.md` §checklist: bordes alineados entre
filas y entre paneles condicionales). Es el mismo criterio que se aplica más abajo a los paneles de botones. El
ancho que se elige es el que admite el título **más largo** de los dos («Contraseña»); dárselo también al del
«PIN» solo le deja algo de holgura, que es preferible a que el recuadro salte de sitio.

`buttons-panel` — un dibujo por estado (los cinco paneles anidados son mutuamente excluyentes):

```
── pasoActual == paso1Inicio ──
rrr......fff   ← btnPaso1InicioRechazar(3) + colOffset(6) + btnPaso1InicioFirmar(3)          [3+6+3 = 12]

── pasoActual == paso2Rechazado ──
aaa......ggg   ← btnPaso2RechazadoAtras(3) + colOffset(6) + btnPaso2RechazadoGuardar(3)      [3+6+3 = 12]

── pasoActual == paso2Firmar && situacionFirma == SIN_CERTIFICADO ──
aaa....ggggg   ← btnPaso2FirmarAtrasAutoFirma(3) + colOffset(4) + btnPaso2FirmarGuardar(5)   [3+4+5 = 12]

── pasoActual == paso2Firmar && situacionFirma ∈ {DISPOSITIVO_CON_PIN, DISPOSITIVO_SIN_PIN,
                                                  FICHERO_CON_CLAVE, FICHERO_SIN_CLAVE} ──
aaa....ggggg   ← btnPaso2FirmarAtrasServidor(3) + colOffset(4) + btnPaso2FirmarServidorGuardar(5)  [3+4+5 = 12]

── pasoActual == paso2Firmar && cualquier otra situacionFirma (SIN_DNI, null o desconocida) ──
aaa·········   ← btnPaso2FirmarAtrasSinDni(3); no hay botón de firmar (RUI-…-014)
```

El panel de botones de la firma en servidor usa **el mismo reparto 3 + colOffset 4 + 5** que el panel
preexistente de AutoFirma: los tres paneles de botones son variantes del **mismo** paso, así que al cambiar de
situación de firma el botón principal **no cambia de tamaño ni de posición**, y sus bordes de columna quedan
alineados entre estados (`k-vistas/forms.md` §checklist: bordes alineados entre filas y entre paneles
condicionales). El reparto que se respeta es el del elemento **preexistente**: el que cede es el panel nuevo.

En los cinco estados el botón secundario (Atrás/Rechazar) está pegado a la izquierda y el principal pegado al
borde derecho (`colOffset + colSpan = 12`). En el estado del panel `buttonsPaso2FirmarSinDni` no hay principal:
la fila la ocupa solo el secundario, y ese hueco es la traducción visual de la desviación que el propio spec
declara («en el panel de firmante sin DNI no hay ningún botón de firmar»).

#### Acciones nuevas y cambiadas

| Acción | Tipo | Propósito | Campos/condiciones |
|---|---|---|---|
| `…-set-claveFirma-null-action` | `action-record` | Vacía la clave tecleada | `claveFirma` ← `eval: null` |
| `…-onLoad-action` | `action-group` (cambiada) | Al abrir la tarea: vacía la clave y sitúa el paso 1 | encadena `set-claveFirma-null` + `set-pasoActual-paso1Inicio` |
| `…-btnPaso1InicioFirmar-action` | `action-group` (cambiada) | Al entrar en el paso de firmar: vacía la clave y sitúa el paso 2 | encadena `set-claveFirma-null` + `set-pasoActual-paso2Firmar` |
| `…-btnPaso2FirmarServidorGuardar-action` | `action-group` (nueva) | Firma en el servidor y cierra | `Local-validateFirmarEnServidor` → `Remote-validateFirmarEnServidor` → `Remote-firmarEnServidor` → `force-back` |
| `…-Local-validateFirmarEnServidor-action` | `action-validate` (nueva) | Refuerzo **de cliente** de V-TareaFirma-005 / V-TareaFirma-006: avisa de la clave obligatoria sin roundtrip | dos `<error>`: «El PIN es obligatorio» si `situacionFirma=='DISPOSITIVO_SIN_PIN' && (claveFirma==null \|\| claveFirma=='')`; «La contraseña es obligatoria» si `situacionFirma=='FICHERO_SIN_CLAVE' && (claveFirma==null \|\| claveFirma=='')` |
| `…-Remote-validateFirmarEnServidor-action` | `action-method` (nueva) | Validación de servidor de la operación custom | `TareaFirmaController.validateFirmarEnServidor` |
| `…-Remote-firmarEnServidor-action` | `action-method` (nueva) | Ejecuta la firma en servidor | `TareaFirmaController.firmarEnServidor` |

La validación de la operación custom va **inmediatamente antes** de la operación, sin nada intercalado
(k-vistas/actions.md). Si cualquiera de las dos devuelve error, la cadena se detiene y el `force-back` **no**
se ejecuta: el firmante se queda en el paso de firmar, con su panel y con lo que hubiera tecleado
(RUI-…-017 y RUI-…-018).

**La clave obligatoria lleva además capa cliente.** `…-Local-validateFirmarEnServidor-action` es un
`<action-validate>` con **dos** `<error>` que reproducen **literalmente** los mensajes del spec («El PIN es
obligatorio» y «La contraseña es obligatoria»), cada uno condicionado a la situación de firma que lo hace
aplicable. Va **la primera** del `action-group`, antes de la validación remota, de modo que el firmante recibe
el aviso sin roundtrip; la fuente de verdad sigue siendo el servidor (V-TareaFirma-005 / V-TareaFirma-006), que
es la única capa por la que pasan todas las vías de entrada. Es el refuerzo **opcional** que admite
`k-validaciones/validaciones.md` §3, nunca la única capa. Como los dos mensajes son idénticos a los del
servidor, el escenario ESC-004 ve el mismo literal lo bloquee el cliente o lo bloquee el servidor.

**MUST NOT** implementarlo con un `<action-condition>` de dos `<check field="claveFirma"/>`: `ActionCondition`
hace `errors.put(field, …)` por cada `check`, así que el segundo **borra** el error del primero y solo se vería
uno de los dos mensajes. Los `<error>` de `<action-validate>` no tienen ese problema.

**Verificación:**
```bash
xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd \
  src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml
grep -nE '<form .*can(Back|Delete|Save)="true"' src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml   # sin coincidencias
grep -nE 'Remote-validate(Save|Delete)-action' src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml     # sin coincidencias
```

---

### Paso 8 — Menús

**Fichero:** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (Modificar — **sin cambios**)
**XML del diseño:** `design/menus.xml`

`screen-documentos-pendientes-de-firma.md` §Menú lo dice explícitamente: «Firmar documentos → Pendientes […]
No cambia». El `design/menus.xml` reproduce verbatim la rama «Firmar documentos» del fichero único del proyecto
para dejar constancia del estado esperado y para que el fichero valide contra el XSD; **MUST NOT** duplicar esos
`<menuitem>` al fusionar: ya existen con esos mismos `name`.

**Verificación:** `git diff --stat src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` no debe
mostrar ningún cambio.

---

### Paso 9 — Seguridad

**Fichero:** `src/main/java/com/educaflow/subsystem/firmas/data-init/input/auth-firmas.xml` (**sin cambios**)

Los permisos actuales ya materializan exactamente lo que pide el spec §Seguridad y **no hace falta tocarlos**:

- `TareaFirma.firmante` — `condition="self.firmante = ?"` con `conditionParams="__user__"`, y
  `read`/`write` a `true`, `create`/`remove`/`export` a `false`. Cada usuario ve y resuelve **solo** sus propias
  tareas, sin alcance por centro (la tarea no pertenece a un centro), y nadie —tampoco el Administrador— puede
  tocar la de otro.
- `DocumentoFirma.propio` — condición equivalente a través de la tarea.

No hay ningún permiso nuevo, ningún rol nuevo y ningún grupo nuevo. En lenguaje natural: **cualquier usuario
autenticado puede leer y resolver las tareas de firma en las que él es el firmante, y ninguna otra**. La firma
en el servidor no añade ninguna restricción por tipo de usuario ni por cargo: el control real está en a quién
le da de alta un certificado el Administrador (spec §Seguridad).

**Defensa en el servidor.** El permiso de Axelor y el `<domain>` del `action-view` filtran, pero la garantía la
da **V-TareaFirma-002**, que compara el firmante de la tarea con `AuthUtils.getUser()` dentro de
`validateFirmarEnServidor`: es la única capa por la que pasan todas las vías de entrada (k-secure-coding §1).

**Verificación:** `git diff --stat src/main/java/com/educaflow/subsystem/firmas/data-init/` vacío.

---

### Paso 10 — Datos de demo: las ocho tareas de firma precargadas

**Ficheros:**
`src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoNotifier.java` (Crear)
`src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoLoader.java` (Crear)
`src/main/resources/data-demo/input/firmas-demo.xml` (Crear)
`src/main/resources/data-demo/input-config.xml` (Modificar)

Son **datos de demo**, no datos iniciales: van en `src/main/resources/data-demo/` junto a `usuarios-demo.xml`
(lo pide `design-guidelines.md`) y por tanto solo se cargan con `data.import.demo-data = true`.
**MUST NOT** ponerlos en la `data-init` del subsistema.

#### 10.1 `firmas-demo.xml` — los datos

Raíz `<datos>`, con un nodo `<tareasFirma>` que contiene ocho `<tareaFirma>`, cada uno con estos atributos:
`firmante` (el `code` del `User`, que en este proyecto es su email), `motivoFirma` (el nombre visible de la
tarea) y `numeroDocumentos`.

| `firmante` | `motivoFirma` | `numeroDocumentos` | Lo usa |
|---|---|---|---|
| `director@mislata.es` | Firma de prueba 1 | 1 | ESC-001 |
| `director@mislata.es` | Firma de prueba 2 | 1 | ESC-002 |
| `director@mislata.es` | Firma de prueba 3 | **2** | ESC-003 |
| `director@mislata.es` | Firma de prueba 4 | 1 | ESC-011 |
| `director@mislata.es` | Firma de prueba 5 | 1 | ESC-004, ESC-005, ESC-012, ESC-013, ESC-010 |
| `secretario@mislata.es` | Firma de prueba del secretario | 1 | ESC-006, ESC-007, ESC-010 |
| `admin` | Firma de prueba del administrador 1 | 1 | ESC-008, ESC-010 |
| `admin` | Firma de prueba del administrador 2 | 1 | ESC-009 |

#### 10.2 `input-config.xml` — el binding (delta)

Se **añade** un `<input>` al final del `<xml-inputs>` existente, **conservando íntegros todos los `<input>` que
el fichero ya tiene** (los de `centros-demo.xml`, los de `usuarios-demo.xml` y los de `permisos-demo.xml`): el
delta es **solo la adición**, no se borra ni se reordena ninguno. Es también el **último** del fichero a
propósito, porque la tarea de firma necesita que su firmante (`User`) ya exista:

- `file="firmas-demo.xml"`, `root="datos"`.
- `<bind node="tareasFirma/tareaFirma" type="com.educaflow.subsystem.firmas.db.TareaFirma"`
  `search="self.motivoFirma = :motivoFirma AND self.firmante.code = :firmanteCode" create="true" update="false"`
  `call="com.educaflow.secretariavirtual.datademo.TareaFirmaDemoLoader:crearDocumentos"`.
  El `search` por (motivo, firmante) es la clave natural: recargar los datos de demo no duplica tareas, y
  `update="false"` impide que una recarga pise una tarea que un test ya resolvió.
- Binds internos: `@motivoFirma` → `motivoFirma`; `@firmante` como alias `firmanteCode` + `<bind to="firmante"`
  `type="com.axelor.auth.db.User" search="self.code = :firmanteCode" create="false" update="false"/>`;
  `@numeroDocumentos` como alias `numeroDocumentos` (solo lo consume el `call`); y valores fijos por `eval`
  para `estadoTareaFirma` (`PENDIENTE`), `fechaSolicitud` (la fecha/hora actual), `x` (75), `y` (200),
  `width` (400), `height` (60) y `page` (1) — el mismo recuadro que el PDF de ejemplo deja libre.
- El `motivoFirma` de la tarea es el nombre por el que el firmante la identifica en el listado, que es lo que
  usan todos los escenarios.

**MUST** respetar la firma exacta que exige el data-import para el `call=`: `(Object bean, Map values)`
devolviendo el bean (`k-datainit/input-config.md`).

#### 10.3 Las dos clases Java

```java
// Clase: com.educaflow.secretariavirtual.datademo.TareaFirmaDemoNotifier
// implements com.educaflow.subsystem.firmas.service.TareaFirmaNotifier
public void notify(TareaFirma tareaFirma, Object callBackData);
//   Notificador sin efectos para las tareas de firma de demo: no hay ningún proceso que avisar.
//   Cuerpo: no hace nada (a lo sumo una traza a nivel debug con el id de la tarea; NUNCA con datos sensibles).
//   Existe porque fireActionRule_NotificarFirmaResuelta hace Class.forName(fqcnFirmaNotifier): una tarea de
//   demo sin notificador rompería al firmarla o al rechazarla.

// Clase: com.educaflow.secretariavirtual.datademo.TareaFirmaDemoLoader
public Object crearDocumentos(Object bean, Map values);
//   Callback `call=` del data-import de demo (firma obligatoria de dos parámetros).
//   Secuencia:
//     1. Castea el bean a TareaFirma. Si ya tiene documentos (recarga sobre una tarea existente), lo devuelve
//        tal cual sin tocar nada: la carga es idempotente.
//     2. Fija fqcnFirmaNotifier con el nombre de TareaFirmaDemoNotifier.
//     3. Lee UNA sola vez del classpath los bytes de `data-demo/input/documento_ejemplo_firma.pdf` (Paso 1)
//        con TareaFirmaDemoLoader.class.getClassLoader().getResourceAsStream("data-demo/input/documento_ejemplo_firma.pdf")
//        (sin barra inicial, porque se pide al ClassLoader), en try-with-resources y envolviendo la IOException
//        en RuntimeException (es una guarda de código, no una validación del usuario). Si el recurso no existe,
//        getResourceAsStream devuelve null: MUST fallar con un RuntimeException explícito y no con un NPE opaco.
//        Después, tantas veces como diga el alias `numeroDocumentos` (índice i = 1..numeroDocumentos), crea la
//        copia de ese documento así:
//          a) String fileName = "documento_ejemplo_firma_" + i + ".pdf";
//             MUST ser distinguible por documento dentro de la misma tarea, porque el grid
//             subsysFirmas.Pendiente@TareaFirma.DocumentoFirma-grid muestra y ordena por
//             `documentoOriginal.fileName`, y los pasos 14-15 de ESC-003 («entra en el primer/segundo documento
//             del listado») necesitan dos filas distinguibles y con orden estable en «Firma de prueba 3»
//             (numeroDocumentos = 2).
//          b) DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes, fileName);
//             (com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory)
//          c) MetaFile metaFile = MetaFileHelper.createMetaFile(documentoPdf);
//             (com.educaflow.base.infrastructure.metafile.MetaFileHelper) — es el helper que usa el resto del
//             proyecto y el ÚNICO que deja el MetaFile con `fileName` y `fileType = "application/pdf"` puestos
//             (por dentro ya hace createMetaFileInstance + setFileName + setFileType + uploadContent).
//             CRITICAL — MUST NOT construir el MetaFile con MetaFileUtil.createMetaFileInstance() +
//             MetaFileUtil.uploadContent(...) «a secas»: `com.educaflow.base.util.MetaFileUtil` NO tiene ningún
//             método que rellene fileName/fileType (createMetaFileInstance() devuelve un MetaFile vacío y
//             uploadContent(metaFile, bytes) solo sube el contenido), así que el MetaFile quedaría con
//             fileType == null y fileName == null. Consecuencia real: la FASE 1 de
//             fireActionRule_FirmarDocumentosEnServidor (rules/R-TareaFirma-001.md) llama a
//             MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal()), que en isPdf lanza
//             RuntimeException("El MetaFile no tiene fileType definido") → la firma en servidor de TODAS las
//             tareas de demo fallaría siempre con «No se han podido firmar los documentos: …» y ESC-001/002/003
//             (T-001/T-002/T-003) no podrían pasar; y con fileName == null el grid citado en (a) pintaría filas
//             sin texto. Si por lo que sea se usara MetaFileUtil directamente, MUST hacerse antes
//             metaFile.setFileName(fileName) y metaFile.setFileType(MetaFileHelper.PDF_MIME_TYPE), y quedarse
//             con el MetaFile que DEVUELVE uploadContent (no con la instancia pasada).
//          d) Un DocumentoFirma con ese MetaFile como documentoOriginal, documentoFirmado a null y la tarea como
//             padre. Cada DocumentoFirma tiene su PROPIO MetaFile (su propia copia física del PDF), para que
//             firmar uno no afecte al otro.
//     4. Asigna la lista a la tarea y devuelve el bean.
//   MUST NOT llamar a TareaFirmaService.insert(...): el data-import ya persiste el bean que devuelve este
//   método, y hacerlo crearía la tarea dos veces.
```

**Por qué en `secretariavirtual.datademo`.** El conjunto de datos de demo es global (`src/main/resources/data-demo`,
con los centros y los usuarios de todos los subsistemas), así que su código de apoyo pertenece al ensamblaje,
que es la capa que puede depender de cualquier subsistema y de la que no depende nadie (reglas C3/C4/C5 de
`architecture-rules.md`). Meterlo en `subsystem/firmas` obligaría a inventar una carpeta que la estructura
canónica de un subsistema no contempla.

**Verificación:** con `data.import.demo-data = true` y la BD recreada, arrancar y comprobar en `psql`:
```sql
SELECT t.motivo_firma, u.code, count(d.id)
FROM firmas_tarea_firma t
JOIN auth_user u ON u.id = t.firmante
LEFT JOIN firmas_documento_firma d ON d.tarea_firma = t.id
GROUP BY t.motivo_firma, u.code ORDER BY 1;
```
Ocho filas; «Firma de prueba 3» con 2 documentos y el resto con 1.

---

### Paso 11 — Verificación final

Compilar, pasar los tests y arrancar:

```bash
./run.sh
```

`./run.sh` hace `./gradlew clean build` (compila y ejecuta los tests, incluidos los de arquitectura de
`com.educaflow.architecture` y los de vistas de `com.educaflow.views`) y arranca en el 8080.

Comprobaciones puntuales tras arrancar:

```bash
# El enum y los dos campos nuevos se han generado
grep -n "claveFirma\|situacionFirma" build/src-gen/main/java/com/educaflow/subsystem/firmas/db/TareaFirma.java
ls build/src-gen/main/java/com/educaflow/subsystem/firmas/db/SituacionFirma.java

# No hay guardas de mass-assignment
grep -nE "if\s*\(.*==\s*null\s*\).*set[A-Z]" src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java

# La clave de firma no se loguea en ningún sitio
grep -rn "getClaveFirma" src/main/java/com/educaflow | grep -i "log"
```

---

## Frontera de confianza — AllowProperties por acción

### `TareaFirmaServiceImpl.firmarEnServidor` (invocado desde `TareaFirmaController.firmarEnServidor` y `TareaFirmaController.validateFirmarEnServidor`)

Entidad: `TareaFirma`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Firmar en el servidor` de `entity-TareaFirma.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `claveFirma` | cliente | sí | Único input del usuario en esta acción (`Input AllowProperties: clave de firma`). Es transitorio: no se persiste nunca y `fireActionRule_DescartarClaveFirma` lo anula al terminar. |
| `situacionFirma` | servidor | **NO** | Campo derivado (`CC-TareaFirma-001`, momento lectura): lo recalcula el getter en cada lectura desde el DNI del firmante y el subsistema de criptografía. Aunque el cliente lo mandara, el getter lo sobrescribe. |
| `estadoTareaFirma` | servidor | **NO** | Asignado en `firmarEnServidor` → `fireActionRule_ResolverComoFirmada` (incondicional). |
| `fechaResolucion` | servidor | **NO** | Asignado en `firmarEnServidor` → `fireActionRule_ResolverComoFirmada` (incondicional). |
| `documentosFirma` | servidor | **NO** | Las versiones firmadas las produce el servidor en `fireActionRule_FirmarDocumentosEnServidor`. Dejarlo fuera es lo que impide que el cliente cuele un PDF «firmado» propio. |
| `firmante` | servidor | **NO** | Lo fijó quien creó la tarea; la acción no lo toca. Que esté fuera es lo que evita que alguien se autoasigne la tarea de otro. |
| `fechaSolicitud`, `motivoFirma`, `motivoRechazo`, `firmaRapida` | servidor | **NO** | Fijados al crear la tarea; esta acción no los toca. |
| `fqcnFirmaNotifier`, `fqcnCallBackData`, `callBackData` | servidor | **NO** | Los fija quien encarga la firma. Son especialmente sensibles: `fqcnFirmaNotifier` acaba en un `Class.forName` + `Beans.get`, así que dejarlos fuera de la whitelist es obligatorio. |
| `x`, `y`, `width`, `height`, `page` | servidor | **NO** | El recuadro y la página de la firma los fija quien encarga la firma; la acción los lee, no los escribe. |

### `TareaFirmaServiceImpl.marcarComoFirmada` (invocado desde `TareaFirmaController.marcarComoFirmada`) — PREEXISTENTE

Entidad: `TareaFirma`. **Forma elegida**: `createAllowProperties` (**PREEXISTENTE, no cambia**).
**Origen spec:** ninguna acción del `entity-TareaFirma.md` la declara (solo declara `Crear`, `Modificar` y `Firmar en el servidor`): es una acción **preexistente** del flujo de AutoFirma que esta iniciativa conserva.

Whitelist real: `Map.of("documentosFirma", Map.of("documentoFirmado", Map.of()))`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `documentosFirma.documentoFirmado` | cliente | sí | Es el flujo de **AutoFirma**: el PDF lo firma el navegador del usuario y sube ya firmado, así que el cliente **tiene** que poder dictarlo. `validarDocumentosFirmados` comprueba antes que lo subido es una firma válida del documento original. |
| `estadoTareaFirma`, `fechaResolucion` | servidor | **NO** | Los asigna `fireActionRule_ResolverComoFirmada` (incondicional). Tras el delta del Paso 5.2 es **la misma** regla que usa `firmarEnServidor`. |
| *(el resto de campos)* | servidor | **NO** | Fijados al crear la tarea; la acción no los toca. |

**No cambia en esta iniciativa**: se documenta porque `design-contract.md` §8.3 pide una tabla por **cada**
acción del servicio invocada desde un `@CallMethod`, y el diseño mantiene ese `@CallMethod` intacto.

### `TareaFirmaServiceImpl.marcarComoRechazada` (invocado desde `TareaFirmaController.marcarComoRechazada`) — PREEXISTENTE

Entidad: `TareaFirma`. **Forma elegida**: `createAllowProperties` (**PREEXISTENTE, no cambia**).
**Origen spec:** ninguna acción del `entity-TareaFirma.md` la declara: es una acción **preexistente** del flujo de rechazo que esta iniciativa conserva.

Whitelist real: `Map.of("motivoRechazo", Map.of())`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `motivoRechazo` | cliente | sí | Único input del usuario al rechazar: lo teclea en el panel `paso2Rechazado`. |
| `estadoTareaFirma`, `fechaResolucion` | servidor | **NO** | Los asigna el propio `marcarComoRechazada` (incondicional). |
| *(el resto de campos)* | servidor | **NO** | Fijados al crear la tarea; la acción no los toca. |

**No cambia en esta iniciativa**: se documenta por la misma razón que la anterior.

### `TareaFirmaServiceImpl.validarDocumentosFirmados` (invocado desde `TareaFirmaController.validarDocumentosFirmados`) — PREEXISTENTE, **deuda de seguridad**

Entidad: `TareaFirma`. **Forma elegida**: `createAllowAllProperties` (**PREEXISTENTE**).
**Origen spec:** ninguna acción del `entity-TareaFirma.md` la declara: es una acción **preexistente** del flujo de AutoFirma que esta iniciativa conserva.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| *(todos)* | cliente | **sí — allow-all** | ⚠️ **DEUDA PREEXISTENTE.** `allowPropertiesValidarDocumentosFirmados()` devuelve hoy `AllowProperties.createAllowAllProperties()`, así que en esa llamada el cliente puede dictar **cualquier** campo de la entidad gestionada (incluidos `estadoTareaFirma`, `fechaResolucion`, `firmante` o `fqcnFirmaNotifier`). La acción solo valida y no persiste, pero el bean queda contaminado dentro de la petición: en particular `validarDocumentosFirmados` comprueba la firma contra `tareaFirma.getFirmante().getDni()`, así que un cliente que dicte `firmante` valida el PDF contra el DNI que él elija. Contraviene `k-secure-coding` §3.2, que exige whitelist explícita sobre una entidad gestionada. |

**Fuera del alcance de esta iniciativa.** Se anota aquí para que no se lea como un descuido de este diseño:
la spec no pide tocar el flujo de AutoFirma y cerrarla exige comprobar qué campos necesita realmente
`validarDocumentosFirmados` (probablemente solo `documentosFirma.documentoFirmado`, como
`marcarComoFirmada`), lo que es un cambio de comportamiento del flujo preexistente. **MUST NOT** cerrarla «de
paso» al implementar esta iniciativa: hay que abordarla con sus propios tests. Ver también la nota 6 de
«Notas y supuestos».

### `TareaFirmaServiceImpl.insert` (endpoint REST automático `/ws/rest/<FQN>`)

Entidad: `TareaFirma`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-TareaFirma.md` — «(ninguna)».

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| *(todos)* | servidor | **NO** | Las tareas de firma no se dan de alta desde la interfaz: las crea el proceso que necesita la firma, por la vía programática `insert(TareaFirmaInsertDTO)` (§DTO de alta programática). El alta por el endpoint REST genérico no es un flujo legítimo. |

### `TareaFirmaServiceImpl.update` (endpoint REST automático `/ws/rest/<FQN>`)

Entidad: `TareaFirma`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-TareaFirma.md` — «(ninguna)».

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| *(todos)* | servidor | **NO** | La tarea solo cambia mediante sus acciones propias (`marcarComoFirmada`, `marcarComoRechazada`, `firmarEnServidor`), cada una con su propia whitelist. Ninguna vista de `TareaFirma` usa la acción `save`, así que cerrar esta puerta no rompe ningún flujo. |

### DTO de alta programática

`TareaFirmaInsertDTO` (preexistente, **no cambia**): `firmante`, `documentos`, `motivoFirma`, `areaFirma`,
`page`, `firmaNotifierClass`, `callBackData`. El DTO **es** la whitelist de esa vía (k-secure-coding §3.5) y no
llega del cliente: lo construye código de servidor (un `PhaseEventManager` de un trámite, o el
`TareaFirmaDemoLoader` de los datos de demo). Todos sus campos son legítimos porque quien encarga la firma es
precisamente quien decide a quién, qué documentos y dónde va el recuadro.

### `CertificadoDigitalServiceImpl.getAlmacenClaveByDni(String, String)`

**No declara `allowProperties…`**: recibe **escalares** (`String dni`, `String claveAcceso`), no la entidad
construida desde el request, y no la invoca ningún `@CallMethod` (la llama el servicio de firmas). No hay mapa
del cliente que filtrar (`k-sistemas/servicios.md` §"`allowPropertiesXxx` y campos `servidor`").

---

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones `V-TareaFirma-NNN`

| V | Origen spec | Ubicación | Qué comprueba |
|---|---|---|---|
| V-TareaFirma-001 | VAL-TareaFirma-001 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | La tarea está en estado `PENDIENTE` |
| V-TareaFirma-002 | VAL-TareaFirma-002 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | El firmante de la tarea es el usuario autenticado (`AuthUtils.getUser()`) |
| V-TareaFirma-003 | VAL-TareaFirma-003 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | La situación de firma no es `SIN_DNI` |
| V-TareaFirma-004 | VAL-TareaFirma-004 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | La situación de firma no es `SIN_CERTIFICADO` |
| V-TareaFirma-005 | VAL-TareaFirma-005 | **Servidor (fuente de verdad):** `TareaFirmaServiceImpl.validateFirmarEnServidor`. **Cliente (refuerzo opcional):** `<error>` «El PIN es obligatorio» de `…-Local-validateFirmarEnServidor-action` en `views/Pendiente-TareaFirma.xml` | Si la situación es `DISPOSITIVO_SIN_PIN`, `claveFirma` no está vacía |
| V-TareaFirma-006 | VAL-TareaFirma-006 | **Servidor (fuente de verdad):** `TareaFirmaServiceImpl.validateFirmarEnServidor`. **Cliente (refuerzo opcional):** `<error>` «La contraseña es obligatoria» de `…-Local-validateFirmarEnServidor-action` en `views/Pendiente-TareaFirma.xml` | Si la situación es `FICHERO_SIN_CLAVE`, `claveFirma` no está vacía |
| V-TareaFirma-007 | VAL-TareaFirma-007 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | La tarea tiene al menos un `DocumentoFirma` |

### Reglas de negocio `R-TareaFirma-NNN`

| R | Origen spec | Ubicación | Momento | Detalle |
|---|---|---|---|---|
| R-TareaFirma-001 | RN-TareaFirma-001, RN-TareaFirma-002, RN-TareaFirma-006, RN-TareaFirma-007 | `TareaFirmaServiceImpl.fireActionRule_FirmarDocumentosEnServidor` | Antes de `repository.save` | Detalle: `design/rules/R-TareaFirma-001.md` |
| R-TareaFirma-002 | RN-TareaFirma-003 | `TareaFirmaServiceImpl.fireActionRule_ResolverComoFirmada` | Antes de `repository.save` | Estado `FIRMADO` + `fechaResolucion` (asignación incondicional) |
| R-TareaFirma-003 | RN-TareaFirma-004, RN-TareaFirma-008 | `TareaFirmaServiceImpl.fireActionRule_DescartarClaveFirma` | Siempre (`finally` de `firmarEnServidor`) | `claveFirma` a `null` pase lo que pase |
| R-TareaFirma-004 | RN-TareaFirma-005 | `TareaFirmaServiceImpl.fireActionRule_NotificarFirmaResuelta` (preexistente) | Después de `repository.save` | Callback al proceso que encargó la firma |

### Campos calculados

| Campo | Origen spec | Ubicación | Notas |
|---|---|---|---|
| `TareaFirma.situacionFirma` | CC-TareaFirma-001 | `domains/TareaFirma.xml` (campo derivado `transient` con cuerpo) → `SituacionFirmaBuilder.build(User)` | `momento: lectura` ⇒ campo derivado de solo lectura, sin R que lo asigne (design-contract §3). `sobreescribible: nunca` ⇒ fuera de todas las whitelists y recalculado en cada lectura. |

### Reglas de UI `U-documentos-pendientes-de-firma-NNN`

Todas viven en `views/Pendiente-TareaFirma.xml`, form `subsysFirmas.Pendiente@TareaFirma-form`.

| U | Origen spec | Ubicación |
|---|---|---|
| U-documentos-pendientes-de-firma-001 | RUI-documentos-pendientes-de-firma-formulario-001 | `showIf` del panel `paso2FirmarSinCertificado` (`situacionFirma=='SIN_CERTIFICADO'`) |
| U-documentos-pendientes-de-firma-002 | RUI-documentos-pendientes-de-firma-formulario-002 | `showIf` del panel `paso2FirmarDispositivoConPin` |
| U-documentos-pendientes-de-firma-003 | RUI-documentos-pendientes-de-firma-formulario-003 | `showIf` del panel `paso2FirmarDispositivoSinPin` |
| U-documentos-pendientes-de-firma-004 | RUI-documentos-pendientes-de-firma-formulario-004 | `showIf` del panel `paso2FirmarFicheroConClave` |
| U-documentos-pendientes-de-firma-005 | RUI-documentos-pendientes-de-firma-formulario-005 | `showIf` del panel `paso2FirmarFicheroSinClave` |
| U-documentos-pendientes-de-firma-006 | RUI-documentos-pendientes-de-firma-formulario-006 | `showIf` del panel `paso2FirmarSinDni` |
| U-documentos-pendientes-de-firma-007 | RUI-documentos-pendientes-de-firma-formulario-007 | `title="PIN"` del `<field name="claveFirma">` de `paso2FirmarDispositivoSinPin` |
| U-documentos-pendientes-de-firma-008 | RUI-documentos-pendientes-de-firma-formulario-008 | `title="Contraseña"` del `<field name="claveFirma">` de `paso2FirmarFicheroSinClave` |
| U-documentos-pendientes-de-firma-009 | RUI-documentos-pendientes-de-firma-formulario-009 | `widget="password"` en los dos `<field name="claveFirma">` (reforzado por `password="true"` en el dominio) |
| U-documentos-pendientes-de-firma-010 | RUI-documentos-pendientes-de-firma-formulario-010 | `required="true"` en los dos `<field name="claveFirma">` |
| U-documentos-pendientes-de-firma-011 | RUI-documentos-pendientes-de-firma-formulario-011 | `action-record` `…-set-claveFirma-null-action`, encadenada en `…-onLoad-action` y en `…-btnPaso1InicioFirmar-action` |
| U-documentos-pendientes-de-firma-012 | RUI-documentos-pendientes-de-firma-formulario-012 | `showIf` del panel `buttonsPaso2FirmarAutoFirma`, que contiene `btnPaso2FirmarGuardar` |
| U-documentos-pendientes-de-firma-013 | RUI-documentos-pendientes-de-firma-formulario-013 | `showIf` del panel `buttonsPaso2FirmarServidor`, que contiene `btnPaso2FirmarServidorGuardar` |
| U-documentos-pendientes-de-firma-014 | RUI-documentos-pendientes-de-firma-formulario-014 | Panel `buttonsPaso2FirmarSinDni`: solo lleva `btnPaso2FirmarAtrasSinDni`. El caso `SIN_DNI` cae en este panel por ser el **caso por defecto** (ver U-…-015) |
| U-documentos-pendientes-de-firma-015 | RUI-documentos-pendientes-de-firma-formulario-015 | Un botón `btnPaso2FirmarAtras…` en cada uno de los tres paneles de botones del paso de firmar. Como la regla es de condición «Siempre», el tercer panel (`buttonsPaso2FirmarSinDni`) usa la **condición negada** de los cinco códigos con botón de firmar, no `situacionFirma=='SIN_DNI'`: así los tres paneles cubren **todo** el dominio y el «Atrás» también aparece si `situacionFirma` llegara vacía o con un valor desconocido |
| U-documentos-pendientes-de-firma-016 | RUI-documentos-pendientes-de-firma-formulario-016 | `<field name="claveFirma">` solo existe dentro de `paso2FirmarDispositivoSinPin` y `paso2FirmarFicheroSinClave` |
| U-documentos-pendientes-de-firma-017 | RUI-documentos-pendientes-de-firma-formulario-017 | `action-group` `…-btnPaso2FirmarServidorGuardar-action`: el `force-back` va **después** de la acción remota, así que un error detiene la cadena y `pasoActual` no cambia |
| U-documentos-pendientes-de-firma-018 | RUI-documentos-pendientes-de-firma-formulario-018 | Mismo `action-group`: no hay ninguna acción que vacíe `claveFirma` tras el error (solo se vacía al entrar en el paso, U-…-011) |

---

## Tests

- **Tests E2E** (Given/When/Then, lenguaje de negocio): descritos en `test-e2e-desc.md`, materializados a partir
  de los trece escenarios `ESC-001` … `ESC-013` del `specification.md`.
- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).

---

## Reglas del spec descartadas

Ninguna. Las siete `VAL-`, las ocho `RN-`, el `CC-` y las dieciocho `RUI-` de la especificación están ubicadas
en la matriz de trazabilidad.

Dos avisos sobre reglas que **sí** están ubicadas pero cuyo efecto hoy no se puede ejercitar — y, en el
segundo caso, tampoco se ejercitaría tal cual está la infraestructura:

- **RUI-…-003 y RUI-…-007** (panel y título «PIN» de `DISPOSITIVO_SIN_PIN`): el panel se construye y el `showIf`
  existe, pero el estado es **inalcanzable** porque `DispositivoCriptografico.pin` es `required="true"` y
  `getTipoAlmacenClaveByDni` nunca devuelve `DISPOSITIVO_SIN_PIN`. Es deliberado y lo pide
  `design-guidelines.md`; **MUST NOT** hacerse opcional el PIN en esta iniciativa.
- **VAL-TareaFirma-005** («El PIN es obligatorio»): la validación **sí se implementa** —V-TareaFirma-005 en el
  servidor y su `<error>` de refuerzo en el cliente— y, por el mismo motivo que el punto anterior, hoy no se
  puede disparar. Pero **MUST NOT** leerse como «solo falta que el PIN deje de ser obligatorio»: aunque el PIN
  llegara a pedirse, **la clave que teclee el firmante NO se usaría para firmar** en la rama de dispositivo. El
  almacén de esa rama es `AlmacenClaveDispositivo(slot, alias)`, que **no admite PIN**, y el PIN efectivo lo
  aporta el `EntornoCriptografico` configurado en el servidor (Paso 3 y `rules/R-TareaFirma-001.md` §«Cuál clave
  se usa»). Es decir: en `DISPOSITIVO_SIN_PIN` se exige un PIN que después se descarta sin usarlo. Esa
  desviación acotada respecto a RN-TareaFirma-006 —y lo que haría falta para cerrarla— está declarada en
  §Notas y supuestos, nota 13.

---

## Eliminaciones declaradas

| Elemento eliminado | Fichero | ID de spec que lo justifica |
|---|---|---|
| `<panel name="paso2Firmar">` (el panel único de firma) | `views/Pendiente-TareaFirma.xml` | `screen-documentos-pendientes-de-firma.md` §Paneles: «Los seis paneles siguientes **sustituyen** al único panel de firma que hay hoy». Su contenido (el aviso de AutoFirma) se conserva íntegro dentro del nuevo `paso2FirmarSinCertificado`. |
| `<panel name="buttonsPaso2Firmar">` (el panel de botones del paso de firmar) | `views/Pendiente-TareaFirma.xml` | RUI-…-012, RUI-…-013, RUI-…-014: los botones del paso de firmar dependen ahora de la situación de firma, así que se reparten en tres paneles excluyentes. |
| `<button name="btnPaso2FirmarAtras">` (el botón «Atrás» único) | `views/Pendiente-TareaFirma.xml` | RUI-…-015: pasa a haber un «Atrás» por panel de estado (`btnPaso2FirmarAtrasAutoFirma`, `btnPaso2FirmarAtrasServidor`, `btnPaso2FirmarAtrasSinDni`), los tres apuntando al **mismo** `…-btnPaso2FirmarAtras-action`, que **no** se elimina. Es la regla de botones gemelos de `k-vistas/forms.md`. |

Nada más se elimina. En particular **se conservan** el botón `btnPaso2FirmarGuardar` de AutoFirma con su
`onClick` `serial:…` intacto, sus dos `action-group` y los cuatro `action-method` preexistentes.

---

## Tests E2E supersedidos

Ninguno. Los tests E2E existentes de `src/test/e2e/subsystem/criptografia/` (`t-001` … `t-006`) siguen siendo
válidos: esta iniciativa no cambia la pantalla de certificados digitales ni el comportamiento del campo
«Habilitado». No hay tests E2E previos del subsistema de firmas.

---

## Notas y supuestos

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

2. **Por qué hace falta ampliar el subsistema de criptografía.** `design-guidelines.md` dice que el almacén se
   obtiene con `getAlmacenClaveByDni(String dni)`. Ese método, tal cual, **no puede** servir a las situaciones
   `FICHERO_SIN_CLAVE` (las de casi todos los escenarios): construye `AlmacenClaveFichero` con la contraseña
   guardada y su constructor rechaza el `null`. El diseño respeta la guía en lo esencial —el almacén se sigue
   pidiendo a ese mismo método de ese mismo servicio— y añade el **overload** mínimo que acepta la clave
   tecleada, dejando intacto el de un argumento y a sus llamadores.

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

6. **Cerrar `allowPropertiesInsert`/`allowPropertiesUpdate` de `TareaFirma`.** El `entity-TareaFirma.md` declara
   «(ninguna)» para `Crear` y `Modificar`, así que el diseño las materializa como whitelist vacía. Se ha
   comprobado que **ninguna** de las cuatro vistas de `TareaFirma` (`Pendiente`, `Todos`, `Firmado`,
   `Rechazado`) usa la acción `save`, así que no hay flujo legítimo que esto rompa. Queda un flanco conocido y
   **fuera del alcance de esta iniciativa**: con la whitelist vacía, un alta por `POST /ws/rest/<FQN>` llega a
   `JPA.edit` con el mapa vacío, y el `CLAUDE.md` del proyecto advierte de que no está comprobado si crea una
   fila vacía o revienta contra los `NOT NULL`. Aquí no es explotable para falsificar datos (no se puede dictar
   ningún campo), pero **MUST NOT** darse por resuelto el problema general que describe el `CLAUDE.md`.
   Quedan además dos deudas **preexistentes** que ni la spec ni esta iniciativa abordan, anotadas para que no
   se lean como descuidos de este diseño:
   - `allowPropertiesValidarDocumentosFirmados()` devuelve `AllowProperties.createAllowAllProperties()` sobre
     una entidad gestionada, así que en esa llamada el cliente puede dictar cualquier campo (ver la tabla
     `TareaFirmaServiceImpl.validarDocumentosFirmados` de §Frontera de confianza). **MUST NOT** cerrarse «de
     paso» al implementar esta iniciativa: cambia el flujo preexistente de AutoFirma y necesita sus propios
     tests.
   - `DocumentoFirma` sigue sin `ModelService` propio.

7. **El formulario no lleva el `buttons-panel` canónico Borrar/Cancelar/Guardar.** Es una desviación
   **preexistente** y justificada por el negocio: la pantalla es un asistente de dos pasos en el que la tarea
   no se guarda ni se borra desde el formulario, sino que se resuelve con acciones propias
   (`k-vistas/forms.md`: «salvo que […] haya algo en el negocio que te haga pensar que no es necesario»). El
   `<form>` sí cumple lo importante: todos los `can*` a `false`, ningún `onSave`, y ningún `<action-method>` de
   validación por entidad para `save`/`delete`. Por mínima intrusión, el diseño **no** cambia esto.

8. **El nombre del fichero firmado.** La firma en el servidor conserva el `fileName` del documento original
   (`DocumentoPdf.firmar` no lo cambia), a diferencia de AutoFirma, que añade el sufijo `_signed`. Ninguna regla
   del spec depende del nombre y los escenarios solo comprueban que el documento «tiene versión firmada»; se
   opta por no añadir código solo para renombrar.

9. **Reintentos y datos de demo.** Los escenarios que **resuelven** una tarea (ESC-001, 002, 003, 009, 011) la
   dejan en `FIRMADO`/`RECHAZADO` y una tarea resuelta no vuelve a `PENDIENTE`; por eso el spec da una tarea
   distinta a cada uno. Reejecutar la tanda completa de tests E2E exige **recrear la base de datos** para que
   los datos de demo vuelvan a cargarse. Los escenarios que dan de alta un certificado sí son reejecutables
   porque empiezan borrando la entrada del DNI, siguiendo el patrón de
   `src/test/e2e/subsystem/criptografia/`.

10. **`required="true"` en el campo de la clave, y las dos capas de la validación.** El `required` lo pide
    RUI-…-010 y aporta solo la **marca visual** del cliente: el `required` de Axelor bloquea el flujo de `save`,
    y esta pantalla no guarda —resuelve con una acción propia—, así que por sí solo no impide pulsar el botón.
    Por eso la clave obligatoria tiene **dos capas de validación**, no una:
    - **Cliente (opcional, refuerzo):** `…-Local-validateFirmarEnServidor-action`, un `<action-validate>` con
      los dos `<error>` y los literales exactos del spec, encadenado el primero del `action-group` del botón.
      Evita el roundtrip.
    - **Servidor (fuente de verdad, obligatoria):** V-TareaFirma-005 / V-TareaFirma-006 en
      `validateFirmarEnServidor`, con los mismos literales. Es la única capa por la que pasan todas las vías
      de entrada.

    Como los literales coinciden, ESC-004 ve el mismo mensaje («La contraseña es obligatoria») lo produzca el
    cliente o el servidor: no queda ninguna duda que resolver al ejecutar el E2E. **MUST NOT** quitar la
    validación de servidor en ningún caso; si algún día molestara la marca de `required`, la corrección sería
    cambiarla por `requiredIf` sobre el mismo panel.

11. **Ambigüedad resuelta — nombre de la acción.** La spec llama a la acción «Firmar en el servidor» y al botón
    «Firmar todos los documentos y finalizar». El diseño nombra el método del servicio, el `@CallMethod` y las
    acciones de vista como `firmarEnServidor` (y `validateFirmarEnServidor`), que es el nombre de la **acción**
    del `entity-*.md`, no el rótulo del botón; así el par acción ↔ validador y el `Remote-{nombreFuncionJava}`
    quedan alineados sin ambigüedad.

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

13. **Desviación acotada de RN-TareaFirma-006 — en `DISPOSITIVO_SIN_PIN` el PIN tecleado se descarta.**
    RN-TareaFirma-006 dice que «la clave que teclea el firmante solo se usa cuando no hay ninguna guardada». El
    diseño lo cumple en las dos ramas de **fichero** (`FICHERO_CON_CLAVE` ignora la tecleada, `FICHERO_SIN_CLAVE`
    la usa), pero **NO** en la rama de **dispositivo**, y conviene decirlo sin rodeos:
    - Lo que sí se hace: el panel `paso2FirmarDispositivoSinPin` pide el PIN (U-…-003, U-…-007, U-…-010) y
      V-TareaFirma-005 exige que no venga vacío, con el literal «El PIN es obligatorio».
    - Lo que **no** se hace: ese PIN **no llega nunca a la firma**. `getAlmacenClaveByDni(dni, claveAcceso)`
      construye para `DISPOSITIVO_PKCS11` un `AlmacenClaveDispositivo(slot, alias)`, cuyo **único** constructor
      útil recibe `(int slot, String alias)` y **no tiene forma de recibir un PIN**; quien aporta el PIN es
      `EntornoCriptografico.getDispositivoCriptografico(slot)`, que lo lee de la **configuración del servidor**
      (`entornoCriptografico.*` de `axelor-config.properties`), no de la pantalla. Por eso el `claveAcceso`
      recibido se descarta en esa rama (Paso 3 y `rules/R-TareaFirma-001.md` §«Cuál clave se usa»).
    - Por qué se acepta: hoy el caso es **inalcanzable** (`DispositivoCriptografico.pin` es `required="true"`,
      así que `getTipoAlmacenClaveByDni` nunca devuelve `DISPOSITIVO_SIN_PIN`), y el `specification.md`
      §"Fuera de alcance" deja explícitamente fuera hacer opcional ese PIN. La desviación, por tanto, **no tiene
      efecto observable** en ningún escenario del spec.
    - **Qué haría falta para cerrarla de verdad** (fuera del alcance de esta iniciativa, anotado para
      `/sdd-implementer` y para quien retome el tema): **no basta** con hacer opcional el PIN del dispositivo.
      Hace falta además **ampliar la infraestructura de criptografía** (`base/infrastructure/criptografia/`) con
      un `AlmacenClaveDispositivo` que acepte el PIN y con el camino en `DocumentoPdfImplIText` para que ese PIN
      llegue al `KeyStore` PKCS#11, en vez de tomarlo siempre de `EntornoCriptografico`. Son ficheros de
      infraestructura compartida, ajenos al subsistema `firmas`, y esta iniciativa **MUST NOT** tocarlos.
    - Mientras tanto, **MUST NOT** documentarse ni en el código ni en las vistas que el PIN tecleado «se usa
      para firmar»: se pide, se valida y se descarta.
