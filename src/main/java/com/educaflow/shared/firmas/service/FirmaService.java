package com.educaflow.shared.firmas.service;

import com.axelor.meta.db.MetaFile;
import com.educaflow.shared.firma.db.Firma;

public interface FirmaService {

    Firma insert(DatosFirma datosFirma);
    Firma firmar(Firma firma, MetaFile documentoFirmado);
    Firma rechazarFirma(Firma firma, String motivoRechazo);
}
