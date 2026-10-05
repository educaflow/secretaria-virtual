package com.educaflow.subsystem.pdfutilities.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.meta.db.MetaFile;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.pdfutilities.db.PdfUtilities;
import com.educaflow.subsystem.pdfutilities.service.PdfUtilitiesService;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.util.Optional;

public class PdfUtilitiesController {

    @Inject
    ModelServiceFactory modelServiceFactory;

    @CallMethod
    public String getInfo(MetaFile metaFilePdf) {
        final PdfUtilitiesService pdfUtilitiesService = (PdfUtilitiesService) modelServiceFactory.resolve(PdfUtilities.class);

        return pdfUtilitiesService.getInfo(metaFilePdf);
    }

    @CallMethod
    @Transactional
    public void getPdfTodasPosicionesFirma(ActionRequest actionRequest, ActionResponse actionResponse) {
        final PdfUtilitiesService pdfUtilitiesService = (PdfUtilitiesService) modelServiceFactory.resolve(PdfUtilities.class);

        ActionRequestHelper<PdfUtilities> actionRequestHelper = new ActionRequestHelper<>(actionRequest, PdfUtilities.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        PdfUtilities pdfUtilities = actionRequestHelper.getModel(pdfUtilitiesService.allowPropertiesGetPdfTodasPosicionesFirma());

        Optional<BusinessMessages> validationResult = pdfUtilitiesService.validateGetPdfTodasPosicionesFirma(pdfUtilities);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        actionResponse.setValue("pdfFirmado", pdfUtilitiesService.getPdfTodasPosicionesFirma(pdfUtilities));
    }

    @CallMethod
    public void pdfAutoFirma(ActionRequest actionRequest, ActionResponse actionResponse) {

        ActionRequestHelper requestHelper=new ActionRequestHelper(actionRequest,PdfUtilities.class);

        int x= Convert.coerceToInt(requestHelper.getRequestData().get("x"));
        int y=Convert.coerceToInt(requestHelper.getRequestData().get("y"));
        int width=Convert.coerceToInt(requestHelper.getRequestData().get("width"));
        int height=Convert.coerceToInt(requestHelper.getRequestData().get("height"));
        int numeroPagina = Convert.coerceToInt(requestHelper.getRequestData().get("numeroPagina"));

        if (width==0) {
            width=200;
        }

        if (height==0) {
            height=100;
        }

        if (numeroPagina <= 0) {
            numeroPagina = 1;
        }

        AutoFirma autofirma = new AutoFirma(PdfUtilities.class)
                .setRectangulo(new Rectangulo(x,y,width,height))
                .setPageNumber(numeroPagina)
                .addSourceTargetField("pdf","pdfFirmado")
                .setMotivo(x+","+y);

        AutoFirma.sendToActionResponse(autofirma,actionResponse);
    }

    /************************************************************************************/
    /***************************** Acciones de Validaciones *****************************/
    /************************************************************************************/

    @CallMethod
    public void validateGetInfo(ActionRequest actionRequest, ActionResponse actionResponse) {
        final PdfUtilitiesService pdfUtilitiesService = (PdfUtilitiesService) modelServiceFactory.resolve(PdfUtilities.class);

        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        // La acción es escalar (getInfo(pdf)): se lee el pdf del contexto, igual que lo recibe getInfo, sin getModel.
        MetaFile pdf = actionRequest.getContext().asType(PdfUtilities.class).getPdf();

        Optional<BusinessMessages> validationResult = pdfUtilitiesService.validateGetInfo(pdf);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

    @CallMethod
    public void validateGetPdfTodasPosicionesFirma(ActionRequest actionRequest, ActionResponse actionResponse) {
        final PdfUtilitiesService pdfUtilitiesService = (PdfUtilitiesService) modelServiceFactory.resolve(PdfUtilities.class);

        ActionRequestHelper<PdfUtilities> actionRequestHelper = new ActionRequestHelper<>(actionRequest, PdfUtilities.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        PdfUtilities pdfUtilities = actionRequestHelper.getModel(pdfUtilitiesService.allowPropertiesGetPdfTodasPosicionesFirma());

        Optional<BusinessMessages> validationResult = pdfUtilitiesService.validateGetPdfTodasPosicionesFirma(pdfUtilities);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/

}
