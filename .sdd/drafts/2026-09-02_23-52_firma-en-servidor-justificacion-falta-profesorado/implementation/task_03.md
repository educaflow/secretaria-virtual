---
type: implementation-task
template: expediente
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-sistemas`
- `k-code-quality`
- `k-secure-coding`
- `k-i18n`

Crea `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper`, la única fuente de verdad de «la clave no es correcta» y de sus mensajes.

## Nota del descomponedor (decisiones de esta descomposición)

Esta iniciativa es un **delta** sobre una versión existente y su tabla §6 lista, además de los ficheros del tipo de expediente, varios ficheros de **subsistema** (las «piezas compartidas» de §9.1 y las reglas de §10.1) que no corresponden a ningún bloque de la tabla de tareas del contrato. Decisión tomada: **una tarea por fichero de subsistema**, en el orden de los pasos del diseño, y **todas antes** de la tarea de la fase `RECEPCION`, que es la que las consume (inyecta el servicio y usa las reglas). Así cada fila de §6 queda cubierta por exactamente una tarea.

## Fichero de esta tarea — fila verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/firmas/util/FirmaEnServidorHelper.java` | Crear | `k-sistemas`, `k-code-quality`, `k-secure-coding` | Especificado en §9.1 — **única** fuente de verdad de «la clave no es correcta» y de sus mensajes |

## Paso del diseño (verbatim del `design.md` §7)

### Paso 3 — `FirmaEnServidorHelper.java`

- Fichero: `src/main/java/com/educaflow/subsystem/firmas/util/FirmaEnServidorHelper.java`; FQCN `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper`.
- Especificación quirúrgica: **§9.1**, apartado «`FirmaEnServidorHelper`». **MUST NOT** duplicarla aquí.
- **Verificación:** compila; existen los **cuatro** métodos públicos de §9.1 con los mensajes **literales** que hoy tiene `TareaFirmaServiceImpl`; ningún método propaga excepción; ninguna traza contiene la clave ni un fragmento de ella; el DNI solo aparece enmascarado.

## Especificación quirúrgica (verbatim del `design.md` §9.1)

### 9.1 Piezas compartidas del subsistema que usan los `trigger*`

Son el **mecanismo reutilizable**: esta iniciativa las escribe (o las extrae de donde ya estaban) una sola vez y quedan disponibles para **cualquier** tipo de expediente que quiera firmar en el servidor su documento de entrada. Ninguna conoce este trámite.

Dos de ellas son **de-duplicación**, no funcionalidad nueva: `FirmaEnServidorHelper` extrae lo que hoy vive dentro de `TareaFirmaServiceImpl`, y `TareaFirmaServiceImpl` pasa a delegar en él. Sin ese par, la firma en servidor del documento de entrada y la regla del DSL serían la segunda y la tercera copia del mismo criterio y de los mismos literales (§14).


#### `FirmaEnServidorHelper` — `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper` (**nuevo**)

Es la pieza que **de-duplica** el criterio de «la clave no es correcta» y los mensajes que lo explican. Hoy ese criterio vive **solo** dentro de `TareaFirmaServiceImpl`; sin esta clase, esta iniciativa lo copiaría dos veces más (en la firma en servidor del documento de entrada y en la regla del DSL) y quedarían **tres** copias del mismo `switch` y de los mismos literales. Las guías de diseño lo prohíben expresamente («MUST reutilizarse ese mecanismo en vez de inventar uno nuevo»).

- Clase `final`, con constructor privado y **solo métodos `static`**: no tiene estado y la construyen tanto un `ModelService` como una regla del DSL (que no pasa por Guice), así que **MUST NOT** depender de inyección.
- Vive en `..firmas.util..`, junto a `SituacionFirmaBuilder`, porque razona sobre `SituacionFirma` y sobre el certificado digital, no sobre expedientes. `..expedientes..` puede depender de `..firmas..` (ya lo hace `PhaseEventManagerImpl` con `TareaFirmaService`), y así **ningún** subsistema pasa a depender de expedientes.
- **MUST NOT** escribir la clave —ni entera ni truncada— en ningún log ni en ningún mensaje; el DNI solo aparece enmascarado (`SituacionFirmaBuilder.enmascararDni`).

Constantes de clase:

- `private static final int LIMITE_CAUSAS_A_RECORRER = 20;` — profundidad máxima al recorrer la cadena de causas de un fallo de firma, para no colgarse con un ciclo. Es **la** declaración de esa constante: **MUST** desaparecer de `TareaFirmaServiceImpl` y **MUST NOT** reaparecer en ninguna otra clase.

