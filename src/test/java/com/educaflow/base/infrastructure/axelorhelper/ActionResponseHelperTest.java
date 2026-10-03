package com.educaflow.base.infrastructure.axelorhelper;

import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.rpc.ActionResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static String errorEnviado(BusinessMessages messages) {
        ActionResponse response = mock(ActionResponse.class);
        new ActionResponseHelper(response).doResponseBusinessMessagesAsError(messages);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setError(captor.capture());
        return captor.getValue();
    }
}
