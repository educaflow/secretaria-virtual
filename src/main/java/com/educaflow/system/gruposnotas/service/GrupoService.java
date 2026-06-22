package com.educaflow.system.gruposnotas.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.gruposnotas.db.Grupo;

import java.util.Optional;

public interface GrupoService extends ModelService<Grupo> {

    Grupo cerrar(Grupo grupo, Grupo grupoOriginal);

    Grupo reabrir(Grupo grupo, Grupo grupoOriginal);

    Optional<BusinessMessages> validateCerrar(Grupo grupo, Grupo grupoOriginal);

    Optional<BusinessMessages> validateReabrir(Grupo grupo, Grupo grupoOriginal);

    AllowProperties allowPropertiesCerrar();

    AllowProperties allowPropertiesReabrir();

}
