package com.educaflow.subsystem.importacion.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;

import java.time.LocalDate;
import java.util.Optional;

public interface TareaImportacionService extends ModelService<TareaImportacion> {

    @Override
    TareaImportacion insert(TareaImportacion tareaImportacion);
    @Override
    TareaImportacion update(TareaImportacion newTareaImportacion, TareaImportacion oldTareaImportacion);
    @Override
    void remove(TareaImportacion tareaImportacion);

    Optional<LocalDate> findFechaUltimaImportacion(Long centroId, TipoFicheroImportacion tipoFichero);
}
