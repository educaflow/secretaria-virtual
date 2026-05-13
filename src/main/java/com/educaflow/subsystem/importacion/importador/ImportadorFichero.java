package com.educaflow.subsystem.importacion.importador;

import com.educaflow.subsystem.importacion.exception.ImportadorException;

public interface ImportadorFichero {

    ResultadoImportacion importar() throws ImportadorException;
}
