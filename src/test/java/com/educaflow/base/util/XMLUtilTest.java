package com.educaflow.base.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;

class XMLUtilTest {

    private Element root;

    @BeforeEach
    void setUp() {
        String xml = "<root><a name=\"x\"/><b><a name=\"y\"/></b><a name=\"z\"/></root>";
        root = XMLUtil.getDocument(xml.getBytes(StandardCharsets.UTF_8)).getDocumentElement();
    }

    @Nested
    class GetElementsFromEvaluateXPath {

        @Test
        void expresionNullLanzaNullPointer() {
            assertThrows(NullPointerException.class,
                    () -> XMLUtil.getElementsFromEvaluateXPath(null, root, false));
        }

        @Test
        void expresionEnBlancoLanzaIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> XMLUtil.getElementsFromEvaluateXPath("   ", root, true));
        }

        @Test
        void rootNullLanzaNullPointer() {
            assertThrows(NullPointerException.class,
                    () -> XMLUtil.getElementsFromEvaluateXPath("a", null, false));
        }

        @Test
        void expresionAbsolutaSinPermisoLanzaIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> XMLUtil.getElementsFromEvaluateXPath("//a", root, false));
            assertEquals("expression no puede empezar por //://a", ex.getMessage());
        }

        @Test
        void expresionAbsolutaConPermisoDevuelveTodosLosDelDocumento() {
            List<Element> elements = XMLUtil.getElementsFromEvaluateXPath("//a", root, true);

            assertEquals(List.of("x", "y", "z"), elements.stream().map(e -> e.getAttribute("name")).toList());
        }

        @Test
        void expresionRelativaDevuelveSoloLosHijosDirectos() {
            List<Element> elements = XMLUtil.getElementsFromEvaluateXPath("a", root, false);

            assertEquals(List.of("x", "z"), elements.stream().map(e -> e.getAttribute("name")).toList());
        }

        @Test
        void expresionSinCoincidenciasDevuelveListaVacia() {
            List<Element> elements = XMLUtil.getElementsFromEvaluateXPath("noexiste", root, false);

            assertTrue(elements.isEmpty());
        }

        @Test
        void sobrecargaDeDosArgumentosNoPermiteExpresionAbsoluta() {
            assertThrows(IllegalArgumentException.class, () -> XMLUtil.getElementsFromEvaluateXPath("//a", root));
            assertEquals(2, XMLUtil.getElementsFromEvaluateXPath("a", root).size());
        }

        @Test
        void expresionInvalidaSeEnvuelveEnRuntimeException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> XMLUtil.getElementsFromEvaluateXPath("a[", root, false));
            assertEquals("Error evaluating XPath expression: a[", ex.getMessage());
            assertInstanceOf(XPathExpressionException.class, ex.getCause());
        }

        @Test
        void resultadoQueNoEsElementoLanzaClassCastException() {
            assertThrows(ClassCastException.class,
                    () -> XMLUtil.getElementsFromEvaluateXPath("a/@name", root, false));
        }
    }

    @Nested
    class GetBooleanAttribute {

        private Element elementoCon(String atributos) {
            String xml = "<e " + atributos + "/>";
            return XMLUtil.getDocument(xml.getBytes(StandardCharsets.UTF_8)).getDocumentElement();
        }

        @Test
        void atributoAusenteDevuelveDefaultTrue() {
            assertTrue(XMLUtil.getBooleanAttribute(elementoCon(""), "flag", true));
        }

        @Test
        void atributoAusenteDevuelveDefaultFalse() {
            assertFalse(XMLUtil.getBooleanAttribute(elementoCon(""), "flag", false));
        }

        @Test
        void atributoVacioDevuelveDefault() {
            assertTrue(XMLUtil.getBooleanAttribute(elementoCon("flag=\"\""), "flag", true));
        }

        @Test
        void atributoEnBlancoDevuelveDefault() {
            assertFalse(XMLUtil.getBooleanAttribute(elementoCon("flag=\"   \""), "flag", false));
        }

        @Test
        void valorNullDevuelveDefault() {
            Element element = Mockito.mock(Element.class);
            Mockito.when(element.hasAttribute("flag")).thenReturn(true);
            Mockito.when(element.getAttribute("flag")).thenReturn(null);
            assertTrue(XMLUtil.getBooleanAttribute(element, "flag", true));
        }

        @Test
        void valorTrueSinDistinguirMayusculasDevuelveTrue() {
            assertTrue(XMLUtil.getBooleanAttribute(elementoCon("flag=\"TRUE\""), "flag", false));
        }

        @Test
        void valorFalseSinDistinguirMayusculasDevuelveFalse() {
            assertFalse(XMLUtil.getBooleanAttribute(elementoCon("flag=\"False\""), "flag", true));
        }

        @Test
        void valorConEspaciosAlrededorNoSeRecortaYLanza() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> XMLUtil.getBooleanAttribute(elementoCon("flag=\" true \""), "flag", false));
            assertEquals("Valor fuera de rango: true  en flag", ex.getMessage());
        }

        @Test
        void valorFueraDeRangoLanzaRuntimeException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> XMLUtil.getBooleanAttribute(elementoCon("flag=\"si\""), "flag", false));
            assertEquals("Valor fuera de rango:si en flag", ex.getMessage());
        }
    }
}
