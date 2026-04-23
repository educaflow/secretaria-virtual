package com.educaflow.subsystem.registroentradasalida.service;

import com.axelor.db.modelservice.ModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;

import java.util.List;

public interface RegistroSalidaService extends ModelService<RegistroSalida> {
    RegistroSalida createRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documento, List<MetaFile> anexos);
}
