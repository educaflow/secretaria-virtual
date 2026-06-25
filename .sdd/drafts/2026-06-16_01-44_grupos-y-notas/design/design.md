---
type: design
---

# Diseño: Grupos y notas

**Objetivo:** Permitir que la secretaría de un centro defina grupos de alumnos ligados a un curso, registre la nota final de cada alumno en cada módulo del grupo y calcule su nota media, con cierre/reapertura del grupo y consulta de notas por el alumno.
**Capa:** system/gruposnotas (paquete `com.educaflow.system.gruposnotas`)
**Especificación de origen:** .sdd/drafts/2026-06-16_01-44_grupos-y-notas/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones, k-datainit

Es un **sistema** nuevo. Depende de los subsistemas `sistemaeducativo` (Curso, CursoModulo, Modulo, Ciclo) y `common` (Centro, CentroUsuario, CentroUsuarioTipoUsuario, TipoUsuario) y del `User` de Axelor. No depende de ningún otro sistema. No se crean módulos Guice para los `ModelService` (los descubre `ModelServiceFactory`).

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/Grupo.xml` | Crear | k-sistemas (modelos.md) | Entidad Grupo + enum EstadoGrupo |
| `system/gruposnotas/domains/ModuloGrupo.xml` | Crear | k-sistemas (modelos.md) | Entidad ModuloGrupo |
| `system/gruposnotas/domains/AlumnoGrupo.xml` | Crear | k-sistemas (modelos.md) | Entidad AlumnoGrupo + transient notaMedia |
| `system/gruposnotas/domains/Nota.xml` | Crear | k-sistemas (modelos.md) | Entidad Nota + enum ValorNota |
| `system/gruposnotas/service/GrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz ModelService de Grupo |
| `system/gruposnotas/service/impl/GrupoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Implementación de Grupo |
| `system/gruposnotas/service/ModuloGrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz ModelService de ModuloGrupo |
| `system/gruposnotas/service/impl/ModuloGrupoServiceImpl.java` | Crear | k-sistemas | Implementación de ModuloGrupo |
| `system/gruposnotas/service/AlumnoGrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz ModelService de AlumnoGrupo (+ calcularNotaMedia) |
| `system/gruposnotas/service/impl/AlumnoGrupoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Implementación de AlumnoGrupo |
| `system/gruposnotas/service/NotaService.java` | Crear | k-sistemas (servicios.md) | Interfaz ModelService de Nota |
| `system/gruposnotas/service/impl/NotaServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Implementación de Nota |
| `system/gruposnotas/db/repo/GrupoRepository.java` | Crear | k-sistemas (modelos.md) | Repository de Grupo (finder duplicado de nombre) |
| `system/gruposnotas/db/repo/AlumnoGrupoRepository.java` | Crear | k-sistemas (modelos.md) | Repository de AlumnoGrupo (finder por alumno + curso académico) |
| `system/gruposnotas/db/repo/NotaRepository.java` | Crear | k-sistemas (modelos.md) | Repository de Nota (contar MH por módulo del grupo) |
| `system/gruposnotas/controller/GrupoController.java` | Crear | k-sistemas (controladores.md) | Botones "Cerrar grupo" / "Reabrir grupo" |
| `system/gruposnotas/views/Grupo-Supervisor.xml` | Crear | k-vistas | `sysGruposNotas.Grupo@Supervisor-action` (árbol completo) |
| `system/gruposnotas/views/Grupo-Administracion.xml` | Crear | k-vistas | `sysGruposNotas.Grupo@Administracion-action` (árbol completo) |
| `system/gruposnotas/views/AlumnoGrupo-MisNotas.xml` | Crear | k-vistas | `sysGruposNotas.AlumnoGrupo@MisNotas-action` (árbol del alumno) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir/fusionar los menús del sistema (ya presentes — fusión idempotente) |
| `system/gruposnotas/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de binding de los permisos |
| `system/gruposnotas/data-init/input/auth-gruposnotas.xml` | Crear | k-datainit | Permisos del sistema (acceso por centro / por alumno) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

---

## Pasos

### Paso 1 — Dominios

Crear los cuatro ficheros `design/domains/<Entidad>.xml` (XML completo y materializado). Resumen estructural:

