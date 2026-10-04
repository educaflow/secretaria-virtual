package com.educaflow.subsystem.pdfutilities.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver;
import com.educaflow.subsystem.pdfutilities.db.PdfUtilities;
import com.educaflow.subsystem.pdfutilities.service.PdfUtilitiesService;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Optional;

public class PdfUtilitiesServiceImpl extends DefaultModelService<PdfUtilities> implements PdfUtilitiesService {

    @Inject
    AlmacenClaveResolver almacenClaveResolver;

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

        for (int x = 0; x <= 500; x += 100) {
            for (int y = 0; y <= 700; y += 50) {
                CampoFirma campoFirma = new CampoFirma(new Rectangulo(x, y, 100, 20)).setNumeroPagina(numeroPagina).setMensaje(x + "," + y);
                documentoPdf = documentoPdf.firmar(almacenClaveResolver.getDummy(), campoFirma);
            }
        }

        return MetaFileHelper.createMetaFile(documentoPdf);
    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/



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


    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesGetPdfTodasPosicionesFirma() {
        return AllowProperties.createAllowProperties(Map.of(
                "pdf", Map.of(),
                "numeroPagina", Map.of()
        ));
    }

}
