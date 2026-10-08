package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.util.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La diferencia de contrato entre los dos tipos de documento: el formulario es bilingüe y estampa
 * siempre los dos idiomas, así que el {@code Idioma} que se le pase a {@code generate} no le afecta.
 * El documento en prosa sale en uno solo, y eso lo fija {@link DocumentoTextoTest}.
 */
class FormularioIgnoraElIdiomaTest {

    private static final String DOCUMENTO = "documento_basico.xml";

    private final PdfGenerator generator = PdfGeneratorFactory.getPdfGenerator();

    @Test
    void elMismoFormularioSaleIgualEnCastellanoQueEnValenciano() throws IOException {
        List<String> castellano = generar(Idioma.CASTELLANO).volcado();
        List<String> valenciano = generar(Idioma.VALENCIANO).volcado();

        assertEquals(castellano, valenciano, "el idioma ha cambiado el formulario, y no debe cambiarlo");
    }

    @Test
    void elFormularioEstampaLosDosIdiomasSeaCualSeaElQueSePida() throws IOException {
        for (Idioma idioma : Idioma.values()) {
            TextosDelPdf textos = generar(idioma);
            assertTrue(textos.contiene("DADES") && textos.contiene("DATOS"),
                    "pidiendo " + idioma + " falta el título de sección de algún idioma: " + textos.volcado());
            assertTrue(textos.contiene("Signat") && textos.contiene("Firmado"),
                    "pidiendo " + idioma + " falta el texto de algún idioma: " + textos.volcado());
        }
    }

    private TextosDelPdf generar(Idioma idioma) throws IOException {
        return TextosDelPdf.de(generator.generate(DocumentosDeEjemplo.xml(DOCUMENTO),
                DocumentosDeEjemplo.contexto(DOCUMENTO), idioma));
    }
}
