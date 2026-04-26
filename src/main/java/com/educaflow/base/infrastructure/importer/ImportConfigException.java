package com.educaflow.base.infrastructure.importer;

public class ImportConfigException extends RuntimeException {

    public ImportConfigException(String message) {
        super(message);
    }

    public ImportConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}