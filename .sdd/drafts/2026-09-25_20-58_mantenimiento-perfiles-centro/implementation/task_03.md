---
type: implementation-task
template: system
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

# Diseño: Mantenimiento de perfiles de trámites por centro

**Objetivo:** que el supervisor (en los centros que supervisa) y el administrador (en cualquier centro) puedan ver, crear, modificar y borrar las filas de `AceProfileCentro`, con las reglas que garantizan que cada fila dice sin ambigüedad a quién se da el perfil.
**Capa:** subsystem/security
**Especificación de origen:** .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-validaciones, k-code-quality, k-secure-coding, k-vistas, k-datainit

Las decisiones difíciles y sus alternativas están en `decisiones.md` (D1–D6); este documento es coherente con ellas.

## Ficheros a crear o modificar

Rutas relativas a `src/main/java/com/educaflow/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/security/service/AceProfileCentroService.java` | Crear | k-sistemas (servicios.md) | Interfaz del `ModelService` de `AceProfileCentro`. |
| `subsystem/security/service/impl/AceProfileCentroServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones, k-secure-coding | Validaciones de la fila, whitelists por acción y los centros que supervisa el usuario. |

Las decisiones D1–D6 citadas están en `design/decisiones.md`. Los mensajes exactos de cada V-AceProfileCentro-00N (texto de la spec) están listados en la sección «Convenciones» de `design/test-unit-desc.md`: los tests unitarios los comparan literalmente.

## Pasos

### Paso 2 — Servicio `AceProfileCentroService` / `AceProfileCentroServiceImpl`

Clasificación de campos (de las líneas `Input AllowProperties` de `entity-AceProfileCentro.md`; la entidad no tiene `CC-`):

| Campo | Crear | Modificar | Borrar |
|---|---|---|---|
| `centro` | cliente | inmutable (fuera de la whitelist) | — |
| `tramite`, `perfil`, `tipoUsuario`, `cargo`, `usuario` | cliente | cliente | — |

No hay campos `servidor`, así que no hay reglas `R-` ni se sobrescriben `insert`/`update`/`remove`: los hereda `DefaultModelService`, que ya ejecuta `validateXxx(...).ifPresent(throwIfInvalid)` + `repository.save/remove`.

**Interfaz** — `com.educaflow.subsystem.security.service.AceProfileCentroService extends ModelService<AceProfileCentro>`:

```java
// Acción propia: los centros en los que el usuario autenticado tiene el tipo de usuario SUPERVISOR.
// La consume la pantalla del supervisor (a través de AceProfileCentroController.idsCentrosSupervisados)
// para restringir el selector de centro y prellenarlo. No recibe entidad del request → sin allowProperties.
List<Centro> getCentrosSupervisados();

// Validador de la acción anterior (C23).
Optional<BusinessMessages> validateGetCentrosSupervisados();
```

**Implementación** — `com.educaflow.subsystem.security.service.impl.AceProfileCentroServiceImpl extends DefaultModelService<AceProfileCentro> implements AceProfileCentroService`. Bloques en el orden de `k-sistemas/servicios.md`.

```java
// Constructor obligatorio (ModelServiceFactory, por reflexión):
public AceProfileCentroServiceImpl(Class<AceProfileCentro> model, Repository<AceProfileCentro> repository);
//   super(model, repository). Sin dependencias @Inject: el repositorio propio se obtiene con el cast
//   ((AceProfileCentroRepository) repository).

// ───── (1) Acciones ─────

@Override
public List<Centro> getCentrosSupervisados();
//   Primera línea: validateGetCentrosSupervisados().ifPresent(BusinessMessages::throwIfInvalid).
//   Devuelve tal cual ((AceProfileCentroRepository) repository).findCentrosSupervisados(SecurityUtil.getUser()),
//   vacía si el usuario no supervisa ningún centro (la lista vacía la resuelve el controlador con el
//   centinela, Paso 4). Sin efectos colaterales.

// ───── (2) Métodos de Validación ─────

@Override
public Optional<BusinessMessages> validateInsert(AceProfileCentro fila);
//   Devuelve validarFila(fila).
//   Cubre V-AceProfileCentro-001…008 (Origen spec: RES-AceProfileCentro-001…007, VAL-AceProfileCentro-001).

