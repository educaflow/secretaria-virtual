# CLAUDE.md — secretaria-virtual

- No hagas cambios haste tener el 95% de confianza. Preguntame hasta llegas a ese nivel.
- /compact al 60%, no al 90%

## Skills obligatorios — NUNCA omitir
NUNCA respondas directamente sobre nombres de vistas, menús, acciones, modelos o sistemas sin invocar primero el skill correspondiente. Esto incluye cualquier acción de: revisar, comprobar, auditar, sugerir, renombrar o crear. Skills aplicables: `menu`, `vistas`, `sistemas`, `modelos`, `seguridad`.

### JPQL en `domain` — reglas del proyecto
- `:__user__` es el objeto `User` (no un Long) → usar `cu.usuario = :__user__` sin `.id`
- En dominios de **permisos** (`auth.xml`): los parámetros son **posicionales sin índice** (`?`), NO `?1`/`?2`. Cada `?` en el JPQL consume una posición del array de params en orden. Si el mismo valor aparece múltiples veces en la condición, debe repetirse en `conditionParams`. El `input-config.xml` usa `@condition`/`@conditionParams` como atributos XML, NO `<domain>`/`<domain-params>`:
  ```xml
  <permission condition="self.centro = ? AND self.usuario = ? AND self.centro = ?"
              conditionParams="__user__.centroActivo, __user__, __user__.centroActivo">
  ```
- En dominios de **action-view/panel**: los parámetros sí son nombrados (`:__user__`), pero `:__user__.campo` NO funciona — usar subquery scalar
- `self` en subconsultas EXISTS puede no correlacionarse → usar patrón `self.id IN (SELECT ...)`
- En permisos, preferir comparaciones de entidad directas (`self IN (SELECT aa.tramite ...)`, `aa.actor IN (SELECT cut.tipoUsuario ...)`) en lugar de comparar IDs
- Entidades con herencia JOINED (`TipoUsuario`, `CentroUsuario`) → navegar a campos de subtipo puede fallar; usar subselect explícito:
  `t.tipoUsuario IN (SELECT tu FROM TipoUsuario tu WHERE tu.code = 'X')`
- 
## Commands

```bash
# Build and run the application
./run.sh

# Build only
./gradlew clean build --info

# Run tests only
./gradlew clean test --info

# Run specific test class
./gradlew test --tests "com.educaflow.SomeTest"

# Run with debug mode
./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /

# Run normally
./gradlew --no-daemon run --port 8080 --context-path /
```

The build triggers several custom Gradle tasks automatically (code generation, view processing, i18n, domain copy, PDF copy, data-init copy, docs).

## Architecture

**Stack:** Axelor framework (JPA/ORM, REST, XML views, Guice DI), Java 21, Kotlin 21, iText 9, PostgreSQL.

**Layer dependencies (inner → outer):**
```
secretariavirtual → system → subsystem → base/infrastructure → base/util
```

### Package structure under `com.educaflow`

- **`base/util/`** — shared utilities: `JsonUtil`, `MetaFileUtil`, `ActionRequestHelper`, `AllowProperties`, `AxelorViewUtil`, `TextUtil`, `Convert`, `DniUtil`, `ReflectionUtil`, `SecurityUtil`, `CryptoUtil`, `XmlUtil`
- **`base/infrastructure/`** — infrastructure modules: `pdf` (iText PDF operations), `validation` (BusinessMessages/BusinessException/ValidationEngine DSL), `criptografia` (X.509 certs, HSMs, FNMT/ACCV/DNI issuers), `autofirma` (desktop client integration), `mapper` (BeanMapperModel), `mail`, `evaluator` (Groovy expressions), `numeradores`, `metafile`
- **`subsystem/`** — business subsystems: `firmas`, `expedientes`, `registroentradasalida`, `pdfutilities`, `common`, `certificados`, `importer`, `sistemaeducativo`, `security`
- **`system/`** — concrete expediente types and active tramites: `tiposexpedientes/`, `tramites/`
- **`secretariavirtual/`** — top-level menus and navigation

