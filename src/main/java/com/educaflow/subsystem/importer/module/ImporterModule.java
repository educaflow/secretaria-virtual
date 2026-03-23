package com.educaflow.subsystem.importer.module;

import com.axelor.app.AxelorModule;
import com.axelor.data.ImportTask;
import com.educaflow.subsystem.importer.services.FileImporter;
import com.educaflow.subsystem.importer.services.impl.ImportTaskImpl;
import com.educaflow.subsystem.importer.services.impl.FileImporterImpl;

public class ImporterModule extends AxelorModule {

    @Override
    public void configure() {
        bind(FileImporter.class).to(FileImporterImpl.class);
        bind(ImportTask.class).to(ImportTaskImpl.class);
    }
}
