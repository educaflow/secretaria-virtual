package com.educaflow.subsystem.firmas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

/** Las dos formas de decir dónde se firma una tarea de firma: el nombre de un campo de firma o un rectángulo. */
class TareaFirmaInsertDTOTest {

    private static final String MOTIVO = "Firma de la solicitud";
    private static final Rectangulo AREA = new Rectangulo(50, 50, 200, 100);

    private final User firmante = new User();
    private final Centro centro = new Centro();
    private final MetaFile solicitud = new MetaFile();
    private final MetaFile anexo = new MetaFile();

    private MockedStatic<MetaFileHelper> metaFileHelperMock;

    @BeforeEach
    void mockearEstaticos() {
        metaFileHelperMock = Mockito.mockStatic(MetaFileHelper.class);
        metaFileHelperMock.when(() -> MetaFileHelper.isPdf(Mockito.any())).thenReturn(true);
        conCamposFirmaVacios(solicitud, "firmaSolicitante");
        conCamposFirmaVacios(anexo, "firmaSolicitante", "firmaDirector");
    }

    @AfterEach
    void cerrarEstaticos() {
        metaFileHelperMock.close();
    }

    @Test
    void conNombreDeCampoFirma_noLlevaRectanguloNiPagina() {
        TareaFirmaInsertDTO dto = new TareaFirmaInsertDTO(firmante, centro, List.of(solicitud, anexo), MOTIVO, "firmaSolicitante", Notifier.class, null);

        assertEquals("firmaSolicitante", dto.nombreCampoFirma());
        assertNull(dto.areaFirma());
        assertNull(dto.page());
    }

    @Test
    void conRectangulo_noLlevaNombreDeCampoFirma() {
        TareaFirmaInsertDTO dto = new TareaFirmaInsertDTO(firmante, centro, List.of(solicitud), MOTIVO, AREA, 1, Notifier.class, null);

        assertNull(dto.nombreCampoFirma());
        assertEquals(AREA, dto.areaFirma());
        assertEquals(1, dto.page());
    }

    @Test
    void conNombreDeCampoFirmaQueNoTienenTodosLosDocumentos_lanzaErrorDiciendoCualYQueCamposTiene() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new TareaFirmaInsertDTO(firmante, centro, List.of(anexo, solicitud), MOTIVO, "firmaDirector", Notifier.class, null));

        assertTrue(ex.getMessage().contains("El documento 1"), ex.getMessage());
        assertTrue(ex.getMessage().contains("firmaDirector"), ex.getMessage());
        assertTrue(ex.getMessage().contains("firmaSolicitante"), ex.getMessage());
    }

    @Test
    void conNombreDeCampoFirmaEnBlanco_lanzaError() {
        assertThrows(IllegalArgumentException.class,
                () -> new TareaFirmaInsertDTO(firmante, centro, List.of(solicitud), MOTIVO, " ", Notifier.class, null));
    }

    @Test
    void sinNombreDeCampoFirma_lanzaError() {
        assertThrows(NullPointerException.class,
                () -> new TareaFirmaInsertDTO(firmante, centro, List.of(solicitud), MOTIVO, (String) null, Notifier.class, null));
    }

    @Test
    void sinRectanguloOSinPagina_lanzaError() {
        assertThrows(NullPointerException.class,
                () -> new TareaFirmaInsertDTO(firmante, centro, List.of(solicitud), MOTIVO, null, 1, Notifier.class, null));
        assertThrows(NullPointerException.class,
                () -> new TareaFirmaInsertDTO(firmante, centro, List.of(solicitud), MOTIVO, AREA, null, Notifier.class, null));
    }

    @Test
    void conLasDosFormasALaVez_lanzaError() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new TareaFirmaInsertDTO(firmante, centro, List.of(solicitud), MOTIVO, AREA, 1, "firmaSolicitante", Notifier.class, null));

        assertTrue(ex.getMessage().contains("no las dos"), ex.getMessage());
    }

    @Test
    void sinNingunaDeLasDosFormas_lanzaError() {
        assertThrows(NullPointerException.class,
                () -> new TareaFirmaInsertDTO(firmante, centro, List.of(solicitud), MOTIVO, null, null, null, Notifier.class, null));
    }

    private void conCamposFirmaVacios(MetaFile documento, String... camposFirmaVacios) {
        DocumentoPdf documentoPdf = mock(DocumentoPdf.class);
        when(documentoPdf.getNombreCamposFirmaVacios()).thenReturn(List.of(camposFirmaVacios));
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(documento)).thenReturn(documentoPdf);
    }

    private static final class Notifier implements TareaFirmaNotifier {
        @Override
        public void notify(TareaFirma tareaFirma, Object callBackData) {
        }
    }
}
