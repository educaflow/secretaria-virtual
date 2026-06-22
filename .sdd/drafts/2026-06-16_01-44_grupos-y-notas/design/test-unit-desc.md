# Tests unitarios (TDD)

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer-system` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Aserciones con `org.junit.jupiter.api.Assertions` (`assertThrows`, `assertEquals`, `assertTrue`); **no** AssertJ. Para excepciones envueltas en cadena de causas se puede usar `JUnitHelper.assertThrowsCause`.
- Nombres de test: `metodo_condicion_resultadoEsperado`.

## Notas y supuestos de mocking (comunes)
- Los `*ServiceImpl` extienden `DefaultModelService<T>` y se construyen con `(Class<T> model, Repository<T> repository)`. En los tests se instancia el servicio con un mock de `Repository<T>` (o del repo personalizado, que también es un `Repository<T>`) y se inyectan a mano los demás colaboradores (`@Mock` en los campos `@Inject`, asignados por reflexión o, si se prefiere, vía un constructor de test; el implementador elige el mecanismo respetando que **no se toca BD real**).
- Los métodos `validate*` devuelven `Optional<BusinessMessages>` (vacío = OK; presente = error). Las acciones (`insert`/`update`/`cerrar`/`guardarNota`/…) invocan `validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid)`, de modo que un `validate*` no vacío hace que la acción lance la excepción de `throwIfInvalid`. En los tests de las acciones, el **mensaje exacto** del spec se comprueba sobre el `BusinessMessage` producido (texto del `BusinessMessages`/excepción). Cuando un test apunta solo a la rama de validación, basta testear `validateXxx` directamente y comprobar que el `Optional` está presente y contiene el mensaje esperado.
- **Usuario autenticado / rol**: el diseño usa `SecurityUtil.getUser()` (delega en `AuthUtils.getUser()`) y la pertenencia al grupo `admins` (administrador) / `AuthUtils.isAdmin`. Se mockean con `Mockito.mockStatic(SecurityUtil.class)` y, donde el diseño consulte `AuthUtils` directamente, `Mockito.mockStatic(AuthUtils.class)`. Por test se indica qué devuelve (supervisor de centro X / administrador). El `User` y el `Centro` se instancian con `new` y setters (`getCentroActivo()`, `getCurso()`).
- **Repositorios personalizados** (`GrupoRepository`, `AlumnoGrupoRepository`, `NotaRepository`): mock; cada finder/contador se programa con `when(...).thenReturn(...)`.
- **`ModelServiceFactory`** (resuelve `NotaService` desde `AlumnoGrupoServiceImpl`): mock; `when(modelServiceFactory.resolve(Nota.class)).thenReturn(notaServiceMock)`.
- **Entidades de dominio** (`Grupo`, `ModuloGrupo`, `AlumnoGrupo`, `Nota`, `Curso`, `Modulo`, `Centro`, `User`): se instancian con `new` y se rellenan con setters; **no** se mockean.
- **Cálculo de la media (CC-001)**: el algoritmo vive ÍNTEGRAMENTE en el cuerpo CDATA de la propiedad `notaMedia` del dominio (no hay clase utilitaria; la entidad es un POJO que no puede depender de `..service..`, C13). El getter generado `AlumnoGrupo.getNotaMedia()` ejecuta ese algoritmo y `AlumnoGrupoServiceImpl.calcularNotaMedia` se limita a delegar en él. Se prueba a través de `calcularNotaMedia(alumnoGrupo)` (que invoca el getter) con `AlumnoGrupo`/`Nota` reales instanciados con `new` y setters, **sin mocks** (el getter es código puro generado por Axelor desde el XML).

---

## Clase: `com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl`  —  servicio

**Responsabilidad:** alta/modificación/borrado de Grupo, cierre y reapertura; fija centro/cursoAcademico/estado en servidor, crea los ModuloGrupo del curso y registra fecha de cierre.
**Colaboradores a mockear:** `GrupoRepository` (mock; `findByNombreCentroCursoAcademico`, `save`), `Repository<ModuloGrupo>` o `ModuloGrupoRepository` (mock; `save`), `SecurityUtil`/`AuthUtils` (estáticos; usuario actual y rol admin). Entidades `Grupo`, `Curso`, `CursoModulo`/`Modulo`, `Centro`, `User` reales.
**Origen diseño:** `insert`, `update`, `validateInsert`, `validateUpdate`, `validateRemove`, `cerrar`, `validateCerrar`, `reabrir`, `validateReabrir`, `fireActionRule_FijarCentroYCursoAcademicoSiSupervisor`, `fireActionRule_EstadoInicialAbierto`, `fireActionRule_CrearModulosGrupo`, `fireActionRule_RegistrarCierre`, `fireActionRule_RegistrarReapertura`. Reglas V-Grupo-001..009, R-Grupo-001..004.

### Método: `validateInsert(Grupo grupo)`

- **`validateInsert_grupoValido_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Grupo-001/002/003.
  - **Arrange:** `Grupo` con `nombre="1º DAM A"`, `curso` no nulo, `centro` y `cursoAcademico` fijados; `grupoRepository.findByNombreCentroCursoAcademico(nombre, centro, cursoAcademico)` → `null` (no hay homónimo).
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** devuelve `Optional.empty()`.
- **`validateInsert_nombreVacio_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Grupo-001 (VAL-001).
  - **Arrange:** `Grupo` con `nombre=null` (y otro test borde con `nombre=""`/espacios, ver más abajo), `curso` no nulo.
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` presente; el `BusinessMessages` contiene el mensaje exacto «El nombre del grupo es obligatorio».
- **`validateInsert_nombreEnBlanco_devuelveMensajeObligatorio`** — Tipo: borde. Verifica: V-Grupo-001 (VAL-001).
  - **Arrange:** `Grupo` con `nombre="   "` (solo espacios), `curso` no nulo. (Comprobación equivalente a `TextUtil.isNullOrBlank`.)
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` presente con «El nombre del grupo es obligatorio».
- **`validateInsert_cursoNulo_devuelveMensajeCursoObligatorio`** — Tipo: error. Verifica: V-Grupo-002 (VAL-002).
  - **Arrange:** `Grupo` con `nombre="1º DAM A"`, `curso=null`.
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` presente con «El curso es obligatorio».
- **`validateInsert_nombreDuplicado_devuelveMensajeYaExiste`** — Tipo: error. Verifica: V-Grupo-003 (VAL-003, RES-001).
  - **Arrange:** `Grupo` válido (nombre, curso, centro, cursoAcademico); `grupoRepository.findByNombreCentroCursoAcademico(...)` → **otro** `Grupo` distinto (id distinto del entrante).
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` presente con «Ya existe un grupo con ese nombre en este centro y curso académico».
- **`validateInsert_mismoNombreOtroCentro_devuelveOptionalVacio`** — Tipo: borde. Verifica: V-Grupo-003.
  - **Arrange:** `grupoRepository.findByNombreCentroCursoAcademico(...)` → `null` (la unicidad es por centro+cursoAcademico; el finder ya filtra por esas claves, así que un homónimo en otro centro no se devuelve).
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional.empty()`.

