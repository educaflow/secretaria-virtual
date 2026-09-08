---
type: design
template: system
---

# Diseño: Nombre y apellidos del titular del certificado digital y varios certificados por DNI

**Objetivo:** Que cada `CertificadoDigital` lleve el nombre y los apellidos de su titular (copiados de la ficha del usuario si existe uno con ese DNI, o escritos por el administrador si no) y que un mismo DNI pueda tener varios certificados dados de alta con solo uno habilitado a la vez.
**Capa:** subsystem/criptografia
**Especificación de origen:** .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/com/educaflow/secretariavirtual/startup/database/V2__certificado_digital_dni_no_unico.sql` | Crear | — (Flyway, ver `agent_docs/deploy.md`) | Migración defensiva que elimina la restricción UNIQUE sobre la columna `dni` de la tabla del certificado digital (Hibernate `ddl=update` no la retira solo). **El número de versión MUST ser > 1** (ver Paso 1) |
| `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` | Modificar | k-sistemas (modelos.md) | Añade `nombre`, `apellidos` y `nombreTomadoDelUsuario`; quita `unique` de `dni`; sustituye el finder `findByDni` por `findByDniHabilitados` |
| `src/main/java/com/educaflow/subsystem/criptografia/service/DatosTitular.java` | Crear | k-sistemas (servicios.md) | `record` con el nombre, los apellidos y el flag «tomado del usuario» resueltos a partir de un DNI. Tipo propio de R-CertificadoDigital-001 (ver `design/rules/R-CertificadoDigital-001.md`) |
| `src/main/java/com/educaflow/subsystem/criptografia/service/CertificadoDigitalService.java` | Modificar | k-sistemas (servicios.md) | Añade la acción propia **escalar** `getDatosTitularByDni(String dni)` con su validador (una acción escalar NO declara `allowProperties`) |
| `src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` | Modificar | k-sistemas (servicios.md), k-validaciones, k-secure-coding | Sobrescribe `insert`/`update` con sus action rules, reescribe las validaciones (V-001…V-005), declara `allowPropertiesInsert`/`allowPropertiesUpdate` y adapta la lectura del certificado por DNI al nuevo finder |
| `src/main/java/com/educaflow/subsystem/criptografia/controller/CertificadoDigitalController.java` | Crear | k-sistemas (controladores.md) | Controlador de la entidad con el `@CallMethod` `getDatosTitularByDni` que alimenta el `onChange` del DNI |
| `src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` | Modificar | k-vistas (grids.md, forms.md, actions.md) | Columnas y campos de nombre/apellidos, orden del listado, `readonlyIf`/`requiredIf`, `onChange` del DNI y su `action-method` |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Sin cambios reales: el `<menuitem>` de «Certificados digitales» se mantiene tal cual (ver `design/menus.xml`) |
| `src/test/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImplTest.java` | Modificar | k-sistemas (servicios.md) | Los tests existentes mockean `repository.findByDni(...)`, que desaparece; hay que reapuntarlos a `findByDniHabilitados(...)` y añadir los tests de las reglas nuevas (el detalle lo materializa `test-unit-desc.md`) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto y, en esta iniciativa, la fusión es un no-op porque el `<menuitem>` ya existe idéntico). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

---

## Clasificación `cliente` / `servidor` de los campos de `CertificadoDigital`

Además de las dos categorías canónicas (`cliente` y `servidor`), esta entidad necesita una **tercera categoría explícita**:

- **`cliente`** — el valor lo aporta el usuario, se valida con una V y va en la whitelist de `AllowProperties`. El servidor **nunca** lo escribe.
- **`servidor`** — lo dicta el servidor, se asigna **incondicionalmente** en una R con momento `Antes` y queda **fuera** de la whitelist.
- **`cliente con sobrescritura condicional del servidor`** (categoría explícita de este diseño, ver la excepción razonada al final de `## Frontera de confianza — AllowProperties por acción` y la nota 1) — el valor lo aporta el usuario y va en la whitelist, pero existe una **rama de datos** en la que el servidor lo descarta y lo sobrescribe incondicionalmente. En la rama complementaria el valor del cliente es el bueno y lo respalda una V. **No** es un `servidor` encubierto: el campo no tiene una asignación de servidor que corra siempre, así que sacarlo de la whitelist rompería el caso legítimo.

| Campo | Origen | Justificación |
|---|---|---|
| `dni` | **cliente** en `Crear` · **excluido** en `Modificar` | Aparece en `Input AllowProperties` de Crear y **no** en la de Modificar (campo inmutable, RN-CertificadoDigital-003). En `Modificar` lo restaura R-CertificadoDigital-002 |
| `nombre` | **cliente con sobrescritura condicional del servidor** | En `Input AllowProperties` de Crear y Modificar. R-CertificadoDigital-001/-003 lo sobrescriben **solo** en la rama «existe usuario titular» / «el original tiene el flag a `true`»; en la rama complementaria el valor del administrador es el bueno y lo respalda V-CertificadoDigital-001/-003 (ver la excepción razonada de `## Frontera de confianza` y la nota 1) |
| `apellidos` | **cliente con sobrescritura condicional del servidor** | Igual que `nombre`, con V-CertificadoDigital-002/-004 |
| `nombreTomadoDelUsuario` | **servidor** | `CC-CertificadoDigital-001`; no aparece en ninguna línea `Input AllowProperties`. Lo asigna R-CertificadoDigital-001 (alta) y lo restaura R-CertificadoDigital-003 (modificación) |
| `tipoCertificado` | cliente | En `Input AllowProperties` de Crear y Modificar |
| `fichero` | cliente | Ídem |
| `password` | cliente | Ídem |
| `dispositivoCriptografico` | cliente | Ídem |
| `alias` | cliente | Ídem |
| `rutaClasspath` | cliente | Ídem |
| `rutaSistemaArchivos` | cliente | Ídem |
| `enabled` | cliente | Ídem (el administrador lo marca/desmarca; RES-CertificadoDigital-001 lo valida, no lo dicta) |

Único campo `servidor` de la entidad: `nombreTomadoDelUsuario`, respaldado por R-CertificadoDigital-001 (momento `Antes`, en `insert`) y R-CertificadoDigital-003 (momento `Antes`, en `update`). Ningún campo `cliente` **puro** aparece asignado por una R-Antes-de-Crear; los dos únicos campos que una R-Antes toca sin ser `servidor` son `nombre` y `apellidos`, y lo hacen **solo dentro de una rama**: son de la tercera categoría, `cliente con sobrescritura condicional del servidor`, y su excepción está razonada al final de `## Frontera de confianza — AllowProperties por acción`.

---

## Pasos

### Paso 1 — Migración de esquema: retirar la unicidad del DNI

**Fichero:** `src/main/resources/com/educaflow/secretariavirtual/startup/database/V2__certificado_digital_dni_no_unico.sql` (**Crear**).

Es el único recurso estático de la iniciativa. `DataBaseStartup.executeMigrate()` ejecuta Flyway en cada arranque sobre `classpath:com/educaflow/secretariavirtual/startup/database` (hoy la carpeta está vacía), así que este es el sitio del proyecto para una corrección de esquema que Hibernate no hace por su cuenta.

**CRITICAL — el número de versión MUST ser mayor que 1 (por eso `V2__`, no `V1__`).** `Flyway.configure()…locations("classpath:com/educaflow/secretariavirtual/startup/database").baselineOnMigrate(true)` se configura **sin** `baselineVersion`, así que en la primera ejecución sobre una base de datos **ya poblada** (la tabla existe porque la creó Hibernate) Flyway crea el baseline en la **versión 1** y marca como «ya aplicadas», sin ejecutarlas, todas las migraciones con versión ≤ 1. Un script llamado `V1__…` no correría **nunca** justo en el escenario para el que existe: una instalación con datos donde la restricción UNIQUE sigue viva. Con `V2__` el script sí se aplica sobre el baseline.

Qué debe hacer el script:

- Localizar, en el esquema actual, **cualquier** restricción de tipo `UNIQUE` definida exactamente sobre la columna `dni` de la tabla de la entidad `CertificadoDigital` (`criptografia_certificado_digital`, según la convención `<módulo>_<entidad_en_snake_case>` de Axelor) y eliminarla con `ALTER TABLE … DROP CONSTRAINT …`.
- Buscar la restricción por **catálogo** (`pg_constraint`/`information_schema`), no por un nombre literal: el nombre lo generó Hibernate y no es estable entre entornos.
- Ser **idempotente y defensivo**: en una base de datos nueva la tabla todavía no existe cuando Flyway corre, así que el script **MUST NOT** fallar si no encuentra ni la tabla ni la restricción (bloque `DO $$ … $$` con las comprobaciones de existencia).

Motivo: al quitar `unique="true"` del modelo, Hibernate con `ddl = update` deja de declarar la restricción pero **no la borra** de una base de datos ya creada; sin esta migración, RES-CertificadoDigital-002 no se cumpliría en entornos existentes (el alta del segundo certificado del mismo DNI reventaría con un error de integridad en vez de guardarse).

**Verificar:** tras arrancar, `psql` → `\d criptografia_certificado_digital` no muestra ninguna restricción única sobre `dni`; y `SELECT dni, count(*) FROM criptografia_certificado_digital GROUP BY dni` admite valores repetidos.

### Paso 2 — Dominio `CertificadoDigital`

**Fichero del diseño:** `design/domains/CertificadoDigital.xml` → `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` (**Modificar**, fichero completo resultante).

**Resumen estructural:**

- **Preexistente (se conserva):** el `<module name="criptografia">`; los campos `dni` (`required`), `tipoCertificado`, `fichero`, `password`, `dispositivoCriptografico`, `alias`, `rutaClasspath`, `rutaSistemaArchivos`, `enabled`; y el `<enum name="TipoUbicacionCertificado">` con sus cuatro `<item>` (títulos verbatim, incluido `CLASSPATH`).
- **Delta:**
  - `dni`: se retira `unique="true"` (RES-CertificadoDigital-002). Sigue siendo `required="true"`.
  - `+ <string name="nombre" title="Nombre">` — nombre de pila del titular. Sin `required`: la obligatoriedad es **condicional** (solo cuando no hay usuario con ese DNI), y las validaciones corren **antes** que las action rules que lo rellenan, así que un `required` declarativo rechazaría altas legítimas.
  - `+ <string name="apellidos" title="Apellidos">` — ídem.
  - `+ <boolean name="nombreTomadoDelUsuario" default="false" title="Nombre tomado del usuario">` — materializa `CC-CertificadoDigital-001`. El getter que genera Axelor para un `<boolean>` es null-safe (devuelve `Boolean.FALSE` cuando el valor es nulo), de modo que las filas anteriores a este cambio se comportan como «escrito por el administrador», tal y como pide el spec.
  - `− <finder-method name="findByDni" …>` (ver `## Eliminaciones declaradas`).
  - `+ <finder-method name="findByDniHabilitados" using="String:dni" filter="self.dni = :dni AND self.enabled = true" all="true"/>` — con `all="true"` el método generado devuelve un `Query<CertificadoDigital>`, así que sirve tanto para la lectura (`.fetchOne()`, el certificado vigente de la persona) como para la validación de RES-CertificadoDigital-001 (`.fetch()`, para comprobar si el habilitado que ya existe es otro registro). El nombre arranca con el prefijo **`findBy`** que `domain-models.xsd` fija como convención para `<finder-method>` («As a convention, always use `findBy` prefix») y que siguen los finders ya existentes del proyecto (`findByDni` en `User.xml`, `findByEstado` en `Correo.xml`); el sufijo `Habilitados` indica el filtro extra por `enabled`, y **no** es un parámetro más del método (la firma generada sigue siendo `findByDniHabilitados(String dni)`).

