---
type: design
template: system
---

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
| `subsystem/security/domains/AceProfileCentro.xml` | Modificar (sin delta) | k-sistemas (modelos.md) | La spec no cambia el modelo: el fichero del diseño es **idéntico** al real (copiarlo es un no-op). Se incluye porque el contrato exige un `domains/<Entidad>.xml` por entidad. |
| `subsystem/security/db/repo/AceProfileCentroRepository.java` | Modificar | k-sistemas (modelos.md, servicios.md) | + métodos `existeOtraIgual` (RES-007) y `findCentrosSupervisados` (dueño de «centros supervisados», D1). |
| `subsystem/security/service/AceProfileCentroService.java` | Crear | k-sistemas (servicios.md) | Interfaz del `ModelService` de `AceProfileCentro`. |
| `subsystem/security/service/impl/AceProfileCentroServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones, k-secure-coding | Validaciones de la fila, whitelists por acción y los centros que supervisa el usuario. |
| `subsystem/security/controller/AceProfileCentroController.java` | Crear | k-sistemas (controladores.md) | Método de tipo 3 que da a la vista los ids de los centros supervisados. |
| `subsystem/security/views/Centro-AceProfileCentro.xml` | Crear | k-vistas (actions.md, grids.md, forms.md) | Pantalla del supervisor («Mi centro → Perfiles de trámites»). |
| `subsystem/security/views/Main-AceProfileCentro.xml` | Crear | k-vistas (actions.md, grids.md, forms.md) | Pantalla del administrador («Administración → Perfiles de trámites por centro»). |
| `secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | + 2 `<menuitem>` (porción en `design/menus.xml`). |
| `subsystem/security/data-init/input/auth-security.xml` | Modificar | k-datainit, k-secure-coding | + permisos `AceProfileCentro.supervisor` y `Tramite.supervisor` + su enlace al grupo `users`. |
| `subsystem/security/data-init/input-config.xml` | Modificar | k-datainit | + `<bind node="group">` en el `<input>` de `auth-security.xml`. |
| `subsystem/security/CLAUDE.md` | Modificar | — | Documentar quién rellena `AceProfileCentro`, el enlace de permisos al grupo desde el propio data-init y la excepción `Tramite.supervisor`. |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los fragmentos de `auth-security.xml`, `input-config.xml` y `CLAUDE.md` del Paso 7 se aplican **añadiendo** a lo que ya hay (no se borra nada).

## Pasos

### Paso 1 — Dominio `AceProfileCentro` (sin cambios)

Fichero: `design/domains/AceProfileCentro.xml`.

- **Preexistente (se conserva):** la entidad completa (`perfil`, `tipoUsuario`, `cargo`, `usuario`, `centro`, `tramite`, `repository="abstract"`), incluidos los `required="true"` de `perfil`, `centro` y `tramite`, que el formulario hereda (U-…-004 / U-…-003 de cada pantalla).
- **Delta:** ninguno. La unicidad (RES-007) **no** se declara con `<unique-constraint>` (decisiones D4: PostgreSQL trata los `NULL` como distintos y el mensaje no sería el de la spec).

**Verificar:** `diff design/domains/AceProfileCentro.xml src/main/java/com/educaflow/subsystem/security/domains/AceProfileCentro.xml` no muestra diferencias.

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

### Paso 3 — Repositorio `AceProfileCentroRepository`

Clase existente `com.educaflow.subsystem.security.db.repo.AceProfileCentroRepository extends AbstractAceProfileCentroRepository`. **Solo se añaden** los dos métodos; el resto de la clase (`findPerfiles`) se conserva.

```java
public List<Centro> findCentrosSupervisados(User usuario);
//   Dueño en el subsistema de «los centros que supervisa el usuario» (decisiones D1): todos los centros en
//   los que tiene el tipo de usuario SUPERVISOR, nunca User.centroActivo. JPQL con parámetro nombrado:
//     SELECT DISTINCT cu.centro FROM CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut
//     JOIN cut.tipoUsuario tu WHERE cu.usuario = :usuario AND tu.codigo = 'SUPERVISOR'
//   Copias declaradas del criterio (D1), que cambian con él: las condiciones JPQL de los permisos
//   AceProfileCentro.supervisor y Tramite.supervisor (Paso 7.1). Sin resultados → lista vacía.

public boolean existeOtraIgual(AceProfileCentro fila);
//   true si existe en BD otra fila (id distinto del de `fila`; en un alta `fila.getId()` es null y no se
//   excluye ninguna) con el mismo centro, trámite y perfil y los mismos tres destinatarios, comparando
//   cada destinatario de forma segura frente a nulos: un destinatario vacío solo coincide con otro vacío
//   (`tipoUsuario` null solo iguala a filas con `tipoUsuario` null, etc.).
//   Consulta con parámetros nombrados enlazados (nunca concatenando valores); la forma concreta de la
//   comparación nula la decide el implementador (p. ej. ramas IS NULL por destinatario, o filtrar en
//   memoria los candidatos de mismo centro/trámite/perfil).
```

