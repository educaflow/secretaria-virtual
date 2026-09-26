package com.educaflow.base.infrastructure.pdf;


import com.educaflow.base.infrastructure.pdf.impl.DocumentoPdfImplIText;
import com.educaflow.base.infrastructure.pdf.impl.helper.ImagenPdfHelper;


public class DocumentoPdfFactory {



    public static DocumentoPdf getDocumentoPdf(byte[] bytesPdf, String fileName) {


        return new DocumentoPdfImplIText(bytesPdf,fileName);

    }

    /**
     * Indica si los bytes son una imagen que se puede convertir a PDF con {@link #getDocumentoPdfFromImagen}.
     */
    public static boolean isImagenSoportada(byte[] bytes) {
        return ImagenPdfHelper.isImagenSoportada(bytes);
    }

    /**
     * Crea un PDF de una única página A4 con la imagen entera, centrada, sin deformar y con margen para imprimir.
     * La página es A4 vertical, salvo si la imagen es apaisada, en cuyo caso es A4 horizontal.
     */
    public static DocumentoPdf getDocumentoPdfFromImagen(byte[] bytesImagen, String fileName) {
        byte[] bytesPdf = ImagenPdfHelper.imagenToPdfA4(bytesImagen);

        return new DocumentoPdfImplIText(bytesPdf,fileName);
    }


    
}