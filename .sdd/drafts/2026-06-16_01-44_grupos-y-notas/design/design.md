---
type: design
---

# Diseño: Grupos y notas

**Objetivo:** Permitir a la secretaría de un centro definir grupos de alumnos ligados a un curso, registrar la nota final de cada alumno por módulo y consultar la nota media; el alumno consulta sus propias notas en solo lectura.
**Capa:** system/gruposnotas
**Especificación de origen:** .sdd/drafts/2026-06-16_01-44_grupos-y-notas/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-validaciones, k-code-quality, k-secure-coding, k-vistas

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/domains/Grupo.xml` | Crear | k-sistemas (modelos.md) | Entidad Grupo + enum EstadoGrupo |
| `system/gruposnotas/domains/ModuloGrupo.xml` | Crear | k-sistemas (modelos.md) | Entidad ModuloGrupo |
| `system/gruposnotas/domains/AlumnoGrupo.xml` | Crear | k-sistemas (modelos.md) | Entidad AlumnoGrupo + campo calculado `notaMedia` (CC-001, propiedad transient con cuerpo CDATA) |
| `system/gruposnotas/domains/Nota.xml` | Crear | k-sistemas (modelos.md) | Entidad Nota + enum ValorNota |
| `system/gruposnotas/service/GrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de Grupo |
| `system/gruposnotas/service/impl/GrupoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Validaciones, reglas y acciones de Grupo |
| `system/gruposnotas/service/ModuloGrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de ModuloGrupo |
| `system/gruposnotas/service/impl/ModuloGrupoServiceImpl.java` | Crear | k-sistemas, k-validaciones | Restricciones de ModuloGrupo |
| `system/gruposnotas/service/AlumnoGrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de AlumnoGrupo |
| `system/gruposnotas/service/impl/AlumnoGrupoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Validaciones, reglas y cálculo de media de AlumnoGrupo |
| `system/gruposnotas/service/NotaService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de Nota |
| `system/gruposnotas/service/impl/NotaServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Validaciones, fechas y acción guardarNota |
| `system/gruposnotas/db/repo/GrupoRepository.java` | Crear | k-sistemas (modelos.md) | Finder duplicado de nombre |
| `system/gruposnotas/db/repo/AlumnoGrupoRepository.java` | Crear | k-sistemas (modelos.md) | Finders de pertenencia |
| `system/gruposnotas/db/repo/NotaRepository.java` | Crear | k-sistemas (modelos.md) | Contador de matrículas de honor por módulo |
| `system/gruposnotas/controller/GrupoController.java` | Crear | k-sistemas (controladores.md), k-secure-coding | Botones "Cerrar grupo" y "Reabrir grupo" |
| `system/gruposnotas/controller/NotaController.java` | Crear | k-sistemas (controladores.md), k-secure-coding | Botón "Guardar" de la nota |
| `system/gruposnotas/views/Grupo-Supervisor.xml` | Crear | k-vistas | Pantalla "Grupos" (supervisor) |
| `system/gruposnotas/views/Grupo-Administracion.xml` | Crear | k-vistas | Pantalla "Grupos (administración)" |
| `system/gruposnotas/views/Grupo-MisNotas.xml` | Crear | k-vistas | Pantalla "Mis notas" (alumno) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menús "Notas → Grupos", "Mis notas" y "Administración → Grupos (administración)" |
| `system/gruposnotas/data-init/input/auth-gruposnotas.xml` (+ entrada en su `system/gruposnotas/data-init/input-config.xml`) | Crear/Modificar | k-secure-coding, k-datainit | Permisos del grupo `admins`/`users` sobre las 4 entidades (descrito en Seguridad) |

> **Repositorios personalizados**: como se crean `GrupoRepository`, `AlumnoGrupoRepository` y `NotaRepository` a mano en `db/repo/`, las entidades `Grupo`, `AlumnoGrupo` y `Nota` llevan **ya** `repository="abstract"` en su `<entity>` dentro de los `domains/*.xml` del diseño (XML declarativo completo, listo para copiar tal cual por `/sdd-implementer-system`, sin edición manual posterior). `ModuloGrupo` no tiene repo propio (sus finders/contadores viven en otras entidades), así que NO lleva `repository="abstract"`.

## Pasos

### Paso 1 — Dominios

Se crean cuatro ficheros de dominio en `system/gruposnotas/domains/` (módulo `gruposnotas`, paquete `com.educaflow.system.gruposnotas.db`). XML completo en `design_5/domains/`.

- **`Grupo.xml`** — Entidad `Grupo`. Campos: `nombre` (string, namecolumn, required), `curso` (many-to-one → `com.educaflow.subsystem.sistemaeducativo.db.Curso`, required), `cursoAcademico` (integer, required), `centro` (many-to-one → `com.educaflow.subsystem.common.db.Centro`, required), `estado` (enum `EstadoGrupo`, required), `fechaCierre` (datetime), `modulosGrupo` (one-to-many → ModuloGrupo, mappedBy="grupo"), `alumnosGrupo` (one-to-many → AlumnoGrupo, mappedBy="grupo"). `unique-constraint(nombre,centro,cursoAcademico)` (RES-001). `finder-method findByNombreCentroCursoAcademico`. Enum `EstadoGrupo { ABIERTO, CERRADO }`.
- **`ModuloGrupo.xml`** — Entidad `ModuloGrupo`. Campos: `grupo` (many-to-one → Grupo, required), `modulo` (many-to-one → `com.educaflow.subsystem.sistemaeducativo.db.Modulo`, required), `notas` (one-to-many → Nota, mappedBy="moduloGrupo"). `unique-constraint(grupo,modulo)` (RES-003).
- **`AlumnoGrupo.xml`** — Entidad `AlumnoGrupo`. Campos: `grupo` (many-to-one → Grupo, required), `alumno` (many-to-one → `com.axelor.auth.db.User`, required), `notas` (one-to-many → Nota, mappedBy="alumnoGrupo"), `centro` (many-to-one → Centro, **transient**, auxiliar UI para filtrar el selector de alumno). **CC-001 (nota media; momento lectura)**: se declara como **propiedad del dominio** — un `<string name="notaMedia" transient="true">` con **el algoritmo completo inline en el cuerpo CDATA del propio campo** (patrón de `Persona.nombreApellidos` / `Centro.administradores`, que computan inline sin utilidades externas). La entidad de dominio es un POJO y **NO** puede depender de `..service..` (C13 `entidadesDominioSonPojos`), por lo que el cálculo **no** se extrae a ninguna clase de `service.impl`: vive en el CDATA y solo referencia `Nota` y `ValorNota`, del mismo paquete `..db..`. Al ser una propiedad declarada, el `<field name="notaMedia"/>` de grids/forms la resuelve; `transient="true"` hace que no se persista y se calcule en memoria al leer, sin onLoad ni llamadas de servicio por fila. Axelor genera el getter desde el cuerpo CDATA: **MUST NOT** añadir además un getter manual del mismo nombre (no compilaría) ni dejarlo como getter suelto en `extra-code-model` (no quedaría registrado como propiedad). `unique-constraint(grupo,alumno)` (RES-005). `finder-method findByGrupo`.
- **`Nota.xml`** — Entidad `Nota`. Campos: `moduloGrupo` (many-to-one → ModuloGrupo, required), `alumnoGrupo` (many-to-one → AlumnoGrupo, required), `valor` (enum `ValorNota`, required), `fechaCalificacion` (datetime), `fechaUltimaModificacion` (datetime). `unique-constraint(moduloGrupo,alumnoGrupo)` (RES-006). El contador de matrículas de honor por módulo (V-Nota-003) **NO** es un `finder-method` (un finder de Axelor devuelve la primera entidad o un `Query`, nunca un `long`): se implementa como método propio de `NotaRepository` con `all().filter(...).bind(...).count()` (ver Paso 3). Enum `ValorNota { NO_EVALUADO, NOTA_01..NOTA_10, MATRICULA_HONOR }`.

