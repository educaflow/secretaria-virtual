package com.educaflow.base.infrastructure.importer;

import com.educaflow.base.infrastructure.importer.impl.FileImporterCsv;
import com.educaflow.base.infrastructure.importer.impl.FileImporterXml;

public class FileImporterFactory {

    public static FileImporter getFileImporterXml() {
        return new FileImporterXml();
    }

    public static FileImporter getFileImporterCsv() {
        return new FileImporterCsv();
    }
}