### Método: `validateUpdate(Grupo grupo, Grupo original)`

- **`validateUpdate_grupoAbiertoNombreUnico_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Grupo-004/005.
  - **Arrange:** `original.estado=ABIERTO`; `grupo` con nombre cambiado; `findByNombreCentroCursoAcademico(...)` → `null` (o el propio grupo, mismo id → no se considera colisión).
  - **Act:** `validateUpdate(grupo, original)`.
  - **Assert:** `Optional.empty()`.
- **`validateUpdate_grupoCerrado_devuelveMensajeNoModificar`** — Tipo: error. Verifica: V-Grupo-004 (VAL-004).
  - **Arrange:** `original.estado=CERRADO`.
  - **Act:** `validateUpdate(grupo, original)`.
  - **Assert:** `Optional` presente con «No se puede modificar un grupo cerrado».
- **`validateUpdate_nombreDuplicadoEnOtroGrupo_devuelveMensajeYaExiste`** — Tipo: error. Verifica: V-Grupo-005 (VAL-005, RES-001).
  - **Arrange:** `original.estado=ABIERTO`; `grupo` con id=10; `findByNombreCentroCursoAcademico(...)` → **otro** grupo con id=20 (≠ id propio).
  - **Act:** `validateUpdate(grupo, original)`.
  - **Assert:** `Optional` presente con «Ya existe un grupo con ese nombre en este centro y curso académico».
- **`validateUpdate_nombreCoincideConPropioGrupo_devuelveOptionalVacio`** — Tipo: borde. Verifica: V-Grupo-005.
  - **Arrange:** `original.estado=ABIERTO`; `grupo` con id=10; `findByNombreCentroCursoAcademico(...)` → el **mismo** grupo id=10 (excluir el propio por id).
  - **Act:** `validateUpdate(grupo, original)`.
  - **Assert:** `Optional.empty()` (no colisiona consigo mismo).

### Método: `validateRemove(Grupo grupo)`

- **`validateRemove_grupoAbierto_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Grupo-006.
  - **Arrange:** `grupo.estado=ABIERTO`.
  - **Act:** `validateRemove(grupo)`.
  - **Assert:** `Optional.empty()`.
- **`validateRemove_grupoCerrado_devuelveMensajeNoBorrar`** — Tipo: error. Verifica: V-Grupo-006 (VAL-009).
  - **Arrange:** `grupo.estado=CERRADO`.
  - **Act:** `validateRemove(grupo)`.
  - **Assert:** `Optional` presente con «No se puede borrar un grupo cerrado».

### Método: `validateCerrar(Grupo grupo, Grupo grupoOriginal)`

- **`validateCerrar_grupoAbierto_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Grupo-009.
  - **Arrange:** `grupoOriginal.estado=ABIERTO`.
  - **Act:** `validateCerrar(grupo, grupoOriginal)`.
  - **Assert:** `Optional.empty()`.
- **`validateCerrar_grupoYaCerrado_devuelveMensajeYaCerrado`** — Tipo: error. Verifica: V-Grupo-009 (VAL-006).
  - **Arrange:** `grupoOriginal.estado=CERRADO`.
  - **Act:** `validateCerrar(grupo, grupoOriginal)`.
  - **Assert:** `Optional` presente con «El grupo ya está cerrado».

### Método: `validateReabrir(Grupo grupo, Grupo grupoOriginal)`

- **`validateReabrir_cerradoYAdministrador_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Grupo-007/008.
  - **Arrange:** `grupoOriginal.estado=CERRADO`; `mockStatic(SecurityUtil)`/`AuthUtils` → usuario actual es administrador (pertenece al grupo `admins` / `isAdmin` → `true`).
  - **Act:** `validateReabrir(grupo, grupoOriginal)`.
  - **Assert:** `Optional.empty()`.
- **`validateReabrir_grupoYaAbierto_devuelveMensajeYaAbierto`** — Tipo: error. Verifica: V-Grupo-007 (VAL-007).
  - **Arrange:** `grupoOriginal.estado=ABIERTO`; usuario administrador.
  - **Act:** `validateReabrir(grupo, grupoOriginal)`.
  - **Assert:** `Optional` presente con «El grupo ya está abierto».
- **`validateReabrir_noAdministrador_devuelveMensajeSinPermisos`** — Tipo: error. Verifica: V-Grupo-008 (VAL-008).
  - **Arrange:** `grupoOriginal.estado=CERRADO`; usuario actual NO administrador (supervisor; `admins` → `false`).
  - **Act:** `validateReabrir(grupo, grupoOriginal)`.
  - **Assert:** `Optional` presente con «No tiene permisos para reabrir el grupo».

### Método: `insert(Grupo grupo)`

- **`insert_supervisor_fijaCentroCursoEstadoYCreaModulos`** — Tipo: happy. Verifica: R-Grupo-002, estado inicial, R-Grupo-001.
  - **Arrange:** usuario actual = supervisor con `centroActivo=centroX` y `centroX.getCurso()=2024` (`mockStatic(SecurityUtil)`); `grupo` con `nombre`/`curso` válidos y `curso.getModulos()` con 2 `CursoModulo` (módulos «Programación», «Bases de datos»); `findByNombreCentroCursoAcademico(...)` → `null`; `grupoRepository.save(grupo)` → el mismo `grupo` (con id asignado); `moduloGrupoRepository.save(any)` → eco del argumento.
  - **Act:** `insert(grupo)`.
  - **Assert:** `grupo.getCentro()==centroX` y `grupo.getCursoAcademico()==2024` (fijados por servidor, ignorando lo entrante); `grupo.getEstado()==ABIERTO`; `verify(grupoRepository).save(grupo)`; `verify(moduloGrupoRepository, times(2)).save(any())` (un ModuloGrupo por módulo del curso) y cada uno con `grupo` asignado.
- **`insert_administrador_respetaCentroYCursoAcademicoDelCliente`** — Tipo: borde. Verifica: R-Grupo-002 (rama admin).
  - **Arrange:** usuario actual = administrador (`isAdmin`/`admins` → `true`); `grupo` con `centro=centroB`, `cursoAcademico=2025` (elegidos por el admin); finder → `null`; `save` → eco.
  - **Act:** `insert(grupo)`.
  - **Assert:** `grupo.getCentro()==centroB` y `grupo.getCursoAcademico()==2025` (no se sobrescriben); `estado==ABIERTO`.
