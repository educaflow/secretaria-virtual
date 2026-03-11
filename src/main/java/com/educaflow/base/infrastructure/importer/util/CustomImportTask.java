package com.educaflow.base.infrastructure.importer.util;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Named
public class CustomImportTask extends ImportTask {

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

    public boolean handle(Exception exception) {
        logger.error("Exception error: " + exception);
        return true;
    }
}
