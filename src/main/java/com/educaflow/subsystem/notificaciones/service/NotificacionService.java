package com.educaflow.subsystem.notificaciones.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.notificaciones.db.Notificacion;

import java.util.List;
import java.util.Optional;

public interface NotificacionService<T extends Notificacion> extends ModelService<T> {

    T create();

    T reenviar(T entidad, T entidadOriginal);

    List<T> listarEnFail();


    Optional<BusinessMessages> validateCreate();

    Optional<BusinessMessages> validateReenviar(T entidad, T entidadOriginal);

    Optional<BusinessMessages> validateListarEnFail();


    AllowProperties allowPropertiesReenviar();
}
