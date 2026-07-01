---
type: design
---

# Diseño: Smoke Test

**Objetivo:** Crear el subsistema `smoketest` con una entidad `SmokeTest` y su pantalla CRUD para que el Administrador verifique rápidamente que la aplicación y el acceso al servidor funcionan; añadir el ítem «Smoke test» bajo el menú «Desarrollador» (ya existente).

**Capa:** subsystem/smoketest

**Especificación de origen:** .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/specification.md

**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/domains/SmokeTest.xml` | Crear | k-sistemas (modelos.md) | Entidad SmokeTest |
| `subsystem/smoketest/service/SmokeTestService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio |
| `subsystem/smoketest/service/impl/SmokeTestServiceImpl.java` | Crear | k-sistemas (servicios.md) | Implementación con validate*, fireActionRule_*, allowProperties* |
| `subsystem/smoketest/views/SmokeTest.xml` | Crear | k-vistas (grids.md, forms.md, actions.md) | Grid, formulario y acciones de SmokeTest |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir ítem «Smoke test» bajo «Desarrollador» (order=1) |
| `subsystem/smoketest/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de carga del permiso del subsistema |
| `subsystem/smoketest/data-init/input/auth-smoketest.xml` | Crear | k-datainit | Definición del permiso SmokeTest.all |
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Añadir **únicamente** la **asignación** del permiso `SmokeTest.all` al grupo `admins` (solo la referencia `<permission name="SmokeTest.all"/>`; la definición completa vive únicamente en `auth-smoketest.xml`) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

---

## Pasos

### Paso 1 — Dominio: entidad SmokeTest

Crear `src/main/java/com/educaflow/subsystem/smoketest/domains/SmokeTest.xml`.

El fichero materializado está en `design/domains/SmokeTest.xml`. **Resumen estructural:**

- Módulo `smoketest`, paquete `com.educaflow.subsystem.smoketest.db`.
- Entidad `SmokeTest` con tres campos:
  - `texto` (`<string large="true">`) — campo **cliente**. `large="true"` mapea la columna a tipo `TEXT` en la BD (sin límite de longitud). Sin `required="true"` en el dominio: la validación con el mensaje exacto del spec («El texto es obligatorio») vive en `validateInsert`/`validateUpdate` del servicio (V-SmokeTest-001). Esto evita que Bean Validation genere un mensaje genérico antes de que el servicio pueda producir el mensaje esperado por ESC-005.
  - `fechaCreacion` (`<datetime>`) — campo **servidor** (CC-001). Sin `required="true"` porque lo asigna el servidor en `insert` vía `fireActionRule_AsignarFechaCreacion`.
  - `fechaUltimaModificacion` (`<datetime>`) — campo **servidor** (CC-002). Sin `required="true"` porque lo asigna el servidor en `insert` y `update` vía `fireActionRule_ActualizarFechaUltimaModificacion`.
- Sin relaciones, sin enumerados, sin finders personalizados.

**Clasificación de campos:**

| Campo | Origen | AllowProperties insert | AllowProperties update |
|-------|--------|------------------------|------------------------|
| `texto` | cliente | sí | sí |
| `fechaCreacion` | servidor | **NO** — asignado incondicionalmente en `fireActionRule_AsignarFechaCreacion` | **NO** — inmutable tras la creación; restaurado desde `original` en `update` |
| `fechaUltimaModificacion` | servidor | **NO** — asignado incondicionalmente en `fireActionRule_ActualizarFechaUltimaModificacion` | **NO** — recalculado incondicionalmente en `fireActionRule_ActualizarFechaUltimaModificacion` |

**Verificar:** `grep -r "SmokeTest" src/main/java/com/educaflow/subsystem/smoketest/domains/`

---

### Paso 2 — Servicio: SmokeTestService / SmokeTestServiceImpl

Crear los dos ficheros Java:

- `src/main/java/com/educaflow/subsystem/smoketest/service/SmokeTestService.java`
- `src/main/java/com/educaflow/subsystem/smoketest/service/impl/SmokeTestServiceImpl.java`

No se crea módulo Guice: `ModelServiceFactory` descubre `SmokeTestServiceImpl` automáticamente por convención de nombre y paquete.

No se crea controlador: el CRUD es estándar (botones `save`/`delete`/`back` de Axelor) sin lógica de negocio adicional en `@CallMethod`.

#### Interfaz: `SmokeTestService.java`

FQN: `com.educaflow.subsystem.smoketest.service.SmokeTestService`

```
package com.educaflow.subsystem.smoketest.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.smoketest.db.SmokeTest;

