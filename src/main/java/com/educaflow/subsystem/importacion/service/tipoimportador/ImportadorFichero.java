package com.educaflow.subsystem.importacion.service.tipoimportador;

import com.educaflow.subsystem.common.db.Centro;

public interface ImportadorFichero {

    ResultadoImportacion importar(byte[] contenido, Centro centro, Integer curso);
}