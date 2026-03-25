package com.educaflow.base.infrastructure.autofirma;

import com.axelor.db.Model;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoFirmaFieldPathTest {

    @Test
    void addSourceTargetField_conCampoSimple_noLanzaError() {
        AutoFirma autoFirma = new AutoFirma(JustificacionDummy.class);

        assertDoesNotThrow(() -> autoFirma.addSourceTargetField("pdfSolicitud", "pdfSolicitudFirmado"));
    }

    @Test
    void addSourceTargetField_conCampoEnListaIndexada_noLanzaError() {
        AutoFirma autoFirma = new AutoFirma(TareaFirmaDummy.class);

        assertDoesNotThrow(() ->
                autoFirma.addSourceTargetField("documentosFirma[0].documentoOriginal", "documentosFirma[0].documentoFirmado")
        );
    }

    @Test
    void addSourceTargetField_conClaseRaizIncorrecta_lanzaErrorEsperado() {
        AutoFirma autoFirma = new AutoFirma(JustificacionDummy.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                autoFirma.addSourceTargetField("documentosFirma[0].documentoOriginal", "documentosFirma[0].documentoFirmado")
        );

        assertTrue(ex.getMessage().contains("El getter getDocumentosFirma no existe"));
        assertTrue(ex.getMessage().contains(JustificacionDummy.class.getName()));
    }

    @Test
    void addSourceTargetField_conIndiceNoNumerico_lanzaErrorEsperado() {
        AutoFirma autoFirma = new AutoFirma(TareaFirmaDummy.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                autoFirma.addSourceTargetField("documentosFirma[a].documentoOriginal", "documentosFirma[0].documentoFirmado")
        );

        assertTrue(ex.getMessage().contains("no es un número válido"));
    }

    @Test
    void addSourceTargetField_conIndiceNegativo_lanzaErrorEsperado() {
        AutoFirma autoFirma = new AutoFirma(TareaFirmaDummy.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                autoFirma.addSourceTargetField("documentosFirma[-1].documentoOriginal", "documentosFirma[0].documentoFirmado")
        );

        assertTrue(ex.getMessage().contains("debe ser mayor o igual a 0"));
    }

    @Test
    void addSourceTargetField_conFormatoIndiceInvalido_lanzaErrorEsperado() {
        AutoFirma autoFirma = new AutoFirma(TareaFirmaDummy.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                autoFirma.addSourceTargetField("documentosFirma[0.documentoOriginal", "documentosFirma[0].documentoFirmado")
        );

        assertTrue(ex.getMessage().contains("Formato de índice inválido"));
    }

    @Test
    void addSourceTargetField_conIndiceEnCampoNoLista_lanzaErrorEsperado() {
        AutoFirma autoFirma = new AutoFirma(JustificacionDummy.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                autoFirma.addSourceTargetField("pdfSolicitud[0]", "pdfSolicitudFirmado")
        );

        assertTrue(ex.getMessage().contains("no es una List"));
    }

    @Test
    void addSourceTargetField_conListaSinTipoGenerico_lanzaErrorEsperado() {
        AutoFirma autoFirma = new AutoFirma(TareaFirmaRawListDummy.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                autoFirma.addSourceTargetField("documentosFirma[0].documentoOriginal", "documentosFirma[0].documentoFirmado")
        );

        assertTrue(ex.getMessage().contains("no tiene tipo genérico definido"));
    }

    @Test
    void addSourceTargetField_conSetterAusente_lanzaErrorEsperado() {
        AutoFirma autoFirma = new AutoFirma(ModeloSinSetterDummy.class);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                autoFirma.addSourceTargetField("campoSoloLectura", "campoSoloLectura")
        );

        assertTrue(ex.getMessage().contains("El setter setCampoSoloLectura no existe"));
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

    static class TareaFirmaDummy extends BaseDummyModel {
        private List<DocumentoFirmaDummy> documentosFirma;

        public List<DocumentoFirmaDummy> getDocumentosFirma() {
            return documentosFirma;
        }

        public void setDocumentosFirma(List<DocumentoFirmaDummy> documentosFirma) {
            this.documentosFirma = documentosFirma;
        }
    }

    static class DocumentoFirmaDummy extends BaseDummyModel {
        private String documentoOriginal;
        private String documentoFirmado;

        public String getDocumentoOriginal() {
            return documentoOriginal;
        }

        public void setDocumentoOriginal(String documentoOriginal) {
            this.documentoOriginal = documentoOriginal;
        }

        public String getDocumentoFirmado() {
            return documentoFirmado;
        }

        public void setDocumentoFirmado(String documentoFirmado) {
            this.documentoFirmado = documentoFirmado;
        }
    }

    static class TareaFirmaRawListDummy extends BaseDummyModel {
        private List documentosFirma;

        public List getDocumentosFirma() {
            return documentosFirma;
        }

        public void setDocumentosFirma(List documentosFirma) {
            this.documentosFirma = documentosFirma;
        }
    }

    static class ModeloSinSetterDummy extends BaseDummyModel {
        private String campoSoloLectura;

        public String getCampoSoloLectura() {
            return campoSoloLectura;
        }
    }
}