**Verificar:** `./gradlew compileJava`; `findPerfiles` sigue intacto.

### Paso 4 — Controlador `AceProfileCentroController`

Clase nueva `com.educaflow.subsystem.security.controller.AceProfileCentroController`, con `@Inject ModelServiceFactory modelServiceFactory` como única dependencia. No expone `insert`/`update`/`remove` ni validaciones de save/delete (las dan `save`/`delete` y las acciones globales `remote-validation*`).

```java
@CallMethod
public List<Long> idsCentrosSupervisados();
//   Tipo 3 (sin ActionRequest/ActionResponse). La invoca el <context name="idsCentrosSupervisados"
//   expr="call:…AceProfileCentroController:idsCentrosSupervisados()"/> de
//   subsysSecurity.Centro@AceProfileCentro-action, igual que BandejaController.idsPendientesDeMi().
//   Resuelve AceProfileCentroService con modelServiceFactory.resolve(AceProfileCentro.class), llama a
//   getCentrosSupervisados() y devuelve los ids de esos centros; si la lista está vacía devuelve el
//   centinela NINGUNO = List.of(-1L), igual que BandejaController.NINGUNO, para que el `domain`
//   `self.id IN (:idsCentrosSupervisados)` nunca quede en `IN ()`. Sin más lógica: solo traduce
//   entidades a ids para el contexto de la vista.
```

**Verificar:** `./gradlew compileJava`; el nombre del método coincide con el de la expresión `call:` de `Centro-AceProfileCentro.xml`.

### Paso 5 — Vistas

#### `views/Centro-AceProfileCentro.xml` (pantalla del supervisor) — Crear

- `<action-view>` `subsysSecurity.Centro@AceProfileCentro-action` («Perfiles de trámites»): grid + form, `show-toolbar-form=false`, `forceEdit=true`. **Sin `<domain>`**: las filas visibles las filtra el permiso `AceProfileCentro.supervisor`. `<context name="idsCentrosSupervisados">` con `call:` al controlador (U-…-mi-centro-001 y -005, decisiones D1).
- `<grid>` `subsysSecurity.Centro@AceProfileCentro-grid`: columnas centro, trámite, perfil, tipo de usuario, cargo, usuario; `orderBy="centro.name,tramite.name,perfil"`; búsqueda por columnas (`allowSearchFields="true"`); botón «Nuevo» (`canNew="true"`, `newButtonTitle="Nuevo"`); clic en fila abre el form en edición.
- `<form>` `subsysSecurity.Centro@AceProfileCentro-form` con `onNew` → `…-onNew-action`:
  - panel `AceProfileCentro` («Perfil»): `centro` con `readonlyIf="id != null"` (U-002), `domain="self.id IN (:idsCentrosSupervisados)"` (U-001) y vistas `subsysCommon.Ref@Centro-*`; `tramite`; `perfil`.
  - panel `destinatario` («A quién se da»): `tipoUsuario`; `cargo`; `usuario` con `domain` de los usuarios con `CentroUsuario` en el `centro` de la fila (U-003) y `form-view` `subsysCommon.Ref@Usuario-form`; sin `grid-view`, porque `Ref@Usuario-grid` es `groups="admins"` y el supervisor no la resolvería.
  - `buttons-panel` estándar Borrar / Cancelar / Guardar.
- Acciones:
  - `…-btnDelete-action`: `remote-validationDelete-action` → `delete`.
  - `…-btnCancel-action`: `back`.
  - `…-btnSave-action`: `remote-validationSave-action` → `save` → `force-back`.
  - `…-onNew-action`: ejecuta `…-set-centro-unicoSupervisado-action` **solo** con `if="idsCentrosSupervisados.size() == 1"` (U-005; la condición en la rama).
  - `…-set-centro-unicoSupervisado-action` (`<action-record>`): `centro` = el `Centro` cuyo id es el único de `idsCentrosSupervisados`.
- Obligatorios marcados (U-004): heredados del `required="true"` del modelo en `centro`, `tramite` y `perfil`.

ASCII Layout (idéntico en las dos pantallas; sin `showIf` salvo `btnDelete`, que va al principio de su fila sin `colOffset` y no desplaza a los demás):

