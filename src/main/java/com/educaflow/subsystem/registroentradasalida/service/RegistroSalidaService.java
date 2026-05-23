package com.educaflow.subsystem.registroentradasalida.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;

import java.util.List;
import java.util.Optional;

public interface RegistroSalidaService extends ModelService<RegistroSalida> {
    RegistroSalida createRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documento, List<MetaFile> anexos);

    Optional<BusinessMessages> validateCreateRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documentoOriginal, List<MetaFile> anexos);
}
