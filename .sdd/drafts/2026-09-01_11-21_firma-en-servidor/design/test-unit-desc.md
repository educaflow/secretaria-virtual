# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Aserciones con `org.junit.jupiter.api.Assertions` (`assertThrows`, `assertEquals`, `assertSame`, `assertNull`, `assertInstanceOf`…), **no** AssertJ, igual que `CorreoServiceImplTest` y `CertificadoDigitalServiceImplTest`.
- Los `*ServiceImpl` se instancian con su constructor real `(Class<T> model, Repository<T> repository)` y el repositorio mockeado; los campos `@Inject` (p. ej. `modelServiceFactory`) se inyectan por **reflexión** en el `@BeforeEach`, tal y como ya hace `CorreoServiceImplTest` con `mailSender`.
- DNI válido de referencia en los tests: `85432016B` (el de la spec y el de `CertificadoDigitalServiceImplTest`). DNI inválido: `12345678A` (letra de control incorrecta).
- `BusinessMessages.throwIfInvalid()` lanza `jakarta.validation.ValidationException` con el `toString()` de los mensajes; por eso los tests de acción esperan `ValidationException` y comprueban el **texto del mensaje** con `contains` del literal exacto de la regla, mientras que los tests de `validate*` comprueban el `Optional<BusinessMessages>` devuelto (los `validate*` **no** lanzan).

### Decisiones tomadas ante ambigüedades del diseño (documentadas aquí, §2.6 del README de la plantilla)
1. **`situacionFirma` en los tests del servicio.** El diseño la implementa como campo derivado cuyo getter generado llama a `SituacionFirmaBuilder.build(getFirmante())`. Para fijar la situación de firma en cada test de `TareaFirmaServiceImpl` se usa `Mockito.mockStatic(SituacionFirmaBuilder.class)` programando `build(any())` con el valor deseado, en lugar de mockear la entidad (las entidades no se mockean, §3 del contrato).
2. **Reglas `fireActionRule_*`.** Son métodos **privados** del `TareaFirmaServiceImpl`: no se testean directamente, sino **a través de** los métodos públicos que las disparan (`firmarEnServidor` y `marcarComoFirmada`). El campo `Verifica` de esos tests apunta a la `R-` que ejercen.
3. **Getter generado `TareaFirma.getSituacionFirma()`.** No se le escribe test de delegación: es código **generado** por AOP a partir del CDATA del `domains.xml`, no lo escribe `/sdd-implementer`, y su único contenido útil (`SituacionFirmaBuilder.build`) ya está cubierto al 100 % por los tests de esa clase. Se documenta como decisión, no como omisión.
4. **Recurso PDF inexistente en `TareaFirmaDemoLoader`.** La rama «`getResourceAsStream` devuelve `null` ⇒ `RuntimeException` explícita» **no se testea unitariamente**: la ruta del recurso es una constante de classpath que siempre resuelve en el classpath de test (el PDF del Paso 1 vive en `src/main/resources`), y forzar su ausencia exigiría manipular el `ClassLoader` de la clase, que el diseño no expone como costura. Queda cubierta por inspección de código.
5. **`TareaFirmaController`.** `ActionRequestHelper` se construye con `new` dentro del `@CallMethod` y usa `JpaRepository.of(...)`, así que sus tests requieren `Mockito.mockStatic(JpaRepository.class)`. Se describen igualmente porque son la única forma unitaria de comprobar que el controlador usa **la whitelist correcta** y que **no captura** la excepción de negocio.

---

## Clase: `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl`  —  servicio (**modificada**)

**Responsabilidad:** además de lo que ya hacía, construir el `AlmacenClave` del certificado habilitado de un DNI usando la clave tecleada **solo cuando** el certificado no tiene guardada la suya (Paso 3 del diseño; materializa la parte de `R-TareaFirma-001` descrita en `rules/R-TareaFirma-001.md` §«Cuál clave se usa»).
**Colaboradores a mockear:** `CertificadoDigitalRepository` (mock; `findByDni`), `com.educaflow.base.util.MetaFileUtil` (estático, `downloadContent`, solo en la rama `FICHERO_BD`). `DniUtil` **no** se mockea: es una función pura y se usan DNIs reales válidos/ inválidos. `AlmacenClaveFichero`/`AlmacenClaveDispositivo` **no** se mockean: son objetos de valor con getters públicos (`getPassword()`, `getSlot()`, `getAlias()`) sobre los que se asierta directamente.
**Origen diseño:** Paso 3 (`getAlmacenClaveByDni(String,String)`, `getAlmacenClaveByDni(String)` reducido a delegación, `validateGetAlmacenClaveByDni(String,String)`); `rules/R-TareaFirma-001.md` §«Cuál clave se usa»; §Frontera de confianza («no declara `allowProperties…`»).

### Método: `AlmacenClave getAlmacenClaveByDni(String dni, String claveAcceso)`

- **`getAlmacenClaveByDni_ficheroConClaveGuardada_usaLaGuardadaEIgnoraLaTecleada`** — Tipo: happy. Verifica: `R-TareaFirma-001` (§«Cuál clave se usa»).
  - **Arrange:** `CertificadoDigital` de tipo `CLASSPATH`, `enabled=true`, `rutaClasspath="firma/mi_certificado.p12"` (recurso real del proyecto), `password="nadanada"`; mock `repository.findByDni("85432016B")` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", "claveTecleadaDistinta")`.
  - **Assert:** el resultado es un `AlmacenClaveFichero` y `getPassword()` devuelve `"nadanada"` (nunca `"claveTecleadaDistinta"`).
- **`getAlmacenClaveByDni_ficheroSinClaveGuardada_usaLaClaveTecleada`** — Tipo: happy. Verifica: `R-TareaFirma-001` (§«Cuál clave se usa»).
  - **Arrange:** mismo certificado `CLASSPATH` pero con `password=null`; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", "nadanada")`.
  - **Assert:** `AlmacenClaveFichero` cuyo `getPassword()` es `"nadanada"`.
- **`getAlmacenClaveByDni_ficheroConClaveGuardadaEnBlanco_usaLaClaveTecleada`** — Tipo: borde. Verifica: `R-TareaFirma-001` (§«Cuál clave se usa»).
  - **Arrange:** certificado `CLASSPATH` con `password="   "` (en blanco, no nulo); mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", "nadanada")`.
  - **Assert:** `getPassword()` es `"nadanada"` (la guardada en blanco **no** gana).
- **`getAlmacenClaveByDni_ficheroSinClaveGuardadaNiTecleada_lanzaExcepcion`** — Tipo: error. Verifica: `R-TareaFirma-001` (§«Cuál clave se usa»).
  - **Arrange:** certificado `CLASSPATH` con `password=null`; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", null)`.
  - **Assert:** lanza `RuntimeException` con mensaje `"El password no puede ser null"` (guarda del constructor de `AlmacenClaveFichero`); es la situación que `V-TareaFirma-006` impide que llegue aquí desde el flujo de firma.
- **`getAlmacenClaveByDni_ficheroEnBaseDeDatos_descargaElContenidoYUsaLaClaveEfectiva`** — Tipo: happy. Verifica: `R-TareaFirma-001` (§«Cuál clave se usa»).
  - **Arrange:** certificado `FICHERO_BD` con `fichero` = un `MetaFile` instanciado y `password=null`; `mockStatic(MetaFileUtil)` con `downloadContent(metaFile)` → `byte[]` no vacío; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", "nadanada")`.
  - **Assert:** `AlmacenClaveFichero` con `getPassword()` = `"nadanada"`; `verify` que `MetaFileUtil.downloadContent` se llamó una vez con ese `MetaFile`.
- **`getAlmacenClaveByDni_sistemaDeArchivos_usaLaClaveEfectiva`** — Tipo: happy. Verifica: `R-TareaFirma-001` (§«Cuál clave se usa»).
  - **Arrange:** fichero temporal creado con `@TempDir`; certificado `SISTEMA_ARCHIVOS` con `rutaSistemaArchivos` apuntando a él y `password=null`; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", "nadanada")`.
  - **Assert:** `AlmacenClaveFichero` con `getPassword()` = `"nadanada"`.
