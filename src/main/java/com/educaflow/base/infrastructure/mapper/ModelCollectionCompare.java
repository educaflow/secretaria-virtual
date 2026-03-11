package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;

import java.util.*;
import java.util.stream.Collectors;

public class ModelCollectionCompare {

    private final Collection<Object> sources;
    private final Collection<Model> targets;


    public ModelCollectionCompare(Collection<Object> sources, Collection<Model> targets) {
        this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
        this.targets = targets != null ? new ArrayList<>(targets) : new ArrayList<>();

    }

    public Collection<Model> getTargetWhereOnlyTarget() {
        return targets.stream()
                .filter(target ->
                    (target.getId() == null) ||
                    sources.stream().noneMatch(source -> getId(source) != null && getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toSet());
    }

    public Collection<Object> getSourceWhereOnlySource() {
        return sources.stream()
                .filter(source ->
                    (getId(source) == null) ||
                    targets.stream().noneMatch(target -> target.getId() != null && getId(source).longValue() == target.getId().longValue() )
                ).collect(Collectors.toSet());
    }

    public List<SourceTargetEntities> getInSourceAndInTarget() {
        return sources.stream()
                .filter(source -> getId(source) != null)
                .flatMap(source -> targets.stream()
                        .filter(target -> target.getId() != null)
                        .filter(target -> getId(source).longValue() == target.getId().longValue())
                        .map(target -> new SourceTargetEntities(source, target))
                )
                .collect(Collectors.toList());
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

    public record SourceTargetEntities(Object source, Model target) {}

}