- **`insert_validacionFalla_lanzaExcepcionYNoGuarda`** — Tipo: error. Verifica: V-Grupo-001/002/003.
  - **Arrange:** `grupo` con `nombre=null` (falla V-Grupo-001).
  - **Act:** `insert(grupo)`.
  - **Assert:** lanza la excepción de `BusinessMessages.throwIfInvalid` (mensaje «El nombre del grupo es obligatorio»); `verify(grupoRepository, never()).save(any())` y `verify(moduloGrupoRepository, never()).save(any())`.

### Método: `update(Grupo grupo, Grupo original)`

- **`update_grupoAbierto_guardaYRestauraInmutables`** — Tipo: happy. Verifica: V-Grupo-004/005 + restauración campos inmutables (k-secure-coding §3).
  - **Arrange:** `original.estado=ABIERTO`, `original.curso=cursoA`, `original.centro=centroX`, `original.cursoAcademico=2024`, `original.fechaCierre=null`; `grupo` (entrante) con nombre nuevo y con `curso`/`centro`/`cursoAcademico`/`estado`/`fechaCierre` manipulados por el cliente; finder → `null`; `save` → eco.
  - **Act:** `update(grupo, original)`.
  - **Assert:** tras `update`, `grupo.getCurso()==cursoA`, `getCentro()==centroX`, `getCursoAcademico()==2024`, `getEstado()==ABIERTO`, `getFechaCierre()==null` (restaurados desde `original`, no los del cliente); `getNombre()` = el nuevo; `verify(grupoRepository).save(grupo)`.
- **`update_grupoCerrado_lanzaExcepcion`** — Tipo: error. Verifica: V-Grupo-004.
  - **Arrange:** `original.estado=CERRADO`.
  - **Act:** `update(grupo, original)`.
  - **Assert:** lanza excepción con «No se puede modificar un grupo cerrado»; `verify(grupoRepository, never()).save(any())`.

### Método: `cerrar(Grupo grupo, Grupo grupoOriginal)`

- **`cerrar_grupoAbierto_pasaACerradoYRegistraFecha`** — Tipo: happy. Verifica: V-Grupo-009, R-Grupo-003.
  - **Arrange:** `grupoOriginal.estado=ABIERTO`; `grupo.estado=ABIERTO`, `fechaCierre=null`; `save` → eco.
  - **Act:** `cerrar(grupo, grupoOriginal)`.
  - **Assert:** `grupo.getEstado()==CERRADO`; `grupo.getFechaCierre()` no nulo (fecha/hora del cierre); `verify(grupoRepository).save(grupo)`.
- **`cerrar_grupoYaCerrado_lanzaExcepcion`** — Tipo: error. Verifica: V-Grupo-009.
  - **Arrange:** `grupoOriginal.estado=CERRADO`.
  - **Act:** `cerrar(grupo, grupoOriginal)`.
  - **Assert:** lanza excepción con «El grupo ya está cerrado»; `verify(grupoRepository, never()).save(any())`.

### Método: `reabrir(Grupo grupo, Grupo grupoOriginal)`

- **`reabrir_cerradoYAdministrador_pasaAAbiertoYBorraFecha`** — Tipo: happy. Verifica: V-Grupo-007/008, R-Grupo-004.
  - **Arrange:** `grupoOriginal.estado=CERRADO`; usuario administrador; `grupo.estado=CERRADO`, `fechaCierre` con valor; `save` → eco.
  - **Act:** `reabrir(grupo, grupoOriginal)`.
  - **Assert:** `grupo.getEstado()==ABIERTO`; `grupo.getFechaCierre()==null`; `verify(grupoRepository).save(grupo)`.
- **`reabrir_noAdministrador_lanzaExcepcionYNoGuarda`** — Tipo: error. Verifica: V-Grupo-008.
  - **Arrange:** `grupoOriginal.estado=CERRADO`; usuario NO administrador (supervisor).
  - **Act:** `reabrir(grupo, grupoOriginal)`.
  - **Assert:** lanza excepción con «No tiene permisos para reabrir el grupo»; `verify(grupoRepository, never()).save(any())`.
- **`reabrir_grupoYaAbierto_lanzaExcepcion`** — Tipo: error. Verifica: V-Grupo-007.
  - **Arrange:** `grupoOriginal.estado=ABIERTO`; usuario administrador.
  - **Act:** `reabrir(grupo, grupoOriginal)`.
  - **Assert:** lanza excepción con «El grupo ya está abierto»; `verify(grupoRepository, never()).save(any())`.

### Método: `allowPropertiesInsert/Update/Remove/Cerrar/Reabrir()`

- **`allowPropertiesInsert_devuelveWhitelistEsperada`** — Tipo: happy. Verifica: — (frontera de confianza).
  - **Arrange:** servicio construido.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** permite `nombre`, `curso`, `centro`, `cursoAcademico`, `alumnosGrupo` y **no** permite `estado` ni `fechaCierre` (`allowProperty(...)` true/false según el campo).
- **`allowPropertiesUpdate_soloNombre`** — Tipo: happy. Verifica: —.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** permite `nombre`; no permite `curso`/`centro`/`cursoAcademico`/`estado`/`fechaCierre`.
- **`allowPropertiesCerrar_denyAll`** / **`allowPropertiesReabrir_denyAll`** / **`allowPropertiesRemove_denyAll`** — Tipo: happy. Verifica: —.
  - **Act:** el método correspondiente.
  - **Assert:** no permite ningún campo (deny-all): `allowProperty("estado")`/`allowProperty("fechaCierre")`/cualquier campo → false.

---

## Clase: `com.educaflow.system.gruposnotas.service.impl.ModuloGrupoServiceImpl`  —  servicio

**Responsabilidad:** respaldo de la unicidad módulo–grupo en alta; no editable.
**Colaboradores a mockear:** `Repository<ModuloGrupo>`/repo para consultar existencia; (si la comprobación de unicidad navega por relaciones, basta con `Grupo`/`ModuloGrupo` reales y un finder mockeado). Entidades reales.
**Origen diseño:** `validateInsert`, `update` (no editable), `allowPropertiesInsert/Update`. Regla V-ModuloGrupo-001.

### Método: `validateInsert(ModuloGrupo moduloGrupo)`

