# Tests unitarios

Descripción de los tests unitarios (JUnit 5 + Mockito) por clase y método para el diseño. **Solo descripción, sin código**: `/sdd-implementer` genera el código a partir de aquí. Las reglas que viven solo en la capa cliente/XML (`U-`) no se testean aquí (van como E2E en `test-e2e-desc.md`).

## Convenciones
- JUnit 5 (Jupiter) + Mockito (`MockitoExtension`). Estáticos del stack con `Mockito.mockStatic`.
- Aserciones con `org.junit.jupiter.api.Assertions` (`assertThrows`, `assertEquals`, `assertNull`, `assertNotNull`), no AssertJ. Helper `com.educaflow.base.infrastructure.junit.JUnitHelper.assertThrowsCause` disponible para excepciones envueltas.
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- **Estáticos a mockear** según el diseño: `com.educaflow.base.util.SecurityUtil` (`getUser`, `isAdmin`) y `com.axelor.auth.AuthUtils` (`getUser`) para el usuario/centro/tipos activos; `com.axelor.inject.Beans` (`get`) e/o `ModelServiceFactory` para resolver servicios colaboradores; `com.axelor.i18n.I18n` (`get`) si los mensajes se obtienen vía I18n (programar `I18n.get(x)` → `x`, identidad). El rol del usuario se programa vía `AuthUtils.getUser().getTiposUsuarioActivos()` (lista de `TipoUsuario` con `getCodigo()` `ADMINISTRADOR`/`SUPERVISOR`/`ALUMNO`) y el centro vía `AuthUtils.getUser().getCentroActivo()`.
- **Supuesto documentado:** el diseño dice "validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid)". Los `validate*` devuelven `Optional<BusinessMessages>`. Por tanto **dos estilos de aserción** son válidos y se indican en cada test:
  - sobre el `validate*` directamente → se afirma que el `Optional` viene **presente** y contiene el mensaje literal (caso fallo) o **vacío** (caso OK);
  - sobre la acción pública (`insert`/`update`/`remove`/`cerrarGrupo`/`reabrirGrupo`) → se afirma que lanza la excepción de negocio (`ValidationException` de Axelor, o `Throwable` con `assertThrowsCause`) con el mensaje literal, o que **no** lanza y persiste (`verify(repository).save(...)`).
- **Supuesto documentado:** el momento de lectura CC-001 (cálculo de la nota media) vive **INLINE en el getter `AlumnoGrupo.getNotaMedia()`** del dominio (no en el servicio); `AlumnoGrupoServiceImpl.calcularNotaMedia` solo **delega** en él. Excepción a §1 "no se testean POJOs de dominio": aquí el getter **sí** tiene lógica propia (el algoritmo de CC-001), así que se testea como clase con lógica (la entidad se instancia con `new`, sin mocks, y se le rellenan las notas con setters). El test del servicio comprueba la **delegación**.

---

## Clase: `com.educaflow.system.gruposnotas.service.impl.GrupoServiceImpl`  —  servicio

**Responsabilidad:** alta/modificación/borrado de `Grupo` y acciones `cerrarGrupo`/`reabrirGrupo`; asignación servidor de estado, centro/cursoAcademico (supervisor), generación de módulos del curso, restauración de campos inmutables y validaciones VAL-001..009.
**Colaboradores a mockear:** `GrupoRepository` (mock; finder `findByNombreAndCentroAndCursoAcademico`), `ModelServiceFactory` (mock; resuelve `ModuloGrupoService` para generar módulos — a su vez mock), `SecurityUtil`/`AuthUtils` (estático; usuario, `getCentroActivo`, `getTiposUsuarioActivos`), `I18n` (estático, identidad). `CursoModulo`/finder del curso (mock del repositorio/finder que devuelve los `CursoModulo` del curso). Entidades `Grupo`, `Curso`, `Centro` instanciadas con `new`.
**Origen diseño:** `insert`, `update`, `remove`, `cerrarGrupo`, `reabrirGrupo`, `validateInsert`, `validateUpdate`, `validateRemove`, `validateCerrarGrupo`, `validateReabrirGrupo` y action-rules `fireActionRule_AsignarEstadoInicial`/`AsignarCentroYCursoAcademicoSiSupervisor`/`GenerarModulosGrupo`/`Cerrar`/`Reabrir`/`RestaurarCamposInmutables`. Reglas V-Grupo-001..009, R-Grupo-001..005.

### Método: `Optional<BusinessMessages> validateInsert(Grupo grupo)`

- **`validateInsert_nombreNulo_devuelveError`** — Tipo: error. Verifica: `V-Grupo-001`.
  - **Arrange:** `Grupo` con `nombre=null`, `curso` no nulo, centro/cursoAcademico válidos; `findByNombreAndCentroAndCursoAcademico` → vacío.
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` presente con el mensaje literal **"El nombre del grupo es obligatorio"**.
- **`validateInsert_nombreVacio_devuelveError`** — Tipo: borde. Verifica: `V-Grupo-001`.
  - **Arrange:** `Grupo` con `nombre=""` (o solo espacios), resto válido.
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` presente con **"El nombre del grupo es obligatorio"**.
- **`validateInsert_cursoNulo_devuelveError`** — Tipo: error. Verifica: `V-Grupo-002`.
  - **Arrange:** `Grupo` con `nombre` válido y `curso=null`.
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` presente con **"El curso es obligatorio"**.
- **`validateInsert_nombreDuplicadoEnCentroYCursoAcademico_devuelveError`** — Tipo: error. Verifica: `V-Grupo-003`.
  - **Arrange:** `Grupo` válido; usuario SUPERVISOR con `centroActivo` = centro X y `centro.getCurso()` = 2024; `findByNombreAndCentroAndCursoAcademico(nombre, X, 2024)` → devuelve un grupo existente.
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` presente con **"Ya existe un grupo con ese nombre en este centro y curso académico"**.
- **`validateInsert_nombreUnicoYCamposValidos_devuelveVacio`** — Tipo: happy. Verifica: `V-Grupo-001`,`V-Grupo-002`,`V-Grupo-003`.
  - **Arrange:** `Grupo` con nombre y curso válidos; `findByNombreAndCentroAndCursoAcademico` → vacío; usuario SUPERVISOR con centro activo.
  - **Act:** `validateInsert(grupo)`.
  - **Assert:** `Optional` **vacío**.

