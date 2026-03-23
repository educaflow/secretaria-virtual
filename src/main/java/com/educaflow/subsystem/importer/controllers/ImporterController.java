package com.educaflow.subsystem.importer.controllers;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.importer.db.ViewDataImport;
import com.educaflow.subsystem.importer.services.FileImporter;
import com.google.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ImporterController {

    private final String LOG_FIELD = "importLog";
    private final Logger logger = LoggerFactory.getLogger(ImporterController.class);

    @Inject
    FileImporter importer;

    @CallMethod
    @Transactional
    public void procesarFichero(ActionRequest request, ActionResponse response) {
        List<String> logs;
        ViewDataImport viewDataImport = null;
        try {
            viewDataImport = request.getContext().asType(ViewDataImport.class);
            logs = importer.importFile(viewDataImport);
            response.setNotify("Proceso de importación finalizado con éxito.");
            response.setValue(LOG_FIELD, String.join(System.lineSeparator(), logs));
        } catch (Exception e) {
            logger.error("Error durante la importación del archivo", e);
            throw new RuntimeException(e);
        } finally {
            response.setValue("dataFile", null);
            if (viewDataImport.getDataFile() != null) {
                MetaFileUtil.delete(viewDataImport.getDataFile());
            }
        }
    }



}