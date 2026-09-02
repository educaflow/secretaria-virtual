# R-TareaFirma-001 — Firmar en el servidor todos los documentos de la tarea (todo o nada)

**Entidad:** TareaFirma
**Origen spec:** RN-TareaFirma-001, RN-TareaFirma-002, RN-TareaFirma-006, RN-TareaFirma-007
**Operación:** firmarEnServidor (operación custom)
**Momento:** Antes de repository.save
**Servicio host:** com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl
**Método host:** private void fireActionRule_FirmarDocumentosEnServidor(TareaFirma tareaFirma)

## Análisis de la regla

### Qué se dispara y cuándo

Se dispara **dentro** de `TareaFirmaServiceImpl.firmarEnServidor(tareaFirma, tareaFirmaOriginal)`, después de que
`validateFirmarEnServidor(...)` haya pasado (V-TareaFirma-001..007) y **antes** de `repository.save(tareaFirma)`.
Es la regla que hace el trabajo real: producir la versión firmada de **todos** los documentos de la tarea usando el
certificado digital que la secretaría virtual tiene dado de alta para el DNI del firmante, sin AutoFirma y sin
intervención del ordenador del usuario.

### Qué información lee y de dónde

| Dato | Origen | Notas |
|---|---|---|
| DNI del firmante | `tareaFirma.getFirmante().getDni()` | Del servidor, nunca del cliente. `validateFirmarEnServidor` ya comprobó que es válido (V-TareaFirma-003). |
| Clave tecleada | `tareaFirma.getClaveFirma()` | Campo transitorio, whitelisteado en `allowPropertiesFirmarEnServidor()`. Puede ser `null` cuando la clave ya está guardada. |
| Almacén de claves | `CertificadoDigitalService.getAlmacenClaveByDni(dni, claveFirma)` | Subsistema `criptografia`. Se resuelve con `modelServiceFactory.resolve(CertificadoDigital.class)`. |
| Recuadro y página de la firma | `tareaFirma.getX()/getY()/getWidth()/getHeight()/getPage()` | Ya los fija quien creó la tarea; esta iniciativa no los cambia. |
| Documentos a firmar | `tareaFirma.getDocumentosFirma()` → `documentoOriginal` (MetaFile PDF) | `validateFirmarEnServidor` ya comprobó que hay al menos uno (V-TareaFirma-007). |

### Qué acciones realiza y en qué orden

La regla trabaja en **dos fases** para poder garantizar el «todo o nada» de RN-TareaFirma-002 sin dejar basura:

1. **Fase de firma (en memoria).** Recorre los `DocumentoFirma` de la tarea y, por cada uno, obtiene su
   `DocumentoPdf` original, lo firma y guarda el `DocumentoPdf` firmado en una lista local emparejada con su
   `DocumentoFirma`. **Todavía no se crea ningún `MetaFile` ni se toca ninguna entidad.**
2. **Fase de publicación.** Solo si la fase 1 terminó **entera** sin error, recorre la lista y, por cada par,
   crea el `MetaFile` del PDF firmado (`MetaFileHelper.createMetaFile`) y lo asigna con
   `documentoFirma.setDocumentoFirmado(...)`.

Si la fase 1 falla en cualquier documento, no se ha creado ningún `MetaFile` ni se ha modificado ningún
`DocumentoFirma`: la fase 2 no llega a ejecutarse y la regla propaga el fallo como error de negocio
(RN-TareaFirma-007). Como además todo ocurre antes del `repository.save` y dentro de la transacción del
`@CallMethod`, la tarea sigue `PENDIENTE` y ningún documento queda firmado (RN-TareaFirma-002).

### CRITICAL — un `AlmacenClave` nuevo por cada documento

`AlmacenClaveFichero` envuelve un `InputStream` sobre el fichero del certificado, y
`DocumentoPdf.firmar(...)` lo **consume** al construir el `KeyStore`. Reutilizar la misma instancia para el
segundo documento firma contra un stream ya agotado y falla.

