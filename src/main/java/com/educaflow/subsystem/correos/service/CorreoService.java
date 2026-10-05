package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.correos.db.Correo;

import java.util.List;
import java.util.Optional;

public interface CorreoService extends ModelService<Correo> {

    List<Correo> listarCorreosEnFail();
    Correo reenviar(Correo entidad, Correo entidadOriginal);


    Optional<BusinessMessages> validateListarCorreosEnFail();
    Optional<BusinessMessages> validateReenviar(Correo entidad, Correo entidadOriginal);


    AllowProperties allowPropertiesReenviar();

}
