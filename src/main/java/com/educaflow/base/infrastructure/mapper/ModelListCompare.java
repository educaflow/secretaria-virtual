package com.educaflow.base.infrastructure.mapper;

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
                    sources.stream().noneMatch(source -> BeanMapperUtil.getId(source) != null && BeanMapperUtil.getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toList());
    }

    public List<Object> getSourceWhereOnlySource() {
        return sources.stream()
                .filter(source ->
                    (BeanMapperUtil.getId(source) == null) ||
                    targets.stream().noneMatch(target -> target.getId() != null && BeanMapperUtil.getId(source).longValue() == target.getId().longValue() )
                ).collect(Collectors.toList());
    }

    public List<Object> getSourceWhereSourceAndTarget() {
        return sources.stream()
                .filter(source ->
                        BeanMapperUtil.getId(source) != null &&
                    targets.stream().anyMatch(target -> target.getId() != null && BeanMapperUtil.getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toList());
    }

    public List<Model> getTargetWhereSourceAndTarget() {
        return targets.stream()
                .filter(target ->
                    target.getId() != null &&
                    sources.stream().anyMatch(source -> BeanMapperUtil.getId(source) != null && BeanMapperUtil.getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toList());
    }
    
    


}
