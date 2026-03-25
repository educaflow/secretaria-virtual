package com.educaflow.base.infrastructure.autofirma;

import com.axelor.db.Model;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.ReflectionUtil;
import com.educaflow.base.util.TextUtil;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoFirma {

    private static final int AUTOFIRMA_Y_OFFSET=-6;

    private final Class clazz;
    private Rectangulo rectangulo;
    private String nif=null;
    private String motivo =null;
    private List<SourceTargetField> sourceTargetFields=new ArrayList<>();
    private String sufijo="_signed";
    private int pageNumber= CampoFirma.DEFAULT_NUMERO_PAGINA;
    private int fontSize=CampoFirma.DEFAULT_FONT_SIZE;

    public AutoFirma(Class clazz) {
        this.clazz = clazz;
    }

    public static void sendToActionResponse(AutoFirma autofirma, ActionResponse actionResponse) {

        if (autofirma.getSourceTargetFields() == null || autofirma.getSourceTargetFields().isEmpty()) {
            throw new RuntimeException("El campo sourceTargetFields no puede estar vacio");
        }
        if (autofirma.getRectangulo() == null) {
            throw new RuntimeException("El campo rectangulo no puede estar vacio");
        }


        actionResponse.setValue("executeJs",true);
        actionResponse.setValue("methodJs","firmaController");
        actionResponse.setValue("payload",autofirma.getPayload());

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
    public AutoFirma addSourceTargetField(String sourceField,String targetField) {
        checkFieldExists(sourceField);
        checkFieldExists(targetField);

        SourceTargetField sourceTargetField=new SourceTargetField(sourceField, targetField);
        sourceTargetFields.add(sourceTargetField);

        return this;
    }

    public List<SourceTargetField> getSourceTargetFields() {
        return sourceTargetFields;
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


    private Map<String,Object> getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nif", this.getNif());
        payload.put("motivo", this.getMotivo());
        payload.put("sourceTargetFields", this.getSourceTargetFields());
        payload.put("sufijo", this.getSufijo());
        payload.put("pageNumber", this.getPageNumber());
        payload.put("fontSize", this.getFontSize());
        Rectangulo rectangulo = this.getRectangulo();
        payload.put("signaturePositionOnPageLowerLeftX", rectangulo.x());
        payload.put("signaturePositionOnPageLowerLeftY", rectangulo.y()+AUTOFIRMA_Y_OFFSET);
        payload.put("signaturePositionOnPageUpperRightX", rectangulo.x() + rectangulo.width());
        payload.put("signaturePositionOnPageUpperRightY", rectangulo.y()+AUTOFIRMA_Y_OFFSET + rectangulo.height());

        return payload;
    }


    private void checkFieldExists(String fieldName) {
        String[] parts = fieldName.split("\\.");
        Class<? extends Model> currentClass = clazz;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            boolean isList = part.contains("[");
            String cleanPart = isList ? part.substring(0, part.indexOf('[')) : part;

            String getMethodName = "get" + TextUtil.toFirstsLetterToUpperCase(cleanPart);
            String setMethodName = "set" + TextUtil.toFirstsLetterToUpperCase(cleanPart);

            Method getMethod = ReflectionUtil.getMethod(currentClass, getMethodName, null, null, null);
            if (getMethod == null) {
                throw new RuntimeException("El getter " + getMethodName + " no existe en " + currentClass.getName()+ " en " + fieldName);
            }

            if (i == parts.length - 1) {
                if (!ReflectionUtil.hasMethod(currentClass, setMethodName, null, null, null)) {
                    throw new RuntimeException("El setter " + setMethodName + " no existe en " + currentClass.getName()+ " en " + fieldName);
                }
            }

            Class<?> returnType = getMethod.getReturnType();

            if (isList) {
                int openBracket = part.indexOf('[');
                int closeBracket = part.indexOf(']');

                if (closeBracket == -1 || closeBracket < openBracket) {
                    throw new RuntimeException("Formato de índice inválido en '" + part + "', se esperaba '[n]'"+ " en " + fieldName);
                }

                String indexStr = part.substring(openBracket + 1, closeBracket).trim();

                try {
                    int index = Integer.parseInt(indexStr);
                    if (index < 0) {
                        throw new RuntimeException("El índice en '" + part + "' debe ser mayor o igual a 0, se obtuvo: " + index+ " en " + fieldName);
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException("El índice en '" + part + "' no es un número válido: '" + indexStr + "'"+ " en " + fieldName);
                }

                if (!List.class.isAssignableFrom(returnType)) {
                    throw new RuntimeException("El campo '" + cleanPart + "' en " + currentClass.getName() + " no es una List, no se puede usar índice []"+ " en " + fieldName);
                }

                Type genericReturnType = getMethod.getGenericReturnType();
                if (!(genericReturnType instanceof ParameterizedType)) {
                    throw new RuntimeException("La List del campo '" + cleanPart + "' en " + currentClass.getName() + " no tiene tipo genérico definido"+ " en " + fieldName);
                }

                Type typeArgument = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
                if (!(typeArgument instanceof Class)) {
                    throw new RuntimeException("El tipo genérico de la List del campo '" + cleanPart + "' en " + currentClass.getName() + " no es una clase concreta"+ " en " + fieldName);
                }

                currentClass = (Class<? extends Model>) typeArgument;
            } else {
                currentClass = (Class<? extends Model>) returnType;
            }
        }
    }


    private record SourceTargetField(String sourceField, String targetField){};

}
