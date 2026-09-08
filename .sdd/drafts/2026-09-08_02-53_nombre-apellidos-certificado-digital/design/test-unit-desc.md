# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones

- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Aserciones con `org.junit.jupiter.api.Assertions` (`assertEquals`, `assertThrows`, `assertTrue`, `assertNull`…), **no** AssertJ, igual que los tests ya existentes del subsistema.
- Datos de referencia usados en todos los ejemplos (los del spec): DNI con usuario titular `29050788V` → usuario `secretario@mislata.es`, nombre «Secretario», apellidos «CIPFP Mislata»; DNI sin usuario `12345678Z`; DNI con formato inválido `12345678A`; ruta classpath `firma/mi_certificado.p12`.
- **`enabled` vale `true` en un `CertificadoDigital` recién instanciado** (`<boolean name="enabled" default="true">` del Paso 2 del diseño; el getter generado por Axelor devuelve ese valor por defecto). Como `V-CertificadoDigital-005` solo consulta `repository.findByDniHabilitados(dni).fetch()` cuando el bean que se guarda está habilitado, **todo test MUST dejar explícito en su `Arrange` uno de los dos casos**: o fija `enabled = false` en el bean que se guarda (escenario que **no** ejerce V-005 y en el que el finder **no** se invoca), o programa el stub `repository.findByDniHabilitados(<dni>)` → `Query` cuyo `fetch()` devuelve la lista correspondiente (normalmente vacía). Sin ninguna de las dos cosas el mock devolvería `null` y el `.fetch()` rompería el escenario descrito.
- **«Certificado por lo demás válido»** — `validateInsert` y `validateUpdate` delegan **siempre** en el helper `validateCertificado(certificado, messages)` (Paso 4.2 del diseño), que exige `dni` con formato válido, `tipoCertificado` obligatorio y los campos que ese tipo requiere; y `insert`/`update` invocan esa validación **en su primera línea**, antes de las action rules y de `repository.save` (Paso 4.1). Por eso **todo test cuyo `Assert` sea `Optional.empty()`, un mensaje concreto, un valor asignado por una action rule o `verify(repository).save(...)` MUST partir de un bean que supere `validateCertificado`**; en este fichero esa fórmula es siempre la misma: `tipoCertificado` = `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` (recurso que existe en el classpath de test) y un `dni` con formato válido. Cuando el `Arrange` de un test dice «certificado (o bean) **por lo demás válido**» —o cualquiera de sus variantes abreviadas: «certificado válido», «certificado `CLASSPATH` válido», «original y bean válidos», «el resto de campos válidos/correctos»— se refiere **exactamente a esa misma fórmula**, `rutaClasspath` incluida: sin ella `validateCertificado` añadiría el mensaje «La ruta classpath es obligatoria» y los `Assert` que cuentan mensajes («**un** mensaje», «los **dos** mensajes») o esperan `Optional.empty()` serían inalcanzables.
- **En `update` y `validateUpdate` el bean ENTRANTE MUST llevar su propio `dni` con formato válido** (el del original, `29050788V`, o el manipulado `12345678Z` cuando el escenario lo pida). Motivo: `validateCertificado` valida el **bean entrante**, y `fireActionRule_ConservarDni` restaura el DNI del original **después** de la validación (Paso 4.1: primero `validateUpdate`, luego las action rules). Un bean entrante sin `dni` acumularía el mensaje «El DNI no es válido» y rompería el `Assert` descrito.
- **«Certificado de fichero legible»** — en `getAlmacenClaveByDni` la rama de fichero (`CLASSPATH`, `FICHERO_BD`, `SISTEMA_ARCHIVOS`) construye `new AlmacenClaveFichero(getInputStreamCertificado(certificado), clave)` (Paso 4.1 del diseño), y ese constructor **lanza `RuntimeException`** si el `InputStream` es nulo («El fileCertificate no puede ser null») o si la clave efectiva es nula («El password no puede ser null»). Por eso **todo test cuyo `Assert` sea un `AlmacenClaveFichero` MUST fijar en su `Arrange` las dos cosas**: (a) una **ubicación legible** —para `CLASSPATH`, una `rutaClasspath` que exista en el classpath de test: `firma/mi_certificado.p12` o `firma/instalar_certificado_criptografico/secretario.p12`, ambos en `src/main/resources`— y (b) una **clave efectiva no nula**, sea la `password` guardada en el certificado o la `claveAcceso` del `Act` (la guardada gana salvo que sea nula o esté en blanco). El test ya existente `getAlmacenClaveByDni_ficheroSinClaveGuardadaNiTecleada_lanzaExcepcion` es justo el caso contrario y sigue esperando esa `RuntimeException`.
- **`nombreTomadoDelUsuario` vale `false` en un `CertificadoDigital` recién instanciado** (`<boolean name="nombreTomadoDelUsuario" default="false">` del Paso 2; el getter generado es null-safe y devuelve `FALSE`). Como `V-CertificadoDigital-003` y `V-CertificadoDigital-004` aplican **solo si `certificadoOriginal.getNombreTomadoDelUsuario()` es false**, **todo test de `update`/`validateUpdate` MUST dejar explícito en su `Arrange` uno de los dos casos**: o `certificadoOriginal.nombreTomadoDelUsuario = true` (V-003/-004 no aplican), o el flag a `false` **y** `nombre`/`apellidos` no vacíos en el bean entrante (aplican, pero no emiten mensajes). La excepción son los tests que buscan precisamente esos mensajes de obligatoriedad.

---

## Clase: `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl`  —  servicio

**Responsabilidad:** `ModelService` de `CertificadoDigital`. Con este delta pasa a: (a) sobrescribir `insert`/`update` con sus action rules (R-CertificadoDigital-001/-002/-003), (b) reescribir `validateInsert`/`validateUpdate` con V-CertificadoDigital-001…-005, (c) declarar las whitelists `allowPropertiesInsert`/`allowPropertiesUpdate`, (d) exponer la acción escalar de solo lectura `getDatosTitularByDni` con su validador y (e) obtener el certificado de una persona por el **habilitado** de su DNI (helper de lectura `getCertificadoHabilitado`) en `getAlmacenClaveByDni` y `getSituacionFirmaByDni`.

**Colaboradores a mockear:**
- `CertificadoDigitalRepository` — mock pasado al constructor `new CertificadoDigitalServiceImpl(CertificadoDigital.class, repository)` (el campo heredado `repository` de `DefaultModelService`). El finder nuevo `findByDniHabilitados(String)` devuelve un `com.axelor.db.Query<CertificadoDigital>`, así que hay que mockear **también** el `Query` y encadenar `fetchOne()` (lectura del vigente) o `fetch()` (lista para V-005). `Query` es una clase pública no final: se mockea con `Mockito.mock(Query.class)` sin necesidad de mock-maker inline.
- `com.axelor.auth.db.repo.UserRepository` — colaborador nuevo, campo privado `@Inject` de la clase. En el test se inyecta por **reflexión** con un helper `setField(service, "userRepository", userRepository)`, exactamente como hace `TareaFirmaControllerTest` con su `modelServiceFactory` (no hay contenedor Guice en un test unitario). Se stubea `userRepository.findByDni(dni)`.
- `MetaFileUtil` (estático, `mockStatic`) — solo en los tests de `getAlmacenClaveByDni` con `tipoCertificado = FICHERO_BD`, igual que hoy.
- **Sin base de datos**: ni JPA real, ni `JpaRepository.of(...)`, ni `Beans.get(...)`. `SecurityUtil` no interviene (la entidad no es multi-centro y el diseño no consulta el usuario autenticado).
- Entidades (`CertificadoDigital`, `User`, `Alias`, `DispositivoCriptografico`) se instancian con `new` y se rellenan con setters; nunca se mockean.