### Método: `Optional<BusinessMessages> validateUpdate(Grupo grupo, Grupo original)`

- **`validateUpdate_grupoOriginalCerrado_devuelveError`** — Tipo: error. Verifica: `V-Grupo-004`.
  - **Arrange:** `original` con `estado=CERRADO`; `grupo` con cualquier nombre.
  - **Act:** `validateUpdate(grupo, original)`.
  - **Assert:** `Optional` presente con **"No se puede modificar un grupo cerrado"**.
- **`validateUpdate_nombreDuplicadoOtroGrupo_devuelveError`** — Tipo: error. Verifica: `V-Grupo-005`.
  - **Arrange:** `original` ABIERTO con `id=1`; `grupo` con nombre que ya usa OTRO grupo (`id=2`); `findByNombreAndCentroAndCursoAcademico` → ese otro grupo (id distinto del propio).
  - **Act:** `validateUpdate(grupo, original)`.
  - **Assert:** `Optional` presente con **"Ya existe un grupo con ese nombre en este centro y curso académico"**.
- **`validateUpdate_mismoNombreMismoGrupo_devuelveVacio`** — Tipo: borde. Verifica: `V-Grupo-005`.
  - **Arrange:** `original` ABIERTO con `id=1`; `grupo` con el mismo nombre; `findByNombreAndCentroAndCursoAcademico` → el propio grupo (`id=1`, que se excluye).
  - **Act:** `validateUpdate(grupo, original)`.
  - **Assert:** `Optional` **vacío** (no se confunde el propio id como duplicado).
- **`validateUpdate_grupoAbiertoNombreUnico_devuelveVacio`** — Tipo: happy. Verifica: `V-Grupo-004`,`V-Grupo-005`.
  - **Arrange:** `original` ABIERTO; `grupo` con nombre nuevo; finder → vacío.
  - **Act:** `validateUpdate(grupo, original)`.
  - **Assert:** `Optional` **vacío**.

### Método: `Optional<BusinessMessages> validateRemove(Grupo grupo)`

- **`validateRemove_grupoCerrado_devuelveError`** — Tipo: error. Verifica: `V-Grupo-009`.
  - **Arrange:** `Grupo` con `estado=CERRADO`.
  - **Act:** `validateRemove(grupo)`.
  - **Assert:** `Optional` presente con **"No se puede borrar un grupo cerrado"**.
- **`validateRemove_grupoAbierto_devuelveVacio`** — Tipo: happy. Verifica: `V-Grupo-009`.
  - **Arrange:** `Grupo` con `estado=ABIERTO`.
  - **Act:** `validateRemove(grupo)`.
  - **Assert:** `Optional` **vacío**.

### Método: `Optional<BusinessMessages> validateCerrarGrupo(Grupo grupo, Grupo original)`

- **`validateCerrarGrupo_grupoYaCerrado_devuelveError`** — Tipo: error. Verifica: `V-Grupo-006`.
  - **Arrange:** `original` con `estado=CERRADO`.
  - **Act:** `validateCerrarGrupo(grupo, original)`.
  - **Assert:** `Optional` presente con **"El grupo ya está cerrado"**.
- **`validateCerrarGrupo_grupoAbierto_devuelveVacio`** — Tipo: happy. Verifica: `V-Grupo-006`.
  - **Arrange:** `original` con `estado=ABIERTO`.
  - **Act:** `validateCerrarGrupo(grupo, original)`.
  - **Assert:** `Optional` **vacío**.

### Método: `Optional<BusinessMessages> validateReabrirGrupo(Grupo grupo, Grupo original)`

- **`validateReabrirGrupo_grupoYaAbierto_devuelveError`** — Tipo: error. Verifica: `V-Grupo-007`.
  - **Arrange:** `original` con `estado=ABIERTO`; usuario ADMINISTRADOR (`getTiposUsuarioActivos` con código `ADMINISTRADOR`).
  - **Act:** `validateReabrirGrupo(grupo, original)`.
  - **Assert:** `Optional` presente con **"El grupo ya está abierto"**.
- **`validateReabrirGrupo_usuarioNoAdministrador_devuelveError`** — Tipo: error. Verifica: `V-Grupo-008`.
  - **Arrange:** `original` con `estado=CERRADO`; usuario SUPERVISOR (no administrador): `AuthUtils.getUser().getTiposUsuarioActivos()` → tipos sin `ADMINISTRADOR`.
  - **Act:** `validateReabrirGrupo(grupo, original)`.
  - **Assert:** `Optional` presente con **"No tiene permisos para reabrir el grupo"**.
- **`validateReabrirGrupo_grupoCerradoYUsuarioAdministrador_devuelveVacio`** — Tipo: happy. Verifica: `V-Grupo-007`,`V-Grupo-008`.
  - **Arrange:** `original` `estado=CERRADO`; usuario ADMINISTRADOR.
  - **Act:** `validateReabrirGrupo(grupo, original)`.
  - **Assert:** `Optional` **vacío**.

### Método: `Grupo insert(Grupo grupo)`

- **`insert_supervisor_asignaEstadoAbiertoCentroYCursoAcademicoDelCentroActivo`** — Tipo: happy. Verifica: `R-Grupo-001` (estado), `R-Grupo-002`.
  - **Arrange:** usuario SUPERVISOR con `centroActivo` = centro X y `X.getCurso()` = 2024; `grupo` con `nombre`/`curso` válidos pero `centro`/`cursoAcademico` con valores falsos (p.ej. centro Y, 1999) que el cliente intenta colar; finder de nombre → vacío; `repository.save(grupo)` → devuelve el mismo `grupo`; finder de `CursoModulo` del curso → lista vacía (no probamos módulos aquí); `ModuloGrupoService` mock.
  - **Act:** `insert(grupo)`.
  - **Assert:** `grupo.getEstado()` == `ABIERTO`; `grupo.getCentro()` == X (sobrescrito, NO Y); `grupo.getCursoAcademico()` == 2024 (NO 1999); `verify(repository).save(grupo)`.
