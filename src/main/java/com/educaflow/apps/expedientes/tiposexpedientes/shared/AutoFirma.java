package com.educaflow.apps.expedientes.tiposexpedientes.shared;

import com.axelor.db.Model;
import com.axelor.rpc.ActionResponse;
import com.educaflow.apps.expedientes.db.Expediente;
import com.educaflow.common.pdf.CampoFirma;
import com.educaflow.common.pdf.Rectangulo;
import com.educaflow.common.util.ReflectionUtil;
import com.educaflow.common.util.TextUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AutoFirma {

    private static final int AUTOFIRMA_Y_OFFSET=-6;

    private final Class<? extends Expediente> expedienteClass;
    private Rectangulo rectangulo;
    private String nif=null;
    private String motivo =null;
    private String sourceField;
    private String targetField;
    private String sufijo="_signed";
    private int pageNumber= CampoFirma.DEFAULT_NUMERO_PAGINA;
    private int fontSize=CampoFirma.DEFAULT_FONT_SIZE;

    public AutoFirma(Class<? extends Expediente> expedienteClass) {
        this.expedienteClass = expedienteClass;
    }

    public static void sendToActionResponse(AutoFirma autofirma, ActionResponse actionResponse) {

        if (autofirma.getSourceField() == null || autofirma.getSourceField().isEmpty()) {
            throw new RuntimeException("El campo sourceField no puede estar vacio");
        }
        if (autofirma.getTargetField() == null || autofirma.getTargetField().isEmpty()) {
            throw new RuntimeException("El campo targetField no puede estar vacio");
        }
        if (autofirma.getRectangulo() == null) {
            throw new RuntimeException("El campo rectangulo no puede estar vacio");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("nif", autofirma.getNif());
        payload.put("motivo", autofirma.getMotivo());
        payload.put("sourceField", autofirma.getSourceField());
        payload.put("sourceFieldClass", getModelClassFromField(autofirma.getExpedienteClass(), autofirma.getSourceField()).getName());
        payload.put("targetField", autofirma.getTargetField());
        payload.put("targetFieldClass", getModelClassFromField(autofirma.getExpedienteClass(),autofirma.getTargetField()).getName());
        payload.put("sufijo", autofirma.getSufijo());
        payload.put("pageNumber", autofirma.getPageNumber());
        payload.put("fontSize", autofirma.getFontSize());
        Rectangulo rectangulo = autofirma.getRectangulo();
        payload.put("signaturePositionOnPageLowerLeftX", rectangulo.getX());
        payload.put("signaturePositionOnPageLowerLeftY", rectangulo.getY()+AUTOFIRMA_Y_OFFSET);
        payload.put("signaturePositionOnPageUpperRightX", rectangulo.getX() + rectangulo.getWidth());
        payload.put("signaturePositionOnPageUpperRightY", rectangulo.getY()+AUTOFIRMA_Y_OFFSET + rectangulo.getHeight());




        actionResponse.setValue("executeJs",true);
        actionResponse.setValue("methodJs","signDocument");
        actionResponse.setValue("payload",payload);

    }

    public AutoFirma setRectangulo(Rectangulo rectangulo) {
        this.rectangulo = rectangulo;
        return this;
    }

    public Rectangulo getRectangulo() {
        return rectangulo;
    }

    public AutoFirma setNif(String nif) {
        this.nif = nif;
        return this;
    }
    public AutoFirma setMotivo(String motivo) {
        this.motivo = motivo;
        return this;
    }

    public String getNif() {
        return nif;
    }
    public String getMotivo() {
        return motivo;
    }
    public AutoFirma setSourceField(String sourceField) {
        checkFieldExists(sourceField);

        this.sourceField = sourceField;
        return this;
    }

    public String getSourceField() {
        return sourceField;
    }

    public AutoFirma setTargetField(String targetField) {
        checkFieldExists(targetField);

        this.targetField = targetField;
        return this;
    }

    public String getTargetField() {
        return targetField;
    }

    public AutoFirma setSufijo(String sufijo) {
        this.sufijo = sufijo;
        return this;
    }

    public String getSufijo() {
        return sufijo;
    }

    public AutoFirma setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public AutoFirma setFontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public int getFontSize() {
        return fontSize;
    }

    public Class<? extends Model> getExpedienteClass() {
        return expedienteClass;
    }


    private void checkFieldExists(String fieldName) {
        String getMethodName = "get" + TextUtil.toFirstsLetterToUpperCase(fieldName);
        String setMethodName = "set" + TextUtil.toFirstsLetterToUpperCase(fieldName);
        if (ReflectionUtil.hasMethod(expedienteClass, getMethodName,null,null,null)==false) {
            throw new RuntimeException("El método sourceField: " + getMethodName + " no existe en la clase: " + expedienteClass.getName());
        }
        if (ReflectionUtil.hasMethod(expedienteClass, setMethodName,null,null,null)==false) {
            throw new RuntimeException("El método sourceField: " + setMethodName + " no existe en la clase: " + expedienteClass.getName());
        }
    }

    private static Class getModelClassFromField(Class expedienteClass,String fieldName) {
        String getMethodName = "get" + TextUtil.toFirstsLetterToUpperCase(fieldName);
        Method method=ReflectionUtil.getMethod(expedienteClass, getMethodName,null,null,null);

        return method.getReturnType();
    }


}