public interface SmokeTestService extends ModelService<SmokeTest> {
    // Sin acciones propias adicionales.
    // El CRUD estándar (insert/update/remove) lo expone ModelService<T>.
    // Las sobrescrituras de validate*/allowProperties*/insert/update viven en la impl.
}
```

#### Implementación: `SmokeTestServiceImpl.java`

FQN: `com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl`

```java
// Clase: com.educaflow.subsystem.smoketest.service.impl.SmokeTestServiceImpl
// Extiende DefaultModelService<SmokeTest>, implementa SmokeTestService.

@Inject
public SmokeTestServiceImpl(AbstractSmokeTestRepository repository);
//   Constructor obligatorio para inyección Guice.
//   Pasa el repository a super(repository) y lo guarda como campo.

@Override
public AllowProperties allowPropertiesInsert();
//   Forma elegida: createAllowProperties (whitelist).
//   Campos cliente aceptados en insert: ["texto"].
//   fechaCreacion y fechaUltimaModificacion EXCLUIDOS de la whitelist:
//     son campos servidor asignados incondicionalmente por fireActionRule_*.

@Override
public AllowProperties allowPropertiesUpdate();
//   Forma elegida: createAllowProperties (whitelist).
//   Campos cliente aceptados en update: ["texto"].
//   fechaCreacion EXCLUIDO: inmutable tras creación (restaurado desde original en update).
//   fechaUltimaModificacion EXCLUIDO: recalculado incondicionalmente por fireActionRule_*.

@Override
public Optional<BusinessMessages> validateInsert(SmokeTest entity);
//   Aplica V-SmokeTest-001 (Origen spec: RES-001):
//     Comprueba que entity.getTexto() no sea null ni cadena vacía/blank.
//     Mensaje debe transmitir: que el texto es obligatorio (campo requerido).
//   Devuelve Optional.empty() si la validación pasa; Optional.of(mensaje) si falla.

@Override
public Optional<BusinessMessages> validateUpdate(SmokeTest entity, SmokeTest original);
//   Aplica V-SmokeTest-001 (Origen spec: RES-001):
//     Misma comprobación que validateInsert: texto no nulo ni vacío.
//     Mensaje debe transmitir: que el texto es obligatorio.
//   Devuelve Optional.empty() si pasa; Optional.of(mensaje) si falla.

@Override
public SmokeTest insert(SmokeTest entity);
//   1ª línea: validateInsert(entity).ifPresent(BusinessMessages::throwIfInvalid).
//   Invoca fireActionRule_AsignarFechaCreacion(entity)            — R-SmokeTest-001 (Antes).
//   Invoca fireActionRule_ActualizarFechaUltimaModificacion(entity)— R-SmokeTest-002 (Antes).
//   Persiste con repository.save(entity).  MUST NOT llamar a super.insert.
//   Devuelve la entidad guardada.

@Override
public SmokeTest update(SmokeTest entity, SmokeTest original);
//   1ª línea: validateUpdate(entity, original).ifPresent(BusinessMessages::throwIfInvalid).
//   Restaura inmutable: entity.setFechaCreacion(original.getFechaCreacion()).
//     Aunque el cliente envíe fechaCreacion, se restaura siempre desde original
//     (defensa anti mass-assignment, k-secure-coding §3.3).
//   Invoca fireActionRule_ActualizarFechaUltimaModificacion(entity)— R-SmokeTest-002 (Antes).
//   Persiste con repository.save(entity).  MUST NOT llamar a super.update.
//   Devuelve la entidad guardada.

private void fireActionRule_AsignarFechaCreacion(SmokeTest entity);
//   Aplica R-SmokeTest-001 (Origen spec: CC-001, campo `fechaCreacion` clasificado `servidor`).
//   Asignación INCONDICIONAL: entity.setFechaCreacion(LocalDateTime.now()).
//   MUST NOT añadir guarda `if (entity.getFechaCreacion() == null)`: permitiría que un
//   atacante vía el endpoint REST genérico cuele una fecha falsificada (k-secure-coding §3.3).
//   Momento: Antes (escribe en el mismo registro, antes de repository.save).