**Verificar:** `bash .claude/skills/sdd-designer/template-system/validate.sh <design>` imprime `VALIDACION-XML: OK`.

### Paso 2 — Servicios

Todos extienden `ModelService`/`DefaultModelService` y los descubre `ModelServiceFactory` por convención (sin módulo Guice). Persisten siempre con `repository.save/remove`, **nunca** `super.*`. Las firmas y comentarios siguen; **no** se incluye cuerpo Java.

#### `com.educaflow.system.gruposnotas.service.GrupoService`

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

#### `com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl`

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

#### `com.educaflow.system.gruposnotas.service.ModuloGrupoService` / `…impl.ModuloGrupoServiceImpl`

```java
public interface ModuloGrupoService extends ModelService<ModuloGrupo> { }

public class ModuloGrupoServiceImpl extends DefaultModelService<ModuloGrupo> implements ModuloGrupoService {
    @Inject public ModuloGrupoServiceImpl(Class<ModuloGrupo> model, Repository<ModuloGrupo> repository) { super(model, repository); }

    @Override
    public Optional<BusinessMessages> validateInsert(ModuloGrupo moduloGrupo);
    //   Aplica:
    //     - V-ModuloGrupo-001 (Origen spec: RES-003) módulo no repetido en el grupo: respaldo de la
    //       unique-constraint(grupo,modulo); comprueba que no exista ya un ModuloGrupo con ese grupo y módulo.
    //       Mensaje transmite: el módulo ya está en el grupo.

    @Override
    public ModuloGrupo update(ModuloGrupo moduloGrupo, ModuloGrupo original);
    //   Defensa en profundidad (k-secure-coding §9.2): ModuloGrupo lo crea el sistema (R-Grupo-001) y NO es
    //   editable. Lanza UnsupportedOperationException incondicionalmente, además del allowPropertiesUpdate
    //   denyAll, para que la invariante de no-editabilidad sea un rechazo explícito de la operación.

    @Override public AllowProperties allowPropertiesInsert();   // denyAll: los crea el servidor (R-Grupo-001), el cliente no envía nada
    @Override public AllowProperties allowPropertiesUpdate();   // denyAll: ModuloGrupo no se edita
}
```

#### `com.educaflow.system.gruposnotas.service.AlumnoGrupoService` / `…impl.AlumnoGrupoServiceImpl`

