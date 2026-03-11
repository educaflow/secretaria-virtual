package com.educaflow.base.infrastructure.pdf.impl.helper;

import com.itextpdf.kernel.pdf.PdfDocument;

@FunctionalInterface
public interface PdfDocumentTransform {
    void doTransform(PdfDocument pdfDocument) throws Exception;
}