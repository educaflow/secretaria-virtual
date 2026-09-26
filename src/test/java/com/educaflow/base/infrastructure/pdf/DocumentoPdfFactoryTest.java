package com.educaflow.base.infrastructure.pdf;

import com.educaflow.base.infrastructure.pdf.impl.helper.ImagenPdfHelper;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoPdfFactoryTest {

    // iText escribe las coordenadas en el PDF con 2 decimales
    private static final float TOLERANCIA_PUNTOS = 0.05f;

    @Test
    void getDocumentoPdf1b() throws Exception  {
        String nombreFichero="prueba_pdf_1b.pdf";
        testDocumentoPdfFactory(nombreFichero);
    }
    @Test
    void getDocumentoPdf2b() throws Exception  {
        String nombreFichero="prueba_pdf_2b.pdf";
        testDocumentoPdfFactory(nombreFichero);

    }

    @Test
    void addNewPage() throws Exception {
        byte[] bytes = getBytes("hola_mundo.pdf");
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes, "hola_mundo.pdf");

        int paginasOriginales = documentoPdf.getNumeroPaginas();
        DocumentoPdf resultado = documentoPdf.addNewPage();

        assertEquals(paginasOriginales + 1, resultado.getNumeroPaginas());

    }

    @Test
    void getDocumentoPdfFromImagenJpegVertical() throws Exception {
        testDocumentoPdfFromImagen("imagen_test.jpeg", false);
    }

    @Test
    void getDocumentoPdfFromImagenJpegVertical2() throws Exception {
        testDocumentoPdfFromImagen("imagen_test2.jpeg", false);
    }

    @Test
    void getDocumentoPdfFromImagenJpegApaisada() throws Exception {
        testDocumentoPdfFromImagen("imagen_test_girada.jpeg", true);
    }

    @Test
    void getDocumentoPdfFromImagenJpegApaisada2() throws Exception {
        testDocumentoPdfFromImagen("imagen_test_girada2.jpeg", true);
    }

    @Test
    void getDocumentoPdfFromImagenPngVertical() throws Exception {
        testDocumentoPdfFromImagen("image_test.png", false);
    }

    @Test
    void getDocumentoPdfFromImagenPngVertical2() throws Exception {
        testDocumentoPdfFromImagen("image_test2.png", false);
    }

    @Test
    void isImagenSoportada() throws Exception {
        assertTrue(DocumentoPdfFactory.isImagenSoportada(getBytes("imagen_test.jpeg")));
        assertTrue(DocumentoPdfFactory.isImagenSoportada(getBytes("image_test.png")));
        assertFalse(DocumentoPdfFactory.isImagenSoportada(getBytes("hola_mundo.pdf")));
        assertFalse(DocumentoPdfFactory.isImagenSoportada("esto no es una imagen".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void getDocumentoPdfFromImagenNoEsImagen() {
        byte[] bytes = "esto no es una imagen".getBytes(StandardCharsets.UTF_8);

        assertThrows(RuntimeException.class, () -> DocumentoPdfFactory.getDocumentoPdfFromImagen(bytes, "imagen.pdf"));
    }

    private void testDocumentoPdfFromImagen(String nombreImagen, boolean esApaisada) throws Exception {
        byte[] bytesImagen = getBytes(nombreImagen);
        ImageData imageData = ImageDataFactory.create(bytesImagen);

        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdfFromImagen(bytesImagen, "imagen.pdf");

        assertEquals("imagen.pdf", documentoPdf.getFileName());
        assertEquals(1, documentoPdf.getNumeroPaginas());
        try (PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(documentoPdf.getDatos())))) {
            Rectangle tamanyoPagina = pdfDocument.getPage(1).getPageSize();
            PageSize a4Esperado = esApaisada ? PageSize.A4.rotate() : PageSize.A4;
            assertEquals(a4Esperado.getWidth(), tamanyoPagina.getWidth(), 0.01, "Ancho de la página");
            assertEquals(a4Esperado.getHeight(), tamanyoPagina.getHeight(), 0.01, "Alto de la página");

            Matrix ctm = getMatrizImagen(pdfDocument);
            float ancho = ctm.get(Matrix.I11);
            float b = ctm.get(Matrix.I12);
            float c = ctm.get(Matrix.I21);
            float alto = ctm.get(Matrix.I22);
            float izquierda = ctm.get(Matrix.I31);
            float abajo = ctm.get(Matrix.I32);

            assertEquals(0, b, 0.01, "La imagen no debe girarse");
            assertEquals(0, c, 0.01, "La imagen no debe girarse");

            // Sin deformar: la escala horizontal y la vertical de la imagen son la misma
            assertEquals(ancho / imageData.getWidth(), alto / imageData.getHeight(), 0.0001, "La imagen está deformada");

            // Entera dentro de los márgenes, ocupando el máximo que cabe y centrada
            float anchoDisponible = tamanyoPagina.getWidth() - 2 * ImagenPdfHelper.MARGEN_PUNTOS;
            float altoDisponible = tamanyoPagina.getHeight() - 2 * ImagenPdfHelper.MARGEN_PUNTOS;
            assertTrue(ancho <= anchoDisponible + TOLERANCIA_PUNTOS, "La imagen invade el margen por el ancho");
            assertTrue(alto <= altoDisponible + TOLERANCIA_PUNTOS, "La imagen invade el margen por el alto");
            assertTrue(Math.abs(ancho - anchoDisponible) < TOLERANCIA_PUNTOS || Math.abs(alto - altoDisponible) < TOLERANCIA_PUNTOS, "La imagen no ocupa el máximo que cabe entre los márgenes");
            assertEquals(tamanyoPagina.getWidth() - ancho, 2 * izquierda, TOLERANCIA_PUNTOS, "La imagen no está centrada en horizontal");
            assertEquals(tamanyoPagina.getHeight() - alto, 2 * abajo, TOLERANCIA_PUNTOS, "La imagen no está centrada en vertical");
        }
    }

    private Matrix getMatrizImagen(PdfDocument pdfDocument) {
        List<Matrix> matrices = new ArrayList<>();
        IEventListener listener = new IEventListener() {
            @Override
            public void eventOccurred(IEventData data, EventType type) {
                matrices.add(((ImageRenderInfo) data).getImageCtm());
            }

            @Override
            public Set<EventType> getSupportedEvents() {
                return Set.of(EventType.RENDER_IMAGE);
            }
        };
        new PdfCanvasProcessor(listener).processPageContent(pdfDocument.getPage(1));

        assertEquals(1, matrices.size(), "La página debe tener exactamente una imagen");
        return matrices.get(0);
    }

    private void testDocumentoPdfFactory(String nombreFichero)  throws Exception{

        byte[] bytes= getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes,nombreFichero);

        assertArrayEquals(bytes,documentoPdf.getDatos());
        assertEquals(nombreFichero,documentoPdf.getFileName());
    }

    public byte[] getBytes(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("No se encontró el recurso: " + resourcePath);
            }

            // 2️⃣ Lee todos los bytes
            byte[] bytes = is.readAllBytes();

            return bytes;
        }
    }

}