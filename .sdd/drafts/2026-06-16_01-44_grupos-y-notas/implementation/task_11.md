---
type: implementation-task
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-validaciones

Implementa el servicio de `Grupo` (interfaz + implementación). Alta/modificación/borrado por REST; acciones `cerrarGrupo`/`reabrirGrupo` invocadas desde `GrupoController`; asignación servidor de estado, centro/cursoAcademico (supervisor), generación de módulos del curso.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/service/GrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz ModelService de Grupo |
| `system/gruposnotas/service/impl/GrupoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Implementación de Grupo |

Del diseño, Paso 2 — Servicios. **MUST NOT** crear módulo Guice. Persistir siempre con `repository`, nunca `super.*`. Cada acción empieza con `validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid)`.

```java
// Clase: com.educaflow.system.gruposnotas.service.GrupoService  (extends ModelService<Grupo>)
public interface GrupoService extends ModelService<Grupo> {
    Grupo cerrarGrupo(Grupo grupo, Grupo original);
    Optional<BusinessMessages> validateCerrarGrupo(Grupo grupo, Grupo original);
    AllowProperties allowPropertiesCerrarGrupo();

    Grupo reabrirGrupo(Grupo grupo, Grupo original);
    Optional<BusinessMessages> validateReabrirGrupo(Grupo grupo, Grupo original);
    AllowProperties allowPropertiesReabrirGrupo();
}
```

```java
// Clase: com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl
//   (extends DefaultModelService<Grupo> implements GrupoService)

public GrupoServiceImpl(Class<Grupo> model, Repository<Grupo> repository);
//   Constructor obligatorio: super(model, repository).

// @Inject ModelServiceFactory modelServiceFactory;   // para resolver NotaService/ModuloGrupoService al generar notas

@Override public Grupo insert(Grupo grupo);
//   1) validateInsert(grupo).ifPresent(throwIfInvalid)  → V-Grupo-001/002/003.
//   2) fireActionRule_AsignarCentroYCursoAcademicoSiSupervisor(grupo)  → R-Grupo-002.
//   3) fireActionRule_AsignarEstadoInicial(grupo)  → R-Grupo-001 (parte estado).
//   4) grupo = repository.save(grupo).
//   5) fireActionRule_GenerarModulosGrupo(grupo)  → R-Grupo-001 (RN-001, módulos del curso); efecto colateral (crea ModuloGrupo). Después de save.
//   6) return grupo.

@Override public Grupo update(Grupo grupo, Grupo original);
//   1) validateUpdate(grupo, original).ifPresent(throwIfInvalid)  → V-Grupo-004/005.
//   2) fireActionRule_RestaurarCamposInmutables(grupo, original)  → restaura curso, centro, cursoAcademico, estado, fechaCierre desde `original` (defensa anti mass-assignment: esos campos no se editan en update).
//   3) grupo = repository.save(grupo). return grupo.

@Override public void remove(Grupo grupo);
//   1) validateRemove(grupo).ifPresent(throwIfInvalid)  → V-Grupo-009.
//   2) repository.remove(grupo).  (orphanRemoval borra módulos, alumnos y notas)

@Override public Grupo cerrarGrupo(Grupo grupo, Grupo original);
//   1) validateCerrarGrupo(grupo, original).ifPresent(throwIfInvalid)  → V-Grupo-006.
//   2) fireActionRule_Cerrar(grupo)  → R-Grupo-003.
//   3) return repository.save(grupo).

@Override public Grupo reabrirGrupo(Grupo grupo, Grupo original);
//   1) validateReabrirGrupo(grupo, original).ifPresent(throwIfInvalid)  → V-Grupo-007/008.
//   2) fireActionRule_Reabrir(grupo)  → R-Grupo-004.
//   3) return repository.save(grupo).

/******** Métodos de Validación ********/

@Override public Optional<BusinessMessages> validateInsert(Grupo grupo);
//   - V-Grupo-001 (Origen spec: VAL-001) nombre obligatorio. Mensaje literal: "El nombre del grupo es obligatorio".
//   - V-Grupo-002 (Origen spec: VAL-002) curso obligatorio. Mensaje literal: "El curso es obligatorio".
//   - V-Grupo-003 (Origen spec: VAL-003, RES-001) no existe otro grupo con el mismo nombre en el mismo
//       centro y curso académico. Se consulta GrupoRepository.findByNombreAndCentroAndCursoAcademico(...)
//       usando el centro/cursoAcademico EFECTIVOS (los del centro activo si es supervisor; los del bean si es admin).
//       Mensaje literal: "Ya existe un grupo con ese nombre en este centro y curso académico".

