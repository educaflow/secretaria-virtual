package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.ValueEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ScalarMapperTest {

    @Test
    void isScalarType_shouldRecognizeSupportedTypes() {
        assertTrue(ScalarMapper.isScalarType(String.class));
        assertTrue(ScalarMapper.isScalarType(Boolean.class));
        assertTrue(ScalarMapper.isScalarType(boolean.class));
        assertTrue(ScalarMapper.isScalarType(Integer.class));
        assertTrue(ScalarMapper.isScalarType(int.class));
        assertTrue(ScalarMapper.isScalarType(Long.class));
        assertTrue(ScalarMapper.isScalarType(long.class));
        assertTrue(ScalarMapper.isScalarType(BigDecimal.class));
        assertTrue(ScalarMapper.isScalarType(LocalDate.class));
        assertTrue(ScalarMapper.isScalarType(LocalTime.class));
        assertTrue(ScalarMapper.isScalarType(LocalDateTime.class));
        assertTrue(ScalarMapper.isScalarType(byte[].class));
        assertTrue(ScalarMapper.isScalarType(TestEnum.class));
    }

    @Test
    void isScalarType_shouldRejectUnsupportedTypes() {
        assertFalse(ScalarMapper.isScalarType(null));
        assertFalse(ScalarMapper.isScalarType(Double.class));
        assertFalse(ScalarMapper.isScalarType(PlainEnum.class));
        assertFalse(ScalarMapper.isScalarType(Object.class));
    }

    @Test
    void getScalarFromObject_shouldConvertPrimitivesAndNumbers() {
        assertEquals("123", ScalarMapper.getScalarFromObject(123, String.class));

        assertEquals(Boolean.TRUE, ScalarMapper.getScalarFromObject(true, Boolean.class));
        assertEquals(Boolean.TRUE, ScalarMapper.getScalarFromObject(1, Boolean.class));
        assertEquals(Boolean.FALSE, ScalarMapper.getScalarFromObject(0, Boolean.class));
        assertEquals(Boolean.TRUE, ScalarMapper.getScalarFromObject("TRUE", Boolean.class));
        assertEquals(Boolean.FALSE, ScalarMapper.getScalarFromObject("no", Boolean.class));

        assertEquals(42, ScalarMapper.getScalarFromObject(42L, Integer.class));
        assertEquals(42, ScalarMapper.getScalarFromObject("42", Integer.class));

        assertEquals(42L, ScalarMapper.getScalarFromObject(42, Long.class));
        assertEquals(42L, ScalarMapper.getScalarFromObject("42", Long.class));

        assertEquals(0, new BigDecimal("10.5").compareTo(ScalarMapper.getScalarFromObject("10.5", BigDecimal.class)));
        assertEquals(0, BigDecimal.valueOf(10).compareTo(ScalarMapper.getScalarFromObject(10, BigDecimal.class)));
        assertEquals(0, BigDecimal.valueOf(10L).compareTo(ScalarMapper.getScalarFromObject(10L, BigDecimal.class)));
        assertEquals(0, BigDecimal.valueOf(10.5d).compareTo(ScalarMapper.getScalarFromObject(10.5d, BigDecimal.class)));
        assertEquals(0, BigDecimal.valueOf(2.5f).compareTo(ScalarMapper.getScalarFromObject(2.5f, BigDecimal.class)));
    }

    @Test
    void getScalarFromObject_shouldConvertDatesAndTimes() {
        assertEquals(LocalDate.of(2026, 3, 24),
                ScalarMapper.getScalarFromObject("2026-03-24", LocalDate.class));
        assertEquals(LocalDate.of(2026, 3, 24),
                ScalarMapper.getScalarFromObject("2026-03-24T10:15:30+01:00", LocalDate.class));

        assertEquals(LocalTime.of(10, 15, 30),
                ScalarMapper.getScalarFromObject("10:15:30", LocalTime.class));

        assertEquals(LocalDateTime.of(2026, 3, 24, 10, 15, 30),
                ScalarMapper.getScalarFromObject("2026-03-24T10:15:30", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 3, 24, 10, 15, 30),
                ScalarMapper.getScalarFromObject("2026-03-24T10:15:30+01:00", LocalDateTime.class));
    }

    @Test
    void getScalarFromObject_shouldConvertBinaryAndEnums() {
        byte[] original = new byte[]{1, 2, 3, 4};
        String encoded = Base64.getEncoder().encodeToString(original);

        assertArrayEquals(original, ScalarMapper.getScalarFromObject(encoded, byte[].class));
        assertArrayEquals(original, ScalarMapper.getScalarFromObject(original, byte[].class));

        assertEquals(TestEnum.ACTIVE, ScalarMapper.getScalarFromObject("ACTIVE", TestEnum.class));
    }

    @Test
    void getScalarFromObject_shouldHandleNullInput() {
        assertNull(ScalarMapper.getScalarFromObject(null, String.class));
        assertNull(ScalarMapper.getScalarFromObject(null, Integer.class));
        assertNull(ScalarMapper.getScalarFromObject(null, TestEnum.class));
    }

    @Test
    void getScalarFromObject_shouldFailForInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("1", null));

        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("abc", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("abc", Long.class));
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("abc", BigDecimal.class));
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("abc", LocalDate.class));
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("abc", LocalTime.class));
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("abc", LocalDateTime.class));
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("not-base64", byte[].class));

        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject(5, byte[].class));
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("3.14", Double.class));
        assertThrows(IllegalArgumentException.class, () -> ScalarMapper.getScalarFromObject("A", PlainEnum.class));
    }

    enum TestEnum implements ValueEnum<String> {
        ACTIVE,
        INACTIVE;

        @Override
        public String getValue() {
            return name();
        }
    }

    enum PlainEnum {
        A,
        B
    }
}

