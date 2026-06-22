---
type: implementation-task
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

Implementa el servicio de `Grupo`: la interfaz `GrupoService` y su implementación `GrupoServiceImpl` (validaciones, reglas, acciones de cierre/reapertura y fronteras de confianza).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/service/GrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de Grupo |
| `system/gruposnotas/service/impl/GrupoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Validaciones, reglas y acciones de Grupo |

Todos los servicios extienden `ModelService`/`DefaultModelService` y los descubre `ModelServiceFactory` por convención (sin módulo Guice). Persisten siempre con `repository.save/remove`, **nunca** `super.*`.

### `com.educaflow.system.gruposnotas.service.GrupoService`

```java
public interface GrupoService extends ModelService<Grupo> {
    Grupo cerrar(Grupo grupo, Grupo grupoOriginal);
    Grupo reabrir(Grupo grupo, Grupo grupoOriginal);

    Optional<BusinessMessages> validateCerrar(Grupo grupo, Grupo grupoOriginal);
    Optional<BusinessMessages> validateReabrir(Grupo grupo, Grupo grupoOriginal);

    AllowProperties allowPropertiesCerrar();
    AllowProperties allowPropertiesReabrir();
}
```

### `com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl`

```java
public class GrupoServiceImpl extends DefaultModelService<Grupo> implements GrupoService {

    @Inject GrupoRepository grupoRepository;
    @Inject ModuloGrupoRepository moduloGrupoRepository;   // (Axelor-generated abstract base; ver Paso 3)

    @Inject
    public GrupoServiceImpl(Class<Grupo> model, Repository<Grupo> repository) { super(model, repository); }

    @Override
    public Grupo insert(Grupo grupo);
    //   1. validateInsert(grupo).ifPresent(throwIfInvalid)  → V-Grupo-001/002/003.
    //   2. fireActionRule_FijarCentroYCursoAcademicoSiSupervisor(grupo)  → R-Grupo-002 (Antes).
    //   3. fireActionRule_EstadoInicialAbierto(grupo)  → estado inicial servidor (Antes).
    //   4. grupo = repository.save(grupo).
    //   5. fireActionRule_CrearModulosGrupo(grupo)  → R-Grupo-001 (Después: crea hijos ModuloGrupo).

    @Override
    public Grupo update(Grupo grupo, Grupo original);
    //   1. validateUpdate(grupo, original).ifPresent(throwIfInvalid)  → V-Grupo-004/005.
    //   2. Restaurar inmutables desde original: curso, centro, cursoAcademico, estado, fechaCierre
    //      (no se recalculan aquí; el cliente no los puede cambiar — k-secure-coding §3).
    //   3. return repository.save(grupo).

    @Override
    public Optional<BusinessMessages> validateInsert(Grupo grupo);
    //   Aplica:
    //     - V-Grupo-001 (Origen spec: VAL-001) nombre obligatorio: comprueba nombre no vacío.
    //       Mensaje transmite: el nombre del grupo es obligatorio.
    //     - V-Grupo-002 (Origen spec: VAL-002) curso obligatorio: comprueba curso != null.
    //       Mensaje transmite: el curso es obligatorio.
    //     - V-Grupo-003 (Origen spec: VAL-003, RES-001) nombre único por centro+cursoAcademico:
    //       consulta grupoRepository.findByNombreCentroCursoAcademico; si existe otro, rechaza.
    //       Mensaje transmite: ya existe un grupo con ese nombre en este centro y curso académico.

    @Override
    public Optional<BusinessMessages> validateUpdate(Grupo grupo, Grupo original);
    //   Aplica:
    //     - V-Grupo-004 (Origen spec: VAL-004) grupo abierto: si original.estado == CERRADO,
    //       rechaza cualquier modificación. Mensaje transmite: no se puede modificar un grupo cerrado.
    //     - V-Grupo-005 (Origen spec: VAL-005, RES-001) nombre único (igual que V-Grupo-003 pero
    //       excluyendo el propio grupo por id). Mensaje transmite: ya existe un grupo con ese nombre…

    @Override
    public Optional<BusinessMessages> validateRemove(Grupo grupo);
    //   Aplica:
    //     - V-Grupo-006 (Origen spec: VAL-009) grupo abierto: si estado == CERRADO, rechaza.
    //       Mensaje transmite: no se puede borrar un grupo cerrado.

    @Override
    public Grupo cerrar(Grupo grupo, Grupo grupoOriginal);
    //   1. validateCerrar(grupo, grupoOriginal).ifPresent(throwIfInvalid)  → V-Grupo-009.
    //   2. fireActionRule_RegistrarCierre(grupo)  → R-Grupo-003 (Antes).
    //   3. return repository.save(grupo).

