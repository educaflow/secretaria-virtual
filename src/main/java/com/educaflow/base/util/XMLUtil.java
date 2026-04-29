package com.educaflow.base.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;

/**
 *
 * @author logongas
 */
public class XMLUtil {

    
    public static Document getDocument(Path filePath) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(false);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(filePath.toFile());

            return document;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Document getDocument(byte[] content) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(false);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(new ByteArrayInputStream(content));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Optional<String> validarConSchema(InputStream xmlFile, InputStream validationSchema) {
        if (xmlFile == null || validationSchema == null) {
            return Optional.of("El archivo XML o el esquema de validación no pueden ser nulos.");
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(validationSchema));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlFile));
            return Optional.empty();
        } catch (SAXException e) {
            return Optional.of("Error de validación: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /****************************************************************************/
    /****************************************************************************/
    /******************************* Buscar hijos *******************************/
    /****************************************************************************/
    /****************************************************************************/
    
    public static List<Element> getChilds(Element parentElement) {
        List<Element> elements = new ArrayList<>();

        NodeList children = parentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node currentNode = children.item(i);

            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) currentNode);

            }
        }
        return elements;
    }    
    
    
    public static List<Element> getChildsFilterByTagName(Element parentElement, String tagName) {
        List<Element> childs = XMLUtil.getChilds(parentElement);
        List<Element> filter = new ArrayList<>();

        for (Element element : childs) {
            if (element.getTagName().equals(tagName)) {
                filter.add(element);
            }
        }
        return filter;
    }
    
    public static Optional<Element> getChildFilterByTagName(Element parentElement, String tagName) {
        List<Element> elements = getChildsFilterByTagName(parentElement, tagName);

        if (elements.size() == 1) {
            return Optional.of(elements.get(0));
        } else if (elements.isEmpty()) {
            return Optional.empty();
        } else {
            throw new RuntimeException("Hay mas de un elemento con tag:" + tagName);
        }
    }
    
    public static List<Element> getChildsFilterByTagNameAndAttributeName(Element parentElement, String name, String tagName) {
        List<Element> childs = XMLUtil.getChildsFilterByTagName(parentElement,tagName);
        List<Element> filter = new ArrayList<>();

        for (Element element : childs) {
            if (element.hasAttribute("name") && element.getAttribute("name").equals(name)) {
                filter.add(element);
            }
        }
        return filter;
    }

    public static Optional<Element> getChildFilterByTagNameAndAttributeName(Element parentElement, String name, String tagName) {
        List<Element> elements = getChildsFilterByTagNameAndAttributeName(parentElement, name, tagName);

        if (elements.size() == 1) {
            return Optional.of(elements.get(0));
        } else if (elements.isEmpty()) {
            return Optional.empty();
        } else {
            throw new RuntimeException("Hay mas de un elemento con tag:" + tagName + " y atributo name=" + name);
        }
    }
    
    
    
    /********************************************************************************************/
    /********************************************************************************************/
    /******************************* Buscar hijos por prefijo Tag *******************************/
    /********************************************************************************************/
    /********************************************************************************************/    
    
    
    public static List<Element> getChildsFilterByPrefixTagName(Element parentElement, String prefixTagName) {
        List<Element> childs = XMLUtil.getChilds(parentElement);
        List<Element> filter = new ArrayList<>();

        for (Element element : childs) {
            if (element.getTagName().startsWith(prefixTagName)) {
                filter.add(element);
            }
        }
        return filter;
    }    
    public static Optional<Element> getChildFilterByPrefixTagName(Element parentElement, String prefixTagName) {
        List<Element> elements = getChildsFilterByPrefixTagName(parentElement, prefixTagName);

        if (elements.size() == 1) {
            return Optional.of(elements.get(0));
        } else if (elements.isEmpty()) {
            return Optional.empty();
        } else {
            throw new RuntimeException("Hay mas de un elemento con tag:" + prefixTagName);
        }
    } 
    
    public static List<Element> getChildsFilterByPrefixTagNameAndAttributeName(Element parentElement, String name, String prefixTagName) {
        List<Element> childs = XMLUtil.getChildsFilterByPrefixTagName(parentElement,prefixTagName);
        List<Element> filter = new ArrayList<>();

        for (Element element : childs) {
            if (element.hasAttribute("name") && element.getAttribute("name").equals(name)) {
                filter.add(element);
            }
        }
        return filter;
    }

    public static Optional<Element> getChildFilterByPrefixTagNameAndAttributeName(Element parentElement, String name, String prefixTagName) {
        List<Element> elements = getChildsFilterByPrefixTagNameAndAttributeName(parentElement, name, prefixTagName);

        if (elements.size() == 1) {
            return Optional.of(elements.get(0));
        } else if (elements.isEmpty()) {
            return Optional.empty();
        } else {
            throw new RuntimeException("Hay mas de un elemento con tag:" + prefixTagName + " y atributo name=" + name);
        }
    }  

    
    
    /**************************************************************************************/
    /**************************************************************************************/
    /******************************* Buscar hijos por XPath *******************************/
    /**************************************************************************************/
    /**************************************************************************************/  

    
    public static List<Element> getElementsFromEvaluateXPath(String expression, Element rootElement) {
        return getElementsFromEvaluateXPath(expression, rootElement,false);
    }    
    
    
    public static List<Element> getElementsFromEvaluateXPath(String expression, Element rootElement,boolean allowRootExpresion) {
        try {
            if (expression == null || expression.trim().isEmpty()) {
                throw new IllegalArgumentException("expression no puede ser null o vacio.");
            }
            if (rootElement == null) {
                throw new IllegalArgumentException("element no puede ser null.");
            }
            if (expression.startsWith("//") && (allowRootExpresion==false)) {
                throw new IllegalArgumentException("expression no puede empezar por //:" + expression);
            }

            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList result = (NodeList) xpath.evaluate(expression, rootElement, XPathConstants.NODESET);
            List<Element> elements=new ArrayList<>();
            for (int i = 0; i < result.getLength(); i++) {
                Element element = (Element) result.item(i);
                elements.add(element);
            }
            
            
            
            return elements;
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluating XPath expression: " + expression, e);
        }
    }
    
    public static Optional<Element> getElementFromEvaluateXPath(String expression, Element element) {
        try {
            if (expression == null || expression.trim().isEmpty()) {
                throw new IllegalArgumentException("expression no puede ser null o vacio.");
            }
            if (element == null) {
                throw new IllegalArgumentException("element no puede ser null.");
            }
            if (expression.startsWith("//")) {
                throw new IllegalArgumentException("expression no puede empezar por //:" + expression);
            }

            XPath xpath = XPathFactory.newInstance().newXPath();
            Element result = (Element) xpath.evaluate(expression, element, XPathConstants.NODE);

            return Optional.ofNullable(result);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluating XPath expression: " + expression, e);
        }
    }
    
    
    /******************************************************************************/
    /******************************************************************************/
    /******************************* Leer Atributos *******************************/
    /******************************************************************************/
    /******************************************************************************/  

    
    
    public static boolean getBooleanAttribute(Element element, String attributeName,boolean defaultValue) {
        
        
        if (element.hasAttribute(attributeName)==false) {
            return defaultValue;
        } else {
            String rawValue=element.getAttribute(attributeName);
            if ((rawValue==null) || (rawValue.trim().isEmpty())) {
                return defaultValue;
            } else if ("true".equalsIgnoreCase(rawValue)) {
                return true;
            } else if ("false".equalsIgnoreCase(rawValue)) {
                return false;
            } else {
                throw new RuntimeException("Valor fuera de rango:"+rawValue+ " en " + attributeName);
            }
        }
        
    }
    
    
    public static int getIntegerAttribute(Element element, String attributeName,int defaultValue) {
        if (element.hasAttribute(attributeName)==false) {
            return defaultValue;
        } else {
            String rawValue=element.getAttribute(attributeName);
            if ((rawValue==null) || (rawValue.trim().isEmpty())) {
                return defaultValue;
            } else {
                return Integer.parseInt(element.getAttribute(attributeName));
            }
        }        
    }
    
    public static String getStringAttribute(Element element, String attributeName,String defaultValue) {
        if (element.hasAttribute(attributeName)==false) {
            return defaultValue;
        } else {
            String rawValue=element.getAttribute(attributeName);
            if ((rawValue==null) || (rawValue.trim().isEmpty())) {
                return defaultValue;
            } else {
                return element.getAttribute(attributeName);
            }
        }        
    }    
    
    
    
    /**********************************************************************/
    /**********************************************************************/
    /******************************* Clonar *******************************/
    /**********************************************************************/
    /**********************************************************************/  
 
       
    
    
    public static List<Element> importElements(List<Element> elements,Document targetDocument) {
        List<Element> clonedElements = new ArrayList<>();

        for (Element element : elements) {
            clonedElements.add(importElement(element, targetDocument));
        }
        return clonedElements;
    }
    public static Element importElement(Element sourceElement,Document targetDocument) {

        Element importedNode = (Element) targetDocument.importNode(sourceElement, true);

        return importedNode;
    }
    

    public static Document cloneDocument(Document originalDocument) {
        if (originalDocument == null) {
            return null;
        }


        Node clonedNode = originalDocument.cloneNode(true);

        if (clonedNode instanceof Document) {
            return (Document) clonedNode;
        } else {
            throw new IllegalStateException("Failed to clone Document. Cloned node is not a Document type.");
        }
    }    
    
    
    
    
    
    
    
    
    
    public static void printNode(Node node) {
        if (node == null) {
            System.out.println("El documento es nulo y no puede ser impreso.");
            return;
        }
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); 
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); 


            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(System.out);


            transformer.transform(source, result);
            System.out.println(); 
        } catch (Exception e) {
            System.err.println("Error al imprimir el documento XML: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static Node getNodeFromString(Document document, String xmlFragmentString) {
        try {

            DocumentBuilderFactory fragmentDbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder fragmentDb = fragmentDbf.newDocumentBuilder();

            Document fragmentDoc = fragmentDb.parse(new InputSource(new StringReader(xmlFragmentString)));
            Node fragmentRootNode = fragmentDoc.getDocumentElement();


            Node importedNode = document.importNode(fragmentRootNode, true); 

            return importedNode;
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener un XML de:\n" + xmlFragmentString, ex);
        }        

    }
    
    
}
