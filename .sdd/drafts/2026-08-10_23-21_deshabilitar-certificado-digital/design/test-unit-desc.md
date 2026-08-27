# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- Estilo de referencia: los tests existentes en `src/test/java/com/educaflow/...` (p.ej. `SmokeTestServiceImplTest`): aserciones de `org.junit.jupiter.api.Assertions` (no AssertJ), entidades instanciadas con `new` + setters, repositorio mockeado e inyectado por el constructor del servicio.
- Esta iniciativa **modifica** una clase existente (`CertificadoDigitalServiceImpl`): se describen **solo** los tests del comportamiento nuevo/cambiado (el criterio de «entrada inexistente» de `getAlmacenClaveByDni`, ampliado con `enabled`), no los de `validateInsert`/`validateUpdate`/`validateCertificado`/`remove`, que el diseño conserva sin cambios.

## Decisiones documentadas (ambigüedades resueltas)

1. **R-CertificadoDigital-001 se cubre con un test sobre la entidad generada `CertificadoDigital`.** El diseño materializa la regla de forma **declarativa** (atributo `default="true"` del campo `enabled` en `domains/CertificadoDigital.xml`, Paso 1): no existe ningún método de servicio al que delegarla. El contrato pide que toda regla server-side tenga ≥1 test y a la vez omitir los POJOs «sin lógica»; como aquí el único punto server-side donde vive la regla es el valor por defecto del bean generado, se describe **una sección mínima** para la entidad con el test del default (más un caso borde del mecanismo del getter en el que se apoya el Paso 2). Es la excepción justificada, no una plantilla a imitar.
2. **El caso «entrada inexistente» (findByDni → null) se incluye aunque sea comportamiento preexistente**, porque V-CertificadoDigital-001 exige que la entrada deshabilitada produzca **la misma excepción con el mismo mensaje** que la inexistente: ese test fija el mensaje de referencia contra el que se compara el caso deshabilitado. Sin él, el «mismo error» no sería verificable unitariamente.
3. **El camino feliz usa el tipo `DISPOSITIVO_PKCS11`** porque su rama del `switch` construye el `AlmacenClave` solo con getters de entidades (`dispositivoCriptografico.getSlot()`, `alias.getName()`), sin tocar ficheros, classpath ni `MetaFileUtil` — no necesita más mocks ni estáticos.
4. **La migración Flyway `V2__backfill_certificado_digital_enabled.sql` no es testable con JUnit** (es SQL ejecutado por Flyway en el arranque, sin clase Java propia); su verificación es la del Paso 1 del diseño (`SELECT count(*) ... WHERE enabled IS NULL` = 0 tras el primer arranque). Se declara excluida en Cobertura.

---

## Clase: `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl`  —  servicio

**Responsabilidad:** resolver el almacén de claves (`AlmacenClave`) del certificado digital de una persona a partir de su DNI; con esta iniciativa, tratar una entrada con `enabled` a FALSE exactamente igual que una entrada inexistente (misma `RuntimeException`, mismo mensaje), sin revelar que la entrada existe pero está deshabilitada.
**Colaboradores a mockear:** `CertificadoDigitalRepository` (mock de Mockito; **debe ser del tipo concreto** `CertificadoDigitalRepository`, no `Repository`, porque el método hace el cast `((CertificadoDigitalRepository) repository).findByDni(dni)`). Se inyecta por el constructor existente: `new CertificadoDigitalServiceImpl(CertificadoDigital.class, repositoryMock)`. No hacen falta estáticos (`I18n`, `SecurityUtil`, `Beans`): el mensaje de la excepción está literal en el método y `validateGetAlmacenClaveByDni` (que devuelve `Optional.empty()`) se ejecuta real sin colaboradores.
**Origen diseño:** Paso 2 — método `getAlmacenClaveByDni(String dni)`; aplica V-CertificadoDigital-001 (origen spec VAL-CertificadoDigital-001).

### Método: `AlmacenClave getAlmacenClaveByDni(String dni)`

- **`getAlmacenClaveByDni_entradaDeshabilitada_lanzaMismaExcepcionQueInexistente`** — Tipo: error. Verifica: `V-CertificadoDigital-001`.
  - **Arrange:** entidad `CertificadoDigital` creada con `new`, con `dni = "85432016B"`, `tipoCertificado = DISPOSITIVO_PKCS11`, `dispositivoCriptografico` y `alias` rellenos (entidades `new` con setters) y **`setEnabled(Boolean.FALSE)`**; mock `repository.findByDni("85432016B")` → esa entidad.
  - **Act:** `service.getAlmacenClaveByDni("85432016B")`.
  - **Assert:** lanza `RuntimeException` con mensaje exacto `No existe certificado para el DNI: 85432016B` — **idéntico** al del caso inexistente; el mensaje no menciona «deshabilitado» ni delata que la entrada existe (exigencia de VAL-CertificadoDigital-001 y design-guidelines.md).
- **`getAlmacenClaveByDni_entradaInexistente_lanzaExcepcionNoExisteCertificado`** — Tipo: error. Verifica: `V-CertificadoDigital-001` (fija el error de referencia del caso «no existe», con el que el caso deshabilitado debe coincidir).
  - **Arrange:** mock `repository.findByDni("85432016B")` → `null`.
  - **Act:** `service.getAlmacenClaveByDni("85432016B")`.
  - **Assert:** lanza `RuntimeException` con mensaje exacto `No existe certificado para el DNI: 85432016B` (el mismo literal que el test anterior; idealmente ambos tests comparten la constante esperada para que la igualdad quede fijada en un único sitio).
