package com.educaflow.subsystem.importer.services;


import com.educaflow.subsystem.importer.db.ViewDataImport;

import java.util.List;

public interface FileImporter {

    public List<String> importFile(ViewDataImport viewDataImport);
}
