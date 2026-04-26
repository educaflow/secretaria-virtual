package com.educaflow.base.infrastructure.importer.impl;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import com.educaflow.base.infrastructure.importer.ImportConfigException;

import java.io.IOException;
import java.util.List;

public class ImportTaskImpl extends ImportTask {

    private final List<String> log;

    ImportTaskImpl(List<String> log) {
        this.log = log;
    }

    @Override
    public void configure() throws IOException {
    }

    @Override
    public boolean handle(ImportException exception) {
        log.add("Error en importación: " + exception.getMessage());
        return true;
    }

    @Override
    public boolean handle(IOException exception) {
        throw new ImportConfigException("Error de E/S durante la importación", exception);
    }

    @Override
    public boolean handle(ClassNotFoundException exception) {
        throw new ImportConfigException("Clase no encontrada durante la importación", exception);
    }
}