**MUST** pedir un `AlmacenClave` **nuevo dentro del bucle**, una vez por documento
(`certificadoDigitalService.getAlmacenClaveByDni(dni, claveFirma)` en cada iteración).
**MUST NOT** obtener uno solo fuera del bucle y reutilizarlo: el escenario ESC-003, con dos documentos en la
misma tarea, fallaría en el segundo.

El `CampoFirma` sí puede construirse una sola vez fuera del bucle: no tiene estado consumible.

### Cuál clave se usa (RN-TareaFirma-006)

La decisión «clave guardada gana a clave tecleada» vive **dentro** de
`CertificadoDigitalServiceImpl.getAlmacenClaveByDni(dni, claveAcceso)`, no aquí:

- Si el `CertificadoDigital` del DNI tiene `password` no vacía → se usa **esa**, y el `claveAcceso` recibido se
  **ignora por completo** (situaciones `FICHERO_CON_CLAVE`).
- Si `password` está vacía o es `null` → se usa el `claveAcceso` recibido (situación `FICHERO_SIN_CLAVE`).
- Si el certificado está en un dispositivo PKCS#11 → se construye un `AlmacenClaveDispositivo(slot, alias)` y el
  `claveAcceso` recibido se **descarta sin usarlo**: `AlmacenClaveDispositivo` **no tiene forma de recibir un
  PIN** y el PIN efectivo lo aporta `EntornoCriptografico.getDispositivoCriptografico(slot)`, que lo lee de la
  **configuración del servidor**. En esta rama, por tanto, RN-TareaFirma-006 **no se cumple**: aunque el
  firmante teclee un PIN (V-TareaFirma-005 se lo exige en `DISPOSITIVO_SIN_PIN`), la firma no lo usa. Se acepta
  porque hoy `DISPOSITIVO_SIN_PIN` es **inalcanzable** (`DispositivoCriptografico.pin` es `required="true"`), así
  que la desviación no tiene efecto observable en ningún escenario. Está declarada en `design.md` §Notas y
  supuestos, nota 13, que además anota qué haría falta para cerrarla (ampliar la infraestructura de criptografía
  con un `AlmacenClaveDispositivo` que acepte PIN), **fuera del alcance de esta iniciativa**.

Así, el servicio de firmas no necesita conocer la situación para elegir la clave: le basta con pasar la que
tenga (o `null`).

### Qué efectos colaterales y garantías

- **Idempotencia:** la regla no es idempotente por sí sola, pero está protegida aguas arriba por
  V-TareaFirma-001 (la tarea debe estar `PENDIENTE`): una tarea ya firmada no vuelve a entrar.
- **Transaccionalidad:** el `@CallMethod` del controlador es `@Transactional`. Un error de negocio propagado
  desde aquí revierte la transacción, así que ni la tarea ni los `MetaFile` creados quedan en BD.
- **Ficheros físicos:** el diseño en dos fases evita crear ningún `MetaFile` cuando algo falla, así que tampoco
  quedan ficheros huérfanos en el directorio de subidas.
- **Nombre del fichero firmado:** `DocumentoPdf.firmar(...)` conserva el `fileName` del original, así que el
  `MetaFile` firmado se llama igual que el original. Es una diferencia deliberada respecto a AutoFirma (que
  añade el sufijo `_signed`); no afecta a ninguna regla del spec.

### Qué errores puede encontrar y cómo tratarlos

Todos los fallos de firma llegan como `RuntimeException` desde la infraestructura
(`DocumentoPdfImplIText.firmar` envuelve cualquier excepción; `CriptografiaUtil.getKeyStore` falla con una clave
incorrecta). La regla los captura y los convierte en un **error de negocio** cuyo mensaje **MUST** empezar por
el literal `No se han podido firmar los documentos: ` seguido del motivo concreto del fallo
(RN-TareaFirma-007), y lo lanza con `BusinessMessages.single(...)` + `throwIfInvalid()`.

