package com.educaflow.subsystem.security.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.security.db.AceProfileCentro;

import java.util.List;
import java.util.Optional;

public interface AceProfileCentroService extends ModelService<AceProfileCentro> {

    List<Centro> getCentrosSupervisados();


    Optional<BusinessMessages> validateGetCentrosSupervisados();
}