private void fireActionRule_ActualizarFechaUltimaModificacion(SmokeTest entity);
//   Aplica R-SmokeTest-002 (Origen spec: CC-002, campo `fechaUltimaModificacion` clasificado `servidor`).
//   Asignación INCONDICIONAL: entity.setFechaUltimaModificacion(LocalDateTime.now()).
//   MUST NOT añadir guarda `if (entity.getFechaUltimaModificacion() == null)`.
//   Invocada tanto en insert como en update (el spec dice que se recalcula en cada modificación).
//   Momento: Antes (escribe en el mismo registro, antes de repository.save).
```

**Verificar:** `./gradlew compileJava` sin errores.

---

### Paso 3 — Vistas: SmokeTest.xml

Crear `src/main/java/com/educaflow/subsystem/smoketest/views/SmokeTest.xml`.

El fichero materializado está en `design/views/SmokeTest.xml`. **Resumen estructural:**

- **`action-view` `subsysSmokeTest.SmokeTest@Main-action`** — abre el grid `@Main-grid` y el formulario `@Main-form`. Parámetros: `show-toolbar-form=false`, `forceEdit=true`.

- **Grid `subsysSmokeTest.SmokeTest@Main-grid`** — columnas: `texto`, `fechaCreacion`, `fechaUltimaModificacion`. Ordenación: `-fechaCreacion` (descendente, los más recientes primero, spec). `allowSearchFields="true"` para filtrar por texto. Los campos de fecha llevan `width="200px"` para acotar su columna al tamaño del formato datetime. Sin atributo `archived`.

- **Form `subsysSmokeTest.SmokeTest@Main-form`** — atributos `canAttach/canBack/canDelete/canNew/canSave/canMore` todos `false`; `canBackOnSave="true"`. Contiene:
  - Panel `SmokeTest`: `texto` (colSpan=12, editable), `fechaCreacion` (colSpan=6, `readonly="true"`, U-smoke-test-001), `fechaUltimaModificacion` (colSpan=6, `readonly="true"`, U-smoke-test-001).
  - Panel `buttons-panel`: `btnDelete` (btn-danger, left, `showIf="(id!=null)||(cid!=null)"`), `btnCancel` (outline, colOffset=6), `btnSave`.

- **Action-groups:**
  - `subsysSmokeTest.SmokeTest@Main-btnDelete-action` → `<action name="delete"/>`.
  - `subsysSmokeTest.SmokeTest@Main-btnCancel-action` → `<action name="back"/>`.
  - `subsysSmokeTest.SmokeTest@Main-btnSave-action` → `<action name="subsysSmokeTest.SmokeTest@Main-btnSave-validate-action"/>` + `<action name="save"/>`.

- **Action-validate `subsysSmokeTest.SmokeTest@Main-btnSave-validate-action`** — V-SmokeTest-001 (cliente), Origen spec: RES-001. `<error if="!texto" message="El texto es obligatorio"/>`.

**Verificar:** `xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd src/main/java/com/educaflow/subsystem/smoketest/views/SmokeTest.xml`

---

### Paso 4 — Menú: añadir «Smoke test» bajo «Desarrollador»

Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`.

El menuitem `desarrollador-menuitem` (`title="Desarrollador"`, `groups="admins"`, `order="90"`) **ya existe** en el fichero de menús (explorado en el análisis). El submenú «Utilidades de PDF» ya cuelga de él con `groups="admins"` y `order="2"`. Solo hay que fusionar el snippet de `design/menus.xml` (el nuevo `smoketest-menuitem` con `order="1"`):

```xml
<menuitem name="smoketest-menuitem"
          parent="desarrollador-menuitem"
          title="Smoke test"
          action="subsysSmokeTest.SmokeTest@Main-action"
          groups="admins"
          order="1"/>
```

**Verificar:** al arrancar, el menú «Desarrollador» → «Smoke test» es visible para `admin` y no visible para usuarios no-administradores.

---

### Paso 5 — Seguridad: permiso SmokeTest.all