Métodos públicos (los cuatro conservan **literalmente** el comportamiento y los textos que hoy tiene `TareaFirmaServiceImpl`):

1. `public static boolean isClaveIncorrecta(Throwable ex)`
   - Recorre la cadena de causas, acotada a `LIMITE_CAUSAS_A_RECORRER` niveles, y devuelve `true` si aparece `java.security.UnrecoverableKeyException` o `javax.security.auth.login.LoginException`. **MUST NOT** decidirse por el texto del mensaje: tanto `CriptografiaUtil.getKeyStore` como `DocumentoPdf.firmar` envuelven el fallo original.

2. `public static String motivoFirmaFallida(RuntimeException ex, SituacionFirma situacionFirma)`
   - Motivo de negocio que se le enseña a quien firma. Si `isClaveIncorrecta(ex) == false` o `situacionFirma == null` → `«ha fallado la firma en el servidor. Póngase en contacto con el administrador»`. Si no, `switch` **exhaustivo y sin `default`**: `DISPOSITIVO_SIN_PIN` → `«el PIN indicado no es correcto»`; `FICHERO_SIN_CLAVE` → `«la contraseña indicada no es correcta»`; `DISPOSITIVO_CON_PIN`, `FICHERO_CON_CLAVE` → `«la clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador»`; `SIN_DNI`, `SIN_CERTIFICADO` → el motivo genérico.
   - Todos los textos pasan por `I18n.get`.

3. `public static boolean isClaveCertificadoIncorrecta(String dni, String clave, SituacionFirma situacionFirma)`
   - Devuelve `false` salvo con `FICHERO_SIN_CLAVE` y `FICHERO_CON_CLAVE`: en un dispositivo criptográfico la única forma de saber si el PIN es correcto es intentar abrirlo, y los intentos fallidos **bloquean la tarjeta**.
   - Resuelve el `CertificadoDigitalService` con `Beans.get(ModelServiceFactory.class).resolve(CertificadoDigital.class)` —es `static` y también la usa una regla del DSL, que Guice no construye—, pide `getAlmacenClaveByDni(dni, clave)` y, **si es un `AlmacenClaveFichero`**, devuelve `isPasswordValid() == false`.
   - Un certificado que **no se puede leer** (fichero corrupto, ruta que ya no existe, blob ilegible) **NO** es una clave incorrecta: cualquier `RuntimeException` se captura, se registra un `warn` con el DNI enmascarado y se devuelve `false`, para que sea la firma quien lo trate con su motivo genérico y no se acuse a nadie de haber tecleado mal la contraseña.

4. `public static String mensajeClaveCertificadoIncorrecta(SituacionFirma situacionFirma)`
   - `FICHERO_CON_CLAVE` → `«La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador»`; cualquier otra → `«La contraseña indicada no es correcta»`. La distinción no es cosmética: el primero no lo puede corregir quien firma.

## Notas y supuestos que aplican a esta tarea (verbatim del `design.md` §14)

- **Una sola fuente de verdad para «la clave no es correcta» (`FirmaEnServidorHelper`).** El criterio (recorrer la cadena de causas buscando `UnrecoverableKeyException`/`LoginException`, con un límite de niveles), la comprobación previa con `isPasswordValid()` solo en certificados en fichero y los textos que explican el fallo ya existían **dentro** de `TareaFirmaServiceImpl`. Copiarlos aquí habría dejado **tres** copias (la tarea de firma, la firma del documento de entrada y la regla del DSL) del mismo `switch` y de los mismos literales, justo lo que las guías prohíben («MUST reutilizarse ese mecanismo en vez de inventar uno nuevo»).
  Por eso esta iniciativa **extrae** esa lógica a `com.educaflow.subsystem.firmas.util.FirmaEnServidorHelper` y **refactoriza `TareaFirmaServiceImpl` para que delegue en ella** (§9.1): es un cambio en código ya en producción, pero sin cambio de comportamiento ni de mensajes, y es la única forma de que la de-duplicación sea real y no una copia más. La única diferencia observable es una línea de `warn` que pasa a llevar el DNI enmascarado en vez del `id` de la tarea.

## Instrucciones

- Acción de la fila §6: **Crear**. Se toca **solo** este fichero: la delegación de `TareaFirmaServiceImpl` es la tarea siguiente.
- Los cuatro métodos públicos conservan **literalmente** el comportamiento y los textos que hoy tiene `TareaFirmaServiceImpl`: léelos del código real antes de escribirlos y **MUST NOT** reescribirlos ni «mejorarlos».
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