@Override
public Optional<BusinessMessages> validateUpdate(AceProfileCentro fila, AceProfileCentro original);
//   Devuelve validarFila(fila) (`original` no se usa). La fila validada ya trae el centro de BD: `centro`
//   está fuera de allowPropertiesUpdate, así que el cliente no puede cambiarlo; la whitelist es el único
//   dueño de esa inmutabilidad (decisiones D6). La unicidad excluye la propia fila por su id.
//   Cubre V-AceProfileCentro-001…008 (Origen spec: RES-AceProfileCentro-001…007, VAL-AceProfileCentro-002).

@Override
public Optional<BusinessMessages> validateRemove(AceProfileCentro fila);
//   Solo V-AceProfileCentro-008 (Origen spec: VAL-AceProfileCentro-003): validarCentroGestionable(fila.getCentro()).
//   `fila` llega con los valores de BD porque allowPropertiesRemove no deja pasar ningún campo del cliente
//   (decisiones D6); por eso el centro comprobado es el real de la fila.

@Override
public Optional<BusinessMessages> validateGetCentrosSupervisados();
//   Devuelve Optional.empty(). Comentario en el código: «la acción no recibe datos del usuario; existe
//   por C23». Sin reglas: un usuario sin centros supervisados recibe una lista vacía, no un error.

private Optional<BusinessMessages> validarFila(AceProfileCentro fila);
//   Único método que valida una fila completa; lo comparten alta y modificación. Dos fases explícitas
//   (decisiones D5):
//   Fase 1 — forma de la fila, acumulando mensajes:
//     1. validarObligatorios(fila, messages)                                        → V-001…003
//     2. validarDestinatario(fila, messages)                                        → V-004 / V-005
//     Si hay algún mensaje, se devuelven solo esos: el resto de reglas necesitan centro, trámite, perfil
//     y exactamente un destinatario.
//   Fase 2 — sobre una fila completa y bien formada, acumulando mensajes:
//     1. validarCentroGestionable(fila.getCentro(), messages)                      → V-008
//     2. if (fila.getUsuario() != null) validarUsuarioDelCentro(fila, messages)     → V-006 (la condición
//        «si hay usuario» es la de la propia RES-006 y va en la rama, no dentro del helper)
//     3. validarAsignacionNoRepetida(fila, messages)                                → V-007 (solo tiene
//        sentido con exactamente un destinatario, que la fase 1 garantiza)
//   Retorno canónico: messages.isValid() ? Optional.empty() : Optional.of(messages).

private void validarObligatorios(AceProfileCentro fila, BusinessMessages messages);
//   - V-AceProfileCentro-001 (Origen spec: RES-AceProfileCentro-001): centro no nulo.
//     Mensaje debe transmitir: que el centro es obligatorio (texto de la spec).
//   - V-AceProfileCentro-002 (Origen spec: RES-AceProfileCentro-002): trámite no nulo.
//     Mensaje: que el trámite es obligatorio.
//   - V-AceProfileCentro-003 (Origen spec: RES-AceProfileCentro-003): perfil no nulo.
//     Mensaje: que el perfil es obligatorio.
//   (El `required="true"` del modelo ya lo impone en BD; se replica aquí para dar el mensaje de la spec
//   en lugar del genérico de JPA — k-validaciones/restricciones.md §1.)

private void validarCentroGestionable(Centro centro, BusinessMessages messages);
//   V-AceProfileCentro-008 (Origen spec: VAL-AceProfileCentro-001, VAL-AceProfileCentro-002, VAL-AceProfileCentro-003):
//   el usuario autenticado (SecurityUtil.getUser()) puede gestionar el centro si
//   SecurityUtil.isAdmin(user) O ((AceProfileCentroRepository) repository).findCentrosSupervisados(user)
//   contiene `centro` (comparando por id).
//   Ramas complementarias por construcción: el administrador gestiona cualquier centro; cualquier otro
//   usuario, solo los que supervisa (un usuario que no supervisa ninguno siempre es rechazado).
//   Mensaje debe transmitir: que solo se gestionan perfiles de los centros de los que se es supervisor.

private void validarDestinatario(AceProfileCentro fila, BusinessMessages messages);
//   Un único concepto, «exactamente un destinatario»: cuenta cuántos de tipoUsuario, cargo y usuario
//   vienen informados.
//   - V-AceProfileCentro-004 (Origen spec: RES-AceProfileCentro-004): cero → mensaje que pida indicar a
//     quién se da el perfil (tipo de usuario, cargo o usuario).
//   - V-AceProfileCentro-005 (Origen spec: RES-AceProfileCentro-005): más de uno → mensaje que pida
//     indicar solo uno.
//   Es la validación que exige el CLAUDE.md de security para las tablas de tiempo de ejecución, y la que
//   mantiene válida la semántica de AceProfileCentroRepository.findPerfiles (un destinatario por fila).

