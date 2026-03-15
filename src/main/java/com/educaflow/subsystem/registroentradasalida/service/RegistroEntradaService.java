package com.educaflow.subsystem.registroentradasalida.service;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;

import java.util.List;

public interface RegistroEntradaService {
    RegistroEntrada createRegistroEntrada(DatosRegistroEntrada datosRegistroEntrada, MetaFile documentoOriginalFirmado, List<MetaFile> anexos);
}
