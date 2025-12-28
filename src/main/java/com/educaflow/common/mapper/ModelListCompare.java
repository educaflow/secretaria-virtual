package com.educaflow.common.mapper;

import com.axelor.db.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelListCompare {

    private final List<Object> sources;
    private final List<Model> targets;


    public ModelListCompare(List<Object> sources, List<Model> targets) {
        this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
        this.targets = targets != null ? new ArrayList<>(targets) : new ArrayList<>();

    }

    public List<Model> getTargetWhereOnlyTarget() {
        return targets.stream()
                .filter(target ->
                    (target.getId() == null) ||
                    sources.stream().noneMatch(source -> getId(source) != null && getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toList());
    }

    public List<Object> getSourceWhereOnlySource() {
        return sources.stream()
                .filter(source ->
                    (getId(source) == null) ||
                    targets.stream().noneMatch(target -> target.getId() != null && getId(source).longValue() == target.getId().longValue() )
                ).collect(Collectors.toList());
    }

    public List<Object> getSourceWhereSourceAndTarget() {
        return sources.stream()
                .filter(source ->
                        getId(source) != null &&
                    targets.stream().anyMatch(target -> target.getId() != null && getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toList());
    }

    public List<Model> getTargetWhereSourceAndTarget() {
        return targets.stream()
                .filter(target ->
                    target.getId() != null &&
                    sources.stream().anyMatch(source -> getId(source) != null && getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toList());
    }
    
    
    private Long getId(Object object) {
        Long id;
        
        if (object==null) {
            throw  new IllegalArgumentException("Object is null");
        }
        
        if (object instanceof Model) {
            id = ((Model) object).getId();
        } else if (object instanceof Map) {
            id=ScalarMapper.getScalarFromObject(((Map)object).get("id"),Long.class);
        } else {
            throw  new IllegalArgumentException("Object no es Model ni es Map");
        }
        
        return id;
    }

}