- **`insert_administrador_respetaCentroYCursoAcademicoDelCliente`** — Tipo: borde. Verifica: `R-Grupo-002`.
  - **Arrange:** usuario ADMINISTRADOR; `grupo` con `centro`=Y y `cursoAcademico`=2025 aportados por el cliente; finder de nombre → vacío; `repository.save` → identidad.
  - **Act:** `insert(grupo)`.
  - **Assert:** `grupo.getCentro()` == Y y `grupo.getCursoAcademico()` == 2025 (no se sobrescriben para el admin); `grupo.getEstado()` == `ABIERTO`.
- **`insert_generaUnModuloGrupoPorCadaCursoModuloDelCurso`** — Tipo: happy. Verifica: `R-Grupo-001` (módulos, RES-002/RN-001).
  - **Arrange:** usuario SUPERVISOR; `grupo` válido con `curso` C; finder de nombre → vacío; `repository.save` → identidad (grupo con id); finder de `CursoModulo` de C → dos `CursoModulo` (módulos «Programación», «Bases de datos»); `ModelServiceFactory.resolve(...)` / `Beans` → `ModuloGrupoService` mock cuyo `insert(dto)` → un `ModuloGrupo`.
  - **Act:** `insert(grupo)`.
  - **Assert:** `verify(moduloGrupoService, times(2)).insert(any())` (un alta por módulo del curso); la generación ocurre **después** de `repository.save` (`InOrder`: save antes de los `insert` de módulos).
- **`insert_nombreDuplicado_lanzaExcepcionYNoPersiste`** — Tipo: error. Verifica: `V-Grupo-003`.
  - **Arrange:** usuario SUPERVISOR; finder de nombre → grupo existente.
  - **Act:** `insert(grupo)`.
  - **Assert:** lanza la excepción de negocio con **"Ya existe un grupo con ese nombre en este centro y curso académico"**; `verify(repository, never()).save(any())`.

### Método: `Grupo update(Grupo grupo, Grupo original)`

- **`update_restauraCamposInmutablesDesdeOriginal`** — Tipo: borde. Verifica: `R-Grupo-005`.
  - **Arrange:** `original` ABIERTO con `curso`=C1, `centro`=X, `cursoAcademico`=2024, `estado`=ABIERTO, `fechaCierre`=null; `grupo` (entrante del cliente) con `nombre` nuevo y los inmutables manipulados (`curso`=C2, `centro`=Y, `cursoAcademico`=1999, `estado`=CERRADO, `fechaCierre`=algo); finder de nombre → vacío; `repository.save` → identidad.
  - **Act:** `update(grupo, original)`.
  - **Assert:** tras `update`, `grupo.getCurso()`==C1, `getCentro()`==X, `getCursoAcademico()`==2024, `getEstado()`==ABIERTO, `getFechaCierre()`==null (todos restaurados desde `original`); `grupo.getNombre()` conserva el nuevo nombre; `verify(repository).save(grupo)`.
- **`update_grupoCerrado_lanzaExcepcionYNoPersiste`** — Tipo: error. Verifica: `V-Grupo-004`.
  - **Arrange:** `original` `estado=CERRADO`.
  - **Act:** `update(grupo, original)`.
  - **Assert:** lanza con **"No se puede modificar un grupo cerrado"**; `verify(repository, never()).save(any())`.

### Método: `void remove(Grupo grupo)`

- **`remove_grupoCerrado_lanzaExcepcionYNoBorra`** — Tipo: error. Verifica: `V-Grupo-009`.
  - **Arrange:** `Grupo` `estado=CERRADO`.
  - **Act:** `remove(grupo)`.
  - **Assert:** lanza con **"No se puede borrar un grupo cerrado"**; `verify(repository, never()).remove(any())`.
- **`remove_grupoAbierto_borra`** — Tipo: happy. Verifica: `V-Grupo-009`.
  - **Arrange:** `Grupo` `estado=ABIERTO`.
  - **Act:** `remove(grupo)`.
  - **Assert:** `verify(repository).remove(grupo)`.

### Método: `Grupo cerrarGrupo(Grupo grupo, Grupo original)`

- **`cerrarGrupo_grupoAbierto_poneEstadoCerradoYFechaCierre`** — Tipo: happy. Verifica: `R-Grupo-003`,`V-Grupo-006`.
  - **Arrange:** `original` ABIERTO; `grupo` ABIERTO; `repository.save` → identidad.
  - **Act:** `cerrarGrupo(grupo, original)`.
  - **Assert:** `grupo.getEstado()` == `CERRADO`; `grupo.getFechaCierre()` != null (fecha asignada por el servidor); `verify(repository).save(grupo)`.
- **`cerrarGrupo_grupoYaCerrado_lanzaExcepcion`** — Tipo: error. Verifica: `V-Grupo-006`.
  - **Arrange:** `original` `estado=CERRADO`.
  - **Act:** `cerrarGrupo(grupo, original)`.
  - **Assert:** lanza con **"El grupo ya está cerrado"**; `verify(repository, never()).save(any())`.

### Método: `Grupo reabrirGrupo(Grupo grupo, Grupo original)`

- **`reabrirGrupo_grupoCerradoYAdministrador_poneEstadoAbiertoYBorraFechaCierre`** — Tipo: happy. Verifica: `R-Grupo-004`,`V-Grupo-007`,`V-Grupo-008`.
  - **Arrange:** usuario ADMINISTRADOR; `original` `estado=CERRADO` con `fechaCierre` no nulo; `grupo` igual; `repository.save` → identidad.
  - **Act:** `reabrirGrupo(grupo, original)`.
  - **Assert:** `grupo.getEstado()` == `ABIERTO`; `grupo.getFechaCierre()` == null; `verify(repository).save(grupo)`.
- **`reabrirGrupo_usuarioNoAdministrador_lanzaExcepcion`** — Tipo: error. Verifica: `V-Grupo-008`.
  - **Arrange:** usuario SUPERVISOR (no admin); `original` `estado=CERRADO`.
  - **Act:** `reabrirGrupo(grupo, original)`.
  - **Assert:** lanza con **"No tiene permisos para reabrir el grupo"**; `verify(repository, never()).save(any())`.
- **`reabrirGrupo_grupoYaAbierto_lanzaExcepcion`** — Tipo: error. Verifica: `V-Grupo-007`.
  - **Arrange:** usuario ADMINISTRADOR; `original` `estado=ABIERTO`.
  - **Act:** `reabrirGrupo(grupo, original)`.
  - **Assert:** lanza con **"El grupo ya está abierto"**; `verify(repository, never()).save(any())`.