- **`validateInsert_moduloNoRepetido_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-ModuloGrupo-001.
  - **Arrange:** `ModuloGrupo` con `grupo` y `modulo`; la consulta de existencia (repo/relación) → no hay otro con ese grupo+módulo.
  - **Act:** `validateInsert(moduloGrupo)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_moduloDuplicado_devuelveMensajeModuloYaEnGrupo`** — Tipo: error. Verifica: V-ModuloGrupo-001 (RES-003).
  - **Arrange:** la consulta de existencia → ya existe un `ModuloGrupo` con ese grupo+módulo.
  - **Act:** `validateInsert(moduloGrupo)`.
  - **Assert:** `Optional` presente con el mensaje que transmite «el módulo ya está en el grupo».

### Método: `update(ModuloGrupo moduloGrupo, ModuloGrupo original)`

- **`update_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: — (defensa en profundidad, k-secure-coding §9.2).
  - **Arrange:** `ModuloGrupo` cualquiera y su `original`.
  - **Act:** `update(moduloGrupo, original)`.
  - **Assert:** lanza `UnsupportedOperationException` incondicionalmente.

### Método: `allowPropertiesInsert/Update()`

- **`allowPropertiesInsert_denyAll`** / **`allowPropertiesUpdate_denyAll`** — Tipo: happy. Verifica: —.
  - **Act:** el método correspondiente.
  - **Assert:** no permite ningún campo (los crea el servidor; ModuloGrupo no se edita).

---

## Clase: `com.educaflow.system.gruposnotas.service.impl.AlumnoGrupoServiceImpl`  —  servicio

**Responsabilidad:** alta/baja de pertenencia alumno–grupo; restaura el `grupo` desde el contexto, valida (alumno indicado, grupo abierto, alumno del centro y tipo Alumno, no repetido, no en otro grupo del mismo curso académico), crea las notas «No evaluado» y calcula la nota media.
**Colaboradores a mockear:** `AlumnoGrupoRepository` (mock; `existsOtroGrupoMismoCursoAcademico`, `findByGrupo`, `save`), `NotaRepository` (mock), `ModelServiceFactory` (mock; `resolve(Nota.class)` → `NotaService` mock), `GrupoRepository` (si la restauración del grupo resuelve por id), repo/relación para V-AlumnoGrupo-003 (centro + tipo Alumno), `SecurityUtil`/`AuthUtils` (estático). Entidades `AlumnoGrupo`, `Grupo`, `User`, `Centro`, `ModuloGrupo`, `Nota` reales.
**Origen diseño:** `insert`, `validateInsert`, `validateRemove`, `update` (no editable), `fireActionRule_RestaurarGrupoDesdeContexto`, `fireActionRule_CrearNotasNoEvaluado`, `calcularNotaMedia`, `allowProperties*`. Reglas V-AlumnoGrupo-001..006, R-AlumnoGrupo-001, R-AlumnoGrupo-002, CC-001.

### Método: `validateInsert(AlumnoGrupo alumnoGrupo)`

> Supuesto: `validateInsert` se invoca cuando `grupo` ya está restaurado en servidor (se asigna en `insert` antes de validar). Los tests programan `alumnoGrupo.setGrupo(grupoAbierto)` directamente.

- **`validateInsert_datosValidos_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-AlumnoGrupo-001..005.
  - **Arrange:** `grupo` ABIERTO con `centro=centroX`, `cursoAcademico=2024`; `alumno` = usuario de `centroX` tipo ALUMNO; consulta tipo/centro → válido; `existsOtroGrupoMismoCursoAcademico(alumno, centroX, 2024, null)` → `false`; no hay pertenencia repetida (RES-005).
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_alumnoNulo_devuelveMensajeDebeElegirAlumno`** — Tipo: error. Verifica: V-AlumnoGrupo-001 (VAL-010).
  - **Arrange:** `alumnoGrupo` con `alumno=null`, `grupo` ABIERTO.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con «Debe elegir un alumno».
- **`validateInsert_grupoCerrado_devuelveMensajeNoAnadir`** — Tipo: error. Verifica: V-AlumnoGrupo-002 (VAL-011).
  - **Arrange:** `grupo` CERRADO; `alumno` válido.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con «No se pueden añadir alumnos a un grupo cerrado».
- **`validateInsert_alumnoNoEsDelCentroOTipo_devuelveMensajeTipoAlumno`** — Tipo: error. Verifica: V-AlumnoGrupo-003 (VAL-012).
  - **Arrange:** `grupo` ABIERTO de `centroX`; `alumno` que NO es usuario tipo ALUMNO de `centroX` (p.ej. profesor del centro, o alumno de otro centro); consulta tipo/centro → no válido.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con «El alumno debe ser un usuario de tipo Alumno del centro del grupo».
- **`validateInsert_alumnoEnOtroGrupoMismoCursoAcademico_devuelveMensajeYaPertenece`** — Tipo: error. Verifica: V-AlumnoGrupo-004 (VAL-013, RES-004).
  - **Arrange:** `grupo` ABIERTO `centroX`/`2024`; `alumno` válido del centro y tipo; `existsOtroGrupoMismoCursoAcademico(alumno, centroX, 2024, null)` → `true`.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con «El alumno ya pertenece a otro grupo de este curso académico».
- **`validateInsert_alumnoYaEnElGrupo_devuelveMensajeYaEstaEnGrupo`** — Tipo: error. Verifica: V-AlumnoGrupo-005 (RES-005).
  - **Arrange:** `grupo` ABIERTO; `alumno` válido y no en otro grupo, pero ya pertenece a ESTE grupo (la consulta de unicidad grupo+alumno → existe).
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con el mensaje que transmite «el alumno ya está en el grupo».

### Método: `validateRemove(AlumnoGrupo alumnoGrupo)`

