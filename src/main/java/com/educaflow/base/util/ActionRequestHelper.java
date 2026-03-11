package com.educaflow.base.util;

import com.axelor.db.Model;
import com.axelor.rpc.ActionRequest;

import java.util.Map;

public class ActionRequestHelper {
    private final ActionRequest request;
    private final Class expectedModelClass;

    public ActionRequestHelper(ActionRequest request) {
        this(request, null);
    }

    public ActionRequestHelper(ActionRequest request,Class expectedModelClass) {


        this.request = request;
        this.expectedModelClass=expectedModelClass;


        if (expectedModelClass!=null) {
            Class actualModelClass=this.getModelClass();

            if (actualModelClass==null) {
                throw new RuntimeException("El _model del ActionRequest es null");
            }

            if (expectedModelClass.getClass()!=actualModelClass.getClass()) {
                throw new RuntimeException("El classModel no coincide con el _model del requestData:"+ expectedModelClass.getCanonicalName() + "!=" + actualModelClass.getCanonicalName());
            }
        }

    }

    public Map<String, Object> getRequestData() {
        Map<String, Object> requestcontext = (Map<String, Object>) request.getData().get("context");

        if (requestcontext == null) {
            throw new RuntimeException("requestcontext es null");
        }
        return requestcontext;
    }

    public Long getId() {
        Map<String, Object> requestData = getRequestData();
        Object idObject = requestData.get("id");

        if (idObject == null) {
            return null;
        } else {
            return Convert.objectToLong(idObject);
        }

    }

    public long getParentId() {
        Map<String, Object> requestData = getRequestData();
        Object idObject = ((Map<String, Object>) requestData.get("_parent")).get("id");

        if (idObject == null) {
            throw new RuntimeException("idObject es null");
        }

        return Convert.objectToLong(idObject);
    }


    public String getEventName() {

        String eventName = (String) getRequestData().get("_signal");

        if (eventName == null) {
            throw new RuntimeException("eventName is null");
        }

        return eventName;
    }

    public String getProfileName() {

        String profileName = (String) getRequestData().get("_profile");
        if (profileName == null) {
            throw new RuntimeException("_profile is null");
        }
        if (profileName.isBlank()) {
            throw new RuntimeException("_profile is blank");
        }

        return profileName;

    }

    public String getParentSource() {

        String parentSource=(String)((Map<String,Object>) getRequestData().get("_parent")).get("_source");

        return parentSource;

    }


    public Class<? extends Model> getModelClass() {
        Map<String, Object> requestData = getRequestData();
        String modelName = (String) requestData.get("_model");

        if (modelName == null) {
            throw new RuntimeException("modelName is null");
        }

        try {
            return (Class<? extends Model>) Class.forName(modelName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo encontrar la clase del modelo: " + modelName, e);
        }
    }

}
