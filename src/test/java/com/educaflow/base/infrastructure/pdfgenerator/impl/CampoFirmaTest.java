package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.util.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.annot.PdfWidgetAnnotation;
import com.itextpdf.signatures.SignatureUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El {@code campoFirma} de los dos tipos de documento: el generador deja un campo de firma vacío en
 * el sitio en que ha quedado el hueco. Que firmar indicando su nombre estampa la firma justo ahí lo
 * comprueba {@code DocumentoPdfImplITextTest}.
 */
class CampoFirmaTest {

    private static final String FORMULARIO = """
            <documentoFormulario>
                <seccion>
                    <castellano>Datos</castellano>
                    <fila visible="self.mostrar">
                        <campo nombreCampo="self.observaciones" colspan="12"><castellano>Observaciones</castellano></campo>
                    </fila>
                </seccion>
                <seccion>
                    <castellano>Firma</castellano>
                    <fila>
                        <texto colspan="12" rowSpan="4" campoFirma="firmaSolicitante"%s>
                            <valenciano>Signatura:</valenciano>
                            <castellano>Firma:</castellano>
                        </texto>
                    </fila>
                </seccion>
            </documentoFormulario>
            """;

    private static final String TEXTO = """
            <documentoTexto>
                <titulo><castellano>Resolución</castellano></titulo>
                <parrafo visible="self.mostrar"><castellano>${self.observaciones}</castellano></parrafo>
                <tabla columnas="2">
                    <fila>
                        <parrafo alineamiento="centrado"><castellano>EL SECRETARIO</castellano></parrafo>
                        <parrafo alineamiento="centrado"><castellano>EL DIRECTOR</castellano></parrafo>
                    </fila>
                    <fila>
                        <espacio alto="80"/>
                        <espacio alto="80" campoFirma="firmaDirector"/>
                    </fila>
                </tabla>
            </documentoTexto>
            """;

    private final PdfGenerator generator = PdfGeneratorFactory.getPdfGenerator();

    // ------------------------------------------------------------ formulario

    @Test
    void elFormularioDejaElCampoFirmaEnElHuecoBajoLosTextosDeSuCelda() {
        byte[] pdf = formulario("", false, "");

        Campo campo = campoFirma(pdf, "firmaSolicitante");
        double baseDeFirma = TextosDelPdf.de(pdf).yDe("Firma:");

        assertEquals(1, campo.pagina());
        assertTrue(campo.recuadro().getTop() < baseDeFirma, "el campo queda por debajo del texto de la celda");
        assertTrue(campo.recuadro().getHeight() > 20, "y ocupa el hueco que le da el rowSpan");
        assertTrue(campo.recuadro().getWidth() > 500, "a todo el ancho de la celda");
    }

    @Test
    void elCampoFirmaSeMueveConElContenidoQueTieneDelante() {
        Campo sinFila = campoFirma(formulario("", false, ""), "firmaSolicitante");
        Campo conFila = campoFirma(formulario("", true, "Una observación"), "firmaSolicitante");

        assertTrue(conFila.recuadro().getBottom() < sinFila.recuadro().getBottom(),
                "al aparecer la fila de delante, el campo de firma baja con su celda");
        assertEquals(sinFila.recuadro().getHeight(), conFila.recuadro().getHeight(), 0.01);
    }

    @Test
    void elCampoFirmaSaltaDePaginaConSuCelda() {
        byte[] pdf = formularioDeDosPaginas();

        assertEquals(2, campoFirma(pdf, "firmaSolicitante").pagina());
    }

    /** Va alargando el valor de la fila de delante hasta que la celda de la firma ya no cabe en la primera página. */
    private byte[] formularioDeDosPaginas() {
        for (int palabras = 100; palabras <= 2000; palabras += 100) {
            byte[] pdf = formulario("", true, String.join(" ", Collections.nCopies(palabras, "observación")));
            if (DocumentoPdfFactory.getDocumentoPdf(pdf, "prueba.pdf").getNumeroPaginas() == 2) {
                return pdf;
            }
        }
        throw new AssertionError("Ningún valor ha dejado el formulario en exactamente dos páginas");
    }