### Método: `AllowProperties allowPropertiesInsert()` / `allowPropertiesUpdate()` / `allowPropertiesCerrarGrupo()` / `allowPropertiesReabrirGrupo()`

- **`allowPropertiesInsert_incluyeSoloCamposClienteYExcluyeAlumnosGrupoYServidor`** — Tipo: borde. Verifica: `—` (frontera de confianza, k-secure-coding).
  - **Arrange:** instancia el servicio.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty("nombre")`, `allowProperty("curso")`, `allowProperty("centro")`, `allowProperty("cursoAcademico")` == true; `allowProperty("alumnosGrupo")`, `allowProperty("estado")`, `allowProperty("fechaCierre")`, `allowProperty("modulosGrupo")` == false.
- **`allowPropertiesUpdate_incluyeSoloNombre`** — Tipo: borde. Verifica: `—`.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty("nombre")` == true; `curso`/`centro`/`cursoAcademico`/`estado`/`fechaCierre` == false.
- **`allowPropertiesCerrarGrupo_yReabrirGrupo_whitelistVacia`** — Tipo: borde. Verifica: `—`.
  - **Act:** `allowPropertiesCerrarGrupo()` y `allowPropertiesReabrirGrupo()`.
  - **Assert:** ninguna propiedad de `Grupo` permitida (whitelist vacía; `allowProperty("estado")`/`allowProperty("fechaCierre")`/`allowProperty("nombre")` == false).

---

## Clase: `com.educaflow.system.gruposnotas.service.impl.AlumnoGrupoServiceImpl`  —  servicio

**Responsabilidad:** alta/baja de `AlumnoGrupo`, creación de notas NO_EVALUADO al añadir, validaciones VAL-010..014/018/019 y delegación de `calcularNotaMedia` en el getter del dominio.
**Colaboradores a mockear:** `AlumnoGrupoRepository` (mock; finder `findByAlumnoAndGrupoCursoAcademico`), `ModelServiceFactory` (mock; resuelve `NotaService`), `NotaService` (mock), método/finder ad-hoc sobre `CentroUsuario`/`CentroUsuarioTipoUsuario` (mock de la consulta de tipo de usuario; se programa "el alumno es ALUMNO del centro" → true/false), `SecurityUtil`/`AuthUtils` (estático; usuario, rol, `getCentroActivo`), `I18n` (identidad). Entidades `AlumnoGrupo`, `Grupo`, `Centro`, `User`, `ModuloGrupo` instanciadas con `new`.
**Origen diseño:** `insert`, `remove`, `validateInsert`, `validateRemove`, `calcularNotaMedia`, action-rule `fireActionRule_CrearNotasNoEvaluado`. Reglas V-AlumnoGrupo-002..008, R-AlumnoGrupo-001, CC-001 (delegado).

### Método: `Optional<BusinessMessages> validateInsert(AlumnoGrupo alumnoGrupo)`

- **`validateInsert_grupoNulo_devuelveError`** — Tipo: error. Verifica: `V-AlumnoGrupo-007`.
  - **Arrange:** `AlumnoGrupo` con `grupo=null` y `alumno` no nulo.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con **"El grupo es obligatorio"**.
- **`validateInsert_alumnoNulo_devuelveError`** — Tipo: error. Verifica: `V-AlumnoGrupo-002`.
  - **Arrange:** `AlumnoGrupo` con `grupo` no nulo (ABIERTO) y `alumno=null`.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con **"Debe elegir un alumno"**.
- **`validateInsert_grupoCerrado_devuelveError`** — Tipo: error. Verifica: `V-AlumnoGrupo-003`.
  - **Arrange:** `grupo` con `estado=CERRADO`; `alumno` no nulo.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con **"No se pueden añadir alumnos a un grupo cerrado"**.
- **`validateInsert_supervisorGrupoDeOtroCentro_devuelveError`** — Tipo: error. Verifica: `V-AlumnoGrupo-008`.
  - **Arrange:** usuario SUPERVISOR con `centroActivo`=X; `grupo` ABIERTO con `centro`=Y (otro centro); `alumno` no nulo.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con **"El grupo no pertenece a su centro"**.
- **`validateInsert_alumnoNoEsAlumnoDelCentroDelGrupo_devuelveError`** — Tipo: error. Verifica: `V-AlumnoGrupo-004`.
  - **Arrange:** `grupo` ABIERTO con `centro`=X; usuario SUPERVISOR con `centroActivo`=X; la consulta `CentroUsuario`/`CentroUsuarioTipoUsuario` (mockeada) indica que el `alumno` NO es ALUMNO del centro X (→ false); finder de curso académico → vacío.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con **"El alumno debe ser un usuario de tipo Alumno del centro del grupo"**.