@Override public Optional<BusinessMessages> validateUpdate(Grupo grupo, Grupo original);
//   - V-Grupo-004 (Origen spec: VAL-004) el grupo (original) está ABIERTO; si está CERRADO se rechaza.
//       Mensaje literal: "No se puede modificar un grupo cerrado".
//   - V-Grupo-005 (Origen spec: VAL-005, RES-001) no existe OTRO grupo (id != el suyo) con el mismo nombre
//       en su centro y curso académico (findByNombreAndCentroAndCursoAcademico filtrando el propio id).
//       Mensaje literal: "Ya existe un grupo con ese nombre en este centro y curso académico".

@Override public Optional<BusinessMessages> validateRemove(Grupo grupo);
//   - V-Grupo-009 (Origen spec: VAL-009) el grupo está ABIERTO; si está CERRADO se rechaza.
//       Mensaje literal: "No se puede borrar un grupo cerrado".

@Override public Optional<BusinessMessages> validateCerrarGrupo(Grupo grupo, Grupo original);
//   - V-Grupo-006 (Origen spec: VAL-006) el grupo está ABIERTO; si ya está CERRADO se rechaza.
//       Mensaje literal: "El grupo ya está cerrado".

@Override public Optional<BusinessMessages> validateReabrirGrupo(Grupo grupo, Grupo original);
//   - V-Grupo-007 (Origen spec: VAL-007) el grupo está CERRADO; si ya está ABIERTO se rechaza.
//       Mensaje literal: "El grupo ya está abierto".
//   - V-Grupo-008 (Origen spec: VAL-008) el usuario conectado es administrador / superusuario de Axelor,
//       comprobado con SecurityUtil.isAdmin() / AuthUtils.isAdmin(). Si no, se rechaza.
//       Mensaje literal: "No tiene permisos para reabrir el grupo".

/******** AllowProperties ********/

@Override public AllowProperties allowPropertiesInsert();
//   Whitelist (createAllowProperties): nombre, curso, centro, cursoAcademico.
//   alumnosGrupo queda FUERA: los AlumnoGrupo anidados en el alta del Grupo (cascade del o2m) se
//   persistirían vía GrupoServiceImpl.insert SIN pasar por AlumnoGrupoServiceImpl.insert, saltándose
//   VAL-010..VAL-019 y RN-005 (k-secure-coding §3.6). Los alumnos se añaden SIEMPRE por el modal
//   "Añadir alumno" del panel de alumnos → AlumnoGrupoServiceImpl.insert (RUI-011/012), que sí aplica
//   esas validaciones y crea las notas NO_EVALUADO. estado y fechaCierre también quedan FUERA (campos
//   servidor que insert asigna). Ver tabla §"Frontera de confianza".

@Override public AllowProperties allowPropertiesUpdate();
//   Whitelist (createAllowProperties): nombre.
//   curso, centro, cursoAcademico (inmutables) y estado, fechaCierre (servidor) quedan FUERA.

@Override public AllowProperties allowPropertiesCerrarGrupo();
//   Whitelist vacía (createAllowProperties con Map vacío): la acción no acepta ningún campo del cliente;
//   estado y fechaCierre los pone el servidor.

@Override public AllowProperties allowPropertiesReabrirGrupo();
//   Whitelist vacía: ídem.

/******** Action Rules ********/

private void fireActionRule_AsignarEstadoInicial(Grupo grupo);
//   Aplica R-Grupo-001 (Origen spec: RN-001 parte estado; campo `estado` servidor): asignación
//   INCONDICIONAL grupo.setEstado(EstadoGrupo.ABIERTO). MUST NOT poner `if (estado == null)`:
//   permitiría que el cliente cuele un estado por /ws/rest (ver k-secure-coding §3.3).

private void fireActionRule_AsignarCentroYCursoAcademicoSiSupervisor(Grupo grupo);
//   Aplica R-Grupo-002 (Origen spec: RN-002; campos `centro`/`cursoAcademico`). Si el usuario conectado
//   es SUPERVISOR (no administrador), sobrescribe INCONDICIONALMENTE grupo.setCentro(centroActivo) y
//   grupo.setCursoAcademico(centroActivo.getCurso()) con los del centro activo, ignorando lo que llegue
//   del cliente. Si es administrador, respeta el centro y cursoAcademico aportados (cliente). Defensa
//   anti mass-assignment: aunque ambos campos estén en la whitelist de insert (para el admin), el
//   supervisor nunca los dicta. Fuente: AuthUtils/SecurityUtil getUser().getCentroActivo() y
//   getTiposUsuarioActivos().

