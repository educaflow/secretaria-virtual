package com.educaflow.subsystem.firmas.controller;

import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Optional;

public class  TareaFirmaController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void firmarDocumentosConAutoFirma(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);
        TareaFirma tareaFirma = JpaRepository.of(TareaFirma.class).find(actionRequestHelper.getId());

        AutoFirma autofirma = (new AutoFirma(TareaFirma.class))
                .setRectangulo(new Rectangulo(tareaFirma.getX().floatValue(),tareaFirma.getY().floatValue(),tareaFirma.getWidth().floatValue(),tareaFirma.getHeight().floatValue()))
                .setPageNumber(tareaFirma.getPage())
                .setDni(tareaFirma.getFirmante().getDni());

        List<DocumentoFirma> documentosFirma = tareaFirma.getDocumentosFirma();
        for(int i=0;i<documentosFirma.size();i++) {
            autofirma.addSourceTargetField("documentosFirma[" + i + "].documentoOriginal","documentosFirma[" + i + "].documentoFirmado");
        }

        AutoFirma.sendToActionResponse(autofirma,actionResponse);
    }


    @CallMethod
    @Transactional
    public void marcarComoFirmada(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);

        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);

        TareaFirma tareaFirmaOriginal=actionRequestHelper.getOriginalModel();
        TareaFirma tareaFirma = actionRequestHelper.getModel(tareaFirmaService.allowPropertiesMarcarComoFirmada());

        tareaFirmaService.marcarComoFirmada(tareaFirma, tareaFirmaOriginal);

    }

    @CallMethod
    @Transactional
    public void marcarComoRechazada(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);

        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);

        TareaFirma tareaFirmaOriginal=actionRequestHelper.getOriginalModel();
        TareaFirma tareaFirma = actionRequestHelper.getModel(tareaFirmaService.allowPropertiesMarcarComoRechazada());

        tareaFirmaService.marcarComoRechazada(tareaFirma, tareaFirmaOriginal);

    }


    @CallMethod
    public void validarDocumentosFirmados(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);

        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        TareaFirma tareaFirma = actionRequestHelper.getModel(tareaFirmaService.allowPropertiesValidarDocumentosFirmados());
        Optional<BusinessMessages> validationResult = tareaFirmaService.validarDocumentosFirmados(tareaFirma);

        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }

    }




}
