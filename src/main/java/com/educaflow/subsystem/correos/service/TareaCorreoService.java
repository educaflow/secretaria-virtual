package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.correos.db.TareaCorreo;

import java.util.List;

public interface TareaCorreoService extends ModelService<TareaCorreo> {

    TareaCorreo procesarEnvio(TareaCorreo tareaCorreo);

    TareaCorreo reenviar(TareaCorreo tareaCorreo, TareaCorreo tareaCorreoOriginal);

    List<TareaCorreo> obtenerPendientes();
}
