package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.sistemaeducativo.db.Curso;
import com.educaflow.subsystem.sistemaeducativo.db.CursoModulo;
import com.educaflow.subsystem.sistemaeducativo.db.Modulo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.repo.GrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.ModuloGrupoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Tests unitarios de {@link GrupoServiceImpl}.
 *
 * <p>El servicio extiende {@code DefaultModelService<Grupo>} y se construye con
 * {@code (Class<Grupo>, Repository<Grupo>)}. Los colaboradores {@code @Inject}
 * ({@code grupoRepository}, {@code moduloGrupoRepository}) se inyectan por reflexión.</p>
 *
 * <p>El usuario autenticado y el rol admin se mockean con {@code mockStatic}:
 * {@link SecurityUtil#isAdmin(User)} y {@link AuthUtils#getUser()}.</p>
 */
@ExtendWith(MockitoExtension.class)
class GrupoServiceImplTest {

    private static final int CURSO_ACADEMICO = 2024;

    @Mock
    private Repository<Grupo> repository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private ModuloGrupoRepository moduloGrupoRepository;

    private GrupoServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new GrupoServiceImpl(Grupo.class, repository);
        inject(service, "grupoRepository", grupoRepository);
        inject(service, "moduloGrupoRepository", moduloGrupoRepository);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Grupo grupo(Long id, String nombre, Curso curso, Centro centro, Integer cursoAcademico) {
        Grupo grupo = new Grupo();
        grupo.setId(id);
        grupo.setNombre(nombre);
        grupo.setCurso(curso);
        grupo.setCentro(centro);
        grupo.setCursoAcademico(cursoAcademico);
        return grupo;
    }

    private static Centro centro() {
        return new Centro();
    }

    private static Curso cursoConModulos(int numeroModulos) {
        Curso curso = new Curso();
        java.util.ArrayList<CursoModulo> modulos = new java.util.ArrayList<>();
        for (int i = 0; i < numeroModulos; i++) {
            CursoModulo cursoModulo = new CursoModulo();
            cursoModulo.setModulo(new Modulo());
            modulos.add(cursoModulo);
        }
        curso.setModulos(modulos);
        return curso;
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
    void validateInsert_grupoValido_devuelveOptionalVacio() {
        Centro centro = centro();
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro, CURSO_ACADEMICO);
        when(grupoRepository.findByNombreCentroCursoAcademico("1º DAM A", centro, CURSO_ACADEMICO))
                .thenReturn(null);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_nombreVacio_devuelveMensajeObligatorio() {
        Grupo grupo = grupo(1L, null, cursoConModulos(0), centro(), CURSO_ACADEMICO);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertEquals("El nombre del grupo es obligatorio.", mensaje(resultado));
    }

    @Test
    void validateInsert_nombreEnBlanco_devuelveMensajeObligatorio() {
        Grupo grupo = grupo(1L, "   ", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertEquals("El nombre del grupo es obligatorio.", mensaje(resultado));
    }

    @Test
    void validateInsert_cursoNulo_devuelveMensajeCursoObligatorio() {
        Grupo grupo = grupo(1L, "1º DAM A", null, centro(), CURSO_ACADEMICO);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertEquals("El curso es obligatorio.", mensaje(resultado));
    }

    @Test
    void validateInsert_nombreDuplicado_devuelveMensajeYaExiste() {
        Centro centro = centro();
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro, CURSO_ACADEMICO);
        Grupo otro = grupo(2L, "1º DAM A", cursoConModulos(0), centro, CURSO_ACADEMICO);
        when(grupoRepository.findByNombreCentroCursoAcademico("1º DAM A", centro, CURSO_ACADEMICO))
                .thenReturn(otro);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertEquals("Ya existe un grupo con ese nombre en este centro y curso académico.",
                mensaje(resultado));
    }

    @Test
    void validateInsert_mismoNombreOtroCentro_devuelveOptionalVacio() {
        Centro centro = centro();
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro, CURSO_ACADEMICO);
        // El finder ya filtra por centro+cursoAcademico: un homónimo en otro centro no se devuelve.
        when(grupoRepository.findByNombreCentroCursoAcademico("1º DAM A", centro, CURSO_ACADEMICO))
                .thenReturn(null);

        Optional<BusinessMessages> resultado = service.validateInsert(grupo);

        assertTrue(resultado.isEmpty());
    }

    // =====================================================================
    // validateUpdate
    // =====================================================================

    @Test
    void validateUpdate_grupoAbiertoNombreUnico_devuelveOptionalVacio() {
        Centro centro = centro();
        Grupo original = grupo(10L, "1º DAM A", cursoConModulos(0), centro, CURSO_ACADEMICO);
        original.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = grupo(10L, "1º DAM B", cursoConModulos(0), centro, CURSO_ACADEMICO);
        when(grupoRepository.findByNombreCentroCursoAcademico("1º DAM B", centro, CURSO_ACADEMICO))
                .thenReturn(null);

        Optional<BusinessMessages> resultado = service.validateUpdate(grupo, original);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateUpdate_grupoCerrado_devuelveMensajeNoModificar() {
        Grupo original = grupo(10L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        original.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = grupo(10L, "1º DAM B", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        Optional<BusinessMessages> resultado = service.validateUpdate(grupo, original);

        assertEquals("No se puede modificar un grupo cerrado.", mensaje(resultado));
    }

    @Test
    void validateUpdate_nombreDuplicadoEnOtroGrupo_devuelveMensajeYaExiste() {
        Centro centro = centro();
        Grupo original = grupo(10L, "1º DAM A", cursoConModulos(0), centro, CURSO_ACADEMICO);
        original.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = grupo(10L, "1º DAM B", cursoConModulos(0), centro, CURSO_ACADEMICO);
        Grupo otro = grupo(20L, "1º DAM B", cursoConModulos(0), centro, CURSO_ACADEMICO);
        when(grupoRepository.findByNombreCentroCursoAcademico("1º DAM B", centro, CURSO_ACADEMICO))
                .thenReturn(otro);

        Optional<BusinessMessages> resultado = service.validateUpdate(grupo, original);

        assertEquals("Ya existe un grupo con ese nombre en este centro y curso académico.",
                mensaje(resultado));
    }

    @Test
    void validateUpdate_nombreCoincideConPropioGrupo_devuelveOptionalVacio() {
        Centro centro = centro();
        Grupo original = grupo(10L, "1º DAM A", cursoConModulos(0), centro, CURSO_ACADEMICO);
        original.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = grupo(10L, "1º DAM A", cursoConModulos(0), centro, CURSO_ACADEMICO);
        // El finder devuelve el propio grupo (mismo id) → no se considera colisión.
        when(grupoRepository.findByNombreCentroCursoAcademico("1º DAM A", centro, CURSO_ACADEMICO))
                .thenReturn(grupo);

        Optional<BusinessMessages> resultado = service.validateUpdate(grupo, original);

        assertTrue(resultado.isEmpty());
    }

    // =====================================================================
    // validateRemove
    // =====================================================================

    @Test
    void validateRemove_grupoAbierto_devuelveOptionalVacio() {
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupo.setEstado(EstadoGrupo.ABIERTO);

        Optional<BusinessMessages> resultado = service.validateRemove(grupo);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateRemove_grupoCerrado_devuelveMensajeNoBorrar() {
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupo.setEstado(EstadoGrupo.CERRADO);

        Optional<BusinessMessages> resultado = service.validateRemove(grupo);

        assertEquals("No se puede borrar un grupo cerrado.", mensaje(resultado));
    }

    // =====================================================================
    // validateCerrar
    // =====================================================================

    @Test
    void validateCerrar_grupoAbierto_devuelveOptionalVacio() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        Optional<BusinessMessages> resultado = service.validateCerrar(grupo, grupoOriginal);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateCerrar_grupoYaCerrado_devuelveMensajeYaCerrado() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        Optional<BusinessMessages> resultado = service.validateCerrar(grupo, grupoOriginal);

        assertEquals("El grupo ya está cerrado.", mensaje(resultado));
    }

    // =====================================================================
    // validateReabrir
    // =====================================================================

    @Test
    void validateReabrir_cerradoYAdministrador_devuelveOptionalVacio() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User admin = new User();
            auth.when(AuthUtils::getUser).thenReturn(admin);
            security.when(() -> SecurityUtil.isAdmin(admin)).thenReturn(true);

            Optional<BusinessMessages> resultado = service.validateReabrir(grupo, grupoOriginal);

            assertTrue(resultado.isEmpty());
        }
    }

    @Test
    void validateReabrir_grupoYaAbierto_devuelveMensajeYaAbierto() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        Optional<BusinessMessages> resultado = service.validateReabrir(grupo, grupoOriginal);

        assertEquals("El grupo ya está abierto.", mensaje(resultado));
    }

    @Test
    void validateReabrir_noAdministrador_devuelveMensajeSinPermisos() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User supervisor = new User();
            auth.when(AuthUtils::getUser).thenReturn(supervisor);
            security.when(() -> SecurityUtil.isAdmin(supervisor)).thenReturn(false);

            Optional<BusinessMessages> resultado = service.validateReabrir(grupo, grupoOriginal);

            assertEquals("No tiene permisos para reabrir el grupo.", mensaje(resultado));
        }
    }

    // =====================================================================
    // insert
    // =====================================================================

    @Test
    void insert_supervisor_fijaCentroCursoEstadoYCreaModulos() {
        Centro centroX = centro();
        centroX.setCurso(CURSO_ACADEMICO);
        Curso curso = cursoConModulos(2);
        // El cliente envía datos que deben ser ignorados por el servidor.
        Grupo grupo = grupo(null, "1º DAM A", curso, centro(), 1999);

        when(grupoRepository.findByNombreCentroCursoAcademico(eq("1º DAM A"), any(), any()))
                .thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);
        when(moduloGrupoRepository.save(any(ModuloGrupo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User supervisor = new User();
            supervisor.setCentroActivo(centroX);
            auth.when(AuthUtils::getUser).thenReturn(supervisor);
            security.when(() -> SecurityUtil.isAdmin(supervisor)).thenReturn(false);

            service.insert(grupo);
        }

        assertSame(centroX, grupo.getCentro());
        assertEquals(CURSO_ACADEMICO, grupo.getCursoAcademico());
        assertEquals(EstadoGrupo.ABIERTO, grupo.getEstado());
        verify(repository).save(grupo);

        // R-Grupo-001: se crean 2 ModuloGrupo, cada uno con su grupo y su módulo asignados.
        ArgumentCaptor<ModuloGrupo> captor = ArgumentCaptor.forClass(ModuloGrupo.class);
        verify(moduloGrupoRepository, times(2)).save(captor.capture());
        List<ModuloGrupo> guardados = captor.getAllValues();
        assertEquals(2, guardados.size());
        for (ModuloGrupo moduloGrupo : guardados) {
            assertSame(grupo, moduloGrupo.getGrupo());
            assertNotNull(moduloGrupo.getModulo());
        }
    }

    @Test
    void insert_administrador_respetaCentroYCursoAcademicoDelCliente() {
        Centro centroB = centro();
        Grupo grupo = grupo(null, "1º DAM A", cursoConModulos(0), centroB, 2025);

        when(grupoRepository.findByNombreCentroCursoAcademico(eq("1º DAM A"), any(), any()))
                .thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User admin = new User();
            auth.when(AuthUtils::getUser).thenReturn(admin);
            security.when(() -> SecurityUtil.isAdmin(admin)).thenReturn(true);

            service.insert(grupo);
        }

        assertSame(centroB, grupo.getCentro());
        assertEquals(2025, grupo.getCursoAcademico());
        assertEquals(EstadoGrupo.ABIERTO, grupo.getEstado());
    }

    @Test
    void insert_validacionFalla_lanzaExcepcionYNoGuarda() {
        Grupo grupo = grupo(null, null, cursoConModulos(2), centro(), CURSO_ACADEMICO);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.insert(grupo));

        assertCadenaContieneMensaje(ex, "El nombre del grupo es obligatorio.");
        verify(repository, never()).save(any());
        verify(moduloGrupoRepository, never()).save(any());
    }

    // =====================================================================
    // update
    // =====================================================================

    @Test
    void update_grupoAbierto_guardaYRestauraInmutables() {
        Centro centroX = centro();
        Curso cursoA = cursoConModulos(0);
        Grupo original = grupo(10L, "1º DAM A", cursoA, centroX, CURSO_ACADEMICO);
        original.setEstado(EstadoGrupo.ABIERTO);
        original.setFechaCierre(null);

        // El cliente manipula campos inmutables; deben restaurarse desde original.
        Curso cursoCliente = cursoConModulos(0);
        Centro centroCliente = centro();
        Grupo grupo = grupo(10L, "1º DAM B", cursoCliente, centroCliente, 1999);
        grupo.setEstado(EstadoGrupo.CERRADO);
        grupo.setFechaCierre(LocalDateTime.now());

        when(grupoRepository.findByNombreCentroCursoAcademico(eq("1º DAM B"), any(), any()))
                .thenReturn(null);
        when(repository.save(grupo)).thenReturn(grupo);

        service.update(grupo, original);

        assertSame(cursoA, grupo.getCurso());
        assertSame(centroX, grupo.getCentro());
        assertEquals(CURSO_ACADEMICO, grupo.getCursoAcademico());
        assertEquals(EstadoGrupo.ABIERTO, grupo.getEstado());
        assertNull(grupo.getFechaCierre());
        assertEquals("1º DAM B", grupo.getNombre());
        verify(repository).save(grupo);
    }

    @Test
    void update_grupoCerrado_lanzaExcepcion() {
        Grupo original = grupo(10L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        original.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = grupo(10L, "1º DAM B", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.update(grupo, original));

        assertCadenaContieneMensaje(ex, "No se puede modificar un grupo cerrado.");
        verify(repository, never()).save(any());
    }

    // =====================================================================
    // cerrar
    // =====================================================================

    @Test
    void cerrar_grupoAbierto_pasaACerradoYRegistraFecha() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupo.setEstado(EstadoGrupo.ABIERTO);
        grupo.setFechaCierre(null);
        when(repository.save(grupo)).thenReturn(grupo);

        service.cerrar(grupo, grupoOriginal);

        assertEquals(EstadoGrupo.CERRADO, grupo.getEstado());
        assertNotNull(grupo.getFechaCierre());
        verify(repository).save(grupo);
    }

    @Test
    void cerrar_grupoYaCerrado_lanzaExcepcion() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.cerrar(grupo, grupoOriginal));

        assertCadenaContieneMensaje(ex, "El grupo ya está cerrado.");
        verify(repository, never()).save(any());
    }

    // =====================================================================
    // reabrir
    // =====================================================================

    @Test
    void reabrir_cerradoYAdministrador_pasaAAbiertoYBorraFecha() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupo.setEstado(EstadoGrupo.CERRADO);
        grupo.setFechaCierre(LocalDateTime.now());
        when(repository.save(grupo)).thenReturn(grupo);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User admin = new User();
            auth.when(AuthUtils::getUser).thenReturn(admin);
            security.when(() -> SecurityUtil.isAdmin(admin)).thenReturn(true);

            service.reabrir(grupo, grupoOriginal);
        }

        assertEquals(EstadoGrupo.ABIERTO, grupo.getEstado());
        assertNull(grupo.getFechaCierre());
        verify(repository).save(grupo);
    }

    @Test
    void reabrir_noAdministrador_lanzaExcepcionYNoGuarda() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.CERRADO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class);
             MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
            User supervisor = new User();
            auth.when(AuthUtils::getUser).thenReturn(supervisor);
            security.when(() -> SecurityUtil.isAdmin(supervisor)).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.reabrir(grupo, grupoOriginal));

            assertCadenaContieneMensaje(ex, "No tiene permisos para reabrir el grupo.");
        }

        verify(repository, never()).save(any());
    }

    @Test
    void reabrir_grupoYaAbierto_lanzaExcepcion() {
        Grupo grupoOriginal = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);
        grupoOriginal.setEstado(EstadoGrupo.ABIERTO);
        Grupo grupo = grupo(1L, "1º DAM A", cursoConModulos(0), centro(), CURSO_ACADEMICO);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.reabrir(grupo, grupoOriginal));

        assertCadenaContieneMensaje(ex, "El grupo ya está abierto.");
        verify(repository, never()).save(any());
    }

    // =====================================================================
    // allowProperties*
    // =====================================================================

    @Test
    void allowPropertiesInsert_devuelveWhitelistEsperada() {
        AllowProperties allow = service.allowPropertiesInsert();

        assertTrue(allow.allowProperty("nombre"));
        assertTrue(allow.allowProperty("curso"));
        assertTrue(allow.allowProperty("centro"));
        assertTrue(allow.allowProperty("cursoAcademico"));
        assertTrue(allow.allowProperty("alumnosGrupo"));
        assertFalse(allow.allowProperty("estado"));
        assertFalse(allow.allowProperty("fechaCierre"));
    }

    @Test
    void allowPropertiesUpdate_soloNombre() {
        AllowProperties allow = service.allowPropertiesUpdate();

        assertTrue(allow.allowProperty("nombre"));
        assertFalse(allow.allowProperty("curso"));
        assertFalse(allow.allowProperty("centro"));
        assertFalse(allow.allowProperty("cursoAcademico"));
        assertFalse(allow.allowProperty("estado"));
        assertFalse(allow.allowProperty("fechaCierre"));
    }

    @Test
    void allowPropertiesCerrar_denyAll() {
        AllowProperties allow = service.allowPropertiesCerrar();

        assertFalse(allow.allowProperty("estado"));
        assertFalse(allow.allowProperty("fechaCierre"));
        assertFalse(allow.allowProperty("nombre"));
    }

    @Test
    void allowPropertiesReabrir_denyAll() {
        AllowProperties allow = service.allowPropertiesReabrir();

        assertFalse(allow.allowProperty("estado"));
        assertFalse(allow.allowProperty("fechaCierre"));
        assertFalse(allow.allowProperty("nombre"));
    }

    @Test
    void allowPropertiesRemove_denyAll() {
        AllowProperties allow = service.allowPropertiesRemove();

        assertFalse(allow.allowProperty("estado"));
        assertFalse(allow.allowProperty("fechaCierre"));
        assertFalse(allow.allowProperty("nombre"));
    }
}