- **`validateRemove_grupoAbierto_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-AlumnoGrupo-006.
  - **Arrange:** `alumnoGrupo.grupo.estado=ABIERTO`.
  - **Act:** `validateRemove(alumnoGrupo)`.
  - **Assert:** `Optional.empty()`.
- **`validateRemove_grupoCerrado_devuelveMensajeNoQuitar`** — Tipo: error. Verifica: V-AlumnoGrupo-006 (VAL-014).
  - **Arrange:** `alumnoGrupo.grupo.estado=CERRADO`.
  - **Act:** `validateRemove(alumnoGrupo)`.
  - **Assert:** `Optional` presente con «No se pueden quitar alumnos de un grupo cerrado».

### Método: `insert(AlumnoGrupo alumnoGrupo)`

- **`insert_alumnoValido_restauraGrupoGuardaYCreaNotasNoEvaluado`** — Tipo: happy. Verifica: R-AlumnoGrupo-002, V-AlumnoGrupo-001..005, R-AlumnoGrupo-001.
  - **Arrange:** contexto del request con id del Grupo padre; `grupoRepository.find(parentId)` → `grupoAbierto` (centroX/2024) con 2 `ModuloGrupo`; `alumnoGrupo` entrante con `alumno` válido y `grupo=null`; consultas de validación → todas OK; `alumnoGrupoRepository.save(alumnoGrupo)` → eco con id; `modelServiceFactory.resolve(Nota.class)` → `notaService` mock; `notaService.insert(any)`/`save` → eco.
  - **Act:** `insert(alumnoGrupo)`.
  - **Assert:** `alumnoGrupo.getGrupo()==grupoAbierto` (restaurado desde el padre, no del cliente); `verify(alumnoGrupoRepository).save(alumnoGrupo)`; se crean 2 notas (una por ModuloGrupo) con `valor=NO_EVALUADO` y `alumnoGrupo` asignado (verify sobre el colaborador que persiste las notas, `times(2)`).
  - **Nota:** la fuente del id del padre (`__parent__` del contexto) es un detalle del helper; el test programa la restauración para que `getGrupo()` quede en `grupoAbierto`.
- **`insert_grupoRestauradoIgnoraGrupoDelCliente`** — Tipo: borde (seguridad IDOR). Verifica: R-AlumnoGrupo-002.
  - **Arrange:** `alumnoGrupo` entrante con un `grupo` malicioso de otro centro ya seteado; contexto del padre → `grupoAbierto` legítimo de centroX; finder → `grupoAbierto`.
  - **Act:** `insert(alumnoGrupo)`.
  - **Assert:** tras `insert`, `alumnoGrupo.getGrupo()==grupoAbierto` (el del padre), no el inyectado por el cliente (asignación incondicional, sin `if`).
- **`insert_validacionFalla_lanzaExcepcionYNoGuardaNiCreaNotas`** — Tipo: error. Verifica: V-AlumnoGrupo-001.
  - **Arrange:** contexto padre → `grupoAbierto`; `alumnoGrupo.alumno=null` (falla V-AlumnoGrupo-001).
  - **Act:** `insert(alumnoGrupo)`.
  - **Assert:** lanza excepción con «Debe elegir un alumno»; `verify(alumnoGrupoRepository, never()).save(any())` y no se crea ninguna nota.

### Método: `update(AlumnoGrupo alumnoGrupo, AlumnoGrupo original)`

- **`update_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: — (defensa en profundidad).
  - **Act:** `update(alumnoGrupo, original)`.
  - **Assert:** lanza `UnsupportedOperationException` incondicionalmente.

### Método: `calcularNotaMedia(AlumnoGrupo alumnoGrupo)`

> El método delega en el getter de dominio `alumnoGrupo.getNotaMedia()`, donde vive el algoritmo (CDATA, CC-001). Como el getter es código puro, el algoritmo se cubre AQUÍ end-to-end con `AlumnoGrupo`/`Nota` reales (sin mocks): cada caso construye el `alumnoGrupo` con su lista de `Nota`, llama a `calcularNotaMedia(alumnoGrupo)` y comprueba el `String` devuelto.

- **`calcularNotaMedia_sinModulosEvaluados_devuelveSinNota`** — Tipo: borde. Verifica: CC-001 (ESC-008).
  - **Arrange:** `alumnoGrupo` con todas las notas en `NO_EVALUADO` (y otro caso con lista vacía).
  - **Act:** `calcularNotaMedia(alumnoGrupo)`.
  - **Assert:** devuelve `"Sin nota"`.
- **`calcularNotaMedia_unaNotaNumerica_devuelveEsaNota`** — Tipo: happy. Verifica: CC-001 (ESC-006).
  - **Arrange:** una `NOTA_08` + una `NO_EVALUADO`.
  - **Act:** `calcularNotaMedia(alumnoGrupo)`.
  - **Assert:** devuelve `"8"` (el NO_EVALUADO no cuenta).
- **`calcularNotaMedia_matriculaHonorCuentaComo10_yExcluyeNoEvaluado`** — Tipo: happy. Verifica: CC-001 (ESC-007, paso 4).
  - **Arrange:** una `MATRICULA_HONOR` en un módulo y `NO_EVALUADO` en el otro.
  - **Act:** `calcularNotaMedia(alumnoGrupo)`.
  - **Assert:** devuelve `"10"` (MH=10; el no evaluado se excluye).
- **`calcularNotaMedia_mediaConMHy7_devuelve9Redondeado`** — Tipo: happy/borde. Verifica: CC-001 (ESC-007, paso 5).
  - **Arrange:** una `MATRICULA_HONOR` (10) y una `NOTA_07` (7).
  - **Act:** `calcularNotaMedia(alumnoGrupo)`.
  - **Assert:** devuelve `"9"` (media de 10 y 7 = 8.5 → `Math.round` → 9).
- **`calcularNotaMedia_media8y6_devuelve7`** — Tipo: happy. Verifica: CC-001 (ESC-014).
  - **Arrange:** `NOTA_08` y `NOTA_06`.
  - **Act:** `calcularNotaMedia(alumnoGrupo)`.
  - **Assert:** devuelve `"7"` (media de 8 y 6 = 7).
- **`calcularNotaMedia_redondeoHaciaArriba_enMitad`** — Tipo: borde (redondeo). Verifica: CC-001.
  - **Arrange:** `NOTA_05` y `NOTA_06` (media 5.5).
  - **Act:** `calcularNotaMedia(alumnoGrupo)`.
  - **Assert:** devuelve `"6"` (`Math.round(5.5)`=6). Documenta el criterio de redondeo «al entero más cercano».
- **`calcularNotaMedia_listaNula_devuelveSinNota`** — Tipo: borde. Verifica: CC-001.
  - **Arrange:** `alumnoGrupo` con `notas = null`.
  - **Act:** `calcularNotaMedia(alumnoGrupo)`.
  - **Assert:** devuelve `"Sin nota"` (sin NPE; el CDATA trata `null` como «sin notas evaluadas»).

### Método: `allowPropertiesInsert/Update/Remove()`

- **`allowPropertiesInsert_soloAlumno`** — Tipo: happy. Verifica: — (frontera de confianza).
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** permite `alumno`; no permite `grupo`, `centro` ni `notaMedia`.
- **`allowPropertiesUpdate_denyAll`** / **`allowPropertiesRemove_denyAll`** — Tipo: happy. Verifica: —.
  - **Assert:** no permite ningún campo.

---

## Clase: `com.educaflow.system.gruposnotas.service.impl.NotaServiceImpl`  —  servicio

