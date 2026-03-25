package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstanceModelList {
    
    private final Map<String, List<Model>> instances;

    public InstanceModelList() {
        instances=new HashMap<>();
    }
    
    public void addInstanceModel(Class clazz,Model model) {
        if (model == null) {
            throw new IllegalArgumentException("El model no puede ser null");
        }
        String fqcn = clazz.getName();
        instances.computeIfAbsent(fqcn, k -> new java.util.ArrayList<>()).add(model);
    }

    public Model getInstance(Class clazz, Long id) {
        if (clazz == null) {
            throw new IllegalArgumentException("El clazz no puede ser null");
        }
        if (id == null) {
            return null;
        }
        String fqcn = clazz.getName();
        List<Model> list = instances.get(fqcn);
        if (list == null) {
            return null;
        }
        return list.stream()
                .filter(m -> id.equals(m.getId()))
                .findFirst()
                .orElse(null);
    }
    
    public boolean existsInstance(Class clazz, Long id) {
        return getInstance(clazz, id) != null;
    }
}