- **`domains/Grupo.xml`** — entidad `Grupo` (`repository="abstract"`). Campos: `nombre` (string, `required`, `namecolumn`, **cliente**), `curso` (m2o Curso, `required`, **cliente**, inmutable), `centro` (m2o Centro, **cliente para admin / servidor para supervisor**, inmutable, SIN `required`), `cursoAcademico` (integer, **cliente para admin / servidor para supervisor**, inmutable, SIN `required`), `estado` (enum `EstadoGrupo`, **servidor**, SIN `required`), `fechaCierre` (datetime, **servidor**, SIN `required`), `modulosGrupo` (o2m ModuloGrupo, `orphanRemoval`), `alumnosGrupo` (o2m AlumnoGrupo, `orphanRemoval`). `unique-constraint(nombre,centro,cursoAcademico)` (RES-001). `finder-method findByNombreAndCentroAndCursoAcademico` (VAL-003/005). Enum `EstadoGrupo {ABIERTO, CERRADO}`. El ciclo del grupo se muestra en grids con el path `curso.ciclo` (no se persiste; RES-002 derivación).
- **`domains/ModuloGrupo.xml`** — entidad `ModuloGrupo` (`repository="abstract"`). Campos: `grupo` (m2o Grupo, `required`, **servidor**), `modulo` (m2o Modulo, `required`, **servidor**), `notas` (o2m Nota, `orphanRemoval`). `unique-constraint(grupo,modulo)` (RES-003).
- **`domains/AlumnoGrupo.xml`** — entidad `AlumnoGrupo` (`repository="abstract"`). Campos: `grupo` (m2o Grupo, **cliente**, inmutable, SIN `required`), `alumno` (m2o User, **cliente**, inmutable, SIN `required`), `notas` (o2m Nota, `orphanRemoval`), `notaMedia` (string `transient`, **servidor**, CC-001 momento lectura: getter computado **INLINE** que recorre `this.getNotas()` referenciando solo `Nota`/`ValorNota` de `..db..` (sin `Beans.get` ni dependencia de `..service..`); devuelve "Sin nota" si no hay módulos evaluados). `unique-constraint(grupo,alumno)` (RES-005). `finder-method findByAlumnoAndGrupoCursoAcademico` (RES-004/VAL-013).
- **`domains/Nota.xml`** — entidad `Nota` (`repository="abstract"`). Campos: `moduloGrupo` (m2o ModuloGrupo, `required`, **servidor**), `alumnoGrupo` (m2o AlumnoGrupo, `required`, **servidor**), `valor` (enum `ValorNota`, **cliente** en Modificar, inicial NO_EVALUADO, SIN `required`), `fechaCalificacion` (datetime, **servidor**, CC-002), `fechaUltimaModificacion` (datetime, **servidor**, CC-003). `unique-constraint(moduloGrupo,alumnoGrupo)` (RES-006). `finder-method countMatriculasHonorByModuloGrupo` (VAL-017). Enum `ValorNota {NO_EVALUADO, NOTA_1..NOTA_10, MATRICULA_HONOR}`.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh` → `VALIDACION-XML: OK`.

### Paso 2 — Servicios

Cuatro `ModelService`/`DefaultModelService`. **MUST NOT** crear módulo Guice (los descubre `ModelServiceFactory`). Persistir siempre con `repository`, nunca `super.*`. Cada acción empieza con `validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid)`.

#### `GrupoService` / `GrupoServiceImpl`

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
//   - V-Grupo-008 (Origen spec: VAL-008) el usuario conectado es ADMINISTRADOR (SecurityUtil/AuthUtils +
//       getTiposUsuarioActivos con código 'ADMINISTRADOR'). Si no, se rechaza.
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

#### `ModuloGrupoService` / `ModuloGrupoServiceImpl`

```java
// Clase: com.educaflow.system.gruposnotas.service.ModuloGrupoService  (extends ModelService<ModuloGrupo>)
public interface ModuloGrupoService extends ModelService<ModuloGrupo> {
    ModuloGrupo insert(ModuloGrupoInsertDTO dto);
    Optional<BusinessMessages> validateInsert(ModuloGrupoInsertDTO dto);
}
// DTO: com.educaflow.system.gruposnotas.service.ModuloGrupoInsertDTO (record: Grupo grupo, Modulo modulo)

// Clase: com.educaflow.system.gruposnotas.service.impl.ModuloGrupoServiceImpl
public ModuloGrupoServiceImpl(Class<ModuloGrupo> model, Repository<ModuloGrupo> repository);
// @Inject ModelServiceFactory modelServiceFactory;   // para crear las notas de cada alumno ya presente (no aplica en alta normal)

@Override public ModuloGrupo insert(ModuloGrupoInsertDTO dto);
//   Alta PROGRAMÁTICA (la invoca GrupoServiceImpl al generar los módulos del grupo; no hay UI/REST de alta).
//   1) validateInsert(dto).ifPresent(throwIfInvalid).
//   2) construye ModuloGrupo(grupo, modulo) y return repository.save(...).
//   El DTO es la whitelist (no hay AllowProperties): ningún campo servidor sin justificar.

@Override public Optional<BusinessMessages> validateInsert(ModuloGrupoInsertDTO dto);
//   Comprueba que grupo y modulo no son nulos (integridad del DTO). Sin mensajes de negocio del spec.
//   NOTA: la unicidad RES-003 la garantiza el unique-constraint del dominio.
```

> No se declara `AllowProperties` para `ModuloGrupo`: no hay `@CallMethod` ni alta del cliente (los módulos del grupo no se editan; `Input AllowProperties` de ModuloGrupo está vacío en el spec). El alta es programática vía DTO.

#### `AlumnoGrupoService` / `AlumnoGrupoServiceImpl`

```java
// Clase: com.educaflow.system.gruposnotas.service.AlumnoGrupoService  (extends ModelService<AlumnoGrupo>)
public interface AlumnoGrupoService extends ModelService<AlumnoGrupo> {
    String calcularNotaMedia(AlumnoGrupo alumnoGrupo);   // CC-001 (lectura) — DELEGA en el getter transient del dominio (única fuente de verdad)
}

// Clase: com.educaflow.system.gruposnotas.service.impl.AlumnoGrupoServiceImpl
public AlumnoGrupoServiceImpl(Class<AlumnoGrupo> model, Repository<AlumnoGrupo> repository);
// @Inject ModelServiceFactory modelServiceFactory;   // para resolver NotaService al crear las notas

@Override public AlumnoGrupo insert(AlumnoGrupo alumnoGrupo);
//   1) validateInsert(alumnoGrupo).ifPresent(throwIfInvalid)
//        → V-AlumnoGrupo-002/003/004/005/007/008 (la 005/RES-005 la garantiza el unique-constraint).
//   2) alumnoGrupo = repository.save(alumnoGrupo).
//   3) fireActionRule_CrearNotasNoEvaluado(alumnoGrupo)  → R-AlumnoGrupo-001 (RN-005); efecto colateral. Después de save.
//   4) return alumnoGrupo.

@Override public void remove(AlumnoGrupo alumnoGrupo);
//   1) validateRemove(alumnoGrupo).ifPresent(throwIfInvalid)  → V-AlumnoGrupo-006.
//   2) repository.remove(alumnoGrupo).  (orphanRemoval borra sus notas)

// NOTA: NO se sobrescribe update (AlumnoGrupo no tiene Input AllowProperties en Modificar; el alumno no
// se reparenta). Si llega un update por /ws/rest, allowPropertiesUpdate (whitelist vacía) lo neutraliza.

@Override public String calcularNotaMedia(AlumnoGrupo alumnoGrupo);
//   Implementa CC-001 (Origen spec: CC-001, momento lectura). DELEGA en el getter del dominio:
//   `return alumnoGrupo.getNotaMedia();`. El algoritmo (excluir NO_EVALUADO; MATRICULA_HONOR→10 y NOTA_n→n;
//   "Sin nota" si no hay ninguna evaluada; media redondeada al entero más cercano como texto) vive INLINE en
//   el getter transient de `AlumnoGrupo` (..db.., sin dependencia de service): el getter es la única fuente de
//   verdad y este método del servicio NO duplica el cálculo (evita que la entidad dependa de ..service.. — C13/C14).