    @Override
    public Optional<BusinessMessages> validateCerrar(Grupo grupo, Grupo grupoOriginal);
    //   Aplica:
    //     - V-Grupo-009 (Origen spec: VAL-006) estado ABIERTO: si grupoOriginal.estado != ABIERTO,
    //       rechaza. Mensaje transmite: el grupo ya está cerrado.

    @Override
    public Grupo reabrir(Grupo grupo, Grupo grupoOriginal);
    //   1. validateReabrir(grupo, grupoOriginal).ifPresent(throwIfInvalid)  → V-Grupo-007/008.
    //   2. fireActionRule_RegistrarReapertura(grupo)  → R-Grupo-004 (Antes).
    //   3. return repository.save(grupo).

    @Override
    public Optional<BusinessMessages> validateReabrir(Grupo grupo, Grupo grupoOriginal);
    //   Aplica:
    //     - V-Grupo-007 (Origen spec: VAL-007) estado CERRADO: si grupoOriginal.estado != CERRADO, rechaza.
    //       Mensaje transmite: el grupo ya está abierto.
    //     - V-Grupo-008 (Origen spec: VAL-008) solo administrador: comprueba que el usuario autenticado
    //       (AuthUtils.getUser()) pertenece al grupo `admins`; si no, rechaza. Mensaje transmite: no tiene
    //       permisos para reabrir el grupo. (Defensa de servidor además del control de menú/grupo.)

    // --- Reglas de negocio / campos servidor ---

    private void fireActionRule_FijarCentroYCursoAcademicoSiSupervisor(Grupo grupo);
    //   Aplica R-Grupo-002 (Origen spec: RN-002; campos `centro` y `cursoAcademico` clasificados servidor
    //   para el supervisor). Si el usuario autenticado NO es administrador (es supervisor), asignación
    //   INCONDICIONAL: grupo.setCentro(AuthUtils.getUser().getCentroActivo());
    //   grupo.setCursoAcademico(centroActivo.getCurso()). MUST NOT usar `if (campo==null)`: el cliente no
    //   puede dictar estos campos aunque vengan en el JSON del endpoint REST genérico (k-secure-coding §3.3).
    //   Si es administrador, se respetan los valores `cliente` (centro y cursoAcademico que él eligió).

    private void fireActionRule_EstadoInicialAbierto(Grupo grupo);
    //   Asigna INCONDICIONALMENTE grupo.setEstado(EstadoGrupo.ABIERTO) en el alta (campo `estado` servidor).
    //   MUST NOT añadir guarda `if (estado==null)` (k-secure-coding §3.3).

    private void fireActionRule_CrearModulosGrupo(Grupo grupo);
    //   Aplica R-Grupo-001 (Origen spec: RN-001, RES-002). Momento: Después de repository.save (necesita
    //   el grupo persistido para asignarlo a cada ModuloGrupo). Por cada CursoModulo del curso del grupo
    //   (grupo.getCurso().getModulos()), crea un ModuloGrupo {grupo, modulo} vía moduloGrupoRepository.save.
    //   Efecto colateral: inserta N filas ModuloGrupo. No es compleja (bucle de 2-3 llamadas).

    private void fireActionRule_RegistrarCierre(Grupo grupo);
    //   Aplica R-Grupo-003 (Origen spec: RN-003; campos `estado`/`fechaCierre` servidor). Asignación
    //   INCONDICIONAL: grupo.setEstado(CERRADO); grupo.setFechaCierre(LocalDateTime.now()). Sin `if`.

    private void fireActionRule_RegistrarReapertura(Grupo grupo);
    //   Aplica R-Grupo-004 (Origen spec: RN-004; campos `estado`/`fechaCierre` servidor). Asignación
    //   INCONDICIONAL: grupo.setEstado(ABIERTO); grupo.setFechaCierre(null). Sin `if`.

    @Override public AllowProperties allowPropertiesInsert();   // whitelist: nombre, curso, centro, cursoAcademico, alumnosGrupo (ver Frontera de confianza)
    @Override public AllowProperties allowPropertiesUpdate();   // whitelist: nombre (solo)
    @Override public AllowProperties allowPropertiesRemove();   // denyAll (no campos del cliente)
    @Override public AllowProperties allowPropertiesCerrar();   // denyAll (cierre dictado por servidor)
    @Override public AllowProperties allowPropertiesReabrir();  // denyAll (reapertura dictada por servidor)
}
```

### Frontera de confianza — AllowProperties por acción

#### `GrupoServiceImpl.insert` (invocado desde el botón "Guardar" genérico de la vista, vía `save`)

Entidad: `Grupo`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-Grupo.md` (nombre, curso, centro, curso académico, alumnos del grupo).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `nombre` | cliente | sí | Input directo del usuario. |
| `curso` | cliente | sí | Input directo (de él derivan ciclo y módulos). |
| `centro` | cliente (admin) / servidor (supervisor) | sí | En `AllowProperties` de Crear; para el supervisor lo sobrescribe INCONDICIONALMENTE R-Grupo-002 en `insert`, ignorando lo que llegue. |
| `cursoAcademico` | cliente (admin) / servidor (supervisor) | sí | Igual que `centro`: lo fija R-Grupo-002 para el supervisor. |
| `alumnosGrupo` | cliente | sí | En `AllowProperties` de Crear (alta de alumnos junto al grupo). |
| `estado` | servidor | **NO** | Asignado en `insert` → `fireActionRule_EstadoInicialAbierto` (ABIERTO). |
| `fechaCierre` | servidor | **NO** | Solo lo escribe el cierre/reapertura; nunca el cliente. |

