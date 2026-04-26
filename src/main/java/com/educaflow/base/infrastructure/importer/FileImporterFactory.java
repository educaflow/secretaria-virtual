package com.educaflow.base.infrastructure.importer;

import com.educaflow.base.infrastructure.importer.impl.FileImporterImpl;

public class FileImporterFactory {

    public static FileImporter getFileImporter() {
        return new FileImporterImpl();
    }
}