```java
public interface AlumnoGrupoService extends ModelService<AlumnoGrupo> {
    String calcularNotaMedia(AlumnoGrupo alumnoGrupo);   // CC-001 (lectura)
}

public class AlumnoGrupoServiceImpl extends DefaultModelService<AlumnoGrupo> implements AlumnoGrupoService {

    @Inject AlumnoGrupoRepository alumnoGrupoRepository;
    @Inject NotaRepository notaRepository;
    @Inject ModelServiceFactory modelServiceFactory;   // para resolver NotaService al crear las notas

    @Inject public AlumnoGrupoServiceImpl(Class<AlumnoGrupo> model, Repository<AlumnoGrupo> repository) { super(model, repository); }

    @Override
    public AlumnoGrupo insert(AlumnoGrupo alumnoGrupo);
    //   0. fireActionRule_RestaurarGrupoDesdeContexto(alumnoGrupo)  → asigna `grupo` (campo servidor)
    //      desde la fuente de confianza (el grupo padre del request) ANTES de validar, porque
    //      `grupo` queda FUERA de la whitelist de allowPropertiesInsert (el cliente no lo dicta).
    //      Sin esto, validateInsert recibiría grupo=null y V-AlumnoGrupo-002 daría NPE.
    //   1. validateInsert(alumnoGrupo).ifPresent(throwIfInvalid)  → V-AlumnoGrupo-001..005.
    //   2. alumnoGrupo = repository.save(alumnoGrupo).
    //   3. fireActionRule_CrearNotasNoEvaluado(alumnoGrupo)  → R-AlumnoGrupo-001 (Después).

    @Override
    public Optional<BusinessMessages> validateInsert(AlumnoGrupo alumnoGrupo);
    //   Aplica:
    //     - V-AlumnoGrupo-001 (Origen spec: VAL-010) alumno indicado: alumno != null.
    //       Mensaje transmite: debe elegir un alumno.
    //     - V-AlumnoGrupo-002 (Origen spec: VAL-011) grupo ABIERTO: alumnoGrupo.getGrupo().getEstado()==ABIERTO.
    //       Mensaje transmite: no se pueden añadir alumnos a un grupo cerrado.
    //     - V-AlumnoGrupo-003 (Origen spec: VAL-012) alumno del centro y tipo Alumno: comprueba que el
    //       alumno es usuario del centro del grupo con tipoUsuario codigo 'ALUMNO' (consulta repo/relación
    //       CentroUsuario/CentroUsuarioTipoUsuario). Mensaje transmite: el alumno debe ser un usuario de tipo
    //       Alumno del centro del grupo. (Defensa de servidor del filtro de UI U-…-005.)
    //     - V-AlumnoGrupo-004 (Origen spec: VAL-013, RES-004) no pertenece a otro grupo del mismo curso
    //       académico: alumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico(alumno, centro,
    //       cursoAcademico, null). En el alta no hay id propio que excluir, por eso excludeAlumnoGrupoId=null.
    //       El centro y cursoAcademico se toman de alumnoGrupo.getGrupo() (grupo ya restaurado en servidor).
    //       Mensaje transmite: el alumno ya pertenece a otro grupo de este curso académico.
    //     - V-AlumnoGrupo-005 (Origen spec: RES-005) alumno no repetido en el grupo: respaldo de la
    //       unique-constraint(grupo,alumno). Mensaje transmite: el alumno ya está en el grupo.

    @Override
    public Optional<BusinessMessages> validateRemove(AlumnoGrupo alumnoGrupo);
    //   Aplica:
    //     - V-AlumnoGrupo-006 (Origen spec: VAL-014) grupo ABIERTO al quitar: estado==ABIERTO.
    //       Mensaje transmite: no se pueden quitar alumnos de un grupo cerrado.

    @Override
    public AlumnoGrupo update(AlumnoGrupo alumnoGrupo, AlumnoGrupo original);
    //   Defensa en profundidad (k-secure-coding §9.2): la pertenencia alumno↔grupo NO es editable (solo se
    //   crea o se quita). Lanza UnsupportedOperationException incondicionalmente, además del
    //   allowPropertiesUpdate denyAll, para convertir la invariante de no-editabilidad en rechazo explícito.

    private void fireActionRule_RestaurarGrupoDesdeContexto(AlumnoGrupo alumnoGrupo);
    //   Campo `grupo` clasificado SERVIDOR (k-secure-coding §3): el alumno se añade SIEMPRE desde el
    //   panel maestro-detalle "Alumnos" del formulario de grupo, así que el grupo padre es la fuente de
    //   confianza, NO un campo que el cliente pueda dictar. `grupo` está FUERA de la whitelist de
    //   allowPropertiesInsert, de modo que AllowProperties.filter lo elimina del bean entrante y aquí
    //   llega siempre como null. Asignación INCONDICIONAL desde el contexto del padre del request
    //   (el id del Grupo padre que Axelor incluye en el contexto maestro-detalle / `__parent__`),
    //   resolviendo el Grupo por id con grupoRepository.find(parentId). MUST NOT usar `if (grupo==null)`
    //   ni confiar en un `grupo` que venga en el JSON (k-secure-coding §3.3): por la vía REST genérica un
    //   atacante podría intentar apuntar la pertenencia a un grupo de otro centro (IDOR) — al restaurarlo
    //   siempre desde el padre se preserva la frontera multi-centro. El controlador del save-modal extrae
    //   el bean con allowPropertiesInsert() (whitelist: alumno) y el id del grupo padre del contexto; este
    //   método lo aplica antes de validateInsert para que V-AlumnoGrupo-002/004/etc. operen sobre el grupo
    //   correcto. No es compleja (resolución por id + set).

    private void fireActionRule_CrearNotasNoEvaluado(AlumnoGrupo alumnoGrupo);
    //   Aplica R-AlumnoGrupo-001 (Origen spec: RN-005). Momento: Después de repository.save. Por cada
    //   ModuloGrupo del grupo, crea una Nota {moduloGrupo, alumnoGrupo, valor=NO_EVALUADO} delegando en
    //   NotaService (resuelto por ModelServiceFactory). Efecto colateral: inserta N notas. No es compleja
    //   (bucle de 2-3 llamadas). El borrado en cascada de las notas al quitar el alumno lo gestiona el
    //   one-to-many/composición del modelo (orphanRemoval del mappedBy="alumnoGrupo").

    @Override
    public String calcularNotaMedia(AlumnoGrupo alumnoGrupo);
    //   Implementa CC-001 (Origen spec: CC-001; momento lectura) **delegando en el getter del dominio**:
    //   return alumnoGrupo.getNotaMedia(). El algoritmo (recorre las notas, excluye NO_EVALUADO; mapea
    //   NOTA_01..NOTA_10 a 1..10 y MATRICULA_HONOR a 10; "Sin nota" si no hay ninguna evaluada; en otro
    //   caso la media redondeada con Math.round como texto) vive ÚNICAMENTE en el cuerpo CDATA de la
    //   propiedad `notaMedia` del dominio (única fuente de verdad). El servicio NO reimplementa ni extrae
    //   el cálculo a una utilidad: solo delega, evitando duplicar la lógica y evitando que la entidad de
    //   dominio dependa de `..service..` (C13). Es campo derivado de SOLO LECTURA: no se persiste
    //   (propiedad transient `notaMedia`), por lo que el campo se rellena solo al renderizarse en grid/form
    //   sin onLoad ni llamada de servicio por fila. No es compleja (algoritmo aritmético simple).

    @Override public AllowProperties allowPropertiesInsert();   // whitelist: alumno (solo). grupo se restaura en servidor desde el contexto del padre (fireActionRule_RestaurarGrupoDesdeContexto); centro es transient auxiliar; notaMedia transient nunca entra
    @Override public AllowProperties allowPropertiesUpdate();   // denyAll: la pertenencia no se edita
    @Override public AllowProperties allowPropertiesRemove();   // denyAll
}
```

#### `com.educaflow.system.gruposnotas.service.NotaService` / `…impl.NotaServiceImpl`

