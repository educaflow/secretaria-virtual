package com.educaflow.common.util;

import com.axelor.db.Model;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.rpc.ActionResponse;
import com.educaflow.common.validation.messages.BusinessMessage;
import com.educaflow.common.validation.messages.BusinessMessages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AxelorViewUtil {
    public static void doResponseViewForm(ActionResponse response, String viewName, Class<? extends Model> modelClass, Model entity, String title, String profile) {
        ActionView.ActionViewBuilder actionViewBuilder=ActionView.define(title)
                .model(modelClass.getName())
                .add("form", viewName)
                .param("forceEdit", "true")
                .param("forceTitle", "true")
                .param("show-confirm", "false")
                .param("show-toolbar", "false")
                .context("_profile",profile);

        if ((entity != null)  && (entity.getId() != null)) {
            actionViewBuilder.context("_showRecord", entity.getId()).param("forceEdit", "true");
        } else {
            actionViewBuilder.context("newEntity", entity);
        }


        response.setView(actionViewBuilder.map());
    }

    public static void doResponseViewGrid(ActionResponse response, String viewName, Class<? extends Model> modelClass) {
        ActionView.ActionViewBuilder actionViewBuilder=ActionView.define("Hola")
                .model(modelClass.getName())
                .add("grid", viewName)
                .name("Pepe");

        response.setView(actionViewBuilder.map());
    }

    public static boolean existsView(String name, String type, String model) {
        AbstractView data = XMLViews.findView(name, type, model);
        if (data==null) {
            return false;
        } else {
            return true;
        }
    }

    public static void doResponseBusinessMessagesAsError(ActionResponse response,String title, BusinessMessages businessMessages) {
        StringBuilder sb= new StringBuilder();
        for(BusinessMessage businessMessage : businessMessages) {
            if (sb.length()>0) {
                sb.append("<br>");
            }

            if ((businessMessage.getLabel()!=null) && (!businessMessage.getLabel().isEmpty())) {
                sb.append("<strong>").append(businessMessage.getLabel()).append(": ").append("</strong>").append(businessMessage.getMessage());
            } else {
                sb.append(businessMessage.getMessage());
            }
        }
        response.setError(sb.toString(),title);
    }

    public static void doResponseBusinessMessages(ActionResponse response, BusinessMessages businessMessages) {

/***
        Map<String, String> mapErrors = new HashMap<>();
        for(BusinessMessage businessMessage : businessMessages) {
            String fieldName = businessMessage.getFieldName();
            String message = businessMessage.getMessage();
            String type = businessMessage.getType();

            if (type.startsWith("Required")) {
                mapErrors.put(fieldName, message);
            }
        }
        response.setErrors(mapErrors);
***/

        storeBusinessMessagesInActionResponse(response, businessMessages);

    }

    private static void storeBusinessMessagesInActionResponse(ActionResponse response, BusinessMessages businessMessages) {
        List<Map<String,String>> errorMensajes=new ArrayList<>();

        if (businessMessages!=null)  {
            for (BusinessMessage businessMessage : businessMessages.removeDuplicates()) {
                String fieldName = businessMessage.getFieldName();
                String message = businessMessage.getMessage();
                String label = businessMessage.getLabel();

                Map<String, String> errorMensaje = new HashMap<>();
                errorMensaje.put("fieldName", fieldName);
                errorMensaje.put("message", message);
                errorMensaje.put("label", label);
                errorMensajes.add(errorMensaje);
            }
        }
        response.setValue("errorMensajes",errorMensajes);

    }



}
