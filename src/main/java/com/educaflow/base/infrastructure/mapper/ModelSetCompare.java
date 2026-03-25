package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelSetCompare {

    private final Set<Object> sources;
    private final Set<Model> targets;


    public ModelSetCompare(Set<Object> sources, Set<Model> targets) {
        this.sources = sources != null ? new LinkedHashSet<>(sources) : new LinkedHashSet<>();
        this.targets = targets != null ? new LinkedHashSet<>(targets) : new LinkedHashSet<>();
    }

    public Set<Model> getTargetWhereOnlyTarget() {
        return targets.stream()
                .filter(target ->
                        (target.getId() == null) ||
                                sources.stream().noneMatch(source -> BeanMapperUtil.getId(source) != null && BeanMapperUtil.getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toSet());
    }

    public Set<Object> getSourceWhereOnlySource() {
        return sources.stream()
                .filter(source ->
                        (BeanMapperUtil.getId(source) == null) ||
                                targets.stream().noneMatch(target -> target.getId() != null && BeanMapperUtil.getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toSet());
    }

    public Set<Object> getSourceWhereSourceAndTarget() {
        return sources.stream()
                .filter(source ->
                        BeanMapperUtil.getId(source) != null &&
                                targets.stream().anyMatch(target -> target.getId() != null && BeanMapperUtil.getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toSet());
    }

    public Set<Model> getTargetWhereSourceAndTarget() {
        return targets.stream()
                .filter(target ->
                        target.getId() != null &&
                                sources.stream().anyMatch(source -> BeanMapperUtil.getId(source) != null && BeanMapperUtil.getId(source).longValue() == target.getId().longValue())
                ).collect(Collectors.toSet());
    }




}
