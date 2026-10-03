package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;

import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InstanceModelList {
    
    private final Map<String, List<Model>> instances;

    public InstanceModelList() {
        instances=new HashMap<>();
    }
    
    public void addInstanceModel(Class clazz,Model model) {
        Objects.requireNonNull(model, "model no puede ser null");
        String fqcn = clazz.getName();
        instances.computeIfAbsent(fqcn, k -> new java.util.ArrayList<>()).add(model);
    }

    public Optional<Model> getInstance(Class clazz, Long id) {
        Objects.requireNonNull(clazz, "clazz no puede ser null");
        if (id == null) {
            return Optional.empty();
        }
        String fqcn = clazz.getName();
        List<Model> list = instances.get(fqcn);
        if (list == null) {
            return Optional.empty();
        }
        return list.stream()
                .filter(m -> id.equals(m.getId()))
                .findFirst();
    }
}
