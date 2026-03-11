package com.educaflow.base.infrastructure.importer.impl;

import com.axelor.data.ImportTask;
import com.axelor.data.Listener;
import com.axelor.data.xml.XMLImporter;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.importer.FileImporter;
import com.educaflow.base.infrastructure.importer.util.ImportTaskUtil;
import com.educaflow.base.infrastructure.importer.util.ListenerUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Path;

@Named
public class XmlFileImporter implements FileImporter {

    private final Logger logger = LoggerFactory.getLogger(XmlFileImporter.class);

    @Override
    public void importFile(String configFileName, String inputFileName, MetaFile metaFile) throws URISyntaxException, FileNotFoundException {

        File archivoImportacion = MetaFiles.getPath(metaFile).toFile();

        String res = Path.of(this.getClass().getResource("/import/" + configFileName).toURI()).toAbsolutePath().toString();

        logger.info("Cargando configuración desde: {}", res);
        XMLImporter importer = new XMLImporter(res);

        logger.info("Configuración cargada");

        ImportTask importTask = ImportTaskUtil.getImportTask();
        importTask.input(inputFileName, archivoImportacion);

        importer.addListener(ListenerUtil.getListener());

        importer.run(importTask);

    }

}