/******** Métodos de Validación ********/

@Override public Optional<BusinessMessages> validateInsert(AlumnoGrupo alumnoGrupo);
//   - V-AlumnoGrupo-007 (Origen spec: VAL-018) grupo indicado (no nulo). Mensaje literal: "El grupo es obligatorio".
//   - V-AlumnoGrupo-002 (Origen spec: VAL-010) alumno indicado (no nulo). Mensaje literal: "Debe elegir un alumno".
//   - V-AlumnoGrupo-003 (Origen spec: VAL-011) el grupo está ABIERTO. Mensaje literal:
//       "No se pueden añadir alumnos a un grupo cerrado".
//   - V-AlumnoGrupo-008 (Origen spec: VAL-019) si el usuario es SUPERVISOR, el grupo pertenece a su
//       centro activo (defensa IDOR del padre, k-secure-coding §3.6). Mensaje literal: "El grupo no pertenece a su centro".
//   - V-AlumnoGrupo-004 (Origen spec: VAL-012) el alumno es un usuario del centro del grupo de tipo Alumno
//       (consulta CentroUsuario/CentroUsuarioTipoUsuario por su repositorio; comprueba que existe un
//       CentroUsuario(alumno, grupo.centro) con TipoUsuario.codigo='ALUMNO'). Mensaje literal:
//       "El alumno debe ser un usuario de tipo Alumno del centro del grupo".
//   - V-AlumnoGrupo-005 (Origen spec: VAL-013, RES-004) el alumno no pertenece ya a otro grupo del mismo
//       curso académico (AlumnoGrupoRepository.findByAlumnoAndGrupoCursoAcademico, excluyendo el propio id).
//       Mensaje literal: "El alumno ya pertenece a otro grupo de este curso académico".
//   NOTA RES-005 (V-AlumnoGrupo-001, Origen spec: ESC-019 / RES-005): el alumno duplicado en el mismo grupo
//       lo impide el unique-constraint(grupo,alumno) del dominio; el guardado falla silenciosamente para el
//       cliente (no se añade dos veces), conforme a ESC-019.

@Override public Optional<BusinessMessages> validateRemove(AlumnoGrupo alumnoGrupo);
//   - V-AlumnoGrupo-006 (Origen spec: VAL-014) el grupo está ABIERTO. Mensaje literal:
//       "No se pueden quitar alumnos de un grupo cerrado".

/******** AllowProperties ********/

@Override public AllowProperties allowPropertiesInsert();
//   Whitelist (createAllowProperties): grupo, alumno  (ambos cliente; el padre `grupo` se valida en
//   validateInsert — k-secure-coding §3.6). notas, notaMedia quedan FUERA (servidor/transient).

@Override public AllowProperties allowPropertiesUpdate();
//   Whitelist vacía (createAllowProperties con Map vacío): AlumnoGrupo no admite cambios de campos del
//   cliente (no se reparenta). grupo y alumno son inmutables y quedan FUERA.

/******** Action Rules ********/

private void fireActionRule_CrearNotasNoEvaluado(AlumnoGrupo alumnoGrupo);
//   Aplica R-AlumnoGrupo-001 (Origen spec: RN-005; efecto colateral). Por cada ModuloGrupo del grupo,
//   crea una Nota(moduloGrupo, alumnoGrupo, valor=NO_EVALUADO) vía NotaService (alta programática).
//   Después de save. Las consultas a ModuloGrupo van por su repositorio/finder, no inline.
```

#### `NotaService` / `NotaServiceImpl`

```java
// Clase: com.educaflow.system.gruposnotas.service.NotaService  (extends ModelService<Nota>)
public interface NotaService extends ModelService<Nota> {
    Nota insert(NotaInsertDTO dto);
    Optional<BusinessMessages> validateInsert(NotaInsertDTO dto);
}
// DTO: com.educaflow.system.gruposnotas.service.NotaInsertDTO (record: ModuloGrupo moduloGrupo, AlumnoGrupo alumnoGrupo)

// Clase: com.educaflow.system.gruposnotas.service.impl.NotaServiceImpl
public NotaServiceImpl(Class<Nota> model, Repository<Nota> repository);

@Override public Nota insert(NotaInsertDTO dto);
//   Alta PROGRAMÁTICA (la invoca AlumnoGrupoServiceImpl al crear las notas NO_EVALUADO; no hay UI/REST de alta).
//   1) validateInsert(dto).ifPresent(throwIfInvalid).
//   2) construye Nota(moduloGrupo, alumnoGrupo, valor=NO_EVALUADO) y return repository.save(...).
//   fechaCalificacion y fechaUltimaModificacion quedan nulas (aún NO_EVALUADO).

@Override public Nota update(Nota nota, Nota original);
//   1) validateUpdate(nota, original).ifPresent(throwIfInvalid)  → V-Nota-002/003/004.
//   2) fireActionRule_RestaurarCamposInmutables(nota, original)  → restaura moduloGrupo, alumnoGrupo desde `original`.
//   3) fireActionRule_AsignarFechaCalificacion(nota, original)   → R-Nota-001 (CC-002).
//   4) fireActionRule_AsignarFechaUltimaModificacion(nota, original) → R-Nota-002 (CC-003).
//   5) return repository.save(nota).

@Override public Optional<BusinessMessages> validateInsert(NotaInsertDTO dto);
//   Comprueba que moduloGrupo y alumnoGrupo no son nulos (integridad del DTO).

@Override public Optional<BusinessMessages> validateUpdate(Nota nota, Nota original);
//   - V-Nota-002 (Origen spec: VAL-015) el grupo de la nota (moduloGrupo.grupo) está ABIERTO. Mensaje literal:
//       "No se pueden modificar las notas de un grupo cerrado".
//   - V-Nota-003 (Origen spec: VAL-016) el valor pertenece al dominio {NO_EVALUADO, NOTA_1..NOTA_10,
//       MATRICULA_HONOR}. (Defensa por si llega un valor crudo por /ws/rest fuera del enum.) Mensaje literal:
//       "La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor".
//   - V-Nota-004 (Origen spec: VAL-017) si el nuevo valor es MATRICULA_HONOR, en el módulo no hay ya 3
//       notas con MATRICULA_HONOR (NotaRepository.countMatriculasHonorByModuloGrupo excluyendo la nota actual).
//       Mensaje literal: "No se pueden poner más de 3 matrículas de honor en un módulo".

