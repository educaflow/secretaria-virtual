package com.educaflow.base.infrastructure.axelorhelper;

import com.axelor.db.Model;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionResponse;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.infrastructure.validation.messages.internal.BusinessMessageHelper;

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

    public void doResponseBusinessMessagesAsError(BusinessMessages businessMessages) {
            doResponseBusinessMessagesAsError(null,  businessMessages);
    }

    public void doResponseBusinessMessagesAsError(String title, BusinessMessages businessMessages) {
        StringBuilder sb= new StringBuilder();
        sb.append("<ul>");
        for(BusinessMessage businessMessage : businessMessages) {
            sb.append("<li>");

            if ((businessMessage.getLabel()!=null) && (businessMessage.getLabel().isBlank()==false)) {
                sb.append("<strong>").append(businessMessage.getLabel()).append(": ").append("</strong>").append(businessMessage.getMessage());
            } else if ((businessMessage.getFieldName()!=null) && (businessMessage.getFieldName().isBlank()==false)) {
                sb.append("<strong>").append(businessMessage.getFieldName()).append(": ").append("</strong>").append(businessMessage.getMessage());
            } else {
                sb.append(businessMessage.getMessage());
            }

            sb.append("</li>");
        }
        sb.append("</ul>");

        if ((title!=null) && (title.isBlank()==false)) {
            response.setError(sb.toString(), title);
        } else {
            response.setError(sb.toString());
        }
    }

    public void doResponseBusinessMessages(BusinessMessages businessMessages) {
        storeBusinessMessagesInActionResponse(businessMessages);
    }

    private void storeBusinessMessagesInActionResponse(BusinessMessages businessMessages) {
        List<Map<String,String>> errorMensajes= BusinessMessageHelper.getAsList(businessMessages);


        response.setValue(BusinessMessageHelper.KEY_MAP_ERROR_MENSAJES,errorMensajes);
    }
}