- **`getAlmacenClaveByDni_dispositivoPkcs11_descartaLaClaveTecleada`** — Tipo: borde. Verifica: `R-TareaFirma-001` (§«Cuál clave se usa»; desviación declarada en `design.md` nota 13).
  - **Arrange:** certificado `DISPOSITIVO_PKCS11`, `enabled=true`, con `DispositivoCriptografico` de `slot=1` y `Alias` de nombre `"certificado-centro"`; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", "pinTecleado")`.
  - **Assert:** el resultado es un `AlmacenClaveDispositivo` con `getSlot()==1` y `getAlias()=="certificado-centro"`; el PIN tecleado no aparece por ninguna parte (el objeto no tiene dónde guardarlo).
- **`getAlmacenClaveByDni_sinCertificadoParaElDni_lanzaExcepcionNoExisteCertificado`** — Tipo: error. Verifica: `—` (guarda de código preexistente).
  - **Arrange:** mock `repository.findByDni("85432016B")` → `null`.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", "nadanada")`.
  - **Assert:** lanza `RuntimeException` con mensaje exacto `"No existe certificado para el DNI: 85432016B"` (idéntico al del overload de un argumento).
- **`getAlmacenClaveByDni_certificadoDeshabilitado_lanzaExcepcionNoExisteCertificado`** — Tipo: error. Verifica: `—`.
  - **Arrange:** certificado `CLASSPATH` con `enabled=false`; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B", "nadanada")`.
  - **Assert:** lanza `RuntimeException` con mensaje exacto `"No existe certificado para el DNI: 85432016B"`.
- **`getAlmacenClaveByDni_dniInvalido_lanzaValidationExceptionYNoConsultaElRepositorio`** — Tipo: error. Verifica: `—` (validador propio del subsistema de criptografía).
  - **Arrange:** sin stubs del repositorio.
  - **Act:** `service.getAlmacenClaveByDni("12345678A", "nadanada")`.
  - **Assert:** lanza `ValidationException` cuyo mensaje contiene `"El DNI no es válido"`; `verify(repository, never()).findByDni(any())`.

### Método: `AlmacenClave getAlmacenClaveByDni(String dni)`

- **`getAlmacenClaveByDniUnArgumento_certificadoConClaveGuardada_devuelveElMismoResultadoQueAntes`** — Tipo: happy. Verifica: `—` (no regresión del comportamiento observable tras convertirlo en delegación).
  - **Arrange:** certificado `CLASSPATH` habilitado con `password="nadanada"`; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B")`.
  - **Assert:** `AlmacenClaveFichero` con `getPassword()` = `"nadanada"`.
- **`getAlmacenClaveByDniUnArgumento_dispositivoPkcs11Habilitado_devuelveAlmacenClaveDispositivo`** — Tipo: happy. Verifica: `—` (no regresión: es el caso que ya cubre el test existente de la clase).
  - **Arrange:** certificado `DISPOSITIVO_PKCS11` habilitado con `slot=1` y alias; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B")`.
  - **Assert:** el resultado es un `AlmacenClaveDispositivo` (los tests preexistentes de esta clase deben seguir en verde).
- **`getAlmacenClaveByDniUnArgumento_certificadoSinClaveGuardada_lanzaExcepcion`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** certificado `CLASSPATH` habilitado con `password=null`; mock `repository.findByDni` → ese certificado.
  - **Act:** `service.getAlmacenClaveByDni("85432016B")`.
  - **Assert:** lanza `RuntimeException` con mensaje `"El password no puede ser null"`: confirma que la delegación pasa `null` como `claveAcceso` y que el caso sigue siendo tan imposible como antes del cambio.

### Método: `Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni, String claveAcceso)`

