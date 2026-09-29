package com.educaflow.subsystem.expedientes.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.db.repo.TramiteRepository;
import com.educaflow.subsystem.expedientes.service.TramiteService;

import java.util.List;
import java.util.Optional;

public class TramiteServiceImpl extends DefaultModelService<Tramite> implements TramiteService {

    public TramiteServiceImpl(Class<Tramite> model, Repository<Tramite> repository) {
        super(model, repository);
    }

    @Override
    public List<Tramite> findConTipoExpedienteActivo() {
        validateFindConTipoExpedienteActivo().ifPresent(BusinessMessages::throwIfInvalid);

        return ((TramiteRepository) repository).findConTipoExpedienteActivo();
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateFindConTipoExpedienteActivo() {
        return Optional.empty();
    }
}