**Responsabilidad:** respaldo de unicidad nota (alumno+módulo) en alta; guardado de nota con validación (valor en dominio, grupo abierto, máx 3 MH) y fijado de fechas de calificación/última modificación en servidor.
**Colaboradores a mockear:** `NotaRepository` (mock; `countMatriculasHonorByModuloGrupo`, `save`, consulta de unicidad). Entidades `Nota`, `ModuloGrupo`, `Grupo`, `AlumnoGrupo` reales.
**Origen diseño:** `validateInsert`, `guardarNota`, `validateGuardarNota`, `fireActionRule_FijarFechasCalificacion`, `allowProperties*`. Reglas V-Nota-001/002/003/005, R-Nota-001, R-Nota-002.

### Método: `validateInsert(Nota nota)`

- **`validateInsert_notaUnica_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Nota-005.
  - **Arrange:** `nota` con `moduloGrupo`+`alumnoGrupo`; consulta de unicidad → no existe otra.
  - **Act:** `validateInsert(nota)`.
  - **Assert:** `Optional.empty()`.
- **`validateInsert_notaDuplicadaParaAlumnoYModulo_devuelveMensaje`** — Tipo: error. Verifica: V-Nota-005 (RES-006).
  - **Arrange:** consulta de unicidad → ya existe nota para ese moduloGrupo+alumnoGrupo.
  - **Act:** `validateInsert(nota)`.
  - **Assert:** `Optional` presente con el mensaje que transmite «ya existe nota para ese alumno y módulo».

### Método: `validateGuardarNota(Nota nota, Nota notaOriginal)`

- **`validateGuardarNota_valorValidoGrupoAbierto_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Nota-001/002/003.
  - **Arrange:** `nota.valor=NOTA_08`; `nota.moduloGrupo.grupo.estado=ABIERTO`; `notaOriginal.valor=NO_EVALUADO`.
  - **Act:** `validateGuardarNota(nota, notaOriginal)`.
  - **Assert:** `Optional.empty()`.
- **`validateGuardarNota_grupoCerrado_devuelveMensajeNoModificarNotas`** — Tipo: error. Verifica: V-Nota-002 (VAL-015).
  - **Arrange:** `nota.moduloGrupo.grupo.estado=CERRADO`; `nota.valor=NOTA_08`.
  - **Act:** `validateGuardarNota(nota, notaOriginal)`.
  - **Assert:** `Optional` presente con «No se pueden modificar las notas de un grupo cerrado».
- **`validateGuardarNota_cuartaMatriculaHonor_devuelveMensajeMax3`** — Tipo: error/borde. Verifica: V-Nota-003 (VAL-017).
  - **Arrange:** `nota.valor=MATRICULA_HONOR`; `notaOriginal.valor != MATRICULA_HONOR`; grupo ABIERTO; `notaRepository.countMatriculasHonorByModuloGrupo(moduloGrupo)` → `3`.
  - **Act:** `validateGuardarNota(nota, notaOriginal)`.
  - **Assert:** `Optional` presente con «No se pueden poner más de 3 matrículas de honor en un módulo».
- **`validateGuardarNota_terceraMatriculaHonor_devuelveOptionalVacio`** — Tipo: borde (límite). Verifica: V-Nota-003.
  - **Arrange:** `nota.valor=MATRICULA_HONOR`; `notaOriginal.valor != MATRICULA_HONOR`; grupo ABIERTO; `countMatriculasHonorByModuloGrupo(...)` → `2` (esta sería la 3ª, permitida).
  - **Act:** `validateGuardarNota(nota, notaOriginal)`.
  - **Assert:** `Optional.empty()`; (se puede `verify` que se consultó el contador).
- **`validateGuardarNota_yaEraMatriculaHonor_noCuentaContraLimite`** — Tipo: borde. Verifica: V-Nota-003.
  - **Arrange:** `nota.valor=MATRICULA_HONOR`; `notaOriginal.valor=MATRICULA_HONOR` (no cambia a MH, ya lo era); grupo ABIERTO.
  - **Act:** `validateGuardarNota(nota, notaOriginal)`.
  - **Assert:** `Optional.empty()`; `verify(notaRepository, never()).countMatriculasHonorByModuloGrupo(any())` (el límite solo aplica cuando se pasa de no-MH a MH).
- **`validateGuardarNota_valorFueraDeDominio_devuelveMensajeNotaInvalida`** — Tipo: error. Verifica: V-Nota-001 (VAL-016).
  - **Arrange:** `nota.valor=null` (o un valor no perteneciente al dominio `{NO_EVALUADO, NOTA_01..NOTA_10, MATRICULA_HONOR}` si por la vía REST llegara uno inválido); grupo ABIERTO.
  - **Act:** `validateGuardarNota(nota, notaOriginal)`.
  - **Assert:** `Optional` presente con «La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor».
  - **Supuesto:** como `valor` es enum, el dominio lo garantiza en gran parte; el caso testeable a nivel de servicio es `valor=null`. Se documenta que un literal inválido no es construible como enum.

### Método: `guardarNota(Nota nota, Nota notaOriginal)`

- **`guardarNota_primeraCalificacion_fijaFechaCalificacion`** — Tipo: happy. Verifica: R-Nota-001 (CC-002).
  - **Arrange:** `notaOriginal.valor=NO_EVALUADO`, `fechaCalificacion=null`, `fechaUltimaModificacion=null`; `nota.valor=NOTA_08`, grupo ABIERTO; `notaRepository.save(nota)` → eco.
  - **Act:** `guardarNota(nota, notaOriginal)`.
  - **Assert:** `nota.getFechaCalificacion()` no nula (fecha/hora del momento); `nota.getFechaUltimaModificacion()` permanece `null`; `verify(notaRepository).save(nota)`.
- **`guardarNota_modificacionPosterior_fijaFechaUltimaModificacion`** — Tipo: happy. Verifica: R-Nota-002 (CC-003).
  - **Arrange:** `notaOriginal.valor=NOTA_08` (ya calificada), `fechaCalificacion` con valor previo; `nota.valor=NOTA_06` (cambia); grupo ABIERTO; `save` → eco.
  - **Act:** `guardarNota(nota, notaOriginal)`.
  - **Assert:** `nota.getFechaUltimaModificacion()` no nula; `nota.getFechaCalificacion()` se conserva igual a la de `notaOriginal` (no se reescribe; restaurada desde original).
- **`guardarNota_noCambiaValor_noTocaFechas`** — Tipo: borde. Verifica: R-Nota-001/002.
  - **Arrange:** `notaOriginal.valor=NOTA_08` con `fechaCalificacion` previa y `fechaUltimaModificacion=null`; `nota.valor=NOTA_08` (mismo valor); grupo ABIERTO; `save` → eco.
  - **Act:** `guardarNota(nota, notaOriginal)`.
  - **Assert:** `nota.getFechaCalificacion()` == la de `original`; `nota.getFechaUltimaModificacion()` == `null` (no cambia el valor → ninguna rama de fecha).