```java
public interface NotaService extends ModelService<Nota> {
    Nota guardarNota(Nota nota, Nota notaOriginal);
    Optional<BusinessMessages> validateGuardarNota(Nota nota, Nota notaOriginal);
    AllowProperties allowPropertiesGuardarNota();
}

public class NotaServiceImpl extends DefaultModelService<Nota> implements NotaService {

    @Inject NotaRepository notaRepository;

    @Inject public NotaServiceImpl(Class<Nota> model, Repository<Nota> repository) { super(model, repository); }

    @Override
    public Optional<BusinessMessages> validateInsert(Nota nota);
    //   Aplica:
    //     - V-Nota-005 (Origen spec: RES-006) una nota por alumno+módulo: respaldo de la
    //       unique-constraint(moduloGrupo,alumnoGrupo). Mensaje transmite: ya existe nota para ese alumno y módulo.
    //   (El alta normal de notas la hace el servidor desde R-AlumnoGrupo-001 con valor NO_EVALUADO.)

    @Override
    public Nota guardarNota(Nota nota, Nota notaOriginal);
    //   1. validateGuardarNota(nota, notaOriginal).ifPresent(throwIfInvalid)  → V-Nota-001..003.
    //   2. fireActionRule_FijarFechasCalificacion(nota, notaOriginal)  → R-Nota-001/002 (Antes).
    //   3. return repository.save(nota).

    @Override
    public Optional<BusinessMessages> validateGuardarNota(Nota nota, Nota notaOriginal);
    //   Aplica:
    //     - V-Nota-001 (Origen spec: VAL-016) valor en dominio: comprueba que `valor` es uno de
    //       {NO_EVALUADO, NOTA_01..NOTA_10, MATRICULA_HONOR} (el enum lo garantiza, pero se valida en
    //       servidor para la vía REST). Mensaje transmite: la nota debe ser No evaluado, un número entero
    //       del 1 al 10 o Matrícula de Honor.
    //     - V-Nota-002 (Origen spec: VAL-015) grupo ABIERTO: nota.getModuloGrupo().getGrupo().getEstado()==ABIERTO.
    //       Mensaje transmite: no se pueden modificar las notas de un grupo cerrado.
    //     - V-Nota-003 (Origen spec: VAL-017) máx 3 matrículas por módulo: si el valor que se pone es
    //       MATRICULA_HONOR y notaOriginal.valor != MATRICULA_HONOR, comprueba que
    //       notaRepository.countMatriculasHonorByModuloGrupo(moduloGrupo) < 3. Mensaje transmite: no se pueden
    //       poner más de 3 matrículas de honor en un módulo.

    private void fireActionRule_FijarFechasCalificacion(Nota nota, Nota notaOriginal);
    //   Aplica R-Nota-001 (Origen spec: CC-002; campo `fechaCalificacion` servidor) y R-Nota-002
    //   (Origen spec: CC-003; campo `fechaUltimaModificacion` servidor). Momento: Antes de save.
    //   Lógica:
    //     - Si notaOriginal.valor == NO_EVALUADO && nota.valor != NO_EVALUADO  → primera calificación:
    //       asignación INCONDICIONAL nota.setFechaCalificacion(LocalDateTime.now()).
    //     - Si notaOriginal.valor != NO_EVALUADO && el valor cambia  → modificación posterior:
    //       asignación INCONDICIONAL nota.setFechaUltimaModificacion(LocalDateTime.now()).
    //   En todo caso se restauran desde notaOriginal las fechas que esta rama no toca, para que el cliente
    //   no las pueda dictar. MUST NOT usar `if (campo==null) set(...)` (k-secure-coding §3.3). Estos dos
    //   campos quedan FUERA de la whitelist de guardarNota (solo `valor` es cliente).

    @Override public AllowProperties allowPropertiesInsert();        // denyAll: las notas las crea el servidor (R-AlumnoGrupo-001)
    @Override public AllowProperties allowPropertiesUpdate();        // denyAll: la nota se cambia por guardarNota, no por update genérico
    @Override public AllowProperties allowPropertiesRemove();        // denyAll
    @Override public AllowProperties allowPropertiesGuardarNota();   // whitelist: valor (solo)
}
```

### Paso 3 — Repositorios

Repositorios personalizados en `system/gruposnotas/db/repo/` (las consultas JPA viven aquí, nunca inline en el servicio — k-sistemas). Heredan de la clase abstracta generada por Axelor (las entidades deben declarar `repository="abstract"`).

```java
// com.educaflow.system.gruposnotas.db.repo.GrupoRepository extends AbstractGrupoRepository
public Grupo findByNombreCentroCursoAcademico(String nombre, Centro centro, Integer cursoAcademico);
//   Generado por el finder-method del dominio. Devuelve el grupo homónimo en el centro+cursoAcademico o null.
//   Lo usan V-Grupo-003 (alta) y V-Grupo-005 (modificación, que además excluye el propio id).

// com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository extends AbstractAlumnoGrupoRepository
public boolean existsOtroGrupoMismoCursoAcademico(User alumno, Centro centro, Integer cursoAcademico, Long excludeAlumnoGrupoId);
//   Cuenta los AlumnoGrupo del alumno cuyos grupos tienen el mismo centro+cursoAcademico, excluyendo el
//   registro con id = excludeAlumnoGrupoId cuando se pasa (si excludeAlumnoGrupoId es null, no excluye
//   ninguno: ese es el caso del alta, donde la pertenencia aún no tiene id). Filtro JPQL con :param y bind
//   (k-secure-coding §5). Lo usa V-AlumnoGrupo-004 (RES-004), que en el alta lo invoca con null.

// com.educaflow.system.gruposnotas.db.repo.NotaRepository extends AbstractNotaRepository
public long countMatriculasHonorByModuloGrupo(ModuloGrupo moduloGrupo);
//   Método propio del repositorio (NO finder-method: un finder devuelve la primera entidad o un Query,
//   nunca un long). Cuenta las Notas con valor MATRICULA_HONOR del módulo (moduloGrupo) dado: filtra
//   por el módulo y por el enum ValorNota.MATRICULA_HONOR (se compara contra el enum, no contra el
//   literal), usando filtro con :param y bind, y termina la consulta del repositorio en .count().
//   Filtro JPQL con :param y bind (k-secure-coding §5). Lo usa V-Nota-003 (VAL-017).
```

`ModuloGrupo` no tiene repositorio personalizado; sus inserciones las hace `GrupoServiceImpl` a través del `Repository<ModuloGrupo>` genérico.

### Paso 4 — Controladores

#### `com.educaflow.system.gruposnotas.controller.GrupoController`

```java
public class GrupoController {

    @Inject private ModelServiceFactory modelServiceFactory;

    @CallMethod @Transactional
    public void cerrar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delega en GrupoService.cerrar. Extrae el bean con allowPropertiesCerrar() (denyAll) y el original.
    //   Patrón TareaFirmaController: getOriginalModel() + getModel(allowProperties).

    @CallMethod @Transactional
    public void reabrir(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delega en GrupoService.reabrir. Extrae el bean con allowPropertiesReabrir() (denyAll) y el original.
    //   La V-Grupo-008 (solo administrador) se valida en el servicio, no solo aquí.
}
```

