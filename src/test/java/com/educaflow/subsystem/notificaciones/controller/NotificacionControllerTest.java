package com.educaflow.subsystem.notificaciones.controller;

import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.db.Notificacion;
import com.educaflow.subsystem.notificaciones.db.Sms;
import com.educaflow.subsystem.notificaciones.db.TipoNotificacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    private NotificacionController controller;

    private ActionRequest actionRequest;
    private ActionResponse actionResponse;

    private Map<String, Object> context;

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() {
        controller = new NotificacionController();

        actionRequest = mock(ActionRequest.class);
        actionResponse = mock(ActionResponse.class);

        context = new HashMap<>();
        context.put("_model", Notificacion.class.getName());

        Map<String, Object> data = new HashMap<>();
        data.put("context", context);
        when(actionRequest.getData()).thenReturn(data);
        when(actionRequest.getContext()).thenReturn(null);

        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
    }

    private void filaDelListado(String tipoNotificacion, Long id) {
        context.put("tipoNotificacion", tipoNotificacion);
        context.put("id", id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> vistaAbierta() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(actionResponse).setView(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> formsDe(Map<String, Object> vista) {
        return (List<Map<String, Object>>) vista.get("views");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> paramsDe(Map<String, Object> vista) {
        return (Map<String, Object>) vista.get("params");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> contextoDe(Map<String, Object> vista) {
        return (Map<String, Object>) vista.get("context");
    }

    private static void assertFormUnico(String nombreForm, Map<String, Object> vista) {
        List<Map<String, Object>> forms = formsDe(vista);
        assertEquals(1, forms.size(), "La vista debe abrir un único form");
        assertEquals("form", forms.get(0).get("type"));
        assertEquals(nombreForm, forms.get(0).get("name"));
    }

    private static void assertEnPopupSinGuardar(Map<String, Object> vista) {
        Map<String, Object> params = paramsDe(vista);
        assertEquals(true, params.get("popup"));
        assertEquals(false, params.get("popup-save"));
    }

    /* ------------------------------------------------------------------ */
    /* abrirTodas                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void abrirTodas_filaCorreo_abreElFormMainDelCorreoEnPopupConSuId() {
        filaDelListado("CORREO", 7L);

        controller.abrirTodas(actionRequest, actionResponse);

        Map<String, Object> vista = vistaAbierta();
        assertEquals(Correo.class.getName(), vista.get("model"));
        assertFormUnico("subsysNotificaciones.Main@Correo-form", vista);
        assertEnPopupSinGuardar(vista);
        assertEquals(7L, contextoDe(vista).get("_showRecord"));
    }

    @Test
    void abrirTodas_filaSms_abreElFormMainDelSms() {
        filaDelListado("SMS", 8L);

        controller.abrirTodas(actionRequest, actionResponse);

        Map<String, Object> vista = vistaAbierta();
        assertEquals(Sms.class.getName(), vista.get("model"));
        assertFormUnico("subsysNotificaciones.Main@Sms-form", vista);
        assertEquals(8L, contextoDe(vista).get("_showRecord"));
    }

    @Test
    void abrirTodas_contextoSinTipo_lanzaIllegalStateException() {
        context.put("id", 7L);

        assertThrows(IllegalStateException.class, () -> controller.abrirTodas(actionRequest, actionResponse));

        verify(actionResponse, never()).setView(any());
    }

    @Test
    void abrirTodas_tipoDesconocido_lanzaIllegalArgumentException() {
        filaDelListado("FAX", 7L);

        assertThrows(IllegalArgumentException.class, () -> controller.abrirTodas(actionRequest, actionResponse));

        verify(actionResponse, never()).setView(any());
    }

    /* ------------------------------------------------------------------ */
    /* abrirDelCentro                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void abrirDelCentro_filaCorreo_abreElFormCentroDelCorreo() {
        filaDelListado("CORREO", 7L);

        controller.abrirDelCentro(actionRequest, actionResponse);

        Map<String, Object> vista = vistaAbierta();
        assertFormUnico("subsysNotificaciones.Centro@Correo-form", vista);
        assertEquals(Correo.class.getName(), vista.get("model"));
        assertEquals(7L, contextoDe(vista).get("_showRecord"));
        assertEquals(true, paramsDe(vista).get("popup"));
    }

    @Test
    void abrirDelCentro_filaSms_abreElFormCentroDelSms() {
        filaDelListado("SMS", 8L);

        controller.abrirDelCentro(actionRequest, actionResponse);

        assertFormUnico("subsysNotificaciones.Centro@Sms-form", vistaAbierta());
    }

    /* ------------------------------------------------------------------ */
    /* abrirRecibida                                                      */
    /* ------------------------------------------------------------------ */

    @Test
    void abrirRecibida_filaCorreo_abreElFormMisDelCorreo() {
        filaDelListado("CORREO", 7L);

        controller.abrirRecibida(actionRequest, actionResponse);

        Map<String, Object> vista = vistaAbierta();
        assertFormUnico("subsysNotificaciones.Mis@Correo-form", vista);
        assertEquals(7L, contextoDe(vista).get("_showRecord"));
    }

    @Test
    void abrirRecibida_filaSms_abreElFormMisDelSms() {
        filaDelListado("SMS", 8L);

        controller.abrirRecibida(actionRequest, actionResponse);

        assertFormUnico("subsysNotificaciones.Mis@Sms-form", vistaAbierta());
    }

    /* ------------------------------------------------------------------ */
    /* continuarAlta                                                      */
    /* ------------------------------------------------------------------ */

    @Test
    void continuarAlta_eligeCorreo_abreElFormMainDelCorreoEnAlta() {
        context.put("tipoNotificacion", "CORREO");

        controller.continuarAlta(actionRequest, actionResponse);

        Map<String, Object> vista = vistaAbierta();
        assertEquals(Correo.class.getName(), vista.get("model"));
        assertFormUnico("subsysNotificaciones.Main@Correo-form", vista);
        assertEnPopupSinGuardar(vista);
        assertFalse(contextoDe(vista).containsKey("_showRecord"), "En el alta no hay fila que mostrar");
        assertEquals("Notificación", vista.get("title"));
    }

    @Test
    void continuarAlta_eligeSms_abreElFormMainDelSmsEnAlta() {
        context.put("tipoNotificacion", "SMS");

        controller.continuarAlta(actionRequest, actionResponse);

        Map<String, Object> vista = vistaAbierta();
        assertFormUnico("subsysNotificaciones.Main@Sms-form", vista);
        assertFalse(contextoDe(vista).containsKey("_showRecord"), "En el alta no hay fila que mostrar");
    }

    @Test
    void continuarAlta_sinCanalElegido_lanzaIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> controller.continuarAlta(actionRequest, actionResponse));

        verify(actionResponse, never()).setView(any());
    }

    /* ------------------------------------------------------------------ */
    /* prepararAlta                                                       */
    /* ------------------------------------------------------------------ */

    @Test
    void prepararAlta_formDelCorreo_muestraTipoCorreo() {
        context.put("_model", Correo.class.getName());

        controller.prepararAlta(actionRequest, actionResponse);

        verify(actionResponse).setValue("tipoNotificacion", TipoNotificacion.CORREO);
    }

    @Test
    void prepararAlta_formDelSms_muestraTipoSms() {
        context.put("_model", Sms.class.getName());

        controller.prepararAlta(actionRequest, actionResponse);

        verify(actionResponse).setValue("tipoNotificacion", TipoNotificacion.SMS);
    }

    @Test
    void prepararAlta_modeloQueNoEsUnCanal_lanzaIllegalStateException() {
        context.put("_model", Notificacion.class.getName());

        assertThrows(IllegalStateException.class, () -> controller.prepararAlta(actionRequest, actionResponse));

        verify(actionResponse, never()).setValue(anyString(), any());
    }
}
