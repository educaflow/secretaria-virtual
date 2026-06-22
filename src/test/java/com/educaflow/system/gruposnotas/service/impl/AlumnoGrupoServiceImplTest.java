package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.db.ValorNota;
import com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.GrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.NotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link AlumnoGrupoServiceImpl}.
 *
 * <p>El servicio extiende {@code DefaultModelService<AlumnoGrupo>} y se construye con
 * {@code (Class<AlumnoGrupo>, Repository<AlumnoGrupo>)}. Los colaboradores {@code @Inject}
 * ({@code alumnoGrupoRepository}, {@code notaRepository}, {@code grupoRepository},
 * {@code modelServiceFactory}) se inyectan por reflexión.</p>
 *
 * <p>Notas sobre el código real frente a la descripción del contrato:</p>
 * <ul>
 *   <li>La acción de alta es {@code guardarAlumnoGrupo(AlumnoGrupo, Long grupoId)} (no un
 *       {@code insert(AlumnoGrupo)}); la restauración del grupo se hace resolviendo
 *       {@code grupoRepository.find(grupoId)} con el id del grupo padre que recibe como
 *       parámetro (en vez de leerlo del contexto del request). Los tests de la sección
 *       «insert» se ejecutan contra {@code guardarAlumnoGrupo}.</li>
 *   <li>Las notas «No evaluado» se crean con {@code modelServiceFactory.resolve(Nota.class)}
 *       y {@code notaService.insert(nota)}; se verifica sobre ese {@code ModelService<Nota>}
 *       mock, no sobre {@code notaRepository}.</li>
 *   <li>El cálculo de la media (CC-001) vive en el getter generado {@code getNotaMedia()}
 *       (que lee {@code getNotas()}); se prueba con {@code AlumnoGrupo}/{@code Nota} reales
 *       sin mocks.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AlumnoGrupoServiceImplTest {

    private static final String CODIGO_ALUMNO = "ALUMNO";
    private static final int CURSO_ACADEMICO = 2024;
    private static final long GRUPO_PADRE_ID = 99L;

    @Mock
    private Repository<AlumnoGrupo> repository;

    @Mock
    private AlumnoGrupoRepository alumnoGrupoRepository;

    @Mock
    private NotaRepository notaRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private ModelServiceFactory modelServiceFactory;

    private AlumnoGrupoServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new AlumnoGrupoServiceImpl(AlumnoGrupo.class, repository);
        inject(service, "alumnoGrupoRepository", alumnoGrupoRepository);
        inject(service, "notaRepository", notaRepository);
        inject(service, "grupoRepository", grupoRepository);
        inject(service, "modelServiceFactory", modelServiceFactory);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Centro centroConId(long id) {
        Centro centro = new Centro();
        centro.setId(id);
        return centro;
    }

    private static Grupo grupoAbierto(Centro centro) {
        Grupo grupo = new Grupo();
        grupo.setEstado(EstadoGrupo.ABIERTO);
        grupo.setCentro(centro);
        grupo.setCursoAcademico(CURSO_ACADEMICO);
        grupo.setModulosGrupo(new ArrayList<>());
        return grupo;
    }

    /** Crea un usuario tipo Alumno del centro indicado (cadena CentroUsuario → TipoUsuario ALUMNO). */
    private static User alumnoDeCentro(Centro centro) {
        TipoUsuario tipoAlumno = new TipoUsuario();
        tipoAlumno.setCodigo(CODIGO_ALUMNO);

        CentroUsuarioTipoUsuario cutu = new CentroUsuarioTipoUsuario();
        cutu.setTipoUsuario(tipoAlumno);

        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        centroUsuario.setCentroUsuarioTipoUsuario(new ArrayList<>(List.of(cutu)));

        User alumno = new User();
        alumno.setCentroUsuarios(new ArrayList<>(List.of(centroUsuario)));
        return alumno;
    }

    private static AlumnoGrupo alumnoGrupo(User alumno, Grupo grupo) {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setAlumno(alumno);
        alumnoGrupo.setGrupo(grupo);
        return alumnoGrupo;
    }

    private static Nota nota(ValorNota valor) {
        Nota nota = new Nota();
        nota.setValor(valor);
        return nota;
    }

    private static AlumnoGrupo conNotas(Nota... notas) {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setNotas(new ArrayList<>(List.of(notas)));
        return alumnoGrupo;
    }

    /** Texto del único {@link com.axelor.db.modelservice.BusinessMessage} presente. */
    private static String mensaje(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent(), "Se esperaba un BusinessMessages presente");
        assertEquals(1, resultado.get().size(), "Se esperaba exactamente un mensaje");
        return resultado.get().get(0).getMessage();
    }

    /** Recorre la cadena de causas buscando el texto esperado en algún mensaje. */
    private static void assertCadenaContieneMensaje(Throwable ex, String mensajeEsperado) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains(mensajeEsperado)) {
                return;
            }
            cause = cause.getCause();
        }
        throw new AssertionError("No se encontró el mensaje '" + mensajeEsperado
                + "' en la cadena de causas de " + ex);
    }

    // =====================================================================
    // validateInsert
    // =====================================================================

    @Test
    void validateInsert_datosValidos_devuelveOptionalVacio() {
        Centro centroX = centroConId(1L);
        Grupo grupo = grupoAbierto(centroX);
        User alumno = alumnoDeCentro(centroX);
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumno, grupo);

        when(alumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico(
                alumno, centroX, CURSO_ACADEMICO, null)).thenReturn(false);
        when(alumnoGrupoRepository.findByGrupo(grupo)).thenReturn(new ArrayList<>());

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_alumnoNulo_devuelveMensajeDebeElegirAlumno() {
        Grupo grupo = grupoAbierto(centroConId(1L));
        AlumnoGrupo alumnoGrupo = alumnoGrupo(null, grupo);

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("Debe elegir un alumno.", mensaje(resultado));
    }

    @Test
    void validateInsert_grupoCerrado_devuelveMensajeNoAnadir() {
        Centro centroX = centroConId(1L);
        Grupo grupo = grupoAbierto(centroX);
        grupo.setEstado(EstadoGrupo.CERRADO);
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumnoDeCentro(centroX), grupo);

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("No se pueden añadir alumnos a un grupo cerrado.", mensaje(resultado));
    }

    @Test
    void validateInsert_alumnoNoEsDelCentroOTipo_devuelveMensajeTipoAlumno() {
        Centro centroX = centroConId(1L);
        Grupo grupo = grupoAbierto(centroX);
        // Alumno de OTRO centro: la cadena CentroUsuario apunta a un centro distinto.
        User alumno = alumnoDeCentro(centroConId(2L));
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumno, grupo);

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("El alumno debe ser un usuario de tipo Alumno del centro del grupo.",
                mensaje(resultado));
    }

    @Test
    void validateInsert_alumnoEnOtroGrupoMismoCursoAcademico_devuelveMensajeYaPertenece() {
        Centro centroX = centroConId(1L);
        Grupo grupo = grupoAbierto(centroX);
        User alumno = alumnoDeCentro(centroX);
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumno, grupo);

        when(alumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico(
                alumno, centroX, CURSO_ACADEMICO, null)).thenReturn(true);

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("El alumno ya pertenece a otro grupo de este curso académico.",
                mensaje(resultado));
    }

    @Test
    void validateInsert_alumnoYaEnElGrupo_devuelveMensajeYaEstaEnGrupo() {
        Centro centroX = centroConId(1L);
        Grupo grupo = grupoAbierto(centroX);
        User alumno = alumnoDeCentro(centroX);
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumno, grupo);

        when(alumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico(
                alumno, centroX, CURSO_ACADEMICO, null)).thenReturn(false);
        // Ya existe otra pertenencia distinta del mismo alumno en ESTE grupo.
        AlumnoGrupo existente = alumnoGrupo(alumno, grupo);
        existente.setId(500L);
        when(alumnoGrupoRepository.findByGrupo(grupo))
                .thenReturn(new ArrayList<>(List.of(existente)));

        Optional<BusinessMessages> resultado = service.validateInsert(alumnoGrupo);

        assertEquals("El alumno ya está en el grupo.", mensaje(resultado));
    }

    // =====================================================================
    // validateRemove
    // =====================================================================

    @Test
    void validateRemove_grupoAbierto_devuelveOptionalVacio() {
        Centro centroX = centroConId(1L);
        Grupo grupo = grupoAbierto(centroX);
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumnoDeCentro(centroX), grupo);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User admin = new User();
            auth.when(AuthUtils::getUser).thenReturn(admin);
            security.when(() -> SecurityUtil.isAdmin(admin)).thenReturn(true);

            Optional<BusinessMessages> resultado = service.validateRemove(alumnoGrupo);

            assertTrue(resultado.isEmpty());
        }
    }

    @Test
    void validateRemove_grupoCerrado_devuelveMensajeNoQuitar() {
        Centro centroX = centroConId(1L);
        Grupo grupo = grupoAbierto(centroX);
        grupo.setEstado(EstadoGrupo.CERRADO);
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumnoDeCentro(centroX), grupo);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User admin = new User();
            auth.when(AuthUtils::getUser).thenReturn(admin);
            security.when(() -> SecurityUtil.isAdmin(admin)).thenReturn(true);

            Optional<BusinessMessages> resultado = service.validateRemove(alumnoGrupo);

            assertEquals("No se pueden quitar alumnos de un grupo cerrado.", mensaje(resultado));
        }
    }

    // =====================================================================
    // insert (guardarAlumnoGrupo)
    // =====================================================================

    @Test
    void insert_alumnoValido_restauraGrupoGuardaYCreaNotasNoEvaluado() {
        Centro centroX = centroConId(1L);
        Grupo grupoAbierto = grupoAbierto(centroX);
        ModuloGrupo modulo1 = new ModuloGrupo();
        ModuloGrupo modulo2 = new ModuloGrupo();
        grupoAbierto.setModulosGrupo(new ArrayList<>(List.of(modulo1, modulo2)));

        User alumno = alumnoDeCentro(centroX);
        // El cliente NO indica grupo: se restaura desde el padre.
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumno, null);

        @SuppressWarnings("unchecked")
        ModelService<Nota> notaService = org.mockito.Mockito.mock(ModelService.class);

        when(grupoRepository.find(GRUPO_PADRE_ID)).thenReturn(grupoAbierto);
        when(alumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico(
                alumno, centroX, CURSO_ACADEMICO, null)).thenReturn(false);
        when(alumnoGrupoRepository.findByGrupo(grupoAbierto)).thenReturn(new ArrayList<>());
        when(repository.save(alumnoGrupo)).thenReturn(alumnoGrupo);
        when(modelServiceFactory.resolve(Nota.class)).thenReturn(notaService);
        when(notaService.insert(any(Nota.class))).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User admin = new User();
            auth.when(AuthUtils::getUser).thenReturn(admin);
            security.when(() -> SecurityUtil.isAdmin(admin)).thenReturn(true);

            service.guardarAlumnoGrupo(alumnoGrupo, GRUPO_PADRE_ID);
        }

        assertSame(grupoAbierto, alumnoGrupo.getGrupo());
        verify(repository).save(alumnoGrupo);

        ArgumentCaptor<Nota> captor = ArgumentCaptor.forClass(Nota.class);
        verify(notaService, times(2)).insert(captor.capture());
        List<Nota> notasCreadas = captor.getAllValues();
        assertEquals(2, notasCreadas.size());
        for (Nota nota : notasCreadas) {
            assertEquals(ValorNota.NO_EVALUADO, nota.getValor());
            assertSame(alumnoGrupo, nota.getAlumnoGrupo());
        }
    }

    @Test
    void insert_grupoRestauradoIgnoraGrupoDelCliente() {
        Centro centroLegitimo = centroConId(1L);
        Grupo grupoLegitimo = grupoAbierto(centroLegitimo);

        // El cliente intenta inyectar un grupo malicioso de otro centro.
        Centro centroMalicioso = centroConId(2L);
        Grupo grupoMalicioso = grupoAbierto(centroMalicioso);

        User alumno = alumnoDeCentro(centroLegitimo);
        AlumnoGrupo alumnoGrupo = alumnoGrupo(alumno, grupoMalicioso);

        @SuppressWarnings("unchecked")
        ModelService<Nota> notaService = org.mockito.Mockito.mock(ModelService.class);

        when(grupoRepository.find(GRUPO_PADRE_ID)).thenReturn(grupoLegitimo);
        when(alumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico(
                alumno, centroLegitimo, CURSO_ACADEMICO, null)).thenReturn(false);
        when(alumnoGrupoRepository.findByGrupo(grupoLegitimo)).thenReturn(new ArrayList<>());
        when(repository.save(alumnoGrupo)).thenReturn(alumnoGrupo);
        when(modelServiceFactory.resolve(Nota.class)).thenReturn(notaService);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User admin = new User();
            auth.when(AuthUtils::getUser).thenReturn(admin);
            security.when(() -> SecurityUtil.isAdmin(admin)).thenReturn(true);

            service.guardarAlumnoGrupo(alumnoGrupo, GRUPO_PADRE_ID);
        }

        // Asignación incondicional: prevalece el grupo del padre, no el del cliente.
        assertSame(grupoLegitimo, alumnoGrupo.getGrupo());
    }

    @Test
    void insert_validacionFalla_lanzaExcepcionYNoGuardaNiCreaNotas() {
        Centro centroX = centroConId(1L);
        Grupo grupoAbierto = grupoAbierto(centroX);
        // alumno = null hace fallar V-AlumnoGrupo-001.
        AlumnoGrupo alumnoGrupo = alumnoGrupo(null, null);

        when(grupoRepository.find(GRUPO_PADRE_ID)).thenReturn(grupoAbierto);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User admin = new User();
            auth.when(AuthUtils::getUser).thenReturn(admin);
            security.when(() -> SecurityUtil.isAdmin(admin)).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.guardarAlumnoGrupo(alumnoGrupo, GRUPO_PADRE_ID));

            assertCadenaContieneMensaje(ex, "Debe elegir un alumno.");
        }

        verify(repository, never()).save(any());
        verify(modelServiceFactory, never()).resolve(Nota.class);
    }

    // =====================================================================
    // update
    // =====================================================================

    @Test
    void update_siempre_lanzaUnsupportedOperationException() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        AlumnoGrupo original = new AlumnoGrupo();

        assertThrows(UnsupportedOperationException.class,
                () -> service.update(alumnoGrupo, original));
    }

    // =====================================================================
    // calcularNotaMedia (CC-001) — entidades reales, sin mocks
    // =====================================================================

    @Test
    void calcularNotaMedia_sinModulosEvaluados_devuelveSinNota() {
        AlumnoGrupo alumnoGrupo = conNotas(nota(ValorNota.NO_EVALUADO), nota(ValorNota.NO_EVALUADO));

        assertEquals("Sin nota", service.calcularNotaMedia(alumnoGrupo));
    }

    @Test
    void calcularNotaMedia_unaNotaNumerica_devuelveEsaNota() {
        AlumnoGrupo alumnoGrupo = conNotas(nota(ValorNota.NOTA_08), nota(ValorNota.NO_EVALUADO));

        assertEquals("8", service.calcularNotaMedia(alumnoGrupo));
    }

    @Test
    void calcularNotaMedia_matriculaHonorCuentaComo10_yExcluyeNoEvaluado() {
        AlumnoGrupo alumnoGrupo = conNotas(nota(ValorNota.MATRICULA_HONOR), nota(ValorNota.NO_EVALUADO));

        assertEquals("10", service.calcularNotaMedia(alumnoGrupo));
    }

    @Test
    void calcularNotaMedia_mediaConMHy7_devuelve9Redondeado() {
        AlumnoGrupo alumnoGrupo = conNotas(nota(ValorNota.MATRICULA_HONOR), nota(ValorNota.NOTA_07));

        assertEquals("9", service.calcularNotaMedia(alumnoGrupo));
    }

    @Test
    void calcularNotaMedia_media8y6_devuelve7() {
        AlumnoGrupo alumnoGrupo = conNotas(nota(ValorNota.NOTA_08), nota(ValorNota.NOTA_06));

        assertEquals("7", service.calcularNotaMedia(alumnoGrupo));
    }

    @Test
    void calcularNotaMedia_redondeoHaciaArriba_enMitad() {
        // Media de 5 y 6 = 5.5 → Math.round(5.5) = 6 (al entero más cercano).
        AlumnoGrupo alumnoGrupo = conNotas(nota(ValorNota.NOTA_05), nota(ValorNota.NOTA_06));

        assertEquals("6", service.calcularNotaMedia(alumnoGrupo));
    }

    @Test
    void calcularNotaMedia_listaNula_devuelveSinNota() {
        AlumnoGrupo alumnoGrupo = new AlumnoGrupo();
        alumnoGrupo.setNotas(null);

        assertEquals("Sin nota", service.calcularNotaMedia(alumnoGrupo));
    }

    // =====================================================================
    // allowProperties*
    // =====================================================================

    @Test
    void allowPropertiesInsert_soloAlumno() {
        AllowProperties allow = service.allowPropertiesInsert();

        assertTrue(allow.allowProperty("alumno"));
        assertFalse(allow.allowProperty("grupo"));
        assertFalse(allow.allowProperty("centro"));
        assertFalse(allow.allowProperty("notaMedia"));
    }

    @Test
    void allowPropertiesUpdate_denyAll() {
        AllowProperties allow = service.allowPropertiesUpdate();

        assertFalse(allow.allowProperty("alumno"));
        assertFalse(allow.allowProperty("grupo"));
        assertFalse(allow.allowProperty("centro"));
        assertFalse(allow.allowProperty("notaMedia"));
    }

    @Test
    void allowPropertiesRemove_denyAll() {
        AllowProperties allow = service.allowPropertiesRemove();

        assertFalse(allow.allowProperty("alumno"));
        assertFalse(allow.allowProperty("grupo"));
        assertFalse(allow.allowProperty("centro"));
        assertFalse(allow.allowProperty("notaMedia"));
    }
}
