package com.educaflow.subsystem.registroentradasalida.service;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;

import java.util.List;

public interface RegistroSalidaService {
    RegistroSalida createRegistroSalida(DatosRegistroSalida datosRegistroSalida, MetaFile documento, List<MetaFile> anexos);
}
