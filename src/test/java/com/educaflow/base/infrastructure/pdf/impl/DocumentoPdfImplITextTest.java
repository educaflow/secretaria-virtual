package com.educaflow.base.infrastructure.pdf.impl;

import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.pdf.impl.helper.PdfDocumentHelper;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.SignatureFormFieldBuilder;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.annot.PdfWidgetAnnotation;
import com.itextpdf.pdfa.exceptions.PdfAConformanceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
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
        EntornoCriptografico.configureAlmacenCertificadosConfiables(null);
        EntornoCriptografico.configureDispositivosCriptograficos(null);
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
    void firmarIndicandoElNombreDelCampoEstampaLaFirmaEnSuRecuadro() {
        DocumentoPdf documentoPdf = pdfConCampoFirmaVacio("firmaSolicitante");

        DocumentoPdf documentoPdfFirmado = firmarPdf(documentoPdf, new CampoFirma("firmaSolicitante"));

        assertEquals(1, documentoPdfFirmado.getFirmasPdf().size());
        assertEquals("firmaSolicitante", documentoPdfFirmado.getFirmasPdf().get(0).getNombreCampo());
        assertEquals(true, documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());
        assertEquals(List.of(), camposFirmaVacios(documentoPdfFirmado));

        Rectangle recuadro = recuadroDelCampo(documentoPdfFirmado, "firmaSolicitante");
        assertEquals(RECUADRO_CAMPO_FIRMA.getLeft(), recuadro.getLeft(), 0.01);
        assertEquals(RECUADRO_CAMPO_FIRMA.getBottom(), recuadro.getBottom(), 0.01);
        assertEquals(RECUADRO_CAMPO_FIRMA.getWidth(), recuadro.getWidth(), 0.01);
        assertEquals(RECUADRO_CAMPO_FIRMA.getHeight(), recuadro.getHeight(), 0.01);
    }

    @Test
    void firmarEnUnCampoQueNoExisteAbortaYDiceQueCamposHay() {
        DocumentoPdf documentoPdf = pdfConCampoFirmaVacio("firmaSolicitante");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> firmarPdf(documentoPdf, new CampoFirma("firmaDirector")));

        String mensaje = ex.getCause().getMessage();
        assertTrue(mensaje.contains("firmaDirector"), mensaje);
        assertTrue(mensaje.contains("firmaSolicitante"), mensaje);
    }

    @Test
    void firmarDosVecesEnElMismoCampoAborta() {
        DocumentoPdf documentoPdfFirmado = firmarPdf(pdfConCampoFirmaVacio("firmaSolicitante"), new CampoFirma("firmaSolicitante"));

        assertThrows(RuntimeException.class, () -> firmarPdf(documentoPdfFirmado, new CampoFirma("firmaSolicitante")));
    }

    @Test
    void firmarEnUnRectanguloSigueFuncionandoAunqueElPdfTengaCamposFirma() {
        DocumentoPdf documentoPdf = pdfConCampoFirmaVacio("firmaSolicitante");

        DocumentoPdf documentoPdfFirmado = firmarPdf(documentoPdf, new CampoFirma(new Rectangulo(100, 20, 300, 40)).setNumeroPagina(1));

        assertEquals("Signature1", documentoPdfFirmado.getFirmasPdf().get(0).getNombreCampo());
        assertEquals(List.of("firmaSolicitante"), camposFirmaVacios(documentoPdfFirmado));
    }

    @Test
    void elCampoFirmaVacioSobreviveAlAnyadirOtroDocumentoYSePuedeFirmarEnEl() {
        DocumentoPdf anexo = DocumentoPdfFactory.getDocumentoPdf(getBytes(FILE_HOLA_MUNDO), FILE_HOLA_MUNDO);

        DocumentoPdf unido = anexo.anyadirDocumentoPdf(pdfConCampoFirmaVacio("firmaSolicitante"));

        assertEquals(List.of("firmaSolicitante"), camposFirmaVacios(unido));
        DocumentoPdf documentoPdfFirmado = firmarPdf(unido, new CampoFirma("firmaSolicitante"));
        assertEquals(true, documentoPdfFirmado.getFirmasPdf().get(0).isCorrecta());
        assertEquals(unido.getNumeroPaginas(), paginaDelCampo(documentoPdfFirmado, "firmaSolicitante"), "el campo sigue en su página, que ahora es la última");
    }

    @Test
    void alAnyadirUnDocumentoYaFirmadoSuFirmaNoPasaAlDocumentoUnido() {
        DocumentoPdf firmado = firmarPdf(pdfConCampoFirmaVacio("firmaSolicitante"), new CampoFirma("firmaSolicitante"));
        DocumentoPdf portada = DocumentoPdfFactory.getDocumentoPdf(getBytes(FILE_HOLA_MUNDO), FILE_HOLA_MUNDO);

        DocumentoPdf unido = portada.anyadirDocumentoPdf(firmado);

        assertEquals(List.of(), unido.getFirmasPdf());
        assertEquals(List.of(), camposFirmaVacios(unido));
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

    private static final Rectangle RECUADRO_CAMPO_FIRMA = new Rectangle(60, 500, 300, 45);

    /** Un PDF de una página con un campo de firma vacío, como los que deja el generador de documentos. */
    private DocumentoPdf pdfConCampoFirmaVacio(String nombreCampo) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDocument = new PdfDocument(new PdfWriter(salida))) {
            PdfPage pagina = pdfDocument.addNewPage();
            PdfAcroForm.getAcroForm(pdfDocument, true).addField(new SignatureFormFieldBuilder(pdfDocument, nombreCampo)
                    .setWidgetRectangle(RECUADRO_CAMPO_FIRMA).setPage(pagina).createSignature(), pagina);
        }
        return DocumentoPdfFactory.getDocumentoPdf(salida.toByteArray(), "con_campo_firma.pdf");
    }

    private DocumentoPdf firmarPdf(DocumentoPdf documentoPdf, CampoFirma campoFirma) {
        AlmacenClaveFichero almacenClaveSistemaArchivos = new AlmacenClaveFichero(this.getClass().getResourceAsStream(FILE_CERTIFICADO), PASSWORD_CERTIFICADO);
        return documentoPdf.firmar(almacenClaveSistemaArchivos, campoFirma.setFechaFirma(LocalDateTime.of(2025, 8, 1, 14, 30, 45)));
    }

    private static List<String> camposFirmaVacios(DocumentoPdf documentoPdf) {
        return documentoPdf.getNombreCamposFirmaVacios();
    }

    private static Rectangle recuadroDelCampo(DocumentoPdf documentoPdf, String nombreCampo) {
        return widgetDelCampo(documentoPdf, nombreCampo).getRectangle().toRectangle();
    }

    private static int paginaDelCampo(DocumentoPdf documentoPdf, String nombreCampo) {
        PdfWidgetAnnotation widget = widgetDelCampo(documentoPdf, nombreCampo);
        return widget.getPage().getDocument().getPageNumber(widget.getPage());
    }

    private static PdfWidgetAnnotation widgetDelCampo(DocumentoPdf documentoPdf, String nombreCampo) {
        PdfDocument pdfDocument = PdfDocumentHelper.getPdfDocument(documentoPdf.getDatos());
        return PdfAcroForm.getAcroForm(pdfDocument, false).getField(nombreCampo).getWidgets().get(0);
    }

    private DocumentoPdf firmarPdf(DocumentoPdf documentoPdf,String ficheroCertificado, String passwordCertificado) {


        AlmacenClaveFichero almacenClaveSistemaArchivos = new AlmacenClaveFichero(this.getClass().getResourceAsStream(ficheroCertificado),passwordCertificado);
        CampoFirma campoFirma= new CampoFirma(new Rectangulo(100,150,130,100)).setFontSize(8).setNumeroPagina(1).setFechaFirma(LocalDateTime.of(2025, 8, 1, 14, 30, 45)).setMensaje("Firmado para pruebas").setMotivo("Motivo Pruebas unitarias");
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