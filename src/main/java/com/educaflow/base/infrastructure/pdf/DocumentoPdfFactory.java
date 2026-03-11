package com.educaflow.base.infrastructure.pdf;


import com.educaflow.base.infrastructure.pdf.impl.DocumentoPdfImplIText;


public class DocumentoPdfFactory {



    public static DocumentoPdf getDocumentoPdf(byte[] bytesPdf, String fileName) {


        return new DocumentoPdfImplIText(bytesPdf,fileName);

    }


    
}