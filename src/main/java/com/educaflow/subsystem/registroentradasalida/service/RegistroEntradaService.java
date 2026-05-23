package com.educaflow.subsystem.registroentradasalida.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;

import java.util.List;
import java.util.Optional;

public interface RegistroEntradaService extends ModelService<RegistroEntrada> {
    RegistroEntrada createRegistroEntrada(RegistroEntradaInsertDTO registroEntradaInsertDTO, MetaFile documentoOriginalFirmado, List<MetaFile> anexos);

    Optional<BusinessMessages> validateCreateRegistroEntrada(RegistroEntradaInsertDTO registroEntradaInsertDTO, MetaFile documentoOriginalFirmado, List<MetaFile> anexos);
}
