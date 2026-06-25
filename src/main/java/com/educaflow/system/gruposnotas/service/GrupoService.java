package com.educaflow.system.gruposnotas.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.gruposnotas.db.Grupo;

import java.util.Optional;

public interface GrupoService extends ModelService<Grupo> {

    Grupo cerrarGrupo(Grupo grupo, Grupo original);

    Optional<BusinessMessages> validateCerrarGrupo(Grupo grupo, Grupo original);

    AllowProperties allowPropertiesCerrarGrupo();

    Grupo reabrirGrupo(Grupo grupo, Grupo original);

    Optional<BusinessMessages> validateReabrirGrupo(Grupo grupo, Grupo original);

    AllowProperties allowPropertiesReabrirGrupo();
}