/******** AllowProperties ********/

@Override public AllowProperties allowPropertiesUpdate();
//   Whitelist (createAllowProperties): valor  (único campo cliente en Modificar).
//   moduloGrupo, alumnoGrupo (inmutables) y fechaCalificacion, fechaUltimaModificacion (servidor) quedan FUERA.

/******** Action Rules ********/

private void fireActionRule_AsignarFechaCalificacion(Nota nota, Nota original);
//   Aplica R-Nota-001 (Origen spec: CC-002; campo `fechaCalificacion` servidor). Si la nota pasa de
//   NO_EVALUADO (original) a un valor distinto y fechaCalificacion aún está vacía, asigna
//   INCONDICIONALMENTE nota.setFechaCalificacion(LocalDateTime.now()). La condición de transición se evalúa
//   contra `original` (servidor), no contra un flag del cliente; el cliente NO puede dictar fechaCalificacion
//   (está fuera de la whitelist). No es el anti-patrón `if (campo==null) set`: la guarda es la transición de
//   estado de la nota leída del servidor, no la nulidad de un campo que el cliente podría rellenar.

private void fireActionRule_AsignarFechaUltimaModificacion(Nota nota, Nota original);
//   Aplica R-Nota-002 (Origen spec: CC-003; campo `fechaUltimaModificacion` servidor). Si el valor cambia
//   respecto a `original` y la nota YA tenía fechaCalificacion (ya estaba calificada antes), asigna
//   INCONDICIONALMENTE nota.setFechaUltimaModificacion(LocalDateTime.now()). El cliente NO puede dictar este
//   campo (fuera de la whitelist).

private void fireActionRule_RestaurarCamposInmutables(Nota nota, Nota original);
//   Aplica R-Nota-003 (Origen spec: — ; defensa técnica): restaura INCONDICIONALMENTE moduloGrupo y
//   alumnoGrupo desde `original` (no se reparenta una nota en update).
```

**Verificar:** que cada `*ServiceImpl` está en `service.impl` con el nombre exacto `<Entidad>ServiceImpl` y el constructor `(Class<T>, Repository<T>)`; `grep` confirma que no hay `super.insert/update/remove`.

### Paso 3 — Repositorios

Tres repositorios personalizados (entidades con `repository="abstract"`). Los finders se declaran como `<finder-method>` en el dominio (ya materializados); los repositorios concretos heredan de `Abstract<Entidad>Repository`:

```java
// com.educaflow.system.gruposnotas.db.repo.GrupoRepository extends AbstractGrupoRepository { }
//   Hereda findByNombreAndCentroAndCursoAcademico (generado del finder). Sin métodos extra.

// com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository extends AbstractAlumnoGrupoRepository { }
//   Hereda findByAlumnoAndGrupoCursoAcademico (lista). Sin métodos extra.

// com.educaflow.system.gruposnotas.db.repo.NotaRepository extends AbstractNotaRepository { }
//   Hereda countMatriculasHonorByModuloGrupo (lista; el servicio cuenta el tamaño). Sin métodos extra.
```

> La comprobación de tipo de usuario del alumno (V-AlumnoGrupo-004) consulta `CentroUsuario`/`CentroUsuarioTipoUsuario` (entidades de `common`); se hace con `JPA.all(CentroUsuario.class).filter(...).bind(...)` parametrizado en un finder/método ad-hoc del servicio sobre esas entidades externas (no del repositorio de gruposnotas). Usar `:param` con `bind` (k-secure-coding §5).

**Verificar:** cada entidad con repositorio personalizado lleva `repository="abstract"` (ya en los dominios).

### Paso 4 — Controladores

Un único controlador para los botones de `Grupo` (Cerrar / Reabrir). Las notas y alumnos se guardan con `save-modal` (endpoint REST), sin controlador.

```java
// Clase: com.educaflow.system.gruposnotas.controller.GrupoController
// @Inject private ModelServiceFactory modelServiceFactory;

@CallMethod @Transactional
public void cerrarGrupo(ActionRequest actionRequest, ActionResponse actionResponse);
//   Resuelve GrupoService con modelServiceFactory.resolve(Grupo.class).
//   getOriginalModel() (estado actual) y getModel(grupoService.allowPropertiesCerrarGrupo()).
//   Llama grupoService.validateCerrarGrupo(grupo, original); si hay errores,
//   actionResponseHelper.doResponseBusinessMessagesAsError(...). Si no, grupoService.cerrarGrupo(grupo, original)
//   y actionResponse.setSignal("refresh", null) / reload para reflejar estado y fechaCierre.

