---
type: design
---

# Diseño: Smoke test + menú «Desarrollador»

**Objetivo:** subsistema CRUD de prueba (`SmokeTest`) accesible solo por el Administrador, con dos fechas selladas por el servidor, bajo un nuevo menú de primer nivel «Desarrollador» que también acoge «Utilidades de PDF».
**Capa:** subsystem/smoketest
**Especificación de origen:** .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones, k-datainit

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/domains/SmokeTest.xml` | Crear | k-sistemas (modelos.md) | Entidad `SmokeTest` (texto + 2 fechas servidor) |
| `subsystem/smoketest/service/SmokeTestService.java` | Crear | k-sistemas (servicios.md) | Interfaz `SmokeTestService extends ModelService<SmokeTest>` (sin métodos propios) |
| `subsystem/smoketest/service/impl/SmokeTestServiceImpl.java` | Crear | k-sistemas (servicios.md), k-secure-coding, k-validaciones | Sella fechas servidor, valida texto, whitelists |
| `subsystem/smoketest/controller/SmokeTestController.java` | Crear | k-sistemas (controladores.md) | `validateSave` (pre-valida antes del `save`) |
| `subsystem/smoketest/views/SmokeTest.xml` | Crear | k-vistas (grids.md, forms.md, actions.md) | `<action-view>` Main: grid + form CRUD |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir «Desarrollador» + «Smoke test»; reparentar y restringir «Utilidades de PDF» |
| `subsystem/smoketest/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de binding del permiso del subsistema |
| `subsystem/smoketest/data-init/input/auth-smoketest.xml` | Crear | k-datainit | **Solo** la definición del permiso `SmokeTest.all` (el enlace grupo→permiso vive en el `auth.xml` global) |
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Añadir `SmokeTest.all` al grupo `admins` **y** quitar `PdfUtilities.all` del grupo `users` (ver §Notas) |

> Raíz de los ficheros del subsistema: `src/main/java/com/educaflow/subsystem/smoketest/`. Las clases Java generadas a partir de `domains/SmokeTest.xml` (entidad `SmokeTest`, `SmokeTestRepository`) las produce el build en `db/` — no se escriben a mano.

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto, ver Paso 7). El código Java (servicio, impl, controlador) es lo único que se implementa a partir de las firmas y comentarios de este diseño. El contenido de `data-init` (Paso 8) está dado verbatim en este `design.md`.

---

## Pasos

### Paso 1 — Dominio `SmokeTest`

Fichero materializado: `design/domains/SmokeTest.xml` → copiar a `subsystem/smoketest/domains/SmokeTest.xml`.

Resumen estructural:
- `module name="smoketest" package="com.educaflow.subsystem.smoketest.db"`.
- `entity SmokeTest` con tres campos:
  - `texto` (`string`, `namecolumn="true"`) — **origen cliente**. RES-001 (texto obligatorio) **NO** se declara con `required="true"`: se valida en servidor (V-SmokeTest-001) para emitir el mensaje exacto y garantizar el rechazo del servidor (ver §Notas).
  - `fechaCreacion` (`datetime`) — **origen servidor** (CC-001). Sin `required` (campo rellenado por el sistema, ver `k-validaciones/modelos.md`).
  - `fechaUltimaModificacion` (`datetime`) — **origen servidor** (CC-002). Sin `required`.
- Sin relaciones (modelo independiente, sin centro/usuario/expediente — Fuera de alcance del spec).

Verificar: el build genera `com.educaflow.subsystem.smoketest.db.SmokeTest` y `SmokeTestRepository` sin errores (`./run.sh` compila).

### Paso 2 — Servicio `SmokeTestService` / `SmokeTestServiceImpl`

**Interfaz** `com.educaflow.subsystem.smoketest.service.SmokeTestService`:

