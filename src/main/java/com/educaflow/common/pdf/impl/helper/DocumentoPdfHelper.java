package com.educaflow.common.pdf.impl.helper;

import com.educaflow.common.pdf.DocumentoPdf;
import com.educaflow.common.pdf.ResultadoFirma;
import com.educaflow.common.pdf.impl.DocumentoPdfImplIText;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfWidgetAnnotation;
import com.itextpdf.kernel.xmp.XMPIterator;
import com.itextpdf.kernel.xmp.XMPMeta;
import com.itextpdf.kernel.xmp.XMPMetaFactory;
import com.itextpdf.kernel.xmp.properties.XMPPropertyInfo;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.verapdf.pdfa.flavours.PDFAFlavour;
/**
 *
 * @author logongas
 */
public class DocumentoPdfHelper {
    
    public static PdfDocument getPdfDocument(DocumentoPdf documentoPdf) {
        try {
            Field campo = DocumentoPdfImplIText.class.getDeclaredField("pdfDocument");
            campo.setAccessible(true);
            return (PdfDocument) campo.get(documentoPdf);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    public static String toString(DocumentoPdf documentoPdf) {
        PdfDocument pdfDocument=getPdfDocument(documentoPdf);

        StringBuilder sb=new StringBuilder();
        
        
        sb.append("\n\n========================================================================= \n\n");

        
        
        
        sb.append(toStringNombreFichero(documentoPdf.getFileName())).append("\n");
        sb.append(toStringFieldsDocumento(pdfDocument)).append("\n");
        sb.append(toStringFieldsPagina(pdfDocument)).append("\n");
        sb.append(toStringResultadoFirmas(documentoPdf)).append("\n");
        sb.append(toStringXMPMetadataDocumento(pdfDocument)).append("\n");
        sb.append(toStringXMPMetadataPagina(pdfDocument)).append("\n");
        sb.append(toStringFuentesPaginas(pdfDocument)).append("\n");
        sb.append(toStringFuentesFormulario(pdfDocument)).append("\n");
        sb.append(toStringFuentesFields(pdfDocument)).append("\n");
        sb.append(toStringOtrosDatos(pdfDocument)).append("\n");
        sb.append(toStringVeraPdf(documentoPdf.getDatos())).append("\n");
        
        return sb.toString();
    }
    
    private static String toStringNombreFichero(String fileName) {
        try {
            List<List<Object>> rows = new ArrayList<>();
            List<Object> row;

            row = new ArrayList<>();
            row.add(fileName);
            rows.add(row);


            return renderTable("Nombre del fichero", null, rows);
        } catch (Exception e) {
            return renderTable("Nombre del fichero", e);
        }

    }
    
    
    private static  String toStringFieldsDocumento(PdfDocument pdfDocument) {
        try {
            List<List<Object>> rows = new ArrayList<>();

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDocument, false);

            if (form != null) {
                Map<String, PdfFormField> pdfFormFields = form.getAllFormFields();

                for (Map.Entry<String, PdfFormField> entry : pdfFormFields.entrySet()) {
                    String name = entry.getKey();
                    PdfFormField pdfFormField = entry.getValue();
                    if (PdfDocumentHelper.isSignatureFormField(pdfFormField) == false) {

                        List<Object> row = new ArrayList<>();

                        row.add(name);
                        if ((pdfFormField.getAppearanceStates() != null) && (pdfFormField.getAppearanceStates().length > 0)) {
                            row.add(Arrays.toString(pdfFormField.getAppearanceStates()));
                        } else {
                            row.add("");
                        }

                        rows.add(row);
                    }
                }
            }

            return renderTable("Campos del formulario del documento", List.of("Nombre", "valores"), rows);
        } catch (Exception e) {
            return renderTable("Campos del formulario del documento", e);
        }
    }
    
    
    private static  String toStringFieldsPagina(PdfDocument pdfDocument) {
        try {
            List<List<Object>> rows=new ArrayList<>();

            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                PdfPage page = pdfDocument.getPage(i);

                for (PdfAnnotation pdfAnnotation : page.getAnnotations()) {
                    if (pdfAnnotation.getSubtype().equals(PdfName.Widget)) {
                        PdfWidgetAnnotation widget = (PdfWidgetAnnotation) pdfAnnotation;

                        PdfFormField pdfFormField = PdfFormField.makeFormField(widget.getPdfObject(), pdfDocument);
                        if (pdfFormField!=null) {
                            if (PdfDocumentHelper.isSignatureFormField(pdfFormField) == false) {

                                List<Object> row=new ArrayList<>();

                                row.add(i);
                                row.add(pdfFormField.getFieldName());
                                if ((pdfFormField.getAppearanceStates() != null) && (pdfFormField.getAppearanceStates().length > 0)) {
                                    row.add(Arrays.toString(pdfFormField.getAppearanceStates()));
                                } else {
                                    row.add("");
                                }

                                rows.add(row);
                            }
                        } else {
                            List<Object> row=new ArrayList<>();
                            row.add(i);
                            row.add(null);
                            row.add("PdfWidgetAnnotation sin PdfFormField asociado");

                            rows.add(row);
                        }
                    }
                }

            }


            return renderTable("Campos sueltos por página",List.of("Página","Nombre","valores"),rows);
        } catch (Exception e) {
            return renderTable("Campos sueltos por página",e);
        }
    }    
    
    
    
