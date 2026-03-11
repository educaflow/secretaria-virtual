package com.educaflow.base.infrastructure.importer;

import com.axelor.meta.db.MetaFile;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

public interface FileImporter {

    public void importFile(String configFileName, String inputFileName, MetaFile metaFile) throws URISyntaxException, FileNotFoundException;
}
