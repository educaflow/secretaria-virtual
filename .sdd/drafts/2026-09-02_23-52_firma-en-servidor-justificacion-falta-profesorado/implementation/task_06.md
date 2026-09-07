---
type: implementation-task
template: expediente
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-sistemas`
- `k-guice`
- `k-secure-coding`
- `k-code-quality`
- `k-i18n`

Crea `com.educaflow.subsystem.expedientes.services.internal.FirmaDocumentoEntradaService`, la firma en servidor del documento de entrada reutilizable por cualquier tipo de expediente.

## Nota del descomponedor (decisiones de esta descomposición)

Esta iniciativa es un **delta** sobre una versión existente y su tabla §6 lista, además de los ficheros del tipo de expediente, varios ficheros de **subsistema** (las «piezas compartidas» de §9.1 y las reglas de §10.1) que no corresponden a ningún bloque de la tabla de tareas del contrato. Decisión tomada: **una tarea por fichero de subsistema**, en el orden de los pasos del diseño, y **todas antes** de la tarea de la fase `RECEPCION`, que es la que las consume (inyecta el servicio y usa las reglas). Así cada fila de §6 queda cubierta por exactamente una tarea.

## Fichero de esta tarea — fila verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/services/internal/FirmaDocumentoEntradaService.java` | Crear | `k-sistemas`, `k-guice`, `k-secure-coding` | Especificado en §9.1 — firma en servidor del documento de entrada, reutilizable por cualquier tipo de expediente; declara la constante del nombre del campo de la clave |

**Tampoco hay fila de `FirmaController.java`: este delta NO lo toca.** El controlador se queda **exactamente como está**, sirviendo el camino de AutoFirma (`firmarDocumentoEntrada`, que la `<action-method>` de la vista sigue llamando). El motivo está razonado en §9.1 (apartado «`FirmaDocumentoEntradaService`») y en §14: unos delegadores de firma en servidor en el controlador se quedarían **sin ningún invocador** —el `triggerPresentar` usa el servicio, ningún `.java`/`.kt` de `tramites/` inyecta un controlador y no pueden llevar `@CallMethod`—, es decir, API pública muerta.

## Paso del diseño (verbatim del `design.md` §7)

### Paso 6 — `FirmaDocumentoEntradaService.java`

- Fichero: `src/main/java/com/educaflow/subsystem/expedientes/services/internal/FirmaDocumentoEntradaService.java`; FQCN `com.educaflow.subsystem.expedientes.services.internal.FirmaDocumentoEntradaService`.
- Especificación quirúrgica: **§9.1**, apartado «`FirmaDocumentoEntradaService`». **MUST NOT** duplicarla aquí.
- **Verificación:** compila; existen `isFirmaEnServidor(SituacionFirma)`, `firmarDocumentoEntradaEnServidor(...)` y la constante `CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA`; la detección de «clave incorrecta» y sus mensajes **no** se reimplementan aquí, se piden a `FirmaEnServidorHelper`; ninguna traza ni ningún mensaje de error contiene la clave ni un fragmento de ella; la clase **no** lleva ningún `@CallMethod`.

## Especificación quirúrgica (verbatim del `design.md` §9.1)

### 9.1 Piezas compartidas del subsistema que usan los `trigger*`

Son el **mecanismo reutilizable**: esta iniciativa las escribe (o las extrae de donde ya estaban) una sola vez y quedan disponibles para **cualquier** tipo de expediente que quiera firmar en el servidor su documento de entrada. Ninguna conoce este trámite.

Dos de ellas son **de-duplicación**, no funcionalidad nueva: `FirmaEnServidorHelper` extrae lo que hoy vive dentro de `TareaFirmaServiceImpl`, y `TareaFirmaServiceImpl` pasa a delegar en él. Sin ese par, la firma en servidor del documento de entrada y la regla del DSL serían la segunda y la tercera copia del mismo criterio y de los mismos literales (§14).


#### `FirmaDocumentoEntradaService` — `com.educaflow.subsystem.expedientes.services.internal.FirmaDocumentoEntradaService` (**nuevo**)