@CallMethod @Transactional
public void reabrirGrupo(ActionRequest actionRequest, ActionResponse actionResponse);
//   Análogo con validateReabrirGrupo + reabrirGrupo. La validación de rol ADMINISTRADOR (V-Grupo-008) vive
//   en el SERVICIO (no en el controlador), para proteger también el endpoint REST.
```

> Parámetros **MUST** llamarse `actionRequest`/`actionResponse`. El controlador no contiene lógica de negocio ni comprobaciones de rol (van en el servicio).

### Paso 5 — Vistas

Tres ficheros, uno por `<action-view>` (regla "un `<action-view>` por fichero"). XML completo materializado en `design/views/`.

- **`views/Grupo-Supervisor.xml`** — `sysGruposNotas.Grupo@Supervisor-action` (grid + form de Grupo) con `<domain>self.centro = :centroActivoUsuario</domain>` (ESC-021). Árbol maestro-detalle: Grupo → panel "Módulos" (`ModuloGrupo@Supervisor` grid/form) → panel "Notas" (`Nota@Supervisor` grid/form), y Grupo → panel "Alumnos" (`AlumnoGrupo@Supervisor` grid/form). Botones: Borrar, "Cerrar grupo" (showIf estado=='ABIERTO' → `Remote-cerrarGrupo`), Cancelar, Guardar. `onNew` rellena centro/cursoAcademico del centro activo (RUI-001/002) y readonly. Form en readonly si CERRADO (RUI-004). El selector de alumno filtra alumnos del centro del supervisor (RUI-011 + UX de VAL-012/ESC-020). Nota: `valor` readonly si grupo CERRADO (RUI-005).
- **`views/Grupo-Administracion.xml`** — `sysGruposNotas.Grupo@Administracion-action` sin domain de centro (todos los centros), columna `centro` en el grid. Form con `centro` y `cursoAcademico` editables solo en alta (RUI-006). Botones añaden "Reabrir grupo" (showIf estado=='CERRADO' → `Remote-reabrirGrupo`, RUI-008) además de "Cerrar grupo" (RUI-007). Form readonly si CERRADO salvo botones (RUI-009). `valor` readonly si grupo CERRADO (RUI-010). Mismo árbol de paneles con discriminador `@Administracion`.
- **`views/AlumnoGrupo-MisNotas.xml`** — `sysGruposNotas.AlumnoGrupo@MisNotas-action` (grid + form de AlumnoGrupo) con `<domain>self.alumno = :__user__</domain>` (ESC-026). Solo lectura: lista de mis grupos (curso académico, ciclo, nombre, nota media) → form "Mi grupo" con panel "Mis notas" (`Nota@MisNotas` grid/form, módulo, valor, fecha de calificación), todo readonly.

**Verificar:** `validate.sh` → `VALIDACION-XML: OK`; cada `<action-view>` en su propio fichero.

### Paso 6 — Menús

Modificar el fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` fusionando los `<menuitem>` de `design/menus.xml`:

- `notas-menuitem` (raíz "Notas", `if` SUPERVISOR) + `notas-grupos-menuitem` (hoja "Grupos" → `sysGruposNotas.Grupo@Supervisor-action`).
- `misNotas-menuitem` (raíz/hoja "Mis notas" → `sysGruposNotas.AlumnoGrupo@MisNotas-action`, `if` ALUMNO).
- `administracionSv-grupos-menuitem` (hoja de `administracionSv-menuitem`, "Grupos (administración)" → `sysGruposNotas.Grupo@Administracion-action`, `groups="admins"`).

> Estas tres líneas **ya están presentes** en el menús del proyecto (verificado): la fusión es **idempotente** (no se duplica nada).

### Paso 7 — Seguridad

Carpeta `system/gruposnotas/data-init/` con `input-config.xml` (binding de permisos) e `input/auth-gruposnotas.xml`. Reglas de acceso (en lenguaje natural; el XML de import lo materializa `/sdd-implementer`):

- **Grupo, ModuloGrupo, Nota, AlumnoGrupo — supervisor (rol/condición por centro):** permisos `<permission>` con `condition="self.centro = ?"` / `conditionParams="__user__.centroActivo"` para `Grupo` (acceso solo a grupos de su centro activo, ESC-021). Para `ModuloGrupo`, `Nota`, `AlumnoGrupo` la condición se expresa navegando al centro del grupo (p.ej. `self.grupo.centro = ?` para ModuloGrupo y AlumnoGrupo; `self.moduloGrupo.grupo.centro = ?` para Nota). Permisos `create/read/write/remove` para el supervisor.
- **Administrador (grupo `admins`):** acceso completo (sin condición de centro) a las cuatro entidades (gestiona cualquier centro y puede reabrir). Se concede por el `groups="admins"` del menú de administración y por permisos sin `condition`.
- **Alumno:** acceso de **solo lectura** restringido a SUS datos: `AlumnoGrupo` con `condition="self.alumno = ?"` / `conditionParams="__user__"`; `Nota` con condición navegando `self.alumnoGrupo.alumno = ?`; `ModuloGrupo`/`Grupo` de solo lectura limitados a los grupos a los que pertenece (ESC-026, ESC-015). Solo `read`.

> El detalle exacto de roles/grupos Axelor y la sintaxis de `condition`/`conditionParams` se materializa en `auth-gruposnotas.xml` siguiendo `k-datainit`. No se usa `k-seguridad` (obsoleto): las condiciones por centro/usuario se modelan como en `auth-gestioncentro.xml` (`condition="self.centro = ?"`, `conditionParams="__user__.centroActivo"`).

### Paso 8 — Datos iniciales

No hay catálogos de negocio propios precargados (los datos maestros — centros, cursos, módulos, usuarios, tipos de usuario — los gestionan `common` y `sistemaeducativo`). El único contenido de `data-init/` es el fichero de **permisos** `auth-gruposnotas.xml` del Paso 7, con su `input-config.xml` (manifiesto de binding del `<permission>`), siguiendo `k-datainit`.

### Paso 9 — Verificación final

Compilar y confirmar que arranca sin errores:

```
./gradlew clean build --info
```

Comprobar: el build genera las clases de `db/` de las cuatro entidades; `ModelServiceFactory` resuelve los cuatro servicios; los menús abren las tres pantallas; `validate.sh` sobre `design/` da `VALIDACION-XML: OK`.

---

## Frontera de confianza — AllowProperties por acción

Acciones del servicio invocadas desde un `@CallMethod`: `GrupoServiceImpl.cerrarGrupo` y `GrupoServiceImpl.reabrirGrupo` (desde `GrupoController`). El resto de altas/bajas/modificaciones entran por el endpoint REST genérico (`save`/`save-modal`/`delete`/`delete-modal`) → `insert`/`update`/`remove`, cuyas `allowProperties*` también se documentan aquí porque definen la whitelist efectiva.

### `GrupoServiceImpl.insert` (vía REST genérico `save`)

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

### `GrupoServiceImpl.update` (vía REST genérico `save`)

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

### `GrupoServiceImpl.cerrarGrupo` (invocado desde `GrupoController.cerrarGrupo`)

Entidad: `Grupo`. **Forma elegida:** `createAllowProperties` (whitelist **vacía**).
**Origen spec:** acción `Cerrar` de `entity-Grupo.md` (sin `Input AllowProperties`).

