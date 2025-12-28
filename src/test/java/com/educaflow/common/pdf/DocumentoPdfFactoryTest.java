package com.educaflow.common.pdf;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

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