```java
public interface SmokeTestService extends ModelService<SmokeTest> { }
```
- No declara métodos propios. `insert`/`update`/`remove`, sus `validateInsert/Update/Remove` y sus `allowPropertiesInsert/Update/Remove` se **heredan** de `ModelService<SmokeTest>` (defaults en `DefaultModelService`). **MUST NOT** re-declararlos (servicios.md).

**Implementación** `com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl extends DefaultModelService<SmokeTest> implements SmokeTestService`.

Orden de bloques: (1) acciones, (2) Métodos de Validación, (3) AllowProperties, (4) Action Rules.

```java
// Constructor obligatorio — lo invoca ModelServiceFactory por reflexión.
public SmokeTestServiceImpl(Class<SmokeTest> model, Repository<SmokeTest> repository);
//   super(model, repository);

// --- (1) Acciones ---

public SmokeTest insert(SmokeTest smokeTest);
//   Sobrescribe insert (genérico: lo invoca la acción `save`/endpoint REST /ws/rest/<FQN>).
//   1ª línea: validateInsert(smokeTest).ifPresent(BusinessMessages::throwIfInvalid)  → V-SmokeTest-001.
//   Aplica R-SmokeTest-001 vía fireActionRule_AsignarFechaCreacion(smokeTest) y
//   R-SmokeTest-002 vía fireActionRule_AsignarFechaUltimaModificacion(smokeTest) ANTES de persistir.
//   Persiste con repository.save(smokeTest). MUST NOT llamar a super.insert.

public SmokeTest update(SmokeTest smokeTest, SmokeTest original);
//   Sobrescribe update (genérico: lo invoca `save`/endpoint REST).
//   1ª línea: validateUpdate(smokeTest, original).ifPresent(BusinessMessages::throwIfInvalid)  → V-SmokeTest-001.
//   Aplica R-SmokeTest-002 + restauración del inmutable vía
//   fireActionRule_RefrescarFechaModificacion(smokeTest, original) ANTES de persistir.
//   Persiste con repository.save(smokeTest). MUST NOT llamar a super.update.

// remove: NO se sobrescribe (sin regla de borrado). Lo hereda de DefaultModelService.

// --- (2) Métodos de Validación ---

public Optional<BusinessMessages> validateInsert(SmokeTest smokeTest);
//   Aplica:
//     - V-SmokeTest-001 (Origen spec: RES-001) texto obligatorio: comprueba que `texto` no sea
//       null ni esté en blanco (trim). Mensaje debe transmitir: que el texto es obligatorio
//       (campo `texto`). [Literal exacto lo fija /sdd-implementer; el spec/test usa "El texto es obligatorio".]
//   Acumula en BusinessMessages y devuelve Optional.empty() si válido.

public Optional<BusinessMessages> validateUpdate(SmokeTest smokeTest, SmokeTest original);
//   Aplica:
//     - V-SmokeTest-001 (Origen spec: RES-001) texto obligatorio: misma comprobación que en
//       validateInsert (el texto sigue siendo obligatorio al modificar).

// --- (3) AllowProperties (frontera de confianza; ver §Frontera de confianza) ---

public AllowProperties allowPropertiesInsert();
//   Whitelist: createAllowProperties(Map.of("texto", Map.of())).
//   `fechaCreacion` y `fechaUltimaModificacion` quedan FUERA (campos servidor, ver k-secure-coding §3.2).

public AllowProperties allowPropertiesUpdate();
//   Whitelist: createAllowProperties(Map.of("texto", Map.of())).
//   Las dos fechas quedan FUERA: `fechaCreacion` es inmutable y `fechaUltimaModificacion` la
//   recalcula el servidor; el cliente no puede dictarlas ni por Vía A ni por Vía B.

// --- (4) Action Rules ---

private void fireActionRule_AsignarFechaCreacion(SmokeTest smokeTest);
//   Aplica R-SmokeTest-001 (Origen spec: CC-001, campo `fechaCreacion` clasificado servidor) en el alta:
//   asignación INCONDICIONAL `smokeTest.setFechaCreacion(LocalDateTime.now())`.
//   MUST NOT añadir guarda `if (... == null)`: permitiría colar una fecha falsificada por el
//   endpoint REST genérico (k-secure-coding §3.3). El cliente NO puede dictar este campo.

private void fireActionRule_AsignarFechaUltimaModificacion(SmokeTest smokeTest);
//   Aplica R-SmokeTest-002 (Origen spec: CC-002, campo `fechaUltimaModificacion` servidor) en el alta:
//   asignación INCONDICIONAL `smokeTest.setFechaUltimaModificacion(LocalDateTime.now())`.
//   MUST NOT añadir guarda `if (... == null)`: permitiría colar una fecha falsificada por el
//   endpoint REST genérico (k-secure-coding §3.3). El cliente NO puede dictar este campo.

private void fireActionRule_RefrescarFechaModificacion(SmokeTest smokeTest, SmokeTest original);
//   Aplica R-SmokeTest-002 (Origen spec: CC-002) en la modificación:
//   asignación INCONDICIONAL `smokeTest.setFechaUltimaModificacion(LocalDateTime.now())`.
//   Restaura el inmutable `fechaCreacion`: `smokeTest.setFechaCreacion(original.getFechaCreacion())`
//   (R-SmokeTest-001 fija la fecha de creación solo en el alta; en update NO se recalcula).
//   Asignaciones INCONDICIONALES, sin `if`. El cliente NO puede dictar ninguna de las dos fechas.
```