#### `com.educaflow.system.gruposnotas.controller.NotaController`

```java
public class NotaController {

    @Inject private ModelServiceFactory modelServiceFactory;

    @CallMethod @Transactional
    public void guardarNota(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delega en NotaService.guardarNota. Extrae el bean con allowPropertiesGuardarNota() (whitelist: valor)
    //   y el original con getOriginalModel(). Parámetros nombrados actionRequest/actionResponse (k-sistemas).
}
```

### Paso 5 — Vistas

Un `<action-view>` por fichero (k-sistemas/k-vistas). Prefijo `sysGruposNotas`. XML completo en `design_5/views/`.

- **`Grupo-Supervisor.xml`** — `action-view sysGruposNotas.Grupo@Supervisor-action` (pantalla "Grupos" del supervisor). Lleva `<domain>self.centro = :centroActivoUsuario</domain>` + `<context>` con `__user__?.centroActivo?.id` (multi-centro, k-secure-coding §4). Contiene: grid de grupos (columnas curso académico, ciclo, nombre, estado); form de grupo con paneles "Datos del grupo", "Módulos" (panel-related → ModuloGrupo), "Alumnos" (panel-related → AlumnoGrupo); botones "Cerrar grupo" (showIf ABIERTO → action-method `cerrar`), "Guardar", "Cancelar"; grids/forms anidados ModuloGrupo → Nota (botón "Guardar" → action-method `guardarNota`) y AlumnoGrupo (selector de alumno con `domain` filtrado por centro y tipo ALUMNO). `onNew` fija estado ABIERTO y, además, `centro = __user__?.centroActivo` y `cursoAcademico = __user__?.centroActivo?.curso` (RUI-001/RUI-002: rellenos y readonly en el alta; UX, el servidor los fija incondicionalmente vía R-Grupo-002, y además alimenta el selector de alumno del panel Alumnos vía `__parent__?.centro`); en AlumnoGrupo, el `onNew` fija el grupo padre y el centro auxiliar.
- **`Grupo-Administracion.xml`** — `action-view sysGruposNotas.Grupo@Administracion-action` (pantalla "Grupos (administración)"). Sin `<domain>` de centro (el administrador ve todos). Añade columna y campos editables `centro` y `cursoAcademico` en el alta (readonlyIf id!=null). Botón adicional "Reabrir grupo" (showIf CERRADO → action-method `reabrir`). Resto análogo al supervisor con sufijo `@Administracion`.
- **`Grupo-MisNotas.xml`** — `action-view sysGruposNotas.AlumnoGrupo@MisNotas-action` (pantalla "Mis notas" del alumno). Lleva `<domain>self.alumno = :__user__</domain>` (el alumno solo ve sus pertenencias). Grid de mis grupos (curso académico, ciclo, nombre, nota media); form "Mi grupo" (solo lectura) con panel "Mis notas" (panel-related → Nota, solo lectura); grid/form de mi nota (módulo, valor, fechas), todo readonly.

**Verificar:** `validate.sh` imprime `VALIDACION-XML: OK`.

### Paso 6 — Módulos Guice

No aplica: las cuatro entidades tienen servicio `ModelService`/`DefaultModelService` descubierto por `ModelServiceFactory`. **MUST NOT** crear módulo Guice (k-sistemas §module). Los controladores y servicios inyectan repositorios y `ModelServiceFactory`, todos disponibles sin binding manual.

### Paso 7 — Jobs programados

No aplica (no hay tarea recurrente).

### Paso 8 — Menús

Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` añadiendo la porción de `design_5/menus.xml`:

- `notas-menuitem` "Notas" (order 35, `if` tipo activo SUPERVISOR) → hijo "Grupos" → `sysGruposNotas.Grupo@Supervisor-action`.
- `misNotas-menuitem` "Mis notas" (order 36, `if` tipo activo ALUMNO) → `sysGruposNotas.AlumnoGrupo@MisNotas-action`.
- `administracionSv-grupos-menuitem` "Grupos (administración)" bajo `administracionSv-menuitem` (groups="admins", order 20) → `sysGruposNotas.Grupo@Administracion-action`.

La visibilidad de menú es solo conveniencia: la frontera real la imponen el `<domain>` de las action-view, los `allowProperties` y las validaciones de servidor.

### Paso 9 — Seguridad

Modelo de roles del proyecto (código real en `subsystem/security`, NO k-seguridad): grupos `admins` y `users`, y tipos de usuario (`TipoUsuario.codigo`: SUPERVISOR, ALUMNO, …). Reglas de acceso, en lenguaje natural:

- **Administrador** (grupo `admins`): permisos CREATE/READ/WRITE/REMOVE sobre `Grupo`, `ModuloGrupo`, `AlumnoGrupo`, `Nota` sin restricción de centro. Ve "Grupos (administración)" y puede reabrir (V-Grupo-008 lo confirma en servidor).
- **Supervisor** (tipo activo SUPERVISOR, grupo `users`): permisos CREATE/READ/WRITE/REMOVE sobre las 4 entidades **restringidos a su centro**; la restricción operativa la imponen el `<domain>` de la action-view del supervisor y las reglas de servidor (R-Grupo-002 fija centro/cursoAcademico; nunca puede reabrir). No ve "Grupos (administración)".
- **Alumno** (tipo activo ALUMNO, grupo `users`): permiso **solo READ** sobre `AlumnoGrupo` y `Nota` (y lectura de `Grupo`/`ModuloGrupo` para mostrar nombres), restringido a sus propias pertenencias por el `<domain>self.alumno = :__user__`. No crea ni modifica nada.

Materialización: fichero `system/gruposnotas/data-init/input/auth-gruposnotas.xml` con los permisos (`Permission`) y su asignación a los grupos `admins`/`users` siguiendo el patrón de los `auth-<sistema>.xml` existentes (p.ej. `subsystem/security/data-init/input/auth-security.xml`, `subsystem/expedientes/data-init/input/auth-expedientes.xml`), más la entrada en `system/gruposnotas/data-init/input-config.xml`. Cada sistema/subsistema lleva su seguridad en su propia carpeta `data-init` (ver `k-datainit`), no en el `data-init` global. El control fino por centro y por propiedad lo dan el diseño de servicio/vista descrito arriba (k-secure-coding), no un ACL por campo.

### Paso 10 — Datos iniciales

No se precargan catálogos propios del sistema (Grupo/ModuloGrupo/AlumnoGrupo/Nota son datos de explotación, no maestros). Los datos maestros (centros, catálogo educativo, usuarios y sus tipos) ya los gestionan `common` y `sistemaeducativo`; el tipo `SUPERVISOR` ya existe en `subsystem/common/data-init/input/tiposUsuario.xml`. Solo se añaden los permisos del Paso 9.

### Paso 11 — Verificación final

Compilar el proyecto y confirmar que arranca sin errores:

```
./gradlew clean build --info
```

Comprobar que las 4 entidades generan sus clases en `build/src-gen`, que `ModelServiceFactory` resuelve los 4 servicios y que las 3 action-view aparecen en sus menús.

## Frontera de confianza — AllowProperties por acción

Tablas por cada acción de servicio invocada desde un `@CallMethod`. Reglas de validez en k-secure-coding §3.

### `GrupoServiceImpl.insert` (invocado desde el botón "Guardar" genérico de la vista, vía `save`)

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

### `GrupoServiceImpl.update` (invocado desde "Guardar" genérico, vía `save`)

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

### `GrupoServiceImpl.cerrar` (invocado desde `GrupoController.cerrar`)

Entidad: `Grupo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** acción `Cerrar` de `entity-Grupo.md` (sin `Input AllowProperties`: el cliente no aporta ningún campo).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `estado` | servidor | **NO** | `fireActionRule_RegistrarCierre` → CERRADO (incondicional). |
| `fechaCierre` | servidor | **NO** | `fireActionRule_RegistrarCierre` → `LocalDateTime.now()` (incondicional). |
| (resto) | servidor | **NO** | El cierre no acepta ningún campo del cliente; se opera sobre el grupo por id. |