**Verificar:** `./gradlew compileJava` genera `CertificadoDigital` con `getNombre`/`getApellidos`/`getNombreTomadoDelUsuario` y `CertificadoDigitalRepository.findByDniHabilitados(String)`; `grep -n 'unique' src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` no devuelve nada.

### Paso 3 — Interfaz `CertificadoDigitalService`

**Clase:** `com.educaflow.subsystem.criptografia.service.CertificadoDigitalService` (**Modificar** — el resto de la interfaz se conserva: `getAlmacenClaveByDni`, `validateGetAlmacenClaveByDni` (ambos overloads), `getSituacionFirmaByDni` y `validateGetSituacionFirmaByDni` no cambian de firma).

Delta — se añade la acción propia del subsistema que alimenta la regla de UI del DNI, con su validador. Es una acción **escalar** (recibe un `String`, no la entidad construida desde el request), exactamente igual que la `getSituacionFirmaByDni(String dni)` que ya tiene esta misma interfaz, así que **MUST NOT** declarar `allowProperties`: no hay mapa del cliente que filtrar (`k-sistemas/servicios.md` §`allowPropertiesXxx`, ✅ «`getSituacionFirmaByDni(String dni)` **sin** `allowProperties`»).

```java
// Clase: com.educaflow.subsystem.criptografia.service.CertificadoDigitalService
// Métodos nuevos:
DatosTitular getDatosTitularByDni(String dni);
//   Acción ESCALAR de SOLO LECTURA (no persiste nada, no recibe ni devuelve la entidad):
//   resuelve el titular de un DNI y devuelve un `DatosTitular`. Si existe un usuario de la
//   aplicación con ese DNI, devuelve el nombre y los apellidos de su ficha con
//   `tomadoDelUsuario = true`; si no existe, devuelve `DatosTitular.sinUsuario()`
//   (nombre y apellidos a null, `tomadoDelUsuario = false`).
//   Implementa la parte de servidor de U-certificados-digitales-001 y -002 y es el MISMO cálculo
//   que aplica R-CertificadoDigital-001 al guardar (ambos delegan en el helper privado
//   `resolverDatosTitular(String dni)`, ver 4.5): la pantalla no puede prometer un titular
//   distinto del que el servidor va a persistir.

Optional<BusinessMessages> validateGetDatosTitularByDni(String dni);
//   Validador de la acción anterior. Ver el comentario de la implementación (Paso 4): no tiene
//   precondiciones de negocio.
```

Tipo propio que la acción devuelve (fichero nuevo `service/DatosTitular.java`, junto a `SituacionFirma`):

```java
// Clase: com.educaflow.subsystem.criptografia.service.DatosTitular
// Tipo (NUEVO — record):
public record DatosTitular(String nombre, String apellidos, boolean tomadoDelUsuario) { }
//   Value object con el titular resuelto de un DNI. `tomadoDelUsuario` materializa
//   CC-CertificadoDigital-001.
//   Factoría estática:
//     public static DatosTitular sinUsuario();
//       Devuelve el resultado de «no hay usuario con ese DNI»: nombre y apellidos a null y
//       `tomadoDelUsuario` a false. Existe para que las dos ramas del cálculo se lean igual y
//       para que ningún llamante tenga que recordar qué valores lleva esa rama.
//   Diseño detallado de la regla que lo usa: design/rules/R-CertificadoDigital-001.md.
```

**MUST NOT** declarar aquí `validateInsert`/`validateUpdate` ni `allowPropertiesInsert`/`allowPropertiesUpdate`: ya vienen de `ModelService<CertificadoDigital>` con defaults en `DefaultModelService`; el Paso 4 solo los **sobrescribe** en la `*Impl`.

**Verificar:** compila; la interfaz declara el par `getDatosTitularByDni` / `validateGetDatosTitularByDni` y `grep -rn "allowPropertiesGetDatosTitularByDni" src/main/java/com/educaflow/subsystem/criptografia/` no devuelve nada (una acción escalar no lleva whitelist). Es el mismo comando que el detector nº 6 del Paso 10: el argumento es un **directorio**, así que `grep` MUST llevar `-r` (sin él aborta con «Is a directory» y código 2 en vez de buscar).

### Paso 4 — Implementación `CertificadoDigitalServiceImpl`

**Clase:** `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl` (**Modificar**). El resto de la clase se conserva: el constructor `CertificadoDigitalServiceImpl(Class<CertificadoDigital>, Repository<CertificadoDigital>)`, `remove`, `validateGetAlmacenClaveByDni` (ambos overloads), `validateGetSituacionFirmaByDni` y el helper privado `getInputStreamCertificado` no cambian.

Delta en la cabecera de la clase — colaborador nuevo:

```java
// Campo (NUEVO):
@Inject
private UserRepository userRepository;
//   Repositorio de la entidad `User` (que NO es la que gestiona este servicio) para resolver el
//   usuario titular por su DNI. `k-sistemas/servicios.md` marca este patrón como el correcto
//   («Repositorios adicionales (NO servicios) se pueden inyectar como campos con @Inject») y es el
//   dominante en el código real (RegistroEntradaServiceImpl, RegistroSalidaServiceImpl,
//   PerfilesUsuarioServiceImpl, Tramitador).
//   Funciona pese a que el servicio se construya por reflexión: ModelServiceFactoryImpl llama a
//   injector.injectMembers(service) justo después de instanciarlo, así que el campo ya está
//   inyectado cuando corre cualquier acción.
//   MUST NOT resolverse con `((UserRepository) JpaRepository.of(User.class))` dentro del helper:
//   es un cast sin comprobar y deja el colaborador sin mockear en los tests unitarios
//   (`test-unit-desc.md` necesita poder stubear `userRepository.findByDni(...)`).
//   Los `ModelService` sí seguirían la regla contraria (se piden a `ModelServiceFactory`), pero
//   `UserRepository` es un repositorio, no un servicio.
```

Los métodos se colocan en los cinco bloques que exige `k-sistemas/servicios.md`, en orden: acciones (sin cabecera), `Métodos de Validación`, `AllowProperties`, `Action Rules`, `Otras funciones`.

#### 4.1 Acciones

```java
// Clase: com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl
// Método (NUEVO — sobrescribe DefaultModelService):
@Override
public CertificadoDigital insert(CertificadoDigital certificado);
//   1. validateInsert(certificado).ifPresent(BusinessMessages::throwIfInvalid)  — primera línea.
//   2. fireActionRule_AsignarTitular(certificado)                              — R-CertificadoDigital-001, Antes.
//   3. return repository.save(certificado)                                     — persiste con repository, NUNCA super.insert.

// Método (NUEVO — sobrescribe DefaultModelService):
@Override
public CertificadoDigital update(CertificadoDigital certificado, CertificadoDigital certificadoOriginal);
//   1. validateUpdate(certificado, certificadoOriginal).ifPresent(BusinessMessages::throwIfInvalid) — primera línea.
//   2. fireActionRule_ConservarDni(certificado, certificadoOriginal)                       — R-CertificadoDigital-002, Antes.
//   3. fireActionRule_ConservarDatosTitularDelUsuario(certificado, certificadoOriginal)    — R-CertificadoDigital-003, Antes.
//      (En ese orden: primero la inmutabilidad del DNI, después la del titular. Son dos reglas
//       independientes y cada una hace UNA cosa — responsabilidad única, k-code-quality.)
//   4. return repository.save(certificado)                                                 — NUNCA super.update.

// Método (NUEVO — acción propia ESCALAR del subsistema, NO persiste y NO toca ninguna entidad):
@Override
public DatosTitular getDatosTitularByDni(String dni);
//   1. validateGetDatosTitularByDni(dni).ifPresent(BusinessMessages::throwIfInvalid) — primera línea.
//   2. return resolverDatosTitular(dni)  — el MISMO helper que usa fireActionRule_AsignarTitular
//      (ver 4.5), así que la pantalla y el guardado no pueden divergir.
//   Aplica U-certificados-digitales-001 y U-certificados-digitales-002.
//   MUST NOT recibir ni cargar la entidad: al ser escalar no llama a ActionRequestHelper.getModel,
//   que devolvería el bean GESTIONADO por JPA (`jpaRepository.find(id)` con el mapa del cliente
//   copiado encima) y cualquier escritura sobre él se persistiría en el flush de la transacción de
//   la petición. Con la forma escalar ese riesgo no existe.
//   MUST NOT llamar a repository.save ni marcar el método @Transactional.

// Método (MODIFICADO — cambia solo la obtención del certificado):
@Override
public AlmacenClave getAlmacenClaveByDni(String dni, String claveAcceso);
//   Igual que hoy salvo la búsqueda: en vez de findByDni(dni) usa
//   getCertificadoHabilitado(dni) (helper del bloque «Otras funciones»), que devuelve el
//   certificado HABILITADO de ese DNI o null. Como el helper ya filtra por `enabled`, la
//   comprobación `certificado.getEnabled() == false` deja de hacer falta: basta el null-check
//   para devolver null («se comporta como si la persona no tuviera certificado»).
//   El resto (switch por tipoCertificado, resolución de la clave) no cambia.

// Método (MODIFICADO — cambia solo la obtención del certificado):
@Override
public SituacionFirma getSituacionFirmaByDni(String dni);
//   Igual que hoy salvo la búsqueda: getCertificadoHabilitado(dni) en vez de findByDni(dni),
//   devolviendo SituacionFirma.SIN_CERTIFICADO cuando no hay ninguno habilitado. Se mantienen
//   SIN_DNI, la comprobación DniUtil.isValid y el enmascarado del DNI en la excepción.
```

#### 4.2 Métodos de Validación

