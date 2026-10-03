package com.educaflow.subsystem.importacion.service.impl;

import com.educaflow.base.util.SecurityUtil;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.exception.ImportadorException;
import com.educaflow.subsystem.importacion.importador.ImportadorFichero;
import com.educaflow.subsystem.importacion.importador.ImportadorFicheroFactory;
import com.educaflow.subsystem.importacion.importador.ResultadoImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import com.educaflow.base.util.Convert;

public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion> implements TareaImportacionService {

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public TareaImportacionServiceImpl(Class<TareaImportacion> model,
                                       Repository<TareaImportacion> repository) {
        super(model, repository);
    }

    @Override
    public TareaImportacion insert(TareaImportacion tareaImportacion) {
        validateInsert(tareaImportacion).ifPresent(BusinessMessages::throwIfInvalid);
        fireActionRule_AsignarCamposSistema(tareaImportacion);
        fireActionRule_EjecutarImportacion(tareaImportacion);
        return repository.save(tareaImportacion);
    }

    @Override
    public TareaImportacion update(TareaImportacion entidad, TareaImportacion entidadOriginal) {
        throw new UnsupportedOperationException(I18n.get("Las importaciones ya registradas no se pueden modificar"));
    }

    @Override
    public void remove(TareaImportacion entidad) {
        throw new UnsupportedOperationException(I18n.get("Las importaciones no se pueden eliminar"));
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(TareaImportacion tareaImportacion) {
        BusinessMessages messages = new BusinessMessages();

        if (tareaImportacion.getTipoFichero() == null) {
            messages.add(new BusinessMessage("tipoFichero",
                    I18n.get("El tipo de fichero es obligatorio. Valores válidos: PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO")));
        }

        if (tareaImportacion.getFichero() == null) {
            messages.add(new BusinessMessage("fichero", I18n.get("El fichero es obligatorio")));
        }

        return messages.isEmpty() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(TareaImportacion entidad,
                                                     TareaImportacion entidadOriginal) {
        BusinessMessages messages = new BusinessMessages();
        messages.add(new BusinessMessage(I18n.get("Las importaciones ya registradas no se pueden modificar")));
        return Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(TareaImportacion entidad) {
        BusinessMessages messages = new BusinessMessages();
        messages.add(new BusinessMessage(I18n.get("Las importaciones no se pueden eliminar")));
        return Optional.of(messages);
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "tipoFichero", Map.of(),
                "fichero", Map.of()
        ));
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_AsignarCamposSistema(TareaImportacion tareaImportacion) {
        tareaImportacion.setUsuario(SecurityUtil.getUser());
        tareaImportacion.setFechaImportacion(LocalDateTime.now(Convert.defaultZoneId));
        tareaImportacion.setFechaExportacion(null);
        tareaImportacion.setEstado(false);
        tareaImportacion.setLog(null);
    }

    private void fireActionRule_EjecutarImportacion(TareaImportacion tareaImportacion) {
        ImportadorFichero importador = ImportadorFicheroFactory.create(
                tareaImportacion.getTipoFichero(), tareaImportacion.getFichero());
        try {
            ResultadoImportacion resultado = importador.importar();
            tareaImportacion.setEstado(true);
            tareaImportacion.setCentro(resultado.centro());
            tareaImportacion.setCurso(resultado.curso());
            tareaImportacion.setLog("Importación finalizada. " + resultado.log());
            tareaImportacion.setFechaExportacion(LocalDateTime.now(Convert.defaultZoneId));
        } catch (ImportadorException ex) {
            tareaImportacion.setEstado(false);
            tareaImportacion.setLog(ex.getMessage());
        }
    }
}