Crear los ficheros de data-init del subsistema:

- `src/main/java/com/educaflow/subsystem/smoketest/data-init/input-config.xml` — manifiesto con `priority="20"` que carga `auth-smoketest.xml`.
- `src/main/java/com/educaflow/subsystem/smoketest/data-init/input/auth-smoketest.xml` — define el permiso `SmokeTest.all` sobre `com.educaflow.subsystem.smoketest.db.SmokeTest` con `create/read/write/remove/export = true`.

Además, modificar `src/main/resources/data-init/input/auth.xml` para añadir **únicamente** la **asignación** del permiso `SmokeTest.all` al grupo `admins`. La **definición** del permiso (bloque `<permission name="SmokeTest.all" object="...">...<can .../>...</permission>`) vive exclusivamente en `subsystem/smoketest/data-init/input/auth-smoketest.xml`; incluirla también en el auth.xml global sería redundante y viola k-datainit §2 (CRITICAL).

Asignación al grupo `admins` (dentro del bloque `<group code="admins">` existente):

```xml
<permission name="SmokeTest.all"/>
```

Sin esta asignación, el grupo `admins` no tendrá acceso real a `SmokeTest` aunque el permiso quede definido en `auth-smoketest.xml`.

**Descripción del permiso en lenguaje natural:**
- `SmokeTest.all` → grupo `admins` → puede crear, leer, modificar y borrar cualquier registro de `SmokeTest`. Alcance global (sin filtro por centro).

**Verificar:** al arrancar, la tabla de permisos de Axelor tiene `SmokeTest.all` asignado al grupo `admins`.

---

### Paso 6 — Verificación final

```bash
./run.sh
```

Verifica que:
1. La aplicación compila y arranca sin errores.
2. El menú «Desarrollador» → «Smoke test» aparece al iniciar sesión como `admin`/`admin`.
3. Se puede crear un registro con texto, y las fechas de creación y última modificación se rellenan solas.
4. Al modificar el texto de un registro existente, la fecha de última modificación se actualiza.
5. Se puede borrar un registro y desaparece del listado.
6. Si se intenta guardar con el campo «Texto» vacío, aparece el mensaje «El texto es obligatorio» y no se crea el registro.

---

## Trazabilidad Origen spec → V/R/U → ubicación

### V — Validaciones

| ID | Origen spec | Capa | Ubicación | Descripción |
|----|-------------|------|-----------|-------------|
| V-SmokeTest-001 | RES-001 | Servidor + Cliente | `SmokeTestServiceImpl.validateInsert` / `SmokeTestServiceImpl.validateUpdate`; `subsysSmokeTest.SmokeTest@Main-btnSave-validate-action` (XML views/SmokeTest.xml) | `texto` no puede ser null ni vacío/blank. Mensaje transmite: que el texto es obligatorio. |

### R — Reglas de negocio

| ID | Origen spec | Momento | Ubicación | Descripción |
|----|-------------|---------|-----------|-------------|
| R-SmokeTest-001 | CC-001 | Antes (insert) | `SmokeTestServiceImpl.fireActionRule_AsignarFechaCreacion` — invocada desde `SmokeTestServiceImpl.insert` antes de `repository.save` | Asignación INCONDICIONAL de `fechaCreacion = LocalDateTime.now()`. Campo servidor; no condicionada con `if`. |
| R-SmokeTest-002 | CC-002 | Antes (insert y update) | `SmokeTestServiceImpl.fireActionRule_ActualizarFechaUltimaModificacion` — invocada desde `SmokeTestServiceImpl.insert` y `SmokeTestServiceImpl.update` antes de `repository.save` | Asignación INCONDICIONAL de `fechaUltimaModificacion = LocalDateTime.now()`. Campo servidor; no condicionada con `if`. |

### U — Reglas de UI

| ID | Origen spec | Ubicación | Descripción |
|----|-------------|-----------|-------------|
| U-smoke-test-001 | RUI-001 | `views/SmokeTest.xml`, `subsysSmokeTest.SmokeTest@Main-form`, campos `fechaCreacion` y `fechaUltimaModificacion` con `readonly="true"` | Las fechas son siempre de solo lectura en el formulario (disparador: continuo, condición: siempre). |