private void validarUsuarioDelCentro(AceProfileCentro fila, BusinessMessages messages);
//   V-AceProfileCentro-006 (Origen spec: RES-AceProfileCentro-006): da por hecho que hay usuario y centro
//   (lo garantizan la rama de validarFila y la fase 1). El usuario pertenece al centro si
//   fila.getUsuario().getCentroUsuario(fila.getCentro()) != null (helper ya existente del User extendido).
//   Mensaje debe transmitir: que el usuario no pertenece al centro de la fila.

private void validarAsignacionNoRepetida(AceProfileCentro fila, BusinessMessages messages);
//   V-AceProfileCentro-007 (Origen spec: RES-AceProfileCentro-007):
//   ((AceProfileCentroRepository) repository).existeOtraIgual(fila). Si existe, rechaza.
//   Mensaje debe transmitir: que ya existe esa asignación de perfil.

// ───── (3) AllowProperties ─────

@Override
public AllowProperties allowPropertiesInsert();
//   Whitelist createAllowProperties con centro, tramite, perfil, tipoUsuario, cargo, usuario
//   (Input AllowProperties de la acción Crear). Ver «Frontera de confianza».

@Override
public AllowProperties allowPropertiesUpdate();
//   Whitelist createAllowProperties con tramite, perfil, tipoUsuario, cargo, usuario: `centro` fuera
//   porque es inmutable tras el alta (Input AllowProperties de la acción Modificar; k-secure-coding §3.2).

@Override
public AllowProperties allowPropertiesRemove();
//   createDenyAllProperties(): borrar no necesita ningún campo del cliente; así la fila que llega a
//   validateRemove (vía remote-validationDelete-action) es la de BD y V-008 comprueba su centro real.

// ───── (4) Action Rules ─────
//   (sin bloque: no hay reglas R ni campos servidor)

// ───── (5) Otras funciones ─────
//   (sin bloque: getCentrosSupervisados y validarCentroGestionable llaman directamente al finder
//   AceProfileCentroRepository.findCentrosSupervisados, Paso 3, decisiones D1)
```

**Verificar:** `./gradlew compileJava`; `grep -n "super\.\(insert\|update\|remove\)" .../AceProfileCentroServiceImpl.java` sin resultados; los tres headers de bloque con líneas de igual longitud (`awk` de `servicios.md`).

## Frontera de confianza — AllowProperties por acción

## Frontera de confianza — AllowProperties por acción

La única acción propia invocada desde un `@CallMethod` es `getCentrosSupervisados()`, que **no recibe entidad** (sin `allowProperties`, `k-sistemas/servicios.md`). Las acciones que sí reciben la entidad del cliente son `insert`/`update`/`remove`, a las que se llega por el endpoint REST automático (acciones `save`/`delete` de la vista) y por las globales `remote-validation*`; sus whitelists son las que defienden la entidad.

### `AceProfileCentroServiceImpl.getCentrosSupervisados` (invocado desde `AceProfileCentroController.idsCentrosSupervisados`)

Entidad: ninguna. **Forma elegida**: no aplica (no hay mapa del cliente que filtrar: la acción trabaja sobre `SecurityUtil.getUser()`).
**Origen spec:** RUI-perfiles-tramites-mi-centro-formulario-001 y -005 (pantalla del supervisor).

### `AceProfileCentroServiceImpl.insert` (endpoint REST `save` y `remote-validationSave-action`, alta)

Entidad: `AceProfileCentro`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-AceProfileCentro.md`.

| Campo         | Origen  | En whitelist | Justificación / Ubicación de la asignación |
|---------------|---------|--------------|--------------------------------------------|
| `centro`      | cliente | sí           | Input del usuario; lo autoriza V-008 en `validateInsert` (no basta el `domain` del selector). |
| `tramite`     | cliente | sí           | Input del usuario. |
| `perfil`      | cliente | sí           | Input del usuario. |
| `tipoUsuario` | cliente | sí           | Input del usuario (destinatario; V-004/005). |
| `cargo`       | cliente | sí           | Input del usuario (destinatario; V-004/005). |
| `usuario`     | cliente | sí           | Input del usuario (destinatario; V-004/005/006). |