### `GrupoServiceImpl.reabrir` (invocado desde `GrupoController.reabrir`)

Entidad: `Grupo`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** acción `Reabrir` de `entity-Grupo.md` (sin `Input AllowProperties`).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `estado` | servidor | **NO** | `fireActionRule_RegistrarReapertura` → ABIERTO (incondicional). |
| `fechaCierre` | servidor | **NO** | `fireActionRule_RegistrarReapertura` → `null` (incondicional). |
| (resto) | servidor | **NO** | La reapertura no acepta campos del cliente. V-Grupo-008 exige rol admin en servidor. |

### `AlumnoGrupoServiceImpl.insert` (invocado desde "Guardar/Añadir alumno" genérico, vía `save-modal`)

Entidad: `AlumnoGrupo`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-AlumnoGrupo.md` (alumno).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `alumno` | cliente | sí | Input directo (elegido en el selector filtrado). Validado por V-AlumnoGrupo-003. |
| `grupo` | servidor | **NO** | Fuera de la whitelist: el cliente NO lo dicta. Se asigna INCONDICIONALMENTE en `insert` → `fireActionRule_RestaurarGrupoDesdeContexto` desde el Grupo padre del contexto del request (`__parent__` id), antes de `validateInsert`. El `onNew` (`grupo = __parent__`) es solo UX; el servidor lo restaura siempre, de modo que el cliente no puede reapuntar la pertenencia a otro grupo (defensa IDOR multi-centro, k-secure-coding §3.3/§4). |
| `centro` | servidor | **NO** | Campo transient auxiliar de UI; nunca se persiste ni lo dicta el cliente. |
| `notaMedia` | servidor | **NO** | Campo transient calculado (CC-001); fuera de la whitelist. |

### `NotaServiceImpl.guardarNota` (invocado desde `NotaController.guardarNota`)

Entidad: `Nota`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-Nota.md` (valor).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `valor` | cliente | sí | Único campo que el usuario edita. Validado por V-Nota-001/003. |
| `moduloGrupo` | servidor | **NO** | Inmutable; se opera sobre la nota por id, se restaura desde `original`. |
| `alumnoGrupo` | servidor | **NO** | Inmutable; restaurado desde `original`. |
| `fechaCalificacion` | servidor | **NO** | `fireActionRule_FijarFechasCalificacion` (incondicional, R-Nota-001). |
| `fechaUltimaModificacion` | servidor | **NO** | `fireActionRule_FijarFechasCalificacion` (incondicional, R-Nota-002). |

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (V)

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
| V-ModuloGrupo-001 | RES-003 | ModuloGrupoServiceImpl.validateInsert (+ unique-constraint en ModuloGrupo.xml) |
| V-AlumnoGrupo-001 | VAL-010 | AlumnoGrupoServiceImpl.validateInsert |
| V-AlumnoGrupo-002 | VAL-011 | AlumnoGrupoServiceImpl.validateInsert |
| V-AlumnoGrupo-003 | VAL-012 | AlumnoGrupoServiceImpl.validateInsert |
| V-AlumnoGrupo-004 | VAL-013, RES-004 | AlumnoGrupoServiceImpl.validateInsert (+ AlumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico) |
| V-AlumnoGrupo-005 | RES-005 | AlumnoGrupoServiceImpl.validateInsert (+ unique-constraint en AlumnoGrupo.xml) |
| V-AlumnoGrupo-006 | VAL-014 | AlumnoGrupoServiceImpl.validateRemove |
| V-Nota-001 | VAL-016 | NotaServiceImpl.validateGuardarNota (+ enum ValorNota en Nota.xml) |
| V-Nota-002 | VAL-015 | NotaServiceImpl.validateGuardarNota |
| V-Nota-003 | VAL-017 | NotaServiceImpl.validateGuardarNota (+ NotaRepository.countMatriculasHonorByModuloGrupo) |
| V-Nota-005 | RES-006 | NotaServiceImpl.validateInsert (+ unique-constraint en Nota.xml) |

### Reglas de negocio (R)

| R | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Grupo-001 | RN-001, RES-002 | GrupoServiceImpl.fireActionRule_CrearModulosGrupo | Después de save |
| R-Grupo-002 | RN-002 | GrupoServiceImpl.fireActionRule_FijarCentroYCursoAcademicoSiSupervisor | Antes de save |
| R-Grupo-003 | RN-003 | GrupoServiceImpl.fireActionRule_RegistrarCierre | Antes de save (en `cerrar`) |
| R-Grupo-004 | RN-004 | GrupoServiceImpl.fireActionRule_RegistrarReapertura | Antes de save (en `reabrir`) |
| R-AlumnoGrupo-001 | RN-005 | AlumnoGrupoServiceImpl.fireActionRule_CrearNotasNoEvaluado | Después de save |
| R-AlumnoGrupo-002 | — (campo `grupo` servidor; derivado de la frontera de confianza de la acción `Crear`) | AlumnoGrupoServiceImpl.fireActionRule_RestaurarGrupoDesdeContexto | Antes de validar |
| R-Nota-001 | CC-002 | NotaServiceImpl.fireActionRule_FijarFechasCalificacion (campo `fechaCalificacion`) | Antes de save (en `guardarNota`) |
| R-Nota-002 | CC-003 | NotaServiceImpl.fireActionRule_FijarFechasCalificacion (campo `fechaUltimaModificacion`) | Antes de save (en `guardarNota`) |