Es donde vive de verdad la firma en servidor del documento de entrada, reutilizable por **cualquier** tipo de expediente. Va en `..services.internal..` y no en `..controllers..` porque quien la invoca es el `trigger*` de un tipo de expediente —código de dominio—, y un `PhaseEventManagerImpl` no debe depender del paquete de controladores: hoy **ningún** `.java`/`.kt` de `tramites/` referencia `controllers`, y meter la primera dependencia invertiría las capas que describe `k-sistemas`. La guía «la firma en el servidor debe apoyarse en `FirmaController`» se cumple **en su intención** —que el mecanismo quede disponible para todos los tipos de expediente y no solo para este trámite— porque es aquí donde vive, en el subsistema de expedientes y sin conocer el trámite; el porqué de no ponerlo en el controlador está en el apartado siguiente y en §14.

- Clase normal de servicio interno. **No necesita binding**: Guice la construye con un **JIT binding** (*just-in-time*), porque es una clase **concreta**, no abstracta y con **constructor público sin argumentos** (el implícito); al construirla, Guice ejecuta `injectMembers` y rellena el `@Inject` de campo. Por eso el delta **no** crea ni toca ningún módulo Guice —el subsistema de expedientes hoy **no tiene** ningún `module/*Module.java`— y **MUST NOT** añadirse por ella ninguna fila a §6 ni ningún paso a §7 (`k-guice`).
  ⚠️ **No** se cita `AlmacenClaveResolver` como precedente de esto: esa clase **sí** tiene binding explícito (`bind(AlmacenClaveResolver.class);` en `com.educaflow.subsystem.criptografia.module.CriptografiaModule`).
  Si al implementar apareciera un `Guice/MissingConstructor` (p. ej. porque se le acabara añadiendo un constructor con argumentos), la solución **MUST** ser declarar el binding explícito en un módulo Guice del subsistema (creándolo como `com.educaflow.subsystem.expedientes.module.ExpedientesModule` y registrándolo donde el proyecto registre los demás) y añadir su fila a §6 y su paso a §7, nunca quitarle el `@Inject`. Es un cambio **fuera** de la carpeta de diseño: queda anotado aquí para `/sdd-implementer`.
- **MUST NOT** llevar ningún `@CallMethod`: ninguna vista la invoca.

Dependencias a inyectar:

- `@Inject AlmacenClaveResolver almacenClaveResolver` — para obtener el almacén de claves del DNI con la clave que corresponda.

Constantes de clase:

- `public static final String CAMPO_CLAVE_FIRMA_DOCUMENTO_ENTRADA = "claveFirmaDocumentoEntrada";` — el nombre del campo transitorio de la clave, declarado **una sola vez** para todo el subsistema (§4.1). Lo usa `ExpedienteController` para devolverlo vacío (RUI-011) y lo cita el `domains.xml` de cada tipo que quiera firma en servidor.

Métodos:

1. `public boolean isFirmaEnServidor(SituacionFirma situacionFirma)`
   - Devuelve `true` si `situacionFirma` es una de `DISPOSITIVO_CON_PIN`, `DISPOSITIVO_SIN_PIN`, `FICHERO_CON_CLAVE`, `FICHERO_SIN_CLAVE`; `false` para `SIN_DNI` y `SIN_CERTIFICADO` y para `null`.
   - Es **la** definición de «corresponde firmar en el servidor» que usan el trigger y las reglas del validador; **MUST NOT** duplicarse esa lista en ningún otro sitio.