    private static  String toStringFuentesFields(PdfDocument pdfDocument) {
        try {
            List<List<Object>> rows = new ArrayList<>();


            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDocument, false);
            if (acroForm != null) {
                Map<String, PdfFormField> fields = acroForm.getAllFormFields();
                for (PdfFormField field : fields.values()) {
                    PdfDictionary fieldResources = field.getPdfObject().getAsDictionary(PdfName.DR);

                    if (fieldResources != null) {
                        addFontToRows(fieldResources, field.getFieldName(), rows);
                    }
                }

            }


            return renderTable("Fuentes de los campos del formulario", List.of("Campo", "Nombre", "Tipo", "Base", "Incrustada"), rows);
        } catch (Exception e) {
            return renderTable("Fuentes de los campos del formulario", e);
        }
    }   
    
    
    private static  String toStringFuentesFormulario(PdfDocument pdfDocument) {
        try {
            List<List<Object>> rows = new ArrayList<>();


            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDocument, false);
            if (acroForm != null) {
                PdfDictionary formResources = acroForm.getPdfObject().getAsDictionary(PdfName.DR);
                if (formResources != null) {
                    addFontToRows(formResources, null, rows);
                }
            }


            return renderTable("Fuentes del formulario", List.of("Nombre", "Tipo", "Base", "Incrustada"), rows);
        } catch (Exception e) {
            return renderTable("Fuentes del formulario", e);
        }
    }     
    
    private static  String toStringFuentesPaginas(PdfDocument pdfDocument) {
        try {
            List<List<Object>> rows=new ArrayList<>();

            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                PdfPage page = pdfDocument.getPage(i);


                PdfDictionary pageResources = page.getResources().getPdfObject();

                if (pageResources!=null) {
                    addFontToRows(pageResources, i, rows);
                }
            }

            return renderTable("Fuentes de páginas",List.of("Página","Nombre","Tipo","Base","Incrustada"),rows);
        } catch (Exception e) {
            return renderTable("Fuentes de páginas",e);
        }
    }    
    
    
    
    
    
    private static  String toStringResultadoFirmas(DocumentoPdf documentoPdf) {
        try {
            List<List<Object>> rows = new ArrayList<>();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            for (ResultadoFirma resultadoFirma : documentoPdf.getFirmasPdf()) {
                List<Object> row = new ArrayList<>();


                row.add(resultadoFirma.getFechaFirma());
                row.add(resultadoFirma.isCorrecta());
                row.add(resultadoFirma.getDatosCertificado().isValidoEnListaCertificadosConfiables());
                row.add(resultadoFirma.getDatosCertificado().isSelloTiempo());
                row.add(resultadoFirma.getNombreCampo());
                row.add(resultadoFirma.getDatosCertificado().getCnSubject());
                row.add(resultadoFirma.getDatosCertificado().getNombre());
                row.add(resultadoFirma.getDatosCertificado().getApellidos());
                row.add(resultadoFirma.getDatosCertificado().getDNI());
                row.add(resultadoFirma.getDatosCertificado().getCif());
                row.add(resultadoFirma.getDatosCertificado().getCnIssuer());
                row.add(resultadoFirma.getDatosCertificado().getTipoEmisorCertificado());
                row.add(resultadoFirma.getDatosCertificado().getTipoCertificado());
                row.add(simpleDateFormat.format(resultadoFirma.getDatosCertificado().getValidoNoAntesDe()));
                row.add(simpleDateFormat.format(resultadoFirma.getDatosCertificado().getValidoNoDespuesDe()));
                row.add(resultadoFirma.getMotivo());


                rows.add(row);
            }

            return renderTable("Firmas", List.of("Fecha Firma", "Correcta", "Valido según TSL", "Es sello tiempo", "Nombre Campo", "CN Firmante", "Nombre", "Apellidos", "DNI", "CIF", "CN Emisor", "Tipo emisor certificado", "Tipo Certificado", "Fecha inicio", "Fecha fin","Motivo"), rows);
        } catch (Exception e) {
            return renderTable("Firmas", e);
        }
        
    }
    
    private static  String toStringXMPMetadataDocumento(PdfDocument pdfDocument) {
        try {
            List<List<Object>> rows=new ArrayList<>();
        
            XMPMeta xmpMeta;
            try {
                xmpMeta = pdfDocument.getXmpMetadata();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (xmpMeta!=null) {
                XMPIterator iterator = xmpMeta.iterator();
                while (iterator.hasNext()) {
                    XMPPropertyInfo prop = (XMPPropertyInfo) iterator.next();
                    List<Object> row=new ArrayList<>();

                    row.add(prop.getPath());
                    row.add(prop.getValue());
                    row.add(prop.getNamespace()); 

                    rows.add(row); 

                }
            }
        
            return renderTable("MXP Documento",List.of("Path","Value","Namespace"),rows);            

        } catch (Exception e) {
            return renderTable("MXP Documento",e);
        }
    }    
    
    private static  String toStringXMPMetadataPagina(PdfDocument pdfDocument) {
        try {
            List<List<Object>> rows=new ArrayList<>();
        
       
            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                PdfPage page = pdfDocument.getPage(i);

                PdfStream pageMetaStream = page.getXmpMetadata();
                if (pageMetaStream!=null) {
                    byte[] bytes = pageMetaStream.getBytes();
                    XMPMeta xmpMeta = XMPMetaFactory.parseFromBuffer(bytes);                    
                    
                    XMPIterator iterator = xmpMeta.iterator();
                    while (iterator.hasNext()) {
                        XMPPropertyInfo prop = (XMPPropertyInfo) iterator.next();
                        List<Object> row=new ArrayList<>();

                        row.add(i);
                        row.add(prop.getPath());
                        row.add(prop.getValue());
                        row.add(prop.getNamespace()); 

                        rows.add(row); 

                    }
                }
            }
            return renderTable("MXP Página",List.of("Página","Path","Value","Namespace"),rows);            

        } catch (Exception e) {
            return renderTable("MXP Página",e);
        }
    }    
    
    
    private static  String toStringOtrosDatos(PdfDocument pdfDocument) {
        try {
            List<List<Object>> rows = new ArrayList<>();
            List<Object> row;

            row = new ArrayList<>();
            row.add("Tiene OutputIntent");
            row.add(PdfDocumentHelper.hasOutputIntent(pdfDocument));
            rows.add(row);


            row = new ArrayList<>();
            row.add("Valor PdfConformance");
            row.add(PdfDocumentHelper.getPdfConformance(pdfDocument));
            rows.add(row);

            return renderTable("Otros Datos", List.of("Clave", "Valor"), rows);
        } catch (Exception e) {
            return renderTable("Otros Datos", e);
        }

    }
    private static  String toStringVeraPdf(byte[] datos) {
        try {
            List<List<Object>> rows = new ArrayList<>();

            Map<PDFAFlavour, List<String>> pdfAFlavourValid = VeraPdfHelper.getPDFAFlavourValid(datos);
            for (Map.Entry<PDFAFlavour, List<String>> entry : pdfAFlavourValid.entrySet()) {

                PDFAFlavour pdfAFlavour = entry.getKey();
                List<String> mensajesFallo = entry.getValue();

                if (mensajesFallo == null) {
                    List<Object> row = new ArrayList<>();
                    row.add(pdfAFlavour);
                    row.add("Correcto");
                    rows.add(row);
                } else {
                    for (String mensajeFallo : mensajesFallo) {
                        List<Object> row = new ArrayList<>();
                        row.add(pdfAFlavour);
                        row.add(mensajeFallo);
                        rows.add(row);
                    }
                }
            }

            return renderTable("Verificacion PDF/A Conformance con VeraPdf", List.of("Versión", "Resultado"), rows);
        } catch (Exception e) {
            return renderTable("Verificacion PDF/A Conformance con VeraPdf", e);
        }


    }

    private static  String renderTable(String tableName,Exception ex) {
        List<List<Object>> rows=new ArrayList<>();

        for(String trace:getStackTrace(ex,0)) {
            List<Object> row=new ArrayList<>();
            row.add(trace);
            rows.add(row);
        }

        return renderTable(tableName,List.of("Error"),rows);
    }

    private static  String renderTable(String tableName,List<String> heads,List<List<Object>> rows) {

        List<String> titulo=new ArrayList<>();
        if ((heads!=null) && (heads.isEmpty()==false)) {
            for(int i=0;i<heads.size()-1;i++) {
                titulo.add(null);
            }
        }

        titulo.add(tableName);
        
        AsciiTable at = new AsciiTable();
        at.addRule();
        at.addRow(titulo.toArray());
        if ((heads!=null) && (heads.isEmpty()==false)) {
            at.addRule();
            at.addRow(heads.toArray());
        }
        at.addRule();
        
        for (List<Object> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (row.get(i) == null) {
                    row.set(i, "__null__");
                }
            }
        }        
        
        if (rows.size()>0) {
            for(List<Object> row:rows) {
                at.addRow(row.toArray());
            }
            at.addRule();
        }
        at.getRenderer().setCWC(new CWC_LongestLine());
        
        return at.render();
    }  
    
    
    private static  void addFontToRows(PdfDictionary pdfDictionary,Object key,List<List<Object>> rows) {       
        PdfDictionary fontDictionary = pdfDictionary.getAsDictionary(PdfName.Font);

        if (fontDictionary != null) {

            // Itera sobre cada entrada en el diccionario de fuentes
            for (Map.Entry<PdfName, PdfObject> entry : fontDictionary.entrySet()) {

                PdfName fontName = entry.getKey();
                PdfDictionary fontDict = (PdfDictionary) entry.getValue();
                PdfName subtype = fontDict.getAsName(PdfName.Subtype);
                PdfName baseFont = fontDict.getAsName(PdfName.BaseFont);
                PdfObject fontDescriptor = fontDict.get(PdfName.FontDescriptor);

                List<Object> row=new ArrayList<>();
                
                if (key!=null) {
                    row.add(key);
                }
                row.add(fontName);
                row.add(subtype != null ? subtype.getValue() : "Desconocido");
                row.add(baseFont != null ? baseFont.getValue() : "Desconocido");
                row.add(fontDescriptor != null);

               
                rows.add(row);
            }
        }       
        
    }

    private static List<String> getStackTrace(Throwable ex,int deep) {
        String tabulador="\u00B7".repeat(4);

        List<String> stackTrace=new ArrayList<>();

        if (deep==0) {
            stackTrace.add(ex.getLocalizedMessage());
        }

        for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
            stackTrace.add(tabulador.repeat(deep)+stackTraceElement.toString());
        }

        if (ex.getCause()!=null) {
            stackTrace.add(tabulador.repeat(deep)+"Caused by:"+ex.getCause().getLocalizedMessage());
            stackTrace.addAll(getStackTrace(ex.getCause(),deep+1));
        }

        return stackTrace;
    }
}
