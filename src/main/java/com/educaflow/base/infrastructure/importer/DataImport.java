package com.educaflow.base.infrastructure.importer;

public record DataImport(byte[] data, String configFilePath, String validationSchemaPath) {
}