**Origen diseño:** Paso 4 de `design.md` (4.1 Acciones, 4.2 Validaciones, 4.3 AllowProperties, 4.4 Action Rules, 4.5 Otras funciones), `design/rules/R-CertificadoDigital-001.md` y la matriz «Trazabilidad Origen spec → V/R/U → ubicación». Reglas cubiertas: V-CertificadoDigital-001…-005, R-CertificadoDigital-001/-002/-003 y CC-CertificadoDigital-001.

**Tests existentes a adaptar** (`src/test/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImplTest.java`, ver `design.md` nota 11): los ~15 tests de `getAlmacenClaveByDni` (y sus dos overloads) stubean hoy `repository.findByDni(DNI)`, finder que el Paso 2 elimina. **MUST** reapuntarse a `repository.findByDniHabilitados(DNI)` + `query.fetchOne()`, y los dos casos que hoy comprueban «entrada deshabilitada → null» (`getAlmacenClaveByDni_entradaDeshabilitada_devuelveNullIgualQueSiNoExistiera`, `getAlmacenClaveByDni_certificadoDeshabilitado_devuelveNull`) pasan a expresarse como «el finder de habilitados no devuelve nada» (`fetchOne()` → `null`), porque el filtro por `enabled` se ha movido al finder y el chequeo `certificado.getEnabled() == false` desaparece del servicio («Eliminaciones declaradas»). El `verify(repository, never()).findByDni(any())` del test de DNI inválido pasa a `never()).findByDniHabilitados(any())`. El `setUp` debe crear además el mock de `UserRepository` e inyectarlo. Los helpers de construcción de certificados (`certificadoClasspath`, `certificadoFicheroBd`, …) se conservan y se les añaden `nombre`, `apellidos` y `nombreTomadoDelUsuario` donde el test lo necesite.

### Método: `CertificadoDigital insert(CertificadoDigital certificado)`

- **`insert_dniDeUsuarioExistente_asignaNombreYApellidosDeLaFichaYMarcaElFlag`** — Tipo: happy. Verifica: `R-CertificadoDigital-001`, `CC-CertificadoDigital-001`.
  - **Arrange:** certificado válido de tipo `CLASSPATH` con `dni` = `29050788V`, `enabled` = `true`, `nombre` y `apellidos` vacíos; mock `userRepository.findByDni("29050788V")` → `User` con `nombre` «Secretario» y `apellidos` «CIPFP Mislata»; mock `repository.findByDniHabilitados("29050788V")` → `Query` cuyo `fetch()` devuelve lista vacía; mock `repository.save(certificado)` → el mismo certificado.
  - **Act:** `insert(certificado)`.
  - **Assert:** `certificado.getNombre()` = «Secretario», `getApellidos()` = «CIPFP Mislata», `getNombreTomadoDelUsuario()` = `true`; `verify(repository).save(certificado)`.
- **`insert_dniSinUsuario_conservaNombreYApellidosDelAdministradorYDejaElFlagAFalse`** — Tipo: happy. Verifica: `R-CertificadoDigital-001`, `CC-CertificadoDigital-001`.
  - **Arrange:** certificado válido con `dni` = `12345678Z`, `nombre` «Ana», `apellidos` «García López»; mock `userRepository.findByDni("12345678Z")` → `null`; `findByDniHabilitados` → `Query` con `fetch()` vacío; `repository.save(...)` → el certificado.
  - **Act:** `insert(certificado)`.
  - **Assert:** `getNombre()` = «Ana», `getApellidos()` = «García López», `getNombreTomadoDelUsuario()` = `false`; `verify(repository).save(certificado)`.
- **`insert_dniDeUsuarioExistente_descartaElNombreYElFlagQueEnviaElCliente`** — Tipo: borde (seguridad, mass-assignment). Verifica: `R-CertificadoDigital-001`, `CC-CertificadoDigital-001`.
  - **Arrange:** igual que el primero pero el bean entrante trae `nombre` «Impostor», `apellidos` «Falso» y `nombreTomadoDelUsuario` = `true` (valores que un atacante podría colar por `/ws/rest/<FQN>`); `userRepository.findByDni("29050788V")` → el usuario «Secretario» / «CIPFP Mislata».
  - **Act:** `insert(certificado)`.
  - **Assert:** `getNombre()` = «Secretario» y `getApellidos()` = «CIPFP Mislata» (los del cliente se descartan sin condición); `getNombreTomadoDelUsuario()` = `true`.
- **`insert_dniSinUsuarioYFlagTrueDelCliente_fuerzaElFlagAFalse`** — Tipo: borde (seguridad). Verifica: `R-CertificadoDigital-001`, `CC-CertificadoDigital-001`.
  - **Arrange:** certificado por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`) con `dni` = `12345678Z`, `nombre` «Ana», `apellidos` «García López» y `nombreTomadoDelUsuario` = `true` enviado por el cliente, `enabled` = `true` (el valor por defecto); `userRepository.findByDni("12345678Z")` → `null`; mock `repository.findByDniHabilitados("12345678Z")` → `Query` cuyo `fetch()` devuelve lista vacía (el bean va habilitado, así que V-CertificadoDigital-005 sí consulta el finder); `repository.save(...)` → el certificado.
  - **Act:** `insert(certificado)`.
  - **Assert:** `getNombreTomadoDelUsuario()` = `false` (la asignación es incondicional en las dos ramas; sin guarda `if (campo == null)`), `getNombre()` = «Ana».
- **`insert_usuarioTitularConNombreVacioEnSuFicha_copiaLosValoresVaciosYMarcaElFlag`** — Tipo: borde. Verifica: `R-CertificadoDigital-001`.
  - **Arrange:** certificado por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`) con `dni` = `29050788V` y `enabled` = `true` (el valor por defecto); `userRepository.findByDni(...)` → `User` con `nombre` = `null` y `apellidos` = `null`; el bean entrante trae «Ana» / «García López»; mock `repository.findByDniHabilitados("29050788V")` → `Query` cuyo `fetch()` devuelve lista vacía (el bean va habilitado, así que V-CertificadoDigital-005 sí consulta el finder); `repository.save(...)` → el certificado.
  - **Act:** `insert(certificado)`.
  - **Assert:** `getNombre()` = `null` y `getApellidos()` = `null` (se copia la ficha tal cual, sin *fallback* al valor del cliente — tabla de errores de `rules/R-CertificadoDigital-001.md`); `getNombreTomadoDelUsuario()` = `true`.
