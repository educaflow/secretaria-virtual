package com.educaflow.subsystem.firmas.service;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.firma.db.TareaFirma;

public interface FirmaService {

    TareaFirma insert(DatosFirma datosFirma);
    TareaFirma firmar(TareaFirma tareaFirma, MetaFile documentoFirmado);
    TareaFirma rechazarFirma(TareaFirma tareaFirma, String motivoRechazo);
}