- **`guardarNota_clienteIntentaDictarFechas_seRestauranDesdeOriginal`** — Tipo: borde (seguridad). Verifica: R-Nota-001/002 (k-secure-coding §3.3).
  - **Arrange:** `nota` entrante con `fechaCalificacion`/`fechaUltimaModificacion` manipuladas por el cliente; `notaOriginal` con sus fechas reales; valor sin cambiar; grupo ABIERTO.
  - **Act:** `guardarNota(nota, notaOriginal)`.
  - **Assert:** las fechas que esta rama no toca quedan igual a las de `notaOriginal` (las del cliente se descartan).
- **`guardarNota_grupoCerrado_lanzaExcepcionYNoGuarda`** — Tipo: error. Verifica: V-Nota-002.
  - **Arrange:** `nota.moduloGrupo.grupo.estado=CERRADO`.
  - **Act:** `guardarNota(nota, notaOriginal)`.
  - **Assert:** lanza excepción con «No se pueden modificar las notas de un grupo cerrado»; `verify(notaRepository, never()).save(any())`.
- **`guardarNota_cuartaMatriculaHonor_lanzaExcepcionYNoGuarda`** — Tipo: error. Verifica: V-Nota-003.
  - **Arrange:** `nota.valor=MATRICULA_HONOR`, `notaOriginal.valor != MH`, grupo ABIERTO; `countMatriculasHonorByModuloGrupo(...)` → `3`.
  - **Act:** `guardarNota(nota, notaOriginal)`.
  - **Assert:** lanza excepción con «No se pueden poner más de 3 matrículas de honor en un módulo»; `verify(notaRepository, never()).save(any())`.

### Método: `allowPropertiesInsert/Update/Remove/GuardarNota()`

- **`allowPropertiesGuardarNota_soloValor`** — Tipo: happy. Verifica: — (frontera de confianza).
  - **Act:** `allowPropertiesGuardarNota()`.
  - **Assert:** permite `valor`; no permite `moduloGrupo`, `alumnoGrupo`, `fechaCalificacion`, `fechaUltimaModificacion`.
- **`allowPropertiesInsert_denyAll`** / **`allowPropertiesUpdate_denyAll`** / **`allowPropertiesRemove_denyAll`** — Tipo: happy. Verifica: —.
  - **Assert:** no permiten ningún campo.

---

> Sobre el getter de dominio `AlumnoGrupo.getNotaMedia()` (cuerpo CDATA con el algoritmo de CC-001 inline): es código generado por Axelor desde el XML, no testeable de forma aislada con JUnit en esta fase; su cálculo queda cubierto por los tests de `AlumnoGrupoServiceImpl.calcularNotaMedia` (que lo invoca con entidades reales) y, en E2E, por ESC-007/008/014 en `test-e2e-desc.md`.

---

## Clase: `com.educaflow.system.gruposnotas.controller.GrupoController`  —  controlador

**Responsabilidad:** botones «Cerrar grupo» y «Reabrir grupo»; extrae bean + original del request y delega en `GrupoService`.
**Colaboradores a mockear:** `ModelServiceFactory` (mock; `resolve(Grupo.class)` → `GrupoService` mock), `ActionRequest`/`ActionResponse` (mock), `ActionRequestHelper` (el controlador lo construye internamente; en los tests se mockea el `ActionRequest` para que `ActionRequestHelper` devuelva el bean/original esperados, o se programa lo que `request.getContext()` provee). `GrupoService` mock con sus `allowPropertiesCerrar/Reabrir()` y `cerrar/reabrir(...)`.
**Origen diseño:** `cerrar(ActionRequest, ActionResponse)`, `reabrir(ActionRequest, ActionResponse)`.

### Método: `cerrar(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`cerrar_delegaEnServicioConBeanYOriginal`** — Tipo: happy. Verifica: — (orquestación; la regla la valida el servicio).
  - **Arrange:** `modelServiceFactory.resolve(Grupo.class)` → `grupoService` mock; `grupoService.allowPropertiesCerrar()` → un `AllowProperties` deny-all; el `ActionRequestHelper` (sobre el `ActionRequest` mockeado) entrega `grupoOriginal` (getOriginalModel) y `grupo` (getModel(allowProperties)); `grupoService.cerrar(grupo, grupoOriginal)` → `grupo` cerrado.
  - **Act:** `cerrar(actionRequest, actionResponse)`.
  - **Assert:** `verify(grupoService).cerrar(grupo, grupoOriginal)`; se usó `allowPropertiesCerrar()` para extraer el bean (`verify(grupoService).allowPropertiesCerrar()`).

### Método: `reabrir(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`reabrir_delegaEnServicioConBeanYOriginal`** — Tipo: happy. Verifica: — (la V-Grupo-008 la valida el servicio).
  - **Arrange:** `resolve(Grupo.class)` → `grupoService`; `allowPropertiesReabrir()` → deny-all; helper entrega `grupoOriginal` y `grupo`; `grupoService.reabrir(grupo, grupoOriginal)` → `grupo` reabierto.
  - **Act:** `reabrir(actionRequest, actionResponse)`.
  - **Assert:** `verify(grupoService).reabrir(grupo, grupoOriginal)` y `verify(grupoService).allowPropertiesReabrir()`.

> Supuesto de mocking de `ActionRequestHelper`: el helper es una clase del proyecto (`base.infrastructure.axelorhelper`) construida con `new ActionRequestHelper(actionRequest, Grupo.class)`. Como se instancia dentro del método, el test programa el `ActionRequest` mock (su `getContext()`/datos) para que el helper resuelva el bean/original; alternativamente, si el implementador prefiere, puede usarse `mockConstruction(ActionRequestHelper.class)` para devolver el bean/original deseados. El objetivo del test es verificar la **delegación** y el uso del `AllowProperties` correcto, no la mecánica interna del helper.

---

## Clase: `com.educaflow.system.gruposnotas.controller.NotaController`  —  controlador

**Responsabilidad:** botón «Guardar» de la nota; extrae bean (whitelist `valor`) + original y delega en `NotaService.guardarNota`.
**Colaboradores a mockear:** `ModelServiceFactory` (mock; `resolve(Nota.class)` → `NotaService` mock), `ActionRequest`/`ActionResponse` (mock), `ActionRequestHelper` (igual que en GrupoController). `NotaService` mock con `allowPropertiesGuardarNota()` y `guardarNota(...)`.
**Origen diseño:** `guardarNota(ActionRequest, ActionResponse)`.