```java
// Método (MODIFICADO — ya existe delegando en validateCertificado; se reescribe su cuerpo):
//   Hoy su cuerpo es exactamente `return validateCertificado(certificado);`.
@Override
public Optional<BusinessMessages> validateInsert(CertificadoDigital certificado);
//   Acumula en un BusinessMessages y devuelve Optional.empty() si queda válido.
//   Aplica:
//     - Las comprobaciones preexistentes de datos del certificado, delegando en el helper
//       validateCertificado(certificado, messages) (DNI con formato válido, tipo obligatorio y
//       campos exigidos por cada tipo). Ver 4.5.
//     - V-CertificadoDigital-005 (Origen spec: RES-CertificadoDigital-001, RES-CertificadoDigital-002),
//       delegando en validateUnicoCertificadoHabilitadoPorDni(certificado.getDni(), certificado, messages).
//       En el alta el DNI del bean entrante ES el bueno (es campo `cliente` de esta acción). Ver 4.5.
//     - V-CertificadoDigital-001 (Origen spec: VAL-CertificadoDigital-001) y
//       V-CertificadoDigital-002 (Origen spec: VAL-CertificadoDigital-002): SOLO si
//       findUsuarioTitular(certificado.getDni()) devuelve null (si hay usuario, el nombre y los
//       apellidos los pone el servidor en R-CertificadoDigital-001 y la validación no aplica),
//       delegando en validateNombreYApellidosIndicados(certificado, messages).
//       Los mensajes deben transmitir que el nombre / los apellidos del titular son obligatorios
//       (el spec fija los literales en VAL-CertificadoDigital-001 y -002) y van anclados a los
//       campos `nombre` y `apellidos` respectivamente.

// Método (MODIFICADO — ya existe delegando en validateCertificado; se reescribe su cuerpo):
//   Hoy su cuerpo es exactamente `return validateCertificado(certificado);`.
@Override
public Optional<BusinessMessages> validateUpdate(CertificadoDigital certificado, CertificadoDigital certificadoOriginal);
//   Aplica:
//     - Las mismas comprobaciones preexistentes: validateCertificado(certificado, messages).
//     - V-CertificadoDigital-005 (Origen spec: RES-CertificadoDigital-001, RES-CertificadoDigital-002):
//       validateUnicoCertificadoHabilitadoPorDni(certificadoOriginal.getDni(), certificado, messages).
//       CRITICAL: el DNI con el que se consulta es el del ORIGINAL, NUNCA el del bean entrante. El
//       `dni` es inmutable (RN-CertificadoDigital-003) y el valor que llega del cliente no es de
//       fiar: las validaciones corren ANTES de la action rule que lo restaura, así que si aquí se
//       usara `certificado.getDni()` la comprobación de unicidad dependería de una sola capa —la
//       exclusión de `dni` de allowPropertiesUpdate— frente al endpoint REST genérico que el propio
//       CLAUDE.md declara abierto. `k-secure-coding` exige defensa en profundidad, y el diseño ya la
//       aplica en R-CertificadoDigital-002; esta es la misma defensa en la capa de validación.
//     - V-CertificadoDigital-003 (Origen spec: VAL-CertificadoDigital-003) y
//       V-CertificadoDigital-004 (Origen spec: VAL-CertificadoDigital-004): SOLO si
//       certificadoOriginal.getNombreTomadoDelUsuario() es false (incluidos los certificados
//       anteriores a este cambio, cuyo valor nulo el getter generado devuelve como FALSE),
//       delegando en validateNombreYApellidosIndicados(certificado, messages).
//       Se consulta el ORIGINAL, no el bean entrante: el flag es un campo `servidor` inmutable y
//       el cliente no puede dictarlo. Los mensajes son los mismos que en el alta.
//     Nota: `certificadoOriginal` está siempre disponible en las dos vías de entrada —
//     Resource.save llama a modelService.update(bean, originalModel) y DefaultModelController
//     lo obtiene con ActionRequestHelper.getOriginalModel().

// Método (NUEVO — validador de la acción propia escalar):
@Override
public Optional<BusinessMessages> validateGetDatosTitularByDni(String dni);
//   Devuelve siempre Optional.empty(): la acción no tiene precondiciones de negocio. Es una
//   consulta de solo lectura que debe aceptar cualquier DNI —nulo, en blanco, incompleto o con
//   letra incorrecta— porque su cometido es precisamente reflejar en el formulario si ese DNI
//   corresponde o no a un usuario mientras el administrador lo teclea. La validez del DNI ya se
//   comprueba al guardar (helper validateCertificado). Existe porque `k-sistemas/servicios.md`
//   exige el par acción + validador para TODA acción propia (ver «Notas y supuestos»).

// Helper privado (MODIFICADO — antes se llamaba igual pero devolvía Optional<BusinessMessages>):
private void validateCertificado(CertificadoDigital certificado, BusinessMessages messages);
//   Comprobaciones preexistentes, ahora acumulando sobre el BusinessMessages que recibe en vez de
//   crear el suyo (así insert y update pueden encadenar varios bloques de validación):
//     - DNI con formato válido (DniUtil.isValid) → mensaje anclado a `dni`.
//     - Tipo de certificado obligatorio; si falta, se sale sin evaluar el resto.
//     - Por tipo: fichero (FICHERO_BD); dispositivo y alias, que el alias pertenezca al
//       dispositivo y que exista en él (DISPOSITIVO_PKCS11, con el try/catch actual para que un
//       dispositivo no configurado no bloquee el resto); ruta classpath existente (CLASSPATH);
//       ruta del sistema de archivos existente (SISTEMA_ARCHIVOS).
//   DELTA: DESAPARECE el bloque que rechazaba un DNI ya usado por otro certificado
//   (Origen spec: RES-CertificadoDigital-002 — el DNI deja de ser único). Ver
//   «## Eliminaciones declaradas».

// Helper privado (NUEVO):
private void validateUnicoCertificadoHabilitadoPorDni(String dni, CertificadoDigital certificado, BusinessMessages messages);
//   Implementa V-CertificadoDigital-005 (Origen spec: RES-CertificadoDigital-001,
//   RES-CertificadoDigital-002).
//   El DNI llega como PARÁMETRO EXPLÍCITO, separado del bean, precisamente porque no siempre es el
//   del bean: `insert` pasa `certificado.getDni()` y `update` pasa `certificadoOriginal.getDni()`
//   (el `dni` es inmutable y el del bean entrante no es de fiar — ver el CRITICAL de validateUpdate).
//   Lógica: si el certificado que se está guardando NO está habilitado, no hay nada que
//   comprobar. Si lo está, pide al repositorio los habilitados de ese DNI
//   (findByDniHabilitados(dni).fetch()) y rechaza si alguno de ellos es un registro DISTINTO del
//   que se guarda (comparación de ids null-safe: en un alta el id es nulo, así que cualquier
//   habilitado preexistente es un conflicto; en una modificación se excluye el propio registro).
//   El mensaje va anclado al campo `enabled` y debe transmitir que ya existe otro certificado
//   digital habilitado para ese DNI, incluyendo el DNI del parámetro (el spec fija el literal en
//   RES-CertificadoDigital-001).

// Helper privado (NUEVO):
private void validateNombreYApellidosIndicados(CertificadoDigital certificado, BusinessMessages messages);
//   Implementa V-CertificadoDigital-001/-002 (alta) y V-CertificadoDigital-003/-004
//   (modificación) — el mismo par de comprobaciones, invocado bajo condiciones distintas.
//   Lógica: si `nombre` es nulo o está en blanco, añade el mensaje anclado a `nombre`; si
//   `apellidos` es nulo o está en blanco, añade el mensaje anclado a `apellidos`. Los dos se
//   evalúan siempre (no hay early return) para que el administrador vea de una vez los dos que
//   le faltan.
```

#### 4.3 AllowProperties

Ver la sección `## Frontera de confianza — AllowProperties por acción` para la tabla campo a campo.

```java
// Método (NUEVO — sobrescribe el createAllowAllProperties() de DefaultModelService):
@Override
public AllowProperties allowPropertiesInsert();
//   Forma: whitelist (createAllowProperties). Enumera exactamente los campos de la línea
//   `Input AllowProperties` de la acción Crear del spec: dni, tipoCertificado, fichero, password,
//   dispositivoCriptografico, alias, rutaClasspath, rutaSistemaArchivos, enabled, nombre,
//   apellidos. Los campos relacionales (`fichero`, `dispositivoCriptografico`, `alias`) llevan un
//   mapa interior vacío, como `centro` en CorreoServiceImpl: AllowProperties.filter conserva
//   siempre `id` y `version` del objeto anidado, que es todo lo que hace falta para una
//   referencia many-to-one.
//   `nombreTomadoDelUsuario` queda FUERA: es campo `servidor` (CC-CertificadoDigital-001).
//   CRITICAL — son 11 campos y `Map.of(...)` está sobrecargado hasta 10 pares, así que el mapa
//   MUST construirse con `Map.ofEntries(Map.entry(...), …)` (o un LinkedHashMap envuelto en
//   Map.copyOf); escribirlo con `Map.of(...)` NO compila. El patrón del proyecto sigue siendo
//   `AllowProperties.createAllowProperties(<mapa>)` (TareaImportacionServiceImpl,
//   AdjuntoServiceImpl, SmokeTestServiceImpl, TareaFirmaServiceImpl): lo único que cambia es la
//   factoría del mapa.

// Método (NUEVO):
@Override
public AllowProperties allowPropertiesUpdate();
//   Forma: whitelist. Los campos de la línea `Input AllowProperties` de la acción Modificar:
//   tipoCertificado, fichero, password, dispositivoCriptografico, alias, rutaClasspath,
//   rutaSistemaArchivos, enabled, nombre, apellidos.
//   Quedan FUERA `dni` (inmutable tras el alta, RN-CertificadoDigital-003) y
//   `nombreTomadoDelUsuario` (campo `servidor` que nunca cambia tras el alta).
//   Son 10 campos, así que este sí cabe en `Map.of(...)`.

// MUST NOT declararse ningún `allowPropertiesGetDatosTitularByDni()`: `getDatosTitularByDni` es una
// acción de parámetros ESCALARES (recibe un String, no la entidad construida desde el request), y
// `k-sistemas/servicios.md` §`allowPropertiesXxx` lo marca como ❌ («no hay entidad que filtrar; el
// método no protege nada»), igual que en la ya existente `getSituacionFirmaByDni(String dni)`.
```

#### 4.4 Action Rules