- **`insert_variosUsuariosConElMismoDni_propagaLaExcepcionDelFinder`** — Tipo: error. Verifica: `R-CertificadoDigital-001`.
  - **Arrange:** certificado por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`) con `dni` = `29050788V` y `enabled` = `false` (para que V-CertificadoDigital-005 no consulte el finder antes de llegar a la resolución del titular); `userRepository.findByDni("29050788V")` programado para lanzar `jakarta.persistence.NonUniqueResultException` (incoherencia de los datos maestros de usuarios).
  - **Act:** `insert(certificado)`.
  - **Assert:** la excepción se propaga sin convertirse en `BusinessMessages` (`assertThrows(NonUniqueResultException.class, …)`); `verify(repository, never()).save(any())`.
- **`insert_certificadoInvalido_lanzaValidationExceptionYNoPersiste`** — Tipo: error. Verifica: `V-CertificadoDigital-001`, `V-CertificadoDigital-002`.
  - **Arrange:** certificado con `dni` = `12345678Z` (sin usuario: `userRepository.findByDni` → `null`), `nombre` y `apellidos` nulos, `enabled` = `false` (así V-CertificadoDigital-005 no consulta el finder y el escenario se centra en V-001/-002), resto de campos válidos.
  - **Act:** `insert(certificado)`.
  - **Assert:** lanza `jakarta.validation.ValidationException` (vía `BusinessMessages::throwIfInvalid`) cuyo texto contiene «El nombre es obligatorio» y «Los apellidos son obligatorios»; `verify(repository, never()).save(any())`.
- **`insert_certificadoValido_devuelveLoQueDevuelveRepositorySave`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** certificado válido con usuario titular (`dni` = `29050788V`, `userRepository.findByDni("29050788V")` → el usuario «Secretario»), `enabled` = `true` (el valor por defecto); mock `repository.findByDniHabilitados("29050788V")` → `Query` cuyo `fetch()` devuelve lista vacía; `repository.save(certificado)` → una instancia distinta `certificadoPersistido`.
  - **Act:** `insert(certificado)`.
  - **Assert:** el retorno es **la misma referencia** que devolvió `repository.save` (`assertSame`); nunca se usa `super.insert`.

### Método: `CertificadoDigital update(CertificadoDigital certificado, CertificadoDigital certificadoOriginal)`

- **`update_clienteCambiaElDni_restauraElDniDelOriginal`** — Tipo: borde (seguridad). Verifica: `R-CertificadoDigital-002`.
  - **Arrange:** original con `id` = 1, `dni` = `29050788V`, `nombreTomadoDelUsuario` = `true`, `nombre`/`apellidos` de la ficha; bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`) con `id` = 1 y `dni` = `12345678Z` (formato válido, cambiado por el cliente — el que `validateCertificado` valida antes de que la action rule lo restaure), `enabled` = `false`; `repository.save(...)` → el bean.
  - **Act:** `update(certificado, certificadoOriginal)`.
  - **Assert:** `certificado.getDni()` = `29050788V` (restaurado incondicionalmente); `verify(repository).save(certificado)`.
- **`update_originalConNombreTomadoDelUsuario_restauraNombreApellidosYFlagDelOriginal`** — Tipo: happy. Verifica: `R-CertificadoDigital-003`, `CC-CertificadoDigital-001`.
  - **Arrange:** original con `id` = 1, `dni` = `29050788V`, `nombreTomadoDelUsuario` = `true`, `nombre` «Secretario», `apellidos` «CIPFP Mislata»; bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`, `dni` = `29050788V`) con `nombre` «Otro», `apellidos` «Distintos» y `nombreTomadoDelUsuario` = `false`; `enabled` = `false` en ambos (para no disparar la consulta de V-005); `repository.save(...)` → el bean.
  - **Act:** `update(certificado, certificadoOriginal)`.
  - **Assert:** `getNombre()` = «Secretario», `getApellidos()` = «CIPFP Mislata», `getNombreTomadoDelUsuario()` = `true`.
- **`update_originalConNombreEscritoPorElAdministrador_conservaElNombreDelFormularioYElFlagAFalse`** — Tipo: happy. Verifica: `R-CertificadoDigital-003`, `CC-CertificadoDigital-001`.
  - **Arrange:** original con `id` = 1, `dni` = `12345678Z`, `nombreTomadoDelUsuario` = `false`, `nombre` «Ana», `apellidos` «García López»; bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`, `dni` = `12345678Z`) con `nombre` «Ana», `apellidos` corregidos a «García Pérez» y `nombreTomadoDelUsuario` = `true` (enviado por el cliente); `enabled` = `false` en ambos (para no disparar la consulta de V-005); `repository.save(...)` → el bean. El bean entrante lleva `nombre` y `apellidos` no vacíos, así que V-CertificadoDigital-003/-004 —que sí aplican, porque el flag del original es `false`— no emiten mensajes.
  - **Act:** `update(certificado, certificadoOriginal)`.
  - **Assert:** `getApellidos()` = «García Pérez» (se acepta la corrección del administrador), `getNombre()` = «Ana», `getNombreTomadoDelUsuario()` = `false` (restaurado del original, el valor del cliente se descarta).
- **`update_originalSinFlagAsignado_seComportaComoEscritoPorElAdministrador`** — Tipo: borde. Verifica: `R-CertificadoDigital-003`, `CC-CertificadoDigital-001`.
  - **Arrange:** original con `id` = 1, `dni` = `12345678Z`, recién instanciado sin `setNombreTomadoDelUsuario` (fila anterior a este cambio; el getter generado por Axelor es null-safe y devuelve `FALSE`), con `nombre`/`apellidos` rellenos a mano; bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`, `dni` = `12345678Z`) con `nombre` «Ana» y `apellidos` «García Pérez» nuevos (no vacíos, para que V-003/-004 —que aplican, porque el flag del original es `FALSE`— no emitan mensajes); `enabled` = `false` en ambos (para no disparar la consulta de V-005); `repository.save(...)` → el bean.
  - **Act:** `update(certificado, certificadoOriginal)`.
  - **Assert:** el nombre y los apellidos del formulario se conservan; `getNombreTomadoDelUsuario()` = `false`.
- **`update_certificadoInvalido_lanzaValidationExceptionYNoPersiste`** — Tipo: error. Verifica: `V-CertificadoDigital-003`, `V-CertificadoDigital-004`.
  - **Arrange:** original con `id` = 1, `dni` = `12345678Z` y `nombreTomadoDelUsuario` = `false`; bean entrante con `nombre` y `apellidos` nulos y el resto válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` y `dni` = `12345678Z`, para que los únicos mensajes sean los de V-003/-004); `enabled` = `false` en ambos (así V-CertificadoDigital-005 no consulta el finder y el escenario se centra en V-003/-004).
  - **Act:** `update(certificado, certificadoOriginal)`.
  - **Assert:** lanza `ValidationException` con «El nombre es obligatorio» y «Los apellidos son obligatorios»; `verify(repository, never()).save(any())`.
