package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.db.ValorNota;
import com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository;
import com.educaflow.system.gruposnotas.service.NotaInsertDTO;
import com.educaflow.system.gruposnotas.service.NotaService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlumnoGrupoServiceImplTest {

    private AlumnoGrupoRepository repository;
    private ModelServiceFactory modelServiceFactory;
    private AlumnoGrupoServiceImpl service;

    private MockedStatic<SecurityUtil> securityUtilMock;
    private MockedStatic<I18n> i18nMock;
    private MockedStatic<JPA> jpaMock;

    @BeforeEach
    void setUp() throws Exception {
        repository = Mockito.mock(AlumnoGrupoRepository.class);
        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);

        service = new AlumnoGrupoServiceImpl(AlumnoGrupo.class, repository);

        Field field = AlumnoGrupoServiceImpl.class.getDeclaredField("modelServiceFactory");
        field.setAccessible(true);
        field.set(service, modelServiceFactory);

        // lenient: los estáticos se programan en el setup pero no todos los tests recorren
        // las ramas que los consumen (happy paths que no producen mensaje, etc.).
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class,
                Mockito.withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
        i18nMock = Mockito.mockStatic(I18n.class,
                Mockito.withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        jpaMock = Mockito.mockStatic(JPA.class,
                Mockito.withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
        i18nMock.close();
        jpaMock.close();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

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

    @SuppressWarnings("unchecked")
    private void programarEsAlumnoDelCentro(long count) {
        Query<CentroUsuario> query = Mockito.mock(Query.class);
        jpaMock.when(() -> JPA.all(CentroUsuario.class)).thenReturn(query);
        when(query.filter(anyString())).thenReturn(query);
        when(query.bind(anyString(), any())).thenReturn(query);
        when(query.count()).thenReturn(count);
    }

    @SuppressWarnings("unchecked")
    private void programarFinderCursoAcademico(List<AlumnoGrupo> existentes) {
        Query<AlumnoGrupo> query = Mockito.mock(Query.class);
        when(repository.findByAlumnoAndGrupoCursoAcademico(any(), any())).thenReturn(query);
        when(query.fetch()).thenReturn(existentes);
    }

    private Grupo grupoAbierto(Centro centro, Integer cursoAcademico) {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);
        grupo.setCentro(centro);
        grupo.setCursoAcademico(cursoAcademico);
        return grupo;
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_grupoNulo_devuelveError() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(null);
        alumnoGrupo.setAlumno(new User());

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("El grupo es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_alumnoNulo_devuelveError() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupo);
        alumnoGrupo.setAlumno(null);

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("Debe elegir un alumno", mensaje(resultado));
    }

    @Test
    void validateInsert_grupoCerrado_devuelveError() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.CERRADO);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupo);
        alumnoGrupo.setAlumno(new User());

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("No se pueden añadir alumnos a un grupo cerrado", mensaje(resultado));
    }

    @Test
    void validateInsert_supervisorGrupoDeOtroCentro_devuelveError() {
        Centro centroX = new Centro();
        Centro centroY = new Centro();
        programarSupervisorConCentro(centroX);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupoAbierto(centroY, 2024));
        alumnoGrupo.setAlumno(new User());

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("El grupo no pertenece a su centro", mensaje(resultado));
    }

    @Test
    void validateInsert_alumnoNoEsAlumnoDelCentroDelGrupo_devuelveError() {
        Centro centroX = new Centro();
        programarSupervisorConCentro(centroX);
        programarEsAlumnoDelCentro(0L);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupoAbierto(centroX, 2024));
        alumnoGrupo.setAlumno(new User());

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("El alumno debe ser un usuario de tipo Alumno del centro del grupo", mensaje(resultado));
    }

    @Test
    void validateInsert_alumnoYaEnOtroGrupoMismoCursoAcademico_devuelveError() {
        Centro centroX = new Centro();
        programarSupervisorConCentro(centroX);
        programarEsAlumnoDelCentro(1L);
        AlumnoGrupo otroAlumnoGrupo = new AlumnoGrupo();
        otroAlumnoGrupo.setId(2L);
        programarFinderCursoAcademico(List.of(otroAlumnoGrupo));
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupoAbierto(centroX, 2024));
        alumnoGrupo.setAlumno(new User());

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("El alumno ya pertenece a otro grupo de este curso académico", mensaje(resultado));
    }

    @Test
    void validateInsert_todoValido_devuelveVacio() {
        Centro centroX = new Centro();
        programarSupervisorConCentro(centroX);
        programarEsAlumnoDelCentro(1L);
        programarFinderCursoAcademico(List.of());
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupoAbierto(centroX, 2024));
        alumnoGrupo.setAlumno(new User());

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateRemove                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateRemove_grupoCerrado_devuelveError() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.CERRADO);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupo);

        Optional<BusinessMessages> resultado = service.validateRemove(alumnoGrupo);

        assertEquals("No se pueden quitar alumnos de un grupo cerrado", mensaje(resultado));
    }

    @Test
    void validateRemove_grupoAbierto_devuelveVacio() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupo);

        Optional<BusinessMessages> resultado = service.validateRemove(alumnoGrupo);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* insert                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void insert_creaUnaNotaNoEvaluadoPorCadaModuloDelGrupo() {
        Centro centroX = new Centro();
        programarSupervisorConCentro(centroX);
        programarEsAlumnoDelCentro(1L);
        programarFinderCursoAcademico(List.of());

        Grupo grupo = grupoAbierto(centroX, 2024);
        grupo.setModulosGrupo(new ArrayList<>(List.of(new ModuloGrupo(), new ModuloGrupo())));
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupo);
        alumnoGrupo.setAlumno(new User());

        when(repository.save(alumnoGrupo)).thenReturn(alumnoGrupo);
        NotaService notaService = Mockito.mock(NotaService.class);
        when(modelServiceFactory.resolve(Nota.class)).thenReturn(notaService);

        service.insert(alumnoGrupo);

        verify(notaService, Mockito.times(2)).insert(any(NotaInsertDTO.class));
        InOrder inOrder = inOrder(repository, notaService);
        inOrder.verify(repository).save(alumnoGrupo);
        inOrder.verify(notaService, Mockito.times(2)).insert(any(NotaInsertDTO.class));
    }

    @Test
    void insert_alumnoNoElegido_lanzaExcepcionYNoPersiste() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupo);
        alumnoGrupo.setAlumno(null);

        NotaService notaService = Mockito.mock(NotaService.class);

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.insert(alumnoGrupo));
        assertEquals("Debe elegir un alumno", ex.getMessage());
        verify(repository, never()).save(any());
        verify(notaService, never()).insert(any(NotaInsertDTO.class));
    }

    /* ------------------------------------------------------------------ */
    /* remove                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void remove_grupoCerrado_lanzaExcepcionYNoBorra() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.CERRADO);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupo);

        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class, () -> service.remove(alumnoGrupo));
        assertEquals("No se pueden quitar alumnos de un grupo cerrado", ex.getMessage());
        verify(repository, never()).remove(any());
    }

    @Test
    void remove_grupoAbierto_borra() {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setGrupo(grupo);

        service.remove(alumnoGrupo);

        verify(repository).remove(alumnoGrupo);
    }

    /* ------------------------------------------------------------------ */
    /* calcularNotaMedia                                                  */
    /* ------------------------------------------------------------------ */

    @Test
    void calcularNotaMedia_delegaEnGetterDelDominio() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        Nota nota = new Nota();
        nota.setValor(ValorNota.NOTA_8);
        alumnoGrupo.addNota(nota);

        String resultado = service.calcularNotaMedia(alumnoGrupo);

        assertEquals("8", resultado);
        assertEquals(alumnoGrupo.getNotaMedia(), resultado);
    }

    /* ------------------------------------------------------------------ */
    /* allowProperties                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_incluyeGrupoYAlumnoYExcluyeServidor() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("grupo"));
        assertTrue(allowProperties.allowProperty("alumno"));
        assertFalse(allowProperties.allowProperty("notas"));
        assertFalse(allowProperties.allowProperty("notaMedia"));
    }

    @Test
    void allowPropertiesUpdate_whitelistVacia() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertFalse(allowProperties.allowProperty("grupo"));
        assertFalse(allowProperties.allowProperty("alumno"));
    }
}
