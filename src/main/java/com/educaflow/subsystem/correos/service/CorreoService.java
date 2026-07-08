package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.correos.db.Correo;

import java.util.List;
import java.util.Optional;

public interface CorreoService extends ModelService<Correo> {

    // Envía (o reintenta) el correo indicado por su id. Misma función para el envío inicial (la
    // invoca R-Correo-001 tras el alta) y para el reintento (la invoca R-Correo-002 tras "reenviar").
    // Invocable también de forma programática desde otros subsistemas (design-guidelines).
    void enviarCorreo(Long correoId);

    // Devuelve todos los correos en estado FAIL (para un futuro reenvío en bloque; design-guidelines).
    List<Correo> listarCorreosEnFail();

    // Acción propia: reintenta el envío de un correo en FAIL. Invocada desde CorreoController.reenviar.
    Correo reenviar(Correo entidad, Correo entidadOriginal);

    Optional<BusinessMessages> validateReenviar(Correo entidad, Correo entidadOriginal);

    AllowProperties allowPropertiesReenviar();
}