- **`update_certificadoValido_devuelveLoQueDevuelveRepositorySave`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** original y bean válidos, con el mismo `dni` = `29050788V` (también en el bean entrante), tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`, `nombre` «Ana» y `apellidos` «García López» en el bean entrante, `nombreTomadoDelUsuario` = `false` en el original y `enabled` = `false` en ambos; `repository.save(certificado)` → instancia distinta.
  - **Act:** `update(certificado, certificadoOriginal)`.
  - **Assert:** el retorno es la misma referencia que devolvió `repository.save` (`assertSame`); nunca se usa `super.update`.

### Método: `Optional<BusinessMessages> validateInsert(CertificadoDigital certificado)`

- **`validateInsert_sinUsuarioTitularYSinNombre_devuelveMensajeElNombreEsObligatorio`** — Tipo: error. Verifica: `V-CertificadoDigital-001`.
  - **Arrange:** certificado `CLASSPATH` válido con `dni` = `12345678Z`, `nombre` = `null`, `apellidos` = «García López», `enabled` = `false`; `userRepository.findByDni("12345678Z")` → `null`.
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** `Optional` presente con **un** mensaje anclado al campo `nombre` y texto exacto «El nombre es obligatorio».
- **`validateInsert_sinUsuarioTitularYSinApellidos_devuelveMensajeLosApellidosSonObligatorios`** — Tipo: error. Verifica: `V-CertificadoDigital-002`.
  - **Arrange:** igual, con `nombre` = «Ana» y `apellidos` = `null`.
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** un mensaje anclado a `apellidos` con texto exacto «Los apellidos son obligatorios».
- **`validateInsert_sinUsuarioTitularYSinNombreNiApellidos_devuelveLosDosMensajes`** — Tipo: error. Verifica: `V-CertificadoDigital-001`, `V-CertificadoDigital-002`.
  - **Arrange:** certificado por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`, `dni` = `12345678Z`) con `nombre` y `apellidos` nulos, `enabled` = `false` (para no disparar la consulta de V-005), sin usuario titular (`userRepository.findByDni("12345678Z")` → `null`).
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** los **dos** mensajes en el mismo `BusinessMessages` (no hay early return entre ellos).
- **`validateInsert_sinUsuarioTitularYNombreEnBlanco_devuelveMensajeElNombreEsObligatorio`** — Tipo: borde. Verifica: `V-CertificadoDigital-001`.
  - **Arrange:** certificado por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`, `dni` = `12345678Z`) con `nombre` = `"   "` (solo espacios), `apellidos` = «García López», `enabled` = `false` (para no disparar la consulta de V-005), sin usuario titular (`userRepository.findByDni("12345678Z")` → `null`).
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** mensaje «El nombre es obligatorio» (la comprobación es «nulo o en blanco», no solo nulo).
- **`validateInsert_conUsuarioTitular_noExigeNombreNiApellidos`** — Tipo: happy. Verifica: `V-CertificadoDigital-001`, `V-CertificadoDigital-002`.
  - **Arrange:** certificado por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`) con `dni` = `29050788V` y `nombre` y `apellidos` nulos (el formulario los envía vacíos porque los pone el servidor); `userRepository.findByDni("29050788V")` → el usuario «Secretario»; `enabled` = `false`.
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** `Optional.empty()` — la validación no aplica cuando existe usuario titular.
- **`validateInsert_habilitadoYYaExisteOtroHabilitadoConEseDni_devuelveMensajeDeCertificadoHabilitadoDuplicado`** — Tipo: error. Verifica: `V-CertificadoDigital-005`.
  - **Arrange:** certificado nuevo (`id` = `null`) por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`) con `dni` = `29050788V` y `enabled` = `true`; `repository.findByDniHabilitados("29050788V")` → `Query` cuyo `fetch()` devuelve una lista con un certificado de `id` = 1; `userRepository.findByDni(...)` → el usuario «Secretario» (así V-001/-002 no aplican y el único mensaje es el de V-005).
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** mensaje anclado al campo `enabled` con el texto de RES-CertificadoDigital-001 incluyendo el DNI: «Ya existe un certificado digital habilitado para el DNI 29050788V».
- **`validateInsert_habilitadoYSinOtrosHabilitadosDeEseDni_devuelveOptionalVacio`** — Tipo: happy. Verifica: `V-CertificadoDigital-005`.
  - **Arrange:** certificado válido y habilitado con usuario titular; `findByDniHabilitados(...)` → `Query` con `fetch()` = lista vacía.
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_deshabilitado_noConsultaLosHabilitadosDelDni`** — Tipo: borde. Verifica: `V-CertificadoDigital-005`.
  - **Arrange:** certificado válido con `enabled` = `false` y usuario titular (ESC-006: segundo certificado del mismo DNI, deshabilitado).
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** `Optional.empty()` y `verify(repository, never()).findByDniHabilitados(any())` — si no se habilita, no hay nada que comprobar.
- **`validateInsert_dniRepetidoEnCertificadosDeshabilitados_noRechazaPorDniDuplicado`** — Tipo: happy (regresión de la eliminación declarada). Verifica: `V-CertificadoDigital-005`.
  - **Arrange:** certificado nuevo habilitado, por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`), con `dni` = `29050788V`; `findByDniHabilitados(...)` → `fetch()` vacío (los otros certificados de ese DNI están deshabilitados, así que el finder no los devuelve); usuario titular presente.
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** `Optional.empty()` — el mensaje preexistente «Ya existe un certificado digital con el DNI …» ya no se emite (RES-CertificadoDigital-002 retira la unicidad del DNI).
- **`validateInsert_dniConFormatoInvalido_devuelveMensajeElDniNoEsValido`** — Tipo: error (regresión de `validateCertificado`). Verifica: `—`.
  - **Arrange:** certificado con `dni` = `12345678A` (letra incorrecta), `enabled` = `false` (para no disparar la consulta de V-005, que corre igualmente aunque el DNI tenga formato inválido) y el resto de campos correctos, incluidos `nombre` «Ana» y `apellidos` «García López»; `userRepository.findByDni("12345678A")` → `null`.
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** mensaje anclado a `dni` con texto «El DNI no es válido».
- **`validateInsert_certificadoCompletoConUsuarioTitular_devuelveOptionalVacio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** certificado `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` (recurso que existe en el classpath de test), `dni` = `29050788V`, `enabled` = `true`; usuario titular presente; `findByDniHabilitados(...)` → `fetch()` vacío.
  - **Act:** `validateInsert(certificado)`.
  - **Assert:** `Optional.empty()`.

### Método: `Optional<BusinessMessages> validateUpdate(CertificadoDigital certificado, CertificadoDigital certificadoOriginal)`

