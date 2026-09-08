package com.educaflow.subsystem.criptografia.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.DatosTitular;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoDigitalControllerTest {

    private static final String DNI_CON_USUARIO = "29050788V";
    private static final String DNI_SIN_USUARIO = "12345678Z";
    private static final int DNI_NUMERICO = 12345678;
    private static final String DNI_NUMERICO_COMO_TEXTO = String.valueOf(DNI_NUMERICO);
    private static final String NOMBRE_TITULAR = "Secretario";
    private static final String APELLIDOS_TITULAR = "CIPFP Mislata";
    private static final String MENSAJE_DNI_NO_VALIDO = "El DNI no es válido";

    private CertificadoDigitalController controller;

    private ModelServiceFactory modelServiceFactory;
    private CertificadoDigitalService certificadoDigitalService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;

    /**
     * Mapa `context` del ActionRequest; cada test le añade las claves que necesite antes de actuar.
     *
     * <p>La clave `_model` va siempre: `ActionRequestHelper` la valida contra la clase esperada en su
     * constructor y revienta antes de llegar al servicio si falta o no coincide.
     */
    private Map<String, Object> context;

    @BeforeEach
    void setUp() throws Exception {
        controller = new CertificadoDigitalController();

        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        setField(controller, "modelServiceFactory", modelServiceFactory);

        certificadoDigitalService = Mockito.mock(CertificadoDigitalService.class);
        actionRequest = Mockito.mock(ActionRequest.class);
        actionResponse = Mockito.mock(ActionResponse.class);

        context = new HashMap<>();
        context.put("_model", CertificadoDigital.class.getName());

        Map<String, Object> data = new HashMap<>();
        data.put("context", context);
        when(actionRequest.getData()).thenReturn(data);

        when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CertificadoDigitalController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /* ------------------------------------------------------------------ */
    /* getDatosTitularByDni                                               */
    /* ------------------------------------------------------------------ */

    @Test
    void getDatosTitularByDni_dniDeUsuarioExistente_devuelveNombreApellidosYFlagAlFormulario() {
        context.put("dni", DNI_CON_USUARIO);
        when(certificadoDigitalService.validateGetDatosTitularByDni(DNI_CON_USUARIO)).thenReturn(Optional.empty());
        when(certificadoDigitalService.getDatosTitularByDni(DNI_CON_USUARIO))
                .thenReturn(new DatosTitular(NOMBRE_TITULAR, APELLIDOS_TITULAR, true));

        controller.getDatosTitularByDni(actionRequest, actionResponse);

        verify(actionResponse).setValue("nombre", NOMBRE_TITULAR);
        verify(actionResponse).setValue("apellidos", APELLIDOS_TITULAR);
        verify(actionResponse).setValue("nombreTomadoDelUsuario", true);
        verify(actionResponse, never()).setError(anyString());
    }

    @Test
    void getDatosTitularByDni_dniSinUsuario_vaciaNombreYApellidosYPoneElFlagAFalse() {
        context.put("dni", DNI_SIN_USUARIO);
        when(certificadoDigitalService.validateGetDatosTitularByDni(DNI_SIN_USUARIO)).thenReturn(Optional.empty());
        when(certificadoDigitalService.getDatosTitularByDni(DNI_SIN_USUARIO)).thenReturn(DatosTitular.sinUsuario());

        controller.getDatosTitularByDni(actionRequest, actionResponse);

        verify(actionResponse).setValue("nombre", null);
        verify(actionResponse).setValue("apellidos", null);
        verify(actionResponse).setValue("nombreTomadoDelUsuario", false);
    }

    @Test
    void getDatosTitularByDni_contextoSinDni_invocaAlServicioConNull() {
        // El contexto no lleva la clave `dni`: el campo del formulario aún está vacío.
        when(certificadoDigitalService.validateGetDatosTitularByDni(null)).thenReturn(Optional.empty());
        when(certificadoDigitalService.getDatosTitularByDni(null)).thenReturn(DatosTitular.sinUsuario());

        controller.getDatosTitularByDni(actionRequest, actionResponse);

        verify(certificadoDigitalService).getDatosTitularByDni(null);
        verify(actionResponse).setValue("nombre", null);
        verify(actionResponse).setValue("apellidos", null);
        verify(actionResponse).setValue("nombreTomadoDelUsuario", false);
    }

    @Test
    void getDatosTitularByDni_dniNoTextoEnElContexto_loConvierteATexto() {
        // El cliente puede mandar cualquier JSON: aquí el DNI llega como número, no como texto.
        context.put("dni", DNI_NUMERICO);
        when(certificadoDigitalService.validateGetDatosTitularByDni(DNI_NUMERICO_COMO_TEXTO))
                .thenReturn(Optional.empty());
        when(certificadoDigitalService.getDatosTitularByDni(DNI_NUMERICO_COMO_TEXTO))
                .thenReturn(DatosTitular.sinUsuario());

        controller.getDatosTitularByDni(actionRequest, actionResponse);

        verify(certificadoDigitalService).getDatosTitularByDni(DNI_NUMERICO_COMO_TEXTO);
    }

    @Test
    void getDatosTitularByDni_validadorDevuelveMensajes_respondeErrorYNoInvocaLaAccion() {
        context.put("dni", DNI_CON_USUARIO);
        when(certificadoDigitalService.validateGetDatosTitularByDni(DNI_CON_USUARIO))
                .thenReturn(Optional.of(BusinessMessages.single(MENSAJE_DNI_NO_VALIDO)));

        controller.getDatosTitularByDni(actionRequest, actionResponse);

        verify(actionResponse).setError(anyString());
        verify(certificadoDigitalService, never()).getDatosTitularByDni(any());
        verify(actionResponse, never()).setValue(anyString(), any());
    }

    @Test
    void getDatosTitularByDni_conIdEnElContexto_noCargaNiGuardaLaEntidad() {
        context.put("dni", DNI_CON_USUARIO);
        context.put("id", 1);
        when(certificadoDigitalService.validateGetDatosTitularByDni(DNI_CON_USUARIO)).thenReturn(Optional.empty());
        when(certificadoDigitalService.getDatosTitularByDni(DNI_CON_USUARIO))
                .thenReturn(new DatosTitular(NOMBRE_TITULAR, APELLIDOS_TITULAR, true));

        controller.getDatosTitularByDni(actionRequest, actionResponse);

        // La acción es escalar: sobre el servicio solo se registran el validador y la propia acción. Ni
        // `insert`/`update` ni la carga de la entidad gestionada por JPA, pese a venir el `id` en el contexto.
        verify(certificadoDigitalService).validateGetDatosTitularByDni(DNI_CON_USUARIO);
        verify(certificadoDigitalService).getDatosTitularByDni(DNI_CON_USUARIO);
        verifyNoMoreInteractions(certificadoDigitalService);
    }
}