> **MUST NOT** crear módulo Guice: `SmokeTestServiceImpl` es un `ModelService` y `ModelServiceFactory` lo descubre por convención de nombre/paquete (servicios.md). No hay repositorio personalizado (sin queries propias) → sin `db/repo/`.

Verificar: `ModelServiceFactory.resolve(SmokeTest.class)` devuelve la impl (nombre exacto `SmokeTestServiceImpl` en `service.impl`). `./run.sh` compila.

### Paso 3 — Repositorios

No aplica: `SmokeTest` no tiene queries propias ni finders; usa el repositorio generado por Axelor. **MUST NOT** poner `repository="abstract"` en el dominio.

### Paso 4 — Controlador `SmokeTestController`

`com.educaflow.subsystem.smoketest.controller.SmokeTestController` (un controlador por entidad). Inyecta `@Inject private ModelServiceFactory modelServiceFactory;`.

```java
@CallMethod
public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);
//   Pre-validación del guardado para mostrar el error de negocio como modal ANTES de la acción `save`.
//   - Resuelve el servicio: (SmokeTestService) modelServiceFactory.resolve(SmokeTest.class).
//   - ActionRequestHelper<SmokeTest> sobre actionRequest; ActionResponseHelper sobre actionResponse.
//   - original = actionRequestHelper.getOriginalModel().
//   - Si actionRequestHelper.getId()==null  (alta):
//       smokeTest = actionRequestHelper.getModel(smokeTestService.allowPropertiesInsert());
//       validationResult = smokeTestService.validateInsert(smokeTest);
//     Si no (modificación):
//       smokeTest = actionRequestHelper.getModel(smokeTestService.allowPropertiesUpdate());
//       validationResult = smokeTestService.validateUpdate(smokeTest, original);
//   - Si validationResult.isPresent(): actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get()).
//   Sin @Transactional (solo lee y valida; no persiste). Parámetros nombrados actionRequest/actionResponse.
```

> **MUST NOT** exponer `@CallMethod` para `insert`/`update`/`remove`: el guardado y el borrado usan las acciones de framework `save` y `delete` (controladores.md). `validateSave` es solo un hook de validación previo, igual que `LeyEducativaController.validateSave`. No se necesita `validateDelete` (el borrado no tiene reglas).

