package com.educaflow.base.infrastructure.autofirma;

import com.axelor.db.Model;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.ReflectionUtil;
import com.educaflow.base.util.TextUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AutoFirma {

    private static final int AUTOFIRMA_Y_OFFSET=-6;

    private final Class clazz;
    private Rectangulo rectangulo;
    private String nif=null;
    private String motivo =null;
    private String sourceField;
    private String targetField;
    private String sufijo="_signed";
    private int pageNumber= CampoFirma.DEFAULT_NUMERO_PAGINA;
    private int fontSize=CampoFirma.DEFAULT_FONT_SIZE;

    public AutoFirma(Class clazz) {
        this.clazz = clazz;
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
        payload.put("sourceFieldClass", getModelClassFromField(autofirma.getClazz(), autofirma.getSourceField()).getName());
        payload.put("targetField", autofirma.getTargetField());
        payload.put("targetFieldClass", getModelClassFromField(autofirma.getClazz(),autofirma.getTargetField()).getName());
        payload.put("sufijo", autofirma.getSufijo());
        payload.put("pageNumber", autofirma.getPageNumber());
        payload.put("fontSize", autofirma.getFontSize());
        Rectangulo rectangulo = autofirma.getRectangulo();
        payload.put("signaturePositionOnPageLowerLeftX", rectangulo.x());
        payload.put("signaturePositionOnPageLowerLeftY", rectangulo.y()+AUTOFIRMA_Y_OFFSET);
        payload.put("signaturePositionOnPageUpperRightX", rectangulo.x() + rectangulo.width());
        payload.put("signaturePositionOnPageUpperRightY", rectangulo.y()+AUTOFIRMA_Y_OFFSET + rectangulo.height());




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

    public Class<? extends Model> getClazz() {
        return clazz;
    }


    private void checkFieldExists(String fieldName) {
        String[] parts = fieldName.split("\\.");
        Class<? extends Model> currentClass = clazz;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String getMethodName = "get" + TextUtil.toFirstsLetterToUpperCase(part);
            String setMethodName = "set" + TextUtil.toFirstsLetterToUpperCase(part);

            Method getMethod = ReflectionUtil.getMethod(currentClass, getMethodName, null,null,null);
            if (getMethod == null) {
                throw new RuntimeException("El getter " + getMethodName + " no existe en " + currentClass.getName());
            }

            if (i == parts.length - 1) {
                if (!ReflectionUtil.hasMethod(currentClass, setMethodName, null, null, null)) {
                    throw new RuntimeException("El setter " + setMethodName + " no existe en " + currentClass.getName());
                }
            }

            currentClass = (Class<? extends Model>)getMethod.getReturnType();
        }
    }

    private static Class<?> getModelClassFromField(Class<?> expedienteClass, String fieldName) {
        String[] parts = fieldName.split("\\.");
        Class<?> currentClass = expedienteClass;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String getMethodName = "get" + TextUtil.toFirstsLetterToUpperCase(part);

            Method method = ReflectionUtil.getMethod(currentClass, getMethodName, null, null, null);

            if (method == null) {
                throw new RuntimeException("No se pudo encontrar el método " + getMethodName + " en la clase " + currentClass.getName());
            }

            // Actualizamos la clase actual con el tipo de retorno del getter
            currentClass = method.getReturnType();
        }

        return currentClass;
    }


}
