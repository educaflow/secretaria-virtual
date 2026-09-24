package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.evaluator.Evaluator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.impl.dibujo.DibujantePdf;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Documento;
import com.educaflow.base.infrastructure.pdfgenerator.impl.parser.DefinicionDocumentoParser;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.DocumentoVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.ResolutorVisibilidad;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.Valores;

import java.util.List;
import java.util.Map;

/**
 * Generación con iText: parsear el XML resuelto, decidir qué se ve (evaluando los {@code visible}),
 * evaluar los valores de lo que se ve y dibujarlo.
 */
public class PdfGeneratorImplIText implements PdfGenerator {

    private final Evaluator evaluator;
    private final DefinicionDocumentoParser parser = new DefinicionDocumentoParser();

    public PdfGeneratorImplIText(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public byte[] generate(byte[] documentoXml, Map<String, Object> contexto) {
        Documento documento = parser.parse(documentoXml);
        DocumentoVisible visible = new ResolutorVisibilidad(evaluator, contexto).resolver(documento);
        Valores valores = Valores.de(visible, evaluator, contexto);

        return DibujantePdf.dibujar(visible, valores);
    }

    @Override
    public List<String> getExpresiones(byte[] documentoXml) {
        return parser.parse(documentoXml).expresiones();
    }
}
