---
type: implementation-task
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/service/CertificadoDigitalService.java` | Modificar | k-sistemas (servicios.md) | Añade `getAlmacenClaveByDni(dni, claveAcceso)` y su `validate…` |
| `src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` | Modificar | k-sistemas (servicios.md) | Implementa el nuevo overload; el de un argumento pasa a delegar en él |

Las dos son `Acción: Modificar`: las clases **ya existen**. Se edita la clase existente añadiendo/cambiando
**solo** el delta que el diseño declara y **conservando** todo lo demás (métodos, campos e imports
preexistentes).

## Texto del diseño (verbatim)

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

### Frontera de confianza — AllowProperties por acción

### `CertificadoDigitalServiceImpl.getAlmacenClaveByDni(String, String)`

**No declara `allowProperties…`**: recibe **escalares** (`String dni`, `String claveAcceso`), no la entidad
construida desde el request, y no la invoca ningún `@CallMethod` (la llama el servicio de firmas). No hay mapa
del cliente que filtrar (`k-sistemas/servicios.md` §"`allowPropertiesXxx` y campos `servidor`").

### Documentación referenciada

El detalle de **cuál clave se usa** al construir el `AlmacenClave` está en `design/rules/R-TareaFirma-001.md`,
§«Cuál clave se usa». **MUST** leerse antes de implementar el overload.

### Notas y supuestos aplicables

2. **Por qué hace falta ampliar el subsistema de criptografía.** `design-guidelines.md` dice que el almacén se
   obtiene con `getAlmacenClaveByDni(String dni)`. Ese método, tal cual, **no puede** servir a las situaciones
   `FICHERO_SIN_CLAVE` (las de casi todos los escenarios): construye `AlmacenClaveFichero` con la contraseña
   guardada y su constructor rechaza el `null`. El diseño respeta la guía en lo esencial —el almacén se sigue
   pidiendo a ese mismo método de ese mismo servicio— y añade el **overload** mínimo que acepta la clave
   tecleada, dejando intacto el de un argumento y a sus llamadores.

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
