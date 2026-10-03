package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.correos.db.Correo;

import java.util.List;
import java.util.Optional;

public interface CorreoService extends ModelService<Correo> {

    List<Correo> listarCorreosEnFail();

    Optional<BusinessMessages> validateListarCorreosEnFail();

    // Acción propia: reintenta el envío de un correo en FAIL. Invocada desde CorreoController.reenviar.
    Correo reenviar(Correo entidad, Correo entidadOriginal);

    Optional<BusinessMessages> validateReenviar(Correo entidad, Correo entidadOriginal);

    AllowProperties allowPropertiesReenviar();
}
