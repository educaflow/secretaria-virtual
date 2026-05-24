package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.correos.db.AdjuntoCorreo;

public interface AdjuntoCorreoService extends ModelService<AdjuntoCorreo> {

    // No declara acciones propias del subsistema: solo sobrescribe métodos
    // heredados de ModelService<AdjuntoCorreo> (validateInsert/validateUpdate/
    // allowPropertiesInsert) en la implementación. No se re-declaran aquí
    // insert/update/remove ni sus validate*/allowProperties*: ya vienen de
    // ModelService<T> con defaults en DefaultModelService<T>.
}