- **`validateGetAlmacenClaveByDni_dniValidoYClaveTecleada_devuelveOptionalVacio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ningún stub.
  - **Act:** `service.validateGetAlmacenClaveByDni("85432016B", "nadanada")`.
  - **Assert:** el `Optional` está vacío.
- **`validateGetAlmacenClaveByDni_claveAccesoNula_devuelveOptionalVacio`** — Tipo: borde. Verifica: `—` (por diseño `claveAcceso` puede ser `null`: significa «no me han tecleado ninguna»).
  - **Arrange:** ningún stub.
  - **Act:** `service.validateGetAlmacenClaveByDni("85432016B", null)`.
  - **Assert:** el `Optional` está vacío (la obligatoriedad de la clave se valida en `V-TareaFirma-005`/`V-TareaFirma-006`, no aquí).
- **`validateGetAlmacenClaveByDni_dniInvalido_devuelveMensajeElDniNoEsValido`** — Tipo: error. Verifica: `—`.
  - **Arrange:** ningún stub.
  - **Act:** `service.validateGetAlmacenClaveByDni("12345678A", "nadanada")`.
  - **Assert:** el `Optional` está presente y contiene un `BusinessMessage` de `fieldName` `"dni"` y mensaje exacto `"El DNI no es válido"` (mismo comportamiento que el validador de un argumento).
- **`validateGetAlmacenClaveByDni_dniInvalido_noIncluyeLaClaveEnNingunMensaje`** — Tipo: borde. Verifica: `—` (k-secure-coding §6: la clave nunca viaja en mensajes ni logs).
  - **Arrange:** ningún stub.
  - **Act:** `service.validateGetAlmacenClaveByDni("12345678A", "claveSecretaDePrueba")`.
  - **Assert:** el `toString()` de los `BusinessMessages` devueltos **no** contiene `"claveSecretaDePrueba"` ni ningún fragmento suyo.

---

## Clase: `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder`  —  helper (**nueva**)

**Responsabilidad:** calcular la `SituacionFirma` del firmante a partir de su DNI y del subsistema de criptografía (implementa `CC-TareaFirma-001`, momento lectura). Nunca devuelve `null` y nunca propaga excepción.
**Colaboradores a mockear:** `com.axelor.inject.Beans` (estático: `Beans.get(ModelServiceFactory.class)`), `ModelServiceFactory` (mock; `resolve(CertificadoDigital.class)`), `CertificadoDigitalService` (mock; `getTipoAlmacenClaveByDni(dni)`). Las entidades `User` se instancian con `new` y se rellenan con setters. `DniUtil` no se mockea.
**Origen diseño:** Paso 4 de `design.md`; `CC-TareaFirma-001` de la matriz de campos calculados; notas 5 y 12 de §Notas y supuestos.

### Método: `static SituacionFirma build(User firmante)`

- **`build_firmanteNulo_devuelveSinDni`** — Tipo: borde. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `mockStatic(Beans)` activo pero sin programar nada.
  - **Act:** `SituacionFirmaBuilder.build(null)`.
  - **Assert:** devuelve `SituacionFirma.SIN_DNI`; `Beans.get` no se invoca nunca (`verifyNoInteractions` sobre el `ModelServiceFactory`).
- **`build_firmanteSinDni_devuelveSinDni`** — Tipo: borde. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `User` con `dni = null`.
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** devuelve `SituacionFirma.SIN_DNI`; no se resuelve ningún servicio.
- **`build_firmanteConDniEnBlanco_devuelveSinDni`** — Tipo: borde. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `User` con `dni = "   "`.
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** devuelve `SituacionFirma.SIN_DNI`; no se resuelve ningún servicio.
- **`build_firmanteConDniInvalido_devuelveSinDniYNoConsultaElCertificado`** — Tipo: borde. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `User` con `dni = "12345678A"` (letra de control incorrecta).
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** devuelve `SituacionFirma.SIN_DNI`; `verify(certificadoDigitalService, never()).getTipoAlmacenClaveByDni(any())` — un DNI inválido es una **situación**, no un error de negocio.
- **`build_sinCertificadoHabilitado_devuelveSinCertificado`** — Tipo: happy. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `User` con `dni = "85432016B"`; `mockStatic(Beans)` → `Beans.get(ModelServiceFactory.class)` devuelve el mock de factoría; `factory.resolve(CertificadoDigital.class)` → mock de `CertificadoDigitalService`; `getTipoAlmacenClaveByDni("85432016B")` → `null`.
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** devuelve `SituacionFirma.SIN_CERTIFICADO`.
- **`build_tipoDispositivoConPin_devuelveDispositivoConPin`** — Tipo: happy. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** como el anterior, con `getTipoAlmacenClaveByDni` → `TipoAlmacenClave.DISPOSITIVO_CON_PIN`.
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** devuelve `SituacionFirma.DISPOSITIVO_CON_PIN`.
- **`build_tipoDispositivoSinPin_devuelveDispositivoSinPin`** — Tipo: happy. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `getTipoAlmacenClaveByDni` → `TipoAlmacenClave.DISPOSITIVO_SIN_PIN`.
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** devuelve `SituacionFirma.DISPOSITIVO_SIN_PIN` (valor hoy inalcanzable en producción, pero la traducción debe existir: `design-guidelines.md`).
- **`build_tipoFicheroConClave_devuelveFicheroConClave`** — Tipo: happy. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `getTipoAlmacenClaveByDni` → `TipoAlmacenClave.FICHERO_CON_CLAVE`.
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** devuelve `SituacionFirma.FICHERO_CON_CLAVE`.
- **`build_tipoFicheroSinClave_devuelveFicheroSinClave`** — Tipo: happy. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `getTipoAlmacenClaveByDni` → `TipoAlmacenClave.FICHERO_SIN_CLAVE`.
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** devuelve `SituacionFirma.FICHERO_SIN_CLAVE`.
- **`build_elServicioDeCriptografiaFalla_degradaASinCertificadoSinPropagarLaExcepcion`** — Tipo: error. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `User` con DNI válido; `getTipoAlmacenClaveByDni` programado para lanzar `RuntimeException("fallo de BD")`.
  - **Act:** `SituacionFirmaBuilder.build(user)`.
  - **Assert:** **no** lanza nada (`assertDoesNotThrow`) y devuelve `SituacionFirma.SIN_CERTIFICADO` (valor seguro: deja al firmante el panel de AutoFirma y su «Atrás»).
- **`build_laResolucionDelServicioFalla_degradaASinCertificado`** — Tipo: error. Verifica: `CC-TareaFirma-001`.
  - **Arrange:** `mockStatic(Beans)` → `Beans.get(ModelServiceFactory.class)` lanza `RuntimeException` (inyector no disponible).
  - **Act:** `SituacionFirmaBuilder.build(user)` con DNI válido.
  - **Assert:** no lanza y devuelve `SituacionFirma.SIN_CERTIFICADO` (el `try/catch` cubre también la resolución del servicio, no solo la consulta).
- **`build_elServicioDeCriptografiaFalla_noRegistraElDniCompletoEnElLog`** — Tipo: borde. Verifica: `—` (k-secure-coding §8: datos personales en logs).
  - **Arrange:** appender en memoria (`ListAppender` de logback) enganchado al logger de `SituacionFirmaBuilder`; `getTipoAlmacenClaveByDni` lanza `RuntimeException`.
  - **Act:** `SituacionFirmaBuilder.build(user)` con `dni = "85432016B"`.
  - **Assert:** se registró al menos un evento de error y **ninguna** línea del log contiene la cadena `"85432016B"` (el DNI va enmascarado).

---

## Clase: `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl`  —  servicio (**modificada**)

**Responsabilidad:** además de lo que ya hacía, ejecutar la acción `firmarEnServidor` (validar, firmar todos los documentos con el certificado del firmante, resolver la tarea como firmada, descartar la clave y notificar), y cerrar la frontera de confianza de `insert`/`update`.
**Colaboradores a mockear:** `TareaFirmaRepository` (mock; `save`), `ModelServiceFactory` (mock inyectado por reflexión en el campo `@Inject`), `CertificadoDigitalService` (mock devuelto por `resolve(CertificadoDigital.class)`), `com.educaflow.base.infrastructure.metafile.MetaFileHelper` (estático: `getDocumentoPdf`, `createMetaFile`), `com.educaflow.base.infrastructure.pdf.DocumentoPdf` (mock; `firmar`), `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder` (estático, para fijar la situación de firma que devuelve el getter derivado), `com.axelor.auth.AuthUtils` (estático: `getUser()`), `com.axelor.inject.Beans` (estático, para el `TareaFirmaNotifier` de `fireActionRule_NotificarFirmaResuelta`). Las entidades `TareaFirma`, `DocumentoFirma`, `User` y `MetaFile` se instancian con `new`.
**Origen diseño:** Paso 5 (`firmarEnServidor`, `validateFirmarEnServidor`, `allowPropertiesFirmarEnServidor`, `allowPropertiesInsert`, `allowPropertiesUpdate`, delta de `marcarComoFirmada`, reglas `fireActionRule_FirmarDocumentosEnServidor` / `_ResolverComoFirmada` / `_DescartarClaveFirma` / `_NotificarFirmaResuelta`); `V-TareaFirma-001`…`007`; `R-TareaFirma-001`…`004`; `rules/R-TareaFirma-001.md`; §Frontera de confianza.

### Método: `Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal)`

> **Arrange común** (helper del test): `User` firmante con `dni = "85432016B"`; `TareaFirma` con `estadoTareaFirma = PENDIENTE`, ese firmante, un `DocumentoFirma` con `documentoOriginal` no nulo, y `claveFirma` según el caso; `mockStatic(AuthUtils)` con `getUser()` → el firmante; `mockStatic(SituacionFirmaBuilder)` con `build(any())` → la situación del caso. El segundo parámetro (`tareaFirmaOriginal`) se pasa como una `TareaFirma` cualquiera: el método no lo usa.

- **`validateFirmarEnServidor_tareaPendienteDelUsuarioConCertificadoConClaveGuardada_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-TareaFirma-001`, `V-TareaFirma-002`, `V-TareaFirma-003`, `V-TareaFirma-004`, `V-TareaFirma-005`, `V-TareaFirma-006`, `V-TareaFirma-007`.
  - **Arrange:** arrange común, situación `FICHERO_CON_CLAVE`, `claveFirma = null`.
  - **Act:** `service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal)`.
  - **Assert:** el `Optional` está vacío.
- **`validateFirmarEnServidor_tareaYaFirmada_devuelveMensajeSoloSePuedenFirmarLasPendientes`** — Tipo: error. Verifica: `V-TareaFirma-001`.
  - **Arrange:** arrange común con `estadoTareaFirma = FIRMADO`, situación `FICHERO_CON_CLAVE`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** el `Optional` está presente y algún mensaje es exactamente `"Solo se pueden firmar las tareas pendientes de firmar"`.
- **`validateFirmarEnServidor_tareaRechazada_devuelveMensajeSoloSePuedenFirmarLasPendientes`** — Tipo: borde. Verifica: `V-TareaFirma-001`.
  - **Arrange:** igual pero con `estadoTareaFirma = RECHAZADO`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** mensaje exacto `"Solo se pueden firmar las tareas pendientes de firmar"`.
- **`validateFirmarEnServidor_estadoNulo_devuelveMensajeSoloSePuedenFirmarLasPendientes`** — Tipo: borde. Verifica: `V-TareaFirma-001`.
  - **Arrange:** igual pero con `estadoTareaFirma = null`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** mensaje exacto `"Solo se pueden firmar las tareas pendientes de firmar"` y ninguna `NullPointerException`.
- **`validateFirmarEnServidor_firmanteDistintoDelUsuarioAutenticado_devuelveMensajeSoloPuedeFirmarLaPersonaEncargada`** — Tipo: error. Verifica: `V-TareaFirma-002`.
  - **Arrange:** arrange común pero `AuthUtils.getUser()` → **otro** `User` (id distinto del firmante), situación `FICHERO_CON_CLAVE`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** algún mensaje es exactamente `"Solo puede firmar los documentos la persona a la que se le han encargado"`.
- **`validateFirmarEnServidor_sinUsuarioAutenticado_devuelveMensajeSoloPuedeFirmarLaPersonaEncargada`** — Tipo: borde. Verifica: `V-TareaFirma-002`.
  - **Arrange:** `AuthUtils.getUser()` → `null`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** mensaje exacto `"Solo puede firmar los documentos la persona a la que se le han encargado"` y ninguna `NullPointerException`.
- **`validateFirmarEnServidor_situacionSinDni_devuelveMensajeSuUsuarioNoTieneDni`** — Tipo: error. Verifica: `V-TareaFirma-003`.
  - **Arrange:** arrange común con situación `SIN_DNI`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** algún mensaje es exactamente `"No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador."`.
- **`validateFirmarEnServidor_situacionSinCertificado_devuelveMensajeNoTieneCertificadoDadoDeAlta`** — Tipo: error. Verifica: `V-TareaFirma-004`.
  - **Arrange:** arrange común con situación `SIN_CERTIFICADO`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** algún mensaje es exactamente `"No es posible firmar en el servidor porque no tiene un certificado digital dado de alta"`.
- **`validateFirmarEnServidor_dispositivoSinPinYSinClaveTecleada_devuelveMensajeElPinEsObligatorio`** — Tipo: error. Verifica: `V-TareaFirma-005`.
  - **Arrange:** arrange común con situación `DISPOSITIVO_SIN_PIN` y `claveFirma = null`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** algún mensaje es exactamente `"El PIN es obligatorio"`.
- **`validateFirmarEnServidor_dispositivoSinPinYClaveEnBlanco_devuelveMensajeElPinEsObligatorio`** — Tipo: borde. Verifica: `V-TareaFirma-005`.
  - **Arrange:** situación `DISPOSITIVO_SIN_PIN` y `claveFirma = "   "`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** mensaje exacto `"El PIN es obligatorio"` (una clave en blanco no cuenta como indicada).
- **`validateFirmarEnServidor_dispositivoSinPinConPinTecleado_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-TareaFirma-005`.
  - **Arrange:** situación `DISPOSITIVO_SIN_PIN` y `claveFirma = "1234"`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** el `Optional` está vacío.
- **`validateFirmarEnServidor_ficheroSinClaveYSinClaveTecleada_devuelveMensajeLaContrasenaEsObligatoria`** — Tipo: error. Verifica: `V-TareaFirma-006`.
  - **Arrange:** arrange común con situación `FICHERO_SIN_CLAVE` y `claveFirma = null`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** algún mensaje es exactamente `"La contraseña es obligatoria"` (literal de ESC-004).
- **`validateFirmarEnServidor_ficheroSinClaveYClaveEnBlanco_devuelveMensajeLaContrasenaEsObligatoria`** — Tipo: borde. Verifica: `V-TareaFirma-006`.
  - **Arrange:** situación `FICHERO_SIN_CLAVE` y `claveFirma = ""`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** mensaje exacto `"La contraseña es obligatoria"`.
- **`validateFirmarEnServidor_ficheroSinClaveConClaveTecleada_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-TareaFirma-006`.
  - **Arrange:** situación `FICHERO_SIN_CLAVE` y `claveFirma = "nadanada"`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** el `Optional` está vacío.
- **`validateFirmarEnServidor_ficheroConClaveGuardadaYSinClaveTecleada_devuelveOptionalVacio`** — Tipo: borde. Verifica: `V-TareaFirma-005`, `V-TareaFirma-006`.
  - **Arrange:** situación `FICHERO_CON_CLAVE` y `claveFirma = null`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** el `Optional` está vacío: la clave solo se exige en las dos situaciones «sin clave guardada».
- **`validateFirmarEnServidor_dispositivoConPinYSinClaveTecleada_devuelveOptionalVacio`** — Tipo: borde. Verifica: `V-TareaFirma-005`.
  - **Arrange:** situación `DISPOSITIVO_CON_PIN` y `claveFirma = null`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** el `Optional` está vacío.
- **`validateFirmarEnServidor_tareaSinDocumentos_devuelveMensajeNoTieneNingunDocumento`** — Tipo: error. Verifica: `V-TareaFirma-007`.
  - **Arrange:** arrange común con `documentosFirma` = lista vacía, situación `FICHERO_CON_CLAVE`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** algún mensaje es exactamente `"La tarea de firma no tiene ningún documento que firmar"`.
- **`validateFirmarEnServidor_documentosNulos_devuelveMensajeNoTieneNingunDocumento`** — Tipo: borde. Verifica: `V-TareaFirma-007`.
  - **Arrange:** `documentosFirma = null`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** mensaje exacto `"La tarea de firma no tiene ningún documento que firmar"` y ninguna `NullPointerException`.
- **`validateFirmarEnServidor_variasReglasIncumplidas_acumulaTodosLosMensajes`** — Tipo: borde. Verifica: `V-TareaFirma-001`, `V-TareaFirma-002`, `V-TareaFirma-004`, `V-TareaFirma-007`.
  - **Arrange:** tarea `FIRMADO`, sin documentos, `AuthUtils.getUser()` → otro usuario, situación `SIN_CERTIFICADO`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** el `BusinessMessages` devuelto contiene **los cuatro** mensajes (no se corta en el primero): se acumulan en un único `BusinessMessages`.
- **`validateFirmarEnServidor_claveTecleada_nuncaApareceEnLosMensajes`** — Tipo: borde. Verifica: `—` (k-secure-coding §6).
  - **Arrange:** situación `SIN_CERTIFICADO` con `claveFirma = "claveSecretaDePrueba"`.
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** el `toString()` de los `BusinessMessages` **no** contiene `"claveSecretaDePrueba"`.
- **`validateFirmarEnServidor_siempre_recalculaLaSituacionDeFirmaEnElServidor`** — Tipo: borde. Verifica: `CC-TareaFirma-001`, `V-TareaFirma-004`.
  - **Arrange:** arrange común; `SituacionFirmaBuilder.build(any())` → `SIN_CERTIFICADO` (aunque la entidad no traiga nada del cliente).
  - **Act:** `service.validateFirmarEnServidor(...)`.
  - **Assert:** hay mensaje de `V-TareaFirma-004`, y `verify` de que `SituacionFirmaBuilder.build` se invocó al menos una vez con el firmante de la tarea: la situación se consulta **en el servidor**, no se toma de lo que trajera la pantalla.

### Método: `TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal)`

> **Arrange común** (helper del test): tarea `PENDIENTE` del usuario autenticado, situación `FICHERO_CON_CLAVE` (salvo indicación), `claveFirma = "nadanada"`, recuadro `x=75`, `y=200`, `width=400`, `height=60` (BigDecimal) y `page=1`, `fqcnFirmaNotifier` = el nombre de `TareaFirmaNotifierDePrueba` (una clase notificadora de prueba declarada en el propio fichero de test, que implementa `com.educaflow.subsystem.firmas.service.TareaFirmaNotifier`; los tests de `subsystem/firmas` **no** dependen de `com.educaflow.secretariavirtual.datademo`), `fqcnCallBackData = null`, y N `DocumentoFirma` con su `documentoOriginal`. Mocks: `modelServiceFactory.resolve(CertificadoDigital.class)` → mock de `CertificadoDigitalService`; `certificadoDigitalService.getAlmacenClaveByDni(anyString(), any())` → mock de `AlmacenClave`; `mockStatic(MetaFileHelper)` con `getDocumentoPdf(any())` → mock de `DocumentoPdf` original y `createMetaFile(any())` → un `MetaFile` nuevo por invocación; el `DocumentoPdf` original devuelve otro `DocumentoPdf` (firmado) en `firmar(any(), any())`; `repository.save(any())` → el mismo argumento recibido; `mockStatic(Beans)` con `Beans.get(TareaFirmaNotifierDePrueba.class)` → mock de `TareaFirmaNotifier`.

- **`firmarEnServidor_tareaValidaConUnDocumento_firmaGuardaYDejaLaTareaFirmada`** — Tipo: happy. Verifica: `R-TareaFirma-001`, `R-TareaFirma-002`.
  - **Arrange:** arrange común con 1 documento.
  - **Act:** `service.firmarEnServidor(tareaFirma, tareaFirmaOriginal)`.
  - **Assert:** el `DocumentoFirma` tiene `documentoFirmado` no nulo; la tarea queda con `estadoTareaFirma = FIRMADO` y `fechaResolucion` no nula; `verify(repository).save(tareaFirma)` una vez.
- **`firmarEnServidor_tareaConDosDocumentos_pideUnAlmacenClaveNuevoPorCadaDocumento`** — Tipo: happy. Verifica: `R-TareaFirma-001` (regla CRITICAL de `rules/R-TareaFirma-001.md`).
  - **Arrange:** arrange común con 2 documentos.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** `verify(certificadoDigitalService, times(2)).getAlmacenClaveByDni("85432016B", "nadanada")` — un almacén **por documento**, nunca uno reutilizado (el `InputStream` se consume).
- **`firmarEnServidor_tareaConDosDocumentos_asignaUnMetaFileFirmadoDistintoACadaDocumento`** — Tipo: happy. Verifica: `R-TareaFirma-001`.
  - **Arrange:** arrange común con 2 documentos; `MetaFileHelper.createMetaFile` devuelve una instancia distinta en cada llamada.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** los dos `DocumentoFirma` tienen `documentoFirmado` no nulo y **distintos** entre sí; `MetaFileHelper.createMetaFile` se invocó exactamente 2 veces.
- **`firmarEnServidor_tareaValida_construyeElCampoFirmaConElRecuadroYLaPaginaDeLaTarea`** — Tipo: happy. Verifica: `R-TareaFirma-001`.
  - **Arrange:** arrange común con 1 documento; `ArgumentCaptor<CampoFirma>` en `documentoPdf.firmar(any(), captor)`.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** el `CampoFirma` capturado lleva un `Rectangulo` con `x=75f`, `y=200f`, `width=400f`, `height=60f` (conversión `.floatValue()` de los `BigDecimal`) y número de página `1`.
- **`firmarEnServidor_tareaValida_pasaAlServicioDeCriptografiaElDniDelFirmanteYLaClaveTecleada`** — Tipo: happy. Verifica: `R-TareaFirma-001`.
  - **Arrange:** arrange común con 1 documento y `claveFirma = "nadanada"`.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** `verify(certificadoDigitalService).getAlmacenClaveByDni("85432016B", "nadanada")`: el DNI sale de la entidad (nunca del cliente) y la decisión «clave guardada gana» queda delegada en el servicio de criptografía.
- **`firmarEnServidor_firmaDelSegundoDocumentoFalla_noDejaFirmadoNingunDocumento`** — Tipo: error. Verifica: `R-TareaFirma-001` (RN-TareaFirma-002, «todo o nada»).
  - **Arrange:** arrange común con 2 documentos; el `DocumentoPdf` del segundo lanza `RuntimeException("clave incorrecta")` en `firmar`.
  - **Act:** `service.firmarEnServidor(...)` (dentro de `assertThrows`).
  - **Assert:** los **dos** `DocumentoFirma` conservan `documentoFirmado == null`; `MetaFileHelper.createMetaFile` **nunca** se invocó (`never()`); `verify(repository, never()).save(any())`.
- **`firmarEnServidor_laFirmaFalla_lanzaValidationExceptionConMensajeQueEmpiezaPorNoSeHanPodidoFirmar`** — Tipo: error. Verifica: `R-TareaFirma-001` (RN-TareaFirma-007).
  - **Arrange:** arrange común con 1 documento; `documentoPdf.firmar` lanza `RuntimeException("clave incorrecta")`.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** lanza `ValidationException` cuyo mensaje **contiene** `"No se han podido firmar los documentos: "` y además el motivo concreto (`"clave incorrecta"`), tal y como comprueba ESC-003.
- **`firmarEnServidor_laObtencionDelAlmacenClaveFalla_lanzaElMismoErrorDeNegocio`** — Tipo: error. Verifica: `R-TareaFirma-001` (RN-TareaFirma-007).
  - **Arrange:** arrange común con 1 documento; `certificadoDigitalService.getAlmacenClaveByDni(any(), any())` lanza `RuntimeException("No existe certificado para el DNI: 85432016B")` (el certificado se deshabilitó entre la validación y la firma).
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** lanza `ValidationException` con mensaje que empieza por `"No se han podido firmar los documentos: "`; ningún documento firmado y `repository.save` nunca invocado.
- **`firmarEnServidor_elPdfOriginalNoEsValido_lanzaElMismoErrorDeNegocio`** — Tipo: error. Verifica: `R-TareaFirma-001` (RN-TareaFirma-007).
  - **Arrange:** arrange común con 1 documento; `MetaFileHelper.getDocumentoPdf(any())` lanza `RuntimeException("El MetaFile no es de tipo PDF")`.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** lanza `ValidationException` con mensaje que empieza por `"No se han podido firmar los documentos: "`.
- **`firmarEnServidor_laFirmaFalla_dejaLaTareaPendienteYSinFechaDeResolucion`** — Tipo: error. Verifica: `R-TareaFirma-002`.
  - **Arrange:** arrange común con 1 documento; `documentoPdf.firmar` lanza `RuntimeException`.
  - **Act:** `service.firmarEnServidor(...)` (dentro de `assertThrows`).
  - **Assert:** la tarea sigue con `estadoTareaFirma = PENDIENTE` y `fechaResolucion == null`: el firmante puede reintentar (ESC-003).
- **`firmarEnServidor_tareaValida_asignaEstadoYFechaDeResolucionIncondicionalmente`** — Tipo: borde. Verifica: `R-TareaFirma-002`.
  - **Arrange:** arrange común con 1 documento y la tarea llegando ya con `fechaResolucion` puesta a una fecha antigua (p. ej. `LocalDateTime.of(2000,1,1,0,0)`).
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** `fechaResolucion` **cambió** (no es la antigua) y `estadoTareaFirma == FIRMADO`: la asignación es incondicional, sin guarda `if (… == null)`.
- **`firmarEnServidor_firmaCompletada_descartaLaClaveDeFirma`** — Tipo: happy. Verifica: `R-TareaFirma-003` (RN-TareaFirma-004).
  - **Arrange:** arrange común con 1 documento y `claveFirma = "nadanada"`.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** `tareaFirma.getClaveFirma()` es `null` al terminar.
- **`firmarEnServidor_laFirmaFalla_descartaLaClaveDeFirmaIgualmente`** — Tipo: error. Verifica: `R-TareaFirma-003` (RN-TareaFirma-008).
  - **Arrange:** arrange común con 1 documento; `documentoPdf.firmar` lanza `RuntimeException`.
  - **Act:** `service.firmarEnServidor(...)` (dentro de `assertThrows`).
  - **Assert:** tras la excepción, `tareaFirma.getClaveFirma()` es `null` (el `finally` la borra pase lo que pase).
- **`firmarEnServidor_validacionRechazada_descartaLaClaveDeFirmaIgualmente`** — Tipo: error. Verifica: `R-TareaFirma-003` (RN-TareaFirma-008).
  - **Arrange:** arrange común con situación `SIN_CERTIFICADO` (la validación falla) y `claveFirma = "nadanada"`.
  - **Act:** `service.firmarEnServidor(...)` (dentro de `assertThrows`).
  - **Assert:** tras la `ValidationException`, `tareaFirma.getClaveFirma()` es `null`: el `try/finally` envuelve también la validación.
- **`firmarEnServidor_validacionRechazada_lanzaValidationExceptionYNoFirmaNiGuarda`** — Tipo: error. Verifica: `V-TareaFirma-001`, `V-TareaFirma-004`.
  - **Arrange:** arrange común pero con `estadoTareaFirma = FIRMADO` y situación `SIN_CERTIFICADO`.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** lanza `ValidationException` cuyo mensaje contiene `"Solo se pueden firmar las tareas pendientes de firmar"`; `verify(repository, never()).save(any())`; `MetaFileHelper.createMetaFile` nunca invocado; `certificadoDigitalService.getAlmacenClaveByDni` nunca invocado (la validación es la **primera** línea ejecutable).
- **`firmarEnServidor_firmaCompletada_notificaAlProcesoQueEncargoLaFirma`** — Tipo: happy. Verifica: `R-TareaFirma-004`.
  - **Arrange:** arrange común con 1 documento; `mockStatic(Beans)` con `Beans.get(TareaFirmaNotifierDePrueba.class)` → mock de `TareaFirmaNotifier`.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** `verify(tareaFirmaNotifier).notify(tareaFirma, null)` una vez, y **después** del `repository.save` (`InOrder` entre el mock del repositorio y el del notificador).
- **`firmarEnServidor_laFirmaFalla_noNotificaAlProcesoQueEncargoLaFirma`** — Tipo: error. Verifica: `R-TareaFirma-004`.
  - **Arrange:** arrange común con 1 documento; `documentoPdf.firmar` lanza `RuntimeException`.
  - **Act:** `service.firmarEnServidor(...)` (dentro de `assertThrows`).
  - **Assert:** `verifyNoInteractions` sobre el mock del `TareaFirmaNotifier`.
- **`firmarEnServidor_firmaCompletada_devuelveLaTareaDevueltaPorElRepositorio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** arrange común con 1 documento; `repository.save(any())` → una instancia **distinta** de `TareaFirma` preparada por el test.
  - **Act:** `TareaFirma resultado = service.firmarEnServidor(...)`.
  - **Assert:** `assertSame` entre el resultado y la instancia devuelta por el repositorio (el método devuelve lo guardado, no el argumento).