| Campo         | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|---------------|----------|--------------|---------------------------------------------|
| (ninguno)     | —        | —            | La acción no acepta campos del cliente; `estado`=CERRADO y `fechaCierre`=now los pone `fireActionRule_Cerrar`. |

### `GrupoServiceImpl.reabrirGrupo` (invocado desde `GrupoController.reabrirGrupo`)

Entidad: `Grupo`. **Forma elegida:** `createAllowProperties` (whitelist **vacía**).
**Origen spec:** acción `Reabrir` de `entity-Grupo.md` (sin `Input AllowProperties`).

| Campo         | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|---------------|----------|--------------|---------------------------------------------|
| (ninguno)     | —        | —            | La acción no acepta campos del cliente; `estado`=ABIERTO y `fechaCierre`=null los pone `fireActionRule_Reabrir`. |

### `AlumnoGrupoServiceImpl.insert` (vía REST genérico `save-modal`)

Entidad: `AlumnoGrupo`. **Forma elegida:** `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-AlumnoGrupo.md`.

| Campo       | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|-------------|----------|--------------|---------------------------------------------|
| `grupo`     | cliente  | sí           | Padre del alta anidada (RUI-011/012). **Validado en `validateInsert`** (indicado, autorizado por centro, ABIERTO) — k-secure-coding §3.6. |
| `alumno`    | cliente  | sí           | Input directo del selector. |
| `notas`     | servidor | **NO**       | Creadas por el servidor (R-AlumnoGrupo-001/RN-005). |
| `notaMedia` | servidor | **NO**       | Transient derivado (CC-001), no persistido. |

### `AlumnoGrupoServiceImpl.update` (vía REST genérico)

Entidad: `AlumnoGrupo`. **Forma elegida:** `createAllowProperties` (whitelist **vacía**).
**Origen spec:** acción `Modificar` de `entity-AlumnoGrupo.md` (sin `Input AllowProperties`).

| Campo    | Origen   | En whitelist | Justificación |
|----------|----------|--------------|----------------|
| `grupo`  | cliente  | **NO**       | Inmutable (no se reparenta). |
| `alumno` | cliente  | **NO**       | Inmutable (se quita y se añade otro). |

### `NotaServiceImpl.update` (vía REST genérico `save-modal`)

Entidad: `Nota`. **Forma elegida:** `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-Nota.md`.

| Campo                     | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|---------------------------|----------|--------------|---------------------------------------------|
| `valor`                   | cliente  | sí           | Único campo editable. |
| `moduloGrupo`             | servidor | **NO**       | Inmutable; restaurado desde `original`. |
| `alumnoGrupo`             | servidor | **NO**       | Inmutable; restaurado desde `original`. |
| `fechaCalificacion`       | servidor | **NO**       | Asignada en `update` → `fireActionRule_AsignarFechaCalificacion` (CC-002). |
| `fechaUltimaModificacion` | servidor | **NO**       | Asignada en `update` → `fireActionRule_AsignarFechaUltimaModificacion` (CC-003). |

### DTOs de alta programática

- **`ModuloGrupoInsertDTO(Grupo grupo, Modulo modulo)`** — el DTO es la whitelist. `grupo` y `modulo` los aporta el servidor (`GrupoServiceImpl`), no el cliente. No hay campos `servidor` injustificados.
- **`NotaInsertDTO(ModuloGrupo moduloGrupo, AlumnoGrupo alumnoGrupo)`** — el DTO es la whitelist. Los aporta `AlumnoGrupoServiceImpl`. `valor` lo fija el servicio a NO_EVALUADO (no viene en el DTO). `fechaCalificacion`/`fechaUltimaModificacion` quedan nulas.

---

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (V)

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
| V-ModuloGrupo-001 | RES-003 | `domains/ModuloGrupo.xml` `unique-constraint(grupo,modulo)` |
| V-AlumnoGrupo-001 | RES-005 | `domains/AlumnoGrupo.xml` `unique-constraint(grupo,alumno)` (ESC-019) |
| V-AlumnoGrupo-002 | VAL-010 | `AlumnoGrupoServiceImpl.validateInsert` |
| V-AlumnoGrupo-003 | VAL-011 | `AlumnoGrupoServiceImpl.validateInsert` |
| V-AlumnoGrupo-004 | VAL-012 | `AlumnoGrupoServiceImpl.validateInsert` (consulta CentroUsuario/CentroUsuarioTipoUsuario) + cliente `domain` del selector (UX) |
| V-AlumnoGrupo-005 | VAL-013, RES-004 | `AlumnoGrupoServiceImpl.validateInsert` (`AlumnoGrupoRepository.findByAlumnoAndGrupoCursoAcademico`) |
| V-AlumnoGrupo-006 | VAL-014 | `AlumnoGrupoServiceImpl.validateRemove` |
| V-AlumnoGrupo-007 | VAL-018 | `AlumnoGrupoServiceImpl.validateInsert` |
| V-AlumnoGrupo-008 | VAL-019 | `AlumnoGrupoServiceImpl.validateInsert` (centro del grupo = centro activo del supervisor; k-secure-coding §3.6) |
| V-Nota-001 | RES-006 | `domains/Nota.xml` `unique-constraint(moduloGrupo,alumnoGrupo)` |
| V-Nota-002 | VAL-015 | `NotaServiceImpl.validateUpdate` |
| V-Nota-003 | VAL-016 | `NotaServiceImpl.validateUpdate` (dominio del valor) + cliente: enum `ValorNota` |
| V-Nota-004 | VAL-017 | `NotaServiceImpl.validateUpdate` (`NotaRepository.countMatriculasHonorByModuloGrupo`) |

### Reglas de negocio (R)