### Axelor conventions

- **DI:** `Beans.get(Clase.class)` to get instances; `@Inject` for field injection; extend `AxelorModule` and override `configure()` with `bind(Interface.class).to(Impl.class)`
- **Controllers:** methods annotated `@CallMethod`, parameters `(ActionRequest, ActionResponse)`; use `ActionRequestHelper<T>` to extract model/id/data
- **Transactions:** `@Transactional` from Guice Persist
- **Repositories:** Axelor JPA repositories (e.g., `TareaFirmaRepository`, `JpaRepository.of(Class)`)
- **Views:** XML files in `domains/` (domain-models namespace) and `views/` (object-views namespace); i18n via `i18n_es.csv` / `i18n_ca.csv` alongside the source

### Expedientes subsystem (state machine)

The core tramitación engine is a state machine built on:

- **`EventManager<T,State,Event,Profile>`** — abstract class per expediente type; convention-based dispatch: event `SOME_EVENT` → method `triggerSomeEvent(@WhenEvent ...)`, state `SOME_STATE` → method `onEnterSomeSate(@OnEnterState ...)`
- **`Tramitador`** — orchestrates `triggerInitialEvent` and `triggerEvent`; applies `StateEventValidator` rules, copies only allowed properties via `AllowProperties`, validates with `ValidationEngine`, saves via JPA repository
- **`StateEventValidator`** — per-expediente class; methods named `getForState{STATE}InEvent{EVENT}()` annotated `@BeanValidationRulesForStateAndEvent` return `BeanValidationRules`
- **View naming convention:** `exp-{EXPEDIENT_CODE}-{STATE_CODE}-{PROFILE_CODE}-form` (with fallback to `exp-{EXPEDIENT_CODE}-{STATE_CODE}-form`)

### Firmas subsystem

- **`FirmaService`/`FirmaServiceImpl`:** `insert(DatosFirma)` creates PENDIENTE task cloning documents; `marcarComoFirmada` / `marcarComoRechazada` update state
- **`DatosFirma`** (record DTO): firmante, documentos, motivoFirma, areaFirma (`Rectangulo`), `firmaNotifierClass`, `callBackData`
- **Callback mechanism:** `fqcnFirmaNotifier` + `fqcnCallBackData` + `callBackData` (JSON) persisted in `TareaFirma`; on invoke: `Class.forName()` → `Beans.get()` → `JsonUtil.fromJson()` → `notifier.notify()`
- **Controller:** `FirmarController` — `marcarComoFirmada` allows only `documentosFirma.documentoFirmado` via `AllowProperties`
- **Views (4 separate):** `firma-pendiente`, `firma-firmado`, `firma-rechazado`, `firma-todos`

### Tipos de expediente (system/tiposexpedientes)

Each expediente type lives in its own package (e.g., `comision_servicio/`) and contains:
- `domains.xml` — entity extending base `Expediente`
- `TipoExpedienteInstance.xml` — data-init configuration
- `EventManagerImpl.java` — concrete `EventManager` subclass
- `StateEventValidatorImpl.kt` — Kotlin validation rules
- `views.xml` — UI views following the naming convention
- `i18n_es.csv` / `i18n_ca.csv` — translations

### Security / property filtering

`AllowProperties` controls which fields can be updated when copying request data to entities. Always use it via `ActionRequestHelper.getModel(AllowProperties)` or `BeanMapperModel.copyMapToEntity(...)` — never allow all properties at system boundaries unless explicitly intended.

### PDF operations

Use `DocumentoPdfFactory.getDocumentoPdf(byte[], fileName)` or `MetaFileHelper.getDocumentoPdf(MetaFile)` to get a `DocumentoPdf` instance. Signing uses `CampoFirma` builder + `AlmacenClave`. AutoFirma (desktop client) integration uses the `AutoFirma` builder and `AutoFirma.sendToActionResponse(...)`.

### i18n
Nunca jamás, crear los ficheros `i18n_ca.csv` ni `i18n_es.csv` ya que hay un script que los genera automáticamente, así que es totalmente innecesario.
