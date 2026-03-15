package com.educaflow.subsystem.firmas.service;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.firma.db.Firma;

public interface FirmaService {

    Firma insert(DatosFirma datosFirma);
    Firma firmar(Firma firma, MetaFile documentoFirmado);
    Firma rechazarFirma(Firma firma, String motivoRechazo);
}