| R | Origen spec | Ubicación | Momento |
|---|-------------|-----------|---------|
| R-Grupo-001 | RN-001, RES-002 | `GrupoServiceImpl.fireActionRule_AsignarEstadoInicial` (Antes) + `fireActionRule_GenerarModulosGrupo` (Después de save) | Antes (estado) / Después (módulos) |
| R-Grupo-002 | RN-002 | `GrupoServiceImpl.fireActionRule_AsignarCentroYCursoAcademicoSiSupervisor` (asignación incondicional para supervisor) | Antes de save (insert) |
| R-Grupo-003 | RN-003 | `GrupoServiceImpl.fireActionRule_Cerrar` (estado=CERRADO, fechaCierre=now, incondicional) | Antes de save (cerrarGrupo) |
| R-Grupo-004 | RN-004 | `GrupoServiceImpl.fireActionRule_Reabrir` (estado=ABIERTO, fechaCierre=null, incondicional) | Antes de save (reabrirGrupo) |
| R-Grupo-005 | — | `GrupoServiceImpl.fireActionRule_RestaurarCamposInmutables` (defensa anti mass-assignment en update) | Antes de save (update) |
| R-AlumnoGrupo-001 | RN-005 | `AlumnoGrupoServiceImpl.fireActionRule_CrearNotasNoEvaluado` (crea una Nota NO_EVALUADO por módulo) | Después de save (insert) |
| R-Nota-001 | CC-002 | `NotaServiceImpl.fireActionRule_AsignarFechaCalificacion` (incondicional al pasar de NO_EVALUADO) | Antes de save (update) |
| R-Nota-002 | CC-003 | `NotaServiceImpl.fireActionRule_AsignarFechaUltimaModificacion` (incondicional al cambiar un valor ya calificado) | Antes de save (update) |
| R-Nota-003 | — | `NotaServiceImpl.fireActionRule_RestaurarCamposInmutables` (defensa anti mass-assignment en update) | Antes de save (update) |

### Campos calculados de solo lectura (CC momento lectura)

| Campo | Origen spec | Ubicación |
|-------|-------------|-----------|
| `AlumnoGrupo.notaMedia` | CC-001 | `domains/AlumnoGrupo.xml` (transient, getter computado **INLINE** sobre `..db..`, sin `Beans.get`/service). `AlumnoGrupoServiceImpl.calcularNotaMedia` **delega** en `getNotaMedia()` del dominio. |

### Reglas de UI (U)

| U | Origen spec | Ubicación |
|---|-------------|-----------|
| U-grupos-supervisor-001 | RUI-001 | `views/Grupo-Supervisor.xml` `onNew` → `set-defaults-action` (centro = centro activo) + `centro` readonly |
| U-grupos-supervisor-002 | RUI-002 | `views/Grupo-Supervisor.xml` `onNew` → `set-defaults-action` (cursoAcademico = centro activo) + `cursoAcademico` readonly |
| U-grupos-supervisor-003 | RUI-003 | `views/Grupo-Supervisor.xml` botón "Cerrar grupo" `showIf="estado == 'ABIERTO'"` (y ausencia de "Reabrir grupo" — ESC-011) |
| U-grupos-supervisor-004 | RUI-004 | `views/Grupo-Supervisor.xml` panel "Datos del grupo" `readonlyIf="estado == 'CERRADO'"` (y paneles related) |
| U-grupos-supervisor-005 | RUI-005 | `views/Grupo-Supervisor.xml` `Nota` form `valor` `readonlyIf="moduloGrupo.grupo.estado == 'CERRADO'"` |
| U-grupos-supervisor-006 | RUI-011 | `views/Grupo-Supervisor.xml` `AlumnoGrupo` `onNew` → `set-grupo-parent-action` (`grupo` = `__parent__`) |
| U-grupos-administrador-001 | RUI-006 | `views/Grupo-Administracion.xml` `centro`/`cursoAcademico` `readonlyIf="id != null"` (editables solo en alta) |
| U-grupos-administrador-002 | RUI-007 | `views/Grupo-Administracion.xml` botón "Cerrar grupo" `showIf="estado == 'ABIERTO'"` |
| U-grupos-administrador-003 | RUI-008 | `views/Grupo-Administracion.xml` botón "Reabrir grupo" `showIf="estado == 'CERRADO'"` |
| U-grupos-administrador-004 | RUI-009 | `views/Grupo-Administracion.xml` panel "Datos del grupo" `readonlyIf="estado == 'CERRADO'"` (botón Reabrir aparte) |
| U-grupos-administrador-005 | RUI-010 | `views/Grupo-Administracion.xml` `Nota` form `valor` `readonlyIf="moduloGrupo.grupo.estado == 'CERRADO'"` |
| U-grupos-administrador-006 | RUI-012 | `views/Grupo-Administracion.xml` `AlumnoGrupo` `onNew` → `set-grupo-parent-action` (`grupo` = `__parent__`) |

---

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline). Cubrirán: `GrupoServiceImpl` (validaciones VAL-001..009, R-Grupo-002 sobrescritura por supervisor, generación de módulos), `AlumnoGrupoServiceImpl` (VAL-010..014/018/019, creación de notas NO_EVALUADO, `calcularNotaMedia` con sus casos: media redondeada, MH=10, exclusión de NO_EVALUADO, "Sin nota"), `NotaServiceImpl` (VAL-015/016/017, fechas de calificación y última modificación).
- **Tests E2E**: descritos en `test-e2e-desc.md` (T-001..T-026), materializados desde los escenarios ESC-001..ESC-026 del spec.

---

## Reglas del spec descartadas

Ninguna regla `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN` del spec queda sin ubicar. Todas las reglas están mapeadas a una V/R/U o a un campo del modelo en las matrices anteriores.

(Se han añadido dos reglas técnicas con `Origen spec: —`, no provenientes del spec: **R-Grupo-005** y **R-Nota-003**, restauración de campos inmutables en `update` como defensa anti mass-assignment requerida por `k-secure-coding` §3.2.)

---

## Notas y supuestos