```java
// Método (NUEVO):
private void fireActionRule_AsignarTitular(CertificadoDigital certificado);
//   Implementa R-CertificadoDigital-001 (Origen spec: RN-CertificadoDigital-001,
//   RN-CertificadoDigital-002, CC-CertificadoDigital-001). Momento: ANTES de repository.save,
//   dentro de insert (escribe sobre el mismo registro, sin efectos colaterales).
//   Diseño detallado en design/rules/R-CertificadoDigital-001.md (regla compleja: necesita el tipo
//   propio `DatosTitular`).
//   Lógica:
//     - DatosTitular datos = resolverDatosTitular(certificado.getDni())  — el MISMO helper que usa
//       la acción de pantalla getDatosTitularByDni, así que lo que el formulario mostró y lo que
//       aquí se persiste no pueden divergir.
//     - Si datos.tomadoDelUsuario(): asignación INCONDICIONAL de `nombre` y `apellidos` con los del
//       record (los de la ficha del usuario). El valor que el cliente hubiera enviado en
//       `nombre`/`apellidos` se descarta: lo dicta el servidor (RN-CertificadoDigital-001).
//     - Si no: `nombre` y `apellidos` se conservan tal y como llegaron del administrador
//       (RN-CertificadoDigital-002), ya validados como no vacíos por V-CertificadoDigital-001/-002.
//     - En LAS DOS ramas, asignación INCONDICIONAL
//       certificado.setNombreTomadoDelUsuario(datos.tomadoDelUsuario()).
//   Campo `servidor` que asigna: `nombreTomadoDelUsuario`, siempre y en las dos ramas.
//   MUST NOT añadir ninguna guarda `if (certificado.getNombreTomadoDelUsuario() == null)` ni
//   equivalente: permitiría que un atacante por el endpoint REST genérico colara el flag y
//   convirtiera en inmutables unos datos que él mismo escribió (ver k-secure-coding §3.3).
//   El cliente NO puede dictar este campo aunque venga relleno en el JSON: además está fuera de
//   allowPropertiesInsert.

// Método (NUEVO):
private void fireActionRule_ConservarDni(CertificadoDigital certificado, CertificadoDigital certificadoOriginal);
//   Implementa R-CertificadoDigital-002 (Origen spec: RN-CertificadoDigital-003). Momento: ANTES de
//   repository.save, dentro de update. Una sola responsabilidad: la inmutabilidad del DNI.
//   Lógica (asignación INCONDICIONAL):
//     - certificado.setDni(certificadoOriginal.getDni())  — RN-CertificadoDigital-003: el DNI se
//       fija al crear y no se puede cambiar. Es defensa en profundidad sobre la exclusión de `dni`
//       de allowPropertiesUpdate (k-secure-coding §11, «update que no restaura campos inmutables»).
//   MUST NOT condicionarla («solo si viene distinto», «solo si no es null»): la restauración es
//   siempre, venga lo que venga del cliente.

// Método (NUEVO):
private void fireActionRule_ConservarDatosTitularDelUsuario(CertificadoDigital certificado, CertificadoDigital certificadoOriginal);
//   Implementa R-CertificadoDigital-003 (Origen spec: RN-CertificadoDigital-004,
//   CC-CertificadoDigital-001). Momento: ANTES de repository.save, dentro de update. Una sola
//   responsabilidad: congelar los datos del titular que puso el servidor.
//   Lógica (todas las asignaciones INCONDICIONALES):
//     - certificado.setNombreTomadoDelUsuario(certificadoOriginal.getNombreTomadoDelUsuario())
//       — CC-CertificadoDigital-001: «después no cambia nunca». Campo `servidor`.
//     - Si certificadoOriginal.getNombreTomadoDelUsuario() es true:
//       certificado.setNombre(certificadoOriginal.getNombre()) y
//       certificado.setApellidos(certificadoOriginal.getApellidos()) — RN-CertificadoDigital-004:
//       se ignora lo que llegue del formulario. La condición es sobre el flag guardado, no un
//       `if (campo == null)`: no es el anti-patrón de k-secure-coding §3.3.
//     - Si es false, `nombre` y `apellidos` se conservan tal y como llegaron (son `cliente` en esta
//       acción) y ya los ha validado V-CertificadoDigital-003/-004.
```

#### 4.5 Otras funciones

```java
// Helper privado (NUEVO):
private DatosTitular resolverDatosTitular(String dni);
//   Cálculo ÚNICO del titular de un DNI. Es el corazón de R-CertificadoDigital-001 y lo comparten
//   sus DOS llamantes: `fireActionRule_AsignarTitular` (al guardar) y la acción de pantalla
//   `getDatosTitularByDni` (al teclear el DNI). Tenerlo en un solo sitio es lo que garantiza que la
//   pantalla no pueda prometer un titular distinto del que el servidor va a persistir.
//   Lógica:
//     - User titular = findUsuarioTitular(dni)
//     - Si titular != null → new DatosTitular(titular.getNombre(), titular.getApellidos(), true)
//     - Si titular == null → DatosTitular.sinUsuario()
//   Diseño detallado, con los casos de error, en design/rules/R-CertificadoDigital-001.md.

// Helper privado (NUEVO):
private User findUsuarioTitular(String dni);
//   Devuelve el usuario de la aplicación cuyo documento (campo `dni` del User extendido en
//   subsystem/common/domains/User.xml) coincide con el DNI recibido, o null si no hay ninguno o
//   el DNI es nulo/en blanco (con DNI nulo/en blanco MUST NOT llegar a consultar).
//   Delega en el finder ya declarado en ese dominio, a través del colaborador inyectado:
//   userRepository.findByDni(dni). Se consulta una entidad DISTINTA de la que gestiona el servicio;
//   la consulta con filtro vive en el finder del repositorio, no inline en el servicio
//   (k-sistemas/servicios.md). MUST NOT loguear el DNI (k-secure-coding §6).

// Helper privado (NUEVO):
private CertificadoDigital getCertificadoHabilitado(String dni);
//   NO es una regla de negocio `R-`: es un HELPER DE LECTURA compartido por getAlmacenClaveByDni y
//   getSituacionFirmaByDni. No escribe en ninguna entidad, no se invoca desde insert/update/remove
//   ni desde ninguna acción custom de escritura, así que no tiene momento Antes/Después y no cabe
//   en la capa que `k-validaciones` reserva a los `fireActionRule_*` (por eso NO lleva
//   identificador `R-` ni fila en la matriz de reglas de negocio; la trazabilidad del
//   comportamiento que materializa está en «Notas y supuestos», nota 15).
//   Devuelve el certificado HABILITADO de ese DNI, o null si no hay ninguno.
//   Delega en ((CertificadoDigitalRepository) repository).findByDniHabilitados(dni).fetchOne().
//   Es el punto único por el que la firma en servidor obtiene el certificado de una persona
//   (getAlmacenClaveByDni y getSituacionFirmaByDni): si no hay ninguno habilitado, se comporta
//   como si la persona no tuviera certificado. Ninguna firma pública cambia, así que los
//   consumidores de fuera del subsistema no se tocan (ver «Notas y supuestos», nota 12).

// Helper privado (SE CONSERVA sin cambios):
private InputStream getInputStreamCertificado(CertificadoDigital certificado);
```

**Verificar:** `./gradlew compileJava`; y que no queda ninguna referencia al finder retirado:
`grep -rnE "findByDni\b" src/main/java/com/educaflow/subsystem/criptografia/` solo debe encontrar la llamada a `UserRepository.findByDni` del helper `findUsuarioTitular` (el `\b` es imprescindible: sin él la búsqueda también casaría con el finder nuevo `findByDniHabilitados`, que comparte prefijo).

### Paso 5 — Controlador `CertificadoDigitalController`

**Clase:** `com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController` (**Crear** — hoy el subsistema solo tiene `DispositivoCriptograficoController`; la regla «un controlador por entidad» obliga a uno propio para `CertificadoDigital`).

```java
// Clase: com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController
// Campo:
@Inject private ModelServiceFactory modelServiceFactory;

// Método:
@CallMethod
public void getDatosTitularByDni(ActionRequest actionRequest, ActionResponse actionResponse);
//   Punto de entrada de la acción de vista
//   `subsysCriptografia.Main@CertificadoDigital-Remote-getDatosTitularByDni-action`, disparada por
//   el onChange del campo `dni` del formulario. Implementa la parte de servidor de
//   U-certificados-digitales-001 y U-certificados-digitales-002.
//   Es un @CallMethod de los que NO construyen la entidad (acción escalar), como el
//   `getClaveFirma` de TareaFirmaController (línea 149) o PdfUtilitiesController.
//   Secuencia:
//     1. Resuelve el servicio:
//        (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class)
//     2. ActionRequestHelper<CertificadoDigital> con CertificadoDigital.class y
//        ActionResponseHelper con actionResponse.
//     3. Lee el DNI del contexto de la petición:
//        Object valorDni = actionRequestHelper.getRequestData().get("dni");
//        String dni = (valorDni == null) ? null : valorDni.toString();
//        MUST NOT usar actionRequestHelper.getModel(...): con `id` en el request devuelve la
//        entidad GESTIONADA por JPA (jpaRepository.find(id)) con el mapa del cliente copiado
//        encima, y escribir sobre ella arriesga un flush no querido en la transacción de la
//        petición. Al ser una acción escalar aquí no hace falta ninguna entidad.
//     4. service.validateGetDatosTitularByDni(dni); si devuelve mensajes,
//        actionResponseHelper.doResponseBusinessMessagesAsError(...) y return.
//     5. DatosTitular datos = service.getDatosTitularByDni(dni)
//     6. actionResponse.setValue("nombre", datos.nombre()),
//        actionResponse.setValue("apellidos", datos.apellidos()) y
//        actionResponse.setValue("nombreTomadoDelUsuario", datos.tomadoDelUsuario()).
//        Los tres son valores de formulario: no se persiste nada aquí.
//   Sin @Transactional: la acción no escribe en base de datos.
//   No hace ninguna comprobación de rol ni de inmutabilidad: eso vive en el servicio y en las
//   whitelists de insert/update (k-sistemas/controladores.md, anti-patrones).
//   Parámetros nombrados `actionRequest` y `actionResponse`, en camelCase completo.
```

**MUST NOT** añadir `@CallMethod` para `insert`/`update`/`remove` ni un `validateSave`/`validateDelete`: los expone el endpoint REST automático y las acciones globales `remote-validation*` de `DefaultModelController`.

**Verificar:** `./gradlew compileJava`; y `grep -n 'method="getDatosTitularByDni"' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` coincide con el nombre del método del controlador y con el segmento `Remote-getDatosTitularByDni` del nombre de la acción (regla `Remote-{nombreFuncionJava}` de `k-vistas/actions.md`).

### Paso 6 — Vista `Main-CertificadoDigital.xml`

**Fichero del diseño:** `design/views/Main-CertificadoDigital.xml` → `src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` (**Modificar**, fichero completo resultante). Un solo `<action-view>`, en su propio fichero, con nomenclatura `{Variante}-{Entidad}.xml`. Las cinco PI `sv-*` aparecen una vez cada una y en orden.

**Resumen estructural:**

