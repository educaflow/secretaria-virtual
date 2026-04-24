package com.educaflow.subsystem.importacion.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.meta.db.MetaFile;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ImportacionController {

    private final Logger logger = LoggerFactory.getLogger(ImportacionController.class);
    private static final String FIELD_IMPORT_LOG = "importLog";

    @Inject
    ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void importarDatos(ActionRequest request, ActionResponse response) {
        TareaImportacionService tareaImportacionService = (TareaImportacionService) modelServiceFactory.resolve(TareaImportacion.class);

        List<String> logs = new ArrayList<>();

        TareaImportacion tareaImportacion = getModelo(request, TareaImportacion.class);

        try {
            BusinessMessages businessMessages = tareaImportacionService.importar(tareaImportacion);
            for (BusinessMessage businessMessage: businessMessages) {
                logs.add(businessMessage.getMessage());
                logger.info("Mensaje de importación: {}", businessMessage.getMessage());
            }
            response.setValue(FIELD_IMPORT_LOG, String.join(System.lineSeparator(), logs));
            response.setValue("centro", tareaImportacion.getCentro());
            response.setValue("curso", tareaImportacion.getCurso());
            response.setNotify("Proceso de importación finalizado con éxito.");
        } catch (BusinessException e) {
            logger.error("Error durante la importación", e);
            limpiarCampo(response, tareaImportacion.getFichero(), "fichero", false);
            response.setError("Error durante la importación: " + e.getMessage());
        } /*finally {
            limpiarCampo(response, tareaImportacion.getFichero(), "fichero", true);
        }*/
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> T getModelo(ActionRequest request, Class<T> modelClass) {
        return (T) new ActionRequestHelper(request, modelClass).getModel(AllowProperties.createAllowAllProperties());
    }

    private void limpiarCampo(ActionResponse response, MetaFile file, String fieldName, boolean mantenerFichero) {
        response.setValue(fieldName, null);
        if (!mantenerFichero && file != null) MetaFileUtil.delete(file);
    }
}