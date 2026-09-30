package com.educaflow.base.infrastructure.pdf.impl.helper;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfSignatureFormField;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfWidgetAnnotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentoPdfHelperTest {

    private static String toStringFieldsPagina(PdfDocument pdfDocument) throws Exception {
        Method method = DocumentoPdfHelper.class.getDeclaredMethod("toStringFieldsPagina", PdfDocument.class);
        method.setAccessible(true);
        return (String) method.invoke(null, pdfDocument);
    }

    private static PdfDocument documentoConUnaPagina(PdfAnnotation... annotations) {
        PdfDocument pdfDocument = mock(PdfDocument.class);
        PdfPage page = mock(PdfPage.class);
        when(pdfDocument.getNumberOfPages()).thenReturn(1);
        when(pdfDocument.getPage(1)).thenReturn(page);
        when(page.getAnnotations()).thenReturn(List.of(annotations));
        return pdfDocument;
    }

    private static PdfWidgetAnnotation widget(PdfDictionary pdfObject) {
        PdfWidgetAnnotation widget = mock(PdfWidgetAnnotation.class);
        when(widget.getSubtype()).thenReturn(PdfName.Widget);
        when(widget.getPdfObject()).thenReturn(pdfObject);
        return widget;
    }

    @Test
    void toStringFieldsPagina_sinPaginas_devuelveTablaSinFilas() throws Exception {
        PdfDocument pdfDocument = mock(PdfDocument.class);
        when(pdfDocument.getNumberOfPages()).thenReturn(0);

        String resultado = toStringFieldsPagina(pdfDocument);

        assertTrue(resultado.contains("Campos sueltos por página"));
        assertTrue(resultado.contains("Página"));
        assertTrue(resultado.contains("Nombre"));
        assertTrue(resultado.contains("valores"));
        verify(pdfDocument, never()).getPage(anyInt());
    }

    @Test
    void toStringFieldsPagina_anotacionQueNoEsWidget_seIgnora() throws Exception {
        PdfAnnotation link = mock(PdfAnnotation.class);
        when(link.getSubtype()).thenReturn(PdfName.Link);
        PdfDocument pdfDocument = documentoConUnaPagina(link);

        try (MockedStatic<PdfFormField> formField = Mockito.mockStatic(PdfFormField.class)) {
            String resultado = toStringFieldsPagina(pdfDocument);

            assertTrue(resultado.contains("Campos sueltos por página"));
            assertFalse(resultado.contains("PdfWidgetAnnotation sin PdfFormField asociado"));
            formField.verifyNoInteractions();
        }
    }

    @Test
    void toStringFieldsPagina_widgetSinFormField_anhadeFilaDeAviso() throws Exception {
        PdfDictionary pdfObject = new PdfDictionary();
        PdfDocument pdfDocument = documentoConUnaPagina(widget(pdfObject));

        try (MockedStatic<PdfFormField> formField = Mockito.mockStatic(PdfFormField.class)) {
            formField.when(() -> PdfFormField.makeFormField(pdfObject, pdfDocument)).thenReturn(null);

            String resultado = toStringFieldsPagina(pdfDocument);

            assertTrue(resultado.contains("PdfWidgetAnnotation sin PdfFormField asociado"));
            assertTrue(resultado.contains("__null__"));
        }
    }

    @Test
    void toStringFieldsPagina_widgetDeFirma_seIgnora() throws Exception {
        PdfDictionary pdfObject = new PdfDictionary();
        PdfDocument pdfDocument = documentoConUnaPagina(widget(pdfObject));
        PdfSignatureFormField firma = mock(PdfSignatureFormField.class);
        when(firma.getFieldName()).thenReturn(new com.itextpdf.kernel.pdf.PdfString("campoFirma"));

        try (MockedStatic<PdfFormField> formField = Mockito.mockStatic(PdfFormField.class)) {
            formField.when(() -> PdfFormField.makeFormField(pdfObject, pdfDocument)).thenReturn(firma);

            String resultado = toStringFieldsPagina(pdfDocument);

            assertFalse(resultado.contains("campoFirma"));
            assertFalse(resultado.contains("PdfWidgetAnnotation sin PdfFormField asociado"));
        }
    }

    @Test
    void toStringFieldsPagina_camposConYSinEstadosDeApariencia_anhadeUnaFilaPorCampo() throws Exception {
        PdfDictionary objConEstados = new PdfDictionary();
        PdfDictionary objEstadosNull = new PdfDictionary();
        PdfDictionary objEstadosVacios = new PdfDictionary();
        PdfDocument pdfDocument = documentoConUnaPagina(widget(objConEstados), widget(objEstadosNull), widget(objEstadosVacios));

        PdfFormField conEstados = mock(PdfFormField.class);
        when(conEstados.getFieldName()).thenReturn(new com.itextpdf.kernel.pdf.PdfString("casilla"));
        when(conEstados.getAppearanceStates()).thenReturn(new String[]{"Si", "Off"});
        PdfFormField estadosNull = mock(PdfFormField.class);
        when(estadosNull.getFieldName()).thenReturn(new com.itextpdf.kernel.pdf.PdfString("nombre"));
        when(estadosNull.getAppearanceStates()).thenReturn(null);
        PdfFormField estadosVacios = mock(PdfFormField.class);
        when(estadosVacios.getFieldName()).thenReturn(new com.itextpdf.kernel.pdf.PdfString("apellidos"));
        when(estadosVacios.getAppearanceStates()).thenReturn(new String[0]);

        try (MockedStatic<PdfFormField> formField = Mockito.mockStatic(PdfFormField.class)) {
            formField.when(() -> PdfFormField.makeFormField(objConEstados, pdfDocument)).thenReturn(conEstados);
            formField.when(() -> PdfFormField.makeFormField(objEstadosNull, pdfDocument)).thenReturn(estadosNull);
            formField.when(() -> PdfFormField.makeFormField(objEstadosVacios, pdfDocument)).thenReturn(estadosVacios);

            String resultado = toStringFieldsPagina(pdfDocument);

            assertTrue(resultado.contains("casilla"));
            assertTrue(resultado.contains("[Si, Off]"));
            assertTrue(resultado.contains("nombre"));
            assertTrue(resultado.contains("apellidos"));
            assertFalse(resultado.contains("PdfWidgetAnnotation sin PdfFormField asociado"));
        }
    }

    @Test
    void toStringFieldsPagina_siFallaAlLeerElDocumento_devuelveTablaDeError() throws Exception {
        PdfDocument pdfDocument = mock(PdfDocument.class);
        when(pdfDocument.getNumberOfPages()).thenReturn(1);
        when(pdfDocument.getPage(1)).thenThrow(new IllegalStateException("pagina rota"));

        String resultado = toStringFieldsPagina(pdfDocument);

        assertTrue(resultado.contains("Campos sueltos por página"));
        assertTrue(resultado.contains("Error"));
        assertTrue(resultado.contains("pagina rota"));
    }

    @SuppressWarnings("unchecked")
    private static void addFontToRows(PdfDictionary pdfDictionary, Object key, List<List<Object>> rows) throws Exception {
        Method method = DocumentoPdfHelper.class.getDeclaredMethod("addFontToRows", PdfDictionary.class, Object.class, List.class);
        method.setAccessible(true);
        method.invoke(null, pdfDictionary, key, rows);
    }

    @Test
    void addFontToRows_sinDiccionarioDeFuentes_noAnhadeFilas() throws Exception {
        List<List<Object>> rows = new java.util.ArrayList<>();

        addFontToRows(new PdfDictionary(), "clave", rows);

        assertTrue(rows.isEmpty());
    }

    @Test
    void addFontToRows_fuenteCompletaConClave_anhadeFilaConClaveYDatos() throws Exception {
        PdfDictionary fontDict = new PdfDictionary();
        fontDict.put(PdfName.Subtype, PdfName.Type1);
        fontDict.put(PdfName.BaseFont, new PdfName("Helvetica"));
        fontDict.put(PdfName.FontDescriptor, new PdfDictionary());
        PdfDictionary fonts = new PdfDictionary();
        fonts.put(new PdfName("F1"), fontDict);
        PdfDictionary resources = new PdfDictionary();
        resources.put(PdfName.Font, fonts);
        List<List<Object>> rows = new java.util.ArrayList<>();

        addFontToRows(resources, 3, rows);

        assertEquals(List.of(List.of(3, new PdfName("F1"), "Type1", "Helvetica", true)), rows);
    }

    @Test
    void addFontToRows_fuenteSinDatosNiClave_anhadeFilaConDesconocidoYSinClave() throws Exception {
        PdfDictionary fonts = new PdfDictionary();
        fonts.put(new PdfName("F2"), new PdfDictionary());
        PdfDictionary resources = new PdfDictionary();
        resources.put(PdfName.Font, fonts);
        List<List<Object>> rows = new java.util.ArrayList<>();

        addFontToRows(resources, null, rows);

        assertEquals(List.of(List.of(new PdfName("F2"), "Desconocido", "Desconocido", false)), rows);
    }

    private static String toStringFieldsDocumento(PdfDocument pdfDocument) throws Exception {
        Method method = DocumentoPdfHelper.class.getDeclaredMethod("toStringFieldsDocumento", PdfDocument.class);
        method.setAccessible(true);
        return (String) method.invoke(null, pdfDocument);
    }

    @Test
    void toStringFieldsDocumento_sinFormulario_devuelveTablaSinFilas() throws Exception {
        PdfDocument pdfDocument = mock(PdfDocument.class);

        try (MockedStatic<PdfAcroForm> acroForm = Mockito.mockStatic(PdfAcroForm.class)) {
            acroForm.when(() -> PdfAcroForm.getAcroForm(pdfDocument, false)).thenReturn(null);

            String resultado = toStringFieldsDocumento(pdfDocument);

            assertTrue(resultado.contains("Campos del formulario del documento"));
            assertTrue(resultado.contains("Nombre"));
            assertTrue(resultado.contains("valores"));
            assertFalse(resultado.contains("Error"));
        }
    }

    @Test
    void toStringFieldsDocumento_camposConYSinEstados_anhadeFilaPorCampoEIgnoraFirmas() throws Exception {
        PdfDocument pdfDocument = mock(PdfDocument.class);
        PdfAcroForm form = mock(PdfAcroForm.class);

        PdfFormField conEstados = mock(PdfFormField.class);
        when(conEstados.getAppearanceStates()).thenReturn(new String[]{"Si", "Off"});
        PdfFormField estadosNull = mock(PdfFormField.class);
        when(estadosNull.getAppearanceStates()).thenReturn(null);
        PdfFormField estadosVacios = mock(PdfFormField.class);
        when(estadosVacios.getAppearanceStates()).thenReturn(new String[0]);
        PdfSignatureFormField firma = mock(PdfSignatureFormField.class);

        Map<String, PdfFormField> campos = new LinkedHashMap<>();
        campos.put("casilla", conEstados);
        campos.put("nombre", estadosNull);
        campos.put("apellidos", estadosVacios);
        campos.put("campoFirma", firma);
        when(form.getAllFormFields()).thenReturn(campos);

        try (MockedStatic<PdfAcroForm> acroForm = Mockito.mockStatic(PdfAcroForm.class)) {
            acroForm.when(() -> PdfAcroForm.getAcroForm(pdfDocument, false)).thenReturn(form);

            String resultado = toStringFieldsDocumento(pdfDocument);

            assertTrue(resultado.contains("casilla"));
            assertTrue(resultado.contains("[Si, Off]"));
            assertTrue(resultado.contains("nombre"));
            assertTrue(resultado.contains("apellidos"));
            assertFalse(resultado.contains("campoFirma"));
            verify(firma, never()).getAppearanceStates();
        }
    }

    @Test
    void toStringFieldsDocumento_siFallaAlLeerElFormulario_devuelveTablaDeError() throws Exception {
        PdfDocument pdfDocument = mock(PdfDocument.class);
        PdfAcroForm form = mock(PdfAcroForm.class);
        when(form.getAllFormFields()).thenThrow(new IllegalStateException("formulario roto"));

        try (MockedStatic<PdfAcroForm> acroForm = Mockito.mockStatic(PdfAcroForm.class)) {
            acroForm.when(() -> PdfAcroForm.getAcroForm(pdfDocument, false)).thenReturn(form);

            String resultado = toStringFieldsDocumento(pdfDocument);

            assertTrue(resultado.contains("Campos del formulario del documento"));
            assertTrue(resultado.contains("Error"));
            assertTrue(resultado.contains("formulario roto"));
        }
    }
}
