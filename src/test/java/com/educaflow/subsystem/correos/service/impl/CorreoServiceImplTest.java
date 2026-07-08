package com.educaflow.subsystem.correos.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Query;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.correos.db.Adjunto;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.db.repo.CorreoRepository;
import com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor;
import com.educaflow.subsystem.correos.infrastructure.PostCommitRunner;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorreoServiceImplTest {

    // DNI real válido (letra de control TRWAGMYFPDXBNJZSQVHLCKE, 12345678 % 23 == 14 -> 'Z')
    private static final String DNI_VALIDO = "12345678Z";
    private static final String DNI_LETRA_INCORRECTA = "12345678A";

    private CorreoRepository repository;
    private CorreoServiceImpl service;
    private MailSender mailSender;
    private CorreoAsyncExecutor correoAsyncExecutor;

    private Centro centroA;
    private Centro centroB;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() throws Exception {
        repository = Mockito.mock(CorreoRepository.class);
        service = new CorreoServiceImpl(Correo.class, repository);

        mailSender = Mockito.mock(MailSender.class);
        correoAsyncExecutor = Mockito.mock(CorreoAsyncExecutor.class);
        setField(service, "mailSender", mailSender);
        setField(service, "correoAsyncExecutor", correoAsyncExecutor);

        centroA = new Centro();
        centroA.setId(1L);
        centroB = new Centro();
        centroB.setId(2L);

        // lenient: I18n se programa en el setup pero no todos los tests recorren las ramas que lo
        // consumen (happy paths que no producen mensaje).
        i18nMock = Mockito.mockStatic(I18n.class,
                Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // lenient: no todos los tests estiman SecurityUtil (p.ej. validateUpdate/validateRemove).
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class,
                Mockito.withSettings().strictness(Strictness.LENIENT));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
        securityUtilMock.close();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CorreoServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private String mensaje(Optional<BusinessMessages> optional) {
        assertTrue(optional.isPresent());
        return optional.get().get(0).getMessage();
    }

    private void stubIsAdmin(boolean esAdmin) {
        securityUtilMock.when(() -> SecurityUtil.isAdmin(any())).thenReturn(esAdmin);
    }

    private CentroUsuario centroUsuario(Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        return centroUsuario;
    }

    private User usuarioDeCentro(Centro centro) {
        User user = new User();
        user.setCentroUsuarios(List.of(centroUsuario(centro)));
        return user;
    }

    private Correo correoValido() {
        Correo correo = new Correo();
        correo.setDniDestinatario(DNI_VALIDO);
        correo.setNombre("Juan");
        correo.setApellidos("Pérez");
        correo.setPara("a@x.com");
        correo.setAsunto("Asunto de prueba");
        correo.setCuerpo("Cuerpo del correo");
        correo.setCentro(centroA);
        correo.setHistorialEstado(null);
        return correo;
    }

    private Correo correoParaEnvio() {
        Correo correo = new Correo();
        correo.setEstado(EstadoCorreo.PENDIENTE);
        correo.setPara("a@x.com");
        correo.setEnCopia(null);
        correo.setEnCopiaOculta(null);
        correo.setAsunto("Asunto de prueba");
        correo.setCuerpo("Cuerpo del correo");
        correo.setAdjuntos(List.of());
        correo.setFechaPrimerIntentoEnvio(null);
        correo.setNumeroReintentos(0);
        return correo;
    }

    private MockedStatic<JPA> mockJpaRunInTransaction() {
        MockedStatic<JPA> jpaMock = Mockito.mockStatic(JPA.class);
        jpaMock.when(() -> JPA.runInTransaction(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                });
        return jpaMock;
    }

    private MockedStatic<AppSettings> mockAppSettings() {
        MockedStatic<AppSettings> appSettingsMock = Mockito.mockStatic(AppSettings.class);
        AppSettings settingsMock = Mockito.mock(AppSettings.class);
        appSettingsMock.when(AppSettings::get).thenReturn(settingsMock);
        when(settingsMock.get("mail.address.from")).thenReturn("noreply@educaflow.test");
        return appSettingsMock;
    }

    /* ------------------------------------------------------------------ */
    /* insert                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void insert_correoValido_asignaValoresInicialesYPersiste() {
        Correo correo = correoValido();
        stubIsAdmin(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<PostCommitRunner> postCommitMock = Mockito.mockStatic(PostCommitRunner.class)) {
            Correo resultado = service.insert(correo);

            assertEquals(EstadoCorreo.PENDIENTE, resultado.getEstado());
            assertNotNull(resultado.getFechaCreacion());
            assertEquals(0, resultado.getNumeroReintentos());
            assertNull(resultado.getFechaPrimerIntentoEnvio());
            assertNull(resultado.getFechaUltimoIntentoEnvio());
            assertNull(resultado.getFechaEnvio());
            assertNull(resultado.getDescripcionUltimoFallo());
            verify(repository).save(correo);
            postCommitMock.verify(() -> PostCommitRunner.runAfterCommit(any()));
        }
    }

    @Test
    void insert_clienteEnviaEstadoOFechasFalsas_seSobrescribenIncondicionalmente() {
        Correo correo = correoValido();
        correo.setEstado(EstadoCorreo.SUCCESS);
        correo.setFechaEnvio(LocalDateTime.of(2000, 1, 1, 0, 0));
        correo.setNumeroReintentos(99);
        stubIsAdmin(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<PostCommitRunner> postCommitMock = Mockito.mockStatic(PostCommitRunner.class)) {
            Correo resultado = service.insert(correo);

            assertEquals(EstadoCorreo.PENDIENTE, resultado.getEstado());
            assertEquals(0, resultado.getNumeroReintentos());
            assertNull(resultado.getFechaEnvio());
        }
    }

    @Test
    void insert_correoInvalido_lanzaValidationExceptionYNoPersiste() {
        Correo correo = correoValido();
        correo.setDniDestinatario(null);
        stubIsAdmin(true);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.insert(correo));

        assertEquals("El DNI del destinatario es obligatorio", ex.getMessage());
        verify(repository, never()).save(any());
        verify(correoAsyncExecutor, never()).submit(any());
    }

    /* ------------------------------------------------------------------ */
    /* update                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void update_siempre_lanzaUnsupportedOperationException() {
        Correo nuevo = correoValido();
        Correo original = correoValido();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.update(nuevo, original));

        assertEquals("El correo es inmutable tras su creación.", ex.getMessage());
    }

    /* ------------------------------------------------------------------ */
    /* remove                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void remove_siempre_lanzaUnsupportedOperationException() {
        Correo correo = correoValido();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.remove(correo));

        assertEquals("Los correos no se pueden borrar.", ex.getMessage());
        verify(repository, never()).remove(any());
    }

    /* ------------------------------------------------------------------ */
    /* reenviar                                                           */
    /* ------------------------------------------------------------------ */

    @Test
    void reenviar_correoEnFail_programaEnvioAsincronoSinPersistirCambiosPropios() {
        Correo entidadOriginal = correoValido();
        entidadOriginal.setId(100L);
        entidadOriginal.setEstado(EstadoCorreo.FAIL);
        entidadOriginal.setCentro(centroA);
        Correo entidad = new Correo();
        entidad.setId(100L);
        stubIsAdmin(true);

        try (MockedStatic<PostCommitRunner> postCommitMock = Mockito.mockStatic(PostCommitRunner.class)) {
            Correo resultado = service.reenviar(entidad, entidadOriginal);

            assertSame(entidadOriginal, resultado);
            verify(repository, never()).save(any());
            postCommitMock.verify(() -> PostCommitRunner.runAfterCommit(any()));
        }
    }

    @Test
    void reenviar_correoNoEnFail_lanzaValidationExceptionYNoProgramaEnvio() {
        Correo entidadOriginal = correoValido();
        entidadOriginal.setId(100L);
        entidadOriginal.setEstado(EstadoCorreo.PENDIENTE);
        entidadOriginal.setCentro(centroA);
        Correo entidad = new Correo();
        entidad.setId(100L);
        stubIsAdmin(true);

        try (MockedStatic<PostCommitRunner> postCommitMock = Mockito.mockStatic(PostCommitRunner.class)) {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.reenviar(entidad, entidadOriginal));

            assertEquals("Solo se pueden reenviar correos que han fallado", ex.getMessage());
            postCommitMock.verify(() -> PostCommitRunner.runAfterCommit(any()), never());
        }
    }

    @Test
    void reenviar_usuarioNoAdminDeOtroCentro_lanzaValidationException() {
        Correo entidadOriginal = correoValido();
        entidadOriginal.setId(100L);
        entidadOriginal.setEstado(EstadoCorreo.FAIL);
        entidadOriginal.setCentro(centroB);
        Correo entidad = new Correo();
        entidad.setId(100L);
        stubIsAdmin(false);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuarioDeCentro(centroA));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.reenviar(entidad, entidadOriginal));

        assertEquals("No puede reenviar correos de un centro que no es suyo", ex.getMessage());
    }

    /* ------------------------------------------------------------------ */
    /* enviarCorreo                                                       */
    /* ------------------------------------------------------------------ */

    @Test
    void enviarCorreo_envioExitoso_marcaSuccessYRegistraFechaEnvio() {
        Long correoId = 1L;
        Correo correo = correoParaEnvio();
        correo.setId(correoId);
        when(repository.find(correoId)).thenReturn(correo);

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            service.enviarCorreo(correoId);
        }

        assertEquals(EstadoCorreo.SUCCESS, correo.getEstado());
        assertNotNull(correo.getFechaEnvio());
        assertNull(correo.getDescripcionUltimoFallo());
        verify(repository).save(correo);
    }

    @Test
    void enviarCorreo_envioFalla_marcaFailYGuardaTrazaCompleta() {
        Long correoId = 1L;
        Correo correo = correoParaEnvio();
        correo.setId(correoId);
        when(repository.find(correoId)).thenReturn(correo);
        doThrow(new RuntimeException("SMTP caído")).when(mailSender).send(any());

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            assertDoesNotThrow(() -> service.enviarCorreo(correoId));
        }

        assertEquals(EstadoCorreo.FAIL, correo.getEstado());
        assertNull(correo.getFechaEnvio());
        assertNotNull(correo.getDescripcionUltimoFallo());
        assertTrue(correo.getDescripcionUltimoFallo().contains("SMTP caído"));
        verify(repository).save(correo);
    }

    @Test
    void enviarCorreo_envioExitoso_sobrescribeIncondicionalmenteDescripcionDeFalloPrevia() {
        Long correoId = 1L;
        Correo correo = correoParaEnvio();
        correo.setId(correoId);
        correo.setEstado(EstadoCorreo.FAIL);
        correo.setDescripcionUltimoFallo("fallo anterior");
        when(repository.find(correoId)).thenReturn(correo);

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            service.enviarCorreo(correoId);
        }

        assertEquals(EstadoCorreo.SUCCESS, correo.getEstado());
        assertNull(correo.getDescripcionUltimoFallo());
        assertNotNull(correo.getFechaEnvio());
    }

    @Test
    void enviarCorreo_envioFalla_sobrescribeIncondicionalmenteFechaEnvioPrevia() {
        Long correoId = 1L;
        Correo correo = correoParaEnvio();
        correo.setId(correoId);
        correo.setFechaEnvio(LocalDateTime.now().minusDays(1));
        when(repository.find(correoId)).thenReturn(correo);
        doThrow(new RuntimeException("SMTP caído")).when(mailSender).send(any());

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            service.enviarCorreo(correoId);
        }

        assertEquals(EstadoCorreo.FAIL, correo.getEstado());
        assertNull(correo.getFechaEnvio());
    }

    @Test
    void enviarCorreo_correoYaEnSuccess_noHaceNadaIdempotente() {
        Long correoId = 1L;
        Correo correo = correoParaEnvio();
        correo.setId(correoId);
        correo.setEstado(EstadoCorreo.SUCCESS);
        when(repository.find(correoId)).thenReturn(correo);

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction()) {
            service.enviarCorreo(correoId);
        }

        verify(mailSender, never()).send(any());
        verify(repository, never()).save(any());
    }

    @Test
    void enviarCorreo_correoIdInexistente_noHaceNada() {
        Long correoId = 1L;
        when(repository.find(correoId)).thenReturn(null);

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction()) {
            assertDoesNotThrow(() -> service.enviarCorreo(correoId));
        }

        verify(mailSender, never()).send(any());
    }

    @Test
    void enviarCorreo_primerIntento_fijaFechaPrimerIntentoEnvio() {
        Long correoId = 1L;
        Correo correo = correoParaEnvio();
        correo.setId(correoId);
        correo.setFechaPrimerIntentoEnvio(null);
        correo.setNumeroReintentos(0);
        when(repository.find(correoId)).thenReturn(correo);

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            service.enviarCorreo(correoId);
        }

        assertNotNull(correo.getFechaPrimerIntentoEnvio());
        assertEquals(1, correo.getNumeroReintentos());
        assertNotNull(correo.getFechaUltimoIntentoEnvio());
    }

    @Test
    void enviarCorreo_reintento_noSobrescribeFechaPrimerIntentoEnvio() {
        Long correoId = 1L;
        LocalDateTime t0 = LocalDateTime.of(2024, 1, 1, 10, 0);
        Correo correo = correoParaEnvio();
        correo.setId(correoId);
        correo.setFechaPrimerIntentoEnvio(t0);
        correo.setEstado(EstadoCorreo.FAIL);
        correo.setNumeroReintentos(1);
        when(repository.find(correoId)).thenReturn(correo);

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            service.enviarCorreo(correoId);
        }

        assertEquals(t0, correo.getFechaPrimerIntentoEnvio());
        assertEquals(2, correo.getNumeroReintentos());
        assertTrue(correo.getFechaUltimoIntentoEnvio().isAfter(t0));
    }

    @Test
    void enviarCorreo_separaDireccionesDeParaCcYBccPorComas() {
        Long correoId = 1L;
        Correo correo = correoParaEnvio();
        correo.setId(correoId);
        correo.setPara("a@x.com, b@x.com");
        correo.setEnCopia("c@x.com");
        correo.setEnCopiaOculta(" d@x.com , e@x.com ");
        correo.setAdjuntos(List.of());
        when(repository.find(correoId)).thenReturn(correo);

        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            service.enviarCorreo(correoId);
        }

        verify(mailSender).send(mailCaptor.capture());
        Mail mail = mailCaptor.getValue();
        assertEquals(List.of("a@x.com", "b@x.com"), mail.to());
        assertEquals(List.of("c@x.com"), mail.cc());
        assertEquals(List.of("d@x.com", "e@x.com"), mail.bcc());
        assertEquals("noreply@educaflow.test", mail.from());
        assertEquals(correo.getAsunto(), mail.subject());
        assertEquals(correo.getCuerpo(), mail.htmlBody());
        assertEquals(correo.getCuerpo(), mail.textBody());
    }

    @Test
    void enviarCorreo_correoConAdjuntos_construyeAttachsDesdeMetaFile() {
        Long correoId = 1L;
        Correo correo = correoParaEnvio();
        correo.setId(correoId);

        MetaFile metaFile = Mockito.mock(MetaFile.class);
        when(metaFile.getFileType()).thenReturn("application/pdf");
        Adjunto adjunto = new Adjunto();
        adjunto.setNombreFichero("doc.pdf");
        adjunto.setContenido(metaFile);
        correo.setAdjuntos(List.of(adjunto));

        when(repository.find(correoId)).thenReturn(correo);

        byte[] contenido = {1, 2, 3, 4};
        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings();
             MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtilMock.when(() -> MetaFileUtil.downloadContent(metaFile)).thenReturn(contenido);

            service.enviarCorreo(correoId);
        }

        verify(mailSender).send(mailCaptor.capture());
        Mail mail = mailCaptor.getValue();
        assertEquals(1, mail.attachs().size());
        Attach attach = mail.attachs().get(0);
        assertEquals("doc.pdf", attach.fileName());
        assertEquals(contenido, attach.data());
        assertEquals("application/pdf", attach.mimeType());
    }

    /* ------------------------------------------------------------------ */
    /* listarCorreosEnFail                                                */
    /* ------------------------------------------------------------------ */

    @Test
    @SuppressWarnings("unchecked")
    void listarCorreosEnFail_delegaEnFinderDelRepositorio() {
        List<Correo> lista = List.of(new Correo(), new Correo());
        Query<Correo> queryMock = Mockito.mock(Query.class);
        when(repository.findByEstado(EstadoCorreo.FAIL)).thenReturn(queryMock);
        when(queryMock.fetch()).thenReturn(lista);

        List<Correo> resultado = service.listarCorreosEnFail();

        assertEquals(lista, resultado);
        verify(repository).findByEstado(EstadoCorreo.FAIL);
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_todoValido_devuelveOptionalVacio() {
        Correo correo = correoValido();
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_dniDestinatarioNulo_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setDniDestinatario(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El DNI del destinatario es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_dniDestinatarioConLetraDeControlIncorrecta_devuelveMensajeNoValido() {
        Correo correo = correoValido();
        correo.setDniDestinatario(DNI_LETRA_INCORRECTA);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El DNI del destinatario no es válido; compruebe la letra", mensaje(resultado));
    }

    @Test
    void validateInsert_nombreNulo_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setNombre(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El nombre es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_apellidosNulos_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setApellidos(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("Los apellidos son obligatorios", mensaje(resultado));
    }

    @Test
    void validateInsert_paraVacioTrasSepararPorComas_devuelveMensajeAlMenosUnDestinatario() {
        stubIsAdmin(true);

        // Arrange/Act/Assert escenario 1: para=""
        Correo correoVacio = correoValido();
        correoVacio.setPara("");
        Optional<BusinessMessages> resultadoVacio = service.validateInsert(correoVacio);
        assertEquals("Debe indicar al menos un destinatario en el «para»", mensaje(resultadoVacio));

        // Arrange/Act/Assert escenario 2: para=" , , "
        Correo correoSoloComas = correoValido();
        correoSoloComas.setPara(" , , ");
        Optional<BusinessMessages> resultadoSoloComas = service.validateInsert(correoSoloComas);
        assertEquals("Debe indicar al menos un destinatario en el «para»", mensaje(resultadoSoloComas));
    }

    @Test
    void validateInsert_paraConDireccionInvalida_devuelveMensajeFormatoInvalido() {
        Correo correo = correoValido();
        correo.setPara("valida@x.com, no-es-un-email");
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)",
                mensaje(resultado));
    }

    @Test
    void validateInsert_enCopiaConDireccionInvalida_devuelveMensajeFormatoInvalido() {
        Correo correo = correoValido();
        correo.setEnCopia("no-es-un-email");
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El «en copia» debe contener direcciones de correo válidas", mensaje(resultado));
    }

    @Test
    void validateInsert_enCopiaVacia_noSeValida() {
        Correo correo = correoValido();
        correo.setEnCopia(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_enCopiaOcultaConDireccionInvalida_devuelveMensajeFormatoInvalido() {
        Correo correo = correoValido();
        correo.setEnCopiaOculta("no-es-un-email");
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El «en copia oculta» debe contener direcciones de correo válidas", mensaje(resultado));
    }

    @Test
    void validateInsert_asuntoNulo_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setAsunto(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El asunto es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_asuntoSuperaDoscientosCincuentaYCincoCaracteres_devuelveMensajeLongitud() {
        Correo correo = correoValido();
        correo.setAsunto("A".repeat(256));
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El asunto no puede superar 255 caracteres", mensaje(resultado));
    }

    @Test
    void validateInsert_asuntoDeDoscientosCincuentaYCincoCaracteres_esValido() {
        Correo correo = correoValido();
        correo.setAsunto("A".repeat(255));
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_cuerpoNulo_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setCuerpo(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El cuerpo es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_centroNulo_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setCentro(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("El centro es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_usuarioNoAdminConCentroDeOtroCentro_devuelveMensajeCentroNoSuyo() {
        Correo correo = correoValido();
        correo.setCentro(centroB);
        stubIsAdmin(false);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuarioDeCentro(centroA));

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertEquals("No puede crear correos para un centro que no es suyo", mensaje(resultado));
    }

    @Test
    void validateInsert_usuarioNoAdminConCentroPropio_esValido() {
        Correo correo = correoValido();
        correo.setCentro(centroA);
        stubIsAdmin(false);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuarioDeCentro(centroA));

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_administradorConCualquierCentro_esValido() {
        Correo correo = correoValido();
        correo.setCentro(centroB);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(correo);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateInsert_historialEstadoIndicadoNoExiste_devuelveMensajeNoExiste() {
        Correo correo = correoValido();
        HistorialEstado historialEstado = new HistorialEstado();
        historialEstado.setId(5L);
        correo.setHistorialEstado(historialEstado);
        stubIsAdmin(true);

        JpaRepository<HistorialEstado> repoMock = Mockito.mock(JpaRepository.class);
        try (MockedStatic<JpaRepository> jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class)) {
            jpaRepositoryMock.when(() -> JpaRepository.of(HistorialEstado.class)).thenReturn(repoMock);
            when(repoMock.find(5L)).thenReturn(null);

            Optional<BusinessMessages> resultado = service.validateInsert(correo);

            assertEquals("El historial de estado indicado no existe", mensaje(resultado));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateInsert_historialEstadoIndicadoExiste_esValido() {
        Correo correo = correoValido();
        HistorialEstado historialEstado = new HistorialEstado();
        historialEstado.setId(5L);
        correo.setHistorialEstado(historialEstado);
        stubIsAdmin(true);

        JpaRepository<HistorialEstado> repoMock = Mockito.mock(JpaRepository.class);
        try (MockedStatic<JpaRepository> jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class)) {
            jpaRepositoryMock.when(() -> JpaRepository.of(HistorialEstado.class)).thenReturn(repoMock);
            when(repoMock.find(5L)).thenReturn(new HistorialEstado());

            Optional<BusinessMessages> resultado = service.validateInsert(correo);

            assertTrue(resultado.isEmpty());
        }
    }

    @Test
    void validateInsert_historialEstadoNoIndicado_noSeValida() {
        Correo correo = correoValido();
        correo.setHistorialEstado(null);
        stubIsAdmin(true);

        try (MockedStatic<JpaRepository> jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class)) {
            Optional<BusinessMessages> resultado = service.validateInsert(correo);

            assertTrue(resultado.isEmpty());
            jpaRepositoryMock.verifyNoInteractions();
        }
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_siempre_devuelveMensajeInmutabilidad() {
        Correo nuevo = correoValido();
        Correo original = correoValido();

        Optional<BusinessMessages> resultado = service.validateUpdate(nuevo, original);

        assertEquals("El correo es inmutable tras su creación.", mensaje(resultado));
    }

    /* ------------------------------------------------------------------ */
    /* validateRemove                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateRemove_siempre_devuelveMensajeNoSePuedeBorrar() {
        Correo correo = correoValido();

        Optional<BusinessMessages> resultado = service.validateRemove(correo);

        assertEquals("Los correos no se pueden borrar.", mensaje(resultado));
    }

    /* ------------------------------------------------------------------ */
    /* validateReenviar                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void validateReenviar_estadoFail_devuelveOptionalVacio() {
        Correo entidadOriginal = correoValido();
        entidadOriginal.setEstado(EstadoCorreo.FAIL);
        entidadOriginal.setCentro(centroA);
        stubIsAdmin(true);
        Correo entidad = new Correo();

        Optional<BusinessMessages> resultado = service.validateReenviar(entidad, entidadOriginal);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateReenviar_estadoDistintoDeFail_devuelveMensajeSoloFallidos() {
        Correo entidadOriginal = correoValido();
        entidadOriginal.setEstado(EstadoCorreo.PENDIENTE);
        entidadOriginal.setCentro(centroA);
        stubIsAdmin(true);
        Correo entidad = new Correo();

        Optional<BusinessMessages> resultado = service.validateReenviar(entidad, entidadOriginal);

        assertEquals("Solo se pueden reenviar correos que han fallado", mensaje(resultado));
    }

    @Test
    void validateReenviar_usuarioNoAdminDeOtroCentro_devuelveMensajeCentroNoSuyo() {
        Correo entidadOriginal = correoValido();
        entidadOriginal.setEstado(EstadoCorreo.FAIL);
        entidadOriginal.setCentro(centroB);
        stubIsAdmin(false);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuarioDeCentro(centroA));
        Correo entidad = new Correo();

        Optional<BusinessMessages> resultado = service.validateReenviar(entidad, entidadOriginal);

        assertEquals("No puede reenviar correos de un centro que no es suyo", mensaje(resultado));
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesInsert                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_permiteCamposDeClienteYDeniegaCamposServidor() {
        AllowProperties result = service.allowPropertiesInsert();

        assertTrue(result.allowProperty("dniDestinatario"));
        assertTrue(result.allowProperty("nombre"));
        assertTrue(result.allowProperty("apellidos"));
        assertTrue(result.allowProperty("para"));
        assertTrue(result.allowProperty("enCopia"));
        assertTrue(result.allowProperty("enCopiaOculta"));
        assertTrue(result.allowProperty("asunto"));
        assertTrue(result.allowProperty("cuerpo"));
        assertTrue(result.allowProperty("centro"));
        assertTrue(result.allowProperty("historialEstado"));
        assertTrue(result.allowProperty("adjuntos"));

        assertFalse(result.allowProperty("estado"));
        assertFalse(result.allowProperty("fechaCreacion"));
        assertFalse(result.allowProperty("fechaPrimerIntentoEnvio"));
        assertFalse(result.allowProperty("fechaUltimoIntentoEnvio"));
        assertFalse(result.allowProperty("fechaEnvio"));
        assertFalse(result.allowProperty("numeroReintentos"));
        assertFalse(result.allowProperty("descripcionUltimoFallo"));
        assertFalse(result.allowProperty("nombreExpediente"));
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesReenviar                                            */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesReenviar_devuelveWhitelistVacia() {
        AllowProperties result = service.allowPropertiesReenviar();

        assertFalse(result.allowProperty("estado"));
        assertFalse(result.allowProperty("centro"));
    }
}