- **`validateUpdate_originalEscritoPorElAdministradorYSinNombre_devuelveMensajeElNombreEsObligatorio`** — Tipo: error. Verifica: `V-CertificadoDigital-003`.
  - **Arrange:** original con `id` = 1, `dni` = `12345678Z`, `nombreTomadoDelUsuario` = `false`; bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` y `dni` = `12345678Z`, para que el único mensaje sea el de V-003) con `nombre` = `null`, `apellidos` = «García López», `enabled` = `false`.
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** mensaje anclado a `nombre` con texto «El nombre es obligatorio».
- **`validateUpdate_originalEscritoPorElAdministradorYSinApellidos_devuelveMensajeLosApellidosSonObligatorios`** — Tipo: error. Verifica: `V-CertificadoDigital-004`.
  - **Arrange:** igual con `nombre` = «Ana» y `apellidos` = `null` (o en blanco).
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** mensaje anclado a `apellidos` con texto «Los apellidos son obligatorios».
- **`validateUpdate_originalConNombreTomadoDelUsuario_noExigeNombreNiApellidos`** — Tipo: happy. Verifica: `V-CertificadoDigital-003`, `V-CertificadoDigital-004`.
  - **Arrange:** original con `id` = 1, `dni` = `29050788V` y `nombreTomadoDelUsuario` = `true`; bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` y `dni` = `29050788V`) con `nombre` y `apellidos` nulos (llegan vacíos porque en la vista son de solo lectura), `enabled` = `false`.
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** `Optional.empty()`.
- **`validateUpdate_flagDelClienteATrueSobreOriginalConFlagFalse_sigueExigiendoNombreYApellidos`** — Tipo: borde (seguridad). Verifica: `V-CertificadoDigital-003`, `V-CertificadoDigital-004`.
  - **Arrange:** original con `id` = 1, `dni` = `12345678Z` y `nombreTomadoDelUsuario` = `false`; bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` y `dni` = `12345678Z`, para que los únicos mensajes sean los de V-003/-004) con `nombreTomadoDelUsuario` = `true` y `nombre`/`apellidos` nulos; `enabled` = `false` en ambos (para no disparar la consulta de V-005).
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** se emiten los dos mensajes de obligatoriedad — la condición se evalúa sobre el **original**, no sobre el bean entrante.
- **`validateUpdate_originalSinFlagAsignado_exigeNombreYApellidos`** — Tipo: borde. Verifica: `V-CertificadoDigital-003`, `V-CertificadoDigital-004`.
  - **Arrange:** original de una fila anterior a este cambio: `id` = 1, `dni` = `12345678Z`, sin `setNombreTomadoDelUsuario` (getter null-safe → `FALSE`) y con `nombre`/`apellidos` vacíos; bean entrante también sin nombre ni apellidos, pero por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` y `dni` = `12345678Z`, para que los únicos mensajes sean los de V-003/-004); `enabled` = `false` en ambos (para no disparar la consulta de V-005).
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** los dos mensajes de obligatoriedad.
- **`validateUpdate_consultaLosHabilitadosConElDniDelOriginalNoConElDelBeanEntrante`** — Tipo: borde (seguridad). Verifica: `V-CertificadoDigital-005`.
  - **Arrange:** original con `id` = 1, `dni` = `29050788V` y `nombreTomadoDelUsuario` = `true` (así V-003/-004 no aplican y el escenario se centra en V-005); bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12`) con `id` = 1, `dni` = `12345678Z` (manipulado por el cliente, con formato válido para que `validateCertificado` no añada su propio mensaje) y `enabled` = `true`; `repository.findByDniHabilitados("29050788V")` → `Query` con `fetch()` vacío.
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** `verify(repository).findByDniHabilitados("29050788V")` y `verify(repository, never()).findByDniHabilitados("12345678Z")`.
- **`validateUpdate_habilitandoCuandoOtroDelMismoDniYaEstaHabilitado_devuelveMensajeDeCertificadoHabilitadoDuplicado`** — Tipo: error. Verifica: `V-CertificadoDigital-005`.
  - **Arrange:** original con `id` = 2, `dni` = `29050788V`, `enabled` = `false` y `nombreTomadoDelUsuario` = `true` (así V-003/-004 no aplican y el único mensaje es el de V-005); bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` y `dni` = `29050788V`) con `id` = 2 y `enabled` = `true`; `findByDniHabilitados("29050788V")` → `fetch()` con un certificado de `id` = 1 (ESC-008).
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** mensaje anclado a `enabled`: «Ya existe un certificado digital habilitado para el DNI 29050788V».
- **`validateUpdate_habilitadoYElUnicoHabilitadoEsElPropioRegistro_devuelveOptionalVacio`** — Tipo: borde. Verifica: `V-CertificadoDigital-005`.
  - **Arrange:** original con `id` = 1, `dni` = `29050788V`, `enabled` = `true` y `nombreTomadoDelUsuario` = `true` (así V-CertificadoDigital-003/-004 no aplican; con el `false` por defecto exigirían nombre y apellidos y el retorno no sería vacío); bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` y `dni` = `29050788V`) con `id` = 1 y `enabled` = `true`; `findByDniHabilitados(...)` → `fetch()` con **ese mismo** certificado (`id` = 1).
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** `Optional.empty()` — la comparación de ids excluye el propio registro (guardar sin tocar el flag no debe fallar).
- **`validateUpdate_deshabilitando_noConsultaLosHabilitadosDelDni`** — Tipo: borde. Verifica: `V-CertificadoDigital-005`.
  - **Arrange:** original con `id` = 1, `dni` = `29050788V`, `enabled` = `true` y `nombreTomadoDelUsuario` = `true` (así V-CertificadoDigital-003/-004 no aplican; con el `false` por defecto exigirían nombre y apellidos y el retorno no sería vacío); bean entrante por lo demás válido (tipo `CLASSPATH` con `rutaClasspath` = `firma/mi_certificado.p12` y `dni` = `29050788V`) con `id` = 1 y `enabled` = `false` (ESC-009, paso 4).
  - **Act:** `validateUpdate(certificado, certificadoOriginal)`.
  - **Assert:** `Optional.empty()` y `verify(repository, never()).findByDniHabilitados(any())`.

### Método: `Optional<BusinessMessages> validateGetDatosTitularByDni(String dni)`

- **`validateGetDatosTitularByDni_dniValido_devuelveOptionalVacio`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** sin mocks programados; `dni` = `29050788V`.
  - **Act:** `validateGetDatosTitularByDni("29050788V")`.
  - **Assert:** `Optional.empty()`.
- **`validateGetDatosTitularByDni_dniNuloEnBlancoOConFormatoInvalido_devuelveOptionalVacio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** tres invocaciones (o `@ParameterizedTest`) con `null`, `"   "` y `"1234"` — el administrador está tecleando.
  - **Act:** `validateGetDatosTitularByDni(<cada valor>)`.
  - **Assert:** `Optional.empty()` en los tres casos; la acción no tiene precondiciones de negocio y no puede dar un error espurio en el `onChange`.
- **`validateGetDatosTitularByDni_noConsultaNingunRepositorio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `dni` = `12345678A` (formato inválido).
  - **Act:** `validateGetDatosTitularByDni("12345678A")`.
  - **Assert:** `verifyNoInteractions(repository, userRepository)`.

### Método: `DatosTitular getDatosTitularByDni(String dni)`

*(Acción escalar de solo lectura; alimenta la parte de servidor de las reglas de UI `U-certificados-digitales-001`/`-002`, que como reglas de vista se validan en E2E, no aquí.)*

- **`getDatosTitularByDni_dniDeUsuarioExistente_devuelveNombreApellidosYTomadoDelUsuarioTrue`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `userRepository.findByDni("29050788V")` → `User` con «Secretario» / «CIPFP Mislata».
  - **Act:** `getDatosTitularByDni("29050788V")`.
  - **Assert:** `nombre()` = «Secretario», `apellidos()` = «CIPFP Mislata», `tomadoDelUsuario()` = `true`.
- **`getDatosTitularByDni_dniSinUsuario_devuelveDatosTitularSinUsuario`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `userRepository.findByDni("12345678Z")` → `null`.
  - **Act:** `getDatosTitularByDni("12345678Z")`.
  - **Assert:** `nombre()` y `apellidos()` nulos, `tomadoDelUsuario()` = `false` (es lo que devuelve la factoría `DatosTitular.sinUsuario()`).
- **`getDatosTitularByDni_dniNulo_devuelveDatosTitularSinUsuarioYNoConsultaElRepositorio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** sin stubs.
  - **Act:** `getDatosTitularByDni(null)`.
  - **Assert:** resultado equivalente a `DatosTitular.sinUsuario()`; `verify(userRepository, never()).findByDni(any())`.
- **`getDatosTitularByDni_dniEnBlanco_devuelveDatosTitularSinUsuarioYNoConsultaElRepositorio`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** sin stubs; `dni` = `"   "`.
  - **Act:** `getDatosTitularByDni("   ")`.
  - **Assert:** ídem al anterior.
- **`getDatosTitularByDni_dniIncompletoOConLetraIncorrecta_noLanzaYDevuelveSinUsuario`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `userRepository.findByDni("1234")` → `null`; `dni` = `"1234"` (el administrador está a medio teclear).
  - **Act:** `getDatosTitularByDni("1234")`.
  - **Assert:** no lanza ninguna excepción y devuelve el resultado «sin usuario»; el formato del DNI no se valida en esta acción.
- **`getDatosTitularByDni_cualquierDni_noPersisteNada`** — Tipo: borde (seguridad). Verifica: `—`.
  - **Arrange:** `userRepository.findByDni("29050788V")` → el usuario «Secretario».
  - **Act:** `getDatosTitularByDni("29050788V")`.
  - **Assert:** `verify(repository, never()).save(any())` y `verify(repository, never()).remove(any())` — es una consulta de solo lectura.
