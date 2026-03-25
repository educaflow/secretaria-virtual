package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelCollectionCompareTest {

    @Test
    void shouldHandleNullCollectionsAsEmptyCollections() {
        ModelCollectionCompare compare = new ModelCollectionCompare(null, null);

        assertEquals(Set.of(), compare.getSourceWhereOnlySource());
        assertEquals(Set.of(), compare.getTargetWhereOnlyTarget());
        assertEquals(List.of(), compare.getInSourceAndInTarget());
    }

    @Test
    void shouldClassifySourcesAndTargetsById() {
        Model sourceModel1 = modelWithId(1L);
        Map<String, Object> sourceMap2 = mapSourceWithId("2");
        Map<String, Object> sourceMapNull = mapSourceWithId(null);
        Model sourceModelNull = modelWithId(null);
        Map<String, Object> sourceMap5 = mapSourceWithId(5L);

        Model targetModel1 = modelWithId(1L);
        Model targetModel3 = modelWithId(3L);
        Model targetModelNull = modelWithId(null);
        Model targetModel2 = modelWithId(2L);

        Collection<Object> sources = List.of(sourceModel1, sourceMap2, sourceMapNull, sourceModelNull, sourceMap5);
        Collection<Model> targets = List.of(targetModel1, targetModel3, targetModelNull, targetModel2);

        ModelCollectionCompare compare = new ModelCollectionCompare(sources, targets);

        Map<Long, Long> expectedSourceFrequency = new HashMap<>();
        expectedSourceFrequency.put(null, 2L);
        expectedSourceFrequency.put(5L, 1L);

        Set<Long> expectedTargetIds = new HashSet<>();
        expectedTargetIds.add(3L);
        expectedTargetIds.add(null);

        assertEquals(expectedSourceFrequency, sourceIdFrequency(compare.getSourceWhereOnlySource()));
        assertEquals(expectedTargetIds, targetIds(compare.getTargetWhereOnlyTarget()));
        assertEquals(Set.of("1->1", "2->2"), pairIds(compare.getInSourceAndInTarget()));
    }

    @Test
    void shouldThrowWhenSourceContainsNullElement() {
        List<Object> sources = new ArrayList<>();
        sources.add(null);

        ModelCollectionCompare compare = new ModelCollectionCompare(sources, List.of());

        assertThrows(IllegalArgumentException.class, compare::getSourceWhereOnlySource);
    }

    @Test
    void shouldThrowWhenSourceContainsUnsupportedType() {
        ModelCollectionCompare compare = new ModelCollectionCompare(List.of(42), List.of());

        assertThrows(IllegalArgumentException.class, compare::getSourceWhereOnlySource);
    }

    @Test
    void shouldThrowWhenMapIdCannotBeConvertedToLong() {
        Map<String, Object> invalidMap = mapSourceWithId("abc");
        ModelCollectionCompare compare = new ModelCollectionCompare(List.of(invalidMap), List.of(modelWithId(1L)));

        assertThrows(IllegalArgumentException.class, compare::getSourceWhereOnlySource);
    }

    private static Model modelWithId(Long id) {
        return new TestModel(id);
    }

    private static Map<String, Object> mapSourceWithId(Object id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        return map;
    }

    private static Map<Long, Long> sourceIdFrequency(Collection<Object> sources) {
        Map<Long, Long> frequency = new HashMap<>();
        for (Object source : sources) {
            Long id = sourceId(source);
            frequency.put(id, frequency.getOrDefault(id, 0L) + 1L);
        }
        return frequency;
    }

    private static Set<Long> targetIds(Collection<Model> targets) {
        return targets.stream()
                .map(Model::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static Set<String> pairIds(List<ModelCollectionCompare.SourceTargetEntities> pairs) {
        return pairs.stream()
                .map(pair -> sourceId(pair.source()) + "->" + pair.target().getId())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static Long sourceId(Object source) {
        if (source instanceof Model model) {
            return model.getId();
        }
        if (source instanceof Map<?, ?> map) {
            Object rawId = map.get("id");
            return rawId == null ? null : Long.valueOf(rawId.toString());
        }
        throw new IllegalArgumentException("Unsupported source type in test");
    }

    private static class TestModel extends Model {
        private Long id;

        private TestModel(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }
}
