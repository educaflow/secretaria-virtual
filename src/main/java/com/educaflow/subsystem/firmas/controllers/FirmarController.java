package com.educaflow.subsystem.firmas.controllers;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.*;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.db.repo.TareaFirmaRepository;
import com.educaflow.subsystem.firmas.service.FirmaService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Map;

public class FirmarController {

    @Inject
    TareaFirmaRepository tareaFirmaRepository;
    @Inject
    FirmaService firmaService;

    @CallMethod
    public void firmarDocumentosConAutoFirma(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);
        TareaFirma tareaFirma = tareaFirmaRepository.find(actionRequestHelper.getId());

        AutoFirma autofirma = (new AutoFirma(TareaFirma.class))
                .setRectangulo(new Rectangulo(tareaFirma.getX().floatValue(),tareaFirma.getY().floatValue(),tareaFirma.getWidth().floatValue(),tareaFirma.getHeight().floatValue()))
                .setPageNumber(tareaFirma.getPage())
                .setNif(tareaFirma.getFirmante().getDni());

        List<DocumentoFirma> documentosFirma = tareaFirma.getDocumentosFirma();
        for(int i=0;i<documentosFirma.size();i++) {
            autofirma.addSourceTargetField("documentosFirma[" + i + "].documentoOriginal","documentosFirma[" + i + "].documentoFirmado");
        }

        AutoFirma.sendToActionResponse(autofirma,actionResponse);
    }


    @CallMethod
    @Transactional
    public void marcarComoFirmada(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            TareaFirma tareaFirmaOriginal=actionRequestHelper.getOriginalModel();
            AllowProperties allowProperties = AllowProperties.createAllowProperties(Map.of("documentosFirma",Map.of("documentoFirmado",Map.of())));
            TareaFirma tareaFirma=actionRequestHelper.getModel(allowProperties);

            firmaService.marcarComoFirmada(tareaFirma,tareaFirmaOriginal);

            actionResponse.setSignal("back",null);
        } catch (BusinessException ex) {
            actionResponseHelper.doResponseBusinessMessages(ex.getBusinessMessages());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @CallMethod
    @Transactional
    public void marcarComoRechazada(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            TareaFirma tareaFirmaOriginal=actionRequestHelper.getOriginalModel();
            AllowProperties allowProperties = AllowProperties.createAllowProperties(Map.of("motivoRechazo",Map.of()));
            TareaFirma tareaFirma=actionRequestHelper.getModel(allowProperties);

            firmaService.marcarComoRechazada(tareaFirma,tareaFirmaOriginal);

            actionResponse.setSignal("back",null);
        } catch (BusinessException ex) {
            actionResponseHelper.doResponseBusinessMessages(ex.getBusinessMessages());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
