package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.ValueEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public class ScalarMapper{

    public static boolean isScalarType(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return clazz.equals(String.class) ||
                clazz.equals(Boolean.class) || clazz.equals(boolean.class) ||
                clazz.equals(Integer.class) || clazz.equals(int.class) ||
                clazz.equals(Long.class) || clazz.equals(long.class) ||
                clazz.equals(BigDecimal.class) ||
                clazz.equals(LocalDate.class) ||
                clazz.equals(LocalTime.class) ||
                clazz.equals(LocalDateTime.class) ||
                clazz.equals(byte[].class) ||
                (clazz.isEnum() && ValueEnum.class.isAssignableFrom(clazz)); // Check if it's an enum and implements ValueEnum
    }

    @SuppressWarnings("unchecked")
    public static <T> T getScalarFromObject(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Target class for conversion cannot be null.");
        }

        if (clazz.equals(String.class)) {
            return (T) getStringFromObject(obj);
        } else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return (T) getBooleanFromObject(obj);
        } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return (T) getIntegerFromObject(obj);
        } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            return (T) getLongFromObject(obj);
        } else if (clazz.equals(BigDecimal.class)) {
            return (T) getBigDecimalFromObject(obj);
        } else if (clazz.equals(LocalDate.class)) {
            return (T) getLocalDateFromObject(obj);
        } else if (clazz.equals(LocalTime.class)) {
            return (T) getLocalTimeFromObject(obj);
        } else if (clazz.equals(LocalDateTime.class)) {
            return (T) getLocalDateTimeFromObject(obj);
        } else if (clazz.equals(byte[].class)) {
            return (T) getBinaryFromObject(obj);
        } else if (clazz.isEnum() && ValueEnum.class.isAssignableFrom(clazz)) {
            // This cast is safe because we check `clazz.isEnum()` and `ValueEnum.class.isAssignableFrom(clazz)`
            return (T) ValueEnum.of((Class<Enum>) clazz, obj);
        } else {
            throw new IllegalArgumentException("Unsupported scalar type for conversion: " + clazz.getName());
        }
    }


    private static String getStringFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String string) {
            return string;
        }
        return obj.toString();
    }
    private static Boolean getBooleanFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean b) {
            return b;
        }
        if (obj instanceof Number number) {
            return number.intValue() != 0;
        }
        if (obj instanceof String string) {
            return "true".equalsIgnoreCase(string);
        }
        throw new IllegalArgumentException("Cannot convert " + obj + " to boolean:" + obj.getClass());

    }
    private static Integer getIntegerFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer i) {
            return i;
        }
        if (obj instanceof Long l) {
            return l.intValue();
        }
        if (obj instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert " + obj + " to int");
            }
        }
        throw new IllegalArgumentException("Cannot convert " + obj + " to Integer:" + obj.getClass());
    }
    private static Long getLongFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long l) {
            return l;
        }
        if (obj instanceof Integer i) {
            return i.longValue();
        }
        if (obj instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert " + obj + " to long");
            }
        }
        throw new IllegalArgumentException("Cannot convert " + obj + " to Long:" + obj.getClass());
    }
    private static BigDecimal getBigDecimalFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (obj instanceof String string) {
            try {
                return new BigDecimal(string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert " + obj + " to BigDecimal");
            }
        }
        if (obj instanceof Integer i) {
            return BigDecimal.valueOf(i);
        }
        if (obj instanceof Long l) {
            return BigDecimal.valueOf(l);
        }

        if (obj instanceof Double d) {
            return BigDecimal.valueOf(d);
        }
        if (obj instanceof Float f) {
            return BigDecimal.valueOf(f);
        }
        throw new IllegalArgumentException("Cannot convert " + obj + " to BigDecimal:" + obj.getClass());
    }

    private static LocalDate getLocalDateFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LocalDate localDate) {
            return localDate;
        }
        if (obj instanceof String string) {
            try {
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(string);
                return offsetDateTime.toLocalDate();
            } catch (DateTimeParseException e) {
                try {
                    return LocalDate.parse(string);
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Cannot convert " + obj + " to LocalDate");
                }
            }
        }
        throw new IllegalArgumentException("Cannot convert " + obj + " to LocalDate:" + obj.getClass());
    }

    private static LocalTime getLocalTimeFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LocalTime localTime) {
            return localTime;
        }
        if (obj instanceof String string) {
            try {
                return LocalTime.parse(string);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Cannot convert " + obj + " to LocalTime");
            }
        }
        throw new IllegalArgumentException("Cannot convert " + obj + " to LocalTime:" + obj.getClass());
    }

    private static LocalDateTime getLocalDateTimeFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (obj instanceof String string) {
            try {
                // Primero intentamos parsear como OffsetDateTime para manejar la zona horaria.
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(string);
                return offsetDateTime.toLocalDateTime();
            } catch (DateTimeParseException e) {
                try {
                    return LocalDateTime.parse(string);
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Cannot convert " + obj + " to LocalDateTime");
                }
            }
        }
        throw new IllegalArgumentException("Cannot convert " + obj + " to LocalDateTime:" + obj.getClass());
    }
    private static byte[] getBinaryFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof byte[] array) {
            return array;
        }
        if (obj instanceof String string) {
            try {
                return Base64.getDecoder().decode(string);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot convert " + obj + " to byte[]");
            }
        }
        throw new IllegalArgumentException("Cannot convert " + obj + " to byte[]:" + obj.getClass());
    }

}