private void fireActionRule_GenerarModulosGrupo(Grupo grupo);
//   Aplica R-Grupo-001 (Origen spec: RN-001, RES-002; efecto colateral). Por cada CursoModulo del curso
//   del grupo, crea un ModuloGrupo(grupo, modulo) vía ModuloGrupoService (modelServiceFactory.resolve).
//   Garantiza RES-002 (los módulos del grupo = módulos del curso). Se ejecuta DESPUÉS de save (el grupo
//   ya tiene id). Las consultas a CursoModulo van por su repositorio/finder, no inline.

private void fireActionRule_Cerrar(Grupo grupo);
//   Aplica R-Grupo-003 (Origen spec: RN-003; campos `estado`/`fechaCierre` servidor): asignación
//   INCONDICIONAL grupo.setEstado(CERRADO) y grupo.setFechaCierre(LocalDateTime.now()).

private void fireActionRule_Reabrir(Grupo grupo);
//   Aplica R-Grupo-004 (Origen spec: RN-004; campos `estado`/`fechaCierre` servidor): asignación
//   INCONDICIONAL grupo.setEstado(ABIERTO) y grupo.setFechaCierre(null).

private void fireActionRule_RestaurarCamposInmutables(Grupo grupo, Grupo original);
//   Aplica R-Grupo-005 (Origen spec: — ; defensa técnica anti mass-assignment): restaura
//   INCONDICIONALMENTE desde `original` los campos que update no debe tocar: curso, centro,
//   cursoAcademico, estado, fechaCierre. Evita que el cliente cambie por /ws/rest campos inmutables o de
//   estado en un update (que solo debería tocar `nombre`).
```

**Verificar:** que el `*ServiceImpl` está en `service.impl` con el nombre exacto `GrupoServiceImpl` y el constructor `(Class<T>, Repository<T>)`; `grep` confirma que no hay `super.insert/update/remove`.

### Frontera de confianza — AllowProperties por acción

#### `GrupoServiceImpl.insert` (vía REST genérico `save`)

Entidad: `Grupo`. **Forma elegida:** `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-Grupo.md`.

| Campo            | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|------------------|----------|--------------|---------------------------------------------|
| `nombre`         | cliente  | sí           | Input directo del usuario. |
| `curso`          | cliente  | sí           | Input directo (de él derivan módulos). Inmutable tras crear. |
| `centro`         | cliente/servidor | sí   | El admin lo aporta; para el supervisor se SOBRESCRIBE incondicionalmente en `fireActionRule_AsignarCentroYCursoAcademicoSiSupervisor` (R-Grupo-002). En whitelist porque el admin sí lo dicta. |
| `cursoAcademico` | cliente/servidor | sí   | Ídem `centro` (admin lo aporta; supervisor sobrescrito por R-Grupo-002). |
| `alumnosGrupo`   | cliente  | **NO**       | Aunque el spec lo lista en `Input AllowProperties` de Crear, se EXCLUYE de la whitelist: los `AlumnoGrupo` anidados (cascade del o2m) se persistirían vía `GrupoServiceImpl.insert` SIN pasar por `AlumnoGrupoServiceImpl.insert`, saltándose VAL-010..VAL-019 y RN-005 (k-secure-coding §3.6, defensa de los hijos creados anidados). Los alumnos se añaden SIEMPRE por el modal "Añadir alumno" → `AlumnoGrupoServiceImpl.insert` (RUI-011/012). |
| `estado`         | servidor | **NO**       | Asignado en `insert` → `fireActionRule_AsignarEstadoInicial` (ABIERTO). |
| `fechaCierre`    | servidor | **NO**       | Lo pone/borra el servidor en cerrar/reabrir; `insert` no lo toca. |
| `modulosGrupo`   | servidor | **NO**       | Generados por el servidor (R-Grupo-001/RN-001). |

#### `GrupoServiceImpl.update` (vía REST genérico `save`)

Entidad: `Grupo`. **Forma elegida:** `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-Grupo.md`.

| Campo            | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|------------------|----------|--------------|---------------------------------------------|
| `nombre`         | cliente  | sí           | Único campo editable en Modificar. |
| `curso`          | cliente  | **NO**       | Inmutable; restaurado desde `original` en `fireActionRule_RestaurarCamposInmutables`. |
| `centro`         | servidor | **NO**       | Inmutable; restaurado desde `original`. |
| `cursoAcademico` | servidor | **NO**       | Inmutable; restaurado desde `original`. |
| `estado`         | servidor | **NO**       | Solo cambia por cerrar/reabrir; restaurado desde `original`. |
| `fechaCierre`    | servidor | **NO**       | Ídem; restaurado desde `original`. |

#### `GrupoServiceImpl.cerrarGrupo` (invocado desde `GrupoController.cerrarGrupo`)

Entidad: `Grupo`. **Forma elegida:** `createAllowProperties` (whitelist **vacía**).
**Origen spec:** acción `Cerrar` de `entity-Grupo.md` (sin `Input AllowProperties`).

| Campo         | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|---------------|----------|--------------|---------------------------------------------|
| (ninguno)     | —        | —            | La acción no acepta campos del cliente; `estado`=CERRADO y `fechaCierre`=now los pone `fireActionRule_Cerrar`. |

#### `GrupoServiceImpl.reabrirGrupo` (invocado desde `GrupoController.reabrirGrupo`)

Entidad: `Grupo`. **Forma elegida:** `createAllowProperties` (whitelist **vacía**).
**Origen spec:** acción `Reabrir` de `entity-Grupo.md` (sin `Input AllowProperties`).

| Campo         | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|---------------|----------|--------------|---------------------------------------------|
| (ninguno)     | —        | —            | La acción no acepta campos del cliente; `estado`=ABIERTO y `fechaCierre`=null los pone `fireActionRule_Reabrir`. |

### Trazabilidad relevante
| V | Origen spec | Ubicación |
|---|-------------|-----------|
| V-Grupo-001 | VAL-001 | `GrupoServiceImpl.validateInsert` (+ cliente `required` en `Grupo.nombre`) |
| V-Grupo-002 | VAL-002 | `GrupoServiceImpl.validateInsert` (+ cliente `required` en `Grupo.curso`) |
| V-Grupo-003 | VAL-003, RES-001 | `GrupoServiceImpl.validateInsert` (`GrupoRepository.findByNombreAndCentroAndCursoAcademico`) + `unique-constraint(nombre,centro,cursoAcademico)` |
| V-Grupo-004 | VAL-004 | `GrupoServiceImpl.validateUpdate` |
| V-Grupo-005 | VAL-005, RES-001 | `GrupoServiceImpl.validateUpdate` (`findByNombreAndCentroAndCursoAcademico`, excluye id propio) |
| V-Grupo-006 | VAL-006 | `GrupoServiceImpl.validateCerrarGrupo` |
| V-Grupo-007 | VAL-007 | `GrupoServiceImpl.validateReabrirGrupo` |
| V-Grupo-008 | VAL-008 | `GrupoServiceImpl.validateReabrirGrupo` (rol ADMINISTRADOR vía AuthUtils/SecurityUtil) |
| V-Grupo-009 | VAL-009 | `GrupoServiceImpl.validateRemove` |

| R | Origen spec | Ubicación | Momento |
|---|-------------|-----------|---------|
| R-Grupo-001 | RN-001, RES-002 | `GrupoServiceImpl.fireActionRule_AsignarEstadoInicial` (Antes) + `fireActionRule_GenerarModulosGrupo` (Después de save) | Antes (estado) / Después (módulos) |
| R-Grupo-002 | RN-002 | `GrupoServiceImpl.fireActionRule_AsignarCentroYCursoAcademicoSiSupervisor` (asignación incondicional para supervisor) | Antes de save (insert) |
| R-Grupo-003 | RN-003 | `GrupoServiceImpl.fireActionRule_Cerrar` (estado=CERRADO, fechaCierre=now, incondicional) | Antes de save (cerrarGrupo) |
| R-Grupo-004 | RN-004 | `GrupoServiceImpl.fireActionRule_Reabrir` (estado=ABIERTO, fechaCierre=null, incondicional) | Antes de save (reabrirGrupo) |
| R-Grupo-005 | — | `GrupoServiceImpl.fireActionRule_RestaurarCamposInmutables` (defensa anti mass-assignment en update) | Antes de save (update) |