2. `public DocumentoPdf firmarDocumentoEntradaEnServidor(String dni, SituacionFirma situacionFirma, String claveFirma, DocumentoPdf documentoOriginal, CampoFirma campoFirma) throws BusinessException`
   - Acciones, **en este orden**:
     1. `ERROR_NEGOCIO` **no**: guarda de código. Si `isFirmaEnServidor(situacionFirma) == false`, o si `documentoOriginal` es `null`, lanza `IllegalStateException` — es un error de programación del llamante, no algo que el usuario pueda corregir.
     2. `SERVICIO(AlmacenClaveResolver.getByDNI(dni, claveFirma))` — obtiene el almacén de claves. **MUST** pedirse **dentro de la propia acción**, con el DNI y la clave de esta invocación: cada llamada firma un documento con la clave que le corresponde, así que el almacén no se cachea, ni se guarda en un campo de la clase, ni se recibe como parámetro desde fuera.
        El motivo **NO** es el ámbito Guice del servicio —se resuelve por **JIT binding** y, al no llevar `@Singleton`, Guice crea una instancia nueva por punto de inyección—, sino el **dato**: el `AlmacenClave` depende del DNI y de la clave **de esa invocación**, y la clave es un dato de un solo uso (§4.1) que **MUST NOT** sobrevivir a la llamada. La regla vale **con independencia** del ámbito Guice que la clase acabe teniendo.
        Nota: `AlmacenClaveFichero` **sí** es reutilizable —lee el certificado a un `byte[]` en el constructor y su `getFileCertificate()` devuelve un `InputStream` nuevo en cada llamada—, así que **MUST NOT** justificarse esta decisión con un supuesto stream que se consuma al firmar: el motivo es la clave, no el stream.
     3. `SERVICIO(DocumentoPdf.firmar(almacenClave, campoFirma))` — devuelve el PDF firmado.
   - Tratamiento del fallo (RN-002): captura `RuntimeException`, escribe la traza en el log con el DNI **ofuscado** y **NUNCA** con la clave ni con un fragmento de ella, y lanza `BusinessException` con el mensaje `«No se ha podido firmar la solicitud: <motivo>»`, donde `<motivo>` es **exactamente** `FirmaEnServidorHelper.motivoFirmaFallida(ex, situacionFirma)`.
     - **MUST NOT** reimplementarse aquí ni el recorrido de la cadena de causas ni los cuatro textos de motivo: son los del helper (apartado «`FirmaEnServidorHelper`»), que es la misma pieza en la que delega `TareaFirmaServiceImpl`.
   - El texto técnico de la excepción **MUST NOT** llegar al usuario: se queda en el log.
   - **MUST** lanzarse `com.educaflow.base.infrastructure.validation.messages.BusinessException` (construida con `BusinessMessages.single(...)`) y no otra: es la única que el `Tramitador` hace `detach` y propaga como mensaje de usuario, y de eso depende que la acción se cancele entera sin dejar rastro.
   - Es un método **de negocio**: no lo invoca ninguna vista, lo invoca el `trigger*` del tipo de expediente.

#### `FirmaController` — `com.educaflow.subsystem.expedientes.controllers.FirmaController` (**NO se toca**)

**MUST NOT** modificarse en esta iniciativa: no tiene fila en §6 ni paso en §7. Sigue exactamente como está, sirviendo el camino de AutoFirma con su `@CallMethod firmarDocumentoEntrada(...)`, que la `<action-method>` de `fases/recepcion/views.xml` sigue llamando igual.

Motivo, y cómo se concilia con la guía de diseño («la firma en el servidor debe apoyarse en `FirmaController` … de forma que el mecanismo quede disponible para todos los tipos de expediente»): se llegó a plantear añadirle dos **delegadores finos** (`isFirmaEnServidor` y `firmarDocumentoEntradaEnServidor`) para apoyarse en él **literalmente**, y se **descartó** porque quedarían **sin ningún invocador** —API pública muerta—:

- el `triggerPresentar` inyecta y llama al **servicio**, no al controlador (§9.2), y no puede hacer otra cosa sin invertir las capas (`k-sistemas`): hoy **ningún** `.java`/`.kt` de `tramites/` referencia el paquete `controllers`;
- tampoco podrían llevar `@CallMethod` (un endpoint de firma sería invocable desde cualquier estado y permitiría re-firmar saltándose la máquina de estados, `k-secure-coding`), así que ninguna vista los alcanzaría;
- y no queda ningún tercer llamador posible.