- **`validateInsert_alumnoYaEnOtroGrupoMismoCursoAcademico_devuelveError`** — Tipo: error. Verifica: `V-AlumnoGrupo-005`.
  - **Arrange:** `grupo` ABIERTO con `centro`=X y `cursoAcademico`=2024; consulta de tipo de usuario → true (sí es ALUMNO de X); `findByAlumnoAndGrupoCursoAcademico(alumno, 2024)` (excluyendo el propio id) → devuelve un `AlumnoGrupo` de otro grupo.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` presente con **"El alumno ya pertenece a otro grupo de este curso académico"**.
- **`validateInsert_todoValido_devuelveVacio`** — Tipo: happy. Verifica: `V-AlumnoGrupo-002`,`-003`,`-004`,`-005`,`-007`,`-008`.
  - **Arrange:** `grupo` ABIERTO con `centro`=X; usuario SUPERVISOR con `centroActivo`=X; `alumno` no nulo; consulta de tipo de usuario → true; finder de curso académico → vacío.
  - **Act:** `validateInsert(alumnoGrupo)`.
  - **Assert:** `Optional` **vacío**.

### Método: `Optional<BusinessMessages> validateRemove(AlumnoGrupo alumnoGrupo)`

- **`validateRemove_grupoCerrado_devuelveError`** — Tipo: error. Verifica: `V-AlumnoGrupo-006`.
  - **Arrange:** `alumnoGrupo` cuyo `grupo` tiene `estado=CERRADO`.
  - **Act:** `validateRemove(alumnoGrupo)`.
  - **Assert:** `Optional` presente con **"No se pueden quitar alumnos de un grupo cerrado"**.
- **`validateRemove_grupoAbierto_devuelveVacio`** — Tipo: happy. Verifica: `V-AlumnoGrupo-006`.
  - **Arrange:** `grupo` `estado=ABIERTO`.
  - **Act:** `validateRemove(alumnoGrupo)`.
  - **Assert:** `Optional` **vacío**.

### Método: `AlumnoGrupo insert(AlumnoGrupo alumnoGrupo)`

- **`insert_creaUnaNotaNoEvaluadoPorCadaModuloDelGrupo`** — Tipo: happy. Verifica: `R-AlumnoGrupo-001`.
  - **Arrange:** usuario SUPERVISOR con `centroActivo`=X; `grupo` ABIERTO `centro`=X con dos `ModuloGrupo` (consulta de módulos del grupo → lista de 2); validaciones todas OK (consulta tipo usuario → true; finder curso académico → vacío); `repository.save(alumnoGrupo)` → identidad (con id); `ModelServiceFactory.resolve(...)`/`Beans` → `NotaService` mock con `insert(dto)` → `Nota`.
  - **Act:** `insert(alumnoGrupo)`.
  - **Assert:** `verify(notaService, times(2)).insert(any())`; `InOrder`: `repository.save` antes de los `insert` de notas; `verify(repository).save(alumnoGrupo)`.
- **`insert_alumnoNoElegido_lanzaExcepcionYNoPersiste`** — Tipo: error. Verifica: `V-AlumnoGrupo-002`.
  - **Arrange:** `alumno=null`; `grupo` ABIERTO.
  - **Act:** `insert(alumnoGrupo)`.
  - **Assert:** lanza con **"Debe elegir un alumno"**; `verify(repository, never()).save(any())`; `verify(notaService, never()).insert(any())`.

### Método: `void remove(AlumnoGrupo alumnoGrupo)`

- **`remove_grupoCerrado_lanzaExcepcionYNoBorra`** — Tipo: error. Verifica: `V-AlumnoGrupo-006`.
  - **Arrange:** `grupo` `estado=CERRADO`.
  - **Act:** `remove(alumnoGrupo)`.
  - **Assert:** lanza con **"No se pueden quitar alumnos de un grupo cerrado"**; `verify(repository, never()).remove(any())`.
- **`remove_grupoAbierto_borra`** — Tipo: happy. Verifica: `V-AlumnoGrupo-006`.
  - **Arrange:** `grupo` `estado=ABIERTO`.
  - **Act:** `remove(alumnoGrupo)`.
  - **Assert:** `verify(repository).remove(alumnoGrupo)`.

### Método: `String calcularNotaMedia(AlumnoGrupo alumnoGrupo)`

- **`calcularNotaMedia_delegaEnGetterDelDominio`** — Tipo: happy. Verifica: `CC-001` (delegación).
  - **Arrange:** `AlumnoGrupo` real (`new`) con una `Nota` `valor=NOTA_8`; sin mocks (el cálculo vive en el dominio).
  - **Act:** `calcularNotaMedia(alumnoGrupo)`.
  - **Assert:** devuelve exactamente lo que devuelve `alumnoGrupo.getNotaMedia()` (== "8"); el método del servicio no duplica el cálculo (única fuente de verdad en el dominio).

### Método: `AllowProperties allowPropertiesInsert()` / `allowPropertiesUpdate()`

- **`allowPropertiesInsert_incluyeGrupoYAlumnoYExcluyeServidor`** — Tipo: borde. Verifica: `—`.
  - **Act:** `allowPropertiesInsert()`.
  - **Assert:** `allowProperty("grupo")`, `allowProperty("alumno")` == true; `allowProperty("notas")`, `allowProperty("notaMedia")` == false.
- **`allowPropertiesUpdate_whitelistVacia`** — Tipo: borde. Verifica: `—`.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty("grupo")`, `allowProperty("alumno")` == false (no se reparenta).

---

## Clase: `com.educaflow.system.gruposnotas.service.impl.NotaServiceImpl`  —  servicio

**Responsabilidad:** alta programática (NO_EVALUADO) y modificación de `Nota`; validaciones VAL-015/016/017, asignación de `fechaCalificacion`/`fechaUltimaModificacion` y restauración de campos inmutables.
**Colaboradores a mockear:** `NotaRepository` (mock; finder `countMatriculasHonorByModuloGrupo`), `I18n` (identidad). Entidades `Nota`, `ModuloGrupo`, `Grupo`, `AlumnoGrupo` instanciadas con `new`.
**Origen diseño:** `insert(NotaInsertDTO)`, `update`, `validateInsert(NotaInsertDTO)`, `validateUpdate`, action-rules `fireActionRule_AsignarFechaCalificacion`/`AsignarFechaUltimaModificacion`/`RestaurarCamposInmutables`. Reglas V-Nota-002..004, R-Nota-001..003.

### Método: `Optional<BusinessMessages> validateUpdate(Nota nota, Nota original)`

- **`validateUpdate_grupoCerrado_devuelveError`** — Tipo: error. Verifica: `V-Nota-002`.
  - **Arrange:** `nota` cuyo `moduloGrupo.grupo` tiene `estado=CERRADO`; `original` con valor previo.
  - **Act:** `validateUpdate(nota, original)`.
  - **Assert:** `Optional` presente con **"No se pueden modificar las notas de un grupo cerrado"**.
- **`validateUpdate_valorFueraDelDominio_devuelveError`** — Tipo: error. Verifica: `V-Nota-003`.
  - **Arrange:** `nota` con `valor=null` (o valor no perteneciente al enum `ValorNota`, simulando un valor crudo por /ws/rest); `grupo` ABIERTO.
  - **Act:** `validateUpdate(nota, original)`.
  - **Assert:** `Optional` presente con **"La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor"**.
- **`validateUpdate_cuartaMatriculaHonorEnModulo_devuelveError`** — Tipo: borde. Verifica: `V-Nota-004`.
  - **Arrange:** `nota` con `valor=MATRICULA_HONOR`, `grupo` ABIERTO; `countMatriculasHonorByModuloGrupo(moduloGrupo)` (excluyendo la nota actual) → ya hay 3 MH.
  - **Act:** `validateUpdate(nota, original)`.
  - **Assert:** `Optional` presente con **"No se pueden poner más de 3 matrículas de honor en un módulo"**.
