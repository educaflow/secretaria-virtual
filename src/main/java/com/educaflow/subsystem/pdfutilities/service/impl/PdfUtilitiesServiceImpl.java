package com.educaflow.subsystem.pdfutilities.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.subsystem.pdfutilities.db.PdfUtilities;
import com.educaflow.subsystem.pdfutilities.service.PdfUtilitiesService;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class PdfUtilitiesServiceImpl extends DefaultModelService<PdfUtilities> implements PdfUtilitiesService {

    public PdfUtilitiesServiceImpl(Class<PdfUtilities> model, Repository<PdfUtilities> repository) {
        super(model, repository);
    }

    @Override
    public String getInfo(MetaFile pdf) {
        validateGetInfo(pdf).ifPresent(BusinessMessages::throwIfInvalid);

        if (pdf == null) {
            return "Sin información";
        }

        return MetaFileHelper.getDocumentoPdf(pdf).toString();
    }

    @Override
    public MetaFile getPdfTodasPosicionesFirma(PdfUtilities pdfUtilities) {
        validateGetPdfTodasPosicionesFirma(pdfUtilities).ifPresent(BusinessMessages::throwIfInvalid);

        MetaFile metaFilePdf = pdfUtilities.getPdf();
        if (metaFilePdf == null) {
            return null;
        }

        int numeroPagina = pdfUtilities.getNumeroPagina() <= 0 ? 1 : pdfUtilities.getNumeroPagina();

        DocumentoPdf documentoPdf = MetaFileHelper.getDocumentoPdf(metaFilePdf).removePdfAConformance();
        AlmacenClave almacenClaveDummy = crearAlmacenClaveDummy();

        for (int x = 0; x <= 500; x += 100) {
            for (int y = 0; y <= 700; y += 50) {
                CampoFirma campoFirma = new CampoFirma(new Rectangulo(x, y, 100, 20)).setNumeroPagina(numeroPagina).setMensaje(x + "," + y);
                documentoPdf = documentoPdf.firmar(almacenClaveDummy, campoFirma);
            }
        }

        return MetaFileHelper.createMetaFile(documentoPdf);
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateGetInfo(MetaFile pdf) {
        BusinessMessages messages = new BusinessMessages();

        validarPdf(pdf, messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateGetPdfTodasPosicionesFirma(PdfUtilities pdfUtilities) {
        BusinessMessages messages = new BusinessMessages();

        validarPdf(pdfUtilities.getPdf(), messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarPdf(MetaFile pdf, BusinessMessages messages) {
        if ((pdf != null) && (MetaFileHelper.isPdf(pdf) == false)) {
            messages.add(new BusinessMessage(I18n.get("El fichero no es un PDF.")));
        }
    }

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    @Override
    public AllowProperties allowPropertiesGetPdfTodasPosicionesFirma() {
        return AllowProperties.createAllowProperties(Map.of(
                "pdf", Map.of(),
                "numeroPagina", Map.of()
        ));
    }

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

    /**
     * Certificado autofirmado de usar y tirar, generado en memoria, para firmar los recuadros de las posiciones.
     * No se lee nada de él (cada recuadro lleva su propio mensaje) ni se valida su confianza, así que no tiene por
     * qué ser de nadie.
     */
    static AlmacenClave crearAlmacenClaveDummy() {
        try {
            String password = "dummy";

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            X500Name titular = new X500Name("CN=Dummy");
            Instant ahora = Instant.now();
            X509Certificate certificado = new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(
                    titular, BigInteger.valueOf(ahora.toEpochMilli()), Date.from(ahora.minus(Duration.ofDays(1))),
                    Date.from(ahora.plus(Duration.ofDays(1))), titular, keyPair.getPublic())
                    .build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("dummy", keyPair.getPrivate(), password.toCharArray(), new Certificate[]{certificado});
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            keyStore.store(byteArrayOutputStream, password.toCharArray());

            return new AlmacenClaveFichero(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), password);
        } catch (Exception ex) {
            throw new RuntimeException("No se puede generar el certificado dummy", ex);
        }
    }

}
