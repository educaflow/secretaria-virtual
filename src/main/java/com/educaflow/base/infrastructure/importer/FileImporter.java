package com.educaflow.base.infrastructure.importer;

import java.util.List;

public interface FileImporter {

    List<String> importFile(DataImport dataImport) throws ImportValidationException;
}