- **`validateUpdate_terceraMatriculaHonorEnModulo_devuelveVacio`** — Tipo: borde. Verifica: `V-Nota-004`.
  - **Arrange:** `nota` con `valor=MATRICULA_HONOR`, `grupo` ABIERTO; `countMatriculasHonorByModuloGrupo` → 2 MH existentes (esta sería la 3ª, permitida).
  - **Act:** `validateUpdate(nota, original)`.
  - **Assert:** `Optional` **vacío** (el tope es 3, no menos).
- **`validateUpdate_valorNumericoValidoGrupoAbierto_devuelveVacio`** — Tipo: happy. Verifica: `V-Nota-002`,`-003`,`-004`.
  - **Arrange:** `nota` con `valor=NOTA_8`, `grupo` ABIERTO (no es MH, no se consulta el contador o devuelve 0).
  - **Act:** `validateUpdate(nota, original)`.
  - **Assert:** `Optional` **vacío**.

### Método: `Optional<BusinessMessages> validateInsert(NotaInsertDTO dto)`

- **`validateInsert_dtoCompleto_devuelveVacio`** — Tipo: happy. Verifica: `—` (integridad del DTO).
  - **Arrange:** `NotaInsertDTO` con `moduloGrupo` y `alumnoGrupo` no nulos.
  - **Act:** `validateInsert(dto)`.
  - **Assert:** `Optional` **vacío**.
- **`validateInsert_moduloGrupoNulo_devuelveError`** — Tipo: error. Verifica: `—` (integridad del DTO).
  - **Arrange:** `NotaInsertDTO` con `moduloGrupo=null`, `alumnoGrupo` no nulo.
  - **Act:** `validateInsert(dto)`.
  - **Assert:** `Optional` presente (integridad; mensaje el que defina el diseño, sin literal del spec).

### Método: `Nota insert(NotaInsertDTO dto)`

- **`insert_creaNotaNoEvaluadoSinFechas`** — Tipo: happy. Verifica: `—` (alta programática NO_EVALUADO; integridad del DTO. R-AlumnoGrupo-001/RN-005 vive en `AlumnoGrupoServiceImpl.fireActionRule_CrearNotasNoEvaluado`, que es quien invoca este `insert`).
  - **Arrange:** `NotaInsertDTO(moduloGrupo, alumnoGrupo)` válidos; `repository.save(any())` → la `Nota` guardada.
  - **Act:** `insert(dto)`.
  - **Assert:** la `Nota` guardada tiene `valor==NO_EVALUADO`, `fechaCalificacion==null`, `fechaUltimaModificacion==null`; `verify(repository).save(any())`.

### Método: `Nota update(Nota nota, Nota original)`

- **`update_primeraCalificacion_asignaFechaCalificacionYNoUltimaModificacion`** — Tipo: happy. Verifica: `R-Nota-001`.
  - **Arrange:** `original` con `valor=NO_EVALUADO`, `fechaCalificacion=null`; `nota` (entrante) con `valor=NOTA_8`, `grupo` ABIERTO; `countMatriculasHonorByModuloGrupo`→0; `repository.save`→identidad.
  - **Act:** `update(nota, original)`.
  - **Assert:** `nota.getFechaCalificacion()` != null (asignada ahora); `nota.getFechaUltimaModificacion()` == null (no era una recalificación); `verify(repository).save(nota)`.
- **`update_recalificaNotaYaCalificada_asignaFechaUltimaModificacion`** — Tipo: borde. Verifica: `R-Nota-002`.
  - **Arrange:** `original` con `valor=NOTA_8` y `fechaCalificacion` no nulo (fecha previa F); `nota` con `valor=NOTA_6`, `grupo` ABIERTO; `repository.save`→identidad.
  - **Act:** `update(nota, original)`.
  - **Assert:** `nota.getFechaUltimaModificacion()` != null (asignada por el cambio); `nota.getFechaCalificacion()` conserva F (no se reasigna porque ya estaba calificada); `verify(repository).save(nota)`.
- **`update_restauraModuloGrupoYAlumnoGrupoDesdeOriginal`** — Tipo: borde. Verifica: `R-Nota-003`.
  - **Arrange:** `original` con `moduloGrupo`=MG1, `alumnoGrupo`=AG1; `nota` (entrante manipulada) con `moduloGrupo`=MG2, `alumnoGrupo`=AG2 y `valor=NOTA_7`; `grupo` ABIERTO; `repository.save`→identidad.
  - **Act:** `update(nota, original)`.
  - **Assert:** tras `update`, `nota.getModuloGrupo()`==MG1 y `nota.getAlumnoGrupo()`==AG1 (restaurados; no se reparenta la nota).
- **`update_grupoCerrado_lanzaExcepcionYNoPersiste`** — Tipo: error. Verifica: `V-Nota-002`.
  - **Arrange:** `nota` con `moduloGrupo.grupo.estado=CERRADO`.
  - **Act:** `update(nota, original)`.
  - **Assert:** lanza con **"No se pueden modificar las notas de un grupo cerrado"**; `verify(repository, never()).save(any())`.

### Método: `AllowProperties allowPropertiesUpdate()`

- **`allowPropertiesUpdate_incluyeSoloValor`** — Tipo: borde. Verifica: `—`.
  - **Act:** `allowPropertiesUpdate()`.
  - **Assert:** `allowProperty("valor")` == true; `allowProperty("moduloGrupo")`, `allowProperty("alumnoGrupo")`, `allowProperty("fechaCalificacion")`, `allowProperty("fechaUltimaModificacion")` == false.

---

## Clase: `com.educaflow.system.gruposnotas.service.impl.ModuloGrupoServiceImpl`  —  servicio

**Responsabilidad:** alta programática de `ModuloGrupo` vía `ModuloGrupoInsertDTO` (la invoca `GrupoServiceImpl` al generar los módulos del grupo).
**Colaboradores a mockear:** `Repository<ModuloGrupo>` (mock). Entidades `Grupo`, `Modulo`, `ModuloGrupo` instanciadas con `new`.
**Origen diseño:** `insert(ModuloGrupoInsertDTO)`, `validateInsert(ModuloGrupoInsertDTO)`. Sin reglas de negocio del spec (RES-003 lo garantiza el unique-constraint del dominio).

