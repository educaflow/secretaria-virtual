package com.educaflow.common.pdf;


import com.educaflow.common.pdf.impl.DocumentoPdfImplIText;


public class DocumentoPdfFactory {



    public static DocumentoPdf getDocumentoPdf(byte[] bytesPdf, String fileName) {


        return new DocumentoPdfImplIText(bytesPdf,fileName);

    }


    
}