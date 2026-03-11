package com.educaflow.shared.common.importar;

import com.axelor.inject.Beans;
import com.axelor.meta.CallMethod;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.importer.FileImporter;
import com.educaflow.base.infrastructure.importer.FileImporterFactory;
import com.educaflow.base.infrastructure.importer.FileType;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.util.MetaFileUtil;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserImporter {

    private final Logger logger = LoggerFactory.getLogger(UserImporter.class);
    private final String USUARIOS_CONFIG = "usuarios-config.xml";
    private final String USUARIOS_FIELD = "importarUsuarios";
    private final String USUARIOS_INPUT = "usuarios";


    @CallMethod
    public void importarUsuarios(ActionRequest request, ActionResponse response) {
        Object usuariosObject = request.getContext().get(USUARIOS_FIELD);

        if (usuariosObject == null) {
            response.setAlert("No se ha seleccionado ningún archivo para importar.");
            return;
        }

        MetaFile file = MetaFileUtil.getMetaFile(usuariosObject);

        try {
            if (file == null) {
                response.setError("El archivo seleccionado no es válido.");
                return;
            }

            FileImporter fileImporter = FileImporterFactory.getFileImporter(FileType.XML);
            fileImporter.importFile(USUARIOS_CONFIG, USUARIOS_INPUT, file);
            response.setValue(USUARIOS_FIELD, null);
            response.setCanClose(true);
            response.setReload(true);
            response.setNotify("Proceso de importación finalizado con éxito.");
        } catch (Exception e) {
            logger.error("Error crítico durante la importación", e);
            response.setError("Error en la importación: " + e.getMessage());
        } finally {
            MetaFileUtil.deleteSafe(file);
        }

    }


}