### Método: `Optional<BusinessMessages> validateInsert(ModuloGrupoInsertDTO dto)`

- **`validateInsert_dtoCompleto_devuelveVacio`** — Tipo: happy. Verifica: `—` (integridad del DTO).
  - **Arrange:** `ModuloGrupoInsertDTO` con `grupo` y `modulo` no nulos.
  - **Act:** `validateInsert(dto)`.
  - **Assert:** `Optional` **vacío**.
- **`validateInsert_grupoNulo_devuelveError`** — Tipo: error. Verifica: `—` (integridad del DTO).
  - **Arrange:** `dto` con `grupo=null`, `modulo` no nulo.
  - **Act:** `validateInsert(dto)`.
  - **Assert:** `Optional` presente.
- **`validateInsert_moduloNulo_devuelveError`** — Tipo: error. Verifica: `—` (integridad del DTO).
  - **Arrange:** `dto` con `grupo` no nulo, `modulo=null`.
  - **Act:** `validateInsert(dto)`.
  - **Assert:** `Optional` presente.

### Método: `ModuloGrupo insert(ModuloGrupoInsertDTO dto)`

- **`insert_dtoValido_construyeYGuardaModuloGrupo`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `dto(grupo G, modulo M)`; `repository.save(any())` → la entidad guardada.
  - **Act:** `insert(dto)`.
  - **Assert:** la `ModuloGrupo` guardada tiene `getGrupo()`==G y `getModulo()`==M; `verify(repository).save(any())`.

---

## Clase: `com.educaflow.system.gruposnotas.db.AlumnoGrupo` — getter `getNotaMedia()`  —  dominio con lógica (CC-001)

**Responsabilidad:** calcular INLINE la nota media (CC-001, momento lectura) recorriendo `this.getNotas()`, mapeando `MATRICULA_HONOR`→10 y `NOTA_n`→n, excluyendo `NO_EVALUADO`; devolviendo el entero más cercano como texto o "Sin nota" si no hay ninguna nota evaluada. Sin dependencia de `..service..` ni `Beans.get`.
**Colaboradores a mockear:** ninguno — entidad POJO instanciada con `new`; las `Nota` se crean con `new` y se asocian con setters/colección. Excepción justificada a §1: este getter contiene la lógica de CC-001, así que se testea aquí (única fuente de verdad).
**Origen diseño:** `domains/AlumnoGrupo.xml` (transient `notaMedia`, getter computado); `Notas y supuestos` punto 4; CC-001.

### Método: `String getNotaMedia()`

- **`getNotaMedia_sinModulosEvaluados_devuelveSinNota`** — Tipo: borde. Verifica: `CC-001`.
  - **Arrange:** `AlumnoGrupo` con dos `Nota` `valor=NO_EVALUADO` (o sin notas).
  - **Act:** `getNotaMedia()`.
  - **Assert:** devuelve exactamente **"Sin nota"** (ESC-008).
- **`getNotaMedia_unaNotaNumerica_devuelveEseValor`** — Tipo: happy. Verifica: `CC-001`.
  - **Arrange:** `AlumnoGrupo` con una `Nota` `valor=NOTA_8`.
  - **Act:** `getNotaMedia()`.
  - **Assert:** devuelve **"8"** (ESC-006).
- **`getNotaMedia_matriculaHonorMasNoEvaluado_cuentaMH10YExcluyeNoEvaluado`** — Tipo: borde. Verifica: `CC-001`.
  - **Arrange:** `AlumnoGrupo` con una `Nota` `valor=MATRICULA_HONOR` y otra `valor=NO_EVALUADO`.
  - **Act:** `getNotaMedia()`.
  - **Assert:** devuelve **"10"** (la MH cuenta 10, el NO_EVALUADO no se promedia — ESC-007).
- **`getNotaMedia_matriculaHonorY7_devuelveMediaRedondeada9`** — Tipo: borde. Verifica: `CC-001`.
  - **Arrange:** `AlumnoGrupo` con `Nota` `valor=MATRICULA_HONOR` (10) y `Nota` `valor=NOTA_7`.
  - **Act:** `getNotaMedia()`.
  - **Assert:** devuelve **"9"** (media de 10 y 7 = 8,5, redondeada al entero más cercano = 9 — ESC-007 paso 5).
- **`getNotaMedia_ochoYSeis_devuelveMedia7`** — Tipo: happy. Verifica: `CC-001`.
  - **Arrange:** `AlumnoGrupo` con `Nota` `valor=NOTA_8` y `Nota` `valor=NOTA_6`.
  - **Act:** `getNotaMedia()`.
  - **Assert:** devuelve **"7"** (media de 8 y 6 — ESC-014).
- **`getNotaMedia_coleccionNotasNula_devuelveSinNota`** — Tipo: borde. Verifica: `CC-001`.
  - **Arrange:** `AlumnoGrupo` recién instanciado sin inicializar `notas` (o lista vacía).
  - **Act:** `getNotaMedia()`.
  - **Assert:** devuelve **"Sin nota"** (sin NPE).

---

## Clase: `com.educaflow.system.gruposnotas.controller.GrupoController`  —  controlador

**Responsabilidad:** botones «Cerrar grupo» / «Reabrir grupo»: resuelve `GrupoService`, obtiene el modelo (con su `AllowProperties`) y el original, valida y, si OK, ejecuta la acción y refresca; si hay errores, los responde como business messages. No contiene lógica de negocio ni de rol (vive en el servicio).
**Colaboradores a mockear:** `ActionRequest`/`ActionResponse` (mock), `ModelServiceFactory` (mock; `resolve(Grupo.class)` → `GrupoService` mock), `GrupoService` (mock), `ActionRequestHelper`/`ActionResponseHelper` si se usan (mock o estático), `SecurityUtil`/`AuthUtils` si el helper lo necesita.
**Origen diseño:** `cerrarGrupo(ActionRequest, ActionResponse)`, `reabrirGrupo(ActionRequest, ActionResponse)`.

