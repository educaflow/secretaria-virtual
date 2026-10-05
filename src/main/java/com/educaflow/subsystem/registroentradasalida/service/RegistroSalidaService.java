package com.educaflow.subsystem.registroentradasalida.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;

import java.util.List;
import java.util.Optional;

public interface RegistroSalidaService extends ModelService<RegistroSalida> {

    /** La ruta, bajo {@code /ws}, de la descarga pública de un registro de salida; detrás va {@code /download?CSV=<csv>}. */
    String RUTA_DESCARGA_PUBLICA = "/public/registro-salida";

    /** El metadato del documento de un registro de salida que lleva su CSV, el mismo que va en la URL de su código QR. */
    String METADATO_CSV = "SecretariaVirtualCSV";

    RegistroSalida createRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documento, List<MetaFile> anexos);
    Fichero getDescargaByCsv(String csv);


    Optional<BusinessMessages> validateCreateRegistroSalida(RegistroSalidaInsertDTO registroSalidaInsertDTO, MetaFile documentoOriginal, List<MetaFile> anexos);
    Optional<BusinessMessages> validateGetDescargaByCsv(String csv);

}
