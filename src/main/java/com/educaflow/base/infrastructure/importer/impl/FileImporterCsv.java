package com.educaflow.base.infrastructure.importer.impl;

import com.axelor.data.ImportTask;
import com.axelor.data.Importer;
import com.axelor.data.csv.CSVImporter;
import com.educaflow.base.util.ImporterUtil;
import com.educaflow.base.infrastructure.importer.DataImport;
import com.educaflow.base.infrastructure.importer.FileImporter;
import com.educaflow.base.infrastructure.importer.ImportValidationException;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileImporterCsv implements FileImporter {

    private final String DATA_FILE_NAME = "data.csv";

    @Override
    public List<String> importFile(DataImport dataImport) throws ImportValidationException {
        List<String> log = new ArrayList<>();
        Path tempConfigFile = null;
        try {
            tempConfigFile = ImporterUtil.crearFicheroTemporalFromResource(dataImport.configFilePath());
            ejecutarImportacion(tempConfigFile, dataImport.data(), dataImport.context(), log);
        } finally {
            ImporterUtil.borrarArchivoTemporal(tempConfigFile);
        }
        return log;
    }

    private void ejecutarImportacion(Path configPath, byte[] data, Map<String, Object> context, List<String> log) {
        Importer importer = new CSVImporter(configPath.toAbsolutePath().toString());
        importer.setContext(new HashMap<>(context));
        importer.addListener(new ListenerImpl(log));

        ImportTask task = new ImportTaskImpl(log);
        task.input(DATA_FILE_NAME, new ByteArrayInputStream(data));
        importer.run(task);
    }
}
