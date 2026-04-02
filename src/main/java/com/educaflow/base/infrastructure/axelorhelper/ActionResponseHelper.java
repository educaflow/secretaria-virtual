package com.educaflow.base.infrastructure.axelorhelper;

import com.axelor.db.Model;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionResponseHelper {

    private final ActionResponse response;

    public ActionResponseHelper(ActionResponse response) {
        this.response = response;
    }

    public void doResponseViewForm(String viewName, Class<? extends Model> modelClass, Model entity, String title, String profile) {
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

    public void doResponseViewGrid(String viewName, Class<? extends Model> modelClass) {
        ActionView.ActionViewBuilder actionViewBuilder=ActionView.define("Hola")
                .model(modelClass.getName())
                .add("grid", viewName)
                .name("Pepe");

        response.setView(actionViewBuilder.map());
    }

    public void doResponseBusinessMessagesAsError(String title, BusinessMessages businessMessages) {
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

    public void doResponseBusinessMessages(BusinessMessages businessMessages) {
        storeBusinessMessagesInActionResponse(businessMessages);
    }

    private void storeBusinessMessagesInActionResponse(BusinessMessages businessMessages) {
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