### Método: `guardarNota(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`guardarNota_delegaEnServicioConValorYOriginal`** — Tipo: happy. Verifica: — (las V-Nota las valida el servicio).
  - **Arrange:** `resolve(Nota.class)` → `notaService`; `notaService.allowPropertiesGuardarNota()` → whitelist `valor`; helper entrega `notaOriginal` (getOriginalModel) y `nota` (getModel(allowPropertiesGuardarNota())); `notaService.guardarNota(nota, notaOriginal)` → `nota` guardada.
  - **Act:** `guardarNota(actionRequest, actionResponse)`.
  - **Assert:** `verify(notaService).guardarNota(nota, notaOriginal)` y `verify(notaService).allowPropertiesGuardarNota()` (se extrajo el bean con la whitelist correcta).

---

## Clase: `com.educaflow.system.gruposnotas.db.repo.GrupoRepository` — sin lógica testable unitariamente
**Motivo:** `findByNombreCentroCursoAcademico` lo genera el `finder-method` del dominio (consulta JPA contra la BD). No tiene lógica propia mockeable sin BD real; su comportamiento se cubre indirectamente en los `validate*` del servicio (donde el finder se mockea) y en E2E. Se omite test unitario.

## Clase: `com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository` — sin lógica testable unitariamente
**Motivo:** `existsOtroGrupoMismoCursoAcademico` es una consulta JPQL (`all().filter(...).bind(...).count()`) que requiere BD/EntityManager real; no es testable con JUnit puro sin integración. Su contrato (qué excluye con `excludeAlumnoGrupoId`, filtro por centro+cursoAcademico) se ejercita mockeado en `AlumnoGrupoServiceImpl` y se cubre en E2E (ESC-004). Se omite test unitario.

## Clase: `com.educaflow.system.gruposnotas.db.repo.NotaRepository` — sin lógica testable unitariamente
**Motivo:** `countMatriculasHonorByModuloGrupo` es una query del repo (`all().filter(...).bind(...).count()`) que necesita BD/EntityManager; no testeable con JUnit puro. Su uso se ejercita mockeado en `NotaServiceImpl.validateGuardarNota` y se cubre en E2E (ESC-009). Se omite test unitario.

## Clases: `Grupo`, `ModuloGrupo`, `AlumnoGrupo`, `Nota`, enums `EstadoGrupo`, `ValorNota`, interfaces `GrupoService`/`ModuloGrupoService`/`AlumnoGrupoService`/`NotaService` — sin lógica testable
**Motivo:** POJOs de dominio generados por Axelor (getters/setters; el getter calculado `AlumnoGrupo.getNotaMedia()` lleva el algoritmo de CC-001 inline en su CDATA y se cubre vía `AlumnoGrupoServiceImpl.calcularNotaMedia`), enums sin comportamiento e interfaces sin implementación. No tienen lógica propia que testear unitariamente.

---

## Cobertura

- **Clases con lógica descritas (6):** `GrupoServiceImpl`, `ModuloGrupoServiceImpl`, `AlumnoGrupoServiceImpl`, `NotaServiceImpl`, `GrupoController`, `NotaController` (el algoritmo de CC-001 vive inline en el CDATA del dominio y se cubre vía `AlumnoGrupoServiceImpl.calcularNotaMedia`).
- **Clases omitidas (sin lógica testable):** `GrupoRepository`, `AlumnoGrupoRepository`, `NotaRepository` (queries JPA/JPQL, requieren BD → E2E); `Grupo`, `ModuloGrupo`, `AlumnoGrupo`, `Nota` (POJOs de dominio); enums `EstadoGrupo`, `ValorNota`; interfaces de servicio.
- **Reglas server-side cubiertas (V):** V-Grupo-001, V-Grupo-002, V-Grupo-003, V-Grupo-004, V-Grupo-005, V-Grupo-006, V-Grupo-007, V-Grupo-008, V-Grupo-009, V-ModuloGrupo-001, V-AlumnoGrupo-001, V-AlumnoGrupo-002, V-AlumnoGrupo-003, V-AlumnoGrupo-004, V-AlumnoGrupo-005, V-AlumnoGrupo-006, V-Nota-001, V-Nota-002, V-Nota-003, V-Nota-005.
- **Reglas server-side cubiertas (R):** R-Grupo-001, R-Grupo-002, R-Grupo-003, R-Grupo-004, R-AlumnoGrupo-001, R-AlumnoGrupo-002, R-Nota-001, R-Nota-002.
- **Campos calculados cubiertos (CC):** CC-001 (algoritmo inline en el CDATA del dominio, cubierto vía `AlumnoGrupoServiceImpl.calcularNotaMedia`). CC-002/CC-003 cubiertos vía R-Nota-001/R-Nota-002 (fijado de fechas en `guardarNota`).
- **Reglas solo-cliente excluidas (E2E en test-e2e-desc.md):** U-grupos-supervisor-001 (RUI-001), U-grupos-supervisor-002 (RUI-002), U-grupos-supervisor-003 (RUI-003), U-grupos-supervisor-004 (RUI-004), U-grupos-supervisor-005 (filtro selector alumno; respaldo servidor V-AlumnoGrupo-003), U-grupos-supervisor-006 (RUI-005), U-grupos-administracion-001 (RUI-006), U-grupos-administracion-002 (RUI-008), U-grupos-administracion-003 (RUI-007), U-grupos-administracion-004 (RUI-009), U-grupos-administracion-005 (RUI-010), U-mis-notas-alumno (acceso de rol + `<domain>` + readonly).

## Supuestos documentados
- **Inyección de colaboradores en los `*ServiceImpl`:** el constructor de Axelor es `(Class<T>, Repository<T>)` y los demás colaboradores son campos `@Inject`. Los tests los rellenan con mocks (vía reflexión/`@InjectMocks` adaptado o un constructor de test si el implementador lo añade), sin tocar BD real.
- **Restauración del `grupo` desde el contexto (`R-AlumnoGrupo-002`):** la obtención del id del Grupo padre (`__parent__` del contexto del request) es un detalle del helper/Axelor; los tests programan que la restauración deje `getGrupo()` en el grupo legítimo, y verifican que un `grupo` dictado por el cliente se ignora (asignación incondicional).
- **V-Nota-001 (valor en dominio):** al ser `valor` un enum, el dominio garantiza el rango salvo `null`; el caso testeable a nivel de servicio es `valor=null`. Un literal fuera de dominio no es construible como enum.
- **Cálculo de CC-001:** el algoritmo vive inline en el CDATA de `AlumnoGrupo.notaMedia` (no hay clase utilitaria; la entidad de dominio es un POJO que no puede depender de `..service..`, C13). Se prueba a través de `AlumnoGrupoServiceImpl.calcularNotaMedia`, que delega en el getter generado `getNotaMedia()`, con entidades reales.
