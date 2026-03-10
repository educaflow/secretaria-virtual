package com.educaflow.shared.registroentradasalida.service;

import com.axelor.meta.db.MetaFile;
import com.educaflow.shared.registroentradasalida.db.RegistroEntrada;

import java.util.List;

public interface RegistroEntradaService {
    RegistroEntrada createRegistroEntrada(DatosRegistroEntrada datosRegistroEntrada, MetaFile documentoOriginalFirmado, List<MetaFile> anexos);
}