```
Panel «Perfil» (AceProfileCentro)
cccttttttppp   ← centro(3) + tramite(6) + perfil(3)                 [qué se da y dónde → misma fila]

Panel «A quién se da» (destinatario)
aaabbbuuuuuu   ← tipoUsuario(3) + cargo(3) + usuario(6)             [los tres destinatarios excluyentes → misma fila, primer borde alineado con la fila de arriba]

buttons-panel
Alta:     ........ccgg ← Borrar oculto (colSpan 2, primera posición, sin offset) + offset(6) + Cancelar(2) + Guardar(2)
Edición:  dd......ccgg ← Borrar(2) + offset(6) + Cancelar(2) + Guardar(2)
```

(Reparto proporcional al contenido: el trámite y el usuario muestran nombres largos —«Justificación de falta del profesorado», «Profesor1 CIPFP Mislata»— y llevan 6 columnas; el perfil es un enum de una palabra, el centro un nombre corto y el tipo de usuario y el cargo códigos cortos, 3 columnas cada uno.)

#### `views/Main-AceProfileCentro.xml` (pantalla del administrador) — Crear

Igual estructura que la del supervisor, con estas diferencias:

- `<action-view>` `subsysSecurity.Main@AceProfileCentro-action` («Perfiles de trámites por centro») sin `<domain>` ni `<context>`: el administrador ve y elige cualquier centro.
- `centro` sin `domain` y **sin** `onNew` en el form (no hay prellenado en esta pantalla).
- `centro` con `readonlyIf="id != null"` (U-perfiles-tramites-todos-centros-001); `usuario` con el mismo `domain` por centro (U-…-todos-centros-002) y, además del `form-view`, `grid-view` `subsysCommon.Ref@Usuario-grid` (el administrador sí la resuelve); obligatorios heredados del modelo (U-…-todos-centros-003).
- Mismo ASCII Layout que la pantalla del supervisor.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh .sdd/drafts/<iniciativa>/design` → `VALIDACION-XML: OK`; `./gradlew test --tests 'com.educaflow.views.*'`.

### Paso 6 — Menús

Porción en `design/menus.xml`, a fusionar en `secretariavirtual/menus/menus.xml` como últimos hijos de sus padres existentes:

- `miCentro-perfilesTramites-menuitem` bajo `miCentro-menuitem` («Mi centro», ya con `if="__config__.menu.isSupervisor()"` y `groups="users"`): «Perfiles de trámites» → `subsysSecurity.Centro@AceProfileCentro-action`, `groups="users"`, `order="3"`.
- `administracion-perfilesTramitesPorCentro-menuitem` bajo `administracion-menuitem`: «Perfiles de trámites por centro» → `subsysSecurity.Main@AceProfileCentro-action`, `groups="admins"`, `order="4"`.

Resultado: el director (no supervisor, grupo `users`) no ve ninguna (ESC-020); el supervisor ve la suya y no la de administración (ESC-021). Un menú oculto no autoriza nada: la autorización son los permisos del Paso 7 y V-AceProfileCentro-008.

**Verificar:** `./gradlew test --tests 'com.educaflow.views.*'` (VAR-10.x: una línea por menú, atributos en orden, `order` único por submenú).

### Paso 7 — Seguridad (data-init del subsistema y documentación)

Todo dentro de `subsystem/security` (guía de diseño). Reglas de acceso en lenguaje natural:

- **Administrador** (grupo `admins`): ve, crea, modifica y borra filas de cualquier centro. No necesita permiso: Axelor no aplica permisos a `admins`, y el servicio no le aplica V-008.
- **Supervisor** (grupo `users` con el tipo de usuario `SUPERVISOR` en algún centro): ve, crea, modifica y borra solo filas de los centros que supervisa, y lee todos los trámites para poder elegirlos.
- **Cualquier otro usuario** del grupo `users`: tiene el permiso por pertenecer a `users`, pero la condición no le deja ver, modificar ni borrar ninguna fila, y V-008 le rechaza el alta.

**7.1 — `data-init/input/auth-security.xml`** (se añade al final de `<auth>`, conservando todo lo existente):

- Permiso `AceProfileCentro.supervisor`, objeto `com.educaflow.subsystem.security.db.AceProfileCentro`, `create/read/write/remove = true`, `export = false`, en literal:
  `condition="self.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')"` con `conditionParams="__user__"` (misma forma que `Correo.propio-centro-supervisor` de `auth-correos.xml`, sin `ADMINISTRATIVO`; nombres de entidad y campos comprobados contra ese permiso real).
  Comentario XML obligatorio: «repite en JPQL la consulta de `AceProfileCentroRepository.findCentrosSupervisados`; si cambia una, cambia la otra». En Axelor la condición se aplica al leer, modificar y borrar (filtra el listado y bloquea filas ajenas); en el alta no se evalúa sobre la fila nueva, por eso V-008 es la defensa del alta.
- Permiso `Tramite.supervisor`, objeto `com.educaflow.subsystem.expedientes.db.Tramite`, solo `read = true`, en literal:
  `condition="EXISTS (SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')"` con `conditionParams="__user__"` (la subconsulta no referencia `self`: da lectura de todos los trámites a quien es `SUPERVISOR` en algún centro). Si al arrancar Axelor no admitiera `EXISTS` sin correlación, la forma equivalente es `? IN (SELECT cu.usuario FROM … mismo JOIN … WHERE tu.codigo = 'SUPERVISOR')` con el mismo `conditionParams`.
  Comentario XML obligatorio: «repite en JPQL la consulta de `AceProfileCentroRepository.findCentrosSupervisados`; si cambia una, cambia la otra» (decisiones D1), y que existe solo para que el selector de trámite de «Mi centro → Perfiles de trámites» no salga vacío (decisiones D3); no da ningún perfil ni cambia lo que ofrece «Nuevo trámite».
- `<group code="users">` con `<permission name="AceProfileCentro.supervisor"/>` y `<permission name="Tramite.supervisor"/>` (decisiones D2).

**7.2 — `data-init/input-config.xml`**: dentro del `<input file="auth-security.xml" root="auth">` existente, tras el `<bind node="permission" …>`, añadir el mismo bind de grupo que usa el data-init global: `<bind node="group" type="com.axelor.auth.db.Group" search="self.code = :code" create="false" update="true">` con `<bind node="@code" to="code"/>` y `<bind node="permission" to="permissions" search="self.name = :name"><bind node="@name" to="name"/></bind>`. Suma a los permisos del grupo (`XMLBinder` hace `addAll`), no los reemplaza; el grupo `users` existe antes de cualquier data-init (`ModuleManager.createDefault`). La `priority="8"` no cambia.

**7.3 — `subsystem/security/CLAUDE.md`** (añadidos, sin quitar nada):

- En la tabla «Las tablas», la fila de `AceProfileCentro` dice que la rellenan las pantallas «Mi centro → Perfiles de trámites» (supervisor, sus centros) y «Administración → Perfiles de trámites por centro» (administrador), a través de `AceProfileCentroService`, que valida el destinatario único.
- En «Permisos de Axelor»: los permisos propios de este subsistema se enlazan al grupo `users` desde su propio `auth-security.xml` (bind de `group` en su `input-config.xml`), no desde el `auth.xml` global. Y la excepción `Tramite.supervisor`: el supervisor **lee** todos los trámites aunque no tenga perfil sobre ellos, solo para poder asignarlos; por eso ahí lo que ve no coincide con lo que puede hacer.

**Verificar:** arrancar con `./run.sh` y comprobar en BD que el grupo `users` tiene los dos permisos (`psql` según `agent_docs/deploy.md`: `auth_group_permissions` del grupo `users` incluye `AceProfileCentro.supervisor` y `Tramite.supervisor`) y que tras un segundo arranque siguen ahí.

### Paso 8 — Verificación final

- Compilar y pasar los tests (JUnit, ArchUnit y vistas) sin arrancar: `./gradlew clean build --info`.
- Arrancar: `./run.sh` (compila, pasa los tests y arranca en el 8080); la app debe arrancar sin errores de data-init.

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

### Reglas de UI (U)

| ID | Origen spec | Ubicación | Qué hace |
|----|-------------|-----------|----------|
| U-perfiles-tramites-mi-centro-001 | RUI-perfiles-tramites-mi-centro-formulario-001 | `views/Centro-AceProfileCentro.xml`: `domain` del `<field name="centro">` + `<context name="idsCentrosSupervisados">` de `subsysSecurity.Centro@AceProfileCentro-action` | El selector de centro solo ofrece los centros que supervisa. |
| U-perfiles-tramites-mi-centro-002 | RUI-perfiles-tramites-mi-centro-formulario-002 | `views/Centro-AceProfileCentro.xml`: `readonlyIf="id != null"` del `<field name="centro">` | Centro de solo lectura en una fila existente. |
| U-perfiles-tramites-mi-centro-003 | RUI-perfiles-tramites-mi-centro-formulario-003 | `views/Centro-AceProfileCentro.xml`: `domain` del `<field name="usuario">` | El selector de usuario solo ofrece usuarios del centro de la fila. |
| U-perfiles-tramites-mi-centro-004 | RUI-perfiles-tramites-mi-centro-formulario-004 | `views/Centro-AceProfileCentro.xml`: campos `centro`, `tramite`, `perfil` (obligatoriedad heredada del `required="true"` de `domains/AceProfileCentro.xml`) | Centro, trámite y perfil marcados como obligatorios. |
| U-perfiles-tramites-mi-centro-005 | RUI-perfiles-tramites-mi-centro-formulario-005 | `views/Centro-AceProfileCentro.xml`: `subsysSecurity.Centro@AceProfileCentro-onNew-action` (rama `if`) → `subsysSecurity.Centro@AceProfileCentro-set-centro-unicoSupervisado-action` | Si supervisa un único centro, el alta nace con ese centro. |
| U-perfiles-tramites-todos-centros-001 | RUI-perfiles-tramites-todos-centros-formulario-001 | `views/Main-AceProfileCentro.xml`: `readonlyIf="id != null"` del `<field name="centro">` | Centro de solo lectura en una fila existente. |
| U-perfiles-tramites-todos-centros-002 | RUI-perfiles-tramites-todos-centros-formulario-002 | `views/Main-AceProfileCentro.xml`: `domain` del `<field name="usuario">` | El selector de usuario solo ofrece usuarios del centro de la fila. |
| U-perfiles-tramites-todos-centros-003 | RUI-perfiles-tramites-todos-centros-formulario-003 | `views/Main-AceProfileCentro.xml`: campos `centro`, `tramite`, `perfil` (obligatoriedad heredada del modelo) | Centro, trámite y perfil marcados como obligatorios. |

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).
- **Tests E2E**: `test-e2e-desc.md` (T-001…T-022, uno por escenario de la spec; la carpeta `src/test/e2e/subsystem/security/` no tiene tests previos).

## Reglas del spec descartadas

Ninguna: todas las `RES-`, `VAL-` y `RUI-` están ubicadas en la matriz.

## Notas y supuestos

- **Nombres de las variantes de vista.** Se sigue a `subsystem/correos`, el hermano con la misma pareja de pantallas: `Centro-…` para la del supervisor («de mi centro») y `Main-…` para la del administrador («todos»).
- **El listado del supervisor no lleva `<domain>`.** Lo filtra el permiso `AceProfileCentro.supervisor`; repetir la condición en la vista sería una tercera copia (decisiones D1). El administrador no tiene grupo `users` ni ve el menú del supervisor.
- **El contexto del `<action-view>` llega al `domain` del campo.** axelor-front (`usePrepareContext`) mezcla el contexto del `action-view` en el contexto del formulario, que es el `_domainContext` que usan los selectores; es el mismo mecanismo por el que `sysVentanilla` usa `_centrosIds`.
- **Supervisor sin ningún centro.** Normalmente no ve el menú; si abre la acción por URL, el controlador devuelve el centinela `List.of(-1L)` (el mismo `NINGUNO` de `BandejaController`): el selector de centro sale vacío y V-AceProfileCentro-008 le rechaza cualquier alta. El prellenado (`idsCentrosSupervisados.size() == 1`) no cambia: con el centinela el único id es `-1`, `find(-1)` da `null` y es lo mismo que no prellenar.
- **Las guías de diseño se respetan sin excepción:** solo se tocan `subsystem/security` y `menus.xml` (el enlace de permisos al grupo va en el data-init de `security`, D2); no se tocan `auth-expedientes.xml`, `PerfilesUsuarioService` ni `AceProfileCentroRepository.findPerfiles`; el «centro del supervisor» son todos sus centros `SUPERVISOR`, nunca `User.centroActivo`.
- **`Tramite.supervisor`** amplía lo que el supervisor puede **leer** de `Tramite` (decisiones D3). No cambia qué trámites le ofrece «Nuevo trámite» (la ventanilla filtra por perfiles), ni qué expedientes ve, ni el cálculo de perfiles: lo que el «Fuera de alcance» protege sigue intacto.
- **Cambio de centro con usuario ya elegido (ESC-015).** No se vacía el usuario al cambiar el centro (la spec no lo pide): el error lo da V-006 al guardar.
- **Selectores sin vista de referencia.** `Tramite`, `TipoUsuario` y `Cargo` no tienen `Ref@…` en el proyecto; se usan sus selectores por defecto en vez de crear vistas que la spec no pide.
- **Independencia de los tests E2E.** Varios escenarios crean filas iguales (p. ej. ESC-001 y ESC-014 en «CIPFP Mislata»); cada test parte del «Estado inicial de la base de datos» (tabla de perfiles vacía), como exige `tests-e2e.md`.