- **`getDatosTitularByDni_yInsert_conElMismoDni_resuelvenElMismoTitular`** — Tipo: borde. Verifica: `R-CertificadoDigital-001`.
  - **Arrange:** un único stub `userRepository.findByDni("29050788V")` → «Secretario» / «CIPFP Mislata»; certificado válido con ese DNI listo para insertar (`findByDniHabilitados` → `fetch()` vacío, `repository.save` → el bean).
  - **Act:** `getDatosTitularByDni("29050788V")` y después `insert(certificado)`.
  - **Assert:** el `nombre`, los `apellidos` y el flag del `DatosTitular` devuelto coinciden con los que quedan asignados en el certificado insertado — la pantalla y el guardado comparten el mismo cálculo y no pueden divergir.

### Método: `AlmacenClave getAlmacenClaveByDni(String dni, String claveAcceso)`

- **`getAlmacenClaveByDni_dniConCertificadoHabilitado_devuelveElAlmacenDeEseCertificado`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `repository.findByDniHabilitados("85432016B")` → `Query` cuyo `fetchOne()` devuelve un certificado de fichero legible: tipo `CLASSPATH` habilitado con `rutaClasspath` = `firma/mi_certificado.p12` y `password` = «nadanada».
  - **Act:** `getAlmacenClaveByDni("85432016B", null)`.
  - **Assert:** devuelve un `AlmacenClaveFichero` cuya clave es la guardada («nadanada»), pese a que la tecleada sea nula (se cierra el `InputStream`, como en los tests actuales).
- **`getAlmacenClaveByDni_sinNingunCertificadoHabilitadoParaEseDni_devuelveNull`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `findByDniHabilitados(...)` → `Query` con `fetchOne()` = `null` (no hay ninguno, o los que hay están deshabilitados).
  - **Act:** `getAlmacenClaveByDni("85432016B", null)`.
  - **Assert:** `assertNull(...)` — se comporta como si la persona no tuviera certificado.
- **`getAlmacenClaveByDni_variosCertificadosDelMismoDni_usaSoloElHabilitado`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `findByDniHabilitados("29050788V")` → `Query` cuyo `fetchOne()` devuelve el certificado **habilitado**, de fichero legible: tipo `CLASSPATH` con `rutaClasspath` = `firma/instalar_certificado_criptografico/secretario.p12` (recurso que existe en el classpath de test, distinto del `firma/mi_certificado.p12` de los demás casos) y `password` = «nadanada» (sin ella, con la `claveAcceso` nula del `Act` la clave efectiva sería nula y el `AlmacenClaveFichero` no llegaría a construirse); en el repositorio «existen» además otros deshabilitados que el finder no devuelve.
  - **Act:** `getAlmacenClaveByDni("29050788V", null)`.
  - **Assert:** el almacén es un `AlmacenClaveFichero` cuyo contenido es el de `firma/instalar_certificado_criptografico/secretario.p12` y cuya clave es «nadanada» — es decir, el del certificado habilitado; `verify(repository).findByDniHabilitados("29050788V")` es la única consulta de certificados.
- **`getAlmacenClaveByDni_dniInvalido_lanzaValidationExceptionYNoConsultaElRepositorio`** — Tipo: error (regresión adaptada). Verifica: `—`.
  - **Arrange:** `dni` = `12345678A`; sin stubs del repositorio.
  - **Act:** `getAlmacenClaveByDni("12345678A", "clave")`.
  - **Assert:** lanza `ValidationException` con «El DNI no es válido»; `verify(repository, never()).findByDniHabilitados(any())`.

### Método: `SituacionFirma getSituacionFirmaByDni(String dni)`

- **`getSituacionFirmaByDni_sinCertificadoHabilitadoParaEseDni_devuelveSinCertificado`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `findByDniHabilitados("85432016B")` → `Query` con `fetchOne()` = `null`.
  - **Act:** `getSituacionFirmaByDni("85432016B")`.
  - **Assert:** `SituacionFirma.SIN_CERTIFICADO`.
- **`getSituacionFirmaByDni_certificadoHabilitadoDeFicheroConClave_devuelveFicheroConClave`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `fetchOne()` → certificado `CLASSPATH` habilitado con `password` = «nadanada». Este método **no abre el fichero del certificado** (solo mira la `password`), así que aquí la `rutaClasspath` es indiferente y la convención «certificado de fichero legible» no aplica.
  - **Act:** `getSituacionFirmaByDni("85432016B")`.
  - **Assert:** `SituacionFirma.FICHERO_CON_CLAVE`.
- **`getSituacionFirmaByDni_certificadoHabilitadoEnDispositivoSinPin_devuelveDispositivoSinPin`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** `fetchOne()` → certificado `DISPOSITIVO_PKCS11` habilitado con un `dispositivoCriptografico` **no nulo** (el método lo desreferencia sin comprobarlo) cuyo `pin` es `null`.
  - **Act:** `getSituacionFirmaByDni("85432016B")`.
  - **Assert:** `SituacionFirma.DISPOSITIVO_SIN_PIN`.
- **`getSituacionFirmaByDni_dniNuloOEnBlanco_devuelveSinDni`** — Tipo: borde (regresión). Verifica: `—`.
  - **Arrange:** sin stubs.
  - **Act:** `getSituacionFirmaByDni(null)` y `getSituacionFirmaByDni("   ")`.
  - **Assert:** `SituacionFirma.SIN_DNI` en ambos; `verify(repository, never()).findByDniHabilitados(any())`.
