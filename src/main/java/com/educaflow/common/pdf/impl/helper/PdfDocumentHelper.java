package com.educaflow.common.pdf.impl.helper;

import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfSignatureFormField;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.xmp.XMPConst;
import com.itextpdf.pdfa.PdfADocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Base64;

/**
 *
 * @author logongas
 */
public class PdfDocumentHelper {


    public static PdfDocument getPdfDocument(byte[] bytesPdf) {
        try {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(bytesPdf));
            PdfDocument pdfDocumentNuevo = new PdfDocument(reader);

            return pdfDocumentNuevo;
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el PDF", e);
        }
    }



    public static byte[] transformPdfDocument(byte[] bytesPdf, PdfDocumentTransform pdfDocumentTransform) {
        try {

            if (pdfDocumentTransform == null) {
                return bytesPdf;
            }

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytesPdf);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();


            PdfReader reader = new PdfReader(byteArrayInputStream);
            PdfWriter writer = new PdfWriter(byteArrayOutputStream);

            PdfDocument pdfDocument = new PdfDocument(reader, writer);

            pdfDocumentTransform.doTransform(pdfDocument);

            pdfDocument.close();
            byteArrayOutputStream.close();

            return byteArrayOutputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public static PdfConformance getPdfConformance(PdfDocument pdfDocument) {
        if ((pdfDocument instanceof PdfADocument) == false) {
            return null;
        }

        PdfADocument pdfADocument = (PdfADocument) pdfDocument;

        return pdfADocument.getConformance();
    }

    public static void setPdfConformance(PdfDocument pdfDocument, PdfConformance pdfConformance) {
        try {
            Class<?> clazz = pdfDocument.getClass();
            Field field = null;

            // Buscar en la jerarquía de clases hasta encontrar el campo
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField("pdfConformance");
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass(); // seguimos buscando en la superclase
                }
            }

            if (field == null) {
                throw new RuntimeException("Campo 'conformance' no encontrado en la jerarquía de clases.");
            }

            field.setAccessible(true);
            field.set(pdfDocument, pdfConformance);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public static boolean hasOutputIntent(PdfDocument pdfDocument) {
        PdfArray outputIntents = pdfDocument.getCatalog().getPdfObject().getAsArray(PdfName.OutputIntents);
        if (outputIntents == null) {
            return false;
        } else {
            return true;
        }
    }


    public static PdfOutputIntent getPdfOutputIntentStandardRedGreenBlue() {
        InputStream iccStandardRedGreenBlue = IccProfile.getStandardRedGreenBlue();

        PdfOutputIntent outputIntent = new PdfOutputIntent(
                "sRGB IEC61966-2.1",
                "sRGB IEC61966-2.1",
                "http://www.color.org",
                "Perfil de color sRGB",
                iccStandardRedGreenBlue
        );

        return outputIntent;
    }

    public static boolean isSignatureFormField(PdfFormField pdfFormField) {
        return pdfFormField instanceof PdfSignatureFormField;
    }

    public static byte[] removePdfAConformance(byte[] datos) {
        byte[] datosNoPdfAConformance=PdfDocumentHelper.transformPdfDocument(datos, pdfDocument -> {
            pdfDocument.getXmpMetadata().deleteProperty(XMPConst.NS_PDFA_ID, XMPConst.PART);
            pdfDocument.getXmpMetadata().deleteProperty(XMPConst.NS_PDFA_ID, XMPConst.CONFORMANCE);
            PdfDocumentHelper.setPdfConformance(pdfDocument, null);
        });

        return datosNoPdfAConformance;
    }
}

