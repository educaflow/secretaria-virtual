package com.educaflow.subsystem.sms.controller;

import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.sms.db.EstadoSms;
import com.educaflow.subsystem.sms.db.Sms;
import com.educaflow.subsystem.sms.service.SmsService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsControllerTest {

    private static final Long ID_SMS = 100L;
    private static final String TELEFONO_EN_BASE_DE_DATOS = "+34600111222";
    private static final String MENSAJE_SOLO_FALLIDOS = "Solo se pueden reenviar SMS que han fallado";
    private static final String AVISO_REENVIO = "El reenvío del SMS se ha puesto en marcha.";

    private SmsController controller;

    private ModelServiceFactory modelServiceFactory;
    private SmsService smsService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;
    private JpaRepository<Sms> smsJpaRepository;

    private MockedStatic<JpaRepository> jpaRepositoryMock;
    private MockedStatic<I18n> i18nMock;

    private Map<String, Object> context;
    private Sms smsEnBaseDeDatos;

    @BeforeEach
    void setUp() throws Exception {
        controller = new SmsController();

        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        setField(controller, "modelServiceFactory", modelServiceFactory);

        smsService = Mockito.mock(SmsService.class);
        actionRequest = Mockito.mock(ActionRequest.class);
        actionResponse = Mockito.mock(ActionResponse.class);
        smsJpaRepository = Mockito.mock(JpaRepository.class);

        Centro centro = new Centro();
        centro.setId(1L);

        smsEnBaseDeDatos = new Sms();
        smsEnBaseDeDatos.setId(ID_SMS);
        smsEnBaseDeDatos.setEstado(EstadoSms.FALLIDO);
        smsEnBaseDeDatos.setTelefono(TELEFONO_EN_BASE_DE_DATOS);
        smsEnBaseDeDatos.setCentro(centro);

        context = new HashMap<>();
        context.put("_model", Sms.class.getName());
        context.put("id", ID_SMS);

        Map<String, Object> data = new HashMap<>();
        data.put("context", context);
        Mockito.lenient().when(actionRequest.getData()).thenReturn(data);

        jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class);
        jpaRepositoryMock.when(() -> JpaRepository.of(Sms.class)).thenReturn(smsJpaRepository);
        Mockito.lenient().when(smsJpaRepository.find(ID_SMS)).thenReturn(smsEnBaseDeDatos);

        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.lenient().when(modelServiceFactory.resolve(Sms.class)).thenReturn(smsService);
        Mockito.lenient().when(smsService.allowPropertiesReenviar())
                .thenReturn(AllowProperties.createDenyAllProperties());
    }

    @AfterEach
    void tearDown() {
        if (jpaRepositoryMock != null) {
            jpaRepositoryMock.close();
        }
        if (i18nMock != null) {
            i18nMock.close();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SmsController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /* ------------------------------------------------------------------ */
    /* validateReenviar                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void validateReenviar_servicioSinMensajes_noDevuelveNingunError() {
        when(smsService.validateReenviar(any(), any())).thenReturn(Optional.empty());

        controller.validateReenviar(actionRequest, actionResponse);

        verify(actionResponse, never()).setError(anyString());
        Mockito.verifyNoInteractions(actionResponse);
    }

    @Test
    void validateReenviar_servicioConMensajes_devuelveElMensajeComoError() {
        when(smsService.validateReenviar(any(), any()))
                .thenReturn(Optional.of(BusinessMessages.single(MENSAJE_SOLO_FALLIDOS)));

        controller.validateReenviar(actionRequest, actionResponse);

        ArgumentCaptor<String> captorError = ArgumentCaptor.forClass(String.class);
        verify(actionResponse).setError(captorError.capture());
        assertTrue(captorError.getValue().contains(MENSAJE_SOLO_FALLIDOS),
                "El error entregado al cliente debe contener el mensaje del servicio: " + captorError.getValue());
    }

    @Test
    void validateReenviar_contextoSinId_pasaEntidadOriginalNulaAlServicio() {
        context.remove("id");
        when(smsJpaRepository.create(null)).thenReturn(new Sms());
        when(smsService.validateReenviar(any(), isNull()))
                .thenReturn(Optional.of(BusinessMessages.single(MENSAJE_SOLO_FALLIDOS)));

        assertDoesNotThrow(() -> controller.validateReenviar(actionRequest, actionResponse));

        verify(smsService).validateReenviar(any(), isNull());
    }

    @Test
    void validateReenviar_pideLaWhitelistAlServicio() {
        when(smsService.validateReenviar(any(), any())).thenReturn(Optional.empty());

        controller.validateReenviar(actionRequest, actionResponse);

        verify(smsService).allowPropertiesReenviar();
    }

    /* ------------------------------------------------------------------ */
    /* reenviar                                                           */
    /* ------------------------------------------------------------------ */

    @Test
    void reenviar_smsFallido_delegaEnElServicioYAvisaAlUsuario() {
        when(smsService.reenviar(any(), any())).thenReturn(smsEnBaseDeDatos);

        controller.reenviar(actionRequest, actionResponse);

        verify(smsService).reenviar(any(), any());
        verify(actionResponse).setNotify(AVISO_REENVIO);
        verify(actionResponse).setSignal("refresh-tab", null);
    }

    @Test
    void reenviar_contextoConCamposDeServidor_noLlegaNingunoAlServicio() {
        context.put("estado", "ENVIADO");
        context.put("telefono", "+34700000000");

        controller.reenviar(actionRequest, actionResponse);

        ArgumentCaptor<Sms> captorEntidad = ArgumentCaptor.forClass(Sms.class);
        verify(smsService).reenviar(captorEntidad.capture(), any());
        Sms entidad = captorEntidad.getValue();
        assertEquals(EstadoSms.FALLIDO, entidad.getEstado(),
                "El estado lo dicta el servidor: el valor enviado por el cliente no puede entrar");
        assertEquals(TELEFONO_EN_BASE_DE_DATOS, entidad.getTelefono(),
                "El teléfono lo dicta el servidor: el valor enviado por el cliente no puede entrar");
    }

    @Test
    void reenviar_servicioLanzaValidationException_noAvisaAlUsuario() {
        when(smsService.reenviar(any(), any())).thenThrow(new ValidationException(MENSAJE_SOLO_FALLIDOS));

        assertThrows(ValidationException.class, () -> controller.reenviar(actionRequest, actionResponse));

        verify(actionResponse, never()).setNotify(anyString());
        verify(actionResponse, never()).setSignal(anyString(), any());
    }

    @Test
    void reenviar_pideLaWhitelistAlServicio() {
        when(smsService.reenviar(any(), any())).thenReturn(smsEnBaseDeDatos);

        controller.reenviar(actionRequest, actionResponse);

        verify(smsService).allowPropertiesReenviar();
    }
}
