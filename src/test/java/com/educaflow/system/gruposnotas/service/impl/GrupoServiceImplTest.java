package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.sistemaeducativo.db.Curso;
import com.educaflow.subsystem.sistemaeducativo.db.CursoModulo;
import com.educaflow.subsystem.sistemaeducativo.db.Modulo;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.GrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.ModuloGrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.NotaRepository;
import com.educaflow.system.gruposnotas.service.AlumnoGrupoService;
import com.educaflow.system.gruposnotas.service.ModuloGrupoInsertDTO;
import com.educaflow.system.gruposnotas.service.ModuloGrupoService;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrupoServiceImplTest {

    private GrupoRepository repository;
    private ModelServiceFactory modelServiceFactory;
    private GrupoServiceImpl service;

    private MockedStatic<SecurityUtil> securityUtilMock;
    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() throws Exception {
        repository = Mockito.mock(GrupoRepository.class);
        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);

        service = new GrupoServiceImpl(Grupo.class, repository);

        inyectarCampo("modelServiceFactory", modelServiceFactory);

        // La nueva lógica de generación de notas NO_EVALUADO (fireActionRule_GenerarNotasNoEvaluadoFaltantes)
        // que corren insert/update consume estos tres repositorios. Se inyectan como mocks: sus finders
        // findByGrupo devuelven List, por lo que Mockito retorna lista vacía por defecto y el barrido
        // módulos×alumnos no se ejecuta (sin notas que generar) ni provoca NPE.
        inyectarCampo("moduloGrupoRepository", Mockito.mock(ModuloGrupoRepository.class));
        inyectarCampo("alumnoGrupoRepository", Mockito.mock(AlumnoGrupoRepository.class));
        inyectarCampo("notaRepository", Mockito.mock(NotaRepository.class));

        // lenient: los estáticos se programan en el setup pero no todos los tests recorren
        // las ramas que los consumen (happy paths que no producen mensaje, etc.).
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class,
                Mockito.withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
        i18nMock = Mockito.mockStatic(I18n.class,
                Mockito.withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
        i18nMock.close();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private void inyectarCampo(String nombreCampo, Object valor) throws Exception {
        Field field = GrupoServiceImpl.class.getDeclaredField(nombreCampo);
        field.setAccessible(true);
        field.set(service, valor);
    }

    private String mensaje(Optional<BusinessMessages> optional) {
        assertTrue(optional.isPresent());
        return optional.get().get(0).getMessage();
    }

    private void programarSupervisorConCentro(Centro centro) {
        User user = new User();
        user.setCentroActivo(centro);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(user);
        securityUtilMock.when(() -> SecurityUtil.isAdmin(user)).thenReturn(false);
    }

    private void programarAdministrador() {
        User user = new User();
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(user);
        securityUtilMock.when(() -> SecurityUtil.isAdmin(user)).thenReturn(true);
    }

    private Centro centroConCurso(Integer cursoAcademico) {
        Centro centro = new Centro();
        centro.setCurso(cursoAcademico);
        return centro;
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_nombreNulo_devuelveError() {
        Grupo grupo = new Grupo();
        grupo.setNombre(null);
        grupo.setCurso(new Curso());

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertEquals("El nombre del grupo es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_nombreVacio_devuelveError() {
        Grupo grupo = new Grupo();
        grupo.setNombre("   ");
        grupo.setCurso(new Curso());

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertEquals("El nombre del grupo es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_cursoNulo_devuelveError() {
        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(null);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertEquals("El curso es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_nombreDuplicadoEnCentroYCursoAcademico_devuelveError() {
        Centro centroX = centroConCurso(2024);
        programarSupervisorConCentro(centroX);
        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(new Curso());
        Grupo existente = new Grupo();
        existente.setId(99L);
        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any()))
                .thenReturn(existente);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertEquals("Ya existe un grupo con ese nombre en este centro y curso académico", mensaje(resultado));
    }

    @Test
    void validateInsert_nombreUnicoYCamposValidos_devuelveVacio() {
        Centro centroX = centroConCurso(2024);
        programarSupervisorConCentro(centroX);
        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(new Curso());
        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_grupoOriginalCerrado_devuelveError() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = new Grupo();
        grupo.setNombre("1A");

        Optional<BusinessMessages> resultado = service.validateUpdate(grupo, original);

        assertEquals("No se puede modificar un grupo cerrado", mensaje(resultado));
    }

    @Test
    void validateUpdate_nombreDuplicadoOtroGrupo_devuelveError() {
        Centro centroX = new Centro();
        Grupo original = new Grupo();
        original.setId(1L);
        original.setEstado(EstadoGrupo.ABIERTO);
        original.setCentro(centroX);
        original.setCursoAcademico(2024);

        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setNombre("2B");

        Grupo otroGrupo = new Grupo();
        otroGrupo.setId(2L);
        when(repository.findByNombreAndCentroAndCursoAcademico("2B", centroX, 2024)).thenReturn(otroGrupo);

        Optional<BusinessMessages> resultado = service.validateUpdate(grupo, original);

        assertEquals("Ya existe un grupo con ese nombre en este centro y curso académico", mensaje(resultado));
    }

    @Test
    void validateUpdate_mismoNombreMismoGrupo_devuelveVacio() {
        Centro centroX = new Centro();
        Grupo original = new Grupo();
        original.setId(1L);
        original.setEstado(EstadoGrupo.ABIERTO);
        original.setCentro(centroX);
        original.setCursoAcademico(2024);

        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setNombre("1A");

        Grupo propio = new Grupo();
        propio.setId(1L);
        when(repository.findByNombreAndCentroAndCursoAcademico("1A", centroX, 2024)).thenReturn(propio);

        Optional<BusinessMessages> resultado = service.validateUpdate(grupo, original);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateUpdate_grupoAbiertoNombreUnico_devuelveVacio() {
        Grupo original = new Grupo();
        original.setId(1L);
        original.setEstado(EstadoGrupo.ABIERTO);

        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setNombre("3C");
        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);

        Optional<BusinessMessages> resultado = service.validateUpdate(grupo, original);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateRemove                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateRemove_grupoCerrado_devuelveError() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.CERRADO);

        Optional<BusinessMessages> resultado = service.validateRemove(grupo);

        assertEquals("No se puede borrar un grupo cerrado", mensaje(resultado));
    }

    @Test
    void validateRemove_grupoAbierto_devuelveVacio() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);

        Optional<BusinessMessages> resultado = service.validateRemove(grupo);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateCerrarGrupo                                                */
    /* ------------------------------------------------------------------ */

    @Test
    void validateCerrarGrupo_grupoYaCerrado_devuelveError() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.CERRADO);

        Optional<BusinessMessages> resultado = service.validateCerrarGrupo(new Grupo(), original);

        assertEquals("El grupo ya está cerrado", mensaje(resultado));
    }

    @Test
    void validateCerrarGrupo_grupoAbierto_devuelveVacio() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.ABIERTO);

        Optional<BusinessMessages> resultado = service.validateCerrarGrupo(new Grupo(), original);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateReabrirGrupo                                               */
    /* ------------------------------------------------------------------ */

    @Test
    void validateReabrirGrupo_grupoYaAbierto_devuelveError() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.ABIERTO);

        Optional<BusinessMessages> resultado = service.validateReabrirGrupo(new Grupo(), original);

        assertEquals("El grupo ya está abierto", mensaje(resultado));
    }

    @Test
    void validateReabrirGrupo_usuarioNoAdministrador_devuelveError() {
        programarSupervisorConCentro(new Centro());
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.CERRADO);

        Optional<BusinessMessages> resultado = service.validateReabrirGrupo(new Grupo(), original);

        assertEquals("No tiene permisos para reabrir el grupo", mensaje(resultado));
    }

    @Test
    void validateReabrirGrupo_grupoCerradoYUsuarioAdministrador_devuelveVacio() {
        programarAdministrador();
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.CERRADO);

        Optional<BusinessMessages> resultado = service.validateReabrirGrupo(new Grupo(), original);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* insert                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void insert_supervisor_asignaEstadoAbiertoCentroYCursoAcademicoDelCentroActivo() {
        Centro centroX = centroConCurso(2024);
        programarSupervisorConCentro(centroX);

        Centro centroY = new Centro();
        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(crearCursoConModulos(List.of()));
        grupo.setCentro(centroY);
        grupo.setCursoAcademico(1999);

        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);
        ModuloGrupoService moduloGrupoService = Mockito.mock(ModuloGrupoService.class);
        when(modelServiceFactory.resolve(ModuloGrupo.class)).thenReturn(moduloGrupoService);

        service.insert(grupo);

        assertEquals(EstadoGrupo.ABIERTO, grupo.getEstado());
        assertSame(centroX, grupo.getCentro());
        assertEquals(2024, grupo.getCursoAcademico());
        verify(repository).save(grupo);
    }

    @Test
    void insert_administrador_respetaCentroYCursoAcademicoDelCliente() {
        programarAdministrador();

        Centro centroY = new Centro();
        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(crearCursoConModulos(List.of()));
        grupo.setCentro(centroY);
        grupo.setCursoAcademico(2025);

        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);
        ModuloGrupoService moduloGrupoService = Mockito.mock(ModuloGrupoService.class);
        when(modelServiceFactory.resolve(ModuloGrupo.class)).thenReturn(moduloGrupoService);

        service.insert(grupo);

        assertSame(centroY, grupo.getCentro());
        assertEquals(2025, grupo.getCursoAcademico());
        assertEquals(EstadoGrupo.ABIERTO, grupo.getEstado());
    }

    @Test
    void insert_generaUnModuloGrupoPorCadaCursoModuloDelCurso() {
        Centro centroX = centroConCurso(2024);
        programarSupervisorConCentro(centroX);

        CursoModulo cursoModulo1 = new CursoModulo();
        cursoModulo1.setModulo(new Modulo());
        CursoModulo cursoModulo2 = new CursoModulo();
        cursoModulo2.setModulo(new Modulo());
        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(crearCursoConModulos(List.of(cursoModulo1, cursoModulo2)));

        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);
        ModuloGrupoService moduloGrupoService = Mockito.mock(ModuloGrupoService.class);
        when(modelServiceFactory.resolve(ModuloGrupo.class)).thenReturn(moduloGrupoService);

        service.insert(grupo);

        verify(moduloGrupoService, Mockito.times(2)).insert(any(ModuloGrupoInsertDTO.class));
        InOrder inOrder = inOrder(repository, moduloGrupoService);
        inOrder.verify(repository).save(grupo);
        inOrder.verify(moduloGrupoService, Mockito.times(2)).insert(any(ModuloGrupoInsertDTO.class));
    }

    @Test
    void insert_nombreDuplicado_lanzaExcepcionYNoPersiste() {
        Centro centroX = centroConCurso(2024);
        programarSupervisorConCentro(centroX);

        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(new Curso());
        Grupo existente = new Grupo();
        existente.setId(99L);
        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(existente);

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.insert(grupo));
        assertEquals("Ya existe un grupo con ese nombre en este centro y curso académico", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void insert_alumnoYaEnOtroGrupoDelCursoAcademico_lanzaExcepcionYNoPersiste() {
        Centro centroX = centroConCurso(2024);
        programarSupervisorConCentro(centroX);

        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(new Curso());
        AlumnoGrupo ag = new AlumnoGrupo();
        ag.setId(50L); // el framework ya le asignó id por auto-flush (cascade) antes de insert
        ag.setAlumno(new User());
        grupo.addAlumnosGrupo(ag);

        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);
        AlumnoGrupoService alumnoGrupoServiceMock = Mockito.mock(AlumnoGrupoService.class);
        when(modelServiceFactory.resolve(AlumnoGrupo.class)).thenReturn(alumnoGrupoServiceMock);
        when(alumnoGrupoServiceMock.validateInsert(any())).thenReturn(Optional.of(
                BusinessMessages.single("El alumno ya pertenece a otro grupo de este curso académico")));

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.insert(grupo));
        assertEquals("El alumno ya pertenece a otro grupo de este curso académico", ex.getMessage());
        verify(repository, never()).save(any());
        assertSame(grupo, ag.getGrupo()); // el servidor fijó el padre antes de validar (k-secure-coding §3.6)
    }

    @Test
    void insert_alumnoValido_validaYPersiste() {
        Centro centroX = centroConCurso(2024);
        programarSupervisorConCentro(centroX);

        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCurso(crearCursoConModulos(List.of()));
        AlumnoGrupo ag = new AlumnoGrupo();
        ag.setId(50L); // id ya asignado por auto-flush (cascade) antes de insert
        ag.setAlumno(new User());
        grupo.addAlumnosGrupo(ag);

        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);
        ModuloGrupoService moduloGrupoService = Mockito.mock(ModuloGrupoService.class);
        when(modelServiceFactory.resolve(ModuloGrupo.class)).thenReturn(moduloGrupoService);
        AlumnoGrupoService alumnoGrupoServiceMock = Mockito.mock(AlumnoGrupoService.class);
        when(modelServiceFactory.resolve(AlumnoGrupo.class)).thenReturn(alumnoGrupoServiceMock);
        when(alumnoGrupoServiceMock.validateInsert(any())).thenReturn(Optional.empty());

        service.insert(grupo);

        verify(repository).save(grupo);
        verify(alumnoGrupoServiceMock).validateInsert(any());
        assertSame(grupo, ag.getGrupo());
    }

    /* ------------------------------------------------------------------ */
    /* update                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void update_restauraCamposInmutablesDesdeOriginal() {
        Curso curso1 = new Curso();
        Centro centroX = new Centro();
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.ABIERTO);
        original.setCurso(curso1);
        original.setCentro(centroX);
        original.setCursoAcademico(2024);
        original.setFechaCierre(null);

        Curso curso2 = new Curso();
        Centro centroY = new Centro();
        Grupo grupo = new Grupo();
        grupo.setNombre("Nuevo nombre");
        grupo.setCurso(curso2);
        grupo.setCentro(centroY);
        grupo.setCursoAcademico(1999);
        grupo.setEstado(EstadoGrupo.CERRADO);
        grupo.setFechaCierre(java.time.LocalDateTime.now());

        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);

        service.update(grupo, original);

        assertSame(curso1, grupo.getCurso());
        assertSame(centroX, grupo.getCentro());
        assertEquals(2024, grupo.getCursoAcademico());
        assertEquals(EstadoGrupo.ABIERTO, grupo.getEstado());
        assertNull(grupo.getFechaCierre());
        assertEquals("Nuevo nombre", grupo.getNombre());
        verify(repository).save(grupo);
    }

    @Test
    void update_grupoCerrado_lanzaExcepcionYNoPersiste() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = new Grupo();
        grupo.setNombre("1A");

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.update(grupo, original));
        assertEquals("No se puede modificar un grupo cerrado", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void update_alumnoYaEnOtroGrupoDelCursoAcademico_lanzaExcepcionYNoPersiste() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.ABIERTO);

        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCentro(new Centro());
        grupo.setCursoAcademico(2024);
        AlumnoGrupo ag = new AlumnoGrupo();
        ag.setAlumno(new User());
        grupo.addAlumnosGrupo(ag);

        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);
        AlumnoGrupoService alumnoGrupoServiceMock = Mockito.mock(AlumnoGrupoService.class);
        when(modelServiceFactory.resolve(AlumnoGrupo.class)).thenReturn(alumnoGrupoServiceMock);
        when(alumnoGrupoServiceMock.validateInsert(any())).thenReturn(Optional.of(
                BusinessMessages.single("El alumno ya pertenece a otro grupo de este curso académico")));

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.update(grupo, original));
        assertEquals("El alumno ya pertenece a otro grupo de este curso académico", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void update_alumnoValido_validaYPersiste() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.ABIERTO);

        Grupo grupo = new Grupo();
        grupo.setNombre("1A");
        grupo.setCentro(new Centro());
        grupo.setCursoAcademico(2024);
        AlumnoGrupo ag = new AlumnoGrupo();
        ag.setAlumno(new User());
        grupo.addAlumnosGrupo(ag);

        when(repository.findByNombreAndCentroAndCursoAcademico(any(), any(), any())).thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);
        AlumnoGrupoService alumnoGrupoServiceMock = Mockito.mock(AlumnoGrupoService.class);
        when(modelServiceFactory.resolve(AlumnoGrupo.class)).thenReturn(alumnoGrupoServiceMock);
        when(alumnoGrupoServiceMock.validateInsert(any())).thenReturn(Optional.empty());

        service.update(grupo, original);

        verify(repository).save(grupo);
        verify(alumnoGrupoServiceMock).validateInsert(any());
        assertSame(grupo, ag.getGrupo());
    }

    /* ------------------------------------------------------------------ */
    /* remove                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void remove_grupoCerrado_lanzaExcepcionYNoBorra() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.CERRADO);

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.remove(grupo));
        assertEquals("No se puede borrar un grupo cerrado", ex.getMessage());
        verify(repository, never()).remove(any());
    }

    @Test
    void remove_grupoAbierto_borra() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);

        service.remove(grupo);

        verify(repository).remove(grupo);
    }

    /* ------------------------------------------------------------------ */
    /* cerrarGrupo                                                        */
    /* ------------------------------------------------------------------ */

    @Test
    void cerrarGrupo_grupoAbierto_poneEstadoCerradoYFechaCierre() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);
        when(repository.save(grupo)).thenReturn(grupo);

        service.cerrarGrupo(grupo, original);

        assertEquals(EstadoGrupo.CERRADO, grupo.getEstado());
        assertNotNull(grupo.getFechaCierre());
        verify(repository).save(grupo);
    }

    @Test
    void cerrarGrupo_grupoYaCerrado_lanzaExcepcion() {
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = new Grupo();

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.cerrarGrupo(grupo, original));
        assertEquals("El grupo ya está cerrado", ex.getMessage());
        verify(repository, never()).save(any());
    }

    /* ------------------------------------------------------------------ */
    /* reabrirGrupo                                                       */
    /* ------------------------------------------------------------------ */

    @Test
    void reabrirGrupo_grupoCerradoYAdministrador_poneEstadoAbiertoYBorraFechaCierre() {
        programarAdministrador();
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.CERRADO);
        original.setFechaCierre(java.time.LocalDateTime.now());
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.CERRADO);
        grupo.setFechaCierre(java.time.LocalDateTime.now());
        when(repository.save(grupo)).thenReturn(grupo);

        service.reabrirGrupo(grupo, original);

        assertEquals(EstadoGrupo.ABIERTO, grupo.getEstado());
        assertNull(grupo.getFechaCierre());
        verify(repository).save(grupo);
    }

    @Test
    void reabrirGrupo_usuarioNoAdministrador_lanzaExcepcion() {
        programarSupervisorConCentro(new Centro());
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = new Grupo();

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.reabrirGrupo(grupo, original));
        assertEquals("No tiene permisos para reabrir el grupo", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void reabrirGrupo_grupoYaAbierto_lanzaExcepcion() {
        programarAdministrador();
        Grupo original = new Grupo();
        original.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = new Grupo();

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.reabrirGrupo(grupo, original));
        assertEquals("El grupo ya está abierto", ex.getMessage());
        verify(repository, never()).save(any());
    }

    /* ------------------------------------------------------------------ */
    /* allowProperties                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_incluyeCamposClienteYAlumnosGrupoYExcluyeServidor() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("nombre"));
        assertTrue(allowProperties.allowProperty("curso"));
        assertTrue(allowProperties.allowProperty("centro"));
        assertTrue(allowProperties.allowProperty("cursoAcademico"));
        assertTrue(allowProperties.allowProperty("alumnosGrupo"));
        assertFalse(allowProperties.allowProperty("estado"));
        assertFalse(allowProperties.allowProperty("fechaCierre"));
        assertFalse(allowProperties.allowProperty("modulosGrupo"));
    }

    @Test
    void allowPropertiesUpdate_incluyeSoloNombre() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("nombre"));
        assertFalse(allowProperties.allowProperty("curso"));
        assertFalse(allowProperties.allowProperty("centro"));
        assertFalse(allowProperties.allowProperty("cursoAcademico"));
        assertFalse(allowProperties.allowProperty("estado"));
        assertFalse(allowProperties.allowProperty("fechaCierre"));
    }

    @Test
    void allowPropertiesCerrarGrupo_yReabrirGrupo_whitelistVacia() {
        AllowProperties cerrar = service.allowPropertiesCerrarGrupo();
        AllowProperties reabrir = service.allowPropertiesReabrirGrupo();

        assertFalse(cerrar.allowProperty("estado"));
        assertFalse(cerrar.allowProperty("fechaCierre"));
        assertFalse(cerrar.allowProperty("nombre"));
        assertFalse(reabrir.allowProperty("estado"));
        assertFalse(reabrir.allowProperty("fechaCierre"));
        assertFalse(reabrir.allowProperty("nombre"));
    }

    /* ------------------------------------------------------------------ */
    /* utilidades                                                         */
    /* ------------------------------------------------------------------ */

    private Curso crearCursoConModulos(List<CursoModulo> modulos) {
        Curso curso = Mockito.mock(Curso.class);
        when(curso.getModulos()).thenReturn(modulos);
        return curso;
    }
}
