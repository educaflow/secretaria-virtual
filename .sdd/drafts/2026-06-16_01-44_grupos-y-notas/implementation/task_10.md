---
type: implementation-task
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-validaciones

Implementa el servicio de `AlumnoGrupo` (interfaz + implementación). Alta por REST (`save-modal`, modal "Añadir alumno"); creación de notas NO_EVALUADO al añadir; `calcularNotaMedia` **delega** en el getter del dominio.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/service/AlumnoGrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz ModelService de AlumnoGrupo (+ calcularNotaMedia) |
| `system/gruposnotas/service/impl/AlumnoGrupoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Implementación de AlumnoGrupo |

Del diseño, Paso 2 — Servicios. **MUST NOT** crear módulo Guice. Persistir siempre con `repository`, nunca `super.*`. Cada acción empieza con `validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid)`.

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

> La comprobación de tipo de usuario del alumno (V-AlumnoGrupo-004) consulta `CentroUsuario`/`CentroUsuarioTipoUsuario` (entidades de `common`); se hace con `JPA.all(CentroUsuario.class).filter(...).bind(...)` parametrizado en un finder/método ad-hoc del servicio sobre esas entidades externas (no del repositorio de gruposnotas). Usar `:param` con `bind` (k-secure-coding §5).

### Frontera de confianza — AllowProperties por acción

#### `AlumnoGrupoServiceImpl.insert` (vía REST genérico `save-modal`)

Entidad: `AlumnoGrupo`. **Forma elegida:** `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-AlumnoGrupo.md`.

| Campo       | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|-------------|----------|--------------|---------------------------------------------|
| `grupo`     | cliente  | sí           | Padre del alta anidada (RUI-011/012). **Validado en `validateInsert`** (indicado, autorizado por centro, ABIERTO) — k-secure-coding §3.6. |
| `alumno`    | cliente  | sí           | Input directo del selector. |
| `notas`     | servidor | **NO**       | Creadas por el servidor (R-AlumnoGrupo-001/RN-005). |
| `notaMedia` | servidor | **NO**       | Transient derivado (CC-001), no persistido. |

#### `AlumnoGrupoServiceImpl.update` (vía REST genérico)

Entidad: `AlumnoGrupo`. **Forma elegida:** `createAllowProperties` (whitelist **vacía**).
**Origen spec:** acción `Modificar` de `entity-AlumnoGrupo.md` (sin `Input AllowProperties`).

| Campo    | Origen   | En whitelist | Justificación |
|----------|----------|--------------|----------------|
| `grupo`  | cliente  | **NO**       | Inmutable (no se reparenta). |
| `alumno` | cliente  | **NO**       | Inmutable (se quita y se añade otro). |

**Verificar:** que el `*ServiceImpl` está en `service.impl` con el nombre exacto `AlumnoGrupoServiceImpl` y el constructor `(Class<T>, Repository<T>)`; `grep` confirma que no hay `super.insert/update/remove`.

### Trazabilidad relevante
| V | Origen spec | Ubicación |
|---|-------------|-----------|
| V-AlumnoGrupo-001 | RES-005 | `domains/AlumnoGrupo.xml` `unique-constraint(grupo,alumno)` (ESC-019) |
| V-AlumnoGrupo-002 | VAL-010 | `AlumnoGrupoServiceImpl.validateInsert` |
| V-AlumnoGrupo-003 | VAL-011 | `AlumnoGrupoServiceImpl.validateInsert` |
| V-AlumnoGrupo-004 | VAL-012 | `AlumnoGrupoServiceImpl.validateInsert` (consulta CentroUsuario/CentroUsuarioTipoUsuario) + cliente `domain` del selector (UX) |
| V-AlumnoGrupo-005 | VAL-013, RES-004 | `AlumnoGrupoServiceImpl.validateInsert` (`AlumnoGrupoRepository.findByAlumnoAndGrupoCursoAcademico`) |
| V-AlumnoGrupo-006 | VAL-014 | `AlumnoGrupoServiceImpl.validateRemove` |
| V-AlumnoGrupo-007 | VAL-018 | `AlumnoGrupoServiceImpl.validateInsert` |
| V-AlumnoGrupo-008 | VAL-019 | `AlumnoGrupoServiceImpl.validateInsert` (centro del grupo = centro activo del supervisor; k-secure-coding §3.6) |

| R | Origen spec | Ubicación | Momento |
|---|-------------|-----------|---------|
| R-AlumnoGrupo-001 | RN-005 | `AlumnoGrupoServiceImpl.fireActionRule_CrearNotasNoEvaluado` (crea una Nota NO_EVALUADO por módulo) | Después de save (insert) |

| Campo | Origen spec | Ubicación |
|-------|-------------|-----------|
| `AlumnoGrupo.notaMedia` | CC-001 | `domains/AlumnoGrupo.xml` (transient, getter computado **INLINE** sobre `..db..`). `AlumnoGrupoServiceImpl.calcularNotaMedia` **delega** en `getNotaMedia()` del dominio. |
