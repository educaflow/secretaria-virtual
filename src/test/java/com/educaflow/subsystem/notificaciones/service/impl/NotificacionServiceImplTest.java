package com.educaflow.subsystem.notificaciones.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import com.educaflow.base.infrastructure.junit.JUnitHelper;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.sms.SmsSender;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import com.educaflow.subsystem.expedientes.db.PruebaV1;
import com.educaflow.subsystem.notificaciones.db.Adjunto;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.db.EstadoNotificacion;
import com.educaflow.subsystem.notificaciones.db.Sms;
import com.educaflow.subsystem.notificaciones.db.TipoNotificacion;
import com.axelor.db.Query;
import com.educaflow.subsystem.notificaciones.db.Notificacion;
import com.educaflow.subsystem.notificaciones.db.repo.CorreoRepository;
import com.educaflow.subsystem.notificaciones.db.repo.NotificacionRepository;
import com.educaflow.subsystem.notificaciones.db.repo.SmsRepository;
import com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil;
import jakarta.inject.Provider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    private static final String DNI_VALIDO = "12345678Z";
    private static final String DNI_LETRA_INCORRECTA = "12345678A";
    private static final Long ID_CORREO = 100L;
    private static final Long ID_SMS = 200L;

    private CorreoRepository correoRepository;
    private NotificacionRepository notificacionRepository;
    private EjecutorAsincrono ejecutorAsincrono;
    private Provider<MailSender> mailSenderProvider;
    private MailSender mailSender;
    private EntityManager em;
    private CorreoServiceImpl service;

    private SmsRepository smsRepository;
    private Provider<SmsSender> smsSenderProvider;
    private SmsSender smsSender;
    private SmsServiceImpl smsService;

    private Centro centroA;
    private Centro centroB;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        correoRepository = Mockito.mock(CorreoRepository.class);
        notificacionRepository = Mockito.mock(NotificacionRepository.class);
        ejecutorAsincrono = Mockito.mock(EjecutorAsincrono.class);
        mailSenderProvider = Mockito.mock(Provider.class);
        mailSender = Mockito.mock(MailSender.class);
        em = Mockito.mock(EntityManager.class);

        service = new CorreoServiceImpl(Correo.class, correoRepository);
        setField(service, NotificacionServiceImpl.class, "ejecutorAsincrono", ejecutorAsincrono);
        setField(service, NotificacionServiceImpl.class, "notificacionRepository", notificacionRepository);
        setField(service, CorreoServiceImpl.class, "mailSenderProvider", mailSenderProvider);
        lenient().when(mailSenderProvider.get()).thenReturn(mailSender);

        smsRepository = Mockito.mock(SmsRepository.class);
        smsSenderProvider = Mockito.mock(Provider.class);
        smsSender = Mockito.mock(SmsSender.class);
        smsService = new SmsServiceImpl(Sms.class, smsRepository);
        setField(smsService, NotificacionServiceImpl.class, "ejecutorAsincrono", ejecutorAsincrono);
        setField(smsService, NotificacionServiceImpl.class, "notificacionRepository", notificacionRepository);
        setField(smsService, SmsServiceImpl.class, "smsSenderProvider", smsSenderProvider);
        lenient().when(smsSenderProvider.get()).thenReturn(smsSender);

        centroA = new Centro();
        centroA.setId(1L);
        centroB = new Centro();
        centroB.setId(2L);

        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class, Mockito.withSettings().strictness(Strictness.LENIENT));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
        securityUtilMock.close();
    }

    /*************************************** Helpers ***************************************/

    private void setField(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void stubIsAdmin(boolean esAdmin) {
        securityUtilMock.when(() -> SecurityUtil.isAdmin(any())).thenReturn(esAdmin);
    }

    private void stubUsuario(User usuario) {
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuario);
    }

    private User usuarioDeCentro(Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        User usuario = new User();
        usuario.setCentroUsuarios(new ArrayList<>(List.of(centroUsuario)));
        return usuario;
    }

    private Correo correoValido() {
        Correo correo = new Correo();
        correo.setName("Aviso de prueba");
        correo.setDniDestinatario(DNI_VALIDO);
        correo.setNombre("Juan");
        correo.setApellidos("Pérez");
        correo.setPara("a@x.com");
        correo.setAsunto("Asunto de prueba");
        correo.setCuerpo("Cuerpo del correo");
        correo.setCentro(centroA);
        return correo;
    }

    private Sms smsValido() {
        Sms sms = new Sms();
        sms.setName("Aviso de prueba");
        sms.setDniDestinatario(DNI_VALIDO);
        sms.setNombre("Juan");
        sms.setApellidos("Pérez");
        sms.setTelefono("600111222");
        sms.setMensaje("Hola");
        sms.setCentro(centroA);
        return sms;
    }

    private Correo correoOriginal(EstadoNotificacion estado, Centro centro) {
        Correo correo = correoValido();
        correo.setId(ID_CORREO);
        correo.setEstado(estado);
        correo.setCentro(centro);
        return correo;
    }

    private HistorialEstado historialEstadoDeCentro(Centro centroExpediente) {
        PruebaV1 expediente = new PruebaV1();
        expediente.setCentro(centroExpediente);
        HistorialEstado historialEstado = new HistorialEstado();
        historialEstado.setId(5L);
        historialEstado.setExpediente(expediente);
        return historialEstado;
    }

    private List<String> mensajes(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent());
        return resultado.get().stream().map(BusinessMessage::getMessage).toList();
    }

    private MockedStatic<JPA> mockJpa() {
        MockedStatic<JPA> jpaMock = Mockito.mockStatic(JPA.class);
        jpaMock.when(JPA::em).thenReturn(em);
        jpaMock.when(() -> JPA.runInTransaction(any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        return jpaMock;
    }

    private MockedStatic<AppSettings> mockAppSettings() {
        MockedStatic<AppSettings> appSettingsMock = Mockito.mockStatic(AppSettings.class,
                Mockito.withSettings().strictness(Strictness.LENIENT));
        AppSettings settings = Mockito.mock(AppSettings.class);
        appSettingsMock.when(AppSettings::get).thenReturn(settings);
        lenient().when(settings.get("mail.address.from")).thenReturn("noreply@educaflow.test");
        // Convert se inicializa con el primer LocalDateTime.now(Convert.defaultZoneId) y lee el locale con valor por defecto.
        lenient().when(settings.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        return appSettingsMock;
    }

    private void enviarCon(Correo correo) {
        when(em.find(Correo.class, ID_CORREO, LockModeType.PESSIMISTIC_WRITE)).thenReturn(correo);
        try (MockedStatic<JPA> jpaMock = mockJpa();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            assertDoesNotThrow(() -> service.procesarEnvio(ID_CORREO));
        }
    }

    private Correo correoParaEnvio(EstadoNotificacion estado) {
        Correo correo = correoValido();
        correo.setId(ID_CORREO);
        correo.setEstado(estado);
        return correo;
    }

    /*************************************** insert ***************************************/

    @Test
    void insert_correoValido_asignaValoresInicialesYPersiste() {
        Correo correo = correoValido();
        stubIsAdmin(true);
        when(correoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Correo resultado = service.insert(correo);

        assertEquals(TipoNotificacion.CORREO, resultado.getTipoNotificacion());
        assertEquals(EstadoNotificacion.PENDIENTE, resultado.getEstado());
        assertEquals(0, resultado.getNumeroReintentos());
        assertNotNull(resultado.getFechaCreacion());
        assertNull(resultado.getFechaPrimerIntentoEnvio());
        assertNull(resultado.getFechaUltimoIntentoEnvio());
        assertNull(resultado.getFechaEnvio());
        assertNull(resultado.getDescripcionUltimoFallo());
        verify(correoRepository, times(1)).save(correo);
    }

    @Test
    void insert_clienteDictaTipoNotificacionSms_seGuardaComoCorreo() {
        Correo correo = correoValido();
        correo.setTipoNotificacion(TipoNotificacion.SMS);
        stubIsAdmin(true);
        when(correoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.insert(correo);

        ArgumentCaptor<Correo> guardado = ArgumentCaptor.forClass(Correo.class);
        verify(correoRepository).save(guardado.capture());
        assertEquals(TipoNotificacion.CORREO, guardado.getValue().getTipoNotificacion());
    }

    @Test
    void insert_clienteDictaCamposDelEnvio_seSobrescribenIncondicionalmente() {
        LocalDateTime fechaDictada = LocalDateTime.of(2000, 1, 1, 0, 0);
        Correo correo = correoValido();
        correo.setEstado(EstadoNotificacion.ENVIADO);
        correo.setNumeroReintentos(7);
        correo.setFechaCreacion(fechaDictada);
        correo.setFechaPrimerIntentoEnvio(fechaDictada);
        correo.setFechaUltimoIntentoEnvio(fechaDictada);
        correo.setFechaEnvio(fechaDictada);
        correo.setDescripcionUltimoFallo("falso");
        stubIsAdmin(true);
        when(correoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Correo resultado = service.insert(correo);

        assertEquals(EstadoNotificacion.PENDIENTE, resultado.getEstado());
        assertEquals(0, resultado.getNumeroReintentos());
        assertTrue(resultado.getFechaCreacion().isAfter(fechaDictada));
        assertNull(resultado.getFechaPrimerIntentoEnvio());
        assertNull(resultado.getFechaUltimoIntentoEnvio());
        assertNull(resultado.getFechaEnvio());
        assertNull(resultado.getDescripcionUltimoFallo());
    }

    @Test
    void insert_correoValido_programaElEnvioTrasCommitConSoloElId() {
        Correo correo = correoValido();
        stubIsAdmin(true);
        when(correoRepository.save(any())).thenAnswer(invocation -> {
            Correo guardado = invocation.getArgument(0);
            guardado.setId(ID_CORREO);
            return guardado;
        });
        when(em.find(Correo.class, ID_CORREO, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(correoParaEnvio(EstadoNotificacion.ENVIADO));

        service.insert(correo);

        InOrder orden = Mockito.inOrder(correoRepository, ejecutorAsincrono);
        orden.verify(correoRepository).save(correo);
        ArgumentCaptor<Runnable> tarea = ArgumentCaptor.forClass(Runnable.class);
        orden.verify(ejecutorAsincrono, times(1)).ejecutarTrasCommit(tarea.capture());

        try (MockedStatic<JPA> jpaMock = mockJpa()) {
            tarea.getValue().run();
        }
        verify(em).find(Correo.class, ID_CORREO, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void insert_correoInvalido_lanzaValidationExceptionYNoPersisteNiPrograma() {
        Correo correo = correoValido();
        correo.setName(null);
        stubIsAdmin(true);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.insert(correo));

        assertEquals("El motivo es obligatorio", ex.getMessage());
        verify(correoRepository, never()).save(any());
        verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any());
    }

    @Test
    void insert_correoConAdjuntos_seGuardanEnCascadaConUnUnicoSave() {
        Correo correo = correoValido();
        Adjunto adjuntoA = new Adjunto();
        adjuntoA.setNombreFichero("a.pdf");
        adjuntoA.setContenido(new MetaFile());
        adjuntoA.setCorreo(correo);
        Adjunto adjuntoB = new Adjunto();
        adjuntoB.setNombreFichero("b.pdf");
        adjuntoB.setContenido(new MetaFile());
        adjuntoB.setCorreo(correo);
        List<MetaFile> copias = new ArrayList<>();
        correo.setAdjuntos(new ArrayList<>(List.of(adjuntoA, adjuntoB)));
        stubIsAdmin(true);
        when(correoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtilMock.when(() -> MetaFileUtil.getSize(any())).thenReturn(1024L);
            metaFileUtilMock.when(() -> MetaFileUtil.cloneMetaFile(any())).thenAnswer(invocation -> {
                MetaFile copia = new MetaFile();
                copias.add(copia);
                return copia;
            });

            service.insert(correo);

            InOrder orden = Mockito.inOrder(MetaFileUtil.class, correoRepository);
            orden.verify(metaFileUtilMock, () -> MetaFileUtil.cloneMetaFile(any()), times(2));
            orden.verify(correoRepository, times(1)).save(correo);
        }

        assertEquals(List.of(adjuntoA, adjuntoB), correo.getAdjuntos());
        assertSame(copias.get(0), adjuntoA.getContenido());
        assertSame(copias.get(1), adjuntoB.getContenido());
    }

    @Test
    void insert_smsConTipoCorreoDictado_seGuardaComoSmsConElTelefonoNormalizado() {
        Sms sms = smsValido();
        sms.setTipoNotificacion(TipoNotificacion.CORREO);
        sms.setTelefono("600111222");
        stubIsAdmin(true);
        when(smsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        smsService.insert(sms);

        ArgumentCaptor<Sms> guardado = ArgumentCaptor.forClass(Sms.class);
        verify(smsRepository).save(guardado.capture());
        assertEquals(TipoNotificacion.SMS, guardado.getValue().getTipoNotificacion());
        assertEquals(EstadoNotificacion.PENDIENTE, guardado.getValue().getEstado());
        assertEquals("+34600111222", guardado.getValue().getTelefono());
        verify(ejecutorAsincrono).ejecutarTrasCommit(any());
    }

    /*************************************** update ***************************************/

    @Test
    void update_siempre_lanzaUnsupportedOperationExceptionConElTextoComun() {
        Correo nueva = correoValido();
        nueva.setId(ID_CORREO);
        Correo original = correoValido();
        original.setId(ID_CORREO);

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.update(nueva, original));

        assertEquals("La notificación es inmutable tras su creación.", ex.getMessage());
        verify(correoRepository, never()).save(any());
    }

    /*************************************** remove ***************************************/

    @Test
    void remove_siempre_lanzaUnsupportedOperationExceptionConElTextoComun() {
        Correo correo = correoValido();
        correo.setId(ID_CORREO);

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.remove(correo));

        assertEquals("Las notificaciones no se pueden borrar.", ex.getMessage());
        verify(correoRepository, never()).remove(any());
    }

    /*************************************** create ***************************************/

    @Test
    void create_correo_devuelveUnCorreoNuevoConTipoCorreoSinInsertarlo() {
        Correo correo = service.create();

        assertNull(correo.getId());
        assertEquals(TipoNotificacion.CORREO, correo.getTipoNotificacion());
        verifyNoInteractions(correoRepository);
    }

    @Test
    void create_sms_devuelveUnSmsNuevoConTipoSms() {
        Sms sms = smsService.create();

        assertNull(sms.getId());
        assertEquals(TipoNotificacion.SMS, sms.getTipoNotificacion());
    }

    @Test
    void create_dosLlamadas_devuelvenInstanciasDistintas() {
        assertNotSame(service.create(), service.create());
    }

    @Test
    void validateCreate_siempre_devuelveOptionalVacio() {
        assertEquals(Optional.empty(), service.validateCreate());
    }

    /*************************************** listarEnFail ***************************************/

    @Test
    @SuppressWarnings("unchecked")
    void listarEnFail_correo_devuelveLosFallidosDelTipoCorreo() {
        Correo fallido = new Correo();
        Query<Notificacion> query = Mockito.mock(Query.class);
        when(notificacionRepository.findByTipoNotificacionAndEstado(TipoNotificacion.CORREO, EstadoNotificacion.FALLIDO)).thenReturn(query);
        when(query.fetch()).thenReturn(List.of(fallido));

        assertEquals(List.of(fallido), service.listarEnFail());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarEnFail_sms_filtraPorElTipoSms() {
        Query<Notificacion> query = Mockito.mock(Query.class);
        when(notificacionRepository.findByTipoNotificacionAndEstado(TipoNotificacion.SMS, EstadoNotificacion.FALLIDO)).thenReturn(query);
        when(query.fetch()).thenReturn(List.of());

        assertEquals(List.of(), smsService.listarEnFail());
    }

    @Test
    void validateListarEnFail_siempre_devuelveOptionalVacio() {
        assertEquals(Optional.empty(), service.validateListarEnFail());
    }

    /*************************************** Notificacion sin servicio instanciable ***************************************/

    @Test
    void notificacionServiceImpl_esAbstractaYSinConstructorPublico_laFactoriaNoPuedeInstanciarlaParaNotificacion() {
        // ModelServiceFactory la encuentra por el nombre para la entidad Notificacion y pide el constructor público
        // (Class, Repository): sin él, el REST de guardar o borrar una Notificacion genérica falla.
        assertTrue(Modifier.isAbstract(NotificacionServiceImpl.class.getModifiers()));
        assertThrows(NoSuchMethodException.class,
                () -> NotificacionServiceImpl.class.getConstructor(Class.class, com.axelor.db.Repository.class));
    }

    /*************************************** reenviar ***************************************/

    @Test
    void reenviar_fallidoPorAdministrador_programaElEnvioSinGuardarYDevuelveElOriginal() {
        Correo entidadOriginal = correoOriginal(EstadoNotificacion.FALLIDO, centroA);
        Correo entidad = new Correo();
        entidad.setId(ID_CORREO);
        stubIsAdmin(true);

        Correo resultado = service.reenviar(entidad, entidadOriginal);

        assertSame(entidadOriginal, resultado);
        verify(correoRepository, never()).save(any());
        verify(ejecutorAsincrono).ejecutarTrasCommit(any());
        assertEquals(EstadoNotificacion.FALLIDO, entidadOriginal.getEstado());
    }

    @Test
    void reenviar_noFallido_lanzaValidationExceptionYNoProgramaEnvio() {
        Correo entidadOriginal = correoOriginal(EstadoNotificacion.PENDIENTE, centroA);
        Correo entidad = new Correo();
        entidad.setId(ID_CORREO);
        stubIsAdmin(true);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.reenviar(entidad, entidadOriginal));

        assertEquals("Solo se pueden reenviar notificaciones que han fallado", ex.getMessage());
        verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any());
    }

    @Test
    void reenviar_usuarioNoGestorDelCentro_lanzaValidationExceptionYNoProgramaEnvio() {
        Correo entidadOriginal = correoOriginal(EstadoNotificacion.FALLIDO, centroB);
        Correo entidad = new Correo();
        entidad.setId(ID_CORREO);
        User usuario = new User();
        stubUsuario(usuario);
        stubIsAdmin(false);

        try (MockedStatic<GestorNotificacionesUtil> gestorMock = Mockito.mockStatic(GestorNotificacionesUtil.class)) {
            gestorMock.when(() -> GestorNotificacionesUtil.idsCentrosGestionados(usuario)).thenReturn(List.of(1L));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.reenviar(entidad, entidadOriginal));

            assertEquals("No puede reenviar notificaciones de un centro que no es suyo", ex.getMessage());
        }
        verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any());
    }

    /*************************************** enviar ***************************************/

    @Test
    void enviar_primerIntentoConExito_marcaEnviadoYRegistraElIntento() {
        Correo correo = correoParaEnvio(EstadoNotificacion.PENDIENTE);
        correo.setNumeroReintentos(0);
        correo.setFechaCreacion(LocalDateTime.now(Convert.defaultZoneId).minusMinutes(1));

        enviarCon(correo);

        verify(mailSender).send(any(Mail.class));
        assertEquals(EstadoNotificacion.ENVIADO, correo.getEstado());
        assertNotNull(correo.getFechaEnvio());
        assertNull(correo.getDescripcionUltimoFallo());
        assertEquals(1, correo.getNumeroReintentos());
        assertNotNull(correo.getFechaPrimerIntentoEnvio());
        assertNotNull(correo.getFechaUltimoIntentoEnvio());
        assertFalse(correo.getFechaUltimoIntentoEnvio().isBefore(correo.getFechaPrimerIntentoEnvio()));
        assertFalse(correo.getFechaPrimerIntentoEnvio().isBefore(correo.getFechaCreacion()));
        assertFalse(correo.getFechaUltimoIntentoEnvio().isBefore(correo.getFechaCreacion()));
        verify(correoRepository).save(correo);
        verify(em).find(Correo.class, ID_CORREO, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void enviar_canalFalla_marcaFallidoConLaTrazaYSinFechaDeEnvio() {
        Correo correo = correoParaEnvio(EstadoNotificacion.PENDIENTE);
        doThrow(new RuntimeException("SMTP caído")).when(mailSender).send(any());

        enviarCon(correo);

        assertEquals(EstadoNotificacion.FALLIDO, correo.getEstado());
        assertTrue(correo.getDescripcionUltimoFallo().contains("SMTP caído"));
        assertNull(correo.getFechaEnvio());
        assertEquals(1, correo.getNumeroReintentos());
        verify(correoRepository).save(correo);
    }

    @Test
    void enviar_providerSinConfiguracion_marcaFallido() {
        Correo correo = correoParaEnvio(EstadoNotificacion.PENDIENTE);
        when(mailSenderProvider.get()).thenThrow(new IllegalArgumentException("clientId no puede ser null ni blank"));

        enviarCon(correo);

        assertEquals(EstadoNotificacion.FALLIDO, correo.getEstado());
        assertTrue(correo.getDescripcionUltimoFallo().contains("clientId no puede ser null ni blank"));
        verify(correoRepository).save(correo);
    }

    @Test
    void enviar_reintentoDeUnFallido_conservaFechaPrimerIntentoEIncrementaReintentos() {
        LocalDateTime t0 = LocalDateTime.now(Convert.defaultZoneId).minusHours(1);
        Correo correo = correoParaEnvio(EstadoNotificacion.FALLIDO);
        correo.setNumeroReintentos(2);
        correo.setFechaPrimerIntentoEnvio(t0);
        correo.setFechaUltimoIntentoEnvio(t0);
        correo.setDescripcionUltimoFallo("fallo anterior");

        enviarCon(correo);

        assertEquals(t0, correo.getFechaPrimerIntentoEnvio());
        assertTrue(correo.getFechaUltimoIntentoEnvio().isAfter(t0));
        assertEquals(3, correo.getNumeroReintentos());
        assertEquals(EstadoNotificacion.ENVIADO, correo.getEstado());
        assertNull(correo.getDescripcionUltimoFallo());
    }

    @Test
    void enviar_reintentoQueVuelveAFallar_sobrescribeFechaEnvioYDescripcion() {
        Correo correo = correoParaEnvio(EstadoNotificacion.FALLIDO);
        correo.setFechaEnvio(LocalDateTime.now(Convert.defaultZoneId).minusDays(1));
        correo.setDescripcionUltimoFallo("viejo");
        doThrow(new RuntimeException("nuevo")).when(mailSender).send(any());

        enviarCon(correo);

        assertEquals(EstadoNotificacion.FALLIDO, correo.getEstado());
        assertNull(correo.getFechaEnvio());
        assertTrue(correo.getDescripcionUltimoFallo().contains("nuevo"));
        assertFalse(correo.getDescripcionUltimoFallo().contains("viejo"));
    }

    @Test
    void enviar_yaEnviado_noHaceNada() {
        Correo correo = correoParaEnvio(EstadoNotificacion.ENVIADO);
        correo.setNumeroReintentos(1);

        enviarCon(correo);

        verify(mailSenderProvider, never()).get();
        verify(correoRepository, never()).save(any());
        assertEquals(1, correo.getNumeroReintentos());
    }

    @Test
    void enviar_idInexistente_lanzaIllegalStateException() {
        Long idInexistente = 999L;
        when(em.find(Correo.class, idInexistente, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

        try (MockedStatic<JPA> jpaMock = mockJpa();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            JUnitHelper.assertThrowsCause(IllegalStateException.class, () -> service.procesarEnvio(idInexistente));
        }
        verify(correoRepository, never()).save(any());
    }

    @Test
    void enviar_canalSms_releeConLaClaseDelCanalYEnviaPorElProveedor() {
        Sms sms = smsValido();
        sms.setId(ID_SMS);
        sms.setEstado(EstadoNotificacion.PENDIENTE);
        sms.setNumeroReintentos(0);
        sms.setTelefono("+34600111222");
        when(em.find(Sms.class, ID_SMS, LockModeType.PESSIMISTIC_WRITE)).thenReturn(sms);

        try (MockedStatic<JPA> jpaMock = mockJpa();
             MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            smsService.procesarEnvio(ID_SMS);
        }

        verify(em).find(Sms.class, ID_SMS, LockModeType.PESSIMISTIC_WRITE);
        verify(smsSender).send(any());
        assertEquals(EstadoNotificacion.ENVIADO, sms.getEstado());
        assertEquals(1, sms.getNumeroReintentos());
        verify(smsRepository).save(sms);
    }

    /*************************************** validateInsert ***************************************/

    private Optional<BusinessMessages> validateInsertComoUsuarioDeCentroA(Correo correo) {
        stubUsuario(usuarioDeCentro(centroA));
        stubIsAdmin(false);
        return service.validateInsert(correo);
    }

    @Test
    void validateInsert_todoValido_devuelveOptionalVacio() {
        assertEquals(Optional.empty(), validateInsertComoUsuarioDeCentroA(correoValido()));
    }

    @Test
    void validateInsert_motivoNulo_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setName(null);

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo)).contains("El motivo es obligatorio"));
    }

    @Test
    void validateInsert_motivoSoloEspacios_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setName("   ");

        List<String> mensajes = mensajes(validateInsertComoUsuarioDeCentroA(correo));

        assertTrue(mensajes.contains("El motivo es obligatorio"));
        assertFalse(mensajes.contains("El motivo no puede superar 255 caracteres"));
    }

    @Test
    void validateInsert_motivoDe256Caracteres_devuelveMensajeLongitud() {
        Correo correo = correoValido();
        correo.setName("a".repeat(256));

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo))
                .contains("El motivo no puede superar 255 caracteres"));
    }

    @Test
    void validateInsert_motivoDe255Caracteres_esValido() {
        Correo correo = correoValido();
        correo.setName("a".repeat(255));

        assertEquals(Optional.empty(), validateInsertComoUsuarioDeCentroA(correo));
    }

    @Test
    void validateInsert_dniNulo_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setDniDestinatario(null);

        List<String> mensajes = mensajes(validateInsertComoUsuarioDeCentroA(correo));

        assertTrue(mensajes.contains("El DNI del destinatario es obligatorio"));
        assertFalse(mensajes.contains("El DNI del destinatario no es válido; compruebe la letra"));
    }

    @Test
    void validateInsert_dniConLetraIncorrecta_devuelveMensajeNoValido() {
        Correo correo = correoValido();
        correo.setDniDestinatario(DNI_LETRA_INCORRECTA);

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo))
                .contains("El DNI del destinatario no es válido; compruebe la letra"));
    }

    @Test
    void validateInsert_nombreNulo_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setNombre(null);

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo)).contains("El nombre es obligatorio"));
    }

    @Test
    void validateInsert_apellidosVacios_devuelveMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setApellidos("");

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo)).contains("Los apellidos son obligatorios"));
    }

    @Test
    void validateInsert_nombreDe256Caracteres_devuelveMensajeLongitud() {
        Correo correo = correoValido();
        correo.setNombre("a".repeat(256));

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo))
                .contains("El nombre no puede superar 255 caracteres"));
    }

    @Test
    void validateInsert_apellidosDe256Caracteres_devuelveMensajeLongitud() {
        Correo correo = correoValido();
        correo.setApellidos("a".repeat(256));

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo))
                .contains("Los apellidos no pueden superar 255 caracteres"));
    }

    @Test
    void validateInsert_nombreYApellidosDe255Caracteres_esValido() {
        Correo correo = correoValido();
        correo.setNombre("a".repeat(255));
        correo.setApellidos("a".repeat(255));

        assertEquals(Optional.empty(), validateInsertComoUsuarioDeCentroA(correo));
    }

    @Test
    void validateInsert_centroNulo_devuelveMensajeObligatorioYNoComprobaPertenencia() {
        Correo correo = correoValido();
        correo.setCentro(null);

        List<String> mensajes = mensajes(validateInsertComoUsuarioDeCentroA(correo));

        assertTrue(mensajes.contains("El centro es obligatorio"));
        assertFalse(mensajes.contains("No puede crear notificaciones para un centro que no es suyo"));
    }

    @Test
    void validateInsert_noAdminConCentroAjeno_devuelveMensajeCentroAjeno() {
        Correo correo = correoValido();
        correo.setCentro(centroB);

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo))
                .contains("No puede crear notificaciones para un centro que no es suyo"));
    }

    @Test
    void validateInsert_administradorConCualquierCentro_esValido() {
        Correo correo = correoValido();
        correo.setCentro(centroB);
        stubUsuario(new User());
        stubIsAdmin(true);

        assertEquals(Optional.empty(), service.validateInsert(correo));
    }

    @Test
    void validateInsert_historialEstadoDelMismoCentro_esValido() {
        Correo correo = correoValido();
        correo.setHistorialEstado(historialEstadoDeCentro(centroA));

        assertEquals(Optional.empty(), validateInsertComoUsuarioDeCentroA(correo));
    }

    @Test
    void validateInsert_historialEstadoSinId_devuelveMensajeNoExiste() {
        Correo correo = correoValido();
        correo.setHistorialEstado(new HistorialEstado());

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo))
                .contains("El estado del expediente indicado no existe"));
    }

    @Test
    void validateInsert_historialEstadoSinExpediente_devuelveMensajeNoExiste() {
        HistorialEstado historialEstado = new HistorialEstado();
        historialEstado.setId(5L);
        historialEstado.setExpediente(null);
        Correo correo = correoValido();
        correo.setHistorialEstado(historialEstado);

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo))
                .contains("El estado del expediente indicado no existe"));
    }

    @Test
    void validateInsert_historialEstadoDeExpedienteDeOtroCentro_devuelveMensajeNoExiste() {
        Correo correo = correoValido();
        correo.setHistorialEstado(historialEstadoDeCentro(centroB));

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo))
                .contains("El estado del expediente indicado no existe"));
    }

    @Test
    void validateInsert_historialEstadoConCentroNulo_noSeValidaElHistorial() {
        Correo correo = correoValido();
        correo.setCentro(null);
        correo.setHistorialEstado(new HistorialEstado());

        List<String> mensajes = mensajes(validateInsertComoUsuarioDeCentroA(correo));

        assertTrue(mensajes.contains("El centro es obligatorio"));
        assertFalse(mensajes.contains("El estado del expediente indicado no existe"));
    }

    @Test
    void validateInsert_variosErrores_losAcumulaTodos() {
        Correo correo = correoValido();
        correo.setName(null);
        correo.setDniDestinatario(null);
        correo.setCentro(null);

        List<String> mensajes = mensajes(validateInsertComoUsuarioDeCentroA(correo));

        List<String> esperados = List.of("El motivo es obligatorio", "El DNI del destinatario es obligatorio",
                "El centro es obligatorio");
        assertEquals(esperados, mensajes.stream().filter(esperados::contains).toList());
    }

    @Test
    void validateInsert_datosDelCanalInvalidos_incluyeLosMensajesDelCanal() {
        Correo correo = correoValido();
        correo.setCuerpo(null);

        assertTrue(mensajes(validateInsertComoUsuarioDeCentroA(correo)).contains("El cuerpo es obligatorio"));
    }

    /*************************************** validateUpdate ***************************************/

    @Test
    void validateUpdate_siempre_devuelveMensajeInmutable() {
        assertEquals(List.of("La notificación es inmutable tras su creación."),
                mensajes(service.validateUpdate(correoValido(), correoValido())));
    }

    /*************************************** validateRemove ***************************************/

    @Test
    void validateRemove_siempre_devuelveMensajeNoBorrable() {
        assertEquals(List.of("Las notificaciones no se pueden borrar."), mensajes(service.validateRemove(correoValido())));
    }

    /*************************************** validateReenviar ***************************************/

    private Optional<BusinessMessages> validateReenviarComoAdministrador(Correo entidad, Correo entidadOriginal) {
        stubUsuario(new User());
        stubIsAdmin(true);
        return service.validateReenviar(entidad, entidadOriginal);
    }

    private Optional<BusinessMessages> validateReenviarComoGestor(Correo entidadOriginal, List<Long> idsCentrosGestionados) {
        User usuario = new User();
        stubUsuario(usuario);
        stubIsAdmin(false);
        try (MockedStatic<GestorNotificacionesUtil> gestorMock = Mockito.mockStatic(GestorNotificacionesUtil.class)) {
            gestorMock.when(() -> GestorNotificacionesUtil.idsCentrosGestionados(usuario)).thenReturn(idsCentrosGestionados);
            return service.validateReenviar(new Correo(), entidadOriginal);
        }
    }

    @Test
    void validateReenviar_fallidoYAdministrador_devuelveOptionalVacio() {
        Correo original = correoOriginal(EstadoNotificacion.FALLIDO, centroB);

        assertEquals(Optional.empty(), validateReenviarComoAdministrador(new Correo(), original));
    }

    @Test
    void validateReenviar_fallidoYGestorDelCentro_devuelveOptionalVacio() {
        Correo original = correoOriginal(EstadoNotificacion.FALLIDO, centroA);

        assertEquals(Optional.empty(), validateReenviarComoGestor(original, List.of(1L)));
    }

    @Test
    void validateReenviar_pendiente_devuelveMensajeSoloFallidas() {
        Correo original = correoOriginal(EstadoNotificacion.PENDIENTE, centroA);

        assertTrue(mensajes(validateReenviarComoAdministrador(new Correo(), original))
                .contains("Solo se pueden reenviar notificaciones que han fallado"));
    }

    @Test
    void validateReenviar_enviado_devuelveMensajeSoloFallidas() {
        Correo original = correoOriginal(EstadoNotificacion.ENVIADO, centroA);

        assertTrue(mensajes(validateReenviarComoAdministrador(new Correo(), original))
                .contains("Solo se pueden reenviar notificaciones que han fallado"));
    }

    @Test
    void validateReenviar_noGestorDelCentroDeLaNotificacion_devuelveMensajeCentroAjeno() {
        Correo original = correoOriginal(EstadoNotificacion.FALLIDO, centroB);

        assertTrue(mensajes(validateReenviarComoGestor(original, List.of(1L)))
                .contains("No puede reenviar notificaciones de un centro que no es suyo"));
    }

    @Test
    void validateReenviar_usuarioSinCentrosGestionados_devuelveMensajeCentroAjeno() {
        Correo original = correoOriginal(EstadoNotificacion.FALLIDO, centroA);

        assertTrue(mensajes(validateReenviarComoGestor(original, List.of(-1L)))
                .contains("No puede reenviar notificaciones de un centro que no es suyo"));
    }

    @Test
    void validateReenviar_seEvaluaSobreElOriginalNoSobreLaEntidad() {
        Correo entidad = new Correo();
        entidad.setEstado(EstadoNotificacion.FALLIDO);
        Correo original = correoOriginal(EstadoNotificacion.PENDIENTE, centroA);

        assertTrue(mensajes(validateReenviarComoAdministrador(entidad, original))
                .contains("Solo se pueden reenviar notificaciones que han fallado"));
    }

    @Test
    void validateReenviar_originalNull_lanzaNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> service.validateReenviar(new Correo(), null));

        assertEquals("entidadOriginal no puede ser null", ex.getMessage());
    }

    /*************************************** AllowProperties ***************************************/

    @Test
    void allowPropertiesInsert_permiteComunesYDelCanalYDeniegaLosDelServidor() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        List.of("name", "dniDestinatario", "nombre", "apellidos", "centro", "historialEstado",
                        "para", "enCopia", "enCopiaOculta", "asunto", "cuerpo", "adjuntos")
                .forEach(propiedad -> assertTrue(allowProperties.allowProperty(propiedad), propiedad));
        List.of("tipoNotificacion", "destino", "nombreExpediente", "estado", "fechaCreacion",
                        "fechaPrimerIntentoEnvio", "fechaUltimoIntentoEnvio", "fechaEnvio", "numeroReintentos",
                        "descripcionUltimoFallo")
                .forEach(propiedad -> assertFalse(allowProperties.allowProperty(propiedad), propiedad));
    }

    @Test
    void allowPropertiesUpdate_denegaTodo() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        List.of("name", "para", "estado", "centro")
                .forEach(propiedad -> assertFalse(allowProperties.allowProperty(propiedad), propiedad));
    }

    @Test
    void allowPropertiesRemove_denegaTodo() {
        AllowProperties allowProperties = service.allowPropertiesRemove();

        List.of("name", "estado", "centro")
                .forEach(propiedad -> assertFalse(allowProperties.allowProperty(propiedad), propiedad));
    }

    @Test
    void allowPropertiesReenviar_denegaTodo() {
        AllowProperties allowProperties = service.allowPropertiesReenviar();

        List.of("estado", "centro", "numeroReintentos", "fechaEnvio")
                .forEach(propiedad -> assertFalse(allowProperties.allowProperty(propiedad), propiedad));
    }
}
