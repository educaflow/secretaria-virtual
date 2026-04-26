package com.educaflow.base.infrastructure.importer.impl;

import com.axelor.data.ImportTask;
import com.axelor.data.Importer;
import com.axelor.data.xml.XMLImporter;
import com.educaflow.base.util.XmlUtil;
import com.educaflow.base.infrastructure.importer.DataImport;
import com.educaflow.base.infrastructure.importer.FileImporter;
import com.educaflow.base.infrastructure.importer.ImportConfigException;
import com.educaflow.base.infrastructure.importer.ImportValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FileImporterImpl implements FileImporter {

    private final Logger logger = LoggerFactory.getLogger(FileImporter.class);
    private final String IMPORT_LOGGER_NAME = "importLogger";
    private final String DATA_FILE_NAME = "dataFile";

    @Override
    public List<String> importFile(DataImport dataImport) throws ImportValidationException {
        List<String> importLog = new ArrayList<>();
        Path tempConfigFile = null;

        try {
            if (dataImport.validationSchemaPath() != null) {
                validarEsquema(dataImport.data(), dataImport.validationSchemaPath());
            }

            tempConfigFile = crearFicheroTemporalFromResource(dataImport.configFilePath());
            ejecutarImportacion(tempConfigFile, dataImport.data(), importLog);
        } finally {
            borrarArchivoTemporal(tempConfigFile);
        }

        return importLog;
    }

    private void validarEsquema(byte[] content, String schemaPath) throws ImportValidationException {
        try (InputStream schema = getClass().getResourceAsStream(schemaPath);
             InputStream data = new ByteArrayInputStream(content)) {
            if (schema == null) {
                throw new ImportConfigException("Esquema de validación no encontrado: " + schemaPath);
            }
            Optional<String> error = XmlUtil.validarConSchema(data, schema);
            if (error.isPresent()) {
                throw new ImportValidationException("El fichero XML no es válido: " + error.get());
            }
        } catch (IOException e) {
            throw new ImportConfigException("Error leyendo el esquema de validación: " + schemaPath, e);
        }
    }

    private void ejecutarImportacion(Path configPath, byte[] data, List<String> log) {
        Importer importer = new XMLImporter(configPath.toAbsolutePath().toString());
        Map<String, Object> context = new HashMap<>();

        context.put(IMPORT_LOGGER_NAME, log);

        String nodoRoot = obtenerNodoRootDeConfiguracion(configPath);
        extraerAtributosNodoAlContexto(data, nodoRoot, context);

        importer.setContext(context);
        importer.addListener(new ListenerImpl(log));

        ImportTask task = new ImportTaskImpl(log);

        task.input(DATA_FILE_NAME, new ByteArrayInputStream(data));

        importer.run(task);
    }

    private String obtenerNodoRootDeConfiguracion(Path configPath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(configPath.toFile());
            XPath xPath = XPathFactory.newInstance().newXPath();

            return xPath.evaluate("//input[1]/@root", doc);
        } catch (Exception e) {
            throw new ImportConfigException("Error leyendo la configuración de importación", e);
        }
    }

    private void extraerAtributosNodoAlContexto(byte[] xmlContent, String targetNode, Map<String, Object> context) {
        if (targetNode == null || targetNode.isEmpty()) return;

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse(new ByteArrayInputStream(xmlContent), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (qName.equalsIgnoreCase(targetNode)) {
                        for (int i = 0; i < attributes.getLength(); i++) {
                            context.put(attributes.getQName(i) + "Raiz", attributes.getValue(i));
                            logger.info("Context actualizado: {} = {}", attributes.getQName(i) + "Raiz", attributes.getValue(i));
                        }
                        throw new SAXException("STOP_PARSING");
                    }
                }
            });
        } catch (SAXException e) {
            if (!"STOP_PARSING".equals(e.getMessage())) {
                throw new ImportConfigException("Error SAX extrayendo atributos del nodo '" + targetNode + "'", e);
            }
        } catch (Exception e) {
            throw new ImportConfigException("Error extrayendo atributos del nodo '" + targetNode + "'", e);
        }
    }

    private Path crearFicheroTemporalFromResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) throw new ImportConfigException("Fichero de configuración no encontrado: " + resourcePath);

            Path temp = Files.createTempFile("import-config-", ".xml");
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (IOException e) {
            throw new ImportConfigException("Error creando fichero temporal de configuración: " + resourcePath, e);
        }
    }

    private void borrarArchivoTemporal(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
                logger.info("Temporal eliminado: {}", path);
            } catch (IOException e) {
                logger.warn("No se pudo eliminar el temporal: {}", path);
            }
        }
    }
}