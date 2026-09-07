---
type: implementation-task
template: expediente
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- `k-validaciones`
- `k-secure-coding`
- `k-code-quality`
- `k-i18n`

Crea `src/main/java/com/educaflow/subsystem/expedientes/services/validation/rules/FirmaDocumentoEntradaRules.kt` con las cuatro reglas nuevas del DSL de validación.

## Nota del descomponedor (decisiones de esta descomposición)

Esta iniciativa es un **delta** sobre una versión existente y su tabla §6 lista, además de los ficheros del tipo de expediente, varios ficheros de **subsistema** (las «piezas compartidas» de §9.1 y las reglas de §10.1) que no corresponden a ningún bloque de la tabla de tareas del contrato. Decisión tomada: **una tarea por fichero de subsistema**, en el orden de los pasos del diseño, y **todas antes** de la tarea de la fase `RECEPCION`, que es la que las consume (inyecta el servicio y usa las reglas). Así cada fila de §6 queda cubierta por exactamente una tarea.

## Fichero de esta tarea — fila verbatim de la tabla §6 del `design.md`

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/services/validation/rules/FirmaDocumentoEntradaRules.kt` | Crear | `k-validaciones`, `k-secure-coding` | Especificado en §10.1 — las cuatro reglas nuevas del DSL de validación |

## Paso del diseño (verbatim del `design.md` §7)

### Paso 8 — `FirmaDocumentoEntradaRules.kt`

- Fichero: `src/main/java/com/educaflow/subsystem/expedientes/services/validation/rules/FirmaDocumentoEntradaRules.kt`; paquete `com.educaflow.subsystem.expedientes.services.validation.rules`.
- Especificación quirúrgica: **§10.1**. Cada regla implementa `com.educaflow.base.infrastructure.validation.engine.ValidationRule`.
- **Verificación:** compila; existen las **cuatro** reglas con los constructores y los mensajes literales de §10.1; las reglas 1 y 2 llevan su parámetro de mensaje con el **valor por defecto** literal de la especificación; la regla 4 **no** reimplementa la comprobación de la clave, se la pide a `FirmaEnServidorHelper`; ninguna escribe la clave en el log ni la devuelve en un mensaje.

## Especificación quirúrgica (verbatim del `design.md` §10.1)

### 10.1 Reglas nuevas del DSL de validación (subsistema de expedientes)

Fichero nuevo `src/main/java/com/educaflow/subsystem/expedientes/services/validation/rules/FirmaDocumentoEntradaRules.kt`, paquete `com.educaflow.subsystem.expedientes.services.validation.rules`.

Por qué aquí y no en `com.educaflow.base.infrastructure.validation.rules`: estas reglas necesitan el enum `SituacionFirma` (subsistema de firmas) y el `CertificadoDigitalService` (subsistema de criptografía), y `base.infrastructure` **MUST NOT** depender de ningún `subsystem` (regla de arquitectura C2, que declara expresamente que no admite exenciones).

Las cuatro reglas reciben como argumentos los **getters** de los campos que necesitan —igual que `FirmaPdf(documentoOriginalField, dniField)`—, así que no dependen de ninguna entidad concreta y sirven para **cualquier** tipo de expediente. Un getter pasado como argumento **no** entra en el `AllowProperties` del evento (solo entran los `field(...)` de primer nivel), de modo que nombrarlos aquí **no** los hace escribibles por el cliente.

Todas se cuelgan del `field(...)` del campo de la **clave**, así que reciben en `value` la clave tecleada. Todas devuelven `null` cuando no tienen nada que decir.

`FieldValidationRules` antepone **siempre** al mensaje la etiqueta del `field(...)` del que cuelga la regla, y el DSL no permite colgar una regla fuera de un `field(...)`: por eso el `title` de ese campo es neutro (`Firma de la solicitud`, §4.1) y no nombra la clave, que es lo que hace legibles los mensajes de las reglas 1 y 2, que no hablan de ella (§14).

1. `data class DocumentoIdentidadParaFirmarRequerido(val situacionFirmaField: KCallable<*>, val mensaje: String = "No es posible firmar la solicitud porque su usuario no tiene un documento de identidad. Póngase en contacto con el administrador.") : ValidationRule` — **VAL-PENDIENTE_PRESENTACION-PRESENTAR-001**
   - Si `situacionFirmaField.call(bean) == SituacionFirma.SIN_DNI` → `BusinessMessages.single(I18n.get(mensaje))`.
   - En cualquier otra situación, `null`.
   - El segundo parámetro es **opcional y su valor por defecto es el literal exacto que fija la especificación**, así que este trámite lo construye con un solo argumento y no cambia ni una letra de su mensaje; otro tipo de expediente que reutilice la regla puede pasar el suyo sin arrastrar el vocabulario («la solicitud») de este trámite (§14).

2. `data class FirmanteDocumentoEntradaEsUsuarioAutenticado(val dniFirmaField: KCallable<*>, val mensaje: String = "Solo puede presentar la solicitud la persona que la firma") : ValidationRule` — **VAL-PENDIENTE_PRESENTACION-PRESENTAR-008**
   - Obtiene el DNI del expediente con `dniFirmaField.call(bean)` y el del usuario autenticado con `AuthUtils.getUser()`.
   - Si el DNI del expediente es nulo o en blanco → `null` (de eso se ocupa la regla 1).
   - Si no hay usuario autenticado, o su DNI no coincide (comparación exacta) con el del expediente → `BusinessMessages.single(I18n.get(mensaje))`.
   - Mismo criterio que la regla 1: parámetro opcional con el literal de la especificación como valor por defecto.
   - Es **nueva y necesaria**: hasta ahora firmar exigía tener el certificado en el propio equipo, así que nadie podía firmar por otro aunque llegase a lanzar la acción; con la firma en el servidor esa barrera desaparece porque la firma se produce sola a partir del DNI guardado en el expediente.
   - **MUST NOT** escribirse el DNI completo en ningún mensaje ni en ningún log.

3. `data class ClaveFirmaDocumentoEntradaRequerida(val situacionFirmaField: KCallable<*>) : ValidationRule` — **VAL-…-002** y **VAL-…-003**
   - Si la clave (`value`) no es nula ni está en blanco → `null`.
   - `SituacionFirma.FICHERO_SIN_CLAVE` → `BusinessMessages.single(I18n.get("La contraseña es obligatoria"))`.
   - `SituacionFirma.DISPOSITIVO_SIN_PIN` → `BusinessMessages.single(I18n.get("El PIN es obligatorio"))`.
   - Cualquier otra situación → `null`: cuando la secretaría virtual custodia la clave, o cuando se firma en el equipo del profesor, no hay nada que teclear.

4. `data class ClaveFirmaDocumentoEntradaCorrecta(val situacionFirmaField: KCallable<*>, val dniFirmaField: KCallable<*>) : ValidationRule` — **VAL-…-004** y **VAL-…-005**
   - **La regla no decide nada por su cuenta: delega entera en `FirmaEnServidorHelper`** (§9.1), que es la misma pieza en la que delega `TareaFirmaServiceImpl`. Su cuerpo es, literalmente:
     - si `FirmaEnServidorHelper.isClaveCertificadoIncorrecta(dniFirmaField.call(bean), value, situacionFirmaField.call(bean))` → `BusinessMessages.single(FirmaEnServidorHelper.mensajeClaveCertificadoIncorrecta(situacionFirmaField.call(bean)))`; si no, `null`.
   - **MUST NOT** reimplementarse aquí el `instanceof AlmacenClaveFichero` + `isPasswordValid()`, ni los dos mensajes, ni el tratamiento del certificado ilegible: todo eso **es** el helper, y duplicarlo devolvería el diseño a las tres copias que esta iniciativa elimina.
   - Lo que el helper garantiza y esta regla hereda: solo actúa con `FICHERO_SIN_CLAVE` y `FICHERO_CON_CLAVE` —con un certificado en un dispositivo criptográfico la única forma de saber si el PIN es correcto es intentar abrirlo, y los intentos fallidos **bloquean la tarjeta**, así que ahí el error lo da la propia firma (RN-002)—; un certificado que no se puede leer **NO** cuenta como clave incorrecta (se registra un `warn` y se deja pasar); cualquier `RuntimeException` se degrada a «no incorrecta»; y la clave **MUST NOT** aparecer en ningún log.
   - Único cuidado propio de la regla: con `FICHERO_SIN_CLAVE` y la clave **vacía** devuelve `null` sin llamar al helper, porque de eso se ocupa la regla 3 y así un solo intento no produce dos mensajes.
   - Mensajes resultantes: `FICHERO_SIN_CLAVE` → `«La contraseña indicada no es correcta»`; `FICHERO_CON_CLAVE` → `«La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador»`. La distinción no es cosmética: el primero lo corrige el profesor, el segundo no.


## Reparto de reglas — filas que aplican (verbatim del `design.md` §11)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<string>`/`<enum>`; aquí, `transient`, `password` y el cuerpo del campo derivado |
| Obligatoriedad de un campo **en un evento** | **DSL del validador** | `+Required()` / regla propia en la pareja (estado, evento) |
| Formato, rango, comparación entre campos, tipo/tamaño de fichero, firma | **DSL del validador** | `Pattern`, `MinValue`/`MaxValue`, `GreaterThan`, `FileType`/`FileMaxSize`, `FirmaPdf` |
| Obligatoriedad **condicional** | **DSL del validador** | `ifValueIn(...) { … }` o la condición interna de la regla propia |
| Qué campos puede dictar el cliente en un evento | **DSL del validador** | el conjunto de `field(...)` de esa pareja (§10.2) |
| Efectos: generar PDF, firmar, registrar, transicionar, limpiar | **`trigger*`** del `PhaseEventManagerImpl` | lista de acciones de §9.2 |
| Inicialización del expediente | **`triggerInitialEvent`** | §8 |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf`, `<help>`, `prompt` — **solo UX, NUNCA defensa** |

Reglas duras: **MUST NOT** usarse `required="true"` en el `domains.xml`; **MUST NOT** validarse datos de usuario en un `trigger*`; **MUST NOT** ponerse lógica de negocio en el validador; **MUST NOT** tratarse una regla de vista como si fuera una validación.

| Regla | Capa | Dónde |
|---|---|---|
| VAL-…-001 — el profesor tiene documento de identidad | DSL del validador | `DocumentoIdentidadParaFirmarRequerido` |
| VAL-…-002 — la contraseña está rellena | DSL del validador | `ClaveFirmaDocumentoEntradaRequerida` (rama `FICHERO_SIN_CLAVE`) |
| VAL-…-003 — el PIN está relleno | DSL del validador | `ClaveFirmaDocumentoEntradaRequerida` (rama `DISPOSITIVO_SIN_PIN`) |
| VAL-…-004 — la contraseña tecleada abre el certificado | DSL del validador | `ClaveFirmaDocumentoEntradaCorrecta` (rama `FICHERO_SIN_CLAVE`) |
| VAL-…-005 — la clave custodiada abre el certificado | DSL del validador | `ClaveFirmaDocumentoEntradaCorrecta` (rama `FICHERO_CON_CLAVE`) |
| VAL-…-006 — la solicitud está firmada | DSL del validador | `field(pdfSolicitudFirmado) { ifValueIn(SIN_CERTIFICADO) { Required() } }` |
| VAL-…-007 — la firma de la solicitud es válida | DSL del validador | `field(pdfSolicitudFirmado) { ifValueIn(SIN_CERTIFICADO) { FirmaPdf(...) } }` |
| VAL-…-008 — quien lanza la acción es quien firma | DSL del validador | `FirmanteDocumentoEntradaEsUsuarioAutenticado` |

## Notas y supuestos que aplican a esta tarea (verbatim del `design.md` §14)

- **Vocabulario del trámite en unas reglas compartidas: mensaje parametrizado con el literal de la spec por defecto.** Las cuatro reglas viven en el subsistema y sirven para cualquier tipo de expediente, pero los textos que la especificación fija para VAL-…-001 y VAL-…-008 hablan de «la solicitud», que es vocabulario de **este** trámite. Se resuelve con un parámetro opcional de mensaje en esas dos reglas cuyo **valor por defecto es el literal exacto de la especificación** (§10.1): este trámite las construye con un solo argumento y no cambia ni una letra, y otro tipo de expediente que las reutilice pasa el suyo. Las reglas 3 y 4 no lo necesitan: sus textos («La contraseña es obligatoria», «El PIN es obligatorio», «La contraseña indicada no es correcta», «La clave guardada de su certificado digital no es correcta…») no nombran ningún documento, y además son **los mismos** que ya usa la tarea de firma, así que salen del helper compartido.
  Excepción consciente: el prefijo de RN-002 («No se ha podido firmar **la solicitud**: …») se queda tal cual en `FirmaDocumentoEntradaService`, porque la especificación lo fija así y parametrizarlo obligaría a cambiar la firma del método; la parte reutilizable de ese mensaje —el motivo— sí es compartida (`FirmaEnServidorHelper.motivoFirmaFallida`).
- **La etiqueta que el footer antepone a cada mensaje: por eso el `title` del campo de la clave es neutro.** `FieldValidationRules` resuelve la etiqueta con el `title` del `field(...)` del que cuelga la regla y **la antepone siempre**; el visor pinta `«<etiqueta>: <mensaje>»`. Como el DSL no deja colgar una regla fuera de un `field(...)`, las cuatro reglas cuelgan del campo de la clave, y con el `title` anterior («Clave del certificado digital») los mensajes de VAL-…-001 y VAL-…-008 —que no hablan de la clave— habrían salido precedidos de una etiqueta que no viene a cuento. Con el `title` neutro `Firma de la solicitud` (§4.1), lo que ve el usuario es:
  - VAL-…-001 → «Firma de la solicitud: No es posible firmar la solicitud porque su usuario no tiene un documento de identidad. Póngase en contacto con el administrador.»
  - VAL-…-002 → «Firma de la solicitud: La contraseña es obligatoria» (ESC-004)
  - VAL-…-003 → «Firma de la solicitud: El PIN es obligatorio»
  - VAL-…-004 → «Firma de la solicitud: La contraseña indicada no es correcta» (ESC-003)
  - VAL-…-005 → «Firma de la solicitud: La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador» (ESC-010)
  - VAL-…-008 → «Firma de la solicitud: Solo puede presentar la solicitud la persona que la firma»
  El cambio **no toca ninguna pantalla**: la vista sobrescribe el `title` por «Contraseña» y por «PIN» en el panel de cada situación, que es lo que los escenarios comprueban.

## Instrucciones

- Acción de la fila §6: **Crear**. Se toca **solo** este fichero.
- Las cuatro reglas viven en el subsistema y **MUST NOT** conocer ningún trámite: reciben los getters como argumentos.
- **La especificación de arriba es contrato fijo y la superficie es cerrada**: MUST NOT crearse ningún método, clase, campo, constante ni acción que esa especificación no liste, y MUST NOT modificarse ningún fichero que no sea el de esta tarea.
