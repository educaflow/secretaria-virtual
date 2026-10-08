package com.educaflow.subsystem.notificaciones.controller;

import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.db.EstadoNotificacion;
import com.educaflow.subsystem.notificaciones.db.Notificacion;
import com.educaflow.subsystem.notificaciones.db.Sms;
import com.educaflow.subsystem.notificaciones.service.NotificacionService;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code reenviar} y {@code validateReenviar} de {@link NotificacionController}: el mismo código sirve a todos
 * los canales, así que cada caso se ejecuta con cada clase de notificación.
 */
class NotificacionControllerReenviarTest {

    private static final Long ID_NOTIFICACION = 100L;
    private static final String MENSAJE_SOLO_FALLIDOS = "Solo se pueden reenviar notificaciones que han fallado";
    private static final String AVISO_REENVIO = "El reenvío de la notificación se ha puesto en marcha.";

    private NotificacionController controller;

    private ModelServiceFactory modelServiceFactory;
    private NotificacionService<Notificacion> canalService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;
    private JpaRepository<Notificacion> jpaRepository;

    private MockedStatic<JpaRepository> jpaRepositoryMock;
    private MockedStatic<I18n> i18nMock;

    private Map<String, Object> context;
    private Centro centroA;
    private Notificacion notificacionEnBaseDeDatos;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        controller = new NotificacionController();

        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        setField(controller, "modelServiceFactory", modelServiceFactory);

        canalService = Mockito.mock(NotificacionService.class);
        actionRequest = Mockito.mock(ActionRequest.class);
        actionResponse = Mockito.mock(ActionResponse.class);
        jpaRepository = Mockito.mock(JpaRepository.class);

        centroA = new Centro();
        centroA.setId(1L);

        context = new HashMap<>();
        context.put("id", ID_NOTIFICACION);

        Map<String, Object> data = new HashMap<>();
        data.put("context", context);
        when(actionRequest.getData()).thenReturn(data);

        jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class);
        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(canalService.allowPropertiesReenviar()).thenReturn(AllowProperties.createDenyAllProperties());
    }

    @AfterEach
    void tearDown() {
        jpaRepositoryMock.close();
        i18nMock.close();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = NotificacionController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** El form abierto es el del canal {@code clase}, con una notificación fallida guardada en BD. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void formDelCanal(Class<? extends Notificacion> clase) throws Exception {
        notificacionEnBaseDeDatos = clase.getDeclaredConstructor().newInstance();
        notificacionEnBaseDeDatos.setId(ID_NOTIFICACION);
        notificacionEnBaseDeDatos.setEstado(EstadoNotificacion.FALLIDO);
        notificacionEnBaseDeDatos.setCentro(centroA);

        context.put("_model", clase.getName());
        jpaRepositoryMock.when(() -> JpaRepository.of((Class) clase)).thenReturn(jpaRepository);
        Mockito.lenient().when(jpaRepository.find(ID_NOTIFICACION)).thenReturn(notificacionEnBaseDeDatos);
        doReturn(canalService).when(modelServiceFactory).resolve(clase);
    }

    /* ------------------------------------------------------------------ */
    /* reenviar                                                           */
    /* ------------------------------------------------------------------ */

    @ParameterizedTest
    @ValueSource(classes = {Correo.class, Sms.class})
    void reenviar_notificacionFallida_delegaEnElServicioDelCanalAvisaYRecarga(Class<? extends Notificacion> clase) throws Exception {
        formDelCanal(clase);

        controller.reenviar(actionRequest, actionResponse);

        verify(modelServiceFactory).resolve(clase);
        verify(canalService).reenviar(any(), any());
        verify(actionResponse).setNotify(AVISO_REENVIO);
        verify(actionResponse).setReload(true);
        verify(actionResponse, never()).setSignal(anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(classes = {Correo.class, Sms.class})
    void reenviar_pasaElOriginalDeBdYUnaEntidadFiltradaPorLaWhitelist(Class<? extends Notificacion> clase) throws Exception {
        formDelCanal(clase);
        context.put("estado", "ENVIADO");
        context.put("centro", Map.of("id", 2L));

        controller.reenviar(actionRequest, actionResponse);

        ArgumentCaptor<Notificacion> captorEntidad = ArgumentCaptor.forClass(Notificacion.class);
        ArgumentCaptor<Notificacion> captorOriginal = ArgumentCaptor.forClass(Notificacion.class);
        verify(canalService).reenviar(captorEntidad.capture(), captorOriginal.capture());

        Notificacion original = captorOriginal.getValue();
        assertInstanceOf(clase, original);
        assertEquals(ID_NOTIFICACION, original.getId());
        assertEquals(EstadoNotificacion.FALLIDO, original.getEstado());
        assertEquals(centroA.getId(), original.getCentro().getId());

        Notificacion entidad = captorEntidad.getValue();
        assertInstanceOf(clase, entidad);
        assertEquals(EstadoNotificacion.FALLIDO, entidad.getEstado(),
                "El estado lo dicta el servidor: el valor enviado por el cliente no puede entrar");
        assertSame(centroA, entidad.getCentro(),
                "El centro lo dicta el servidor: el valor enviado por el cliente no puede entrar");
        verify(canalService).allowPropertiesReenviar();
    }

    @ParameterizedTest
    @ValueSource(classes = {Correo.class, Sms.class})
    void reenviar_servicioLanzaValidationException_noAvisaNiRecarga(Class<? extends Notificacion> clase) throws Exception {
        formDelCanal(clase);
        when(canalService.reenviar(any(), any())).thenThrow(new ValidationException(MENSAJE_SOLO_FALLIDOS));

        assertThrows(ValidationException.class, () -> controller.reenviar(actionRequest, actionResponse));

        verify(actionResponse, never()).setNotify(anyString());
        verify(actionResponse, never()).setReload(anyBoolean());
        verify(actionResponse, never()).setSignal(anyString(), any());
    }

    @Test
    void reenviar_modeloQueNoEsUnCanal_lanzaIllegalStateExceptionSinTocarNingunServicio() {
        context.put("_model", Notificacion.class.getName());

        assertThrows(IllegalStateException.class, () -> controller.reenviar(actionRequest, actionResponse));

        verify(modelServiceFactory, never()).resolve(any());
        verify(actionResponse, never()).setNotify(anyString());
    }

    /* ------------------------------------------------------------------ */
    /* validateReenviar                                                   */
    /* ------------------------------------------------------------------ */

    @ParameterizedTest
    @ValueSource(classes = {Correo.class, Sms.class})
    void validateReenviar_servicioSinMensajes_noDevuelveError(Class<? extends Notificacion> clase) throws Exception {
        formDelCanal(clase);
        when(canalService.validateReenviar(any(), any())).thenReturn(Optional.empty());

        controller.validateReenviar(actionRequest, actionResponse);

        verify(actionResponse, never()).setError(anyString());
        verify(canalService).allowPropertiesReenviar();
    }

    @ParameterizedTest
    @ValueSource(classes = {Correo.class, Sms.class})
    void validateReenviar_servicioConMensajes_losDevuelveComoError(Class<? extends Notificacion> clase) throws Exception {
        formDelCanal(clase);
        when(canalService.validateReenviar(any(), any()))
                .thenReturn(Optional.of(BusinessMessages.single(MENSAJE_SOLO_FALLIDOS)));

        controller.validateReenviar(actionRequest, actionResponse);

        ArgumentCaptor<String> captorError = ArgumentCaptor.forClass(String.class);
        verify(actionResponse).setError(captorError.capture());
        assertTrue(captorError.getValue().contains(MENSAJE_SOLO_FALLIDOS),
                "El error entregado al cliente debe contener el mensaje del servicio: " + captorError.getValue());
    }

    @ParameterizedTest
    @ValueSource(classes = {Correo.class, Sms.class})
    void validateReenviar_contextoSinId_pasaOriginalNuloAlServicio(Class<? extends Notificacion> clase) throws Exception {
        formDelCanal(clase);
        context.remove("id");
        when(jpaRepository.create(null)).thenReturn(clase.getDeclaredConstructor().newInstance());
        when(canalService.validateReenviar(any(), isNull())).thenReturn(Optional.empty());

        controller.validateReenviar(actionRequest, actionResponse);

        verify(canalService).validateReenviar(any(), isNull());
    }
}
