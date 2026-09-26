package com.educaflow.base.infrastructure.pdf.impl.helper;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import java.io.ByteArrayOutputStream;

public class ImagenPdfHelper {

    /**
     * Margen que se deja en cada lado de la página para que se pueda imprimir: 1 cm en puntos PDF (1/72 de pulgada).
     */
    public static final float MARGEN_PUNTOS = 10 / 25.4f * 72;

    /**
     * Indica si los bytes son una imagen que iText sabe leer (GIF, JPEG, JPEG2000, PNG, BMP, TIFF o JBIG2).
     * Se detecta por la cabecera del fichero, no por su extensión ni su tipo MIME.
     */
    public static boolean isImagenSoportada(byte[] bytes) {
        return ImageDataFactory.isSupportedType(bytes);
    }

    /**
     * Genera un PDF de una única página A4 con la imagen entera, centrada, sin deformar y dejando {@link #MARGEN_PUNTOS} de margen.
     * La página es A4 vertical, salvo si la imagen es apaisada, en cuyo caso es A4 horizontal. La imagen nunca se gira.
     */
    public static byte[] imagenToPdfA4(byte[] bytesImagen) {
        try {
            ImageData imageData = ImageDataFactory.create(bytesImagen);
            float anchoImagen = imageData.getWidth();
            float altoImagen = imageData.getHeight();

            boolean apaisada = anchoImagen > altoImagen;
            PageSize pageSize = apaisada ? PageSize.A4.rotate() : PageSize.A4;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(new PdfWriter(byteArrayOutputStream));
            PdfPage pdfPage = pdfDocument.addNewPage(pageSize);

            float anchoDisponible = pageSize.getWidth() - 2 * MARGEN_PUNTOS;
            float altoDisponible = pageSize.getHeight() - 2 * MARGEN_PUNTOS;
            float escala = Math.min(anchoDisponible / anchoImagen, altoDisponible / altoImagen);
            float ancho = anchoImagen * escala;
            float alto = altoImagen * escala;
            float x = (pageSize.getWidth() - ancho) / 2;
            float y = (pageSize.getHeight() - alto) / 2;

            new PdfCanvas(pdfPage).addImageWithTransformationMatrix(imageData, ancho, 0, 0, alto, x, y);

            pdfDocument.close();

            return byteArrayOutputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error al generar el PDF a partir de la imagen", ex);
        }
    }
}
