package com.educaflow.subsystem.firmas.controller;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TareaFirmaController {

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
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            TareaFirma tareaFirmaOriginal=actionRequestHelper.getOriginalModel();
            AllowProperties allowProperties = AllowProperties.createAllowProperties(Map.of("documentosFirma", Map.of("documentoFirmado", Map.of())));
            TareaFirma tareaFirma = actionRequestHelper.getModel(allowProperties);

            tareaFirmaService.marcarComoFirmada(tareaFirma, tareaFirmaOriginal);

            actionResponse.setSignal("force-back", null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @CallMethod
    @Transactional
    public void marcarComoRechazada(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);

        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        TareaFirma tareaFirmaOriginal=actionRequestHelper.getOriginalModel();
        AllowProperties allowProperties = AllowProperties.createAllowProperties(Map.of("motivoRechazo", Map.of()));
        TareaFirma tareaFirma = actionRequestHelper.getModel(allowProperties);

        tareaFirmaService.marcarComoRechazada(tareaFirma, tareaFirmaOriginal);

        actionResponse.setSignal("force-back", null);
    }


    @CallMethod
    public void validarDocumentosFirmados(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);

        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowProperties(Map.of("documentosFirma", Map.of("documentoFirmado", Map.of())));
        TareaFirma tareaFirma = actionRequestHelper.getModel(allowProperties);
        Optional<BusinessMessages> validationResult = tareaFirmaService.validarDocumentosFirmados(tareaFirma);

        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }

    }

}
