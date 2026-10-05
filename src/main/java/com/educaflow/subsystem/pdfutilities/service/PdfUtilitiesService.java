package com.educaflow.subsystem.pdfutilities.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.pdfutilities.db.PdfUtilities;

import java.util.Optional;

public interface PdfUtilitiesService extends ModelService<PdfUtilities> {

    String getInfo(MetaFile pdf);
    MetaFile getPdfTodasPosicionesFirma(PdfUtilities pdfUtilities);


    Optional<BusinessMessages> validateGetInfo(MetaFile pdf);
    Optional<BusinessMessages> validateGetPdfTodasPosicionesFirma(PdfUtilities pdfUtilities);


    AllowProperties allowPropertiesGetPdfTodasPosicionesFirma();

}
