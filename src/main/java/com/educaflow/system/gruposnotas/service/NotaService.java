package com.educaflow.system.gruposnotas.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.gruposnotas.db.Nota;

import java.util.Optional;

public interface NotaService extends ModelService<Nota> {

    Nota guardarNota(Nota nota, Nota notaOriginal);

    Optional<BusinessMessages> validateGuardarNota(Nota nota, Nota notaOriginal);

    AllowProperties allowPropertiesGuardarNota();

}
