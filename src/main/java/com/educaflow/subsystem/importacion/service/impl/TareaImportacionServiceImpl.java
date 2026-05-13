package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.exception.ImportadorException;
import com.educaflow.subsystem.importacion.importador.ImportadorFichero;
import com.educaflow.subsystem.importacion.importador.ImportadorFicheroFactory;
import com.educaflow.subsystem.importacion.importador.ResultadoImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion> implements TareaImportacionService {

    private final Logger logger = LoggerFactory.getLogger(TareaImportacionServiceImpl.class);

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public TareaImportacionServiceImpl(Class<TareaImportacion> model,
                                       Repository<TareaImportacion> repository) {
        super(model, repository);
    }

    // --- Métodos CRUD ---

    @Override
    public TareaImportacion insert(TareaImportacion entidad) {
        logger.info("Empezando importación");
        fireActionRule_asignarCamposSistema(entidad);
        fireActionRule_ejecutarImportacion(entidad);
        return super.insert(entidad);
    }

    // --- Métodos de validación ---

    @Override
    public Optional<BusinessMessages> validateInsert(TareaImportacion entidad) {
        BusinessMessages messages = new BusinessMessages();

        if (entidad.getTipoFichero() == null) {
            messages.add(new BusinessMessage("tipoFichero",
                    "El tipo de fichero es obligatorio. Valores válidos: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO"));
        }

        if (entidad.getFichero() == null) {
            messages.add(new BusinessMessage("fichero", "El fichero es obligatorio"));
        }

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(TareaImportacion entidad,
                                                     TareaImportacion entidadOriginal) {
        BusinessMessages messages = new BusinessMessages();
        messages.add(new BusinessMessage("Las importaciones ya registradas no se pueden modificar"));
        return Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(TareaImportacion entidad) {
        BusinessMessages messages = new BusinessMessages();
        messages.add(new BusinessMessage("Las importaciones no se pueden eliminar"));
        return Optional.of(messages);
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_asignarCamposSistema(TareaImportacion entidad) {
        entidad.setUsuario(AuthUtils.getUser());
        entidad.setFechaImportacion(LocalDateTime.now());
        entidad.setFechaExportacion(null);
        entidad.setEstado(false);
        entidad.setLog(null);
    }

    private void fireActionRule_ejecutarImportacion(TareaImportacion entidad) {
        ImportadorFichero importador = ImportadorFicheroFactory.create(
                entidad.getTipoFichero(), entidad.getFichero());
        try {
            ResultadoImportacion resultado = importador.importar();
            entidad.setEstado(true);
            entidad.setLog(resultado.log());
            entidad.setCentro(resultado.centro());
            entidad.setCurso(resultado.curso());
            entidad.setLog("Importación finalizada. " + resultado.log());
            entidad.setFechaExportacion(LocalDateTime.now());
        } catch (ImportadorException ex) {
            entidad.setEstado(false);
            entidad.setLog(ex.getMessage());
        }
    }
}
