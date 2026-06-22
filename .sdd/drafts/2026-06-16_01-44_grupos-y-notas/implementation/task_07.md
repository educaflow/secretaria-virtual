---
type: implementation-task
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

Implementa el servicio de `AlumnoGrupo`: la interfaz `AlumnoGrupoService` y su implementación `AlumnoGrupoServiceImpl` (alta/baja de pertenencia, restauración del grupo desde contexto, validaciones, creación de notas "No evaluado" y delegación del cálculo de la media).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/service/AlumnoGrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de AlumnoGrupo |
| `system/gruposnotas/service/impl/AlumnoGrupoServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Validaciones, reglas y cálculo de media de AlumnoGrupo |

Extiende `ModelService`/`DefaultModelService`, descubierto por `ModelServiceFactory` por convención (sin módulo Guice). Persiste con `repository.save/remove`, **nunca** `super.*`.

### `com.educaflow.system.gruposnotas.service.AlumnoGrupoService` / `…impl.AlumnoGrupoServiceImpl`

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

### Frontera de confianza — AllowProperties por acción

#### `AlumnoGrupoServiceImpl.insert` (invocado desde "Guardar/Añadir alumno" genérico, vía `save-modal`)

Entidad: `AlumnoGrupo`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-AlumnoGrupo.md` (alumno).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `alumno` | cliente | sí | Input directo (elegido en el selector filtrado). Validado por V-AlumnoGrupo-003. |
| `grupo` | servidor | **NO** | Fuera de la whitelist: el cliente NO lo dicta. Se asigna INCONDICIONALMENTE en `insert` → `fireActionRule_RestaurarGrupoDesdeContexto` desde el Grupo padre del contexto del request (`__parent__` id), antes de `validateInsert`. El `onNew` (`grupo = __parent__`) es solo UX; el servidor lo restaura siempre, de modo que el cliente no puede reapuntar la pertenencia a otro grupo (defensa IDOR multi-centro, k-secure-coding §3.3/§4). |
| `centro` | servidor | **NO** | Campo transient auxiliar de UI; nunca se persiste ni lo dicta el cliente. |
| `notaMedia` | servidor | **NO** | Campo transient calculado (CC-001); fuera de la whitelist. |

### Trazabilidad V/R → ubicación (filas de esta tarea)

| V | Origen spec | Ubicación |
|---|---|---|
| V-AlumnoGrupo-001 | VAL-010 | AlumnoGrupoServiceImpl.validateInsert |
| V-AlumnoGrupo-002 | VAL-011 | AlumnoGrupoServiceImpl.validateInsert |
| V-AlumnoGrupo-003 | VAL-012 | AlumnoGrupoServiceImpl.validateInsert |
| V-AlumnoGrupo-004 | VAL-013, RES-004 | AlumnoGrupoServiceImpl.validateInsert (+ AlumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico) |
| V-AlumnoGrupo-005 | RES-005 | AlumnoGrupoServiceImpl.validateInsert (+ unique-constraint en AlumnoGrupo.xml) |
| V-AlumnoGrupo-006 | VAL-014 | AlumnoGrupoServiceImpl.validateRemove |

| R | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-AlumnoGrupo-001 | RN-005 | AlumnoGrupoServiceImpl.fireActionRule_CrearNotasNoEvaluado | Después de save |
| R-AlumnoGrupo-002 | — (campo `grupo` servidor; derivado de la frontera de confianza de la acción `Crear`) | AlumnoGrupoServiceImpl.fireActionRule_RestaurarGrupoDesdeContexto | Antes de validar |

#### Campos calculados de solo lectura (no son R)

`CC-001` (nota media; `momento: lectura`) **no** se mapea a una regla de negocio `R-`: es un campo derivado de solo lectura del modelo. Se cubre con la propiedad transient `notaMedia` de `AlumnoGrupo` (algoritmo inline en el CDATA del dominio); `AlumnoGrupoServiceImpl.calcularNotaMedia` se limita a delegar en el getter `getNotaMedia()`. No lleva identificador `R-` (en particular **no** reutiliza `R-AlumnoGrupo-001`).

> **`grupo` de AlumnoGrupo es campo servidor (regla añadida por el diseño).** R-AlumnoGrupo-002 no proviene de ninguna regla del spec: la añade el diseño porque la frontera de confianza de la acción `Crear` deja `grupo` fuera de la whitelist (`Input AllowProperties` = solo alumno) y, sin asignarlo en servidor, las validaciones que navegan el grupo recibirían `grupo=null` y darían NPE.