- **`getAlmacenClaveByDni_entradaHabilitada_devuelveAlmacenClave`** — Tipo: happy. Verifica: `V-CertificadoDigital-001` (rama OK: entrada habilitada ⇒ se usa).
  - **Arrange:** entidad `CertificadoDigital` con `dni = "85432016B"`, **`setEnabled(Boolean.TRUE)`**, `tipoCertificado = DISPOSITIVO_PKCS11`, `dispositivoCriptografico` con `slot` relleno y `alias` con `name` relleno; mock `repository.findByDni("85432016B")` → esa entidad.
  - **Act:** `service.getAlmacenClaveByDni("85432016B")`.
  - **Assert:** devuelve un `AlmacenClave` no nulo (instancia de `AlmacenClaveDispositivo`); no se lanza excepción.
- **`getAlmacenClaveByDni_entradaConEnabledPorDefecto_devuelveAlmacenClave`** — Tipo: borde. Verifica: `—` (complementa a `R-CertificadoDigital-001`: una entrada creada sin tocar `enabled` se comporta como habilitada también en la resolución por DNI).
  - **Arrange:** entidad `CertificadoDigital` creada con `new` **sin llamar a `setEnabled`** (el constructor generado la inicializa a `TRUE` por el `default="true"` del dominio), `dni = "85432016B"`, `tipoCertificado = DISPOSITIVO_PKCS11`, `dispositivoCriptografico` y `alias` rellenos; mock `repository.findByDni("85432016B")` → esa entidad.
  - **Act:** `service.getAlmacenClaveByDni("85432016B")`.
  - **Assert:** devuelve un `AlmacenClave` no nulo; no se lanza excepción.

---

## Clase: `com.educaflow.subsystem.criptografia.db.CertificadoDigital`  —  entidad de dominio generada (sección excepcional, ver Decisión 1)

**Responsabilidad:** entidad JPA generada por Axelor desde `domains/CertificadoDigital.xml`. Sin lógica propia salvo el comportamiento **generado a partir del delta del diseño**: el valor por defecto `TRUE` del campo `enabled` (materialización declarativa de R-CertificadoDigital-001) y el colapso de NULL a FALSE en el getter (hecho verificado del Paso 1 en el que se apoya la condición del Paso 2). Solo se describen estos dos comportamientos; el resto de la entidad queda sin tests (POJO).
**Colaboradores a mockear:** ninguno (entidad instanciada con `new`; sin mocks).
**Origen diseño:** Paso 1 — campo `<boolean name="enabled" default="true" .../>`; aplica R-CertificadoDigital-001 (origen spec RN-CertificadoDigital-001) y el «Hecho verificado» 2 del Paso 1 / Nota 3.

### Método: `Boolean getEnabled()` (generado; incluye el default del constructor)

- **`getEnabled_entidadRecienCreadaSinIndicarValor_devuelveTrue`** — Tipo: happy. Verifica: `R-CertificadoDigital-001`.
  - **Arrange:** `new CertificadoDigital()` sin llamar a `setEnabled` (equivale a «la interfaz no envía valor para "habilitado"»: el bean se construye con el default del dominio y solo un valor enviado por el cliente lo cambiaría).
  - **Act:** `getEnabled()`.
  - **Assert:** devuelve `Boolean.TRUE` (la entrada se guarda habilitada si no se indica lo contrario).
- **`getEnabled_valorNull_devuelveFalse`** — Tipo: borde. Verifica: `—` (documenta el mecanismo del getter generado —NULL colapsa a `Boolean.FALSE`, «Hecho verificado» 2 del Paso 1— que hace correcta la condición `getEnabled() == FALSE ⇒ inexistente` del Paso 2 una vez aplicado el backfill).
  - **Arrange:** `new CertificadoDigital()` y `setEnabled(null)`.
  - **Act:** `getEnabled()`.
  - **Assert:** devuelve `Boolean.FALSE` (no existe tercer estado observable a nivel de entidad).

---

## Cobertura

- Clases con lógica descritas: 2 — `CertificadoDigitalServiceImpl` (solo el delta de `getAlmacenClaveByDni`) y `CertificadoDigital` (sección excepcional limitada al comportamiento generado por el delta del diseño, ver Decisión 1).
- Clases omitidas (sin lógica): ninguna otra — el diseño no crea ni modifica más clases Java. Artefactos del diseño sin clase Java (no testables con JUnit): `views/Main-CertificadoDigital.xml`, `menus.xml` y la migración Flyway `V2__backfill_certificado_digital_enabled.sql` (ver Decisión 4; su verificación es la del Paso 1 del diseño).
- Reglas server-side cubiertas (`V`/`R`/`CC`): `V-CertificadoDigital-001` (3 tests: deshabilitada, inexistente, habilitada), `R-CertificadoDigital-001` (1 test sobre el default de la entidad, complementado por el borde del servicio). No hay reglas `CC-` en el diseño.
- Reglas solo-cliente excluidas (E2E en test-e2e-desc.md): `U-certificados-digitales-001` (casilla «Habilitado» marcada al crear, `onNew` de la vista — la ejercitan T-001…T-006, en particular T-001/ESC-001).
