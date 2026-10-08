package com.educaflow.subsystem.notificaciones.service.impl;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.sms.SmsSender;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.notificaciones.db.Sms;
import com.educaflow.subsystem.notificaciones.db.repo.SmsRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    private static final String DNI_VALIDO = "12345678Z";

    private static final String MENSAJE_TELEFONO_OBLIGATORIO = "El teléfono es obligatorio";
    private static final String MENSAJE_MOVIL_NO_VALIDO =
            "El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)";
    private static final String MENSAJE_OBLIGATORIO = "El mensaje es obligatorio";
    private static final String MENSAJE_NO_CABE =
            "El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales";

    private SmsRepository smsRepository;
    private Provider<SmsSender> smsSenderProvider;
    private SmsSender smsSender;
    private SmsServiceImpl service;

    private Centro centroA;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        smsRepository = mock(SmsRepository.class);
        smsSenderProvider = mock(Provider.class);
        smsSender = mock(SmsSender.class);

        service = new SmsServiceImpl(Sms.class, smsRepository);
        setField(SmsServiceImpl.class, "smsSenderProvider", smsSenderProvider);
        lenient().when(smsSenderProvider.get()).thenReturn(smsSender);

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

    private BusinessMessages validar(Sms sms) {
        // Como administrador: las validaciones de centro son comunes y se prueban en NotificacionServiceImplTest.
        securityUtilMock.when(() -> SecurityUtil.isAdmin(any())).thenReturn(true);
        return service.validateInsert(sms).orElseGet(BusinessMessages::new);
    }

    private List<String> textos(BusinessMessages messages) {
        return messages.stream().map(BusinessMessage::getMessage).toList();
    }

    /*************************************** validateInsert ***************************************/

    @Test
    void validateDatosDelCanal_smsValido_noAnadeMensajes() {
        assertTrue(validar(smsValido()).isValid());
    }

    @Test
    void validateDatosDelCanal_telefonoNulo_anadeMensajeObligatorio() {
        Sms sms = smsValido();
        sms.setTelefono(null);

        List<String> mensajes = textos(validar(sms));

        assertTrue(mensajes.contains(MENSAJE_TELEFONO_OBLIGATORIO));
        assertFalse(mensajes.contains(MENSAJE_MOVIL_NO_VALIDO));
    }

    @Test
    void validateDatosDelCanal_telefonoFijo_anadeMensajeMovilNoValido() {
        Sms sms = smsValido();
        sms.setTelefono("961234567");

        assertTrue(textos(validar(sms)).contains(MENSAJE_MOVIL_NO_VALIDO));
    }

    @Test
    void validateDatosDelCanal_telefonoConPrefijoYEspacios_esValido() {
        Sms sms = smsValido();
        sms.setTelefono("+34 600 111 222");

        assertTrue(validar(sms).isValid());
    }

    @Test
    void validateDatosDelCanal_mensajeSoloEspacios_anadeMensajeObligatorio() {
        Sms sms = smsValido();
        sms.setMensaje("   ");

        List<String> mensajes = textos(validar(sms));

        assertTrue(mensajes.contains(MENSAJE_OBLIGATORIO));
        assertFalse(mensajes.contains(MENSAJE_NO_CABE));
    }

    @Test
    void validateDatosDelCanal_mensajeBasicoDe160Caracteres_esValido() {
        Sms sms = smsValido();
        sms.setMensaje("a".repeat(160));

        assertTrue(validar(sms).isValid());
    }

    @Test
    void validateDatosDelCanal_mensajeBasicoDe161Caracteres_anadeMensajeNoCabe() {
        Sms sms = smsValido();
        sms.setMensaje("a".repeat(161));

        assertTrue(textos(validar(sms)).contains(MENSAJE_NO_CABE));
    }

    @Test
    void validateDatosDelCanal_mensajeConAcentoDe70Caracteres_esValido() {
        Sms sms = smsValido();
        sms.setMensaje("á" + "a".repeat(69));

        assertTrue(validar(sms).isValid());
    }

    @Test
    void validateDatosDelCanal_mensajeConAcentoDe71Caracteres_anadeMensajeNoCabe() {
        Sms sms = smsValido();
        sms.setMensaje("á" + "a".repeat(70));

        assertTrue(textos(validar(sms)).contains(MENSAJE_NO_CABE));
    }

    /*************************************** antesDeGuardar ***************************************/

    @Test
    void antesDeGuardar_telefonoNacional_loGuardaEnE164() {
        Sms sms = smsValido();
        sms.setTelefono("600111222");

        service.antesDeGuardar(sms);

        assertEquals("+34600111222", sms.getTelefono());
    }

    @Test
    void antesDeGuardar_telefonoInternacionalConEspacios_loNormaliza() {
        Sms sms = smsValido();
        sms.setTelefono("+34 600 111 222");

        service.antesDeGuardar(sms);

        assertEquals("+34600111222", sms.getTelefono());
    }

    /*************************************** enviar ***************************************/

    @Test
    void enviar_enviaTelefonoYMensajeAlProveedor() {
        Sms sms = smsValido();
        sms.setTelefono("+34600111222");
        sms.setMensaje("Hola");

        service.enviar(sms);

        ArgumentCaptor<com.educaflow.base.infrastructure.sms.Sms> smsCaptor =
                ArgumentCaptor.forClass(com.educaflow.base.infrastructure.sms.Sms.class);
        verify(smsSender).send(smsCaptor.capture());
        assertEquals("+34600111222", smsCaptor.getValue().telefonoDestino());
        assertEquals("Hola", smsCaptor.getValue().mensaje());
    }

    @Test
    void enviar_proveedorSinConfigurar_propagaLaRuntimeException() {
        Sms sms = smsValido();
        when(smsSenderProvider.get()).thenThrow(new IllegalArgumentException("sin proveedor de SMS"));

        assertThrows(IllegalArgumentException.class, () -> service.enviar(sms));
        verifyNoInteractions(smsSender);
    }

    /*************************************** allowPropertiesInsert ***************************************/

    @Test
    void allowPropertiesInsert_permiteLosComunesYTelefonoYMensaje() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        List.of("name", "dniDestinatario", "nombre", "apellidos", "centro", "historialEstado", "telefono", "mensaje")
                .forEach(propiedad -> assertTrue(allowProperties.allowProperty(propiedad), propiedad));
        assertFalse(allowProperties.allowProperty("estado"));
    }
}
