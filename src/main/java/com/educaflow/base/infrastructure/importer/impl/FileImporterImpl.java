package com.educaflow.base.infrastructure.importer.impl;

import com.axelor.data.ImportTask;
import com.axelor.data.Importer;
import com.axelor.data.xml.XMLImporter;
import com.educaflow.base.util.XmlUtil;
import com.educaflow.base.infrastructure.importer.DataImport;
import com.educaflow.base.infrastructure.importer.FileImporter;
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
    public List<String> importFile(DataImport dataImport) {

        List<String> importLog = new ArrayList<>();
        Path tempConfigFile = null;

        try {
            byte[] dataFileContent = dataImport.data();

            if (dataImport.validationSchemaPath() != null) {
                if (!ejecutarValidacion(dataFileContent, dataImport.validationSchemaPath(), importLog)) {
                    return importLog;
                }
            }

            tempConfigFile = crearFicheroTemporalFromResource(dataImport.configFilePath());

            ejecutarImportacion(tempConfigFile, dataFileContent, importLog);
        } catch (Exception e) {
            logger.error("Error durante la importación del archivo", e);
            throw new RuntimeException(e);
        } finally {
            borrarArchivoTemporal(tempConfigFile);
        }

        return importLog;
    }

    private boolean ejecutarValidacion(byte[] content, String validationSchemaPath, List<String> log) {
        Optional<String> result = validarConSchema(content, validationSchemaPath);
        if (result.isPresent()) {
            log.add("Error de validación XML: " + result.get());
            return false;
        }
        return true;
    }

    private Optional<String> validarConSchema(byte[] dataFileContent, String validationSchemaPath) {
        try (InputStream validationSchema = this.getClass().getResourceAsStream(validationSchemaPath);
             InputStream archivoImportacion = new ByteArrayInputStream(dataFileContent)) {
            if (validationSchema == null) {
                return Optional.of("No se pudo cargar el esquema de validación desde la ruta: " + validationSchemaPath);
            }
            return XmlUtil.validarConSchema(archivoImportacion, validationSchema);
        } catch (IOException e) {
            throw new RuntimeException(e);
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

        ImportTask task = new ImportTaskImpl();
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
            logger.error("Error leyendo el root de la configuración", e);
        }
        return null;
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
                logger.warn("Excepción SAX inesperada: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error crítico extrayendo atributos de {}", targetNode, e);
        }
    }

    private Path crearFicheroTemporalFromResource(String resourcePath) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream(resourcePath)) {
            if (is == null) throw new FileNotFoundException("No se encontró: " + resourcePath);

            Path temp = Files.createTempFile("import-config-", ".xml");
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
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