    @Test
    void unaCeldaReservadaNoDejaCampoFirma() {
        byte[] pdf = formulario(" visible=\"false\" siOculto=\"reservar\"", false, "");

        assertEquals(List.of(), camposFirmaVacios(pdf));
    }

    @Test
    void unaCeldaSinHuecoBajoSusTextosAborta() {
        String sinRowSpan = FORMULARIO.formatted("").replace(" rowSpan=\"4\"", "");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> generar(sinRowSpan, false, ""));

        assertTrue(ex.getMessage().contains("firmaSolicitante"), ex.getMessage());
        assertTrue(ex.getMessage().contains("rowSpan"), ex.getMessage());
    }

    @Test
    void dosCamposFirmaConElMismoNombreAbortan() {
        String repetido = TEXTO.replace("<espacio alto=\"80\"/>", "<espacio alto=\"80\" campoFirma=\"firmaDirector\"/>");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> generar(repetido, false, ""));

        assertTrue(ex.getMessage().contains("firmaDirector"), ex.getMessage());
    }

    @Test
    void elCampoFirmaNoEsUnCampoDeFormularioQueRellenar() {
        DocumentoPdf documento = DocumentoPdfFactory.getDocumentoPdf(formulario("", false, ""), "prueba.pdf");

        assertEquals(List.of(), documento.getNombreCamposFormulario());
        assertEquals(List.of(), documento.getFirmasPdf());
    }

    // ----------------------------------------------------------------- texto

    @Test
    void elDocumentoDeTextoDejaElCampoFirmaEnTodoElHuecoDeSuEspacio() {
        byte[] pdf = generar(TEXTO, false, "");

        Campo campo = campoFirma(pdf, "firmaDirector");
        TextosDelPdf textos = TextosDelPdf.de(pdf);

        assertEquals(1, campo.pagina());
        assertEquals(80, campo.recuadro().getHeight(), 0.01);
        assertTrue(campo.recuadro().getTop() < textos.yDe("DIRECTOR"), "debajo del cargo que firma");
        assertTrue(campo.recuadro().getLeft() > 250, "en la columna de la derecha, que es la suya");
    }

    @Test
    void elCampoFirmaDelDocumentoDeTextoSeMueveConElContenidoQueTieneDelante() {
        Campo sinParrafo = campoFirma(generar(TEXTO, false, ""), "firmaDirector");
        Campo conParrafo = campoFirma(generar(TEXTO, true, "Una observación"), "firmaDirector");

        assertTrue(conParrafo.recuadro().getBottom() < sinParrafo.recuadro().getBottom());
    }

    // -------------------------------------------------------------- utilidades

    private byte[] formulario(String atributosDeLaCelda, boolean mostrar, String observaciones) {
        return generar(FORMULARIO.formatted(atributosDeLaCelda), mostrar, observaciones);
    }

    private byte[] generar(String xml, boolean mostrar, String observaciones) {
        Map<String, Object> self = Map.of("mostrar", mostrar, "observaciones", observaciones);
        return generator.generate(xml.getBytes(StandardCharsets.UTF_8),
                Map.of("self", self, "now", LocalDateTime.of(2026, 9, 24, 10, 30)), Idioma.CASTELLANO);
    }

    /** Un campo de firma: la página en la que está y su recuadro. */
    private record Campo(int pagina, Rectangle recuadro) {
    }

    private static List<String> camposFirmaVacios(byte[] pdf) {
        try (PdfDocument documento = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            return new SignatureUtil(documento).getBlankSignatureNames();
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /** El campo de firma con ese nombre, que además tiene que estar vacío. */
    private static Campo campoFirma(byte[] pdf, String nombre) {
        assertTrue(camposFirmaVacios(pdf).contains(nombre), "campos de firma vacíos: " + camposFirmaVacios(pdf));
        try (PdfDocument documento = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            PdfFormField campo = PdfAcroForm.getAcroForm(documento, false).getField(nombre);
            PdfWidgetAnnotation widget = campo.getWidgets().get(0);
            return new Campo(documento.getPageNumber(widget.getPage()), widget.getRectangle().toRectangle());
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }
}
