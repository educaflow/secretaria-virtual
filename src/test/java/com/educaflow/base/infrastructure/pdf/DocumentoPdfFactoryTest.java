package com.educaflow.base.infrastructure.pdf;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoPdfFactoryTest {

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