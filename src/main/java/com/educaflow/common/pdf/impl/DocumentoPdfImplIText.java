package com.educaflow.common.pdf.impl;

import com.educaflow.common.criptografia.*;
import com.educaflow.common.criptografia.impl.helper.CriptografiaUtil;
import com.educaflow.common.pdf.*;
import com.educaflow.common.pdf.impl.helper.DocumentoPdfHelper;
import com.educaflow.common.pdf.impl.helper.PKCS11ExternalSignature;
import com.educaflow.common.pdf.impl.helper.PdfDocumentHelper;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.form.element.SignatureFieldAppearance;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.crypto.DigestAlgorithms;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSignature;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.signatures.SignatureUtil;
import com.itextpdf.signatures.SignerProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

public class DocumentoPdfImplIText implements DocumentoPdf {

    private final byte[] bytesPdf;
    protected final PdfDocument pdfDocument;
    private final String fileName;

    public DocumentoPdfImplIText(byte[] bytesPdf, String fileName) {
        this.bytesPdf = bytesPdf;
        this.fileName = fileName;
        this.pdfDocument = PdfDocumentHelper.getPdfDocument(this.bytesPdf);
    }

    @Override
    public byte[] getDatos() {
        return bytesPdf;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }
    
    @Override
    public int getNumeroPaginas() {
        return this.pdfDocument.getNumberOfPages();
    }    


    @Override
    public String toString() {
        return DocumentoPdfHelper.toString(this);
    }

    @Override
    public List<String> getNombreCamposFormulario() {
        List<String> fields = new ArrayList<>();

        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDocument, false);

        if (form != null) {
            Map<String, PdfFormField> PdfFormFields = form.getAllFormFields();

            for (Entry<String, PdfFormField> entry : PdfFormFields.entrySet()) {
                String name = entry.getKey();
                PdfFormField pdfFormField = entry.getValue();
                if (allowFormField(pdfFormField) == true) {
                    fields.add(name);
                    //System.out.println("Campo:-->" + name + "<----                tipo:" + pdfFormField.getFormType() );
                    if ((pdfFormField.getAppearanceStates() != null) && (pdfFormField.getAppearanceStates().length > 0)) {
                        //System.out.println("    Valores posibles:" + Arrays.toString(pdfFormField.getAppearanceStates()));
                    }
                }
            }
        }