> Nota de numeración: en `Nota` el cálculo de CC-002/CC-003 lo hace un único método; se han numerado R-Nota-001 (fechaCalificacion/CC-002) y R-Nota-002 (fechaUltimaModificacion/CC-003) por el campo servidor que cada una asegura, aunque comparten método host. En la firma del método se indica que implementa ambas.

#### Campos calculados de solo lectura (no son R)

`CC-001` (nota media; `momento: lectura`) **no** se mapea a una regla de negocio `R-`: según `design-contract.md` §2/§3, un `CC-NNN` de momento lectura es un **campo derivado de solo lectura del modelo**, no una R. Se cubre con la propiedad transient `notaMedia` de `AlumnoGrupo` (`<string name="notaMedia" transient="true">` con el algoritmo completo inline en el cuerpo CDATA, paquete `..db..`), renderizada como `<field name="notaMedia"/>` en los grids/forms; `AlumnoGrupoServiceImpl.calcularNotaMedia` se limita a delegar en el getter `getNotaMedia()`. Ver el detalle en «Notas y supuestos → CC-001». No lleva identificador `R-` (en particular **no** reutiliza `R-AlumnoGrupo-001`, que designa la R de RN-005, `fireActionRule_CrearNotasNoEvaluado`).

### Reglas de UI (U)

| U | Origen spec | Ubicación |
|---|---|---|
| U-grupos-supervisor-001 | RUI-001 | Grupo-Supervisor.xml: campo `centro` readonly + onNew `set-centro-curso-academico-action` (`centro = eval: __user__?.centroActivo`) que lo rellena en el alta (UX; el servidor lo fija incondicionalmente, R-Grupo-002) |
| U-grupos-supervisor-002 | RUI-002 | Grupo-Supervisor.xml: campo `cursoAcademico` readonly + onNew `set-centro-curso-academico-action` (`cursoAcademico = eval: __user__?.centroActivo?.curso`) que lo rellena en el alta (UX; el servidor lo fija incondicionalmente, R-Grupo-002) |
| U-grupos-supervisor-003 | RUI-003 | Grupo-Supervisor.xml: botón "Cerrar grupo" `showIf="estado == 'ABIERTO' && id != null"` (y ausencia de "Reabrir grupo") |
| U-grupos-supervisor-004 | RUI-004 | Grupo-Supervisor.xml: panel "Datos del grupo" y panel "Alumnos" `readonlyIf="estado == 'CERRADO'"`; valor de nota `readonlyIf` grupo CERRADO |
| U-grupos-supervisor-005 | — | Grupo-Supervisor.xml: campo `alumno` con `domain` (alumnos del centro del grupo, tipo ALUMNO). U añadida por el diseño (filtro del selector); no proviene de ninguna RUI. Su respaldo de servidor es VAL-012 (V-AlumnoGrupo-003), que materializa ESC-020 |
| U-grupos-administracion-001 | RUI-006 | Grupo-Administracion.xml: campos `centro` y `cursoAcademico` editables en alta (`readonlyIf="id != null"`) |
| U-grupos-administracion-002 | RUI-008 | Grupo-Administracion.xml: botón "Reabrir grupo" `showIf="estado == 'CERRADO'"` |
| U-grupos-administracion-003 | RUI-007 | Grupo-Administracion.xml: botón "Cerrar grupo" `showIf="estado == 'ABIERTO' && id != null"` |
| U-grupos-administracion-004 | RUI-009 | Grupo-Administracion.xml: paneles `readonlyIf="estado == 'CERRADO'"` (salvo "Reabrir grupo") |
| U-grupos-supervisor-006 | RUI-005 | Grupo-Supervisor.xml: campo `valor` de la nota `readonlyIf="moduloGrupo.grupo.estado == 'CERRADO'"` |
| U-grupos-administracion-005 | RUI-010 | Grupo-Administracion.xml: campo `valor` de la nota `readonlyIf="moduloGrupo.grupo.estado == 'CERRADO'"` |
| U-mis-notas-alumno | (acceso de rol; sin RUI propia) | Grupo-MisNotas.xml: action-view con `<domain>self.alumno = :__user__</domain>` y todos los paneles readonly |

> `U-grupos-supervisor-005` la **añadió el diseño** por necesidad técnica (filtrar el selector de alumno por centro y tipo ALUMNO, que materializa ESC-020): no proviene de ninguna RUI del spec, por lo que su `Origen spec` es `—`. Su respaldo de negocio es la VAL-012 (V-AlumnoGrupo-003), que es la defensa de servidor. La pantalla "Mis notas" no define RUI; su comportamiento de solo-su-grupo/solo-lectura es alcance de rol (Seguridad), materializado con el `<domain>` y los `readonly`.

## Reglas del spec descartadas

Ninguna. Todas las reglas del spec (`RES-001..006`, `VAL-001..017`, `RN-001..005`, `RUI-001..010`, `CC-001..003`) están mapeadas a una V/R/U ubicada según las tablas de trazabilidad.

## Notas y supuestos