- **Preexistente (se conserva):** el `<action-view>` `subsysCriptografia.Main@CertificadoDigital-action` con sus dos `<view>` y sus dos `<view-param>`; el `<grid>` con todos sus atributos salvo `orderBy`; el `<form>` con sus atributos (`can*` a `false`, `canBackOnSave="true"`, `onNew`), los cuatro paneles condicionales por tipo de certificado (`panelFicheroBD`, `panelPkcs11`, `panelClasspath`, `panelSistemaArchivos`) intactos, el `buttons-panel` con `btnDelete`/`btnCancel`/`btnSave`, sus tres `<action-group>` (el de `btnSave` con `remote-validationSave-action` → `save` → `back`), el `action-group` `onChange-dispositivoCriptografico`, el `onNew` y los dos `<action-record>`.
- **Delta:**
  - `<grid>`: `orderBy="dni"` → `orderBy="dni,-enabled"` (DNI ascendente y, dentro del mismo DNI, los habilitados primero — `-` = descendente sobre un booleano, y `true` ordena antes que `false`). Columnas nuevas `nombre` y `apellidos` entre `dni` y `tipoCertificado`, en el orden que fija `screen-certificados-digitales.md`.
  - `<form>`: panel nuevo `panelDatosCalculados`, `hidden="true"`, `showFrame="false"`, con el único campo `nombreTomadoDelUsuario` (**sin** `showIf`). **Es una decisión de maquetación del diseño**, no un panel del spec: `screen-certificados-digitales.md` solo declara el panel «Certificado digital», y este es un panel **técnico** sin ningún campo de negocio visible. Existe porque el campo debe estar declarado en la vista para que los `readonlyIf`/`requiredIf` de `nombre` y `apellidos` puedan leer su valor, tanto al cargar como tras el `onChange` del DNI. Un panel oculto **colapsa por completo** y no reserva columnas (a diferencia de un campo con `showIf`), y es el patrón que ya usa `gestion-centro-cambio-curso.xml`. **Un solo mecanismo de ocultación**: el `hidden` del panel; añadirle además `showIf="false"` al campo sería redundante y ruidoso, así que el campo va sin él.
  - `<form>` / panel `CertificadoDigital`: campos `nombre` y `apellidos` nuevos, junto al DNI y antes del tipo de certificado. `dni` gana `readonlyIf="id != null"` (U-003) y `onChange` (U-001/U-002). `nombre` y `apellidos` llevan `readonlyIf="nombreTomadoDelUsuario"` (U-001, U-004) y `requiredIf="(dni != null) &amp;&amp; (nombreTomadoDelUsuario != true)"` (U-002, U-005). `enabled` pasa de `colOffset="1"` a `colOffset="3"` porque el DNI se ha llevado sus 2 columnas a la fila de arriba; `tipoCertificado` y `enabled` mantienen su `colSpan`.
  - `<?sv-primary-actions?>`: `action-group` nuevo `…-onChange-dni-action` (los eventos MUST referenciar siempre un `action-group`, nunca una acción suelta).
  - `<?sv-primary-actions?>` / `…-btnDelete-action`: se le antepone `<action name="remote-validationDelete-action"/>` a `<action name="delete"/>`. **Corrección de conformidad sobre lo preexistente**: `vistas.md` §1.5 exige que el `action-group` de `btnDelete` del form principal incluya `remote-validationDelete-action` antes de `delete`, y §1.8 aplica esa auditoría al fichero **resultante**, que es el que se copia verbatim a `src/main/...`. El fichero base no lo llevaba.
  - `<?sv-remotes?>`: `action-method` nuevo `…-Remote-getDatosTitularByDni-action` → `CertificadoDigitalController.getDatosTitularByDni`. El segmento `Remote-{X}` coincide con el `method="X"` del `<call>` (regla `Remote-{nombreFuncionJava}` de `k-vistas/actions.md`).
  - **Sin** `<action-method>` de validación por entidad: guardar y borrar siguen usando las acciones globales `remote-validationSave-action` / `remote-validationDelete-action`.

**Acciones declaradas en el fichero** (las nueve; el propósito de cada una, incluidas las preexistentes que el fichero copia verbatim):

| Acción | Tipo | Preexistente | Propósito | Campos / condiciones que intervienen |
|---|---|---|---|---|
| `…-btnDelete-action` | `action-group` | sí (**modificada**: + `remote-validationDelete-action`) | Borrar el certificado abierto: valida el borrado en servidor (`validateRemove`) y, si pasa, ejecuta `delete` | Botón `btnDelete`, visible con `showIf="(id!=null) \|\| (cid!=null)"` |
| `…-btnCancel-action` | `action-group` | sí | Salir del formulario sin guardar (`back`) | Botón `btnCancel` |
| `…-btnSave-action` | `action-group` | sí | Guardar: `remote-validationSave-action` (V-001…V-005 en el servidor) → `save` → `back` | Botón `btnSave`; el `back` final cierra la ventana aunque `save` sea un no-op |
| `…-onChange-dni-action` | `action-group` | **no (delta)** | Envolver la llamada remota que resuelve el titular al cambiar el DNI (los eventos referencian siempre un `action-group`) | `onChange` del campo `dni` |
| `…-onChange-dispositivoCriptografico-action` | `action-group` | sí | Al cambiar de dispositivo criptográfico, invalidar el alias elegido | `onChange` del campo `dispositivoCriptografico` |
| `…-onNew-action` | `action-group` | sí | Inicializar el formulario de alta | `onNew` del `<form>` |
| `…-set-alias-null-action` | `action-record` | sí | Pone `alias` a null (el alias anterior pertenecía a otro dispositivo) | Campo `alias` |
| `…-set-enabled-true-action` | `action-record` | sí | Marca `enabled = true` en el alta (U-certificados-digitales-006) | Campo `enabled` |
| `…-Remote-getDatosTitularByDni-action` | `action-method` | **no (delta)** | Llama a `CertificadoDigitalController.getDatosTitularByDni`, que devuelve nombre, apellidos y el flag `nombreTomadoDelUsuario` del DNI tecleado (U-001/U-002) | Lee `dni` del contexto; escribe `nombre`, `apellidos`, `nombreTomadoDelUsuario` |

> **CRITICAL — la vista NO es la defensa.** Los `readonlyIf` de `dni`, `nombre` y `apellidos`, los `requiredIf` de `nombre`/`apellidos` y el `hidden` del panel `panelDatosCalculados` son **solo UX**: el cliente puede saltárselos por el endpoint REST automático `/ws/rest/<FQN>` que Axelor publica para toda entidad (ver el apartado PENDIENTE de `CLAUDE.md`). La defensa real de esos mismos campos son las whitelists `allowPropertiesInsert`/`allowPropertiesUpdate` y las action rules R-CertificadoDigital-001/-002/-003 del Paso 4, más las validaciones V-CertificadoDigital-001…-005. Ver `k-secure-coding` §3.

**ASCII Layout — panel `CertificadoDigital`** (un dibujo por estado del `showIf` de los paneles anidados; el panel `panelDatosCalculados` es `hidden` y colapsa, no aparece en ningún dibujo):

```
Estado 1 — tipoCertificado = FICHERO_BD
ddnnnnaaaaaa   ← dni(2) + nombre(4) + apellidos(6)                    [identificación del titular]
tttttt...eee   ← tipoCertificado(6) + colOffset(3) + enabled(3)       [ubicación del certificado + estado]
── panelFicheroBD ─────────────────────────────
ppppffff····   ← password(4) + fichero(4)                             [preexistente, sin tocar]

Estado 2 — tipoCertificado = DISPOSITIVO_PKCS11
ddnnnnaaaaaa
tttttt...eee
── panelPkcs11 ────────────────────────────────
ccccaaaaaaaa   ← dispositivoCriptografico(4) + alias(8)               [preexistente, sin tocar]

Estado 3 — tipoCertificado = CLASSPATH
ddnnnnaaaaaa
tttttt...eee
── panelClasspath ─────────────────────────────
ppppcccccccc   ← password(4) + rutaClasspath(8)                       [preexistente, sin tocar]

Estado 4 — tipoCertificado = SISTEMA_ARCHIVOS
ddnnnnaaaaaa
tttttt...eee
── panelSistemaArchivos ───────────────────────
ppppssssssss   ← password(4) + rutaSistemaArchivos(8)                 [preexistente, sin tocar]

Estado 5 — tipoCertificado sin valor (ningún panel condicional visible)
ddnnnnaaaaaa
tttttt...eee
```

Las dos filas nuevas suman exactamente 12. El borde de columna 6|7 es común a las dos (`apellidos` arranca donde acaba `tipoCertificado`), y `apellidos` y `enabled` terminan los dos en la columna 12. `nombre` y `apellidos` van en la misma fila y en el orden convencional del dominio (nombre antes que apellidos), pegados al DNI porque los tres identifican al titular; `apellidos` recibe más columnas que `nombre` porque su valor típico es más largo. `dni(2)`, `tipoCertificado(6)` y `enabled(3)` conservan el `colSpan` que ya tenían (mínima intrusión); lo único que se recalcula es el `colOffset` de `enabled`, forzado por el hueco que deja el DNI al subir de fila.

**ASCII Layout — `buttons-panel`** (sin cambios respecto al fichero real):

```
bb......ccgg   ← btnDelete(2) + colOffset(6) + btnCancel(2) + btnSave(2)
```

Secundario (Borrar) a la izquierda, principales (Cancelar, Guardar) pegados al borde derecho; `btnDelete` es condicional pero está al principio del panel y sin `colOffset`, que es la excepción documentada en `k-vistas/forms.md`.

**Verificar:** al abrir «Certificados digitales» el listado muestra las cinco columnas en orden; al teclear un DNI de usuario en el alta, nombre y apellidos se rellenan solos y quedan en gris; al teclear uno que no lo es, se vacían y salen marcados como obligatorios.

### Paso 7 — Menús

**Fichero del diseño:** `design/menus.xml` → se fusiona en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (**Modificar**).

Esta iniciativa **no cambia ningún menú**: `screen-certificados-digitales.md` declara «Administración SV → Certificados digitales — lo ve el Administrador; lleva a esta pantalla (sin cambios)». El `design/menus.xml` reproduce ese único `<menuitem>` verbatim para dejar constancia de que el delta no lo toca; la fusión es un **no-op** y `/sdd-implementer` **MUST NOT** duplicar la entrada.

**Verificar:** `grep -c 'administracionSv-certificadosDigitales-menuitem' src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` devuelve `1`.

### Paso 8 — Seguridad

Sin cambios. El certificado digital **no pertenece a ningún centro**, así que no hay filtro multi-centro que aplicar ni riesgo de IDOR cross-tenant en esta pantalla. El acceso lo da el `<menuitem>` con `groups="admins"` sobre el grupo de administradores de Axelor, que ya existe; no hay ningún `auth-*.xml` que conceda permisos sobre `CertificadoDigital` y no hace falta añadirlo (no se crea ninguna entidad nueva).

Regla de acceso, en lenguaje natural: **el Administrador** ve, crea, modifica y borra certificados digitales de cualquier persona, sin restricción por centro. Ningún otro tipo de usuario llega a la pantalla.

La superficie de seguridad que **sí** cambia es la frontera de confianza del bean, y se cierra en el Paso 4.3: la entidad pasa de `createAllowAllProperties()` heredado a whitelists explícitas en `insert` y `update`. Ver `## Frontera de confianza — AllowProperties por acción`.

**Verificar:** con el usuario `admin` la pantalla es accesible y operativa; ningún otro menú expone la entidad.

### Paso 9 — Datos iniciales

Sin cambios. La iniciativa no añade catálogos ni datos maestros: los recursos que usan los escenarios (`firma/mi_certificado.p12` y `firma/instalar_certificado_criptografico/secretario.p12`) ya están dentro del WAR, y el usuario `secretario@mislata.es` con documento `29050788V`, nombre «Secretario» y apellidos «CIPFP Mislata» ya está en `src/main/resources/data-demo/input/usuarios-demo.xml`. No se crea ninguna carpeta `data-init` en `subsystem/criptografia`.

**Verificar:** `grep -n '29050788V' src/main/resources/data-demo/input/usuarios-demo.xml` devuelve la línea del secretario.

### Paso 10 — Verificación final

Compilar, pasar los tests y arrancar:

```bash
./run.sh
```

(`./run.sh` hace `./gradlew clean build` —compila y ejecuta los tests— y, si todo pasa, arranca en el 8080 con la config privada.) Para compilar sin arrancar: `./gradlew clean build --info`.

**Detectores mecánicos** (cada uno con su resultado esperado; una salida distinta es un fallo a corregir antes de dar la iniciativa por terminada):