        return fields;
    }
    
    @Override
    public List<ResultadoFirma> getFirmasPdf() {
        List<ResultadoFirma> resultadoFirmas = new ArrayList<>();

        SignatureUtil signatureUtil = new SignatureUtil(pdfDocument);

        List<String> signatureNames = signatureUtil.getSignatureNames();

        for (String signatureName : signatureNames) {
            PdfPKCS7 pkcs7 = signatureUtil.readSignatureData(signatureName);
            PdfSignature pdfSignature=signatureUtil.getSignature(signatureName);

            ResultadoFirma resultadoFirma=new ResultadoFirmaImpl(signatureName,pkcs7,pdfSignature);
            resultadoFirmas.add(resultadoFirma);
        }

        return resultadoFirmas;

    }


    @Override
    public DocumentoPdf setValorCamposFormularioAndFlatten(Map<String,String> valores) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();


            PdfDocument pdfDocumentNuevosValoresCampos = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(bytesPdf)),
                    new PdfWriter(byteArrayOutputStream)
            );


            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDocumentNuevosValoresCampos, false);

            if (form == null) {
                throw new RuntimeException("No existe ningun formulario en el pdf");
            }

            for(Entry<String, String> entry:valores.entrySet()) {
                String nombre=entry.getKey();
                String valor=entry.getValue();

                PdfFormField pdfFormField = form.getField(nombre);
                if (pdfFormField == null) {
                    throw new RuntimeException("No existe en el formulario el campo:" + pdfFormField);
                }

                pdfFormField.setValue(valor);            

            }
            
            form.flattenFields();
            
            pdfDocumentNuevosValoresCampos.close();
            byteArrayOutputStream.close();
            
            return DocumentoPdfFactory.getDocumentoPdf(byteArrayOutputStream.toByteArray(),this.fileName);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }


    }


    @Override
    public DocumentoPdf firmar(AlmacenClave almacenClave, CampoFirma campoFirma) {
        try {
            String alias;
            Certificate[] chain = null;
            PrivateKey privateKey = null;
            int slot = 0;

            if (almacenClave instanceof AlmacenClaveFichero) {
                AlmacenClaveFichero almacenClaveFichero = (AlmacenClaveFichero) almacenClave;
                InputStream fileCertificate = almacenClaveFichero.getFileCertificate();
                String password = almacenClaveFichero.getPassword();

                KeyStore userKeyStore = CriptografiaUtil.getKeyStore(fileCertificate, password, CriptografiaUtil.KeyStoreType.PKCS12);
                alias = userKeyStore.aliases().nextElement();
                privateKey = (PrivateKey) userKeyStore.getKey(alias, password.toCharArray());
                chain = userKeyStore.getCertificateChain(alias);
            } else if (almacenClave instanceof AlmacenClaveDispositivo) {
                AlmacenClaveDispositivo almacenClaveDispositivo = (AlmacenClaveDispositivo) almacenClave;
                slot = almacenClaveDispositivo.getSlot();
                alias = almacenClaveDispositivo.getAlias();

                privateKey = EntornoCriptografico.getDispositivoCriptografico(slot).getPrivateKey(alias);
                chain = EntornoCriptografico.getDispositivoCriptografico(slot).getCertificateChain(alias);
            } else {
                throw new RuntimeException("Almacen desconocido:" + almacenClave.getClass().getName());
            }

            SignerProperties signerProperties = getSignerProperties(campoFirma, (X509Certificate) chain[0], alias);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytesPdf);
            PdfReader pdfReader = new PdfReader(byteArrayInputStream);
            PdfWriter pdfWriter = new PdfWriter(byteArrayOutputStream, new WriterProperties());
            PdfSigner signer = new PdfSigner(pdfReader, pdfWriter, new StampingProperties().useAppendMode());
            if (signerProperties != null) {
                signer.setSignerProperties(signerProperties);
            }


            if (almacenClave instanceof AlmacenClaveFichero) {
                IExternalDigest digest = new BouncyCastleDigest();
                IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, "BC");

                signer.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
            } else if (almacenClave instanceof AlmacenClaveDispositivo) {
                synchronized (EntornoCriptografico.getDispositivoCriptografico(slot)) {
                    IExternalDigest digest = new BouncyCastleDigest();
                    IExternalSignature pks = new PKCS11ExternalSignature(privateKey, DigestAlgorithms.SHA256, "RSA");

                    signer.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
                }
            } else {
                throw new RuntimeException("Almacen desconocido:" + almacenClave.getClass().getName());
            }

            pdfReader.close();
            byteArrayOutputStream.close();
            return DocumentoPdfFactory.getDocumentoPdf(byteArrayOutputStream.toByteArray(), this.fileName);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }


    @Override
    public DocumentoPdf anyadirDocumentoPdf(DocumentoPdf documentoPdf2) {
        return anyadirDocumentoPdf(documentoPdf2, this.fileName);
    }

    @Override
    public DocumentoPdf anyadirDocumentoPdf(DocumentoPdf documentoPdf2, String fileName) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfDocument pdfDestino = new PdfDocument(new PdfWriter(byteArrayOutputStream));
            PdfMerger merger = new PdfMerger(pdfDestino);
            merger.merge(this.pdfDocument, 1, this.pdfDocument.getNumberOfPages());
            merger.merge(DocumentoPdfHelper.getPdfDocument(documentoPdf2), 1, DocumentoPdfHelper.getPdfDocument(documentoPdf2).getNumberOfPages());

            merger.close();
            byteArrayOutputStream.close();

            return DocumentoPdfFactory.getDocumentoPdf(byteArrayOutputStream.toByteArray(), fileName);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**********************************************************************************/
    /*********************************** Utilidades ***********************************/
    /**********************************************************************************/



    private boolean allowFormField(PdfFormField pdfFormField) {
        if (PdfDocumentHelper.isSignatureFormField(pdfFormField)) {
            return false;
        } else if (pdfFormField.getFormType() == null) {
            return false;
        } else {
            return true;
        }
    }



    private String getSignatureFieldName() {
        List<ResultadoFirma> resultadoFirmas = this.getFirmasPdf();

        for (int i = 1; i < 100; i++) {
            final String signatureFieldName = "Signature" + Integer.toString(i);

            boolean existeCampoFirma = resultadoFirmas.stream().anyMatch(resultadoFirma -> signatureFieldName.equals(resultadoFirma.getNombreCampo()));

            if (existeCampoFirma == false) {
                return signatureFieldName;
            }

        }

        throw new RuntimeException("No se encontró ningun campo donde firmar , están todos usados");
    }




    private SignerProperties getSignerProperties(CampoFirma campoFirma, X509Certificate cert, String alias) {
        if (campoFirma == null) {
            return null;
        }
        SignatureFieldAppearance signatureFieldAppearance = getSignatureFieldAppearance(campoFirma, cert, alias);

        SignerProperties signerProperties = new SignerProperties();
        signerProperties.setFieldName(getSignatureFieldName());
        signerProperties.setPageRect(getRectangle(campoFirma));
        signerProperties.setPageNumber(campoFirma.getNumeroPagina());
        signerProperties.setSignatureAppearance(signatureFieldAppearance);
        signerProperties.setClaimedSignDate(toCalendar(campoFirma.getFechaFirma()));
        if (campoFirma.getMotivo()!=null) {
            signerProperties.setReason(campoFirma.getMotivo());
        }

        return signerProperties;
    }

    private SignatureFieldAppearance getSignatureFieldAppearance(CampoFirma campoFirma, X509Certificate cert, String alias) {
        try {
            String message;
            if (campoFirma.getMensaje() == null) {
                message = getMensajeFirma(cert, alias, campoFirma.getFechaFirma());
            } else {
                message = campoFirma.getMensaje();
            }

            SignatureFieldAppearance signatureFieldAppearance = new SignatureFieldAppearance(SignerProperties.IGNORED_ID);
            if (campoFirma.getImage() == null) {
                signatureFieldAppearance.setContent(message);
            } else {
                ImageData imageData = ImageDataFactory.create(campoFirma.getImage());
                signatureFieldAppearance.setContent(message, imageData);
            }
            signatureFieldAppearance.setFontSize(campoFirma.getFontSize());
            PdfFont courier = PdfFontFactory.createFont(StandardFonts.COURIER);
            signatureFieldAppearance.setFont(courier);

            return signatureFieldAppearance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calcula el rectángulo que ocupa la firma en función del rectángulo del mensaje y de la imagen
     * Añade al rectangulo del texto el alto de la imagen y el ancho es el máximo entre el del texto y el de la imagen
     * @param campoFirma
     * @return
     */
    private Rectangle getRectangle(CampoFirma campoFirma) {
        float width;
        float height;

        float mensajeX = campoFirma.getRectanguloMensaje().getX();
        float mensajeY = campoFirma.getRectanguloMensaje().getY();
        float mensajeWidth = campoFirma.getRectanguloMensaje().getWidth();
        float mensajeHeight = campoFirma.getRectanguloMensaje().getHeight();

        if (campoFirma.getImage() != null) {
            ImageData imageData = ImageDataFactory.create(campoFirma.getImage());
            float imageWidth = imageData.getWidth();
            float imageHeight = imageData.getHeight();

            width = Math.max(mensajeWidth, imageWidth);
            height = mensajeHeight + imageHeight;
        } else {
            width = mensajeWidth;
            height = mensajeHeight;
        }

        return new Rectangle(mensajeX, mensajeY, width, height);
    }


    /***********************************************************************************/
    /******************************* Mensaje de la firma *******************************/
    /***********************************************************************************/
    private static String getMensajeFirma(X509Certificate cert, String alias, LocalDateTime localDateTime) {
        try {
            String mensaje;


            DatosCertificado datosCertificado = EntornoCriptografico.getDatosCertificado(cert);
            mensaje = "Firmado por " + datosCertificado.getCnSubject() + " el dia " + getStringDateForMensajeFirma(localDateTime) + " con un certificado emitido por " + datosCertificado.getCnIssuer();


            return mensaje;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /************************************************************************************/
    /************************************ Date Utils ************************************/
    /************************************************************************************/

    private static String getStringDateForMensajeFirma(LocalDateTime localDateTime) {
        String fecha = localDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return fecha;
    }

    private static Calendar toCalendar(LocalDateTime fecha) {
        return GregorianCalendar.from(fecha.atZone(TimeZone.getDefault().toZoneId()));
    }

}