Verificar: la `<action-method>` de la vista referencia exactamente `com.educaflow.subsystem.smoketest.controller.SmokeTestController#validateSave`. `./run.sh` compila.

### Paso 5 — Módulos Guice

No aplica (solo hay un `ModelService`, descubierto por la factoría). Sin `module/`.

### Paso 6 — Vistas `SmokeTest.xml`

Fichero materializado: `design/views/SmokeTest.xml` → copiar a `subsystem/smoketest/views/SmokeTest.xml`. Un único `<action-view>` (`@Main`) → un fichero (regla "un action-view por fichero").

Resumen estructural:
- `action-view subsysSmokeTest.SmokeTest@Main-action` (title "Smoke test", model `SmokeTest`): grid `@Main-grid` + form `@Main-form`; `view-param show-toolbar-form=false`, `forceEdit=true`.
- `grid @Main-grid` (`groups="admins"`): columnas `texto`, `fechaCreacion`, `fechaUltimaModificacion`; `orderBy="-fechaCreacion"` (más recientes primero; **U-smoke-test-002**, Ordenación por defecto del spec); `allowSearchFields="true"` (búsqueda por texto); `canNew="true"` ("Nuevo"); `canDelete="true"` ("Eliminar" por fila/selección); `canEditOnClick="true"` (abre el form al pulsar fila).
- `form @Main-form` (`groups="admins"`): panel "Smoke test" con `texto` editable y `fechaCreacion`/`fechaUltimaModificacion` con `readonly="true"` (**U-smoke-test-001 / RUI-001**). Botón "Guardar" → `@Main-btnSave-action`; `canBackOnSave="true"` (vuelve al listado tras guardar).
- Acciones: `action-group @Main-btnSave-action` = `[@Main-Remote-validateSave-action, save]`; `action-method @Main-Remote-validateSave-action` → `SmokeTestController.validateSave`.

> Las fechas en `readonly` son sólo UX (RUI-001). La **defensa** de que el cliente no las dicte es la whitelist + asignación incondicional del Paso 2 (k-secure-coding §1).

Verificar: el menú abre el grid; "Nuevo" abre el form; guardar con texto sella las fechas; borrar desde la fila elimina. `./run.sh` arranca sin errores de vista.

### Paso 7 — Menús (modificar el `menus.xml` único del proyecto)

Fichero materializado: `design/menus.xml` (porción a fusionar) → fusionar en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

Acciones de la fusión:
1. **Añadir** el menú de primer nivel `desarrollador-menuitem` (title "Desarrollador", `order="90"`, `groups="admins"`) y su hijo `desarrollador-smokeTest-menuitem` (title "Smoke test", `action="subsysSmokeTest.SmokeTest@Main-action"`, `groups="admins"`, `order="1"`).
2. **Sustituir** el bloque existente de "Utilidades de PDF" (hoy de primer nivel: `utilidadesPdf-menuitem order="80"` **sin** `groups`, con sus 3 hijos sin `groups`) por la versión del fichero: `utilidadesPdf-menuitem` pasa a `parent="desarrollador-menuitem"`, `order="2"`, `groups="admins"`; y sus 3 hijos (Información, Posiciones Firma, Posición Autofirma__!!) reciben `groups="admins"`. Sus `action` (`subsysPdfUtilities.PdfUtilities@*-action`) **no cambian** (las pantallas de PDF no se tocan, solo ubicación y acceso).

Verificar: con el usuario `admin` aparece el menú "Desarrollador" con "Smoke test" y "Utilidades de PDF" colgando; "Utilidades de PDF" ya no está en primer nivel; un usuario del grupo `users` no ve ninguno de los dos.

### Paso 8 — Seguridad (data-init del subsistema)

El subsistema es dueño de su permiso (k-datainit). Crear `subsystem/smoketest/data-init/` con:

