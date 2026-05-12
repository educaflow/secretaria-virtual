package com.educaflow.base.infrastructure.validation.messages.internal;

import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusinessMessageHelper {

    public static String KEY_MAP_ERROR_MENSAJES="errorMensajes";

    public static Map<String,List<Map<String,String>>> getAsMap(BusinessMessages businessMessages) {
        List<Map<String,String>> errorMensajes=getAsList(businessMessages);

        return Map.of(KEY_MAP_ERROR_MENSAJES,errorMensajes);
    }

    public static List<Map<String,String>> getAsList(BusinessMessages businessMessages) {
        List<Map<String,String>> errorMensajes=new ArrayList<>();

        if (businessMessages!=null)  {
            for (BusinessMessage businessMessage : businessMessages.removeDuplicates()) {
                Map<String, String> errorMensaje = getAsMap(businessMessage);
                errorMensajes.add(errorMensaje);
            }
        }

        return errorMensajes;
    }

    private static Map<String,String> getAsMap(BusinessMessage businessMessage) {
        String fieldName = businessMessage.getFieldName();
        String message = businessMessage.getMessage();
        String label = businessMessage.getLabel();

        Map<String, String> errorMensaje = new HashMap<>();
        errorMensaje.put("fieldName", fieldName);
        errorMensaje.put("message", message);
        errorMensaje.put("label", label);

        return errorMensaje;
    }

}

