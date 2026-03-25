package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelListCompareTest {

    @Test
    void shouldHandleNullListsAsEmptyLists() {
        ModelListCompare compare = new ModelListCompare(null, null);

        assertEquals(List.of(), compare.getSourceWhereOnlySource());
        assertEquals(List.of(), compare.getSourceWhereSourceAndTarget());
        assertEquals(List.of(), compare.getTargetWhereOnlyTarget());
        assertEquals(List.of(), compare.getTargetWhereSourceAndTarget());
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

        List<Object> sources = List.of(sourceModel1, sourceMap2, sourceMapNull, sourceModelNull, sourceMap5);
        List<Model> targets = List.of(targetModel1, targetModel3, targetModelNull, targetModel2);

        ModelListCompare compare = new ModelListCompare(sources, targets);

        assertEquals(Arrays.asList(null, null, 5L), sourceIds(compare.getSourceWhereOnlySource()));
        assertEquals(List.of(1L, 2L), sourceIds(compare.getSourceWhereSourceAndTarget()));
        assertEquals(Arrays.asList(3L, null), targetIds(compare.getTargetWhereOnlyTarget()));
        assertEquals(List.of(1L, 2L), targetIds(compare.getTargetWhereSourceAndTarget()));
    }

    @Test
    void shouldThrowWhenSourceContainsNullElement() {
        List<Object> sources = new ArrayList<>();
        sources.add(null);
        ModelListCompare compare = new ModelListCompare(sources, List.of());

        assertThrows(IllegalArgumentException.class, compare::getSourceWhereOnlySource);
    }

    @Test
    void shouldThrowWhenSourceContainsUnsupportedType() {
        ModelListCompare compare = new ModelListCompare(List.of(42), List.of());

        assertThrows(IllegalArgumentException.class, compare::getSourceWhereOnlySource);
    }

    @Test
    void shouldThrowWhenMapIdCannotBeConvertedToLong() {
        Map<String, Object> invalidMap = mapSourceWithId("abc");
        ModelListCompare compare = new ModelListCompare(List.of(invalidMap), List.of(modelWithId(1L)));

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

    private static List<Long> sourceIds(List<Object> sources) {
        List<Long> ids = new ArrayList<>();
        for (Object source : sources) {
            ids.add(sourceId(source));
        }
        return ids;
    }

    private static List<Long> targetIds(List<Model> targets) {
        List<Long> ids = new ArrayList<>();
        for (Model target : targets) {
            ids.add(target.getId());
        }
        return ids;
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