- **`getSituacionFirmaByDni_dniConFormatoInvalido_lanzaIllegalArgumentExceptionConElDniEnmascarado`** — Tipo: error (regresión). Verifica: `—`.
  - **Arrange:** `dni` = `12345678A`.
  - **Act:** `getSituacionFirmaByDni("12345678A")`.
  - **Assert:** lanza `IllegalArgumentException` cuyo mensaje empieza por «El DNI no es válido: » y **no** contiene el DNI completo (va enmascarado).

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteLosOnceCamposDeLaAccionCrear`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ninguno (no consulta colaboradores).
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty(...)` es `true` para `dni`, `nombre`, `apellidos`, `tipoCertificado`, `fichero`, `password`, `dispositivoCriptografico`, `alias`, `rutaClasspath`, `rutaSistemaArchivos` y `enabled` (mismo estilo que `CorreoServiceImplTest.allowPropertiesInsert_*`).
- **`allowPropertiesInsert_deniegaNombreTomadoDelUsuario`** — Tipo: borde (seguridad). Verifica: `CC-CertificadoDigital-001`.
  - **Arrange:** ninguno.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty("nombreTomadoDelUsuario")` es `false` — es campo `servidor` y queda fuera de la whitelist.

### Método: `AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_permiteLosDiezCamposDeLaAccionModificar`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** ninguno.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty(...)` es `true` para `nombre`, `apellidos`, `tipoCertificado`, `fichero`, `password`, `dispositivoCriptografico`, `alias`, `rutaClasspath`, `rutaSistemaArchivos` y `enabled`.
- **`allowPropertiesUpdate_deniegaDniYNombreTomadoDelUsuario`** — Tipo: borde (seguridad). Verifica: `R-CertificadoDigital-002`, `CC-CertificadoDigital-001`.
  - **Arrange:** ninguno.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty("dni")` y `allowProperty("nombreTomadoDelUsuario")` son `false` — el DNI es inmutable tras el alta y el flag nunca cambia.

---

## Clase: `com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController`  —  controlador

**Responsabilidad:** punto de entrada de la acción de vista `…-Remote-getDatosTitularByDni-action`, disparada por el `onChange` del campo `dni` del formulario. Lee el DNI del contexto de la petición, invoca el validador y la acción escalar del servicio y devuelve `nombre`, `apellidos` y `nombreTomadoDelUsuario` al formulario con `actionResponse.setValue(...)`. No persiste nada.

**Colaboradores a mockear:** `ModelServiceFactory` (campo `@Inject`, inyectado por reflexión con `setField(controller, "modelServiceFactory", modelServiceFactory)`), `CertificadoDigitalService` (lo devuelve `modelServiceFactory.resolve(CertificadoDigital.class)`), `ActionRequest` y `ActionResponse`. **`actionRequest.getData()`** debe devolver un mapa con la clave `"context"` → mapa que contenga `"_model"` = `"com.educaflow.subsystem.criptografia.db.CertificadoDigital"` (`ActionRequestHelper` valida el `_model` contra la clase esperada en su constructor y revienta si no coincide) y la clave `"dni"`. **Ese mapa es lo que significan «contexto válido» y «contexto con `dni` = …» en los `Arrange` de esta clase**: la clave `_model` va **siempre**, aunque el `Arrange` solo enumere el `dni`; sin ella el constructor de `ActionRequestHelper` lanza `RuntimeException` («modelName is null») antes de llegar al servicio y el `Assert` descrito sería inalcanzable. No se mockea ningún repositorio ni `JpaRepository`: el controlador no debe cargar la entidad.

**Origen diseño:** Paso 5 de `design.md`.

### Método: `void getDatosTitularByDni(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`getDatosTitularByDni_dniDeUsuarioExistente_devuelveNombreApellidosYFlagAlFormulario`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** contexto con `_model` correcto y `dni` = `29050788V`; `service.validateGetDatosTitularByDni("29050788V")` → `Optional.empty()`; `service.getDatosTitularByDni("29050788V")` → `DatosTitular("Secretario", "CIPFP Mislata", true)`.
  - **Act:** `getDatosTitularByDni(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setValue("nombre", "Secretario")`, `setValue("apellidos", "CIPFP Mislata")` y `setValue("nombreTomadoDelUsuario", true)`; `verify(actionResponse, never()).setError(anyString())`.
- **`getDatosTitularByDni_dniSinUsuario_vaciaNombreYApellidosYPoneElFlagAFalse`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** contexto con `dni` = `12345678Z`; validador → `Optional.empty()`; `service.getDatosTitularByDni("12345678Z")` → `DatosTitular.sinUsuario()`.
  - **Act:** `getDatosTitularByDni(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setValue("nombre", null)`, `setValue("apellidos", null)` y `setValue("nombreTomadoDelUsuario", false)`.
- **`getDatosTitularByDni_contextoSinDni_invocaAlServicioConNull`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** contexto con `_model` pero **sin** la clave `dni` (el campo aún está vacío); validador con `null` → `Optional.empty()`; `service.getDatosTitularByDni(null)` → `DatosTitular.sinUsuario()`.
  - **Act:** `getDatosTitularByDni(actionRequest, actionResponse)`.
  - **Assert:** `verify(service).getDatosTitularByDni(null)` (no se lanza `NullPointerException` al convertir el valor a `String`) y el formulario recibe nombre y apellidos nulos con el flag a `false`.
- **`getDatosTitularByDni_dniNoTextoEnElContexto_loConvierteATexto`** — Tipo: borde. Verifica: `—`.
  - **Arrange:** contexto con `dni` = `12345678` como `Integer` (el cliente puede mandar cualquier JSON); validador → `Optional.empty()`; `service.getDatosTitularByDni("12345678")` → `DatosTitular.sinUsuario()`.
  - **Act:** `getDatosTitularByDni(actionRequest, actionResponse)`.
  - **Assert:** `verify(service).getDatosTitularByDni("12345678")` — el valor se convierte con `toString()`.
- **`getDatosTitularByDni_validadorDevuelveMensajes_respondeErrorYNoInvocaLaAccion`** — Tipo: error. Verifica: `—`.
  - **Arrange:** contexto válido; `service.validateGetDatosTitularByDni(...)` → `Optional` con un `BusinessMessages` que lleva un mensaje.
  - **Act:** `getDatosTitularByDni(actionRequest, actionResponse)`.
  - **Assert:** `verify(actionResponse).setError(anyString())` (lo hace `ActionResponseHelper.doResponseBusinessMessagesAsError`), `verify(service, never()).getDatosTitularByDni(any())` y `verify(actionResponse, never()).setValue(anyString(), any())`.
- **`getDatosTitularByDni_conIdEnElContexto_noCargaNiGuardaLaEntidad`** — Tipo: borde (seguridad). Verifica: `—`.
  - **Arrange:** contexto con `_model`, `dni` = `29050788V` y también `id` = `1` (como llega desde un registro ya guardado); validador → `Optional.empty()`; `service.getDatosTitularByDni(...)` → `DatosTitular("Secretario", "CIPFP Mislata", true)`.
  - **Act:** `getDatosTitularByDni(actionRequest, actionResponse)`.
  - **Assert:** sobre el mock del servicio solo se registran `validateGetDatosTitularByDni` y `getDatosTitularByDni` (`verifyNoMoreInteractions(service)`): no se invoca `insert`/`update`/`find`, ni se construye la entidad gestionada por JPA (la acción es escalar).

---

## Clase: `com.educaflow.subsystem.criptografia.service.DatosTitular` — sin lógica testable
**Motivo:** `record` inmutable (value object) sin comportamiento: solo los tres componentes `nombre`, `apellidos`, `tomadoDelUsuario` y la factoría estática `sinUsuario()`, que devuelve una instancia constante `(null, null, false)`. `tests-unitarios.md` §1 excluye los DTO sin comportamiento. Su contrato queda ejercido de forma indirecta en los tests de `getDatosTitularByDni` (rama «sin usuario») y de `insert`.

## Clase: `com.educaflow.subsystem.criptografia.service.CertificadoDigitalService` — sin lógica testable
**Motivo:** interfaz; solo declara las firmas `getDatosTitularByDni` / `validateGetDatosTitularByDni`. La lógica se testea en su implementación.

## Clase: `com.educaflow.subsystem.criptografia.db.CertificadoDigital` — sin lógica testable
**Motivo:** entidad de dominio generada por Axelor a partir de `domains/CertificadoDigital.xml`; POJO con getters/setters (los nuevos `nombre`, `apellidos`, `nombreTomadoDelUsuario`) y sin lógica propia. El comportamiento null-safe del getter de `nombreTomadoDelUsuario` (fila anterior al cambio → `FALSE`) lo genera el framework y queda ejercido en `update_originalSinFlagAsignado_*` y `validateUpdate_originalSinFlagAsignado_*`.

---

## Cobertura

- **Clases con lógica descritas: 2.**
  - `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl` (modificada) — 10 métodos públicos descritos: `insert`, `update`, `validateInsert`, `validateUpdate`, `validateGetDatosTitularByDni`, `getDatosTitularByDni`, `getAlmacenClaveByDni(String,String)`, `getSituacionFirmaByDni`, `allowPropertiesInsert`, `allowPropertiesUpdate`. Los helpers privados (`resolverDatosTitular`, `findUsuarioTitular`, `getCertificadoHabilitado`, `validateCertificado`, `validateUnicoCertificadoHabilitadoPorDni`, `validateNombreYApellidosIndicados`, `fireActionRule_AsignarTitular`, `fireActionRule_ConservarDni`, `fireActionRule_ConservarDatosTitularDelUsuario`) se ejercen **a través** de esos métodos públicos, nunca por reflexión.
  - `com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController` (nueva) — 1 método: `getDatosTitularByDni`.
- **Clases omitidas (sin lógica):** `com.educaflow.subsystem.criptografia.service.DatosTitular` (record/value object), `com.educaflow.subsystem.criptografia.service.CertificadoDigitalService` (interfaz), `com.educaflow.subsystem.criptografia.db.CertificadoDigital` (entidad generada por Axelor). No son clases Java y por tanto quedan fuera de los tests unitarios: `V2__certificado_digital_dni_no_unico.sql` (migración Flyway, se verifica con `psql` según el Paso 1 y en E2E), `domains/CertificadoDigital.xml`, `views/Main-CertificadoDigital.xml` y `menus.xml`.
- **Reglas server-side cubiertas (`V`/`R`/`CC`):**
  - `V-CertificadoDigital-001` — `validateInsert_sinUsuarioTitularYSinNombre_*`, `validateInsert_sinUsuarioTitularYSinNombreNiApellidos_*`, `validateInsert_sinUsuarioTitularYNombreEnBlanco_*`, `validateInsert_conUsuarioTitular_noExigeNombreNiApellidos`, `insert_certificadoInvalido_*`.
  - `V-CertificadoDigital-002` — `validateInsert_sinUsuarioTitularYSinApellidos_*`, `validateInsert_sinUsuarioTitularYSinNombreNiApellidos_*`, `validateInsert_conUsuarioTitular_noExigeNombreNiApellidos`, `insert_certificadoInvalido_*`.
  - `V-CertificadoDigital-003` — `validateUpdate_originalEscritoPorElAdministradorYSinNombre_*`, `validateUpdate_originalConNombreTomadoDelUsuario_noExigeNombreNiApellidos`, `validateUpdate_flagDelClienteATrueSobreOriginalConFlagFalse_*`, `validateUpdate_originalSinFlagAsignado_*`, `update_certificadoInvalido_*`.
  - `V-CertificadoDigital-004` — los mismos que `V-003` en su vertiente de apellidos (`validateUpdate_originalEscritoPorElAdministradorYSinApellidos_*`).
  - `V-CertificadoDigital-005` — `validateInsert_habilitadoYYaExisteOtroHabilitadoConEseDni_*`, `validateInsert_habilitadoYSinOtrosHabilitadosDeEseDni_*`, `validateInsert_deshabilitado_noConsultaLosHabilitadosDelDni`, `validateInsert_dniRepetidoEnCertificadosDeshabilitados_*`, `validateUpdate_habilitandoCuandoOtroDelMismoDniYaEstaHabilitado_*`, `validateUpdate_habilitadoYElUnicoHabilitadoEsElPropioRegistro_*`, `validateUpdate_deshabilitando_noConsultaLosHabilitadosDelDni`, `validateUpdate_consultaLosHabilitadosConElDniDelOriginalNoConElDelBeanEntrante`.
  - `R-CertificadoDigital-001` — `insert_dniDeUsuarioExistente_asignaNombreYApellidosDeLaFichaYMarcaElFlag`, `insert_dniSinUsuario_conservaNombreYApellidosDelAdministradorYDejaElFlagAFalse`, `insert_dniDeUsuarioExistente_descartaElNombreYElFlagQueEnviaElCliente`, `insert_dniSinUsuarioYFlagTrueDelCliente_fuerzaElFlagAFalse`, `insert_usuarioTitularConNombreVacioEnSuFicha_*`, `insert_variosUsuariosConElMismoDni_propagaLaExcepcionDelFinder`, `getDatosTitularByDni_yInsert_conElMismoDni_resuelvenElMismoTitular`.
  - `R-CertificadoDigital-002` — `update_clienteCambiaElDni_restauraElDniDelOriginal`, `allowPropertiesUpdate_deniegaDniYNombreTomadoDelUsuario`.
  - `R-CertificadoDigital-003` — `update_originalConNombreTomadoDelUsuario_restauraNombreApellidosYFlagDelOriginal`, `update_originalConNombreEscritoPorElAdministrador_*`, `update_originalSinFlagAsignado_*`.
  - `CC-CertificadoDigital-001` — cubierta a través de `R-001` (asignación en el alta), `R-003` (congelación en la modificación) y las dos whitelists (`allowPropertiesInsert_deniegaNombreTomadoDelUsuario`, `allowPropertiesUpdate_deniegaDniYNombreTomadoDelUsuario`).
  - Comportamiento sin identificador (nota 15 del diseño, helper de lectura `getCertificadoHabilitado`) — `getAlmacenClaveByDni_*` y `getSituacionFirmaByDni_*`.
- **Reglas solo-cliente excluidas (E2E en `test-e2e-desc.md`):** `U-certificados-digitales-001`, `U-certificados-digitales-002`, `U-certificados-digitales-003`, `U-certificados-digitales-004`, `U-certificados-digitales-005`, `U-certificados-digitales-006`, `U-certificados-digitales-007`. Viven en `views/Main-CertificadoDigital.xml` (`onChange` del DNI, `readonlyIf`, `requiredIf`, `orderBy`, `action-record` del `onNew`) y no son testeables con JUnit. La **parte de servidor** que alimenta `U-001`/`U-002` (la acción escalar `getDatosTitularByDni` y su `@CallMethod`) sí queda cubierta arriba, pero se describe con `Verifica: —` porque lo que la regla `U-` fija —el comportamiento de la vista— no se comprueba aquí.

---

## Decisiones ante ambigüedades

1. **Mensaje de V-CertificadoDigital-005.** El spec fija «Ya existe un certificado digital habilitado para el DNI \<DNI\>» y el diseño no cierra el formato exacto de la interpolación (el código preexistente entrecomillaba el DNI). Los tests **MUST** afirmar el literal del spec con el DNI concatenado; si la implementación difiere solo en comillas o mayúsculas, se ajusta la aserción al literal real del código y **no** se toca el diseño.
2. **Helpers privados sin test propio.** El diseño solo declara públicos los diez métodos listados en «Cobertura»; el resto son `private`. Se ha decidido ejercerlos siempre a través del método público que los invoca (nunca con reflexión ni cambiando su visibilidad), que es lo que hacen los tests ya existentes del subsistema.
3. **`allowPropertiesInsert`/`allowPropertiesUpdate` se testean** aunque no implementen ninguna `V`/`R`/`CC`: son la frontera de confianza del bean que el Paso 4.3 introduce y `CorreoServiceImplTest` ya sienta el precedente. Sus tests llevan `Verifica: —` salvo cuando lo que comprueban es la exclusión de un campo que sí tiene regla (`CC-CertificadoDigital-001`, `R-CertificadoDigital-002`).
4. **Inyección del `UserRepository`.** Es un campo `@Inject` privado y en un test unitario no hay inyector Guice; se decide inyectarlo por reflexión con un helper `setField`, la técnica que ya usa `TareaFirmaControllerTest`. Es además la razón por la que el diseño lo inyecta en vez de resolverlo con `JpaRepository.of(...)`.
5. **`getAlmacenClaveByDni(String dni)` (overload de un argumento)** no tiene sección propia: el diseño lo conserva sin cambios y delega en el de dos argumentos. Sus tests existentes se mantienen y solo se les adapta el stub del finder, como se indica en «Tests existentes a adaptar».
