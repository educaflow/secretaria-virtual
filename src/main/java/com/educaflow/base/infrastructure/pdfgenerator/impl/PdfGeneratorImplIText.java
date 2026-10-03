package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.evaluator.Evaluator;
import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.expresion.Valores;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.dibujo.DibujanteDocumentoTexto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.DocumentoTexto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.parser.DocumentoTextoParser;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.visibilidad.DocumentoTextoVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.visibilidad.ResolutorPresencia;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.dibujo.DibujantePdf;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Documento;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.parser.DefinicionDocumentoParser;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.DocumentoVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.ResolutorVisibilidad;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public class PdfGeneratorImplIText implements PdfGenerator {

    private static final String FORMULARIO = "documentoFormulario";
    private static final String TEXTO = "documentoTexto";

    private final Evaluator evaluator;
    private final DefinicionDocumentoParser parserFormulario = new DefinicionDocumentoParser();
    private final DocumentoTextoParser parserTexto = new DocumentoTextoParser();

    public PdfGeneratorImplIText(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public byte[] generate(byte[] documentoXml, Map<String, Object> contexto, Idioma idioma) {
        Element raiz = raiz(documentoXml);
        return switch (raiz.getTagName()) {
            case FORMULARIO -> generarFormulario(raiz, contexto);
            case TEXTO -> generarTexto(raiz, contexto, idioma);
            default -> throw raizDesconocida(raiz);
        };
    }

    @Override
    public List<String> getExpresiones(byte[] documentoXml) {
        Element raiz = raiz(documentoXml);
        return switch (raiz.getTagName()) {
            case FORMULARIO -> parserFormulario.parse(raiz).expresiones();
            case TEXTO -> parserTexto.parse(raiz).expresiones();
            default -> throw raizDesconocida(raiz);
        };
    }

    private byte[] generarFormulario(Element raiz, Map<String, Object> contexto) {
        Documento documento = parserFormulario.parse(raiz);
        DocumentoVisible visible = new ResolutorVisibilidad(evaluator, contexto).resolver(documento);
        Valores valores = Valores.de(visible.expresionesDeValor(), evaluator, contexto);

        return DibujantePdf.dibujar(visible, valores);
    }

    private byte[] generarTexto(Element raiz, Map<String, Object> contexto, Idioma idioma) {
        DocumentoTexto documento = parserTexto.parse(raiz);
        DocumentoTextoVisible visible = new ResolutorPresencia(evaluator, contexto).resolver(documento);
        Valores valores = Valores.de(visible.expresionesDeValor(idioma), evaluator, contexto);

        return DibujanteDocumentoTexto.dibujar(visible, valores, idioma);
    }

    private static RuntimeException raizDesconocida(Element raiz) {
        return new RuntimeException("El elemento raíz debe ser <" + FORMULARIO + "> o <" + TEXTO + ">"
                + " y es <" + raiz.getTagName() + ">");
    }

    private static Element raiz(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml)).getDocumentElement();
        } catch (Exception ex) {
            throw new RuntimeException("El XML del documento no se puede parsear: " + ex.getMessage(), ex);
        }
    }
}