**La guía se cumple en su intención, que es que el mecanismo sea reutilizable por todos los tipos de expediente y no exclusivo de este trámite** — y lo es: vive en `FirmaDocumentoEntradaService`, en el subsistema de expedientes, no conoce este trámite y cualquier otro tipo lo usa inyectándolo en su `PhaseEventManagerImpl`. Lo único que cambia respecto a la letra de la guía es **la clase** en la que se apoya (un servicio del subsistema en vez del controlador del subsistema), y ese cambio es lo que permite cumplir a la vez la especificación (la solicitud firmada la produce la propia acción `PRESENTAR`, RN-001/CC-002) y las capas. Queda recogido en §14 «Notas y supuestos».

**Nota para `/sdd-implementer`:** si en el futuro se decidiera exponer también la firma en servidor desde `FirmaController`, MUST hacerse con un invocador real, no «por si acaso».

## Reparto de reglas — filas que aplican (verbatim del `design.md` §11)

| Regla | Capa | Dónde |
|---|---|---|
| RN-001 — el servidor firma y descarta lo que envió el formulario | `trigger*` | `triggerPresentar` acciones 2–3 |
| RN-002 — si la firma falla se cancela la acción entera | `trigger*` + pieza compartida | `BusinessException` de `FirmaDocumentoEntradaService.firmarDocumentoEntradaEnServidor`, cuyo motivo lo da `FirmaEnServidorHelper.motivoFirmaFallida` (§9.1); el `Tramitador` hace `detach` y no persiste nada |

## Notas y supuestos que aplican a esta tarea (verbatim del `design.md` §14)

- **Dónde vive la firma en el servidor (guías de diseño vs. especificación vs. capas).** Las guías piden apoyarse en `FirmaController`; la especificación exige que la solicitud firmada la produzca **la propia acción** `PRESENTAR` y que se **descarte** lo que el formulario hubiera enviado (RN-001, CC-002, y las guardas nuevas de VAL-…-006/007); y `k-sistemas` pide que el dominio no dependa del paquete de controladores. Las tres se han conciliado así (con **una desviación consciente de la letra** de la guía, detallada al final de esta nota):
  la lógica reutilizable vive en `FirmaDocumentoEntradaService` (`..expedientes.services.internal..`, §9.1), y quien la invoca es el `triggerPresentar` del tipo (§9.2), que es el único punto donde se puede pisar lo que envió el cliente, **inyectando el servicio**.
  Motivo de esa última pieza: hoy **ningún** `.java`/`.kt` de `tramites/` referencia el paquete `controllers`, y estrenar esa dependencia invertiría las capas aunque los tests de arquitectura no la prohíban (`..expedientes..` y `..tramites..` son paquetes **exentos**, `agent_docs/architecture-rules.md`, §Convenciones).
  **Desviación consciente de la letra de la guía, y por qué:** la guía nombra `FirmaController`, y **`FirmaController` NO se toca** en esta iniciativa (§6, §9.1). Se descartó añadirle dos delegadores finos (`isFirmaEnServidor`, `firmarDocumentoEntradaEnServidor`) porque **no tendrían ningún invocador**: el trigger usa el servicio, no pueden llevar `@CallMethod` —un endpoint de firma sería invocable desde cualquier estado y permitiría re-firmar saltándose la máquina de estados— y no hay ningún tercer llamador. Serían API pública muerta, que es exactamente lo que `k-code-quality` prohíbe.
  Lo que la guía **persigue** —«que el mecanismo quede disponible para todos los tipos de expediente y no solo para este trámite»— **sí se cumple íntegramente**: el mecanismo vive en el subsistema de expedientes, no conoce este trámite y cualquier otro tipo lo usa inyectando `FirmaDocumentoEntradaService` en su `PhaseEventManagerImpl`. Lo único que cambia es la **clase de apoyo** (un servicio del subsistema en vez del controlador del subsistema).
  El método del servicio tampoco lleva `@CallMethod`, por el mismo motivo de máquina de estados.

## Instrucciones

- Acción de la fila §6: **Crear**. Se toca **solo** este fichero: `FirmaController.java` **NO** se toca en esta iniciativa y **MUST NOT** aparecer en el diff.
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
