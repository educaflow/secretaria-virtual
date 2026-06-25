package com.educaflow.system.gruposnotas.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;

import java.util.Optional;

public interface ModuloGrupoService extends ModelService<ModuloGrupo> {

    ModuloGrupo insert(ModuloGrupoInsertDTO dto);

    Optional<BusinessMessages> validateInsert(ModuloGrupoInsertDTO dto);
}
