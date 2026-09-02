package com.educaflow.subsystem.firmas.controller;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.subsystem.firmas.db.EstadoTareaFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TareaFirmaControllerTest {

    private static final Long ID_TAREA = 1L;
    private static final String CLAVE = "nadanada";
    private static final String MENSAJE_CONTRASENA_OBLIGATORIA = "La contraseña es obligatoria";
    private static final String MENSAJE_NO_SE_HAN_PODIDO_FIRMAR =
            "No se han podido firmar los documentos: clave incorrecta";

    private TareaFirmaController controller;

    private ModelServiceFactory modelServiceFactory;
    private TareaFirmaService tareaFirmaService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;
    private JpaRepository<TareaFirma> tareaFirmaJpaRepository;

    private MockedStatic<JpaRepository> jpaRepositoryMock;

    /** Mapa `context` del ActionRequest; cada test puede añadirle claves antes de actuar. */
    private Map<String, Object> context;

    /** La tarea tal y como está en base de datos: es lo que devuelve `find(1L)`. */
    private TareaFirma tareaFirmaEnBaseDeDatos;

    /** Firmante que la tarea tiene en base de datos, para comprobar que el cliente no lo pisa. */
    private User firmanteEnBaseDeDatos;

    @BeforeEach
    void setUp() throws Exception {
        controller = new TareaFirmaController();

        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        setField(controller, "modelServiceFactory", modelServiceFactory);

        tareaFirmaService = Mockito.mock(TareaFirmaService.class);
        actionRequest = Mockito.mock(ActionRequest.class);
        actionResponse = Mockito.mock(ActionResponse.class);
        tareaFirmaJpaRepository = Mockito.mock(JpaRepository.class);

        // El firmante no lleva DNI a propósito: así el getter del campo derivado `situacionFirma`
        // (que Axelor invoca al clonar la entidad para obtener el original) resuelve a SIN_DNI sin
        // tocar el subsistema de criptografía, que en un test unitario no está cableado.
        firmanteEnBaseDeDatos = new User();
        firmanteEnBaseDeDatos.setId(7L);
        firmanteEnBaseDeDatos.setCode("firmante");

        tareaFirmaEnBaseDeDatos = new TareaFirma();
        tareaFirmaEnBaseDeDatos.setId(ID_TAREA);
        tareaFirmaEnBaseDeDatos.setEstadoTareaFirma(EstadoTareaFirma.PENDIENTE);
        tareaFirmaEnBaseDeDatos.setFirmante(firmanteEnBaseDeDatos);

        context = new HashMap<>();
        context.put("_model", TareaFirma.class.getName());
        context.put("id", ID_TAREA);
        context.put("claveFirma", CLAVE);

        Map<String, Object> data = new HashMap<>();
        data.put("context", context);
        when(actionRequest.getData()).thenReturn(data);

        jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class);
        jpaRepositoryMock.when(() -> JpaRepository.of(TareaFirma.class)).thenReturn(tareaFirmaJpaRepository);
        when(tareaFirmaJpaRepository.find(ID_TAREA)).thenReturn(tareaFirmaEnBaseDeDatos);

        when(modelServiceFactory.resolve(TareaFirma.class)).thenReturn(tareaFirmaService);
        when(tareaFirmaService.allowPropertiesFirmarEnServidor())
                .thenReturn(AllowProperties.createAllowProperties(Map.of("claveFirma", Map.of())));
    }

    @AfterEach
    void tearDown() {
        if (jpaRepositoryMock != null) {
            jpaRepositoryMock.close();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = TareaFirmaController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private ArgumentCaptor<TareaFirma> capturarLlamadaAFirmarEnServidor() {
        ArgumentCaptor<TareaFirma> captorEntidad = ArgumentCaptor.forClass(TareaFirma.class);
        ArgumentCaptor<TareaFirma> captorOriginal = ArgumentCaptor.forClass(TareaFirma.class);
        verify(tareaFirmaService, times(1)).firmarEnServidor(captorEntidad.capture(), captorOriginal.capture());
        return captorEntidad;
    }

    /* ------------------------------------------------------------------ */
    /* validateFirmarEnServidor                                           */
    /* ------------------------------------------------------------------ */

    @Test
    void validateFirmarEnServidor_servicioSinMensajes_noDevuelveNingunError() {
        when(tareaFirmaService.validateFirmarEnServidor(any(), any())).thenReturn(Optional.empty());

        controller.validateFirmarEnServidor(actionRequest, actionResponse);

        verify(actionResponse, never()).setError(anyString());
        verify(actionResponse, never()).setError(anyString(), anyString());
    }

    @Test
    void validateFirmarEnServidor_servicioConMensajes_entregaLosMensajesComoError() {
        when(tareaFirmaService.validateFirmarEnServidor(any(), any()))
                .thenReturn(Optional.of(BusinessMessages.single(MENSAJE_CONTRASENA_OBLIGATORIA)));

        controller.validateFirmarEnServidor(actionRequest, actionResponse);

        ArgumentCaptor<String> captorError = ArgumentCaptor.forClass(String.class);
        verify(actionResponse).setError(captorError.capture());
        assertTrue(captorError.getValue().contains(MENSAJE_CONTRASENA_OBLIGATORIA),
                "El error entregado al cliente debe contener el mensaje del validador: " + captorError.getValue());
    }

    @Test
    void validateFirmarEnServidor_siempre_usaLaWhitelistDeLaAccion() {
        when(tareaFirmaService.validateFirmarEnServidor(any(), any())).thenReturn(Optional.empty());

        controller.validateFirmarEnServidor(actionRequest, actionResponse);

        verify(tareaFirmaService).allowPropertiesFirmarEnServidor();
        verify(tareaFirmaService, never()).allowPropertiesValidarDocumentosFirmados();
        verify(tareaFirmaService, never()).allowPropertiesMarcarComoFirmada();
        verify(tareaFirmaService, never()).allowPropertiesMarcarComoRechazada();
    }

    @Test
    void validateFirmarEnServidor_siempre_noEjecutaLaFirma() {
        when(tareaFirmaService.validateFirmarEnServidor(any(), any())).thenReturn(Optional.empty());

        controller.validateFirmarEnServidor(actionRequest, actionResponse);

        verify(tareaFirmaService, never()).firmarEnServidor(any(), any());
    }

    /* ------------------------------------------------------------------ */
    /* firmarEnServidor                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void firmarEnServidor_peticionValida_delegaEnElServicioConLaEntidadYElOriginal() {
        when(tareaFirmaService.firmarEnServidor(any(), any())).thenReturn(tareaFirmaEnBaseDeDatos);

        controller.firmarEnServidor(actionRequest, actionResponse);

        ArgumentCaptor<TareaFirma> captorEntidad = ArgumentCaptor.forClass(TareaFirma.class);
        ArgumentCaptor<TareaFirma> captorOriginal = ArgumentCaptor.forClass(TareaFirma.class);
        verify(tareaFirmaService, times(1)).firmarEnServidor(captorEntidad.capture(), captorOriginal.capture());

        assertEquals(CLAVE, captorEntidad.getValue().getClaveFirma(),
                "La clave tecleada tiene que llegar al servicio: es el único campo de la whitelist");

        TareaFirma original = captorOriginal.getValue();
        assertNotNull(original, "El original se obtiene de base de datos y no puede ser nulo");
        assertNotSame(captorEntidad.getValue(), original, "El original es una copia, no la propia entidad");
        assertEquals(ID_TAREA, original.getId());
    }

    @Test
    void firmarEnServidor_peticionValida_usaLaWhitelistAllowPropertiesFirmarEnServidor() {
        context.put("estadoTareaFirma", "FIRMADO");
        context.put("firmante", Map.of("id", 99L));
        when(tareaFirmaService.firmarEnServidor(any(), any())).thenReturn(tareaFirmaEnBaseDeDatos);

        controller.firmarEnServidor(actionRequest, actionResponse);

        verify(tareaFirmaService).allowPropertiesFirmarEnServidor();

        TareaFirma entidad = capturarLlamadaAFirmarEnServidor().getValue();
        assertEquals(EstadoTareaFirma.PENDIENTE, entidad.getEstadoTareaFirma(),
                "El estado lo dicta el servidor: el valor enviado por el cliente no puede entrar");
        assertSame(firmanteEnBaseDeDatos, entidad.getFirmante(),
                "El firmante lo dicta el servidor: el valor enviado por el cliente no puede entrar");
    }

    @Test
    void firmarEnServidor_elServicioLanzaValidationException_laPropagaSinCapturarla() {
        when(tareaFirmaService.firmarEnServidor(any(), any()))
                .thenThrow(new ValidationException(MENSAJE_NO_SE_HAN_PODIDO_FIRMAR));

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> controller.firmarEnServidor(actionRequest, actionResponse));

        assertEquals(MENSAJE_NO_SE_HAN_PODIDO_FIRMAR, excepcion.getMessage());
        verify(actionResponse, never()).setError(anyString());
    }

    @Test
    void firmarEnServidor_peticionValida_noMontaNingunaRespuestaEnElActionResponse() {
        when(tareaFirmaService.firmarEnServidor(any(), any())).thenReturn(tareaFirmaEnBaseDeDatos);

        controller.firmarEnServidor(actionRequest, actionResponse);

        verifyNoInteractions(actionResponse);
    }
}
