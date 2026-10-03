package com.educaflow.base.infrastructure.metafile;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.MetaFileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

class MetaFileHelperTest {

    private MockedStatic<MetaFileUtil> metaFileUtilMock;

    @BeforeEach
    void setUp() {
        metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class);
    }

    @AfterEach
    void tearDown() {
        metaFileUtilMock.close();
    }

    @Test
    void isPdf_null_devuelveFalse() {
        assertFalse(MetaFileHelper.isPdf(null));
    }

    @Test
    void isPdf_contenidoPdfDeclaradoComoOctetStream_devuelveTrue() {
        MetaFile metaFile = metaFile("application/octet-stream", "%PDF-1.7\n...");

        assertTrue(MetaFileHelper.isPdf(metaFile));
    }

    @Test
    void isPdf_cabeceraPrecedidaDeBytesDentroDelMargenQueAceptaIText_devuelveTrue() {
        MetaFile metaFile = metaFile("application/octet-stream", "basura previa\n%PDF-1.7\n...");

        assertTrue(MetaFileHelper.isPdf(metaFile));
    }

    @Test
    void isPdf_cabeceraMasAllaDelMargenQueAceptaIText_devuelveFalse() {
        MetaFile metaFile = metaFile("application/octet-stream", "x".repeat(2000) + "%PDF-1.7\n...");

        assertFalse(MetaFileHelper.isPdf(metaFile));
    }

    @Test
    void isPdf_contenidoQueNoEsPdfDeclaradoComoPdf_devuelveFalse() {
        MetaFile metaFile = metaFile(MetaFileHelper.PDF_MIME_TYPE, "MZ ejecutable renombrado");

        assertFalse(MetaFileHelper.isPdf(metaFile));
    }

    @Test
    void isPdf_contenidoMasCortoQueLaCabecera_devuelveFalse() {
        MetaFile metaFile = metaFile(MetaFileHelper.PDF_MIME_TYPE, "%PD");

        assertFalse(MetaFileHelper.isPdf(metaFile));
    }

    @Test
    void isPdf_noCargaElFicheroEnteroEnMemoria() {
        MetaFile metaFile = metaFile(MetaFileHelper.PDF_MIME_TYPE, "%PDF-1.7\n...");

        MetaFileHelper.isPdf(metaFile);

        metaFileUtilMock.verify(() -> MetaFileUtil.downloadContent(any()), never());
    }

    private MetaFile metaFile(String fileType, String contenido) {
        MetaFile metaFile = new MetaFile();
        metaFile.setFileType(fileType);
        byte[] bytes = contenido.getBytes(StandardCharsets.US_ASCII);
        metaFileUtilMock.when(() -> MetaFileUtil.downloadHeader(eq(metaFile), anyInt()))
                .thenAnswer(invocation -> Arrays.copyOf(bytes, Math.min(bytes.length, invocation.<Integer>getArgument(1))));

        return metaFile;
    }
}
