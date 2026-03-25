package com.educaflow.base.infrastructure.mapper;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeanMapperUtilTest {

    // ───── Clases de dominio de prueba ─────

    static class Padre {
        String campoPadre;
    }

    static class Hijo extends Padre {
        @ManyToOne
        Object relacionManyToOne;

        @OneToOne
        Object relacionOneToOne;

        @OneToMany(mappedBy = "hijo")
        List<Object> relacionOneToMany;

        String campoSimple;
    }

    // ───── isManyToOne ─────

    @Test
    void isManyToOne_shouldReturnTrue_whenFieldHasManyToOneAnnotation() {
        assertTrue(BeanMapperUtil.isManyToOne(Hijo.class, "relacionManyToOne"));
    }

    @Test
    void isManyToOne_shouldReturnFalse_whenFieldDoesNotHaveManyToOneAnnotation() {
        assertFalse(BeanMapperUtil.isManyToOne(Hijo.class, "campoSimple"));
    }

    @Test
    void isManyToOne_shouldReturnFalse_whenFieldHasOneToOneAnnotation() {
        assertFalse(BeanMapperUtil.isManyToOne(Hijo.class, "relacionOneToOne"));
    }

    // ───── isOneToOne ─────

    @Test
    void isOneToOne_shouldReturnTrue_whenFieldHasOneToOneAnnotation() {
        assertTrue(BeanMapperUtil.isOneToOne(Hijo.class, "relacionOneToOne"));
    }

    @Test
    void isOneToOne_shouldReturnFalse_whenFieldDoesNotHaveOneToOneAnnotation() {
        assertFalse(BeanMapperUtil.isOneToOne(Hijo.class, "campoSimple"));
    }

    @Test
    void isOneToOne_shouldReturnFalse_whenFieldHasManyToOneAnnotation() {
        assertFalse(BeanMapperUtil.isOneToOne(Hijo.class, "relacionManyToOne"));
    }

    // ───── isOneToMany ─────

    @Test
    void isOneToMany_shouldReturnTrue_whenFieldHasOneToManyAnnotation() {
        assertTrue(BeanMapperUtil.isOneToMany(Hijo.class, "relacionOneToMany"));
    }

    @Test
    void isOneToMany_shouldReturnFalse_whenFieldDoesNotHaveOneToManyAnnotation() {
        assertFalse(BeanMapperUtil.isOneToMany(Hijo.class, "campoSimple"));
    }

    // ───── getMappedByInOneToMany ─────

    @Test
    void getMappedByInOneToMany_shouldReturnMappedByValue() {
        String mappedBy = BeanMapperUtil.getMappedByInOneToMany(Hijo.class, "relacionOneToMany");
        assertEquals("hijo", mappedBy);
    }

    // ───── getField ─────

    @Test
    void getField_shouldReturnField_whenFieldExistsInClass() {
        Field field = BeanMapperUtil.getField(Hijo.class, "campoSimple");
        assertNotNull(field);
        assertEquals("campoSimple", field.getName());
    }

    @Test
    void getField_shouldReturnField_whenFieldIsInSuperclass() {
        Field field = BeanMapperUtil.getField(Hijo.class, "campoPadre");
        assertNotNull(field);
        assertEquals("campoPadre", field.getName());
    }

    @Test
    void getField_shouldThrowRuntimeException_whenFieldDoesNotExist() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> BeanMapperUtil.getField(Hijo.class, "campoInexistente"));
        assertNotNull(ex);
    }
}

