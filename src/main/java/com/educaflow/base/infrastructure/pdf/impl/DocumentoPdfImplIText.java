package com.educaflow.base.infrastructure.pdf.impl;

import com.educaflow.base.util.Convert;
import com.educaflow.base.infrastructure.criptografia.*;
import com.educaflow.base.infrastructure.criptografia.impl.helper.CriptografiaUtil;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdf.ResultadoFirma;
import com.educaflow.base.infrastructure.pdf.*;
import com.educaflow.base.infrastructure.pdf.impl.helper.DocumentoPdfHelper;
import com.educaflow.base.infrastructure.pdf.impl.helper.PKCS11ExternalSignature;
import com.educaflow.base.infrastructure.pdf.impl.helper.PdfDocumentHelper;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfTextFormField;
import com.itextpdf.forms.fields.TextFormFieldBuilder;
import com.itextpdf.forms.form.element.SignatureFieldAppearance;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.crypto.DigestAlgorithms;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfWidgetAnnotation;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentoPdfImplIText implements DocumentoPdf {

    private static final Logger log = LoggerFactory.getLogger(DocumentoPdfImplIText.class);

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
    public Rectangulo getTamanyoPagina(int numeroPagina) {
        Rectangle pageSize = this.pdfDocument.getPage(getPageNumber(numeroPagina)).getPageSizeWithRotation();

        return new Rectangulo(pageSize.getX(), pageSize.getY(), pageSize.getWidth(), pageSize.getHeight());
    }


    @Override
    public String getMetadato(String nombre) {
        return this.pdfDocument.getDocumentInfo().getMoreInfo(nombre);
    }

    @Override
    public String toString() {
        return DocumentoPdfHelper.toString(this);
    }

    @Override
    public List<String> getNombreCamposFormulario() {
        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDocument, false);

        if (form == null) {
            return List.of();
        }

        return form.getAllFormFields().entrySet().stream()
                .filter(entry -> allowFormField(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }
    
    @Override
    public List<String> getNombreCamposFirmaVacios() {
        return new SignatureUtil(pdfDocument).getBlankSignatureNames();
    }

    @Override
    public List<ResultadoFirma> getFirmasPdf() {
        SignatureUtil signatureUtil = new SignatureUtil(pdfDocument);

        return signatureUtil.getSignatureNames().stream()
                .<ResultadoFirma>map(signatureName -> new ResultadoFirmaImpl(signatureName,
                        signatureUtil.readSignatureData(signatureName), signatureUtil.getSignature(signatureName),
                        signatureUtil.signatureCoversWholeDocument(signatureName)))
                .toList();
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

            Map<String, PdfFormField> pdfFormFields = form.getAllFormFields();

            for (Entry<String, PdfFormField> entry : pdfFormFields.entrySet()) {
                String nombre = entry.getKey();
                PdfFormField pdfFormField = entry.getValue();
                if (allowFormField(pdfFormField) == true) {
                    String valor = valores.get(nombre);
                    if (valor == null) {
                        valor="";
                        log.warn("No se ha proporcionado valor para el campo '{}'. Se establecerá un valor vacío.", nombre);
                    }

                    pdfFormField.setValue(valor);
                }
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
        Objects.requireNonNull(campoFirma, "campoFirma no puede ser null");
        Objects.requireNonNull(almacenClave, "almacenClave no puede ser null");
        try {
            String alias;
            Certificate[] chain = null;
            PrivateKey privateKey = null;

            if (almacenClave instanceof AlmacenClaveFichero almacenClaveFichero) {
                InputStream fileCertificate = almacenClaveFichero.getFileCertificate();
                String password = almacenClaveFichero.getPassword();

                KeyStore userKeyStore = CriptografiaUtil.getKeyStore(fileCertificate, password);
                // La primera entrada puede ser solo un certificado: se firma con la primera que tiene clave privada.
                alias = null;
                for (String candidato : Collections.list(userKeyStore.aliases())) {
                    if (userKeyStore.isKeyEntry(candidato)) {
                        alias = candidato;
                        break;
                    }
                }
                if (alias == null) {
                    throw new RuntimeException("El almacén de claves no contiene ninguna clave privada");
                }
                privateKey = (PrivateKey) userKeyStore.getKey(alias, password.toCharArray());
                chain = userKeyStore.getCertificateChain(alias);
            } else if (almacenClave instanceof AlmacenClaveDispositivo almacenClaveDispositivo) {
                alias = almacenClaveDispositivo.getAlias();

                DispositivoCriptografico dispositivo = EntornoCriptografico.getDispositivoCriptografico(almacenClaveDispositivo.getSlot());
                privateKey = dispositivo.getPrivateKey(alias);
                chain = dispositivo.getCertificateChain(alias);
            } else {
                throw new RuntimeException("Almacen desconocido:" + almacenClave.getClass().getName());
            }

            SignerProperties signerProperties = getSignerProperties(campoFirma, (X509Certificate) chain[0], alias);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytesPdf);
            PdfReader pdfReader = new PdfReader(byteArrayInputStream);
            PdfWriter pdfWriter = new PdfWriter(byteArrayOutputStream, new WriterProperties());
            PdfSigner signer = new PdfSigner(pdfReader, pdfWriter, new StampingProperties().useAppendMode());
            signer.setSignerProperties(signerProperties);


            if (almacenClave instanceof AlmacenClaveFichero) {
                IExternalDigest digest = new BouncyCastleDigest();
                IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, "BC");

                signer.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
            } else if (almacenClave instanceof AlmacenClaveDispositivo almacenClaveDispositivo) {
                int slot=almacenClaveDispositivo.getSlot();
                DispositivoCriptografico dispositivo = EntornoCriptografico.getDispositivoCriptografico(slot);
                synchronized (dispositivo) {
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
    public DocumentoPdf setMetadato(String nombre, String valor) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocumentConMetadato = new PdfDocument(new PdfReader(new ByteArrayInputStream(this.bytesPdf)), new PdfWriter(byteArrayOutputStream), new StampingProperties().useAppendMode());
            pdfDocumentConMetadato.getDocumentInfo().setMoreInfo(nombre, valor);
            pdfDocumentConMetadato.close();
            byteArrayOutputStream.close();

            return DocumentoPdfFactory.getDocumentoPdf(byteArrayOutputStream.toByteArray(), this.fileName);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public DocumentoPdf estamparTextoConAppend(String texto, int numeroPagina, Rectangulo rectangulo) {


        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            PdfReader reader = new PdfReader(new ByteArrayInputStream(this.bytesPdf));
            PdfWriter writer = new PdfWriter(baos);
            StampingProperties properties = new StampingProperties().useAppendMode();
            PdfDocument pdfDoc = new PdfDocument(reader, writer, properties);

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            String fieldName = "TXT_INCREMENTAL_" + UUID.randomUUID();
            TextFormFieldBuilder builder = new TextFormFieldBuilder(pdfDoc, fieldName);

            PdfTextFormField field = builder.setWidgetRectangle(new Rectangle(rectangulo.x(), rectangulo.y(), rectangulo.width(), rectangulo.height())).setPage(pdfDoc.getPage(getPageNumber(numeroPagina))).createText();

            field.setValue(texto);
            field.setReadOnly(true);
            field.setFont(font);
            field.setFontSize(7);
            field.setColor(ColorConstants.BLACK);


            PdfWidgetAnnotation widget = field.getWidgets().get(0);
            widget.setBorder(new PdfArray(new float[]{0, 0, 0}));
            widget.getPdfObject().remove(PdfName.MK);


            form.addField(field, pdfDoc.getPage(getPageNumber(numeroPagina)));

            pdfDoc.close();

            return DocumentoPdfFactory.getDocumentoPdf(baos.toByteArray(), this.fileName);

        } catch (Exception e) {
            throw new RuntimeException("Error al añadir texto incremental: " + e.getMessage(), e);
        }
    }


    @Override
    public DocumentoPdf addNewPage() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfDocument pdfDestino = new PdfDocument(new PdfReader(new ByteArrayInputStream(this.bytesPdf)), new PdfWriter(byteArrayOutputStream));
            pdfDestino.addNewPage();
            pdfDestino.close();
            byteArrayOutputStream.close();
            return DocumentoPdfFactory.getDocumentoPdf(byteArrayOutputStream.toByteArray(), this.fileName);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public DocumentoPdf removePdfAConformance() {
        return DocumentoPdfFactory.getDocumentoPdf(PdfDocumentHelper.removePdfAConformance(this.bytesPdf), this.fileName);
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
            registrarCamposFirmaVacios(pdfDestino);

            merger.close();
            byteArrayOutputStream.close();

            return DocumentoPdfFactory.getDocumentoPdf(byteArrayOutputStream.toByteArray(), fileName);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getPlainText() {
        StringBuilder plainText = new StringBuilder();

        int numberOfPages = this.pdfDocument.getNumberOfPages();

        for (int i = 1; i <= numberOfPages; i++) {
            String pageText = PdfTextExtractor.getTextFromPage(
                    this.pdfDocument.getPage(i),
                    new SimpleTextExtractionStrategy()
            );
            plainText.append(pageText);
            plainText.append("\n");
        }


        return plainText.toString();
    }


    /**********************************************************************************/
    /*********************************** Utilidades ***********************************/
    /**********************************************************************************/



    /**
     * Al unir documentos, PdfMerger copia el widget de cada campo con su página pero no lo da de alta en el
     * formulario del PDF de destino. Aquí se dan de alta solo los campos de firma vacíos, para que se pueda
     * seguir firmando en ellos por su nombre. Los ya firmados se quedan como estaban (solo su dibujo): su
     * firma era del documento de origen y en el unido ya no vale.
     */
    private static void registrarCamposFirmaVacios(PdfDocument pdfDocument) {
        for (int numeroPagina = 1; numeroPagina <= pdfDocument.getNumberOfPages(); numeroPagina++) {
            for (PdfAnnotation pdfAnnotation : pdfDocument.getPage(numeroPagina).getAnnotations()) {
                if (isCampoFirmaVacio(pdfAnnotation.getPdfObject())) {
                    PdfAcroForm.getAcroForm(pdfDocument, true).addField(PdfFormField.makeFormField(pdfAnnotation.getPdfObject(), pdfDocument), null);
                }
            }
        }
    }

    private static boolean isCampoFirmaVacio(PdfDictionary pdfDictionary) {
        return PdfName.Widget.equals(pdfDictionary.getAsName(PdfName.Subtype))
                && PdfName.Sig.equals(pdfDictionary.getAsName(PdfName.FT))
                && pdfDictionary.containsKey(PdfName.V) == false;
    }

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




    /**
     * Comprueba que el PDF tiene un campo de firma vacío con ese nombre. Sin esta comprobación iText
     * crearía él mismo un campo con ese nombre, invisible, y el documento saldría firmado sin que se vea dónde.
     */
    private String getCampoFirmaVacio(String nombreCampo) {
        List<String> camposFirmaVacios = getNombreCamposFirmaVacios();

        if (camposFirmaVacios.contains(nombreCampo) == false) {
            throw new RuntimeException("El PDF no tiene ningún campo de firma vacío que se llame '" + nombreCampo + "'. Los campos de firma vacíos que tiene son: " + camposFirmaVacios);
        }

        return nombreCampo;
    }

    private SignerProperties getSignerProperties(CampoFirma campoFirma, X509Certificate cert, String alias) {
        SignatureFieldAppearance signatureFieldAppearance = getSignatureFieldAppearance(campoFirma, cert, alias);

        SignerProperties signerProperties = new SignerProperties();
        if (campoFirma.getNombreCampo() != null) {
            // el campo ya existe: iText firma en su página y en su recuadro
            signerProperties.setFieldName(getCampoFirmaVacio(campoFirma.getNombreCampo()));
        } else {
            signerProperties.setFieldName(getSignatureFieldName());
            signerProperties.setPageRect(getRectangle(campoFirma));
            int pageNumber= getPageNumber(campoFirma.getNumeroPagina());
            signerProperties.setPageNumber(pageNumber);
        }
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
                signatureFieldAppearance.setContent(getContenidoConImagen(campoFirma, message));
            }
            signatureFieldAppearance.setFontSize(campoFirma.getFontSize());
            signatureFieldAppearance.setHorizontalAlignment(HorizontalAlignment.LEFT);
            signatureFieldAppearance.setProperty(Property.VERTICAL_ALIGNMENT, VerticalAlignment.BOTTOM);
            PdfFont courier = PdfFontFactory.createFont(StandardFonts.COURIER);
            signatureFieldAppearance.setFont(courier);

            return signatureFieldAppearance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * El mensaje y la imagen, cada uno en su sitio. Al darle a iText un Div ya no reparte él el recuadro
     * (a medias y con la imagen siempre arriba o a la izquierda) ni ajusta nada al hueco: el mensaje ocupa
     * lo que mide con el tamaño de letra del campo y la imagen se escala a lo que queda libre.
     */
    private Div getContenidoConImagen(CampoFirma campoFirma, String message) {
        Rectangle recuadro = getRecuadroFirma(campoFirma);
        float width = recuadro.getWidth() - 2 * PADDING_CAMPO_FIRMA;
        float height = recuadro.getHeight() - 2 * PADDING_CAMPO_FIRMA;
        Paragraph mensaje = new Paragraph(message).setMargin(0).setMultipliedLeading(1);
        Image imagen = new Image(ImageDataFactory.create(campoFirma.getImage()));

        return switch (campoFirma.getPosicionImagen()) {
            case ARRIBA -> new Div().add(scaleToFit(imagen, width, height - getAltoMensaje(campoFirma, message))).add(mensaje);
            case ABAJO -> new Div().add(mensaje).add(scaleToFit(imagen, width, height - getAltoMensaje(campoFirma, message)));
            case IZQUIERDA -> {
                float widthImagen = scaleToFit(imagen, width / 2, height).getImageScaledWidth();
                yield new Div().add(getFila(widthImagen, width - widthImagen).addCell(getCelda().add(imagen)).addCell(getCelda().add(mensaje)));
            }
            case DERECHA -> {
                float widthImagen = scaleToFit(imagen, width / 2, height).getImageScaledWidth();
                yield new Div().add(getFila(width - widthImagen, widthImagen).addCell(getCelda().add(mensaje)).addCell(getCelda().add(imagen)));
            }
        };
    }

    /** El mismo relleno que iText le pone por defecto a la apariencia de un campo de firma. */
    private static final float PADDING_CAMPO_FIRMA = 2;

    private static Image scaleToFit(Image imagen, float width, float height) {
        if ((width <= 0) || (height <= 0)) {
            throw new IllegalArgumentException("En el recuadro de la firma no cabe la imagen: le queda un hueco de " + width + "x" + height);
        }

        return imagen.scaleToFit(width, height);
    }

    /**
     * Lo que ocupa de alto el mensaje, con un punto de holgura para que la imagen no apure el recuadro. Solo
     * cuentan los saltos de línea que trae: una línea que no quepa a lo ancho se parte y se sale del recuadro.
     */
    private static float getAltoMensaje(CampoFirma campoFirma, String message) {
        return message.split("\\n", -1).length * campoFirma.getFontSize() + 1;
    }

    /** La imagen y el mensaje uno al lado del otro, cada uno en su celda. */
    private static Table getFila(float widthIzquierda, float widthDerecha) {
        return new Table(new float[]{widthIzquierda, widthDerecha}).setFixedLayout().setWidth(widthIzquierda + widthDerecha);
    }

    private static Cell getCelda() {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0);
    }

    /**
     * @return el recuadro que ocupa la firma en la página: el del campo de firma, si se firma en uno que ya existe, o el que se indicó
     */
    private Rectangle getRecuadroFirma(CampoFirma campoFirma) {
        if (campoFirma.getNombreCampo() != null) {
            String nombreCampo = getCampoFirmaVacio(campoFirma.getNombreCampo());

            return PdfAcroForm.getAcroForm(pdfDocument, false).getField(nombreCampo).getWidgets().get(0).getRectangle().toRectangle();
        }

        return getRectangle(campoFirma);
    }

    private Rectangle getRectangle(CampoFirma campoFirma) {
        Rectangulo rectangulo = campoFirma.getRectanguloMensaje();

        return new Rectangle(rectangulo.x(), rectangulo.y(), rectangulo.width(), rectangulo.height());
    }

    /**
     * Esta función existe porque permite números de página negativos y entonces empieza por el final
     * @param numeroPagina No se permite el cero
     * @return
     */
    private int getPageNumber(int numeroPagina) {
        int numeroPaginas=this.getNumeroPaginas();
        int realNumeroPagina;

        if (numeroPagina == 0) {
            throw new IllegalArgumentException("El numero de pagina no puede ser 0");
        }

        if (numeroPagina < 0) {
            realNumeroPagina=this.getNumeroPaginas() + numeroPagina + 1;
        } else {
            realNumeroPagina=numeroPagina;
        }

        if ((realNumeroPagina < 1) || (realNumeroPagina > numeroPaginas)) {
            throw new IllegalArgumentException("El numero de pagina es incorrecto. El documento tiene " + numeroPaginas + " paginas y se ha indicado el numero de pagina " + realNumeroPagina);
        }


        return realNumeroPagina;
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
        return GregorianCalendar.from(fecha.atZone(Convert.defaultZoneId));
    }

}
