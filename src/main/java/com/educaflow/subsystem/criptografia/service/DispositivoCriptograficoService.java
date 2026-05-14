package com.educaflow.subsystem.criptografia.service;

import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;

import java.util.Optional;

public interface DispositivoCriptograficoService extends ModelService<DispositivoCriptografico> {

    void recargarDispositivosEnEntornoCriptografico();

    Optional<BusinessMessages> validateInsert(DispositivoCriptografico dispositivo);

    Optional<BusinessMessages> validateUpdate(DispositivoCriptografico dispositivo, DispositivoCriptografico dispositivoOriginal);

    Optional<BusinessMessages> validateRemove(DispositivoCriptografico dispositivo);

}
