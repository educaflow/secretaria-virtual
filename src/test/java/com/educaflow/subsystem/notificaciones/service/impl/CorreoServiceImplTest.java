package com.educaflow.subsystem.notificaciones.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.db.Query;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.notificaciones.db.Adjunto;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.db.EstadoNotificacion;
import com.educaflow.subsystem.notificaciones.db.repo.CorreoRepository;
import jakarta.inject.Provider;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorreoServiceImplTest {

    private static final String DNI_VALIDO = "12345678Z";
    private static final long MB = 1024L * 1024;

    private static final String MENSAJE_AL_MENOS_UN_DESTINATARIO = "Debe indicar al menos un destinatario en el «para»";
    private static final String MENSAJE_UNA_SOLA_DIRECCION =
            "El «para» debe contener una sola dirección de correo; use «en copia» para añadir más destinatarios";
    private static final String MENSAJE_DIRECCION_REPETIDA =
            "Una misma dirección de correo no puede aparecer más de una vez entre el «para», el «en copia» y el «en copia oculta»";
    private static final String MENSAJE_ASUNTO_CARACTERES_DE_CONTROL =
            "El asunto no puede contener saltos de línea ni caracteres de control";
    private static final String MENSAJE_TAMANO_TOTAL_ADJUNTOS = "Los adjuntos del correo no pueden superar 25 MB en total";

    private CorreoRepository correoRepository;
    private Provider<MailSender> mailSenderProvider;
    private MailSender mailSender;
    private CorreoServiceImpl service;

    private Centro centroA;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        correoRepository = mock(CorreoRepository.class);
        mailSenderProvider = mock(Provider.class);
        mailSender = mock(MailSender.class);

        service = new CorreoServiceImpl(Correo.class, correoRepository);
        setField(CorreoServiceImpl.class, "mailSenderProvider", mailSenderProvider);
        lenient().when(mailSenderProvider.get()).thenReturn(mailSender);

        centroA = new Centro();
        centroA.setId(1L);

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

    private void setField(Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
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

    private MetaFile metaFile(long id) {
        MetaFile metaFile = new MetaFile();
        metaFile.setId(id);
        return metaFile;
    }

    private Adjunto adjunto(String nombreFichero, MetaFile contenido) {
        Adjunto adjunto = new Adjunto();
        adjunto.setNombreFichero(nombreFichero);
        adjunto.setContenido(contenido);
        return adjunto;
    }

    private BusinessMessages validar(Correo correo) {
        // Como administrador: las validaciones de centro son comunes y se prueban en NotificacionServiceImplTest.
        securityUtilMock.when(() -> SecurityUtil.isAdmin(any())).thenReturn(true);
        return service.validateInsert(correo).orElseGet(BusinessMessages::new);
    }

    private List<String> textos(BusinessMessages messages) {
        return messages.stream().map(BusinessMessage::getMessage).toList();
    }

    private MockedStatic<AppSettings> mockAppSettings() {
        MockedStatic<AppSettings> appSettingsMock = Mockito.mockStatic(AppSettings.class);
        AppSettings settings = mock(AppSettings.class);
        appSettingsMock.when(AppSettings::get).thenReturn(settings);
        when(settings.get("mail.address.from")).thenReturn("noreply@educaflow.test");
        return appSettingsMock;
    }

    private Mail enviarYCapturar(Correo correo) {
        try (MockedStatic<AppSettings> appSettingsMock = mockAppSettings()) {
            service.enviar(correo);
        }
        ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailSender).send(mailCaptor.capture());
        return mailCaptor.getValue();
    }

    /*************************************** validateInsert ***************************************/

    @Test
    void validateDatosDelCanal_correoValido_noAnadeMensajes() {
        Correo correo = correoValido();
        correo.setEnCopia("b@x.com, c@x.com");
        correo.setEnCopiaOculta("d@x.com");

        assertTrue(validar(correo).isValid());
    }

    @Test
    void validateDatosDelCanal_paraNulo_anadeMensajeAlMenosUnDestinatario() {
        Correo correo = correoValido();
        correo.setPara(null);

        assertTrue(textos(validar(correo)).contains(MENSAJE_AL_MENOS_UN_DESTINATARIO));
    }

    @Test
    void validateDatosDelCanal_paraSoloComasYEspacios_anadeMensajeAlMenosUnDestinatario() {
        Correo correo = correoValido();
        correo.setPara(" , ,");

        List<String> mensajes = textos(validar(correo));

        assertTrue(mensajes.contains(MENSAJE_AL_MENOS_UN_DESTINATARIO));
        assertFalse(mensajes.contains(MENSAJE_UNA_SOLA_DIRECCION));
    }

    @Test
    void validateDatosDelCanal_paraConDosDirecciones_anadeMensajeUnaSolaDireccion() {
        Correo correo = correoValido();
        correo.setPara("a@x.com, b@x.com");

        assertTrue(textos(validar(correo)).contains(MENSAJE_UNA_SOLA_DIRECCION));
    }

    @Test
    void validateDatosDelCanal_paraConFormatoInvalido_anadeMensajeFormato() {
        Correo correo = correoValido();
        correo.setPara("no-es-un-correo");

        assertTrue(textos(validar(correo)).contains(
                "El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)"));
    }

    @Test
    void validateDatosDelCanal_paraConEspaciosAlrededor_esValido() {
        Correo correo = correoValido();
        correo.setPara("  a@x.com  ");

        assertTrue(validar(correo).isValid());
    }

    @Test
    void validateDatosDelCanal_enCopiaConUnaDireccionInvalida_anadeMensajeEnCopia() {
        Correo correo = correoValido();
        correo.setEnCopia("b@x.com, malo");

        assertTrue(textos(validar(correo)).contains("El «en copia» debe contener direcciones de correo válidas"));
    }

    @Test
    void validateDatosDelCanal_enCopiaOcultaConUnaDireccionInvalida_anadeMensajeEnCopiaOculta() {
        Correo correo = correoValido();
        correo.setEnCopiaOculta("malo@");

        assertTrue(textos(validar(correo)).contains("El «en copia oculta» debe contener direcciones de correo válidas"));
    }

    @Test
    void validateDatosDelCanal_copiasEnBlanco_noSeValidanNiSeBuscanRepetidas() {
        Correo correo = correoValido();
        correo.setEnCopia("  ");
        correo.setEnCopiaOculta(null);

        assertTrue(validar(correo).isValid());
    }

    @Test
    void validateDatosDelCanal_direccionDelParaRepetidaEnCopiaConOtraCapitalizacion_anadeMensajeRepetida() {
        Correo correo = correoValido();
        correo.setPara("a@x.com");
        correo.setEnCopia(" A@X.com ");

        assertTrue(textos(validar(correo)).contains(MENSAJE_DIRECCION_REPETIDA));
    }

    @Test
    void validateDatosDelCanal_direccionRepetidaDentroDeEnCopia_anadeMensajeRepetida() {
        Correo correo = correoValido();
        correo.setEnCopia("b@x.com,b@x.com");

        assertTrue(textos(validar(correo)).contains(MENSAJE_DIRECCION_REPETIDA));
    }

    @Test
    void validateDatosDelCanal_direccionRepetidaEntreEnCopiaYEnCopiaOculta_anadeMensajeRepetida() {
        Correo correo = correoValido();
        correo.setEnCopia("b@x.com");
        correo.setEnCopiaOculta("b@x.com");

        assertTrue(textos(validar(correo)).contains(MENSAJE_DIRECCION_REPETIDA));
    }

    @Test
    void validateDatosDelCanal_asuntoSoloEspacios_anadeMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setAsunto("   ");

        List<String> mensajes = textos(validar(correo));

        assertEquals(List.of("El asunto es obligatorio"), mensajes.stream().filter(m -> m.startsWith("El asunto")).toList());
    }

    @Test
    void validateDatosDelCanal_asuntoDe256Caracteres_anadeMensajeLongitud() {
        Correo correo = correoValido();
        correo.setAsunto("a".repeat(256));

        assertTrue(textos(validar(correo)).contains("El asunto no puede superar 255 caracteres"));
    }

    @Test
    void validateDatosDelCanal_asuntoDe255Caracteres_esValido() {
        Correo correo = correoValido();
        correo.setAsunto("a".repeat(255));

        assertTrue(validar(correo).isValid());
    }

    @Test
    void validateDatosDelCanal_asuntoConSaltoDeLinea_anadeMensajeCaracteresDeControl() {
        Correo correo = correoValido();
        correo.setAsunto("Línea 1\nLínea 2");

        assertTrue(textos(validar(correo)).contains(MENSAJE_ASUNTO_CARACTERES_DE_CONTROL));
    }

    @Test
    void validateDatosDelCanal_asuntoConTabulador_anadeMensajeCaracteresDeControl() {
        Correo correo = correoValido();
        correo.setAsunto("Asunto\tcon tab");

        assertTrue(textos(validar(correo)).contains(MENSAJE_ASUNTO_CARACTERES_DE_CONTROL));
    }

    @Test
    void validateDatosDelCanal_cuerpoSoloEspacios_anadeMensajeObligatorio() {
        Correo correo = correoValido();
        correo.setCuerpo("  \n ");

        assertTrue(textos(validar(correo)).contains("El cuerpo es obligatorio"));
    }

    @Test
    void validateDatosDelCanal_adjuntosQueSuperan25MB_anadeMensajeTamanoTotal() {
        MetaFile contenido1 = metaFile(11L);
        MetaFile contenido2 = metaFile(12L);
        Correo correo = correoValido();
        correo.setAdjuntos(new ArrayList<>(List.of(adjunto("uno.pdf", contenido1), adjunto("dos.pdf", contenido2))));

        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtilMock.when(() -> MetaFileUtil.getSize(contenido1)).thenReturn(15 * MB);
            metaFileUtilMock.when(() -> MetaFileUtil.getSize(contenido2)).thenReturn(15 * MB);

            assertTrue(textos(validar(correo)).contains(MENSAJE_TAMANO_TOTAL_ADJUNTOS));
        }
    }

    @Test
    void validateDatosDelCanal_adjuntosDeExactamente25MB_esValido() {
        MetaFile contenido1 = metaFile(11L);
        MetaFile contenido2 = metaFile(12L);
        Correo correo = correoValido();
        correo.setAdjuntos(new ArrayList<>(List.of(adjunto("uno.pdf", contenido1), adjunto("dos.pdf", contenido2))));

        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtilMock.when(() -> MetaFileUtil.getSize(contenido1)).thenReturn(10 * MB);
            metaFileUtilMock.when(() -> MetaFileUtil.getSize(contenido2)).thenReturn(15 * MB);

            assertTrue(validar(correo).isValid());
        }
    }

    @Test
    void validateDatosDelCanal_seMideEnDiscoNoConElFileSizeDelCliente() {
        MetaFile contenido = metaFile(11L);
        contenido.setFileSize(1L);
        Correo correo = correoValido();
        correo.setAdjuntos(new ArrayList<>(List.of(adjunto("uno.pdf", contenido))));

        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtilMock.when(() -> MetaFileUtil.getSize(contenido)).thenReturn(26 * MB);

            assertTrue(textos(validar(correo)).contains(MENSAJE_TAMANO_TOTAL_ADJUNTOS));
        }
    }

    @Test
    void validateDatosDelCanal_adjuntoSinContenido_noSeSumaNiFalla() {
        MetaFile contenido = metaFile(11L);
        Correo correo = correoValido();
        correo.setAdjuntos(new ArrayList<>(List.of(adjunto("vacio.pdf", null), adjunto("uno.pdf", contenido))));

        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtilMock.when(() -> MetaFileUtil.getSize(contenido)).thenReturn(MB);

            assertTrue(validar(correo).isValid());
            metaFileUtilMock.verify(() -> MetaFileUtil.getSize(any()), times(1));
        }
    }

    @Test
    void validateDatosDelCanal_sinAdjuntos_noMideNada() {
        Correo sinListaDeAdjuntos = correoValido();
        sinListaDeAdjuntos.setAdjuntos(null);
        Correo conListaVacia = correoValido();
        conListaVacia.setAdjuntos(new ArrayList<>());

        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            assertTrue(validar(sinListaDeAdjuntos).isValid());
            assertTrue(validar(conListaVacia).isValid());
            metaFileUtilMock.verify(() -> MetaFileUtil.getSize(any()), never());
        }
    }

    /*************************************** antesDeGuardar ***************************************/

    @Test
    void antesDeGuardar_sustituyeCadaContenidoPorUnaCopiaConElNombreDelAdjunto() {
        MetaFile original1 = metaFile(11L);
        MetaFile original2 = metaFile(12L);
        MetaFile copia1 = metaFile(21L);
        MetaFile copia2 = metaFile(22L);
        Adjunto adjunto1 = adjunto("  informe.pdf ", original1);
        Adjunto adjunto2 = adjunto("foto.png", original2);
        Correo correo = correoValido();
        correo.setAdjuntos(new ArrayList<>(List.of(adjunto1, adjunto2)));

        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtilMock.when(() -> MetaFileUtil.cloneMetaFile(original1)).thenReturn(copia1);
            metaFileUtilMock.when(() -> MetaFileUtil.cloneMetaFile(original2)).thenReturn(copia2);

            service.antesDeGuardar(correo);

            metaFileUtilMock.verify(() -> MetaFileUtil.cloneMetaFile(original1), times(1));
            metaFileUtilMock.verify(() -> MetaFileUtil.cloneMetaFile(original2), times(1));
        }

        assertSame(copia1, adjunto1.getContenido());
        assertSame(copia2, adjunto2.getContenido());
        assertEquals("informe.pdf", copia1.getFileName());
        assertEquals("foto.png", copia2.getFileName());
    }

    @Test
    void antesDeGuardar_sinAdjuntos_noClonaNada() {
        Correo sinListaDeAdjuntos = correoValido();
        sinListaDeAdjuntos.setAdjuntos(null);
        Correo conListaVacia = correoValido();
        conListaVacia.setAdjuntos(new ArrayList<>());

        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            assertDoesNotThrow(() -> service.antesDeGuardar(sinListaDeAdjuntos));
            assertDoesNotThrow(() -> service.antesDeGuardar(conListaVacia));
            metaFileUtilMock.verify(() -> MetaFileUtil.cloneMetaFile(any()), never());
        }
    }

    /*************************************** enviar ***************************************/

    @Test
    void enviar_correoCompleto_construyeElMailConDireccionesRecortadasYTextoPlano() {
        Correo correo = correoValido();
        correo.setPara(" a@x.com ");
        correo.setEnCopia("b@x.com , c@x.com");
        correo.setEnCopiaOculta(" d@x.com");

        Mail mail = enviarYCapturar(correo);

        assertEquals(List.of("a@x.com"), mail.to());
        assertEquals(List.of("b@x.com", "c@x.com"), mail.cc());
        assertEquals(List.of("d@x.com"), mail.bcc());
        assertEquals("noreply@educaflow.test", mail.from());
        assertEquals(correo.getAsunto(), mail.subject());
        assertEquals(correo.getCuerpo(), mail.textBody());
        assertNull(mail.htmlBody());
        assertTrue(mail.attachs().isEmpty());
    }

    @Test
    void enviar_copiasVaciasOConEntradasVacias_seOmiten() {
        Correo correo = correoValido();
        correo.setEnCopia(null);
        correo.setEnCopiaOculta(" , ");

        Mail mail = enviarYCapturar(correo);

        assertTrue(mail.cc().isEmpty());
        assertTrue(mail.bcc().isEmpty());
    }

    @Test
    void enviar_correoConAdjuntos_losAdjuntaConElNombreDelAdjunto() {
        MetaFile contenido = metaFile(11L);
        contenido.setFileType("application/pdf");
        byte[] bytes = {1, 2, 3};
        Correo correo = correoValido();
        correo.setAdjuntos(new ArrayList<>(List.of(adjunto("informe.pdf", contenido))));

        Mail mail;
        try (MockedStatic<MetaFileUtil> metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtilMock.when(() -> MetaFileUtil.downloadContent(contenido)).thenReturn(bytes);

            mail = enviarYCapturar(correo);
        }

        assertEquals(1, mail.attachs().size());
        Fichero fichero = mail.attachs().get(0);
        assertEquals("informe.pdf", fichero.fileName());
        assertArrayEquals(new byte[]{1, 2, 3}, fichero.data());
        assertEquals("application/pdf", fichero.mimeType());
    }

    @Test
    void enviar_providerFalla_propagaLaRuntimeException() {
        Correo correo = correoValido();
        when(mailSenderProvider.get()).thenThrow(new IllegalArgumentException("sin servidor de correo"));

        assertThrows(IllegalArgumentException.class, () -> service.enviar(correo));
        verifyNoInteractions(mailSender);
    }

    /*************************************** allowPropertiesInsert ***************************************/

    @Test
    void allowPropertiesInsert_permiteLosComunesYLosDelCorreo() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        List.of("name", "dniDestinatario", "nombre", "apellidos", "centro", "historialEstado",
                        "para", "enCopia", "enCopiaOculta", "asunto", "cuerpo", "adjuntos")
                .forEach(propiedad -> assertTrue(allowProperties.allowProperty(propiedad), propiedad));
        assertFalse(allowProperties.allowProperty("estado"));
    }
}