#### `GrupoServiceImpl.update` (invocado desde "Guardar" genérico, vía `save`)

Entidad: `Grupo`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-Grupo.md` (nombre).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `nombre` | cliente | sí | Único campo editable tras crear. |
| `curso` | servidor | **NO** | Inmutable; se restaura desde `original` en `update`. |
| `centro` | servidor | **NO** | Inmutable; restaurado desde `original`. |
| `cursoAcademico` | servidor | **NO** | Inmutable; restaurado desde `original`. |
| `estado` | servidor | **NO** | Solo lo cambian cerrar/reabrir; restaurado desde `original` en `update`. |
| `fechaCierre` | servidor | **NO** | Restaurado desde `original`. |

#### `GrupoServiceImpl.cerrar` (invocado desde `GrupoController.cerrar`)

Entidad: `Grupo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** acción `Cerrar` de `entity-Grupo.md` (sin `Input AllowProperties`: el cliente no aporta ningún campo).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `estado` | servidor | **NO** | `fireActionRule_RegistrarCierre` → CERRADO (incondicional). |
| `fechaCierre` | servidor | **NO** | `fireActionRule_RegistrarCierre` → `LocalDateTime.now()` (incondicional). |
| (resto) | servidor | **NO** | El cierre no acepta ningún campo del cliente; se opera sobre el grupo por id. |

#### `GrupoServiceImpl.reabrir` (invocado desde `GrupoController.reabrir`)

Entidad: `Grupo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** acción `Reabrir` de `entity-Grupo.md` (sin `Input AllowProperties`).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `estado` | servidor | **NO** | `fireActionRule_RegistrarReapertura` → ABIERTO (incondicional). |
| `fechaCierre` | servidor | **NO** | `fireActionRule_RegistrarReapertura` → `null` (incondicional). |
| (resto) | servidor | **NO** | La reapertura no acepta campos del cliente. V-Grupo-008 exige rol admin en servidor. |

### Trazabilidad V/R → ubicación (filas de esta tarea)

| V | Origen spec | Ubicación |
|---|---|---|
| V-Grupo-001 | VAL-001 | GrupoServiceImpl.validateInsert |
| V-Grupo-002 | VAL-002 | GrupoServiceImpl.validateInsert |
| V-Grupo-003 | VAL-003, RES-001 | GrupoServiceImpl.validateInsert (+ unique-constraint en Grupo.xml) |
| V-Grupo-004 | VAL-004 | GrupoServiceImpl.validateUpdate |
| V-Grupo-005 | VAL-005, RES-001 | GrupoServiceImpl.validateUpdate |
| V-Grupo-006 | VAL-009 | GrupoServiceImpl.validateRemove |
| V-Grupo-007 | VAL-007 | GrupoServiceImpl.validateReabrir |
| V-Grupo-008 | VAL-008 | GrupoServiceImpl.validateReabrir |
| V-Grupo-009 | VAL-006 | GrupoServiceImpl.validateCerrar |

| R | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Grupo-001 | RN-001, RES-002 | GrupoServiceImpl.fireActionRule_CrearModulosGrupo | Después de save |
| R-Grupo-002 | RN-002 | GrupoServiceImpl.fireActionRule_FijarCentroYCursoAcademicoSiSupervisor | Antes de save |
| R-Grupo-003 | RN-003 | GrupoServiceImpl.fireActionRule_RegistrarCierre | Antes de save (en `cerrar`) |
| R-Grupo-004 | RN-004 | GrupoServiceImpl.fireActionRule_RegistrarReapertura | Antes de save (en `reabrir`) |

> **ADMINISTRADOR no es tipoUsuario.** El administrador se identifica por el grupo `admins` (no hay `TipoUsuario` codigo ADMINISTRADOR en `data-init`). Por eso V-Grupo-008 comprueba pertenencia al grupo `admins` en servidor.