- **Es un sistema** (`com.educaflow.system.gruposnotas`), no un subsistema: nadie depende de él (CLAUDE.md). Carpetas `domains/`, `service/`, `service/impl/`, `controller/`, `db/repo/`, `views/`.
- **Curso académico = Integer.** El modelo real no tiene entidad CursoAcademico: el curso académico del centro es `Centro.curso` (Integer, p.ej. 2024). Por tanto `Grupo.cursoAcademico` es un `Integer` alineado con `Centro.curso`. El spec habla de «2024/2025»; en el modelo se guarda el entero del año de inicio (2024). En las pantallas/tests se muestra/usa el entero.
- **Representación del valor de la nota.** Se eligió un `<enum name="ValorNota">` con items `NO_EVALUADO`, `NOTA_01`..`NOTA_10` (títulos "1".."10") y `MATRICULA_HONOR`, en lugar de un selection-string o un numérico+flag. Razones: (a) sigue la convención del proyecto (los estados/dominios cerrados se modelan con `<enum>`, p.ej. `EstadoTareaFirma`); (b) restringe el dominio a nivel de modelo, reforzando VAL-016; (c) el cálculo de la media mapea el enum a int (NOTA_0n→n, MATRICULA_HONOR→10, NO_EVALUADO excluido) de forma trivial. Los items numéricos llevan prefijo `NOTA_` porque los nombres de item de enum deben ser identificadores Java válidos (no pueden empezar por dígito).
- **CC-001 (nota media), momento lectura.** Se modela como una **propiedad del dominio**: un `<string name="notaMedia" transient="true">` con **cuerpo CDATA dentro del propio campo** (patrón de `Persona.nombreApellidos` / `Centro.administradores`). Al ser una propiedad declarada, el `<field name="notaMedia"/>` de grids/forms la resuelve (un getter suelto en `extra-code-model` NO se registra como propiedad → columna en blanco / fallo de meta-vista). `transient="true"` => no se persiste y se calcula en memoria al leer. Axelor genera el getter a partir del cuerpo CDATA: **MUST NOT** declarar a la vez el campo y un getter manual del mismo nombre (no compilaría). El cuerpo devuelve `String` (para poder devolver "Sin nota" o el entero) **calculando** la media con el algoritmo completo inline en el propio CDATA (única fuente de verdad), referenciando solo `Nota` y `ValorNota` del paquete `..db..`; **no** se extrae a ninguna clase de `service.impl` porque la entidad de dominio es un POJO y no puede depender de `..service..` (C13 `entidadesDominioSonPojos`). El `AlumnoGrupoServiceImpl.calcularNotaMedia` **delega en el getter** `getNotaMedia()`, no reimplementa el cálculo. NO es campo cliente. Como computa al leer, el campo `<field name="notaMedia"/>` se renderiza solo en los grids/forms (sin onLoad ni llamada de servicio por fila), cumpliendo ESC-007 (media 9 con una MH), ESC-008 ("Sin nota" sin evaluadas) y ESC-014 (media 7). El cálculo: excluye NO_EVALUADO, MATRICULA_HONOR=10, media redondeada al entero más cercano (Math.round), "Sin nota" si no hay ninguna evaluada.
- **CC-002/CC-003 (fechas de la nota), momento escritura.** Campos `servidor` asignados INCONDICIONALMENTE en `NotaServiceImpl.guardarNota` → `fireActionRule_FijarFechasCalificacion`, fuera de la whitelist (solo `valor` es cliente). Defensa anti mass-assignment (k-secure-coding §3.3): sin `if (campo==null)`.
- **Multi-centro / IDOR.** La pantalla del supervisor filtra por `<domain>self.centro = :centroActivoUsuario</domain>` (centro activo del servidor); al crear, el supervisor recibe centro y cursoAcademico fijados por el servidor (R-Grupo-002). El administrador los elige (campos cliente). El alumno solo ve `self.alumno = :__user__`. El selector de alumno se filtra por centro del grupo y tipo ALUMNO (UI) con respaldo de servidor V-AlumnoGrupo-003. Al añadir un alumno, el `grupo` de la pertenencia NO lo dicta el cliente: queda fuera de la whitelist y se restaura en servidor desde el Grupo padre del contexto (R-AlumnoGrupo-002), evitando que por la vía REST genérica se apunte la pertenencia a un grupo de otro centro.
- **`grupo` de AlumnoGrupo es campo servidor (regla añadida por el diseño).** R-AlumnoGrupo-002 (`fireActionRule_RestaurarGrupoDesdeContexto`) no proviene de ninguna regla del spec: la añade el diseño porque la frontera de confianza de la acción `Crear` deja `grupo` fuera de la whitelist (`Input AllowProperties` = solo alumno) y, sin asignarlo en servidor, las validaciones que navegan el grupo (V-AlumnoGrupo-002 grupo ABIERTO, V-AlumnoGrupo-004 mismo curso académico) recibirían `grupo=null` y darían NPE. Es análoga a U-grupos-supervisor-005 (añadida por necesidad técnica). No se descarta ninguna regla del spec.
- **Composición / borrado en cascada.** Modelado **en los `domains/*.xml`** del diseño (el `cascade`/`orphanRemoval` es atributo de dominio en este proyecto, p.ej. `tiposexpedientes/prueba/domains.xml`), no diferido a la implementación. Propietarios del ciclo de vida y borrado en cascada:
  - `Grupo.modulosGrupo` y `Grupo.alumnosGrupo` → `cascade="all" orphanRemoval="true"`: al borrar el Grupo se borran sus ModuloGrupo y AlumnoGrupo.
  - `AlumnoGrupo.notas` → `cascade="all" orphanRemoval="true"`: AlumnoGrupo es el **padre propietario** de la Nota; al quitar un alumno del grupo se borran sus Notas (ESC-005). Por la cadena Grupo→AlumnoGrupo→Nota, al borrar el Grupo también se borran las Notas.
  - `ModuloGrupo.notas` → **navegación inversa SOLO** (sin cascade/orphanRemoval): la Nota tiene un único padre propietario (AlumnoGrupo) para no declarar dos padres con `orphanRemoval` sobre la misma Nota (borrado contradictorio). Un ModuloGrupo solo se elimina al borrar su Grupo, y en ese caso sus Notas ya se borran por la cadena Grupo→AlumnoGrupo→Nota.
- **Reglas complejas.** Ninguna `R-` cumple los criterios de complejidad (R-Grupo-001 y R-AlumnoGrupo-001 son bucles de 2-3 llamadas; el cálculo de la media es aritmético simple; las fechas son asignaciones directas). Por eso NO se crea la carpeta `rules/`.
- **`repository="abstract"`.** `Grupo`, `AlumnoGrupo` y `Nota` ya declaran `repository="abstract"` en sus `domains/*.xml` (tienen repo personalizado); `ModuloGrupo` no. Se documenta también en la tabla de ficheros.
- **ADMINISTRADOR no es tipoUsuario.** El administrador se identifica por el grupo `admins` (no hay `TipoUsuario` codigo ADMINISTRADOR en `data-init`). Por eso el menú de administración usa `groups="admins"` y V-Grupo-008 comprueba pertenencia al grupo `admins` en servidor.
