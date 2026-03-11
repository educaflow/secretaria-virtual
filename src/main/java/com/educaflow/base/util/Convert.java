package com.educaflow.base.util;

import com.axelor.db.ValueEnum;
import com.axelor.db.annotations.EnumWidget;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class Convert {

    public static final Locale defaultLocale = new Locale.Builder().setLanguage("es").setRegion("ES").build();
    public static final ZoneId defaultZoneId = ZoneId.of("Europe/Madrid");

    public static Long objectToLong(Object obj) {
        if (obj == null) {
            return null;
        } else {
            return ((Number) obj).longValue();
        }
    }

    public static long coerceToLong(Object  obj) {
        if (obj == null) {
            return 0;
        } if (obj instanceof String) {
            if (((String) obj).isEmpty()) {
                return 0;
            } else {
                return Long.parseLong((String) obj);
            }
        } else {
            return ((Number) obj).longValue();
        }
    }

    public static int coerceToInt(Object  obj) {
        if (obj == null) {
            return 0;
        } if (obj instanceof String) {
            if (((String) obj).isEmpty()) {
                return 0;
            } else {
                return Integer.parseInt((String) obj);
            }
        } else {
            return ((Number) obj).intValue();
        }
    }

    public static String objectToUserString(Object obj) {
        try {

            String userString;

            if (obj == null) {
                userString = "";
            } else if (obj instanceof Boolean) {
                userString = ((Boolean) obj) ? "Sí" : "No";
            } else if ((obj instanceof Long) || (obj instanceof Integer) || (obj instanceof Byte) || (obj instanceof Short)) {
                NumberFormat integerFormat = NumberFormat.getIntegerInstance(defaultLocale);
                userString = integerFormat.format(obj);
            } else if (obj instanceof Number) {
                NumberFormat nf = NumberFormat.getNumberInstance(defaultLocale);
                nf.setGroupingUsed(true);
                nf.setMaximumFractionDigits(2);
                userString = nf.format(obj);
            } else if (obj instanceof LocalDate) {
                userString = DateTimeFormatter.ofPattern("dd/MM/yyyy").format((LocalDate) obj);
            } else if (obj instanceof LocalTime) {
                userString = DateTimeFormatter.ofPattern("HH:mm").format((LocalTime) obj);
            } else if (obj instanceof LocalDateTime) {
                userString = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format((LocalDateTime) obj);
            } else if (obj instanceof Instant) {
                userString = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(((Instant) obj).atZone(defaultZoneId));
            } else if (obj instanceof Date) {
                userString = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(((Date) obj).toInstant().atZone(defaultZoneId));
            } else if (obj instanceof ValueEnum) {
                Class<?> clazz = obj.getClass();

                String enumName = ((Enum<?>) obj).name();

                // Accedemos al campo del enum por su nombre
                Field field = clazz.getField(enumName);

                // Obtenemos la anotación EnumWidget
                EnumWidget annotation = field.getAnnotation(EnumWidget.class);
                if (annotation != null) {
                    String title = annotation.title();
                    if (title != null) {
                        if (title.equalsIgnoreCase("")) {
                            userString = TextUtil.humanize(enumName);
                        } else {
                            userString = title;
                        }
                    } else  {
                        userString = TextUtil.humanize(enumName);
                    }
                } else {
                    userString = TextUtil.humanize(enumName);
                }

            } else if (obj instanceof Enum) {
                String enumName = ((Enum<?>) obj).name();
                userString = TextUtil.humanize(enumName);
            } else {
                userString = obj.toString();
            }

            return userString;
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir el objeto a String: " + obj, e);
        }

    }


}