```bash
# 1. Finder retirado: la ÚNICA coincidencia admisible es la llamada a UserRepository.findByDni
#    del helper findUsuarioTitular. Cualquier otra significa que quedó una referencia al
#    findByDni de CertificadoDigitalRepository, que ya no existe.
#    El `\b` acota la búsqueda al nombre exacto: sin él casaría también el finder nuevo
#    findByDniHabilitados, que comparte prefijo.
grep -rnE "findByDni\b" src/main/java/com/educaflow/subsystem/criptografia/

# 2. Persistencia: MUST NOT usarse super.insert/update/remove (k-sistemas/servicios.md).
#    Esperado: SIN resultados.
grep -rnE "super\.(insert|update|remove)\(" src/main/java/com/educaflow/subsystem/criptografia/

# 3. Botones del formulario: la toolbar nativa de Axelor MUST estar apagada (vistas.md §1.4/§3.b).
#    Esperado: SIN resultados.
grep -nE '<form .*can(Back|Delete|Save)="true"' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml

# 4. Validación remota por entidad: MUST usarse las acciones globales, no un action-method
#    propio de save/delete (vistas.md §1.5/§3.c). Esperado: SIN resultados.
grep -nE 'Remote-validate(Save|Delete)-action' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml

# 5. Validación remota global de borrado presente en el action-group de btnDelete (vistas.md §1.5).
#    Esperado: 1 línea.
grep -c 'remote-validationDelete-action' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml

# 6. Acción escalar sin whitelist (k-sistemas/servicios.md). Esperado: SIN resultados.
grep -rn "allowPropertiesGetDatosTitularByDni" src/main/java/com/educaflow/subsystem/criptografia/
```

Comprobaciones tras el arranque:

- El log no muestra errores de Flyway ni de generación de dominios.
- `psql` → `\d criptografia_certificado_digital` no lista ninguna restricción única sobre `dni`.
- La pantalla «Certificados digitales» abre, permite dar de alta dos certificados con el mismo DNI (uno habilitado y otro no) y rechaza el segundo habilitado.

---

## Frontera de confianza — AllowProperties por acción

### `CertificadoDigitalServiceImpl.getDatosTitularByDni` (invocado desde `CertificadoDigitalController.getDatosTitularByDni`)

**Sin tabla: la acción es ESCALAR y por tanto NO declara whitelist.** Recibe un `String dni` leído del contexto de la petición (`actionRequestHelper.getRequestData().get("dni")`) y devuelve un `DatosTitular`; no construye la entidad desde el request, así que no hay mapa del cliente que filtrar y un `allowPropertiesGetDatosTitularByDni()` no protegería nada — `k-sistemas/servicios.md` §`allowPropertiesXxx` lo marca explícitamente como ❌, con el ejemplo de la `getSituacionFirmaByDni(String dni)` que ya existe en este mismo servicio.

Superficie de la acción sobre la entidad: **ninguna**. No lee ningún campo del bean, no escribe ninguno y no persiste nada; los tres valores que devuelve (`nombre`, `apellidos`, `nombreTomadoDelUsuario`) viajan al formulario como `actionResponse.setValue(...)` y solo se convierten en datos guardados si el administrador pulsa «Guardar», momento en el que vuelven a pasar por `allowPropertiesInsert`/`allowPropertiesUpdate` y por las action rules de abajo. Que la pantalla muestre el titular no lo hace confiable: lo que se persiste lo vuelve a calcular el servidor en `fireActionRule_AsignarTitular`.

### `CertificadoDigitalServiceImpl.insert` (endpoint REST automático `/ws/rest/<FQN>` y acción global `remote-validationSave-action`)

Entidad: `CertificadoDigital`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-CertificadoDigital.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `dni` | cliente | sí | Input directo del administrador (en `Input AllowProperties` de Crear). Se fija al crear. |
| `nombre` | cliente | sí | En `Input AllowProperties`. Si existe un usuario con ese DNI, `fireActionRule_AsignarTitular` lo **sobrescribe incondicionalmente** con el de la ficha (RN-CertificadoDigital-001); si no, se conserva el del administrador (RN-CertificadoDigital-002) y V-CertificadoDigital-001 exige que no venga vacío. |
| `apellidos` | cliente | sí | Ídem, con RN-CertificadoDigital-001/-002 y V-CertificadoDigital-002. |
| `nombreTomadoDelUsuario` | servidor | **NO** | Campo calculado (CC-CertificadoDigital-001). Asignado **incondicionalmente** en `insert` → `fireActionRule_AsignarTitular` en sus dos ramas. |
| `tipoCertificado` | cliente | sí | En `Input AllowProperties`. |
| `fichero` | cliente | sí | En `Input AllowProperties` (mapa interior vacío: solo viaja la referencia). |
| `password` | cliente | sí | En `Input AllowProperties`. |
| `dispositivoCriptografico` | cliente | sí | En `Input AllowProperties` (mapa interior vacío). |
| `alias` | cliente | sí | En `Input AllowProperties` (mapa interior vacío). |
| `rutaClasspath` | cliente | sí | En `Input AllowProperties`. |
| `rutaSistemaArchivos` | cliente | sí | En `Input AllowProperties`. |
| `enabled` | cliente | sí | En `Input AllowProperties`. El servidor no lo dicta: lo valida V-CertificadoDigital-005. |

### `CertificadoDigitalServiceImpl.update` (endpoint REST automático `/ws/rest/<FQN>` y acción global `remote-validationSave-action`)

Entidad: `CertificadoDigital`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-CertificadoDigital.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `dni` | servidor en esta acción | **NO** | Inmutable tras el alta: aparece en `Input AllowProperties` de Crear pero **no** en la de Modificar. Además `fireActionRule_ConservarDni` (R-CertificadoDigital-002) lo restaura **incondicionalmente** desde el original (RN-CertificadoDigital-003), y `validateUpdate` consulta la unicidad con el DNI del ORIGINAL, no con el del bean entrante. |
| `nombre` | cliente | sí | En `Input AllowProperties` de Modificar. Si `nombreTomadoDelUsuario` del original es `true`, `fireActionRule_ConservarDatosTitularDelUsuario` (R-CertificadoDigital-003) lo restaura incondicionalmente desde el original (RN-CertificadoDigital-004); si es `false`, se acepta el del administrador y V-CertificadoDigital-003 exige que no venga vacío. |
| `apellidos` | cliente | sí | Ídem, con RN-CertificadoDigital-004 y V-CertificadoDigital-004. |
| `nombreTomadoDelUsuario` | servidor | **NO** | CC-CertificadoDigital-001: «después no cambia nunca». Restaurado **incondicionalmente** desde el original en `fireActionRule_ConservarDatosTitularDelUsuario` (R-CertificadoDigital-003). |
| `tipoCertificado` | cliente | sí | En `Input AllowProperties` de Modificar. |
| `fichero` | cliente | sí | En `Input AllowProperties` (mapa interior vacío). |
| `password` | cliente | sí | En `Input AllowProperties`. |
| `dispositivoCriptografico` | cliente | sí | En `Input AllowProperties` (mapa interior vacío). |
| `alias` | cliente | sí | En `Input AllowProperties` (mapa interior vacío). |
| `rutaClasspath` | cliente | sí | En `Input AllowProperties`. |
| `rutaSistemaArchivos` | cliente | sí | En `Input AllowProperties`. |
| `enabled` | cliente | sí | En `Input AllowProperties`. Validado por V-CertificadoDigital-005. |

`allowPropertiesRemove` no se sobrescribe: el borrado no acepta datos del cliente más allá del `id`, y el default heredado no abre ninguna puerta nueva (el bean se carga de BD por id).

### Excepción razonada — `nombre` y `apellidos`: `cliente` en la whitelist y a la vez tocados por una R-Antes

`design-contract.md` §3 y el punto de checklist de §9 («ningún `cliente` aparece asignado por una R-Antes-de-Crear») parten de que la asignación de servidor es **incondicional y siempre**: si el servidor siempre dicta el valor, el campo es `servidor` y dejarlo en la whitelist es mass-assignment. Aquí ese supuesto **no se da**, y por eso el diseño declara la tercera categoría `cliente con sobrescritura condicional del servidor` en la sección de clasificación en vez de forzar una de las dos etiquetas canónicas:

- **El spec obliga a las dos cosas a la vez.** `nombre` y `apellidos` están en la línea `Input AllowProperties` de las acciones `Crear` **y** `Modificar` (⇒ `cliente`), y a la vez RN-CertificadoDigital-001 y RN-CertificadoDigital-004 hacen que el servidor los dicte **cuando existe un usuario titular**. No es una contradicción del spec: son dos ramas de datos disjuntas del mismo campo.
- **Por qué NO se reclasifican como `servidor`.** Un `servidor` va fuera de la whitelist. Sacar `nombre`/`apellidos` de `allowPropertiesInsert`/`allowPropertiesUpdate` haría **imposible** el caso que el spec exige explícitamente (RN-CertificadoDigital-002: no hay usuario con ese DNI y el administrador los escribe): el filtro los descartaría del mapa y el alta se guardaría siempre con el titular vacío. Ese caso, además, tiene sus propias validaciones de servidor —V-CertificadoDigital-001/-002 en el alta y -003/-004 en la modificación—, que solo tienen sentido sobre un valor que el cliente **sí** puede aportar.
- **Por qué NO se reclasifican como `cliente` puro sin tocarlos en la R.** La rama «existe usuario titular» es precisamente la defensa: el nombre y los apellidos del titular deben salir de la ficha del usuario y no de lo que teclee (o inyecte por REST) quien da de alta el certificado. Quitar la sobrescritura dejaría RN-CertificadoDigital-001 y RN-CertificadoDigital-004 sin implementar en el servidor y confiando en el `readonlyIf` de la vista, que `k-secure-coding` §3 declara explícitamente que **no es defensa**.
- **Por qué la excepción es segura.** Dentro de cada rama la asignación **sí** es incondicional: cuando `datos.tomadoDelUsuario()` es `true` (alta) o el flag del original es `true` (modificación), R-CertificadoDigital-001/-003 asignan **siempre**, sin ninguna guarda `if (campo == null)`, y descartan lo que llegara del cliente. Quién elige la rama **no lo decide el cliente**: la elige el servidor a partir de la existencia de un `User` con ese DNI (alta) o del flag `nombreTomadoDelUsuario` **del registro original leído de BD** (modificación). Y el discriminante en sí, `nombreTomadoDelUsuario`, es un campo `servidor` puro: fuera de las dos whitelists, asignado incondicionalmente en las dos ramas de R-CertificadoDigital-001 y restaurado incondicionalmente desde el original en R-CertificadoDigital-003. El atacante que envíe `nombre`, `apellidos` y `nombreTomadoDelUsuario=true` por el endpoint REST genérico no consigue nada: el flag se le sobrescribe y, si hay usuario titular, el nombre y los apellidos también.
- **Alcance de la excepción.** Aplica **solo** a `nombre` y `apellidos` de `CertificadoDigital`. Ningún otro campo `cliente` de la entidad se asigna en ninguna R, y el único `servidor` (`nombreTomadoDelUsuario`) cumple el contrato canónico sin excepción alguna.