### `AceProfileCentroServiceImpl.update` (endpoint REST `save` y `remote-validationSave-action`, modificación)

Entidad: `AceProfileCentro`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-AceProfileCentro.md`.

| Campo         | Origen  | En whitelist | Justificación / Ubicación de la asignación |
|---------------|---------|--------------|--------------------------------------------|
| `centro`      | cliente (solo en alta) | **NO** | Inmutable tras el alta: la whitelist conserva el valor de BD, así que la fila que valida `validateUpdate` (V-008 sobre `fila.getCentro()`) ya trae el centro de BD. El `readonlyIf` de la vista es solo comodidad. |
| `tramite`     | cliente | sí           | Input del usuario. |
| `perfil`      | cliente | sí           | Input del usuario. |
| `tipoUsuario` | cliente | sí           | Input del usuario. |
| `cargo`       | cliente | sí           | Input del usuario. |
| `usuario`     | cliente | sí           | Input del usuario. |

### `AceProfileCentroServiceImpl.remove` (endpoint REST `delete` y `remote-validationDelete-action`)

Entidad: `AceProfileCentro`. **Forma elegida**: `createDenyAllProperties` (whitelist vacía).
**Origen spec:** la acción `Borrar` de `entity-AceProfileCentro.md` no tiene `Input AllowProperties`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| (todos) | — | **NO** | Borrar no necesita datos del cliente; la fila se valida con los valores de BD (V-008 sobre su centro real). |

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (V)

| ID | Origen spec | Ubicación | Qué comprueba |
|----|-------------|-----------|---------------|
| V-AceProfileCentro-001 | RES-AceProfileCentro-001 | `AceProfileCentroServiceImpl.validarObligatorios` (desde `validateInsert`/`validateUpdate` → `validarFila`, fase 1); `required` en `domains/AceProfileCentro.xml` | Centro indicado. |
| V-AceProfileCentro-002 | RES-AceProfileCentro-002 | ídem | Trámite indicado. |
| V-AceProfileCentro-003 | RES-AceProfileCentro-003 | ídem | Perfil indicado. |
| V-AceProfileCentro-004 | RES-AceProfileCentro-004 | `AceProfileCentroServiceImpl.validarDestinatario` (fase 1 de `validarFila`) | Al menos un destinatario. |
| V-AceProfileCentro-005 | RES-AceProfileCentro-005 | `AceProfileCentroServiceImpl.validarDestinatario` (fase 1 de `validarFila`) | Como mucho un destinatario. |
| V-AceProfileCentro-006 | RES-AceProfileCentro-006 | `AceProfileCentroServiceImpl.validarUsuarioDelCentro` (rama `usuario != null` de `validarFila`) | El usuario pertenece al centro. |
| V-AceProfileCentro-007 | RES-AceProfileCentro-007 | `AceProfileCentroServiceImpl.validarAsignacionNoRepetida` → `AceProfileCentroRepository.existeOtraIgual` | Asignación no repetida. |
| V-AceProfileCentro-008 | VAL-AceProfileCentro-001, VAL-AceProfileCentro-002, VAL-AceProfileCentro-003 | `AceProfileCentroServiceImpl.validarCentroGestionable`, desde `validateInsert` (centro elegido), `validateUpdate` y `validateRemove` (centro de BD: `fila.getCentro()`, que las whitelists no dejan cambiar) | El usuario gestiona ese centro (administrador o supervisor de él). |

### Reglas de negocio (R)

Ninguna: la spec no tiene `RN-` ni `CC-` y la entidad no tiene campos `servidor`.

## Notas y supuestos (aplicables)

- **Supervisor sin ningún centro.** Normalmente no ve el menú; si abre la acción por URL, el controlador devuelve el centinela `List.of(-1L)` (el mismo `NINGUNO` de `BandejaController`): el selector de centro sale vacío y V-AceProfileCentro-008 le rechaza cualquier alta. El prellenado (`idsCentrosSupervisados.size() == 1`) no cambia: con el centinela el único id es `-1`, `find(-1)` da `null` y es lo mismo que no prellenar.
- **Cambio de centro con usuario ya elegido (ESC-015).** No se vacía el usuario al cambiar el centro (la spec no lo pide): el error lo da V-006 al guardar.
