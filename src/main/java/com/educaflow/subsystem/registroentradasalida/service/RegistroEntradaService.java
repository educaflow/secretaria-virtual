package com.educaflow.subsystem.registroentradasalida.service;

import com.axelor.db.modelservice.ModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;

import java.util.List;

public interface RegistroEntradaService extends ModelService<RegistroEntrada> {
    RegistroEntrada createRegistroEntrada(RegistroEntradaInsertDTO registroEntradaInsertDTO, MetaFile documentoOriginalFirmado, List<MetaFile> anexos);
}
