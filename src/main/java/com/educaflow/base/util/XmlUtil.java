package com.educaflow.base.util;


import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;

public class XmlUtil {

    public static ValidationResult validarConSchema(InputStream xmlFile, InputStream validationSchema) {
        if (xmlFile == null || validationSchema == null) {
            return new ValidationResult(false, "El archivo XML o el esquema de validación no pueden ser nulos.");
        }
        try {
            // 1. Configurar la factoría para XSD
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // 2. Cargar el Schema y crear el validador
            Schema schema = factory.newSchema(new StreamSource(validationSchema));
            Validator validator = schema.newValidator();

            // 3. Validar el XML
            validator.validate(new StreamSource(xmlFile));

            return new ValidationResult(true, "Validación exitosa.");

        } catch (SAXException e) {
            // Captura errores específicos de estructura XML contra el XSD
            return new ValidationResult(false, "Error de validación: " + e.getMessage());
        } catch (IOException e) {
            // Captura problemas de lectura del stream
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public record ValidationResult(
            boolean success,
            String message
    ) {

    }
}