**DTO de alta programática: no aplica.** El diseño no define ningún `record` de alta ni ninguna vía de creación desde código de otro subsistema: el alta llega siempre por el endpoint REST automático `/ws/rest/<FQN>` (desde el formulario de la pantalla), que pasa por `allowPropertiesInsert` y por `validateInsert` → `insert`. El único `record` del diseño, `DatosTitular`, es de **salida** de una consulta de solo lectura y no alimenta ninguna escritura.

---

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones `V-CertificadoDigital-NNN`

| Regla | Origen spec | Ubicación | Qué comprueba |
|---|---|---|---|
| V-CertificadoDigital-001 | VAL-CertificadoDigital-001 | `CertificadoDigitalServiceImpl.validateInsert` → `validateNombreYApellidosIndicados` | El nombre está indicado, solo cuando no existe usuario con ese DNI |
| V-CertificadoDigital-002 | VAL-CertificadoDigital-002 | `CertificadoDigitalServiceImpl.validateInsert` → `validateNombreYApellidosIndicados` | Los apellidos están indicados, misma condición |
| V-CertificadoDigital-003 | VAL-CertificadoDigital-003 | `CertificadoDigitalServiceImpl.validateUpdate` → `validateNombreYApellidosIndicados` | El nombre está indicado, solo cuando `nombreTomadoDelUsuario` del original es `false` |
| V-CertificadoDigital-004 | VAL-CertificadoDigital-004 | `CertificadoDigitalServiceImpl.validateUpdate` → `validateNombreYApellidosIndicados` | Los apellidos están indicados, misma condición |
| V-CertificadoDigital-005 | RES-CertificadoDigital-001, RES-CertificadoDigital-002 | `CertificadoDigitalServiceImpl.validateInsert` (con `certificado.getDni()`) y `.validateUpdate` (con `certificadoOriginal.getDni()`) → `validateUnicoCertificadoHabilitadoPorDni(String dni, …)` | Para un mismo DNI solo puede haber un certificado habilitado; los deshabilitados pueden repetirse sin límite (RES-002 retira la unicidad global del DNI, RES-001 la sustituye por esta) |

### Reglas de negocio `R-CertificadoDigital-NNN`

| Regla | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-CertificadoDigital-001 | RN-CertificadoDigital-001, RN-CertificadoDigital-002, CC-CertificadoDigital-001 | `CertificadoDigitalServiceImpl.fireActionRule_AsignarTitular`, invocado desde `insert` (comparte el cálculo `resolverDatosTitular` con la acción de pantalla `getDatosTitularByDni`) — **Detalle: `design/rules/R-CertificadoDigital-001.md`** | Antes de `repository.save` |
| R-CertificadoDigital-002 | RN-CertificadoDigital-003 | `CertificadoDigitalServiceImpl.fireActionRule_ConservarDni`, invocado desde `update` | Antes de `repository.save` |
| R-CertificadoDigital-003 | RN-CertificadoDigital-004, CC-CertificadoDigital-001 | `CertificadoDigitalServiceImpl.fireActionRule_ConservarDatosTitularDelUsuario`, invocado desde `update` | Antes de `repository.save` |

> **No hay ninguna R- de lectura.** Las tres reglas de negocio del diseño son `fireActionRule_*` invocados desde `insert`/`update` con momento `Antes` de `repository.save`, que es la única capa que la taxonomía admite para una `R-`. El cambio de *lookup* de la firma en servidor (usar el certificado **habilitado** del DNI) **no** es una `R-`: no escribe nada y no participa de ninguna operación de escritura. Se materializa como el helper de lectura `getCertificadoHabilitado` del bloque «Otras funciones» del Paso 4.5, y su trazabilidad está en «Notas y supuestos», nota 15.

### Reglas de UI `U-certificados-digitales-NNN`

| Regla | Origen spec | Ubicación | Mecanismo |
|---|---|---|---|
| U-certificados-digitales-001 | RUI-certificados-digitales-formulario-001 | `views/Main-CertificadoDigital.xml`: `onChange` del campo `dni` → `…-onChange-dni-action` → `…-Remote-getDatosTitularByDni-action`; más `readonlyIf="nombreTomadoDelUsuario"` en `nombre` y `apellidos` | `action-group` + `action-method` (rama «existe usuario»: rellena nombre y apellidos y pone el flag a `true`, lo que los deja de solo lectura) |
| U-certificados-digitales-002 | RUI-certificados-digitales-formulario-002 | Misma acción `…-Remote-getDatosTitularByDni-action`; más `requiredIf="(dni != null) && (nombreTomadoDelUsuario != true)"` en `nombre` y `apellidos` | `action-method` (rama «no existe usuario»: vacía nombre y apellidos y pone el flag a `false`, lo que los deja editables y obligatorios) |
| U-certificados-digitales-003 | RUI-certificados-digitales-formulario-003 | `views/Main-CertificadoDigital.xml`: atributo `readonlyIf="id != null"` del campo `dni` | Atributo inline, reevaluado de forma continua (cubre el disparador «al cargar») |
| U-certificados-digitales-004 | RUI-certificados-digitales-formulario-004 | `views/Main-CertificadoDigital.xml`: atributo `readonlyIf="nombreTomadoDelUsuario"` de `nombre` y `apellidos` | Atributo inline; el campo `nombreTomadoDelUsuario` está declarado en el panel oculto `panelDatosCalculados`, así que su valor llega también al cargar |
| U-certificados-digitales-005 | RUI-certificados-digitales-formulario-005 | `views/Main-CertificadoDigital.xml`: atributo `requiredIf="(dni != null) && (nombreTomadoDelUsuario != true)"` de `nombre` y `apellidos` | Atributo inline; el valor nulo del flag en las filas anteriores al cambio se evalúa como «no tomado del usuario» |
| U-certificados-digitales-006 | RUI-certificados-digitales-formulario-006 | `views/Main-CertificadoDigital.xml`: `onNew` → `…-onNew-action` → `…-set-enabled-true-action` | `action-group` + `action-record` **preexistentes**, sin cambios |
| U-certificados-digitales-007 | — | `views/Main-CertificadoDigital.xml`: atributo `orderBy="dni,-enabled"` del `<grid>` | Añadida por el diseño para materializar la propiedad «Ordenación por defecto» de `screen-certificados-digitales.md` (el spec no la numera como `RUI-`) |

### Reglas materializadas en el modelo de dominio

| Regla del spec | Ubicación | Cómo |
|---|---|---|
| RES-CertificadoDigital-002 | `domains/CertificadoDigital.xml` (campo `dni` sin `unique`) + `V2__certificado_digital_dni_no_unico.sql` + eliminación del bloque de unicidad en `validateCertificado` | La restricción se **retira**: en el modelo (deja de declararse), en la base de datos ya creada (migración Flyway) y en el servicio (desaparece el rechazo por DNI repetido). Su contrapartida positiva es V-CertificadoDigital-005, que la sustituye por «solo uno habilitado» |
| CC-CertificadoDigital-001 | `domains/CertificadoDigital.xml` (campo `nombreTomadoDelUsuario`, `momento: escritura`, clasificado `servidor`) | Campo persistido, asignado por R-CertificadoDigital-001 (alta) y restaurado por R-CertificadoDigital-003 (modificación); nunca `sobreescribible` por el cliente (fuera de las dos whitelists) |

---

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).
- **Tests E2E**: descritos en `test-e2e-desc.md` (T-007 … T-017), materializados a partir de los escenarios ESC-001 … ESC-010.

---

## Reglas del spec descartadas

Ninguna. Las cuatro `VAL-` (`VAL-CertificadoDigital-001`/`-002` en la acción Crear y `-003`/`-004` en la acción Modificar), las cuatro `RN-`, las dos `RES-`, el `CC-` y las seis `RUI-` del spec están ubicadas en la matriz de trazabilidad (RES-CertificadoDigital-002 y CC-CertificadoDigital-001 aparecen además en el bloque «Reglas materializadas en el modelo de dominio»).

---

## Eliminaciones declaradas

| Elemento preexistente eliminado | Fichero | ID de spec que lo justifica |
|---|---|---|
| Atributo `unique="true"` del campo `dni` | `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` | RES-CertificadoDigital-002 |
| `<finder-method name="findByDni" using="String:dni" filter="self.dni = :dni"/>` | `src/main/java/com/educaflow/subsystem/criptografia/domains/CertificadoDigital.xml` | RES-CertificadoDigital-002 — con el DNI ya no único, un finder que devuelve «el» certificado de un DNI devolvería uno arbitrario (`fetchOne` aplica `LIMIT 1`, sin error). Lo sustituye `findByDniHabilitados` |
| Bloque de `validateCertificado` que rechazaba un DNI ya usado por otro certificado (mensaje «Ya existe un certificado digital con el DNI …») | `src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` | RES-CertificadoDigital-002 (el propio spec dice «se retira la unicidad actual del DNI y su mensaje») |
| Condición `\|\| (certificado.getEnabled() == false)` de `getAlmacenClaveByDni` y `getSituacionFirmaByDni` (hoy en las líneas 45 y 88) | `src/main/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImpl.java` | RES-CertificadoDigital-001 — el filtro por `enabled` pasa al finder `findByDniHabilitados`, así que el chequeo del flag en el servicio queda muerto y basta el null-check del certificado devuelto por `getCertificadoHabilitado` |

Nada más se elimina: todos los demás campos, enums, paneles, botones y acciones preexistentes se conservan.

---

## Tests E2E supersedidos

Ninguno. Los seis tests ya persistidos en `src/test/e2e/subsystem/criptografia/` (`t-001` … `t-006`) usan el DNI «85432016B», que es el documento del usuario de demostración `director@mislata.es` (nombre «Director», apellidos «CIPFP Mislata»): al teclearlo, U-certificados-digitales-001 rellena solo el nombre y los apellidos, así que V-CertificadoDigital-001/-002 no aplican y las altas siguen guardándose sin tocar esos campos. Ninguno de los seis modifica el DNI de un certificado ya guardado (U-003 no les afecta) y ninguno crea un segundo certificado para el mismo DNI (V-005 no les afecta). Todos siguen siendo válidos tal cual.

---

## Notas y supuestos

1. **`nombre` y `apellidos` son de la categoría `cliente con sobrescritura condicional del servidor`.** El spec los pone en la línea `Input AllowProperties` de las dos acciones (⇒ `cliente`) y a la vez hace que RN-CertificadoDigital-001 y RN-CertificadoDigital-004 los dicten cuando existe un usuario titular. No es una contradicción ni un campo `servidor` encubierto: cuando hay usuario, el servidor descarta el valor del cliente de forma incondicional (la defensa se mantiene); cuando no lo hay, el valor del cliente es el bueno y lo validan V-CertificadoDigital-001/-002 y -003/-004. Por eso están en las whitelists y no se documentan como campos `servidor`. Como `design-contract.md` §3/§9 solo contempla las dos categorías canónicas, el diseño **declara la tercera de forma explícita** en la sección de clasificación y razona la excepción al final de `## Frontera de confianza — AllowProperties por acción` (por qué ninguna de las dos etiquetas canónicas sirve, quién elige la rama y por qué es segura). El único campo `servidor` de la entidad es `nombreTomadoDelUsuario`.

