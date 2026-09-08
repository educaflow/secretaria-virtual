package com.educaflow.subsystem.firmas.controller;

import com.axelor.auth.db.User;
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
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Optional;

public class  TareaFirmaController {

    /** Nombre del campo de vista en el que el firmante teclea la clave (ver views/Pendiente-TareaFirma.xml). */
    private static final String CAMPO_VISTA_CLAVE_CERTIFICADO = "claveCertificado";

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public String getSituacionFirma() {
        User usuarioAutenticado = SecurityUtil.getUser();
        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(usuarioAutenticado == null ? null : usuarioAutenticado.getDni());

        return situacionFirma.name();
    }

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

    @CallMethod
    @Transactional
    public void firmarEnServidor(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);

        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);

        TareaFirma tareaFirmaOriginal=actionRequestHelper.getOriginalModel();
        TareaFirma tareaFirma = actionRequestHelper.getModel(tareaFirmaService.allowPropertiesFirmarEnServidor());
        String claveCertificado = getClaveCertificado(actionRequestHelper);

        tareaFirmaService.firmarEnServidor(tareaFirma, tareaFirmaOriginal, claveCertificado);

    }

    @CallMethod
    public void validateFirmarEnServidor(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);

        ActionRequestHelper<TareaFirma> actionRequestHelper = new ActionRequestHelper(actionRequest, TareaFirma.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        TareaFirma tareaFirmaOriginal=actionRequestHelper.getOriginalModel();
        TareaFirma tareaFirma = actionRequestHelper.getModel(tareaFirmaService.allowPropertiesFirmarEnServidor());
        String claveCertificado = getClaveCertificado(actionRequestHelper);

        Optional<BusinessMessages> validationResult = tareaFirmaService.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal, claveCertificado);

        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }

    }

    /**
     * La clave de firma tecleada en el formulario (PIN del dispositivo o contraseña del fichero del
     * certificado), o {@code null} si no se ha tecleado ninguna.
     *
     * <p>Es el único sitio del servidor que conoce el nombre del campo de vista {@code claveCertificado}: no existe en
     * el modelo, así que no lo trae {@code getModel(...)} y se lee directamente del contexto de la petición, que
     * lleva todos los campos del formulario, sean o no de la entidad.
     */
    private static String getClaveCertificado(ActionRequestHelper<TareaFirma> actionRequestHelper) {
        Object claveCertificado = actionRequestHelper.getRequestData().get(CAMPO_VISTA_CLAVE_CERTIFICADO);

        return claveCertificado == null ? null : claveCertificado.toString();
    }

}