### Método: `void cerrarGrupo(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`cerrarGrupo_sinErroresDeValidacion_ejecutaCerrarYRefresca`** — Tipo: happy. Verifica: `—` (orquestación; R-Grupo-003/V-Grupo-006 viven en el servicio).
  - **Arrange:** `modelServiceFactory.resolve(Grupo.class)` → `grupoService`; del request se obtienen `original` (estado actual) y `grupo` (con `allowPropertiesCerrarGrupo`); `grupoService.validateCerrarGrupo(grupo, original)` → `Optional.empty()`.
  - **Act:** `cerrarGrupo(actionRequest, actionResponse)`.
  - **Assert:** `verify(grupoService).cerrarGrupo(grupo, original)`; `verify(actionResponse)` setea la señal de refresh/reload; NO se responde error.
- **`cerrarGrupo_conErroresDeValidacion_respondeBusinessMessagesYNoCierra`** — Tipo: error. Verifica: `V-Grupo-006`.
  - **Arrange:** `grupoService.validateCerrarGrupo(grupo, original)` → `Optional` presente con un `BusinessMessages` (p.ej. «El grupo ya está cerrado»).
  - **Act:** `cerrarGrupo(actionRequest, actionResponse)`.
  - **Assert:** se responde el error de negocio en `actionResponse` (vía `doResponseBusinessMessagesAsError` / `setError`); `verify(grupoService, never()).cerrarGrupo(any(), any())`.

### Método: `void reabrirGrupo(ActionRequest actionRequest, ActionResponse actionResponse)`

- **`reabrirGrupo_sinErroresDeValidacion_ejecutaReabrirYRefresca`** — Tipo: happy. Verifica: `—`.
  - **Arrange:** `grupoService.validateReabrirGrupo(grupo, original)` → `Optional.empty()`.
  - **Act:** `reabrirGrupo(actionRequest, actionResponse)`.
  - **Assert:** `verify(grupoService).reabrirGrupo(grupo, original)`; `actionResponse` refresca.
- **`reabrirGrupo_conErroresDeValidacion_respondeBusinessMessagesYNoReabre`** — Tipo: error. Verifica: `V-Grupo-007`/`V-Grupo-008`.
  - **Arrange:** `grupoService.validateReabrirGrupo(grupo, original)` → `Optional` presente con un `BusinessMessages` (p.ej. «No tiene permisos para reabrir el grupo»).
  - **Act:** `reabrirGrupo(actionRequest, actionResponse)`.
  - **Assert:** se responde el error de negocio; `verify(grupoService, never()).reabrirGrupo(any(), any())`.

---

## Clase: `com.educaflow.system.gruposnotas.db.repo.GrupoRepository` — sin lógica testable
**Motivo:** repositorio concreto que solo hereda `AbstractGrupoRepository` con el finder generado del dominio (`findByNombreAndCentroAndCursoAcademico`); sin métodos propios. La lógica que lo usa se testea en `GrupoServiceImpl` (mockeando el repositorio).

## Clase: `com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository` — sin lógica testable
**Motivo:** ídem; solo hereda `findByAlumnoAndGrupoCursoAcademico` generado. Se testea en `AlumnoGrupoServiceImpl`.

## Clase: `com.educaflow.system.gruposnotas.db.repo.NotaRepository` — sin lógica testable
**Motivo:** ídem; solo hereda `countMatriculasHonorByModuloGrupo` generado. Se testea en `NotaServiceImpl`.

## Clase: `com.educaflow.system.gruposnotas.db.Grupo` / `ModuloGrupo` / `Nota` — sin lógica testable
**Motivo:** POJOs de dominio generados por Axelor sin lógica propia (`AlumnoGrupo.getNotaMedia()` es la única excepción, testeada arriba). Enums `EstadoGrupo`/`ValorNota` sin comportamiento. Los demás campos servidor se ejercen desde los tests de servicio.

## Clase: interfaces `GrupoService`/`ModuloGrupoService`/`AlumnoGrupoService`/`NotaService` y records `ModuloGrupoInsertDTO`/`NotaInsertDTO` — sin lógica testable
**Motivo:** interfaces y DTOs (records) sin comportamiento; su uso se ejerce desde las `*Impl`.

---

## Cobertura

- **Clases con lógica descritas:** 6 — `GrupoServiceImpl`, `AlumnoGrupoServiceImpl`, `NotaServiceImpl`, `ModuloGrupoServiceImpl`, `AlumnoGrupo` (getter `getNotaMedia()`, lógica CC-001), `GrupoController`.
- **Clases omitidas (sin lógica testable):** `GrupoRepository`, `AlumnoGrupoRepository`, `NotaRepository` (solo finders heredados); POJOs `Grupo`/`ModuloGrupo`/`Nota` y enums `EstadoGrupo`/`ValorNota`; interfaces de servicio; records `ModuloGrupoInsertDTO`/`NotaInsertDTO`.
- **Reglas server-side cubiertas (`V`/`R`/`CC`):**
  - V: V-Grupo-001, V-Grupo-002, V-Grupo-003, V-Grupo-004, V-Grupo-005, V-Grupo-006, V-Grupo-007, V-Grupo-008, V-Grupo-009, V-AlumnoGrupo-002, V-AlumnoGrupo-003, V-AlumnoGrupo-004, V-AlumnoGrupo-005, V-AlumnoGrupo-006, V-AlumnoGrupo-007, V-AlumnoGrupo-008, V-Nota-002, V-Nota-003, V-Nota-004.
  - R: R-Grupo-001, R-Grupo-002, R-Grupo-003, R-Grupo-004, R-Grupo-005, R-AlumnoGrupo-001, R-Nota-001, R-Nota-002, R-Nota-003.
  - CC: CC-001 (getter del dominio + delegación en `AlumnoGrupoServiceImpl.calcularNotaMedia`).
  - **No testeadas como unitario (garantizadas por `unique-constraint` del dominio, no por código Java):** V-ModuloGrupo-001 (RES-003), V-AlumnoGrupo-001 (RES-005), V-Nota-001 (RES-006). CC-002/CC-003 se cubren a través de R-Nota-001/R-Nota-002 (mismas action-rules `fechaCalificacion`/`fechaUltimaModificacion`).
- **Reglas solo-cliente excluidas (E2E en `test-e2e-desc.md`):** U-grupos-supervisor-001, U-grupos-supervisor-002, U-grupos-supervisor-003, U-grupos-supervisor-004, U-grupos-supervisor-005, U-grupos-supervisor-006, U-grupos-administrador-001, U-grupos-administrador-002, U-grupos-administrador-003, U-grupos-administrador-004, U-grupos-administrador-005, U-grupos-administrador-006.
</content>
</invoke>