1. **`cursoAcademico` es un `integer`** (año de inicio, p.ej. 2024), coherente con `Centro.curso` (entero, "Curso académico"). En el spec "2024/2025" es una etiqueta; el diseño lo modela como el año de inicio. RES-001/VAL-003/VAL-005 (nombre único por centro+cursoAcademico) y RES-004/VAL-013 (un grupo por curso académico) usan ese entero. Para el supervisor, `cursoAcademico` se fija al `Centro.curso` de su centro activo (R-Grupo-002).
2. **`estado`** es un enum `EstadoGrupo {ABIERTO, CERRADO}`; **`fechaCierre`** es `datetime` servidor. Ningún estado es terminal.
3. **`valor` de Nota** se modela como un **enum `ValorNota`** con items `NO_EVALUADO`, `NOTA_1`..`NOTA_10`, `MATRICULA_HONOR`. Justificación: representa exactamente el dominio del spec (No evaluado / 1..10 / Matrícula de Honor) con un único campo, hace VAL-016 trivialmente cierto desde la UI (el selector solo ofrece valores válidos) pero mantiene VAL-016 como validación servidor (defensa ante un valor crudo por `/ws/rest`), y permite VAL-017 (contar `valor = 'MATRICULA_HONOR'`) y CC-001 (mapear `MATRICULA_HONOR`→10, `NOTA_n`→n, excluir `NO_EVALUADO`). La nota inicial es `NO_EVALUADO` (RN-005).
4. **`notaMedia` (CC-001, momento lectura)** es un campo **`transient`** de `AlumnoGrupo` (no se persiste): un getter computado del dominio calcula la media **INLINE**, recorriendo `this.getNotas()` y referenciando **solo** entidades/enums de `..db..` (`Nota`, `ValorNota`). **NO** usa `Beans.get` ni referencia a `..service..`, de modo que la entidad POJO no depende de la capa de servicio (cumple C13 "las entidades de dominio son POJOs" y C14 "`Beans.get` prohibido" de `k-archunit/secretaria-virtual-rules.md`). La relación se invierte respecto al delegado clásico: `AlumnoGrupoService.calcularNotaMedia(this)` **delega en `getNotaMedia()`** del dominio (única fuente de verdad), no al revés. Cálculo: media redondeada al entero más cercano de las notas evaluadas (MH=10, `NOTA_n`→`n` vía el ordinal del item del enum, con `NO_EVALUADO` en el ordinal 0), excluyendo las `NO_EVALUADO`; si no hay ninguna evaluada devuelve la cadena **"Sin nota"**. Al ser `String`, "Sin nota" se muestra tal cual y los valores numéricos como su texto.
5. **CC-002 (`fechaCalificacion`) y CC-003 (`fechaUltimaModificacion`)** son `datetime` servidor, asignadas en `NotaServiceImpl.update`: `fechaCalificacion` la primera vez que la nota deja de ser `NO_EVALUADO` (comparando contra `original`, leído del servidor); `fechaUltimaModificacion` cuando el valor cambia y la nota ya estaba calificada. La condición de transición se evalúa contra `original` (servidor), no contra la nulidad de un campo que el cliente pudiera rellenar; ambos campos están fuera de la whitelist de `update`, así que el cliente no los puede dictar (no es el anti-patrón `if (campo==null) set`).
6. **Clasificación cliente/servidor** (derivada de las líneas `Input AllowProperties` del spec): Grupo.Crear → nombre, curso, centro, cursoAcademico (cliente); Grupo.Modificar → nombre (cliente); AlumnoGrupo.Crear → alumno, grupo (cliente); Nota.Modificar → valor (cliente). **`alumnosGrupo`** aparece en `Input AllowProperties` de Grupo.Crear en el spec, pero el diseño lo EXCLUYE de la whitelist de `insert`: un o2m en la whitelist persistiría sus hijos `AlumnoGrupo` anidados a través de `GrupoServiceImpl.insert` (cascade), saltándose VAL-010..VAL-019 y RN-005 que solo aplica `AlumnoGrupoServiceImpl.insert` (k-secure-coding §3.6). Los alumnos se dan de alta exclusivamente por el modal "Añadir alumno" (RUI-011/012), nunca anidados en el alta del grupo. **RN-002**: para el supervisor, `centro` y `cursoAcademico` se SOBRESCRIBEN incondicionalmente con los del centro activo, ignorando el cliente; conviven en la whitelist de `insert` porque el **administrador** sí los aporta. El supervisor nunca los dicta (la sobrescritura los neutraliza).
7. **Selector de alumno (VAL-012, ESC-020):** en la pantalla del supervisor, el `domain` del campo `alumno` filtra a los usuarios de tipo ALUMNO del centro del supervisor conectado (subconsulta sobre `CentroUsuario`/`CentroUsuarioTipoUsuario`, sin parámetros con punto). Es UX; la defensa real es **V-AlumnoGrupo-004** en el servidor, que comprueba el centro del **grupo**. En la pantalla de administración el `domain` solo filtra por tipo ALUMNO (el admin abarca todos los centros) y el servidor valida el centro del grupo.
8. **Multicentro / acceso:** el supervisor solo ve los grupos de su centro (`<domain>self.centro = :centroActivoUsuario</domain>` con `<context>`); el alumno solo ve sus propios grupos (`<domain>self.alumno = :__user__</domain>`); los permisos `data-init/input/auth-gruposnotas.xml` lo refuerzan en servidor con `condition`/`conditionParams`. Se evita siempre `:__user__.campo` con punto (k-secure-coding §4).
9. **`orphanRemoval`** en las colecciones (`Grupo.modulosGrupo`, `Grupo.alumnosGrupo`, `ModuloGrupo.notas`, `AlumnoGrupo.notas`) materializa la composición del spec: al borrar el grupo se borran módulos, alumnos y notas; al quitar un alumno se borran sus notas (ESC-005).
10. **Ninguna regla cumple los criterios de "regla compleja"** de `reglas-complejas.md` (el cálculo de la media CC-001 y el tope de 3 MH son lógica acotada dentro del servicio, sin clases auxiliares, máquinas de estado ni integraciones externas). Por eso **no** se crea la carpeta `rules/`.
11. **Numeración V/R/U local y secuencial:** los identificadores `V`/`R`/`U` se numeran **localmente por entidad y por pantalla empezando en `001`** y de forma secuencial sin saltos (p.ej. `V-AlumnoGrupo-001..008`, `V-Nota-001..004`, `U-grupos-supervisor-001..006`, `U-grupos-administrador-001..006`), conforme a `design-contract.md` §2. La trazabilidad con el spec **no** depende del número del identificador: vive en la columna **`Origen spec`** de cada fila de la matriz (los IDs `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-NNN`). El prefijo `V-<Entidad>` / `R-<Entidad>` / `U-<slug-pantalla>` garantiza la unicidad global.
