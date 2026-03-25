package com.educaflow.base.util;

import java.util.HashMap;
import java.util.Map;

public class AllowProperties {

    private final Map<String,Object> allowProperties;


    public static AllowProperties createAllowProperties(Map<String,Object> allowProperties) {
        return new AllowProperties(allowProperties);
    }

    public static AllowProperties createAllowAllProperties() {
        Map<String,Object> allowAllProperties=new HashMap<>();
        allowAllProperties.put("*",null);
        return new AllowProperties(allowAllProperties);
    }

    public AllowProperties(Map<String,Object> allowProperties) {
        this.allowProperties=allowProperties;
    }


    public boolean allowProperty(String propertyName) {
        if (allowProperties==null) {
            return false;
        }

        if ("class".equals(propertyName)) {
            return false;
        }

        if (allowProperties.containsKey(propertyName) == true) {
            return true;
        }

        if (allowProperties.containsKey("*") == true) {
            if (allowProperties.get("*")!=null) {
                throw new RuntimeException("Si se permite todas las propiedades con '*', no se pueden especificar propiedades concretas dentro de '*'");
            }
            if (allowProperties.size()!=1) {
                throw new RuntimeException("Si se permite todas las propiedades con '*', solo puede haber en el Map un '*'");
            }

            return true;
        }

        return false;
    }


    public AllowProperties innerAllowProperties(String propertyName) {
        if (allowProperties==null) {
            return null;
        }

        if (allowProperties.containsKey("*") == true) {
            if (allowProperties.get("*")!=null) {
                throw new RuntimeException("Si se permite todas las propiedades con '*', no se pueden especificar propiedades concretas dentro de '*'");
            }
            if (allowProperties.size()!=1) {
                throw new RuntimeException("Si se permite todas las propiedades con '*', solo puede haber en el Map un '*'");
            }

            return createAllowAllProperties();
        } else {
            return createAllowProperties((Map<String,Object>)allowProperties.get(propertyName));
        }
    }
}