**MUST NOT** registrar en el log la clave de firma ni ningún fragmento de ella al construir ese mensaje
(`[[k-secure-coding]]` §6). Si se loguea el fallo, se loguea el id de la tarea y el nombre del fichero, nunca la
clave.

## Diseño detallado

### Clases nuevas

Ninguna. La regla se implementa con métodos privados del `TareaFirmaServiceImpl` ya existente y reutiliza la
infraestructura del proyecto:

- `com.educaflow.base.infrastructure.metafile.MetaFileHelper` — `getDocumentoPdf(MetaFile)` y `createMetaFile(DocumentoPdf)`.
- `com.educaflow.base.infrastructure.pdf.DocumentoPdf` — `firmar(AlmacenClave, CampoFirma)`.
- `com.educaflow.base.infrastructure.pdf.CampoFirma` / `Rectangulo`.
- `com.educaflow.subsystem.criptografia.service.CertificadoDigitalService` — `getAlmacenClaveByDni(dni, claveAcceso)` (método nuevo, ver `design.md` Paso 3).

### Interfaces

Ninguna nueva.

### Tipos propios

Ninguno nuevo. La lista intermedia de la fase 1 es una estructura local del método (una lista de pares
`DocumentoFirma` ↔ `DocumentoPdf` firmado); **MUST NOT** convertirse en un tipo público del subsistema.

### Diagrama de secuencia

```
firmarEnServidor(tareaFirma, tareaFirmaOriginal)
  ├─ validateFirmarEnServidor(...)                        → Optional<BusinessMessages> (V-TareaFirma-001..007)
  ├─ fireActionRule_FirmarDocumentosEnServidor(tareaFirma)
  │    ├─ dni = tareaFirma.getFirmante().getDni()
  │    ├─ campoFirma = new CampoFirma(new Rectangulo(tareaFirma.getX().floatValue(), tareaFirma.getY().floatValue(),
  │    │                                               tareaFirma.getWidth().floatValue(), tareaFirma.getHeight().floatValue()))
  │    │                     .setNumeroPagina(tareaFirma.getPage())            ← .floatValue() OBLIGATORIO: x/y/width/height
  │    │                                                                         son BigDecimal y Rectangulo es
  │    │                                                                         record Rectangulo(float,float,float,float)
  │    ├─ FASE 1 — por cada documentoFirma de tareaFirma.getDocumentosFirma():
  │    │    ├─ almacenClave = certificadoDigitalService.getAlmacenClaveByDni(dni, tareaFirma.getClaveFirma())   ← NUEVO POR DOCUMENTO
  │    │    ├─ original     = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal())   → DocumentoPdf
  │    │    └─ firmado      = original.firmar(almacenClave, campoFirma)                               → DocumentoPdf (en memoria)
  │    └─ FASE 2 — solo si la fase 1 terminó entera:
  │         └─ por cada par (documentoFirma, firmado):
  │              ├─ metaFile = MetaFileHelper.createMetaFile(firmado)                                 → MetaFile
  │              └─ documentoFirma.setDocumentoFirmado(metaFile)
  ├─ fireActionRule_ResolverComoFirmada(tareaFirma)        → estado FIRMADO + fechaResolucion (R-TareaFirma-002)
  ├─ repository.save(tareaFirma)                           → persiste tarea + documentos (cascada)
  ├─ [finally] fireActionRule_DescartarClaveFirma(tareaFirma)  → claveFirma = null (R-TareaFirma-003)
  └─ fireActionRule_NotificarFirmaResuelta(tareaFirma)     → callback al proceso que encargó la firma (R-TareaFirma-004)
```

### Errores

| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| La clave tecleada no abre el fichero del certificado | `CriptografiaUtil.getKeyStore` vía `DocumentoPdf.firmar` | Capturar la `RuntimeException`, abortar la fase 1 y lanzar `BusinessMessages.single("No se han podido firmar los documentos: " + motivo)` con `throwIfInvalid()`. No se firma ningún documento. |
| El recurso del certificado no existe / no se puede leer | `CertificadoDigitalServiceImpl.getAlmacenClaveByDni` | Igual que el anterior: el mensaje sigue empezando por el literal de RN-TareaFirma-007. |
| El PDF original no es un PDF válido o está corrupto | `MetaFileHelper.getDocumentoPdf` | Igual que el anterior. |
| El certificado dejó de estar habilitado entre la validación y la firma | `CertificadoDigitalServiceImpl.getAlmacenClaveByDni` lanza `RuntimeException` | Igual que el anterior. La ventana es mínima porque `validateFirmarEnServidor` acaba de comprobar la situación real en el servidor. |
| El dispositivo PKCS#11 no está configurado en el servidor | `EntornoCriptografico.getDispositivoCriptografico` | Igual que el anterior. |

En **todos** los casos la tarea sigue `PENDIENTE` y el firmante puede reintentar (ESC-003).

### Contenido del método `fireActionRule_*`

```java
// Firma:
private void fireActionRule_FirmarDocumentosEnServidor(TareaFirma tareaFirma);
//   Implementa R-TareaFirma-001 (Origen spec: RN-TareaFirma-001, RN-TareaFirma-002, RN-TareaFirma-006,
//   RN-TareaFirma-007). Diseño detallado en design/rules/R-TareaFirma-001.md.
//   Momento: Antes de repository.save.
//   Secuencia:
//     1. Obtener el DNI del firmante desde la entidad (nunca del cliente).
//     2. Construir UNA sola vez el CampoFirma con el Rectangulo (x, y, width, height) y el número de página
//        que ya lleva la propia tarea, CONVIRTIENDO los cuatro valores con .floatValue():
//          new CampoFirma(new Rectangulo(tareaFirma.getX().floatValue(), tareaFirma.getY().floatValue(),
//                                        tareaFirma.getWidth().floatValue(), tareaFirma.getHeight().floatValue()))
//              .setNumeroPagina(tareaFirma.getPage())
//        MUST usar .floatValue(): x/y/width/height son <decimal> en el dominio (BigDecimal en la entidad
//        generada) y Rectangulo es un `record Rectangulo(float x, float y, float width, float height)`; sin la
//        conversión NO compila. Es exactamente lo que ya hace TareaFirmaController.firmarDocumentosConAutoFirma.
//     3. FASE 1 — por cada DocumentoFirma: pedir un AlmacenClave NUEVO (CRITICAL: uno por documento, el
//        InputStream de AlmacenClaveFichero se consume) con getAlmacenClaveByDni(dni, tareaFirma.getClaveFirma()),
//        leer el DocumentoPdf original con MetaFileHelper.getDocumentoPdf y firmarlo. El resultado se acumula
//        en una lista local emparejado con su DocumentoFirma; no se persiste nada todavía.
//     4. Si cualquier paso de la fase 1 lanza RuntimeException: abortarla y lanzar el error de negocio cuyo
//        mensaje empieza por «No se han podido firmar los documentos: » seguido del motivo del fallo
//        (BusinessMessages.single(...) + throwIfInvalid). MUST NOT loguear la clave de firma.
//     5. FASE 2 — solo si la fase 1 terminó entera: por cada par, crear el MetaFile con
//        MetaFileHelper.createMetaFile y asignarlo con documentoFirma.setDocumentoFirmado(...).
//   Garantía «todo o nada» (RN-TareaFirma-002): al no crear ningún MetaFile ni tocar ningún DocumentoFirma
//   hasta que TODOS los documentos están firmados, un fallo deja la tarea intacta; además la transacción del
//   @CallMethod revierte cualquier escritura.
```
