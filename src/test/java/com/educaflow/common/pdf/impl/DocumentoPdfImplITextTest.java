package com.educaflow.common.pdf.impl;

import com.educaflow.common.criptografia.EntornoCriptografico;
import com.educaflow.common.criptografia.AlmacenClaveFichero;
import com.educaflow.common.pdf.CampoFirma;
import com.educaflow.common.pdf.DocumentoPdf;
import com.educaflow.common.pdf.DocumentoPdfFactory;
import com.educaflow.common.pdf.Rectangulo;
import com.educaflow.common.pdf.impl.helper.PdfDocumentHelper;
import com.itextpdf.pdfa.exceptions.PdfAConformanceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentoPdfImplITextTest {

    public static final String FILE_CERTIFICADO="../mi_certificado_password_nadanada.p12";
    public static final String PASSWORD_CERTIFICADO="nadanada";
    public static final String FILE_PDF_1b="../prueba_pdf_1b.pdf";
    public static final String FILE_PDF_2b="../prueba_pdf_2b.pdf";
    public static final String SUBJECT="Juan Garcia NIF:1234567Z";
    public static final String ISSUER="Juan Garcia NIF:1234567Z";

    public static final String FILE_HOLA_MUNDO="../hola_mundo.pdf";
    public static final String FILE_HOLA_MUNDO_PDF_1b="../hola_mundo_1b.pdf";
    public static final String FILE_HOLA_MUNDO_PDF_2b="../hola_mundo_2b.pdf";

    @BeforeAll
    static void initAll() {
        EntornoCriptografico.configure(null);
    }


    @Test
    void getDatos()  {
        String nombreFichero=FILE_PDF_1b;
        byte[] bytes= getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes,nombreFichero);

        assertArrayEquals(bytes,documentoPdf.getDatos());
    }

    @Test
    void getFileName() {
        String nombreFichero=FILE_PDF_1b;
        byte[] bytes= getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes,nombreFichero);
        assertEquals(nombreFichero,documentoPdf.getFileName());
    }

    @Test
    void getNumeroPaginas() {
        String nombreFichero=FILE_PDF_1b;
        byte[] bytes= getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes,nombreFichero);

        assertEquals(1,documentoPdf.getNumeroPaginas());
    }

    @Test
    void getNombreCamposFormulario() {
        String nombreFichero=FILE_PDF_1b;
        byte[] bytes= getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes,nombreFichero);

        assertEquals(3,documentoPdf.getNombreCamposFormulario().size());
        assertEquals((List<String>)List.of("campo1","campo2","campo3"),documentoPdf.getNombreCamposFormulario());
    }

    @Test
    void getFirmasPdfNinguna() {
        String nombreFichero=FILE_PDF_1b;
        byte[] bytes= getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes,nombreFichero);

        assertEquals(0,documentoPdf.getFirmasPdf().size());
    }

    @Test
    void getFirmasPdf1b_Fallafirma() {
        String nombreFichero=FILE_PDF_1b;
        byte[] bytes= getBytes(nombreFichero);

        DocumentoPdf documentoPdfPlantilla = DocumentoPdfFactory.getDocumentoPdf(bytes,nombreFichero);
        Map<String,String> mapaCampos= new HashMap<>();
        DocumentoPdf documentoPdfConDatos=documentoPdfPlantilla.setValorCamposFormularioAndFlatten(mapaCampos);

        assertThrowsCause(PdfAConformanceException.class, () -> {
            firmarPdf(documentoPdfConDatos, FILE_CERTIFICADO, PASSWORD_CERTIFICADO);
        });
    }

    @Test
    void getFirmasPdf2b_Fallafirma() {
        String nombreFichero=FILE_PDF_2b;
        byte[] bytes= getBytes(nombreFichero);

        DocumentoPdf documentoPdfPlantilla = DocumentoPdfFactory.getDocumentoPdf(bytes,nombreFichero);
        Map<String,String> mapaCampos= new HashMap<>();
        DocumentoPdf documentoPdfConDatos=documentoPdfPlantilla.setValorCamposFormularioAndFlatten(mapaCampos);

        assertThrowsCause(PdfAConformanceException.class, () -> {
            firmarPdf(documentoPdfConDatos, FILE_CERTIFICADO, PASSWORD_CERTIFICADO);
        });
    }

    @Test
    void getFirmasPdf1b() {
        String nombreFichero=FILE_PDF_1b;
        byte[] bytes= getBytes(nombreFichero);
        byte[] datosArreglados= PdfDocumentHelper.removePdfAConformance(bytes);
        DocumentoPdf documentoPdfPlantilla = DocumentoPdfFactory.getDocumentoPdf(datosArreglados,nombreFichero);
        Map<String,String> mapaCampos= new HashMap<>();
        DocumentoPdf documentoPdfConDatos=documentoPdfPlantilla.setValorCamposFormularioAndFlatten(mapaCampos);

        DocumentoPdf documentoPdfFirmado= firmarPdf(documentoPdfConDatos,FILE_CERTIFICADO,PASSWORD_CERTIFICADO);

        assertEquals(1,documentoPdfFirmado.getFirmasPdf().size());
        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());
        assertEquals(false,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().isValidoEnListaCertificadosConfiables());
        assertEquals(null,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getTipoEmisorCertificado());
        assertEquals(SUBJECT,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getCnSubject());
        assertEquals(ISSUER,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getCnIssuer());
    }

    @Test
    void getFirmasPdf2b() {
        String nombreFichero=FILE_PDF_2b;
        byte[] bytes= getBytes(nombreFichero);
        byte[] datosArreglados= PdfDocumentHelper.removePdfAConformance(bytes);
        DocumentoPdf documentoPdfPlantilla = DocumentoPdfFactory.getDocumentoPdf(datosArreglados,nombreFichero);
        Map<String,String> mapaCampos= new HashMap<>();
        DocumentoPdf documentoPdfConDatos=documentoPdfPlantilla.setValorCamposFormularioAndFlatten(mapaCampos);

        DocumentoPdf documentoPdfFirmado= firmarPdf(documentoPdfConDatos,FILE_CERTIFICADO,PASSWORD_CERTIFICADO);

        assertEquals(1,documentoPdfFirmado.getFirmasPdf().size());
        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());
        assertEquals(false,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().isValidoEnListaCertificadosConfiables());
        assertEquals(null,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getTipoEmisorCertificado());
        assertEquals(SUBJECT,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getCnSubject());
        assertEquals(ISSUER,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getCnIssuer());
    }

    @Test
    void getFirmasPdf1b_2firmas() {
        String nombreFichero=FILE_PDF_1b;
        byte[] bytes= getBytes(nombreFichero);
        byte[] datosArreglados= PdfDocumentHelper.removePdfAConformance(bytes);
        DocumentoPdf documentoPdfPlantilla = DocumentoPdfFactory.getDocumentoPdf(datosArreglados,nombreFichero);
        Map<String,String> mapaCampos= new HashMap<>();
        DocumentoPdf documentoPdfConDatos=documentoPdfPlantilla.setValorCamposFormularioAndFlatten(mapaCampos);

        DocumentoPdf documentoPdfFirmado= firmarPdf(documentoPdfConDatos,FILE_CERTIFICADO,PASSWORD_CERTIFICADO);
        documentoPdfFirmado= firmarPdf(documentoPdfFirmado,FILE_CERTIFICADO,PASSWORD_CERTIFICADO);

        assertEquals(2,documentoPdfFirmado.getFirmasPdf().size());
        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());
        assertEquals(false,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().isValidoEnListaCertificadosConfiables());
        assertEquals(null,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getTipoEmisorCertificado());
        assertEquals(SUBJECT,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getCnSubject());
        assertEquals(ISSUER,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getCnIssuer());

        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(1).isCorrecta());
        assertEquals(false,documentoPdfFirmado.getFirmasPdf().get(1).getDatosCertificado().isValidoEnListaCertificadosConfiables());
        assertEquals(null,documentoPdfFirmado.getFirmasPdf().get(1).getDatosCertificado().getTipoEmisorCertificado());
        assertEquals(SUBJECT,documentoPdfFirmado.getFirmasPdf().get(1).getDatosCertificado().getCnSubject());
        assertEquals(ISSUER,documentoPdfFirmado.getFirmasPdf().get(1).getDatosCertificado().getCnIssuer());
    }

    @Test
    void getFirmasPdf2b_2firmas() {
        String nombreFichero=FILE_PDF_2b;
        byte[] bytes= getBytes(nombreFichero);
        byte[] datosArreglados= PdfDocumentHelper.removePdfAConformance(bytes);
        DocumentoPdf documentoPdfPlantilla = DocumentoPdfFactory.getDocumentoPdf(datosArreglados,nombreFichero);
        Map<String,String> mapaCampos= new HashMap<>();
        DocumentoPdf documentoPdfConDatos=documentoPdfPlantilla.setValorCamposFormularioAndFlatten(mapaCampos);

        DocumentoPdf documentoPdfFirmado= firmarPdf(documentoPdfConDatos,FILE_CERTIFICADO,PASSWORD_CERTIFICADO);
        documentoPdfFirmado= firmarPdf(documentoPdfFirmado,FILE_CERTIFICADO,PASSWORD_CERTIFICADO);

        assertEquals(2,documentoPdfFirmado.getFirmasPdf().size());
        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());
        assertEquals(false,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().isValidoEnListaCertificadosConfiables());
        assertEquals(null,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getTipoEmisorCertificado());
        assertEquals(SUBJECT,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getCnSubject());
        assertEquals(ISSUER,documentoPdfFirmado.getFirmasPdf().get(0).getDatosCertificado().getCnIssuer());

        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(1).isCorrecta());
        assertEquals(false,documentoPdfFirmado.getFirmasPdf().get(1).getDatosCertificado().isValidoEnListaCertificadosConfiables());
        assertEquals(null,documentoPdfFirmado.getFirmasPdf().get(1).getDatosCertificado().getTipoEmisorCertificado());
        assertEquals(SUBJECT,documentoPdfFirmado.getFirmasPdf().get(1).getDatosCertificado().getCnSubject());
        assertEquals(ISSUER,documentoPdfFirmado.getFirmasPdf().get(1).getDatosCertificado().getCnIssuer());
    }



    @Test
    void firmarHolaMundo() {
        String nombreFichero = FILE_HOLA_MUNDO;
        byte[] bytes = getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes, nombreFichero);

        DocumentoPdf documentoPdfFirmado = firmarPdf(documentoPdf, FILE_CERTIFICADO, PASSWORD_CERTIFICADO);

        assertEquals(1,documentoPdfFirmado.getFirmasPdf().size());
        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());
    }

    @Test
    void firmarHolaMundo1b() {
        String nombreFichero = FILE_HOLA_MUNDO_PDF_1b;
        byte[] bytes = getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes, nombreFichero);

        assertThrowsCause(PdfAConformanceException.class, () -> {
            firmarPdf(documentoPdf, FILE_CERTIFICADO, PASSWORD_CERTIFICADO);
        });
    }

    @Test
    void firmarHolaMundo2b() {
        String nombreFichero = FILE_HOLA_MUNDO_PDF_1b;
        byte[] bytes = getBytes(nombreFichero);
        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes, nombreFichero);

        assertThrowsCause(PdfAConformanceException.class, () -> {
            firmarPdf(documentoPdf, FILE_CERTIFICADO, PASSWORD_CERTIFICADO);
        });
    }

    @Test
    void firmarHolaMundo1bArreglado() {
        String nombreFichero = FILE_HOLA_MUNDO_PDF_1b;
        byte[] bytes = getBytes(nombreFichero);

        byte[] datosArreglados= PdfDocumentHelper.removePdfAConformance(bytes);

        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(datosArreglados, nombreFichero);

        DocumentoPdf documentoPdfFirmado = firmarPdf(documentoPdf, FILE_CERTIFICADO, PASSWORD_CERTIFICADO);

        assertEquals(1,documentoPdfFirmado.getFirmasPdf().size());
        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());
    }

    @Test
    void firmarHolaMundo2bArreglado() {
        String nombreFichero = FILE_HOLA_MUNDO_PDF_2b;
        byte[] bytes = getBytes(nombreFichero);

        byte[] datosArreglados= PdfDocumentHelper.removePdfAConformance(bytes);

        DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(datosArreglados, nombreFichero);

        DocumentoPdf documentoPdfFirmado = firmarPdf(documentoPdf, FILE_CERTIFICADO, PASSWORD_CERTIFICADO);

        assertEquals(1,documentoPdfFirmado.getFirmasPdf().size());
        assertEquals(true,documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());

    }


    @Test
    void firmar() {
    }

    @Test
    void anyadirDocumentoPdf() {
    }

    @Test
    void testAnyadirDocumentoPdf() {
    }


    public byte[] getBytes(String resourcePath)  {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("No se encontró el recurso: " + resourcePath);
            }

            // 2️⃣ Lee todos los bytes
            byte[] bytes = is.readAllBytes();

            return bytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DocumentoPdf firmarPdf(DocumentoPdf documentoPdf,String ficheroCertificado, String passwordCertificado) {


        AlmacenClaveFichero almacenClaveSistemaArchivos = new AlmacenClaveFichero(this.getClass().getResourceAsStream(ficheroCertificado),passwordCertificado);
        CampoFirma campoFirma=(new CampoFirma(new Rectangulo(100,150,130,100))).setFontSize(8).setNumeroPagina(1).setFechaFirma(LocalDateTime.of(2025, 8, 1, 14, 30, 45)).setMensaje("Firmado para pruebas").setMotivo("Motivo Pruebas unitarias");
        DocumentoPdf documentoPdfFirmado= documentoPdf.firmar(almacenClaveSistemaArchivos,campoFirma);



        return documentoPdfFirmado;
    }

    public static <T extends Throwable> T assertThrowsCause(Class<T> expectedType, Executable executable) {
        Throwable ex = assertThrows(Throwable.class, executable);

        Throwable cause = ex;
        while (cause != null) {
            if (expectedType.isInstance(cause)) {
                @SuppressWarnings("unchecked")
                T result = (T) cause;
                return result;
            }
            cause = cause.getCause();
        }

        fail("No se encontró excepción de tipo " + expectedType.getName() +
                " en la cadena de causas de " + ex);
        return null; // nunca llega aquí
    }
}