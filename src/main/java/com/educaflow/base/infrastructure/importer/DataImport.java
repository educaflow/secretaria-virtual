package com.educaflow.base.infrastructure.importer;

import java.util.Map;

public record DataImport(byte[] data, String configFilePath, String validationSchemaPath, Map<String, Object> context) {
    public DataImport(byte[] data, String configFilePath, String validationSchemaPath) {
        this(data, configFilePath, validationSchemaPath, Map.of());
    }
}