package com.educaflow.subsystem.expedientes.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.service.TipoExpedienteService;

import java.util.Optional;

public class TipoExpedienteServiceImpl extends DefaultModelService<TipoExpediente> implements TipoExpedienteService {

    public TipoExpedienteServiceImpl(Class<TipoExpediente> model, Repository<TipoExpediente> repository) {
        super(model, repository);
    }

    @Override
    public TipoExpediente insert(TipoExpediente tipoExpediente) {
        throw new UnsupportedOperationException(mensajeSoloConsulta());
    }

    @Override
    public TipoExpediente update(TipoExpediente tipoExpediente, TipoExpediente original) {
        throw new UnsupportedOperationException(mensajeSoloConsulta());
    }

    @Override
    public void remove(TipoExpediente tipoExpediente) {
        throw new UnsupportedOperationException(mensajeSoloConsulta());
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(TipoExpediente tipoExpediente) {
        return Optional.of(BusinessMessages.single(mensajeSoloConsulta()));
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(TipoExpediente tipoExpediente, TipoExpediente original) {
        return Optional.of(BusinessMessages.single(mensajeSoloConsulta()));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(TipoExpediente tipoExpediente) {
        return Optional.of(BusinessMessages.single(mensajeSoloConsulta()));
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        // Deny-all: el REST solo deja pasar id, version y claves «_»; insert/update de este servicio lanzan siempre.
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private String mensajeSoloConsulta() {
        return I18n.get("Los tipos de expediente los registra la aplicación al arrancar y no se pueden crear, modificar ni borrar desde la aplicación, tampoco el Administrador.");
    }
}