`data-init/input-config.xml`:
```xml
<?xml version="1.0"?>
<xml-inputs priority="10" xmlns="http://axelor.com/xml/ns/data-import"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://axelor.com/xml/ns/data-import
  https://axelor.com/xml/ns/data-import/data-import_8.0.xsd">

    <input file="auth-smoketest.xml" root="auth">
        <bind node="permission" type="com.axelor.auth.db.Permission" search="self.name = :name" create="true" update="true">
            <bind node="@name" to="name"/>
            <bind node="@object" to="object"/>
            <bind node="can/@create" to="canCreate"/>
            <bind node="can/@read" to="canRead"/>
            <bind node="can/@write" to="canWrite"/>
            <bind node="can/@remove" to="canRemove"/>
            <bind node="can/@export" to="canExport"/>
        </bind>
    </input>
</xml-inputs>
```

`data-init/input/auth-smoketest.xml` (**solo** la definición del permiso; el enlace grupo→permiso NO va aquí — ver más abajo):
```xml
<?xml version="1.0"?>
<auth>
  <permission name="SmokeTest.all" object="com.educaflow.subsystem.smoketest.db.SmokeTest">
    <can create="true" read="true" write="true" remove="true" export="true"/>
  </permission>
</auth>
```

> **Por qué el enlace grupo→permiso NO va en el `data-init` del subsistema** (k-datainit; orden de carga por `priority` descendente): este `input-config.xml` tiene `priority="10"`, pero los grupos `admins`/`users` se crean en el `auth.xml` **global** (`src/main/resources/data-init/input/auth.xml`), cuyo manifiesto tiene `priority="-1"` y por tanto se carga el **ÚLTIMO**. Si se intentara enlazar `SmokeTest.all` al grupo `admins` aquí (en `priority=10`), el grupo `admins` **aún no existiría** y un `group`-bind con `create="false"` no encontraría nada → el enlace nunca se crearía. Por eso el enlace se hace en el `auth.xml` global (en `priority=-1`, cuando el permiso `SmokeTest.all` ya está creado por este `data-init`). Esto respeta además la convención de k-datainit: el `auth-<sistema>.xml` del subsistema define **solo** permisos; el enlace grupo→permiso vive en el `auth.xml` global.

**Enlazar `SmokeTest.all` al grupo `admins`** (Seguridad del spec: el Administrador tiene CRUD sobre `SmokeTest`): **modificar** `src/main/resources/data-init/input/auth.xml` para **añadir** la línea `<permission name="SmokeTest.all"/>` dentro del bloque `<group code="admins">`. El grupo `users` **no** recibe el permiso.

Regla de acceso (lenguaje natural): **solo el grupo `admins`** (Administrador, login `admin`) tiene CRUD completo sobre `SmokeTest`. El grupo `users` no recibe el permiso → no accede ni por menú ni por el endpoint REST genérico. No hay filtrado multicentro (el spec lo excluye: `SmokeTest` no tiene `centro`).

**Restricción de "Utilidades de PDF" al Administrador** (Seguridad del spec): además del menú `groups="admins"` (Paso 7), **modificar** `src/main/resources/data-init/input/auth.xml` para quitar `<permission name="PdfUtilities.all"/>` del bloque `<group code="users">` (el grupo `admins` lo mantiene). Ver §Notas sobre el alcance real de esta modificación.

Verificar: arrancar con `./run.sh`; `admin` accede a "Smoke test"; un usuario `users` no ve el permiso `SmokeTest.all`.

### Paso 9 — Datos iniciales

No aplica: la tabla `SmokeTest` arranca **vacía** (los propios escenarios crean y borran sus datos). No hay catálogos precargados.

### Paso 10 — Verificación final

Compilar, ejecutar tests y arrancar:

```bash
./run.sh
```

Comprobar: compila sin errores; el menú "Desarrollador" → "Smoke test" abre el listado; alta/consulta/modificación/borrado funcionan; las fechas las sella el servidor; el alta sin texto se rechaza con el mensaje del servidor.