- **`firmarEnServidor_laFirmaFalla_noIncluyeLaClaveDeFirmaEnElMensajeDeError`** — Tipo: borde. Verifica: `—` (k-secure-coding §6).
  - **Arrange:** arrange común con `claveFirma = "claveSecretaDePrueba"`; `documentoPdf.firmar` lanza `RuntimeException("clave incorrecta")`.
  - **Act:** `service.firmarEnServidor(...)`.
  - **Assert:** el mensaje de la `ValidationException` **no** contiene `"claveSecretaDePrueba"` ni ningún fragmento suyo.

### Método: `TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal)` (**delta**: pasa a usar la regla extraída)

- **`marcarComoFirmada_tareaValida_dejaLaTareaFirmadaConFechaDeResolucion`** — Tipo: happy. Verifica: `R-TareaFirma-002`.
  - **Arrange:** `TareaFirma` `PENDIENTE` con `fqcnFirmaNotifier` = el nombre de `TareaFirmaNotifierDePrueba` (la clase notificadora de prueba declarada en el propio fichero de test) y `fqcnCallBackData = null`; `repository.save(any())` → el argumento; `mockStatic(Beans)` con `Beans.get(TareaFirmaNotifierDePrueba.class)` → mock de `TareaFirmaNotifier`.
  - **Act:** `service.marcarComoFirmada(tareaFirma, tareaFirmaOriginal)`.
  - **Assert:** `estadoTareaFirma == FIRMADO` y `fechaResolucion` no nula; `verify(repository).save(tareaFirma)`. Comportamiento **idéntico** al de antes del delta (test de no regresión del flujo de AutoFirma).
