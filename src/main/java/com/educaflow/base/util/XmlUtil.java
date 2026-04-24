package com.educaflow.base.util;


import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class XmlUtil {

    public static Optional<String> validarConSchema(InputStream xmlFile, InputStream validationSchema) {
        if (xmlFile == null || validationSchema == null) {
            return Optional.of("El archivo XML o el esquema de validación no pueden ser nulos.");
        }
        try {
            // 1. Configurar la factoría para XSD
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // 2. Cargar el Schema y crear el validador
            Schema schema = factory.newSchema(new StreamSource(validationSchema));
            Validator validator = schema.newValidator();

            // 3. Validar el XML
            validator.validate(new StreamSource(xmlFile));

            return Optional.empty();

        } catch (SAXException e) {
            // Captura errores específicos de estructura XML contra el XSD
            return Optional.of("Error de validación: " + e.getMessage());
        } catch (IOException e) {
            // Captura problemas de lectura del stream
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Lee los atributos de un nodo XML identificado por una expresión XPath.
     * Útil para extraer metadatos del nodo raíz antes de procesar el fichero,
     * por ejemplo comprobar que varios XMLs pertenecen al mismo centro y curso.
     *
     * @param xml        contenido del fichero XML
     * @param xpathNodo  expresión XPath que identifica el nodo (p.ej. {@code "/centro"})
     * @return mapa de nombre de atributo → valor; vacío si el nodo no existe o no tiene atributos
     */
    public static Map<String, String> leerAtributosNodo(InputStream xml, String xpathNodo) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xml);

            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate(xpathNodo, document, XPathConstants.NODESET);

            Map<String, String> atributos = new LinkedHashMap<>();
            if (nodes.getLength() > 0) {
                NamedNodeMap attrs = nodes.item(0).getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    atributos.put(attr.getNodeName(), attr.getNodeValue());
                }
            }
            return atributos;
        } catch (Exception e) {
            throw new RuntimeException("Error al leer atributos del nodo '" + xpathNodo + "'", e);
        }
    }
}