---

## Frontera de confianza — AllowProperties por acción

El único `@CallMethod` del diseño es `SmokeTestController.validateSave`, que pre-valida el guardado consumiendo `allowPropertiesInsert()` (rama alta) y `allowPropertiesUpdate()` (rama modificación) del servicio. Esas mismas whitelists son la defensa del flujo de guardado genérico (`save` / `POST /ws/rest/<FQN>`), que filtra el JSON entrante con ellas antes de llegar a `insert`/`update`. Reglas aplicadas: `k-secure-coding` §3.

### `SmokeTestServiceImpl.insert` (whitelist consumida por `SmokeTestController.validateSave`, rama alta)

Entidad: `SmokeTest`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-SmokeTest.md` → `texto`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|---------------------------------------------|
| `texto` | cliente | sí | Input directo del usuario (en `Input AllowProperties` de `Crear`). |
| `fechaCreacion` | servidor | **NO** | CC-001. Asignada incondicionalmente en `insert` → `fireActionRule_AsignarFechaCreacion`. |
| `fechaUltimaModificacion` | servidor | **NO** | CC-002. Asignada incondicionalmente en `insert` → `fireActionRule_AsignarFechaUltimaModificacion`. |

### `SmokeTestServiceImpl.update` (whitelist consumida por `SmokeTestController.validateSave`, rama modificación)

Entidad: `SmokeTest`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-SmokeTest.md` → `texto`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|---------------------------------------------|
| `texto` | cliente | sí | Input directo del usuario (en `Input AllowProperties` de `Modificar`). |
| `fechaCreacion` | servidor | **NO** | Inmutable (no aparece en `Modificar`). Restaurada desde `original` en `update` → `fireActionRule_RefrescarFechaModificacion`. |
| `fechaUltimaModificacion` | servidor | **NO** | CC-002. Recalculada incondicionalmente en `update` → `fireActionRule_RefrescarFechaModificacion`. |

---

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (V)

| V | Origen spec | Ubicación | Lógica / Mensaje |
|---|-------------|-----------|------------------|
| V-SmokeTest-001 | RES-001 | `SmokeTestServiceImpl.validateInsert` y `.validateUpdate` | `texto` no null ni en blanco; mensaje transmite que el texto es obligatorio (campo `texto`). Servidor = fuente de verdad (ESC-005). |

### Reglas de negocio / campos calculados (R)

| R | Origen spec | Ubicación | Momento / Efecto |
|---|-------------|-----------|------------------|
| R-SmokeTest-001 | CC-001 | `SmokeTestServiceImpl.fireActionRule_AsignarFechaCreacion` (desde `insert`) | Antes de `repository.save`. Asigna INCONDICIONALMENTE `fechaCreacion = now` solo en el alta; en `update` se restaura desde `original` (inmutable). Campo servidor. |
| R-SmokeTest-002 | CC-002 | `SmokeTestServiceImpl.fireActionRule_AsignarFechaUltimaModificacion` (insert) y `fireActionRule_RefrescarFechaModificacion` (update) | Antes de `repository.save`. Asigna INCONDICIONALMENTE `fechaUltimaModificacion = now` en alta y en cada modificación. Campo servidor. |

### Reglas de UI (U)

| U | Origen spec | Ubicación | Atributo |
|---|-------------|-----------|----------|
| U-smoke-test-001 | RUI-001 | `views/SmokeTest.xml` form `@Main-form`, campos `fechaCreacion` y `fechaUltimaModificacion` | `readonly="true"` (disparador continuo, condición Siempre). |
| U-smoke-test-002 | Ordenación por defecto (screen-smoke-test.md) | `views/SmokeTest.xml` grid `@Main-grid` | `orderBy="-fechaCreacion"` (más recientes primero). |

### Clasificación de campos (cliente/servidor)

