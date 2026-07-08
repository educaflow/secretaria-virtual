package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.correos.db.Adjunto;

public interface AdjuntoService extends ModelService<Adjunto> {

    // Sin acciones propias: solo se sobrescriben validateInsert/Update/Remove y allowPropertiesInsert
    // (no hace falta redeclarar insert/update/remove ni sus validate*/allowProperties* si no se añade
    // una acción nueva; ModelService ya los declara).
}
