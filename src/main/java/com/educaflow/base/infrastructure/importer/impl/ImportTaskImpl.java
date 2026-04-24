package com.educaflow.base.infrastructure.importer.impl;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ImportTaskImpl extends ImportTask {

    private static final Logger logger = LoggerFactory.getLogger(ImportTask.class);

    @Override
    public void configure() throws IOException {

    }

    @Override
    public boolean handle(ImportException exception) {
        logger.error("Import error: " + exception);
        return true;
    }

    @Override
    public boolean handle(IOException exception) {
        logger.error("IOException error: " + exception);
        return false;
    }

    @Override
    public boolean handle(ClassNotFoundException exception) {
        logger.error("ClassNotFoundException: " + exception);
        return false;
    }
}