| Campo | Origen | Respaldo |
|-------|--------|----------|
| `texto` | cliente | En whitelist `insert`/`update`; validado por V-SmokeTest-001. |
| `fechaCreacion` | servidor | CC-001 → R-SmokeTest-001 (Antes, alta). Fuera de whitelists. |
| `fechaUltimaModificacion` | servidor | CC-002 → R-SmokeTest-002 (Antes, alta y modificación). Fuera de whitelists. |

---

## Tests

- **Tests E2E** (Given/When/Then): descritos en `test-e2e-desc.md` (este diseño), cubren ESC-001…ESC-005.
- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline) — sobre `SmokeTestServiceImpl` (validación de texto, sellado incondicional de fechas en insert, refresco + restauración de inmutable en update, whitelists).

---

## Reglas del spec descartadas

Ninguna. Todas las reglas del spec están ubicadas: RES-001 → V-SmokeTest-001; CC-001 → R-SmokeTest-001 (campo `fechaCreacion` servidor); CC-002 → R-SmokeTest-002 (campo `fechaUltimaModificacion` servidor); RUI-001 → U-smoke-test-001; Ordenación por defecto → U-smoke-test-002.

---

## Notas y supuestos

1. **RES-001 implementada en servidor, no como `required="true"`.** El spec (ESC-005) exige que el alta sin texto sea **rechazada por el servidor** con el mensaje exacto «El texto es obligatorio». `required="true"` en el modelo produce el mensaje **estándar** de Axelor (no el literal) y actúa como validación de cliente. Para honrar el literal y la frase "rechazada por el servidor" se implementa como `V-SmokeTest-001` en `validateInsert`/`validateUpdate` (k-validaciones permite el camino `validate*` cuando la restricción no se puede declarar con el mensaje requerido). `validateSave` del controlador la muestra como modal antes del `save`, y además la valida el flujo genérico (Vía B) — defensa en profundidad (k-secure-coding §9).
2. **«Administrador» = grupo `admins`.** El spec define un único usuario administrador global (login `admin`/`admin`, alcance global). Se mapea al grupo Axelor `admins` (igual que el resto de menús `groups="admins"`), no a un tipo de usuario `ADMINISTRADOR` por centro. Por eso menús, grid, form y permiso usan `groups="admins"` / grupo `admins`.
3. **Restricción de «Utilidades de PDF» al Administrador.** Mecanismo inmediato y suficiente para la UI: el menú pasa a `groups="admins"` (Paso 7) → desaparece para `users`. Para revocar también el permiso de **objeto** `PdfUtilities.all` del grupo `users` se quita esa línea del `<group code="users">` en el `auth.xml` global (Paso 8). **Caveat data-import:** el data-import de Axelor sobre una colección (`to="permissions"`) **añade/actualiza**, no **elimina**; quitar la línea surte efecto en una **BD recreada** (`./run.sh` con reset), pero en una BD ya poblada el permiso ya concedido al grupo `users` debe retirarse manualmente (o por migración). Como `PdfUtilities` no expone datos de negocio reales y el spec marca "no se modifican las pantallas", la combinación menú `groups="admins"` + ausencia del permiso en `users` (en BD limpia) cumple el requisito.
4. **`SmokeTest` sin centro.** Por mandato del spec (Fuera de alcance: sin asociación a centro ni filtrado multicentro), no se aplica el patrón multicentro de `k-secure-coding` §4: el Administrador tiene alcance global y ve todos los registros.
5. **Borrado sin reglas.** ESC-004 es un borrado simple; no hay `validateRemove` ni `R-` de borrado. La fila del grid usa la acción de framework `delete` (`canDelete="true"`), sin controlador propio.
6. **Sin reglas complejas ni módulo Guice.** El sellado de fechas son asignaciones triviales en el servicio (no cumplen criterios de `reglas-complejas.md`), por lo que no hay `rules/`. El único servicio es un `ModelService` → sin `module/`.
