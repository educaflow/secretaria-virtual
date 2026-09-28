package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Familia;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.MedidorTexto;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Las cuatro fuentes de <b>un</b> documento PDF, en la familia que ese documento use (un {@code PdfFont} no se puede compartir entre
 * documentos), incrustadas con Identity-H para que cualquier carácter de un valor tenga glifo.
 */
public final class Fuentes implements MedidorTexto {

    private final Map<Fuente, PdfFont> fuentes = new EnumMap<>(Fuente.class);

    public Fuentes(Familia familia) {
        for (Fuente fuente : Fuente.values()) {
            fuentes.put(fuente, PdfFontFactory.createFont(Assets.programa(familia, fuente), PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED));
        }
    }

    PdfFont de(Fuente fuente) {
        return fuentes.get(fuente);
    }

    @Override
    public double ancho(Fuente fuente, String texto, double tamanyo) {
        return de(fuente).getWidth(texto, (float) tamanyo);
    }
}
