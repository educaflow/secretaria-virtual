package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.exception.ImportadorException;
import com.educaflow.subsystem.importacion.importador.ImportadorFichero;
import com.educaflow.subsystem.importacion.importador.ImportadorFicheroFactory;
import com.educaflow.subsystem.importacion.importador.ResultadoImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;

import java.time.LocalDateTime;
import java.util.Optional;

public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion> implements TareaImportacionService {

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public TareaImportacionServiceImpl(Class<TareaImportacion> model,
                                       Repository<TareaImportacion> repository) {
        super(model, repository);
    }

    // --- Métodos CRUD ---

    @Override
    public TareaImportacion insert(TareaImportacion tareaImportacion) {
        fireActionRule_asignarCamposSistema(tareaImportacion);
        fireActionRule_ejecutarImportacion(tareaImportacion);
        return super.insert(tareaImportacion);
    }

    // --- Métodos de validación ---

    @Override
    public Optional<BusinessMessages> validateInsert(TareaImportacion tareaImportacion) {
        BusinessMessages messages = new BusinessMessages();

        if (tareaImportacion.getTipoFichero() == null) {
            messages.add(new BusinessMessage("tipoFichero",
                    "El tipo de fichero es obligatorio. Valores válidos: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO"));
        }

        if (tareaImportacion.getFichero() == null) {
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

    private void fireActionRule_asignarCamposSistema(TareaImportacion tareaImportacion) {
        tareaImportacion.setUsuario(AuthUtils.getUser());
        tareaImportacion.setFechaImportacion(LocalDateTime.now());
        tareaImportacion.setFechaExportacion(null);
        tareaImportacion.setEstado(false);
        tareaImportacion.setLog(null);
    }

    private void fireActionRule_ejecutarImportacion(TareaImportacion tareaImportacion) {
        ImportadorFichero importador = ImportadorFicheroFactory.create(
                tareaImportacion.getTipoFichero(), tareaImportacion.getFichero());
        try {
            ResultadoImportacion resultado = importador.importar();
            tareaImportacion.setEstado(true);
            tareaImportacion.setCentro(resultado.centro());
            tareaImportacion.setCurso(resultado.curso());
            tareaImportacion.setLog("Importación finalizada. " + resultado.log());
            tareaImportacion.setFechaExportacion(LocalDateTime.now());
        } catch (ImportadorException ex) {
            tareaImportacion.setEstado(false);
            tareaImportacion.setLog(ex.getMessage());
        }
    }
}