---

## Seguridad

- **Rol con acceso:** solo el grupo `admins` (Administrador).
- **Alcance:** global. La entidad `SmokeTest` no tiene campo `centro` ni filtrado multicentro (la spec lo indica explícitamente).
- **Permisos:** `SmokeTest.all` — `create/read/write/remove/export=true` — asignado al grupo `admins`.
- **Menú:** `smoketest-menuitem` con `groups="admins"` — no visible para usuarios no-administradores.
- **Sin multi-centro:** no se aplica ningún filtro `centroActivo`. No se usa `AuthUtils.getUser().getCentroActivo()` en queries (la entidad no tiene campo `centro`).
- **Sin `@CallMethod`:** no hay sección «Frontera de confianza — AllowProperties por acción» porque el diseño no declara ninguna acción invocada desde un `@CallMethod`. La defensa anti mass-assignment se implementa en `allowPropertiesInsert`/`allowPropertiesUpdate` del servicio (whitelists explícitas) y en la asignación incondicional de los campos servidor dentro de `fireActionRule_*`.

---

## Tests

- **Tests E2E** (Playwright): descritos en `test-e2e-desc.md`, materializados desde ESC-001…ESC-005.
- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).

---

## Reglas del spec descartadas

Ninguna. Todas las reglas del spec están ubicadas en el diseño:

| ID spec | Tipo | Mapeo en el diseño |
|---------|------|--------------------|
| RES-001 | Restricción → V | V-SmokeTest-001 |
| CC-001 | Campo calculado → R (Antes) + campo servidor | R-SmokeTest-001 + campo `fechaCreacion` (servidor) |
| CC-002 | Campo calculado → R (Antes) + campo servidor | R-SmokeTest-002 + campo `fechaUltimaModificacion` (servidor) |
| RUI-001 | Regla de UI → U | U-smoke-test-001 |

---

## Notas y supuestos

1. **El menú «Desarrollador» ya existe.** La spec describe «Desarrollador» como un «menú de primer nivel nuevo»; sin embargo, inspeccionando `menus/menus.xml` del proyecto se comprueba que `desarrollador-menuitem` ya está declarado con `groups="admins"` y `order="90"`, y que el submenú «Utilidades de PDF» ya cuelga de él con `groups="admins"`. El diseño solo añade `smoketest-menuitem` en `order="1"`. No se crea ningún menuitem raíz nuevo.

2. **Utilidades de PDF ya restringidas a admins.** El spec indica que las opciones de Utilidades de PDF deben quedar restringidas solo al Administrador al moverse bajo «Desarrollador». Inspeccionando `menus.xml`, ya tienen `groups="admins"`. No se requiere ninguna modificación de esos menuitems.

3. **Sin `required="true"` en el dominio para `texto`.** Para garantizar que el mensaje de error sea exactamente «El texto es obligatorio» (spec ESC-005), la validación vive en `validateInsert`/`validateUpdate` del servicio en lugar de como `@NotBlank` generada por el dominio XML (que produciría un mensaje genérico de Bean Validation antes de que el servicio pudiera producir el mensaje del spec).

4. **Sin filtrado multi-centro.** El spec indica explícitamente que SmokeTest no se asocia a ningún centro. No se implementa ningún filtro `centroActivo` ni se asigna el centro al crear.

5. **Sin controlador.** No hay lógica de negocio adicional en botones que requiera `@CallMethod`. El CRUD completo se gestiona con las acciones predefinidas de Axelor (`save`, `delete`, `back`).

6. **`SmokeTest.all` NO existe en auth.xml global (subsistema nuevo).** SmokeTest se crea de cero en este diseño. La **definición** del permiso `SmokeTest.all` vive ÚNICAMENTE en `subsystem/smoketest/data-init/input/auth-smoketest.xml` (k-datainit §2 CRITICAL: la definición de permisos de un subsistema NO va en el auth.xml global). El implementador DEBE añadir en `src/main/resources/data-init/input/auth.xml` ÚNICAMENTE la **asignación** (`<permission name="SmokeTest.all"/>` dentro del bloque `<group code="admins">`). Omitir la asignación dejaría el subsistema inaccesible aunque el permiso esté definido en `auth-smoketest.xml`.