2. **`validateGetDatosTitularByDni` devuelve siempre `Optional.empty()`.** `k-sistemas/servicios.md` exige el par acción + validador para **toda** acción propia «sin excepciones», y también desaconseja declarar validadores que serán stubs vacíos. Aquí gana la primera regla: `getDatosTitularByDni` es una acción propia expuesta desde un `@CallMethod` y necesita su validador para que el contrato del servicio sea uniforme (lo mismo que ya ocurre con `getSituacionFirmaByDni` / `validateGetSituacionFirmaByDni`), pero no tiene ninguna precondición de negocio — debe aceptar cualquier DNI, incluso incompleto, porque se dispara mientras el administrador teclea. Validar ahí el formato del DNI daría un error espurio en el `onChange` y duplicaría la comprobación que ya hace `validateCertificado` al guardar.

   (Por qué la acción es escalar y no recibe la entidad: ver la nota 12.)

3. **Mecanismo elegido para RUI-001/RUI-002: `action-method` (type1), no `action-record` con `call:`.** La regla toca **tres** campos a la vez (`nombre`, `apellidos`, `nombreTomadoDelUsuario`) y `k-vistas/actions.md` reserva el `call:` de `<action-record>` para cuando el servidor solo calcula el valor de **un** campo; además, con `call:` harían falta tres métodos de controlador y tres búsquedas del usuario por cada cambio de DNI. El `readonly`/`required` que esas mismas reglas piden **no** viaja en la respuesta del controlador: se resuelve de forma declarativa con `readonlyIf`/`requiredIf` sobre el campo `nombreTomadoDelUsuario`, que es un campo real del modelo. Así la misma expresión cubre el disparador «al cambiar DNI» (U-001/U-002) y el disparador «al cargar» (U-004/U-005) sin duplicar lógica.

4. **`requiredIf` lleva la guarda `dni != null`.** Sin ella, un formulario recién abierto marcaría el nombre y los apellidos como obligatorios antes de que el administrador haya escrito ningún DNI, que no es lo que describe RUI-002 («al escribir un DNI que no corresponde a ningún usuario»). La obligatoriedad real la impone el servidor en V-CertificadoDigital-001/-002 y -003/-004; el `requiredIf` es solo UX.

5. **Migración de esquema.** Quitar `unique="true"` no basta en una base de datos ya creada: Hibernate con `ddl = update` nunca borra restricciones. De ahí el paso 1 con Flyway, que el proyecto ya tiene cableado en `DataBaseStartup` y cuya carpeta de migraciones está hoy vacía. **El script se llama `V2__certificado_digital_dni_no_unico.sql`, no `V1__`**: `DataBaseStartup.executeMigrate` configura `baselineOnMigrate(true)` **sin** `baselineVersion`, así que sobre una base de datos ya poblada Flyway hace baseline en la versión 1 y da por aplicadas —sin ejecutarlas— todas las migraciones ≤ 1; un `V1__` no correría nunca justo donde hace falta. Cualquier migración futura de este proyecto MUST seguir numerando por encima de 1 mientras esa configuración no cambie. El script se describe (no se escribe) porque el diseño no materializa código; debe buscar la restricción por catálogo y no fallar en una base de datos nueva donde la tabla aún no existe. Alternativa descartada: resetear la base de datos en cada entorno (`agent_docs/deploy.md`), porque no serviría en un entorno con datos reales.

6. **Nombre de la tabla.** Se asume `criptografia_certificado_digital`, por la convención de Axelor `<módulo>_<entidad en snake_case>` con `<module name="criptografia">`. No se ha podido confirmar contra una base de datos viva (el esquema local está vacío), y por eso el script del paso 1 debe localizar la restricción por catálogo y tolerar que la tabla no exista.

7. **El título del enum `CLASSPATH` contiene la errata «dentro del del WAR».** El spec la escribe corregida («dentro del WAR») en los escenarios, pero el título es preexistente y corregirlo no está en el delta, así que se conserva verbatim (mínima intrusión). `test-e2e-desc.md` usa el literal **real** de la vista, que es el que verá el navegador.

8. **Numeración de las `U-`.** Se numeran 001…006 en correspondencia 1:1 con las `RUI-certificados-digitales-formulario-001…006` del spec, para que la trazabilidad se lea de un vistazo. Los `.desc.md` ya persistidos de una iniciativa anterior citan `U-certificados-digitales-001` con otro significado (el «Habilitado por defecto», que aquí es U-certificados-digitales-006); son artefactos congelados «as-tested» y no se reescriben.

9. **`R-CertificadoDigital-001` SÍ es una regla «compleja»; las demás no.** R-001 necesita un **tipo propio** del dominio de la regla —el `record DatosTitular`, que no es una entidad JPA y no existía—, que es exactamente uno de los criterios de `reglas-complejas.md` §1; además tiene casos de error propios que conviene dejar escritos (usuario duplicado con el mismo DNI en `User.findByDni`, usuario con nombre o apellidos vacíos, DNI nulo o en blanco). Por eso se crea `design/rules/R-CertificadoDigital-001.md` con su análisis, sus tipos, su diagrama de secuencia y su tabla de errores. R-002 (`fireActionRule_ConservarDni`) y R-003 (`fireActionRule_ConservarDatosTitularDelUsuario`) **no** son complejas: se reducen a unas asignaciones desde el original, sin clases auxiliares, tipos propios, interfaces, máquina de estados, integración externa ni algoritmo no trivial, así que se documentan inline en el comentario de su método. (El helper de lectura `getCertificadoHabilitado` no entra en este análisis porque no es una regla `R-`; ver nota 15.)

10. **Ningún cableado Guice nuevo.** `CertificadoDigitalService` es un `ModelService` y lo descubre `ModelServiceFactory` por convención de nombre y paquete; el controlador solo inyecta `ModelServiceFactory`. `CriptografiaModule` no se toca y no se carga `k-guice`.

11. **Los tests unitarios existentes se rompen y hay que adaptarlos.** `CertificadoDigitalServiceImplTest` mockea `repository.findByDni(...)` en una veintena de casos; al sustituirse el finder, esos mocks deben apuntar a `findByDniHabilitados(...)` (que devuelve un `Query<CertificadoDigital>`, así que el mock encadena `.fetchOne()`). El `UserRepository` inyectado como campo `@Inject` (Paso 4) se mockea igual que cualquier otro colaborador — es una de las razones para inyectarlo en vez de resolverlo con `JpaRepository.of(...)` dentro del helper. La descripción completa de los tests unitarios —los adaptados y los nuevos de V-001…V-005, R-001…R-003 y el helper de lectura `getCertificadoHabilitado`— la materializa `test-unit-desc.md` en la fase siguiente del pipeline.

12. **La acción de pantalla es ESCALAR y comparte el cálculo con la regla de guardado.** Se descartó la forma «recibe y devuelve la entidad» (`rellenarTitular(CertificadoDigital)`) por tres motivos, todos verificados contra el código real: (a) obligaría a declarar un `allowProperties` que `k-sistemas/servicios.md` marca como ❌ para acciones escalares; (b) el controlador tendría que construir el bean con `ActionRequestHelper.getModel(...)`, que cuando el request trae `id` devuelve `jpaRepository.find(id)` —una entidad **gestionada** por JPA, con el mapa del cliente copiado encima—, de modo que asignarle el titular arriesgaría persistir esos valores en el flush de la transacción de la petición; hoy solo sería inocuo por un efecto colateral de la UI (el `readonlyIf="id != null"` del DNI impide que el `onChange` se dispare en un registro ya guardado), y una dependencia así no debe quedar implícita; y (c) duplicaría en dos sitios la lógica «hay usuario / no hay usuario», con lo que la pantalla podría prometer un titular distinto del que el servidor acabaría persistiendo. Con la forma escalar, `getDatosTitularByDni` y `fireActionRule_AsignarTitular` invocan **el mismo** helper privado `resolverDatosTitular(String dni)` y no pueden divergir. El mecanismo de vista no cambia: sigue siendo un `action-method` colgado del `onChange` del DNI, y U-001/U-002 se materializan igual.

13. **Análisis de impacto: ningún consumidor fuera del subsistema se toca.** El delta cambia **cómo** `CertificadoDigitalServiceImpl` localiza el certificado de un DNI (helper de lectura `getCertificadoHabilitado`, nota 15), pero **ninguna firma pública cambia**: `getAlmacenClaveByDni(String)`, `getAlmacenClaveByDni(String, String)` y `getSituacionFirmaByDni(String)` siguen igual, y sus contratos de «no hay certificado» (`null` y `SituacionFirma.SIN_CERTIFICADO`) también. Consumidores comprobados en el árbol real, **todos ellos sin cambios**: `subsystem/criptografia/util/CertificadoDigitalHelper`, `subsystem/criptografia/service/AlmacenClaveResolver`, `subsystem/criptografia/service/FirmaEnServidorService`, `subsystem/firmas` (`controller/TareaFirmaController`, `service/impl/TareaFirmaServiceImpl`), `tramites/util/firma` (`FirmaServidorController`, `FirmaServidorRules.kt`) y `tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/PhaseEventManagerImpl`. Lo único que cambia para ellos es el **comportamiento** que el spec pide: con varios certificados del mismo DNI se obtiene el habilitado y, si no hay ninguno habilitado, se comportan como si la persona no tuviera certificado — que es justo lo que ya hacían cuando la única entrada estaba deshabilitada. `/sdd-implementer` **MUST NOT** modificar ninguno de esos ficheros.

14. **Ficheros fuera de `design/` que la implementación debe tocar.** Este diseño no escribe en el árbol del proyecto: todo lo que hay que crear o modificar está en la tabla «Ficheros a crear o modificar» y lo materializa `/sdd-implementer`. En particular, el `V2__certificado_digital_dni_no_unico.sql` **no** existe todavía en `src/main/resources/...` y la carpeta de migraciones sigue vacía a día de hoy.

15. **El nuevo *lookup* de la firma en servidor NO se numera como regla `R-`.** El `# Objetivo` del `specification.md` y la descripción de `entity-CertificadoDigital.md` piden que «la firma en servidor use el certificado **habilitado** de ese DNI y, si no hay ninguno, se comporte como si la persona no tuviera certificado». Eso **no** proviene de ninguna `RN-`/`RES-`/`VAL-`/`RUI-`/`CC-` del spec (no hay nada que listar en «Reglas del spec descartadas», que solo cubre reglas numeradas) y **no** es una regla de negocio en el sentido de la taxonomía: no escribe en ninguna entidad y no corre desde `insert`/`update`/`remove` ni desde ninguna operación custom de escritura, así que no tiene momento `Antes`/`Después` de `repository.save/remove`. Numerarlo como `R-CertificadoDigital-004` con momento «Lectura» inventaba un valor que la taxonomía no admite, así que el identificador se ha retirado. Se materializa como el **helper privado de lectura** `getCertificadoHabilitado(String dni)` del bloque «Otras funciones» (Paso 4.5), punto único por el que `getAlmacenClaveByDni` y `getSituacionFirmaByDni` obtienen el certificado de una persona; el filtro por `enabled` vive en el finder `findByDniHabilitados` del dominio (Paso 2), y la retirada del chequeo posterior del flag está en «Eliminaciones declaradas». Las reglas de negocio del diseño siguen siendo exactamente tres: R-CertificadoDigital-001, -002 y -003.
