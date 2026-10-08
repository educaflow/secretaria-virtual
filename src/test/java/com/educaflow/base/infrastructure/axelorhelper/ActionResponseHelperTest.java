package com.educaflow.base.infrastructure.axelorhelper;

import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.rpc.ActionResponse;
import com.educaflow.subsystem.notificaciones.db.Correo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActionResponseHelperTest {

    @Test
    void doResponseBusinessMessagesAsError_labelYMensajeConHtml_losEscapa() {
        BusinessMessages messages = new BusinessMessages();
        messages.add(new BusinessMessage("campo", "<b>mensaje</b>", "<b>x</b>.pdf"));

        String html = errorEnviado(messages);

        assertTrue(html.contains("&lt;b&gt;x&lt;/b&gt;.pdf"));
        assertTrue(html.contains("&lt;b&gt;mensaje&lt;/b&gt;"));
        assertFalse(html.contains("<b>"));
    }

    @Test
    void doResponseBusinessMessagesAsError_campoYMensajeConHtml_losEscapa() {
        BusinessMessages messages = new BusinessMessages();
        messages.add(new BusinessMessage("<b>campo</b>", "<a href=\"x\">mensaje</a>"));

        String html = errorEnviado(messages);

        assertTrue(html.contains("&lt;b&gt;campo&lt;/b&gt;"));
        assertFalse(html.contains("<a href"));
        assertTrue(html.startsWith("<ul><li><strong>"));
    }

    @Test
    void doResponseViewFormEnPopup_conId_abreElFormEnPopupMostrandoElRegistro() {
        ActionResponse response = mock(ActionResponse.class);

        new ActionResponseHelper(response).doResponseViewFormEnPopup("subsysNotificaciones.Main@Correo-form", Correo.class, 7L, "Notificación");

        Map<String, Object> vista = vistaEnviada(response);
        assertEquals("Notificación", vista.get("title"));
        assertEquals(Correo.class.getName(), vista.get("model"));
        assertEquals(List.of(Map.of("type", "form", "name", "subsysNotificaciones.Main@Correo-form")), vista.get("views"));
        Map<String, Object> params = params(vista);
        assertParamsDePopup(params);
        assertFalse(params.containsKey("forceTitle"));
        Map<String, Object> context = context(vista);
        assertEquals(7L, context.get("_showRecord"));
        assertFalse(context.containsKey("_profile"));
        assertFalse(context.containsKey("newEntity"));
    }

    @Test
    void doResponseViewFormEnPopup_sinId_abreElFormEnAlta() {
        ActionResponse response = mock(ActionResponse.class);

        new ActionResponseHelper(response).doResponseViewFormEnPopup("subsysNotificaciones.Main@Correo-form", Correo.class, null, "Notificación");

        Map<String, Object> vista = vistaEnviada(response);
        assertParamsDePopup(params(vista));
        assertFalse(context(vista).containsKey("_showRecord"));
    }

    @Test
    void doResponseViewForm_entidadConId_conservaElComportamientoAnterior() {
        ActionResponse response = mock(ActionResponse.class);
        Correo correo = new Correo();
        correo.setId(5L);

        new ActionResponseHelper(response).doResponseViewForm("subsysNotificaciones.Main@Correo-form", Correo.class, correo, "Notificación", "perfil");

        Map<String, Object> vista = vistaEnviada(response);
        Map<String, Object> params = params(vista);
        assertEquals(true, params.get("forceEdit"));
        assertEquals(true, params.get("forceTitle"));
        assertEquals(false, params.get("show-confirm"));
        assertEquals(false, params.get("show-toolbar"));
        assertFalse(params.containsKey("popup"));
        assertFalse(params.containsKey("popup-save"));
        Map<String, Object> context = context(vista);
        assertEquals("perfil", context.get("_profile"));
        assertEquals(5L, context.get("_showRecord"));
    }

    @Test
    void doResponseViewForm_entidadSinId_pasaLaEntidadNueva() {
        ActionResponse response = mock(ActionResponse.class);
        Correo correo = new Correo();

        new ActionResponseHelper(response).doResponseViewForm("subsysNotificaciones.Main@Correo-form", Correo.class, correo, "Notificación", "perfil");

        Map<String, Object> context = context(vistaEnviada(response));
        assertSame(correo, context.get("newEntity"));
        assertFalse(context.containsKey("_showRecord"));
    }

    private static void assertParamsDePopup(Map<String, Object> params) {
        assertEquals(true, params.get("popup"));
        assertEquals(false, params.get("popup-save"));
        assertEquals(true, params.get("forceEdit"));
        assertEquals(false, params.get("show-confirm"));
        assertEquals(false, params.get("show-toolbar"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> vistaEnviada(ActionResponse response) {
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(response).setView(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> params(Map<String, Object> vista) {
        return (Map<String, Object>) vista.get("params");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> context(Map<String, Object> vista) {
        return (Map<String, Object>) vista.get("context");
    }

    private static String errorEnviado(BusinessMessages messages) {
        ActionResponse response = mock(ActionResponse.class);
        new ActionResponseHelper(response).doResponseBusinessMessagesAsError(messages);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setError(captor.capture());
        return captor.getValue();
    }
}
