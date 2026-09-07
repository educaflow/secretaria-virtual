---
type: implementation-task
template: expediente
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-sistemas`
- `k-secure-coding`
- `k-code-quality`

Añade a `com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver` la sobrecarga `getByDNI(String dni, String claveAcceso)`.

## Nota del descomponedor (decisiones de esta descomposición)

Esta iniciativa es un **delta** sobre una versión existente y su tabla §6 lista, además de los ficheros del tipo de expediente, varios ficheros de **subsistema** (las «piezas compartidas» de §9.1 y las reglas de §10.1) que no corresponden a ningún bloque de la tabla de tareas del contrato. Decisión tomada: **una tarea por fichero de subsistema**, en el orden de los pasos del diseño, y **todas antes** de la tarea de la fase `RECEPCION`, que es la que las consume (inyecta el servicio y usa las reglas). Así cada fila de §6 queda cubierta por exactamente una tarea.

## Fichero de esta tarea — fila verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/service/AlmacenClaveResolver.java` | Modificar | `k-sistemas` | Especificado en §9.1 — nueva sobrecarga `getByDNI(String dni, String claveAcceso)` |

## Paso del diseño (verbatim del `design.md` §7)

### Paso 5 — `AlmacenClaveResolver.java`

- Fichero: `src/main/java/com/educaflow/subsystem/criptografia/service/AlmacenClaveResolver.java`; FQCN `com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver`.
- Especificación quirúrgica: **§9.1**, apartado «`AlmacenClaveResolver`».
- **Verificación:** compila; existe `public AlmacenClave getByDNI(String dni, String claveAcceso)`; el `getByDNI(String)` anterior sigue existiendo con el mismo comportamiento.

## Especificación quirúrgica (verbatim del `design.md` §9.1)

### 9.1 Piezas compartidas del subsistema que usan los `trigger*`

Son el **mecanismo reutilizable**: esta iniciativa las escribe (o las extrae de donde ya estaban) una sola vez y quedan disponibles para **cualquier** tipo de expediente que quiera firmar en el servidor su documento de entrada. Ninguna conoce este trámite.

Dos de ellas son **de-duplicación**, no funcionalidad nueva: `FirmaEnServidorHelper` extrae lo que hoy vive dentro de `TareaFirmaServiceImpl`, y `TareaFirmaServiceImpl` pasa a delegar en él. Sin ese par, la firma en servidor del documento de entrada y la regla del DSL serían la segunda y la tercera copia del mismo criterio y de los mismos literales (§14).


#### `AlmacenClaveResolver` — `com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver`

- **Nuevo** `public AlmacenClave getByDNI(String dni, String claveAcceso)`: resuelve el `CertificadoDigitalService` con el `ModelServiceFactory` ya inyectado y devuelve `certificadoDigitalService.getAlmacenClaveByDni(dni, claveAcceso)`.
- El `getByDNI(String dni)` existente **no cambia** (equivale a pasar `claveAcceso = null`).
- **MUST NOT** loguearse `claveAcceso` ni ninguna parte de ella.
- Recordatorio del contrato de `getAlmacenClaveByDni`: la clave **custodiada gana siempre** a la tecleada (CC-004), y si no hay certificado habilitado para el DNI lanza `RuntimeException`; por eso quien llame **MUST** haber comprobado antes la situación.
- ⚠️ **Recordatorio — la clave tecleada NO llega a un dispositivo criptográfico, y este delta NO lo arregla.** `CertificadoDigitalServiceImpl.getAlmacenClaveByDni(dni, claveAcceso)` usa `claveAcceso` **solo** en la rama `FICHERO_BD` / `CLASSPATH` / `SISTEMA_ARCHIVOS`; en `DISPOSITIVO_PKCS11` devuelve `new AlmacenClaveDispositivo(slot, alias)` y **descarta** `claveAcceso` (el PIN lo aporta `EntornoCriptografico`, desde la configuración del servidor, no desde la pantalla). El nuevo `getByDNI(dni, claveAcceso)` es un delegador fino, así que **hereda** esa limitación tal cual. Consecuencia y alcance en §14 «Situaciones sin escenario»; **MUST NOT** documentarse aquí, ni en el código, ni en las vistas, que el PIN tecleado se usa para firmar.

## Notas y supuestos que aplican a esta tarea (verbatim del `design.md` §14)

- **Situaciones sin escenario — y `DISPOSITIVO_*` además NO operativa con el mecanismo que se reutiliza.** `DISPOSITIVO_CON_PIN` y `DISPOSITIVO_SIN_PIN` quedan **especificadas y cableadas** por este diseño (su `<panel>` con el aviso RUI-…-CREADOR-004, el `<field>` del PIN, la validación VAL-…-003 y su inclusión en `FirmaDocumentoEntradaService.isFirmaEnServidor`), pero **MUST NOT** decirse que quedan «programadas»: no lo están del todo, y conviene decirlo sin rodeos.
  - **Lo que sí se hace:** se detecta la situación (CC-001), se muestra el aviso y el campo «PIN», y VAL-…-003 exige que no venga vacío.
  - **Lo que NO se hace:** ese PIN **nunca llega a la firma**. `CertificadoDigitalServiceImpl.getAlmacenClaveByDni(dni, claveAcceso)` solo usa `claveAcceso` en la rama `FICHERO_BD` / `CLASSPATH` / `SISTEMA_ARCHIVOS`; en `DISPOSITIVO_PKCS11` devuelve `new AlmacenClaveDispositivo(slot, alias)` —cuyo constructor no admite PIN— y **descarta** `claveAcceso`. Quien aporta el PIN es `EntornoCriptografico.getDispositivoCriptografico(slot)`, que lo lee de la configuración del servidor (`entornoCriptografico.*`). Por tanto, en `DISPOSITIVO_SIN_PIN` —la única situación de dispositivo que depende de la clave tecleada, porque la secretaría virtual **no** la custodia— la acción `PRESENTAR` **no puede firmar aunque el usuario rellene el campo**: el PIN se pide, se valida y se descarta. `DISPOSITIVO_CON_PIN` no depende de lo tecleado, pero su PIN sale igualmente de la configuración del servidor y no del certificado custodiado.
  - **El hueco NO se cierra en esta iniciativa, y MUST NOT intentarse aquí.** Cerrarlo exigiría cambiar `AlmacenClaveDispositivo` para que acepte el PIN y el camino de `EntornoCriptografico` / `DocumentoPdfImplIText` para que llegue al `KeyStore` PKCS#11: infraestructura criptográfica compartida, ajena a este trámite y **fuera del alcance**. Las guías de diseño obligan a **reutilizar** el mecanismo existente (`getAlmacenClaveByDni(dni, claveAcceso)`) en vez de inventar uno nuevo, y eso es exactamente lo que se hace, con su limitación incluida.
  - **Es la misma desviación consciente** que la iniciativa de referencia `.sdd/drafts/2026-09-01_11-21_firma-en-servidor` documentó en su **nota 13** («en `DISPOSITIVO_SIN_PIN` el PIN tecleado se descarta»). Aquí se **hereda entera**, sin agravarla ni repararla.
  - **Sin test, además, por el entorno:** las dos situaciones de dispositivo exigen un dispositivo criptográfico físico conectado y configurado en el servidor, que no existe en el entorno de pruebas (la propia especificación lo declara fuera de alcance). Lo mismo con `SIN_DNI` (no hay cuenta de profesor de demo sin documento de identidad) y con la firma real con AutoFirma.

## Instrucciones

- Acción de la fila §6: **Modificar**. Se toca **solo** este fichero.
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
