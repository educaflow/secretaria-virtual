package com.educaflow.base.infrastructure.autofirma;

import com.axelor.db.Model;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AutoFirmaSendToActionResponseTest {

    @Test
    void sendToActionResponse_sinSourceTargetFields_lanzaError() {
        AutoFirma autoFirma = new AutoFirma(JustificacionDummy.class)
                .setRectangulo(new Rectangulo(10, 20, 30, 40));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> AutoFirma.sendToActionResponse(autoFirma, mock(ActionResponse.class)));

        assertTrue(ex.getMessage().contains("sourceTargetFields"));
    }

    @Test
    void sendToActionResponse_sinRectangulo_lanzaError() {
        AutoFirma autoFirma = new AutoFirma(JustificacionDummy.class)
                .addSourceTargetField("pdfSolicitud", "pdfSolicitudFirmado");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> AutoFirma.sendToActionResponse(autoFirma, mock(ActionResponse.class)));

        assertTrue(ex.getMessage().contains("rectangulo"));
    }

    @Test
    void sendToActionResponse_conDefaults_publicaPayloadEsperado() {
        AutoFirma autoFirma = new AutoFirma(JustificacionDummy.class)
                .setRectangulo(new Rectangulo(10, 20, 100, 50))
                .addSourceTargetField("pdfSolicitud", "pdfSolicitudFirmado");

        ActionResponse actionResponse = mock(ActionResponse.class);
        AutoFirma.sendToActionResponse(autoFirma, actionResponse);

        verify(actionResponse).setValue("executeJs", true);
        verify(actionResponse).setValue("methodJs", "firmaController");

        Map<String, Object> payload = capturarPayload(actionResponse);

        assertEquals(null, payload.get("nif"));
        assertEquals(null, payload.get("motivo"));
        assertEquals("_signed", payload.get("sufijo"));
        assertEquals(CampoFirma.DEFAULT_NUMERO_PAGINA, payload.get("pageNumber"));
        assertEquals(CampoFirma.DEFAULT_FONT_SIZE, payload.get("fontSize"));
        assertEquals(10.0f, payload.get("signaturePositionOnPageLowerLeftX"));
        assertEquals(14.0f, payload.get("signaturePositionOnPageLowerLeftY"));
        assertEquals(110.0f, payload.get("signaturePositionOnPageUpperRightX"));
        assertEquals(64.0f, payload.get("signaturePositionOnPageUpperRightY"));

        Object sourceTargetFields = payload.get("sourceTargetFields");
        assertTrue(sourceTargetFields instanceof List<?>);
        assertEquals(1, ((List<?>) sourceTargetFields).size());
    }

    @Test
    void sendToActionResponse_conValoresCustom_publicaPayloadEsperado() {
        AutoFirma autoFirma = new AutoFirma(JustificacionDummy.class)
                .setRectangulo(new Rectangulo(3, 8, 20, 10))
                .setNif("12345678A")
                .setMotivo("Prueba de firma")
                .setSufijo("_firmado")
                .setPageNumber(2)
                .setFontSize(12)
                .addSourceTargetField("pdfSolicitud", "pdfSolicitudFirmado");

        ActionResponse actionResponse = mock(ActionResponse.class);
        AutoFirma.sendToActionResponse(autoFirma, actionResponse);

        Map<String, Object> payload = capturarPayload(actionResponse);

        assertEquals("12345678A", payload.get("nif"));
        assertEquals("Prueba de firma", payload.get("motivo"));
        assertEquals("_firmado", payload.get("sufijo"));
        assertEquals(2, payload.get("pageNumber"));
        assertEquals(12, payload.get("fontSize"));
        assertEquals(3.0f, payload.get("signaturePositionOnPageLowerLeftX"));
        assertEquals(2.0f, payload.get("signaturePositionOnPageLowerLeftY"));
        assertEquals(23.0f, payload.get("signaturePositionOnPageUpperRightX"));
        assertEquals(12.0f, payload.get("signaturePositionOnPageUpperRightY"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturarPayload(ActionResponse actionResponse) {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(actionResponse).setValue(org.mockito.ArgumentMatchers.eq("payload"), payloadCaptor.capture());
        return (Map<String, Object>) payloadCaptor.getValue();
    }

    static class BaseDummyModel extends Model {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }

    static class JustificacionDummy extends BaseDummyModel {
        private String pdfSolicitud;
        private String pdfSolicitudFirmado;

        public String getPdfSolicitud() {
            return pdfSolicitud;
        }

        public void setPdfSolicitud(String pdfSolicitud) {
            this.pdfSolicitud = pdfSolicitud;
        }

        public String getPdfSolicitudFirmado() {
            return pdfSolicitudFirmado;
        }

        public void setPdfSolicitudFirmado(String pdfSolicitudFirmado) {
            this.pdfSolicitudFirmado = pdfSolicitudFirmado;
        }
    }
}

