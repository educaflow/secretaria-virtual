package com.educaflow.base.infrastructure.pdfgenerator;

import com.educaflow.base.infrastructure.evaluator.impl.EvaluatorImplGroovy;
import com.educaflow.base.infrastructure.pdfgenerator.impl.PdfGeneratorImplIText;

public final class PdfGeneratorFactory {

    private PdfGeneratorFactory() {
    }

    public static PdfGenerator getPdfGenerator() {
        return new PdfGeneratorImplIText(new EvaluatorImplGroovy());
    }
}