- **`marcarComoFirmada_fechaResolucionPreexistente_laSobrescribe`** — Tipo: borde. Verifica: `R-TareaFirma-002`.
  - **Arrange:** tarea que llega con `fechaResolucion` antigua y `estadoTareaFirma = PENDIENTE`.
  - **Act:** `service.marcarComoFirmada(...)`.
  - **Assert:** `fechaResolucion` cambió respecto a la antigua (asignación incondicional, sin guarda de mass-assignment).
- **`marcarComoFirmada_tareaValida_notificaAlProcesoQueEncargoLaFirma`** — Tipo: happy. Verifica: `R-TareaFirma-004`.
  - **Arrange:** como el primero.
  - **Act:** `service.marcarComoFirmada(...)`.
  - **Assert:** `verify(tareaFirmaNotifier).notify(tareaFirma, null)` una vez (el delta no rompe la notificación).

### Método: `AllowProperties allowPropertiesFirmarEnServidor()`

- **`allowPropertiesFirmarEnServidor_soloPermiteLaClaveDeFirma`** — Tipo: happy. Verifica: `—` (§Frontera de confianza, acción `firmarEnServidor`).
  - **Arrange:** servicio instanciado con el repositorio mockeado; sin más stubs.
  - **Act:** `service.allowPropertiesFirmarEnServidor()`.
  - **Assert:** `allowProperty("claveFirma")` es `true`; `allowProperty` es `false` para `"situacionFirma"`, `"estadoTareaFirma"`, `"fechaResolucion"`, `"firmante"`, `"documentosFirma"`, `"motivoFirma"`, `"motivoRechazo"`, `"fqcnFirmaNotifier"`, `"fqcnCallBackData"`, `"callBackData"`, `"x"`, `"y"`, `"width"`, `"height"` y `"page"`.

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_noPermiteNingunCampo`** — Tipo: happy. Verifica: `—` (§Frontera de confianza, acción `Crear` — «(ninguna)»).
  - **Arrange:** servicio instanciado.
  - **Act:** `service.allowPropertiesInsert()`.
  - **Assert:** `allowProperty` es `false` para una muestra representativa (`"firmante"`, `"estadoTareaFirma"`, `"documentosFirma"`, `"claveFirma"`, `"fqcnFirmaNotifier"`): es una whitelist deny-all.

### Método: `AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_noPermiteNingunCampo`** — Tipo: happy. Verifica: `—` (§Frontera de confianza, acción `Modificar` — «(ninguna)»).
  - **Arrange:** servicio instanciado.
  - **Act:** `service.allowPropertiesUpdate()`.
  - **Assert:** `allowProperty` es `false` para la misma muestra representativa, incluidos `"estadoTareaFirma"`, `"fechaResolucion"`, `"firmante"`, `"documentosFirma"` y `"claveFirma"`: la tarea solo cambia por sus acciones propias, nunca guardando el formulario ni por `POST /ws/rest/<FQN>`.

---

## Clase: `com.educaflow.subsystem.firmas.controller.TareaFirmaController`  —  controlador (**modificada**)

**Responsabilidad:** exponer como `@CallMethod` la validación y la ejecución de la firma en servidor, obteniendo la entidad y el original con la whitelist de la acción y delegando en el servicio.
**Colaboradores a mockear:** `ActionRequest` / `ActionResponse` (mocks), `ModelServiceFactory` (mock, inyectado por reflexión en el campo `@Inject` o con `@InjectMocks`), `TareaFirmaService` (mock devuelto por `resolve(TareaFirma.class)`), `com.axelor.db.JpaRepository` (estático: `JpaRepository.of(TareaFirma.class)` → mock de repositorio cuyo `find(id)` devuelve la `TareaFirma` del test).
**Origen diseño:** Paso 6 (`validateFirmarEnServidor` y `firmarEnServidor` como `@CallMethod`); §Frontera de confianza (misma whitelist en los dos métodos).

> **Arrange común:** `actionRequest.getData()` devuelve un mapa con la clave `"context"` → mapa con `"_model" = "com.educaflow.subsystem.firmas.db.TareaFirma"`, `"id" = 1L` y `"claveFirma" = "nadanada"`; `mockStatic(JpaRepository)` con `of(TareaFirma.class)` → repositorio mock cuyo `find(1L)` devuelve una `TareaFirma` preparada; `tareaFirmaService.allowPropertiesFirmarEnServidor()` → `AllowProperties.createAllowProperties(Map.of("claveFirma", Map.of()))`.

### Método: `void validateFirmarEnServidor(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`validateFirmarEnServidor_servicioSinMensajes_noDevuelveNingunError`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** arrange común; `tareaFirmaService.validateFirmarEnServidor(any(), any())` → `Optional.empty()`.
  - **Act:** `controller.validateFirmarEnServidor(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse, never()).setError(anyString())` y `never()).setError(anyString(), anyString())`.
- **`validateFirmarEnServidor_servicioConMensajes_entregaLosMensajesComoError`** — Tipo: error. Verifica: `—` (entrega al cliente de los mensajes del validador; el servicio está mockeado, así que ninguna `V-` se ejerce aquí: la cobertura de `V-TareaFirma-001`…`V-TareaFirma-007` está en los tests de `TareaFirmaServiceImpl.validateFirmarEnServidor`).
  - **Arrange:** arrange común; `validateFirmarEnServidor` → `Optional.of(BusinessMessages.single("La contraseña es obligatoria"))`.
  - **Act:** `controller.validateFirmarEnServidor(...)`.
  - **Assert:** `verify(actionResponse).setError(captor)` y el texto capturado contiene `"La contraseña es obligatoria"` (el helper lo envuelve en `<ul><li>…</li></ul>`).
- **`validateFirmarEnServidor_siempre_usaLaWhitelistDeLaAccion`** — Tipo: borde. Verifica: `—` (§Frontera de confianza).
  - **Arrange:** arrange común; `validateFirmarEnServidor` → `Optional.empty()`.
  - **Act:** `controller.validateFirmarEnServidor(...)`.
  - **Assert:** `verify(tareaFirmaService).allowPropertiesFirmarEnServidor()`; **no** se invoca `allowPropertiesValidarDocumentosFirmados()` (allow-all preexistente) ni ninguna otra whitelist.
- **`validateFirmarEnServidor_siempre_noEjecutaLaFirma`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** arrange común; `validateFirmarEnServidor` → `Optional.empty()`.
  - **Act:** `controller.validateFirmarEnServidor(...)`.
  - **Assert:** `verify(tareaFirmaService, never()).firmarEnServidor(any(), any())` — el método solo valida, no escribe (por eso no lleva `@Transactional`).

### Método: `void firmarEnServidor(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`firmarEnServidor_peticionValida_delegaEnElServicioConLaEntidadYElOriginal`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** arrange común; `tareaFirmaService.firmarEnServidor(any(), any())` → la tarea.
  - **Act:** `controller.firmarEnServidor(actionRequest, actionResponse)`.
  - **Assert:** `verify(tareaFirmaService).firmarEnServidor(any(TareaFirma.class), any())` una vez; el primer argumento capturado tiene `claveFirma == "nadanada"` (llegó por la whitelist).
- **`firmarEnServidor_peticionValida_usaLaWhitelistAllowPropertiesFirmarEnServidor`** — Tipo: borde. Verifica: `—` (§Frontera de confianza).
  - **Arrange:** arrange común; el mapa de contexto incluye además `"estadoTareaFirma" = "FIRMADO"` y `"firmante"` con otro id.
  - **Act:** `controller.firmarEnServidor(...)`.
  - **Assert:** `verify(tareaFirmaService).allowPropertiesFirmarEnServidor()`; la entidad capturada **no** trae el estado ni el firmante dictados por el cliente (conserva los de la entidad cargada de BD).
- **`firmarEnServidor_elServicioLanzaValidationException_laPropagaSinCapturarla`** — Tipo: error. Verifica: `—` (propagación de la excepción de negocio sin reempaquetar; el servicio está mockeado: la cobertura de `R-TareaFirma-001` y de `V-TareaFirma-001`…`007` está en los tests de `TareaFirmaServiceImpl`).
  - **Arrange:** arrange común; `tareaFirmaService.firmarEnServidor(any(), any())` lanza `ValidationException("No se han podido firmar los documentos: clave incorrecta")`.
  - **Act:** `controller.firmarEnServidor(...)`.
  - **Assert:** la `ValidationException` **sale** del `@CallMethod` (`assertThrows`) con el mismo mensaje; el controlador no la reempaqueta ni la convierte en respuesta (`verify(actionResponse, never()).setError(anyString())`), que es lo que hace que la cadena de acciones de la vista se detenga y la transacción revierta.
- **`firmarEnServidor_peticionValida_noMontaNingunaRespuestaEnElActionResponse`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** arrange común; `firmarEnServidor` del servicio devuelve la tarea.
  - **Act:** `controller.firmarEnServidor(...)`.
  - **Assert:** `verifyNoInteractions(actionResponse)` — el cierre de la ventana lo hace el `force-back` de la vista, no el controlador.

---

## Clase: `com.educaflow.secretariavirtual.datademo.TareaFirmaDemoLoader`  —  helper (**nueva**)

**Responsabilidad:** callback `call=` del data-import de demo: crea, de forma idempotente, los `DocumentoFirma` de una tarea de firma precargada a partir del PDF de ejemplo del classpath, y le fija el notificador de demo.
**Colaboradores a mockear:** `com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory` (estático: `getDocumentoPdf(byte[], String)` → mock de `DocumentoPdf` cuyo `getFileName()` devuelve el nombre pedido), `com.educaflow.base.infrastructure.metafile.MetaFileHelper` (estático: `createMetaFile(DocumentoPdf)` → un `MetaFile` distinto por invocación). `TareaFirma` y `DocumentoFirma` se instancian con `new`. El recurso `data-demo/input/documento_ejemplo_firma.pdf` se lee del classpath **real** de test (existe porque vive en `src/main/resources`).
**Origen diseño:** Paso 10.3 de `design.md`.

### Método: `Object crearDocumentos(Object bean, Map values)`

- **`crearDocumentos_tareaSinDocumentosYUnSoloDocumento_creaUnDocumentoFirmaConElPdfDeEjemplo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `TareaFirma` nueva sin documentos; `values` con `numeroDocumentos = 1`; estáticos mockeados como arriba.
  - **Act:** `loader.crearDocumentos(tareaFirma, values)`.
  - **Assert:** la tarea queda con **un** `DocumentoFirma`, cuyo `documentoOriginal` es el `MetaFile` devuelto por `MetaFileHelper.createMetaFile`, con `documentoFirmado == null` y con la propia tarea como padre (`getTareaFirma()`).
- **`crearDocumentos_dosDocumentos_lesDaNombresDistinguiblesYMetaFilesDistintos`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `TareaFirma` nueva; `values` con `numeroDocumentos = 2`; `DocumentoPdfFactory.getDocumentoPdf` con `ArgumentCaptor<String>` sobre el nombre; `MetaFileHelper.createMetaFile` devuelve dos instancias distintas.
  - **Act:** `loader.crearDocumentos(tareaFirma, values)`.
  - **Assert:** se crearon 2 `DocumentoFirma`; los nombres capturados son exactamente `"documento_ejemplo_firma_1.pdf"` y `"documento_ejemplo_firma_2.pdf"` (el grid ordena por `documentoOriginal.fileName` y ESC-003 necesita dos filas distinguibles); los dos `MetaFile` son objetos distintos (cada documento tiene su propia copia física).
- **`crearDocumentos_dosDocumentos_leeElPdfDelClasspathUnaSolaVez`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `values` con `numeroDocumentos = 2`; `ArgumentCaptor<byte[]>` en `DocumentoPdfFactory.getDocumentoPdf`.
  - **Act:** `loader.crearDocumentos(tareaFirma, values)`.
  - **Assert:** los dos `byte[]` capturados son **el mismo contenido** y no vacío (el recurso se lee una vez y se reutiliza), y `getDocumentoPdf` se invocó exactamente 2 veces.
- **`crearDocumentos_tareaQueYaTieneDocumentos_noCreaNingunoMas`** — Tipo: borde. Verifica: `—` (idempotencia de la recarga de datos de demo).
  - **Arrange:** `TareaFirma` que ya trae un `DocumentoFirma`; `values` con `numeroDocumentos = 2`.
  - **Act:** `loader.crearDocumentos(tareaFirma, values)`.
  - **Assert:** la tarea sigue con **un** `DocumentoFirma` (el mismo objeto); `MetaFileHelper.createMetaFile` y `DocumentoPdfFactory.getDocumentoPdf` **nunca** se invocaron.
- **`crearDocumentos_siempre_fijaElFqcnDelNotificadorDeDemo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `TareaFirma` nueva; `values` con `numeroDocumentos = 1`.
  - **Act:** `loader.crearDocumentos(tareaFirma, values)`.
  - **Assert:** `getFqcnFirmaNotifier()` es exactamente `"com.educaflow.secretariavirtual.datademo.TareaFirmaDemoNotifier"` (sin él, `fireActionRule_NotificarFirmaResuelta` rompería al firmar o rechazar la tarea de demo).
- **`crearDocumentos_siempre_devuelveElMismoBeanRecibido`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `TareaFirma` nueva; `values` con `numeroDocumentos = 1`.
  - **Act:** `Object resultado = loader.crearDocumentos(tareaFirma, values)`.
  - **Assert:** `assertSame(tareaFirma, resultado)` — el data-import persiste el bean devuelto; devolver otro crearía la tarea dos veces.
- **`crearDocumentos_numeroDocumentosComoCadena_loInterpretaComoNumero`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `values` con `numeroDocumentos = "2"` (el data-import de Axelor entrega los atributos XML como `String`).
  - **Act:** `loader.crearDocumentos(tareaFirma, values)`.
  - **Assert:** se crean 2 `DocumentoFirma`. *(Supuesto documentado: si al implementar se comprueba que el binding ya entrega un numérico, el test se mantiene igual porque la conversión debe aceptar ambas formas.)*
- **`crearDocumentos_numeroDocumentosAusente_creaUnUnicoDocumento`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `values` sin la clave `numeroDocumentos`.
  - **Act:** `loader.crearDocumentos(tareaFirma, values)`.
  - **Assert:** se crea **un** `DocumentoFirma` (valor por defecto razonable: siete de las ocho tareas de demo tienen uno). *(Decisión ante ambigüedad: el diseño no fija el comportamiento sin el alias; se elige el valor por defecto 1 en lugar de fallar, para que un `firmas-demo.xml` incompleto no impida arrancar.)*

---

## Clase: `com.educaflow.secretariavirtual.datademo.TareaFirmaDemoNotifier` — sin lógica testable
**Motivo:** notificador de demo **sin efectos**: `notify(TareaFirma, Object)` no hace nada (a lo sumo una traza `debug` con el id). No hay comportamiento que aseverar; su razón de existir —que `Class.forName(fqcnFirmaNotifier)` encuentre una clase— la comprueban los tests de `TareaFirmaDemoLoader` (`crearDocumentos_siempre_fijaElFqcnDelNotificadorDeDemo`); los tests de `TareaFirmaServiceImpl` usan su propia clase notificadora de prueba, no esta.

## Clase: `com.educaflow.subsystem.firmas.service.TareaFirmaService` — sin lógica testable
**Motivo:** interfaz (§1 del contrato: interfaces sin comportamiento no se testean). Sus tres métodos nuevos se testean sobre la implementación.

## Clase: `com.educaflow.subsystem.criptografia.service.CertificadoDigitalService` — sin lógica testable
**Motivo:** interfaz; el overload nuevo y su validador se testean sobre `CertificadoDigitalServiceImpl`.

## Clase: `com.educaflow.subsystem.firmas.db.TareaFirma` — sin lógica testable
**Motivo:** POJO de dominio **generado** por Axelor a partir de `domains/TareaFirma.xml`. `claveFirma` es un `@Transient` sin lógica y `getSituacionFirma()` es un getter generado cuyo único contenido delega en `SituacionFirmaBuilder.build(getFirmante())`, ya cubierto al 100 % por los tests de esa clase (ver Decisión 3 de §Convenciones).

## Clase: `com.educaflow.subsystem.firmas.db.SituacionFirma` — sin lógica testable
**Motivo:** enumerado generado, sin comportamiento. Sus seis valores se ejercitan desde `SituacionFirmaBuilder` y `validateFirmarEnServidor`.

---

## Cobertura

- **Clases con lógica descritas: 5.**
  1. `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl` (modificada) — 3 métodos, 17 tests.
  2. `com.educaflow.subsystem.firmas.util.SituacionFirmaBuilder` (nueva) — 1 método, 12 tests.
  3. `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl` (modificada) — 6 métodos públicos (+ 4 reglas privadas `fireActionRule_*` ejercidas a través de ellos), 46 tests.
  4. `com.educaflow.subsystem.firmas.controller.TareaFirmaController` (modificada) — 2 métodos, 8 tests.
  5. `com.educaflow.secretariavirtual.datademo.TareaFirmaDemoLoader` (nueva) — 1 método, 8 tests.
- **Clases omitidas (sin lógica):** `TareaFirmaDemoNotifier` (método sin efectos), `TareaFirmaService` e `CertificadoDigitalService` (interfaces), `TareaFirma` y `SituacionFirma` (dominio/enumerado generados por Axelor).
- **Ficheros del diseño sin clase Java** (no aplican a los tests unitarios): `domains/TareaFirma.xml`, `views/Pendiente-TareaFirma.xml`, `menus.xml`, `data-init/input/auth-firmas.xml`, `data-demo/input/firmas-demo.xml`, `data-demo/input-config.xml` y el PDF de ejemplo.
- **Reglas server-side cubiertas (`V`/`R`/`CC`):**
  - `V-TareaFirma-001` — `validateFirmarEnServidor_tareaYaFirmada…`, `…_tareaRechazada…`, `…_estadoNulo…`, `…_variasReglasIncumplidas…`, `firmarEnServidor_validacionRechazada_lanzaValidationExceptionYNoFirmaNiGuarda`, `…_tareaPendienteDelUsuario…` (rama OK).
  - `V-TareaFirma-002` — `…_firmanteDistintoDelUsuarioAutenticado…`, `…_sinUsuarioAutenticado…`, `…_variasReglasIncumplidas…`, rama OK en el test feliz.
  - `V-TareaFirma-003` — `…_situacionSinDni…`, rama OK en el test feliz.
  - `V-TareaFirma-004` — `…_situacionSinCertificado…`, `…_siempre_recalculaLaSituacionDeFirmaEnElServidor`, `…_variasReglasIncumplidas…`, rama OK en el test feliz.
  - `V-TareaFirma-005` — `…_dispositivoSinPinYSinClaveTecleada…`, `…_dispositivoSinPinYClaveEnBlanco…`, `…_dispositivoSinPinConPinTecleado…` (OK), `…_dispositivoConPinYSinClaveTecleada…` (no aplica), `…_ficheroConClaveGuardadaYSinClaveTecleada…`.
  - `V-TareaFirma-006` — `…_ficheroSinClaveYSinClaveTecleada…`, `…_ficheroSinClaveYClaveEnBlanco…`, `…_ficheroSinClaveConClaveTecleada…` (OK), `…_ficheroConClaveGuardadaYSinClaveTecleada…`.
  - `V-TareaFirma-007` — `…_tareaSinDocumentos…`, `…_documentosNulos…`, `…_variasReglasIncumplidas…`, rama OK en el test feliz.
  - `R-TareaFirma-001` — 9 tests de `firmarEnServidor` (un documento; dos documentos / `MetaFile` firmado distinto por documento; almacén por documento; `CampoFirma` con `.floatValue()`; DNI+clave al servicio de criptografía; «todo o nada»; mensaje `No se han podido firmar los documentos: `; fallo del almacén; PDF inválido) + 7 tests de `CertificadoDigitalServiceImpl` sobre §«Cuál clave se usa» (guardada gana, tecleada cuando no hay guardada, guardada en blanco, sin ninguna, `FICHERO_BD`, sistema de archivos, dispositivo que descarta el PIN).
  - `R-TareaFirma-002` — `firmarEnServidor_tareaValidaConUnDocumento…`, `firmarEnServidor_laFirmaFalla_dejaLaTareaPendiente…`, `firmarEnServidor_tareaValida_asignaEstadoYFechaDeResolucionIncondicionalmente`, `marcarComoFirmada_tareaValida…`, `marcarComoFirmada_fechaResolucionPreexistente_laSobrescribe`.
  - `R-TareaFirma-003` — `firmarEnServidor_firmaCompletada_descartaLaClaveDeFirma`, `…_laFirmaFalla_descartaLaClaveDeFirmaIgualmente`, `…_validacionRechazada_descartaLaClaveDeFirmaIgualmente`.
  - `R-TareaFirma-004` — `firmarEnServidor_firmaCompletada_notificaAlProcesoQueEncargoLaFirma`, `…_laFirmaFalla_noNotificaAlProcesoQueEncargoLaFirma`, `marcarComoFirmada_tareaValida_notificaAlProcesoQueEncargoLaFirma`.
  - `CC-TareaFirma-001` — los 11 tests de `SituacionFirmaBuilder.build` que declaran la regla (seis valores de la situación + DNI ausente/en blanco/inválido + firmante nulo + las dos degradaciones seguras ante error) y `validateFirmarEnServidor_siempre_recalculaLaSituacionDeFirmaEnElServidor`. El duodécimo test de `build` —`build_elServicioDeCriptografiaFalla_noRegistraElDniCompletoEnElLog`, el del DNI enmascarado en el log— declara `Verifica: —`: es un test sin regla asociada (k-secure-coding §8).
- **Reglas solo-cliente excluidas (E2E en `test-e2e-desc.md`, no se testean aquí):** `U-documentos-pendientes-de-firma-001`, `-002`, `-003`, `-004`, `-005`, `-006`, `-007`, `-008`, `-009`, `-010`, `-011`, `-012`, `-013`, `-014`, `-015`, `-016`, `-017`, `-018`. Todas viven en `showIf`/`title`/`widget`/`required` de `views/Pendiente-TareaFirma.xml` o en el encadenado de sus `action-group`/`action-record`/`action-validate`, capa no testeable con JUnit.
  - Caso particular: el refuerzo de cliente `…-Local-validateFirmarEnServidor-action` (los dos `<error>` con «El PIN es obligatorio» / «La contraseña es obligatoria») es XML y queda excluido; su **fuente de verdad** en el servidor, `V-TareaFirma-005` y `V-TareaFirma-006`, sí está cubierta arriba con los mismos literales.
